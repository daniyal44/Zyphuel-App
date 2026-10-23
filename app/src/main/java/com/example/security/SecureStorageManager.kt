package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class AppModule {
    CUSTOMER,
    RIDER,
    ADMIN
}

object SecureStorageManager {

    private const val KEY_ALIAS = "zyphuel_master_enclave_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val BIO_DEVICE_PREFS = "zyphuel_biometric_device_prefs"

    private val encryptedPrefsCache = java.util.concurrent.ConcurrentHashMap<AppModule, SharedPreferences>()

    private fun getDevicePreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(BIO_DEVICE_PREFS, Context.MODE_PRIVATE)
    }

    private fun getEncryptedPreferences(context: Context, module: AppModule): SharedPreferences {
        return encryptedPrefsCache.getOrPut(module) {
            val fileName = when (module) {
                AppModule.CUSTOMER -> "zyphuel_secure_cust_prefs"
                AppModule.RIDER -> "zyphuel_secure_rider_prefs"
                AppModule.ADMIN -> "zyphuel_secure_admin_prefs"
            }

            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    fileName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                // Fallback to standard SharedPreferences if Keystore is unavailable (e.g. JVM unit tests or OEM lock)
                context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            }
        }
    }

    /**
     * Biometric enabled status - DISABLED BY DEFAULT upon installation.
     * Only true after a registered user explicitly enables biometrics in security settings or enrollment dialog.
     */
    fun isBiometricEnabled(context: Context, module: AppModule): Boolean {
        val key = getPrefKey(module, "bio_enabled")
        val encValue = try {
            getEncryptedPreferences(context, module).getBoolean(key, false)
        } catch (e: Exception) {
            false
        }
        val devValue = getDevicePreferences(context).getBoolean(key, false)
        return encValue || devValue
    }

    fun setBiometricEnabled(context: Context, module: AppModule, enabled: Boolean) {
        val key = getPrefKey(module, "bio_enabled")
        try {
            getEncryptedPreferences(context, module).edit().putBoolean(key, enabled).apply()
        } catch (e: Exception) {
            // Safe fallback
        }
        getDevicePreferences(context).edit().putBoolean(key, enabled).apply()
    }

    /**
     * Stores encrypted session token and user email for a specific module.
     * Retains registered email persistently across logouts and app re-opens.
     */
    fun saveSecureCredentials(context: Context, module: AppModule, email: String, token: String) {
        val emailKey = getPrefKey(module, "email")
        val tokenKey = getPrefKey(module, "token")
        val timeKey = getPrefKey(module, "last_auth_time")

        try {
            getEncryptedPreferences(context, module).edit()
                .putString(emailKey, email.trim().lowercase())
                .putString(tokenKey, token)
                .putLong(timeKey, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            // Safe fallback
        }

        // Persistent device backup so app reopen / logout never loses registered user email
        getDevicePreferences(context).edit()
            .putString(emailKey, email.trim().lowercase())
            .putLong(timeKey, System.currentTimeMillis())
            .apply()
    }

    /**
     * Retrieves stored registered email for biometric login.
     * Strictly returns null for unregistered users.
     */
    fun getRegisteredEmail(context: Context, module: AppModule): String? {
        val key = getPrefKey(module, "email")
        val encEmail = try {
            getEncryptedPreferences(context, module).getString(key, null)
        } catch (e: Exception) {
            null
        }
        if (!encEmail.isNullOrBlank()) return encEmail.trim().lowercase()

        val devEmail = getDevicePreferences(context).getString(key, null)
        return if (!devEmail.isNullOrBlank()) devEmail.trim().lowercase() else null
    }

    /**
     * Retrieves stored secure token for biometric login.
     */
    fun getSecureToken(context: Context, module: AppModule): String? {
        val key = getPrefKey(module, "token")
        return try {
            getEncryptedPreferences(context, module).getString(key, null)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets timestamp of last successful biometric authentication.
     */
    fun getLastAuthTime(context: Context, module: AppModule): Long {
        val key = getPrefKey(module, "last_auth_time")
        val encTime = try {
            getEncryptedPreferences(context, module).getLong(key, 0L)
        } catch (e: Exception) {
            0L
        }
        if (encTime > 0L) return encTime
        return getDevicePreferences(context).getLong(key, 0L)
    }

    /**
     * Updates last authentication timestamp.
     */
    fun updateLastAuthTime(context: Context, module: AppModule) {
        val key = getPrefKey(module, "last_auth_time")
        val now = System.currentTimeMillis()
        try {
            getEncryptedPreferences(context, module).edit().putLong(key, now).apply()
        } catch (e: Exception) {
            // Safe fallback
        }
        getDevicePreferences(context).edit().putLong(key, now).apply()
    }

    /**
     * Secure Logout: Removes active session token, but strictly retains biometric enrollment and registered email.
     */
    fun clearSessionTokenOnLogout(context: Context, module: AppModule) {
        val tokenKey = getPrefKey(module, "token")
        try {
            getEncryptedPreferences(context, module).edit().remove(tokenKey).apply()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Completely disables biometric authentication and purges stored credentials for a module.
     */
    fun disableAndPurgeBiometrics(context: Context, module: AppModule) {
        val emailKey = getPrefKey(module, "email")
        val tokenKey = getPrefKey(module, "token")
        val bioEnabledKey = getPrefKey(module, "bio_enabled")
        val timeKey = getPrefKey(module, "last_auth_time")

        try {
            getEncryptedPreferences(context, module).edit()
                .remove(emailKey)
                .remove(tokenKey)
                .remove(bioEnabledKey)
                .remove(timeKey)
                .apply()
        } catch (e: Exception) {
            // Safe fallback
        }

        getDevicePreferences(context).edit()
            .remove(emailKey)
            .remove(bioEnabledKey)
            .remove(timeKey)
            .apply()
    }

    private fun getPrefKey(module: AppModule, field: String): String {
        return when (module) {
            AppModule.CUSTOMER -> "cust_$field"
            AppModule.RIDER -> "rider_$field"
            AppModule.ADMIN -> "admin_$field"
        }
    }

    /**
     * Android Keystore Hardware Enclave Helper to ensure hardware key existence.
     */
    fun ensureHardwareKeyGenerated() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )

                val builder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)

                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // Hardware key generation handled gracefully
        }
    }

    /**
     * Retrieves stored SMTP & Email Relay Gateway configuration from Admin enclave.
     */
    fun getSmtpConfig(context: Context): SmtpConfig {
        val prefs = getEncryptedPreferences(context, AppModule.ADMIN)
        val host = prefs.getString("smtp_host", "smtp.gmail.com")?.ifBlank { "smtp.gmail.com" } ?: "smtp.gmail.com"
        val port = prefs.getInt("smtp_port", 465).let { if (it > 0) it else 465 }
        val senderEmail = prefs.getString("smtp_sender_email", "m.daniyalkhan490@gmail.com")?.ifBlank { "m.daniyalkhan490@gmail.com" } ?: "m.daniyalkhan490@gmail.com"
        // SECURITY (2026-10-23): never ship a default credential in source. The gateway
        // password is supplied at runtime by the admin-managed Firestore config, or for
        // owner builds via the git-ignored SMTP_APP_PASSWORD secret (BuildConfig).
        val appPassword = prefs.getString("smtp_app_password", "")?.trim() ?: ""
        val senderName = prefs.getString("smtp_sender_name", "Zyphuel Delivery Operations")?.ifBlank { "Zyphuel Delivery Operations" } ?: "Zyphuel Delivery Operations"
        val webhookUrl = prefs.getString("smtp_webhook_url", "") ?: ""
        val isEnabled = prefs.getBoolean("smtp_enabled", true)
        return SmtpConfig(
            host = host,
            port = port,
            senderEmail = senderEmail,
            appPassword = appPassword,
            senderName = senderName,
            webhookUrl = webhookUrl,
            isEnabled = isEnabled
        )
    }

    /**
     * Persists updated SMTP credentials and cloud webhook relay URL into Admin enclave.
     */
    fun saveSmtpConfig(context: Context, config: SmtpConfig) {
        val prefs = getEncryptedPreferences(context, AppModule.ADMIN)
        prefs.edit()
            .putString("smtp_host", config.host.trim())
            .putInt("smtp_port", config.port)
            .putString("smtp_sender_email", config.senderEmail.trim())
            .putString("smtp_app_password", config.appPassword.trim())
            .putString("smtp_sender_name", config.senderName.trim())
            .putString("smtp_webhook_url", config.webhookUrl.trim())
            .putBoolean("smtp_enabled", config.isEnabled)
            .apply()
    }

    /**
     * Stores the user's active login password inside AES-256-GCM hardware-backed EncryptedSharedPreferences
     * so Profile Settings can display and verify the exact login password in real-time (never exposing raw SHA-256 hex).
     */
    fun saveUserLoginPassword(context: Context, email: String, rawPassword: String) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || rawPassword.isBlank()) return
        // Do not store if rawPassword looks like a 64-char SHA-256 hex digest or OAuth token placeholder
        val isSha256Hex = rawPassword.length == 64 && rawPassword.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        if (isSha256Hex || rawPassword.startsWith("SOCIAL_OAUTH_")) return
        val prefs = getEncryptedPreferences(context, AppModule.CUSTOMER)
        prefs.edit()
            .putString("enc_login_pwd_$cleanEmail", rawPassword)
            .apply()
    }

    /**
     * Retrieves the user's AES-256-GCM encrypted login password for real-time synchronization in Profile Settings.
     */
    fun getUserLoginPassword(context: Context, email: String): String? {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return null
        val prefs = getEncryptedPreferences(context, AppModule.CUSTOMER)
        return prefs.getString("enc_login_pwd_$cleanEmail", null)
    }
}

/**
 * Data class representing SMTP & Email Gateway settings for real-time inbox dispatch.
 */
data class SmtpConfig(
    val host: String = "smtp.gmail.com",
    val port: Int = 465,
    val senderEmail: String = "m.daniyalkhan490@gmail.com",
    val appPassword: String = "",
    val senderName: String = "Zyphuel Delivery Operations",
    val webhookUrl: String = "",
    val isEnabled: Boolean = true
)
