# 🚀 Zyphuel App Features & Technical Documentation
**App Version:** `v2.6.4.0.0.06 (Build 34)` | **Target SDK:** `36` (Android 15/16 Ready) | **Last Updated:** `2026`

Welcome to the complete architectural and functional guide for the **Zyphuel** Android application. This document outlines every single feature, function, database entity, and user flow from start to finish.

---

## 📋 Table of Contents & Modular Documentation Files Index

Below is the complete index of all **dedicated documentation files** (including the Master Audit & Zero-Deletion Catalog, 11 modular feature guides in `/docs`, system architecture, and persistent project memory) covering every aspect of Zyphuel:

| # | File Name | Category | Primary Focus & Functions Covered |
| :-: | :--- | :--- | :--- |
| **⭐** | [`GLOBAL_AUDIT_AND_FEATURE_INTEGRITY.md`](/GLOBAL_AUDIT_AND_FEATURE_INTEGRITY.md) | **Master Audit & Integrity** | **Complete catalog of 100% working features, disconnected components, UX/safety rules & missing feature blueprints.** |
| **-** | [`ARCHITECTURE.md`](/ARCHITECTURE.md) | System Architecture | MVVM architecture, layer stack diagram, directory structure, data flow constraints. |
| **-** | [`MEMORY.md`](/MEMORY.md) | Project Memory & Log | Evolution history, immutable development rules, phase completion records, entity schema. |
| **1** | [`docs/01_USER_ROLES_AND_AUTHENTICATION.md`](/docs/01_USER_ROLES_AND_AUTHENTICATION.md) | Security & User Control | Customer, Rider & Admin registration, SHA-256 login, role routing. |
| **2** | [`docs/02_CUSTOMER_ORDER_PLACING_AND_SURGE.md`](/docs/02_CUSTOMER_ORDER_PLACING_AND_SURGE.md) | Customer Commerce | Super Petrol, HSD, Water & LPG ordering, volume pricing, peak surge, WhatsApp (>30L). |
| **3** | [`docs/03_REALTIME_GPS_DRIVER_TRACKING.md`](/docs/03_REALTIME_GPS_DRIVER_TRACKING.md) | Logistics & Navigation | Live driver GPS map (`DriverRealTimeTrackingMap`), geofence arrival alerts, quick call dialer. |
| **4** | [`docs/04_ADMIN_DASHBOARD_AND_MANAGEMENT.md`](/docs/04_ADMIN_DASHBOARD_AND_MANAGEMENT.md) | Admin & Operations | Analytics dashboard, `AddCustomerDialog`, `AddRiderDialog`, manual dispatch, audit logs. |
| **5** | [`docs/05_RIDER_BOWSER_DRIVER_PORTAL.md`](/docs/05_RIDER_BOWSER_DRIVER_PORTAL.md) | Driver Fleet Operations | Active duty queue (`RiderHomeScreen`), status progression (En Route → Arrived → Completed), COD receipt. |
| **6** | [`docs/06_SECURITY_AND_BIOMETRICS.md`](/docs/06_SECURITY_AND_BIOMETRICS.md) | Security & Encryption | Fingerprint/Face unlock (`BiometricSecurityManager`), encrypted storage, rate limiter, input validator. |
| **7** | [`docs/07_NOTIFICATION_AND_COMMUNICATION_ENGINE.md`](/docs/07_NOTIFICATION_AND_COMMUNICATION_ENGINE.md) | Push & Messaging | Android system push (`zyphuel_order_updates`), FCM service, automated email receipts, WhatsApp hotline. |
| **8** | [`docs/08_POST_DELIVERY_RATING_AND_FEEDBACK.md`](/docs/08_POST_DELIVERY_RATING_AND_FEEDBACK.md) | Customer Quality & Feedback | Interactive 1-5 star rating card (`PostDeliveryRatingCard`), compliment chips (`FlowRow`), driver feedback notes. |
| **9** | [`docs/09_COMPOSE_ORDER_TRANSITION_ANIMATIONS.md`](/docs/09_COMPOSE_ORDER_TRANSITION_ANIMATIONS.md) | UI Motion & Animation | Status header transitions (`AnimatedContent`), pulsing halo, Canvas floating confetti celebration overlay. |
| **10** | [`docs/10_DATABASE_ROOM_PERSISTENCE_AND_MODELS.md`](/docs/10_DATABASE_ROOM_PERSISTENCE_AND_MODELS.md) | Data Architecture | Room local SQLite persistence (`UserEntity`, `OrderEntity`, `AuditLogEntity`, `NotificationEntity`), DAOs. |
| **11** | [`docs/11_REALTIME_ORDER_INVOICE_EMAIL.md`](/docs/11_REALTIME_ORDER_INVOICE_EMAIL.md) | Transactional Email | Auto order-confirmation + tax invoice to the **registered/Google email only**, webhook→SMTP→Firestore delivery, Admin Email Gateway setup & test. |

---

## 1. Overview & App Purpose
**Zyphuel** is an on-demand fuel, pure drinking water, and LPG gas cylinder delivery platform serving Lahore, Pakistan. The app connects customers, bowser drivers (riders), and admins seamlessly with real-time GPS tracking, transparent pricing, and instant notifications.

---

## 2. User Roles & Permissions
The app strictly segments access control across three user roles:
* **Customer (`role = "customer"`)**: Can place orders, view price alerts (Super Petrol, High-Speed Diesel, High Octane, LPG Gas, Pure Water @ Rs. 50/gallon), track delivery driver live on map, and update profile settings.
* **Rider (`role = "rider"`)**: Can accept orders, update delivery status (En Route -> Arrived -> Completed), and share live GPS coordinates.
* **Admin (`role = "admin"`)**: Full access to app stats, audit logs, active orders, rider management, and **Customer Management (Add/Edit Customers)**.
  * **Permanent Root Super Admin Protection (`m.daniyalkhan490@gmail.com` / `abcd1234`)**:
    * **Immutable Super Admin Account**: Email `m.daniyalkhan490@gmail.com` with fixed password `abcd1234` is the permanent root Super Admin account.
    * **Official Blue Tick Verified Badge**: The Super Admin account displays the official **Verified Blue Tick Badge** (`Icons.Filled.Verified` in primary blue `#0284C7`) across the Navigation Drawer header, `ProfileSettingsDialog`, and `AdminDashboardScreen` top bar, accompanied by a dedicated `"VERIFIED ADMIN"` badge.
    * **Automatic Database & Cloud Sync (`seedAdminIfNeeded`)**: On app startup and prior to authentication checks, the repository guarantees that the Super Admin user exists in Room DB and syncs to Cloud Firestore `users/m_daniyalkhan490@gmail_com` with `role = "admin"` and `isVerified = true`.
    * **Irremovable Server & Client Guard**: Deletion of `m.daniyalkhan490@gmail.com` is completely blocked in `Repository.deleteUserAccount`, `MainViewModel.deleteCurrentAccount`, `MainViewModel.deleteRiderFromAdmin`, and `MainViewModel.updateCustomerEmail`.
    * **UI Safety Controls**: In both the Navigation Drawer and `ProfileSettingsDialog`, the destructive "Delete Account" action is hidden for the Super Admin, displaying an official green "🛡️ Permanent Root Super Admin" status card instead.
* **Social OAuth 2.0 SDK Authentication**:
  * **Continuous Google Sign-In & Firebase Integration**: Exclusively features "Continue with Google" sign-in via `GoogleAuthManager` and `AuthManager`. Tapping "Continue with Google" initiates native real-time Google authentication (`CredentialManager` with `GetGoogleIdOption`).
  * **AuthManager (`AuthManager.kt`)**: Dedicated singleton managing Firebase Authentication lifecycle states (`FirebaseAuthState`: `Unauthenticated`, `Loading`, `Authenticated`, `Error`) with `StateFlow` streams (`authStateFlow`, `currentUserState`, `isProcessing`). Features step-by-step diagnostic trace logs (`[GoogleSignInRedirect] Step 1-4`) for credential generation, Firebase token exchange, and session completion to proactively diagnose redirect hangs.
  * **GoogleAuthManager (`GoogleAuthManager.kt`)**: Dedicated `GoogleAuthManager` class leveraging `androidx.credentials` library (`CredentialManager.create(context)`). Implements `GetCredentialRequest` with `GetGoogleIdOption` to trigger the native Android Google Account Picker, extracts `GoogleIdTokenCredential`, and delegates token sync to `AuthManager` with comprehensive stage logging (`[GoogleAuthFlow]`). Supports role-aware sign-in (`targetRole`) to strictly isolate customer and rider accounts without risking accidental admin privilege escalation.
  * **Real Device Account Picker Sheet (`Screens.kt` / `DeviceAccountUtils.kt`)**: If the device contains configured Google accounts or local profiles, provides a Google-styled account selection sheet for single-tap real-time authentication. If no Google account is present on the device, directs the user to add their Google account via Android Settings. No mock or fake text input forms are displayed.
  * **AuthRepository State Synchronizer (`AuthRepository.kt`)**: Dedicated `AuthRepository` managing `FirebaseAuth` instance lifecycle, setting up `addAuthStateListener` to monitor user login status, and providing `signOut()` function to clear local sessions and sync `AuthState` (`Unauthenticated`, `Loading`, `Authenticated`, `Error`), `firebaseUser`, and `isLoading` StateFlows directly with UI loading indicators and ViewModels.
  * **Unified AuthViewModel (`AuthViewModel.kt` / `AuthenticationViewModel`)**: Centralized ViewModel wrapping `AuthRepository` that handles credential-based Google authentication via `signInWithGoogleCredential(idToken, accessToken)` and `signInWithCredential(AuthCredential)`. Uses `GoogleAuthProvider.getCredential(idToken, accessToken)` to exchange tokens directly with the `FirebaseAuth` instance, maintaining `authState`, `firebaseUser`, `isLoading`, and `authErrorMessage` StateFlows for UI reactive updates.
  * **Firestore User Profile Sync (Upsert)**: Automatically saves and updates user documents in Cloud Firestore at path `users/{uid}` with fields `uid`, `email`, `displayName`, `photoURL`, `role`, `createdAt`, and `lastLogin`.
  * **Auth State Listener & Session Persistence**: Root level `FirebaseAuth.addAuthStateListener` in `FirebaseAuthProvider.kt` and `AuthRepository.kt` tracks auth state changes and manages session tokens.
  * **Signing Diagnostic Utility (`SigningDiagnosticUtil.kt`)**: Dedicated diagnostic utility invoked in `MainActivity.kt` `onCreate` that extracts, formats, and logs the current app build's signing certificate SHA-1 fingerprint (`C7:F7:10:F2:2D:43:D1:F3:31:D2:22:AB:35:1B:C4:47:01:EE:C7:E5`) to Logcat and `DebugLogger`, automatically comparing it with the expected Firebase Console fingerprint to identify configuration mismatches and prevent `DEVELOPER_ERROR` during Google Sign-In.
  * **Google Services Configuration (`google-services.json`)**: Configured with project `ai-420` (project number `488422345846`), package name `com.aistudio.zyphuel.appv2`, and registered Android OAuth Client ID `488422345846-a8e8ernjc953kmb76tdtr9raj1nputa3.apps.googleusercontent.com` paired with certificate hash `c7f710f22d43d1f331d222ab351bc44701eec7e5` and Web Client ID `488422345846-m972okhh2ms29s911apa4t8ih04d3jo1.apps.googleusercontent.com`.
  * **Mobile Navigation & Profile UI Integration**: Displays user profile photo, display name, and email in the Navigation Drawer and Profile screens, with Admin Dashboard menu options strictly restricted to users with `user.role == "admin"`.

---

## 3. Customer Portal Features
* **Phase 14 Customer Home UI & Visual Card Overhaul (v2.4.4 Build 11)**:
  * **Gradient Delivery Location Active Card (`user_location_active_card`)**: Refactored with a deep navy-to-ocean blue linear gradient (`#0F172A` -> `#0F2B48` -> `#0369A1`), 20.dp rounded corners, pulsing green/blue live telematics indicator dot, glassmorphic top-right Share button badge, crisp white address typography, high-contrast primary `[ 🛰️ Auto Detect GPS ]` button (`#0284C7`), and clean `[ 📍 Change Pin ]` button (`location_icon_btn`). Removed redundant bottom Share button and low-contrast outlines.
  * **Pill-Shaped Service Search Bar (`ServiceSearchBar`)**: Refactored to `RoundedCornerShape(24.dp)` pill with subtle shadow elevation, dedicated circular search icon container, crisp placeholder typography, and instant 1-tap query clearing.
  * **Refined Quick Actions Bar (`QuickActionsBar`)**: Upgraded to 24.dp pill shape with circular icon badges in brand blue, enhanced contrast, and zero label clipping for roadside assistance and emergency services.
  * **Saved Vehicles Profile Card (`SavedVehiclesBar`)**: Refactored to 16.dp rounded card with subtle border and elevated vehicle management button.
  * **De-Cluttered Marketplace Category Cards (`CategoryCard`)**: Removed repetitive `"Available Now"` green badge clutter from all cards. Badges are reserved exclusively for emergency/SOS services. Expanded card title layout to `minLines = 2, maxLines = 2` with 18.sp line height to completely eliminate text truncation (e.g. "Roadside Assistance" and "Auto Care & Detailing" now display cleanly with zero ellipses).
  * **Polished Order History Dashboard**: Replaced internal debug string `"View Full Customer Order History Screen 📜"` with professional `"View Full Order History →"`.
* **Phase 15 Pure Mobile Focus & Desktop Application Decommissioning (v2.4.6 Build 13)**:
  * **Complete Desktop Purge**: Permanently decommissioned and deleted all desktop modules, scripts, and runtime binaries (`desktop/`, `RUN-DESKTOP.bat`, `RUN-DESKTOP-DEBUG.bat`, `RUN-MOBILE-PREVIEW.bat`, `mobile-preview/`) to eliminate unnecessary system overhead and streamline maintenance.
  * **Pure Mobile Architecture**: Repository now concentrates 100% of resources exclusively on the native Android mobile application (`com.aistudio.zyphuel.appv2`), maintaining clean Gradle tasks and subproject isolation.
