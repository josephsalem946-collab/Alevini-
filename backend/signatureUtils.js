const crypto = require("node:crypto");

function sortObjectKeysRecursively(value) {
  if (Array.isArray(value)) {
    return value.map(sortObjectKeysRecursively);
  }
  if (value !== null && typeof value === "object") {
    return Object.keys(value)
      .sort()
      .reduce((acc, key) => {
        acc[key] = sortObjectKeysRecursively(value[key]);
        return acc;
      }, {});
  }
  return value;
}

function timingSafeEqualString(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  const bufA = Buffer.from(a, "utf8");
  const bufB = Buffer.from(b, "utf8");
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

/**
 * NOWPayments IPN verification:
 * Sort payload keys alphabetically, serialize JSON, compute HMAC-SHA512 with IPN secret.
 */
function computeNowPaymentsSignature(payloadObj, ipnSecret) {
  const sorted = sortObjectKeysRecursively(payloadObj);
  const hmac = crypto.createHmac("sha512", ipnSecret);
  hmac.update(JSON.stringify(sorted));
  return hmac.digest("hex");
}

function verifyNowPaymentsSignature(payloadObj, signatureHeader, ipnSecret) {
  if (!payloadObj || !signatureHeader || !ipnSecret) return false;
  const expected = computeNowPaymentsSignature(payloadObj, ipnSecret);
  return timingSafeEqualString(expected.toLowerCase(), String(signatureHeader).trim().toLowerCase());
}

/**
 * Binance Pay OpenAPI v3 request signing:
 * payloadToSign = `${timestamp}\n${nonce}\n${bodyString}\n` -> uppercase HMAC-SHA512
 */
function signBinancePayRequest(timestamp, nonce, bodyString, secretKey) {
  const payload = `${timestamp}\n${nonce}\n${bodyString}\n`;
  return crypto.createHmac("sha512", secretKey).update(payload).digest("hex").toUpperCase();
}

/**
 * Binance Pay webhook signature verification:
 * Uses RSA SHA256 against BinancePay-Certificate public key when configured,
 * or HMAC-SHA512 fallback if verified via internal signed gateway proxy.
 */
function verifyBinancePayWebhookSignature({
  timestamp,
  nonce,
  rawBody,
  signatureHeader,
  publicKeyPem,
  hmacSecretFallback,
}) {
  if (!timestamp || !nonce || !rawBody || !signatureHeader) return false;
  const signedPayload = `${timestamp}\n${nonce}\n${rawBody}\n`;

  if (publicKeyPem && publicKeyPem.includes("BEGIN PUBLIC KEY")) {
    try {
      const verifier = crypto.createVerify("RSA-SHA256");
      verifier.update(signedPayload);
      verifier.end();
      return verifier.verify(publicKeyPem, signatureHeader, "base64");
    } catch {
      return false;
    }
  }

  if (hmacSecretFallback) {
    const expectedHex = crypto
      .createHmac("sha512", hmacSecretFallback)
      .update(signedPayload)
      .digest("hex")
      .toUpperCase();
    return timingSafeEqualString(expectedHex, String(signatureHeader).trim().toUpperCase());
  }

  return false;
}

/**
 * Generic HMAC-SHA256 signing & verification for NatCash / MonCash webhooks.
 */
function computeHmacSha256(rawPayload, secret) {
  return crypto.createHmac("sha256", secret).update(rawPayload).digest("hex");
}

function verifyHmacSha256Signature(rawPayload, signatureHeader, secret) {
  if (!rawPayload || !signatureHeader || !secret) return false;
  const expected = computeHmacSha256(rawPayload, secret);
  return timingSafeEqualString(expected.toLowerCase(), String(signatureHeader).trim().toLowerCase());
}

module.exports = {
  sortObjectKeysRecursively,
  timingSafeEqualString,
  computeNowPaymentsSignature,
  verifyNowPaymentsSignature,
  signBinancePayRequest,
  verifyBinancePayWebhookSignature,
  computeHmacSha256,
  verifyHmacSha256Signature,
};
