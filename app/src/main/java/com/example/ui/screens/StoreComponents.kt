package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExchangeConfig
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreCategory
import com.example.data.model.StoreConfig
import com.example.data.model.StoreOrder
import com.example.data.model.StoreOrderStatus
import com.example.data.model.StoreProduct
import com.example.data.model.UserProfile
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

@Composable
fun StoreKpiBentoGrid(
    storeConfig: StoreConfig,
    liveStarlinkPending: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StoreKpiCard(
                label = "API Reloadly USD",
                value = "$${formatNum(storeConfig.reloadlyBalanceUsd)}",
                statusText = if (storeConfig.reloadlyActive) {
                    "Connecté (${storeConfig.reloadlyLatencyMs}ms)"
                } else {
                    "En pause"
                },
                accentColor = if (storeConfig.reloadlyActive) EmeraldPrimary else GoldAccent,
                icon = Icons.Rounded.Bolt,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            StoreKpiCard(
                label = "Carte Meru USD",
                value = "$${formatNum(storeConfig.meruBalanceUsd)}",
                statusText = "Starlink & Top-Up",
                accentColor = GoldAccent,
                icon = Icons.Rounded.CreditCard,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StoreKpiCard(
                label = "Profils Netflix 4K",
                value = "${storeConfig.availableNetflixCount} Dispo",
                statusText = if (storeConfig.availableNetflixCount > 0) "Livraison instant" else "Sur demande",
                accentColor = if (storeConfig.availableNetflixCount > 0) EmeraldPrimary else RubyError,
                icon = Icons.Rounded.Movie,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            StoreKpiCard(
                label = "File Starlink",
                value = "$liveStarlinkPending En attente",
                statusText = "Traitement via Meru",
                accentColor = SkyInfo,
                icon = Icons.Rounded.SatelliteAlt,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun StoreKpiCard(
    label: String,
    value: String,
    statusText: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

fun storeCategoryIcon(category: StoreCategory): ImageVector = when (category) {
    StoreCategory.GAMING -> Icons.Rounded.SportsEsports
    StoreCategory.STREAMING -> Icons.Rounded.Movie
    StoreCategory.TELECOM -> Icons.Rounded.SatelliteAlt
    StoreCategory.CARDS -> Icons.Rounded.CreditCard
    StoreCategory.AI_PRO -> Icons.Rounded.AutoAwesome
    StoreCategory.WEB_TRAVEL -> Icons.Rounded.Language
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreProductOptionSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelectOption: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val currentText = selectedOption.ifBlank { options.first() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = currentText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label.ifBlank { "Formule souhaitée" }) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = ObsidianOutline,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag("store_option_selector"),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = ObsidianSurface,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == currentText) EmeraldPrimary else TextPrimary,
                            fontWeight = if (option == currentText) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelectOption(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun StoreProductCard(
    product: StoreProduct,
    priceHtg: Double,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryIcon = storeCategoryIcon(product.category)
    Surface(
        color = if (selected) EmeraldContainer.copy(alpha = 0.55f) else ObsidianSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) EmeraldPrimary else ObsidianOutline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("store_product_${product.id}"),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(categoryIcon, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = if (selected) EmeraldPrimary else SkyContainer,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = if (selected) "SÉLECTIONNÉ" else product.deliveryType.shortBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) ObsidianBg else SkyInfo,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = product.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = product.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatNum(priceHtg)} HTG",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldPrimary,
                )
                Text(
                    text = "≈ $${formatNum(product.usdSell)} USD",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldAccent,
                )
            }
        }
    }
}

@Composable
fun StoreGatewaySelectorRow(
    storeConfig: StoreConfig,
    exchangeConfig: ExchangeConfig,
    userProfile: UserProfile?,
    selectedGateway: PaymentMethod,
    onSelectGateway: (PaymentMethod) -> Unit,
) {
    val gateways = listOf(
        PaymentMethod.MONCASH,
        PaymentMethod.NATCASH,
        PaymentMethod.BINANCE,
        PaymentMethod.NOWPAYMENTS,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        gateways.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { method ->
                    val enabled = storeConfig.isGatewayEnabled(method)
                    val selected = selectedGateway == method
                    val bal = userProfile?.balanceFor(method) ?: 0.0
                    val feePct = exchangeConfig.feeFor(method)
                    FilterChip(
                        selected = selected,
                        enabled = enabled,
                        onClick = { if (enabled) onSelectGateway(method) },
                        leadingIcon = if (!enabled) {
                            { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(15.dp)) }
                        } else if (selected) {
                            { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp)) }
                        } else null,
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                GatewayLogo(
                                    method = method,
                                    size = 20.dp,
                                    cornerRadius = 6.dp,
                                )
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = method.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 14.sp,
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${formatNum(bal)} ${method.defaultCurrency.code} • ${formatNum(feePct)}%",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (bal > 0.0) EmeraldPrimary else TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = EmeraldPrimary,
                            selectedLeadingIconColor = EmeraldPrimary,
                            containerColor = ObsidianSurfaceElevated,
                            labelColor = TextSecondary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .testTag("store_gw_${method.id}"),
                    )
                }
            }
        }
    }
}

