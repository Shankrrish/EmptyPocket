package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FinanceDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AppFontStyle
import com.example.data.model.AppThemeMode
import com.example.data.model.AssetLiabilityCategory
import com.example.data.model.BudgetEntity
import com.example.data.model.CategorizationRuleEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.CreditCardEntity
import com.example.data.model.EpfAccountEntity
import com.example.data.model.EpfEntryEntity
import com.example.data.model.HomeCardConfig
import com.example.data.model.HomeCardId
import com.example.data.model.MutualFundEntity
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
import com.example.data.model.SplitAllocation
import com.example.data.model.StockEntity
import com.example.data.model.StockTradeEntity
import com.example.data.model.StockTradeType
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettingsEntity
import com.example.data.repository.FinanceRepository
import com.example.util.BackupManager
import com.example.util.IndiaCaptureParser
import com.example.util.MonthYear
import com.example.util.ParsedCaptureCandidate
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FinanceDatabase.getInstance(application)
    private val repository = FinanceRepository(db.financeDao(), db.portfolioDao())

    private val selectedMonth = MutableStateFlow(MonthYear.current())
    private val isBalanceHidden = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val txFilter = MutableStateFlow(TxFilter.ALL)
    private val selectedCategoryFilterId = MutableStateFlow<Long?>(null)
    private val selectedCardFilterId = MutableStateFlow<Long?>(null)
    private val selectedAccountFilterId = MutableStateFlow<Long?>(null)

    private val _sessionUnlocked = MutableStateFlow(false)
    val sessionUnlocked: StateFlow<Boolean> = _sessionUnlocked.asStateFlow()

    init {
        viewModelScope.launch { repository.seedDemoDataIfEmpty() }
    }

    private val baseDataFlow = combine(
        repository.accounts,
        repository.creditCards,
        repository.categories,
        repository.transactions,
        repository.budgets
    ) { accounts, cards, categories, transactions, budgets ->
        BaseDataBundle(accounts, cards, categories, transactions, budgets)
    }

    private val extendedDataFlow = combine(
        repository.savingsGoals,
        repository.netWorthItems,
        repository.netWorthSnapshots,
        repository.rules,
        repository.recurringBills,
        repository.settings
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ExtendedDataBundle(
            goals = values[0] as List<SavingsGoalEntity>,
            nwItems = values[1] as List<NetWorthItemEntity>,
            nwSnaps = values[2] as List<NetWorthSnapshotEntity>,
            rules = values[3] as List<CategorizationRuleEntity>,
            bills = values[4] as List<RecurringBillEntity>,
            settings = (values[5] as UserSettingsEntity?) ?: UserSettingsEntity()
        )
    }

    private val mfAndStockFlow = combine(
        repository.mutualFunds,
        repository.mutualFundTxs,
        repository.stocks,
        repository.stockTrades,
        repository.otherAssets
    ) { mfs, mfTxs, stocks, trades, others ->
        PortfolioDataBundle(mutualFunds = mfs, mfTxs = mfTxs, stocks = stocks, stockTrades = trades, otherAssets = others)
    }

    private val ppfAndEpfFlow = combine(
        repository.ppfAccounts,
        repository.ppfTxs,
        repository.ppfRates,
        repository.epfAccounts,
        repository.epfEntries
    ) { ppfs, ppfTxs, ppfRates, epfs, epfEntries ->
        PortfolioDataBundle(ppfAccounts = ppfs, ppfTxs = ppfTxs, ppfRates = ppfRates, epfAccounts = epfs, epfEntries = epfEntries)
    }

    private val portfolioDataFlow = combine(mfAndStockFlow, ppfAndEpfFlow) { a, b ->
        a.copy(
            ppfAccounts = b.ppfAccounts,
            ppfTxs = b.ppfTxs,
            ppfRates = b.ppfRates,
            epfAccounts = b.epfAccounts,
            epfEntries = b.epfEntries
        )
    }

    private val uiControlsFlow = combine(
        selectedMonth,
        isBalanceHidden,
        searchQuery,
        txFilter,
        combine(selectedCategoryFilterId, selectedCardFilterId, selectedAccountFilterId) { catId, cardId, accId ->
            Triple(catId, cardId, accId)
        }
    ) { month, hidden, query, filter, contextIds ->
        UiControlsBundle(month, hidden, query, filter, contextIds.first, contextIds.second, contextIds.third)
    }

    val uiState: StateFlow<FinanceUiState> = combine(
        baseDataFlow,
        extendedDataFlow,
        portfolioDataFlow,
        uiControlsFlow
    ) { base, ext, pf, ctrl ->
        val month = ctrl.selectedMonth
        val monthTxs = base.transactions.filter { MonthYear.fromMillis(it.dateMillis) == month }

        val allAccountSummaries = base.accounts.map { acc ->
            AccountSummary(acc, FinanceAnalytics.computeAccountBalance(acc, base.transactions))
        }
        val activeAccountSummaries = allAccountSummaries.filter { !it.account.isArchived }
        val activeAccounts = base.accounts.filter { !it.isArchived }
        val totalAccountBalance = activeAccountSummaries.sumOf { it.currentBalance }

        val monthlyIncome = monthTxs.filter { FinanceAnalytics.isCountedIncome(it) }.sumOf { it.amount }
        val monthlyExpense = monthTxs.filter { FinanceAnalytics.isCountedExpense(it) }.sumOf { it.amount }

        val allCardSummaries = base.cards.map { card ->
            val out = FinanceAnalytics.computeCardOutstanding(card, base.transactions)
            CardSummary(
                card = card,
                openInvoice = out,
                availableLimit = FinanceAnalytics.computeCardAvailableLimit(card, out),
                minimumDue = FinanceAnalytics.computeCardMinimumDue(card, out)
            )
        }
        val activeCardSummaries = allCardSummaries.filter { !it.card.isArchived }
        val activeCards = base.cards.filter { !it.isArchived }

        val expenseCategories = base.categories.filter { it.type == CategoryType.EXPENSE }
        val monthBudgets = base.budgets.filter { it.monthYear == month.key() }
        val spendByCat = FinanceAnalytics.spendByCategory(monthTxs)

        val budgetList = expenseCategories.map { cat ->
            BudgetProgress(cat, monthBudgets.find { it.categoryId == cat.id }, spendByCat[cat.id] ?: 0.0)
        }
        val totalPlanned = monthBudgets.sumOf { it.limitAmount }
        val totalSpentOnBudgeted = budgetList.filter { it.budget != null }.sumOf { it.spent }

        val slices = expenseCategories.mapNotNull { cat ->
            val amt = spendByCat[cat.id] ?: 0.0
            if (amt > 0.0 && monthlyExpense > 0.0) CategorySlice(cat, amt, (amt / monthlyExpense).toFloat()) else null
        }.sortedByDescending { it.amount }

        val duplicateIds = monthTxs.filter { tx ->
            IndiaCaptureParser.isPotentialDuplicate(tx.amount, tx.dateMillis, tx.title, monthTxs, excludeId = tx.id)
        }.map { it.id }.toSet()

        val filtered = monthTxs.filter { tx ->
            val matchesFilter = when (ctrl.txFilter) {
                TxFilter.ALL -> true
                TxFilter.EXPENSE -> FinanceAnalytics.isCountedExpense(tx)
                TxFilter.INCOME -> tx.type == TransactionType.INCOME
                TxFilter.CARD -> tx.type == TransactionType.CC_EXPENSE || tx.type == TransactionType.CC_PAYMENT
                TxFilter.TRANSFER_INVEST -> tx.type == TransactionType.TRANSFER ||
                    tx.type == TransactionType.INVESTMENT_BUY ||
                    tx.type == TransactionType.INVESTMENT_SELL ||
                    tx.type == TransactionType.INVESTMENT_INCOME ||
                    tx.type == TransactionType.VALUATION_ADJUSTMENT ||
                    tx.type == TransactionType.INTEREST_CREDIT
                TxFilter.PENDING -> !tx.isPaid
                TxFilter.DUPLICATES -> tx.id in duplicateIds
            }
            val matchesCategory = ctrl.categoryFilterId == null ||
                tx.categoryId == ctrl.categoryFilterId ||
                SplitAllocation.decode(tx.splitSummary).any { it.categoryId == ctrl.categoryFilterId }
            val matchesCard = ctrl.cardFilterId == null || tx.creditCardId == ctrl.cardFilterId
            val matchesAccount = ctrl.accountFilterId == null ||
                tx.accountId == ctrl.accountFilterId ||
                tx.toAccountId == ctrl.accountFilterId
            val matchesQuery = ctrl.searchQuery.isBlank() ||
                tx.title.contains(ctrl.searchQuery, ignoreCase = true) ||
                tx.notes.contains(ctrl.searchQuery, ignoreCase = true) ||
                tx.tags.contains(ctrl.searchQuery, ignoreCase = true)
            matchesFilter && matchesCategory && matchesCard && matchesAccount && matchesQuery
        }

        val portfolioSummary = PortfolioAnalytics.computePortfolioSummary(
            mutualFunds = pf.mutualFunds,
            mfTxs = pf.mfTxs,
            stocks = pf.stocks,
            stockTrades = pf.stockTrades,
            ppfAccounts = pf.ppfAccounts,
            ppfTxs = pf.ppfTxs,
            ppfRates = pf.ppfRates,
            epfAccounts = pf.epfAccounts,
            epfEntries = pf.epfEntries,
            otherAssets = pf.otherAssets,
            netWorthItems = ext.nwItems
        )

        val safeToSpend = FinanceAnalytics.computeSafeToSpend(month, monthlyIncome, monthTxs, ext.bills, ext.settings)
        val cashFlow = FinanceAnalytics.computeCashFlowForecast(month, totalAccountBalance, monthTxs, ext.bills, budgetList, ext.goals, ext.settings)
        val insights = FinanceAnalytics.computeInsights(month, monthTxs, base.transactions, base.categories, budgetList, ext.bills, monthlyIncome, monthlyExpense)
        val recurringEvents = FinanceAnalytics.computeRecurringCalendarEvents(ext.bills, activeCards, activeCardSummaries)
        val monthlySubs = ext.bills.filter { it.isActive && it.isSubscription }.sumOf {
            if (it.frequency == RecurringFrequency.MONTHLY) it.amount else it.amount / 12.0
        }

        val totalCardOutstanding = activeCardSummaries.sumOf { it.openInvoice }
        val netWorth = FinanceAnalytics.computeNetWorth(
            liquidBankAndCash = totalAccountBalance,
            cardOutstanding = totalCardOutstanding,
            items = ext.nwItems,
            snapshots = ext.nwSnaps,
            currentMonth = month,
            portfolioTotalValue = portfolioSummary.totalCurrentValue
        )

        FinanceUiState(
            selectedMonth = month,
            isBalanceHidden = ctrl.isBalanceHidden,
            searchQuery = ctrl.searchQuery,
            txFilter = ctrl.txFilter,
            accounts = activeAccounts,
            accountSummaries = activeAccountSummaries,
            allAccountSummaries = allAccountSummaries,
            creditCards = activeCards,
            categories = base.categories,
            allTransactions = base.transactions,
            monthlyTransactions = monthTxs,
            filteredTransactions = filtered,
            duplicateTransactionIds = duplicateIds,
            totalBalance = totalAccountBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            cardSummaries = activeCardSummaries,
            allCardSummaries = allCardSummaries,
            totalBudgetPlanned = totalPlanned,
            totalBudgetSpent = totalSpentOnBudgeted,
            categorySlices = slices,
            budgetProgressList = budgetList,
            savingsGoals = ext.goals,
            netWorthItems = ext.nwItems,
            netWorthSummary = netWorth,
            rules = ext.rules,
            recurringBills = ext.bills,
            recurringCalendarEvents = recurringEvents,
            monthlySubscriptionsTotal = monthlySubs,
            annualSubscriptionsTotal = monthlySubs * 12.0,
            settings = ext.settings,
            homeCards = HomeCardConfig.decode(ext.settings.homeCardsConfig),
            selectedCategoryFilterId = ctrl.categoryFilterId,
            selectedCardFilterId = ctrl.cardFilterId,
            selectedAccountFilterId = ctrl.accountFilterId,
            safeToSpend = safeToSpend,
            cashFlowForecast = cashFlow,
            insights = insights,
            portfolioSummary = portfolioSummary,
            ppfRates = pf.ppfRates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceUiState())

    fun selectMonth(month: MonthYear) { selectedMonth.value = month }
    fun toggleBalanceVisibility() { isBalanceHidden.value = !isBalanceHidden.value }
    fun setSearchQuery(q: String) { searchQuery.value = q }
    fun setTxFilter(f: TxFilter) { txFilter.value = f }

    fun filterTransactionsByCategory(categoryId: Long) {
        txFilter.value = TxFilter.EXPENSE
        selectedCategoryFilterId.value = categoryId
        selectedCardFilterId.value = null
        selectedAccountFilterId.value = null
    }

    fun filterTransactionsByCard(cardId: Long) {
        txFilter.value = TxFilter.ALL
        selectedCardFilterId.value = cardId
        selectedCategoryFilterId.value = null
        selectedAccountFilterId.value = null
    }

    fun filterTransactionsByAccount(accountId: Long) {
        txFilter.value = TxFilter.ALL
        selectedAccountFilterId.value = accountId
        selectedCategoryFilterId.value = null
        selectedCardFilterId.value = null
    }

    fun clearContextualFilters() {
        selectedCategoryFilterId.value = null
        selectedCardFilterId.value = null
        selectedAccountFilterId.value = null
    }

    fun togglePaid(tx: TransactionEntity) = viewModelScope.launch {
        repository.saveTransaction(tx.copy(isPaid = !tx.isPaid))
    }

    fun saveTransaction(tx: TransactionEntity, learnRule: Boolean = false) = viewModelScope.launch {
        repository.saveTransaction(tx)
        if (tx.id == 0L && tx.isPaid && tx.linkedLoanId != null) {
            uiState.value.netWorthItems.find { it.id == tx.linkedLoanId }?.let { loan ->
                repository.saveNetWorthItem(loan.copy(amount = (loan.amount - tx.amount).coerceAtLeast(0.0)))
            }
        }
        if (learnRule && tx.categoryId != null && tx.title.trim().length >= 3) {
            val keyword = tx.title.trim().split(" ").take(2).joinToString(" ")
            val alreadyExists = uiState.value.rules.any { it.keyword.equals(keyword, ignoreCase = true) }
            if (!alreadyExists && keyword.length >= 3) {
                repository.saveRule(CategorizationRuleEntity(keyword = keyword, categoryId = tx.categoryId, isAutoLearned = true))
            }
        }
    }

    fun deleteTransaction(tx: TransactionEntity) = viewModelScope.launch { repository.deleteTransaction(tx) }

    // Account & Card Management
    fun addAccount(name: String, type: AccountType, balance: Double) = viewModelScope.launch {
        val color = when (type) {
            AccountType.CHECKING -> 0xFF556B2F
            AccountType.CASH -> 0xFF3B6E22
            AccountType.SAVINGS -> 0xFF2F6F6D
            AccountType.E_WALLET -> 0xFF6D28D9
        }
        repository.insertAccount(AccountEntity(name = name, type = type, initialBalance = balance, colorHex = color))
    }

    fun saveAccount(account: AccountEntity) = viewModelScope.launch { repository.insertAccount(account) }
    fun toggleArchiveAccount(account: AccountEntity) = viewModelScope.launch { repository.insertAccount(account.copy(isArchived = !account.isArchived)) }
    fun deleteAccount(acc: AccountEntity) = viewModelScope.launch { repository.deleteAccount(acc) }

    fun addCreditCard(name: String, brand: String, limit: Double, closingDay: Int, dueDay: Int) = viewModelScope.launch {
        repository.insertCreditCard(CreditCardEntity(name = name, brand = brand, limitAmount = limit, closingDay = closingDay, dueDay = dueDay))
    }

    fun saveCreditCard(card: CreditCardEntity) = viewModelScope.launch { repository.insertCreditCard(card) }
    fun toggleArchiveCard(card: CreditCardEntity) = viewModelScope.launch { repository.insertCreditCard(card.copy(isArchived = !card.isArchived)) }
    fun deleteCreditCard(card: CreditCardEntity) = viewModelScope.launch { repository.deleteCreditCard(card) }

    fun addCategory(name: String, type: CategoryType, emoji: String, colorHex: Long, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return@launch
        val id = repository.insertCategory(
            CategoryEntity(name = cleanName, type = type, iconName = emoji.ifBlank { if (type == CategoryType.INCOME) "💰" else "🛍️" }, colorHex = colorHex)
        )
        onCreated(id)
    }

    fun setBudget(categoryId: Long, limit: Double, existingId: Long = 0) = viewModelScope.launch {
        repository.saveBudget(BudgetEntity(id = existingId, categoryId = categoryId, monthYear = selectedMonth.value.key(), limitAmount = limit))
    }

    // Settings & Homepage Customization
    fun updateSavingsTarget(target: Double) = viewModelScope.launch {
        repository.saveSettings(uiState.value.settings.copy(monthlySavingsTarget = target.coerceAtLeast(0.0)))
    }
    fun updateSmsConsent(granted: Boolean) = viewModelScope.launch {
        repository.saveSettings(uiState.value.settings.copy(smsAutoScanConsent = granted))
    }
    fun moveHomeCard(index: Int, delta: Int) = viewModelScope.launch {
        val current = HomeCardConfig.decode(uiState.value.settings.homeCardsConfig).toMutableList()
        val target = index + delta
        if (index in current.indices && target in current.indices) {
            val item = current.removeAt(index)
            current.add(target, item)
            repository.saveSettings(uiState.value.settings.copy(homeCardsConfig = HomeCardConfig.encode(current)))
        }
    }
    fun toggleHomeCardVisibility(id: HomeCardId) = viewModelScope.launch {
        val current = HomeCardConfig.decode(uiState.value.settings.homeCardsConfig).map {
            if (it.id == id) it.copy(visible = !it.visible) else it
        }
        repository.saveSettings(uiState.value.settings.copy(homeCardsConfig = HomeCardConfig.encode(current)))
    }
    fun resetHomeCardsLayout() = viewModelScope.launch {
        repository.saveSettings(uiState.value.settings.copy(homeCardsConfig = HomeCardConfig.encode(HomeCardConfig.defaultList())))
    }

    // Privacy Lock & Backup
    fun setPinAndEnableLock(pin: String, enableBiometrics: Boolean) = viewModelScope.launch {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin(pin, salt)
        _sessionUnlocked.value = true
        repository.saveSettings(uiState.value.settings.copy(appLockEnabled = true, biometricEnabled = enableBiometrics, pinSalt = salt, pinHash = hash))
    }
    fun toggleBiometricLock(enabled: Boolean) = viewModelScope.launch {
        repository.saveSettings(uiState.value.settings.copy(biometricEnabled = enabled))
    }
    fun disableAppLock() = viewModelScope.launch {
        _sessionUnlocked.value = false
        repository.saveSettings(uiState.value.settings.copy(appLockEnabled = false, biometricEnabled = false, pinHash = "", pinSalt = ""))
    }
    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch {
        repository.saveSettings(uiState.value.settings.copy(themeMode = mode.name))
    }
    fun setFontStyle(style: AppFontStyle) = viewModelScope.launch {
        repository.saveSettings(uiState.value.settings.copy(fontStyle = style.name))
    }
    fun verifyPinAndUnlock(pin: String): Boolean {
        val ok = SecurityUtils.verifyPin(pin, uiState.value.settings.pinSalt, uiState.value.settings.pinHash)
        if (ok) _sessionUnlocked.value = true
        return ok
    }
    fun markBiometricUnlocked() { _sessionUnlocked.value = true }
    fun lockNow() { if (uiState.value.settings.appLockEnabled && uiState.value.settings.pinHash.isNotEmpty()) _sessionUnlocked.value = false }

    fun exportBackupJson(onReady: (String) -> Unit) = viewModelScope.launch {
        onReady(BackupManager.encodeBackupJson(repository.createBackupData()))
    }
    fun exportTransactionsCsv(onReady: (String) -> Unit) = viewModelScope.launch {
        val s = uiState.value
        onReady(BackupManager.exportTransactionsCsv(s.allTransactions, s.categories, s.accounts, s.creditCards))
    }
    fun restoreBackupJson(rawJson: String, onResult: (Boolean, String) -> Unit) = viewModelScope.launch {
        runCatching { repository.restoreBackupData(BackupManager.decodeBackupJson(rawJson)) }
            .onSuccess { onResult(true, "Backup restored successfully.") }
            .onFailure { err -> onResult(false, "Could not parse backup file: ${err.message ?: "Invalid JSON"}") }
    }
    fun deleteAllFinancialData() = viewModelScope.launch {
        _sessionUnlocked.value = true
        repository.clearAllUserData()
    }

    // Goals, Net Worth, Rules, Bills, Capture
    fun addOrUpdateGoal(goal: SavingsGoalEntity) = viewModelScope.launch { repository.saveSavingsGoal(goal) }
    fun contributeToGoal(goal: SavingsGoalEntity, contribution: Double) = viewModelScope.launch {
        if (contribution > 0.0) repository.saveSavingsGoal(goal.copy(savedAmount = goal.savedAmount + contribution))
    }
    fun deleteGoal(goal: SavingsGoalEntity) = viewModelScope.launch { repository.deleteSavingsGoal(goal) }
    fun saveNetWorthItem(name: String, category: AssetLiabilityCategory, amount: Double, id: Long = 0) = viewModelScope.launch {
        repository.saveNetWorthItem(NetWorthItemEntity(id = id, name = name, category = category, amount = amount))
    }
    fun deleteNetWorthItem(item: NetWorthItemEntity) = viewModelScope.launch { repository.deleteNetWorthItem(item) }
    fun saveRule(keyword: String, categoryId: Long) = viewModelScope.launch {
        if (keyword.isNotBlank()) repository.saveRule(CategorizationRuleEntity(keyword = keyword.trim(), categoryId = categoryId))
    }
    fun toggleRule(rule: CategorizationRuleEntity) = viewModelScope.launch { repository.saveRule(rule.copy(isEnabled = !rule.isEnabled)) }
    fun deleteRule(rule: CategorizationRuleEntity) = viewModelScope.launch { repository.deleteRule(rule) }
    fun saveRecurringBill(bill: RecurringBillEntity) = viewModelScope.launch { repository.saveRecurringBill(bill) }
    fun deleteRecurringBill(bill: RecurringBillEntity) = viewModelScope.launch { repository.deleteRecurringBill(bill) }

    fun importCapturedCandidates(candidates: List<ParsedCaptureCandidate>, defaultAccountId: Long?, defaultCardId: Long?) = viewModelScope.launch {
        candidates.forEach { item ->
            repository.saveTransaction(
                TransactionEntity(
                    title = item.title, amount = item.amount, type = item.type, dateMillis = item.dateMillis, isPaid = true,
                    categoryId = item.suggestedCategoryId,
                    accountId = if (item.type == TransactionType.CC_EXPENSE) null else defaultAccountId,
                    creditCardId = if (item.type == TransactionType.CC_EXPENSE) defaultCardId else null,
                    notes = "Captured via ${item.detectedBankOrRail}", tags = item.tags
                )
            )
        }
    }

    // Portfolio Actions (Offline + Unified Ledger Synced)
    fun saveMutualFund(fund: MutualFundEntity) = viewModelScope.launch { repository.saveMutualFund(fund) }
    fun deleteMutualFund(fund: MutualFundEntity) = viewModelScope.launch { repository.deleteMutualFund(fund) }
    fun recordMutualFundTx(tx: MutualFundTxEntity, fundTitle: String, syncBankLedger: Boolean) = viewModelScope.launch {
        repository.saveMutualFundTx(tx)
        if (syncBankLedger && tx.linkedBankAccountId != null && (tx.txType == MutualFundTxType.PURCHASE || tx.txType == MutualFundTxType.SIP || tx.txType == MutualFundTxType.REDEMPTION)) {
            val ledgerType = if (tx.txType == MutualFundTxType.REDEMPTION) TransactionType.INVESTMENT_SELL else TransactionType.INVESTMENT_BUY
            repository.saveTransaction(
                TransactionEntity(
                    title = "$fundTitle (${tx.txType.label})",
                    amount = tx.amount,
                    type = ledgerType,
                    dateMillis = tx.dateMillis,
                    isPaid = true,
                    categoryId = null,
                    accountId = tx.linkedBankAccountId,
                    creditCardId = null,
                    notes = tx.notes.ifBlank { "Units: ${"%.3f".format(tx.units)} @ ₹${tx.nav}" },
                    linkedAssetType = "MF",
                    linkedAssetId = tx.fundId
                )
            )
        }
    }
    fun updateMutualFundNavs(navUpdates: Map<Long, Double>) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        uiState.value.portfolioSummary.mfHoldings.forEach { h ->
            val updatedNav = navUpdates[h.fund.id]
            if (updatedNav != null && updatedNav > 0.0 && updatedNav != h.fund.currentNav) {
                repository.saveMutualFund(h.fund.copy(currentNav = updatedNav, navUpdatedAtMillis = now))
            }
        }
    }

    fun saveStock(stock: StockEntity) = viewModelScope.launch { repository.saveStock(stock) }
    fun deleteStock(stock: StockEntity) = viewModelScope.launch { repository.deleteStock(stock) }
    fun recordStockTrade(trade: StockTradeEntity, stockSymbol: String, syncBankLedger: Boolean) = viewModelScope.launch {
        repository.saveStockTrade(trade)
        if (syncBankLedger && trade.linkedBankAccountId != null) {
            val gross = trade.quantity * trade.price
            val ledgerAmt = if (trade.tradeType == StockTradeType.BUY) gross + trade.charges else (gross - trade.charges).coerceAtLeast(0.0)
            repository.saveTransaction(
                TransactionEntity(
                    title = "$stockSymbol (${trade.tradeType.label} ${trade.quantity.toInt()})",
                    amount = ledgerAmt,
                    type = if (trade.tradeType == StockTradeType.BUY) TransactionType.INVESTMENT_BUY else TransactionType.INVESTMENT_SELL,
                    dateMillis = trade.dateMillis,
                    isPaid = true,
                    categoryId = null,
                    accountId = trade.linkedBankAccountId,
                    creditCardId = null,
                    notes = trade.notes,
                    linkedAssetType = "STOCK",
                    linkedAssetId = trade.stockId
                )
            )
        }
    }
    fun updateStockPrices(priceUpdates: Map<Long, Double>) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        uiState.value.portfolioSummary.stockHoldings.forEach { h ->
            val updated = priceUpdates[h.stock.id]
            if (updated != null && updated > 0.0 && updated != h.stock.currentPrice) {
                repository.saveStock(h.stock.copy(currentPrice = updated, priceUpdatedAtMillis = now))
            }
        }
    }

    fun savePpfAccount(acc: PpfAccountEntity) = viewModelScope.launch { repository.savePpfAccount(acc) }
    fun deletePpfAccount(acc: PpfAccountEntity) = viewModelScope.launch { repository.deletePpfAccount(acc) }
    fun recordPpfTx(tx: PpfTxEntity, accountName: String, syncBankLedger: Boolean) = viewModelScope.launch {
        repository.savePpfTx(tx)
        if (syncBankLedger && tx.linkedBankAccountId != null && tx.txType != PpfTxType.INTEREST_CREDIT) {
            val ledgerType = if (tx.txType == PpfTxType.DEPOSIT) TransactionType.INVESTMENT_BUY else TransactionType.INVESTMENT_SELL
            repository.saveTransaction(
                TransactionEntity(
                    title = "$accountName (${tx.txType.label})",
                    amount = tx.amount,
                    type = ledgerType,
                    dateMillis = tx.dateMillis,
                    isPaid = true,
                    categoryId = null,
                    accountId = tx.linkedBankAccountId,
                    creditCardId = null,
                    notes = tx.notes,
                    linkedAssetType = "PPF",
                    linkedAssetId = tx.ppfAccountId
                )
            )
        } else if (tx.txType == PpfTxType.INTEREST_CREDIT) {
            repository.saveTransaction(
                TransactionEntity(
                    title = "$accountName Interest Credit",
                    amount = tx.amount,
                    type = TransactionType.INTEREST_CREDIT,
                    dateMillis = tx.dateMillis,
                    isPaid = true,
                    categoryId = null,
                    accountId = null,
                    creditCardId = null,
                    notes = tx.notes.ifBlank { "Annual PPF Interest compounded March 31" },
                    linkedAssetType = "PPF",
                    linkedAssetId = tx.ppfAccountId
                )
            )
        }
    }
    fun creditCurrentFyEstimatedPpfInterest(summary: PpfSummary) = viewModelScope.launch {
        val interest = summary.estimatedUncreditedInterestForFy
        if (interest <= 0.0) return@launch
        val fyStart = PortfolioAnalytics.financialYearStart(System.currentTimeMillis())
        val mar31Millis = Calendar.getInstance().apply { set(fyStart + 1, Calendar.MARCH, 31, 18, 0, 0) }.timeInMillis
        val tx = PpfTxEntity(
            ppfAccountId = summary.account.id,
            txType = PpfTxType.INTEREST_CREDIT,
            amount = interest,
            dateMillis = mar31Millis,
            notes = "${PortfolioAnalytics.formatFyLabel(fyStart)} Annual Compounded Interest (${summary.currentFyRatePercent}%)"
        )
        repository.savePpfTx(tx)
        repository.saveTransaction(
            TransactionEntity(
                title = "${summary.account.accountName} Interest",
                amount = interest,
                type = TransactionType.INTEREST_CREDIT,
                dateMillis = mar31Millis,
                isPaid = true,
                categoryId = null,
                accountId = null,
                creditCardId = null,
                notes = tx.notes,
                linkedAssetType = "PPF",
                linkedAssetId = summary.account.id
            )
        )
    }
    fun updatePpfFyRate(fyStart: Int, ratePercent: Double) = viewModelScope.launch {
        repository.savePpfRate(PpfFyRateEntity(fyStart, ratePercent))
    }

    fun saveEpfAccount(acc: EpfAccountEntity) = viewModelScope.launch { repository.saveEpfAccount(acc) }
    fun deleteEpfAccount(acc: EpfAccountEntity) = viewModelScope.launch { repository.deleteEpfAccount(acc) }
    fun recordEpfEntry(entry: EpfEntryEntity) = viewModelScope.launch { repository.saveEpfEntry(entry) }

    fun saveOtherAsset(asset: OtherAssetEntity) = viewModelScope.launch { repository.saveOtherAsset(asset) }
    fun deleteOtherAsset(asset: OtherAssetEntity) = viewModelScope.launch { repository.deleteOtherAsset(asset) }
    fun updateGoldRates(rate24K: Double?, rate22K: Double?, rate18K: Double?) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        uiState.value.portfolioSummary.otherAssetSummaries
            .filter { it.asset.kind == OtherAssetKind.GOLD }
            .forEach { s ->
                val newRate = when (s.asset.goldPurityKarat) {
                    24 -> rate24K
                    22 -> rate22K
                    18 -> rate18K
                    else -> rate22K
                }
                if (newRate != null && newRate > 0.0) {
                    repository.saveOtherAsset(
                        s.asset.copy(
                            goldRatePerGram = newRate,
                            currentValue = s.asset.goldGrams * newRate,
                            updatedAtMillis = now
                        )
                    )
                }
            }
    }

    fun resetDemoData() = viewModelScope.launch { repository.resetDemoData() }
}

private data class BaseDataBundle(
    val accounts: List<AccountEntity>,
    val cards: List<CreditCardEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>
)

private data class ExtendedDataBundle(
    val goals: List<SavingsGoalEntity>,
    val nwItems: List<NetWorthItemEntity>,
    val nwSnaps: List<NetWorthSnapshotEntity>,
    val rules: List<CategorizationRuleEntity>,
    val bills: List<RecurringBillEntity>,
    val settings: UserSettingsEntity
)

private data class UiControlsBundle(
    val selectedMonth: MonthYear,
    val isBalanceHidden: Boolean,
    val searchQuery: String,
    val txFilter: TxFilter,
    val categoryFilterId: Long?,
    val cardFilterId: Long?,
    val accountFilterId: Long?
)
