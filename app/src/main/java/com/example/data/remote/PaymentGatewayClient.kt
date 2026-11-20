package com.example.data.remote

import com.example.data.model.PaymentMethod
import com.example.data.model.StoreProviderHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class OnlineCheckoutResponse(
    val sessionId: String,
    val gatewayOrderId: String,
    val checkoutUrl: String,
    val payAddress: String,
    val payCryptoAmount: Double,
    val mode: String,
)

data class OnlineStatusResponse(
    val orderId: String,
    val status: String,
    val gatewayStatus: String,
    val externalTxRef: String,
)

data class StoreStatusResponse(
    val orderId: String,
    val status: String,
    val gatewayStatus: String,
    val externalTxRef: String,
    val deliverable: String,
)

class PaymentGatewayClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun initiateOnlinePayment(
        backendBaseUrl: String,
        firebaseIdToken: String,
        orderId: String,
        provider: PaymentMethod,
    ): Result<OnlineCheckoutResponse> = withContext(Dispatchers.IO) {
        val base = validateBackendBaseUrl(backendBaseUrl)
            ?: return@withContext Result.failure(
                IllegalStateException(
                    "URL Backend sécurisée (HTTPS) requise dans Admin > Passerelles API pour MonCash, NatCash, Binance et NOWPayments."
                )
            )
        if (firebaseIdToken.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Jeton d'authentification Firebase introuvable. Reconnectez-vous avec Google.")
            )
        }

        val jsonBody = JSONObject().apply {
            put("orderId", orderId)
        }.toString()

        val request = Request.Builder()
            .url("$base/api/payments/${provider.id}/create")
            .addHeader("Authorization", "Bearer $firebaseIdToken")
            .addHeader("Accept", "application/json")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val parsed = if (raw.startsWith("{")) JSONObject(raw) else JSONObject()
                if (!response.isSuccessful) {
                    val serverMsg = parsed.optString("error").ifBlank {
                        "Erreur serveur (${response.code})"
                    }
                    throw IllegalStateException(serverMsg)
                }
                OnlineCheckoutResponse(
                    sessionId = parsed.optString("sessionId", "${orderId}_${provider.id}"),
                    gatewayOrderId = parsed.optString("gatewayOrderId"),
                    checkoutUrl = parsed.optString("checkoutUrl"),
                    payAddress = parsed.optString("payAddress"),
                    payCryptoAmount = parsed.optDouble("payCryptoAmount", 0.0),
                    mode = parsed.optString("mode", "SANDBOX"),
                )
            }
        }
    }

    suspend fun checkPaymentStatus(
        backendBaseUrl: String,
        firebaseIdToken: String,
        orderId: String,
    ): Result<OnlineStatusResponse> = withContext(Dispatchers.IO) {
        val base = validateBackendBaseUrl(backendBaseUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("URL Backend HTTPS non configurée.")
            )
        if (firebaseIdToken.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Jeton Firebase manquant.")
            )
        }

        val request = Request.Builder()
            .url("$base/api/payments/status/$orderId")
            .addHeader("Authorization", "Bearer $firebaseIdToken")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val parsed = if (raw.startsWith("{")) JSONObject(raw) else JSONObject()
                if (!response.isSuccessful) {
                    val serverMsg = parsed.optString("error").ifBlank {
                        "Erreur de vérification (${response.code})"
                    }
                    throw IllegalStateException(serverMsg)
                }
                OnlineStatusResponse(
                    orderId = parsed.optString("orderId", orderId),
                    status = parsed.optString("status", "PENDING"),
                    gatewayStatus = parsed.optString("gatewayStatus", "NONE"),
                    externalTxRef = parsed.optString("externalTxRef", ""),
                )
            }
        }
    }

    suspend fun initiateStoreCheckout(
        backendBaseUrl: String,
        firebaseIdToken: String,
        orderId: String,
        provider: PaymentMethod,
    ): Result<OnlineCheckoutResponse> = withContext(Dispatchers.IO) {
        val base = validateBackendBaseUrl(backendBaseUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("URL Backend HTTPS requise dans Admin > Passerelles API.")
            )
        if (firebaseIdToken.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Jeton d'authentification Firebase introuvable.")
            )
        }

        val jsonBody = JSONObject().apply {
            put("provider", provider.id)
        }.toString()

        val request = Request.Builder()
            .url("$base/api/store/orders/$orderId/checkout")
            .addHeader("Authorization", "Bearer $firebaseIdToken")
            .addHeader("Accept", "application/json")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val parsed = if (raw.startsWith("{")) JSONObject(raw) else JSONObject()
                if (!response.isSuccessful) {
                    val serverMsg = parsed.optString("error").ifBlank {
                        "Erreur checkout Store (${response.code})"
                    }
                    throw IllegalStateException(serverMsg)
                }
                OnlineCheckoutResponse(
                    sessionId = parsed.optString("sessionId", "${orderId}_${provider.id}"),
                    gatewayOrderId = parsed.optString("gatewayOrderId"),
                    checkoutUrl = parsed.optString("checkoutUrl"),
                    payAddress = parsed.optString("payAddress"),
                    payCryptoAmount = parsed.optDouble("payCryptoAmount", 0.0),
                    mode = parsed.optString("mode", "SANDBOX"),
                )
            }
        }
    }

    suspend fun checkStoreOrderStatus(
        backendBaseUrl: String,
        firebaseIdToken: String,
        orderId: String,
    ): Result<StoreStatusResponse> = withContext(Dispatchers.IO) {
        val base = validateBackendBaseUrl(backendBaseUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("URL Backend HTTPS non configurée.")
            )
        if (firebaseIdToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Jeton Firebase manquant."))
        }

        val request = Request.Builder()
            .url("$base/api/store/orders/$orderId/status")
            .addHeader("Authorization", "Bearer $firebaseIdToken")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val parsed = if (raw.startsWith("{")) JSONObject(raw) else JSONObject()
                if (!response.isSuccessful) {
                    val serverMsg = parsed.optString("error").ifBlank {
                        "Erreur statut Store (${response.code})"
                    }
                    throw IllegalStateException(serverMsg)
                }
                StoreStatusResponse(
                    orderId = parsed.optString("orderId", orderId),
                    status = parsed.optString("status", "PENDING"),
                    gatewayStatus = parsed.optString("gatewayStatus", "NONE"),
                    externalTxRef = parsed.optString("externalTxRef", ""),
                    deliverable = parsed.optString("deliverable", ""),
                )
            }
        }
    }

    suspend fun checkStoreProvidersHealth(
        backendBaseUrl: String,
        firebaseIdToken: String,
    ): Result<List<StoreProviderHealth>> = withContext(Dispatchers.IO) {
        val base = validateBackendBaseUrl(backendBaseUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("URL Backend HTTPS non configurée dans Passerelles API.")
            )
        if (firebaseIdToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Jeton Firebase manquant."))
        }

        val request = Request.Builder()
            .url("$base/api/store/providers/health")
            .addHeader("Authorization", "Bearer $firebaseIdToken")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val parsed = if (raw.startsWith("{")) JSONObject(raw) else JSONObject()
                if (!response.isSuccessful) {
                    val serverMsg = parsed.optString("error").ifBlank {
                        "Erreur test API (${response.code})"
                    }
                    throw IllegalStateException(serverMsg)
                }
                val arr: JSONArray = parsed.optJSONArray("providers") ?: JSONArray()
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        add(
                            StoreProviderHealth(
                                provider = obj.optString("provider"),
                                mode = obj.optString("mode", "SANDBOX"),
                                configured = obj.optBoolean("configured", false),
                                latencyMs = obj.optInt("latencyMs", 0),
                                status = obj.optString("status", "UP"),
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun requestPinRecoveryCode(
        backendBaseUrl: String,
        email: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val base = validateBackendBaseUrl(backendBaseUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("Serveur de récupération e-mail non configuré. Connectez-vous pour configurer l'URL Backend HTTPS.")
            )
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Adresse e-mail enregistrée : Non renseigné.")
            )
        }
        val jsonBody = JSONObject().apply {
            put("email", trimmed)
        }.toString()
        val request = Request.Builder()
            .url("$base/api/security/pin-recovery/request")
            .addHeader("Accept", "application/json")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val parsed = if (raw.startsWith("{")) JSONObject(raw) else JSONObject()
                if (!response.isSuccessful) {
                    val serverMsg = parsed.optString("error").ifBlank {
                        "Échec de l'envoi du code (${response.code})"
                    }
                    throw IllegalStateException(serverMsg)
                }
            }
        }
    }

    suspend fun verifyPinRecoveryCode(
        backendBaseUrl: String,
        email: String,
        code: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val base = validateBackendBaseUrl(backendBaseUrl)
            ?: return@withContext Result.failure(
                IllegalStateException("Serveur de récupération e-mail non configuré.")
            )
        val jsonBody = JSONObject().apply {
            put("email", email.trim())
            put("code", code.trim())
        }.toString()
        val request = Request.Builder()
            .url("$base/api/security/pin-recovery/verify")
            .addHeader("Accept", "application/json")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val parsed = if (raw.startsWith("{")) JSONObject(raw) else JSONObject()
                if (!response.isSuccessful) {
                    val serverMsg = parsed.optString("error").ifBlank {
                        "Code de validation incorrect (${response.code})"
                    }
                    throw IllegalStateException(serverMsg)
                }
            }
        }
    }

    private fun validateBackendBaseUrl(backendBaseUrl: String): String? {
        val base = backendBaseUrl.trim().trimEnd('/')
        return if (base.startsWith("https://") || base.startsWith("http://127.0.0.1") || base.startsWith("http://10.0.2.2")) {
            base
        } else {
            null
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

