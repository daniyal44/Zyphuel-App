# 🔁 Zyphuel — Database Migrations

**Last Updated:** <font color="#10b981"><b>2026-10-23 22:48:18 PKT</b></font>
**App Version:** <font color="#10b981"><b>v2.6.4.0.0.33 (Build 61)</b></font>
**Current Room Version:** <font color="#10b981"><b>13</b></font>
**Document Status:** <font color="#10b981"><b>ACTIVE</b></font>

---

## 1. Non-negotiable rules

1. **Never** call `fallbackToDestructiveMigration()` / `fallbackToDestructiveMigrationOnDowngrade()`.
   Both wipe every user table the moment the schema version changes.
2. Every `@Database(version = N)` bump **must** ship a `Migration(N-1, N)` and be registered in **both**
   builders: `AppDatabase.getDatabase()` (Models.kt) and `AppRepository` (Repository.kt).
3. Migrations are additive and idempotent-safe: `ADD COLUMN` with a `DEFAULT`, `CREATE TABLE IF NOT EXISTS`,
   `CREATE INDEX IF NOT EXISTS`. No `DROP TABLE`, no `DELETE`, no `UPDATE` that rewrites existing user data.
4. Destructive migrations are **prohibited on production data**. If a column must be removed, deprecate it
   in code first and drop it in a later, explicitly approved release.
5. A migration must be preceded by a verified backup ([DATABASE.md §6.2](DATABASE.md)) and a restore test.
6. Ship the migration in a build that is installed **over** the previous build (no uninstall) to prove data
   preservation. An uninstall/reinstall proves nothing.

---

## 2. Migration registry

| Version | Updater | Purpose | Data impact | Backup required |
| :---: | :--- | :--- | :--- | :--- |
| 11 → 12 | legacy | Added `vehicles` (VehicleEntity/VehicleDao) | New table only; no existing rows touched | No |
| **12 → 13** | `MIGRATION_12_TO_13` (Models.kt) | Schema-identical version bump that **replaces the destructive fallback** | **None** — no-op `migrate()`; all rows preserved | Recommended |

`MIGRATION_12_TO_13` exists precisely so an install-over keeps accounts, orders, audit logs, notifications,
marked locations and vehicles instead of dropping the database. The no-op body is intentional: bumping the
version without a registered migration is what previously forced data loss.

---

## 3. Adding a new migration (checklist)

1. Bump `@Database(version = 14)`.
2. Add the entity changes (new nullable columns with defaults, or new tables).
3. Write `MIGRATION_13_TO_14` next to `MIGRATION_12_TO_13`.
4. Register it in **both** builders: `.addMigrations(MIGRATION_12_TO_13, MIGRATION_13_TO_14)`.
5. Add a test in [`SecurityAndDataIntegrityTest.kt`](app/src/test/java/com/example/SecurityAndDataIntegrityTest.kt)
   that opens a v13 database with rows, runs the migration via Room, and asserts every row survives.
6. Bump `versionCode`/`versionName` (AGENTS.md rule) and add a `changes.md` entry with the migration note.
7. Update this table, [DATABASE.md](DATABASE.md) and [CHANGELOG section of changes.md](changes.md).

---

## 4. Execution (device)

```text
Existing install (DB v12)
        │  user installs new APK over the old one
        ▼
Room opens zyphuel_database, sees stored version 12 < expected 13
        │
        ▼
Runs MIGRATION_12_TO_13.migrate()  (no-op)  → sets user_version = 13
        │
        ▼
All rows remain; DAOs read the same data
```

Verification commands (debug build):

```bash
adb shell "run-as com.aistudio.zyphuel.appv2 ls -l databases/"
adb shell "run-as com.aistudio.zyphuel.appv2 sqlite3 databases/zyphuel_database 'PRAGMA user_version;'"
adb shell "run-as com.aistudio.zyphuel.appv2 sqlite3 databases/zyphuel_database 'SELECT COUNT(*) FROM users;'"
```

Expected after the update: `PRAGMA user_version` = `13`, user/order counts identical to before.

---

## 5. Failure handling & recovery

A migration that throws aborts the open; Room leaves the file unchanged at its previous `user_version`, so the
worst case is "new build cannot open the old DB", never "half-migrated data".

Recovery procedure:

1. Stop using the new build; reinstall the previous version (same signing key) — the DB is still at its old
   version and opens normally.
2. Reproduce the failure on a copy of the exported DB, fix the migration, and re-run the full test suite.
3. If the file itself is corrupted, restore from the pre-migration export (§[DATABASE.md 6.3](DATABASE.md))
   or re-sync from Firestore, which holds the authoritative account/order records.

### Rollback limitations (be explicit)

- Room has **no down-migration** by default. Rolling back an app version after a schema bump requires a
  registered `Migration(14, 13)`; shipping one is only worth it when a real rollback is planned.
- Firestore has no transactional rollback of client writes; restore from export into a staging project and
  copy back deliberately.
- Once a destructive statement has run, the data is gone — which is why destructive statements are banned.

---

## 6. Compatibility policy

| Item | Policy |
| :--- | :--- |
| Backward-compatible schema changes | Preferred: nullable columns + defaults; old rows remain readable |
| Minimum supported app version | Enforced via the app-version document (`latestVersionCode`); `forceUpdate = true` makes the update banner non-dismissible |
| API/field compatibility | Firestore documents are read defensively (`?: default`), so a document written by an older build still parses |
| Downgrade (new DB → old app) | Not supported without an explicit down-migration; Play Store never downgrades an installed app |

---

## Related documents

- [DATABASE.md](DATABASE.md) — schema, ownership, backup/restore, retention
- [TESTING.md](TESTING.md) — data-preservation and repeat-migration tests
- [docs/10_DATABASE_ROOM_PERSISTENCE_AND_MODELS.md](docs/10_DATABASE_ROOM_PERSISTENCE_AND_MODELS.md)
