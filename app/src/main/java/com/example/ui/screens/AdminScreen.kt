package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminNotification
import com.example.data.model.ExchangeConfig
import com.example.data.model.ExchangeOrder
import com.example.data.model.LedgerEntry
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.repository.AleviniRepository
import com.example.ui.components.OrderStatusBadge
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
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

private data class SecurityGroup(
    val idPrefix: String,
    val title: String,
    val items: List<String>,
)

private data class SecuritySection(
    val title: String,
    val subtitle: String,
    val groups: List<SecurityGroup>,
)

private val SECURITY_GUIDE_SECTIONS = listOf(
    SecuritySection(
        title = "1. Pandan w ap kode",
        subtitle = "Nivo kodaj ak lojik",
        groups = listOf(
            SecurityGroup(
                idPrefix = "0-0",
                title = "Jesyon sekrè ak kle",
                items = listOf(
                    "Mete kle sekrè ak sètifika deyò kòd sous la.",
                    "Sèvi ak varyab anviwònman (.env) pou enfòmasyon sansib.",
                    "Ajoute .env nan .gitignore pou evite pibliye l sou git pa erè.",
                ),
            ),
            SecurityGroup(
                idPrefix = "0-1",
                title = "Izolasyon ak kontwòl done",
                items = listOf(
                    "Pwoteje baz done a, pa janm kite l louvri san kontwòl.",
                    "Verifye sou sèvè ke chak itilizatè wè sèlman pwòp done l.",
                ),
            ),
            SecurityGroup(
                idPrefix = "0-2",
                title = "Otantifikasyon ak sesyon",
                items = listOf(
                    "Pwoteje paj admin yo ak restriksyon strik.",
                    "Asire sèvis koneksyon an idantifye itilizatè yo yon fason serye.",
                    "Limite tantativ koneksyon pou bloke atak fòs brit (brute-force).",
                    "Fè sesyon yo ekspire apre yon tan inaktivite.",
                ),
            ),
            SecurityGroup(
                idPrefix = "0-3",
                title = "Validasyon ak SQL",
                items = listOf(
                    "Verifye tout done itilizatè voye sou bò sèvè a.",
                    "Itilize rekèt SQL paramétré (prepared statements) kont SQL Injection.",
                    "Kontwole kalite ak gwosè fichye itilizatè telechaje.",
                ),
            ),
            SecurityGroup(
                idPrefix = "0-4",
                title = "Antèt ak CORS",
                items = listOf(
                    "Ajoute antèt sekirite nesesè nan repons HTTP yo.",
                    "Konfigure CORS strikteman: sèlman domèn pa w la.",
                ),
            ),
            SecurityGroup(
                idPrefix = "0-5",
                title = "Finans ak twazyèm pati",
                items = listOf(
                    "Kalkile pri ak montan sou sèvè a, pa sou frontend.",
                    "Valide siyati kriptografik webhook peman yo.",
                    "Mete yon plafon depans sou API entèlijans atifisyèl yo.",
                ),
            ),
            SecurityGroup(
                idPrefix = "0-6",
                title = "Jounal ak erè",
                items = listOf(
                    "Kache mesaj erè teknik detaye pou itilizatè final la.",
                    "Pa janm ekri done pèsonèl oswa sansib nan logs yo.",
                ),
            ),
            SecurityGroup(
                idPrefix = "0-7",
                title = "Fyabilite ak tès",
                items = listOf(
                    "Verifye chak paket ekstèn: èske li vrèman egziste e li sèten?",
                    "Separe baz done tès la de baz done pwodiksyon an.",
                ),
            ),
        ),
    ),
    SecuritySection(
        title = "2. Anvan lansman",
        subtitle = "Verifikasyon final",
        groups = listOf(
            SecurityGroup(
                idPrefix = "1-0",
                title = "Kontwòl final",
                items = listOf(
                    "Chache epi retire tout sekrè oswa modpas bliye nan kòd la.",
                    "Korije depandans ki gen vilnerabilite konnen.",
                    "Fè yon kontwòl konplè ak lis OWASP Top 10.",
                    "Eksplike chak fay jwenn an tèm senp pou korije l byen.",
                ),
            ),
        ),
    ),
    SecuritySection(
        title = "3. Nan mizanliy",
        subtitle = "Apre deplwaman",
        groups = listOf(
            SecurityGroup(
                idPrefix = "2-0",
                title = "Pwoteksyon pwodiksyon",
                items = listOf(
                    "Fòse HTTPS sou tout wout aplikasyon an.",
                    "Mete sove otomatik regilye baz done a an plas.",
                ),
            ),
        ),
    ),
)

