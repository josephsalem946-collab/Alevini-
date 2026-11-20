package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Workspaces
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvestCatalog
import com.example.data.model.InvestCategory
import com.example.data.model.InvestCurrency
import com.example.data.model.InvestDuration
import com.example.data.model.InvestStatus
import com.example.data.model.InvestmentRecord
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.ui.components.GatewayLogo
import com.example.ui.components.ScrollToTopButton
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.LocalDataSaverMode
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RubyContainer
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.InvestViewModel
import com.example.ui.viewmodel.UiState

private data class InvestTabMeta(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun InvestScreen(
    userProfile: UserProfile?,
    investmentsState: UiState<List<InvestmentRecord>>,
    dismissedInvestmentIds: Set<String>,
    activeTab: String,
    selectedCurrency: InvestCurrency,
    selectedAmount: Double,
    selectedDuration: InvestDuration,
    selectedSector: String,
    selectedGateway: PaymentMethod,
    isSubmitting: Boolean,
    onSelectTab: (String) -> Unit,
    onSelectCurrency: (InvestCurrency) -> Unit,
    onSelectAmount: (Double) -> Unit,
    onSelectDuration: (InvestDuration) -> Unit,
    onSelectSector: (String) -> Unit,
    onSelectGateway: (PaymentMethod) -> Unit,
    onValidateInvestment: () -> Unit,
    onDeleteOrHideInvestment: (InvestmentRecord) -> Unit,
    onNavigateToTopUp: (PaymentMethod) -> Unit,
) {
    val context = LocalContext.current
    val isDataSaver = LocalDataSaverMode.current
    var investToDelete by remember { mutableStateOf<InvestmentRecord?>(null) }

    val visibleInvestments = remember(investmentsState, dismissedInvestmentIds) {
        ((investmentsState as? UiState.Success)?.data ?: emptyList())
            .filterNot { it.id in dismissedInvestmentIds }
    }

    val currentCategory = remember(activeTab) { InvestCategory.fromId(activeTab) }
    val currentGatewayBalance = remember(userProfile, selectedGateway) {
        userProfile?.balanceFor(selectedGateway) ?: 0.0
    }
    val simulation = remember(currentCategory, selectedAmount, selectedDuration, currentGatewayBalance) {
        InvestCatalog.calculateSimulation(
            category = currentCategory,
            amount = selectedAmount,
            duration = selectedDuration,
            availableBalance = currentGatewayBalance,
        )
    }

    investToDelete?.let { target ->
        val isRejected = InvestStatus.fromString(target.status) == InvestStatus.REJECTED
        AlertDialog(
            onDismissRequest = { investToDelete = null },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    if (isRejected) "Supprimer ${target.investCode} ?" else "Retirer ${target.investCode} de la vue ?",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    if (isRejected) {
                        "Cet investissement refusé sera supprimé définitivement."
                    } else {
                        "Ce dossier restera actif dans le suivi MCSE mais ne sera plus affiché dans cette liste."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteOrHideInvestment(target)
                        investToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyError, contentColor = EmeraldOnPrimary),
                ) {
                    Text(if (isRejected) "Supprimer" else "Retirer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { investToDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
        )
    }

    val listState = rememberLazyListState()
    val tabs = remember(visibleInvestments.size) {
        listOf(
            InvestTabMeta(InvestViewModel.TAB_SANS_RISQUE, "Investissement sans risque", Icons.Rounded.VerifiedUser),
            InvestTabMeta(InvestViewModel.TAB_MOINS_RISQUE, "Investissement à moins de risque", Icons.AutoMirrored.Rounded.TrendingUp),
            InvestTabMeta(InvestViewModel.TAB_A_RISQUE, "Investissement à risque", Icons.Rounded.WarningAmber),
            InvestTabMeta(InvestViewModel.TAB_MES_INVESTISSEMENTS, "Mes Investissements (${visibleInvestments.size})", Icons.Rounded.Workspaces),
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Hero Header Banner: MCSE L'investissement
            item(key = "invest_hero") {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.75.dp, ObsidianOutline),
                    shadowElevation = if (isDataSaver) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(0.75.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                        contentDescription = null,
                                        tint = ForestGreen,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "CAPITAL & RENDEMENT • MCSE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            if (isDataSaver) {
                                Surface(
                                    color = GoldContainer,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(0.75.dp, GoldAccent.copy(alpha = 0.4f)),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DataSaverOn,
                                            contentDescription = "Mode Économie de Données actif",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text = "Éco Data",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "MCSE L'investissement",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Faites fructifier votre capital en Gourdes (GDS) ou en Dollars (\$USD) selon votre profil de risque, avec suivi transparent et récupération garantie à l'échéance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // 2. Horizontal Category Tabs
            item(key = "invest_tabs") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tabs.forEach { tab ->
                        val isSelected = activeTab == tab.key
                        Surface(
                            color = if (isSelected) EmeraldPrimary else ObsidianSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                if (isSelected) 1.dp else 0.75.dp,
                                if (isSelected) EmeraldPrimary else ObsidianOutline,
                            ),
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectTab(tab.key) }
                                .testTag("invest_tab_${tab.key}"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) EmeraldOnPrimary else ForestGreen,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) EmeraldOnPrimary else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            if (activeTab == InvestViewModel.TAB_MES_INVESTISSEMENTS) {
                item(key = "my_invest_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Historique & Suivi de vos investissements",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        Text(
                            text = "${visibleInvestments.size} dossier(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                when (investmentsState) {
                    is UiState.Loading -> {
                        item(key = "my_invest_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = EmeraldPrimary)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item(key = "my_invest_error") {
                            Surface(
                                color = RubyContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = investmentsState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RubyError,
                                    modifier = Modifier.padding(14.dp),
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (visibleInvestments.isEmpty()) {
                            item(key = "my_invest_empty") {
                                Surface(
                                    color = ObsidianSurface,
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(0.75.dp, ObsidianOutline),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            text = "Vous n'avez aucun investissement enregistré pour le moment.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                        )
                                        Button(
                                            onClick = { onSelectTab(InvestViewModel.TAB_SANS_RISQUE) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldPrimary,
                                                contentColor = EmeraldOnPrimary,
                                            ),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("invest_start_button"),
                                        ) {
                                            Text("Commencer un investissement", fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(visibleInvestments, key = { it.id }) { inv ->
                                InvestmentHistoryCard(
                                    investment = inv,
                                    onPrintClick = { printInvestmentReceipt(context, inv) },
                                    onDeleteClick = { investToDelete = inv },
                                )
                            }
                        }
                    }
                }
            } else {
                // 3. Category Info, Rate, Terms & Transparency / Sector Picker
                item(key = "category_info_${currentCategory.id}") {
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(0.75.dp, ObsidianOutline),
                        shadowElevation = 2.dp,
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
                                Text(
                                    text = currentCategory.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = EmeraldContainer,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(0.75.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                                ) {
                                    Text(
                                        text = currentCategory.badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            Surface(
                                color = ObsidianSurfaceElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.75.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(12.dp),
                                ) {
                                    Surface(
                                        color = EmeraldContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(38.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                                contentDescription = null,
                                                tint = ForestGreen,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "RENDEMENT ESTIMÉ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                        )
                                        Text(
                                            text = currentCategory.rateLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ForestGreen,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = ObsidianSurfaceElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.75.dp, ObsidianOutline),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Description,
                                            contentDescription = null,
                                            tint = ForestGreen,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = "Termes & Conditions",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ForestGreen,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Text(
                                        text = currentCategory.terms,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                            }

                            if (currentCategory == InvestCategory.MOINS_RISQUE) {
                                InvestTransparencySection(items = InvestCatalog.MOINS_RISQUE_FEED)
                            } else if (currentCategory == InvestCategory.A_RISQUE) {
                                InvestSectorPickerSection(
                                    sectors = InvestCatalog.A_RISQUE_SECTORS,
                                    selectedSectorName = selectedSector,
                                    onSelectSector = onSelectSector,
                                )
                            }
                        }
                    }
                }

                // 4. Simulation Parameters: Currency, Amount, Duration
                item(key = "invest_params") {
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(0.75.dp, ObsidianOutline),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "1. Montant & Devise",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Surface(
                                    color = ObsidianSurfaceElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.75.dp, ObsidianOutline),
                                ) {
                                    Row(modifier = Modifier.padding(3.dp)) {
                                        InvestCurrency.entries.forEach { curr ->
                                            val isSelected = selectedCurrency == curr
                                            Surface(
                                                color = if (isSelected) EmeraldPrimary else ObsidianSurfaceElevated,
                                                shape = RoundedCornerShape(9.dp),
                                                modifier = Modifier
                                                    .heightIn(min = 36.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .clickable { onSelectCurrency(curr) }
                                                    .testTag("invest_currency_${curr.code}"),
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                ) {
                                                    Text(
                                                        text = if (curr == InvestCurrency.GDS) "GDS" else "\$USD",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isSelected) EmeraldOnPrimary else TextSecondary,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2x2 Amount grid
                            val amountRows = selectedCurrency.amounts.chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                amountRows.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        rowItems.forEach { amt ->
                                            val isSelected = selectedAmount == amt
                                            Surface(
                                                color = if (isSelected) EmeraldContainer else ObsidianSurfaceElevated,
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(
                                                    if (isSelected) 1.5.dp else 0.75.dp,
                                                    if (isSelected) EmeraldPrimary else ObsidianOutline,
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable { onSelectAmount(amt) }
                                                    .testTag("invest_amount_${amt.toInt()}"),
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                                ) {
                                                    Text(
                                                        text = InvestCatalog.formatInvestMoney(amt, selectedCurrency.code),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = if (isSelected) ForestGreen else TextPrimary,
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "2. Durée du placement",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                InvestDuration.entries.forEach { dur ->
                                    val isSelected = selectedDuration == dur
                                    Surface(
                                        color = if (isSelected) EmeraldPrimary else ObsidianSurfaceElevated,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            if (isSelected) 1.dp else 0.75.dp,
                                            if (isSelected) EmeraldPrimary else ObsidianOutline,
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectDuration(dur) }
                                            .testTag("invest_duration_${dur.id}"),
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                                        ) {
                                            Text(
                                                text = dur.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) EmeraldOnPrimary else TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                color = EmeraldContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.75.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = ForestGreen,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "Règle générale : Dès que l'investissement est validé, le client pourra récupérer son capital initial de ${InvestCatalog.formatInvestMoney(selectedAmount, selectedCurrency.code)} à la fin de la période choisie (${selectedDuration.label}).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldOnContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Validation & Payment Gateway Panel
                item(key = "invest_validation_panel") {
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "INVESTISSEMENT SÉLECTIONNÉ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "${currentCategory.title} (${selectedDuration.label})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = InvestCatalog.formatInvestMoney(selectedAmount, selectedCurrency.code),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                    Text(
                                        text = "Profit : +${InvestCatalog.formatInvestMoney(simulation.monthlyProfit, selectedCurrency.code)} / mois",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }

                            HorizontalDivider(thickness = 0.75.dp, color = ObsidianOutline)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Passerelle de règlement sécurisée (Frais 1,5 %)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InvestCatalog.SUPPORTED_GATEWAYS.forEach { method ->
                                    val isSelected = selectedGateway == method
                                    val gwCurr = InvestCatalog.currencyForGateway(method)
                                    val gwBalance = userProfile?.balanceFor(method) ?: 0.0
                                    Surface(
                                        color = if (isSelected) EmeraldContainer.copy(alpha = 0.65f) else ObsidianSurfaceElevated,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            if (isSelected) 1.5.dp else 0.75.dp,
                                            if (isSelected) EmeraldPrimary else ObsidianOutline,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onSelectGateway(method) }
                                            .testTag("invest_gateway_${method.id}"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                GatewayLogo(
                                                    method = method,
                                                    size = 34.dp,
                                                    cornerRadius = 9.dp,
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = method.label,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = "Solde : ${InvestCatalog.formatInvestMoney(gwBalance, gwCurr.code)} • 1,5 %",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ForestGreen,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) EmeraldPrimary else ObsidianOutline
                                                    ),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(ObsidianSurface)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Financial summary breakdown
                            Surface(
                                color = ObsidianSurfaceElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.75.dp, ObsidianOutline),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    SummaryLine(
                                        label = "Solde disponible (${selectedGateway.label}) :",
                                        value = InvestCatalog.formatInvestMoney(currentGatewayBalance, selectedCurrency.code),
                                        valueColor = if (simulation.hasEnoughBalance) TextPrimary else RubyError,
                                    )
                                    SummaryLine(
                                        label = "Capital investi :",
                                        value = InvestCatalog.formatInvestMoney(selectedAmount, selectedCurrency.code),
                                        valueColor = TextPrimary,
                                    )
                                    SummaryLine(
                                        label = "Profit total estimé (${selectedDuration.label}) :",
                                        value = "+${InvestCatalog.formatInvestMoney(simulation.totalProfit, selectedCurrency.code)}",
                                        valueColor = EmeraldPrimary,
                                    )
                                    SummaryLine(
                                        label = "Frais de passerelle (1,5 %) :",
                                        value = InvestCatalog.formatInvestMoney(simulation.feeAmount, selectedCurrency.code),
                                        valueColor = GoldAccent,
                                    )
                                    HorizontalDivider(thickness = 0.75.dp, color = ObsidianOutline)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Coût total à régler :",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        )
                                        Text(
                                            text = InvestCatalog.formatInvestMoney(simulation.totalCost, selectedCurrency.code),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ForestGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }

                            if (!simulation.hasEnoughBalance) {
                                Surface(
                                    color = GoldContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.75.dp, GoldAccent.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "Solde ${selectedGateway.label} insuffisant.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        )
                                        OutlinedButton(
                                            onClick = { onNavigateToTopUp(selectedGateway) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            border = BorderStroke(0.75.dp, GoldAccent),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                                            modifier = Modifier.heightIn(min = 48.dp),
                                        ) {
                                            Text("Recharger", fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = onValidateInvestment,
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = EmeraldOnPrimary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("invest_validate_button"),
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        color = EmeraldOnPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = "Valider • ${InvestCatalog.formatInvestMoney(simulation.totalCost, selectedCurrency.code)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
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

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
}
