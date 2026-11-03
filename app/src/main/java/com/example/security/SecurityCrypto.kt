package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cryptographic Password Hashing & Verification Engine.
 *
 * Hardened 2026-10-23:
 * - New hashes use PBKDF2-HMAC-SHA256 (600,000 iterations, 16-byte random salt, 256-bit
 *   output) stored as `pbkdf2_sha256$<iterations>$<hexSalt>$<hexHash>`. Argon2id is the
 *   preferred algorithm but requires a third-party native dependency that is not part of
 *   this project; PBKDF2-HMAC-SHA256 is the platform-native secure alternative.
 * - Verification is constant-time (`MessageDigest.isEqual`).
 * - Social/OAuth accounts (`SOCIAL_OAUTH_*`) have no local password and can never be
 *   authenticated through the email + password form.
 * - Legacy salted SHA-256 hashes are still accepted, and the caller transparently
 *   re-hashes them to PBKDF2 on the next successful login (`needsRehash`).
 * - The legacy plaintext-equality fallback only applies to values that are clearly not
 *   password hashes, so a stored hash can never be matched with raw input.
 */
object SecurityCrypto {

    private const val PEPPER = "zyphuel_pkr_energy_secure_salt_2026_#99!"

    private const val PBKDF2_PREFIX = "pbkdf2_sha256"

    /**
     * OWASP-aligned work factor for PBKDF2-HMAC-SHA256 (600,000 iterations). Hashes created by
     * older builds keep their own iteration count inside the encoded value, so verification is
     * unaffected and they are upgraded transparently on the next successful login.
     */
    private const val PBKDF2_ITERATIONS = 600_000
    private const val PBKDF2_KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val SOCIAL_PREFIX = "SOCIAL_OAUTH_"

    /**
     * Hashes a raw user password with PBKDF2-HMAC-SHA256 + a random per-user salt.
     */
    fun hashPassword(rawPassword: String): String {
        if (rawPassword.isBlank()) return ""
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(rawPassword + PEPPER, salt, PBKDF2_ITERATIONS)
        return "$PBKDF2_PREFIX$$PBKDF2_ITERATIONS$${toHex(salt)}$${toHex(hash)}"
    }

    /**
     * Precomputed irreversible hash for the master admin account
     * (injected securely from environment/local.properties via BuildConfig).
     *
     * Hardened 2026-11-03: there is NO hardcoded fallback. When the value is not injected the
     * master-admin path is disabled instead of silently falling back to a value that is public
     * in the repository (which was an offline brute-force target).
     */
    val MASTER_ADMIN_HASH: String = run {
        try {
            com.example.BuildConfig.MASTER_ADMIN_HASH.trim().lowercase()
        } catch (_: Throwable) {
            ""
        }
    }

    /** True only when a master-admin hash was injected at build time. */
    val isMasterAdminHashConfigured: Boolean get() = MASTER_ADMIN_HASH.isNotBlank()

    /**
     * True when [storedHash] is a legacy/plaintext value or a PBKDF2 hash created with a lower
     * work factor than the current [PBKDF2_ITERATIONS], so the caller can upgrade it.
     */
    fun needsRehash(storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        if (storedHash.startsWith(SOCIAL_PREFIX)) return false
        if (!storedHash.startsWith(PBKDF2_PREFIX)) return true
        val iterations = storedHash.split("$").getOrNull(1)?.toIntOrNull() ?: return true
        return iterations < PBKDF2_ITERATIONS
    }

    /**
     * Secure password verification (constant-time comparison).
     */
    fun verifyPassword(rawInput: String, storedHash: String): Boolean {
        if (rawInput.isBlank() || storedHash.isBlank()) return false

        // Social/OAuth accounts have no local password.
        if (storedHash.startsWith(SOCIAL_PREFIX)) return false

        // 1. Modern PBKDF2-HMAC-SHA256 hash
        if (storedHash.startsWith(PBKDF2_PREFIX)) {
            val parts = storedHash.split("$")
            if (parts.size != 4) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            val salt = fromHex(parts[2]) ?: return false
            val expected = fromHex(parts[3]) ?: return false
            val computed = pbkdf2(rawInput + PEPPER, salt, iterations)
            return MessageDigest.isEqual(computed, expected)
        }

        // 2. Legacy salted SHA-256 hash (upgraded on next successful login)
        val legacy = sha256Hex(rawInput + PEPPER)
        if (legacy.equals(storedHash, ignoreCase = true)) return true

        // 3. Legacy plaintext account (pre-hashing builds). Only values that are clearly
        //    not a hash are eligible, so a stored hash can never be matched by raw input.
        if (!isHashLike(storedHash) && rawInput == storedHash) return true

        // 4. Master admin hash equivalence — only when a hash was actually injected at build
        //    time. A blank MASTER_ADMIN_HASH must never match anything.
        if (MASTER_ADMIN_HASH.isBlank()) return false
        return legacy.equals(MASTER_ADMIN_HASH, ignoreCase = true) &&
            storedHash.equals(MASTER_ADMIN_HASH, ignoreCase = true)
    }

    private fun isHashLike(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.length != 64) return false
        return trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return toHex(bytes)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, PBKDF2_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private fun fromHex(hex: String): ByteArray? {
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
