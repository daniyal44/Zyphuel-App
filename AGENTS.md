# Zyphuel Project Rules & Maintenance Guidelines

## App Versioning & Release Rule (CRITICAL)
Whenever you make ANY change, fix, feature, or modification to the app:
- **MUST Increment `versionCode`**: Always increment `versionCode` in `app/build.gradle.kts` (e.g., from 4 to 5, then 6...).
- **MUST Advance `versionName`**: Update the semantic version in `app/build.gradle.kts` (e.g., 2.3.1, 2.3.2...).
- **Never publish with stale version codes**: Google Play Store rejects APK/AAB uploads if the `versionCode` is not strictly higher than the previous release.

## Play Store Compliance, Terms & Privacy Rule (CRITICAL)
- **Legal Sync**: Whenever new permissions (location, notifications, storage), APIs, payment methods, or user fields are introduced, **MUST ALWAYS update `PRIVACY_POLICY.md`**, **`TERMS_AND_CONDITIONS.md`**, and in-app legal dialogs (`TermsAndPrivacyDialog.kt`).
- **Play Store Requirements**: Maintain prominent in-app disclosures for background/foreground location telematics (`FOREGROUND_SERVICE_LOCATION`), OGRA petroleum safety compliance, and permanent Account Deletion options in accordance with Google Play Developer Policies.

## Documentation Maintenance Rule (CRITICAL)
Whenever you create, modify, or update any feature, screen, ViewModel method, or database model in Zyphuel:
- **MUST ALWAYS Update `FEATURES_DOCUMENTATION.md`**: Ensure new functions, UI components (such as maps, modals, tracking cards), and permissions are documented in `FEATURES_DOCUMENTATION.md`.
- Keep descriptions clear, concise, and structured with functional details.

## UI & Architecture Rules
- Language: Kotlin with Jetpack Compose (Material 3).
- State Management: `MainViewModel` with `StateFlow`.
- Test Tags: Always include `testTag` for key interactive UI elements.
- Room DB: Retain `UserEntity`, `OrderEntity`, `AuditLogEntity` definitions.

