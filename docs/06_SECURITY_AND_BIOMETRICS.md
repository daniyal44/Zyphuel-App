# 06. Security & Biometrics Engine Documentation 🔐

## 📌 Category
**Security, Biometrics & Encryption**

## 🎯 Purpose & Overview
The Security Engine provides end-to-end security, input sanitization, rate limiting, encrypted storage, and hardware biometric authentication (Fingerprint & Face Unlock) for sensitive areas of Zyphuel.

---

## ⚙️ Key Security Modules & Functions
1. **Biometric Security Manager (`BiometricSecurityManager.kt`)**:
   * Uses AndroidX `BiometricPrompt` API.
   * `canAuthenticate()`: Verifies hardware fingerprint/face availability.
   * `showBiometricPrompt()`: Displays system biometric scanner modal.
2. **Encrypted Storage Manager (`SecureStorageManager.kt`)**:
   * Encrypts biometric session tokens and user preferences using AES-256 / SHA-256 digests.
3. **Security Rate Limiter (`SecurityRateLimiter.kt`)**:
   * Limits login attempts, order placements, and rating submissions to prevent brute-force attacks.
4. **Security Input Validator (`SecurityInputValidator.kt`)**:
   * Enforces strict regex validation on emails, passwords, phone numbers, ratings (1-5 stars), and feedback notes.
5. **Order History Biometric Vault (`Screens.kt`)**:
   * Protects sensitive customer order history and expenditure receipts behind biometric authentication.

---

## 🔒 Biometric Features
* **Conditional Registered Biometric Sign-In Card**: Login form biometric card (`Screens.kt`) is displayed ONLY if a registered user has explicitly enabled fingerprint biometrics. Hidden by default on new app installs and for new/unregistered users.
* **Default Disabled State**: Biometric authentication is disabled by default upon fresh app install and account registration.
* **Enable Fingerprint Option**: Users see "Enable fingerprint" in Profile Settings and Security Settings once logged in. Tapping it scans fingerprint and enables biometric login.
* **Post-Logout Quick Sign-In**: If biometrics are enabled, when a registered user logs out, the Fingerprint / Face ID login card appears on the login form for 1-tap quick sign-in using AndroidX `BiometricPrompt`.
* **Instant Fallback Biometric Profile Lookup**: Integrated `loginWithBiometrics` in `MainViewModel` that verifies hardware fingerprint/face or uses email/role fallback credentials for 1-tap seamless login.
* **Biometric Vault Badge**: Status badge on `CustomerOrderHistoryScreen`.
* **Security Settings Panel (`SecuritySettingsScreen.kt`)**: User toggles for role-specific biometric enforcement ("Enable Fingerprint" / "Disable Fingerprint") and session timeout lock.

---

## 📁 Source Locations
* **Biometric Manager**: `app/src/main/java/com/example/security/BiometricSecurityManager.kt`
* **Encrypted Storage**: `app/src/main/java/com/example/security/SecureStorageManager.kt`
* **Rate Limiter**: `app/src/main/java/com/example/security/SecurityRateLimiter.kt`
* **Input Validator**: `app/src/main/java/com/example/security/SecurityInputValidator.kt`
* **UI Integration**: `app/src/main/java/com/example/ui/Screens.kt` (`SecuritySettingsScreen`)

---

## 🔄 Biometric Authentication Flow
1. **Trigger**: User taps Biometric Login button or attempts to access Order Vault.
2. **Scanner Launch**: `BiometricPrompt` pops up system fingerprint/face dialog.
3. **Verification**: Hardware chip verifies biometric template against Android Keystore.
4. **Grant Access**: On success, loads authenticated session state instantly.
