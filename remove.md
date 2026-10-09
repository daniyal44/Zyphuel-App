# Zyphuel Deprecations & Removals (`remove.md`)

## Version 2.6.4.0.0.24 Removals & Deprecations — <font color="#10b981"><b>2026-10-09</b></font>

### 1. Deprecated Off-Hour Order Placement (<font color="#10b981"><b>2026-10-09</b></font>)
- **Deprecated 24/7 Unrestricted Fuel Ordering:** Deprecated continuous 24/7 fuel order placement. Doorstep delivery is now restricted strictly to official operational hours:
  - **Mon–Thu**: <font color="#10b981"><b>08:00 AM – 08:00 PM PKT</b></font>
  - **Fri**: <font color="#10b981"><b>08:00 AM – 01:00 PM PKT</b></font>
  - **Sat–Sun**: <font color="#10b981"><b>10:00 AM – 06:00 PM PKT</b></font>
- **Deprecated Direct Off-Hour Submissions:** In [`MainViewModel.placeOrder()`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/ui/MainViewModel.kt#L1040), order placement requests initiated outside operational hours are rejected with user feedback.

### 2. Deprecated Surcharge Labels & Debug Notes (<font color="#10b981"><b>2026-10-09</b></font>)
- **Deprecated `+Rs 5.00/L` Markup Labels in UI:** Removed user-facing surcharge tags and debug strings from the order flow.
- **Silent Retail Markup:** Retail margins over official OGRA ex-depot base rates (`PUMP_RATE_MARKUP = 5.00` Rs./L) are now processed transparently and silently in the background, showing only the final pump rate to customers.

### 3. Deprecated Legacy Delivery Fee Bands (<font color="#10b981"><b>2026-10-09</b></font>)
- **Deprecated Old Fee Structure:** The legacy delivery fee bands (Rs. 250 flat, Rs. 280 for 5L, Rs. 350 for 15L) have been deprecated.
- **Standardized Per-Liter Tiered Fees:** Replaced with the official tiered delivery rates in [`FeeConstants.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/FeeConstants.kt):
  - 1 to 10 Liters (including 5L, 7L, 10L): <font color="#10b981"><b>Rs. 300.00</b></font>
  - 11 Liters: <font color="#10b981"><b>Rs. 320.00</b></font>
  - 12 Liters: <font color="#10b981"><b>Rs. 340.00</b></font>
  - 13 Liters: <font color="#10b981"><b>Rs. 360.00</b></font>
  - 14 Liters: <font color="#10b981"><b>Rs. 380.00</b></font>
  - 15 Liters (Strict Max Cap): <font color="#10b981"><b>Rs. 400.00</b></font>

### 4. Deprecated Static / Non-Trading Git Charts (<font color="#10b981"><b>2026-10-09</b></font>)
- **Deprecated Static SVG Graphs:** Deprecated uninformative static activity diagrams in favor of the real-time **Trading-Style Movement Engine** ([`.github/assets/repo-activity-chart.svg`](file:///d:/Games/New%20folder-web/Claude/.github/assets/repo-activity-chart.svg)) tracking movements across Daily (1D), Weekly (1W), and Monthly (1M) timeframes, commit volumes, push timing, push intervals, and incline/decline trends.
