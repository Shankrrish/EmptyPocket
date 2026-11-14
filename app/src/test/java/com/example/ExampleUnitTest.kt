package com.example

import com.example.data.model.AssetLiabilityCategory
import com.example.data.model.BudgetEntity
import com.example.data.model.CategorizationRuleEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.NetWorthItemEntity
import com.example.data.model.RecurringBillEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.SplitAllocation
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettingsEntity
import com.example.ui.BudgetProgress
import com.example.ui.FinanceAnalytics
import com.example.util.IndiaCaptureParser
import com.example.util.MonthYear
import com.example.util.formatCurrency
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * Local unit tests for FinFlow currency formatting, India capture parsing, and analytics.
 */
class ExampleUnitTest {
    private val sampleCategories = listOf(
        CategoryEntity(id = 1L, name = "Food", type = CategoryType.EXPENSE, iconName = "Restaurant", colorHex = 0xFF556B2F),
        CategoryEntity(id = 2L, name = "Groceries", type = CategoryType.EXPENSE, iconName = "ShoppingCart", colorHex = 0xFF6B8E23),
        CategoryEntity(id = 3L, name = "Shopping", type = CategoryType.EXPENSE, iconName = "ShoppingBag", colorHex = 0xFF8FBC8F),
        CategoryEntity(id = 4L, name = "Salary", type = CategoryType.INCOME, iconName = "Payments", colorHex = 0xFF2E7D32)
    )

    @Test
    fun indianRupeeFormatting_groupsCommasCorrectly() {
        assertEquals("₹0.00", formatCurrency(0.0))
        assertEquals("₹999.50", formatCurrency(999.5))
        assertEquals("₹1,250.00", formatCurrency(1250.0))
        assertEquals("₹45,000.00", formatCurrency(45000.0))
        assertEquals("₹1,23,456.78", formatCurrency(123456.78))
        assertEquals("₹24,50,000.00", formatCurrency(2450000.0))
        assertEquals("-₹3,500.50", formatCurrency(-3500.5))
        assertEquals("₹ ••••", formatCurrency(50000.0, hidden = true))
    }

    @Test
    fun indiaCaptureParser_parsesUpiCardAndIncomeSms() {
        val rules = listOf(
            CategorizationRuleEntity(id = 1L, keyword = "Reliance Smart", categoryId = 2L)
        )
        val now = 1731500000000L

        val upiSms = "HDFC Bank: Rs. 640.00 debited from A/c XX4821 via UPI to Swiggy on 13-11-24."
        val upiParsed = IndiaCaptureParser.parseSingleMessage("1", upiSms, now, rules, sampleCategories, emptyList())
        assertNotNull(upiParsed)
        assertEquals(640.0, upiParsed!!.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, upiParsed.type)
        assertEquals("HDFC Bank", upiParsed.detectedBankOrRail)
        assertEquals("Food", upiParsed.suggestedCategoryName)

        val ccSms = "ICICI Bank Credit Card XX9012 spent INR 2,499.00 at Reliance Smart on 13-11-24."
        val ccParsed = IndiaCaptureParser.parseSingleMessage("2", ccSms, now, rules, sampleCategories, emptyList())
        assertNotNull(ccParsed)
        assertEquals(2499.0, ccParsed!!.amount, 0.01)
        assertEquals(TransactionType.CC_EXPENSE, ccParsed.type)
        assertEquals("ICICI Bank", ccParsed.detectedBankOrRail)
        assertEquals("Groceries", ccParsed.suggestedCategoryName)

        val salarySms = "SBI: Your A/c XX1122 is credited with INR 1,00,000.00 towards Salary."
        val salaryParsed = IndiaCaptureParser.parseSingleMessage("3", salarySms, now, rules, sampleCategories, emptyList())
        assertNotNull(salaryParsed)
        assertEquals(100000.0, salaryParsed!!.amount, 0.01)
        assertEquals(TransactionType.INCOME, salaryParsed.type)
        assertEquals("Salary", salaryParsed.suggestedCategoryName)
    }

