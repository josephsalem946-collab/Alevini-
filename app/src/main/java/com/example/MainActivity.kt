package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.data.repository.AleviniRepository
import com.example.data.repository.SecurityPreferencesRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.BiometricUnlockHelper
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.AleviniDrawerSheet
import com.example.ui.components.DrawerDestination
import com.example.ui.components.DrawerFeatureModalSheet
import com.example.ui.components.OrderDetailBottomSheet
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.ExchangeScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.SecurityAndProfileScreen
import com.example.ui.theme.AleviniTheme
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AleviniViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private object Routes {
    const val EXCHANGE = "exchange"
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
        enableEdgeToEdge()
        setContent {
            AleviniTheme {
                AppNavigation()
            }
        }
    }
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

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var autoSignInAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!autoSignInAttempted && auth.currentUser == null) {
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

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            val viewModel: AleviniViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
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
            AuthenticatedAleviniShell(
                viewModel = viewModel,
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
    val adminOrdersState by viewModel.adminOrdersState.collectAsStateWithLifecycle()
    val adminUsersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val adminLedgerState by viewModel.adminLedgerState.collectAsStateWithLifecycle()
    val adminNotificationsState by viewModel.adminNotificationsState.collectAsStateWithLifecycle()
    val calcState by viewModel.calculatorState.collectAsStateWithLifecycle()
    val quote by viewModel.computedQuote.collectAsStateWithLifecycle()
    val selectedOrder by viewModel.selectedOrder.collectAsStateWithLifecycle()
    val checkedSecurityItems by viewModel.checkedSecurityItems.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val selectedLanguageCode by viewModel.selectedLanguageCode.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val walletSummary by viewModel.walletSummary.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
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
            onDismiss = { viewModel.closeOrderDetail() },
            onSubmitTxRef = { ref -> viewModel.submitTxRef(liveSelectedOrder.id, ref) },
            onMarkExpired = { viewModel.markOrderExpired(liveSelectedOrder.id) },
        )
    }

    val tabs = remember(viewModel.isAdmin) {
        buildList {
            add(NavTab(Routes.EXCHANGE, "Échanger", Icons.Rounded.CurrencyExchange, Icons.Outlined.CurrencyExchange))
            add(NavTab(Routes.ORDERS, "Commandes", Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong))
            add(NavTab(Routes.SECURITY, "Sécurité", Icons.Rounded.Security, Icons.Outlined.Security))
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
                navigateToTab(Routes.EXCHANGE)
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            AleviniDrawerSheet(
                userName = userProfile?.displayName?.ifBlank { viewModel.currentUserName } ?: viewModel.currentUserName,
                userEmail = viewModel.currentUserEmail,
                walletSummary = walletSummary,
                isBalanceHidden = isBalanceHidden,
                notificationCount = notifications.size,
                selectedLanguage = LanguageCatalog.findByCode(selectedLanguageCode),
                onToggleBalanceHidden = viewModel::toggleBalanceHidden,
                onQuickDeposit = {
                    scope.launch { drawerState.close() }
                    viewModel.startDepositPreset()
                    navigateToTab(Routes.EXCHANGE)
                },
                onQuickWithdraw = {
                    scope.launch { drawerState.close() }
                    viewModel.startWithdrawPreset()
                    navigateToTab(Routes.EXCHANGE)
                },
                onSelectDestination = { destination ->
                    scope.launch { drawerState.close() }
                    activeDrawerSheet = destination
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
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ALEVINI",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
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
                                        imageVector = Icons.Rounded.Notifications,
                                        contentDescription = "Notifications",
                                        tint = TextPrimary,
                                    )
                                    if (notifications.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .align(Alignment.TopEnd)
                                                .clip(RoundedCornerShape(50))
                                                .background(EmeraldPrimary)
                                        )
                                    }
                                }
                            }
                            if (viewModel.isAdmin) {
                                Surface(
                                    color = ObsidianSurfaceElevated,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.padding(end = 12.dp),
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        },
                    )
                },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                    ) {
                        tabs.forEach { tab ->
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
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                            )
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
                        tabs.forEach { tab ->
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
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
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
                        )
                    }
                    composable(Routes.ORDERS) {
                        OrdersScreen(
                            ordersState = ordersState,
                            onOrderClick = viewModel::openOrderDetail,
                        )
                    }
                    composable(Routes.SECURITY) {
                        SecurityAndProfileScreen(
                            userEmail = viewModel.currentUserEmail,
                            userName = viewModel.currentUserName,
                            isAdmin = viewModel.isAdmin,
                            userProfile = userProfile,
                            isBiometricEnabled = isBiometricEnabled,
                            onUpdateProfile = viewModel::updateProfile,
                            onToggleBiometric = viewModel::setBiometricLock,
                            onSignOut = onSignOut,
                        )
                    }
                    if (viewModel.isAdmin) {
                        composable(Routes.ADMIN) {
                            AdminScreen(
                                config = config,
                                ordersState = adminOrdersState,
                                usersState = adminUsersState,
                                ledgerState = adminLedgerState,
                                adminNotificationsState = adminNotificationsState,
                                checkedSecurityItems = checkedSecurityItems,
                                onSaveConfig = viewModel::adminSaveConfig,
                                onCompleteOrder = viewModel::adminCompleteOrder,
                                onUpdateOrderStatus = viewModel::adminUpdateOrderStatus,
                                onDeleteOrder = viewModel::adminDeleteOrder,
                                onToggleUserBlocked = viewModel::adminToggleUserBlocked,
                                onDeleteUser = viewModel::adminDeleteUserProfile,
                                onDeleteLedgerEntry = viewModel::adminDeleteLedgerEntry,
                                onSendNotification = viewModel::adminSendNotification,
                                onDeleteNotification = viewModel::adminDeleteNotification,
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
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage ?: "Authentifiez-vous pour accéder à vos échanges.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBg),
                modifier = Modifier.height(50.dp),
            ) {
                Text("Déverrouiller", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSignOut) {
                Text("Se déconnecter", color = TextSecondary)
            }
        }
    }
}
