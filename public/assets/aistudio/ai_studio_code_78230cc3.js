require("dotenv").config();
const express = require("express");
const crypto = require("crypto");
const cors = require("cors");
const helmet = require("helmet");
const { Pool } = require("pg");

const app = express();
app.use(helmet());
app.use(cors());
app.use(express.json());
app.use(express.static("public")); // Sert votre fichier public/index.html

const pool = new Pool({ connectionString: process.env.DATABASE_URL });

// --- UTILITAIRES ---

// Tri récursif des clés exigé par NOWPayments pour vérifier x-nowpayments-sig
function sortKeysRecursively(obj) {
  if (Array.isArray(obj)) return obj.map(sortKeysRecursively);
  if (obj !== null && typeof obj === "object") {
    return Object.keys(obj)
      .sort()
      .reduce((acc, key) => {
        acc[key] = sortKeysRecursively(obj[key]);
        return acc;
      }, {});
  }
  return obj;
}

function verifyNowPaymentsSignature(payload, receivedSig) {
  if (!receivedSig || !process.env.NOWPAYMENTS_IPN_SECRET) return false;
  const sortedJson = JSON.stringify(sortKeysRecursively(payload));
  const expectedSig = crypto
    .createHmac("sha512", process.env.NOWPAYMENTS_IPN_SECRET)
    .update(sortedJson)
    .digest("hex");

  const bufA = Buffer.from(expectedSig, "utf8");
  const bufB = Buffer.from(receivedSig, "utf8");
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

function requireAdmin(req, res, next) {
  const key = req.headers["x-admin-key"];
  if (!key || key !== process.env.ADMIN_API_KEY) {
    return res.status(401).json({ error: "Accès Admin non autorisé" });
  }
  next();
}

// Moteur de calcul central (USD / HTG / EUR + Pourcentage)
function computeExchange(config, { sendAmount, sendCurrency, receiveCurrency, sendMethod, receiveMethod }) {
  const feesMap = {
    nowpayments: Number(config.fee_nowpayments),
    binance: Number(config.fee_binance),
    meru: Number(config.fee_meru),
    moncash: Number(config.fee_moncash),
    natcash: Number(config.fee_natcash),
  };

  if (!(sendMethod in feesMap) || !(receiveMethod in feesMap) || sendMethod === receiveMethod) {
    throw new Error("Méthodes d'échange invalides");
  }

  const eurToUsd = Number(config.eur_to_usd);
  // Choisir le taux HTG selon que le client achète ou vend du HTG
  const usdToHtg = receiveCurrency === "HTG"
    ? Number(config.usd_to_htg_buy)
    : Number(config.usd_to_htg_sell);

  const toUSD = (amt, curr) => {
    if (curr === "USD") return amt;
    if (curr === "HTG") return amt / usdToHtg;
    if (curr === "EUR") return amt * eurToUsd;
    throw new Error("Devise non supportée");
  };

  const fromUSD = (usdAmt, curr) => {
    if (curr === "USD") return usdAmt;
    if (curr === "HTG") return usdAmt * usdToHtg;
    if (curr === "EUR") return usdAmt / eurToUsd;
    throw new Error("Devise non supportée");
  };

  const grossUSD = toUSD(sendAmount, sendCurrency);
  if (grossUSD < Number(config.min_order_usd) || grossUSD > Number(config.max_order_usd)) {
    throw new Error(`Le montant doit être entre ${config.min_order_usd} USD et ${config.max_order_usd} USD`);
  }

  const feePercent = feesMap[sendMethod] + feesMap[receiveMethod];
  const netUSD = grossUSD * (1 - feePercent / 100);
  const receiveAmount = fromUSD(netUSD, receiveCurrency);

  const baseRate = fromUSD(toUSD(1, sendCurrency), receiveCurrency);
  const effectiveRate = baseRate * (1 - feePercent / 100);

  return {
    grossUSD: Number(grossUSD.toFixed(4)),
    netUSD: Number(netUSD.toFixed(4)),
    amountHTG: Number((netUSD * usdToHtg).toFixed(2)),
    amountEUR: Number((netUSD / eurToUsd).toFixed(4)),
    receiveAmount: Number(receiveAmount.toFixed(2)),
    baseRate: Number(baseRate.toFixed(6)),
    feePercent: Number(feePercent.toFixed(2)),
    effectiveRate: Number(effectiveRate.toFixed(6)),
  };
}

// --- ROUTES PUBLIQUES / CLIENT ---

// 1. Lire les taux et pourcentages en direct
app.get("/api/rates", async (req, res) => {
  try {
    const { rows } = await pool.query("SELECT * FROM exchange_config WHERE id = 1");
    res.json({ config: rows[0] });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. Créer une commande d'échange
app.post("/api/orders", async (req, res) => {
  const client = await pool.connect();
  try {
    const {
      sendAmount,
      sendCurrency,
      receiveCurrency,
      sendMethod,
      receiveMethod,
      cryptoCoin, // ex: 'usdttrc20', 'btc', 'trx'
      destAccount,
    } = req.body;

    if (!destAccount || Number(sendAmount) <= 0) {
      return res.status(400).json({ error: "Montant et compte de réception requis" });
    }

    const { rows: cfgRows } = await client.query("SELECT * FROM exchange_config WHERE id = 1");
    const calc = computeExchange(cfgRows[0], {
      sendAmount: Number(sendAmount),
      sendCurrency,
      receiveCurrency,
      sendMethod,
      receiveMethod,
    });

    const orderCode = "ALV-" + crypto.randomInt(100000, 999999);
    const expiresAt = new Date(Date.now() + 15 * 60 * 1000); // Verrou 15 minutes

    let externalPaymentId = null;
    let payAddress = null;
    let payCryptoAmount = null;
    let paymentInstruction = "";

    // Si paiement via NOWPayments : appel API réel pour générer l'adresse crypto
    if (sendMethod === "nowpayments") {
      const npRes = await fetch(`${process.env.NOWPAYMENTS_BASE_URL}/payment`, {
        method: "POST",
        headers: {
          "x-api-key": process.env.NOWPAYMENTS_API_KEY,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          price_amount: calc.grossUSD,
          price_currency: "usd",
          pay_currency: (cryptoCoin || "usdttrc20").toLowerCase(),
          order_id: orderCode,
          order_description: `Alevini ${orderCode} vers ${receiveMethod}`,
          ipn_callback_url: `${process.env.BASE_URL}/api/webhooks/nowpayments`,
        }),
      });

      const npData = await npRes.json();
      if (!npRes.ok || !npData.pay_address) {
        throw new Error(npData.message || "Erreur passerelle NOWPayments");
      }

      externalPaymentId = String(npData.payment_id);
      payAddress = npData.pay_address;
      payCryptoAmount = npData.pay_amount;
      paymentInstruction = `Envoyez exactement ${payCryptoAmount} ${npData.pay_currency.toUpperCase()} à cette adresse.`;
    } else {
      const targets = {
        binance: `Binance Pay ID / UID : ${process.env.ALEVINI_BINANCE_PAY_ID}`,
        meru: `Tag Meru : ${process.env.ALEVINI_MERU_TAG}`,
        moncash: `Numéro Marchand MonCash : ${process.env.ALEVINI_MONCASH_NUMBER}`,
        natcash: `Numéro Marchand NatCash : ${process.env.ALEVINI_NATCASH_NUMBER}`,
      };
      paymentInstruction = targets[sendMethod];
    }

    const { rows: inserted } = await client.query(
      `INSERT INTO orders (
        order_code, send_method, receive_method, send_currency, receive_currency, crypto_coin,
        send_amount, receive_amount, amount_usd, amount_htg, amount_eur,
        base_rate, fee_percent, effective_rate, dest_account,
        external_payment_id, pay_address, pay_crypto_amount, expires_at
      ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19)
      RETURNING *`,
      [
        orderCode, sendMethod, receiveMethod, sendCurrency, receiveCurrency, cryptoCoin || null,
        sendAmount, calc.receiveAmount, calc.netUSD, calc.amountHTG, calc.amountEUR,
        calc.baseRate, calc.feePercent, calc.effectiveRate, destAccount,
        externalPaymentId, payAddress, payCryptoAmount, expiresAt,
      ]
    );

    res.status(201).json({
      order: inserted[0],
      instruction: paymentInstruction,
    });
  } catch (err) {
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

// 3. Client soumet sa référence de paiement (MonCash / NatCash / Meru / Binance)
app.post("/api/orders/:orderCode/submit-ref", async (req, res) => {
  try {
    const { externalTxRef } = req.body;
    if (!externalTxRef || externalTxRef.trim().length < 4) {
      return res.status(400).json({ error: "Référence de transaction invalide" });
    }

    const { rows } = await pool.query(
      `UPDATE orders
       SET external_tx_ref = $1, status = 'SUBMITTED'
       WHERE order_code = $2 AND status = 'PENDING' AND expires_at > NOW()
       RETURNING *`,
      [externalTxRef.trim(), req.params.orderCode]
    );

    if (rows.length === 0) {
      return res.status(400).json({ error: "Commande expirée, introuvable ou déjà soumise" });
    }
    res.json({ order: rows[0] });
  } catch (err) {
    // Code 23505 = référence déjà utilisée (anti-fraude)
    if (err.code === "23505") {
      return res.status(409).json({ error: "Cette référence a déjà été utilisée !" });
    }
    res.status(500).json({ error: err.message });
  }
});

// --- WEBHOOK AUTOMATIQUE NOWPAYMENTS ---

app.post("/api/webhooks/nowpayments", async (req, res) => {
  const sig = req.headers["x-nowpayments-sig"];
  if (!verifyNowPaymentsSignature(req.body, sig)) {
    return res.status(403).json({ error: "Signature IPN invalide" });
  }

  const { order_id, payment_status, actually_paid, pay_amount } = req.body;
  const client = await pool.connect();

  try {
    await client.query("BEGIN");
    const { rows } = await client.query(
      "SELECT * FROM orders WHERE order_code = $1 FOR UPDATE",
      [order_id]
    );

    if (rows.length === 0) {
      await client.query("COMMIT");
      return res.status(200).send("OK");
    }

    const order = rows[0];
    // Idempotence : ne rien refaire si déjà payé ou terminé
    if (["PAID_IN", "COMPLETED"].includes(order.status)) {
      await client.query("COMMIT");
      return res.status(200).send("ALREADY_PROCESSED");
    }

    if (payment_status === "finished" || payment_status === "confirmed") {
      const paidRatio = Number(actually_paid) / Number(pay_amount);
      const newStatus = paidRatio >= 0.995 ? "PAID_IN" : "UNDERPAID";

      await client.query(
        `UPDATE orders SET status = $1, actually_paid_crypto = $2, paid_in_at = NOW() WHERE id = $3`,
        [newStatus, actually_paid, order.id]
      );

      if (newStatus === "PAID_IN") {
        await client.query(
          `INSERT INTO ledger_entries (order_id, account_name, entry_type, currency, amount, note)
           VALUES ($1, 'CAISSE_NOWPAYMENTS', 'CREDIT', $2, $3, 'Dépôt crypto IPN confirmé')`,
          [order.id, order.send_currency, order.send_amount]
        );
      }
    }

    await client.query("COMMIT");
    res.status(200).send("OK");
  } catch (err) {
    await client.query("ROLLBACK");
    res.status(500).json({ error: err.message });
  } finally {
    client.release();
  }
});

// --- ROUTES ADMIN (PROTÉGÉES PAR x-admin-key) ---

app.put("/api/admin/rates", requireAdmin, async (req, res) => {
  try {
    const {
      usd_to_htg_buy, usd_to_htg_sell, eur_to_usd,
      fee_nowpayments, fee_binance, fee_meru, fee_moncash, fee_natcash,
    } = req.body;

    const { rows } = await pool.query(
      `UPDATE exchange_config SET
        usd_to_htg_buy = COALESCE($1, usd_to_htg_buy),
        usd_to_htg_sell = COALESCE($2, usd_to_htg_sell),
        eur_to_usd = COALESCE($3, eur_to_usd),
        fee_nowpayments = COALESCE($4, fee_nowpayments),
        fee_binance = COALESCE($5, fee_binance),
        fee_meru = COALESCE($6, fee_meru),
        fee_moncash = COALESCE($7, fee_moncash),
        fee_natcash = COALESCE($8, fee_natcash),
        updated_at = NOW()
       WHERE id = 1 RETURNING *`,
      [usd_to_htg_buy, usd_to_htg_sell, eur_to_usd, fee_nowpayments, fee_binance, fee_meru, fee_moncash, fee_natcash]
    );
    res.json({ config: rows[0] });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get("/api/admin/orders", requireAdmin, async (req, res) => {
  const { rows } = await pool.query("SELECT * FROM orders ORDER BY created_at DESC LIMIT 100");
  res.json({ orders: rows });
});

app.post("/api/admin/orders/:orderCode/complete", requireAdmin, async (req, res) => {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const { rows } = await client.query(
      "SELECT * FROM orders WHERE order_code = $1 FOR UPDATE",
      [req.params.orderCode]
    );
    if (rows.length === 0) throw new Error("Commande introuvable");

    const order = rows[0];
    if (order.status === "COMPLETED") throw new Error("Déjà payée");

    await client.query(
      "UPDATE orders SET status = 'COMPLETED', completed_at = NOW() WHERE id = $1",
      [order.id]
    );

    // Enregistrement comptable du décaissement
    await client.query(
      `INSERT INTO ledger_entries (order_id, account_name, entry_type, currency, amount, note)
       VALUES ($1, $2, 'DEBIT', $3, $4, 'Paiement envoyé au client')`,
      [order.id, `CAISSE_${order.receive_method.toUpperCase()}`, order.receive_currency, order.receive_amount]
    );

    await client.query("COMMIT");
    res.json({ success: true, order_code: order.order_code });
  } catch (err) {
    await client.query("ROLLBACK");
    res.status(400).json({ error: err.message });
  } finally {
    client.release();
  }
});

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`Alevini API démarrée sur le port ${port}`));