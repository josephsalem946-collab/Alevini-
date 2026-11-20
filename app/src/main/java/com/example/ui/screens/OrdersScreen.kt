package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.domain.OrderRetentionFilter
import com.example.data.model.ExchangeOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.ScrollToTopButton
import com.example.ui.components.formatNum
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RubyContainer
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

private enum class OrderFilter(val label: String) {
    ALL("Toutes"),
    PENDING("En attente"),
    SUBMITTED("Soumises"),
    COMPLETED("Complétées"),
}

@Composable
fun OrdersScreen(
    ordersState: UiState<List<ExchangeOrder>>,
    dismissedOrderIds: Set<String>,
    onOrderClick: (ExchangeOrder) -> Unit,
    onDeleteOrder: (ExchangeOrder) -> Unit,
    onDeleteCompletedOrders: (List<ExchangeOrder>) -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(OrderFilter.ALL) }
    var orderPendingDelete by remember { mutableStateOf<ExchangeOrder?>(null) }
    var showDeleteCompletedDialog by remember { mutableStateOf(false) }

    val visibleRecentOrders = remember(ordersState, dismissedOrderIds) {
        val all = (ordersState as? UiState.Success)?.data.orEmpty()
        OrderRetentionFilter.filterRecentOrders(all).filterNot { it.id in dismissedOrderIds }
    }
    val completedOrders = remember(visibleRecentOrders) {
        visibleRecentOrders.filter { it.status == OrderStatus.COMPLETED.name }
    }

    orderPendingDelete?.let { target ->
        val isRemovableDb = target.status == OrderStatus.PENDING.name || target.status == OrderStatus.EXPIRED.name
        AlertDialog(
            onDismissRequest = { orderPendingDelete = null },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Supprimer la transaction ?") },
            text = {
                Text(
                    if (isRemovableDb) {
                        "L'ordre ${target.orderCode} (non payé) sera définitivement annulé et supprimé."
                    } else {
                        "La transaction ${target.orderCode} sera retirée de votre vue historique personnelle tout en conservant le reçu d'audit sécurisé."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteOrder(target)
                        orderPendingDelete = null
                    },
                ) {
                    Text("Supprimer", color = RubyError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderPendingDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
        )
    }

    if (showDeleteCompletedDialog && completedOrders.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteCompletedDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Supprimer les ordres complétés ?") },
            text = {
                Text(
                    "${completedOrders.size} ordre(s) complété(s) seront retirés de votre vue historique personnelle tout en conservant les reçus d'audit sécurisés."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCompletedOrders(completedOrders)
                        showDeleteCompletedDialog = false
                    },
                ) {
                    Text("Supprimer", color = RubyError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCompletedDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Mes échanges",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Text(
                text = "Les détails des transactions récentes sont affichés sur les 3 derniers mois.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OrderFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = ForestGreen,
                        ),
                        modifier = Modifier.testTag("orders_filter_${filter.name.lowercase()}"),
                    )
                    if (filter == OrderFilter.COMPLETED) {
                        IconButton(
                            onClick = { showDeleteCompletedDialog = true },
                            enabled = completedOrders.isNotEmpty(),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("orders_delete_completed_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Supprimer les ordres complétés",
                                tint = if (completedOrders.isNotEmpty()) RubyError else TextMuted.copy(alpha = 0.45f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (ordersState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                }
                is UiState.Error -> {
                    Surface(
                        color = RubyContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, RubyError),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        Text(
                            text = ordersState.message,
                            color = TextPrimary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                is UiState.Success -> {
                    val filtered = visibleRecentOrders.filter { order ->
                        when (selectedFilter) {
                            OrderFilter.ALL -> true
                            OrderFilter.PENDING -> order.status == OrderStatus.PENDING.name
                            OrderFilter.SUBMITTED -> order.status == OrderStatus.SUBMITTED.name || order.status == OrderStatus.PAID_IN.name
                            OrderFilter.COMPLETED -> order.status == OrderStatus.COMPLETED.name
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(56.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aucun ordre dans cette catégorie",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Créez votre premier échange depuis l'onglet Échanger pour suivre vos transactions en temps réel.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        val listState = rememberLazyListState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(filtered, key = { it.id }) { order ->
                                    OrderCard(
                                        order = order,
                                        onClick = { onOrderClick(order) },
                                        onDeleteClick = { orderPendingDelete = order },
                                    )
                                }
                            }
                            ScrollToTopButton(listState = listState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: ExchangeOrder,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val status = OrderStatus.fromString(order.status)
    val sendLabel = PaymentMethod.fromId(order.sendMethod).label
    val receiveLabel = PaymentMethod.fromId(order.receiveMethod).label
    val dateStr = remember(order.createdAt) {
        order.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM HH:mm", Locale.FRENCH).format(it)
        } ?: "À l'instant"
    }

    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.75.dp, ObsidianOutline),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("order_card_${order.orderCode}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    val isTopUpOrder = order.orderType == "TOPUP" || order.sendMethod == order.receiveMethod
                    Text(
                        text = if (isTopUpOrder) "${order.orderCode} • RECHARGE" else order.orderCode,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OrderStatusBadge(status = status)
                    if (status == OrderStatus.COMPLETED) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("order_delete_${order.orderCode}"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Supprimer l'ordre ${order.orderCode}",
                                tint = RubyError,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val isTopUpOrder = order.orderType == "TOPUP" || order.sendMethod == order.receiveMethod
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sendLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${formatNum(order.sendAmount)} ${order.sendCurrency}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "vers",
                        tint = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (isTopUpOrder) "Crédit portefeuille" else receiveLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${formatNum(order.receiveAmount)} ${order.receiveCurrency}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = EmeraldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (status == OrderStatus.PENDING && order.gatewayMode != "WALLET") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Appuyez pour voir les instructions et soumettre votre référence",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            } else if (order.gatewayMode == "WALLET") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Débité de votre portefeuille $sendLabel • Traitement en cours",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldPrimary,
                )
            } else if (order.externalTxRef.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Réf : ${order.externalTxRef}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
}
