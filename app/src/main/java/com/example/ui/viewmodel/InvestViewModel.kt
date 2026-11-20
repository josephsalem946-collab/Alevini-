package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.InvestCatalog
import com.example.data.model.InvestCategory
import com.example.data.model.InvestCurrency
import com.example.data.model.InvestDuration
import com.example.data.model.InvestStatus
import com.example.data.model.InvestmentRecord
import com.example.data.model.PaymentMethod
import com.example.data.repository.AleviniRepository
import com.example.data.repository.InvestRepository
import com.example.data.repository.SecurityPreferencesRepository
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

class InvestViewModel(
    private val repository: InvestRepository,
    private val securityPrefs: SecurityPreferencesRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    isEmailVerified: Boolean,
) : ViewModel() {

    val isAdmin: Boolean =
        currentUserEmail.equals(AleviniRepository.ADMIN_EMAIL, ignoreCase = true) && isEmailVerified

    val userInvestmentsState: StateFlow<UiState<List<InvestmentRecord>>> =
        repository.observeUserInvestments(currentUserId)
            .map<List<InvestmentRecord>, UiState<List<InvestmentRecord>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "user_investments")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val adminInvestmentsState: StateFlow<UiState<List<InvestmentRecord>>> = if (isAdmin) {
        repository.observeAllInvestments()
            .map<List<InvestmentRecord>, UiState<List<InvestmentRecord>>> { UiState.Success(it) }
            .catch { error ->
                val msg = unwrapAndLog(error, "admin_investments")
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )
    } else {
        flowOf(UiState.Success(emptyList<InvestmentRecord>())).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList()),
        )
    }

    val dismissedInvestmentIds: StateFlow<Set<String>> = securityPrefs.dismissedInvestmentIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet(),
        )

    private val _activeTab = MutableStateFlow(TAB_SANS_RISQUE)
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(InvestCurrency.GDS)
    val selectedCurrency: StateFlow<InvestCurrency> = _selectedCurrency.asStateFlow()

    private val _selectedAmount = MutableStateFlow(InvestCurrency.GDS.defaultAmount)
    val selectedAmount: StateFlow<Double> = _selectedAmount.asStateFlow()

    private val _selectedDuration = MutableStateFlow(InvestDuration.THREE_MONTHS)
    val selectedDuration: StateFlow<InvestDuration> = _selectedDuration.asStateFlow()

    private val _selectedSector = MutableStateFlow(InvestCatalog.A_RISQUE_SECTORS.first().name)
    val selectedSector: StateFlow<String> = _selectedSector.asStateFlow()

    private val _selectedGateway = MutableStateFlow(PaymentMethod.MONCASH)
    val selectedGateway: StateFlow<PaymentMethod> = _selectedGateway.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    fun selectTab(tabKey: String) {
        _activeTab.value = tabKey
    }

    fun selectCurrency(newCurrency: InvestCurrency) {
        if (_selectedCurrency.value == newCurrency) return
        _selectedCurrency.value = newCurrency
        _selectedAmount.value = newCurrency.defaultAmount
        if (InvestCatalog.currencyForGateway(_selectedGateway.value) != newCurrency) {
            _selectedGateway.value = InvestCatalog.defaultGatewayForCurrency(newCurrency)
        }
    }

    fun selectAmount(amount: Double) {
        _selectedAmount.value = amount
    }

    fun selectDuration(duration: InvestDuration) {
        _selectedDuration.value = duration
    }

    fun selectSector(sectorName: String) {
        _selectedSector.value = sectorName
    }

    fun selectGateway(gateway: PaymentMethod) {
        if (gateway !in InvestCatalog.SUPPORTED_GATEWAYS) return
        _selectedGateway.value = gateway
        val expectedCurrency = InvestCatalog.currencyForGateway(gateway)
        if (_selectedCurrency.value != expectedCurrency) {
            _selectedCurrency.value = expectedCurrency
            _selectedAmount.value = expectedCurrency.defaultAmount
        }
    }

    fun submitInvestment() {
        val tab = _activeTab.value
        val category = if (tab == TAB_MES_INVESTISSEMENTS) {
            InvestCategory.SANS_RISQUE
        } else {
            InvestCategory.fromId(tab)
        }
        val currency = _selectedCurrency.value
        val amount = _selectedAmount.value
        val duration = _selectedDuration.value
        val gateway = _selectedGateway.value
        val sector = _selectedSector.value

        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                val result = repository.createInvestment(
                    userEmail = currentUserEmail,
                    category = category,
                    sector = sector,
                    amount = amount,
                    currency = currency,
                    duration = duration,
                    gateway = gateway,
                )
                result.onSuccess { created ->
                    _activeTab.value = TAB_MES_INVESTISSEMENTS
                    _snackbarEvents.tryEmit(
                        "Demande ${created.investCode} enregistrée ! En attente de validation Admin."
                    )
                }.onFailure { err ->
                    _snackbarEvents.tryEmit(
                        err.message ?: "Impossible d'enregistrer cet investissement."
                    )
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun deleteOrHideInvestment(investment: InvestmentRecord) {
        viewModelScope.launch {
            if (InvestStatus.fromString(investment.status) == InvestStatus.REJECTED) {
                repository.deleteRejectedInvestment(investment.id)
                    .onSuccess {
                        _snackbarEvents.tryEmit("Investissement ${investment.investCode} supprimé.")
                    }
                    .onFailure {
                        securityPrefs.dismissInvestment(investment.id)
                        _snackbarEvents.tryEmit("Investissement retiré de votre liste.")
                    }
            } else {
                securityPrefs.dismissInvestment(investment.id)
                _snackbarEvents.tryEmit(
                    "Investissement ${investment.investCode} masqué de votre liste."
                )
            }
        }
    }

    fun adminApproveInvestment(investment: InvestmentRecord, note: String = "") {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminDecideInvestment(investment, approve = true, adminNote = note)
                .onSuccess {
                    _snackbarEvents.tryEmit("Investissement ${investment.investCode} accepté et activé !")
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Erreur lors de l'activation.")
                }
        }
    }

    fun adminRejectInvestment(investment: InvestmentRecord, note: String = "") {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminDecideInvestment(investment, approve = false, adminNote = note)
                .onSuccess {
                    _snackbarEvents.tryEmit("Investissement ${investment.investCode} refusé & remboursé.")
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Erreur lors du refus.")
                }
        }
    }

    fun adminDeleteInvestment(investId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminDeleteInvestment(investId)
                .onSuccess { _snackbarEvents.tryEmit("Dossier d'investissement supprimé.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Erreur suppression.") }
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
                "Accès Investissements refusé (règles de sécurité)."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Service indisponible hors ligne."
            else -> error.message ?: "Erreur de chargement des investissements."
        }
    }

    companion object {
        const val TAB_SANS_RISQUE = "sans_risque"
        const val TAB_MOINS_RISQUE = "moins_risque"
        const val TAB_A_RISQUE = "a_risque"
        const val TAB_MES_INVESTISSEMENTS = "mes_investissements"
        private const val TAG = "InvestViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
