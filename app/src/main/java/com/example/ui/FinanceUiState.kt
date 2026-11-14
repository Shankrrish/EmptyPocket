package com.example.ui

import com.example.data.model.AccountEntity
import com.example.data.model.CategorizationRuleEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.EpfAccountEntity
import com.example.data.model.EpfEntryEntity
import com.example.data.model.HomeCardConfig
import com.example.data.model.MutualFundEntity
import com.example.data.model.MutualFundTxEntity
import com.example.data.model.NetWorthItemEntity
import com.example.data.model.NetWorthSnapshotEntity
import com.example.data.model.OtherAssetEntity
import com.example.data.model.PpfAccountEntity
import com.example.data.model.PpfFyRateEntity
import com.example.data.model.PpfTxEntity
import com.example.data.model.RecurringBillEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.StockEntity
import com.example.data.model.StockTradeEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserSettingsEntity
import com.example.util.MonthYear

enum class TxFilter { ALL, EXPENSE, INCOME, CARD, TRANSFER_INVEST, PENDING, DUPLICATES }

data class CategorySlice(
    val category: CategoryEntity,
    val amount: Double,
    val percentage: Float
)

data class CardSummary(
    val card: CreditCardEntity,
    val openInvoice: Double,
    val availableLimit: Double,
    val minimumDue: Double = 0.0
)

data class AccountSummary(
    val account: AccountEntity,
    val currentBalance: Double
)

data class BudgetProgress(
    val category: CategoryEntity,
    val budget: com.example.data.model.BudgetEntity?,
    val spent: Double
)

data class FinanceUiState(
    val selectedMonth: MonthYear = MonthYear.current(),
    val isBalanceHidden: Boolean = false,
    val searchQuery: String = "",
    val txFilter: TxFilter = TxFilter.ALL,
    val accounts: List<AccountEntity> = emptyList(),
    val accountSummaries: List<AccountSummary> = emptyList(),
    val allAccountSummaries: List<AccountSummary> = emptyList(),
    val creditCards: List<CreditCardEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val monthlyTransactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val duplicateTransactionIds: Set<Long> = emptySet(),
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val cardSummaries: List<CardSummary> = emptyList(),
    val allCardSummaries: List<CardSummary> = emptyList(),
    val totalBudgetPlanned: Double = 0.0,
    val totalBudgetSpent: Double = 0.0,
    val categorySlices: List<CategorySlice> = emptyList(),
    val budgetProgressList: List<BudgetProgress> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val netWorthItems: List<NetWorthItemEntity> = emptyList(),
    val netWorthSummary: NetWorthSummary = NetWorthSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList()),
    val rules: List<CategorizationRuleEntity> = emptyList(),
    val recurringBills: List<RecurringBillEntity> = emptyList(),
    val recurringCalendarEvents: List<RecurringCalendarEvent> = emptyList(),
    val monthlySubscriptionsTotal: Double = 0.0,
    val annualSubscriptionsTotal: Double = 0.0,
    val settings: UserSettingsEntity = UserSettingsEntity(),
    val homeCards: List<HomeCardConfig> = HomeCardConfig.defaultList(),
    val selectedCategoryFilterId: Long? = null,
    val selectedCardFilterId: Long? = null,
    val selectedAccountFilterId: Long? = null,
    val safeToSpend: SafeToSpendSummary = SafeToSpendSummary(0.0, 0.0, 20000.0, 0.0, 0.0, 0.0, 15),
    val cashFlowForecast: CashFlowForecast = CashFlowForecast(0.0, 0.0, 0.0, 0.0, 0.0, "", 15, emptyList()),
    val insights: List<FinancialInsight> = emptyList(),
    val portfolioSummary: PortfolioSummary = PortfolioSummary(),
    val ppfRates: List<PpfFyRateEntity> = emptyList()
)

data class PortfolioDataBundle(
    val mutualFunds: List<MutualFundEntity> = emptyList(),
    val mfTxs: List<MutualFundTxEntity> = emptyList(),
    val stocks: List<StockEntity> = emptyList(),
    val stockTrades: List<StockTradeEntity> = emptyList(),
    val ppfAccounts: List<PpfAccountEntity> = emptyList(),
    val ppfTxs: List<PpfTxEntity> = emptyList(),
    val ppfRates: List<PpfFyRateEntity> = emptyList(),
    val epfAccounts: List<EpfAccountEntity> = emptyList(),
    val epfEntries: List<EpfEntryEntity> = emptyList(),
    val otherAssets: List<OtherAssetEntity> = emptyList()
)
