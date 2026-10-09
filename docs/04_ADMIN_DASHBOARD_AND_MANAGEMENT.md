# 04. Admin Dashboard & Operations Management Documentation 🛠️

> **Current App Version:** `2.6.4.0.0.24 (Build 52)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

## 📌 Category
**Admin Control & Fleet Operations**

## 🎯 Purpose & Overview
The Admin Dashboard gives administrators total operational control over the Zyphuel platform in Lahore. Admins can view real-time platform metrics, monitor delivery operating hours, register new customers and riders, dispatch fuel orders, oversee tiered pricing compliance, and audit platform security events.

---

## ⚙️ Key Admin Functions & ViewModel Methods
* `addCustomerByAdmin(name, email, phone, password, address)` (`MainViewModel.kt`): Modal dialog registration allowing admins to add new customers manually. Sends welcome email and logs audit event.
* `addRiderFromAdmin(name, email, phone, vehicleType, vehicleNo, cnicNumber, drivingLicense, address, password, autoApprove)` (`MainViewModel.kt`): Registers bowser driver account with vehicle, license, address, and CNIC metadata.
* `approveRiderVerification(riderEmail)` (`MainViewModel.kt`): Approves driver's headshot/biodata verification and grants a Verified Badge.
* `denyRiderVerification(riderEmail)` (`MainViewModel.kt`): Denies driver verification request and records audit event.
* <font color="#10b981"><b>`assignRiderToOrder(orderId, riderEmail, riderName)`</b> (`MainViewModel.kt`): Manually routes pending fuel order to specific rider (Strict 15L cap; Water & LPG unavailable).</font>
* `changeOrderStatus(orderId, nextStatus)` (`MainViewModel.kt`): Updates order lifecycle state (Pending → Assigned → Delivering → Arrived → Completed).
* `getAuditLogs()` (`MainViewModel.kt`): Fetches complete security log history (`AuditLogEntity`).
* <font color="#10b981"><b>`refreshOperatingHoursStatus()`</b> (`MainViewModel.kt`): Monitors active delivery window in PKT (`Asia/Karachi`).</font>

---

## 🖥️ Admin UI Sub-Screens & Modals
1. **Analytics Summary Cards**: Total Revenue (PKR), Active Orders, Registered Customers, Active Bowser Riders.
2. <font color="#10b981"><b>Operating Hours Telematics Card</b>: Real-time status badge showing whether doorstep delivery is currently active or closed across Lahore.</font>
3. **Add Customer Modal (`AddCustomerDialog`)**: Form input with automated email fallback and instant credential generation.
4. **Add Driver Modal (`AddRiderDialog`)**: Complete form for driver name, phone, email, password, vehicle type (Bike/Pickup/Bowser), plate number, CNIC, license ID, address, and instant verification checkbox.
5. **Driver Quick Overview Card (`AdminRiderCard`)**: High-contrast summary grid showing driver Phone, Vehicle Type, Registration Plate No, and CNIC.
6. **Driver Biodata Inspection Modal (`AdminRiderBiodataDialog`)**: Grouped into 4 distinct sections (Personal, Identity, Vehicle, Emergency) displaying only the details provided in the driver form, with explicit Approve & Verify and Deny buttons.
7. **Order Management Table**: Real-time list of all orders with one-tap status transition buttons.
8. **Customer Directory**: View customer profiles, order history, and contact numbers.
9. **Audit Log Viewer**: High-security event history with timestamps and operator identity.

---

## 📁 Source Locations
* **UI Screen**: `app/src/main/java/com/example/ui/Screens.kt` (`AdminDashboardScreen`, `AddCustomerDialog`, `AddRiderDialog`)
* **State Management**: `app/src/main/java/com/example/ui/MainViewModel.kt`
* **Operating Hours**: `app/src/main/java/com/example/util/DeliveryOperatingHoursManager.kt`
* **Data Models**: `app/src/main/java/com/example/data/Models.kt` (`UserEntity`, `OrderEntity`, `AuditLogEntity`)

---

## 🔄 Operations Flow
1. **Login**: Admin logs in via `admin@zyphuel.com`.
2. **Order Dispatch**: Admin views pending orders and assigns an active bowser driver.
3. **Customer Registration**: Admin can register walk-in or phone-order customers directly via `AddCustomerDialog`.
4. **Audit Monitoring**: Admin checks audit logs for rate limit triggers or biometric authentication attempts.
