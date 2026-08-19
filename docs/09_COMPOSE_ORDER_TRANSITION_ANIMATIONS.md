# 09. Compose Order Status Transition Animations Documentation 🎬

## 📌 Category
**UI Motion & Visual Animation**

## 🎯 Purpose & Overview
This feature provides rich Jetpack Compose transition animations and visual overlays when an order changes status (e.g., Pending → Dispatched → Arrived → Delivered) to make the user experience fluid and delightful.

---

## ⚙️ Key Animation Composables
* `OrderStatusAnimatedTransitionHeader` (`Screens.kt`): Animated header card using `AnimatedContent` for vertical sliding, scaling, and fading between order states.
* `OrderStatusConfettiOverlay` (`Screens.kt`): Custom `Canvas` floating particle confetti overlay triggered when an order is completed.
* **Pulsing Halo Effect**: Pulsing glowing background ring animation around status icons created via `rememberInfiniteTransition`.

---

## 🎨 Animation Details & Specs
| Status State | Background Tint | Animation / Effects |
| :--- | :--- | :--- |
| **Order Placed** | Soft Yellow (`#FEF3C7`) | Pulsing yellow halo indicator |
| **Driver Assigned** | Soft Blue (`#F0F9FF`) | Slide-in vertical icon transition |
| **Out for Delivery** | Sky Blue (`#EFF6FF`) | Moving radar pulse animation |
| **Driver Reached Location** | Cyan Blue (`#E0F2FE`) | Fast pulsing location pin halo |
| **Order Delivered** | Soft Emerald (`#F0FDF4`) | **Confetti Particle Burst Celebration 🎉** |

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
