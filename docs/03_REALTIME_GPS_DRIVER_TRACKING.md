# 03. Real-Time GPS Driver Tracking Documentation 🗺️

## 📌 Category
**Logistics & Navigation**

## 🎯 Purpose & Overview
The Real-Time GPS Driver Tracking system provides live visualization of the assigned bowser driver moving towards the customer's delivery destination in Lahore. It features dynamic polyline routes, arrival geofence alerts, and direct phone dialer integration.

---

## ⚙️ Key Components & Functions
* `UnifiedGoogleMapView` (`Screens.kt`): Unified, single native-styled Google Map view merging Green Town HQ, live driver location, and customer order pin on a single map interface.
* `RealTimeOrderTrackingCard` (`Screens.kt`): Main container composables displaying active order tracking state, ETA countdown, and driver details.
* `DriverRealTimeTrackingMap` (`Screens.kt`), `GoogleMapComposeView`, `DeliveryTrackerComponent`, `LahoreGoogleEmbedMapView`: Delegating wrapper composables forwarding to `UnifiedGoogleMapView`.
* `notifyArrivingSoon(orderId)` (`MainViewModel.kt`): Triggers push notification when driver enters $1\text{ km}$ geofence.
* `notifyReachedLocation(orderId)` (`MainViewModel.kt`): Triggers high-priority push notification when driver reaches customer address ($>95\%$ path progress).

---

## 🗺️ Map Visual Elements
1. **Single Merged Map Engine**: Consolidates driver, customer destination, and HQ onto one single Google Map canvas.
2. **Bowser Delivery Vehicle Marker 🚚**: Custom ride-sharing marker with live orientation heading ($0^\circ-360^\circ$), sonar radar pulse animation, and live driver speed tag (`driverLat`, `driverLng`).
3. **Customer Delivery Destination Pin ("Where to Order From") 📍**: Interactive red marker showing customer delivery pin and landmark address in Lahore.
4. **Green Town Central HQ Origin 🏢**: Fixed dispatch origin hub marker at Green Town, Lahore ($31.4380, 74.3050$).
5. **Combined Route Line 🛣️**: Polylines connecting Green Town HQ -> Driver Bowser -> Customer Destination Pin.
6. **Floating Navigation Button 🗺️**: "Navigate in Google Maps App" floating CTA launching Google Maps driving directions with fallback chooser.
7. **Call Driver Button 📞**: One-tap phone dialer (`Intent.ACTION_DIAL`) linking directly to driver's phone number.

---

## 📁 Source Locations
* **UI Component**: `app/src/main/java/com/example/ui/Screens.kt` (`DriverRealTimeTrackingMap`, `RealTimeOrderTrackingCard`)
* **State Management**: `app/src/main/java/com/example/ui/MainViewModel.kt`

---

## 🔄 Live Tracking Workflow
1. **Driver Dispatches**: Status updates to `Delivering` or `Dispatched`.
2. **Simulation / GPS Stream**: Driver coordinates update smoothly along Lahore route corridor.
3. **Arriving Soon Alert**: When path progress hits $>70\%$, app triggers `notifyArrivingSoon` system notification.
4. **Reached Location Alert**: When path progress hits $>95\%$, app triggers `notifyReachedLocation` push notification.
5. **Completion**: Driver marks order delivered; card transitions to `PostDeliveryRatingCard`.
