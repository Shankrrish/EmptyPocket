package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryType
import com.example.data.model.SplitAllocation
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.FinanceUiState
import com.example.ui.components.CategoryCardGrid
import com.example.ui.components.CustomCategoryDialog
import com.example.ui.components.accountEmoji
import com.example.ui.theme.FinFlowTheme
import com.example.util.DateUtils
import com.example.util.IndiaCaptureParser
import com.example.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionSheet(
    state: FinanceUiState,
    initialType: TransactionType,
    preselectedCardId: Long? = null,
    preselectedAccountId: Long? = null,
    editingTx: TransactionEntity?,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity, Boolean) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onCreateCustomCategory: (String, CategoryType, String, Long, (Long) -> Unit) -> Unit = { _, _, _, _, _ -> }
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    var type by remember { mutableStateOf(editingTx?.type ?: initialType) }

    val initialCardSummary = preselectedCardId?.let { cid -> state.cardSummaries.find { it.card.id == cid } }
    var amountText by remember {
        mutableStateOf(
            editingTx?.amount?.toString()
                ?: if (initialType == TransactionType.CC_PAYMENT && initialCardSummary != null && initialCardSummary.openInvoice > 0) {
                    initialCardSummary.openInvoice.toString()
                } else ""
        )
    }
    var amountError by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(editingTx?.title ?: "") }
    var isPaid by remember { mutableStateOf(editingTx?.isPaid ?: true) }
    var isRecurring by remember { mutableStateOf(editingTx?.isRecurring ?: false) }
    var notes by remember { mutableStateOf(editingTx?.notes ?: "") }
    var tags by remember { mutableStateOf(editingTx?.tags ?: "") }
    var dateMillis by remember { mutableStateOf(editingTx?.dateMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val supportsCategories = type == TransactionType.EXPENSE ||
        type == TransactionType.CC_EXPENSE ||
        type == TransactionType.INCOME ||
        type == TransactionType.INVESTMENT_INCOME
    val filteredCategories = state.categories.filter {
        if (type == TransactionType.INCOME || type == TransactionType.INVESTMENT_INCOME) {
            it.type == CategoryType.INCOME
        } else {
            it.type == CategoryType.EXPENSE
        }
    }
    var categoryManuallyPicked by remember { mutableStateOf(editingTx != null) }
    var selectedCategoryId by remember(type) {
        mutableStateOf(
            editingTx?.categoryId?.takeIf { id -> filteredCategories.any { it.id == id } }
                ?: filteredCategories.firstOrNull()?.id
        )
    }
    var learnMerchantRule by remember { mutableStateOf(false) }

    // Transaction splitting across 2 categories
    val initialSplits = remember(editingTx) { SplitAllocation.decode(editingTx?.splitSummary ?: "") }
    var isSplitEnabled by remember { mutableStateOf(initialSplits.size >= 2) }
    var splitPrimaryAmountText by remember {
        mutableStateOf(initialSplits.firstOrNull()?.amount?.toString() ?: "")
    }
    var splitSecondaryCategoryId by remember {
        mutableStateOf(
            initialSplits.getOrNull(1)?.categoryId
                ?: filteredCategories.getOrNull(1)?.id
                ?: filteredCategories.firstOrNull()?.id
        )
    }
    var splitMenuExpanded by remember { mutableStateOf(false) }

    var selectedAccountId by remember {
        mutableStateOf(editingTx?.accountId ?: preselectedAccountId ?: state.accounts.firstOrNull()?.id)
    }
    var selectedCardId by remember {
        mutableStateOf(
            editingTx?.creditCardId
                ?: preselectedCardId
                ?: if (initialType == TransactionType.CC_EXPENSE || initialType == TransactionType.CC_PAYMENT) {
                    state.creditCards.firstOrNull()?.id
                } else null
        )
    }
    var toAccountId by remember {
        mutableStateOf(
            editingTx?.toAccountId
                ?: state.accounts.lastOrNull()?.id
                ?: state.accounts.firstOrNull()?.id
        )
    }
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var cardMenuExpanded by remember { mutableStateOf(false) }
    var toAccountMenuExpanded by remember { mutableStateOf(false) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val potentialDup = parsedAmount > 0.0 && IndiaCaptureParser.isPotentialDuplicate(
        amount = parsedAmount,
        dateMillis = dateMillis,
        title = title,
        existing = state.allTransactions,
        excludeId = editingTx?.id ?: -1L
    )

    var showCustomCategoryDialog by remember { mutableStateOf(false) }

    if (showCustomCategoryDialog) {
        CustomCategoryDialog(
            defaultType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE,
            onDismiss = { showCustomCategoryDialog = false },
            onSave = { catName, catType, emoji, colorHex ->
                onCreateCustomCategory(catName, catType, emoji, colorHex) { newId ->
                    selectedCategoryId = newId
                    categoryManuallyPicked = true
                }
                showCustomCategoryDialog = false
            }
        )
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dpState)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = semanticColors.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (editingTx == null) "New Entry" else "Edit Entry",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Categorize with expressive emojis & smart rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = semanticColors.textSecondary
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TransactionType.entries.forEach { t ->
                    val label = when (t) {
                        TransactionType.EXPENSE -> "💸 Expense"
                        TransactionType.CC_EXPENSE -> "💳 Card Spend"
                        TransactionType.CC_PAYMENT -> "🧾 Pay Card"
                        TransactionType.INCOME -> "💰 Income"
                        TransactionType.TRANSFER -> "🔄 Transfer"
                        TransactionType.INVESTMENT_BUY -> "📈 Invest Buy"
                        TransactionType.INVESTMENT_SELL -> "💵 Invest Sell"
                        TransactionType.INVESTMENT_INCOME -> "🪙 Invest Income"
                        TransactionType.VALUATION_ADJUSTMENT -> "📊 Valuation Adj."
                        TransactionType.INTEREST_CREDIT -> "🏛️ Interest"
                    }
                    FilterChip(
                        selected = type == t,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            type = t
                            when (t) {
                                TransactionType.CC_EXPENSE -> {
                                    selectedAccountId = null
                                    if (selectedCardId == null) selectedCardId = state.creditCards.firstOrNull()?.id
                                }
                                TransactionType.CC_PAYMENT -> {
                                    if (selectedAccountId == null) selectedAccountId = state.accounts.firstOrNull()?.id
                                    if (selectedCardId == null) selectedCardId = state.creditCards.firstOrNull()?.id
                                }
                                TransactionType.VALUATION_ADJUSTMENT,
                                TransactionType.INTEREST_CREDIT -> {
                                    selectedCardId = null
                                    selectedAccountId = null
                                }
                                else -> {
                                    selectedCardId = null
                                    if (selectedAccountId == null) selectedAccountId = state.accounts.firstOrNull()?.id
                                }
                            }
                        },
                        label = { Text(label) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("sheet_type_${t.name.lowercase()}")
                    )
                }
            }

            if (potentialDup) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semanticColors.warningContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("duplicate_warning_banner")
                ) {
                    Text(
                        text = "Possible duplicate detected: a similar transaction for ${formatCurrency(parsedAmount)} already exists around this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = semanticColors.warning,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (type == TransactionType.CC_PAYMENT) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Card bill payments transfer funds from your bank account to restore your card limit without double-counting expenses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                border = BorderStroke(0.75.dp, semanticColors.heroBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(semanticColors.heroGradientStart, semanticColors.cardSheenEnd)
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            amountError = false
                        },
                        label = { Text("Amount (₹)") },
                        isError = amountError,
                        supportingText = if (amountError) {
                            { Text("Enter a valid amount greater than 0") }
                        } else null,
                        textStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sheet_amount_input")
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { newTitle ->
                            title = newTitle
                            if (!categoryManuallyPicked && (type == TransactionType.EXPENSE || type == TransactionType.CC_EXPENSE || type == TransactionType.INCOME)) {
                                val matched = IndiaCaptureParser.matchCategoryByRules(
                                    text = newTitle,
                                    type = type,
                                    rules = state.rules,
                                    categories = state.categories
                                )
                                if (matched != null) {
                                    selectedCategoryId = matched.id
                                }
                            }
                        },
                        label = {
                            Text(
                                if (type == TransactionType.CC_PAYMENT) "Description (e.g. Monthly Card Bill)"
                                else "Merchant / Description (auto-categorizes)"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sheet_title_input")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateStr = DateUtils.formatIsoDate(dateMillis)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.testTag("sheet_date_button")
                ) {
                    Text(dateStr)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (type == TransactionType.INCOME) "Received" else "Paid",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isPaid,
                        onCheckedChange = { isPaid = it },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("sheet_paid_switch")
                    )
                }
            }

            if (supportsCategories) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!categoryManuallyPicked && title.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "✨ Auto-matched",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                CategoryCardGrid(
                    categories = filteredCategories,
                    selectedCategoryId = selectedCategoryId,
                    onSelectCategory = { cat ->
                        selectedCategoryId = cat.id
                        categoryManuallyPicked = true
                        if (title.trim().length >= 3) {
                            learnMerchantRule = true
                        }
                    },
                    onRequestCustomCategory = { showCustomCategoryDialog = true }
                )

                if (categoryManuallyPicked && title.trim().length >= 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Always categorize \"${title.trim().split(" ").take(2).joinToString(" ")}\" here",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = learnMerchantRule,
                            onCheckedChange = { learnMerchantRule = it },
                            modifier = Modifier.testTag("sheet_learn_rule_switch")
                        )
                    }
                }

                if (type == TransactionType.EXPENSE || type == TransactionType.CC_EXPENSE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Split across 2 categories", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isSplitEnabled,
                            onCheckedChange = { enabled ->
                                isSplitEnabled = enabled
                                if (enabled && splitPrimaryAmountText.isBlank() && parsedAmount > 0) {
                                    splitPrimaryAmountText = (parsedAmount / 2.0).toString()
                                }
                            },
                            modifier = Modifier.testTag("sheet_split_switch")
                        )
                    }

                    if (isSplitEnabled) {
                        val firstSplitAmt = (splitPrimaryAmountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, parsedAmount)
                        val secondSplitAmt = (parsedAmount - firstSplitAmt).coerceAtLeast(0.0)
                        val secondCatName = filteredCategories.find { it.id == splitSecondaryCategoryId }?.name ?: "Second Category"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = splitPrimaryAmountText,
                                onValueChange = { splitPrimaryAmountText = it },
                                label = { Text("1st Split (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            ExposedDropdownMenuBox(
                                expanded = splitMenuExpanded,
                                onExpandedChange = { splitMenuExpanded = it },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                OutlinedTextField(
                                    value = "$secondCatName (${formatCurrency(secondSplitAmt)})",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("2nd Category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = splitMenuExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = splitMenuExpanded,
                                    onDismissRequest = { splitMenuExpanded = false }
                                ) {
                                    filteredCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name) },
                                            onClick = {
                                                splitSecondaryCategoryId = cat.id
                                                splitMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val needsSourceAccount = type != TransactionType.CC_EXPENSE &&
                type != TransactionType.VALUATION_ADJUSTMENT &&
                type != TransactionType.INTEREST_CREDIT
            if (needsSourceAccount) {
                val selectedAcc = state.accounts.find { it.id == selectedAccountId }
                val accountLabel = selectedAcc?.name ?: "Select Account"
                ExposedDropdownMenuBox(
                    expanded = sourceMenuExpanded,
                    onExpandedChange = { sourceMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "Account: $accountLabel",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                when (type) {
                                    TransactionType.TRANSFER, TransactionType.CC_PAYMENT -> "From Bank / Cash / Wallet"
                                    TransactionType.INCOME, TransactionType.INVESTMENT_SELL, TransactionType.INVESTMENT_INCOME -> "Destination Account"
                                    TransactionType.INVESTMENT_BUY -> "Debited Bank Account"
                                    else -> "Bank / Cash / Prepaid Wallet"
                                }
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("sheet_source_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = sourceMenuExpanded,
                        onDismissRequest = { sourceMenuExpanded = false }
                    ) {
                        state.accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${accountEmoji(acc.type)} Account: ${acc.name}") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    sourceMenuExpanded = false
                                }
                            )
                        }
                        if (type == TransactionType.EXPENSE && state.creditCards.isNotEmpty()) {
                            state.creditCards.forEach { card ->
                                DropdownMenuItem(
                                    text = { Text("💳 Credit Card: ${card.name}") },
                                    onClick = {
                                        type = TransactionType.CC_EXPENSE
                                        selectedCardId = card.id
                                        selectedAccountId = null
                                        sourceMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (type == TransactionType.CC_EXPENSE || type == TransactionType.CC_PAYMENT) {
                val chosenSummary = state.cardSummaries.find { it.card.id == selectedCardId }
                val cardDisplay = chosenSummary?.let {
                    if (type == TransactionType.CC_PAYMENT) {
                        "💳 ${it.card.name} (Due: ${formatCurrency(it.openInvoice, state.isBalanceHidden)})"
                    } else {
                        "💳 ${it.card.name} (Avail: ${formatCurrency(it.availableLimit, state.isBalanceHidden)})"
                    }
                } ?: "💳 Select Credit Card"

                ExposedDropdownMenuBox(
                    expanded = cardMenuExpanded,
                    onExpandedChange = { cardMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = cardDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(if (type == TransactionType.CC_PAYMENT) "To Credit Card" else "Credit Card")
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardMenuExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("sheet_card_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = cardMenuExpanded,
                        onDismissRequest = { cardMenuExpanded = false }
                    ) {
                        state.cardSummaries.forEach { summary ->
                            DropdownMenuItem(
                                text = {
                                    Text("💳 ${summary.card.name} • Due ${formatCurrency(summary.openInvoice, state.isBalanceHidden)}")
                                },
                                onClick = {
                                    selectedCardId = summary.card.id
                                    cardMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (type == TransactionType.TRANSFER) {
                val toLabel = state.accounts.find { it.id == toAccountId }?.name ?: "Select Destination"
                ExposedDropdownMenuBox(
                    expanded = toAccountMenuExpanded,
                    onExpandedChange = { toAccountMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "Account: $toLabel",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("sheet_to_account_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = toAccountMenuExpanded,
                        onDismissRequest = { toAccountMenuExpanded = false }
                    ) {
                        state.accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("Account: ${acc.name}") },
                                onClick = {
                                    toAccountId = acc.id
                                    toAccountMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated, e.g. upi, work)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_tags_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sheet_notes_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurring monthly", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    modifier = Modifier.testTag("sheet_recurring_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (editingTx != null) {
                    OutlinedButton(
                        onClick = { onDelete(editingTx) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = semanticColors.expense),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sheet_delete_button")
                    ) {
                        Text("Delete")
                    }
                }
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            amountError = true
                            return@Button
                        }
                        val splitEncoded = if (
                            isSplitEnabled &&
                            (type == TransactionType.EXPENSE || type == TransactionType.CC_EXPENSE) &&
                            selectedCategoryId != null &&
                            splitSecondaryCategoryId != null
                        ) {
                            val firstAmt = (splitPrimaryAmountText.toDoubleOrNull() ?: (amt / 2.0)).coerceIn(0.01, amt)
                            val secondAmt = (amt - firstAmt).coerceAtLeast(0.0)
                            if (secondAmt > 0.0) {
                                SplitAllocation.encode(
                                    listOf(
                                        SplitAllocation(selectedCategoryId!!, firstAmt),
                                        SplitAllocation(splitSecondaryCategoryId!!, secondAmt)
                                    )
                                )
                            } else ""
                        } else ""

                        val defaultTitle = when (type) {
                            TransactionType.CC_EXPENSE -> "Card Purchase"
                            TransactionType.CC_PAYMENT -> "Card Bill Payment"
                            else -> type.label
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(
                            TransactionEntity(
                                id = editingTx?.id ?: 0L,
                                title = title.ifBlank { defaultTitle },
                                amount = amt,
                                type = type,
                                dateMillis = dateMillis,
                                isPaid = isPaid,
                                categoryId = if (supportsCategories) selectedCategoryId else null,
                                accountId = if (needsSourceAccount) selectedAccountId else null,
                                creditCardId = if (type == TransactionType.CC_EXPENSE || type == TransactionType.CC_PAYMENT) selectedCardId else null,
                                toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                                notes = notes,
                                isRecurring = isRecurring,
                                tags = tags,
                                splitSummary = splitEncoded
                            ),
                            learnMerchantRule
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (type) {
                            TransactionType.INCOME, TransactionType.INVESTMENT_SELL, TransactionType.INVESTMENT_INCOME, TransactionType.INTEREST_CREDIT -> semanticColors.income
                            TransactionType.CC_PAYMENT -> semanticColors.cardPayment
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("sheet_save_button")
                ) {
                    Text(if (type == TransactionType.CC_PAYMENT) "Pay Card Bill" else "Save")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
