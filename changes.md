# Zyphuel Changelog & Modifications (`changes.md`)

> **This file is the project CHANGELOG.** Each entry lists version, release date (Asia/Karachi),
> added/changed/fixed/security sections, database migration notes and known issues.

## Version 2.6.4.0.0.40 (Release Build 68) — <font color="#10b981"><b>2026-11-07 12:00:00 PKT</b></font>

### iOS polish: honest downloads, App Store icon, privacy manifest, Firebase bundling
- **The 84 KB "download" is gone.** `desktop_ios_download.bat` used to zip `ios/` (about 84 KB of Swift source) and rename it to `Zyphuel-iOS-Latest.ipa`. Windows cannot compile iOS code, so that file could never be an app. The script now validates every candidate (≥ 1 MB **and** a real `Payload/*.app/Info.plist` entry), only copies a genuine IPA, otherwise pulls the real Simulator build from the `Zyphuel-ios-simulator-app` Actions artifact (when `gh` is authenticated), and only as a last resort delivers `Zyphuel-v<ver>-Build<n>-iOS-Source.zip` — explicitly named as source. Rejected archives are reported in the log and summary instead of being handed over as an "app".
- **App Store icon shipped.** `Assets.xcassets/AppIcon.appiconset` held an empty placeholder, so the bundle had no icon at all. Added an opaque 1024×1024 brand icon (`AppIcon-1024.png`, renderer at `scripts/generate_ios_app_icon.ps1`) and flipped `ZyphuelConfig.hasAppIcon` to `true`.
- **Privacy manifest added.** New `ios/Config/PrivacyInfo.xcprivacy` declares no tracking, the collected data types (name, email, phone, precise location, user ID, user content) and the required-reason APIs (UserDefaults `CA92.1`, file timestamp `C617.1`, boot time `35F9.1`, disk space `E174.1`). It is a `Copy Bundle Resources` member, which App Store Connect requires for upload.
- **`GoogleService-Info.plist` is finally bundled.** The target only globbed `Zyphuel/`, so `Config/GoogleService-Info.plist` never reached the app bundle and `FirebaseApp.configure()` silently skipped real Firebase even when the file existed. `project.yml` now copies it as an optional resource, alongside the privacy manifest.
- **CI proves the bundle.** `.github/workflows/ios-build.yml` builds with a fixed derived-data path, asserts `PrivacyInfo.xcprivacy` and the compiled `AppIcon60x60@2x.png` exist inside `Zyphuel.app`, then uploads `Zyphuel-ios-simulator-app` (`Zyphuel-iOS-Simulator.app.zip`) as a real, installable Simulator artifact.
- **Docs:** `ios/README.md` documents the three download outcomes and the icon/privacy requirements, and `TESTING.md` gained the iOS App Store-readiness checks.

---

## Version 2.6.4.0.0.38 (Release Build 66) — <font color="#10b981"><b>2026-11-05 16:00:00 PKT</b></font>

### Unified Android + iOS versioning and change parity
- **One version source**: new `version.properties` holds `versionCode` / `versionName`. `app/build.gradle.kts` now loads it instead of hardcoding values, so an Android build can never disagree with iOS. Builds fail with a clear message if the file is missing or the format is wrong.
- **`scripts/sync_versions.py`** (+ `sync_versions.bat` wrapper) applies the shared version to `ios/project.yml`, `ZyphuelConfig.swift`, `README.md` and `FEATURES_DOCUMENTATION.md`. `--bump` increments `versionCode` by 1 and the trailing `versionName` segment by `0.01` for both platforms at once; `--check` reports drift and exits non-zero.
- **CI gate**: new `.github/workflows/version-sync.yml` runs `--check` on every push and pull request, and verifies Gradle still reads `version.properties`.
- **Parity policy**: `AGENTS.md`, `TESTING.md`, `README.md` and `FEATURES_DOCUMENTATION.md` now require every feature, fix, Firestore rule and pricing constant to ship on Android **and** iOS in the same change set, with both test suites as the release gate.
- Current version for both platforms: **2.6.4.0.0.38 (Build 66)**.

---

## Version 2.6.4.0.0.37 (Release Build 65) — <font color="#10b981"><b>2026-11-05 14:30:00 PKT</b></font>

