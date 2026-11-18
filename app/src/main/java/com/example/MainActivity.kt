package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.StaffMemberEntity
import com.example.ui.components.ConnectivityStatusPill
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ReportsAndTeamScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.StaffPinLockScreen
import com.example.ui.screens.StockGraphsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.VaultSetupScreen
import com.example.ui.state.AuthSessionState
import com.example.ui.state.ScannedBarcodeRequest
import com.example.ui.state.ScannedItemState
import com.example.ui.state.StockPulseUiState
import com.example.ui.state.StockPulseViewModel
import com.example.ui.theme.StockPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockPulseTheme {
                StockPulseRoot()
            }
        }
    }
}

private data class NavDestinationItem(
    val route: AppRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopDestinations = listOf(
    NavDestinationItem(AppRoute.Dashboard, "Home", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    NavDestinationItem(AppRoute.Tasks, "Tasks", Icons.Rounded.TaskAlt, Icons.Outlined.TaskAlt, "nav_tasks"),
    NavDestinationItem(AppRoute.Inventory, "Products", Icons.Rounded.Inventory2, Icons.Outlined.Inventory2, "nav_inventory"),
    NavDestinationItem(AppRoute.Scanner, "Scan", Icons.Rounded.QrCodeScanner, Icons.Outlined.QrCodeScanner, "nav_scanner"),
    NavDestinationItem(AppRoute.ReportsAndTeam, "Reports", Icons.Rounded.Assessment, Icons.Outlined.Assessment, "nav_audits")
)

@Composable
fun StockPulseRoot() {
    val context = LocalContext.current
    val viewModel: StockPulseViewModel = viewModel(
        factory = remember(context) { StockPulseViewModel.Factory(context) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val scannedItemState by viewModel.scannedItemState.collectAsStateWithLifecycle()
    val recentScans by viewModel.recentScans.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    AnimatedContent(
        targetState = state.authState,
        contentKey = {
            when (it) {
                AuthSessionState.Loading -> "loading"
                AuthSessionState.NeedsSetup -> "setup"
                is AuthSessionState.Locked -> "locked"
                is AuthSessionState.Authenticated -> "auth_${it.activeStaff.id}"
            }
        },
        transitionSpec = {
            fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "auth_state_transition"
    ) { auth ->
        when (auth) {
            AuthSessionState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            AuthSessionState.NeedsSetup -> {
                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        VaultSetupScreen(
                            authError = authError,
                            onClearError = viewModel::clearAuthError,
                            onInitializeVault = viewModel::initializeVault
                        )
                    }
                }
            }

            is AuthSessionState.Locked -> {
                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        StaffPinLockScreen(
                            storeName = state.storeConfig.storeName,
                            staffList = auth.staff,
                            authError = authError,
                            onClearError = viewModel::clearAuthError,
                            onUnlock = viewModel::signInWithPin
                        )
                    }
                }
            }

            is AuthSessionState.Authenticated -> {
                AuthenticatedStoreShell(
                    state = state,
                    activeStaff = auth.activeStaff,
                    scannedItemState = scannedItemState,
                    recentScans = recentScans,
                    snackbarHostState = snackbarHostState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedStoreShell(
    state: StockPulseUiState,
    activeStaff: StaffMemberEntity,
    scannedItemState: ScannedItemState?,
    recentScans: List<ScannedBarcodeRequest>,
    snackbarHostState: SnackbarHostState,
    viewModel: StockPulseViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isStockGraphs = currentDestination?.hasRoute(AppRoute.StockGraphs::class) == true

    fun navigateTo(route: AppRoute) {
        navController.popBackStack(AppRoute.StockGraphs, inclusive = true)
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (isStockGraphs) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("stock_graphs_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = if (isStockGraphs) "Stock & Profit Graphs" else "Store Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        ConnectivityStatusPill(
                            isOnline = state.isOnline,
                            pendingCount = state.pendingSyncCount,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        IconButton(
                            onClick = { viewModel.lockVault() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("topbar_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = stringResource(R.string.cd_lock_vault)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    NavTabIcon(
                                        dest = dest,
                                        selected = selected,
                                        alertCount = state.activeAlertCount
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    NavTabIcon(
                                        dest = dest,
                                        selected = selected,
                                        alertCount = state.activeAlertCount
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoute.Dashboard,
                        enterTransition = { fadeIn(animationSpec = tween(220)) },
                        exitTransition = { fadeOut(animationSpec = tween(180)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                        popExitTransition = { fadeOut(animationSpec = tween(180)) },
                        modifier = Modifier
                            .widthIn(max = 960.dp)
                            .fillMaxSize()
                    ) {
                        composable<AppRoute.Dashboard> {
                            DashboardScreen(
                                state = state,
                                activeStaff = activeStaff,
                                onOpenScanner = { navigateTo(AppRoute.Scanner) },
                                onOpenTasks = { navigateTo(AppRoute.Tasks) },
                                onOpenInventory = { navigateTo(AppRoute.Inventory) },
                                onOpenReports = { navigateTo(AppRoute.ReportsAndTeam) },
                                onOpenStockGraphs = {
                                    navController.navigate(AppRoute.StockGraphs) {
                                        launchSingleTop = true
                                    }
                                },
                                onAcknowledgeAlert = viewModel::acknowledgeAlert
                            )
                        }

                        composable<AppRoute.Tasks> {
                            TasksScreen(
                                tasks = state.tasks,
                                staffList = state.staff,
                                activeStaff = activeStaff,
                                onCycleStatus = viewModel::cycleTaskStatus,
                                onSaveTask = viewModel::saveTask,
                                onDeleteTask = viewModel::deleteTask
                            )
                        }

                        composable<AppRoute.Inventory> {
                            InventoryScreen(
                                products = state.products,
                                currencySymbol = state.storeConfig.currencySymbol,
                                activeStaff = activeStaff,
                                onOpenScanner = { navigateTo(AppRoute.Scanner) },
                                onOpenStockGraphs = {
                                    navController.navigate(AppRoute.StockGraphs) {
                                        launchSingleTop = true
                                    }
                                },
                                onAdjustStock = viewModel::adjustStock,
                                onSaveProduct = viewModel::saveProduct,
                                onDeleteProduct = viewModel::deleteProduct
                            )
                        }

                        composable<AppRoute.StockGraphs>(
                            enterTransition = {
                                slideInHorizontally(initialOffsetX = { it / 6 }, animationSpec = tween(240)) +
                                    fadeIn(animationSpec = tween(220))
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { it / 6 }, animationSpec = tween(200)) +
                                    fadeOut(animationSpec = tween(180))
                            }
                        ) {
                            StockGraphsScreen(
                                state = state,
                                activeStaff = activeStaff
                            )
                        }

                        composable<AppRoute.Scanner> {
                            ScannerScreen(
                                currencySymbol = state.storeConfig.currencySymbol,
                                activeStaff = activeStaff,
                                scannedItemState = scannedItemState,
                                recentScans = recentScans,
                                onBarcodeDetected = viewModel::onBarcodeDetected,
                                onClearScanned = viewModel::clearScannedBarcode,
                                onAdjustStock = viewModel::adjustStock,
                                onSaveNewProduct = viewModel::saveProduct
                            )
                        }

                        composable<AppRoute.ReportsAndTeam> {
                            ReportsAndTeamScreen(
                                state = state,
                                activeStaff = activeStaff,
                                onSharePdf = viewModel::generateShareablePdf,
                                onSavePdfToUri = viewModel::savePdfToUri,
                                onSaveBackupToUri = viewModel::saveEncryptedBackupToUri,
                                onCommitOfflineQueue = viewModel::commitOfflineQueue,
                                onAddStaff = viewModel::addStaff,
                                onToggleStaffActive = viewModel::toggleStaffActive,
                                onUpdateAutomation = viewModel::updateAutomation,
                                onUpdateTaxId = viewModel::updateTaxId,
                                onUpdateCurrency = viewModel::updateCurrency,
                                onLockSession = viewModel::lockVault
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTabIcon(
    dest: NavDestinationItem,
    selected: Boolean,
    alertCount: Int
) {
    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
    if (dest.route == AppRoute.Inventory && alertCount > 0) {
        BadgedBox(
            badge = {
                Badge { Text("$alertCount") }
            }
        ) {
            Icon(icon, contentDescription = dest.label)
        }
    } else {
        Icon(icon, contentDescription = dest.label)
    }
}
