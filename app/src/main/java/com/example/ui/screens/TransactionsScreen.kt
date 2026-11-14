package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SplitAllocation
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.FinanceUiState
import com.example.ui.TxFilter
import com.example.ui.components.EmojiBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinSurfaceCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.transactionEmoji
import com.example.ui.theme.FinFlowTheme
import com.example.util.DateUtils
import com.example.util.formatCurrency

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    state: FinanceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleVisibility: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (TxFilter) -> Unit,
    onClearContextFilters: () -> Unit,
    onTogglePaid: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onOpenAutoCapture: () -> Unit,
    onOpenRules: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    val grouped = remember(state.filteredTransactions) {
        state.filteredTransactions.groupBy { DateUtils.dayStartKey(it.dateMillis) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MonthSelectorBar(
            selectedMonth = state.selectedMonth,
            isBalanceHidden = state.isBalanceHidden,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            onToggleVisibility = onToggleVisibility
        )

        // Quick actions for India Auto-Capture & Smart Categorization Rules
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenAutoCapture()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_auto_capture_button")
            ) {
                Text("📲", fontSize = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("SMS / UPI / CSV", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenRules()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_rules_button")
            ) {
                Text("✨", fontSize = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Smart Rules", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text(stringResource(R.string.search_transactions_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = semanticColors.hairlineBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("transactions_search_input")
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TxFilter.entries) { filter ->
                val label = when (filter) {
                    TxFilter.ALL -> "All"
                    TxFilter.EXPENSE -> "💸 Expenses"
                    TxFilter.INCOME -> "💰 Income"
                    TxFilter.CARD -> "💳 Credit Card"
                    TxFilter.TRANSFER_INVEST -> "🔄 Transfers & Invest"
                    TxFilter.PENDING -> "⏳ Pending"
                    TxFilter.DUPLICATES -> "⚠️ Duplicates (${state.duplicateTransactionIds.size})"
                }
                FilterChip(
                    selected = state.txFilter == filter,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFilterChange(filter)
                    },
                    label = { Text(label) },
                    modifier = Modifier.testTag("tx_filter_${filter.name.lowercase()}")
                )
            }
        }

        val contextLabel = when {
            state.selectedCategoryFilterId != null ->
                "Category: ${state.categories.find { it.id == state.selectedCategoryFilterId }?.name ?: "Selected"}"
            state.selectedCardFilterId != null ->
                "Card: ${state.creditCards.find { it.id == state.selectedCardFilterId }?.name ?: "Selected"}"
            state.selectedAccountFilterId != null ->
                "Account: ${state.accounts.find { it.id == state.selectedAccountFilterId }?.name ?: "Selected"}"
            else -> null
        }

        if (contextLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = true,
                    onClick = onClearContextFilters,
                    label = { Text(contextLabel) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Clear contextual filter", modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("active_context_filter_chip")
                )
            }
        }

        if (grouped.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    title = "No matching transactions",
                    subtitle = "Adjust your filters or tap + to add an entry, or import via SMS / UPI / CSV.",
                    emoji = "🧾"
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("transactions_list")
            ) {
                grouped.forEach { (dayKey, txs) ->
                    val dayIn = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val dayOut = txs.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.CC_EXPENSE }.sumOf { it.amount }
                    item(key = "header_$dayKey") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, bottom = 6.dp, start = 2.dp, end = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateUtils.formatDateHeader(dayKey),
                                style = MaterialTheme.typography.labelLarge,
                                color = semanticColors.textSecondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dayIn > 0.0) {
                                    Text(
                                        text = "+${formatCurrency(dayIn, state.isBalanceHidden)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semanticColors.income
                                    )
                                }
                                if (dayOut > 0.0) {
                                    Text(
                                        text = "-${formatCurrency(dayOut, state.isBalanceHidden)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semanticColors.expense
                                    )
                                }
                            }
                        }
                    }
                    item(key = "day_card_$dayKey") {
                        FinSurfaceCard(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                txs.forEachIndexed { idx, tx ->
                                    val category = state.categories.find { it.id == tx.categoryId }
                                    val splits = SplitAllocation.decode(tx.splitSummary)
                                    val isDup = tx.id in state.duplicateTransactionIds
                                    val sourceName = when (tx.type) {
                                        TransactionType.TRANSFER -> {
                                            val fromName = state.accounts.find { it.id == tx.accountId }?.name ?: "Account"
                                            val toName = state.accounts.find { it.id == tx.toAccountId }?.name ?: "Savings"
                                            "$fromName → $toName"
                                        }
                                        TransactionType.CC_PAYMENT -> {
                                            val fromName = state.accounts.find { it.id == tx.accountId }?.name ?: "Bank"
                                            val cardName = state.creditCards.find { it.id == tx.creditCardId }?.name ?: "Credit Card"
                                            "Card Bill: $fromName → $cardName"
                                        }
                                        TransactionType.CC_EXPENSE -> {
                                            val cardName = state.creditCards.find { it.id == tx.creditCardId }?.name ?: "Credit Card"
                                            "Card • $cardName"
                                        }
                                        TransactionType.INVESTMENT_BUY,
                                        TransactionType.INVESTMENT_SELL,
                                        TransactionType.INVESTMENT_INCOME -> {
                                            val accName = state.accounts.find { it.id == tx.accountId }?.name ?: "Portfolio"
                                            "${tx.type.label} • $accName"
                                        }
                                        TransactionType.VALUATION_ADJUSTMENT,
                                        TransactionType.INTEREST_CREDIT -> tx.type.label
                                        else -> {
                                            state.creditCards.find { it.id == tx.creditCardId }?.name
                                                ?: state.accounts.find { it.id == tx.accountId }?.name
                                                ?: "Account"
                                        }
                                    }
                                    val amountColor = when (tx.type) {
                                        TransactionType.INCOME,
                                        TransactionType.INVESTMENT_SELL,
                                        TransactionType.INVESTMENT_INCOME,
                                        TransactionType.INTEREST_CREDIT -> semanticColors.income
                                        TransactionType.EXPENSE,
                                        TransactionType.CC_EXPENSE -> semanticColors.expense
                                        TransactionType.CC_PAYMENT -> semanticColors.cardPayment
                                        TransactionType.TRANSFER,
                                        TransactionType.INVESTMENT_BUY,
                                        TransactionType.VALUATION_ADJUSTMENT -> MaterialTheme.colorScheme.primary
                                    }
                                    val badgeTint = when (tx.type) {
                                        TransactionType.CC_PAYMENT -> semanticColors.cardPayment
                                        TransactionType.TRANSFER,
                                        TransactionType.INVESTMENT_BUY,
                                        TransactionType.VALUATION_ADJUSTMENT -> MaterialTheme.colorScheme.primary
                                        TransactionType.INVESTMENT_SELL,
                                        TransactionType.INVESTMENT_INCOME,
                                        TransactionType.INTEREST_CREDIT -> semanticColors.income
                                        else -> category?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
                                    }
                                    val emoji = transactionEmoji(tx.type, category)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onEditTransaction(tx)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .testTag("transaction_card_${tx.id}")
                                    ) {
                                        EmojiBadge(
                                            emoji = emoji,
                                            tintColor = badgeTint,
                                            size = 44.dp,
                                            emojiSize = 21.sp,
                                            shape = RoundedCornerShape(14.dp),
                                            contentDescription = category?.name ?: tx.type.name
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = tx.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (tx.isRecurring) {
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(text = "🔁", fontSize = 13.sp)
                                                }
                                            }
                                            Text(
                                                text = sourceName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = semanticColors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val tagList = tx.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                            if (tagList.isNotEmpty() || splits.isNotEmpty() || isDup) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isDup) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = semanticColors.warningContainer
                                                        ) {
                                                            Text(
                                                                text = "⚠️ Duplicate?",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = semanticColors.warning,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    if (splits.isNotEmpty()) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.secondaryContainer
                                                        ) {
                                                            Text(
                                                                text = "✂️ Split (${splits.size})",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    tagList.take(3).forEach { tag ->
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.surfaceVariant
                                                        ) {
                                                            Text(
                                                                text = "#$tag",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = semanticColors.textSecondary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            val prefix = when (tx.type) {
                                                TransactionType.INCOME,
                                                TransactionType.INVESTMENT_SELL,
                                                TransactionType.INVESTMENT_INCOME,
                                                TransactionType.INTEREST_CREDIT -> "+"
                                                TransactionType.EXPENSE,
                                                TransactionType.CC_EXPENSE -> "-"
                                                TransactionType.TRANSFER,
                                                TransactionType.CC_PAYMENT,
                                                TransactionType.INVESTMENT_BUY,
                                                TransactionType.VALUATION_ADJUSTMENT -> ""
                                            }
                                            Text(
                                                text = "$prefix${formatCurrency(tx.amount, state.isBalanceHidden)}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = amountColor
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (tx.isPaid) semanticColors.incomeContainer else semanticColors.warningContainer,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onTogglePaid(tx)
                                                    }
                                                    .testTag("tx_status_pill_${tx.id}")
                                            ) {
                                                Text(
                                                    text = if (tx.isPaid) {
                                                        if (tx.type == TransactionType.INCOME) "Received" else "Paid"
                                                    } else "Pending",
                                                    color = if (tx.isPaid) semanticColors.income else semanticColors.warning,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (idx < txs.lastIndex) {
                                        HorizontalDivider(
                                            thickness = 0.75.dp,
                                            color = semanticColors.hairlineBorder,
                                            modifier = Modifier.padding(horizontal = 14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
