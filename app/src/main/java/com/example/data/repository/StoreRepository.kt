package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.domain.ExchangeCalculator
import com.example.data.model.CustomStoreProduct
import com.example.data.model.NetflixInventoryItem
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreCategory
import com.example.data.model.StoreConfig
import com.example.data.model.StoreDeliveryType
import com.example.data.model.StoreFulfillmentMode
import com.example.data.model.StoreOrder
import com.example.data.model.StoreOrderStatus
import com.example.data.model.StoreProduct
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import kotlin.math.ceil

class StoreRepository(
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

    fun observeStoreConfig(): Flow<StoreConfig> = flow {
        requireUserId()
        val path = "$COLLECTION_STORE_CONFIG/$DOC_DEFAULT_CONFIG"
        emitAll(
            db.collection(COLLECTION_STORE_CONFIG).document(DOC_DEFAULT_CONFIG)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            StoreConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: StoreConfig()
                    } else {
                        StoreConfig()
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeUserStoreOrders(userId: String): Flow<List<StoreOrder>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_STORE_ORDERS)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { query ->
                    query.documents
                        .mapNotNull { doc ->
                            doc.toObject(
                                StoreOrder::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_STORE_ORDERS)
                    throw error
                }
        )
    }

    fun observeAllStoreOrders(): Flow<List<StoreOrder>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_STORE_ORDERS)
                .snapshots()
                .map { query ->
                    query.documents
                        .mapNotNull { doc ->
                            doc.toObject(
                                StoreOrder::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_STORE_ORDERS)
                    throw error
                }
        )
    }

    fun observeNetflixInventory(): Flow<List<NetflixInventoryItem>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_NETFLIX_INVENTORY)
                .snapshots()
                .map { query ->
                    query.documents
                        .mapNotNull { doc ->
                            doc.toObject(
                                NetflixInventoryItem::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        }
                        .sortedBy { it.status }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_NETFLIX_INVENTORY)
                    throw error
                }
        )
    }

    fun observeCustomStoreProducts(): Flow<List<StoreProduct>> = flow {
        requireUserId()
        emitAll(
            db.collection(COLLECTION_STORE_PRODUCTS)
                .snapshots()
                .map { query ->
                    query.documents
                        .mapNotNull { doc ->
                            doc.toObject(
                                CustomStoreProduct::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )?.takeIf { it.title.isNotBlank() }?.toStoreProduct()
                        }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, COLLECTION_STORE_PRODUCTS)
                    throw error
                }
        )
    }

    suspend fun createStoreOrder(
        storeConfig: StoreConfig,
        userEmail: String,
        product: StoreProduct,
        gateway: PaymentMethod,
        targetAccount: String,
        selectedOption: String = "",
    ): Result<StoreOrder> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanTarget = targetAccount.trim()
        val cleanOption = selectedOption.trim()
        if (product.requiresTargetAccount && cleanTarget.length < 3) {
            return Result.failure(
                IllegalArgumentException("Veuillez renseigner : ${product.targetAccountLabel}.")
            )
        }
        val combinedTarget = when {
            cleanOption.isNotBlank() && cleanTarget.isNotBlank() -> "$cleanOption → $cleanTarget"
            cleanOption.isNotBlank() -> cleanOption
            else -> cleanTarget
        }.take(160)
        if (gateway == PaymentMethod.MERU || !storeConfig.isGatewayEnabled(gateway)) {
            return Result.failure(
                IllegalStateException("La passerelle ${gateway.label} est désactivée pour le Store.")
            )
        }

        val effectiveRate = storeConfig.effectiveRate()
        val amountHtg = ceil(product.usdSell * effectiveRate)
        val requiredDebit = if (gateway.supportsHtg) amountHtg else product.usdSell
        val balanceField = ExchangeCalculator.balanceFieldFor(gateway)
        val orderCode = "STK-" + (100000 + random.nextInt(900000))
        val userRef = db.collection(AleviniRepository.COLLECTION_USERS).document(uid)
        val docRef = db.collection(COLLECTION_STORE_ORDERS).document()

        val nowTime = SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date())
        val amountLabel = if (gateway.supportsHtg) {
            "${String.format(Locale.US, "%,.0f", requiredDebit)} HTG"
        } else {
            "$${String.format(Locale.US, "%.2f", requiredDebit)} USD"
        }
        val modeNote = if (storeConfig.isAutoFulfillment()) {
            "Mode Automatique actif : traitement fournisseur prioritaire."
        } else {
            "Mode Manuel actif : commande placée en file Régie Admin."
        }
        val initialLogs = listOf(
            "$nowTime — Débit ${gateway.label} confirmé ($amountLabel)",
            "$nowTime — $modeNote",
        )

        val payload = hashMapOf<String, Any?>(
            "userId" to uid,
            "userEmail" to userEmail.take(200),
            "orderCode" to orderCode,
            "productId" to product.id,
            "productTitle" to product.title,
            "category" to product.category.id,
            "deliveryType" to product.deliveryType.id,
            "usdCost" to product.usdCost,
            "usdSell" to product.usdSell,
            "amountHtg" to amountHtg,
            "effectiveRate" to effectiveRate,
            "gateway" to gateway.id,
            "targetAccount" to combinedTarget,
            "externalTxRef" to "",
            "gatewayOrderId" to "",
            "checkoutUrl" to "WALLET",
            "gatewayStatus" to "WALLET_DEBITED",
            "status" to StoreOrderStatus.PENDING.name,
            "deliverable" to "",
            "adminNote" to "",
            "logs" to initialLogs,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        return try {
            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) {
                    throw IllegalStateException("Rechargez d'abord votre compte ${gateway.label} avant de commander.")
                }
                if (userSnap.getBoolean("isBlocked") == true) {
                    throw IllegalStateException("Votre compte est bloqué par l'administrateur.")
                }
                val currentBalance = userSnap.getDouble(balanceField) ?: 0.0
                if (currentBalance + 1e-6 < requiredDebit) {
                    throw IllegalStateException(
                        "Solde ${gateway.label} insuffisant (${String.format(Locale.US, "%,.2f", currentBalance)} ${gateway.defaultCurrency.code} disponibles sur ${String.format(Locale.US, "%,.2f", requiredDebit)} ${gateway.defaultCurrency.code} requis). Rechargez votre compte ${gateway.label}."
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

            Result.success(
                StoreOrder(
                    id = docRef.id,
                    userId = uid,
                    userEmail = userEmail,
                    orderCode = orderCode,
                    productId = product.id,
                    productTitle = product.title,
                    category = product.category.id,
                    deliveryType = product.deliveryType.id,
                    usdCost = product.usdCost,
                    usdSell = product.usdSell,
                    amountHtg = amountHtg,
                    effectiveRate = effectiveRate,
                    gateway = gateway.id,
                    targetAccount = combinedTarget,
                    checkoutUrl = "WALLET",
                    gatewayStatus = "WALLET_DEBITED",
                    status = StoreOrderStatus.PENDING.name,
                    logs = initialLogs,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitStoreTxRef(orderId: String, rawRef: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val normalizedRef = rawRef.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        if (normalizedRef.length < 4) {
            return Result.failure(
                IllegalArgumentException("Référence de transaction invalide (min 4 caractères).")
            )
        }
        val orderRef = db.collection(COLLECTION_STORE_ORDERS).document(orderId)
        val txLockRef = db.collection(AleviniRepository.COLLECTION_TX_REFS).document("STK_$normalizedRef")

        return try {
            val batch = db.batch()
            batch.set(
                txLockRef,
                mapOf(
                    "userId" to uid,
                    "orderId" to orderId,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                orderRef,
                mapOf(
                    "externalTxRef" to rawRef.trim().take(120),
                    "status" to StoreOrderStatus.SUBMITTED.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun attachStoreOnlineCheckout(
        orderId: String,
        gatewayOrderId: String,
        checkoutUrl: String,
    ): Result<Unit> {
        val orderRef = db.collection(COLLECTION_STORE_ORDERS).document(orderId)
        return try {
            orderRef.update(
                mapOf(
                    "gatewayOrderId" to gatewayOrderId.take(160),
                    "checkoutUrl" to checkoutUrl.take(500),
                    "gatewayStatus" to "INITIATED",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePendingOrExpiredStoreOrder(orderId: String): Result<Unit> {
        val orderRef = db.collection(COLLECTION_STORE_ORDERS).document(orderId)
        return try {
            val snap = orderRef.get().await()
            if (!snap.exists()) return Result.success(Unit)
            val status = snap.getString("status").orEmpty()
            if (status != StoreOrderStatus.PENDING.name && status != StoreOrderStatus.EXPIRED.name) {
                return Result.failure(
                    IllegalStateException("Cet ordre Store est verrouillé et masqué localement.")
                )
            }
            orderRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminSaveStoreConfig(config: StoreConfig): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_STORE_CONFIG).document(DOC_DEFAULT_CONFIG)
        val payload = hashMapOf<String, Any?>(
            "baseRateHtg" to config.baseRateHtg.coerceIn(10.0, 9999.0),
            "markupPercent" to config.markupPercent.coerceIn(0.0, 50.0),
            "reloadlyBalanceUsd" to config.reloadlyBalanceUsd.coerceAtLeast(0.0),
            "meruBalanceUsd" to config.meruBalanceUsd.coerceAtLeast(0.0),
            "reloadlyActive" to config.reloadlyActive,
            "ezpinActive" to config.ezpinActive,
            "moncashActive" to config.moncashActive,
            "natcashActive" to config.natcashActive,
            "binanceActive" to config.binanceActive,
            "nowpaymentsActive" to config.nowpaymentsActive,
            "reloadlyLatencyMs" to config.reloadlyLatencyMs.coerceIn(0, 60000),
            "ezpinLatencyMs" to config.ezpinLatencyMs.coerceIn(0, 60000),
            "availableNetflixCount" to config.availableNetflixCount.coerceAtLeast(0),
            "pendingStarlinkCount" to config.pendingStarlinkCount.coerceAtLeast(0),
            "fulfillmentMode" to StoreFulfillmentMode.fromId(config.fulfillmentMode).id,
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

    suspend fun adminAddStoreProduct(
        category: StoreCategory,
        deliveryType: StoreDeliveryType,
        badge: String,
        title: String,
        subtitle: String,
        usdCost: Double,
        usdSell: Double,
        targetAccountLabel: String,
    ): Result<StoreProduct> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanTitle = title.trim()
        val cleanSub = subtitle.trim().ifBlank { "Service digital vérifié • Livraison rapide" }
        val cleanBadge = badge.trim().ifBlank { category.label.uppercase() }
        val cleanLabel = targetAccountLabel.trim().ifBlank { "Email ou Identifiant de livraison" }
        if (cleanTitle.length < 2 || usdSell <= 0.0) {
            return Result.failure(IllegalArgumentException("Titre (min 2 car.) et prix USD (> 0) requis."))
        }
        val safeSell = usdSell.coerceIn(0.5, 10000.0)
        val safeCost = usdCost.coerceIn(0.1, safeSell)
        val docRef = db.collection(COLLECTION_STORE_PRODUCTS).document()
        val payload = hashMapOf<String, Any?>(
            "category" to category.id,
            "deliveryType" to deliveryType.id,
            "badge" to cleanBadge.take(48),
            "title" to cleanTitle.take(160),
            "subtitle" to cleanSub.take(240),
            "usdCost" to safeCost,
            "usdSell" to safeSell,
            "requiresTargetAccount" to true,
            "targetAccountLabel" to cleanLabel.take(120),
            "targetAccountHint" to "Ex: client@email.com / +509...",
            "selectLabel" to "",
            "selectOptions" to emptyList<String>(),
            "stock" to 100,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.set(payload).await()
            Result.success(
                StoreProduct(
                    id = docRef.id,
                    category = category,
                    deliveryType = deliveryType,
                    badge = cleanBadge.take(48),
                    title = cleanTitle.take(160),
                    subtitle = cleanSub.take(240),
                    usdCost = safeCost,
                    usdSell = safeSell,
                    requiresTargetAccount = true,
                    targetAccountLabel = cleanLabel.take(120),
                    targetAccountHint = "Ex: client@email.com / +509...",
                    stock = 100,
                    isCustom = true,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteStoreProduct(productId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_STORE_PRODUCTS).document(productId)
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

    suspend fun adminAddNetflixProfile(
        email: String,
        password: String,
        profileName: String,
        pin: String,
        currentConfig: StoreConfig,
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanProfile = profileName.trim()
        val cleanPin = pin.trim()
        if (cleanEmail.length < 3 || cleanPassword.isBlank() || cleanProfile.isBlank() || cleanPin.length < 4) {
            return Result.failure(
                IllegalArgumentException("Email, mot de passe, nom de profil et PIN (min 4 chiffres) requis.")
            )
        }

        val itemRef = db.collection(COLLECTION_NETFLIX_INVENTORY).document()
        val configRef = db.collection(COLLECTION_STORE_CONFIG).document(DOC_DEFAULT_CONFIG)
        return try {
            val batch = db.batch()
            batch.set(
                itemRef,
                mapOf(
                    "accountEmail" to cleanEmail.take(200),
                    "accountPassword" to cleanPassword.take(120),
                    "profileName" to cleanProfile.take(60),
                    "profilePin" to cleanPin.take(12),
                    "status" to "AVAILABLE",
                    "assignedOrderId" to "",
                    "assignedUserId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                configRef,
                storeConfigMap(
                    currentConfig.copy(availableNetflixCount = currentConfig.availableNetflixCount + 1),
                    uid,
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, itemRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteNetflixProfile(
        item: NetflixInventoryItem,
        currentConfig: StoreConfig,
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val itemRef = db.collection(COLLECTION_NETFLIX_INVENTORY).document(item.id)
        val configRef = db.collection(COLLECTION_STORE_CONFIG).document(DOC_DEFAULT_CONFIG)
        return try {
            val batch = db.batch()
            batch.delete(itemRef)
            if (item.status == "AVAILABLE" && currentConfig.availableNetflixCount > 0) {
                batch.set(
                    configRef,
                    storeConfigMap(
                        currentConfig.copy(
                            availableNetflixCount = (currentConfig.availableNetflixCount - 1).coerceAtLeast(0)
                        ),
                        uid,
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, itemRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminDeliverNetflixOrderFromStock(
        order: StoreOrder,
        inventoryItem: NetflixInventoryItem,
        currentConfig: StoreConfig,
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val deliverable = "Email: ${inventoryItem.accountEmail} | Pass: ${inventoryItem.accountPassword} | Profil: ${inventoryItem.profileName} | PIN: ${inventoryItem.profilePin}"
        val orderRef = db.collection(COLLECTION_STORE_ORDERS).document(order.id)
        val stockRef = db.collection(COLLECTION_NETFLIX_INVENTORY).document(inventoryItem.id)
        val configRef = db.collection(COLLECTION_STORE_CONFIG).document(DOC_DEFAULT_CONFIG)
        val notifRef = db.collection(AleviniRepository.COLLECTION_ADMIN_NOTIFICATIONS).document()

        return try {
            val batch = db.batch()
            batch.update(
                stockRef,
                mapOf(
                    "status" to "OCCUPIED",
                    "assignedOrderId" to order.id,
                    "assignedUserId" to order.userId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                orderRef,
                mapOf(
                    "status" to StoreOrderStatus.DELIVERED.name,
                    "deliverable" to deliverable.take(500),
                    "adminNote" to "Profil Netflix attribué automatiquement",
                    "logs" to appendOrderLog(order.logs, "Profil Netflix attribué & livré par Admin."),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                configRef,
                storeConfigMap(
                    currentConfig.copy(
                        availableNetflixCount = (currentConfig.availableNetflixCount - 1).coerceAtLeast(0)
                    ),
                    uid,
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "title" to "Store • ${order.orderCode} livré",
                    "message" to "Vos identifiants ${order.productTitle} sont prêts dans Store > Mes Commandes.",
                    "targetUserId" to order.userId,
                    "targetLabel" to order.userEmail.ifBlank { order.userId },
                    "isAlert" to false,
                    "sentBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminFulfillStarlinkViaMeru(
        order: StoreOrder,
        receiptCode: String,
        currentConfig: StoreConfig,
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (currentConfig.meruBalanceUsd < order.usdCost) {
            return Result.failure(
                IllegalStateException(
                    "Solde Carte Meru insuffisant ($${currentConfig.meruBalanceUsd} < $${order.usdCost}). Rechargez Meru P2P."
                )
            )
        }
        val finalReceipt = receiptCode.trim().ifBlank {
            "#STL-" + (100000 + random.nextInt(900000))
        }
        val orderRef = db.collection(COLLECTION_STORE_ORDERS).document(order.id)
        val configRef = db.collection(COLLECTION_STORE_CONFIG).document(DOC_DEFAULT_CONFIG)
        val notifRef = db.collection(AleviniRepository.COLLECTION_ADMIN_NOTIFICATIONS).document()

        return try {
            val batch = db.batch()
            batch.update(
                orderRef,
                mapOf(
                    "status" to StoreOrderStatus.DELIVERED.name,
                    "deliverable" to "Facture réglée (Reçu Meru VCC: $finalReceipt)",
                    "adminNote" to "Payé $${order.usdCost} via Carte Virtuelle Meru",
                    "logs" to appendOrderLog(order.logs, "Paiement Carte Meru VCC exécuté ($finalReceipt)."),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                configRef,
                storeConfigMap(
                    currentConfig.copy(
                        meruBalanceUsd = (currentConfig.meruBalanceUsd - order.usdCost).coerceAtLeast(0.0),
                        pendingStarlinkCount = (currentConfig.pendingStarlinkCount - 1).coerceAtLeast(0),
                    ),
                    uid,
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "title" to "Service Meru VCC livré • ${order.orderCode}",
                    "message" to "${order.productTitle} réglé (${order.targetAccount}). Reçu : $finalReceipt",
                    "targetUserId" to order.userId,
                    "targetLabel" to order.userEmail.ifBlank { order.userId },
                    "isAlert" to false,
                    "sentBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminDeliverStoreOrder(
        order: StoreOrder,
        deliverableText: String,
        adminNote: String,
        currentConfig: StoreConfig,
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanDeliverable = deliverableText.trim()
        if (cleanDeliverable.length < 3) {
            return Result.failure(IllegalArgumentException("Contenu livré ou référence requis."))
        }
        val orderRef = db.collection(COLLECTION_STORE_ORDERS).document(order.id)
        val configRef = db.collection(COLLECTION_STORE_CONFIG).document(DOC_DEFAULT_CONFIG)
        val notifRef = db.collection(AleviniRepository.COLLECTION_ADMIN_NOTIFICATIONS).document()

        return try {
            val batch = db.batch()
            batch.update(
                orderRef,
                mapOf(
                    "status" to StoreOrderStatus.DELIVERED.name,
                    "deliverable" to cleanDeliverable.take(500),
                    "adminNote" to adminNote.trim().take(300),
                    "logs" to appendOrderLog(order.logs, "Validé & livré manuellement par Admin."),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            val isReloadlyTopUp = order.deliveryType == StoreDeliveryType.TOPUP.id
            if (isReloadlyTopUp || order.deliveryType == StoreDeliveryType.STARLINK.id) {
                batch.set(
                    configRef,
                    storeConfigMap(
                        currentConfig.copy(
                            reloadlyBalanceUsd = if (isReloadlyTopUp) {
                                (currentConfig.reloadlyBalanceUsd - order.usdCost).coerceAtLeast(0.0)
                            } else {
                                currentConfig.reloadlyBalanceUsd
                            },
                            pendingStarlinkCount = if (order.deliveryType == StoreDeliveryType.STARLINK.id) {
                                (currentConfig.pendingStarlinkCount - 1).coerceAtLeast(0)
                            } else {
                                currentConfig.pendingStarlinkCount
                            },
                        ),
                        uid,
                    )
                )
            }
            batch.set(
                notifRef,
                mapOf(
                    "title" to "Store • ${order.orderCode} livré",
                    "message" to "Votre article ${order.productTitle} est disponible dans le coffre-fort Store.",
                    "targetUserId" to order.userId,
                    "targetLabel" to order.userEmail.ifBlank { order.userId },
                    "isAlert" to false,
                    "sentBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminRetryStoreOrderApi(order: StoreOrder): Result<Unit> {
        val orderRef = db.collection(COLLECTION_STORE_ORDERS).document(order.id)
        val nextStatus = if (
            order.status == StoreOrderStatus.PENDING.name ||
            order.status == StoreOrderStatus.SUBMITTED.name
        ) {
            StoreOrderStatus.PAID.name
        } else {
            order.status
        }
        return try {
            orderRef.update(
                mapOf(
                    "status" to nextStatus,
                    "adminNote" to "Relance API / traitement en cours par Admin",
                    "logs" to appendOrderLog(order.logs, "Relance API / vérification déclenchée par Admin."),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminRejectStoreOrder(order: StoreOrder, note: String): Result<Unit> {
        val orderRef = db.collection(COLLECTION_STORE_ORDERS).document(order.id)
        return try {
            db.runTransaction { tx ->
                val orderSnap = tx.get(orderRef)
                val currentStatus = orderSnap.getString("status").orEmpty()
                val gwStatus = orderSnap.getString("gatewayStatus").orEmpty()
                val shouldRefund = gwStatus == "WALLET_DEBITED" &&
                    currentStatus != StoreOrderStatus.REJECTED.name &&
                    currentStatus != StoreOrderStatus.DELIVERED.name &&
                    order.userId.isNotBlank()
                val gw = PaymentMethod.fromId(order.gateway)
                val balanceField = ExchangeCalculator.balanceFieldFor(gw)
                val userRef = if (shouldRefund) {
                    db.collection(AleviniRepository.COLLECTION_USERS).document(order.userId)
                } else {
                    null
                }
                val userSnap = if (userRef != null) tx.get(userRef) else null

                tx.update(
                    orderRef,
                    mapOf(
                        "status" to StoreOrderStatus.REJECTED.name,
                        "adminNote" to note.trim().take(300),
                        "logs" to appendOrderLog(
                            order.logs,
                            if (shouldRefund) "Refusé par Admin • Solde ${gw.label} remboursé." else "Refusé par Admin."
                        ),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                if (shouldRefund && userRef != null && userSnap != null && userSnap.exists()) {
                    val refundAmount = if (gw.supportsHtg) order.amountHtg else order.usdSell
                    val currentBal = userSnap.getDouble(balanceField) ?: 0.0
                    val nextBal = kotlin.math.round((currentBal + refundAmount) * 100.0) / 100.0
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
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteStoreOrder(orderId: String): Result<Unit> {
        val orderRef = db.collection(COLLECTION_STORE_ORDERS).document(orderId)
        return try {
            orderRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, orderRef.path)
            Result.failure(e)
        }
    }

    private fun appendOrderLog(existing: List<String>, message: String): List<String> {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date())
        return (existing + "$ts — $message").takeLast(25)
    }

    private fun storeConfigMap(config: StoreConfig, uid: String): Map<String, Any?> = mapOf(
        "baseRateHtg" to config.baseRateHtg,
        "markupPercent" to config.markupPercent,
        "reloadlyBalanceUsd" to config.reloadlyBalanceUsd,
        "meruBalanceUsd" to config.meruBalanceUsd,
        "reloadlyActive" to config.reloadlyActive,
        "ezpinActive" to config.ezpinActive,
        "moncashActive" to config.moncashActive,
        "natcashActive" to config.natcashActive,
        "binanceActive" to config.binanceActive,
        "nowpaymentsActive" to config.nowpaymentsActive,
        "reloadlyLatencyMs" to config.reloadlyLatencyMs,
        "ezpinLatencyMs" to config.ezpinLatencyMs,
        "availableNetflixCount" to config.availableNetflixCount,
        "pendingStarlinkCount" to config.pendingStarlinkCount,
        "fulfillmentMode" to StoreFulfillmentMode.fromId(config.fulfillmentMode).id,
        "updatedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp(),
    )

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
        const val COLLECTION_STORE_CONFIG = "store_config"
        const val DOC_DEFAULT_CONFIG = "default"
        const val COLLECTION_STORE_PRODUCTS = "store_products"
        const val COLLECTION_STORE_ORDERS = "store_orders"
        const val COLLECTION_NETFLIX_INVENTORY = "store_netflix_inventory"
    }
}
