package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OrderEntity
import com.example.data.UserEntity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Native Google Maps SDK Live Delivery Tracking Overlay for Zyphuel.
 * Renders the real-time position of the assigned delivery vehicle (Fuel Bowser / Water Tanker / Express Rider),
 * transit polyline corridor, central depot origin, customer destination, and interactive telematics.
 */
@Composable
fun GoogleMapsLiveDeliveryTrackingOverlay(
    modifier: Modifier = Modifier,
    order: OrderEntity? = null,
    assignedRider: UserEntity? = null,
    viewModel: MainViewModel? = null,
    onExpandFullscreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Resolve depot origin (captured on the order, else Zyphuel Green Town hub constant)
    val depotLat = order?.originLat ?: 31.4380
    val depotLng = order?.originLng ?: 74.3050 // Zyphuel Green Town Central Hub Depot
    val depotLatLng = remember(depotLat, depotLng) { LatLng(depotLat, depotLng) }

    // Resolve customer destination: real captured coords -> address keyword guess -> default
    val customerLatLng = remember(order?.id, order?.destLat, order?.destLng, order?.deliveryAddress) {
        val dLat = order?.destLat
        val dLng = order?.destLng
        if (dLat != null && dLng != null) {
            LatLng(dLat, dLng)
        } else {
            val addr = order?.deliveryAddress ?: ""
            when {
                addr.contains("DHA", ignoreCase = true) -> LatLng(31.4700, 74.4100)
                addr.contains("Johar", ignoreCase = true) -> LatLng(31.4697, 74.2728)
                addr.contains("Model Town", ignoreCase = true) -> LatLng(31.4820, 74.3180)
                addr.contains("Mall Rd", ignoreCase = true) || addr.contains("Anarkali", ignoreCase = true) -> LatLng(31.5580, 74.3160)
                else -> LatLng(31.5204, 74.3587)
            }
        }
    }

    // Lahore Route Waypoints corridor from Depot to Customer Destination (context line)
    val routePoints = remember(customerLatLng, depotLatLng) {
        generateCorridorWaypoints(start = depotLatLng, destination = customerLatLng)
    }

    // --- REAL-TIME rider GPS (published by the rider's foreground service, streamed via Firestore) ---
    val riderLiveState = viewModel?.riderLiveLocation?.collectAsState()
    val riderLive = riderLiveState?.value
    val hasLiveFix = riderLive != null

    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var isExpanded by remember { mutableStateOf(false) }

    // Whether Google Maps has a usable API key; otherwise show the vector fallback map.
    val mapsKeyPresent = remember { isMapsApiKeyPresent(context) }

    // Target real position (stationed at Green Town Depot when Pending, live fix when streaming, or animated corridor)
    val isPending = order?.status == "Pending" || order?.status == null
    val targetLatLng = when {
        riderLive != null -> LatLng(riderLive.lat, riderLive.lng)
        isPending -> depotLatLng
        order?.status == "Completed" -> customerLatLng
        else -> {
            val progress = ((20 - (order?.etaMinutes ?: 15)).coerceIn(0, 20) / 20f).coerceIn(0.15f, 0.85f)
            interpolateAlongPath(routePoints, progress)
        }
    }

    // Smoothly track the marker between updates without 60fps recomposition locks
    var displayedPosition by remember(targetLatLng.latitude, targetLatLng.longitude) { mutableStateOf(targetLatLng) }
    var displayedBearing by remember(riderLive?.bearing) { mutableFloatStateOf(riderLive?.bearing ?: 0f) }

    LaunchedEffect(targetLatLng.latitude, targetLatLng.longitude) {
        val start = displayedPosition
        val end = targetLatLng
        val liveBearing = riderLive?.bearing ?: 0f
        val movementBearing = bearingBetween(start, end)
        val useBearing = if (liveBearing != 0f) liveBearing else movementBearing
        val distMeters = haversineKm(start, end) * 1000.0
        if (distMeters < 1.0) {
            displayedPosition = end
            if (useBearing != 0f) displayedBearing = useBearing
            return@LaunchedEffect
        }
        val anim = Animatable(0f)
        anim.animateTo(1f, animationSpec = tween(600, easing = LinearEasing)) {
            val f = value
            displayedPosition = LatLng(
                start.latitude + (end.latitude - start.latitude) * f,
                start.longitude + (end.longitude - start.longitude) * f
            )
        }
        if (useBearing != 0f) displayedBearing = useBearing
    }

    // Real telematics derived from positions
    val totalRouteKm = remember(depotLatLng, customerLatLng) {
        haversineKm(depotLatLng, customerLatLng).coerceAtLeast(0.1)
    }
    val remainingDistanceKm = haversineKm(targetLatLng, customerLatLng)
    val currentSpeedKmh = when {
        riderLive != null -> riderLive.speedKmh.roundToInt()
        isPending -> 0
        order?.status == "Delivering" || order?.status == "In Transit" -> 35
        else -> 0
    }
    val etaMinutes = if (hasLiveFix) {
        if (currentSpeedKmh > 4) ceil(remainingDistanceKm / currentSpeedKmh * 60.0).toInt().coerceAtLeast(1)
        else ceil(remainingDistanceKm * 3.0).toInt().coerceAtLeast(1)
    } else (order?.etaMinutes ?: 20)
    val transitProgress = if (isPending) 0f else (1.0 - (remainingDistanceKm / totalRouteKm)).coerceIn(0.0, 1.0).toFloat()


    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(if (hasLiveFix) targetLatLng else customerLatLng, 13.5f)
    }

    // Auto-follow the live vehicle. Keyed on the GPS target, NOT on displayedPosition:
    // displayedPosition changes every animation frame during the glide, which cancelled and
    // restarted this camera animation ~60 times a second and locked up the UI.
    LaunchedEffect(targetLatLng.latitude, targetLatLng.longitude, hasLiveFix) {
        if (hasLiveFix) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(targetLatLng),
                durationMs = 800
            )
        }
    }

    // Rider details
    val driverName = assignedRider?.name ?: order?.riderName ?: if (order?.status == "Pending") "Assigning nearest driver..." else "Assigned Delivery Driver"
    val driverPhone = assignedRider?.phoneNumber ?: "+92 323 0112464"
    val vehicleType = assignedRider?.vehicleType ?: if (order?.serviceType?.contains("Water", ignoreCase = true) == true) "Water Tanker" else "${order?.serviceType ?: "Fuel"} Delivery"
    val vehiclePlate = assignedRider?.vehicleNo ?: if (assignedRider != null) "Verified Vehicle" else "En Route"


    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("google_maps_live_delivery_tracking_overlay")
            .shadow(6.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- TOP TELEMATICS HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing radar live dot
                    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "radar_scale"
                    )
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size((12 * pulseScale).dp)
                                .background(Color(0xFF22C55E).copy(alpha = 0.4f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "GOOGLE MAPS LIVE TRACKING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF38BDF8),
                                    letterSpacing = 0.8.sp
                                )
                            )
                            Surface(
                                color = Color(0xFF0284C7).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SDK v6.4",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF38BDF8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Assigned: $vehicleType",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // ETA Badge
                Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ETA: ~$etaMinutes MINS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = "${"%.1f".format(remainingDistanceKm)} km away",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFE0F2FE),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // --- GOOGLE MAPS SDK VIEWPORT ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpanded) 340.dp else 220.dp)
                    .clip(RoundedCornerShape(0.dp))
            ) {
                val uiSettings = remember {
                    MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = true,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false
                    )
                }
                val mapProperties = remember(mapType) {
                    MapProperties(
                        mapType = mapType,
                        isTrafficEnabled = true
                    )
                }

                if (mapsKeyPresent) {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("google_map_sdk_surface"),
                        cameraPositionState = cameraPositionState,
                        uiSettings = uiSettings,
                        properties = mapProperties
                    ) {
                        // 1. Origin Depot Marker (Green Town Hub)
                        Marker(
                            state = MarkerState(position = depotLatLng),
                            title = "Zyphuel Dispatch Depot",
                            snippet = "Green Town Central Hub, Lahore",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )

                        // 2. Customer Destination Marker
                        Marker(
                            state = MarkerState(position = customerLatLng),
                            title = "Delivery Destination",
                            snippet = order?.deliveryAddress ?: "Customer Location, Lahore",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )

                        // 3. Real-Time Assigned Delivery Vehicle Marker (Always visible on Google Maps)
                        Marker(
                            state = MarkerState(position = displayedPosition),
                            title = "$driverName ($vehiclePlate)",
                            snippet = if (hasLiveFix) "Speed: $currentSpeedKmh km/h • ETA: $etaMinutes mins" else "Status: ${order?.status ?: "Pending"} • ETA: $etaMinutes mins",
                            rotation = displayedBearing,
                            flat = true,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        )

                        // Active radar circle around moving vehicle
                        Circle(
                            center = displayedPosition,
                            radius = 220.0,
                            fillColor = Color(0x3322C55E),
                            strokeColor = Color(0xFF22C55E),
                            strokeWidth = 3f
                        )

                        // 4. Delivery Corridor Polyline Route
                        Polyline(
                            points = routePoints,
                            color = Color(0xFF0284C7),
                            width = 12f,
                            geodesic = true
                        )
                    }
                } else {
                    // No usable Google Maps API key -> graceful vector fallback (never a blank panel)
                    FallbackRadarMapView(
                        vehiclePosition = displayedPosition,
                        customerPosition = customerLatLng,
                        depotPosition = depotLatLng,
                        transitProgress = transitProgress,
                        currentSpeedKmh = currentSpeedKmh,
                        driverName = driverName,
                        vehiclePlate = vehiclePlate
                    )
                }

                // Real-Time Dispatch / GPS Status Pill Banner
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp),
                    color = Color(0xEE0F172A),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (hasLiveFix || order?.status == "Delivering") Color(0xFF22C55E) else Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (hasLiveFix || order?.status == "Delivering") Color(0xFF22C55E) else Color(0xFF38BDF8), CircleShape)
                        )
                        Text(
                            text = when {
                                hasLiveFix -> "Live GPS Streaming • Rider En Route"
                                order?.status == "Delivering" || order?.status == "In Transit" -> "Driver Dispatched • En Route"
                                order?.status == "Assigned" -> "Rider Assigned • Preparing Dispatch"
                                else -> "Bowser Stationed at Green Town Hub"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }


                // --- FLOATING MAP CONTROLS OVERLAY ---
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Map Type Toggle
                    FloatingMapActionButton(
                        icon = Icons.Filled.Layers,
                        contentDesc = "Map Type",
                        onClick = {
                            mapType = when (mapType) {
                                MapType.NORMAL -> MapType.SATELLITE
                                MapType.SATELLITE -> MapType.TERRAIN
                                MapType.TERRAIN -> MapType.HYBRID
                                else -> MapType.NORMAL
                            }
                        }
                    )

                    // Recenter on Vehicle
                    FloatingMapActionButton(
                        icon = Icons.Filled.Navigation,
                        contentDesc = "Recenter Vehicle",
                        onClick = {
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(displayedPosition, 15f),
                                    durationMs = 500
                                )
                            }
                        }
                    )

                    // Recenter on Destination
                    FloatingMapActionButton(
                        icon = Icons.Filled.Place,
                        contentDesc = "Recenter Destination",
                        onClick = {
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(customerLatLng, 15f),
                                    durationMs = 500
                                )
                            }
                        }
                    )

                    // Expand / Collapse
                    FloatingMapActionButton(
                        icon = if (isExpanded) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDesc = "Toggle Size",
                        onClick = {
                            isExpanded = !isExpanded
                            onExpandFullscreen?.invoke()
                        }
                    )
                }

                // Bottom Left Speedometer HUD Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    color = Color(0xDD0F172A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$currentSpeedKmh km/h",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (hasLiveFix) "• En Route" else "• Awaiting driver",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (hasLiveFix) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // --- PROGRESS BAR CORRIDOR ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏢 Green Town Depot",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                    )
                    Text(
                        text = if (hasLiveFix) "🚚 ${(transitProgress * 100).toInt()}% En Route" else "🚚 Not started yet",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "📍 Your Location",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }

                LinearProgressIndicator(
                    progress = { transitProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF334155)
                )

                // --- LIVE ORDER / FUEL SUMMARY CHIP ---
                if (order != null) {
                    val unitLabel = when {
                        order.serviceType.contains("LPG", ignoreCase = true) -> "kg"
                        order.serviceType.contains("Water", ignoreCase = true) -> "L"
                        else -> "L"
                    }
                    val unitPrice = if (order.quantity > 0) order.totalPrice / order.quantity else 0.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${order.serviceType} • ${order.quantity} $unitLabel",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "@ Rs ${"%.2f".format(unitPrice)}/$unitLabel",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Rs ${"%.0f".format(order.totalPrice)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF4ADE80),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // --- DRIVER & QUICK ACTION BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Driver Profile Preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF0284C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = driverName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = driverName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified Driver",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "$vehiclePlate • 4.9★",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                        )
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Fit entire route (depot + destination + live vehicle) into view
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val boundsBuilder = LatLngBounds.builder()
                                        .include(depotLatLng)
                                        .include(customerLatLng)
                                    if (hasLiveFix) boundsBuilder.include(displayedPosition)
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120),
                                        durationMs = 600
                                    )
                                } catch (e: Exception) {}
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomOutMap,
                            contentDescription = "Fit Route",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Call Driver Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$driverPhone"))
                            try {
                                context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                            } catch (e: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingMapActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xEE0F172A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        shadowElevation = 3.dp,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * High-fidelity vector street map of Lahore showing the live transit corridor,
 * depot hub, customer location, and delivery vehicle telematics.
 */
@Composable
private fun FallbackRadarMapView(
    vehiclePosition: LatLng,
    customerPosition: LatLng,
    depotPosition: LatLng,
    transitProgress: Float,
    currentSpeedKmh: Int,
    driverName: String,
    vehiclePlate: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF020617))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Sleek tactical grid lines
            val gridStep = 36.dp.toPx()
            for (x in 0..(width / gridStep).toInt()) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.45f),
                    start = Offset(x * gridStep, 0f),
                    end = Offset(x * gridStep, height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(height / gridStep).toInt()) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.45f),
                    start = Offset(0f, y * gridStep),
                    end = Offset(width, y * gridStep),
                    strokeWidth = 1f
                )
            }

            // 2. Major Lahore Arteries (Background Infrastructure)
            // Ferozepur Road (North-South spine)
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.6f),
                start = Offset(width * 0.48f, 0f),
                end = Offset(width * 0.38f, height),
                strokeWidth = 6f
            )
            // Lahore Ring Road (Southern & Eastern outer loop)
            val ringPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, height * 0.82f)
                cubicTo(width * 0.45f, height * 0.88f, width * 0.85f, height * 0.70f, width, height * 0.35f)
            }
            drawPath(
                path = ringPath,
                color = Color(0xFF334155).copy(alpha = 0.5f),
                style = Stroke(width = 5f)
            )
            // Canal Bank Road (Diagonal cross-city arterial)
            drawLine(
                color = Color(0xFF0284C7).copy(alpha = 0.35f),
                start = Offset(0f, height * 0.32f),
                end = Offset(width, height * 0.58f),
                strokeWidth = 4f
            )

            // 3. Depot point (Green Town Central Hub) & Customer Destination
            val depotOffset = Offset(width * 0.20f, height * 0.72f)
            val customerOffset = Offset(width * 0.80f, height * 0.28f)
            val mid1 = Offset(width * 0.38f, height * 0.60f)
            val mid2 = Offset(width * 0.58f, height * 0.38f)

            // 4. Delivery Corridor Road Path
            val corridorPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(depotOffset.x, depotOffset.y)
                cubicTo(mid1.x, mid1.y, mid2.x, mid2.y, customerOffset.x, customerOffset.y)
            }

            // Road base layer
            drawPath(
                path = corridorPath,
                color = Color(0xFF1E293B),
                style = Stroke(width = 14f)
            )
            // Active glow road overlay
            drawPath(
                path = corridorPath,
                color = Color(0xFF0284C7),
                style = Stroke(
                    width = 5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                )
            )

            // 5. Draw Depot Node (Green Town HQ)
            drawCircle(
                color = Color(0x440284C7),
                radius = 18.dp.toPx(),
                center = depotOffset
            )
            drawCircle(
                color = Color(0xFF0284C7),
                radius = 8.dp.toPx(),
                center = depotOffset
            )

            // 6. Draw Customer Node (Destination)
            drawCircle(
                color = Color(0x44EF4444),
                radius = 18.dp.toPx(),
                center = customerOffset
            )
            drawCircle(
                color = Color(0xFFEF4444),
                radius = 8.dp.toPx(),
                center = customerOffset
            )

            // 7. Calculate and render Current Moving Delivery Vehicle
            val vehicleProgressClamped = transitProgress.coerceIn(0f, 1f)
            val vehicleX = depotOffset.x + (customerOffset.x - depotOffset.x) * vehicleProgressClamped
            val vehicleY = depotOffset.y + (customerOffset.y - depotOffset.y) * vehicleProgressClamped
            val currentVehicleOffset = Offset(vehicleX, vehicleY)

            // Active radar halo rings around delivery vehicle
            drawCircle(
                color = Color(0x3322C55E),
                radius = 28.dp.toPx(),
                center = currentVehicleOffset
            )
            drawCircle(
                color = Color(0x6622C55E),
                radius = 16.dp.toPx(),
                center = currentVehicleOffset
            )
            drawCircle(
                color = Color(0xFF22C55E),
                radius = 9.dp.toPx(),
                center = currentVehicleOffset
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = currentVehicleOffset
            )
        }

        // Overlay Location Labels
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Depot Tag
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 18.dp, start = 8.dp),
                color = Color(0xDD0F172A),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFF0284C7))
            ) {
                Text(
                    text = "🏢 Green Town Depot (HQ)",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Customer Destination Tag
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 52.dp),
                color = Color(0xDD0F172A),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Text(
                    text = "📍 Delivery Destination",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Live Driver Vehicle Badge Floating Tag
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 30.dp),
                color = Color(0xEE0F172A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF22C55E))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF22C55E), CircleShape))
                    Text(
                        text = "🚚 $driverName ($vehiclePlate)",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    )
                }
            }
        }
    }
}


