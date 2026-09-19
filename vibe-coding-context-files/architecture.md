# Zyphuel System Architecture

> Solution-level design for Zyphuel: On-Demand Doorstep Fuel, Energy & Mobile Auto Care Delivery Platform (Lahore, Pakistan).

## 1. Overview
- **Product:** Zyphuel — On-demand doorstep fuel (Super Petrol, High-Speed Diesel, High-Octane HOBC 97), sealed LPG cylinders, pure drinking water, and roadside auto mechanics across Lahore.
- **Core value:** Safe, verified, real-time doorstep delivery of petroleum products at transparent official petrol pump rates (+Rs. 2.50/L petrol pump surcharge over base OGRA rates) with live GPS bowser telematics.
- **Scale target (v1):** 50,000 active customer households and fleet commercial accounts across Lahore operational sectors (Gulberg, DHA, Johar Town, Bahria Town, Model Town, Cantt).

## 2. Tech Stack
| Layer | Choice | Why |
|-------|--------|-----|
| Front-end / UI | Kotlin + Jetpack Compose (Material 3) | Declarative UI, reactive state binding, responsive layout, smooth animations |
| Architecture | Android MVVM with StateFlow & Coroutines | Clean separation of concerns, reactive unidirectional data flow |
| Local Database | Android Room (SQLite) with KSP | Offline-first caching, ACID compliance, zero-boilerplate entity mappings |
| Cloud Database | Firebase Cloud Firestore | Multi-client live sync for orders, rider GPS telematics, and audit logs |
| Telematics & Maps | Google Maps Android SDK + OpenStreetMap | Live driver bowser GPS tracking, route polylines, ETA updates |
| Pricing Grounding | Google Gemini 2.0 Flash + Trackmate API | Real-time OGRA Pakistan official retail prices with fallback AI grounding |
| Communications | Authenticated JavaMail (SMTP) + Firebase Cloud Messaging (FCM) | Dual-channel transactional notifications & automated HTML invoices |
| Security | Android Keystore + BiometricPrompt + RootDetector | Hardware-backed AES-256 GCM encryption, biometric auth, anti-tamper |

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

## 4. Fuel & Service Pricing Architecture
- **Base OGRA Rates:** Sourced periodically via Trackmate API / Gemini Grounding.
- **Retail Petrol Pump Rate (+Rs. 2.50/L):** Fixed standard petrol pump surcharge applied to **Petrol**, **Diesel**, and **High-Octane** (`basePrice + FeeConstants.PETROL_PUMP_RATE_SURCHARGE`).
- **Fixed Delivery Fees:** Rs. 250.00 for Fuel/Gas orders; Rs. 50.00 for pure drinking water orders. Multi-item orders (>= 2 items) receive a 50% delivery fee discount.

## 5. Third-Party Integrations
- **Google Play Services:** Location Services (FusedLocationProviderClient), Maps SDK, In-App Reviews.
- **Firebase:** Cloud Firestore, Firebase Auth, Firebase Cloud Messaging.
- **AI Grounding:** Google Gemini 2.0 Flash REST API for dynamic rate grounding.
- **Email:** Hostinger SMTP Gateway for real-time customer and admin order confirmation invoices.
