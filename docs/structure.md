# Zyphuel Application - Codebase & Architecture Structure (`docs/structure.md`)

**Last Updated:** <font color="#10b981"><b>2026-10-09</b></font> • **Application Version:** <font color="#10b981"><b>2.6.4.0.0.24 (Build 52)</b></font>

---

## 1. High-Level Architecture Overview
**Zyphuel** is an enterprise-grade Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room Local Database**, **Coroutines & Flow**, **WorkManager**, and **Firebase Cloud Messaging**. It follows clean **MVVM (Model-View-ViewModel)** architecture with unidirectional data flow (UDF).

```
app/
 ├── src/
 │    ├── main/
 │    │    ├── java/com/example/
 │    │    │    ├── MainActivity.kt                       # Single-activity launcher with Compose Navigation & Sidebars
 │    │    │    ├── data/
 │    │    │    │    ├── Models.kt                        # Room Entities (UserEntity, OrderEntity, AuditLogEntity, NotificationEntity)
 │    │    │    │    ├── Repository.kt                    # Data Access Objects (DAOs) & Unified Repository pattern
 │    │    │    │    ├── category/
 │    │    │    │    │    ├── CategoryModels.kt           # 10-Category Catalog, Subcategories & Vehicle Models
 │    │    │    │    │    └── CategoryRepository.kt       # Typo-tolerant Levenshtein search & catalog repository
 │    │    │    │    └── TrackmateFuelApiService.kt       # Fuel price API and network endpoints
 │    │    │    ├── ui/
 │    │    │    │    ├── MainViewModel.kt                 # Central StateFlow state manager, hours gate & business logic
 │    │    │    │    ├── Screens.kt                       # Compose UI screens (Customer, Rider, Admin, OrderDialog, Maps)
 │    │    │    │    ├── CategoryComponents.kt            # Marketplace grid, category modals & search components
 │    │    │    │    ├── SecuritySettingsScreen.kt        # Security & Biometrics management UI
 │    │    │    │    ├── components/
 │    │    │    │    │    └── TermsAndPrivacyDialog.kt    # In-App Legal Viewer (Terms & Privacy with Live Build Badge)
 │    │    │    │    └── theme/                           # Material 3 Color, Type, Theme definitions
 │    │    │    ├── security/
 │    │    │    │    ├── BiometricSecurityManager.kt      # BiometricPrompt integration
 │    │    │    │    ├── SecureStorageManager.kt          # EncryptedSharedPreferences
 │    │    │    │    ├── RootAndSecurityDetector.kt       # Anti-root, tampered build detection
 │    │    │    │    ├── SecurityRateLimiter.kt           # Rate limiting for auth & requests
 │    │    │    │    └── SecurityInputValidator.kt        # Input sanitization
 │    │    │    ├── service/
 │    │    │    │    ├── LocationService.kt               # FusedLocationProvider real-time tracking
 │    │    │    │    ├── RiderLocationForegroundService.kt # Foreground live navigation service
 │    │    │    │    └── ZyphuelFcmService.kt             # Firebase push notification service
 │    │    │    └── util/
 │    │    │         ├── DeliveryOperatingHoursManager.kt # PKT Timezone Delivery Operating Hours & Status Evaluator
 │    │    │         ├── FeeConstants.kt                  # Per-Liter Tiered Delivery Fee & Silent OGRA Pump Markup (+5.00)
 │    │    │         ├── InvoiceGenerator.kt              # PDF Tax Invoice generator & Android PrintManager bridge
 │    │    │         ├── RealtimeEmailEngine.kt           # Multi-channel transactional SMTP & Webhook email engine
 │    │    │         └── DebugLogger.kt                   # Sanitized zero-leak debug logger
 │    │    └── res/
 │    │         ├── drawable/                            # App vector icons & logos
 │    │         ├── mipmap-*/                            # Adaptive launcher icons
 │    │         └── values/                              # strings.xml, colors.xml, themes.xml
 │    └── test/                                          # Local JVM Unit Tests (FuelPumpPricingTest, ComprehensiveAppTest)
 ├── build.gradle.kts                                    # App gradle build configuration (versionCode 52, versionName 2.6.4.0.0.24)
 └── AndroidManifest.xml                                 # Manifest permissions, services, metadata
```