@Composable
fun AdminScreen(
    config: ExchangeConfig,
    ordersState: UiState<List<ExchangeOrder>>,
    usersState: UiState<List<UserProfile>>,
    ledgerState: UiState<List<LedgerEntry>>,
    adminNotificationsState: UiState<List<AdminNotification>>,
    checkedSecurityItems: Set<String>,
    onSaveConfig: (ExchangeConfig) -> Unit,
    onCompleteOrder: (ExchangeOrder, String) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String) -> Unit,
    onDeleteOrder: (String) -> Unit,
    onToggleUserBlocked: (String, Boolean) -> Unit,
    onDeleteUser: (String) -> Unit,
    onDeleteLedgerEntry: (String) -> Unit,
    onSendNotification: (title: String, message: String, targetUserId: String, targetLabel: String, isAlert: Boolean, onSuccess: () -> Unit) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onToggleSecurityItem: (String) -> Unit,
    onResetSecurityChecklist: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Commandes", "Notifications", "Taux & Frais", "Clients & Caisse", "Gid Sekirite")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Back-Office Admin",
                        style = MaterialTheme.typography.headlineMedium,
                        color = GoldAccent,
                    )
                    Text(
                        text = "Contrôle Alevini • josephsalem946@gmail.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = ObsidianSurface,
                contentColor = EmeraldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = EmeraldPrimary,
                    )
                },
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                maxLines = 1,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) EmeraldPrimary else TextSecondary,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminOrdersTab(
                    ordersState = ordersState,
                    onCompleteOrder = onCompleteOrder,
                    onUpdateOrderStatus = onUpdateOrderStatus,
                    onDeleteOrder = onDeleteOrder,
                )
                1 -> AdminNotificationsTab(
                    usersState = usersState,
                    notificationsState = adminNotificationsState,
                    onSendNotification = onSendNotification,
                    onDeleteNotification = onDeleteNotification,
                )
                2 -> AdminRatesTab(
                    config = config,
                    onSaveConfig = onSaveConfig,
                )
                3 -> AdminUsersAndLedgerTab(
                    usersState = usersState,
                    ledgerState = ledgerState,
                    onToggleUserBlocked = onToggleUserBlocked,
                    onDeleteUser = onDeleteUser,
                    onDeleteLedgerEntry = onDeleteLedgerEntry,
                )
                4 -> AdminSecurityGuideTab(
                    checkedItems = checkedSecurityItems,
                    onToggleSecurityItem = onToggleSecurityItem,
                    onResetChecklist = onResetSecurityChecklist,
                )
            }
        }
    }
}

@Composable
private fun AdminConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RubyError, contentColor = TextPrimary),
            ) {
                Text("Supprimer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = TextSecondary)
            }
        },
    )
}

