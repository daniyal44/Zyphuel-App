# Zyphuel Application - Features Documentation (`docs/Feature.md`)

**Last Updated:** <font color="#10b981"><b>2026-10-09</b></font> • **Application Version:** <font color="#10b981"><b>2.6.4.0.0.24 (Build 52)</b></font>

---

## 1. Granular Per-Liter Tiered Fuel Delivery Pricing (<font color="#10b981"><b>2026-10-09</b></font>)
- **Official Per-Liter Doorstep Rate Schedule**:
  - **1 to 10 Liters (including 5L, 7L, 10L)**: <font color="#10b981"><b>Rs. 300.00 Delivery Fee</b></font> (Flat Rs. 300 for up to 10L)
  - **11 Liters**: <font color="#10b981"><b>Rs. 320.00 Delivery Fee</b></font>
  - **12 Liters**: <font color="#10b981"><b>Rs. 340.00 Delivery Fee</b></font>
  - **13 Liters**: <font color="#10b981"><b>Rs. 360.00 Delivery Fee</b></font>
  - **14 Liters**: <font color="#10b981"><b>Rs. 380.00 Delivery Fee</b></font>
  - **15 Liters (Strict Max Limit Cap)**: <font color="#10b981"><b>Rs. 400.00 Delivery Fee</b></font>
- **Linear Step Formula**: `300.00 + (liters - 10) * 20.00` for $11 \le \text{liters} \le 15$.
- **Strict 15L Public Safety Cap**: Doorstep mobile fuel delivery is strictly capped at a maximum of 15 Liters per order under OGRA safety rules; higher volumes are rejected.

---

## 2. Standardized Silent OGRA Pump Rate Markup Constant (<font color="#10b981"><b>2026-10-09</b></font>)
- **Markup Constant**: `FeeConstants.PUMP_RATE_MARKUP = 5.00` (Rs./Litre) applied on top of official OGRA ex-depot base rates for Super Euro-V Petrol, Euro-V Diesel, and High-Octane 97 (`finalRate = ograBase + 5.00`).
- **Silent Background Processing**: The math runs silently in the background; UI screens, dialogs, cards, and tax invoices display strictly the final pump rate without debug labels or surcharge annotations.

---

## 3. Delivery Operating Hours Window Enforcement & UI Gate (<font color="#10b981"><b>2026-10-09</b></font>)
- **Official Delivery Windows (Pakistan Standard Time, `Asia/Karachi`)**:
  - **Monday – Thursday**: <font color="#10b981"><b>08:00 AM – 08:00 PM PKT</b></font>
  - **Friday**: <font color="#10b981"><b>08:00 AM – 01:00 PM PKT</b></font>
  - **Saturday – Sunday**: <font color="#10b981"><b>10:00 AM – 06:00 PM PKT</b></font>
- **UI Button Hiding & Backend Hard Guards**:
  - The "Order Now" FAB (`home_fab`) on `CustomerHomeScreen` is completely omitted when closed.
  - The "Confirm Order COD" button (`confirmButton`) in `OrderDialog` is completely omitted when closed.
  - The "Reorder 🔁" button on past orders is hidden when closed.
  - Tapping fuel cards when closed displays a Toast informing the user of operational hours.
  - Hard guards in `MainViewModel.placeOrder()` reject off-hour submissions.
  - Live status card on `CustomerHomeScreen` informs users whether delivery is currently open or closed.

---

## 4. Real-Time Trading-Style Codebase Velocity & Movement Engine (<font color="#10b981"><b>2026-10-09</b></font>)
- **Pure Code-Based Dynamic Visualization (Zero Static SVG Files)**: Driven directly by the codebase and git commit churn analysis (`git log --numstat`), evaluating additions/deletions and categorizing updates by scale: **Major Shift**, **Moderate Shift**, or **Minor Shift**.
- **Multi-Timeframe Movement Tracking**: Tracks velocity across **1D (Daily)**, **1W (Weekly)**, and **1M (Monthly Macro)** timeframes with native GitHub Mermaid `xychart-beta`, Mermaid `gitGraph`, and ASCII/Unicode Trading Terminal directly in [`README.md`](file:///d:/Games/New%20folder-web/Claude/README.md).
- **Cadence & Push Interval Tracking**: Records the exact interval between pushes (e.g. ~1.8h–2.4h active sprint cadence vs. multi-day consolidation intervals).
- **Incline / Decline Trend Indicators**: Surfaces velocity surges (`▲ BULLISH SURGE`) and consolidation windows.
- **Automated Sync Pipeline**: Automated execution on git commits via `.git/hooks/pre-commit`, `github.bat`, and GitHub Actions.

---

## 5. 10-Category Marketplace & Saved Vehicle Profiles
- **Comprehensive Mobility Catalog**: 10 top-level categories and 35+ subcategories covering fuel, emergency roadside SOS, mobile detailing, auto repair, tyre services, and battery jumpstarts.
- **"My Vehicles" Profile Vault**: Room DB `VehicleEntity` storing multiple customer vehicles with fuel types and tank capacities for instant profile selection.
- **Typo-Tolerant Search**: Levenshtein edit-distance matching across all services with instant autocomplete suggestions.

---

## 6. Multi-Role Navigation & Security Architecture
- **Multi-Role Portals**: Role-segregated navigation for Customer, Rider (bowser dispatch console), and Administrator (master supervisory controls).
- **Universal Biometric Authentication**: AndroidX `BiometricPrompt` supporting fingerprint and face unlock for both Google OAuth and manual logins.
- **Play Store Data Safety & Compliance**: Prominent disclosures for foreground telematics (`FOREGROUND_SERVICE_LOCATION`), in-app permanent Account Deletion (`deleteCurrentAccount`), and zero plaintext credential storage.
