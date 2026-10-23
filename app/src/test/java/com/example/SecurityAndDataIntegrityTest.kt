package com.example

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.MIGRATION_12_TO_13
import com.example.data.OrderEntity
import com.example.data.UserEntity
import com.example.security.RateLimitConfig
import com.example.security.RateLimitResult
import com.example.security.SecurityCrypto
import com.example.security.SecurityInputValidator
import com.example.security.SecurityRateLimiter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * Automated security & data-integrity suite (added 2026-10-23).
 *
 * Covers the master security requirements:
 * - Brute-force lockout: 4 wrong passwords / 1 minute -> 5 minute error-422 cooldown.
 * - Restart cannot bypass the cooldown (persisted lockout state).
 * - SQL-injection payloads cannot authenticate / create accounts.
 * - Duplicate (case-insensitive) email registration is rejected.
 * - User A cannot read User B's orders (local ownership scoping).
 * - Room records survive database close/reopen (app update simulation) with MIGRATION_12_TO_13.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityAndDataIntegrityTest {

    private val client = "unit_test_client"
    private lateinit var context: Context
    private lateinit var originalConfig: RateLimitConfig

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        originalConfig = SecurityRateLimiter.getConfig()
        SecurityRateLimiter.initialize(context)
        SecurityRateLimiter.resetAllLimits()
    }

    @After
    fun tearDown() {
        SecurityRateLimiter.updateConfig(originalConfig)
        SecurityRateLimiter.resetAllLimits()
        context.deleteDatabase("persistence_test.db")
    }

    // ------------------------------------------------------------------
    // 1. Brute-force lockout policy
    // ------------------------------------------------------------------

    @Test
    fun `first three wrong passwords stay unlocked`() {
        repeat(3) { index ->
            val message = SecurityRateLimiter.recordAuthFailure(client, "viola@example.com")
            assertNull("Failure #${index + 1} must not lock the account yet", message)
        }
        assertTrue(
            "Account must still be inside the allowed window",
            SecurityRateLimiter.checkAuthLockout(client, "viola@example.com") is RateLimitResult.Allowed
        )
    }

    @Test
    fun `fourth wrong password locks account with Error 422 for five minutes`() {
        repeat(3) { SecurityRateLimiter.recordAuthFailure(client, "viola@example.com") }
        val lockMessage = SecurityRateLimiter.recordAuthFailure(client, "viola@example.com")

        assertNotNull("4th wrong password must trigger the lockout", lockMessage)
        assertTrue("Lock message must expose the app's Error 422 code", lockMessage!!.contains("Error 422"))
        assertTrue("Lock message must announce the 5-minute cooldown", lockMessage.contains("5 minutes"))

        val probe = SecurityRateLimiter.checkAuthLockout(client, "viola@example.com")
        assertTrue("Account must be blocked during the cooldown", probe is RateLimitResult.Blocked)
        val blocked = probe as RateLimitResult.Blocked
        assertTrue("Cooldown must be roughly 5 minutes", blocked.remainingCooldownSeconds in 290..300)
        assertTrue("Blocked reason must carry Error 422", blocked.reason.contains("Error 422"))
    }

    @Test
    fun `cooldown expiry automatically re-allows login`() {
        SecurityRateLimiter.updateConfig(originalConfig.copy(authLockoutMs = 1500L)) // fast-forward surrogate
        repeat(4) { SecurityRateLimiter.recordAuthFailure(client, "expiry@example.com") }
        assertTrue(
            "Account is locked immediately after the 4th failure",
            SecurityRateLimiter.checkAuthLockout(client, "expiry@example.com") is RateLimitResult.Blocked
        )

        // Represent the cooldown truly elapsing: wait for the real wall clock AND
        // advance Robolectric's simulated elapsedRealtime (both are used by the limiter).
        Thread.sleep(1800L)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(2))

        assertTrue(
            "After the cooldown elapses login must be allowed again with no manual reset",
            SecurityRateLimiter.checkAuthLockout(client, "expiry@example.com") is RateLimitResult.Allowed
        )
    }

    @Test
    fun `app restart cannot bypass the cooldown`() {
        repeat(4) { SecurityRateLimiter.recordAuthFailure(client, "restart@example.com") }
        assertTrue(SecurityRateLimiter.checkAuthLockout(client, "restart@example.com") is RateLimitResult.Blocked)

        // Simulate a fresh process: drop in-memory state and reload from persisted storage.
        SecurityRateLimiter.initialize(context, forceReload = true)

        assertTrue(
            "Persisted lockout must survive an app restart",
            SecurityRateLimiter.checkAuthLockout(client, "restart@example.com") is RateLimitResult.Blocked
        )
    }

    @Test
    fun `parallel failed attempts are counted exactly once each and cannot slip past the fourth`() {
        val account = "race@example.com"
        // Exactly 4 parallel attempts: without atomic accounting a lost update would leave
        // the account unlocked, so this fails if the per-account mutex is removed.
        val threads = 4
        val startGate = java.util.concurrent.CountDownLatch(1)
        val done = java.util.concurrent.CountDownLatch(threads)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(threads)

        repeat(threads) {
            executor.execute {
                try {
                    startGate.await()
                    SecurityRateLimiter.recordAuthFailure(client, account)
                } finally {
                    done.countDown()
                }
            }
        }
        startGate.countDown() // fire all attempts at the same instant
        assertTrue("All parallel attempts must finish", done.await(15, java.util.concurrent.TimeUnit.SECONDS))
        executor.shutdown()

        assertTrue(
            "Concurrent attempts must still trigger the cooldown at the 4th failure",
            SecurityRateLimiter.checkAuthLockout(client, account) is RateLimitResult.Blocked
        )
        // Clean up so this account's lockout never leaks into another test.
        SecurityRateLimiter.recordAuthSuccess(client, account)
    }

    @Test
    fun `successful login clears the failure counters and lockout`() {
        repeat(4) { SecurityRateLimiter.recordAuthFailure(client, "reset@example.com") }
        assertTrue(SecurityRateLimiter.checkAuthLockout(client, "reset@example.com") is RateLimitResult.Blocked)

        SecurityRateLimiter.recordAuthSuccess(client, "reset@example.com")

        assertTrue(
            "Successful authentication must clear the lockout",
            SecurityRateLimiter.checkAuthLockout(client, "reset@example.com") is RateLimitResult.Allowed
        )
    }

    @Test
    fun `persisted lockout keys never contain the plain email address`() {
        repeat(4) { SecurityRateLimiter.recordAuthFailure(client, "privacy@example.com") }
        val stored = context.getSharedPreferences("zyphuel_auth_guard", Context.MODE_PRIVATE).all

        assertTrue("A persisted lockout entry must exist", stored.keys.any { it.startsWith("lockout_v1_") })
        assertFalse(
            "Persisted rate-limit keys must be hashed, never plain email/client identifiers",
            stored.keys.any { it.contains("privacy@example.com") || it.contains("unit_test_client") }
        )
    }

    // ------------------------------------------------------------------
    // 2. SQL injection / credential bypass resistance
    // ------------------------------------------------------------------

    @Test
    fun `SQL injection payloads are rejected by email validation`() {
        val payloads = listOf(
            "' OR '1'='1",
            "admin'--@example.com",
            "a@b.com' UNION SELECT * FROM users--",
            "\" OR \"\"=\"",
            "test@example.com' OR 1=1 --"
        )
        payloads.forEach { payload ->
            assertFalse(
                "SQL injection payload must fail email validation: $payload",
                SecurityInputValidator.validateEmail(payload).isValid
            )
        }
    }

    @Test
    fun `password hashes use PBKDF2 and verify only the correct secret`() {
        val hash = SecurityCrypto.hashPassword("Zyphuel#2026Secure")

        assertTrue("New hashes must use the PBKDF2 format", hash.startsWith("pbkdf2_sha256$"))
        assertNotEquals("Hash must never equal the raw password", "Zyphuel#2026Secure", hash)
        assertTrue(SecurityCrypto.verifyPassword("Zyphuel#2026Secure", hash))
        assertFalse(SecurityCrypto.verifyPassword("Zyphuel#2026Securee", hash))
        assertFalse("PBKDF2 hashes need no upgrade", SecurityCrypto.needsRehash(hash))

        // Two hashes of the same password must differ (random per-user salt).
        assertNotEquals(hash, SecurityCrypto.hashPassword("Zyphuel#2026Secure"))
    }

    @Test
    fun `social OAuth placeholder can never authenticate through the password form`() {
        val socialHash = "SOCIAL_OAUTH_GOOGLE_${System.currentTimeMillis()}"

        assertFalse(
            "Google-only accounts must not be loginable with email + a guessed OAuth placeholder",
            SecurityCrypto.verifyPassword(socialHash, socialHash)
        )
        assertFalse("OAuth accounts must not be re-hashed", SecurityCrypto.needsRehash(socialHash))
    }

    @Test
    fun `a stored hash can never be matched by raw input`() {
        val storedHashLike = "9c16c5decc45c872ee18e857d659bd6786c3f3e5de7220cb07935048a22b9e7f"
        assertFalse(
            "Submitting the stored hash itself must not authenticate (no plaintext fallback for hash-like values)",
            SecurityCrypto.verifyPassword(storedHashLike, storedHashLike)
        )
        assertTrue("A hash-like legacy value must be scheduled for upgrade", SecurityCrypto.needsRehash(storedHashLike))
    }

    @Test
    fun `SQL injection payload cannot pass password verification`() {
        val realPassword = "Zyphuel#2026Secure"
        val storedHash = SecurityCrypto.hashPassword(realPassword)

        assertNotEquals("Password must never be stored in plaintext", realPassword, storedHash)
        assertTrue("Correct password must verify", SecurityCrypto.verifyPassword(realPassword, storedHash))
        assertFalse(
            "Injection payload must not authenticate",
            SecurityCrypto.verifyPassword("' OR '1'='1' --", storedHash)
        )
        assertFalse(
            "Empty password must not authenticate",
            SecurityCrypto.verifyPassword("", storedHash)
        )
    }

    // ------------------------------------------------------------------
    // 3. Duplicate email prevention (case-insensitive)
    // ------------------------------------------------------------------

    @Test
    fun `email lookup is case-insensitive so duplicate signups are blocked`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            // registerUser() normalizes to trimmed lowercase before inserting.
            db.userDao().insertNewUser(
                UserEntity(
                    email = "ali@example.com",
                    name = "Ali",
                    passwordHash = "hash",
                    role = "customer",
                    phoneNumber = "+923001234567"
                )
            )

            val existing = db.userDao().getUserByEmail("ALI@Example.com")
            assertNotNull(
                "Case-varied lookup must find the account so a duplicate signup is blocked",
                existing
            )
            assertEquals("ali@example.com", existing!!.email)

            // Registration inserts use ABORT (not REPLACE), so a duplicate address is
            // rejected by the database instead of silently overwriting the original user.
            assertThrows(SQLiteConstraintException::class.java) {
                runBlocking {
                    db.userDao().insertNewUser(
                        UserEntity(
                            email = "ali@example.com",
                            name = "Impostor",
                            passwordHash = "hash2",
                            role = "customer",
                            phoneNumber = "+923009999999"
                        )
                    )
                }
            }

            // The original account must be fully intact after the rejected attempt.
            val survivor = db.userDao().getUserByEmail("ali@example.com")
            assertNotNull(survivor)
            assertEquals("Ali", survivor!!.name)
            assertEquals("hash", survivor.passwordHash)

            // Profile updates still work through the REPLACE upsert used by the app.
            db.userDao().insertUser(survivor.copy(name = "Ali Updated"))
            assertEquals("Ali Updated", db.userDao().getUserByEmail("ali@example.com")!!.name)
        } finally {
            db.close()
        }
    }

    // ------------------------------------------------------------------
    // 4. Local user data isolation (ownership)
    // ------------------------------------------------------------------

    @Test
    fun `customer order flow only returns that customer's own orders`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            fun order(customer: String, id: Int) = OrderEntity(
                id = id,
                customerEmail = customer,
                customerName = customer.substringBefore("@"),
                customerPhone = "+923001234567",
                serviceType = "Petrol",
                quantity = 5,
                totalPrice = 2500.0,
                deliveryAddress = "Gulberg III, Lahore",
                status = "Pending"
            )

            db.orderDao().insertOrder(order("alice@example.com", 1))
            db.orderDao().insertOrder(order("bob@example.com", 2))

            val aliceOrders = db.orderDao().getOrdersForCustomerFlow("alice@example.com").first()
            assertEquals("Alice must see exactly one order", 1, aliceOrders.size)
            assertTrue(aliceOrders.all { it.customerEmail == "alice@example.com" })

            val bobOrders = db.orderDao().getOrdersForCustomerFlow("ALICE@example.com").first()
            assertTrue(
                "Case-varied lookups must still be scoped to the requesting customer only",
                bobOrders.all { it.customerEmail.equals("alice@example.com", ignoreCase = true) }
            )
        } finally {
            db.close()
        }
    }

    // ------------------------------------------------------------------
    // 5. Data preservation across app update (DB reopen + migration)
    // ------------------------------------------------------------------

    @Test
    fun `migration object bridges version 12 to version 13`() {
        assertEquals("Migration must start at schema version 12", 12, MIGRATION_12_TO_13.startVersion)
        assertEquals("Migration must end at schema version 13", 13, MIGRATION_12_TO_13.endVersion)
    }

    @Test
    fun `user and order records survive a database close and reopen`() = runBlocking {
        val dbName = "persistence_test.db"
        context.deleteDatabase(dbName)

        // First app run: create data.
        val firstRun = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_12_TO_13)
            .allowMainThreadQueries()
            .build()
        firstRun.userDao().insertUser(
            UserEntity(
                email = "persist@example.com",
                name = "Persist User",
                passwordHash = "hash",
                role = "customer",
                phoneNumber = "+923001112223"
            )
        )
        firstRun.orderDao().insertOrder(
            OrderEntity(
                id = 0,
                customerEmail = "persist@example.com",
                customerName = "Persist User",
                customerPhone = "+923001112223",
                serviceType = "Diesel",
                quantity = 10,
                totalPrice = 3500.0,
                deliveryAddress = "DHA Phase 5, Lahore",
                status = "Completed"
            )
        )
        firstRun.close()

        // Second app run (simulated update): data must still be there.
        val secondRun = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_12_TO_13)
            .allowMainThreadQueries()
            .build()
        try {
            val user = secondRun.userDao().getUserByEmail("persist@example.com")
            assertNotNull("Existing user must survive the update", user)
            assertEquals("Persist User", user!!.name)

            val orders = secondRun.orderDao().getOrdersForCustomerFlow("persist@example.com").first()
            assertEquals("Existing order history must survive the update", 1, orders.size)
            assertEquals("Completed", orders.first().status)
        } finally {
            secondRun.close()
        }
    }
}
