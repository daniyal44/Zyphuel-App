package com.example.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

/**
 * All console state in one place, plus the polling loops that keep it fresh.
 *
 * Orders are re-read every 5 s; live rider positions every 3 s — matching the phone's
 * ~4 s GPS write cadence without hammering the API.
 *
 * Connection is always attempted against the real Firestore backend.
 * Demo simulation is only available as a manual opt-in (toggled by the user).
 */
class OpsConsoleState(val rest: FirestoreRest) {

    // ── Navigation Tabs ──────────────────────────────────────────────────────
    enum class ConsoleTab(val title: String, val icon: String) {
        DISPATCH("Live Dispatch", "🚚"),
        FLEET("Fleet & Bowsers", "🏍️"),
        ANALYTICS("Analytics & Revenue", "📊"),
        SYSTEM("Cloud & Database", "⚙️")
    }

    var currentTab by mutableStateOf(ConsoleTab.DISPATCH)

    // ── Data state ──────────────────────────────────────────────────────────
    var orders by mutableStateOf<List<DesktopOrder>>(emptyList())
        private set

    var livePositions by mutableStateOf<Map<Int, RiderPosition>>(emptyMap())
        private set

    val fleetRiders: List<DesktopFleetRider> = DEFAULT_FLEET_RIDERS

    var selectedFleetRider by mutableStateOf<DesktopFleetRider?>(null)

    // ── Audio & Notification Alerts ──────────────────────────────────────────
    var soundAlertsEnabled by mutableStateOf(true)

    var bannerNotification by mutableStateOf<String?>(null)

    fun dismissBanner() {
        bannerNotification = null
    }

    // ── Connection state ─────────────────────────────────────────────────────
    enum class ConnState { CONNECTING, CONNECTED, ERROR, SETUP_NEEDED }

    var connState by mutableStateOf(ConnState.CONNECTING)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var lastRefreshAt by mutableStateOf(0L)
        private set

    var hasLoadedOnce by mutableStateOf(false)
        private set

    // ── Demo simulation (manual opt-in only) ─────────────────────────────────
    var isDemoMode by mutableStateOf(false)
        private set

    private var demoStep = 0

    // ── UI state ─────────────────────────────────────────────────────────────
    var isBusy by mutableStateOf(false)
        private set

    var selectedOrderId by mutableStateOf<Int?>(null)

    var filter by mutableStateOf(Filter.ACTIVE)

    // ── Computed ─────────────────────────────────────────────────────────────
    val firebaseConsoleUrl: String
        get() = "https://console.firebase.google.com/project/${rest.config.projectId}/firestore"

