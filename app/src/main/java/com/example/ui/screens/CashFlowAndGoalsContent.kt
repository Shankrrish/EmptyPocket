package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavingsGoalEntity
import com.example.ui.FinanceUiState
import com.example.ui.components.AnimatedFinProgressBar
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.components.resolveCategoryEmoji
import com.example.ui.theme.FinFlowTheme
import com.example.util.MonthYear
import com.example.util.formatCurrency
import kotlin.math.ceil

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashFlowAndGoalsContent(
    state: FinanceUiState,
    onSaveGoal: (SavingsGoalEntity) -> Unit,
    onContributeGoal: (SavingsGoalEntity, Double) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    var showAddGoal by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    if (showAddGoal) {
        var name by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        var saved by remember { mutableStateOf("0") }
        var monthly by remember { mutableStateOf("5000") }
        var recurring by remember { mutableStateOf(true) }
        var goalEmoji by remember { mutableStateOf("🐷") }
        val goalEmojiChoices = listOf("🐷", "🎯", "🏖️", "🚗", "🎓", "🏡", "💎", "🏥")

        AlertDialog(
            onDismissRequest = { showAddGoal = false },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("New Savings Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose Emoji", style = MaterialTheme.typography.labelMedium, color = semanticColors.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        goalEmojiChoices.forEach { em ->
                            val picked = goalEmoji == em
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (picked) 1.5.dp else 0.dp,
                                        color = if (picked) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { goalEmoji = em }
                            ) {
                                Text(em, fontSize = 17.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal Name (e.g. Emergency Fund)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("goal_name_input")
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("goal_target_input")
                    )
                    OutlinedTextField(
                        value = saved,
                        onValueChange = { saved = it },
                        label = { Text("Already Saved (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = monthly,
                        onValueChange = { monthly = it },
                        label = { Text("Monthly Contribution (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recurring contribution", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = recurring, onCheckedChange = { recurring = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tgt = target.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && tgt > 0.0) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSaveGoal(
                                SavingsGoalEntity(
                                    name = name.trim(),
                                    targetAmount = tgt,
                                    savedAmount = saved.toDoubleOrNull() ?: 0.0,
                                    monthlyContribution = monthly.toDoubleOrNull() ?: 0.0,
                                    isRecurringContribution = recurring,
                                    iconName = goalEmoji
                                )
                            )
                            showAddGoal = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("goal_save_button")
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoal = false }) { Text("Cancel") }
            }
        )
    }

    contributingGoal?.let { goal ->
        var amountInput by remember { mutableStateOf(goal.monthlyContribution.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { contributingGoal = null },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Contribute to ${goal.name}") },
            text = {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Contribution (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("goal_contribute_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        amountInput.toDoubleOrNull()?.let { onContributeGoal(goal, it) }
                        contributingGoal = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("goal_contribute_confirm")
                ) { Text("Add Funds") }
            },
            dismissButton = {
                TextButton(onClick = { contributingGoal = null }) { Text("Cancel") }
            }
        )
    }

    val forecast = state.cashFlowForecast

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("forecast_goals_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Feature 3: Cash Flow Forecasting Card
        item {
            FinSurfaceCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cash_flow_forecast_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(
                                emoji = "📈",
                                tintColor = MaterialTheme.colorScheme.primary,
                                size = 36.dp,
                                emojiSize = 18.sp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Cash Flow Forecast", style = MaterialTheme.typography.titleMedium)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = semanticColors.surfaceElevated,
                            border = BorderStroke(0.75.dp, semanticColors.hairlineBorder)
                        ) {
                            Text(
                                text = "${forecast.salaryCycleDaysLeft}d to next salary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Based on your planned transactions, your estimated balance on ${forecast.endOfMonthLabel} is ${formatCurrency(forecast.estimatedEndOfMonthBalance, state.isBalanceHidden)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Forecast Canvas Line Chart with soft gradient fill
                    val lineColor = MaterialTheme.colorScheme.primary
                    val dotSurfaceColor = MaterialTheme.colorScheme.surface
                    if (forecast.points.size >= 2) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(108.dp)
                        ) {
                            val minVal = forecast.points.minOf { it.projectedBalance }
                            val maxVal = forecast.points.maxOf { it.projectedBalance }
                            val span = (maxVal - minVal).coerceAtLeast(1.0)
                            val strokePath = Path()
                            val fillPath = Path()
                            val coords = forecast.points.mapIndexed { idx, pt ->
                                val x = size.width * (idx.toFloat() / (forecast.points.size - 1))
                                val normY = ((pt.projectedBalance - minVal) / span).toFloat()
                                val y = size.height - (normY * (size.height - 24f)) - 12f
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
                                brush = Brush.verticalGradient(
                                    colors = listOf(lineColor.copy(alpha = 0.24f), Color.Transparent)
                                )
                            )
                            drawPath(
                                path = strokePath,
                                color = lineColor,
                                style = Stroke(width = 5f, cap = StrokeCap.Round)
                            )
                            coords.forEach { pt ->
                                drawCircle(color = dotSurfaceColor, radius = 8f, center = pt)
                                drawCircle(color = lineColor, radius = 5.5f, center = pt)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ForecastMetric("Current", formatCurrency(forecast.currentLiquidBalance, state.isBalanceHidden))
                        ForecastMetric("+ Expected", formatCurrency(forecast.expectedRemainingIncome, state.isBalanceHidden))
                        ForecastMetric("- Bills/EMIs", formatCurrency(forecast.upcomingBillsAndEmis, state.isBalanceHidden))
                        ForecastMetric("- Planned", formatCurrency(forecast.plannedBudgetReserve, state.isBalanceHidden))
                    }
                }
            }
        }

        // Feature 4: Savings Goals & Sinking Funds Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Savings Goals & Sinking Funds", style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = { showAddGoal = true },
                    modifier = Modifier.testTag("add_goal_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Savings Goal")
                }
            }
        }

        items(state.savingsGoals, key = { "goal_${it.id}" }) { goal ->
            val rawRatio = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
            val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
            val etaText = when {
                remaining <= 0.0 -> "🎉 Goal reached!"
                goal.monthlyContribution > 0.0 -> {
                    val monthsNeeded = ceil(remaining / goal.monthlyContribution).toInt()
                    val etaMonth = MonthYear.current().plusMonths(monthsNeeded)
                    "Est. ${etaMonth.displayLabel()} (${monthsNeeded}m)"
                }
                else -> "Set monthly contribution"
            }
            val goalColor = Color(goal.colorHex)

            FinSurfaceCard(
                shape = RoundedCornerShape(18.dp),
                elevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_card_${goal.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            EmojiBadge(
                                emoji = resolveCategoryEmoji(goal.iconName, goal.name),
                                tintColor = goalColor,
                                size = 42.dp,
                                emojiSize = 20.sp,
                                shape = RoundedCornerShape(14.dp),
                                contentDescription = goal.name
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(goal.name, style = MaterialTheme.typography.titleMedium)
                                Text(etaText, style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            }
                        }
                        Text(
                            text = "${(rawRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = goalColor
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedFinProgressBar(
                        progress = rawRatio,
                        color = goalColor,
                        height = 8.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${formatCurrency(goal.savedAmount, state.isBalanceHidden)} / ${formatCurrency(goal.targetAmount, state.isBalanceHidden)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${formatCurrency(goal.monthlyContribution, state.isBalanceHidden)}/mo" +
                                    if (goal.isRecurringContribution) " • Auto-recurring" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = semanticColors.textSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { contributingGoal = goal },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("goal_contribute_${goal.id}")
                            ) {
                                Text("+ Add")
                            }
                            IconButton(onClick = { onDeleteGoal(goal) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete ${goal.name}", tint = semanticColors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = FinFlowTheme.colors.textSecondary)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
