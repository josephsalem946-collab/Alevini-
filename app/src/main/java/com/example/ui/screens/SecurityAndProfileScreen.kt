package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DataSaverOff
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.repository.PinSecurityState
import com.example.ui.auth.BiometricUnlockHelper
import com.example.ui.components.ScrollToTopButton
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PastelAmberStart
import com.example.ui.theme.PastelAmberText
import com.example.ui.theme.PastelOrangeEnd
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecurityAndProfileScreen(
    userEmail: String,
    userName: String,
    isAdmin: Boolean,
    userProfile: UserProfile?,
    isBiometricEnabled: Boolean,
    isDataSaverEnabled: Boolean,
    pinSecurityState: PinSecurityState,
    onUpdateProfile: (String, String, Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleDataSaver: (Boolean) -> Unit,
    onTogglePinLock: (Boolean) -> Unit,
    onUpdatePinCode: (currentPin: String, newPin: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onUpdateRecoveryEmail: (String) -> Unit,
    onLockAppNow: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val biometricSupported = remember(context) { BiometricUnlockHelper.isBiometricAvailable(context) }
    val cardElevation = if (isDataSaverEnabled) 0.dp else 3.dp

    var nameInput by remember(userProfile?.displayName, userName) {
        mutableStateOf(userProfile?.displayName?.ifBlank { userName } ?: userName)
    }
    var phoneInput by remember(userProfile?.phoneNumber) {
        mutableStateOf(userProfile?.phoneNumber ?: "")
    }
    var ageVerified by remember(userProfile?.isAgeVerified) {
        mutableStateOf(userProfile?.isAgeVerified ?: true)
    }
    var recoveryEmailInput by remember(pinSecurityState.recoveryEmail) {
        mutableStateOf(pinSecurityState.recoveryEmail)
    }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            if (userProfile.displayName.isNotBlank()) nameInput = userProfile.displayName
            phoneInput = userProfile.phoneNumber
            ageVerified = userProfile.isAgeVerified
        }
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                currentPinInput = ""
                newPinInput = ""
                pinDialogError = null
            },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = if (pinSecurityState.isPinConfigured) "Modifier le code PIN" else "Créer un code PIN (4 chiffres)",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pinSecurityState.isPinConfigured) {
                        OutlinedTextField(
                            value = currentPinInput,
                            onValueChange = { s ->
                                currentPinInput = s.filter { it.isDigit() }.take(4)
                                pinDialogError = null
                            },
                            label = { Text("Code PIN actuel (4 chiffres)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth().testTag("current_pin_dialog_input"),
                        )
                    }
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { s ->
                            newPinInput = s.filter { it.isDigit() }.take(4)
                            pinDialogError = null
                        },
                        label = { Text("Nouveau code PIN (4 chiffres)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_dialog_input"),
                    )
                    if (pinDialogError != null) {
                        Text(
                            text = pinDialogError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = RubyError,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePinCode(currentPinInput, newPinInput) { success, message ->
                            if (success) {
                                showChangePinDialog = false
                                currentPinInput = ""
                                newPinInput = ""
                                pinDialogError = null
                            } else {
                                pinDialogError = message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary),
                    modifier = Modifier.testTag("confirm_pin_dialog_button"),
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangePinDialog = false
                        currentPinInput = ""
                        newPinInput = ""
                        pinDialogError = null
                    }
                ) {
                    Text("Annuler", color = TextSecondary)
                }
            },
        )
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
        Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            Text(
                text = "Profil & Sécurité",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Account Identity Card
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.75.dp, ObsidianOutline),
                shadowElevation = cardElevation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = ForestGreen,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = nameInput.ifBlank { "Utilisateur Alevini" },
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
                        if (isAdmin) {
                            Surface(
                                color = GoldContainer,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ADMIN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nom complet") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedContainerColor = Color(0xFFFAFDFB),
                            unfocusedContainerColor = Color(0xFFFAFDFB),
                            focusedLabelColor = ForestGreen,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Numéro MonCash / NatCash (+509)") },
                        placeholder = { Text("+509 XXXX XXXX", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedContainerColor = Color(0xFFFAFDFB),
                            unfocusedContainerColor = Color(0xFFFAFDFB),
                            focusedLabelColor = ForestGreen,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Vérification d'âge (18+)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Requis pour les opérations financières", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Switch(
                            checked = ageVerified,
                            onCheckedChange = { ageVerified = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ObsidianSurfaceElevated,
                                uncheckedBorderColor = ObsidianOutline,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onUpdateProfile(nameInput, phoneInput, ageVerified) },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = EmeraldOnPrimary,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("save_profile_button"),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enregistrer le profil",
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Digit Security PIN & Email Recovery Card
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.75.dp, ObsidianOutline),
                shadowElevation = cardElevation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = ForestGreen)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Code PIN de sécurité (4 chiffres)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = if (pinSecurityState.isPinConfigured) {
                                    "Demandé avant l'accès à la connexion"
                                } else {
                                    "Aucun code PIN configuré"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                        Switch(
                            checked = pinSecurityState.isPinConfigured && pinSecurityState.isPinEnabled,
                            onCheckedChange = onTogglePinLock,
                            enabled = pinSecurityState.isPinConfigured,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ObsidianSurfaceElevated,
                                uncheckedBorderColor = ObsidianOutline,
                            ),
                            modifier = Modifier.testTag("pin_lock_toggle"),
                        )
                    }

                    OutlinedTextField(
                        value = recoveryEmailInput,
                        onValueChange = { recoveryEmailInput = it },
                        label = { Text("E-mail de récupération PIN (6 chiffres)") },
                        placeholder = { Text("Non renseigné", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianOutline,
                            focusedContainerColor = Color(0xFFFAFDFB),
                            unfocusedContainerColor = Color(0xFFFAFDFB),
                            focusedLabelColor = ForestGreen,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("profile_recovery_email_input"),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateRecoveryEmail(recoveryEmailInput) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                                .testTag("save_recovery_email_button"),
                        ) {
                            Text(
                                text = "Enregistrer e-mail",
                                style = MaterialTheme.typography.labelMedium,
                                color = ForestGreen,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            onClick = { showChangePinDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, Color(0xFFEAB76A)),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 48.dp)
                                .testTag("change_pin_button"),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(PastelAmberStart, PastelOrangeEnd),
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = if (pinSecurityState.isPinConfigured) "Modifier PIN" else "Créer PIN",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PastelAmberText,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (pinSecurityState.isPinConfigured && pinSecurityState.isPinEnabled) {
                        OutlinedButton(
                            onClick = onLockAppNow,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            border = BorderStroke(0.75.dp, ObsidianOutline),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("lock_app_now_button"),
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Verrouiller l'application maintenant",
                                color = TextSecondary,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Biometric Lock Card
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.75.dp, ObsidianOutline),
                shadowElevation = cardElevation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = ForestGreen)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Verrouillage biométrique",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (biometricSupported) {
                                "Protéger l'ouverture avec empreinte / Face / code"
                            } else {
                                "Configurez un code ou une empreinte sur l'appareil"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable && biometricSupported) {
                                BiometricUnlockHelper.promptUnlock(
                                    context = context,
                                    onSuccess = { onToggleBiometric(true) },
                                    onError = { },
                                )
                            } else {
                                onToggleBiometric(wantEnable)
                            }
                        },
                        enabled = biometricSupported || isBiometricEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ObsidianSurfaceElevated,
                            uncheckedBorderColor = ObsidianOutline,
                        ),
                        modifier = Modifier.testTag("biometric_toggle"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Data Saver Mode Card
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = if (isDataSaverEnabled) 1.dp else 0.75.dp,
                    color = if (isDataSaverEnabled) EmeraldPrimary.copy(alpha = 0.5f) else ObsidianOutline,
                ),
                shadowElevation = cardElevation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isDataSaverEnabled) Icons.Rounded.DataSaverOn else Icons.Rounded.DataSaverOff,
                                contentDescription = null,
                                tint = ForestGreen,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Mode Économie de Données",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                )
                                if (isDataSaverEnabled) {
                                    Surface(
                                        color = EmeraldContainer,
                                        shape = RoundedCornerShape(50),
                                    ) {
                                        Text(
                                            text = "ACTIF",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ForestGreen,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isDataSaverEnabled) {
                                    "Cache hors-ligne prioritaire • Flux & effets réduits"
                                } else {
                                    "Réduit la consommation data sur connexion lente"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDataSaverEnabled) ForestGreen else TextMuted,
                            )
                        }
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
                            modifier = Modifier.testTag("security_data_saver_toggle"),
                        )
                    }
                    Text(
                        text = "• Cache local persistant (100 Mo)\n• Notifications et historiques limités à l'essentiel\n• Suppression des ombres et animations lourdes",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                border = BorderStroke(1.dp, RubyError.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RubyError),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("sign_out_button"),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Déconnexion",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
        ScrollToTopButton(scrollState = scrollState)
    }
}