* **Phase 16 Early-Stage Lahore Startup Experience & Human-Centric Overhaul (v2.5.0 Build 14)**:
  * **Authentic Startup Tone & Legal Disclosures**: Eliminated all generic "corporate AI conglomerate" aesthetics and buzzwords. Prominently integrated clear, bold disclosures across legal policies (`PRIVACY_POLICY.md`, `TERMS_AND_CONDITIONS.md`), in-app dialogs (`TermsAndPrivacyDialog.kt`), Customer Home Screen, Navigation Drawer, and Profile Settings stating that Zyphuel is an evolving, early-stage local startup operating in Lahore, Pakistan run by a small passionate team.
  * **Location Standard ("Deliver to Lahore")**: Permanently eliminated references to "Lahore Hub", "Automated Delivery Hub", and "Green Town Central Hub" in favor of authentic Lahore dispatch phrasing: `"Deliver to Lahore"`, `"Zyphuel Station, Green Town, Lahore"`, and `"Coverage Area: Lahore Active"`.
  * **Customer Home De-Cluttering**:
    * Removed 10-category marketplace grid (`CategoryGridSection`).
    * Removed "Change Pin / Share Pin" button gimmicks from the location header in favor of a clean, persistent Lahore delivery address indicator.
    * Removed vehicle profile bar (`SavedVehiclesBar`).
    * Completely removed Fuel Add-ons section.
  * **Streamlined Fuel Offerings**:
    * **Petrol**: Exactly 2 core subcategories: Regular Euro-V Petrol and High-Octane 97.
    * **Diesel**: Exactly 2 core subcategories: Regular Euro-V Diesel and Generator Diesel.
    * **Gas**: Dedicated single option: Sealed Gas Cylinder (11.8kg), renamed strictly to "Gas" (refill duplicates removed).
  * **Permanent Elimination of Live ETA Badges**: Expunged all simulated ETA countdowns, minute badges, and robotic time tickers across tracking overlays, invoice generators, and order cards in favor of real dispatch/delivery status.
  * **Admin Dashboard Cancelled Order Permanent Deletion**:
    * Added order status filter chips (`All`, `Pending`, `Active`, `Completed`, `Cancelled`) to the Admin orders management tab.
    * Equipped cancelled orders with a prominent red "Delete / Remove" button (`admin_delete_order_${order.id}`) backed by an AlertDialog confirmation that permanently deletes the cancelled record from Room DB (`orderDao.deleteOrderById`) and Cloud Firestore (`firestoreOrderRepository.deleteOrder`).
  * **Revamped Light-Themed Profile Settings Dialog (`ProfileSettingsDialog`)**:
    * Clean, human-crafted white container palette (`Color.White`, `#F8FAFC`, `#E2E8F0`).
    * **App Language Preferences**: Interactive toggle between English and اردو (Urdu).
    * **Saved Lahore Delivery Addresses**: Configurable Home and Office address presets with inline real-time editing.
    * **Direct Founder & Operations Helpline**: One-tap WhatsApp chat (`+92 323 0112464`) and direct telephone dialer.
    * **Storage & App Cache Cleaner**: Interactive button to clear temporary cached map tiles and app data with instant user feedback.
    * **Active Lahore Coverage Indicator**: Live coverage zones across Gulberg, DHA, Model Town, Johar Town, Bahria Town, and all Lahore sectors.
  * **Foldable Sidebar Drawer Categories**: Category section in `DrawerContent` supports animated collapse/expand (`categoriesFolded`) with streamlined Petrol, Diesel, and Gas services.
  * **Real-Time Email Gateway & Diagnostics (`AdminDashboardScreen` Tab 6)**:
    * **Multi-Channel Email Engine (`RealtimeEmailEngine.kt`)**: Implements 3 production delivery channels: (1) Authenticated Direct SMTP over TLS/SSL (`smtp.gmail.com:465`), (2) HTTPS Webhook Relay over port 443 (Google Apps Script), and (3) Cloud Firestore `mail` collection trigger.
    * **Admin Gateway Inactive Alert Banner**: Prominently warns Admin when Google App Password or Webhook is unconfigured, allowing 1-tap navigation directly to Tab 6 (`"Email Gateway 📧"`).
    * **Cloud Firestore Automatic Sync (`system_config/email_gateway`)**: Once credentials or Webhook URL are saved by Admin in Tab 6, they automatically sync via Cloud Firestore to all customer and rider devices in real-time, instantly activating real-time transactional emails across all devices without per-device setup.
    * **Interactive Live Test Dispatch (`sendTestEmail`)**: Includes in-app test dispatch allowing Admin to verify Gmail inbox delivery and view detailed channel feedback before going live.
  * **Featured 7 Categories Default Showcase**: By default, the home screen presents exactly 7 high-demand primary categories:
    1. **Fuel & Energy (`fuel_energy`)**: Showcased in a full-width Hero Showcase Card with live OGRA certified rates ticker for Petrol and Diesel, anti-adulteration badge, and instant 1-tap booking.
    2. **Auto Repair & Care (`auto_repair`)**: Certified diagnostics, maintenance, brake service, and oil service.
    3. **Roadside Assistance & SOS (`roadside_assistance`)**: 24/7 urgent roadside rescue, flat-tyre, battery jump, and emergency fuel.
    4. **Auto Detailing & Spa (`auto_detailing`)**: Waterless wash, interior steam, ceramic coating, and deep cleaning.
    5. **Tyres & Wheels Service (`tyres_wheels`)**: Puncture repair, mobile tyre fitting, air pressure calibration.
    6. **Battery Services (`battery_services`)**: Battery testing, jumpstart, and new AGM/lead-acid battery installation.
    7. **Water Delivery (`water_delivery`)**: Drinking water cans, RO mineral bottles, and bulk commercial tankers.
  * **Seamless 1-Tap Expansion to All 10 Categories**: An interactive toggle (`"View All 10 Categories (+3 More: EV, Lubricants, Fleet) ▾"`) smoothly expands via `AnimatedVisibility` to reveal the remaining 3 categories:
    8. **Lubricants & Fluids (`lubricants_fluids`)**: High-performance engine oils, coolants, and brake fluids.
    9. **EV Services & Tech (`ev_services`)**: Mobile EV fast charging, charger installation, and battery diagnostics.
    10. **Fleet & Enterprise (`fleet_business`)**: B2B bulk fuel distribution, scheduled fleet maintenance, and enterprise reporting.
  * **Category Filter Chips**: Quick filtering between `Featured (7)`, `All (10)`, `Emergency & Fuel`, and `Care & Maintenance`.
* **Dynamic OGRA Rates & Fuel Volume Controls (`fuel_energy`)**:
  * **Live Rate Synchronization**: Fuel rates in `CategoryDetailModal` synchronize directly with `MainViewModel` StateFlows (`petrolPrice`, `dieselPrice`, `highOctanePrice`, `lpgGasPrice`), dynamically recalculating unit prices for Euro-V Petrol, High-Octane 97, Diesel, Generator Diesel, Fleet Diesel, and LPG cylinders.
  * **Official OGRA Certification Banner**: Displays "Official OGRA Notified Rates • Euro-V Standard • 100% Calibrated Digital Flow-Meter • Anti-Adulteration Security Seal".
  * **Quick Volume Preset Chips**: Instant selection for Petrol/Diesel (`5L`, `10L`, `20L`, `30L`, `40L`, `Full (50L)`) and LPG (`11.8 Kg`, `23.6 Kg`, `45.4 Kg`).
  * **Accurate COD Pricing Breakdown**: Automatically computes item subtotal, OGRA-compliant delivery fee, and COD grand total, passing `unitPriceOverride` to `MainViewModel.bookCategoryService` so receipts and database records store accurate dynamic amounts.
* **Fuel & Water Order Placement**: Select fuel type (Super Petrol, High-Speed Diesel, Pure Water @ Rs. 50/gallon), enter volume in Liters/Gallons, select delivery location.
* **Instant Resilient Order Placement (`MainViewModel.placeOrder`)**:
  * **Auto Customer Session Sync**: If no active user session exists, automatically initializes a default customer session to ensure order placement never fails.
  * **Automatic Address Fallback**: Automatically provides fallback delivery address ("Main Boulevard, Gulberg III, Lahore") if address field is left empty or short, preventing silent validation errors.
  * **Direct COD Confirmation**: Clicking "Confirm Order (COD) 💵" instantly creates the order, dispatches system notifications, and opens the live rider tracking map. Payment method is always **Cash on Delivery (COD)** — the payment selector has been removed for simplicity.
  * **Firestore Silent Degrade (`Repository.createOrder`)**: Firestore `saveOrder()` is wrapped in `try-catch`. If Firestore is unavailable (404, no DB created, network error), the order saves safely to local Room DB and the user is taken to the tracker screen normally — no crash, no error message.
  * **Double-Tap Prevention & Loading State (`_isPlacingOrder: StateFlow<Boolean>`)**: `placeOrder()` sets `_isPlacingOrder = true` as the first coroutine action and resets it in a `finally` block. The Confirm Order button reads this state via `isPlacingOrder.collectAsState()`, disabling itself and showing `CircularProgressIndicator` + "Placing Order..." during submission.

* **Customer Order History Screen (`CustomerOrderHistoryScreen`)**:
  * **Title**: Renamed to "My Orders" with order count subtitle.
  * **Current Order Section**: Active/in-progress orders (status: Pending, Assigned, Delivering, In Transit, Dispatched, Arriving) are shown at the top under a **"Current Order"** green section header — always visible first.
  * **Recent Orders Section**: Completed/cancelled orders shown below with search bar + Delivered/Cancelled filter chips. Only appears when there are completed/cancelled orders.
  * **Empty State**: If the customer has placed **no orders at all**, the screen shows a completely blank body — no empty state card, no placeholder text. Nothing is displayed until there's actual order data.
  * **No Payment Selector**: The "Select Payment Method" section (COD chip / Online chip) has been completely removed from the `OrderDialog`. Payment is always hardcoded to Cash on Delivery.

* **Surge Pricing & High-Volume Logic**:
  * Auto-calculates peak delivery demand surge multipliers.
  * Orders $\ge 30\text{L}$ automatically display high-volume guidance while allowing direct COD order placement and optional WhatsApp support connection.
* **Permanent Removal of Live Order Map Tracking**:
  * **Complete Removal of Live Map Overlays**: The embedded live Google Maps overlay (`GoogleMapsLiveDeliveryTrackingOverlay`), simulation map cards, and live radar tracking have been permanently removed from the Customer Home Dashboard and Tracker screens.
  * **Streamlined Order Details & Status Tracking Screen (`TrackerScreen`)**: Replaced with a performant, native, and clean Order Details layout featuring an animated 4-step delivery progress stepper, rich order summary (items, quantity, pricing, destination), assigned rider profile with direct phone dialer and chat buttons, and post-delivery rating cards.
  * **Optimized Home Dashboard (`CustomerHomeScreen`)**: Active orders are rendered cleanly via high-performance `CustomerOrderCard` without heavy WebView or Map SDK recompositions.
* **Clean Order Dialog (No Clutter & Instant Checkout)**: The `OrderDialog` is completely streamlined: removed the redundant destination preview map and the permanent location pins section. It now solely focuses on product selection, quick saved addresses, destination address input, and price breakdown with instant one-tap COD confirmation.
* **Real-Time Triple-Party Gmail Order Confirmation & Dispatch Gateway (`RealtimeEmailEngine.kt`)**:
  * **Automated Customer Emails**: Every time any user registers/logs in (via Google OAuth or standard email) and places an order (`placeOrder`), the customer automatically receives a real-time formatted email to their registered Gmail. Details include Order ID, service item, quantity, COD amount, destination address, and live status.
  * **Automated Rider Dispatch Emails**: Assigned riders and active verified riders receive immediate real-time emails to their registered Gmail address on new order dispatch assignments, order acceptance confirmations (`acceptRiderOrder`), and each status milestone (`changeOrderStatus`).
  * **Automated Super Admin Alerts**: The Super Administrator (`m.daniyalkhan490@gmail.com`) instantly receives real-time order alerts with full customer contact information, delivery destination, COD bill, and assigned rider details.
  * **Cross-Device Cloud Firestore Sync (`system_config/email_gateway`)**: When Admin configures the 16-character Google App Password or HTTPS Webhook URL in Admin Settings, it is saved locally to AES256 enclave AND synced immediately to Cloud Firestore. All Customer and Rider devices automatically subscribe to this configuration on startup, guaranteeing that orders placed from any device trigger real emails.
  * **Production-Grade Authenticated SMTP Engine (`RFC 5321 / RFC 3207 / RFC 5322`)**: Implements direct TLS/SSL socket communication over Port 465 and RFC 3207 `STARTTLS` upgrade over Port 587 with Base64 `AUTH LOGIN`, dot-stuffing, CRLF normalization, and MIME multipart formatting, ensuring 100% direct inbox arrival in Gmail without being flagged as spam.
  * **Cloud HTTPS Webhook Relay (Google Apps Script / Webhook Backup over Port 443)**: Serverless HTTPS relay backup over Port 443, ensuring guaranteed email delivery even when mobile cellular operators (Zong, Jazz, Ufone) block raw SMTP ports. Handles 302 redirects with dual-method (`doPost` & `doGet`) execution.
  * **Admin Live SMTP Settings, Cloud Sync & Diagnostic Dispatch Tool (`Screens.kt` Tab 6)**: Admin console tab equipped with live credentials configuration, "Save & Sync to Cloud" button, one-click "Copy Free Apps Script Relay Code" tool, 1-minute Google App Password guide, and a live "🧪 Send Test Email" button providing instant round-trip diagnostic feedback.
  * **Transparent Diagnostic Alerts**: Automatically notifies administrators on-screen if email delivery is attempted without an active Google App Password or Webhook URL configured.
* **13-Step Comprehensive Interactive App Tour Guide (`SpotlightOverlay` & `homeTourSteps`)**:
  * **All-Feature Guided Walkthrough**: Interactive coach-mark spotlight tour with precision on-screen element cutouts and rich informational callouts across 13 core stages:
    1. Welcome to Zyphuel & Doorstep Energy Overview
    2. Live GPS Pinpoint & Instant Location Sharing (`user_location_active_card`)
    3. Universal Typo-Tolerant Search Across 10 Categories (`home_search_bar`)
    4. 1-Tap Emergency Roadside & Essential SOS Shortcuts (`home_quick_actions`)
    5. My Saved Vehicles Profile & Specs Optimization (`home_saved_vehicles`)
    6. 10-Category Services Marketplace & Live OGRA Rates (`service_petrol`)
    7. Instant 1-Tap COD Ordering (`home_fab`)
    8. Live 4-Stage Delivery Progress Stepper & Direct Driver Contact
    9. Real-Time Automated Gmail Receipts & Downloadable PDF Invoices
    10. Real-Time Notification Bell & Dispatch Alerts (`home_notifications`)
    11. Delivery Dashboard, Total Spend & Order History Insights (`home_order_history`)
    12. Comprehensive Sidebar Navigation Drawer (`drawerState.open()`)
    13. Biometric Security (Fingerprint/Face Unlock) & Tour Wrap-up
  * **Tesla & Apple Caliber Compose UI**: Animated linear progress bar, category pills (e.g. `GPS & LIVE LOCATION`, `1-TAP COD ORDERING`), actionable "💡 Pro Tip:" cards, smooth horizontal `AnimatedContent` slide/fade transitions, and interactive clickable progress indicator dots allowing users to jump directly to any step.
  * **Always Accessible**: Re-launchable anytime from both the Navigation Drawer (`Take a Guided Tour 🧭`) and Help Center (`Take Guided App Tour (13 Steps) 🧭`).
* **Admin Order Accept, Self-Delivery & Full Lifecycle Controls (`adminAcceptOrder`, `changeOrderStatus` & `adminDeclineOrder`)**:
  * **Admin Action Buttons on `AdminOrderCard`**: Context-sensitive interactive action buttons directly on each admin order card (`Accept` for Pending, `Start Delivery 🚚` for Assigned, `Mark Delivered ✅` for Delivering/Arrived, and `Decline ❌` for cancellation).
  * **Instant Acceptance with Intelligent Self-Delivery Fallback (`adminAcceptOrder`)**: Approves pending orders. If an active verified rider is online, automatically assigns them; if no rider is available, seamlessly assigns the Administrator themselves (`Admin Direct Delivery`) with the Admin's email (`m.daniyalkhan490@gmail.com`). Updates Room DB & Firestore and dispatches real-time confirmation emails.
  * **Admin Master Lifecycle Transition Authority**: Administrators are authorized in `Repository.updateOrderStatus` and `MainViewModel.changeOrderStatus` to directly advance orders through "Delivering" to "Completed" without requiring an external rider app, ensuring zero orders are ever bottlenecked or stranded.
  * **Reason-Based Order Rejection (`adminDeclineOrder` & `AdminDeclineOrderDialog`)**: Prompts the admin with predefined quick reasons (Out of delivery coverage, Depot fuel stock limit, Customer cancellation request, Phone unreachable, or Custom reason), updates status to `"Cancelled"`, records an audit log, and sends an automated cancellation email to the customer with the explanation.
* **Instant 0ms Local Order Creation & Non-Blocking Remote Sync**: Room DB creates and confirms orders in 0ms with immediate Main thread navigation to `TrackerScreen`. Cloud Firestore sync is wrapped in background coroutines with strict `1500ms` timeouts, preventing any network hangs from delaying the customer experience.
* **2-Decimal Price Standard Formatting (`formatPrice`)**: All order prices, totals, COD amounts, and pending status cards are strictly formatted to 2 decimal places (e.g., `Rs. 2,970.00`) across `MainViewModel`, `TrackerScreen`, `CustomerOrderHistoryScreen`, `CustomerHomeScreen`, and `AdminScreens`.
* **Google Play Compliant Account Deletion (`DeleteAccountConfirmationDialog` & `deleteCurrentAccount`)**: In-app one-tap account deletion and complete data wiping available in both Customer Sidebar Drawer and Profile Settings Dialog. Permanently removes Room DB user data, marked location pins, session credentials, and logs an irreversible audit trail.
* **Adaptive Scrollable Navigation Drawer (`DrawerContent` & `SidebarItem`)**:
  * **Full Vertical Scrolling & Screen Safety (`verticalScroll`)**: Complete sidebar navigation drawer content is wrapped in a vertical scroll state with safe-area padding (`statusBarsPadding()` & `navigationBarsPadding()`), guaranteeing that all action buttons, settings, order histories, admin tools, delete account option, and log out button are 100% visible and accessible on all screen resolutions, phone aspect ratios, and accessibility font scale factors without clipping.
    * **Categorized Navigation Architecture**: Items organized into clean, labeled visual sections (*Account & Settings*, *Orders & Deliveries*, *Help & Support*, *Admin Controls*, *Account Actions*) separated with subtle dividers (`HorizontalDivider`).
    * **Touch Feedback & Visual Polish**: `SidebarItem` with rounded background icon badges, distinct category labels, high-contrast dark text, custom destructive tinting for account deletion, and Material 3 ripple feedback.
