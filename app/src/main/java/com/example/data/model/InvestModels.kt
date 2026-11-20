package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.round

enum class InvestCategory(
    val id: String,
    val shortTitle: String,
    val title: String,
    val badge: String,
    val monthlyRate: Double,
    val rateLabel: String,
    val terms: String,
    val defaultSector: String,
) {
    SANS_RISQUE(
        id = "sans_risque",
        shortTitle = "Sans risque",
        title = "Investissement sans risque",
        badge = "GARANTI • FIXE",
        monthlyRate = 0.008,
        rateLabel = "400 GDS / mois par tranche de 50 000 GDS (0,8 % / mois)",
        terms = "Après avoir effectué l'investissement, le client doit seulement attendre pour toucher son profit chaque mois, puis attendre le remboursement du capital initial à la fin de la période d'investissement.",
        defaultSector = "Capital Garanti MCSE",
    ),
    MOINS_RISQUE(
        id = "moins_risque",
        shortTitle = "Moins de risque",
        title = "Investissement à moins de risque",
        badge = "TRANSPARENT • MODÉRÉ",
        monthlyRate = 0.011,
        rateLabel = "Plus de 500 GDS / mois par tranche de 50 000 GDS (~1,1 % / mois)",
        terms = "Le client est informé dans quoi son argent est investi (Immobilier locatif MCSE, Commerce & Import-Export, Micro-crédit garanti). Il dispose d'informations transparentes montrant avec authenticité comment les profits augmentent ou diminuent.",
        defaultSector = "Portefeuille Mixte MCSE",
    ),
    A_RISQUE(
        id = "a_risque",
        shortTitle = "À risque",
        title = "Investissement à risque",
        badge = "HAUT RENDEMENT • CHOIX LIBRE",
        monthlyRate = 0.023,
        rateLabel = "Plus de 1 000 GDS / mois par tranche de 50 000 GDS (~2,3 % / mois)",
        terms = "Le client est au courant de l'investissement de ses fonds, dispose d'informations sur l'évolution des profits (hausse/baisse), et choisit lui-même dans quoi il souhaite investir.",
        defaultSector = "Trading Crypto & Devises",
    );

    companion object {
        fun fromId(id: String): InvestCategory =
            entries.firstOrNull { it.id == id } ?: SANS_RISQUE
    }
}

data class InvestTransparencyItem(
    val sector: String,
    val trend: String,
    val positive: Boolean,
    val note: String,
)

data class InvestRiskSector(
    val id: String,
    val name: String,
    val performance: String,
    val positive: Boolean,
    val riskNote: String,
)

enum class InvestCurrency(
    val code: String,
    val label: String,
    val amounts: List<Double>,
    val defaultAmount: Double,
) {
    GDS(
        code = "GDS",
        label = "Gourdes (GDS)",
        amounts = listOf(25000.0, 50000.0, 100000.0, 250000.0),
        defaultAmount = 50000.0,
    ),
    USD(
        code = "USD",
        label = "Dollars (\$USD)",
        amounts = listOf(500.0, 1000.0, 2500.0, 5000.0),
        defaultAmount = 1000.0,
    );

    companion object {
        fun fromCode(code: String): InvestCurrency =
            entries.firstOrNull { it.code == code } ?: GDS
    }
}

enum class InvestDuration(
    val id: String,
    val label: String,
    val months: Int,
) {
    ONE_MONTH("1m", "1 mois", 1),
    THREE_MONTHS("3m", "3 mois", 3),
    SIX_MONTHS("6m", "6 mois", 6),
    ONE_YEAR("1y", "1 an", 12);

    companion object {
        fun fromMonths(months: Int): InvestDuration =
            entries.firstOrNull { it.months == months } ?: THREE_MONTHS
    }
}

enum class InvestStatus(val label: String) {
    PENDING("En attente de validation Admin"),
    ACTIVE("Validé / Actif"),
    REJECTED("Refusé");

