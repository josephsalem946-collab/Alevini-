package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SendToMobile
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.BuildConfig
import com.example.data.domain.AppLanguage
import com.example.data.domain.LanguageCatalog
import com.example.data.model.CurrencyCode
import com.example.data.model.PaymentMethod
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
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
import com.example.ui.viewmodel.AppNotification
import com.example.ui.viewmodel.WalletSummary

private const val SUPPORT_EMAIL = "supportdisponible@gmail.com"
private const val SUPPORT_WHATSAPP_LABEL = "Whatsapp business MCSE"
private const val SUPPORT_WHATSAPP_NUMBER = "+509 32153281"
private const val SUPPORT_WHATSAPP_DIGITS = "50932153281"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerFeatureModalSheet(
    destination: DrawerDestination,
    notifications: List<AppNotification>,
    walletSummary: WalletSummary,
    isBalanceHidden: Boolean,
    selectedLanguageCode: String,
    onDismiss: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onOpenOrderById: (String) -> Unit,
    onToggleBalanceHidden: () -> Unit,
    onStartDeposit: () -> Unit,
    onStartDepositMethod: (PaymentMethod) -> Unit,
    onStartWithdraw: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenSupport: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary,
    ) {
        when (destination) {
            DrawerDestination.NOTIFICATIONS -> NotificationsSheetContent(
                notifications = notifications,
                onDismiss = onDismiss,
                onDeleteNotification = onDeleteNotification,
                onClearAll = onClearAllNotifications,
                onNotificationClick = { orderId ->
                    if (orderId != null) {
                        onDismiss()
                        onOpenOrderById(orderId)
                    }
                },
            )
            DrawerDestination.WALLET -> WalletSheetContent(
                walletSummary = walletSummary,
                isBalanceHidden = isBalanceHidden,
                onToggleBalanceHidden = onToggleBalanceHidden,
                onStartDeposit = onStartDeposit,
                onStartDepositMethod = onStartDepositMethod,
                onStartWithdraw = onStartWithdraw,
                onDismiss = onDismiss,
            )
            DrawerDestination.LANGUAGES -> LanguagesSheetContent(
                selectedCode = selectedLanguageCode,
                onSelectLanguage = onSelectLanguage,
                onDismiss = onDismiss,
            )
            DrawerDestination.ABOUT -> AboutSheetContent(onDismiss = onDismiss)
            DrawerDestination.FAQ -> FaqSheetContent(
                onOpenSupport = onOpenSupport,
                onDismiss = onDismiss,
            )
            DrawerDestination.PRIVACY -> PrivacySheetContent(onDismiss = onDismiss)
            DrawerDestination.DOWNLOAD -> AppDownloadSheetContent(
                onShowMessage = onShowMessage,
                onDismiss = onDismiss,
            )
            DrawerDestination.SUPPORT -> SupportSheetContent(
                onShowMessage = onShowMessage,
                onDismiss = onDismiss,
            )
            DrawerDestination.STORE, DrawerDestination.ADMIN -> Unit
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    trailingAction: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingAction != null) {
                trailingAction()
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Fermer", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun NotificationsSheetContent(
    notifications: List<AppNotification>,
    onDismiss: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onClearAll: () -> Unit,
    onNotificationClick: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SheetHeader(
            title = "Notifications",
            subtitle = "${notifications.size} alerte(s) active(s)",
            onDismiss = onDismiss,
            trailingAction = {
                if (notifications.isNotEmpty()) {
                    IconButton(
                        onClick = onClearAll,
                        modifier = Modifier.size(48.dp).testTag("clear_all_notifications_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Tout supprimer",
                            tint = RubyError,
                        )
                    }
                }
            },
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (notifications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsOff,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aucune notification",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = "Vos alertes de dépôts, retraits et confirmations d'ordres apparaîtront ici.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 460.dp),
            ) {
                items(notifications, key = { it.id }) { item ->
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (item.isAlert) RubyError.copy(alpha = 0.45f) else ObsidianOutline,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = item.orderId != null) {
                                onNotificationClick(item.orderId)
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (item.isAlert) RubyError else EmeraldPrimary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                    )
                                    Text(
                                        text = item.timeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onDeleteNotification(item.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_notification_${item.id}"),
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

@Composable
private fun WalletSheetContent(
    walletSummary: WalletSummary,
    isBalanceHidden: Boolean,
    onToggleBalanceHidden: () -> Unit,
    onStartDeposit: () -> Unit,
    onStartDepositMethod: (PaymentMethod) -> Unit,
    onStartWithdraw: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        SheetHeader(
            title = "Wallet Balance",
            subtitle = "Soldes séparés par passerelle et frais associés",
            onDismiss = onDismiss,
            trailingAction = {
                IconButton(
                    onClick = onToggleBalanceHidden,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sheet_balance_eye_toggle"),
                ) {
                    Icon(
                        imageVector = if (isBalanceHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (isBalanceHidden) "Afficher les soldes" else "Masquer les soldes",
                        tint = EmeraldPrimary,
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            walletSummary.gateways.forEach { gw ->
                val amountLabel = if (isBalanceHidden) {
                    "•••••• ${gw.currency.code}"
                } else if (gw.currency == CurrencyCode.USD) {
                    "$${formatNum(gw.balance)} USD"
                } else {
                    "${formatNum(gw.balance)} HTG"
                }
                val gwDesc = when (gw.method) {
                    PaymentMethod.NOWPAYMENTS -> "Crypto & autres portefeuilles (USDT, BTC, TRX)"
                    PaymentMethod.BINANCE -> "Portefeuille Binance Pay USD"
                    PaymentMethod.MERU -> "Compte Meru USD"
                    PaymentMethod.MONCASH -> "Portefeuille mobile Digicel HTG"
                    PaymentMethod.NATCASH -> "Portefeuille mobile Natcom HTG"
                }
                Surface(
                    color = ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (gw.balance > 0.0) EmeraldPrimary.copy(alpha = 0.45f) else ObsidianOutline,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sheet_wallet_${gw.method.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        ) {
                            GatewayLogo(
                                method = gw.method,
                                size = 42.dp,
                                cornerRadius = 11.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = gw.method.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = gwDesc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                                Text(
                                    text = "Frais passerelle : ${formatNum(gw.feePercent)}%",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldAccent,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = amountLabel,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (gw.balance > 0.0 || isBalanceHidden) EmeraldPrimary else TextMuted,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { onStartDepositMethod(gw.method) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = "Recharger",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onStartDeposit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = ObsidianBg,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 50.dp),
                ) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recharger",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = onStartWithdraw,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 50.dp),
                ) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Échanger / Retirer",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = "Ordres complétés : ${walletSummary.completedOrdersCount}  •  En cours : ${walletSummary.pendingOrdersCount}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun LanguagesSheetContent(
    selectedCode: String,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            LanguageCatalog.SUPPORTED_LANGUAGES
        } else {
            LanguageCatalog.SUPPORTED_LANGUAGES.filter { lang ->
                lang.frenchName.lowercase().contains(q) ||
                    lang.nativeName.lowercase().contains(q) ||
                    lang.code.lowercase().contains(q)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        SheetHeader(
            title = "Langues (${LanguageCatalog.SUPPORTED_LANGUAGES.size} disponibles)",
            subtitle = "Actuelle : ${LanguageCatalog.findByCode(selectedCode).frenchName}",
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher parmi 130 langues...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = ObsidianOutline,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("language_search_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 420.dp),
        ) {
            items(filteredLanguages, key = { it.code }) { lang ->
                val isSelected = lang.code.equals(selectedCode, ignoreCase = true)
                Surface(
                    color = if (isSelected) EmeraldContainer.copy(alpha = 0.7f) else ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else ObsidianOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectLanguage(lang) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lang.frenchName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) EmeraldPrimary else TextPrimary,
                            )
                            Text(
                                text = "${lang.nativeName} • ${lang.code.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Sélectionnée",
                                tint = EmeraldPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSheetContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        SheetHeader(
            title = "À propos d'Alevini",
            subtitle = "Version ${BuildConfig.VERSION_NAME} • Plateforme d'échange sécurisée",
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoBlock(
                title = "Notre mission",
                body = "Alevini connecte vos portefeuilles locaux (MonCash, NatCash) et internationaux (Binance Pay, Meru, Crypto NOWPayments) avec des taux transparents en HTG, USD et EUR.",
            )
            InfoBlock(
                title = "Sécurité & Fiabilité",
                body = "• Verrouillage automatique du taux pendant 15 minutes.\n• Vérification anti-doublon des références de transaction.\n• Protection biométrique locale et isolation stricte de vos ordres.",
            )
            InfoBlock(
                title = "Support officiel",
                body = "Service client disponible par e-mail : $SUPPORT_EMAIL",
            )
        }
    }
}

@Composable
private fun FaqSheetContent(
    onOpenSupport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val faqItems = remember {
        listOf(
            "Comment effectuer un dépôt ou un retrait ?" to
                "Sélectionnez votre passerelle d'envoi (ex. MonCash) et votre passerelle de réception (ex. Binance Pay), saisissez le montant, entrez votre compte de réception et créez l'ordre.",
            "Combien de temps le taux reste-t-il verrouillé ?" to
                "Chaque ordre verrouille le taux affiché pendant exactement 15 minutes afin de vous protéger contre toute variation pendant le transfert.",
            "Comment valider mon paiement ?" to
                "Après avoir effectué le transfert vers le numéro marchand affiché, collez l'ID de transaction (référence MonCash/NatCash/Binance) dans la fiche de l'ordre.",
            "Comment masquer mon solde ?" to
                "Ouvrez le menu latéral et appuyez sur l'icône d'œil dans la carte Solde ou dans Dépôt • Retrait • Solde pour masquer instantanément vos montants.",
            "Que faire si une transaction prend du retard ?" to
                "Vérifiez le statut dans l'onglet Commandes ou contactez directement notre support à $SUPPORT_EMAIL avec votre code d'ordre.",
        )
    }
    var expandedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        SheetHeader(
            title = "FAQ & Aide",
            subtitle = "Réponses rapides aux questions fréquentes",
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            faqItems.forEachIndexed { idx, (q, a) ->
                val isOpen = expandedIndex == idx
                Surface(
                    color = ObsidianSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { expandedIndex = if (isOpen) -1 else idx }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = q,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = if (isOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                            )
                        }
                        AnimatedVisibility(visible = isOpen) {
                            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                                HorizontalDivider(color = ObsidianOutline.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = a,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenSupport,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            ) {
                Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Contacter le support",
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PrivacySheetContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        SheetHeader(
            title = "Politique de confidentialité",
            subtitle = "Protection de vos données personnelles",
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoBlock(
                title = "1. Données collectées",
                body = "Nous collectons uniquement les informations nécessaires à vos opérations : adresse e-mail Google, nom d'affichage, numéro MonCash/NatCash fourni dans votre profil et références de transactions soumises.",
            )
            InfoBlock(
                title = "2. Utilisation et sécurité",
                body = "Vos données servent exclusivement à traiter vos ordres d'échange, prévenir la fraude (vérification d'unicité des références) et sécuriser votre compte via l'authentification Google et la biométrie locale de l'appareil.",
            )
            InfoBlock(
                title = "3. Stockage et confidentialité",
                body = "Les données biométriques ne quittent jamais votre téléphone. Vos préférences locales (masquage du solde, langue) sont stockées localement de façon chiffrée sur l'appareil.",
            )
            InfoBlock(
                title = "4. Vos droits",
                body = "Pour demander l'accès, la rectification ou la suppression de vos données, écrivez-nous à : $SUPPORT_EMAIL",
            )
        }
    }
}

@Composable
private fun AppDownloadSheetContent(
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storeUrl = remember(context.packageName) { AppShareHelper.playStoreWebUrl(context) }
    val qrBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, storeUrl) {
        value = withContext(Dispatchers.Default) {
            AppShareHelper.generateQrBitmap(storeUrl, sizePx = 512)
        }
    }
    var isPreparingApk by remember { mutableStateOf(false) }
    var isPreparingQr by remember { mutableStateOf(false) }
    var pendingSplitShare by remember { mutableStateOf<ApkSharePreparation.Ready?>(null) }
    val splitCount = remember(context) {
        context.applicationInfo.splitSourceDirs?.count { it.isNotBlank() } ?: 0
    }

    pendingSplitShare?.let { prep ->
        AlertDialog(
            onDismissRequest = { pendingSplitShare = null },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Installation en fichiers séparés détectée") },
            text = {
                Text(
                    "L'application installée sur cet appareil provient d'un format divisé (1 fichier principal + ${prep.uris.size - 1} partie(s)). " +
                        "Un autre téléphone ne pourra pas l'installer facilement sans l'APK universel unique. " +
                        "Pour installer sans erreur sur n'importe quel téléphone, partagez le QR Code Google Play ou installez d'abord l'APK universel unique."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingSplitShare = null
                        runCatching {
                            AppShareHelper.shareApkDirect(context, prep)
                            onShowMessage("Fichiers préparés (${prep.totalSizeLabel}).")
                        }.onFailure {
                            onShowMessage("Aucune application de transfert trouvée sur cet appareil.")
                        }
                    },
                ) {
                    Text("Envoyer quand même", color = ForestGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSplitShare = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        SheetHeader(
            title = "Partager & Télécharger Alevini",
            subtitle = "QR Code Google Play • Envoi direct téléphone à téléphone",
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Scannable QR Code Card (replaces all visible URLs)
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_share_qr_card"),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(18.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode2,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "QR Code Officiel Alevini",
                            style = MaterialTheme.typography.titleSmall,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ObsidianOutline),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(216.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(10.dp),
                        ) {
                            val bmp = qrBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "QR Code pour ouvrir Alevini sur Google Play",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(196.dp)
                                        .testTag("app_playstore_qr_image"),
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = EmeraldPrimary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scannez ce QR Code depuis n'importe quel téléphone pour ouvrir Alevini directement sur Google Play.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (splitCount == 0) {
                            "Version ${BuildConfig.VERSION_NAME} • APK universel (1 fichier)"
                        } else {
                            "Version ${BuildConfig.VERSION_NAME} • Installation fragmentée ($splitCount parties)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (splitCount == 0) ForestGreen else TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Direct Phone-to-Phone APK Transfer
            Button(
                onClick = {
                    if (isPreparingApk) return@Button
                    scope.launch {
                        isPreparingApk = true
                        val prep = AppShareHelper.prepareAppApkForSharing(context)
                        isPreparingApk = false
                        when (prep) {
                            is ApkSharePreparation.Ready -> {
                                if (prep.isSplitBundle) {
                                    pendingSplitShare = prep
                                } else {
                                    runCatching {
                                        AppShareHelper.shareApkDirect(context, prep)
                                        onShowMessage(
                                            "APK universel prêt (${prep.totalSizeLabel}) — choisissez Partage à proximité, Bluetooth ou WhatsApp."
                                        )
                                    }.onFailure {
                                        onShowMessage("Aucune application de transfert trouvée sur cet appareil.")
                                    }
                                }
                            }
                            is ApkSharePreparation.Error -> onShowMessage(prep.message)
                        }
                    }
                },
                enabled = !isPreparingApk,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = EmeraldOnPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("send_apk_phone_to_phone_btn"),
            ) {
                if (isPreparingApk) {
                    CircularProgressIndicator(
                        color = EmeraldOnPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Préparation de l'application...",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Icon(Icons.Rounded.SendToMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Envoyer l'app (Téléphone à téléphone)",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }

            // Share QR Code Image with Community
            Button(
                onClick = {
                    if (isPreparingQr) return@Button
                    scope.launch {
                        isPreparingQr = true
                        val uri = AppShareHelper.createShareableQrCardUri(context)
                        isPreparingQr = false
                        if (uri != null) {
                            runCatching { AppShareHelper.shareQrImage(context, uri) }
                                .onFailure { onShowMessage("Impossible d'ouvrir le menu de partage.") }
                        } else {
                            onShowMessage("Erreur lors de la création du QR Code partageable.")
                        }
                    }
                },
                enabled = !isPreparingQr,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldContainer,
                    contentColor = ForestGreen,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("share_qr_community_btn"),
            ) {
                if (isPreparingQr) {
                    CircularProgressIndicator(
                        color = ForestGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Partager le QR Code dans la communauté",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }

            // Preserved "Ouvrir sur Google Play" button
            OutlinedButton(
                onClick = { AppShareHelper.openOnGooglePlay(context) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, EmeraldPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("open_google_play_btn"),
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ouvrir sur Google Play",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

@Composable
private fun SupportSheetContent(
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        SheetHeader(
            title = "Support client",
            subtitle = "Assistance rapide pour vos dépôts, retraits et échanges",
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = SUPPORT_WHATSAPP_LABEL,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        softWrap = true,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SUPPORT_WHATSAPP_NUMBER,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = ForestGreen,
                        softWrap = true,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$SUPPORT_WHATSAPP_LABEL : $SUPPORT_WHATSAPP_NUMBER — Assistance directe par messagerie WhatsApp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        softWrap = true,
                    )
                }
            }

            Button(
                onClick = {
                    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$SUPPORT_WHATSAPP_DIGITS"))
                    runCatching { context.startActivity(waIntent) }
                        .onFailure {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(SUPPORT_WHATSAPP_LABEL, SUPPORT_WHATSAPP_NUMBER))
                            onShowMessage("WhatsApp indisponible. Numéro copié : $SUPPORT_WHATSAPP_NUMBER")
                        }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("open_support_whatsapp_btn"),
            ) {
                Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Contacter sur WhatsApp Business",
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            }

            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(SUPPORT_WHATSAPP_LABEL, SUPPORT_WHATSAPP_NUMBER))
                    onShowMessage("Numéro WhatsApp copié : $SUPPORT_WHATSAPP_NUMBER")
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("copy_support_whatsapp_btn"),
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Copier $SUPPORT_WHATSAPP_NUMBER",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = ObsidianOutline.copy(alpha = 0.7f),
            )

            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("E-mail du support officiel", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SUPPORT_EMAIL,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = ForestGreen,
                        softWrap = true,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Indiquez votre code d'ordre (ex. ALV-XXXX) et votre référence de paiement pour un traitement prioritaire.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        softWrap = true,
                    )
                }
            }

            Button(
                onClick = {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$SUPPORT_EMAIL")
                        putExtra(Intent.EXTRA_SUBJECT, "Support Alevini - Assistance")
                    }
                    runCatching { context.startActivity(emailIntent) }
                        .onFailure {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Support Email", SUPPORT_EMAIL))
                            onShowMessage("Aucune app e-mail trouvée. Adresse copiée : $SUPPORT_EMAIL")
                        }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("send_support_email_btn"),
            ) {
                Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Envoyer un e-mail",
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Support Email", SUPPORT_EMAIL))
                    onShowMessage("Adresse support copiée : $SUPPORT_EMAIL")
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("copy_support_email_btn"),
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Copier $SUPPORT_EMAIL",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Surface(
        color = ObsidianSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
    }
}
