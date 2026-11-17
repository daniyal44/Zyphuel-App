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
            // Use same keys as MainViewModel: "petrol", "diesel", "high_octane"
            // Default to current OGRA rates so first-launch never shows 0
            val oldPetrol = sharedPrefs.getFloat("petrol", 335.00f)

            var newPetrol = if (oldPetrol > 0f) oldPetrol else 335.00f
            var newDiesel = sharedPrefs.getFloat("diesel", 336.00f).let { if (it > 0f) it else 336.00f }
            var newOctane = sharedPrefs.getFloat("high_octane", 350.00f).let { if (it > 0f) it else 350.00f }
            var sourceName = "Official Fuel Market API"

            try {
                val trackmateResult = com.example.data.TrackmateFuelApiService.fetchLatestFuelPrices()
                newPetrol = trackmateResult.petrol
                newDiesel = trackmateResult.diesel
                newOctane = trackmateResult.highOctane
                sourceName = trackmateResult.source
            } catch (e: Exception) {
                e.printStackTrace()
                // Secondary Gemini fallback
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    try {
                        val promptText = "Get the latest OGRA Pakistan official consumer fuel retail rates in Pakistani Rupees (PKR) per liter for: 1) Petrol (Motor Spirit), 2) Diesel (High Speed Diesel/HSD), 3) High-Octane (HOBC/RON 97). Return ONLY a JSON object with keys: petrol, diesel, high_octane, source. No markdown, no explanation."
                        val jsonPayload = JSONObject().apply {
                            put("contents", org.json.JSONArray().put(
                                JSONObject().apply {
                                    put("parts", org.json.JSONArray().put(
                                        JSONObject().apply { put("text", promptText) }
                                    ))
                                }
                            ))
                            // Enable Google Search grounding for real-time data
                            put("tools", org.json.JSONArray().put(
                                JSONObject().apply {
                                    put("googleSearch", JSONObject())
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
                                    sourceName = pricesObj.optString("source", "OGRA Pakistan / Live Market")
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }

            // Save updated prices to SharedPreferences (same keys as MainViewModel)
            sharedPrefs.edit()
                .putFloat("petrol", newPetrol)
                .putFloat("diesel", newDiesel)
                .putFloat("high_octane", newOctane)
                .putLong("price_last_updated", System.currentTimeMillis())
                .putString("price_source", sourceName)
                .apply()

            // Push to Firestore so the ViewModel's real-time listener picks up the update
            // and ALL connected devices sync immediately — not just on next app restart.
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                firestore.collection("system_config").document("fuel_rates").set(
                    mapOf(
                        "petrol" to newPetrol.toDouble(),
                        "diesel" to newDiesel.toDouble(),
                        "highOctane" to newOctane.toDouble(),
                        "updatedAt" to System.currentTimeMillis(),
                        "source" to sourceName
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                com.example.util.DebugLogger.i("FuelPriceWorker", "Pushed prices to Firestore: petrol=$newPetrol diesel=$newDiesel octane=$newOctane ($sourceName)")
            } catch (e: Exception) {
                com.example.util.DebugLogger.w("FuelPriceWorker", "Firestore push failed (prices saved locally): ${e.message}")
            }

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

            // Detect price changes for notification
            val petrolChanged = kotlin.math.abs(newPetrol - oldPetrol) > 0.01f
            val dieselOld = sharedPrefs.getFloat("diesel", 336.00f)
            val octaneOld = sharedPrefs.getFloat("high_octane", 350.00f)
            val dieselChanged = kotlin.math.abs(newDiesel - dieselOld) > 0.01f
            val octaneChanged = kotlin.math.abs(newOctane - octaneOld) > 0.01f
            val anyPriceChanged = petrolChanged || dieselChanged || octaneChanged

            val title = if (anyPriceChanged) "⛽ Fuel Prices Updated!" else "⛽ Pakistan Fuel Prices Today"
            val displaySource = when {
                sourceName.contains("OilPrices", ignoreCase = true) -> "OGRA Official"
                sourceName.contains("TrackMate", ignoreCase = true) -> "Official Market"
                sourceName.contains("Google", ignoreCase = true) -> "Live Market"
                sourceName.contains("HamariWeb", ignoreCase = true) -> "OGRA Official"
                sourceName.contains("GlobalPetrol", ignoreCase = true) -> "OGRA Official"
                sourceName.contains("PSO", ignoreCase = true) -> "PSO Official"
                sourceName.contains("Shell", ignoreCase = true) -> "Shell Official"
                sourceName.contains("Gemini", ignoreCase = true) || sourceName.contains("OGRA", ignoreCase = true) -> "OGRA Official"
                sourceName.contains("API", ignoreCase = true) -> "Official Rates"
                else -> sourceName
            }

            // Format prices with 2 decimal places
            val petrolStr = String.format("%.2f", newPetrol)
            val dieselStr = String.format("%.2f", newDiesel)
            val octaneStr = String.format("%.2f", newOctane)

            val message = if (anyPriceChanged) {
                val changes = mutableListOf<String>()
                if (petrolChanged) {
                    val diff = newPetrol - oldPetrol
                    val arrow = if (diff > 0) "↑" else "↓"
                    changes.add("Petrol: Rs. $petrolStr/L $arrow")
                }
                if (dieselChanged) {
                    val diff = newDiesel - dieselOld
                    val arrow = if (diff > 0) "↑" else "↓"
                    changes.add("Diesel: Rs. $dieselStr/L $arrow")
                }
                if (octaneChanged) {
                    val diff = newOctane - octaneOld
                    val arrow = if (diff > 0) "↑" else "↓"
                    changes.add("Hi-Octane: Rs. $octaneStr/L $arrow")
                }
                "${changes.joinToString("  •  ")}  ($displaySource)"
            } else {
                "Petrol: Rs. $petrolStr/L  •  Diesel: Rs. $dieselStr/L  •  Hi-Octane: Rs. $octaneStr/L  ($displaySource)"
            }

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
        com.example.util.ZyphuelNotificationHelper.dispatchModernNotification(
            context = appContext,
            title = title,
            message = message,
            senderName = "Zyphuel Fuel Alert",
            openScreen = "customer_home",
            includeFuelActions = true
        )
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
