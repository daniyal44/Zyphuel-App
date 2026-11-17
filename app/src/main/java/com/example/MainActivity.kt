package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.security.BiometricSecurityManager
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DebugLogger
import com.example.util.GlobalErrorBoundary
import com.example.util.LocalAppLanguage
import com.example.util.UnifiedAssetManager

class MainActivity : FragmentActivity() {
    private var viewModel: MainViewModel? = null
    private var initError: Throwable? = null

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Global Crash Handler ──
        // Catches ANY uncaught exception and saves the full stacktrace to a file
        // so the developer can diagnose instant-close crashes without adb logcat.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashFile = java.io.File(filesDir, "crash_log.txt")
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                val stackTrace = android.util.Log.getStackTraceString(throwable)
                val prevContent = if (crashFile.exists()) crashFile.readText() else ""
                crashFile.writeText("=== ZYPHUEL CRASH LOG ===\nTime: $timestamp\nThread: ${thread.name}\nException: ${throwable::class.java.name}: ${throwable.message}\n\nFull Stack Trace:\n$stackTrace\n\n--- Previous Log ---\n$prevContent")
                DebugLogger.e("CRASH_HANDLER", "App crashed: ${throwable.message}", throwable)
            } catch (_: Exception) { /* best-effort */ }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        enableEdgeToEdge()

        DebugLogger.i("MainActivity", "Initializing Zyphuel Application Activity")
        try {
            com.example.util.SigningDiagnosticUtil.logAppSigningSha1(this)
        } catch (e: Exception) {
            DebugLogger.e("MainActivity", "SigningDiagnosticUtil failed: ${e.message}", e)
        }
        try {
            UnifiedAssetManager.verifyAssetIntegrity(this)
        } catch (e: Exception) {
            DebugLogger.e("MainActivity", "UnifiedAssetManager failed: ${e.message}", e)
        }

        try {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.cancelAll()
        } catch (e: Exception) {
            DebugLogger.w("MainActivity", "Could not clear existing system notifications: ${e.message}")
        }

        // Initialize MainViewModel — if it fails, show a recovery screen instead of crashing
        try {
            viewModel = ViewModelProvider(this)[MainViewModel::class.java]
            DebugLogger.i("MainActivity", "MainViewModel initialized successfully")
        } catch (e: Exception) {
            DebugLogger.e("MainActivity", "CRITICAL: MainViewModel init crashed: ${e.message}", e)
            initError = e
            try {
                val crashFile = java.io.File(filesDir, "crash_log.txt")
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                crashFile.writeText("=== VIEWMODEL INIT CRASH ===\nTime: $timestamp\n${android.util.Log.getStackTraceString(e)}\n")
            } catch (_: Exception) {}
            // DO NOT re-throw — show recovery UI instead
        }

        // Only run post-init code if ViewModel succeeded
        if (viewModel != null) {
            try {
                viewModel!!.refreshSecurityAndBiometricStates(this)
            } catch (e: Exception) {
                DebugLogger.e("MainActivity", "refreshSecurityAndBiometricStates crashed: ${e.message}", e)
            }

            // Track unique app install in Firestore & pre-fetch download count for Admin Dashboard
            try {
                viewModel!!.trackAppInstall(this)
                viewModel!!.fetchAppDownloadCount()
            } catch (e: Exception) {
                DebugLogger.e("MainActivity", "trackAppInstall/fetchDownloadCount failed: ${e.message}", e)
            }

            // Request runtime permissions early so live delivery tracking works without a hitch:
            //  - Location (FINE/COARSE): customer destination fix + rider GPS foreground service.
            //  - POST_NOTIFICATIONS (Android 13+): delivery + tracking notifications.
            run {
                val needed = mutableListOf<String>()
                val hasFine = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasFine && !hasCoarse) {
                    needed.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    needed.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    needed.add(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                if (needed.isNotEmpty()) {
                    requestPermissions(needed.toTypedArray(), 101)
                }
            }

            // Process notification intent launch
            handleNotificationIntent(intent)

            // Schedule periodic background WorkManager task for real-time fuel price updates & notifications
            try {
                com.example.worker.FuelPriceWorker.schedulePeriodicPriceWork(this)
                com.example.worker.FuelPriceWorker.triggerImmediatePriceWork(this)
            } catch (e: Exception) {
                DebugLogger.e("MainActivity", "FuelPriceWorker scheduling failed: ${e.message}", e)
            }
        }

        setContent {
            // If ViewModel init failed, show a recovery screen instead of crashing
            if (viewModel == null) {
                CrashRecoveryScreen(
                    error = initError,
                    onClearDataAndRetry = {
                        // Delete the Room database and all SharedPreferences, then restart
                        try {
                            applicationContext.deleteDatabase("zyphuel_database")
                            applicationContext.getSharedPreferences("zyphuel_prices", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                            applicationContext.getSharedPreferences("zyphuel_session", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                            applicationContext.getSharedPreferences("zyphuel_ui_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                        } catch (_: Exception) {}
                        // Restart the activity
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                        Runtime.getRuntime().exit(0)
                    },
                    onRetryWithoutClearing = {
                        recreate()
                    }
                )
                return@setContent
            }
            val vm = viewModel!!
            MyApplicationTheme {
                // --- Multi-Language + RTL root ---
                // Feed the in-app selected language into the Compose tree. LocalAppLanguage is a
                // staticCompositionLocalOf, so changing it recomposes the entire subtree — every
                // tr()/trStatus() label re-renders in one go. LocalLayoutDirection mirrors the whole
                // UI for RTL scripts (Urdu, Arabic, Sindhi, Pashto, Persian).
                val appLanguage by vm.currentLanguage.collectAsState()
                val appLayoutDirection = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(
                    LocalAppLanguage provides appLanguage,
                    LocalLayoutDirection provides appLayoutDirection
                ) {
                GlobalErrorBoundary(
                    onResetAppState = {
                        DebugLogger.i("MainActivity", "Resetting App State from Error Boundary")
                        vm.navigateTo("customer_home")
                    }
                ) {
                    val currentScreen by vm.currentScreen.collectAsState()
                    val isTransitioningPlatform by vm.isTransitioningPlatform.collectAsState()
                    val transitionTargetPlatformName by vm.transitionTargetPlatformName.collectAsState()
                    val uiMessage by vm.uiMessage.collectAsState()

                    // Center notification engine & App open price notification preference check
                    LaunchedEffect(Unit) {
                        vm.checkAppOpenNotificationChoicePrompt(this@MainActivity)
                    }

                    LaunchedEffect(uiMessage) {
                        uiMessage?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                            vm.clearMessage()
                        }
                    }

                    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                            SharedTransitionLayout {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        if (targetState == "tracker") {
                                            (slideInVertically(initialOffsetY = { it / 3 }, animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) +
                                                    fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.92f)) togetherWith
                                                    (fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.96f))
                                        } else if (initialState == "tracker") {
                                            (fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.96f)) togetherWith
                                                    (slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) +
                                                            fadeOut(animationSpec = tween(350)) + scaleOut(targetScale = 0.92f))
                                        } else {
                                            fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
                                        }
                                    },
                                    label = "screen_routing"
                                ) { screen ->
                                    CompositionLocalProvider(
                                        LocalSharedTransitionScope provides this@SharedTransitionLayout,
                                        LocalAnimatedVisibilityScope provides this@AnimatedContent
                                    ) {
                                        when (screen) {
                                            "splash" -> SplashScreen(vm)
                                            "portal_select" -> {
                                                // Portal Selection removed — redirect to login
                                                LaunchedEffect(Unit) { vm.navigateTo("login_customer") }
                                            }
                                            "onboarding" -> OnboardingScreen(vm)
                                            "login_customer" -> AuthScreen(vm, isRegister = false, isRider = false)
                                            "register_customer" -> AuthScreen(vm, isRegister = true, isRider = false)
                                            "login_rider" -> {
                                                LaunchedEffect(Unit) {
                                                    vm.navigateTo("login_customer")
                                                }
                                            }
                                            "register_rider" -> AuthScreen(vm, isRegister = true, isRider = true)
                                            "customer_home" -> CustomerHomeScreen(vm)
                                            "customer_order_history" -> CustomerOrderHistoryScreen(vm, onBack = { vm.navigateTo("customer_home") })
                                            "rider_home" -> RiderHomeScreen(vm)
                                            "rider_complete_profile" -> RiderCompleteProfileScreen(vm)
                                            "tracker" -> TrackerScreen(vm)
                                            "admin_dashboard" -> AdminDashboardScreen(vm)
                                            "customer_security" -> SecuritySettingsScreen(vm, com.example.security.AppModule.CUSTOMER, onBack = { vm.navigateTo("customer_home") })
                                            "rider_security" -> SecuritySettingsScreen(vm, com.example.security.AppModule.RIDER, onBack = { vm.navigateTo("rider_home") })
                                            "admin_security" -> SecuritySettingsScreen(vm, com.example.security.AppModule.ADMIN, onBack = { vm.navigateTo("admin_dashboard") })
                                            else -> SplashScreen(vm)
                                        }
                                    }
                                }
                            }

                            // 3-Second Delayed Logo Animation Platform Transition Splash Screen
                            if (isTransitioningPlatform) {
                                PlatformTransitionSplashScreen(targetPlatformName = transitionTargetPlatformName)
                            }

                            // Global Permanent Location Pin Marking Modal (Without Maps)
                            val showMarkLocModal by vm.showMarkLocationModal.collectAsState()
                            if (showMarkLocModal) {
                                MarkDesiredLocationModal(
                                    onDismiss = { vm.closeMarkLocationModal() },
                                    onSaveLocation = { label, address, lat, lng ->
                                        vm.savePermanentMarkedLocation(label, address, lat, lng)
                                    }
                                )
                            }

                            // Global Biometric Enrollment Auto-Offer — shown once after a normal
                            // (password / Google) first login on a device that has an enrolled fingerprint.
                            // The interactive spotlight tour itself is driven inside CustomerHomeScreen
                            // (it needs the on-screen element bounds), triggered by vm.showAppTourGuide.
                            val bioEnrollModule by vm.biometricEnrollPrompt.collectAsState()
                            bioEnrollModule?.let { module ->
                                BiometricEnrollPromptDialog(
                                    module = module,
                                    onEnable = {
                                        BiometricSecurityManager.showBiometricPrompt(
                                            activity = this@MainActivity,
                                            title = "Enable Fingerprint Login",
                                            subtitle = "Confirm your identity",
                                            description = "Scan your fingerprint to turn on quick biometric login for next time.",
                                            negativeButtonText = "Cancel",
                                            onSuccess = {
                                                vm.currentUser.value?.let { user ->
                                                    vm.enableBiometricForModule(this@MainActivity, module, user)
                                                }
                                                vm.dismissBiometricEnrollPrompt(decline = false)
                                            },
                                            // Cancel / hardware error: dismiss without remembering, so it may offer again next login.
                                            onError = { _, _ -> vm.dismissBiometricEnrollPrompt(decline = false) },
                                            // A non-matching scan keeps the system sheet up for retry — do nothing here.
                                            onFailed = { }
                                        )
                                    },
                                    onDecline = { vm.dismissBiometricEnrollPrompt(decline = true) }
                                )
                            }

                            // Global Outdated App Version Message Bar (for Auth/Portal screens; Home screens render it inline)
                            val isOutdatedGlobal by vm.isOutdatedAppVersion.collectAsState()
                            if (isOutdatedGlobal && currentScreen !in listOf("customer_home", "rider_home", "splash")) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .align(androidx.compose.ui.Alignment.TopCenter)
                                        .padding(horizontal = 16.dp, vertical = 42.dp)
                                ) {
                                    OutdatedAppVersionMessageBar(viewModel = vm)
                                }
                            }

                            // Global Browser Web-Push Notification UI Banner Overlay
                            val webPushPayload by vm.webPushNotification.collectAsState()
                            webPushPayload?.let { payload ->
                                WebPushNotificationBanner(
                                    payload = payload,
                                    onDismiss = { vm.dismissWebPush() },
                                    onClick = {
                                        vm.dismissWebPush()
                                        // If tracking order is set, navigate to tracker
                                        val trackerOrder = vm.trackingOrder.value
                                        if (trackerOrder != null) {
                                            vm.navigateTo("tracker")
                                        } else {
                                            vm.navigateTo("customer_home")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(targetIntent: android.content.Intent?) {
        val vm = viewModel ?: return
        targetIntent?.let {
            val openScreen = it.getStringExtra("open_screen")
            when (openScreen) {
                "tracker" -> vm.navigateTo("tracker")
                "order", "fuel_rates", "home" -> vm.navigateTo("customer_home")
            }
        }
    }
}

