package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.random.Random


class AppRepository(context: Context) {

    companion object {
        // Zyphuel central depot / dispatch origin (Lahore) — pickup start for every delivery
        const val DEPOT_LAT = 31.4380
        const val DEPOT_LNG = 74.3050
    }

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "zyphuel_database"
    ).fallbackToDestructiveMigration().build()

    val userDao = db.userDao()
    val orderDao = db.orderDao()
    val auditLogDao = db.auditLogDao()
    val notificationDao = db.notificationDao()
    val markedLocationDao = db.markedLocationDao()
    val firestoreOrderRepository = FirestoreOrderRepository(context)
    val firestoreUserRepository = FirestoreUserRepository(context)
    val authRepository = AuthRepository(context)
    val liveTrackingRepository = LiveTrackingRepository(context)

    fun getMarkedLocationsForUserFlow(email: String): Flow<List<MarkedLocationEntity>> =
        markedLocationDao.getMarkedLocationsForUserFlow(email)

    suspend fun saveMarkedLocation(location: MarkedLocationEntity): Long {
        if (location.isPrimary) {
            markedLocationDao.clearPrimaryLocationsForUser(location.userEmail)
        }
        return markedLocationDao.insertMarkedLocation(location)
    }

    suspend fun deleteMarkedLocation(id: Int) {
        markedLocationDao.deleteMarkedLocation(id)
    }

    suspend fun deleteUserAccount(email: String) {
        userDao.deleteUserByEmail(email)
        markedLocationDao.deleteMarkedLocationsForUser(email)
    }


    // Seed default admin account on startup if not present
    suspend fun seedAdminIfNeeded() {
        val adminEmail = "m.daniyalkhan490@gmail.com"
        val existingAdmin = userDao.getUserByEmail(adminEmail)
        if (existingAdmin == null) {
            val adminUser = UserEntity(
                email = adminEmail,
                name = "Muhammad Daniyal Khan",
                passwordHash = "abcd1234",
                role = "admin",
                phoneNumber = "03001234567",
                isVerified = true
            )
            userDao.insertUser(adminUser)
        } else if (existingAdmin.passwordHash != "abcd1234" || existingAdmin.role != "admin") {
            // Guarantee admin password and role remain fixed and unchangeable
            val fixedAdmin = existingAdmin.copy(
                passwordHash = "abcd1234",
                role = "admin",
                isVerified = true
            )
            userDao.updateUser(fixedAdmin)
        }
    }

    // --- Authentication Operations ---

    suspend fun registerUser(user: UserEntity): Boolean {
        val existing = userDao.getUserByEmail(user.email)
        if (existing != null) return false // User already exists

        userDao.insertUser(user)
        auditLogDao.insertLog(
            AuditLogEntity(
                action = "USER_REGISTERED",
                performedBy = user.email,
                details = "Registered as ${user.role} (Verified: ${user.isVerified})"
            )
        )
        return true
    }

    suspend fun loginUser(email: String, password: String): UserEntity? {
        // Seed first to ensure the admin exists for authorized checking
        seedAdminIfNeeded()

        val user = userDao.getUserByEmail(email)
        if (user != null && user.passwordHash == password) {
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "USER_LOGIN_SUCCESS",
                    performedBy = email,
                    details = "Login successful as ${user.role}"
                )
            )
            return user
        }
        
        auditLogDao.insertLog(
            AuditLogEntity(
                action = "USER_LOGIN_FAILED",
                performedBy = email,
                details = "Login attempt with incorrect password"
            )
        )
        return null
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun insertAuditLog(log: AuditLogEntity) {
        auditLogDao.insertLog(log)
    }

    // --- Customer Order Operations ---

    suspend fun createOrder(
        customerEmail: String,
        customerName: String,
        customerPhone: String,
        serviceType: String,
        quantity: Int,
        totalPrice: Double,
        deliveryAddress: String,
        destLat: Double? = null,
        destLng: Double? = null,
        paymentMethod: String = "Cash on Delivery"
    ): OrderEntity {
        val order = OrderEntity(
            customerEmail = customerEmail,
            customerName = customerName,
            customerPhone = customerPhone,
            serviceType = serviceType,
            quantity = quantity,
            totalPrice = totalPrice,
            deliveryAddress = deliveryAddress,
            paymentMethod = paymentMethod,
            status = "Pending",
            etaMinutes = Random.nextInt(15, 45),
            destLat = destLat,
            destLng = destLng,
            originLat = DEPOT_LAT,
            originLng = DEPOT_LNG
        )
        val insertedId = orderDao.insertOrder(order)
        val finalOrder = if (insertedId > 0) order.copy(id = insertedId.toInt()) else order

        // Asynchronous non-blocking Firestore sync (Room DB order is created instantly)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                firestoreOrderRepository.saveOrder(finalOrder)
            } catch (e: Exception) {
                android.util.Log.w("AppRepository", "Firestore save skipped: ${e.message}")
            }
        }

        try {
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "ORDER_CREATED",
                    performedBy = customerEmail,
                    details = "Created $serviceType order of $quantity units for Rs. $totalPrice ($paymentMethod)"
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "Audit log insert warning: ${e.message}")
        }
        return finalOrder
    }


    // --- Cross-device sync: Firestore -> Room upsert ---
    // The rest of the app reads from Room DAOs. By mirroring the Firestore
    // "orders" collection into Room, every existing Room-reading flow becomes
    // cross-device automatically (order placed on phone A shows on phone B).

    fun observeCustomerOrdersFromFirestore(customerEmail: String): Flow<List<OrderEntity>> =
        firestoreOrderRepository.getCustomerOrdersFlow(customerEmail)

    fun observeRiderOrdersFromFirestore(riderEmail: String): Flow<List<OrderEntity>> =
        firestoreOrderRepository.getRiderDeliveryHistoryFlow(riderEmail)

    fun observeAllOrdersFromFirestore(): Flow<List<OrderEntity>> =
        firestoreOrderRepository.getAllOrdersFlow()

    /** Upserts Firestore-sourced orders into Room (REPLACE keeps latest remote state). */
    suspend fun upsertOrdersToRoom(orders: List<OrderEntity>) {
        for (order in orders) {
            // Preserve locally-known destination coords if remote hasn't got them yet
            val merged = if (order.destLat == null || order.destLng == null) {
                val local = orderDao.getOrderById(order.id)
                order.copy(
                    destLat = order.destLat ?: local?.destLat,
                    destLng = order.destLng ?: local?.destLng,
                    originLat = order.originLat ?: local?.originLat,
                    originLng = order.originLng ?: local?.originLng
                )
            } else order
            orderDao.insertOrder(merged)
        }
    }

    // --- Rider Order Operations ---

    suspend fun acceptOrder(orderId: Int, riderEmail: String, riderName: String) {
        val order = orderDao.getOrderById(orderId)
        if (order != null && order.status == "Pending") {
            val updated = order.copy(
                riderEmail = riderEmail,
                riderName = riderName,
                status = "Assigned",
                etaMinutes = Random.nextInt(20, 35)
            )
            orderDao.insertOrder(updated)
            firestoreOrderRepository.saveOrder(updated)
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "ORDER_ACCEPTED",
                    performedBy = riderEmail,
                    details = "Accepted order #$orderId ($riderName)"
                )
            )
            // Notifications to Customer & Rider (all) and Admin
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Driver Assigned 🚚",
                    message = "Driver $riderName has been assigned to your Order #$orderId. Estimated arrival: ${updated.etaMinutes} mins.",
                    targetRole = "all"
                )
            )
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Admin: Driver Assigned for Order #$orderId",
                    message = "Rider $riderName ($riderEmail) assigned to order #$orderId.",
                    targetRole = "admin"
                )
            )
        }
    }

    suspend fun updateOrderStatus(orderId: Int, newStatus: String, riderEmail: String) {
        val order = orderDao.getOrderById(orderId)
        if (order != null && order.riderEmail == riderEmail) {
            if (order.status != newStatus) {
                val updated = order.copy(status = newStatus)
                orderDao.insertOrder(updated)
                firestoreOrderRepository.saveOrder(updated)
                auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "ORDER_STATUS_CHANGED",
                        performedBy = riderEmail,
                        details = "Order #$orderId changed status to $newStatus"
                    )
                )

                val (notifTitle, notifMsg) = when (newStatus) {
                    "Assigned" -> "Driver Assigned 🚚" to "Driver assigned for Order #$orderId."
                    "Delivering", "Dispatched" -> "Out for Delivery 🛵" to "Your Order #$orderId is now Out for Delivery! Track your delivery vehicle live on the map."
                    "Arriving", "Arriving Soon" -> "Arriving Soon 📍" to "Your delivery driver for Order #$orderId is Arriving Soon! Please get ready at your delivery location."
                    "Completed" -> "Order Delivered 🎉" to "Order #$orderId has been delivered successfully. Thank you for choosing Zyphuel!"
                    else -> "Order #$orderId Status Update 📦" to "Your order #$orderId status has been updated to: $newStatus"
                }

                notificationDao.insertNotification(
                    NotificationEntity(
                        title = notifTitle,
                        message = notifMsg,
                        targetRole = "all"
                    )
                )
                notificationDao.insertNotification(
                    NotificationEntity(
                        title = "Admin: Order #$orderId Status Update",
                        message = "Rider updated Order #$orderId status to: $newStatus",
                        targetRole = "admin"
                    )
                )
            }
        }
    }

    suspend fun rateOrder(orderId: Int, rating: Int, feedback: String?) {
        val order = orderDao.getOrderById(orderId)
        if (order != null) {
            val updated = order.copy(rating = rating, feedback = feedback)
            orderDao.insertOrder(updated)
            firestoreOrderRepository.rateOrder(orderId, rating, feedback)
        }
    }

    // --- Admin Panel Operations ---

    suspend fun approveRider(riderEmail: String, adminEmail: String) {
        val rider = userDao.getUserByEmail(riderEmail)
        if (rider != null && rider.role == "rider") {
            val updated = rider.copy(isVerified = true)
            userDao.insertUser(updated)
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "RIDER_APPROVED",
                    performedBy = adminEmail,
                    details = "Approved rider account: $riderEmail"
                )
            )
        }
    }

    suspend fun rejectRider(riderEmail: String, adminEmail: String) {
        val rider = userDao.getUserByEmail(riderEmail)
        if (rider != null && rider.role == "rider") {
            val updated = rider.copy(isVerified = false)
            userDao.insertUser(updated)
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "RIDER_REJECTED",
                    performedBy = adminEmail,
                    details = "Suspended/Rejected rider account: $riderEmail"
                )
            )
        }
    }

    suspend fun requestRiderVerification(riderEmail: String) {
        val rider = userDao.getUserByEmail(riderEmail)
        if (rider != null && rider.role == "rider") {
            val updated = rider.copy(
                hasRequestedVerification = true,
                adminApprovalStatus = "Pending Verification"
            )
            userDao.insertUser(updated)
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Verified Badge Application Received 🎖️",
                    message = "Rider ${rider.name} (${rider.email}) applied for a Verified Badge. Please inspect rider profile in Admin Panel to Approve or Deny.",
                    targetRole = "admin"
                )
            )
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "RIDER_VERIFICATION_APPLIED",
                    performedBy = riderEmail,
                    details = "Rider $riderEmail submitted a Verified Badge application to Admin"
                )
            )
        }
    }

    suspend fun approveRiderVerification(riderEmail: String, adminEmail: String) {
        val rider = userDao.getUserByEmail(riderEmail)
        if (rider != null && rider.role == "rider") {
            val updated = rider.copy(
                isVerified = true,
                hasRequestedVerification = false,
                adminApprovalStatus = "Approved"
            )
            userDao.insertUser(updated)
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "🎉 Verified Badge Approved!",
                    message = "Congratulations ${rider.name}! Your Verified Badge application was reviewed and APPROVED by Zyphuel Operations Admin.",
                    targetRole = "rider"
                )
            )
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "RIDER_VERIFICATION_APPROVED",
                    performedBy = adminEmail,
                    details = "Admin $adminEmail approved Verified Badge for rider $riderEmail"
                )
            )
        }
    }

    suspend fun denyRiderVerification(riderEmail: String, adminEmail: String) {
        val rider = userDao.getUserByEmail(riderEmail)
        if (rider != null && rider.role == "rider") {
            val updated = rider.copy(
                isVerified = false,
                hasRequestedVerification = false,
                adminApprovalStatus = "Rejected"
            )
            userDao.insertUser(updated)
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "❌ Verified Badge Request Status",
                    message = "Hello ${rider.name}, your application for a Verified Badge was reviewed and DENIED by the Admin.",
                    targetRole = "rider"
                )
            )
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "RIDER_VERIFICATION_DENIED",
                    performedBy = adminEmail,
                    details = "Admin $adminEmail denied Verified Badge for rider $riderEmail"
                )
            )
        }
    }

    suspend fun toggleRiderVerification(riderEmail: String, adminEmail: String): Boolean {
        val rider = userDao.getUserByEmail(riderEmail)
        if (rider != null && rider.role == "rider") {
            val newStatus = !rider.isVerified
            val updated = rider.copy(
                isVerified = newStatus,
                hasRequestedVerification = false,
                adminApprovalStatus = if (newStatus) "Approved" else "Rejected"
            )
            userDao.insertUser(updated)
            if (newStatus) {
                notificationDao.insertNotification(
                    NotificationEntity(
                        title = "🎉 Verified Badge Approved!",
                        message = "Congratulations ${rider.name}! Your Verified Badge has been activated by Admin.",
                        targetRole = "rider"
                    )
                )
            } else {
                notificationDao.insertNotification(
                    NotificationEntity(
                        title = "🔒 Verified Badge Revoked",
                        message = "Hello ${rider.name}, your Verified Badge status was updated to unverified by Admin.",
                        targetRole = "rider"
                    )
                )
            }
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = if (newStatus) "RIDER_VERIFIED_TOGGLE_ON" else "RIDER_VERIFIED_TOGGLE_OFF",
                    performedBy = adminEmail,
                    details = "Toggled verification status for $riderEmail to $newStatus"
                )
            )
            return newStatus
        }
        return false
    }

    // --- Password Suggestion Utility ---

    fun suggestStrongPassword(): PasswordSuggestion {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val symbols = "!@#$%^&*()_+="
        
        val random = Random.Default
        val password = StringBuilder()
        
        // Ensure at least one of each type is present
        password.append(uppercase[random.nextInt(uppercase.length)])
        password.append(lowercase[random.nextInt(lowercase.length)])
        password.append(digits[random.nextInt(digits.length)])
        password.append(symbols[random.nextInt(symbols.length)])
        
        val allChars = uppercase + lowercase + digits + symbols
        for (i in 4 until 12) {
            password.append(allChars[random.nextInt(allChars.length)])
        }
        
        // Shuffle the characters
        val list = password.toString().toList().shuffled()
        val finalPassword = list.joinToString("")
        
        return PasswordSuggestion(
            password = finalPassword,
            strength = "ULTRA SECURE",
            explanation = "Your password contains uppercase, lowercase, numbers, and special symbols. It is extremely resilient against dictionary and brute-force attacks, keeping your Zyphuel details fully safe."
        )
    }
}

data class PasswordSuggestion(
    val password: String,
    val strength: String,
    val explanation: String
)
