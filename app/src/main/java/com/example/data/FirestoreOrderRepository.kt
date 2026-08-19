package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreOrderRepository(private val context: Context) {

    private val TAG = "FirestoreOrderRepository"

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
     * Converts a Firestore document data map to [OrderEntity].
     */
    private fun documentToOrderEntity(docId: String, data: Map<String, Any?>): OrderEntity {
        val numericId = docId.toIntOrNull() ?: data["id"]?.toString()?.toIntOrNull() ?: docId.hashCode()
        return OrderEntity(
            id = numericId,
            customerEmail = data["customerEmail"] as? String ?: "",
            customerName = data["customerName"] as? String ?: "Customer",
            customerPhone = data["customerPhone"] as? String ?: "",
            serviceType = data["serviceType"] as? String ?: "Petrol",
            quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
            totalPrice = (data["totalPrice"] as? Number)?.toDouble() ?: 0.0,
            deliveryAddress = data["deliveryAddress"] as? String ?: "",
            paymentMethod = data["paymentMethod"] as? String ?: "Cash on Delivery",
            status = data["status"] as? String ?: "Pending",
            riderEmail = data["riderEmail"] as? String,
            riderName = data["riderName"] as? String,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            etaMinutes = (data["etaMinutes"] as? Number)?.toInt() ?: 30,
            rating = (data["rating"] as? Number)?.toInt(),
            feedback = data["feedback"] as? String,
            destLat = (data["destLat"] as? Number)?.toDouble(),
            destLng = (data["destLng"] as? Number)?.toDouble(),
            originLat = (data["originLat"] as? Number)?.toDouble(),
            originLng = (data["originLng"] as? Number)?.toDouble()
        )
    }

    /**
     * Converts [OrderEntity] to a map for Firestore.
     */
    private fun orderEntityToMap(order: OrderEntity): Map<String, Any?> {
        return mapOf(
            "id" to order.id,
            "customerEmail" to order.customerEmail,
            "customerName" to order.customerName,
            "customerPhone" to order.customerPhone,
            "serviceType" to order.serviceType,
            "quantity" to order.quantity,
            "totalPrice" to order.totalPrice,
            "deliveryAddress" to order.deliveryAddress,
            "paymentMethod" to order.paymentMethod,
            "status" to order.status,
            "riderEmail" to order.riderEmail,
            "riderName" to order.riderName,
            "createdAt" to order.createdAt,
            "etaMinutes" to order.etaMinutes,
            "rating" to order.rating,
            "feedback" to order.feedback,
            "destLat" to order.destLat,
            "destLng" to order.destLng,
            "originLat" to order.originLat,
            "originLng" to order.originLng,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    /**
     * Saves or updates an order in Firestore.
     */
    suspend fun saveOrder(order: OrderEntity): Boolean {
        val firestore = db ?: return false
        return try {
            val docRef = if (order.id > 0) {
                firestore.collection("orders").document(order.id.toString())
            } else {
                firestore.collection("orders").document()
            }
            val map = orderEntityToMap(order)
            docRef.set(map, SetOptions.merge()).await()
            Log.d(TAG, "Order #${order.id} saved to Firestore successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving order to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Real-time Flow of orders for a specific customer.
     */
    fun getCustomerOrdersFlow(customerEmail: String): Flow<List<OrderEntity>> = callbackFlow {
        val firestore = db
        if (firestore == null || customerEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("orders")
            .whereEqualTo("customerEmail", customerEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to customer orders from Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { documentToOrderEntity(doc.id, it) }
                    }.sortedByDescending { it.createdAt }
                    trySend(orders)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Real-time Flow of delivery history for a specific rider.
     */
    fun getRiderDeliveryHistoryFlow(riderEmail: String): Flow<List<OrderEntity>> = callbackFlow {
        val firestore = db
        if (firestore == null || riderEmail.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("orders")
            .whereEqualTo("riderEmail", riderEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to rider delivery history from Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { documentToOrderEntity(doc.id, it) }
                    }.sortedByDescending { it.createdAt }
                    trySend(orders)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Real-time Flow of all active and past orders (Admin / General).
     */
    fun getAllOrdersFlow(): Flow<List<OrderEntity>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to all orders from Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { documentToOrderEntity(doc.id, it) }
                    }.sortedByDescending { it.createdAt }
                    trySend(orders)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Updates status of an existing order in Firestore.
     */
    suspend fun updateOrderStatus(
        orderId: Int,
        newStatus: String,
        riderEmail: String? = null,
        riderName: String? = null,
        etaMinutes: Int? = null
    ): Boolean {
        val firestore = db ?: return false
        return try {
            val docRef = firestore.collection("orders").document(orderId.toString())
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to System.currentTimeMillis()
            )
            riderEmail?.let { updates["riderEmail"] = it }
            riderName?.let { updates["riderName"] = it }
            etaMinutes?.let { updates["etaMinutes"] = it }

            docRef.update(updates).await()
            Log.d(TAG, "Order #$orderId status updated to $newStatus in Firestore.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order status in Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Rating and feedback update for completed orders in Firestore.
     */
    suspend fun rateOrder(orderId: Int, rating: Int, feedback: String?): Boolean {
        val firestore = db ?: return false
        return try {
            val docRef = firestore.collection("orders").document(orderId.toString())
            val updates = mutableMapOf<String, Any>(
                "rating" to rating,
                "updatedAt" to System.currentTimeMillis()
            )
            feedback?.takeIf { it.isNotBlank() }?.let { validFeedback ->
                updates["feedback"] = validFeedback
            }
            docRef.update(updates).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error rating order in Firestore: ${e.message}", e)
            false
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}
