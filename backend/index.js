require("dotenv").config();
const crypto = require("node:crypto");
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const admin = require("firebase-admin");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const {
  verifyNowPaymentsSignature,
  signBinancePayRequest,
  verifyBinancePayWebhookSignature,
  computeHmacSha256,
  verifyHmacSha256Signature,
} = require("./signatureUtils");

if (!admin.apps.length) {
  admin.initializeApp();
}

const FIRESTORE_DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-appforge-38d1ee97-de3b-420d-9185-d993d1abd89c";

const db = getFirestore(FIRESTORE_DATABASE_ID);
const ADMIN_EMAIL = "josephsalem946@gmail.com";

const app = express();
app.use(helmet());

const allowedOrigins = (process.env.ALLOWED_ORIGINS || "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);

app.use(
  cors({
    origin: (origin, callback) => {
      if (!origin || allowedOrigins.length === 0 || allowedOrigins.includes(origin)) {
        callback(null, true);
      } else {
        callback(new Error("CORS non autorisé"));
      }
    },
  })
);

// Preserve raw body for cryptographic webhook verification
app.use(
  express.json({
    limit: "1mb",
    verify: (req, _res, buf) => {
      req.rawBody = buf.toString("utf8");
    },
  })
);

const apiLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use("/api/", apiLimiter);

/**
 * Middleware: Authenticate Firebase ID Token from Android app & ensure user is not blocked.
 */
async function verifyFirebaseToken(req, res, next) {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Jeton d'authentification Firebase manquant." });
  }
  const idToken = authHeader.slice(7).trim();
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    req.user = decoded;

    const userSnap = await db.collection("users").doc(decoded.uid).get();
    if (userSnap.exists && userSnap.data()?.isBlocked === true) {
      return res.status(403).json({ error: "Compte suspendu par l'administration." });
    }
    return next();
  } catch (err) {
    console.error("Token verification failed:", err.message);
    return res.status(401).json({ error: "Session invalide ou expirée." });
  }
}

function getGatewayHosts() {
  const isLive = (process.env.GATEWAY_MODE || "SANDBOX").trim().toUpperCase() === "LIVE";
  const moncashIsLive =
    (process.env.MONCASH_MODE || process.env.GATEWAY_MODE || "SANDBOX")
      .trim()
      .toUpperCase() === "LIVE";
  return {
    isLive,
    modeLabel: isLive ? "LIVE" : "SANDBOX",
    moncashIsLive,
    moncashModeLabel: moncashIsLive ? "LIVE" : "SANDBOX",
    moncashApiBase: moncashIsLive
      ? "https://moncashbutton.digicelgroup.com/Api"
      : "https://sandbox.moncashbutton.digicelgroup.com/Api",
    moncashWebBase: moncashIsLive
      ? "https://moncashbutton.digicelgroup.com/Moncash-middleware"
      : "https://sandbox.moncashbutton.digicelgroup.com/Moncash-middleware",
    natcashApiBase: process.env.NATCASH_API_URL || "https://api.natcash.ht",
    binanceApiBase: "https://bpay.binanceapi.com",
    nowpaymentsApiBase: "https://api.nowpayments.io/v1",
  };
}

async function getMonCashOAuthToken(apiBase) {
  const clientId = process.env.MONCASH_CLIENT_ID;
  const clientSecret = process.env.MONCASH_CLIENT_SECRET;
  if (!clientId || !clientSecret) {
    throw new Error("Identifiants MonCash non configurés sur le serveur.");
  }
  const credentials = Buffer.from(`${clientId}:${clientSecret}`).toString("base64");
  const resp = await fetch(`${apiBase}/oauth/token`, {
    method: "POST",
    headers: {
      Authorization: `Basic ${credentials}`,
      "Content-Type": "application/x-www-form-urlencoded",
      Accept: "application/json",
    },
    body: "scope=read,write&grant_type=client_credentials",
  });
  if (!resp.ok) {
    throw new Error(`Échec OAuth MonCash (${resp.status})`);
  }
  const data = await resp.json();
  return data.access_token;
}

async function createMonCashPayment(order) {
  const hosts = getGatewayHosts();
  const accessToken = await getMonCashOAuthToken(hosts.moncashApiBase);
  const resp = await fetch(`${hosts.moncashApiBase}/v1/CreatePayment`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({
      amount: order.sendAmount,
      orderId: order.orderCode,
    }),
  });
  if (!resp.ok) {
    throw new Error(`Erreur API MonCash (${resp.status})`);
  }
  const data = await resp.json();
  const token = data?.payment_token?.token;
  if (!token) {
    throw new Error("Jeton de paiement MonCash absent de la réponse.");
  }
  return {
    gatewayOrderId: order.orderCode,
    checkoutUrl: `${hosts.moncashWebBase}/Payment/Redirect?token=${encodeURIComponent(token)}`,
    payAddress: "",
    payCryptoAmount: 0,
    mode: hosts.moncashModeLabel,
  };
}

