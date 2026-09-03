package com.example.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.DEPOT_LAT
import com.example.desktop.DEPOT_LNG
import com.example.desktop.DesktopFleetRider
import com.example.desktop.DesktopOrder
import com.example.desktop.LatLng
import com.example.desktop.OpsConsoleState
import com.example.desktop.RiderPosition
import com.example.desktop.estimateEtaMinutes
import com.example.desktop.haversineKm
import com.example.desktop.tripProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Color palette ─────────────────────────────────────────────────────────────
private val Ink        = Color(0xFF0B132B)
private val InkSoft    = Color(0xFF1C2541)
private val InkMid     = Color(0xFF3A506B)
private val Sky        = Color(0xFF0284C7)
private val SkyBright  = Color(0xFF38BDF8)
private val Paper      = Color(0xFFF8FAFC)
private val PaperMid   = Color(0xFFE2E8F0)
private val Slate      = Color(0xFF64748B)
private val SlateLight = Color(0xFF94A3B8)
private val GreenTone  = Color(0xFF10B981)
private val AmberTone  = Color(0xFFF59E0B)
private val RedTone    = Color(0xFFEF4444)
private val PurpleTone = Color(0xFF8B5CF6)

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun OpsConsoleScreen(state: OpsConsoleState) {
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showInvoiceDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedServiceFilter by remember { mutableStateOf("All") }

    // Start background Firestore polling
    LaunchedEffect(Unit) { state.pollOrdersForever() }
    LaunchedEffect(Unit) { state.pollLiveTrackingForever() }

    // System clock timer
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMillis = System.currentTimeMillis()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Paper)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Navigation & KPI Bar ──────────────────────────────────────
            TopNavigationBar(
                state = state,
                nowMillis = nowMillis,
                onNewOrder = { showCreateDialog = true },
                onRefresh = { scope.launch { state.refresh() } }
            )

            // ── Notification Banner (if any) ──────────────────────────────────
            state.bannerNotification?.let { msg ->
                BannerNotificationBar(
                    message = msg,
                    onDismiss = { state.dismissBanner() }
                )
            }

            // ── Main Body based on selected Tab ───────────────────────────────
            when (state.currentTab) {
                OpsConsoleState.ConsoleTab.DISPATCH -> {
                    DispatchTabView(
                        state = state,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedService = selectedServiceFilter,
                        onServiceChange = { selectedServiceFilter = it },
                        nowMillis = nowMillis,
                        onNewOrder = { showCreateDialog = true },
                        onRefresh = { scope.launch { state.refresh() } },
                        onOpenInvoice = { showInvoiceDialog = true },
                        onOpenAssign = { showAssignDialog = true }
                    )
                }
                OpsConsoleState.ConsoleTab.FLEET -> {
                    FleetTabView(
                        state = state,
                        nowMillis = nowMillis,
                        onInspectOrder = { orderId ->
                            state.selectedOrderId = orderId
                            state.currentTab = OpsConsoleState.ConsoleTab.DISPATCH
                        }
                    )
                }
                OpsConsoleState.ConsoleTab.ANALYTICS -> {
                    AnalyticsTabView(state = state)
                }
                OpsConsoleState.ConsoleTab.SYSTEM -> {
                    SystemSettingsTabView(
                        state = state,
                        onSeed = { scope.launch { state.seedOrdersToFirestore() } },
                        onExport = { state.exportOrdersCsv() }
                    )
                }
            }
        }

        // ── Dialogs ───────────────────────────────────────────────────────────
        if (showCreateDialog) {
            CreateOrderDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { newOrder ->
                    showCreateDialog = false
                    scope.launch { state.addOrder(newOrder) }
                }
            )
        }

        if (showInvoiceDialog && state.selectedOrder != null) {
            InvoiceDialog(
                order = state.selectedOrder!!,
                onDismiss = { showInvoiceDialog = false }
            )
        }

        if (showAssignDialog && state.selectedOrder != null) {
            AssignRiderDialog(
                order = state.selectedOrder!!,
                fleetRiders = state.fleetRiders,
                onDismiss = { showAssignDialog = false },
                onAssign = { rider ->
                    showAssignDialog = false
                    scope.launch { state.assignRider(state.selectedOrder!!, rider) }
                }
            )
        }
    }
}

// ── Top Navigation Bar ────────────────────────────────────────────────────────

