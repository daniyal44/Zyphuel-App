package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// 1. User Entity representing Customers, Riders, and Admin accounts
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val role: String, // "customer", "rider", "admin"
    val phoneNumber: String,
    val isVerified: Boolean = false, // Riders must be approved by Admin
    val hasRequestedVerification: Boolean = false, // Rider requested verified badge application
    val vehicleType: String? = null, // "Bike", "Pickup", "Truck" (Riders only)
    val vehicleNo: String? = null,    // Riders only
    val profilePictureUri: String? = null, // Profile picture URI or Base64 (both customers & riders)
    // Pakistani Rider Legal Verification fields
    val country: String? = "Pakistan",
    val documentType: String? = null, // "CNIC" or "Passport"
    val cnicOrPassport: String? = null,
    val drivingLicense: String? = null,
    val isFaceVerified: Boolean = false,
    
    // --- ADVANCED RIDER MASTER REGISTRATION FIELDS ---
    val fathersName: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val cnicIssueDate: String? = null,
    val cnicExpiryDate: String? = null,
    val cnicFrontImage: String? = null,
    val cnicBackImage: String? = null,
    
    val residentialAddress: String? = null,
    val city: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    
    val vehicleMake: String? = null,
    val vehicleModel: String? = null,
    val vehicleColor: String? = null,
    val vehicleRegBookImage: String? = null,
    val vehiclePhoto: String? = null,
    
    val licenseCategory: String? = null,
    val licenseIssueDate: String? = null,
    val licenseExpiryDate: String? = null,
    val licenseFrontImage: String? = null,
    val licenseBackImage: String? = null,
    
    val emergencyName: String? = null,
    val emergencyRelationship: String? = null,
    val emergencyPhone: String? = null,
    
    val passportPhoto: String? = null,
    val selfieHoldingCnic: String? = null,
    val policeCertificate: String? = null,
    
    val termsAccepted: Boolean = false,
    val declarationAccepted: Boolean = false,
    val authProvider: String = "Email", // "Email", "Google", "Facebook", "GitHub"
    
    // --- SYSTEM FIELDS (HIDDEN / BACKEND) ---
    val riderNumber: Int? = null, // Sequential assigned rider number (e.g. 1, 2, 3...)
    val riderId: String? = null, // e.g. "RIDER-1"
    val cnicVerificationStatus: String = "Pending", // "Pending", "Verified", "Rejected"
    val licenseVerificationStatus: String = "Pending", // "Pending", "Verified", "Rejected"
    val adminApprovalStatus: String = "Pending", // "Pending", "Approved", "Rejected"
    val registrationStatus: String = "Pending", // "Pending", "Approved", "Rejected"
    val accountStatus: String = "Active", // "Active", "Suspended", "Blocked"
    val blockedUsers: String = "", // Comma-separated emails of blocked users
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 2. Order Entity representing premium delivery orders for Lahore
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerEmail: String,
    val customerName: String,
    val customerPhone: String,
    val serviceType: String, // "Petrol", "Diesel", "High-Octane", "LPG Gas", "Water"
    val quantity: Int, // Liters, KGs, or Gallons
    val totalPrice: Double,
    val deliveryAddress: String,
    val paymentMethod: String = "Cash on Delivery",
    val status: String, // "Pending", "Assigned", "Delivering", "Completed", "Cancelled"
    val riderEmail: String? = null,
    val riderName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val etaMinutes: Int = 30, // Estimated Time of Arrival
    val rating: Int? = null,
    val feedback: String? = null,
    // Geo coordinates for real-time map tracking (nullable = backward compatible)
    val destLat: Double? = null,  // Customer delivery destination latitude
    val destLng: Double? = null,  // Customer delivery destination longitude
    val originLat: Double? = null, // Depot / pickup origin latitude
    val originLng: Double? = null  // Depot / pickup origin longitude
)

// 3. Audit Log Entity for administrative action tracking
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val performedBy: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 4. Notification Entity for in-app notifications
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val targetRole: String, // "customer", "rider", "admin", "all"
    val isRead: Boolean = false
)

// --- DAOs (Data Access Objects) ---

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role")
    suspend fun getUsersByRole(role: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE role = 'rider'")
    fun getAllRidersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'rider' AND isVerified = 1")
    fun getActiveVerifiedRidersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'customer'")
    fun getAllCustomersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isVerified = :verified WHERE email = :email")
    suspend fun updateRiderVerification(email: String, verified: Boolean)

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE LOWER(customerEmail) = LOWER(:email) ORDER BY createdAt DESC")
    fun getOrdersForCustomerFlow(email: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE LOWER(riderEmail) = LOWER(:email) OR (riderEmail IS NULL AND status = 'Pending') ORDER BY createdAt DESC")
    fun getOrdersForRiderFlow(email: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Int): OrderEntity?
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}

// 5. Marked Location Entity for permanent user-selected location pins
@Entity(tableName = "marked_locations")
data class MarkedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val label: String, // e.g. "Home Pin", "Work Site", "Farmhouse Pin"
    val address: String, // e.g. "House 42, Block C2, Gulberg III, Lahore"
    val latitude: Double = 31.5204,
    val longitude: Double = 74.3587,
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE targetRole = :role OR targetRole = 'all' ORDER BY timestamp DESC")
    fun getNotificationsForRoleFlow(role: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}

@Dao
interface MarkedLocationDao {
    @Query("SELECT * FROM marked_locations WHERE userEmail = :email ORDER BY isPrimary DESC, createdAt DESC")
    fun getMarkedLocationsForUserFlow(email: String): Flow<List<MarkedLocationEntity>>

    @Query("SELECT * FROM marked_locations WHERE userEmail = :email AND isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryLocationForUser(email: String): MarkedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarkedLocation(location: MarkedLocationEntity): Long

    @Query("DELETE FROM marked_locations WHERE id = :id")
    suspend fun deleteMarkedLocation(id: Int)

    @Query("UPDATE marked_locations SET isPrimary = 0 WHERE userEmail = :email")
    suspend fun clearPrimaryLocationsForUser(email: String)

    @Query("UPDATE marked_locations SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimaryLocation(id: Int)

    @Query("DELETE FROM marked_locations WHERE userEmail = :email")
    suspend fun deleteMarkedLocationsForUser(email: String)
}


// --- App Database definition ---

@Database(entities = [UserEntity::class, OrderEntity::class, AuditLogEntity::class, NotificationEntity::class, MarkedLocationEntity::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun orderDao(): OrderDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun notificationDao(): NotificationDao
    abstract fun markedLocationDao(): MarkedLocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zyphuel_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
