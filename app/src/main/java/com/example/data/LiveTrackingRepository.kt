package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Snapshot of a rider's live position for one order, streamed in real time.
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
 * Real-time rider location channel (Uber/Careem-style).
 *
 * Uses a dedicated Firestore collection `live_tracking/{orderId}` so that the
 * frequent (every few seconds) position writes do not churn the larger order
 * document. Riders publish; customers observe.
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
     * Publishes the rider's current position for an order. Uses merge so the
     * doc is created on first write and updated thereafter.
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
        val firestore = db ?: return false
        return try {
            val data = mapOf(
                "orderId" to orderId,
                "riderEmail" to riderEmail,
                "lat" to lat,
                "lng" to lng,
                "bearing" to bearing,
                "speedKmh" to speedKmh,
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION)
                .document(orderId.toString())
                .set(data, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing live location for order #$orderId: ${e.message}", e)
            false
        }
    }

    /**
     * Streams the rider's live position for an order. Emits null until the
     * first location arrives (or if tracking is unavailable).
     */
    fun observeLocation(orderId: Int): Flow<RiderLiveLocation?> = callbackFlow {
        val firestore = db
        if (firestore == null || orderId <= 0) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(COLLECTION)
            .document(orderId.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to live location for order #$orderId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        trySend(
                            RiderLiveLocation(
                                orderId = (data["orderId"] as? Number)?.toInt() ?: orderId,
                                riderEmail = data["riderEmail"] as? String ?: "",
                                lat = (data["lat"] as? Number)?.toDouble() ?: 0.0,
                                lng = (data["lng"] as? Number)?.toDouble() ?: 0.0,
                                bearing = (data["bearing"] as? Number)?.toFloat() ?: 0f,
                                speedKmh = (data["speedKmh"] as? Number)?.toFloat() ?: 0f,
                                status = data["status"] as? String ?: "",
                                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                            )
                        )
                    }
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Clears the live-tracking doc for an order once delivery is finished.
     */
    suspend fun clearLocation(orderId: Int): Boolean {
        val firestore = db ?: return false
        return try {
            firestore.collection(COLLECTION).document(orderId.toString()).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing live location for order #$orderId: ${e.message}", e)
            false
        }
    }
}