async function createNatCashPayment(order) {
  const hosts = getGatewayHosts();
  const apiKey = process.env.NATCASH_API_KEY;
  const apiSecret = process.env.NATCASH_API_SECRET || process.env.NATCASH_API_KEY;
  const merchantCode =
    process.env.NATCASH_MERCHANT_ID || process.env.NATCASH_MERCHANT_CODE || "ALEVINI";
  if (!apiKey || !apiSecret) {
    throw new Error("Identifiants NatCash non configurés sur le serveur.");
  }

  const publicBase = (process.env.PUBLIC_BASE_URL || "").replace(/\/$/, "");
  const payload = {
    merchantCode,
    merchantId: merchantCode,
    orderId: order.orderCode,
    amount: order.sendAmount,
    currency: order.sendCurrency,
    callbackUrl: `${publicBase}/webhooks/natcash`,
  };
  const bodyStr = JSON.stringify(payload);
  const signature = computeHmacSha256(bodyStr, apiSecret);

  const resp = await fetch(`${hosts.natcashApiBase}/v1/payments/create`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-API-KEY": apiKey,
      "X-SIGNATURE": signature,
    },
    body: bodyStr,
  });
  if (!resp.ok) {
    throw new Error(`Erreur API NatCash (${resp.status})`);
  }
  const data = await resp.json();
  return {
    gatewayOrderId: String(data.transactionId || data.orderId || order.orderCode),
    checkoutUrl: String(data.checkoutUrl || data.redirectUrl || ""),
    payAddress: "",
    payCryptoAmount: 0,
    mode: hosts.modeLabel,
  };
}

async function createBinancePayOrder(order) {
  const hosts = getGatewayHosts();
  const certSn =
    process.env.BINANCE_PAY_CERT_SN ||
    process.env.BINANCE_PAY_API_KEY ||
    process.env.BINANCE_API_KEY;
  const secretKey = process.env.BINANCE_PAY_SECRET_KEY || process.env.BINANCE_SECRET_KEY;
  if (!certSn || !secretKey) {
    throw new Error("Identifiants Binance Pay non configurés sur le serveur.");
  }

  const timestamp = Date.now().toString();
  const nonce = crypto.randomBytes(16).toString("hex");
  const publicBase = (process.env.PUBLIC_BASE_URL || "").replace(/\/$/, "");
  const merchantTradeNo = `${order.orderCode.replace(/[^A-Za-z0-9]/g, "")}${timestamp.slice(-6)}`.slice(0, 32);

  const bodyObj = {
    env: { terminalType: "APP" },
    merchantTradeNo,
    orderAmount: Number(order.sendAmount.toFixed(2)),
    currency: order.sendCurrency === "EUR" ? "EUR" : "USDT",
    description: `Alevini Exchange ${order.orderCode}`,
    goodsDetails: [
      {
        goodsType: "02",
        goodsCategory: "Z000",
        referenceGoodsId: order.orderCode,
        goodsName: `Alevini ${order.orderCode}`,
      },
    ],
    webhookUrl: `${publicBase}/webhooks/binance`,
  };
  const bodyStr = JSON.stringify(bodyObj);
  const signature = signBinancePayRequest(timestamp, nonce, bodyStr, secretKey);

  const resp = await fetch(`${hosts.binanceApiBase}/binancepay/openapi/v3/order`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "BinancePay-Timestamp": timestamp,
      "BinancePay-Nonce": nonce,
      "BinancePay-Certificate-SN": certSn,
      "BinancePay-Signature": signature,
    },
    body: bodyStr,
  });
  if (!resp.ok) {
    throw new Error(`Erreur API Binance Pay (${resp.status})`);
  }
  const result = await resp.json();
  if (result.status !== "SUCCESS" || !result.data) {
    throw new Error(result.errorMessage || "Réponse Binance Pay invalide.");
  }
  return {
    gatewayOrderId: String(result.data.prepayId || merchantTradeNo),
    checkoutUrl: String(result.data.checkoutUrl || result.data.universalUrl || ""),
    payAddress: String(result.data.qrcodeLink || ""),
    payCryptoAmount: Number(order.sendAmount),
    mode: hosts.modeLabel,
  };
}

async function createNowPaymentsOrder(order) {
  const hosts = getGatewayHosts();
  const apiKey = process.env.NOWPAYMENTS_API_KEY;
  if (!apiKey) {
    throw new Error("Clé API NOWPayments non configurée sur le serveur.");
  }
  const publicBase = (process.env.PUBLIC_BASE_URL || "").replace(/\/$/, "");
  const payCurrency = order.cryptoCoin || "usdttrc20";

  const resp = await fetch(`${hosts.nowpaymentsApiBase}/payment`, {
    method: "POST",
    headers: {
      "x-api-key": apiKey,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      price_amount: order.amountUsd > 0 ? order.amountUsd : order.sendAmount,
      price_currency: "usd",
      pay_currency: payCurrency,
      ipn_callback_url: `${publicBase}/webhooks/nowpayments`,
      order_id: order.orderCode,
      order_description: `Alevini Exchange ${order.orderCode}`,
    }),
  });
  if (!resp.ok) {
    throw new Error(`Erreur API NOWPayments (${resp.status})`);
  }
  const data = await resp.json();
  return {
    gatewayOrderId: String(data.payment_id || order.orderCode),
    checkoutUrl: String(data.invoice_url || ""),
    payAddress: String(data.pay_address || ""),
    payCryptoAmount: Number(data.pay_amount || order.payCryptoAmount || 0),
    mode: hosts.modeLabel,
  };
}

/**
 * POST /api/payments/:provider/create
 * Authenticated endpoint creating a checkout session for moncash | natcash | binance | nowpayments
 */
