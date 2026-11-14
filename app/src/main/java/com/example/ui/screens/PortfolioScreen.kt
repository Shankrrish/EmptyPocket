package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EpfAccountEntity
import com.example.data.model.EpfEntryEntity
import com.example.data.model.MutualFundEntity
import com.example.data.model.MutualFundTxEntity
import com.example.data.model.OtherAssetEntity
import com.example.data.model.PpfAccountEntity
import com.example.data.model.PpfTxEntity
import com.example.data.model.StockEntity
import com.example.data.model.StockTradeEntity
import com.example.ui.FinanceUiState
import com.example.ui.PortfolioSummary
import com.example.ui.PpfSummary
import com.example.ui.components.EmojiBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinSurfaceCard
import com.example.ui.theme.FinFlowTheme
import com.example.util.formatCurrency

import androidx.activity.compose.BackHandler

private enum class PortfolioSectionTab(val label: String) {
    OVERVIEW("📊 Overview"),
    MUTUAL_FUNDS("📈 Mutual Funds"),
    STOCKS("🏢 Stocks"),
    PPF_EPF("🏛️ PPF & EPF"),
    OTHER("🪙 Other Assets")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    state: FinanceUiState,
    onBack: () -> Unit,
    onSaveMutualFund: (MutualFundEntity) -> Unit,
    onDeleteMutualFund: (MutualFundEntity) -> Unit,
    onRecordMfTx: (MutualFundTxEntity, String, Boolean) -> Unit,
    onUpdateMfNavs: (Map<Long, Double>) -> Unit,
    onSaveStock: (StockEntity) -> Unit,
    onDeleteStock: (StockEntity) -> Unit,
    onRecordStockTrade: (StockTradeEntity, String, Boolean) -> Unit,
    onUpdateStockPrices: (Map<Long, Double>) -> Unit,
    onSavePpfAccount: (PpfAccountEntity) -> Unit,
    onDeletePpfAccount: (PpfAccountEntity) -> Unit,
    onRecordPpfTx: (PpfTxEntity, String, Boolean) -> Unit,
    onCreditPpfFyInterest: (PpfSummary) -> Unit,
    onUpdatePpfRate: (Int, Double) -> Unit,
    onSaveEpfAccount: (EpfAccountEntity) -> Unit,
    onDeleteEpfAccount: (EpfAccountEntity) -> Unit,
    onRecordEpfEntry: (EpfEntryEntity) -> Unit,
    onSaveOtherAsset: (OtherAssetEntity) -> Unit,
    onDeleteOtherAsset: (OtherAssetEntity) -> Unit,
    onUpdateGoldRates: (Double?, Double?, Double?) -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember { mutableStateOf(PortfolioSectionTab.OVERVIEW) }
    var showBulkPriceSheet by remember { mutableStateOf(false) }

    var showAddMfSheet by remember { mutableStateOf(false) }
    var activeMfForTx by remember { mutableStateOf<MutualFundEntity?>(null) }

    var showAddStockSheet by remember { mutableStateOf(false) }
    var activeStockForTrade by remember { mutableStateOf<StockEntity?>(null) }

    var showAddPpfSheet by remember { mutableStateOf(false) }
    var activePpfForTx by remember { mutableStateOf<PpfAccountEntity?>(null) }
    var showPpfRatesSheet by remember { mutableStateOf(false) }

    var showAddEpfSheet by remember { mutableStateOf(false) }
    var activeEpfForEntry by remember { mutableStateOf<EpfAccountEntity?>(null) }

    var showAddOtherAssetSheet by remember { mutableStateOf(false) }

    if (showBulkPriceSheet) {
        BulkPriceUpdateSheet(
            summary = state.portfolioSummary,
            onDismiss = { showBulkPriceSheet = false },
            onSaveAll = { mfMap, stockMap, g24, g22, g18 ->
                if (mfMap.isNotEmpty()) onUpdateMfNavs(mfMap)
                if (stockMap.isNotEmpty()) onUpdateStockPrices(stockMap)
                if (g24 != null || g22 != null || g18 != null) onUpdateGoldRates(g24, g22, g18)
                showBulkPriceSheet = false
            }
        )
    }

    if (showAddMfSheet) {
        AddMutualFundSheet(
            onDismiss = { showAddMfSheet = false },
            onSave = {
                onSaveMutualFund(it)
                showAddMfSheet = false
            }
        )
    }

    activeMfForTx?.let { fund ->
        RecordMfTxSheet(
            fund = fund,
            accounts = state.accounts,
            onDismiss = { activeMfForTx = null },
            onSave = { tx, syncBank ->
                onRecordMfTx(tx, fund.schemeName, syncBank)
                activeMfForTx = null
            }
        )
    }

