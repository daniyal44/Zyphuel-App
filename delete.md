# Zyphuel Deletion & Purge Audit Log (`delete.md`)

## Version 2.6.4.0.0.24 Deletions, Suppressions & Cleanups — <font color="#10b981"><b>2026-10-09</b></font>

### 1. Off-Hours UI Button Suppression (<font color="#10b981"><b>2026-10-09</b></font>)
- **Order Placement Actions Omitted When Closed:** In strict compliance with the delivery operating hours directive:
  - Removed "Order Now" Floating Action Button (`home_fab`) from the Compose hierarchy on [`CustomerHomeScreen`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/ui/Screens.kt) whenever `!isDeliveryOpen`.
  - Removed "Place Delivery Order / Confirm Order COD" button (`confirmButton`) from [`OrderDialog`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/ui/Screens.kt) whenever `!isDeliveryOpen`.
  - Removed "Reorder 🔁" button on past customer order cards (`CustomerOrderCard`) whenever `!isDeliveryOpen`.
- **Zero Accidental Submission:** Ensured buttons are completely omitted (not merely disabled) so users cannot interact or submit off-hour requests.

### 2. Purge of Visible Markup / Surcharge Debug Text (<font color="#10b981"><b>2026-10-09</b></font>)
- **Eliminated User-Facing Markup Labels:** Purged all visible `+Rs 5.00/L surcharge` labels and debug text from customer order cards, summary dialogs, and item cards.
- **Silent Computation:** The OGRA pump markup calculation (`finalRate = ograBase + 5.00`) runs strictly in the background; only the clean, final retail pump rate is shown.

### 3. Decommission of Obsolete Delivery Pricing Tiers (<font color="#10b981"><b>2026-10-09</b></font>)
- **Purged Legacy Rate Tiers:** Cleaned out older flat delivery fee constants (Rs. 250, Rs. 280, Rs. 350) in favor of the standardized per-liter tiered structure in [`FeeConstants.kt`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/util/FeeConstants.kt) (1..10L: Rs. 300, 11L: Rs. 320 .. 15L: Rs. 400).
- **Purged Uncapped Stepper Bounds:** Capped all fuel selection steppers and preset buttons strictly to the 15-liter maximum cap.

### 4. Database & Entity Retention Verification (<font color="#10b981"><b>2026-10-09</b></font>)
- **Zero Deletion Safety Rule Compliance:** In accordance with `AGENTS.md`, core entity definitions for [`UserEntity`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/data/UserEntity.kt), [`OrderEntity`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/data/OrderEntity.kt), [`AuditLogEntity`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/data/AuditLogEntity.kt), and [`NotificationEntity`](file:///d:/Games/New%20folder-web/Claude/app/src/main/java/com/example/data/NotificationEntity.kt) remain intact with zero structural deletions.
- **Account Deletion Protocol:** Permanent customer account erasure retains compliance via `userDao.deleteUser(user)` and `auditLogDao.insertLog("ACCOUNT_DELETED")` in compliance with Google Play Developer Policies.
