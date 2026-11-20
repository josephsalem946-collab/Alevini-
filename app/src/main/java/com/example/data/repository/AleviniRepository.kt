package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.domain.ExchangeCalculator
import com.example.data.model.AdminNotification
import com.example.data.model.CryptoCoin
import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeConfig
import com.example.data.model.ExchangeOrder
import com.example.data.model.GatewayStatus
import com.example.data.model.LedgerEntry
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentSession
import com.example.data.model.UserProfile
import com.example.data.model.WebhookEvent
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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
import kotlin.math.roundToLong

class AleviniRepository(
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

    suspend fun getFreshIdToken(): String? {
        val user = auth.currentUser ?: return null
        return try {
            user.getIdToken(false).await()?.token
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    fun observeExchangeConfig(): Flow<ExchangeConfig> = flow {
        requireUserId()
        val path = "$COLLECTION_CONFIG/$DOC_DEFAULT_CONFIG"
        emitAll(
            db.collection(COLLECTION_CONFIG).document(DOC_DEFAULT_CONFIG)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ExchangeConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: ExchangeConfig()
                    } else {
                        ExchangeConfig()
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "$COLLECTION_USERS/$userId"
        emitAll(
            db.collection(COLLECTION_USERS).document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == userId) {
                        handleFlowError(error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        email: String,
        displayName: String,
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_USERS).document(userId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to userId,
                    "email" to email.take(200),
                    "displayName" to displayName.ifBlank { "Utilisateur Alevini" }.take(120),
                    "phoneNumber" to "",
                    "isAgeVerified" to true,
                    "isBlocked" to false,
                    "balanceMoncashHtg" to 0.0,
                    "balanceNatcashHtg" to 0.0,
                    "balanceBinanceUsd" to 0.0,
                    "balanceMeruUsd" to 0.0,
                    "balanceNowpaymentsUsd" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        phoneNumber: String,
        isAgeVerified: Boolean,
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_USERS).document(userId)
        return try {
            val updates = mapOf(
                "displayName" to displayName.trim().ifBlank { "Utilisateur Alevini" }.take(120),
                "phoneNumber" to phoneNumber.trim().take(40),
                "isAgeVerified" to isAgeVerified,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserOrders(userId: String): Flow<List<ExchangeOrder>> = flow {
        val path = COLLECTION_ORDERS
        emitAll(
            db.collection(COLLECTION_ORDERS)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ExchangeOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == userId) {
                        handleFlowError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createOrder(
        config: ExchangeConfig,
        userEmail: String,
        sendMethod: PaymentMethod,
        receiveMethod: PaymentMethod,
        sendCurrency: CurrencyCode,
        receiveCurrency: CurrencyCode,
        cryptoCoin: CryptoCoin?,
        sendAmount: Double,
        destAccount: String,
    ): Result<ExchangeOrder> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val cleanDest = destAccount.trim()
        if (cleanDest.length < 3) {
            return Result.failure(IllegalArgumentException("Compte de réception invalide (minimum 3 caractères)."))
        }

        val validation = ExchangeCalculator.compute(
            config = config,
            sendAmount = sendAmount,
            sendCurrency = sendCurrency,
            receiveCurrency = receiveCurrency,
            sendMethod = sendMethod,
            receiveMethod = receiveMethod,
        )
        val calc = when (validation) {
            is ExchangeCalculator.ValidationResult.Invalid ->
                return Result.failure(IllegalArgumentException(validation.message))
            is ExchangeCalculator.ValidationResult.Valid -> validation.calculation
        }

        val orderCode = "ALV-${100000 + random.nextInt(900000)}"
        val expiresAtMillis = System.currentTimeMillis() + 15L * 60L * 1000L
        val coinId = if (sendMethod == PaymentMethod.NOWPAYMENTS) {
            (cryptoCoin ?: CryptoCoin.USDT_TRC20).id
        } else {
            ""
        }
        val payCryptoAmount = if (sendMethod == PaymentMethod.NOWPAYMENTS) {
            val rate = (cryptoCoin ?: CryptoCoin.USDT_TRC20).approxUsdRate
            ((calc.grossUsd / rate) * 1_000_000.0).roundToLong() / 1_000_000.0
        } else {
            0.0
        }
        val payAddress = if (sendMethod == PaymentMethod.NOWPAYMENTS) {
            "TRC20-${orderCode.replace("-", "")}ALV"
        } else {
            ""
        }
        val instruction = "Financé depuis votre solde ${sendMethod.label} (${ExchangeCalculator.requiredGatewayDebit(config, sendMethod, sendCurrency, sendAmount)} ${sendMethod.defaultCurrency.code}) → ${receiveMethod.label} : $cleanDest"
        val requiredDebit = ExchangeCalculator.requiredGatewayDebit(
            config = config,
            sendMethod = sendMethod,
            sendCurrency = sendCurrency,
            sendAmount = sendAmount,
        )
        val balanceField = ExchangeCalculator.balanceFieldFor(sendMethod)
        val userRef = db.collection(COLLECTION_USERS).document(uid)
        val docRef = db.collection(COLLECTION_ORDERS).document()
        val payload = mapOf(
            "userId" to uid,
            "userEmail" to userEmail.take(200),
            "orderCode" to orderCode,
            "sendMethod" to sendMethod.id,
            "receiveMethod" to receiveMethod.id,
            "sendCurrency" to sendCurrency.code,
            "receiveCurrency" to receiveCurrency.code,
            "cryptoCoin" to coinId,
            "sendAmount" to sendAmount,
            "receiveAmount" to calc.receiveAmount,
            "amountUsd" to calc.netUsd,
            "amountHtg" to calc.amountHtg,
            "amountEur" to calc.amountEur,
            "baseRate" to calc.baseRate,
            "feePercent" to calc.feePercent,
            "effectiveRate" to calc.effectiveRate,
            "destAccount" to cleanDest.take(160),
            "paymentInstruction" to instruction.take(300),
            "payAddress" to payAddress,
            "payCryptoAmount" to payCryptoAmount,
            "externalTxRef" to "",
            "gatewayMode" to "WALLET",
            "gatewayOrderId" to "",
            "checkoutUrl" to "",
            "gatewayStatus" to GatewayStatus.NONE.name,
            "orderType" to "EXCHANGE",
            "status" to OrderStatus.PENDING.name,
            "adminNote" to "",
            "expiresAtMillis" to expiresAtMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        return try {
            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) {
                    throw IllegalStateException("Rechargez d'abord votre compte ${sendMethod.label} avant d'échanger.")
                }
                if (userSnap.getBoolean("isBlocked") == true) {
                    throw IllegalStateException("Votre compte est bloqué par l'administrateur.")
                }
                val currentBalance = userSnap.getDouble(balanceField) ?: 0.0
                if (currentBalance + 1e-6 < requiredDebit) {
                    throw IllegalStateException(
                        "Solde ${sendMethod.label} insuffisant (${formatBalance(currentBalance)} ${sendMethod.defaultCurrency.code} disponibles sur ${formatBalance(requiredDebit)} ${sendMethod.defaultCurrency.code} requis). Rechargez votre compte ${sendMethod.label}."
                    )
                }
                val nextBalance = kotlin.math.round((currentBalance - requiredDebit).coerceAtLeast(0.0) * 100.0) / 100.0
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

            val created = ExchangeOrder(
                id = docRef.id,
                userId = uid,
                userEmail = userEmail,
                orderCode = orderCode,
                sendMethod = sendMethod.id,
                receiveMethod = receiveMethod.id,
                sendCurrency = sendCurrency.code,
                receiveCurrency = receiveCurrency.code,
                cryptoCoin = coinId,
                sendAmount = sendAmount,
                receiveAmount = calc.receiveAmount,
                amountUsd = calc.netUsd,
                amountHtg = calc.amountHtg,
                amountEur = calc.amountEur,
                baseRate = calc.baseRate,
                feePercent = calc.feePercent,
                effectiveRate = calc.effectiveRate,
                destAccount = cleanDest,
                paymentInstruction = instruction,
                payAddress = payAddress,
                payCryptoAmount = payCryptoAmount,
                externalTxRef = "",
                gatewayMode = "WALLET",
                gatewayOrderId = "",
                checkoutUrl = "",
                gatewayStatus = GatewayStatus.NONE.name,
                orderType = "EXCHANGE",
                status = OrderStatus.PENDING.name,
                adminNote = "",
                expiresAtMillis = expiresAtMillis,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createTopUpOrder(
        config: ExchangeConfig,
        userEmail: String,
        gatewayMethod: PaymentMethod,
        cryptoCoin: CryptoCoin?,
        amount: Double,
    ): Result<ExchangeOrder> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val validation = ExchangeCalculator.computeTopUp(
            config = config,
            gatewayMethod = gatewayMethod,
            amount = amount,
        )
        val calc = when (validation) {
            is ExchangeCalculator.ValidationResult.Invalid ->
                return Result.failure(IllegalArgumentException(validation.message))
            is ExchangeCalculator.ValidationResult.Valid -> validation.calculation
        }

        val nativeCurrency = gatewayMethod.defaultCurrency
        val orderCode = "TOP-${100000 + random.nextInt(900000)}"
        val expiresAtMillis = System.currentTimeMillis() + 15L * 60L * 1000L
        val coinId = if (gatewayMethod == PaymentMethod.NOWPAYMENTS) {
            (cryptoCoin ?: CryptoCoin.USDT_TRC20).id
        } else {
            ""
        }
        val payCryptoAmount = if (gatewayMethod == PaymentMethod.NOWPAYMENTS) {
            val rate = (cryptoCoin ?: CryptoCoin.USDT_TRC20).approxUsdRate
            ((calc.grossUsd / rate) * 1_000_000.0).roundToLong() / 1_000_000.0
        } else {
            0.0
        }
        val payAddress = if (gatewayMethod == PaymentMethod.NOWPAYMENTS) {
            "TRC20-${orderCode.replace("-", "")}ALV"
        } else {
            ""
        }
        val instruction = ExchangeCalculator.buildPaymentInstruction(
            config = config,
            sendMethod = gatewayMethod,
            cryptoCoinId = coinId,
            payCryptoAmount = payCryptoAmount,
        )
        val destWalletLabel = "Portefeuille ${gatewayMethod.label} (${nativeCurrency.code})"

        val docRef = db.collection(COLLECTION_ORDERS).document()
        val payload = mapOf(
            "userId" to uid,
            "userEmail" to userEmail.take(200),
            "orderCode" to orderCode,
            "sendMethod" to gatewayMethod.id,
            "receiveMethod" to gatewayMethod.id,
            "sendCurrency" to nativeCurrency.code,
            "receiveCurrency" to nativeCurrency.code,
            "cryptoCoin" to coinId,
            "sendAmount" to amount,
            "receiveAmount" to calc.receiveAmount,
            "amountUsd" to calc.netUsd,
            "amountHtg" to calc.amountHtg,
            "amountEur" to calc.amountEur,
            "baseRate" to calc.baseRate,
            "feePercent" to calc.feePercent,
            "effectiveRate" to calc.effectiveRate,
            "destAccount" to destWalletLabel,
            "paymentInstruction" to instruction.take(300),
            "payAddress" to payAddress,
            "payCryptoAmount" to payCryptoAmount,
            "externalTxRef" to "",
            "gatewayMode" to "MANUAL",
            "gatewayOrderId" to "",
            "checkoutUrl" to "",
            "gatewayStatus" to GatewayStatus.NONE.name,
            "orderType" to "TOPUP",
            "status" to OrderStatus.PENDING.name,
            "adminNote" to "",
            "expiresAtMillis" to expiresAtMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        return try {
            docRef.set(payload).await()
            val created = ExchangeOrder(
                id = docRef.id,
                userId = uid,
                userEmail = userEmail,
                orderCode = orderCode,
                sendMethod = gatewayMethod.id,
                receiveMethod = gatewayMethod.id,
                sendCurrency = nativeCurrency.code,
                receiveCurrency = nativeCurrency.code,
                cryptoCoin = coinId,
                sendAmount = amount,
                receiveAmount = calc.receiveAmount,
                amountUsd = calc.netUsd,
                amountHtg = calc.amountHtg,
                amountEur = calc.amountEur,
                baseRate = calc.baseRate,
                feePercent = calc.feePercent,
                effectiveRate = calc.effectiveRate,
                destAccount = destWalletLabel,
                paymentInstruction = instruction,
                payAddress = payAddress,
                payCryptoAmount = payCryptoAmount,
                externalTxRef = "",
                gatewayMode = "MANUAL",
                gatewayOrderId = "",
                checkoutUrl = "",
                gatewayStatus = GatewayStatus.NONE.name,
                orderType = "TOPUP",
                status = OrderStatus.PENDING.name,
                adminNote = "",
                expiresAtMillis = expiresAtMillis,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    private fun formatBalance(value: Double): String = String.format(java.util.Locale.US, "%,.2f", value)

    suspend fun submitTxRef(orderId: String, rawTxRef: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val trimmed = rawTxRef.trim()
        if (trimmed.length < 4 || trimmed.length > 120) {
            return Result.failure(IllegalArgumentException("Référence de transaction invalide (4 à 120 caractères)."))
        }

        val sanitizedRefId = trimmed
            .uppercase()
            .replace(Regex("[^A-Z0-9_\\-]"), "_")
            .take(128)
            .ifBlank { "REF_${System.currentTimeMillis()}" }

        val txRefDoc = db.collection(COLLECTION_TX_REFS).document(sanitizedRefId)
        val orderDoc = db.collection(COLLECTION_ORDERS).document(orderId)

        return try {
            val existingClaim = txRefDoc.get().await()
            if (existingClaim.exists()) {
                return Result.failure(IllegalStateException("Cette référence a déjà été utilisée !"))
            }

            val batch = db.batch()
            batch.set(
                txRefDoc,
                mapOf(
                    "userId" to uid,
                    "orderId" to orderId,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                orderDoc,
                mapOf(
                    "externalTxRef" to trimmed,
                    "status" to OrderStatus.SUBMITTED.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, orderDoc.path)
            val msg = e.message.orEmpty()
            if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                Result.failure(IllegalStateException("Référence déjà utilisée ou commande expirée."))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun markOrderExpired(orderId: String): Result<Unit> {
        val orderDoc = db.collection(COLLECTION_ORDERS).document(orderId)
        return try {
            orderDoc.update(
                mapOf(
                    "status" to OrderStatus.EXPIRED.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderDoc.path)
            Result.failure(e)
        }
    }

    suspend fun deletePendingOrExpiredOrder(orderId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_ORDERS).document(orderId)
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

    // --- ADMIN OPERATIONS ---

    fun observeAllOrders(): Flow<List<ExchangeOrder>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_ORDERS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ExchangeOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_ORDERS)
                    throw error
                }
        )
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_USERS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_USERS)
                    throw error
                }
        )
    }

    fun observeLedgerEntries(): Flow<List<LedgerEntry>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_LEDGER)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LedgerEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_LEDGER)
                    throw error
                }
        )
    }

    suspend fun updateExchangeConfig(config: ExchangeConfig): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_CONFIG).document(DOC_DEFAULT_CONFIG)
        val normalizedMode = if (config.gatewayMode.uppercase() == "LIVE") "LIVE" else "SANDBOX"
        val payload = mapOf(
            "usdToHtgBuy" to config.usdToHtgBuy,
            "usdToHtgSell" to config.usdToHtgSell,
            "eurToUsd" to config.eurToUsd,
            "feeNowpayments" to config.feeNowpayments,
            "feeBinance" to config.feeBinance,
            "feeMeru" to config.feeMeru,
            "feeMoncash" to config.feeMoncash,
            "feeNatcash" to config.feeNatcash,
            "minOrderUsd" to config.minOrderUsd,
            "maxOrderUsd" to config.maxOrderUsd,
            "moncashMerchant" to config.moncashMerchant.trim(),
            "natcashMerchant" to config.natcashMerchant.trim(),
            "binancePayId" to config.binancePayId.trim(),
            "meruTag" to config.meruTag.trim(),
            "backendBaseUrl" to config.backendBaseUrl.trim().take(300),
            "gatewayMode" to normalizedMode,
            "moncashOnlineEnabled" to config.moncashOnlineEnabled,
            "natcashOnlineEnabled" to config.natcashOnlineEnabled,
            "binanceOnlineEnabled" to config.binanceOnlineEnabled,
            "nowpaymentsOnlineEnabled" to config.nowpaymentsOnlineEnabled,
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun attachOnlineCheckoutSession(
        order: ExchangeOrder,
        gatewayMode: String,
        gatewayOrderId: String,
        checkoutUrl: String,
        payAddress: String = order.payAddress,
        payCryptoAmount: Double = order.payCryptoAmount,
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val orderDoc = db.collection(COLLECTION_ORDERS).document(order.id)
        val sessionId = "${order.id}_${order.sendMethod}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val sessionDoc = db.collection(COLLECTION_PAYMENT_SESSIONS).document(sessionId)
        val safeMode = when (gatewayMode.uppercase()) {
            "LIVE" -> "LIVE"
            "ONLINE" -> "ONLINE"
            else -> "SANDBOX"
        }
        return try {
            val batch = db.batch()
            batch.update(
                orderDoc,
                mapOf(
                    "gatewayMode" to "ONLINE",
                    "gatewayOrderId" to gatewayOrderId.take(160),
                    "checkoutUrl" to checkoutUrl.take(500),
                    "gatewayStatus" to GatewayStatus.WAITING_WEBHOOK.name,
                    "payAddress" to payAddress.take(200),
                    "payCryptoAmount" to payCryptoAmount.coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                sessionDoc,
                mapOf(
                    "userId" to uid,
                    "orderId" to order.id,
                    "orderCode" to order.orderCode,
                    "provider" to order.sendMethod,
                    "gatewayMode" to safeMode,
                    "gatewayOrderId" to gatewayOrderId.take(160),
                    "checkoutUrl" to checkoutUrl.take(500),
                    "amount" to order.sendAmount,
                    "currency" to order.sendCurrency,
                    "status" to "INITIATED",
                    "webhookVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sessionDoc.path)
            Result.failure(e)
        }
    }

    fun observeUserPaymentSessions(userId: String): Flow<List<PaymentSession>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_PAYMENT_SESSIONS)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaymentSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == userId) {
                        handleFlowError(error, OperationType.LIST, COLLECTION_PAYMENT_SESSIONS)
                    }
                    throw error
                }
        )
    }

    fun observeAllPaymentSessions(): Flow<List<PaymentSession>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_PAYMENT_SESSIONS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaymentSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_PAYMENT_SESSIONS)
                    throw error
                }
        )
    }

    fun observeWebhookEvents(): Flow<List<WebhookEvent>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_WEBHOOK_EVENTS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WebhookEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.receivedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_WEBHOOK_EVENTS)
                    throw error
                }
        )
    }

    suspend fun deletePaymentSession(sessionId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_PAYMENT_SESSIONS).document(sessionId)
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

    suspend fun deleteWebhookEvent(eventId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_WEBHOOK_EVENTS).document(eventId)
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

    suspend fun completeOrder(order: ExchangeOrder, adminNote: String): Result<Unit> {
        val orderDoc = db.collection(COLLECTION_ORDERS).document(order.id)
        val ledgerDoc = db.collection(COLLECTION_LEDGER).document()
        val isTopUp = order.orderType == "TOPUP" || order.sendMethod == order.receiveMethod
        val defaultNote = if (isTopUp) {
            "Recharge ${PaymentMethod.fromId(order.sendMethod).label} créditée (+${formatBalance(order.receiveAmount)} ${order.receiveCurrency})"
        } else {
            "Paiement envoyé au client"
        }
        val noteText = adminNote.trim().ifBlank { defaultNote }.take(200)
        return try {
            val batch = db.batch()
            batch.update(
                orderDoc,
                mapOf(
                    "status" to OrderStatus.COMPLETED.name,
                    "adminNote" to noteText,
                    "completedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            if (isTopUp && order.userId.isNotBlank()) {
                val method = PaymentMethod.fromId(order.sendMethod)
                val balanceField = ExchangeCalculator.balanceFieldFor(method)
                val userDoc = db.collection(COLLECTION_USERS).document(order.userId)
                batch.update(
                    userDoc,
                    mapOf(
                        balanceField to FieldValue.increment(order.receiveAmount),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                batch.set(
                    ledgerDoc,
                    mapOf(
                        "orderId" to order.id,
                        "orderCode" to order.orderCode,
                        "accountName" to "WALLET_${method.id.uppercase()}",
                        "entryType" to "CREDIT",
                        "currency" to order.receiveCurrency,
                        "amount" to order.receiveAmount,
                        "note" to noteText,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            } else {
                batch.set(
                    ledgerDoc,
                    mapOf(
                        "orderId" to order.id,
                        "orderCode" to order.orderCode,
                        "accountName" to "CAISSE_${order.receiveMethod.uppercase()}",
                        "entryType" to "DEBIT",
                        "currency" to order.receiveCurrency,
                        "amount" to order.receiveAmount,
                        "note" to noteText,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, orderDoc.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        adminNote: String,
    ): Result<Unit> {
        val orderDoc = db.collection(COLLECTION_ORDERS).document(orderId)
        return try {
            db.runTransaction { tx ->
                val orderSnap = tx.get(orderDoc)
                val currentStatus = orderSnap.getString("status").orEmpty()
                val gatewayMode = orderSnap.getString("gatewayMode").orEmpty()
                val orderType = orderSnap.getString("orderType").orEmpty()
                val userId = orderSnap.getString("userId").orEmpty()
                val sendMethodId = orderSnap.getString("sendMethod").orEmpty()
                val sendAmount = orderSnap.getDouble("sendAmount") ?: 0.0
                val sendCurrencyCode = orderSnap.getString("sendCurrency").orEmpty()

                val shouldRefund = newStatus == OrderStatus.REJECTED &&
                    gatewayMode == "WALLET" &&
                    orderType != "TOPUP" &&
                    currentStatus != OrderStatus.REJECTED.name &&
                    currentStatus != OrderStatus.COMPLETED.name &&
                    userId.isNotBlank() &&
                    sendAmount > 0.0

                val sendMethod = PaymentMethod.fromId(sendMethodId)
                val balanceField = ExchangeCalculator.balanceFieldFor(sendMethod)
                val userRef = if (shouldRefund) db.collection(COLLECTION_USERS).document(userId) else null
                val userSnap = if (userRef != null) tx.get(userRef) else null

                tx.update(
                    orderDoc,
                    mapOf(
                        "status" to newStatus.name,
                        "adminNote" to adminNote.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                // Refund wallet-debited EXCHANGE orders if Admin rejects them cleanly
                if (shouldRefund && userRef != null && userSnap != null && userSnap.exists()) {
                    val sendCurr = CurrencyCode.fromCode(sendCurrencyCode)
                    val refundAmt = if (sendMethod.defaultCurrency == sendCurr) {
                        sendAmount
                    } else {
                        val amountUsd = orderSnap.getDouble("amountUsd") ?: 0.0
                        val feePct = orderSnap.getDouble("feePercent") ?: 0.0
                        val grossUsd = if (feePct < 100.0) amountUsd / (1.0 - feePct / 100.0) else sendAmount
                        val htgNet = orderSnap.getDouble("amountHtg") ?: sendAmount
                        val grossHtg = if (feePct < 100.0) htgNet / (1.0 - feePct / 100.0) else htgNet
                        if (sendMethod.supportsHtg) grossHtg else grossUsd
                    }
                    val currentBal = userSnap.getDouble(balanceField) ?: 0.0
                    val nextBal = kotlin.math.round((currentBal + refundAmt) * 100.0) / 100.0
                    tx.update(
                        userRef,
                        mapOf(
                            balanceField to nextBal,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderDoc.path)
            Result.failure(e)
        }
    }

    suspend fun adminAdjustGatewayBalance(
        userId: String,
        method: PaymentMethod,
        newBalance: Double,
    ): Result<Unit> {
        val userDoc = db.collection(COLLECTION_USERS).document(userId)
        val safeBalance = kotlin.math.round(newBalance.coerceIn(0.0, 100_000_000.0) * 100.0) / 100.0
        val fieldName = ExchangeCalculator.balanceFieldFor(method)
        return try {
            userDoc.update(
                mapOf(
                    fieldName to safeBalance,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDoc.path)
            Result.failure(e)
        }
    }

    suspend fun toggleUserBlocked(userId: String, blocked: Boolean): Result<Unit> {
        val userDoc = db.collection(COLLECTION_USERS).document(userId)
        return try {
            userDoc.update(
                mapOf(
                    "isBlocked" to blocked,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDoc.path)
            Result.failure(e)
        }
    }

    fun observeUserAdminNotifications(userId: String): Flow<List<AdminNotification>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_ADMIN_NOTIFICATIONS)
                .whereIn("targetUserId", listOf("ALL", userId))
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (auth.currentUser?.uid == userId) {
                        handleFlowError(error, OperationType.LIST, COLLECTION_ADMIN_NOTIFICATIONS)
                    }
                    throw error
                }
        )
    }

    fun observeAllAdminNotifications(): Flow<List<AdminNotification>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_ADMIN_NOTIFICATIONS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_ADMIN_NOTIFICATIONS)
                    throw error
                }
        )
    }

    suspend fun sendAdminNotification(
        title: String,
        message: String,
        targetUserId: String,
        targetLabel: String,
        isAlert: Boolean,
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanTitle = title.trim()
        val cleanMessage = message.trim()
        if (cleanTitle.length < 2 || cleanMessage.length < 2) {
            return Result.failure(IllegalArgumentException("Titre et message requis (minimum 2 caractères)."))
        }
        val docRef = db.collection(COLLECTION_ADMIN_NOTIFICATIONS).document()
        val payload = mapOf(
            "title" to cleanTitle.take(120),
            "message" to cleanMessage.take(500),
            "targetUserId" to targetUserId.ifBlank { "ALL" }.take(128),
            "targetLabel" to targetLabel.ifBlank { "Tous les utilisateurs" }.take(160),
            "isAlert" to isAlert,
            "sentBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAdminNotification(notificationId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_ADMIN_NOTIFICATIONS).document(notificationId)
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

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_ORDERS).document(orderId)
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

    suspend fun deleteUserProfile(userId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_USERS).document(userId)
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

    suspend fun deleteLedgerEntry(entryId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_LEDGER).document(entryId)
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
        const val ADMIN_EMAIL = "josephsalem946@gmail.com"
        const val COLLECTION_CONFIG = "exchange_config"
        const val DOC_DEFAULT_CONFIG = "default"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_ORDERS = "orders"
        const val COLLECTION_TX_REFS = "tx_refs"
        const val COLLECTION_LEDGER = "ledger_entries"
        const val COLLECTION_ADMIN_NOTIFICATIONS = "admin_notifications"
        const val COLLECTION_PAYMENT_SESSIONS = "payment_sessions"
        const val COLLECTION_WEBHOOK_EVENTS = "webhook_events"
    }
}
