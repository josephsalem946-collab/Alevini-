package com.example.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.domain.PinSecurityHelper
import com.example.data.repository.PinSecurityState
import com.example.data.repository.PinVerifyResult
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
import com.example.ui.theme.RubyContainer
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PinStage {
    VERIFY_LOCK,
    RECOVERY,
    SETUP_CREATE,
    SETUP_CONFIRM,
    RESET_CREATE,
    RESET_CONFIRM,
}

@Composable
fun PinLockScreen(
    pinState: PinSecurityState,
    onVerifyPin: suspend (String) -> PinVerifyResult,
    onSaveNewPin: suspend (pin: String, recoveryEmail: String?) -> Boolean,
    onRequestRecoveryCode: suspend (email: String) -> Result<Unit>,
    onVerifyRecoveryCode: suspend (email: String, code: String) -> Result<Unit>,
    onUnlocked: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var stage by remember(pinState.isPinConfigured) {
        mutableStateOf(
            if (pinState.isPinConfigured) PinStage.VERIFY_LOCK else PinStage.SETUP_CREATE
        )
    }

    var pinInput by remember { mutableStateOf("") }
    var firstPinCandidate by remember { mutableStateOf("") }
    var setupRecoveryEmailInput by remember { mutableStateOf(pinState.recoveryEmail) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Recovery state
    var recoveryCodeInput by remember { mutableStateOf("") }
    var isRecoveryBusy by remember { mutableStateOf(false) }
    var recoveryCodeSent by remember { mutableStateOf(false) }
    var recoveryInfoMessage by remember { mutableStateOf<String?>(null) }

    // Live cooldown countdown if locked out
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val remainingLockSeconds = ((pinState.lockoutUntilMillis - nowMillis) / 1000L).coerceAtLeast(0L)

    LaunchedEffect(pinState.lockoutUntilMillis) {
        while (pinState.lockoutUntilMillis > System.currentTimeMillis()) {
            nowMillis = System.currentTimeMillis()
            delay(500L)
        }
        nowMillis = System.currentTimeMillis()
    }

    // Support system back gesture inside recovery or confirmation steps
    BackHandler(enabled = stage != PinStage.VERIFY_LOCK && stage != PinStage.SETUP_CREATE) {
        errorMessage = null
        pinInput = ""
        stage = when (stage) {
            PinStage.RECOVERY -> PinStage.VERIFY_LOCK
            PinStage.SETUP_CONFIRM -> PinStage.SETUP_CREATE
            PinStage.RESET_CREATE -> PinStage.RECOVERY
            PinStage.RESET_CONFIRM -> PinStage.RESET_CREATE
            else -> stage
        }
    }

    fun submitCompletePin(digits: String) {
        when (stage) {
            PinStage.VERIFY_LOCK -> {
                if (remainingLockSeconds > 0L) {
                    pinInput = ""
                    return
                }
                scope.launch {
                    when (val result = onVerifyPin(digits)) {
                        PinVerifyResult.Success -> {
                            errorMessage = null
                            pinInput = ""
                            onUnlocked()
                        }
                        PinVerifyResult.NotConfigured -> {
                            pinInput = ""
                            stage = PinStage.SETUP_CREATE
                        }
                        is PinVerifyResult.Incorrect -> {
                            pinInput = ""
                            errorMessage = "Code PIN incorrect (${result.remainingAttempts} essai(s) restant(s))."
                        }
                        is PinVerifyResult.LockedOut -> {
                            pinInput = ""
                            nowMillis = System.currentTimeMillis()
                            errorMessage = "Trop de tentatives erronées. Veuillez patienter avant de réessayer."
                        }
                    }
                }
            }
            PinStage.SETUP_CREATE -> {
                firstPinCandidate = digits
                pinInput = ""
                errorMessage = null
                stage = PinStage.SETUP_CONFIRM
            }
            PinStage.SETUP_CONFIRM -> {
                if (digits != firstPinCandidate) {
                    pinInput = ""
                    firstPinCandidate = ""
                    errorMessage = "Les deux codes PIN ne correspondent pas. Recommencez."
                    stage = PinStage.SETUP_CREATE
                } else {
                    scope.launch {
                        val saved = onSaveNewPin(digits, setupRecoveryEmailInput.trim())
                        if (saved) {
                            pinInput = ""
                            errorMessage = null
                            onUnlocked()
                        } else {
                            pinInput = ""
                            errorMessage = "Impossible d'enregistrer le code PIN."
                        }
                    }
                }
            }
            PinStage.RESET_CREATE -> {
                firstPinCandidate = digits
                pinInput = ""
                errorMessage = null
                stage = PinStage.RESET_CONFIRM
            }
            PinStage.RESET_CONFIRM -> {
                if (digits != firstPinCandidate) {
                    pinInput = ""
                    firstPinCandidate = ""
                    errorMessage = "Les codes PIN saisis ne correspondent pas."
                    stage = PinStage.RESET_CREATE
                } else {
                    scope.launch {
                        val saved = onSaveNewPin(digits, null)
                        if (saved) {
                            pinInput = ""
                            errorMessage = null
                            onUnlocked()
                        } else {
                            pinInput = ""
                            errorMessage = "Échec de la réinitialisation du PIN."
                        }
                    }
                }
            }
            PinStage.RECOVERY -> Unit
        }
    }

    fun appendDigit(digit: Char) {
        if (stage == PinStage.RECOVERY) return
        if (stage == PinStage.VERIFY_LOCK && remainingLockSeconds > 0L) return
        if (pinInput.length >= PinSecurityHelper.PIN_LENGTH) return
        errorMessage = null
        val updated = pinInput + digit
        pinInput = updated
        if (updated.length == PinSecurityHelper.PIN_LENGTH) {
            submitCompletePin(updated)
        }
    }

    fun deleteDigit() {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBg, ObsidianSurface)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Branding & Security Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.alevini_icon_fg_1794757900851),
                        contentDescription = "Logo Alevini",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ALEVINI",
                            style = MaterialTheme.typography.titleLarge,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Accès Sécurisé",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = if (stage == PinStage.RECOVERY) GoldContainer else EmeraldContainer,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        1.dp,
                        if (stage == PinStage.RECOVERY) GoldAccent.copy(alpha = 0.45f) else EmeraldPrimary.copy(alpha = 0.4f),
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = when (stage) {
                                PinStage.VERIFY_LOCK -> Icons.Rounded.Lock
                                PinStage.RECOVERY -> Icons.Rounded.VpnKey
                                else -> Icons.Rounded.Shield
                            },
                            contentDescription = null,
                            tint = if (stage == PinStage.RECOVERY) GoldAccent else EmeraldPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (stage) {
                                PinStage.VERIFY_LOCK -> "VERROUILLAGE"
                                PinStage.RECOVERY -> "RÉCUPÉRATION"
                                else -> "CODE PIN 4 CHIFFRES"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stage == PinStage.RECOVERY) GoldAccent else EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = stage,
                label = "pin_stage_transition",
                modifier = Modifier.fillMaxWidth(),
            ) { currentStage ->
                if (currentStage == PinStage.RECOVERY) {
                    // --- RÉCUPÉRATION (6-digit email validation code) ---
                    val registeredEmail = pinState.recoveryEmail.trim()
                    val hasRegisteredEmail = registeredEmail.isNotBlank()

                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_recovery_panel"),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Récupération du code PIN",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Un code de validation à 6 chiffres est envoyé à l'adresse e-mail enregistrée pour réinitialiser votre code PIN.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )

                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (hasRegisteredEmail) EmeraldPrimary.copy(alpha = 0.4f) else GoldAccent.copy(alpha = 0.5f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Email,
                                        contentDescription = null,
                                        tint = if (hasRegisteredEmail) EmeraldPrimary else GoldAccent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Adresse e-mail enregistrée",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                        )
                                        Text(
                                            text = if (hasRegisteredEmail) {
                                                PinSecurityHelper.maskEmail(registeredEmail)
                                            } else {
                                                "Non renseigné"
                                            },
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasRegisteredEmail) TextPrimary else GoldAccent,
                                            modifier = Modifier.testTag("recovery_registered_email_text"),
                                        )
                                    }
                                }
                            }

                            if (!hasRegisteredEmail) {
                                Surface(
                                    color = RubyContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, RubyError.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "Aucune adresse e-mail n'est enregistrée (Non renseigné). Connectez-vous d'abord puis configurez votre e-mail de récupération dans Profil & Sécurité.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    errorMessage = null
                                    recoveryInfoMessage = null
                                    isRecoveryBusy = true
                                    scope.launch {
                                        val res = onRequestRecoveryCode(registeredEmail)
                                        isRecoveryBusy = false
                                        res.onSuccess {
                                            recoveryCodeSent = true
                                            recoveryInfoMessage = "Code à 6 chiffres envoyé à ${PinSecurityHelper.maskEmail(registeredEmail)}."
                                        }.onFailure { err ->
                                            errorMessage = err.message ?: "Impossible d'envoyer le code à 6 chiffres."
                                        }
                                    }
                                },
                                enabled = hasRegisteredEmail && !isRecoveryBusy,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = ObsidianBg,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("send_recovery_code_button"),
                            ) {
                                if (isRecoveryBusy && !recoveryCodeSent) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianBg, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (recoveryCodeSent) "Renvoyer le code (6 chiffres)" else "Envoyer le code à 6 chiffres",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            if (recoveryInfoMessage != null) {
                                Text(
                                    text = recoveryInfoMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary,
                                )
                            }

                            OutlinedTextField(
                                value = recoveryCodeInput,
                                onValueChange = { raw ->
                                    val digitsOnly = raw.filter { it.isDigit() }.take(PinSecurityHelper.RECOVERY_CODE_LENGTH)
                                    recoveryCodeInput = digitsOnly
                                    errorMessage = null
                                },
                                label = { Text("Code de validation à 6 chiffres") },
                                placeholder = { Text("000000", color = TextMuted) },
                                singleLine = true,
                                enabled = hasRegisteredEmail,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = ObsidianOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recovery_code_input"),
                            )

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RubyError,
                                )
                            }

                            Button(
                                onClick = {
                                    if (!PinSecurityHelper.isValidRecoveryCodeFormat(recoveryCodeInput)) {
                                        errorMessage = "Entrez les 6 chiffres du code de validation."
                                        return@Button
                                    }
                                    errorMessage = null
                                    isRecoveryBusy = true
                                    scope.launch {
                                        val res = onVerifyRecoveryCode(registeredEmail, recoveryCodeInput)
                                        isRecoveryBusy = false
                                        res.onSuccess {
                                            recoveryCodeInput = ""
                                            pinInput = ""
                                            firstPinCandidate = ""
                                            stage = PinStage.RESET_CREATE
                                        }.onFailure { err ->
                                            errorMessage = err.message ?: "Code de validation invalide."
                                        }
                                    }
                                },
                                enabled = hasRegisteredEmail && recoveryCodeInput.length == PinSecurityHelper.RECOVERY_CODE_LENGTH && !isRecoveryBusy,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = ObsidianBg,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("verify_recovery_code_button"),
                            ) {
                                if (isRecoveryBusy && recoveryCodeSent) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianBg, strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = "Valider le code à 6 chiffres",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    recoveryInfoMessage = null
                                    recoveryCodeInput = ""
                                    pinInput = ""
                                    stage = PinStage.VERIFY_LOCK
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                border = BorderStroke(1.dp, ObsidianOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("back_to_pin_lock_button"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Retour au verrouillage PIN",
                                    color = TextSecondary,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                } else {
                    // --- 4-DIGIT PIN KEYPAD (VERIFY / SETUP / RESET) ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = when (currentStage) {
                                PinStage.VERIFY_LOCK -> "Entrez votre code PIN"
                                PinStage.SETUP_CREATE -> "Créer votre code PIN (4 chiffres)"
                                PinStage.SETUP_CONFIRM -> "Confirmer votre code PIN"
                                PinStage.RESET_CREATE -> "Nouveau code PIN (4 chiffres)"
                                PinStage.RESET_CONFIRM -> "Confirmer le nouveau PIN"
                                PinStage.RECOVERY -> ""
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (currentStage) {
                                PinStage.VERIFY_LOCK -> "Saisissez le code PIN de sécurité à 4 chiffres avant d'accéder à la connexion."
                                PinStage.SETUP_CREATE -> "Ce code PIN à 4 chiffres protège l'accès à ALEVINI dès l'ouverture."
                                PinStage.SETUP_CONFIRM -> "Saisissez une deuxième fois vos 4 chiffres pour confirmer."
                                PinStage.RESET_CREATE -> "Choisissez un nouveau code PIN à 4 chiffres."
                                PinStage.RESET_CONFIRM -> "Confirmez votre nouveau code PIN à 4 chiffres."
                                PinStage.RECOVERY -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )

                        if (currentStage == PinStage.SETUP_CREATE) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = setupRecoveryEmailInput,
                                onValueChange = { setupRecoveryEmailInput = it },
                                label = { Text("E-mail de récupération (optionnel)") },
                                placeholder = { Text("Non renseigné", color = TextMuted) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = ObsidianOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_recovery_email_input"),
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // 4 Animated PIN Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("pin_dots_row"),
                        ) {
                            repeat(PinSecurityHelper.PIN_LENGTH) { index ->
                                val filled = index < pinInput.length
                                val hasError = errorMessage != null
                                val dotColor by animateColorAsState(
                                    targetValue = when {
                                        hasError -> RubyError
                                        filled -> EmeraldPrimary
                                        else -> ObsidianSurfaceElevated
                                    },
                                    label = "pin_dot_color",
                                )
                                val borderColor by animateColorAsState(
                                    targetValue = when {
                                        hasError -> RubyError
                                        filled -> EmeraldPrimary
                                        else -> ObsidianOutline
                                    },
                                    label = "pin_dot_border",
                                )
                                val dotScale by animateFloatAsState(
                                    targetValue = if (filled) 1.15f else 1.0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    label = "pin_dot_scale",
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(dotScale)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                        .border(1.5.dp, borderColor, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (remainingLockSeconds > 0L) {
                            Surface(
                                color = RubyContainer,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, RubyError.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Trop de tentatives. Réessayez dans ${remainingLockSeconds}s.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        } else if (errorMessage != null) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = RubyError,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Numeric Keypad
                        val keypadRows = listOf(
                            listOf('1', '2', '3'),
                            listOf('4', '5', '6'),
                            listOf('7', '8', '9'),
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            keypadRows.forEach { row ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    row.forEach { digit ->
                                        PinKeypadDigitButton(
                                            digit = digit,
                                            enabled = remainingLockSeconds == 0L,
                                            onClick = { appendDigit(digit) },
                                        )
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Empty placeholder to balance keypad symmetry
                                Spacer(modifier = Modifier.size(68.dp))
                                PinKeypadDigitButton(
                                    digit = '0',
                                    enabled = remainingLockSeconds == 0L,
                                    onClick = { appendDigit('0') },
                                )
                                Surface(
                                    color = ObsidianSurfaceElevated,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, ObsidianOutline),
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = pinInput.isNotEmpty()) { deleteDigit() }
                                        .testTag("pin_backspace_button"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                            contentDescription = "Effacer un chiffre",
                                            tint = if (pinInput.isNotEmpty()) TextPrimary else TextMuted,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                if (stage == PinStage.VERIFY_LOCK) {
                    TextButton(
                        onClick = {
                            errorMessage = null
                            recoveryInfoMessage = null
                            recoveryCodeInput = ""
                            pinInput = ""
                            stage = PinStage.RECOVERY
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("forgot_pin_button"),
                    ) {
                        Text(
                            text = "Forgot my pin • J'ai oublié mon PIN",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldAccent,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                } else if (stage == PinStage.SETUP_CONFIRM || stage == PinStage.RESET_CONFIRM) {
                    TextButton(
                        onClick = {
                            pinInput = ""
                            firstPinCandidate = ""
                            errorMessage = null
                            stage = if (stage == PinStage.SETUP_CONFIRM) PinStage.SETUP_CREATE else PinStage.RESET_CREATE
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Text("Recommencer", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeypadDigitButton(
    digit: Char,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = ObsidianSurfaceElevated,
        shape = CircleShape,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("pin_digit_$digit"),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit.toString(),
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.headlineSmall,
                color = if (enabled) TextPrimary else TextMuted,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
