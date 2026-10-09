# Zyphuel Application - Admin Panel Documentation (`docs/Admin_panel.md`)

> **Current App Version:** `2.6.4.0.0.26 (Build 54)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

This document describes the structure, controls, telematics monitoring, and administrative governance features of the **Admin Panel** (`AdminDashboardScreen` in `Screens.kt`).

---

## 1. Overview
The Admin Panel serves as the central mission control hub for Zyphuel operations in Lahore. It governs system-wide retail pump rates, automated price broadcast schedules, delivery operating hours monitoring, live fleet dispatches, account verifications, and regulatory audit logging.

---

## 2. Key Modules & Administrative Controls

### A. Centralized Fuel Pricing & OGRA Markup Engine
- **Base Rate & Silent Markup**: Oversees official OGRA ex-depot base rates and the standard <font color="#10b981"><b>`PUMP_RATE_MARKUP = 5.00` Rs./Litre</b></font> silent markup for:
  - Super Euro-V Petrol
  - Euro-V Diesel
  - High-Octane 97
- **Auto-Broadcast Schedule Configurator**:
  - Sets the exact hour interval after which fuel price update notifications automatically broadcast to all users (1h, 2h, 4h, 6h, 12h, 24h).
  - Integrates directly with `FuelPriceWorker` WorkManager.
  - Features instant test alert trigger (`admin_test_broadcast_btn`).

### B. Delivery Operating Hours Monitoring
<font color="#10b981"><b>Updated in Build 52 (2026-10-09)</b></font>
- Monitors active operating hours gate in Pakistan Standard Time (`Asia/Karachi` / PKT):
  - <font color="#10b981"><b>Mon–Thu:</b> 08:00 AM – 08:00 PM PKT</font>
  - <font color="#10b981"><b>Fri:</b> 08:00 AM – 01:00 PM PKT</font>
  - <font color="#10b981"><b>Sat–Sun:</b> 10:00 AM – 06:00 PM PKT</font>
- Verifies that outside these hours, order intake is completely blocked at ViewModel/Database layers and action buttons remain hidden.

### C. Tiered Delivery Pricing & Product Availability Oversight
- **Tiered Delivery Fee Schedule**:
  - <font color="#10b981"><b>1 to 10 Liters (5L, 7L, 10L):</b> Flat Rs. 300 Delivery Fee</font>
  - <font color="#10b981"><b>11 Liters:</b> Rs. 320 Delivery Fee</font>
  - <font color="#10b981"><b>12 Liters:</b> Rs. 340 Delivery Fee</font>
  - <font color="#10b981"><b>13 Liters:</b> Rs. 360 Delivery Fee</font>
  - <font color="#10b981"><b>14 Liters:</b> Rs. 380 Delivery Fee</font>
  - <font color="#10b981"><b>15 Liters (Strict Max Cap):</b> Rs. 400 Delivery Fee</font>
- **Product Status**: Enforces that **Pure Water** and **LPG Gas Cylinders** remain strictly marked as **UNAVAILABLE**.

### D. Customer Order Oversight & Dispatch Monitoring
- **Live Order Stream**: Displays all pending, confirmed, active, and completed fuel delivery orders across Lahore.
- **Rider Dispatch Status**: Monitors rider assignment states (`"Searching for nearby rider..."`, `"Rider Accepted"`, `"Delivering"`, `"Delivered"`).
- **Self-Delivery & Manual Override**: Allows admin to manually assign emergency standby bowser drivers or handle customer escalations.

### E. Rider Verification & Fleet Management
- **Rider Verification**: Inspects uploaded driver CNIC and commercial driving licenses before granting verified status (`admin_verify_rider_btn`).
- **Vehicle Type Registration**: Classifies bowser vehicles (e.g., 5,000L Fuel Bowser, Fuel Tanker, Delivery Bike).

### F. Security, Audit Logging & Play Store Compliance
- **Audit Logs (`AuditLogEntity`)**: Tracks system events (fuel rate updates, broadcast alerts, role changes, failed logins).
- **Account Deletion Handling**: Complies with Google Play Store policies by processing user deletion requests and wiping associated telemetry records.
- **Root/Frida Detection**: Alerts admin if an untrusted or rooted device attempts administrative actions.