    if (showAddStockSheet) {
        AddStockSheet(
            onDismiss = { showAddStockSheet = false },
            onSave = {
                onSaveStock(it)
                showAddStockSheet = false
            }
        )
    }

    activeStockForTrade?.let { stock ->
        RecordStockTradeSheet(
            stock = stock,
            accounts = state.accounts,
            onDismiss = { activeStockForTrade = null },
            onSave = { trade, syncBank ->
                onRecordStockTrade(trade, stock.symbol, syncBank)
                activeStockForTrade = null
            }
        )
    }

    if (showAddPpfSheet) {
        AddPpfAccountSheet(
            onDismiss = { showAddPpfSheet = false },
            onSave = {
                onSavePpfAccount(it)
                showAddPpfSheet = false
            }
        )
    }

    activePpfForTx?.let { ppf ->
        PpfTxSheet(
            account = ppf,
            accounts = state.accounts,
            onDismiss = { activePpfForTx = null },
            onSave = { tx, syncBank ->
                onRecordPpfTx(tx, ppf.accountName, syncBank)
                activePpfForTx = null
            }
        )
    }

    if (showPpfRatesSheet) {
        PpfRatesSheet(
            configuredRates = state.ppfRates,
            onSaveRate = onUpdatePpfRate,
            onDismiss = { showPpfRatesSheet = false }
        )
    }

    if (showAddEpfSheet) {
        AddEpfAccountSheet(
            onDismiss = { showAddEpfSheet = false },
            onSave = {
                onSaveEpfAccount(it)
                showAddEpfSheet = false
            }
        )
    }

    activeEpfForEntry?.let { epf ->
        EpfEntrySheet(
            account = epf,
            onDismiss = { activeEpfForEntry = null },
            onSave = { entry ->
                onRecordEpfEntry(entry)
                activeEpfForEntry = null
            }
        )
    }

