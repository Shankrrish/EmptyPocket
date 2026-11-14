package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.CardSummary
import com.example.ui.FinanceViewModel
import com.example.ui.components.CenteredBottomNavBar
import com.example.ui.components.CenteredNavTabItem
import com.example.ui.components.CenteredQuickAddTray
import com.example.ui.components.ExpandableGradientFab
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AutoCaptureScreen
import com.example.ui.screens.CreditCardDetailSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HomeCustomizationSheet
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.NetWorthScreen
import com.example.ui.screens.PlanningScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.PrivacySettingsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.FinFlowTheme

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_TRANSACTIONS = "transactions"
const val ROUTE_ACCOUNTS = "accounts"
const val ROUTE_PLANNING = "planning"
const val ROUTE_PORTFOLIO = "portfolio"
const val ROUTE_REPORTS = "reports"
const val ROUTE_MORE = "more"
const val ROUTE_NET_WORTH = "net_worth"
const val ROUTE_RULES = "rules"
const val ROUTE_CAPTURE = "auto_capture"
const val ROUTE_PRIVACY = "privacy"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: FinanceViewModel = viewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            FinFlowTheme(
                themeMode = state.settings.resolvedThemeMode(),
                fontStyle = state.settings.resolvedFontStyle()
            ) {
                FinFlowApp(vm = vm)
            }
        }
    }
}

