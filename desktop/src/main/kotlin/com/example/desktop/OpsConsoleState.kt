package com.example.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.awt.Desktop
import java.net.URI

/**
 * All console state in one place, plus the polling loop that keeps it fresh.
 *
 * Firestore's REST API has no snapshot listeners, so the console re-reads `orders` and
 * `live_tracking` on a short interval. Rider positions are written every ~4s by the phone,
 * so a 4s poll matches the data's real update rate without hammering the API.
 *
 * If Firestore database is not created (HTTP 404), the console automatically provides
 * an interactive demo simulation dataset and a direct link to create the database in
 * the Firebase Console.
 */
class OpsConsoleState(val rest: FirestoreRest) {

    var orders by mutableStateOf<List<DesktopOrder>>(emptyList())
        private set

    var livePositions by mutableStateOf<Map<Int, RiderPosition>>(emptyMap())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isDemoMode by mutableStateOf(false)
        private set

    var firestoreSetupNeeded by mutableStateOf(false)
        private set

    var isBusy by mutableStateOf(false)
        private set

    var lastRefreshAt by mutableStateOf(0L)
        private set

    var hasLoadedOnce by mutableStateOf(false)
        private set

    var selectedOrderId by mutableStateOf<Int?>(null)

    var filter by mutableStateOf(Filter.ACTIVE)

