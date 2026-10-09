# Findings: Delivery Pricing, Pump Rate Markup, Operating Hours & Trading Graph

> **Current App Version:** `2.6.4.0.0.24 (Build 52)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

## Implementation Verification State

1. **Delivery Fees**:
   - Implemented in `FeeConstants.calculateFuelDeliveryFee`:
     - <font color="#10b981"><b>1..10L (5L, 7L, 10L):</b> Flat Rs. 300 Delivery Fee</font>
     - <font color="#10b981"><b>11L:</b> Rs. 320 Delivery Fee</font>
     - <font color="#10b981"><b>12L:</b> Rs. 340 Delivery Fee</font>
     - <font color="#10b981"><b>13L:</b> Rs. 360 Delivery Fee</font>
     - <font color="#10b981"><b>14L:</b> Rs. 380 Delivery Fee</font>
     - <font color="#10b981"><b>15L:</b> Rs. 400 Delivery Fee (Strict Max Cap)</font>
   - Max cap is strictly 15L (`FUEL_MAX_LITERS = 15`).
   - Water and LPG are strictly unavailable.

2. **Markup Calculation**:
   - `FeeConstants.PUMP_RATE_MARKUP = 5.00` (Rs./Litre) applied on top of official OGRA ex-depot base rate for:
     - Super Euro-V Petrol
     - Euro-V Diesel
     - High-Octane 97
   - Display ONLY final pump rate in UI. Formula: `finalRate = ograBase + 5.00`.
   - Math runs silently in background, zero debug/markup text.

3. **Operating Hours**:
   - Implemented via `DeliveryOperatingHoursManager` in PKT (`Asia/Karachi`):
     - <font color="#10b981"><b>Mon–Thu:</b> 08:00 AM – 08:00 PM (08:00 – 20:00 PKT)</font>
     - <font color="#10b981"><b>Fri:</b> 08:00 AM – 01:00 PM (08:00 – 13:00 PKT)</font>
     - <font color="#10b981"><b>Sat–Sun:</b> 10:00 AM – 06:00 PM (10:00 – 18:00 PKT)</font>
   - Enforcement:
     - Order action buttons and FAB are dynamically hidden when delivery window is closed.
     - Database and ViewModel reject any order placed when closed.

4. **App Versioning**:
   - Released: <font color="#10b981"><b>`versionCode = 52`</b></font>, <font color="#10b981"><b>`versionName = "2.6.4.0.0.24"`</b></font>.

5. **Trading-Style GitHub Activity Graph**:
   - Generates high-res vector SVG at `.github/assets/repo-activity-chart.svg`.
   - Embeds 1D, 1W, and 1M velocity and interval tracking directly into `README.md`.
   - Pre-commit hook `.git/hooks/pre-commit` and `github.bat` keep graph continuously updated.
