# 🚚 Zyphuel Platform (Android v2.3.0 • Release 2026)

<div align="center">
  <img src="company .png" width="140" height="140" alt="Zyphuel MDK Logo" />
  <h3>Pakistan's Premier On-Demand Energy & Clean Water Ecosystem</h3>
  <p><b>Version 2.3.0 (Build 3) • Target SDK 36 • Android 15/16 Ready • Jetpack Compose Material 3 • Room DB • Real-Time Dual Notifications</b></p>
</div>

---

## 📌 About Zyphuel
Zyphuel is Pakistan's premier on-demand doorstep delivery platform for **Super Petrol, High-Speed Diesel, High-Octane, LPG Gas Cylinders, and Pure Mineral Drinking Water** across Lahore, Punjab. The platform delivers seamless consumer, rider, and admin operations backed by real-time dual email dispatch, biometric security, and 0ms instantaneous order processing.

---

## 🚀 Key Features (v2.3.0 Latest)

* **8-Step Interactive App Tour Guide (`AppTourGuideDialog`)**: Interactive onboarding guide for all new users with "Take Tour (8 Steps) 🚀" and "Skip for Now" options, also re-launchable anytime from the Drawer.
* **Admin Order Controls (Accept & Decline)**: Admin Dashboard order cards equipped with direct "Accept Order" (rider assignment) and "Decline Order" (with predefined cancellation reasons and instant customer email alerts).
* **Guaranteed Inbox Real-Time Email Delivery (`RealtimeEmailEngine.kt`)**: Multi-channel delivery engine using FormSubmit.co AJAX HTTPS relay, Cloud Firestore triggers, and direct SMTP communication for automated delivery directly to Gmail inboxes.
* **Verified Admin Blue Tick & Permanent Super Admin Guard**: Root Administrator account (`m.daniyalkhan490@gmail.com`) is permanently protected from deletion, resets, or removal across local Room DB and Cloud Firestore, displaying the official **Blue Tick Verified Badge** across Drawer, Profile, and Admin headers.
* **Native Share Location Integration**: Replaced static address edits with native Android Share Location functionality, sending GPS latitude/longitude and Google Maps coordinates directly via messaging/social apps.
* **High-Performance Order Details & Status Stepper**: Replaced heavy map overlays with a blazing-fast, 0ms responsive native Order Details and 4-step delivery progress timeline (`TrackerScreen` & `CustomerOrderCard`).
* **Universal Biometric Authentication**: Hardware-backed AndroidX `BiometricPrompt` authentication available for **both Google Sign-In and Manual Email logins**, configurable directly from Profile and Security Settings.
* **Instant Multi-Product Ordering**: Seamless multi-item selection with automatic bundled delivery discounts and clean Cash on Delivery (COD) checkout.
* **Google Play Policy Compliant**: Full support for In-App and Sidebar Account Deletion (`deleteCurrentAccount`), encrypted AES-256 session tokens, SHA-256 password hashing, and OGRA safety compliance.

---

## 🛠️ Tech Stack & Architecture

* **Language**: Kotlin 2.0+ (Jetpack Compose with Material Design 3)
* **Architecture**: MVVM with `MainViewModel` and Kotlin `StateFlow`
* **Local Persistence**: Android Room DB (SQLite) with KSP compiler (v11 schema)
* **Cloud Sync**: Google Cloud Firestore real-time listeners & Firebase Cloud Messaging (FCM)
* **Security & Auth**: AndroidX `BiometricPrompt`, `EncryptedSharedPreferences`, Google OAuth & SHA-256 hashing
* **Location & Sharing**: Google Play Services `FusedLocationProviderClient` (`PRIORITY_HIGH_ACCURACY`) and Android `ACTION_SEND`

---

## 📦 Build & Release Commands

### 1. Compile Debug Build
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"; & "C:\Users\mdani\.gradle\wrapper\dists\gradle-9.5.0-bin\bvnork1r7n8i6kp5cnkibsc9q\gradle-9.5.0\bin\gradle.bat" assembleDebug
```

### 2. Generate Google Play Store Release Bundle (`.aab`)
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"; & "C:\Users\mdani\.gradle\wrapper\dists\gradle-9.5.0-bin\bvnork1r7n8i6kp5cnkibsc9q\gradle-9.5.0\bin\gradle.bat" bundleRelease
```
*Generated output path:* `app/build/outputs/bundle/release/app-release.aab`

---

## 📁 Key Documentation References
* [`FEATURES_DOCUMENTATION.md`](file:///d:/Games/New%20folder-web/Claude/FEATURES_DOCUMENTATION.md): Comprehensive feature inventory & component specifications (v2.3.0).
* [`ARCHITECTURE.md`](file:///d:/Games/New%20folder-web/Claude/ARCHITECTURE.md): System architecture, layered diagram, and data flow constraints.
* [`PLAY_STORE_ASO_BLUEPRINT.md`](file:///d:/Games/New%20folder-web/Claude/PLAY_STORE_ASO_BLUEPRINT.md): App Store Optimization, keywords, and release assets guide.
* [`bugs.md`](file:///d:/Games/New%20folder-web/Claude/bugs.md): Master bug resolution and verification ledger.
* [`MEMORY.md`](file:///d:/Games/New%20folder-web/Claude/MEMORY.md): Persistent implementation memory and evolution phases.

