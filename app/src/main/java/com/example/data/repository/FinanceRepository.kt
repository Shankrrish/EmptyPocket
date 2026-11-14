package com.example.data.repository

import com.example.data.local.FinanceDao
import com.example.data.local.PortfolioDao
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AssetLiabilityCategory
import com.example.data.model.BudgetEntity
import com.example.data.model.CategorizationRuleEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.CreditCardEntity
import com.example.data.model.EpfAccountEntity
import com.example.data.model.EpfEntryEntity
import com.example.data.model.EpfEntryType
import com.example.data.model.MutualFundCategory
import com.example.data.model.MutualFundEntity
import com.example.data.model.MutualFundOption
import com.example.data.model.MutualFundPlan
import com.example.data.model.MutualFundTxEntity
import com.example.data.model.MutualFundTxType
import com.example.data.model.NetWorthItemEntity
import com.example.data.model.NetWorthSnapshotEntity
import com.example.data.model.OtherAssetEntity
import com.example.data.model.OtherAssetKind
import com.example.data.model.PpfAccountEntity
import com.example.data.model.PpfFyRateEntity
import com.example.data.model.PpfTxEntity
import com.example.data.model.PpfTxType
import com.example.data.model.RecurringBillEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.StockEntity
import com.example.data.model.StockExchange
import com.example.data.model.StockTradeEntity
import com.example.data.model.StockTradeType
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettingsEntity
import com.example.ui.PortfolioAnalytics
import com.example.util.DateUtils
import com.example.util.FinFlowBackupData
import com.example.util.MonthYear
import com.example.util.PortfolioBackupSlice
import kotlinx.coroutines.flow.first
import java.util.Calendar