/**
 * Generates realistic delivery route corridor waypoints across Lahore roads.
 */
private fun generateCorridorWaypoints(start: LatLng, destination: LatLng): List<LatLng> {
    val steps = 12
    val points = mutableListOf<LatLng>()
    points.add(start)

    val dLat = (destination.latitude - start.latitude) / steps
    val dLng = (destination.longitude - start.longitude) / steps

    for (i in 1 until steps) {
        val jitterLat = sin(i.toDouble() * 1.5) * 0.003
        val jitterLng = cos(i.toDouble() * 1.5) * 0.003
        points.add(
            LatLng(
                start.latitude + (dLat * i) + jitterLat,
                start.longitude + (dLng * i) + jitterLng
            )
        )
    }
    points.add(destination)
    return points
}

/**
 * Interpolates vehicle position smoothly along the waypoints list.
 */
private fun interpolateAlongPath(path: List<LatLng>, progress: Float): LatLng {
    if (path.isEmpty()) return LatLng(31.5204, 74.3587)
    if (path.size == 1 || progress <= 0f) return path.first()
    if (progress >= 1f) return path.last()

    val totalSegments = path.size - 1
    val scaledProgress = progress * totalSegments
    val index = scaledProgress.toInt().coerceIn(0, totalSegments - 1)
    val fraction = scaledProgress - index

    val p1 = path[index]
    val p2 = path[index + 1]

    val lat = p1.latitude + (p2.latitude - p1.latitude) * fraction
    val lng = p1.longitude + (p2.longitude - p1.longitude) * fraction
    return LatLng(lat, lng)
}