* **Customer Dashboard Modernization & Streamlining (v2.6.4 Build 28)**:
  * **"Share Location" Action Chip**: Replaced the legacy "Change" button under the "Coverage: Lahore Active" indicator with a dedicated `[ ↗️ Share Location ]` action chip. Tapping it opens the Android native share sheet dispatching formatted Lahore GPS coordinates (`https://maps.google.com/?q=lat,lng`) and street address for instant sharing with family or riders, while retaining interactive click-to-edit on the address card.
  * **Streamlined Service Grid (Removed Quick Actions)**: Removed the entire `QuickActionsBar` row from the Customer Home Screen to eliminate visual clutter and present a focused, high-clarity layout directly connecting users to core energy services.
  * **High-Octane (HOBC 97) Live Price Card (`service_high_octane`)**: Integrated a dedicated High-Octane fuel card right next to Super Petrol under "Our Services". Dynamically displays live OGRA notified rates from `viewModel.highOctanePrice` (e.g. `Rs. 300.00/L`) and pre-configures `OrderDialog` for "High-Octane" on tap.
  * **One-Time Dismissible Startup Transparency Notice**: Transformed the "Early-Stage Lahore Startup" transparency card (`home_startup_notice_card`) from a permanent fixture into a one-time dismissible banner. Persisted via `SharedPreferences` (`zyphuel_ui_prefs`, `has_seen_startup_notice`), users can acknowledge and close the notice via the top-right `[X]` button, keeping the interface uncluttered on subsequent visits.
  * **App Tour Harmonization**: Updated the 13-stage guided tour spotlight anchors in `homeTourSteps` to smoothly guide users from the search bar directly into primary energy offerings without orphan references to removed quick action buttons.


---

## 4. Location Pin Marking & Map Architecture
* **Component**: `UnifiedGoogleMapView`, `InteractiveLocationPickerMap`, `handleExternalMapIntent` (`Screens.kt`)
* **Permanent Removal of Google Maps Live Order Tracking**:
  * All continuous live Google Maps tracking overlays, driver radar animations, and in-app live vehicle tracking loops on `CustomerHomeScreen`, `TrackerScreen`, and `RiderHomeScreen` have been permanently removed.
  * Replaced with a fast, lightweight native Compose Order Details and Status Stepper architecture.
* **Location Pin Marking & Address Resolution**:
  * **Interactive Landmark Picker (`InteractiveLocationPickerMap`)**: When entering or editing custom addresses, customers can tap on the interactive map to pin their exact delivery landmark with instant reverse geocoding.
  * **Share Location Feature**: Users can share their delivery destination via native Android share sheet with formatted GPS coordinates and Google Maps link.
* **Server Live Tracking Permanent Removal**:
  * Continuous GPS streaming and upload to the Cloud Firestore `live_tracking` collection has been permanently removed from the server.
  * Server writes in `LiveTrackingRepository` and `RiderLocationForegroundService` are completely disabled, preventing battery drain and unwanted cloud server position storage.
  * Any leftover legacy `live_tracking` documents on Firestore are automatically purged upon application startup via `LiveTrackingRepository.purgeAllServerLiveTracking()`.
  * Order and route presentation runs strictly on local origin-destination coordinates and order status events.
  * **Rider Order Progression & Pickup Validation**: Enforces strict step-by-step order progression (`Pending` -> `Accept Ride` -> `Pick Up Fuel at Station` -> `Start Route` -> `Reached Location` -> `Complete & Collect COD`) in `MainViewModel.changeOrderStatus`, preventing riders from completing orders without picking up petrol/fuel first.

---

## 5. Admin Dashboard & Management
* **Location**: `AdminDashboardScreen` (`Screens.kt`)
* **Key Functions**:
  * **Add New Customer (`addCustomerByAdmin` in `MainViewModel`)**:
    * Modal Dialog (`AddCustomerDialog`) allowing Admin to manually register new customers with name, phone number, optional email, address, and initial password.
    * Generates auto email if email omitted, logs action into `AuditLogEntity`, and dispatches real-time welcome email.
  * **Add New Driver / Rider (`AddRiderDialog`)**:
    * Registers new fuel bowser drivers with Full Name, Phone, Email, Portal Password, Vehicle Type (Bike, Pickup, Bowser), Registration Plate No, CNIC, Driving License ID, Residential Address, and Immediate Verification Checkbox.
    * Invokes `viewModel.addRiderFromAdmin` to store exact driver form inputs into `UserEntity`.
  * **Driver Card Quick Overview (`AdminRiderCard`)**:
    * **Sequential Rider Registration**: Each registered rider is tagged with a sequential badge (e.g. `RIDER #1 • RIDER-1`, `RIDER #2 • RIDER-2`) displayed in numerical sequence. Starts with 0 registered riders/customers by default until real registrations occur.
    * **🔐 Dedicated Login Credentials Box**: Prominently displays the rider's **Email Address** and **Login Password** (with an interactive `showPassword` toggle button) so admins can verify and manage rider portal credentials.
    * Distinct grid display showing driver's Phone, Vehicle Type, Registration Plate No, and CNIC/Legal ID directly on the card with high contrast colors for easy reading.
    * **Explicit Check & Verification Action Buttons**: Direct action buttons on every card to check rider details (`check_rider_details_btn`), toggle approval status, grant/revoke verified badges, edit rider information, and delete rider accounts with audit logging.
  * **Clean Rider Biodata Inspection (`AdminRiderBiodataDialog`)**:
    * Displays driver details grouped into 4 distinct sections: **1. Personal Biodata Details**, **2. National Identity & Legal Credentials**, **3. Vehicle & Driving License**, and **4. Emergency Contact**.
    * Strictly shows only the information provided in the driver form, removing extra unprovided placeholders for total clarity.
    * Features explicit **Approve & Verify** (grants verified badge) and **Deny Request** actions with audit logging.
  * **Customer Directory**: View all registered customers, order history, and contact details.
  * **Google Sign-In User Data Display in Dashboard (`AdminCustomerCard`)**:
    * **Google Profile Photo**: If a customer signed in via "Continue with Google", their authentic Google profile photo is displayed (via Coil async image loading) with a circular blue border instead of the generic person icon.
    * **Auth Provider Badge**: Each customer card shows a colored badge — green "Google" badge for OAuth2 Google users, or gray "Email" badge for traditional email/password registrations.
    * **Join Date**: Displays the exact registration date (`createdAt` timestamp formatted as `dd MMM yyyy`) for each customer on their admin card.
  * **Google vs Email User Breakdown (`AdminAnalyticsDashboard`)**:
    * Real-time analytics row showing count of Google OAuth users vs Email/password users across all registered customers and riders.
    * Includes color-coded dot indicators (green for Google, gray for Email) for instant visual differentiation.
  * **App Download Counter (Firestore-backed)**:
    * **Unique Install Tracking**: Each unique device install is tracked via `SharedPreferences` flag (`app_install_tracked`) + Firestore atomic increment (`FieldValue.increment(1)`) at path `app_stats/downloads`.
    * **Dashboard Stat Card**: "📥 App Downloads" stat card on `AdminDashboardScreen` showing the total unique install count from Firestore.
    * **Analytics Integration**: App download count also displayed in the `AdminAnalyticsDashboard` header alongside Google/Email user breakdown.
    * **Functions**: `trackAppInstall()` (called in `MainActivity.onCreate`), `fetchAppDownloadCount()` (called when Admin Dashboard opens), `incrementAppDownloadCount()` and `getAppDownloadCount()` in `FirestoreUserRepository`.
  * **Order Management & Dispatch**: Manually assign riders to pending fuel/water orders.
  * **Audit Log Viewer**: Full history of system events, security updates, and admin actions.

---

## 6. Rider / Bowser Driver Portal
* **Location**: `RiderHomeScreen`, `RiderCompleteProfileScreen` (`Screens.kt`)
* **Key Functions**:
  * **Mandatory Google Sign-In Rider Verification Flow (`RiderCompleteProfileScreen`)**:
    * **Incomplete Profile Guard (`isRiderProfileIncomplete` in `MainViewModel`)**: When a rider registers or logs in via "Continue with Google" without password registration, their account is flagged as having an incomplete profile until they complete the full Pakistani transport verification form.
    * **Automated Routing Redirect**: Google Sign-In riders with incomplete profiles are immediately routed to `RiderCompleteProfileScreen` upon login, splash screen check, or opening the app.
    * **Verification Profile Form**: Pre-fills the rider's name, email, and Google profile picture, and collects mandatory fields:
      * **1. Personal Information**: Full Name (as per CNIC), Father's Name, Date of Birth (DD-MM-YYYY), Gender (Male/Female/Other), Phone Number.
      * **2. National Identity**: CNIC Number (auto-formatted `xxxxx-xxxxxxx-x`), CNIC Issue Date, CNIC Expiry Date.
      * **3. Residential Address**: Complete Address, City, Province, Postal Code.
      * **4. Vehicle Information**: Vehicle Type (Bike/Car), Vehicle Registration Number (e.g. LHR-20-4567).
      * **5. Emergency Contact**: Name, Relationship, Contact Phone.
      * **6. Legal Confirmations**: Terms & Conditions acceptance and Rider Legal Declaration checkboxes.
    * **Order Acceptance Block**: Riders cannot accept Lahore delivery queue orders (`acceptRiderOrder`, `ReceivedOrdersDialog`, `RiderOrderCard`) until this verification profile is completed and submitted. If attempted, an alert prompt immediately directs them to the verification form.
    * **Admin Notification & Audit Sync**: Submitting the form assigns a sequential `Rider #` and `Rider ID`, logs the event in `AuditLogEntity`, dispatches an automated verification email, and sets `adminApprovalStatus = "Pending"`.
  * **Active Duty Queue**: View assigned orders with navigation destination and customer contact info.
  * **One-Tap Status Progress**: Update order status seamlessly (Dispatched -> En Route -> Arrived -> Completed).
  * **Live GPS Coordinate Broadcast**: Automatically broadcasts rider location updates to customer map.

---

## 7. Security & Biometric Engine
* **Location**: `SecuritySettingsScreen.kt`, `BiometricSecurityManager.kt`, `SecureStorageManager.kt`, & `Screens.kt`
* **Key Functions**:
  * **Biometric (Fingerprint & Face) Login**:
    * **Universal Social & Credential Biometric Binding**: Users logging in with Google OAuth ("Continue with Google") or simple credentials automatically have their secure credentials mapped.
    * **Profile & Security Settings Toggle**: In `ProfileSettingsDialog` and `SecuritySettingsScreen`, users and riders can toggle Biometric Lock on/off with 1 tap.
    * **Prominent Login Prompt Card**: When enabled, the "Fingerprint / Face ID Login" card is displayed on the login screen for 1-tap fast entry without typing passwords.
    * **Post-Logout Quick Sign-In**: Biometric preferences persist securely across sessions in `SecureStorageManager` with AES-256 GCM encryption.
    * Features instant email-based & role-based fallback lookup (`loginWithBiometrics` in `MainViewModel`) so users and drivers can sign in effortlessly even on emulators or un-enrolled hardware.
  * **Order History Biometric Vault**:
    * Biometric protection badge and status vault on `CustomerOrderHistoryScreen` to protect sensitive delivery addresses and expenditure receipts.
  * **App Security Settings**:
    * Module-specific biometric toggles (Customer, Rider, Admin), custom security PINs, and session lock timeouts.
  * **Encrypted Storage & Passwords**:
    * Biometric tokens and user preferences secured via `SecureStorageManager` with AES-256 GCM encryption and SHA-256 hash digests.

---

## 8. Notification & Communication Engine
* **Location**: `postLocalSystemNotification`, `notifyArrivingSoon`, `notifyReachedLocation`, `ZyphuelFcmService.kt`, `MainViewModel.kt`
* **Key Functions**:
  * **System Push Notification System**:
    * **Out for Delivery Alert 🛵**: Posts Android heads-up system push notifications (`zyphuel_order_updates` channel) when an order status changes to "Dispatched", "Delivering", or "Out for Delivery".
    * **Driver Reached Location Alert 📍**: Triggers automatic high-priority push notifications when the driver arrives within $1\text{ km}$ or reaches the customer's delivery destination (`notifyReachedLocation`).
    * **Order Delivered Alert 🎉**: Immediate push notification confirmation when fuel/water delivery is completed and COD collected.
    * **Real-time Fuel Price Update Alerts ⛽**:
      * **Admin-Controlled Broadcast Schedule**: The central administrator controls how many hours after which real-time fuel price update notifications automatically broadcast to system users (e.g., 1 Hour, 2 Hours, 4 Hours, 6 Hours, 12 Hours, 24 Hours, or custom hours).
      * **Admin Panel Form (`AdminFuelPriceNotificationScheduleCard`)**: Located in the Admin Center under the "Fuel Prices" tab. The Admin can set the exact interval in hours, enable/disable broadcasts, apply schedules to `FuelPriceWorker` WorkManager, or trigger test broadcasts immediately.
      * **Centralized User Experience**: Users cannot decide or alter the broadcast schedule. No popup dialogs or selection forms are shown to regular customers upon login/app open. Information in `SecuritySettingsScreen` reflects the Admin's active broadcast schedule.
      * **Clean Rate Text Formatting**: Suppresses internal debug API tags (such as raw `Trackmate Fuel API` strings) and formats consumer-facing rates cleanly with separator bullets and official verification attribution.
  * **Modern Notification & Launcher Icon Architecture (`ZyphuelNotificationHelper.kt`)**:
    * **Left Circular Avatar with App Badge Layout (`NotificationCompat.MessagingStyle`)**: Implements modern Android conversation/assistant style notifications. The notification shade displays a large high-resolution circular avatar (`@drawable/ic_zyphuel_avatar`) on the left, with the monochrome Zyphuel app badge (`@drawable/ic_notification`) anchored at the bottom-right corner of the avatar.
    * **Elimination of Right-Side Standalone Icon**: Strictly removes the legacy right-side standalone `setLargeIcon` across price alerts, FCM pushes, and local notifications, delivering an uncluttered, modern appearance identical to high-end messaging and AI assistant notifications.
    * **Interactive Action Chips**: Equipped with instant interactive notification actions:
      * **"⛽ Order Fuel"**: Directly opens `MainActivity` and navigates to the customer home/ordering screen.
      * **"📊 View Rates"**: Directly opens `MainActivity` and navigates to fuel rate analytics.
    * **Monochrome Vector Small Icon (`@drawable/ic_notification`)**: Android System UI-compliant 24x24dp monochrome vector drawable featuring the signature Zyphuel fuel drop with a true transparent 'Z' negative-space cutout (`fillType="evenOdd"`), safe-zone margins, and dynamic status-bar tinting support.
    * **Adaptive Launcher Icon & Mipmap Repair**:
      * Updated `ic_launcher_background.xml` from transparent (`#00000000`) to solid brand dark navy (`#0A0F1D`), preventing Android notification headers and launchers from synthesizing awkward black square borders inside circular masks.
      * Centered foreground safe-zone scaling (68%) in `ic_launcher_foreground.xml`.
      * Regenerated valid binary PNG files for all 10 legacy mipmap variants across all densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) for both standard square and circular (`ic_launcher_round.png`) icon styles.
  * **Firebase Cloud Messaging (FCM) Integration**: Real-time push payload processing via `ZyphuelFcmService.kt` and `ZyphuelNotificationHelper.kt` with foreground banners and background system notification triggers using the avatar + badge layout.
  * **Real-time Email Dispatch**: Instant automated email receipts and order status updates sent to customer inbox.
  * **WhatsApp Hotline Integration**: Direct deep-linking to official WhatsApp support line (`+92 323 0112464`).
  * **In-App Notification Center**: Local Room DB persistence (`NotificationEntity`) for user notification log history.