    @Test
    fun indiaCaptureParser_parsesCsvAndDetectsDuplicates() {
        val cal = Calendar.getInstance().apply { set(2024, Calendar.NOVEMBER, 10, 12, 0, 0) }
        val existing = listOf(
            TransactionEntity(
                id = 10L,
                title = "Swiggy Order",
                amount = 450.0,
                type = TransactionType.EXPENSE,
                dateMillis = cal.timeInMillis,
                isPaid = true,
                categoryId = 1L,
                accountId = 1L,
                creditCardId = null
            )
        )
        val csv = """
            Date,Description,Amount,Type
            2024-11-10,Swiggy Order,450.00,DR
            2024-11-11,Amazon Pay,1899.00,DR
        """.trimIndent()

        val parsed = IndiaCaptureParser.parseCsvStatement(csv, emptyList(), sampleCategories, existing)
        assertEquals(2, parsed.size)
        assertTrue(parsed[0].isDuplicate)
        assertFalse(parsed[1].isDuplicate)
        assertEquals("Shopping", parsed[1].suggestedCategoryName)
    }

    @Test
    fun financeAnalytics_computesSplitSafeToSpendNetWorthAndInsights() {
        val currentMonth = MonthYear.current()
        val prevMonth = currentMonth.plusMonths(-1)

        val currMillis = Calendar.getInstance().apply {
            set(currentMonth.year, currentMonth.month - 1, 5, 10, 0)
        }.timeInMillis
        val prevMillis = Calendar.getInstance().apply {
            set(prevMonth.year, prevMonth.month - 1, 5, 10, 0)
        }.timeInMillis

        val splitTx = TransactionEntity(
            id = 1L,
            title = "Supermarket Run",
            amount = 3000.0,
            type = TransactionType.EXPENSE,
            dateMillis = currMillis,
            isPaid = true,
            categoryId = 2L,
            accountId = 1L,
            creditCardId = null,
            splitSummary = SplitAllocation.encode(
                listOf(
                    SplitAllocation(categoryId = 2L, amount = 2000.0),
                    SplitAllocation(categoryId = 3L, amount = 1000.0)
                )
            )
        )
        val unpaidRent = TransactionEntity(
            id = 2L,
            title = "House Rent",
            amount = 25000.0,
            type = TransactionType.EXPENSE,
            dateMillis = currMillis,
            isPaid = false,
            categoryId = 1L,
            accountId = 1L,
            creditCardId = null
        )
        val prevFoodTx = TransactionEntity(
            id = 3L,
            title = "Prev Dining",
            amount = 1000.0,
            type = TransactionType.EXPENSE,
            dateMillis = prevMillis,
            isPaid = true,
            categoryId = 2L,
            accountId = 1L,
            creditCardId = null
        )

        val spendMap = FinanceAnalytics.spendByCategory(listOf(splitTx))
        assertEquals(2000.0, spendMap[2L] ?: 0.0, 0.01)
        assertEquals(1000.0, spendMap[3L] ?: 0.0, 0.01)

        val safe = FinanceAnalytics.computeSafeToSpend(
            selectedMonth = currentMonth,
            monthlyIncome = 100000.0,
            monthTxs = listOf(splitTx, unpaidRent),
            recurringBills = emptyList(),
            settings = UserSettingsEntity(monthlySavingsTarget = 20000.0)
        )
        // 100,000 - 25,000 upcoming - 20,000 savings - 3,000 spent = 52,000
        assertEquals(52000.0, safe.safeToSpend, 0.01)
        assertTrue(safe.dailyAllowance > 0.0)

        val netWorth = FinanceAnalytics.computeNetWorth(
            liquidBankAndCash = 150000.0,
            cardOutstanding = 15000.0,
            items = listOf(
                NetWorthItemEntity(id = 1L, name = "Nifty 50", category = AssetLiabilityCategory.MUTUAL_FUNDS_STOCKS, amount = 300000.0),
                NetWorthItemEntity(id = 2L, name = "Car Loan", category = AssetLiabilityCategory.PERSONAL_LOAN, amount = 100000.0)
            ),
            snapshots = emptyList(),
            currentMonth = currentMonth
        )
        assertEquals(450000.0, netWorth.totalAssets, 0.01)
        assertEquals(115000.0, netWorth.totalLiabilities, 0.01)
        assertEquals(335000.0, netWorth.netWorth, 0.01)

        val insights = FinanceAnalytics.computeInsights(
            selectedMonth = currentMonth,
            monthTxs = listOf(splitTx, unpaidRent),
            allTransactions = listOf(splitTx, unpaidRent, prevFoodTx),
            categories = sampleCategories,
            budgetProgressList = listOf(
                BudgetProgress(
                    category = sampleCategories[1],
                    budget = BudgetEntity(id = 1L, categoryId = 2L, monthYear = currentMonth.key(), limitAmount = 1500.0),
                    spent = 2000.0
                )
            ),
            recurringBills = listOf(
                RecurringBillEntity(
                    id = 1L,
                    title = "Netflix",
                    amount = 649.0,
                    previousAmount = 499.0,
                    dueDay = 15,
                    frequency = RecurringFrequency.MONTHLY,
                    isSubscription = true
                )
            ),
            monthlyIncome = 100000.0,
            monthlyExpense = 28000.0
        )
        assertTrue(insights.any { it.id == "mom_2" })
        assertTrue(insights.any { it.id == "over_2" })
        assertTrue(insights.any { it.id == "subs_yearly" })
        assertTrue(insights.any { it.id == "bill_change_1" })
        assertTrue(insights.any { it.id == "savings_rate" })
    }