@Composable
private fun AdminOrdersTab(
    ordersState: UiState<List<ExchangeOrder>>,
    onCompleteOrder: (ExchangeOrder, String) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String) -> Unit,
    onDeleteOrder: (String) -> Unit,
) {
    var orderToDelete by remember { mutableStateOf<ExchangeOrder?>(null) }

    orderToDelete?.let { target ->
        AdminConfirmDeleteDialog(
            title = "Supprimer l'ordre ${target.orderCode} ?",
            message = "Cette action supprimera définitivement cet ordre du Back-Office.",
            onConfirm = {
                onDeleteOrder(target.id)
                orderToDelete = null
            },
            onDismiss = { orderToDelete = null },
        )
    }

    when (ordersState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldPrimary)
        }
        is UiState.Error -> Text(
            text = ordersState.message,
            color = RubyError,
            modifier = Modifier.padding(16.dp),
        )
        is UiState.Success -> {
            if (ordersState.data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune commande client pour le moment.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ordersState.data, key = { it.id }) { order ->
                        AdminOrderItemCard(
                            order = order,
                            onCompleteOrder = onCompleteOrder,
                            onUpdateOrderStatus = onUpdateOrderStatus,
                            onDeleteClick = { orderToDelete = order },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOrderItemCard(
    order: ExchangeOrder,
    onCompleteOrder: (ExchangeOrder, String) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String) -> Unit,
    onDeleteClick: () -> Unit,
) {
    val status = OrderStatus.fromString(order.status)
    var noteInput by remember(order.id, order.adminNote) { mutableStateOf(order.adminNote) }

    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.orderCode,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldPrimary,
                    )
                    Text(order.userEmail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OrderStatusBadge(status = status)
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_delete_order_${order.orderCode}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Supprimer l'ordre",
                            tint = RubyError,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${PaymentMethod.fromId(order.sendMethod).label} (${formatNum(order.sendAmount)} ${order.sendCurrency}) → ${PaymentMethod.fromId(order.receiveMethod).label} (${formatNum(order.receiveAmount)} ${order.receiveCurrency})",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Compte destination : ${order.destAccount}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            if (order.externalTxRef.isNotBlank()) {
                Text(
                    text = "Référence client : ${order.externalTxRef}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldAccent,
                )
            }

            if (status != OrderStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note Admin (optionnel)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onCompleteOrder(order, noteInput) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = ObsidianBg,
                        ),
                        modifier = Modifier.weight(1f).testTag("admin_complete_${order.orderCode}"),
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compléter", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onUpdateOrderStatus(order.id, OrderStatus.PAID_IN, noteInput) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SkyInfo),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SkyInfo),
                    ) {
                        Text("Reçu")
                    }

                    OutlinedButton(
                        onClick = { onUpdateOrderStatus(order.id, OrderStatus.REJECTED, noteInput) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RubyError),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RubyError),
                    ) {
                        Text("Refuser")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRatesTab(
    config: ExchangeConfig,
    onSaveConfig: (ExchangeConfig) -> Unit,
) {
    var buyRate by remember(config) { mutableStateOf(config.usdToHtgBuy.toString()) }
    var sellRate by remember(config) { mutableStateOf(config.usdToHtgSell.toString()) }
    var eurToUsd by remember(config) { mutableStateOf(config.eurToUsd.toString()) }
    var feeMoncash by remember(config) { mutableStateOf(config.feeMoncash.toString()) }
    var feeNatcash by remember(config) { mutableStateOf(config.feeNatcash.toString()) }
    var feeBinance by remember(config) { mutableStateOf(config.feeBinance.toString()) }
    var feeMeru by remember(config) { mutableStateOf(config.feeMeru.toString()) }
    var feeNowpayments by remember(config) { mutableStateOf(config.feeNowpayments.toString()) }
    var moncashMerchant by remember(config) { mutableStateOf(config.moncashMerchant) }
    var natcashMerchant by remember(config) { mutableStateOf(config.natcashMerchant) }
    var binancePayId by remember(config) { mutableStateOf(config.binancePayId) }
    var meruTag by remember(config) { mutableStateOf(config.meruTag) }

    LaunchedEffect(config) {
        buyRate = config.usdToHtgBuy.toString()
        sellRate = config.usdToHtgSell.toString()
        eurToUsd = config.eurToUsd.toString()
        feeMoncash = config.feeMoncash.toString()
        feeNatcash = config.feeNatcash.toString()
        feeBinance = config.feeBinance.toString()
        feeMeru = config.feeMeru.toString()
        feeNowpayments = config.feeNowpayments.toString()
        moncashMerchant = config.moncashMerchant
        natcashMerchant = config.natcashMerchant
        binancePayId = config.binancePayId
        meruTag = config.meruTag
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Taux de change (USD / HTG / EUR)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminDecimalField("Achat HTG", buyRate, { buyRate = it }, Modifier.weight(1f))
            AdminDecimalField("Vente HTG", sellRate, { sellRate = it }, Modifier.weight(1f))
            AdminDecimalField("EUR -> USD", eurToUsd, { eurToUsd = it }, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Frais par passerelle (%)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminDecimalField("MonCash %", feeMoncash, { feeMoncash = it }, Modifier.weight(1f))
            AdminDecimalField("NatCash %", feeNatcash, { feeNatcash = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminDecimalField("Binance %", feeBinance, { feeBinance = it }, Modifier.weight(1f))
            AdminDecimalField("Meru %", feeMeru, { feeMeru = it }, Modifier.weight(1f))
            AdminDecimalField("Crypto %", feeNowpayments, { feeNowpayments = it }, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Comptes Marchands Officiels", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        AdminTextField("Numéro Marchand MonCash", moncashMerchant, { moncashMerchant = it })
        AdminTextField("Numéro Marchand NatCash", natcashMerchant, { natcashMerchant = it })
        AdminTextField("Binance Pay ID / UID", binancePayId, { binancePayId = it })
        AdminTextField("Tag Meru", meruTag, { meruTag = it })

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onSaveConfig(
                    config.copy(
                        usdToHtgBuy = buyRate.toDoubleOrNull() ?: config.usdToHtgBuy,
                        usdToHtgSell = sellRate.toDoubleOrNull() ?: config.usdToHtgSell,
                        eurToUsd = eurToUsd.toDoubleOrNull() ?: config.eurToUsd,
                        feeMoncash = feeMoncash.toDoubleOrNull() ?: config.feeMoncash,
                        feeNatcash = feeNatcash.toDoubleOrNull() ?: config.feeNatcash,
                        feeBinance = feeBinance.toDoubleOrNull() ?: config.feeBinance,
                        feeMeru = feeMeru.toDoubleOrNull() ?: config.feeMeru,
                        feeNowpayments = feeNowpayments.toDoubleOrNull() ?: config.feeNowpayments,
                        moncashMerchant = moncashMerchant,
                        natcashMerchant = natcashMerchant,
                        binancePayId = binancePayId,
                        meruTag = meruTag,
                    )
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_save_rates_button"),
        ) {
            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publier les taux en direct", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AdminNotificationsTab(
    usersState: UiState<List<UserProfile>>,
    notificationsState: UiState<List<AdminNotification>>,
    onSendNotification: (String, String, String, String, Boolean, () -> Unit) -> Unit,
    onDeleteNotification: (String) -> Unit,
) {
    var titleInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var targetUserId by remember { mutableStateOf("ALL") }
    var targetLabel by remember { mutableStateOf("Tous les utilisateurs") }
    var isAlert by remember { mutableStateOf(false) }

    val usersList = (usersState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Envoyer une notification",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Destinataire", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = targetUserId == "ALL",
                            onClick = {
                                targetUserId = "ALL"
                                targetLabel = "Tous les utilisateurs"
                            },
                            label = { Text("Tous les utilisateurs") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldPrimary,
                            ),
                        )
                        usersList.forEach { user ->
                            FilterChip(
                                selected = targetUserId == user.userId,
                                onClick = {
                                    targetUserId = user.userId
                                    targetLabel = user.email.ifBlank { user.displayName }
                                },
                                label = { Text(user.email.ifBlank { user.displayName }, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldContainer,
                                    selectedLabelColor = EmeraldPrimary,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it.take(120) },
                        label = { Text("Titre de la notification") },
                        placeholder = { Text("Ex: Taux HTG mis à jour !", color = TextMuted) },
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
                            .testTag("admin_notif_title_input"),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it.take(500) },
                        label = { Text("Message") },
                        placeholder = { Text("Contenu affiché dans Notifications...", color = TextMuted) },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_notif_message_input"),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Marquer comme alerte urgente", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Text("Affiche un contour rouge chez le client", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Switch(
                            checked = isAlert,
                            onCheckedChange = { isAlert = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = RubyError,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onSendNotification(
                                titleInput,
                                messageInput,
                                targetUserId,
                                targetLabel,
                                isAlert,
                            ) {
                                titleInput = ""
                                messageInput = ""
                                isAlert = false
                            }
                        },
                        enabled = titleInput.trim().length >= 2 && messageInput.trim().length >= 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_send_notif_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Envoyer la notification", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Notifications envoyées",
                style = MaterialTheme.typography.titleMedium,
                color = GoldAccent,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        when (notificationsState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = EmeraldPrimary) }
            is UiState.Error -> item { Text(notificationsState.message, color = RubyError) }
            is UiState.Success -> {
                if (notificationsState.data.isEmpty()) {
                    item {
                        Text("Aucune notification envoyée pour le moment.", color = TextMuted)
                    }
                } else {
                    items(notificationsState.data, key = { "admin_notif_${it.id}" }) { notif ->
                        val dateLabel = remember(notif.createdAt) {
                            notif.createdAt?.toDate()?.let {
                                SimpleDateFormat("dd MMM HH:mm", Locale.FRENCH).format(it)
                            } ?: "À l'instant"
                        }
                        Surface(
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (notif.isAlert) RubyError.copy(alpha = 0.5f) else ObsidianOutline,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Surface(
                                            color = if (notif.targetUserId == "ALL") EmeraldContainer else GoldContainer,
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                text = notif.targetLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (notif.targetUserId == "ALL") EmeraldPrimary else GoldAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                        Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (notif.isAlert) RubyError else TextPrimary,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteNotification(notif.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("admin_delete_notif_${notif.id}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Supprimer cette notification",
                                        tint = RubyError,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUsersAndLedgerTab(
    usersState: UiState<List<UserProfile>>,
    ledgerState: UiState<List<LedgerEntry>>,
    onToggleUserBlocked: (String, Boolean) -> Unit,
    onDeleteUser: (String) -> Unit,
    onDeleteLedgerEntry: (String) -> Unit,
) {
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }
    var ledgerToDelete by remember { mutableStateOf<LedgerEntry?>(null) }

    userToDelete?.let { target ->
        AdminConfirmDeleteDialog(
            title = "Supprimer ce profil client ?",
            message = "Le profil de ${target.email.ifBlank { target.displayName }} sera supprimé.",
            onConfirm = {
                onDeleteUser(target.userId)
                userToDelete = null
            },
            onDismiss = { userToDelete = null },
        )
    }

    ledgerToDelete?.let { target ->
        AdminConfirmDeleteDialog(
            title = "Supprimer l'écriture ${target.orderCode} ?",
            message = "Cette écriture comptable de ${formatNum(target.amount)} ${target.currency} sera supprimée.",
            onConfirm = {
                onDeleteLedgerEntry(target.id)
                ledgerToDelete = null
            },
            onDismiss = { ledgerToDelete = null },
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Utilisateurs inscrits", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        when (usersState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = EmeraldPrimary) }
            is UiState.Error -> item { Text(usersState.message, color = RubyError) }
            is UiState.Success -> {
                items(usersState.data, key = { "user_${it.userId}" }) { user ->
                    val isOwnerAdmin = user.email.equals(AleviniRepository.ADMIN_EMAIL, ignoreCase = true)
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                if (user.phoneNumber.isNotBlank()) {
                                    Text(
                                        text = user.phoneNumber,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (user.isBlocked) Icons.Rounded.Block else Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = if (user.isBlocked) RubyError else EmeraldPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = user.isBlocked,
                                    enabled = !isOwnerAdmin,
                                    onCheckedChange = { onToggleUserBlocked(user.userId, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TextPrimary,
                                        checkedTrackColor = RubyError,
                                    ),
                                )
                                if (!isOwnerAdmin) {
                                    IconButton(
                                        onClick = { userToDelete = user },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("admin_delete_user_${user.userId}"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Supprimer le profil client",
                                            tint = RubyError,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ObsidianOutline)
            Text("Journal comptable (Caisse)", style = MaterialTheme.typography.titleMedium, color = GoldAccent)
        }

        when (ledgerState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = EmeraldPrimary) }
            is UiState.Error -> item { Text(ledgerState.message, color = RubyError) }
            is UiState.Success -> {
                if (ledgerState.data.isEmpty()) {
                    item {
                        Text("Aucune écriture de caisse enregistrée.", color = TextMuted)
                    }
                } else {
                    items(ledgerState.data, key = { "ledger_${it.id}" }) { entry ->
                        Surface(
                            color = ObsidianSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${entry.accountName} • ${entry.orderCode}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary,
                                    )
                                    Text(entry.note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (entry.entryType == "CREDIT") EmeraldContainer else RubyContainer,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            text = "${if (entry.entryType == "CREDIT") "+" else "-"}${formatNum(entry.amount)} ${entry.currency}",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (entry.entryType == "CREDIT") EmeraldPrimary else RubyError,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { ledgerToDelete = entry },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("admin_delete_ledger_${entry.id}"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Supprimer l'écriture comptable",
                                            tint = RubyError,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDecimalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { s -> onValueChange(s.filter { it.isDigit() || it == '.' }) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldPrimary,
            unfocusedBorderColor = ObsidianOutline,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        ),
        modifier = modifier,
    )
}

@Composable
private fun AdminTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldPrimary,
            unfocusedBorderColor = ObsidianOutline,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AdminSecurityGuideTab(
    checkedItems: Set<String>,
    onToggleSecurityItem: (String) -> Unit,
    onResetChecklist: () -> Unit,
) {
    var expandedGroup by remember { mutableStateOf("0-0") }
    var checklistExpanded by remember { mutableStateOf(true) }

    val totalItems = remember {
        SECURITY_GUIDE_SECTIONS.sumOf { sec -> sec.groups.sumOf { it.items.size } }
    }
    val completedCount = remember(checkedItems) {
        checkedItems.size.coerceAtMost(totalItems)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ObsidianOutline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { checklistExpanded = !checklistExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gid Devlopè pou Sekirize yon Aplikasyon",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = "$completedCount sou $totalItems pwen fini",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                            )
                        }
                    }
                    Icon(
                        imageVector = if (checklistExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Afficher le guide de sécurité",
                        tint = TextSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { if (totalItems > 0) completedCount.toFloat() / totalItems else 0f },
                    color = EmeraldPrimary,
                    trackColor = ObsidianSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )

                AnimatedVisibility(visible = checklistExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        SECURITY_GUIDE_SECTIONS.forEach { section ->
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = GoldAccent,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                            )
                            Text(
                                text = section.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            section.groups.forEach { group ->
                                val isOpen = expandedGroup == group.idPrefix
                                Surface(
                                    color = ObsidianSurfaceElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ObsidianOutline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .clickable {
                                                    expandedGroup = if (isOpen) "" else group.idPrefix
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = group.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = TextPrimary,
                                            )
                                            Icon(
                                                imageVector = if (isOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                            )
                                        }
                                        AnimatedVisibility(visible = isOpen) {
                                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                                HorizontalDivider(color = ObsidianOutline.copy(alpha = 0.5f))
                                                group.items.forEachIndexed { idx, txt ->
                                                    val itemId = "${group.idPrefix}-$idx"
                                                    val checked = itemId in checkedItems
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(min = 48.dp)
                                                            .clickable { onToggleSecurityItem(itemId) }
                                                            .padding(vertical = 6.dp),
                                                    ) {
                                                        Checkbox(
                                                            checked = checked,
                                                            onCheckedChange = { onToggleSecurityItem(itemId) },
                                                            colors = CheckboxDefaults.colors(
                                                                checkedColor = EmeraldPrimary,
                                                                checkmarkColor = ObsidianBg,
                                                            ),
                                                        )
                                                        Text(
                                                            text = txt,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (checked) TextMuted else TextPrimary,
                                                            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = onResetChecklist,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text("Efase tout koche yo", color = TextSecondary)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
