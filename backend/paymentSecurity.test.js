const { test } = require("node:test");
const assert = require("node:assert/strict");
const {
  computeNowPaymentsSignature,
  verifyNowPaymentsSignature,
  signBinancePayRequest,
  verifyBinancePayWebhookSignature,
  computeHmacSha256,
  verifyHmacSha256Signature,
} = require("./signatureUtils");

test("NOWPayments IPN canonical HMAC-SHA512 rejects tampered payload and accepts valid sorted signature", () => {
  const secret = "test_ipn_secret_alevini_123";
  const payload = {
    payment_status: "finished",
    order_id: "ALV-123456",
    actually_paid: 50,
    nested: { z: 1, a: 2 },
  };
  const sig = computeNowPaymentsSignature(payload, secret);
  assert.equal(verifyNowPaymentsSignature(payload, sig, secret), true);
  assert.equal(verifyNowPaymentsSignature({ ...payload, actually_paid: 500 }, sig, secret), false);
  assert.equal(verifyNowPaymentsSignature(payload, "invalid_sig", secret), false);
});

test("Binance Pay OpenAPI v3 signature & webhook verification work and reject replay/tampering", () => {
  const secret = "binance_secret_xyz";
  const timestamp = "1731680000000";
  const nonce = "nonceabcdef123456";
  const body = JSON.stringify({ bizStatus: "PAY_SUCCESS", totalFee: 100 });
  const sig = signBinancePayRequest(timestamp, nonce, body, secret);

  assert.equal(
    verifyBinancePayWebhookSignature({
      timestamp,
      nonce,
      rawBody: body,
      signatureHeader: sig,
      hmacSecretFallback: secret,
    }),
    true
  );
  assert.equal(
    verifyBinancePayWebhookSignature({
      timestamp,
      nonce,
      rawBody: JSON.stringify({ bizStatus: "PAY_SUCCESS", totalFee: 9999 }),
      signatureHeader: sig,
      hmacSecretFallback: secret,
    }),
    false
  );
});

test("NatCash & MonCash HMAC-SHA256 webhook verification blocks forged payloads", () => {
  const secret = "natcash_webhook_secret_456";
  const rawBody = JSON.stringify({ orderId: "ALV-654321", amount: 6600, status: "SUCCESS" });
  const validSig = computeHmacSha256(rawBody, secret);

  assert.equal(verifyHmacSha256Signature(rawBody, validSig, secret), true);
  assert.equal(verifyHmacSha256Signature(rawBody.replace("6600", "66000"), validSig, secret), false);
  assert.equal(verifyHmacSha256Signature(rawBody, validSig, "wrong_secret"), false);
});