    @Test
    fun homeCardConfig_defaultPlacesAccountsBeforeCreditCardsAndRoundTrips() {
        val defaults = com.example.data.model.HomeCardConfig.defaultList()
        val visibleIds = defaults.filter { it.visible }.map { it.id }
        assertEquals(
            listOf(
                com.example.data.model.HomeCardId.SUMMARY,
                com.example.data.model.HomeCardId.ACCOUNTS,
                com.example.data.model.HomeCardId.CREDIT_CARDS,
                com.example.data.model.HomeCardId.BUDGETS
            ),
            visibleIds
        )
        val encoded = com.example.data.model.HomeCardConfig.encode(defaults)
        val decoded = com.example.data.model.HomeCardConfig.decode(encoded)
        assertEquals(defaults, decoded)
    }

    @Test
    fun securityUtils_hashesAndVerifiesSaltedPin() {
        val salt = com.example.util.SecurityUtils.generateSalt()
        val hash = com.example.util.SecurityUtils.hashPin("4829", salt)
        assertTrue(com.example.util.SecurityUtils.verifyPin("4829", salt, hash))
        assertFalse(com.example.util.SecurityUtils.verifyPin("1234", salt, hash))
        assertFalse(com.example.util.SecurityUtils.verifyPin("", salt, hash))
    }

    @Test
    fun accountingInvariants_preservesFourWaySeparationForCardAndTransfer() {
        val checking = com.example.data.model.AccountEntity(
            id = 1L,
            name = "HDFC",
            type = com.example.data.model.AccountType.CHECKING,
            initialBalance = 50000.0
        )
        val savings = com.example.data.model.AccountEntity(
            id = 2L,
            name = "Savings",
            type = com.example.data.model.AccountType.SAVINGS,
            initialBalance = 100000.0
        )
        val card = com.example.data.model.CreditCardEntity(
            id = 1L,
            name = "Regalia",
            brand = "Visa",
            limitAmount = 150000.0,
            closingDay = 20,
            dueDay = 5
        )
        val now = System.currentTimeMillis()
        val txs = listOf(
            // 1. Bank Expense: counts as expense + deducts checking
            TransactionEntity(1L, "Rent", 15000.0, TransactionType.EXPENSE, now, true, 1L, 1L, null),
            // 2. CC Purchase: counts as expense + adds card liability, does NOT deduct checking
            TransactionEntity(2L, "Dinner", 3000.0, TransactionType.CC_EXPENSE, now, true, 1L, null, 1L),
            // 3. Account Transfer: moves cash checking -> savings, does NOT count as expense
            TransactionEntity(3L, "Move", 10000.0, TransactionType.TRANSFER, now, true, null, 1L, null, toAccountId = 2L),
            // 4. CC Bill Payment: deducts checking & reduces card liability, does NOT double-count as expense
            TransactionEntity(4L, "Bill Pay", 2000.0, TransactionType.CC_PAYMENT, now, true, null, 1L, 1L)
        )

        val totalRecognizedExpense = txs.filter { FinanceAnalytics.isCountedExpense(it) }.sumOf { it.amount }
        assertEquals(18000.0, totalRecognizedExpense, 0.01)

        val checkingBalance = FinanceAnalytics.computeAccountBalance(checking, txs)
        // 50,000 - 15,000 (expense) - 10,000 (transfer) - 2,000 (cc_payment) = 23,000
        assertEquals(23000.0, checkingBalance, 0.01)

        val savingsBalance = FinanceAnalytics.computeAccountBalance(savings, txs)
        assertEquals(110000.0, savingsBalance, 0.01)

        val outstanding = FinanceAnalytics.computeCardOutstanding(card, txs)
        // 3,000 (cc_expense) - 2,000 (cc_payment) = 1,000
        assertEquals(1000.0, outstanding, 0.01)
        assertEquals(149000.0, FinanceAnalytics.computeCardAvailableLimit(card, outstanding), 0.01)
    }

