# Zyphuel Application - Sidebar & Navigation Architecture (`docs/Side_bar.md`)

> **Current App Version:** `2.6.4.0.0.25 (Build 53)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

This document details the navigation sidebar drawer structure, role-based menu options, operating hours indicators, and routing logic implemented in `MainActivity.kt`.

---

## 1. Overview
The sidebar navigation drawer in **Zyphuel** provides a role-aware modal layout that dynamically adapts based on whether the active user is logged in as a **Customer**, **Rider**, or **Admin**.

```
Zyphuel App Sidebar Drawer
 ├── Header Section (User Name, Email, Profile Avatar, Role Badge)
 ├── Operating Hours Live Indicator (Open / Closed Status Badge)
 ├── Primary Menu Options (Role-Specific)
 │    ├── Customer Options: Home, Order History, Station Finder, Active Tracking
 │    ├── Rider Options: Available Requests, Active Delivery, Earnings, Vehicle Profile
 │    └── Admin Options: Dashboard, Order Oversight, Fuel Prices, User Management, Audit Logs
 ├── Global Utilities & Legal Disclosures
 │    ├── Security & Biometrics (Fingerprint Authentication)
 │    ├── Notification Center
 │    ├── Terms of Service & Privacy Policy (Play Store Compliance)
 │    ├── Permanent Account Deletion (Google Play User Data Policy)
 │    └── Support Hotline (WhatsApp Direct)
 └── Footer Section (App Version v2.6.4.0.0.25 Build 53, Logout Action)
```

---

## 2. Menu Navigation Items & Routes

### A. Customer Drawer Items
1. **Home / Place Order (`ROUTE_CUSTOMER_HOME`)**: Main fuel ordering screen with nearby pumps map, silent retail pump rates, and tiered delivery fee calculator.
2. **Order History (`ROUTE_ORDER_HISTORY`)**: List of past and active orders with receipt viewing and re-order actions (re-order hidden when delivery window closed).
3. **Nearby Stations (`ROUTE_STATIONS_MAP`)**: Full-screen interactive map displaying petrol pump locations (PSO, Shell, TotalParco) in Lahore.
4. **Security & Biometrics (`ROUTE_SECURITY_SETTINGS`)**: Navigates to `SecuritySettingsScreen` to manage fingerprint authentication.

### B. Rider Drawer Items
1. **Order Requests (`ROUTE_RIDER_HOME`)**: Real-time order dispatch feed where riders accept or decline delivery requests at their discretion.
2. **Active Delivery Navigation (`ROUTE_RIDER_ACTIVE_NAV`)**: Real-time GPS navigation map displaying customer location, delivery route, and vehicle movement.
3. **Vehicle Profile & Registration (`ROUTE_RIDER_VEHICLE`)**: Displays registered vehicle type (e.g., 5,000L Fuel Bowser, Fuel Tanker, Bike), registration plate, and verification status.
4. **Security & Biometrics (`ROUTE_SECURITY_SETTINGS`)**: Fingerprint biometric login settings for riders.

### C. Admin Drawer Items
1. **Admin Center (`ROUTE_ADMIN_PANEL`)**: Comprehensive administration dashboard.
2. **Fuel Price Broadcast Schedule (`ROUTE_ADMIN_PRICES`)**: Houses `AdminFuelPriceNotificationScheduleCard` to manage platform fuel prices and auto-broadcast intervals (1h, 2h, 4h, 6h, 12h, 24h).
3. **Audit Log Inspection (`ROUTE_ADMIN_AUDIT`)**: Real-time security and system activity logs stored in Room DB.
4. **Security & Biometrics (`ROUTE_SECURITY_SETTINGS`)**: Admin fingerprint security configuration.

### D. Legal, Privacy & Compliance Items
<font color="#10b981"><b>Updated in Build 52 (2026-10-09)</b></font>
1. **Terms & Privacy Policy**: Launches in-app legal dialog (`TermsAndPrivacyDialog.kt`) reflecting official delivery hours and tiered delivery fees.
2. **Permanent Account Deletion**: Provides direct 2-click account and data erasure in compliance with Google Play Developer Policies.

---

## 3. Sidebar Security & UI Policy Compliance
- **Strict Role Isolation**: Admin options are hidden and inaccessible to non-admin accounts.
- **Delivery Operating Window Badge**: Displays live status indicator reflecting whether the delivery gate is open:
  - <font color="#10b981"><b>Mon–Thu:</b> 08:00 AM – 08:00 PM PKT</font>
  - <font color="#10b981"><b>Fri:</b> 08:00 AM – 01:00 PM PKT</font>
  - <font color="#10b981"><b>Sat–Sun:</b> 10:00 AM – 06:00 PM PKT</font>
- **Version Footer**: Displays current release version <font color="#10b981"><b>`v2.6.4.0.0.25 (Build 53)`</b></font>.
