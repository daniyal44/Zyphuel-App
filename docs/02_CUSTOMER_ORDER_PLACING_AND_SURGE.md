# 02. Customer Order Placing & Operating Hours Gate Documentation ⛽

**Last Updated:** <font color="#10b981"><b>2026-10-09</b></font> • **Application Version:** <font color="#10b981"><b>2.6.4.0.0.24 (Build 52)</b></font>

## 📌 Category
**Customer Commerce & Order Management Engine**

## 🎯 Purpose & Overview
The Order Placement engine allows customers to order doorstep fuel (Super Euro-V Petrol, High-Speed Diesel, and High-Octane 97) in Lahore, Punjab. It enforces a strict 15-liter public safety cap, a standardized silent OGRA retail pump markup, granular per-liter tiered delivery charges, and strict operational hours gate enforcement.

---

## 🕒 Official Delivery Operating Hours & Checkout Gate (<font color="#10b981"><b>2026-10-09</b></font>)
Doorstep deliveries operate strictly during official delivery windows (Pakistan Standard Time, UTC+5):
- **Monday – Thursday**: <font color="#10b981"><b>08:00 AM – 08:00 PM PKT</b></font>
- **Friday**: <font color="#10b981"><b>08:00 AM – 01:00 PM PKT</b></font>
- **Saturday – Sunday**: <font color="#10b981"><b>10:00 AM – 06:00 PM PKT</b></font>

**UI Actions Suppressed Outside Operating Hours:**
* **Floating Action Button**: The "Order Now" FAB (`home_fab`) on `CustomerHomeScreen` is completely omitted when closed.
* **Modal Confirmation Button**: The "Confirm Order COD" button (`confirmButton`) in `OrderDialog` is hidden when closed.
* **Past Order Reorder**: The "Reorder 🔁" button on `CustomerOrderCard` is hidden when closed.
* **Hard Guard**: `MainViewModel.placeOrder()` enforces `isDeliveryOpen` check and rejects submissions outside operating hours.

---

## 💰 Per-Liter Tiered Delivery Pricing (<font color="#10b981"><b>2026-10-09</b></font>)
Enforced in [`FeeConstants.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/FeeConstants.kt) via `calculateFuelDeliveryFee(liters)`:
- **1 to 10 Liters (including 5L, 7L, 10L)**: <font color="#10b981"><b>Rs. 300.00</b></font> (Flat Rs. 300 for up to 10L)
- **11 Liters**: <font color="#10b981"><b>Rs. 320.00</b></font>
- **12 Liters**: <font color="#10b981"><b>Rs. 340.00</b></font>
- **13 Liters**: <font color="#10b981"><b>Rs. 360.00</b></font>
- **14 Liters**: <font color="#10b981"><b>Rs. 380.00</b></font>
- **15 Liters (Strict Max Cap)**: <font color="#10b981"><b>Rs. 400.00</b></font>
- *Linear Step Formula:* `300.00 + (liters - 10) * 20.00` for $11 \le \text{liters} \le 15$.

---

## ⛽ Fuel Types & Silent OGRA Pump Markup (<font color="#10b981"><b>2026-10-09</b></font>)
- **Markup Constant**: `PUMP_RATE_MARKUP = 5.00` (Rs./Litre) applied over official OGRA ex-depot base rates (`finalRate = ograBase + 5.00`).
- **Silent Background Math**: Only the final pump rate is shown across UI screens without debug or surcharge labels.
- **Available Products**:
  - **Super Euro-V Petrol**: Retail pump rate calculated dynamically.
  - **Euro-V High-Speed Diesel (HSD)**: Retail pump rate calculated dynamically.
  - **High-Octane 97 (HOBC)**: Retail pump rate calculated dynamically.
- **Unavailable Products**:
  - **Pure Mineral Drinking Water**: Currently **UNAVAILABLE** (blocked across all screens).
  - **LPG Gas Cylinders**: Currently **UNAVAILABLE** (blocked across all screens).

---

## ⚙️ Key Functions & ViewModel Methods
* `placeOrder(serviceType, volumeLiters, deliveryAddress, customerEmail, customerName)` ([`MainViewModel.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/ui/MainViewModel.kt)): Validates operating hours (`isDeliveryOpen`), enforces the 15L cap, computes tiered delivery fees, and saves [`OrderEntity`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/data/Models.kt) to Room DB.
* `calculateFuelDeliveryFee(liters)` ([`FeeConstants.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/FeeConstants.kt)): Computes granular delivery charge.
* `isDeliveryWindowOpen()` ([`DeliveryOperatingHoursManager.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/DeliveryOperatingHoursManager.kt)): Returns boolean status based on PKT calendar time.

---

## 📁 Source Locations
* **UI Screens**: `app/src/main/java/com/example/ui/Screens.kt` (`CustomerHomeScreen`, `OrderDialog`)
* **State Management**: `app/src/main/java/com/example/ui/MainViewModel.kt`
* **Operating Hours**: `app/src/main/java/com/example/util/DeliveryOperatingHoursManager.kt`
* **Pricing Utility**: `app/src/main/java/com/example/util/FeeConstants.kt`
* **Data Model**: `app/src/main/java/com/example/data/Models.kt` (`OrderEntity`)
