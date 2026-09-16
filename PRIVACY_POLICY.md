# Privacy Policy for Zyphuel

**Effective Date:** September 2026  
**Last Updated:** September 2026  
**Application Version:** 2.6.4.0.0.10 (Build 38)  
**Package Name:** `com.aistudio.zyphuel.appv2`  
**Official Website:** [https://www.zyphuel.com/](https://www.zyphuel.com/)  
**Privacy Policy URL:** [https://www.zyphuel.com/privacy](https://www.zyphuel.com/privacy)  
**Terms of Use URL:** [https://www.zyphuel.com/terms-of-use](https://www.zyphuel.com/terms-of-use)  
**Support Email:** m.daniyalkhan490@gmail.com  
**WhatsApp Helpline:** +92 323 0112464

---

> [!IMPORTANT]
> **⚠️ CRITICAL STARTUP STATUS & OPERATIONAL SCALE DISCLOSURE:**  
> **Zyphuel is an independent, early-stage technology and doorstep energy logistics STARTUP founded and based in Lahore, Punjab, Pakistan. Zyphuel is NOT a large corporate conglomerate, multinational entity, or established oil marketing company (OMC). All services, dispatches, customer care, and operations are managed by our dedicated local founding and dispatch team. We are actively developing our services on a startup scale across Lahore.**

---

## 1. Introduction
Welcome to **Zyphuel** ("we", "our", or "us"). Zyphuel is an early-stage on-demand fuel delivery, clean energy, and vehicle support startup operating in Lahore, Pakistan. We are committed to protecting the privacy, confidentiality, and security of our customers, service technicians, riders, and administrative personnel.

This Privacy Policy explains how we collect, use, disclose, and safeguard your personal information when you access or use the Zyphuel Android application and our online services. By installing, creating an account on, or using Zyphuel, you consent to the practices described in this Privacy Policy.

---

## 2. Information We Collect

### A. Location Information (Prominent Disclosure)
Zyphuel requires access to device location services to fulfill on-demand doorstep deliveries and emergency roadside dispatch:
* **Precise GPS Location (`ACCESS_FINE_LOCATION`) & Approximate Location (`ACCESS_COARSE_LOCATION`)**:
  - **For Customers**: Collected when you search for nearby service coverage, mark drop-off locations, or place orders for Fuel Delivery (Euro-V Regular Petrol, HOBC 97 Octane, High-Speed Diesel), Sealed LPG Cylinders, Emergency Battery Boost, or Pure Drinking Water.
  - **For Riders & Field Technicians**: Collected during active service shifts to calculate optimal driving routes, navigate to customer coordinates, and trigger geofenced customer notifications (e.g., "Rider is 1 km away").
* **Foreground Service Location (`FOREGROUND_SERVICE_LOCATION`)**:
  - Used by delivery riders and roadside technicians via `RiderLocationForegroundService` exclusively while active on dispatch assignments to provide live telematic tracking to the customer and dispatch console. A persistent notification is displayed whenever this service is operating.
  - **We DO NOT collect continuous background location without an active delivery or assistance assignment.**

### B. Personal Identifiable Information (PII)
* **Customer Accounts**: Full Name, Email Address, Phone Number, Delivery Addresses, and Account Password (cryptographically hashed using SHA-256 with salting).
* **Rider & Field Technician Profiles**: Full Name, Phone Number, Father's Name, Date of Birth, National Identity Card (CNIC / Passport number), CNIC Issue/Expiry dates, Residential Address, Vehicle Type & Plate Registration Number, and Driving License ID.
* **Emergency Contacts**: For delivery fleet safety, riders provide an emergency contact name, relationship, and contact number.

### C. Financial & Transaction Data
* Orders placed, quantities ordered (Liters / Kilograms / Units), order timestamps, discount vouchers applied, payment methods selected (Cash on Delivery, JazzCash, EasyPaisa, or direct bank transfer), and automated electronic tax invoice records.
* **Payment Security**: Zyphuel never collects, processes, or stores raw credit/debit card numbers or CVV codes on our servers. Transaction confirmations and references are stored for accounting and invoice reconciliation.

### D. Device and Technical Data
* Hardware model, operating system version, unique device identifiers, Firebase Cloud Messaging (FCM) tokens for delivery status notifications, and crash diagnostics.

---

## 3. How We Use Your Information
We process your personal information strictly for legitimate operational purposes:
1. **Order Processing & Doorstep Delivery**: Connecting customers with nearby authorized fuel and energy dispatch riders.
2. **Real-Time Live Telematics**: Displaying real-time rider progression on live Google Maps to ensure route accuracy and prompt delivery.
3. **Safety & Regulatory Compliance**: Adhering to Oil & Gas Regulatory Authority (OGRA) and civil safety guidelines concerning the secure transportation and handling of petroleum and LPG products.
4. **Order Status Notifications**: Dispatching real-time status updates via push notifications, transactional emails, and SMS alerts.
5. **Fraud Prevention & Account Security**: Enforcing rate limits, brute-force protection, biometric authentication, and audit logs.

---

## 4. Information Sharing & Third Parties
We do not sell, rent, or trade your personal data to advertisers or commercial third parties. We share data only with trusted infrastructure providers required to operate the service:
* **Google Play Services & Google Maps SDK**: For map rendering, geocoding, and distance calculations.
* **Firebase (Google LLC)**: For authentication session verification, crash analytics, and push notification delivery (FCM).
* **Law Enforcement & Regulatory Authorities**: Solely when legally obligated by court order, law enforcement inquiry, or governmental emergency safety directive.

---

## 5. Data Storage, Security & Retention
* **Encrypted Local Storage**: Sensitive session tokens and security preferences are protected using Android Jetpack `EncryptedSharedPreferences` and SQLite Room database with AES-256 encryption.
* **Data in Transit**: All communications between the mobile application and backend services are encrypted over TLS/SSL (HTTPS).
* **Retention Policy**: Transaction records and order invoices are retained for the statutory period required by taxation and trade laws. Non-essential telemetry is purged periodically.

---

## 6. Google Play Account Deletion Policy & Your Rights
In strict compliance with Google Play's User Data & Account Deletion policies:
* **In-App Account Deletion**: Any customer or rider can permanently delete their account and associated personal data at any time:
  1. Open the Zyphuel App.
  2. Open the Navigation Drawer or Profile Settings.
  3. Select **"Delete Account / Erase Data"**.
  4. Confirm your selection on the modal.
* **Web-Based Deletion Request**: If you have uninstalled the app or prefer to request deletion online, you can submit an account and data erasure request by visiting [https://www.zyphuel.com/privacy](https://www.zyphuel.com/privacy) or emailing `m.daniyalkhan490@gmail.com`. Your request will be fulfilled within 30 days.

---

## 7. Children's Privacy
Zyphuel's services are strictly intended for individuals aged 18 years or older due to the regulated nature of energy and fuel distribution. We do not knowingly collect personal information from minors.

---

## 8. Changes to This Privacy Policy
We may revise this Privacy Policy periodically to reflect new features, updated legal requirements, or Android OS permission changes. When updates are published, the "Last Updated" date at the top will be updated, and the in-app version will reflect the current version.

---

## 9. Contact Information
If you have any questions, concerns, or inquiries regarding this Privacy Policy, please contact our Data Protection Team:
* **Support Email**: m.daniyalkhan490@gmail.com
* **WhatsApp / Phone Helpline**: +92 323 0112464
* **Company**: Zyphuel Pakistan
* **Address**: 75-Main Boulevard, Gulberg III, Lahore, Punjab, Pakistan
* **Official Website**: [https://www.zyphuel.com/](https://www.zyphuel.com/)
