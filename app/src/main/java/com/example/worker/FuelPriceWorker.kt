package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.data.AppDatabase
import com.example.data.NotificationEntity
import com.example.security.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class FuelPriceWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sharedPrefs = appContext.getSharedPreferences("zyphuel_prices", Context.MODE_PRIVATE)
            val oldPetrol = sharedPrefs.getFloat("price_petrol", 272.82f)
            val oldLpg = sharedPrefs.getFloat("price_lpg_gas", 230.00f)

            var newPetrol = oldPetrol
            var newDiesel = sharedPrefs.getFloat("price_diesel", 273.40f)
            var newOctane = sharedPrefs.getFloat("price_high_octane", 295.00f)
            var newLpg = oldLpg
            var sourceName = "Official Fuel Market API"

            try {
                val trackmateResult = com.example.data.TrackmateFuelApiService.fetchLatestFuelPrices()
                newPetrol = trackmateResult.petrol
                newDiesel = trackmateResult.diesel
                newOctane = trackmateResult.highOctane
                newLpg = trackmateResult.lpgGas
                sourceName = trackmateResult.source
            } catch (e: Exception) {
                e.printStackTrace()
                // Secondary Gemini fallback
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                if (apiKey.isNotBlank()) {
                    try {
                        val promptText = "Query current July 2026 OGRA Pakistan petrol and LPG gas prices in PKR per liter and per KG. Respond in raw JSON format only with keys: petrol, diesel, high_octane, lpg_gas, source."
                        val jsonPayload = JSONObject().apply {
                            put("contents", org.json.JSONArray().put(
                                JSONObject().apply {
                                    put("parts", org.json.JSONArray().put(
                                        JSONObject().apply { put("text", promptText) }
                                    ))
                                }
                            ))
                        }

                        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
                            .post(requestBody)
                            .build()

                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val responseStr = response.body?.string() ?: ""
                            val jsonResp = JSONObject(responseStr)
                            val candidates = jsonResp.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                                val text = parts?.optJSONObject(0)?.optString("text") ?: ""
                                val cleanedText = text.replace("```json", "").replace("```", "").trim()
                                if (cleanedText.startsWith("{")) {
                                    val pricesObj = JSONObject(cleanedText)
                                    newPetrol = pricesObj.optDouble("petrol", oldPetrol.toDouble()).toFloat()
                                    newDiesel = pricesObj.optDouble("diesel", newDiesel.toDouble()).toFloat()
                                    newOctane = pricesObj.optDouble("high_octane", newOctane.toDouble()).toFloat()
                                    newLpg = pricesObj.optDouble("lpg_gas", oldLpg.toDouble()).toFloat()
                                    sourceName = pricesObj.optString("source", "OGRA Pakistan / Live Market")
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }

            // Save updated prices to SharedPreferences
            sharedPrefs.edit()
                .putFloat("price_petrol", newPetrol)
                .putFloat("price_diesel", newDiesel)
                .putFloat("price_high_octane", newOctane)
                .putFloat("price_lpg_gas", newLpg)
                .putLong("price_last_updated", System.currentTimeMillis())
                .putString("price_source", sourceName)
                .apply()

            val db = AppDatabase.getDatabase(appContext)

            // Check admin notification permission and schedule interval settings
            val notifPrefs = appContext.getSharedPreferences("zyphuel_notification_settings", Context.MODE_PRIVATE)
            val intervalHours = notifPrefs.getInt("price_notification_interval_hours", 4)
            val priceNotifsAllowed = notifPrefs.getBoolean("price_notifications_allowed", true)

            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val systemNotifsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationManager.areNotificationsEnabled()
            } else true

            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

            val allowSending = priceNotifsAllowed && systemNotifsEnabled

            val title = "⛽ Real-time Fuel Price Update"
            val message = "Latest Rates in Pakistan: Petrol: Rs. $newPetrol/L | LPG Gas: Rs. $newLpg/KG | High Octane: Rs. $newOctane/L ($sourceName)"

            if (allowSending) {
                notifPrefs.edit().putString("last_sent_price_notif_date", todayDateStr).apply()

                db.notificationDao().insertNotification(
                    NotificationEntity(
                        title = title,
                        message = message,
                        targetRole = "all",
                        timestamp = System.currentTimeMillis()
                    )
                )

                sendSystemNotification(title, message)
            } else {
                com.example.util.DebugLogger.i("FuelPriceWorker", "Price update notification suppressed. Allowed: $priceNotifsAllowed, OS Enabled: $systemNotifsEnabled, Interval: ${intervalHours}h")
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendSystemNotification(title: String, message: String) {
        val channelId = com.example.util.UnifiedAssetManager.NOTIFICATION_CHANNEL_ID
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                com.example.util.UnifiedAssetManager.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic real-time fuel price updates every 4 hours in Pakistan"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(appContext, com.example.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            appContext,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(appContext.resources, com.example.R.drawable.icon)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(com.example.util.UnifiedAssetManager.NOTIFICATION_SMALL_ICON)
            .setColor(androidx.core.content.ContextCompat.getColor(appContext, com.example.R.color.primary))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        try {
            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
            com.example.util.DebugLogger.i("FuelPriceWorker", "System notification posted successfully: $title")
        } catch (e: Exception) {
            com.example.util.DebugLogger.e("FuelPriceWorker", "Failed to post system notification", e)
        }
    }

    companion object {
        fun schedulePeriodicPriceWork(context: Context, intervalHours: Int = 4, isEnabled: Boolean = true) {
            val workManager = WorkManager.getInstance(context)
            if (!isEnabled) {
                workManager.cancelUniqueWork("ZyphuelFuelPriceUpdateWork")
                return
            }

            val safeInterval = intervalHours.coerceAtLeast(1)
            val workRequest = PeriodicWorkRequestBuilder<FuelPriceWorker>(safeInterval.toLong(), TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                "ZyphuelFuelPriceUpdateWork",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                workRequest
            )
        }

        fun triggerImmediatePriceWork(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<FuelPriceWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
        }
    }
}
