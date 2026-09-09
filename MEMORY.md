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
### Phase 8: Real-Time Multi-Channel Email Gateway & Admin Full Lifecycle Management
* **Real-Time Triple-Party Gmail Dispatch Gateway (`RealtimeEmailEngine.kt`)**: Production-grade RFC 5321/3207 SMTP engine over TLS/SSL Port 465, Port 587 STARTTLS, and Port 443 Google Apps Script Webhook relay. Dispatches transactional emails directly to Gmail inboxes with 0ms UI blocking.
* **Cross-Device Cloud Firestore Gateway Sync (`system_config/email_gateway`)**: Syncs active SMTP credentials and Webhook URL across all Customer, Rider, and Admin devices via Firestore snapshot listener, ensuring cross-device order dispatch.
* **Admin Order Controls & Self-Delivery Fallback (`adminAcceptOrder`)**: In Admin Dashboard, if no active verified rider is online, automatically assigns the order to the Administrator for direct fulfillment (`m.daniyalkhan490@gmail.com` / `Admin Direct Delivery`).
* **Admin Master Lifecycle Controls (`changeOrderStatus`)**: Equipped `AdminOrderCard` with real-time status buttons (`Start Delivery 🚚` for Assigned orders, `Mark Delivered ✅` for Delivering orders). Admins possess master supervisory authority in `Repository.updateOrderStatus` to transition orders to completion.
### Phase 9: Practical Step-by-Step App Tour, Downloadable PDF Invoices & Delivery Fee Standardization
* **Revamped Practical Step-by-Step App Tour Guide (`AppTourGuideDialog`)**: Replaced generic text with actionable, real-life app usage instructions across 8 stages (Fuel/Water Selection, Liters Stepper & Address, Transparent OGRA Rates & 1-Tap COD, 4-Stage Live Tracker, Direct Driver Call & Live Chat, PDF Invoice Downloads, Sidebar Drawer & History, Biometric Login). Built with fluid `AnimatedContent` slide/fade transitions and animated progress bar (`FastOutSlowInEasing`).
* **Official Order Tax Invoice Generation & PDF Download (`InvoiceGenerator.kt`, `InvoiceDialog`)**: Built official itemized invoice engine for Customer and Rider clarity. Integrates native Android `PrintManager` allowing 1-tap "Save as PDF" to device storage or printing without requiring sensitive storage permissions. Added social text sharing via WhatsApp and Email. Accessible from `TrackerScreen`, `CustomerOrderCard`, and `AdminOrderCard`.
* **Standardized Delivery Fee Calculation (`FeeConstants.kt`)**: Fixed delivery charge for Fuel (**Petrol, Diesel, High-Octane**) and LPG is permanently set to **Rs. 250.00** (Water: Rs. 50.00, 50% multi-item bundle discount) across `OrderDialog`, `OrderSummaryCard`, `TrackerScreen`, `showFareBreakdownDialog`, and official invoices.
* **Streamlined Address Input in `OrderDialog`**: Removed redundant "Saved Addresses (Quick Fill)" and "Save Current" chips to deliver a clean, clutter-free checkout experience.
* **Removed "Stay Updated on Your Fuel Delivery" Bell Icon & Dialog**: Removed the bell icon button from the Order Details header (`TrackerScreen`), removed the automatic prompt trigger from `MainActivity.kt`, and removed the notification banner card from `CustomerHomeScreen`.
* **Customer Order History Isolation & Admin Visibility**:
  - Regular customer: strictly views only their own orders (`getOrdersForCustomerFlow` with case-insensitive `LOWER(customerEmail) = LOWER(:email)`).
  - Administrator: `customerOrders` automatically streams `getAllOrdersFlow()`, viewing all users' orders and Admin's own orders on `CustomerOrderHistoryScreen` and Admin Dashboard with `[Admin Order]` badges and customer details.
* **Semantic Micro-Versioning**: Adopted versioning scheme `v2.3.0.1` (`versionCode = 4`) for minor patch updates, with `v2.3.x.x` reserved for major feature sets.