app.post("/api/payments/:provider/create", verifyFirebaseToken, async (req, res) => {
  try {
    const provider = String(req.params.provider || "").toLowerCase();
    if (!["moncash", "natcash", "binance", "nowpayments"].includes(provider)) {
      return res.status(400).json({ error: "Passerelle non supportée." });
    }
    const { orderId } = req.body || {};
    if (!orderId || typeof orderId !== "string") {
      return res.status(400).json({ error: "orderId requis." });
    }

    const orderRef = db.collection("orders").doc(orderId);
    const orderSnap = await orderRef.get();
    if (!orderSnap.exists) {
      return res.status(404).json({ error: "Ordre introuvable." });
    }

    const order = orderSnap.data();
    const isAdminUser =
      req.user.email === ADMIN_EMAIL && req.user.email_verified === true;
    if (order.userId !== req.user.uid && !isAdminUser) {
      return res.status(403).json({ error: "Accès refusé à cet ordre." });
    }
    if (order.status !== "PENDING") {
      return res.status(409).json({ error: "L'ordre n'est plus en attente." });
    }
    if (Date.now() > order.expiresAtMillis) {
      await orderRef.update({ status: "EXPIRED", updatedAt: FieldValue.serverTimestamp() });
      return res.status(410).json({ error: "Le verrou de 15 minutes a expiré." });
    }

    let gatewayResult;
    if (provider === "moncash") {
      gatewayResult = await createMonCashPayment(order);
    } else if (provider === "natcash") {
      gatewayResult = await createNatCashPayment(order);
    } else if (provider === "binance") {
      gatewayResult = await createBinancePayOrder(order);
    } else {
      gatewayResult = await createNowPaymentsOrder(order);
    }

    const sessionRef = db.collection("payment_sessions").doc(`${orderId}_${provider}`);
    const batch = db.batch();
    batch.update(orderRef, {
      gatewayMode: "ONLINE",
      gatewayOrderId: gatewayResult.gatewayOrderId,
      checkoutUrl: gatewayResult.checkoutUrl,
      gatewayStatus: "WAITING_WEBHOOK",
      payAddress: gatewayResult.payAddress || order.payAddress || "",
      payCryptoAmount: gatewayResult.payCryptoAmount || order.payCryptoAmount || 0,
      updatedAt: FieldValue.serverTimestamp(),
    });
    batch.set(
      sessionRef,
      {
        userId: order.userId,
        orderId,
        orderCode: order.orderCode,
        provider,
        gatewayMode: gatewayResult.mode,
        gatewayOrderId: gatewayResult.gatewayOrderId,
        checkoutUrl: gatewayResult.checkoutUrl,
        amount: order.sendAmount,
        currency: order.sendCurrency,
        status: "INITIATED",
        webhookVerified: false,
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
    await batch.commit();

    return res.json({
      success: true,
      sessionId: sessionRef.id,
      ...gatewayResult,
    });
  } catch (err) {
    console.error("Create payment error:", err.message);
    return res.status(502).json({ error: err.message || "Erreur passerelle de paiement." });
  }
});

/**
 * GET /api/payments/status/:orderId
 * Allows authenticated owner or admin to query current gateway/order state cleanly.
 */
app.get("/api/payments/status/:orderId", verifyFirebaseToken, async (req, res) => {
  try {
    const orderId = String(req.params.orderId || "");
    const orderSnap = await db.collection("orders").doc(orderId).get();
    if (!orderSnap.exists) {
      return res.status(404).json({ error: "Ordre introuvable." });
    }
    const order = orderSnap.data();
    const isAdminUser =
      req.user.email === ADMIN_EMAIL && req.user.email_verified === true;
    if (order.userId !== req.user.uid && !isAdminUser) {
      return res.status(403).json({ error: "Accès refusé." });
    }

    // Active MonCash server-to-server poll when waiting on MonCash webhook
    if (
      order.status === "PENDING" &&
      order.sendMethod === "moncash" &&
      order.gatewayOrderId &&
      process.env.MONCASH_CLIENT_ID
    ) {
      try {
        const hosts = getGatewayHosts();
        const token = await getMonCashOAuthToken(hosts.moncashApiBase);
        const resp = await fetch(`${hosts.moncashApiBase}/v1/RetrieveOrderPayment`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ orderId: order.orderCode }),
        });
        if (resp.ok) {
          const data = await resp.json();
          const payment = data?.payment;
          if (payment && String(payment.message).toLowerCase() === "successful") {
            await applyVerifiedWebhook({
              provider: "moncash",
              eventId: `mc_${payment.transaction_id}`,
              orderCode: order.orderCode,
              gatewayOrderId: String(payment.transaction_id),
              amountPaid: Number(payment.cost || 0),
              currency: "HTG",
              signatureValid: true,
            });
            const updatedSnap = await db.collection("orders").doc(orderId).get();
            const updated = updatedSnap.data();
            return res.json({
              orderId,
              status: updated.status,
              gatewayStatus: updated.gatewayStatus,
              externalTxRef: updated.externalTxRef,
            });
          }
        }
      } catch (pollErr) {
        console.warn("MonCash poll note:", pollErr.message);
      }
    }

    // Active NOWPayments server-to-server poll when waiting on IPN webhook
    if (
      order.status === "PENDING" &&
      order.sendMethod === "nowpayments" &&
      order.gatewayOrderId &&
      process.env.NOWPAYMENTS_API_KEY
    ) {
      try {
        const hosts = getGatewayHosts();
        const resp = await fetch(
          `${hosts.nowpaymentsApiBase}/payment/${encodeURIComponent(order.gatewayOrderId)}`,
          {
            method: "GET",
            headers: {
              "x-api-key": process.env.NOWPAYMENTS_API_KEY,
              Accept: "application/json",
            },
          }
        );
        if (resp.ok) {
          const data = await resp.json();
          const paymentStatus = String(data?.payment_status || "").toLowerCase();
          if (["finished", "confirmed", "partially_paid"].includes(paymentStatus)) {
            await applyVerifiedWebhook({
              provider: "nowpayments",
              eventId: String(data.payment_id || order.gatewayOrderId),
              orderCode: order.orderCode,
              gatewayOrderId: String(data.payment_id || order.gatewayOrderId),
              amountPaid: Number(data.actually_paid || data.pay_amount || 0),
              currency: String(data.pay_currency || "USD").toUpperCase(),
              signatureValid: true,
            });
            const updatedSnap = await db.collection("orders").doc(orderId).get();
            const updated = updatedSnap.data();
            return res.json({
              orderId,
              status: updated.status,
              gatewayStatus: updated.gatewayStatus,
              externalTxRef: updated.externalTxRef,
            });
          }
        }
      } catch (pollErr) {
        console.warn("NOWPayments poll note:", pollErr.message);
      }
    }

    // Active Binance Pay server-to-server poll when waiting on Binance Pay webhook
    const binanceKey =
      process.env.BINANCE_PAY_CERT_SN ||
      process.env.BINANCE_PAY_API_KEY ||
      process.env.BINANCE_API_KEY;
    const binanceSecret = process.env.BINANCE_PAY_SECRET_KEY || process.env.BINANCE_SECRET_KEY;
    if (
      order.status === "PENDING" &&
      order.sendMethod === "binance" &&
      order.gatewayOrderId &&
      binanceKey &&
      binanceSecret
    ) {
      try {
        const hosts = getGatewayHosts();
        const timestamp = Date.now().toString();
        const nonce = crypto.randomBytes(16).toString("hex");
        const bodyStr = JSON.stringify({ prepayId: order.gatewayOrderId });
        const signature = signBinancePayRequest(timestamp, nonce, bodyStr, binanceSecret);
        const resp = await fetch(`${hosts.binanceApiBase}/binancepay/openapi/v2/order/query`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "BinancePay-Timestamp": timestamp,
            "BinancePay-Nonce": nonce,
            "BinancePay-Certificate-SN": binanceKey,
            "BinancePay-Signature": signature,
          },
          body: bodyStr,
        });
        if (resp.ok) {
          const result = await resp.json();
          if (result?.status === "SUCCESS" && result?.data?.status === "PAID") {
            await applyVerifiedWebhook({
              provider: "binance",
              eventId: String(result.data.transactionId || order.gatewayOrderId),
              orderCode: order.orderCode,
              gatewayOrderId: String(result.data.transactionId || order.gatewayOrderId),
              amountPaid: Number(result.data.orderAmount || order.sendAmount || 0),
              currency: String(result.data.currency || order.sendCurrency || "USD"),
              signatureValid: true,
            });
            const updatedSnap = await db.collection("orders").doc(orderId).get();
            const updated = updatedSnap.data();
            return res.json({
              orderId,
              status: updated.status,
              gatewayStatus: updated.gatewayStatus,
              externalTxRef: updated.externalTxRef,
            });
          }
        }
      } catch (pollErr) {
        console.warn("Binance Pay poll note:", pollErr.message);
      }
    }

    return res.json({
      orderId,
      status: order.status,
      gatewayStatus: order.gatewayStatus || "NONE",
      externalTxRef: order.externalTxRef || "",
    });
  } catch (err) {
    console.error("Status check error:", err.message);
    return res.status(500).json({ error: "Erreur lors de la vérification du statut." });
  }
});

