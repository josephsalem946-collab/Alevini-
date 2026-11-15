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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ExchangeOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.formatNum
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
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
    onOrderClick: (ExchangeOrder) -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(OrderFilter.ALL) }

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
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OrderFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldContainer,
                            selectedLabelColor = EmeraldPrimary,
                        ),
                    )
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
                    val filtered = ordersState.data.filter { order ->
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
                                    imageVector = Icons.Rounded.ReceiptLong,
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
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(filtered, key = { it.id }) { order ->
                                OrderCard(order = order, onClick = { onOrderClick(order) })
                            }
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
        border = BorderStroke(1.dp, ObsidianOutline),
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
                Column {
                    Text(
                        text = order.orderCode,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldPrimary,
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                OrderStatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(sendLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = "${formatNum(order.sendAmount)} ${order.sendCurrency}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "vers",
                        tint = EmeraldPrimary,
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(receiveLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            text = "${formatNum(order.receiveAmount)} ${order.receiveCurrency}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = EmeraldPrimary,
                        )
                    }
                }
            }

            if (status == OrderStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Appuyez pour voir les instructions et soumettre votre référence",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
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
