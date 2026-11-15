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

  // Admin can delete notification and order
  await assertSucceeds(adminDb.collection("admin_notifications").doc("notif_all").delete());
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_del").set(validOrderPayload(ALICE_UID))
  );
  await assertFails(aliceDb.collection("orders").doc("order_del").delete());
  await assertSucceeds(adminDb.collection("orders").doc("order_del").delete());
});
