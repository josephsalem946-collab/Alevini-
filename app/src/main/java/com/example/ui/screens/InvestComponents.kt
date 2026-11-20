package com.example.ui.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Html
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import com.example.data.model.InvestCatalog
import com.example.data.model.InvestRiskSector
import com.example.data.model.InvestStatus
import com.example.data.model.InvestTransparencyItem
import com.example.data.model.InvestmentRecord
import com.example.ui.components.ScrollToTopButton
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
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
import java.util.Date
import java.util.Locale

@Composable
fun InvestStatusBadge(
    status: InvestStatus,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, icon) = when (status) {
        InvestStatus.ACTIVE -> Triple(EmeraldContainer, ForestGreen, Icons.Rounded.CheckCircle)
        InvestStatus.REJECTED -> Triple(RubyContainer, RubyError, Icons.Rounded.Cancel)
        InvestStatus.PENDING -> Triple(GoldContainer, GoldAccent, Icons.Rounded.Schedule)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.75.dp, fg.copy(alpha = 0.35f)),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = fg,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun InvestTransparencySection(items: List<InvestTransparencyItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Transparence des placements en cours (Évolution réelle) :",
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        items.forEach { item ->
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.75.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.sector,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val trendColor = if (item.positive) EmeraldPrimary else GoldAccent
                    val trendBg = if (item.positive) EmeraldContainer else GoldContainer
                    Surface(
                        color = trendBg,
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = if (item.positive) {
                                    Icons.AutoMirrored.Rounded.TrendingUp
                                } else {
                                    Icons.AutoMirrored.Rounded.TrendingDown
                                },
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = item.trend,
                                style = MaterialTheme.typography.labelSmall,
                                color = trendColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvestSectorPickerSection(
    sectors: List<InvestRiskSector>,
    selectedSectorName: String,
    onSelectSector: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Choisissez le secteur dans lequel vous souhaitez investir :",
            style = MaterialTheme.typography.labelMedium,
            color = ForestGreen,
            fontWeight = FontWeight.Bold,
        )
        sectors.forEach { sector ->
            val isSelected = selectedSectorName == sector.name
            Surface(
                color = if (isSelected) EmeraldContainer.copy(alpha = 0.65f) else ObsidianSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(if (isSelected) 1.5.dp else 0.75.dp, if (isSelected) EmeraldPrimary else ObsidianOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectSector(sector.name) }
                    .testTag("invest_sector_${sector.id}"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sector.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = sector.riskNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val badgeBg = if (sector.positive) EmeraldContainer else GoldContainer
                    val badgeFg = if (sector.positive) ForestGreen else GoldAccent
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = sector.performance,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = badgeFg,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvestmentHistoryCard(
    investment: InvestmentRecord,
    onPrintClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val status = InvestStatus.fromString(investment.status)
    val dateLabel = rememberCreatedDateLabel(investment)

    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.75.dp, ObsidianOutline),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invest_card_${investment.investCode}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = investment.investCode,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestGreen,
                            maxLines = 1,
                        )
                        Text(
                            text = "• Créé le $dateLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = investment.categoryName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = investment.sector,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                InvestStatusBadge(status = status)
            }

            HorizontalDivider(thickness = 0.75.dp, color = ObsidianOutline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InvestKpiTile(
                    label = "Capital investi",
                    value = InvestCatalog.formatInvestMoney(investment.amount, investment.currency),
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                InvestKpiTile(
                    label = "Profit / mois",
                    value = "+${InvestCatalog.formatInvestMoney(investment.monthlyProfit, investment.currency)}",
                    valueColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InvestKpiTile(
                    label = "Profit total (${investment.durationLabel})",
                    value = "+${InvestCatalog.formatInvestMoney(investment.totalProfit, investment.currency)}",
                    valueColor = ForestGreen,
                    modifier = Modifier.weight(1f),
                )
                InvestKpiTile(
                    label = "Récupération capital",
                    value = investment.endDateLabel,
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }

            if (investment.adminNote.isNotBlank()) {
                Surface(
                    color = EmeraldContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Note Admin : ${investment.adminNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldOnContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Règlement : ${investment.gatewayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Text(
                        text = "Frais (1,5 %) : ${InvestCatalog.formatInvestMoney(investment.feeAmount, investment.currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        textAlign = TextAlign.End,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onPrintClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        border = BorderStroke(0.75.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = EmeraldContainer.copy(alpha = 0.55f),
                            contentColor = ForestGreen,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 48.dp)
                            .testTag("invest_print_${investment.investCode}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Print,
                            contentDescription = "Imprimer le reçu",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Imprimer",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    OutlinedButton(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        border = BorderStroke(0.75.dp, RubyError.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = RubyContainer.copy(alpha = 0.65f),
                            contentColor = RubyError,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 48.dp)
                            .testTag("invest_delete_${investment.investCode}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Supprimer cet investissement",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Supprimer",
                            style = MaterialTheme.typography.labelMedium,
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

@Composable
private fun InvestKpiTile(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.75.dp, ObsidianOutline),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun rememberCreatedDateLabel(inv: InvestmentRecord): String {
    val date = inv.createdAt?.toDate() ?: Date()
    return SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(date)
}

fun printInvestmentReceipt(context: Context, inv: InvestmentRecord) {
    val createdStr = inv.createdAt?.toDate()?.let {
        SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.FRANCE).format(it)
    } ?: SimpleDateFormat("dd MMM yyyy", Locale.FRANCE).format(Date())
    val statusLabel = InvestStatus.fromString(inv.status).label
    fun esc(value: String): String = Html.escapeHtml(value)

    val html = """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="utf-8" />
          <style>
            body { font-family: sans-serif; color: #13221B; padding: 24px; }
            .box { border: 2px solid #135538; border-radius: 12px; padding: 20px; max-width: 580px; margin: 0 auto; }
            .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #E0E7E3; padding-bottom: 12px; margin-bottom: 16px; }
            .title { font-size: 18px; font-weight: 800; color: #135538; margin: 0; }
            .subtitle { font-size: 12px; color: #51655B; margin: 4px 0 0 0; }
            .code { font-family: monospace; font-weight: 700; font-size: 15px; color: #1D8356; }
            .row { margin: 8px 0; font-size: 13px; line-height: 1.5; }
            .footer { border-top: 1px solid #E0E7E3; padding-top: 12px; margin-top: 16px; font-size: 11px; color: #51655B; }
          </style>
        </head>
        <body>
          <div class="box">
            <div class="header">
              <div>
                <h1 class="title">ALEVINI • MCSE L'INVESTISSEMENT</h1>
                <p class="subtitle">Certificat / Reçu d'investissement officiel</p>
              </div>
              <span class="code">${esc(inv.investCode)}</span>
            </div>
            <div class="row"><strong>Client :</strong> ${esc(inv.userEmail.ifBlank { inv.userId })}</div>
            <div class="row"><strong>Date de souscription :</strong> ${esc(createdStr)}</div>
            <div class="row"><strong>Catégorie :</strong> ${esc(inv.categoryName)}</div>
            <div class="row"><strong>Secteur / Affectation :</strong> ${esc(inv.sector)}</div>
            <div class="row"><strong>Capital investi :</strong> ${esc(InvestCatalog.formatInvestMoney(inv.amount, inv.currency))}</div>
            <div class="row"><strong>Durée :</strong> ${esc(inv.durationLabel)} (Échéance : ${esc(inv.endDateLabel)})</div>
            <div class="row"><strong>Profit mensuel estimé :</strong> +${esc(InvestCatalog.formatInvestMoney(inv.monthlyProfit, inv.currency))}</div>
            <div class="row"><strong>Profit total estimé :</strong> +${esc(InvestCatalog.formatInvestMoney(inv.totalProfit, inv.currency))}</div>
            <div class="row"><strong>Passerelle de règlement :</strong> ${esc(inv.gatewayName)} (Frais 1,5 % : ${esc(InvestCatalog.formatInvestMoney(inv.feeAmount, inv.currency))})</div>
            <div class="row"><strong>Total réglé :</strong> ${esc(InvestCatalog.formatInvestMoney(inv.totalPaid, inv.currency))}</div>
            <div class="row"><strong>Statut actuel :</strong> ${esc(statusLabel)}</div>
            <div class="footer">
              Règle générale : Dès validation par l'administration ALEVINI MCSE, le capital initial est récupérable à la fin de la période choisie.
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val jobName = "ALEVINI_${inv.investCode}"
            val adapter = view?.createPrintDocumentAdapter(jobName) ?: return
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

@Composable
fun AdminInvestTab(
    investmentsState: UiState<List<InvestmentRecord>>,
    onApproveInvestment: (InvestmentRecord) -> Unit,
    onRejectInvestment: (InvestmentRecord) -> Unit,
    onDeleteInvestment: (String) -> Unit,
) {
    var investToDelete by remember { mutableStateOf<InvestmentRecord?>(null) }

    investToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { investToDelete = null },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Supprimer ${target.investCode} ?", fontWeight = FontWeight.Bold) },
            text = { Text("Cette action supprimera définitivement ce dossier d'investissement du Back-Office.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteInvestment(target.id)
                        investToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RubyError,
                        contentColor = EmeraldOnPrimary,
                    ),
                ) {
                    Text("Supprimer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { investToDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
        )
    }

    when (investmentsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldPrimary)
        }
        is UiState.Error -> Text(
            text = investmentsState.message,
            color = RubyError,
            modifier = Modifier.padding(16.dp),
        )
        is UiState.Success -> {
            val all = investmentsState.data
            val pendingCount = all.count { InvestStatus.fromString(it.status) == InvestStatus.PENDING }
            val listState = rememberLazyListState()

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "admin_invest_header") {
                        Surface(
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(0.75.dp, ObsidianOutline),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Administration • Validation des Investissements",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Acceptez ou refusez les demandes d'investissement. Un refus recrédite automatiquement le solde client.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = GoldContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "En attente : $pendingCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        )
                                    }
                                    Surface(color = EmeraldContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "Total : ${all.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ForestGreen,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (all.isEmpty()) {
                        item(key = "admin_invest_empty") {
                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.75.dp, ObsidianOutline),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Aucune demande d'investissement enregistrée pour le moment.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(24.dp),
                                )
                            }
                        }
                    } else {
                        items(all, key = { it.id }) { inv ->
                            val status = InvestStatus.fromString(inv.status)
                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(0.75.dp, ObsidianOutline),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${inv.investCode} • ${inv.userEmail.ifBlank { inv.userId }}",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ForestGreen,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${inv.categoryName} — ${inv.sector}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        InvestStatusBadge(status = status)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        InvestKpiTile(
                                            label = "Capital",
                                            value = InvestCatalog.formatInvestMoney(inv.amount, inv.currency),
                                            valueColor = TextPrimary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        InvestKpiTile(
                                            label = "Profit / mois",
                                            value = "+${InvestCatalog.formatInvestMoney(inv.monthlyProfit, inv.currency)}",
                                            valueColor = EmeraldPrimary,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        InvestKpiTile(
                                            label = "Durée & Échéance",
                                            value = "${inv.durationLabel} (${inv.endDateLabel})",
                                            valueColor = TextPrimary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        InvestKpiTile(
                                            label = "Passerelle",
                                            value = inv.gatewayName,
                                            valueColor = TextPrimary,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Max),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        OutlinedButton(
                                            onClick = { investToDelete = inv },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            border = BorderStroke(0.75.dp, ObsidianOutline),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .heightIn(min = 48.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Supprimer",
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { onRejectInvestment(inv) },
                                            enabled = status != InvestStatus.REJECTED && status != InvestStatus.ACTIVE,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                            border = BorderStroke(0.75.dp, RubyError.copy(alpha = 0.4f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = RubyContainer.copy(alpha = 0.6f),
                                                contentColor = RubyError,
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .heightIn(min = 48.dp),
                                        ) {
                                            Text(
                                                text = "Refuser",
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                textAlign = TextAlign.Center,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }

                                        Button(
                                            onClick = { onApproveInvestment(inv) },
                                            enabled = status != InvestStatus.ACTIVE,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldPrimary,
                                                contentColor = EmeraldOnPrimary,
                                            ),
                                            modifier = Modifier
                                                .weight(1.35f)
                                                .fillMaxHeight()
                                                .heightIn(min = 48.dp),
                                        ) {
                                            Text(
                                                text = "Accepter & Activer",
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
                ScrollToTopButton(listState = listState)
            }
        }
    }
}

