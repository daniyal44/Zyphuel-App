# 10. Database Room Persistence & Data Models Documentation 🗄️

## 📌 Category
**Data Architecture & Persistence**

## 🎯 Purpose & Overview
Zyphuel relies on Android Room database for reliable offline-first local data persistence. It manages user accounts, fuel/water orders, security audit logs, and notification history.

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

### 2. `OrderEntity` (`orders` table)
* `id` (Int, Primary Key, AutoGenerate) - Unique order ID
* `customerEmail` (String) - Ordering customer email
* `customerName` (String) - Ordering customer name
* `serviceType` (String) - `Super Petrol`, `Diesel`, `Pure Water`, `LPG`
* `fuelVolumeLiters` (Double) - Order volume in liters
* `totalAmountPkr` (Double) - Total PKR price
* `status` (String) - `Pending`, `Assigned`, `Delivering`, `Arrived`, `Completed`
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

---

## 📁 Source Locations
* **Entities & Models**: `app/src/main/java/com/example/data/Models.kt`
* **DAOs**: `app/src/main/java/com/example/data/Daos.kt` (`UserDao`, `OrderDao`, `AuditLogDao`, `NotificationDao`)
* **Database Class**: `app/src/main/java/com/example/data/AppDatabase.kt`
* **Repository**: `app/src/main/java/com/example/data/ZyphuelRepository.kt`

---

## 🔄 Room Persistence Lifecycle
1. **Repository Operations**: All database reads/writes run asynchronously on Kotlin Coroutines (`Dispatchers.IO`).
2. **Reactive Flow**: UI collects database queries via Kotlin `Flow` or `StateFlow` for immediate screen updates upon database mutations.
