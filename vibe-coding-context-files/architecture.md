# Zyphuel System Architecture

> **Current App Version:** `2.6.4.0.0.24 (Build 52)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

---

## 1. Overview
- **Product:** Zyphuel — On-demand doorstep fuel delivery platform (Lahore, Pakistan).
- **Core Value:** Safe, verified, real-time doorstep delivery of petroleum products (Super Euro-V Petrol, High-Speed Diesel, High-Octane 97) capped strictly at 15 Liters with silent retail pump markup (+Rs. 5.00/L over base OGRA rates) and tiered delivery fees.
- **Operating Scope:** Official delivery hours enforced:
  - <font color="#10b981"><b>Mon–Thu:</b> 08:00 AM – 08:00 PM PKT</font>
  - <font color="#10b981"><b>Fri:</b> 08:00 AM – 01:00 PM PKT</font>
  - <font color="#10b981"><b>Sat–Sun:</b> 10:00 AM – 06:00 PM PKT</font>
- **Product Status:** *Pure Water and LPG Gas delivery are currently UNAVAILABLE.*

---

## 2. Tech Stack
| Layer | Choice | Why |
|---|---|---|
| Front-end / UI | Kotlin + Jetpack Compose (Material 3) | Declarative UI, reactive state binding, responsive layout, smooth animations |
| Architecture | Android MVVM with StateFlow & Coroutines | Clean separation of concerns, reactive unidirectional data flow |
| Local Database | Android Room (SQLite) with KSP | Offline-first caching, ACID compliance, zero-boilerplate entity mappings |
| Cloud Database | Firebase Cloud Firestore | Multi-client live sync for orders, rider GPS telematics, and audit logs |
| Telematics & Maps | Google Maps Android SDK + OpenStreetMap | Live driver bowser GPS tracking, route polylines, ETA updates |
| Pricing Grounding | Google Gemini 2.0 Flash + Trackmate API | Real-time OGRA Pakistan official retail prices with fallback AI grounding |
| Communications | Authenticated SMTP / Webhook + FCM | Multi-channel transactional notifications & automated HTML invoices |
| Security | Android Keystore + BiometricPrompt + RootDetector | Hardware-backed AES-256 GCM encryption, biometric auth, anti-tamper |
| Git Telemetry | Pure Code-Based Trading-Style Activity Engine | Code churn & shift magnitude analysis via Mermaid & Terminal in README (Zero SVG) |

---

## 3. System Data Flow
```
[ Customer / Admin / Rider UI (Jetpack Compose) ]
                      |  1. User Action / State Observation
                      v
            [ MainViewModel (StateFlow) ]
                      |  2. Repository Orchestration
                      v
        [ AppRepository (Data Layer) ]
         /                            \
        v                              v
[ Room SQLite Database ]    [ Firebase Firestore / OkHttp APIs ]
(users, orders, audit_logs)  (Live Driver GPS, Gemini Grounding, Trackmate)
```

---

## 4. Fuel & Service Pricing Architecture
- **Base OGRA Rates:** Sourced periodically via Trackmate API / Gemini Grounding.
- <font color="#10b981"><b>Retail Pump Rate Markup (+Rs. 5.00/L)</b>: Fixed standard markup applied silently to <b>Petrol</b>, <b>Diesel</b>, and <b>High-Octane</b> (`basePrice + FeeConstants.PUMP_RATE_MARKUP`). Zero debug text displayed.</font>
- <font color="#10b981"><b>Tiered Delivery Fees</b>: 1..10L (Flat Rs. 300), 11L (Rs. 320), 12L (Rs. 340), 13L (Rs. 360), 14L (Rs. 380), 15L max cap (Rs. 400).</font>
- **Volume Cap:** Strictly limited to 15 Liters per order.

---

## 5. Third-Party Integrations
- **Google Play Services:** Location Services (FusedLocationProviderClient), Maps SDK, In-App Reviews.
- **Firebase:** Cloud Firestore, Firebase Auth, Firebase Cloud Messaging.
- **AI Grounding:** Google Gemini 2.0 Flash REST API for dynamic rate grounding.
- **Email:** Webhook relay / SMTP Gateway for real-time customer and admin order confirmation invoices.
