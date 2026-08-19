package com.example.security

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

data class RateLimitConfig(
    val authMaxAttempts: Int = 5,
    val authWindowMs: Long = 300_000L, // 5 minutes window
    val authBaseBackoffMs: Long = 15_000L, // 15 seconds initial backoff
    val authMaxBackoffMs: Long = 900_000L, // 15 minutes max backoff
    val publicMaxPerMinute: Int = 30,
    val authenticatedActionMaxPerMinute: Int = 60
)

sealed class RateLimitResult {
    object Allowed : RateLimitResult()
    data class Blocked(val remainingCooldownSeconds: Long, val reason: String) : RateLimitResult()
}

object SecurityRateLimiter {

    @Volatile
    private var config = RateLimitConfig()

    private val authAttemptsMap = ConcurrentHashMap<String, MutableList<Long>>()
    private val authBackoffMap = ConcurrentHashMap<String, Long>() // Key -> Unblock timestamp
    private val authConsecutiveFailures = ConcurrentHashMap<String, Int>()

    private val publicEndpointTracker = ConcurrentHashMap<String, MutableList<Long>>()
    private val authenticatedActionTracker = ConcurrentHashMap<String, MutableList<Long>>()

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
        val combinedKey = "${clientIdentifier.trim().lowercase()}_${accountKey.trim().lowercase()}"

        // Check if currently under exponential backoff
        val unblockTime = authBackoffMap[combinedKey] ?: 0L
        if (now < unblockTime) {
            val cooldownSec = ((unblockTime - now) / 1000L).coerceAtLeast(1L)
            return RateLimitResult.Blocked(
                remainingCooldownSeconds = cooldownSec,
                reason = "Too many authentication attempts. Please wait $cooldownSec seconds before trying again."
            )
        }

        val timestamps = authAttemptsMap.computeIfAbsent(combinedKey) { mutableListOf() }
        synchronized(timestamps) {
            // Remove entries outside current sliding window
            timestamps.removeAll { now - it > config.authWindowMs }

            if (timestamps.size >= config.authMaxAttempts) {
                val failures = (authConsecutiveFailures[combinedKey] ?: config.authMaxAttempts) + 1
                authConsecutiveFailures[combinedKey] = failures

                // Calculate exponential backoff
                val backoffMs = (config.authBaseBackoffMs * (2.0.pow(failures - config.authMaxAttempts))).toLong()
                    .coerceAtMost(config.authMaxBackoffMs)

                val newUnblockTime = now + backoffMs
                authBackoffMap[combinedKey] = newUnblockTime

                val cooldownSec = (backoffMs / 1000L).coerceAtLeast(1L)
                return RateLimitResult.Blocked(
                    remainingCooldownSeconds = cooldownSec,
                    reason = "Authentication attempt limit exceeded. Exponential backoff applied for $cooldownSec seconds."
                )
            }

            timestamps.add(now)
        }

        return RateLimitResult.Allowed
    }

    /**
     * Resets failure counters upon successful authentication.
     */
    fun recordAuthSuccess(clientIdentifier: String, accountKey: String) {
        val combinedKey = "${clientIdentifier.trim().lowercase()}_${accountKey.trim().lowercase()}"
        authAttemptsMap.remove(combinedKey)
        authBackoffMap.remove(combinedKey)
        authConsecutiveFailures.remove(combinedKey)
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
        authConsecutiveFailures.clear()
        publicEndpointTracker.clear()
        authenticatedActionTracker.clear()
    }
}
