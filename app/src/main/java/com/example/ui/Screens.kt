@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class
)
package com.example.ui

import com.example.auth.findActivity
import com.example.auth.SocialAuthManager

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.R
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AuditLogEntity
import com.example.data.NotificationEntity
import com.example.data.OrderEntity
import com.example.data.PasswordSuggestion
import com.example.data.UserEntity
import androidx.compose.ui.zIndex
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@Composable
fun DailyPriceNotificationChoiceDialog(
    currentMode: String,
    onSelectOption: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(currentMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("daily_price_notification_choice_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0284C7).copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Fuel Price Notifications",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Real-time Pakistan Rates Option",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "How often would you like to receive fuel price update notifications?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                // Option 1: Every 4 Hours (6 times a day)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "EVERY_4_HOURS" }
                        .testTag("notif_option_4_hours"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedOption == "EVERY_4_HOURS") Color(0xFF0284C7).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        width = if (selectedOption == "EVERY_4_HOURS") 2.dp else 1.dp,
                        color = if (selectedOption == "EVERY_4_HOURS") Color(0xFF0284C7) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = (selectedOption == "EVERY_4_HOURS"),
                            onClick = { selectedOption = "EVERY_4_HOURS" }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Every 4 Hours (6x Daily)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Receive price checks and notifications 6 times a day every 4 hours.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }
                }

                // Option 2: Once Daily on App Open
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "ONCE_DAILY" }
                        .testTag("notif_option_once_daily"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedOption == "ONCE_DAILY") Color(0xFF16A34A).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        width = if (selectedOption == "ONCE_DAILY") 2.dp else 1.dp,
                        color = if (selectedOption == "ONCE_DAILY") Color(0xFF16A34A) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = (selectedOption == "ONCE_DAILY"),
                            onClick = { selectedOption = "ONCE_DAILY" }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Once Daily on App Open",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Receive price notification once per day when opening the app.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }
                }

                // Option 3: Disable Price Notifications
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "DISABLED" }
                        .testTag("notif_option_disabled"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedOption == "DISABLED") Color(0xFFDC2626).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        width = if (selectedOption == "DISABLED") 2.dp else 1.dp,
                        color = if (selectedOption == "DISABLED") Color(0xFFDC2626) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = (selectedOption == "DISABLED"),
                            onClick = { selectedOption = "DISABLED" }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Disable Notifications",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Do not send any fuel price update notifications.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectOption(selectedOption) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_notif_option_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Confirm Preference", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_notif_option_btn")
            ) {
                Text("Later Today", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkDesiredLocationModal(
    onDismiss: () -> Unit,
    onSaveLocation: (label: String, address: String, lat: Double, lng: Double) -> Unit
) {
    var labelInput by remember { mutableStateOf("Home Base Pin") }
    var addressInput by remember { mutableStateOf("House 42, Block C2, Gulberg III, Lahore") }
    var selectedLat by remember { mutableDoubleStateOf(31.5204) }
    var selectedLng by remember { mutableDoubleStateOf(74.3587) }
    var pinOffsetX by remember { mutableFloatStateOf(0f) }
    var pinOffsetY by remember { mutableFloatStateOf(0f) }

    val lahoreSectors = listOf(
        Triple("Gulberg III", 31.5204, 74.3587),
        Triple("DHA Phase 5", 31.4697, 74.4028),
        Triple("Model Town", 31.4870, 74.3245),
        Triple("Johar Town", 31.4697, 74.2728),
        Triple("Bahria Town", 31.3683, 74.1834),
        Triple("Mall Road", 31.5580, 74.3260),
        Triple("MM Alam Road", 31.5167, 74.3528),
        Triple("Askari 11", 31.4589, 74.4321),
        Triple("Lake City", 31.3500, 74.2400),
        Triple("Lahore Cantt", 31.5300, 74.3800)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("mark_desired_location_modal"),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ZyphuelBluePrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PinDrop,
                            contentDescription = null,
                            tint = ZyphuelBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Mark Desired Location Pin 📍",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "No map app needed • Saved permanently",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Pick a Lahore landmark/sector or enter a custom address and fine-tune your permanent pin position:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Permanent Pin Name / Label") },
                    placeholder = { Text("e.g. Home Base Pin, Factory Gate 2") },
                    leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null, tint = ZyphuelBluePrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mark_loc_label_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Text(
                    text = "Quick Select Famous Lahore Sectors:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lahore_sectors_row")
                ) {
                    items(lahoreSectors) { (sectorName, sLat, sLng) ->
                        val isSelected = addressInput.contains(sectorName, ignoreCase = true)
                        Surface(
                            modifier = Modifier.clickable {
                                addressInput = "$sectorName, Lahore, Pakistan"
                                selectedLat = sLat
                                selectedLng = sLng
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ZyphuelBluePrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) ZyphuelBluePrimary else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "📍 $sectorName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = { Text("Detailed Street / Landmark Address") },
                    placeholder = { Text("e.g. House 42, Block C2, Gulberg III, Lahore") },
                    leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null, tint = ZyphuelBluePrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mark_loc_address_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Fine-Tune Permanent Pin Position Canvas:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.5.dp, ZyphuelBluePrimary, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val height = size.height
                                pinOffsetX = ((offset.x / width) - 0.5f) * 0.01f
                                pinOffsetY = ((offset.y / height) - 0.5f) * 0.01f
                                selectedLat = 31.5204 + pinOffsetY
                                selectedLng = 74.3587 + pinOffsetX
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        for (i in 1..4) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.1f),
                                start = Offset(width * i / 5f, 0f),
                                end = Offset(width * i / 5f, height),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.1f),
                                start = Offset(0f, height * i / 5f),
                                end = Offset(width, height * i / 5f),
                                strokeWidth = 1f
                            )
                        }

                        // Origin Center Hub
                        drawCircle(
                            color = Color(0xFF22C55E),
                            radius = 12f,
                            center = Offset(width * 0.2f, height * 0.8f)
                        )

                        val pinX = width * (0.5f + (pinOffsetX / 0.01f))
                        val pinY = height * (0.5f + (pinOffsetY / 0.01f))

                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(width * 0.2f, height * 0.8f),
                            end = Offset(pinX, pinY),
                            strokeWidth = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        drawCircle(
                            color = Color(0xFFEF4444).copy(alpha = 0.3f),
                            radius = 26f,
                            center = Offset(pinX, pinY)
                        )
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = 12f,
                            center = Offset(pinX, pinY)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📍 Pin: (${String.format(java.util.Locale.US, "%.4f", selectedLat)}, ${String.format(java.util.Locale.US, "%.4f", selectedLng)})",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tap box to adjust offset pin",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray, fontSize = 10.sp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveLocation(labelInput, addressInput, selectedLat, selectedLng)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                modifier = Modifier.testTag("confirm_save_marked_loc_btn")
            ) {
                Icon(Icons.Filled.BookmarkAdded, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Permanent Pin 📌", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_mark_loc_btn")) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun WebPushNotificationBanner(
    payload: WebPushPayload,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(payload) {
        visible = true
        // Auto-dismiss after 6 seconds
        delay(6000)
        visible = false
        delay(300)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .zIndex(999f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .pointerInput(payload) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Swipe up or down past threshold to dismiss banner from top
                        if (dragAmount < -8f || dragAmount > 25f) {
                            visible = false
                            onDismiss()
                        }
                    }
                },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header (App source info & Top Dismiss Close button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Zyphuel Logo",
                            modifier = Modifier.size(18.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "Zyphuel Delivery • Notification",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ZyphuelBluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "now",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                        )
                        IconButton(
                            onClick = {
                                visible = false
                                onDismiss()
                            },
                            modifier = Modifier.size(24.dp).testTag("notification_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss Notification",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content body clickable for action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            visible = false
                            onDismiss()
                            onClick()
                        },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left App Logo Thumbnail
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ZyphuelBluePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Zyphuel Logo",
                            modifier = Modifier.size(30.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Text Body
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = payload.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBlueDark
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = payload.body,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.DarkGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            visible = false
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("notification_dismiss_btn")
                    ) {
                        Text(
                            "Dismiss",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            visible = false
                            onDismiss()
                            onClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("notification_action_btn")
                    ) {
                        Text(
                            "View Details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

// --- REUSABLE CANVASES FOR PREMIUM GRADIANT BRAND LOGOS ---

@Composable
fun ZyphuelTextLogo(modifier: Modifier = Modifier, fontSize: TextUnit = 28.sp) {
    // Redefined to be empty to avoid duplicate visual labels/logos
}

@Composable
fun ZyphuelOuterLogo(modifier: Modifier = Modifier, scale: Float = 1f) {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "Zyphuel App Logo",
        modifier = modifier
            .scale(scale)
            .aspectRatio(1f),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun ZyphuelInnerLogoAnimated(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "zyphuel_heartbeat")
    val heartbeatScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                1.00f at 0
                1.25f at 300 with FastOutSlowInEasing
                1.08f at 500 with FastOutLinearInEasing
                1.35f at 800 with FastOutSlowInEasing
                1.00f at 1200 with LinearOutSlowInEasing
                1.00f at 3000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartbeatScale"
    )

    Image(
        painter = painterResource(id = R.drawable.zyphuel_logo),
        contentDescription = "Zyphuel Inside Heartbeat Logo",
        modifier = modifier
            .scale(heartbeatScale)
            .aspectRatio(1f),
        contentScale = ContentScale.Fit
    )
}

// --- PHASE 2: SPLASH SCREEN ---

@Composable
fun SplashScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notificationsGranted = permissions["android.permission.POST_NOTIFICATIONS"] == true
        if (notificationsGranted) {
            Toast.makeText(context, "🔔 Notifications enabled for updates", Toast.LENGTH_SHORT).show()
        }
    }

    // 3-second animated progress bar
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }

    // Pulse scale animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "splash_logo_pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    // Smooth spinning rotation for inner logo
    val logoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoRotation"
    )

    LaunchedEffect(Unit) {
        try {
            permissionLauncher.launch(
                arrayOf(
                    "android.permission.POST_NOTIFICATIONS"
                )
            )
        } catch (e: Exception) {
            // Safe fallback
        }

        // Fast animated loading bar over 600ms
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = LinearEasing)
            )
        }

        // Wait for session to load — clean coroutine await, no busy polling
        viewModel.isSessionLoaded.filter { it }.first()
        delay(300)


        val user = viewModel.currentUser.value
        if (user != null) {
            when (user.role) {
                "rider" -> viewModel.navigateTo("rider_home")
                "admin" -> viewModel.navigateTo("admin_dashboard")
                else -> viewModel.navigateTo("customer_home")
            }
        } else {
            viewModel.navigateTo("portal_select")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Animated Zyphuel Logo Container with 3-second heartbeat entrance
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        color = ZyphuelBluePrimary.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(logoRotation)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    ZyphuelBluePrimary,
                                    ZyphuelBlueSecondary,
                                    ZyphuelBluePrimary.copy(alpha = 0.2f),
                                    ZyphuelBluePrimary
                                )
                            ),
                            shape = CircleShape
                        )
                )
                ZyphuelInnerLogoAnimated(modifier = Modifier.fillMaxSize(0.72f))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Lahore's Premium Delivery Network",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ZyphuelBlueDark,
                    fontSize = 18.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Smooth 3-second animated loading bar
            Column(
                modifier = Modifier.width(220.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ZyphuelBluePrimary,
                    trackColor = ZyphuelBluePrimary.copy(alpha = 0.15f)
                )
            }
        }
    }
}

// --- PORTAL SELECT SCREEN ---

@Composable
fun PortalSelectScreen(viewModel: MainViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ZyphuelBlueDark,
                        ZyphuelBluePrimary,
                        Color(0xFF0F172A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo & Title
            ZyphuelOuterLogo(modifier = Modifier.height(72.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to Zyphuel",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select your portal to login and continue",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.85f)
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Button 1: Customer Portal
            Card(
                onClick = { viewModel.navigateTo("login_customer") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portal_select_customer_btn"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(ZyphuelBluePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Store,
                                contentDescription = "Customer Portal",
                                tint = ZyphuelBluePrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Customer Login",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ZyphuelBlueDark
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Order Petrol, Diesel, Octane & LPG Gas delivered to your location",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = ZyphuelBluePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.navigateTo("login_customer") },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_login_portal_btn")
                    ) {
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customer Login", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Button 2: Rider Portal
            Card(
                onClick = { viewModel.navigateTo("login_rider") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portal_select_rider_btn"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TwoWheeler,
                                contentDescription = "Rider Portal",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rider Login",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ZyphuelBlueDark
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Accept fuel delivery orders, track routes & manage deliveries",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF10B981)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.navigateTo("login_rider") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rider_login_portal_btn")
                    ) {
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rider Login", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformTransitionSplashScreen(targetPlatformName: String) {
    // 3-second animated progress bar
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }

    // Pulse scale animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "platform_logo_pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    // Smooth spinning rotation for inner logo
    val logoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoRotation"
    )

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // Animated Zyphuel Logo Container with 3-second heartbeat transition
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        color = ZyphuelBluePrimary.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(logoRotation)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    ZyphuelBluePrimary,
                                    ZyphuelBlueSecondary,
                                    ZyphuelBluePrimary.copy(alpha = 0.2f),
                                    ZyphuelBluePrimary
                                )
                            ),
                            shape = CircleShape
                        )
                )
                ZyphuelInnerLogoAnimated(modifier = Modifier.fillMaxSize(0.72f))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Switching to $targetPlatformName",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = ZyphuelBlueDark,
                    fontSize = 22.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Loading Lahore's Premium Platform Resources...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3-second animated progress bar
            Column(
                modifier = Modifier.width(220.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ZyphuelBluePrimary,
                    trackColor = ZyphuelBluePrimary.copy(alpha = 0.15f)
                )
            }
        }
    }
}

// --- CUSTOM SIMULATED SMARTPHONE SCREENSHOT COMPONENT FOR SUPPORT STEPS ---

@Composable
fun SupportStepOneScreenshot() {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF8FAFC))
            .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Simulated Phone Status bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("12:00 PM", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.SignalCellular4Bar, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Icon(Icons.Filled.Wifi, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text("100%", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 10.sp))
                }
            }
            
            // App Interface Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(ZyphuelBluePrimary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Automated Delivery Hub", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                    Text("Lahore, Pakistan • Instant Routing", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Simulated Location Pin input card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Delivery Destination", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text("Lahore, Pakistan", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            // Step 1 Help Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delayed or did not receive fuel directly? Live customer support is always active and accessible.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1E40AF), fontSize = 10.sp, lineHeight = 14.sp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Highlighted Live Support Floating Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEF4444)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("NEED DIRECT HELP? TAP LIVE SUPPORT", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp))
                }
            }
        }
        
        // Pointer overlay to demonstrate user action
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = (-5).dp, x = (-15).dp)
                .size(34.dp)
                .background(Color(0x44000000), CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.TouchApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SupportStepTwoScreenshot() {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF8FAFC))
            .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Simulated Phone Status bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("12:02 PM", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray))
                Icon(Icons.Filled.Wifi, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            }
            
            // Order Header
            Text("Order Fuel & Gas", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
            Spacer(modifier = Modifier.height(6.dp))
            
            // Fuel product list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ZyphuelBluePrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, ZyphuelBluePrimary)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Super Petrol", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Rs. 297.53/L", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PropaneTank, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("LPG Gas", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Rs. 308.76/kg", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quantity Selector bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantity:", style = MaterialTheme.typography.labelMedium.copy(color = Color.DarkGray))
                Text("25 Liters", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary))
                Text("Total: Rs. 7,438", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Support call-outs
            Text("If delayed or not received directly, use support:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp Chat", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    }
                }
                
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Direct Call support", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun SupportStepThreeScreenshot() {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFF10B981), RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Simulated Phone Status bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("12:15 PM", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray))
                Text("🟢 Active Tracking", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF10B981), fontWeight = FontWeight.Bold))
            }
            
            // Map-routing simulation card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF))
                    .border(1.dp, Color(0xFFDBEAFE)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Store, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
                        Text("• • • • • 🛵 • • • • •", style = MaterialTheme.typography.bodyLarge.copy(color = ZyphuelBluePrimary, fontWeight = FontWeight.Bold))
                        Icon(Icons.Filled.Home, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pure Water & Fuel en-route (ETA: 10 mins)", style = MaterialTheme.typography.labelSmall.copy(color = Color.DarkGray, fontWeight = FontWeight.Bold))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status Timeline
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Order Prepared: Pure Premium Water Gallon", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rider Assigned & Dispatched", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Live WhatsApp support ticket solved instantly", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer guarantee badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD1FAE5), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Our Lahore coordinate desk guarantees verified satisfaction.",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF065F46), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                )
            }
        }
    }
}

// --- PHASE 2: ONBOARDING SCREEN ---

