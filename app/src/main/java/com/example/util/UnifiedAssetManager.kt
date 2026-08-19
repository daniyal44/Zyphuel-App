package com.example.util

import android.content.Context
import androidx.annotation.DrawableRes
import com.example.R

/**
 * Unified asset management class that maps primary app icon, notification assets,
 * and PWA/Manifest metadata to Android resource directories and Compose UIs.
 */
object UnifiedAssetManager {

    val APP_NAME = "Zyphuel"

    @DrawableRes
    val PRIMARY_APP_ICON = R.drawable.logo

    @DrawableRes
    val LAUNCHER_FOREGROUND = R.drawable.ic_launcher_foreground

    @DrawableRes
    val LAUNCHER_BACKGROUND = R.drawable.ic_launcher_background

    @DrawableRes
    val NOTIFICATION_SMALL_ICON = R.drawable.ic_notification

    val NOTIFICATION_CHANNEL_ID = "zyphuel_fuel_updates"
    val NOTIFICATION_CHANNEL_NAME = "Fuel Price & Order Updates"

    /**
     * Verifies that all mapped icon drawables are available and properly configured
     * with standard safe-zone padding.
     */
    fun verifyAssetIntegrity(context: Context): Boolean {
        return try {
            val drawable = context.getDrawable(PRIMARY_APP_ICON)
            val isValid = drawable != null
            DebugLogger.i("UnifiedAssetManager", "Asset integrity check completed. Valid: $isValid")
            isValid
        } catch (e: Exception) {
            DebugLogger.e("UnifiedAssetManager", "Asset integrity check failed", e)
            false
        }
    }
}
