package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CurrencyBitcoin
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ExchangeConfig
import com.example.data.model.ExchangeOrder
import com.example.data.model.GatewayStatus
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
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
import com.example.ui.theme.SkyContainer
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

fun methodIcon(method: PaymentMethod): ImageVector = when (method) {
    PaymentMethod.MONCASH, PaymentMethod.NATCASH -> Icons.Rounded.PhoneAndroid
    PaymentMethod.BINANCE, PaymentMethod.MERU -> Icons.Rounded.AccountBalanceWallet
    PaymentMethod.NOWPAYMENTS -> Icons.Rounded.CurrencyBitcoin
}

fun methodTint(method: PaymentMethod): Color = when (method) {
    PaymentMethod.MONCASH -> Color(0xFFFF4B4B)
    PaymentMethod.NATCASH -> Color(0xFFF38020)
    PaymentMethod.BINANCE -> Color(0xFFF4B942)
    PaymentMethod.MERU -> Color(0xFF9E86FF)
    PaymentMethod.NOWPAYMENTS -> EmeraldPrimary
}

fun methodLogoDrawable(method: PaymentMethod): Int = when (method) {
    PaymentMethod.MONCASH -> R.drawable.logo_moncash
    PaymentMethod.NATCASH -> R.drawable.logo_natcash
    PaymentMethod.BINANCE -> R.drawable.logo_binance
    PaymentMethod.MERU -> R.drawable.logo_meru
    PaymentMethod.NOWPAYMENTS -> R.drawable.logo_nowpayments
}

@Composable
fun GatewayLogo(
    method: PaymentMethod,
    size: Dp = 28.dp,
    cornerRadius: Dp = 8.dp,
    modifier: Modifier = Modifier,
) {
    val backdrop = when (method) {
        PaymentMethod.MONCASH, PaymentMethod.MERU -> Color.White
        PaymentMethod.NATCASH -> Color(0xFFF38020)
        PaymentMethod.BINANCE -> Color(0xFF181A20)
        PaymentMethod.NOWPAYMENTS -> Color(0xFF12161E)
    }
    val innerPad = when (method) {
        PaymentMethod.NATCASH -> 0.dp
        else -> (size * 0.08f)
    }
    Surface(
        color = backdrop,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(0.6.dp, ObsidianOutline.copy(alpha = 0.55f)),
        modifier = modifier.size(size),
    ) {
        Image(
            painter = painterResource(id = methodLogoDrawable(method)),
            contentDescription = method.label,
            contentScale = if (method == PaymentMethod.NATCASH) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPad),
        )
    }
}

@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING -> GoldContainer to GoldAccent
        OrderStatus.SUBMITTED, OrderStatus.PAID_IN -> SkyContainer to SkyInfo
        OrderStatus.COMPLETED -> EmeraldContainer to EmeraldPrimary
        OrderStatus.UNDERPAID, OrderStatus.REJECTED, OrderStatus.EXPIRED -> RubyContainer to RubyError
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(if (compact) 0.8.dp else 1.dp, fg.copy(alpha = 0.4f)),
        modifier = modifier,
    ) {
        Text(
            text = status.label,
            style = if (compact) {
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp,
                )
            } else {
                MaterialTheme.typography.labelSmall
            },
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = if (compact) 7.dp else 10.dp,
                vertical = if (compact) 2.5.dp else 4.dp,
            ),
        )
    }
}

