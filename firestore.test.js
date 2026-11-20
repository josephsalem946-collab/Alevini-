const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_joseph";
const ADMIN_EMAIL = "josephsalem946@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function validOrderPayload(uid, overrides = {}) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    userEmail: "alice@example.com",
    orderCode: "ALV-482910",
    sendMethod: "moncash",
    receiveMethod: "binance",
    sendCurrency: "HTG",
    receiveCurrency: "USD",
    cryptoCoin: "",
    sendAmount: 13200,
    receiveAmount: 97.0,
    amountUsd: 97.0,
    amountHtg: 12804.0,
    amountEur: 89.81,
    baseRate: 0.007576,
    feePercent: 3.0,
    effectiveRate: 0.007348,
    destAccount: "BINANCE-12345678",
    paymentInstruction: "Numéro Marchand MonCash : +509 4687 7695",
    payAddress: "",
    payCryptoAmount: 0,
    externalTxRef: "",
    status: "PENDING",
    adminNote: "",
    expiresAtMillis: Date.now() + 900000,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validConfigPayload(uid) {
  return {
    usdToHtgBuy: 134.5,
    usdToHtgSell: 132.0,
    eurToUsd: 1.08,
    feeNowpayments: 1.5,
    feeBinance: 1.0,
    feeMeru: 1.5,
    feeMoncash: 2.0,
    feeNatcash: 2.0,
    minOrderUsd: 5.0,
    maxOrderUsd: 5000.0,
    moncashMerchant: "+509 4687 7695",
    natcashMerchant: "+509 3215 3281",
    binancePayId: "ALEVINI-BINANCE-PAY",
    meruTag: "@alevini.ht",
    backendBaseUrl: "https://api.alevini.example.com",
    gatewayMode: "SANDBOX",
    moncashOnlineEnabled: true,
    natcashOnlineEnabled: true,
    binanceOnlineEnabled: true,
    nowpaymentsOnlineEnabled: true,
    updatedAt: pastTimestamp(),
    updatedBy: uid,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read orders or config", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("exchange_config").doc("default").get());
});

test("Authenticated user can create valid PENDING order and query own orders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1").set(validOrderPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user fails unfiltered orders list query (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("orders").get());
});

test("Cross-user isolation: Bob cannot read or modify Alice's order", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("orders").doc("order_alice").set(validOrderPayload(ALICE_UID));
  });
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("orders").doc("order_alice").get());
  await assertFails(bobDb.collection("orders").doc("order_alice").update({
    status: "SUBMITTED",
    externalTxRef: "MC-999999",
    updatedAt: pastTimestamp(),
  }));
});

test("User cannot create COMPLETED order directly or inject shadow fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("orders").doc("order_cheat").set(validOrderPayload(ALICE_UID, { status: "COMPLETED" }))
  );
  await assertFails(
    aliceDb.collection("orders").doc("order_ghost").set(validOrderPayload(ALICE_UID, { isVerified: true }))
  );
});

test("User can submit externalTxRef on own PENDING order and claim tx_ref once", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_2").set(validOrderPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("tx_refs").doc("MC_884422").set({
      userId: ALICE_UID,
      orderId: "order_2",
      createdAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_2").update({
      externalTxRef: "MC-884422",
      status: "SUBMITTED",
      updatedAt: pastTimestamp(),
    })
  );
  // Anti-fraud: Bob cannot overwrite or reuse the claimed tx_ref
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("tx_refs").doc("MC_884422").set({
      userId: BOB_UID,
      orderId: "order_bob",
      createdAt: pastTimestamp(),
    })
  );
});

test("Non-admin cannot update exchange_config; spoofed unverified admin email is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  await assertFails(
    aliceDb.collection("exchange_config").doc("default").set(validConfigPayload(ALICE_UID))
  );

  // Email spoofing test: matching admin email but email_verified = false
  const spoofDb = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  await assertFails(
    spoofDb.collection("exchange_config").doc("default").set(validConfigPayload("spoof_uid"))
  );
});