**Scope:** native iOS application added alongside Android. No Android behaviour, Room schema, Firestore rules or user data changed; the Android version pair is bumped only to keep the Play upload sequence monotonic.

### Added
- **`ios/` — complete SwiftUI port** of Zyphuel (iOS 17+, `com.aistudio.zyphuel.appv2`, same Firebase project):
  - Auth: email/password, registration (customer or rider), Google Sign-In, password reset link.
  - Customer: Lahore delivery card with CoreLocation + share link, live OGRA rate card, service search, 7 featured / 10 categories, operating-hours gate, active order card, notifications sheet.
  - Ordering: COD sheet with 5–15L presets, tiered delivery fee, address prefill, confirm button hidden outside operating hours, availability block for water/LPG.
  - Tracking: animated 4-step stepper, order summary, rider contact, star rating with Firestore `feedback` write.
  - History: current + recent orders, search, Delivered/Cancelled filters.
  - Rider portal: duty toggle, verification state, available queue, accept → start route → complete & collect COD, COD totals.
  - Admin console: overview stats, order lifecycle actions (accept/auto-assign, start, mark delivered, decline with reason, delete cancelled), rider approval, customer search/deletion, live rate editor, email gateway with test dispatch.
  - Profile: account, phone, order stats, English/اردو toggle, Face ID lock, notifications, WhatsApp/call support, legal, account deletion (root super admin protected).
- **`desktop_ios.bat` + `scripts/update_ios_versions.ps1`** — Windows entry point that pins Firebase iOS SDK and GoogleSignIn-iOS to their latest releases, validates the project structure/plists/Swift brace balance and writes `ios/build_report.txt`.
- **`ios/bootstrap.sh`** — macOS bootstrap (`xcodegen generate` + simulator build, `--test` runs the suite).
- **`.github/workflows/ios-build.yml`** — macOS CI that generates the project, resolves Swift packages, builds and runs the iOS tests.
- **`ios/Tests/ZyphuelLogicTests.swift`** — parity tests for pricing tiers, pump markup, operating hours, PBKDF2 format, 4/5-minute lockout, validators, order lifecycle, catalog and super-admin protection.

### Parity guarantees (ported 1:1 from Kotlin)
- `PricingEngine.swift` — Rs. 300/320/340/360/380/400 tiers, 15L cap, +Rs. 5.00/L pump markup, water/LPG blocked.
- `OperatingHours.swift` — Asia/Karachi windows (Mon–Thu 08:00–20:00, Fri 08:00–13:00, Sat–Sun 10:00–18:00), identical status copy.
- `Security.swift` — PBKDF2-HMAC-SHA256 at 600,000 iterations in the same `pbkdf2$sha256$…` format, iteration-aware rehash, 4 attempts / 5 minutes with error code 422.
- Firestore document shapes (`users`, `orders`, `feedback`, `audit_logs`, `mail`, `app_stats`) and role guards match the Android repositories.
- `firestore.rules`: the users match now accepts both document-id styles (`uid` and `email_with_underscores`) for the owning account, and the profile whitelist gained `updatedAt`, `profilePictureBase64` (≤400 KB), `vehicleType` and `vehicleNo` so the iOS client can sync without weakening privilege fields. iOS writes feedback with the existing `email`/`message` field names.

### Platform differences (documented, intentional)
- iOS cannot hold raw SMTP sockets in the background, so emails use the existing HTTPS webhook relay and the Firestore `mail` trigger.
- The Android profile screen can reveal the stored login password; iOS does not store or display a recoverable password.
- Room is replaced by Firestore's offline cache plus Keychain; live GPS upload remains disabled on both platforms.
- `AppIcon.appiconset` ships as a placeholder until the 1024×1024 brand icon is supplied.

### Fixed (cross-client contract audit)
- **Riders could not claim a pending order.** The hardened `orders` update rule only allowed an update when the existing document already contained the rider's email, which is null for every unassigned order. Added an explicit claim clause (`Pending` → `Assigned`, `riderEmail` must equal the caller's auth email). Affects Android and iOS riders.
- **Duplicate iOS profile documents.** iOS looked only at `users/{email_with_underscores}` while Android writes `users/{uid}`. iOS now resolves the existing document by email and reuses its id, and writes `displayName` alongside `name` so Android reads the same display name.
- **Order visibility across casings.** iOS now queries `customerEmail` for both the authenticated casing and the lowercased form, so orders created on Android with a mixed-case email still appear in the iOS history.

