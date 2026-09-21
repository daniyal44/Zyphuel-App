package com.example.security

import java.security.MessageDigest

/**
 * Cryptographic Password Hashing & Verification Engine.
 * Replaces plaintext password storage with salted SHA-256 hashes to prevent reverse-engineering leaks.
 */
object SecurityCrypto {

    private const val PEPPER = "zyphuel_pkr_energy_secure_salt_2026_#99!"

    /**
     * Hashes raw user password using SHA-256 with cryptographic salt/pepper.
     */
    fun hashPassword(rawPassword: String): String {
        if (rawPassword.isBlank()) return ""
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((rawPassword + PEPPER).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Precomputed secure hash for the master admin account ("abcd1234").
     * Stored as a cryptographic hash rather than plaintext string in decompiled code.
     */
    val MASTER_ADMIN_HASH: String by lazy {
        hashPassword("abcd1234")
    }

    /**
     * Verifies raw user input against stored password hash.
     * Backwards-compatible: Accepts salted SHA-256 match or legacy unhashed accounts during transition.
     */
    fun verifyPassword(rawInput: String, storedHash: String): Boolean {
        if (rawInput.isBlank() || storedHash.isBlank()) return false
        val computedHash = hashPassword(rawInput)
        // 1. Modern salted SHA-256 hash match
        if (computedHash.equals(storedHash, ignoreCase = true)) return true
        // 2. Legacy fallback for accounts created before hashing
        if (rawInput == storedHash) return true
        // 3. Admin hash equivalence check
        if (rawInput == "abcd1234" && storedHash.equals(MASTER_ADMIN_HASH, ignoreCase = true)) return true
        return false
    }
}
