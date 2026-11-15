package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CurrencyBitcoin
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ExchangeOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
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
import com.example.ui.theme.SkyContainer
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

fun methodIcon(method: PaymentMethod): ImageVector = when (method) {
    PaymentMethod.MONCASH, PaymentMethod.NATCASH -> Icons.Rounded.PhoneAndroid
    PaymentMethod.BINANCE, PaymentMethod.MERU -> Icons.Rounded.AccountBalanceWallet
    PaymentMethod.NOWPAYMENTS -> Icons.Rounded.CurrencyBitcoin
}

fun methodTint(method: PaymentMethod): Color = when (method) {
    PaymentMethod.MONCASH -> Color(0xFFFF4B4B)
    PaymentMethod.NATCASH -> Color(0xFF38BDF8)
    PaymentMethod.BINANCE -> Color(0xFFF4B942)
    PaymentMethod.MERU -> Color(0xFF9E86FF)
    PaymentMethod.NOWPAYMENTS -> EmeraldPrimary
}

@Composable
fun OrderStatusBadge(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING -> GoldContainer to GoldAccent
        OrderStatus.SUBMITTED, OrderStatus.PAID_IN -> SkyContainer to SkyInfo
        OrderStatus.COMPLETED -> EmeraldContainer to EmeraldPrimary
        OrderStatus.UNDERPAID, OrderStatus.REJECTED, OrderStatus.EXPIRED -> RubyContainer to RubyError
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f)),
        modifier = modifier,
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
            Icon(
                imageVector = methodIcon(method),
                contentDescription = null,
                tint = methodTint(method),
                modifier = Modifier.size(18.dp),
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
    onDismiss: () -> Unit,
    onSubmitTxRef: (String) -> Unit,
    onMarkExpired: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var txRefInput by remember(order.id) { mutableStateOf(order.externalTxRef) }
    var copiedNotice by remember { mutableStateOf(false) }

    val status = OrderStatus.fromString(order.status)
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
                Column {
                    Text(
                        text = order.orderCode,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        color = EmeraldPrimary,
                    )
                    Text(
                        text = "${PaymentMethod.fromId(order.sendMethod).label} → ${PaymentMethod.fromId(order.receiveMethod).label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OrderStatusBadge(status = status)
                    IconButton(onClick = onDismiss) {
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

            if (status == OrderStatus.PENDING) {
                Text(
                    text = "Soumettre votre référence de transaction",
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = ObsidianBg,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_tx_ref_button"),
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Soumettre la preuve de paiement", fontWeight = FontWeight.Bold)
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
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value,
            fontFamily = if (highlight) JetBrainsMonoFamily else MaterialTheme.typography.bodyMedium.fontFamily,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (highlight) EmeraldPrimary else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun formatNum(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", value)
    } else {
        String.format(Locale.US, "%,.2f", value)
    }
}
