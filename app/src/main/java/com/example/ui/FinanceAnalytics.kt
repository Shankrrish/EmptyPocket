package com.example.ui

import com.example.data.model.AccountEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.CreditCardEntity
import com.example.data.model.NetWorthItemEntity
import com.example.data.model.NetWorthSnapshotEntity
import com.example.data.model.RecurringBillEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.SplitAllocation
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettingsEntity
import com.example.util.MonthYear
import com.example.util.formatCurrency
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

enum class InsightSeverity { POSITIVE, INFO, WARNING, ALERT }

data class FinancialInsight(
    val id: String,
    val title: String,
    val message: String,
    val severity: InsightSeverity
)

data class SafeToSpendSummary(
    val monthlyIncome: Double,
    val upcomingBills: Double,
    val savingsTarget: Double,
    val currentSpending: Double,
    val safeToSpend: Double,
    val dailyAllowance: Double,
    val remainingDays: Int
)

data class CashFlowPoint(
    val dayOfMonth: Int,
    val projectedBalance: Double
)

data class CashFlowForecast(
    val currentLiquidBalance: Double,
    val expectedRemainingIncome: Double,
    val upcomingBillsAndEmis: Double,
    val plannedBudgetReserve: Double,
    val estimatedEndOfMonthBalance: Double,
    val endOfMonthLabel: String,
    val salaryCycleDaysLeft: Int,
    val points: List<CashFlowPoint>
)

data class RecurringCalendarEvent(
    val dayOfMonth: Int,
    val title: String,
    val amount: Double,
    val isCreditCardDue: Boolean,
    val isSubscription: Boolean,
    val amountChangedFrom: Double? = null
)

data class NetWorthSummary(
    val liquidBankAndCash: Double,
    val totalInvestmentsAndAssets: Double,
    val totalAssets: Double,
    val cardOutstandingLiabilities: Double,
    val loanLiabilities: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val historyPoints: List<NetWorthSnapshotEntity>
)

object FinanceAnalytics {

    /**
     * Expense recognition: cash/bank EXPENSES and CC_EXPENSEs recognize consumption spending immediately.
     * Transfers, CC_PAYMENTs, investment buys/redemptions, and non-cash revaluations NEVER inflate expenses.
     */
    fun isCountedExpense(tx: TransactionEntity): Boolean =
        !tx.isIgnored && (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.CC_EXPENSE)

    /**
     * Regular income recognition: INCOME counts toward monthly earned cashflow without being distorted
     * by investment redemptions or transfers.
     */
    fun isCountedIncome(tx: TransactionEntity): Boolean =
        !tx.isIgnored && tx.type == TransactionType.INCOME

    /**
     * Actual cash movement on a bank/cash/prepaid wallet account:
     * - INCOME, incoming TRANSFER, INVESTMENT_SELL, and INVESTMENT_INCOME add cash when posted
     * - EXPENSE, outgoing TRANSFER, CC_PAYMENT, and INVESTMENT_BUY deduct cash when posted
     * - CC_EXPENSE, VALUATION_ADJUSTMENT, and INTEREST_CREDIT never touch liquid bank/cash accounts
     */
    fun computeAccountBalance(account: AccountEntity, transactions: List<TransactionEntity>): Double {
        val incoming = transactions.filter {
            it.isPaid && (
                ((it.type == TransactionType.INCOME ||
                    it.type == TransactionType.INVESTMENT_SELL ||
                    it.type == TransactionType.INVESTMENT_INCOME) && it.accountId == account.id) ||
                    (it.type == TransactionType.TRANSFER && it.toAccountId == account.id)
                )
        }.sumOf { it.amount }
        val outgoing = transactions.filter {
            it.isPaid && it.accountId == account.id && (
                it.type == TransactionType.EXPENSE ||
                    it.type == TransactionType.TRANSFER ||
                    it.type == TransactionType.CC_PAYMENT ||
                    it.type == TransactionType.INVESTMENT_BUY
                )
        }.sumOf { it.amount }
        return account.initialBalance + incoming - outgoing
    }

