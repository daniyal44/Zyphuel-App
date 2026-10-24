# 🗄️ Zyphuel — Database Architecture, Backup & Recovery

**Last Updated:** <font color="#10b981"><b>2026-10-23 22:48:18 PKT</b></font>
**App Version:** <font color="#10b981"><b>v2.6.4.0.0.33 (Build 61)</b></font>
**Room Schema Version:** <font color="#10b981"><b>13</b></font>
**Document Status:** <font color="#10b981"><b>ACTIVE</b></font>

---

## 1. Storage topology

Zyphuel has **two** data stores. Understanding the split is what guarantees "no data loss on update".

| Layer | Engine | Role | Survives app update? | Survives uninstall / new device? |
| :--- | :--- | :--- | :--- | :--- |
| Local | Android **Room 2.7.0** over SQLite (`zyphuel_database`) | Offline-first cache + instant UI reads | ✅ Yes — same file, migrated in place | ❌ No (but `allowBackup="false"`; nothing is silently restored either) |
| Cloud | **Google Cloud Firestore** | Authoritative accounts, orders, live tracking, audit log, gateway config | ✅ Yes | ✅ Yes — same account, same documents |

**Rule:** anything a user must not lose is written to Firestore. Room is a cache/mirror, never the only copy
of an account or an order. This is why an app update, a reinstall, or a second device can never orphan data.

---

## 2. Local schema (Room, version 13)

Defined in [`app/src/main/java/com/example/data/Models.kt`](app/src/main/java/com/example/data/Models.kt)
(and `data/vehicle/VehicleModels.kt`). `exportSchema = false` — the migration code below is the schema contract.

### 2.1 `users`
Primary key: `email` (normalized `trim().lowercase()` on registration).

Key columns: `name`, `passwordHash`, `role` (`customer`/`rider`/`admin`), `phoneNumber`, `isVerified`,
`hasRequestedVerification`, vehicle fields, CNIC/licence document fields, `authProvider`,
`accountStatus`, `createdAt`, `updatedAt`.

Ownership: one row per person. `role` is assigned at signup (only `customer`/`rider` for public signup) and
is **immutable by the owner** in Firestore Rules.

### 2.2 `orders`
Primary key: `id` (autoincrement). `customerEmail` and `riderEmail` are the ownership columns.

Key columns: `serviceType`, `quantity`, `totalPrice`, `deliveryAddress`, `status`, `paymentMethod`,
`etaMinutes`, `rating`, `feedback`, `destLat/destLng`, `originLat/originLng`, `createdAt`.

Relationships: `orders.customerEmail → users.email`, `orders.riderEmail → users.email` (soft references,
not enforced by SQLite foreign keys, so a deleted account never cascades away order history).

### 2.3 `audit_logs`
Primary key: `id`. Columns: `action`, `performedBy`, `details`, `timestamp`. Append-only by convention;
Firestore denies `update`/`delete` on the cloud copy.

### 2.4 `notifications`
Primary key: `id`. Columns: `title`, `message`, `timestamp`, `targetRole`, `isRead`.

### 2.5 `marked_locations`
Primary key: `id`. Ownership column: `userEmail`. Columns: `label`, `address`, `latitude`, `longitude`,
`isPrimary`, `createdAt`. All DAO queries filter on `userEmail`.

### 2.6 `vehicles`
Primary key: `id`. Ownership column: `userEmail`. Columns: `nickname`, `vehicleType`, `make`, `model`,
`year`, `registrationNumber`, `fuelType`, `isEv`, `preferredServices`, `isDefault`, `createdAt`.

---

## 3. Ownership model (who may read what)

| Data | Owner | Enforced where |
| :--- | :--- | :--- |
| `users` row | the account itself; full visibility for admin | `UserDao` lookups + Firestore `users/{uid}` rule |
| `orders` | `customerEmail` (read), `riderEmail` (assigned rider, read/update status), admin (all) | DAO email filters + `orders/{orderId}` rule |
| `marked_locations`, `vehicles` | `userEmail` | every DAO query filters by `userEmail` |
| `audit_logs` | admin read; append by any signed-in client | Firestore rule (`update`/`delete` denied) |
| `notifications` | target role | DAO role filter |
| SMTP/gateway config | admin | `system_config/{doc}` rule |

Guessing another user's order ID cannot expose it: the local query is scoped by email and the cloud rule
requires `request.auth.token.email` to match `customerEmail`/`riderEmail`.

---

## 4. Cloud collections (Firestore)

