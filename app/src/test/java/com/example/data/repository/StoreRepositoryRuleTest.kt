package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreCatalog
import com.example.data.model.StoreConfig
import com.example.data.model.StoreOrderStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createStoreOrder_requiresFundedGateway_enforcesSegregatedDebitAndOwnerIsolation() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aleviniRepo = AleviniRepository(firestore, auth)
        val repo = StoreRepository(firestore, auth)
        val product = StoreCatalog.findById("ff_100")!!

        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                aleviniRepo.ensureUserProfile(aliceUid, ALICE_EMAIL, "Alice Store").isSuccess
            }
        )

        // 1. Store order MUST fail when selected gateway balance is 0
        val unfundedRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createStoreOrder(
                storeConfig = StoreConfig(),
                userEmail = ALICE_EMAIL,
                product = product,
                gateway = PaymentMethod.MONCASH,
                targetAccount = "2459876120",
            )
        }
        assertTrue("Unfunded store order must fail", unfundedRes.isFailure)

        // 2. Verified Admin credits ONLY Alice's MonCash & NatCash balances (Binance/NOWPayments stay 0)
        signInVerifiedAdmin(AleviniRepository.ADMIN_EMAIL)
        val adminAleviniRepo = AleviniRepository(firestore, auth)
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                adminAleviniRepo.adminAdjustGatewayBalance(aliceUid, PaymentMethod.MONCASH, 5000.0).isSuccess
            }
        )
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                adminAleviniRepo.adminAdjustGatewayBalance(aliceUid, PaymentMethod.NATCASH, 10000.0).isSuccess
            }
        )

        // 3. Sign back in as Alice
        signInTestUser(ALICE_EMAIL)

        // Cross-gateway check: Binance has 0.0 USD, so ordering via Binance Pay MUST fail
        val unfundedBinance = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createStoreOrder(
                storeConfig = StoreConfig(),
                userEmail = ALICE_EMAIL,
                product = product,
                gateway = PaymentMethod.BINANCE,
                targetAccount = "2459876120",
            )
        }
        assertTrue("Binance order must fail when balanceBinanceUsd == 0", unfundedBinance.isFailure)

        // Funded MonCash Store order succeeds & atomically debits balanceMoncashHtg ONLY
        val createdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createStoreOrder(
                storeConfig = StoreConfig(),
                userEmail = ALICE_EMAIL,
                product = product,
                gateway = PaymentMethod.MONCASH,
                targetAccount = "2459876120",
            )
        }
        assertTrue("Expected createStoreOrder success, got ${createdResult.exceptionOrNull()}", createdResult.isSuccess)
        val order = createdResult.getOrThrow()
        assertEquals(StoreOrderStatus.PENDING.name, order.status)
        assertEquals("WALLET_DEBITED", order.gatewayStatus)
        assertTrue(order.orderCode.startsWith("STK-"))

        val profileAfterMc = withTimeout(FLOW_TIMEOUT_MS) {
            aleviniRepo.observeUserProfile(aliceUid).first { (it?.balanceMoncashHtg ?: 5000.0) < 5000.0 }
        }!!
        assertEquals(5000.0 - order.amountHtg, profileAfterMc.balanceMoncashHtg, 0.01)
        assertEquals(10000.0, profileAfterMc.balanceNatcashHtg, 0.01)

        val userOrders = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserStoreOrders(aliceUid).first { list -> list.any { it.id == order.id } }
        }
        assertTrue(userOrders.any { it.id == order.id })

        // Non-admin cannot read admin-only Netflix credential vault
        var netflixCaught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeNetflixInventory().first()
            }
        } catch (t: Throwable) {
            netflixCaught = t
        }
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            netflixCaught?.unwrapFirestoreException()?.code,
        )

        // Non-admin cannot update StoreConfig pricing/balances
        val saveCfgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.adminSaveStoreConfig(StoreConfig(baseRateHtg = 99.0))
        }
        assertTrue("Non-admin must fail adminSaveStoreConfig", saveCfgResult.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            saveCfgResult.exceptionOrNull()?.unwrapFirestoreException()?.code,
        )

        // Owner CANNOT delete WALLET_DEBITED store order cleanly (prevents double-refund / audit loss)
        val delWalletOrder = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deletePendingOrExpiredStoreOrder(order.id)
        }
        assertTrue("Owner must NOT delete WALLET_DEBITED store order", delWalletOrder.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            delWalletOrder.exceptionOrNull()?.unwrapFirestoreException()?.code,
        )

        // Admin rejects WALLET_DEBITED order -> refunds balanceMoncashHtg back to 5000.0
        signInVerifiedAdmin(AleviniRepository.ADMIN_EMAIL)
        val adminStoreRepo = StoreRepository(firestore, auth)
        val rejectRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminStoreRepo.adminRejectStoreOrder(order, "Remboursement test Store")
        }
        assertTrue("Expected adminRejectStoreOrder success, got ${rejectRes.exceptionOrNull()}", rejectRes.isSuccess)

        signInTestUser(ALICE_EMAIL)
        val profileAfterRefund = withTimeout(FLOW_TIMEOUT_MS) {
            aleviniRepo.observeUserProfile(aliceUid).first { (it?.balanceMoncashHtg ?: 0.0) >= 5000.0 }
        }!!
        assertEquals(5000.0, profileAfterRefund.balanceMoncashHtg, 0.01)

        // Second Store order using NatCash + txRef submission
        val secondOrder = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createStoreOrder(
                storeConfig = StoreConfig(),
                userEmail = ALICE_EMAIL,
                product = StoreCatalog.findById("sl_res")!!,
                gateway = PaymentMethod.NATCASH,
                targetAccount = "KIT0098123",
            ).getOrThrow()
        }
        val proofResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitStoreTxRef(secondOrder.id, "NC-${UUID.randomUUID().toString().take(8).uppercase()}")
        }
        assertTrue("Expected submitStoreTxRef success, got ${proofResult.exceptionOrNull()}", proofResult.isSuccess)

        // Bob cannot read or query Alice's store order
        signInTestUser(BOB_EMAIL)
        val bobRepo = StoreRepository(firestore, auth)
        val bobOrders = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeUserStoreOrders(auth.currentUser!!.uid).first()
        }
        assertTrue(bobOrders.none { it.id == secondOrder.id })
    }

    private companion object {
        const val ALICE_EMAIL = "alice_store@test.com"
        const val BOB_EMAIL = "bob_store@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
