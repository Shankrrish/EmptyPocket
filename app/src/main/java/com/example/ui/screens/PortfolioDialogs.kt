package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountEntity
import com.example.data.model.EpfAccountEntity
import com.example.data.model.EpfEntryEntity
import com.example.data.model.EpfEntryType
import com.example.data.model.MutualFundCategory
import com.example.data.model.MutualFundEntity
import com.example.data.model.MutualFundOption
import com.example.data.model.MutualFundPlan
import com.example.data.model.MutualFundTxEntity
import com.example.data.model.MutualFundTxType
import com.example.data.model.NetWorthItemEntity
import com.example.data.model.OtherAssetEntity
import com.example.data.model.OtherAssetKind
import com.example.data.model.PpfAccountEntity
import com.example.data.model.PpfFyRateEntity
import com.example.data.model.PpfTxEntity
import com.example.data.model.PpfTxType
import com.example.data.model.StockEntity
import com.example.data.model.StockExchange
import com.example.data.model.StockTradeEntity
import com.example.data.model.StockTradeType
import com.example.ui.PortfolioAnalytics
import com.example.ui.PortfolioSummary
import com.example.ui.theme.FinFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkPriceUpdateSheet(
    summary: PortfolioSummary,
    onDismiss: () -> Unit,
    onSaveAll: (Map<Long, Double>, Map<Long, Double>, Double?, Double?, Double?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mfNavs = remember(summary.mfHoldings) {
        summary.mfHoldings.associate { it.fund.id to mutableStateOf(it.fund.currentNav.toString()) }
    }
    val stockPrices = remember(summary.stockHoldings) {
        summary.stockHoldings.associate { it.stock.id to mutableStateOf(it.stock.currentPrice.toString()) }
    }
    val existingGold = summary.otherAssetSummaries.filter { it.asset.kind == OtherAssetKind.GOLD }
    var gold24 by remember { mutableStateOf(existingGold.find { it.asset.goldPurityKarat == 24 }?.asset?.goldRatePerGram?.toString() ?: "") }
    var gold22 by remember { mutableStateOf(existingGold.find { it.asset.goldPurityKarat == 22 }?.asset?.goldRatePerGram?.toString() ?: "") }
    var gold18 by remember { mutableStateOf(existingGold.find { it.asset.goldPurityKarat == 18 }?.asset?.goldRatePerGram?.toString() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Offline Valuation Update", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Update latest NAVs, Stock CMPs, and Gold rates manually offline. Timestamps update automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = FinFlowTheme.colors.textSecondary
            )

            if (summary.mfHoldings.isNotEmpty()) {
                Text("Mutual Fund NAVs (₹)", style = MaterialTheme.typography.titleSmall)
                summary.mfHoldings.forEach { h ->
                    val state = mfNavs[h.fund.id] ?: return@forEach
                    OutlinedTextField(
                        value = state.value,
                        onValueChange = { state.value = it },
                        label = { Text(h.fund.schemeName) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (summary.stockHoldings.isNotEmpty()) {
                Text("Stock CMPs (₹)", style = MaterialTheme.typography.titleSmall)
                summary.stockHoldings.forEach { h ->
                    val state = stockPrices[h.stock.id] ?: return@forEach
                    OutlinedTextField(
                        value = state.value,
                        onValueChange = { state.value = it },
                        label = { Text("${h.stock.symbol} (${h.stock.exchange.label})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (existingGold.isNotEmpty()) {
                Text("Gold Rate per Gram (₹/g)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = gold24, onValueChange = { gold24 = it }, label = { Text("24K") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = gold22, onValueChange = { gold22 = it }, label = { Text("22K") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = gold18, onValueChange = { gold18 = it }, label = { Text("18K") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
            }

            Button(
                onClick = {
                    val mfs = mfNavs.mapNotNull { (id, st) -> st.value.toDoubleOrNull()?.takeIf { it > 0 }?.let { id to it } }.toMap()
                    val sts = stockPrices.mapNotNull { (id, st) -> st.value.toDoubleOrNull()?.takeIf { it > 0 }?.let { id to it } }.toMap()
                    onSaveAll(mfs, sts, gold24.toDoubleOrNull(), gold22.toDoubleOrNull(), gold18.toDoubleOrNull())
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("bulk_price_save_button")
            ) { Text("Save Valuations") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMutualFundSheet(onDismiss: () -> Unit, onSave: (MutualFundEntity) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amc by remember { mutableStateOf("") }
    var scheme by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MutualFundCategory.EQUITY) }
    var plan by remember { mutableStateOf(MutualFundPlan.DIRECT) }
    var option by remember { mutableStateOf(MutualFundOption.GROWTH) }
    var folio by remember { mutableStateOf("") }
    var navText by remember { mutableStateOf("") }
    var sipEnabled by remember { mutableStateOf(false) }
    var sipAmountText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Add Mutual Fund Scheme", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = amc, onValueChange = { amc = it }, label = { Text("AMC Name (e.g. Parag Parikh / HDFC)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("mf_amc_input"))
            OutlinedTextField(value = scheme, onValueChange = { scheme = it }, label = { Text("Scheme Name") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("mf_scheme_input"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MutualFundCategory.entries.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MutualFundPlan.entries.forEach { p ->
                    FilterChip(selected = plan == p, onClick = { plan = p }, label = { Text(p.label) })
                }
                MutualFundOption.entries.forEach { o ->
                    FilterChip(selected = option == o, onClick = { option = o }, label = { Text(o.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = folio, onValueChange = { folio = it }, label = { Text("Folio (opt.)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = navText, onValueChange = { navText = it }, label = { Text("Current NAV (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Active Monthly SIP", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = sipEnabled, onCheckedChange = { sipEnabled = it })
            }
            if (sipEnabled) {
                OutlinedTextField(value = sipAmountText, onValueChange = { sipAmountText = it }, label = { Text("SIP Amount (₹/mo)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Button(
                onClick = {
                    if (amc.isNotBlank() && scheme.isNotBlank()) {
                        onSave(
                            MutualFundEntity(
                                amcName = amc.trim(),
                                schemeName = scheme.trim(),
                                category = category,
                                plan = plan,
                                option = option,
                                folioNumber = folio.trim(),
                                currentNav = navText.toDoubleOrNull() ?: 0.0,
                                sipEnabled = sipEnabled,
                                sipAmount = sipAmountText.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("mf_save_button")
            ) { Text("Save Scheme") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordMfTxSheet(
    fund: MutualFundEntity,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (MutualFundTxEntity, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var txType by remember { mutableStateOf(MutualFundTxType.SIP) }
    var amountText by remember { mutableStateOf("") }
    var navText by remember { mutableStateOf(if (fund.currentNav > 0) fund.currentNav.toString() else "") }
    var syncBank by remember { mutableStateOf(true) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Log MF Transaction", style = MaterialTheme.typography.titleLarge)
            Text(fund.schemeName, style = MaterialTheme.typography.labelMedium, color = FinFlowTheme.colors.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MutualFundTxType.entries.forEach { t ->
                    FilterChip(selected = txType == t, onClick = { txType = t }, label = { Text(t.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = navText, onValueChange = { navText = it }, label = { Text("Tx NAV (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
            if (accounts.isNotEmpty() && txType != MutualFundTxType.SWITCH_IN && txType != MutualFundTxType.SWITCH_OUT) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sync Bank Ledger (${accounts.find { it.id == selectedAccountId }?.name ?: "Bank"})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = syncBank, onCheckedChange = { syncBank = it })
                }
            }
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    val nav = navText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0 && nav > 0.0) {
                        onSave(
                            MutualFundTxEntity(
                                fundId = fund.id,
                                txType = txType,
                                units = amt / nav,
                                nav = nav,
                                amount = amt,
                                dateMillis = System.currentTimeMillis(),
                                linkedBankAccountId = if (syncBank) selectedAccountId else null
                            ),
                            syncBank
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save MF Entry") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockSheet(onDismiss: () -> Unit, onSave: (StockEntity) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var exchange by remember { mutableStateOf(StockExchange.NSE) }
    var symbol by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var cmpText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Add Stock Holding", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StockExchange.entries.forEach { ex ->
                    FilterChip(selected = exchange == ex, onClick = { exchange = ex }, label = { Text(ex.label) })
                }
            }
            OutlinedTextField(value = symbol, onValueChange = { symbol = it.uppercase() }, label = { Text("Symbol (e.g. TCS, RELIANCE)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cmpText, onValueChange = { cmpText = it }, label = { Text("Current Market Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (symbol.isNotBlank()) {
                        onSave(
                            StockEntity(
                                exchange = exchange,
                                symbol = symbol.trim(),
                                companyName = company.ifBlank { symbol.trim() },
                                currentPrice = cmpText.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Stock") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordStockTradeSheet(
    stock: StockEntity,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (StockTradeEntity, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tradeType by remember { mutableStateOf(StockTradeType.BUY) }
    var qtyText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf(if (stock.currentPrice > 0) stock.currentPrice.toString() else "") }
    var chargesText by remember { mutableStateOf("0") }
    var syncBank by remember { mutableStateOf(true) }
    val bankId = accounts.firstOrNull()?.id

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Log Trade • ${stock.symbol}", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StockTradeType.entries.forEach { t ->
                    FilterChip(selected = tradeType == t, onClick = { tradeType = t }, label = { Text(t.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = chargesText, onValueChange = { chargesText = it }, label = { Text("Brokerage") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
            if (bankId != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Update Bank Account Balance", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = syncBank, onCheckedChange = { syncBank = it })
                }
            }
            Button(
                onClick = {
                    val qty = qtyText.toDoubleOrNull() ?: 0.0
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    if (qty > 0.0 && price > 0.0) {
                        onSave(
                            StockTradeEntity(
                                stockId = stock.id,
                                tradeType = tradeType,
                                quantity = qty,
                                price = price,
                                charges = chargesText.toDoubleOrNull() ?: 0.0,
                                dateMillis = System.currentTimeMillis(),
                                linkedBankAccountId = if (syncBank) bankId else null
                            ),
                            syncBank
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Trade") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPpfAccountSheet(onDismiss: () -> Unit, onSave: (PpfAccountEntity) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("My PPF Account") }
    var bank by remember { mutableStateOf("SBI") }
    var startFy by remember { mutableStateOf(PortfolioAnalytics.financialYearStart(System.currentTimeMillis()).toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add PPF Account", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bank, onValueChange = { bank = it }, label = { Text("Bank / Post Office") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = startFy, onValueChange = { startFy = it }, label = { Text("Opening Financial Year (e.g. 2021)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(PpfAccountEntity(accountName = name.trim(), bankOrPostOffice = bank.trim(), startFinancialYear = startFy.toIntOrNull() ?: 2021))
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save PPF Account") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PpfTxSheet(
    account: PpfAccountEntity,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (PpfTxEntity, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var txType by remember { mutableStateOf(PpfTxType.DEPOSIT) }
    var amountText by remember { mutableStateOf("") }
    var syncBank by remember { mutableStateOf(true) }
    val bankId = accounts.firstOrNull()?.id

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("PPF Entry • ${account.accountName}", style = MaterialTheme.typography.titleLarge)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "Indian PPF Rule: Deposits on or before the 5th earn interest for that month; deposits on day 6+ start earning from the next month.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PpfTxType.entries.forEach { t ->
                    FilterChip(selected = txType == t, onClick = { txType = t }, label = { Text(t.label) })
                }
            }
            OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            if (bankId != null && txType != PpfTxType.INTEREST_CREDIT) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Deduct / Credit Bank Ledger", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = syncBank, onCheckedChange = { syncBank = it })
                }
            }
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onSave(
                            PpfTxEntity(
                                ppfAccountId = account.id,
                                txType = txType,
                                amount = amt,
                                dateMillis = System.currentTimeMillis(),
                                linkedBankAccountId = if (syncBank) bankId else null
                            ),
                            syncBank
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save PPF Entry") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PpfRatesSheet(
    configuredRates: List<PpfFyRateEntity>,
    onSaveRate: (Int, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val merged = remember(configuredRates) {
        val m = PortfolioAnalytics.defaultHistoricalPpfRates.toMutableMap()
        configuredRates.forEach { m[it.financialYearStart] = it.annualRatePercent }
        m.entries.sortedByDescending { it.key }
    }
    var fyInput by remember { mutableStateOf(PortfolioAnalytics.financialYearStart(System.currentTimeMillis()).toString()) }
    var rateInput by remember { mutableStateOf("7.1") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("PPF Interest Rates by Financial Year", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = fyInput, onValueChange = { fyInput = it }, label = { Text("FY Start") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = rateInput, onValueChange = { rateInput = it }, label = { Text("Rate %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        val fy = fyInput.toIntOrNull()
                        val rate = rateInput.toDoubleOrNull()
                        if (fy != null && rate != null && rate > 0) onSaveRate(fy, rate)
                    }
                ) { Text("Set") }
            }
            merged.take(10).forEach { (fy, rate) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(PortfolioAnalytics.formatFyLabel(fy), style = MaterialTheme.typography.bodyMedium)
                    Text("$rate% p.a.", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEpfAccountSheet(onDismiss: () -> Unit, onSave: (EpfAccountEntity) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var employer by remember { mutableStateOf("") }
    var uan by remember { mutableStateOf("") }
    var empShare by remember { mutableStateOf("0") }
    var erShare by remember { mutableStateOf("0") }
    var epsShare by remember { mutableStateOf("0") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add EPF Account", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = employer, onValueChange = { employer = it }, label = { Text("Employer Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = uan, onValueChange = { uan = it }, label = { Text("Masked UAN (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = empShare, onValueChange = { empShare = it }, label = { Text("Employee (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = erShare, onValueChange = { erShare = it }, label = { Text("Employer (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = epsShare, onValueChange = { epsShare = it }, label = { Text("EPS (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    if (employer.isNotBlank()) {
                        onSave(
                            EpfAccountEntity(
                                employerName = employer.trim(),
                                uanMasked = uan.trim(),
                                openingEmployeeShare = empShare.toDoubleOrNull() ?: 0.0,
                                openingEmployerShare = erShare.toDoubleOrNull() ?: 0.0,
                                openingPensionShare = epsShare.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save EPF Account") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpfEntrySheet(account: EpfAccountEntity, onDismiss: () -> Unit, onSave: (EpfEntryEntity) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var entryType by remember { mutableStateOf(EpfEntryType.CONTRIBUTION) }
    var empAmt by remember { mutableStateOf("") }
    var erAmt by remember { mutableStateOf("") }
    var epsAmt by remember { mutableStateOf("") }
    var vpfAmt by remember { mutableStateOf("0") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Log EPF Entry • ${account.employerName}", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EpfEntryType.entries.forEach { t ->
                    FilterChip(selected = entryType == t, onClick = { entryType = t }, label = { Text(t.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = empAmt, onValueChange = { empAmt = it }, label = { Text("Employee (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = erAmt, onValueChange = { erAmt = it }, label = { Text("Employer EPF") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = epsAmt, onValueChange = { epsAmt = it }, label = { Text("EPS Pension") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = vpfAmt, onValueChange = { vpfAmt = it }, label = { Text("VPF (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    val emp = empAmt.toDoubleOrNull() ?: 0.0
                    val er = erAmt.toDoubleOrNull() ?: 0.0
                    val eps = epsAmt.toDoubleOrNull() ?: 0.0
                    val vpf = vpfAmt.toDoubleOrNull() ?: 0.0
                    if (emp + er + eps + vpf > 0.0) {
                        onSave(
                            EpfEntryEntity(
                                epfAccountId = account.id,
                                entryType = entryType,
                                employeeAmount = emp,
                                employerEpfAmount = er,
                                epsPensionAmount = eps,
                                vpfAmount = vpf,
                                dateMillis = System.currentTimeMillis()
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save EPF Entry") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddOtherAssetSheet(
    liabilities: List<NetWorthItemEntity>,
    onDismiss: () -> Unit,
    onSave: (OtherAssetEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember { mutableStateOf(OtherAssetKind.NPS) }
    var customCategory by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var investedText by remember { mutableStateOf("") }
    var valueText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var karat by remember { mutableStateOf(22) }
    var gramsText by remember { mutableStateOf("") }
    var ratePerGramText by remember { mutableStateOf("") }
    var linkedLoanId by remember { mutableStateOf<Long?>(null) }
    var loanExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = FinFlowTheme.colors.surfaceElevated) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Add Other Asset", style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OtherAssetKind.entries.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text("${k.emoji} ${k.label}") })
                }
            }
            if (kind == OtherAssetKind.CUSTOM) {
                OutlinedTextField(value = customCategory, onValueChange = { customCategory = it }, label = { Text("Custom Category Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Asset Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            if (kind == OtherAssetKind.GOLD) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 22, 18).forEach { k ->
                        FilterChip(selected = karat == k, onClick = { karat = k }, label = { Text("${k}K") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = gramsText, onValueChange = { gramsText = it }, label = { Text("Grams") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ratePerGramText, onValueChange = { ratePerGramText = it }, label = { Text("Rate / g (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = investedText, onValueChange = { investedText = it }, label = { Text("Invested (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = valueText, onValueChange = { valueText = it }, label = { Text("Current Value (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
            }

            if (kind == OtherAssetKind.FD || kind == OtherAssetKind.RD || kind == OtherAssetKind.NPS) {
                OutlinedTextField(value = rateText, onValueChange = { rateText = it }, label = { Text("Interest / Return % p.a. (opt.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            if (kind == OtherAssetKind.PROPERTY && liabilities.isNotEmpty()) {
                val chosen = liabilities.find { it.id == linkedLoanId }?.name ?: "None"
                ExposedDropdownMenuBox(expanded = loanExpanded, onExpandedChange = { loanExpanded = it }) {
                    OutlinedTextField(
                        value = chosen,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link Home Loan (opt.)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = loanExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = loanExpanded, onDismissRequest = { loanExpanded = false }) {
                        DropdownMenuItem(text = { Text("None") }, onClick = { linkedLoanId = null; loanExpanded = false })
                        liabilities.forEach { liab ->
                            DropdownMenuItem(text = { Text(liab.name) }, onClick = { linkedLoanId = liab.id; loanExpanded = false })
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val grams = gramsText.toDoubleOrNull() ?: 0.0
                    val rpg = ratePerGramText.toDoubleOrNull() ?: 0.0
                    val computedVal = if (kind == OtherAssetKind.GOLD) grams * rpg else (valueText.toDoubleOrNull() ?: 0.0)
                    val inv = investedText.toDoubleOrNull() ?: computedVal
                    if (name.isNotBlank() && computedVal > 0.0) {
                        onSave(
                            OtherAssetEntity(
                                kind = kind,
                                customCategoryName = customCategory.trim(),
                                name = name.trim(),
                                investedAmount = inv,
                                currentValue = computedVal,
                                interestRatePercent = rateText.toDoubleOrNull() ?: 0.0,
                                goldPurityKarat = karat,
                                goldGrams = grams,
                                goldRatePerGram = rpg,
                                linkedLoanId = if (kind == OtherAssetKind.PROPERTY) linkedLoanId else null
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Asset") }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
