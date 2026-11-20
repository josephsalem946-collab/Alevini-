package com.example.data.domain

import com.example.data.model.ExchangeOrder
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class SecurityAndRetentionUnitTest {

    @Test
    fun pinFormatValidation_acceptsOnlyFourDigits() {
        assertTrue(PinSecurityHelper.isValidPinFormat("0000"))
        assertTrue(PinSecurityHelper.isValidPinFormat("4829"))
        assertFalse(PinSecurityHelper.isValidPinFormat("123"))
        assertFalse(PinSecurityHelper.isValidPinFormat("12345"))
        assertFalse(PinSecurityHelper.isValidPinFormat("12a4"))
        assertFalse(PinSecurityHelper.isValidPinFormat(""))
    }

    @Test
    fun recoveryCodeValidation_acceptsOnlySixDigits() {
        assertTrue(PinSecurityHelper.isValidRecoveryCodeFormat("019283"))
        assertFalse(PinSecurityHelper.isValidRecoveryCodeFormat("12345"))
        assertFalse(PinSecurityHelper.isValidRecoveryCodeFormat("1234567"))
        assertFalse(PinSecurityHelper.isValidRecoveryCodeFormat("12345x"))
    }

    @Test
    fun pinHashingAndVerification_validatesCorrectPinAndRejectsWrongPinOrSalt() {
        val salt = PinSecurityHelper.generateSaltHex()
        val hash = PinSecurityHelper.hashPin("7391", salt)

        assertTrue(PinSecurityHelper.verifyPinHash("7391", salt, hash))
        assertFalse(PinSecurityHelper.verifyPinHash("7392", salt, hash))
        assertFalse(PinSecurityHelper.verifyPinHash("7391", PinSecurityHelper.generateSaltHex(), hash))
        assertFalse(PinSecurityHelper.verifyPinHash("7391", "", hash))
    }

    @Test
    fun maskEmail_returnsNonRenseigneWhenBlankAndMasksOtherwise() {
        assertEquals("Non renseigné", PinSecurityHelper.maskEmail(""))
        assertEquals("Non renseigné", PinSecurityHelper.maskEmail("   "))
        assertEquals("jo***@gmail.com", PinSecurityHelper.maskEmail("josephsalem946@gmail.com"))
    }

    @Test
    fun orderRetentionFilter_keepsRecentAndNullAndHidesOlderThanThreeMonths() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 16, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = nowCal.timeInMillis

        val twoMonthsAgo = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.MONTH, -2)
        }.timeInMillis

        val exactThreeMonthsAgo = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.MONTH, -3)
        }.timeInMillis

        val olderThanThreeMonths = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.MONTH, -3)
            add(Calendar.DAY_OF_MONTH, -1)
        }.timeInMillis

        val orders = listOf(
            ExchangeOrder(id = "null_ts", orderCode = "ALV-001", createdAt = null),
            ExchangeOrder(id = "recent", orderCode = "ALV-002", createdAt = Timestamp(Date(twoMonthsAgo))),
            ExchangeOrder(id = "boundary", orderCode = "ALV-003", createdAt = Timestamp(Date(exactThreeMonthsAgo))),
            ExchangeOrder(id = "expired", orderCode = "ALV-004", createdAt = Timestamp(Date(olderThanThreeMonths))),
        )

        val visible = OrderRetentionFilter.filterRecentOrders(orders, nowMs).map { it.id }

        assertEquals(listOf("null_ts", "recent", "boundary"), visible)
        assertFalse(OrderRetentionFilter.isWithinLastThreeMonths(olderThanThreeMonths, nowMs))
    }
}
