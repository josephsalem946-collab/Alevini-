package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeConfig
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AleviniRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun topUp_segregatedBalances_atomicExchangeDebit_andAdminRejectRefund() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = AleviniRepository(firestore, auth)
        val config = ExchangeConfig()

        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.ensureUserProfile(aliceUid, ALICE_EMAIL, "Alice").isSuccess
            }
        )

        // 1. Exchange fails before recharging the selected gateway
        val unfundedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                config = config,
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
        assertTrue("Unfunded exchange must fail", unfundedResult.isFailure)

        // 2. Client creates TOPUP order on MonCash (20,000 HTG gross -> 2% fee = 19,600 HTG net)
        val topUp = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createTopUpOrder(
                config = config,
                userEmail = ALICE_EMAIL,
                gatewayMethod = PaymentMethod.MONCASH,
                cryptoCoin = null,
                amount = 20000.0,
            ).getOrThrow()
        }
        assertEquals("TOPUP", topUp.orderType)
        assertEquals(19600.0, topUp.receiveAmount, 0.01)

        // 3. Verified Admin completes TOPUP and credits MonCash balance ONLY
        signInVerifiedAdmin(AleviniRepository.ADMIN_EMAIL)
        val adminRepo = AleviniRepository(firestore, auth)
        val completeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.completeOrder(topUp, "Recharge vérifiée")
        }
        assertTrue("Expected admin completeOrder success, got ${completeRes.exceptionOrNull()}", completeRes.isSuccess)

        // 4. Sign back in as Alice and verify segregated wallet balances
        signInTestUser(ALICE_EMAIL)
        val aliceProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(aliceUid).first { (it?.balanceMoncashHtg ?: 0.0) > 0.0 }
        }!!
        assertEquals(19600.0, aliceProfile.balanceMoncashHtg, 0.01)
        assertEquals(0.0, aliceProfile.balanceNatcashHtg, 0.01)
        assertEquals(0.0, aliceProfile.balanceBinanceUsd, 0.01)
        assertEquals(0.0, aliceProfile.balanceMeruUsd, 0.01)
        assertEquals(0.0, aliceProfile.balanceNowpaymentsUsd, 0.01)

        // 5. Cross-gateway isolation: NatCash has 0 HTG, so NatCash exchange fails even with 19,600 HTG on MonCash
        val crossGatewayFail = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                config = config,
                userEmail = ALICE_EMAIL,
                sendMethod = PaymentMethod.NATCASH,
                receiveMethod = PaymentMethod.BINANCE,
                sendCurrency = CurrencyCode.HTG,
                receiveCurrency = CurrencyCode.USD,
                cryptoCoin = null,
                sendAmount = 6600.0,
                destAccount = "BINANCE-UID-9988"
            )
        }
        assertTrue("NatCash exchange must fail when only MonCash is funded", crossGatewayFail.isFailure)

        // 6. Exchange using funded MonCash succeeds and atomically debits balanceMoncashHtg ONLY
        val exchangeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                config = config,
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
        assertTrue("Expected funded createOrder success, got ${exchangeResult.exceptionOrNull()}", exchangeResult.isSuccess)
        val order = exchangeResult.getOrThrow()
        assertEquals(OrderStatus.PENDING.name, order.status)
        assertEquals("WALLET", order.gatewayMode)
        assertEquals("EXCHANGE", order.orderType)
        assertTrue(order.orderCode.startsWith("ALV-"))

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserOrders(aliceUid).first { list -> list.any { it.id == order.id } }
        }
        assertTrue(orders.any { it.id == order.id })

        val afterDebitProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(aliceUid).first { (it?.balanceMoncashHtg ?: 0.0) < 19600.0 }
        }!!
        assertEquals(6400.0, afterDebitProfile.balanceMoncashHtg, 0.01)

        // 7. Client cannot self-credit wallet or delete WALLET-debited order
        var creditCheat: Throwable? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(AleviniRepository.COLLECTION_USERS).document(aliceUid)
                    .update(mapOf("balanceMoncashHtg" to 99999.0, "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
            }
        } catch (t: Throwable) {
            creditCheat = t
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, creditCheat?.unwrapFirestoreException()?.code)

        val deleteWalletOrder = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deletePendingOrExpiredOrder(order.id)
        }
        assertTrue("Owner must not delete WALLET-debited order", deleteWalletOrder.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            deleteWalletOrder.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // 8. Admin rejecting WALLET-debited exchange refunds MonCash balance back to 19,600.0 HTG
        signInVerifiedAdmin(AleviniRepository.ADMIN_EMAIL)
        val rejectRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.updateOrderStatus(order.id, OrderStatus.REJECTED, "Remboursement test")
        }
        assertTrue("Expected admin reject/refund success, got ${rejectRes.exceptionOrNull()}", rejectRes.isSuccess)

        signInTestUser(ALICE_EMAIL)
        val refundedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(aliceUid).first { (it?.balanceMoncashHtg ?: 0.0) >= 19600.0 }
        }!!
        assertEquals(19600.0, refundedProfile.balanceMoncashHtg, 0.01)
    }

    @Test
    fun submitTxRef_uniqueRefSucceeds_andDuplicateRefFails() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = AleviniRepository(firestore, auth)
        repo.ensureUserProfile(aliceUid, ALICE_EMAIL, "Alice")

        val order1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createTopUpOrder(
                config = ExchangeConfig(),
                userEmail = ALICE_EMAIL,
                gatewayMethod = PaymentMethod.NATCASH,
                cryptoCoin = null,
                amount = 6600.0,
            ).getOrThrow()
        }

        val uniqueRef = "NC-${UUID.randomUUID().toString().take(8).uppercase()}"
        val submit1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitTxRef(order1.id, uniqueRef)
        }
        assertTrue("Expected submitTxRef success, got ${submit1.exceptionOrNull()}", submit1.isSuccess)

        // Second user tries to reuse the exact same transaction reference
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = AleviniRepository(firestore, auth)
        bobRepo.ensureUserProfile(bobUid, BOB_EMAIL, "Bob")
        val order2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.createTopUpOrder(
                config = ExchangeConfig(),
                userEmail = BOB_EMAIL,
                gatewayMethod = PaymentMethod.NATCASH,
                cryptoCoin = null,
                amount = 6600.0,
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
        signInUnverifiedUser(AleviniRepository.ADMIN_EMAIL)
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
        repo.ensureUserProfile(uid, ALICE_EMAIL, "Alice")

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

        val topUpPending = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createTopUpOrder(
                config = ExchangeConfig(),
                userEmail = ALICE_EMAIL,
                gatewayMethod = PaymentMethod.MONCASH,
                cryptoCoin = null,
                amount = 13200.0,
            ).getOrThrow()
        }

        // Owner can delete own non-wallet PENDING TOPUP order cleanly
        val pendingDeleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deletePendingOrExpiredOrder(topUpPending.id)
        }
        assertTrue("Owner must be allowed to delete PENDING TOPUP order", pendingDeleteResult.isSuccess)

        // Owner cannot delete SUBMITTED order (audit trail protection)
        val submittedOrder = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createTopUpOrder(
                config = ExchangeConfig(),
                userEmail = ALICE_EMAIL,
                gatewayMethod = PaymentMethod.MONCASH,
                cryptoCoin = null,
                amount = 13200.0,
            ).getOrThrow()
        }
        val refClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitTxRef(submittedOrder.id, "MC-${UUID.randomUUID().toString().take(8).uppercase()}")
        }
        assertTrue(refClaim.isSuccess)

        val lockedDeleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deleteOrder(submittedOrder.id)
        }
        assertTrue("Non-admin must not delete SUBMITTED order", lockedDeleteResult.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            lockedDeleteResult.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val notifs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserAdminNotifications(uid).first()
        }
        assertTrue(notifs.isEmpty() || notifs.all { it.targetUserId == "ALL" || it.targetUserId == uid })
    }

    @Test
    fun attachOnlineCheckoutSession_ownerSucceedsWithoutPermissionDenied_andWebhookEventsAreBlockedForNonAdmin() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = AleviniRepository(firestore, auth)
        repo.ensureUserProfile(uid, ALICE_EMAIL, "Alice")

        val order = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createTopUpOrder(
                config = ExchangeConfig(),
                userEmail = ALICE_EMAIL,
                gatewayMethod = PaymentMethod.MONCASH,
                cryptoCoin = null,
                amount = 13200.0,
            ).getOrThrow()
        }

        val attachResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.attachOnlineCheckoutSession(
                order = order,
                gatewayMode = "SANDBOX",
                gatewayOrderId = "MC-ONLINE-7788",
                checkoutUrl = "https://sandbox.moncashbutton.digicelgroup.com/Moncash-middleware/Payment/Redirect?token=test123",
            )
        }
        assertTrue("Expected attachOnlineCheckoutSession success, got ${attachResult.exceptionOrNull()}", attachResult.isSuccess)

        val sessions = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserPaymentSessions(uid).first { list -> list.any { it.orderId == order.id } }
        }
        assertTrue(sessions.any { it.orderId == order.id && it.gatewayOrderId == "MC-ONLINE-7788" })

        // Non-admin reading backend-only webhook_events must receive PERMISSION_DENIED
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeWebhookEvents().first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            caught?.unwrapFirestoreException()?.code
        )
    }

    private companion object {
        const val ALICE_EMAIL = "alice_alevini@test.com"
        const val BOB_EMAIL = "bob_alevini@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