---

## 2. Core Modules & Component Hierarchy

### A. Data Layer (`com.example.data`)
- **`Models.kt`**: Contains core Room entities:
  - `UserEntity`: `email` (PK), `name`, `passwordHash`, `role` (`customer`, `rider`, `admin`), `phoneNumber`, `residentialAddress`, `isVerified`.
  - `OrderEntity`: `id` (PK), `customerEmail`, `customerName`, `serviceType`, `fuelVolumeLiters`, `totalAmountPkr`, `status` (`Pending`, `Assigned`, `Delivering`, `Completed`, `Cancelled`), `assignedRiderEmail`, `assignedRiderName`, `deliveryAddress`, `etaMinutes`, `rating`, `feedback`.
  - `AuditLogEntity`: `id` (PK), `timestamp`, `action`, `performedBy`, `details`.
  - `NotificationEntity`: `id` (PK), `timestamp`, `title`, `message`, `targetRole`, `isRead`.
  - `VehicleEntity`: `id` (PK), `userId`, `make`, `model`, `plateNumber`, `fuelType`, `tankCapacityLiters`.
- **`Repository.kt`**: Encapsulates DAOs (`UserDao`, `OrderDao`, `AuditLogDao`, `NotificationDao`, `VehicleDao`), providing thread-safe reactive Kotlin Flow queries and suspend functions.

### B. Business Logic & ViewModel (`com.example.ui.MainViewModel`)
- Centralizes state streams (`StateFlow` / `SharedFlow`) for authenticated users, active orders, real-time map positions, biometrics status, and notification logs.
- Enforces the **Operating Hours Window Gate** (`isDeliveryOpen`) and blocks off-hour order placement attempts in `placeOrder()`.
- Calculates retail pump rates silently via `FeeConstants.PUMP_RATE_MARKUP_FLOAT` (+Rs. 5.00/L).

### C. Operating Hours & Delivery Fee Utilities (`com.example.util`)
- **`DeliveryOperatingHoursManager.kt`**: Evaluates active delivery window in Pakistan Standard Time (`Asia/Karachi`):
  - Mon–Thu: <font color="#10b981"><b>08:00 AM – 08:00 PM PKT</b></font>
  - Fri: <font color="#10b981"><b>08:00 AM – 01:00 PM PKT</b></font>
  - Sat–Sun: <font color="#10b981"><b>10:00 AM – 06:00 PM PKT</b></font>
- **`FeeConstants.kt`**: Standardizes granular delivery fees:
  - 1..10L: <font color="#10b981"><b>Rs. 300.00</b></font> | 11L: <font color="#10b981"><b>Rs. 320.00</b></font> | 12L: <font color="#10b981"><b>Rs. 340.00</b></font> | 13L: <font color="#10b981"><b>Rs. 360.00</b></font> | 14L: <font color="#10b981"><b>Rs. 380.00</b></font> | 15L (Strict Max Cap): <font color="#10b981"><b>Rs. 400.00</b></font>.
  - Retail Pump Markup: `PUMP_RATE_MARKUP = 5.00` Rs./L.

### D. Pure Code-Based Trading-Style Velocity Engine (`scripts/generate_github_graph.js`)
- Directly evaluates codebase churn (`git log --numstat`), additions, deletions, and classifies each update as Major, Moderate, or Minor Shift.
- Tracks velocity across Daily (1D), Weekly (1W), and Monthly (1M) timeframes with exact push interval analysis.
- Dynamically generates native GitHub Mermaid `xychart-beta`, Mermaid `gitGraph`, and ASCII/Unicode Trading Terminal directly in `README.md` (Zero static SVG files).
