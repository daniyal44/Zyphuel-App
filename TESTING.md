# 🧪 Zyphuel — Testing & Release Verification

**Last Updated:** <font color="#10b981"><b>2026-10-23 22:48:18 PKT</b></font>
**App Version:** <font color="#10b981"><b>v2.6.4.0.0.33 (Build 61)</b></font>
**Last Full Run:** <font color="#10b981"><b>2026-10-23 22:44 PKT — 54 tests, 0 failed, 0 skipped</b></font>
**Document Status:** <font color="#10b981"><b>ACTIVE</b></font>

---

## 1. Commands

```bash
# whole JVM (Robolectric) suite — the gate used for this release
./gradlew :app:testDebugUnitTest

# one security/data class only
./gradlew :app:testDebugUnitTest --tests "com.example.SecurityAndDataIntegrityTest"

# compile without running tests
./gradlew :app:compileDebugKotlin

# debug APK
./gradlew :app:assembleDebug

# Play Store bundle (requires KEYSTORE_PATH / STORE_PASSWORD / KEY_PASSWORD)
./gradlew :app:bundleRelease
```

Reports: `app/build/reports/tests/testDebugUnitTest/index.html` and machine-readable XML in
`app/build/test-results/testDebugUnitTest/`.

Tests run against **Robolectric** with temporary/in-memory databases only. They never touch Firebase,
never use real accounts, and never run destructive operations on user data.

---

## 2. Test suites

| Suite | Tests | Purpose |
| :--- | :---: | :--- |
| `SecurityAndDataIntegrityTest` | 16 | Lockout policy, restart/parallel bypass, hashing, injection, duplicate email, ownership, migrations |
| `ZyphuelComprehensiveAppTest` | 19 | Roles, category catalog, pricing, operating hours, cross-feature flows |
| `CategoryAndVehicleArchitectureTest` | 9 | Category seed integrity (4 orderable fuel SKUs, Water/LPG disabled), vehicles, OGRA resolution |
| `FuelPumpPricingTest` | 7 | Per-litre tiered delivery fees and pump-rate math |
| `ExampleUnitTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest` | 3 | Smoke tests and screenshot baseline |
| **Total** | **54** | **0 failed, 0 skipped (2026-10-23 22:44 PKT)** |

---

## 3. Security test cases (`SecurityAndDataIntegrityTest`)

### 3.1 Failed-login / cooldown

| Test | Assertion |
| :--- | :--- |
| `first three wrong passwords stay unlocked` | Attempts 1–3 must not lock the account |
| `fourth wrong password locks account with Error 422 for five minutes` | 4th failure returns `Error 422` + "5 minutes"; probe reports 290–300 s remaining |
| `cooldown expiry automatically re-allows login` | After the window elapses login is allowed again with no manual reset |
| `app restart cannot bypass the cooldown` | `initialize(forceReload = true)` (simulated cold start) still reports `Blocked` |
| `parallel failed attempts are counted exactly once each and cannot slip past the fourth` | 4 simultaneous failures still trigger the lockout — fails if the per-account mutex is removed |
| `successful login clears the failure counters and lockout` | `recordAuthSuccess` clears both persisted keys |
| `persisted lockout keys never contain the plain email address` | Stored prefs are SHA-256 keyed; no PII in clear text |

### 3.2 Password & injection

| Test | Assertion |
| :--- | :--- |
| `SQL injection payloads are rejected by email validation` | `' OR '1'='1`, `admin'--`, `UNION SELECT`, `" OR ""="` all fail validation |
| `password hashes use PBKDF2 and verify only the correct secret` | Stored value starts with `pbkdf2_sha256$`, two hashes of the same password differ, wrong password fails |
| `social OAuth placeholder can never authenticate through the password form` | `SOCIAL_OAUTH_*` accounts are unresolvable via the password form |
| `a stored hash can never be matched by raw input` | Submitting the stored hash itself does not authenticate |
| `SQL injection payload cannot pass password verification` | Injection string never verifies against a real PBKDF2 hash |

### 3.3 Account uniqueness & isolation

| Test | Assertion |
| :--- | :--- |
| `email lookup is case-insensitive so duplicate signups are blocked` | `Ali@x.com` and `ali@x.com` are the same account; second registration returns `false` and the existing row is untouched |
| `customer order flow only returns that customer's own orders` | User A's flow never contains User B's order (local ownership scoping) |

### 3.4 Data preservation

| Test | Assertion |
| :--- | :--- |
| `migration object bridges version 12 to version 13` | `MIGRATION_12_TO_13` exists and targets 12 → 13 |
| `user and order records survive a database close and reopen` | Write rows → close → reopen with `.addMigrations(MIGRATION_12_TO_13)` → every row is identical (app-update simulation) |

A faithful repeat-migration check is covered by reopening the same file twice at version 13: the second open
runs no migration and produces no duplicate rows.

---

## 4. Manual verification matrix (per release)

| Check | How |
| :--- | :--- |
| Install-over preserves data | Install previous build, register + place an order, install new build **without uninstalling**, confirm both are still visible |
| Update banner | Publish a higher version → banner appears; `forceUpdate = false` allows "Later"; `forceUpdate = true` hides dismiss |
| Cooldown UI | 4 wrong passwords → button shows `Locked (04:59)`, counts down every second, re-enables at zero |
| Notification permission denied | Deny `POST_NOTIFICATIONS` → app still runs, no crash, orders still placeable |
| Sign-out | Sign out → session cleared, cloud account untouched, sign-in again restores the same data |
| Firestore ownership | Sign in as a second test account; confirm it cannot see the first account's orders |

---

## 5. Build & release verification

1. `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL, 0 failures.
2. `./gradlew :app:assembleDebug` → APK produced.
3. `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
4. Signing: `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` set; **never** publish a bundle signed with
   the debug keystore fallback.
5. `versionCode` strictly greater than the previous Play upload (currently **61**).
6. No secrets in the tree: `git grep -i "app_password\|MASTER_ADMIN\|keystore"` should return only
   template/`.example` references.

Tests are a gate, not proof of a production deployment: nothing in this repository auto-publishes.

---

## Related documents

- [SECURITY.md](SECURITY.md) • [DATABASE.md](DATABASE.md) • [MIGRATIONS.md](MIGRATIONS.md)
- [changes.md](changes.md) — release history