    fun openFirebaseConsoleInBrowser() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(firebaseConsoleUrl))
            }
        } catch (_: Exception) {}
    }

    enum class Filter(val label: String) {
        ACTIVE("Active"),
        ALL("All"),
        FINISHED("Finished")
    }

    val visibleOrders: List<DesktopOrder>
        get() = when (filter) {
            Filter.ACTIVE  -> orders.filter { it.isActive }
            Filter.FINISHED -> orders.filter { it.isFinished }
            Filter.ALL     -> orders
        }

    val selectedOrder: DesktopOrder?
        get() = selectedOrderId?.let { id -> visibleOrders.firstOrNull { it.id == id } }
            ?: visibleOrders.firstOrNull()
            ?: orders.firstOrNull()

    fun liveFor(order: DesktopOrder?): RiderPosition? =
        order?.let { livePositions[it.id] }

    val ridersOnline: Int
        get() {
            val now = System.currentTimeMillis()
            return livePositions.values.count { !it.isStale(now) }
        }

    // ── Manual demo toggle (user-initiated only) ──────────────────────────────
    fun toggleDemo() {
        if (isDemoMode) {
            // Exit demo → try real connection
            isDemoMode = false
            orders = emptyList()
            livePositions = emptyMap()
            connState = ConnState.CONNECTING
            hasLoadedOnce = false
        } else {
            // Enter demo
            isDemoMode = true
            orders = DEMO_ORDERS
            selectedOrderId = DEMO_ORDERS.firstOrNull()?.id
            updateDemoRiders()
        }
    }

    // ── Polling loops ─────────────────────────────────────────────────────────

    /** Polls orders from real Firestore until coroutine is cancelled. */
    suspend fun pollOrdersForever(intervalMillis: Long = 5_000) {
        while (true) {
            if (!isDemoMode) refreshOrders()
            else updateDemoRiders()
            delay(intervalMillis)
        }
    }

    /** Polls live rider positions until coroutine is cancelled. */
    suspend fun pollLiveTrackingForever(intervalMillis: Long = 3_000) {
        while (true) {
            if (!isDemoMode) refreshLiveTracking()
            delay(intervalMillis)
        }
    }

    /** Compatibility shim — callers that use pollForever() still work. */
    suspend fun pollForever(intervalMillis: Long = 5_000) = pollOrdersForever(intervalMillis)

    /** Manual refresh (button tap). */
    suspend fun refresh() {
        if (isDemoMode) {
            updateDemoRiders()
            return
        }
        refreshOrders()
        refreshLiveTracking()
    }

    private suspend fun refreshOrders() {
        try {
            val previousIds = orders.map { it.id }.toSet()
            val freshOrders = rest.listOrders()

            // New order audio / visual notification
            if (hasLoadedOnce && !isDemoMode && previousIds.isNotEmpty()) {
                val newIncoming = freshOrders.filter { it.id !in previousIds }
                if (newIncoming.isNotEmpty()) {
                    val firstNew = newIncoming.first()
                    bannerNotification = "🎉 New Order #${firstNew.id} (${firstNew.serviceType}) from ${firstNew.customerName}!"
                    if (soundAlertsEnabled) {
                        try {
                            Toolkit.getDefaultToolkit().beep()
                        } catch (_: Exception) {}
                    }
                }
            }

            orders = freshOrders
            lastRefreshAt = System.currentTimeMillis()
            errorMessage = null
            connState = ConnState.CONNECTED
            hasLoadedOnce = true

            // Keep selected order in view; pick first if it disappeared
            if (selectedOrderId != null && orders.none { it.id == selectedOrderId }) {
                selectedOrderId = orders.firstOrNull()?.id
            }
        } catch (e: Exception) {
            val raw = e.message ?: e.toString()
            errorMessage = describe(e)
            hasLoadedOnce = true
            connState = when {
                raw.contains("404") || raw.contains("does not exist") || raw.contains("NOT_FOUND") -> {
                    ConnState.SETUP_NEEDED
                }
                else -> ConnState.ERROR
            }
        }
    }

    private suspend fun refreshLiveTracking() {
        try {
            val freshPositions = rest.listLivePositions().associateBy { it.orderId }
            if (freshPositions.isNotEmpty()) {
                livePositions = freshPositions
            }
        } catch (_: Exception) {
            // Tracking errors are non-fatal; keep last known positions
        }
    }

    // ── Demo simulation ────────────────────────────────────────────────────────
    fun updateDemoRiders() {
        demoStep++
        val factor = ((demoStep % 35) / 35.0).toFloat()
        val r1Lat = DEPOT_LAT + (31.5120 - DEPOT_LAT) * factor
        val r1Lng = DEPOT_LNG + (74.3450 - DEPOT_LNG) * factor
        val r2Lat = DEPOT_LAT + (31.4680 - DEPOT_LAT) * (0.25f + factor * 0.55f)
        val r2Lng = DEPOT_LNG + (74.3980 - DEPOT_LNG) * (0.25f + factor * 0.55f)

        livePositions = mapOf(
            101 to RiderPosition(
                orderId = 101,
                riderEmail = "rider.rashid@zyphuel.com",
                lat = r1Lat, lng = r1Lng,
                bearing = 28f, speedKmh = 38f,
                status = "Delivering",
                updatedAt = System.currentTimeMillis()
            ),
            102 to RiderPosition(
                orderId = 102,
                riderEmail = "rider.hamza@zyphuel.com",
                lat = r2Lat, lng = r2Lng,
                bearing = 52f, speedKmh = 42f,
                status = "Assigned",
                updatedAt = System.currentTimeMillis()
            )
        )
        lastRefreshAt = System.currentTimeMillis()
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    /** Dispatches a new order from the desktop console. */
    suspend fun addOrder(order: DesktopOrder) {
        if (isBusy) return
        isBusy = true
        try {
            if (isDemoMode) {
                orders = listOf(order) + orders
                selectedOrderId = order.id
            } else {
                rest.createOrder(order)
                orders = listOf(order) + orders
                selectedOrderId = order.id
                refreshOrders()
            }
        } catch (e: Exception) {
            errorMessage = describe(e)
            orders = listOf(order) + orders
            selectedOrderId = order.id
        } finally {
            isBusy = false
        }
    }

    /** Assigns a specific fleet bowser rider to an order. */
    suspend fun assignRider(order: DesktopOrder, rider: DesktopFleetRider) {
        if (isBusy) return
        isBusy = true
        try {
            if (isDemoMode) {
                orders = orders.map {
                    if (it.id == order.id) it.copy(status = "Assigned", riderName = rider.name, riderEmail = rider.email) else it
                }
            } else {
                rest.updateOrderStatus(
                    orderId = order.id,
                    status = "Assigned",
                    riderName = rider.name,
                    riderEmail = rider.email
                )
                refreshOrders()
            }
        } catch (e: Exception) {
            errorMessage = describe(e)
        } finally {
            isBusy = false
        }
    }

    /** Seeds the sample demo orders into Cloud Firestore (only when DB exists). */
    suspend fun seedOrdersToFirestore() {
        if (isBusy) return
        isBusy = true
        try {
            for (order in DEMO_ORDERS) rest.createOrder(order)
            isDemoMode = false
            connState = ConnState.CONNECTED
            errorMessage = null
            refreshOrders()
        } catch (e: Exception) {
            errorMessage = "Seed failed: " + describe(e)
        } finally {
            isBusy = false
        }
    }

    /**
     * Moves an order to [newStatus]. Terminal statuses also clear the live-tracking
     * document, exactly as the rider's foreground service does when it stops.
     */
    suspend fun changeStatus(order: DesktopOrder, newStatus: String) {
        if (isBusy) return
        isBusy = true
        try {
            if (isDemoMode) {
                orders = orders.map { if (it.id == order.id) it.copy(status = newStatus) else it }
                if (newStatus in setOf("Completed", "Delivered", "Cancelled", "Canceled")) {
                    livePositions = livePositions - order.id
                }
            } else {
                rest.updateOrderStatus(orderId = order.id, status = newStatus)
                if (newStatus in setOf("Completed", "Delivered", "Cancelled", "Canceled")) {
                    rest.clearLiveTracking(order.id)
                    livePositions = livePositions - order.id
                }
                refreshOrders()
            }
        } catch (e: Exception) {
            errorMessage = describe(e)
        } finally {
            isBusy = false
        }
    }

    /** Exports all current orders to standard CSV and copies to system clipboard. */
    fun exportOrdersCsv(): String {
        val sb = StringBuilder()
        sb.append("Order ID,Customer Name,Phone,Email,Service Type,Quantity,Total Price (PKR),Delivery Address,Payment Method,Status,Rider Name,Rider Email,Created Timestamp\n")
        for (o in orders) {
            sb.append("${o.id},\"${o.customerName.replace("\"", "\"\"")}\",\"${o.customerPhone}\",\"${o.customerEmail}\",\"${o.serviceType}\",${o.quantity},${o.totalPrice},\"${o.deliveryAddress.replace("\"", "\"\"")}\",\"${o.paymentMethod}\",\"${o.status}\",\"${o.riderName ?: ""}\",\"${o.riderEmail ?: ""}\",${o.createdAt}\n")
        }
        val csv = sb.toString()
        try {
            val selection = StringSelection(csv)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            bannerNotification = "📋 Exported ${orders.size} orders to CSV (copied to clipboard)!"
        } catch (_: Exception) {}
        return csv
    }

    // ── Error description ────────────────────────────────────────────────────
    private fun describe(e: Exception): String {
        val raw = e.message ?: e.toString()
        return when {
            raw.contains("HTTP 403") || raw.contains("PERMISSION_DENIED") ->
                "Firestore access denied (HTTP 403).\n\nFirebase Console → Firestore → Rules mein " +
                    "'orders' aur 'live_tracking' collections par read/write allow karein, ya test mode on karein."
            raw.contains("HTTP 404") || raw.contains("does not exist") || raw.contains("NOT_FOUND") ->
                "Firestore database abhi create nahi hui project '${rest.config.projectId}' mein.\n\n" +
                    "Neeche 'Open Firebase' button click karein aur database create karein."
            raw.contains("UnknownHost") || raw.contains("ConnectException") || raw.contains("timed out") ->
                "Firestore reach nahi ho rahi. Internet connection check karein."
            else -> raw
        }
    }

    companion object {
        const val DEPOT_LAT = 31.4380
        const val DEPOT_LNG = 74.3050

        val DEMO_ORDERS = listOf(
            DesktopOrder(
                id = 101, customerName = "Ali Raza",
                customerPhone = "+92 300 8472911", customerEmail = "ali.raza@zyphuel.pk",
                serviceType = "Super Petrol", quantity = 25, totalPrice = 6950.0,
                deliveryAddress = "Main Boulevard, Gulberg III, Lahore",
                paymentMethod = "Cash on Delivery", status = "Delivering",
                riderName = "Rashid Minhas", riderEmail = "rider.rashid@zyphuel.com",
                createdAt = System.currentTimeMillis() - 15 * 60 * 1000L, etaMinutes = 8,
                destLat = 31.5120, destLng = 74.3450, originLat = DEPOT_LAT, originLng = DEPOT_LNG
            ),
            DesktopOrder(
                id = 102, customerName = "Usman Tariq",
                customerPhone = "+92 321 4459821", customerEmail = "usman.tariq@gmail.com",
                serviceType = "High-Speed Diesel", quantity = 40, totalPrice = 11200.0,
                deliveryAddress = "Sector C, DHA Phase 5, Lahore",
                paymentMethod = "Online Paid", status = "Assigned",
                riderName = "Hamza Akram", riderEmail = "rider.hamza@zyphuel.com",
                createdAt = System.currentTimeMillis() - 8 * 60 * 1000L, etaMinutes = 18,
                destLat = 31.4680, destLng = 74.3980, originLat = DEPOT_LAT, originLng = DEPOT_LNG
            ),
            DesktopOrder(
                id = 103, customerName = "Fatima Noor",
                customerPhone = "+92 333 9821045", customerEmail = "fatima.noor@zyphuel.pk",
                serviceType = "Pure Drinking Water (5 Gal)", quantity = 4, totalPrice = 200.0,
                deliveryAddress = "Block G3, Johar Town, Lahore",
                paymentMethod = "Cash on Delivery", status = "Pending",
                riderName = null, riderEmail = null,
                createdAt = System.currentTimeMillis() - 2 * 60 * 1000L, etaMinutes = 25,
                destLat = 31.4690, destLng = 74.2920, originLat = DEPOT_LAT, originLng = DEPOT_LNG
            ),
            DesktopOrder(
                id = 104, customerName = "Zubair Ahmad",
                customerPhone = "+92 301 5592310", customerEmail = "zubair.ahmad@hotmail.com",
                serviceType = "High Octane 97", quantity = 30, totalPrice = 9450.0,
                deliveryAddress = "Model Town Block B, Lahore",
                paymentMethod = "Cash on Delivery", status = "Completed",
                riderName = "Rashid Minhas", riderEmail = "rider.rashid@zyphuel.com",
                createdAt = System.currentTimeMillis() - 75 * 60 * 1000L, etaMinutes = 0,
                destLat = 31.4850, destLng = 74.3210, originLat = DEPOT_LAT, originLng = DEPOT_LNG
            )
        )
    }
}