@Composable
fun MethodChip(
    method: PaymentMethod,
    selected: Boolean,
    feePercent: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) EmeraldPrimary else ObsidianOutline
    val bgColor = if (selected) EmeraldContainer.copy(alpha = 0.7f) else ObsidianSurfaceElevated
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            GatewayLogo(
                method = method,
                size = 28.dp,
                cornerRadius = 8.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = method.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Frais ${formatNum(feePercent)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) EmeraldPrimary else TextMuted,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailBottomSheet(
    order: ExchangeOrder,
    config: ExchangeConfig,
    isGatewayBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmitTxRef: (String) -> Unit,
    onInitiateOnlinePayment: () -> Unit,
    onVerifyOnlinePayment: () -> Unit,
    onMarkExpired: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var txRefInput by remember(order.id) { mutableStateOf(order.externalTxRef) }
    var copiedNotice by remember { mutableStateOf(false) }

    val sendMethod = PaymentMethod.fromId(order.sendMethod)
    val status = OrderStatus.fromString(order.status)
    val gatewayStatus = GatewayStatus.fromString(order.gatewayStatus)
    var remainingMillis by remember(order.id, order.expiresAtMillis) {
        mutableLongStateOf((order.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
    }

    LaunchedEffect(order.id, status) {
        if (status == OrderStatus.PENDING) {
            while (remainingMillis > 0L) {
                delay(1000L)
                remainingMillis = (order.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            }
            if (remainingMillis == 0L) {
                onMarkExpired()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    val isTopUpOrder = order.orderType == "TOPUP" || order.sendMethod == order.receiveMethod
                    Text(
                        text = order.orderCode,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        color = EmeraldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isTopUpOrder) {
                            "Recharge Portefeuille • ${PaymentMethod.fromId(order.sendMethod).label}"
                        } else {
                            "${PaymentMethod.fromId(order.sendMethod).label} → ${PaymentMethod.fromId(order.receiveMethod).label}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OrderStatusBadge(status = status)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fermer", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Countdown Banner for PENDING orders
            if (status == OrderStatus.PENDING) {
                val mins = (remainingMillis / 60000L).toInt()
                val secs = ((remainingMillis % 60000L) / 1000L).toInt()
                Surface(
                    color = GoldContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = String.format(
                                Locale.US,
                                "Taux verrouillé : %02d:%02d restantes",
                                mins,
                                secs
                            ),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldAccent,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Summary Amounts Card
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SummaryRow(
                        label = "Vous envoyez",
                        value = "${formatNum(order.sendAmount)} ${order.sendCurrency}",
                        highlight = false,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow(
                        label = "Vous recevez",
                        value = "${formatNum(order.receiveAmount)} ${order.receiveCurrency}",
                        highlight = true,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = ObsidianOutline.copy(alpha = 0.6f),
                    )
                    SummaryRow(label = "Frais appliqués", value = "${formatNum(order.feePercent)}%", highlight = false)
                    Spacer(modifier = Modifier.height(4.dp))
                    SummaryRow(label = "Taux effectif", value = String.format(Locale.US, "%.4f", order.effectiveRate), highlight = false)
                    Spacer(modifier = Modifier.height(4.dp))
                    SummaryRow(label = "Compte de réception", value = order.destAccount, highlight = false)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Instructions Card
            Surface(
                color = ObsidianSurfaceBright,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Instructions de paiement",
                            style = MaterialTheme.typography.titleSmall,
                            color = EmeraldPrimary,
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val textToCopy = if (order.payAddress.isNotBlank()) order.payAddress else order.paymentInstruction
                                clipboard.setPrimaryClip(ClipData.newPlainText("Alevini", textToCopy))
                                copiedNotice = true
                            },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copier les instructions",
                                tint = EmeraldPrimary,
                            )
                        }
                    }
                    Text(
                        text = order.paymentInstruction,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    if (order.payAddress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Adresse : ${order.payAddress}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent,
                        )
                    }
                    if (copiedNotice) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copié dans le presse-papier !",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isWalletFunded = order.gatewayMode == "WALLET"
            if (isWalletFunded) {
                Surface(
                    color = EmeraldContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Financé par votre portefeuille ${sendMethod.label}",
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Le montant a déjà été prélevé de votre solde ${sendMethod.label}. Aucun transfert supplémentaire n'est nécessaire.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (status == OrderStatus.PENDING && !isWalletFunded && sendMethod.supportsOnlineApi && config.isOnlineEnabledFor(sendMethod)) {
                val hasBackend = config.backendBaseUrl.trim().isNotBlank()
                Surface(
                    color = ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            ) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = SkyInfo, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${sendMethod.label} Online",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SkyInfo,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Surface(
                                color = if (config.gatewayMode == "LIVE") EmeraldContainer else GoldContainer,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = "${config.gatewayMode} • ${gatewayStatus.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (config.gatewayMode == "LIVE") EmeraldPrimary else GoldAccent,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!hasBackend) {
                            Text(
                                text = "Serveur API/Webhooks non configuré : l'admin doit renseigner l'URL HTTPS dans Admin > Passerelles API pour activer le guichet automatique.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        } else if (order.checkoutUrl.isBlank()) {
                            Text(
                                text = "Lancez une session sécurisée via le backend Alevini (authentification Firebase + vérification webhook automatique).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onInitiateOnlinePayment,
                                enabled = !isGatewayBusy && remainingMillis > 0L,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SkyInfo,
                                    contentColor = ObsidianBg,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("initiate_online_payment_button"),
                            ) {
                                if (isGatewayBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = ObsidianBg,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Payer en ligne (${sendMethod.label})",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else {
                            if (order.gatewayOrderId.isNotBlank()) {
                                Text(
                                    text = "ID Passerelle : ${order.gatewayOrderId}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldAccent,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(order.checkoutUrl))
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SkyInfo,
                                        contentColor = ObsidianBg,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .heightIn(min = 48.dp)
                                        .testTag("open_checkout_url_button"),
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ouvrir guichet",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                OutlinedButton(
                                    onClick = onVerifyOnlinePayment,
                                    enabled = !isGatewayBusy,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    border = BorderStroke(1.dp, EmeraldPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .heightIn(min = 48.dp)
                                        .testTag("verify_online_payment_button"),
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Vérifier", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Statut", maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (status == OrderStatus.PENDING && !isWalletFunded) {
                Text(
                    text = "Soumettre votre référence de transaction (ou preuve directe)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = txRefInput,
                    onValueChange = { txRefInput = it },
                    placeholder = { Text("Ex: MC-884422 ou ID Binance", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_ref_input"),
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                        .testTag("submit_tx_ref_button"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Soumettre la preuve de paiement",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                if (order.externalTxRef.isNotBlank()) {
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Référence soumise", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = order.externalTxRef,
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                }
                if (order.adminNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Note Admin", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(order.adminNote, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(0.46f).padding(end = 8.dp),
        )
        Text(
            text = value,
            fontFamily = if (highlight) JetBrainsMonoFamily else MaterialTheme.typography.bodyMedium.fontFamily,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (highlight) EmeraldPrimary else TextPrimary,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.54f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizontalBarSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    modifier: Modifier = Modifier,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        thumb = {
            Spacer(modifier = Modifier.size(0.dp))
        },
        track = { sliderState ->
            val span = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(1e-6f)
            val fraction = ((sliderState.value - sliderState.valueRange.start) / span).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, ObsidianOutline.copy(alpha = 0.6f), RoundedCornerShape(50)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(EmeraldPrimary),
                )
            }
        },
    )
}

fun formatNum(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", value)
    } else {
        String.format(Locale.US, "%,.2f", value)
    }
}

internal const val SCROLL_TO_TOP_INACTIVITY_MILLIS = 15_000L
internal const val SCROLL_TO_TOP_THRESHOLD_PX = 160

@Composable
fun BoxScope.ScrollToTopButton(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isVisible by remember(scrollState) { mutableStateOf(false) }

    LaunchedEffect(scrollState) {
        snapshotFlow {
            Triple(
                scrollState.value > SCROLL_TO_TOP_THRESHOLD_PX,
                scrollState.value,
                scrollState.isScrollInProgress,
            )
        }.collectLatest { (pastThreshold, _, isScrolling) ->
            if (!pastThreshold) {
                isVisible = false
            } else {
                isVisible = true
                if (!isScrolling) {
                    delay(SCROLL_TO_TOP_INACTIVITY_MILLIS)
                    isVisible = false
                }
            }
        }
    }

    ScrollToTopAnimatedPill(
        visible = isVisible,
        onClick = {
            scope.launch {
                scrollState.animateScrollTo(0)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun BoxScope.ScrollToTopButton(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isVisible by remember(listState) { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow {
            val pastThreshold = listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > SCROLL_TO_TOP_THRESHOLD_PX
            val signature = (listState.firstVisibleItemIndex * 100_000L) + listState.firstVisibleItemScrollOffset
            Triple(pastThreshold, signature, listState.isScrollInProgress)
        }.collectLatest { (pastThreshold, _, isScrolling) ->
            if (!pastThreshold) {
                isVisible = false
            } else {
                isVisible = true
                if (!isScrolling) {
                    delay(SCROLL_TO_TOP_INACTIVITY_MILLIS)
                    isVisible = false
                }
            }
        }
    }

    ScrollToTopAnimatedPill(
        visible = isVisible,
        onClick = {
            scope.launch {
                listState.animateScrollToItem(0)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun BoxScope.ScrollToTopAnimatedPill(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) +
            scaleIn(
                initialScale = 0.78f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) { it / 2 },
        exit = fadeOut(animationSpec = tween(260)) +
            scaleOut(targetScale = 0.82f, animationSpec = tween(260)) +
            slideOutVertically(animationSpec = tween(260)) { it / 2 },
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = EmeraldContainer.copy(alpha = 0.96f),
            contentColor = ForestGreen,
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.55f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(48.dp)
                .testTag("scroll_to_top_button"),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Remonter en haut",
                    tint = ForestGreen,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

