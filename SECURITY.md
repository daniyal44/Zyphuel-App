# 🔐 Zyphuel — Security Architecture & Incident Response

**Last Updated:** <font color="#10b981"><b>2026-10-23 22:48:18 PKT</b></font>
**App Version:** <font color="#10b981"><b>v2.6.4.0.0.33 (Build 61)</b></font>
**Document Status:** <font color="#10b981"><b>ACTIVE — verified against source at the timestamp above</b></font>

---

## 1. Authentication Architecture

Zyphuel is an Android (Kotlin + Jetpack Compose) app with a **local-first Room database** and
**Google Cloud Firestore** as the cloud authority. There is **no self-hosted auth server**: the
authentication authority for email + password is the on-device credential store, and Firestore
Security Rules are the authority for cloud data access.

| Aspect | Implementation |
| :--- | :--- |
| Primary (and only credential-based) method | **Email + Password** — `MainViewModel.login()` → `AppRepository.loginUser()` |
| Password storage | PBKDF2-HMAC-SHA256, 120,000 iterations, 16-byte random per-user salt, 256-bit output |
| Stored format | `pbkdf2_sha256$<iterations>$<hex salt>$<hex hash>` (`SecurityCrypto.kt`) |
| Comparison | Constant-time `MessageDigest.isEqual` |
| Session | `sessionPrefs` (`logged_in_email`) + AES-256-GCM token in `EncryptedSharedPreferences` |
| Sign-out | Clears session prefs, token and in-memory user; the server-side account is **never** deleted |
| Biometric unlock | Optional; only unlocks the encrypted stored password, which is then re-verified through `loginUser()`. It never replaces the email + password check. |
| Social sign-in (Google) | Pre-existing optional path (`SOCIAL_OAUTH_*` placeholder accounts). It is **not** an alternative email/password identity and such accounts can never be authenticated through the password form. |
| Password recovery | Email-only one-time code: 6 digits, single use, 5-minute expiry |

### 1.1 Login flow (current code path)

1. `SecurityInputValidator.validateEmail()` rejects malformed/oversized addresses and injection payloads.
2. `SecurityRateLimiter.checkAuthLockout()` blocks the attempt if the account is inside a cooldown.
3. `AppRepository.loginUser()` looks the account up by `LOWER(email)` and verifies the password hash.
4. Success → `recordAuthSuccess()` clears counters, the token is issued and the session is stored.
5. Failure → `recordAuthFailure()` counts the attempt and returns the cooldown message when triggered.

### 1.2 What login is NOT based on

- No raw SQL string concatenation anywhere in the auth path (`@Query` with bound parameters only).
- No `isLoggedIn` / `isAdmin` client flag is trusted: the role is read from the stored `UserEntity`
  and re-checked by Firestore Rules server-side.
- No lookup by "first user of this role" fallback (removed 2026-10-23) — that previously allowed a
  fingerprint prompt to open whichever account happened to be first.
- Firestore documents are never fetched by a client-supplied user ID without an ownership rule.

---

## 2. Password Hashing

| Property | Value |
| :--- | :--- |
| Algorithm | PBKDF2-HMAC-SHA256 (platform-native; Argon2id needs a native dependency this project does not ship) |
| Iterations | 120,000 |
| Salt | 16 random bytes per user (`SecureRandom`) |
| Pepper | Application-level constant in `SecurityCrypto.PEPPER` (defence in depth, not a substitute for the salt) |
| Legacy compatibility | Old salted SHA-256 hashes are still accepted and are **transparently re-hashed to PBKDF2** on the next successful login (`PASSWORD_HASH_UPGRADED` audit entry) |
| Plaintext fallback | Only for values that are clearly not a hash (not 64 hex chars). A stored hash can never be matched by submitting it. |
| Master admin | `MASTER_ADMIN_HASH`, injected from `ADMIN_PASSWORD_HASH` (env / `admin-credentials.properties`), never in source |

Passwords are never written to logs, audit entries, crash reports or documentation.

---

## 3. Failed-Login Protection (4 attempts / 5-minute cooldown)

Implemented in `app/src/main/java/com/example/security/SecurityRateLimiter.kt`.

