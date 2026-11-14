package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinanceUiState
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.theme.FinFlowTheme
import com.example.util.SecurityUtils

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    state: FinanceUiState,
    onBack: () -> Unit,
    onSetPinAndEnable: (String, Boolean) -> Unit,
    onToggleBiometrics: (Boolean) -> Unit,
    onDisableLock: () -> Unit,
    onLockNow: () -> Unit,
    onExportBackupJson: ((String) -> Unit) -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    onRestoreBackupJson: (String, (Boolean, String) -> Unit) -> Unit,
    onDeleteAllData: () -> Unit,
    onResetDemoData: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val semanticColors = FinFlowTheme.colors
    val canBiometrics = remember(context) { SecurityUtils.canUseBiometrics(context) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showResetDemoDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var pendingJsonPayload by remember { mutableStateOf<String?>(null) }
    var pendingCsvPayload by remember { mutableStateOf<String?>(null) }

    val jsonSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingJsonPayload
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                statusMessage = "Full JSON backup saved."
            }.onFailure {
                statusMessage = "Failed to save backup file."
            }
        }
        pendingJsonPayload = null
    }

    val csvSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val payload = pendingCsvPayload
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                statusMessage = "Transactions CSV exported."
            }.onFailure {
                statusMessage = "Failed to export CSV file."
            }
        }
        pendingCsvPayload = null
    }

    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (!content.isNullOrBlank()) {
                onRestoreBackupJson(content) { _, msg -> statusMessage = msg }
            } else {
                statusMessage = "Selected file was empty or unreadable."
            }
        }
    }

    if (showPinSetupDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text(if (state.settings.appLockEnabled) "Change 4-Digit PIN" else "Set 4-Digit PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                        label = { Text("Enter 4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("setup_pin_input")
                    )
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirmInput = it },
                        label = { Text("Confirm 4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("setup_pin_confirm_input")
                    )
                    pinError?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = semanticColors.expense)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            pinInput.length != 4 -> pinError = "PIN must be 4 digits."
                            pinInput != confirmInput -> pinError = "PINs do not match."
                            else -> {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSetPinAndEnable(pinInput, state.settings.biometricEnabled || canBiometrics)
                                statusMessage = "4-digit PIN lock enabled."
                                showPinSetupDialog = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_pin_button")
                ) { Text("Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete All Financial Data?") },
            text = {
                Text("This permanently erases all accounts, credit cards, transactions, budgets, goals, bills, rules, and net worth items, leaving a blank slate.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteAllData()
                        statusMessage = "All financial records deleted."
                        showDeleteAllDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = semanticColors.expense),
                    modifier = Modifier.testTag("confirm_delete_all_data_button")
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetDemoDialog) {
        AlertDialog(
            onDismissRequest = { showResetDemoDialog = false },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Reset Demo Data?") },
            text = { Text("Restore sample Indian Rupee accounts, cards, budgets, goals, and transactions.") },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResetDemoData()
                        statusMessage = "Sample demo data restored."
                        showResetDemoDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_reset_demo_privacy_button")
                ) { Text("Restore Demo") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDemoDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy, Lock & Backups") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("privacy_back_button")) {
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
                .testTag("privacy_settings_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            statusMessage?.let { msg ->
                item {
                    FinSurfaceCard(
                        shape = RoundedCornerShape(16.dp),
                        containerColor = semanticColors.surfaceElevated,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
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

            // App Lock & Biometrics
            item {
                FinSurfaceCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(
                                emoji = "🔐",
                                tintColor = MaterialTheme.colorScheme.primary,
                                size = 36.dp,
                                emojiSize = 17.sp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("App Lock & Biometrics", style = MaterialTheme.typography.titleMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Require PIN on Launch", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = if (state.settings.appLockEnabled) "Enabled (4-digit salted SHA-256 PIN)" else "Off by default — enable anytime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semanticColors.textSecondary
                                )
                            }
                            Switch(
                                checked = state.settings.appLockEnabled,
                                onCheckedChange = { wantEnable ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (wantEnable) {
                                        showPinSetupDialog = true
                                    } else {
                                        onDisableLock()
                                        statusMessage = "App lock disabled."
                                    }
                                },
                                modifier = Modifier.testTag("app_lock_switch")
                            )
                        }

                        if (state.settings.appLockEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Biometric Unlock", style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = if (canBiometrics) "Fingerprint / face unlock ready" else "No device biometrics enrolled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = semanticColors.textSecondary
                                        )
                                    }
                                }
                                Switch(
                                    checked = state.settings.biometricEnabled,
                                    enabled = canBiometrics,
                                    onCheckedChange = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleBiometrics(it)
                                    },
                                    modifier = Modifier.testTag("biometric_lock_switch")
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showPinSetupDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Change PIN")
                                }
                                OutlinedButton(
                                    onClick = onLockNow,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("lock_now_button")
                                ) {
                                    Text("Lock Now")
                                }
                            }
                        }
                    }
                }
            }

            // Standard JSON & CSV Exports
            item {
                FinSurfaceCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(
                                emoji = "📦",
                                tintColor = MaterialTheme.colorScheme.secondary,
                                size = 36.dp,
                                emojiSize = 17.sp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Backups & Data Export", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Export standard JSON backups or CSV ledgers using Android's system document picker without extra storage permissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = semanticColors.textSecondary
                        )

                        OutlinedButton(
                            onClick = {
                                onExportBackupJson { json ->
                                    pendingJsonPayload = json
                                    jsonSaveLauncher.launch("moneytracker_backup.json")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_json_backup_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Full Backup (JSON)")
                        }

                        OutlinedButton(
                            onClick = { jsonRestoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_json_backup_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Backup (JSON)")
                        }

                        OutlinedButton(
                            onClick = {
                                onExportCsv { csv ->
                                    pendingCsvPayload = csv
                                    csvSaveLauncher.launch("moneytracker_transactions.csv")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Transactions (CSV)")
                        }
                    }
                }
            }

            // Delete & Reset Controls
            item {
                FinSurfaceCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(
                                emoji = "🛡️",
                                tintColor = semanticColors.expense,
                                size = 36.dp,
                                emojiSize = 17.sp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Data Control", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = { showResetDemoDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("privacy_reset_demo_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Sample Demo Data")
                        }
                        OutlinedButton(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = semanticColors.expense),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_all_data_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete All Financial Data")
                        }
                    }
                }
            }
        }
    }
}
