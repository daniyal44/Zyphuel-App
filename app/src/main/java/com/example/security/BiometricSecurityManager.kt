package com.example.security

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

enum class BiometricCapabilityStatus {
    SUPPORTED,
    NOT_SUPPORTED,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_ERROR
}

object BiometricSecurityManager {

    /**
     * Checks the device's biometric capability using BiometricManager.
     */
    fun checkBiometricCapability(context: Context): BiometricCapabilityStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapabilityStatus.SUPPORTED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapabilityStatus.NOT_SUPPORTED
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapabilityStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapabilityStatus.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapabilityStatus.SECURITY_ERROR
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricCapabilityStatus.NOT_SUPPORTED
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricCapabilityStatus.NOT_SUPPORTED
            else -> BiometricCapabilityStatus.NOT_SUPPORTED
        }
    }

    /**
     * Formats user-friendly capability status message for UI display.
     */
    fun getStatusDescription(status: BiometricCapabilityStatus): String {
        return when (status) {
            BiometricCapabilityStatus.SUPPORTED -> "Biometric hardware is fully compatible and ready."
            BiometricCapabilityStatus.NOT_SUPPORTED -> "Biometric authentication is not supported on this device."
            BiometricCapabilityStatus.HARDWARE_UNAVAILABLE -> "Biometric hardware is currently unavailable or busy."
            BiometricCapabilityStatus.NONE_ENROLLED -> "No fingerprint or face data enrolled on this device."
            BiometricCapabilityStatus.SECURITY_ERROR -> "Security update required for biometric hardware."
        }
    }

    /**
     * Launches Android System Biometric Enrollment Settings screen.
     */
    fun openBiometricEnrollmentSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings if explicit biometric settings intent is unavailable
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    /**
     * Prompts the user with AndroidX BiometricPrompt on a FragmentActivity.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String,
        negativeButtonText: String = "Use Password",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor: Executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(-1, e.message ?: "Biometric prompt initialization notice")
        }
    }
}
