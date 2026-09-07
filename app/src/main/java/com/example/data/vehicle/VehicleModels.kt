package com.example.data.vehicle

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Entity representing Customer's saved vehicles for personalized services.
 */
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val nickname: String, // e.g. "My Car", "My Daily Bike", "Work Van"
    val vehicleType: String, // "Bike", "Car", "SUV", "Van", "Commercial", "EV"
    val make: String, // e.g. "Toyota", "Honda", "Suzuki", "Tesla"
    val model: String, // e.g. "Corolla", "CD 70", "Civic", "Model 3"
    val year: Int, // e.g. 2022
    val registrationNumber: String, // e.g. "LEA-22-1234"
    val fuelType: String, // "Petrol", "Diesel", "High-Octane", "Electric", "Hybrid", "LPG"
    val isEv: Boolean = false,
    val preferredServices: String = "", // Comma-separated tags
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE LOWER(userEmail) = LOWER(:email) ORDER BY isDefault DESC, createdAt DESC")
    fun getVehiclesForUserFlow(email: String): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: Int): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE LOWER(userEmail) = LOWER(:email) AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultVehicle(email: String): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteVehicle(id: Int)

    @Query("UPDATE vehicles SET isDefault = 0 WHERE LOWER(userEmail) = LOWER(:email)")
    suspend fun clearDefaultVehicles(email: String)

    @Query("UPDATE vehicles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultVehicle(id: Int)

    @Query("DELETE FROM vehicles WHERE LOWER(userEmail) = LOWER(:email)")
    suspend fun deleteVehiclesForUser(email: String)
}
