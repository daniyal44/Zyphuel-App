# Zyphuel Database Schema (Room SQLite & Firestore)

> The definitive single source of truth for Zyphuel's persistent data models.

## Conventions
- **Primary Keys:** Auto-incrementing integers (`id: Int = 0`) for Room entities; string IDs for Firestore documents.
- **Timestamps:** Epoch milliseconds (`System.currentTimeMillis()`) stored as `Long` or formatted ISO date strings.
- **Naming:** CamelCase in Kotlin code (`UserEntity`, `OrderEntity`), snake_case in Room table names (`users`, `orders`, `audit_logs`, `notifications`, `marked_locations`, `vehicles`).

## Room Entities

### 1. users (`UserEntity`)
| Column | Type | Notes |
|--------|------|-------|
| `email` | String | Primary Key, Unique, Lowercase |
| `name` | String | Full name |
| `passwordHash` | String | SHA-256 password hash |
| `role` | String | "customer", "rider", or "admin" |
| `phoneNumber` | String | Customer phone number |
| `isVerified` | Boolean | True if KYC / identity verified |

### 2. orders (`OrderEntity`)
| Column | Type | Notes |
|--------|------|-------|
| `id` | Int | Primary Key, Auto-generate |
| `customerEmail` | String | Foreign key reference to users.email |
| `customerName` | String | Customer name at time of order |
| `customerPhone` | String | Delivery contact phone |
| `serviceType` | String | "Petrol", "Diesel", "High-Octane", "Gas", or combo |
| `quantity` | Int | Quantity ordered (liters or units) |
| `totalPrice` | Double | Grand total calculated using Petrol Pump Rate |
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
|--------|------|-------|
| `id` | Int | Primary Key, Auto-generate |
| `action` | String | Action type (e.g. "PRICE_UPDATED", "ACCOUNT_DELETED", "ORDER_CREATED") |
| `performedBy` | String | Email or "System" |
| `details` | String | Full descriptive audit trail |
| `timestamp` | Long | Epoch millis |

### 4. notifications (`NotificationEntity`)
| Column | Type | Notes |
|--------|------|-------|
| `id` | Int | Primary Key, Auto-generate |
| `title` | String | Notification headline |
| `message` | String | Detailed notification text |
| `targetRole` | String | "all", "customer", "rider", "admin" |
| `timestamp` | Long | Epoch millis |
| `isRead` | Boolean | Read receipt state |

### 5. marked_locations (`MarkedLocationEntity`)
| Column | Type | Notes |
|--------|------|-------|
| `id` | Long | Primary Key, Auto-generate |
| `userEmail` | String | Owner email |
| `label` | String | "Home", "Office", "Factory Generator" |
| `latitude` | Double | GPS Lat |
| `longitude` | Double | GPS Lng |
| `address` | String | Street address string |
| `isPrimary` | Boolean | Default delivery destination |

### 6. vehicles (`VehicleEntity`)
| Column | Type | Notes |
|--------|------|-------|
| `id` | Long | Primary Key, Auto-generate |
| `userEmail` | String | Owner email |
| `nickname` | String | "My Civic", "Office Bowser" |
| `make` | String | "Honda", "Toyota", "Suzuki" |
| `model` | String | Model name & year |
| `registrationNumber`| String | License plate number |
| `fuelType` | String | "Petrol", "Diesel", "High-Octane", "Hybrid", "Electric" |
| `fuelTankCapacityLiters` | Int | Tank capacity in liters |
| `isPrimary` | Boolean | Active selected vehicle |
