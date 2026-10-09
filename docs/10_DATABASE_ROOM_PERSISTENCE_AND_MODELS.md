# 10. Database Room Persistence & Data Models Documentation 🗄️

> **Current App Version:** `2.6.4.0.0.26 (Build 54)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

## 📌 Category
**Data Architecture & Persistence**

## 🎯 Purpose & Overview
Zyphuel relies on Android Room database for reliable offline-first local data persistence. It manages user accounts, fuel orders, vehicle garages, saved delivery locations, security audit logs, and notification history.

---

## 🗄️ Database Tables & Data Entities

### 1. `UserEntity` (`users` table)
* `email` (String, Primary Key) - User email address
* `name` (String) - Full name
* `passwordHash` (String) - SHA-256 hashed password
* `role` (String) - User role (`customer`, `rider`, `admin`)
* `phoneNumber` (String) - Contact phone number
* `residentialAddress` (String) - Delivery address in Lahore
* `isVerified` (Boolean) - Verification flag
* `authProvider` (String) - `"Standard"`, `"Google"`

### 2. `OrderEntity` (`orders` table)
* `id` (Int, Primary Key, AutoGenerate) - Unique order ID
* `customerEmail` (String) - Ordering customer email
* `customerName` (String) - Ordering customer name
* <font color="#10b981"><b>`serviceType` (String)</b> - `"Super Petrol"`, `"Diesel"`, `"High Octane"` <i>(Water & LPG Gas permanently blocked/unavailable)</i></font>
* <font color="#10b981"><b>`fuelVolumeLiters` (Double)</b> - Order volume in liters <i>(Strict 15L maximum volume cap)</i></font>
* <font color="#10b981"><b>`totalAmountPkr` (Double)</b> - Total PKR price calculated with silent retail pump markup (+Rs. 5.00/L) and tiered delivery fee (Rs. 300–400)</font>
* `status` (String) - `"Pending"`, `"Assigned"`, `"Delivering"`, `"Arrived"`, `"Completed"`
* `assignedRiderEmail` (String?) - Assigned driver email
* `assignedRiderName` (String?) - Assigned driver name
* `deliveryAddress` (String) - Destination address
* `etaMinutes` (Int) - Estimated delivery time
* `rating` (Int?) - Customer rating (1-5 stars)
* `feedback` (String?) - Customer driver feedback

### 3. `AuditLogEntity` (`audit_logs` table)
* `id` (Int, Primary Key, AutoGenerate) - Log ID
* `timestamp` (Long) - System millisecond timestamp
* `action` (String) - Action identifier
* `performedBy` (String) - User email who performed action
* `details` (String) - Log details

### 4. `NotificationEntity` (`notifications` table)
* `id` (Int, Primary Key, AutoGenerate) - Notification ID
* `timestamp` (Long) - Notification timestamp
* `title` (String) - Notification title
* `message` (String) - Notification body text
* `targetRole` (String) - Role filter (`customer`, `rider`, `admin`, `all`)
* `isRead` (Boolean) - Read status flag

### 5. `VehicleEntity` (`vehicles` table)
* `id` (Long, Primary Key, AutoGenerate) - Vehicle ID
* `userEmail` (String) - Owner email
* `nickname` (String) - Friendly display name (e.g. "My Civic", "Generator 1")
* `make` (String) - Vehicle make (e.g. "Honda", "Toyota")
* `model` (String) - Vehicle model
* `registrationNumber` (String) - License plate number
* `fuelType` (String) - `"Petrol"`, `"Diesel"`, `"High-Octane"`
* `fuelTankCapacityLiters` (Int) - Tank capacity
* `isPrimary` (Boolean) - Active selected vehicle

---

## 📁 Source Locations
* **Entities & Models**: `app/src/main/java/com/example/data/Models.kt`
* **DAOs**: `app/src/main/java/com/example/data/Daos.kt` (`UserDao`, `OrderDao`, `AuditLogDao`, `NotificationDao`, `VehicleDao`)
* **Database Class**: `app/src/main/java/com/example/data/AppDatabase.kt`
* **Repository**: `app/src/main/java/com/example/data/ZyphuelRepository.kt`

---

## 🔄 Room Persistence Lifecycle
1. **Repository Operations**: All database reads/writes run asynchronously on Kotlin Coroutines (`Dispatchers.IO`).
2. **Reactive Flow**: UI collects database queries via Kotlin `Flow` or `StateFlow` for immediate screen updates upon database mutations.
