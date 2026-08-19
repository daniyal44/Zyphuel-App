# Zyphuel Application - Features Documentation (`docs/Feature.md`)

This document outlines all major functional features available in the **Zyphuel** application.

---

## 1. Interactive Fuel Ordering & Map Engine
- **Nearby Petrol Pumps Display**: Interactive map automatically displays nearby verified petrol stations (e.g. PSO, Shell, TotalParco) with marker pins and pump names.
- **Distance-Based Delivery Charge**: Dynamically calculates the distance (in kilometers) from the selected petrol pump to the customer's delivery location and applies a transparent per-kilometer delivery fee (e.g., PKR 50/km).
- **Interactive Location Picker**: Customers can fine-tune their delivery location on the map with real-time latitude/longitude reverse geocoding.
- **Rider Dispatch & Search State**: Upon order confirmation, the map transitions into a searching state ("Searching for nearby rider..."), displaying an animated visual indicator until a rider accepts the order.
- **Discretionary Rider Acceptance**: Registered riders receive real-time order requests on their dashboard and can choose to accept or decline orders at their discretion.
- **Real-Time Vehicle & Rider Tracking**: Once accepted, the map shows the rider's registered vehicle (e.g., 5,000L Fuel Bowser, Fuel Tanker, Delivery Bike) moving smoothly along the route towards the delivery destination.
- **Estimated Time of Arrival (ETA)**: Displays continuous ETA in minutes (e.g., "~8 mins remaining") alongside total route distance on the tracking overlay.

---

## 2. Multi-Role Navigation & Security Architecture
- **Role-Based Workflows**:
  - **Customer Module**: Fuel ordering, live order tracking, location marking, order history, and profile management.
  - **Rider Module**: Order dispatch feed, discretionary order acceptance, active navigation map, delivery fulfillment, and earnings summary.
  - **Admin Panel**: Centralized management for fuel prices, broadcast notification schedules, user account role controls, order status oversight, and security audit logs.
- **Biometric Security Integration**: Fingerprint authentication via `BiometricSecurityManager` for instant, secure logins for Customer, Rider, and Admin accounts.
- **Root & Security Tamper Detection**: Proactive detection of rooted devices, tampered builds, and unauthorized execution environments via `RootAndSecurityDetector`.

---

## 3. Notification & Broadcast System
- **Admin-Controlled Broadcast Schedule**: Central administrator configures the auto-broadcast interval (e.g., every 1, 2, 4, 6, 12, or 24 hours) for real-time fuel price updates.
- **Firebase Cloud Messaging (FCM)**: Foreground and background push notification processing via `ZyphuelFcmService.kt`, utilizing the compliant vector icon `@drawable/ic_notification`.
- **In-App Notification Center**: Local Room DB persistence (`NotificationEntity`) maintaining a searchable log of all customer and system alerts.

---

## 4. Google Play Store Policy Compliance
- **100% Policy Compliant**: Clean manifest configuration, verified vector drawables, runtime dynamic permission requests (Location, Notifications, Biometrics), and zero hardcoded credentials.
- **Data Privacy & Security**: Encrypted local storage using `EncryptedSharedPreferences`, input sanitization, and structured audit logging.
