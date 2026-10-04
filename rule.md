# 🛡️ Zyphuel Security & Operational Rules (`rule.md`)

## 1. Zero Credential & Secret Leak Policy (CRITICAL - STRICT PRIORITY)
* **No Plaintext Passwords or Credentials**:
  - Admin login passwords, user account passwords, and authentication secrets must **NEVER** be committed to Git or pushed to GitHub in plaintext.
  - Passwords must be handled exclusively via irreversible salted cryptographic hashes (`MASTER_ADMIN_HASH`) or local environment variables.
  - Documentation (`.md` files) must never contain actual passwords, private tokens, or credential strings.
* **Strict Git Tracking Exclusions**:
  - The following files must remain permanently excluded in `.gitignore` and must never be tracked in the repository:
    - `app/google-services.json` and any Google API configuration files.
    - `local.properties` and `/local.properties`.
    - `.env` and `.env.*` configuration files.
    - `*.jks`, `*.keystore`, and Android upload signing keys (`my-upload-key.jks`, `debug.keystore`).
    - Any file matching `*credentials*`, `*secret*`, `*admin_login*`, or `*service_account*`.
  - For new clones and open-source contributors, provide only sanitized `.example` templates (e.g. `app/google-services.json.example`, `.env.example`) containing non-functional dummy placeholders.

## 2. App Versioning & Release Rule (CRITICAL)
Whenever any modification, fix, security update, or feature is applied:
- **Increment `versionCode`**: Always increment `versionCode` in `app/build.gradle.kts` (e.g., from 49 to 50, then 51...).
- **Advance `versionName` with Sub-Version Format**: Format as `2.6.4.0.0.XX`, incrementing the trailing segment by `+0.0.0.0.01` on every change (`2.6.4.0.0.22` -> `2.6.4.0.0.23`...).
- Never publish or push with stale version codes.

## 3. Play Store Compliance & Legal Documentation
- **Legal Synchronization**: Update `PRIVACY_POLICY.md`, `TERMS_AND_CONDITIONS.md`, and `TermsAndPrivacyDialog.kt` whenever permissions, APIs, or user data handling changes.
- **Prominent Disclosures**: Maintain disclosures for foreground location telematics (`FOREGROUND_SERVICE_LOCATION`), OGRA petroleum safety rules, and permanent Account Deletion options in compliance with Google Play Developer Policies.

## 4. Doorstep Fuel Delivery & Safety Cap Limits
- **Strict Maximum Cap**: Doorstep mobile fuel delivery (Petrol, Diesel, High-Octane) is strictly capped at **15 Liters** per order. Any order exceeding 15L must be prevented.
- **Tiered Delivery Rates**:
  - **1 to 5 Liters**: Rs. 280.00 Delivery Fee
  - **6 to 10 Liters**: Rs. 300.00 Delivery Fee
  - **11 to 15 Liters (Max Limit)**: Rs. 350.00 Delivery Fee
- **Product Availability**:
  - Pure Drinking Water: **UNAVAILABLE** (blocked across all UI and order flows).
  - LPG Gas Cylinders: **UNAVAILABLE** (blocked across all UI and order flows).

## 5. Architectural & Coding Standards
- UI: Kotlin with Jetpack Compose (Material 3).
- State Management: `MainViewModel` with `StateFlow`.
- Room Database: Maintain `UserEntity`, `OrderEntity`, `AuditLogEntity`, and `NotificationEntity`.
- Testing: All key interactive UI elements must include Compose `testTag`.
