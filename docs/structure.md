# Zyphuel Application - Codebase & Architecture Structure (`docs/structure.md`)

## 1. High-Level Architecture Overview
**Zyphuel** is an enterprise-grade Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room Local Database**, **Coroutines & Flow**, **WorkManager**, and **Firebase Cloud Messaging**. It follows clean **MVVM (Model-View-ViewModel)** architecture with unidirectional data flow (UDF).

```
app/
 ├── src/
 │    ├── main/
 │    │    ├── java/com/example/
 │    │    │    ├── MainActivity.kt                       # Single-activity launcher with Compose Navigation & Sidebars
 │    │    │    ├── data/
 │    │    │    │    ├── Models.kt                        # Room Entities (UserEntity, OrderEntity, AuditLogEntity, etc.)
 │    │    │    │    ├── Repository.kt                    # Data Access Objects (DAOs) & Repository pattern
 │    │    │    │    └── TrackmateFuelApiService.kt       # Fuel price API and network endpoints
 │    │    │    ├── ui/
 │    │    │    │    ├── MainViewModel.kt                 # Central StateFlow state manager & business logic
 │    │    │    │    ├── Screens.kt                       # Compose UI screens (Customer, Rider, Admin, Maps)
 │    │    │    │    ├── SecuritySettingsScreen.kt        # Security & Biometrics management UI
 │    │    │    │    └── theme/                           # Material 3 Color, Type, Theme definitions
 │    │    │    ├── security/
 │    │    │    │    ├── BiometricSecurityManager.kt      # BiometricPrompt integration
 │    │    │    │    ├── SecureStorageManager.kt          # EncryptedSharedPreferences
 │    │    │    │    ├── RootAndSecurityDetector.kt       # Anti-root, tampered build detection
 │    │    │    │    ├── SecurityRateLimiter.kt           # Rate limiting for auth & requests
 │    │    │    │    ├── SecurityInputValidator.kt        # Input sanitization
 │    │    │    │    ├── SecurityFileUploadValidator.kt   # File upload verification
 │    │    │    │    └── SecurityErrorFormatter.kt        # User-friendly error messages
 │    │    │    ├── service/
 │    │    │    │    ├── LocationService.kt               # FusedLocationProvider real-time tracking
 │    │    │    │    └── ZyphuelFcmService.kt             # Firebase push notification service
 │    │    │    ├── worker/
 │    │    │    │    └── FuelPriceWorker.kt               # WorkManager periodic background broadcaster
 │    │    │    └── util/
 │    │    │         ├── UnifiedAssetManager.kt          # App icons, notification drawables, PWA metadata
 │    │    │         ├── GlobalErrorBoundary.kt          # Global exception handler
 │    │    │         └── DebugLogger.kt                  # Safe logging utility
 │    │    └── res/
 │    │         ├── drawable/                            # App vector icons & logos
 │    │         ├── mipmap-*/                            # Adaptive launcher icons
 │    │         ├── values/                              # strings.xml, colors.xml, themes.xml
 │    │         └── xml/                                 # Backup & data extraction rules
 │    └── test/                                          # Local JVM & Roborazzi screenshot tests
 ├── build.gradle.kts                                    # App gradle build configuration
 └── AndroidManifest.xml                                 # Manifest permissions, services, metadata
```

## 2. Core Modules & Component Hierarchy

### A. Data Layer (`com.example.data`)
- **`Models.kt`**: Contains Room entities (`UserEntity`, `OrderEntity`, `AuditLogEntity`, `NotificationEntity`, `FuelStationEntity`) and UI data classes (`FuelPriceItem`, `RiderLocation`).
- **`Repository.kt`**: Encapsulates Room DAOs (`UserDao`, `OrderDao`, `AuditLogDao`, `NotificationDao`), providing thread-safe Flow queries and suspend functions for database operations.
- **`TrackmateFuelApiService.kt`**: Handles external network requests for real-time fuel market prices in Pakistan.

### B. Business Logic & ViewModel (`com.example.ui.MainViewModel`)
- Centralizes state streams (`StateFlow` / `SharedFlow`) for authenticated users, active orders, real-time map positions, rider dispatches, biometrics status, and notification logs.
- Manages order placement logic: calculating distance charges per km from nearby petrol pumps, searching for nearby riders upon order confirmation, and dispatching orders to riders.

### C. UI Presentation Layer (`com.example.ui`)
- **`MainActivity.kt`**: Configures edge-to-edge Compose layout, navigation drawers for Customer, Rider, and Admin roles, top action bars, bottom navigation rails, and modal dialogs.
- **`Screens.kt`**: Houses all primary app composables:
  - `CustomerHomeScreen`: Map preview, fuel ordering form, distance charge breakdown, and nearby pumps.
  - `RiderHomeScreen`: Available order requests, discretionary order acceptance, active delivery navigation map.
  - `AdminDashboardScreen`: Order oversight, user management, audit logs, and `AdminFuelPriceNotificationScheduleCard`.
  - `UnifiedGoogleMapView` & `DriverRealTimeTrackingMap`: Interactive Leaflet/Google WebView map components displaying nearby petrol pumps, delivery route polyline, real-time vehicle movement, driver details, and ETA in minutes.
- **`SecuritySettingsScreen.kt`**: Manages fingerprint biometrics enrollment, status indicators, and security settings.

### D. Security & System Services (`com.example.security`, `com.example.service`, `com.example.worker`)
- **`BiometricSecurityManager.kt`**: Provides Android `BiometricPrompt` authentication.
- **`SecureStorageManager.kt`**: Stores sensitive user sessions in `EncryptedSharedPreferences`.
- **`ZyphuelFcmService.kt`**: Listens for Firebase messaging push payloads and displays notification alerts using `@drawable/ic_notification`.
- **`FuelPriceWorker.kt`**: Background WorkManager task for periodic fuel price updates based on admin broadcast schedule.
- **`UnifiedAssetManager.kt`**: Centralizes asset integrity verification for launcher icons and notification drawables.
