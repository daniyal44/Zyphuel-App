# 01. User Roles & Authentication Feature Documentation 🔐

> **Current App Version:** `2.6.4.0.0.26 (Build 54)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

## 📌 Category
**Security & User Access Control**

## 🎯 Purpose & Overview
The User Roles & Authentication module manages account creation, authentication, role-based access control (RBAC), and session management for the Zyphuel platform. It supports three distinct user personas: **Customer**, **Rider (Bowser Driver)**, and **Admin**.

---

## 🔑 User Roles & Permission Matrix
| Role | Capabilities | Primary Entry Screen |
| :--- | :--- | :--- |
| **Customer** (`role = "customer"`) | <font color="#10b981"><b>Place fuel orders (Strict 15L cap, tiered fees, operating hours gate; Water & LPG unavailable)</b></font>, view live GPS tracking, rate drivers, view order history. | `CustomerHomeScreen` |
| **Rider** (`role = "rider"`) | Accept assigned orders, update delivery status (En Route → Arrived → Completed), call customer. | `RiderHomeScreen` |
| **Admin** (`role = "admin"`) | View system analytics, add/manage customers, add riders, assign orders, monitor operating hours, view audit logs. | `AdminDashboardScreen` |

---

## ⚙️ Key Functions & ViewModel Methods
* `FirebaseAuthProvider` (`app/src/main/java/com/example/auth/FirebaseAuthProvider.kt`): Manages Firebase authentication user sessions, state listeners (`StateFlow<FirebaseUser?>`), Google ID Token credential exchange (`signInWithGoogleIdToken`), generic OAuth credentials (`signInWithCredential`), and active session termination (`signOut`).
* `loginUser(email, password)` (`MainViewModel.kt`): Validates user credentials against Room database (`UserEntity`), hashes password via SHA-256, and sets logged-in state `currentUser`.
* `loginWithSocialAccount(provider, socialEmail, socialName, targetRole, onSuccess)` (`MainViewModel.kt`): Handles OAuth 2.0 single sign-on (SSO) exclusively for "Continue with Google" via `GoogleAuthManager`. Auto-provisions role-isolated accounts (`google.customer@zyphuel.com` or `google.rider@zyphuel.com`) or links existing email addresses with Google accounts, updates avatar URIs, and dispatches audit log entries.
* `registerUser(name, email, phone, password, address)` (`MainViewModel.kt`): Validates inputs via `SecurityInputValidator`, checks for an existing email via `AppRepository.isEmailRegistered()` / case-insensitive `LOWER(email)` lookup (<font color="#10b981"><b>2026-10-23</b></font> — blocks duplicate signups such as `Ali@x.com` vs `ali@x.com`), inserts new `UserEntity`, and triggers welcome notification/email.
* <font color="#10b981"><b>`sendPasswordResetOtp(email)` / `resetPassword(email, phone, otp, newPassword)` (2026-10-23)</b></font> (`MainViewModel.kt`): Password recovery emails a single-use 6-digit code (5-minute expiry) to the registered address; no password can be changed without the matching code for that exact email, closing the previous email+phone-only takeover path.
* `logoutUser()` (`MainViewModel.kt`): Clears active user session state, terminates `FirebaseAuthProvider` session, clears biometric authentication state, and returns navigation to `LoginScreen`.
* `loginWithBiometrics(context, module, userEmailInput)` (<font color="#10b981"><b>2026-10-23</b></font>): Biometric sign-in that resolves the account by email and re-verifies the stored password through `repository.loginUser()` — the old "first user of this role" query fallback was removed so no login path can bypass email + password.
* `addRiderFromAdmin(...)` (`MainViewModel.kt`): Admin registration of new drivers storing personal, legal (CNIC/License), vehicle, and address credentials.
* <font color="#10b981"><b>`deleteAccountPermanently(context)`</b> (`MainViewModel.kt`): Enforces Google Play User Data policy by deleting user account, profile records, and stored credentials upon confirmed user request.</font>

---

## 📁 Source Locations
* **Google Auth Manager**: `app/src/main/java/com/example/auth/GoogleAuthManager.kt`
* **Social Auth Manager**: `app/src/main/java/com/example/auth/SocialAuthManager.kt`
* **UI Screen**: `app/src/main/java/com/example/ui/Screens.kt` (`AuthScreen`)
* **Vector Asset**: `app/src/main/res/drawable/ic_google.xml`
* **State Management**: `app/src/main/java/com/example/ui/MainViewModel.kt` (`loginWithSocialAccount`, `logout`, `deleteAccountPermanently`)
* **Data Entity**: `app/src/main/java/com/example/data/Models.kt` (`UserEntity.authProvider`)
* **Security Helper**: `app/src/main/java/com/example/security/SecurityInputValidator.kt`

---

## 🔄 User Flow
1. **Launch**: User opens app and sees `AuthScreen` with email/password input, "Continue with Google" button, and biometric quick-login card.
2. **Google Sign-In Choice**: User taps "Continue with Google" button (`testTag("social_login_google")`).
3. **Role-Aware Credential Authorization (`GoogleAuthManager`)**: Triggers native Android `CredentialManager` Google Account Picker with role-aware target isolation (`targetRole`).
4. **Account Provisioning & Link**: Invokes `loginWithSocialAccount`. Automatically links with existing email account or creates new `UserEntity` with `authProvider = "Google"` and role-isolated credentials (`google.customer@zyphuel.com` / `google.rider@zyphuel.com`).
5. **Role Routing**: Automatically routes user to `CustomerHomeScreen` or `RiderHomeScreen` based on selected persona.

---

## 🛡️ Security & Social Features
* SHA-256 secure password hashing.
* Native Google OAuth 2.0 single sign-on (SSO) integration via `CredentialManager` and `GoogleAuthManager`.
* Rate limiting on login attempts via `SecurityRateLimiter` (<font color="#10b981"><b>2026-10-23</b></font>): 4 wrong passwords in 1 minute → Error 422 lockout with 5-minute cooldown; wrong reset codes count toward the same lockout.
* Email + password only for credential login; Google OAuth and fingerprint paths both end in server-verified or password-verified authentication.
* Biometric hardware token verification via `BiometricPrompt` API.
* Permanent account deletion compliance.
