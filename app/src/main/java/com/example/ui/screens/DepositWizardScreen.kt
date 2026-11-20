package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptoCoin
import com.example.data.model.CurrencyCode
import com.example.data.model.ExchangeConfig
import com.example.data.model.ExchangeOrder
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.ui.components.GatewayLogo
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.ScrollToTopButton
import com.example.ui.components.formatNum
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceBright
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.WalletSummary
import java.text.SimpleDateFormat
import java.util.Locale

private enum class DepositSubTab(val label: String, val icon: ImageVector) {
    DEPOT("Dépôt", Icons.Rounded.AccountBalanceWallet),
    RETRAIT("Retrait", Icons.Rounded.ArrowUpward),
    HISTORIQUE("Historique", Icons.Rounded.History),
}

@Composable
fun DepositWizardScreen(
    config: ExchangeConfig,
    userProfile: UserProfile?,
    walletSummary: WalletSummary,
    preselectedMethod: PaymentMethod?,
    isBalanceHidden: Boolean,
    ordersState: UiState<List<ExchangeOrder>>,
    dismissedOrderIds: Set<String>,
    isGatewayBusy: Boolean,
    isSubmittingOrder: Boolean,
    onConsumePreselectedMethod: () -> Unit,
    onToggleBalanceHidden: () -> Unit,
    onCreateTopUpOrder: (
        gatewayMethod: PaymentMethod,
        cryptoCoin: CryptoCoin?,
        amount: Double,
        onCreated: (ExchangeOrder) -> Unit,
        onError: (String) -> Unit,
    ) -> Unit,
    onInitiateOnlinePayment: (ExchangeOrder, (ExchangeOrder) -> Unit) -> Unit,
    onVerifyOnlinePayment: (ExchangeOrder) -> Unit,
    onSubmitTxRef: (String, String, () -> Unit) -> Unit,
    onOrderClick: (ExchangeOrder) -> Unit,
    onDeleteOrder: (ExchangeOrder) -> Unit,
    onStartWithdraw: (PaymentMethod) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var activeSubTab by rememberSaveable { mutableStateOf(DepositSubTab.DEPOT) }
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    var sendMethod by rememberSaveable { mutableStateOf(PaymentMethod.MONCASH) }
    var cryptoCoin by rememberSaveable { mutableStateOf(CryptoCoin.USDT_TRC20) }
    var amountText by rememberSaveable { mutableStateOf("2500") }
    var isConfirmed by rememberSaveable { mutableStateOf(false) }

    var activeOrder by remember { mutableStateOf<ExchangeOrder?>(null) }

    LaunchedEffect(preselectedMethod) {
        if (preselectedMethod != null) {
            activeSubTab = DepositSubTab.DEPOT
            sendMethod = preselectedMethod
            amountText = if (preselectedMethod.supportsHtg) "2500" else "50"
            isConfirmed = false
            currentStep = 2
            onConsumePreselectedMethod()
        }
    }

    // Keep Step 3's activeOrder synchronized with live Firestore updates
    val liveActiveOrder = remember(activeOrder, ordersState) {
        val current = activeOrder ?: return@remember null
        val list = (ordersState as? UiState.Success)?.data
        list?.firstOrNull { it.id == current.id } ?: current
    }

    BackHandler(enabled = activeSubTab == DepositSubTab.DEPOT && currentStep > 1) {
        currentStep = (currentStep - 1).coerceAtLeast(1)
    }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Column(
            modifier = Modifier.widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top Gradient Card: Security badge + Segregated Balances + 3 sub-tabs
            DepositHeroCard(
                walletSummary = walletSummary,
                isBalanceHidden = isBalanceHidden,
                activeSubTab = activeSubTab,
                onToggleBalanceHidden = onToggleBalanceHidden,
                onSelectSubTab = { activeSubTab = it },
                onQuickSelectGateway = { method ->
                    activeSubTab = DepositSubTab.DEPOT
                    sendMethod = method
                    amountText = if (method.supportsHtg) "2500" else "50"
                    currentStep = 2
                },
            )

            AnimatedContent(targetState = activeSubTab, label = "deposit_sub_tab") { tab ->
                when (tab) {
                    DepositSubTab.DEPOT -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            WizardStepperBar(currentStep = currentStep)

                            when (currentStep) {
                                1 -> DepositStepOneContent(
                                    config = config,
                                    walletSummary = walletSummary,
                                    isBalanceHidden = isBalanceHidden,
                                    selectedMethod = sendMethod,
                                    onSelectMethod = { chosen ->
                                        sendMethod = chosen
                                        if (!chosen.supportsHtg && amountText == "2500") {
                                            amountText = "50"
                                        } else if (chosen.supportsHtg && amountText == "50") {
                                            amountText = "2500"
                                        }
                                    },
                                    onContinue = { currentStep = 2 },
                                )
                                2 -> DepositStepTwoContent(
                                    config = config,
                                    walletSummary = walletSummary,
                                    sendMethod = sendMethod,
                                    cryptoCoin = cryptoCoin,
                                    amountText = amountText,
                                    isConfirmed = isConfirmed,
                                    isSubmitting = isSubmittingOrder,
                                    onBackToMode = { currentStep = 1 },
                                    onAmountChange = { amountText = it },
                                    onSelectCryptoCoin = { cryptoCoin = it },
                                    onToggleConfirm = { isConfirmed = it },
                                    onValidateAndCreateTopUp = {
                                        val amountVal = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                        onCreateTopUpOrder(
                                            sendMethod,
                                            if (sendMethod == PaymentMethod.NOWPAYMENTS) cryptoCoin else null,
                                            amountVal,
                                            { created ->
                                                activeOrder = created
                                                currentStep = 3
                                            },
                                            { errMsg ->
                                                onShowMessage(errMsg)
                                            },
                                        )
                                    },
                                )
                                else -> {
                                    val order = liveActiveOrder
                                    if (order == null) {
                                        currentStep = 1
                                    } else {
                                        DepositStepThreeContent(
                                            order = order,
                                            config = config,
                                            isGatewayBusy = isGatewayBusy,
                                            onBackToAmount = {
                                                isConfirmed = false
                                                currentStep = 1
                                            },
                                            onInitiateOnlinePayment = {
                                                onInitiateOnlinePayment(order) { updated ->
                                                    activeOrder = updated
                                                }
                                            },
                                            onVerifyOnlinePayment = { onVerifyOnlinePayment(order) },
                                            onSubmitTxRef = { ref ->
                                                onSubmitTxRef(order.id, ref) {
                                                    isConfirmed = false
                                                    currentStep = 1
                                                    activeSubTab = DepositSubTab.HISTORIQUE
                                                }
                                            },
                                            onResetNewDeposit = {
                                                isConfirmed = false
                                                activeOrder = null
                                                currentStep = 1
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DepositSubTab.RETRAIT -> {
                        WithdrawTabContent(
                            walletSummary = walletSummary,
                            isBalanceHidden = isBalanceHidden,
                            onStartWithdraw = onStartWithdraw,
                            onGoToDeposit = { activeSubTab = DepositSubTab.DEPOT },
                        )
                    }

                    DepositSubTab.HISTORIQUE -> {
                        HistoryTabContent(
                            ordersState = ordersState,
                            dismissedOrderIds = dismissedOrderIds,
                            onOrderClick = onOrderClick,
                            onDeleteOrder = onDeleteOrder,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
        ScrollToTopButton(scrollState = scrollState)
    }
}

@Composable
private fun DepositHeroCard(
    walletSummary: WalletSummary,
    isBalanceHidden: Boolean,
    activeSubTab: DepositSubTab,
    onToggleBalanceHidden: () -> Unit,
    onSelectSubTab: (DepositSubTab) -> Unit,
    onQuickSelectGateway: (PaymentMethod) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(0.75.dp, EmeraldPrimary.copy(alpha = 0.28f)),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(ObsidianSurface, EmeraldContainer.copy(alpha = 0.65f))
                    )
                )
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Wallet Balance • Comptes séparés",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = ObsidianSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(0.8.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "SÉCURISÉ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                lineHeight = 12.sp,
                                letterSpacing = 0.5.sp,
                            ),
                            color = ForestGreen,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = ObsidianSurface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.75.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "SOLDES DISPONIBLES PAR PASSERELLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                lineHeight = 13.sp,
                                letterSpacing = 0.4.sp,
                            ),
                            color = TextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 6.dp),
                        )
                        IconButton(
                            onClick = onToggleBalanceHidden,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("wizard_balance_eye_toggle"),
                        ) {
                            Icon(
                                imageVector = if (isBalanceHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (isBalanceHidden) "Afficher les soldes" else "Masquer les soldes",
                                tint = ForestGreen,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.padding(end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        walletSummary.gateways.forEach { gw ->
                            val balDisplay = if (isBalanceHidden) {
                                "•••••• ${gw.currency.code}"
                            } else if (gw.currency == CurrencyCode.USD) {
                                "$${formatNum(gw.balance)} USD"
                            } else {
                                "${formatNum(gw.balance)} HTG"
                            }
                            Surface(
                                color = ObsidianSurface.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(0.8.dp, ObsidianOutline.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { onQuickSelectGateway(gw.method) }
                                    .testTag("hero_wallet_${gw.method.id}"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    ) {
                                        GatewayLogo(
                                            method = gw.method,
                                            size = 20.dp,
                                            cornerRadius = 5.dp,
                                        )
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text(
                                            text = gw.method.label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.5.sp,
                                                lineHeight = 15.sp,
                                            ),
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Frais ${formatNum(gw.feePercent)}%",
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.5.sp,
                                                lineHeight = 12.sp,
                                            ),
                                            color = GoldAccent.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                    Text(
                                        text = balDisplay,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp,
                                        ),
                                        color = if (gw.balance > 0.0 || isBalanceHidden) EmeraldPrimary else TextMuted,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 sub-tabs inside Hero Card
            Surface(
                color = ObsidianBg.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DepositSubTab.entries.forEach { tab ->
                        val selected = activeSubTab == tab
                        Surface(
                            color = if (selected) EmeraldPrimary else ObsidianBg.copy(alpha = 0f),
                            shape = RoundedCornerShape(11.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable { onSelectSubTab(tab) }
                                .testTag("wizard_subtab_${tab.name.lowercase()}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (selected) ObsidianBg else TextSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) ObsidianBg else TextSecondary,
                                    maxLines = 1,
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

@Composable
private fun WizardStepperBar(currentStep: Int) {
    val steps = listOf(1 to "Mode", 2 to "Montant", 3 to "Paiement")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        steps.forEachIndexed { idx, (num, label) ->
            val completed = num < currentStep
            val active = num == currentStep
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (completed || active) EmeraldPrimary else ObsidianSurfaceElevated
                        ),
                ) {
                    if (completed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = ObsidianBg,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text(
                            text = num.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (active) ObsidianBg else TextMuted,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active || completed) TextPrimary else TextMuted,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
            if (idx < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (num < currentStep) EmeraldPrimary else ObsidianOutline,
                )
            }
        }
    }
}

@Composable
private fun WithdrawTabContent(
    walletSummary: WalletSummary,
    isBalanceHidden: Boolean,
    onStartWithdraw: (PaymentMethod) -> Unit,
    onGoToDeposit: () -> Unit,
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = EmeraldContainer,
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Text(
                text = "Espace Retrait Alevini",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Transférez ou échangez depuis l'un de vos portefeuilles dédiés vers MonCash, NatCash, Binance Pay, Meru ou Crypto.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    walletSummary.gateways.forEach { gw ->
                        val balStr = if (isBalanceHidden) {
                            "•••••• ${gw.currency.code}"
                        } else if (gw.currency == CurrencyCode.USD) {
                            "$${formatNum(gw.balance)} USD"
                        } else {
                            "${formatNum(gw.balance)} HTG"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = gw.method.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            Text(
                                text = balStr,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (gw.balance > 0.0) EmeraldPrimary else TextMuted,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onStartWithdraw(PaymentMethod.MONCASH) },
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("withdraw_moncash_btn"),
            ) {
                Text("Retrait vers MonCash", fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            OutlinedButton(
                onClick = { onStartWithdraw(PaymentMethod.NATCASH) },
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                border = BorderStroke(1.dp, EmeraldPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("withdraw_natcash_btn"),
            ) {
                Text("Retrait vers NatCash", fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
            }

            TextButton(onClick = onGoToDeposit, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Retourner aux Recharges", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun HistoryTabContent(
    ordersState: UiState<List<ExchangeOrder>>,
    dismissedOrderIds: Set<String>,
    onOrderClick: (ExchangeOrder) -> Unit,
    onDeleteOrder: (ExchangeOrder) -> Unit,
) {
    var orderPendingDelete by remember { mutableStateOf<ExchangeOrder?>(null) }

    if (orderPendingDelete != null) {
        val target = orderPendingDelete!!
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

    when (ordersState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        }
        is UiState.Error -> {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RubyError),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(ordersState.message, color = TextPrimary, modifier = Modifier.padding(16.dp))
            }
        }
        is UiState.Success -> {
            val visibleOrders = ordersState.data.filterNot { it.id in dismissedOrderIds }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Journal des Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = "${visibleOrders.size} transaction${if (visibleOrders.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                if (visibleOrders.isEmpty()) {
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucune transaction enregistrée.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                } else {
                    visibleOrders.forEach { order ->
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
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOrderClick(order) }
                                .testTag("history_order_${order.orderCode}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    color = EmeraldContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.padding(9.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                    val isTopUp = order.orderType == "TOPUP" || order.sendMethod == order.receiveMethod
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = if (isTopUp) "${order.orderCode} • Recharge $sendLabel" else "${order.orderCode} • $sendLabel → $receiveLabel",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OrderStatusBadge(status = status)
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(start = 6.dp),
                                ) {
                                    Text(
                                        text = "+${formatNum(order.sendAmount)} ${order.sendCurrency}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldPrimary,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = "→ ${formatNum(order.receiveAmount)} ${order.receiveCurrency}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                    )
                                }
                                IconButton(
                                    onClick = { orderPendingDelete = order },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete_history_${order.orderCode}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Supprimer la transaction",
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
