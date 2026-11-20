package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.domain.AppLanguage
import com.example.data.domain.ExchangeCalculator
import com.example.data.domain.PinSecurityHelper
import com.example.data.model.AdminNotification
import com.example.data.model.CryptoCoin
import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeCalculation
import com.example.data.model.ExchangeConfig
import com.example.data.model.ExchangeOrder
import com.example.data.model.GatewayWalletItem
import com.example.data.model.LedgerEntry
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentSession
import com.example.data.model.UserProfile
import com.example.data.model.WebhookEvent
import com.example.data.remote.PaymentGatewayClient
import com.example.data.repository.AleviniRepository
import com.example.data.repository.PinSecurityState
import com.example.data.repository.PinVerifyResult
import com.example.data.repository.SecurityPreferencesRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CalculatorUiState(
    val sendMethod: PaymentMethod = PaymentMethod.MONCASH,
    val receiveMethod: PaymentMethod = PaymentMethod.BINANCE,
    val sendCurrency: CurrencyCode = CurrencyCode.HTG,
    val receiveCurrency: CurrencyCode = CurrencyCode.USD,
    val cryptoCoin: CryptoCoin = CryptoCoin.USDT_TRC20,
    val sendAmountText: String = "13200",
    val destAccount: String = "",
    val isSubmittingOrder: Boolean = false,
)

data class ComputedQuote(
    val calculation: ExchangeCalculation? = null,
    val errorMessage: String? = null,
)

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timeLabel: String,
    val orderId: String? = null,
    val isAlert: Boolean = false,
)

data class WalletSummary(
    val gateways: List<GatewayWalletItem> = PaymentMethod.entries.map { method ->
        GatewayWalletItem(
            method = method,
            balance = 0.0,
            currency = method.defaultCurrency,
            feePercent = 0.0,
            totalTopUps = 0.0,
            totalSpent = 0.0,
        )
    },
    val pendingOrdersCount: Int = 0,
    val completedOrdersCount: Int = 0,
) {
    fun gatewayFor(method: PaymentMethod): GatewayWalletItem? =
        gateways.firstOrNull { it.method == method }
}

