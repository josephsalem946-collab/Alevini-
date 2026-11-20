package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.domain.ExchangeCalculator
import com.example.data.model.CryptoCoin
import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeConfig
import com.example.data.model.ExchangeOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.ui.components.GatewayLogo
import com.example.ui.components.formatNum
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RubyContainer
import com.example.ui.theme.RubyError
import com.example.ui.theme.SkyContainer
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.ceil

internal data class GatewayMethodMeta(
    val method: PaymentMethod,
    val title: String,
    val subtitle: String,
    val badge: String,
    val badgeBg: Color,
    val badgeFg: Color,
)

@Composable
internal fun DepositStepOneContent(
    config: ExchangeConfig,
    walletSummary: com.example.ui.viewmodel.WalletSummary,
    isBalanceHidden: Boolean,
    selectedMethod: PaymentMethod,
    onSelectMethod: (PaymentMethod) -> Unit,
    onContinue: () -> Unit,
) {
    val methods = remember {
        listOf(
            GatewayMethodMeta(
                method = PaymentMethod.MONCASH,
                title = "MonCash - Online",
                subtitle = "Paiement mobile via Digicel",
                badge = "HTG",
                badgeBg = SkyContainer,
                badgeFg = SkyInfo,
            ),
            GatewayMethodMeta(
                method = PaymentMethod.NATCASH,
                title = "NatCash - Online",
                subtitle = "Paiement mobile via Natcom",
                badge = "HTG",
                badgeBg = EmeraldContainer,
                badgeFg = EmeraldPrimary,
            ),
            GatewayMethodMeta(
                method = PaymentMethod.BINANCE,
                title = "Binance Pay",
                subtitle = "Passerelle Binance officielle",
                badge = "USD",
                badgeBg = GoldContainer,
                badgeFg = GoldAccent,
            ),
            GatewayMethodMeta(
                method = PaymentMethod.MERU,
                title = "Meru",
                subtitle = "Recharge directe Tag Meru",
                badge = "USD",
                badgeBg = ObsidianSurfaceElevated,
                badgeFg = GoldAccent,
            ),
            GatewayMethodMeta(
                method = PaymentMethod.NOWPAYMENTS,
                title = "NOWPayments",
                subtitle = "USDT TRC-20, BTC, TRX & autres cryptos",
                badge = "Crypto USD",
                badgeBg = ObsidianSurfaceElevated,
                badgeFg = EmeraldPrimary,
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Recharger un portefeuille",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Chaque passerelle possède son propre solde et ses frais de dépôt",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )

        methods.forEach { meta ->
            val isSelected = selectedMethod == meta.method
            val feePercent = config.feeFor(meta.method)
            val currentBal = walletSummary.gatewayFor(meta.method)?.balance ?: 0.0
            val balLabel = if (isBalanceHidden) {
                "•••• ${meta.method.defaultCurrency.code}"
            } else if (meta.method.supportsHtg) {
                "${formatNum(currentBal)} HTG"
            } else {
                "$${formatNum(currentBal)} USD"
            }
            val limitsLabel = if (meta.method.supportsHtg) {
                val minHtg = ceil(config.minOrderUsd * config.usdToHtgSell)
                val maxHtg = config.maxOrderUsd * config.usdToHtgSell
                "Solde : $balLabel • Frais : ${formatNum(feePercent)}% • Min ${formatNum(minHtg)} HTG (Max ${formatNum(maxHtg)})"
            } else {
                "Solde : $balLabel • Frais : ${formatNum(feePercent)}% • Min $${formatNum(config.minOrderUsd)} (Max $${formatNum(config.maxOrderUsd)})"
            }

            Surface(
                color = if (isSelected) EmeraldContainer.copy(alpha = 0.28f) else ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.75.dp else 1.dp,
                    color = if (isSelected) EmeraldPrimary else ObsidianOutline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectMethod(meta.method) }
                    .testTag("wizard_method_${meta.method.id}"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GatewayLogo(
                        method = meta.method,
                        size = 46.dp,
                        cornerRadius = 12.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = meta.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Surface(
                                color = meta.badgeBg,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = meta.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = meta.badgeFg,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = meta.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Text(
                            text = limitsLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectMethod(meta.method) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = EmeraldPrimary,
                            unselectedColor = TextMuted,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = ObsidianBg,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("wizard_step1_continue"),
        ) {
            Text("Continuer avec ce mode", fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DepositStepTwoContent(
    config: ExchangeConfig,
    walletSummary: com.example.ui.viewmodel.WalletSummary,
    sendMethod: PaymentMethod,
    cryptoCoin: CryptoCoin,
    amountText: String,
    isConfirmed: Boolean,
    isSubmitting: Boolean,
    onBackToMode: () -> Unit,
    onAmountChange: (String) -> Unit,
    onSelectCryptoCoin: (CryptoCoin) -> Unit,
    onToggleConfirm: (Boolean) -> Unit,
    onValidateAndCreateTopUp: () -> Unit,
) {
    val nativeCurrency = sendMethod.defaultCurrency
    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val currentGatewayBalance = walletSummary.gatewayFor(sendMethod)?.balance ?: 0.0

    val quoteResult = remember(config, parsedAmount, sendMethod) {
        ExchangeCalculator.computeTopUp(
            config = config,
            gatewayMethod = sendMethod,
            amount = parsedAmount,
        )
    }

    val approxEquivalent = if (nativeCurrency == CurrencyCode.HTG) {
        val usd = parsedAmount / config.usdToHtgSell
        "≈ $${formatNum(usd)} USD"
    } else {
        val htg = parsedAmount * config.usdToHtgSell
        "≈ ${formatNum(htg)} HTG"
    }

    val limitsText = if (nativeCurrency == CurrencyCode.HTG) {
        val minHtg = ceil(config.minOrderUsd * config.usdToHtgSell)
        val maxHtg = config.maxOrderUsd * config.usdToHtgSell
        "Min : ${formatNum(minHtg)} HTG • Max : ${formatNum(maxHtg)} HTG"
    } else {
        "Min : $${formatNum(config.minOrderUsd)} USD • Max : $${formatNum(config.maxOrderUsd)} USD"
    }

    val presets = if (nativeCurrency == CurrencyCode.HTG) {
        listOf(
            "100" to "100 HTG",
            "250" to "250 HTG",
            "500" to "500 HTG",
            "1000" to "1 000 HTG",
            "2500" to "2 500 HTG",
            "5000" to "5 000 HTG",
            "10000" to "10k HTG",
            "25000" to "25k HTG",
        )
    } else {
        listOf(
            "5" to "$5 USD",
            "10" to "$10 USD",
            "25" to "$25 USD",
            "50" to "$50 USD",
            "100" to "$100 USD",
            "250" to "$250 USD",
            "500" to "$500 USD",
            "1000" to "$1 000",
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Définir le montant",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Moyen choisi : ${sendMethod.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldPrimary,
                )
            }
            TextButton(
                onClick = onBackToMode,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Changer", color = EmeraldPrimary)
            }
        }

        // Limits banner
        Surface(
            color = EmeraldContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Limites de dépôt", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    Text(limitsText, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
            }
        }

        if (sendMethod == PaymentMethod.NOWPAYMENTS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CryptoCoin.entries.forEach { coin ->
                    FilterChip(
                        selected = cryptoCoin == coin,
                        onClick = { onSelectCryptoCoin(coin) },
                        label = { Text(coin.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = EmeraldPrimary,
                        ),
                    )
                }
            }
        }

        // Amount input card
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ObsidianOutline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MONTANT À RECHARGER (${nativeCurrency.code})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        onAmountChange(raw.filter { it.isDigit() || it == '.' || it == ',' }.take(12))
                    },
                    placeholder = { Text("0.00", color = TextMuted) },
                    suffix = { Text(nativeCurrency.code, color = EmeraldPrimary, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_amount_input"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Conversion estimée :",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Surface(
                        color = EmeraldContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = approxEquivalent,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldPrimary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // Quick 8 amount presets
        Text(
            text = "MONTANTS RAPIDES",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 4,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { (valStr, label) ->
                val isSelected = amountText == valStr
                OutlinedButton(
                    onClick = { onAmountChange(valStr) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else ObsidianOutline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) EmeraldContainer.copy(alpha = 0.5f) else ObsidianSurface,
                        contentColor = if (isSelected) EmeraldPrimary else TextPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Gateway Top-Up Fee & Net Credit Breakdown
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ObsidianOutline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CRÉDIT SUR VOTRE PORTEFEUILLE ${sendMethod.label.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Solde actuel (${sendMethod.label})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Text(
                        text = "${formatNum(currentGatewayBalance)} ${nativeCurrency.code}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        maxLines = 1,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Frais passerelle (${sendMethod.label})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Text(
                        text = "${formatNum(config.feeFor(sendMethod))}%",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldAccent,
                        maxLines = 1,
                    )
                }
                when (quoteResult) {
                    is ExchangeCalculator.ValidationResult.Valid -> {
                        val calc = quoteResult.calculation
                        Text(
                            text = "Net crédité sur ${sendMethod.label} : +${formatNum(calc.receiveAmount)} ${nativeCurrency.code}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    is ExchangeCalculator.ValidationResult.Invalid -> {
                        if (parsedAmount > 0.0) {
                            Text(
                                text = quoteResult.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = RubyError,
                            )
                        }
                    }
                }
            }
        }

        // Confirmation checkbox
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isConfirmed) EmeraldPrimary.copy(alpha = 0.6f) else ObsidianOutline),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onToggleConfirm(!isConfirmed) },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isConfirmed,
                    onCheckedChange = onToggleConfirm,
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldPrimary,
                        checkmarkColor = ObsidianBg,
                        uncheckedColor = TextMuted,
                    ),
                    modifier = Modifier.testTag("wizard_confirm_checkbox"),
                )
                Text(
                    text = "Je confirme effectuer cette recharge vers mon portefeuille ${sendMethod.label}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        val canProceed = quoteResult is ExchangeCalculator.ValidationResult.Valid &&
            isConfirmed &&
            !isSubmitting

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onBackToMode,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .heightIn(min = 52.dp),
            ) {
                Text(
                    text = "Changer mode",
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onValidateAndCreateTopUp,
                enabled = canProceed,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = ObsidianBg,
                ),
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .heightIn(min = 52.dp)
                    .testTag("wizard_step2_next"),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ObsidianBg,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "VALIDER / SUIVANT",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
internal fun DepositStepThreeContent(
    order: ExchangeOrder,
    config: ExchangeConfig,
    isGatewayBusy: Boolean,
    onBackToAmount: () -> Unit,
    onInitiateOnlinePayment: () -> Unit,
    onVerifyOnlinePayment: () -> Unit,
    onSubmitTxRef: (String) -> Unit,
    onResetNewDeposit: () -> Unit,
) {
    val context = LocalContext.current
    val sendMethod = PaymentMethod.fromId(order.sendMethod)
    val status = OrderStatus.fromString(order.status)
    var txRefInput by remember(order.id) { mutableStateOf(order.externalTxRef) }
    var copiedMessage by remember { mutableStateOf(false) }

    var remainingMillis by remember(order.id, order.expiresAtMillis) {
        mutableLongStateOf((order.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
    }
    LaunchedEffect(order.id, status) {
        if (status == OrderStatus.PENDING) {
            while (remainingMillis > 0L) {
                delay(1000L)
                remainingMillis = (order.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Passerelle de Paiement",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Ordre ${order.orderCode} • ${sendMethod.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldPrimary,
                    fontFamily = JetBrainsMonoFamily,
                )
            }
            TextButton(
                onClick = onBackToAmount,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nouveau", color = EmeraldPrimary)
            }
        }

        // Countdown & summary banner
        Surface(
            color = SkyContainer.copy(alpha = 0.7f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Language, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Redirection API sécurisée active (${config.gatewayMode})",
                        style = MaterialTheme.typography.titleSmall,
                        color = SkyInfo,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "Montant à déposer : ${formatNum(order.sendAmount)} ${order.sendCurrency} → Crédit net : ${formatNum(order.receiveAmount)} ${order.receiveCurrency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                )
                if (status == OrderStatus.PENDING) {
                    val mins = (remainingMillis / 60000L).toInt()
                    val secs = ((remainingMillis % 60000L) / 1000L).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "Taux verrouillé : %02d:%02d restantes", mins, secs),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent,
                        )
                    }
                }
            }
        }

        // CRITICAL SECURITY BANNER: NEVER ASK FOR USER MOBILE MONEY PIN INSIDE APP
        Surface(
            color = EmeraldContainer.copy(alpha = 0.35f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Sécurité PIN / OTP maximale",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Alevini ne vous demandera JAMAIS votre code PIN secret ni mot de passe dans l'application. L'autorisation se fait uniquement sur le portail officiel du partenaire ou via votre ID de transaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                    )
                }
            }
        }

        // Portal Launch / Webhook verification
        if (sendMethod.supportsOnlineApi && config.isOnlineEnabledFor(sendMethod)) {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Option 1 • Portail officiel ${sendMethod.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    if (config.backendBaseUrl.isBlank()) {
                        Text(
                            text = "URL serveur backend en attente dans Admin > Passerelles API. Vous pouvez finaliser immédiatement via l'Option 2 ci-dessous.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    } else if (order.checkoutUrl.isBlank()) {
                        Button(
                            onClick = onInitiateOnlinePayment,
                            enabled = !isGatewayBusy && remainingMillis > 0L,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SkyInfo,
                                contentColor = ObsidianBg,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("wizard_initiate_portal_btn"),
                        ) {
                            if (isGatewayBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianBg, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Générer le lien portail officiel", fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(order.checkoutUrl)))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SkyInfo,
                                    contentColor = ObsidianBg,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 50.dp)
                                    .testTag("wizard_open_external_portal_btn"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accéder au portail", fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
                            }
                            OutlinedButton(
                                onClick = onVerifyOnlinePayment,
                                enabled = !isGatewayBusy,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                border = BorderStroke(1.dp, EmeraldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .heightIn(min = 50.dp)
                                    .testTag("wizard_verify_webhook_btn"),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Vérifier", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vérifier", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Instructions + Direct TX Reference verification
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, ObsidianOutline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Option 2 • Transfert & Référence TX",
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldPrimary,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val copyText = order.payAddress.ifBlank { order.paymentInstruction }
                            clipboard.setPrimaryClip(ClipData.newPlainText("Alevini", copyText))
                            copiedMessage = true
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copier instructions", tint = EmeraldPrimary)
                    }
                }

                Text(
                    text = order.paymentInstruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
                if (copiedMessage) {
                    Text("Instructions copiées !", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                }

                if (status == OrderStatus.PENDING) {
                    OutlinedTextField(
                        value = txRefInput,
                        onValueChange = { txRefInput = it },
                        label = { Text("ID de transaction / Référence", color = TextSecondary) },
                        placeholder = { Text("Ex: MC-884422 ou ID Binance", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_tx_ref_input"),
                    )
                    Button(
                        onClick = { onSubmitTxRef(txRefInput) },
                        enabled = txRefInput.trim().length >= 4 && remainingMillis > 0L,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = ObsidianBg,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("wizard_finalize_deposit_btn"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmer et Finaliser le Dépôt", fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
                    }
                } else {
                    Surface(
                        color = EmeraldContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Statut : ${status.label} (${order.externalTxRef})",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onResetNewDeposit,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Effectuer un autre dépôt", fontWeight = FontWeight.SemiBold, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
