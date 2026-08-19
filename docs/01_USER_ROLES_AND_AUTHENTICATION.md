# 01. User Roles & Authentication Feature Documentation 🔐

## 📌 Category
**Security & User Access Control**

## 🎯 Purpose & Overview
The User Roles & Authentication module manages account creation, authentication, role-based access control (RBAC), and session management for the Zyphuel platform. It supports three distinct user personas: **Customer**, **Rider (Bowser Driver)**, and **Admin**.

---

## 🔑 User Roles & Permission Matrix
| Role | Capabilities | Primary Entry Screen |
| :--- | :--- | :--- |
| **Customer** (`role = "customer"`) | Place fuel/water orders, view live GPS tracking, rate drivers, view order history. | `CustomerHomeScreen` |
| **Rider** (`role = "rider"`) | Accept assigned orders, update delivery status (En Route → Arrived → Completed), call customer. | `RiderHomeScreen` |
| **Admin** (`role = "admin"`) | View system analytics, add/manage customers, add riders, assign orders, view audit logs. | `AdminDashboardScreen` |

---

## ⚙️ Key Functions & ViewModel Methods
* `FirebaseAuthProvider` (`app/src/main/java/com/example/auth/FirebaseAuthProvider.kt`): Manages Firebase authentication user sessions, state listeners (`StateFlow<FirebaseUser?>`), Google ID Token credential exchange (`signInWithGoogleIdToken`), generic OAuth credentials (`signInWithCredential`), and active session termination (`signOut`).
* `loginUser(email, password)` (`MainViewModel.kt`): Validates user credentials against Room database (`UserEntity`), hashes password via SHA-256, and sets logged-in state `currentUser`.
* `loginWithSocialAccount(provider, socialEmail, socialName, targetRole, onSuccess)` (`MainViewModel.kt`): Handles OAuth 2.0 single sign-on (SSO) exclusively for "Continue with Google" via `GoogleAuthManager`. Auto-provisions role-isolated accounts (`google.customer@zyphuel.com` or `google.rider@zyphuel.com`) or links existing email addresses with Google accounts, updates avatar URIs, and dispatches audit log entries.
* `registerUser(name, email, phone, password, address)` (`MainViewModel.kt`): Validates inputs via `SecurityInputValidator`, checks for existing email, inserts new `UserEntity`, and triggers welcome notification/email.
* `logoutUser()` (`MainViewModel.kt`): Clears active user session state, terminates `FirebaseAuthProvider` session, clears biometric authentication state, and returns navigation to `LoginScreen`.
* `loginWithBiometrics(context, module, userEmailInput)` (`MainViewModel.kt`): Biometric hardware authentication with instant profile lookup for fast 1-tap sign-in on both Customer and Rider login forms.
* `addRiderFromAdmin(...)` (`MainViewModel.kt`): Admin registration of new drivers storing personal, legal (CNIC/License), vehicle, and address credentials.

---

## 📁 Source Locations
* **Google Auth Manager**: `app/src/main/java/com/example/auth/GoogleAuthManager.kt`
* **Social Auth Manager**: `app/src/main/java/com/example/auth/SocialAuthManager.kt`
* **UI Screen**: `app/src/main/java/com/example/ui/Screens.kt` (`AuthScreen`)
* **Vector Asset**: `app/src/main/res/drawable/ic_google.xml`
* **State Management**: `app/src/main/java/com/example/ui/MainViewModel.kt` (`loginWithSocialAccount`, `logout`)
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
* Rate limiting on login attempts via `SecurityRateLimiter`.
* Biometric hardware token verification via `BiometricPrompt` API.
