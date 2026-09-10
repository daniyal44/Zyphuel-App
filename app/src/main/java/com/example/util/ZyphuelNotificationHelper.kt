package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

/**
 * Centralized modern notification builder and dispatcher for Zyphuel.
 * Produces clean conversation-style notifications featuring:
 * - Circular avatar on the left (Zyphuel Flame avatar)
 * - Brand app badge overlay at the bottom-right of the avatar
 * - Clean title, timestamp, and body
 * - Quick interactive action chips ("Order Fuel", "View Rates")
 * - Strictly avoids placing awkward large standalone icons on the right side
 */
object ZyphuelNotificationHelper {

    private const val TAG = "ZyphuelNotificationHelper"

    /**
     * Creates or updates the notification channel for Android 8.0+
     */
    fun createNotificationChannel(
        context: Context,
        channelId: String = UnifiedAssetManager.NOTIFICATION_CHANNEL_ID,
        channelName: String = UnifiedAssetManager.NOTIFICATION_CHANNEL_NAME,
        importance: Int = NotificationManager.IMPORTANCE_HIGH
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Zyphuel Fuel Rates, Orders, and Delivery Updates"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Decodes and returns the circular avatar Bitmap for notification sender representation.
     */
    fun getAvatarBitmap(context: Context): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_zyphuel_avatar)
        } catch (e: Exception) {
            try {
                BitmapFactory.decodeResource(context.resources, R.drawable.icon)
            } catch (ex: Exception) {
                null
            }
        }
    }

    /**
     * Dispatches a modern conversation-style notification with a circular avatar on the left,
     * app badge overlay, and interactive action chips.
     */
    fun dispatchModernNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = UnifiedAssetManager.NOTIFICATION_CHANNEL_ID,
        channelName: String = UnifiedAssetManager.NOTIFICATION_CHANNEL_NAME,
        senderName: String = "Zyphuel Fuel Alert",
        openScreen: String? = null,
        includeFuelActions: Boolean = true,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        try {
            createNotificationChannel(context, channelId, channelName)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (openScreen != null) {
                    putExtra("open_screen", openScreen)
                }
            }

            val mainPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val avatarBmp = getAvatarBitmap(context)

            // Setup Person representing Zyphuel bot / service
            val personBuilder = Person.Builder()
                .setName(senderName)
                .setBot(true)

            if (avatarBmp != null) {
                personBuilder.setIcon(IconCompat.createWithBitmap(avatarBmp))
            } else {
                personBuilder.setIcon(IconCompat.createWithResource(context, UnifiedAssetManager.NOTIFICATION_AVATAR))
            }

            val person = personBuilder.build()

            // Modern MessagingStyle layout: avatar on the left, small app badge on bottom-right of avatar
            val messagingStyle = NotificationCompat.MessagingStyle(person)
                .setConversationTitle(title)
                .addMessage(
                    NotificationCompat.MessagingStyle.Message(
                        message,
                        System.currentTimeMillis(),
                        person
                    )
                )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(UnifiedAssetManager.NOTIFICATION_SMALL_ICON)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(messagingStyle)
                .setContentIntent(mainPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVibrate(longArrayOf(0, 250, 100, 250))

            if (includeFuelActions) {
                // Action 1: Order Fuel
                val orderIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("open_screen", "order")
                }
                val orderPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId + 1,
                    orderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    R.drawable.ic_notification,
                    "⛽ Order Fuel",
                    orderPendingIntent
                )

                // Action 2: View Rates
                val ratesIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("open_screen", "fuel_rates")
                }
                val ratesPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId + 2,
                    ratesIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    R.drawable.ic_notification,
                    "📊 View Rates",
                    ratesPendingIntent
                )
            }

            notificationManager.notify(notificationId, builder.build())
            DebugLogger.i(TAG, "Modern notification successfully posted: $title")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Failed to dispatch modern notification", e)
        }
    }
}
