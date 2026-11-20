package com.example.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DataSaverOff
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.domain.AppLanguage
import com.example.data.model.CurrencyCode
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.WalletSummary

enum class DrawerDestination {
    STORE,
    ADMIN,
    NOTIFICATIONS,
    WALLET,
    LANGUAGES,
    ABOUT,
    FAQ,
    PRIVACY,
    DOWNLOAD,
    SUPPORT,
}

@Composable
fun AleviniDrawerSheet(
    userName: String,
    userEmail: String,
    isAdmin: Boolean,
    walletSummary: WalletSummary,
    isBalanceHidden: Boolean,
    isDataSaverEnabled: Boolean,
    notificationCount: Int,
    selectedLanguage: AppLanguage,
    onToggleBalanceHidden: () -> Unit,
    onToggleDataSaver: (Boolean) -> Unit,
    onQuickDeposit: () -> Unit,
    onQuickWithdraw: () -> Unit,
    onSelectDestination: (DrawerDestination) -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = ObsidianSurface,
        drawerContentColor = TextPrimary,
        modifier = Modifier.widthIn(max = 332.dp),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // Drawer Brand & User Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.alevini_icon_fg_1794757900851),
                    contentDescription = "Logo Alevini",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifBlank { "Alevini" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Solde / Dépôt / Retrait Card with Mask Toggle
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ObsidianOutline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Wallet Balance • Comptes séparés",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        IconButton(
                            onClick = onToggleBalanceHidden,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("drawer_balance_eye_toggle"),
                        ) {
                            Icon(
                                imageVector = if (isBalanceHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (isBalanceHidden) "Afficher le solde" else "Masquer le solde",
                                tint = EmeraldPrimary,
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        walletSummary.gateways.forEach { gw ->
                            val balStr = if (isBalanceHidden) {
                                "••••• ${gw.currency.code}"
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                ) {
                                    GatewayLogo(
                                        method = gw.method,
                                        size = 18.dp,
                                        cornerRadius = 5.dp,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = gw.method.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${formatNum(gw.feePercent)}%)",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                                Text(
                                    text = balStr,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (gw.balance > 0.0 || isBalanceHidden) EmeraldPrimary else TextSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onQuickDeposit,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = ObsidianBg,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                                .testTag("drawer_deposit_btn"),
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Dépôt",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(
                            onClick = onQuickWithdraw,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                                .testTag("drawer_withdraw_btn"),
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Retrait",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ObsidianOutline.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            val itemColors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                selectedContainerColor = EmeraldContainer,
                unselectedIconColor = EmeraldPrimary,
                unselectedTextColor = TextPrimary,
                selectedIconColor = EmeraldPrimary,
                selectedTextColor = EmeraldPrimary,
            )

            NavigationDrawerItem(
                label = { Text("Alevini Digital Store", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Rounded.Storefront, contentDescription = null) },
                badge = {
                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = "LIVE",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.STORE) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_store"),
            )

            if (isAdmin) {
                NavigationDrawerItem(
                    label = { Text("Back-Office Admin", maxLines = 1, overflow = TextOverflow.Ellipsis, color = GoldAccent) },
                    icon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = GoldAccent) },
                    selected = false,
                    onClick = { onSelectDestination(DrawerDestination.ADMIN) },
                    colors = itemColors,
                    modifier = Modifier.testTag("drawer_item_admin"),
                )
            }

            NavigationDrawerItem(
                label = { Text("Notifications", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                badge = {
                    if (notificationCount > 0) {
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = notificationCount.toString(),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.NOTIFICATIONS) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_notifications"),
            )

            NavigationDrawerItem(
                label = { Text("Dépôt • Retrait • Solde", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) },
                badge = {
                    Text(
                        text = if (isBalanceHidden) "Masqué" else "Visible",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBalanceHidden) GoldAccent else EmeraldPrimary,
                    )
                },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.WALLET) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_wallet"),
            )

            NavigationDrawerItem(
                label = { Text("Langues (130 disponibles)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Rounded.Translate, contentDescription = null) },
                badge = {
                    Surface(
                        color = GoldContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = selectedLanguage.code.uppercase(),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.LANGUAGES) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_languages"),
            )

            Surface(
                color = if (isDataSaverEnabled) EmeraldContainer.copy(alpha = 0.65f) else Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onToggleDataSaver(!isDataSaverEnabled) }
                    .testTag("drawer_item_data_saver"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    Icon(
                        imageVector = if (isDataSaverEnabled) Icons.Rounded.DataSaverOn else Icons.Rounded.DataSaverOff,
                        contentDescription = null,
                        tint = if (isDataSaverEnabled) ForestGreen else EmeraldPrimary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                        Text(
                            text = "Économie de Données",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isDataSaverEnabled) ForestGreen else TextPrimary,
                            fontWeight = if (isDataSaverEnabled) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (isDataSaverEnabled) "Actif • Consommation réduite" else "Réduit la data mobile",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDataSaverEnabled) ForestGreen.copy(alpha = 0.85f) else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isDataSaverEnabled,
                        onCheckedChange = onToggleDataSaver,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ObsidianSurfaceElevated,
                            uncheckedBorderColor = ObsidianOutline,
                        ),
                        modifier = Modifier.testTag("drawer_data_saver_toggle"),
                    )
                }
            }

            NavigationDrawerItem(
                label = { Text("À propos", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.ABOUT) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_about"),
            )

            NavigationDrawerItem(
                label = { Text("FAQ & Aide", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null) },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.FAQ) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_faq"),
            )

            NavigationDrawerItem(
                label = { Text("Politique de confidentialité", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Rounded.PrivacyTip, contentDescription = null) },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.PRIVACY) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_privacy"),
            )

            NavigationDrawerItem(
                label = { Text("Partager l'app • QR Code", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Rounded.QrCode2, contentDescription = null) },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.DOWNLOAD) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_download"),
            )

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = ObsidianOutline.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onSelectDestination(DrawerDestination.SUPPORT) }
                    .testTag("drawer_item_support"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SupportAgent,
                        contentDescription = null,
                        tint = TextSecondary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Support",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                        )
                        Text(
                            text = "supportdisponible@gmail.com",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            softWrap = true,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Whatsapp business MCSE : +509 32153281",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            softWrap = true,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
