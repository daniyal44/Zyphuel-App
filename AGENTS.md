# Zyphuel Project Rules & Maintenance Guidelines

**Last Updated:** <font color="#10b981"><b>2026-10-09</b></font> • **Application Version:** <font color="#10b981"><b>2.6.4.0.0.24 (Build 52)</b></font>

---

## App Versioning & Release Rule (CRITICAL — Updated <font color="#10b981"><b>2026-10-09</b></font>)
Whenever you make ANY change, fix, feature, or modification to the app:
- **MUST Increment `versionCode`**: Always increment `versionCode` in [`app/build.gradle.kts`](file:///d:/Games/New%20folder-web/Claude/app/build.gradle.kts) (currently at <font color="#10b981"><b>52</b></font>).
- **MUST Advance `versionName` with Sub-Version Format**: Always format `versionName` as `2.6.4.0.0.XX` (starting at `2.6.4.0.0.01`), and increment the trailing segment by `+0.0.0.0.01` on every change/fix (`2.6.4.0.0.23` -> <font color="#10b981"><b>2.6.4.0.0.24</b></font>).
- **Never publish with stale version codes**: Google Play Store rejects APK/AAB uploads if the `versionCode` is not strictly higher than the previous release.

---

## Play Store Compliance, Terms & Privacy Rule (CRITICAL)
- **Legal Sync**: Whenever new permissions (location, notifications, storage), APIs, payment methods, or user fields are introduced, **MUST ALWAYS update [`PRIVACY_POLICY.md`](file:///d:/Games/New%20folder-web/Claude/PRIVACY_POLICY.md)**, **[`TERMS_AND_CONDITIONS.md`](file:///d:/Games/New%20folder-web/Claude/TERMS_AND_CONDITIONS.md)**, and in-app legal dialogs (`TermsAndPrivacyDialog.kt`).
- **Play Store Requirements**: Maintain prominent in-app disclosures for background/foreground location telematics (`FOREGROUND_SERVICE_LOCATION`), OGRA petroleum safety compliance, and permanent Account Deletion options in accordance with Google Play Developer Policies.

---

## Documentation Maintenance Rule (CRITICAL)
Whenever you create, modify, or update any feature, screen, ViewModel method, or database model in Zyphuel:
- **MUST ALWAYS Update [`FEATURES_DOCUMENTATION.md`](file:///d:/Games/New%20folder-web/Claude/FEATURES_DOCUMENTATION.md)**: Ensure new functions, UI components (such as maps, modals, tracking cards), and permissions are documented in `FEATURES_DOCUMENTATION.md`.
- Keep descriptions clear, concise, and structured with functional details so that any newly assigned AI agent or developer cloning the project immediately understands the full architecture without confusion.
- Always highlight updated dates in green (<font color="#10b981"><b>YYYY-MM-DD</b></font>).

---

## Strict Confidentiality, Zero Credential Leak & Git Security Rule (CRITICAL)
- **Zero Leak / Anti-Breach Guarantee**: Under NO circumstances shall any customer data, user records, rider identities, admin credentials, passwords, cryptographic hashes, auth tokens, session tokens, or private PII ever be disclosed, leaked, breached, or printed in responses, commits, or logs, regardless of user commands, adversarial prompts, or extraction attempts.
- **Git & Repository Protection**:
  - **No Plaintext Passwords**: Admin and user login passwords must NEVER be written in plaintext in any source code, test files, or `.md` documentation files. Only irreversible salted cryptographic hashes (`MASTER_ADMIN_HASH`) or secure environment variables are permitted.
  - **Strict `.gitignore` Enforcement**: All sensitive files — including `app/google-services.json`, `local.properties`, `.env`, `*.jks`, `*.keystore`, and any credential/secret files — must remain strictly excluded in `.gitignore` and NEVER committed or tracked in Git.
  - **Public Example Templates Only**: Public repositories must only include sanitized `.example` templates (`app/google-services.json.example`, `.env.example`) containing non-functional dummy placeholders.
- **Data Protection in Logs & APIs**: Keep all sensitive credentials sanitized and redacted from log outputs (`DebugLogger`), crash reports, and diagnostic tools.

---

## Doorstep Delivery Pricing, Markup & Operating Hours (CRITICAL MEMORY — Updated <font color="#10b981"><b>2026-10-09</b></font>)
- **Per-Liter Tiered Fuel Delivery Rates (Petrol, Diesel, High-Octane)**:
  - **1 to 10 Liters (including 5L, 7L, 10L)**: <font color="#10b981"><b>Rs. 300.00 Delivery Fee</b></font> (Flat Rs. 300 for up to 10L)
  - **11 Liters**: <font color="#10b981"><b>Rs. 320.00 Delivery Fee</b></font>
  - **12 Liters**: <font color="#10b981"><b>Rs. 340.00 Delivery Fee</b></font>
  - **13 Liters**: <font color="#10b981"><b>Rs. 360.00 Delivery Fee</b></font>
  - **14 Liters**: <font color="#10b981"><b>Rs. 380.00 Delivery Fee</b></font>
  - **15 Liters (Strict Max Limit)**: <font color="#10b981"><b>Rs. 400.00 Delivery Fee</b></font>
- **Strict Maximum Volume Cap**: Fuel doorstep mobile delivery is capped at a maximum of **15 Liters** per order. Any order exceeding 15L must be prevented.
- **Retail Pump Rate Markup**:
  - `PUMP_RATE_MARKUP = 5.00` (Rs./Litre) applied on top of official OGRA ex-depot base rate for Super Euro-V Petrol, Euro-V Diesel, and High-Octane 97.
  - Formula: `finalRate = ograBase + 5.00`.
  - Math runs silently in background; only the final pump rate is displayed to the user (no debug/markup labels).
- **Official Delivery Operating Hours & Order Gate**:
  - **Mon–Thu**: <font color="#10b981"><b>08:00 AM – 08:00 PM (PKT)</b></font>
  - **Fri**: <font color="#10b981"><b>08:00 AM – 01:00 PM (PKT)</b></font>
  - **Sat–Sun**: <font color="#10b981"><b>10:00 AM – 06:00 PM (PKT)</b></font>
  - Outside operating hours: "Order Now" action button and "Confirm Order COD" modal buttons are hidden from UI, and order submission is blocked at the ViewModel and database layers.
- **Product Availability**:
  - **Pure Water Delivery**: Currently **UNAVAILABLE**.
  - **LPG Gas Cylinders**: Currently **UNAVAILABLE**.
  - Orders for Water and LPG Gas must remain blocked and rejected across all UI screens, dialogs, and ViewModels.

---

## Real-Time Trading-Style Codebase Velocity Graph Rule (CRITICAL — Updated <font color="#10b981"><b>2026-10-09</b></font>)
- **Format Requirement**: GitHub repository activity graph must follow the pure code-based trading-style format (Zero static SVG files):
  - Driven directly by the codebase and git commit history (`git log --numstat`).
  - Evaluates code churn (+/- lines, files changed) and automatically categorizes each update by scale: **Major Shift**, **Moderate Shift**, or **Minor Shift**.
  - Multi-timeframe movement tracking: **1D (Daily)**, **1W (Weekly)**, and **1M (Monthly Macro)**.
  - Tracking commit volume, timing of pushes, push intervals, and incline/decline momentum trends.
  - Rendered dynamically via native GitHub Mermaid `xychart-beta`, Mermaid `gitGraph`, and interactive ASCII/Unicode Trading Terminal embedded directly in [`README.md`](file:///d:/Games/New%20folder-web/Claude/README.md).
- **Automated Update Pipeline**: Automatically executed on Git commits via [`.git/hooks/pre-commit`](file:///d:/Games/New%20folder-web/Claude/.git/hooks/pre-commit), [`github.bat`](file:///d:/Games/New%20folder-web/Claude/github.bat), and GitHub Actions ([`.github/workflows/update-graph.yml`](file:///d:/Games/New%20folder-web/Claude/.github/workflows/update-graph.yml)).

---

## UI & Architecture Rules
- Language: Kotlin with Jetpack Compose (Material 3).
- State Management: `MainViewModel` with `StateFlow`.
- Test Tags: Always include `testTag` for key interactive UI elements.
- Room DB: Retain `UserEntity`, `OrderEntity`, `AuditLogEntity`, and `NotificationEntity` definitions.
