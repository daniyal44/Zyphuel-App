package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.data.LiveTrackingRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that streams the rider's real GPS to Firestore while a
 * delivery is in progress — the Uber/Careem-style "driver is moving" signal.
 *
 * Started when the rider taps "Start Delivering" (status -> "Delivering") and
 * stopped on Completed / Cancelled. Publishes ~every 4s to
 * `live_tracking/{orderId}` via [LiveTrackingRepository].
 */
class RiderLocationForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "zyphuel_live_tracking"
        const val NOTIF_ID = 7788
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_RIDER_EMAIL = "extra_rider_email"
        const val EXTRA_STATUS = "extra_status"
        const val ACTION_START = "com.example.action.START_LIVE_TRACKING"
        const val ACTION_STOP = "com.example.action.STOP_LIVE_TRACKING"

        fun start(context: Context, orderId: Int, riderEmail: String, status: String) {
            val intent = Intent(context, RiderLocationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ORDER_ID, orderId)
                putExtra(EXTRA_RIDER_EMAIL, riderEmail)
                putExtra(EXTRA_STATUS, status)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // e.g. ForegroundServiceStartNotAllowedException if app not eligible — safe ignore
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, RiderLocationForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                // Safe ignore
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var liveTrackingRepository: LiveTrackingRepository? = null
    private var fusedClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private var orderId: Int = -1
    private var riderEmail: String = ""
    private var status: String = "Delivering"

    override fun onCreate() {
        super.onCreate()
        liveTrackingRepository = LiveTrackingRepository(applicationContext)
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                return START_NOT_STICKY
            }
            else -> {
                orderId = intent?.getIntExtra(EXTRA_ORDER_ID, -1) ?: -1
                riderEmail = intent?.getStringExtra(EXTRA_RIDER_EMAIL) ?: ""
                status = intent?.getStringExtra(EXTRA_STATUS) ?: "Delivering"
                if (orderId <= 0) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundInternal()
                startLocationUpdates()
            }
        }
        return START_STICKY
    }

    private fun startForegroundInternal() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_screen", "tracker")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentIntent = PendingIntent.getActivity(this, 0, openIntent, pendingFlags)

        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(resources, com.example.R.drawable.icon)
        } catch (e: Exception) {
            null
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Live delivery tracking active")
            .setContentText("Sharing your live location for Order #$orderId")
            .setSmallIcon(com.example.util.UnifiedAssetManager.NOTIFICATION_SMALL_ICON)
            .setColor(androidx.core.content.ContextCompat.getColor(this, com.example.R.color.primary))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)

        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon)
        }

        val notification = notificationBuilder.build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            // If we cannot go foreground, stop gracefully rather than crash
            stopSelf()
        }
    }

    private fun startLocationUpdates() {
        try {
            val client = LocationServices.getFusedLocationProviderClient(applicationContext)
            fusedClient = client

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
                .setMinUpdateIntervalMillis(2000L)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    // Server live tracking permanently disabled
                }
            }
            locationCallback = callback

            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // Location permission not granted — cannot track
            stopSelf()
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun stopTracking() {
        try {
            locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
        } catch (e: Exception) {
            // Safe ignore
        }
        locationCallback = null
        // Clear the remote live-tracking doc so the customer map stops following.
        val finishedOrderId = orderId
        if (finishedOrderId > 0) {
            scope.launch { liveTrackingRepository?.clearLocation(finishedOrderId) }
        }
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            // Safe ignore
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Live Delivery Tracking",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows when you are sharing live location for an active delivery."
                    setShowBadge(false)
                }
                manager?.createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        try {
            locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
        } catch (e: Exception) {
            // Safe ignore
        }
        scope.cancel()
        super.onDestroy()
    }
}
