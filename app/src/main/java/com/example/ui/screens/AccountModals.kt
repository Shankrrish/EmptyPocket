package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.AccountPresets
import com.example.data.model.AccountType
import com.example.data.model.CreditCardEntity
import com.example.ui.components.CategoryAccentColors
import com.example.ui.theme.FinFlowTheme

private enum class AccountBucket(val label: String) {
    BANK("🏦 Bank"),
    CASH("💵 Cash"),
    WALLET("📱 E-Wallet")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountEditorBottomSheet(
    initial: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bucket by remember {
        mutableStateOf(
            when {
                initial?.type?.isCash() == true -> AccountBucket.CASH
                initial?.type?.isWallet() == true -> AccountBucket.WALLET
                else -> AccountBucket.BANK
            }
        )
    }
    var accountName by remember { mutableStateOf(initial?.name ?: "") }
    var bankName by remember { mutableStateOf(initial?.bankName ?: "") }
    var subType by remember { mutableStateOf(initial?.accountSubType ?: "Savings") }
    var openingBalance by remember { mutableStateOf(initial?.initialBalance?.toString() ?: "0") }
    var maskedNumber by remember { mutableStateOf(initial?.maskedNumber ?: "") }
    var ifsc by remember { mutableStateOf(initial?.ifsc ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🏦") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: 0xFF556B2F) }

    val presets = when (bucket) {
        AccountBucket.BANK -> AccountPresets.indianBanks
        AccountBucket.CASH -> AccountPresets.cashPresets
        AccountBucket.WALLET -> AccountPresets.eWalletPresets
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FinFlowTheme.colors.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (initial == null) "Add Account" else "Edit Account",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountBucket.entries.forEach { b ->
                    FilterChip(
                        selected = bucket == b,
                        onClick = {
                            bucket = b
                            emoji = when (b) {
                                AccountBucket.BANK -> "🏦"
                                AccountBucket.CASH -> "💵"
                                AccountBucket.WALLET -> "📱"
                            }
                            subType = when (b) {
                                AccountBucket.BANK -> "Savings"
                                AccountBucket.CASH -> "Home cash"
                                AccountBucket.WALLET -> "Prepaid wallet"
                            }
                        },
                        label = { Text(b.label) },
                        modifier = Modifier.testTag("acc_bucket_${b.name.lowercase()}")
                    )
                }
            }

            Text("Quick Presets", style = MaterialTheme.typography.labelMedium, color = FinFlowTheme.colors.textSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { p ->
                    val tag = "preset_${bucket.name.lowercase()}_${p.name.lowercase().replace(' ', '_')}"
                    FilterChip(
                        selected = bankName.equals(p.name, ignoreCase = true) || accountName.equals(p.name, ignoreCase = true),
                        onClick = {
                            if (p.name.startsWith("Custom") || p.name.startsWith("Other")) {
                                bankName = ""
                            } else {
                                bankName = p.name
                                if (accountName.isBlank()) accountName = p.name
                            }
                            emoji = p.emoji
                            colorHex = p.colorHex
                            subType = p.defaultSubType
                        },
                        label = { Text("${p.emoji} ${p.name}") },
                        modifier = Modifier.testTag(tag)
                    )
                }
            }

            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Account Label (e.g. HDFC Primary)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("acc_editor_name_input")
            )

            if (bucket == AccountBucket.BANK) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Savings", "Current").forEach { kind ->
                        FilterChip(
                            selected = subType.equals(kind, ignoreCase = true),
                            onClick = { subType = kind },
                            label = { Text(kind) }
                        )
                    }
                }
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name (e.g. ICICI Bank, SBI)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maskedNumber,
                        onValueChange = { maskedNumber = it },
                        label = { Text("Masked A/c No. (opt.)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ifsc,
                        onValueChange = { ifsc = it.uppercase() },
                        label = { Text("IFSC (opt.)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = openingBalance,
                onValueChange = { openingBalance = it },
                label = { Text("Opening Balance (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("acc_editor_balance_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("🏦", "🏛️", "💵", "🏡", "👛", "📱", "⚡", "🪙", "📮").forEach { em ->
                    FilterChip(
                        selected = emoji == em,
                        onClick = { emoji = em },
                        label = { Text(em) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FinFlowTheme.colors.expense),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Delete") }
                }
                Button(
                    onClick = {
                        if (accountName.isBlank()) return@Button
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val accType = when (bucket) {
                            AccountBucket.BANK -> if (subType.equals("Savings", true)) AccountType.SAVINGS else AccountType.CHECKING
                            AccountBucket.CASH -> AccountType.CASH
                            AccountBucket.WALLET -> AccountType.E_WALLET
                        }
                        onSave(
                            AccountEntity(
                                id = initial?.id ?: 0L,
                                name = accountName.trim(),
                                type = accType,
                                initialBalance = openingBalance.toDoubleOrNull() ?: 0.0,
                                colorHex = colorHex.takeIf { it != 0L } ?: CategoryAccentColors.first(),
                                bankName = bankName.trim(),
                                accountSubType = subType.trim(),
                                maskedNumber = maskedNumber.trim(),
                                ifsc = ifsc.trim(),
                                emoji = emoji.trim(),
                                isArchived = initial?.isArchived ?: false,
                                isPrepaidWallet = accType == AccountType.E_WALLET
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("acc_editor_save_button")
                ) { Text("Save Account") }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardEditorBottomSheet(
    initial: CreditCardEntity?,
    onDismiss: () -> Unit,
    onSave: (CreditCardEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var issuerBank by remember { mutableStateOf(initial?.issuerBank ?: "") }
    var brand by remember { mutableStateOf(initial?.brand ?: "Visa") }
    var limit by remember { mutableStateOf(initial?.limitAmount?.toInt()?.toString() ?: "100000") }
    var closingDay by remember { mutableStateOf(initial?.closingDay?.toString() ?: "20") }
    var statementDay by remember { mutableStateOf(initial?.statementDay?.toString() ?: "20") }
    var dueDay by remember { mutableStateOf(initial?.dueDay?.toString() ?: "5") }
    var minDue by remember { mutableStateOf(if ((initial?.minimumDueAmount ?: 0.0) > 0) initial?.minimumDueAmount?.toInt()?.toString() ?: "" else "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FinFlowTheme.colors.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initial == null) "Add Credit Card" else "Edit Credit Card",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Card Name (e.g. HDFC Millennia)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cc_editor_name_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = issuerBank,
                    onValueChange = { issuerBank = it },
                    label = { Text("Issuer Bank") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Network (Visa / MC / RuPay)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Credit Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = minDue,
                    onValueChange = { minDue = it },
                    label = { Text("Min Due Override (opt.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = closingDay,
                    onValueChange = { closingDay = it },
                    label = { Text("Cycle Close") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = statementDay,
                    onValueChange = { statementDay = it },
                    label = { Text("Stmt Day") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { dueDay = it },
                    label = { Text("Due Day") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FinFlowTheme.colors.expense),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Delete") }
                }
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val cDay = (closingDay.toIntOrNull() ?: 20).coerceIn(1, 28)
                        val sDay = (statementDay.toIntOrNull() ?: cDay).coerceIn(1, 28)
                        val dDay = (dueDay.toIntOrNull() ?: 5).coerceIn(1, 28)
                        onSave(
                            CreditCardEntity(
                                id = initial?.id ?: 0L,
                                name = name.trim(),
                                brand = brand.ifBlank { "Visa" },
                                limitAmount = limit.toDoubleOrNull() ?: 50000.0,
                                closingDay = cDay,
                                dueDay = dDay,
                                colorHex = initial?.colorHex ?: 0xFF556B2F,
                                issuerBank = issuerBank.trim(),
                                statementDay = sDay,
                                minimumDueAmount = minDue.toDoubleOrNull() ?: 0.0,
                                isArchived = initial?.isArchived ?: false
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cc_editor_save_button")
                ) { Text("Save Card") }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
