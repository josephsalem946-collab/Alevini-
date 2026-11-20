package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ExchangeConfig
import com.example.data.model.NetflixInventoryItem
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreCatalog
import com.example.data.model.StoreCategory
import com.example.data.model.StoreConfig
import com.example.data.model.StoreDeliveryType
import com.example.data.model.StoreFulfillmentMode
import com.example.data.model.StoreOrder
import com.example.data.model.StoreOrderStatus
import com.example.data.model.StoreProduct
import com.example.data.model.StoreProviderHealth
import com.example.data.remote.PaymentGatewayClient
import com.example.data.repository.AleviniRepository
import com.example.data.repository.SecurityPreferencesRepository
import com.example.data.repository.StoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoreViewModel(
    private val repository: StoreRepository,
    private val securityPrefs: SecurityPreferencesRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    isEmailVerified: Boolean,
    private val gatewayClient: PaymentGatewayClient = PaymentGatewayClient(),
) : ViewModel() {

    val isAdmin: Boolean =
        currentUserEmail.equals(AleviniRepository.ADMIN_EMAIL, ignoreCase = true) && isEmailVerified

    val storeConfig: StateFlow<StoreConfig> = repository.observeStoreConfig()
        .catch { error ->
            unwrapAndLog(error, "store_config")
            emit(StoreConfig())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StoreConfig(),
        )

    val catalogProducts: StateFlow<List<StoreProduct>> = repository.observeCustomStoreProducts()
        .map { custom -> StoreCatalog.mergeCatalog(custom) }
        .catch { error ->
            unwrapAndLog(error, "store_products")
            emit(StoreCatalog.DEFAULT_PRODUCTS)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StoreCatalog.DEFAULT_PRODUCTS,
        )

    val userStoreOrdersState: StateFlow<UiState<List<StoreOrder>>> = repository.observeUserStoreOrders(currentUserId)
        .map<List<StoreOrder>, UiState<List<StoreOrder>>> { UiState.Success(it) }
        .catch { error ->
            val msg = unwrapAndLog(error, "user_store_orders")
            emit(UiState.Error(msg))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val adminStoreOrdersState: StateFlow<UiState<List<StoreOrder>>> = if (isAdmin) {
        repository.observeAllStoreOrders()
            .map<List<StoreOrder>, UiState<List<StoreOrder>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "admin_store_orders")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    } else {
        flowOf(UiState.Success(emptyList<StoreOrder>())).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList()),
        )
    }

    val netflixInventoryState: StateFlow<UiState<List<NetflixInventoryItem>>> = if (isAdmin) {
        repository.observeNetflixInventory()
            .map<List<NetflixInventoryItem>, UiState<List<NetflixInventoryItem>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "netflix_inventory")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    } else {
        flowOf(UiState.Success(emptyList<NetflixInventoryItem>())).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList()),
        )
    }

    val dismissedStoreOrderIds: StateFlow<Set<String>> = securityPrefs.dismissedStoreOrderIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet(),
        )

    private val _selectedProductId = MutableStateFlow(StoreCatalog.DEFAULT_PRODUCTS.first().id)
    val selectedProductId: StateFlow<String> = _selectedProductId.asStateFlow()

    private val _selectedGateway = MutableStateFlow(PaymentMethod.MONCASH)
    val selectedGateway: StateFlow<PaymentMethod> = _selectedGateway.asStateFlow()

    private val _targetAccountInput = MutableStateFlow("")
    val targetAccountInput: StateFlow<String> = _targetAccountInput.asStateFlow()

    private val _selectedOptionInput = MutableStateFlow("")
    val selectedOptionInput: StateFlow<String> = _selectedOptionInput.asStateFlow()

    private val _selectedStoreOrder = MutableStateFlow<StoreOrder?>(null)
    val selectedStoreOrder: StateFlow<StoreOrder?> = _selectedStoreOrder.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isGatewayBusy = MutableStateFlow(false)
    val isGatewayBusy: StateFlow<Boolean> = _isGatewayBusy.asStateFlow()

    private val _providersHealth = MutableStateFlow<List<StoreProviderHealth>>(emptyList())
    val providersHealth: StateFlow<List<StoreProviderHealth>> = _providersHealth.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    fun selectProduct(product: StoreProduct) {
        _selectedProductId.value = product.id
        _targetAccountInput.value = ""
        _selectedOptionInput.value = product.selectOptions.firstOrNull().orEmpty()
    }

    fun selectCategoryDefault(category: StoreCategory) {
        val catalog = catalogProducts.value
        val current = StoreCatalog.findById(_selectedProductId.value, catalog)
        if (current?.category != category) {
            val firstInCategory = catalog.firstOrNull { it.category == category }
            if (firstInCategory != null) {
                selectProduct(firstInCategory)
            }
        }
    }

    fun selectGateway(gateway: PaymentMethod) {
        _selectedGateway.value = gateway
    }

    fun setTargetAccountInput(value: String) {
        _targetAccountInput.value = value.take(160)
    }

    fun setSelectedOptionInput(value: String) {
        _selectedOptionInput.value = value.take(120)
    }

    fun openStoreOrder(order: StoreOrder) {
        _selectedStoreOrder.value = order
    }

    fun closeStoreOrder() {
        _selectedStoreOrder.value = null
    }

    fun createStoreOrder(onCreated: (StoreOrder) -> Unit = {}) {
        val product = StoreCatalog.findById(_selectedProductId.value, catalogProducts.value) ?: return
        val gateway = _selectedGateway.value
        val cfg = storeConfig.value

        if (!cfg.isGatewayEnabled(gateway)) {
            _snackbarEvents.tryEmit("${gateway.label} est temporairement désactivé par l'Admin.")
            return
        }
        val target = _targetAccountInput.value.trim()
        if (product.requiresTargetAccount && target.length < 3) {
            _snackbarEvents.tryEmit("Renseignez : ${product.targetAccountLabel}")
            return
        }
        val selectedOption = _selectedOptionInput.value.ifBlank {
            product.selectOptions.firstOrNull().orEmpty()
        }

        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val result = repository.createStoreOrder(
                    storeConfig = cfg,
                    userEmail = currentUserEmail,
                    product = product,
                    gateway = gateway,
                    targetAccount = target,
                    selectedOption = selectedOption,
                )
                result.onSuccess { order ->
                    _targetAccountInput.value = ""
                    _selectedStoreOrder.value = order
                    _snackbarEvents.tryEmit("Commande ${order.orderCode} créée.")
                    onCreated(order)
                }.onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Impossible de créer la commande Store.")
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun submitStoreTxRef(orderId: String, txRef: String) {
        viewModelScope.launch {
            repository.submitStoreTxRef(orderId, txRef)
                .onSuccess {
                    _snackbarEvents.tryEmit("Preuve transmise pour vérification & livraison.")
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Référence déjà utilisée ou invalide.")
                }
        }
    }

    fun initiateStoreOnlinePayment(order: StoreOrder, exchangeConfig: ExchangeConfig) {
        val provider = PaymentMethod.fromId(order.gateway)
        val backendUrl = exchangeConfig.backendBaseUrl.trim()
        if (backendUrl.isBlank()) {
            _snackbarEvents.tryEmit("URL serveur backend non configurée dans Admin > Passerelles API.")
            return
        }
        viewModelScope.launch {
            _isGatewayBusy.value = true
            try {
                val idToken = repository.getFreshIdToken().orEmpty()
                if (idToken.isBlank()) {
                    _snackbarEvents.tryEmit("Session Google expirée. Veuillez vous reconnecter.")
                    return@launch
                }
                val response = gatewayClient.initiateStoreCheckout(
                    backendBaseUrl = backendUrl,
                    firebaseIdToken = idToken,
                    orderId = order.id,
                    provider = provider,
                )
                response.onSuccess { checkout ->
                    repository.attachStoreOnlineCheckout(
                        orderId = order.id,
                        gatewayOrderId = checkout.gatewayOrderId,
                        checkoutUrl = checkout.checkoutUrl,
                    )
                    _selectedStoreOrder.value = order.copy(
                        gatewayOrderId = checkout.gatewayOrderId,
                        checkoutUrl = checkout.checkoutUrl,
                        gatewayStatus = "INITIATED",
                    )
                    _snackbarEvents.tryEmit("Session ${provider.label} (${checkout.mode}) prête.")
                }.onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Erreur passerelle Store.")
                }
            } finally {
                _isGatewayBusy.value = false
            }
        }
    }

    fun checkStoreOnlinePaymentStatus(order: StoreOrder, exchangeConfig: ExchangeConfig) {
        val backendUrl = exchangeConfig.backendBaseUrl.trim()
        if (backendUrl.isBlank()) {
            _snackbarEvents.tryEmit("URL serveur backend non configurée dans Admin > Passerelles API.")
            return
        }
        viewModelScope.launch {
            _isGatewayBusy.value = true
            try {
                val idToken = repository.getFreshIdToken().orEmpty()
                if (idToken.isBlank()) {
                    _snackbarEvents.tryEmit("Session Google expirée. Veuillez vous reconnecter.")
                    return@launch
                }
                gatewayClient.checkStoreOrderStatus(
                    backendBaseUrl = backendUrl,
                    firebaseIdToken = idToken,
                    orderId = order.id,
                ).onSuccess { st ->
                    _snackbarEvents.tryEmit("Statut Store : ${st.status} (${st.gatewayStatus})")
                }.onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Impossible de vérifier le statut.")
                }
            } finally {
                _isGatewayBusy.value = false
            }
        }
    }

    fun deleteOrHideStoreOrder(order: StoreOrder) {
        viewModelScope.launch {
            val status = StoreOrderStatus.fromString(order.status)
            if (status == StoreOrderStatus.PENDING || status == StoreOrderStatus.EXPIRED) {
                repository.deletePendingOrExpiredStoreOrder(order.id)
                    .onSuccess { _snackbarEvents.tryEmit("Commande ${order.orderCode} supprimée.") }
                    .onFailure {
                        securityPrefs.dismissStoreOrder(order.id)
                        _snackbarEvents.tryEmit("Commande masquée de votre liste.")
                    }
            } else {
                securityPrefs.dismissStoreOrder(order.id)
                _snackbarEvents.tryEmit("Commande ${order.orderCode} retirée de votre historique.")
            }
        }
    }

    // --- Admin Actions ---
    fun adminSaveStoreConfig(config: StoreConfig, successMsg: String = "Configuration Store mise à jour.") {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminSaveStoreConfig(config)
                .onSuccess { _snackbarEvents.tryEmit(successMsg) }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur mise à jour Store.") }
        }
    }

    fun adminTopUpMeruUsd(deltaUsd: Double = 100.0) {
        val current = storeConfig.value
        adminSaveStoreConfig(
            current.copy(meruBalanceUsd = (current.meruBalanceUsd + deltaUsd).coerceAtLeast(0.0)),
            "Carte Meru rechargée (+$${deltaUsd.toInt()} USD P2P).",
        )
    }

    fun adminTransferMeruToReloadly(amountUsd: Double = 50.0) {
        val current = storeConfig.value
        if (current.meruBalanceUsd < amountUsd) {
            _snackbarEvents.tryEmit("Solde Carte Meru insuffisant pour transférer $$amountUsd.")
            return
        }
        adminSaveStoreConfig(
            current.copy(
                meruBalanceUsd = current.meruBalanceUsd - amountUsd,
                reloadlyBalanceUsd = current.reloadlyBalanceUsd + amountUsd,
            ),
            "$${amountUsd.toInt()} transférés : Carte Meru -> API Reloadly.",
        )
    }

    fun adminTestAllProviders(exchangeConfig: ExchangeConfig) {
        if (!isAdmin) return
        val backendUrl = exchangeConfig.backendBaseUrl.trim()
        if (backendUrl.isBlank()) {
            _snackbarEvents.tryEmit("Renseignez d'abord l'URL HTTPS du serveur dans Passerelles API.")
            return
        }
        viewModelScope.launch {
            _isGatewayBusy.value = true
            try {
                val idToken = repository.getFreshIdToken().orEmpty()
                if (idToken.isBlank()) {
                    _snackbarEvents.tryEmit("Session Google expirée. Veuillez vous reconnecter.")
                    return@launch
                }
                gatewayClient.checkStoreProvidersHealth(
                    backendBaseUrl = backendUrl,
                    firebaseIdToken = idToken,
                ).onSuccess { list ->
                    _providersHealth.value = list
                    val reloadlyMs = list.firstOrNull { it.provider == "reloadly" }?.latencyMs
                        ?: storeConfig.value.reloadlyLatencyMs
                    val ezpinMs = list.firstOrNull { it.provider == "ezpin" }?.latencyMs
                        ?: storeConfig.value.ezpinLatencyMs
                    repository.adminSaveStoreConfig(
                        storeConfig.value.copy(
                            reloadlyLatencyMs = reloadlyMs,
                            ezpinLatencyMs = ezpinMs,
                        )
                    )
                    _snackbarEvents.tryEmit("Audit API terminé (${list.count { it.configured }}/${list.size} configurées).")
                }.onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Impossible de joindre le backend /health.")
                }
            } finally {
                _isGatewayBusy.value = false
            }
        }
    }

    fun adminAddNetflixProfile(
        email: String,
        password: String,
        profileName: String,
        pin: String,
        onSuccess: () -> Unit,
    ) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminAddNetflixProfile(email, password, profileName, pin, storeConfig.value)
                .onSuccess {
                    _snackbarEvents.tryEmit("Profil Netflix ajouté au coffre-fort.")
                    onSuccess()
                }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur ajout profil.") }
        }
    }

    fun adminDeleteNetflixProfile(item: NetflixInventoryItem) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminDeleteNetflixProfile(item, storeConfig.value)
                .onSuccess { _snackbarEvents.tryEmit("Profil supprimé de l'inventaire.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur suppression profil.") }
        }
    }

    fun adminAssignNetflixAndDeliver(order: StoreOrder) {
        if (!isAdmin) return
        val inventory = (netflixInventoryState.value as? UiState.Success)?.data.orEmpty()
        val available = inventory.firstOrNull { it.status == "AVAILABLE" }
        if (available == null) {
            _snackbarEvents.tryEmit("Stock Netflix épuisé ! Ajoutez un profil dans l'inventaire ci-dessous.")
            return
        }
        viewModelScope.launch {
            repository.adminDeliverNetflixOrderFromStock(order, available, storeConfig.value)
                .onSuccess { _snackbarEvents.tryEmit("Profil Netflix attribué & livré à ${order.orderCode}.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur livraison Netflix.") }
        }
    }

    fun adminFulfillStarlinkViaMeru(order: StoreOrder, receiptCode: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminFulfillStarlinkViaMeru(order, receiptCode, storeConfig.value)
                .onSuccess { _snackbarEvents.tryEmit("Facture Starlink ${order.orderCode} réglée via Meru.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur règlement Starlink.") }
        }
    }

    fun adminDeliverStoreOrder(order: StoreOrder, deliverableText: String, adminNote: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminDeliverStoreOrder(order, deliverableText, adminNote, storeConfig.value)
                .onSuccess { _snackbarEvents.tryEmit("Commande Store ${order.orderCode} livrée.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur livraison Store.") }
        }
    }

    fun adminRejectStoreOrder(order: StoreOrder, note: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminRejectStoreOrder(order, note)
                .onSuccess { _snackbarEvents.tryEmit("Commande Store ${order.orderCode} refusée.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur refus Store.") }
        }
    }

    fun adminSetFulfillmentMode(mode: StoreFulfillmentMode) {
        if (!isAdmin) return
        adminSaveStoreConfig(
            storeConfig.value.copy(fulfillmentMode = mode.id),
            "Mode basculé sur : ${mode.label}.",
        )
    }

    fun adminRetryStoreOrderApi(order: StoreOrder) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminRetryStoreOrderApi(order)
                .onSuccess { _snackbarEvents.tryEmit("API relancée pour ${order.orderCode}.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur relance API.") }
        }
    }

    fun adminAddStoreProduct(
        category: StoreCategory,
        deliveryType: StoreDeliveryType,
        badge: String,
        title: String,
        subtitle: String,
        usdCost: Double,
        usdSell: Double,
        targetAccountLabel: String,
        onSuccess: () -> Unit,
    ) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminAddStoreProduct(
                category = category,
                deliveryType = deliveryType,
                badge = badge,
                title = title,
                subtitle = subtitle,
                usdCost = usdCost,
                usdSell = usdSell,
                targetAccountLabel = targetAccountLabel,
            ).onSuccess { created ->
                _snackbarEvents.tryEmit("\"${created.title}\" publié dans le Store !")
                onSuccess()
            }.onFailure {
                _snackbarEvents.tryEmit(it.message ?: "Erreur ajout produit Store.")
            }
        }
    }

    fun adminDeleteStoreProduct(productId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminDeleteStoreProduct(productId)
                .onSuccess { _snackbarEvents.tryEmit("Produit retiré du catalogue Store.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur suppression produit.") }
        }
    }

    fun adminDeleteStoreOrder(orderId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminDeleteStoreOrder(orderId)
                .onSuccess { _snackbarEvents.tryEmit("Commande Store supprimée.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur suppression Store.") }
        }
    }

    private fun unwrapAndLog(error: Throwable, tag: String): String {
        if (error is CancellationException) throw error
        val fsError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        Log.w(TAG, "Firestore flow error [$tag]", error)
        return when (fsError?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Accès Store refusé (règles de sécurité)."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Service indisponible hors ligne."
            else -> error.message ?: "Erreur de chargement Store."
        }
    }

    private companion object {
        const val TAG = "StoreViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