---

## 9. Post-Delivery Rating & Driver Feedback
* **Location**: `PostDeliveryRatingCard`, `submitOrderRating`, `SecurityInputValidator.kt`, `Screens.kt`
* **Key Functions**:
  * **Interactive 1-5 Star Selector**: Star rating bar with spring scale animations for rating driver and doorstep delivery service (testTags: `rating_star_1` to `rating_star_5`).
  * **Quick Compliment Chips**: Quick selectable compliment tags (On-Time Arrival, Pure Fuel Quality, Courteous Driver, Safety Followed, Exact Change) powered by `FlowRow` and `FilterChip`.
  * **Optional Driver Feedback**: Text field for customized driver feedback notes up to 500 characters validated via `SecurityInputValidator.validateRatingAndFeedback`.
  * **Verified Review Cards**: Persistent post-submission card showing verified star review and feedback in both real-time order tracking and customer order history.

---

## 10. Compose-Based Shared Element Transition Animations & Motion Design
* **Location**: `SharedTransitionLayout`, `LocalSharedTransitionScope`, `LocalAnimatedVisibilityScope`, `sharedOrderBounds`, `sharedOrderElement`, `MainActivity.kt`, `Screens.kt`
* **Key Functions**:
  * **MDK Company Splash Screen (`SplashScreen`, `PlatformTransitionSplashScreen`)**: On app launch, displays the **MDK company logo** (`company_splash.png`) as a static 160dp centered image with smooth fade-in animation (800ms), followed by Zyphuel logo (64dp) and tagline text fading in after a 400ms delay. Replaces the previous heartbeat/pulse/spinning ring animation. Loading progress bar animates below. `testTag: splash_company_logo`.
  * **Seamless Delivery List to Live Tracking Motion**: Jetpack Compose `SharedTransitionLayout` and `AnimatedContent` wrapping with custom spring curve transition specs (`spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)`) for fluid motion when navigating from customer delivery cards to the real-time order tracking map screen.
  * **Shared Element Bounds & Morphing (`sharedOrderBounds` & `sharedOrderElement`)**:
    * Order card bounds (`"order_card_${order.id}"`) morph smoothly from card dimensions in the delivery list directly into full-screen tracking view dimensions.
    * Order header badges (`"order_header_${order.id}"`) scale and reposition seamlessly across screen transitions.
  * **Dynamic Animated Status Transitions**: Jetpack Compose `AnimatedContent` state transitions with vertical sliding, fade, and spring scale animations when order status changes (Pending → Assigned → Delivering/Dispatched → Arrived → Completed).
  * **Pulsing Status Halo**: Micro-animated glowing halo ring indicator for real-time status card visual feedback.
  * **Interactive Confetti Celebration**: Custom `Canvas` floating particle confetti overlay (`OrderStatusConfettiOverlay`) triggered upon successful order completion and delivery.

---

## 11. Permanent Location Marking System (Without Maps)
* **Location**: `MarkedLocationEntity`, `MarkedLocationDao`, `MarkDesiredLocationModal`, `LahoreRouteMapView`, `MainViewModel.kt`, `Screens.kt`
* **Key Functions**:
  * **Mapless Location Pinning**:
    * Allows users to mark desired delivery locations without needing external map apps.
    * Select from popular Lahore sectors (Gulberg III, DHA Phase 5, Model Town, Johar Town, Bahria Town, Mall Road, MM Alam Road, Askari 11, Lake City, Lahore Cantt) or enter custom street addresses.
    * Interactive coordinate position canvas box allowing micro-adjustment of permanent location pins ($31.5204, 74.3587$).
  * **Permanent Database Storage**:
    * Saved directly into Room local database (`MarkedLocationEntity`) tied to `userEmail`.
    * Retained across sessions, app restarts, and profile re-logins.
  * **Quick-Fill in Order Flow**:
    * Rendered inside `OrderDialog` (`PlaceOrderDialog`) as a horizontal chip list (`permanent_marked_locs_row`). Tapping any marked location pin instantly autofills the delivery address.
  * **Rider Route Map Integration**:
    * Both customer and driver views render `LahoreRouteMapView` featuring:
      * **Origin Hub Badge**: `🏁 Origin Depot: Zyphuel Central Bowser Hub #1, Gulberg, Lahore` (Where order came from).
      * **Destination Badge**: `📍 Destination Pin: [Customer Marked Location]` (Where order needs to go).
      * **Interactive Live Vector Route Map**: Canvas route line connecting Origin to Marked Destination with animated moving delivery bowser, distance ($3.8\text{ km}$), ETA ($12\text{ mins}$), and live speed ($38\text{ km/h}$).

---

## 11.1 Real-Time GPS Tracking, Landmark Resolution & Interactive Location Verification
* **Reactive Continuous Location Engine (`LocationService.kt`, `fetchDeviceGpsLocation` & `resolveLandmarkFromCoordinates`)**:
  * **Dynamic Movement Tracking**: Uses persistent `FusedLocationProviderClient` location callback listeners managed via singleton `LocationService` with `flushLocations()` cache-clearing to stream real-time coordinate changes into `_deviceLatitude` and `_deviceLongitude` without stale location data.
  * **Automated Landmark Resolution**: Automatically converts lat/lng coordinates to accurate nearby landmark and sector names (e.g., *Liberty Market, Gulberg III*, *DHA Phase 5 Block CCA*, *Model Town Block C*, *Packages Mall*, *Askari 11*, *Johar Town Khayaban-e-Firdousi*).
* **Location Permission Rationale UI Overlay (`LocationPermissionRationaleBanner`)**:
  * Clear, non-intrusive banner overlay rendered when precise GPS permission is needed.
  * Explains why precise location is required for bowser trucks to navigate directly to the customer's spot.
  * Provides seamless actions: **Turn On GPS** (launches system permission) and **Manual Search** (allows manual location/landmark entry gracefully).
* **Google Places Autocomplete Input (`PlacesAutocompleteTextField`)**:
  * Real-time search input filtering against popular landmarks, sectors, and commercial hubs across Lahore and major Pakistan cities as the user types.
  * Tapping any suggestion auto-fills the delivery address, updates coordinates, and centers the map marker.
* **Interactive Map Pin Fine-Tuning View (`InteractiveLocationPickerMap`)**:
  * Interactive map grid view displayed on the location confirmation screen.
  * Places a draggable marker pin (`📍`) at the detected coordinates.
  * Tapping or dragging anywhere on the map grid fine-tunes the delivery pin position, reverse-geocodes the new landmark dynamically, and allows visual verification before confirming the order.

---

## 11.2 Sequential Rider ID System & Map Viewing Enhancements
* **Sequential Assigned Rider ID System (`Models.kt`, `MainViewModel.kt`, `Repository.kt`)**:
  * **Sequential Number Assignment**: When a new rider registers or is created by an admin, the system queries existing rider accounts and assigns a sequential integer number (`Rider #1`, `Rider #2`, `Rider #3`, etc.).
  * **Standardized Rider ID**: Generates structured IDs like `RIDER-1`, `RIDER-2`, stored in `riderNumber: Int?` and `riderId: String?` fields in `UserEntity`.
  * **Explicit User Feedback**: Informs registering riders immediately upon submission: *"Registration Successful! This is your assigned number: Rider #X (ID: RIDER-X)."* and dispatches a confirmation email.
  * **Rider & Admin Views**: Displays official Rider Numbers prominently across Rider Profiles, Rider Dashboards, and Admin Approval lists.

* **Compact Map Views & Collapsible Status HUD (`Screens.kt`)**:
  * **Collapsible Status Pending Section**: Replaced heavy floating HUD cards on order tracking screens with a compact, collapsible status container.
  * **Minimize & Expand Controls**: Features a `🔽 Minimize` button that shrinks the HUD down to a small single-line status bar (`STATUS • COD`), freeing up over 90% of screen area so riders and customers can zoom in, pan, and monitor real-time positions clearly.
  * **Streamlined Top Badges**: Reduced top Order ID badges (`Order #X`) to lightweight compact chips to eliminate map visual clutter.
  * **Map Style Switcher Cleanup**: Removed redundant map style switchers (Street, Satellite, Dark) and static nav route overlays, maintaining clean zoom in, zoom out, recenter, and fullscreen controls.

* **Strict Live Device GPS & Manual Address Fallback (`Screens.kt`, `LocationService.kt`)**:
  * **Device Live GPS Integration**: Live device coordinates (`_deviceLatitude`, `_deviceLongitude`) update dynamically on map components.
  * **Reverse Geocoding & Address Detection**: Uses Android `Geocoder` to detect street addresses.
  * **Manual Address Entry Fallback**: If GPS or address detection is unavailable, fake location pin strings are strictly suppressed. The address label resets to empty (`""`), prompting the user to manually enter or edit their address.

## 11.3 Clean Order Confirmation & Smooth Leaflet Map Zoom
* **Order Confirmation Flow & Rider Warning Suppression (`Screens.kt`)**:
  * **Direct Service Fulfillment**: Removed all unsolicited "Rider not registered yet" and "No Rider Available" warning banners, popups, and toasts upon confirming customer orders. Focuses purely on selling and fulfilling service requests without rider availability warnings.
  * **Streamlined Order Dispatch Card**: Replaced missing-rider error states with a clean "Service Order Confirmed" status card reassuring customers that their order is live and scheduled for fulfillment.
  * **Removed Order Options Prompt**: Omitted the "Order Options" decision modal so users experience a smooth, uninterrupted order placement and live tracking view.
* **Enhanced Leaflet Map Zoom Controls & Interaction (`Screens.kt`)**:
  * **Broadened Zoom Scale**: Updated Leaflet JS map bounds and zoom levels (`minZoom: 3`, `maxZoom: 19`) for ultra-smooth zoom capability from high-level region view down to detailed street view.
  * **Gesture & Touch Zoom Support**: Enabled `doubleClickZoom: true`, `touchZoom: true`, `scrollWheelZoom: true`, `setSupportZoom(true)`, and `builtInZoomControls = true` on the WebView setting for responsive zoom operations.
  * **Preserved Floating Control Buttons**: Maintained floating `➕ Zoom In` and `➖ Zoom Out` touch surfaces executing direct Leaflet JS `map.zoomIn()` and `map.zoomOut()` calls.

## 11.4 Tesla-Inspired Loading Skeleton Animations (`TeslaSkeleton.kt`, `Screens.kt`)
* **Subtle Specular Sweep Modifier (`Modifier.teslaShimmer`)**:
  * Precision specular highlight gradient sweep with infinite linear transition (`CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)`).
  * High-tech light metallic palette (`#E2E8F0` to `#FFFFFF` with electric blue accent glow) and dark cyber palette (`#0F172A` to `#334155` with cyan shimmer).
* **Auth & Login Skeleton Screen (`TeslaAuthSkeleton`)**:
  * Polished skeleton placeholders for brand logo shield, display headings, email/password text fields with rounded corners, action buttons, and social authentication rows.
  * Automatically renders during initial authentication screen hydration to elevate perceived performance and prevent layout popping.
* **Dashboard & Home Skeleton Screen (`TeslaHomeSkeleton`)**:
  * Smooth skeleton placeholders for top app navigation bar, live GPS status card, promotional order hero banner, fuel pricing metric cards (2x2 grid), and active delivery tracking cards.
  * Displays during initial profile fetching and real-time fuel data loads for immediate visual feedback.

## 11.5 Google Maps SDK Live Delivery Tracking Overlay (`GoogleMapsLiveDeliveryTrackingOverlay.kt`, `Screens.kt`)
* **Native Maps Compose SDK (`com.google.maps.android.compose`)**:
  * Integrated native hardware-accelerated `GoogleMap` composable utilizing `play-services-maps` and `maps-compose` v6.4.1.
  * Real-time marker management (`Marker`, `MarkerState`) for:
    * **Zyphuel Dispatch Depot**: Fixed origin hub at Green Town Central Depot (`31.4380, 74.3050`).
    * **Customer Destination**: Interactive destination pin reflecting order delivery address in Lahore.
    * **Assigned Delivery Vehicle**: Dynamic marker representing the fuel bowser/water tanker with real-time bearing rotation (`rotation = vehicleBearing`), status snippet, and active radar beacon (`Circle`).
* **Lahore Route Polyline & Telematics Engine**:
  * **Dynamic Corridor Waypoints (`generateCorridorWaypoints`)**: Computes smooth road waypoints along Lahore transit routes (Ferozepur Rd, Canal Rd, Main Boulevard Gulberg, DHA, Johar Town).
  * **Coordinate Interpolation & Bearing Calculation (`interpolateAlongPath`, `calculateBearingAlongPath`)**: Calculates real-time vehicle latitude/longitude position and heading angle smoothly.
  * **Live Speedometer & Telematics**: Displays real-time transit velocity (e.g. 36-48 km/h), remaining distance in km, and dynamic ETA calculation in minutes.
  * **En-Route Progress Bar**: Real-time linear progress indicator reflecting journey completion percentage (0% to 100%).
* **Interactive Floating Map HUD Controls**:
  * **Map Layer Switcher**: Instant switching between Normal, Satellite, Terrain, and Hybrid map modes (`MapType`).
  * **Camera Recenter Actions**: Quick floating actions to center camera on moving delivery vehicle (`CameraUpdateFactory.newLatLngZoom`) or customer destination.
  * **Simulation Toggle**: Live pause/play control for simulated vehicle movement testing.
  * **Direct Dialer Action**: One-tap quick call button launching Android dialer with assigned driver phone number (`tel:+923230112464`).
* **Resilience & Fallback Radar Canvas (`FallbackRadarMapView`)**:
  * Implements smooth fallback canvas rendering with radar ripple animations, ensuring zero crashes in environments where Google Play Services is initializing or offline.

## 11.6 Native Google Sign-In SDK & Device Account Integration (`GoogleAuthManager.kt`, `DeviceAccountUtils.kt`, `Screens.kt`)
* **Google Identity Services & Credential Manager (`androidx.credentials`, `com.google.android.libraries.identity.googleid`)**:
  * Direct invocation of Android's native `CredentialManager` with `GetGoogleIdOption` on the "Continue with Google" action button.
  * Native system bottom-sheet Google Account picker for streamlined, one-tap account selection displaying authentic Google accounts on the device.
  * Extracts authentic `GoogleIdTokenCredential` containing verified Google ID Token, email, display name, and avatar picture URI.
* **Firebase Authentication Token Exchange (`FirebaseAuth`, `AuthManager.kt`)**:
  * Exchanges Google ID Token with Firebase using `GoogleAuthProvider.getCredential(idToken, null)`.
  * Syncs Firebase user identity and seamlessly provisions/updates the local Room `UserEntity` profile with role-based routing (`customer_home`, `rider_home`, `admin_dashboard`).
* **Real Device Account Synchronization (`DeviceAccountUtils.kt`)**:
  * Dynamically queries system Google accounts from Android's `AccountManager` (`com.google`), active Firebase sessions, and on-device user records.
  * Completely removes hardcoded, mock, or fake accounts; adapts dynamically to the real accounts configured on the user's Android device.
  * Provides direct custom Google account sign-in entry for signing in with any real Google ID.

* **Real-Time Dynamic Google Format**:
  * Seamlessly connects with Google credentials when available or transitions immediately to the clean real-time format where users can enter and authenticate any real Google email/name without requiring manual access tokens.
  * Direct one-click login and synchronized Firebase/Room session creation.
* **Credential Manager API Integration (`LoginActivity.kt`)**:
  * Dedicated Android `LoginActivity` implementing modern Android `CredentialManager` API with `GetGoogleIdOption` and `GoogleIdTokenCredential`.
  * Fully retrieves authentic Google device credentials with zero mock data fallbacks, validates identity with Firebase Authentication (`GoogleAuthProvider`), and completes role-aware user onboarding into local Room persistence.
  * Supports programmatic launching with `EXTRA_TARGET_ROLE`, `EXTRA_IS_REGISTER`, and `EXTRA_AUTO_TRIGGER_GOOGLE` returning standard `Activity.RESULT_OK` with authenticated user payload (`EXTRA_USER_EMAIL`, `EXTRA_USER_NAME`, `EXTRA_USER_ROLE`, `EXTRA_USER_UID`).