    /**
     * Credit card liability:
     * - CC_EXPENSE increases card outstanding immediately
     * - CC_PAYMENT reduces card outstanding (and restores available limit) once paid
     */
    fun computeCardOutstanding(card: CreditCardEntity, transactions: List<TransactionEntity>): Double {
        val totalCharges = transactions
            .filter { it.creditCardId == card.id && (it.type == TransactionType.CC_EXPENSE || it.type == TransactionType.EXPENSE) }
            .sumOf { it.amount }
        val totalPayments = transactions
            .filter { it.isPaid && it.creditCardId == card.id && it.type == TransactionType.CC_PAYMENT }
            .sumOf { it.amount }
        return (totalCharges - totalPayments).coerceAtLeast(0.0)
    }

    fun computeCardAvailableLimit(card: CreditCardEntity, outstanding: Double): Double =
        (card.limitAmount - outstanding).coerceAtLeast(0.0)

    fun computeCardMinimumDue(card: CreditCardEntity, outstanding: Double): Double {
        if (outstanding <= 0.0) return 0.0
        if (card.minimumDueAmount > 0.0) return card.minimumDueAmount.coerceAtMost(outstanding)
        return maxOf(200.0, outstanding * 0.05).coerceAtMost(outstanding)
    }

    /**
     * Computes spending per category for a list of transactions, honoring SplitAllocations.
     */
    fun spendByCategory(transactions: List<TransactionEntity>): Map<Long, Double> {
        val map = mutableMapOf<Long, Double>()
        transactions.filter { isCountedExpense(it) }.forEach { tx ->
            val splits = SplitAllocation.decode(tx.splitSummary)
            if (splits.isNotEmpty()) {
                splits.forEach { split ->
                    map[split.categoryId] = (map[split.categoryId] ?: 0.0) + split.amount
                }
            } else if (tx.categoryId != null) {
                map[tx.categoryId] = (map[tx.categoryId] ?: 0.0) + tx.amount
            }
        }
        return map
    }