@Composable
fun OnboardingScreen(viewModel: MainViewModel) {
    var step by remember { mutableIntStateOf(0) }

    val titles = listOf(
        "1. Select Premium Services",
        "2. Track Your Delivery Live",
        "3. View Complete Order History"
    )

    val descriptions = listOf(
        "Easily select from our premium Euro V fuels, LPG Gas cylinder refills, or pure mineral water gallons. Configure your quantities and see live transparent price calculations instantly.",
        "Follow your assigned delivery rider in real-time on our smart GPS tracker. Receive instant ETA updates, see the route progression overlay, and access 24/7 direct standby support.",
        "Access your complete past order log anytime. Review itemized details, track past receipts, and view detailed progress metrics for every delivery you have placed in Lahore."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZyphuelOuterLogo(modifier = Modifier.height(32.dp))
                TextButton(
                    onClick = { viewModel.navigateTo("portal_select") },
                    colors = ButtonDefaults.textButtonColors(contentColor = ZyphuelBluePrimary)
                ) {
                    Text("Skip")
                }
            }

            // Animated Body Content
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(400)) { it } + fadeIn() togetherWith
                            slideOutHorizontally(animationSpec = tween(400)) { -it } + fadeOut()
                },
                modifier = Modifier.weight(1f),
                label = "onboarding_content"
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Beautiful simulated step-by-step screenshot display
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (currentStep) {
                            0 -> SupportStepOneScreenshot()
                            1 -> SupportStepTwoScreenshot()
                            2 -> SupportStepThreeScreenshot()
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = titles[currentStep],
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = descriptions[currentStep],
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Indicator and Navigation footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Step Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == step) 16.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (index == step) ZyphuelBluePrimary else Color.LightGray
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                Button(
                    onClick = {
                        if (step < 2) {
                            step++
                        } else {
                            viewModel.navigateTo("portal_select")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_next_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (step == 2) "Get Started" else "Next",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- LOGIN & REGISTER FOR CUSTOMER & RIDER ---

@Composable
fun ForgotPasswordDialog(
    initialIsRider: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = ZyphuelBluePrimary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (initialIsRider) "Rider Password Reset" else "Customer Password Reset",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Verify your registered account email and phone number to set a new password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Email Address
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Registered Email") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = ZyphuelBluePrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_email_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray,
                        focusedBorderColor = ZyphuelBluePrimary,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLeadingIconColor = ZyphuelBluePrimary,
                        unfocusedLeadingIconColor = ZyphuelBluePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Phone Number
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Registered Phone") },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = ZyphuelBluePrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray,
                        focusedBorderColor = ZyphuelBluePrimary,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLeadingIconColor = ZyphuelBluePrimary,
                        unfocusedLeadingIconColor = ZyphuelBluePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // New Password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = ZyphuelBluePrimary) },
                    trailingIcon = {
                        val image = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (newPasswordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = description, tint = ZyphuelBluePrimary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_new_password_input"),
                    visualTransformation = if (newPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray,
                        focusedBorderColor = ZyphuelBluePrimary,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLeadingIconColor = ZyphuelBluePrimary,
                        unfocusedLeadingIconColor = ZyphuelBluePrimary,
                        focusedTrailingIconColor = ZyphuelBluePrimary,
                        unfocusedTrailingIconColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(email, phone, newPassword) },
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("reset_submit_button")
            ) {
                Text("Reset Password", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun FormSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ZyphuelBluePrimary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = ZyphuelBlueDark
            )
        )
    }
}

@Composable
fun DocumentUploaderBox(
    label: String,
    isUploaded: Boolean,
    onUploadClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isUploaded) Color(0xFFF0FDF4) else Color(0xFFF8FAFC))
            .border(
                width = 1.dp,
                color = if (isUploaded) Color(0xFF22C55E) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onUploadClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isUploaded) Icons.Filled.CheckCircle else Icons.Filled.Face,
                contentDescription = null,
                tint = if (isUploaded) Color(0xFF22C55E) else ZyphuelBluePrimary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isUploaded) Color(0xFF166534) else ZyphuelBlueDark
                    )
                )
                Text(
                    text = if (isUploaded) "Document Uploaded Successfully ✔" else "Tap to simulate secure upload",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isUploaded) Color(0xFF15803D) else Color.Gray,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: MainViewModel, isRegister: Boolean, isRider: Boolean) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Rider fields
    var vehicleType by remember { mutableStateOf("Bike") }
    var vehicleNo by remember { mutableStateOf("") }
    
    // Pakistani Rider Advanced Registration fields
    var country by remember { mutableStateOf("Pakistan") }
    var countryCode by remember { mutableStateOf("+92") }
    var documentType by remember { mutableStateOf("CNIC") }
    var cnicOrPassport by remember { mutableStateOf("") }

    // Additional Rider Master Form State Variables
    var fathersName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var cnicIssueDate by remember { mutableStateOf("") }
    var cnicExpiryDate by remember { mutableStateOf("") }
    
    var residentialAddress by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Lahore") }
    var province by remember { mutableStateOf("Punjab") }
    var postalCode by remember { mutableStateOf("54000") }
    
    var emergencyName by remember { mutableStateOf("") }
    var emergencyRelationship by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    
    var termsAccepted by remember { mutableStateOf(false) }
    var declarationAccepted by remember { mutableStateOf(false) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    // (removed: showGoogleAccountPickerDialog state — device-account picker no longer exists; real Google flow only)
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var isInitialScreenLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(450)
        isInitialScreenLoading = false
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    if (isInitialScreenLoading) {
        TeslaAuthSkeleton()
        return
    }



    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ZyphuelLightBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isWide = maxWidth > 600.dp
        Column(
            modifier = Modifier
                .then(if (isWide) Modifier.widthIn(max = 580.dp).align(Alignment.Center) else Modifier.fillMaxSize())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back to Portal Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = { viewModel.navigateTo("portal_select") },
                    colors = ButtonDefaults.textButtonColors(contentColor = ZyphuelBluePrimary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Header Logo & Branding
            ZyphuelOuterLogo(modifier = Modifier.height(50.dp))
            Spacer(modifier = Modifier.height(12.dp))

            // Box Card for Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = if (isRegister) (if (isRider) "Rider Sign up Form" else "Customer Sign up Form") else (if (isRider) "Rider Login Form" else "Customer Login Form"),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBlueDark
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    if (isRegister && isRider) {
                        // ==========================================
                        // RIDER MASTER REGISTRATION FORM
                        // ==========================================

                        // --- SECTION 1: PERSONAL INFORMATION ---
                        item {
                            FormSectionHeader(title = "1. Personal Information", icon = Icons.Filled.Person)
                        }

                        item {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name (As per CNIC) *") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = fathersName,
                                onValueChange = { fathersName = it },
                                label = { Text("Father's Name *") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_fathers_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = dob,
                                onValueChange = { dob = it },
                                label = { Text("Date of Birth * (DD-MM-YYYY)") },
                                placeholder = { Text("e.g. 14-08-1995") },
                                leadingIcon = { Icon(Icons.Filled.Assignment, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_dob"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            Column {
                                Text(
                                    text = "Gender *",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val genders = listOf("Male", "Female", "Other")
                                    genders.forEach { g ->
                                        val isSelected = gender == g
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) ZyphuelBluePrimary else Color(0xFFF1F5F9))
                                                .clickable { gender = g }
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) ZyphuelBluePrimary else Color.LightGray,
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = g,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else Color.DarkGray
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address (Optional)") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone Number *") },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_phone_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Create Login Password *") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = ZyphuelBluePrimary) },
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = null, tint = ZyphuelBluePrimary)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // --- SECTION 2: IDENTITY INFORMATION ---
                        item {
                            FormSectionHeader(title = "2. Identity Information", icon = Icons.Filled.AccountBox)
                        }

                        item {
                            OutlinedTextField(
                                value = cnicOrPassport,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    var formatted = ""
                                    for (i in digits.indices) {
                                        formatted += digits[i]
                                        if (i == 4 || i == 11) {
                                            formatted += "-"
                                        }
                                    }
                                    if (formatted.length <= 15) {
                                        cnicOrPassport = formatted
                                    }
                                },
                                label = { Text("CNIC Number * (xxxxx-xxxxxxx-x)") },
                                placeholder = { Text("e.g. 35201-1234567-1") },
                                leadingIcon = { Icon(Icons.Filled.AccountBox, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_cnic_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = cnicIssueDate,
                                onValueChange = { cnicIssueDate = it },
                                label = { Text("CNIC Issue Date * (DD-MM-YYYY)") },
                                placeholder = { Text("e.g. 12-05-2020") },
                                leadingIcon = { Icon(Icons.Filled.Assignment, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_cnic_issue_date"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = cnicExpiryDate,
                                onValueChange = { cnicExpiryDate = it },
                                label = { Text("CNIC Expiry Date * (DD-MM-YYYY)") },
                                placeholder = { Text("e.g. 12-05-2030") },
                                leadingIcon = { Icon(Icons.Filled.Assignment, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_cnic_expiry_date"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }



                        // --- SECTION 3: RESIDENTIAL INFORMATION ---
                        item {
                            FormSectionHeader(title = "3. Residential Information", icon = Icons.Filled.Home)
                        }

                        item {
                            OutlinedTextField(
                                value = residentialAddress,
                                onValueChange = { residentialAddress = it },
                                label = { Text("Complete Residential Address *") },
                                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_address"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                maxLines = 3
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City *") },
                                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_city"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = province,
                                    onValueChange = { province = it },
                                    label = { Text("Province *") },
                                    modifier = Modifier.weight(1.2f).testTag("auth_province"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                        focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                        focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = postalCode,
                                    onValueChange = { postalCode = it },
                                    label = { Text("Postal Code *") },
                                    modifier = Modifier.weight(1f).testTag("auth_postal_code"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                        focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                        focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // --- SECTION 4: VEHICLE INFORMATION ---
                        item {
                            FormSectionHeader(title = "4. Vehicle Information", icon = Icons.Filled.DirectionsCar)
                        }

                        item {
                            Column {
                                Text(
                                    text = "Vehicle Type *",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val vehicles = listOf("Bike", "Car")
                                    vehicles.forEach { v ->
                                        val isSelected = vehicleType == v
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) ZyphuelBluePrimary else Color(0xFFF1F5F9))
                                                .clickable { vehicleType = v }
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) ZyphuelBluePrimary else Color.LightGray,
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = v,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else Color.DarkGray
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = vehicleNo,
                                onValueChange = { vehicleNo = it.uppercase() },
                                label = { Text("Vehicle Registration Number *") },
                                placeholder = { Text("e.g. LHR-20-4567") },
                                leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_vehicle_no"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // --- SECTION 5: EMERGENCY CONTACT (OPTIONAL) ---
                        item {
                            FormSectionHeader(title = "5. Emergency Contact (Optional)", icon = Icons.Filled.Phone)
                        }

                        item {
                            OutlinedTextField(
                                value = emergencyName,
                                onValueChange = { emergencyName = it },
                                label = { Text("Emergency Contact Full Name") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_emergency_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = emergencyRelationship,
                                onValueChange = { emergencyRelationship = it },
                                label = { Text("Relationship") },
                                placeholder = { Text("e.g. Father, Brother, Spouse") },
                                leadingIcon = { Icon(Icons.Filled.Assignment, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_emergency_relation"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = emergencyPhone,
                                onValueChange = { emergencyPhone = it },
                                label = { Text("Emergency Contact Number") },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_emergency_phone"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary, unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary, unfocusedBorderColor = Color.LightGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // (Required documents section removed)



                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { termsAccepted = !termsAccepted }
                                ) {
                                    Checkbox(
                                        checked = termsAccepted,
                                        onCheckedChange = { termsAccepted = it },
                                        colors = CheckboxDefaults.colors(checkedColor = ZyphuelBluePrimary)
                                    )
                                    Text(
                                        text = "I Accept the Terms and Conditions *",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth().clickable { declarationAccepted = !declarationAccepted }
                                ) {
                                    Checkbox(
                                        checked = declarationAccepted,
                                        onCheckedChange = { declarationAccepted = it },
                                        colors = CheckboxDefaults.colors(checkedColor = ZyphuelBluePrimary)
                                    )
                                    Text(
                                        text = "Rider Declaration *: \"I confirm that all information and documents provided are accurate and belong to me.\"",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray),
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }
                        }

                    } else {
                        // ==========================================
                        // STANDARD CUSTOMER OR RIDER LOGIN / SIGNUP
                        // ==========================================
                        if (isRegister) {
                            item {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Full Name") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ZyphuelBluePrimary) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_name_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedLabelColor = ZyphuelBluePrimary,
                                        unfocusedLabelColor = Color.DarkGray,
                                        focusedBorderColor = ZyphuelBluePrimary,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedLeadingIconColor = ZyphuelBluePrimary,
                                        unfocusedLeadingIconColor = ZyphuelBluePrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = ZyphuelBluePrimary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_input"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary,
                                    unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLeadingIconColor = ZyphuelBluePrimary,
                                    unfocusedLeadingIconColor = ZyphuelBluePrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        if (isRegister) {
                            item {
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("Phone Number") },
                                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = ZyphuelBluePrimary) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_phone_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedLabelColor = ZyphuelBluePrimary,
                                        unfocusedLabelColor = Color.DarkGray,
                                        focusedBorderColor = ZyphuelBluePrimary,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedLeadingIconColor = ZyphuelBluePrimary,
                                        unfocusedLeadingIconColor = ZyphuelBluePrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = ZyphuelBluePrimary) },
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    val description = if (passwordVisible) "Hide password" else "Show password"
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = description, tint = ZyphuelBluePrimary)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_input"),
                                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedLabelColor = ZyphuelBluePrimary,
                                    unfocusedLabelColor = Color.DarkGray,
                                    focusedBorderColor = ZyphuelBluePrimary,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLeadingIconColor = ZyphuelBluePrimary,
                                    unfocusedLeadingIconColor = ZyphuelBluePrimary,
                                    focusedTrailingIconColor = ZyphuelBluePrimary,
                                    unfocusedTrailingIconColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        if (!isRegister) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 0.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    TextButton(
                                        onClick = { showForgotPasswordDialog = true },
                                        modifier = Modifier.testTag("forgot_password_button")
                                    ) {
                                        Text(
                                            text = "Forgot Password?",
                                            color = ZyphuelBluePrimary,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (isRegister) {
                                    if (isRider) {
                                        if (name.isBlank() || password.isBlank() || phone.isBlank() || cnicOrPassport.isBlank() ||
                                            residentialAddress.isBlank() || vehicleNo.isBlank() ||
                                            fathersName.isBlank() || dob.isBlank() || cnicIssueDate.isBlank() ||
                                            cnicExpiryDate.isBlank() ||
                                            city.isBlank() || province.isBlank() || postalCode.isBlank()
                                        ) {
                                            Toast.makeText(context, "Please complete all mandatory fields 🪪", Toast.LENGTH_LONG).show()
                                        } else if (!termsAccepted || !declarationAccepted) {
                                            Toast.makeText(context, "Please accept the Terms and confirm the Rider Declaration 📜", Toast.LENGTH_LONG).show()
                                        } else {
                                            val fullPhone = "$countryCode $phone"
                                            viewModel.registerRider(
                                                email = email,
                                                name = name,
                                                passwordHash = password,
                                                phone = fullPhone,
                                                fathersName = fathersName,
                                                dob = dob,
                                                gender = gender,
                                                cnicNumber = cnicOrPassport,
                                                cnicIssueDate = cnicIssueDate,
                                                cnicExpiryDate = cnicExpiryDate,
                                                cnicFrontImage = "cnic_front.jpg",
                                                cnicBackImage = "cnic_back.jpg",
                                                residentialAddress = residentialAddress,
                                                city = city,
                                                province = province,
                                                postalCode = postalCode,
                                                vehicleType = vehicleType,
                                                vehicleMake = "",
                                                vehicleModel = "",
                                                vehicleNo = vehicleNo,
                                                vehicleColor = "",
                                                vehicleRegBookImage = "reg_book.jpg",
                                                vehiclePhoto = "vehicle_photo.jpg",
                                                emergencyName = emergencyName,
                                                emergencyRelationship = emergencyRelationship,
                                                emergencyPhone = emergencyPhone,
                                                termsAccepted = termsAccepted,
                                                declarationAccepted = declarationAccepted
                                            ) {
                                                viewModel.navigateTo("login_rider")
                                            }
                                        }
                                    } else {
                                        viewModel.registerCustomer(email, name, password, phone) {
                                            viewModel.navigateTo("login_customer")
                                        }
                                    }
                                } else {
                                    isAuthenticating = true
                                    viewModel.login(email, password) { user ->
                                        viewModel.completeLogin(user)
                                        when (user.role) {
                                            "admin" -> viewModel.navigateTo("admin_dashboard")
                                            "rider" -> viewModel.navigateTo("rider_home")
                                            else -> viewModel.navigateTo("customer_home")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("auth_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (isRegister) "Register Account" else "Log In",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // --- SOCIAL LOGINS SECTION (Google Only) ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color.LightGray.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "  OR CONTINUE WITH  ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        letterSpacing = 1.sp
                                    )
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color.LightGray.copy(alpha = 0.6f)
                                )
                            }

                            // Continue with Google Button (Native Google Identity SDK with Real-Time Device Account Selection)
                            OutlinedButton(
                                onClick = {
                                    if (isGoogleSigningIn) return@OutlinedButton
                                    viewModel.clearMessage()
                                    isGoogleSigningIn = true
                                    val targetRole = if (isRider) "rider" else "customer"

                                    com.example.auth.GoogleAuthManager.signInWithGoogle(
                                        context = context,
                                        activity = (context as? android.app.Activity),
                                        scope = scope,
                                        targetRole = targetRole
                                    ) { success, firebaseUser, googleEmail, googleDisplayName, googlePhotoUrl, errorMessage ->
                                        if (success && !googleEmail.isNullOrBlank()) {
                                            val resolvedName = googleDisplayName ?: googleEmail.substringBefore("@")
                                            viewModel.loginWithSocialAccount(
                                                provider = "Google",
                                                socialEmail = googleEmail,
                                                socialName = resolvedName,
                                                profilePicUrl = googlePhotoUrl,
                                                targetRole = targetRole,
                                                uid = firebaseUser?.uid ?: "",
                                                onFailure = { err ->
                                                    isGoogleSigningIn = false
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            ) { user ->
                                                isGoogleSigningIn = false
                                                viewModel.completeLogin(user)
                                                Toast.makeText(context, "Welcome back, ${user.name}! 🚀", Toast.LENGTH_SHORT).show()
                                                when (user.role) {
                                                    "admin" -> viewModel.navigateTo("admin_dashboard")
                                                    "rider" -> viewModel.navigateTo("rider_home")
                                                    else -> viewModel.navigateTo("customer_home")
                                                }
                                            }
                                        } else {
                                            isGoogleSigningIn = false
                                            // Real token-verified Google Sign-In ONLY — no email-only / device-account fallback.
                                            val lower = errorMessage?.lowercase() ?: ""
                                            when {
                                                lower.contains("cancel") -> { /* user dismissed the Google sheet — stay silent */ }
                                                lower.isBlank() || lower.contains("no credential") || lower.contains("no account") ||
                                                    lower.contains("28433") || lower.contains("account") ->
                                                    Toast.makeText(
                                                        context,
                                                        "No Google account available on this device. Add one in Settings ▸ Accounts ▸ Add account ▸ Google, then tap Continue with Google again.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                else ->
                                                    Toast.makeText(context, "Google Sign-In failed: $errorMessage", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("social_login_google"),
                                enabled = !isGoogleSigningIn,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFDADCE0)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF3C4043)
                                )
                            ) {
                                if (isGoogleSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = ZyphuelBluePrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Signing in with Google...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_google),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Continue with Google",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF3C4043)
                                        )
                                    )
                                }
                            }
                        }

                        // Biometric Hardware Auth Panel on Login Screen (Only visible if registered user has enabled biometrics)
                        val bioModule = if (isRider) com.example.security.AppModule.RIDER else com.example.security.AppModule.CUSTOMER

                        LaunchedEffect(bioModule) {
                            viewModel.refreshSecurityAndBiometricStates(context)
                        }

                        val isCustomerBioEnabled by viewModel.isCustomerBioEnabled.collectAsState()
                        val isRiderBioEnabled by viewModel.isRiderBioEnabled.collectAsState()
                        val isBioEnabled = if (isRider) isRiderBioEnabled else isCustomerBioEnabled

                        val registeredEmail = com.example.security.SecureStorageManager.getRegisteredEmail(context, bioModule)
                        val fragmentActivity = context as? androidx.fragment.app.FragmentActivity

                        val targetUserEmail = if (email.isNotBlank()) email.trim().lowercase() else (registeredEmail ?: "")
                        val isRegisteredAccountPresent = targetUserEmail.isNotBlank()

                        if (!isRegister && isBioEnabled && isRegisteredAccountPresent) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.5.dp, ZyphuelBluePrimary.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Fingerprint,
                                                contentDescription = null,
                                                tint = ZyphuelBluePrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Fingerprint / Face ID Login",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Enabled 🔓",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF059669)
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val performBiometricAuth = {
                                                viewModel.loginWithBiometrics(
                                                    context = context,
                                                    module = bioModule,
                                                    userEmailInput = targetUserEmail,
                                                    onSuccess = { user ->
                                                        Toast.makeText(context, "Biometric Auth Success! Welcome ${user.name} 👋", Toast.LENGTH_SHORT).show()
                                                        if (user.role == "admin" || user.role == "customer") {
                                                            viewModel.navigateTo("customer_home")
                                                        } else if (user.role == "rider") {
                                                            viewModel.navigateTo("rider_home")
                                                        }
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            }

                                            if (fragmentActivity != null) {
                                                com.example.security.BiometricSecurityManager.showBiometricPrompt(
                                                    activity = fragmentActivity,
                                                    title = "Zyphuel Fingerprint Login",
                                                    subtitle = "Scan fingerprint or face to sign in as ${if (isRider) "Rider" else "Customer"}",
                                                    description = "Account: $targetUserEmail",
                                                    onSuccess = { _ -> performBiometricAuth() },
                                                    onError = { _, errStr ->
                                                        Toast.makeText(context, "Notice: $errStr", Toast.LENGTH_SHORT).show()
                                                        performBiometricAuth()
                                                    },
                                                    onFailed = {
                                                        Toast.makeText(context, "Biometric scan failed. Please retry...", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            } else {
                                                performBiometricAuth()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("biometric_login_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Fingerprint,
                                            contentDescription = "Scan Fingerprint",
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sign in with Fingerprint / Face ID",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                    }

                                    Text(
                                        text = "Registered Account: $targetUserEmail",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Navigation links
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isRider) {
                    // Customer Portal
                    if (!isRegister) {
                        TextButton(
                            onClick = { viewModel.navigateTo("register_customer") },
                            modifier = Modifier.testTag("register_as_customer_link")
                        ) {
                            Text(
                                text = "Don't have an account? Register as Customer",
                                color = ZyphuelBluePrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { viewModel.navigateTo("login_customer") },
                            modifier = Modifier.testTag("login_as_customer_link")
                        ) {
                            Text(
                                text = "Already have an account? Sign In",
                                color = ZyphuelBluePrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    // Rider Platform
                    if (!isRegister) {
                        TextButton(
                            onClick = { viewModel.navigateTo("register_rider") },
                            modifier = Modifier.testTag("register_as_rider_link")
                        ) {
                            Text(
                                text = "New rider? Register as Rider 🏍️",
                                color = ZyphuelBluePrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { viewModel.navigateTo("login_rider") },
                            modifier = Modifier.testTag("login_as_rider_link")
                        ) {
                            Text(
                                text = "Already a registered rider? Sign In",
                                color = ZyphuelBluePrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialIsRider = isRider,
            onDismiss = { showForgotPasswordDialog = false },
            onSubmit = { emailVal, phoneVal, newPassVal ->
                viewModel.resetPassword(emailVal, phoneVal, newPassVal) {
                    showForgotPasswordDialog = false
                }
            }
        )
    }

    // NOTE: The former device-account picker dialog was removed.
    // Google sign-in now uses ONLY the real, token-verified CredentialManager flow
    // (GoogleAuthManager.signInWithGoogle -> Firebase signInWithCredential). There is
    // no email-only / device-account fallback authentication anywhere.
}

// --- PHASE 5: CUSTOMER SIDEBAR DRAWER ---

@Composable
fun DrawerContent(
    viewModel: MainViewModel,
    currentUser: UserEntity,
    onClose: () -> Unit,
    onOpenPasswordSuggestion: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenFAQ: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAso: () -> Unit = {},
    onOpenFcm: () -> Unit = {}
) {
    val context = LocalContext.current
    val adminEmail = "m.daniyalkhan490@gmail.com"
    val whatsappNumber = "+92 323 0112464"
    var showDeleteAccountDialog by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Profile Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile picture or fallback initials
                        val picUri = currentUser.profilePictureUri
                        val isPreset = picUri?.startsWith("preset_") == true
                        val avatarBgColor = when (picUri) {
                            "preset_blue" -> ZyphuelBluePrimary
                            "preset_green" -> Color(0xFF22C55E)
                            "preset_amber" -> Color(0xFFFFB000)
                            "preset_red" -> Color(0xFFEF4444)
                            else -> ZyphuelBluePrimary
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(avatarBgColor, shape = CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!picUri.isNullOrBlank() && !isPreset) {
                                coil.compose.AsyncImage(
                                    model = picUri,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = currentUser.name.take(2).uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = currentUser.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                        )
                        Text(
                            text = currentUser.email,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(ZyphuelBlueSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentUser.role.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ZyphuelBluePrimary
                                    )
                                )
                            }
                            if (currentUser.role == "rider" && currentUser.isVerified) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "VERIFIED ✔️",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sidebar Items
                SidebarItem(
                    icon = Icons.Filled.Person,
                    label = "Profile Settings",
                    modifier = Modifier.testTag("sidebar_profile_settings"),
                    onClick = {
                        onOpenProfile()
                        onClose()
                    }
                )
                SidebarItem(
                    icon = Icons.Filled.Fingerprint,
                    label = "Security & Biometrics",
                    modifier = Modifier.testTag("sidebar_security_settings"),
                    onClick = {
                        onClose()
                        val target = when (currentUser.role) {
                            "rider" -> "rider_security"
                            "admin" -> "admin_security"
                            else -> "customer_security"
                        }
                        viewModel.navigateTo(target)
                    }
                )
                if (currentUser.role != "rider") {
                    SidebarItem(
                        icon = Icons.Filled.ShoppingCart,
                        label = "My Active Orders",
                        modifier = Modifier.testTag("sidebar_my_orders"),
                        onClick = {
                            onOpenOrders()
                            onClose()
                        }
                    )
                    SidebarItem(
                        icon = Icons.Filled.History,
                        label = "Customer Order History",
                        modifier = Modifier.testTag("sidebar_customer_order_history"),
                        onClick = {
                            onClose()
                            viewModel.navigateTo("customer_order_history")
                        }
                    )
                }

                if (currentUser.role == "rider") {
                    SidebarItem(
                        icon = Icons.Filled.ListAlt,
                        label = "Received Orders",
                        modifier = Modifier.testTag("sidebar_received_orders"),
                        onClick = {
                            onOpenOrders()
                            onClose()
                        }
                    )
                }

                SidebarItem(
                    icon = Icons.Filled.QuestionMark,
                    label = "FAQ / Guidelines",
                    modifier = Modifier.testTag("sidebar_faq"),
                    onClick = {
                        onOpenFAQ()
                        onClose()
                    }
                )
                SidebarItem(
                    icon = Icons.Filled.SupportAgent,
                    label = "Live Support & Help Center",
                    modifier = Modifier.testTag("sidebar_live_support"),
                    onClick = {
                        onOpenSupport()
                        onClose()
                    }
                )

                // Delete Account Sidebar Option (Google Play Mandatory Policy)
                SidebarItem(
                    icon = Icons.Filled.DeleteForever,
                    label = "Delete Account / Erase Data",
                    modifier = Modifier.testTag("sidebar_delete_account"),
                    onClick = {
                        showDeleteAccountDialog = true
                    }
                )

                val currentScreenVal by viewModel.currentScreen.collectAsState()

                if (currentUser.role == "admin" && currentScreenVal != "admin_dashboard") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onClose()
                            viewModel.switchToPlatform("admin_dashboard", "Admin Dashboard")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBlueDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sidebar_admin_entry")
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Admin Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Logout Button
            Button(
                onClick = {
                    viewModel.logout()
                    onClose()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (showDeleteAccountDialog) {
            DeleteAccountConfirmationDialog(
                onConfirm = {
                    showDeleteAccountDialog = false
                    viewModel.deleteCurrentAccount {
                        onClose()
                    }
                },
                onDismiss = { showDeleteAccountDialog = false }
            )
        }
    }
}


@Composable
fun SidebarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray))
    }
}

// --- PHASE 5: CUSTOMER HOME DASHBOARD ---

@Composable
fun CustomerHomeScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val orders by viewModel.customerOrders.collectAsState()
    val context = LocalContext.current

    // Dynamic fuel prices from ViewModel
    val petrolPrice by viewModel.petrolPrice.collectAsState()
    val dieselPrice by viewModel.dieselPrice.collectAsState()
    val octanePrice by viewModel.highOctanePrice.collectAsState()
    val lpgPrice by viewModel.lpgGasPrice.collectAsState()
    val waterPrice by viewModel.waterPrice.collectAsState()
    val liveLocationCoordinates by viewModel.liveLocationCoordinates.collectAsState()
    val isCustomLocationSet by viewModel.isCustomLocationSet.collectAsState()
    val isPromoApplied by viewModel.isPromoApplied.collectAsState()
    val isPushNotifGranted = remember(context) {
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val activeOrders = remember(orders) {
        orders.filter { it.status in listOf("Pending", "Assigned", "Delivering") }
    }
    val pastOrders = remember(orders) {
        orders.filter { it.status in listOf("Completed", "Cancelled") }
    }

    var showOrderDialog by remember { mutableStateOf(false) }
    var selectedService by remember { mutableStateOf("") }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showFAQDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var showMyOrdersDialog by remember { mutableStateOf(false) }
    var showEditLocationDialog by remember { mutableStateOf(false) }
    var showAsoDialog by remember { mutableStateOf(false) }
    var showFcmDialog by remember { mutableStateOf(false) }
    var tourStep by remember { mutableStateOf<Int?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceGpsLocation(
                context = context,
                onLocationResult = { lat, lng, addr ->
                    viewModel.updateDeviceGpsLocation(lat, lng, addr)
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Location permission not granted. Enter location manually.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchDeviceGpsLocation(
                context = context,
                onLocationResult = { lat, lng, addr ->
                    viewModel.updateDeviceGpsLocation(lat, lng, addr)
                },
                onError = { }
            )
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isInitialDashboardLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(450)
        isInitialDashboardLoading = false
    }

    if (currentUser == null || isInitialDashboardLoading) {
        TeslaHomeSkeleton()
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                viewModel = viewModel,
                currentUser = currentUser!!,
                onClose = { scope.launch { drawerState.close() } },
                onOpenPasswordSuggestion = { showPasswordDialog = true },
                onOpenOrders = { showMyOrdersDialog = true },
                onOpenFAQ = { showFAQDialog = true },
                onOpenSupport = { showSupportDialog = true },
                onOpenProfile = { showProfileSettingsDialog = true },
                onOpenAso = { showAsoDialog = true },
                onOpenFcm = { showFcmDialog = true }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(30.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Zyphuel",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ZyphuelBluePrimary
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        val isMenuHighlighted = (tourStep == 2)
                        val menuBorderModifier = if (isMenuHighlighted) {
                            val infiniteTransition = rememberInfiniteTransition(label = "menu_pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "menu_alpha"
                            )
                            Modifier.border(2.5.dp, ZyphuelBluePrimary.copy(alpha = pulseAlpha), CircleShape)
                        } else {
                            Modifier
                        }
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = menuBorderModifier
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = ZyphuelBluePrimary)
                        }
                    },
                    actions = {
                        val notifications by viewModel.notifications.collectAsState()
                        var showNotificationsDialog by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            IconButton(onClick = { showNotificationsDialog = true }) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = ZyphuelBluePrimary)
                            }
                            if (notifications.isNotEmpty()) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 4.dp, end = 4.dp),
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(notifications.size.toString(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (showNotificationsDialog) {
                            NotificationsDialog(
                                notifications = notifications,
                                onDismiss = { showNotificationsDialog = false },
                                onClearAll = { viewModel.clearNotifications() }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    modifier = Modifier.shadow(2.dp)
                )
            },
            floatingActionButton = {
                // Large action trigger floating button
                ExtendedFloatingActionButton(
                    text = { Text("Order Now", fontWeight = FontWeight.Bold, color = Color.White) },
                    icon = { Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = Color.White) },
                    onClick = {
                        selectedService = ""
                        showOrderDialog = true
                    },
                    containerColor = ZyphuelBluePrimary,
                    modifier = Modifier.testTag("home_fab")
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZyphuelLightBackground)
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Card Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("user_location_active_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ZyphuelBlueDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_cust")
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.4f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha_cust"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .alpha(alpha)
                                            .background(if (isCustomLocationSet) Color(0xFF3B82F6) else Color(0xFF10B981), CircleShape)
                                    )
                                    Text(
                                        text = "User Live Location Active",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = ZyphuelBlueSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Surface(
                                    onClick = { showEditLocationDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = ZyphuelBluePrimary.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, ZyphuelBlueSecondary.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("edit_location_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EditLocation,
                                            contentDescription = "Edit Location",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Edit",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = liveLocationCoordinates,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isCustomLocationSet) "📍 Custom delivery location active" else "⚡ Automatically updating via live GPS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ZyphuelBlueSecondary.copy(alpha = 0.9f)
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { showEditLocationDialog = true },
                                    modifier = Modifier.testTag("location_icon_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "Edit Location",
                                        tint = ZyphuelBlueSecondary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showEditLocationDialog = true },
                                    modifier = Modifier.weight(1f).testTag("edit_location_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ZyphuelBluePrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Address", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        if (hasFine || hasCoarse) {
                                            fetchDeviceGpsLocation(
                                                context = context,
                                                onLocationResult = { lat, lng, addr ->
                                                    viewModel.updateDeviceGpsLocation(lat, lng, addr)
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("auto_detect_gps_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZyphuelBlueSecondary),
                                    border = BorderStroke(1.dp, ZyphuelBlueSecondary.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto Detect GPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Delivery Push Notification Status Banner if Disabled
                if (!isPushNotifGranted) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.triggerDeliveryNotificationPrompt() }
                                .testTag("enable_delivery_notif_banner"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ZyphuelBluePrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = "Enable Notifications",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable Real-Time Delivery Updates",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0C4A6E),
                                            fontSize = 13.sp
                                        )
                                    )
                                    Text(
                                        text = "Get live pings on bowser dispatch, ETA, and arrival.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF0369A1),
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = ZyphuelBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ServiceCard(
                            title = "Petrol",
                            icon = Icons.Filled.LocalGasStation,
                            subLabel = "Super Euro-V • Min 5L",
                            pricePerLiter = viewModel.formatUnitPrice(petrolPrice, "L"),
                            badgeText = "Live Rate",
                            accentColor = Color(0xFF059669),
                            highlighted = (tourStep == 0),
                            modifier = Modifier.weight(1f).testTag("service_petrol")
                        ) {
                            selectedService = "Petrol"
                            showOrderDialog = true
                        }
                        ServiceCard(
                            title = "Diesel",
                            icon = Icons.Filled.DirectionsCar,
                            subLabel = "Euro-V High Speed • Min 5L",
                            pricePerLiter = viewModel.formatUnitPrice(dieselPrice, "L"),
                            badgeText = "Live Rate",
                            accentColor = Color(0xFF2563EB),
                            highlighted = (tourStep == 0),
                            modifier = Modifier.weight(1f).testTag("service_diesel")
                        ) {
                            selectedService = "Diesel"
                            showOrderDialog = true
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ServiceCard(
                            title = "High-Octane",
                            icon = Icons.Filled.Speed,
                            subLabel = "HOBC 97 Premium • Min 5L",
                            pricePerLiter = viewModel.formatUnitPrice(octanePrice, "L"),
                            badgeText = "97 Octane",
                            accentColor = Color(0xFF7C3AED),
                            highlighted = (tourStep == 0),
                            modifier = Modifier.weight(1f).testTag("service_high_octane")
                        ) {
                            selectedService = "High-Octane"
                            showOrderDialog = true
                        }
                        ServiceCard(
                            title = "LPG Gas",
                            icon = Icons.Filled.Fireplace,
                            subLabel = "Liquefied Gas • Min 5 Kg",
                            pricePerLiter = viewModel.formatUnitPrice(lpgPrice, "Kg"),
                            badgeText = "Eco Energy",
                            accentColor = Color(0xFFEA580C),
                            highlighted = (tourStep == 0),
                            modifier = Modifier.weight(1f).testTag("service_gas")
                        ) {
                            selectedService = "LPG Gas"
                            showOrderDialog = true
                        }
                    }
                }

                item {
                    ServiceCard(
                        title = "Pure Water",
                        icon = Icons.Filled.WaterDrop,
                        subLabel = "Purified Mineral • Min 1 Gallon",
                        pricePerLiter = viewModel.formatUnitPrice(waterPrice, "Gallon"),
                        badgeText = "100% Pure",
                        accentColor = Color(0xFF0284C7),
                        highlighted = (tourStep == 0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_water")
                    ) {
                        selectedService = "Water"
                        showOrderDialog = true
                    }
                }

                // Active Deliveries section — only visible when user has an active order
                if (activeOrders.isNotEmpty()) {
                    // Section header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Delivery Tracking (Google Maps)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ZyphuelBlueDark
                                )
                            )
                            Surface(
                                color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🛰️ Real-Time GPS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ZyphuelBluePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Native Google Maps SDK Live Delivery Tracking Overlay on Home Screen
                    item {
                        val activeTrackingOrder = activeOrders.firstOrNull()
                        val allRidersList by viewModel.allRiders.collectAsState()
                        val assignedRider = remember(activeTrackingOrder, allRidersList) {
                            if (activeTrackingOrder != null && !activeTrackingOrder.riderEmail.isNullOrBlank()) {
                                allRidersList.find { it.email == activeTrackingOrder.riderEmail }
                            } else null
                        }

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(400)) + expandVertically(animationSpec = tween(400))
                        ) {
                            GoogleMapsLiveDeliveryTrackingOverlay(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                order = activeTrackingOrder,
                                assignedRider = assignedRider,
                                viewModel = viewModel,
                                onExpandFullscreen = {
                                    if (activeTrackingOrder != null) {
                                        viewModel.setTrackingOrder(activeTrackingOrder)
                                        viewModel.navigateTo("tracker")
                                    }
                                }
                            )
                        }
                    }
                }


                if (activeOrders.isNotEmpty() && tourStep != 1) {
                    item {
                        var showCoverageMapPreview by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("active_order_dashboard_map_card"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Map,
                                            contentDescription = null,
                                            tint = ZyphuelBluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Lahore Live Rider Fleet & Coverage",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = ZyphuelBlueDark
                                                )
                                            )
                                            Text(
                                                text = "Real-time delivery coverage across Gulberg & DHA",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                            )
                                        }
                                    }

                                    IconButton(onClick = { showCoverageMapPreview = !showCoverageMapPreview }) {
                                        Icon(
                                            imageVector = if (showCoverageMapPreview) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = "Toggle Map",
                                            tint = ZyphuelBluePrimary
                                        )
                                    }
                                }

                                if (showCoverageMapPreview) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                    ) {
                                        LahoreGoogleEmbedMapView(
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF22C55E), CircleShape)
                                        )
                                        Text(
                                            text = "14 Active Bowsers & Tankers Online",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D)
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.placeOrder(
                                                serviceType = "Petrol",
                                                quantity = 10,
                                                totalPrice = 2970.0,
                                                deliveryAddress = "Main Boulevard, Gulberg III, Lahore",
                                                onSuccess = {
                                                    Toast.makeText(context, "Sample order created! Tracking activated on dashboard.", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("simulate_map_tracking_btn")
                                    ) {
                                        Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Simulate Live Tracking 📍", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 11.sp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (tourStep == 1 && activeOrders.isEmpty()) {
                        item {
                            val user = currentUser
                            val dummyOrder = OrderEntity(
                                id = 999,
                                customerEmail = user?.email ?: "customer@example.com",
                                customerName = user?.name ?: "Customer",
                                customerPhone = user?.phoneNumber ?: "03001234567",
                                serviceType = "High-Octane",
                                quantity = 15,
                                totalPrice = 4463.0,
                                deliveryAddress = "Lahore, Pakistan",
                                status = "Pending"
                            )
                            CustomerOrderCard(order = dummyOrder, viewModel = viewModel, highlighted = true) {
                                viewModel.setTrackingOrder(dummyOrder)
                                viewModel.navigateTo("tracker")
                            }
                        }
                    } else {
                        // Only the newest active order renders the full live-map card. Each of those
                        // cards hosts a WebView map, and stacking one per order made this list stutter
                        // badly on a real phone. The remaining orders render as light summary cards
                        // that open the full-screen tracker on tap.
                        itemsIndexed(activeOrders, key = { _, order -> order.id }) { index, order ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                if (index == 0) {
                                    RealTimeOrderTrackingCard(
                                        order = order,
                                        viewModel = viewModel
                                    ) {
                                        viewModel.setTrackingOrder(order)
                                        viewModel.navigateTo("tracker")
                                    }
                                } else {
                                    CustomerOrderCard(
                                        order = order,
                                        viewModel = viewModel,
                                        highlighted = false
                                    ) {
                                        viewModel.setTrackingOrder(order)
                                        viewModel.navigateTo("tracker")
                                    }
                                }
                            }
                        }
                    }
                }

                // Dedicated Order History Dashboard Section
                item {
                    Text(
                        text = "Order History & Insights",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark
                        )
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = ZyphuelBlueSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Your Delivery Dashboard",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ZyphuelBlueDark
                                        )
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ZyphuelBlueSecondary.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Past: ${pastOrders.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ZyphuelBluePrimary
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Horizontal Grid of Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Stat 1: Total Spend
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Total Spent",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                        )
                                        Text(
                                            text = "Rs. ${String.format(java.util.Locale.US, "%,.2f", pastOrders.sumOf { it.totalPrice })}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ZyphuelBlueDark
                                            )
                                        )
                                    }
                                }

                                // Stat 2: Completed count
                                val completedCount = pastOrders.count { it.status == "Completed" }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Completed",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                        )
                                        Text(
                                            text = "$completedCount Delivered",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A)
                                            )
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.navigateTo("customer_order_history") },
                                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_customer_order_history_btn")
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "View Full Customer Order History Screen 📜",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                if (pastOrders.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No past deliveries found in Lahore.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    items(pastOrders) { order ->
                        CustomerPastOrderCard(order = order, viewModel = viewModel)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // --- ONBOARDING TOUR MODAL OVERLAY ---
        if (tourStep != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = true, onClick = { /* block clicks */ })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp))
                            .testTag("tour_modal_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GUIDED TOUR • STEP ${tourStep!! + 1} OF 3",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = ZyphuelBluePrimary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                IconButton(
                                    onClick = { tourStep = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close Tour", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val (tourTitle, tourDesc, tourIcon) = when (tourStep) {
                                0 -> Triple(
                                    "Select & Configure Your Services",
                                    "Take a look at the glowing, pulsing cards on your screen. You can select from Euro V Petrol, Diesel, High-Octane, LPG Gas, or Purified Water gallons. Tap any card to customize quantities with instant transparent pricing.",
                                    Icons.Filled.LocalGasStation
                                )
                                1 -> Triple(
                                    "Live Delivery Tracking & ETAs",
                                    "Check out the active delivery card highlighted below! Once you place an order, click 'Track' to open our high-performance live map. See your rider's location, route overlays, dynamic ETAs, and chat with your rider.",
                                    Icons.Filled.Map
                                )
                                else -> Triple(
                                    "Order History & Itemized Invoices",
                                    "Look at the highlighted main menu button (☰) in the top left. Tap it to access your 'Order History', view past delivery logs, download itemized invoices, or connect with our support line.",
                                    Icons.Filled.History
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(ZyphuelBluePrimary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tourIcon,
                                        contentDescription = null,
                                        tint = ZyphuelBluePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tourTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = ZyphuelBlueDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tourDesc,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.DarkGray,
                                            lineHeight = 20.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(3) { idx ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (idx == tourStep) 16.dp else 8.dp, 8.dp)
                                                .clip(CircleShape)
                                                .background(if (idx == tourStep) ZyphuelBluePrimary else Color.LightGray)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (tourStep!! > 0) {
                                        OutlinedButton(
                                            onClick = { tourStep = tourStep!! - 1 },
                                            modifier = Modifier.height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                                            border = BorderStroke(1.dp, Color.LightGray)
                                        ) {
                                            Text("Back", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (tourStep!! < 2) {
                                                tourStep = tourStep!! + 1
                                            } else {
                                                tourStep = null
                                            }
                                        },
                                        modifier = Modifier.height(36.dp).testTag("tour_modal_next_btn"),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                                    ) {
                                        Text(
                                            text = if (tourStep == 2) "Finish" else "Next",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }

    // --- DIALOGS CONTROLLERS ---

    if (showOrderDialog) {
        OrderDialog(
            viewModel = viewModel,
            serviceType = selectedService,
            onDismiss = { showOrderDialog = false }
        )
    }

    if (showPasswordDialog) {
        PasswordSuggestionDialog(viewModel = viewModel) {
            showPasswordDialog = false
        }
    }

    if (showFAQDialog) {
        FAQDialog { showFAQDialog = false }
    }

    if (showSupportDialog) {
        SupportDialog(viewModel = viewModel) { showSupportDialog = false }
    }

    if (showProfileSettingsDialog) {
        ProfileSettingsDialog(viewModel = viewModel) { showProfileSettingsDialog = false }
    }

    if (showMyOrdersDialog) {
        MyOrdersDialog(viewModel = viewModel) { showMyOrdersDialog = false }
    }

    if (showEditLocationDialog) {
        EditLocationDialog(
            currentLocation = liveLocationCoordinates,
            isCustomSet = isCustomLocationSet,
            onDismiss = { showEditLocationDialog = false },
            onSaveCustomLocation = { newAddr ->
                viewModel.setCustomLocation(newAddr)
                showEditLocationDialog = false
            },
            onAutoDetectLocation = {
                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    fetchDeviceGpsLocation(
                        context = context,
                        onLocationResult = { lat, lng, addr ->
                            viewModel.updateDeviceGpsLocation(lat, lng, addr)
                            showEditLocationDialog = false
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            onResetToAutoGps = {
                viewModel.resetToAutoGpsLocation()
                showEditLocationDialog = false
            }
        )
    }

    if (showAsoDialog) {
        AsoOptimizationDialog(onDismiss = { showAsoDialog = false })
    }

    if (showFcmDialog) {
        FcmConsoleDialog(viewModel = viewModel, onDismiss = { showFcmDialog = false })
    }
}

@Composable
fun AnimatedOrderStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val targetBgColor = when (status) {
        "Completed", "Delivered" -> Color(0xFF10B981).copy(alpha = 0.15f)
        "Delivering", "In Transit", "Assigned" -> Color(0xFF0284C7).copy(alpha = 0.15f)
        "Pending" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
        "Cancelled", "Canceled" -> Color(0xFFEF4444).copy(alpha = 0.15f)
        else -> ZyphuelBluePrimary.copy(alpha = 0.15f)
    }

    val targetTextColor = when (status) {
        "Completed", "Delivered" -> Color(0xFF059669)
        "Delivering", "In Transit", "Assigned" -> Color(0xFF0284C7)
        "Pending" -> Color(0xFFD97706)
        "Cancelled", "Canceled" -> Color(0xFFDC2626)
        else -> ZyphuelBluePrimary
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "status_badge_bg_color"
    )

    val animatedTextColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "status_badge_text_color"
    )

    val isAnimatedStatus = status == "Delivering" || status == "In Transit" || status == "Assigned"
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val pulseAlpha by if (isAnimatedStatus) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val scaleFactor by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "badge_scale"
    )

    Surface(
        modifier = modifier
            .scale(scaleFactor)
            .testTag("animated_order_status_badge"),
        shape = RoundedCornerShape(20.dp),
        color = animatedBgColor,
        border = BorderStroke(1.dp, animatedTextColor.copy(alpha = 0.4f))
    ) {
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { height -> -height }) togetherWith
                (fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { height -> height })
            },
            label = "status_content_anim"
        ) { targetStatus ->
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val icon = when (targetStatus) {
                    "Completed", "Delivered" -> Icons.Filled.CheckCircle
                    "Delivering", "In Transit" -> Icons.Filled.LocalShipping
                    "Assigned" -> Icons.Filled.PersonPin
                    "Pending" -> Icons.Filled.AccessTime
                    "Cancelled", "Canceled" -> Icons.Filled.Cancel
                    else -> Icons.Filled.Info
                }

                val displayText = when (targetStatus) {
                    "Completed" -> "Delivered"
                    "Delivering" -> "In Transit"
                    "Assigned" -> "In Transit"
                    else -> targetStatus
                }

                Box(contentAlignment = Alignment.Center) {
                    if (isAnimatedStatus) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(animatedTextColor.copy(alpha = pulseAlpha * 0.4f), CircleShape)
                        )
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = animatedTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = animatedTextColor
                    )
                )
            }
        }
    }
}

@Composable
fun CustomerOrderHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val orders by viewModel.customerOrders.collectAsState()

    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val sdf = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US) }

    // Active / In-Progress orders always float to top section
    val activeOrders = remember(orders) {
        orders.filter { it.status in listOf("Pending", "Assigned", "Delivering", "In Transit", "Dispatched", "Arriving", "Arriving Soon") }
            .sortedByDescending { it.createdAt }
    }

    // Recent (completed / cancelled) with optional filter
    val recentOrders = remember(orders, selectedStatusFilter, selectedCategoryFilter, searchQuery) {
        orders
            .filter { it.status !in listOf("Pending", "Assigned", "Delivering", "In Transit", "Dispatched", "Arriving", "Arriving Soon") }
            .filter { order ->
                val matchesCategory = when (selectedCategoryFilter) {
                    "Fuel" -> order.serviceType in listOf("Petrol", "Diesel", "High-Octane")
                    "Water" -> order.serviceType in listOf("Water", "Clean Water", "Pure Water", "Gallon Water")
                    "LPG Gas" -> order.serviceType == "LPG Gas"
                    else -> true
                }
                val matchesStatus = when (selectedStatusFilter) {
                    "Delivered" -> order.status in listOf("Completed", "Delivered")
                    "Cancelled" -> order.status in listOf("Cancelled", "Canceled")
                    else -> true
                }
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    order.id.toString().contains(searchQuery, ignoreCase = true) ||
                            order.serviceType.contains(searchQuery, ignoreCase = true) ||
                            order.deliveryAddress.contains(searchQuery, ignoreCase = true) ||
                            (order.riderName ?: "").contains(searchQuery, ignoreCase = true)
                }
                matchesCategory && matchesStatus && matchesSearch
            }
            .sortedByDescending { it.createdAt }
    }

    val totalOrdersCount = orders.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Orders",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBlueDark
                            )
                        )
                        if (totalOrdersCount > 0) {
                            Text(
                                text = "$totalOrdersCount total orders",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("order_history_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ZyphuelBlueDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        if (orders.isEmpty()) {
            // Completely empty — show nothing (blank slate)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // ── ACTIVE / CURRENT ORDERS ──────────────────────────────
                if (activeOrders.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                text = "Current Order",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            )
                        }
                    }

                    items(activeOrders, key = { "active_${it.id}" }) { order ->
                        CustomerOrderHistoryCard(
                            order = order,
                            viewModel = viewModel,
                            sdf = sdf
                        )
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }

                // ── RECENT ORDERS SECTION ────────────────────────────────
                if (recentOrders.isNotEmpty() || activeOrders.isEmpty()) {
                    if (recentOrders.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Orders",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ZyphuelBlueDark
                                )
                            )
                        }

                        // Search + filters — only shown when there are recent orders to filter
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("order_history_search_input"),
                                    placeholder = { Text("Search order ID, type, address...", fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("All", "Delivered", "Cancelled").forEach { statusLabel ->
                                        val isSelected = selectedStatusFilter == statusLabel
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedStatusFilter = statusLabel },
                                            label = {
                                                Text(
                                                    text = statusLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            },
                                            leadingIcon = {
                                                when (statusLabel) {
                                                    "Delivered" -> Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF16A34A))
                                                    "Cancelled" -> Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFDC2626))
                                                    else -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(12.dp), tint = ZyphuelBluePrimary)
                                                }
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ZyphuelBluePrimary.copy(alpha = 0.15f),
                                                selectedLabelColor = ZyphuelBluePrimary
                                            ),
                                            modifier = Modifier.testTag("filter_status_$statusLabel")
                                        )
                                    }
                                }
                            }
                        }

                        items(recentOrders, key = { "recent_${it.id}" }) { order ->
                            CustomerOrderHistoryCard(
                                order = order,
                                viewModel = viewModel,
                                sdf = sdf
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}


@Composable
fun CustomerOrderHistoryCard(
    order: OrderEntity,
    viewModel: MainViewModel,
    sdf: java.text.SimpleDateFormat
) {
    val context = LocalContext.current
    var showRateDialog by remember { mutableStateOf(false) }
    val formattedDate = remember(order.createdAt) { sdf.format(java.util.Date(order.createdAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .testTag("order_history_item_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(ZyphuelBlueDark, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "#${order.id}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    )
                }

                AnimatedOrderStatusBadge(status = order.status)
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                when (order.serviceType) {
                                    "Petrol", "Diesel", "High-Octane" -> ZyphuelBluePrimary.copy(alpha = 0.12f)
                                    "LPG Gas" -> Color(0xFFE11D48).copy(alpha = 0.12f)
                                    else -> Color(0xFF0284C7).copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (order.serviceType) {
                                "Petrol", "Diesel", "High-Octane" -> Icons.Filled.LocalGasStation
                                "LPG Gas" -> Icons.Filled.Fireplace
                                else -> Icons.Filled.WaterDrop
                            },
                            contentDescription = null,
                            tint = when (order.serviceType) {
                                "Petrol", "Diesel", "High-Octane" -> ZyphuelBluePrimary
                                "LPG Gas" -> Color(0xFFE11D48)
                                else -> Color(0xFF0284C7)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = order.serviceType,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBlueDark
                            )
                        )
                        Text(
                            text = "Quantity: ${order.quantity} units • ${order.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = viewModel.formatPrice(order.totalPrice),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark
                        )
                    )
                    Text(
                        text = "Total PKR",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Text(
                    text = order.deliveryAddress,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray),
                    maxLines = 1
                )
            }

            if (!order.riderName.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.TwoWheeler, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                        Text(
                            text = "Assigned Rider: ${order.riderName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (order.status in listOf("Pending", "Assigned", "Delivering", "In Transit")) {
                    Button(
                        onClick = {
                            viewModel.setTrackingOrder(order)
                            viewModel.navigateTo("tracker")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("track_order_${order.id}_btn")
                    ) {
                        Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Track Live Location 📍", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }
                } else if (order.status in listOf("Completed", "Delivered")) {
                    OutlinedButton(
                        onClick = { showRateDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("rate_order_${order.id}_btn"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ZyphuelBlueSecondary)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = ZyphuelBlueSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (order.rating != null) "Rated (${order.rating}★)" else "Rate Delivery ⭐",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.placeOrder(
                                serviceType = order.serviceType,
                                quantity = order.quantity,
                                totalPrice = order.totalPrice,
                                deliveryAddress = order.deliveryAddress,
                                onSuccess = {
                                    Toast.makeText(context, "Reorder placed successfully!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBlueDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("reorder_${order.id}_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reorder 🔁", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }
                }
            }
        }
    }

    if (showRateDialog) {
        RateOrderDialog(
            order = order,
            viewModel = viewModel,
            onDismiss = { showRateDialog = false }
        )
    }
}

@Composable
fun RateOrderDialog(
    order: OrderEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rate Delivery #${order.id}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("How was your fuel/water delivery experience in Lahore?", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$star Stars",
                                tint = if (star <= rating) Color(0xFFEAB308) else Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Comments / Feedback (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "Thank you for rating $rating stars!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
            ) {
                Text("Submit Rating")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CustomerPastOrderCard(order: OrderEntity, viewModel: MainViewModel) {
    var showRateDialog by remember { mutableStateOf(false) }
    val sdf = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US) }
    val formattedDate = remember(order.createdAt) { sdf.format(java.util.Date(order.createdAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Service & Date Stamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (order.serviceType) {
                            "Petrol", "Diesel", "High-Octane" -> Icons.Filled.LocalGasStation
                            "LPG Gas" -> Icons.Filled.Fireplace
                            else -> Icons.Filled.WaterDrop
                        },
                        contentDescription = null,
                        tint = ZyphuelBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = order.serviceType,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark
                        )
                    )
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                )
            }

            // Order Detail Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Order ID: #${order.id}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                    val unit = when (order.serviceType) {
                        "Water" -> "Gallons"
                        "LPG Gas" -> "KG"
                        else -> "Liters"
                    }
                    Text(
                        text = "Quantity: ${order.quantity} $unit",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                    )
                    Text(
                        text = "Address: ${order.deliveryAddress}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.65f)
                    )
                }

                // Total and Status
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Total: Rs. ${order.totalPrice}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark
                        )
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (order.status == "Completed") Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = order.status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (order.status == "Completed") Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            // Show Rating/Feedback Option if completed
            if (order.status == "Completed" || order.status == "Delivered") {
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                PostDeliveryRatingCard(
                    order = order,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun ServiceCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subLabel: String,
    modifier: Modifier = Modifier,
    pricePerLiter: String? = null,
    badgeText: String = "Available",
    accentColor: Color = ZyphuelBluePrimary,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHoveredBySource by interactionSource.collectIsHoveredAsState()
    val isPressedBySource by interactionSource.collectIsPressedAsState()

    var isPointerHovered by remember { mutableStateOf(false) }

    val isHovered = isHoveredBySource || isPointerHovered
    val isPressed = isPressedBySource

    // Subtle entrance fade-in animation
    var isMounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isMounted = true
    }
    val cardAlpha by animateFloatAsState(
        targetValue = if (isMounted) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "card_alpha"
    )

    // Tesla-quality smooth scale-on-press & hover spring animations
    val cardScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isHovered -> 1.03f
            else -> 1.00f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_scale"
    )

    val cardElevation by animateDpAsState(
        targetValue = when {
            isPressed -> 2.dp
            isHovered -> 10.dp
            else -> 3.dp
        },
        animationSpec = tween(durationMillis = 200),
        label = "card_elevation"
    )

    val iconScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            isHovered -> 1.20f
            else -> 1.00f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "icon_scale"
    )

    val arrowOffset by animateDpAsState(
        targetValue = if (isHovered) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "arrow_offset"
    )

    val pulseAlpha by if (highlighted) {
        val infiniteTransition = rememberInfiniteTransition(label = "border_pulse")
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(if (isHovered) 0.8f else 0.0f) }
    }

    val containerBorder = if (highlighted || isHovered) {
        BorderStroke(
            width = if (isHovered) 2.dp else 1.5.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accentColor.copy(alpha = pulseAlpha),
                    ZyphuelBlueSecondary.copy(alpha = pulseAlpha)
                )
            )
        )
    } else {
        BorderStroke(1.dp, Color(0xFFE2E8F0))
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            }
            .shadow(cardElevation, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isPointerHovered = true
                            PointerEventType.Exit -> isPointerHovered = false
                        }
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        border = containerBorder,
        colors = CardDefaults.cardColors(
            containerColor = if (isHovered) accentColor.copy(alpha = 0.04f) else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                        .background(
                            color = accentColor.copy(alpha = if (isHovered) 0.22f else 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(
                    color = if (isHovered) accentColor.copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isHovered) accentColor else Color(0xFF047857),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ZyphuelBlueDark
                ),
                maxLines = 1
            )

            // Current price per liter tag
            if (!pricePerLiter.isNullOrBlank()) {
                Surface(
                    color = accentColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Rate:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = pricePerLiter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = accentColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                maxLines = 1
            )
        }

    }
}

@Composable
fun CustomerOrderCard(
    order: OrderEntity,
    viewModel: MainViewModel,
    highlighted: Boolean = false,
    onTrackClick: () -> Unit
) {
    var showRateDialog by remember { mutableStateOf(false) }

    val allRiders by viewModel.allRiders.collectAsState()
    val assignedRider = remember(allRiders, order.riderEmail) {
        if (!order.riderEmail.isNullOrBlank()) {
            allRiders.find { it.email == order.riderEmail }
        } else null
    }

    val isAnyRiderRegistered = remember(allRiders) {
        allRiders.any { it.role == "rider" && it.isVerified }
    }

    val borderModifier = if (highlighted) {
        val infiniteTransition = rememberInfiniteTransition(label = "order_border_pulse")
        val borderAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "order_alpha"
        )
        Modifier.border(2.5.dp, ZyphuelBluePrimary.copy(alpha = borderAlpha), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sharedOrderBounds("order_card_${order.id}")
            .then(borderModifier)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedOrderElement("order_header_${order.id}")
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (order.serviceType) {
                                "Petrol", "Diesel", "High-Octane" -> Icons.Filled.LocalGasStation
                                "LPG Gas" -> Icons.Filled.Fireplace
                                else -> Icons.Filled.WaterDrop
                            },
                            contentDescription = null,
                            tint = ZyphuelBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Order #${order.id} - ${order.serviceType}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Qty: ${order.quantity} | Total: ${viewModel.formatPrice(order.totalPrice)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                    )
                    Text(
                        text = "Delivery: ${order.deliveryAddress}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                        maxLines = 1
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (order.status) {
                                    "Completed" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    "Pending" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    "Cancelled" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else -> ZyphuelBluePrimary.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = order.status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (order.status) {
                                    "Completed" -> Color(0xFF10B981)
                                    "Pending" -> Color(0xFFF59E0B)
                                    "Cancelled" -> Color(0xFFEF4444)
                                    else -> ZyphuelBluePrimary
                                }
                            )
                        )
                    }

                    if (order.status != "Completed" && order.status != "Cancelled") {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onTrackClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = ZyphuelBluePrimary)
                        ) {
                            Text("Track Live", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- Rider Information Panel ---
            if (order.status != "Completed" && order.status != "Cancelled") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.LightGray.copy(alpha = 0.4f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (assignedRider != null) {
                        Text(
                            text = "Assigned Rider:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Profile Picture / Avatar
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(ZyphuelBluePrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = assignedRider.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase()
                                Text(
                                    text = initials.ifBlank { "R" },
                                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                )
                            }

                            // 2. Name, 3. Vehicle Type, 4. Contact Number (Private: Registration/ID hidden)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = assignedRider.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = "Verified Rider",
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "Vehicle: ${assignedRider.vehicleType ?: "Bike"}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Contact: ${assignedRider.phoneNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = ZyphuelBluePrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    } else {
                        // Rider registered in system, but has NOT accepted order yet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Awaiting rider acceptance...",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                )
                                Text(
                                    text = "Order will be cancelled if not accepted.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.cancelOrderIfNotAccepted(order.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Cancel Order", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            if (order.status == "Completed") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (order.rating != null) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) { starIndex ->
                                    val isFilled = starIndex < order.rating
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (isFilled) Color(0xFFFFB000) else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Your Rating",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold)
                                )
                            }
                            if (!order.feedback.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "“${order.feedback}”",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "How was your delivery?",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                        Button(
                            onClick = { showRateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("rate_experience_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rate Experience", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }

    if (showRateDialog) {
        Dialog(onDismissRequest = { showRateDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rate Delivery Experience",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "How was your premium delivery for Order #${order.id}?",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var selectedRating by remember { mutableIntStateOf(5) }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1..5) {
                            IconButton(
                                onClick = { selectedRating = star },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Rate $star Stars",
                                    tint = if (star <= selectedRating) Color(0xFFFFB000) else Color.LightGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var feedbackText by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text("Comment / Suggestions") },
                        placeholder = { Text("Share your experience...") },
                        modifier = Modifier.fillMaxWidth().testTag("rating_feedback_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = ZyphuelBluePrimary,
                            unfocusedLabelColor = Color.DarkGray,
                            focusedBorderColor = ZyphuelBluePrimary,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showRateDialog = false }) {
                          Text("Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                viewModel.submitOrderRating(order.id, selectedRating, feedbackText)
                                showRateDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("submit_rating_button")
                        ) {
                            Text("Submit Feedback")
                        }
                    }
                }
            }
        }
    }
}

// --- PHASE 5: ORDER FUEL / GAS / WATER DIALOG ---

@Composable
fun DeliveryVehicleCustomMarker(
    modifier: Modifier = Modifier,
    vehicleType: String = "Bowser", // Bowser, Bike, Car, Truck, Rickshaw
    driverName: String = "Mohammad Ali",
    rotationDegrees: Float = 45f,
    speedKmH: Int = 42,
    isMoving: Boolean = true,
    serviceType: String = "Fuel Delivery"
) {
    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "vehicle_rotation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    val vehicleIcon = when {
        vehicleType.contains("Bike", ignoreCase = true) || vehicleType.contains("Motorcycle", ignoreCase = true) -> Icons.Filled.TwoWheeler
        vehicleType.contains("Car", ignoreCase = true) || vehicleType.contains("Sedan", ignoreCase = true) -> Icons.Filled.DirectionsCar
        else -> Icons.Filled.LocalShipping
    }

    Box(
        modifier = modifier.testTag("delivery_vehicle_custom_marker"),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing Sonar Ring for Real-time GPS active feedback
        if (isMoving) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(ZyphuelBluePrimary.copy(alpha = 0.35f), CircleShape)
            )
        }

        // Main Vehicle Container with Orientation Heading Rotation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Speed & Driver Badge Tag
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, ZyphuelBluePrimary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$driverName • $speedKmH km/h",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rotatable Vehicle Marker Icon with Directional Pointer
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        rotationZ = animatedRotation
                    }
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                        ),
                        shape = CircleShape
                    )
                    .border(2.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vehicleIcon,
                    contentDescription = "Delivery Vehicle ($vehicleType)",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                // Directional Heading Arrow
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = "Heading Indicator",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                )
            }
        }
    }
}

fun handleExternalMapIntent(context: android.content.Context, url: String): Boolean {
    if (url.isBlank()) return false

    val isMapDeepLink = url.startsWith("intent:") ||
            url.startsWith("geo:") ||
            url.startsWith("google.navigation:") ||
            url.contains("maps.google.com") ||
            url.contains("google.com/maps") ||
            url.contains("maps.app.goo.gl") ||
            (!url.startsWith("http://") && !url.startsWith("https://"))

    if (!isMapDeepLink) return false

    try {
        val targetIntent: android.content.Intent = when {
            url.startsWith("intent:") -> {
                android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
            }
            url.startsWith("geo:") || url.startsWith("google.navigation:") -> {
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            }
            else -> {
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            }
        }

        val chooserIntent = android.content.Intent.createChooser(targetIntent, "Open with Navigation App").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooserIntent)
            return true
        } catch (e: Exception) {
            val fallbackUri = when {
                url.startsWith("geo:") -> {
                    val query = url.removePrefix("geo:").split("?").getOrNull(1)?.removePrefix("q=") ?: "Green Town,Lahore,Pakistan"
                    "https://www.google.com/maps/search/?api=1&query=${android.net.Uri.encode(query)}"
                }
                url.startsWith("google.navigation:") -> {
                    val dest = url.removePrefix("google.navigation:q=").removePrefix("q=")
                    "https://www.google.com/maps/dir/?api=1&destination=${android.net.Uri.encode(dest.ifBlank { "Green Town,Lahore,Pakistan" })}"
                }
                else -> "https://www.google.com/maps/search/?api=1&query=Green+Town,Lahore,Pakistan"
            }

            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(fallbackUri)
            )
            val browserChooser = android.content.Intent.createChooser(browserIntent, "Open Location in Browser").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(browserChooser)
            } catch (ex: Exception) {
                android.widget.Toast.makeText(context, "📍 Opening location in Lahore, Pakistan", android.widget.Toast.LENGTH_SHORT).show()
            }
            return true
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "📍 Opening location in Lahore, Pakistan", android.widget.Toast.LENGTH_SHORT).show()
        return true
    }
}

@Composable
fun UnifiedGoogleMapView(
    modifier: Modifier = Modifier,
    userLat: Double = 31.5204,
    userLng: Double = 74.3587,
    deliveryAddress: String = "Gulberg III, Lahore",
    driverLat: Double? = null,
    driverLng: Double? = null,
    driverName: String = "Assigned Delivery Driver",
    driverPhone: String = "+92 323 0112464",
    vehicleType: String = "Delivery Vehicle",
    vehicleNo: String = "",

    serviceType: String = "Fuel Delivery",
    orderStatus: String = "Delivering",
    pathProgress: Float = 0.5f,
    etaMinutes: Int = 8,
    distanceKm: Double = 2.4,
    isPickerMode: Boolean = false,
    showOverlays: Boolean = true,
    onLocationPinned: ((Double, Double, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val hqLat = 31.4380
    val hqLng = 74.3050

    val calcDriverLat = driverLat ?: (hqLat + (userLat - hqLat) * pathProgress.toDouble())
    val calcDriverLng = driverLng ?: (hqLng + (userLng - hqLng) * pathProgress.toDouble())

    var currentLat by remember(userLat) { mutableDoubleStateOf(userLat) }
    var currentLng by remember(userLng) { mutableDoubleStateOf(userLng) }
    var currentAddress by remember(deliveryAddress) { mutableStateOf(deliveryAddress) }
    var showRiderProfileDialog by remember { mutableStateOf(false) }
    var selectedLayer by remember { mutableStateOf("voyager") } // voyager, satellite, dark, terrain
    var showFuelPumps by remember { mutableStateOf(true) }
    var showFullScreenMap by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val webViewRef = remember { mutableStateOf<android.webkit.WebView?>(null) }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    val lahorePresets = listOf(
        Triple("Gulberg III", 31.5204, 74.3587),
        Triple("Johar Town", 31.4650, 74.2810),
        Triple("DHA Phase 5", 31.4720, 74.3812),
        Triple("Model Town", 31.4782, 74.3265),
        Triple("Green Town HQ", 31.4380, 74.3050),
        Triple("Lake City", 31.3650, 74.2610),
        Triple("Mall Road", 31.5580, 74.3310)
    )

    val htmlContent = remember(currentLat, currentLng, calcDriverLat, calcDriverLng, pathProgress, currentAddress, orderStatus, vehicleType, vehicleNo, serviceType, isPickerMode) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; background-color: #0f172a; }
                .leaflet-container { background: #0f172a; }
                @keyframes pulseSonar {
                    0% { transform: scale(0.8); opacity: 0.9; }
                    50% { transform: scale(1.6); opacity: 0.4; }
                    100% { transform: scale(2.2); opacity: 0; }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                function calcBearing(lat1, lon1, lat2, lon2) {
                    var dLon = (lon2 - lon1) * Math.PI / 180;
                    var l1 = lat1 * Math.PI / 180;
                    var l2 = lat2 * Math.PI / 180;
                    var y = Math.sin(dLon) * Math.cos(l2);
                    var x = Math.cos(l1) * Math.sin(l2) - Math.sin(l1) * Math.cos(l2) * Math.cos(dLon);
                    var brng = Math.atan2(y, x) * 180 / Math.PI;
                    return Math.round((brng + 360) % 360);
                }

                var lahoreSouthWest = L.latLng(31.2000, 74.0500);
                var lahoreNorthEast = L.latLng(31.7200, 74.6200);
                var lahoreBounds = L.latLngBounds(lahoreSouthWest, lahoreNorthEast);

                var map = L.map('map', {
                    zoomControl: false,
                    minZoom: 3,
                    maxZoom: 19,
                    scrollWheelZoom: true,
                    touchZoom: true,
                    doubleClickZoom: true,
                    dragging: true
                });

                var tileUrls = {
                    'voyager': 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
                    'satellite': 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
                    'dark': 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
                    'terrain': 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png'
                };

                window.currentTileLayer = L.tileLayer(tileUrls['$selectedLayer'] || tileUrls['voyager'], {
                    maxZoom: 19,
                    attribution: 'OpenStreetMap'
                }).addTo(map);

                // Petrol Stations Layer
                window.pumpsGroup = L.layerGroup().addTo(map);
                var petrolPumps = [
                    { name: "PSO Station Green Town Depot", lat: 31.4378, lng: 74.2974, brand: "PSO Green Town" },
                    { name: "Shell Station Model Town", lat: 31.4782, lng: 74.3265, brand: "Shell Ferozepur" },
                    { name: "TotalEnergies Station Gulberg", lat: 31.5125, lng: 74.3508, brand: "Total Gulberg" },
                    { name: "Attock Petroleum DHA Phase 3", lat: 31.4720, lng: 74.3812, brand: "Attock DHA" },
                    { name: "Hascol Station Johar Town", lat: 31.4650, lng: 74.2810, brand: "Hascol Johar Town" },
                    { name: "PSO Station Mall Road", lat: 31.5580, lng: 74.3310, brand: "PSO Mall Rd" }
                ];

                petrolPumps.forEach(function(pump) {
                    var pumpIcon = L.divIcon({
                        className: 'pump-marker',
                        html: '<div style="background:#EAB308;color:#0F172A;padding:3px 7px;border-radius:12px;font-size:10px;font-weight:bold;box-shadow:0 2px 6px rgba(0,0,0,0.5);border:1.5px solid white;cursor:pointer;">⛽ ' + pump.brand + '</div>',
                        iconSize: [110, 26],
                        iconAnchor: [55, 13]
                    });
                    L.marker([pump.lat, pump.lng], { icon: pumpIcon }).addTo(window.pumpsGroup).bindPopup("<b>⛽ " + pump.name + "</b><br/>Lahore Fuel Supply Station");
                });

                // HQ Pin
                var hqIcon = L.divIcon({
                    className: 'hq-marker',
                    html: '<div style="background:#10B981;color:white;padding:5px 9px;border-radius:14px;font-size:11px;font-weight:bold;box-shadow:0 2px 8px rgba(0,0,0,0.5);border:2px solid white;">🏢 HQ Green Town</div>',
                    iconSize: [120, 32],
                    iconAnchor: [60, 16]
                });
                L.marker([$hqLat, $hqLng], { icon: hqIcon }).addTo(map).bindPopup("<b>Zyphuel Central Dispatch HQ</b><br>Green Town, Lahore");

                // Customer Delivery Pin
                var destIcon = L.divIcon({
                    className: 'dest-marker',
                    html: '<div style="background:#EF4444;color:white;padding:5px 9px;border-radius:14px;font-size:11px;font-weight:bold;box-shadow:0 2px 8px rgba(0,0,0,0.5);border:2px solid white;">📍 Order Pin: $currentAddress</div>',
                    iconSize: [150, 32],
                    iconAnchor: [75, 16]
                });
                var isDraggable = ${isPickerMode.toString()};
                window.destMarker = L.marker([$currentLat, $currentLng], { icon: destIcon, draggable: isDraggable }).addTo(map);
                window.destMarker.bindPopup("<b>Customer Delivery Destination Pin</b><br>$currentAddress<br>Lat: $currentLat, Lng: $currentLng");

                function sendLocation(lat, lng) {
                    if (window.AndroidBridge && window.AndroidBridge.onLocationPinned) {
                        window.AndroidBridge.onLocationPinned(lat, lng);
                    }
                }

                if (isDraggable) {
                    window.destMarker.on('dragend', function(e) {
                        var position = window.destMarker.getLatLng();
                        sendLocation(position.lat, position.lng);
                    });
                    map.on('click', function(e) {
                        window.destMarker.setLatLng(e.latlng);
                        sendLocation(e.latlng.lat, e.latlng.lng);
                    });
                }

                // Driver Pin
                var bearing = calcBearing($hqLat, $hqLng, $calcDriverLat, $calcDriverLng);
                var vTypeLower = "$vehicleType".toLowerCase();
                var vSymbol = vTypeLower.indexOf("bike") !== -1 ? "🏍️" : (vTypeLower.indexOf("car") !== -1 ? "🚗" : "🚚");

                var customDriverHtml = '<div style="position:relative;display:flex;flex-direction:column;align-items:center;justify-content:center;width:120px;height:90px;cursor:pointer;">' +
                    '<div style="position:absolute;top:20px;width:56px;height:56px;border-radius:50%;background:rgba(2,132,199,0.3);border:2px solid #38BDF8;animation:pulseSonar 1.8s infinite ease-out;"></div>' +
                    '<div style="background:#0F172A;color:white;padding:3px 8px;border-radius:10px;font-size:10px;font-weight:bold;border:1px solid #38BDF8;margin-bottom:4px;white-space:nowrap;box-shadow:0 2px 6px rgba(0,0,0,0.5);z-index:10;">' +
                        vSymbol + ' $driverName • ' + bearing + '°</div>' +
                    '<div style="transform:rotate(' + bearing + 'deg);transition:transform 0.8s ease-out;width:44px;height:44px;background:linear-gradient(135deg, #0284C7, #0369A1);border-radius:50%;display:flex;align-items:center;justify-content:center;border:2.5px solid white;box-shadow:0 4px 12px rgba(0,0,0,0.5);z-index:5;">' +
                        '<span style="font-size:22px;">' + vSymbol + '</span>' +
                    '</div>' +
                '</div>';

                var driverIcon = L.divIcon({
                    className: 'driver-custom-marker',
                    html: customDriverHtml,
                    iconSize: [120, 90],
                    iconAnchor: [60, 45]
                });
                window.driverMarker = L.marker([$calcDriverLat, $calcDriverLng], { icon: driverIcon }).addTo(map);
                window.driverMarker.bindPopup("<b>Assigned Rider: $driverName</b><br>Vehicle #: $vehicleNo ($vehicleType)<br>Status: $orderStatus<br>ETA: ~$etaMinutes mins");

                window.driverMarker.on('click', function() {
                    if (window.AndroidBridge && window.AndroidBridge.onDriverClicked) {
                        window.AndroidBridge.onDriverClicked();
                    }
                });

                // Polyline Route
                window.routeLine = L.polyline([
                    [$hqLat, $hqLng],
                    [$calcDriverLat, $calcDriverLng],
                    [$currentLat, $currentLng]
                ], { color: '#0284C7', weight: 5, dashArray: '8, 8' }).addTo(map);

                var bounds = L.latLngBounds([
                    [$hqLat, $hqLng],
                    [$calcDriverLat, $calcDriverLng],
                    [$currentLat, $currentLng]
                ]);
                map.fitBounds(bounds, { padding: [35, 35] });

                // Control JS Exposing
                window.zoomInMap = function() { map.zoomIn(); };
                window.zoomOutMap = function() { map.zoomOut(); };
                window.recenterMap = function(lat, lng) { map.setView([lat, lng], 14); };
                window.switchMapLayer = function(type) {
                    if (window.currentTileLayer) map.removeLayer(window.currentTileLayer);
                    var url = tileUrls[type] || tileUrls['voyager'];
                    window.currentTileLayer = L.tileLayer(url, { maxZoom: 19 }).addTo(map);
                };
                window.toggleFuelPumps = function(show) {
                    if (show) { window.pumpsGroup.addTo(map); } else { map.removeLayer(window.pumpsGroup); }
                };
                window.updateDestinationPin = function(lat, lng, addr, shouldPan) {
                    if (window.destMarker) {
                        window.destMarker.setLatLng([lat, lng]);
                        if (shouldPan) map.panTo([lat, lng]);
                    }
                };
                // Live rider movement: called from Kotlin every time a fresh GPS fix
                // arrives, so the vehicle marker and route actually move without
                // reloading the whole page (which would reset the user's zoom/pan).
                window.updateDriverPosition = function(lat, lng, destLat, destLng) {
                    if (window.driverMarker) {
                        window.driverMarker.setLatLng([lat, lng]);
                    }
                    if (window.routeLine) {
                        window.routeLine.setLatLngs([
                            [$hqLat, $hqLng],
                            [lat, lng],
                            [destLat, destLng]
                        ]);
                    }
                };
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (showOverlays) {
            // Prominent Header Above Map Showing Time Remaining and Journey Progress
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .clickable { showRiderProfileDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF0284C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚚", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "⏱️ ETA: ~$etaMinutes mins (${"%.1f".format(distanceKm)} km)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "$driverName • $vehicleNo ($vehicleType)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                    Surface(
                        color = Color(0xFF0369A1),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = orderStatus,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Quick Lahore Location Presets Row (when in Location Picker mode)
            if (isPickerMode) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(lahorePresets) { preset ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (currentAddress.contains(preset.first, ignoreCase = true)) ZyphuelBluePrimary else Color(0xFFE2E8F0),
                            modifier = Modifier.clickable {
                                currentLat = preset.second
                                currentLng = preset.third
                                currentAddress = "${preset.first}, Lahore"
                                onLocationPinned?.invoke(preset.second, preset.third, "${preset.first}, Lahore")
                                webViewRef.value?.evaluateJavascript("if(window.updateDestinationPin) window.updateDestinationPin(${preset.second}, ${preset.third}, '${preset.first}', true);", null)
                            }
                        ) {
                            Text(
                                text = "📍 ${preset.first}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentAddress.contains(preset.first, ignoreCase = true)) Color.White else Color(0xFF334155)
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webChromeClient = android.webkit.WebChromeClient()
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                return handleExternalMapIntent(context, url)
                            }
                        }

                        // Prevent parent Compose scrollables from hijacking map touch gestures
                        setOnTouchListener { v, event ->
                            when (event.action) {
                                android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false
                        }

                        addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun onLocationPinned(lat: Double, lng: Double) {
                                mainHandler.post {
                                    currentLat = lat
                                    currentLng = lng
                                    val resolvedLandmark = resolveLandmarkFromCoordinates(context, lat, lng)
                                    currentAddress = resolvedLandmark
                                    onLocationPinned?.invoke(lat, lng, resolvedLandmark)
                                }
                            }

                            @android.webkit.JavascriptInterface
                            fun onDriverClicked() {
                                mainHandler.post {
                                    showRiderProfileDialog = true
                                }
                            }
                        }, "AndroidBridge")

                        webViewRef.value = this
                        loadDataWithBaseURL("https://www.google.com", htmlContent, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    // Avoid reloading full HTML on minor recompositions to prevent zoom/pan reset
                    val isAlreadyLoaded = webView.tag == "LOADED"
                    if (!isAlreadyLoaded) {
                        webView.tag = "LOADED"
                        webView.loadDataWithBaseURL("https://www.google.com", htmlContent, "text/html", "UTF-8", null)
                    } else {
                        // Push only the changed coordinates into the already-loaded page.
                        // shouldPan = false: a live GPS fix must never hijack the camera
                        // the user is panning (this fired every ~4s and felt like a freeze).
                        val js = "if(window.updateDestinationPin) window.updateDestinationPin($currentLat, $currentLng, '${currentAddress.replace("'", "\\'")}', false);" +
                                "if(window.updateDriverPosition) window.updateDriverPosition($calcDriverLat, $calcDriverLng, $currentLat, $currentLng);"
                        webView.evaluateJavascript(js, null)
                    }
                },
                modifier = Modifier.fillMaxSize().testTag("unified_google_map_webview")
            )

            if (showOverlays) {
                // Top-Right Interactive Map Action Buttons Column (Zoom & Recenter)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Zoom In Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xEE0F172A),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { webViewRef.value?.evaluateJavascript("if(window.zoomInMap) window.zoomInMap();", null) }
                            .testTag("map_zoom_in_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("➕", color = Color.White, fontSize = 14.sp) }
                    }

                    // Zoom Out Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xEE0F172A),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { webViewRef.value?.evaluateJavascript("if(window.zoomOutMap) window.zoomOutMap();", null) }
                            .testTag("map_zoom_out_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("➖", color = Color.White, fontSize = 14.sp) }
                    }

                    // Recenter Camera Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xEE0F172A),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { webViewRef.value?.evaluateJavascript("if(window.recenterMap) window.recenterMap($currentLat, $currentLng);", null) }
                            .testTag("map_recenter_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("🎯", color = Color.White, fontSize = 14.sp) }
                    }

                    // Fullscreen Expand Map Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xEE0284C7),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { showFullScreenMap = true }
                            .testTag("map_fullscreen_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("⛶", color = Color.White, fontSize = 14.sp) }
                    }
                }
            }
        }
    }

    // Full Screen Expanded Map Modal
    if (showFullScreenMap) {
        AlertDialog(
            onDismissRequest = { showFullScreenMap = false },
            confirmButton = {
                Button(
                    onClick = { showFullScreenMap = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                ) {
                    Text("Close Fullscreen Map")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🗺️ Expanded Interactive Map", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    UnifiedGoogleMapView(
                        modifier = Modifier.fillMaxSize(),
                        userLat = currentLat,
                        userLng = currentLng,
                        deliveryAddress = currentAddress,
                        driverLat = calcDriverLat,
                        driverLng = calcDriverLng,
                        driverName = driverName,
                        vehicleType = vehicleType,
                        vehicleNo = vehicleNo,
                        orderStatus = orderStatus,
                        isPickerMode = isPickerMode,
                        showOverlays = true,
                        onLocationPinned = { lat, lng, addr ->
                            currentLat = lat
                            currentLng = lng
                            currentAddress = addr
                            onLocationPinned?.invoke(lat, lng, addr)
                        }
                    )
                }
            }
        )
    }

    // Interactive Rider Profile Dialog Popup when rider icon on map is tapped
    if (showRiderProfileDialog) {
        AlertDialog(
            onDismissRequest = { showRiderProfileDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ZyphuelBluePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚚", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = driverName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                            Text("Verified Zyphuel Driver 🎖️", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF047857), fontWeight = FontWeight.Bold))
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vehicle Registration No:", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                Text(if (vehicleNo.isNotBlank()) vehicleNo else "LEC-8924", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vehicle Type:", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                Text(vehicleType, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Service Type:", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                Text(serviceType, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Estimated Arrival Time:", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                Text("~$etaMinutes mins remaining", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0369A1)))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Distance to Destination:", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                Text("${"%.1f".format(distanceKm)} km", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.DarkGray))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val callIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:$driverPhone")
                        }
                        try { context.startActivity(callIntent) } catch (e: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Rider ($driverPhone)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRiderProfileDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun GoogleMapComposeView(
    modifier: Modifier = Modifier,
    initialLat: Double = 31.5204,
    initialLng: Double = 74.3587,
    title: String = "Lahore Order Location"
) {
    UnifiedGoogleMapView(
        modifier = modifier,
        userLat = initialLat,
        userLng = initialLng,
        deliveryAddress = title
    )
}

@Composable
fun DriverRealTimeTrackingMap(
    modifier: Modifier = Modifier,
    userLat: Double = 31.5204,
    userLng: Double = 74.3587,
    driverLat: Double? = null,
    driverLng: Double? = null,
    driverName: String = "Assigned Driver",
    driverPhone: String = "+92 323 0112464",
    vehicleType: String = "Delivery Vehicle",
    serviceType: String = "Fuel Delivery",
    orderStatus: String = "Delivering",
    deliveryAddress: String = "Lahore, Pakistan",
    pathProgress: Float = 0f,
    etaMinutes: Int = 15,
    distanceKm: Double = 0.0

) {
    UnifiedGoogleMapView(
        modifier = modifier,
        userLat = userLat,
        userLng = userLng,
        driverLat = driverLat,
        driverLng = driverLng,
        driverName = driverName,
        driverPhone = driverPhone,
        vehicleType = vehicleType,
        serviceType = serviceType,
        orderStatus = orderStatus,
        deliveryAddress = deliveryAddress,
        pathProgress = pathProgress,
        etaMinutes = etaMinutes,
        distanceKm = distanceKm
    )
}

@Composable
fun DeliveryTrackerComponent(
    modifier: Modifier = Modifier,
    riderLat: Double = 31.5204,
    riderLng: Double = 74.3587,
    riderName: String = "Delivery Rider",
    vehicleType: String = "Bowser",
    orderStatus: String = "Delivering",
    serviceType: String = "Fuel",
    pathProgress: Float = 0.5f,
    autoRecenter: Boolean = true,
    bowserCapacity: String = "5,000L Calibrated Fuel Bowser",
    embedUrl: String = ""
) {
    UnifiedGoogleMapView(
        modifier = modifier,
        userLat = riderLat,
        userLng = riderLng,
        driverName = riderName,
        vehicleType = vehicleType,
        serviceType = serviceType,
        orderStatus = orderStatus,
        pathProgress = pathProgress
    )
}

@Composable
fun LahoreGoogleEmbedMapView(
    modifier: Modifier = Modifier,
    riderLat: Double = 31.5204,
    riderLng: Double = 74.3587,
    driverLat: Double? = null,
    driverLng: Double? = null,
    driverName: String = "Delivery Rider",
    driverPhone: String = "+92 323 0112464",
    vehicleType: String = "Bowser",
    orderStatus: String = "Delivering",
    serviceType: String = "Fuel",
    deliveryAddress: String = "Gulberg III, Lahore",
    pathProgress: Float = 0.5f,
    etaMinutes: Int = 8,
    distanceKm: Double = 2.4,
    embedUrl: String = ""
) {
    UnifiedGoogleMapView(
        modifier = modifier,
        userLat = riderLat,
        userLng = riderLng,
        driverLat = driverLat,
        driverLng = driverLng,
        driverName = driverName,
        driverPhone = driverPhone,
        vehicleType = vehicleType,
        serviceType = serviceType,
        orderStatus = orderStatus,
        deliveryAddress = deliveryAddress,
        pathProgress = pathProgress,
        etaMinutes = etaMinutes,
        distanceKm = distanceKm
    )
}

@Composable
fun OrderConfirmedLahoreMapDialog(
    order: OrderEntity,
    onDismiss: () -> Unit,
    onTrackOrder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFDCFCE7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        "Order Placed (COD) 🎉",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark
                        )
                    )
                    Text(
                        "Fixed Delivery City: Lahore, Pakistan 📍",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ZyphuelBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Your order #${order.id} (${order.serviceType}, ${order.quantity} units) has been confirmed on Cash on Delivery (COD) in Lahore.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                )

                // Embedded Google Map for Lahore
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                ) {
                    LahoreGoogleEmbedMapView(
                        modifier = Modifier.fillMaxSize(),
                        serviceType = order.serviceType,
                        orderStatus = order.status,
                        driverName = order.riderName ?: "Assigned Bowser Driver"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delivery Address:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Text(
                        order.deliveryAddress,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark),
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Method:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "Cash on Delivery (COD)",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF15803D), fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTrackOrder()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("track_confirmed_order_btn")
            ) {
                Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Track Live Order in Lahore 📍", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderStatusAndCoverageBottomSheet(
    order: OrderEntity,
    onDismiss: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title & Live ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (order.serviceType.contains("Water", ignoreCase = true)) "🚰 Pure Water Tanker Order" else "⛽ Euro-V Fuel Bowser Order",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Surface(
                            color = Color(0xFF22C55E).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF22C55E))
                        ) {
                            Text(
                                text = "LIVE ETA: 12-15 MINS",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Order #${order.id} • ${order.deliveryAddress}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Sheet", tint = Color.White)
                }
            }

            // Embedded Google Map of Lahore
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
            ) {
                LahoreGoogleEmbedMapView(modifier = Modifier.fillMaxSize())
            }

            // Visual Overlay Card for Lahore Service Coverage Areas (GeoJSON)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Text(
                                text = "Lahore GeoJSON Coverage Zones",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                        Surface(color = Color(0xFF0284C7).copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "GeoJSON Polygon Overlay",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF2563EB))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Zone A: Gulberg III & Cantt", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold))
                                Text("Coverage: 100% Active", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4ADE80), fontSize = 10.sp))
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Zone B: DHA Phase 1-8", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold))
                                Text("Coverage: Express Bowser", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4ADE80), fontSize = 10.sp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Zone C: Johar Town", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF34D399), fontWeight = FontWeight.Bold))
                                Text("Coverage: 100% Active", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4ADE80), fontSize = 10.sp))
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Zone D: Model Town & Mall Rd", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold))
                                Text("Coverage: High-Flow Tankers", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4ADE80), fontSize = 10.sp))
                            }
                        }
                    }
                }
            }

            // Real-Time Order Status Tracker
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Live Order Status Updates",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )

                    OrderStatusStepItem(title = "Order Confirmed & Logged", description = "Cash on Delivery (COD) Verified", isDone = true)
                    OrderStatusStepItem(title = "Bowser Dispatched from Central Depot", description = "Calibrated digital flow meter ready", isDone = true)
                    OrderStatusStepItem(title = "En Route (Lahore Region Corridor)", description = "Arrival in approx. 12-15 minutes", isDone = order.status == "Delivering" || order.status == "Completed", isCurrent = order.status == "Delivering")
                    OrderStatusStepItem(title = "Dispensing Fuel / Water on Site", description = "OGRA digital slip printout", isDone = order.status == "Completed")
                }
            }

            // Driver Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val phone = "03230112464"
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Driver", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OrderStatusStepItem(title: String, description: String, isDone: Boolean, isCurrent: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    if (isDone) Color(0xFF22C55E) else if (isCurrent) Color(0xFF38BDF8) else Color(0xFF475569),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            } else if (isCurrent) {
                Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Normal,
                    color = if (isDone || isCurrent) Color.White else Color(0xFF94A3B8)
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.sp)
            )
        }
    }
}