@Composable
fun StoreOrderVaultCard(
    order: StoreOrder,
    onOpenDetail: () -> Unit,
    onCopyDeliverable: (String) -> Unit,
    onDeleteOrHide: () -> Unit,
) {
    val status = StoreOrderStatus.fromString(order.status)
    val (badgeBg, badgeColor) = when (status) {
        StoreOrderStatus.DELIVERED -> EmeraldContainer to EmeraldPrimary
        StoreOrderStatus.PAID -> SkyContainer to SkyInfo
        StoreOrderStatus.SUBMITTED -> GoldContainer to GoldAccent
        StoreOrderStatus.REJECTED, StoreOrderStatus.EXPIRED -> RubyContainer to RubyError
        StoreOrderStatus.PENDING -> ObsidianSurfaceElevated to TextSecondary
    }

    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (status == StoreOrderStatus.DELIVERED) EmeraldPrimary.copy(alpha = 0.6f) else ObsidianOutline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .testTag("store_vault_card_${order.orderCode}"),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = order.orderCode,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = order.productTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    IconButton(
                        onClick = onDeleteOrHide,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("store_delete_${order.orderCode}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Supprimer ou masquer la commande",
                            tint = RubyError,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${formatNum(order.amountHtg)} HTG ($${formatNum(order.usdSell)} USD) • ${PaymentMethod.fromId(order.gateway).label}",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            if (order.targetAccount.isNotBlank()) {
                Text(
                    text = "Cible : ${order.targetAccount}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent,
                )
            }

            if (order.deliverable.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = EmeraldContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = order.deliverable,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onCopyDeliverable(order.deliverable) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copier code ou accès", tint = EmeraldPrimary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreOrderDetailSheet(
    order: StoreOrder,
    exchangeConfig: ExchangeConfig,
    isGatewayBusy: Boolean,
    onDismiss: () -> Unit,
    onInitiateOnlinePayment: () -> Unit,
    onCheckOnlineStatus: () -> Unit,
    onSubmitTxRef: (String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val status = StoreOrderStatus.fromString(order.status)
    val gateway = PaymentMethod.fromId(order.gateway)
    var txRefInput by remember(order.id) { mutableStateOf("") }

    fun copy(label: String, text: String) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText(label, text))
        onShowMessage("$label copié !")
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
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Coffre Store • ${order.orderCode}",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldPrimary,
                    )
                    Text(
                        text = order.productTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "À régler : ${formatNum(order.amountHtg)} HTG ($${formatNum(order.usdSell)} USD)",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                    )
                    Text(
                        text = "Passerelle choisie : ${gateway.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    if (order.targetAccount.isNotBlank()) {
                        Text(
                            text = "Compte cible / UID / KIT : ${order.targetAccount}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyInfo,
                        )
                    }
                    Text(
                        text = "Statut : ${status.label}",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary,
                    )
                }
            }

            if (order.deliverable.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = EmeraldContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Accès / Code PIN livré",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = order.deliverable,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { copy("Livraison Store", order.deliverable) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Copier mon code / accès",
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            if (order.gatewayStatus == "WALLET_DEBITED" && status != StoreOrderStatus.DELIVERED && status != StoreOrderStatus.REJECTED) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = EmeraldContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Débité de votre portefeuille ${gateway.label}",
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Votre paiement a été prélevé directement sur votre solde ${gateway.label}. Traitement et livraison Admin en cours.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            } else if (status == StoreOrderStatus.PENDING) {
                Spacer(modifier = Modifier.height(14.dp))
                val merchantAccount = when (gateway) {
                    PaymentMethod.MONCASH -> exchangeConfig.moncashMerchant
                    PaymentMethod.NATCASH -> exchangeConfig.natcashMerchant
                    PaymentMethod.BINANCE -> exchangeConfig.binancePayId
                    PaymentMethod.NOWPAYMENTS -> "Checkout Crypto NOWPayments"
                    PaymentMethod.MERU -> exchangeConfig.meruTag
                }
                Surface(
                    color = ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Compte officiel ${gateway.label}", style = MaterialTheme.typography.labelMedium, color = GoldAccent)
                            Text(merchantAccount, fontFamily = JetBrainsMonoFamily, color = TextPrimary)
                        }
                        IconButton(
                            onClick = { copy(gateway.label, merchantAccount) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copier compte officiel", tint = EmeraldPrimary)
                        }
                    }
                }

                if (exchangeConfig.isOnlineEnabledFor(gateway) && exchangeConfig.backendBaseUrl.startsWith("https://")) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onInitiateOnlinePayment,
                        enabled = !isGatewayBusy,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyInfo, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        if (isGatewayBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianBg)
                        } else {
                            Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Payer en ligne (${gateway.label})",
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (order.checkoutUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(order.checkoutUrl)))
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 48.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ouvrir paiement",
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            OutlinedButton(
                                onClick = onCheckOnlineStatus,
                                enabled = !isGatewayBusy,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = "Vérifier statut",
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ObsidianOutline)
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Soumettre l'ID de transaction (sans jamais saisir votre PIN)",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = txRefInput,
                    onValueChange = { txRefInput = it.take(120) },
                    label = { Text("Référence / ID de transaction") },
                    placeholder = { Text("Ex: MC-884422 ou TX Hash", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("store_sheet_tx_ref_input"),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onSubmitTxRef(txRefInput) },
                    enabled = txRefInput.trim().length >= 4,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("store_sheet_submit_ref"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Valider ma preuve de transfert",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (order.logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Journal de suivi",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                        )
                        order.logs.forEach { entry ->
                            Text(
                                text = "• $entry",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
