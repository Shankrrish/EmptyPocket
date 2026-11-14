package com.example.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!AutoBackupManager.isEnabled(applicationContext)) return Result.success()
        // Errors are recorded and shown in Settings; do not retry-loop.
        AutoBackupManager.runBackup(applicationContext)
        return Result.success()
    }
}