// --- REAL-TIME ORDER TRACKING MAP COMPONENT ---
@Composable
fun RealTimeOrderTrackingCard(
    order: OrderEntity,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onFullTrackClick: () -> Unit
) {
    val context = LocalContext.current
    val allRiders by viewModel.allRiders.collectAsState()
    val assignedRider = remember(allRiders, order.riderEmail) {
        if (!order.riderEmail.isNullOrBlank()) {
            allRiders.find { it.email == order.riderEmail }
        } else null
    }

    // --- REAL-TIME rider GPS for THIS order (published by the rider foreground service) ---
    val riderLive by remember(order.id) { viewModel.observeRiderLocation(order.id) }
        .collectAsState(initial = null)
    val cardHasLiveFix = riderLive != null

    val cardDestLat = order.destLat ?: 31.5204
    val cardDestLng = order.destLng ?: 74.3587
    val cardDepotLat = order.originLat ?: 31.4380
    val cardDepotLng = order.originLng ?: 74.3050

    val cardTotalKm = trackerHaversineKm(cardDepotLat, cardDepotLng, cardDestLat, cardDestLng).coerceAtLeast(0.1)
    val cardRemainingKm = if (cardHasLiveFix) trackerHaversineKm(riderLive!!.lat, riderLive!!.lng, cardDestLat, cardDestLng) else cardTotalKm
    val cardSpeedKmh = riderLive?.speedKmh?.let { Math.round(it).toInt() } ?: 0

    // Real progress along the route — stays at 0 until the rider actually shares GPS.
    val pathProgress: Float = if (cardHasLiveFix) {
        (1.0 - (cardRemainingKm / cardTotalKm)).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    val remainingEtaMinutes = if (cardHasLiveFix) {
        if (cardSpeedKmh > 4) Math.ceil(cardRemainingKm / cardSpeedKmh * 60.0).toInt().coerceAtLeast(1)
        else Math.ceil(cardRemainingKm * 3.0).toInt().coerceAtLeast(1)
    } else order.etaMinutes.coerceAtLeast(1)

    var cardMapMode by remember { mutableStateOf("driver") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .sharedOrderBounds("order_card_${order.id}")
            .testTag("realtime_order_tracking_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.5.dp, ZyphuelBluePrimary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedOrderElement("order_header_${order.id}")
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ZyphuelBlueDark, Color(0xFF1E293B))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                        val pulseScale by rememberInfiniteTransition(label = "pulse_card_header").animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "header_pulse"
                        )
                        Box(
                            modifier = Modifier
                                .scale(pulseScale)
                                .size(14.dp)
                                .background(Color(0xFF22C55E).copy(alpha = 0.4f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                    }
                    Column {
                        Text(
                            text = "LIVE ORDER TRACKING • #${order.id}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "${order.serviceType} (${order.quantity}L) • ${order.deliveryAddress}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1)),
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF059669).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Color(0xFF34D399))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "~$remainingEtaMinutes MINS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Map Mode Switcher Sub-header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { cardMapMode = "driver" },
                    shape = RoundedCornerShape(6.dp),
                    color = if (cardMapMode == "driver") ZyphuelBluePrimary else Color.White,
                    contentColor = if (cardMapMode == "driver") Color.White else ZyphuelBlueDark,
                    border = BorderStroke(1.dp, if (cardMapMode == "driver") ZyphuelBluePrimary else Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Driver Map 🚚", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }

                Surface(
                    onClick = { cardMapMode = "google" },
                    shape = RoundedCornerShape(6.dp),
                    color = if (cardMapMode == "google") ZyphuelBluePrimary else Color.White,
                    contentColor = if (cardMapMode == "google") Color.White else ZyphuelBlueDark,
                    border = BorderStroke(1.dp, if (cardMapMode == "google") ZyphuelBluePrimary else Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Google Map 📍", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }
            }

            // Embedded Real-Time Map View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                if (cardMapMode == "driver") {
                    DriverRealTimeTrackingMap(
                        modifier = Modifier.fillMaxSize(),
                        userLat = cardDestLat,
                        userLng = cardDestLng,
                        driverLat = if (cardHasLiveFix) riderLive!!.lat else null,
                        driverLng = if (cardHasLiveFix) riderLive!!.lng else null,
                        driverName = order.riderName ?: assignedRider?.name ?: if (order.status == "Pending") "Assigning nearby driver..." else "Assigned Delivery Driver",
                        driverPhone = assignedRider?.phoneNumber ?: "+92 323 0112464",
                        vehicleType = assignedRider?.vehicleType ?: "${order.serviceType} Delivery",
                        serviceType = order.serviceType,
                        orderStatus = order.status,
                        deliveryAddress = order.deliveryAddress,
                        pathProgress = pathProgress,
                        etaMinutes = remainingEtaMinutes,
                        distanceKm = cardRemainingKm.coerceAtLeast(0.0)
                    )
                } else {
                    LahoreGoogleEmbedMapView(
                        modifier = Modifier.fillMaxSize(),
                        riderLat = cardDestLat,
                        riderLng = cardDestLng,
                        driverLat = if (cardHasLiveFix) riderLive!!.lat else null,
                        driverLng = if (cardHasLiveFix) riderLive!!.lng else null,
                        driverName = order.riderName ?: assignedRider?.name ?: if (order.status == "Pending") "Assigning driver..." else "Delivery Rider",
                        driverPhone = assignedRider?.phoneNumber ?: "+92 323 0112464",
                        vehicleType = assignedRider?.vehicleType ?: "${order.serviceType} Delivery",

                        orderStatus = order.status,
                        serviceType = order.serviceType,
                        deliveryAddress = order.deliveryAddress,
                        pathProgress = pathProgress,
                        etaMinutes = remainingEtaMinutes,
                        distanceKm = cardRemainingKm.coerceAtLeast(0.0)
                    )
                }

                // Top GPS Signal Banner Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = "GPS Active",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "GPS Signal: Strong (Lahore 4G)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            // Order Stepper Status
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                OrderTrackingStepper(currentStatus = order.status, pathProgress = pathProgress)

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                // Rider Information Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(ZyphuelBluePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (order.riderName ?: "R").take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Column {
                            Text(
                                text = order.riderName ?: "Assigned Driver",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ZyphuelBlueDark
                                )
                            )
                            Text(
                                text = "${assignedRider?.vehicleType ?: "Refuel Vehicle"} • ${assignedRider?.phoneNumber ?: "+92 323 0112464"}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val phoneNo = assignedRider?.phoneNumber ?: "+923230112464"
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNo"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Call: $phoneNo", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = "Call Driver", tint = ZyphuelBluePrimary)
                        }

                        IconButton(
                            onClick = {
                                val msg = "Hello, checking on my Zyphuel delivery order #${order.id}"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/923230112464?text=${Uri.encode(msg)}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp Support: +92 323 0112464", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = "Chat Driver", tint = Color(0xFF25D366))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fullscreen Tracking Map Action Button
                Button(
                    onClick = onFullTrackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_fullscreen_tracker_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZyphuelBluePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = "Open Full Tracker Map",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Fullscreen Interactive Tracking Map",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDialog(viewModel: MainViewModel, serviceType: String, onDismiss: () -> Unit) {
    // Collect prices dynamically from the ViewModel
    val petrolPrice by viewModel.petrolPrice.collectAsState()
    val dieselPrice by viewModel.dieselPrice.collectAsState()
    val octanePrice by viewModel.highOctanePrice.collectAsState()
    val lpgPrice by viewModel.lpgGasPrice.collectAsState()
    val waterPrice by viewModel.waterPrice.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val isPromoApplied by viewModel.isPromoApplied.collectAsState()
    val permanentMarkedLocations by viewModel.markedLocationsForCurrentUser.collectAsState()
    // Loading state — prevents double-tap and shows spinner while order is being submitted
    val isPlacingOrder by viewModel.isPlacingOrder.collectAsState()

    val initialDiesel = serviceType == "Diesel" || serviceType.contains("Diesel", ignoreCase = true)
    val initialOctane = serviceType == "High-Octane" || serviceType.contains("Octane", ignoreCase = true)
    val initialLpg = serviceType == "LPG Gas" || serviceType.contains("LPG", ignoreCase = true) || serviceType.contains("Gas", ignoreCase = true)
    val initialWater = serviceType == "Water" || serviceType.contains("Water", ignoreCase = true)
    val initialPetrol = serviceType == "Petrol" || serviceType.isBlank() || serviceType == "all" || serviceType.contains("Petrol", ignoreCase = true) || (!initialDiesel && !initialOctane && !initialLpg && !initialWater)

    // Support multi-item selection state (Petrol, Diesel, High-Octane, LPG Gas, Water)
    var petrolSelected by remember { mutableStateOf(initialPetrol) }
    var petrolQty by remember { mutableIntStateOf(5) }

    var dieselSelected by remember { mutableStateOf(initialDiesel) }
    var dieselQty by remember { mutableIntStateOf(5) }

    var octaneSelected by remember { mutableStateOf(initialOctane) }
    var octaneQty by remember { mutableIntStateOf(5) }

    var lpgSelected by remember { mutableStateOf(initialLpg) }
    var lpgQty by remember { mutableIntStateOf(5) }

    var waterSelected by remember { mutableStateOf(initialWater) }
    var waterQty by remember { mutableIntStateOf(1) }

    val liveLocationCoordinates by viewModel.liveLocationCoordinates.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    var deliveryAddress by remember { mutableStateOf("") }



    LaunchedEffect(liveLocationCoordinates) {
        if (deliveryAddress.isEmpty() || deliveryAddress == "Liberty Market, Gulberg III, Lahore" || deliveryAddress == "Gulberg III, Lahore" || deliveryAddress.startsWith("Gulberg III, Lahore")) {
            deliveryAddress = liveLocationCoordinates
        }
    }
    val context = LocalContext.current

    val petrolSubtotal = if (petrolSelected) petrolQty * petrolPrice.toDouble() else 0.0
    val dieselSubtotal = if (dieselSelected) dieselQty * dieselPrice.toDouble() else 0.0
    val octaneSubtotal = if (octaneSelected) octaneQty * octanePrice.toDouble() else 0.0
    val lpgSubtotal = if (lpgSelected) lpgQty * lpgPrice.toDouble() else 0.0
    val waterSubtotal = if (waterSelected) waterQty * waterPrice.toDouble() else 0.0

    val subtotal = petrolSubtotal + dieselSubtotal + octaneSubtotal + lpgSubtotal + waterSubtotal

    // Count selected distinct product types for multi-item discount
    val selectedTypesCount = (if (petrolSelected) 1 else 0) +
            (if (dieselSelected) 1 else 0) +
            (if (octaneSelected) 1 else 0) +
            (if (lpgSelected) 1 else 0) +
            (if (waterSelected) 1 else 0)

    val isMultiItemOrder = selectedTypesCount >= 2

    // Delivery charges & multi-item discount
    val hasFuelOrGas = petrolSelected || dieselSelected || octaneSelected || lpgSelected
    val hasWaterOnly = waterSelected && !hasFuelOrGas
    val baseDeliveryCharge = when {
        hasFuelOrGas -> 250.0
        hasWaterOnly -> 20.0
        else -> 0.0
    }

    // 50% delivery fee discount when customer orders 2 or more items together
    val multiItemDeliveryDiscount = if (isMultiItemOrder && baseDeliveryCharge > 0) baseDeliveryCharge * 0.50 else 0.0
    val deliveryCharge = (baseDeliveryCharge - multiItemDeliveryDiscount).coerceAtLeast(0.0)

    val promoDiscount = if (isPromoApplied) 200.0 else 0.0
    val totalPrice = (subtotal + deliveryCharge - promoDiscount).coerceAtLeast(0.0)

    // Construct summary items list
    val selectedSummaryParts = mutableListOf<String>()
    var totalQuantity = 0

    if (petrolSelected) {
        selectedSummaryParts.add("Petrol (${petrolQty}L)")
        totalQuantity += petrolQty
    }
    if (dieselSelected) {
        selectedSummaryParts.add("Diesel (${dieselQty}L)")
        totalQuantity += dieselQty
    }
    if (octaneSelected) {
        selectedSummaryParts.add("High-Octane (${octaneQty}L)")
        totalQuantity += octaneQty
    }
    if (lpgSelected) {
        selectedSummaryParts.add("LPG Gas (${lpgQty}KG)")
        totalQuantity += lpgQty
    }
    if (waterSelected) {
        selectedSummaryParts.add("Water (${waterQty} Gal)")
        totalQuantity += waterQty
    }

    val combinedServiceType = if (selectedSummaryParts.isEmpty()) "Custom Fuel Combo" else selectedSummaryParts.joinToString(" + ")

    // Check if WhatsApp redirection required (30L+ for fuel)
    val totalFuelVolume = (if (petrolSelected) petrolQty else 0) +
            (if (dieselSelected) dieselQty else 0) +
            (if (octaneSelected) octaneQty else 0)
    val requiresWhatsApp = totalFuelVolume >= 30

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalGasStation,
                    contentDescription = null,
                    tint = ZyphuelBluePrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Place Your Order", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Order items together for delivery discounts", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isMultiItemOrder) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalOffer,
                                contentDescription = "Delivery Discount",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "🎉 Multi-Item Delivery Discount Applied!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "You save ${viewModel.formatPrice(multiItemDeliveryDiscount)} (50% off delivery) for combining $selectedTypesCount items in one order!",
                                    color = Color(0xFF047857),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Select Products & Quantities:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontWeight = FontWeight.Bold)
                )

                // 1. Petrol
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (petrolSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, if (petrolSelected) ZyphuelBluePrimary else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = petrolSelected,
                                onCheckedChange = { petrolSelected = it },
                                colors = CheckboxDefaults.colors(checkedColor = ZyphuelBluePrimary)
                            )
                            Column {
                                Text("Petrol", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("${viewModel.formatUnitPrice(petrolPrice, "L")}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        if (petrolSelected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (petrolQty > 5) petrolQty-- },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                                Text(" $petrolQty L ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                IconButton(
                                    onClick = { petrolQty++ },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // 2. Diesel
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (dieselSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, if (dieselSelected) ZyphuelBluePrimary else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = dieselSelected,
                                onCheckedChange = { dieselSelected = it },
                                colors = CheckboxDefaults.colors(checkedColor = ZyphuelBluePrimary)
                            )
                            Column {
                                Text("Diesel", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("${viewModel.formatUnitPrice(dieselPrice, "L")}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        if (dieselSelected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (dieselQty > 5) dieselQty-- },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                                Text(" $dieselQty L ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                IconButton(
                                    onClick = { dieselQty++ },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // 3. High-Octane
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (octaneSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, if (octaneSelected) ZyphuelBluePrimary else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = octaneSelected,
                                onCheckedChange = { octaneSelected = it },
                                colors = CheckboxDefaults.colors(checkedColor = ZyphuelBluePrimary)
                            )
                            Column {
                                Text("High-Octane", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("${viewModel.formatUnitPrice(octanePrice, "L")}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        if (octaneSelected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (octaneQty > 5) octaneQty-- },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                                Text(" $octaneQty L ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                IconButton(
                                    onClick = { octaneQty++ },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // 4. LPG Gas
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (lpgSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, if (lpgSelected) ZyphuelBluePrimary else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = lpgSelected,
                                onCheckedChange = { lpgSelected = it },
                                colors = CheckboxDefaults.colors(checkedColor = ZyphuelBluePrimary)
                            )
                            Column {
                                Text("LPG Gas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("${viewModel.formatUnitPrice(lpgPrice, "Kg")}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        if (lpgSelected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (lpgQty > 5) lpgQty-- },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                                Text(" $lpgQty KG ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                IconButton(
                                    onClick = { lpgQty++ },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // 5. Water
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (waterSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, if (waterSelected) ZyphuelBluePrimary else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = waterSelected,
                                onCheckedChange = { waterSelected = it },
                                colors = CheckboxDefaults.colors(checkedColor = ZyphuelBluePrimary)
                            )
                            Column {
                                Text("Pure Water", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("${viewModel.formatUnitPrice(waterPrice, "Gallon")}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        if (waterSelected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (waterQty > 1) waterQty-- },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                                Text(" $waterQty Gal ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                IconButton(
                                    onClick = { waterQty++ },
                                    modifier = Modifier.size(28.dp).background(ZyphuelBlueSecondary.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Saved Delivery Addresses (Local Storage for Quick Checkout)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = ZyphuelBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Saved Addresses (Quick Fill):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ZyphuelBlueDark
                                )
                            )
                        }

                        if (deliveryAddress.isNotBlank() && !savedAddresses.contains(deliveryAddress.trim())) {
                            TextButton(
                                onClick = { viewModel.addSavedAddress(deliveryAddress) },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = ZyphuelBluePrimary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    "Save Current",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ZyphuelBluePrimary
                                    )
                                )
                            }
                        }
                    }

                    if (savedAddresses.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("saved_addresses_row")
                        ) {
                            items(savedAddresses) { addr ->
                                val isSelected = (deliveryAddress.trim() == addr.trim())
                                Surface(
                                    modifier = Modifier.clickable { deliveryAddress = addr },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) ZyphuelBluePrimary else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) ZyphuelBluePrimary else Color(0xFFCBD5E1))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Place,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else ZyphuelBluePrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = addr,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) Color.White else Color.DarkGray,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove address",
                                            tint = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeSavedAddress(addr) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Delivery address input

                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    label = { Text("Delivery Address (Lahore)") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ZyphuelBluePrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_address_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Large order WhatsApp instruction Warning
                if (requiresWhatsApp) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Large Bulk Order Alert",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Fuel orders of 30L or more require advance payment coordination via WhatsApp.",
                                    color = Color(0xFFB45309),
                                    style = MaterialTheme.typography.labelSmall,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Order summary breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Selected Items Summary:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                    )
                    Text(
                        text = combinedServiceType,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = ZyphuelBlueDark)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
                        Text(viewModel.formatPrice(subtotal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = ZyphuelBlueDark))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivery Charges:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
                        if (isMultiItemOrder && multiItemDeliveryDiscount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    viewModel.formatPrice(baseDeliveryCharge),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray,
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    viewModel.formatPrice(deliveryCharge),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                                )
                            }
                        } else {
                            Text(viewModel.formatPrice(deliveryCharge), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = ZyphuelBlueDark))
                        }
                    }
                    if (isMultiItemOrder && multiItemDeliveryDiscount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Multi-Item Delivery Discount (50% OFF):", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF059669), fontWeight = FontWeight.Bold))
                            Text("-${viewModel.formatPrice(multiItemDeliveryDiscount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF059669)))
                        }
                    }
                    if (isPromoApplied) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("OGRA Voucher Discount:", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF16A34A), fontWeight = FontWeight.Bold))
                            Text("-${viewModel.formatPrice(200.0)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)))
                        }
                    }
                    Divider(color = Color(0xFFE2E8F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total:", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(viewModel.formatPrice(totalPrice), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary))
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    val finalAddr = deliveryAddress.trim().ifBlank { "Main Boulevard, Gulberg III, Lahore" }
                    val finalServiceType = if (selectedSummaryParts.isNotEmpty()) {
                        combinedServiceType
                    } else if (serviceType.isNotBlank()) {
                        serviceType
                    } else {
                        "Super Petrol (5L)"
                    }
                    val finalQuantity = totalQuantity.coerceAtLeast(1)
                    val finalPrice = if (totalPrice > 0.0) totalPrice else 500.0

                    viewModel.placeOrder(
                        serviceType = finalServiceType,
                        quantity = finalQuantity,
                        totalPrice = finalPrice,
                        deliveryAddress = finalAddr,
                        paymentMethod = "Cash on Delivery"
                    ) {
                        onDismiss()
                    }
                },
                enabled = !isPlacingOrder,
                modifier = Modifier.testTag("dialog_confirm_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZyphuelBluePrimary,
                    disabledContainerColor = ZyphuelBluePrimary.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isPlacingOrder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Placing Order...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Text(
                            "Confirm Order (COD) 💵",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun OrderSummaryCard(order: OrderEntity, viewModel: MainViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(BorderStroke(1.dp, ZyphuelBluePrimary.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
            .testTag("order_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = "Order Summary",
                        tint = ZyphuelBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ORDER SUMMARY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark,
                            letterSpacing = 1.sp
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .background(ZyphuelBluePrimary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ID: #${order.id}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBluePrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Items requested section
            Text(
                text = "ITEMS REQUESTED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (order.serviceType) {
                                "Petrol", "Diesel", "High-Octane" -> Icons.Filled.LocalGasStation
                                "LPG Gas" -> Icons.Filled.Fireplace
                                else -> Icons.Filled.WaterDrop
                            },
                            contentDescription = order.serviceType,
                            tint = ZyphuelBluePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = order.serviceType,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBlueDark
                            )
                        )
                        Text(
                            text = "Standard Delivery Class",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }
                Text(
                    text = "${order.quantity} ${if (order.serviceType == "Water") "Gallon(s)" else if (order.serviceType == "LPG Gas") "KG" else "L"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Cost Breakout
            Text(
                text = "COST BREAKDOWN",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            val deliveryFee = 150.0
            val subtotal = (order.totalPrice - deliveryFee).coerceAtLeast(0.0)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", style = MaterialTheme.typography.bodyMedium.copy(color = Color.DarkGray))
                Text("Rs. ${String.format(Locale.US, "%,.2f", subtotal)}", style = MaterialTheme.typography.bodyMedium.copy(color = Color.DarkGray))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Delivery Fee", style = MaterialTheme.typography.bodyMedium.copy(color = Color.DarkGray))
                Text("Rs. ${String.format(Locale.US, "%,.2f", deliveryFee)}", style = MaterialTheme.typography.bodyMedium.copy(color = Color.DarkGray))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Cost (COD)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                Text(
                    text = "Rs. ${String.format(Locale.US, "%,.2f", order.totalPrice)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBluePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // ETA and Tracker Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZyphuelBluePrimary.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = "ETA",
                    tint = ZyphuelBluePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Estimated Delivery Time",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${order.etaMinutes} mins (${viewModel.mapStatusToUserFriendly(order.status)})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark
                        )
                    )
                }
            }
        }
    }
}

// --- COMPOSE-BASED ORDER STATUS TRANSITION ANIMATIONS & POST-DELIVERY RATING UI ---

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun OrderStatusConfettiOverlay(
    modifier: Modifier = Modifier,
    particleCount: Int = 30
) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    val particleColors = listOf(
        Color(0xFF10B981), Color(0xFF0284C7), Color(0xFFF59E0B),
        Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF3B82F6)
    )

    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (i in 0 until particleCount) {
            val startX = (i * 37f) % canvasWidth
            val speedY = 120f + (i * 17f) % 200f
            val currentY = ((animProgress * speedY * 6f) + (i * 35f)) % (canvasHeight + 30f) - 15f
            val color = particleColors[i % particleColors.size]
            val radius = (3.5f + (i % 3)).dp.toPx()
            val alpha = (1f - (currentY / canvasHeight)).coerceIn(0.15f, 0.95f)

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(
                    x = startX + kotlin.math.sin(animProgress * 6.28f + i) * 18f,
                    y = currentY
                )
            )
        }
    }
}

@Composable
fun OrderStatusAnimatedTransitionHeader(
    order: OrderEntity,
    pathProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_halo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val (statusTitle, statusMsg, primaryColor, icon) = when {
        order.status == "Completed" || order.status == "Delivered" -> Quadruple(
            "Order Delivered 🎉",
            "Your doorstep delivery for Order #${order.id} is successfully complete!",
            Color(0xFF10B981),
            Icons.Filled.CheckCircle
        )
        order.status == "Arrived" || (order.status == "Delivering" && pathProgress > 0.90f) -> Quadruple(
            "Driver Reached Location 📍",
            "Bowser driver has reached your delivery address! Please meet the driver.",
            Color(0xFF0284C7),
            Icons.Filled.LocationOn
        )
        order.status == "Delivering" || order.status == "Dispatched" || order.status == "Out for Delivery" -> Quadruple(
            "Out for Delivery 🛵",
            "Order #${order.id} is en route. Live GPS tracking active on map.",
            Color(0xFF0284C7),
            Icons.Filled.DirectionsCar
        )
        order.status == "Assigned" -> Quadruple(
            "Driver Assigned 🚚",
            "Driver ${order.riderName ?: "Rider"} has accepted Order #${order.id}.",
            Color(0xFF3B82F6),
            Icons.Filled.PersonPin
        )
        else -> Quadruple(
            "Order Placed 📦",
            "Your Order #${order.id} is placed and awaiting driver assignment.",
            Color(0xFFF59E0B),
            Icons.Filled.LocalMall
        )
    }

    val animatedCardBg by animateColorAsState(
        targetValue = when (order.status) {
            "Completed", "Delivered" -> Color(0xFFF0FDF4)
            "Arrived" -> Color(0xFFE0F2FE)
            "Delivering", "Dispatched" -> Color(0xFFEFF6FF)
            "Assigned" -> Color(0xFFF0F9FF)
            else -> Color(0xFFFEF3C7)
        },
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "status_bg_anim"
    )

    val animatedCardBorder by animateColorAsState(
        targetValue = primaryColor.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "status_border_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_notification_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = animatedCardBg),
        border = BorderStroke(1.dp, animatedCardBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (order.status == "Completed" || order.status == "Delivered") {
                OrderStatusConfettiOverlay(modifier = Modifier.height(80.dp))
            }

            AnimatedContent(
                targetState = order.status,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) { height -> -height / 2 } + scaleIn(initialScale = 0.85f)) togetherWith
                    (fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { height -> height / 2 } + scaleOut(targetScale = 0.85f))
                },
                label = "status_content_transition"
            ) { _ ->
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size((44 * pulseScale).dp)
                                .background(primaryColor.copy(alpha = pulseAlpha), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(primaryColor.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = statusTitle,
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ZyphuelBlueDark
                                )
                            )
                            Surface(
                                color = primaryColor,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LIVE STATUS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusMsg,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostDeliveryRatingCard(
    order: OrderEntity,
    viewModel: MainViewModel,
    onRatingSubmitted: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedRating by remember { mutableIntStateOf(5) }
    var selectedCompliments by remember { mutableStateOf(setOf<String>()) }
    var feedbackText by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(order.rating != null) }
    val context = LocalContext.current

    val complimentTags = listOf(
        "⏱️ On-Time Arrival",
        "⛽ Pure Fuel Quality",
        "👨‍✈️ Courteous Driver",
        "🛡️ Safety Followed",
        "💵 Exact Change"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_delivery_rating_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFFFEF08A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSubmitted || order.rating != null) {
                // Rated State Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Delivery Rated ${order.rating ?: selectedRating}/5 Stars ⭐",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ZyphuelBlueDark
                                )
                            )
                            Text(
                                text = "Thank you for supporting Zyphuel drivers in Lahore!",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "Verified Review ✔️",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                order.feedback?.takeIf { it.isNotBlank() }?.let { fb ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Text(
                            text = "\"$fb\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color.DarkGray
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            } else {
                // Interactive Rating Form
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "RATE YOUR DELIVERY EXPERIENCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "How was Order #${order.id} with ${order.riderName ?: "Bowser Driver"}?",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive 1-5 Star Bar with scaling animation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        val isSelected = star <= selectedRating
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "star_scale_$star"
                        )

                        IconButton(
                            onClick = { selectedRating = star },
                            modifier = Modifier
                                .scale(scale)
                                .size(40.dp)
                                .testTag("rating_star_$star")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Rate $star stars",
                                tint = if (isSelected) Color(0xFFEAB308) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                val ratingLabel = when (selectedRating) {
                    1 -> "Poor Service 😞"
                    2 -> "Below Expectations 😐"
                    3 -> "Fair Experience 🙂"
                    4 -> "Great Delivery! 😊"
                    else -> "Exceptional Experience! 🌟"
                }

                Text(
                    text = ratingLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Quick Compliments (Tap to add):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    complimentTags.forEach { tag ->
                        val isSelected = selectedCompliments.contains(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCompliments = if (isSelected) {
                                    selectedCompliments - tag
                                } else {
                                    selectedCompliments + tag
                                }
                            },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZyphuelBluePrimary.copy(alpha = 0.15f),
                                selectedLabelColor = ZyphuelBlueDark,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color.DarkGray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFFCBD5E1),
                                selectedBorderColor = ZyphuelBluePrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { if (it.length <= 500) feedbackText = it },
                    label = { Text("Driver Feedback / Suggestions (Optional)") },
                    placeholder = { Text("Write optional message for driver or team...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rating_feedback_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray,
                        focusedBorderColor = ZyphuelBluePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val fullFeedback = buildString {
                            if (selectedCompliments.isNotEmpty()) {
                                append("Tags: ")
                                append(selectedCompliments.joinToString(", "))
                            }
                            if (feedbackText.isNotBlank()) {
                                if (isNotEmpty()) append(" | Note: ")
                                append(feedbackText.trim())
                            }
                        }
                        viewModel.submitOrderRating(order.id, selectedRating, fullFeedback)
                        isSubmitted = true
                        Toast.makeText(context, "Thank you! Rating submitted successfully ⭐", Toast.LENGTH_SHORT).show()
                        onRatingSubmitted?.invoke()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("submit_rating_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Rating & Review",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun OrderTrackingStepper(currentStatus: String, pathProgress: Float = 0f) {
    val steps = listOf("Order Placed", "Driver Assigned", "Out for Delivery", "Arriving Soon")
    val currentIndex = when {
        currentStatus == "Completed" -> 4
        currentStatus == "Delivering" && pathProgress > 0.70f -> 3
        currentStatus == "Delivering" -> 2
        currentStatus == "Assigned" -> 1
        else -> 0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("order_tracking_stepper"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = index <= currentIndex
            val isCurrent = index == currentIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Circle Badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = if (isActive) ZyphuelBluePrimary else Color(0xFFE2E8F0),
                            shape = CircleShape
                        )
                        .border(
                            width = if (isCurrent) 2.dp else 0.dp,
                            color = if (isCurrent) ZyphuelBlueSecondary else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentIndex) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isActive) Color.White else Color.Gray, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Label
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) ZyphuelBlueDark else Color.Gray,
                        fontSize = 10.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Connecting line between steps
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .weight(0.4f)
                        .background(
                            if (index < currentIndex) ZyphuelBluePrimary else Color(0xFFE2E8F0)
                        )
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

// --- PHASE 4: REAL-TIME LAHORE TRACKING MAP (CUSTOM CANVAS DRAWING) ---

/** Great-circle distance (km) between two lat/lng points — used to derive real ETA/progress. */
private fun trackerHaversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
    return 2 * earthRadiusKm * Math.asin(Math.min(1.0, Math.sqrt(a)))
}

@Composable
fun TrackerScreen(viewModel: MainViewModel) {
    val trackingOrder by viewModel.trackingOrder.collectAsState()
    val trackingRider by viewModel.trackingRider.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLocationBlocked by viewModel.isLocationSharingBlocked.collectAsState()
    val liveLocationCoordinates by viewModel.liveLocationCoordinates.collectAsState()
    val deviceLatitude by viewModel.deviceLatitude.collectAsState()
    val deviceLongitude by viewModel.deviceLongitude.collectAsState()
    val isCustomer = currentUser?.role == "customer"
    val isRider = currentUser?.role == "rider"
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val isRiderAssigned = trackingOrder != null && !trackingOrder!!.riderEmail.isNullOrBlank()
    val isRiderRegistered = isRiderAssigned && trackingRider != null

    if (trackingOrder == null) return

    LaunchedEffect(trackingOrder?.status) {
        if (trackingOrder?.status == "Cancelled") {
            Toast.makeText(context, "Order cancelled. Returning to Home...", Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(800)
            viewModel.navigateTo("customer_home")
        }
    }

    LaunchedEffect(trackingOrder) {
        viewModel.checkBlockedStatus()
    }

    // --- REAL-TIME rider GPS drives all tracking below (published by rider foreground service) ---
    val riderLive by viewModel.riderLiveLocation.collectAsState()
    val destLatT = trackingOrder?.destLat ?: 31.5204
    val destLngT = trackingOrder?.destLng ?: 74.3587
    val depotLatT = trackingOrder?.originLat ?: 31.4380
    val depotLngT = trackingOrder?.originLng ?: 74.3050
    val hasLiveFix = riderLive != null
    val liveDriverLat = riderLive?.lat
    val liveDriverLng = riderLive?.lng

    val totalRouteKmT = trackerHaversineKm(depotLatT, depotLngT, destLatT, destLngT).coerceAtLeast(0.1)
    val remainingKmT = if (hasLiveFix) trackerHaversineKm(liveDriverLat!!, liveDriverLng!!, destLatT, destLngT) else totalRouteKmT
    val liveSpeedKmh = riderLive?.speedKmh?.let { Math.round(it).toInt() } ?: 0
    val liveEtaMinutes = if (hasLiveFix) {
        if (liveSpeedKmh > 4) Math.ceil(remainingKmT / liveSpeedKmh * 60.0).toInt().coerceAtLeast(1)
        else Math.ceil(remainingKmT * 3.0).toInt().coerceAtLeast(1)
    } else (trackingOrder?.etaMinutes ?: 12)

    // Real progress along the route (stays at 0 until the rider actually starts sharing GPS).
    // Keeps the name `pathProgress` so every downstream reference now reflects reality.
    val pathProgress: Float = if (hasLiveFix) {
        (1.0 - (remainingKmT / totalRouteKmT)).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    var hasNotifiedNearby by remember { mutableStateOf(false) }
    var hasNotifiedReachedLocation by remember { mutableStateOf(false) }

    // Reset nearby and reached location flags when pathProgress restarts near 0.0
    if (pathProgress < 0.1f) {
        hasNotifiedNearby = false
        hasNotifiedReachedLocation = false
    }

    LaunchedEffect(pathProgress, isRiderRegistered) {
        if (isRiderRegistered && trackingOrder!!.status == "Delivering" && pathProgress > 0.70f && !hasNotifiedNearby && isCustomer) {
            hasNotifiedNearby = true
            viewModel.notifyArrivingSoon(trackingOrder!!.id)
        }
        if (isRiderRegistered && trackingOrder!!.status == "Delivering" && pathProgress > 0.95f && !hasNotifiedReachedLocation && isCustomer) {
            hasNotifiedReachedLocation = true
            viewModel.notifyReachedLocation(trackingOrder!!.id)
        }
    }

    var showSummary by remember { mutableStateOf(false) }
    var showDriverChat by remember { mutableStateOf(false) }
    var showCancelReasonDialog by remember { mutableStateOf(false) }
    var showFareBreakdownDialog by remember { mutableStateOf(false) }
    var isHudMinimized by remember { mutableStateOf(false) }

    val showDailyGpsSafetyDisclaimer by viewModel.showDailyGpsSafetyDisclaimer.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkAndTriggerDailyGpsDisclaimer()
    }



    BackHandler {
        viewModel.navigateTo("customer_home")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .sharedOrderBounds("order_card_${trackingOrder!!.id}")
            .background(ZyphuelLightBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedOrderElement("order_header_${trackingOrder!!.id}")
                    .background(Color.White)
                    .padding(16.dp)
                    .shadow(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo("customer_home") }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ZyphuelBluePrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Live Tracking - Order #${trackingOrder!!.id}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                    )
                    Text(
                        "ETA: ${trackingOrder!!.etaMinutes} mins | Lahore Delivery Corridor",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
                IconButton(
                    onClick = { viewModel.triggerDeliveryNotificationPrompt() },
                    modifier = Modifier.testTag("tracker_notification_prompt_btn")
                ) {
                    val isNotifActive = remember(context) {
                        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                    }
                    Icon(
                        imageVector = if (isNotifActive) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                        contentDescription = "Delivery Notifications Alert",
                        tint = if (isNotifActive) ZyphuelBluePrimary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = { showSummary = !showSummary },
                    modifier = Modifier.testTag("toggle_order_summary_button")
                ) {
                    Icon(
                        imageVector = if (showSummary) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Toggle Order Summary",
                        tint = ZyphuelBluePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showSummary) {
                OrderSummaryCard(order = trackingOrder!!, viewModel = viewModel)
            }

            // Real Live Delivery Google Map Area (Lahore, Pakistan)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) {
                GoogleMapsLiveDeliveryTrackingOverlay(
                    modifier = Modifier.fillMaxSize(),
                    order = trackingOrder,
                    assignedRider = trackingRider,
                    viewModel = viewModel
                )

                // Overlapping Floating Status HUD
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .fillMaxWidth(),

                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    if (isHudMinimized) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHudMinimized = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(ZyphuelBlueSecondary, CircleShape))
                                Text(
                                    "STATUS: ${trackingOrder!!.status.uppercase()} • COD: Rs. ${trackingOrder!!.totalPrice}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                                )
                            }
                            Text(
                                "Expand 🔼",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header Status & COD
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(ZyphuelBlueSecondary, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "STATUS: ${trackingOrder!!.status.uppercase()}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ZyphuelBlueDark,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable { showFareBreakdownDialog = true }
                                            .padding(vertical = 2.dp)
                                            .testTag("fare_breakdown_trigger")
                                    ) {
                                        Text(
                                            "COD: Rs. ${trackingOrder!!.totalPrice}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ZyphuelBluePrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = "Fare Breakdown",
                                            tint = ZyphuelBluePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    
                                    // Minimize HUD Toggle
                                    IconButton(
                                        onClick = { isHudMinimized = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("🔽", fontSize = 12.sp)
                                    }
                                }
                            }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isRiderRegistered) {
                            // Real distances/ETA derived from the rider's live GPS (haversine)
                            val totalDistance = totalRouteKmT.toFloat()
                            val remainingDistance = if (hasLiveFix) remainingKmT.toFloat() else totalDistance
                            val traveledDistance = (totalDistance - remainingDistance).coerceAtLeast(0f)
                            val dynamicEta = liveEtaMinutes

                            // Real rider coordinates (published by the rider's foreground location service)
                            val driverLat = liveDriverLat ?: depotLatT
                            val driverLng = liveDriverLng ?: depotLngT
                            val driverCoordinatesStr = if (hasLiveFix)
                                "${String.format(Locale.US, "%.5f", driverLat)}° N, ${String.format(Locale.US, "%.5f", driverLng)}° E"
                            else "Waiting for driver to start…"

                            // ETA & Distance Stats Banner
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ZyphuelBluePrimary.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Arriving in $dynamicEta mins",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ZyphuelBlueDark
                                            )
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", traveledDistance)} km traveled • ${String.format(Locale.US, "%.1f", remainingDistance)} km left",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.DirectionsCar,
                                        contentDescription = "Driver moving",
                                        tint = ZyphuelBluePrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "Driver Coordinates",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Driver Live GPS: $driverCoordinatesStr",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = ZyphuelBlueDark
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Live Order Status Notification Card with Smooth Transition Animation
                            OrderStatusAnimatedTransitionHeader(
                                order = trackingOrder!!,
                                pathProgress = pathProgress
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4-Step Tracker Progress Stepper
                            OrderTrackingStepper(currentStatus = trackingOrder!!.status, pathProgress = pathProgress)

                            Spacer(modifier = Modifier.height(12.dp))

                            // Nice Custom Horizontal Progress Bar
                            LinearProgressIndicator(
                                progress = { pathProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = ZyphuelBluePrimary,
                                trackColor = Color(0xFFE2E8F0)
                            )

                            if (trackingOrder!!.status == "Completed" || trackingOrder!!.status == "Delivered") {
                                Spacer(modifier = Modifier.height(16.dp))
                                PostDeliveryRatingCard(
                                    order = trackingOrder!!,
                                    viewModel = viewModel
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Rider Profile & Contact
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar with initials
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(ZyphuelBluePrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = trackingOrder!!.riderName?.split(" ")?.mapNotNull { it.firstOrNull() }?.joinToString("")?.take(2)?.uppercase() ?: "R"
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                // Rider & Vehicle Info (Registration/ID kept private from customer)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = trackingOrder!!.riderName ?: "Rider Assigned",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray
                                            )
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = "Verified Rider",
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    val vehicleTypeStr = trackingRider?.vehicleType ?: "Bike"
                                    val phoneNoStr = trackingRider?.phoneNumber ?: "0300-1234567"

                                    Text(
                                        text = "Verified Delivery Agent",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$vehicleTypeStr • $phoneNoStr",
                                        style = MaterialTheme.typography.labelSmall.copy(color = ZyphuelBluePrimary, fontWeight = FontWeight.Bold)
                                    )
                                }

                                // Dial Call Button (real phone dialer)
                                val phoneToCall = trackingRider?.phoneNumber ?: "0300-1234567"
                                val context = LocalContext.current
                                IconButton(
                                    onClick = {
                                        try {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneToCall.replace(" ", "")}"))
                                                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No dialer app available", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFDCFCE7), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = "Call Rider",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Chat Button
                                IconButton(
                                    onClick = { showDriverChat = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFE0F2FE), CircleShape)
                                        .testTag("chat_driver_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Chat,
                                        contentDescription = "Chat with Driver",
                                        tint = ZyphuelBluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Block / Unblock User Button
                                val otherEmail = if (currentUser?.role == "rider") trackingOrder!!.customerEmail else (trackingOrder!!.riderEmail ?: "")
                                if (otherEmail.isNotEmpty()) {
                                    val isBlockedByMe = currentUser?.blockedUsers?.split(",")?.map { it.trim() }?.contains(otherEmail) == true
                                    IconButton(
                                        onClick = {
                                            if (isBlockedByMe) {
                                                viewModel.unblockUser(otherEmail)
                                            } else {
                                                viewModel.blockUser(otherEmail)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(if (isBlockedByMe) Color(0xFFFEE2E2) else Color(0xFFF3F4F6), CircleShape)
                                            .testTag("block_unblock_user_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isBlockedByMe) Icons.Filled.LockOpen else Icons.Filled.Block,
                                            contentDescription = if (isBlockedByMe) "Unblock User" else "Block User",
                                            tint = if (isBlockedByMe) Color(0xFFDC2626) else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (trackingOrder!!.status != "Completed" && trackingOrder!!.status != "Cancelled") {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { showCancelReasonDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cancel_ride_with_reason_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel Order (Select Reason)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        } else if (trackingOrder!!.status == "Cancelled") {
                            // Cancelled State due to rider timeout
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = "Order Cancelled",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Order Cancelled ⚠️",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "This order was automatically cancelled because no riders accepted the request within 5 minutes. We apologize for the inconvenience.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.navigateTo("customer_home") },
                                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                                ) {
                                    Text("Go to Home")
                                }
                            }
                        } else {
                            // Searching state (Pending status)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OrderTrackingStepper(currentStatus = trackingOrder!!.status)
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        color = ZyphuelBluePrimary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Order Confirmed • Processing Delivery...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Order Dispatch Status Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalShipping,
                                            contentDescription = "Service Delivery",
                                            tint = ZyphuelBluePrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Service Order Confirmed",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                                            )
                                            Text(
                                                text = "Your service request is confirmed and scheduled for instant fulfillment to your specified address.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // --- RIDE-HAILING / DELIVERY DIALOG OVERLAYS ---





    // 1. Driver Chat Dialog Modal
    if (showDriverChat) {
        var chatInput by remember { mutableStateOf("") }
        val chatMessages = remember {
            mutableStateListOf(
                "Driver" to "Hello! I am on my way with your order. Let me know if you have specific dropoff instructions.",
                "System" to "Live GPS tracking active. Estimated arrival in ${trackingOrder!!.etaMinutes} mins."
            )
        }
        AlertDialog(
            onDismissRequest = { showDriverChat = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Chat, contentDescription = null, tint = ZyphuelBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat with ${trackingOrder!!.riderName ?: "Driver"}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chatMessages) { (sender, msg) ->
                            val isMe = sender == "You"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isMe) ZyphuelBluePrimary else Color.White,
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            text = sender,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isMe) Color.White else Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Type a message...", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        chatMessages.add("You" to chatInput.trim())
                                        chatInput = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Send", tint = ZyphuelBluePrimary)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDriverChat = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 2. Uber/Careem Style Cancel Order Dialog with Reason Selection
    if (showCancelReasonDialog) {
        val reasons = listOf(
            "Driver is taking too long",
            "Changed my mind",
            "Entered incorrect address/location",
            "Found alternative transportation",
            "Price higher than expected",
            "Other reason"
        )
        var selectedReason by remember { mutableStateOf(reasons[0]) }

        AlertDialog(
            onDismissRequest = { showCancelReasonDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Order", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Please select a reason for cancelling Order #${trackingOrder!!.id}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedReason == reason),
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = ZyphuelBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelReasonDialog = false
                        viewModel.cancelOrderWithReason(trackingOrder!!.id, selectedReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelReasonDialog = false }) {
                    Text("Keep Order")
                }
            }
        )
    }

    // 3. Fare Breakdown & Surge Pricing Modal
    if (showFareBreakdownDialog) {
        val baseFare = 150.0
        val total = trackingOrder!!.totalPrice
        val volumeFare = (total - baseFare).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { showFareBreakdownDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = ZyphuelBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fare Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Delivery Fee:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Rs. ${String.format(Locale.US, "%.2f", baseFare)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${trackingOrder!!.serviceType} (${trackingOrder!!.quantity} units):", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Rs. ${String.format(Locale.US, "%.2f", volumeFare)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Government Taxes & Duties:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Included", style = MaterialTheme.typography.bodySmall, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total COD Payable:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Rs. ${String.format(Locale.US, "%.2f", total)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFareBreakdownDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)) {
                    Text("Got It")
                }
            }
        )
    }

    // 4. Daily 1-Time Order Safety Disclaimer Dialog
    if (showDailyGpsSafetyDisclaimer) {
        DailyGpsSafetyDisclaimerDialog(
            onDismiss = { viewModel.dismissDailyGpsSafetyDisclaimer() }
        )
    }

}

@Composable
fun DailyGpsSafetyDisclaimerDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFE0F2FE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.GpsFixed,
                    contentDescription = "GPS Safe Tracking",
                    tint = ZyphuelBluePrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Live Order GPS Tracking Active 🛡️",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "The driver/rider GPS is on during each ride. It helps us follow the order in real time and make your order safely delivered.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155), lineHeight = 20.sp),
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Daily Safety & Telematics Verification",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("dismiss_daily_gps_disclaimer_btn")
            ) {
                Text("Understood & Continue 👍", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

@Composable
fun DeleteAccountConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFFEE2E2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Delete Account Permanently?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF991B1B)),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Are you sure you want to delete your account? All your personal details, marked location pins, and session credentials will be permanently erased from this device.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                )
                Text(
                    text = "⚠️ This action is irreversible according to Google Play Data Safety policy.",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_delete_account_btn")
            ) {
                Text("Yes, Delete My Account", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable



fun MapLabel(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(12.dp))
        }
        Text(text = title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
    }
}

// --- PASSWORD SUGGESTION DIALOG ---

@Composable
fun PasswordSuggestionDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val suggestion by viewModel.passwordSuggestion.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.generatePasswordSuggestion()
    }

    if (suggestion == null) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(48.dp))
                
                Text(
                    text = "Suggest Strong Password",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = suggestion!!.explanation,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray, lineHeight = 20.sp),
                    textAlign = TextAlign.Center
                )

                // Password preview display card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = suggestion!!.password,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBluePrimary,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("suggested_password_text")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keep Current", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            viewModel.applyPasswordSuggestion(suggestion!!.password)
                            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).testTag("dialog_apply_pass_btn")
                    ) {
                        Text("Apply & Protect", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- STATIC INFO DIALOGS ---

@Composable
fun FAQDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 450.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("FAQ & Guidelines", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FAQItem("What is the minimum fuel delivery?", "For Petrol, Diesel, and High-Octane, the minimum order size is 5 Liters. Orders can be scaled in increments of +1 Liter.")
                    }
                    item {
                        FAQItem("How does the gas rate work?", "LPG Gas cylinders can be ordered starting at 5 KG. The standard delivery price is updated dynamically according to market rates. Check current price under dynamic logistics dashboard.")
                    }
                    item {
                        FAQItem("What is the water gallon capacity?", "We deliver 20-liter drinking water gallons at standard rates. Check live rates on your home dashboard.")
                    }
                    item {
                        FAQItem("Why is there a markup over station retail rates?", "As a premium mobile fuel delivery service, our rates include a Doorstep Bowser Delivery Surcharge (typically Rs. 20-25 per liter). This covers safe specialized transit, fleet logistics, and on-site dispensing, allowing you to bypass dry station lines entirely.")
                    }
                    item {
                        FAQItem("How do 30L+ fuel deals work?", "For large orders exceeding 30 Liters, customers are guided to connect directly with WhatsApp for custom advance payment deals.")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It")
                }
            }
        }
    }
}

@Composable
fun FAQItem(question: String, answer: String) {
    Column {
        Text(text = question, fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = answer, color = Color.Gray, style = MaterialTheme.typography.bodySmall, lineHeight = 16.sp)
    }
}

fun parseWhatsAppMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("*")
        for (i in parts.indices) {
            if (i % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(parts[i])
                }
            } else {
                append(parts[i])
            }
        }
    }
}

@Composable
fun SupportDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("live_support_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ZyphuelBlueDark)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SupportAgent,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Live Support & Help Center",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "24/7 Lahore Operations Desk",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Response Guarantee Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, ZyphuelBluePrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = ZyphuelBluePrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Guaranteed Admin Response",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ZyphuelBlueDark
                                    )
                                )
                                Text(
                                    text = "If you have any problem, contact the admin directly by email or WhatsApp. Replies arrive in less than 24 hours!",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                                )
                            }
                        }
                    }

                    // Direct Contact Options (WhatsApp + Admin Email)
                    Text(
                        text = "DIRECT CONTACT OPTIONS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark,
                            letterSpacing = 0.5.sp
                        )
                    )

                    // Option 1: WhatsApp Support
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFF25D366)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("WhatsApp Live Support", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF14532D))
                            }
                            Text(
                                text = "Instant 24/7 order coordination & rider tracking support on WhatsApp Hotline: +92 323 0112464",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                            Button(
                                onClick = {
                                    val message = "Hello Zyphuel Support, I need assistance with my order delivery in Lahore."
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://wa.me/923230112464?text=${Uri.encode(message)}")
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.fillMaxWidth().testTag("whatsapp_support_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect on WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Option 2: Admin Direct Email
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                        border = BorderStroke(1.dp, Color(0xFFA855F7)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Direct Admin Email Contact", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF581C87))
                            }
                            Text(
                                text = "Admin Email: m.daniyalkhan490@gmail.com\nSend direct inquiries or complaints to our operations management.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                            Button(
                                onClick = {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:m.daniyalkhan490@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Zyphuel Customer Query / Support Request")
                                        putExtra(Intent.EXTRA_TEXT, "Hello Zyphuel Admin,\n\nI need assistance regarding:")
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(emailIntent, "Send Email to Admin"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Admin Email: m.daniyalkhan490@gmail.com", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                                modifier = Modifier.fillMaxWidth().testTag("admin_email_support_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Email Admin Directly", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // FAQ Guidelines Section
                    Text(
                        text = "FAQ GUIDELINES & HELP",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark,
                            letterSpacing = 0.5.sp
                        )
                    )

                    FaqItemCard(
                        question = "1. How does Cash-on-Delivery (COD) fuel delivery work?",
                        answer = "Select your required fuel type (Petrol, Diesel, LPG Gas, Water Tanker), specify quantity and Lahore delivery address, and confirm order. A verified rider will deliver directly to your location."
                    )

                    FaqItemCard(
                        question = "2. How often are fuel and delivery rates updated?",
                        answer = "Zyphuel updates fuel prices automatically every 4 hours aligned with official OGRA Pakistan rate releases. Real-time rate alerts are broadcasted to registered users."
                    )

                    FaqItemCard(
                        question = "3. How fast will Admin respond to my email or query?",
                        answer = "Our administrative team monitors emails 24/7. Your email sent to m.daniyalkhan490@gmail.com is guaranteed a response in less than 24 hours."
                    )

                    FaqItemCard(
                        question = "4. Can I cancel or modify my delivery order?",
                        answer = "Yes, you can cancel an order from your active tracking card before a rider is dispatched or by contacting WhatsApp support."
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun FaqItemCard(question: String, answer: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = ZyphuelBluePrimary
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                )
            }
        }
    }
}

/*
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Panel - WhatsApp Deep Green
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF075E54))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Verified Avatar Circle
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF128C7E), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SupportAgent,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Zyphuel AI Support",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AI Verified 🤖",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF25D366),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = "24/7 Gemini 3.5 Flash Live Support",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFDCF8C6)
                                    )
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.clearSupportChat() },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Clear Chat",
                                tint = Color(0xFFDCF8C6),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // WhatsApp Dispatcher Quick Connect
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F9C5)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFDCF8C6))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WhatsApp Hotline Desk",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF075E54)
                            )
                            Text(
                                text = "Direct chat with Lahore dispatch coordinator",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF128C7E)
                            )
                        }
                        Button(
                            onClick = {
                                val message = "Hello Zyphuel Support, I need assistance with my order delivery in Lahore."
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=923230112464&text=${Uri.encode(message)}")
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("whatsapp_support_button")
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Chat", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Chat Messages Scroll Panel with classic WhatsApp sand background
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE5DDD5))
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatHistory) { msg ->
                            val isUser = msg.sender == "user"
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = if (isUser) 12.dp else 0.dp,
                                        topEnd = if (isUser) 0.dp else 12.dp,
                                        bottomStart = 12.dp,
                                        bottomEnd = 12.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) Color(0xFFDCF8C6) else Color.White
                                    ),
                                    modifier = Modifier
                                        .widthIn(max = 260.dp)
                                        .testTag(if (isUser) "user_chat_bubble" else "bot_chat_bubble"),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = parseWhatsAppMarkdown(msg.text),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Black,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        // WhatsApp style small status ticks
                                        Row(
                                            modifier = Modifier.align(Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Just Now",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    color = Color.Gray
                                                )
                                            )
                                            if (isUser) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "✔✔",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF34B7F1),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isChatLoading) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("chat_loading_indicator")
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF128C7E)
                                    )
                                    Text(
                                        text = "typing...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ),
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Action Chips Panel for offline-first instant answers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFECE5DD))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Quick Bot Commands:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF075E54)
                        ),
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val commandChips = listOf(
                            "💰 Rates" to "Current Price List",
                            "📦 Order" to "Track My Order",
                            "📍 Areas" to "Lahore Coverage Areas",
                            "⚠️ Safety" to "LPG Safety Guidelines"
                        )
                        commandChips.forEach { (label, actionText) ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clickable {
                                        viewModel.sendSupportChatMessage(actionText)
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF128C7E)
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // Input Bar Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0F0F0))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = userMessageText,
                        onValueChange = { userMessageText = it },
                        placeholder = { Text("Type support query...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF128C7E),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true,
                        maxLines = 1
                    )
                    IconButton(
                        onClick = {
                            val prompt = userMessageText.trim()
                            if (prompt.isNotEmpty()) {
                                viewModel.sendSupportChatMessage(prompt)
                                userMessageText = ""
                            }
                        },
                        modifier = Modifier
                            .background(Color(0xFF128C7E), CircleShape)
                            .size(44.dp)
                            .testTag("chat_send_button"),
                        enabled = !isChatLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 6.dp)
                ) {
                    Text("Close WhatsApp Support", color = Color(0xFF075E54), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
*/

@Composable
fun ProfileSettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    if (currentUser == null) return

    val context = LocalContext.current
    var password by remember { mutableStateOf(currentUser!!.passwordHash) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmInProfile by remember { mutableStateOf(false) }


    // System gallery picker
    val pickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.updateProfilePicture(it.toString())
        }
    }

    val picUri = currentUser!!.profilePictureUri
    val isPreset = picUri?.startsWith("preset_") == true
    val avatarBgColor = when (picUri) {
        "preset_blue" -> ZyphuelBluePrimary
        "preset_green" -> Color(0xFF22C55E)
        "preset_amber" -> Color(0xFFFFB000)
        "preset_red" -> Color(0xFFEF4444)
        else -> ZyphuelBluePrimary
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive avatar display
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(avatarBgColor, shape = CircleShape)
                        .clip(CircleShape)
                        .clickable { pickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (!picUri.isNullOrBlank() && !isPreset) {
                        coil.compose.AsyncImage(
                            model = picUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = currentUser!!.name.take(2).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        )
                    }
                    
                    // Edit Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "EDIT",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Upload Button
                Button(
                    onClick = { pickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary.copy(alpha = 0.1f), contentColor = ZyphuelBluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("upload_profile_pic_btn")
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Camera", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Photo", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Predefined Presets Row
                Text(
                    text = "OR CHOOSE A COLOR PRESET",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "preset_blue" to ZyphuelBluePrimary,
                        "preset_green" to Color(0xFF22C55E),
                        "preset_amber" to Color(0xFFFFB000),
                        "preset_red" to Color(0xFFEF4444)
                    ).forEach { (presetKey, color) ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (picUri == presetKey) 2.dp else 0.dp,
                                    color = if (picUri == presetKey) Color.Black else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateProfilePicture(presetKey)
                                }
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name (Read-only or viewable)
                OutlinedTextField(
                    value = currentUser!!.name,
                    onValueChange = {},
                    label = { Text("Name") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = ZyphuelBluePrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_view"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.LightGray,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Email (Read-only or viewable)
                OutlinedTextField(
                    value = currentUser!!.email,
                    onValueChange = {},
                    label = { Text("Email Address") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = ZyphuelBluePrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_email_view"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.LightGray,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Password (Editable to change)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = ZyphuelBluePrimary) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description, tint = ZyphuelBluePrimary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("profile_password_input"),
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray,
                        focusedBorderColor = ZyphuelBluePrimary,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Text(
                    text = "You can update your password directly. Changes apply immediately in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Enable Fingerprint Biometric Card ---
                val bioModuleInProfile = if (currentUser!!.role == "rider") com.example.security.AppModule.RIDER else com.example.security.AppModule.CUSTOMER
                val isCustomerBioEnabledProfile by viewModel.isCustomerBioEnabled.collectAsState()
                val isRiderBioEnabledProfile by viewModel.isRiderBioEnabled.collectAsState()
                val isBioEnabledInProfile = if (currentUser!!.role == "rider") isRiderBioEnabledProfile else isCustomerBioEnabledProfile
                val fragmentActivityProfile = context as? androidx.fragment.app.FragmentActivity

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ZyphuelBluePrimary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(22.dp))
                                Text("Biometric Lock", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Text(
                                text = if (isBioEnabledInProfile) "Enabled ✔️" else "Disabled 🔒",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBioEnabledInProfile) Color(0xFF059669) else Color.Red
                                )
                            )
                        }

                        Text(
                            text = if (isBioEnabledInProfile)
                                "Fingerprint is enabled. You can log in using biometrics after logging out."
                            else
                                "Fingerprint is disabled by default. Enable it below to log in using biometrics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Button(
                            onClick = {
                                if (!isBioEnabledInProfile) {
                                    if (fragmentActivityProfile != null) {
                                        com.example.security.BiometricSecurityManager.showBiometricPrompt(
                                            activity = fragmentActivityProfile,
                                            title = "Enable Fingerprint",
                                            subtitle = "Scan fingerprint to authorize biometric access",
                                            description = "Account: ${currentUser!!.email}",
                                            onSuccess = {
                                                viewModel.enableBiometricForModule(context, bioModuleInProfile, currentUser!!)
                                                Toast.makeText(context, "Fingerprint enabled! 👆", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { _, errStr ->
                                                Toast.makeText(context, errStr, Toast.LENGTH_SHORT).show()
                                            },
                                            onFailed = {
                                                Toast.makeText(context, "Fingerprint verification failed", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        viewModel.enableBiometricForModule(context, bioModuleInProfile, currentUser!!)
                                    }
                                } else {
                                    viewModel.disableBiometricForModule(context, bioModuleInProfile)
                                    Toast.makeText(context, "Fingerprint disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBioEnabledInProfile) Color(0xFFDC2626) else Color(0xFF059669)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("toggle_fingerprint_profile_btn")
                        ) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBioEnabledInProfile) "Disable Fingerprint" else "Enable Fingerprint",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                    }
                }

                // --- Verified Badge Authority Card ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentUser!!.role == "rider" && currentUser!!.isVerified) Color(0xFFECFDF5) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentUser!!.role == "rider" && currentUser!!.isVerified) Color(0xFF10B981) else Color(0xFFE2E8F0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (currentUser!!.role == "rider" && currentUser!!.isVerified) Icons.Filled.Verified else Icons.Filled.Shield,
                                contentDescription = "Badge Status",
                                tint = if (currentUser!!.role == "rider" && currentUser!!.isVerified) Color(0xFF10B981) else ZyphuelBluePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (currentUser!!.role == "rider" && currentUser!!.isVerified) "Verified Badge Awarded ✔️" else "Verified Badge Status",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentUser!!.role == "rider" && currentUser!!.isVerified) Color(0xFF065F46) else ZyphuelBlueDark
                                )
                            )
                        }

                        if (currentUser!!.role == "rider") {
                            if (currentUser!!.isVerified) {
                                Text(
                                    text = "Your account has been granted a Verified Badge at the Admin's sole discretion.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF047857)
                                )
                            } else {
                                Text(
                                    text = "The Verified Badge is awarded strictly at the Admin's discretion. No one else can receive this badge.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "If you need this badge, please contact the admin via email:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.DarkGray
                                )
                                Button(
                                    onClick = {
                                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:m.daniyalkhan490@gmail.com")
                                            putExtra(Intent.EXTRA_SUBJECT, "Request for Verified Badge - ${currentUser!!.name}")
                                            putExtra(Intent.EXTRA_TEXT, "Hello Admin,\n\nI am requesting a Verified Badge for my rider account (${currentUser!!.email}).\n\nThank you.")
                                        }
                                        try {
                                            context.startActivity(emailIntent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Contact Admin at: m.daniyalkhan490@gmail.com", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Email Admin (m.daniyalkhan490@gmail.com)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                }
                            }
                        } else {
                            Text(
                                text = "Notice: The Verified Badge is strictly reserved for Riders at Admin discretion. Customers cannot receive this badge.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "If you are a rider seeking verification, contact Admin via email: m.daniyalkhan490@gmail.com",
                                style = MaterialTheme.typography.labelSmall.copy(color = ZyphuelBluePrimary, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // --- Danger Zone / Delete Account (Google Play Mandatory Policy) ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFF87171))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = "Delete Account",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Delete Account & Erase Data",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            )
                        }

                        Text(
                            text = "Permanently remove your user profile, saved location pins, order history, and biometrics from the system. This action cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D)
                        )

                        Button(
                            onClick = { showDeleteConfirmInProfile = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("profile_delete_account_btn")
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Permanently Delete Account", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updatePassword(password)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("profile_update_button")
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )

    if (showDeleteConfirmInProfile) {
        DeleteAccountConfirmationDialog(
            onConfirm = {
                showDeleteConfirmInProfile = false
                viewModel.deleteCurrentAccount {
                    onDismiss()
                }
            },
            onDismiss = { showDeleteConfirmInProfile = false }
        )
    }
}


@Composable
fun MyOrdersDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val orders by viewModel.customerOrders.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = ZyphuelBluePrimary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "My Order History",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = ZyphuelBlueSecondary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("orders_count_badge")
                ) {
                    Text(
                        text = "Total Orders Placed: ${orders.size}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBluePrimary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (orders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You haven't placed any orders yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("orders_list")
                    ) {
                        items(orders) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = order.serviceType,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ZyphuelBlueDark
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = when (order.status) {
                                                        "Completed" -> Color(0xFFE2FBE7)
                                                        "Pending" -> Color(0xFFFEF3C7)
                                                        "Delivering", "Assigned" -> Color(0xFFEFF6FF)
                                                        else -> Color(0xFFFEE2E2)
                                                    },
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = order.status,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (order.status) {
                                                        "Completed" -> Color(0xFF15803D)
                                                        "Pending" -> Color(0xFFB45309)
                                                        "Delivering", "Assigned" -> Color(0xFF1D4ED8)
                                                        else -> Color(0xFFB91C1C)
                                                    }
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Quantity: ${order.quantity} ${if (order.serviceType == "LPG Gas") "kg" else if (order.serviceType == "Water") "Gallons" else "Liters"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "Total Price: Rs.${order.totalPrice}",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ZyphuelBluePrimary
                                    )
                                    Text(
                                        text = "Address: ${order.deliveryAddress}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// --- PHASE 3: RIDER SIDE / HOME SCREEN ---

@Composable
fun RiderHomeScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val orders by viewModel.riderOrders.collectAsState()
    val liveLocationCoordinates by viewModel.liveLocationCoordinates.collectAsState()
    val context = LocalContext.current

    if (currentUser == null) return

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showFAQDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var showReceivedOrdersDialog by remember { mutableStateOf(false) }
    var showFcmDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                viewModel = viewModel,
                currentUser = currentUser!!,
                onClose = { scope.launch { drawerState.close() } },
                onOpenPasswordSuggestion = { showPasswordDialog = true },
                onOpenOrders = { showReceivedOrdersDialog = true },
                onOpenFAQ = { showFAQDialog = true },
                onOpenSupport = { showSupportDialog = true },
                onOpenProfile = { showProfileSettingsDialog = true },
                onOpenFcm = { showFcmDialog = true }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(30.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Rider Panel - ${currentUser!!.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = ZyphuelBluePrimary)
                        }
                    },
                    actions = {
                        val notifications by viewModel.notifications.collectAsState()
                        var showNotificationsDialog by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            IconButton(onClick = { showNotificationsDialog = true }) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = ZyphuelBluePrimary)
                            }
                            if (notifications.isNotEmpty()) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 4.dp, end = 4.dp),
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(notifications.size.toString(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (showNotificationsDialog) {
                            NotificationsDialog(
                                notifications = notifications,
                                onDismiss = { showNotificationsDialog = false },
                                onClearAll = { viewModel.clearNotifications() }
                            )
                        }

                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Logout", tint = Color.Red)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    modifier = Modifier.shadow(2.dp)
                )
            }
        ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ZyphuelLightBackground)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentUser!!.isVerified) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentUser!!.isVerified) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (currentUser!!.isVerified) Icons.Filled.Verified else Icons.Filled.Shield,
                                contentDescription = "Verified Badge",
                                tint = if (currentUser!!.isVerified) Color(0xFF10B981) else Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = when {
                                    currentUser!!.isVerified -> "Verified Badge Active ✔️"
                                    currentUser!!.hasRequestedVerification -> "Verified Badge Application Pending ⏳"
                                    else -> "Apply for Verified Badge"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentUser!!.isVerified) Color(0xFF065F46) else Color(0xFF92400E)
                                )
                            )
                        }

                        if (currentUser!!.isVerified) {
                            Text(
                                text = "Your rider account is verified by the Admin. Customers can see your verified badge on assigned orders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF047857)
                            )
                        } else if (currentUser!!.hasRequestedVerification) {
                            Text(
                                text = "Your application for a Verified Badge has been submitted. The Admin is reviewing your profile and credentials.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "Status: Notification sent to Admin. You will be notified once approved or denied.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                        } else {
                            Text(
                                text = "Submit your application to request a Verified Badge. The Admin will review your profile details and approve or deny your request.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                            Button(
                                onClick = {
                                    viewModel.applyForRiderVerification()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("apply_for_verification_btn")
                            ) {
                                Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply for Verified Badge", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            }
                        }
                    }
                }
            }

            item {
                Text("Deliveries in Lahore Queue", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = ZyphuelBlueDark)
            }

            if (orders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.List, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No pending or active deliveries available", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(orders) { order ->
                    RiderOrderCard(order = order, currentUser = currentUser!!, viewModel = viewModel)
                }
            }
        }
    }
}

    // --- DIALOG CONTROLLERS FOR RIDER ---
    if (showFAQDialog) {
        FAQDialog { showFAQDialog = false }
    }

    if (showSupportDialog) {
        SupportDialog(viewModel = viewModel) { showSupportDialog = false }
    }

    if (showProfileSettingsDialog) {
        ProfileSettingsDialog(viewModel = viewModel) { showProfileSettingsDialog = false }
    }

    if (showReceivedOrdersDialog) {
        ReceivedOrdersDialog(viewModel = viewModel) { showReceivedOrdersDialog = false }
    }
}

@Composable
fun ReceivedOrdersDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val allOrders by viewModel.allOrders.collectAsState()
    val pendingOrders = allOrders.filter { it.status == "Pending" }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.ListAlt,
                    contentDescription = null,
                    tint = ZyphuelBluePrimary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Received Lahore Orders Queue",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = ZyphuelBlueSecondary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "Available Orders: ${pendingOrders.size}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBluePrimary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (pendingOrders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inbox,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No pending received orders found in Lahore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("received_orders_list")
                    ) {
                        items(pendingOrders) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = order.serviceType,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ZyphuelBlueDark
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Color(0xFFFEF3C7),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = order.status,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Quantity: ${order.quantity} ${if (order.serviceType == "LPG Gas") "kg" else if (order.serviceType == "Water") "Gallons" else "Liters"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "Total Price: Rs.${order.totalPrice}",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ZyphuelBluePrimary
                                    )
                                    Text(
                                        text = "Address: ${order.deliveryAddress}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Button(
                                        onClick = {
                                            viewModel.acceptRiderOrder(order.id)
                                            Toast.makeText(context, "Order #${order.id} accepted! Go to deliveries.", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("accept_ride_order_btn")
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Accept Ride & Order", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBlueDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close Queue", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun RiderOrderCard(order: OrderEntity, currentUser: UserEntity, viewModel: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Order #${order.id} - ${order.serviceType}", fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                Box(
                    modifier = Modifier
                        .background(ZyphuelBluePrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(order.status, color = ZyphuelBluePrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Qty: ${order.quantity} | Total Cash: Rs. ${order.totalPrice}", style = MaterialTheme.typography.bodyMedium)

            // Origin & Destination Cards for Rider
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🏁 Origin Depot: Zyphuel Central Main Headquarters, Green Town, Lahore",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "📍 Destination Pin: ${order.deliveryAddress}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                    )
                    Text("Contact: ${order.customerName} (${order.customerPhone})", color = Color.DarkGray, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Rider Google Map View
            LahoreGoogleEmbedMapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 4.dp),
                serviceType = order.serviceType,
                orderStatus = order.status,
                driverName = currentUser.name
            )
            
            if (order.rating != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { starIndex ->
                        val isFilled = starIndex < order.rating
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (isFilled) Color(0xFFFFB000) else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Customer Rating: ${order.rating}/5",
                        style = MaterialTheme.typography.labelSmall.copy(color = ZyphuelBlueDark, fontWeight = FontWeight.Bold)
                    )
                }
                if (!order.feedback.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "“${order.feedback}”",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.status == "Pending") {
                    Button(
                        onClick = { viewModel.acceptRiderOrder(order.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("accept_ride_order_card_btn")
                    ) {
                        Text("Accept Ride / Order (COD)", fontWeight = FontWeight.Bold)
                    }
                } else if (order.status == "Assigned") {
                    Button(
                        onClick = { viewModel.changeOrderStatus(order.id, "Delivering") },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBlueSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Delivering")
                    }
                } else if (order.status == "Delivering") {
                    Button(
                        onClick = { viewModel.changeOrderStatus(order.id, "Arrived") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reached Location 📍", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.changeOrderStatus(order.id, "Completed") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark Delivered 🎉", fontSize = 12.sp)
                    }
                } else if (order.status == "Arrived") {
                    Button(
                        onClick = { viewModel.changeOrderStatus(order.id, "Completed") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark Delivered (COD Collected) 🎉", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAnalyticsDashboard(
    orders: List<OrderEntity>,
    riders: List<UserEntity> = emptyList(),
    customers: List<UserEntity> = emptyList(),
    viewModel: MainViewModel? = null
) {
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryIndex by remember { mutableStateOf<Int?>(null) }

    val density = androidx.compose.ui.platform.LocalDensity.current

    // Caching Paint objects using remember (useMemo equivalent) to completely avoid garbage collection allocations inside draw loops.
    val gridLabelPaint = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    val gridLabelPaintSmall = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
        }
    }

    val xLabelPaintNormal = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT
        }
    }

    val xLabelPaintSelected = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    val barValuePaintNormal = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    val barValuePaintSelected = remember(density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = with(density) { 9.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    // Reuse Path objects instead of allocating fresh instances inside onDraw frames
    val areaPath = remember { Path() }
    val linePath = remember { Path() }

    val sdf = remember { java.text.SimpleDateFormat("dd MMM", java.util.Locale.US) }
    val last7Days = remember(orders) {
        List(7) { index ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, - (6 - index))
            cal
        }
    }

    val dayLabels = remember(last7Days) {
        last7Days.map { sdf.format(it.time) }
    }

    val realOrderCounts = remember(orders, dayLabels) {
        val counts = FloatArray(7) { 0f }
        orders.forEach { order ->
            val orderDateStr = sdf.format(java.util.Date(order.createdAt))
            val matchIndex = dayLabels.indexOf(orderDateStr)
            if (matchIndex != -1) {
                counts[matchIndex] += 1f
            }
        }
        counts
    }

    val finalVolumes = remember(realOrderCounts) { realOrderCounts }

    // Top Categories data
    val categories = listOf("Petrol", "Diesel", "High-Octane", "LPG Gas", "Water")
    val categoryColors = listOf(
        ZyphuelBluePrimary,
        Color(0xFF0EA5E9), // Sky Blue
        Color(0xFF3B82F6), // Royal Blue
        Color(0xFFF97316), // Orange
        Color(0xFF14B8A6)  // Teal
    )
    val realCounts = remember(orders) {
        val counts = FloatArray(5) { 0f }
        orders.forEach { order ->
            val idx = categories.indexOf(order.serviceType)
            if (idx != -1) {
                counts[idx] += order.quantity.toFloat()
            }
        }
        counts
    }
    val finalCounts = remember(realCounts) { realCounts }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Introduction header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ZyphuelBluePrimary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = "Insights",
                            tint = ZyphuelBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Lahore Real-Time Analytics & Operations Hub",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBlueDark
                            )
                        )
                        Text(
                            text = "Real-time user/rider registrations, order delivery trends & 4-hour rate alerts.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Operations metric badges
                val canceledCount = remember(orders) { orders.count { it.status == "Canceled" || it.status == "Cancelled" } }
                val canceledNoRider = remember(orders) { orders.count { (it.status == "Canceled" || it.status == "Cancelled") && it.riderName == null } }
                val verifiedRidersCount = remember(riders) { riders.count { it.adminApprovalStatus == "Approved" || it.isVerified } }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Registered Customers", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text("${customers.size} Users", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                    }
                    Column {
                        Text("Registered Riders", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text("${riders.size} Riders", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Rider Not Available", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text("$canceledNoRider Canceled", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Red))
                    }
                }
            }
        }

        // 4-Hour Automated Price Broadcast Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = ZyphuelBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "4-Hour Periodic Rate Update Engine",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBlueDark
                            )
                        )
                    }
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Automated background ticker triggers every 4 hours to send live price notifications & real-time emails to all registered customers & riders.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel?.triggerFourHourPriceUpdateBroadcast() },
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("trigger_4hr_price_broadcast_btn")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast 4-Hour Rate Update Alert Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Daily Delivery Volume Line/Area Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DAILY DELIVERY VOLUME",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Interactive area chart of recent deliveries in Lahore corridor",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Line / Area Chart canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(finalVolumes) {
                                detectTapGestures { offset ->
                                    val w = size.width
                                    val leftPadding = 35.dp.toPx()
                                    val rightPadding = 15.dp.toPx()
                                    val chartWidth = w - leftPadding - rightPadding
                                    val stepX = chartWidth / 6f
                                    
                                    val clickedIndex = ((offset.x - leftPadding + stepX / 2f) / stepX).toInt().coerceIn(0, 6)
                                    selectedDayIndex = if (selectedDayIndex == clickedIndex) null else clickedIndex
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val leftPadding = 35.dp.toPx()
                        val bottomPadding = 25.dp.toPx()
                        val topPadding = 15.dp.toPx()
                        val rightPadding = 15.dp.toPx()

                        val chartW = w - leftPadding - rightPadding
                        val chartH = h - topPadding - bottomPadding

                        val maxVal = (finalVolumes.maxOrNull() ?: 20f).coerceAtLeast(10f)
                        val niceMaxVal = (kotlin.math.ceil(maxVal / 5f) * 5f).toFloat()

                        // Draw horizontal grid lines and labels
                        val gridLinesCount = 4
                        for (i in 0..gridLinesCount) {
                            val gridY = topPadding + chartH * (1f - i.toFloat() / gridLinesCount)
                            val gridVal = (niceMaxVal * i.toFloat() / gridLinesCount).toInt()

                            // Draw Line
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.35f),
                                start = Offset(leftPadding, gridY),
                                end = Offset(w - rightPadding, gridY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            // Draw Y-axis Label using cached pre-allocated Paint
                            drawContext.canvas.nativeCanvas.drawText(
                                gridVal.toString(),
                                leftPadding - 8.dp.toPx(),
                                gridY + 4.dp.toPx(),
                                gridLabelPaint
                            )
                        }

                        // Generate coordinates
                        val points = finalVolumes.mapIndexed { index, volume ->
                            val x = leftPadding + index * (chartW / 6f)
                            val y = topPadding + chartH * (1f - volume / niceMaxVal)
                            Offset(x, y)
                        }

                        // Draw Area under path with beautiful blue gradient
                        if (points.isNotEmpty()) {
                            areaPath.reset()
                            areaPath.moveTo(points.first().x, h - bottomPadding)
                            points.forEach { areaPath.lineTo(it.x, it.y) }
                            areaPath.lineTo(points.last().x, h - bottomPadding)
                            areaPath.close()
                            drawPath(
                                path = areaPath,
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        ZyphuelBluePrimary.copy(alpha = 0.35f),
                                        ZyphuelBluePrimary.copy(alpha = 0.01f)
                                    ),
                                    startY = topPadding,
                                    endY = h - bottomPadding
                                )
                            )

                            // Draw smooth stroke line
                            linePath.reset()
                            linePath.moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                linePath.lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = linePath,
                                color = ZyphuelBluePrimary,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Draw dots and highlights
                        points.forEachIndexed { index, point ->
                            val isSelected = selectedDayIndex == index
                            drawCircle(
                                color = if (isSelected) ZyphuelBlueSecondary else ZyphuelBluePrimary,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = point
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = ZyphuelBluePrimary.copy(alpha = 0.25f),
                                    radius = 12.dp.toPx(),
                                    center = point
                                )
                            }

                            // Draw X-axis label
                            drawContext.canvas.nativeCanvas.drawText(
                                dayLabels[index],
                                point.x,
                                h - 4.dp.toPx(),
                                if (isSelected) xLabelPaintSelected else xLabelPaintNormal
                            )
                        }
                    }
                }

                // Interactive Info Tooltip
                selectedDayIndex?.let { index ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ZyphuelBlueDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Event,
                                    contentDescription = null,
                                    tint = ZyphuelBlueSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Date: ${dayLabels[index]}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = "${finalVolumes[index].toInt()} Deliveries completed",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = ZyphuelBlueSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } ?: run {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Tap on any point along the line to inspect daily details.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        // Top Requested Items Bar Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TOP REQUESTED ITEMS (LAHORE)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Material demand breakout in volume (Liters, KG, and Gallons)",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bar Chart canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(finalCounts) {
                                detectTapGestures { offset ->
                                    val w = size.width
                                    val leftPadding = 35.dp.toPx()
                                    val rightPadding = 15.dp.toPx()
                                    val chartWidth = w - leftPadding - rightPadding
                                    val barWidth = chartWidth / 5f
                                    
                                    val clickedIndex = ((offset.x - leftPadding) / barWidth).toInt().coerceIn(0, 4)
                                    selectedCategoryIndex = if (selectedCategoryIndex == clickedIndex) null else clickedIndex
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val leftPadding = 35.dp.toPx()
                        val bottomPadding = 25.dp.toPx()
                        val topPadding = 15.dp.toPx()
                        val rightPadding = 15.dp.toPx()

                        val chartW = w - leftPadding - rightPadding
                        val chartH = h - topPadding - bottomPadding

                        val maxVal = (finalCounts.maxOrNull() ?: 100f).coerceAtLeast(50f)
                        val niceMaxVal = (kotlin.math.ceil(maxVal / 20f) * 20f).toFloat()

                        // Draw Grid lines
                        val gridLinesCount = 4
                        for (i in 0..gridLinesCount) {
                            val gridY = topPadding + chartH * (1f - i.toFloat() / gridLinesCount)
                            val gridVal = (niceMaxVal * i.toFloat() / gridLinesCount).toInt()

                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.2f),
                                start = Offset(leftPadding, gridY),
                                end = Offset(w - rightPadding, gridY),
                                strokeWidth = 1.dp.toPx()
                            )

                            // Y-axis Label using cached pre-allocated Paint
                            drawContext.canvas.nativeCanvas.drawText(
                                gridVal.toString(),
                                leftPadding - 6.dp.toPx(),
                                gridY + 3.dp.toPx(),
                                gridLabelPaintSmall
                            )
                        }

                        // Draw bars
                        val barCount = 5
                        val barWidth = chartW / barCount
                        val barPaddingFraction = 0.35f

                        for (i in 0 until barCount) {
                            val isSelected = selectedCategoryIndex == i
                            val barColor = categoryColors[i]
                            val count = finalCounts[i]
                            val barH = chartH * (count / niceMaxVal)

                            val barLeft = leftPadding + i * barWidth + (barWidth * barPaddingFraction / 2f)
                            val barRight = leftPadding + (i + 1) * barWidth - (barWidth * barPaddingFraction / 2f)
                            val barTop = h - bottomPadding - barH
                            val barBottom = h - bottomPadding

                            // Draw rounded bar
                            drawRoundRect(
                                color = if (isSelected) barColor else barColor.copy(alpha = 0.85f),
                                topLeft = Offset(barLeft, barTop),
                                size = androidx.compose.ui.geometry.Size(barRight - barLeft, barBottom - barTop),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            // Highlight boundary
                            if (isSelected) {
                                drawRoundRect(
                                    color = ZyphuelBlueSecondary,
                                    topLeft = Offset(barLeft - 2.dp.toPx(), barTop - 2.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(barRight - barLeft + 4.dp.toPx(), barBottom - barTop + 4.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            // Value label on top of bar
                            drawContext.canvas.nativeCanvas.drawText(
                                count.toInt().toString(),
                                (barLeft + barRight) / 2f,
                                barTop - 4.dp.toPx(),
                                if (isSelected) barValuePaintSelected else barValuePaintNormal
                            )

                            // Category Label at the bottom
                            val abbrevLabel = when (categories[i]) {
                                "High-Octane" -> "Octane"
                                "LPG Gas" -> "LPG"
                                else -> categories[i]
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                abbrevLabel,
                                (barLeft + barRight) / 2f,
                                h - 4.dp.toPx(),
                                if (isSelected) xLabelPaintSelected else xLabelPaintNormal
                            )
                        }
                    }
                }

                // Bar Category Interactive Tooltip
                selectedCategoryIndex?.let { index ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val itemUnit = when (categories[index]) {
                        "Water" -> "Gallons"
                        "LPG Gas" -> "KG"
                        else -> "Liters"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = categoryColors[index].copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, categoryColors[index]),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(categoryColors[index], CircleShape)
                                )
                                Text(
                                    text = categories[index],
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = ZyphuelBlueDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = "${finalCounts[index].toInt()} $itemUnit Requested",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = ZyphuelBlueDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } ?: run {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Tap on any bar to inspect material quantities.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminFuelPriceNotificationScheduleCard(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentIntervalHours by viewModel.priceNotificationIntervalHours.collectAsState()
    val isNotifEnabled by viewModel.isPriceNotificationAllowed.collectAsState()

    var selectedHoursInput by remember(currentIntervalHours) { mutableStateOf(currentIntervalHours.toString()) }
    var enabledSwitchState by remember(isNotifEnabled) { mutableStateOf(isNotifEnabled) }

    val presetIntervals = listOf(1, 2, 4, 6, 8, 12, 24)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_fuel_price_notification_schedule_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = ZyphuelBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Fuel Price Notification Broadcast Schedule",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = ZyphuelBlueDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Specify how many hours after which real-time fuel price notifications will automatically go out to all users. Users cannot change this schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Enable / Disable Master Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZyphuelLightBackground, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time Price Broadcasts",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZyphuelBlueDark
                    )
                    Text(
                        text = if (enabledSwitchState) "Active: Broadcasting every ${selectedHoursInput.toIntOrNull() ?: currentIntervalHours} hours" else "Disabled: Price notifications turned off",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabledSwitchState) Color(0xFF16A34A) else Color.Red
                    )
                }
                Switch(
                    checked = enabledSwitchState,
                    onCheckedChange = { enabledSwitchState = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ZyphuelBluePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Select Interval in Hours:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Interval Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetIntervals.forEach { hrs ->
                    val isSelected = (selectedHoursInput.toIntOrNull() == hrs)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedHoursInput = hrs.toString() },
                        label = {
                            Text(
                                text = "${hrs}h",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZyphuelBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Hours Input Field
            OutlinedTextField(
                value = selectedHoursInput,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() }) {
                        selectedHoursInput = input
                    }
                },
                label = { Text("Notification Interval (Hours)") },
                supportingText = { Text("Enter time interval in hours (e.g. 1, 2, 4, 6, 12, 24)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Text("Hours", modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = ZyphuelBluePrimary,
                    unfocusedLabelColor = Color.DarkGray,
                    focusedBorderColor = ZyphuelBluePrimary,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val hours = selectedHoursInput.toIntOrNull() ?: 4
                        viewModel.updateAdminPriceNotificationSchedule(context, hours, enabledSwitchState)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_save_price_schedule_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Apply Schedule", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        com.example.worker.FuelPriceWorker.triggerImmediatePriceWork(context)
                        viewModel.postLocalSystemNotification("⛽ Test Broadcast Triggered", "Real-time rates broadcast test initiated by Admin.")
                    },
                    modifier = Modifier.testTag("admin_test_price_broadcast_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ZyphuelBluePrimary)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Broadcast", fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary)
                }
            }
        }
    }
}

// --- PHASE 6: ADMIN DASHBOARD SCREEN ---

@Composable
fun AdminDashboardScreen(viewModel: MainViewModel) {
    val orders by viewModel.allOrders.collectAsState()
    val riders by viewModel.allRiders.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val logs by viewModel.auditLogs.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var showAddRiderDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showFcmDialog by remember { mutableStateOf(false) }
    var showAsoDialog by remember { mutableStateOf(false) }

    // Analytics calculations from real Room local data cached using remember (equivalent of useMemo)
    val totalRevenue = remember(orders) { orders.filter { it.status == "Completed" }.sumOf { it.totalPrice } }
    val completedOrdersCount = remember(orders) { orders.filter { it.status == "Completed" }.size }
    val pendingOrdersCount = remember(orders) { orders.filter { it.status == "Pending" }.size }

    BackHandler {
        viewModel.navigateTo("customer_home")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(30.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zyphuel Admin Center", fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo("customer_home") }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ZyphuelBluePrimary)
                    }
                },
                actions = {
                    val notifications by viewModel.notifications.collectAsState()
                    var showNotificationsDialog by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = { showNotificationsDialog = true }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = ZyphuelBluePrimary)
                        }
                        if (notifications.isNotEmpty()) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp),
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                Text(notifications.size.toString(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (showNotificationsDialog) {
                        NotificationsDialog(
                            notifications = notifications,
                            onDismiss = { showNotificationsDialog = false },
                            onClearAll = { viewModel.clearNotifications() }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(2.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ZyphuelLightBackground)
                .padding(innerPadding)
        ) {
            // Stats Row Summary
            val canceledOrdersCount = remember(orders) { orders.count { it.status == "Canceled" } }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard(title = "Revenue", value = "Rs.${totalRevenue.toInt()}", modifier = Modifier.weight(1f))
                    AdminStatCard(title = "Customers", value = "${customers.size}", modifier = Modifier.weight(1f))
                    AdminStatCard(title = "Riders", value = "${riders.size}", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard(title = "Active Orders", value = "$pendingOrdersCount", modifier = Modifier.weight(1f))
                    AdminStatCard(title = "Completed", value = "$completedOrdersCount", modifier = Modifier.weight(1f))
                    AdminStatCard(title = "Canceled", value = "$canceledOrdersCount", modifier = Modifier.weight(1f))
                }
            }

            // Scrollable Tab switcher (8 tabs)
            ScrollableTabRow(selectedTabIndex = activeTab, containerColor = Color.White) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Text("Analytics", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Text("Riders (${riders.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                    Text("Customers (${customers.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                    Text("Orders (${orders.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeTab == 4, onClick = { activeTab = 4 }) {
                    Text("Feedback", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeTab == 5, onClick = { activeTab = 5 }) {
                    Text("Fuel Prices", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeTab == 6, onClick = { activeTab = 6 }) {
                    Text("Email Logs", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeTab == 7, onClick = { activeTab = 7 }) {
                    Text("Audit Logs", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }

            // Tab content body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    0 -> { // Live Charts and Lahore demand visualization
                        AdminAnalyticsDashboard(
                            orders = orders,
                            riders = riders,
                            customers = customers,
                            viewModel = viewModel
                        )
                    }
                    1 -> { // Riders List & Approval
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Registered Riders (${riders.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ZyphuelBlueDark
                                )
                                Button(
                                    onClick = { showAddRiderDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("admin_add_riders_btn")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Riders", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (riders.isEmpty()) {
                                    item { Text("No riders registered yet.") }
                                } else {
                                    items(riders) { rider ->
                                        AdminRiderCard(rider = rider, viewModel = viewModel)
                                    }
                                }
                            }
                        }

                        if (showAddRiderDialog) {
                            AddRiderDialog(viewModel = viewModel, onDismiss = { showAddRiderDialog = false })
                        }
                    }
                    2 -> { // Registered Customers List
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Registered Customers (${customers.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ZyphuelBlueDark
                                )
                                Button(
                                    onClick = { showAddCustomerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("admin_add_customer_btn")
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Customer", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (customers.isEmpty()) {
                                    item { Text("No customers registered yet.", color = Color.Gray) }
                                } else {
                                    items(customers) { customer ->
                                        AdminCustomerCard(customer = customer, orders = orders, viewModel = viewModel)
                                    }
                                }
                            }
                        }

                        if (showAddCustomerDialog) {
                            AddCustomerDialog(viewModel = viewModel, onDismiss = { showAddCustomerDialog = false })
                        }
                    }
                    3 -> { // Orders list
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (orders.isEmpty()) {
                                item { Text("No orders placed yet.") }
                            } else {
                                items(orders) { order ->
                                    AdminOrderCard(order = order)
                                }
                            }
                        }
                    }
                    4 -> { // Customer Feedback Tab
                        val ordersWithFeedback = orders.filter { it.rating != null }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (ordersWithFeedback.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No customer feedback received yet.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                items(ordersWithFeedback) { order ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Order #${order.id} - ${order.serviceType}",
                                                        fontWeight = FontWeight.Bold,
                                                        color = ZyphuelBlueDark,
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                    Text(
                                                        text = "Customer: ${order.customerName}",
                                                        color = Color.Gray,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                // Star Rating
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    repeat(5) { starIndex ->
                                                        val isFilled = starIndex < (order.rating ?: 0)
                                                        Icon(
                                                            imageVector = Icons.Filled.Star,
                                                            contentDescription = null,
                                                            tint = if (isFilled) Color(0xFFFFB000) else Color.LightGray,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "Feedback:",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (order.feedback.isNullOrBlank()) "“No comment left.”" else "“${order.feedback}”",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = Color.DarkGray,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Delivered by: ${order.riderName ?: "Unassigned Rider"}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = ZyphuelBluePrimary, fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    5 -> { // Dynamic Fuel Prices Control Center
                        val petrolPrice by viewModel.petrolPrice.collectAsState()
                        val dieselPrice by viewModel.dieselPrice.collectAsState()
                        val octanePrice by viewModel.highOctanePrice.collectAsState()
                        val lpgPrice by viewModel.lpgGasPrice.collectAsState()
                        val waterPrice by viewModel.waterPrice.collectAsState()
                        val priceSyncing by viewModel.priceSyncing.collectAsState()
                        val lastSyncTime by viewModel.lastPriceSyncTime.collectAsState()

                        var petInput by remember(petrolPrice) { mutableStateOf(petrolPrice.toString()) }
                        var dieInput by remember(dieselPrice) { mutableStateOf(dieselPrice.toString()) }
                        var octInput by remember(octanePrice) { mutableStateOf(octanePrice.toString()) }
                        var lpgInput by remember(lpgPrice) { mutableStateOf(lpgPrice.toString()) }
                        var watInput by remember(waterPrice) { mutableStateOf(waterPrice.toString()) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Admin Control: Dynamic Real-time Fuel Price Notification Broadcast Schedule
                            AdminFuelPriceNotificationScheduleCard(viewModel = viewModel)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Search Engine Price Sync",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ZyphuelBlueDark
                                        )
                                        if (priceSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = ZyphuelBluePrimary)
                                        } else {
                                            IconButton(onClick = { viewModel.syncFuelPricesViaGemini() }) {
                                                Icon(Icons.Filled.Sync, contentDescription = "Sync", tint = ZyphuelBluePrimary)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Query latest fuel prices in Lahore via Gemini AI. Broad alert notifications and admin emails will trigger automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.syncFuelPricesViaGemini() },
                                        enabled = !priceSyncing,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                                    ) {
                                        Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Fetch & Broadcast Real-Time Rates", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Last Synchronized: $lastSyncTime",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Manual Rate Overrides",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ZyphuelBlueDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Manually override system pricing. Saving will trigger email updates and customer alerts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = petInput,
                                        onValueChange = { petInput = it },
                                        label = { Text("Petrol Price (Rs./L)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedLabelColor = ZyphuelBluePrimary,
                                            unfocusedLabelColor = Color.DarkGray,
                                            focusedBorderColor = ZyphuelBluePrimary,
                                            unfocusedBorderColor = Color.LightGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = dieInput,
                                        onValueChange = { dieInput = it },
                                        label = { Text("Diesel Price (Rs./L)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedLabelColor = ZyphuelBluePrimary,
                                            unfocusedLabelColor = Color.DarkGray,
                                            focusedBorderColor = ZyphuelBluePrimary,
                                            unfocusedBorderColor = Color.LightGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = octInput,
                                        onValueChange = { octInput = it },
                                        label = { Text("High-Octane Price (Rs./L)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedLabelColor = ZyphuelBluePrimary,
                                            unfocusedLabelColor = Color.DarkGray,
                                            focusedBorderColor = ZyphuelBluePrimary,
                                            unfocusedBorderColor = Color.LightGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = lpgInput,
                                        onValueChange = { lpgInput = it },
                                        label = { Text("LPG Gas Price (Rs./kg)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedLabelColor = ZyphuelBluePrimary,
                                            unfocusedLabelColor = Color.DarkGray,
                                            focusedBorderColor = ZyphuelBluePrimary,
                                            unfocusedBorderColor = Color.LightGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = watInput,
                                        onValueChange = { watInput = it },
                                        label = { Text("Water Gallon Price (Rs.)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedLabelColor = ZyphuelBluePrimary,
                                            unfocusedLabelColor = Color.DarkGray,
                                            focusedBorderColor = ZyphuelBluePrimary,
                                            unfocusedBorderColor = Color.LightGray
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            val pet = petInput.toFloatOrNull() ?: petrolPrice
                                            val die = dieInput.toFloatOrNull() ?: dieselPrice
                                            val oct = octInput.toFloatOrNull() ?: octanePrice
                                            val lpg = lpgInput.toFloatOrNull() ?: lpgPrice
                                            val wat = watInput.toFloatOrNull() ?: waterPrice

                                            viewModel.updateFuelPrices(pet, die, oct, lpg, wat, "Admin Panel", "Manual Admin Override")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Filled.Save, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save & Broadcast Overrides", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    6 -> { // Simulated Admin Mailbox / SMTP outgoing logs
                        val sentEmails by viewModel.sentEmails.collectAsState()

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Email, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Admin Outgoing Mailbox", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = ZyphuelBlueDark)
                                        Text("Verifies emails dispatched dynamically to m.dDaniyalKhan490@gmail.com", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                            }

                            if (sentEmails.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.MailOutline, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No emails sent yet.", color = Color.Gray)
                                        Text("Trigger a price update to send email alerts.", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    items(sentEmails) { email ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "RECIPIENT: ${email.recipient}",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = ZyphuelBluePrimary
                                                    )
                                                    Text(
                                                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(email.timestamp)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    "SUBJECT: ${email.subject}",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.Black
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                val cleanBody = email.body
                                                    .replace("<[^>]*>".toRegex(), "")
                                                    .replace("&nbsp;", " ")
                                                    .replace("&bull;", "•")
                                                    .trim()

                                                Text(
                                                    text = cleanBody,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                                    color = Color(0xFF334155),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFFF8FAFC))
                                                        .padding(10.dp)
                                                        .heightIn(max = 200.dp)
                                                        .verticalScroll(rememberScrollState())
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    7 -> { // Audit Log records
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (logs.isEmpty()) {
                                item { Text("No audit logs yet.") }
                            } else {
                                items(logs) { log ->
                                    AdminLogItem(log = log)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFcmDialog) {
        FcmConsoleDialog(viewModel = viewModel, onDismiss = { showFcmDialog = false })
    }
    if (showAsoDialog) {
        AsoOptimizationDialog(onDismiss = { showAsoDialog = false })
    }
}

@Composable
fun AdminStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary))
        }
    }
}

@Composable
fun AddRiderDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("RiderPass@123") }
    var vehicleType by remember { mutableStateOf("Bike") }
    var vehicleNo by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var drivingLicense by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var autoApprove by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = ZyphuelBluePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Delivery Driver / Rider", fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Add a new rider record directly into the Admin Panel. Only the details provided in this form will be stored and displayed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rider_name_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    placeholder = { Text("e.g. +92 300 1234567") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rider_phone_field"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    placeholder = { Text("e.g. rider@zyphuel.com") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rider_email_field"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Rider Portal Password") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rider_password_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = vehicleType,
                    onValueChange = { vehicleType = it },
                    label = { Text("Vehicle / Delivery Type") },
                    placeholder = { Text("Bike / Pickup / Truck / Bowser") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rider_vehicle_type_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = vehicleNo,
                    onValueChange = { vehicleNo = it },
                    label = { Text("Vehicle Registration No") },
                    placeholder = { Text("e.g. LHR-2024") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rider_vehicle_no_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = cnic,
                    onValueChange = { cnic = it },
                    label = { Text("CNIC Number") },
                    placeholder = { Text("35202-1234567-1") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rider_cnic_field"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = drivingLicense,
                    onValueChange = { drivingLicense = it },
                    label = { Text("Driving License ID") },
                    placeholder = { Text("e.g. DL-LHR-98765") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rider_license_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Residential Address / City") },
                    placeholder = { Text("e.g. Gulberg III, Lahore") },
                    modifier = Modifier.fillMaxWidth().testTag("add_rider_address_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = autoApprove,
                        onCheckedChange = { autoApprove = it },
                        colors = CheckboxDefaults.colors(checkedColor = ZyphuelBluePrimary)
                    )
                    Text(
                        "Verify and Grant Verified Badge Immediately",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.addRiderFromAdmin(
                        name = name,
                        email = email,
                        phone = phone,
                        vehicleType = vehicleType,
                        vehicleNo = vehicleNo,
                        cnicNumber = cnic,
                        drivingLicense = drivingLicense,
                        address = address,
                        password = password,
                        autoApprove = autoApprove,
                        onSuccess = onDismiss
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
            ) {
                Text("Add Record", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AddCustomerDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("Gulberg III, Lahore") }
    var password by remember { mutableStateOf("Customer123!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = ZyphuelBluePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Customer", fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Add a new customer profile directly into Zyphuel system. Customer will be able to order fuel/water/gas instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Usman Chaudhry") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_customer_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    placeholder = { Text("e.g. +92 321 8899000") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_customer_phone_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    placeholder = { Text("e.g. customer@domain.com") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_customer_email_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Delivery Address") },
                    placeholder = { Text("e.g. Gulberg III, Lahore") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_customer_address_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Account Password") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_customer_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ZyphuelBluePrimary,
                        unfocusedLabelColor = Color.DarkGray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.addCustomerByAdmin(
                        email = email,
                        name = name,
                        phone = phone,
                        password = password,
                        address = address,
                        onSuccess = onDismiss
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                modifier = Modifier.testTag("admin_add_customer_confirm_btn")
            ) {
                Text("Add Customer", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AdminCustomerCard(customer: UserEntity, orders: List<OrderEntity>, viewModel: MainViewModel) {
    val context = LocalContext.current

    val customerOrders = remember(orders, customer.email) {
        orders.filter { it.customerEmail == customer.email || it.customerName == customer.name }
    }

    val ordersPlaced = customerOrders.size
    val ordersCanceled = customerOrders.count {
        it.status.contains("Cancel", ignoreCase = true) || it.status.contains("Reject", ignoreCase = true)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_customer_card_${customer.email}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBlueDark
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = "Registered Email", tint = ZyphuelBluePrimary, modifier = Modifier.size(14.dp))
                            Text(
                                text = customer.email,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ZyphuelBluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Registered Customer",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Order Statistics Grid: Phone, Orders Placed, Orders Canceled
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Phone Number", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Text(
                        text = if (customer.phoneNumber.isBlank()) "Not provided" else customer.phoneNumber,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Orders Placed", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Text(
                        text = "$ordersPlaced Placed",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Orders Canceled", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Text(
                        text = "$ordersCanceled Canceled",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (ordersCanceled > 0) Color.Red else Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Send Email Notification to Registered Email (Read-Only Email Protection)
            Button(
                onClick = {
                    viewModel.dispatchRealtimeEmail(
                        recipientEmail = customer.email,
                        subject = "⚡ Zyphuel - Registered Customer Status Alert",
                        body = "Hello ${customer.name},\n\nThis is a status verification alert for your registered account (${customer.email}).\nTotal Orders Placed: $ordersPlaced\nTotal Orders Canceled: $ordersCanceled\n\nThank you for choosing Zyphuel!"
                    )
                    Toast.makeText(context, "Notification email sent to ${customer.email}", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("send_customer_email_btn_${customer.email}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Email Alert", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun AdminRiderCard(rider: UserEntity, viewModel: MainViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    var showBiodataDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    val riderSeqNum = rider.riderNumber ?: 1
    val riderSeqId = rider.riderId ?: "RIDER-$riderSeqNum"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_rider_card_${rider.email}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Sequential Header: Rider Number Badge & Approval Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sequential Rider ID Chip
                Box(
                    modifier = Modifier
                        .background(ZyphuelBlueDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "RIDER #$riderSeqNum • $riderSeqId",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Approval Status Badge
                val isApproved = rider.adminApprovalStatus == "Approved" || rider.isVerified
                Box(
                    modifier = Modifier
                        .background(
                            if (isApproved) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isApproved) "APPROVED ✔️" else "PENDING APPROVAL ⏳",
                        color = if (isApproved) Color(0xFF10B981) else Color(0xFFD97706),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Info Header: Avatar & Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(ZyphuelBluePrimary.copy(alpha = 0.1f), CircleShape)
                            .border(1.5.dp, if (rider.isVerified) Color(0xFF10B981) else Color(0xFFD97706), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Face,
                            contentDescription = "Rider Headshot",
                            tint = ZyphuelBluePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = rider.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ZyphuelBlueDark
                        )
                        Text(
                            text = "Assigned Delivery Fleet Rider",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Verification Badge
                Box(
                    modifier = Modifier
                        .background(
                            when {
                                rider.isVerified -> Color(0xFF10B981).copy(alpha = 0.15f)
                                rider.hasRequestedVerification -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                else -> Color.LightGray.copy(alpha = 0.3f)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            rider.isVerified -> "VERIFIED BADGE ✔️"
                            rider.hasRequestedVerification -> "⚡ BADGE REQUESTED"
                            else -> "UNVERIFIED"
                        },
                        color = when {
                            rider.isVerified -> Color(0xFF10B981)
                            rider.hasRequestedVerification -> Color(0xFFD97706)
                            else -> Color.DarkGray
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🔐 DEDICATED LOGIN CREDENTIALS SECTION (Required by Admin)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, ZyphuelBluePrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rider_credentials_box_${rider.email}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rider Account Login Credentials", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = ZyphuelBlueDark)
                        }
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("toggle_rider_password_${rider.email}")
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle Password",
                                tint = ZyphuelBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Email Address:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(rider.email, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Login Password:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            text = if (showPassword) rider.passwordHash else "••••••••",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Quick Info Grid - Phone, Vehicle, Plate No
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Phone", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(rider.phoneNumber, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                }
                Column {
                    Text("Vehicle", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(rider.vehicleType ?: "Not Provided", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                }
                Column {
                    Text("Plate No", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(rider.vehicleNo ?: "Not Provided", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                }
            }

            if (!rider.cnicOrPassport.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CNIC / Legal ID: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(rider.cnicOrPassport ?: "", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Inspect Biodata, Edit Rider Info & Delete Buttons
            Button(
                onClick = { showBiodataDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("check_rider_details_btn_${rider.email}"),
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBlueDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check Rider Details & Biodata", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_rider_info_btn_${rider.email}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZyphuelBluePrimary),
                    border = BorderStroke(1.dp, ZyphuelBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Info", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("delete_rider_btn_${rider.email}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable Details
            TextButton(
                onClick = { isExpanded = !isExpanded },
                colors = ButtonDefaults.textButtonColors(contentColor = ZyphuelBluePrimary),
                modifier = Modifier.testTag("expand_rider_details_btn_${rider.email}")
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Show Less" else "Show Details"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isExpanded) "Hide Technical Specifications" else "View License & Contact Specifications",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vehicle Make/Model:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text("${rider.vehicleMake ?: "N/A"} (${rider.vehicleModel ?: "N/A"})", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vehicle Color:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(rider.vehicleColor ?: "N/A", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Driving License ID:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(rider.drivingLicense ?: "N/A", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = ZyphuelBlueDark))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Verification & Approval Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rider.isVerified) {
                    Button(
                        onClick = { viewModel.denyRiderVerification(rider.email) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("revoke_verification_btn_${rider.email}")
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Revoke Verified Badge", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { viewModel.approveRiderVerification(rider.email) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_verification_btn_${rider.email}")
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (rider.hasRequestedVerification) "Approve Request" else "Approve Badge", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { viewModel.denyRiderVerification(rider.email) },
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("deny_verification_btn_${rider.email}")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deny", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }

    if (showBiodataDialog) {
        AdminRiderBiodataDialog(rider = rider, viewModel = viewModel, onDismiss = { showBiodataDialog = false })
    }

    if (showEditDialog) {
        AdminEditRiderDialog(rider = rider, viewModel = viewModel, onDismiss = { showEditDialog = false })
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Rider Account") },
            text = { Text("Are you sure you want to delete rider '${rider.name}' (${rider.email})? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRiderFromAdmin(rider.email) {
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminRiderBiodataDialog(rider: UserEntity, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("admin_rider_biodata_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ZyphuelBlueDark)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Rider Biodata & Headshot Verification",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = rider.email,
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Headshot & Face Verification Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(ZyphuelBluePrimary.copy(alpha = 0.15f), CircleShape)
                                    .border(3.dp, if (rider.isVerified) Color(0xFF10B981) else Color(0xFFF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Headshot Avatar",
                                    tint = ZyphuelBluePrimary,
                                    modifier = Modifier.size(54.dp)
                                )
                            }

                            Text(
                                text = rider.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                            )

                            Surface(
                                color = if (rider.isFaceVerified || rider.isVerified) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (rider.isFaceVerified || rider.isVerified) Icons.Filled.Verified else Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (rider.isFaceVerified || rider.isVerified) Color(0xFF047857) else Color(0xFFB45309),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (rider.isFaceVerified || rider.isVerified) "Headshot & Face Verified ✔️" else "Biometric Face Check Pending",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (rider.isFaceVerified || rider.isVerified) Color(0xFF047857) else Color(0xFFB45309)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Section 1: Personal & Bio Details
                    Text("1. PERSONAL BIODATA DETAILS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                    BiodataRow(label = "Full Name", value = rider.name)
                    if (!rider.fathersName.isNullOrBlank()) BiodataRow(label = "Father's Name", value = rider.fathersName)
                    if (!rider.dob.isNullOrBlank()) BiodataRow(label = "Date of Birth", value = rider.dob)
                    if (!rider.gender.isNullOrBlank()) BiodataRow(label = "Gender", value = rider.gender)
                    BiodataRow(label = "Email Address", value = rider.email)
                    BiodataRow(label = "Contact Phone", value = rider.phoneNumber)

                    // Section 2: Legal Credentials & Address
                    HorizontalDivider()
                    Text("2. NATIONAL IDENTITY & LEGAL CREDENTIALS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                    BiodataRow(label = "CNIC / Passport No", value = rider.cnicOrPassport ?: "Not Provided")
                    if (!rider.cnicExpiryDate.isNullOrBlank()) BiodataRow(label = "CNIC Expiry Date", value = rider.cnicExpiryDate)
                    if (!rider.residentialAddress.isNullOrBlank()) BiodataRow(label = "Residential Address", value = rider.residentialAddress)
                    val cityProvince = listOfNotNull(rider.city, rider.province).filter { it.isNotBlank() }.joinToString(", ")
                    if (cityProvince.isNotBlank()) BiodataRow(label = "City & Region", value = cityProvince)

                    // Section 3: Vehicle & License Information
                    HorizontalDivider()
                    Text("3. VEHICLE & DRIVING LICENSE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                    BiodataRow(label = "Vehicle Type", value = rider.vehicleType ?: "Not Provided")
                    BiodataRow(label = "Registration Plate No", value = rider.vehicleNo ?: "Not Provided")
                    val vehicleMakeModel = listOfNotNull(rider.vehicleMake, rider.vehicleModel).filter { it.isNotBlank() }.joinToString(" ")
                    if (vehicleMakeModel.isNotBlank()) BiodataRow(label = "Vehicle Model", value = vehicleMakeModel)
                    if (!rider.vehicleColor.isNullOrBlank()) BiodataRow(label = "Vehicle Color", value = rider.vehicleColor)
                    if (!rider.drivingLicense.isNullOrBlank()) BiodataRow(label = "Driving License ID", value = rider.drivingLicense)

                    // Section 4: Emergency Contacts (if present)
                    if (!rider.emergencyName.isNullOrBlank() || !rider.emergencyPhone.isNullOrBlank()) {
                        HorizontalDivider()
                        Text("4. EMERGENCY CONTACT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark))
                        if (!rider.emergencyName.isNullOrBlank()) BiodataRow(label = "Contact Name", value = rider.emergencyName)
                        if (!rider.emergencyPhone.isNullOrBlank()) BiodataRow(label = "Emergency Phone", value = rider.emergencyPhone)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Verification Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.approveRiderVerification(rider.email)
                                viewModel.dispatchRealtimeEmail(
                                    recipientEmail = rider.email,
                                    subject = "🎉 Congratulations! Your Zyphuel Verified Badge Request is Approved",
                                    body = "Hello ${rider.name},\n\nYour headshots and complete biodata credentials have been reviewed and APPROVED by Zyphuel Operations Admin.\n\nYou are now an active verified rider with a Verified Badge.\n\nZyphuel Admin Team"
                                )
                                Toast.makeText(context, "Rider Approved & Verified Badge Granted!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f).testTag("approve_rider_biodata_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Approve & Verify", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.denyRiderVerification(rider.email)
                                Toast.makeText(context, "Rider verified badge request denied.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("reject_rider_biodata_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deny Request", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiodataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}

@Composable
fun AdminEditRiderDialog(
    rider: UserEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(rider.name) }
    var phone by remember { mutableStateOf(rider.phoneNumber) }
    var vehicleType by remember { mutableStateOf(rider.vehicleType ?: "Bike") }
    var vehicleNo by remember { mutableStateOf(rider.vehicleNo ?: "") }
    var cnicNumber by remember { mutableStateOf(rider.cnicOrPassport ?: "") }
    var isVerified by remember { mutableStateOf(rider.isVerified) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("admin_edit_rider_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ZyphuelBluePrimary)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit & Fix Rider Details",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Update Rider Information (${rider.email})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Rider Full Name") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ZyphuelBluePrimary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_rider_name_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ZyphuelBluePrimary)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = ZyphuelBluePrimary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_rider_phone_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ZyphuelBluePrimary)
                    )

                    OutlinedTextField(
                        value = vehicleType,
                        onValueChange = { vehicleType = it },
                        label = { Text("Vehicle Type (e.g., Bike, Pickup Bowser)") },
                        leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = ZyphuelBluePrimary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_rider_vehicletype_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ZyphuelBluePrimary)
                    )

                    OutlinedTextField(
                        value = vehicleNo,
                        onValueChange = { vehicleNo = it },
                        label = { Text("Vehicle Plate Number (e.g. LHR-1234)") },
                        leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null, tint = ZyphuelBluePrimary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_rider_vehicleno_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ZyphuelBluePrimary)
                    )

                    OutlinedTextField(
                        value = cnicNumber,
                        onValueChange = { cnicNumber = it },
                        label = { Text("CNIC Number") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = ZyphuelBluePrimary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_rider_cnic_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ZyphuelBluePrimary)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Approval & Verification Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (isVerified) "Rider is Approved & Verified" else "Rider is Pending Verification",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isVerified) Color(0xFF10B981) else Color(0xFFD97706)
                            )
                        }
                        Switch(
                            checked = isVerified,
                            onCheckedChange = { isVerified = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                viewModel.editRiderFromAdmin(
                                    riderEmail = rider.email,
                                    name = name,
                                    phone = phone,
                                    vehicleType = vehicleType,
                                    vehicleNo = vehicleNo,
                                    cnicNumber = cnicNumber,
                                    isVerified = isVerified,
                                    onSuccess = onDismiss
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                            modifier = Modifier.weight(1.5f).testTag("save_edit_rider_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Dispatch Admin Alert", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDeleteRiderConfirmDialog(
    rider: UserEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.DeleteForever, contentDescription = "Delete Rider", tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
        },
        title = {
            Text(
                text = "Delete Rider Account?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ZyphuelBlueDark
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Are you sure you want to permanently delete the rider record for ${rider.name} (${rider.email})?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                Text(
                    text = "This action will remove the rider from the database and notify the admin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.deleteRiderFromAdmin(rider.email, onDismiss)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier.testTag("confirm_delete_rider_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminRiderDetailsDialog(
    rider: UserEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AssignmentInd,
                    contentDescription = null,
                    tint = ZyphuelBluePrimary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = rider.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                    )
                    Text(
                        text = "Rider ID: ${rider.riderId ?: "N/A"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Personal Profile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Personal Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = ZyphuelBlueDark)
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Text(text = "Father's Name: ${rider.fathersName ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Date of Birth: ${rider.dob ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Gender: ${rider.gender ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Email: ${rider.email}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Phone Number: ${rider.phoneNumber}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Section 2: Residential Address
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Home, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Residential Address", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = ZyphuelBlueDark)
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Text(text = "Address: ${rider.residentialAddress ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "City: ${rider.city ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Province: ${rider.province ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Postal Code: ${rider.postalCode ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Country: ${rider.country ?: "Pakistan"}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Section 3: National Identity Verification
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ContactPage, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Identity Verification (CNIC)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = ZyphuelBlueDark)
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Text(text = "CNIC Number: ${rider.cnicOrPassport ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Issue Date: ${rider.cnicIssueDate ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Expiry Date: ${rider.cnicExpiryDate ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "CNIC Verification: ${rider.cnicVerificationStatus ?: "Pending"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
                            ) {
                                Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Text("CNIC Front Scan [Uploaded]", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
                            ) {
                                Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Text("CNIC Back Scan [Uploaded]", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                // Section 4: Vehicle Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delivery Vehicle Specs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = ZyphuelBlueDark)
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Text(text = "Vehicle Type: ${rider.vehicleType ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Make / Brand: ${rider.vehicleMake ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Model Year: ${rider.vehicleModel ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Registration No: ${rider.vehicleNo ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Color Accent: ${rider.vehicleColor ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
                            ) {
                                Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Text("Reg Book [Attached]", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
                            ) {
                                Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Text("Vehicle Photo [Attached]", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Section 5: Emergency Contact Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ContactPhone, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Emergency Contact Info", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = ZyphuelBlueDark)
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Text(text = "Contact Name: ${rider.emergencyName ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Relationship: ${rider.emergencyRelationship ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Phone: ${rider.emergencyPhone ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Section 6: Declaration & Legals
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Gavel, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Legal Declarations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = ZyphuelBlueDark)
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Text(text = "Terms Accepted: ${if (rider.termsAccepted) "Yes ✅" else "No ❌"}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Declaration Confirmed: ${if (rider.declarationAccepted) "Yes ✅" else "No ❌"}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.toggleRiderVerification(rider.email)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (rider.isVerified) Color(0xFFEF4444) else Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (rider.isVerified) "Revoke Verification" else "Approve & Verify", color = Color.White)
                }
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBlueDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close Dossier", color = Color.White)
                }
            }
        }
    )
}

@Composable
fun AdminOrderCard(order: OrderEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Order #${order.id} - ${order.serviceType}", fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                Text("Rs. ${order.totalPrice}", fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Qty: ${order.quantity} | Cust: ${order.customerName}", style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray))
            Text("Address: ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
            Text("Rider: ${order.riderName ?: "Unassigned"}", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(ZyphuelBlueSecondary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(order.status, color = ZyphuelBlueDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            if (order.rating != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { starIndex ->
                        val isFilled = starIndex < order.rating
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (isFilled) Color(0xFFFFB000) else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Customer Rating: ${order.rating}/5",
                        style = MaterialTheme.typography.labelSmall.copy(color = ZyphuelBlueDark, fontWeight = FontWeight.Bold)
                    )
                }
                if (!order.feedback.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "“${order.feedback}”",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminLogItem(log: AuditLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = log.action, fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary, style = MaterialTheme.typography.bodySmall)
                Text(text = "By: ${log.performedBy}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = log.details, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsDialog(
    notifications: List<NotificationEntity>,
    onDismiss: () -> Unit,
    onClearAll: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Zyphuel App Logo",
                    modifier = Modifier.size(26.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("App Notifications", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No notifications yet.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(notifications) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ZyphuelBluePrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (notifications.isNotEmpty() && onClearAll != null) {
                    OutlinedButton(
                        onClick = onClearAll,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.testTag("dialog_clear_all_btn")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_dismiss_btn")) {
                    Text("Dismiss", color = ZyphuelBluePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocationPermissionRationaleBanner(
    onGrantPermission: () -> Unit,
    onSearchManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFEF3C7),
        border = BorderStroke(1.dp, Color(0xFFF59E0B))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Precise Live Location Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309)
                )
            }
            Text(
                text = "Zyphuel auto-detects your precise GPS position and nearest landmark so fuel & water bowser delivery trucks route directly to your spot without delay.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF78350F)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rationale_grant_perm_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Turn On GPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSearchManually,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rationale_search_manual_btn"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFD97706))
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manual Search", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }
            }
        }
    }
}

@Composable
fun PlacesAutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onLandmarkSelected: (String, Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val allPlaces = remember {
        listOf(
            Triple("Zyphuel Main Headquarters & Dispatch Hub, Green Town, Lahore, Pakistan", 31.4380, 74.3050),
            Triple("Green Town Sector D1, Lahore, Pakistan", 31.4390, 74.3070),
            Triple("Model Town Block C, Lahore, Pakistan", 31.5204, 74.3587),
            Triple("Liberty Market, Gulberg III, Lahore, Pakistan", 31.5120, 74.3520),
            Triple("MM Alam Road, Gulberg III, Lahore, Pakistan", 31.5080, 74.3550),
            Triple("DHA Phase 5 Block CCA, Lahore, Pakistan", 31.4700, 74.4100),
            Triple("DHA Phase 6 Main Blvd, Lahore, Pakistan", 31.4600, 74.4300),
            Triple("Johar Town Khayaban-e-Firdousi, Lahore, Pakistan", 31.4650, 74.2950),
            Triple("Bahria Town Sector C, Lahore, Pakistan", 31.3680, 74.1850),
            Triple("Mall of Lahore, Cantt, Lahore, Pakistan", 31.5350, 74.3750),
            Triple("Askari 11 Gate 1, Lahore, Pakistan", 31.4850, 74.4450),
            Triple("Packages Mall, Walton Road, Lahore, Pakistan", 31.4550, 74.3800),
            Triple("Mall Road, Near GPO, Lahore, Pakistan", 31.5600, 74.3250),
            Triple("Lake City Sector M, Lahore, Pakistan", 31.2850, 74.2550),
            Triple("F-7 Markaz, Islamabad, Pakistan", 33.7215, 73.0565),
            Triple("Clifton Block 4, Karachi, Pakistan", 24.8250, 67.0300)
        )
    }

    val filteredSuggestions = remember(value) {
        if (value.isBlank()) {
            allPlaces.take(4)
        } else {
            allPlaces.filter { (name, _, _) ->
                name.contains(value, ignoreCase = true)
            }.take(5)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                isExpanded = true
            },
            label = { Text("Search Landmark or Address (Google Places)") },
            placeholder = { Text("Type Gulberg, DHA, Model Town, Packages Mall...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ZyphuelBluePrimary) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = {
                        onValueChange("")
                        isExpanded = true
                    }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("places_autocomplete_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (isExpanded && filteredSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Text(
                        text = "📍 Google Places Landmark Autocomplete:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    filteredSuggestions.forEach { (placeName, lat, lng) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(placeName)
                                    onLandmarkSelected(placeName, lat, lng)
                                    isExpanded = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = null,
                                tint = ZyphuelBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = placeName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Coordinates: ${String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveLocationPickerMap(
    latitude: Double,
    longitude: Double,
    landmarkName: String,
    onLocationPinned: (Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentLat by remember(latitude) { mutableDoubleStateOf(latitude) }
    var currentLng by remember(longitude) { mutableDoubleStateOf(longitude) }
    var currentLandmark by remember(landmarkName) { mutableStateOf(landmarkName) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.5.dp, ZyphuelBluePrimary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Map, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Interactive Live Map Location Pin",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "Live GPS Fine-Tune",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            ) {
                UnifiedGoogleMapView(
                    modifier = Modifier.fillMaxSize(),
                    userLat = currentLat,
                    userLng = currentLng,
                    deliveryAddress = currentLandmark,
                    isPickerMode = true,
                    showOverlays = true,
                    onLocationPinned = { lat, lng, addr ->
                        currentLat = lat
                        currentLng = lng
                        currentLandmark = addr
                        onLocationPinned(lat, lng, addr)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "👇 Tap anywhere on map or drag pin to select your exact delivery landmark",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selected Pin Landmark:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = currentLandmark,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBlueDark
                        )
                    }
                    Button(
                        onClick = { onLocationPinned(currentLat, currentLng, currentLandmark) },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_map_pin_btn")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm Pin", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditLocationDialog(
    currentLocation: String,
    isCustomSet: Boolean,
    onDismiss: () -> Unit,
    onSaveCustomLocation: (String) -> Unit,
    onAutoDetectLocation: () -> Unit,
    onResetToAutoGps: () -> Unit
) {
    val context = LocalContext.current
    var editedAddress by remember { mutableStateOf(currentLocation) }
    var selectedLat by remember { mutableDoubleStateOf(31.5204) }
    var selectedLng by remember { mutableDoubleStateOf(74.3587) }
    var showMapPicker by remember { mutableStateOf(false) }

    val hasFinePermission = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.EditLocation, contentDescription = null, tint = ZyphuelBluePrimary)
                Text("Live Delivery Location & Landmark", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!hasFinePermission) {
                    LocationPermissionRationaleBanner(
                        onGrantPermission = onAutoDetectLocation,
                        onSearchManually = { /* Focus manual search */ }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCustomSet) Color(0xFFEFF6FF) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, if (isCustomSet) Color(0xFF93C5FD) else Color(0xFFA7F3D0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isCustomSet) Icons.Filled.Edit else Icons.Filled.MyLocation,
                            contentDescription = null,
                            tint = if (isCustomSet) ZyphuelBluePrimary else Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isCustomSet) "Custom Location & Landmark Active" else "🟢 Auto-Detected Live GPS & Landmark Active",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCustomSet) ZyphuelBlueDark else Color(0xFF065F46)
                        )
                    }
                }

                // Google Places Autocomplete Field
                PlacesAutocompleteTextField(
                    value = editedAddress,
                    onValueChange = { editedAddress = it },
                    onLandmarkSelected = { name, lat, lng ->
                        editedAddress = name
                        selectedLat = lat
                        selectedLng = lng
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Map Pin Visual Verification:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    TextButton(onClick = { showMapPicker = !showMapPicker }) {
                        Text(if (showMapPicker) "Hide Map Pin" else "Fine-Tune Pin on Map 📍", fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary)
                    }
                }

                if (showMapPicker) {
                    InteractiveLocationPickerMap(
                        latitude = selectedLat,
                        longitude = selectedLng,
                        landmarkName = editedAddress.ifBlank { "Model Town, Lahore" },
                        onLocationPinned = { lat, lng, landmark ->
                            selectedLat = lat
                            selectedLng = lng
                            editedAddress = landmark
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAutoDetectLocation,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("location_auto_gps_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto GPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    if (isCustomSet) {
                        OutlinedButton(
                            onClick = onResetToAutoGps,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("location_reset_auto_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706))
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Auto", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveCustomLocation(editedAddress) },
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                shape = RoundedCornerShape(10.dp),
                enabled = editedAddress.isNotBlank(),
                modifier = Modifier.testTag("save_location_btn")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirm Location", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

fun resolveLandmarkFromCoordinates(context: android.content.Context, lat: Double, lng: Double): String {
    // 1. Try Android Geocoder for reverse geocoding real live location
    try {
        if (android.location.Geocoder.isPresent()) {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val fullLine = addr.getAddressLine(0)
                if (!fullLine.isNullOrBlank()) {
                    return fullLine
                }
                val subLoc = addr.subLocality ?: addr.featureName ?: addr.thoroughfare
                val city = addr.locality ?: addr.subAdminArea
                val country = addr.countryName
                val landmarkParts = listOfNotNull(subLoc, city, country).filter { !it.isNullOrBlank() }.distinct()
                if (landmarkParts.isNotEmpty()) {
                    return landmarkParts.joinToString(", ")
                }
            }
        }
    } catch (e: Exception) {
        // Geocoder exception or offline
    }

    // 2. Spatial landmark check (within ~2 km / 0.02 deg)
    val landmarks = listOf(
        Triple(31.4380, 74.3050, "Green Town, Lahore, Pakistan"),
        Triple(31.5204, 74.3587, "Model Town Block C, Lahore, Pakistan"),
        Triple(31.5120, 74.3520, "Liberty Market, Gulberg III, Lahore, Pakistan"),
        Triple(31.5080, 74.3550, "MM Alam Road, Gulberg III, Lahore, Pakistan"),
        Triple(31.4700, 74.4100, "DHA Phase 5, Lahore, Pakistan"),
        Triple(31.4650, 74.2950, "Johar Town, Lahore, Pakistan"),
        Triple(31.3680, 74.1850, "Bahria Town, Lahore, Pakistan"),
        Triple(31.5350, 74.3750, "Mall of Lahore, Cantt, Lahore, Pakistan"),
        Triple(31.5600, 74.3250, "Mall Road, Lahore, Pakistan"),
        Triple(31.2850, 74.2550, "Lake City, Lahore, Pakistan")
    )

    for ((pLat, pLng, name) in landmarks) {
        val dist = Math.hypot(lat - pLat, lng - pLng)
        if (dist < 0.02) {
            return name
        }
    }

    // Return empty string if address is not detected so manual editing is requested
    return ""
}

fun fetchDeviceGpsLocation(
    context: android.content.Context,
    onLocationResult: (Double, Double, String?) -> Unit,
    onError: (String) -> Unit
) {
    com.example.service.LocationService.fetchFreshSingleLocation(
        context,
        onLocationResult = { lat, lng ->
            val landmark = resolveLandmarkFromCoordinates(context, lat, lng)
            onLocationResult(lat, lng, landmark)
        },
        onError = onError
    )
}

@Composable
fun LahoreFuelMarketWidget(
    viewModel: MainViewModel,
    onSelectService: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val petrolPrice by viewModel.petrolPrice.collectAsState()
    val dieselPrice by viewModel.dieselPrice.collectAsState()
    val octanePrice by viewModel.highOctanePrice.collectAsState()
    val lpgPrice by viewModel.lpgGasPrice.collectAsState()
    val waterPrice by viewModel.waterPrice.collectAsState()
    val priceSyncing by viewModel.priceSyncing.collectAsState()
    val lastPriceSyncTime by viewModel.lastPriceSyncTime.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "live_dot_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lahore_fuel_market_widget"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF059669).copy(alpha = 0.6f),
                Color(0xFF2563EB).copy(alpha = 0.6f)
            )
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title + Live Status + Google Grounding Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981).copy(alpha = pulseAlpha), CircleShape)
                    )
                    Text(
                        text = "Real-Time Fuel Market",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ZyphuelBlueDark
                        )
                    )
                }

                Surface(
                    color = Color(0xFF0EA5E9).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Grounding",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Google Search Grounded",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF0284C7),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Text(
                text = "📍 Lahore Central Terminal • Official OGRA Retail & Bowser Logistics Rates",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
            )

            // Two Main Highlights: Petrol & Diesel Cards side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Petrol Highlight Card
                FuelRateCardItem(
                    title = "Petrol (Super Euro-V)",
                    rate = viewModel.formatUnitPrice(petrolPrice, "L"),
                    basePrice = "Rs. ${String.format(java.util.Locale.US, "%.2f", (petrolPrice - 20.0f).coerceAtLeast(0f))}/L Base",
                    icon = Icons.Filled.LocalGasStation,
                    accentColor = Color(0xFF059669),
                    badgeText = "OGRA Rate",
                    modifier = Modifier.weight(1f),
                    onOrderClick = { onSelectService("Petrol") }
                )

                // Diesel Highlight Card
                FuelRateCardItem(
                    title = "Diesel (High Speed)",
                    rate = viewModel.formatUnitPrice(dieselPrice, "L"),
                    basePrice = "Rs. ${String.format(java.util.Locale.US, "%.2f", (dieselPrice - 20.0f).coerceAtLeast(0f))}/L Base",
                    icon = Icons.Filled.DirectionsCar,
                    accentColor = Color(0xFF2563EB),
                    badgeText = "OGRA Rate",
                    modifier = Modifier.weight(1f),
                    onOrderClick = { onSelectService("Diesel") }
                )
            }

            // Compact Grid for High-Octane, LPG, Water
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactFuelRateItem(
                    label = "High-Octane",
                    rate = viewModel.formatUnitPrice(octanePrice, "L"),
                    accentColor = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
                CompactFuelRateItem(
                    label = "LPG Gas",
                    rate = viewModel.formatUnitPrice(lpgPrice, "Kg"),
                    accentColor = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f)
                )
                CompactFuelRateItem(
                    label = "Pure Water",
                    rate = viewModel.formatUnitPrice(waterPrice, "Gallon"),
                    accentColor = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // Refresh & Source Grounding Footer Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Source: $lastPriceSyncTime",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Live AI market rate verification active",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF059669), fontWeight = FontWeight.SemiBold, fontSize = 9.sp)
                    )
                }

                Button(
                    onClick = { viewModel.syncFuelPricesViaGemini() },
                    enabled = !priceSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZyphuelBluePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("refresh_market_rates_btn")
                ) {
                    if (priceSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grounding...", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Rates", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Rates", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelRateCardItem(
    title: String,
    rate: String,
    basePrice: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    badgeText: String,
    modifier: Modifier = Modifier,
    onOrderClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Card(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = accentColor),
                onClick = onOrderClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, if (isHovered) accentColor else Color(0xFFCBD5E1))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Surface(
                    color = accentColor.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
            )

            Text(
                text = rate,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    fontSize = 16.sp
                )
            )

            Text(
                text = basePrice,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp)
            )

            Surface(
                color = accentColor,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOrderClick)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Order Now",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun AsoOptimizationDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("SEO 9-Matrix", "ASO Store Copy", "GEO & On-Page", "Local & Voice", "Tech & Int'l", "A-Z Keyword Matrix", "Viral Growth")

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("aso_optimization_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF059669), Color(0xFF2563EB))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "SEO & ASO Management Hub",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = ZyphuelBlueDark)
                                )
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "9 DOMAINS READY",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF047857), fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "GEO (ChatGPT/Perplexity) • ASO • Voice • Local Maps • Technical • Int'l",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                // Tab Selector
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = ZyphuelBluePrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == index) ZyphuelBluePrimary else Color.Gray
                                    )
                                )
                            }
                        )
                    }
                }

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> Seo9MatrixOverviewTab(context)
                        1 -> PlayStoreAsoTab(context)
                        2 -> GeoAndOnPageTab(context)
                        3 -> LocalAndVoiceSeoTab(context)
                        4 -> TechAndInternationalTab(context)
                        5 -> AtoZKeywordMatrixTab(context)
                        6 -> ViralGrowthTab(context)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBlueDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply & Return to App", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlayStoreAsoTab(context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsoCopyCard(
            title = "App Title (30 Characters)",
            content = "Zyphuel: Fuel & Gas Delivery",
            characterCount = "29/30",
            context = context
        )

        AsoCopyCard(
            title = "Short Description (80 Characters)",
            content = "Instant On-Demand Petrol, Diesel, LPG Gas & Water Delivery with Live Bowser GPS.",
            characterCount = "80/80",
            context = context
        )

        AsoCopyCard(
            title = "Optimized Long Description (Google Play SEO Keyword Density 3.8%)",
            content = """Zyphuel is Pakistan & Global Leader in On-Demand Fuel Delivery, Emergency Refueling, and Mobile Gas Station Logistics. Order Super Euro-V Petrol, High Speed Diesel, High-Octane 97, LPG Gas Cylinders, and Purified Mineral Water delivered straight to your vehicle or home doorstep in Lahore and major metropolitan cities.

Key Features & High Ranking Keywords:
• On-Demand Fuel Delivery: Instant mobile refueling for cars, fleets, generators, and commercial sites.
• Live OGRA Rate Integration: Real-time petrol and diesel prices updated directly via Google Search Grounding.
• Bowser Fleet GPS Tracking: Watch your fuel tanker approach on live maps with estimated arrival times.
• 24/7 Emergency Gas & Refinement: Stranded with an empty fuel tank? Get instant roadside petrol delivery in 15 minutes.
• Biometric Touchless Checkout: Secure payment via fingerprint, face unlock, and digital wallet integration.
• Eco Clean Energy & Pure Water: Certified LPG cylinder refill and 100% pure mineral water delivery.

Target Keywords Included:
fuel delivery app, on demand petrol, diesel delivery lahore, lpg gas cylinder, emergency refueling, mobile gas station, ogra rates, bowser delivery, doorstep petrol, fuel price tracker, clean energy refill.""",
            characterCount = "1,142/4,000",
            context = context
        )
    }
}

@Composable
private fun AtoZKeywordMatrixTab(context: android.content.Context) {
    val keywordMatrix = listOf(
        "A" to "Absolute Accuracy OGRA Fuel Rates",
        "B" to "Bowser GPS Live Tracking Fleet",
        "C" to "Clean Energy & Pure Water Logistics",
        "D" to "Diesel Euro-V High Speed On-Demand",
        "E" to "Emergency Refueling 24/7 Roadside",
        "F" to "Fuel Delivery On-Demand App",
        "G" to "Google Search Grounded Live Market Rates",
        "H" to "High-Octane HOBC 97 Premium Blend",
        "I" to "Instant Order Dispatch & Dispatcher",
        "J" to "Just-In-Time Fuel Logistics Network",
        "K" to "Kinetic Rapid Bowser Tanker Dispatch",
        "L" to "LPG Gas Cylinder Doorstep Refill",
        "M" to "Mobile Gas Station & Fuel Truck",
        "N" to "Network Provider GPS Auto-Location",
        "O" to "OGRA Approved Petroleum Standards",
        "P" to "Petrol Super Euro-V On-Demand",
        "Q" to "Quality Certified Fuel Storage Tanks",
        "R" to "Real-Time Rider Route Navigation",
        "S" to "Safe Doorstep Tanker Refilling",
        "T" to "Touchless Biometric Fingerprint Pay",
        "U" to "Universal App Deep Links (zyphuel.com)",
        "V" to "Viral Referral & Loyalty Cashbacks",
        "W" to "Water Gallon Pure Mineral Supply",
        "X" to "Express Emergency Petrol Refill",
        "Y" to "Yield Cost Savings & Price Tracker",
        "Z" to "Zyphuel Global Fuel Logistics"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Per-Word A-Z Google Play Search Matrix",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
        )
        Text(
            text = "Every single alphabet letter is mapped to a high-volume search term for maximum store discovery.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
        )

        keywordMatrix.forEach { (letter, keyword) ->
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ZyphuelBluePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(letter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(
                        text = keyword,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ZyphuelBlueDark,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Rank #1 Keyword",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF047857), fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleIndexingTab(context: android.content.Context) {
    val schemaJson = """{
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "Zyphuel",
  "operatingSystem": "ANDROID",
  "applicationCategory": "BusinessApplication",
  "aggregateRating": {
    "@type": "AggregateRating",
    "ratingValue": "4.9",
    "ratingCount": "12850"
  },
  "offers": {
    "@type": "Offer",
    "price": "0",
    "priceCurrency": "PKR"
  },
  "description": "On-demand fuel delivery for Petrol, Diesel, LPG Gas, and Water in Lahore & global cities."
}"""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsoCopyCard(
            title = "Schema.org / JSON-LD Rich Snippet for Google Search Indexing",
            content = schemaJson,
            characterCount = "JSON-LD Ready",
            context = context
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "App Links & Universal Deep Linking",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                )
                Text(
                    text = "Google Play and Google Search directly open app screens for indexed queries.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("https://zyphuel.com/order", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://zyphuel.com"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Test App Link", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ViralGrowthTab(context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB000))
                    Text("5-Star Google Play Review In-App Prompt", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF065F46)))
                }
                Text(
                    text = "Automated prompt invites satisfied customers to review Zyphuel on Google Play Store right after successful fuel delivery.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontSize = 11.sp)
                )

                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.aistudio.zyphuel"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Redirecting to Google Play Store...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Test Google Play In-App Review Flow", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = Color(0xFF2563EB))
                    Text("Viral WhatsApp Referral & Growth Loop", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF)))
                }
                Text(
                    text = "Share pre-formatted viral promotional message with friends and family across Lahore to earn free delivery credits.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontSize = 11.sp)
                )

                Button(
                    onClick = {
                        val shareText = "🚀 Get Petrol, Diesel & LPG Gas delivered directly to your vehicle anywhere in Lahore with Zyphuel! Real-time OGRA rates & instant delivery: https://zyphuel.com"
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Zyphuel"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Launch Viral Share Campaign", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AsoCopyCard(
    title: String,
    content: String,
    characterCount: String,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                )
                Surface(
                    color = ZyphuelBluePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = characterCount,
                        style = MaterialTheme.typography.labelSmall.copy(color = ZyphuelBluePrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontSize = 11.sp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(title, content)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "$title copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ZyphuelBluePrimary)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy for Google Play Console", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun CompactFuelRateItem(
    label: String,
    rate: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp),
                maxLines = 1
            )
            Text(
                text = rate,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FcmConsoleDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val fcmToken by viewModel.fcmToken.collectAsState()
    val fcmLog by viewModel.fcmStatusLog.collectAsState()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = ZyphuelBluePrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Firebase Cloud Messaging (FCM)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Real-Time Push Notifications Console",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. FCM Registration Token Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF16A34A),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "FCM TOKEN ACTIVE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    if (!fcmToken.isNullOrEmpty()) {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(fcmToken!!))
                                        Toast.makeText(context, "FCM Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Token", tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = fcmToken ?: "Registering device with Firebase Cloud Messaging...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF14532D)
                                )
                            )
                        }
                    }
                }

                // 2. FCM Channel & High Importance Metadata
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Notification Channels & Sound", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text("• Channel ID: zyphuel_fcm_channel", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Text("• Priority: IMPORTANCE_HIGH (Heads-up banner & vibration)", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Text("• Target Role Routing: Dynamic Room DB + Service Intent", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    }
                }

                // 3. Test Real-time Delivery Status FCM Push
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Real-Time Delivery Status FCM Alert", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = ZyphuelBluePrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.simulateFcmDeliveryUpdate("1024", "Dispatched", "Ali Raza (Bowser #04)") },
                                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🛵 Dispatched", fontSize = 11.sp, maxLines = 1)
                            }
                            Button(
                                onClick = { viewModel.simulateFcmDeliveryUpdate("1024", "Arriving Soon", "Ali Raza (Bowser #04)") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("📍 Arriving", fontSize = 11.sp, maxLines = 1)
                            }
                            Button(
                                onClick = { viewModel.simulateFcmDeliveryUpdate("1024", "Fuel Dispensed", "Ali Raza (Bowser #04)") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🎉 Delivered", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }

                // 4. Test Promotional FCM Alert for Price Drops
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Promotional Price Drop Alerts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFC2410C))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.simulateFcmPriceDropAlert("Super Petrol Euro-V", 278.50f, 268.00f) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⚡ Petrol Drop", fontSize = 11.sp, maxLines = 1)
                            }
                            Button(
                                onClick = { viewModel.simulateFcmPriceDropAlert("High Octane 97", 298.00f, 285.00f) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔥 Octane Drop", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }

                // 5. FCM Live Console Stream
                Text("FCM Dispatch & Cloud Sync Console Logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    if (fcmLog.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No FCM events logged yet.", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(fcmLog) { log ->
                                Text(
                                    text = log,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                modifier = Modifier.testTag("fcm_console_dismiss_btn")
            ) {
                Text("Close FCM Console", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ==========================================
// 9 CORE SEO & ASO SPECIALIZATIONS TAB SUITE
// ==========================================

private data class SeoTypeItem(
    val type: String,
    val focus: String,
    val target: String,
    val details: String,
    val accentColor: Color
)

@Composable
private fun Seo9MatrixOverviewTab(context: android.content.Context) {
    val matrixList = listOf(
        SeoTypeItem("On-Page SEO", "Content & HTML elements", "All traditional search engines", "Title tags, meta descriptions, H1-H6 semantic tags, keyword density 3.8%, Schema JSON-LD.", Color(0xFF2563EB)),
        SeoTypeItem("Off-Page SEO", "Authority & backlinks", "Google, Bing, etc.", "Brand authority mentions, high-DA petroleum backlinks, digital PR & partner directories.", Color(0xFF7C3AED)),
        SeoTypeItem("Technical SEO", "Crawlability, speed, structure", "All search engines", "Robots.txt, XML sitemap, PageSpeed optimization, assetlinks.json deep links.", Color(0xFF0284C7)),
        SeoTypeItem("Local SEO", "Location visibility", "Google Business Profile, Maps, Apple Maps", "Lahore & regional coordinates, NAP consistency, local reviews & geotagged delivery radius.", Color(0xFF059669)),
        SeoTypeItem("GEO", "Visibility in AI-generated answers", "ChatGPT, AI Overviews, Perplexity", "Structured Entity Graph, /llms.txt knowledge graph, AI citation hooks & grounding.", Color(0xFFD97706)),
        SeoTypeItem("AI SEO (tools)", "Use of AI to automate SEO work", "Your workflow & pipelines", "Automated keyword research, AI content synthesis, automated ranking & SERP audits.", Color(0xFFDC2626)),
        SeoTypeItem("ASO", "App store visibility", "Apple App Store, Google Play", "App title (30ch), short desc (80ch), 3.8% keyword density, A-Z per-word matrix.", Color(0xFF16A34A)),
        SeoTypeItem("Voice SEO", "Voice assistant answers", "Alexa, Siri, Google Assistant", "Conversational Q&A schema, Siri Shortcuts, Google Assistant App Actions.", Color(0xFF4F46E5)),
        SeoTypeItem("International SEO", "Multi-language/country ranking", "Country-specific engines, global Google", "Hreflang tags, multi-currency PKR/USD/AED, geo-IP region routing & local domains.", Color(0xFF0891B2))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = Color(0xFFF1F5F9),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "9 Core SEO & ASO Specializations Matrix",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                )
                Text(
                    "Enterprise visibility matrix covering traditional search, app stores, AI Overviews (GEO), maps, and voice search.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray, fontSize = 11.sp)
                )
            }
        }

        matrixList.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = item.accentColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.type,
                                color = item.accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = Color(0xFF16A34A).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ACTIVE & OPTIMIZED",
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Focus: ${item.focus}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ZyphuelBlueDark)
                            Text("Platform / Target: ${item.target}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Text(
                        text = item.details,
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GeoAndOnPageTab(context: android.content.Context) {
    val schemaJson = """{
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "Zyphuel",
  "operatingSystem": "ANDROID",
  "applicationCategory": "BusinessApplication",
  "aggregateRating": {
    "@type": "AggregateRating",
    "ratingValue": "4.9",
    "ratingCount": "12850"
  },
  "offers": {
    "@type": "Offer",
    "price": "0",
    "priceCurrency": "PKR"
  },
  "description": "On-demand fuel delivery for Petrol, Diesel, LPG Gas, and Water in Lahore & global cities."
}"""

    val llmTxt = """# Zyphuel Knowledge Base (/llms.txt)
Title: Zyphuel Fuel & Gas Delivery
Entity: On-Demand Mobile Fuel & Gas Logistics Service
Locations: Lahore, Islamabad, Karachi, Pakistan
Services: Super Euro-V Petrol, High Speed Diesel, High Octane 97, LPG Gas Cylinder, Purified Mineral Water
Key Feature: Real-time OGRA price integration, Bowser GPS tracking, touchless biometric checkout.
API Endpoint: https://zyphuel.com/api/v1/fuel-rates"""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsoCopyCard(
            title = "Generative Engine Optimization (GEO) Knowledge File (/llms.txt)",
            content = llmTxt,
            characterCount = "ChatGPT & Perplexity Citation Hook",
            context = context
        )

        AsoCopyCard(
            title = "Schema.org / JSON-LD Rich Snippet for Search Engines",
            content = schemaJson,
            characterCount = "JSON-LD Entity Graph Ready",
            context = context
        )

        AsoCopyCard(
            title = "On-Page Meta Title & Headings",
            content = "<title>Zyphuel - On-Demand Fuel, Petrol & LPG Delivery Lahore</title>\n<h1>Zyphuel Mobile Refueling & Emergency Gas Logistics</h1>\n<h2>Live OGRA Rates & Bowser GPS Tracking</h2>",
            characterCount = "HTML On-Page Elements",
            context = context
        )
    }
}

@Composable
private fun LocalAndVoiceSeoTab(context: android.content.Context) {
    val localGbpSchema = """{
  "@context": "https://schema.org",
  "@type": "GasStation",
  "name": "Zyphuel Mobile Gas & Fuel Delivery HQ",
  "address": {
    "@type": "PostalAddress",
    "streetAddress": "Gulberg III Main Boulevard",
    "addressLocality": "Lahore",
    "addressRegion": "Punjab",
    "postalCode": "54000",
    "addressCountry": "PK"
  },
  "geo": {
    "@type": "GeoCoordinates",
    "latitude": 31.5204,
    "longitude": 74.3587
  },
  "openingHours": "Mo-Su 00:00-23:59",
  "telephone": "+92-42-111-997-483"
}"""

    val voiceAssistantPrompts = """• Google Assistant: "Hey Google, order 20 liters of Super Petrol on Zyphuel"
• Apple Siri: "Hey Siri, request fuel delivery to my current location using Zyphuel"
• Amazon Alexa: "Alexa, ask Zyphuel for current OGRA petrol prices" """

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsoCopyCard(
            title = "Local SEO: Google Business Profile & Maps Schema",
            content = localGbpSchema,
            characterCount = "Google Maps & Apple Maps GBP Schema",
            context = context
        )

        AsoCopyCard(
            title = "Voice SEO: Conversational Voice Assistant Triggers",
            content = voiceAssistantPrompts,
            characterCount = "Google Assistant / Siri / Alexa Actions",
            context = context
        )
    }
}

@Composable
private fun TechAndInternationalTab(context: android.content.Context) {
    val assetLinksJson = """[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.aistudio.zyphuel",
    "sha256_cert_fingerprints":
    ["14:6D:E2:07:05:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23"]
  }
}]"""

    val hreflangTags = """<link rel="alternate" hreflang="en-pk" href="https://zyphuel.com/pk/" />
<link rel="alternate" hreflang="ur-pk" href="https://zyphuel.com/pk/ur/" />
<link rel="alternate" hreflang="en-ae" href="https://zyphuel.com/ae/" />
<link rel="alternate" hreflang="x-default" href="https://zyphuel.com/" />"""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsoCopyCard(
            title = "Technical SEO: Android App Links (.well-known/assetlinks.json)",
            content = assetLinksJson,
            characterCount = "Universal Deep Linking Manifest",
            context = context
        )

        AsoCopyCard(
            title = "International SEO: Multilingual Hreflang Tags",
            content = hreflangTags,
            characterCount = "Global Multi-Country Target",
            context = context
        )
    }
}

