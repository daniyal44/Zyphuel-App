# 11. Real-Time Order Invoice & Email Delivery Documentation 📧🧾

> **Current App Version:** `2.6.4.0.0.24 (Build 52)`  
> **Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>  
> **Status:** <font color="#10b981"><b>Production Verified (100% Tests Passing)</b></font>

## 📌 Category
**Automated Transactional Email — Order Confirmation & Official Tax Invoice Delivery**

## 🎯 Purpose & Overview
When a customer places an order, the app **automatically emails an official order confirmation + itemized tax invoice** to the customer. The email is delivered **only to a genuine registered email** — the exact address the user signed in with via **"Continue with Google"** or sign-up. This module guarantees that behavior end-to-end and delivers it through a **resilient multi-channel engine** so an email is not lost when a mobile network blocks SMTP ports.

---

## ✅ Requirements & How Each Is Met

| # | Requirement | Status | Where Implemented |
| :-: | :--- | :---: | :--- |
| 1 | User **must receive an email** when an order is placed | ✅ Done | `placeOrder()` triggers `sendOrderInvoiceEmail(order, isCompletedReceipt = false)` right after the order is created — `MainViewModel.kt` (`placeOrder`) |
| 2 | The **invoice** must be sent **directly by email** | ✅ Done | `sendOrderInvoiceEmail()` builds a plain-text receipt + HTML invoice via `InvoiceGenerator` and dispatches them — `MainViewModel.kt` (`sendOrderInvoiceEmail`) |
| 3 | Email must go **only to the registered email** (same as "Continue with Google") — never a placeholder | ✅ Done | Recipient resolution rejects the guest placeholder `customer@zyphuel.com` and falls back to the stored registered email; skips entirely if none exists — `MainViewModel.kt` (`sendOrderInvoiceEmail`) |
| 4 | Delivery must be **reliable** (not silently fail) | ✅ Done | Webhook-first channel order + real success/failure surfaced to UI |

---

## 🔗 End-to-End Delivery Pipeline

1. **Login** — "Continue with Google" → `loginWithSocialAccount(...)` → `completeLogin(user)` sets `_currentUser.value = user` (the Google email). Registered email is also persisted encrypted via `SecureStorageManager.saveSecureCredentials(...)`.
2. **Order placed** — `placeOrder()` (`MainViewModel.kt`) creates the order with `customerEmail = user.email`, verifying `DeliveryOperatingHoursManager.isDeliveryWindowOpen()` and 15L cap.
3. **Invoice dispatch** — after the order is saved and the UI confirms, a background block calls `sendOrderInvoiceEmail(order)`. The callback sets `📧 Invoice emailed to …` on success, or logs the real failure reason.
4. **Recipient resolution** — `sendOrderInvoiceEmail()` resolves the **true** recipient (rejecting the placeholder), then generates the invoice and calls `dispatchRealtimeEmail(...)` for the customer **and** a `[Admin Copy]`.
5. **Engine dispatch** — `dispatchRealtimeEmail()` calls `RealtimeEmailEngine.sendRealtimeEmailDetailed(...)` with the active `_smtpConfig`, writes an audit log (`REALTIME_EMAIL_DELIVERED` / `REALTIME_EMAIL_ATTEMPTED`), stores a `NotificationEntity`, and posts a system notification on success.
6. **Multi-channel send** — `RealtimeEmailEngine` attempts each channel in priority order (below) and returns the first success.

---

## 📮 Multi-Channel Delivery Order (`RealtimeEmailEngine.sendRealtimeEmailDetailed`)

Attempted in this exact order; first success wins:

| Priority | Channel | When Used | Why |
| :-: | :--- | :--- | :--- |
| **1** | <font color="#10b981"><b>HTTPS Webhook Relay</b> (Google Apps Script, port 443)</font> | Only if a `webhookUrl` is configured | Fast, **stores no secret in the app**, and is **immune to carriers/networks that block SMTP ports 465/587**. Tried first so a dead SMTP password can't cause a ~12 s hang. |
| **2** | **Authenticated Direct SMTP** (TLS 465 / STARTTLS 587) | If `isEnabled` and an app password is present | Direct RFC 5321 delivery from the sender Gmail. |
| **3** | **Cloud Firestore `mail` collection** | Fallback | Consumed by the Firebase "Trigger Email" extension if installed. |

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

---

## 🧪 How to Verify (for QA / future agents)
1. **Admin → Email Gateway → Send Test Email** to your own address.
2. **Place a real order** while signed in with Google → the signed-in Google email should receive the invoice, and a `[Admin Copy]` should reach the admin address.
3. Confirm the on-screen toast changes to `📧 Invoice emailed to <email>` (proves the success callback fired).

---

## 📝 Change Log — <font color="#10b981"><b>2026-10-09 (Build 52)</b></font>

| Area | Change | File |
| :--- | :--- | :--- |
| <font color="#10b981"><b>Operating Hours Delivery Gate</b></font> | Invoices dispatched only for orders successfully placed within official delivery hours (Mon–Thu 08:00–20:00, Fri 08:00–13:00, Sat–Sun 10:00–18:00 PKT) | `MainViewModel.kt` `placeOrder()` |
| <font color="#10b981"><b>Tiered Delivery Fee Invoicing</b></font> | Invoice itemization correctly reflects updated tiered delivery fees (Rs. 300 up to 10L; +Rs. 20/L up to 15L cap) | `InvoiceGenerator.kt` |
| <font color="#10b981"><b>Silent Retail Markup</b></font> | Invoice itemizes final retail pump rate (`ograBase + 5.00`) cleanly without internal debug labels | `InvoiceGenerator.kt` |
| <font color="#10b981"><b>Reliable Delivery</b></font> | Webhook relay priority ensured; verified against Build 52 test suite | `RealtimeEmailEngine.kt` |

---

### 🔗 Related Docs
* [`07_NOTIFICATION_AND_COMMUNICATION_ENGINE.md`](/docs/07_NOTIFICATION_AND_COMMUNICATION_ENGINE.md) — push/FCM/WhatsApp + email overview
* [`02_CUSTOMER_ORDER_PLACING_AND_SURGE.md`](/docs/02_CUSTOMER_ORDER_PLACING_AND_SURGE.md) — the order flow that triggers the invoice
* [`06_SECURITY_AND_BIOMETRICS.md`](/docs/06_SECURITY_AND_BIOMETRICS.md) — encrypted storage used for credentials & SMTP config
