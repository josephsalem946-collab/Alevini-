package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.domain.AppLanguage
import com.example.data.domain.ExchangeCalculator
import com.example.data.model.AdminNotification
import com.example.data.model.CryptoCoin
import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeCalculation
import com.example.data.model.ExchangeConfig
import com.example.data.model.ExchangeOrder
import com.example.data.model.LedgerEntry
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.repository.AleviniRepository
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
    val netBalanceUsd: Double = 0.0,
    val netBalanceHtg: Double = 0.0,
    val totalDepositsUsd: Double = 0.0,
    val totalWithdrawalsUsd: Double = 0.0,
    val pendingOrdersCount: Int = 0,
    val completedOrdersCount: Int = 0,
)

class AleviniViewModel(
    private val repository: AleviniRepository,
    private val securityPrefs: SecurityPreferencesRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String,
    isEmailVerified: Boolean,
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

    val notifications: StateFlow<List<AppNotification>> = combine(
        userAdminNotificationsFlow,
        ordersState,
        securityPrefs.dismissedNotificationIds,
    ) { adminNotifs, state, dismissed ->
        val raw = buildList {
            adminNotifs.forEach { n ->
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
            orders.take(25).forEach { order ->
                val status = OrderStatus.fromString(order.status)
                val dateLabel = order.updatedAt?.toDate()?.let {
                    SimpleDateFormat("dd MMM HH:mm", Locale.FRENCH).format(it)
                } ?: "Récent"
                val sendLabel = PaymentMethod.fromId(order.sendMethod).label
                val receiveLabel = PaymentMethod.fromId(order.receiveMethod).label
                val msg = when (status) {
                    OrderStatus.PENDING -> "Ordre ${order.orderCode} ($sendLabel → $receiveLabel) en attente de preuve."
                    OrderStatus.SUBMITTED -> "Preuve soumise pour ${order.orderCode}. Validation admin en cours."
                    OrderStatus.PAID_IN -> "Fonds reçus pour ${order.orderCode}. Transfert sortant imminent."
                    OrderStatus.COMPLETED -> "Ordre ${order.orderCode} complété ! ${String.format(Locale.US, "%.2f", order.receiveAmount)} ${order.receiveCurrency} envoyé."
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
                    message = "Vos taux sont verrouillés 15 minutes à chaque ordre. Activez la biométrie dans Sécurité.",
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
        ordersState,
        exchangeConfig,
    ) { state, cfg ->
        val orders = (state as? UiState.Success)?.data.orEmpty()
        var depositsUsd = 0.0
        var withdrawalsUsd = 0.0
        var pendingCount = 0
        var completedCount = 0
        orders.forEach { order ->
            val st = OrderStatus.fromString(order.status)
            if (st == OrderStatus.PENDING || st == OrderStatus.SUBMITTED || st == OrderStatus.PAID_IN) {
                pendingCount++
            }
            if (st == OrderStatus.COMPLETED) {
                completedCount++
                val sendIsLocal = order.sendMethod == PaymentMethod.MONCASH.id || order.sendMethod == PaymentMethod.NATCASH.id
                if (sendIsLocal) {
                    depositsUsd += order.amountUsd
                } else {
                    withdrawalsUsd += order.amountUsd
                }
            }
        }
        val netUsd = (depositsUsd - withdrawalsUsd).coerceAtLeast(0.0)
        WalletSummary(
            netBalanceUsd = netUsd,
            netBalanceHtg = netUsd * cfg.usdToHtgSell,
            totalDepositsUsd = depositsUsd,
            totalWithdrawalsUsd = withdrawalsUsd,
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

    fun startDepositPreset() {
        _calculatorState.update {
            it.copy(
                sendMethod = PaymentMethod.MONCASH,
                receiveMethod = PaymentMethod.BINANCE,
                sendCurrency = CurrencyCode.HTG,
                receiveCurrency = CurrencyCode.USD,
            )
        }
    }

    fun startWithdrawPreset() {
        _calculatorState.update {
            it.copy(
                sendMethod = PaymentMethod.BINANCE,
                receiveMethod = PaymentMethod.MONCASH,
                sendCurrency = CurrencyCode.USD,
                receiveCurrency = CurrencyCode.HTG,
            )
        }
    }

    fun openOrderById(orderId: String) {
        val list = (ordersState.value as? UiState.Success)?.data.orEmpty()
        list.firstOrNull { it.id == orderId }?.let { _selectedOrder.value = it }
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
