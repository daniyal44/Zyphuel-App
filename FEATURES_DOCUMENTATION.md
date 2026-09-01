# 🚀 Zyphuel App Features & Technical Documentation
**App Version:** `v2.3.0 (Build 3)` | **Target SDK:** `36` (Android 15/16 Ready) | **Last Updated:** `2026`

Welcome to the complete architectural and functional guide for the **Zyphuel** Android application. This document outlines every single feature, function, database entity, and user flow from start to finish.

---

## 📋 Table of Contents & Modular Documentation Files Index

Below is the complete index of all **12 dedicated documentation files** (including 10 modular feature guides in `/docs`, system architecture, and persistent project memory) covering every aspect of Zyphuel:

| # | File Name | Category | Primary Focus & Functions Covered |
| :-: | :--- | :--- | :--- |
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
* **8-Step Interactive App Tour Guide (`AppTourGuideDialog`)**:
  * **Interactive Onboarding Modal**: First-time users are greeted with a full 8-step interactive tour explaining Doorstep Fuel & Essentials, Live OGRA Rates, 1-Tap COD Ordering, Share Location, Order Details & 4-Step Stepper, Direct Driver Communication, Real-Time Emails, and Biometric Security.
  * **Flexible Navigation**: Features initial **"Take Tour (8 Steps) 🚀"** and **"Skip for Now"** options, with visual step indicators (`Step X of 8`), linear progress bar, Back/Next controls, and persistent storage so returning users are not re-prompted.
  * **Always Accessible**: Users can re-launch the full 8-step guide anytime via the Navigation Drawer (`App Tour Guide (8 Steps) 📖`).
* **Admin Order Accept & Decline Controls (`adminAcceptOrder` & `adminDeclineOrder`)**:
  * **Admin Action Buttons on `AdminOrderCard`**: For active and pending orders, administrators have green **"Accept"** and red **"Decline"** buttons directly on the card.
  * **Instant Acceptance & Rider Assignment (`adminAcceptOrder`)**: Approves the order, assigns a verified rider, updates Room DB & Firestore, and immediately dispatches real-time confirmation emails to both customer and rider.
  * **Reason-Based Order Rejection (`adminDeclineOrder` & `AdminDeclineOrderDialog`)**: Prompts the admin with predefined quick reasons (Out of delivery coverage, Depot fuel stock limit, Customer cancellation request, Phone unreachable, or Custom reason), updates status to `"Cancelled"`, records an audit log, and sends an automated cancellation email to the customer with the explanation.
* **Instant 0ms Local Order Creation & Non-Blocking Remote Sync**: Room DB creates and confirms orders in 0ms with immediate Main thread navigation to `TrackerScreen`. Cloud Firestore sync is wrapped in background coroutines with strict `1500ms` timeouts, preventing any network hangs from delaying the customer experience.
* **2-Decimal Price Standard Formatting (`formatPrice`)**: All order prices, totals, COD amounts, and pending status cards are strictly formatted to 2 decimal places (e.g., `Rs. 2,970.00`) across `MainViewModel`, `TrackerScreen`, `CustomerOrderHistoryScreen`, `CustomerHomeScreen`, and `AdminScreens`.
* **Google Play Compliant Account Deletion (`DeleteAccountConfirmationDialog` & `deleteCurrentAccount`)**: In-app one-tap account deletion and complete data wiping available in both Customer Sidebar Drawer and Profile Settings Dialog. Permanently removes Room DB user data, marked location pins, session credentials, and logs an irreversible audit trail.
* **Adaptive Scrollable Navigation Drawer (`DrawerContent` & `SidebarItem`)**:
  * **Full Vertical Scrolling & Screen Safety (`verticalScroll`)**: Complete sidebar navigation drawer content is wrapped in a vertical scroll state with safe-area padding (`statusBarsPadding()` & `navigationBarsPadding()`), guaranteeing that all action buttons, settings, order histories, admin tools, delete account option, and log out button are 100% visible and accessible on all screen resolutions, phone aspect ratios, and accessibility font scale factors without clipping.
    * **Categorized Navigation Architecture**: Items organized into clean, labeled visual sections (*Account & Settings*, *Orders & Deliveries*, *Help & Support*, *Admin Controls*, *Account Actions*) separated with subtle dividers (`HorizontalDivider`).
    * **Touch Feedback & Visual Polish**: `SidebarItem` with rounded background icon badges, distinct category labels, high-contrast dark text, custom destructive tinting for account deletion, and Material 3 ripple feedback.


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
  * **System Notification Icon & UI Alignment Architecture**:
    * **Monochrome Vector Small Icon (`@drawable/ic_notification`)**: Android System UI-compliant 24x24dp monochrome vector drawable featuring the signature Zyphuel fuel drop with transparent 'Z' cutout (`fillType="evenOdd"`), optical padding safe-zones (2dp margin), and dynamic status-bar tinting support.
    * **Brand Primary Accent Tinting**: Automatic notification header and small icon tinting using Zyphuel primary blue (`#0284C7` / `R.color.primary`).
    * **High-Res Large Icon Display**: Rich notification shade cards decode and render the full-color Zyphuel brand badge (`@drawable/icon`) via `setLargeIcon` across FCM pushes, local notifications, foreground services, and WorkManager price alerts.
  * **Firebase Cloud Messaging (FCM) Integration**: Real-time push payload processing via `ZyphuelFcmService.kt` with foreground banners and background system notification triggers using the monochrome vector notification icon (`@drawable/ic_notification`) and high-res brand large icon.
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

---

## 12. Database Architecture & Data Models
Room Local Persistence layer (`AppDatabase.kt` v9):
* **`UserEntity`**: `email` (PK), `name`, `passwordHash`, `role`, `phoneNumber`, `residentialAddress`, `isVerified`.
* **`OrderEntity`**: `id` (PK), `customerEmail`, `customerName`, `serviceType`, `fuelVolumeLiters`, `totalAmountPkr`, `status`, `assignedRiderEmail`, `assignedRiderName`, `deliveryAddress`, `etaMinutes`.
* **`AuditLogEntity`**: `id` (PK), `timestamp`, `action`, `performedBy`, `details`.
* **`NotificationEntity`**: `id` (PK), `userEmail`, `title`, `message`, `timestamp`, `isRead`.
* **`MarkedLocationEntity`**: `id` (PK), `userEmail`, `label`, `address`, `latitude`, `longitude`, `createdAt`.

---

## 13. Auto-Update & Maintenance Protocol
This document (`FEATURES_DOCUMENTATION.md`) serves as the single source of truth for app features. Whenever a new screen, viewmodel function, or database field is added:
1. Update `FEATURES_DOCUMENTATION.md` under the respective section.
2. Maintain `AGENTS.md` instructions so AI agents continue maintaining this file in sync with future codebase changes.
