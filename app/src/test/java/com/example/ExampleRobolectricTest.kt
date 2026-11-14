package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.HomeCardConfig
import com.example.data.model.TransactionType
import com.example.ui.AccountSummary
import com.example.ui.FinanceUiState
import com.example.ui.components.CenteredBottomNavBar
import com.example.ui.components.CenteredNavTabItem
import com.example.ui.components.CenteredQuickAddTray
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.FinFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleAccounts = listOf(
        AccountEntity(id = 1L, name = "HDFC Bank", type = AccountType.CHECKING, initialBalance = 29650.0),
        AccountEntity(id = 2L, name = "Home Cash", type = AccountType.CASH, initialBalance = 5500.0)
    )

    private val sampleCategories = listOf(
        CategoryEntity(id = 1L, name = "Food", type = CategoryType.EXPENSE, iconName = "Restaurant", colorHex = 0xFF556B2F)
    )

    private fun sampleState() = FinanceUiState(
        accounts = sampleAccounts,
        accountSummaries = listOf(
            AccountSummary(sampleAccounts[0], 29650.0),
            AccountSummary(sampleAccounts[1], 5500.0)
        ),
        categories = sampleCategories,
        totalBalance = 35150.0,
        monthlyIncome = 100000.0,
        monthlyExpense = 24500.0,
        homeCards = HomeCardConfig.defaultList()
    )

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MoneyTracker", appName)
    }

    @Test
    fun dashboard_displaysCurvedHeroAndVerticalAccountsWithQuickAdd() {
        var inspectedAccountId: Long? = null
        var quickAddedAccountId: Long? = null
        var customizeClicked = false
        var privacyClicked = false

        composeRule.setContent {
            FinFlowTheme {
                DashboardScreen(
                    state = sampleState(),
                    onMonthSelected = {},
                    onToggleVisibility = {},
                    onNavigateToPlanning = {},
                    onNavigateToTransactions = {},
                    onInspectAccount = { inspectedAccountId = it },
                    onQuickAddForAccount = { quickAddedAccountId = it },
                    onOpenCardDetails = {},
                    onPayCreditCard = {},
                    onUpdateSavingsTarget = {},
                    onOpenHomeCustomization = { customizeClicked = true },
                    onOpenPrivacySettings = { privacyClicked = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("balance_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("header_privacy_button").assertIsDisplayed()
        composeRule.onNodeWithTag("customize_home_button").assertIsDisplayed()
        composeRule.onNodeWithTag("total_balance_text").assertTextContains("₹35,150.00")
        composeRule.onNodeWithTag("toggle_visibility_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_income_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_expense_pill").assertIsDisplayed()

        composeRule.onNodeWithTag("dashboard_accounts_section").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_account_card_1").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_account_card_2").assertIsDisplayed()
        composeRule.onNodeWithText("HDFC Bank").assertIsDisplayed()
        composeRule.onNodeWithText("Home Cash").assertIsDisplayed()

        composeRule.onNodeWithTag("quick_add_account_2").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(2L, quickAddedAccountId)

        composeRule.onNodeWithTag("dashboard_account_card_1").performClick()
        composeRule.onNodeWithTag("customize_home_button").performClick()
        composeRule.onNodeWithTag("header_privacy_button").performClick()
        composeRule.waitForIdle()

        assertEquals(1L, inspectedAccountId)
        assertEquals(true, customizeClicked)
        assertEquals(true, privacyClicked)
    }

    @Test
    fun centeredBottomNavBar_rendersFourTabsAndExpandsQuickAddTray() {
        var selectedType: TransactionType? = null
        var selectedTab: String? = null
        val tabs = listOf(
            CenteredNavTabItem("dashboard", "Dashboard", Icons.Filled.Home, Icons.Outlined.Home),
            CenteredNavTabItem("transactions", "Transactions", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
            CenteredNavTabItem("planning", "Planning", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
            CenteredNavTabItem("more", "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
        )

        composeRule.setContent {
            FinFlowTheme {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxSize()) {
                    CenteredQuickAddTray(
                        visible = expanded,
                        onSelectType = {
                            selectedType = it
                            expanded = false
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        CenteredBottomNavBar(
                            tabs = tabs,
                            currentRoute = "dashboard",
                            fabExpanded = expanded,
                            onSelectTab = { selectedTab = it },
                            onToggleCenterAdd = { expanded = !expanded }
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_dashboard").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_transactions").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_planning").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_more").assertIsDisplayed()
        composeRule.onNodeWithTag("main_add_fab").assertIsDisplayed()

        composeRule.onNodeWithTag("main_add_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("fab_expense_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_cc_expense_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_income_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_cc_payment_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_transfer_button").assertIsDisplayed()

        composeRule.onNodeWithTag("fab_cc_expense_button").performClick()
        composeRule.onNodeWithTag("nav_tab_planning").performClick()
        composeRule.waitForIdle()

        assertEquals(TransactionType.CC_EXPENSE, selectedType)
        assertEquals("planning", selectedTab)
    }

    @Test
    fun addEditTransactionSheet_preselectsAccountFromQuickAdd() {
        composeRule.setContent {
            FinFlowTheme {
                AddEditTransactionSheet(
                    state = sampleState(),
                    initialType = TransactionType.EXPENSE,
                    preselectedCardId = null,
                    preselectedAccountId = 2L,
                    editingTx = null,
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sheet_source_dropdown").assertTextContains("Account: Home Cash")
    }

    @Test
    fun finFlowApp_showsVerticalAccountsCardAndCenteredBottomBarOnLaunch() {
        composeRule.setContent {
            FinFlowTheme {
                FinFlowApp()
            }
        }
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("HDFC Salary"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("balance_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_accounts_section").assertIsDisplayed()
        composeRule.onNodeWithText("HDFC Salary").assertIsDisplayed()
        composeRule.onNodeWithText("Wallet Cash").assertIsDisplayed()
        composeRule.onNodeWithText("Emergency Savings").assertIsDisplayed()
        composeRule.onNodeWithTag("main_add_fab").assertIsDisplayed()
    }

    @Test
    fun categoryCardGrid_opensCustomCategoryDialogAndSavesCustomEmojiCategory() {
        var createdName: String? = null
        var createdEmoji: String? = null
        var createdType: CategoryType? = null

        composeRule.setContent {
            FinFlowTheme {
                var showDialog by remember { mutableStateOf(false) }
                var categories by remember { mutableStateOf(sampleCategories) }
                var selectedId by remember { mutableStateOf<Long?>(1L) }

                com.example.ui.components.CategoryCardGrid(
                    categories = categories,
                    selectedCategoryId = selectedId,
                    onSelectCategory = { selectedId = it.id },
                    onRequestCustomCategory = { showDialog = true }
                )

                if (showDialog) {
                    com.example.ui.components.CustomCategoryDialog(
                        defaultType = CategoryType.EXPENSE,
                        onDismiss = { showDialog = false },
                        onSave = { name, type, emoji, colorHex ->
                            createdName = name
                            createdEmoji = emoji
                            createdType = type
                            val next = CategoryEntity(id = 99L, name = name, type = type, iconName = emoji, colorHex = colorHex)
                            categories = categories + next
                            selectedId = next.id
                            showDialog = false
                        }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sheet_category_food").assertIsDisplayed()
        composeRule.onNodeWithTag("add_custom_category_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("custom_category_name_input").performScrollTo().performTextInput("Coffee")
        composeRule.onNodeWithTag("emoji_pick_☕").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_custom_category_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("Coffee", createdName)
        assertEquals("☕", createdEmoji)
        assertEquals(CategoryType.EXPENSE, createdType)
        composeRule.onNodeWithTag("sheet_category_coffee").assertIsDisplayed()
    }

    @Test
    fun accountsScreen_displaysFiltersBanksAndEwalletDistinction() {
        composeRule.setContent {
            FinFlowTheme {
                com.example.ui.screens.AccountsScreen(
                    state = sampleState(),
                    onInspectAccount = {},
                    onQuickTransferFrom = {},
                    onSaveAccount = {},
                    onToggleArchiveAccount = {},
                    onDeleteAccount = {},
                    onOpenCardDetails = {},
                    onPayCreditCard = {},
                    onSaveCard = {},
                    onToggleArchiveCard = {},
                    onDeleteCard = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("accounts_total_liquid").assertTextContains("₹35,150.00")
        composeRule.onNodeWithTag("accounts_filter_wallets").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("📱 Prepaid E-Wallets").assertIsDisplayed()
        composeRule.onNodeWithTag("accounts_add_account_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("preset_bank_icici_bank").assertIsDisplayed()
    }

    @Test
    fun portfolioScreen_switchesBetweenTabsAndShowsHoldings() {
        val state = sampleState().copy(
            portfolioSummary = com.example.ui.PortfolioSummary(
                totalInvested = 100000.0,
                totalCurrentValue = 125000.0,
                unrealizedGain = 25000.0,
                unrealizedGainPercent = 25.0,
                monthlySipCommitment = 5000.0,
                assetAllocation = listOf(
                    com.example.ui.AllocationSlice("Mutual Funds", "📊", 125000.0, 1f, 0xFF556B2F)
                ),
                ppfSummaries = listOf(
                    com.example.ui.PpfSummary(
                        account = com.example.data.model.PpfAccountEntity(id = 1L, accountName = "SBI PPF", startFinancialYear = 2021),
                        currentBalance = 125000.0,
                        totalDeposits = 100000.0,
                        totalCreditedInterest = 25000.0,
                        estimatedUncreditedInterestForFy = 4200.0,
                        suggestedInterestToCreditForCompletedFy = 0.0,
                        currentFyDeposited = 50000.0,
                        currentFyLimit = 150000.0,
                        currentFyRemaining = 100000.0,
                        currentFyRatePercent = 7.1,
                        completedFysCount = 4,
                        maturityFyStart = 2036,
                        isLoanEligible = true,
                        isPartialWithdrawalEligible = false,
                        isMatured = false,
                        transactions = emptyList()
                    )
                )
            )
        )
        composeRule.setContent {
            FinFlowTheme {
                com.example.ui.screens.PortfolioScreen(
                    state = state,
                    onBack = {},
                    onSaveMutualFund = {},
                    onDeleteMutualFund = {},
                    onRecordMfTx = { _, _, _ -> },
                    onUpdateMfNavs = {},
                    onSaveStock = {},
                    onDeleteStock = {},
                    onRecordStockTrade = { _, _, _ -> },
                    onUpdateStockPrices = {},
                    onSavePpfAccount = {},
                    onDeletePpfAccount = {},
                    onRecordPpfTx = { _, _, _ -> },
                    onCreditPpfFyInterest = {},
                    onUpdatePpfRate = { _, _ -> },
                    onSaveEpfAccount = {},
                    onDeleteEpfAccount = {},
                    onRecordEpfEntry = {},
                    onSaveOtherAsset = {},
                    onDeleteOtherAsset = {},
                    onUpdateGoldRates = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("portfolio_total_value").assertTextContains("₹1,25,000.00")
        composeRule.onNodeWithTag("portfolio_tab_ppf_epf").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SBI PPF").assertIsDisplayed()
        composeRule.onNodeWithTag("portfolio_bulk_price_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Offline Valuation Update").assertIsDisplayed()
    }
}

