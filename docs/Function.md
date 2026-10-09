# Zyphuel Application - Functions Documentation (`docs/Function.md`)

> **Current App Version:** `2.6.4.0.0.24 (Build 52)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

This document provides a comprehensive architectural catalog of all key functions, business engines, ViewModel methods, and utility components across the Zyphuel Android codebase.

---

## 1. Operating Hours & Delivery Window Engine (`DeliveryOperatingHoursManager.kt`)

<font color="#10b981"><b>Added in Build 52 (2026-10-09)</b></font> to enforce OGRA logistics compliance and official delivery timings.

### `isDeliveryWindowOpen(calendar: Calendar = Calendar.getInstance(PKT_ZONE)): Boolean`
- **Purpose**: Evaluates whether the current local time in Pakistan Standard Time (`Asia/Karachi` / PKT, UTC+5) is within official fuel doorstep delivery operating hours.
- **Rules Enforced**:
  - <font color="#10b981"><b>Monday – Thursday:</b> 08:00 AM – 08:00 PM PKT (08:00 – 20:00)</font>
  - <font color="#10b981"><b>Friday:</b> 08:00 AM – 01:00 PM PKT (08:00 – 13:00)</font>
  - <font color="#10b981"><b>Saturday – Sunday:</b> 10:00 AM – 06:00 PM PKT (10:00 – 18:00)</font>
- **Return**: `true` if active delivery window is open; `false` otherwise.

### `getNextOpeningTime(calendar: Calendar = Calendar.getInstance(PKT_ZONE)): String`
- **Purpose**: Formats a human-readable display string informing customers when the delivery gate reopens (e.g., `"Tomorrow at 08:00 AM"`, `"Monday at 08:00 AM"`).

### `getOperatingHoursScheduleSummary(): String`
- **Purpose**: Formats the official delivery hours schedule banner for display in FAQs, dialogs, and off-hours notices.

---

## 2. Retail Pump Markup & Tiered Delivery Pricing (`FeeConstants.kt`)

<font color="#10b981"><b>Updated in Build 52 (2026-10-09)</b></font> with exact user-mandated rates.

### `calculateFuelDeliveryFee(liters: Int): Double`
- **Purpose**: Computes the exact doorstep delivery fee based on ordered volume.
- **Per-Liter Tiered Delivery Fee Schedule**:
  - <font color="#10b981"><b>1 to 10 Liters (5L, 7L, 10L):</b> Flat Rs. 300 Delivery Fee</font>
  - <font color="#10b981"><b>11 Liters:</b> Rs. 320 Delivery Fee</font>
  - <font color="#10b981"><b>12 Liters:</b> Rs. 340 Delivery Fee</font>
  - <font color="#10b981"><b>13 Liters:</b> Rs. 360 Delivery Fee</font>
  - <font color="#10b981"><b>14 Liters:</b> Rs. 380 Delivery Fee</font>
  - <font color="#10b981"><b>15 Liters (Strict Max Cap):</b> Rs. 400 Delivery Fee</font>
- **Cap Enforcement**: Rejects / caps orders exceeding 15 Liters.

### `PUMP_RATE_MARKUP = 5.00` (Rs./Litre)
- **Purpose**: Silent background markup added on top of official OGRA ex-depot base rates for:
  - Super Euro-V Petrol (`ograBase + 5.00`)
  - Euro-V Diesel (`ograBase + 5.00`)
  - High-Octane 97 (`ograBase + 5.00`)
- **Display Rule**: UI displays ONLY the final retail pump rate. Markup math runs silently in background with zero debug text.

---

## 3. Order Orchestration & State Management (`MainViewModel.kt`)

### `placeOrder(...)`
- **Purpose**: Validates customer order parameters, enforces delivery operating hours gate, applies tiered delivery fees, writes `OrderEntity` to Room DB, and dispatches invoice email.
- **Recent Enforcements (<font color="#10b981"><b>2026-10-09</b></font>)**:
  - **Operating Hours Gate**: Invokes `DeliveryOperatingHoursManager.isDeliveryWindowOpen()`. If closed, immediately aborts order creation and displays operating hours rejection dialog.
  - **Volume Cap**: Blocks any order $> 15\text{L}$.
  - **Product Availability Gate**: Water and LPG Gas are permanently blocked.
  - **Automated Tax Invoice**: Fires `sendOrderInvoiceEmail(order)` directly to registered user email.

### `acceptOrder(orderId, riderId)`
- **Purpose**: Allows a registered rider to accept an assigned order.
- **Functionality**: Updates `OrderEntity` status to `"Rider Accepted"`, assigns rider vehicle details, and begins live telematics tracking.

### `updateRiderLocation(orderId, lat, lng)`
- **Purpose**: Streams real-time rider latitude/longitude coordinates and recalculates remaining distance and ETA in minutes.
- **Functionality**: Smooth coordinate interpolation for real-time bowser icon movement on `UnifiedGoogleMapView`.

### `refreshSecurityAndBiometricStates(context)`
- **Purpose**: Queries `SecureStorageManager` and `BiometricSecurityManager` to refresh UI state flows for active biometric enrollment.

### `sendOrderInvoiceEmail(order, isCompletedReceipt)`
- **Purpose**: Dispatches itemized HTML + text invoice to customer's registered email via Webhook/SMTP channels.

---

## 4. UI Screens & Interactive Composables (`Screens.kt`)

### `UnifiedGoogleMapView(...)`
- **Purpose**: Core interactive Leaflet/Google WebView map composable that renders user pin, nearby petrol pumps, delivery route, and rider vehicle overlay.
- **Functionality**:
  - Nearby petrol pump markers (PSO, Shell, TotalParco) with distance callouts.
  - Real-time vehicle movement animations and clear ETA in minutes overlay.

### `CustomerHomeScreen(...)`
- **Purpose**: Main customer dashboard for fuel selection, quantity picker, pump selection, and order placement.
- **Recent Enforcements (<font color="#10b981"><b>2026-10-09</b></font>)**:
  - Dynamically observes `DeliveryOperatingHoursManager.isDeliveryWindowOpen()`.
  - Outside operating hours: Hides "Order Now" action button and floating action buttons (FAB), displaying clear off-hours card with next opening time.

### `DriverRealTimeTrackingMap(...)`
- **Purpose**: Live tracking map displayed to customers during active delivery.
- **Functionality**: Shows rider vehicle movement, ETA, and direct phone contact.

### `AdminFuelPriceNotificationScheduleCard(...)`
- **Purpose**: Admin control component for setting periodic broadcast intervals (1h, 2h, 4h, 6h, 12h, 24h) and triggering instant price alert broadcasts.

---

## 5. Security & Biometrics (`SecuritySettingsScreen.kt`, `BiometricSecurityManager.kt`)

### `SecuritySettingsScreen(...)`
- **Purpose**: Render the security and biometrics settings screen for Customer, Rider, and Admin roles.
- **Functionality**:
  - Displays fingerprint enrollment status with `testTag` attributes.
  - Invokes `BiometricSecurityManager.showBiometricPrompt` to register device credentials.

---

## 6. Real-Time Telematics Service (`LocationService.kt`)

### `startPersistentLocationUpdates(context, onLocationUpdate)`
- **Purpose**: Starts persistent high-accuracy real-time GPS tracking using `FusedLocationProviderClient` with `Priority.PRIORITY_HIGH_ACCURACY` and application context.
- **Memory Safety**: Uses `context.applicationContext` preventing leaks across activity lifecycles.

### `fetchFreshSingleLocation(context, onLocationResult, onError)`
- **Purpose**: Obtains single fresh GPS coordinate pair with cancellation token and request update fallbacks.

---

## 7. High-Frequency Git Graph Engine (`scripts/generate_github_graph.js`)

<font color="#10b981"><b>Added in Build 52 (2026-10-09)</b></font>

### `generateTradingStyleCommitGraph()`
- **Purpose**: Analyzes complete git history (`%h|%ad|%at|%s`) and computes:
  - Exact push timestamps and intervals between consecutive pushes.
  - Multi-timeframe velocity tracking across **Daily (1D)**, **Weekly (1W)**, and **Monthly (1M)** buckets.
  - Incline/decline trend arrows (Bullish surge vs. consolidation/cooldown).
  - High-res vector SVG trading terminal chart output at `.github/assets/repo-activity-chart.svg`.
  - Synchronized Markdown telemetry block in `README.md`.
