# Zyphuel Persistent Project Memory & Feature Evolution Log 🧠

## 📌 Project Identity
* **App Name**: Zyphuel
* **Description**: Pakistan's premier doorstep fuel & drinking water delivery platform operating in Lahore.
* **Target Audience**: Residential customers, commercial fleet managers, bowser drivers, and platform administrators.

---

## 🏛️ Immutable Architectural Rules (`AGENTS.md`)
* **Documentation Maintenance Rule**: Whenever creating, modifying, or updating any feature, screen, ViewModel method, or database model in Zyphuel, **MUST ALWAYS Update `FEATURES_DOCUMENTATION.md`** and relevant modular docs in `/docs`.
* **UI Stack**: Kotlin with Jetpack Compose (Material 3).
* **State Management**: Centralized `MainViewModel` with Kotlin `StateFlow`.
* **Testing Requirement**: Always assign Compose `testTag` for key interactive UI components (`testTag("login_button")`, `testTag("rating_star_1")`, etc.).
* **Room DB Schema**: Maintain `UserEntity`, `OrderEntity`, `AuditLogEntity`, and `NotificationEntity` table structures.

---

## 📜 Feature Implementation History & Log

### Phase 1: Core Platform Foundation & Multi-Role Commerce
* Implemented multi-role authentication system (`customer`, `rider`, `admin`) with SHA-256 password hashing.
* Built doorstep order placement engine for Super Petrol (Rs. 272.50/L), High-Speed Diesel (Rs. 283.00/L), Pure Drinking Water (Rs. 50.00/Gallon), and LPG cylinders.

---

### Phase 6: OAuth Continue with Google Role Isolation & Pure Water Pricing Update
* Updated Pure Drinking Water price to Rs. 50.00 / Gallon.
* Fixed Cash-on-Delivery (COD) order confirmation workflow logic to allow smooth order placement without infinite loops or unnecessary blocks.
* Refactored "Continue with Google" sign-in via `GoogleAuthManager` and `androidx.credentials` with role isolation (`targetRole`: `google.customer@zyphuel.com` vs `google.rider@zyphuel.com`), ensuring user account protection without admin privilege escalation.
* Updated `FEATURES_DOCUMENTATION.md`, `MEMORY.md`, `sitemap.xml`, and modular `/docs` files in accordance with project maintenance rules.
* Implemented peak-hour surge pricing calculator and WhatsApp advance payment routing for bulk orders ($\ge 30\text{L}$).

### Phase 2: Real-Time GPS Tracking & Fleet Logistics
* Built live driver tracking map (`DriverRealTimeTrackingMap`) with route polyline, vehicle marker, and geofence visualizers.
* Implemented automated geofence push notification triggers for $1\text{ km}$ boundary (`notifyArrivingSoon`) and arrival destination (`notifyReachedLocation`).

### Phase 3: Security, Biometrics & Notifications
* Integrated AndroidX `BiometricPrompt` API (`BiometricSecurityManager`) for fingerprint and face unlock quick login and order history vault protection.
* Implemented security input validator (`SecurityInputValidator`), brute-force rate limiter (`SecurityRateLimiter`), and AES-256 encrypted storage (`SecureStorageManager`).
* Built multi-channel notification engine supporting Android system push notifications (`zyphuel_order_updates` channel), Firebase Cloud Messaging (`ZyphuelFcmService`), and automated email receipts.

### Phase 4: Post-Delivery Quality & Motion UI Polish
* Added post-delivery rating UI (`PostDeliveryRatingCard`) featuring an interactive 1-5 star selector with spring scaling animations, quick compliment chips (`FlowRow` + `FilterChip`), and optional driver feedback notes.
* Integrated Jetpack Compose transition animations (`OrderStatusAnimatedTransitionHeader`) with vertical sliding, pulsing halo indicator, and custom Canvas floating confetti celebration overlay (`OrderStatusConfettiOverlay`).

### Phase 5: Driver Details & Login Biometric Integration
* Refactored `AddRiderDialog` in Admin Dashboard to include complete driver form fields: Full Name, Phone, Email, Portal Password, Vehicle Type (Bike, Pickup, Bowser), Registration Plate No, CNIC, Driving License ID, Residential Address, and Immediate Verification Checkbox.
* Added `AdminRiderCard` high-contrast summary grid displaying key driver information (Phone, Vehicle, Plate No, CNIC) distinctly on the card.
* Refactored `AdminRiderBiodataDialog` into 4 distinct structured sections (Personal Biodata, Legal Credentials, Vehicle & License, Emergency Contact) displaying strictly provided driver form details without unprovided filler placeholders.
* Added always-visible Biometric Hardware Lock card directly on the main Login Form (`Screens.kt`) for both Customer and Rider logins with instant fallback authentication via `loginWithBiometrics` in `MainViewModel`.
* Updated `FEATURES_DOCUMENTATION.md`, `ARCHITECTURE.md`, `MEMORY.md`, and `/docs` feature files in compliance with project maintenance rules.

### Phase 7: Live GPS Telematics, Clean Order Journey, Play Store Compliance & Release App Bundle
* **Clean Order Dialog**: Completely streamlined `OrderDialog` to focus exclusively on product quantities, destination address, discount vouchers, and pricing breakdown, eliminating duplicate embedded static maps and preview triggers under Grand Total.
* **Unified Live Google Maps in TrackerScreen**: Embedded genuine `GoogleMapsLiveDeliveryTrackingOverlay` into `TrackerScreen`, replacing static iframe tabs and mock driver strings (`Mohammad Ali`, `LEC-8924`, `2.4 km`) with dynamic real-time database assignments and Haversine distance calculations.
* **Daily 1-Time Order Safety GPS Disclaimer (`DailyGpsSafetyDisclaimerDialog`)**: Implemented daily 1-time safety disclosure banner: *"The driver/rider GPS is on during each ride. It helps us follow the order in real time and make your order safely delivered."* Persisted locally using `zyphuel_gps_safety_prefs` timestamp checking.
* **Google Play Policy Compliant Account Deletion (`deleteCurrentAccount`)**: Added prominent "Delete Account / Erase Data" options in both the Customer Sidebar Drawer and Profile Settings Dialog with double confirmation modal (`DeleteAccountConfirmationDialog`), permanently purging user records, marked locations, and session tokens.
* **Release Signing & App Bundle Generation**: Configured `signingConfigs.release` with automatic fallback to debug keystore for development environments, successfully generating production-ready Android App Bundle (`app-release.aab`) with `bundleRelease`.


---

## 🗄️ Database Entity Schema Reference
* `UserEntity`: `email` (PK), `name`, `passwordHash`, `role`, `phoneNumber`, `residentialAddress`, `isVerified`.
* `OrderEntity`: `id` (PK), `customerEmail`, `customerName`, `serviceType`, `fuelVolumeLiters`, `totalAmountPkr`, `status`, `assignedRiderEmail`, `assignedRiderName`, `deliveryAddress`, `etaMinutes`, `rating`, `feedback`.
* `AuditLogEntity`: `id` (PK), `timestamp`, `action`, `performedBy`, `details`.
* `NotificationEntity`: `id` (PK), `timestamp`, `title`, `message`, `targetRole`, `isRead`.