@Composable
fun FinFlowApp(vm: FinanceViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val sessionUnlocked by vm.sessionUnlocked.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: ROUTE_DASHBOARD

    var fabExpanded by remember { mutableStateOf(false) }
    var sheetVisible by remember { mutableStateOf(false) }
    var sheetInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var preselectedCardId by remember { mutableStateOf<Long?>(null) }
    var preselectedAccountId by remember { mutableStateOf<Long?>(null) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var homeCustomSheetVisible by remember { mutableStateOf(false) }
    var selectedCardForDetails by remember { mutableStateOf<CardSummary?>(null) }

    val mustLock = state.settings.appLockEnabled && state.settings.pinHash.isNotEmpty() && !sessionUnlocked
    if (mustLock) {
        AppLockScreen(
            biometricEnabled = state.settings.biometricEnabled,
            onVerifyPin = vm::verifyPinAndUnlock,
            onBiometricSuccess = vm::markBiometricUnlocked
        )
        return
    }

    val compactTabs = listOf(
        NavTab(ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavTab(ROUTE_TRANSACTIONS, R.string.nav_transactions, Icons.AutoMirrored.Filled.FormatListBulleted, Icons.AutoMirrored.Outlined.FormatListBulleted),
        NavTab(ROUTE_PLANNING, R.string.nav_planning, Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
        NavTab(ROUTE_MORE, R.string.nav_more, Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
    )
    val railTabs = listOf(
        NavTab(ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavTab(ROUTE_TRANSACTIONS, R.string.nav_transactions, Icons.AutoMirrored.Filled.FormatListBulleted, Icons.AutoMirrored.Outlined.FormatListBulleted),
        NavTab(ROUTE_PLANNING, R.string.nav_planning, Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
        NavTab(ROUTE_ACCOUNTS, R.string.nav_accounts, Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
        NavTab(ROUTE_PORTFOLIO, R.string.nav_portfolio, Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp),
        NavTab(ROUTE_MORE, R.string.nav_more, Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
    )
    val isPrimaryTab = compactTabs.any { it.route == currentRoute }

    val navigateToTab: (String) -> Unit = { route ->
        fabExpanded = false
        navController.navigate(route) {
            popUpTo(ROUTE_DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openTransactionSheet: (TransactionType, Long?, Long?) -> Unit = { type, cardId, accountId ->
        fabExpanded = false
        editingTx = null
        preselectedCardId = cardId
        preselectedAccountId = accountId
        sheetInitialType = type
        sheetVisible = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                if (useRail && (isPrimaryTab || currentRoute == ROUTE_ACCOUNTS || currentRoute == ROUTE_PORTFOLIO)) {
                    ExpandableGradientFab(
                        expanded = fabExpanded,
                        onToggle = { fabExpanded = !fabExpanded },
                        onSelectType = { type -> openTransactionSheet(type, null, null) }
                    )
                }
            },
            bottomBar = {
                if (!useRail && isPrimaryTab) {
                    val navTabItems = compactTabs.map {
                        CenteredNavTabItem(
                            route = it.route,
                            label = stringResource(it.labelRes),
                            selectedIcon = it.selectedIcon,
                            unselectedIcon = it.unselectedIcon
                        )
                    }
                    CenteredBottomNavBar(
                        tabs = navTabItems,
                        currentRoute = currentRoute,
                        fabExpanded = fabExpanded,
                        onSelectTab = navigateToTab,
                        onToggleCenterAdd = { fabExpanded = !fabExpanded }
                    )
                }
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (useRail && (isPrimaryTab || currentRoute == ROUTE_ACCOUNTS || currentRoute == ROUTE_PORTFOLIO)) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        railTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DASHBOARD,
                        modifier = Modifier.widthIn(max = 760.dp)
                    ) {
                        composable(ROUTE_DASHBOARD) {
                            DashboardScreen(
                                state = state,
                                onPreviousMonth = vm::previousMonth,
                                onNextMonth = vm::nextMonth,
                                onToggleVisibility = vm::toggleBalanceVisibility,
                                onNavigateToPlanning = { navigateToTab(ROUTE_PLANNING) },
                                onNavigateToTransactions = {
                                    vm.clearContextualFilters()
                                    navigateToTab(ROUTE_TRANSACTIONS)
                                },
                                onInspectAccount = { accId ->
                                    vm.filterTransactionsByAccount(accId)
                                    navigateToTab(ROUTE_TRANSACTIONS)
                                },
                                onQuickAddForAccount = { accId ->
                                    openTransactionSheet(TransactionType.EXPENSE, null, accId)
                                },
                                onOpenCardDetails = { summary -> selectedCardForDetails = summary },
                                onPayCreditCard = { cardId ->
                                    openTransactionSheet(TransactionType.CC_PAYMENT, cardId, null)
                                },
                                onUpdateSavingsTarget = vm::updateSavingsTarget,
                                onOpenHomeCustomization = { homeCustomSheetVisible = true },
                                onOpenPrivacySettings = { navController.navigate(ROUTE_PRIVACY) },
                                onOpenAccounts = { navController.navigate(ROUTE_ACCOUNTS) }
                            )
                        }
                        composable(ROUTE_TRANSACTIONS) {
                            TransactionsScreen(
                                state = state,
                                onPreviousMonth = vm::previousMonth,
                                onNextMonth = vm::nextMonth,
                                onToggleVisibility = vm::toggleBalanceVisibility,
                                onSearchChange = vm::setSearchQuery,
                                onFilterChange = vm::setTxFilter,
                                onClearContextFilters = vm::clearContextualFilters,
                                onTogglePaid = vm::togglePaid,
                                onEditTransaction = { tx ->
                                    fabExpanded = false
                                    editingTx = tx
                                    preselectedCardId = tx.creditCardId
                                    preselectedAccountId = tx.accountId
                                    sheetInitialType = tx.type
                                    sheetVisible = true
                                },
                                onOpenAutoCapture = { navController.navigate(ROUTE_CAPTURE) },
                                onOpenRules = { navController.navigate(ROUTE_RULES) }
                            )
                        }
                        composable(ROUTE_ACCOUNTS) {
                            AccountsScreen(
                                state = state,
                                onInspectAccount = { accId ->
                                    vm.filterTransactionsByAccount(accId)
                                    navigateToTab(ROUTE_TRANSACTIONS)
                                },
                                onQuickTransferFrom = { accId ->
                                    openTransactionSheet(TransactionType.TRANSFER, null, accId)
                                },
                                onSaveAccount = vm::saveAccount,
                                onToggleArchiveAccount = vm::toggleArchiveAccount,
                                onDeleteAccount = vm::deleteAccount,
                                onOpenCardDetails = { summary -> selectedCardForDetails = summary },
                                onPayCreditCard = { cardId ->
                                    openTransactionSheet(TransactionType.CC_PAYMENT, cardId, null)
                                },
                                onSaveCard = vm::saveCreditCard,
                                onToggleArchiveCard = vm::toggleArchiveCard,
                                onDeleteCard = vm::deleteCreditCard,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_PLANNING) {
                            PlanningScreen(
                                state = state,
                                onPreviousMonth = vm::previousMonth,
                                onNextMonth = vm::nextMonth,
                                onToggleVisibility = vm::toggleBalanceVisibility,
                                onSetBudget = vm::setBudget,
                                onInspectBudgetCategory = { catId ->
                                    vm.filterTransactionsByCategory(catId)
                                    navigateToTab(ROUTE_TRANSACTIONS)
                                },
                                onSaveRecurringBill = vm::saveRecurringBill,
                                onDeleteRecurringBill = vm::deleteRecurringBill,
                                onSaveGoal = vm::addOrUpdateGoal,
                                onContributeGoal = vm::contributeToGoal,
                                onDeleteGoal = vm::deleteGoal,
                                onBack = null
                            )
                        }
                        composable(ROUTE_PORTFOLIO) {
                            PortfolioScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSaveMutualFund = vm::saveMutualFund,
                                onDeleteMutualFund = vm::deleteMutualFund,
                                onRecordMfTx = vm::recordMutualFundTx,
                                onUpdateMfNavs = vm::updateMutualFundNavs,
                                onSaveStock = vm::saveStock,
                                onDeleteStock = vm::deleteStock,
                                onRecordStockTrade = vm::recordStockTrade,
                                onUpdateStockPrices = vm::updateStockPrices,
                                onSavePpfAccount = vm::savePpfAccount,
                                onDeletePpfAccount = vm::deletePpfAccount,
                                onRecordPpfTx = vm::recordPpfTx,
                                onCreditPpfFyInterest = vm::creditCurrentFyEstimatedPpfInterest,
                                onUpdatePpfRate = vm::updatePpfFyRate,
                                onSaveEpfAccount = vm::saveEpfAccount,
                                onDeleteEpfAccount = vm::deleteEpfAccount,
                                onRecordEpfEntry = vm::recordEpfEntry,
                                onSaveOtherAsset = vm::saveOtherAsset,
                                onDeleteOtherAsset = vm::deleteOtherAsset,
                                onUpdateGoldRates = vm::updateGoldRates
                            )
                        }
                        composable(ROUTE_REPORTS) {
                            ReportsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onPreviousMonth = vm::previousMonth,
                                onNextMonth = vm::nextMonth,
                                onToggleVisibility = vm::toggleBalanceVisibility,
                                onOpenNetWorthItems = { navController.navigate(ROUTE_NET_WORTH) },
                                onOpenPortfolio = { navController.navigate(ROUTE_PORTFOLIO) }
                            )
                        }
                        composable(ROUTE_MORE) {
                            MoreScreen(
                                state = state,
                                onSetThemeMode = vm::setThemeMode,
                                onSetFontStyle = vm::setFontStyle,
                                onResetDemo = vm::resetDemoData,
                                onOpenPortfolio = { navController.navigate(ROUTE_PORTFOLIO) },
                                onOpenReports = { navController.navigate(ROUTE_REPORTS) },
                                onOpenPlanning = { navigateToTab(ROUTE_PLANNING) },
                                onOpenAccounts = { navController.navigate(ROUTE_ACCOUNTS) },
                                onOpenNetWorth = { navController.navigate(ROUTE_NET_WORTH) },
                                onOpenRules = { navController.navigate(ROUTE_RULES) },
                                onOpenAutoCapture = { navController.navigate(ROUTE_CAPTURE) },
                                onOpenHomeCustomization = { homeCustomSheetVisible = true },
                                onOpenPrivacySettings = { navController.navigate(ROUTE_PRIVACY) }
                            )
                        }
                        composable(ROUTE_PRIVACY) {
                            PrivacySettingsScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSetPinAndEnable = vm::setPinAndEnableLock,
                                onToggleBiometrics = vm::toggleBiometricLock,
                                onDisableLock = vm::disableAppLock,
                                onLockNow = {
                                    vm.lockNow()
                                    navController.popBackStack()
                                },
                                onExportBackupJson = vm::exportBackupJson,
                                onExportCsv = vm::exportTransactionsCsv,
                                onRestoreBackupJson = vm::restoreBackupJson,
                                onDeleteAllData = vm::deleteAllFinancialData,
                                onResetDemoData = vm::resetDemoData
                            )
                        }
                        composable(ROUTE_NET_WORTH) {
                            NetWorthScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onSaveItem = vm::saveNetWorthItem,
                                onDeleteItem = vm::deleteNetWorthItem
                            )
                        }
                        composable(ROUTE_RULES) {
                            RulesScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onAddRule = vm::saveRule,
                                onToggleRule = vm::toggleRule,
                                onDeleteRule = vm::deleteRule
                            )
                        }
                        composable(ROUTE_CAPTURE) {
                            AutoCaptureScreen(
                                state = state,
                                onBack = { navController.popBackStack() },
                                onConsentChange = vm::updateSmsConsent,
                                onImportCandidates = { chosen ->
                                    vm.importCapturedCandidates(
                                        candidates = chosen,
                                        defaultAccountId = state.accounts.firstOrNull()?.id,
                                        defaultCardId = state.creditCards.firstOrNull()?.id
                                    )
                                }
                            )
                        }
                    }
                    if (!useRail && isPrimaryTab) {
                        BackHandler(enabled = fabExpanded) {
                            fabExpanded = false
                        }
                        CenteredQuickAddTray(
                            visible = fabExpanded,
                            onSelectType = { type -> openTransactionSheet(type, null, null) },
                            onDismiss = { fabExpanded = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (homeCustomSheetVisible) {
        HomeCustomizationSheet(
            cards = state.homeCards,
            onMove = vm::moveHomeCard,
            onToggleVisibility = vm::toggleHomeCardVisibility,
            onResetDefault = vm::resetHomeCardsLayout,
            onDismiss = { homeCustomSheetVisible = false }
        )
    }

    selectedCardForDetails?.let { cardSummary ->
        val freshSummary = state.cardSummaries.find { it.card.id == cardSummary.card.id } ?: cardSummary
        val cardTxs = state.monthlyTransactions.filter { it.creditCardId == freshSummary.card.id }
        CreditCardDetailSheet(
            summary = freshSummary,
            recentCardTransactions = cardTxs,
            isBalanceHidden = state.isBalanceHidden,
            onPayBill = {
                selectedCardForDetails = null
                openTransactionSheet(TransactionType.CC_PAYMENT, freshSummary.card.id, null)
            },
            onViewTransactions = {
                selectedCardForDetails = null
                vm.filterTransactionsByCard(freshSummary.card.id)
                navigateToTab(ROUTE_TRANSACTIONS)
            },
            onDismiss = { selectedCardForDetails = null }
        )
    }

    if (sheetVisible) {
        AddEditTransactionSheet(
            state = state,
            initialType = sheetInitialType,
            preselectedCardId = preselectedCardId,
            preselectedAccountId = preselectedAccountId,
            editingTx = editingTx,
            onDismiss = { sheetVisible = false },
            onSave = { tx, learnRule ->
                vm.saveTransaction(tx, learnRule)
                sheetVisible = false
            },
            onDelete = { tx ->
                vm.deleteTransaction(tx)
                sheetVisible = false
            },
            onCreateCustomCategory = vm::addCategory
        )
    }
}
