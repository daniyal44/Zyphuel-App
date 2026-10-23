package com.example.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

data class RateLimitConfig(
    val authMaxAttempts: Int = 4, // BRUTE-FORCE RULE: 4 wrong passwords ...
    val authWindowMs: Long = 60_000L, // ... within 1 minute trigger the lockout
    val authLockoutMs: Long = 300_000L, // 5 minutes cooldown after the lockout (Error 422)
    val authBaseBackoffMs: Long = 15_000L, // 15 seconds initial backoff (non-login auth routes)
    val authMaxBackoffMs: Long = 900_000L, // 15 minutes max backoff
    val publicMaxPerMinute: Int = 30,
    val authenticatedActionMaxPerMinute: Int = 60
)

sealed class RateLimitResult {
    object Allowed : RateLimitResult()
    data class Blocked(val remainingCooldownSeconds: Long, val reason: String) : RateLimitResult()
}

/**
 * Central auth/action rate limiter.
 *
 * Hardening added 2026-10-23:
 * - Lockout and failure state is persisted to private app storage. Storage keys are
 *   SHA-256 hashes of the account key, so no email/client identifier is written in
 *   clear text, and force-closing or restarting the app cannot bypass the cooldown.
 * - In-session remaining time is also tracked with `SystemClock.elapsedRealtime()` so a
 *   manual device-clock change cannot shorten an active lockout.
 * - Client-side enforcement only: the app has no self-hosted auth server, so there is no
 *   shared Redis-style store across devices. Documented in SECURITY.md.
 */
object SecurityRateLimiter {

    @Volatile
    private var config = RateLimitConfig()

    private val authAttemptsMap = ConcurrentHashMap<String, MutableList<Long>>()
    private val authBackoffMap = ConcurrentHashMap<String, Long>() // combinedKey -> wall unblock time
    private val authBackoffElapsedMap = ConcurrentHashMap<String, Long>() // combinedKey -> elapsedRealtime unblock
    private val authConsecutiveFailures = ConcurrentHashMap<String, Int>()

    private val publicEndpointTracker = ConcurrentHashMap<String, MutableList<Long>>()
    private val authenticatedActionTracker = ConcurrentHashMap<String, MutableList<Long>>()

    // Per-account mutexes (added 2026-10-23): parallel login attempts for the same
    // account must not race on the read-modify-write of the failure window, otherwise
    // two simultaneous requests could both read "3 failures" and only count one.
    private val accountLocks = ConcurrentHashMap<String, Any>()

    private fun lockFor(key: String): Any = accountLocks.computeIfAbsent(key) { Any() }

    // --- Persistence (added 2026-10-23) ------------------------------------
    private const val PREFS_NAME = "zyphuel_auth_guard"
    private const val KEY_LOCKOUT_PREFIX = "lockout_v1_"
    private const val KEY_FAILURES_PREFIX = "failures_v1_"

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var appContext: Context? = null

    /**
     * Initializes persistence. Safe to call repeatedly.
     * [forceReload] simulates a fresh process (clears in-memory state, re-reads storage)
     * and is used by the restart-bypass regression test.
     */
    fun initialize(context: Context, forceReload: Boolean = false) {
        val app = context.applicationContext
        if (prefs != null && !forceReload && appContext === app) return
        synchronized(this) {
            if (prefs != null && !forceReload && appContext === app) return
            val contextChanged = appContext !== app
            appContext = app
            val p = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p

            if (forceReload || contextChanged) {
                authAttemptsMap.clear()
                authBackoffMap.clear()
                authBackoffElapsedMap.clear()
                authConsecutiveFailures.clear()
            }

            // Prune lockouts that already expired.
            val now = System.currentTimeMillis()
            var editor: SharedPreferences.Editor? = null
            for ((storageKey, value) in p.all) {
                if (storageKey.startsWith(KEY_LOCKOUT_PREFIX) && value is Long && value <= now) {
                    if (editor == null) editor = p.edit()
                    editor.remove(storageKey)
                }
            }
            editor?.apply()
        }
    }

    private fun combinedKey(clientIdentifier: String, accountKey: String): String =
        "${clientIdentifier.trim().lowercase()}_${accountKey.trim().lowercase()}"

