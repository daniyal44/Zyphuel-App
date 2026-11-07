# Zyphuel iOS

Native SwiftUI port of the Zyphuel Android app (`com.aistudio.zyphuel.appv2`).
Same Firebase project, same Firestore collections, same pricing rules, same roles.

> **Build environment:** iOS builds require macOS with Xcode 16+ and XcodeGen.
> This repository is developed on Windows, so the Swift code here has **not been
> compiled locally**. The `iOS Build` GitHub Actions workflow
> (`.github/workflows/ios-build.yml`) is the compile/test gate; `desktop_ios.bat`
> validates structure and refreshes dependency versions from Windows.

---

## 0. Shared versioning with Android

The version is **not** stored in this folder. `version.properties` at the repository root is the
single source; `app/build.gradle.kts` reads it directly and `scripts/sync_versions.py` writes
`ios/project.yml` and `ZyphuelConfig.swift` from it.

```bat
sync_versions.bat --bump    # bump Android + iOS + docs together
python scripts/sync_versions.py --check   # fail on drift (CI + pre-commit)
```

Every functional change must land on Android and iOS in the same change set.

---

## 1. What is ported

| Area | Android | iOS |
| --- | --- | --- |
| UI | Jetpack Compose, Material 3 | SwiftUI, iOS 17+ |
| Auth | Firebase Auth + Google Credential Manager | Firebase Auth + GoogleSignIn |
| Database | Room (SQLite) + Firestore | Firestore (offline cache) |
| Push | FCM | APNs through FCM |
| Location | Fused location, live tracking disabled | CoreLocation, no upload |
| Images | Base64 in Firestore | Base64 in Firestore |
| Roles | customer / rider / admin | identical |
| Pricing | `FeeConstants.kt` | `PricingEngine.swift` (1:1) |
| Hours | `DeliveryOperatingHoursManager.kt` | `OperatingHours.swift` (1:1) |
| Hashing | PBKDF2-HMAC-SHA256, 600k | same construction (`PasswordHasher`) |
| Lockout | 4 attempts / 5 min, code 422 | same (`SecurityRateLimiter`) |

### Screens

- **Auth** — sign in, create account (customer or rider), Google sign-in, reset link.
- **Home** — Lahore delivery card, live OGRA rates, search, 7 featured + 10 categories,
  operating-hours gate, current order, notifications, COD ordering.
- **Order sheet** — volume presets 5–15L, address, tiered delivery fee, COD total,
  confirm button hidden outside operating hours.
- **Tracker** — animated stepper, order summary, rider contact, star rating.
- **History** — current orders plus searchable/filterable recent orders.
- **Rider portal** — duty toggle, verification state, available queue, progression
  (accept → pick up/start route → complete & collect COD), COD totals.
- **Admin console** — overview, orders (accept/start/complete/decline/delete),
  rider approval, customer search + deletion, live rate editor, email gateway.
- **Profile** — account, phone, stats, language (English/اردو), Face ID lock,
  notifications, support channels, legal, account deletion (super admin protected).

---

## 2. Setup

1. **Firebase iOS app**
   - Firebase Console → Project settings → Add app → iOS.
   - Bundle ID: `com.aistudio.zyphuel.appv2`
   - Download `GoogleService-Info.plist` into `ios/Config/` (git-ignored).
2. **Google Sign-In URL scheme**
   - Copy `REVERSED_CLIENT_ID` from the plist into `Info.plist`
     (`CFBundleURLTypes` → first URL scheme).
   - Add the same reversed client ID to the iOS app in Firebase Authentication.
3. **Enable providers** — Email/Password and Google must already be enabled for Android;
   the same project serves iOS.
4. **APNs** (optional, for push)
   - Upload an APNs auth key in Firebase Console → Cloud Messaging.
   - Switch `aps-environment` in `Config/Zyphuel.entitlements` to `production` for Release.

Without `GoogleService-Info.plist` the app still launches in a clearly labelled setup
state rather than crashing.

---

## 3. Build

### Windows

```bat
desktop_ios_download.bat
```

This refreshes Firebase / GoogleSignIn pins to the latest releases, validates the
project, writes `ios/build_report.txt` and stages the best installable artifact it can
actually find on the Desktop:

