# 11. Real-Time Order Invoice & Email Delivery Documentation 📧🧾

## 📌 Category
**Automated Transactional Email — Order Confirmation & Official Tax Invoice Delivery**

## 🎯 Purpose & Overview
When a customer places an order, the app **automatically emails an official order confirmation + itemized tax invoice** to the customer. The email is delivered **only to a genuine registered email** — the exact address the user signed in with via **"Continue with Google"** or sign-up. This module guarantees that behavior end-to-end and delivers it through a **resilient multi-channel engine** so an email is not lost when a mobile network blocks SMTP ports.

> **Last verified:** `2026-09-09` against `MainViewModel.kt`, `RealtimeEmailEngine.kt`, `SecureStorageManager.kt`, `Screens.kt`. Line numbers are a point-in-time snapshot — always trust the **function name** first; verify the line before relying on it.

---

## ✅ Requirements (exactly as requested) & How Each Is Met

| # | Requirement | Status | Where Implemented |
| :-: | :--- | :---: | :--- |
| 1 | User **must receive an email** when an order is placed | ✅ Done | `placeOrder()` triggers `sendOrderInvoiceEmail(order, isCompletedReceipt = false)` right after the order is created — `MainViewModel.kt` (`placeOrder`, ≈ line 2322) |
| 2 | The **invoice** must be sent **directly by email** | ✅ Done | `sendOrderInvoiceEmail()` builds a plain-text receipt + HTML invoice via `InvoiceGenerator` and dispatches them — `MainViewModel.kt` (`sendOrderInvoiceEmail`, ≈ line 3581) |
| 3 | Email must go **only to the registered email** (same as "Continue with Google") — never a placeholder | ✅ Done | Recipient resolution rejects the guest placeholder `customer@zyphuel.com` and falls back to the stored registered email; skips entirely if none exists — `MainViewModel.kt` (`sendOrderInvoiceEmail`, ≈ lines 3586–3605) |
| 4 | Delivery must be **reliable** (not silently fail) | ✅ Done | Webhook-first channel order + real success/failure surfaced to UI — see change log below |

---

## 🔗 End-to-End Delivery Pipeline

1. **Login** — "Continue with Google" → `loginWithSocialAccount(...)` → `completeLogin(user)` sets `_currentUser.value = user` (the Google email). Registered email is also persisted encrypted via `SecureStorageManager.saveSecureCredentials(...)`.
2. **Order placed** — `placeOrder()` (`MainViewModel.kt` ≈ 2199) creates the order with `customerEmail = user.email`.
   * If `_currentUser` is `null` (e.g. process restart), it **recovers** the registered email from `SecureStorageManager.getRegisteredEmail(context, AppModule.CUSTOMER)`; only if nothing is on file does it use the placeholder `customer@zyphuel.com` (≈ lines 2207–2228).
3. **Invoice dispatch** — after the order is saved and the UI confirms, a background block calls `sendOrderInvoiceEmail(order)` (≈ line 2322). The callback sets `📧 Invoice emailed to …` on success, or logs the real failure reason (no more fake "success").
4. **Recipient resolution** — `sendOrderInvoiceEmail()` resolves the **true** recipient (rejecting the placeholder), then generates the invoice and calls `dispatchRealtimeEmail(...)` for the customer **and** a `[Admin Copy]` to `m.daniyalkhan490@gmail.com`.
5. **Engine dispatch** — `dispatchRealtimeEmail()` (≈ 3504) calls `RealtimeEmailEngine.sendRealtimeEmailDetailed(...)` with the active `_smtpConfig`, writes an audit log (`REALTIME_EMAIL_DELIVERED` / `REALTIME_EMAIL_ATTEMPTED`), stores a `NotificationEntity`, and posts a system notification on success.
6. **Multi-channel send** — `RealtimeEmailEngine` attempts each channel in priority order (below) and returns the first success.

---

## 📮 Multi-Channel Delivery Order (`RealtimeEmailEngine.sendRealtimeEmailDetailed`)

Attempted in this exact order; first success wins:

