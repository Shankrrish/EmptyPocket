package com.example.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stores uncaught exceptions as text files in app-private storage so they can be viewed/shared in-app. */
object CrashLogger {
    private const val DIR = "crash_logs"
    private const val MAX_FILES = 30

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(appContext, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun record(context: Context, thread: Thread?, throwable: Throwable) {
        val ts = Date()
        val name = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(ts)
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val text = buildString {
            appendLine("Time: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(ts))
            appendLine("Thread: " + (thread?.name ?: "unknown"))
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
            appendLine()
            append(sw.toString())
        }
        File(dir(context), "crash_$name.txt").writeText(text)
        dir(context).listFiles()?.sortedByDescending { it.name }?.drop(MAX_FILES)?.forEach { it.delete() }
    }

    fun list(context: Context): List<File> =
        dir(context).listFiles()?.filter { it.isFile }?.sortedByDescending { it.name } ?: emptyList()

    fun readAll(context: Context): String =
        list(context).joinToString("\n\n==========\n\n") { it.readText() }

    fun clear(context: Context) {
        list(context).forEach { it.delete() }
    }
}
