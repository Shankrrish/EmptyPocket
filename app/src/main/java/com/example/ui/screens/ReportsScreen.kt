package com.example.ui.screens

import com.example.util.MonthYear
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.FinanceAnalytics
import com.example.ui.FinanceUiState
import com.example.ui.components.AnimatedFinProgressBar
import com.example.ui.components.EmojiBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinSurfaceCard
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.accountEmoji
import com.example.ui.components.resolveCategoryEmoji
import com.example.ui.theme.FinFlowTheme
import com.example.util.DateUtils
import com.example.util.formatCurrency

import androidx.activity.compose.BackHandler

private enum class ReportTab(val label: String) {
    SPENDING("💸 Spending"),
    ACCOUNTS("🏦 Accounts"),
    NET_WORTH("💎 Net Worth")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: FinanceUiState,
    onBack: () -> Unit,
    onMonthSelected: (MonthYear) -> Unit,
    onToggleVisibility: () -> Unit,
    onOpenNetWorthItems: () -> Unit,
    onOpenPortfolio: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember { mutableStateOf(ReportTab.SPENDING) }
    var includeManualPropertyAssets by remember { mutableStateOf(true) }
    val semanticColors = FinFlowTheme.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Financial Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reports_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthSelectorBar(
                selectedMonth = state.selectedMonth,
                isBalanceHidden = state.isBalanceHidden,
                onMonthSelected = onMonthSelected,
                onToggleVisibility = onToggleVisibility
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("report_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (selectedTab) {
                ReportTab.SPENDING -> SpendingReportTab(state = state)
                ReportTab.ACCOUNTS -> AccountsReportTab(state = state)
                ReportTab.NET_WORTH -> {
                    val nw = state.netWorthSummary
                    val manualItemsAssets = state.netWorthItems.filter { !it.category.isLiability }.sumOf { it.amount }
                    val displayedAssets = if (includeManualPropertyAssets) {
                        nw.totalAssets
                    } else {
                        (nw.totalAssets - manualItemsAssets).coerceAtLeast(0.0)
                    }
                    val displayedNetWorth = displayedAssets - nw.totalLiabilities

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("report_net_worth_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            FinSurfaceCard(
                                shape = RoundedCornerShape(22.dp),
                                backgroundBrush = Brush.linearGradient(
                                    listOf(semanticColors.heroGradientStart, semanticColors.heroGradientEnd)
                                ),
                                borderColor = semanticColors.heroBorder,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Consolidated Net Worth",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = semanticColors.textSecondary
                                        )
                                        Text(
                                            text = "As of ${DateUtils.formatDateHeader(System.currentTimeMillis())}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = semanticColors.textSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatCurrency(displayedNetWorth, state.isBalanceHidden),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Assets", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                                            Text(
                                                text = formatCurrency(displayedAssets, state.isBalanceHidden),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = semanticColors.income
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Liabilities", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                                            Text(
                                                text = formatCurrency(nw.totalLiabilities, state.isBalanceHidden),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = semanticColors.expense
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            FinSurfaceCard(shape = RoundedCornerShape(16.dp), elevation = 0.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Include Manual Asset Overrides", style = MaterialTheme.typography.titleSmall)
                                        Text("Loans & liabilities always remain included", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                                    }
                                    Switch(checked = includeManualPropertyAssets, onCheckedChange = { includeManualPropertyAssets = it })
                                }
                            }
                        }

                        item {
                            FinSurfaceCard(shape = RoundedCornerShape(18.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Net Worth Composition (Zero Double-Counting)", style = MaterialTheme.typography.titleMedium)
                                    BreakdownRow("🏦 Liquid Banks, Cash & E-Wallets", nw.liquidBankAndCash, state.isBalanceHidden, semanticColors.income)
                                    HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                                    BreakdownRow("📈 Investment Portfolio (MF, Stocks, PPF, EPF, Gold)", state.portfolioSummary.totalCurrentValue, state.isBalanceHidden, semanticColors.income)
                                    if (includeManualPropertyAssets && manualItemsAssets > 0.0) {
                                        HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                                        BreakdownRow("🏠 Manual Assets", manualItemsAssets, state.isBalanceHidden, semanticColors.income)
                                    }
                                    HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                                    BreakdownRow("💳 Credit Card Outstanding", -nw.cardOutstandingLiabilities, state.isBalanceHidden, semanticColors.expense)
                                    val loans = (nw.totalLiabilities - nw.cardOutstandingLiabilities).coerceAtLeast(0.0)
                                    if (loans > 0.0) {
                                        HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                                        BreakdownRow("🏛️ Loans & Liabilities", -loans, state.isBalanceHidden, semanticColors.expense)
                                    }
                                }
                            }
                        }

                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = onOpenPortfolio, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                    Text("Open Portfolio")
                                }
                                OutlinedButton(onClick = onOpenNetWorthItems, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                    Text("Edit Loans / Items")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingReportTab(state: FinanceUiState) {
    val semanticColors = FinFlowTheme.colors
    val netSavings = state.monthlyIncome - state.monthlyExpense
    val savingsRate = if (state.monthlyIncome > 0) ((netSavings / state.monthlyIncome) * 100.0) else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("report_spending_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FinSurfaceCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Monthly Cashflow Summary", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Income", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            Text(formatCurrency(state.monthlyIncome, state.isBalanceHidden), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = semanticColors.income)
                        }
                        Column {
                            Text("Expenses", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            Text(formatCurrency(state.monthlyExpense, state.isBalanceHidden), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = semanticColors.expense)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Savings Rate", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            Text("${"%.1f".format(savingsRate)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (netSavings >= 0) semanticColors.income else semanticColors.expense)
                        }
                    }
                }
            }
        }

        item {
            FinSurfaceCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Expenses by Category", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (state.categorySlices.isEmpty()) {
                        EmptyStateCard(title = "No expenses this month", subtitle = "Recorded expenses and card purchases appear here.", emoji = "💸")
                    } else {
                        InteractiveDonutChart(slices = state.categorySlices, isBalanceHidden = state.isBalanceHidden)
                    }
                }
            }
        }

        items(state.categorySlices, key = { "cat_rep_${it.category.id}" }) { slice ->
            FinSurfaceCard(shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(
                        emoji = resolveCategoryEmoji(slice.category.iconName, slice.category.name),
                        tintColor = Color(slice.category.colorHex),
                        size = 40.dp,
                        emojiSize = 19.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(slice.category.name, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        AnimatedFinProgressBar(progress = slice.percentage, color = Color(slice.category.colorHex), height = 6.dp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatCurrency(slice.amount, state.isBalanceHidden), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("${(slice.percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountsReportTab(state: FinanceUiState) {
    val semanticColors = FinFlowTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("report_accounts_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Monthly Account Cashflow", style = MaterialTheme.typography.titleMedium) }
        items(state.accountSummaries, key = { "acc_rep_${it.account.id}" }) { s ->
            val txs = state.monthlyTransactions
            val inflow = txs.sumOf { tx ->
                when {
                    tx.accountId == s.account.id && (tx.type == TransactionType.INCOME || tx.type == TransactionType.INVESTMENT_SELL || tx.type == TransactionType.INVESTMENT_INCOME) -> tx.amount
                    tx.toAccountId == s.account.id && tx.type == TransactionType.TRANSFER -> tx.amount
                    else -> 0.0
                }
            }
            val outflow = txs.sumOf { tx ->
                when {
                    tx.accountId == s.account.id && (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT || tx.type == TransactionType.INVESTMENT_BUY) -> tx.amount
                    else -> 0.0
                }
            }
            FinSurfaceCard(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(emoji = accountEmoji(s.account.type, s.account.emoji), tintColor = Color(s.account.colorHex), size = 40.dp, emojiSize = 19.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(s.account.name, style = MaterialTheme.typography.titleSmall)
                            Text(s.account.type.displayLabel, style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                        }
                        Text(formatCurrency(s.currentBalance, state.isBalanceHidden), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Inflow: +${formatCurrency(inflow, state.isBalanceHidden)}", style = MaterialTheme.typography.labelSmall, color = semanticColors.income)
                        Text("Outflow: -${formatCurrency(outflow, state.isBalanceHidden)}", style = MaterialTheme.typography.labelSmall, color = semanticColors.expense)
                    }
                }
            }
        }
        if (state.cardSummaries.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(4.dp)); Text("Credit Card Utilization", style = MaterialTheme.typography.titleMedium) }
            items(state.cardSummaries, key = { "cc_rep_${it.card.id}" }) { cs ->
                val util = if (cs.card.limitAmount > 0) (cs.openInvoice / cs.card.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                FinSurfaceCard(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("💳 ${cs.card.name}", style = MaterialTheme.typography.titleSmall)
                            Text("${(util * 100).toInt()}% used", style = MaterialTheme.typography.labelSmall, color = if (util > 0.8f) semanticColors.warning else semanticColors.textSecondary)
                        }
                        AnimatedFinProgressBar(progress = util, color = if (util > 0.8f) semanticColors.warning else MaterialTheme.colorScheme.primary, height = 6.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Due: ${formatCurrency(cs.openInvoice, state.isBalanceHidden)}", style = MaterialTheme.typography.labelSmall, color = semanticColors.expense)
                            Text("Avail: ${formatCurrency(cs.availableLimit, state.isBalanceHidden)}", style = MaterialTheme.typography.labelSmall, color = semanticColors.income)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Double, hidden: Boolean, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(formatCurrency(amount, hidden), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
    }
}
