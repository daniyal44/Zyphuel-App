# Zyphuel Changelog & Modifications (`changes.md`)

## Version 2.6.4.0.0.11 (Release Build 39) - September 2026

### 1. Petrol Pump Rate Engine (+Rs. 2.50 / Litre)
- **Petrol, Diesel & High-Octane Pump Rate Surcharge:** Implemented standard +Rs. 2.50 per litre petrol pump retail markup over official OGRA base prices.
- **Consistent Application:** Regardless of whether official prices decrease, increase, or remain unchanged, the +Rs. 2.50/L petrol pump rate is consistently computed across all order calculations.
- **LPG & Water Exemption:** Sealed LPG cylinders and pure drinking water remain strictly exempt from the petrol pump surcharge.
- **`FeeConstants.kt` Enhancements:**
  - Added `PETROL_PUMP_RATE_SURCHARGE = 2.50` (Double) and `PETROL_PUMP_RATE_SURCHARGE_FLOAT = 2.50f` (Float).
  - Added `isPumpRateApplicable(serviceType)` to identify eligible fuels.
  - Added `getPumpRate(basePrice, serviceType)` calculation helpers.

### 2. Order Page & Dialog Updates (`OrderDialog`)
- **Real-Time Petrol Pump Unit Rates:** Petrol, Diesel, and High-Octane cards now display the live retail petrol pump price (e.g. Rs. 291.88/L) with badge `(Pump Rate • incl. +Rs 2.5/L)`.
- **Dynamic Subtotal Calculation:** Item subtotals for fuel now compute accurately as `quantity * pumpPrice`.
- **Transparent Summary Breakdown:** Added explicit `Pump Rate Included (+Rs 2.50/L)` row in the order cost summary showing the total petrol pump surcharge included in the subtotal.
- **Grand Total Consistency:** Total price passed to `placeOrder()` accurately reflects the petrol pump rates.

### 3. Customer Home & Fuel Market Widget Updates
- **`CustomerHomeScreen` Quick Service Cards:** Petrol, Diesel, and High-Octane cards updated to display the petrol pump rates with `Pump Rate (+Rs 2.50)` labels.
- **`LahoreFuelMarketWidget` / `RatesScreen`:** Primary cards for Petrol and Diesel display the Petrol Pump Rate, with base OGRA price comparison shown underneath.

### 4. Category & ViewModel Reactive Data Layer
- **`MainViewModel.kt` StateFlows:**
  - Added `petrolPumpPrice: StateFlow<Float>`
  - Added `dieselPumpPrice: StateFlow<Float>`
  - Added `highOctanePumpPrice: StateFlow<Float>`
  - Added `getEffectiveFuelPumpPrice(serviceType): Float`
- **`CategoryRepository.kt`:**
  - Updated `syncLiveFuelPrices()` to synchronize the +Rs. 2.50 pump surcharge into all fuel subcategories (`petrol_regular`, `petrol_octane`, `diesel_regular`, `diesel_generator`).
- **`CategoryComponents.kt`:**
  - Updated Category Dashboard and `CategoryDetailModal` to bind directly to petrol pump prices.

### 5. Vibe Coding Context Files
- Imported and tailored all 8 architecture context files into `vibe-coding-context-files/`:
  - `README.md`
  - `architecture.md`
  - `database.md`
  - `error-handling.md`
  - `phases.md`
  - `prompts.md`
  - `security.md`
  - `generator-prompt.md`

### 6. App Versioning & Legal Terms
- `app/build.gradle.kts`:
  - `versionCode`: incremented from 38 to 39.
  - `versionName`: advanced from `2.6.4.0.0.10` to `2.6.4.0.0.11`.
- `TERMS_AND_CONDITIONS.md` & `PRIVACY_POLICY.md` & `TermsAndPrivacyDialog.kt`:
  - Updated with petrol pump retail rate disclosure (+Rs. 2.50/L).
- `FEATURES_DOCUMENTATION.md`:
  - Comprehensive documentation added for the Petrol Pump Rate engine.
