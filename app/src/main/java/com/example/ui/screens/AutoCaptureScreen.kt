package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.model.CategorizationRuleEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.ui.FinanceUiState
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.components.resolveCategoryEmoji
import com.example.ui.theme.FinFlowTheme
import com.example.util.IndiaCaptureParser
import com.example.util.ParsedCaptureCandidate
import com.example.util.formatCurrency

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCaptureScreen(
    state: FinanceUiState,
    onBack: () -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onImportCandidates: (List<ParsedCaptureCandidate>) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current

    var pasteInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var candidates by remember { mutableStateOf<List<ParsedCaptureCandidate>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val updateCandidates: (List<ParsedCaptureCandidate>) -> Unit = { parsed ->
        candidates = parsed
        selectedIds = parsed.filter { !it.isDuplicate }.map { it.id }.toSet()
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    val runSmsScan: () -> Unit = {
        if (!isScanning) {
            isScanning = true
            statusMessage = "Scanning SMS inbox locally..."
            scope.launch {
                val result = try {
                    withContext(Dispatchers.IO) {
                        scanDeviceBankSms(
                            context = context,
                            rules = state.rules,
                            categories = state.categories,
                            existingTransactions = state.allTransactions
                        )
                    }
                } catch (t: Throwable) {
                    SmsScanResult(emptyList(), 0, t.message ?: t.javaClass.simpleName)
                }
                updateCandidates(result.candidates)
                statusMessage = smsScanMessage(result)
                isScanning = false
            }
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runSmsScan()
        } else {
            statusMessage = "SMS permission declined. Use Paste SMS / UPI Receipt or CSV Import below (zero permissions required)."
        }
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text.isNullOrBlank()) {
                statusMessage = "Unable to read selected file."
            } else {
                val parsed = IndiaCaptureParser.parseCsvStatement(
                    csvContent = text,
                    rules = state.rules,
                    categories = state.categories,
                    existingTransactions = state.allTransactions
                )
                updateCandidates(parsed)
                statusMessage = if (parsed.isEmpty()) {
                    "No valid rows found. Ensure CSV has Date, Description, Amount columns."
                } else {
                    "Parsed ${parsed.size} row(s) from statement CSV."
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("India Auto-Capture") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("capture_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("auto_capture_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Privacy & Explicit Consent Card
            item {
                FinSurfaceCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(
                                emoji = "🔒",
                                tintColor = MaterialTheme.colorScheme.primary,
                                size = 36.dp,
                                emojiSize = 17.sp,
                                shape = RoundedCornerShape(10.dp),
                                contentDescription = "Privacy"
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("On-Device Privacy & Consent", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "All bank SMS, UPI, email receipt, and CSV parsing runs 100% locally on your device. FinFlow never uploads messages or requests banking passwords.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = semanticColors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Allow local Bank SMS parsing", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = state.settings.smsAutoScanConsent,
                                onCheckedChange = onConsentChange,
                                modifier = Modifier.testTag("sms_consent_switch")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_SMS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        runSmsScan()
                                    } else {
                                        smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                    }
                                },
                                enabled = state.settings.smsAutoScanConsent,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scan_sms_button")
                            ) {
                                Text("📩 Scan SMS")
                            }
                            OutlinedButton(
                                onClick = {
                                    csvPickerLauncher.launch(arrayOf("text/*", "text/csv", "application/csv"))
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_csv_button")
                            ) {
                                Text("📄 Import CSV")
                            }
                        }
                    }
                }
            }

            // Paste SMS / UPI / Email Receipt Parser (Zero-Permission)
            item {
                FinSurfaceCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(
                                emoji = "📲",
                                tintColor = MaterialTheme.colorScheme.secondary,
                                size = 36.dp,
                                emojiSize = 17.sp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Paste Bank SMS, UPI Alert or Receipt", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedTextField(
                            value = pasteInput,
                            onValueChange = { pasteInput = it },
                            placeholder = {
                                Text("e.g. Rs. 485.00 debited from HDFC A/c XX4821 via UPI to Swiggy on 13-11-26")
                            },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paste_capture_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val parsed = IndiaCaptureParser.parseTextMessages(
                                        rawInput = pasteInput,
                                        rules = state.rules,
                                        categories = state.categories,
                                        existingTransactions = state.allTransactions
                                    )
                                    updateCandidates(parsed)
                                    statusMessage = if (parsed.isEmpty()) {
                                        "Could not detect an INR amount. Include Rs./INR/₹ and merchant details."
                                    } else {
                                        "Detected ${parsed.size} transaction(s) with smart bank & merchant matching."
                                    }
                                },
                                enabled = pasteInput.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("parse_text_button")
                            ) {
                                Text("Parse Text")
                            }
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = semanticColors.surfaceElevated,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (candidates.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Detected Transactions (${candidates.size})", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = {
                                val chosen = candidates.filter { it.id in selectedIds }
                                if (chosen.isNotEmpty()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onImportCandidates(chosen)
                                    statusMessage = "Imported ${chosen.size} transaction(s) into FinFlow."
                                    candidates = emptyList()
                                    selectedIds = emptySet()
                                    pasteInput = ""
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("import_selected_button")
                        ) {
                            Text("Import (${selectedIds.size})")
                        }
                    }
                }

                items(candidates, key = { it.id }) { item ->
                    val checked = item.id in selectedIds
                    val isIncome = item.type == com.example.data.model.TransactionType.INCOME
                    val tint = if (isIncome) semanticColors.income else MaterialTheme.colorScheme.primary
                    val emoji = resolveCategoryEmoji("", item.suggestedCategoryName)
                    FinSurfaceCard(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (checked) selectedIds - item.id else selectedIds + item.id
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedIds = if (isChecked) selectedIds + item.id else selectedIds - item.id
                                }
                            )
                            EmojiBadge(
                                emoji = emoji,
                                tintColor = tint,
                                size = 38.dp,
                                emojiSize = 18.sp,
                                shape = RoundedCornerShape(11.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${item.detectedBankOrRail} • ${item.suggestedCategoryName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = semanticColors.textSecondary
                                )
                                if (item.isDuplicate) {
                                    Text(
                                        text = "⚠️ Duplicate detected — unchecked by default",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semanticColors.warning
                                    )
                                }
                            }
                            Text(
                                text = formatCurrency(item.amount, state.isBalanceHidden),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) semanticColors.income else semanticColors.expense
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SmsScanResult(
    val candidates: List<ParsedCaptureCandidate>,
    val scannedCount: Int,
    val error: String? = null
)

private fun smsScanMessage(r: SmsScanResult): String = when {
    r.error != null -> "SMS scan failed: ${r.error}. You can still paste SMS text or import a CSV below."
    r.scannedCount == 0 -> "No SMS found in the last 180 days (or the inbox is not readable). Make sure this app is allowed to read SMS."
    r.candidates.isEmpty() -> "Checked ${r.scannedCount} SMS from the last 180 days: no bank/UPI transaction alerts recognised. Try Paste SMS below with a sample alert."
    else -> "Checked ${r.scannedCount} SMS from the last 180 days: found ${r.candidates.size} bank/UPI transaction(s)."
}

private fun scanDeviceBankSms(
    context: Context,
    rules: List<CategorizationRuleEntity>,
    categories: List<CategoryEntity>,
    existingTransactions: List<TransactionEntity>,
    lookbackDays: Int = 180,
    maxMessages: Int = 5000
): SmsScanResult {
    val results = mutableListOf<ParsedCaptureCandidate>()
    var scanned = 0
    val since = System.currentTimeMillis() - lookbackDays * 24L * 60L * 60L * 1000L
    return try {
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext() && scanned < maxMessages) {
                scanned++
                val id = cursor.getString(idCol)
                val body = cursor.getString(bodyCol) ?: continue
                val date = cursor.getLong(dateCol)
                if (IndiaCaptureParser.isLikelyFinancialMessage(body)) {
                    IndiaCaptureParser.parseSingleMessage(
                        id = "sms_$id",
                        msg = body,
                        dateMillis = date,
                        rules = rules,
                        categories = categories,
                        existingTransactions = existingTransactions
                    )?.let { results.add(it) }
                }
            }
        }
        SmsScanResult(results, scanned)
    } catch (e: Exception) {
        SmsScanResult(results, scanned, e.message ?: e.javaClass.simpleName)
    }
}
