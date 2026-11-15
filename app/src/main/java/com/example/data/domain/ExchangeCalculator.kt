package com.example.data.domain

import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeCalculation
import com.example.data.model.ExchangeConfig
import com.example.data.model.PaymentMethod
import kotlin.math.round

object ExchangeCalculator {

    sealed class ValidationResult {
        data class Valid(val calculation: ExchangeCalculation) : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    fun compute(
        config: ExchangeConfig,
        sendAmount: Double,
        sendCurrency: CurrencyCode,
        receiveCurrency: CurrencyCode,
        sendMethod: PaymentMethod,
        receiveMethod: PaymentMethod,
    ): ValidationResult {
        if (sendMethod == receiveMethod) {
            return ValidationResult.Invalid("Les méthodes d'envoi et de réception doivent être différentes.")
        }
        if (sendAmount <= 0.0 || sendAmount.isNaN() || sendAmount.isInfinite()) {
            return ValidationResult.Invalid("Entrez un montant valide supérieur à 0.")
        }

        val eurToUsd = config.eurToUsd
        val usdToHtg = if (receiveCurrency == CurrencyCode.HTG) {
            config.usdToHtgBuy
        } else {
            config.usdToHtgSell
        }

        fun toUsd(amt: Double, curr: CurrencyCode): Double = when (curr) {
            CurrencyCode.USD -> amt
            CurrencyCode.HTG -> amt / usdToHtg
            CurrencyCode.EUR -> amt * eurToUsd
        }

        fun fromUsd(usdAmt: Double, curr: CurrencyCode): Double = when (curr) {
            CurrencyCode.USD -> usdAmt
            CurrencyCode.HTG -> usdAmt * usdToHtg
            CurrencyCode.EUR -> usdAmt / eurToUsd
        }

        val grossUsd = toUsd(sendAmount, sendCurrency)
        if (grossUsd < config.minOrderUsd || grossUsd > config.maxOrderUsd) {
            return ValidationResult.Invalid(
                "Le montant doit être entre ${format2(config.minOrderUsd)} USD et ${format2(config.maxOrderUsd)} USD."
            )
        }

        val feePercent = config.feeFor(sendMethod) + config.feeFor(receiveMethod)
        val netUsd = grossUsd * (1.0 - feePercent / 100.0)
        val receiveAmount = fromUsd(netUsd, receiveCurrency)

        val baseRate = fromUsd(toUsd(1.0, sendCurrency), receiveCurrency)
        val effectiveRate = baseRate * (1.0 - feePercent / 100.0)

        return ValidationResult.Valid(
            ExchangeCalculation(
                grossUsd = roundN(grossUsd, 4),
                netUsd = roundN(netUsd, 4),
                amountHtg = roundN(netUsd * usdToHtg, 2),
                amountEur = roundN(netUsd / eurToUsd, 4),
                receiveAmount = roundN(receiveAmount, 2),
                baseRate = roundN(baseRate, 6),
                feePercent = roundN(feePercent, 2),
                effectiveRate = roundN(effectiveRate, 6),
            )
        )
    }

    fun buildPaymentInstruction(
        config: ExchangeConfig,
        sendMethod: PaymentMethod,
        cryptoCoinId: String,
        payCryptoAmount: Double,
    ): String = when (sendMethod) {
        PaymentMethod.NOWPAYMENTS ->
            "Envoyez exactement ${formatCrypto(payCryptoAmount)} ${cryptoCoinId.uppercase()} à l'adresse générée."
        PaymentMethod.BINANCE ->
            "Binance Pay ID / UID : ${config.binancePayId}"
        PaymentMethod.MERU ->
            "Tag Meru : ${config.meruTag}"
        PaymentMethod.MONCASH ->
            "Numéro Marchand MonCash : ${config.moncashMerchant}"
        PaymentMethod.NATCASH ->
            "Numéro Marchand NatCash : ${config.natcashMerchant}"
    }

    private fun roundN(value: Double, decimals: Int): Double {
        var factor = 1.0
        repeat(decimals) { factor *= 10.0 }
        return round(value * factor) / factor
    }

    private fun format2(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
    private fun formatCrypto(value: Double): String = String.format(java.util.Locale.US, "%.6f", value)
}
