# Task Plan: Delivery Pricing Update, Pump Rate Markup, Operating Hours Gate & Trading Activity Graph

> **Current App Version:** `2.6.4.0.0.24 (Build 52)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

## Goals
1. Implement updated per-liter delivery rates (5L: Rs. 300, 7L: Rs. 300, 10L: Rs. 300, 11L: Rs. 320, 12L: Rs. 340, 13L: Rs. 360, 14L: Rs. 380, 15L: Rs. 400).
2. Standardize `PUMP_RATE_MARKUP = 5.00` (Rs./L) over official OGRA ex-depot base rates for Super Euro-V Petrol, Euro-V Diesel, and High-Octane 97, running silently in the background with zero debug/markup text shown to users.
3. Enforce strict operating delivery hours (Mon–Thu 08:00 AM – 08:00 PM, Fri 08:00 AM – 01:00 PM, Sat–Sun 10:00 AM – 06:00 PM PKT). Outside these hours: hide order placement buttons and reject any order placement attempts.
4. Implement a high-frequency trading-style GitHub activity graph engine tracking 1D, 1W, and 1M velocity, commit cadence, and incline/decline trends with auto-update hooks.
5. Synchronize all tests, documentation, and app versioning (versionCode 52, versionName 2.6.4.0.0.24).

## Phases
- [x] Phase 1: Research & Codebase Analysis (<font color="#10b981"><b>2026-10-09</b></font>)
- [x] Phase 2: User Approval on Implementation Plan (<font color="#10b981"><b>2026-10-09</b></font>)
- [x] Phase 3: Core Implementation (FeeConstants, DeliveryOperatingHoursManager, MainViewModel, UI & Dialogs) (<font color="#10b981"><b>2026-10-09</b></font>)
- [x] Phase 4: Verification & Test Execution (<font color="#10b981"><b>2026-10-09</b></font>)
- [x] Phase 5: Documentation & Version Bump (<font color="#10b981"><b>2026-10-09</b></font>)
- [x] Phase 6: Trading-Style Git Activity Graph Engine & Full Markdown Sync (<font color="#10b981"><b>2026-10-09</b></font>)

## Decisions
| Decision | Rationale | Date |
|---|---|---|
| Use Asia/Karachi timezone for `DeliveryOperatingHoursManager` | Zyphuel operates in Lahore, Punjab; timezone must be consistent regardless of user device roaming or system clock offset. | <font color="#10b981"><b>2026-10-09</b></font> |
| Delivery fee formula: 1..10L = Rs. 300, 11..15L = 300 + (liters - 10) * 20 | Matches all user points exactly: 5L(300), 7L(300), 10L(300), 11L(320), 12L(340), 13L(360), 14L(380), 15L(400). | <font color="#10b981"><b>2026-10-09</b></font> |
| Hide FAB & order actions when closed | User requirement: "order button will not show on after time over, no oder place". Both UI button hiding and ViewModel hard guard checks ensure 100% compliance. | <font color="#10b981"><b>2026-10-09</b></font> |
| High-frequency Trading-Style Activity Chart | SVG vector rendering + 1D/1W/1M velocity breakdown + automated git hooks guarantees auto-updating activity metrics on every commit. | <font color="#10b981"><b>2026-10-09</b></font> |