    companion object {
        fun fromString(value: String): InvestStatus =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}

@IgnoreExtraProperties
data class InvestmentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val investCode: String = "",
    val categoryKey: String = InvestCategory.SANS_RISQUE.id,
    val categoryName: String = InvestCategory.SANS_RISQUE.title,
    val sector: String = InvestCategory.SANS_RISQUE.defaultSector,
    val amount: Double = 0.0,
    val currency: String = InvestCurrency.GDS.code,
    val durationLabel: String = InvestDuration.THREE_MONTHS.label,
    val months: Int = InvestDuration.THREE_MONTHS.months,
    val monthlyProfit: Double = 0.0,
    val totalProfit: Double = 0.0,
    val gatewayId: String = PaymentMethod.MONCASH.id,
    val gatewayName: String = PaymentMethod.MONCASH.label,
    val feeAmount: Double = 0.0,
    val totalPaid: Double = 0.0,
    val endDateLabel: String = "",
    val status: String = InvestStatus.PENDING.name,
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class InvestmentSimulation(
    val monthlyProfit: Double,
    val totalProfit: Double,
    val feeAmount: Double,
    val totalCost: Double,
    val hasEnoughBalance: Boolean,
)

object InvestCatalog {
    const val GATEWAY_FEE_RATE = 0.015

    val SUPPORTED_GATEWAYS: List<PaymentMethod> = listOf(
        PaymentMethod.MONCASH,
        PaymentMethod.NATCASH,
        PaymentMethod.BINANCE,
        PaymentMethod.NOWPAYMENTS,
    )

    val MOINS_RISQUE_FEED: List<InvestTransparencyItem> = listOf(
        InvestTransparencyItem(
            sector = "Immobilier locatif MCSE",
            trend = "+1.24%",
            positive = true,
            note = "Taux d’occupation 98%",
        ),
        InvestTransparencyItem(
            sector = "Commerce & Import-Export",
            trend = "+1.08%",
            positive = true,
            note = "Rotation stocks régulière",
        ),
        InvestTransparencyItem(
            sector = "Micro-crédit garanti",
            trend = "-0.12%",
            positive = false,
            note = "Ajustement provision mensuelle",
        ),
    )

    val A_RISQUE_SECTORS: List<InvestRiskSector> = listOf(
        InvestRiskSector(
            id = "crypto_fx",
            name = "Trading Crypto & Devises",
            performance = "+2.65% ce mois",
            positive = true,
            riskNote = "Volatilité élevée sur paires USDT / HTG / BTC",
        ),
        InvestRiskSector(
            id = "tech_mcse",
            name = "Tech & Applications MCSE",
            performance = "+2.15% ce mois",
            positive = true,
            riskNote = "Croissance des transactions digitales Alevini",
        ),
        InvestRiskSector(
            id = "energie_log",
            name = "Énergie & Logistique",
            performance = "-0.40% semaine dernière",
            positive = false,
            riskNote = "Variation liée aux coûts d’approvisionnement",
        ),
    )

    fun currencyForGateway(method: PaymentMethod): InvestCurrency =
        if (method.supportsHtg) InvestCurrency.GDS else InvestCurrency.USD

    fun defaultGatewayForCurrency(currency: InvestCurrency): PaymentMethod = when (currency) {
        InvestCurrency.GDS -> PaymentMethod.MONCASH
        InvestCurrency.USD -> PaymentMethod.BINANCE
    }

    fun calculateSimulation(
        category: InvestCategory,
        amount: Double,
        duration: InvestDuration,
        availableBalance: Double,
    ): InvestmentSimulation {
        val monthlyProfit = round2(amount * category.monthlyRate)
        val totalProfit = round2(monthlyProfit * duration.months)
        val fee = round2(amount * GATEWAY_FEE_RATE)
        val totalCost = round2(amount + fee)
        val hasEnough = availableBalance + 1e-6 >= totalCost
        return InvestmentSimulation(
            monthlyProfit = monthlyProfit,
            totalProfit = totalProfit,
            feeAmount = fee,
            totalCost = totalCost,
            hasEnoughBalance = hasEnough,
        )
    }

    fun computeMaturityDateLabel(months: Int, startDate: Date = Date()): String {
        val cal = Calendar.getInstance().apply {
            time = startDate
            add(Calendar.MONTH, months)
        }
        return SimpleDateFormat("dd MMM yyyy", Locale.FRANCE).format(cal.time)
    }

    fun formatInvestMoney(amount: Double, currencyCode: String): String {
        return if (currencyCode.equals("GDS", ignoreCase = true) || currencyCode.equals("HTG", ignoreCase = true)) {
            val nf = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 0
            }
            "${nf.format(amount)} GDS"
        } else {
            val nf = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }
            "${nf.format(amount)} \$USD"
        }
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