class FinanceRepository(
    private val dao: FinanceDao,
    private val portfolioDao: PortfolioDao
) {
    val accounts = dao.observeAccounts()
    val creditCards = dao.observeCreditCards()
    val categories = dao.observeCategories()
    val transactions = dao.observeTransactions()
    val budgets = dao.observeAllBudgets()
    val savingsGoals = dao.observeSavingsGoals()
    val netWorthItems = dao.observeNetWorthItems()
    val netWorthSnapshots = dao.observeNetWorthSnapshots()
    val rules = dao.observeRules()
    val recurringBills = dao.observeRecurringBills()
    val settings = dao.observeSettings()

    // Portfolio flows
    val mutualFunds = portfolioDao.observeMutualFunds()
    val mutualFundTxs = portfolioDao.observeMutualFundTxs()
    val stocks = portfolioDao.observeStocks()
    val stockTrades = portfolioDao.observeStockTrades()
    val ppfAccounts = portfolioDao.observePpfAccounts()
    val ppfTxs = portfolioDao.observePpfTxs()
    val ppfRates = portfolioDao.observePpfRates()
    val epfAccounts = portfolioDao.observeEpfAccounts()
    val epfEntries = portfolioDao.observeEpfEntries()
    val otherAssets = portfolioDao.observeOtherAssets()

    suspend fun seedDemoDataIfEmpty() {
        // Seed only a truly empty database so real/restored data is never mixed with demo data.
        if (dao.getCategoryCount() == 0 && dao.getAccountCount() == 0) {
            populateDemoData()
        }
    }

    suspend fun clearAllUserData() {
        dao.clearTransactions()
        dao.clearBudgets()
        dao.clearCreditCards()
        dao.clearAccounts()
        dao.clearSavingsGoals()
        dao.clearNetWorthItems()
        dao.clearNetWorthSnapshots()
        dao.clearRules()
        dao.clearRecurringBills()
        clearPortfolioTables()
        dao.upsertSettings(UserSettingsEntity())
    }

    suspend fun resetDemoData() {
        dao.clearTransactions()
        dao.clearBudgets()
        dao.clearCreditCards()
        dao.clearAccounts()
        dao.clearCategories()
        dao.clearSavingsGoals()
        dao.clearNetWorthItems()
        dao.clearNetWorthSnapshots()
        dao.clearRules()
        dao.clearRecurringBills()
        clearPortfolioTables()
        dao.clearSettings()
        populateDemoData()
    }

    private suspend fun clearPortfolioTables() {
        portfolioDao.clearMutualFundTxs()
        portfolioDao.clearMutualFunds()
        portfolioDao.clearStockTrades()
        portfolioDao.clearStocks()
        portfolioDao.clearPpfTxs()
        portfolioDao.clearPpfAccounts()
        portfolioDao.clearPpfRates()
        portfolioDao.clearEpfEntries()
        portfolioDao.clearEpfAccounts()
        portfolioDao.clearOtherAssets()
    }

    suspend fun createBackupData(): FinFlowBackupData = FinFlowBackupData(
        accounts = dao.observeAccounts().first(),
        creditCards = dao.observeCreditCards().first(),
        categories = dao.observeCategories().first(),
        transactions = dao.observeTransactions().first(),
        budgets = dao.observeAllBudgets().first(),
        savingsGoals = dao.observeSavingsGoals().first(),
        netWorthItems = dao.observeNetWorthItems().first(),
        netWorthSnapshots = dao.observeNetWorthSnapshots().first(),
        rules = dao.observeRules().first(),
        recurringBills = dao.observeRecurringBills().first(),
        settings = dao.observeSettings().first(),
        portfolio = PortfolioBackupSlice(
            mutualFunds = portfolioDao.observeMutualFunds().first(),
            mutualFundTxs = portfolioDao.observeMutualFundTxs().first(),
            stocks = portfolioDao.observeStocks().first(),
            stockTrades = portfolioDao.observeStockTrades().first(),
            ppfAccounts = portfolioDao.observePpfAccounts().first(),
            ppfTxs = portfolioDao.observePpfTxs().first(),
            ppfRates = portfolioDao.observePpfRates().first(),
            epfAccounts = portfolioDao.observeEpfAccounts().first(),
            epfEntries = portfolioDao.observeEpfEntries().first(),
            otherAssets = portfolioDao.observeOtherAssets().first()
        )
    )

    suspend fun restoreBackupData(backup: FinFlowBackupData) {
        dao.clearTransactions()
        dao.clearBudgets()
        dao.clearCreditCards()
        dao.clearAccounts()
        dao.clearCategories()
        dao.clearSavingsGoals()
        dao.clearNetWorthItems()
        dao.clearNetWorthSnapshots()
        dao.clearRules()
        dao.clearRecurringBills()
        clearPortfolioTables()

        backup.accounts.forEach { dao.insertAccount(it) }
        backup.creditCards.forEach { dao.insertCreditCard(it) }
        backup.categories.forEach { dao.insertCategory(it) }
        backup.transactions.forEach { dao.insertTransaction(it) }
        backup.budgets.forEach { dao.insertBudget(it) }
        backup.savingsGoals.forEach { dao.insertSavingsGoal(it) }
        backup.netWorthItems.forEach { dao.insertNetWorthItem(it) }
        backup.netWorthSnapshots.forEach { dao.insertNetWorthSnapshot(it) }
        backup.rules.forEach { dao.insertRule(it) }
        backup.recurringBills.forEach { dao.insertRecurringBill(it) }

        val p = backup.portfolio
        p.mutualFunds.forEach { portfolioDao.insertMutualFund(it) }
        p.mutualFundTxs.forEach { portfolioDao.insertMutualFundTx(it) }
        p.stocks.forEach { portfolioDao.insertStock(it) }
        p.stockTrades.forEach { portfolioDao.insertStockTrade(it) }
        p.ppfAccounts.forEach { portfolioDao.insertPpfAccount(it) }
        p.ppfTxs.forEach { portfolioDao.insertPpfTx(it) }
        p.ppfRates.forEach { portfolioDao.insertPpfRate(it) }
        p.epfAccounts.forEach { portfolioDao.insertEpfAccount(it) }
        p.epfEntries.forEach { portfolioDao.insertEpfEntry(it) }
        p.otherAssets.forEach { portfolioDao.insertOtherAsset(it) }

        backup.settings?.let { restored ->
            val current = dao.observeSettings().first() ?: UserSettingsEntity()
            dao.upsertSettings(
                current.copy(
                    monthlySavingsTarget = restored.monthlySavingsTarget,
                    salaryDay = restored.salaryDay,
                    smsAutoScanConsent = restored.smsAutoScanConsent,
                    homeCardsConfig = restored.homeCardsConfig,
                    themeMode = restored.themeMode,
                    fontStyle = restored.fontStyle
                )
            )
        }
    }

    private suspend fun populateDemoData() {
        dao.upsertSettings(UserSettingsEntity(id = 1, monthlySavingsTarget = 20000.0, salaryDay = 1))

        val checkingId = dao.insertAccount(
            AccountEntity(
                name = "HDFC Salary",
                type = AccountType.CHECKING,
                initialBalance = 62000.00,
                colorHex = 0xFF1D4ED8,
                bankName = "HDFC Bank",
                accountSubType = "Savings",
                maskedNumber = "XXXX 4821",
                ifsc = "HDFC0000142",
                emoji = "🏛️"
            )
        )
        val cashId = dao.insertAccount(
            AccountEntity(
                name = "Wallet Cash",
                type = AccountType.CASH,
                initialBalance = 6500.00,
                colorHex = 0xFF3B6E22,
                bankName = "Physical Cash",
                accountSubType = "Wallet cash",
                emoji = "💵"
            )
        )
        val savingsId = dao.insertAccount(
            AccountEntity(
                name = "Emergency Savings",
                type = AccountType.SAVINGS,
                initialBalance = 185000.00,
                colorHex = 0xFF2F6F6D,
                bankName = "SBI",
                accountSubType = "Savings",
                maskedNumber = "XXXX 1122",
                ifsc = "SBIN0004321",
                emoji = "🏦"
            )
        )
        dao.insertAccount(
            AccountEntity(
                name = "PhonePe Wallet",
                type = AccountType.E_WALLET,
                initialBalance = 1850.00,
                colorHex = 0xFF6D28D9,
                bankName = "PhonePe",
                accountSubType = "Prepaid wallet",
                emoji = "📱",
                isPrepaidWallet = true
            )
        )

        val cardOlive = dao.insertCreditCard(
            CreditCardEntity(
                name = "Olive Regalia",
                brand = "Mastercard",
                limitAmount = 150000.0,
                closingDay = 25,
                dueDay = 5,
                colorHex = 0xFF556B2F,
                issuerBank = "HDFC Bank",
                statementDay = 25,
                minimumDueAmount = 0.0
            )
        )
        val cardSage = dao.insertCreditCard(
            CreditCardEntity(
                name = "Sage Travel",
                brand = "Visa",
                limitAmount = 80000.0,
                closingDay = 18,
                dueDay = 28,
                colorHex = 0xFF6B8E23,
                issuerBank = "ICICI Bank",
                statementDay = 18,
                minimumDueAmount = 0.0
            )
        )

        val catFood = dao.insertCategory(CategoryEntity(name = "Food", type = CategoryType.EXPENSE, iconName = "Restaurant", colorHex = 0xFFC23B2B))
        val catGroceries = dao.insertCategory(CategoryEntity(name = "Groceries", type = CategoryType.EXPENSE, iconName = "ShoppingCart", colorHex = 0xFF556B2F))
        val catShopping = dao.insertCategory(CategoryEntity(name = "Shopping", type = CategoryType.EXPENSE, iconName = "ShoppingBag", colorHex = 0xFFC97A16))
        val catTransport = dao.insertCategory(CategoryEntity(name = "Transport", type = CategoryType.EXPENSE, iconName = "DirectionsCar", colorHex = 0xFF2F6F6D))
        val catUtilities = dao.insertCategory(CategoryEntity(name = "Utilities", type = CategoryType.EXPENSE, iconName = "Bolt", colorHex = 0xFF0284C7))
        val catHousing = dao.insertCategory(CategoryEntity(name = "Housing", type = CategoryType.EXPENSE, iconName = "Home", colorHex = 0xFF3E4F22))
        val catSubs = dao.insertCategory(CategoryEntity(name = "Subscriptions", type = CategoryType.EXPENSE, iconName = "Subscriptions", colorHex = 0xFF6B8E23))
        val catHealth = dao.insertCategory(CategoryEntity(name = "Health", type = CategoryType.EXPENSE, iconName = "Favorite", colorHex = 0xFF0D9488))
        val catSalary = dao.insertCategory(CategoryEntity(name = "Salary", type = CategoryType.INCOME, iconName = "Work", colorHex = 0xFF3B6E22))
        dao.insertCategory(CategoryEntity(name = "Freelance", type = CategoryType.INCOME, iconName = "Laptop", colorHex = 0xFF6B8E23))

        val current = MonthYear.current()
        val prev = current.plusMonths(-1)
        fun dayMillis(ym: MonthYear, day: Int): Long = DateUtils.dayMillis(ym.year, ym.month, day)

        val prevMonthTxs = listOf(
            TransactionEntity(title = "Monthly Salary", amount = 100000.0, type = TransactionType.INCOME, dateMillis = dayMillis(prev, 1), isPaid = true, categoryId = catSalary, accountId = checkingId, creditCardId = null, isRecurring = true, tags = "salary"),
            TransactionEntity(title = "Apartment Rent", amount = 22000.0, type = TransactionType.EXPENSE, dateMillis = dayMillis(prev, 3), isPaid = true, categoryId = catHousing, accountId = checkingId, creditCardId = null, isRecurring = true, tags = "rent"),
            TransactionEntity(title = "Swiggy Orders", amount = 3400.0, type = TransactionType.CC_EXPENSE, dateMillis = dayMillis(prev, 7), isPaid = true, categoryId = catFood, accountId = null, creditCardId = cardOlive, tags = "upi,food"),
            TransactionEntity(title = "Reliance Smart", amount = 4200.0, type = TransactionType.EXPENSE, dateMillis = dayMillis(prev, 11), isPaid = true, categoryId = catGroceries, accountId = checkingId, creditCardId = null, tags = "upi,groceries"),
            TransactionEntity(title = "BESCOM Electricity", amount = 1950.0, type = TransactionType.EXPENSE, dateMillis = dayMillis(prev, 15), isPaid = true, categoryId = catUtilities, accountId = checkingId, creditCardId = null, isRecurring = true, tags = "utilities"),
            TransactionEntity(title = "Amazon Prime & Shopping", amount = 3500.0, type = TransactionType.CC_EXPENSE, dateMillis = dayMillis(prev, 18), isPaid = true, categoryId = catShopping, accountId = null, creditCardId = cardSage, tags = "shopping"),
            TransactionEntity(title = "Netflix & Spotify", amount = 768.0, type = TransactionType.CC_EXPENSE, dateMillis = dayMillis(prev, 20), isPaid = true, categoryId = catSubs, accountId = null, creditCardId = cardOlive, isRecurring = true, tags = "subscription")
        )
        val currentMonthTxs = listOf(
            TransactionEntity(title = "Monthly Salary", amount = 100000.0, type = TransactionType.INCOME, dateMillis = dayMillis(current, 1), isPaid = true, categoryId = catSalary, accountId = checkingId, creditCardId = null, isRecurring = true, tags = "salary"),
            TransactionEntity(title = "Apartment Rent", amount = 22000.0, type = TransactionType.EXPENSE, dateMillis = dayMillis(current, 3), isPaid = true, categoryId = catHousing, accountId = checkingId, creditCardId = null, isRecurring = true, tags = "rent"),
            TransactionEntity(title = "Swiggy Dinner", amount = 2650.0, type = TransactionType.CC_EXPENSE, dateMillis = dayMillis(current, 5), isPaid = true, categoryId = catFood, accountId = null, creditCardId = cardOlive, tags = "upi,food"),
            TransactionEntity(title = "Reliance Smart Haul", amount = 4850.0, type = TransactionType.CC_EXPENSE, dateMillis = dayMillis(current, 6), isPaid = true, categoryId = catGroceries, accountId = null, creditCardId = cardOlive, tags = "groceries,upi"),
            TransactionEntity(title = "Uber & Metro Pass", amount = 1850.0, type = TransactionType.CC_EXPENSE, dateMillis = dayMillis(current, 7), isPaid = true, categoryId = catTransport, accountId = null, creditCardId = cardOlive, tags = "commute"),
            TransactionEntity(title = "Zomato Weekend", amount = 1650.0, type = TransactionType.CC_EXPENSE, dateMillis = dayMillis(current, 8), isPaid = true, categoryId = catFood, accountId = null, creditCardId = cardSage, tags = "food"),
            TransactionEntity(title = "Amazon Electronics", amount = 7200.0, type = TransactionType.CC_EXPENSE, dateMillis = dayMillis(current, 9), isPaid = true, categoryId = catShopping, accountId = null, creditCardId = cardSage, tags = "shopping"),
            TransactionEntity(title = "Apollo Pharmacy", amount = 1450.0, type = TransactionType.EXPENSE, dateMillis = dayMillis(current, 10), isPaid = true, categoryId = catHealth, accountId = cashId, creditCardId = null, tags = "health"),
            TransactionEntity(title = "Regalia Bill Payment", amount = 4500.0, type = TransactionType.CC_PAYMENT, dateMillis = dayMillis(current, 11), isPaid = true, categoryId = null, accountId = checkingId, creditCardId = cardOlive, notes = "Partial bill payment restoring Regalia limit"),
            TransactionEntity(title = "Netflix Standard", amount = 649.0, type = TransactionType.CC_EXPENSE, dateMillis = dayMillis(current, 12), isPaid = true, categoryId = catSubs, accountId = null, creditCardId = cardOlive, isRecurring = true, tags = "subscription"),
            TransactionEntity(title = "Emergency Fund Deposit", amount = 10000.0, type = TransactionType.TRANSFER, dateMillis = dayMillis(current, 12), isPaid = true, categoryId = null, accountId = checkingId, creditCardId = null, toAccountId = savingsId, tags = "savings"),
            TransactionEntity(title = "Parag Parikh Flexi Cap SIP", amount = 8000.0, type = TransactionType.INVESTMENT_BUY, dateMillis = dayMillis(current, 5), isPaid = true, categoryId = null, accountId = checkingId, creditCardId = null, notes = "Monthly SIP (not counted as expense)", tags = "sip,mf", linkedAssetType = "MF"),
            TransactionEntity(title = "BESCOM Electricity", amount = 2350.0, type = TransactionType.EXPENSE, dateMillis = dayMillis(current, 18), isPaid = false, categoryId = catUtilities, accountId = checkingId, creditCardId = null, isRecurring = true, tags = "utilities,upi"),
            TransactionEntity(title = "Bike EMI", amount = 6500.0, type = TransactionType.EXPENSE, dateMillis = dayMillis(current, 22), isPaid = false, categoryId = catTransport, accountId = checkingId, creditCardId = null, isRecurring = true, tags = "emi")
        )
        (prevMonthTxs + currentMonthTxs).forEach { dao.insertTransaction(it) }

        val monthKey = current.key()
        dao.insertBudget(BudgetEntity(categoryId = catFood, monthYear = monthKey, limitAmount = 5000.0))
        dao.insertBudget(BudgetEntity(categoryId = catGroceries, monthYear = monthKey, limitAmount = 7000.0))
        dao.insertBudget(BudgetEntity(categoryId = catShopping, monthYear = monthKey, limitAmount = 5000.0))
        dao.insertBudget(BudgetEntity(categoryId = catTransport, monthYear = monthKey, limitAmount = 9000.0))
        dao.insertBudget(BudgetEntity(categoryId = catUtilities, monthYear = monthKey, limitAmount = 3500.0))
        dao.insertBudget(BudgetEntity(categoryId = catHousing, monthYear = monthKey, limitAmount = 22000.0))
        dao.insertBudget(BudgetEntity(categoryId = catSubs, monthYear = monthKey, limitAmount = 1500.0))
        dao.insertBudget(BudgetEntity(categoryId = catHealth, monthYear = monthKey, limitAmount = 3000.0))

        listOf(
            RecurringBillEntity(title = "Apartment Rent", amount = 22000.0, previousAmount = 22000.0, dueDay = 3, frequency = RecurringFrequency.MONTHLY, type = TransactionType.EXPENSE, categoryId = catHousing, accountId = checkingId, isSubscription = false),
            RecurringBillEntity(title = "Netflix", amount = 649.0, previousAmount = 649.0, dueDay = 12, frequency = RecurringFrequency.MONTHLY, type = TransactionType.CC_EXPENSE, categoryId = catSubs, creditCardId = cardOlive, isSubscription = true),
            RecurringBillEntity(title = "Spotify Duo", amount = 149.0, previousAmount = 119.0, dueDay = 15, frequency = RecurringFrequency.MONTHLY, type = TransactionType.CC_EXPENSE, categoryId = catSubs, creditCardId = cardOlive, isSubscription = true),
            RecurringBillEntity(title = "YouTube Premium", amount = 149.0, previousAmount = 149.0, dueDay = 17, frequency = RecurringFrequency.MONTHLY, type = TransactionType.CC_EXPENSE, categoryId = catSubs, creditCardId = cardOlive, isSubscription = true),
            RecurringBillEntity(title = "BESCOM Electricity", amount = 2350.0, previousAmount = 1950.0, dueDay = 18, frequency = RecurringFrequency.MONTHLY, type = TransactionType.EXPENSE, categoryId = catUtilities, accountId = checkingId, isSubscription = false),
            RecurringBillEntity(title = "Bike EMI", amount = 6500.0, previousAmount = 6500.0, dueDay = 22, frequency = RecurringFrequency.MONTHLY, type = TransactionType.EXPENSE, categoryId = catTransport, accountId = checkingId, isSubscription = false),
            RecurringBillEntity(title = "Amazon Prime Annual", amount = 1499.0, previousAmount = 1499.0, dueDay = 26, frequency = RecurringFrequency.ANNUAL, type = TransactionType.CC_EXPENSE, categoryId = catSubs, creditCardId = cardSage, isSubscription = true)
        ).forEach { dao.insertRecurringBill(it) }

        listOf(
            SavingsGoalEntity(name = "Emergency Fund", targetAmount = 300000.0, savedAmount = 185000.0, monthlyContribution = 10000.0, isRecurringContribution = true, iconName = "Savings", colorHex = 0xFF556B2F),
            SavingsGoalEntity(name = "Goa & Kerala Vacation", targetAmount = 50000.0, savedAmount = 32000.0, monthlyContribution = 5000.0, isRecurringContribution = true, iconName = "Flight", colorHex = 0xFF2F6F6D),
            SavingsGoalEntity(name = "New Vehicle", targetAmount = 150000.0, savedAmount = 54000.0, monthlyContribution = 8000.0, isRecurringContribution = true, iconName = "DirectionsCar", colorHex = 0xFF6B8E23),
            SavingsGoalEntity(name = "Children's Education", targetAmount = 500000.0, savedAmount = 140000.0, monthlyContribution = 12000.0, isRecurringContribution = true, iconName = "School", colorHex = 0xFFC97A16)
        ).forEach { dao.insertSavingsGoal(it) }

        // Seed Liabilities only (live portfolio holdings supply assets to avoid double-counting)
        val homeLoanId = dao.insertNetWorthItem(NetWorthItemEntity(name = "Home Loan Principal", category = AssetLiabilityCategory.HOME_LOAN, amount = 650000.0))
        dao.insertNetWorthItem(NetWorthItemEntity(name = "Personal Bike Loan", category = AssetLiabilityCategory.PERSONAL_LOAN, amount = 78000.0))

        val snapMonths = listOf(-5, -4, -3, -2, -1)
        val baseAssets = listOf(1280000.0, 1320000.0, 1365000.0, 1410000.0, 1455000.0)
        val baseLiab = listOf(810000.0, 795000.0, 778000.0, 760000.0, 745000.0)
        snapMonths.forEachIndexed { idx, offset ->
            val ym = current.plusMonths(offset)
            dao.insertNetWorthSnapshot(
                NetWorthSnapshotEntity(monthYear = ym.key(), label = ym.displayLabel().take(3), totalAssets = baseAssets[idx], totalLiabilities = baseLiab[idx])
            )
        }

        listOf(
            CategorizationRuleEntity(keyword = "Swiggy", categoryId = catFood),
            CategorizationRuleEntity(keyword = "Zomato", categoryId = catFood),
            CategorizationRuleEntity(keyword = "Reliance Smart", categoryId = catGroceries),
            CategorizationRuleEntity(keyword = "BigBasket", categoryId = catGroceries),
            CategorizationRuleEntity(keyword = "Amazon", categoryId = catShopping),
            CategorizationRuleEntity(keyword = "BESCOM", categoryId = catUtilities),
            CategorizationRuleEntity(keyword = "Electricity", categoryId = catUtilities),
            CategorizationRuleEntity(keyword = "Netflix", categoryId = catSubs),
            CategorizationRuleEntity(keyword = "Spotify", categoryId = catSubs),
            CategorizationRuleEntity(keyword = "Uber", categoryId = catTransport)
        ).forEach { dao.insertRule(it) }

        seedPortfolioDemoData(checkingId, homeLoanId, current)
    }

    private suspend fun seedPortfolioDemoData(checkingId: Long, homeLoanId: Long, current: MonthYear) {
        val now = System.currentTimeMillis()
        fun monthsAgo(m: Int, day: Int): Long {
            val ym = current.plusMonths(-m)
            return DateUtils.dayMillis(ym.year, ym.month, day)
        }

        // 1. Mutual Funds
        val ppfasId = portfolioDao.insertMutualFund(
            MutualFundEntity(
                amcName = "PPFAS Mutual Fund",
                schemeName = "Parag Parikh Flexi Cap Fund",
                category = MutualFundCategory.EQUITY,
                plan = MutualFundPlan.DIRECT,
                option = MutualFundOption.GROWTH,
                folioNumber = "10293847",
                currentNav = 88.45,
                navUpdatedAtMillis = now,
                sipEnabled = true,
                sipAmount = 8000.0,
                sipDay = 5
            )
        )
        portfolioDao.insertMutualFundTx(MutualFundTxEntity(fundId = ppfasId, txType = MutualFundTxType.PURCHASE, units = 1500.0, nav = 68.0, amount = 102000.0, dateMillis = monthsAgo(10, 4), linkedBankAccountId = checkingId, notes = "Initial lumpsum"))
        portfolioDao.insertMutualFundTx(MutualFundTxEntity(fundId = ppfasId, txType = MutualFundTxType.SIP, units = 100.0, nav = 80.0, amount = 8000.0, dateMillis = monthsAgo(2, 5), linkedBankAccountId = checkingId))
        portfolioDao.insertMutualFundTx(MutualFundTxEntity(fundId = ppfasId, txType = MutualFundTxType.SIP, units = 93.02, nav = 86.0, amount = 8000.0, dateMillis = monthsAgo(0, 5), linkedBankAccountId = checkingId))

        val niftyId = portfolioDao.insertMutualFund(
            MutualFundEntity(
                amcName = "HDFC Mutual Fund",
                schemeName = "HDFC Index Fund - Nifty 50 Plan",
                category = MutualFundCategory.INDEX,
                plan = MutualFundPlan.DIRECT,
                option = MutualFundOption.GROWTH,
                folioNumber = "88492011",
                currentNav = 236.20,
                navUpdatedAtMillis = now,
                sipEnabled = true,
                sipAmount = 5000.0,
                sipDay = 10
            )
        )
        portfolioDao.insertMutualFundTx(MutualFundTxEntity(fundId = niftyId, txType = MutualFundTxType.PURCHASE, units = 500.0, nav = 198.0, amount = 99000.0, dateMillis = monthsAgo(8, 10), linkedBankAccountId = checkingId))
        portfolioDao.insertMutualFundTx(MutualFundTxEntity(fundId = niftyId, txType = MutualFundTxType.SIP, units = 22.72, nav = 220.0, amount = 5000.0, dateMillis = monthsAgo(1, 10), linkedBankAccountId = checkingId))

        // 2. Stocks
        val tcsId = portfolioDao.insertStock(
            StockEntity(exchange = StockExchange.NSE, symbol = "TCS", companyName = "Tata Consultancy Services", currentPrice = 4145.0, priceUpdatedAtMillis = now)
        )
        portfolioDao.insertStockTrade(StockTradeEntity(stockId = tcsId, tradeType = StockTradeType.BUY, quantity = 15.0, price = 3720.0, charges = 65.0, dateMillis = monthsAgo(6, 12), linkedBankAccountId = checkingId))

        val hdfcStockId = portfolioDao.insertStock(
            StockEntity(exchange = StockExchange.NSE, symbol = "HDFCBANK", companyName = "HDFC Bank Ltd", currentPrice = 1742.0, priceUpdatedAtMillis = now)
        )
        portfolioDao.insertStockTrade(StockTradeEntity(stockId = hdfcStockId, tradeType = StockTradeType.BUY, quantity = 40.0, price = 1560.0, charges = 80.0, dateMillis = monthsAgo(7, 15), linkedBankAccountId = checkingId))

        // 3. PPF & Historical FY Rates
        PortfolioAnalytics.defaultHistoricalPpfRates.forEach { (fy, rate) ->
            portfolioDao.insertPpfRate(PpfFyRateEntity(financialYearStart = fy, annualRatePercent = rate))
        }
        val ppfId = portfolioDao.insertPpfAccount(
            PpfAccountEntity(accountName = "SBI Public Provident Fund", bankOrPostOffice = "SBI", accountNumberMasked = "XXXX 8842", startFinancialYear = 2021)
        )
        portfolioDao.insertPpfTx(PpfTxEntity(ppfAccountId = ppfId, txType = PpfTxType.DEPOSIT, amount = 180000.0, dateMillis = monthsAgo(14, 4), notes = "Prior FY cumulative principal"))
        portfolioDao.insertPpfTx(PpfTxEntity(ppfAccountId = ppfId, txType = PpfTxType.INTEREST_CREDIT, amount = 12780.0, dateMillis = Calendar.getInstance().apply { set(current.year, Calendar.MARCH, 31) }.timeInMillis, notes = "Annual FY Interest"))
        portfolioDao.insertPpfTx(PpfTxEntity(ppfAccountId = ppfId, txType = PpfTxType.DEPOSIT, amount = 25000.0, dateMillis = monthsAgo(2, 3), linkedBankAccountId = checkingId, notes = "Deposited before 5th"))
        portfolioDao.insertPpfTx(PpfTxEntity(ppfAccountId = ppfId, txType = PpfTxType.DEPOSIT, amount = 15000.0, dateMillis = monthsAgo(0, 4), linkedBankAccountId = checkingId, notes = "Monthly PPF contribution"))

        // 4. EPF
        val epfId = portfolioDao.insertEpfAccount(
            EpfAccountEntity(employerName = "FinTech Solutions India", uanMasked = "XXXX XXXX 7710", openingEmployeeShare = 185000.0, openingEmployerShare = 98000.0, openingPensionShare = 42000.0, interestRatePercent = 8.25)
        )
        portfolioDao.insertEpfEntry(EpfEntryEntity(epfAccountId = epfId, entryType = EpfEntryType.CONTRIBUTION, employeeAmount = 7200.0, employerEpfAmount = 5950.0, epsPensionAmount = 1250.0, vpfAmount = 2000.0, dateMillis = monthsAgo(1, 2), financialYearLabel = PortfolioAnalytics.formatFyLabel(PortfolioAnalytics.financialYearStart(now))))
        portfolioDao.insertEpfEntry(EpfEntryEntity(epfAccountId = epfId, entryType = EpfEntryType.CONTRIBUTION, employeeAmount = 7200.0, employerEpfAmount = 5950.0, epsPensionAmount = 1250.0, vpfAmount = 2000.0, dateMillis = monthsAgo(0, 2), financialYearLabel = PortfolioAnalytics.formatFyLabel(PortfolioAnalytics.financialYearStart(now))))
        portfolioDao.insertEpfEntry(EpfEntryEntity(epfAccountId = epfId, entryType = EpfEntryType.INTEREST_CREDIT, employeeAmount = 15200.0, employerEpfAmount = 8100.0, dateMillis = Calendar.getInstance().apply { set(current.year, Calendar.MARCH, 31) }.timeInMillis, financialYearLabel = "FY ${current.year - 1}-${(current.year % 100).toString().padStart(2, '0')}", notes = "Annual EPFO Interest Credit"))

        // 5. Other Assets
        portfolioDao.insertOtherAsset(OtherAssetEntity(kind = OtherAssetKind.GOLD, name = "22K Gold Jewellery & Coins", investedAmount = 195000.0, currentValue = 228000.0, goldPurityKarat = 22, goldGrams = 30.0, goldRatePerGram = 7600.0, notes = "30g 22K gold"))
        portfolioDao.insertOtherAsset(OtherAssetEntity(kind = OtherAssetKind.FD, name = "Equitas Fixed Deposit", investedAmount = 100000.0, currentValue = 107800.0, interestRatePercent = 7.8, notes = "1-Year Senior/Standard FD"))
        portfolioDao.insertOtherAsset(OtherAssetEntity(kind = OtherAssetKind.PROPERTY, name = "Bengaluru 2BHK Flat", investedAmount = 2400000.0, currentValue = 2850000.0, linkedLoanId = homeLoanId, notes = "Linked to Home Loan Principal"))
    }

    // Standard Finance CRUD
    suspend fun saveTransaction(tx: TransactionEntity) {
        if (tx.id == 0L) dao.insertTransaction(tx) else dao.updateTransaction(tx)
    }

    suspend fun deleteTransaction(tx: TransactionEntity) = dao.deleteTransaction(tx)
    suspend fun insertAccount(account: AccountEntity): Long = dao.insertAccount(account)
    suspend fun deleteAccount(account: AccountEntity) = dao.deleteAccount(account)
    suspend fun insertCreditCard(card: CreditCardEntity): Long = dao.insertCreditCard(card)
    suspend fun deleteCreditCard(card: CreditCardEntity) = dao.deleteCreditCard(card)
    suspend fun insertCategory(category: CategoryEntity): Long = dao.insertCategory(category)
    suspend fun saveBudget(budget: BudgetEntity) = dao.insertBudget(budget)
    suspend fun saveSavingsGoal(goal: SavingsGoalEntity) = dao.insertSavingsGoal(goal)
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = dao.deleteSavingsGoal(goal)
    suspend fun saveNetWorthItem(item: NetWorthItemEntity) = dao.insertNetWorthItem(item)
    suspend fun deleteNetWorthItem(item: NetWorthItemEntity) = dao.deleteNetWorthItem(item)
    suspend fun saveRule(rule: CategorizationRuleEntity) = dao.insertRule(rule)
    suspend fun deleteRule(rule: CategorizationRuleEntity) = dao.deleteRule(rule)
    suspend fun saveRecurringBill(bill: RecurringBillEntity) = dao.insertRecurringBill(bill)
    suspend fun deleteRecurringBill(bill: RecurringBillEntity) = dao.deleteRecurringBill(bill)
    suspend fun saveSettings(settings: UserSettingsEntity) = dao.upsertSettings(settings)

    // Portfolio CRUD
    suspend fun saveMutualFund(fund: MutualFundEntity): Long = portfolioDao.insertMutualFund(fund)
    suspend fun deleteMutualFund(fund: MutualFundEntity) {
        portfolioDao.deleteTxsForFund(fund.id)
        portfolioDao.deleteMutualFund(fund)
    }
    suspend fun saveMutualFundTx(tx: MutualFundTxEntity): Long = portfolioDao.insertMutualFundTx(tx)
    suspend fun deleteMutualFundTx(tx: MutualFundTxEntity) = portfolioDao.deleteMutualFundTx(tx)

    suspend fun saveStock(stock: StockEntity): Long = portfolioDao.insertStock(stock)
    suspend fun deleteStock(stock: StockEntity) {
        portfolioDao.deleteTradesForStock(stock.id)
        portfolioDao.deleteStock(stock)
    }
    suspend fun saveStockTrade(trade: StockTradeEntity): Long = portfolioDao.insertStockTrade(trade)
    suspend fun deleteStockTrade(trade: StockTradeEntity) = portfolioDao.deleteStockTrade(trade)

    suspend fun savePpfAccount(acc: PpfAccountEntity): Long = portfolioDao.insertPpfAccount(acc)
    suspend fun deletePpfAccount(acc: PpfAccountEntity) {
        portfolioDao.deleteTxsForPpf(acc.id)
        portfolioDao.deletePpfAccount(acc)
    }
    suspend fun savePpfTx(tx: PpfTxEntity): Long = portfolioDao.insertPpfTx(tx)
    suspend fun deletePpfTx(tx: PpfTxEntity) = portfolioDao.deletePpfTx(tx)
    suspend fun savePpfRate(rate: PpfFyRateEntity) = portfolioDao.insertPpfRate(rate)

    suspend fun saveEpfAccount(acc: EpfAccountEntity): Long = portfolioDao.insertEpfAccount(acc)
    suspend fun deleteEpfAccount(acc: EpfAccountEntity) {
        portfolioDao.deleteEntriesForEpf(acc.id)
        portfolioDao.deleteEpfAccount(acc)
    }
    suspend fun saveEpfEntry(entry: EpfEntryEntity): Long = portfolioDao.insertEpfEntry(entry)
    suspend fun deleteEpfEntry(entry: EpfEntryEntity) = portfolioDao.deleteEpfEntry(entry)

    suspend fun saveOtherAsset(asset: OtherAssetEntity): Long = portfolioDao.insertOtherAsset(asset)
    suspend fun deleteOtherAsset(asset: OtherAssetEntity) = portfolioDao.deleteOtherAsset(asset)
}
