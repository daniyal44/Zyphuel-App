# Zyphuel Deprecations & Removals (`remove.md`)

## Version 2.6.4.0.0.11 Removals & Deprecations

### 1. Replaced Legacy Hardcoded Rates
- **Removed Old Flat Base Rates:** Replaced outdated fallback defaults (Petrol Rs. 275.60, Diesel Rs. 284.20) with updated market-aligned rates (Base Petrol Rs. 289.38 -> Pump Rate Rs. 291.88; Base Diesel Rs. 289.84 -> Pump Rate Rs. 292.34).
- **Removed Static Price Computations in Order Screen:** Deprecated unadjusted `petrolPrice` multiplier in `OrderDialog` in favor of dynamic `petrolPumpPrice` (+Rs. 2.50/L).

### 2. Deprecated Category Surcharge Inconsistencies
- **Replaced Raw OGRA Fuel Subcategory Pricing:** In `CategoryRepository.syncLiveFuelPrices()`, raw ex-depot prices were previously mapped directly to fuel subcategories. This has been deprecated and replaced with official petrol pump retail rates (`basePrice + FeeConstants.PETROL_PUMP_RATE_SURCHARGE`).

### 3. Deprecated Direct Price Multipliers
- In `CategoryDetailModal`, direct fallback references to `livePetrol` without pump surcharge have been removed.

### 4. UI Cleanups
- Removed ambiguous "OGRA Rate" badge on customer-facing order cards where the retail pump price was actually being charged, replacing it with transparent `Pump Rate (+Rs 2.50)` disclosure.
