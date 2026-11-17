# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Zyphuel is a dual-platform fuel delivery app (Android Kotlin/Compose + iOS Swift/SwiftUI) for on-demand fuel delivery in Lahore, Pakistan. Three user roles: **Customer** (orders fuel), **Rider** (delivers fuel), **Admin** (manages everything). The Android app is the primary codebase; iOS mirrors it feature-for-feature.

## Build & Test Commands

```bash
# Android — compile check (fastest gate)
./gradlew :app:compileDebugKotlin

# Android — full unit test suite (54 Robolectric tests)
./gradlew :app:testDebugUnitTest

# Android — single test class
./gradlew :app:testDebugUnitTest --tests "com.example.SecurityAndDataIntegrityTest"

# Android — debug APK
./gradlew :app:assembleDebug

# Android — release bundle (needs KEYSTORE_PATH / STORE_PASSWORD / KEY_PASSWORD)
./gradlew :app:bundleRelease

# iOS — generate Xcode project + build
cd ios && ./bootstrap.sh test

# Version bump (both platforms simultaneously)
sync_versions.bat --bump
# or: python scripts/sync_versions.py --bump

# Version drift check (used in CI)
python scripts/sync_versions.py --check
```

Test reports land in `app/build/reports/tests/testDebugUnitTest/index.html`.

## Architecture

**Android** follows MVVM with a single `MainViewModel` (~4700 lines) that owns all UI state as `StateFlow`s. All Compose screens observe it and call its methods. There is no navigation library — screen routing is a `StateFlow<String>` (`currentScreen`) switched inside `AnimatedContent` in `MainActivity.kt`.

**Data layer**: `AppRepository` wraps a Room database (v13, `Models.kt`) and two Firestore repositories (`FirestoreUserRepository`, `FirestoreOrderRepository`). Room is the local source of truth; Firestore syncs cross-device in real time via snapshot listeners.

**iOS** mirrors Android's structure: `ZyphuelConfig.swift` holds shared constants, `Core/Services/` holds Firestore/API services, `Features/` holds SwiftUI views grouped by domain.

### Key data flows

**Fuel prices**: OGRA-set defaults (SharedPreferences `zyphuel_prices`) → overwritten by Firestore real-time listener (`system_config/fuel_rates`) → background refresh via `FuelPriceWorker` (WorkManager, 4-hour cycle) calling `TrackmateFuelApiService` (7-source fallback chain). `PRICE_CACHE_VERSION` in MainViewModel init forces cache refresh when OGRA changes rates.

**Authentication**: Email+password only → PBKDF2-HMAC-SHA256 hashing in `SecurityCrypto.kt` → session stored in `EncryptedSharedPreferences` via `SecureStorageManager` → optional biometric unlock. Rate-limited by `SecurityRateLimiter` (4 attempts, 5-min cooldown).

**Orders**: Customer places order → Room insert + Firestore write → Rider sees in real-time → status updates flow back via Firestore snapshot listeners → synced to Room for offline access.

### Tech stack versions (from `gradle/libs.versions.toml`)

AGP 9.1.1, Kotlin 2.2.10, Compose BOM 2024.09.00, Room 2.7.0, Firebase BOM 34.15.0, OkHttp 4.10.0, targetSdk 36. iOS uses Swift 6.x, Xcode 26.x, Firebase iOS SDK 13.x, XcodeGen for project generation.

## Critical Rules

**Cross-platform parity**: Every change MUST land on both Android (`app/`) and iOS (`ios/`) in the same commit. A one-platform change is incomplete. See `AGENTS.md` for the full rule.

**Version bumping**: The single source of truth is `version.properties`. Never edit version numbers by hand in `build.gradle.kts` or `project.yml` — always use `sync_versions.bat --bump`.

**Room DB migrations**: Every schema version bump MUST have a registered `Migration` in BOTH `AppDatabase.getDatabase()` (Models.kt) and the `AppRepository` constructor (Repository.kt). The current version is 13 with `MIGRATION_12_TO_13`.

**Secrets**: `google-services.json`, `.env`, `*.jks`, `*.keystore`, and any credential files must stay in `.gitignore` and never be committed. API keys come from `BuildConfig` via the Secrets Gradle Plugin reading `.env`. The SMTP app password is injected via `SMTP_APP_PASSWORD` build secret.

**NEVER delete user data**: Fixes and updates must NEVER clear, delete, or reset user data — this includes the Room database (`zyphuel_database`), SharedPreferences files (`zyphuel_prices`, `zyphuel_session`, `zyphuel_ui_prefs`, `zyphuel_categories_v2`), EncryptedSharedPreferences (admin enclave), or Firestore documents (orders, user profiles, `system_config/*`). Users have real orders, login sessions, and payment history that cannot be recreated. If a migration or fix requires data changes, it must be additive or transformative — never destructive. The `CrashRecoveryScreen` "Clear Data" button is the ONLY acceptable path for data clearing, and it requires explicit user consent.

**SharedPreferences keys for fuel prices**: `petrol`, `diesel`, `high_octane` in the `zyphuel_prices` preference file. The iOS equivalent mirrors these key names in `UserDefaults`.

## File Layout (non-obvious parts)

`Screens.kt` (19,000+ lines) contains ALL Compose screen implementations — `SplashScreen`, `AuthScreen`, `CustomerHomeScreen`, `RiderHomeScreen`, `AdminDashboardScreen`, `TrackerScreen`, etc. — in a single file.

`MainViewModel.kt` has three `init {}` blocks: (1) security + fuel price cache init, (2) Firestore cloud sync + session restore, (3) category price sync + cross-device order sync.

`TrackmateFuelApiService.kt` implements the 7-source fuel price fallback: OilPrices.pk API → TrackMate API → HamariWeb → GlobalPetrolPrices → Google Search → PSO → Shell. PSO and Shell are JS SPAs that return empty HTML shells to server-side fetchers.

`CategoryRepository.kt` / `CategoryModels.kt` define the service catalog (fuel types, pricing tiers). Prices are computed from base OGRA rate + `FeeConstants.PETROL_PUMP_RATE_SURCHARGE`.

`RealtimeEmailEngine.kt` sends transactional emails (order confirmations, invoices) via direct SMTP — no third-party email service.

## CI Workflows

`android-build.yml` — compiles + runs unit tests on every push. `ios-build.yml` — builds via XcodeGen on macOS. `version-sync.yml` — enforces version parity between platforms. `version-bump.yml` — auto-bumps on merge to main. `update-graph.yml` — regenerates the codebase velocity chart in README.

## Crash Diagnostics

`crash_debug.bat` captures Logcat output and the app's internal `crash_log.txt` (written by the global `Thread.UncaughtExceptionHandler` in `MainActivity`). `DebugLogger` sanitizes sensitive data from all log output.
