package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ExchangeConfig
import com.example.data.model.PaymentMethod
import com.example.data.model.StoreCatalog
import com.example.data.model.StoreCategory
import com.example.data.model.StoreConfig
import com.example.data.model.StoreOrder
import com.example.data.model.StoreOrderStatus
import com.example.data.model.StoreProduct
import com.example.data.model.UserProfile
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
import com.example.ui.theme.RubyContainer
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreScreen(
    storeConfig: StoreConfig,
    exchangeConfig: ExchangeConfig,
    userProfile: UserProfile?,
    catalogProducts: List<StoreProduct>,
    ordersState: UiState<List<StoreOrder>>,
    dismissedOrderIds: Set<String>,
    selectedProductId: String,
    selectedGateway: PaymentMethod,
    selectedOptionInput: String,
    targetAccountInput: String,
    isSubmitting: Boolean,
    isAdmin: Boolean,
    onSelectCategoryDefault: (StoreCategory) -> Unit,
    onSelectProduct: (StoreProduct) -> Unit,
    onSelectGateway: (PaymentMethod) -> Unit,
    onSelectedOptionChange: (String) -> Unit,
    onTargetAccountChange: (String) -> Unit,
    onPlaceOrder: () -> Unit,
    onNavigateToTopUp: (PaymentMethod) -> Unit,
    onOpenOrder: (StoreOrder) -> Unit,
    onDeleteOrHideOrder: (StoreOrder) -> Unit,
    onOpenAdminStoreControl: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var orderToConfirmDelete by remember { mutableStateOf<StoreOrder?>(null) }

    val categories = StoreCategory.entries
    val ordersTabIndex = categories.size

    val allUserOrders = remember(ordersState, dismissedOrderIds) {
        ((ordersState as? UiState.Success)?.data ?: emptyList())
            .filterNot { it.id in dismissedOrderIds }
    }

    orderToConfirmDelete?.let { target ->
        val isPendingOrExpired =
            target.status == StoreOrderStatus.PENDING.name || target.status == StoreOrderStatus.EXPIRED.name
        AlertDialog(
            onDismissRequest = { orderToConfirmDelete = null },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    if (isPendingOrExpired) "Supprimer ${target.orderCode} ?" else "Retirer ${target.orderCode} de la vue ?",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    if (isPendingOrExpired) {
                        "Cette commande Store en attente sera supprimée."
                    } else {
                        "Cette commande restera archivée côté Back-Office mais ne sera plus affichée ici."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteOrHideOrder(target)
                        orderToConfirmDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyError, contentColor = TextPrimary),
                ) {
                    Text(if (isPendingOrExpired) "Supprimer" else "Retirer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToConfirmDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
        )
    }

    val activeCategory: StoreCategory? = categories.getOrNull(selectedTabIndex)

    val selectedProduct = remember(selectedProductId, catalogProducts, activeCategory) {
        StoreCatalog.findById(selectedProductId, catalogProducts)
            ?.takeIf { activeCategory == null || it.category == activeCategory }
            ?: catalogProducts.firstOrNull { it.category == activeCategory }
            ?: catalogProducts.first()
    }
    val selectedPriceHtg = storeConfig.calculatePriceHtg(selectedProduct.usdSell)

    val listState = rememberLazyListState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isWide = maxWidth >= 600.dp

        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Alevini Digital Hub",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Surface(color = EmeraldContainer, shape = RoundedCornerShape(50)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Surface(color = EmeraldPrimary, shape = CircleShape, modifier = Modifier.size(6.dp)) {}
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (storeConfig.isAutoFulfillment()) "LIVE API" else "MODE MANUEL",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "1 USD = ${formatNum(storeConfig.effectiveRate())} HTG • Services digitaux vérifiés",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldAccent,
                            )
                        }

                        if (isAdmin) {
                            Surface(
                                color = GoldContainer,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                                onClick = onOpenAdminStoreControl,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("store_admin_shortcut"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Régie Admin",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 4.dp,
                    containerColor = ObsidianSurface,
                    contentColor = EmeraldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = EmeraldPrimary,
                        )
                    },
                ) {
                    categories.forEachIndexed { idx, cat ->
                        val selected = selectedTabIndex == idx
                        Tab(
                            selected = selected,
                            onClick = {
                                selectedTabIndex = idx
                                onSelectCategoryDefault(cat)
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(storeCategoryIcon(cat), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = cat.label,
                                        maxLines = 1,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) EmeraldPrimary else TextSecondary,
                                    )
                                }
                            },
                            modifier = Modifier.testTag("store_tab_${cat.id}"),
                        )
                    }

                    val ordersSelected = selectedTabIndex == ordersTabIndex
                    val ordersLabel = if (allUserOrders.isNotEmpty()) {
                        "Mes Commandes (${allUserOrders.size})"
                    } else {
                        "Mes Commandes"
                    }
                    Tab(
                        selected = ordersSelected,
                        onClick = { selectedTabIndex = ordersTabIndex },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = ordersLabel,
                                    maxLines = 1,
                                    fontWeight = if (ordersSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (ordersSelected) EmeraldPrimary else TextSecondary,
                                )
                            }
                        },
                        modifier = Modifier.testTag("store_tab_orders"),
                    )
                }
            }

            if (activeCategory != null) {
                val categoryProducts = catalogProducts.filter { it.category == activeCategory }

                if (isWide) {
                    items(categoryProducts.chunked(2)) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pair.forEach { prod ->
                                StoreProductCard(
                                    product = prod,
                                    priceHtg = storeConfig.calculatePriceHtg(prod.usdSell),
                                    selected = prod.id == selectedProduct.id,
                                    onClick = { onSelectProduct(prod) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(categoryProducts, key = { it.id }) { prod ->
                        StoreProductCard(
                            product = prod,
                            priceHtg = storeConfig.calculatePriceHtg(prod.usdSell),
                            selected = prod.id == selectedProduct.id,
                            onClick = { onSelectProduct(prod) },
                        )
                    }
                }

                // Checkout Dock Card
                item {
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("Article sélectionné", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Text(
                                        text = selectedProduct.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${formatNum(selectedPriceHtg)} HTG",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = "$${formatNum(selectedProduct.usdSell)} USD",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldAccent,
                                        maxLines = 1,
                                    )
                                }
                            }

                            if (selectedProduct.selectOptions.isNotEmpty()) {
                                StoreProductOptionSelector(
                                    label = selectedProduct.selectLabel,
                                    options = selectedProduct.selectOptions,
                                    selectedOption = selectedOptionInput.ifBlank { selectedProduct.selectOptions.first() },
                                    onSelectOption = onSelectedOptionChange,
                                )
                            }

                            if (selectedProduct.requiresTargetAccount || selectedProduct.targetAccountLabel.isNotBlank()) {
                                OutlinedTextField(
                                    value = targetAccountInput,
                                    onValueChange = onTargetAccountChange,
                                    label = { Text(selectedProduct.targetAccountLabel) },
                                    placeholder = { Text(selectedProduct.targetAccountHint, color = TextMuted) },
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
                                        .testTag("store_target_account_input"),
                                )
                            }

                            val selectedGatewayBal = userProfile?.balanceFor(selectedGateway) ?: 0.0
                            val selectedGwCurrency = selectedGateway.defaultCurrency
                            val requiredGatewayDebit = if (selectedGateway.supportsHtg) selectedPriceHtg else selectedProduct.usdSell
                            val hasEnoughStoreFunds = selectedGatewayBal + 1e-6 >= requiredGatewayDebit

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Passerelle de règlement sécurisée", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                StoreGatewaySelectorRow(
                                    storeConfig = storeConfig,
                                    exchangeConfig = exchangeConfig,
                                    userProfile = userProfile,
                                    selectedGateway = selectedGateway,
                                    onSelectGateway = onSelectGateway,
                                )
                            }

                            Surface(
                                color = if (hasEnoughStoreFunds) EmeraldContainer.copy(alpha = 0.35f) else RubyContainer.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (hasEnoughStoreFunds) EmeraldPrimary.copy(alpha = 0.45f) else RubyError.copy(alpha = 0.65f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Solde ${selectedGateway.label} :",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        )
                                        Text(
                                            text = "${formatNum(selectedGatewayBal)} ${selectedGwCurrency.code}",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (hasEnoughStoreFunds) EmeraldPrimary else RubyError,
                                            maxLines = 1,
                                        )
                                    }
                                    Text(
                                        text = "Frais passerelle : ${formatNum(exchangeConfig.feeFor(selectedGateway))}% • Coût article : ${formatNum(requiredGatewayDebit)} ${selectedGwCurrency.code}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                    if (!hasEnoughStoreFunds) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = RubyError, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Solde ${selectedGateway.label} insuffisant. Rechargez ce compte avant de commander.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextPrimary,
                                            )
                                        }
                                        Button(
                                            onClick = { onNavigateToTopUp(selectedGateway) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("store_recharge_gateway_btn"),
                                        ) {
                                            Text(
                                                text = "Recharger ${selectedGateway.label}",
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                textAlign = TextAlign.Center,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "Débit automatique du portefeuille ${selectedGateway.label} • ${if (storeConfig.isAutoFulfillment()) "Mode API Instantané" else "Validation Admin"}.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                )
                            }

                            Button(
                                onClick = onPlaceOrder,
                                enabled = !isSubmitting && hasEnoughStoreFunds && userProfile?.isBlocked != true,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = ObsidianBg,
                                    disabledContainerColor = ObsidianSurface,
                                    disabledContentColor = TextMuted,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("store_checkout_button"),
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ObsidianBg)
                                } else {
                                    Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (hasEnoughStoreFunds) {
                                            "Commander • ${formatNum(requiredGatewayDebit)} ${selectedGwCurrency.code}"
                                        } else {
                                            "Solde ${selectedGateway.label} insuffisant"
                                        },
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
            } else {
                // Vault Tab ("Mes Commandes")
                when (ordersState) {
                    is UiState.Loading -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        }
                    }
                    is UiState.Error -> item {
                        Text(ordersState.message, color = RubyError)
                    }
                    is UiState.Success -> {
                        if (allUserOrders.isEmpty()) {
                            item {
                                Surface(
                                    color = ObsidianSurface,
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, ObsidianOutline),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                        Text("Votre coffre-fort Store est vide", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(
                                            text = "Commandez vos diamants, abonnements Streaming/IA, eSIM, Cartes Meru VCC ou factures Starlink pour retrouver vos accès ici.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                }
                            }
                        } else {
                            items(allUserOrders, key = { it.id }) { order ->
                                StoreOrderVaultCard(
                                    order = order,
                                    onOpenDetail = { onOpenOrder(order) },
                                    onCopyDeliverable = { deliverable ->
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Code Alevini Store", deliverable))
                                        onShowMessage("Code / accès copié dans le presse-papiers !")
                                    },
                                    onDeleteOrHide = { orderToConfirmDelete = order },
                                )
                            }
                        }
                    }
                }
            }
        }
        ScrollToTopButton(listState = listState)
    }
}