/**
 * Shared atomic webhook processor with idempotency protection.
 */
async function applyVerifiedWebhook({
  provider,
  eventId,
  orderCode,
  gatewayOrderId,
  amountPaid,
  currency,
  signatureValid,
}) {
  const safeEventId = `${provider}_${String(eventId).replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 96)}`;
  const eventRef = db.collection("webhook_events").doc(safeEventId);
  const existingEvent = await eventRef.get();
  if (existingEvent.exists) {
    return { alreadyProcessed: true };
  }

  const ordersQuery = await db
    .collection("orders")
    .where("orderCode", "==", orderCode)
    .limit(1)
    .get();

  if (ordersQuery.empty) {
    // Check Digital Store orders (orderCode e.g. STK-XXXXXX)
    const storeQuery = await db
      .collection("store_orders")
      .where("orderCode", "==", orderCode)
      .limit(1)
      .get();

    if (!storeQuery.empty) {
      const storeDoc = storeQuery.docs[0];
      const storeOrder = storeDoc.data();
      const expectedStoreAmount =
        provider === "moncash" || provider === "natcash"
          ? Number(storeOrder.amountHtg)
          : Number(storeOrder.usdSell);
      const isStoreSufficient = Number(amountPaid) >= expectedStoreAmount * 0.99;

      let finalStatus = isStoreSufficient ? "PAID" : "PENDING";
      let deliverable = storeOrder.deliverable || "";
      let assignedNetflixRef = null;

      if (
        isStoreSufficient &&
        (storeOrder.category === "netflix" || storeOrder.category === "streaming") &&
        storeOrder.deliveryType === "netflix_profile"
      ) {
        const freeSnap = await db
          .collection("store_netflix_inventory")
          .where("status", "==", "AVAILABLE")
          .limit(1)
          .get();
        if (!freeSnap.empty) {
          const nfDoc = freeSnap.docs[0];
          const nf = nfDoc.data();
          assignedNetflixRef = nfDoc.ref;
          deliverable = `Email: ${nf.accountEmail} | Passe: ${nf.accountPassword} | ${nf.profileName} (PIN: ${nf.profilePin})`;
          finalStatus = "DELIVERED";
        }
      }

      const batch = db.batch();
      batch.set(eventRef, {
        provider,
        eventId: safeEventId,
        orderId: storeDoc.id,
        gatewayOrderId: String(gatewayOrderId || ""),
        signatureValid,
        amountPaid: Number(amountPaid || 0),
        currency: String(currency || "USD"),
        outcome: `STORE_${finalStatus}`,
        receivedAt: FieldValue.serverTimestamp(),
      });
      batch.update(storeDoc.ref, {
        status: finalStatus,
        gatewayStatus: isStoreSufficient ? "CONFIRMED" : "FAILED",
        gatewayOrderId: String(gatewayOrderId || storeOrder.gatewayOrderId || ""),
        externalTxRef: storeOrder.externalTxRef || String(gatewayOrderId || safeEventId),
        deliverable,
        updatedAt: FieldValue.serverTimestamp(),
      });
      if (assignedNetflixRef) {
        batch.update(assignedNetflixRef, {
          status: "OCCUPIED",
          assignedOrderId: storeDoc.id,
          assignedUserId: storeOrder.userId,
          updatedAt: FieldValue.serverTimestamp(),
        });
        batch.set(
          db.collection("store_config").doc("default"),
          { availableNetflixCount: FieldValue.increment(-1), updatedAt: FieldValue.serverTimestamp() },
          { merge: true }
        );
      } else if (
        isStoreSufficient &&
        (storeOrder.category === "starlink" || (storeOrder.category === "telecom" && storeOrder.deliveryType === "starlink"))
      ) {
        batch.set(
          db.collection("store_config").doc("default"),
          { pendingStarlinkCount: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp() },
          { merge: true }
        );
      }
      await batch.commit();
      return { ok: true, storeStatus: finalStatus };
    }

    await eventRef.set({
      provider,
      eventId: safeEventId,
      orderId: "UNKNOWN",
      gatewayOrderId: String(gatewayOrderId || ""),
      signatureValid,
      amountPaid: Number(amountPaid || 0),
      currency: String(currency || "USD"),
      outcome: "ORDER_NOT_FOUND",
      receivedAt: FieldValue.serverTimestamp(),
    });
    return { orderNotFound: true };
  }

  const orderDoc = ordersQuery.docs[0];
  const order = orderDoc.data();
  const expectedAmount =
    provider === "nowpayments"
      ? Number(order.payCryptoAmount > 0 ? order.payCryptoAmount : order.amountUsd)
      : Number(order.sendAmount);

  const isSufficient = Number(amountPaid) >= expectedAmount * 0.99;
  const isTopUp = order.orderType === "TOPUP";
  const nextOrderStatus = isSufficient ? (isTopUp ? "COMPLETED" : "PAID_IN") : "UNDERPAID";
  const nextGatewayStatus = isSufficient ? "CONFIRMED" : "FAILED";
  const sessionRef = db.collection("payment_sessions").doc(`${orderDoc.id}_${provider}`);

  const GATEWAY_BALANCE_FIELD = {
    moncash: "balanceMoncashHtg",
    natcash: "balanceNatcashHtg",
    binance: "balanceBinanceUsd",
    meru: "balanceMeruUsd",
    nowpayments: "balanceNowpaymentsUsd",
  };

  const batch = db.batch();
  batch.set(eventRef, {
    provider,
    eventId: safeEventId,
    orderId: orderDoc.id,
    gatewayOrderId: String(gatewayOrderId || ""),
    signatureValid,
    amountPaid: Number(amountPaid || 0),
    currency: String(currency || order.sendCurrency),
    outcome: nextOrderStatus,
    receivedAt: FieldValue.serverTimestamp(),
  });
  batch.update(orderDoc.ref, {
    status: nextOrderStatus,
    gatewayStatus: nextGatewayStatus,
    gatewayOrderId: String(gatewayOrderId || order.gatewayOrderId || ""),
    externalTxRef: order.externalTxRef || String(gatewayOrderId || safeEventId),
    ...(isSufficient && isTopUp ? { completedAt: FieldValue.serverTimestamp() } : {}),
    updatedAt: FieldValue.serverTimestamp(),
  });
  if (isSufficient && isTopUp && order.userId) {
    const fieldName = GATEWAY_BALANCE_FIELD[order.sendMethod];
    if (fieldName) {
      batch.set(
        db.collection("users").doc(order.userId),
        {
          [fieldName]: FieldValue.increment(Number(order.receiveAmount || order.sendAmount || 0)),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      batch.set(db.collection("ledger_entries").doc(), {
        orderId: orderDoc.id,
        orderCode: order.orderCode,
        accountName: `WALLET_${String(order.sendMethod).toUpperCase()}`,
        entryType: "CREDIT",
        currency: order.receiveCurrency || order.sendCurrency,
        amount: Number(order.receiveAmount || order.sendAmount || 0),
        note: `Recharge automatique Webhook (${provider})`,
        createdAt: FieldValue.serverTimestamp(),
      });
    }
  }
  batch.set(
    sessionRef,
    {
      userId: order.userId,
      orderId: orderDoc.id,
      orderCode: order.orderCode,
      provider,
      gatewayMode: getGatewayHosts().modeLabel,
      gatewayOrderId: String(gatewayOrderId || ""),
      checkoutUrl: order.checkoutUrl || "",
      amount: order.sendAmount,
      currency: order.sendCurrency,
      status: isSufficient ? "CONFIRMED" : "FAILED",
      webhookVerified: true,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );
  await batch.commit();

  return { ok: true, status: nextOrderStatus };
}

// --- CRYPTOGRAPHICALLY SECURED WEBHOOK ENDPOINTS ---

app.post("/webhooks/nowpayments", async (req, res) => {
  const sig = req.headers["x-nowpayments-sig"];
  const secret = process.env.NOWPAYMENTS_IPN_SECRET;
  if (!verifyNowPaymentsSignature(req.body, sig, secret)) {
    return res.status(401).json({ error: "Signature IPN NOWPayments invalide." });
  }
  const paymentStatus = String(req.body.payment_status || "").toLowerCase();
  if (!["finished", "confirmed", "partially_paid"].includes(paymentStatus)) {
    return res.status(200).json({ ignored: true, status: paymentStatus });
  }
  await applyVerifiedWebhook({
    provider: "nowpayments",
    eventId: req.body.payment_id || req.body.invoice_id || Date.now().toString(),
    orderCode: req.body.order_id,
    gatewayOrderId: req.body.payment_id,
    amountPaid: req.body.actually_paid || req.body.pay_amount || 0,
    currency: String(req.body.pay_currency || "USD").toUpperCase(),
    signatureValid: true,
  });
  return res.status(200).json({ received: true });
});

app.post("/webhooks/binance", async (req, res) => {
  const isValid = verifyBinancePayWebhookSignature({
    timestamp: req.headers["binancepay-timestamp"],
    nonce: req.headers["binancepay-nonce"],
    rawBody: req.rawBody,
    signatureHeader: req.headers["binancepay-signature"],
    publicKeyPem: process.env.BINANCE_PAY_PUBLIC_KEY_PEM,
    hmacSecretFallback:
      process.env.BINANCE_PAY_SECRET_KEY || process.env.BINANCE_SECRET_KEY,
  });
  if (!isValid) {
    return res.status(401).json({ error: "Signature Binance Pay invalide." });
  }
  if (req.body.bizStatus !== "PAY_SUCCESS") {
    return res.status(200).json({ ignored: true });
  }
  const dataObj =
    typeof req.body.data === "string" ? JSON.parse(req.body.data) : req.body.data || {};
  const orderCodeMatch = String(dataObj.merchantTradeNo || "").match(/^ALV\d{6}/);
  const orderCode = orderCodeMatch ? `ALV-${orderCodeMatch[0].slice(3)}` : dataObj.orderCode;

  await applyVerifiedWebhook({
    provider: "binance",
    eventId: req.body.bizIdStr || dataObj.prepayId || Date.now().toString(),
    orderCode,
    gatewayOrderId: dataObj.transactionId || dataObj.prepayId,
    amountPaid: dataObj.totalFee || dataObj.orderAmount || 0,
    currency: dataObj.currency || "USD",
    signatureValid: true,
  });
  return res.status(200).json({ returnCode: "SUCCESS", returnMessage: null });
});

app.post("/webhooks/moncash", async (req, res) => {
  const sig = req.headers["x-moncash-signature"];
  const secret = process.env.MONCASH_WEBHOOK_SECRET;
  if (secret && !verifyHmacSha256Signature(req.rawBody, sig, secret)) {
    return res.status(401).json({ error: "Signature MonCash invalide." });
  }
  const transactionId = req.body.transactionId || req.query.transactionId;
  const orderCode = req.body.orderId || req.body.reference;
  if (!transactionId || !orderCode) {
    return res.status(400).json({ error: "Payload MonCash incomplet." });
  }
  // Always confirm with MonCash RetrieveTransaction server-to-server when credentials exist
  let verifiedCost = Number(req.body.amount || 0);
  if (process.env.MONCASH_CLIENT_ID && process.env.MONCASH_CLIENT_SECRET) {
    const hosts = getGatewayHosts();
    const token = await getMonCashOAuthToken(hosts.moncashApiBase);
    const verifyResp = await fetch(`${hosts.moncashApiBase}/v1/RetrieveTransactionPayment`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ transactionId }),
    });
    if (!verifyResp.ok) {
      return res.status(401).json({ error: "Transaction MonCash non vérifiée." });
    }
    const verifyData = await verifyResp.json();
    verifiedCost = Number(verifyData?.payment?.cost || verifiedCost);
  }
  await applyVerifiedWebhook({
    provider: "moncash",
    eventId: transactionId,
    orderCode,
    gatewayOrderId: transactionId,
    amountPaid: verifiedCost,
    currency: "HTG",
    signatureValid: true,
  });
  return res.status(200).json({ received: true });
});

