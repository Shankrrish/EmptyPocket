package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringBillEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.TransactionType
import com.example.ui.FinanceAnalytics
import com.example.ui.FinanceUiState
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.theme.FinFlowTheme
import com.example.util.formatCurrency
import java.util.Calendar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecurringManagerContent(
    state: FinanceUiState,
    onSaveBill: (RecurringBillEntity) -> Unit,
    onDeleteBill: (RecurringBillEntity) -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    var showAddDialog by remember { mutableStateOf(false) }
    var prefilledDueDay by remember { mutableIntStateOf(10) }

    val cal = remember(state.selectedMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, state.selectedMonth.year)
            set(Calendar.MONTH, state.selectedMonth.month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDay)

    val calendarEvents = remember(
        state.recurringCalendarEvents,
        state.recurringBills,
        state.creditCards,
        state.cardSummaries
    ) {
        state.recurringCalendarEvents.ifEmpty {
            FinanceAnalytics.computeRecurringCalendarEvents(
                bills = state.recurringBills,
                cards = state.creditCards,
                cardSummaries = state.cardSummaries
            )
        }
    }
    val eventsByDay = remember(calendarEvents) { calendarEvents.groupBy { it.dayOfMonth } }

    var selectedDay by remember(state.selectedMonth, calendarEvents.size) {
        val defaultDay = when {
            eventsByDay[today]?.isNotEmpty() == true -> today
            calendarEvents.isNotEmpty() -> calendarEvents.first().dayOfMonth.coerceIn(1, maxDay)
            else -> today
        }
        mutableIntStateOf(defaultDay)
    }

    if (showAddDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var title by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var dueDayText by remember(prefilledDueDay) { mutableStateOf(prefilledDueDay.toString()) }
        var isSubscription by remember { mutableStateOf(true) }
        var frequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }

        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = sheetState,
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Recurring Bill / EMI / Subscription",
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name (e.g. Netflix, Home EMI, Rent)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_title_input")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_amount_input")
                )
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it },
                    label = { Text("Due Day of Month (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_due_day_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurringFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark as Subscription", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isSubscription, onCheckedChange = { isSubscription = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            val day = (dueDayText.toIntOrNull() ?: prefilledDueDay).coerceIn(1, 31)
                            if (title.isNotBlank() && amt > 0.0) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSaveBill(
                                    RecurringBillEntity(
                                        title = title.trim(),
                                        amount = amt,
                                        dueDay = day,
                                        frequency = frequency,
                                        type = TransactionType.EXPENSE,
                                        isSubscription = isSubscription
                                    )
                                )
                                selectedDay = day.coerceIn(1, maxDay)
                                showAddDialog = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("recurring_save_button")
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }

    val changedBills = state.recurringBills.filter { it.previousAmount != null && it.previousAmount != it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("recurring_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Subscription Cost Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FinSurfaceCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(
                                emoji = "🎬",
                                tintColor = MaterialTheme.colorScheme.primary,
                                size = 32.dp,
                                emojiSize = 15.sp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Monthly Subs", style = MaterialTheme.typography.labelMedium, color = semanticColors.textSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatCurrency(state.monthlySubscriptionsTotal, state.isBalanceHidden),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                FinSurfaceCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(
                                emoji = "📅",
                                tintColor = semanticColors.income,
                                size = 32.dp,
                                emojiSize = 15.sp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Annual Subs", style = MaterialTheme.typography.labelMedium, color = semanticColors.textSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatCurrency(state.annualSubscriptionsTotal, state.isBalanceHidden),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Amount-change alerts
        if (changedBills.isNotEmpty()) {
            item {
                FinSurfaceCard(
                    shape = RoundedCornerShape(18.dp),
                    containerColor = semanticColors.warningContainer,
                    borderColor = semanticColors.warning.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_amount_change_alert")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Amount Change Alerts",
                                style = MaterialTheme.typography.labelLarge,
                                color = semanticColors.warning
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        changedBills.forEach { bill ->
                            val prev = bill.previousAmount ?: bill.amount
                            Text(
                                text = "• ${bill.title}: ${formatCurrency(prev, state.isBalanceHidden)} → ${formatCurrency(bill.amount, state.isBalanceHidden)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Interactive Calendar View for Bills, EMIs & Card Due Dates
        item {
            val selectedDayEvents = eventsByDay[selectedDay].orEmpty()

            FinSurfaceCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_calendar_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(
                            emoji = "🗓️",
                            tintColor = MaterialTheme.colorScheme.primary,
                            size = 36.dp,
                            emojiSize = 17.sp,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bills & Card Due Calendar", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Tap any day to inspect due EMIs, bills & cards",
                                style = MaterialTheme.typography.bodySmall,
                                color = semanticColors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FlowRow(
                        maxItemsInEachRow = 7,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (day in 1..maxDay) {
                            val dayEvents = eventsByDay[day].orEmpty()
                            val hasCardDue = dayEvents.any { it.isCreditCardDue }
                            val hasBill = dayEvents.any { !it.isCreditCardDue }
                            val isSelected = selectedDay == day
                            val bg = when {
                                hasCardDue -> semanticColors.warningContainer
                                hasBill -> MaterialTheme.colorScheme.primaryContainer
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                day == today -> MaterialTheme.colorScheme.surfaceVariant
                                else -> Color.Transparent
                            }
                            val borderStroke = when {
                                isSelected && hasCardDue -> BorderStroke(1.5.dp, semanticColors.warning)
                                isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else -> null
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(bg, CircleShape)
                                    .then(
                                        if (borderStroke != null) {
                                            Modifier.border(borderStroke, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedDay = day
                                    }
                                    .testTag("calendar_day_$day")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$day",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (dayEvents.isNotEmpty() || day == today || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            hasCardDue -> semanticColors.warning
                                            hasBill -> MaterialTheme.colorScheme.onPrimaryContainer
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                    if (dayEvents.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(
                                                    color = if (hasCardDue) semanticColors.warning else MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("● Bills, EMIs & Subs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("● Card Due Dates", style = MaterialTheme.typography.labelSmall, color = semanticColors.warning)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Interactive Selected Day Due Items Panel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_selected_day_panel"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${state.selectedMonth.displayLabel().substringBefore(" ")} $selectedDay Due Items",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (selectedDayEvents.isEmpty()) "No scheduled payments" else "${selectedDayEvents.size} item(s) due on Day $selectedDay",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semanticColors.textSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    prefilledDueDay = selectedDay
                                    showAddDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("calendar_add_bill_day_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add on Day $selectedDay", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (selectedDayEvents.isEmpty()) {
                            Text(
                                text = "No bills, EMIs, or credit card payments scheduled on Day $selectedDay.",
                                style = MaterialTheme.typography.bodySmall,
                                color = semanticColors.textSecondary,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .testTag("calendar_selected_day_empty")
                            )
                        } else {
                            selectedDayEvents.forEachIndexed { index, ev ->
                                val itemEmoji = when {
                                    ev.isCreditCardDue -> "💳"
                                    ev.isSubscription -> "🎬"
                                    else -> "🔔"
                                }
                                val itemTint = if (ev.isCreditCardDue) semanticColors.warning else MaterialTheme.colorScheme.primary
                                val itemSubtitle = when {
                                    ev.isCreditCardDue -> "Credit Card Bill Due"
                                    ev.isSubscription -> "Subscription • Due Day ${ev.dayOfMonth}"
                                    else -> "Recurring Bill / EMI • Due Day ${ev.dayOfMonth}"
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(semanticColors.surfaceElevated)
                                        .border(
                                            BorderStroke(0.75.dp, semanticColors.hairlineBorder),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("calendar_due_item_$index")
                                ) {
                                    EmojiBadge(
                                        emoji = itemEmoji,
                                        tintColor = itemTint,
                                        size = 36.dp,
                                        emojiSize = 17.sp,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ev.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = itemSubtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = semanticColors.textSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (ev.amount > 0.0) {
                                            formatCurrency(ev.amount, state.isBalanceHidden)
                                        } else {
                                            "Statement Due"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ev.isCreditCardDue) semanticColors.warning else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Header + Add button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scheduled Recurring & Cards", style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = {
                        prefilledDueDay = selectedDay
                        showAddDialog = true
                    },
                    modifier = Modifier.testTag("add_recurring_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Recurring Bill")
                }
            }
        }

        // Credit card due reminders
        items(state.creditCards, key = { "cc_due_${it.id}" }) { card ->
            val cardSummary = state.cardSummaries.find { it.card.id == card.id }
            FinSurfaceCard(shape = RoundedCornerShape(16.dp), elevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedDay = card.dueDay.coerceIn(1, maxDay)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmojiBadge(
                        emoji = "💳",
                        tintColor = semanticColors.warning,
                        size = 40.dp,
                        emojiSize = 18.sp,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${card.name} Bill Due", style = MaterialTheme.typography.titleMedium)
                        val openStr = if (cardSummary != null && cardSummary.openInvoice > 0.0) {
                            " • Bill: ${formatCurrency(cardSummary.openInvoice, state.isBalanceHidden)}"
                        } else ""
                        Text(
                            text = "Due every ${card.dueDay}th • Closes ${card.closingDay}th$openStr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = semanticColors.textSecondary
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = semanticColors.warningContainer) {
                        Text(
                            text = "Day ${card.dueDay}",
                            style = MaterialTheme.typography.labelSmall,
                            color = semanticColors.warning,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Recurring Bills & Subscriptions list
        items(state.recurringBills, key = { "bill_${it.id}" }) { bill ->
            val daysUntil = bill.dueDay - today
            val reminderText = when {
                daysUntil in 0..bill.reminderDaysBefore -> "Due in ${daysUntil}d"
                daysUntil < 0 -> "Day ${bill.dueDay}"
                else -> "Day ${bill.dueDay} (${bill.frequency.name.lowercase()})"
            }
            val billEmoji = if (bill.isSubscription) "🎬" else "🔔"
            FinSurfaceCard(
                shape = RoundedCornerShape(16.dp),
                elevation = 0.dp,
                modifier = Modifier.testTag("recurring_bill_${bill.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedDay = bill.dueDay.coerceIn(1, maxDay)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmojiBadge(
                        emoji = billEmoji,
                        tintColor = MaterialTheme.colorScheme.primary,
                        size = 40.dp,
                        emojiSize = 18.sp,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bill.title, style = MaterialTheme.typography.titleMedium)
                        Text(reminderText, style = MaterialTheme.typography.bodyMedium, color = semanticColors.textSecondary)
                    }
                    Text(
                        text = formatCurrency(bill.amount, state.isBalanceHidden),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onDeleteBill(bill) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete ${bill.title}", tint = semanticColors.textSecondary)
                    }
                }
            }
        }
    }
}
