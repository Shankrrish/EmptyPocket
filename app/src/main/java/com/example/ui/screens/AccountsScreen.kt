package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.CreditCardEntity
import com.example.ui.AccountSummary
import com.example.ui.CardSummary
import com.example.ui.FinanceUiState
import com.example.ui.components.AnimatedFinProgressBar
import com.example.ui.components.EmojiBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinSurfaceCard
import com.example.ui.components.accountEmoji
import com.example.ui.theme.FinFlowTheme
import com.example.util.formatCurrency

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.filled.ArrowBack

private enum class AccountsSectionFilter(val label: String) {
    ALL("All"),
    BANKS("🏦 Banks"),
    CASH("💵 Cash"),
    WALLETS("📱 E-Wallets"),
    CARDS("💳 Cards")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsScreen(
    state: FinanceUiState,
    onInspectAccount: (Long) -> Unit,
    onQuickTransferFrom: (Long) -> Unit,
    onSaveAccount: (AccountEntity) -> Unit,
    onToggleArchiveAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onOpenCardDetails: (CardSummary) -> Unit,
    onPayCreditCard: (Long) -> Unit,
    onSaveCard: (CreditCardEntity) -> Unit,
    onToggleArchiveCard: (CreditCardEntity) -> Unit,
    onDeleteCard: (CreditCardEntity) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    var filter by remember { mutableStateOf(AccountsSectionFilter.ALL) }
    var showArchived by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    var showCardSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    if (showAccountSheet) {
        AccountEditorBottomSheet(
            initial = editingAccount,
            onDismiss = { showAccountSheet = false },
            onSave = {
                onSaveAccount(it)
                showAccountSheet = false
            },
            onDelete = if (editingAccount != null) {
                {
                    onDeleteAccount(editingAccount!!)
                    showAccountSheet = false
                }
            } else null
        )
    }

    if (showCardSheet) {
        CreditCardEditorBottomSheet(
            initial = editingCard,
            onDismiss = { showCardSheet = false },
            onSave = {
                onSaveCard(it)
                showCardSheet = false
            },
            onDelete = if (editingCard != null) {
                {
                    onDeleteCard(editingCard!!)
                    showCardSheet = false
                }
            } else null
        )
    }

    val visibleAccounts = remember(state.allAccountSummaries, showArchived) {
        state.allAccountSummaries.filter { it.account.isArchived == showArchived }
    }
    val bankAccounts = remember(visibleAccounts) { visibleAccounts.filter { it.account.type.isBank() } }
    val cashAccounts = remember(visibleAccounts) { visibleAccounts.filter { it.account.type.isCash() } }
    val walletAccounts = remember(visibleAccounts) { visibleAccounts.filter { it.account.type.isWallet() } }
    val visibleCards = remember(state.allCardSummaries, showArchived) {
        state.allCardSummaries.filter { it.card.isArchived == showArchived }
    }

    val totalCardDebt = state.cardSummaries.sumOf { it.openInvoice }
    val totalCardAvail = state.cardSummaries.sumOf { it.availableLimit }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("accounts_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("accounts_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accounts & Cards", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Banks, cash, prepaid e-wallets & credit limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = semanticColors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            editingAccount = null
                            showAccountSheet = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("accounts_add_account_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Account")
                    }
                    OutlinedButton(
                        onClick = {
                            editingCard = null
                            showCardSheet = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("accounts_add_card_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Card")
                    }
                }
            }
        }

        // Liquid vs Credit Summary Card
        item {
            FinSurfaceCard(
                shape = RoundedCornerShape(20.dp),
                backgroundBrush = Brush.linearGradient(
                    listOf(semanticColors.heroGradientStart, semanticColors.heroGradientEnd)
                ),
                borderColor = semanticColors.heroBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Liquid Balance", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(state.totalBalance, state.isBalanceHidden),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = semanticColors.income,
                            modifier = Modifier.testTag("accounts_total_liquid")
                        )
                        Text(
                            text = "${state.accountSummaries.size} active accounts",
                            style = MaterialTheme.typography.labelSmall,
                            color = semanticColors.textSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text("Card Outstanding", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(totalCardDebt, state.isBalanceHidden),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (totalCardDebt > 0) semanticColors.expense else semanticColors.income
                        )
                        Text(
                            text = "Avail: ${formatCurrency(totalCardAvail, state.isBalanceHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = semanticColors.textSecondary
                        )
                    }
                }
            }
        }

        // Filter row + Archived toggle
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AccountsSectionFilter.entries) { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            filter = f
                        },
                        label = { Text(f.label) },
                        modifier = Modifier.testTag("accounts_filter_${f.name.lowercase()}")
                    )
                }
                item {
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = !showArchived },
                        label = { Text(if (showArchived) "📦 Showing Archived" else "📦 Archived") },
                        modifier = Modifier.testTag("accounts_archived_toggle")
                    )
                }
            }
        }

        val showBanks = filter == AccountsSectionFilter.ALL || filter == AccountsSectionFilter.BANKS
        val showCash = filter == AccountsSectionFilter.ALL || filter == AccountsSectionFilter.CASH
        val showWallets = filter == AccountsSectionFilter.ALL || filter == AccountsSectionFilter.WALLETS
        val showCards = filter == AccountsSectionFilter.ALL || filter == AccountsSectionFilter.CARDS

        if (showBanks && (bankAccounts.isNotEmpty() || filter == AccountsSectionFilter.BANKS)) {
            item {
                Text("🏦 Bank Accounts", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            if (bankAccounts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = if (showArchived) "No archived banks" else "No bank accounts",
                        subtitle = "Tap + Account to add HDFC, ICICI, SBI, Equitas SFB, Post Office, or a custom bank.",
                        emoji = "🏦"
                    )
                }
            } else {
                items(bankAccounts, key = { "bank_${it.account.id}" }) { summary ->
                    RichAccountRowCard(
                        summary = summary,
                        isBalanceHidden = state.isBalanceHidden,
                        onInspect = { onInspectAccount(summary.account.id) },
                        onTransfer = { onQuickTransferFrom(summary.account.id) },
                        onEdit = {
                            editingAccount = summary.account
                            showAccountSheet = true
                        },
                        onToggleArchive = { onToggleArchiveAccount(summary.account) }
                    )
                }
            }
        }

        if (showCash && (cashAccounts.isNotEmpty() || filter == AccountsSectionFilter.CASH)) {
            item {
                Text("💵 Physical Cash", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            if (cashAccounts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No cash accounts",
                        subtitle = "Track Home Cash, Personal Purse, or Wallet Cash separately.",
                        emoji = "💵"
                    )
                }
            } else {
                items(cashAccounts, key = { "cash_${it.account.id}" }) { summary ->
                    RichAccountRowCard(
                        summary = summary,
                        isBalanceHidden = state.isBalanceHidden,
                        onInspect = { onInspectAccount(summary.account.id) },
                        onTransfer = { onQuickTransferFrom(summary.account.id) },
                        onEdit = {
                            editingAccount = summary.account
                            showAccountSheet = true
                        },
                        onToggleArchive = { onToggleArchiveAccount(summary.account) }
                    )
                }
            }
        }

        if (showWallets && (walletAccounts.isNotEmpty() || filter == AccountsSectionFilter.WALLETS)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📱 Prepaid E-Wallets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = FinFlowTheme.colors.surfaceElevated,
                        border = BorderStroke(0.75.dp, FinFlowTheme.colors.hairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Prepaid wallet balances only (PhonePe, Paytm, Amazon Pay, Mobikwik). Direct UPI payments debit your linked bank account directly without creating duplicate wallet entries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinFlowTheme.colors.textSecondary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            if (walletAccounts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No prepaid e-wallets",
                        subtitle = "Add PhonePe Wallet, Paytm Wallet, Amazon Pay, or Mobikwik.",
                        emoji = "📱"
                    )
                }
            } else {
                items(walletAccounts, key = { "wallet_${it.account.id}" }) { summary ->
                    RichAccountRowCard(
                        summary = summary,
                        isBalanceHidden = state.isBalanceHidden,
                        onInspect = { onInspectAccount(summary.account.id) },
                        onTransfer = { onQuickTransferFrom(summary.account.id) },
                        onEdit = {
                            editingAccount = summary.account
                            showAccountSheet = true
                        },
                        onToggleArchive = { onToggleArchiveAccount(summary.account) }
                    )
                }
            }
        }

        if (showCards && (visibleCards.isNotEmpty() || filter == AccountsSectionFilter.CARDS)) {
            item {
                Text("💳 Credit Cards", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            if (visibleCards.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = if (showArchived) "No archived cards" else "No credit cards",
                        subtitle = "Add cards to track billing cycles, statement dates, available limits, and liability-settled bill payments.",
                        emoji = "💳"
                    )
                }
            } else {
                items(visibleCards, key = { "cc_${it.card.id}" }) { summary ->
                    RichCreditCardRow(
                        summary = summary,
                        isBalanceHidden = state.isBalanceHidden,
                        onOpenDetails = { onOpenCardDetails(summary) },
                        onPayBill = { onPayCreditCard(summary.card.id) },
                        onEdit = {
                            editingCard = summary.card
                            showCardSheet = true
                        },
                        onToggleArchive = { onToggleArchiveCard(summary.card) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RichAccountRowCard(
    summary: AccountSummary,
    isBalanceHidden: Boolean,
    onInspect: () -> Unit,
    onTransfer: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit
) {
    val acc = summary.account
    val accColor = Color(acc.colorHex)
    val semanticColors = FinFlowTheme.colors
    val subtitleParts = buildList {
        if (acc.bankName.isNotBlank()) add(acc.bankName)
        if (acc.accountSubType.isNotBlank()) add(acc.accountSubType)
        if (acc.maskedNumber.isNotBlank()) add(acc.maskedNumber)
        if (acc.ifsc.isNotBlank()) add(acc.ifsc)
    }

    FinSurfaceCard(
        shape = RoundedCornerShape(18.dp),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onInspect)
            .testTag("rich_account_card_${acc.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(
                    emoji = accountEmoji(acc.type, acc.emoji),
                    tintColor = accColor,
                    size = 44.dp,
                    emojiSize = 20.sp,
                    shape = RoundedCornerShape(14.dp),
                    contentDescription = acc.name
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = acc.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleParts.joinToString(" • ").ifBlank { acc.type.displayLabel },
                        style = MaterialTheme.typography.labelSmall,
                        color = semanticColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatCurrency(summary.currentBalance, isBalanceHidden),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.currentBalance >= 0) semanticColors.income else semanticColors.expense
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!acc.isArchived) {
                    IconButton(onClick = onTransfer, modifier = Modifier.testTag("account_transfer_${acc.id}")) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer from ${acc.name}", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.testTag("account_edit_${acc.id}")) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${acc.name}", tint = semanticColors.textSecondary)
                }
                IconButton(onClick = onToggleArchive, modifier = Modifier.testTag("account_archive_${acc.id}")) {
                    Icon(
                        imageVector = if (acc.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = if (acc.isArchived) "Restore ${acc.name}" else "Archive ${acc.name}",
                        tint = semanticColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RichCreditCardRow(
    summary: CardSummary,
    isBalanceHidden: Boolean,
    onOpenDetails: () -> Unit,
    onPayBill: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit
) {
    val card = summary.card
    val semanticColors = FinFlowTheme.colors
    val ratio = if (card.limitAmount > 0.0) (summary.openInvoice / card.limitAmount).toFloat().coerceIn(0f, 1f) else 0f

    val cardColor = Color(card.colorHex)
    FinSurfaceCard(
        shape = RoundedCornerShape(18.dp),
        backgroundBrush = Brush.linearGradient(
            listOf(
                cardColor.copy(alpha = if (semanticColors.isAmoledDark) 0.22f else 0.12f),
                semanticColors.heroGradientEnd
            )
        ),
        borderColor = cardColor.copy(alpha = if (semanticColors.isAmoledDark) 0.42f else 0.28f),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenDetails)
            .testTag("rich_card_row_${card.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(
                    emoji = "💳",
                    tintColor = Color(card.colorHex),
                    size = 44.dp,
                    emojiSize = 20.sp,
                    shape = RoundedCornerShape(14.dp),
                    contentDescription = card.name
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = buildList {
                            if (card.issuerBank.isNotBlank()) add(card.issuerBank)
                            add(card.brand)
                            add("Stmt ${card.statementDay}")
                            add("Due ${card.dueDay}")
                        }.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = semanticColors.textSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(summary.openInvoice, isBalanceHidden),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.openInvoice > 0.0) semanticColors.expense else semanticColors.income
                    )
                    if (summary.minimumDue > 0.0) {
                        Text(
                            text = "Min Due: ${formatCurrency(summary.minimumDue, isBalanceHidden)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = semanticColors.warning
                        )
                    }
                }
            }
            AnimatedFinProgressBar(
                progress = ratio,
                color = if (ratio > 0.8f) semanticColors.warning else MaterialTheme.colorScheme.primary,
                height = 7.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avail: ${formatCurrency(summary.availableLimit, isBalanceHidden)} / ${formatCurrency(card.limitAmount, isBalanceHidden)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = semanticColors.textSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (summary.openInvoice > 0.0 && !card.isArchived) {
                        OutlinedButton(
                            onClick = onPayBill,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("rich_card_pay_${card.id}")
                        ) { Text("Pay Bill", fontSize = 12.sp) }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit ${card.name}", tint = semanticColors.textSecondary)
                    }
                    IconButton(onClick = onToggleArchive) {
                        Icon(
                            imageVector = if (card.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (card.isArchived) "Restore ${card.name}" else "Archive ${card.name}",
                            tint = semanticColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