app.post("/webhooks/natcash", async (req, res) => {
  const sig = req.headers["x-natcash-signature"] || req.headers["x-signature"];
  const secret = process.env.NATCASH_API_SECRET || process.env.NATCASH_API_KEY;
  if (!verifyHmacSha256Signature(req.rawBody, sig, secret)) {
    return res.status(401).json({ error: "Signature NatCash invalide." });
  }
  if (String(req.body.status || "").toUpperCase() !== "SUCCESS") {
    return res.status(200).json({ ignored: true });
  }
  await applyVerifiedWebhook({
    provider: "natcash",
    eventId: req.body.transactionId || Date.now().toString(),
    orderCode: req.body.orderId,
    gatewayOrderId: req.body.transactionId,
    amountPaid: Number(req.body.amount || 0),
    currency: req.body.currency || "HTG",
    signatureValid: true,
  });
  return res.status(200).json({ received: true });
});

// --- DIGITAL STORE ENDPOINTS ---

app.post("/api/store/orders/:orderId/checkout", verifyFirebaseToken, async (req, res) => {
  try {
    const orderId = String(req.params.orderId || "");
    const storeRef = db.collection("store_orders").doc(orderId);
    const snap = await storeRef.get();
    if (!snap.exists) {
      return res.status(404).json({ error: "Commande Store introuvable." });
    }
    const storeOrder = snap.data();
    const isAdminUser =
      req.user.email === ADMIN_EMAIL && req.user.email_verified === true;
    if (storeOrder.userId !== req.user.uid && !isAdminUser) {
      return res.status(403).json({ error: "Accès refusé." });
    }
    if (storeOrder.status !== "PENDING") {
      return res.status(409).json({ error: "La commande Store n'est plus en attente." });
    }
    const provider = String(storeOrder.gateway || "").toLowerCase();
    const syntheticOrder = {
      orderCode: storeOrder.orderCode,
      sendAmount: provider === "moncash" || provider === "natcash" ? storeOrder.amountHtg : storeOrder.usdSell,
      sendCurrency: provider === "moncash" || provider === "natcash" ? "HTG" : "USD",
      amountUsd: storeOrder.usdSell,
      cryptoCoin: "usdttrc20",
      payCryptoAmount: storeOrder.usdSell,
    };
    let gatewayResult;
    if (provider === "moncash") {
      gatewayResult = await createMonCashPayment(syntheticOrder);
    } else if (provider === "natcash") {
      gatewayResult = await createNatCashPayment(syntheticOrder);
    } else if (provider === "binance") {
      gatewayResult = await createBinancePayOrder(syntheticOrder);
    } else if (provider === "nowpayments") {
      gatewayResult = await createNowPaymentsOrder(syntheticOrder);
    } else {
      return res.status(400).json({ error: "Passerelle Store non supportée." });
    }

    await storeRef.update({
      gatewayOrderId: gatewayResult.gatewayOrderId,
      checkoutUrl: gatewayResult.checkoutUrl,
      gatewayStatus: "WAITING_WEBHOOK",
      updatedAt: FieldValue.serverTimestamp(),
    });

    return res.json({
      success: true,
      sessionId: `${orderId}_${provider}`,
      ...gatewayResult,
    });
  } catch (err) {
    console.error("Store checkout error:", err.message);
    return res.status(502).json({ error: err.message || "Erreur passerelle Store." });
  }
});

