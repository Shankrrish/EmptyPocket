package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.CardSummary
import com.example.ui.components.AnimatedFinProgressBar
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.theme.FinFlowTheme
import com.example.util.DateUtils
import com.example.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDetailSheet(
    summary: CardSummary,
    recentCardTransactions: List<TransactionEntity>,
    isBalanceHidden: Boolean,
    onPayBill: () -> Unit,
    onViewTransactions: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    val usedRatio = if (summary.card.limitAmount > 0) {
        (summary.openInvoice / summary.card.limitAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = semanticColors.surfaceElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("credit_card_detail_sheet")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(
                    emoji = "💳",
                    tintColor = Color(summary.card.colorHex),
                    size = 46.dp,
                    emojiSize = 22.sp,
                    shape = RoundedCornerShape(14.dp),
                    contentDescription = summary.card.name
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.card.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = buildList {
                            if (summary.card.issuerBank.isNotBlank()) add(summary.card.issuerBank)
                            add(summary.card.brand)
                            add("Stmt ${summary.card.statementDay}")
                            add("Due ${summary.card.dueDay}")
                        }.joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = semanticColors.textSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = semanticColors.warningContainer
                ) {
                    Text(
                        text = "Due ${summary.card.dueDay}",
                        style = MaterialTheme.typography.labelMedium,
                        color = semanticColors.warning,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FinSurfaceCard(
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                elevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Outstanding", style = MaterialTheme.typography.labelMedium, color = semanticColors.textSecondary)
                            Text(
                                text = formatCurrency(summary.openInvoice, isBalanceHidden),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.openInvoice > 0.0) semanticColors.expense else semanticColors.income,
                                modifier = Modifier.testTag("card_detail_outstanding")
                            )
                            if (summary.minimumDue > 0.0) {
                                Text(
                                    text = "Min Due: ${formatCurrency(summary.minimumDue, isBalanceHidden)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semanticColors.warning
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Available Limit", style = MaterialTheme.typography.labelMedium, color = semanticColors.textSecondary)
                            Text(
                                text = formatCurrency(summary.availableLimit, isBalanceHidden),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = semanticColors.income,
                                modifier = Modifier.testTag("card_detail_available")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedFinProgressBar(
                        progress = usedRatio,
                        color = if (usedRatio > 0.8f) semanticColors.warning else MaterialTheme.colorScheme.primary,
                        height = 8.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Limit: ${formatCurrency(summary.card.limitAmount, isBalanceHidden)} • Closes ${summary.card.closingDay} • ${(usedRatio * 100).toInt()}% utilized",
                        style = MaterialTheme.typography.labelSmall,
                        color = semanticColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Recent Card Activity", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (recentCardTransactions.isEmpty()) {
                Text(
                    text = "No transactions on this card in the selected month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = semanticColors.textSecondary
                )
            } else {
                recentCardTransactions.take(4).forEachIndexed { idx, tx ->
                    val isPayment = tx.type == TransactionType.CC_PAYMENT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(text = if (isPayment) "🧾" else "💳", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tx.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(DateUtils.formatDateHeader(tx.dateMillis), style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                            }
                        }
                        Text(
                            text = (if (isPayment) "-" else "+") + formatCurrency(tx.amount, isBalanceHidden),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPayment) semanticColors.income else semanticColors.expense
                        )
                    }
                    if (idx < recentCardTransactions.take(4).lastIndex) {
                        HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onViewTransactions()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_detail_view_tx_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Transactions")
                }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPayBill()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_detail_pay_bill_button")
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pay Bill")
                }
            }
        }
    }
}
