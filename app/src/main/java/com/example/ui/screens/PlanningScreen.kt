package com.example.ui.screens

import com.example.util.MonthYear
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringBillEntity
import com.example.data.model.SavingsGoalEntity
import com.example.ui.BudgetProgress
import com.example.ui.FinanceUiState
import com.example.ui.components.AnimatedFinProgressBar
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.resolveCategoryEmoji
import com.example.ui.theme.FinFlowTheme
import com.example.util.formatCurrency

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.heightIn

@Composable
fun PlanningScreen(
    state: FinanceUiState,
    onMonthSelected: (MonthYear) -> Unit,
    onToggleVisibility: () -> Unit,
    onSetBudget: (Long, Double, Long) -> Unit,
    onInspectBudgetCategory: (Long) -> Unit,
    onSaveRecurringBill: (RecurringBillEntity) -> Unit,
    onDeleteRecurringBill: (RecurringBillEntity) -> Unit,
    onSaveGoal: (SavingsGoalEntity) -> Unit,
    onContributeGoal: (SavingsGoalEntity, Double) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val semanticColors = FinFlowTheme.colors
    var selectedSection by remember { mutableIntStateOf(0) }
    val sections = listOf("Budgets", "Bills & EMIs", "Savings Goals")

    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("planning_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("🎯 Budgets, Bills & Goals", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "Planning",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Budgets, EMI & bill calendar, subscriptions & goals",
                    style = MaterialTheme.typography.bodySmall,
                    color = semanticColors.textSecondary
                )
            }
        }
        if (selectedSection != 2) {
            MonthSelectorBar(
                selectedMonth = state.selectedMonth,
                isBalanceHidden = state.isBalanceHidden,
                onMonthSelected = onMonthSelected,
                onToggleVisibility = onToggleVisibility
            )
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            sections.forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = selectedSection == idx,
                    onClick = { selectedSection = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = sections.size),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("planning_tab_$idx")
                ) {
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedSection) {
            0 -> BudgetsTabContent(
                state = state,
                onSetBudget = onSetBudget,
                onInspectBudgetCategory = onInspectBudgetCategory
            )
            1 -> RecurringManagerContent(
                state = state,
                onSaveBill = onSaveRecurringBill,
                onDeleteBill = onDeleteRecurringBill
            )
            2 -> CashFlowAndGoalsContent(
                state = state,
                onSaveGoal = onSaveGoal,
                onContributeGoal = onContributeGoal,
                onDeleteGoal = onDeleteGoal
            )
        }
    }
}

@Composable
private fun BudgetsTabContent(
    state: FinanceUiState,
    onSetBudget: (Long, Double, Long) -> Unit,
    onInspectBudgetCategory: (Long) -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    var editingItem by remember { mutableStateOf<BudgetProgress?>(null) }
    var showOnlyBudgeted by remember { mutableStateOf(false) }

    val displayedList = remember(state.budgetProgressList, showOnlyBudgeted) {
        if (showOnlyBudgeted) {
            state.budgetProgressList.filter { (it.budget?.limitAmount ?: 0.0) > 0.0 }
        } else {
            state.budgetProgressList
        }
    }

    editingItem?.let { item ->
        var limitInput by remember { mutableStateOf(item.budget?.limitAmount?.toString() ?: "") }
        val catEmoji = resolveCategoryEmoji(item.category.iconName, item.category.name)
        AlertDialog(
            onDismissRequest = { editingItem = null },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(
                        emoji = catEmoji,
                        tintColor = Color(item.category.colorHex),
                        size = 36.dp,
                        emojiSize = 18.sp,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Budget for ${item.category.name}")
                }
            },
            text = {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Monthly Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("budget_limit_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        limitInput.toDoubleOrNull()?.let { limit ->
                            if (limit >= 0.0) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSetBudget(item.category.id, limit, item.budget?.id ?: 0L)
                            }
                        }
                        editingItem = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("budget_save_button")
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingItem = null },
                    modifier = Modifier.testTag("budget_cancel_button")
                ) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("planning_list"),
        contentPadding = PaddingValues(bottom = 104.dp)
    ) {
        item {
            val totalRatio = if (state.totalBudgetPlanned > 0) {
                (state.totalBudgetSpent / state.totalBudgetPlanned).toFloat()
            } else 0f
            val overallColor = when {
                totalRatio > 1f -> semanticColors.expense
                totalRatio >= 0.8f -> semanticColors.warning
                else -> semanticColors.income
            }
            val remaining = (state.totalBudgetPlanned - state.totalBudgetSpent).coerceAtLeast(0.0)

            FinSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("planning_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            EmojiBadge(
                                emoji = "🎯",
                                tintColor = overallColor,
                                size = 40.dp,
                                emojiSize = 19.sp,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Total Monthly Plan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap a category to inspect transactions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semanticColors.textSecondary
                                )
                            }
                        }
                        Text(
                            text = "${(totalRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = overallColor
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedFinProgressBar(
                        progress = totalRatio,
                        color = overallColor,
                        height = 10.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spent: ${formatCurrency(state.totalBudgetSpent, state.isBalanceHidden)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = semanticColors.textSecondary
                        )
                        Text(
                            text = "Remaining: ${formatCurrency(remaining, state.isBalanceHidden)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Limits (${displayedList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilterChip(
                    selected = showOnlyBudgeted,
                    onClick = { showOnlyBudgeted = !showOnlyBudgeted },
                    label = { Text(if (showOnlyBudgeted) "Budgeted Only" else "All Categories") },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        items(displayedList, key = { it.category.id }) { item ->
            val limit = item.budget?.limitAmount ?: 0.0
            val ratio = if (limit > 0) (item.spent / limit).toFloat() else 0f
            val barColor = when {
                ratio > 1f -> semanticColors.expense
                ratio >= 0.8f -> semanticColors.warning
                else -> semanticColors.income
            }
            val catColor = Color(item.category.colorHex)

            FinSurfaceCard(
                shape = RoundedCornerShape(18.dp),
                elevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onInspectBudgetCategory(item.category.id)
                    }
                    .testTag("budget_card_${item.category.name.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            EmojiBadge(
                                emoji = resolveCategoryEmoji(item.category.iconName, item.category.name),
                                tintColor = catColor,
                                size = 40.dp,
                                emojiSize = 19.sp,
                                shape = RoundedCornerShape(13.dp),
                                contentDescription = item.category.name
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.category.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (limit > 0) "${(ratio * 100).toInt()}%" else "No cap",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (limit > 0) barColor else semanticColors.textSecondary
                        )
                        IconButton(
                            onClick = { editingItem = item },
                            modifier = Modifier.testTag("edit_budget_${item.category.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit ${item.category.name} limit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedFinProgressBar(
                        progress = ratio,
                        color = barColor,
                        height = 8.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${formatCurrency(item.spent, state.isBalanceHidden)} / ${formatCurrency(limit, state.isBalanceHidden)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = semanticColors.textSecondary
                    )
                }
            }
        }
    }
}
