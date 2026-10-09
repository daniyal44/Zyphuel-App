# Zyphuel System Architecture & Design Documentation 🏛️

**Last Updated:** <font color="#10b981"><b>2026-10-09</b></font> • **Application Version:** <font color="#10b981"><b>2.6.4.0.0.26 (Build 54)</b></font>

---

## 📌 Architecture Overview
Zyphuel is engineered as a modern, high-performance, offline-first Android application built with **Kotlin** and **Jetpack Compose (Material 3)**. The application follows Clean Architecture principles organized into an **MVVM (Model-View-ViewModel)** pattern with a unified reactive state flow and centralized Repository data handling.

---

## 🏗️ Layered System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                            │
│   Jetpack Compose UI Screens (Screens.kt) + Material Design 3 Components │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Reactive StateFlow / Events
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            STATE / VIEWMODEL                            │
│   MainViewModel.kt (StateFlow<UiState>, CoroutineScope, Event Handlers)  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Unified Data Contracts
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    SECURITY, VALIDATION & UTILITIES                     │
│  BiometricSecurityManager | SecurityInputValidator | SecurityRateLimiter │
│  DeliveryOperatingHoursManager | FeeConstants | InvoiceGenerator        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Asynchronous Coroutine Flow
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     REPOSITORY & DATA PERSISTENCE                       │
│  ZyphuelRepository.kt  ◄►  Room SQLite DB (AppDatabase.kt + DAOs)     │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    EXTERNAL & SYSTEM SERVICES ENGINE                    │
│   ZyphuelFcmService (FCM) | Android NotificationTray | Email Dispatch  │
│   Real-Time Trading Movement Engine (scripts/generate_github_graph.js) │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack & Key Frameworks
* **UI Framework**: Jetpack Compose (Material 3) with dynamic color system, custom Canvas animations, and M3 components.
* **Architecture Pattern**: MVVM with single `MainViewModel` managing reactive `StateFlow` streams.
* **Local Database**: Android Room DB (SQLite) with KSP compiler and Kotlin Coroutines/Flow integration.
* **Operating Hours & Schedule Engine**: [`DeliveryOperatingHoursManager.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/DeliveryOperatingHoursManager.kt) calibrated to Pakistan Standard Time (`Asia/Karachi`, UTC+5).
* **Delivery Fee Engine**: [`FeeConstants.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/FeeConstants.kt) providing per-liter tiered rates (Rs. 300 to Rs. 400) and silent OGRA pump rate markup (`PUMP_RATE_MARKUP = 5.00` Rs./L).
* **Security & Biometrics**: AndroidX `BiometricPrompt` API, SHA-256 password hashing, AES-256 encrypted preferences (`SecureStorageManager`), input sanitization (`SecurityInputValidator`), and rate limiting (`SecurityRateLimiter`).
* **Push Notifications & Messaging**: Android System Push Notification Manager (`zyphuel_order_updates` high-importance channel) and Firebase Cloud Messaging (`ZyphuelFcmService.kt`).
* **Real-Time Transactional Email Gateway**: Multi-channel authenticated engine (`RealtimeEmailEngine.kt`) supporting direct TLS/SSL Port 465, RFC 3207 STARTTLS Port 587, and Port 443 HTTPS Webhook relay with cross-device Cloud Firestore sync (`system_config/email_gateway`).
* **Trading-Style Codebase Movement Graph**: Built via [`scripts/generate_github_graph.js`](file:///d:/Games/New%20folder-web/Claude/scripts/generate_github_graph.js), tracking movement across Daily (1D), Weekly (1W), and Monthly (1M) timeframes with vector SVG rendering.

---

## 📁 Core Codebase Directory Structure

```
app/src/main/java/com/example/
├── data/
│   ├── AppDatabase.kt                  # Room Database Instance & Version Migrations
│   ├── Models.kt                       # Room Data Entities & DAOs (UserDao, OrderDao, AuditLogDao, NotificationDao, MarkedLocationDao)
│   ├── Repository.kt                   # Unified Data Repository Abstraction (with Admin Master Supervisory Authority)
│   ├── FirestoreOrderRepository.kt     # Firestore Cloud Order Sync & Live Listeners
│   └── FirestoreUserRepository.kt      # Firestore Cloud User Sync & Email Gateway Real-time Config Sync
├── notifications/
│   └── ZyphuelFcmService.kt            # Firebase Cloud Messaging & System Push Handler
├── security/
│   ├── BiometricSecurityManager.kt     # AndroidX Fingerprint/Face Unlock Hardware Bridge
│   ├── SecureStorageManager.kt         # AES-256 Encrypted Session & Preference Storage
│   ├── SecurityInputValidator.kt       # Input Sanitization Regex Rules
│   └── SecurityRateLimiter.kt          # Brute-Force Action Rate Limiter
├── service/
│   ├── LocationService.kt              # FusedLocationProviderClient Background & Live GPS Provider
│   └── RiderLocationForegroundService.kt # Foreground Service for Live Delivery Navigation
├── ui/
│   ├── MainViewModel.kt                # Central ViewModel, Telematics Calculations, Operating Hours Gate & Business Logic Hub
│   ├── Screens.kt                      # Jetpack Compose Screens, Order Dialog, Profile, Drawer UI & Admin Mailbox
│   ├── Theme.kt                        # Material 3 Custom Theme Colors, Shapes & Typography
│   └── components/
│       ├── TermsAndPrivacyDialog.kt    # In-App Legal Viewer (Terms & Privacy with Live Build Badge)
│       └── DeliveryNotificationPermissionPrompt.kt # Notification Permission Onboarding Modal
└── util/
    ├── DeliveryOperatingHoursManager.kt # PKT Timezone Delivery Operating Hours & Status Evaluator
    ├── FeeConstants.kt                 # Per-Liter Tiered Delivery Fee (300-400 PKR) & Silent OGRA Pump Markup (+5.00)
    ├── InvoiceGenerator.kt             # Tax Invoice Generator, Native Android PDF Printing & Sharing Engine
    └── RealtimeEmailEngine.kt          # RFC 5321 / RFC 3207 Real-Time Multi-Channel Email Gateway
```

---

## 🕒 Doorstep Delivery Operating Hours & Pricing Pipeline (<font color="#10b981"><b>2026-10-09</b></font>)
1. **Operating Hours Windows (PKT, UTC+5)**:
   - **Mon–Thu**: <font color="#10b981"><b>08:00 AM – 08:00 PM</b></font>
   - **Fri**: <font color="#10b981"><b>08:00 AM – 01:00 PM</b></font>
   - **Sat–Sun**: <font color="#10b981"><b>10:00 AM – 06:00 PM</b></font>
2. **UI & ViewModel Gate**:
   - Order actions (Home FAB, Order Modal Confirm Button, Reorder button) are completely omitted when closed.
   - `MainViewModel.placeOrder()` enforces `isDeliveryOpen` check and rejects submissions outside operational hours.
3. **Per-Liter Delivery Rates**:
   - 1..10L: <font color="#10b981"><b>Rs. 300.00</b></font> | 11L: <font color="#10b981"><b>Rs. 320.00</b></font> | 12L: <font color="#10b981"><b>Rs. 340.00</b></font> | 13L: <font color="#10b981"><b>Rs. 360.00</b></font> | 14L: <font color="#10b981"><b>Rs. 380.00</b></font> | 15L (Max Cap): <font color="#10b981"><b>Rs. 400.00</b></font>.
   - Formula for 11..15L: `300.00 + (liters - 10) * 20.00`.
4. **Pump Rate Markup Constant**:
   - `PUMP_RATE_MARKUP = 5.00` Rs./L added on top of official OGRA ex-depot base rates (`finalRate = ograBase + 5.00`), running silently in the background.

---

## 🔒 Security, Privacy & Google Play Compliance Constraints
1. **Repository Single Source of Truth**: All database operations route through `AppRepository` on background IO threads.
2. **Security Input Validation**: All user inputs (email, phone, volume, feedback, rating) are pre-validated by `SecurityInputValidator` before database insertion.
3. **Audit Trail Persistence**: Security-sensitive actions (logins, order placements, rating submissions, biometric toggles, account deletions) automatically log an `AuditLogEntity` entry.
4. **Google Play Account Deletion Policy**: Complete user record purge (`deleteCurrentAccount`) wipes `UserEntity`, `MarkedLocationEntity`, and session tokens in compliance with Google Play Data Safety rules.
5. **Daily Safety Notice Policy**: Daily 1-time GPS disclaimer (`DailyGpsSafetyDisclaimerDialog`) informs users regarding live GPS rider telemetry for transparent order fulfillment.
6. **Test Tag Mandate**: Interactive UI elements must maintain unique Compose `testTag` modifiers for accessibility and automated testing.