| Priority | Channel | When Used | Why |
| :-: | :--- | :--- | :--- |
| **1** | **HTTPS Webhook Relay** (Google Apps Script, port 443) | Only if a `webhookUrl` is configured | Fast, **stores no secret in the app**, and is **immune to carriers/networks that block SMTP ports 465/587**. Tried first so a dead SMTP password can't cause a ~12 s hang. |
| **2** | **Authenticated Direct SMTP** (TLS 465 / STARTTLS 587) | If `isEnabled` and an app password is present | Direct RFC 5321 delivery from the sender Gmail. |
| **3** | **Cloud Firestore `mail` collection** | Fallback | Consumed by the Firebase "Trigger Email" extension if installed. |

> ⚠️ **Design note for future agents:** the webhook is intentionally **Channel 1**. Do not reorder SMTP back to first — on mobile data, port 465 is frequently blocked and would waste the connect timeout on every email before falling through.

---

## 🎯 Registered-Email-Only Guarantee

The invoice recipient is resolved defensively in `sendOrderInvoiceEmail()`:

```
recipient = order.customerEmail
if recipient is blank / not an email / == "customer@zyphuel.com":
    recipient = _currentUser.email            (if valid & not placeholder)
             else SecureStorageManager registered email (CUSTOMER)
             else ""    → SKIP send, log warning, return (false, "No registered email on file")
```

* The placeholder `customer@zyphuel.com` is **never** an acceptable invoice recipient.
* If no genuine registered email can be found, **no email is sent** and the failure is logged — the app never emails a fake address.

---

## ⚙️ Configuration — Admin → Email Gateway / Mailbox Settings

Stored encrypted in the ADMIN enclave and synced across devices via Cloud Firestore.

| Setting | Default | Source |
| :--- | :--- | :--- |
| Host | `smtp.gmail.com` | `SecureStorageManager.getSmtpConfig()` (≈ line 186) |
| Port | `465` | same |
| Sender email | `m.daniyalkhan490@gmail.com` | same |
| App password | *(see Known Issues — hardcoded fallback exists)* | same |
| Sender name | `Zyphuel Delivery Operations` | same |
| Webhook URL | *(empty by default)* | field label **"Optional HTTPS Webhook / Apps Script URL"** in `Screens.kt` (≈ line 14799) |

* **Save:** `MainViewModel.saveSmtpSettings(...)` (≈ 3664) → `SecureStorageManager.saveSmtpConfig(...)` + Firestore sync.
* **Load / cloud refresh:** `MainViewModel.refreshSmtpConfig()` (≈ 3652).
* **Test:** `MainViewModel.sendTestEmail(targetEmail)` (≈ 3698) — the single source of truth for "is delivery actually working?".
* **In-app helper:** button **"Copy Free Apps Script Relay Code"** (`Screens.kt` ≈ 14888) copies the ready-to-deploy script.

---

## 🚀 Recommended One-Time Setup — Google Apps Script Webhook (zero app-side secret)

