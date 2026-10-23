# Zyphuel Security & Play Store Compliance

> **Current App Version:** `2.6.4.0.0.33 (Build 61)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-23</b></font>  
> **Status:** <font color="#10b981"><b>Build Verified (37/38 Tests Passing — 1 Pre-Existing Unrelated Failure)</b></font>

Defense-in-depth architecture, telemetry privacy, anti-breach enforcement, and compliance checklist.

---

## 1. Authentication & Identity
- Passwords hashed using SHA-256 before Room persistence.
- Biometric authentication via AndroidX `BiometricPrompt` with hardware biometric fallback.
- Firebase Auth session token management for cloud synchronizations.
- <font color="#10b981"><b>Brute-Force Lockout — Error 422 (2026-10-23):</b> 4 wrong passwords within 1 minute lock login for a 5-minute cooldown; enforced across password login, biometric login, and reset-code guessing via `SecurityRateLimiter.checkAuthLockout()` / `recordAuthFailure()`.</font>
- <font color="#10b981"><b>Email OTP Password Recovery (2026-10-23):</b> single-use 6-digit code (5-min expiry) emailed to the registered address; no password reset without the matching code, closing the email+phone-only takeover path.</font>
- <font color="#10b981"><b>Password-Verified Biometric Login (2026-10-23):</b> fingerprint sign-in resolves the account by email and re-verifies the stored password; the "first user of this role" query bypass was removed. Credential login is email + password only.</font>
- <font color="#10b981"><b>Duplicate Email Guard (2026-10-23):</b> case-insensitive `LOWER(email)` lookup blocks the same address being registered twice in any letter-case.</font>
- <font color="#10b981"><b>Strict Anti-Breach Guarantee:</b> Zero plaintext passwords, auth tokens, or private PII in code, logs, or commits.</font>

---

## 2. Data Protection & Secure Storage
- `EncryptedSharedPreferences` backed by Android Keystore MasterKey (AES-256 GCM).
- Sensitive credentials (SMTP passwords, session tokens) stored strictly in encrypted storage.
- `.env` and sensitive API keys excluded from version control.

---

## 3. Anti-Tamper & Environment Integrity
- `RootAndSecurityDetector`: Detects rooted Android environments, Magisk su binaries, test-keys build tags, and Frida dynamic instrumentation frameworks.
- Blocks sensitive administrative overrides when running on compromised devices.

---

## 4. Petroleum & OGRA Regulatory Compliance
- Mandatory prominent disclosures regarding petroleum transportation safety.
- Verified bowser riders must adhere to OGRA, Civil Defence, and explosive carriage regulations.
- <font color="#10b981"><b>Strict 15L Max Volume Cap:</b> Protects against hazardous carriage overages.</font>
- <font color="#10b981"><b>Official Delivery Hours Gate:</b> Orders restricted to Mon–Thu 08:00–20:00, Fri 08:00–13:00, Sat–Sun 10:00–18:00 PKT.</font>

---

## 5. Google Play Store Policy Compliance
- **Location Telematics (`FOREGROUND_SERVICE_LOCATION`):** Clear user-facing disclosures explaining background vehicle tracking.
- **Permanent Account Deletion:** Direct in-app option allowing users to permanently erase their account and data within 2 clicks (`SettingsScreen` -> `Delete Account`).
- **Privacy Policy & Terms:** Continuously synchronized with app features in `PRIVACY_POLICY.md` and `TERMS_AND_CONDITIONS.md`.