test("Verified Admin can update exchange_config, complete orders, and write ledger entries", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("exchange_config").doc("default").set(validConfigPayload(ADMIN_UID))
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_3").set(validOrderPayload(ALICE_UID))
  );
  await assertSucceeds(
    adminDb.collection("orders").doc("order_3").update({
      status: "COMPLETED",
      adminNote: "Paiement vérifié et envoyé",
      completedAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    adminDb.collection("ledger_entries").doc("ledger_1").set({
      orderId: "order_3",
      orderCode: "ALV-482910",
      accountName: "CAISSE_BINANCE",
      entryType: "DEBIT",
      currency: "USD",
      amount: 97.0,
      note: "Paiement envoyé au client",
      createdAt: pastTimestamp(),
    })
  );
});

test("User cannot unblock themselves after admin blocks them, and blocked user cannot create orders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const ts = pastTimestamp();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      phoneNumber: "+509 3700 0000",
      isAgeVerified: true,
      isBlocked: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      isBlocked: true,
      updatedAt: pastTimestamp(),
    })
  );

  // Alice cannot self-unblock
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isBlocked: false,
      updatedAt: pastTimestamp(),
    })
  );

  // Blocked Alice cannot create new orders
  await assertFails(
    aliceDb.collection("orders").doc("order_blocked").set(validOrderPayload(ALICE_UID))
  );
});

