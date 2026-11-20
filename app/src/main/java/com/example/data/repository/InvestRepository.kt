package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.domain.ExchangeCalculator
import com.example.data.model.InvestCatalog
import com.example.data.model.InvestCategory
import com.example.data.model.InvestCurrency
import com.example.data.model.InvestDuration
import com.example.data.model.InvestStatus
import com.example.data.model.InvestmentRecord
import com.example.data.model.PaymentMethod
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Locale
import kotlin.math.round

class InvestRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private val random = SecureRandom()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google.")
    }

    fun observeUserInvestments(userId: String): Flow<List<InvestmentRecord>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_INVESTMENTS)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { query ->
                    query.documents
                        .mapNotNull { doc ->
                            doc.toObject(
                                InvestmentRecord::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_INVESTMENTS)
                    throw error
                }
        )
    }

    fun observeAllInvestments(): Flow<List<InvestmentRecord>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_INVESTMENTS)
                .snapshots()
                .map { query ->
                    query.documents
                        .mapNotNull { doc ->
                            doc.toObject(
                                InvestmentRecord::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_INVESTMENTS)
                    throw error
                }
        )
    }

    suspend fun createInvestment(
        userEmail: String,
        category: InvestCategory,
        sector: String,
        amount: Double,
        currency: InvestCurrency,
        duration: InvestDuration,
        gateway: PaymentMethod,
    ): Result<InvestmentRecord> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (gateway !in InvestCatalog.SUPPORTED_GATEWAYS) {
            return Result.failure(IllegalArgumentException("Passerelle non prise en charge pour Investir."))
        }
        if (InvestCatalog.currencyForGateway(gateway) != currency) {
            return Result.failure(
                IllegalArgumentException("La passerelle ${gateway.label} utilise ${InvestCatalog.currencyForGateway(gateway).code}.")
            )
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Montant d'investissement invalide."))
        }

        val effectiveSector = when (category) {
            InvestCategory.A_RISQUE -> sector.trim().ifBlank { InvestCatalog.A_RISQUE_SECTORS.first().name }
            else -> category.defaultSector
        }.take(120)

        val sim = InvestCatalog.calculateSimulation(
            category = category,
            amount = amount,
            duration = duration,
            availableBalance = Double.MAX_VALUE,
        )
        val investCode = "INV-" + (1000 + random.nextInt(9000))
        val endDateLabel = InvestCatalog.computeMaturityDateLabel(duration.months)
        val balanceField = ExchangeCalculator.balanceFieldFor(gateway)
        val userRef = db.collection(AleviniRepository.COLLECTION_USERS).document(uid)
        val docRef = db.collection(COLLECTION_INVESTMENTS).document()

        val payload = hashMapOf<String, Any?>(
            "userId" to uid,
            "userEmail" to userEmail.take(200),
            "investCode" to investCode,
            "categoryKey" to category.id,
            "categoryName" to category.title,
            "sector" to effectiveSector,
            "amount" to amount,
            "currency" to currency.code,
            "durationLabel" to duration.label,
            "months" to duration.months,
            "monthlyProfit" to sim.monthlyProfit,
            "totalProfit" to sim.totalProfit,
            "gatewayId" to gateway.id,
            "gatewayName" to gateway.label,
            "feeAmount" to sim.feeAmount,
            "totalPaid" to sim.totalCost,
            "endDateLabel" to endDateLabel,
            "status" to InvestStatus.PENDING.name,
            "adminNote" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        return try {
            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) {
                    throw IllegalStateException("Rechargez d'abord votre compte ${gateway.label} avant d'investir.")
                }
                if (userSnap.getBoolean("isBlocked") == true) {
                    throw IllegalStateException("Votre compte est bloqué par l'administrateur.")
                }
                val currentBalance = userSnap.getDouble(balanceField) ?: 0.0
                if (currentBalance + 1e-6 < sim.totalCost) {
                    throw IllegalStateException(
                        "Solde ${gateway.label} insuffisant (${String.format(Locale.US, "%,.2f", currentBalance)} ${currency.code} disponibles sur ${String.format(Locale.US, "%,.2f", sim.totalCost)} ${currency.code} requis)."
                    )
                }
                val nextBalance = round((currentBalance - sim.totalCost).coerceAtLeast(0.0) * 100.0) / 100.0
                tx.update(
                    userRef,
                    mapOf(
                        balanceField to nextBalance,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                tx.set(docRef, payload)
                null
            }.await()

            Result.success(
                InvestmentRecord(
                    id = docRef.id,
                    userId = uid,
                    userEmail = userEmail,
                    investCode = investCode,
                    categoryKey = category.id,
                    categoryName = category.title,
                    sector = effectiveSector,
                    amount = amount,
                    currency = currency.code,
                    durationLabel = duration.label,
                    months = duration.months,
                    monthlyProfit = sim.monthlyProfit,
                    totalProfit = sim.totalProfit,
                    gatewayId = gateway.id,
                    gatewayName = gateway.label,
                    feeAmount = sim.feeAmount,
                    totalPaid = sim.totalCost,
                    endDateLabel = endDateLabel,
                    status = InvestStatus.PENDING.name,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminDecideInvestment(
        investment: InvestmentRecord,
        approve: Boolean,
        adminNote: String = "",
    ): Result<Unit> {
        val adminUid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_INVESTMENTS).document(investment.id)
        val userRef = if (investment.userId.isNotBlank()) {
            db.collection(AleviniRepository.COLLECTION_USERS).document(investment.userId)
        } else {
            null
        }
        val notifRef = db.collection(AleviniRepository.COLLECTION_ADMIN_NOTIFICATIONS).document()
        val ledgerRef = db.collection(AleviniRepository.COLLECTION_LEDGER).document()
        val gateway = PaymentMethod.fromId(investment.gatewayId)
        val balanceField = ExchangeCalculator.balanceFieldFor(gateway)
        val targetStatus = if (approve) InvestStatus.ACTIVE else InvestStatus.REJECTED

        return try {
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) {
                    throw IllegalStateException("Investissement introuvable.")
                }
                val currentStatus = snap.getString("status").orEmpty()
                val shouldRefund = !approve &&
                    currentStatus == InvestStatus.PENDING.name &&
                    userRef != null
                val userSnap = if (shouldRefund && userRef != null) tx.get(userRef) else null

                val noteText = adminNote.trim().ifBlank {
                    if (approve) "Investissement activé par Admin." else "Refusé par Admin • Montant remboursé."
                }.take(300)

                tx.update(
                    docRef,
                    mapOf(
                        "status" to targetStatus.name,
                        "adminNote" to noteText,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                if (shouldRefund && userRef != null && userSnap != null && userSnap.exists()) {
                    val currentBalance = userSnap.getDouble(balanceField) ?: 0.0
                    val nextBalance = round((currentBalance + investment.totalPaid) * 100.0) / 100.0
                    tx.update(
                        userRef,
                        mapOf(
                            balanceField to nextBalance,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }

                val ledgerCurrency = if (investment.currency == "GDS") "HTG" else "USD"
                tx.set(
                    ledgerRef,
                    mapOf(
                        "orderId" to investment.id,
                        "orderCode" to investment.investCode,
                        "accountName" to "INVEST_${gateway.id.uppercase(Locale.US)}",
                        "entryType" to if (approve) "CREDIT" else "DEBIT",
                        "currency" to ledgerCurrency,
                        "amount" to investment.totalPaid.coerceAtLeast(1.0),
                        "note" to (if (approve) "Activation ${investment.investCode} (${investment.sector})" else "Remboursement refus ${investment.investCode}").take(200),
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )

                if (investment.userId.isNotBlank()) {
                    val title = if (approve) {
                        "Investissement ${investment.investCode} activé"
                    } else {
                        "Investissement ${investment.investCode} refusé"
                    }
                    val message = if (approve) {
                        "Votre placement de ${InvestCatalog.formatInvestMoney(investment.amount, investment.currency)} (${investment.durationLabel}) est actif jusqu'au ${investment.endDateLabel}."
                    } else {
                        "Votre demande ${investment.investCode} a été refusée. Le montant de ${InvestCatalog.formatInvestMoney(investment.totalPaid, investment.currency)} a été recrédité sur ${gateway.label}."
                    }
                    tx.set(
                        notifRef,
                        mapOf(
                            "title" to title.take(120),
                            "message" to message.take(500),
                            "targetUserId" to investment.userId,
                            "targetLabel" to investment.userEmail.ifBlank { investment.userId }.take(160),
                            "isAlert" to !approve,
                            "sentBy" to adminUid,
                            "createdAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRejectedInvestment(investId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_INVESTMENTS).document(investId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) return Result.success(Unit)
            val status = snap.getString("status").orEmpty()
            if (status != InvestStatus.REJECTED.name) {
                return Result.failure(
                    IllegalStateException("Cet investissement est actif/en attente et a été retiré localement de votre vue.")
                )
            }
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteInvestment(investId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_INVESTMENTS).document(investId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null) {
            handleFirestoreError(toLog, op, path)
        }
    }

    companion object {
        const val COLLECTION_INVESTMENTS = "investments"
    }
}
