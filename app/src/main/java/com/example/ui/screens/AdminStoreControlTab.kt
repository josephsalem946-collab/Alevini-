package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetflixInventoryItem
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreCategory
import com.example.data.model.StoreConfig
import com.example.data.model.StoreDeliveryType
import com.example.data.model.StoreFulfillmentMode
import com.example.data.model.StoreOrder
import com.example.data.model.StoreOrderStatus
import com.example.data.model.StoreProduct
import com.example.data.model.StoreProviderHealth
import com.example.ui.components.HorizontalBarSlider
import com.example.ui.components.ScrollToTopButton
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
import com.example.ui.theme.RubyError
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

private enum class AdminOrderFilter(val label: String) {
    ALL("Toutes"),
    TO_VALIDATE("À valider"),
    IN_PROGRESS("En cours"),
    DELIVERED("Livrées"),
    REJECTED("Refusées"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStoreControlTab(
    storeConfig: StoreConfig,
    catalogProducts: List<StoreProduct>,
    storeOrdersState: UiState<List<StoreOrder>>,
    netflixInventoryState: UiState<List<NetflixInventoryItem>>,
    providersHealth: List<StoreProviderHealth>,
    isGatewayBusy: Boolean,
    onSaveStoreConfig: (StoreConfig) -> Unit,
    onSetFulfillmentMode: (StoreFulfillmentMode) -> Unit,
    onTopUpMeruUsd: () -> Unit,
    onTransferMeruToReloadly: () -> Unit,
    onTestAllApis: () -> Unit,
    onFulfillStarlinkViaMeru: (StoreOrder, String) -> Unit,
    onAssignNetflixAndDeliver: (StoreOrder) -> Unit,
    onDeliverManualCode: (StoreOrder, String, String) -> Unit,
    onRetryStoreOrderApi: (StoreOrder) -> Unit,
    onRejectStoreOrder: (StoreOrder, String) -> Unit,
    onDeleteStoreOrder: (String) -> Unit,
    onAddStoreProduct: (
        category: StoreCategory,
        deliveryType: StoreDeliveryType,
        badge: String,
        title: String,
        subtitle: String,
        usdCost: Double,
        usdSell: Double,
        targetAccountLabel: String,
        onSuccess: () -> Unit,
    ) -> Unit,
    onDeleteStoreProduct: (String) -> Unit,
    onAddNetflixProfile: (email: String, password: String, profileName: String, pin: String, onSuccess: () -> Unit) -> Unit,
    onDeleteNetflixProfile: (NetflixInventoryItem) -> Unit,
) {
    var baseRateText by remember(storeConfig.baseRateHtg) { mutableStateOf(storeConfig.baseRateHtg.toString()) }
    var markupSlider by remember(storeConfig.markupPercent) { mutableFloatStateOf(storeConfig.markupPercent.toFloat()) }
    var reloadlyActive by remember(storeConfig.reloadlyActive) { mutableStateOf(storeConfig.reloadlyActive) }
    var ezpinActive by remember(storeConfig.ezpinActive) { mutableStateOf(storeConfig.ezpinActive) }
    var moncashActive by remember(storeConfig.moncashActive) { mutableStateOf(storeConfig.moncashActive) }
    var natcashActive by remember(storeConfig.natcashActive) { mutableStateOf(storeConfig.natcashActive) }
    var binanceActive by remember(storeConfig.binanceActive) { mutableStateOf(storeConfig.binanceActive) }
    var nowpaymentsActive by remember(storeConfig.nowpaymentsActive) { mutableStateOf(storeConfig.nowpaymentsActive) }

    LaunchedEffect(storeConfig) {
        baseRateText = storeConfig.baseRateHtg.toString()
        markupSlider = storeConfig.markupPercent.toFloat()
        reloadlyActive = storeConfig.reloadlyActive
        ezpinActive = storeConfig.ezpinActive
        moncashActive = storeConfig.moncashActive
        natcashActive = storeConfig.natcashActive
        binanceActive = storeConfig.binanceActive
        nowpaymentsActive = storeConfig.nowpaymentsActive
    }

    var selectedOrderFilter by remember { mutableStateOf(AdminOrderFilter.ALL) }

    // Custom catalog product builder state
    var prodCategory by remember { mutableStateOf(StoreCategory.GAMING) }
    var prodCategoryExpanded by remember { mutableStateOf(false) }
    var prodDeliveryType by remember { mutableStateOf(StoreDeliveryType.STARLINK) }
    var prodDeliveryExpanded by remember { mutableStateOf(false) }
    var prodBadge by remember { mutableStateOf("") }
    var prodTitle by remember { mutableStateOf("") }
    var prodSubtitle by remember { mutableStateOf("") }
    var prodCostText by remember { mutableStateOf("") }
    var prodSellText by remember { mutableStateOf("") }
    var prodTargetLabel by remember { mutableStateOf("Email ou Identifiant de livraison") }

    // Netflix vault state
    var nfEmail by remember { mutableStateOf("") }
    var nfPass by remember { mutableStateOf("") }
    var nfProfile by remember { mutableStateOf("Écran 1") }
    var nfPin by remember { mutableStateOf("1001") }

    var deliverModalOrder by remember { mutableStateOf<StoreOrder?>(null) }
    var deliverPayloadText by remember { mutableStateOf("") }
    var deliverNoteText by remember { mutableStateOf("") }

    deliverModalOrder?.let { order ->
        val isStarlink = order.deliveryType == StoreDeliveryType.STARLINK.id
        AlertDialog(
            onDismissRequest = { deliverModalOrder = null },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = if (isStarlink) "Régler via Meru ${order.orderCode}" else "Livrer ${order.orderCode}",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${order.productTitle} • Cible: ${order.targetAccount.ifBlank { order.userEmail }}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = deliverPayloadText,
                        onValueChange = { deliverPayloadText = it },
                        label = {
                            Text(if (isStarlink) "N° Reçu Starlink / Meru (auto si vide)" else "Code PIN / Réf Reloadly livrée")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!isStarlink) {
                        OutlinedTextField(
                            value = deliverNoteText,
                            onValueChange = { deliverNoteText = it },
                            label = { Text("Note Admin (optionnel)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isStarlink) {
                            onFulfillStarlinkViaMeru(order, deliverPayloadText)
                        } else {
                            onDeliverManualCode(order, deliverPayloadText, deliverNoteText)
                        }
                        deliverModalOrder = null
                        deliverPayloadText = ""
                        deliverNoteText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                ) {
                    Text("Valider & Livrer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deliverModalOrder = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
        )
    }

    val previewBase = baseRateText.toDoubleOrNull() ?: storeConfig.baseRateHtg
    val previewEffRate = previewBase * (1.0 + markupSlider / 100.0)

    val allStoreOrders = (storeOrdersState as? UiState.Success)?.data.orEmpty()
    val filteredStoreOrders = remember(allStoreOrders, selectedOrderFilter) {
        when (selectedOrderFilter) {
            AdminOrderFilter.ALL -> allStoreOrders
            AdminOrderFilter.TO_VALIDATE -> allStoreOrders.filter {
                it.status == StoreOrderStatus.PENDING.name || it.status == StoreOrderStatus.SUBMITTED.name
            }
            AdminOrderFilter.IN_PROGRESS -> allStoreOrders.filter {
                it.status == StoreOrderStatus.PAID.name
            }
            AdminOrderFilter.DELIVERED -> allStoreOrders.filter {
                it.status == StoreOrderStatus.DELIVERED.name
            }
            AdminOrderFilter.REJECTED -> allStoreOrders.filter {
                it.status == StoreOrderStatus.REJECTED.name || it.status == StoreOrderStatus.EXPIRED.name
            }
        }
    }
    val liveStarlinkPending = remember(allStoreOrders, storeConfig.pendingStarlinkCount) {
        maxOf(
            storeConfig.pendingStarlinkCount,
            allStoreOrders.count {
                it.deliveryType == StoreDeliveryType.STARLINK.id &&
                    it.status in listOf(
                        StoreOrderStatus.PENDING.name,
                        StoreOrderStatus.SUBMITTED.name,
                        StoreOrderStatus.PAID.name,
                    )
            },
        )
    }
    val customProducts = remember(catalogProducts) {
        catalogProducts.filter { it.isCustom }
    }

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        // 0. Indicateurs et Soldes (exclusivement dans Admin)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Indicateurs & Soldes Store",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                )
                StoreKpiBentoGrid(
                    storeConfig = storeConfig,
                    liveStarlinkPending = liveStarlinkPending,
                )
            }
        }

        // 0b. Mode de Traitement Global (Auto vs Manuel)
        item {
            val currentMode = StoreFulfillmentMode.fromId(storeConfig.fulfillmentMode)
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = SkyInfo)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mode de Traitement des Ordres", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Text(
                        text = if (currentMode == StoreFulfillmentMode.AUTO) {
                            "Actif : API Reloadly & Stock Auto livrent instantanément les commandes éligibles."
                        } else {
                            "Actif : Toutes les nouvelles commandes attendent votre validation manuelle en Régie."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val isAuto = currentMode == StoreFulfillmentMode.AUTO
                        Surface(
                            color = if (isAuto) EmeraldContainer else ObsidianSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isAuto) 1.5.dp else 1.dp,
                                color = if (isAuto) EmeraldPrimary else ObsidianOutline,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 56.dp)
                                .clickable { onSetFulfillmentMode(StoreFulfillmentMode.AUTO) }
                                .testTag("admin_mode_auto"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Bolt,
                                    contentDescription = "Mode Automatique",
                                    tint = if (isAuto) EmeraldPrimary else TextMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mode Automatique",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isAuto) EmeraldPrimary else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        val isManual = currentMode == StoreFulfillmentMode.MANUAL
                        Surface(
                            color = if (isManual) GoldContainer else ObsidianSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isManual) 1.5.dp else 1.dp,
                                color = if (isManual) GoldAccent else ObsidianOutline,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 56.dp)
                                .clickable { onSetFulfillmentMode(StoreFulfillmentMode.MANUAL) }
                                .testTag("admin_mode_manual"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Shield,
                                    contentDescription = "Mode Manuel",
                                    tint = if (isManual) GoldAccent else TextMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mode Manuel",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isManual) GoldAccent else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Moteur de Prix HTG/USD
        item {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Moteur de Prix & Marge HTG/USD", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = baseRateText,
                            onValueChange = { s -> baseRateText = s.filter { it.isDigit() || it == '.' } },
                            label = { Text("Taux Base HTG") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianOutline,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Surface(color = GoldContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Taux Client Final", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                                Text(
                                    text = "${formatNum(previewEffRate)} HTG",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }

                    Text(
                        text = "Marge commerciale : +${markupSlider.roundToInt()}%",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldAccent,
                    )
                    HorizontalBarSlider(
                        value = markupSlider,
                        onValueChange = { markupSlider = it.roundToInt().toFloat() },
                        valueRange = 0f..20f,
                        steps = 19,
                        modifier = Modifier.testTag("admin_store_markup_bar"),
                    )

                    Button(
                        onClick = {
                            onSaveStoreConfig(
                                storeConfig.copy(
                                    baseRateHtg = baseRateText.toDoubleOrNull() ?: storeConfig.baseRateHtg,
                                    markupPercent = markupSlider.roundToInt().toDouble(),
                                    reloadlyActive = reloadlyActive,
                                    ezpinActive = ezpinActive,
                                    moncashActive = moncashActive,
                                    natcashActive = natcashActive,
                                    binanceActive = binanceActive,
                                    nowpaymentsActive = nowpaymentsActive,
                                )
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("admin_store_save_config"),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enregistrer Tarifs & Passerelles Store",
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // 2. Carte Virtuelle Meru & Reloadly Balance
        item {
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CreditCard, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trésorerie Carte Meru & Reloadly", style = MaterialTheme.typography.titleMedium, color = GoldAccent)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = ObsidianSurface, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Carte Virtuelle Meru", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "$${formatNum(storeConfig.meruBalanceUsd)} USD",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                )
                            }
                        }
                        Surface(color = ObsidianSurface, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Wallet Reloadly B2B", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "$${formatNum(storeConfig.reloadlyBalanceUsd)} USD",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onTopUpMeruUsd,
                            border = BorderStroke(1.dp, GoldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp),
                        ) {
                            Text(
                                text = "+100$ P2P",
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Button(
                            onClick = onTransferMeruToReloadly,
                            colors = ButtonDefaults.buttonColors(containerColor = SkyInfo, contentColor = ObsidianBg),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "50$ -> Reloadly",
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // 3. Interrupteurs Fournisseurs & Audit API
        item {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Contrôle Fournisseurs & Passerelles", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    StoreToggleRow("API Reloadly Top-Up", reloadlyActive) { reloadlyActive = it }
                    StoreToggleRow("API EZPin Digital Gift Cards", ezpinActive) { ezpinActive = it }
                    StoreToggleRow("MonCash Online (Store)", moncashActive) { moncashActive = it }
                    StoreToggleRow("NatCash Online (Store)", natcashActive) { natcashActive = it }
                    StoreToggleRow("Binance Pay OpenAPI", binanceActive) { binanceActive = it }
                    StoreToggleRow("NOWPayments Crypto", nowpaymentsActive) { nowpaymentsActive = it }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onTestAllApis,
                        enabled = !isGatewayBusy,
                        border = BorderStroke(1.dp, SkyInfo),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SkyInfo),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        if (isGatewayBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SkyInfo)
                        } else {
                            Icon(Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tester toutes les API Backend",
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    providersHealth.forEach { h ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${h.provider.uppercase()} (${h.mode})",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (h.configured) EmeraldPrimary else GoldAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                            )
                            Text(
                                text = if (h.configured) "CLÉ OK • ${h.latencyMs}ms" else "MODE SANDBOX • ${h.latencyMs}ms",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        // 4. Commandes Store & File Starlink avec Filtres + Logs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.SatelliteAlt, contentDescription = null, tint = SkyInfo)
                    Text("File des Ordres Store & Services", style = MaterialTheme.typography.titleMedium, color = SkyInfo)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AdminOrderFilter.entries.forEach { filter ->
                        val selected = selectedOrderFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { selectedOrderFilter = filter },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldPrimary,
                                containerColor = ObsidianSurface,
                                labelColor = TextSecondary,
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("admin_store_filter_${filter.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        when (storeOrdersState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = EmeraldPrimary) }
            is UiState.Error -> item { Text(storeOrdersState.message, color = RubyError) }
            is UiState.Success -> {
                if (filteredStoreOrders.isEmpty()) {
                    item { Text("Aucune commande Store dans ce filtre.", color = TextMuted) }
                } else {
                    items(filteredStoreOrders, key = { "adm_st_${it.id}" }) { order ->
                        val status = StoreOrderStatus.fromString(order.status)
                        Surface(
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(15.dp),
                            border = BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${order.orderCode} • ${order.productTitle}",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                            ),
                                            color = EmeraldPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = order.userEmail,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 10.5.sp,
                                                lineHeight = 14.sp,
                                            ),
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    IconButton(onClick = { onDeleteStoreOrder(order.id) }, modifier = Modifier.size(48.dp)) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Supprimer commande Store",
                                            tint = RubyError,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = "${formatNum(order.amountHtg)} HTG ($${formatNum(order.usdSell)} • Coût $${formatNum(order.usdCost)}) via ${PaymentMethod.fromId(order.gateway).label}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                    ),
                                    color = TextPrimary,
                                )
                                if (order.targetAccount.isNotBlank()) {
                                    Text(
                                        text = "Cible : ${order.targetAccount}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                        ),
                                        color = GoldAccent,
                                    )
                                }
                                if (order.externalTxRef.isNotBlank()) {
                                    Text(
                                        text = "Preuve : ${order.externalTxRef}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                        ),
                                        color = SkyInfo,
                                    )
                                }
                                Text(
                                    text = "Statut : ${status.label}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        lineHeight = 13.sp,
                                    ),
                                    color = TextSecondary,
                                )

                                if (order.logs.isNotEmpty()) {
                                    Surface(
                                        color = ObsidianBg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, ObsidianOutline),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Text(
                                                text = "Journal d'activité",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.5.sp,
                                                    lineHeight = 12.sp,
                                                ),
                                                color = GoldAccent,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            order.logs.takeLast(5).forEach { entry ->
                                                Text(
                                                    text = "• $entry",
                                                    fontFamily = JetBrainsMonoFamily,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.5.sp,
                                                        lineHeight = 12.sp,
                                                    ),
                                                    color = TextSecondary,
                                                )
                                            }
                                        }
                                    }
                                }

                                if (status != StoreOrderStatus.DELIVERED && status != StoreOrderStatus.REJECTED) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Max),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        when (order.deliveryType) {
                                            StoreDeliveryType.STARLINK.id -> {
                                                Button(
                                                    onClick = { deliverModalOrder = order },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = ObsidianBg),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .heightIn(min = 48.dp),
                                                ) {
                                                    Text(
                                                        text = "Payer via Meru ($${formatNum(order.usdCost)})",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2,
                                                        textAlign = TextAlign.Center,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                            StoreDeliveryType.NETFLIX_PROFILE.id -> {
                                                Button(
                                                    onClick = { onAssignNetflixAndDeliver(order) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .heightIn(min = 48.dp),
                                                ) {
                                                    Text(
                                                        text = "Attribuer profil dispo",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2,
                                                        textAlign = TextAlign.Center,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                            else -> {
                                                Button(
                                                    onClick = { deliverModalOrder = order },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .heightIn(min = 48.dp),
                                                ) {
                                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Livrer Code / Ref",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2,
                                                        textAlign = TextAlign.Center,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { onRejectStoreOrder(order, "Refusé par Back-Office") },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            border = BorderStroke(1.dp, RubyError),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RubyError),
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .heightIn(min = 48.dp),
                                        ) {
                                            Text(
                                                text = "Refuser",
                                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onRetryStoreOrderApi(order) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.65f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SkyInfo),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("admin_retry_api_${order.orderCode}"),
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Relancer API / Notification",
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Catalogue & Stock (Publier un Nouveau Produit)
        item {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Catalogue & Stock (Publier un Nouveau Produit)", style = MaterialTheme.typography.titleMedium, color = GoldAccent)
                    }

                    ExposedDropdownMenuBox(
                        expanded = prodCategoryExpanded,
                        onExpandedChange = { prodCategoryExpanded = !prodCategoryExpanded },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = prodCategory.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Catégorie du Store") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prodCategoryExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = prodCategoryExpanded,
                            onDismissRequest = { prodCategoryExpanded = false },
                            containerColor = ObsidianSurface,
                        ) {
                            StoreCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.label, color = TextPrimary) },
                                    onClick = {
                                        prodCategory = cat
                                        prodCategoryExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = prodDeliveryExpanded,
                        onExpandedChange = { prodDeliveryExpanded = !prodDeliveryExpanded },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = prodDeliveryType.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mode de livraison") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prodDeliveryExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = prodDeliveryExpanded,
                            onDismissRequest = { prodDeliveryExpanded = false },
                            containerColor = ObsidianSurface,
                        ) {
                            StoreDeliveryType.entries.forEach { dt ->
                                DropdownMenuItem(
                                    text = { Text(dt.label, color = TextPrimary) },
                                    onClick = {
                                        prodDeliveryType = dt
                                        prodDeliveryExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prodTitle,
                        onValueChange = { prodTitle = it.take(80) },
                        label = { Text("Nom du produit / service") },
                        placeholder = { Text("Ex: Carte Meru VCC $100 USD", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_new_product_title"),
                    )

                    OutlinedTextField(
                        value = prodSubtitle,
                        onValueChange = { prodSubtitle = it.take(140) },
                        label = { Text("Description courte") },
                        placeholder = { Text("Livraison rapide & sécurisée", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = prodBadge,
                            onValueChange = { prodBadge = it.take(28) },
                            label = { Text("Badge") },
                            placeholder = { Text("EX: PROMO VIP", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = prodCostText,
                            onValueChange = { s -> prodCostText = s.filter { it.isDigit() || it == '.' } },
                            label = { Text("Coût USD") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.75f),
                        )
                        OutlinedTextField(
                            value = prodSellText,
                            onValueChange = { s -> prodSellText = s.filter { it.isDigit() || it == '.' } },
                            label = { Text("Vente USD") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.75f),
                        )
                    }

                    OutlinedTextField(
                        value = prodTargetLabel,
                        onValueChange = { prodTargetLabel = it.take(80) },
                        label = { Text("Champ requis client") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val sellVal = prodSellText.toDoubleOrNull() ?: 0.0
                    val costVal = prodCostText.toDoubleOrNull() ?: sellVal
                    Button(
                        onClick = {
                            onAddStoreProduct(
                                prodCategory,
                                prodDeliveryType,
                                prodBadge,
                                prodTitle,
                                prodSubtitle,
                                costVal,
                                sellVal,
                                prodTargetLabel,
                            ) {
                                prodTitle = ""
                                prodSubtitle = ""
                                prodBadge = ""
                                prodCostText = ""
                                prodSellText = ""
                            }
                        },
                        enabled = prodTitle.trim().length >= 3 && sellVal > 0.0,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = ObsidianBg),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("admin_publish_store_product"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Publier immédiatement dans le Store",
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (customProducts.isNotEmpty()) {
            items(customProducts, key = { "adm_custom_prod_${it.id}" }) { prod ->
                Surface(
                    color = ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${prod.category.label} • ${prod.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${prod.badge} • Vente $${formatNum(prod.usdSell)} USD (${formatNum(storeConfig.calculatePriceHtg(prod.usdSell))} HTG)",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent,
                            )
                        }
                        IconButton(
                            onClick = { onDeleteStoreProduct(prod.id) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Supprimer produit Store", tint = RubyError)
                        }
                    }
                }
            }
        }

        // 6. Inventaire Profils Netflix (Stock Auto privé Admin)
        item {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Movie, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stock Coffre-Fort Netflix (Admin seul)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    OutlinedTextField(
                        value = nfEmail,
                        onValueChange = { nfEmail = it },
                        label = { Text("Email compte Netflix") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = nfPass,
                        onValueChange = { nfPass = it },
                        label = { Text("Mot de passe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nfProfile,
                            onValueChange = { nfProfile = it },
                            label = { Text("Nom profil") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = nfPin,
                            onValueChange = { nfPin = it.filter { ch -> ch.isDigit() }.take(8) },
                            label = { Text("Code PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Button(
                        onClick = {
                            onAddNetflixProfile(nfEmail, nfPass, nfProfile, nfPin) {
                                nfEmail = ""
                                nfPass = ""
                            }
                        },
                        enabled = nfEmail.trim().length >= 3 && nfPass.isNotBlank() && nfPin.length >= 4,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("admin_add_netflix_button"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ajouter un profil Netflix au stock",
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        when (netflixInventoryState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = EmeraldPrimary) }
            is UiState.Error -> item { Text(netflixInventoryState.message, color = RubyError) }
            is UiState.Success -> {
                items(netflixInventoryState.data, key = { "nf_${it.id}" }) { slot ->
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${slot.accountEmail} • ${slot.profileName} (PIN ${slot.profilePin})",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = if (slot.status == "AVAILABLE") "DISPONIBLE EN STOCK" else "ATTRIBUÉ (${slot.assignedOrderId.take(8)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (slot.status == "AVAILABLE") EmeraldPrimary else GoldAccent,
                                )
                            }
                            IconButton(onClick = { onDeleteNetflixProfile(slot) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Supprimer profil Netflix", tint = RubyError)
                            }
                        }
                    }
                }
            }
        }
    }
        ScrollToTopButton(listState = listState)
    }
}

@Composable
private fun StoreToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = EmeraldPrimary),
        )
    }
}