| Situation | Delivered file |
| --- | --- |
| A signed/real `.ipa` exists in the workspace | `Zyphuel-v<ver>-Build<n>.ipa` (validated: `Payload/*.app/Info.plist`, ≥ 1 MB) |
| GitHub CLI is authenticated and a CI run exists | `Zyphuel-v<ver>-Build<n>-Simulator.app.zip` from the `Zyphuel-ios-simulator-app` artifact |
| No build available (Windows has no Xcode) | `Zyphuel-v<ver>-Build<n>-iOS-Source.zip` — **source only, never renamed to `.ipa`** |

Windows cannot compile iOS code, so a real device `.ipa` always comes from macOS or
GitHub Actions; the script no longer produces a tiny fake `.ipa` out of the source tree.

### macOS

```bash
bash ios/bootstrap.sh          # generate project + simulator build
bash ios/bootstrap.sh --test   # generate + build + unit tests
```

Or manually:

```bash
brew install xcodegen
cd ios && xcodegen generate
open Zyphuel.xcodeproj
```

### CI

`.github/workflows/ios-build.yml` runs XcodeGen, resolves Swift packages, builds and
executes the test suite on a macOS runner for every change under `ios/`.

---

## 4. Tests

`ios/Tests/ZyphuelLogicTests.swift` covers the parts that must not drift from Android:

- delivery-fee tiers (300 / 320 / 340 / 360 / 380 / 400) and the 15L cap
- Rs. 5.00 pump markup and 2-decimal money formatting
- operating-hours windows in Asia/Karachi (Mon–Thu, Fri, Sat–Sun boundaries)
- PBKDF2 round-trip, format compatibility, legacy 120k rehash detection
- 4-attempt / 5-minute lockout with error code 422
- email, Pakistani phone and password validation
- order lifecycle rules (riders cannot skip steps, admins can drive the full flow)
- catalog parity (10 categories, 7 featured, fuel seed prices, water/LPG inactive)
- super-admin protection is case-insensitive

---

## 5. Deliberate differences from Android

These are security or platform constraints, not missing features:

1. **No SMTP socket.** iOS uses the HTTPS webhook relay and the Firestore `mail`
   trigger; raw SMTP stays Android-only.
2. **No plaintext password display.** Android's profile screen can reveal the stored
   login password; iOS does not store or show a recoverable password.
3. **No live tracking upload.** Android already disabled it; iOS never writes to
   `live_tracking` and does not request background location for customers.
4. **Local storage.** Room is replaced by Firestore's offline cache plus Keychain for
   secrets; there is no SQLite schema to migrate on iOS.
5. **App icon.** `AppIcon.appiconset` ships the generated 1024×1024 brand mark
   (`AppIcon-1024.png`, opaque RGB, no alpha channel). Regenerate it with
   `scripts/generate_ios_app_icon.ps1` if the brand mark changes.

---

## 6. Firestore rules dependency

The hardened rules in `firestore.rules` apply to both platforms. iOS writes:

- `users/{email_with_underscores}` — profile fields only
- `orders/{id}` — COD orders, status transitions, rating
- `feedback/{id}` — post-delivery rating
- `audit_logs/{id}` — admin actions only
- `mail/{id}` — one recipient, sender `uid`, whitelisted message fields
- `app_stats/launches` — +1 counter only

Deploy the rules with `firebase deploy --only firestore:rules` before testing iOS
against production. Until then, live writes still use the previous ruleset.

---

## 7. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| "Firebase is not configured" banner | missing plist | add `ios/Config/GoogleService-Info.plist` |
| Google sign-in returns to app but not signed in | URL scheme missing | set `REVERSED_CLIENT_ID` in `Info.plist` |
| `Missing package product 'FirebaseAuth'` | packages not resolved | `xcodebuild -resolvePackageDependencies` |
| Build fails on Xcode 15 | iOS 17 APIs | use Xcode 16+ |
| Sign-in works but orders denied | rules not deployed | deploy `firestore.rules` |
| Download gives an ~84 KB ".ipa" | old builds renamed the source zip to `.ipa` | re-run `desktop_ios_download.bat`; source packages now end in `-iOS-Source.zip` |
| `NSPrivacyAccessedAPITypes` upload error | privacy manifest missing from the bundle | keep `Config/PrivacyInfo.xcprivacy` in `project.yml` resources |
