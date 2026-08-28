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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DebugLogger
import com.example.util.GlobalErrorBoundary
import com.example.util.UnifiedAssetManager

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        DebugLogger.i("MainActivity", "Initializing Zyphuel Application Activity")
        com.example.util.SigningDiagnosticUtil.logAppSigningSha1(this)
        UnifiedAssetManager.verifyAssetIntegrity(this)

        try {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
            notificationManager?.cancelAll()
        } catch (e: Exception) {
            DebugLogger.w("MainActivity", "Could not clear existing system notifications: ${e.message}")
        }

        // Initialize MainViewModel
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.refreshSecurityAndBiometricStates(this)

        // Track unique app install in Firestore & pre-fetch download count for Admin Dashboard
        viewModel.trackAppInstall(this)
        viewModel.fetchAppDownloadCount()

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
        intent?.let { targetIntent ->
            val openScreen = targetIntent.getStringExtra("open_screen")
            if (openScreen == "tracker") {
                viewModel.navigateTo("tracker")
            }
        }

        // Schedule periodic background WorkManager task for real-time fuel price updates & notifications
        com.example.worker.FuelPriceWorker.schedulePeriodicPriceWork(this)

        setContent {
            MyApplicationTheme {
                GlobalErrorBoundary(
                    onResetAppState = {
                        DebugLogger.i("MainActivity", "Resetting App State from Error Boundary")
                        viewModel.navigateTo("customer_home")
                    }
                ) {
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val isTransitioningPlatform by viewModel.isTransitioningPlatform.collectAsState()
                    val transitionTargetPlatformName by viewModel.transitionTargetPlatformName.collectAsState()
                    val uiMessage by viewModel.uiMessage.collectAsState()

                    // Center notification engine & App open price notification preference check
                    LaunchedEffect(Unit) {
                        viewModel.checkAppOpenNotificationChoicePrompt(this@MainActivity)
                        viewModel.checkAndPromptDeliveryNotifications(this@MainActivity)
                    }

                    LaunchedEffect(uiMessage) {
                        uiMessage?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                            viewModel.clearMessage()
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
                                            "splash" -> SplashScreen(viewModel)
                                            "portal_select" -> PortalSelectScreen(viewModel)
                                            "onboarding" -> OnboardingScreen(viewModel)
                                            "login_customer" -> AuthScreen(viewModel, isRegister = false, isRider = false)
                                            "register_customer" -> AuthScreen(viewModel, isRegister = true, isRider = false)
                                            "login_rider" -> AuthScreen(viewModel, isRegister = false, isRider = true)
                                            "register_rider" -> AuthScreen(viewModel, isRegister = true, isRider = true)
                                            "customer_home" -> CustomerHomeScreen(viewModel)
                                            "customer_order_history" -> CustomerOrderHistoryScreen(viewModel, onBack = { viewModel.navigateTo("customer_home") })
                                            "rider_home" -> RiderHomeScreen(viewModel)
                                            "rider_complete_profile" -> RiderCompleteProfileScreen(viewModel)
                                            "tracker" -> TrackerScreen(viewModel)
                                            "admin_dashboard" -> AdminDashboardScreen(viewModel)
                                            "customer_security" -> SecuritySettingsScreen(viewModel, com.example.security.AppModule.CUSTOMER, onBack = { viewModel.navigateTo("customer_home") })
                                            "rider_security" -> SecuritySettingsScreen(viewModel, com.example.security.AppModule.RIDER, onBack = { viewModel.navigateTo("rider_home") })
                                            "admin_security" -> SecuritySettingsScreen(viewModel, com.example.security.AppModule.ADMIN, onBack = { viewModel.navigateTo("admin_dashboard") })
                                            else -> SplashScreen(viewModel)
                                        }
                                    }
                                }
                            }

                            // 3-Second Delayed Logo Animation Platform Transition Splash Screen
                            if (isTransitioningPlatform) {
                                PlatformTransitionSplashScreen(targetPlatformName = transitionTargetPlatformName)
                            }

                            // Global Permanent Location Pin Marking Modal (Without Maps)
                            val showMarkLocModal by viewModel.showMarkLocationModal.collectAsState()
                            if (showMarkLocModal) {
                                MarkDesiredLocationModal(
                                    onDismiss = { viewModel.closeMarkLocationModal() },
                                    onSaveLocation = { label, address, lat, lng ->
                                        viewModel.savePermanentMarkedLocation(label, address, lat, lng)
                                    }
                                )
                            }

                            // Global User-Friendly Fuel Delivery Push Notification Permission Prompt
                            val showDeliveryNotifPrompt by viewModel.showDeliveryNotificationPrompt.collectAsState()
                            if (showDeliveryNotifPrompt) {
                                com.example.ui.components.DeliveryNotificationPermissionPrompt(
                                    viewModel = viewModel,
                                    onDismiss = { viewModel.dismissDeliveryNotificationPrompt() }
                                )
                            }

                            // Global Browser Web-Push Notification UI Banner Overlay
                            val webPushPayload by viewModel.webPushNotification.collectAsState()
                            webPushPayload?.let { payload ->
                                WebPushNotificationBanner(
                                    payload = payload,
                                    onDismiss = { viewModel.dismissWebPush() },
                                    onClick = {
                                        viewModel.dismissWebPush()
                                        // If tracking order is set, navigate to tracker
                                        val trackerOrder = viewModel.trackingOrder.value
                                        if (trackerOrder != null) {
                                            viewModel.navigateTo("tracker")
                                        } else {
                                            viewModel.navigateTo("customer_home")
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
