# 02. Customer Order Placing & Surge Pricing Documentation ⛽

## 📌 Category
**Customer Commerce & Order Management**

## 🎯 Purpose & Overview
The Order Placement engine allows customers to order doorstep fuel (Super Petrol, High-Speed Diesel), pure drinking water, and LPG cylinders in Lahore. It features dynamic volume calculation, surge pricing rules during peak hours, and automatic high-volume payment routing.

---

## ⚙️ Key Functions & ViewModel Methods
* `placeOrder(serviceType, volumeLiters, deliveryAddress, customerEmail, customerName)` (`MainViewModel.kt`): Validates order input, applies rate limits, calculates base price + delivery fee + surge tax, and saves `OrderEntity` into local database.
* `calculateSurgePrice(basePrice, peakHourMultiplier)` (`MainViewModel.kt`): Applies surge multipliers based on peak delivery hours and high demand zones in Lahore.
* `checkHighVolumeOrder(volumeLiters)` (`Screens.kt`): Detects orders $\ge 30\text{L}$ and displays a WhatsApp advance payment coordination prompt to prevent fraud.

---

## ⛽ Available Fuel & Service Types
* **Super Petrol (92 Octane)**: Rs. 272.50 / Liter (Default doorstep fuel delivery)
* **High-Speed Diesel (HSD)**: Rs. 283.00 / Liter (Commercial & heavy vehicle fuel)
* **Pure Mineral Drinking Water**: Rs. 50.00 / Gallon (Pure Purified Drinking Water)
* **LPG Gas Cylinders**: Standard household and commercial gas refill

---

## 📁 Source Locations
* **UI Screen**: `app/src/main/java/com/example/ui/Screens.kt` (`CustomerHomeScreen`, `OrderPlacementScreen`)
* **State Management**: `app/src/main/java/com/example/ui/MainViewModel.kt`
* **Data Model**: `app/src/main/java/com/example/data/Models.kt` (`OrderEntity`)

---

## 🔄 User Flow
1. **Service Selection**: Customer taps desired service card (Super Petrol / Diesel / Water).
2. **Volume Slider / Counter**: Customer specifies volume (e.g., 10L, 20L, 50L).
3. **Surge & Total Calculation**: Real-time summary displays fuel price, delivery fee (Rs. 150), and total PKR.
4. **Address & Note Selection**: Selects saved home/work address or pinpoints on map.
5. **High Volume Routing**: If volume $\ge 30\text{L}$, guides user to WhatsApp advance payment confirmation (`+92 323 0112464`).
6. **Order Submission**: Order is logged to Room DB with status `Pending`, triggering notification to Admin and Riders.