### Phase 10: Google Play Store Publication Compliance, Interactive Legal Viewers & Automated Version Lifecycle
* **Automated Version Lifecycle & Bump Rule**: Enforced strict policy in `AGENTS.md` to advance `versionCode` (incremented to `5`) and `versionName` (`2.3.1`) in `app/build.gradle.kts` on every code iteration.
* **Interactive In-App Legal Viewer (`TermsAndPrivacyDialog.kt`)**: Built dual-tab Jetpack Compose dialog presenting complete **Terms & Conditions** and **Privacy Policy**, with dynamic version badge (`v2.3.1 Build 5`), section headers, bullet points, and web browser redirection button.
* **Customer Registration Compliance**: Integrated mandatory "I agree to Terms & Conditions & Privacy Policy" checkbox with interactive clickable legal links on `AuthScreen`.
* **Rider Registration & Profile Compliance**: Connected Terms checkboxes on `AuthScreen` and `RiderCompleteProfileScreen` to open the full legal modal on tap.
* **Sidebar Drawer & Profile Integration**: Added "Terms & Privacy Policy" option and live version badge `Zyphuel v2.3.1 • Build 5` to `CustomerSidebarDrawer`.
* **Security Settings Legal Card**: Added a dedicated "Legal & Play Store Compliance" card in `SecuritySettingsScreen` with verified safety status badge (`Verified 🛡️`) and direct viewing buttons.
* **Target SDK 36 Permission Audit**: Added `android:maxSdkVersion="22"` to `GET_ACCOUNTS` in `AndroidManifest.xml` to prevent Play Console policy warnings on modern Android (API 23–36).
* **Full Production Legal Documents & Web Copy**:
  - `PRIVACY_POLICY.md` (root)
  - `TERMS_AND_CONDITIONS.md` (root)
  - `PLAY_STORE_DATA_SAFETY_AND_COMPLIANCE.md` (root)
  - `web/privacy-policy.html` (web-ready)
  - `web/terms-and-conditions.html` (web-ready)

### Phase 11: 10-Category Marketplace Architecture, Saved Vehicle Profiles & Typo-Tolerant Search (v2.4.1 Build 8)
* **10 Top-Level Categories & 35+ Subcategories**: Launched comprehensive automotive and mobility marketplace (`CategoryCatalogSeed`, `CategoryModels.kt`) with live OGRA fuel pricing, emergency roadside SOS, auto repair, mobile detailing, tyre repair, battery jumpstart/installation, lubricants, EV services, water delivery, and fleet management.
* **Customer Saved Vehicle Profiles ("My Vehicles")**: Added Room entity `VehicleEntity` and `VehicleDao` (Database version 12) with multi-vehicle registration, fuel types, and dynamic service compatibility filtering.
* **Typo-Tolerant Global Search Engine**: Integrated Levenshtein edit-distance matching into `CategoryRepository.kt` with real-time `serviceSearchQuery` and `serviceSearchResults` StateFlows.
* **Location Coverage & Dynamic ETA Engine**: Implemented `LocationCoverageManager.kt` using real Haversine distance and zone mapping across Greater Lahore without mock/fake values.
* **Admin Category Control Tab**: Added Tab 8 in `AdminDashboardScreen` with instant live category availability toggles and pricing overrides.
* **Version Advancement**: Updated `versionCode = 8` and `versionName = "2.4.1"` in `app/build.gradle.kts`.

### Phase 12: Comprehensive 13-Step Interactive App Tour Overhaul & Modern Compose UI Polish (v2.4.2 Build 9)
* **Comprehensive 13-Step Tour Architecture (`SpotlightOverlay` & `homeTourSteps`)**: Upgraded the on-screen guided tour from an 8/11-step basic walkthrough to a comprehensive 13-stage tour covering 100% of app features (Welcome Overview, Live GPS Pinpoint & Sharing, Universal Typo-Tolerant Search, Emergency SOS Quick Actions, Saved Vehicles Profile, 10-Category Marketplace, 1-Tap COD Ordering, Live 4-Stage Delivery Stepper, Automated Gmail Receipts & PDF Invoices, Notification Alerts, Delivery Dashboard, Sidebar Menu, and Biometric Security).
* **Tesla & Apple Caliber Compose Polish (`SpotlightTour.kt`)**: Added top linear animated progress bar, category badge pills (`Surface`), actionable "💡 Pro Tip:" cards, smooth horizontal `AnimatedContent` slide & fade transitions between steps, and interactive clickable progress indicator dots (`state.jumpTo(targetIndex)`).
* **Help Center & Sidebar Integration**: Re-launchable anytime from both the Navigation Drawer (`Take a Guided Tour 🧭`) and the Help Center / Support Dialog (`Take Guided App Tour (13 Steps) 🧭`).
* **Test Suite Verification**: Updated `ZyphuelComprehensiveAppTest.kt` with 13-step integrity verification.
* **Version Advancement**: Advanced `versionCode = 9` and `versionName = "2.4.2"` in `app/build.gradle.kts`.

