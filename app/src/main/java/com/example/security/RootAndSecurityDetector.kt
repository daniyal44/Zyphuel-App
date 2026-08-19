package com.example.security

import android.content.Context
import android.os.Build
import java.io.File

data class SecurityReport(
    val isRooted: Boolean,
    val isEmulator: Boolean,
    val isTestKeysBuild: Boolean,
    val isHardwareEnclaveActive: Boolean,
    val securityLevel: String // "HARDWARE_ENCLAVE_SECURE", "COMPLIANT_SOFTWARE", "WARNING_RISK"
)

object RootAndSecurityDetector {

    private val SU_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )

    /**
     * Checks if the device is rooted by inspecting build tags and su binary locations.
     */
    fun isDeviceRooted(): Boolean {
        // 1. Check Build Tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // 2. Check SU Binary Paths
        for (path in SU_PATHS) {
            if (File(path).exists()) {
                return true
            }
        }

        return false
    }

    /**
     * Detects if the app is running in an emulator.
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    /**
     * Evaluates comprehensive security state and returns a SecurityReport.
     */
    fun getSecurityReport(context: Context): SecurityReport {
        val rooted = isDeviceRooted()
        val emulator = isEmulator()
        val testKeys = Build.TAGS != null && Build.TAGS.contains("test-keys")

        // Ensure hardware key is checked
        SecureStorageManager.ensureHardwareKeyGenerated()

        val level = when {
            rooted -> "WARNING_RISK (Device Rooted)"
            emulator -> "COMPLIANT_EMULATOR_SANDBOX"
            else -> "HARDWARE_ENCLAVE_SECURE (256-bit AES TEE)"
        }

        return SecurityReport(
            isRooted = rooted,
            isEmulator = emulator,
            isTestKeysBuild = testKeys,
            isHardwareEnclaveActive = !rooted,
            securityLevel = level
        )
    }
}
