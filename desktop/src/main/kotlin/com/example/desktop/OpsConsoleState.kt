package com.example.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * All console state in one place, plus the polling loop that keeps it fresh.
 *
 * Firestore's REST API has no snapshot listeners, so the console re-reads `orders` and
 * `live_tracking` on a short interval. Rider positions are written every ~4s by the phone,
 * so a 4s poll matches the data's real update rate without hammering the API.
 */
class OpsConsoleState(private val rest: FirestoreRest) {

    var orders by mutableStateOf<List<DesktopOrder>>(emptyList())
        private set

    var livePositions by mutableStateOf<Map<Int, RiderPosition>>(emptyMap())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isBusy by mutableStateOf(false)
        private set

    var lastRefreshAt by mutableStateOf(0L)
        private set

    var hasLoadedOnce by mutableStateOf(false)
        private set

    var selectedOrderId by mutableStateOf<Int?>(null)

    var filter by mutableStateOf(Filter.ACTIVE)

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
            hasLoadedOnce = true
        } catch (e: Exception) {
            errorMessage = describe(e)
            hasLoadedOnce = true
        }
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
            rest.updateOrderStatus(orderId = order.id, status = newStatus)
            if (newStatus in setOf("Completed", "Delivered", "Cancelled", "Canceled")) {
                rest.clearLiveTracking(order.id)
            }
            refresh()
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
                "Firestore denied access (HTTP 403).\n\nThe security rules for this project do not " +
                    "allow unauthenticated reads. Allow read/write on the 'orders' and 'live_tracking' " +
                    "collections, or run the console against a project in test mode.\n\n$raw"
            raw.contains("HTTP 404") ->
                "Firestore returned 404 - check that the project id is correct and that Firestore " +
                    "has been created for it.\n\n$raw"
            raw.contains("UnknownHost") || raw.contains("ConnectException") || raw.contains("timed out") ->
                "Cannot reach Firestore. Check the internet connection.\n\n$raw"
            else -> raw
        }
    }
}
