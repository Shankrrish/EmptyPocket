package com.example.ui.screens

import com.example.util.MonthYear
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.HomeCardConfig
import com.example.data.model.HomeCardId
import com.example.ui.AccountSummary
import com.example.ui.CardSummary
import com.example.ui.FinanceUiState
import com.example.ui.InsightSeverity
import com.example.ui.components.AnimatedFinProgressBar
import com.example.ui.components.EmojiBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinSurfaceCard
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.SculptedActionIcon
import com.example.ui.components.accountEmoji
import com.example.ui.theme.FinFlowTheme
import com.example.util.formatCurrency

@Composable
fun DashboardScreen(
    state: FinanceUiState,
    onMonthSelected: (MonthYear) -> Unit,
    onToggleVisibility: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onInspectAccount: (Long) -> Unit,
    onQuickAddForAccount: (Long) -> Unit,
    onOpenCardDetails: (CardSummary) -> Unit,
    onPayCreditCard: (Long) -> Unit,
    onUpdateSavingsTarget: (Double) -> Unit,
    onOpenHomeCustomization: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenAccounts: () -> Unit = {}
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    var showTargetDialog by remember { mutableStateOf(false) }
    var showProgressiveDetails by remember { mutableStateOf(false) }

    if (showTargetDialog) {
        var targetInput by remember { mutableStateOf(state.settings.monthlySavingsTarget.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Monthly Savings Target") },
            text = {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Target Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("savings_target_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        targetInput.toDoubleOrNull()?.let(onUpdateSavingsTarget)
                        showTargetDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("savings_target_save_button")
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) { Text("Cancel") }
            }
        )
    }

    val visibleCards = remember(state.homeCards) { state.homeCards.filter { it.visible } }
    val summaryFirst = visibleCards.firstOrNull()?.id == HomeCardId.SUMMARY
    val hiddenProgressiveCards = remember(state.homeCards) {
        state.homeCards.filter {
            !it.visible && (
                it.id == HomeCardId.SAFE_TO_SPEND ||
                    it.id == HomeCardId.INSIGHTS ||
                    it.id == HomeCardId.CATEGORY_CHART
                )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(bottom = 104.dp)
    ) {
        if (!summaryFirst) {
            item(key = "fallback_month_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MonthSelectorBar(
                            selectedMonth = state.selectedMonth,
                            isBalanceHidden = state.isBalanceHidden,
                            onMonthSelected = onMonthSelected,
                            onToggleVisibility = onToggleVisibility
                        )
                    }
                    IconButton(
                        onClick = onOpenHomeCustomization,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("customize_home_button")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Customize homepage cards",
                            tint = semanticColors.textSecondary
                        )
                    }
                }
            }
        }

        visibleCards.forEach { cardConfig ->
            when (cardConfig.id) {
                HomeCardId.SUMMARY -> {
                    item(key = "home_summary") {
                        CurvedHeroSummaryHeader(
                            state = state,
                            includeMonthControls = summaryFirst,
                            onMonthSelected = onMonthSelected,
                            onToggleVisibility = onToggleVisibility,
                            onNavigateToTransactions = onNavigateToTransactions,
                            onOpenHomeCustomization = onOpenHomeCustomization,
                            onOpenPrivacySettings = onOpenPrivacySettings
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
                HomeCardId.ACCOUNTS -> {
                    item(key = "home_accounts") {
                        VerticalAccountsCardSection(
                            state = state,
                            onInspectAccount = { accId ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onInspectAccount(accId)
                            },
                            onQuickAddForAccount = { accId ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onQuickAddForAccount(accId)
                            },
                            onOpenHomeCustomization = onOpenHomeCustomization,
                            onOpenAccounts = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenAccounts()
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                HomeCardId.CREDIT_CARDS -> {
                    item(key = "home_credit_cards") {
                        CreditCardsSection(
                            state = state,
                            onOpenCardDetails = { cardSummary ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenCardDetails(cardSummary)
                            },
                            onPayCreditCard = { cardId ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPayCreditCard(cardId)
                            }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
                HomeCardId.BUDGETS -> {
                    item(key = "home_budgets") {
                        MonthlyBudgetOverviewCard(
                            state = state,
                            onNavigateToPlanning = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToPlanning()
                            }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
                HomeCardId.SAFE_TO_SPEND -> {
                    item(key = "home_safe_to_spend") {
                        SafeToSpendCard(
                            state = state,
                            onEditSavingsTarget = { showTargetDialog = true }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
                HomeCardId.INSIGHTS -> {
                    if (state.insights.isNotEmpty()) {
                        item(key = "home_insights") {
                            MonthlyInsightsCard(state = state)
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
                HomeCardId.CATEGORY_CHART -> {
                    item(key = "home_category_chart") {
                        CategoryBreakdownCard(state = state)
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }
        }

        // Progressive disclosure for advanced analytics when hidden from the minimal homepage
        if (hiddenProgressiveCards.isNotEmpty()) {
            item(key = "progressive_disclosure_section") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FinSurfaceCard(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showProgressiveDetails = !showProgressiveDetails
                            }
                            .testTag("progressive_analytics_toggle")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                EmojiBadge(
                                    emoji = "✨",
                                    tintColor = MaterialTheme.colorScheme.primary,
                                    size = 36.dp,
                                    emojiSize = 17.sp,
                                    shape = RoundedCornerShape(11.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "More Insights & Breakdown",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Safe-to-Spend • ${state.insights.size} insights • Category chart",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = semanticColors.textSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showProgressiveDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showProgressiveDetails) "Collapse details" else "Expand details",
                                tint = semanticColors.textSecondary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showProgressiveDetails,
                        enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut() + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(top = 14.dp)
                        ) {
                            hiddenProgressiveCards.forEach { hiddenCard ->
                                when (hiddenCard.id) {
                                    HomeCardId.SAFE_TO_SPEND -> SafeToSpendCard(
                                        state = state,
                                        horizontalPadding = 0,
                                        onEditSavingsTarget = { showTargetDialog = true }
                                    )
                                    HomeCardId.INSIGHTS -> if (state.insights.isNotEmpty()) {
                                        MonthlyInsightsCard(
                                            state = state,
                                            horizontalPadding = 0
                                        )
                                    }
                                    HomeCardId.CATEGORY_CHART -> CategoryBreakdownCard(
                                        state = state,
                                        horizontalPadding = 0
                                    )
                                    else -> Unit
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
private fun CurvedHeroSummaryHeader(
    state: FinanceUiState,
    includeMonthControls: Boolean,
    onMonthSelected: (MonthYear) -> Unit,
    onToggleVisibility: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onOpenHomeCustomization: () -> Unit,
    onOpenPrivacySettings: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    val totalCcDebt = state.cardSummaries.sumOf { it.openInvoice }
    val safePerDay = state.safeToSpend.dailyAllowance

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (includeMonthControls) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenPrivacySettings()
                    },
                    modifier = Modifier.testTag("header_privacy_button")
                ) {
                    Icon(
                        imageVector = if (state.settings.appLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Privacy & Security",
                        tint = semanticColors.textSecondary
                    )
                }

                com.example.ui.components.MonthNavigator(
                    selectedMonth = state.selectedMonth,
                    onMonthSelected = onMonthSelected
                )

                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenHomeCustomization()
                    },
                    modifier = Modifier.testTag("customize_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Customize Homepage Cards",
                        tint = semanticColors.textSecondary
                    )
                }
            }
        }

        FinSurfaceCard(
            shape = RoundedCornerShape(22.dp),
            backgroundBrush = Brush.linearGradient(
                listOf(semanticColors.heroGradientStart, semanticColors.heroGradientEnd)
            ),
            borderColor = semanticColors.heroBorder,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("balance_hero_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(
                            emoji = "🌿",
                            tintColor = MaterialTheme.colorScheme.primary,
                            size = 28.dp,
                            emojiSize = 14.sp,
                            shape = CircleShape
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accounts balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = semanticColors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleVisibility()
                        },
                        modifier = Modifier.testTag("toggle_visibility_button")
                    ) {
                        Icon(
                            imageVector = if (state.isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.cd_toggle_visibility),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(state.totalBalance, state.isBalanceHidden),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("total_balance_text")
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // 2x2 Charcoal Breakdown Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroBreakdownCell(
                        label = stringResource(R.string.income),
                        value = formatCurrency(state.monthlyIncome, state.isBalanceHidden),
                        valueColor = semanticColors.income,
                        indicatorIcon = Icons.Default.ArrowUpward,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onNavigateToTransactions)
                            .padding(4.dp)
                            .testTag("dashboard_income_pill")
                    )
                    HeroBreakdownCell(
                        label = stringResource(R.string.expenses),
                        value = formatCurrency(state.monthlyExpense, state.isBalanceHidden),
                        valueColor = semanticColors.expense,
                        indicatorIcon = Icons.Default.ArrowDownward,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onNavigateToTransactions)
                            .padding(4.dp)
                            .testTag("dashboard_expense_pill")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroBreakdownCell(
                        label = "Safe Cash / Day",
                        value = formatCurrency(safePerDay, state.isBalanceHidden),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        indicatorIcon = null,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    )
                    HeroBreakdownCell(
                        label = "Total CC Balance",
                        value = formatCurrency(totalCcDebt, state.isBalanceHidden),
                        valueColor = if (totalCcDebt > 0.0) semanticColors.warning else MaterialTheme.colorScheme.onSurface,
                        indicatorIcon = null,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBreakdownCell(
    label: String,
    value: String,
    valueColor: Color,
    indicatorIcon: ImageVector?,
    modifier: Modifier = Modifier
) {
    val semanticColors = FinFlowTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (indicatorIcon != null) {
                Icon(
                    imageVector = indicatorIcon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = semanticColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VerticalAccountsCardSection(
    state: FinanceUiState,
    onInspectAccount: (Long) -> Unit,
    onQuickAddForAccount: (Long) -> Unit,
    onOpenHomeCustomization: () -> Unit,
    onOpenAccounts: () -> Unit = {}
) {
    val semanticColors = FinFlowTheme.colors
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .testTag("dashboard_accounts_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onOpenAccounts,
                    modifier = Modifier.testTag("dashboard_manage_accounts_button")
                ) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onOpenHomeCustomization,
                    modifier = Modifier.testTag("accounts_section_customize_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Reorder homepage cards",
                        tint = semanticColors.textSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        FinSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            if (state.accountSummaries.isEmpty()) {
                Text(
                    text = "No accounts added yet. Tap Manage to add bank, cash, or prepaid wallets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = semanticColors.textSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    state.accountSummaries.forEachIndexed { idx, summary ->
                        val acc = summary.account
                        val accColor = Color(acc.colorHex)
                        val accountSubtitle = buildList {
                            if (acc.bankName.isNotBlank() && !acc.name.contains(acc.bankName, ignoreCase = true)) {
                                add(acc.bankName)
                            }
                            if (acc.maskedNumber.isNotBlank()) add(acc.maskedNumber)
                        }.joinToString(" • ")

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInspectAccount(acc.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("dashboard_account_card_${acc.id}")
                        ) {
                            EmojiBadge(
                                emoji = accountEmoji(acc.type, acc.emoji),
                                tintColor = accColor,
                                size = 44.dp,
                                emojiSize = 21.sp,
                                shape = RoundedCornerShape(14.dp),
                                contentDescription = acc.name
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accColor.copy(alpha = if (semanticColors.isAmoledDark) 0.16f else 0.10f)
                                    ) {
                                        Text(
                                            text = acc.type.shortBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (semanticColors.isAmoledDark) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                accColor
                                            },
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = formatCurrency(summary.currentBalance, state.isBalanceHidden),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (summary.currentBalance >= 0) semanticColors.income else semanticColors.expense
                                    )
                                    if (accountSubtitle.isNotBlank()) {
                                        Text(
                                            text = "• $accountSubtitle",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = semanticColors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            SculptedActionIcon(
                                icon = Icons.Default.Add,
                                contentDescription = "Add transaction for ${acc.name}",
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { onQuickAddForAccount(acc.id) },
                                testTag = "quick_add_account_${acc.id}"
                            )
                        }
                        if (idx < state.accountSummaries.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.75.dp,
                                color = semanticColors.hairlineBorder,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditCardsSection(
    state: FinanceUiState,
    onOpenCardDetails: (CardSummary) -> Unit,
    onPayCreditCard: (Long) -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    Column(modifier = Modifier.testTag("dashboard_credit_cards_section")) {
        Text(
            text = stringResource(R.string.credit_cards),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (state.cardSummaries.isEmpty()) {
            FinSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "No credit cards added yet. Manage cards in the Accounts tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = semanticColors.textSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.cardSummaries, key = { it.card.id }) { summary ->
                    val usedRatio = if (summary.card.limitAmount > 0) {
                        (summary.openInvoice / summary.card.limitAmount).toFloat().coerceIn(0f, 1f)
                    } else 0f
                    val cardAccent = Color(summary.card.colorHex)
                    FinSurfaceCard(
                        shape = RoundedCornerShape(18.dp),
                        backgroundBrush = Brush.linearGradient(
                            listOf(
                                cardAccent.copy(alpha = if (semanticColors.isAmoledDark) 0.22f else 0.12f),
                                semanticColors.heroGradientEnd
                            )
                        ),
                        borderColor = cardAccent.copy(alpha = if (semanticColors.isAmoledDark) 0.42f else 0.28f),
                        modifier = Modifier
                            .width(276.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenCardDetails(summary) }
                            .testTag("credit_card_${summary.card.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    EmojiBadge(
                                        emoji = "💳",
                                        tintColor = Color(summary.card.colorHex),
                                        size = 38.dp,
                                        emojiSize = 18.sp,
                                        shape = RoundedCornerShape(12.dp),
                                        contentDescription = summary.card.name
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = summary.card.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${summary.card.brand} Credit Card",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = semanticColors.textSecondary
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = semanticColors.warningContainer
                                ) {
                                    Text(
                                        text = "Due ${summary.card.dueDay}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semanticColors.warning,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "Outstanding",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semanticColors.textSecondary
                                    )
                                    Text(
                                        text = formatCurrency(summary.openInvoice, state.isBalanceHidden),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (summary.openInvoice > 0.0) semanticColors.expense else semanticColors.income
                                    )
                                }
                                if (summary.openInvoice > 0.0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onPayCreditCard(summary.card.id) }
                                            .testTag("pay_card_bill_${summary.card.id}")
                                    ) {
                                        Text(
                                            text = "Pay Bill",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            AnimatedFinProgressBar(
                                progress = usedRatio,
                                color = if (usedRatio > 0.8f) semanticColors.warning else MaterialTheme.colorScheme.primary,
                                height = 7.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Available: ${formatCurrency(summary.availableLimit, state.isBalanceHidden)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = semanticColors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBudgetOverviewCard(
    state: FinanceUiState,
    onNavigateToPlanning: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val targetPct = if (state.totalBudgetPlanned > 0) {
        (state.totalBudgetSpent / state.totalBudgetPlanned).toFloat()
    } else 0f
    val budgetColor = when {
        targetPct > 1f -> semanticColors.expense
        targetPct >= 0.8f -> semanticColors.warning
        else -> semanticColors.income
    }
    FinSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onNavigateToPlanning)
            .testTag("dashboard_budget_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(
                        emoji = "🎯",
                        tintColor = budgetColor,
                        size = 34.dp,
                        emojiSize = 17.sp,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.monthly_budget),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (state.totalBudgetPlanned > 0) "${(targetPct * 100).toInt()}%" else "Set limits",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.totalBudgetPlanned > 0) budgetColor else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedFinProgressBar(
                progress = targetPct,
                color = budgetColor,
                height = 10.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatCurrency(state.totalBudgetSpent, state.isBalanceHidden)} spent of ${formatCurrency(state.totalBudgetPlanned, state.isBalanceHidden)}",
                style = MaterialTheme.typography.bodyMedium,
                color = semanticColors.textSecondary
            )
        }
    }
}

@Composable
private fun SafeToSpendCard(
    state: FinanceUiState,
    horizontalPadding: Int = 16,
    onEditSavingsTarget: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val safe = state.safeToSpend
    val isHealthy = safe.safeToSpend >= 0.0
    val ratio = if (safe.monthlyIncome > 0) {
        ((safe.upcomingBills + safe.savingsTarget + safe.currentSpending) / safe.monthlyIncome)
            .toFloat().coerceIn(0f, 1f)
    } else 0f

    FinSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding.dp)
            .testTag("safe_to_spend_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(
                        emoji = "🛡️",
                        tintColor = MaterialTheme.colorScheme.primary,
                        size = 38.dp,
                        emojiSize = 18.sp,
                        shape = RoundedCornerShape(12.dp),
                        contentDescription = "Safe to Spend"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Safe to Spend",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${formatCurrency(safe.dailyAllowance, state.isBalanceHidden)} / day • ${safe.remainingDays}d left",
                            style = MaterialTheme.typography.bodySmall,
                            color = semanticColors.textSecondary
                        )
                    }
                }
                Text(
                    text = formatCurrency(safe.safeToSpend, state.isBalanceHidden),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isHealthy) semanticColors.income else semanticColors.expense,
                    modifier = Modifier.testTag("safe_to_spend_amount")
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedFinProgressBar(
                progress = ratio,
                color = if (isHealthy) MaterialTheme.colorScheme.primary else semanticColors.expense,
                height = 8.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SafeStatColumn("Income", formatCurrency(safe.monthlyIncome, state.isBalanceHidden))
                SafeStatColumn("Bills Due", formatCurrency(safe.upcomingBills, state.isBalanceHidden))
                SafeStatColumn(
                    label = "Savings Goal",
                    value = formatCurrency(safe.savingsTarget, state.isBalanceHidden),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEditSavingsTarget)
                        .testTag("edit_savings_target_button")
                )
                SafeStatColumn("Spent", formatCurrency(safe.currentSpending, state.isBalanceHidden))
            }
        }
    }
}

@Composable
private fun MonthlyInsightsCard(
    state: FinanceUiState,
    horizontalPadding: Int = 16
) {
    val semanticColors = FinFlowTheme.colors
    FinSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding.dp)
            .testTag("financial_insights_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(
                    emoji = "💡",
                    tintColor = MaterialTheme.colorScheme.primary,
                    size = 34.dp,
                    emojiSize = 16.sp,
                    shape = RoundedCornerShape(10.dp),
                    contentDescription = "Insights"
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Monthly Review & Insights",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            state.insights.take(4).forEachIndexed { idx, insight ->
                val (tint, emoji) = when (insight.severity) {
                    InsightSeverity.POSITIVE -> semanticColors.income to "🎉"
                    InsightSeverity.WARNING -> semanticColors.warning to "⚠️"
                    InsightSeverity.ALERT -> semanticColors.expense to "🚨"
                    InsightSeverity.INFO -> MaterialTheme.colorScheme.primary to "📊"
                }
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp)
                ) {
                    EmojiBadge(
                        emoji = emoji,
                        tintColor = tint,
                        size = 32.dp,
                        emojiSize = 15.sp,
                        shape = RoundedCornerShape(10.dp),
                        contentDescription = insight.title
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = insight.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = semanticColors.textSecondary
                        )
                    }
                }
                if (idx < state.insights.take(4).lastIndex) {
                    HorizontalDivider(color = semanticColors.divider.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    state: FinanceUiState,
    horizontalPadding: Int = 16
) {
    FinSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding.dp)
            .testTag("dashboard_category_chart_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(
                    emoji = "🍩",
                    tintColor = MaterialTheme.colorScheme.primary,
                    size = 32.dp,
                    emojiSize = 16.sp,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.expenses_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            if (state.categorySlices.isEmpty()) {
                EmptyStateCard(
                    title = stringResource(R.string.no_expenses_month),
                    subtitle = "Tap the + button to log an expense or navigate to another month.",
                    emoji = "📊"
                )
            } else {
                InteractiveDonutChart(
                    slices = state.categorySlices,
                    isBalanceHidden = state.isBalanceHidden
                )
            }
        }
    }
}

@Composable
private fun SafeStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = FinFlowTheme.colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CashflowMetricPill(
    label: String,
    amount: String,
    icon: ImageVector,
    circleBg: Color,
    amountTint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(circleBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = amountTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 820)
@Composable
private fun DashboardScreenPreview() {
    val previewAccounts = listOf(
        AccountEntity(id = 1L, name = "HDFC Salary", type = AccountType.CHECKING, initialBalance = 62000.0, colorHex = 0xFF556B2F),
        AccountEntity(id = 2L, name = "Wallet Cash", type = AccountType.CASH, initialBalance = 6500.0, colorHex = 0xFF3B6E22),
        AccountEntity(id = 3L, name = "Emergency Savings", type = AccountType.SAVINGS, initialBalance = 185000.0, colorHex = 0xFF2F6F6D)
    )
    FinFlowTheme {
        DashboardScreen(
            state = FinanceUiState(
                accounts = previewAccounts,
                accountSummaries = previewAccounts.map { AccountSummary(it, it.initialBalance) },
                totalBalance = 253500.0,
                monthlyIncome = 100000.0,
                monthlyExpense = 42350.0,
                homeCards = HomeCardConfig.defaultList()
            ),
            onMonthSelected = {},
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
    }
}
