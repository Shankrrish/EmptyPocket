package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FinanceDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AppFontStyle
import com.example.data.model.AppThemeMode
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.CreditCardEntity
import com.example.data.model.HomeCardConfig
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettingsEntity
import com.example.ui.AccountSummary
import com.example.ui.CardSummary
import com.example.ui.FinanceUiState
import com.example.ui.NetWorthSummary
import com.example.ui.PortfolioSummary
import com.example.ui.components.CenteredBottomNavBar
import com.example.ui.components.CenteredNavTabItem
import com.example.ui.components.CenteredQuickAddTray
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.FinFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Samsung Galaxy S23 (360x780dp) AMOLED Dark & Crisp Light verification + Room MIGRATION_5_6 tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-xhdpi")
class S23AmoledThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun s23SampleState(themeMode: AppThemeMode = AppThemeMode.AMOLED_DARK): FinanceUiState {
        val accounts = listOf(
            AccountEntity(id = 1L, name = "HDFC Salary", type = AccountType.CHECKING, initialBalance = 85000.0, emoji = "🏦"),
            AccountEntity(id = 2L, name = "Home Cash", type = AccountType.CASH, initialBalance = 7500.0, emoji = "💵")
        )
        val cards = listOf(
            CreditCardEntity(id = 1L, name = "HDFC Millennia", brand = "Visa", limitAmount = 150000.0, closingDay = 20, dueDay = 5)
        )
        val categories = listOf(
            CategoryEntity(id = 1L, name = "Food & Dining", type = CategoryType.EXPENSE, iconName = "🍔", colorHex = 0xFF556B2F),
            CategoryEntity(id = 2L, name = "Salary", type = CategoryType.INCOME, iconName = "💰", colorHex = 0xFF3B6E22)
        )
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionEntity(id = 1L, title = "Swiggy Lunch", amount = 450.0, type = TransactionType.EXPENSE, dateMillis = now, isPaid = true, categoryId = 1L, accountId = 1L, creditCardId = null),
            TransactionEntity(id = 2L, title = "Monthly Salary", amount = 90000.0, type = TransactionType.INCOME, dateMillis = now, isPaid = true, categoryId = 2L, accountId = 1L, creditCardId = null)
        )
        return FinanceUiState(
            accounts = accounts,
            accountSummaries = listOf(
                AccountSummary(accounts[0], 85000.0),
                AccountSummary(accounts[1], 7500.0)
            ),
            creditCards = cards,
            cardSummaries = listOf(CardSummary(cards[0], openInvoice = 4200.0, availableLimit = 145800.0)),
            categories = categories,
            allTransactions = txs,
            monthlyTransactions = txs,
            filteredTransactions = txs,
            totalBalance = 92500.0,
            monthlyIncome = 90000.0,
            monthlyExpense = 14500.0,
            portfolioSummary = PortfolioSummary(totalInvested = 200000.0, totalCurrentValue = 245000.0, unrealizedGain = 45000.0, unrealizedGainPercent = 22.5),
            netWorthSummary = NetWorthSummary(
                liquidBankAndCash = 92500.0,
                totalInvestmentsAndAssets = 245000.0,
                totalAssets = 337500.0,
                cardOutstandingLiabilities = 4200.0,
                loanLiabilities = 0.0,
                totalLiabilities = 4200.0,
                netWorth = 333300.0,
                historyPoints = emptyList()
            ),
            settings = UserSettingsEntity(themeMode = themeMode.name),
            homeCards = HomeCardConfig.defaultList()
        )
    }

    @Test
    fun migration5To6_preservesExistingUserSettingsAndDefaultsThemeModeToAmoledDark() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_5_6_test.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS user_settings (
                            id INTEGER NOT NULL PRIMARY KEY,
                            appLockEnabled INTEGER NOT NULL,
                            biometricEnabled INTEGER NOT NULL,
                            pinHash TEXT NOT NULL,
                            pinSalt TEXT NOT NULL,
                            monthlySavingsTarget REAL NOT NULL,
                            salaryDay INTEGER NOT NULL,
                            smsAutoScanConsent INTEGER NOT NULL,
                            homeCardsConfig TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO user_settings (id, appLockEnabled, biometricEnabled, pinHash, pinSalt, monthlySavingsTarget, salaryDay, smsAutoScanConsent, homeCardsConfig)
                        VALUES (1, 0, 0, '', '', 35000.0, 7, 1, 'SUMMARY:1')
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        FinanceDatabase.MIGRATION_5_6.migrate(db)

        db.query("SELECT monthlySavingsTarget, salaryDay, smsAutoScanConsent, homeCardsConfig, themeMode FROM user_settings WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(35000.0, cursor.getDouble(0), 0.01)
            assertEquals(7, cursor.getInt(1))
            assertEquals(1, cursor.getInt(2))
            assertEquals("SUMMARY:1", cursor.getString(3))
            assertEquals("AMOLED_DARK", cursor.getString(4))
        }
        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun s23Viewport_amoledDarkTheme_rendersTrueBlackSurfaceTokensHeroAndBottomBar() {
        var observedBg: Color? = null
        var observedSurface: Color? = null
        var observedIsAmoled = false

        val tabs = listOf(
            CenteredNavTabItem("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
            CenteredNavTabItem("transactions", "Transactions", Icons.AutoMirrored.Filled.FormatListBulleted, Icons.AutoMirrored.Outlined.FormatListBulleted),
            CenteredNavTabItem("accounts", "Accounts", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
            CenteredNavTabItem("more", "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
        )

        composeRule.setContent {
            FinFlowTheme(themeMode = AppThemeMode.AMOLED_DARK) {
                observedBg = MaterialTheme.colorScheme.background
                observedSurface = MaterialTheme.colorScheme.surface
                observedIsAmoled = FinFlowTheme.colors.isAmoledDark
                Box(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        state = s23SampleState(AppThemeMode.AMOLED_DARK),
                        onPreviousMonth = {},
                        onNextMonth = {},
                        onToggleVisibility = {},
                        onNavigateToPlanning = {},
                        onNavigateToTransactions = {},
                        onInspectAccount = {},
                        onQuickAddForAccount = {},
                        onOpenCardDetails = {},
                        onPayCreditCard = {},
                        onUpdateSavingsTarget = {},
                        onOpenHomeCustomization = {},
                        onOpenPrivacySettings = {}
                    )
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        CenteredBottomNavBar(
                            tabs = tabs,
                            currentRoute = "dashboard",
                            fabExpanded = false,
                            onSelectTab = {},
                            onToggleCenterAdd = {}
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        assertTrue(observedIsAmoled)
        assertEquals(Color(0xFF000000), observedBg)
        assertEquals(Color(0xFF121312), observedSurface)

        composeRule.onNodeWithTag("balance_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("total_balance_text").assertTextContains("₹92,500.00")
        composeRule.onNodeWithText("Safe Cash / Day").assertIsDisplayed()
        composeRule.onNodeWithText("Total CC Balance").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_accounts_section").assertIsDisplayed()
        composeRule.onNodeWithText("HDFC Salary").assertIsDisplayed()
        composeRule.onNodeWithText("Bank").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_dashboard").assertIsDisplayed()
        composeRule.onNodeWithTag("main_add_fab").assertIsDisplayed()
    }

    @Test
    fun s23Viewport_crispLightTheme_rendersCleanLightTokensAndGroupedTransactions() {
        var observedBg: Color? = null
        var observedIsAmoled = true

        composeRule.setContent {
            FinFlowTheme(themeMode = AppThemeMode.CRISP_LIGHT) {
                observedBg = MaterialTheme.colorScheme.background
                observedIsAmoled = FinFlowTheme.colors.isAmoledDark
                TransactionsScreen(
                    state = s23SampleState(AppThemeMode.CRISP_LIGHT),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToggleVisibility = {},
                    onSearchChange = {},
                    onFilterChange = {},
                    onClearContextFilters = {},
                    onTogglePaid = {},
                    onEditTransaction = {},
                    onOpenAutoCapture = {},
                    onOpenRules = {}
                )
            }
        }
        composeRule.waitForIdle()

        assertFalse(observedIsAmoled)
        assertEquals(Color(0xFFF6F7F4), observedBg)
        composeRule.onNodeWithTag("transactions_search_input").assertIsDisplayed()
        composeRule.onNodeWithText("Swiggy Lunch").assertIsDisplayed()
        composeRule.onNodeWithText("Monthly Salary").assertIsDisplayed()
    }

    @Test
    fun s23Viewport_moreScreenThemeToggle_switchesBetweenAmoledAndCrispLight() {
        var latestMode: AppThemeMode? = null
        composeRule.setContent {
            var currentMode by remember { mutableStateOf(AppThemeMode.AMOLED_DARK) }
            FinFlowTheme(themeMode = currentMode) {
                MoreScreen(
                    state = s23SampleState(currentMode),
                    onSetThemeMode = {
                        currentMode = it
                        latestMode = it
                    },
                    onResetDemo = {},
                    onOpenPortfolio = {},
                    onOpenReports = {},
                    onOpenPlanning = {},
                    onOpenAccounts = {},
                    onOpenNetWorth = {},
                    onOpenRules = {},
                    onOpenAutoCapture = {},
                    onOpenHomeCustomization = {},
                    onOpenPrivacySettings = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("appearance_theme_card").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_mode_crisp_light").performClick()
        composeRule.waitForIdle()
        assertEquals(AppThemeMode.CRISP_LIGHT, latestMode)
        composeRule.onNodeWithText("Crisp Light • Clean daylight financial workspace").assertIsDisplayed()

        composeRule.onNodeWithTag("theme_mode_amoled_dark").performClick()
        composeRule.waitForIdle()
        assertEquals(AppThemeMode.AMOLED_DARK, latestMode)
        composeRule.onNodeWithText("True Black AMOLED • S23 battery & contrast optimized").assertIsDisplayed()
    }

    @Test
    fun s23Viewport_portfolioHeroCard_displaysCorrectlyInAmoledDark() {
        composeRule.setContent {
            FinFlowTheme(themeMode = AppThemeMode.AMOLED_DARK) {
                PortfolioScreen(
                    state = s23SampleState(AppThemeMode.AMOLED_DARK),
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

        composeRule.onNodeWithTag("portfolio_total_value").assertTextContains("₹2,45,000.00")
        composeRule.onNodeWithText("Offline Tracking").assertIsDisplayed()
    }

    @Test
    fun s23Viewport_reportsHeroCard_displaysCorrectlyInAmoledDark() {
        composeRule.setContent {
            FinFlowTheme(themeMode = AppThemeMode.AMOLED_DARK) {
                ReportsScreen(
                    state = s23SampleState(AppThemeMode.AMOLED_DARK),
                    onBack = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToggleVisibility = {},
                    onOpenNetWorthItems = {},
                    onOpenPortfolio = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("report_tab_net_worth").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Consolidated Net Worth").assertIsDisplayed()
        composeRule.onNodeWithText("₹3,33,300.00").assertIsDisplayed()
    }

    @Test
    fun backupManager_roundTripsThemeModeAndDefaultsLegacyBackupsToAmoledDark() {
        val sampleCats = listOf(
            CategoryEntity(id = 1L, name = "Food", type = CategoryType.EXPENSE, iconName = "🍔", colorHex = 0xFF556B2F)
        )
        val data = com.example.util.FinFlowBackupData(
            accounts = listOf(AccountEntity(1L, "HDFC", AccountType.CHECKING, 25000.0)),
            creditCards = emptyList(),
            categories = sampleCats,
            transactions = emptyList(),
            budgets = emptyList(),
            savingsGoals = emptyList(),
            netWorthItems = emptyList(),
            netWorthSnapshots = emptyList(),
            rules = emptyList(),
            recurringBills = emptyList(),
            settings = UserSettingsEntity(
                monthlySavingsTarget = 30000.0,
                salaryDay = 5,
                themeMode = AppThemeMode.CRISP_LIGHT.name
            )
        )
        val json = com.example.util.BackupManager.encodeBackupJson(data)
        val decoded = com.example.util.BackupManager.decodeBackupJson(json)
        assertEquals(AppThemeMode.CRISP_LIGHT, decoded.settings?.resolvedThemeMode())
        assertEquals(30000.0, decoded.settings?.monthlySavingsTarget ?: 0.0, 0.01)

        // Legacy JSON without themeMode key inside settings should safely default to AMOLED_DARK
        val legacyJson = """
            {
              "schemaVersion": 1,
              "settings": {
                "monthlySavingsTarget": 15000.0,
                "salaryDay": 1,
                "smsAutoScanConsent": false
              }
            }
        """.trimIndent()
        val legacyDecoded = com.example.util.BackupManager.decodeBackupJson(legacyJson)
        assertEquals(AppThemeMode.AMOLED_DARK, legacyDecoded.settings?.resolvedThemeMode())
    }

    @Test
    fun s23Viewport_planningInteractiveCalendar_showsDueItemsOnDayTapAndPreFillsDay() {
        val stateWithRecurring = s23SampleState(AppThemeMode.AMOLED_DARK).copy(
            recurringBills = listOf(
                com.example.data.model.RecurringBillEntity(
                    id = 1L,
                    title = "Home Loan EMI",
                    amount = 28500.0,
                    dueDay = 12,
                    frequency = com.example.data.model.RecurringFrequency.MONTHLY,
                    isSubscription = false
                )
            )
        )

        composeRule.setContent {
            FinFlowTheme(themeMode = AppThemeMode.AMOLED_DARK) {
                com.example.ui.screens.PlanningScreen(
                    state = stateWithRecurring,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onToggleVisibility = {},
                    onSetBudget = { _, _, _ -> },
                    onInspectBudgetCategory = {},
                    onSaveRecurringBill = {},
                    onDeleteRecurringBill = {},
                    onSaveGoal = {},
                    onContributeGoal = { _, _ -> },
                    onDeleteGoal = {},
                    onBack = null
                )
            }
        }
        composeRule.waitForIdle()

        // Switch to Recurring tab inside Planning
        composeRule.onNodeWithTag("planning_tab_1").performClick()
        composeRule.waitForIdle()

        // Tap Day 5 (Credit Card HDFC Millennia due day)
        composeRule.onNodeWithTag("calendar_day_5").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_due_item_0").assertIsDisplayed()
        composeRule.onNodeWithText("HDFC Millennia Due").assertIsDisplayed()
        composeRule.onNodeWithText("₹4,200.00").assertIsDisplayed()

        // Tap Day 12 (Home Loan EMI due day)
        composeRule.onNodeWithTag("calendar_day_12").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_due_item_0").assertIsDisplayed()
        composeRule.onNodeWithText("Home Loan EMI").assertIsDisplayed()

        // Tap Day 18 (empty day) and verify quick-add button pre-fills Day 18
        composeRule.onNodeWithTag("calendar_day_18").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_selected_day_empty").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_add_bill_day_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("recurring_due_day_input").assertTextContains("18")
    }

    @Test
    fun migration6To7_preservesSettingsAndDefaultsFontStyleToJakartaSans() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_6_7_test.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS user_settings (
                            id INTEGER NOT NULL PRIMARY KEY,
                            appLockEnabled INTEGER NOT NULL,
                            biometricEnabled INTEGER NOT NULL,
                            pinHash TEXT NOT NULL,
                            pinSalt TEXT NOT NULL,
                            monthlySavingsTarget REAL NOT NULL,
                            salaryDay INTEGER NOT NULL,
                            smsAutoScanConsent INTEGER NOT NULL,
                            homeCardsConfig TEXT NOT NULL,
                            themeMode TEXT NOT NULL DEFAULT 'AMOLED_DARK'
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO user_settings (id, appLockEnabled, biometricEnabled, pinHash, pinSalt, monthlySavingsTarget, salaryDay, smsAutoScanConsent, homeCardsConfig, themeMode)
                        VALUES (1, 0, 0, '', '', 42000.0, 5, 1, 'SUMMARY:1', 'CRISP_LIGHT')
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase
        FinanceDatabase.MIGRATION_6_7.migrate(db)

        db.query("SELECT monthlySavingsTarget, salaryDay, themeMode, fontStyle FROM user_settings WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(42000.0, cursor.getDouble(0), 0.01)
            assertEquals(5, cursor.getInt(1))
            assertEquals("CRISP_LIGHT", cursor.getString(2))
            assertEquals("JAKARTA_SANS", cursor.getString(3))
        }
        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun s23Viewport_moreScreenFontSelector_switchesBetweenJakartaManropeAndOutfit() {
        var latestFont: AppFontStyle? = null
        composeRule.setContent {
            var currentFont by remember { mutableStateOf(AppFontStyle.JAKARTA_SANS) }
            val state = s23SampleState(AppThemeMode.AMOLED_DARK).let {
                it.copy(settings = it.settings.copy(fontStyle = currentFont.name))
            }
            FinFlowTheme(themeMode = AppThemeMode.AMOLED_DARK, fontStyle = currentFont) {
                MoreScreen(
                    state = state,
                    onSetThemeMode = {},
                    onSetFontStyle = {
                        currentFont = it
                        latestFont = it
                    },
                    onResetDemo = {},
                    onOpenPortfolio = {},
                    onOpenReports = {},
                    onOpenPlanning = {},
                    onOpenAccounts = {},
                    onOpenNetWorth = {},
                    onOpenRules = {},
                    onOpenAutoCapture = {},
                    onOpenHomeCustomization = {},
                    onOpenPrivacySettings = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("font_style_jakarta_sans").assertIsDisplayed()
        composeRule.onNodeWithTag("font_style_inter").performClick()
        composeRule.waitForIdle()
        assertEquals(AppFontStyle.INTER, latestFont)

        composeRule.onNodeWithTag("font_style_roboto").performClick()
        composeRule.waitForIdle()
        assertEquals(AppFontStyle.ROBOTO, latestFont)

        composeRule.onNodeWithTag("font_style_manrope").performClick()
        composeRule.waitForIdle()
        assertEquals(AppFontStyle.MANROPE, latestFont)
        composeRule.onNodeWithText("Modern editorial balance").assertIsDisplayed()

        composeRule.onNodeWithTag("font_style_outfit").performClick()
        composeRule.waitForIdle()
        assertEquals(AppFontStyle.OUTFIT, latestFont)
        composeRule.onNodeWithText("Clean rounded contemporary").assertIsDisplayed()

        composeRule.onNodeWithTag("font_style_jakarta_sans").performClick()
        composeRule.waitForIdle()
        assertEquals(AppFontStyle.JAKARTA_SANS, latestFont)
        composeRule.onNodeWithText("Crisp geometric fintech • Default").assertIsDisplayed()
    }

    @Test
    fun backupManager_roundTripsFontStyleAndDefaultsLegacyToJakartaSans() {
        val data = com.example.util.FinFlowBackupData(
            accounts = listOf(AccountEntity(1L, "HDFC", AccountType.CHECKING, 25000.0)),
            creditCards = emptyList(),
            categories = emptyList(),
            transactions = emptyList(),
            budgets = emptyList(),
            savingsGoals = emptyList(),
            netWorthItems = emptyList(),
            netWorthSnapshots = emptyList(),
            rules = emptyList(),
            recurringBills = emptyList(),
            settings = UserSettingsEntity(
                monthlySavingsTarget = 25000.0,
                themeMode = AppThemeMode.AMOLED_DARK.name,
                fontStyle = AppFontStyle.OUTFIT.name
            )
        )
        val json = com.example.util.BackupManager.encodeBackupJson(data)
        val decoded = com.example.util.BackupManager.decodeBackupJson(json)
        assertEquals(AppFontStyle.OUTFIT, decoded.settings?.resolvedFontStyle())

        val legacyJson = """
            {
              "schemaVersion": 2,
              "settings": {
                "monthlySavingsTarget": 20000.0,
                "themeMode": "CRISP_LIGHT"
              }
            }
        """.trimIndent()
        val legacyDecoded = com.example.util.BackupManager.decodeBackupJson(legacyJson)
        assertEquals(AppThemeMode.CRISP_LIGHT, legacyDecoded.settings?.resolvedThemeMode())
        assertEquals(AppFontStyle.JAKARTA_SANS, legacyDecoded.settings?.resolvedFontStyle())
    }

    @Test
    fun s23Viewport_quickAddPopupScrimDismissesOverlayAndSquareCategoryTilesRender() {
        var dismissed = false
        composeRule.setContent {
            var expanded by remember { mutableStateOf(true) }
            FinFlowTheme(themeMode = AppThemeMode.AMOLED_DARK, fontStyle = AppFontStyle.MANROPE) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CenteredQuickAddTray(
                        visible = expanded,
                        onSelectType = {},
                        onDismiss = {
                            expanded = false
                            dismissed = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quick_add_popup_card").assertIsDisplayed()
        composeRule.onNodeWithText("Quick Transaction").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_expense_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_income_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_cc_expense_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_cc_payment_button").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_transfer_button").assertIsDisplayed()
        composeRule.onNodeWithText("Bank & Cash").assertIsDisplayed()
        composeRule.onNodeWithText("Salary & Inflow").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_scrim").performClick()
        composeRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun s23Viewport_transactionSheet_rendersVerticalSquarePastelCategoryTiles() {
        composeRule.setContent {
            FinFlowTheme(themeMode = AppThemeMode.AMOLED_DARK) {
                AddEditTransactionSheet(
                    state = s23SampleState(AppThemeMode.AMOLED_DARK),
                    initialType = TransactionType.EXPENSE,
                    preselectedCardId = null,
                    preselectedAccountId = 1L,
                    editingTx = null,
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sheet_category_food & dining").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("add_custom_category_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("sheet_save_button").performScrollTo().assertIsDisplayed()
    }
}
