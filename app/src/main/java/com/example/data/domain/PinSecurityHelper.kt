package com.example.data.domain

import java.security.MessageDigest
import java.security.SecureRandom

object PinSecurityHelper {
    const val PIN_LENGTH = 4
    const val RECOVERY_CODE_LENGTH = 6
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 30_000L

    private val secureRandom = SecureRandom()
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidPinFormat(pin: String): Boolean =
        pin.length == PIN_LENGTH && pin.all { it.isDigit() }

    fun isValidRecoveryCodeFormat(code: String): Boolean =
        code.length == RECOVERY_CODE_LENGTH && code.all { it.isDigit() }

    fun isValidEmailFormat(email: String): Boolean =
        email.trim().isNotEmpty() && emailRegex.matches(email.trim())

    fun generateSaltHex(byteCount: Int = 16): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$saltHex:$pin".toByteArray(Charsets.UTF_8)
        return digest.digest(input).joinToString("") { "%02x".format(it) }
    }

    fun verifyPinHash(pin: String, storedSaltHex: String, storedHashHex: String): Boolean {
        if (!isValidPinFormat(pin) || storedSaltHex.isBlank() || storedHashHex.isBlank()) {
            return false
        }
        val candidate = hashPin(pin, storedSaltHex).toByteArray(Charsets.UTF_8)
        val expected = storedHashHex.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(candidate, expected)
    }

    fun maskEmail(email: String): String {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return "Non renseigné"
        val atIdx = trimmed.indexOf('@')
        if (atIdx <= 1) return trimmed
        val local = trimmed.substring(0, atIdx)
        val domain = trimmed.substring(atIdx)
        val visible = local.take(2)
        return "$visible***$domain"
    }
}
