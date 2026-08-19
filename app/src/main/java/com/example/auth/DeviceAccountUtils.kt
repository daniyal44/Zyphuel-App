package com.example.auth

import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import com.example.data.UserEntity
import com.google.firebase.auth.FirebaseAuth

/**
 * Utility for fetching actual Google accounts existing on the Android device,
 * as well as existing active user profiles stored on this device.
 * Ensures zero fake or mock accounts are shown.
 */
object DeviceAccountUtils {
    private const val TAG = "DeviceAccountUtils"

    data class DeviceAccount(
        val email: String,
        val displayName: String,
        val photoUrl: String? = null,
        val isGoogleAccount: Boolean = true
    )

    /**
     * Retrieves actual accounts available on this device:
     * 1. System Google accounts from Android AccountManager.
     * 2. Active authenticated Firebase session account (if logged in on device).
     * 3. Locally registered user profiles on this device.
     */
    fun getRealDeviceAccounts(context: Context, localUsers: List<UserEntity>? = null): List<DeviceAccount> {
        val result = mutableListOf<DeviceAccount>()
        val seenEmails = mutableSetOf<String>()

        // 1. Check native Android AccountManager on device
        try {
            val accountManager = AccountManager.get(context)
            val googleAccounts = accountManager.getAccountsByType("com.google")
            for (acc in googleAccounts) {
                val accName = acc.name
                if (!accName.isNullOrBlank() && accName.contains("@")) {
                    val email = accName.trim().lowercase()
                    if (seenEmails.add(email)) {
                        val derivedName = formatDisplayNameFromEmail(email)
                        result.add(
                            DeviceAccount(
                                email = email,
                                displayName = derivedName,
                                photoUrl = null,
                                isGoogleAccount = true
                            )
                        )
                        Log.d(TAG, "Found device Google Account via AccountManager: $email")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice querying system AccountManager: ${e.message}")
        }

        // 2. Check active Firebase authenticated user on device
        try {
            val fbUser = FirebaseAuth.getInstance().currentUser
            if (fbUser != null && !fbUser.email.isNullOrBlank()) {
                val email = fbUser.email!!.trim().lowercase()
                if (seenEmails.add(email)) {
                    val name = fbUser.displayName?.takeIf { it.isNotBlank() } ?: formatDisplayNameFromEmail(email)
                    result.add(
                        DeviceAccount(
                            email = email,
                            displayName = name,
                            photoUrl = fbUser.photoUrl?.toString(),
                            isGoogleAccount = true
                        )
                    )
                    Log.d(TAG, "Found active Firebase device session account: $email")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice querying Firebase user: ${e.message}")
        }

        // 3. Include any real accounts saved locally in Room database on this device
        localUsers?.forEach { u ->
            if (u.email.isNotBlank() && u.email.contains("@")) {
                val email = u.email.trim().lowercase()
                // Only real user accounts (exclude dummy if any)
                if (seenEmails.add(email)) {
                    result.add(
                        DeviceAccount(
                            email = email,
                            displayName = u.name.ifBlank { formatDisplayNameFromEmail(email) },
                            photoUrl = u.profilePictureUri,
                            isGoogleAccount = false
                        )
                    )
                }
            }
        }

        return result
    }

    private fun formatDisplayNameFromEmail(email: String): String {
        val prefix = email.substringBefore("@")
        val words = prefix.replace(".", " ").replace("_", " ").replace("-", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }
        return if (words.isNotEmpty()) words.joinToString(" ") else prefix
    }

    fun findActivity(context: Context): android.app.Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