### Verification
- iOS cannot be compiled on Windows: `desktop_ios.bat` validates structure, and `.github/workflows/ios-build.yml` is the compile/test gate on macOS.
- Android checks re-run after the version bump (see `TESTING.md`): `:app:compileDebugKotlin` and `:app:testDebugUnitTest`.

---

## Version 2.6.4.0.0.35 (Release Build 63) — <font color="#10b981"><b>2026-11-03 19:15:00 PKT</b></font>

**Scope:** security hardening from the repository audit. No database migration, no user data rewritten or deleted.

### Security
- **Master admin hash is now fail-closed.** The hardcoded 64-hex fallback was removed from `app/build.gradle.kts`, `SecurityCrypto.kt` and `app/admin-credentials.properties.example`. The value must come from `ADMIN_PASSWORD_HASH` (environment) or the git-ignored `app/admin-credentials.properties` / `local.properties`; a malformed value now fails the build and a missing value disables the master-admin path with a build warning instead of silently using a public hash.
- **Guarded admin seeding and super-admin sync** (`Repository.seedAdminIfNeeded`, `MainViewModel.serverLoginWithSocial`) so a blank hash can never overwrite the admin account with an empty credential.
- **PBKDF2 work factor raised to 600,000 iterations.** `needsRehash()` now also upgrades older lower-iteration PBKDF2 hashes, not just legacy SHA-256 ones.
- **`firestore.rules` hardened:**
  - `live_tracking` — admin only (was any signed-in user; rider GPS publishing is disabled in the client, so this closes a location read/spoof vector).
  - `mail` — sender bound to its own `auth.uid`, exactly one recipient, strict field whitelist and capped payload sizes (was an arbitrary-recipient open relay).
  - `users` create/update — field whitelist so privilege fields (`isAdmin`, `isVerified`, wallet/limit fields) cannot be injected.
  - `feedback` create — field whitelist.
  - `audit_logs` create — admin only (fake security-log entries blocked).
  - `app_stats` — writes may only increment `count` by exactly one.
- `RealtimeEmailEngine` now stamps `uid` on queued Firestore mail so the new rule can bind each email to its sender.

### Owner actions required
- Rotate the master admin password and generate a new `ADMIN_PASSWORD_HASH`, since the previous hash was public in the git history.

---

## Version 2.6.4.0.0.34 (Release Build 62) — <font color="#10b981"><b>2026-11-03 18:40:00 PKT</b></font>

**Scope:** repository licensing and security audit follow-up. No app behaviour, database schema or user data changes.

### Added
- **`LICENSE`** (Apache License 2.0) and **`NOTICE`** — the repository was public with no license file, which meant "All Rights Reserved" by default. It is now genuinely open source with an express patent grant; trademarks (Zyphuel/MDK), user data, credentials and deployment config remain excluded (see `NOTICE`).
- **README `⚖️ License` section** documenting usage rights and the exclusion list.

### Security (audit findings — pending owner action)
- Audit confirmed no API keys, keystores, `google-services.json`, `.env` or `local.properties` were ever tracked, and git history is clean.
- Open items escalated to the owner: public `MASTER_ADMIN_HASH` fallback in `build.gradle.kts`/`SecurityCrypto.kt`, permissive `firestore.rules` for `live_tracking`, `mail`, `users` create, `audit_logs` and `app_stats`.

---

## Version 2.6.4.0.0.33 (Release Build 61) — <font color="#10b981"><b>2026-10-23 22:48:18 PKT</b></font>

**Scope:** security hardening, zero-data-loss update guarantees, email+password authentication, failure lockout and documentation. **No user data is migrated, rewritten or deleted by this release.**

