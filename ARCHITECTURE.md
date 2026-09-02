# Zyphuel System Architecture & Design Documentation 🏛️

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
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack & Key Frameworks
* **UI Framework**: Jetpack Compose (Material 3) with dynamic color system, custom Canvas animations, and M3 components.
* **Architecture Pattern**: MVVM with single `MainViewModel` managing reactive `StateFlow` streams.
* **Local Database**: Android Room DB (SQLite) with KSP compiler and Kotlin Coroutines/Flow integration.
* **Security & Biometrics**: AndroidX `BiometricPrompt` API, SHA-256 password hashing, AES-256 encrypted preferences (`SecureStorageManager`), input sanitization (`SecurityInputValidator`), and rate limiting (`SecurityRateLimiter`).
* **Push Notifications & Messaging**: Android System Push Notification Manager (`zyphuel_order_updates` high-importance channel) and Firebase Cloud Messaging (`ZyphuelFcmService.kt`).
* **Real-Time Transactional Email Gateway**: Multi-channel authenticated engine (`RealtimeEmailEngine.kt`) supporting direct TLS/SSL Port 465, RFC 3207 STARTTLS Port 587, and Port 443 HTTPS Webhook relay with cross-device Cloud Firestore sync (`system_config/email_gateway`).
* **Asynchronous Execution**: Kotlin Coroutines (`Dispatchers.IO`, `viewModelScope`) and reactive `Flow` pipelines.

---

## 📁 Core Codebase Directory Structure

```
app/src/main/java/com/example/
├── data/
│   ├── AppDatabase.kt          # Room Database Instance & Version Migrations
│   ├── Models.kt               # Room Data Entities & DAOs (UserDao, OrderDao, AuditLogDao, NotificationDao, MarkedLocationDao)
│   ├── Repository.kt           # Unified Data Repository Abstraction (with Admin Master Supervisory Authority)
│   ├── FirestoreOrderRepository.kt # Firestore Cloud Order Sync & Live Listeners
│   └── FirestoreUserRepository.kt  # Firestore Cloud User Sync & Email Gateway Real-time Config Sync
├── notifications/
│   └── ZyphuelFcmService.kt    # Firebase Cloud Messaging & System Push Handler
├── security/
│   ├── BiometricSecurityManager.kt # AndroidX Fingerprint/Face Unlock Hardware Bridge
│   ├── SecureStorageManager.kt     # AES-256 Encrypted Session & Preference Storage
│   ├── SecurityInputValidator.kt   # Input Sanitization Regex Rules
│   └── SecurityRateLimiter.kt      # Brute-Force Action Rate Limiter
├── service/
│   ├── LocationService.kt      # FusedLocationProviderClient Background & Live GPS Provider
│   └── RiderLocationForegroundService.kt # Foreground Service for Live Delivery Navigation
├── ui/
│   ├── MainViewModel.kt        # Central ViewModel, Telematics Calculations & Business Logic Hub
│   ├── Screens.kt              # Jetpack Compose Screens, Order Dialog, Profile, Drawer UI & Admin Mailbox
│   ├── Theme.kt                # Material 3 Custom Theme Colors, Shapes & Typography
│   └── components/
│       └── DeliveryNotificationPermissionPrompt.kt # Notification Permission Onboarding Modal
└── util/
    ├── FeeConstants.kt         # Centralized Delivery Fee Calculation Utility
    ├── InvoiceGenerator.kt     # Tax Invoice Generator, Native Android PDF Printing & Sharing Engine
    └── RealtimeEmailEngine.kt  # RFC 5321 / RFC 3207 Real-Time Multi-Channel Email Gateway
```

---

## 🔒 Security, Privacy & Google Play Compliance Constraints
1. **Repository Single Source of Truth**: All database operations route through `AppRepository` on background IO threads.
2. **Security Input Validation**: All user inputs (email, phone, volume, feedback, rating) are pre-validated by `SecurityInputValidator` before database insertion.
3. **Audit Trail Persistence**: Security-sensitive actions (logins, order placements, rating submissions, biometric toggles, account deletions) automatically log an `AuditLogEntity` entry.
4. **Google Play Account Deletion Policy**: Complete user record purge (`deleteCurrentAccount`) wipes `UserEntity`, `MarkedLocationEntity`, and session tokens in compliance with Google Play Data Safety rules.
5. **Daily Safety Notice Policy**: Daily 1-time GPS disclaimer (`DailyGpsSafetyDisclaimerDialog`) informs users regarding live GPS rider telemetry for transparent order fulfillment.
6. **Test Tag Mandate**: Interactive UI elements must maintain unique Compose `testTag` modifiers for accessibility and automated testing.

