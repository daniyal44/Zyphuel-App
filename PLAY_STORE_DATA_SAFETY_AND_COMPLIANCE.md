# Google Play Console: Data Safety, Permissions & Store Compliance Guide

**Application:** Zyphuel Doorstep Fuel & Energy Delivery  
**Package Name:** `com.aistudio.zyphuel.appv2`  
**Current Version:** <font color="#10b981"><b>2.6.4.0.0.26 (Build 54)</b></font>  
**Target SDK:** 36 (Android 16) | **Min SDK:** 24 (Android 7.0)  
**Last Synchronized:** <font color="#10b981"><b>2026-10-09</b></font>

---

## 1. Google Play Console: Data Safety Form Answers

When filling out the **Data safety** questionnaire in Google Play Console (Policy > App content > Data safety), answer exactly as specified below:

### Overview Questions
* **Does your app collect or share any of the required user data types?**  
  👉 **Yes**
* **Is all of the user data collected by your app encrypted in transit?**  
  👉 **Yes** (All network traffic uses TLS/HTTPS)
* **Do you provide a way for users to request that their data be deleted?**  
  👉 **Yes**
* **Add your account deletion URL:**  
  👉 `https://www.zyphuel.com/request-deletion` (also supported in-app via *Profile > Delete Account*)

---

### Data Types Declaration Table

| Category | Data Type | Collected? | Shared? | Purpose | Ephemeral? | Required or Optional? |
|---|---|---|---|---|---|---|
| **Location** | Approximate location (`ACCESS_COARSE_LOCATION`) | **Yes** | No | App functionality (zone discovery, address tagging) | No | Required |
| **Location** | Precise location (`ACCESS_FINE_LOCATION`) | **Yes** | No | App functionality (delivery pin drop, rider route navigation) | No | Required |
| **Personal Info** | Name | **Yes** | No | App functionality, Account management | No | Required |
| **Personal Info** | Email address | **Yes** | No | App functionality, Account management, Security | No | Required |
| **Personal Info** | Phone number | **Yes** | No | App functionality, Order delivery coordination | No | Required |
| **Personal Info** | Address | **Yes** | No | App functionality (doorstep delivery fulfillment) | No | Required |
| **Personal Info** | Government ID (Rider CNIC / License) | **Yes** (Riders only) | No | Fraud prevention, Compliance & Safety | No | Required for Riders |
| **Financial Info**| Purchase history & Payment method | **Yes** | No | App functionality, Invoicing, Accounting | No | Required |
| **App Info** | Crash logs & Diagnostics | **Yes** | Shared with Google/Firebase | Analytics, App functionality | No | Optional |
| **Device IDs** | Device / Firebase Cloud Messaging Token | **Yes** | Shared with Google/Firebase | App functionality (Push notifications for delivery status) | No | Required |

---

## 2. Sensitive Permissions Justification Declaration

### A. Location Permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)
* **User-Facing Description in Play Console:**  
  *"Zyphuel requires device location to identify the user's doorstep delivery address and enable dispatch riders to navigate directly to the customer's vehicle or storage tank."*
* **When is it requested?**  
  In-context, when searching delivery areas or selecting delivery address on map.

### B. Foreground Service Location (`FOREGROUND_SERVICE_LOCATION`)
* **Declared Foreground Service Type:** `location`
* **Service Class:** `.service.RiderLocationForegroundService`
* **Purpose:**  
  *"The delivery rider's live position is tracked during active order fulfillment to calculate exact arrival times, trigger customer arrival geofences, and provide live telematic tracking to the customer on Google Maps. A permanent foreground notification is always visible to the rider while tracking is active."*

### C. Push Notifications (`POST_NOTIFICATIONS`)
* **Purpose:** Order placement confirmations, rider dispatched updates, arrival alerts, and invoice delivery notifications.

---

## 3. Google Play Store Listing Essentials

### A. Privacy Policy URL
Enter this URL in **Store presence > Store settings > Privacy Policy**:  
👉 `https://www.zyphuel.com/privacy-policy`

### B. App Content & Target Audience
* **Target Age Group:** 18 and over (Mark 18+ due to hazardous materials / fuel delivery operations).
* **Could your app appeal to children?**  
  👉 **No**.

### C. Financial Features Declaration
* **Does your app provide financial features?**  
  👉 Select: *Digital wallet / Payment checkout facilitation* (Cash on Delivery, JazzCash / EasyPaisa / Bank transfer invoice recording). Zyphuel does not issue credit or loans.

### D. Regulated Goods & Services
* Petroleum fuel delivery is categorized under essential logistics. Ensure OGRA compliance, strict 15L delivery cap, <font color="#10b981"><b>tiered delivery fees (5L: Rs. 300, 7L: Rs. 300, 10L: Rs. 300, 11L: Rs. 320, 12L: Rs. 340, 13L: Rs. 360, 14L: Rs. 380, 15L: Rs. 400)</b></font>, official delivery operating hours (<font color="#10b981"><b>Mon–Thu 08:00 AM – 08:00 PM, Fri 08:00 AM – 01:00 PM, Sat–Sun 10:00 AM – 06:00 PM PKT</b></font>), and Rider Safety Declaration are active in the app. Note: Drinking Water and LPG Gas cylinders are currently unavailable and blocked across all ordering flows.

---

## 4. App Version History Log

* <font color="#10b981"><b>v2.6.4.0.0.24 (Build 52) — 2026-10-09</b></font>:
  - Enforced official delivery operating hours window (Mon–Thu 08:00–20:00, Fri 08:00–13:00, Sat–Sun 10:00–18:00 PKT) with dynamic order action button hiding and database hard guards.
  - Standardized `PUMP_RATE_MARKUP = 5.00` Rs./Litre silent markup on OGRA ex-depot base rates for Super Euro-V Petrol, Euro-V Diesel, and High-Octane 97.
  - Updated per-liter tiered delivery fees: 5L–10L (Flat Rs. 300), 11L (Rs. 320), 12L (Rs. 340), 13L (Rs. 360), 14L (Rs. 380), 15L max cap (Rs. 400).
  - Integrated high-frequency trading-style git activity graph engine generating vector SVG and 1D/1W/1M velocity telemetry tables.
  - Confirmed 100% unit test success (`FuelPumpPricingTest`) and strict zero-credential leak compliance.
