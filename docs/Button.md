# Zyphuel Application - Buttons Catalog (`docs/Button.md`)

This document documents all interactive buttons across the application, including their `testTag` identifiers, screen locations, state bindings, and click actions.

---

## 1. Security & Biometrics Buttons (`SecuritySettingsScreen.kt`)

| Button Name | `testTag` | Location | State / Condition | Action / Trigger |
| :--- | :--- | :--- | :--- | :--- |
| **Back Button** | `security_settings_back_btn` | Top App Bar | Always Enabled | Returns to parent drawer screen |
| **Enable Biometric** | `enable_biometric_action_btn` | Biometric Options Card | Enabled when biometrics disabled | Triggers `BiometricSecurityManager.showBiometricPrompt` to enroll fingerprint |
| **Disable Biometric** | `disable_biometric_action_btn` | Biometric Options Card | Enabled when biometrics enabled | Calls `viewModel.disableBiometricForModule(...)` to revoke biometric session |

---

## 2. Customer Home & Ordering Buttons (`Screens.kt`)

| Button Name | `testTag` | Location | State / Condition | Action / Trigger |
| :--- | :--- | :--- | :--- | :--- |
| **Place Order** | `place_fuel_order_btn` | Customer Home Screen | Enabled when fuel type & qty selected | Opens order confirmation dialog with map preview and per-km charge breakdown |
| **Confirm Fuel Order** | `confirm_order_dialog_btn` | Order Confirmation Modal | Enabled when address valid | Confirms order, initiates rider search ("Searching for nearby rider..."), and records Room order entity |
| **Cancel Order** | `cancel_order_dialog_btn` | Order Confirmation Modal | Always Enabled | Closes order modal without placing order |
| **Fine-Tune Pin on Map** | `fine_tune_map_pin_btn` | Location Picker Section | Always Enabled | Opens `InteractiveLocationPickerMap` to adjust location pin |
| **Zoom In Map** | `map_zoom_in_btn` | Interactive Map Overlay | Always Enabled | Zoom in Leaflet/Google map view |
| **Zoom Out Map** | `map_zoom_out_btn` | Interactive Map Overlay | Always Enabled | Zoom out Leaflet/Google map view |
| **Recenter Map** | `map_recenter_btn` | Interactive Map Overlay | Always Enabled | Recenters map view onto customer delivery coordinates |
| **Call Rider** | `call_rider_action_btn` | Rider Profile Dialog | Enabled when rider assigned | Launches system dialer with rider's phone number |

---

## 3. Rider Dashboard Buttons (`Screens.kt`)

| Button Name | `testTag` | Location | State / Condition | Action / Trigger |
| :--- | :--- | :--- | :--- | :--- |
| **Accept Delivery Order** | `rider_accept_order_btn` | Rider Request Feed | Enabled for pending orders | Accepts order at rider discretion, assigning vehicle and launching active navigation map |
| **Decline Order** | `rider_decline_order_btn` | Rider Request Feed | Always Enabled | Dismisses order request from feed |
| **Mark Order Delivered** | `rider_mark_delivered_btn` | Active Delivery Tracker | Enabled when at location | Updates order status to "Delivered", notifies customer, and logs audit entry |

---

## 4. Admin Center Buttons (`Screens.kt`)

| Button Name | `testTag` | Location | State / Condition | Action / Trigger |
| :--- | :--- | :--- | :--- | :--- |
| **Update Broadcast Schedule** | `admin_save_schedule_btn` | Fuel Prices Tab | Always Enabled | Updates auto-broadcast interval hours in `FuelPriceWorker` WorkManager |
| **Broadcast Test Alert** | `admin_test_broadcast_btn` | Fuel Prices Tab | Always Enabled | Sends an instant FCM test push alert to all connected devices |
| **Update Fuel Prices** | `admin_update_fuel_prices_btn` | Fuel Prices Tab | Enabled when price inputs valid | Updates petrol, diesel, and hi-octane rates across the platform |
| **Verify Rider Account** | `admin_verify_rider_btn` | Rider Management Tab | Enabled for unverified riders | Grants official rider verification badge and vehicle delivery authorization |

---

## 5. Sidebar & Navigation Drawer Buttons (`MainActivity.kt`)

| Button Name | `testTag` | Location | State / Condition | Action / Trigger |
| :--- | :--- | :--- | :--- | :--- |
| **Customer Navigation Drawer Item** | `drawer_item_customer_home` | Sidebar | Always Enabled | Navigates to Customer Home Screen |
| **Rider Navigation Drawer Item** | `drawer_item_rider_home` | Sidebar | Always Enabled | Navigates to Rider Dashboard Screen |
| **Admin Panel Drawer Item** | `drawer_item_admin_panel` | Sidebar | Enabled for Admin role | Navigates to Admin Center |
| **Security & Biometrics Item** | `drawer_item_security_settings` | Sidebar | Always Enabled | Navigates to `SecuritySettingsScreen` |
| **Logout Button** | `drawer_logout_btn` | Sidebar Footer | Always Enabled | Logs out current session, clears encrypted tokens, and returns to Auth Screen |
