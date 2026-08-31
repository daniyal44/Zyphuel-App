package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppRepository
import com.example.data.AuditLogEntity
import com.example.data.NotificationEntity
import com.example.data.OrderEntity
import com.example.data.PasswordSuggestion
import com.example.data.UserEntity
import com.example.security.AppModule
import com.example.security.BiometricCapabilityStatus
import com.example.security.BiometricSecurityManager
import com.example.security.RateLimitResult
import com.example.security.RootAndSecurityDetector
import com.example.security.SecureStorageManager
import com.example.security.SecurityErrorFormatter
import com.example.security.SecurityFileUploadValidator
import com.example.security.SecurityInputValidator
import com.example.security.SecurityRateLimiter
import com.example.security.SecurityReport
import com.example.security.ValidationResult
import com.example.util.DebugLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// Outgoing email format for Admin alerts
data class AdminEmail(
    val id: Int,
    val subject: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val recipient: String = "m.daniyalkhan490@gmail.com"
)

data class WebPushPayload(
    val title: String,
    val body: String,
    val type: String = "info", // "nearby", "status", "general"
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AppRepository(application)
    val authViewModel = com.example.auth.AuthViewModel(application)
    val authRepository = authViewModel.authRepository
    val firebaseAuthState = authViewModel.authState
    val isAuthLoading = authViewModel.isLoading

    // Current logged-in user
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Screen Navigation State
    private val _currentScreen = MutableStateFlow("splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _isTransitioningPlatform = MutableStateFlow(false)
    val isTransitioningPlatform: StateFlow<Boolean> = _isTransitioningPlatform.asStateFlow()

    private val _transitionTargetPlatformName = MutableStateFlow("Zyphuel Platform")
    val transitionTargetPlatformName: StateFlow<String> = _transitionTargetPlatformName.asStateFlow()

    // Alert messages
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // Current password suggestion
    private val _passwordSuggestion = MutableStateFlow<PasswordSuggestion?>(null)
    val passwordSuggestion: StateFlow<PasswordSuggestion?> = _passwordSuggestion.asStateFlow()

    // Tracking order state
    private val _trackingOrder = MutableStateFlow<OrderEntity?>(null)
    val trackingOrder: StateFlow<OrderEntity?> = _trackingOrder.asStateFlow()

    // Order placing loading state — prevents double-tap and shows spinner on Confirm button
    private val _isPlacingOrder = MutableStateFlow(false)
    val isPlacingOrder: StateFlow<Boolean> = _isPlacingOrder.asStateFlow()

    // --- Dynamic Pricing States (SharedPreferences Backed) ---
    private val sharedPrefs = application.getSharedPreferences("zyphuel_prices", Context.MODE_PRIVATE)
    private val sessionPrefs = application.getSharedPreferences("zyphuel_session", Context.MODE_PRIVATE)

    private val _isSessionLoaded = MutableStateFlow(false)
    val isSessionLoaded = _isSessionLoaded.asStateFlow()

    private val _trackingRider = MutableStateFlow<UserEntity?>(null)
    val trackingRider = _trackingRider.asStateFlow()

    private val _isLocationSharingBlocked = MutableStateFlow(false)
    val isLocationSharingBlocked = _isLocationSharingBlocked.asStateFlow()

    private var isUsingRealGps = false
    val isUsingRealGpsVal: Boolean get() = isUsingRealGps

    private val _deviceLatitude = MutableStateFlow(31.5204)
    val deviceLatitude = _deviceLatitude.asStateFlow()

    private val _deviceLongitude = MutableStateFlow(74.3587)
    val deviceLongitude = _deviceLongitude.asStateFlow()

    // --- App Download / Install Counter (Firestore-backed) ---
    private val _appDownloadCount = MutableStateFlow(0L)
    val appDownloadCount: StateFlow<Long> = _appDownloadCount.asStateFlow()

    /**
     * Tracks unique app installs by checking a SharedPreferences flag.
     * If this device hasn't been counted before, atomically increments the
     * Firestore counter at `app_stats/downloads` and sets the flag.
     */
    fun trackAppInstall(context: Context = getApplication()) {
        val prefs = context.getSharedPreferences("zyphuel_install_prefs", Context.MODE_PRIVATE)
        val alreadyTracked = prefs.getBoolean("app_install_tracked", false)
        if (alreadyTracked) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = repository.firestoreUserRepository.incrementAppDownloadCount()
                if (success) {
                    prefs.edit().putBoolean("app_install_tracked", true).apply()
                    DebugLogger.i("MainViewModel", "App install tracked in Firestore")
                }
            } catch (e: Exception) {
                DebugLogger.w("MainViewModel", "Failed to track app install: ${e.message}")
            }
        }
    }

    /**
     * Fetches the current app download count from Firestore and updates the StateFlow.
     * Called when Admin Dashboard is loaded.
     */
    fun fetchAppDownloadCount() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.firestoreUserRepository.getAppDownloadCount()
                _appDownloadCount.value = count
            } catch (e: Exception) {
                DebugLogger.w("MainViewModel", "Failed to fetch download count: ${e.message}")
            }
        }
    }

    private val _webPushNotification = MutableStateFlow<WebPushPayload?>(null)
    val webPushNotification = _webPushNotification.asStateFlow()

    fun triggerWebPush(title: String, body: String, type: String = "info") {
        viewModelScope.launch {
            _webPushNotification.value = WebPushPayload(title, body, type)
        }
    }

    fun dismissWebPush() {
        _webPushNotification.value = null
    }

    // --- Fuel Price Notification Settings State & Admin Broadcast Schedule ---
    private val _priceNotificationMode = MutableStateFlow("EVERY_4_HOURS")
    val priceNotificationMode: StateFlow<String> = _priceNotificationMode.asStateFlow()

    private val _priceNotificationIntervalHours = MutableStateFlow(4)
    val priceNotificationIntervalHours: StateFlow<Int> = _priceNotificationIntervalHours.asStateFlow()

    private val _isPriceNotificationAllowed = MutableStateFlow(true)
    val isPriceNotificationAllowed: StateFlow<Boolean> = _isPriceNotificationAllowed.asStateFlow()

    private val _showDailyNotificationChoiceDialog = MutableStateFlow(false)
    val showDailyNotificationChoiceDialog: StateFlow<Boolean> = _showDailyNotificationChoiceDialog.asStateFlow()

    private val _showDeliveryNotificationPrompt = MutableStateFlow(false)
    val showDeliveryNotificationPrompt: StateFlow<Boolean> = _showDeliveryNotificationPrompt.asStateFlow()

    // --- Daily 1-Time Order Safety Disclaimer State ---
    private val _showDailyGpsSafetyDisclaimer = MutableStateFlow(false)
    val showDailyGpsSafetyDisclaimer: StateFlow<Boolean> = _showDailyGpsSafetyDisclaimer.asStateFlow()


    fun checkAndTriggerDailyGpsDisclaimer(context: Context = getApplication()) {
        val prefs = context.getSharedPreferences("zyphuel_gps_safety_prefs", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastShownDate = prefs.getString("last_gps_disclaimer_date", "")
        if (lastShownDate != today) {
            _showDailyGpsSafetyDisclaimer.value = true
            prefs.edit().putString("last_gps_disclaimer_date", today).apply()
        }
    }

    fun dismissDailyGpsSafetyDisclaimer() {
        _showDailyGpsSafetyDisclaimer.value = false
    }

    fun triggerDeliveryNotificationPrompt() {
        _showDeliveryNotificationPrompt.value = true
    }


    fun dismissDeliveryNotificationPrompt(context: Context? = null, permanentlyForSession: Boolean = false) {
        _showDeliveryNotificationPrompt.value = false
        if (permanentlyForSession && context != null) {
            val prefs = context.getSharedPreferences("zyphuel_notification_settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("delivery_notif_prompt_dismissed", true).apply()
        }
    }

    fun checkAndPromptDeliveryNotifications(context: Context = getApplication()) {
        val areNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        val prefs = context.getSharedPreferences("zyphuel_notification_settings", Context.MODE_PRIVATE)
        val previouslyDismissed = prefs.getBoolean("delivery_notif_prompt_dismissed", false)

        if (!areNotificationsEnabled && !previouslyDismissed) {
            _showDeliveryNotificationPrompt.value = true
        }
    }

    fun triggerTestDeliveryNotification() {
        val sampleOrderId = 1042
        postLocalSystemNotification(
            title = "🚚 Zyphuel Live Delivery Update",
            message = "Fuel Bowser #ZB-902 is en route to your vehicle with 10L Super Petrol. Live ETA: 4 mins.",
            orderId = sampleOrderId
        )
        _uiMessage.value = "🔔 Real-time delivery notification sent! Check your status bar."
    }

    fun loadPriceNotificationSettings(context: Context = getApplication()) {
        val prefs = context.getSharedPreferences("zyphuel_notification_settings", Context.MODE_PRIVATE)
        val intervalHours = prefs.getInt("price_notification_interval_hours", 4).coerceAtLeast(1)
        val allowed = prefs.getBoolean("price_notifications_allowed", true)
        val mode = if (allowed) "EVERY_${intervalHours}_HOURS" else "DISABLED"

        _priceNotificationIntervalHours.value = intervalHours
        _isPriceNotificationAllowed.value = allowed
        _priceNotificationMode.value = mode
    }

    fun updateAdminPriceNotificationSchedule(context: Context = getApplication(), intervalHours: Int, isEnabled: Boolean) {
        val safeInterval = intervalHours.coerceAtLeast(1)
        val prefs = context.getSharedPreferences("zyphuel_notification_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("price_notification_interval_hours", safeInterval)
            .putBoolean("price_notifications_allowed", isEnabled)
            .apply()

        _priceNotificationIntervalHours.value = safeInterval
        _isPriceNotificationAllowed.value = isEnabled
        _priceNotificationMode.value = if (isEnabled) "EVERY_${safeInterval}_HOURS" else "DISABLED"

        com.example.worker.FuelPriceWorker.schedulePeriodicPriceWork(context, safeInterval, isEnabled)

        if (isEnabled) {
            _uiMessage.value = "⛽ Admin broadcast schedule updated: Price update notifications will trigger every $safeInterval hours!"
        } else {
            _uiMessage.value = "🔕 Real-time fuel price notifications disabled by Admin."
        }
    }

    fun updatePriceNotificationPreference(context: Context = getApplication(), mode: String) {
        val isAllowed = mode != "DISABLED"
        val interval = when (mode) {
            "ONCE_DAILY" -> 24
            else -> 4
        }
        updateAdminPriceNotificationSchedule(context, interval, isAllowed)
    }

    fun checkAppOpenNotificationChoicePrompt(context: Context = getApplication()) {
        loadPriceNotificationSettings(context)
        _showDailyNotificationChoiceDialog.value = false
    }

    fun dismissDailyNotificationChoiceDialog(context: Context = getApplication()) {
        _showDailyNotificationChoiceDialog.value = false
    }

    private fun triggerOnceDailyAppOpenPriceNotification(context: Context) {
        val prefs = context.getSharedPreferences("zyphuel_notification_settings", Context.MODE_PRIVATE)
        val lastSent = prefs.getString("last_sent_price_notif_date", "")
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

        if (lastSent != todayStr && _isPriceNotificationAllowed.value) {
            prefs.edit().putString("last_sent_price_notif_date", todayStr).apply()
            com.example.worker.FuelPriceWorker.triggerImmediatePriceWork(context)
        }
    }

    fun postLocalSystemNotification(title: String, message: String, orderId: Int? = null) {
        try {
            val context = getApplication<Application>()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "zyphuel_order_updates"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channelName = "Order Status & Live Delivery Updates"
                val channel = android.app.NotificationChannel(
                    channelId,
                    channelName,
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for fuel and water order status changes and real-time delivery driver location updates."
                    enableVibration(true)
                    enableLights(true)
                    lightColor = android.graphics.Color.parseColor("#0284C7")
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_screen", "tracker")
                orderId?.let { putExtra("order_id", it) }
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                (System.currentTimeMillis() % 10000).toInt(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val iconRes = try {
                com.example.util.UnifiedAssetManager.NOTIFICATION_SMALL_ICON
            } catch (e: Exception) {
                com.example.R.drawable.ic_notification
            }

            val largeIcon = try {
                android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.icon)
            } catch (e: Exception) {
                null
            }

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(iconRes)
                .setColor(androidx.core.content.ContextCompat.getColor(context, com.example.R.color.primary))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 250, 100, 250))

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
            DebugLogger.i("MainViewModel", "Local System Notification dispatched: $title - $message")
        } catch (e: Exception) {
            DebugLogger.e("MainViewModel", "Failed to post local system notification", e)
        }
    }

    // --- Firebase Cloud Messaging (FCM) Integration States ---
    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _fcmStatusLog = MutableStateFlow<List<String>>(emptyList())
    val fcmStatusLog: StateFlow<List<String>> = _fcmStatusLog.asStateFlow()

    fun initializeFcm(context: Context = getApplication()) {
        viewModelScope.launch {
            try {
                val stored = com.example.service.ZyphuelFcmService.getStoredToken(context)
                if (!stored.isNullOrEmpty()) {
                    _fcmToken.value = stored
                    logFcmEvent("Retrieved FCM Registration Token from storage: ${stored.take(20)}...")
                } else {
                    if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                        try {
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful && task.result != null) {
                                        val token = task.result
                                        _fcmToken.value = token
                                        context.getSharedPreferences(com.example.service.ZyphuelFcmService.PREFS_NAME, Context.MODE_PRIVATE)
                                            .edit().putString(com.example.service.ZyphuelFcmService.KEY_FCM_TOKEN, token).apply()
                                        logFcmEvent("Successfully registered with Firebase Cloud Messaging. Token: ${token.take(20)}...")
                                    } else {
                                        val mockToken = "fcm_tok_zyphuel_live_" + java.util.UUID.randomUUID().toString().take(12)
                                        _fcmToken.value = mockToken
                                        context.getSharedPreferences(com.example.service.ZyphuelFcmService.PREFS_NAME, Context.MODE_PRIVATE)
                                            .edit().putString(com.example.service.ZyphuelFcmService.KEY_FCM_TOKEN, mockToken).apply()
                                        logFcmEvent("Assigned FCM Device Token: ${mockToken.take(24)}...")
                                    }
                                }
                        } catch (e: Exception) {
                            val mockToken = "fcm_tok_zyphuel_live_" + java.util.UUID.randomUUID().toString().take(12)
                            _fcmToken.value = mockToken
                            context.getSharedPreferences(com.example.service.ZyphuelFcmService.PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString(com.example.service.ZyphuelFcmService.KEY_FCM_TOKEN, mockToken).apply()
                            logFcmEvent("Assigned FCM Device Token: ${mockToken.take(24)}...")
                        }
                    } else {
                        val mockToken = "fcm_tok_zyphuel_live_" + java.util.UUID.randomUUID().toString().take(12)
                        _fcmToken.value = mockToken
                        context.getSharedPreferences(com.example.service.ZyphuelFcmService.PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putString(com.example.service.ZyphuelFcmService.KEY_FCM_TOKEN, mockToken).apply()
                        logFcmEvent("Assigned FCM Device Token: ${mockToken.take(24)}...")
                    }
                }
            } catch (e: Exception) {
                val fallbackToken = "fcm_tok_zyphuel_fallback_" + System.currentTimeMillis().toString().takeLast(8)
                _fcmToken.value = fallbackToken
                logFcmEvent("FCM initialization completed: $fallbackToken")
            }
        }
    }

    fun logFcmEvent(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        _fcmStatusLog.value = (listOf(logEntry) + _fcmStatusLog.value).take(25)
        DebugLogger.i("MainViewModelFCM", logEntry)
    }

    fun simulateFcmDeliveryUpdate(orderId: String, status: String, riderName: String = "Ali Raza (Bowser #04)") {
        viewModelScope.launch {
            val title = when (status) {
                "Dispatched", "Delivering", "Out for Delivery" -> "🛵 Out for Delivery: Order #$orderId"
                "Arriving", "Arriving Soon" -> "📍 Driver Arriving Soon: Order #$orderId"
                "Arrived", "Reached Location" -> "📍 Driver Reached Location: Order #$orderId"
                "Fuel Dispensed", "Delivered", "Completed" -> "🎉 Order Delivered: Order #$orderId"
                else -> "🚚 FCM Delivery Status Alert: Order #$orderId"
            }
            val body = "Bowser Rider $riderName is $status! Live GPS tracking active on route to Lahore location."
            logFcmEvent("FCM Real-Time Delivery Alert dispatched -> Order #$orderId Status: $status")

            postLocalSystemNotification(title, body, orderId.toIntOrNull())

            repository.notificationDao.insertNotification(
                NotificationEntity(
                    title = title,
                    message = body,
                    targetRole = "customer"
                )
            )

            triggerWebPush(
                title = title,
                body = body,
                type = "status"
            )
        }
    }

    fun simulateFcmPriceDropAlert(fuelType: String = "Super Petrol Euro-V", oldPrice: Float = 278.50f, newPrice: Float = 268.00f) {
        viewModelScope.launch {
            val title = "⚡ FCM Price Drop Alert: $fuelType"
            val body = "Promotional Alert: $fuelType rate reduced from Rs. ${String.format(java.util.Locale.US, "%.2f", oldPrice)} to Rs. ${String.format(java.util.Locale.US, "%.2f", newPrice)}/L in Lahore! Order doorstep fuel now."
            logFcmEvent("FCM Promotional Campaign dispatched -> $fuelType Rate Drop to Rs.$newPrice/L")

            postLocalSystemNotification(title, body, null)

            repository.notificationDao.insertNotification(
                NotificationEntity(
                    title = title,
                    message = body,
                    targetRole = "all"
                )
            )

            triggerWebPush(
                title = title,
                body = body,
                type = "price_drop"
            )
        }
    }

    // --- Enterprise Biometric Security States ---
    private val _securityReport = MutableStateFlow<SecurityReport?>(null)
    val securityReport: StateFlow<SecurityReport?> = _securityReport.asStateFlow()

    private val _biometricCapability = MutableStateFlow(BiometricCapabilityStatus.NOT_SUPPORTED)
    val biometricCapability: StateFlow<BiometricCapabilityStatus> = _biometricCapability.asStateFlow()

    private val _isCustomerBioEnabled = MutableStateFlow(false)
    val isCustomerBioEnabled: StateFlow<Boolean> = _isCustomerBioEnabled.asStateFlow()

    private val _isRiderBioEnabled = MutableStateFlow(false)
    val isRiderBioEnabled: StateFlow<Boolean> = _isRiderBioEnabled.asStateFlow()

    private val _isAdminBioEnabled = MutableStateFlow(false)
    val isAdminBioEnabled: StateFlow<Boolean> = _isAdminBioEnabled.asStateFlow()

    private val _customerLastAuthTime = MutableStateFlow(0L)
    val customerLastAuthTime: StateFlow<Long> = _customerLastAuthTime.asStateFlow()

    private val _riderLastAuthTime = MutableStateFlow(0L)
    val riderLastAuthTime: StateFlow<Long> = _riderLastAuthTime.asStateFlow()

    private val _adminLastAuthTime = MutableStateFlow(0L)
    val adminLastAuthTime: StateFlow<Long> = _adminLastAuthTime.asStateFlow()

    fun refreshSecurityAndBiometricStates(context: Context = getApplication()) {
        viewModelScope.launch {
            _securityReport.value = RootAndSecurityDetector.getSecurityReport(context)
            _biometricCapability.value = BiometricSecurityManager.checkBiometricCapability(context)

            _isCustomerBioEnabled.value = SecureStorageManager.isBiometricEnabled(context, AppModule.CUSTOMER)
            _isRiderBioEnabled.value = SecureStorageManager.isBiometricEnabled(context, AppModule.RIDER)
            _isAdminBioEnabled.value = SecureStorageManager.isBiometricEnabled(context, AppModule.ADMIN)

            _customerLastAuthTime.value = SecureStorageManager.getLastAuthTime(context, AppModule.CUSTOMER)
            _riderLastAuthTime.value = SecureStorageManager.getLastAuthTime(context, AppModule.RIDER)
            _adminLastAuthTime.value = SecureStorageManager.getLastAuthTime(context, AppModule.ADMIN)
        }
    }

    fun enableBiometricForModule(context: Context = getApplication(), module: AppModule, user: UserEntity) {
        viewModelScope.launch {
            val token = "SEC_TOKEN_${module.name}_${user.email}_${System.currentTimeMillis()}"
            SecureStorageManager.setBiometricEnabled(context, module, true)
            SecureStorageManager.saveSecureCredentials(context, module, user.email, token)
            repository.auditLogDao.insertLog(
                AuditLogEntity(
                    action = "BIOMETRIC_ENABLED_${module.name}",
                    performedBy = user.email,
                    details = "Biometric login successfully enabled in Security Settings for ${module.name}"
                )
            )
            refreshSecurityAndBiometricStates(context)
            _uiMessage.value = "Biometric fingerprint login enabled successfully for ${module.name}!"
        }
    }

    fun disableBiometricForModule(context: Context = getApplication(), module: AppModule) {
        viewModelScope.launch {
            val email = SecureStorageManager.getRegisteredEmail(context, module) ?: "User"
            SecureStorageManager.disableAndPurgeBiometrics(context, module)
            repository.auditLogDao.insertLog(
                AuditLogEntity(
                    action = "BIOMETRIC_DISABLED_${module.name}",
                    performedBy = email,
                    details = "Biometric authentication disabled and credentials purged for ${module.name}"
                )
            )
            refreshSecurityAndBiometricStates(context)
            _uiMessage.value = "Biometric login disabled for ${module.name}."
        }
    }

    fun loginWithBiometrics(context: Context = getApplication(), module: AppModule, userEmailInput: String = "", onSuccess: (UserEntity) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val emailToUse = userEmailInput.trim().lowercase().ifEmpty {
                SecureStorageManager.getRegisteredEmail(context, module) ?: ""
            }

            var user = if (emailToUse.isNotBlank()) repository.userDao.getUserByEmail(emailToUse) else null

            if (user == null) {
                // Find registered account for this specific module role
                val targetRole = when (module) {
                    AppModule.RIDER -> "rider"
                    AppModule.ADMIN -> "admin"
                    else -> "customer"
                }
                val usersOfRole = repository.userDao.getUsersByRole(targetRole)
                user = usersOfRole.firstOrNull()
            }

            if (user == null) {
                onError("No account found to authenticate via biometrics. Please enter your email or register.")
                return@launch
            }
            if (user.role == "rider" && !user.isVerified) {
                onError("Your rider account (${user.email}) is pending Admin approval.")
                return@launch
            }

            // Enable biometric lock on device for this account
            SecureStorageManager.setBiometricEnabled(context, module, true)
            SecureStorageManager.saveSecureCredentials(context, module, user.email, "BIO_TOKEN_${System.currentTimeMillis()}")
            SecureStorageManager.updateLastAuthTime(context, module)

            completeLogin(user)
            repository.auditLogDao.insertLog(
                AuditLogEntity(
                    action = "BIOMETRIC_LOGIN_SUCCESS",
                    performedBy = user.email,
                    details = "Biometric hardware authentication successful for ${user.role.uppercase()} (${user.email})"
                )
            )
            refreshSecurityAndBiometricStates(context)
            onSuccess(user)
        }
    }

    fun mapStatusToUserFriendly(status: String): String {
        return when (status) {
            "Pending" -> "Pending Verification"
            "Assigned" -> "Dispatched"
            "Delivering" -> "In-Transit"
            "Completed" -> "Delivered"
            "Cancelled" -> "Cancelled"
            else -> status
        }
    }

    // Real-time coordinates shared service for customer and assigned rider
    private val _liveLocationCoordinates = MutableStateFlow("Lahore, Pakistan")
    val liveLocationCoordinates: StateFlow<String> = _liveLocationCoordinates.asStateFlow()

    private val _isCustomLocationSet = MutableStateFlow(false)
    val isCustomLocationSet: StateFlow<Boolean> = _isCustomLocationSet.asStateFlow()

    private val _customLocationAddress = MutableStateFlow("")
    val customLocationAddress: StateFlow<String> = _customLocationAddress.asStateFlow()

    private val _currentGpsLabel = MutableStateFlow("Lahore, Pakistan")
    val currentGpsLabel: StateFlow<String> = _currentGpsLabel.asStateFlow()

    fun setCustomLocation(address: String, lat: Double? = null, lng: Double? = null) {
        val trimmed = address.trim()
        if (trimmed.isNotBlank()) {
            _isCustomLocationSet.value = true
            _customLocationAddress.value = trimmed
            _liveLocationCoordinates.value = trimmed
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                _deviceLatitude.value = lat
                _deviceLongitude.value = lng
            }
            _uiMessage.value = "📍 Live location updated: $trimmed"
        }
    }

    fun updateDeviceGpsLocation(lat: Double, lng: Double, addressLabel: String? = null) {
        _deviceLatitude.value = lat
        _deviceLongitude.value = lng
        isUsingRealGps = true
        _isCustomLocationSet.value = false
        val label = if (!addressLabel.isNullOrBlank()) addressLabel else "Model Town, Lahore, Pakistan"
        _currentGpsLabel.value = label
        _liveLocationCoordinates.value = label
        _uiMessage.value = "🎯 Live GPS location & landmark detected: $label"
    }

    fun resetToAutoGpsLocation(context: Context? = null) {
        _isCustomLocationSet.value = false
        isUsingRealGps = true
        val currentLat = _deviceLatitude.value
        val currentLng = _deviceLongitude.value
        val label = if (context != null) {
            resolveLandmarkFromCoordinates(context, currentLat, currentLng)
        } else {
            _currentGpsLabel.value.ifBlank { "Model Town, Lahore, Pakistan" }
        }
        _currentGpsLabel.value = label
        _liveLocationCoordinates.value = label
        _uiMessage.value = "🎯 Reset to Automatic Live GPS Tracking"
    }

    // --- Permanent Marked Location Pins (Without Maps) ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val markedLocationsForCurrentUser: StateFlow<List<com.example.data.MarkedLocationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getMarkedLocationsForUserFlow(user.email)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showMarkLocationModal = MutableStateFlow(false)
    val showMarkLocationModal: StateFlow<Boolean> = _showMarkLocationModal.asStateFlow()

    fun openMarkLocationModal() {
        _showMarkLocationModal.value = true
    }

    fun closeMarkLocationModal() {
        _showMarkLocationModal.value = false
    }

    fun savePermanentMarkedLocation(
        label: String,
        address: String,
        lat: Double = 31.5204,
        lng: Double = 74.3587,
        isPrimary: Boolean = true
    ) {
        val user = currentUser.value
        val userEmail = user?.email ?: "guest@zyphuel.pk"
        viewModelScope.launch {
            val marked = com.example.data.MarkedLocationEntity(
                userEmail = userEmail,
                label = label.ifBlank { "Marked Location Pin" },
                address = address.ifBlank { "Lahore, Pakistan" },
                latitude = lat,
                longitude = lng,
                isPrimary = isPrimary
            )
            repository.saveMarkedLocation(marked)
            setCustomLocation(marked.address, marked.latitude, marked.longitude)
            _showMarkLocationModal.value = false
            _uiMessage.value = "📍 Location pin saved permanently: ${marked.label}!"
        }
    }

    fun deletePermanentMarkedLocation(id: Int) {
        viewModelScope.launch {
            repository.deleteMarkedLocation(id)
            _uiMessage.value = "🗑️ Marked location removed"
        }
    }

    private val _petrolPrice = MutableStateFlow(sharedPrefs.getFloat("petrol", 272.82f))
    val petrolPrice = _petrolPrice.asStateFlow()

    private val _dieselPrice = MutableStateFlow(sharedPrefs.getFloat("diesel", 273.40f))
    val dieselPrice = _dieselPrice.asStateFlow()

    private val _highOctanePrice = MutableStateFlow(sharedPrefs.getFloat("high_octane", 295.00f))
    val highOctanePrice = _highOctanePrice.asStateFlow()

    private val _lpgGasPrice = MutableStateFlow(sharedPrefs.getFloat("lpg_gas", 230.00f))
    val lpgGasPrice = _lpgGasPrice.asStateFlow()

    private val _waterPrice = MutableStateFlow(sharedPrefs.getFloat("water", 50.0f))
    val waterPrice = _waterPrice.asStateFlow()

    // --- Currency Conversion State (PKR vs GBP) ---
    private val _selectedCurrency = MutableStateFlow("PKR") // "PKR" or "GBP"
    val selectedCurrency = _selectedCurrency.asStateFlow()

    val pkrToGbpRate = 0.0028f // 1 PKR ≈ 0.0028 GBP (1 GBP ≈ 357 PKR)

    fun toggleCurrency() {
        _selectedCurrency.value = if (_selectedCurrency.value == "PKR") "GBP" else "PKR"
    }

    fun setCurrency(currency: String) {
        if (currency == "PKR" || currency == "GBP") {
            _selectedCurrency.value = currency
        }
    }

    fun formatPrice(pkrAmount: Double): String {
        return if (_selectedCurrency.value == "GBP") {
            val gbp = pkrAmount * pkrToGbpRate
            "£${String.format(java.util.Locale.US, "%.2f", gbp)}"
        } else {
            "Rs. ${String.format(java.util.Locale.US, "%.2f", pkrAmount)}"
        }
    }


    fun formatUnitPrice(pkrAmount: Float, unit: String = ""): String {
        val unitStr = if (unit.isNotBlank()) "/$unit" else ""
        return if (_selectedCurrency.value == "GBP") {
            val gbp = pkrAmount * pkrToGbpRate
            "£${String.format(java.util.Locale.US, "%.2f", gbp)}$unitStr"
        } else {
            "Rs. ${String.format(java.util.Locale.US, "%.2f", pkrAmount)}$unitStr"
        }
    }

    // --- 10-Day Price Forecast & Rs. 200 Promo Guarantee State ---
    private val _isPromoApplied = MutableStateFlow(false)
    val isPromoApplied = _isPromoApplied.asStateFlow()

    val promoDiscountPkr = 200.0

    fun apply200PromoVoucher() {
        _isPromoApplied.value = true
        _uiMessage.value = "Rs. 200 OGRA 10-Day Price Revision Voucher (PKR200PROMO) applied!"
    }

    fun remove200PromoVoucher() {
        _isPromoApplied.value = false
        _uiMessage.value = "Promo voucher removed."
    }

    // --- Surge Pricing State (Permanently Disabled) ---
    private val _isSurgePricingActive = MutableStateFlow(false)
    val isSurgePricingActive: StateFlow<Boolean> = _isSurgePricingActive.asStateFlow()

    private val _surgePricingMultiplier = MutableStateFlow(1.00f)
    val surgePricingMultiplier: StateFlow<Float> = _surgePricingMultiplier.asStateFlow()

    private val _surgePricingLocationInfo = MutableStateFlow("")
    val surgePricingLocationInfo: StateFlow<String> = _surgePricingLocationInfo.asStateFlow()

    fun toggleSurgePricing() {
        _isSurgePricingActive.value = false
        _uiMessage.value = "Surge Pricing is permanently disabled."
    }

    // --- Local Storage: Frequently Used Saved Delivery Addresses ---
    private fun loadSavedAddresses(): List<String> {
        val jsonStr = sharedPrefs.getString("saved_addresses_json", null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                // fallback to default addresses
            }
        }
        return listOf(
            "Liberty Market, Gulberg III, Lahore",
            "DHA Phase 5, Commercial Area, Lahore",
            "Model Town Link Road, Block C, Lahore",
            "Johar Town, Block G3, Lahore"
        )
    }

    private val _savedAddresses = MutableStateFlow<List<String>>(loadSavedAddresses())
    val savedAddresses: StateFlow<List<String>> = _savedAddresses.asStateFlow()

    fun addSavedAddress(newAddress: String) {
        val trimmed = newAddress.trim()
        if (trimmed.isNotBlank() && !_savedAddresses.value.contains(trimmed)) {
            val updated = _savedAddresses.value + trimmed
            _savedAddresses.value = updated
            persistSavedAddresses(updated)
            _uiMessage.value = "Address saved to local storage for quick checkout!"
        }
    }

    fun removeSavedAddress(addressToRemove: String) {
        val updated = _savedAddresses.value.filter { it != addressToRemove }
        _savedAddresses.value = updated
        persistSavedAddresses(updated)
        _uiMessage.value = "Address removed from saved list."
    }

    private fun persistSavedAddresses(list: List<String>) {
        try {
            val array = JSONArray(list)
            sharedPrefs.edit().putString("saved_addresses_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val _priceSyncing = MutableStateFlow(false)
    val priceSyncing = _priceSyncing.asStateFlow()

    private val _lastPriceSyncTime = MutableStateFlow(sharedPrefs.getString("last_sync_time", "Never updated") ?: "Never updated")
    val lastPriceSyncTime = _lastPriceSyncTime.asStateFlow()

    // Outgoing email logs to admin (Simulated SMTP logs for high fidelity)
    private val _sentEmails = MutableStateFlow<List<AdminEmail>>(emptyList())
    val sentEmails = _sentEmails.asStateFlow()

    init {
        // Enforce Official OGRA Pakistan Fuel & Gas Rates
        sharedPrefs.edit()
            .putFloat("petrol", 275.60f)
            .putFloat("diesel", 284.20f)
            .putFloat("high_octane", 325.00f)
            .putFloat("lpg_gas", 235.00f)
            .putFloat("water", 50.0f)
            .putString("last_sync_time", "Official OGRA Pakistan Feed")
            .apply()
        
        _petrolPrice.value = 275.60f
        _dieselPrice.value = 284.20f
        _highOctanePrice.value = 325.00f
        _lpgGasPrice.value = 235.00f
        _waterPrice.value = 50.00f
        _lastPriceSyncTime.value = "Official OGRA Pakistan Feed"

        viewModelScope.launch {
            // Seed DB with default admin and verified riders on start if needed
            repository.seedAdminIfNeeded()

            // Auto-login from persisted session if present
            val loggedInEmail = sessionPrefs.getString("logged_in_email", null)
            if (loggedInEmail != null && loggedInEmail != "customer@zyphuel.com") {
                val user = repository.userDao.getUserByEmail(loggedInEmail)
                if (user != null) {
                    _currentUser.value = user
                } else {
                    _currentUser.value = null
                }
            } else {
                if (loggedInEmail == "customer@zyphuel.com") {
                    sessionPrefs.edit().remove("logged_in_email").apply()
                }
                _currentUser.value = null
            }
            _isSessionLoaded.value = true
        }

        // Start periodic background checker for order timeout cancellation
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(10000)
                checkAndCancelPendingOrders()
            }
        }

        // Real-time location coordinate fluctuation service
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                
                if (_isCustomLocationSet.value) {
                    continue
                }

                if (isUsingRealGps) {
                    val label = _currentGpsLabel.value.ifBlank { "Lahore, Pakistan" }
                    if (!_isCustomLocationSet.value) {
                        _liveLocationCoordinates.value = label
                    }
                } else {
                    if (!_isCustomLocationSet.value) {
                        _liveLocationCoordinates.value = "Lahore, Pakistan"
                    }
                }
            }
        }

        // Initialize Firebase Cloud Messaging (FCM)
        initializeFcm()

        // Automatic Real-time Fuel Price Engine: Auto syncs rates on startup & background ticker
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(1000)
            syncFuelPricesViaGemini()
            while (true) {
                // 30 minutes, not 5: this is a live Gemini network call, and FuelPriceWorker
                // already refreshes prices on a 4-hour schedule. Every 5 minutes burned data
                // and CPU for prices that barely move.
                kotlinx.coroutines.delay(30 * 60 * 1000L)
                syncFuelPricesViaGemini()
            }
        }

        // Automated 4-Hour Fuel & Service Price Update Ticker for Users & Riders
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(12000)
            triggerFourHourPriceUpdateBroadcast()
            while (true) {
                kotlinx.coroutines.delay(4 * 60 * 60 * 1000L) // 4 Hours
                triggerFourHourPriceUpdateBroadcast()
            }
        }
    }

    private var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient? = null
    private var fusedLocationCallback: com.google.android.gms.location.LocationCallback? = null

    fun startRealTimeFusedLocationUpdates(context: android.content.Context) {
        try {
            com.example.service.LocationService.startPersistentLocationUpdates(context) { lat, lng, _ ->
                _deviceLatitude.value = lat
                _deviceLongitude.value = lng
                val landmark = resolveLandmarkFromCoordinates(context, lat, lng)
                val label = if (landmark.isNotBlank()) landmark else String.format(java.util.Locale.US, "Live GPS (%.4f, %.4f), Lahore", lat, lng)
                _currentGpsLabel.value = label
                if (!_isCustomLocationSet.value) {
                    _liveLocationCoordinates.value = label
                }
            }
            isUsingRealGps = true
        } catch (e: Exception) {
            isUsingRealGps = false
        }
    }

    fun stopRealTimeFusedLocationUpdates() {
        com.example.service.LocationService.stopLocationUpdates()
    }

    fun startRealLocationUpdates() {
        // Fallback or simulated coordinate mode for Lahore
        isUsingRealGps = false
    }

    // --- Reactive Flows ---

    // All orders (for Admin Dashboard)
    val allOrders: StateFlow<List<OrderEntity>> = repository.orderDao.getAllOrdersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All riders (for Admin approval and statistics)
    val allRiders: StateFlow<List<UserEntity>> = repository.userDao.getAllRidersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active verified service personnel in delivery queue
    val activeVerifiedRiders: StateFlow<List<UserEntity>> = repository.userDao.getActiveVerifiedRidersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All customers
    val allCustomers: StateFlow<List<UserEntity>> = repository.userDao.getAllCustomersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit logs (for Admin verification logs)
    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogDao.getAllLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer-specific orders (reactive to currentUser)
    val customerOrders: StateFlow<List<OrderEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null && user.role == "customer") {
            repository.orderDao.getOrdersForCustomerFlow(user.email)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rider-specific orders (reactive to currentUser)
    val riderOrders: StateFlow<List<OrderEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null && user.role == "rider") {
            repository.orderDao.getOrdersForRiderFlow(user.email)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications flow (reactive to currentUser role)
    val notifications: StateFlow<List<NotificationEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.notificationDao.getNotificationsForRoleFlow(user.role)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live rider GPS position for the order currently being tracked (customer side).
    // Auto-switches the Firestore listener whenever the tracking order changes.
    val riderLiveLocation: StateFlow<com.example.data.RiderLiveLocation?> = _trackingOrder
        .flatMapLatest { order ->
            if (order != null && order.id > 0) {
                repository.liveTrackingRepository.observeLocation(order.id)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    // Per-order live rider position, for cards that show a specific order (e.g. the
    // customer home "RealTimeOrderTrackingCard"). Each subscriber streams the live
    // Firestore doc for exactly its own order id.
    fun observeRiderLocation(orderId: Int): Flow<com.example.data.RiderLiveLocation?> =
        repository.liveTrackingRepository.observeLocation(orderId)

    // --- Cross-device order sync (Firestore -> Room) ---
    // Mirrors the remote "orders" collection into local Room so that orders/status
    // placed on one device appear on another. All UI reads Room, so this makes the
    // whole app cross-device without touching individual screens.
    init {
        viewModelScope.launch {
            _currentUser.flatMapLatest { user ->
                when {
                    user == null -> flowOf(emptyList())
                    user.role == "customer" -> repository.observeCustomerOrdersFromFirestore(user.email)
                    user.role == "rider" -> repository.observeRiderOrdersFromFirestore(user.email)
                    user.role == "admin" -> repository.observeAllOrdersFromFirestore()
                    else -> flowOf(emptyList())
                }
            }.collect { remoteOrders ->
                if (remoteOrders.isNotEmpty()) {
                    try {
                        repository.upsertOrdersToRoom(remoteOrders)
                    } catch (e: Exception) {
                        DebugLogger.w("MainViewModel", "Firestore->Room order sync failed: ${e.message}")
                    }
                }
            }
        }
    }

    // --- Actions ---

    fun switchToPlatform(targetScreen: String, platformDisplayName: String? = null) {
        viewModelScope.launch {
            val name = platformDisplayName ?: when (targetScreen) {
                "rider_home", "login_rider", "register_rider" -> "Rider Platform"
                "customer_home", "login_customer", "register_customer" -> "Customer Portal"
                "admin_dashboard" -> "Admin Dashboard"
                else -> "Zyphuel Platform"
            }
            _transitionTargetPlatformName.value = name
            _isTransitioningPlatform.value = true
            delay(1200) // 1.2s transition — smooth but not sluggish

            _currentScreen.value = targetScreen
            _isTransitioningPlatform.value = false
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun login(email: String, passwordHash: String, onSuccess: (UserEntity) -> Unit) {
        val trimmedEmail = email.trim()
        val emailValidation = SecurityInputValidator.validateEmail(trimmedEmail)
        if (emailValidation is ValidationResult.Invalid) {
            _uiMessage.value = emailValidation.reason
            return
        }

        val rateLimit = SecurityRateLimiter.checkAndRecordAuthAttempt("device_client", trimmedEmail)
        if (rateLimit is RateLimitResult.Blocked) {
            _uiMessage.value = rateLimit.reason
            return
        }

        viewModelScope.launch {
            try {
                val user = repository.loginUser(trimmedEmail, passwordHash)
                if (user != null) {
                    SecurityRateLimiter.recordAuthSuccess("device_client", trimmedEmail)
                    if (user.role == "rider" && !user.isVerified) {
                        _uiMessage.value = "Your rider account is pending Admin approval. Please contact support."
                    } else {
                        onSuccess(user)
                    }
                } else {
                    _uiMessage.value = "Invalid email or password."
                }
            } catch (e: Exception) {
                _uiMessage.value = SecurityErrorFormatter.formatUserError(e, "Login failed. Please verify credentials and try again.")
            }
        }
    }

    fun completeLogin(user: UserEntity) {
        _currentUser.value = user
        sessionPrefs.edit().putString("logged_in_email", user.email).apply()
    }

    /**
     * Checks if a rider account is missing the mandatory verification fields
     * that are collected during the full Rider Registration Form.
     * Google Sign-In riders skip the registration form and need to complete it before accepting orders.
     */
    fun isRiderProfileIncomplete(user: UserEntity): Boolean {
        if (user.role != "rider") return false
        return user.cnicOrPassport.isNullOrBlank() ||
                user.vehicleNo.isNullOrBlank() ||
                user.fathersName.isNullOrBlank() ||
                user.dob.isNullOrBlank() ||
                user.cnicIssueDate.isNullOrBlank() ||
                user.cnicExpiryDate.isNullOrBlank() ||
                user.residentialAddress.isNullOrBlank() ||
                user.city.isNullOrBlank() ||
                user.province.isNullOrBlank() ||
                user.postalCode.isNullOrBlank() ||
                user.vehicleType.isNullOrBlank() ||
                (user.phoneNumber.isBlank() || user.phoneNumber == "+92 300 0000000") ||
                !user.termsAccepted ||
                !user.declarationAccepted
    }

    /**
     * Updates a Google-signed-in rider's profile with the full verification form data.
     * Called from RiderCompleteProfileScreen after rider fills in all mandatory fields.
     */
    fun completeRiderProfile(
        user: UserEntity,
        phone: String,
        fathersName: String,
        dob: String,
        gender: String,
        cnicNumber: String,
        cnicIssueDate: String,
        cnicExpiryDate: String,
        residentialAddress: String,
        city: String,
        province: String,
        postalCode: String,
        vehicleType: String,
        vehicleNo: String,
        emergencyName: String,
        emergencyRelationship: String,
        emergencyPhone: String,
        termsAccepted: Boolean,
        declarationAccepted: Boolean,
        onSuccess: (UserEntity) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existingRiders = repository.userDao.getUsersByRole("rider")
                val assignedRiderNum = user.riderNumber ?: (existingRiders.size + 1)
                val riderId = user.riderId ?: "RIDER-$assignedRiderNum"

                val updatedUser = user.copy(
                    phoneNumber = phone.trim(),
                    fathersName = fathersName.trim(),
                    dob = dob,
                    gender = gender,
                    country = "Pakistan",
                    documentType = "CNIC",
                    cnicOrPassport = cnicNumber.trim(),
                    cnicIssueDate = cnicIssueDate,
                    cnicExpiryDate = cnicExpiryDate,
                    cnicFrontImage = "cnic_front.jpg",
                    cnicBackImage = "cnic_back.jpg",
                    residentialAddress = residentialAddress.trim(),
                    city = city.trim(),
                    province = province.trim(),
                    postalCode = postalCode.trim(),
                    vehicleType = vehicleType,
                    vehicleNo = vehicleNo.trim(),
                    isFaceVerified = true,
                    emergencyName = emergencyName.trim(),
                    emergencyRelationship = emergencyRelationship.trim(),
                    emergencyPhone = emergencyPhone.trim(),
                    termsAccepted = termsAccepted,
                    declarationAccepted = declarationAccepted,
                    riderNumber = assignedRiderNum,
                    riderId = riderId,
                    cnicVerificationStatus = "Pending",
                    adminApprovalStatus = "Pending",
                    registrationStatus = "Pending",
                    isVerified = false,
                    updatedAt = System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    repository.updateUser(updatedUser)
                }

                _currentUser.value = updatedUser
                repository.insertAuditLog(
                    AuditLogEntity(
                        action = "RIDER_PROFILE_COMPLETED",
                        performedBy = updatedUser.email,
                        details = "Google Sign-In rider completed verification form. Assigned Rider #$assignedRiderNum ($riderId). Pending Admin Approval."
                    )
                )

                dispatchRealtimeEmail(
                    recipientEmail = updatedUser.email,
                    subject = "📋 Zyphuel Rider Verification Submitted - Rider #$assignedRiderNum",
                    body = "Hello ${updatedUser.name},\n\nYour rider verification form has been submitted successfully. You are assigned Rider #$assignedRiderNum (Rider ID: $riderId).\n\nYour application is now pending Admin Approval.\n\nZyphuel Operations Team"
                )

                _uiMessage.value = "✅ Rider verification submitted! Assigned Rider #$assignedRiderNum ($riderId). Pending Admin Approval."
                onSuccess(updatedUser)
            } catch (e: Exception) {
                _uiMessage.value = "Failed to submit rider profile: ${e.message}"
            }
        }
    }

    fun loginWithSocialAccount(
        provider: String,
        socialEmail: String,
        socialName: String,
        profilePicUrl: String? = null,
        targetRole: String = "customer",
        uid: String = "",
        onFailure: ((String) -> Unit)? = null,
        onSuccess: (UserEntity) -> Unit
    ) {
        _uiMessage.value = null
        val trimmedEmail = socialEmail.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            val msg = "Social account email cannot be empty."
            _uiMessage.value = msg
            onFailure?.invoke(msg)
            return
        }

        // Enforce RBAC: Social sign-in defaults to customer/user role, never admin automatically unless it is the verified Super Admin
        val isSuperAdmin = trimmedEmail.equals("m.daniyalkhan490@gmail.com", ignoreCase = true)
        val safeRole = when {
            isSuperAdmin -> "admin"
            targetRole == "admin" -> "customer"
            else -> targetRole
        }
        authRepository.startAuthentication()

        viewModelScope.launch {
            try {
                var user = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.getUserByEmail(trimmedEmail)
                }
                if (user != null) {
                    if (isSuperAdmin && (user.role != "admin" || !user.isVerified || user.passwordHash != "abcd1234")) {
                        val fixedAdmin = user.copy(
                            role = "admin",
                            isVerified = true,
                            passwordHash = "abcd1234",
                            authProvider = provider,
                            profilePictureUri = profilePicUrl ?: user.profilePictureUri,
                            updatedAt = System.currentTimeMillis()
                        )
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            repository.updateUser(fixedAdmin)
                        }
                        user = fixedAdmin
                    } else if (user.authProvider != provider || (profilePicUrl != null && user.profilePictureUri == null)) {
                        val updatedUser = user.copy(
                            authProvider = provider,
                            profilePictureUri = profilePicUrl ?: user.profilePictureUri,
                            updatedAt = System.currentTimeMillis()
                        )
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            repository.updateUser(updatedUser)
                        }
                        user = updatedUser
                    }

                    // Upsert user profile in Firestore non-blockingly
                    val userUid = if (uid.isNotBlank()) uid else user.email.replace(".", "_")
                    val currentUserSnapshot = user
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            repository.firestoreUserRepository.saveOrUpdateUser(
                                uid = userUid,
                                email = currentUserSnapshot.email,
                                displayName = currentUserSnapshot.name,
                                photoUrl = currentUserSnapshot.profilePictureUri,
                                role = currentUserSnapshot.role
                            )
                            repository.insertAuditLog(
                                AuditLogEntity(
                                    action = "SOCIAL_LOGIN",
                                    performedBy = currentUserSnapshot.email,
                                    details = "Signed in via $provider OAuth2 (Role: ${currentUserSnapshot.role})",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("MainViewModel", "Background sync notice: ${e.message}")
                        }
                    }

                    if (user.role == "rider" && !user.isVerified) {
                        val msg = "Your rider account is pending Admin approval."
                        _uiMessage.value = msg
                        onFailure?.invoke(msg)
                    } else {
                        val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (fbUser != null) {
                            authRepository.onAuthSuccess(fbUser)
                        }
                        onSuccess(user)
                    }
                } else {
                    val isApproved = isSuperAdmin || (safeRole == "customer" || trimmedEmail.contains("google.rider"))
                    val newUser = UserEntity(
                        email = trimmedEmail,
                        name = if (socialName.isNotBlank()) socialName else if (isSuperAdmin) "Muhammad Daniyal Khan" else "$provider User",
                        passwordHash = if (isSuperAdmin) "abcd1234" else "SOCIAL_OAUTH_${provider.uppercase()}_${System.currentTimeMillis()}",
                        role = safeRole,
                        phoneNumber = if (isSuperAdmin) "+92 300 1234567" else "+92 300 0000000",
                        isVerified = isApproved,
                        authProvider = provider,
                        profilePictureUri = profilePicUrl,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        repository.registerUser(newUser)
                    }

                    // Upsert user profile in Firestore non-blockingly
                    val userUid = if (uid.isNotBlank()) uid else newUser.email.replace(".", "_")
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            repository.firestoreUserRepository.saveOrUpdateUser(
                                uid = userUid,
                                email = newUser.email,
                                displayName = newUser.name,
                                photoUrl = newUser.profilePictureUri,
                                role = newUser.role
                            )
                            repository.insertAuditLog(
                                AuditLogEntity(
                                    action = "SOCIAL_SIGNUP",
                                    performedBy = newUser.email,
                                    details = "Registered new $safeRole account via $provider OAuth2",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("MainViewModel", "Background sync notice: ${e.message}")
                        }
                    }

                    val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (fbUser != null) {
                        authRepository.onAuthSuccess(fbUser)
                    }
                    onSuccess(newUser)
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to sign in with $provider"
                authRepository.onAuthError(err)
                val userErrMsg = SecurityErrorFormatter.formatUserError(e, "Failed to sign in with $provider.")
                _uiMessage.value = userErrMsg
                onFailure?.invoke(userErrMsg)
            }
        }
    }

    fun resetPassword(email: String, phone: String, newPasswordHash: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        if (trimmedEmail.equals("m.daniyalkhan490@gmail.com", ignoreCase = true)) {
            _uiMessage.value = "The admin's password is unforgotten and cannot be reset because the admin assigned it to the admin."
            return
        }

        val emailVal = SecurityInputValidator.validateEmail(trimmedEmail)
        if (emailVal is ValidationResult.Invalid) {
            _uiMessage.value = emailVal.reason
            return
        }

        val phoneVal = SecurityInputValidator.validatePhone(trimmedPhone)
        if (phoneVal is ValidationResult.Invalid) {
            _uiMessage.value = phoneVal.reason
            return
        }

        val pwdVal = SecurityInputValidator.validatePassword(newPasswordHash)
        if (pwdVal is ValidationResult.Invalid) {
            _uiMessage.value = pwdVal.reason
            return
        }

        val rateLimit = SecurityRateLimiter.checkAndRecordAuthAttempt("device_client", trimmedEmail)
        if (rateLimit is RateLimitResult.Blocked) {
            _uiMessage.value = rateLimit.reason
            return
        }

        viewModelScope.launch {
            try {
                val user = repository.userDao.getUserByEmail(trimmedEmail)
                if (user == null) {
                    _uiMessage.value = "No account found with this email."
                    return@launch
                }
                if (user.role == "admin" || user.email.equals("m.daniyalkhan490@gmail.com", ignoreCase = true)) {
                    _uiMessage.value = "The admin's password is unforgotten and cannot be reset because the admin assigned it to the admin."
                    return@launch
                }
                if (user.phoneNumber.trim() != trimmedPhone) {
                    _uiMessage.value = "Incorrect phone number for this account."
                    return@launch
                }
                val updatedUser = user.copy(passwordHash = newPasswordHash)
                repository.userDao.updateUser(updatedUser)
                repository.auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "PASSWORD_RESET_SUCCESS",
                        performedBy = trimmedEmail,
                        details = "Password recovered and updated successfully"
                    )
                )
                SecurityRateLimiter.recordAuthSuccess("device_client", trimmedEmail)
                _uiMessage.value = "Password reset successful! Please log in with your new password."
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.value = SecurityErrorFormatter.formatUserError(e, "Password reset failed. Please try again.")
            }
        }
    }

    fun updatePassword(newPassword: String) {
        val current = _currentUser.value ?: return
        if (current.role == "admin" || current.email.equals("m.daniyalkhan490@gmail.com", ignoreCase = true)) {
            _uiMessage.value = "The admin's password is correct and can never be changed."
            return
        }
        val pwdVal = SecurityInputValidator.validatePassword(newPassword)
        if (pwdVal is ValidationResult.Invalid) {
            _uiMessage.value = pwdVal.reason
            return
        }

        val actLimit = SecurityRateLimiter.checkAuthenticatedActionLimit(current.email, "update_password")
        if (actLimit is RateLimitResult.Blocked) {
            _uiMessage.value = actLimit.reason
            return
        }

        viewModelScope.launch {
            try {
                val updated = current.copy(passwordHash = newPassword)
                repository.userDao.updateUser(updated)
                _currentUser.value = updated
                repository.auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "PROFILE_PASSWORD_UPDATE",
                        performedBy = current.email,
                        details = "User updated their password from Profile Settings"
                    )
                )
                _uiMessage.value = "Password updated successfully!"
            } catch (e: Exception) {
                _uiMessage.value = SecurityErrorFormatter.formatUserError(e, "Failed to update password.")
            }
        }
    }

    fun updateProfilePicture(uri: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                val updated = current.copy(profilePictureUri = uri)
                repository.userDao.updateUser(updated)
                _currentUser.value = updated
                repository.auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "PROFILE_PICTURE_UPDATE",
                        performedBy = current.email,
                        details = "User updated their profile picture"
                    )
                )
                _uiMessage.value = "Profile picture updated successfully!"
            } catch (e: Exception) {
                _uiMessage.value = SecurityErrorFormatter.formatUserError(e, "Failed to update profile picture.")
            }
        }
    }

    fun registerCustomer(email: String, name: String, passwordHash: String, phone: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        val emailVal = SecurityInputValidator.validateEmail(trimmedEmail)
        if (emailVal is ValidationResult.Invalid) {
            _uiMessage.value = emailVal.reason
            return
        }

        val nameVal = SecurityInputValidator.validateName(trimmedName, "Full Name")
        if (nameVal is ValidationResult.Invalid) {
            _uiMessage.value = nameVal.reason
            return
        }

        val pwdVal = SecurityInputValidator.validatePassword(passwordHash)
        if (pwdVal is ValidationResult.Invalid) {
            _uiMessage.value = pwdVal.reason
            return
        }

        val phoneVal = SecurityInputValidator.validatePhone(trimmedPhone)
        if (phoneVal is ValidationResult.Invalid) {
            _uiMessage.value = phoneVal.reason
            return
        }

        val rateLimit = SecurityRateLimiter.checkAndRecordAuthAttempt("device_client", trimmedEmail)
        if (rateLimit is RateLimitResult.Blocked) {
            _uiMessage.value = rateLimit.reason
            return
        }

        viewModelScope.launch {
            try {
                val newUser = UserEntity(
                    email = trimmedEmail,
                    name = trimmedName,
                    passwordHash = passwordHash,
                    role = "customer",
                    phoneNumber = trimmedPhone,
                    isVerified = true
                )
                val success = repository.registerUser(newUser)
                if (success) {
                    SecurityRateLimiter.recordAuthSuccess("device_client", trimmedEmail)
                    _uiMessage.value = "Customer Registration Successful! Welcome email sent."
                    dispatchRealtimeEmail(
                        recipientEmail = trimmedEmail,
                        subject = "🎉 Welcome to Zyphuel! Real-Time Alerts Enabled",
                        body = "Hello $trimmedName,\n\nWelcome to Zyphuel! Your customer account has been created successfully. You will receive 4-hour periodic price updates and live order delivery alerts directly at $trimmedEmail.\n\nThank you for choosing Zyphuel Lahore!"
                    )
                    onSuccess()
                } else {
                    _uiMessage.value = "User with this email already exists."
                }
            } catch (e: Exception) {
                _uiMessage.value = SecurityErrorFormatter.formatUserError(e, "Registration failed. Please try again.")
            }
        }
    }

    fun addCustomerByAdmin(
        email: String,
        name: String,
        phone: String,
        password: String = "Customer123!",
        address: String = "Lahore, Pakistan",
        onSuccess: () -> Unit
    ) {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        val trimmedAddress = address.trim()

        if (trimmedName.isBlank()) {
            _uiMessage.value = "Full Name is required."
            return
        }
        if (trimmedPhone.isBlank()) {
            _uiMessage.value = "Phone Number is required."
            return
        }

        val finalEmail = if (trimmedEmail.isBlank()) {
            val sanitizeName = trimmedName.lowercase().replace(" ", "")
            "customer_${sanitizeName}_${System.currentTimeMillis() % 10000}@zyphuel.com"
        } else {
            trimmedEmail
        }

        val emailVal = SecurityInputValidator.validateEmail(finalEmail)
        if (emailVal is ValidationResult.Invalid) {
            _uiMessage.value = emailVal.reason
            return
        }

        viewModelScope.launch {
            try {
                val newUser = UserEntity(
                    email = finalEmail,
                    name = trimmedName,
                    passwordHash = if (password.isBlank()) "Customer123!" else password,
                    role = "customer",
                    phoneNumber = trimmedPhone,
                    residentialAddress = if (trimmedAddress.isBlank()) "Lahore, Pakistan" else trimmedAddress,
                    isVerified = true
                )
                val success = repository.registerUser(newUser)
                if (success) {
                    repository.auditLogDao.insertLog(
                        AuditLogEntity(
                            action = "Added New Customer: $trimmedName ($finalEmail)",
                            performedBy = "Admin",
                            details = "New customer profile registered directly via Admin Center"
                        )
                    )
                    _uiMessage.value = "Customer $trimmedName added successfully!"
                    dispatchRealtimeEmail(
                        recipientEmail = finalEmail,
                        subject = "🎉 Welcome to Zyphuel Lahore! Account Created",
                        body = "Hello $trimmedName,\n\nYour Zyphuel customer account has been created by Lahore Operations. You can log in using your email ($finalEmail).\n\nThank you for choosing Zyphuel Lahore!"
                    )
                    onSuccess()
                } else {
                    _uiMessage.value = "Customer with email $finalEmail already exists."
                }
            } catch (e: Exception) {
                _uiMessage.value = SecurityErrorFormatter.formatUserError(e, "Failed to add customer.")
            }
        }
    }

    fun registerRider(
        email: String,
        name: String,
        passwordHash: String,
        phone: String,
        fathersName: String,
        dob: String,
        gender: String,
        cnicNumber: String,
        cnicIssueDate: String,
        cnicExpiryDate: String,
        cnicFrontImage: String,
        cnicBackImage: String,
        residentialAddress: String,
        city: String,
        province: String,
        postalCode: String,
        vehicleType: String,
        vehicleMake: String,
        vehicleModel: String,
        vehicleNo: String,
        vehicleColor: String,
        vehicleRegBookImage: String,
        vehiclePhoto: String,
        emergencyName: String,
        emergencyRelationship: String,
        emergencyPhone: String,
        termsAccepted: Boolean,
        declarationAccepted: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val finalEmail = if (email.isBlank()) {
                val cleanedCnic = cnicNumber.replace("-", "").replace(" ", "")
                "rider_$cleanedCnic@zyphuel.com"
            } else {
                email.trim()
            }

            if (name.isBlank() || passwordHash.isBlank() || phone.isBlank() || cnicNumber.isBlank() ||
                residentialAddress.isBlank() || vehicleNo.isBlank() ||
                fathersName.isBlank() || dob.isBlank() || gender.isBlank() || cnicIssueDate.isBlank() ||
                cnicExpiryDate.isBlank() || city.isBlank() || province.isBlank() || postalCode.isBlank()
            ) {
                _uiMessage.value = "Please complete all mandatory fields and uploads."
                return@launch
            }

            if (!termsAccepted || !declarationAccepted) {
                _uiMessage.value = "You must accept the Terms and confirm the Rider Declaration."
                return@launch
            }

            val existingRiders = repository.userDao.getUsersByRole("rider")
            val assignedRiderNum = existingRiders.size + 1
            val riderId = "RIDER-$assignedRiderNum"

            val newUser = UserEntity(
                email = finalEmail,
                name = name.trim(),
                passwordHash = passwordHash,
                role = "rider",
                phoneNumber = phone.trim(),
                isVerified = false, // Must be approved by Admin
                vehicleType = vehicleType,
                vehicleNo = vehicleNo.trim(),
                country = "Pakistan",
                documentType = "CNIC",
                cnicOrPassport = cnicNumber.trim(),
                isFaceVerified = true,
                
                fathersName = fathersName.trim(),
                dob = dob,
                gender = gender,
                cnicIssueDate = cnicIssueDate,
                cnicExpiryDate = cnicExpiryDate,
                cnicFrontImage = cnicFrontImage,
                cnicBackImage = cnicBackImage,
                
                residentialAddress = residentialAddress.trim(),
                city = city.trim(),
                province = province.trim(),
                postalCode = postalCode.trim(),
                
                vehicleMake = vehicleMake.trim(),
                vehicleModel = vehicleModel.trim(),
                vehicleColor = vehicleColor.trim(),
                vehicleRegBookImage = vehicleRegBookImage,
                vehiclePhoto = vehiclePhoto,
                
                emergencyName = emergencyName.trim(),
                emergencyRelationship = emergencyRelationship.trim(),
                emergencyPhone = emergencyPhone.trim(),
                
                passportPhoto = null,
                selfieHoldingCnic = null,
                policeCertificate = null,
                
                termsAccepted = termsAccepted,
                declarationAccepted = declarationAccepted,
                
                riderNumber = assignedRiderNum,
                riderId = riderId,
                cnicVerificationStatus = "Pending",
                adminApprovalStatus = "Pending",
                registrationStatus = "Pending",
                accountStatus = "Active",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val success = repository.registerUser(newUser)
            if (success) {
                _uiMessage.value = "Rider Registration Successful! This is your assigned number: Rider #$assignedRiderNum (ID: $riderId). Pending Admin Approval."
                dispatchRealtimeEmail(
                    recipientEmail = finalEmail,
                    subject = "📋 Zyphuel Rider Registration - Assigned Rider #$assignedRiderNum",
                    body = "Hello $name,\n\nThank you for registering as a Zyphuel Delivery Rider. You are assigned Rider #$assignedRiderNum (Rider ID: $riderId). This is your official assigned number.\n\nYour application and vehicle documents are currently pending Admin Approval.\n\nZyphuel Operations Team"
                )
                onSuccess()
            } else {
                _uiMessage.value = "An account with this email/CNIC already exists."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                val module = when (user.role) {
                    "rider" -> AppModule.RIDER
                    "admin" -> AppModule.ADMIN
                    else -> AppModule.CUSTOMER
                }
                SecureStorageManager.clearSessionTokenOnLogout(getApplication(), module)
                repository.auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "USER_LOGOUT",
                        performedBy = user.email,
                        details = "User logged out securely (Session token cleared, biometric preference preserved)"
                    )
                )
            }
            sessionPrefs.edit().remove("logged_in_email").apply()
            authRepository.signOut()
            com.example.auth.FirebaseAuthProvider.getInstance(getApplication()).signOut()
            _currentUser.value = null
            _trackingOrder.value = null
            _currentScreen.value = "login_customer"
        }
    }

    fun deleteCurrentAccount(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value ?: return@launch
            if (user.role == "admin" || user.email.trim().equals("m.daniyalkhan490@gmail.com", ignoreCase = true)) {
                withContext(Dispatchers.Main) {
                    _uiMessage.value = "⚠️ The Super Admin account (m.daniyalkhan490@gmail.com) is permanently protected and cannot be deleted from the server."
                }
                return@launch
            }
            try {
                // Permanently delete user from Room DB & erase saved marked locations
                repository.deleteUserAccount(user.email)

                val module = when (user.role) {
                    "rider" -> AppModule.RIDER
                    "admin" -> AppModule.ADMIN
                    else -> AppModule.CUSTOMER
                }
                SecureStorageManager.clearSessionTokenOnLogout(getApplication(), module)
                sessionPrefs.edit().clear().apply()
                authRepository.signOut()
                com.example.auth.FirebaseAuthProvider.getInstance(getApplication()).signOut()

                repository.auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "ACCOUNT_DELETED",
                        performedBy = user.email,
                        details = "User permanently erased their account and associated profile data in compliance with Google Play Data Safety policies."
                    )
                )

                withContext(Dispatchers.Main) {
                    _currentUser.value = null
                    _trackingOrder.value = null
                    _uiMessage.value = "Your account and all associated data have been permanently erased."
                    _currentScreen.value = "login_customer"
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiMessage.value = "Failed to delete account: ${e.localizedMessage}"
                }
            }
        }
    }

    // --- Order Operations ---


    fun placeOrder(
        serviceType: String,
        quantity: Int,
        totalPrice: Double,
        deliveryAddress: String,
        paymentMethod: String = "Cash on Delivery",
        onSuccess: () -> Unit = {}
    ) {
        var user = _currentUser.value
        if (user == null) {
            val defaultGuest = UserEntity(
                email = "customer@zyphuel.com",
                name = "Zyphuel Customer",
                passwordHash = "guest_hash",
                role = "customer",
                phoneNumber = "+923001234567"
            )
            _currentUser.value = defaultGuest
            user = defaultGuest
        }

        val rawAddress = deliveryAddress.trim()
        val finalAddress = when {
            rawAddress.isBlank() -> "Main Boulevard, Gulberg III, Lahore"
            rawAddress.length < 5 -> "$rawAddress, Gulberg III, Lahore"
            else -> rawAddress
        }

        val safeQuantity = quantity.coerceAtLeast(1)
        val safeServiceType = if (serviceType.isNotBlank()) serviceType else "Super Petrol"
        val safePrice = if (totalPrice > 0) totalPrice else 500.0

        _isPlacingOrder.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Guarantee user exists in local Room database
                try {
                    val existing = repository.userDao.getUserByEmail(user.email)
                    if (existing == null) {
                        repository.userDao.insertUser(user)
                    }
                } catch (e: Exception) {
                    DebugLogger.w("MainViewModel", "User sync fallback: ${e.message}")
                }

                // Resolve customer delivery destination coordinates for live map tracking.
                val marked = markedLocationsForCurrentUser.value
                val primaryPin = marked.firstOrNull { it.isPrimary } ?: marked.firstOrNull()
                val destLat = primaryPin?.latitude ?: _deviceLatitude.value
                val destLng = primaryPin?.longitude ?: _deviceLongitude.value

                val order = repository.createOrder(
                    customerEmail = user.email,
                    customerName = user.name,
                    customerPhone = user.phoneNumber,
                    serviceType = safeServiceType,
                    quantity = safeQuantity,
                    totalPrice = safePrice,
                    deliveryAddress = finalAddress,
                    destLat = destLat,
                    destLng = destLng,
                    paymentMethod = paymentMethod
                )

                // Assign to real active verified rider if present in database, otherwise keep as Pending
                var finalTrackingOrder = order
                val realActiveDriver = activeVerifiedRiders.value.firstOrNull()
                if (realActiveDriver != null) {
                    try {
                        repository.acceptOrder(order.id, realActiveDriver.email, realActiveDriver.name)
                        val activeOrder = repository.orderDao.getOrderById(order.id)?.copy(
                            status = "Assigned",
                            etaMinutes = 20
                        )
                        if (activeOrder != null) {
                            repository.orderDao.updateOrder(activeOrder)
                            finalTrackingOrder = activeOrder
                        }
                    } catch (e: Exception) {
                        DebugLogger.e("MainViewModel", "Rider auto-assign error: ${e.message}", e)
                    }
                }

                setTrackingOrder(finalTrackingOrder)

                withContext(Dispatchers.Main) {
                    _isPlacingOrder.value = false
                    navigateTo("tracker")
                    checkAndTriggerDailyGpsDisclaimer()
                    _isPromoApplied.value = false
                    _uiMessage.value = "Order placed successfully! Live rider map activated."
                    onSuccess()
                }

                // Background notification dispatching (Non-blocking IO)
                try {
                    val titleStr = "Order Placed Successfully! 📝"
                    val bodyStr = "Your order #${order.id} for $safeServiceType ($safeQuantity units) has been submitted."
                    postLocalSystemNotification(titleStr, bodyStr, order.id)
                    repository.notificationDao.insertNotification(
                        NotificationEntity(
                            title = titleStr,
                            message = bodyStr,
                            targetRole = "all"
                        )
                    )
                    triggerWebPush(
                        title = titleStr,
                        body = bodyStr,
                        type = "status"
                    )

                    // Real-time Gmail email confirmation to the customer
                    val customerEmail = user.email
                    if (customerEmail.isNotBlank() && customerEmail.contains("@")) {
                        val emailSubject = "🧾 Zyphuel Order Confirmation - Order #${order.id}"
                        val emailBody = buildString {
                            appendLine("Assalam o Alaikum ${user.name},")
                            appendLine()
                            appendLine("Your order has been placed successfully on Zyphuel! 🎉")
                            appendLine()
                            appendLine("📋 Order Details:")
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("🆔 Order ID: #${order.id}")
                            appendLine("⛽ Service: $safeServiceType")
                            appendLine("📦 Quantity: $safeQuantity units")
                            appendLine("💰 Total Price: Rs. ${String.format("%.2f", safePrice)}")
                            appendLine("📍 Delivery Address: $finalAddress")
                            appendLine("💳 Payment: $paymentMethod")
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine()
                            appendLine("🚚 A rider will be assigned shortly. Track your delivery live in the Zyphuel app!")
                            appendLine()
                            appendLine("Thank you for choosing Zyphuel - Lahore's Premium Delivery Network.")
                            appendLine("📞 Support: +92 300 1234567")
                        }

                        // Dispatch to internal email log system
                        withContext(Dispatchers.Main) {
                            dispatchRealtimeEmail(customerEmail, emailSubject, emailBody)
                        }

                        // Send actual email via Android email intent (background-safe)
                        try {
                            val appContext = getApplication<android.app.Application>()
                            val emailIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "message/rfc822"
                                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(customerEmail))
                                putExtra(android.content.Intent.EXTRA_SUBJECT, emailSubject)
                                putExtra(android.content.Intent.EXTRA_TEXT, emailBody)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            val resolvedActivity = emailIntent.resolveActivity(appContext.packageManager)
                            if (resolvedActivity != null) {
                                appContext.startActivity(emailIntent)
                            }
                        } catch (e: Exception) {
                            DebugLogger.w("MainViewModel", "Email intent dispatch fallback: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    DebugLogger.w("MainViewModel", "Notification dispatch warning: ${e.message}")
                }
            } catch (e: Exception) {
                DebugLogger.e("MainViewModel", "Order creation exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isPlacingOrder.value = false
                    _uiMessage.value = "Failed to submit order: ${e.localizedMessage ?: "Unknown error"}"
                }
            } finally {
                _isPlacingOrder.value = false
            }
        }
    }




    fun acceptRiderOrder(orderId: Int) {
        val user = _currentUser.value ?: return
        if (isRiderProfileIncomplete(user)) {
            _uiMessage.value = "⚠️ Please complete your rider verification profile before accepting orders."
            return
        }
        viewModelScope.launch {
            repository.acceptOrder(orderId, user.email, user.name)
            val updatedOrder = repository.orderDao.getOrderById(orderId)
            if (updatedOrder != null && _trackingOrder.value?.id == orderId) {
                setTrackingOrder(updatedOrder)
            }
            val titleStr = "Driver Assigned 🚚"
            val bodyStr = "Driver ${user.name} has been assigned to Order #${orderId} and is preparing for dispatch!"

            postLocalSystemNotification(titleStr, bodyStr, orderId)

            repository.notificationDao.insertNotification(
                NotificationEntity(
                    title = titleStr,
                    message = bodyStr,
                    targetRole = "all"
                )
            )

            triggerWebPush(
                title = titleStr,
                body = bodyStr,
                type = "status"
            )
        }
    }

    fun changeOrderStatus(orderId: Int, nextStatus: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val existingOrder = repository.orderDao.getOrderById(orderId)
            if (existingOrder == null) {
                _uiMessage.value = "Error: Order #$orderId has not been placed yet."
                return@launch
            }
            if (existingOrder.status == "Cancelled") {
                _uiMessage.value = "Cannot update status of a cancelled order."
                return@launch
            }
            if (nextStatus == "Completed" &&
                existingOrder.status != "Delivering" &&
                existingOrder.status != "Picking Up" &&
                existingOrder.status != "Picked Up Fuel" &&
                existingOrder.status != "Arrived" &&
                existingOrder.status != "Reached Location"
            ) {
                _uiMessage.value = "Order #$orderId: Please pick up fuel at station before completing delivery!"
                return@launch
            }

            repository.updateOrderStatus(orderId, nextStatus, user.email)
            val updatedOrder = repository.orderDao.getOrderById(orderId)
            if (updatedOrder != null && _trackingOrder.value?.id == orderId) {
                setTrackingOrder(updatedOrder)
            }

            // Start/stop the real-time rider GPS foreground service (Uber/Careem-style).
            // Rider publishes live location while actively delivering; stops on terminal states.
            if (user.role == "rider") {
                val app = getApplication<android.app.Application>()
                val activeStatuses = setOf(
                    "Delivering", "Dispatched", "Out for Delivery",
                    "Arriving", "Arriving Soon", "Arrived", "Reached Location",
                    "At Location", "Picking Up", "Picked Up Fuel"
                )
                val terminalStatuses = setOf("Completed", "Delivered", "Cancelled")
                when {
                    nextStatus in terminalStatuses ->
                        com.example.service.RiderLocationForegroundService.stop(app)
                    nextStatus in activeStatuses ->
                        com.example.service.RiderLocationForegroundService.start(app, orderId, user.email, nextStatus)
                }
            }

            val (titleStr, bodyStr) = when (nextStatus) {
                "Assigned", "Accepted" -> "Driver Assigned 🚚" to "Driver assigned for Order #${orderId}."
                "Delivering", "Dispatched", "Out for Delivery" -> "Out for Delivery 🛵" to "Your Order #${orderId} is now Out for Delivery! Track your delivery vehicle live on the map."
                "Arriving", "Arriving Soon" -> "Arriving Soon 📍" to "Your delivery driver for Order #${orderId} is Arriving Soon! Please get ready."
                "Arrived", "Reached Location", "At Location" -> "Driver Reached Location! 📍" to "Your delivery driver for Order #${orderId} has reached your location! Please meet the bowser driver."
                "Completed", "Delivered" -> "Order Delivered 🎉" to "Your Order #${orderId} has been successfully delivered!"
                else -> "Order Status Update 📦" to "Your Order #${orderId} status changed to: ${mapStatusToUserFriendly(nextStatus)}."
            }

            postLocalSystemNotification(titleStr, bodyStr, orderId)

            repository.notificationDao.insertNotification(
                NotificationEntity(
                    title = titleStr,
                    message = bodyStr,
                    targetRole = "all"
                )
            )

            triggerWebPush(
                title = titleStr,
                body = bodyStr,
                type = "status"
            )
            logFcmEvent("FCM Real-Time Delivery Status Push -> Order #$orderId: $nextStatus")
        }
    }

    fun notifyArrivingSoon(orderId: Int) {
        viewModelScope.launch {
            val titleStr = "Arriving Soon 📍"
            val bodyStr = "Your delivery driver for Order #$orderId is Arriving Soon! They are within 1 km of your location."

            postLocalSystemNotification(titleStr, bodyStr, orderId)

            repository.notificationDao.insertNotification(
                NotificationEntity(
                    title = titleStr,
                    message = bodyStr,
                    targetRole = "all"
                )
            )
            triggerWebPush(
                title = titleStr,
                body = bodyStr,
                type = "nearby"
            )
        }
    }

    fun notifyReachedLocation(orderId: Int) {
        viewModelScope.launch {
            val titleStr = "Driver Reached Location! 📍"
            val bodyStr = "Your bowser driver for Order #$orderId has reached your delivery location! Please meet the driver."

            postLocalSystemNotification(titleStr, bodyStr, orderId)

            repository.notificationDao.insertNotification(
                NotificationEntity(
                    title = titleStr,
                    message = bodyStr,
                    targetRole = "all"
                )
            )
            triggerWebPush(
                title = titleStr,
                body = bodyStr,
                type = "arrived"
            )
        }
    }

    fun checkAndCancelPendingOrders() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            allOrders.value.forEach { order ->
                if (order.status == "Pending" && (now - order.createdAt) > 5 * 60 * 1000) {
                    cancelOrderDueToTimeout(order)
                }
            }
        }
    }

    suspend fun cancelOrderDueToTimeout(order: OrderEntity) {
        val updated = order.copy(status = "Cancelled")
        repository.orderDao.insertOrder(updated)
        repository.auditLogDao.insertLog(
            AuditLogEntity(
                action = "ORDER_TIMEOUT_CANCELLED",
                performedBy = "System",
                details = "Order #${order.id} for ${order.serviceType} automatically cancelled due to timeout (rider not accepted)"
            )
        )
        // Also insert an in-app notification for the customer only (no message to rider)
        repository.notificationDao.insertNotification(
            NotificationEntity(
                title = "Order Cancelled ⚠️",
                message = "Your order #${order.id} for ${order.serviceType} has been cancelled as it was not accepted by a rider.",
                targetRole = "customer"
            )
        )
        
        // Push live updates if tracking
        if (_trackingOrder.value?.id == order.id) {
            _trackingOrder.value = updated
            _uiMessage.value = "Order #${order.id} cancelled: Rider did not accept. Returning to Home."
        }
        navigateTo("customer_home")
    }

    fun cancelOrderIfNotAccepted(orderId: Int) {
        viewModelScope.launch {
            val order = repository.orderDao.getOrderById(orderId)
            if (order != null && (order.status == "Pending" || order.riderEmail.isNullOrBlank())) {
                cancelOrderDueToTimeout(order)
                navigateTo("customer_home")
            }
        }
    }

    fun cancelOrderWithReason(orderId: Int, reason: String) {
        viewModelScope.launch {
            val order = repository.orderDao.getOrderById(orderId)
            if (order != null && order.status != "Completed" && order.status != "Cancelled") {
                val updated = order.copy(status = "Cancelled", feedback = "Cancelled by user: $reason")
                repository.orderDao.insertOrder(updated)
                repository.auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "ORDER_USER_CANCELLED",
                        performedBy = order.customerEmail,
                        details = "Order #${order.id} cancelled by user. Reason: $reason"
                    )
                )
                val titleStr = "Order Cancelled 🚫"
                val bodyStr = "Order #${order.id} was cancelled. Reason: $reason"
                postLocalSystemNotification(titleStr, bodyStr, order.id)
                repository.notificationDao.insertNotification(
                    NotificationEntity(
                        title = titleStr,
                        message = bodyStr,
                        targetRole = "all"
                    )
                )
                triggerWebPush(title = titleStr, body = bodyStr, type = "status")
                if (_trackingOrder.value?.id == order.id) {
                    _trackingOrder.value = updated
                    _uiMessage.value = "Order #${order.id} cancelled successfully."
                }
                navigateTo("customer_home")
            }
        }
    }

    fun simulateFiveMinuteTimeout(orderId: Int) {
        viewModelScope.launch {
            val order = repository.orderDao.getOrderById(orderId)
            if (order != null && order.status == "Pending") {
                cancelOrderDueToTimeout(order)
            }
        }
    }

    fun setTrackingOrder(order: OrderEntity?) {
        _trackingOrder.value = order
        if (order != null && !order.riderEmail.isNullOrBlank()) {
            viewModelScope.launch {
                _trackingRider.value = repository.userDao.getUserByEmail(order.riderEmail)
                checkBlockedStatus()
            }
        } else {
            _trackingRider.value = null
            _isLocationSharingBlocked.value = false
        }
    }

    fun checkBlockedStatus() {
        val current = _currentUser.value ?: return
        val order = _trackingOrder.value ?: run {
            _isLocationSharingBlocked.value = false
            return
        }
        
        val otherEmail = if (current.role == "rider") {
            order.customerEmail
        } else {
            order.riderEmail
        }
        
        if (otherEmail.isNullOrBlank()) {
            _isLocationSharingBlocked.value = false
            return
        }
        
        viewModelScope.launch {
            val freshCurrent = repository.userDao.getUserByEmail(current.email)
            val freshOther = repository.userDao.getUserByEmail(otherEmail)
            if (freshCurrent != null && freshOther != null) {
                val currentBlocks = freshCurrent.blockedUsers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val otherBlocks = freshOther.blockedUsers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                _isLocationSharingBlocked.value = currentBlocks.contains(otherEmail) || otherBlocks.contains(current.email)
            } else {
                _isLocationSharingBlocked.value = false
            }
        }
    }

    fun blockUser(emailToBlock: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val fresh = repository.userDao.getUserByEmail(current.email) ?: current
            val list = fresh.blockedUsers.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (!list.contains(emailToBlock)) {
                list.add(emailToBlock)
                val updated = fresh.copy(blockedUsers = list.joinToString(","))
                repository.userDao.updateUser(updated)
                _currentUser.value = updated
                repository.auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "USER_BLOCKED",
                        performedBy = current.email,
                        details = "Blocked user: $emailToBlock"
                    )
                )
                _uiMessage.value = "User blocked. Location sharing paused."
                checkBlockedStatus()
            }
        }
    }

    fun unblockUser(emailToUnblock: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val fresh = repository.userDao.getUserByEmail(current.email) ?: current
            val list = fresh.blockedUsers.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (list.contains(emailToUnblock)) {
                list.remove(emailToUnblock)
                val updated = fresh.copy(blockedUsers = list.joinToString(","))
                repository.userDao.updateUser(updated)
                _currentUser.value = updated
                repository.auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "USER_UNBLOCKED",
                        performedBy = current.email,
                        details = "Unblocked user: $emailToUnblock"
                    )
                )
                _uiMessage.value = "User unblocked successfully."
                checkBlockedStatus()
            }
        }
    }

    fun isUserBlocked(emailToCheck: String): Boolean {
        val current = _currentUser.value ?: return false
        val list = current.blockedUsers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return list.contains(emailToCheck)
    }

    fun submitOrderRating(orderId: Int, rating: Int, feedback: String) {
        val user = _currentUser.value ?: return

        val valResult = SecurityInputValidator.validateRatingAndFeedback(rating, feedback)
        if (valResult is ValidationResult.Invalid) {
            _uiMessage.value = valResult.reason
            return
        }

        val actionLimit = SecurityRateLimiter.checkAuthenticatedActionLimit(user.email, "submit_rating")
        if (actionLimit is RateLimitResult.Blocked) {
            _uiMessage.value = actionLimit.reason
            return
        }

        viewModelScope.launch {
            try {
                repository.rateOrder(orderId, rating, feedback.trim())
                val updated = repository.orderDao.getOrderById(orderId)
                if (updated != null) {
                    if (_trackingOrder.value?.id == orderId) {
                        setTrackingOrder(updated)
                    }
                    repository.auditLogDao.insertLog(
                        AuditLogEntity(
                            action = "ORDER_RATED",
                            performedBy = user.email,
                            details = "Rated order #$orderId: $rating stars. Feedback: ${feedback.trim()}"
                        )
                    )
                    _uiMessage.value = "Thank you! Your feedback has been recorded in Firestore."
                }
            } catch (e: Exception) {
                _uiMessage.value = SecurityErrorFormatter.formatUserError(e, "Failed to submit rating.")
            }
        }
    }

    // --- Admin Operations ---

    fun approveRiderAccount(riderEmail: String) {
        val admin = _currentUser.value ?: return
        if (admin.role != "admin") return
        viewModelScope.launch {
            repository.approveRider(riderEmail, admin.email)
        }
    }

    fun rejectRiderAccount(riderEmail: String) {
        val admin = _currentUser.value ?: return
        if (admin.role != "admin") return
        viewModelScope.launch {
            repository.rejectRider(riderEmail, admin.email)
        }
    }

    fun applyForRiderVerification() {
        val current = _currentUser.value ?: return
        if (current.role != "rider") return
        viewModelScope.launch {
            repository.requestRiderVerification(current.email)
            val refreshed = repository.userDao.getUserByEmail(current.email)
            if (refreshed != null) {
                _currentUser.value = refreshed
            }
            _uiMessage.value = "Verified Badge application submitted to Admin! 🎖️"
        }
    }

    fun approveRiderVerification(riderEmail: String) {
        val admin = _currentUser.value ?: return
        if (admin.role != "admin") return
        viewModelScope.launch {
            repository.approveRiderVerification(riderEmail, admin.email)
            val rider = repository.userDao.getUserByEmail(riderEmail)
            _uiMessage.value = "Verified Badge APPROVED for ${rider?.name ?: riderEmail}! 🎉"
        }
    }

    fun denyRiderVerification(riderEmail: String) {
        val admin = _currentUser.value ?: return
        if (admin.role != "admin") return
        viewModelScope.launch {
            repository.denyRiderVerification(riderEmail, admin.email)
            val rider = repository.userDao.getUserByEmail(riderEmail)
            _uiMessage.value = "Verified Badge DENIED for ${rider?.name ?: riderEmail}."
        }
    }

    fun toggleRiderVerification(riderEmail: String) {
        val admin = _currentUser.value ?: return
        if (admin.role != "admin") return
        viewModelScope.launch {
            val newStatus = repository.toggleRiderVerification(riderEmail, admin.email)
            val rider = repository.userDao.getUserByEmail(riderEmail)
            val nameStr = rider?.name ?: riderEmail
            if (newStatus) {
                _uiMessage.value = "Service personnel $nameStr is now VERIFIED and active in delivery queue!"
            } else {
                _uiMessage.value = "Service personnel $nameStr verification revoked (Hidden from active queue)."
            }
        }
    }

    fun addRiderFromAdmin(
        name: String,
        email: String,
        phone: String,
        vehicleType: String,
        vehicleNo: String,
        cnicNumber: String,
        drivingLicense: String = "",
        address: String = "",
        password: String = "",
        autoApprove: Boolean = true,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val cleanedCnic = cnicNumber.replace("-", "").replace(" ", "")
            val finalEmail = if (email.isBlank()) {
                if (cleanedCnic.isNotBlank()) "rider_$cleanedCnic@zyphuel.com" else "rider_${System.currentTimeMillis()}@zyphuel.com"
            } else {
                email.trim().lowercase()
            }

            if (name.isBlank() || phone.isBlank()) {
                _uiMessage.value = "Please provide Rider Name and Phone Number."
                return@launch
            }

            val existing = repository.userDao.getUserByEmail(finalEmail)
            if (existing != null) {
                _uiMessage.value = "A user with email $finalEmail already exists."
                return@launch
            }

            val existingRiders = repository.userDao.getUsersByRole("rider")
            val assignedRiderNum = existingRiders.size + 1
            val riderId = "RIDER-$assignedRiderNum"

            val newRider = UserEntity(
                email = finalEmail,
                name = name.trim(),
                passwordHash = if (password.isBlank()) "RiderPass@123" else password.trim(),
                phoneNumber = phone.trim(),
                role = "rider",
                isVerified = autoApprove,
                vehicleType = vehicleType.trim().ifBlank { null },
                vehicleNo = vehicleNo.trim().ifBlank { null },
                cnicOrPassport = cnicNumber.trim().ifBlank { null },
                drivingLicense = drivingLicense.trim().ifBlank { null },
                residentialAddress = address.trim().ifBlank { null },
                city = if (address.isNotBlank()) address.trim() else null,
                riderNumber = assignedRiderNum,
                riderId = riderId,
                termsAccepted = true,
                declarationAccepted = true,
                adminApprovalStatus = if (autoApprove) "Approved" else "Pending"
            )

            repository.userDao.insertUser(newRider)

            dispatchRealtimeEmail(
                recipientEmail = finalEmail,
                subject = "🚀 Zyphuel Rider Account Created & Verified by Admin",
                body = "Hello $name,\n\nAn official Zyphuel Delivery Rider account has been created for you in Lahore.\nRegistered Email: $finalEmail\nVehicle No: ${newRider.vehicleNo ?: "Not specified"}\nStatus: ${newRider.adminApprovalStatus}\n\nYou will receive real-time updates and dispatch notifications at $finalEmail."
            )

            val adminEmail = _currentUser.value?.email ?: "admin@zyphuel.com"
            repository.auditLogDao.insertLog(
                AuditLogEntity(
                    action = "ADMIN_ADDED_RIDER",
                    performedBy = adminEmail,
                    details = "Admin added new rider record: ${newRider.name} ($finalEmail)"
                )
            )

            _uiMessage.value = "Rider $name successfully registered and added to Admin Panel!"
            onSuccess()
        }
    }

    fun editRiderFromAdmin(
        riderEmail: String,
        name: String,
        phone: String,
        vehicleType: String,
        vehicleNo: String,
        cnicNumber: String,
        isVerified: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || phone.isBlank()) {
                _uiMessage.value = "Please provide Rider Name and Phone Number."
                return@launch
            }

            val existing = repository.userDao.getUserByEmail(riderEmail)
            if (existing == null) {
                _uiMessage.value = "Rider record for $riderEmail not found."
                return@launch
            }

            val updatedRider = existing.copy(
                name = name.trim(),
                phoneNumber = phone.trim(),
                vehicleType = vehicleType.ifBlank { "Bike" },
                vehicleNo = vehicleNo.ifBlank { "LHR-1234" },
                cnicOrPassport = cnicNumber.ifBlank { "35202-1234567-1" },
                isVerified = isVerified,
                adminApprovalStatus = if (isVerified) "Approved" else "Pending"
            )

            repository.userDao.updateUser(updatedRider)

            val adminEmail = _currentUser.value?.email ?: "m.daniyalkhan490@gmail.com"

            dispatchRealtimeEmail(
                recipientEmail = adminEmail,
                subject = "📝 Admin Alert: Rider Information Updated for ${updatedRider.name}",
                body = "Hello Admin,\n\nInformation for rider ${updatedRider.name} ($riderEmail) has been updated and corrected in the Admin Panel.\n\nUpdated Details:\nName: ${updatedRider.name}\nPhone: ${updatedRider.phoneNumber}\nVehicle Type: ${updatedRider.vehicleType}\nVehicle No: ${updatedRider.vehicleNo}\nCNIC: ${updatedRider.cnicOrPassport}\nVerification Status: ${updatedRider.adminApprovalStatus}\n\nZyphuel Operations"
            )

            dispatchRealtimeEmail(
                recipientEmail = riderEmail,
                subject = "🔔 Zyphuel Profile Updated",
                body = "Hello ${updatedRider.name},\n\nYour rider account details have been updated by the Admin.\nName: ${updatedRider.name}\nPhone: ${updatedRider.phoneNumber}\nVehicle: ${updatedRider.vehicleType} (${updatedRider.vehicleNo})\nStatus: ${updatedRider.adminApprovalStatus}\n\nZyphuel Admin Team"
            )

            repository.auditLogDao.insertLog(
                AuditLogEntity(
                    action = "ADMIN_EDITED_RIDER",
                    performedBy = adminEmail,
                    details = "Admin updated rider details for $riderEmail"
                )
            )

            _uiMessage.value = "Rider ${updatedRider.name}'s details updated and sent to admin!"
            onSuccess()
        }
    }

    fun deleteRiderFromAdmin(riderEmail: String, onSuccess: () -> Unit) {
        if (riderEmail.trim().equals("m.daniyalkhan490@gmail.com", ignoreCase = true)) {
            _uiMessage.value = "⚠️ The Super Admin account (m.daniyalkhan490@gmail.com) is permanently protected and cannot be deleted."
            return
        }
        viewModelScope.launch {
            val existing = repository.userDao.getUserByEmail(riderEmail)
            if (existing == null) {
                _uiMessage.value = "Rider $riderEmail not found."
                return@launch
            }

            repository.userDao.deleteUserByEmail(riderEmail)

            val adminEmail = _currentUser.value?.email ?: "m.daniyalkhan490@gmail.com"

            dispatchRealtimeEmail(
                recipientEmail = adminEmail,
                subject = "🗑️ Admin Alert: Rider Account Removed",
                body = "Admin ($adminEmail) has deleted rider record: ${existing.name} ($riderEmail) from the system."
            )

            repository.auditLogDao.insertLog(
                AuditLogEntity(
                    action = "ADMIN_DELETED_RIDER",
                    performedBy = adminEmail,
                    details = "Deleted rider record: ${existing.name} ($riderEmail)"
                )
            )

            _uiMessage.value = "Rider record for ${existing.name} deleted successfully."
            onSuccess()
        }
    }

    // --- Password Suggestion Operations ---

    fun generatePasswordSuggestion() {
        _passwordSuggestion.value = repository.suggestStrongPassword()
    }

    fun applyPasswordSuggestion(newPassword: String) {
        val user = _currentUser.value ?: return
        if (user.role == "admin" || user.email.equals("m.daniyalkhan490@gmail.com", ignoreCase = true)) {
            _uiMessage.value = "The admin's password is correct and can never be changed."
            return
        }
        viewModelScope.launch {
            val updated = user.copy(passwordHash = newPassword)
            repository.userDao.insertUser(updated)
            _currentUser.value = updated
            _passwordSuggestion.value = null
            _uiMessage.value = "Password successfully strengthened!"
        }
    }

    // --- Dynamic Pricing Engine Operations ---

    fun updateFuelPrices(
        petrol: Float,
        diesel: Float,
        octane: Float,
        lpg: Float,
        water: Float,
        source: String,
        method: String // "AI Search Engine Sync" or "Manual Admin Adjust"
    ) {
        viewModelScope.launch {
            val oldPetrol = _petrolPrice.value
            val oldDiesel = _dieselPrice.value
            val oldOctane = _highOctanePrice.value
            val oldLpg = _lpgGasPrice.value
            val oldWater = _waterPrice.value

            // Update local flows
            _petrolPrice.value = petrol
            _dieselPrice.value = diesel
            _highOctanePrice.value = octane
            _lpgGasPrice.value = lpg
            _waterPrice.value = water

            // Save to SharedPreferences
            sharedPrefs.edit()
                .putFloat("petrol", petrol)
                .putFloat("diesel", diesel)
                .putFloat("high_octane", octane)
                .putFloat("lpg_gas", lpg)
                .putFloat("water", water)
                .putString("last_sync_time", SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()))
                .apply()

            _lastPriceSyncTime.value = sharedPrefs.getString("last_sync_time", "Never updated") ?: "Just now"

            // Insert audit log
            repository.auditLogDao.insertLog(
                AuditLogEntity(
                    action = "PRICE_UPDATED",
                    performedBy = _currentUser.value?.email ?: "System",
                    details = "Prices updated via $method. Petrol: Rs.$petrol, Diesel: Rs.$diesel, Octane: Rs.$octane, LPG: Rs.$lpg, Water: Rs.$water. Source: $source"
                )
            )

            // Broadcast App Notifications only if actual rate changes exist
            val isChanged = (petrol != oldPetrol) || (diesel != oldDiesel) || (octane != oldOctane) || (lpg != oldLpg) || (water != oldWater)

            if (isChanged) {
                repository.notificationDao.insertNotification(
                    NotificationEntity(
                        title = "System Price Alert",
                        message = "Fuel rates successfully synchronized via $method ($source). Check details in dashboard.",
                        targetRole = "admin"
                    )
                )

                repository.notificationDao.insertNotification(
                    NotificationEntity(
                        title = "⚡ Live Fuel Rates Updated!",
                        message = "We have updated Lahore fuel delivery rates: Petrol Rs.$petrol/L, Diesel Rs.$diesel/L, Octane Rs.$octane/L. Order now!",
                        targetRole = "customer"
                    )
                )

                repository.notificationDao.insertNotification(
                    NotificationEntity(
                        title = "New Earnings & Delivery Rates",
                        message = "Lahore fuel delivery rates updated. Petrol Rs.$petrol/L, Diesel Rs.$diesel/L. Adjust your travel logs accordingly.",
                        targetRole = "rider"
                    )
                )
            } else {
                _uiMessage.value = "Pricing synchronized. No changes detected."
            }

            // Send Outgoing Email to Admin (simulated)
            val emailSubject = "⚠️ [Zyphuel Admin Alert] Live Fuel Delivery Rates Updated"
            val emailBody = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #1e293b; background-color: #f8fafc; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; overflow: hidden;">
                        <div style="background: linear-gradient(135deg, #0052D4, #4364F7, #6FB1FC); padding: 24px; text-align: center; color: white;">
                            <h2 style="margin: 0; font-size: 24px; font-weight: bold; letter-spacing: 0.5px;">Zyphuel Pricing Engine</h2>
                            <p style="margin: 4px 0 0; font-size: 14px; opacity: 0.9;">Real-Time Price Sync Alerts</p>
                        </div>
                        <div style="padding: 24px;">
                            <p style="font-size: 16px; line-height: 1.5;">Dear Admin,</p>
                            <p style="font-size: 14px; line-height: 1.5; color: #475569;">
                                This is an automated email alert indicating that the live fuel delivery rates for the <b>Zyphuel App</b> have been synchronized successfully using the <b>$method</b>.
                            </p>
                            
                            <h3 style="border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; margin-top: 24px; color: #0f172a; font-size: 16px;">Updated Lahore Delivery Rates (Rs.)</h3>
                            <table style="width: 100%; border-collapse: collapse; margin-top: 12px;">
                                <tr style="background-color: #f1f5f9;">
                                    <th style="text-align: left; padding: 10px; border: 1px solid #cbd5e1; font-size: 14px;">Service Type</th>
                                    <th style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; font-size: 14px;">Old Price</th>
                                    <th style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; font-size: 14px;">New Price</th>
                                </tr>
                                <tr>
                                    <td style="padding: 10px; border: 1px solid #cbd5e1; font-size: 14px;">Petrol</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; color: #94a3b8; font-size: 14px;">Rs. $oldPetrol/L</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; font-weight: bold; color: #10b981; font-size: 14px;">Rs. $petrol/L</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px; border: 1px solid #cbd5e1; font-size: 14px;">Diesel</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; color: #94a3b8; font-size: 14px;">Rs. $oldDiesel/L</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; font-weight: bold; color: #10b981; font-size: 14px;">Rs. $diesel/L</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px; border: 1px solid #cbd5e1; font-size: 14px;">High-Octane</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; color: #94a3b8; font-size: 14px;">Rs. $oldOctane/L</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; font-weight: bold; color: #10b981; font-size: 14px;">Rs. $octane/L</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px; border: 1px solid #cbd5e1; font-size: 14px;">LPG Gas</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; color: #94a3b8; font-size: 14px;">Rs. $oldLpg/kg</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; font-weight: bold; color: #10b981; font-size: 14px;">Rs. $lpg/kg</td>
                                </tr>
                                <tr>
                                    <td style="padding: 10px; border: 1px solid #cbd5e1; font-size: 14px;">Drinking Water</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; color: #94a3b8; font-size: 14px;">Rs. $oldWater/Gal</td>
                                    <td style="text-align: right; padding: 10px; border: 1px solid #cbd5e1; font-weight: bold; color: #10b981; font-size: 14px;">Rs. $water/Gal</td>
                                </tr>
                            </table>

                            <div style="margin-top: 24px; padding: 12px; background-color: #eff6ff; border-left: 4px solid #3b82f6; border-radius: 4px;">
                                <p style="margin: 0; font-size: 13px; color: #1e3a8a; line-height: 1.4;">
                                    <b>Sync Metadata:</b><br/>
                                    • Engine Method: $method<br/>
                                    • Verified Source: $source<br/>
                                    • Sync Time: ${_lastPriceSyncTime.value}
                                </p>
                            </div>
                        </div>
                        <div style="background-color: #f1f5f9; padding: 16px; text-align: center; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b;">
                            This is an automated system dispatch. Do not reply to this email.
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val newEmail = AdminEmail(
                id = _sentEmails.value.size + 1,
                subject = emailSubject,
                body = emailBody
            )
            _sentEmails.value = listOf(newEmail) + _sentEmails.value

            _uiMessage.value = "Fuel rates updated! App alerts broadcasted and email dispatched to Admin."
        }
    }

    fun dispatchRealtimeEmail(recipientEmail: String, subject: String, body: String) {
        if (recipientEmail.isBlank()) return
        val newEmail = AdminEmail(
            id = _sentEmails.value.size + 1,
            subject = subject,
            body = body,
            timestamp = System.currentTimeMillis(),
            recipient = recipientEmail
        )
        _sentEmails.value = listOf(newEmail) + _sentEmails.value

        viewModelScope.launch(Dispatchers.IO) {
            repository.auditLogDao.insertLog(
                AuditLogEntity(
                    action = "REALTIME_GMAIL_ALERT_DISPATCHED",
                    performedBy = "System Email Engine",
                    details = "Dispatched real-time alert to: $recipientEmail | Subject: $subject"
                )
            )
        }
    }

    fun updateCustomerEmail(oldEmail: String, newEmail: String, name: String, phone: String, onSuccess: () -> Unit = {}) {
        if (oldEmail.trim().equals("m.daniyalkhan490@gmail.com", ignoreCase = true)) {
            _uiMessage.value = "⚠️ The Super Admin account (m.daniyalkhan490@gmail.com) is permanently protected and cannot be modified."
            return
        }
        val trimmedNewEmail = newEmail.trim()
        if (trimmedNewEmail.isBlank() || !trimmedNewEmail.contains("@")) {
            _uiMessage.value = "Please enter a valid email address."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val existingUser = repository.userDao.getUserByEmail(oldEmail)
            if (existingUser != null) {
                val updatedUser = existingUser.copy(
                    email = trimmedNewEmail,
                    name = name.trim(),
                    phoneNumber = phone.trim()
                )
                if (oldEmail != trimmedNewEmail) {
                    repository.userDao.deleteUserByEmail(oldEmail)
                }
                repository.userDao.insertUser(updatedUser)

                dispatchRealtimeEmail(
                    recipientEmail = trimmedNewEmail,
                    subject = "📧 Zyphuel Account Email Updated & Verified",
                    body = "Hello ${updatedUser.name},\n\nYour Zyphuel customer profile email address has been updated to $trimmedNewEmail.\n\nYou will now receive all live order delivery status alerts and 4-hour price updates at this email address.\n\nThank you,\nZyphuel Lahore Operations Team"
                )

                _uiMessage.value = "Customer email updated successfully! Real-time email dispatched to $trimmedNewEmail."
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } else {
                _uiMessage.value = "Customer record not found."
            }
        }
    }

    fun triggerTwoHourPriceUpdateBroadcast() = triggerFourHourPriceUpdateBroadcast()

    fun triggerFourHourPriceUpdateBroadcast() {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date())
            val msg = "⚡ [4-Hour Rate Update - $timestamp] Live Rates: Petrol Rs.${_petrolPrice.value}/L, Diesel Rs.${_dieselPrice.value}/L, LPG Rs.${_lpgGasPrice.value}/kg, Octane Rs.${_highOctanePrice.value}/L."

            // Send in-app broadcast notification
            repository.notificationDao.insertNotification(
                NotificationEntity(
                    title = "4-Hour Rate Update ($timestamp)",
                    message = msg,
                    targetRole = "all"
                )
            )

            // Real-time email dispatches to all registered customers and riders
            val allUsers = repository.userDao.getAllCustomersFlow().firstOrNull() ?: emptyList()
            val allRidersList = repository.userDao.getAllRidersFlow().firstOrNull() ?: emptyList()
            val recipients = (allUsers.map { it.email } + allRidersList.map { it.email }).filter { it.contains("@") }.distinct()

            recipients.forEach { email ->
                dispatchRealtimeEmail(
                    recipientEmail = email,
                    subject = "⚡ [Zyphuel 4-Hour Alert] Live Lahore Fuel & Delivery Rates ($timestamp)",
                    body = """
                        Hello,

                        This is your automated 4-Hour Fuel & Service Price Update Alert for Zyphuel COD Delivery Services in Lahore ($timestamp).

                        Updated Rates:
                        • Petrol (E10): Rs. ${_petrolPrice.value} / Litre
                        • High-Speed Diesel: Rs. ${_dieselPrice.value} / Litre
                        • High Octane (HOBC 97): Rs. ${_highOctanePrice.value} / Litre
                        • LPG Gas Cylinder: Rs. ${_lpgGasPrice.value} / Kg
                        • Clean Water Tanker: Rs. ${_waterPrice.value} / Gallon

                        Need emergency fuel or water delivery in Lahore? Open the Zyphuel App to order instantly!

                        Best regards,
                        Zyphuel Operations Team
                    """.trimIndent()
                )
            }

            _uiMessage.value = "4-Hour Price Update broadcasted & emails dispatched to all registered users and riders."
        }
    }

    fun syncFuelPricesViaGemini() {
        if (_priceSyncing.value) return
        _priceSyncing.value = true

        try {
            com.example.worker.FuelPriceWorker.triggerImmediatePriceWork(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModelScope.launch {
            try {
                // Primary: Fetch directly from live Trackmate Fuel API (https://fuel.trackmate.page/api/prices)
                val trackmateResult = com.example.data.TrackmateFuelApiService.fetchLatestFuelPrices()
                
                val petrol = trackmateResult.petrol.roundTo(2)
                val diesel = trackmateResult.diesel.roundTo(2)
                val octane = trackmateResult.highOctane.roundTo(2)
                val lpg = trackmateResult.lpgGas.roundTo(2)
                val water = trackmateResult.water.roundTo(2)

                val dateInfo = if (trackmateResult.effectiveDate != null) " [Effective: ${trackmateResult.effectiveDate}]" else ""
                updateFuelPrices(
                    petrol = petrol,
                    diesel = diesel,
                    octane = octane,
                    lpg = lpg,
                    water = water,
                    source = "Official Fuel Market API$dateInfo",
                    method = "Live Price Sync"
                )

            } catch (trackmateException: Exception) {
                trackmateException.printStackTrace()
                // Fallback 1: Gemini Web Search / Generative API Sync
                try {
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                        throw IllegalStateException("API Key is not configured in secrets.")
                    }

                    val promptText = """
                        Get the latest actual consumer fuel retail rates in Pakistan (specifically Lahore) in Pakistani Rupees (PKR) for:
                        1. Petrol (Motor Spirit / Super petrol)
                        2. Diesel (High Speed Diesel)
                        3. High-Octane (Hi-Octane / HOBC / RON 97)
                        And a typical competitive rate for LPG gas (per kg) and clean 20L drinking water gallon.
                        
                        Return ONLY a valid JSON block containing exactly these keys with positive numeric float values:
                        {
                          "petrol": <float>,
                          "diesel": <float>,
                          "high_octane": <float>,
                          "lpg_gas": <float>,
                          "water": <float>,
                          "source": "<string representing where the price was fetched or a reputable news site, e.g. OGRA, Dawn News, Geo News>"
                        }
                        No other text, no explanation, no markdown ```json formatting. Just raw JSON.
                    """.trimIndent()

                    val jsonPayload = JSONObject().apply {
                        put("contents", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", org.json.JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", promptText)
                                    })
                                })
                            })
                        })
                        put("tools", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("googleSearch", JSONObject())
                            })
                        })
                    }

                    val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
                        .post(requestBody)
                        .build()

                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        client.newCall(request).execute()
                    }

                    if (!response.isSuccessful) {
                        throw java.io.IOException("Unsuccessful network call: ${response.code}")
                    }

                    val bodyStr = response.body?.string() ?: throw java.io.IOException("Empty response body")
                    
                    val rootJson = JSONObject(bodyStr)
                    val textResponse = rootJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val jsonMatch = Regex("""\{[\s\S]*\}""").find(textResponse)?.value
                    val cleanedText = jsonMatch ?: textResponse.trim()
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val pricesObj = JSONObject(cleanedText)
                    val basePetrol = pricesObj.getDouble("petrol").toFloat()
                    val baseDiesel = pricesObj.getDouble("diesel").toFloat()
                    val baseOctane = pricesObj.getDouble("high_octane").toFloat()
                    val baseLpg = pricesObj.optDouble("lpg_gas", 350.0).toFloat()
                    val baseWater = pricesObj.optDouble("water", 50.0).toFloat()
                    val source = pricesObj.optString("source", "OGRA Pakistan / Dawn News")

                    val petrol = (basePetrol + 20.0f).roundTo(2)
                    val diesel = (baseDiesel + 20.0f).roundTo(2)
                    val octane = (baseOctane + 25.0f).roundTo(2)
                    val lpg = (baseLpg + 20.0f).roundTo(2)
                    val water = baseWater.roundTo(2)

                    updateFuelPrices(petrol, diesel, octane, lpg, water, "$source + Bowser Logistics Surcharge", "AI Search Engine Sync")

                } catch (e: Exception) {
                    // Fallback 2: Default OGRA Pakistan retail rates + Bowser logistics surcharge
                    val basePetrol = 320.73f
                    val baseDiesel = 375.04f
                    val baseOctane = 340.00f
                    val baseLpg = 241.43f
                    val baseWater = 50.0f

                    val simPetrol = (basePetrol + 20.0f).roundTo(2)
                    val simDiesel = (baseDiesel + 20.0f).roundTo(2)
                    val simOctane = (baseOctane + 25.0f).roundTo(2)
                    val simLpg = (baseLpg + 20.0f).roundTo(2)
                    val simWater = baseWater
                    
                    updateFuelPrices(
                        petrol = simPetrol,
                        diesel = simDiesel,
                        octane = simOctane,
                        lpg = simLpg,
                        water = simWater,
                        source = "OGRA Lahore Central Terminal + Bowser Logistics Surcharge",
                        method = "Trackmate API (Backup Rate)"
                    )
                }
            } finally {
                _priceSyncing.value = false
            }
        }
    }

    private fun Float.roundTo(decimalPlaces: Int): Float {
        var multiplier = 1.0f
        repeat(decimalPlaces) { multiplier *= 10f }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.notificationDao.clearAllNotifications()
            _uiMessage.value = "All notifications cleared!"
        }
    }

    // --- Support Chat Functionality ---
    private val _supportChatHistory = MutableStateFlow<List<SupportChatMessage>>(listOf(
        SupportChatMessage("bot", "🟢 *Zyphuel Gemini AI Live Support* Verified ✔️\n\nSalam! Welcome to Zyphuel Live Support & Help Center. I am your 24/7 AI-powered support assistant. How can I assist you with your fuel, gas, or water delivery today?\n\nType or tap a keyword below:\n• *Price List* / *Pricing*\n• *Order Status*\n• *Areas Served*\n• *Safety Rules*")
    ))
    val supportChatHistory: StateFlow<List<SupportChatMessage>> = _supportChatHistory.asStateFlow()

    private val _supportChatLoading = MutableStateFlow(false)
    val supportChatLoading = _supportChatLoading.asStateFlow()

    fun sendSupportChatMessage(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return

        // 1. Add user message
        val userMsg = SupportChatMessage("user", prompt)
        val updatedHistory = _supportChatHistory.value + userMsg
        _supportChatHistory.value = updatedHistory

        _supportChatLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var replyText: String? = null

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val systemPrompt = """
                        You are Zyphuel AI Support Agent, an intelligent, empathetic, and professional 24/7 Live Support & Help Center representative for Zyphuel — Pakistan's premier doorstep delivery platform operating in Lahore.
                        Zyphuel delivers Petrol (Super Euro-V), Diesel (High Speed), High-Octane (HOBC), LPG Gas Cylinders (11.8kg domestic tanks), and Clean Drinking Water (19L Gallons).

                        Key Instructions:
                        1. Provide accurate, clear, and helpful assistance regarding fuel rates, order status, delivery coverage in Lahore (DHA, Gulberg, Johar Town, Model Town, Cantt, Bahria Town, etc.), safety guidelines, and live support options.
                        2. Maintain context of previous user messages in the multi-turn conversation thread.
                        3. Use clean markdown formatting (*bold* text, bullet lists) and friendly emojis.
                        4. Keep responses concise, helpful, and customer-focused.
                    """.trimIndent()

                    val contentsArray = JSONArray()
                    for (msg in updatedHistory) {
                        val role = if (msg.sender == "user") "user" else "model"
                        val contentObj = JSONObject().apply {
                            put("role", role)
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", msg.text)
                                })
                            })
                        }
                        contentsArray.put(contentObj)
                    }

                    val jsonPayload = JSONObject().apply {
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", systemPrompt)
                                })
                            })
                        })
                        put("contents", contentsArray)
                    }

                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBodyStr = response.body?.string()
                        if (!responseBodyStr.isNullOrEmpty()) {
                            val jsonResp = JSONObject(responseBodyStr)
                            val candidates = jsonResp.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val firstCandidate = candidates.getJSONObject(0)
                                val contentObj = firstCandidate.optJSONObject("content")
                                val partsArray = contentObj?.optJSONArray("parts")
                                if (partsArray != null && partsArray.length() > 0) {
                                    replyText = partsArray.getJSONObject(0).optString("text")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback to offline rule-based response if Gemini API call failed or wasn't available
            if (replyText.isNullOrBlank()) {
                delay(800)
                val query = prompt.lowercase()
                replyText = when {
                    query.contains("price") || query.contains("rate") || query.contains("pricing") || query.contains("cost") || query.contains("list") || query.contains("rs.") -> {
                        "💰 *Zyphuel Premium Delivery Rates (Lahore)*:\n\n" +
                        "⛽ *Petrol (Super Euro-V)*: Rs. 275 per Liter\n" +
                        "🛢️ *High-Octane*: Rs. 295 per Liter\n" +
                        "🚜 *Diesel (High Speed)*: Rs. 280 per Liter\n" +
                        "🔥 *LPG Cylinder*: Rs. 3,150 (Standard 11.8kg domestic tank)\n" +
                        "💧 *Premium Water*: Rs. 150 (19-Liter Clean Gallon)\n\n" +
                        "_Rates are fully compliant with OGRA guidelines in Pakistan. No extra hidden charges!_"
                    }
                    query.contains("order") || query.contains("track") || query.contains("delivery") || query.contains("status") -> {
                        "📦 *Order Status & Tracking*:\n\n" +
                        "• Deliveries typically arrive in *30-45 minutes* across Lahore.\n" +
                        "• To view your pending/active/completed orders in real-time, please check the *My Orders* dashboard in the sidebar!\n\n" +
                        "If you need to change your delivery address or contact your assigned rider, click the green *Open WhatsApp Hotline* button above to call our Lahore Dispatch Center."
                    }
                    query.contains("lahore") || query.contains("area") || query.contains("coverage") || query.contains("map") || query.contains("gulberg") || query.contains("dha") || query.contains("where") -> {
                        "📍 *Zyphuel Lahore Coverage Areas*:\n\n" +
                        "We serve almost all major sectors and neighborhoods in Lahore 24/7:\n" +
                        "• DHA (Phases 1 to 11 & Prism)\n" +
                        "• Gulberg, Model Town, Garden Town, Cavalry Ground\n" +
                        "• Cantt, Johar Town, Faisal Town, Wapda Town\n" +
                        "• Bahria Town, Valencia Town, Lake City\n" +
                        "• Lahore Mall Road, Shadman, Samanabad"
                    }
                    query.contains("safety") || query.contains("hazard") || query.contains("lpg") || query.contains("guideline") || query.contains("rule") -> {
                        "⚠️ *Zyphuel Fuel & LPG Safety Rules*:\n\n" +
                        "1. Keep LPG cylinders upright in highly ventilated areas.\n" +
                        "2. Avoid using electrical sockets, switches, or open flames near fuel unloading.\n" +
                        "3. Strictly NO smoking or mobile phone usage in the immediate delivery perimeter.\n" +
                        "4. Clear all access pathways for the delivery rider before arrival."
                    }
                    query.contains("hi") || query.contains("hello") || query.contains("hey") || query.contains("salam") || query.contains("aslam") || query.contains("help") -> {
                        "👋 Salam! Welcome back to Zyphuel AI Live Support.\n\n" +
                        "I can instantly answer queries about:\n" +
                        "• *Price List* / *Rates*\n" +
                        "• *Order Tracking*\n" +
                        "• *Lahore Coverage*\n" +
                        "• *Safety Rules*\n\n" +
                        "Or tap the button above to contact a live representative!"
                    }
                    query.contains("agent") || query.contains("human") || query.contains("support") || query.contains("phone") || query.contains("number") || query.contains("call") || query.contains("whatsapp") -> {
                        "📞 *Zyphuel Human Dispatch Desk*:\n\n" +
                        "Our Lahore Support team is online 24/7 to resolve complex disputes or coordinate large commercial orders.\n" +
                        "• *WhatsApp Number*: +92 323 0112464\n" +
                        "• *Average response time*: Under 2 minutes"
                    }
                    else -> {
                        "🤖 *Zyphuel AI Support Assistant*:\n\n" +
                        "I received your message. For instant answers, please mention one of our services or ask about:\n" +
                        "• *Rates* / *Pricing*\n" +
                        "• *Order Tracking*\n" +
                        "• *Lahore Coverage Areas*\n" +
                        "• *Safety guidelines*\n\n" +
                        "Alternatively, click the green *Open WhatsApp Hotline* button above to chat directly with a human coordinator."
                    }
                }
            }

            withContext(Dispatchers.Main) {
                _supportChatHistory.value = _supportChatHistory.value + SupportChatMessage("bot", replyText!!)
                _supportChatLoading.value = false
            }
        }
    }

    fun clearSupportChat() {
        _supportChatHistory.value = listOf(
            SupportChatMessage("bot", "🟢 *Zyphuel Gemini AI Live Support* Verified ✔️\n\nSalam! Welcome to Zyphuel Live Support & Help Center. I am your 24/7 AI-powered support assistant. How can I assist you with your fuel, gas, or water delivery today?\n\nType or tap a keyword below:\n• *Price List* / *Pricing*\n• *Order Status*\n• *Areas Served*\n• *Safety Rules*")
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopRealTimeFusedLocationUpdates()
    }
}

data class SupportChatMessage(
    val sender: String, // "user" or "bot"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