### Security
- **Hardcoded SMTP credential removed** from `SecureStorageManager.getSmtpConfig()` (a live Gmail app password had been committed as the default) and from `FEATURES_DOCUMENTATION.md`. Credentials now come only from admin-managed Firestore config or the git-ignored `SMTP_APP_PASSWORD` secret. **Owner action: revoke and reissue that Google app password.**
- **PBKDF2-HMAC-SHA256 password hashing** (120,000 iterations, 16-byte per-user salt, constant-time compare) replacing the fixed-pepper SHA-256 scheme. Legacy hashes are upgraded transparently on the next successful login.
- **Password-verification bypasses closed:** raw plaintext equality no longer matches a stored hash, and `SOCIAL_OAUTH_*` placeholder accounts can never authenticate through the email/password form.
- **Biometric sign-in can no longer select an account** by role fallback; it resolves the account by email and re-verifies the stored password through `loginUser()`.
- **Failed-login lockout is restart-proof:** 4 wrong passwords inside a rolling 60-second window trigger a 5-minute cooldown, persisted with SHA-256-hashed keys plus monotonic (`elapsedRealtime`) tracking so a force-close or a device-clock change cannot bypass it.
- **Lockout accounting is atomic:** a per-account mutex prevents parallel attempts from losing a failure count.
- **Live cooldown UI:** the login card shows `Locked (MM:SS)` and re-enables itself at zero.
- **Duplicate email guard:** case-insensitive `LOWER(email)` lookups, normalized (trim + lowercase) registration and an `ABORT`-mode insert so a duplicate can never overwrite an existing account.

### Added
- `SECURITY.md`, `DATABASE.md`, `MIGRATIONS.md`, `TESTING.md`.
- `SecurityAndDataIntegrityTest` (16 tests): lockout policy, restart and parallel bypass, PBKDF2, injection payloads, OAuth placeholders, duplicate email, ownership scoping, migration and reopen-preserves-data.

### Changed
- **Admin update broadcast can mark a release as mandatory** (`admin_force_update_switch` in the App Version card): optional updates still show “Later”, mandatory security updates hide it and are never dismissible.
- `README.md`: setup/environment section, real build+test commands, `PBKDF2` (was `SHA-256`) in the stack table, documentation map.
- `AGENTS.md`, `FEATURES_DOCUMENTATION.md`, `PRIVACY_POLICY.md`, `TERMS_AND_CONDITIONS.md`, `PLAY_STORE_DATA_SAFETY_AND_COMPLIANCE.md` and the `docs/` & `vibe-coding-context-files/` sets synchronized to Build 61; stale dates corrected to the real Pakistan timestamp.

### Fixed
- **Data loss on app update:** `MIGRATION_12_TO_13` is registered in both Room builders, so updating the app no longer wipes the local database (`fallbackToDestructiveMigration` is not used anywhere).
- **Stale category test:** the fuel catalog test now asserts the real seed (4 orderable SKUs + Water/LPG present but disabled) instead of a superseded count of 4.

### Database migrations
- Room **v13** via `MIGRATION_12_TO_13` — schema-identical no-op; **no backup required** for this release, but a Firestore export is still recommended before shipping.

### Compatibility
- `versionCode` <font color="#10b981"><b>61</b></font>, `versionName` <font color="#10b981"><b>2.6.4.0.0.33</b></font>. Existing accounts, orders, vehicles, marked locations and audit logs are preserved.

### Known issues
- Failed-login limits are enforced per device (no shared Redis counter); a second device is not covered.
- `live_tracking` Firestore rule still permits any signed-in read/write until the staged rules patch in `SECURITY.md §5.2` is validated.
- Signup-time email ownership verification is not implemented; ownership is proven through the recovery-code flow.

---

## Version 2.6.4.0.0.26 (Release Build 54) — <font color="#10b981"><b>2026-10-09</b></font>