    private var demoStep = 0

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
            Filter.ACTIVE -> orders.filter { it.isActive }
            Filter.FINISHED -> orders.filter { it.isFinished }
            Filter.ALL -> orders
        }

    val selectedOrder: DesktopOrder?
        get() = selectedOrderId?.let { id -> orders.firstOrNull { it.id == id } }
            ?: visibleOrders.firstOrNull()

    fun liveFor(order: DesktopOrder?): RiderPosition? =
        order?.let { livePositions[it.id] }

    /** Count of riders currently reporting a position, for the header. */
    val ridersOnline: Int
        get() {
            val now = System.currentTimeMillis()
            return livePositions.values.count { !it.isStale(now) }
        }

    suspend fun refresh() {
        try {
            val freshOrders = rest.listOrders()
            val freshPositions = rest.listLivePositions().associateBy { it.orderId }
            orders = freshOrders
            livePositions = freshPositions
            lastRefreshAt = System.currentTimeMillis()
            errorMessage = null
            isDemoMode = false
            firestoreSetupNeeded = false
            hasLoadedOnce = true
        } catch (e: Exception) {
            val desc = describe(e)
            errorMessage = desc
            if (desc.contains("404") || desc.contains("does not exist")) {
                firestoreSetupNeeded = true
                isDemoMode = true
                if (orders.isEmpty()) {
                    orders = DEMO_ORDERS
                    selectedOrderId = 101
                }
                updateDemoRiders()
            }
            hasLoadedOnce = true
        }
    }

    private fun updateDemoRiders() {
        demoStep++
        val factor = ((demoStep % 30) / 30.0).toFloat()
        val r1Lat = DEPOT_LAT + (31.5120 - DEPOT_LAT) * factor
        val r1Lng = DEPOT_LNG + (74.3450 - DEPOT_LNG) * factor
        val r2Lat = DEPOT_LAT + (31.4680 - DEPOT_LAT) * (0.3 + factor * 0.5)
        val r2Lng = DEPOT_LNG + (74.3980 - DEPOT_LNG) * (0.3 + factor * 0.5)

        livePositions = mapOf(
            101 to RiderPosition(
                orderId = 101,
                riderEmail = "rider.rashid@zyphuel.com",
                lat = r1Lat,
                lng = r1Lng,
                bearing = 25f,
                speedKmh = 38f,
                status = "Delivering",
                updatedAt = System.currentTimeMillis()
            ),
            102 to RiderPosition(
                orderId = 102,
                riderEmail = "rider.hamza@zyphuel.com",
                lat = r2Lat,
                lng = r2Lng,
                bearing = 55f,
                speedKmh = 42f,
                status = "Assigned",
                updatedAt = System.currentTimeMillis()
            )
        )
        lastRefreshAt = System.currentTimeMillis()
    }

    /** Polls until the calling coroutine is cancelled (the window closing). */
    suspend fun pollForever(intervalMillis: Long = 4_000) {
        while (true) {
            refresh()
            delay(intervalMillis)
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
                }
                refresh()
            }
        } catch (e: Exception) {
            errorMessage = describe(e)
        } finally {
            isBusy = false
        }
    }

    private fun describe(e: Exception): String {
        val raw = e.message ?: e.toString()
        return when {
            raw.contains("HTTP 403") || raw.contains("PERMISSION_DENIED") ->
                "Firestore denied access (HTTP 403).\n\nThe security rules for project '${rest.config.projectId}' do not " +
                    "allow unauthenticated reads. Allow read/write on the 'orders' and 'live_tracking' " +
                    "collections in Firebase Console -> Firestore -> Rules, or set test mode.\n\n$raw"
            raw.contains("HTTP 404") || raw.contains("does not exist") ->
                "Cloud Firestore database '(default)' has not been created yet in Firebase project '${rest.config.projectId}'.\n" +
                    "Running in interactive Demo Simulation Mode. To connect live, click 'Open Firebase Console' below to create the database in 1 click."
            raw.contains("UnknownHost") || raw.contains("ConnectException") || raw.contains("timed out") ->
                "Cannot reach Firestore. Check the internet connection.\n\n$raw"
            else -> raw
        }
    }

    companion object {
        val DEMO_ORDERS = listOf(
            DesktopOrder(
                id = 101,
                customerName = "Ali Raza",
                customerPhone = "+92 300 8472911",
                customerEmail = "ali.raza@zyphuel.pk",
                serviceType = "Super Petrol",
                quantity = 25,
                totalPrice = 6950.0,
                deliveryAddress = "Main Boulevard, Gulberg III, Lahore",
                paymentMethod = "Cash on Delivery",
                status = "Delivering",
                riderName = "Rashid Minhas",
                riderEmail = "rider.rashid@zyphuel.com",
                createdAt = System.currentTimeMillis() - 15 * 60 * 1000L,
                etaMinutes = 8,
                destLat = 31.5120,
                destLng = 74.3450,
                originLat = DEPOT_LAT,
                originLng = DEPOT_LNG
            ),
            DesktopOrder(
                id = 102,
                customerName = "Usman Tariq",
                customerPhone = "+92 321 4459821",
                customerEmail = "usman.tariq@gmail.com",
                serviceType = "High-Speed Diesel",
                quantity = 40,
                totalPrice = 11200.0,
                deliveryAddress = "Sector C, DHA Phase 5, Lahore",
                paymentMethod = "Online Paid",
                status = "Assigned",
                riderName = "Hamza Akram",
                riderEmail = "rider.hamza@zyphuel.com",
                createdAt = System.currentTimeMillis() - 8 * 60 * 1000L,
                etaMinutes = 18,
                destLat = 31.4680,
                destLng = 74.3980,
                originLat = DEPOT_LAT,
                originLng = DEPOT_LNG
            ),
            DesktopOrder(
                id = 103,
                customerName = "Fatima Noor",
                customerPhone = "+92 333 9821045",
                customerEmail = "fatima.noor@zyphuel.pk",
                serviceType = "Pure Drinking Water (5 Gal)",
                quantity = 4,
                totalPrice = 200.0,
                deliveryAddress = "Block G3, Johar Town, Lahore",
                paymentMethod = "Cash on Delivery",
                status = "Pending",
                riderName = null,
                riderEmail = null,
                createdAt = System.currentTimeMillis() - 2 * 60 * 1000L,
                etaMinutes = 25,
                destLat = 31.4690,
                destLng = 74.2920,
                originLat = DEPOT_LAT,
                originLng = DEPOT_LNG
            ),
            DesktopOrder(
                id = 104,
                customerName = "Zubair Ahmad",
                customerPhone = "+92 301 5592310",
                customerEmail = "zubair.ahmad@hotmail.com",
                serviceType = "High Octane 97",
                quantity = 30,
                totalPrice = 9450.0,
                deliveryAddress = "Model Town Block B, Lahore",
                paymentMethod = "Cash on Delivery",
                status = "Completed",
                riderName = "Rashid Minhas",
                riderEmail = "rider.rashid@zyphuel.com",
                createdAt = System.currentTimeMillis() - 75 * 60 * 1000L,
                etaMinutes = 0,
                destLat = 31.4850,
                destLng = 74.3210,
                originLat = DEPOT_LAT,
                originLng = DEPOT_LNG
            )
        )
    }
}

