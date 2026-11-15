package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.domain.AppLanguage
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
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
    walletSummary: WalletSummary,
    isBalanceHidden: Boolean,
    notificationCount: Int,
    selectedLanguage: AppLanguage,
    onToggleBalanceHidden: () -> Unit,
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
                            text = "Solde du portefeuille",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
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

                    Text(
                        text = if (isBalanceHidden) "•••••• HTG" else "${formatNum(walletSummary.netBalanceHtg)} HTG",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = EmeraldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBalanceHidden) "≈ •••••• USD" else "≈ $${formatNum(walletSummary.netBalanceUsd)} USD",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onQuickDeposit,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = ObsidianBg,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("drawer_deposit_btn"),
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dépôt", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onQuickWithdraw,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("drawer_withdraw_btn"),
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retrait", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
                label = { Text("Téléchargement de l'application", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.DOWNLOAD) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_download"),
            )

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = ObsidianOutline.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(6.dp))

            NavigationDrawerItem(
                label = {
                    Column {
                        Text("Support", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Text(
                            text = "supportdisponible@gmail.com",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                icon = { Icon(Icons.Rounded.SupportAgent, contentDescription = null) },
                selected = false,
                onClick = { onSelectDestination(DrawerDestination.SUPPORT) },
                colors = itemColors,
                modifier = Modifier.testTag("drawer_item_support"),
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
