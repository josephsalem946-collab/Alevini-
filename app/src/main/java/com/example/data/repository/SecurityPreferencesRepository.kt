package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.domain.PinSecurityHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aleviniSecurityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "alevini_security_prefs"
)

data class PinSecurityState(
    val isPinConfigured: Boolean = false,
    val isPinEnabled: Boolean = true,
    val recoveryEmail: String = "",
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0L,
    val cachedBackendUrl: String = "",
    val isLoaded: Boolean = false,
)

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data object NotConfigured : PinVerifyResult
    data class Incorrect(val remainingAttempts: Int) : PinVerifyResult
    data class LockedOut(val untilMillis: Long) : PinVerifyResult
}

class SecurityPreferencesRepository(private val context: Context) {

    private val biometricLockKey = booleanPreferencesKey("biometric_lock_enabled")
    private val dataSaverEnabledKey = booleanPreferencesKey("data_saver_enabled")
    private val checkedSecurityItemsKey = stringSetPreferencesKey("checked_security_items")
    private val balanceHiddenKey = booleanPreferencesKey("balance_hidden")
    private val languageCodeKey = stringPreferencesKey("selected_language_code")
    private val dismissedNotificationsKey = stringSetPreferencesKey("dismissed_notification_ids")
    private val dismissedOrderIdsKey = stringSetPreferencesKey("dismissed_order_ids")
    private val dismissedStoreOrderIdsKey = stringSetPreferencesKey("dismissed_store_order_ids")
    private val dismissedInvestmentIdsKey = stringSetPreferencesKey("dismissed_investment_ids")

    private val pinHashKey = stringPreferencesKey("security_pin_hash")
    private val pinSaltKey = stringPreferencesKey("security_pin_salt")
    private val pinEnabledKey = booleanPreferencesKey("security_pin_enabled")
    private val recoveryEmailKey = stringPreferencesKey("security_pin_recovery_email")
    private val pinFailedAttemptsKey = intPreferencesKey("security_pin_failed_attempts")
    private val pinLockoutUntilMillisKey = longPreferencesKey("security_pin_lockout_until_millis")
    private val cachedBackendUrlKey = stringPreferencesKey("cached_backend_base_url")

