# 🛡️ Zyphuel Security, Architecture & Operational Rules (`rule.md`)

**Last Updated:** <font color="#10b981"><b>2026-10-09</b></font> • **Application Version:** <font color="#10b981"><b>2.6.4.0.0.25 (Build 53)</b></font>

---

## 1. Zero Credential & Secret Leak Policy (CRITICAL - STRICT PRIORITY)
* **No Plaintext Passwords or Credentials**:
  - Admin login passwords, user account passwords, authentication tokens, and private session secrets must **NEVER** be committed to Git or pushed to GitHub in plaintext.
  - Passwords must be handled exclusively via irreversible salted cryptographic hashes (`MASTER_ADMIN_HASH`) or secure environment variables.
  - Documentation (`.md` files) must never contain actual passwords, private tokens, or credential strings.
* **Strict Git Tracking Exclusions**:
  - The following files must remain permanently excluded in `.gitignore` and must never be tracked in the repository:
    - `app/google-services.json` and any Google API configuration files.
    - `local.properties` and `/local.properties`.
    - `.env` and `.env.*` configuration files.
    - `*.jks`, `*.keystore`, and Android upload signing keys (`my-upload-key.jks`, `debug.keystore`).
    - Any file matching `*credentials*`, `*secret*`, `*admin_login*`, or `*service_account*`.
  - For new clones and open-source contributors, provide only sanitized `.example` templates (e.g. `app/google-services.json.example`, `.env.example`) containing non-functional dummy placeholders.

---

## 2. App Versioning & Release Rule (CRITICAL — Updated <font color="#10b981"><b>2026-10-09</b></font>)
Whenever any modification, fix, security update, or feature is applied:
- **MUST Increment `versionCode`**: Always increment `versionCode` in [`app/build.gradle.kts`](file:///d:/Games/New%20folder-web/Claude/app/build.gradle.kts) (currently at <font color="#10b981"><b>52</b></font>).
- **MUST Advance `versionName` with Sub-Version Format**: Format as `2.6.4.0.0.XX`, incrementing the trailing segment by `+0.0.0.0.01` on every change (`2.6.4.0.0.24` -> <font color="#10b981"><b>2.6.4.0.0.25</b></font>).
- Never publish or push with stale version codes.

---

## 3. Play Store Compliance & Legal Documentation
- **Legal Synchronization**: Update [`PRIVACY_POLICY.md`](file:///d:/Games/New%20folder-web/Claude/PRIVACY_POLICY.md), [`TERMS_AND_CONDITIONS.md`](file:///d:/Games/New%20folder-web/Claude/TERMS_AND_CONDITIONS.md), and in-app legal dialogs (`TermsAndPrivacyDialog.kt`) whenever permissions, APIs, or user data handling changes.
- **Prominent Disclosures**: Maintain disclosures for foreground location telematics (`FOREGROUND_SERVICE_LOCATION`), OGRA petroleum safety rules, and permanent Account Deletion options in compliance with Google Play Developer Policies.

---

## 4. Doorstep Delivery Pricing, Markup & Operating Hours (CRITICAL — Updated <font color="#10b981"><b>2026-10-09</b></font>)
- **Per-Liter Tiered Fuel Delivery Rates**:
  - **1 to 10 Liters (including 5L, 7L, 10L)**: <font color="#10b981"><b>Rs. 300.00 Delivery Fee</b></font> (Flat Rs. 300 for up to 10L)
  - **11 Liters**: <font color="#10b981"><b>Rs. 320.00 Delivery Fee</b></font>
  - **12 Liters**: <font color="#10b981"><b>Rs. 340.00 Delivery Fee</b></font>
  - **13 Liters**: <font color="#10b981"><b>Rs. 360.00 Delivery Fee</b></font>
  - **14 Liters**: <font color="#10b981"><b>Rs. 380.00 Delivery Fee</b></font>
  - **15 Liters (Strict Max Cap)**: <font color="#10b981"><b>Rs. 400.00 Delivery Fee</b></font>
- **Strict Maximum Volume Cap**: Fuel doorstep mobile delivery is strictly capped at **15 Liters** per order. Any order exceeding 15L must be prevented.
- **Retail Pump Rate Markup Constant**:
  - `PUMP_RATE_MARKUP = 5.00` (Rs./Litre) applied on top of official OGRA ex-depot base rates for Super Euro-V Petrol, Euro-V Diesel, and High-Octane 97 (`finalRate = ograBase + 5.00`).
  - Silent background execution: only the final pump rate is shown in the UI (zero visible surcharge labels or debug notes).
- **Official Delivery Operating Hours & Order Gate**:
  - **Monday – Thursday**: <font color="#10b981"><b>08:00 AM – 08:00 PM PKT</b></font>
  - **Friday**: <font color="#10b981"><b>08:00 AM – 01:00 PM PKT</b></font>
  - **Saturday – Sunday**: <font color="#10b981"><b>10:00 AM – 06:00 PM PKT</b></font>
  - Outside operating hours: "Order Now" action button and "Confirm Order COD" modal buttons are hidden from the UI, and order submission is blocked at the ViewModel and database layers.
- **Product Availability**:
  - Pure Drinking Water: **UNAVAILABLE** (blocked across all UI screens and order flows).
  - LPG Gas Cylinders: **UNAVAILABLE** (blocked across all UI screens and order flows).

---

## 5. Architectural & Coding Standards
- UI Stack: Kotlin with Jetpack Compose (Material 3).
- State Management: `MainViewModel` with Kotlin `StateFlow`.
- Room Database Schema: Maintain `UserEntity`, `OrderEntity`, `AuditLogEntity`, and `NotificationEntity` table definitions.
- Testing Tags: All key interactive UI components must include Compose `testTag` for automated test suites.

---

## 6. Trading-Style Codebase Velocity Graph Maintenance Rule (CRITICAL — Updated <font color="#10b981"><b>2026-10-09</b></font>)
- **Pure Code-Based Trading Graph (Zero Static SVG Files)**: The repository activity graph on GitHub must be purely code-based:
  - Driven directly by codebase churn analysis (`git log --numstat`), evaluating line additions/deletions and files modified.
  - Dynamically classifies the scale of every update: **Major Shift**, **Moderate Shift**, or **Minor Shift**.
  - Tracking movement across **Daily (1D)**, **Weekly (1W)**, and **Monthly (1M)** timeframes.
  - Recording commit volume, timing of pushes, intervals between pushes, and incline/decline trends.
  - Visualized via native GitHub Mermaid `xychart-beta`, Mermaid `gitGraph`, and interactive ASCII/Unicode Trading Terminal directly in [`README.md`](file:///d:/Games/New%20folder-web/Claude/README.md).
- **Automatic Execution on Every Code Update**:
  - Maintained via [`scripts/generate_github_graph.js`](file:///d:/Games/New%20folder-web/Claude/scripts/generate_github_graph.js).
  - Triggered via Git pre-commit hook ([`.git/hooks/pre-commit`](file:///d:/Games/New%20folder-web/Claude/.git/hooks/pre-commit)), [`github.bat`](file:///d:/Games/New%20folder-web/Claude/github.bat), and GitHub Actions ([`.github/workflows/update-graph.yml`](file:///d:/Games/New%20folder-web/Claude/.github/workflows/update-graph.yml)).
