package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PaymentMethod(
    val id: String,
    val label: String,
    val defaultCurrency: CurrencyCode,
    val supportsHtg: Boolean,
    val accountHint: String,
) {
    MONCASH(
        id = "moncash",
        label = "MonCash",
        defaultCurrency = CurrencyCode.HTG,
        supportsHtg = true,
        accountHint = "+509 XXXX XXXX",
    ),
    NATCASH(
        id = "natcash",
        label = "NatCash",
        defaultCurrency = CurrencyCode.HTG,
        supportsHtg = true,
        accountHint = "+509 XXXX XXXX",
    ),
    BINANCE(
        id = "binance",
        label = "Binance Pay",
        defaultCurrency = CurrencyCode.USD,
        supportsHtg = false,
        accountHint = "Binance Pay ID / UID",
    ),
    MERU(
        id = "meru",
        label = "Meru",
        defaultCurrency = CurrencyCode.USD,
        supportsHtg = false,
        accountHint = "Tag Meru (@utilisateur)",
    ),
    NOWPAYMENTS(
        id = "nowpayments",
        label = "Crypto (NOWPayments)",
        defaultCurrency = CurrencyCode.USD,
        supportsHtg = false,
        accountHint = "Adresse Wallet Crypto",
    );

    companion object {
        fun fromId(id: String): PaymentMethod =
            entries.firstOrNull { it.id == id } ?: MONCASH
    }
}

enum class CurrencyCode(val code: String, val symbol: String) {
    USD("USD", "$"),
    HTG("HTG", "G"),
    EUR("EUR", "€");

    companion object {
        fun fromCode(code: String): CurrencyCode =
            entries.firstOrNull { it.code == code } ?: USD
    }
}

enum class CryptoCoin(val id: String, val displayName: String, val approxUsdRate: Double) {
    USDT_TRC20("usdttrc20", "USDT (TRC20)", 1.0),
    BTC("btc", "Bitcoin (BTC)", 92000.0),
    TRX("trx", "TRON (TRX)", 0.24);

    companion object {
        fun fromId(id: String): CryptoCoin =
            entries.firstOrNull { it.id == id } ?: USDT_TRC20
    }
}

enum class OrderStatus(val label: String) {
    PENDING("En attente"),
    SUBMITTED("Preuve soumise"),
    PAID_IN("Fonds reçus"),
    COMPLETED("Complétée"),
    UNDERPAID("Sous-payée"),
    REJECTED("Refusée"),
    EXPIRED("Expirée");

    companion object {
        fun fromString(value: String): OrderStatus =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}

@IgnoreExtraProperties
data class ExchangeConfig(
    val usdToHtgBuy: Double = 134.50,
    val usdToHtgSell: Double = 132.00,
    val eurToUsd: Double = 1.08,
    val feeNowpayments: Double = 1.5,
    val feeBinance: Double = 1.0,
    val feeMeru: Double = 1.5,
    val feeMoncash: Double = 2.0,
    val feeNatcash: Double = 2.0,
    val minOrderUsd: Double = 5.0,
    val maxOrderUsd: Double = 5000.0,
    val moncashMerchant: String = "+509 4687 7695",
    val natcashMerchant: String = "+509 3215 3281",
    val binancePayId: String = "ALEVINI-BINANCE-PAY",
    val meruTag: String = "@alevini.ht",
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val updatedBy: String = "",
) {
    fun feeFor(method: PaymentMethod): Double = when (method) {
        PaymentMethod.NOWPAYMENTS -> feeNowpayments
        PaymentMethod.BINANCE -> feeBinance
        PaymentMethod.MERU -> feeMeru
        PaymentMethod.MONCASH -> feeMoncash
        PaymentMethod.NATCASH -> feeNatcash
    }
}

data class ExchangeCalculation(
    val grossUsd: Double,
    val netUsd: Double,
    val amountHtg: Double,
    val amountEur: Double,
    val receiveAmount: Double,
    val baseRate: Double,
    val feePercent: Double,
    val effectiveRate: Double,
)

@IgnoreExtraProperties
data class ExchangeOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val orderCode: String = "",
    val sendMethod: String = "",
    val receiveMethod: String = "",
    val sendCurrency: String = "USD",
    val receiveCurrency: String = "HTG",
    val cryptoCoin: String = "",
    val sendAmount: Double = 0.0,
    val receiveAmount: Double = 0.0,
    val amountUsd: Double = 0.0,
    val amountHtg: Double = 0.0,
    val amountEur: Double = 0.0,
    val baseRate: Double = 0.0,
    val feePercent: Double = 0.0,
    val effectiveRate: Double = 0.0,
    val destAccount: String = "",
    val paymentInstruction: String = "",
    val payAddress: String = "",
    val payCryptoAmount: Double = 0.0,
    val externalTxRef: String = "",
    val status: String = "PENDING",
    val adminNote: String = "",
    val expiresAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val isAgeVerified: Boolean = true,
    val isBlocked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LedgerEntry(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val orderCode: String = "",
    val accountName: String = "",
    val entryType: String = "DEBIT",
    val currency: String = "USD",
    val amount: Double = 0.0,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AdminNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetUserId: String = "ALL",
    val targetLabel: String = "Tous les utilisateurs",
    val isAlert: Boolean = false,
    val sentBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)
