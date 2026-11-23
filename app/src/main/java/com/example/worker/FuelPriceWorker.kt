package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.data.AppDatabase
import com.example.data.NotificationEntity
import com.example.data.AuthorizedFuelPriceService
import com.example.security.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.abs

class FuelPriceWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sharedPrefs = appContext.getSharedPreferences("zyphuel_prices", Context.MODE_PRIVATE)
            // Use same keys as MainViewModel: "petrol", "diesel", "high_octane"
            val oldPetrol = sharedPrefs.getFloat("petrol", 327.00f)
            val oldDiesel = sharedPrefs.getFloat("diesel", 360.00f)
            val oldOctane = sharedPrefs.getFloat("high_octane", 350.00f)

            // Fetch authorized website retail rates (https://zyphuel.netlify.app/order)
            val authorizedPrices = AuthorizedFuelPriceService.fetchAuthorizedPrices()
            val newPetrol = authorizedPrices.petrol
            val newDiesel = authorizedPrices.diesel
            val newOctane = authorizedPrices.highOctane
            val sourceName = authorizedPrices.source
            val isLive = authorizedPrices.isLive

            com.example.util.DebugLogger.i("FuelPriceWorker",
                "Authorized website prices fetched: petrol=$newPetrol diesel=$newDiesel octane=$newOctane " +
                "(source=$sourceName, live=$isLive)")

            // Only update if we got valid prices (non-zero)
            if (newPetrol > 0f && newDiesel > 0f && newOctane > 0f) {
                // Save OGRA BASE prices to SharedPreferences (same keys as MainViewModel)
                sharedPrefs.edit()
                    .putFloat("petrol", newPetrol)
                    .putFloat("diesel", newDiesel)
                    .putFloat("high_octane", newOctane)
                    .putLong("price_last_updated", System.currentTimeMillis())
                    .putString("price_source", sourceName)
                    .putBoolean("price_is_live", isLive)
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
                            "source" to sourceName,
                            "isLive" to isLive,
                            "effectiveDate" to (authorizedPrices.effectiveDate ?: "")
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    com.example.util.DebugLogger.i("FuelPriceWorker",
                        "Pushed authorized website prices to Firestore: petrol=$newPetrol diesel=$newDiesel octane=$newOctane ($sourceName)")
                } catch (e: Exception) {
                    com.example.util.DebugLogger.w("FuelPriceWorker",
                        "Firestore push failed (prices saved locally): ${e.message}")
                }
            } else {
                com.example.util.DebugLogger.w("FuelPriceWorker",
                    "Skipping price update — invalid values: petrol=$newPetrol diesel=$newDiesel octane=$newOctane")
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

            // Detect actual price changes
            val petrolChanged = newPetrol > 0f && abs(newPetrol - oldPetrol) > 0.01f
            val dieselChanged = newDiesel > 0f && abs(newDiesel - oldDiesel) > 0.01f
            val octaneChanged = newOctane > 0f && abs(newOctane - oldOctane) > 0.01f
            val anyPriceChanged = petrolChanged || dieselChanged || octaneChanged

            // Apply Rs. 5.00/L pump markup for display
            val pumpMarkup = com.example.util.FeeConstants.PUMP_RATE_MARKUP_FLOAT
            val displayPetrol = newPetrol + pumpMarkup
            val displayDiesel = newDiesel + pumpMarkup
            val displayOctane = newOctane + pumpMarkup

            val title = "⛽ Pakistan Fuel Prices Today"
            val displaySource = if (isLive) "Website (live)" else "Website (last verified)"

            // Format prices with 2 decimal places
            val petrolStr = String.format("%.2f", displayPetrol)
            val dieselStr = String.format("%.2f", displayDiesel)
            val octaneStr = String.format("%.2f", displayOctane)

            val changeNote = if (anyPriceChanged) " 🔄 UPDATED" else ""
            val message = "Petrol: Rs. $petrolStr/L  •  Diesel: Rs. $dieselStr/L  •  Hi-Octane: Rs. $octaneStr/L  ($displaySource)$changeNote"

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
            com.example.util.DebugLogger.e("FuelPriceWorker", "Worker failed: ${e.message}")
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
        private const val WORK_NAME = "ZyphuelFuelPriceUpdateWork"

        /**
         * Schedule periodic OGRA fuel price synchronization.
         * Runs every [intervalHours] hours to check for OGRA price updates.
         * OGRA typically updates prices fortnightly, but we check more frequently
         * to catch updates promptly.
         */
        fun schedulePeriodicPriceWork(context: Context, intervalHours: Int = 4, isEnabled: Boolean = true) {
            val workManager = WorkManager.getInstance(context)
            if (!isEnabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<FuelPriceWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES) // Small delay on first run
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )

            com.example.util.DebugLogger.i("FuelPriceWorker",
                "Periodic OGRA price sync scheduled every ${intervalHours}h")

            // Also trigger an immediate one-time sync
            triggerImmediatePriceWork(context)
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
            com.example.util.DebugLogger.i("FuelPriceWorker", "Immediate OGRA price sync triggered")
        }
    }
}
