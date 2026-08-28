package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.NotificationEntity
import com.example.util.DebugLogger
import com.example.util.UnifiedAssetManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ZyphuelFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        DebugLogger.i(TAG, "New FCM Registration Token generated: $token")

        // Save token locally
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()

        // Broadcast token update
        val intent = Intent(ACTION_FCM_TOKEN_UPDATED).apply {
            putExtra(EXTRA_TOKEN, token)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        DebugLogger.i(TAG, "FCM Message Received from: ${remoteMessage.from}")

        // Extract title, body and data payloads
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: "⚡ Zyphuel Live Update"
        val message = notification?.body ?: data["message"] ?: data["body"] ?: "New update available on Zyphuel."
        val type = data["type"] ?: "delivery_status" // "delivery_status", "price_drop", "promotional_alert"
        val orderId = data["order_id"] ?: ""
        val fuelType = data["fuel_type"] ?: ""
        val newPrice = data["new_price"] ?: ""

        DebugLogger.i(TAG, "FCM Payload -> Type: $type, Title: $title, Message: $message, OrderId: $orderId")

        // 1. Post Android System Push Notification
        showSystemNotification(title, message, type, orderId)

        // 2. Persist in local Room database so it displays in-app Notification Center
        persistInAppNotification(title, message, type)

        // 3. Broadcast in-app reactive event for foreground toast/banner
        broadcastInAppNotification(title, message, type, orderId, fuelType, newPrice)
    }

    private fun showSystemNotification(title: String, message: String, type: String, orderId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Zyphuel FCM Real-Time Delivery & Price Alerts"
            val channel = NotificationChannel(
                FCM_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time delivery status updates, Bowser GPS updates, and fuel price drop notifications."
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#059669")
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("fcm_type", type)
            putExtra("fcm_order_id", orderId)
            putExtra("open_screen", if (type == "delivery_status") "tracker" else "home")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = try {
            UnifiedAssetManager.NOTIFICATION_SMALL_ICON
        } catch (e: Exception) {
            R.drawable.ic_notification
        }

        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(resources, R.drawable.icon)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, FCM_CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 100, 250))

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        try {
            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
            DebugLogger.i(TAG, "System Push Notification posted: $title")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error posting system push notification", e)
        }
    }

    private fun persistInAppNotification(title: String, message: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val targetRole = when (type) {
                    "delivery_status" -> "customer"
                    "rider_dispatch" -> "rider"
                    else -> "all"
                }
                database.notificationDao().insertNotification(
                    NotificationEntity(
                        title = title,
                        message = message,
                        timestamp = System.currentTimeMillis(),
                        targetRole = targetRole,
                        isRead = false
                    )
                )
                DebugLogger.i(TAG, "Notification saved to Room DB: $title")
            } catch (e: Exception) {
                DebugLogger.e(TAG, "Failed to insert FCM notification to Room DB", e)
            }
        }
    }

    private fun broadcastInAppNotification(
        title: String,
        message: String,
        type: String,
        orderId: String,
        fuelType: String,
        newPrice: String
    ) {
        val intent = Intent(ACTION_FCM_MESSAGE_RECEIVED).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_ORDER_ID, orderId)
            putExtra(EXTRA_FUEL_TYPE, fuelType)
            putExtra(EXTRA_NEW_PRICE, newPrice)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    companion object {
        const val TAG = "ZyphuelFcmService"
        const val FCM_CHANNEL_ID = "zyphuel_fcm_channel"

        const val PREFS_NAME = "zyphuel_fcm_prefs"
        const val KEY_FCM_TOKEN = "fcm_registration_token"

        const val ACTION_FCM_TOKEN_UPDATED = "com.example.zyphuel.FCM_TOKEN_UPDATED"
        const val ACTION_FCM_MESSAGE_RECEIVED = "com.example.zyphuel.FCM_MESSAGE_RECEIVED"

        const val EXTRA_TOKEN = "extra_fcm_token"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_FUEL_TYPE = "extra_fuel_type"
        const val EXTRA_NEW_PRICE = "extra_new_price"

        fun getStoredToken(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)
        }
    }
}
