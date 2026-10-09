# Findings: Delivery Pricing, Pump Rate Markup & Operating Hours

## Current Implementation State
1. **Delivery Fees**:
   - Currently implemented in `FeeConstants.calculateFuelDeliveryFee`: 1..5L = Rs. 280, 6..10L = Rs. 300, 11..15L = Rs. 350.
   - Max cap is strictly 15L (`FUEL_MAX_LITERS = 15`).
   - Water and LPG are unavailable.

2. **Markup Calculation**:
   - `FeeConstants.PETROL_PUMP_RATE_SURCHARGE` is currently 5.00.
   - User specified naming: `PUMP_RATE_MARKUP = 5.00` (Rs./Litre), applied on top of official OGRA ex-depot base rate for:
     - Super Euro-V Petrol
     - Euro-V Diesel
     - High-Octane 97
   - Display ONLY final pump rate in UI. Formula: `finalRate = ograBase + 5.00`.
   - Math runs silently in background, zero debug/markup text.

3. **Operating Hours**:
   - Currently no operating hours time check exists in `MainViewModel.placeOrder()` or `CustomerHomeScreen`.
   - Requested schedule:
     - Mon–Thu: 08:00 AM – 08:00 PM (08:00 – 20:00 PKT)
     - Fri: 08:00 AM – 01:00 PM (08:00 – 13:00 PKT)
     - Sat–Sun: 10:00 AM – 06:00 PM (10:00 – 18:00 PKT)
   - Enforcement:
     - Order button must NOT show when closed.
     - No order can be placed after time over.

4. **App Versioning**:
   - Current: `versionCode = 51`, `versionName = "2.6.4.0.0.23"`.
   - Target: `versionCode = 52`, `versionName = "2.6.4.0.0.24"`.