    /** SHA-256 hashed storage key: persisted data never contains emails or client IDs. */
    private fun storageKey(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun lockoutStorageKey(key: String) = KEY_LOCKOUT_PREFIX + storageKey(key)
    private fun failuresStorageKey(key: String) = KEY_FAILURES_PREFIX + storageKey(key)

    private fun nowElapsed(): Long = try {
        android.os.SystemClock.elapsedRealtime()
    } catch (_: Throwable) {
        -1L
    }

    /** Remaining cooldown combining persisted wall-clock state and in-memory monotonic time. */
    private fun remainingLockoutMs(key: String): Long {
        val now = System.currentTimeMillis()
        var wallUnblock = authBackoffMap[key] ?: 0L

        // Restored-from-disk lockout (also covers a cold start where memory is empty).
        val persistedUnblock = prefs?.getLong(lockoutStorageKey(key), 0L) ?: 0L
        if (persistedUnblock > 0L) {
            if (persistedUnblock > wallUnblock) {
                wallUnblock = persistedUnblock
                authBackoffMap[key] = persistedUnblock
            } else if (persistedUnblock <= now) {
                prefs?.edit()?.remove(lockoutStorageKey(key))?.apply()
            }
        }

        val wallRemaining = (wallUnblock - now).coerceAtLeast(0L)
        val elapsedUnblock = authBackoffElapsedMap[key] ?: 0L
        val nowE = nowElapsed()
        val elapsedRemaining = if (elapsedUnblock > 0L && nowE > 0L && elapsedUnblock > nowE) {
            (elapsedUnblock - nowE).coerceAtLeast(0L)
        } else {
            0L
        }
        return maxOf(wallRemaining, elapsedRemaining)
    }

    private fun secondsRemaining(remainingMs: Long): Long = ((remainingMs + 999L) / 1000L).coerceAtLeast(1L)

    private fun applyLockout(key: String, lockoutMs: Long) {
        val wallUnblock = System.currentTimeMillis() + lockoutMs
        authBackoffMap[key] = wallUnblock
        val nowE = nowElapsed()
        if (nowE > 0L) authBackoffElapsedMap[key] = nowE + lockoutMs
        prefs?.edit()?.putLong(lockoutStorageKey(key), wallUnblock)?.apply()
    }

    private fun clearLockout(key: String) {
        authBackoffMap.remove(key)
        authBackoffElapsedMap.remove(key)
        prefs?.edit()?.remove(lockoutStorageKey(key))?.apply()
    }

    /** Failure timestamps for the sliding window; persisted state is authoritative. */
    private fun loadFailures(key: String): MutableList<Long> {
        val now = System.currentTimeMillis()
        val stored = prefs?.getString(failuresStorageKey(key), null)
        val list = if (stored != null) {
            stored.split(",").mapNotNull { it.toLongOrNull() }.filter { now - it <= config.authWindowMs }
                .toMutableList()
        } else {
            authAttemptsMap[key]?.filter { now - it <= config.authWindowMs }?.toMutableList() ?: mutableListOf()
        }
        authAttemptsMap[key] = list
        return list
    }

    private fun saveFailures(key: String, list: List<Long>) {
        authAttemptsMap[key] = list.toMutableList()
        val storageKey = failuresStorageKey(key)
        if (list.isEmpty()) {
            prefs?.edit()?.remove(storageKey)?.apply()
        } else {
            prefs?.edit()?.putString(storageKey, list.joinToString(","))?.apply()
        }
    }

    fun updateConfig(newConfig: RateLimitConfig) {
        config = newConfig
    }

    fun getConfig(): RateLimitConfig = config

    /**
     * Checks rate limits for Auth routes (login, signup, password reset).
     * Enforces per-client/IP and per-account rate limits with exponential backoff.
     */
    fun checkAndRecordAuthAttempt(clientIdentifier: String, accountKey: String): RateLimitResult {
        val now = System.currentTimeMillis()
        val key = combinedKey(clientIdentifier, accountKey)

        val remaining = remainingLockoutMs(key)
        if (remaining > 0L) {
            val cooldownSec = secondsRemaining(remaining)
            return RateLimitResult.Blocked(
                remainingCooldownSeconds = cooldownSec,
                reason = "Too many authentication attempts. Please wait $cooldownSec seconds before trying again."
            )
        }
        clearLockout(key)

        synchronized(lockFor(key)) {
            val timestamps = loadFailures(key)
            if (timestamps.size >= config.authMaxAttempts) {
                val failures = (authConsecutiveFailures[key] ?: config.authMaxAttempts) + 1
                authConsecutiveFailures[key] = failures

                val backoffMs = (config.authBaseBackoffMs * (2.0.pow(failures - config.authMaxAttempts))).toLong()
                    .coerceAtMost(config.authMaxBackoffMs)

                applyLockout(key, backoffMs)
                saveFailures(key, emptyList())

                val cooldownSec = (backoffMs / 1000L).coerceAtLeast(1L)
                return RateLimitResult.Blocked(
                    remainingCooldownSeconds = cooldownSec,
                    reason = "Authentication attempt limit exceeded. Exponential backoff applied for $cooldownSec seconds."
                )
            }

            timestamps.add(now)
            saveFailures(key, timestamps)
        }

        return RateLimitResult.Allowed
    }

    /**
     * Check-only lockout probe for password login. Returns [RateLimitResult.Blocked]
     * when the account is currently inside the cooldown window. Records nothing.
     * Shows the app's "Error 422" UI code (see SECURITY.md for the compatibility note).
     */
    fun checkAuthLockout(clientIdentifier: String, accountKey: String): RateLimitResult {
        val key = combinedKey(clientIdentifier, accountKey)
        val remaining = remainingLockoutMs(key)
        if (remaining > 0L) {
            val cooldownSec = secondsRemaining(remaining)
            return RateLimitResult.Blocked(
                remainingCooldownSeconds = cooldownSec,
                reason = "Error 422: Too many failed login attempts. Please wait $cooldownSec seconds before trying again."
            )
        }
        clearLockout(key)
        return RateLimitResult.Allowed
    }

    /**
     * Records ONE FAILED password attempt for [accountKey].
     * When [RateLimitConfig.authMaxAttempts] (4) failures happen inside
     * [RateLimitConfig.authWindowMs] (1 minute), the account is locked for
     * [RateLimitConfig.authLockoutMs] (5 minutes) and an "Error 422" message
     * with the remaining cooldown seconds is returned.
     * Returns null while the account is still unlocked.
     */
    fun recordAuthFailure(clientIdentifier: String, accountKey: String): String? {
        val now = System.currentTimeMillis()
        val key = combinedKey(clientIdentifier, accountKey)

        val remaining = remainingLockoutMs(key)
        if (remaining > 0L) {
            val cooldownSec = secondsRemaining(remaining)
            return "Error 422: Too many failed login attempts. Please wait $cooldownSec seconds before trying again."
        }
        clearLockout(key)

        synchronized(lockFor(key)) {
            // Atomic read-modify-write: the window is re-read inside the lock so
            // concurrent attempts are counted exactly once each.
            val timestamps = loadFailures(key)
            // Sliding window: only failures from the last minute count
            timestamps.removeAll { now - it > config.authWindowMs }
            timestamps.add(now)

            if (timestamps.size >= config.authMaxAttempts) {
                authConsecutiveFailures[key] = (authConsecutiveFailures[key] ?: 0) + 1
                applyLockout(key, config.authLockoutMs)
                saveFailures(key, emptyList())
                val cooldownSec = config.authLockoutMs / 1000L
                return "Error 422: Too many failed login attempts. Login is locked for 5 minutes ($cooldownSec seconds). Please try again later."
            }
            saveFailures(key, timestamps)
        }
        return null
    }

    /**
     * Resets failure counters upon successful authentication.
     */
    fun recordAuthSuccess(clientIdentifier: String, accountKey: String) {
        val key = combinedKey(clientIdentifier, accountKey)
        authAttemptsMap.remove(key)
        authBackoffMap.remove(key)
        authBackoffElapsedMap.remove(key)
        authConsecutiveFailures.remove(key)
        prefs?.edit()
            ?.remove(lockoutStorageKey(key))
            ?.remove(failuresStorageKey(key))
            ?.apply()
    }

    /**
     * Checks rate limits for moderate public endpoints (e.g. guest rate queries, public FAQs).
     */
    fun checkPublicEndpointLimit(clientIdentifier: String, endpointName: String): RateLimitResult {
        val now = System.currentTimeMillis()
        val key = "pub_${clientIdentifier}_$endpointName"
        val timestamps = publicEndpointTracker.computeIfAbsent(key) { mutableListOf() }

        synchronized(timestamps) {
            timestamps.removeAll { now - it > 60_000L } // 1 minute window
            if (timestamps.size >= config.publicMaxPerMinute) {
                val resetInSec = ((60_000L - (now - (timestamps.firstOrNull() ?: now))) / 1000L).coerceAtLeast(1L)
                return RateLimitResult.Blocked(
                    remainingCooldownSeconds = resetInSec,
                    reason = "Public endpoint rate limit reached. Limit: ${config.publicMaxPerMinute} requests/min. Try again in $resetInSec seconds."
                )
            }
            timestamps.add(now)
        }

        return RateLimitResult.Allowed
    }

    /**
     * Checks rate limits for looser authenticated user actions (e.g. order creation, tracking updates).
     */
    fun checkAuthenticatedActionLimit(userId: String, actionName: String): RateLimitResult {
        val now = System.currentTimeMillis()
        val key = "auth_act_${userId}_$actionName"
        val timestamps = authenticatedActionTracker.computeIfAbsent(key) { mutableListOf() }

        synchronized(timestamps) {
            timestamps.removeAll { now - it > 60_000L } // 1 minute window
            if (timestamps.size >= config.authenticatedActionMaxPerMinute) {
                val resetInSec = ((60_000L - (now - (timestamps.firstOrNull() ?: now))) / 1000L).coerceAtLeast(1L)
                return RateLimitResult.Blocked(
                    remainingCooldownSeconds = resetInSec,
                    reason = "Action rate limit reached (${config.authenticatedActionMaxPerMinute}/min). Please wait $resetInSec seconds."
                )
            }
            timestamps.add(now)
        }

        return RateLimitResult.Allowed
    }

    /**
     * Clear all rate limiting state (e.g. for testing or admin reset).
     */
    fun resetAllLimits() {
        authAttemptsMap.clear()
        authBackoffMap.clear()
        authBackoffElapsedMap.clear()
        authConsecutiveFailures.clear()
        publicEndpointTracker.clear()
        authenticatedActionTracker.clear()
        prefs?.edit()?.clear()?.apply()
    }
}