@Composable
private fun TopNavigationBar(
    state: OpsConsoleState,
    nowMillis: Long,
    onNewOrder: () -> Unit,
    onRefresh: () -> Unit
) {
    val timeStr = remember(nowMillis) {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(nowMillis))
    }
    val dateStr = remember(nowMillis) {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(Date(nowMillis))
    }

    Column(modifier = Modifier.fillMaxWidth().background(Ink)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Brand
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Sky),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = 16.sp)
                }
                Column {
                    Text(
                        "ZYPHUEL",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text("Central Command Console", color = SkyBright, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(8.dp))

            // Navigation Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OpsConsoleState.ConsoleTab.entries.forEach { tab ->
                    val isSelected = state.currentTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Sky else InkSoft)
                            .border(1.dp, if (isSelected) SkyBright else InkMid, RoundedCornerShape(8.dp))
                            .clickable { state.currentTab = tab }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(tab.icon, fontSize = 12.sp)
                            Text(
                                tab.title,
                                color = if (isSelected) Color.White else SlateLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Sound Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(InkSoft)
                    .clickable { state.soundAlertsEnabled = !state.soundAlertsEnabled }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("btn_toggle_sound"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.soundAlertsEnabled) "🔔 Beep ON" else "🔕 Muted",
                    color = if (state.soundAlertsEnabled) GreenTone else SlateLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Connection Status Pill
            ConnectionStatusPill(state = state)

            // Clock
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    timeStr,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(dateStr, color = SlateLight, fontSize = 9.sp)
            }
        }

        HorizontalDivider(color = InkMid, thickness = 1.dp)
    }
}

@Composable
private fun ConnectionStatusPill(state: OpsConsoleState) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    val (color, text) = when {
        state.isDemoMode -> AmberTone to "⚡ DEMO SIMULATION"
        state.connState == OpsConsoleState.ConnState.CONNECTING -> SlateLight.copy(alpha = alpha) to "CONNECTING..."
        state.connState == OpsConsoleState.ConnState.CONNECTED -> GreenTone to "LIVE FIRESTORE"
        state.connState == OpsConsoleState.ConnState.SETUP_NEEDED -> AmberTone to "DB SETUP NEEDED"
        else -> RedTone.copy(alpha = alpha) to "OFFLINE / ERROR"
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(InkSoft)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { state.toggleDemo() }
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .testTag("btn_status_pill"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

// ── Banner Notification ───────────────────────────────────────────────────────

@Composable
private fun BannerNotificationBar(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF065F46))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(message, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
                .clickable(onClick = onDismiss)
                .testTag("btn_dismiss_banner"),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = Color.White, fontSize = 10.sp)
        }
    }
}

// ── TAB 1: Dispatch View ──────────────────────────────────────────────────────

@Composable
private fun DispatchTabView(
    state: OpsConsoleState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedService: String,
    onServiceChange: (String) -> Unit,
    nowMillis: Long,
    onNewOrder: () -> Unit,
    onRefresh: () -> Unit,
    onOpenInvoice: () -> Unit,
    onOpenAssign: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column: Order Queue
        OrderListSidebar(
            state = state,
            searchQuery = searchQuery,
            onSearchChange = onSearchChange,
            selectedService = selectedService,
            onServiceChange = onServiceChange,
            nowMillis = nowMillis,
            onNewOrder = onNewOrder,
            onRefresh = onRefresh,
            modifier = Modifier.width(380.dp).fillMaxHeight()
        )

        // Right Column: Map + Order Telemetry Card
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val order = state.selectedOrder
            val live = state.liveFor(order)

            // Map View with live controls
            TileMapView(
                markers = buildMarkers(order, live),
                route = buildRoute(order, live),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            // Setup banner if Firestore DB not found
            if (state.connState == OpsConsoleState.ConnState.SETUP_NEEDED) {
                FirebaseSetupCard(
                    state = state,
                    onSeed = { scope.launch { state.seedOrdersToFirestore() } }
                )
            } else if (order != null) {
                OrderDetailCard(
                    order = order,
                    live = live,
                    isBusy = state.isBusy,
                    nowMillis = nowMillis,
                    onStatus = { s -> scope.launch { state.changeStatus(order, s) } },
                    onInvoice = onOpenInvoice,
                    onAssign = onOpenAssign
                )
            } else {
                EmptyDetailCard(
                    connState = state.connState,
                    isDemoMode = state.isDemoMode,
                    hasLoaded = state.hasLoadedOnce,
                    onSwitchToDemo = { state.toggleDemo() },
                    onNewOrder = onNewOrder
                )
            }
        }
    }
}

// ── Left Sidebar (Order List) ─────────────────────────────────────────────────

