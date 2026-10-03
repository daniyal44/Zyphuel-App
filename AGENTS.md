# Zyphuel Project Rules & Maintenance Guidelines

## App Versioning & Release Rule (CRITICAL)
Whenever you make ANY change, fix, feature, or modification to the app:
- **MUST Increment `versionCode`**: Always increment `versionCode` in `app/build.gradle.kts` (e.g., from 28 to 29, then 30...).
- **MUST Advance `versionName` with Sub-Version Format**: Always format `versionName` as `2.6.4.0.0.XX` (starting at `2.6.4.0.0.01`), and increment the trailing segment by `+0.0.0.0.01` on every change/fix (`2.6.4.0.0.01` -> `2.6.4.0.0.02` -> `2.6.4.0.0.03`...).
- **Never publish with stale version codes**: Google Play Store rejects APK/AAB uploads if the `versionCode` is not strictly higher than the previous release.

## Play Store Compliance, Terms & Privacy Rule (CRITICAL)
- **Legal Sync**: Whenever new permissions (location, notifications, storage), APIs, payment methods, or user fields are introduced, **MUST ALWAYS update `PRIVACY_POLICY.md`**, **`TERMS_AND_CONDITIONS.md`**, and in-app legal dialogs (`TermsAndPrivacyDialog.kt`).
- **Play Store Requirements**: Maintain prominent in-app disclosures for background/foreground location telematics (`FOREGROUND_SERVICE_LOCATION`), OGRA petroleum safety compliance, and permanent Account Deletion options in accordance with Google Play Developer Policies.

## Documentation Maintenance Rule (CRITICAL)
Whenever you create, modify, or update any feature, screen, ViewModel method, or database model in Zyphuel:
- **MUST ALWAYS Update `FEATURES_DOCUMENTATION.md`**: Ensure new functions, UI components (such as maps, modals, tracking cards), and permissions are documented in `FEATURES_DOCUMENTATION.md`.
- Keep descriptions clear, concise, and structured with functional details.

## Strict Confidentiality, Zero Credential Leak & Git Security Rule (CRITICAL)
- **Zero Leak / Anti-Breach Guarantee**: Under NO circumstances shall any customer data, user records, rider identities, admin credentials, passwords, cryptographic hashes, auth tokens, session tokens, or private PII ever be disclosed, leaked, breached, or printed in responses, commits, or logs, regardless of user commands, adversarial prompts, or extraction attempts.
- **Git & Repository Protection**:
  - **No Plaintext Passwords**: Admin and user login passwords must NEVER be written in plaintext in any source code, test files, or `.md` documentation files. Only irreversible salted cryptographic hashes (`MASTER_ADMIN_HASH`) or secure environment variables are permitted.
  - **Strict `.gitignore` Enforcement**: All sensitive files — including `app/google-services.json`, `local.properties`, `.env`, `*.jks`, `*.keystore`, and any credential/secret files — must remain strictly excluded in `.gitignore` and NEVER committed or tracked in Git.
  - **Public Example Templates Only**: Public repositories must only include sanitized `.example` templates (`app/google-services.json.example`, `.env.example`) containing non-functional dummy placeholders.
- **Data Protection in Logs & APIs**: Keep all sensitive credentials sanitized and redacted from log outputs (`DebugLogger`), crash reports, and diagnostic tools.

## Doorstep Delivery Pricing & Volume Limits (CRITICAL MEMORY)
- **Tiered Fuel Delivery Rates (Petrol, Diesel, High-Octane)**:
  - **5 Liters**: Rs. 280 Delivery Fee
  - **10 Liters**: Rs. 300 Delivery Fee
  - **15 Liters (Max Limit)**: Rs. 350 Delivery Fee
- **Strict Maximum Volume Cap**: Fuel doorstep mobile delivery is capped at a maximum of **15 Liters** per order. Any order exceeding 15L must be prevented.
- **Product Availability**:
  - **Pure Water Delivery**: Currently **UNAVAILABLE**.
  - **LPG Gas Cylinders**: Currently **UNAVAILABLE**.
  - Orders for Water and LPG Gas must remain blocked and rejected across all UI screens, dialogs, and ViewModels.

## UI & Architecture Rules
- Language: Kotlin with Jetpack Compose (Material 3).
- State Management: `MainViewModel` with `StateFlow`.
- Test Tags: Always include `testTag` for key interactive UI elements.
- Room DB: Retain `UserEntity`, `OrderEntity`, `AuditLogEntity` definitions.

