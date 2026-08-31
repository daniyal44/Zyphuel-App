package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Snapshot of a rider's live position for one order.
 */
data class RiderLiveLocation(
    val orderId: Int,
    val riderEmail: String,
    val lat: Double,
    val lng: Double,
    val bearing: Float,
    val speedKmh: Float,
    val status: String,
    val updatedAt: Long
)

/**
 * Server Live Tracking Manager.
 * Live tracking has been permanently disabled on the cloud server (Firestore).
 * Server writes are blocked, listeners are removed, and server records are purged.
 */
class LiveTrackingRepository(private val context: Context) {

    private val TAG = "LiveTrackingRepository"
    private val COLLECTION = "live_tracking"

    private val db: FirebaseFirestore?
        get() {
            return try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseFirestore.getInstance()
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FirebaseFirestore instance", e)
                null
            }
        }

    /**
     * Disabled: No GPS coordinates are published to the cloud server.
     */
    suspend fun publishLocation(
        orderId: Int,
        riderEmail: String,
        lat: Double,
        lng: Double,
        bearing: Float,
        speedKmh: Float,
        status: String
    ): Boolean {
        // Permanently disabled on server
        return false
    }

    /**
     * Disabled: Returns an empty flow to avoid opening Firestore server snapshot listeners.
     */
    fun observeLocation(orderId: Int): Flow<RiderLiveLocation?> {
        // Permanently disabled on server
        return flowOf(null)
    }

    /**
     * Clears any lingering live-tracking document for an order from Firestore server.
     */
    suspend fun clearLocation(orderId: Int): Boolean {
        val firestore = db ?: return false
        return try {
            firestore.collection(COLLECTION).document(orderId.toString()).delete().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Notice clearing server live-tracking document #$orderId: ${e.message}")
            false
        }
    }

    /**
     * Permanently purges and deletes the entire live_tracking collection from the Firestore server.
     */
    suspend fun purgeAllServerLiveTracking(): Boolean {
        val firestore = db ?: return false
        return try {
            val snapshots = firestore.collection(COLLECTION).get().await()
            for (doc in snapshots.documents) {
                doc.reference.delete().await()
            }
            Log.i(TAG, "Successfully purged ${snapshots.size()} server live-tracking records from Firestore.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Server live tracking purge notice: ${e.message}")
            false
        }
    }
}
