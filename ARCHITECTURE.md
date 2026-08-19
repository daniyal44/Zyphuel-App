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
* **Asynchronous Execution**: Kotlin Coroutines (`Dispatchers.IO`, `viewModelScope`) and reactive `Flow` pipelines.

---

## 📁 Core Codebase Directory Structure

```
app/src/main/java/com/example/
├── data/
│   ├── AppDatabase.kt          # Room Database Instance & Migrations
│   ├── Daos.kt                 # Data Access Objects (UserDao, OrderDao, AuditLogDao, NotificationDao)
│   ├── Models.kt               # Room Data Entities (UserEntity, OrderEntity, AuditLogEntity, NotificationEntity)
│   └── ZyphuelRepository.kt    # Unified Data Repository Abstraction
├── notifications/
│   └── ZyphuelFcmService.kt    # Firebase Cloud Messaging & System Push Handler
├── security/
│   ├── BiometricSecurityManager.kt # AndroidX Fingerprint/Face Unlock Hardware Bridge
│   ├── SecureStorageManager.kt     # AES-256 Encrypted Session & Preference Storage
│   ├── SecurityInputValidator.kt   # Input Sanitization Regex Rules
│   └── SecurityRateLimiter.kt      # Brute-Force Action Rate Limiter
└── ui/
    ├── MainViewModel.kt        # Central ViewModel & Business Logic Hub
    ├── Screens.kt              # Jetpack Compose Views, Cards, Tracking Map & Rating UI
    └── Theme.kt                # Material 3 Custom Theme Colors, Shapes & Typography
```

---

## 🔒 Security & Data Flow Constraints
1. **Repository Single Source of Truth**: All database operations route through `ZyphuelRepository` on background IO threads.
2. **Security Input Validation**: All user inputs (email, phone, volume, feedback, rating) are pre-validated by `SecurityInputValidator` before database insertion.
3. **Audit Trail Persistence**: Security-sensitive actions (logins, order placements, rating submissions, biometric toggles) automatically log an `AuditLogEntity` entry.
4. **Test Tag Mandate**: Interactive UI elements must maintain unique Compose `testTag` modifiers for accessibility and automated testing.