    fun computeSafeToSpend(
        selectedMonth: MonthYear,
        monthlyIncome: Double,
        monthTxs: List<TransactionEntity>,
        recurringBills: List<RecurringBillEntity>,
        settings: UserSettingsEntity
    ): SafeToSpendSummary {
        val unpaidTxs = monthTxs
            .filter { !it.isPaid && isCountedExpense(it) }
            .sumOf { it.amount }

        // Also count active monthly recurring bills not yet logged in this month's transactions
        val loggedTitles = monthTxs.map { it.title.lowercase() }
        val unloggedRecurring = recurringBills
            .filter { it.isActive && it.frequency == RecurringFrequency.MONTHLY }
            .filter { bill -> loggedTitles.none { it.contains(bill.title.lowercase()) || bill.title.lowercase().contains(it) } }
            .sumOf { it.amount }

        val upcomingBills = unpaidTxs + unloggedRecurring
        val currentSpending = monthTxs
            .filter { it.isPaid && isCountedExpense(it) }
            .sumOf { it.amount }

        val effectiveIncome = if (monthlyIncome > 0.0) monthlyIncome else 0.0
        val safeToSpend = (effectiveIncome - upcomingBills - settings.monthlySavingsTarget - currentSpending)

        val cal = Calendar.getInstance()
        val maxDay = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedMonth.year)
            set(Calendar.MONTH, selectedMonth.month - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentMonth = MonthYear.current()
        val remainingDays = when {
            selectedMonth == currentMonth -> (maxDay - cal.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
            else -> maxDay
        }
        val dailyAllowance = if (safeToSpend > 0.0) safeToSpend / remainingDays else 0.0

        return SafeToSpendSummary(
            monthlyIncome = effectiveIncome,
            upcomingBills = upcomingBills,
            savingsTarget = settings.monthlySavingsTarget,
            currentSpending = currentSpending,
            safeToSpend = safeToSpend,
            dailyAllowance = dailyAllowance,
            remainingDays = remainingDays
        )
    }

    fun computeCashFlowForecast(
        selectedMonth: MonthYear,
        currentLiquidBalance: Double,
        monthTxs: List<TransactionEntity>,
        recurringBills: List<RecurringBillEntity>,
        budgetProgressList: List<BudgetProgress>,
        goals: List<SavingsGoalEntity>,
        settings: UserSettingsEntity
    ): CashFlowForecast {
        val cal = Calendar.getInstance()
        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedMonth.year)
            set(Calendar.MONTH, selectedMonth.month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = if (selectedMonth == MonthYear.current()) cal.get(Calendar.DAY_OF_MONTH) else 1

        val expectedRemainingIncome = monthTxs
            .filter { !it.isPaid && it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val unpaidScheduledOutflows = monthTxs
            .filter { !it.isPaid && (it.type == TransactionType.EXPENSE || it.type == TransactionType.CC_PAYMENT || it.type == TransactionType.TRANSFER) }
            .sumOf { it.amount }

        val loggedTitles = monthTxs.map { it.title.lowercase() }
        val unloggedMonthlyBills = recurringBills
            .filter { it.isActive && it.frequency == RecurringFrequency.MONTHLY && it.type == TransactionType.EXPENSE }
            .filter { bill -> loggedTitles.none { it.contains(bill.title.lowercase()) || bill.title.lowercase().contains(it) } }
            .sumOf { it.amount }

        val upcomingBillsAndEmis = unpaidScheduledOutflows + unloggedMonthlyBills

        // Remaining unspent discretionary budget (capped at remaining non-bill budget)
        val remainingPlannedBudget = budgetProgressList.sumOf { bp ->
            val cap = bp.budget?.limitAmount ?: 0.0
            (cap - bp.spent).coerceAtLeast(0.0)
        } * 0.35 // Realistic remaining discretionary pace

        val remainingGoalContrib = goals
            .filter { it.isRecurringContribution && it.savedAmount < it.targetAmount }
            .sumOf { it.monthlyContribution } * 0.25

        val plannedReserve = (remainingPlannedBudget + remainingGoalContrib).roundToInt().toDouble()
        val estimatedEnd = currentLiquidBalance + expectedRemainingIncome - upcomingBillsAndEmis - plannedReserve

        // Build trajectory points across days in month
        val points = mutableListOf<CashFlowPoint>()
        var running = currentLiquidBalance
        val dailyDiscretionary = if (maxDay > today) plannedReserve / (maxDay - today) else 0.0
        for (day in today..maxDay) {
            if (day > today) running -= dailyDiscretionary
            monthTxs.filter { !it.isPaid }.forEach { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                if (txCal.get(Calendar.DAY_OF_MONTH) == day) {
                    when (tx.type) {
                        TransactionType.INCOME,
                        TransactionType.INVESTMENT_SELL,
                        TransactionType.INVESTMENT_INCOME -> running += tx.amount
                        TransactionType.EXPENSE,
                        TransactionType.CC_PAYMENT,
                        TransactionType.TRANSFER,
                        TransactionType.INVESTMENT_BUY -> running -= tx.amount
                        TransactionType.CC_EXPENSE,
                        TransactionType.VALUATION_ADJUSTMENT,
                        TransactionType.INTEREST_CREDIT -> Unit
                    }
                }
            }
            recurringBills
                .filter { it.isActive && it.frequency == RecurringFrequency.MONTHLY && it.type == TransactionType.EXPENSE && it.dueDay == day }
                .filter { bill -> loggedTitles.none { it.contains(bill.title.lowercase()) || bill.title.lowercase().contains(it) } }
                .forEach { running -= it.amount }

            if (day == today || day == maxDay || day % 5 == 0) {
                points.add(CashFlowPoint(day, running))
            }
        }
        if (points.isEmpty()) points.add(CashFlowPoint(today, currentLiquidBalance))

        val monthName = selectedMonth.displayLabel().substringBefore(" ")
        val endLabel = "$monthName $maxDay"
        val salaryDaysLeft = if (settings.salaryDay > today) {
            settings.salaryDay - today
        } else {
            (maxDay - today) + settings.salaryDay
        }

        return CashFlowForecast(
            currentLiquidBalance = currentLiquidBalance,
            expectedRemainingIncome = expectedRemainingIncome,
            upcomingBillsAndEmis = upcomingBillsAndEmis,
            plannedBudgetReserve = plannedReserve,
            estimatedEndOfMonthBalance = estimatedEnd,
            endOfMonthLabel = endLabel,
            salaryCycleDaysLeft = salaryDaysLeft,
            points = points
        )
    }

    fun computeInsights(
        selectedMonth: MonthYear,
        monthTxs: List<TransactionEntity>,
        allTransactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgetProgressList: List<BudgetProgress>,
        recurringBills: List<RecurringBillEntity>,
        monthlyIncome: Double,
        monthlyExpense: Double
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val prevMonth = selectedMonth.plusMonths(-1)
        val prevMonthTxs = allTransactions.filter { MonthYear.fromMillis(it.dateMillis) == prevMonth }

        // 1. Month-over-month category comparison using actual data
        val currentSpendMap = spendByCategory(monthTxs)
        val prevSpendMap = spendByCategory(prevMonthTxs)
        val expenseCats = categories.filter { it.type == CategoryType.EXPENSE }

        val biggestDelta = expenseCats.mapNotNull { cat ->
            val currAmt = currentSpendMap[cat.id] ?: 0.0
            val prevAmt = prevSpendMap[cat.id] ?: 0.0
            if (prevAmt > 200.0 && currAmt > 0.0) {
                val pctChange = ((currAmt - prevAmt) / prevAmt * 100.0).roundToInt()
                Triple(cat, pctChange, currAmt - prevAmt)
            } else null
        }.maxByOrNull { abs(it.second) }

        if (biggestDelta != null && abs(biggestDelta.second) >= 5) {
            val (cat, pct, _) = biggestDelta
            val prevName = prevMonth.displayLabel().substringBefore(" ")
            val verb = if (pct > 0) "increased $pct%" else "decreased ${abs(pct)}%"
            insights.add(
                FinancialInsight(
                    id = "mom_${cat.id}",
                    title = "${cat.name} Spend Trend",
                    message = "Your ${cat.name.lowercase()} expenses $verb compared with $prevName.",
                    severity = if (pct > 15) InsightSeverity.WARNING else InsightSeverity.POSITIVE
                )
            )
        }

        // 2. Budget overage alerts
        budgetProgressList.forEach { bp ->
            val cap = bp.budget?.limitAmount ?: 0.0
            if (cap > 0.0 && bp.spent > cap) {
                val over = bp.spent - cap
                insights.add(
                    FinancialInsight(
                        id = "over_${bp.category.id}",
                        title = "${bp.category.name} Over Budget",
                        message = "Your ${bp.category.name.lowercase()} expenses exceeded the budget by ${formatCurrency(over)}.",
                        severity = InsightSeverity.ALERT
                    )
                )
            }
        }

        // 3. Subscriptions yearly spend
        val yearlySubCost = recurringBills
            .filter { it.isActive && it.isSubscription }
            .sumOf { if (it.frequency == RecurringFrequency.ANNUAL) it.amount else it.amount * 12.0 }
        if (yearlySubCost > 0.0) {
            insights.add(
                FinancialInsight(
                    id = "subs_yearly",
                    title = "Annual Subscription Footprint",
                    message = "Your active subscriptions total ${formatCurrency(yearlySubCost)} per year.",
                    severity = InsightSeverity.INFO
                )
            )
        }

        // 4. Recurring bill amount-change alert
        recurringBills.filter { it.isActive && it.previousAmount != null && it.previousAmount != it.amount }
            .forEach { bill ->
                val prevAmt = bill.previousAmount ?: bill.amount
                val diff = bill.amount - prevAmt
                val dir = if (diff > 0) "increased" else "dropped"
                insights.add(
                    FinancialInsight(
                        id = "bill_change_${bill.id}",
                        title = "Recurring Amount Changed",
                        message = "${bill.title} $dir from ${formatCurrency(prevAmt)} to ${formatCurrency(bill.amount)}.",
                        severity = if (diff > 0) InsightSeverity.WARNING else InsightSeverity.POSITIVE
                    )
                )
            }

        // 5. Savings rate calculation from real income & expenses
        if (monthlyIncome > 0.0) {
            val savingsRate = (((monthlyIncome - monthlyExpense) / monthlyIncome) * 100.0).roundToInt()
            insights.add(
                FinancialInsight(
                    id = "savings_rate",
                    title = "Monthly Savings Rate",
                    message = if (savingsRate >= 0) {
                        "You saved $savingsRate% of your income this month (${formatCurrency(monthlyIncome - monthlyExpense)})."
                    } else {
                        "Expenses exceed income by ${formatCurrency(monthlyExpense - monthlyIncome)} this month."
                    },
                    severity = when {
                        savingsRate >= 20 -> InsightSeverity.POSITIVE
                        savingsRate >= 0 -> InsightSeverity.INFO
                        else -> InsightSeverity.ALERT
                    }
                )
            )
        }

        // 6. Unusual single-transaction spike detection
        val expenseTxs = monthTxs.filter { isCountedExpense(it) }
        if (expenseTxs.size >= 4) {
            val avg = expenseTxs.map { it.amount }.average()
            val nonHousingSpike = expenseTxs
                .filter { tx ->
                    val catName = categories.find { it.id == tx.categoryId }?.name ?: ""
                    !catName.equals("Housing", ignoreCase = true) && tx.amount > avg * 2.1
                }
                .maxByOrNull { it.amount }
            if (nonHousingSpike != null) {
                insights.add(
                    FinancialInsight(
                        id = "spike_${nonHousingSpike.id}",
                        title = "Unusual Spend Alert",
                        message = "${nonHousingSpike.title} (${formatCurrency(nonHousingSpike.amount)}) is more than 2x your typical expense.",
                        severity = InsightSeverity.WARNING
                    )
                )
            }
        }

        return insights
    }

    fun computeRecurringCalendarEvents(
        bills: List<RecurringBillEntity>,
        cards: List<CreditCardEntity>,
        cardSummaries: List<CardSummary> = emptyList()
    ): List<RecurringCalendarEvent> {
        val billEvents = bills.filter { it.isActive }.map { bill ->
            RecurringCalendarEvent(
                dayOfMonth = bill.dueDay.coerceIn(1, 31),
                title = bill.title,
                amount = bill.amount,
                isCreditCardDue = false,
                isSubscription = bill.isSubscription,
                amountChangedFrom = bill.previousAmount?.takeIf { it != bill.amount }
            )
        }
        val cardDueEvents = cards.map { card ->
            val openInvoice = cardSummaries.find { it.card.id == card.id }?.openInvoice ?: 0.0
            RecurringCalendarEvent(
                dayOfMonth = card.dueDay.coerceIn(1, 31),
                title = "${card.name} Due",
                amount = openInvoice,
                isCreditCardDue = true,
                isSubscription = false
            )
        }
        return (billEvents + cardDueEvents).sortedBy { it.dayOfMonth }
    }

    fun computeNetWorth(
        liquidBankAndCash: Double,
        cardOutstanding: Double,
        items: List<NetWorthItemEntity>,
        snapshots: List<NetWorthSnapshotEntity>,
        currentMonth: MonthYear,
        portfolioTotalValue: Double = 0.0
    ): NetWorthSummary {
        val manualAssets = items.filter { !it.category.isLiability }.sumOf { it.amount }
        val investmentsAndAssets = portfolioTotalValue + manualAssets
        val loans = items.filter { it.category.isLiability }.sumOf { it.amount }
        val totalAssets = liquidBankAndCash + investmentsAndAssets
        val totalLiabilities = cardOutstanding + loans
        val netWorth = totalAssets - totalLiabilities

        val currentSnap = NetWorthSnapshotEntity(
            monthYear = currentMonth.key(),
            label = currentMonth.displayLabel().take(3),
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities
        )
        val combinedHistory = snapshots
            .filter { it.monthYear != currentMonth.key() } + currentSnap

        return NetWorthSummary(
            liquidBankAndCash = liquidBankAndCash,
            totalInvestmentsAndAssets = investmentsAndAssets,
            totalAssets = totalAssets,
            cardOutstandingLiabilities = cardOutstanding,
            loanLiabilities = loans,
            totalLiabilities = totalLiabilities,
            netWorth = netWorth,
            historyPoints = combinedHistory
        )
    }
}