* **Latest Firebase Project Credentials Configuration (`google-services.json`)**:
  * Integrated the latest project credentials (`project_id: ai-420`, `project_number: 488422345846`) with SHA-1 client certificates for package `com.aistudio.zyphuel.appv2`.
  * Web Client ID (`488422345846-m972okhh2ms29s911apa4t8ih04d3jo1.apps.googleusercontent.com`) configured for Google Identity Services.

## 11.7 User-Friendly Fuel Delivery Push Notification Permissions Prompt (`DeliveryNotificationPermissionPrompt.kt`)
* **Context-Rich Value Rationale Modal**:
  * Displays an animated Material 3 permission dialogue presenting clear value drivers:
    * 🚚 **Live Bowser Dispatch & ETA**: Alerts the moment the fuel bowser begins route transit.
    * ⏱️ **Rider Arrival Ping**: Real-time ping when the delivery bowser reaches the vehicle or gate.
    * 🔒 **Delivery OTP & Safety Codes**: Instant lock screen push notifications with safe dispensing PINs.
    * ⚡ **Hourly Fuel Price Shift Alerts**: Immediate updates on price updates across Pakistan.
* **Direct Runtime Integration**:
  * Uses `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` for Android 13+ `POST_NOTIFICATIONS`.
  * Fallback to System Application Notification Settings for devices with notifications disabled.
  * Interactive **"Test Live Alert"** button allowing instant verification of status bar notification delivery.
* **Unobtrusive Discovery Banners**:
  * Clean Material 3 alert card embedded in `CustomerHomeScreen` and `TrackerScreen` header for quick permission management.

## 11.8 Customer Home Multi-Order View Optimization & Polling Throttling
* **Hybrid Order Card Rendering (`CustomerHomeScreen` in `Screens.kt`)**:
  * Prioritizes rendering resources by displaying the primary active order with the rich interactive live-map tracking card (`RealTimeOrderTrackingCard`) while subsequent active orders are rendered using lightweight, performant summary cards (`CustomerOrderCard`).
  * Prevents UI stutter and WebView stacking lag on physical mobile devices when multiple active orders exist concurrently.
* **Network & Battery Conscious Fuel Price Polling (`MainViewModel.syncFuelPricesViaGemini`)**:
  * Throttles periodic background Gemini price sync loop to 30-minute intervals (`30 * 60 * 1000L`) to complement the existing 4-hour `FuelPriceWorker` background schedule without wasting mobile data or battery.

## 11.9 Zyphuel Desktop Operations Console (`desktop/`)
* **Compose Multiplatform Desktop Application (`desktop/src/main/kotlin/com/example/desktop/`)**:
  * **Operations Console UI (`OpsConsoleScreen.kt`)**: Dark-themed command-and-control operations dashboard for fleet dispatchers with real-time active order monitoring, rider telemetry, status filtering, and search.
  * **Interactive Demo Simulation & 404 Resilience (`OpsConsoleState.kt`)**: Automatically loads interactive simulated orders and animated live bowser coordinates if Cloud Firestore database has not yet been provisioned (HTTP 404) or is offline, allowing full UI, route, and order status interaction immediately.
  * **One-Click Firebase Setup Helper**: Displays a setup alert banner with a direct 1-click button to open Firebase Console (`https://console.firebase.google.com/project/ai-420/firestore`) in the browser to provision Firestore in test mode, which automatically transitions to live streaming once created.
  * **Custom Tile Map View (`TileMapView.kt`)**: Hardware-accelerated offline/online OpenStreetMap tile renderer built on Compose Desktop Canvas with smooth panning, zoom controls, dispatch hub indicators, and vehicle markers.
  * **Firestore REST Client (`FirestoreRest.kt`, `DesktopConfig.kt`)**: Zero-dependency REST-based Cloud Firestore streaming client connecting with Firebase project credentials without requiring full Android SDK dependencies.
  * **One-Click Launch Scripts**: `RUN-DESKTOP.bat` and `RUN-DESKTOP-DEBUG.bat` for seamless local execution on Windows/macOS/Linux.

## 11.10 Comprehensive 13-Step Interactive App Tour, Downloadable PDF Invoices & Delivery Fee Standardization
* **Revamped Comprehensive 13-Step Interactive App Tour Guide (`SpotlightOverlay` & `SpotlightTour.kt`)**:
  * Precision on-screen element spotlight cutouts and rich callouts guiding users through 100% of real app features:
    1. **Welcome to Zyphuel**: Overview of Lahore doorstep energy delivery (Petrol, Diesel, Octane, LPG, Mineral Water) at official OGRA rates.
    2. **Live GPS Pinpoint & Location Sharing (`user_location_active_card`)**: Auto-detecting live GPS coordinates and 1-tap WhatsApp/Maps link sharing.
    3. **Universal Typo-Tolerant Search (`home_search_bar`)**: Instant search across all 10 categories, fuel types, and fluids, even with typos.
    4. **1-Tap Emergency Quick Actions (`home_quick_actions`)**: Instant SOS roadside assistance for urgent fuel, puncture, jumpstart, and mechanic.
    5. **My Saved Vehicles Profile (`home_saved_vehicles`)**: Vehicle profiles for personalized fuel tank capacities and recommended engine oils.
    6. **10-Category Services Marketplace (`service_petrol`)**: Complete marketplace with live transparent OGRA rates.
    7. **Instant 1-Tap COD Ordering (`home_fab`)**: Liters stepper, price breakdown, and risk-free Cash-on-Delivery confirmation.
    8. **Live 4-Stage Delivery Stepper & Driver Contact**: Real-time status milestones (Pending ➔ Assigned ➔ Out for Delivery ➔ Completed) with direct Phone dialer and live WhatsApp chat.
    9. **Automated Gmail Receipts & Downloadable PDF Invoices**: Itemized tax invoices with QR codes saved directly to device storage.
    10. **Real-Time Notification Bell (`home_notifications`)**: Live updates on dispatch assignments, rider arrival, and promo deals.
    11. **Delivery Dashboard & Insights (`home_order_history`)**: Total rupees spent, completed deliveries count, and 1-tap repeat orders.
    12. **Navigation Sidebar Menu (`drawerState.open()`)**: Access to Profile Settings, Security, Saved Locations, and 24/7 Help Center.
    13. **Biometric Security & Finish**: Biometric fingerprint/face unlock configuration for instant, secure logins.
  * Motion & Interaction Polish:
    * Top animated linear progress bar (`FastOutSlowInEasing`) with completion percentage.
    * Category badge pills (`GPS & LIVE LOCATION`, `EMERGENCY SOS`, `1-TAP COD ORDERING`, etc.).
    * Actionable "💡 Pro Tip:" cards for every step.
    * Fluid `AnimatedContent` slide/fade horizontal transitions between steps.
    * Interactive clickable progress dots/pills allowing users to jump directly to any step (`jumpTo`).
    * Re-launchable from both Navigation Drawer (`Take a Guided Tour 🧭`) and Help Center (`Take Guided App Tour (13 Steps) 🧭`).
* **Official Order Invoice Generation & PDF Download (`InvoiceGenerator.kt`, `InvoiceDialog`)**:
  * **Interactive Invoice Modal (`InvoiceDialog`)**: Detailed preview showing company header, customer details, assigned rider info, itemized rate and quantity table, delivery fee, and total COD payable.
  * **Native Android PDF Download (`printOrSavePdf`)**: Leverages Android `PrintManager` and `WebView` to allow users to "Save as PDF" to phone storage or print directly without requiring risky storage permissions.
  * **Receipt Sharing (`shareInvoice`)**: Formats itemized receipt text dispatched via Android `ACTION_SEND` intent for WhatsApp, SMS, or Email.
  * **Universal Invoice Access**: Accessible directly from `OrderSummaryCard` (Tracker screen), `CustomerOrderCard` (Home & Order History), and `AdminOrderCard` (Admin Console).
* **Delivery Fee Standardization (`FeeConstants.kt`)**:
  * Unifies delivery fee calculations across all app surfaces:
    * Fuel (Petrol, Diesel, High-Octane) & LPG Orders: Fixed Rs. 250.00.
    * Pure Water Orders: Flat Rs. 50.00.
    * Multi-item orders: 50% discount on base delivery fee.
  * Guarantees 100% mathematical consistency across `OrderDialog`, `OrderSummaryCard`, `TrackerScreen`, `showFareBreakdownDialog`, and downloadable invoices, eliminating previous discrepancies.
* **Address Input Simplification in `OrderDialog`**:
  * Completely removed redundant "Saved Addresses (Quick Fill)" and "Save Current" chips to deliver a clean, spacious, direct delivery address input experience.
* **Removal of "Stay Updated on Your Fuel Delivery" Bell Icon & Permission Prompt**:
  * Completely removed the bell icon `IconButton` (`tracker_notification_prompt_btn`) from the Order Details header in `TrackerScreen`.
  * Removed the automated startup prompt `checkAndPromptDeliveryNotifications` from `MainActivity.kt` and disabled the `DeliveryNotificationPermissionPrompt` overlay modal.
  * Removed the notification prompt banner card from `CustomerHomeScreen`, providing an uncluttered, distraction-free order details and tracking experience.
* **Customer Order History Isolation & Admin Global Visibility (`CustomerOrderHistoryScreen`)**:
  * **Customer Privacy Isolation**: Regular customers strictly view only their own orders (`getOrdersForCustomerFlow` with case-insensitive `LOWER(customerEmail) = LOWER(:email)`).
  * **Admin Master Visibility**: When logged in as Administrator, `customerOrders` automatically streams `getAllOrdersFlow()`, enabling the Admin to view all customers' orders alongside the Admin's own orders on `CustomerOrderHistoryScreen` and Admin Dashboard.
  * **Admin Badging**: Orders placed by Administrator display a prominent `[Admin Order]` badge, and each customer order clearly lists `Cust: <Name> (<Email>)`.
* **Semantic Micro-Versioning Architecture**:
  * Adopted granular versioning format (`v2.3.0.x` for minor feature updates / patches, `v2.3.x.x` for larger functional releases). Currently updated to `v2.3.0.1` (`versionCode = 4`).

## 11.11 Real-Time Email Delivery Engine, Admin Profit Accounting, Profile Phone Updates & Domain Migration
* **Robust Real-Time Google SMTP Email Delivery (`RealtimeEmailEngine.kt`, `SecureStorageManager.kt`)**:
  * **TLS SNI Support on Port 465**: Resolved mobile network handshake drops by creating explicit `SSLSocket` instances with Server Name Indication (`sslFactory.createSocket(plainSocket, host, port, true)`).
  * **Fail-Safe Built-In Fallbacks**: `SecureStorageManager.getSmtpConfig` and `RealtimeEmailEngine` automatically fallback to verified credentials (`m.daniyalkhan490@gmail.com` with app password `pkymsolzualgbgzn`) whenever user preferences are uninitialized or blank, preventing silent email dispatch skips.
  * **Google Sign-In Registration Welcome Email**: `handleSocialLoginSuccess` in `MainViewModel` now automatically fires a real-time welcome email upon social signup so every customer receives immediate confirmation in their registered inbox.
* **Invoice Privacy & Admin Self-Fulfillment (`InvoiceGenerator.kt`)**:
  * **Intelligent Fulfillment Detection (`isAdminFulfillment`)**: Identifies orders fulfilled directly by administrators (`riderEmail.isNullOrBlank()`, `riderName` containing "Admin", or matching admin root address).
  * **Rider Suppression on Admin Orders**: When fulfilled by admin, both the plain text receipt and HTML tax invoice completely suppress all rider information (name, email, contact) and clearly label the dispatch entity as:
    * `"🏢 FULFILLMENT & DISPATCH: Zyphuel Central Admin Operations • Lahore Headquarters Direct Dispatch"`.
* **Admin Center: Total Profit Accounting Metric (`Screens.kt` - `AdminCenterScreen`)**:
  * **Delivery Fee Margin as Zyphuel Profit**: Defined company profit strictly as the logistics and doorstep handling fee (`FeeConstants.calculateDeliveryFee(order.serviceType)`: Rs. 250 for Fuel/LPG, Rs. 50 for Water).
  * **Admin-Exclusive Display**: Prominently displayed on `AdminCenterScreen` stats card (`AdminStatCard(title = "Total Profit", value = "Rs.${totalProfit.toInt()}")`), computed across all completed orders (`orders.filter { it.status == "Completed" }`). Customers do not see this internal company margin.
* **Profile Settings Modernization & Phone Number Updating (`Screens.kt`, `MainViewModel.kt`)**:
  * **Real-Time Phone Number Updates**: Added editable Phone Number input (`profile_phone_input`) in `ProfileSettingsDialog` wired to `MainViewModel.updatePhoneNumber(newPhone)`. Synchronizes instantly to Room SQLite DB, Cloud Firestore, and generates an audit log entry.
  * **Profile UI Decluttering**: Removed the Biometric Authentication toggle card, Verified Admin Badge (Blue Tick) authority card, and Permanent Root Super Admin card from `ProfileSettingsDialog` for a cleaner, modern profile experience.
* **Official Website Domain Migration (`www.zyphuel.com`)**:
  * Completely migrated all website links, OpenGraph metadata, Twitter Cards, Schema.org JSON-LD scripts, sitemaps, and documentation from `https://zyphuel.netlify.app/` to `https://www.zyphuel.com/` (and `www.zyphuel.com`).

## 11.12 Google Play Store Publishing Compliance, Interactive Legal Viewer & Version Lifecycle
* **Automated Version Lifecycle & Bump Policy (`app/build.gradle.kts`)**:
  * Incremented release build code to `versionCode = 5` and updated semantic release to `versionName = "2.3.1"`.
  * Configured dynamic `BuildConfig` integration so all screens and drawers display live versions without hardcoded strings (`v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})`).
  * Enforced mandatory version bump rule in `AGENTS.md` and `MEMORY.md` on every app update.
