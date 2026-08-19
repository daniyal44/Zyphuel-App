package com.example.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String,
    val exceptionDetails: String? = null
)

/**
 * Global debug logging utility to record UI interactions, rendering state transitions,
 * component failures, and background tasks.
 */
object DebugLogger {
    private const val MAX_LOGS = 100
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun i(tag: String, message: String) {
        addLog("INFO", tag, message)
        Log.i("ZyphuelDebug", "[$tag] $message")
    }

    fun d(tag: String, message: String) {
        addLog("DEBUG", tag, message)
        Log.d("ZyphuelDebug", "[$tag] $message")
    }

    fun w(tag: String, message: String) {
        addLog("WARN", tag, message)
        Log.w("ZyphuelDebug", "[$tag] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val exStr = throwable?.let { Log.getStackTraceString(it) }
        addLog("ERROR", tag, message, exStr)
        Log.e("ZyphuelDebug", "[$tag] $message", throwable)
    }

    private fun addLog(level: String, tag: String, message: String, exceptionDetails: String? = null) {
        val time = dateFormat.format(Date())
        logQueue.add(LogEntry(time, level, tag, message, exceptionDetails))
        while (logQueue.size > MAX_LOGS) {
            logQueue.poll()
        }
    }

    fun getLogs(): List<LogEntry> = logQueue.toList()

    fun getFormattedLogs(): String {
        return logQueue.joinToString("\n") { log ->
            "[${log.timestamp}] [${log.level}] [${log.tag}]: ${log.message}" +
                    if (log.exceptionDetails != null) "\nEx: ${log.exceptionDetails}" else ""
        }
    }

    fun clear() {
        logQueue.clear()
    }
}
