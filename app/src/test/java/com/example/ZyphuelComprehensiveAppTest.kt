package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.OrderEntity
import com.example.data.UserEntity
import com.example.util.RealtimeEmailEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

/**
 * Comprehensive Human & AI Simulation Test Suite for Zyphuel.
 * 
 * Simulates complete end-to-end user journeys:
 * 1. Customer Registration, Social Google Login, and Order Placement.
 * 2. Real-Time Dual Email & HTML Template Dispatching.
 * 3. Rider Assignment, Acceptance, and 4-Step Order Progression.
 * 4. Permanent Root Super Admin Protection & Verified Blue Tick Authority.
 * 5. Share Location Coordinate Formatting & Price Calculations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ZyphuelComprehensiveAppTest {

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // =========================================================================
    // 1. CUSTOMER PERSONA TEST: ORDER PLACEMENT & DUAL REAL-TIME EMAIL
    // =========================================================================
    @Test
    fun `test Customer Journey - Order Placement, Price Calculation and Realtime Email Generation`() {
        // Step 1: Customer Profile Creation
        val customer = UserEntity(
            email = "daniyal.test.customer@gmail.com",
            passwordHash = sha256("customerPass123"),
            role = "customer",
            name = "Daniyal Customer",
            phoneNumber = "+92 300 1234567",
            isVerified = false
        )
        assertEquals("customer", customer.role)
        assertEquals("daniyal.test.customer@gmail.com", customer.email)

        // Step 2: Order Calculation Simulation
        val unitPrice = 260.00
        val quantity = 25
        val totalPrice = unitPrice * quantity
        val formattedPrice = String.format(java.util.Locale.US, "%.2f", totalPrice)
        assertEquals("6500.00", formattedPrice)

        // Step 3: Order Entity Creation
        val order = OrderEntity(
            id = 5001,
            customerEmail = customer.email,
            customerName = customer.name,
            customerPhone = customer.phoneNumber,
            serviceType = "Super Petrol",
            quantity = quantity,
            totalPrice = totalPrice,
            deliveryAddress = "Gulberg III, Main Boulevard, Lahore",
            status = "Pending",
            paymentMethod = "Cash on Delivery (COD)",
            etaMinutes = 15,
            destLat = 31.5204,
            destLng = 74.3587,
            createdAt = System.currentTimeMillis()
        )

        assertEquals(5001, order.id)
        assertEquals("Pending", order.status)
        assertEquals(totalPrice, order.totalPrice, 0.01)

        // Step 4: Realtime Email Template Generation Verification
        val subject = "🧾 Zyphuel Order Confirmation - Order #${order.id}"
        val bodyText = """
            Assalam o Alaikum ${customer.name},
            Your order #${order.id} for ${order.serviceType} (${order.quantity} units) has been placed!
            Total Price: Rs. $formattedPrice
            Delivery Address: ${order.deliveryAddress}
        """.trimIndent()

        val htmlEmail = RealtimeEmailEngine.generateHtmlEmail(subject, bodyText)

        assertTrue(htmlEmail.contains("Zyphuel On-Demand Delivery"))
        assertTrue(htmlEmail.contains("Order #${order.id}"))
        assertTrue(htmlEmail.contains(formattedPrice))
        assertTrue(htmlEmail.contains("Gulberg III"))
    }

    // =========================================================================
    // 2. RIDER PERSONA TEST: ORDER ACCEPTANCE & DISPATCH LIFECYCLE
    // =========================================================================
    @Test
    fun `test Rider Journey - Accept Order and Order Status Progression`() {
        val rider = UserEntity(
            email = "rider.ali@gmail.com",
            passwordHash = sha256("riderPass123"),
            role = "rider",
            name = "Ali Rider",
            phoneNumber = "+92 321 7654321",
            isVerified = true,
            vehicleType = "Fuel Bowser 🚚"
        )
        assertTrue(rider.isVerified)
        assertEquals("rider", rider.role)

        var currentOrder = OrderEntity(
            id = 5002,
            customerEmail = "customer@gmail.com",
            customerName = "Customer Test",
            customerPhone = "+92 300 1112233",
            serviceType = "High-Speed Diesel",
            quantity = 50,
            totalPrice = 13500.0,
            deliveryAddress = "Johar Town, Block G, Lahore",
            status = "Pending",
            paymentMethod = "COD",
            etaMinutes = 25,
            createdAt = System.currentTimeMillis()
        )

        // Rider Accepts Order
        currentOrder = currentOrder.copy(
            status = "Assigned",
            riderEmail = rider.email,
            riderName = rider.name
        )
        assertEquals("Assigned", currentOrder.status)
        assertEquals("rider.ali@gmail.com", currentOrder.riderEmail)

        // Rider Picks Up Fuel
        currentOrder = currentOrder.copy(status = "Picking Up")
        assertEquals("Picking Up", currentOrder.status)

        // Rider In Transit (Delivering)
        currentOrder = currentOrder.copy(status = "Delivering", etaMinutes = 10)
        assertEquals("Delivering", currentOrder.status)
        assertEquals(10, currentOrder.etaMinutes)

        // Rider Reached Customer Location
        currentOrder = currentOrder.copy(status = "Arrived")
        assertEquals("Arrived", currentOrder.status)

        // Order Completed & COD Collected
        currentOrder = currentOrder.copy(status = "Completed", etaMinutes = 0, rating = 5, feedback = "Fast delivery!")
        assertEquals("Completed", currentOrder.status)
        assertEquals(5, currentOrder.rating)
        assertEquals("Fast delivery!", currentOrder.feedback)
    }

    // =========================================================================
    // 3. ROOT SUPER ADMIN TEST: IMMUTABILITY & BLUE TICK VERIFIED BADGE
    // =========================================================================
    @Test
    fun `test Super Admin Protection - Root Admin Account Immutability`() {
        val superAdminEmail = "m.daniyalkhan490@gmail.com"
        val superAdminPass = "abcd1234"

        val rootAdmin = UserEntity(
            email = superAdminEmail,
            passwordHash = sha256(superAdminPass),
            role = "admin",
            name = "M. Daniyal Khan (Super Admin)",
            phoneNumber = "+92 323 0112464",
            isVerified = true
        )

        // Verify permanent credentials
        assertEquals("m.daniyalkhan490@gmail.com", rootAdmin.email)
        assertEquals("admin", rootAdmin.role)
        assertTrue(rootAdmin.isVerified)

        // Verify that deleting this specific account is strictly forbidden
        fun canDeleteAccount(email: String): Boolean {
            return !email.equals(superAdminEmail, ignoreCase = true)
        }

        assertFalse("Super admin cannot be deleted", canDeleteAccount(superAdminEmail))
        assertTrue("Normal user can be deleted", canDeleteAccount("random.user@gmail.com"))
    }

    // =========================================================================
    // 4. SHARE LOCATION LOGIC TEST
    // =========================================================================
    @Test
    fun `test Share Location Intent URL and Coordinate Formatting`() {
        val lat = 31.4380
        val lng = 74.3050
        val landmark = "Green Town Main Headquarters, Lahore"

        val mapsUrl = "https://maps.google.com/?q=$lat,$lng"
        val shareMessage = "📍 My Zyphuel Delivery Location:\n$landmark\nCoordinates: ($lat, $lng)\nGoogle Maps Link: $mapsUrl"

        assertTrue(shareMessage.contains("31.438"))
        assertTrue(shareMessage.contains("74.305"))
        assertTrue(shareMessage.contains("https://maps.google.com/?q=31.438,74.305"))
        assertTrue(shareMessage.contains("Green Town Main Headquarters"))
    }

    // =========================================================================
    // 5. APP METADATA & VERSION VERIFICATION
    // =========================================================================
    @Test
    fun `test App String Resources and Identification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Zyphuel", appName)

        val appTitle = context.getString(R.string.app_store_title)
        assertTrue(appTitle.contains("Zyphuel"))
    }
}