@Composable
private fun OrderListSidebar(
    state: OpsConsoleState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedService: String,
    onServiceChange: (String) -> Unit,
    nowMillis: Long,
    onNewOrder: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val serviceOptions = listOf("All", "Petrol", "Diesel", "Octane", "Water", "LPG")

    Column(modifier = modifier.background(Ink)) {
        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // New Order Button
            Button(
                onClick = onNewOrder,
                colors = ButtonDefaults.buttonColors(containerColor = Sky),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).height(34.dp).testTag("btn_new_order")
            ) {
                Text("➕ Dispatch Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Refresh Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(InkSoft)
                    .border(1.dp, InkMid, RoundedCornerShape(8.dp))
                    .clickable(onClick = onRefresh)
                    .testTag("btn_refresh_orders"),
                contentAlignment = Alignment.Center
            ) {
                Text("🔄", fontSize = 13.sp)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by #ID, customer, location...", fontSize = 11.sp, color = Slate) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .testTag("input_search_orders"),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Status Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            OpsConsoleState.Filter.entries.forEach { filterOpt ->
                val selected = state.filter == filterOpt
                val count = when (filterOpt) {
                    OpsConsoleState.Filter.ACTIVE -> state.orders.count { it.isActive }
                    OpsConsoleState.Filter.FINISHED -> state.orders.count { it.isFinished }
                    OpsConsoleState.Filter.ALL -> state.orders.size
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) Sky else InkSoft)
                        .clickable { state.filter = filterOpt }
                        .padding(vertical = 5.dp)
                        .testTag("filter_${filterOpt.label}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${filterOpt.label} ($count)",
                        color = if (selected) Color.White else SlateLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Fuel Type Chip Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            serviceOptions.forEach { opt ->
                val selected = selectedService == opt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (selected) Sky.copy(alpha = 0.3f) else InkSoft)
                        .border(1.dp, if (selected) Sky else InkMid, RoundedCornerShape(5.dp))
                        .clickable { onServiceChange(opt) }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                        .testTag("chip_service_$opt"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        opt,
                        color = if (selected) SkyBright else SlateLight,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Order List
        val query = searchQuery.trim().lowercase()
        val filteredOrders = state.visibleOrders.filter { o ->
            val matchQuery = query.isEmpty() ||
                o.customerName.lowercase().contains(query) ||
                o.id.toString().contains(query) ||
                o.deliveryAddress.lowercase().contains(query) ||
                o.serviceType.lowercase().contains(query) ||
                o.status.lowercase().contains(query)

            val matchService = selectedService == "All" || o.serviceType.contains(selectedService, ignoreCase = true)

            matchQuery && matchService
        }

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val message = when {
                        !state.hasLoadedOnce -> "Connecting to Firebase..."
                        query.isNotEmpty() -> "No orders matching \"$searchQuery\""
                        state.connState == OpsConsoleState.ConnState.CONNECTED -> "No ${state.filter.label.lowercase()} orders from app"
                        state.isDemoMode -> "No orders in this category"
                        else -> "No orders found"
                    }
                    Text(message, color = Slate, fontSize = 11.sp, textAlign = TextAlign.Center)
                    if (!state.isDemoMode && state.connState != OpsConsoleState.ConnState.CONNECTED) {
                        Button(
                            onClick = { state.toggleDemo() },
                            colors = ButtonDefaults.buttonColors(containerColor = Sky),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_load_demo_orders")
                        ) {
                            Text("⚡ Try Demo Mode", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    OrderListCard(
                        order = order,
                        live = state.livePositions[order.id],
                        selected = state.selectedOrder?.id == order.id,
                        nowMillis = nowMillis,
                        onClick = { state.selectedOrderId = order.id }
                    )
                }
            }
        }
    }
}

// ── Order Card in List ────────────────────────────────────────────────────────

@Composable
private fun OrderListCard(
    order: DesktopOrder,
    live: RiderPosition?,
    selected: Boolean,
    nowMillis: Long,
    onClick: () -> Unit
) {
    val hasLiveFix = live != null && !live.isStale(nowMillis)
    val color = statusColor(order.status)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF0F2848) else InkSoft)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) SkyBright else InkMid,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag("order_row_${order.id}")
    ) {
        // Status indicator stripe
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(color, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        )

        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${order.id}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(5.dp))
                Text(serviceIcon(order.serviceType), fontSize = 11.sp)
                Spacer(Modifier.width(3.dp))
                Text(
                    order.serviceType,
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(order.status)
            }

            Spacer(Modifier.height(3.dp))

            Text(
                "👤 ${order.customerName} · ${order.deliveryAddress}",
                color = SlateLight,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Rs. ${formatMoney(order.totalPrice)}",
                    color = SkyBright,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    order.paymentMethod,
                    color = Slate,
                    fontSize = 8.sp
                )
                Spacer(Modifier.weight(1f))

                if (hasLiveFix) {
                    Box(modifier = Modifier.size(5.dp).background(GreenTone, CircleShape))
                    Spacer(Modifier.width(3.dp))
                    Text("LIVE GPS", color = GreenTone, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.width(4.dp))
                Text(agoText(nowMillis - order.createdAt), color = Slate, fontSize = 8.sp)
            }
        }
    }
}

// ── Selected Order Detail Card ────────────────────────────────────────────────

