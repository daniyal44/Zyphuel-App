package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import com.example.ui.MainViewModel

private val BrandNavy = Color(0xFF0F172A)
private val BrandBluePrimary = Color(0xFF0284C7)
private val BrandBlueSecondary = Color(0xFF0369A1)
private val FlameAmber = Color(0xFFF59E0B)
private val EmeraldSuccess = Color(0xFF10B981)
private val SoftSlate = Color(0xFFF8FAFC)
private val TextDark = Color(0xFF1E293B)
private val TextMuted = Color(0xFF64748B)

/**
 * Modern, user-friendly prompt requesting push notification permissions
 * to ensure customers & riders receive real-time fuel delivery status updates.
 */
@Composable
fun DeliveryNotificationPermissionPrompt(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val areNotificationsEnabled = remember(context) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(
                context,
                "🔔 Real-time fuel delivery alerts activated!",
                Toast.LENGTH_SHORT
            ).show()
            viewModel.dismissDeliveryNotificationPrompt(context, permanentlyForSession = true)
            // Trigger a quick test preview notification
            viewModel.triggerTestDeliveryNotification()
            onDismiss()
        } else {
            permissionRequestedOnce = true
            Toast.makeText(
                context,
                "Notifications disabled. You can enable them anytime in Settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Bell swinging / pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "bell_ring")
    val bellRotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bellRotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Dialog(
        onDismissRequest = {
            viewModel.dismissDeliveryNotificationPrompt(context, permanentlyForSession = true)
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .testTag("delivery_notification_permission_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Graphic with Pulsing Bell Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(glowPulse)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    BrandBluePrimary.copy(alpha = 0.18f),
                                    BrandBluePrimary.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(BrandBluePrimary, BrandBlueSecondary)
                                ),
                                shape = CircleShape
                            )
                            .shadow(8.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Notification Bell",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(bellRotation)
                        )
                    }

                    // Little glowing fuel pill badge on bell
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(22.dp)
                            .background(FlameAmber, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalGasStation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Stay Updated on Your Fuel Delivery",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy,
                        fontSize = 20.sp,
                        lineHeight = 26.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("notification_prompt_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enable push notifications to receive real-time updates as your fuel bowser is dispatched, tracked, and delivered.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Feature Highlights List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoftSlate)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NotificationFeatureRow(
                        icon = Icons.Filled.DeliveryDining,
                        iconColor = BrandBluePrimary,
                        title = "Live Bowser Dispatch & ETA",
                        description = "Get notified the exact second your tanker begins driving to your location."
                    )

                    NotificationFeatureRow(
                        icon = Icons.Filled.PinDrop,
                        iconColor = EmeraldSuccess,
                        title = "Rider Arrival Ping",
                        description = "Instant alert when the bowser arrives at your vehicle or gate."
                    )

                    NotificationFeatureRow(
                        icon = Icons.Filled.VerifiedUser,
                        iconColor = FlameAmber,
                        title = "Delivery OTP & Safety PIN",
                        description = "Instant lock screen notification with dispensing authorization codes."
                    )

                    NotificationFeatureRow(
                        icon = Icons.Filled.TrendingDown,
                        iconColor = Color(0xFF8B5CF6),
                        title = "Hourly Fuel Price Broadcasts",
                        description = "Timely alerts whenever official rates shift in Pakistan."
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Main CTA Button
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // On pre-Android 13, system notifications are granted by default unless disabled in settings
                            if (!areNotificationsEnabled) {
                                openAppNotificationSettings(context)
                            } else {
                                Toast.makeText(context, "🔔 Delivery updates are already enabled!", Toast.LENGTH_SHORT).show()
                                viewModel.triggerTestDeliveryNotification()
                            }
                            viewModel.dismissDeliveryNotificationPrompt(context, permanentlyForSession = true)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("enable_delivery_notifications_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBluePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (permissionRequestedOnce && !areNotificationsEnabled) "Open Notification Settings" else "Allow Real-Time Delivery Updates",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Action & Test Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Test Alert Button
                    TextButton(
                        onClick = {
                            viewModel.triggerTestDeliveryNotification()
                        },
                        modifier = Modifier.testTag("test_delivery_notification_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Test Live Alert",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = BrandBluePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Dismiss Button
                    TextButton(
                        onClick = {
                            viewModel.dismissDeliveryNotificationPrompt(context, permanentlyForSession = true)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("dismiss_delivery_notifications_prompt"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Maybe Later",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            )
        }
    }
}

/**
 * Opens system notification settings for this application.
 */
private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                else -> {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Please open Settings > Apps > Zyphuel > Notifications", Toast.LENGTH_LONG).show()
    }
}
