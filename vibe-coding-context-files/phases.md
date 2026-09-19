# Zyphuel Project Phases & Milestones

> Progressive delivery roadmap for Zyphuel.

## Phase 1 — Core On-Demand Fuel MVP
**Goal:** Deliver end-to-end fuel ordering with local Room persistence and role-based access.
- [x] Customer, Rider, and Admin authentication with biometric verification.
- [x] Fuel order creation with cash on delivery payment.
- [x] Interactive customer order tracking with ETA updates.
- [x] Automated dual HTML order invoice generation via Hostinger SMTP.

## Phase 2 — Real-Time Logistics & Rate Grounding
**Goal:** Live bowser GPS tracking and AI price sync.
- [x] Google Maps & OpenStreetMap live telematics for bowser vehicle positioning.
- [x] Periodic background price sync via `FuelPriceWorker` and Gemini 2.0 Flash grounding.
- [x] Push notifications for status updates and live rate broadcasts.

## Phase 3 — Multi-Category Expansion & Vehicle Fleet
**Goal:** Auto care, emergency roadside SOS, and customer vehicle profiles.
- [x] 10 production service categories (Fuel, Auto Repair, Roadside SOS, Detailing, etc.).
- [x] My Vehicles garage manager with primary vehicle binding.
- [x] Saved delivery addresses and geofenced coverage verification.

## Phase 4 — Petrol Pump Rate Engine & Production Hardening
**Goal:** Transparent retail petrol pump pricing and Google Play Store compliance.
- [x] Implement standard **+Rs. 2.50/L Petrol Pump Rate** surcharge for Petrol, Diesel, and High-Octane.
- [x] Update Order page, OrderDialog, and category checkouts with live pump rate calculations.
- [x] Biometric security, root detection, and AES-256 GCM encrypted storage.
- [x] Permanent Account Deletion in compliance with Play Store User Data policies.
- [x] Play Store ASO optimization, SEO schema markup, and bilingual support (Urdu/English).
