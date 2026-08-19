package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.security.MessageDigest

/**
 * Diagnostic utility to extract, format, and log the runtime app signature SHA-1 fingerprint
 * to Logcat and DebugLogger for verifying alignment with Firebase Console configuration.
 */
object SigningDiagnosticUtil {

    private const val TAG = "SigningDiagnostic"
    private const val EXPECTED_SHA1 = "C7:F7:10:F2:2D:43:D1:F3:31:D2:22:AB:35:1B:C4:47:01:EE:C7:E5"

    fun logAppSigningSha1(context: Context): String? {
        return try {
            val packageName = context.packageName
            val packageManager = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                DebugLogger.e(TAG, "No signing certificates found for package $packageName")
                return null
            }

            val cert = signatures[0].toByteArray()
            val md = MessageDigest.getInstance("SHA-1")
            val publicKey = md.digest(cert)
            val hexString = StringBuilder()

            for (i in publicKey.indices) {
                val hex = Integer.toHexString(0xFF and publicKey[i].toInt()).uppercase()
                if (hex.length == 1) {
                    hexString.append("0")
                }
                hexString.append(hex)
                if (i < publicKey.size - 1) {
                    hexString.append(":")
                }
            }

            val currentSha1 = hexString.toString()
            val cleanedExpected = EXPECTED_SHA1.replace(" ", "").trim()

            Log.i(TAG, "Package Name: $packageName")
            Log.i(TAG, "Current Build SHA-1 Fingerprint: $currentSha1")
            Log.i(TAG, "Firebase Console Target SHA-1: $cleanedExpected")

            DebugLogger.i(TAG, "Package Name: $packageName | SHA-1: $currentSha1")

            if (currentSha1.equals(cleanedExpected, ignoreCase = true)) {
                Log.i(TAG, "SHA-1 MATCH CONFIRMED: Current build signature matches Firebase Console SHA-1 ($cleanedExpected)")
                DebugLogger.i(TAG, "SHA-1 MATCH CONFIRMED: Current build matches Firebase Console SHA-1 ($cleanedExpected)")
            } else {
                Log.w(TAG, "SHA-1 MISMATCH! Current: $currentSha1 | Expected: $cleanedExpected")
                DebugLogger.w(TAG, "SHA-1 MISMATCH! Current: $currentSha1 | Expected: $cleanedExpected")
            }

            currentSha1
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error calculating SHA-1 fingerprint: ${e.message}", e)
            null
        }
    }
}
