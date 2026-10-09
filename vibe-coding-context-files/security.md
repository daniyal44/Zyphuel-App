# Zyphuel Security & Play Store Compliance

> **Current App Version:** `2.6.4.0.0.24 (Build 52)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

Defense-in-depth architecture, telemetry privacy, anti-breach enforcement, and compliance checklist.

---

## 1. Authentication & Identity
- Passwords hashed using SHA-256 before Room persistence.
- Biometric authentication via AndroidX `BiometricPrompt` with hardware biometric fallback.
- Firebase Auth session token management for cloud synchronizations.
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