    val isBiometricLockEnabled: Flow<Boolean> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[biometricLockKey] ?: false }

    val isDataSaverEnabled: Flow<Boolean> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[dataSaverEnabledKey] ?: false }

    val checkedSecurityItems: Flow<Set<String>> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[checkedSecurityItemsKey] ?: DEFAULT_DONE_ITEMS }

    val isBalanceHidden: Flow<Boolean> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[balanceHiddenKey] ?: false }

    val selectedLanguageCode: Flow<String> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[languageCodeKey] ?: "fr" }

    val dismissedNotificationIds: Flow<Set<String>> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[dismissedNotificationsKey] ?: emptySet() }

    val dismissedOrderIds: Flow<Set<String>> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[dismissedOrderIdsKey] ?: emptySet() }

    val dismissedStoreOrderIds: Flow<Set<String>> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[dismissedStoreOrderIdsKey] ?: emptySet() }

    val dismissedInvestmentIds: Flow<Set<String>> = context.aleviniSecurityDataStore.data
        .map { prefs -> prefs[dismissedInvestmentIdsKey] ?: emptySet() }

    val pinSecurityState: Flow<PinSecurityState> = context.aleviniSecurityDataStore.data
        .map { prefs ->
            val hash = prefs[pinHashKey].orEmpty()
            val salt = prefs[pinSaltKey].orEmpty()
            PinSecurityState(
                isPinConfigured = hash.isNotBlank() && salt.isNotBlank(),
                isPinEnabled = prefs[pinEnabledKey] ?: true,
                recoveryEmail = prefs[recoveryEmailKey].orEmpty(),
                failedAttempts = prefs[pinFailedAttemptsKey] ?: 0,
                lockoutUntilMillis = prefs[pinLockoutUntilMillisKey] ?: 0L,
                cachedBackendUrl = prefs[cachedBackendUrlKey].orEmpty(),
                isLoaded = true,
            )
        }

    suspend fun saveNewPin(pin: String): Boolean {
        if (!PinSecurityHelper.isValidPinFormat(pin)) return false
        val salt = PinSecurityHelper.generateSaltHex()
        val hash = PinSecurityHelper.hashPin(pin, salt)
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[pinSaltKey] = salt
            prefs[pinHashKey] = hash
            prefs[pinEnabledKey] = true
            prefs[pinFailedAttemptsKey] = 0
            prefs[pinLockoutUntilMillisKey] = 0L
        }
        return true
    }

    suspend fun verifyPin(pin: String, nowMillis: Long = System.currentTimeMillis()): PinVerifyResult {
        val prefs = context.aleviniSecurityDataStore.data.first()
        val storedHash = prefs[pinHashKey].orEmpty()
        val storedSalt = prefs[pinSaltKey].orEmpty()
        if (storedHash.isBlank() || storedSalt.isBlank()) {
            return PinVerifyResult.NotConfigured
        }
        val lockoutUntil = prefs[pinLockoutUntilMillisKey] ?: 0L
        if (lockoutUntil > nowMillis) {
            return PinVerifyResult.LockedOut(lockoutUntil)
        }

        val matches = PinSecurityHelper.verifyPinHash(pin, storedSalt, storedHash)
        if (matches) {
            context.aleviniSecurityDataStore.edit { editPrefs ->
                editPrefs[pinFailedAttemptsKey] = 0
                editPrefs[pinLockoutUntilMillisKey] = 0L
            }
            return PinVerifyResult.Success
        }

        val newAttempts = (prefs[pinFailedAttemptsKey] ?: 0) + 1
        if (newAttempts >= PinSecurityHelper.MAX_FAILED_ATTEMPTS) {
            val until = nowMillis + PinSecurityHelper.LOCKOUT_DURATION_MS
            context.aleviniSecurityDataStore.edit { editPrefs ->
                editPrefs[pinFailedAttemptsKey] = 0
                editPrefs[pinLockoutUntilMillisKey] = until
            }
            return PinVerifyResult.LockedOut(until)
        }

        context.aleviniSecurityDataStore.edit { editPrefs ->
            editPrefs[pinFailedAttemptsKey] = newAttempts
        }
        return PinVerifyResult.Incorrect(
            remainingAttempts = (PinSecurityHelper.MAX_FAILED_ATTEMPTS - newAttempts).coerceAtLeast(1)
        )
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[pinEnabledKey] = enabled
        }
    }

    suspend fun setRecoveryEmail(email: String) {
        val cleaned = email.trim()
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[recoveryEmailKey] = cleaned
        }
    }

    suspend fun setRecoveryEmailIfBlank(email: String) {
        val cleaned = email.trim()
        if (cleaned.isBlank()) return
        context.aleviniSecurityDataStore.edit { prefs ->
            if (prefs[recoveryEmailKey].isNullOrBlank()) {
                prefs[recoveryEmailKey] = cleaned
            }
        }
    }

    suspend fun setCachedBackendUrl(url: String) {
        val cleaned = url.trim()
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[cachedBackendUrlKey] = cleaned
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[biometricLockKey] = enabled
        }
    }

    suspend fun setDataSaverEnabled(enabled: Boolean) {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[dataSaverEnabledKey] = enabled
        }
    }

    suspend fun toggleSecurityItem(id: String) {
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[checkedSecurityItemsKey] ?: DEFAULT_DONE_ITEMS
            prefs[checkedSecurityItemsKey] = if (id in current) current - id else current + id
        }
    }

    suspend fun resetSecurityChecklist() {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[checkedSecurityItemsKey] = emptySet()
        }
    }

    suspend fun setBalanceHidden(hidden: Boolean) {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[balanceHiddenKey] = hidden
        }
    }

    suspend fun setSelectedLanguageCode(code: String) {
        context.aleviniSecurityDataStore.edit { prefs ->
            prefs[languageCodeKey] = code
        }
    }

    suspend fun dismissNotification(id: String) {
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[dismissedNotificationsKey] ?: emptySet()
            prefs[dismissedNotificationsKey] = current + id
        }
    }

    suspend fun dismissAllNotifications(ids: Set<String>) {
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[dismissedNotificationsKey] ?: emptySet()
            prefs[dismissedNotificationsKey] = current + ids
        }
    }

    suspend fun dismissHistoryOrder(orderId: String) {
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[dismissedOrderIdsKey] ?: emptySet()
            prefs[dismissedOrderIdsKey] = current + orderId
        }
    }

    suspend fun dismissHistoryOrders(orderIds: Set<String>) {
        if (orderIds.isEmpty()) return
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[dismissedOrderIdsKey] ?: emptySet()
            prefs[dismissedOrderIdsKey] = current + orderIds
        }
    }

    suspend fun dismissStoreOrder(orderId: String) {
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[dismissedStoreOrderIdsKey] ?: emptySet()
            prefs[dismissedStoreOrderIdsKey] = current + orderId
        }
    }

    suspend fun dismissInvestment(investId: String) {
        context.aleviniSecurityDataStore.edit { prefs ->
            val current = prefs[dismissedInvestmentIdsKey] ?: emptySet()
            prefs[dismissedInvestmentIdsKey] = current + investId
        }
    }

    companion object {
        // Items already enforced natively by Alevini's architecture & Firestore security rules
        val DEFAULT_DONE_ITEMS = setOf(
            "0-0-0", "0-0-1", "0-0-2",
            "0-1-0", "0-1-1",
            "0-2-0", "0-2-1",
            "0-5-0"
        )
    }
}
