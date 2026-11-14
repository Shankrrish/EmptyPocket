package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.FinSurfaceCard
import com.example.ui.theme.FinFlowTheme
import com.example.util.AutoBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutoBackupCard() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val semanticColors = FinFlowTheme.colors
    var enabled by remember { mutableStateOf(AutoBackupManager.isEnabled(ctx)) }
    var lastOk by remember { mutableStateOf(AutoBackupManager.lastSuccessMillis(ctx)) }
    var lastError by remember { mutableStateOf(AutoBackupManager.lastError(ctx)) }
    var folder by remember { mutableStateOf(AutoBackupManager.folderLabel(ctx)) }
    var busy by remember { mutableStateOf(false) }

    fun refresh() {
        enabled = AutoBackupManager.isEnabled(ctx)
        lastOk = AutoBackupManager.lastSuccessMillis(ctx)
        lastError = AutoBackupManager.lastError(ctx)
        folder = AutoBackupManager.folderLabel(ctx)
    }

    fun backupNow() {
        if (busy) return
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) { AutoBackupManager.runBackup(ctx) }
            busy = false
            refresh()
        }
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            AutoBackupManager.enable(ctx, uri)
            refresh()
            backupNow()
        }
    }

    FinSurfaceCard(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Automatic Monthly Backup", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = enabled,
                    onCheckedChange = { on ->
                        if (on) pickFolder.launch(null) else { AutoBackupManager.disable(ctx); refresh() }
                    },
                    modifier = Modifier.testTag("auto_backup_switch")
                )
            }
            Text(
                "Saves a full JSON backup once a month into a folder you choose, keeping the latest 12. " +
                    "Pick a folder outside the app (e.g. Downloads or a synced folder) so it survives uninstall.",
                style = MaterialTheme.typography.bodyMedium,
                color = semanticColors.textSecondary
            )
            if (enabled) {
                Text("Folder: $folder", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Last backup: " + if (lastOk > 0) SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date(lastOk)) else "never",
                    style = MaterialTheme.typography.bodyMedium
                )
                lastError?.let { Text("Last error: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { backupNow() },
                        enabled = !busy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("backup_now_button")
                    ) { Text(if (busy) "Backing up..." else "Back up now") }
                    OutlinedButton(
                        onClick = { pickFolder.launch(null) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Change folder") }
                }
            }
        }
    }
}