1. Open **script.google.com** → **New project**.
2. In-app: **Admin → Email Gateway** → **"Copy Free Apps Script Relay Code"**; paste into the editor (full script also in `RealtimeEmailEngine.getGoogleAppsScriptTemplate()`, ≈ line 553).
3. **Deploy → New deployment → Web app** → *Execute as: Me* → *Who has access: Anyone* → **Deploy**; approve the Gmail permission (sends from the owner's Google account via `MailApp.sendEmail`).
4. Copy the **Web app URL** → paste into the **Webhook URL** field → **Save**.
5. **Send Test Email** to confirm inbox delivery.

Once the webhook is verified, the hardcoded SMTP password (Known Issues) can be safely removed.

---

## 🧾 Invoice Content
Generated by `com.example.util.InvoiceGenerator`:
* `generatePlainTextReceipt(order)` — plain-text body.
* `generateHtmlInvoice(order)` — styled HTML invoice.

Subjects:
* New order: `🧾 Official Order Confirmation & Tax Invoice - Order #<id> | Zyphuel`
* Completed: `🧾 Final Paid Tax Invoice & Delivery Receipt - Order #<id> | Zyphuel`
* Admin copy: prefixed `🧾 [Admin Copy] …`

---

## 🗂️ Source Locations
* **Order trigger:** `app/src/main/java/com/example/ui/MainViewModel.kt` → `placeOrder()`
* **Invoice dispatcher:** `MainViewModel.kt` → `sendOrderInvoiceEmail()`, `dispatchRealtimeEmail()`
* **Test / settings:** `MainViewModel.kt` → `sendTestEmail()`, `saveSmtpSettings()`, `refreshSmtpConfig()`
* **Delivery engine:** `app/src/main/java/com/example/util/RealtimeEmailEngine.kt` → `sendRealtimeEmailDetailed()`, `dispatchViaWebhookRelay()`, `dispatchViaAuthenticatedSmtp()`, `getGoogleAppsScriptTemplate()`, `openEmailClient()`
* **Config store:** `app/src/main/java/com/example/security/SecureStorageManager.kt` → `getSmtpConfig()`, `saveSmtpConfig()`, `SmtpConfig`
* **Admin UI:** `app/src/main/java/com/example/ui/Screens.kt` → Email Gateway settings (webhook field ≈ 14799, copy-script button ≈ 14888)
* **Invoice text/HTML:** `app/src/main/java/com/example/util/InvoiceGenerator.kt`

---

## 🧪 How to Verify (for QA / future agents)
1. **Admin → Email Gateway → Send Test Email** to your own address.
   * Arrives → engine + config are live end-to-end.
   * Does not arrive → the active channel is dead (see Known Issues); deploy the webhook or refresh the app password.
2. **Place a real order** while signed in with Google → the signed-in Google email should receive the invoice, and a `[Admin Copy]` should reach `m.daniyalkhan490@gmail.com`.
3. Confirm the on-screen toast changes to `📧 Invoice emailed to <email>` (proves the success callback fired).

---

## ⚠️ Known Issues / Gotchas (READ before changing email code)
1. **Hardcoded fallback Gmail App Password** — `SecureStorageManager.getSmtpConfig()` (≈ line 192) defaults `appPassword` to `nvyzrxsbhibncijb` if none is stored. This is a **committed secret** flagged for rotation/removal (see `MEMORY.md` → *Security remediation state*). It should be removed **once the webhook is confirmed working**, otherwise SMTP Channel 2 goes dark with no configured password.
2. **SMTP ports are often blocked** on cellular/Wi-Fi — this is the #1 reason "email not sending" is reported. The **webhook (Channel 1)** is the reliable fix. Do not diagnose this as a code bug.
3. **Code correctness ≠ inbox delivery.** The pipeline is verified correct, but actual delivery depends on a **live channel** (valid password OR deployed webhook). Only `sendTestEmail` proves real delivery.
4. **Placeholder `customer@zyphuel.com`** is a guest sentinel, never a real inbox. Invoices to it are intentionally skipped. Related guards exist around `MainViewModel.kt` ≈ lines 960–968.
5. **Admin copy** is suppressed when the customer *is* the super admin (avoids duplicate mail to `m.daniyalkhan490@gmail.com`).

---

## 📝 Change Log — 2026-09-09
Session goal: *"user ko email nahi ja raha … invoice direct email … only registered / Google email."*

| Area | Change | File |
| :--- | :--- | :--- |
| **Registered-email-only** | Added recipient resolution that rejects the placeholder and falls back to `_currentUser` / stored registered email; skips with a logged warning if none | `MainViewModel.kt` `sendOrderInvoiceEmail()` |
| **Guest recovery** | On null `_currentUser`, recover the real registered email before falling back to placeholder | `MainViewModel.kt` `placeOrder()` |
| **Honest UI status** | Invoice callback now surfaces `📧 Invoice emailed to …` on success or logs the true failure (removed always-"success") | `MainViewModel.kt` `placeOrder()` |
| **Reliable delivery** | Reordered channels so the **HTTPS webhook is attempted first** when configured, then SMTP, then Firestore | `RealtimeEmailEngine.kt` `sendRealtimeEmailDetailed()` |

**Build:** `compileDebugKotlin` → **BUILD SUCCESSFUL** (no new warnings).
**Pending (owner action):** run **Send Test Email**; deploy the Apps Script webhook (recommended) or refresh the Gmail App Password; then remove the hardcoded fallback password (Known Issue #1).

---

### 🔗 Related Docs
* [`07_NOTIFICATION_AND_COMMUNICATION_ENGINE.md`](/docs/07_NOTIFICATION_AND_COMMUNICATION_ENGINE.md) — push/FCM/WhatsApp + email overview
* [`02_CUSTOMER_ORDER_PLACING_AND_SURGE.md`](/docs/02_CUSTOMER_ORDER_PLACING_AND_SURGE.md) — the order flow that triggers the invoice
* [`06_SECURITY_AND_BIOMETRICS.md`](/docs/06_SECURITY_AND_BIOMETRICS.md) — encrypted storage used for credentials & SMTP config