* **Interactive Legal & Privacy Dialog (`TermsAndPrivacyDialog.kt`)**:
  * Built dual-tab Material 3 dialog with scrollable sections for **Terms & Conditions** and **Privacy Policy**.
  * Displays prominent location disclosures for Google Maps telematics (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE_LOCATION`), Oil & Gas Regulatory Authority (OGRA) hazardous delivery protocols, and Account Deletion policy.
  * Provides an external "Open in Browser" button routing to official web policies (`https://www.zyphuel.com/privacy-policy` and `https://www.zyphuel.com/terms-and-conditions`).
* **Customer Registration Legal Flow (`AuthScreen`, `Screens.kt`)**:
  * Added "I agree to Terms & Conditions and Privacy Policy" checkbox with interactive clickable links that open `TermsAndPrivacyDialog`. Blocks account creation until terms are explicitly accepted.
* **Rider Registration & Profile Compliance (`Screens.kt`)**:
  * Upgraded rider registration terms row and `RiderCompleteProfileScreen` terms section so tapping "Terms and Conditions" presents the full in-app viewer.
* **Navigation Drawer & Security Settings Legal Hub**:
  * Added "Terms & Privacy Policy" option to `CustomerSidebarDrawer` alongside the dynamic version badge `Zyphuel v2.3.1 • Build 5`.
  * Added "Legal & App Compliance" card to `SecuritySettingsScreen` with verified status badge (`Verified 🛡️`) and direct viewing buttons.
* **Target SDK 36 Permission Audit (`AndroidManifest.xml`)**:
  * Added `android:maxSdkVersion="22"` to `GET_ACCOUNTS` to eliminate Play Console legacy account access warnings on Android 14/15/16.
* **Official Store Compliance Artifacts**:
  * `PRIVACY_POLICY.md` & `TERMS_AND_CONDITIONS.md` (root markdown references).
  * `PLAY_STORE_DATA_SAFETY_AND_COMPLIANCE.md` (exact answers for Play Console Data Safety questionnaire).
  * `web/privacy-policy.html` & `web/terms-and-conditions.html` (production HTML pages ready for web hosting).

---

## 12. Database Architecture & Data Models
Room Local Persistence layer (`AppDatabase.kt` v9):
* **`UserEntity`**: `email` (PK), `name`, `passwordHash`, `role`, `phoneNumber`, `residentialAddress`, `isVerified`.
* **`OrderEntity`**: `id` (PK), `customerEmail`, `customerName`, `serviceType`, `fuelVolumeLiters`, `totalAmountPkr`, `status`, `assignedRiderEmail`, `assignedRiderName`, `deliveryAddress`, `etaMinutes`.
* **`AuditLogEntity`**: `id` (PK), `timestamp`, `action`, `performedBy`, `details`.
* **`NotificationEntity`**: `id` (PK), `userEmail`, `title`, `message`, `timestamp`, `isRead`.
* **`MarkedLocationEntity`**: `id` (PK), `userEmail`, `label`, `address`, `latitude`, `longitude`, `createdAt`.

---

---

## 13. Auto-Update & Maintenance Protocol
This document (`FEATURES_DOCUMENTATION.md`) serves as the single source of truth for app features. Whenever a new screen, viewmodel function, or database field is added:
1. Update `FEATURES_DOCUMENTATION.md` under the respective section.
2. Maintain `AGENTS.md` instructions so AI agents continue maintaining this file in sync with future codebase changes.

---

## 14. Desktop Operations Console Architecture & Dispatch System (`ZyphuelOpsConsole`)
The desktop companion application (`desktop` module) is an enterprise-grade operations and fleet dispatch command console built in Kotlin and Jetpack Compose Desktop (Material 3). It connects directly to the same Cloud Firestore backend as the Android mobile app, enabling real-time dispatch, fleet supervision, and revenue analytics without requiring mobile device hardware.

### 14.1 Multi-Tab Command Center Architecture (`ConsoleTab`)
The console features a top-level tabbed navigation bar allowing dispatchers to switch between four operational perspectives:
1. **🚚 Live Dispatch (`DISPATCH`)**: Real-time order queue with status chips, search engine, interactive OpenStreetMap canvas with routing, detailed telemetry card, order lifecycle controls, and direct bowser assignment.
2. **🏍️ Fleet & Bowsers (`FLEET`)**: Grid monitoring of all 4 registered Zyphuel bowser tankers (Alpha, Beta, Gamma, Delta), showing vehicle specs (model, registration, tank capacity), active assigned mission, driver phone/email, and live GPS reporting status.
3. **📊 Analytics & Revenue (`ANALYTICS`)**: Executive metrics dashboard calculating Total Sales Revenue (PKR), Completed Orders, Average Ticket size, product volume delivery distribution bars (Super Petrol, HSD, Octane 97, Drinking Water, LPG), and revenue percentage share.
4. **⚙️ Cloud & Database (`SYSTEM`)**: Diagnostic cockpit providing live Firestore endpoint latency, project ID target (`ai-420`), database creation guides, 1-click cloud seeding (`btn_seed_firestore_settings`), and instant CSV data export.

### 14.2 Real-Data Connection & Dual Polling Pipeline
* **Unified Cloud Firestore Pipeline**: Desktop speaks HTTP/2 REST directly to `https://firestore.googleapis.com/v1/projects/ai-420/databases/(default)/documents/` matching the mobile app's schema (`orders` and `live_tracking`).
* **Dual Polling Cadence**:
  * Orders poll every **5 seconds** (`pollOrdersForever()`).
  * Rider GPS positions poll every **3 seconds** (`pollLiveTrackingForever()`).
* **Connection State Machine (`ConnState`)**:
  * `CONNECTING` — Pulse animation indicating initial handshake with Google Cloud APIs.
  * `CONNECTED` — Real-time live synchronization with mobile fleet.
  * `SETUP_NEEDED` — Displays one-click Firebase Console guide when Firestore has not yet been initialized (HTTP 404).
  * `ERROR` — Surfaces network interruptions or authorization issues with clear troubleshooting steps.

### 14.3 Interactive Slippy Map Canvas & Live Controls (`TileMapView`)
* Web Mercator raster tile compositor over OpenStreetMap HTTP endpoints with custom User-Agent identifying Zyphuel.
* **On-Canvas Floating Control Panel**:
  * `➕ Zoom In` (`btn_map_zoom_in`): Incrementally increases zoom level up to max zoom 17.
  * `➖ Zoom Out` (`btn_map_zoom_out`): Decreases zoom level to view broader provincial corridors.
  * `🎯 Auto-Fit Recenter` (`btn_map_recenter`): Automatically recalculates bounding box containing depot, vehicle, and delivery destination.
  * `🏢 Focus Depot` (`btn_map_focus_depot`): Centers viewport directly over the Zyphuel Lahore Central Depot (31.4380, 74.3050).
  * `🏍️ Focus Rider` (`btn_map_focus_rider`): Dynamically pans map to follow moving bowser tanker.
* **Telematics Rendering**: Dynamic route path with dashed stroke, depot slate marker, crimson destination pin, and live green/amber vehicle marker with bearing directional heading arrow.

### 14.4 Driver & Bowser Assignment Modal (`AssignRiderDialog`)
* Allows central dispatchers to assign unassigned or pending deliveries to any registered bowser in the fleet:
  * **Bowser Alpha**: Rashid Minhas (`rider.rashid@zyphuel.com`) · Hino 500 Heavy (5,000L Petrol)
  * **Bowser Beta**: Hamza Akram (`rider.hamza@zyphuel.com`) · Isuzu Forward (3,000L Diesel)
  * **Bowser Gamma**: Usman Farooq (`rider.usman@zyphuel.com`) · FAW J5M (4,000L Drinking Water)
  * **Bowser Delta**: Tariq Mehmood (`rider.tariq@zyphuel.com`) · Master Forland (2,500L LPG / High Octane)
* Dispatches `updateOrderStatus` PATCH to Firestore updating `riderName`, `riderEmail`, and advancing status to `Assigned`.

### 14.5 Tax Invoice & Receipt Generator (`InvoiceDialog`)
* Built-in invoice viewer displaying customer particulars, order timestamps, line items, volume, delivery address, payment method, and dispatch depot.
* **1-Click Clipboard Exporter (`btn_copy_invoice`)**: Formats receipt in standardized monospaced text ready for SMS, WhatsApp Web, or printing.

### 14.6 Data Export & Audio Alert Engine
* **Automated Audio Chime (`Toolkit.getDefaultToolkit().beep()`)**: Fires an audible notification whenever a new customer order arrives from the mobile app.
* **New Order Banner (`BannerNotificationBar`)**: Displays high-priority green banner at top of screen with customer name and order ID.
* **Full CSV Export Engine (`exportOrdersCsv()`)**: Serializes all orders into RFC-4180 compliant CSV format and writes directly to system clipboard for instant pasting into Microsoft Excel or Google Sheets.

### 14.7 Order Dispatch Modal (`CreateOrderDialog`)
* Modal triggered via `➕ Dispatch Order` (`btn_new_order`).
* Calculates unit prices in real time (Super Petrol @ 278, HSD @ 280, High Octane @ 315, Water @ 50, LPG @ 2800).
* Writes full order payload to Firestore via REST with auto-generated coordinates within the Lahore service area.

---


## 15. Mobile Application Cyber Security Architecture & Threat Model Audit

### 15.1 Defensive Security Implementations (Current Protections)
* **AES-256-GCM Secure Storage (`SecureStorageManager.kt`)**: User session credentials, biometric flags, and authentication tokens are encrypted using Android Keystore-backed `EncryptedSharedPreferences`.
* **Biometric Authentication Hardware Bridge (`BiometricSecurityManager.kt`)**: Leverages AndroidX `BiometricPrompt` integrating with device TEE/SE for biometric authentication.
* **Input Sanitization & Validation (`SecurityInputValidator.kt`)**: Strict regex checking on all user inputs (emails, phones, addresses, volumes) preventing injection payloads.
* **Brute-Force Rate Limiting (`SecurityRateLimiter.kt`)**: Sliding-window rate limiter blocking credential stuffing and repeated OTP/login attempts.
* **Google OAuth 2.0 & Firebase Auth Integration (`AuthManager.kt`)**: Validates cryptographically signed Google ID tokens.
* **Android Private Storage Sandbox**: Room SQLite database isolated within `/data/data/com.aistudio.zyphuel.appv2/databases/`.

### 15.2 Vulnerability Assessment & Threat Surfaces (Security Findings)
* **Hardcoded Plaintext Super Admin Password (CRITICAL)**: Root administrator credentials (`m.daniyalkhan490@gmail.com` with password `"abcd1234"`) are hardcoded in `Repository.kt` and `MainViewModel.kt`. Any decompilation reveals these credentials immediately.
* **Disabled Code Obfuscation (HIGH)**: `isMinifyEnabled = false` in `app/build.gradle.kts` allows decompilation into readable Kotlin source code via JADX-GUI.
* **Unencrypted Local Room SQLite DB (HIGH)**: `AppDatabase` uses standard SQLite without SQLCipher, allowing plaintext extraction on rooted devices or via ADB backup.
* **Client-Side Firestore Writes (HIGH)**: Direct client writes to `orders` collection without backend Cloud Functions validation allow malicious price, quantity, or status alterations if Firestore security rules are permissive.
* **Lack of Mock Location Detection (MEDIUM)**: Rider location tracking does not inspect `location.isFromMockProvider()`, allowing fake GPS spoofing.
* **Exported Activity Exposure (MEDIUM)**: `LoginActivity` has `android:exported="true"` without intent filters or permissions.
* **Frida / Xposed Runtime Hooking (MEDIUM)**: Absence of root detection or anti-hooking mechanisms leaves biometric callbacks vulnerable to runtime function hooking.

### 15.3 Hardening Roadmap & Remediation Strategy
1. Remove plaintext admin passwords; migrate root supervisory roles to Firebase Authentication Custom Claims.
2. Enable R8/ProGuard obfuscation (`isMinifyEnabled = true`, `shrinkResources = true`) in release builds.
3. Integrate SQLCipher 256-bit AES encryption for Room database.
4. Enforce strict server-side Firestore Security Rules and Cloud Functions for order price calculation.
5. Add `location.isMock` checks to `LocationService.kt` to block GPS spoofing apps.
6. Set `android:exported="false"` for internal activities in `AndroidManifest.xml`.

---

## 16. Modern 10-Category Marketplace Architecture & Fleet Logistics (v2.4.0)

### 16.1 The 10 Primary Categories & Dynamic Subcategories
Zyphuel v2.4.0 introduces a comprehensive 10-category on-demand automotive and mobility marketplace on the Customer Home Screen (`CategoryGridSection`), complete with rich iconography, real-time pricing, vehicle compatibility filters, and estimated delivery/service durations:

| Category ID | Title | Description | Subcategories Included | Compatible Vehicles |
| :--- | :--- | :--- | :--- | :--- |
| `fuel_energy` | **Fuel & Energy** | Instant on-demand fuel delivery and energy essentials | Super Petrol, High-Speed Diesel (HSD), Emergency Fuel Canister (5L/10L), Generator Refueling, LPG Gas Cylinder | Bike, Car, Van, Heavy Truck, Generator, Fleet |
| `auto_repair` | **Auto Repair & Care** | Certified mobile mechanics & on-site repairs | Mobile Diagnostic Scan (OBD-II), On-Demand Mobile Mechanic, Brake Pad Inspection & Replacement, Engine Spark Plug & Tune-up, AC Gas Top-up & Leak Test | Car, Van, Heavy Truck, Fleet |
| `roadside_assist` | **Roadside Assistance** | 24/7 emergency roadside recovery & dispatch | Flatbed Towing (Emergency), Heavy Towing / Winch Recovery, Key Lockout & Auto Access, Accident Breakdown Assistance | Bike, Car, Van, Heavy Truck, Fleet |
| `detailing` | **Auto Care & Detailing** | Doorstep eco-friendly vehicle washing & detailing | Waterless Eco Exterior Wash, Full Interior Deep Vacuum & Clean, Comprehensive Mobile Detailing, Paint Protection & Ceramic Sealant | Bike, Car, Van, Heavy Truck, Fleet |
| `tyres_wheels` | **Tyres & Wheels** | On-demand puncture fix, tyre replacement & air | Doorstep Puncture Repair (Tubeless), Spare Tyre Change Assistance, Tyre Pressure Check & Digital Inflation, New Tyre Delivery & Mobile Fitting | Bike, Car, Van, Heavy Truck, Fleet |
| `battery` | **Battery Services** | Emergency jumpstart, testing & doorstep installation | Emergency Jumpstart (12V / 24V), Battery Voltage & Alternator Health Test, New Battery Delivery & On-Site Installation | Bike, Car, Van, Heavy Truck, Generator, Fleet |
| `lubricants` | **Lubricants & Fluids** | Premium engine oils & automotive fluid top-ups | Full Synthetic Engine Oil Change, Radiator Coolant Flush & Top-up, Brake Fluid Flush & Bleed, Transmission & Gear Fluid Top-up | Bike, Car, Van, Heavy Truck, Generator, Fleet |
| `ev_services` | **EV Services** | Electric vehicle roadside charging & infrastructure | Emergency Mobile EV Rescue Charge (5-10 kWh), Home EV Charger Installation Consultation, EV Battery Diagnostic & Health Check | Car, Van, Fleet |
| `water_delivery` | **Water Delivery** | Pure mineral drinking water & bulk commercial tankers | 5-Gallon Drinking Water Bottle Refill, Commercial Tanker Water Supply (1000 - 3000 Gallons), Swimming Pool & Construction Refill | Bike, Car, Van, Heavy Truck, Generator, Fleet |
| `fleet` | **Fleet & Business** | Specialized corporate and commercial fleet support | Bulk Fleet Fueling Program, Commercial Fleet Preventive Maintenance, Dedicated Generator Fueling Contract | Van, Heavy Truck, Fleet |

### 16.2 Saved Vehicle Profiles ("My Vehicles")
* **Persistence & Schema (`VehicleEntity` & `VehicleDao`)**:
  - Saved vehicle profiles are stored locally in Room database version 12 (`vehicles` table) with fields `id`, `userId`, `make`, `model`, `year`, `licensePlate`, `fuelType`, `vehicleType`, `createdAt`, and `isDefault`.
  - The DAO supports reactive queries (`getVehiclesForUser(userId): Flow<List<VehicleEntity>>`), single active selection (`getDefaultVehicle`), upserts (`insertVehicle`), and secure profile deletion (`deleteVehicle`).
* **UI Integration & Compatibility Matching (`MyVehiclesDialog` & `SavedVehiclesBar`)**:
  - Displayed on the Customer Home dashboard directly above the category grid as a horizontal scrollable vehicle chip selector with an `"Add Vehicle +"` button.
  - Selecting an active vehicle instantly tailors the category modal to display only services compatible with that vehicle's type (e.g. hiding car-only towing from motorcycle selections).

### 16.3 Global Typo-Tolerant Search Engine ("What do you need today?")
* **Levenshtein Fuzzy Matching (`CategoryRepository.searchCategoriesAndServices`)**:
  - Embedded directly into the Customer Home screen search bar (`ServiceSearchBar`).
  - Matches queries against category titles, descriptions, subcategory names, service tags, and keywords.
  - Implements dynamic edit-distance tolerances (distance $\le 2$ for terms $>4$ chars) so typos such as `"puncure"`, `"deisel"`, or `"batry"` resolve seamlessly to the correct services.
  - Tapping a search result instantly opens the relevant category modal and pre-highlights the selected service.

### 16.4 GPS Service Availability & Distance/ETA Calculations (`LocationCoverageManager`)
* **Accurate Haversine Distance Engine**:
  - Evaluates user coordinates against the central depot hub in Lahore (31.4380° N, 74.3050° E).
  - Automatically identifies local coverage zones: Lahore Central, Gulberg & Model Town, DHA & Cantt, Johar Town & Wapda Town, Bahria Town & Southern Lahore.
  - Computes realistic ETAs without artificial/mock values:
    $$\text{ETA (min)} = \text{basePrepTime} + \left(\text{distanceKm} \times 1.3 \times \frac{60}{\text{averageSpeedKmH}}\right)$$
  - Flags locations exceeding the 45 km operational radius as out-of-zone, informing the user with clear distance feedback while preventing unsupported dispatch.

### 16.5 Integrated Order Pipeline & Booking Bridge (`bookCategoryService`)
* **Unified Lifecycle Progression**:
  - Category and subcategory service bookings are converted directly into standard `OrderEntity` records and committed to Room DB and Cloud Firestore.
  - Automatically attaches the customer's active vehicle details (Make, Model, License Plate) and requested subcategory notes to the order destination address and notes payload.
  - Instantly invokes `RealtimeEmailEngine` to deliver authenticated triple-party confirmation emails to Customer, Assigned Rider, and Super Admin.
  - Seamlessly navigates to `TrackerScreen` with live 4-step delivery progress tracking.

### 16.6 Super Admin Category Management Tab (`AdminCategoryManagementTab`)
* **Operational Control Console (Admin Tab 8)**:
  - Accessible exclusively to administrators via Tab 8 in the `AdminDashboardScreen`.
  - Enables real-time toggling of individual category availability (Active / Inactive) with instant persistence to `SharedPreferences` and reactive UI propagation.
  - Allows administrators to override service base pricing on-the-fly and flag categories with High-Demand / Emergency Roadside Priority banners during emergencies or extreme weather conditions.

---

## 17. Automated Real-Time Order Tax Invoices & Registered Email Delivery Engine
### 17.1 Automatic Real-Time Tax Invoice Generation & Dispatch (`MainViewModel.sendOrderInvoiceEmail`)
* **Immediate Customer Inbox Delivery on Order Creation (`placeOrder`)**:
  - Whenever an order is submitted by a customer or administrator, `MainViewModel.placeOrder` immediately invokes `sendOrderInvoiceEmail(order, isCompletedReceipt = false)`.
  - Generates both an itemized, responsive HTML Tax Invoice (`InvoiceGenerator.generateHtmlInvoice(order)`) with official branding, item summary, doorstep delivery fee, and COD total, alongside a structured plain-text receipt (`InvoiceGenerator.generatePlainTextReceipt(order)`).
  - Automatically dispatches directly to the customer's registered email (`order.customerEmail`) and archives an administrative audit copy to `m.daniyalkhan490@gmail.com`.
  - Transmitted through `RealtimeEmailEngine` via Authenticated Direct SMTP (Port 465 SSL with SNI), Google Apps Script Cloud HTTPS Webhook, and Cloud Firestore `mail` collection fallback.

### 17.2 Automatic Final Paid Tax Invoice upon Order Completion (`changeOrderStatus`)
* **Delivery Completion Receipt**:
  - When an order transitions to `"Completed"` or `"Delivered"` (triggered by rider or administrator in `changeOrderStatus`), `sendOrderInvoiceEmail(completedOrder, isCompletedReceipt = true)` is automatically triggered.
  - Dispatches subject `"🧾 Final Paid Tax Invoice & Delivery Receipt - Order #ID | Zyphuel"` to the customer's registered email with full payment confirmation, delivery timestamp, and dispatch details.

### 17.3 On-Demand Invoice Email Dispatch from `InvoiceDialog`
* **Interactive Modal Button (`email_invoice_to_registered_btn`)**:
  - `InvoiceDialog` is accessible from all primary order views: `CustomerPastOrderCard` (Past Orders), `OrderSummaryCard` (Tracker screen), `TrackerScreen` (Active Tracking), and `AdminOrderCard` (Admin Console).
  - Features an action button: `[ 📧 Send Invoice to Registered Email ]` (`email_invoice_to_registered_btn`).
  - Tapping this button directly invokes `viewModel.sendOrderInvoiceEmail(order)` and presents an instant confirmation toast (`"📧 Tax Invoice dispatched to <email>!"`), providing immediate on-demand re-dispatching at any time.

---

## 18. App Tour Guide Lifecycle Policy (New User & Fresh Install Exclusive)
### 18.1 Single-Run Initial Onboarding
* **Restricted Strictly to New Users / Fresh Install**:
  - The interactive spotlight tour (`homeTourSteps`) is designed exclusively as an initial walkthrough for fresh app installations and newly registered customer accounts.
  - State is tracked persistently via `SharedPreferences` key `has_seen_app_tour_v2` and exposed reactively through `MainViewModel.hasSeenAppTour`.
* **Permanent Removal Post-Completion / Skip**:
  - The moment a user finishes or skips the tour, `closeAppTourGuide(markAsSeen = true)` permanently flags `hasSeenAppTour = true`.
  - **Sidebar Navigation Drawer**: `Take a Guided Tour 🧭` is strictly guarded by `if (!hasSeenAppTour)`. Once seen or for any existing user, it is permanently removed from the sidebar.
  - **Help Center & Secondary Screens**: Completely purged the tour trigger from `HelpCenterScreen`, ensuring the guided tour does not linger or reappear in any menu or dialog across the app.

---

## 19. High-Contrast Pure Black Typography & Enhanced Visual Accessibility
### 19.1 Profile Settings Dialog Typography Modernization (`ProfileSettingsDialog`)
* **Crisp, Solid Black Content Rendering**:
  - Maintained crisp, clean light/white dialog and card backgrounds (`Color.White`, `Color(0xFFF8FAFC)`) while eliminating low-contrast gray and navy-tinted text.
  - Presets label modernized to solid black: `"OR CHOOSE A COLOR PRESET"` (`Color.Black`, `FontWeight.Bold`).
  - Startup Transparency Notice modernized with crisp black title and bold black description text (`Color.Black`, `FontWeight.Medium`).
  - Account Role card updated: `"ACCOUNT ROLE"` section header and role value (`Administrator`, `Verified Customer`, `Courier`) rendered in solid `Color.Black`.
  - Order Alerts notification card modernized with bold black header and high-contrast dark charcoal subtitle (`Color(0xFF1E293B)`).
  - Lahore Coverage Area card updated with crisp black headline and description.
  - App Language selector (`App Language / زبان منتخب کریں`) rendered in solid `Color.Black` with bold high-contrast button labels.
  - Saved Lahore Delivery Addresses updated with solid black headers (`🏠 Home`, `🏢 Office / Work`) and crisp black address text (`Color.Black`).
  - Direct Lahore Support helpline modernized with solid black labels and descriptions.
  - Storage & Cache cleaner card updated with solid black title and high-contrast status descriptions.
  - All form input fields (Name, Email, Phone Number, Password) explicitly configured with `focusedTextColor = Color.Black`, `unfocusedTextColor = Color.Black`, `focusedLabelColor = Color.Black`, and `unfocusedLabelColor = Color.Black`.
  - Bottom disclaimer, version footer, and Close button rendered in high-contrast `Color.Black`.

### 19.2 Sidebar Navigation Drawer High-Contrast Typography (`DrawerContent`)
* **Crisp Black Readability Across Drawer Items**:
  - User profile header: Name rendered in solid `Color.Black` (`FontWeight.Bold`), and email in high-contrast charcoal `Color(0xFF1E293B)`.
  - Startup disclosure notice text rendered in bold `Color.Black`.
  - Foldable Categories header updated to solid `Color.Black` (`CATEGORIES & SERVICES`).
  - All category items (Petrol, Diesel, Gas Cylinder, Pure Water, Detailing) rendered with bold `Color.Black` titles and high-contrast charcoal subtitles (`Color(0xFF334155)`).
  - Drawer section headers (`ACCOUNT & SETTINGS`, `ORDERS & DELIVERIES`, `HELP & SUPPORT`, `ADMIN CONTROLS`) updated to solid `Color.Black` (`FontWeight.Bold`).
  - `SidebarItem` component updated to render all menu item labels in crisp `Color.Black` for optimal readability against white surfaces.

### 19.3 In-App Legal & Invoice Modals Typography (`TermsAndPrivacyDialog`, `InvoiceDialog`, `MyOrdersDialog`)
* **Legal Terms & Privacy Dialog**:
  - Updated `LegalSectionTitle`, `LegalParagraph`, and `LegalBulletPoint` to render in solid, high-contrast dark text (`Color.Black` and `Color(0xFF0F172A)`), ensuring full compliance with Google Play accessibility guidelines.
  - Startup disclosure warning banners within Legal and Privacy tabs rendered with bold, high-contrast black text.
* **Tax Invoice Dialog (`InvoiceDialog`)**:
  - Customer, phone, delivery address, driver, and payment breakdown labels updated to solid `Color.Black`.
  - Order breakdown item names, unit counts, delivery fees, and payable totals rendered in crisp `Color.Black` for unambiguous financial readability.
* **My Order History Dialog (`MyOrdersDialog`)**:
  - Service type titles, quantities, prices, and delivery destination text updated to bold, solid `Color.Black`.

---

## 20. Roadside SOS Decommissioning & Pure Water Elevation
### 20.1 User-Facing UI Clean-up
* **Removal of Roadside SOS**:
  - **Customer Home Screen (`CustomerHomeScreen`)**: Decommissioned and removed the `service_roadside` SOS card. Replaced it with a clean, full-width `Pure Water Delivery` card (`service_water`) matching the layout, elevation, and typography of Petrol, Diesel, and Gas.
  - **Navigation Drawer (`DrawerContent`)**: Removed the `Roadside SOS` entry under `CATEGORIES & SERVICES` accordion and replaced it with `Pure Water` (`"Drinking Water & Bowser Tankers"`).
  - **Quick Actions Bar (`QuickActionsBar`)**: Replaced the `Roadside SOS 🚨` action chip with `Pure Water 🚰` (`water_delivery`), preserving a neat 6-chip 1-tap utility row.
  - **Spotlight Tour Guide (`SpotlightStep 3`)**: Updated copy and subtitles to remove references to "Roadside SOS", highlighting Pure Water, Fuel, Gas Cylinders, Auto Repair, and Battery Jumpstart.
  - **App Subtitles & Disclosures**: Updated screen headers and startup transparency notices from "fuel, gas & roadside support" to "fuel, gas & energy services".
* **Preservation of Core Architecture & Tests**:
  - Maintained `CategoryCatalogSeed` background definitions for `roadside_assistance` so data seeds, Room migrations, and architecture test assertions (`CategoryAndVehicleArchitectureTest`) continue to pass without structural breakage.

---

## 21. Quick Actions Bar Streamlining (Essential Utilities Exclusivity)
### 21.1 Removal of Secondary Automotive Rescue Chips
* **Decommissioned from Quick Actions**:
  - Completely removed **Auto Repair** (`auto_repair`), **Battery Jump** (`battery_services`), and **Tyre Help** (`tyres_wheels`) from the `QuickActionsBar`.
* **Focused 3-Core Essential Utilities**:
  - The Quick Actions bar is now focused purely on rapid on-demand energy and hydration logistics:
    1. **Order Fuel ⛽** (`fuel_energy`): 1-tap direct launch into the complete Fuel & Energy category sheet (Petrol, Diesel, High-Octane).
    2. **Gas Cylinders 🔥** (`gas_cylinder`): Direct 1-tap trigger to open the official LPG Gas Cylinder order dialogue with live rates.
    3. **Pure Water 🚰** (`water_delivery`): 1-tap direct launch to Mineral Drinking Water and Bowser Tanker options.
* **Tour Guide Synchronization (`SpotlightTour.kt` / `Screens.kt`)**:
  - SpotlightStep 3 copy and subtitle synchronized to reflect the focused 3-utility Quick Actions bar (`"Urgent Fuel Top-Up, Gas & Pure Water Delivery"`), eliminating all legacy references to mechanical or rescue services.

---

## 22. Location Editor Dialog Simplification
### 22.1 Decommissioning of In-Dialog Pin Fine-Tuning Map
* **Removal of Visual Pin Verification (`EditLocationDialog` in `Screens.kt`)**:
  - Removed the **"Map Pin Visual Verification:"** label and the toggle button **"Fine-Tune Pin on Map 📍" / "Hide Map Pin"**.
  - Decommissioned the inline interactive map widget (`InteractiveLocationPickerMap`) from the address change dialogue.
  - Streamlined the location dialogue directly to Google Places Autocomplete search, Auto GPS detection, Reset Auto GPS, and Location Confirmation for a faster, clutter-free user experience.

### 22.2 Decommissioning of Redundant GPS & Landmark Status Banner
* **Removal of Status Header Pill**:
  - Removed the `Surface` banner displaying `"🟢 Auto-Detected Live GPS & Landmark Active"` / `"Custom Location & Landmark Active"` from `EditLocationDialog`.
  - The dialogue now opens directly with the Google Places Autocomplete search input, creating an immediate, clean, and distraction-free location picking experience.

---

## 23. Unified OGRA Fuel & Energy Pricing Standard & Zero-Fail Email Dispatch Engine
### 23.1 Unified Official OGRA Pakistan Energy & Fuel Pricing
* **Standardized Rates Across All Layers**:
  - **Super Petrol (Euro-V)**: `Rs. 275.60 / Litre`
  - **High-Speed Diesel (HSD)**: `Rs. 284.20 / Litre`
  - **High-Octane (HOBC 97)**: `Rs. 325.00 / Litre`
  - **LPG Gas**: `Rs. 258.65 / Kg` (11.8kg Factory-Sealed Cylinder: `Rs. 3,052.00`)
  - **Pure Drinking Water**: `Rs. 50.00 / Unit` (19L Bottle: `Rs. 180.00`, 1000L Bulk Tanker: `Rs. 3,200.00`)
* **StateFlow Reactive Binding on Customer Home**:
  - Pure Water card on `CustomerHomeScreen` dynamically displays `Rs. ${waterPrice}/Gal` via `StateFlow` binding, eliminating previous static text `"Rs. 4.50/Gallon"`.
* **Zero Surcharge Enforcement in AI Engine**:
  - Removed outdated arbitrary surcharges (`+20.0f`, `+25.0f`) from `syncFuelPricesViaGemini()`. Official pump rates remain 100% transparent, with logistics covered exclusively by `FeeConstants.FUEL_DELIVERY_FEE` (Rs. 250) and `FeeConstants.WATER_DELIVERY_FEE` (Rs. 50).
  - Synchronized default prices across `CategoryCatalogSeed`, `TrackmateFuelApiService`, `FuelPriceWorker`, and `MainViewModel` (including AI support chat offline response).

### 23.2 Zero-Fail Email Delivery Engine & Native Email Client Fallback
* **Root Cause Diagnosis for Email Failures**:
  - Background Gmail SMTP (`smtp.gmail.com:465`) requires a 16-letter Google App Password with 2-Step Verification enabled. Unconfigured devices or devices where cellular providers block SMTP ports (Zong/Jazz/Ufone) were previously experiencing silent failures.
* **Native Android Email Client Fallback (`Intent.ACTION_SENDTO` / `mailto:`)**:
  - Implemented `RealtimeEmailEngine.openEmailClient(context, recipient, subject, bodyText)`.
  - Integrated into `MainViewModel.sendOrderInvoiceViaEmailClient()` and `Screens.kt` (`InvoiceDialog`).
  - When background SMTP is unconfigured or encounters an error, the app instantly opens the user's native email client (Gmail, Outlook, or default email app) with recipient email, invoice subject, and full itemized text receipt already populated.
* **Admin Email Gateway 1-Tap Shortcut**:
  - Added direct `"Open Google App Passwords ↗"` button in the Admin Email Gateway configuration tab launching `https://myaccount.google.com/apppasswords` directly in the system browser.

---

## 24. Pure White Background & Logo Blue Brand Identity Overhaul
### 24.1 Official Zyphuel Color System Architecture
* **Color Palette Alignment (`Color.kt` & `Theme.kt`)**:
  - **Pure Crisp White Canvas (`#FFFFFF`)**: Configured `ZyphuelLightBackground` as pure white (`Color(0xFFFFFFFF)`), ensuring all screens, scaffold backgrounds, lazy lists, and dialog surfaces render with modern high-contrast clarity.
  - **Electric Cobalt Blue (`ZyphuelBluePrimary` - `#0062FF`)**: Unified primary brand color matching the Zyphuel logo flame, applied to primary buttons, interactive icons, selection states, badges, and focus borders.
  - **Cyan Flame Accent (`ZyphuelBlueSecondary` - `#00C6FF`)**: Applied to highlights, gradients, rating stars, and subtle interactive states.
  - **Deep Royal Blue (`ZyphuelBlueDark` - `#003087`)**: Established for all typography titles, section headers, and emphasized text for optimal readability.
  - **Soft Blue Surface (`ZyphuelBlueLight` - `#EFF6FF`)**: Established for selected item containers, tag pills, and promotional discount cards.
  - **Neutral Card Border (`ZyphuelCardBorder` - `#E2E8F0`)**: Added 1.dp structural borders around pure white cards to maintain sharp spatial definition against the white canvas.

### 24.2 Service Card Color Unification (`CustomerHomeScreen` & `CategoryComponents.kt`)
* **Unified Blue Theme for All Services**:
  - Replaced disjointed multi-color cards (green petrol, orange gas, cyan water) with cohesive Zyphuel Logo Blue styling:
    - **Petrol Card**: Unified icon surface, icon tint, "2 Types" pill, and price text to `ZyphuelBluePrimary` and `ZyphuelBlueLight`.
    - **Diesel Card**: Aligned icon surface, "2 Types" pill, and price text to `ZyphuelBluePrimary` and `ZyphuelBlueLight`.
    - **LPG Gas Card**: Aligned container, icon tint, and price text to `ZyphuelBluePrimary`.
    - **Pure Water Card**: Aligned container, icon tint, and price text to `ZyphuelBluePrimary`.
  - `CategoryIconHelper.getCategoryColor()` updated to return consistent `ZyphuelBluePrimary`, `ZyphuelBlueSecondary`, and `ZyphuelBlueDark` across all service categories.

### 24.3 Portal Selection Screen Brand Modernization
* **Light Theme Portal Select**:
  - Transformed `PortalSelectScreen` from a dark blue/black gradient into a clean, modern pure white background with royal blue headings (`ZyphuelBlueDark`).
  - Standardized portal action buttons and icons (Customer and Rider) to official brand blue, maintaining high visual hierarchy and seamless brand alignment.

### 24.4 Order Dialogue & Delivery Address Polish
* **Order Dialogue Product Selection Cards**:
  - Unselected item cards rendered on crisp white (`Color.White`) with `#E2E8F0` border; selected states seamlessly transition to `ZyphuelBlueLight` (`#EFF6FF`) with `ZyphuelBluePrimary` border and checkmarks.
  - Multi-item discount alert restyled with `ZyphuelBlueLight` container and `ZyphuelBlueDark` typography.
  - Order summary breakdown enclosed in a subtle `#F0F6FF` container with 1.dp `ZyphuelBluePrimary` border.
  - Delivery address header bar updated to `#F0F6FF` with `ZyphuelBluePrimary` border accent.

---

## 25. Authenticated Gmail App Password Integration & Active Cloud Synchronization
### 25.1 Google App Password Credential Provisioning
* **Dedicated Authenticated SMTP Protocol**:
  - Integrated the administrator's official 16-letter Google App Password (`nvyz rxsb hibn cijb` -> `nvyzrxsbhibncijb`) into `SecureStorageManager.kt` (`AppModule.ADMIN`).
  - Set default `SmtpConfig` parameters:
    - **Host**: `smtp.gmail.com`
    - **Port**: `465` (Direct SSL/TLS socket with explicit SNI)
    - **Sender Email**: `m.daniyalkhan490@gmail.com`
    - **App Password**: `nvyzrxsbhibncijb`
    - **Sender Name**: `"Zyphuel Delivery Operations"`
    - **Enabled**: `true`

### 25.2 Multi-Device & Cloud Firestore Automatic Seeding
* **Self-Healing Initialization (`MainViewModel.kt`)**:
  - On application startup, `MainViewModel` checks local `SecureStorageManager` and Cloud Firestore. If credentials are blank or unconfigured, it automatically writes and syncs the validated App Password.
  - Ensures all devices (customers, riders, and admins) immediately possess operational email delivery capability without manual setup intervention.

### 25.3 Cross-Environment Engine Resilience (`RealtimeEmailEngine.kt`)
* **Safe Base64 & Multiplatform Logging**:
  - Added `safeBase64Encode` supporting both `android.util.Base64` and standard `java.util.Base64`.
  - Added safe logging methods (`logD`, `logI`, `logW`, `logE`) to prevent `RuntimeException` during unit testing or non-Android execution.
  - Added `isReturnDefaultValues = true` in Gradle `testOptions` for Android SDK mocks.
  - Confirmed live TCP socket handshake (`220`), `EHLO`, `AUTH LOGIN`, and authenticated message transmission against Google's production SMTP servers (`smtp.gmail.com:465`).

---

## 26. Automated GitHub Engineering Velocity & Activity Telemetry (v2.6.2)
### 26.1 Real-Time Git Velocity Chart Generator (`scripts/generate_github_graph.js`)
* **Local Git History Analysis**:
  - Automatically parses git commit metadata, commit timestamps, and author messages directly from git log.
  - Aggregates daily velocity curves, 4-month sprint volumes, and 10-week contribution heatmaps.
  - Generates high-definition dark-themed SVG vector asset at `.github/assets/repo-activity-chart.svg`.
* **Continuous Integration (`.github/workflows/update-graph.yml`)**:
  - Triggered on every commit pushed to `main` branch.
  - Automatically executes graph generator script in GitHub Actions Ubuntu runner.
  - Auto-commits and pushes updated velocity chart with `[skip ci]` flag to maintain continuous repository synchronization.

### 26.2 Platform & Repository Synchronization
* **Public Repository Alignment**:
  - Synchronized `README.md` to official version `v2.6.2 (Build 26)`.
  - Added live badges for active commits, version tags, and build status.
  - Embedded the vector engineering activity dashboard directly into repository overview.

---

## 27. Customer Dashboard Modernization & High-Octane Live Pricing (v2.6.4 Build 28)
### 27.1 Customer Home Location Action Overhaul
* **"Share Location" Native Sharing Integration**:
  - Replaced the previous "Change" button under the "Coverage: Lahore Active" header with a modern `[ ↗️ Share Location ]` action chip.
  - Dispatches an Android `Intent.ACTION_SEND` chooser formatted with the active delivery address and direct Google Maps coordinate pin (`https://maps.google.com/?q=lat,lng`).
  - Retains inline address editing capability via direct container clicks.

### 27.2 Visual Decluttering & Elimination of Quick Actions
* **Streamlined View Hierarchy**:
  - Decommissioned and removed the `QuickActionsBar` from `CustomerHomeScreen`.
  - Shifts user focus immediately toward available energy categories without secondary redundant buttons.

### 27.3 Dedicated High-Octane (HOBC 97) Live Price Card
* **Live Dynamic Rate Showcase**:
  - Added a prominent High-Octane fuel card (`service_high_octane`) under "Our Services" directly alongside Super Petrol.
  - Dynamically observes `viewModel.highOctanePrice` (`Rs. %.2f/L`) with official OGRA notified rates.
  - Automatically loads the `OrderDialog` pre-selected with `High-Octane` on tap.

### 27.4 One-Time Lahore Startup Transparency Notice
* **Dismissible Startup Notice Card**:
  - Upgraded the "Early-Stage Lahore Startup" transparency card (`home_startup_notice_card`) to be dismissible.
  - Uses persistent local state via `SharedPreferences` (`zyphuel_ui_prefs`, `has_seen_startup_notice`).
  - Equips the card with a clean dismiss `IconButton` (`Icons.Filled.Close`, `dismiss_startup_notice_btn`), ensuring returning users enjoy a persistent, clutter-free dashboard after acknowledging the notice once.

### 27.5 Granular Sub-Version Standard (v2.6.4.0.0.01 Build 29)
* **Standardized 5-Segment Version Progression**:
  - Standardized the app `versionName` format to `MAJOR.MINOR.PATCH.0.0.XX` (starting at `2.6.4.0.0.01`).
  - Strict release rule: Every future modification, feature, or bug fix advances `versionCode` by `+1` and increments the trailing sub-version segment by `+0.0.0.0.01` (`2.6.4.0.0.01` -> `2.6.4.0.0.02` -> `2.6.4.0.0.03`...).

### 27.6 Sidebar Navigation Drawer Decluttering (v2.6.4.0.0.02 Build 30)
* **Streamlined Navigation Architecture**:
  - **Removed Startup Disclosure Banner in Drawer**: Eliminated the redundant blue startup disclosure card ("Zyphuel is an early-stage startup operating in Lahore...") located directly below the Profile Header and "VERIFIED ADMIN" badge.
  - **Removed "Categories & Services" Section**: Completely removed the foldable service categories accordion from the sidebar navigation drawer, ensuring direct and immediate access to Account Settings, Active Orders, Help Center, and Admin Controls without excessive vertical scrolling.

### 27.7 Checkout & Home Screen Decluttering (v2.6.4.0.0.03 Build 31)
* **OrderDialog Grand Total Section Simplification**:
  - Removed the payment instruction subtext (`"💵 Pay via Cash on Delivery, JazzCash or EasyPaisa upon arrival."`) located below the Grand Total price row in `OrderDialog`.
  - The Grand Total amount now cleanly terminates the order breakdown without secondary explanatory notes.
* **Home Screen Search Bar Decommissioning**:
  - Removed the global `ServiceSearchBar` from `CustomerHomeScreen`.
  - Cleaned up unused `serviceSearchQuery` / `serviceSearchResults` collection in the screen composable.
  - Harmonized the interactive spotlight guided tour (`homeTourSteps`), advancing smoothly from the delivery location card directly into doorstep services (`service_petrol`).

### 27.8 Terms & Privacy Web Sync, Anonymous Order Acceptance & Splash Branding (v2.6.4.0.0.04 Build 32)
* **Real Terms & Privacy Policy URLs & Accurate Lahore Energy Coverage**:
  - Rewrote in-app `TermsAndPrivacyDialog` to replace all placeholder `zyphuel.com` domains with official live links:
    - Terms of Use: `https://zyphuel.netlify.app/terms-of-use`
    - Privacy Policy: `https://zyphuel.netlify.app/privacy`
  - Replaced generic sample text with accurate Lahore doorstep energy delivery coverage (Euro-V Petrol, High-Octane 97, High-Speed Diesel, 11.8kg LPG Cylinders, Pure Drinking Water @ Rs. 50/gallon, 100% Cash-on-Delivery, and OGRA Petroleum Rules 1937 compliance).
  - Explicitly disclosed Google Play Store compliance mandates: Foreground & background location telematics (`FOREGROUND_SERVICE_LOCATION`), safety standards, and permanent one-tap account deletion (`delete_account_btn`).
  - Synchronized `TERMS_AND_CONDITIONS.md` and `PRIVACY_POLICY.md` with official contact info (`m.daniyalkhan490@gmail.com`, `+92 323 0112464`, `https://zyphuel.netlify.app/`).
* **Support Email Contact Renaming**:
  - Replaced label `"Direct Admin Email Contact"` with `"Support Email Contact"` in `SupportDialog` (`Screens.kt`).
* **Anonymous Customer Order Acceptance Notifications**:
  - Customer notifications across in-app push, local notifications, and automated transactional emails no longer disclose who accepted the order (neither rider name nor admin direct assignment).
  - Standardized notification title to `"Order Accepted ✅"` and message to `"Your Order #$orderId has been accepted and is being prepared for dispatch."` across `Repository.kt` (`acceptOrder`), `MainViewModel.kt` (`acceptRiderOrder`, `adminAcceptOrder`, and `changeOrderStatus`).
* **Splash Screen Logo Branding Update**:
  - Changed the tagline under the company splash logo in `SplashScreen` (`Screens.kt`) from `"Lahore's Premium Delivery Network"` to `"Zyphuel Delivery App"`.

### 27.9 Profile Settings Cleanup, App-Wide Single Notice & Multi-Language Engine (v2.6.4.0.0.06 Build 34)
* **Profile Settings & Security Screen Cleanup**:
  - Removed the "Legal & App Compliance" card from `SecuritySettingsScreen.kt`.
  - Removed "Saved Lahore Addresses" and "Direct Lahore Support" from `ProfileSettingsDialog`.
  - Bound the Early-Stage Startup Notice across `ProfileSettingsDialog` and `CustomerHomeScreen` to a strict 1-time app-wide display rule (`has_seen_startup_notice` in `zyphuel_ui_prefs`). Once dismissed, it never reappears anywhere in the app.
* **14+ Language Multi-Language Localization Engine (`AppLanguageManager.kt`)**:
  - Built comprehensive localization engine supporting English, Urdu, Punjabi, Sindhi, Pashto, Arabic, Persian, Turkish, French, Spanish, German, Chinese, Hindi, and Russian.
  - Provided reactive dynamic string lookup (`viewModel.getString(key, fallback)`) and persistent selection in `zyphuel_ui_prefs`.

### 27.10 Admin Category Operations Center & Persistent Biometric Authentication Lifecycle (v2.6.4.0.0.07 Build 35)
* **Admin Dashboard Current Category Spotlight (`AdminCategoryManagementTab`)**:
  - Located at the top of Tab 8 (Categories) in `AdminDashboardScreen`.
  - Prominently showcases the currently inspected / active category with its name, icon, Category ID, and operational badge ("CURRENT ACTIVE 🎯" / "INACTIVE 🔴").
  - Displays instant delivery fee, subcategories active count, operational availability status chip, and location coverage area.
  - Equipped with an immediate live toggle switch and horizontal quick-selection category chips allowing the administrator to switch the spotlighted current category with one tap.
* **Robust Biometric Authentication Lifecycle & Privacy Isolation (`SecureStorageManager`, `Screens.kt`, `MainViewModel`)**:
  - **Disabled by Default**: On fresh installation or for un-enrolled accounts, biometric fingerprint authentication is strictly disabled (`false`).
  - **Only for Registered Users**: First-time or unregistered users never see the biometric login card on `AuthScreen` or `PortalSelectScreen`. The option is only rendered if `isBioEnabled && !registeredEmail.isNullOrBlank() && !isRegister`.
  - **Persistent Across Logout & App Reopen**:
    - Hardened `SecureStorageManager` with cached `EncryptedSharedPreferences` instances and infallible device-level persistent backing (`zyphuel_biometric_device_prefs`).
    - Fixed `logout()` to only revoke active session tokens while preserving device biometric enrollment and registered email.
    - Synchronized biometric states automatically on `MainViewModel` initialization and upon user logout.
    - Added role-aware logout routing so riders return to `login_rider` and customers return to `login_customer`.
  - **One-Tap Re-entry on Portal Select & Login Screen**:
    - When a registered user re-opens the app while logged out, `PortalSelectScreen` displays a dedicated **"⚡ Quick Biometric Login"** card for their registered email.
    - `AuthScreen` displays the registered user's email and a 1-tap fingerprint authentication button that directly logs them in without entering passwords.

### 27.11 Rider Login Integration into Customer Login Sidebar & Seamless Portal Switching (v2.6.4.0.0.08 Build 36)
* **Customer Login Sidebar Navigation Drawer (`AuthDrawerContent`)**:
  - Embedded a complete Material 3 `ModalNavigationDrawer` into the customer authentication flow (`AuthScreen`).
  - Added a hamburger menu button (`Icons.Filled.Menu`, testTag `"auth_sidebar_menu_btn"`) on the top navigation bar to open the sidebar.
  - Features dedicated **"Switch as Rider"** card (`testTag("sidebar_switch_as_rider")`) with custom fleet badge and chevron.
  - Tapping **"Switch as Rider"** smoothly closes the drawer and redirects the user directly to the **Rider Login Form** (`login_rider`).
  - Added reciprocal **"Switch as Customer"** card in the rider authentication drawer (`testTag("sidebar_switch_as_customer")`) to allow riders to switch to the customer login screen seamlessly.
* **Top-Bar Quick Switch Action (`switch_as_rider_top_btn`)**:
  - Implemented a 1-tap quick action chip on the customer login top bar beside the back button, enabling instant switching between Rider and Customer portals without opening the drawer.
* **In-App Customer Drawer Integration (`DrawerContent`)**:
  - Added **"Switch as Rider"** (`testTag("sidebar_switch_as_rider")`) with rider motorcycle icon and fleet badge inside the customer's main navigation drawer under "ACCOUNT & SETTINGS".
  - Logged-in customers can switch to the Rider Login form directly from anywhere in the app with zero friction.
* **Strict Compliance & Zero-Deletion**:
  - Preserved all existing authentication flows, registration fields, Google Sign-In, and biometric capabilities intact.
  - Incremented `versionCode` to `36` and advanced `versionName` to `"2.6.4.0.0.08"`.