| Requirement | Status | Detail |
| :--- | :--- | :--- |
| Max 4 failed passwords per rolling 60 s window | ✅ | `authMaxAttempts = 4`, `authWindowMs = 60_000` |
| 5-minute cooldown on the 4th failure | ✅ | `authLockoutMs = 300_000` |
| Cooldown enforced independently of the UI | ✅ | Every `login()` re-checks before touching the password hash |
| Survives app restart / force-close | ✅ | Lockout and failure window persisted to `zyphuel_auth_guard` prefs with **SHA-256 hashed keys** (no emails stored in clear text) |
| Resists device clock changes | ✅ | Also tracked with `SystemClock.elapsedRealtime()`; the larger of wall-clock and monotonic remaining time wins |
| Atomic under parallel attempts | ✅ | Per-account mutex (`accountLocks`) around the read-modify-write of the failure window — added 2026-10-23 |
| Auto re-enable when it expires | ✅ | `MainViewModel.loginCooldownSeconds` ticks every second; the form re-enables at zero and shows "Locked (MM:SS)" |
| Successful login clears state | ✅ | `recordAuthSuccess()` removes both stored keys |

### 3.1 What is different from a server deployment

The prompt's reference design assumes an HTTP auth endpoint with `401` / `429` + `Retry-After` and a
shared Redis store. This app has **no auth HTTP endpoint** — credentials are verified on-device and the
UI is Compose, so:

- The equivalent of `429` is the blocking lockout result (`RateLimitResult.Blocked`) and the app's
  established `Error 422` message code, kept for **backward compatibility** with existing UI strings
  and tests rather than renumbered to 429.
- There is no cross-device shared counter (no Redis). A user could in principle retry from a second
  device; the per-account data itself is still protected by the password hash. This is documented as
  **Partially Completed** in the release report and requires a Cloud Function + Redis/Firestore
  counter to fully close.
- IP/network rate limiting is not applicable on-device; the equivalent is the per-public-endpoint and
  per-authenticated-action limiters (`checkPublicEndpointLimit`, `checkAuthenticatedActionLimit`).

---

## 4. Email Validation & Duplicate Accounts

- Format validation: `SecurityInputValidator.validateEmail()` (length, shape, no injection payloads).
- Normalization: `trim()` + `lowercase()` on registration **and** lookup.
- Uniqueness: `users.email` is the `PRIMARY KEY`; registration uses `@Insert(onConflict = ABORT)`
  (`insertNewUser`) so a duplicate can never overwrite an existing row (`REPLACE` would silently merge).
- Case-insensitive duplicate check: `SELECT * FROM users WHERE LOWER(email) = LOWER(:email)`.
- Existing accounts are **not** rewritten by this change; only new registrations are normalized.
- Login errors are generic ("Invalid email or password.") so accounts cannot be enumerated.
- Signup duplicate gives a specific, user-friendly message (acceptable: the attacker must already know
  the address to see it, and signup must explain why it was rejected).

**Ownership verification:** the app supports email-domain ownership verification only through the
one-time-code recovery flow. A full signup-time verification (double opt-in) is **Not Implemented** —
it requires an outbound mail provider that is configured at runtime and not guaranteed to be present.

---

## 5. Authorization & User Data Isolation

### 5.1 Local (Room)

- Every user-scoped DAO query filters by the owning email
  (`getOrdersForCustomerFlow(email)`, `getOrdersForRiderFlow(email)`, `marked_locations.userEmail`).
- The customer order history reads only the signed-in email; the admin view is gated on `role == "admin"`.
- `getUserByEmail` is the only credential lookup and requires the exact (case-insensitive) address.

### 5.2 Cloud (Firestore Rules — `firestore.rules`)

| Collection | Rule |
| :--- | :--- |
| `users/{uid}` | read: owner or admin; create: role must be `customer`/`rider`; update: role immutable by owner |
| `orders/{orderId}` | read/update: admin, owning customer or assigned rider; `totalPrice` and `customerEmail` immutable after creation; delete: admin only |
| `audit_logs` | append-only; read admin-only; update/delete denied |
| `system_config` (SMTP credentials) | admin only |
| `feedback`, `mail` | create signed-in, read admin |
| `app_stats` | public read, signed-in write |
| default | deny everything not explicitly matched |

**Known residual risk (documented, not silently changed):** `live_tracking/{orderId}` currently allows
any signed-in user to read *and write*, because the tracking document does not carry an owner email and
the app's session may be anonymously bridged. Tightening it requires a staging rules deploy and a
two-device tracking test; shipping an untested rule could silently break live GPS. The reviewed patch
is below and must be validated in staging before production deploy:

```javascript
match /live_tracking/{orderId} {
  function order() {
    return get(/databases/$(database)/documents/orders/$(orderId)).data;
  }
  allow read: if isSignedIn() && (
    isAdmin()
    || order().customerEmail == request.auth.token.email
    || order().riderEmail == request.auth.token.email
  );
  allow write: if isSignedIn() && (
    isAdmin() || order().riderEmail == request.auth.token.email
  );
}
```

