# 🚚 Zyphuel - Doorstep Fuel & Clean Water Delivery Platform (Android)

<div align="center">
  <img src="app/src/main/res/drawable/logo.png" width="120" height="120" alt="Zyphuel Logo" />
  <h3>Pakistan's Premier On-Demand Energy & Water Delivery Ecosystem</h3>
  <p><b>Target SDK 36 • Android 15/16 Ready • Jetpack Compose Material 3 • Room DB • Live GPS Radar Telematics</b></p>
</div>

---

## 📌 About Zyphuel
Zyphuel brings on-demand doorstep delivery of **Super Petrol, High-Speed Diesel, High-Octane, LPG Gas Cylinders, and Pure Mineral Drinking Water** across Lahore, Punjab. The platform coordinates residential and fleet deliveries with automated route dispatching from the central Green Town Depot origin to customer destination pins.

---

## 🚀 Key Features

* **Instant Multi-Product Ordering**: Seamless multi-item selection with automatic 50% bundled delivery discounts and clean Cash on Delivery (COD) checkout.
* **Native Live Google Maps Telematics (`TrackerScreen`)**: Real-time vehicle radar, custom heading-oriented bowser markers, dynamic driver assignment, and live Haversine distance computations.
* **Daily 1-Time Order Safety GPS Notice (`DailyGpsSafetyDisclaimerDialog`)**: Daily safety disclosure banner informing customers of active GPS tracking for secure delivery fulfillment.
* **Biometric & Encrypted Security**: AndroidX `BiometricPrompt` hardware authentication, AES-256 encrypted session tokens, SHA-256 password hashing, and brute-force rate limiters.
* **Google Play Policy Compliant**: Full support for In-App and Sidebar Account Deletion (`deleteCurrentAccount`), foreground service disclosures, and OGRA hazardous materials guidelines.

---

## 🛠️ Tech Stack & Architecture

* **Language**: Kotlin 2.0+
* **UI Toolkit**: Jetpack Compose with Material Design 3
* **State Management**: Centralized `MainViewModel` with Kotlin `StateFlow`
* **Local Persistence**: Android Room DB (SQLite) with KSP compiler
* **Cloud & Push**: Firebase Cloud Messaging (FCM), Cloud Firestore real-time listeners
* **Location Engine**: Google Play Services `FusedLocationProviderClient` (`PRIORITY_HIGH_ACCURACY`)

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
* [`FEATURES_DOCUMENTATION.md`](file:///d:/Games/New%20folder-web/Claude/FEATURES_DOCUMENTATION.md): Comprehensive feature inventory & component specifications.
* [`ARCHITECTURE.md`](file:///d:/Games/New%20folder-web/Claude/ARCHITECTURE.md): System architecture, layered diagram, and data flow constraints.
* [`PLAY_STORE_ASO_BLUEPRINT.md`](file:///d:/Games/New%20folder-web/Claude/PLAY_STORE_ASO_BLUEPRINT.md): App Store Optimization, keywords, and release assets guide.
* [`bugs.md`](file:///d:/Games/New%20folder-web/Claude/bugs.md): Master bug resolution and verification ledger.
* [`MEMORY.md`](file:///d:/Games/New%20folder-web/Claude/MEMORY.md): Persistent implementation memory and evolution phases.