class AleviniViewModel(
    private val repository: AleviniRepository,
    private val securityPrefs: SecurityPreferencesRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String,
    isEmailVerified: Boolean,
    private val gatewayClient: PaymentGatewayClient = PaymentGatewayClient(),
) : ViewModel() {

    val isAdmin: Boolean =
        currentUserEmail.equals(AleviniRepository.ADMIN_EMAIL, ignoreCase = true) && isEmailVerified

    val exchangeConfig: StateFlow<ExchangeConfig> = repository.observeExchangeConfig()
        .catch { error ->
            unwrapAndLog(error, "exchange_config")
            emit(ExchangeConfig())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ExchangeConfig(),
        )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapAndLog(error, "user_profile")
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null,
        )

    val ordersState: StateFlow<UiState<List<ExchangeOrder>>> = repository.observeUserOrders(currentUserId)
        .map<List<ExchangeOrder>, UiState<List<ExchangeOrder>>> { UiState.Success(it) }
        .catch { error ->
            val msg = unwrapAndLog(error, "user_orders")
            emit(UiState.Error(msg))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val adminOrdersState: StateFlow<UiState<List<ExchangeOrder>>> = if (isAdmin) {
        repository.observeAllOrders()
            .map<List<ExchangeOrder>, UiState<List<ExchangeOrder>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "admin_orders")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    } else {
        flowOf(UiState.Success(emptyList<ExchangeOrder>())).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList()),
        )
    }

    val adminUsersState: StateFlow<UiState<List<UserProfile>>> = if (isAdmin) {
        repository.observeAllUsers()
            .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "admin_users")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    } else {
        flowOf(UiState.Success(emptyList<UserProfile>())).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList()),
        )
    }

    val adminLedgerState: StateFlow<UiState<List<LedgerEntry>>> = if (isAdmin) {
        repository.observeLedgerEntries()
            .map<List<LedgerEntry>, UiState<List<LedgerEntry>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "admin_ledger")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    } else {
        flowOf(UiState.Success(emptyList<LedgerEntry>())).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList()),
        )
    }

    val adminNotificationsState: StateFlow<UiState<List<AdminNotification>>> = if (isAdmin) {
        repository.observeAllAdminNotifications()
            .map<List<AdminNotification>, UiState<List<AdminNotification>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "admin_notifications")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    } else {
        flowOf(UiState.Success(emptyList<AdminNotification>())).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList()),
        )
    }

    val adminPaymentSessionsState: StateFlow<UiState<List<PaymentSession>>> = if (isAdmin) {
        repository.observeAllPaymentSessions()
            .map<List<PaymentSession>, UiState<List<PaymentSession>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "admin_payment_sessions")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    } else {
        flowOf(UiState.Success(emptyList<PaymentSession>())).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList()),
        )
    }

    val adminWebhookEventsState: StateFlow<UiState<List<WebhookEvent>>> = if (isAdmin) {
        repository.observeWebhookEvents()
            .map<List<WebhookEvent>, UiState<List<WebhookEvent>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "admin_webhook_events")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    } else {
        flowOf(UiState.Success(emptyList<WebhookEvent>())).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList()),
        )
    }

    private val userAdminNotificationsFlow = repository.observeUserAdminNotifications(currentUserId)
        .catch { error ->
            unwrapAndLog(error, "user_admin_notifications")
            emit(emptyList())
        }

    val isBiometricLockEnabled: StateFlow<Boolean> = securityPrefs.isBiometricLockEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false,
        )

    val isDataSaverEnabled: StateFlow<Boolean> = securityPrefs.isDataSaverEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false,
        )

    val pinSecurityState: StateFlow<PinSecurityState> = securityPrefs.pinSecurityState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PinSecurityState(),
        )

    init {
        viewModelScope.launch {
            if (currentUserEmail.isNotBlank()) {
                securityPrefs.setRecoveryEmailIfBlank(currentUserEmail)
            }
        }
        viewModelScope.launch {
            exchangeConfig
                .map { it.backendBaseUrl.trim() }
                .distinctUntilChanged()
                .collect { url ->
                    securityPrefs.setCachedBackendUrl(url)
                }
        }
    }

    val checkedSecurityItems: StateFlow<Set<String>> = securityPrefs.checkedSecurityItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SecurityPreferencesRepository.DEFAULT_DONE_ITEMS,
        )

    val isBalanceHidden: StateFlow<Boolean> = securityPrefs.isBalanceHidden
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false,
        )

    val selectedLanguageCode: StateFlow<String> = securityPrefs.selectedLanguageCode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = "fr",
        )

    val dismissedOrderIds: StateFlow<Set<String>> = securityPrefs.dismissedOrderIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet(),
        )

    val notifications: StateFlow<List<AppNotification>> = combine(
        userAdminNotificationsFlow,
        ordersState,
        securityPrefs.dismissedNotificationIds,
        isDataSaverEnabled,
    ) { adminNotifs, state, dismissed, dataSaver ->
        val raw = buildList {
            val notifLimit = if (dataSaver) 8 else 25
            adminNotifs.take(notifLimit).forEach { n ->
                val dateLabel = n.createdAt?.toDate()?.let {
                    SimpleDateFormat("dd MMM HH:mm", Locale.FRENCH).format(it)
                } ?: "Admin"
                add(
                    AppNotification(
                        id = "admin_${n.id}",
                        title = n.title,
                        message = n.message,
                        timeLabel = dateLabel,
                        isAlert = n.isAlert,
                    )
                )
            }
            val orders = (state as? UiState.Success)?.data.orEmpty()
            orders.take(notifLimit).forEach { order ->
                val status = OrderStatus.fromString(order.status)
                val dateLabel = order.updatedAt?.toDate()?.let {
                    SimpleDateFormat("dd MMM HH:mm", Locale.FRENCH).format(it)
                } ?: "Récent"
                val sendLabel = PaymentMethod.fromId(order.sendMethod).label
                val receiveLabel = PaymentMethod.fromId(order.receiveMethod).label
                val isTopUp = order.orderType == "TOPUP" || order.sendMethod == order.receiveMethod
                val routeTitle = if (isTopUp) "Recharge $sendLabel" else "$sendLabel → $receiveLabel"
                val msg = when (status) {
                    OrderStatus.PENDING -> "Ordre ${order.orderCode} ($routeTitle) en attente de validation."
                    OrderStatus.SUBMITTED -> "Preuve soumise pour ${order.orderCode}. Validation admin en cours."
                    OrderStatus.PAID_IN -> "Fonds reçus pour ${order.orderCode}. Traitement imminent."
                    OrderStatus.COMPLETED -> if (isTopUp) {
                        "Recharge ${order.orderCode} créditée ! +${String.format(Locale.US, "%.2f", order.receiveAmount)} ${order.receiveCurrency} sur $sendLabel."
                    } else {
                        "Ordre ${order.orderCode} complété ! ${String.format(Locale.US, "%.2f", order.receiveAmount)} ${order.receiveCurrency} envoyé."
                    }
                    OrderStatus.REJECTED -> "Ordre ${order.orderCode} refusé. Vérifiez la note admin ou contactez le support."
                    OrderStatus.UNDERPAID -> "Ordre ${order.orderCode} sous-payé. Contactez supportdisponible@gmail.com."
                    OrderStatus.EXPIRED -> "Le verrou de 15 min pour ${order.orderCode} a expiré."
                }
                add(
                    AppNotification(
                        id = "order_${order.id}_${order.status}",
                        title = "${order.orderCode} • ${status.label}",
                        message = msg,
                        timeLabel = dateLabel,
                        orderId = order.id,
                        isAlert = status == OrderStatus.REJECTED || status == OrderStatus.UNDERPAID || status == OrderStatus.EXPIRED,
                    )
                )
            }
            add(
                AppNotification(
                    id = "welcome_alevini_security",
                    title = "Bienvenue sur Alevini",
                    message = "Rechargez d'abord votre passerelle dédiée (MonCash, NatCash, Binance Pay, Meru ou NOWPayments) avant d'échanger ou commander.",
                    timeLabel = "Système",
                    isAlert = false,
                )
            )
        }
        raw.filterNot { it.id in dismissed }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
    )

    val walletSummary: StateFlow<WalletSummary> = combine(
        userProfile,
        ordersState,
        exchangeConfig,
    ) { profile, state, cfg ->
        val orders = (state as? UiState.Success)?.data.orEmpty()
        var pendingCount = 0
        var completedCount = 0
        val topUpByMethod = mutableMapOf<PaymentMethod, Double>()
        val spentByMethod = mutableMapOf<PaymentMethod, Double>()

        orders.forEach { order ->
            val st = OrderStatus.fromString(order.status)
            if (st == OrderStatus.PENDING || st == OrderStatus.SUBMITTED || st == OrderStatus.PAID_IN) {
                pendingCount++
            }
            if (st == OrderStatus.COMPLETED) {
                completedCount++
            }
            val method = PaymentMethod.fromId(order.sendMethod)
            val isTopUp = order.orderType == "TOPUP" || order.sendMethod == order.receiveMethod
            if (isTopUp && st == OrderStatus.COMPLETED) {
                topUpByMethod[method] = (topUpByMethod[method] ?: 0.0) + order.receiveAmount
            } else if (!isTopUp && st != OrderStatus.REJECTED && st != OrderStatus.EXPIRED && order.gatewayMode == "WALLET") {
                val debit = ExchangeCalculator.requiredGatewayDebit(
                    config = cfg,
                    sendMethod = method,
                    sendCurrency = CurrencyCode.fromCode(order.sendCurrency),
                    sendAmount = order.sendAmount,
                )
                spentByMethod[method] = (spentByMethod[method] ?: 0.0) + debit
            }
        }

        val gateways = listOf(
            PaymentMethod.MONCASH,
            PaymentMethod.NATCASH,
            PaymentMethod.BINANCE,
            PaymentMethod.MERU,
            PaymentMethod.NOWPAYMENTS,
        ).map { method ->
            GatewayWalletItem(
                method = method,
                balance = profile?.balanceFor(method) ?: 0.0,
                currency = method.defaultCurrency,
                feePercent = cfg.feeFor(method),
                totalTopUps = topUpByMethod[method] ?: 0.0,
                totalSpent = spentByMethod[method] ?: 0.0,
            )
        }

        WalletSummary(
            gateways = gateways,
            pendingOrdersCount = pendingCount,
            completedOrdersCount = completedCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WalletSummary(),
    )

    private val _calculatorState = MutableStateFlow(CalculatorUiState())
    val calculatorState: StateFlow<CalculatorUiState> = _calculatorState.asStateFlow()

    val computedQuote: StateFlow<ComputedQuote> = combine(
        exchangeConfig,
        _calculatorState,
    ) { config, input ->
        val amount = input.sendAmountText.replace(",", ".").toDoubleOrNull() ?: 0.0
        when (val result = ExchangeCalculator.compute(
            config = config,
            sendAmount = amount,
            sendCurrency = input.sendCurrency,
            receiveCurrency = input.receiveCurrency,
            sendMethod = input.sendMethod,
            receiveMethod = input.receiveMethod,
        )) {
            is ExchangeCalculator.ValidationResult.Valid -> ComputedQuote(calculation = result.calculation)
            is ExchangeCalculator.ValidationResult.Invalid -> ComputedQuote(errorMessage = result.message)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ComputedQuote(),
    )

    private val _selectedOrder = MutableStateFlow<ExchangeOrder?>(null)
    val selectedOrder: StateFlow<ExchangeOrder?> = _selectedOrder.asStateFlow()

    private val _preselectedDepositMethod = MutableStateFlow<PaymentMethod?>(null)
    val preselectedDepositMethod: StateFlow<PaymentMethod?> = _preselectedDepositMethod.asStateFlow()

    private val _isGatewayBusy = MutableStateFlow(false)
    val isGatewayBusy: StateFlow<Boolean> = _isGatewayBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                userId = currentUserId,
                email = currentUserEmail,
                displayName = currentUserName,
            )
        }
    }

    fun selectSendMethod(method: PaymentMethod) {
        _calculatorState.update { current ->
            val adjustedReceive = if (current.receiveMethod == method) current.sendMethod else current.receiveMethod
            val newSendCurr = if (!method.supportsHtg && current.sendCurrency == CurrencyCode.HTG) {
                CurrencyCode.USD
            } else if (method.supportsHtg && current.sendMethod != method && current.sendCurrency == CurrencyCode.USD) {
                CurrencyCode.HTG
            } else {
                current.sendCurrency
            }
            current.copy(
                sendMethod = method,
                receiveMethod = adjustedReceive,
                sendCurrency = newSendCurr,
            )
        }
    }

    fun selectReceiveMethod(method: PaymentMethod) {
        _calculatorState.update { current ->
            val adjustedSend = if (current.sendMethod == method) current.receiveMethod else current.sendMethod
            val newReceiveCurr = if (!method.supportsHtg && current.receiveCurrency == CurrencyCode.HTG) {
                CurrencyCode.USD
            } else if (method.supportsHtg && current.receiveMethod != method) {
                CurrencyCode.HTG
            } else {
                current.receiveCurrency
            }
            current.copy(
                receiveMethod = method,
                sendMethod = adjustedSend,
                receiveCurrency = newReceiveCurr,
            )
        }
    }

    fun swapMethods() {
        _calculatorState.update { current ->
            current.copy(
                sendMethod = current.receiveMethod,
                receiveMethod = current.sendMethod,
                sendCurrency = current.receiveCurrency,
                receiveCurrency = current.sendCurrency,
            )
        }
    }

    fun setSendCurrency(currency: CurrencyCode) {
        _calculatorState.update { it.copy(sendCurrency = currency) }
    }

    fun setReceiveCurrency(currency: CurrencyCode) {
        _calculatorState.update { it.copy(receiveCurrency = currency) }
    }

    fun setCryptoCoin(coin: CryptoCoin) {
        _calculatorState.update { it.copy(cryptoCoin = coin) }
    }

    fun setSendAmountText(text: String) {
        val filtered = text.filter { it.isDigit() || it == '.' || it == ',' }.take(12)
        _calculatorState.update { it.copy(sendAmountText = filtered) }
    }

    fun setDestAccount(account: String) {
        _calculatorState.update { it.copy(destAccount = account.take(160)) }
    }

    fun openOrderDetail(order: ExchangeOrder) {
        _selectedOrder.value = order
    }

    fun closeOrderDetail() {
        _selectedOrder.value = null
    }

    fun createOrder(onCreated: (ExchangeOrder) -> Unit = {}) {
        val input = _calculatorState.value
        val amount = input.sendAmountText.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (userProfile.value?.isBlocked == true) {
            _snackbarEvents.tryEmit("Compte suspendu. Contactez le support Alevini.")
            return
        }
        viewModelScope.launch {
            _calculatorState.update { it.copy(isSubmittingOrder = true) }
            val result = repository.createOrder(
                config = exchangeConfig.value,
                userEmail = currentUserEmail,
                sendMethod = input.sendMethod,
                receiveMethod = input.receiveMethod,
                sendCurrency = input.sendCurrency,
                receiveCurrency = input.receiveCurrency,
                cryptoCoin = if (input.sendMethod == PaymentMethod.NOWPAYMENTS) input.cryptoCoin else null,
                sendAmount = amount,
                destAccount = input.destAccount,
            )
            _calculatorState.update { it.copy(isSubmittingOrder = false) }
            result.onSuccess { order ->
                _selectedOrder.value = order
                _calculatorState.update { it.copy(destAccount = "") }
                _snackbarEvents.tryEmit("Ordre ${order.orderCode} créé ! Verrouillé 15 minutes.")
                onCreated(order)
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Impossible de créer l'ordre.")
            }
        }
    }

    fun submitTxRef(orderId: String, txRef: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.submitTxRef(orderId, txRef)
            result.onSuccess {
                _selectedOrder.value = _selectedOrder.value?.copy(
                    externalTxRef = txRef.trim(),
                    status = OrderStatus.SUBMITTED.name,
                )
                _snackbarEvents.tryEmit("Référence soumise pour validation admin.")
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Erreur lors de la soumission.")
            }
        }
    }

    fun markOrderExpired(orderId: String) {
        viewModelScope.launch {
            repository.markOrderExpired(orderId)
        }
    }

    fun createWizardTopUpOrder(
        gatewayMethod: PaymentMethod,
        cryptoCoin: CryptoCoin?,
        amount: Double,
        onCreated: (ExchangeOrder) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (userProfile.value?.isBlocked == true) {
            val msg = "Compte suspendu. Contactez le support Alevini."
            _snackbarEvents.tryEmit(msg)
            onError(msg)
            return
        }
        viewModelScope.launch {
            _calculatorState.update { it.copy(isSubmittingOrder = true) }
            val result = repository.createTopUpOrder(
                config = exchangeConfig.value,
                userEmail = currentUserEmail,
                gatewayMethod = gatewayMethod,
                cryptoCoin = if (gatewayMethod == PaymentMethod.NOWPAYMENTS) cryptoCoin else null,
                amount = amount,
            )
            _calculatorState.update { it.copy(isSubmittingOrder = false) }
            result.onSuccess { order ->
                _snackbarEvents.tryEmit("Recharge ${order.orderCode} (${gatewayMethod.label}) prête !")
                onCreated(order)
            }.onFailure { err ->
                val msg = err.message ?: "Impossible d'initialiser la recharge."
                _snackbarEvents.tryEmit(msg)
                onError(msg)
            }
        }
    }

    fun selectDepositGatewayPreset(method: PaymentMethod) {
        _preselectedDepositMethod.value = method
    }

    fun clearPreselectedDepositMethod() {
        _preselectedDepositMethod.value = null
    }

    fun initiateOnlineGatewayPayment(
        order: ExchangeOrder,
        onSessionReady: (ExchangeOrder) -> Unit = {},
    ) {
        val cfg = exchangeConfig.value
        val method = PaymentMethod.fromId(order.sendMethod)
        if (!method.supportsOnlineApi || !cfg.isOnlineEnabledFor(method)) {
            _snackbarEvents.tryEmit("La passerelle en ligne ${method.label} est désactivée.")
            return
        }
        val backendUrl = cfg.backendBaseUrl.trim()
        if (backendUrl.isBlank()) {
            _snackbarEvents.tryEmit(
                "Serveur API non encore connecté : utilisez le paiement direct ou configurez l'URL HTTPS dans Admin > Passerelles API."
            )
            return
        }
        viewModelScope.launch {
            _isGatewayBusy.value = true
            try {
                val idToken = repository.getFreshIdToken()
                if (idToken.isNullOrBlank()) {
                    _snackbarEvents.tryEmit("Session Google expirée. Veuillez vous reconnecter.")
                    return@launch
                }
                val result = gatewayClient.initiateOnlinePayment(
                    backendBaseUrl = backendUrl,
                    firebaseIdToken = idToken,
                    orderId = order.id,
                    provider = method,
                )
                result.onSuccess { res ->
                    repository.attachOnlineCheckoutSession(
                        order = order,
                        gatewayMode = res.mode,
                        gatewayOrderId = res.gatewayOrderId,
                        checkoutUrl = res.checkoutUrl,
                        payAddress = res.payAddress.ifBlank { order.payAddress },
                        payCryptoAmount = if (res.payCryptoAmount > 0.0) res.payCryptoAmount else order.payCryptoAmount,
                    )
                    val updated = order.copy(
                        gatewayMode = "ONLINE",
                        gatewayOrderId = res.gatewayOrderId,
                        checkoutUrl = res.checkoutUrl,
                        gatewayStatus = "WAITING_WEBHOOK",
                        payAddress = res.payAddress.ifBlank { order.payAddress },
                        payCryptoAmount = if (res.payCryptoAmount > 0.0) res.payCryptoAmount else order.payCryptoAmount,
                    )
                    if (_selectedOrder.value?.id == order.id) {
                        _selectedOrder.value = updated
                    }
                    onSessionReady(updated)
                    _snackbarEvents.tryEmit("Portail ${method.label} sécurisé prêt !")
                }.onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Impossible d'initialiser ${method.label} en ligne.")
                }
            } finally {
                _isGatewayBusy.value = false
            }
        }
    }

    fun verifyOnlinePaymentStatus(order: ExchangeOrder) {
        val backendUrl = exchangeConfig.value.backendBaseUrl.trim()
        if (backendUrl.isBlank()) {
            _snackbarEvents.tryEmit("URL Backend non configurée dans Admin > Passerelles API.")
            return
        }
        viewModelScope.launch {
            _isGatewayBusy.value = true
            try {
                val idToken = repository.getFreshIdToken()
                if (idToken.isNullOrBlank()) {
                    _snackbarEvents.tryEmit("Session Google expirée.")
                    return@launch
                }
                val result = gatewayClient.checkPaymentStatus(
                    backendBaseUrl = backendUrl,
                    firebaseIdToken = idToken,
                    orderId = order.id,
                )
                result.onSuccess { st ->
                    _snackbarEvents.tryEmit("Statut API : ${st.status} (${st.gatewayStatus})")
                }.onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Échec de vérification du statut.")
                }
            } finally {
                _isGatewayBusy.value = false
            }
        }
    }

    fun updateProfile(displayName: String, phoneNumber: String, isAgeVerified: Boolean) {
        viewModelScope.launch {
            val result = repository.updateUserProfile(
                userId = currentUserId,
                displayName = displayName,
                phoneNumber = phoneNumber,
                isAgeVerified = isAgeVerified,
            )
            result.onSuccess {
                _snackbarEvents.tryEmit("Profil mis à jour avec succès.")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Échec de la mise à jour du profil.")
            }
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            securityPrefs.setBiometricLockEnabled(enabled)
            _snackbarEvents.tryEmit(
                if (enabled) "Verrouillage biométrique activé." else "Verrouillage biométrique désactivé."
            )
        }
    }

    fun setDataSaverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPrefs.setDataSaverEnabled(enabled)
            _snackbarEvents.tryEmit(
                if (enabled) {
                    "Mode Économie de Données activé : cache hors-ligne prioritaire & flux réduits."
                } else {
                    "Mode Économie de Données désactivé."
                }
            )
        }
    }

    fun setPinLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !pinSecurityState.value.isPinConfigured) {
                _snackbarEvents.tryEmit("Définissez d'abord un code PIN à 4 chiffres.")
                return@launch
            }
            securityPrefs.setPinEnabled(enabled)
            _snackbarEvents.tryEmit(
                if (enabled) "Code PIN à l'ouverture activé." else "Code PIN à l'ouverture désactivé."
            )
        }
    }

    fun updatePinCode(
        currentPin: String,
        newPin: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> },
    ) {
        if (!PinSecurityHelper.isValidPinFormat(newPin)) {
            onResult(false, "Le nouveau code PIN doit contenir exactement 4 chiffres.")
            return
        }
        viewModelScope.launch {
            if (pinSecurityState.value.isPinConfigured) {
                when (val verify = securityPrefs.verifyPin(currentPin)) {
                    PinVerifyResult.Success, PinVerifyResult.NotConfigured -> Unit
                    is PinVerifyResult.Incorrect -> {
                        onResult(false, "Code PIN actuel incorrect (${verify.remainingAttempts} essai(s) restant(s)).")
                        return@launch
                    }
                    is PinVerifyResult.LockedOut -> {
                        onResult(false, "Trop de tentatives erronées. Réessayez dans un instant.")
                        return@launch
                    }
                }
            }
            val ok = securityPrefs.saveNewPin(newPin)
            if (ok) {
                _snackbarEvents.tryEmit("Code PIN à 4 chiffres enregistré.")
                onResult(true, "Code PIN enregistré.")
            } else {
                onResult(false, "Impossible d'enregistrer le code PIN.")
            }
        }
    }

    fun updateRecoveryEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isNotEmpty() && !PinSecurityHelper.isValidEmailFormat(trimmed)) {
            _snackbarEvents.tryEmit("Adresse e-mail invalide.")
            return
        }
        viewModelScope.launch {
            securityPrefs.setRecoveryEmail(trimmed)
            _snackbarEvents.tryEmit(
                if (trimmed.isEmpty()) "E-mail de récupération effacé (Non renseigné)."
                else "E-mail de récupération enregistré."
            )
        }
    }

    fun toggleSecurityItem(id: String) {
        viewModelScope.launch {
            securityPrefs.toggleSecurityItem(id)
        }
    }

    fun resetSecurityChecklist() {
        viewModelScope.launch {
            securityPrefs.resetSecurityChecklist()
        }
    }

    fun toggleBalanceHidden() {
        val next = !isBalanceHidden.value
        viewModelScope.launch {
            securityPrefs.setBalanceHidden(next)
            _snackbarEvents.tryEmit(if (next) "Solde masqué." else "Solde affiché.")
        }
    }

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            securityPrefs.setSelectedLanguageCode(language.code)
            _snackbarEvents.tryEmit("Langue sélectionnée : ${language.frenchName} (${language.nativeName})")
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            securityPrefs.dismissNotification(id)
        }
    }

    fun clearAllNotifications() {
        val ids = notifications.value.map { it.id }.toSet()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            securityPrefs.dismissAllNotifications(ids)
            _snackbarEvents.tryEmit("Toutes les notifications ont été supprimées.")
        }
    }

    fun startDepositPreset(method: PaymentMethod = PaymentMethod.MONCASH) {
        _preselectedDepositMethod.value = method
    }

    fun startWithdrawPreset(receiveMethod: PaymentMethod = PaymentMethod.MONCASH) {
        val sendMethod = if (receiveMethod == PaymentMethod.BINANCE) PaymentMethod.NOWPAYMENTS else PaymentMethod.BINANCE
        val receiveCurr = if (receiveMethod.supportsHtg) CurrencyCode.HTG else CurrencyCode.USD
        _calculatorState.update {
            it.copy(
                sendMethod = sendMethod,
                receiveMethod = receiveMethod,
                sendCurrency = CurrencyCode.USD,
                receiveCurrency = receiveCurr,
            )
        }
    }

    fun openOrderById(orderId: String) {
        val list = (ordersState.value as? UiState.Success)?.data.orEmpty()
        list.firstOrNull { it.id == orderId }?.let { _selectedOrder.value = it }
    }

    fun deleteOrHideUserOrder(order: ExchangeOrder) {
        viewModelScope.launch {
            val isRemovableInDb =
                order.status == OrderStatus.PENDING.name || order.status == OrderStatus.EXPIRED.name
            if (isRemovableInDb) {
                val result = repository.deletePendingOrExpiredOrder(order.id)
                result.onSuccess {
                    if (_selectedOrder.value?.id == order.id) {
                        _selectedOrder.value = null
                    }
                    _snackbarEvents.tryEmit("Transaction ${order.orderCode} supprimée.")
                }.onFailure {
                    // Fallback to local archive if status transitioned concurrently
                    securityPrefs.dismissHistoryOrder(order.id)
                    _snackbarEvents.tryEmit("Transaction ${order.orderCode} retirée de votre historique.")
                }
            } else {
                securityPrefs.dismissHistoryOrder(order.id)
                if (_selectedOrder.value?.id == order.id) {
                    _selectedOrder.value = null
                }
                _snackbarEvents.tryEmit("Transaction ${order.orderCode} retirée de votre historique.")
            }
        }
    }

    fun deleteCompletedUserOrders(orders: List<ExchangeOrder>) {
        val completedIds = orders
            .filter { it.status == OrderStatus.COMPLETED.name }
            .map { it.id }
            .toSet()
        if (completedIds.isEmpty()) return
        viewModelScope.launch {
            securityPrefs.dismissHistoryOrders(completedIds)
            if (_selectedOrder.value?.id in completedIds) {
                _selectedOrder.value = null
            }
            val msg = if (completedIds.size == 1) {
                "1 ordre complété retiré de votre historique."
            } else {
                "${completedIds.size} ordres complétés retirés de votre historique."
            }
            _snackbarEvents.tryEmit(msg)
        }
    }

    // --- ADMIN ACTIONS ---

    fun adminSaveConfig(newConfig: ExchangeConfig) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.updateExchangeConfig(newConfig)
            result.onSuccess {
                _snackbarEvents.tryEmit("Taux et passerelles mis à jour en direct !")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Erreur lors de la sauvegarde.")
            }
        }
    }

    fun adminCompleteOrder(order: ExchangeOrder, note: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.completeOrder(order, note)
            result.onSuccess {
                _snackbarEvents.tryEmit("Ordre ${order.orderCode} complété et enregistré en caisse.")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Erreur lors de la validation.")
            }
        }
    }

    fun adminUpdateOrderStatus(orderId: String, status: OrderStatus, note: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.updateOrderStatus(orderId, status, note)
            result.onSuccess {
                _snackbarEvents.tryEmit("Statut mis à jour : ${status.label}")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Erreur de mise à jour.")
            }
        }
    }

    fun adminAdjustGatewayBalance(userId: String, method: PaymentMethod, newBalance: Double) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.adminAdjustGatewayBalance(userId, method, newBalance)
            result.onSuccess {
                _snackbarEvents.tryEmit("Solde ${method.label} ajusté (${String.format(Locale.US, "%.2f", newBalance)} ${method.defaultCurrency.code}).")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Erreur lors de l'ajustement du solde.")
            }
        }
    }

    fun adminToggleUserBlocked(userId: String, blocked: Boolean) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.toggleUserBlocked(userId, blocked)
            result.onSuccess {
                _snackbarEvents.tryEmit(if (blocked) "Compte utilisateur bloqué." else "Compte débloqué.")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Erreur lors de la modification du compte.")
            }
        }
    }

    fun adminSendNotification(
        title: String,
        message: String,
        targetUserId: String,
        targetLabel: String,
        isAlert: Boolean,
        onSuccess: () -> Unit = {},
    ) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.sendAdminNotification(
                title = title,
                message = message,
                targetUserId = targetUserId,
                targetLabel = targetLabel,
                isAlert = isAlert,
            )
            result.onSuccess {
                _snackbarEvents.tryEmit("Notification envoyée à : $targetLabel")
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Échec de l'envoi de la notification.")
            }
        }
    }

    fun adminDeleteNotification(notificationId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.deleteAdminNotification(notificationId)
            result.onSuccess {
                _snackbarEvents.tryEmit("Notification supprimée.")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Erreur lors de la suppression.")
            }
        }
    }

    fun adminDeleteOrder(orderId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.deleteOrder(orderId)
            result.onSuccess {
                _snackbarEvents.tryEmit("Ordre supprimé du Back-Office.")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Impossible de supprimer l'ordre.")
            }
        }
    }

    fun adminDeleteUserProfile(userId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.deleteUserProfile(userId)
            result.onSuccess {
                _snackbarEvents.tryEmit("Profil utilisateur supprimé.")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Impossible de supprimer l'utilisateur.")
            }
        }
    }

    fun adminDeleteLedgerEntry(entryId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.deleteLedgerEntry(entryId)
            result.onSuccess {
                _snackbarEvents.tryEmit("Écriture comptable supprimée.")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Impossible de supprimer l'écriture.")
            }
        }
    }

    fun adminDeletePaymentSession(sessionId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.deletePaymentSession(sessionId)
            result.onSuccess {
                _snackbarEvents.tryEmit("Session API supprimée.")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Impossible de supprimer la session.")
            }
        }
    }

    fun adminDeleteWebhookEvent(eventId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            val result = repository.deleteWebhookEvent(eventId)
            result.onSuccess {
                _snackbarEvents.tryEmit("Événement webhook supprimé.")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Impossible de supprimer l'événement.")
            }
        }
    }

    private fun unwrapAndLog(error: Throwable, tagContext: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $tagContext", rootError)
        return rootError.localizedMessage ?: "Erreur de chargement ($tagContext)"
    }

    private companion object {
        const val TAG = "AleviniVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