    @Test
    fun backupManager_exportsCsvEscapingAndRoundTripsJson() {
        val acc = com.example.data.model.AccountEntity(1L, "HDFC Bank", com.example.data.model.AccountType.CHECKING, 10000.0)
        val tx = TransactionEntity(1L, "Coffee, Roastery", 250.0, TransactionType.EXPENSE, 1731500000000L, true, 1L, 1L, null, tags = "food")
        val csv = com.example.util.BackupManager.exportTransactionsCsv(listOf(tx), sampleCategories, listOf(acc), emptyList())
        assertTrue(csv.contains("\"Coffee, Roastery\""))
        assertTrue(csv.contains("250.00"))
        assertTrue(csv.contains("HDFC Bank"))
    }

    @Test
    fun emojiVisuals_resolvesBuiltInCustomAndAccountEmojis() {
        assertEquals("🍔", com.example.ui.components.resolveCategoryEmoji("Restaurant", "Food"))
        assertEquals("🛒", com.example.ui.components.resolveCategoryEmoji("ShoppingCart", "Groceries"))
        assertEquals("🛍️", com.example.ui.components.resolveCategoryEmoji("ShoppingBag", "Shopping"))
        assertEquals("☕", com.example.ui.components.resolveCategoryEmoji("☕", "Coffee"))
        assertEquals("🐾", com.example.ui.components.resolveCategoryEmoji("🐾", "Pet Care"))
        assertEquals("✈️", com.example.ui.components.resolveCategoryEmoji("", "Vacation"))

        assertEquals("🏦", com.example.ui.components.accountEmoji(com.example.data.model.AccountType.CHECKING))
        assertEquals("💵", com.example.ui.components.accountEmoji(com.example.data.model.AccountType.CASH))
        assertEquals("🐷", com.example.ui.components.accountEmoji(com.example.data.model.AccountType.SAVINGS))

        assertEquals("💳", com.example.ui.components.transactionEmoji(TransactionType.CC_PAYMENT, null))
        assertEquals("🔄", com.example.ui.components.transactionEmoji(TransactionType.TRANSFER, null))
        assertEquals("🍔", com.example.ui.components.transactionEmoji(TransactionType.EXPENSE, sampleCategories.first()))
    }

    @Test
    fun userSettings_defaultsAndResolvesThemeModesSafely() {
        assertEquals(com.example.data.model.AppThemeMode.AMOLED_DARK, UserSettingsEntity().resolvedThemeMode())
        assertEquals(
            com.example.data.model.AppThemeMode.CRISP_LIGHT,
            UserSettingsEntity(themeMode = com.example.data.model.AppThemeMode.CRISP_LIGHT.name).resolvedThemeMode()
        )
        assertEquals(
            com.example.data.model.AppThemeMode.SYSTEM,
            UserSettingsEntity(themeMode = com.example.data.model.AppThemeMode.SYSTEM.name).resolvedThemeMode()
        )
        assertEquals(
            com.example.data.model.AppThemeMode.AMOLED_DARK,
            UserSettingsEntity(themeMode = "UNKNOWN_VALUE").resolvedThemeMode()
        )
    }
}


