# Zyphuel Deletion & Purge Audit Log (`delete.md`)

## Version 2.6.4.0.0.11 Deletions & Cleanups

### 1. Database Model Retention Compliance
- **Rule Verification:** As mandated by `AGENTS.md`, definitions for `UserEntity`, `OrderEntity`, and `AuditLogEntity` are strictly retained and never purged.
- **Permanent Account Deletion Routine:** User records requested for deletion undergo clean erasure via `repository.userDao.deleteUser(user)` and `repository.auditLogDao.insertLog("ACCOUNT_DELETED")` in compliance with Google Play Developer Policies.

### 2. File & Build Artifacts Cleanups
- **Cleaned Build Cache Records:** Stale compiler build logs from earlier development iterations cleared.
- **Obsolete SharedPreferences Keys:** Legacy un-migrated price cache keys consolidated under `price_petrol`, `price_diesel`, `price_high_octane`, `price_lpg_gas`.

### 3. Deprecated Test Scaffolds
- Removed redundant standalone mock price assertion blocks that bypassed `FeeConstants`.
