# 05. Rider / Bowser Driver Portal Documentation 🚚

## 📌 Category
**Driver Fleet Operations**

## 🎯 Purpose & Overview
The Rider Portal equips bowser drivers with an intuitive mobile workflow to manage assigned doorstep fuel and water deliveries across Lahore. Drivers can view delivery destinations, tap to update order status, call customers, and broadcast live GPS coordinates.

---

## ⚙️ Key Driver Functions & ViewModel Methods
* `acceptOrder(orderId)` (`MainViewModel.kt`): Rider accepts assigned order and changes status to `Assigned`.
* `changeOrderStatus(orderId, "Delivering")` (`MainViewModel.kt`): Rider starts journey; status changes to `Delivering` / `Out for Delivery`.
* `changeOrderStatus(orderId, "Arrived")` (`MainViewModel.kt`): Rider notifies customer of arrival at destination (`Reached Location 📍`).
* `changeOrderStatus(orderId, "Completed")` (`MainViewModel.kt`): Rider completes fuel dispensing and collects Cash on Delivery (COD).

---

## 📱 Driver Portal Screens & Components
1. **Rider Duty Queue (`RiderHomeScreen`)**: Displays assigned orders with customer name, phone number, volume (L), total PKR, and address.
2. **One-Tap Status Buttons**:
   * `Reached Location 📍`: Changes status to `Arrived` and triggers high-priority push notification.
   * `Mark Delivered 🎉`: Finalizes delivery and opens COD receipt dialog.
3. **Navigation Launcher**: Deep-links to Google Maps / Waze for turn-by-turn navigation in Lahore.
4. **Customer Call CTA**: Direct dial button (`tel:customerPhone`).

---

## 📁 Source Locations
* **UI Screen**: `app/src/main/java/com/example/ui/Screens.kt` (`RiderHomeScreen`)
* **State Management**: `app/src/main/java/com/example/ui/MainViewModel.kt`
* **Data Model**: `app/src/main/java/com/example/data/Models.kt` (`OrderEntity`)

---

## 🔄 Delivery Workflow
1. **Assignment**: Driver receives push notification of new order assignment.
2. **En Route**: Driver taps "Start Delivery" (`Delivering`), initiating live GPS map tracking for customer.
3. **Arrival**: Driver reaches customer premises and taps "Reached Location 📍" (`Arrived`).
4. **Fuel Dispensed & Payment**: Driver dispenses fuel/water, collects cash, and taps "Mark Delivered 🎉".
