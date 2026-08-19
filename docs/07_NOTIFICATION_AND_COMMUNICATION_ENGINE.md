# 07. Notification & Communication Engine Documentation 🔔

## 📌 Category
**Push Notifications & Multi-Channel Communication**

## 🎯 Purpose & Overview
The Notification & Communication Engine delivers real-time notifications across system push notifications, Firebase Cloud Messaging (FCM), automated email receipts, WhatsApp hotline support, and an in-app notification center.

---

## ⚙️ Key Communication Functions
* `postLocalSystemNotification(title, body, orderId)` (`MainViewModel.kt`): Creates Android heads-up system push notifications under channel `zyphuel_order_updates` (Notification ID, High Importance).
* `notifyArrivingSoon(orderId)` (`MainViewModel.kt`): Dispatches push notification when driver enters $1\text{ km}$ boundary.
* `notifyReachedLocation(orderId)` (`MainViewModel.kt`): Dispatches push notification when driver reaches destination.
* `ZyphuelFcmService.kt`: Receives background FCM payloads, creates local system notification triggers, and persists notification history to Room DB (`NotificationEntity`).
* `dispatchRealtimeEmail(recipientEmail, subject, content)` (`MainViewModel.kt`): Generates instant automated email receipts for new orders, driver assignments, and admin customer registrations.
* **WhatsApp Hotline Deep-Link**: Direct button linking to official support line (`+92 323 0112464`).

---

## 🔔 Notification Types & Triggers
| Trigger Event | Channel / Method | Notification Title |
| :--- | :--- | :--- |
| **Order Out for Delivery** | System Push + FCM + Email | `Out for Delivery 🛵` |
| **Driver Within 1km** | System Push + Local Notification | `Arriving Soon 📍` |
| **Driver Reached Location** | High-Priority System Push + FCM | `Driver Reached Location! 📍` |
| **Order Completed** | System Push + FCM + Email Receipt | `Order Delivered 🎉` |
| **Promotional Price Alert** | FCM Payload | `Zyphuel Fuel Price Drop Alert` |

---

## 📁 Source Locations
* **FCM Service**: `app/src/main/java/com/example/notifications/ZyphuelFcmService.kt`
* **ViewModel Helpers**: `app/src/main/java/com/example/ui/MainViewModel.kt`
* **In-App Notification Screen**: `app/src/main/java/com/example/ui/Screens.kt` (`NotificationCenterScreen`)
* **Data Model**: `app/src/main/java/com/example/data/Models.kt` (`NotificationEntity`)

---

## 🔄 Push Notification Lifecycle
1. **Status Trigger**: Order status changes in `MainViewModel`.
2. **System Notification**: `postLocalSystemNotification` posts heads-up banner to Android system notification tray.
3. **FCM Payload**: `triggerWebPush` sends FCM payload to remote devices.
4. **Room Storage**: Notification is stored in `NotificationEntity` table for in-app inbox viewing.
