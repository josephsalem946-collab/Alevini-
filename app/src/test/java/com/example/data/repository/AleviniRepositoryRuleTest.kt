package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeConfig
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AleviniRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createOrder_validPayload_createsPendingOrderAndEmitsInFlow() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = AleviniRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                config = ExchangeConfig(),
                userEmail = ALICE_EMAIL,
                sendMethod = PaymentMethod.MONCASH,
                receiveMethod = PaymentMethod.BINANCE,
                sendCurrency = CurrencyCode.HTG,
                receiveCurrency = CurrencyCode.USD,
                cryptoCoin = null,
                sendAmount = 13200.0,
                destAccount = "BINANCE-UID-9988"
            )
        }
        assertTrue("Expected createOrder success, got ${result.exceptionOrNull()}", result.isSuccess)
        val order = result.getOrThrow()
        assertEquals(OrderStatus.PENDING.name, order.status)
        assertTrue(order.orderCode.startsWith("ALV-"))

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserOrders(uid).first { list -> list.any { it.id == order.id } }
        }
        assertTrue(orders.any { it.id == order.id })
    }

    @Test
    fun submitTxRef_uniqueRefSucceeds_andDuplicateRefFails() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = AleviniRepository(firestore, auth)

        val order1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                config = ExchangeConfig(),
                userEmail = ALICE_EMAIL,
                sendMethod = PaymentMethod.NATCASH,
                receiveMethod = PaymentMethod.MERU,
                sendCurrency = CurrencyCode.HTG,
                receiveCurrency = CurrencyCode.USD,
                cryptoCoin = null,
                sendAmount = 6600.0,
                destAccount = "@alice.meru"
            ).getOrThrow()
        }

        val uniqueRef = "NC-${UUID.randomUUID().toString().take(8).uppercase()}"
        val submit1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitTxRef(order1.id, uniqueRef)
        }
        assertTrue("Expected submitTxRef success, got ${submit1.exceptionOrNull()}", submit1.isSuccess)

        // Second user tries to reuse the exact same transaction reference
        signInTestUser(BOB_EMAIL)
        val bobRepo = AleviniRepository(firestore, auth)
        val order2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.createOrder(
                config = ExchangeConfig(),
                userEmail = BOB_EMAIL,
                sendMethod = PaymentMethod.NATCASH,
                receiveMethod = PaymentMethod.BINANCE,
                sendCurrency = CurrencyCode.HTG,
                receiveCurrency = CurrencyCode.USD,
                cryptoCoin = null,
                sendAmount = 6600.0,
                destAccount = "BINANCE-BOB"
            ).getOrThrow()
        }

        val submitDuplicate = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.submitTxRef(order2.id, uniqueRef)
        }
        assertTrue("Expected duplicate txRef to fail", submitDuplicate.isFailure)
    }

    @Test
    fun observeUserOrders_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = AleviniRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeUserOrders("some_uid").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun updateExchangeConfig_nonAdminAndUnverifiedSpoof_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = AleviniRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateExchangeConfig(ExchangeConfig(usdToHtgBuy = 140.0))
        }
        assertTrue("Non-admin must not update exchange_config", result.isFailure)
        val fsErr = result.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)

        // Unverified email spoofing admin email in emulator must also fail
        signInTestUser(AleviniRepository.ADMIN_EMAIL)
        val spoofRepo = AleviniRepository(firestore, auth)
        val spoofResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            spoofRepo.updateExchangeConfig(ExchangeConfig(usdToHtgBuy = 140.0))
        }
        assertTrue("Unverified email must not bypass isAdmin()", spoofResult.isFailure)
    }

    @Test
    fun adminNotifications_nonAdminCannotSendOrDelete_andUserCanObserveAlignedQuery() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = AleviniRepository(firestore, auth)

        val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.sendAdminNotification(
                title = "Promo",
                message = "Test non admin",
                targetUserId = "ALL",
                targetLabel = "Tous",
                isAlert = false,
            )
        }
        assertTrue("Non-admin must not send admin notification", sendResult.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            sendResult.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val order = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                config = ExchangeConfig(),
                userEmail = ALICE_EMAIL,
                sendMethod = PaymentMethod.MONCASH,
                receiveMethod = PaymentMethod.BINANCE,
                sendCurrency = CurrencyCode.HTG,
                receiveCurrency = CurrencyCode.USD,
                cryptoCoin = null,
                sendAmount = 13200.0,
                destAccount = "BINANCE-UID-1111"
            ).getOrThrow()
        }

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deleteOrder(order.id)
        }
        assertTrue("Non-admin must not delete order", deleteResult.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            deleteResult.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val notifs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserAdminNotifications(uid).first()
        }
        assertTrue(notifs.isEmpty() || notifs.all { it.targetUserId == "ALL" || it.targetUserId == uid })
    }

    private companion object {
        const val ALICE_EMAIL = "alice_alevini@test.com"
        const val BOB_EMAIL = "bob_alevini@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
