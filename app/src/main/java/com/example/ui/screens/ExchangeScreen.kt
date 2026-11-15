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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CryptoCoin
import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeConfig
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.ui.components.MethodChip
import com.example.ui.components.formatNum
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
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
) {
    var swapFlipped by remember { mutableStateOf(false) }
    val swapRotation by animateFloatAsState(
        targetValue = if (swapFlipped) 180f else 0f,
        animationSpec = spring(),
        label = "swap_rotation",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, ObsidianOutline, RoundedCornerShape(22.dp))
            ) {
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
                                    ObsidianBg.copy(alpha = 0.92f),
                                    ObsidianBg.copy(alpha = 0.62f),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Taux en direct • Alevini",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary,
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

            Spacer(modifier = Modifier.height(16.dp))

            // SEND SECTION CARD
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
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

                    AnimatedVisibility(visible = calcState.sendMethod == PaymentMethod.NOWPAYMENTS) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Crypto de dépôt",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                ) {
                                    Text(
                                        text = "$preset ${calcState.sendCurrency.symbol}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
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
                border = BorderStroke(1.dp, ObsidianOutline),
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
                                Text(
                                    text = "${formatNum(calc.receiveAmount)} ${calcState.receiveCurrency.code}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.testTag("receive_amount_preview"),
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = ObsidianOutline.copy(alpha = 0.5f),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Frais totaux : ${formatNum(calc.feePercent)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                    Text(
                                        text = "1 ${calcState.sendCurrency.code} = ${String.format(Locale.US, "%.4f", calc.effectiveRate)} ${calcState.receiveCurrency.code}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GoldAccent,
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
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dest_account_input"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateOrder,
                enabled = quote.calculation != null &&
                    calcState.destAccount.trim().length >= 3 &&
                    !calcState.isSubmittingOrder &&
                    userProfile?.isBlocked != true,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = ObsidianBg,
                    disabledContainerColor = ObsidianSurfaceElevated,
                    disabledContentColor = TextMuted,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
                        text = "Créer l'ordre & Verrouiller le taux",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Official Gateways Reference Card
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Passerelles officielles Alevini",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "MonCash : ${config.moncashMerchant}  •  NatCash : ${config.natcashMerchant}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Limites : ${formatNum(config.minOrderUsd)} USD – ${formatNum(config.maxOrderUsd)} USD",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RatePill(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = ObsidianSurfaceElevated.copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
