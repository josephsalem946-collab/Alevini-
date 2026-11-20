package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.domain.LanguageCatalog
import com.example.data.remote.PaymentGatewayClient
import com.example.data.repository.AleviniRepository
import com.example.data.repository.InvestRepository
import com.example.data.repository.PinSecurityState
import com.example.data.repository.SecurityPreferencesRepository
import com.example.data.repository.StoreRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.BiometricUnlockHelper
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.auth.PinLockScreen
import com.example.ui.components.AleviniDrawerSheet
import com.example.ui.components.DrawerDestination
import com.example.ui.components.DrawerFeatureModalSheet
import com.example.ui.components.OrderDetailBottomSheet
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.DepositWizardScreen
import com.example.ui.screens.ExchangeScreen
import com.example.ui.screens.InvestScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.SecurityAndProfileScreen
import com.example.ui.screens.StoreOrderDetailSheet
import com.example.ui.screens.StoreScreen
import com.example.ui.theme.AleviniTheme
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AleviniViewModel
import com.example.ui.viewmodel.InvestViewModel
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private object Routes {
    const val EXCHANGE = "exchange"
    const val DEPOSIT = "deposit"
    const val INVEST = "invest"
    const val STORE = "store"
    const val ORDERS = "orders"
    const val SECURITY = "security"
    const val ADMIN = "admin"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent {
            val appPrefs = remember { SecurityPreferencesRepository(applicationContext) }
            val isDataSaverGlobal by appPrefs.isDataSaverEnabled.collectAsStateWithLifecycle(initialValue = false)
            AleviniTheme(isDataSaverEnabled = isDataSaverGlobal) {
                AppNavigation()
            }
        }
    }
}

