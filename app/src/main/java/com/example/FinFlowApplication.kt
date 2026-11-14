package com.example

import android.app.Application
import com.example.util.AutoBackupManager
import com.example.util.CrashLogger

class FinFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        AutoBackupManager.schedule(this)
    }
}
