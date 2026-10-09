# Zyphuel Changelog & Modifications (`changes.md`)

## Version 2.6.4.0.0.24 (Release Build 52) — <font color="#10b981"><b>2026-10-09</b></font>

### 1. Per-Liter Tiered Fuel Delivery Pricing Engine (<font color="#10b981"><b>2026-10-09</b></font>)
- **Granular Per-Liter Tiered Rates:** Updated [`FeeConstants.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/FeeConstants.kt) to enforce exact doorstep delivery charges:
  - **1 to 10 Liters (including 5L, 7L, 10L)**: <font color="#10b981"><b>Rs. 300.00</b></font> (Flat Rs. 300 for up to 10L)
  - **11 Liters**: <font color="#10b981"><b>Rs. 320.00</b></font>
  - **12 Liters**: <font color="#10b981"><b>Rs. 340.00</b></font>
  - **13 Liters**: <font color="#10b981"><b>Rs. 360.00</b></font>
  - **14 Liters**: <font color="#10b981"><b>Rs. 380.00</b></font>
  - **15 Liters (Strict Max Limit Cap)**: <font color="#10b981"><b>Rs. 400.00</b></font>
- **Linear Step Formula:** Implemented in `calculateFuelDeliveryFee(liters)` as `300.00 + (liters - 10) * 20.00` for $11 \le \text{liters} \le 15$.
- **Strict 15L Safety Cap:** Mobile doorstep delivery remains capped at a maximum of 15 Liters per order; orders exceeding 15L are rejected across all dialogs and ViewModels.

### 2. Standardized Silent OGRA Pump Rate Markup Constant (<font color="#10b981"><b>2026-10-09</b></font>)
- **Markup Constant Definition:** Defined `FeeConstants.PUMP_RATE_MARKUP = 5.00` (Double) and `FeeConstants.PUMP_RATE_MARKUP_FLOAT = 5.00f` (Float).
- **Universal Application:** Applied uniformly over official OGRA ex-depot base notifications for:
  - Super Euro-V Petrol (`finalRate = ograBase + 5.00`)
  - Euro-V High-Speed Diesel (`finalRate = ograBase + 5.00`)
  - High-Octane 97 HOBC (`finalRate = ograBase + 5.00`)
- **Silent Background Execution:** The retail markup math executes silently in the background. UI screens, cards, dialogs, and invoice breakdowns display **strictly the final pump rate** without any debug labels or surcharge annotations.

### 3. Delivery Operating Hours Window Enforcement & UI Gate (<font color="#10b981"><b>2026-10-09</b></font>)
- **Operating Hours Manager:** Created [`DeliveryOperatingHoursManager.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/DeliveryOperatingHoursManager.kt) calibrated to Pakistan Standard Time (`Asia/Karachi`, UTC+5):
  - **Monday – Thursday**: <font color="#10b981"><b>08:00 AM – 08:00 PM PKT</b></font>
  - **Friday**: <font color="#10b981"><b>08:00 AM – 01:00 PM PKT</b></font>
  - **Saturday – Sunday**: <font color="#10b981"><b>10:00 AM – 06:00 PM PKT</b></font>
- **UI Button Hiding When Closed:**
  - **Home Screen FAB**: "Order Now" ExtendedFloatingActionButton (`home_fab`) is completely hidden when closed.
  - **Order Dialog Confirmation Button**: "Place Delivery Order / Confirm Order COD" (`confirmButton`) is completely omitted from the layout when closed.
  - **Customer Order Card**: "Reorder 🔁" button on past orders is hidden when closed.
  - **Category Fuel Cards**: Clicking fuel cards while closed displays an informative Toast indicating operating hours.
- **Backend & Database Hard Guard:** `MainViewModel.placeOrder()` enforces `isDeliveryOpen` check, blocking off-hours submissions and notifying the user.
- **Live Status Card:** Added live open/closed status banner and schedule summary to [`CustomerHomeScreen`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/ui/Screens.kt).

### 4. Real-Time Trading-Style Codebase Movement Graph (<font color="#10b981"><b>2026-10-09</b></font>)
- **Pure Code-Based Telemetry Engine:** Built [`scripts/generate_github_graph.js`](file:///d:/Games/New%20folder-web/Claude/scripts/generate_github_graph.js) generating:
  - Dynamic code churn analysis (`git log --numstat`), evaluating line additions/deletions and categorizing changes by scale: **Major Shift**, **Moderate Shift**, or **Minor Shift**.
  - Multi-timeframe tracking across **1D (Daily)**, **1W (Weekly)**, and **1M (Monthly Macro)** timeframes.
  - Native GitHub Mermaid `xychart-beta`, Mermaid `gitGraph`, and interactive ASCII/Unicode Trading Terminal directly in [`README.md`](file:///d:/Games/New%20folder-web/Claude/README.md).
  - Incline / Decline trend momentum indicators (`▲ BULLISH SURGE`, `◄ CONSOLIDATION`).
  - Exact push timing, cadence, and interval tracking.
- **Automated Update Pipeline:** Installed Git pre-commit hook ([`.git/hooks/pre-commit`](file:///d:/Games/New%20folder-web/Claude/.git/hooks/pre-commit)) and upgraded [`github.bat`](file:///d:/Games/New%20folder-web/Claude/github.bat) so the graph automatically regenerates and embeds into [`README.md`](file:///d:/Games/New%20folder-web/Claude/README.md) on every commit.

### 5. Application Micro-Versioning & Compliance (<font color="#10b981"><b>2026-10-09</b></font>)
- **`app/build.gradle.kts`**:
  - `versionCode`: incremented from 51 to <font color="#10b981"><b>52</b></font>.
  - `versionName`: advanced from `2.6.4.0.0.23` to <font color="#10b981"><b>2.6.4.0.0.24</b></font>.
- **Legal Synchronization**: Updated [`TERMS_AND_CONDITIONS.md`](file:///d:/Games/New%20folder-web/Claude/TERMS_AND_CONDITIONS.md), [`PRIVACY_POLICY.md`](file:///d:/Games/New%20folder-web/Claude/PRIVACY_POLICY.md), and in-app legal viewer (`TermsAndPrivacyDialog.kt`) to reflect Build 52, tiered delivery pricing, and delivery operating hours.
- **Test Suite**: Verified via `FuelPumpPricingTest.kt`, `ZyphuelComprehensiveAppTest`, and `CategoryAndVehicleArchitectureTest` with 100% test pass (0 failures).
