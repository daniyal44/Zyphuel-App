# Zyphuel Application - Sidebar & Navigation Architecture (`docs/Side_bar.md`)

This document details the navigation sidebar drawer structure, role-based menu options, and routing logic implemented in `MainActivity.kt`.

---

## 1. Overview
The sidebar navigation drawer in **Zyphuel** provides a role-aware modal layout that adjusts dynamically based on whether the active user is logged in as a **Customer**, **Rider**, or **Admin**.

```
Zyphuel App Sidebar Drawer
 ├── Header Section (User Name, Email, Profile Avatar, Role Badge)
 ├── Primary Menu Options (Role-Specific)
 │    ├── Customer Options: Home, Order History, Station Finder, Active Tracking
 │    ├── Rider Options: Available Requests, Active Delivery, Earnings, Vehicle Profile
 │    └── Admin Options: Dashboard, Order Oversight, Fuel Prices, User Management, Audit Logs
 ├── Global Utilities
 │    ├── Security & Biometrics (Fingerprint Authentication)
 │    ├── Notification Center
 │    └── Support Hotline (WhatsApp Direct)
 └── Footer Section (App Version, Logout Action)
```

---

## 2. Menu Navigation Items & Routes

### A. Customer Drawer Items
1. **Home / Place Order (`ROUTE_CUSTOMER_HOME`)**: Main fuel ordering screen with nearby pumps map and per-km delivery fee calculator.
2. **Order History (`ROUTE_ORDER_HISTORY`)**: List of past and active orders with receipt downloading and status tracking.
3. **Nearby Stations (`ROUTE_STATIONS_MAP`)**: Full-screen interactive map displaying petrol pump locations (PSO, Shell, TotalParco) in Lahore and surrounding regions.
4. **Security & Biometrics (`ROUTE_SECURITY_SETTINGS`)**: Navigates to `SecuritySettingsScreen` to manage fingerprint authentication. *(Note: Fuel Price Update Alerts section removed as per admin centralization).*

### B. Rider Drawer Items
1. **Order Requests (`ROUTE_RIDER_HOME`)**: Real-time order dispatch feed where riders accept or decline delivery requests at their discretion.
2. **Active Delivery Navigation (`ROUTE_RIDER_ACTIVE_NAV`)**: Real-time GPS navigation map displaying customer location, delivery route, and vehicle movement.
3. **Vehicle Profile & Registration (`ROUTE_RIDER_VEHICLE`)**: Displays registered vehicle type (e.g. 5,000L Fuel Bowser, Fuel Tanker, Bike), registration plate, and verification status.
4. **Security & Biometrics (`ROUTE_SECURITY_SETTINGS`)**: Fingerprint biometric login settings for riders.

### C. Admin Drawer Items
1. **Admin Center (`ROUTE_ADMIN_PANEL`)**: Comprehensive administration dashboard.
2. **Fuel Price Broadcast Schedule (`ROUTE_ADMIN_PRICES`)**: Houses `AdminFuelPriceNotificationScheduleCard` to manage platform fuel prices and auto-broadcast intervals (1h, 2h, 4h, 6h, 12h, 24h).
3. **Audit Log Inspection (`ROUTE_ADMIN_AUDIT`)**: Real-time security and system activity logs stored in Room DB.
4. **Security & Biometrics (`ROUTE_SECURITY_SETTINGS`)**: Admin fingerprint security configuration.

---

## 3. Sidebar Security & UI Policy Compliance
- **Strict Role Isolation**: Admin options are hidden and inaccessible to non-admin accounts.
- **Biometric Convenience**: Tapping "Security & Biometrics" allows quick enrollment without re-entering passwords.
- **Clean Layout**: Designed according to Material 3 navigation drawer guidelines with generous touch targets (min 48dp) and accessibility content descriptions.