@Composable
private fun OrderDetailCard(
    order: DesktopOrder,
    live: RiderPosition?,
    isBusy: Boolean,
    nowMillis: Long,
    onStatus: (String) -> Unit,
    onInvoice: () -> Unit,
    onAssign: () -> Unit
) {
    val hasLiveFix = live != null && !live.isStale(nowMillis)
    val remainingKm = if (hasLiveFix) haversineKm(live!!.position, order.destination) else null
    val progress = if (hasLiveFix) tripProgress(order.origin, live!!.position, order.destination) else 0f
    val eta = if (hasLiveFix && remainingKm != null) estimateEtaMinutes(remainingKm, live!!.speedKmh, order.etaMinutes) else order.etaMinutes
    val createdStr = SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(order.createdAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, PaperMid, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Customer & Delivery Info
        Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(serviceIcon(order.serviceType), fontSize = 20.sp)
                Spacer(Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Order #${order.id}", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        StatusPill(order.status)
                    }
                    Text(
                        "${order.serviceType} · ${order.quantity} units · Rs. ${formatMoney(order.totalPrice)}",
                        color = Slate,
                        fontSize = 10.sp
                    )
                }
            }

            HorizontalDivider(color = PaperMid, thickness = 1.dp)

            InfoRow("👤 Customer", order.customerName)
            InfoRow("📞 Phone", order.customerPhone.ifBlank { "—" })
            if (order.customerEmail.isNotBlank()) InfoRow("📧 Email", order.customerEmail)
            InfoRow("📍 Destination", order.deliveryAddress)
            InfoRow("💳 Payment", order.paymentMethod)
            InfoRow("🕒 Created At", createdStr)

            HorizontalDivider(color = PaperMid, thickness = 1.dp)

            // Rider Row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🏍️ Assigned Bowser Rider", color = Slate, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        order.riderName ?: "Unassigned (Click Assign to dispatch)",
                        color = if (order.riderName != null) Ink else AmberTone,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onAssign,
                    colors = ButtonDefaults.buttonColors(containerColor = if (order.riderName == null) AmberTone else Sky),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp).testTag("btn_assign_rider_open")
                ) {
                    Text(if (order.riderName == null) "👤 Assign Rider" else "Change Rider", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Right Column: Telemetry & Actions
        Column(modifier = Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            // Telemetry Metric Tiles
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                TelemetryStatTile("Distance", remainingKm?.let { formatKm(it) } ?: "—", modifier = Modifier.weight(1f))
                TelemetryStatTile("Dynamic ETA", "$eta min", modifier = Modifier.weight(1f))
                TelemetryStatTile("Speed", if (hasLiveFix) "${live!!.speedKmh.toInt()} km/h" else "—", modifier = Modifier.weight(1f))
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Delivery Progress", color = Slate, fontSize = 9.sp)
                    Text("${(progress * 100).toInt()}%", color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PaperMid)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(Sky, RoundedCornerShape(4.dp))
                    )
                }
            }

            HorizontalDivider(color = PaperMid, thickness = 1.dp)

            // Status Transitions
            Text("Update Status:", color = Slate, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("Assigned", "Delivering", "Completed").forEach { status ->
                    Button(
                        onClick = { onStatus(status) },
                        enabled = !isBusy && order.status != status,
                        colors = ButtonDefaults.buttonColors(containerColor = statusColor(status)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                        modifier = Modifier.weight(1f).height(28.dp).testTag("btn_status_$status")
                    ) {
                        Text(status, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = { onStatus("Cancelled") },
                    enabled = !isBusy && !order.isFinished,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                    modifier = Modifier.weight(0.9f).height(28.dp).testTag("btn_cancel_order")
                ) {
                    Text("Cancel", fontSize = 9.sp, color = RedTone, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = PaperMid, thickness = 1.dp)

            // Tools (Invoice, Maps, Email)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onInvoice,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("btn_view_invoice")
                ) {
                    Text("📄 Invoice", fontSize = 9.sp, color = Ink, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val dest = order.destination
                            val url = "https://maps.google.com/?q=${dest.lat},${dest.lng}"
                            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                                java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                            }
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("btn_share_location")
                ) {
                    Text("📍 Google Maps", fontSize = 9.sp, color = Sky, fontWeight = FontWeight.Bold)
                }

                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Sky)
                }
            }
        }
    }
}

// ── TAB 2: Fleet & Bowsers Overview ───────────────────────────────────────────

@Composable
private fun FleetTabView(
    state: OpsConsoleState,
    nowMillis: Long,
    onInspectOrder: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Zyphuel Bowsers & Mobile Tanker Fleet", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Real-time monitoring of all active fuel bowsers, water carriers, and drivers.",
                    color = Slate,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Sky.copy(alpha = 0.1f))
                    .border(1.dp, Sky.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("🚚 ${state.fleetRiders.size} Registered Tankers in Fleet", color = Sky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.fleetRiders, key = { it.id }) { rider ->
                // Check if this rider is currently assigned to an active order
                val activeOrder = state.orders.firstOrNull {
                    it.isActive && (it.riderEmail == rider.email || it.riderName == rider.name)
                }
                val live = activeOrder?.let { state.livePositions[it.id] }
                val isReportingGps = live != null && !live.isStale(nowMillis)

                FleetRiderCard(
                    rider = rider,
                    activeOrder = activeOrder,
                    live = live,
                    isReportingGps = isReportingGps,
                    onInspectOrder = { activeOrder?.let { onInspectOrder(it.id) } }
                )
            }
        }
    }
}

