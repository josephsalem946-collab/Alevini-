package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.domain.ExchangeCalculator
import com.example.data.model.CryptoCoin
import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeConfig
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.ui.components.MethodChip
import com.example.ui.components.ScrollToTopButton
import com.example.ui.components.formatNum
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBright
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RubyContainer
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CalculatorUiState
import com.example.ui.viewmodel.ComputedQuote
import java.util.Locale

@Composable
fun ExchangeScreen(
    config: ExchangeConfig,
    userProfile: UserProfile?,
    calcState: CalculatorUiState,
    quote: ComputedQuote,
    onSelectSendMethod: (PaymentMethod) -> Unit,
    onSelectReceiveMethod: (PaymentMethod) -> Unit,
    onSwapMethods: () -> Unit,
    onSetSendCurrency: (CurrencyCode) -> Unit,
    onSetReceiveCurrency: (CurrencyCode) -> Unit,
    onSetCryptoCoin: (CryptoCoin) -> Unit,
    onSetSendAmount: (String) -> Unit,
    onSetDestAccount: (String) -> Unit,
    onCreateOrder: () -> Unit,
    onNavigateToTopUp: (PaymentMethod) -> Unit,
) {
    var swapFlipped by remember { mutableStateOf(false) }
    val swapRotation by animateFloatAsState(
        targetValue = if (swapFlipped) 180f else 0f,
        animationSpec = spring(),
        label = "swap_rotation",
    )

    val sendAmountParsed = calcState.sendAmountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val availableGatewayBalance = userProfile?.balanceFor(calcState.sendMethod) ?: 0.0
    val gatewayCurrency = calcState.sendMethod.defaultCurrency
    val requiredDebit = remember(config, calcState.sendMethod, calcState.sendCurrency, sendAmountParsed) {
        ExchangeCalculator.requiredGatewayDebit(
            config = config,
            sendMethod = calcState.sendMethod,
            sendCurrency = calcState.sendCurrency,
            sendAmount = sendAmountParsed,
        )
    }
    val hasEnoughFunds = sendAmountParsed > 0.0 && availableGatewayBalance + 1e-6 >= requiredDebit
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            // Blocked Account Warning
            if (userProfile?.isBlocked == true) {
                Surface(
                    color = RubyContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, RubyError),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = RubyError)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Votre compte est temporairement suspendu par l'administration.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                    }
                }
            }

            // Live Rates Hero Banner
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(0.75.dp, ObsidianOutline),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.alevini_hero_banner_1794757901223),
                    contentDescription = "Taux en direct Alevini",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ObsidianSurface.copy(alpha = 0.93f),
                                    ObsidianSurfaceBright.copy(alpha = 0.85f),
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = ForestGreen,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Taux en direct • Alevini",
                                style = MaterialTheme.typography.labelLarge,
                                color = ForestGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            color = GoldContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = "Verrou 15 min",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RatePill(
                            title = "Achat HTG",
                            value = String.format(Locale.US, "%.2f G/$", config.usdToHtgBuy),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        RatePill(
                            title = "Vente HTG",
                            value = String.format(Locale.US, "%.2f G/$", config.usdToHtgSell),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        RatePill(
                            title = "EUR / USD",
                            value = String.format(Locale.US, "%.2f $", config.eurToUsd),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SEND SECTION CARD
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(0.75.dp, ObsidianOutline),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Vous envoyez",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        CurrencySelectorRow(
                            selected = calcState.sendCurrency,
                            onSelect = onSetSendCurrency,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            MethodChip(
                                method = method,
                                selected = calcState.sendMethod == method,
                                feePercent = config.feeFor(method),
                                onClick = { onSelectSendMethod(method) },
                                modifier = Modifier.testTag("send_method_${method.id}"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = if (hasEnoughFunds || sendAmountParsed <= 0.0) {
                            EmeraldContainer.copy(alpha = 0.4f)
                        } else {
                            RubyContainer.copy(alpha = 0.55f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (hasEnoughFunds || sendAmountParsed <= 0.0) EmeraldPrimary.copy(alpha = 0.45f) else RubyError.copy(alpha = 0.6f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Solde ${calcState.sendMethod.label} : ${formatNum(availableGatewayBalance)} ${gatewayCurrency.code}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (hasEnoughFunds || sendAmountParsed <= 0.0) EmeraldPrimary else RubyError,
                                )
                                Text(
                                    text = "Frais passerelle : ${formatNum(config.feeFor(calcState.sendMethod))}% • Requis : ${formatNum(requiredDebit)} ${gatewayCurrency.code}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                            if (!hasEnoughFunds) {
                                Surface(
                                    color = RubyError.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { onNavigateToTopUp(calcState.sendMethod) },
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .testTag("exchange_inline_topup_badge"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+ Recharger",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = RubyError,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = calcState.sendMethod == PaymentMethod.NOWPAYMENTS) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Crypto de dépôt",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CryptoCoin.entries.forEach { coin ->
                                    FilterChip(
                                        selected = calcState.cryptoCoin == coin,
                                        onClick = { onSetCryptoCoin(coin) },
                                        label = { Text(coin.displayName, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldContainer,
                                            selectedLabelColor = EmeraldPrimary,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = calcState.sendAmountText,
                        onValueChange = onSetSendAmount,
                        label = { Text("Montant (${calcState.sendCurrency.code})") },
                        suffix = {
                            Text(
                                text = calcState.sendCurrency.code,
                                fontFamily = JetBrainsMonoFamily,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                            .testTag("send_amount_input"),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Amount Presets
                    val presets = if (calcState.sendCurrency == CurrencyCode.HTG) {
                        listOf("2500", "5000", "13200", "25000")
                    } else {
                        listOf("25", "50", "100", "500")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presets.forEach { preset ->
                            Surface(
                                color = ObsidianSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ObsidianOutline),
                                onClick = { onSetSendAmount(preset) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        text = "$preset ${calcState.sendCurrency.symbol}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SWAP BUTTON OVERLAP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = EmeraldContainer,
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, EmeraldPrimary),
                    shadowElevation = 6.dp,
                ) {
                    IconButton(
                        onClick = {
                            swapFlipped = !swapFlipped
                            onSwapMethods()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("swap_methods_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapVert,
                            contentDescription = "Inverser les méthodes",
                            tint = EmeraldPrimary,
                            modifier = Modifier.rotate(swapRotation),
                        )
                    }
                }
            }

            // RECEIVE SECTION CARD
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(0.75.dp, ObsidianOutline),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Vous recevez",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        CurrencySelectorRow(
                            selected = calcState.receiveCurrency,
                            onSelect = onSetReceiveCurrency,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            MethodChip(
                                method = method,
                                selected = calcState.receiveMethod == method,
                                feePercent = config.feeFor(method),
                                onClick = { onSelectReceiveMethod(method) },
                                modifier = Modifier.testTag("receive_method_${method.id}"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Calculated Output Display
                    Surface(
                        color = ObsidianSurfaceBright,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val calc = quote.calculation
                            if (calc != null) {
                                Text(
                                    text = "Montant net estimé",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val previewText = "${formatNum(calc.receiveAmount)} ${calcState.receiveCurrency.code}"
                                Text(
                                    text = previewText,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = if (previewText.length > 14) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                                    color = EmeraldPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("receive_amount_preview"),
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = ObsidianOutline.copy(alpha = 0.5f),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Frais totaux : ${formatNum(calc.feePercent)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    )
                                    Text(
                                        text = "1 ${calcState.sendCurrency.code} = ${String.format(Locale.US, "%.4f", calc.effectiveRate)} ${calcState.receiveCurrency.code}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldAccent,
                                        textAlign = TextAlign.End,
                                        maxLines = 2,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, tint = GoldAccent)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = quote.errorMessage ?: "Entrez un montant valide",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GoldAccent,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = calcState.destAccount,
                        onValueChange = onSetDestAccount,
                        label = { Text("Compte de réception (${calcState.receiveMethod.label})") },
                        placeholder = { Text(calcState.receiveMethod.accountHint, color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedContainerColor = Color(0xFFFAFDFB),
                            unfocusedContainerColor = Color(0xFFFAFDFB),
                            focusedLabelColor = ForestGreen,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dest_account_input"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (quote.calculation != null && !hasEnoughFunds && userProfile?.isBlocked != true) {
                Surface(
                    color = RubyContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, RubyError),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("exchange_insufficient_balance_banner"),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = RubyError)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Solde ${calcState.sendMethod.label} insuffisant",
                                style = MaterialTheme.typography.titleSmall,
                                color = RubyError,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "Vous disposez de ${formatNum(availableGatewayBalance)} ${gatewayCurrency.code} sur ${calcState.sendMethod.label} (${formatNum(requiredDebit)} ${gatewayCurrency.code} requis). Rechargez d'abord ce portefeuille.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                        )
                        Button(
                            onClick = { onNavigateToTopUp(calcState.sendMethod) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("exchange_topup_cta_btn"),
                        ) {
                            Text("Recharger ${calcState.sendMethod.label}", fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Button(
                onClick = onCreateOrder,
                enabled = quote.calculation != null &&
                    hasEnoughFunds &&
                    calcState.destAccount.trim().length >= 3 &&
                    !calcState.isSubmittingOrder &&
                    userProfile?.isBlocked != true,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = ObsidianBg,
                    disabledContainerColor = ObsidianSurfaceElevated,
                    disabledContentColor = TextMuted,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("create_order_button"),
            ) {
                if (calcState.isSubmittingOrder) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = ObsidianBg,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasEnoughFunds) {
                            "Échanger via solde ${calcState.sendMethod.label}"
                        } else {
                            "Solde ${calcState.sendMethod.label} insuffisant"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Marketing Card (all gateway details and figures intentionally hidden)
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alevini_marketing_card"),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Alevini — Votre partenaire financier de confiance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                    )
                    Text(
                        text = "Connectez vos portefeuilles locaux et numériques en toute sérénité grâce à une expérience moderne, fluide et sécurisée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = "• Rapidité d'exécution : vos conversions traitées avec réactivité.",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                    )
                    Text(
                        text = "• Protection avancée : accès sécurisé par code PIN et validation transparente.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = "• Liberté sans frontières : passez de vos fonds locaux aux services digitaux à tout moment.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
        ScrollToTopButton(scrollState = scrollState)
    }
}

@Composable
private fun RatePill(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = ObsidianSurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.75.dp, ObsidianOutline),
        shadowElevation = 1.dp,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CurrencySelectorRow(
    selected: CurrencyCode,
    onSelect: (CurrencyCode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CurrencyCode.entries.forEach { currency ->
            val isSelected = currency == selected
            Surface(
                color = if (isSelected) EmeraldPrimary else ObsidianSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else ObsidianOutline),
                onClick = { onSelect(currency) },
            ) {
                Text(
                    text = currency.code,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) ObsidianBg else TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
