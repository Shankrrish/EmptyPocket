package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EpfAccountEntity
import com.example.data.model.MutualFundEntity
import com.example.data.model.OtherAssetEntity
import com.example.data.model.OtherAssetKind
import com.example.data.model.PpfAccountEntity
import com.example.data.model.StockEntity
import com.example.ui.AllocationSlice
import com.example.ui.PortfolioAnalytics
import com.example.ui.PortfolioSummary
import com.example.ui.PpfSummary
import com.example.ui.components.AnimatedFinProgressBar
import com.example.ui.components.EmojiBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinSurfaceCard
import com.example.ui.theme.FinFlowTheme
import com.example.util.DateUtils
import com.example.util.formatCurrency

private enum class MfViewMode(val label: String) {
    FUNDS("Funds"),
    AMC("By AMC"),
    CATEGORY("By Category")
}

@Composable
fun MutualFundsTabContent(
    summary: PortfolioSummary,
    isBalanceHidden: Boolean,
    onAddFund: () -> Unit,
    onRecordTx: (MutualFundEntity) -> Unit,
    onDeleteFund: (MutualFundEntity) -> Unit,
    onOpenBulkNav: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    var viewMode by remember { mutableStateOf(MfViewMode.FUNDS) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portfolio_mf_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MfViewMode.entries.forEach { m ->
                        FilterChip(
                            selected = viewMode == m,
                            onClick = { viewMode = m },
                            label = { Text(m.label) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = onAddFund,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("mf_add_fund_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fund")
                }
            }
        }

        if (summary.mfHoldings.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No mutual funds added",
                    subtitle = "Add your Equity, Index, ELSS, Debt, or Hybrid schemes and track Purchase, SIP, Redemption & XIRR offline.",
                    emoji = "📊"
                )
            }
        } else if (viewMode != MfViewMode.FUNDS) {
            val slices = if (viewMode == MfViewMode.AMC) summary.mfAmcAllocation else summary.mfCategoryAllocation
            item {
                AllocationListCard(
                    title = if (viewMode == MfViewMode.AMC) "AMC Allocation" else "Category Allocation",
                    slices = slices,
                    isBalanceHidden = isBalanceHidden
                )
            }
        } else {
            items(summary.mfHoldings, key = { "mf_${it.fund.id}" }) { h ->
                val f = h.fund
                val gainColor = if (h.unrealizedGain >= 0) semanticColors.income else semanticColors.expense
                FinSurfaceCard(shape = RoundedCornerShape(18.dp), elevation = 0.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(emoji = "📊", tintColor = MaterialTheme.colorScheme.primary, size = 40.dp, emojiSize = 19.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(f.schemeName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = buildList {
                                        add(f.amcName)
                                        add(f.category.label)
                                        add("${f.plan.label} • ${f.option.label}")
                                        if (f.folioNumber.isNotBlank()) add("Folio ${f.folioNumber}")
                                    }.joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semanticColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatCurrency(h.currentValue, isBalanceHidden), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                val sign = if (h.unrealizedGain >= 0) "+" else ""
                                Text(
                                    text = "$sign${formatCurrency(h.unrealizedGain, isBalanceHidden)} (${"%.1f".format(h.unrealizedGainPercent)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = gainColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCol("Units", "%.3f".format(h.totalUnits))
                            StatCol("Avg NAV", "₹${"%.2f".format(h.avgBuyNav)}")
                            StatCol("Curr NAV", "₹${"%.2f".format(f.currentNav)}")
                            StatCol("XIRR", h.xirrPercent?.let { "${"%.1f".format(it)}%" } ?: "—")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = semanticColors.surfaceElevated,
                                    border = BorderStroke(0.75.dp, semanticColors.hairlineBorder),
                                    onClick = onOpenBulkNav
                                ) {
                                    Text(
                                        text = "Manual NAV • ${DateUtils.formatDateHeader(f.navUpdatedAtMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semanticColors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (f.sipEnabled && f.sipAmount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "SIP ₹${f.sipAmount.toInt()}/m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { onRecordTx(f) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("mf_add_tx_${f.id}")
                                ) { Text("+ Tx", fontSize = 12.sp) }
                                IconButton(onClick = { onDeleteFund(f) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${f.schemeName}", tint = semanticColors.textSecondary)
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
fun StocksTabContent(
    summary: PortfolioSummary,
    isBalanceHidden: Boolean,
    onAddStock: () -> Unit,
    onRecordTrade: (StockEntity) -> Unit,
    onDeleteStock: (StockEntity) -> Unit,
    onOpenBulkPrices: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portfolio_stocks_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NSE / BSE Equity Holdings", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = onAddStock,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("stock_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stock")
                }
            }
        }
        if (summary.stockHoldings.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No stock holdings",
                    subtitle = "Add NSE/BSE stocks and log Buy/Sell trades with automatic FIFO cost & P&L.",
                    emoji = "📈"
                )
            }
        } else {
            items(summary.stockHoldings, key = { "st_${it.stock.id}" }) { h ->
                val s = h.stock
                val gainColor = if (h.unrealizedPnl >= 0) semanticColors.income else semanticColors.expense
                FinSurfaceCard(shape = RoundedCornerShape(18.dp), elevation = 0.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(emoji = "📈", tintColor = Color(0xFF0284C7), size = 40.dp, emojiSize = 19.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${s.symbol} (${s.exchange.label})", style = MaterialTheme.typography.titleMedium)
                                Text(s.companyName, style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatCurrency(h.currentValue, isBalanceHidden), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                val sign = if (h.unrealizedPnl >= 0) "+" else ""
                                Text(
                                    text = "$sign${formatCurrency(h.unrealizedPnl, isBalanceHidden)} (${"%.1f".format(h.unrealizedPnlPercent)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = gainColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCol("Qty", h.quantity.toInt().toString())
                            StatCol("Avg Buy", "₹${"%.2f".format(h.avgBuyPrice)}")
                            StatCol("Manual CMP", "₹${"%.2f".format(s.currentPrice)}")
                            StatCol("Realized", formatCurrency(h.realizedPnl, isBalanceHidden))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = semanticColors.surfaceElevated,
                                border = BorderStroke(0.75.dp, semanticColors.hairlineBorder),
                                onClick = onOpenBulkPrices
                            ) {
                                Text(
                                    text = "Updated ${DateUtils.formatDateHeader(s.priceUpdatedAtMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semanticColors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { onRecordTrade(s) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("stock_add_trade_${s.id}")
                                ) { Text("+ Trade", fontSize = 12.sp) }
                                IconButton(onClick = { onDeleteStock(s) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${s.symbol}", tint = semanticColors.textSecondary)
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
fun PpfEpfTabContent(
    summary: PortfolioSummary,
    isBalanceHidden: Boolean,
    onAddPpf: () -> Unit,
    onRecordPpfTx: (PpfAccountEntity) -> Unit,
    onDeletePpf: (PpfAccountEntity) -> Unit,
    onCreditFyInterest: (PpfSummary) -> Unit,
    onEditPpfRates: () -> Unit,
    onAddEpf: () -> Unit,
    onRecordEpfEntry: (EpfAccountEntity) -> Unit,
    onDeleteEpf: (EpfAccountEntity) -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portfolio_ppf_epf_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏛️ Public Provident Fund (PPF)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onEditPpfRates,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("ppf_rates_button")
                    ) { Text("FY Rates", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = onAddPpf,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("ppf_add_account_button")
                    ) { Text("+ PPF", fontSize = 12.sp) }
                }
            }
        }

        if (summary.ppfSummaries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No PPF account",
                    subtitle = "Track 5th-to-month-end minimum balance interest, ₹1.5L FY cap, and 15-year maturity.",
                    emoji = "🏛️"
                )
            }
        } else {
            items(summary.ppfSummaries, key = { "ppf_${it.account.id}" }) { p ->
                val acc = p.account
                val capRatio = (p.currentFyDeposited / PortfolioAnalytics.PPF_ANNUAL_LIMIT).toFloat().coerceIn(0f, 1f)
                FinSurfaceCard(shape = RoundedCornerShape(18.dp), elevation = 0.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(emoji = "🏛️", tintColor = semanticColors.income, size = 40.dp, emojiSize = 19.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.accountName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${acc.bankOrPostOffice} • Started FY ${acc.startFinancialYear} • Matures FY ${p.maturityFyStart} (${p.currentFyRatePercent}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semanticColors.textSecondary
                                )
                            }
                            Text(formatCurrency(p.currentBalance, isBalanceHidden), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = semanticColors.income)
                        }
                        AnimatedFinProgressBar(progress = capRatio, color = MaterialTheme.colorScheme.primary, height = 7.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "FY Deposited: ${formatCurrency(p.currentFyDeposited, isBalanceHidden)} / ₹1,50,000",
                                style = MaterialTheme.typography.labelSmall,
                                color = semanticColors.textSecondary
                            )
                            Text(
                                text = "Left: ${formatCurrency(p.currentFyRemaining, isBalanceHidden)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCol("Credited Int.", formatCurrency(p.totalCreditedInterest, isBalanceHidden))
                            StatCol("Est. FY Int.", formatCurrency(p.estimatedUncreditedInterestForFy, isBalanceHidden))
                            StatCol(
                                "Eligibility",
                                when {
                                    p.isMatured -> "Matured"
                                    p.isPartialWithdrawalEligible -> "Withdrawal OK"
                                    p.isLoanEligible -> "Loan OK"
                                    else -> "Lock-in"
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (p.estimatedUncreditedInterestForFy > 1.0) {
                                OutlinedButton(
                                    onClick = { onCreditFyInterest(p) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("ppf_credit_interest_${acc.id}")
                                ) { Text("Credit FY Interest", fontSize = 11.sp) }
                            } else Spacer(modifier = Modifier.width(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { onRecordPpfTx(acc) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("ppf_add_tx_${acc.id}")
                                ) { Text("+ Deposit/Tx", fontSize = 12.sp) }
                                IconButton(onClick = { onDeletePpf(acc) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${acc.accountName}", tint = semanticColors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛡️ Employees' Provident Fund (EPF)", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = onAddEpf,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("epf_add_account_button")
                ) { Text("+ EPF", fontSize = 12.sp) }
            }
        }

        if (summary.epfSummaries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No EPF account",
                    subtitle = "Track Employee Share, Employer EPF, EPS Pension, VPF, and Annual Interest separately.",
                    emoji = "🛡️"
                )
            }
        } else {
            items(summary.epfSummaries, key = { "epf_${it.account.id}" }) { e ->
                val acc = e.account
                FinSurfaceCard(shape = RoundedCornerShape(18.dp), elevation = 0.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(emoji = "🛡️", tintColor = Color(0xFF2F6F6D), size = 40.dp, emojiSize = 19.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.employerName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (acc.uanMasked.isNotBlank()) "UAN ${acc.uanMasked} • ${acc.interestRatePercent}% p.a." else "${acc.interestRatePercent}% p.a.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semanticColors.textSecondary
                                )
                            }
                            Text(formatCurrency(e.totalBalance, isBalanceHidden), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = semanticColors.income)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCol("Employee", formatCurrency(e.employeeTotal, isBalanceHidden))
                            StatCol("Employer EPF", formatCurrency(e.employerEpfTotal, isBalanceHidden))
                            StatCol("EPS Pension", formatCurrency(e.epsPensionTotal, isBalanceHidden))
                            StatCol("VPF+Int", formatCurrency(e.vpfTotal + e.interestTotal, isBalanceHidden))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { onRecordEpfEntry(acc) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("epf_add_entry_${acc.id}")
                            ) { Text("+ Entry", fontSize = 12.sp) }
                            IconButton(onClick = { onDeleteEpf(acc) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete ${acc.employerName}", tint = semanticColors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtherAssetsTabContent(
    summary: PortfolioSummary,
    isBalanceHidden: Boolean,
    onAddAsset: () -> Unit,
    onDeleteAsset: (OtherAssetEntity) -> Unit,
    onUpdateGoldRates: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val hasGold = summary.otherAssetSummaries.any { it.asset.kind == OtherAssetKind.GOLD }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portfolio_other_assets_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NPS, FD, RD, Gold & Property", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasGold) {
                        OutlinedButton(
                            onClick = onUpdateGoldRates,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("Gold Rate", fontSize = 12.sp) }
                    }
                    OutlinedButton(
                        onClick = onAddAsset,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("other_asset_add_button")
                    ) { Text("+ Asset", fontSize = 12.sp) }
                }
            }
        }
        if (summary.otherAssetSummaries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No other assets yet",
                    subtitle = "Add NPS, Fixed Deposits, RDs, Gold (18K/22K/24K grams), Property with linked loans, or Custom assets.",
                    emoji = "🪙"
                )
            }
        } else {
            items(summary.otherAssetSummaries, key = { "oa_${it.asset.id}" }) { s ->
                val a = s.asset
                val kindLabel = if (a.kind == OtherAssetKind.CUSTOM && a.customCategoryName.isNotBlank()) a.customCategoryName else a.kind.label
                FinSurfaceCard(shape = RoundedCornerShape(18.dp), elevation = 0.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(emoji = a.kind.emoji, tintColor = Color(0xFFC97A16), size = 40.dp, emojiSize = 19.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = buildList {
                                        add(kindLabel)
                                        if (a.kind == OtherAssetKind.GOLD && a.goldGrams > 0) add("${a.goldGrams}g • ${a.goldPurityKarat}K")
                                        if (a.interestRatePercent > 0) add("${a.interestRatePercent}% p.a.")
                                    }.joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semanticColors.textSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatCurrency(s.computedValue, isBalanceHidden), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                val gSign = if (s.gain >= 0) "+" else ""
                                Text(
                                    text = "$gSign${formatCurrency(s.gain, isBalanceHidden)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (s.gain >= 0) semanticColors.income else semanticColors.expense
                                )
                            }
                            IconButton(onClick = { onDeleteAsset(a) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete ${a.name}", tint = semanticColors.textSecondary)
                            }
                        }
                        if (s.linkedLoanAmount > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = semanticColors.surfaceElevated,
                                border = BorderStroke(0.75.dp, semanticColors.hairlineBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Linked Loan: ${formatCurrency(s.linkedLoanAmount, isBalanceHidden)} • Net Equity: ${formatCurrency(s.netEquity, isBalanceHidden)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllocationListCard(
    title: String,
    slices: List<AllocationSlice>,
    isBalanceHidden: Boolean
) {
    val semanticColors = FinFlowTheme.colors
    FinSurfaceCard(shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            slices.forEachIndexed { idx, s ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(emoji = s.emoji, tintColor = Color(s.colorHex), size = 32.dp, emojiSize = 15.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(s.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatCurrency(s.value, isBalanceHidden), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text("${(s.percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                    }
                }
                if (idx < slices.lastIndex) HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
            }
        }
    }
}

@Composable
private fun StatCol(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FinFlowTheme.colors.textSecondary)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
