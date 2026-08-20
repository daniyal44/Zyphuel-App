package com.example.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.DesktopOrder
import com.example.desktop.LatLng
import com.example.desktop.OpsConsoleState
import com.example.desktop.RiderPosition
import com.example.desktop.estimateEtaMinutes
import com.example.desktop.haversineKm
import com.example.desktop.tripProgress
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF0F172A)
private val InkSoft = Color(0xFF1E293B)
private val Sky = Color(0xFF0284C7)
private val SkyBright = Color(0xFF38BDF8)
private val Paper = Color(0xFFF1F5F9)

@Composable
fun OpsConsoleScreen(state: OpsConsoleState) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { state.pollForever() }

    Row(modifier = Modifier.fillMaxSize().background(Paper)) {
        OrderListPane(state = state, modifier = Modifier.width(374.dp).fillMaxHeight())

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val order = state.selectedOrder
            val live = state.liveFor(order)

            TileMapView(
                markers = buildMarkers(order, live),
                route = buildRoute(order, live),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            if (order != null) {
                DetailPane(
                    order = order,
                    live = live,
                    isBusy = state.isBusy,
                    onStatus = { status -> scope.launch { state.changeStatus(order, status) } }
                )
            } else {
                EmptyDetail(hasLoaded = state.hasLoadedOnce)
            }

            if (state.firestoreSetupNeeded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "⚠️ Firestore database '(default)' is not created yet in Firebase project '${state.rest.config.projectId}'",
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Running in interactive Demo Simulation Mode. To stream live orders, click to open Firebase Console and click 'Create database' (in test mode).",
                                color = Color(0xFFB45309),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { state.openFirebaseConsoleInBrowser() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🌐 Create Database", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                state.errorMessage?.let { message ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = message,
                            color = Color(0xFF991B1B),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Left pane ----------------

@Composable
private fun OrderListPane(state: OpsConsoleState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Ink)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text("ZYPHUEL", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                "Operations Console",
                color = SkyBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotColor = when {
                    state.firestoreSetupNeeded || state.isDemoMode -> Color(0xFFF59E0B)
                    state.errorMessage != null -> Color(0xFFEF4444)
                    else -> Color(0xFF22C55E)
                }
                val statusText = when {
                    state.isDemoMode -> "Demo Mode · ${state.ridersOnline} simulated rider(s)"
                    state.errorMessage != null -> "Connection problem"
                    else -> "Live · ${state.ridersOnline} rider(s) reporting"
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp
                )
                Spacer(Modifier.weight(1f))
                if (!state.hasLoadedOnce) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = SkyBright,
                        strokeWidth = 2.dp
                    )
                } else if (state.lastRefreshAt > 0) {
                    Text(
                        text = clockFormat.format(Date(state.lastRefreshAt)),
                        color = Color(0xFF64748B),
                        fontSize = 9.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OpsConsoleState.Filter.entries.forEach { option ->
                val selected = state.filter == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (selected) Sky else InkSoft)
                        .clickable { state.filter = option }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        color = if (selected) Color.White else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val orders = state.visibleOrders
        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (state.hasLoadedOnce) "No ${state.filter.label.lowercase()} orders" else "Loading orders…",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderRow(
                        order = order,
                        live = state.livePositions[order.id],
                        selected = state.selectedOrder?.id == order.id,
                        onClick = { state.selectedOrderId = order.id }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderRow(
    order: DesktopOrder,
    live: RiderPosition?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val hasLiveFix = live != null && !live.isStale(System.currentTimeMillis())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Color(0xFF14304A) else InkSoft)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) SkyBright else Color(0xFF334155),
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#${order.id}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            Text(text = order.serviceType, color = Color(0xFFCBD5E1), fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            StatusPill(order.status)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = order.customerName + " · " + order.deliveryAddress,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Rs. " + formatMoney(order.totalPrice) + " · " + order.paymentMethod,
                color = Color(0xFF64748B),
                fontSize = 9.sp
            )
            Spacer(Modifier.weight(1f))
            if (hasLiveFix) {
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF22C55E), RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(4.dp))
                Text("GPS", color = Color(0xFF22C55E), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            } else if (live != null) {
                Text("GPS stale", color = Color(0xFFF59E0B), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = statusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text = status, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------- Right pane ----------------

@Composable
private fun DetailPane(
    order: DesktopOrder,
    live: RiderPosition?,
    isBusy: Boolean,
    onStatus: (String) -> Unit
) {
    val now = System.currentTimeMillis()
    val hasLiveFix = live != null && !live.isStale(now)
    val remainingKm = if (hasLiveFix) haversineKm(live!!.position, order.destination) else null
    val progress = if (hasLiveFix) tripProgress(order.origin, live!!.position, order.destination) else 0f
    val eta = if (hasLiveFix && remainingKm != null) {
        estimateEtaMinutes(remainingKm, live!!.speedKmh, order.etaMinutes)
    } else {
        order.etaMinutes
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Order #${order.id} · ${order.serviceType}",
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            StatusPill(order.status)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (hasLiveFix) "Live GPS · updated ${agoText(now - live!!.updatedAt)}"
                else if (live != null) "GPS stale · last fix ${agoText(now - live.updatedAt)}"
                else "Rider has not started sharing GPS yet",
                color = if (hasLiveFix) Color(0xFF16A34A) else Color(0xFF64748B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            StatCell("Customer", order.customerName)
            StatCell("Phone", order.customerPhone.ifBlank { "—" })
            StatCell("Rider", order.riderName ?: "Unassigned")
            StatCell("Quantity", "${order.quantity} units")
            StatCell("Amount", "Rs. " + formatMoney(order.totalPrice))
            StatCell("Payment", order.paymentMethod)
        }

        Spacer(Modifier.height(8.dp))
        Text(text = "Destination: " + order.deliveryAddress, color = Color(0xFF475569), fontSize = 11.sp)

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            StatCell("Remaining", remainingKm?.let { formatKm(it) } ?: "—")
            Spacer(Modifier.width(18.dp))
            StatCell("ETA", "$eta min")
            Spacer(Modifier.width(18.dp))
            StatCell("Speed", if (hasLiveFix) "${Math.round(live!!.speedKmh)} km/h" else "—")
            Spacer(Modifier.width(18.dp))
            StatCell("Progress", "${(progress * 100).toInt()}%")
        }

        Spacer(Modifier.height(8.dp))

        // Hand-rolled progress bar: avoids the M3 indicator API differences across versions.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(Sky, RoundedCornerShape(4.dp))
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Set status:", color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            listOf("Assigned", "Delivering", "Completed").forEach { status ->
                Button(
                    onClick = { onStatus(status) },
                    enabled = !isBusy && order.status != status,
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor(status)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(status, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(
                onClick = { onStatus("Cancelled") },
                enabled = !isBusy && !order.isFinished,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel order", fontSize = 11.sp, color = Color(0xFFDC2626))
            }
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Sky)
            }
        }
    }
}

@Composable
private fun EmptyDetail(hasLoaded: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasLoaded) "Select an order on the left to track it." else "Connecting to Firestore…",
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// ---------------- Helpers ----------------

private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

private fun buildMarkers(order: DesktopOrder?, live: RiderPosition?): List<MapMarker> {
    if (order == null) return emptyList()
    val markers = mutableListOf(
        MapMarker(order.origin, Color(0xFF475569), "Depot", radiusPx = 7f),
        MapMarker(order.destination, Color(0xFFDC2626), "Destination")
    )
    if (live != null) {
        markers += MapMarker(
            position = live.position,
            color = if (live.isStale(System.currentTimeMillis())) Color(0xFFF59E0B) else Color(0xFF16A34A),
            label = "Rider" + (order.riderName?.let { " · $it" } ?: ""),
            bearing = live.bearing,
            radiusPx = 10f
        )
    }
    return markers
}

private fun buildRoute(order: DesktopOrder?, live: RiderPosition?): List<LatLng> {
    if (order == null) return emptyList()
    return if (live != null) {
        listOf(order.origin, live.position, order.destination)
    } else {
        listOf(order.origin, order.destination)
    }
}

private fun statusColor(status: String): Color = when (status) {
    "Pending" -> Color(0xFFF59E0B)
    "Assigned", "Accepted" -> Color(0xFF0284C7)
    "Delivering", "On the way" -> Color(0xFF7C3AED)
    "Completed", "Delivered" -> Color(0xFF16A34A)
    "Cancelled", "Canceled" -> Color(0xFFDC2626)
    else -> Color(0xFF64748B)
}

private fun formatMoney(value: Double): String = String.format(Locale.US, "%,.0f", value)

private fun formatKm(value: Double): String =
    if (value < 1.0) String.format(Locale.US, "%.0f m", value * 1000)
    else String.format(Locale.US, "%.1f km", value)

private fun agoText(millis: Long): String {
    if (millis < 0) return "just now"
    val seconds = millis / 1000
    return when {
        seconds < 10 -> "just now"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        else -> "${seconds / 3600}h ago"
    }
}
