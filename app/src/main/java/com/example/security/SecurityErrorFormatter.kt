package com.example.security

import android.util.Log

object SecurityErrorFormatter {

    private const val TAG = "ZyphuelSecurityLogger"

    /**
     * Converts raw exception or error message into generic, non-leaking user-facing error message.
     * Logs the full raw trace/details internally for debugging.
     */
    fun formatUserError(throwable: Throwable?, defaultUserMessage: String = "An unexpected error occurred. Please try again."): String {
        if (throwable != null) {
            Log.e(TAG, "Internal System Exception: ${throwable.message}", throwable)
        }

        val rawMsg = throwable?.message ?: return defaultUserMessage

        // Check for internal path or stack trace leakage signatures
        val isInternalPath = rawMsg.contains("/data/data/") || rawMsg.contains("/storage/") || rawMsg.contains(".kt:") || rawMsg.contains("Exception")
        val isDatabaseError = rawMsg.contains("SQLite") || rawMsg.contains("FOREIGN KEY") || rawMsg.contains("UNIQUE constraint") || rawMsg.contains("ROOM")

        return when {
            isDatabaseError -> "Database operation failed. Please verify your data entry and try again."
            isInternalPath -> "A system processing error occurred. Please try again later."
            rawMsg.contains("rate limit", ignoreCase = true) -> rawMsg
            rawMsg.contains("invalid", ignoreCase = true) -> rawMsg
            else -> defaultUserMessage
        }
    }

    /**
     * Format a string error safely.
     */
    fun sanitizeErrorMessage(rawMessage: String, fallback: String = "Action could not be completed."): String {
        if (rawMessage.contains("/data/data/") || rawMessage.contains(".kt:") || rawMessage.contains("SQLiteException")) {
            Log.e(TAG, "Raw error sanitized to prevent information leak: $rawMessage")
            return fallback
        }
        return rawMessage
    }
}