    if (showAddOtherAssetSheet) {
        AddOtherAssetSheet(
            liabilities = state.netWorthItems.filter { it.category.isLiability },
            onDismiss = { showAddOtherAssetSheet = false },
            onSave = {
                onSaveOtherAsset(it)
                showAddOtherAssetSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📈 Investment Portfolio") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("portfolio_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { showBulkPriceSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("portfolio_bulk_price_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update NAV", style = MaterialTheme.typography.labelMedium)
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
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PortfolioSectionTab.entries) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("portfolio_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (selectedTab) {
                PortfolioSectionTab.OVERVIEW -> PortfolioOverviewContent(
                    summary = state.portfolioSummary,
                    isBalanceHidden = state.isBalanceHidden
                )
                PortfolioSectionTab.MUTUAL_FUNDS -> MutualFundsTabContent(
                    summary = state.portfolioSummary,
                    isBalanceHidden = state.isBalanceHidden,
                    onAddFund = { showAddMfSheet = true },
                    onRecordTx = { activeMfForTx = it },
                    onDeleteFund = onDeleteMutualFund,
                    onOpenBulkNav = { showBulkPriceSheet = true }
                )
                PortfolioSectionTab.STOCKS -> StocksTabContent(
                    summary = state.portfolioSummary,
                    isBalanceHidden = state.isBalanceHidden,
                    onAddStock = { showAddStockSheet = true },
                    onRecordTrade = { activeStockForTrade = it },
                    onDeleteStock = onDeleteStock,
                    onOpenBulkPrices = { showBulkPriceSheet = true }
                )
                PortfolioSectionTab.PPF_EPF -> PpfEpfTabContent(
                    summary = state.portfolioSummary,
                    isBalanceHidden = state.isBalanceHidden,
                    onAddPpf = { showAddPpfSheet = true },
                    onRecordPpfTx = { activePpfForTx = it },
                    onDeletePpf = onDeletePpfAccount,
                    onCreditFyInterest = onCreditPpfFyInterest,
                    onEditPpfRates = { showPpfRatesSheet = true },
                    onAddEpf = { showAddEpfSheet = true },
                    onRecordEpfEntry = { activeEpfForEntry = it },
                    onDeleteEpf = onDeleteEpfAccount
                )
                PortfolioSectionTab.OTHER -> OtherAssetsTabContent(
                    summary = state.portfolioSummary,
                    isBalanceHidden = state.isBalanceHidden,
                    onAddAsset = { showAddOtherAssetSheet = true },
                    onDeleteAsset = onDeleteOtherAsset,
                    onUpdateGoldRates = { showBulkPriceSheet = true }
                )
            }
        }
    }
}

@Composable
private fun PortfolioOverviewContent(
    summary: PortfolioSummary,
    isBalanceHidden: Boolean
) {
    val semanticColors = FinFlowTheme.colors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portfolio_overview_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            text = "Total Portfolio Value",
                            style = MaterialTheme.typography.labelLarge,
                            color = semanticColors.textSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = semanticColors.surfaceElevated,
                            border = BorderStroke(0.75.dp, semanticColors.hairlineBorder)
                        ) {
                            Text(
                                text = "Offline Tracking",
                                style = MaterialTheme.typography.labelSmall,
                                color = semanticColors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatCurrency(summary.totalCurrentValue, isBalanceHidden),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("portfolio_total_value")
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Invested", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            Text(
                                text = formatCurrency(summary.totalInvested, isBalanceHidden),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Unrealized P&L", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            val sign = if (summary.unrealizedGain >= 0) "+" else ""
                            val pnlColor = if (summary.unrealizedGain >= 0) semanticColors.income else semanticColors.expense
                            Text(
                                text = "$sign${formatCurrency(summary.unrealizedGain, isBalanceHidden)} (${"%.1f".format(summary.unrealizedGainPercent)}%)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = pnlColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Realized", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            val rSign = if (summary.realizedGain >= 0) "+" else ""
                            val rColor = if (summary.realizedGain >= 0) semanticColors.income else semanticColors.expense
                            Text(
                                text = "$rSign${formatCurrency(summary.realizedGain, isBalanceHidden)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = rColor
                            )
                        }
                    }
                }
            }
        }

        if (summary.monthlySipCommitment > 0.0) {
            item {
                FinSurfaceCard(shape = RoundedCornerShape(16.dp), elevation = 0.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(emoji = "🗓️", tintColor = MaterialTheme.colorScheme.primary, size = 36.dp, emojiSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Active Monthly SIPs", style = MaterialTheme.typography.titleSmall)
                                Text("Scheduled mutual fund commitments", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            }
                        }
                        Text(
                            text = "${formatCurrency(summary.monthlySipCommitment, isBalanceHidden)}/mo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            FinSurfaceCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Asset Allocation", style = MaterialTheme.typography.titleMedium)
                    if (summary.assetAllocation.isEmpty()) {
                        EmptyStateCard(
                            title = "No holdings yet",
                            subtitle = "Switch to Mutual Funds, Stocks, PPF & EPF, or Other Assets tabs to add holdings.",
                            emoji = "📈"
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            summary.assetAllocation.forEach { slice ->
                                val w = slice.percentage.coerceAtLeast(0.03f)
                                Box(
                                    modifier = Modifier
                                        .weight(w)
                                        .fillMaxHeight()
                                        .background(Color(slice.colorHex))
                                )
                            }
                        }
                        summary.assetAllocation.forEachIndexed { idx, slice ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EmojiBadge(
                                    emoji = slice.emoji,
                                    tintColor = Color(slice.colorHex),
                                    size = 34.dp,
                                    emojiSize = 16.sp,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(slice.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatCurrency(slice.value, isBalanceHidden), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Text("${(slice.percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                                }
                            }
                            if (idx < summary.assetAllocation.lastIndex) {
                                HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                            }
                        }
                    }
                }
            }
        }

        if (summary.historyPoints.size >= 2 && summary.totalCurrentValue > 0.0) {
            item {
                FinSurfaceCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("6-Month Portfolio Trajectory", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Based on units held & latest offline valuations",
                            style = MaterialTheme.typography.labelSmall,
                            color = semanticColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val points = summary.historyPoints.map { it.value }
                        val lineColor = MaterialTheme.colorScheme.primary
                        val dotSurfaceColor = MaterialTheme.colorScheme.surface
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val minVal = points.minOrNull() ?: 0.0
                            val maxVal = points.maxOrNull() ?: 1.0
                            val span = (maxVal - minVal).coerceAtLeast(1.0)
                            val strokePath = Path()
                            val fillPath = Path()
                            val coords = points.mapIndexed { idx, v ->
                                val x = size.width * (idx.toFloat() / (points.size - 1))
                                val y = size.height - (((v - minVal) / span).toFloat() * (size.height - 24f)) - 12f
                                Offset(x, y)
                            }
                            coords.forEachIndexed { idx, pt ->
                                if (idx == 0) {
                                    strokePath.moveTo(pt.x, pt.y)
                                    fillPath.moveTo(pt.x, size.height)
                                    fillPath.lineTo(pt.x, pt.y)
                                } else {
                                    strokePath.lineTo(pt.x, pt.y)
                                    fillPath.lineTo(pt.x, pt.y)
                                }
                            }
                            fillPath.lineTo(coords.last().x, size.height)
                            fillPath.close()
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), Color.Transparent))
                            )
                            drawPath(path = strokePath, color = lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                            coords.forEach { pt ->
                                drawCircle(color = dotSurfaceColor, radius = 7.5f, center = pt)
                                drawCircle(color = lineColor, radius = 5f, center = pt)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            summary.historyPoints.forEach { pt ->
                                Text(pt.label, style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