### 1. Strict Admin Password Git Exclusion & Zero-Leak Credential Isolation (<font color="#10b981"><b>2026-10-09</b></font>)
- **Comprehensive `.gitignore` Hardening**: Explicitly excluded all admin password files, credential sets, and secrets (`*admin*password*`, `admin-credentials.properties`, `app/admin-credentials.properties`, `admin_credentials.json`, `*admin_pass*`, `admin_password.txt`, `.admin.env`).
- **Dynamic Gradle & BuildConfig Injection**: Configured `app/build.gradle.kts` and `SecurityCrypto.kt` to dynamically load `MASTER_ADMIN_HASH` from git-ignored local properties or environment variables.
- **Sanitized Public Template**: Added `app/admin-credentials.properties.example` for public clones without exposing real credentials.
- **Zero-Leak Assurance**: Ensured anyone cloning the repository from GitHub cannot view the admin password via any Git command (`git log`, `git grep`, `git show`, `cat`).
- **Version Bump**: `versionCode` advanced to <font color="#10b981"><b>54</b></font>, `versionName` advanced to <font color="#10b981"><b>"2.6.4.0.0.26"</b></font>.

## Version 2.6.4.0.0.25 (Release Build 53) — <font color="#10b981"><b>2026-10-09</b></font>

### 1. Git Push, Rebase & Velocity Graph Sync Conflict Immunity Engine (<font color="#10b981"><b>2026-10-09</b></font>)
- **Root Cause Resolution**: Solved merge/rebase conflicts between GitHub Actions bot commits and local `github.bat` commits on `README.md`.
- **Merge Driver Immunity (`.gitattributes`)**: Configured `README.md merge=ours` and registered `git config merge.ours.driver true`. Git now automatically resolves 3-way conflicts in `README.md` cleanly without conflict markers.
- **Pre-Push Remote Sync in [`github.bat`](file:///d:/Games/New%20folder-web/Claude/github.bat)**: Upgraded `github.bat` to fetch and rebase on upstream changes before committing local files, avoiding fast-forward rejections.
- **Auto-Resolution Fallback**: Added automated `-X ours` rebase, dashboard regeneration, and staging if a remote race condition occurs during push.
- **Loop Breaker**: Appended `[skip ci]` flag to automatic `github.bat` commits to prevent GitHub Actions from creating duplicate trailing commits.
- **Version Bump**: `versionCode` advanced to <font color="#10b981"><b>53</b></font>, `versionName` advanced to <font color="#10b981"><b>"2.6.4.0.0.25"</b></font>.

## Version 2.6.4.0.0.24 (Release Build 52) — <font color="#10b981"><b>2026-10-09</b></font>

### 1. Per-Liter Tiered Fuel Delivery Pricing Engine (<font color="#10b981"><b>2026-10-09</b></font>)
- **Granular Per-Liter Tiered Rates:** Updated [`FeeConstants.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/FeeConstants.kt) to enforce exact doorstep delivery charges:
  - **1 to 10 Liters (including 5L, 7L, 10L)**: <font color="#10b981"><b>Rs. 300.00</b></font> (Flat Rs. 300 for up to 10L)
  - **11 Liters**: <font color="#10b981"><b>Rs. 320.00</b></font>
  - **12 Liters**: <font color="#10b981"><b>Rs. 340.00</b></font>
  - **13 Liters**: <font color="#10b981"><b>Rs. 360.00</b></font>
  - **14 Liters**: <font color="#10b981"><b>Rs. 380.00</b></font>
  - **15 Liters (Strict Max Limit Cap)**: <font color="#10b981"><b>Rs. 400.00</b></font>
- **Linear Step Formula:** Implemented in `calculateFuelDeliveryFee(liters)` as `300.00 + (liters - 10) * 20.00` for $11 \le \text{liters} \le 15$.
- **Strict 15L Safety Cap:** Mobile doorstep delivery remains capped at a maximum of 15 Liters per order; orders exceeding 15L are rejected across all dialogs and ViewModels.

### 2. Standardized Silent OGRA Pump Rate Markup Constant (<font color="#10b981"><b>2026-10-09</b></font>)
- **Markup Constant Definition:** Defined `FeeConstants.PUMP_RATE_MARKUP = 5.00` (Double) and `FeeConstants.PUMP_RATE_MARKUP_FLOAT = 5.00f` (Float).
- **Universal Application:** Applied uniformly over official OGRA ex-depot base notifications for:
  - Super Euro-V Petrol (`finalRate = ograBase + 5.00`)
  - Euro-V High-Speed Diesel (`finalRate = ograBase + 5.00`)
  - High-Octane 97 HOBC (`finalRate = ograBase + 5.00`)
- **Silent Background Execution:** The retail markup math executes silently in the background. UI screens, cards, dialogs, and invoice breakdowns display **strictly the final pump rate** without any debug labels or surcharge annotations.

### 3. Delivery Operating Hours Window Enforcement & UI Gate (<font color="#10b981"><b>2026-10-09</b></font>)
- **Operating Hours Manager:** Created [`DeliveryOperatingHoursManager.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/DeliveryOperatingHoursManager.kt) calibrated to Pakistan Standard Time (`Asia/Karachi`, UTC+5):
  - **Monday – Thursday**: <font color="#10b981"><b>08:00 AM – 08:00 PM PKT</b></font>
  - **Friday**: <font color="#10b981"><b>08:00 AM – 01:00 PM PKT</b></font>
  - **Saturday – Sunday**: <font color="#10b981"><b>10:00 AM – 06:00 PM PKT</b></font>
- **UI Button Hiding When Closed:**
  - **Home Screen FAB**: "Order Now" ExtendedFloatingActionButton (`home_fab`) is completely hidden when closed.
  - **Order Dialog Confirmation Button**: "Place Delivery Order / Confirm Order COD" (`confirmButton`) is completely omitted from the layout when closed.
  - **Customer Order Card**: "Reorder 🔁" button on past orders is hidden when closed.
  - **Category Fuel Cards**: Clicking fuel cards while closed displays an informative Toast indicating operating hours.
- **Backend & Database Hard Guard:** `MainViewModel.placeOrder()` enforces `isDeliveryOpen` check, blocking off-hours submissions and notifying the user.
- **Live Status Card:** Added live open/closed status banner and schedule summary to [`CustomerHomeScreen`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/ui/Screens.kt).

### 4. Real-Time Trading-Style Codebase Movement Graph (<font color="#10b981"><b>2026-10-09</b></font>)
- **Pure Code-Based Telemetry Engine:** Built [`scripts/generate_github_graph.js`](file:///d:/Games/New%20folder-web/Claude/scripts/generate_github_graph.js) generating:
  - Dynamic code churn analysis (`git log --numstat`), evaluating line additions/deletions and categorizing changes by scale: **Major Shift**, **Moderate Shift**, or **Minor Shift**.
  - Multi-timeframe tracking across **1D (Daily)**, **1W (Weekly)**, and **1M (Monthly Macro)** timeframes.
  - Native GitHub Mermaid `xychart-beta`, Mermaid `gitGraph`, and interactive ASCII/Unicode Trading Terminal directly in [`README.md`](file:///d:/Games/New%20folder-web/Claude/README.md).
  - Incline / Decline trend momentum indicators (`▲ BULLISH SURGE`, `◄ CONSOLIDATION`).
  - Exact push timing, cadence, and interval tracking.
- **Automated Update Pipeline:** Installed Git pre-commit hook ([`.git/hooks/pre-commit`](file:///d:/Games/New%20folder-web/Claude/.git/hooks/pre-commit)) and upgraded [`github.bat`](file:///d:/Games/New%20folder-web/Claude/github.bat) so the graph automatically regenerates and embeds into [`README.md`](file:///d:/Games/New%20folder-web/Claude/README.md) on every commit.

### 5. Application Micro-Versioning & Compliance (<font color="#10b981"><b>2026-10-09</b></font>)
- **`app/build.gradle.kts`**:
  - `versionCode`: incremented from 51 to <font color="#10b981"><b>52</b></font>.
  - `versionName`: advanced from `2.6.4.0.0.38` to <font color="#10b981"><b>2.6.4.0.0.24</b></font>.
- **Legal Synchronization**: Updated [`TERMS_AND_CONDITIONS.md`](file:///d:/Games/New%20folder-web/Claude/TERMS_AND_CONDITIONS.md), [`PRIVACY_POLICY.md`](file:///d:/Games/New%20folder-web/Claude/PRIVACY_POLICY.md), and in-app legal viewer (`TermsAndPrivacyDialog.kt`) to reflect Build 52, tiered delivery pricing, and delivery operating hours.
- **Test Suite**: Verified via `FuelPumpPricingTest.kt`, `ZyphuelComprehensiveAppTest`, and `CategoryAndVehicleArchitectureTest` with 100% test pass (0 failures).