app.get("/api/store/orders/:orderId/status", verifyFirebaseToken, async (req, res) => {
  try {
    const orderId = String(req.params.orderId || "");
    const snap = await db.collection("store_orders").doc(orderId).get();
    if (!snap.exists) {
      return res.status(404).json({ error: "Commande Store introuvable." });
    }
    const order = snap.data();
    const isAdminUser =
      req.user.email === ADMIN_EMAIL && req.user.email_verified === true;
    if (order.userId !== req.user.uid && !isAdminUser) {
      return res.status(403).json({ error: "Accès refusé." });
    }
    return res.json({
      orderId,
      status: order.status,
      gatewayStatus: order.gatewayStatus || "NONE",
      externalTxRef: order.externalTxRef || "",
    });
  } catch (err) {
    return res.status(500).json({ error: "Erreur vérification statut Store." });
  }
});

app.get("/api/store/providers/health", verifyFirebaseToken, async (req, res) => {
  const isAdminUser =
    req.user.email === ADMIN_EMAIL && req.user.email_verified === true;
  if (!isAdminUser) {
    return res.status(403).json({ error: "Réservé à l'administrateur." });
  }
  const measure = async (fn) => {
    const start = Date.now();
    try {
      await fn();
      return { ok: true, latencyMs: Math.max(1, Date.now() - start) };
    } catch (e) {
      return { ok: false, latencyMs: Math.max(1, Date.now() - start), note: e.message };
    }
  };
  const hosts = getGatewayHosts();
  const reloadlyCheck = process.env.RELOADLY_CLIENT_ID
    ? await measure(() =>
        fetch("https://auth.reloadly.com/oauth/token", { method: "HEAD" })
      )
    : { ok: false, latencyMs: 0, note: "Clé Reloadly non définie" };
  const moncashCheck = process.env.MONCASH_CLIENT_ID
    ? await measure(() => getMonCashOAuthToken(hosts.moncashApiBase))
    : { ok: false, latencyMs: 0, note: "Clé MonCash non définie" };
  const nowpaymentsCheck = process.env.NOWPAYMENTS_API_KEY
    ? await measure(async () => {
        const resp = await fetch(`${hosts.nowpaymentsApiBase}/status`, {
          headers: { "x-api-key": process.env.NOWPAYMENTS_API_KEY },
        });
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      })
    : { ok: false, latencyMs: 0, note: "Clé NOWPayments non définie" };

  const binanceConfigured = Boolean(
    (process.env.BINANCE_PAY_CERT_SN ||
      process.env.BINANCE_PAY_API_KEY ||
      process.env.BINANCE_API_KEY) &&
      (process.env.BINANCE_PAY_SECRET_KEY || process.env.BINANCE_SECRET_KEY)
  );
  const natcashConfigured = Boolean(process.env.NATCASH_API_KEY);

  return res.json({
    reloadlyLatencyMs: reloadlyCheck.latencyMs,
    moncashOk: moncashCheck.ok,
    reloadlyOk: reloadlyCheck.ok,
    nowpaymentsOk: nowpaymentsCheck.ok,
    binanceConfigured,
    providers: [
      {
        provider: "MonCash",
        mode: hosts.moncashModeLabel,
        configured: Boolean(process.env.MONCASH_CLIENT_ID && process.env.MONCASH_CLIENT_SECRET),
        latencyMs: moncashCheck.latencyMs,
        status: moncashCheck.ok ? "UP" : "CONFIG_REQUIRED",
      },
      {
        provider: "NatCash",
        mode: hosts.modeLabel,
        configured: natcashConfigured,
        latencyMs: 0,
        status: natcashConfigured ? "UP" : "CONFIG_REQUIRED",
      },
      {
        provider: "NOWPayments",
        mode: hosts.modeLabel,
        configured: Boolean(process.env.NOWPAYMENTS_API_KEY && process.env.NOWPAYMENTS_IPN_SECRET),
        latencyMs: nowpaymentsCheck.latencyMs,
        status: nowpaymentsCheck.ok ? "UP" : "CONFIG_REQUIRED",
      },
      {
        provider: "Binance Pay",
        mode: hosts.modeLabel,
        configured: binanceConfigured,
        latencyMs: 0,
        status: binanceConfigured ? "UP" : "CONFIG_REQUIRED",
      },
      {
        provider: "Reloadly",
        mode: process.env.RELOADLY_SANDBOX === "false" ? "LIVE" : "SANDBOX",
        configured: Boolean(process.env.RELOADLY_CLIENT_ID && process.env.RELOADLY_CLIENT_SECRET),
        latencyMs: reloadlyCheck.latencyMs,
        status: reloadlyCheck.ok ? "UP" : "CONFIG_REQUIRED",
      },
    ],
  });
});

const pinRecoveryLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 15,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Trop de demandes de récupération. Réessayez plus tard." },
});

async function sendRecoveryEmailViaProvider(toEmail, code) {
  const fromAddress = process.env.EMAIL_FROM || "Alevini Sécurité <security@alevini.ht>";
  const subject = "ALEVINI — Code de récupération PIN (6 chiffres)";
  const textBody = `Votre code de validation ALEVINI à 6 chiffres est : ${code}. Il expire dans 10 minutes.`;

  if (process.env.RESEND_API_KEY) {
    const resp = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${process.env.RESEND_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: fromAddress,
        to: [toEmail],
        subject,
        text: textBody,
      }),
    });
    if (!resp.ok) {
      throw new Error(`Échec d'envoi e-mail (${resp.status}).`);
    }
    return;
  }

  if (process.env.PIN_EMAIL_WEBHOOK_URL) {
    const resp = await fetch(process.env.PIN_EMAIL_WEBHOOK_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ to: toEmail, from: fromAddress, subject, text: textBody, code }),
    });
    if (!resp.ok) {
      throw new Error(`Échec webhook e-mail (${resp.status}).`);
    }
    return;
  }

  const err = new Error("Service d'envoi e-mail non configuré sur le serveur.");
  err.statusCode = 503;
  throw err;
}

