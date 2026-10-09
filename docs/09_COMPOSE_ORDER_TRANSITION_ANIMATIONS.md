# 09. Compose Order Status Transition Animations Documentation 🎬

> **Current App Version:** `2.6.4.0.0.26 (Build 54)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

## 📌 Category
**UI Motion & Visual Animation**

## 🎯 Purpose & Overview
This feature provides rich Jetpack Compose transition animations and visual overlays when an order changes status (e.g., Pending → Dispatched → Arrived → Delivered) to make the user experience fluid and delightful.

---

## ⚙️ Key Animation Composables
* <font color="#10b981"><b>`OrderStatusAnimatedTransitionHeader`</b></font> (`Screens.kt`): Animated header card using `AnimatedContent` for vertical sliding, scaling, and fading between order states.
* <font color="#10b981"><b>`OrderStatusConfettiOverlay`</b></font> (`Screens.kt`): Custom `Canvas` floating particle confetti overlay triggered when an order is completed.
* **Pulsing Halo Effect**: Pulsing glowing background ring animation around status icons created via `rememberInfiniteTransition`.

---

## 🎨 Animation Details & Specs
| Status State | Background Tint | Animation / Effects |
| :--- | :--- | :--- |
| **Order Placed** | Soft Yellow (`#FEF3C7`) | Pulsing yellow halo indicator |
| **Driver Assigned** | Soft Blue (`#F0F9FF`) | Slide-in vertical icon transition |
| **Out for Delivery** | Sky Blue (`#EFF6FF`) | Moving radar pulse animation |
| **Driver Reached Location** | Cyan Blue (`#E0F2FE`) | Fast pulsing location pin halo |
| <font color="#10b981"><b>Order Delivered</b></font> | <font color="#10b981">Soft Emerald (`#F0FDF4`)</font> | <font color="#10b981"><b>Confetti Particle Burst Celebration 🎉</b></font> |

---

## 📁 Source Locations
* **UI Components**: `app/src/main/java/com/example/ui/Screens.kt` (`OrderStatusAnimatedTransitionHeader`, `OrderStatusConfettiOverlay`)
* **Live Integration**: Embedded inside `RealTimeOrderTrackingCard` in `Screens.kt`.

---

## 🔄 Animation Workflow
1. **Status Update**: Order status changes in `MainViewModel`.
2. **AnimatedContent**: Smoothly slides out old status header and slides in new status header with spring physics.
3. **Pulsing Halo**: Continuous infinite pulse keeps card visually dynamic.
4. **Confetti Celebration**: On completion (`Completed` / `Delivered`), `OrderStatusConfettiOverlay` draws floating colored particles over the header.