---

## 6. Secrets Management

| Secret | Storage | Never in |
| :--- | :--- | :--- |
| `ADMIN_PASSWORD_HASH` | `ADMIN_PASSWORD_HASH` env var or `app/admin-credentials.properties` (git-ignored) → `BuildConfig` | source, docs, commits |
| `SMTP_APP_PASSWORD` | admin-managed Firestore `system_config` → `EncryptedSharedPreferences`; owner builds may inject via env/`local.properties` (`BuildConfig.SMTP_APP_PASSWORD`) | source, docs, logs |
| `google-services.json`, `local.properties`, `*.jks`, `.env` | git-ignored; only `.example` templates are committed | git history |
| Session tokens | AES-256-GCM via Android Keystore (`EncryptedSharedPreferences`) | logs, backups |

**Removed in this release:** a live Gmail app password was hardcoded as the default SMTP credential in
`SecureStorageManager.getSmtpConfig()` and had also been copied into `FEATURES_DOCUMENTATION.md`. Both
were removed; the default is now an empty string, and credentials come only from the admin-managed
Firestore config or the git-ignored `SMTP_APP_PASSWORD` secret. **The leaked app password must still be
revoked and reissued in the Google Account settings** — removing it from source does not invalidate a
credential that may already be in git history or on a shared device.

---

## 7. Logging & Error Handling

- `DebugLogger` and `SecurityErrorFormatter` are used for user-facing errors; stack traces, file paths,
  SQL statements and secrets are not surfaced to the UI.
- Login failures are logged as `USER_LOGIN_FAILED` with the email only (no password, no hash).
- Password change/upgrade events are logged as `PASSWORD_HASH_UPGRADED` without any credential material.
- Audit logs are append-only in Firestore (update/delete denied by rule).

---

## 8. Dependency & Platform Security

- `targetSdk`/`compileSdk` 36, `minSdk` 24; AGP 9.1.1, Kotlin 2.2.10, Gradle 9.5.0, Room 2.7.0,
  Firebase BOM 34.15.0.
- Versions are pinned in `gradle/libs.versions.toml`; upgrades are deliberate and build-verified.
- Release signing reads `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` from the environment; a
  missing keystore falls back to the debug keystore so local builds still work (never publish such a build).

---

## 9. Reporting a Vulnerability

Report privately to the project owner (repository owner on GitHub) with: affected version, steps to
reproduce, and impact. Do **not** open a public issue containing credentials, personal data or exploit
details. Expected acknowledgement: 72 hours. Do not test against production accounts or real customer
data.

### Incident response

1. Contain — disable the affected credential/config in the admin console (Firestore `system_config`).
2. Revoke — rotate the exposed secret (Google app password, admin hash, keystore).
3. Assess — check `audit_logs` for `USER_LOGIN_FAILED`, `PASSWORD_HASH_UPGRADED`, role changes.
4. Eradicate & recover — ship a patched build; user data is never deleted as part of incident handling.
5. Document — record the incident, timeline and fix in [`changes.md`](changes.md).

---

## 10. Security Boundaries (no absolute guarantees)

This document deliberately makes no "breach-proof" claim. Residual risks that remain by design:

1. **Client-side credential verification.** A rooted/patched device can bypass UI checks; the password
   hash and Firestore rules remain the real protections. A server-verified login (Cloud Function) is
   the recommended next step.
2. **No cross-device rate limiting** (see §3.1).
3. **`live_tracking` write scope** (see §5.2) until the staged rules patch is validated.
4. **Signup email ownership verification** is only enforced through the recovery-code flow.
5. **Local uninstall** clears the Room cache; the account and cloud records survive, but unsynced
   local-only rows (e.g. saved addresses stored in prefs) do not.

---

## Related documents

- [README.md](README.md) — overview, setup, commands
- [DATABASE.md](DATABASE.md) — schema, ownership, backup/restore
- [MIGRATIONS.md](MIGRATIONS.md) — migration policy and verification
- [TESTING.md](TESTING.md) — security, rate-limit and data-preservation tests
- [docs/06_SECURITY_AND_BIOMETRICS.md](docs/06_SECURITY_AND_BIOMETRICS.md) — biometric and device security detail
- [docs/01_USER_ROLES_AND_AUTHENTICATION.md](docs/01_USER_ROLES_AND_AUTHENTICATION.md) — role matrix
