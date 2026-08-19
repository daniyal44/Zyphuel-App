# Zyphuel Application - Admin Panel Documentation (`docs/Admin_panel.md`)

This document describes the structure, controls, and features of the **Admin Panel** (`AdminDashboardScreen` in `Screens.kt`).

---

## 1. Overview
The Admin Panel serves as the central command hub for Zyphuel system administrators. It controls system-wide fuel pricing, auto-broadcast notification schedules, order dispatches, user account verification, and security audit logs.

---

## 2. Key Modules & Controls

### A. Centralized Fuel Price & Broadcast Schedule Manager (`AdminFuelPriceNotificationScheduleCard`)
- **Fuel Price Rates Control**: Updates market rates for Petrol (E10), High-Speed Diesel (HSD), and Hi-Octane (HOBC) in PKR per liter.
- **Auto-Broadcast Schedule Configurator**:
  - Sets the exact hour interval after which fuel price update notifications automatically broadcast to all system users (e.g., 1 Hour, 2 Hours, 4 Hours, 6 Hours, 12 Hours, 24 Hours).
  - Integrates directly with `FuelPriceWorker` WorkManager.
  - Allows toggling broadcasts ON/OFF platform-wide.
  - Provides a "Broadcast Test Alert Now" button (`admin_test_broadcast_btn`) for instant FCM push notifications.

### B. Customer Order Oversight & Dispatch Monitoring
- **Live Order Stream**: Displays all pending, confirmed, active, and completed fuel delivery orders across the network.
- **Rider Dispatch Status**: Monitors rider assignment state ("Searching for nearby rider...", "Rider Accepted", "Delivering", "Delivered").
- **Manual Override**: Ability to reassign riders or cancel disputed orders.

### C. Rider & User Management
- **Rider Verification**: Inspects uploaded driver licenses and vehicle registration documents before granting verified status.
- **Vehicle Type Registration**: Assigns vehicle classification (e.g. 5,000L Fuel Bowser, Fuel Tanker, Delivery Bike) to verified riders.
- **Role Assignment**: Elevates or revokes user account privileges (Customer, Rider, Admin).

### D. Security & Audit Logging
- **Room Database Audit Log (`AuditLogEntity`)**: Tracks sensitive system actions including fuel rate modifications, admin price broadcasts, role changes, and failed login attempts.
- **Tamper Alert Review**: Displays security warnings flagged by `RootAndSecurityDetector`.

---

## 3. UI & Accessibility Design
- Built with Material 3 tabs, high-contrast typography, and intuitive card containers.
- All interactive buttons feature unique `testTag` attributes (e.g. `admin_save_schedule_btn`, `admin_test_broadcast_btn`, `admin_update_fuel_prices_btn`).
