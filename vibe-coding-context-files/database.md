# Zyphuel Database Schema (Room SQLite & Firestore)

> **Current App Version:** `2.6.4.0.0.33 (Build 61)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-23</b></font>  
> **Status:** <font color="#10b981"><b>Build Verified (37/38 Tests Passing — 1 Pre-Existing Unrelated Failure)</b></font>

The definitive single source of truth for Zyphuel's persistent data models.

---

## Room Database Version & Migration Policy (<font color="#10b981"><b>2026-10-23</b></font>)
* **Current Room version: 13** (`@Database` annotation in `app/src/main/java/com/example/data/Models.kt`).
* **Migration path shipped:** `MIGRATION_12_TO_13` — a schema-preserving no-op wired via `.addMigrations(...)` in both database builders (`Models.kt` `getDatabase()` and `Repository.kt` `AppRepository`). User data (users, orders, audit logs, notifications, marked locations, vehicles) survives every app update.
* **`fallbackToDestructiveMigration()` is permanently removed.** Any future database version bump MUST ship a matching `Migration` object, otherwise Room throws instead of silently wiping user data.
* Duplicate-email protection: `UserDao.getUserByEmail()` matches `LOWER(email) = LOWER(:email)` (case-insensitive).

---

## Conventions
- **Primary Keys:** Auto-incrementing integers (`id: Int = 0`) for Room entities; string IDs for Firestore documents.
- **Timestamps:** Epoch milliseconds (`System.currentTimeMillis()`) stored as `Long` or formatted ISO date strings.
- **Naming:** CamelCase in Kotlin code (`UserEntity`, `OrderEntity`), snake_case in Room table names (`users`, `orders`, `audit_logs`, `notifications`, `marked_locations`, `vehicles`).

---

## Room Entities

### 1. users (`UserEntity`)
| Column | Type | Notes |
|---|---|---|
| `email` | String | Primary Key, Unique, Lowercase |
| `name` | String | Full name |
| `passwordHash` | String | SHA-256 password hash |
| `role` | String | "customer", "rider", or "admin" |
| `phoneNumber` | String | Customer phone number |
| `residentialAddress` | String | Delivery address in Lahore |
| `isVerified` | Boolean | True if KYC / identity verified |
| `authProvider` | String | "Standard" or "Google" |

### 2. orders (`OrderEntity`)
| Column | Type | Notes |
|---|---|---|
| `id` | Int | Primary Key, Auto-generate |
| `customerEmail` | String | Foreign key reference to users.email |
| `customerName` | String | Customer name at time of order |
| `customerPhone` | String | Delivery contact phone |
| <font color="#10b981"><b>`serviceType`</b></font> | String | <font color="#10b981"><b>"Super Petrol", "Diesel", "High-Octane"</b> (Water & LPG unavailable)</font> |
| <font color="#10b981"><b>`quantity` / `fuelVolumeLiters`</b></font> | Double | <font color="#10b981"><b>Order volume in liters (Strict 15L Max Cap)</b></font> |
| <font color="#10b981"><b>`totalPrice` / `totalAmountPkr`</b></font> | Double | <font color="#10b981"><b>Grand total with silent retail pump markup (+Rs. 5.00/L) and tiered fee (Rs. 300–400)</b></font> |
| `deliveryAddress` | String | Dropoff street address |
| `destLat` | Double | Destination latitude |
| `destLng` | Double | Destination longitude |
| `status` | String | "Pending", "Assigned", "Delivering", "Completed", "Cancelled" |
| `assignedRiderEmail` | String? | Email of assigned bowser driver |
| `assignedRiderName` | String? | Name of assigned driver |
| `etaMinutes` | Int? | Dynamic ETA |
| `paymentMethod` | String | "Cash on Delivery", "Card", etc. |
| `orderDate` | String | Formatted order timestamp |
| `rating` | Float? | Customer rating (1 to 5 stars) |
| `feedback` | String? | Customer review comment |

### 3. audit_logs (`AuditLogEntity`)
| Column | Type | Notes |
|---|---|---|
| `id` | Int | Primary Key, Auto-generate |
| `action` | String | Action type (e.g. "PRICE_UPDATED", "ACCOUNT_DELETED", "ORDER_CREATED") |
| `performedBy` | String | Email or "System" |
| `details` | String | Full descriptive audit trail |
| `timestamp` | Long | Epoch millis |

### 4. notifications (`NotificationEntity`)
| Column | Type | Notes |
|---|---|---|
| `id` | Int | Primary Key, Auto-generate |
| `title` | String | Notification headline |
| `message` | String | Detailed notification text |
| `targetRole` | String | "all", "customer", "rider", "admin" |
| `timestamp` | Long | Epoch millis |
| `isRead` | Boolean | Read receipt state |

### 5. marked_locations (`MarkedLocationEntity`)
| Column | Type | Notes |
|---|---|---|
| `id` | Long | Primary Key, Auto-generate |
| `userEmail` | String | Owner email |
| `label` | String | "Home", "Office", "Generator" |
| `latitude` | Double | GPS Lat |
| `longitude` | Double | GPS Lng |
| `address` | String | Street address string |
| `isPrimary` | Boolean | Default delivery destination |

### 6. vehicles (`VehicleEntity`)
| Column | Type | Notes |
|---|---|---|
| `id` | Long | Primary Key, Auto-generate |
| `userEmail` | String | Owner email |
| `nickname` | String | "My Civic", "Generator 1" |
| `make` | String | "Honda", "Toyota", "Suzuki" |
| `model` | String | Model name & year |
| `registrationNumber`| String | License plate number |
| `fuelType` | String | "Petrol", "Diesel", "High-Octane" |
| `fuelTankCapacityLiters` | Int | Tank capacity in liters |
| `isPrimary` | Boolean | Active selected vehicle |
