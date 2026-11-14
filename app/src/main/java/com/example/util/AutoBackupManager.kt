package com.example.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.FinanceDatabase
import com.example.data.repository.FinanceRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Writes a monthly full JSON backup into a folder the user picked (Storage Access Framework). */
object AutoBackupManager {
    private const val PREFS = "auto_backup"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TREE_URI = "tree_uri"
    private const val KEY_LAST_OK = "last_success_millis"
    private const val KEY_LAST_ERROR = "last_error"
    private const val WORK_NAME = "monthly_auto_backup"
    private const val FILE_PREFIX = "emptypocket_backup_"
    private const val KEEP = 12

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = prefs(c).getBoolean(KEY_ENABLED, false) && folderUri(c) != null
    fun folderUri(c: Context): Uri? = prefs(c).getString(KEY_TREE_URI, null)?.let(Uri::parse)
    fun lastSuccessMillis(c: Context): Long = prefs(c).getLong(KEY_LAST_OK, 0L)
    fun lastError(c: Context): String? = prefs(c).getString(KEY_LAST_ERROR, null)

    fun folderLabel(c: Context): String =
        folderUri(c)?.let { DocumentFile.fromTreeUri(c, it)?.name } ?: "Not selected"

    fun enable(c: Context, treeUri: Uri) {
        prefs(c).edit().putString(KEY_TREE_URI, treeUri.toString()).putBoolean(KEY_ENABLED, true).apply()
        schedule(c)
    }

    fun disable(c: Context) {
        prefs(c).edit().putBoolean(KEY_ENABLED, false).apply()
        WorkManager.getInstance(c.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    /** Idempotent: safe to call on every app start. */
    fun schedule(c: Context) {
        if (!isEnabled(c)) return
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(30, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .build()
        WorkManager.getInstance(c.applicationContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Runs one backup now. Returns null on success or an error message. Same-month file is replaced. */
    suspend fun runBackup(c: Context): String? {
        val app = c.applicationContext
        val uri = folderUri(app) ?: return fail(app, "No backup folder selected")
        return try {
            val dir = DocumentFile.fromTreeUri(app, uri)
            if (dir == null || !dir.canWrite()) return fail(app, "Backup folder is not writable. Pick the folder again.")

            val db = FinanceDatabase.getInstance(app)
            val repo = FinanceRepository(db.financeDao(), db.portfolioDao())
            val json = BackupManager.encodeBackupJson(repo.createBackupData())

            val name = FILE_PREFIX + SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) + ".json"
            dir.findFile(name)?.delete()
            val file = dir.createFile("application/json", name)
                ?: return fail(app, "Could not create backup file")
            app.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: return fail(app, "Could not write backup file")

            dir.listFiles()
                .filter { (it.name ?: "").startsWith(FILE_PREFIX) && (it.name ?: "").endsWith(".json") }
                .sortedByDescending { it.name }
                .drop(KEEP)
                .forEach { it.delete() }

            prefs(app).edit().putLong(KEY_LAST_OK, System.currentTimeMillis()).remove(KEY_LAST_ERROR).apply()
            null
        } catch (e: Exception) {
            fail(app, e.message ?: e.javaClass.simpleName)
        }
    }

    private fun fail(c: Context, msg: String): String {
        prefs(c).edit().putString(KEY_LAST_ERROR, msg).apply()
        return msg
    }
}
