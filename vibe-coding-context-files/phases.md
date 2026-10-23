# Zyphuel Project Phases & Milestones

> **Current App Version:** `2.6.4.0.0.33 (Build 61)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-23</b></font>  
> **Status:** <font color="#10b981"><b>Build Verified (37/38 Tests Passing — 1 Pre-Existing Unrelated Failure)</b></font>

---

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
- [x] Production service categories and vehicle garages.
- [x] My Vehicles garage manager with primary vehicle binding.
- [x] Saved delivery addresses and geofenced coverage verification.

## Phase 4 — Petrol Pump Rate Engine & Production Hardening
**Goal:** Transparent retail petrol pump pricing and Google Play Store compliance.
- [x] Implement standard retail pump markup (+Rs. 5.00/L) for Petrol, Diesel, and High-Octane.
- [x] Biometric security, root detection, and AES-256 GCM encrypted storage.
- [x] Permanent Account Deletion in compliance with Play Store User Data policies.
- [x] Play Store ASO optimization, SEO schema markup, and bilingual support.

## <font color="#10b981"><b>Phase 5 — Build 52 Operating Hours Gate, Tiered Rates & Trading Graph (2026-10-09)</b></font>
**Goal:** Regulatory delivery time gating, tiered doorstep logistics pricing, and automated git telemetry.
- [x] <font color="#10b981"><b>Operating Hours Gate</b>: Mon–Thu 08:00 AM – 08:00 PM, Fri 08:00 AM – 01:00 PM, Sat–Sun 10:00 AM – 06:00 PM PKT enforcement.</font>
- [x] <font color="#10b981"><b>Per-Liter Tiered Delivery Fee</b>: 1..10L (Flat Rs. 300), 11L (Rs. 320), 12L (Rs. 340), 13L (Rs. 360), 14L (Rs. 380), 15L max cap (Rs. 400).</font>
- [x] <font color="#10b981"><b>Silent Retail Markup</b>: `PUMP_RATE_MARKUP = 5.00` Rs./Litre applied over base OGRA rates silently in background.</font>
- [x] <font color="#10b981"><b>Trading-Style GitHub Activity Graph</b>: High-frequency repository analyzer tracking 1D, 1W, and 1M velocity, commit intervals, and trendlines.</font>
- [x] <font color="#10b981"><b>Product Availability Gate</b>: Water and LPG Gas permanently blocked.</font>

## <font color="#10b981"><b>Phase 6 — Build 61 Data Preservation & Auth Hardening (2026-10-23)</b></font>
**Goal:** Zero user-data loss across any app update; email + password as the only credential path.
- [x] <font color="#10b981"><b>Room DB v13 Migration</b>: `MIGRATION_12_TO_13` (schema-preserving); `fallbackToDestructiveMigration()` permanently removed.</font>
- [x] <font color="#10b981"><b>Error 422 Brute-Force Lockout</b>: 4 wrong passwords in 1 minute → 5-minute cooldown across all auth paths.</font>
- [x] <font color="#10b981"><b>Email OTP Password Reset</b>: single-use 6-digit code, 5-minute expiry, closes email+phone-only takeover path.</font>
- [x] <font color="#10b981"><b>Biometric Password Verification</b>: query-based account bypass removed; stored password re-verified on fingerprint sign-in.</font>
- [x] <font color="#10b981"><b>Case-Insensitive Duplicate Email Guard</b>: `LOWER(email)` matching blocks duplicate signups in any letter-case.</font>