app.post("/api/security/pin-recovery/request", pinRecoveryLimiter, async (req, res) => {
  try {
    const rawEmail = String(req.body?.email || "").trim().toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(rawEmail)) {
      return res.status(400).json({ error: "Adresse e-mail enregistrée invalide ou non renseignée." });
    }

    const emailKey = crypto.createHash("sha256").update(rawEmail).digest("hex");
    const challengeRef = db.collection("pin_recovery_challenges").doc(emailKey);
    const existing = await challengeRef.get();
    const now = Date.now();
    if (existing.exists && now - Number(existing.data()?.lastSentAtMillis || 0) < 60_000) {
      return res.status(429).json({ error: "Veuillez patienter 60 secondes avant de renvoyer un code." });
    }

    const code = crypto.randomInt(0, 1_000_000).toString().padStart(6, "0");
    const salt = crypto.randomBytes(16).toString("hex");
    const codeHash = crypto.createHash("sha256").update(`${salt}:${code}`).digest("hex");

    await challengeRef.set({
      emailKey,
      salt,
      codeHash,
      expiresAtMillis: now + 10 * 60 * 1000,
      attemptsRemaining: 5,
      lastSentAtMillis: now,
    });

    await sendRecoveryEmailViaProvider(rawEmail, code);
    return res.json({ sent: true });
  } catch (err) {
    console.error("PIN recovery request error:", err.message);
    return res.status(err.statusCode || 500).json({
      error: err.message || "Impossible d'envoyer le code de validation.",
    });
  }
});

app.post("/api/security/pin-recovery/verify", pinRecoveryLimiter, async (req, res) => {
  try {
    const rawEmail = String(req.body?.email || "").trim().toLowerCase();
    const code = String(req.body?.code || "").trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(rawEmail) || !/^\d{6}$/.test(code)) {
      return res.status(400).json({ error: "E-mail ou code à 6 chiffres invalide." });
    }

    const emailKey = crypto.createHash("sha256").update(rawEmail).digest("hex");
    const challengeRef = db.collection("pin_recovery_challenges").doc(emailKey);
    const snap = await challengeRef.get();
    if (!snap.exists) {
      return res.status(404).json({ error: "Aucun code actif trouvé. Demandez un nouveau code." });
    }

    const data = snap.data();
    const now = Date.now();
    if (now > Number(data.expiresAtMillis || 0)) {
      await challengeRef.delete();
      return res.status(410).json({ error: "Le code à 6 chiffres a expiré. Demandez un nouveau code." });
    }

    const attemptsRemaining = Number(data.attemptsRemaining || 0);
    if (attemptsRemaining <= 0) {
      await challengeRef.delete();
      return res.status(429).json({ error: "Trop de tentatives erronées. Demandez un nouveau code." });
    }

    const candidateHash = crypto.createHash("sha256").update(`${data.salt}:${code}`).digest("hex");
    const expectedHash = String(data.codeHash || "");
    const match =
      candidateHash.length === expectedHash.length &&
      crypto.timingSafeEqual(Buffer.from(candidateHash, "utf8"), Buffer.from(expectedHash, "utf8"));

    if (!match) {
      await challengeRef.update({ attemptsRemaining: attemptsRemaining - 1 });
      return res.status(400).json({ error: `Code à 6 chiffres incorrect (${attemptsRemaining - 1} essai(s) restant(s)).` });
    }

    await challengeRef.delete();
    return res.json({ verified: true });
  } catch (err) {
    console.error("PIN recovery verify error:", err.message);
    return res.status(500).json({ error: "Erreur lors de la vérification du code." });
  }
});

app.get("/health", (_req, res) => {
  res.json({
    ok: true,
    service: "alevini-payment-backend",
    databaseId: FIRESTORE_DATABASE_ID,
    gatewayMode: getGatewayHosts().modeLabel,
  });
});

exports.api = onRequest({ region: "us-central1" }, app);

if (require.main === module) {
  const port = process.env.PORT || 8080;
  app.listen(port, () => {
    console.log(`Alevini payment backend listening on port ${port}`);
  });
}