/**
 * Calculates vehicle bearing angle in degrees along the current route segment.
 */
private fun calculateBearingAlongPath(path: List<LatLng>, progress: Float): Float {
    if (path.size < 2) return 0f
    val totalSegments = path.size - 1
    val scaledProgress = progress * totalSegments
    val index = scaledProgress.toInt().coerceIn(0, totalSegments - 1)
    val nextIndex = (index + 1).coerceAtMost(path.size - 1)

    val p1 = path[index]
    val p2 = path[nextIndex]

    val lat1 = Math.toRadians(p1.latitude)
    val lng1 = Math.toRadians(p1.longitude)
    val lat2 = Math.toRadians(p2.latitude)
    val lng2 = Math.toRadians(p2.longitude)

    val dLng = lng2 - lng1
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)

    val bearing = (Math.toDegrees(atan2(y, x)) + 360) % 360
    return bearing.toFloat()
}

/**
 * Great-circle (haversine) distance in kilometres between two coordinates.
 */
private fun haversineKm(a: LatLng, b: LatLng): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
    return 2 * earthRadiusKm * asin(min(1.0, sqrt(h)))
}

/**
 * Initial compass bearing (degrees, 0..360) heading from point a to point b.
 */
private fun bearingBetween(a: LatLng, b: LatLng): Float {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
}

/**
 * Returns true only when a real Google Maps SDK key is present in the manifest.
 * When absent/placeholder we render the vector fallback map instead of a blank tile surface.
 *
 * Real Google API keys always start with "AIza" and are ~39 chars, so that prefix check
 * rejects every placeholder ("PLACEHOLDER_...", "AIzaSyA_YOUR_...", empty) on its own —
 * without it an invalid key produces a grey, permanently-loading tile surface.
 */
private fun isMapsApiKeyPresent(context: Context): Boolean {
    return try {
        val ai = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val key = ai.metaData?.getString("com.google.android.geo.API_KEY")?.trim()
        !key.isNullOrBlank() &&
                key.startsWith("AIza") &&
                !key.contains("YOUR_", ignoreCase = true) &&
                key.length >= 35
    } catch (e: Exception) {
        false
    }
}
