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
    fun `test petrol pump rate constant is exactly Rs 2 point 50`() {
        assertEquals(2.50, FeeConstants.PETROL_PUMP_RATE_SURCHARGE, 0.001)
        assertEquals(2.50f, FeeConstants.PETROL_PUMP_RATE_SURCHARGE_FLOAT, 0.001f)
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
    fun `test getPumpRate adds exactly Rs 2 point 50 to base fuel rate`() {
        val basePetrol = 289.38
        val expectedPumpPetrol = 291.88
        val actualPumpPetrol = FeeConstants.getPumpRate(basePetrol, "Petrol")
        assertEquals(expectedPumpPetrol, actualPumpPetrol, 0.001)

        val baseDiesel = 289.84
        val expectedPumpDiesel = 292.34
        val actualPumpDiesel = FeeConstants.getPumpRate(baseDiesel, "Diesel")
        assertEquals(expectedPumpDiesel, actualPumpDiesel, 0.001)

        val baseOctane = 325.00
        val expectedPumpOctane = 327.50
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
    fun `test CategoryRepository syncLiveFuelPrices adds Rs 2 point 50 to petrol, diesel, and octane subcategories`() {
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
        assertEquals(289.38 + 2.50, petrolSub.basePrice, 0.001)

        val octaneSub = fuelCategory.subcategories.first { it.id == "petrol_octane" }
        assertEquals(325.00 + 2.50, octaneSub.basePrice, 0.001)

        val dieselRegularSub = fuelCategory.subcategories.first { it.id == "diesel_regular" }
        assertEquals(289.84 + 2.50, dieselRegularSub.basePrice, 0.001)

        val dieselGenSub = fuelCategory.subcategories.first { it.id == "diesel_generator" }
        assertEquals(289.84 + 2.50, dieselGenSub.basePrice, 0.001)

        // LPG must not have pump surcharge added
        val lpgSub = fuelCategory.subcategories.first { it.id == "lpg_sealed_cylinder" }
        assertEquals(258.65, lpgSub.basePrice, 0.001)
    }

    @Test
    fun `test simulated OrderDialog grand total calculation with petrol pump rate`() {
        val basePetrol = 289.38
        val pumpPetrol = FeeConstants.getPumpRate(basePetrol, "Petrol") // 291.88
        val quantity = 10 // 10 liters
        val deliveryFee = FeeConstants.FUEL_DELIVERY_FEE // 250.0

        val itemSubtotal = quantity * pumpPetrol // 2918.80
        val grandTotal = itemSubtotal + deliveryFee // 3168.80

        assertEquals(291.88, pumpPetrol, 0.001)
        assertEquals(2918.80, itemSubtotal, 0.01)
        assertEquals(3168.80, grandTotal, 0.01)
    }
}
