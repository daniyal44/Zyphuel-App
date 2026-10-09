package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.category.CategoryCatalogSeed
import com.example.data.category.CategoryRepository
import com.example.util.FeeConstants
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FuelPumpPricingTest {

    private lateinit var context: Context
    private lateinit var categoryRepository: CategoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        categoryRepository = CategoryRepository(context)
    }

    @Test
    fun `test petrol pump rate constant is exactly Rs 5 point 00`() {
        assertEquals(5.00, FeeConstants.PUMP_RATE_MARKUP, 0.001)
        assertEquals(5.00f, FeeConstants.PUMP_RATE_MARKUP_FLOAT, 0.001f)
        assertEquals(5.00, FeeConstants.PETROL_PUMP_RATE_SURCHARGE, 0.001)
        assertEquals(5.00f, FeeConstants.PETROL_PUMP_RATE_SURCHARGE_FLOAT, 0.001f)
    }

    @Test
    fun `test isPumpRateApplicable correctly identifies petrol, diesel, and high-octane`() {
        // Must be applicable to petrol, diesel, high-octane
        assertTrue(FeeConstants.isPumpRateApplicable("Petrol"))
        assertTrue(FeeConstants.isPumpRateApplicable("Super Petrol"))
        assertTrue(FeeConstants.isPumpRateApplicable("petrol_regular"))
        assertTrue(FeeConstants.isPumpRateApplicable("petrol_octane"))
        assertTrue(FeeConstants.isPumpRateApplicable("Diesel"))
        assertTrue(FeeConstants.isPumpRateApplicable("High-Speed Diesel"))
        assertTrue(FeeConstants.isPumpRateApplicable("Generator Diesel"))
        assertTrue(FeeConstants.isPumpRateApplicable("diesel_regular"))
        assertTrue(FeeConstants.isPumpRateApplicable("High-Octane"))
        assertTrue(FeeConstants.isPumpRateApplicable("High-Octane Petrol"))
        assertTrue(FeeConstants.isPumpRateApplicable("HOBC 97"))

        // Must NOT be applicable to LPG, Gas, or Water
        assertFalse(FeeConstants.isPumpRateApplicable("LPG Gas"))
        assertFalse(FeeConstants.isPumpRateApplicable("Gas Cylinder"))
        assertFalse(FeeConstants.isPumpRateApplicable("lpg_sealed_cylinder"))
        assertFalse(FeeConstants.isPumpRateApplicable("Water"))
        assertFalse(FeeConstants.isPumpRateApplicable("Pure Drinking Water"))
        assertFalse(FeeConstants.isPumpRateApplicable("water_drinking"))
        assertFalse(FeeConstants.isPumpRateApplicable(null))
        assertFalse(FeeConstants.isPumpRateApplicable(""))
    }

    @Test
    fun `test getPumpRate adds exactly Rs 5 point 00 to base fuel rate`() {
        val basePetrol = 289.38
        val expectedPumpPetrol = 294.38
        val actualPumpPetrol = FeeConstants.getPumpRate(basePetrol, "Petrol")
        assertEquals(expectedPumpPetrol, actualPumpPetrol, 0.001)

        val baseDiesel = 289.84
        val expectedPumpDiesel = 294.84
        val actualPumpDiesel = FeeConstants.getPumpRate(baseDiesel, "Diesel")
        assertEquals(expectedPumpDiesel, actualPumpDiesel, 0.001)

        val baseOctane = 325.00
        val expectedPumpOctane = 330.00
        val actualPumpOctane = FeeConstants.getPumpRate(baseOctane, "High-Octane")
        assertEquals(expectedPumpOctane, actualPumpOctane, 0.001)

        // Ensure LPG and Water are unaffected
        val baseLpg = 258.65
        val actualLpg = FeeConstants.getPumpRate(baseLpg, "LPG Gas")
        assertEquals(baseLpg, actualLpg, 0.001)

        val baseWater = 50.0
        val actualWater = FeeConstants.getPumpRate(baseWater, "Water")
        assertEquals(baseWater, actualWater, 0.001)
    }

    @Test
    fun `test CategoryRepository syncLiveFuelPrices adds Rs 5 point 00 to petrol, diesel, and octane subcategories`() {
        val testBasePetrol = 289.38f
        val testBaseDiesel = 289.84f
        val testBaseOctane = 325.00f
        val testBaseLpg = 258.65f
        val testBaseWater = 50.0f

        categoryRepository.syncLiveFuelPrices(
            petrol = testBasePetrol,
            diesel = testBaseDiesel,
            octane = testBaseOctane,
            lpg = testBaseLpg,
            water = testBaseWater
        )

        val categories = categoryRepository.categories.value
        val fuelCategory = categories.first { it.id == "fuel_energy" }

        val petrolSub = fuelCategory.subcategories.first { it.id == "petrol_regular" }
        assertEquals(289.38 + 5.00, petrolSub.basePrice, 0.001)

        val octaneSub = fuelCategory.subcategories.first { it.id == "petrol_octane" }
        assertEquals(325.00 + 5.00, octaneSub.basePrice, 0.001)

        val dieselRegularSub = fuelCategory.subcategories.first { it.id == "diesel_regular" }
        assertEquals(289.84 + 5.00, dieselRegularSub.basePrice, 0.001)

        val dieselGenSub = fuelCategory.subcategories.first { it.id == "diesel_generator" }
        assertEquals(289.84 + 5.00, dieselGenSub.basePrice, 0.001)

        // LPG is permanently removed from subcategories
        assertTrue(fuelCategory.subcategories.none { it.id == "lpg_sealed_cylinder" })
    }

    @Test
    fun `test simulated OrderDialog grand total calculation with petrol pump rate`() {
        val basePetrol = 289.38
        val pumpPetrol = FeeConstants.getPumpRate(basePetrol, "Petrol") // 294.38
        val quantity = 10 // 10 liters
        val deliveryFee = FeeConstants.calculateFuelDeliveryFee(quantity) // 300.0 for 10L

        val itemSubtotal = quantity * pumpPetrol // 2943.80
        val grandTotal = itemSubtotal + deliveryFee // 3243.80

        assertEquals(294.38, pumpPetrol, 0.001)
        assertEquals(2943.80, itemSubtotal, 0.01)
        assertEquals(3243.80, grandTotal, 0.01)
    }

    @Test
    fun `test tiered fuel delivery rates and service availability`() {
        // Per-liter delivery rates
        assertEquals(300.0, FeeConstants.calculateFuelDeliveryFee(1), 0.001)
        assertEquals(300.0, FeeConstants.calculateFuelDeliveryFee(5), 0.001)
        assertEquals(300.0, FeeConstants.calculateFuelDeliveryFee(7), 0.001)
        assertEquals(300.0, FeeConstants.calculateFuelDeliveryFee(10), 0.001)
        assertEquals(320.0, FeeConstants.calculateFuelDeliveryFee(11), 0.001)
        assertEquals(340.0, FeeConstants.calculateFuelDeliveryFee(12), 0.001)
        assertEquals(360.0, FeeConstants.calculateFuelDeliveryFee(13), 0.001)
        assertEquals(380.0, FeeConstants.calculateFuelDeliveryFee(14), 0.001)
        assertEquals(400.0, FeeConstants.calculateFuelDeliveryFee(15), 0.001)

        // Max volume limit
        assertEquals(15, FeeConstants.FUEL_MAX_LITERS)

        // Water and Gas unavailability
        assertTrue(FeeConstants.isServiceUnavailable("Water"))
        assertTrue(FeeConstants.isServiceUnavailable("Pure Drinking Water"))
        assertTrue(FeeConstants.isServiceUnavailable("LPG Gas"))
        assertTrue(FeeConstants.isServiceUnavailable("Gas Cylinder"))
        assertFalse(FeeConstants.isServiceUnavailable("Petrol"))
        assertFalse(FeeConstants.isServiceUnavailable("Diesel"))
        assertFalse(FeeConstants.isServiceUnavailable("High-Octane"))
    }

    @Test
    fun `test DeliveryOperatingHoursManager schedule windows`() {
        val tz = com.example.util.DeliveryOperatingHoursManager.PAKISTAN_TIMEZONE

        // 1. Monday to Thursday (08:00 AM to 08:00 PM)
        val monMorningBefore = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 7)
            set(java.util.Calendar.MINUTE, 59)
        }
        assertFalse(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(monMorningBefore))

        val monOpen = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
        }
        assertTrue(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(monOpen))

        val thuEveningJustBefore = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.THURSDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 19)
            set(java.util.Calendar.MINUTE, 59)
        }
        assertTrue(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(thuEveningJustBefore))

        val thuEveningClosed = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.THURSDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 20)
            set(java.util.Calendar.MINUTE, 0)
        }
        assertFalse(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(thuEveningClosed))

        // 2. Friday (08:00 AM to 01:00 PM)
        val friMorningOpen = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.FRIDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
        }
        assertTrue(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(friMorningOpen))

        val friJustBeforeClose = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.FRIDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 59)
        }
        assertTrue(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(friJustBeforeClose))

        val friClosedAt1Pm = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.FRIDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 13)
            set(java.util.Calendar.MINUTE, 0)
        }
        assertFalse(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(friClosedAt1Pm))

        // 3. Saturday and Sunday (10:00 AM to 06:00 PM)
        val satBeforeOpen = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SATURDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 59)
        }
        assertFalse(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(satBeforeOpen))

        val satOpen = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SATURDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 10)
            set(java.util.Calendar.MINUTE, 0)
        }
        assertTrue(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(satOpen))

        val sunEveningOpen = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 17)
            set(java.util.Calendar.MINUTE, 59)
        }
        assertTrue(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(sunEveningOpen))

        val sunEveningClosed = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 18)
            set(java.util.Calendar.MINUTE, 0)
        }
        assertFalse(com.example.util.DeliveryOperatingHoursManager.isDeliveryOpen(sunEveningClosed))
    }
}