### Phase 14: Customer Home Screen & Visual Card Overhaul (v2.4.4 Build 11)
* **Delivery Location Card Overhaul (`user_location_active_card`)**: Modernized with a sleek deep ocean gradient (`#0F172A` -> `#0369A1`), pulsing live GPS telematics indicator, single top-right glassmorphic Share Location badge (`testTag("share_location_badge")` and `testTag("share_location_btn")`), vibrant primary `[ Auto Detect GPS ]` button (`#0284C7`), and clean `[ Change Pin ]` button. Removed redundant duplicate share button and low-contrast outlines.
* **Pill-Shaped Global Service Search Bar (`ServiceSearchBar`)**: Refactored to a modern `RoundedCornerShape(24.dp)` pill with subtle shadow elevation, circular search icon container, and 1-tap query clear button.
* **Quick Actions Bar Polish (`QuickActionsBar`)**: Upgraded to 24.dp pill shape with circular icon badges in brand blue, enhanced contrast, and zero label clipping for roadside assistance and emergency services.
* **Saved Vehicles Profile Card (`SavedVehiclesBar`)**: Refactored to a 16.dp rounded card with crisp subtle border and elevated vehicle management trigger.
* **De-Cluttered Marketplace Category Cards (`CategoryCard`)**: Removed repetitive `"Available Now"` green badge noise from all cards, reserving badges strictly for emergency/SOS services. Expanded card title layout to `minLines = 2, maxLines = 2` with 18.sp line height to completely eliminate text truncation (e.g., "Roadside Assistance" and "Auto Care & Detailing" now render with zero clipping).
* **Delivery Dashboard Clean Text**: Replaced developer-style button text `"View Full Customer Order History Screen 📜"` with clean `"View Full Order History →"`.
* **Elevated Past Order Cards (`CustomerPastOrderCard`)**: Upgraded to 16.dp rounded cards with soft pastel status pills (`#DCFCE7` / `#16A34A` for Completed/Delivered, `#FEE2E2` / `#DC2626` for Cancelled, `#FEF3C7` / `#D97706` for In-Progress) and tinted service icon containers.
* **Version Advancement**: Incremented `versionCode = 11` and `versionName = "2.4.4"` in `app/build.gradle.kts`.

### Phase 15: Pure Mobile Architecture & Desktop Application Decommissioning (v2.4.6 Build 13)
* **Complete Desktop Decommissioning**: Permanently deleted all desktop applications, launchers, and modules (`desktop/`, `RUN-DESKTOP.bat`, `RUN-DESKTOP-DEBUG.bat`, `RUN-MOBILE-PREVIEW.bat`, `mobile-preview/`) per user directive to keep the system pure, lightweight, and exclusively focused on the Android mobile application.
* **Streamlined Mobile Repository**: Removed extraneous build targets and desktop Gradle modules, ensuring 100% of project resources, testing, and CI/CD maintenance target the core Android mobile app.
* **Version Advancement**: Incremented `versionCode = 13` and `versionName = "2.4.6"` in `app/build.gradle.kts`.

---

## 🗄️ Database Entity Schema Reference
* `UserEntity`: `email` (PK), `name`, `passwordHash`, `role`, `phoneNumber`, `residentialAddress`, `isVerified`.
* `OrderEntity`: `id` (PK), `customerEmail`, `customerName`, `serviceType`, `fuelVolumeLiters`, `totalAmountPkr`, `status`, `assignedRiderEmail`, `assignedRiderName`, `deliveryAddress`, `etaMinutes`, `rating`, `feedback`.
* `AuditLogEntity`: `id` (PK), `timestamp`, `action`, `performedBy`, `details`.
* `NotificationEntity`: `id` (PK), `timestamp`, `title`, `message`, `targetRole`, `isRead`.
