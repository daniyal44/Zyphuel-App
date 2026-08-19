# Zyphuel Application - Functions Documentation (`docs/Function.md`)

This document provides a complete catalog of all key functions in the codebase, detailed by file, along with the specific changes implemented.

---

## 1. `SecuritySettingsScreen.kt`

### `SecuritySettingsScreen(...)`
- **Purpose**: Render the security and biometrics settings screen for Customer, Rider, and Admin roles.
- **Functionality**:
  - Displays fingerprint biometrics enrollment status.
  - Provides interactive "Enable Biometric" and "Disable Biometric" buttons with `testTag` attributes.
  - Invokes `BiometricSecurityManager.showBiometricPrompt` to register device credentials.
- **Recent Changes Made**:
  - **Removed Redundant Section**: Deleted the obsolete "Fuel Price Update Alerts" card from the bottom of `SecuritySettingsScreen`.
  - **Centralized Admin Alignment**: Delegated all fuel price alert scheduling exclusively to the Admin Center (`AdminFuelPriceNotificationScheduleCard`).

---

## 2. `MainViewModel.kt`

### `placeOrder(...)`
- **Purpose**: Validates customer order parameters, calculates distance charges from nearest petrol pump, creates an `OrderEntity`, and inserts it into Room DB.
- **Recent Changes Made**:
  - Updated distance charge logic (e.g. PKR 50 per kilometer from selected pump).
  - Initiates rider search state ("Searching for nearby rider...") upon order creation.

### `acceptOrder(orderId, riderId)`
- **Purpose**: Allows a registered rider to accept an assigned or pending order at their discretion.
- **Recent Changes Made**:
  - Updates `OrderEntity` status to `"Rider Accepted"`, assigns rider vehicle details, and begins live vehicle movement updates on the map.

### `updateRiderLocation(...)`
- **Purpose**: Streams real-time rider latitude/longitude coordinates and recalculates remaining distance and ETA in minutes.
- **Recent Changes Made**:
  - Smooth coordinate interpolation for real-time vehicle movement visualization.

### `refreshSecurityAndBiometricStates(context)`
- **Purpose**: Queries `SecureStorageManager` and `BiometricManager` to refresh UI state flows for active biometric enrollment.

### `getFcmToken()`
- **Purpose**: Obtains or generates Firebase Cloud Messaging token for push notifications.
- **Recent Changes Made**:
  - Replaced deprecated task access with `addOnCompleteListener` listener pattern.

---

## 3. `Screens.kt`

### `UnifiedGoogleMapView(...)`
- **Purpose**: Core interactive Leaflet/Google WebView map composable that renders user pin, nearby petrol pumps, delivery route, and rider vehicle overlay.
- **Recent Changes Made**:
  - Added nearby petrol pump markers (PSO, Shell, TotalParco) with distance callouts.
  - Enhanced rider vehicle icon display (e.g. 5,000L Fuel Bowser, Fuel Tanker).
  - Added real-time vehicle movement animations and clear ETA in minutes overlay.

### `DriverRealTimeTrackingMap(...)`
- **Purpose**: Specialized tracking map composable displayed to customers during active delivery.
- **Recent Changes Made**:
  - Displays registered rider name, phone number, vehicle registration number, and live progress bar.

### `CustomerHomeScreen(...)`
- **Purpose**: Main customer dashboard for fuel selection, quantity picker, pump selection, and order placement.
- **Recent Changes Made**:
  - Displays transparent per-kilometer delivery fee calculation before order confirmation.

### `AdminFuelPriceNotificationScheduleCard(...)`
- **Purpose**: Admin control component for setting periodic broadcast intervals (1h, 2h, 4h, 6h, 12h, 24h) and triggering instant price alert broadcasts.

---

## 4. `ZyphuelFcmService.kt`

### `onMessageReceived(remoteMessage)`
- **Purpose**: Handles incoming FCM push messages when app is in foreground or background.

### `showNotification(title, message)`
- **Purpose**: Builds and posts system notification in Android notification shade.
- **Recent Changes Made**:
  - Configured builder to use `@drawable/ic_notification` (compliant vector icon) ensuring compatibility with Android 12+ System UI rules.

---

## 5. `FuelPriceWorker.kt`

### `doWork()`
- **Purpose**: Background WorkManager task executed periodically according to the admin broadcast schedule. Fetches latest fuel prices and fires local notifications.

---

## 7. `LocationService.kt`

### `startPersistentLocationUpdates(context, onLocationUpdate)`
- **Purpose**: Starts persistent high-accuracy real-time GPS tracking using `FusedLocationProviderClient` with `Priority.PRIORITY_HIGH_ACCURACY` and application context.
- **Recent Changes Made**:
  - Refactored to utilize `context.applicationContext` preventing memory leaks across activity lifecycles.
  - Exposes `locationError` and `isLocationAvailable` state flows to detect GPS signal weak states and trigger manual address overrides seamlessly.

### `fetchFreshSingleLocation(context, onLocationResult, onError)`
- **Purpose**: Obtains a single fresh GPS coordinate pair using `getCurrentLocation` with cancellation token and request update fallbacks.
- **Recent Changes Made**:
  - Integrated graceful fallback to default Lahore landmarks if hardware GPS is unavailable or permissions are denied.

