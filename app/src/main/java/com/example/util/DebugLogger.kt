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

    private val SENSITIVE_PATTERNS = listOf(
        Regex("(?i)(password|token|secret|apiKey|hash|credential|bearer)\\s*[:=]\\s*[^\\s,;]+"),
        Regex("(?i)SEC_TOKEN_[A-Z]+_[^\\s]+"),
        Regex("(?i)AIza[0-9A-Za-z_-]{35}")
    )

    private fun sanitizeMessage(msg: String): String {
        var clean = msg
        for (pattern in SENSITIVE_PATTERNS) {
            clean = clean.replace(pattern) { matchResult ->
                val key = matchResult.value.substringBefore("=").substringBefore(":")
                if (key.length < matchResult.value.length) "$key=[REDACTED]" else "[REDACTED]"
            }
        }
        return clean
    }

    fun i(tag: String, message: String) {
        val sanitized = sanitizeMessage(message)
        addLog("INFO", tag, sanitized)
        Log.i("ZyphuelDebug", "[$tag] $sanitized")
    }

    fun d(tag: String, message: String) {
        val sanitized = sanitizeMessage(message)
        addLog("DEBUG", tag, sanitized)
        Log.d("ZyphuelDebug", "[$tag] $sanitized")
    }

    fun w(tag: String, message: String) {
        val sanitized = sanitizeMessage(message)
        addLog("WARN", tag, sanitized)
        Log.w("ZyphuelDebug", "[$tag] $sanitized")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = sanitizeMessage(message)
        val exStr = throwable?.let { Log.getStackTraceString(it) }?.let { sanitizeMessage(it) }
        addLog("ERROR", tag, sanitized, exStr)
        Log.e("ZyphuelDebug", "[$tag] $sanitized", throwable)
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