private fun obtainConfiguredFirestore(dbId: String): FirebaseFirestore {
    val db = FirebaseFirestore.getInstance(dbId)
    try {
        db.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100L * 1024L * 1024L)
                    .build()
            )
        }
    } catch (_: IllegalStateException) {
        // Firestore already initialized; ignore safely
    }
    return db
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val securityPrefs = remember(context) { SecurityPreferencesRepository(context.applicationContext) }
    val pinGatewayClient = remember { PaymentGatewayClient() }
    val pinState by securityPrefs.pinSecurityState.collectAsStateWithLifecycle(initialValue = PinSecurityState())
    var isPinSessionUnlocked by rememberSaveable { mutableStateOf(false) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var autoSignInAttempted by remember { mutableStateOf(false) }

    val requiresPinGate = pinState.isLoaded && (
        !pinState.isPinConfigured || (pinState.isPinEnabled && !isPinSessionUnlocked)
    )

    LaunchedEffect(requiresPinGate, pinState.isLoaded) {
        if (pinState.isLoaded && !requiresPinGate && !autoSignInAttempted && auth.currentUser == null) {
            autoSignInAttempted = true
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { },
                onUnauthenticated = { },
                scope = scope,
            )
        }
    }

    if (!pinState.isLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
        )
        return
    }

    if (requiresPinGate) {
        PinLockScreen(
            pinState = pinState,
            onVerifyPin = { pin -> securityPrefs.verifyPin(pin) },
            onSaveNewPin = { pin, email ->
                if (email != null) {
                    securityPrefs.setRecoveryEmail(email)
                }
                securityPrefs.saveNewPin(pin)
            },
            onRequestRecoveryCode = { email ->
                pinGatewayClient.requestPinRecoveryCode(pinState.cachedBackendUrl, email)
            },
            onVerifyRecoveryCode = { email, code ->
                pinGatewayClient.verifyPinRecoveryCode(pinState.cachedBackendUrl, email, code)
            },
            onUnlocked = { isPinSessionUnlocked = true },
        )
        return
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { },
            canLockApp = pinState.isPinConfigured && pinState.isPinEnabled,
            onLockApp = { isPinSessionUnlocked = false },
        )
    } else {
        key(user.uid) {
            val viewModel: AleviniViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = obtainConfiguredFirestore(dbId)
                        AleviniViewModel(
                            repository = AleviniRepository(db, auth),
                            securityPrefs = SecurityPreferencesRepository(app),
                            currentUserId = user.uid,
                            currentUserEmail = user.email.orEmpty(),
                            currentUserName = user.displayName.orEmpty(),
                            isEmailVerified = user.isEmailVerified,
                        )
                    }
                }
            )
            val storeViewModel: StoreViewModel = viewModel(
                key = "store_${user.uid}",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = obtainConfiguredFirestore(dbId)
                        StoreViewModel(
                            repository = StoreRepository(db, auth),
                            securityPrefs = SecurityPreferencesRepository(app),
                            currentUserId = user.uid,
                            currentUserEmail = user.email.orEmpty(),
                            isEmailVerified = user.isEmailVerified,
                        )
                    }
                }
            )
            val investViewModel: InvestViewModel = viewModel(
                key = "invest_${user.uid}",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = obtainConfiguredFirestore(dbId)
                        InvestViewModel(
                            repository = InvestRepository(db, auth),
                            securityPrefs = SecurityPreferencesRepository(app),
                            currentUserId = user.uid,
                            currentUserEmail = user.email.orEmpty(),
                            isEmailVerified = user.isEmailVerified,
                        )
                    }
                }
            )
            AuthenticatedAleviniShell(
                viewModel = viewModel,
                storeViewModel = storeViewModel,
                investViewModel = investViewModel,
                onLockAppNow = { isPinSessionUnlocked = false },
                onSignOut = {
                    GoogleAuthHelper.signOut(credentialManager, scope)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAleviniShell(
    viewModel: AleviniViewModel,
    storeViewModel: StoreViewModel,
    investViewModel: InvestViewModel,
    onLockAppNow: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val isBiometricEnabled by viewModel.isBiometricLockEnabled.collectAsStateWithLifecycle()
    var isUnlocked by remember { mutableStateOf(true) }
    var biometricPromptedOnce by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && !biometricPromptedOnce && BiometricUnlockHelper.isBiometricAvailable(context)) {
            isUnlocked = false
            biometricPromptedOnce = true
            BiometricUnlockHelper.promptUnlock(
                context = context,
                onSuccess = {
                    isUnlocked = true
                    biometricError = null
                },
                onError = { msg ->
                    biometricError = msg
                },
            )
        }
    }

    if (isBiometricEnabled && !isUnlocked) {
        BiometricLockGate(
            errorMessage = biometricError,
            onRetry = {
                BiometricUnlockHelper.promptUnlock(
                    context = context,
                    onSuccess = {
                        isUnlocked = true
                        biometricError = null
                    },
                    onError = { msg -> biometricError = msg },
                )
            },
            onSignOut = onSignOut,
        )
        return
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var activeDrawerSheet by remember { mutableStateOf<DrawerDestination?>(null) }

    val config by viewModel.exchangeConfig.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val calcState by viewModel.calculatorState.collectAsStateWithLifecycle()
    val quote by viewModel.computedQuote.collectAsStateWithLifecycle()
    val selectedOrder by viewModel.selectedOrder.collectAsStateWithLifecycle()
    val checkedSecurityItems by viewModel.checkedSecurityItems.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val isDataSaverEnabled by viewModel.isDataSaverEnabled.collectAsStateWithLifecycle()
    val selectedLanguageCode by viewModel.selectedLanguageCode.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val walletSummary by viewModel.walletSummary.collectAsStateWithLifecycle()
    val preselectedDepositMethod by viewModel.preselectedDepositMethod.collectAsStateWithLifecycle()
    val dismissedOrderIds by viewModel.dismissedOrderIds.collectAsStateWithLifecycle()
    val isGatewayBusy by viewModel.isGatewayBusy.collectAsStateWithLifecycle()

    val storeConfig by storeViewModel.storeConfig.collectAsStateWithLifecycle()
    val catalogProducts by storeViewModel.catalogProducts.collectAsStateWithLifecycle()
    val userStoreOrdersState by storeViewModel.userStoreOrdersState.collectAsStateWithLifecycle()
    val dismissedStoreOrderIds by storeViewModel.dismissedStoreOrderIds.collectAsStateWithLifecycle()
    val selectedStoreProductId by storeViewModel.selectedProductId.collectAsStateWithLifecycle()
    val selectedStoreGateway by storeViewModel.selectedGateway.collectAsStateWithLifecycle()
    val storeSelectedOption by storeViewModel.selectedOptionInput.collectAsStateWithLifecycle()
    val storeTargetInput by storeViewModel.targetAccountInput.collectAsStateWithLifecycle()
    val selectedStoreOrder by storeViewModel.selectedStoreOrder.collectAsStateWithLifecycle()
    val isStoreSubmitting by storeViewModel.isSubmitting.collectAsStateWithLifecycle()
    val isStoreGatewayBusy by storeViewModel.isGatewayBusy.collectAsStateWithLifecycle()
    val storeProvidersHealth by storeViewModel.providersHealth.collectAsStateWithLifecycle()

    val userInvestmentsState by investViewModel.userInvestmentsState.collectAsStateWithLifecycle()
    val dismissedInvestmentIds by investViewModel.dismissedInvestmentIds.collectAsStateWithLifecycle()
    val investActiveTab by investViewModel.activeTab.collectAsStateWithLifecycle()
    val investSelectedCurrency by investViewModel.selectedCurrency.collectAsStateWithLifecycle()
    val investSelectedAmount by investViewModel.selectedAmount.collectAsStateWithLifecycle()
    val investSelectedDuration by investViewModel.selectedDuration.collectAsStateWithLifecycle()
    val investSelectedSector by investViewModel.selectedSector.collectAsStateWithLifecycle()
    val investSelectedGateway by investViewModel.selectedGateway.collectAsStateWithLifecycle()
    val isInvestSubmitting by investViewModel.isSubmitting.collectAsStateWithLifecycle()
    var adminInitialTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(Unit) {
        storeViewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(Unit) {
        investViewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Keep open OrderDetailBottomSheet synced with live Firestore status updates
    val liveSelectedOrder = remember(selectedOrder, ordersState) {
        val current = selectedOrder ?: return@remember null
        val list = (ordersState as? UiState.Success)?.data
        list?.firstOrNull { it.id == current.id } ?: current
    }

    if (liveSelectedOrder != null) {
        OrderDetailBottomSheet(
            order = liveSelectedOrder,
            config = config,
            isGatewayBusy = isGatewayBusy,
            onDismiss = { viewModel.closeOrderDetail() },
            onSubmitTxRef = { ref -> viewModel.submitTxRef(liveSelectedOrder.id, ref) },
            onInitiateOnlinePayment = { viewModel.initiateOnlineGatewayPayment(liveSelectedOrder) },
            onVerifyOnlinePayment = { viewModel.verifyOnlinePaymentStatus(liveSelectedOrder) },
            onMarkExpired = { viewModel.markOrderExpired(liveSelectedOrder.id) },
        )
    }

    val liveSelectedStoreOrder = remember(selectedStoreOrder, userStoreOrdersState) {
        val current = selectedStoreOrder ?: return@remember null
        val list = (userStoreOrdersState as? UiState.Success)?.data
        list?.firstOrNull { it.id == current.id } ?: current
    }

    if (liveSelectedStoreOrder != null) {
        StoreOrderDetailSheet(
            order = liveSelectedStoreOrder,
            exchangeConfig = config,
            isGatewayBusy = isStoreGatewayBusy,
            onDismiss = storeViewModel::closeStoreOrder,
            onInitiateOnlinePayment = { storeViewModel.initiateStoreOnlinePayment(liveSelectedStoreOrder, config) },
            onCheckOnlineStatus = { storeViewModel.checkStoreOnlinePaymentStatus(liveSelectedStoreOrder, config) },
            onSubmitTxRef = { ref -> storeViewModel.submitStoreTxRef(liveSelectedStoreOrder.id, ref) },
            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
        )
    }

    val bottomTabs = remember {
        listOf(
            NavTab(Routes.EXCHANGE, "Échanger", Icons.Rounded.CurrencyExchange, Icons.Outlined.CurrencyExchange),
            NavTab(Routes.DEPOSIT, "Dépôt", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
            NavTab(Routes.INVEST, "Investir", Icons.Rounded.AddCircle, Icons.Outlined.AddCircle),
            NavTab(Routes.STORE, "Store", Icons.Rounded.Storefront, Icons.Outlined.Storefront),
            NavTab(Routes.ORDERS, "Ordres", Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
        )
    }

    val railTabs = remember(viewModel.isAdmin) {
        buildList {
            addAll(bottomTabs)
            if (viewModel.isAdmin) {
                add(NavTab(Routes.ADMIN, "Admin", Icons.Rounded.AdminPanelSettings, Icons.Outlined.AdminPanelSettings))
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.EXCHANGE

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    activeDrawerSheet?.let { destination ->
        DrawerFeatureModalSheet(
            destination = destination,
            notifications = notifications,
            walletSummary = walletSummary,
            isBalanceHidden = isBalanceHidden,
            selectedLanguageCode = selectedLanguageCode,
            onDismiss = { activeDrawerSheet = null },
            onDeleteNotification = viewModel::deleteNotification,
            onClearAllNotifications = viewModel::clearAllNotifications,
            onOpenOrderById = viewModel::openOrderById,
            onToggleBalanceHidden = viewModel::toggleBalanceHidden,
            onStartDeposit = {
                activeDrawerSheet = null
                viewModel.startDepositPreset()
                navigateToTab(Routes.DEPOSIT)
            },
            onStartDepositMethod = { method ->
                activeDrawerSheet = null
                viewModel.selectDepositGatewayPreset(method)
                navigateToTab(Routes.DEPOSIT)
            },
            onStartWithdraw = {
                activeDrawerSheet = null
                viewModel.startWithdrawPreset()
                navigateToTab(Routes.EXCHANGE)
            },
            onSelectLanguage = viewModel::selectLanguage,
            onOpenSupport = { activeDrawerSheet = DrawerDestination.SUPPORT },
            onShowMessage = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            AleviniDrawerSheet(
                userName = userProfile?.displayName?.ifBlank { viewModel.currentUserName } ?: viewModel.currentUserName,
                userEmail = viewModel.currentUserEmail,
                isAdmin = viewModel.isAdmin,
                walletSummary = walletSummary,
                isBalanceHidden = isBalanceHidden,
                isDataSaverEnabled = isDataSaverEnabled,
                notificationCount = notifications.size,
                selectedLanguage = LanguageCatalog.findByCode(selectedLanguageCode),
                onToggleBalanceHidden = viewModel::toggleBalanceHidden,
                onToggleDataSaver = viewModel::setDataSaverEnabled,
                onQuickDeposit = {
                    scope.launch { drawerState.close() }
                    viewModel.startDepositPreset()
                    navigateToTab(Routes.DEPOSIT)
                },
                onQuickWithdraw = {
                    scope.launch { drawerState.close() }
                    viewModel.startWithdrawPreset()
                    navigateToTab(Routes.EXCHANGE)
                },
                onSelectDestination = { destination ->
                    scope.launch { drawerState.close() }
                    when (destination) {
                        DrawerDestination.STORE -> navigateToTab(Routes.STORE)
                        DrawerDestination.ADMIN -> {
                            if (viewModel.isAdmin) {
                                adminInitialTab = 0
                                navigateToTab(Routes.ADMIN)
                            }
                        }
                        else -> activeDrawerSheet = destination
                    }
                },
            )
        },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = ObsidianBg,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = ObsidianSurface,
                                titleContentColor = TextPrimary,
                            ),
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("drawer_menu_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Menu,
                                        contentDescription = "Ouvrir le menu latéral",
                                        tint = TextPrimary,
                                    )
                                }
                            },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.alevini_icon_fg_1794757900851),
                                        contentDescription = "Logo Alevini",
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ALEVINI",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = ForestGreen,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { activeDrawerSheet = DrawerDestination.NOTIFICATIONS },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("topbar_notifications_button"),
                                ) {
                                    Box {
                                        Icon(
                                            imageVector = Icons.Outlined.Notifications,
                                            contentDescription = "Notifications",
                                            tint = TextPrimary,
                                        )
                                        if (notifications.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .align(Alignment.TopEnd)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(EmeraldPrimary)
                                            )
                                        }
                                    }
                                }
                                val isSecuritySelected = currentRoute == Routes.SECURITY
                                IconButton(
                                    onClick = { navigateToTab(Routes.SECURITY) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("topbar_security_button"),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSecuritySelected) EmeraldContainer else Color.Transparent),
                                    ) {
                                        Icon(
                                            imageVector = if (isSecuritySelected) Icons.Rounded.AccountCircle else Icons.Outlined.AccountCircle,
                                            contentDescription = "Profil et sécurité",
                                            tint = if (isSecuritySelected) ForestGreen else TextPrimary,
                                        )
                                    }
                                }
                                if (viewModel.isAdmin) {
                                    Surface(
                                        color = if (currentRoute == Routes.ADMIN) GoldAccent else ObsidianSurfaceElevated,
                                        shape = RoundedCornerShape(50),
                                        onClick = {
                                            adminInitialTab = 0
                                            navigateToTab(Routes.ADMIN)
                                        },
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .minimumInteractiveComponentSize()
                                            .testTag("topbar_admin_button"),
                                    ) {
                                        Text(
                                            text = "ADMIN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (currentRoute == Routes.ADMIN) Color.White else GoldAccent,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            },
                        )
                        HorizontalDivider(thickness = 0.75.dp, color = ObsidianOutline)
                    }
                },
            bottomBar = {
                if (!useRail) {
                    Column {
                        HorizontalDivider(thickness = 0.75.dp, color = ObsidianOutline)
                        NavigationBar(
                            containerColor = ObsidianSurface,
                            contentColor = TextPrimary,
                            tonalElevation = 0.dp,
                        ) {
                            bottomTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ForestGreen,
                                        selectedTextColor = ForestGreen,
                                        indicatorColor = EmeraldContainer,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        railTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ForestGreen,
                                    selectedTextColor = ForestGreen,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.EXCHANGE,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    composable(Routes.EXCHANGE) {
                        ExchangeScreen(
                            config = config,
                            userProfile = userProfile,
                            calcState = calcState,
                            quote = quote,
                            onSelectSendMethod = viewModel::selectSendMethod,
                            onSelectReceiveMethod = viewModel::selectReceiveMethod,
                            onSwapMethods = viewModel::swapMethods,
                            onSetSendCurrency = viewModel::setSendCurrency,
                            onSetReceiveCurrency = viewModel::setReceiveCurrency,
                            onSetCryptoCoin = viewModel::setCryptoCoin,
                            onSetSendAmount = viewModel::setSendAmountText,
                            onSetDestAccount = viewModel::setDestAccount,
                            onCreateOrder = { viewModel.createOrder() },
                            onNavigateToTopUp = { method ->
                                viewModel.selectDepositGatewayPreset(method)
                                navigateToTab(Routes.DEPOSIT)
                            },
                        )
                    }
                    composable(Routes.DEPOSIT) {
                        DepositWizardScreen(
                            config = config,
                            userProfile = userProfile,
                            walletSummary = walletSummary,
                            preselectedMethod = preselectedDepositMethod,
                            isBalanceHidden = isBalanceHidden,
                            ordersState = ordersState,
                            dismissedOrderIds = dismissedOrderIds,
                            isGatewayBusy = isGatewayBusy,
                            isSubmittingOrder = calcState.isSubmittingOrder,
                            onConsumePreselectedMethod = viewModel::clearPreselectedDepositMethod,
                            onToggleBalanceHidden = viewModel::toggleBalanceHidden,
                            onCreateTopUpOrder = viewModel::createWizardTopUpOrder,
                            onInitiateOnlinePayment = viewModel::initiateOnlineGatewayPayment,
                            onVerifyOnlinePayment = viewModel::verifyOnlinePaymentStatus,
                            onSubmitTxRef = viewModel::submitTxRef,
                            onOrderClick = viewModel::openOrderDetail,
                            onDeleteOrder = viewModel::deleteOrHideUserOrder,
                            onStartWithdraw = { method ->
                                viewModel.startWithdrawPreset(method)
                                navigateToTab(Routes.EXCHANGE)
                            },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                    }
                    composable(Routes.INVEST) {
                        InvestScreen(
                            userProfile = userProfile,
                            investmentsState = userInvestmentsState,
                            dismissedInvestmentIds = dismissedInvestmentIds,
                            activeTab = investActiveTab,
                            selectedCurrency = investSelectedCurrency,
                            selectedAmount = investSelectedAmount,
                            selectedDuration = investSelectedDuration,
                            selectedSector = investSelectedSector,
                            selectedGateway = investSelectedGateway,
                            isSubmitting = isInvestSubmitting,
                            onSelectTab = investViewModel::selectTab,
                            onSelectCurrency = investViewModel::selectCurrency,
                            onSelectAmount = investViewModel::selectAmount,
                            onSelectDuration = investViewModel::selectDuration,
                            onSelectSector = investViewModel::selectSector,
                            onSelectGateway = investViewModel::selectGateway,
                            onValidateInvestment = investViewModel::submitInvestment,
                            onDeleteOrHideInvestment = investViewModel::deleteOrHideInvestment,
                            onNavigateToTopUp = { method ->
                                viewModel.selectDepositGatewayPreset(method)
                                navigateToTab(Routes.DEPOSIT)
                            },
                        )
                    }
                    composable(Routes.STORE) {
                        StoreScreen(
                            storeConfig = storeConfig,
                            exchangeConfig = config,
                            userProfile = userProfile,
                            catalogProducts = catalogProducts,
                            ordersState = userStoreOrdersState,
                            dismissedOrderIds = dismissedStoreOrderIds,
                            selectedProductId = selectedStoreProductId,
                            selectedGateway = selectedStoreGateway,
                            selectedOptionInput = storeSelectedOption,
                            targetAccountInput = storeTargetInput,
                            isSubmitting = isStoreSubmitting,
                            isAdmin = viewModel.isAdmin,
                            onSelectCategoryDefault = storeViewModel::selectCategoryDefault,
                            onSelectProduct = storeViewModel::selectProduct,
                            onSelectGateway = storeViewModel::selectGateway,
                            onSelectedOptionChange = storeViewModel::setSelectedOptionInput,
                            onTargetAccountChange = storeViewModel::setTargetAccountInput,
                            onPlaceOrder = { storeViewModel.createStoreOrder() },
                            onNavigateToTopUp = { method ->
                                viewModel.selectDepositGatewayPreset(method)
                                navigateToTab(Routes.DEPOSIT)
                            },
                            onOpenOrder = storeViewModel::openStoreOrder,
                            onDeleteOrHideOrder = storeViewModel::deleteOrHideStoreOrder,
                            onOpenAdminStoreControl = {
                                adminInitialTab = 1
                                navigateToTab(Routes.ADMIN)
                            },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                    }
                    composable(Routes.ORDERS) {
                        OrdersScreen(
                            ordersState = ordersState,
                            dismissedOrderIds = dismissedOrderIds,
                            onOrderClick = viewModel::openOrderDetail,
                            onDeleteOrder = viewModel::deleteOrHideUserOrder,
                            onDeleteCompletedOrders = viewModel::deleteCompletedUserOrders,
                        )
                    }
                    composable(Routes.SECURITY) {
                        val pinSecurityState by viewModel.pinSecurityState.collectAsStateWithLifecycle()
                        SecurityAndProfileScreen(
                            userEmail = viewModel.currentUserEmail,
                            userName = viewModel.currentUserName,
                            isAdmin = viewModel.isAdmin,
                            userProfile = userProfile,
                            isBiometricEnabled = isBiometricEnabled,
                            isDataSaverEnabled = isDataSaverEnabled,
                            pinSecurityState = pinSecurityState,
                            onUpdateProfile = viewModel::updateProfile,
                            onToggleBiometric = viewModel::setBiometricLock,
                            onToggleDataSaver = viewModel::setDataSaverEnabled,
                            onTogglePinLock = viewModel::setPinLockEnabled,
                            onUpdatePinCode = viewModel::updatePinCode,
                            onUpdateRecoveryEmail = viewModel::updateRecoveryEmail,
                            onLockAppNow = onLockAppNow,
                            onSignOut = onSignOut,
                        )
                    }
                    if (viewModel.isAdmin) {
                        composable(Routes.ADMIN) {
                            val adminOrdersState by viewModel.adminOrdersState.collectAsStateWithLifecycle()
                            val adminUsersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
                            val adminLedgerState by viewModel.adminLedgerState.collectAsStateWithLifecycle()
                            val adminNotificationsState by viewModel.adminNotificationsState.collectAsStateWithLifecycle()
                            val adminPaymentSessionsState by viewModel.adminPaymentSessionsState.collectAsStateWithLifecycle()
                            val adminWebhookEventsState by viewModel.adminWebhookEventsState.collectAsStateWithLifecycle()
                            val adminStoreOrdersState by storeViewModel.adminStoreOrdersState.collectAsStateWithLifecycle()
                            val netflixInventoryState by storeViewModel.netflixInventoryState.collectAsStateWithLifecycle()
                            val adminInvestmentsState by investViewModel.adminInvestmentsState.collectAsStateWithLifecycle()
                            AdminScreen(
                                config = config,
                                ordersState = adminOrdersState,
                                usersState = adminUsersState,
                                ledgerState = adminLedgerState,
                                adminNotificationsState = adminNotificationsState,
                                paymentSessionsState = adminPaymentSessionsState,
                                webhookEventsState = adminWebhookEventsState,
                                storeConfig = storeConfig,
                                catalogProducts = catalogProducts,
                                storeOrdersState = adminStoreOrdersState,
                                netflixInventoryState = netflixInventoryState,
                                investmentsState = adminInvestmentsState,
                                storeProvidersHealth = storeProvidersHealth,
                                isStoreGatewayBusy = isStoreGatewayBusy,
                                initialTab = adminInitialTab,
                                checkedSecurityItems = checkedSecurityItems,
                                onSaveConfig = viewModel::adminSaveConfig,
                                onCompleteOrder = viewModel::adminCompleteOrder,
                                onUpdateOrderStatus = viewModel::adminUpdateOrderStatus,
                                onDeleteOrder = viewModel::adminDeleteOrder,
                                onToggleUserBlocked = viewModel::adminToggleUserBlocked,
                                onAdjustGatewayBalance = viewModel::adminAdjustGatewayBalance,
                                onDeleteUser = viewModel::adminDeleteUserProfile,
                                onDeleteLedgerEntry = viewModel::adminDeleteLedgerEntry,
                                onSendNotification = viewModel::adminSendNotification,
                                onDeleteNotification = viewModel::adminDeleteNotification,
                                onDeletePaymentSession = viewModel::adminDeletePaymentSession,
                                onDeleteWebhookEvent = viewModel::adminDeleteWebhookEvent,
                                onSaveStoreConfig = { storeViewModel.adminSaveStoreConfig(it) },
                                onSetFulfillmentMode = storeViewModel::adminSetFulfillmentMode,
                                onTopUpMeruUsd = { storeViewModel.adminTopUpMeruUsd() },
                                onTransferMeruToReloadly = { storeViewModel.adminTransferMeruToReloadly() },
                                onTestAllStoreApis = { storeViewModel.adminTestAllProviders(config) },
                                onFulfillStarlinkViaMeru = storeViewModel::adminFulfillStarlinkViaMeru,
                                onAssignNetflixAndDeliver = storeViewModel::adminAssignNetflixAndDeliver,
                                onDeliverManualStoreCode = storeViewModel::adminDeliverStoreOrder,
                                onRetryStoreOrderApi = storeViewModel::adminRetryStoreOrderApi,
                                onRejectStoreOrder = storeViewModel::adminRejectStoreOrder,
                                onDeleteStoreOrder = storeViewModel::adminDeleteStoreOrder,
                                onAddStoreProduct = storeViewModel::adminAddStoreProduct,
                                onDeleteStoreProduct = storeViewModel::adminDeleteStoreProduct,
                                onAddNetflixProfile = storeViewModel::adminAddNetflixProfile,
                                onDeleteNetflixProfile = storeViewModel::adminDeleteNetflixProfile,
                                onApproveInvestment = { inv -> investViewModel.adminApproveInvestment(inv) },
                                onRejectInvestment = { inv -> investViewModel.adminRejectInvestment(inv) },
                                onDeleteInvestment = investViewModel::adminDeleteInvestment,
                                onToggleSecurityItem = viewModel::toggleSecurityItem,
                                onResetSecurityChecklist = viewModel::resetSecurityChecklist,
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
private fun BiometricLockGate(
    errorMessage: String?,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(68.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Alevini est verrouillé",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage ?: "Authentifiez-vous pour accéder à vos échanges.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = EmeraldOnPrimary),
                modifier = Modifier.heightIn(min = 50.dp),
            ) {
                Text(
                    text = "Déverrouiller",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSignOut) {
                Text("Se déconnecter", color = TextSecondary)
            }
        }
    }
}