`users/{uid}`, `orders/{orderId}`, `live_tracking/{orderId}`, `feedback/{id}`, `audit_logs/{id}`,
`system_config/{doc}` (SMTP gateway), `mail/{id}` (transactional invoices), `app_stats/{doc}`,
plus the app-version document used for update broadcasts. Rules live in [`firestore.rules`](firestore.rules)
and are summarized in [SECURITY.md §5](SECURITY.md).

---

## 5. Migration process

Every schema change ships as a coded `Migration` object; `fallbackToDestructiveMigration()` is **never**
used (it deletes all user data on a version bump — that was a real defect, fixed in Build 61).

```kotlin
val MIGRATION_12_TO_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Intentional no-op: identical schema, version counter only.
    }
}
```

Registration point (`AppDatabase.getDatabase` and `AppRepository`):

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "zyphuel_database")
    .addMigrations(MIGRATION_12_TO_13)
    .build()
```

Full procedure, verification and rollback limitations: [MIGRATIONS.md](MIGRATIONS.md).

---

## 6. Backup

### 6.1 What is backed up automatically

| Asset | Mechanism | Encryption | Retention |
| :--- | :--- | :--- | :--- |
| Accounts, orders, tracking, audit, gateway config | Firestore managed backups/PITR (Google Cloud) | At rest (Google-managed keys) + TLS in transit | Per the Firebase project's backup policy |
| Local Room DB | **Not** uploaded (`allowBackup="false"`, `data_extraction_rules` exclude app data) | n/a | n/a |

`allowBackup="false"` is deliberate: an unencrypted local SQLite file containing password hashes must not
be copied to Google Drive by the OS. Cloud Firestore already holds the authoritative copy.

### 6.2 Manual backup before a risky migration (owner procedure)

1. Firebase console → Firestore → **Export** the `(default)` database to a Cloud Storage bucket.
2. Pull a local copy for device inspection (debug builds only):
   `adb shell "run-as com.aistudio.zyphuel.appv2 cat databases/zyphuel_database" > backup_zyphuel.sqlite`
3. Record the export path, timestamp and app version in `changes.md`.
4. Keep the backup until the new build has been verified on a real device with real (test) accounts.

Backup credentials are Google Cloud IAM credentials and must never be placed in this repository.

### 6.3 Restore

- Cloud: Firebase console → Firestore → **Import** the exported collection set into a staging project
  first; never import over production without explicit owner approval.
- Local: `adb push backup_zyphuel.sqlite /data/data/com.aistudio.zyphuel.appv2/databases/zyphuel_database`
  (debug build, app force-stopped) then relaunch. The DB version must match the installed build; if the
  backup is older, Room runs the registered migrations forward.

### 6.4 Recovery objectives (documented targets, not a guarantee)

| Objective | Target | Basis |
| :--- | :--- | :--- |
| **RPO** (max data loss) | ≤ 24 h for cloud records | Daily Firestore export schedule; day-to-day writes are live-replicated |
| **RTO** (time to restore service) | ≤ 4 h | Import into a fresh project + repoint `google-services.json` + reissue a build |

If the Firebase project has no scheduled export configured, the effective RPO is "whatever the managed
backup window provides" — configure exports before the next release (owner action).

### 6.5 Restore test (performed / to be repeated each release)

1. Register a test account, place a test order, save a vehicle and a marked location.
2. Record the counts (`SELECT COUNT(*)` per table and the Firestore document count).
3. Install the new build over the old one (no uninstall) and reopen the app.
4. Confirm every row is still present, still owned by the same email, and the order is still readable.
5. For a backup test: export → import into staging → confirm the same counts.

---

## 7. Data retention

| Data | Retention | Deletion path |
| :--- | :--- | :--- |
| Account + profile | Until the user deletes the account | In-app **Delete Account** (`deleteCurrentAccount`) |
| Orders & invoices | Retained for accounting/history after account deletion | Admin-only delete |
| Audit logs | Append-only, retained for security review | None (immutable) |
| Live tracking | Ephemeral telematics; safe to prune after delivery | Admin/maintenance |
| Local cache (Room) | Until uninstall or explicit sign-out policy | OS |

**Routine app updates never delete, reset or truncate user data.** No automatic `DROP`, `TRUNCATE` or
destructive seed runs against production; `seedAdminIfNeeded()` only inserts the master admin if absent.

---

## Related documents

- [MIGRATIONS.md](MIGRATIONS.md) • [SECURITY.md](SECURITY.md) • [TESTING.md](TESTING.md)
- [docs/10_DATABASE_ROOM_PERSISTENCE_AND_MODELS.md](docs/10_DATABASE_ROOM_PERSISTENCE_AND_MODELS.md)
- [vibe-coding-context-files/database.md](vibe-coding-context-files/database.md)