test("Admin can send, list, and delete notifications; users can query targeted/ALL notifications only and cannot delete or send", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(
    adminDb.collection("admin_notifications").doc("notif_all").set({
      title: "Mise à jour des taux",
      message: "Nouveau taux HTG disponible dès maintenant.",
      targetUserId: "ALL",
      targetLabel: "Tous les utilisateurs",
      isAlert: false,
      sentBy: ADMIN_UID,
      createdAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("admin_notifications").doc("notif_alice").set({
      title: "Message privé",
      message: "Votre transfert Alice est confirmé.",
      targetUserId: ALICE_UID,
      targetLabel: "alice@example.com",
      isAlert: false,
      sentBy: ADMIN_UID,
      createdAt: pastTimestamp(),
    })
  );

  // Non-admin cannot create or delete notifications
  await assertFails(
    aliceDb.collection("admin_notifications").doc("notif_fake").set({
      title: "Fake",
      message: "Fake",
      targetUserId: "ALL",
      targetLabel: "ALL",
      isAlert: false,
      sentBy: ALICE_UID,
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(aliceDb.collection("admin_notifications").doc("notif_all").delete());

  // Alice can query ALL + own notifications using whereIn
  await assertSucceeds(
    aliceDb.collection("admin_notifications").where("targetUserId", "in", ["ALL", ALICE_UID]).get()
  );

  // Bob cannot read Alice's private notification
  await assertFails(bobDb.collection("admin_notifications").doc("notif_alice").get());
  await assertFails(bobDb.collection("admin_notifications").get());

  // Admin can delete notification and any order; owner can delete own PENDING order but not SUBMITTED/COMPLETED
  await assertSucceeds(adminDb.collection("admin_notifications").doc("notif_all").delete());
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_del_pending").set(validOrderPayload(ALICE_UID))
  );
  await assertFails(bobDb.collection("orders").doc("order_del_pending").delete());
  await assertSucceeds(aliceDb.collection("orders").doc("order_del_pending").delete());

  await assertSucceeds(
    aliceDb.collection("orders").doc("order_del_locked").set(validOrderPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_del_locked").update({
      status: "EXPIRED",
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(aliceDb.collection("orders").doc("order_del_locked").delete());

  await assertSucceeds(
    aliceDb.collection("orders").doc("order_del_submitted").set(validOrderPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_del_submitted").update({
      externalTxRef: "MC-900112",
      status: "SUBMITTED",
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(aliceDb.collection("orders").doc("order_del_submitted").delete());
  await assertSucceeds(adminDb.collection("orders").doc("order_del_submitted").delete());
});

test("User can attach online checkout session metadata to PENDING order without PERMISSION_DENIED, but cannot spoof CONFIRMED or PAID_IN", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("orders").doc("order_online_1").set(validOrderPayload(ALICE_UID))
  );

  // Legitimate online checkout initiation update succeeds
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_online_1").update({
      gatewayMode: "ONLINE",
      gatewayOrderId: "MC-LIVE-998811",
      checkoutUrl: "https://moncashbutton.digicelgroup.com/Moncash-middleware/Payment/Redirect?token=abc",
      gatewayStatus: "INITIATED",
      updatedAt: pastTimestamp(),
    })
  );

  // Spoofing CONFIRMED or PAID_IN from client fails
  await assertFails(
    aliceDb.collection("orders").doc("order_online_1").update({
      gatewayStatus: "CONFIRMED",
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("orders").doc("order_online_1").update({
      status: "PAID_IN",
      updatedAt: pastTimestamp(),
    })
  );

  // Payment session creation & aligned query succeed
  await assertSucceeds(
    aliceDb.collection("payment_sessions").doc("sess_1").set({
      userId: ALICE_UID,
      orderId: "order_online_1",
      orderCode: "ALV-482910",
      provider: "moncash",
      gatewayMode: "SANDBOX",
      gatewayOrderId: "MC-LIVE-998811",
      checkoutUrl: "https://moncashbutton.digicelgroup.com/Moncash-middleware/Payment/Redirect?token=abc",
      amount: 13200,
      currency: "HTG",
      status: "INITIATED",
      webhookVerified: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Client cannot self-verify webhook
  await assertFails(
    aliceDb.collection("payment_sessions").doc("sess_spoof").set({
      userId: ALICE_UID,
      orderId: "order_online_1",
      orderCode: "ALV-482910",
      provider: "moncash",
      gatewayMode: "LIVE",
      gatewayOrderId: "MC-FAKE",
      checkoutUrl: "https://example.com",
      amount: 13200,
      currency: "HTG",
      status: "CONFIRMED",
      webhookVerified: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Aligned userId query succeeds; unfiltered list and cross-user read fail
  await assertSucceeds(
    aliceDb.collection("payment_sessions").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("payment_sessions").get());
  await assertFails(bobDb.collection("payment_sessions").doc("sess_1").get());
});

test("Webhook events are backend/admin only: clients cannot read or write, admin can read and delete", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("webhook_events").doc("evt_now_1").set({
      provider: "nowpayments",
      eventId: "np_1001",
      orderId: "order_online_1",
      gatewayOrderId: "NP-9981",
      signatureValid: true,
      amountPaid: 100.0,
      currency: "USD",
      outcome: "PAID_IN",
      receivedAt: pastTimestamp(),
    });
  });

  // Non-admin cannot read or forge webhook_events
  await assertFails(aliceDb.collection("webhook_events").doc("evt_now_1").get());
  await assertFails(aliceDb.collection("webhook_events").get());
  await assertFails(
    aliceDb.collection("webhook_events").doc("evt_fake").set({
      provider: "binance",
      eventId: "fake",
      orderId: "order_online_1",
      signatureValid: true,
      outcome: "PAID_IN",
      receivedAt: pastTimestamp(),
    })
  );

  // Admin can read and delete
  await assertSucceeds(adminDb.collection("webhook_events").get());
  await assertSucceeds(adminDb.collection("webhook_events").doc("evt_now_1").delete());
});

function validStoreConfigPayload(uid) {
  return {
    baseRateHtg: 132.5,
    markupPercent: 7.0,
    reloadlyBalanceUsd: 420.5,
    meruBalanceUsd: 185.0,
    reloadlyActive: true,
    ezpinActive: true,
    moncashActive: true,
    natcashActive: true,
    binanceActive: true,
    nowpaymentsActive: true,
    reloadlyLatencyMs: 28,
    ezpinLatencyMs: 42,
    availableNetflixCount: 4,
    pendingStarlinkCount: 1,
    updatedBy: uid,
    updatedAt: pastTimestamp(),
  };
}

function validStoreOrderPayload(uid, overrides = {}) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    userEmail: "alice@example.com",
    orderCode: "STK-501290",
    productId: "nf_1",
    productTitle: "Netflix 4K Ultra HD (1 Écran)",
    category: "netflix",
    deliveryType: "netflix_profile",
    usdCost: 3.5,
    usdSell: 5.5,
    amountHtg: 780.0,
    effectiveRate: 141.78,
    gateway: "moncash",
    targetAccount: "alice@example.com",
    externalTxRef: "",
    gatewayOrderId: "",
    checkoutUrl: "",
    gatewayStatus: "NONE",
    status: "PENDING",
    deliverable: "",
    adminNote: "",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

test("Store security: clients can read store_config, cannot modify store_config or read/write netflix_inventory", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  await assertSucceeds(
    adminDb.collection("store_config").doc("default").set(validStoreConfigPayload(ADMIN_UID))
  );
  await assertSucceeds(aliceDb.collection("store_config").doc("default").get());
  await assertFails(
    aliceDb.collection("store_config").doc("default").set(validStoreConfigPayload(ALICE_UID))
  );

  // Admin creates private Netflix profile in store_netflix_inventory
  await assertSucceeds(
    adminDb.collection("store_netflix_inventory").doc("nf_slot_1").set({
      accountEmail: "vip1@alevini.com",
      accountPassword: "SecretPassword#2026",
      profileName: "Écran 1",
      profilePin: "4821",
      status: "AVAILABLE",
      assignedOrderId: "",
      assignedUserId: "",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Normal clients NEVER see raw Netflix credentials
  await assertFails(aliceDb.collection("store_netflix_inventory").doc("nf_slot_1").get());
  await assertFails(aliceDb.collection("store_netflix_inventory").get());
  await assertFails(
    aliceDb.collection("store_netflix_inventory").doc("nf_hack").set({
      accountEmail: "hack@alevini.com",
      accountPassword: "123",
      profileName: "Hack",
      profilePin: "0000",
      status: "AVAILABLE",
      assignedOrderId: "",
      assignedUserId: "",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(adminDb.collection("store_netflix_inventory").get());
});

test("Store orders: owner isolation, no self-delivery or fake PAID, proof submission, and admin fulfillment", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  // Client cannot create pre-paid or pre-delivered order
  await assertFails(
    aliceDb.collection("store_orders").doc("st_cheat_1").set(validStoreOrderPayload(ALICE_UID, { status: "DELIVERED", deliverable: "FAKE-PIN" }))
  );
  await assertFails(
    aliceDb.collection("store_orders").doc("st_cheat_2").set(validStoreOrderPayload(ALICE_UID, { status: "PAID" }))
  );

  // Valid PENDING store order succeeds
  await assertSucceeds(
    aliceDb.collection("store_orders").doc("st_1").set(validStoreOrderPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("store_orders").where("userId", "==", ALICE_UID).get()
  );

  // Bob cannot read or mutate Alice's store order
  await assertFails(bobDb.collection("store_orders").doc("st_1").get());
  await assertFails(bobDb.collection("store_orders").get());

  // Alice cannot self-deliver or write deliverable field
  await assertFails(
    aliceDb.collection("store_orders").doc("st_1").update({
      status: "DELIVERED",
      deliverable: "SPOOF-CODE-999",
      updatedAt: pastTimestamp(),
    })
  );

  // Alice CAN submit txRef proof (PENDING -> SUBMITTED)
  await assertSucceeds(
    aliceDb.collection("store_orders").doc("st_1").update({
      externalTxRef: "MC-STORE-7721",
      status: "SUBMITTED",
      updatedAt: pastTimestamp(),
    })
  );

  // Alice cannot delete SUBMITTED order (anti-fraud audit trail)
  await assertFails(aliceDb.collection("store_orders").doc("st_1").delete());

  // Admin can deliver the Netflix credentials cleanly
  await assertSucceeds(
    adminDb.collection("store_orders").doc("st_1").update({
      status: "DELIVERED",
      deliverable: "Email: vip1@alevini.com | PIN: 4821",
      adminNote: "Livré automatiquement par Admin",
      updatedAt: pastTimestamp(),
    })
  );

  // Owner CAN delete own PENDING order
  await assertSucceeds(
    aliceDb.collection("store_orders").doc("st_del_pending").set(validStoreOrderPayload(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("store_orders").doc("st_del_pending").delete());
});

test("Segregated per-gateway wallets: zero initial balances, non-increasing client rule, TOPUP completion, and atomic WALLET debit for Exchange & Store", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const ts = pastTimestamp();

  // Client CANNOT initialize a profile with non-zero gateway balance
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      phoneNumber: "+509 3700 0000",
      isAgeVerified: true,
      isBlocked: false,
      balanceMoncashHtg: 5000,
      balanceNatcashHtg: 0,
      balanceBinanceUsd: 0,
      balanceMeruUsd: 0,
      balanceNowpaymentsUsd: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid zero-balance profile creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      phoneNumber: "+509 3700 0000",
      isAgeVerified: true,
      isBlocked: false,
      balanceMoncashHtg: 0,
      balanceNatcashHtg: 0,
      balanceBinanceUsd: 0,
      balanceMeruUsd: 0,
      balanceNowpaymentsUsd: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Client CANNOT self-credit any gateway balance
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      balanceMoncashHtg: 15000,
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      balanceBinanceUsd: 50,
      updatedAt: pastTimestamp(),
    })
  );

  // Client CAN create a TOPUP order (same sendMethod & receiveMethod, including Meru)
  await assertSucceeds(
    aliceDb.collection("orders").doc("topup_mc_1").set(
      validOrderPayload(ALICE_UID, {
        orderCode: "TOP-112233",
        orderType: "TOPUP",
        gatewayMode: "MANUAL",
        sendMethod: "moncash",
        receiveMethod: "moncash",
        sendCurrency: "HTG",
        receiveCurrency: "HTG",
        sendAmount: 20000,
        receiveAmount: 19600,
        amountHtg: 19600,
        amountUsd: 145.72,
        destAccount: "Portefeuille MonCash (HTG)",
      })
    )
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("topup_meru_1").set(
      validOrderPayload(ALICE_UID, {
        orderCode: "TOP-445566",
        orderType: "TOPUP",
        gatewayMode: "MANUAL",
        sendMethod: "meru",
        receiveMethod: "meru",
        sendCurrency: "USD",
        receiveCurrency: "USD",
        sendAmount: 100,
        receiveAmount: 98.5,
        amountUsd: 98.5,
        amountHtg: 13002,
        destAccount: "Portefeuille Meru (USD)",
      })
    )
  );

  // Admin credits Alice's MonCash balance ONLY (leaving NatCash/Binance/Meru/NOWPayments at 0)
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      balanceMoncashHtg: 19600,
      updatedAt: pastTimestamp(),
    })
  );

  // Client CANNOT forge a WALLET exchange order without atomically debiting users/{uid}
  await assertFails(
    aliceDb.collection("orders").doc("wallet_cheat_1").set(
      validOrderPayload(ALICE_UID, {
        gatewayMode: "WALLET",
        orderType: "EXCHANGE",
        sendMethod: "moncash",
        receiveMethod: "binance",
        sendAmount: 13200,
      })
    )
  );

  // Atomic batch debiting MonCash AND creating WALLET exchange order SUCCEEDS
  {
    const batch = aliceDb.batch();
    batch.update(aliceDb.collection("users").doc(ALICE_UID), {
      balanceMoncashHtg: 6400,
      updatedAt: pastTimestamp(),
    });
    batch.set(
      aliceDb.collection("orders").doc("wallet_ok_1"),
      validOrderPayload(ALICE_UID, {
        gatewayMode: "WALLET",
        orderType: "EXCHANGE",
        sendMethod: "moncash",
        receiveMethod: "binance",
        sendAmount: 13200,
      })
    );
    await assertSucceeds(batch.commit());
  }

  // Cross-gateway isolation: debiting MonCash while creating a NatCash WALLET order FAILS!
  {
    const batch = aliceDb.batch();
    batch.update(aliceDb.collection("users").doc(ALICE_UID), {
      balanceMoncashHtg: 1000,
      updatedAt: pastTimestamp(),
    });
    batch.set(
      aliceDb.collection("orders").doc("wallet_cross_fail"),
      validOrderPayload(ALICE_UID, {
        gatewayMode: "WALLET",
        orderType: "EXCHANGE",
        sendMethod: "natcash",
        receiveMethod: "binance",
        sendAmount: 5000,
      })
    );
    await assertFails(batch.commit());
  }

  // Client CANNOT delete a WALLET-debited exchange order (prevents double-refund / audit loss)
  await assertFails(aliceDb.collection("orders").doc("wallet_ok_1").delete());

  // Store WALLET_DEBITED requires atomic debit on exact selected gateway
  await assertFails(
    aliceDb.collection("store_orders").doc("st_wallet_cheat").set(
      validStoreOrderPayload(ALICE_UID, {
        gateway: "moncash",
        gatewayStatus: "WALLET_DEBITED",
        checkoutUrl: "WALLET",
      })
    )
  );

  {
    const batch = aliceDb.batch();
    batch.update(aliceDb.collection("users").doc(ALICE_UID), {
      balanceMoncashHtg: 5620,
      updatedAt: pastTimestamp(),
    });
    batch.set(
      aliceDb.collection("store_orders").doc("st_wallet_ok"),
      validStoreOrderPayload(ALICE_UID, {
        gateway: "moncash",
        gatewayStatus: "WALLET_DEBITED",
        checkoutUrl: "WALLET",
      })
    );
    await assertSucceeds(batch.commit());
  }

  // Client CANNOT delete WALLET_DEBITED store order
  await assertFails(aliceDb.collection("store_orders").doc("st_wallet_ok").delete());
});

test("Digital Hub Store: 6 enriched categories, fulfillmentMode, activity logs, and admin-published store_products", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    adminDb.collection("store_config").doc("default").set({
      ...validStoreConfigPayload(ADMIN_UID),
      fulfillmentMode: "MANUAL",
    })
  );

  const customProductPayload = {
    category: "ai_pro",
    deliveryType: "starlink",
    badge: "MERU VCC",
    title: "Perplexity Pro (1 Mois)",
    subtitle: "Abonnement réglé via Carte Meru",
    usdCost: 17.5,
    usdSell: 20.5,
    requiresTargetAccount: true,
    targetAccountLabel: "Email compte Perplexity",
    targetAccountHint: "client@email.com",
    selectLabel: "Formule",
    selectOptions: ["Mensuel Pro"],
    stock: 50,
    createdBy: ADMIN_UID,
    createdAt: ts,
    updatedAt: ts,
  };
  await assertFails(
    aliceDb.collection("store_products").doc("custom_perp_1").set({
      ...customProductPayload,
      createdBy: ALICE_UID,
    })
  );
  await assertSucceeds(
    adminDb.collection("store_products").doc("custom_perp_1").set(customProductPayload)
  );
  await assertSucceeds(aliceDb.collection("store_products").get());
  await assertFails(aliceDb.collection("store_products").doc("custom_perp_1").delete());

  for (const cat of ["streaming", "telecom", "cards", "ai_pro", "web_travel"]) {
    await assertSucceeds(
      aliceDb.collection("store_orders").doc(`st_${cat}`).set(
        validStoreOrderPayload(ALICE_UID, {
          category: cat,
          logs: ["12:00:00 — Commande créée par le client."],
        })
      )
    );
  }

  await assertSucceeds(
    adminDb.collection("store_orders").doc("st_ai_pro").update({
      status: "DELIVERED",
      deliverable: "Licence #AI-2026 activée",
      adminNote: "Livré via Régie Admin",
      logs: [
        "12:00:00 — Commande créée par le client.",
        "12:01:15 — Validé & Exécuté par Admin.",
      ],
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(adminDb.collection("store_products").doc("custom_perp_1").delete());
});

function validInvestmentPayload(uid, overrides = {}) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    userEmail: "alice@example.com",
    investCode: "INV-8512",
    categoryKey: "sans_risque",
    categoryName: "Investissement sans risque",
    sector: "Capital Garanti MCSE",
    amount: 50000,
    currency: "GDS",
    durationLabel: "3 mois",
    months: 3,
    monthlyProfit: 400,
    totalProfit: 1200,
    gatewayId: "moncash",
    gatewayName: "MonCash",
    feeAmount: 750,
    totalPaid: 50750,
    endDateLabel: "18 févr. 2027",
    status: "PENDING",
    adminNote: "",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

test("MCSE Investments: atomic wallet debit required, owner isolation, no self-approval, and admin decision/refund/delete", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastTimestamp();

  // Setup Alice profile and fund MonCash balance to 100000 HTG
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      phoneNumber: "+509 3700 0000",
      isAgeVerified: true,
      isBlocked: false,
      balanceMoncashHtg: 0,
      balanceNatcashHtg: 0,
      balanceBinanceUsd: 0,
      balanceMeruUsd: 0,
      balanceNowpaymentsUsd: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      balanceMoncashHtg: 100000,
      updatedAt: pastTimestamp(),
    })
  );

  // Unfunded or non-atomic investment creation FAILS
  await assertFails(
    aliceDb.collection("investments").doc("inv_no_debit").set(validInvestmentPayload(ALICE_UID))
  );

  // Spoofed ACTIVE initial status FAILS even with debit
  {
    const batch = aliceDb.batch();
    batch.update(aliceDb.collection("users").doc(ALICE_UID), {
      balanceMoncashHtg: 49250,
      updatedAt: pastTimestamp(),
    });
    batch.set(
      aliceDb.collection("investments").doc("inv_cheat_status"),
      validInvestmentPayload(ALICE_UID, { status: "ACTIVE" })
    );
    await assertFails(batch.commit());
  }

  // Cross-gateway debit FAILS (debiting MonCash for a NatCash investment)
  {
    const batch = aliceDb.batch();
    batch.update(aliceDb.collection("users").doc(ALICE_UID), {
      balanceMoncashHtg: 49250,
      updatedAt: pastTimestamp(),
    });
    batch.set(
      aliceDb.collection("investments").doc("inv_cross_gw"),
      validInvestmentPayload(ALICE_UID, { gatewayId: "natcash", gatewayName: "NatCash" })
    );
    await assertFails(batch.commit());
  }

  // Valid atomic MonCash debit + investment creation SUCCEEDS
  {
    const batch = aliceDb.batch();
    batch.update(aliceDb.collection("users").doc(ALICE_UID), {
      balanceMoncashHtg: 49250,
      updatedAt: pastTimestamp(),
    });
    batch.set(
      aliceDb.collection("investments").doc("inv_valid_1"),
      validInvestmentPayload(ALICE_UID)
    );
    await assertSucceeds(batch.commit());
  }

  // Owner can query own investments; unfiltered list and cross-user read fail
  await assertSucceeds(aliceDb.collection("investments").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("investments").get());
  await assertFails(bobDb.collection("investments").doc("inv_valid_1").get());

  // Owner cannot self-approve or delete PENDING/ACTIVE debited investment
  await assertFails(
    aliceDb.collection("investments").doc("inv_valid_1").update({
      status: "ACTIVE",
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(aliceDb.collection("investments").doc("inv_valid_1").delete());

  // Admin can read all and approve/reject
  await assertSucceeds(adminDb.collection("investments").get());
  await assertSucceeds(
    adminDb.collection("investments").doc("inv_valid_1").update({
      status: "ACTIVE",
      adminNote: "Activé par Admin",
      updatedAt: pastTimestamp(),
    })
  );

  // When Admin rejects an investment, user CAN delete the REJECTED record cleanly
  await assertSucceeds(
    adminDb.collection("investments").doc("inv_valid_1").update({
      status: "REJECTED",
      adminNote: "Refusé pour test",
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(aliceDb.collection("investments").doc("inv_valid_1").delete());
});