@Composable
private fun FleetRiderCard(
    rider: DesktopFleetRider,
    activeOrder: DesktopOrder?,
    live: RiderPosition?,
    isReportingGps: Boolean,
    onInspectOrder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, PaperMid, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(InkSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👨‍✈️", fontSize = 20.sp)
                }
                Column {
                    Text(rider.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${rider.id} · ${rider.phone}", color = Slate, fontSize = 10.sp)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isReportingGps) GreenTone.copy(alpha = 0.15f) else AmberTone.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    if (isReportingGps) "🟢 En Route (GPS Live)" else if (activeOrder != null) "🟡 Assigned (Idle)" else "⚪ Ready at Depot",
                    color = if (isReportingGps) GreenTone else AmberTone,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(color = PaperMid, thickness = 1.dp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryStatTile("Vehicle", rider.vehicleName, modifier = Modifier.weight(1f))
            TelemetryStatTile("Registration", rider.vehicleNumber, modifier = Modifier.weight(0.7f))
            TelemetryStatTile("Capacity", "${rider.capacityLiters} L", modifier = Modifier.weight(0.6f))
        }

        // Active Mission Section
        if (activeOrder != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Paper)
                    .border(1.dp, PaperMid, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Delivering Order #${activeOrder.id}: ${activeOrder.serviceType} (${activeOrder.quantity} units)",
                            color = Ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Destination: ${activeOrder.deliveryAddress}",
                            color = Slate,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onInspectOrder,
                        colors = ButtonDefaults.buttonColors(containerColor = Sky),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("btn_inspect_order_${activeOrder.id}")
                    ) {
                        Text("Track ❯", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Text(
                "Currently available for new dispatch missions from Lahore Depot.",
                color = Slate,
                fontSize = 10.sp
            )
        }
    }
}

// ── TAB 3: Analytics & Revenue ────────────────────────────────────────────────

@Composable
private fun AnalyticsTabView(state: OpsConsoleState) {
    val allOrders = state.orders
    val finishedOrders = allOrders.filter { it.isFinished }
    val totalRevenue = finishedOrders.sumOf { it.totalPrice }
    val avgOrderValue = if (finishedOrders.isNotEmpty()) totalRevenue / finishedOrders.size else 0.0

    // Volume by service type
    val volumeByService = allOrders.groupBy { it.serviceType }
        .mapValues { entry -> entry.value.sumOf { it.quantity } }

    val revenueByService = allOrders.groupBy { it.serviceType }
        .mapValues { entry -> entry.value.sumOf { it.totalPrice } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("Operations Analytics & Revenue Intelligence", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Real-time revenue metrics, fuel delivery volume, and payment method statistics.", color = Slate, fontSize = 11.sp)
        }

        // Top Metrics
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticsKpiCard("Total Sales Revenue", "Rs. ${formatMoney(totalRevenue)}", "💰", GreenTone, modifier = Modifier.weight(1f))
            AnalyticsKpiCard("Completed Deliveries", "${finishedOrders.size} orders", "✅", Sky, modifier = Modifier.weight(1f))
            AnalyticsKpiCard("Average Ticket", "Rs. ${formatMoney(avgOrderValue)}", "📊", PurpleTone, modifier = Modifier.weight(1f))
            AnalyticsKpiCard("Active Orders Queue", "${allOrders.count { it.isActive }} in progress", "⏳", AmberTone, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Product Volume Breakdown Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, PaperMid, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Volume Breakdown by Service", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                val maxVolume = volumeByService.values.maxOrNull()?.coerceAtLeast(1) ?: 1

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(volumeByService.entries.toList(), key = { it.key }) { (service, vol) ->
                        val pct = vol.toFloat() / maxVolume.toFloat()
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$service (${serviceIcon(service)})", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("$vol units", color = Sky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PaperMid)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(pct)
                                        .background(Sky, RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Payment Methods & Revenue Share Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, PaperMid, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Revenue Share by Product", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(revenueByService.entries.toList(), key = { it.key }) { (service, rev) ->
                        val sharePct = if (totalRevenue > 0) ((rev / totalRevenue) * 100).toInt() else 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Paper)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(serviceIcon(service), fontSize = 16.sp)
                                Column {
                                    Text(service, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("$sharePct% of total sales", color = Slate, fontSize = 9.sp)
                                }
                            }
                            Text("Rs. ${formatMoney(rev)}", color = GreenTone, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    icon: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, PaperMid, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Slate, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(icon, fontSize = 16.sp)
        }
        Text(value, color = accentColor, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

// ── TAB 4: Cloud & System Settings ────────────────────────────────────────────

@Composable
private fun SystemSettingsTabView(
    state: OpsConsoleState,
    onSeed: () -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("System Architecture & Cloud Integrations", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Backend connection diagnostics, database initialization, and data export tools.", color = Slate, fontSize = 11.sp)
        }

        // Connection Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, PaperMid, RoundedCornerShape(12.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Cloud Firestore Diagnostic Panel", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            InfoRow("Firebase Project ID", state.rest.config.projectId)
            InfoRow("Database Target", "(default)")
            InfoRow(
                "Connection State",
                when (state.connState) {
                    OpsConsoleState.ConnState.CONNECTED -> "🟢 Connected & Polling (Live REST API)"
                    OpsConsoleState.ConnState.SETUP_NEEDED -> "🟡 Setup Needed (Database not created on Cloud)"
                    OpsConsoleState.ConnState.ERROR -> "🔴 Connection Error / Network Unreachable"
                    OpsConsoleState.ConnState.CONNECTING -> "⚪ Connecting..."
                }
            )
            InfoRow("Current Mode", if (state.isDemoMode) "⚡ Interactive Demo Simulation" else "🟢 Live Production Sync")
            InfoRow("Total Cached Orders", "${state.orders.size} orders")
            InfoRow("Online Active Riders", "${state.ridersOnline} riders reporting live GPS")

            HorizontalDivider(color = PaperMid, thickness = 1.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { state.openFirebaseConsoleInBrowser() },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberTone),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_open_firebase_settings")
                ) {
                    Text("🌐 Open Firebase Console", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSeed,
                    colors = ButtonDefaults.buttonColors(containerColor = Sky),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_seed_firestore_settings")
                ) {
                    Text("🌱 Seed 4 Sample Orders to Cloud", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExport,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_export_csv_settings")
                ) {
                    Text("📥 Export All to CSV (Clipboard)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Firebase Setup Card ───────────────────────────────────────────────────────

@Composable
private fun FirebaseSetupCard(state: OpsConsoleState, onSeed: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFFBEB))
            .border(1.5.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("⚠️ Cloud Firestore Database Not Created in Project '${state.rest.config.projectId}'", color = Color(0xFF92400E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            "Both the Android mobile app and this desktop console require the Firestore database to transfer live orders. Please create it once:",
            color = Color(0xFFB45309),
            fontSize = 11.sp
        )

        listOf(
            "1️⃣  Click 'Open Firebase Console' below to open the setup page",
            "2️⃣  Click 'Create database' button",
            "3️⃣  Choose location: asia-south1 (Mumbai) — closest region to Pakistan",
            "4️⃣  Choose Security Rules: 'Start in test mode'",
            "5️⃣  Click 'Create' (ready in 30 seconds)",
            "6️⃣  Return here and click 'Seed Sample Orders' to populate initial data"
        ).forEach { step ->
            Text(step, color = Color(0xFF92400E), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { state.openFirebaseConsoleInBrowser() },
                colors = ButtonDefaults.buttonColors(containerColor = AmberTone),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_create_db_browser")
            ) {
                Text("🌐 Open Firebase Console", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onSeed,
                colors = ButtonDefaults.buttonColors(containerColor = Sky),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_seed_firestore")
            ) {
                Text("🌱 Seed Sample Orders", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Empty Detail Card ─────────────────────────────────────────────────────────

@Composable
private fun EmptyDetailCard(
    connState: OpsConsoleState.ConnState,
    isDemoMode: Boolean,
    hasLoaded: Boolean,
    onSwitchToDemo: () -> Unit,
    onNewOrder: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, PaperMid, RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val (emoji, msg) = when {
                isDemoMode -> "⚡" to "Demo mode active.\nSelect any order from the queue to inspect live vehicle telematics."
                !hasLoaded || connState == OpsConsoleState.ConnState.CONNECTING ->
                    "⏳" to "Connecting to Firebase Firestore…"
                connState == OpsConsoleState.ConnState.CONNECTED ->
                    "✅" to "Connected to Firebase.\nNo orders in queue. Place an order from the mobile app or dispatch one now."
                else -> "👈" to "Select an order on the left to track and manage."
            }
            Text(emoji, fontSize = 26.sp)
            Text(msg, color = Slate, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (connState != OpsConsoleState.ConnState.CONNECTED || isDemoMode) {
                    Button(
                        onClick = onSwitchToDemo,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDemoMode) InkSoft else Sky),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_empty_switch_demo")
                    ) {
                        Text(if (isDemoMode) "🔴 Exit Demo" else "⚡ Try Demo Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onNewOrder,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_empty_new_order")
                ) {
                    Text("➕ Dispatch Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Invoice / Receipt Dialog ──────────────────────────────────────────────────

@Composable
private fun InvoiceDialog(order: DesktopOrder, onDismiss: () -> Unit) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(order.createdAt))

    val invoiceText = """
========================================
           ZYPHUEL FUEL DISPATCH
          TAX INVOICE & CASH RECEIPT
========================================
Order ID      : #${order.id}
Date & Time   : $dateStr
Status        : ${order.status.uppercase()}

CUSTOMER DETAILS:
Name          : ${order.customerName}
Phone         : ${order.customerPhone}
Address       : ${order.deliveryAddress}

ORDER DETAILS:
Service / Fuel: ${order.serviceType}
Quantity      : ${order.quantity} Units / Liters
Amount (PKR)  : Rs. ${formatMoney(order.totalPrice)}
Payment Method: ${order.paymentMethod}

DISPATCH & FLEET:
Depot Origin  : Zyphuel Lahore Central
Assigned Rider: ${order.riderName ?: "Unassigned"}
========================================
    Thank you for choosing Zyphuel!
========================================
""".trimIndent()

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, PaperMid, RoundedCornerShape(14.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📄 Order Invoice & Receipt", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Paper)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 11.sp, color = Slate)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Paper)
                    .border(1.dp, PaperMid, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = invoiceText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Ink,
                    lineHeight = 14.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        try {
                            val sel = java.awt.datatransfer.StringSelection(invoiceText)
                            java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
                        } catch (_: Exception) {}
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Sky),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_copy_invoice")
                ) {
                    Text("📋 Copy Invoice to Clipboard", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.5f)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

// ── Assign Rider Dialog ───────────────────────────────────────────────────────

@Composable
private fun AssignRiderDialog(
    order: DesktopOrder,
    fleetRiders: List<DesktopFleetRider>,
    onDismiss: () -> Unit,
    onAssign: (DesktopFleetRider) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, PaperMid, RoundedCornerShape(14.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("👤 Assign Bowser Rider to Order #${order.id}", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Select an available bowser tanker driver to fulfill this delivery:", color = Slate, fontSize = 11.sp)

            HorizontalDivider(color = PaperMid, thickness = 1.dp)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(280.dp)) {
                items(fleetRiders, key = { it.id }) { rider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Paper)
                            .border(1.dp, PaperMid, RoundedCornerShape(8.dp))
                            .clickable { onAssign(rider) }
                            .padding(10.dp)
                            .testTag("btn_select_rider_${rider.id}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🚚", fontSize = 18.sp)
                            Column {
                                Text(rider.name, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${rider.vehicleName} · ${rider.vehicleNumber}", color = Slate, fontSize = 9.sp)
                                Text("Capacity: ${rider.capacityLiters} L · ${rider.phone}", color = Sky, fontSize = 9.sp)
                            }
                        }

                        Button(
                            onClick = { onAssign(rider) },
                            colors = ButtonDefaults.buttonColors(containerColor = Sky),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Assign ❯", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// ── Create Order Dialog ───────────────────────────────────────────────────────

@Composable
private fun CreateOrderDialog(
    onDismiss: () -> Unit,
    onConfirm: (DesktopOrder) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("+92 3") }
    var customerEmail by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("Super Petrol") }
    var quantity by remember { mutableStateOf("25") }
    var totalPrice by remember { mutableStateOf("6950") }
    var deliveryAddress by remember { mutableStateOf("Gulberg III, Lahore") }
    var paymentMethod by remember { mutableStateOf("Cash on Delivery") }

    val servicePrices = listOf(
        "Super Petrol" to 278,
        "High-Speed Diesel" to 280,
        "High Octane 97" to 315,
        "Drinking Water" to 50,
        "LPG Gas" to 2800
    )
    val paymentOptions = listOf("Cash on Delivery", "Online Paid", "Card")

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, PaperMid, RoundedCornerShape(16.dp))
                .clickable(enabled = false) {}
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("➕ Dispatch New Fuel/Water Delivery", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Fill in delivery details to publish directly to mobile riders.", color = Slate, fontSize = 11.sp)

            HorizontalDivider(color = PaperMid, thickness = 1.dp)

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Customer Full Name *") },
                modifier = Modifier.fillMaxWidth().testTag("input_customer_name"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Phone *") },
                    modifier = Modifier.weight(1f).testTag("input_customer_phone"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = customerEmail,
                    onValueChange = { customerEmail = it },
                    label = { Text("Email (optional)") },
                    modifier = Modifier.weight(1f).testTag("input_customer_email"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Text("Product / Fuel Type:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                servicePrices.forEach { (name, unitPrice) ->
                    val selected = serviceType == name
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) Sky else Paper)
                            .border(1.dp, if (selected) Sky else PaperMid, RoundedCornerShape(6.dp))
                            .clickable {
                                serviceType = name
                                val q = quantity.toIntOrNull() ?: 20
                                totalPrice = (q * unitPrice).toString()
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.substringBefore(" ") + "\n${serviceIcon(name)}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else Ink,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        val q = it.toIntOrNull() ?: 1
                        val unitPrice = servicePrices.firstOrNull { (n, _) -> n == serviceType }?.second ?: 278
                        totalPrice = (q * unitPrice).toString()
                    },
                    label = { Text("Quantity (Liters/Units)") },
                    modifier = Modifier.weight(1f).testTag("input_quantity"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = "Rs. $totalPrice",
                    onValueChange = { totalPrice = it.removePrefix("Rs. ") },
                    label = { Text("Total Price") },
                    modifier = Modifier.weight(1f).testTag("input_total_price"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            OutlinedTextField(
                value = deliveryAddress,
                onValueChange = { deliveryAddress = it },
                label = { Text("Delivery Address (Lahore) *") },
                modifier = Modifier.fillMaxWidth().testTag("input_delivery_address"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Text("Payment Method:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                paymentOptions.forEach { opt ->
                    val selected = paymentMethod == opt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) Sky else Paper)
                            .border(1.dp, if (selected) Sky else PaperMid, RoundedCornerShape(6.dp))
                            .clickable { paymentMethod = opt }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            opt,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else Ink
                        )
                    }
                }
            }

            HorizontalDivider(color = PaperMid, thickness = 1.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).testTag("btn_cancel_dispatch"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val newId = (1000..9999).random()
                        val q = quantity.toIntOrNull() ?: 25
                        val p = totalPrice.removePrefix("Rs. ").toDoubleOrNull() ?: 6950.0
                        val destLat = 31.4800 + (Math.random() * 0.08 - 0.04)
                        val destLng = 74.3400 + (Math.random() * 0.08 - 0.04)
                        onConfirm(
                            DesktopOrder(
                                id = newId,
                                customerName = customerName.ifBlank { "Customer" },
                                customerPhone = customerPhone,
                                customerEmail = customerEmail,
                                serviceType = serviceType,
                                quantity = q,
                                totalPrice = p,
                                deliveryAddress = deliveryAddress.ifBlank { "Lahore, Punjab" },
                                paymentMethod = paymentMethod,
                                status = "Pending",
                                riderName = null,
                                riderEmail = null,
                                createdAt = System.currentTimeMillis(),
                                etaMinutes = 25,
                                destLat = destLat,
                                destLng = destLng,
                                originLat = DEPOT_LAT,
                                originLng = DEPOT_LNG
                            )
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("btn_confirm_dispatch"),
                    colors = ButtonDefaults.buttonColors(containerColor = Sky),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🚀 Dispatch Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Map Markers and Route Builder ─────────────────────────────────────────────

private fun buildMarkers(order: DesktopOrder?, live: RiderPosition?): List<MapMarker> {
    if (order == null) return emptyList()
    val now = System.currentTimeMillis()
    val markers = mutableListOf(
        MapMarker(order.origin, Color(0xFF1E293B), "Lahore Depot", radiusPx = 7f),
        MapMarker(order.destination, RedTone, "Destination (${order.customerName})", radiusPx = 9f)
    )
    if (live != null) {
        markers += MapMarker(
            position = live.position,
            color = if (live.isStale(now)) AmberTone else GreenTone,
            label = "Bowser Rider" + (order.riderName?.let { " · $it" } ?: ""),
            bearing = live.bearing,
            radiusPx = 11f
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

// ── Helper Composables and Formatters ─────────────────────────────────────────

@Composable
private fun StatusPill(status: String) {
    val c = statusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(c.copy(alpha = 0.15f))
            .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(status, color = c, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Slate, fontSize = 9.sp, modifier = Modifier.width(90.dp))
        Text(value, color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TelemetryStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Paper)
            .border(1.dp, PaperMid, RoundedCornerShape(6.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Slate, fontSize = 8.sp)
    }
}

private fun statusColor(status: String): Color = when (status) {
    "Pending" -> AmberTone
    "Assigned", "Accepted" -> Sky
    "Delivering", "On the way" -> PurpleTone
    "Completed", "Delivered" -> GreenTone
    "Cancelled", "Canceled" -> RedTone
    else -> Slate
}

private fun serviceIcon(type: String): String = when {
    type.contains("Petrol", ignoreCase = true) -> "⛽"
    type.contains("Diesel", ignoreCase = true) -> "🛢️"
    type.contains("Octane", ignoreCase = true) -> "🔥"
    type.contains("Water", ignoreCase = true) -> "💧"
    type.contains("LPG", ignoreCase = true) -> "🔵"
    else -> "📦"
}

private fun formatMoney(v: Double): String = String.format(Locale.US, "%,.0f", v)

private fun formatKm(v: Double): String =
    if (v < 1.0) String.format(Locale.US, "%.0f m", v * 1000)
    else String.format(Locale.US, "%.1f km", v)

private fun agoText(millis: Long): String {
    if (millis < 0) return "just now"
    val s = millis / 1000
    return when {
        s < 10 -> "just now"
        s < 60 -> "${s}s ago"
        s < 3600 -> "${s / 60}m ago"
        else -> "${s / 3600}h ago"
    }
}
