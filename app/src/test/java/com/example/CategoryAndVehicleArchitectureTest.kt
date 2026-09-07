package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.category.CategoryCatalogSeed
import com.example.data.category.CategoryRepository
import com.example.data.category.VehicleType
import com.example.data.vehicle.VehicleEntity
import com.example.util.CoverageStatus
import com.example.util.LocationCoverageManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryAndVehicleArchitectureTest {

    private lateinit var context: Context
    private lateinit var categoryRepository: CategoryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        categoryRepository = CategoryRepository(context)
    }

    @Test
    fun `test CategoryCatalogSeed has exactly 10 production categories`() {
        val seed = CategoryCatalogSeed.getDefaultCategories()
        assertEquals(10, seed.size)

        val expectedIds = setOf(
            "fuel_energy",
            "auto_repair",
            "roadside_assistance",
            "auto_detailing",
            "tyres_wheels",
            "battery_services",
            "lubricants_fluids",
            "ev_services",
            "water_delivery",
            "fleet_business"
        )
        val actualIds = seed.map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }

    @Test
    fun `test all subcategories have valid configurations and positive pricing`() {
        val seed = CategoryCatalogSeed.getDefaultCategories()
        for (category in seed) {
            assertTrue("Category ${category.id} must have subcategories", category.subcategories.isNotEmpty())
            assertTrue("Category ${category.id} must have a name", category.name.isNotBlank())
            assertTrue("Category ${category.id} must have a valid icon", category.iconName.isNotBlank())

            for (sub in category.subcategories) {
                assertTrue("Subcategory ${sub.id} must have a non-blank name", sub.name.isNotBlank())
                assertTrue("Subcategory ${sub.id} must have pricing >= 0", sub.basePrice >= 0.0)
                assertTrue("Subcategory ${sub.id} supportedVehicleTypes must not be empty", sub.supportedVehicleTypes.isNotEmpty())
                assertTrue("Subcategory ${sub.id} estimatedDuration must be non-blank", sub.estimatedDuration.isNotBlank())
            }
        }
    }

    @Test
    fun `test CategoryRepository search finds exact and partial matches`() {
        val petrolResults = categoryRepository.searchServices("petrol")
        assertTrue("Expected search results for 'petrol'", petrolResults.isNotEmpty())
        assertTrue(petrolResults.any { it.subcategory.name.contains("Petrol", ignoreCase = true) })

        val batteryResults = categoryRepository.searchServices("battery")
        assertTrue("Expected search results for 'battery'", batteryResults.isNotEmpty())

        val oilResults = categoryRepository.searchServices("oil")
        assertTrue("Expected search results for 'oil'", oilResults.isNotEmpty())

        val tyreResults = categoryRepository.searchServices("puncture")
        assertTrue("Expected search results for 'puncture'", tyreResults.isNotEmpty())
    }

    @Test
    fun `test CategoryRepository fuzzy search matches typos`() {
        // "puncure" typo for "puncture"
        val typoPuncture = categoryRepository.searchServices("puncure")
        assertTrue("Fuzzy search should match 'puncure' to tyre/puncture services", typoPuncture.isNotEmpty())

        // "deisel" typo for "diesel"
        val typoDiesel = categoryRepository.searchServices("deisel")
        assertTrue("Fuzzy search should match 'deisel' to diesel service", typoDiesel.isNotEmpty())
    }

    @Test
    fun `test LocationCoverageManager detects Lahore coverage zones and computes realistic ETA`() {
        // Lahore Gulberg coordinates (close to depot 31.4380, 74.3050)
        val gulbergLat = 31.5204
        val gulbergLng = 74.3587

        val coverage = LocationCoverageManager.checkCoverage(gulbergLat, gulbergLng)
        assertTrue("Gulberg Lahore must be covered", coverage is CoverageStatus.Covered)
        val covered = coverage as CoverageStatus.Covered
        assertTrue("Distance should be within 25 km", covered.distanceKm in 0.1..25.0)
        assertTrue("ETA should be between 10 and 60 minutes", covered.etaMinutes in 10..60)
        assertNotNull("Should detect a zone name", covered.zoneName)
    }

    @Test
    fun `test LocationCoverageManager flags out of coverage locations`() {
        // Karachi coordinate (approx 1000 km away)
        val karachiLat = 24.8607
        val karachiLng = 67.0011

        val coverage = LocationCoverageManager.checkCoverage(karachiLat, karachiLng)
        assertTrue("Karachi should be outside coverage", coverage is CoverageStatus.OutOfZone)
        val outOfZone = coverage as CoverageStatus.OutOfZone
        assertTrue("Distance should be > 500 km", outOfZone.distanceKm > 500.0)
        assertTrue("Message should explain out of zone", outOfZone.message.contains("expanding", ignoreCase = true))
    }

    @Test
    fun `test VehicleEntity model validation and types`() {
        val vehicle = VehicleEntity(
            id = 101,
            userEmail = "daniyal.test.customer@gmail.com",
            nickname = "My Daily Ride",
            make = "Toyota",
            model = "Corolla",
            year = 2022,
            registrationNumber = "LEA-2022",
            fuelType = "Petrol",
            vehicleType = VehicleType.CAR.name,
            isDefault = true
        )

        assertEquals("daniyal.test.customer@gmail.com", vehicle.userEmail)
        assertEquals("Toyota", vehicle.make)
        assertEquals("Corolla", vehicle.model)
        assertEquals("LEA-2022", vehicle.registrationNumber)
        assertTrue(vehicle.isDefault)
        assertEquals(VehicleType.CAR.name, vehicle.vehicleType)
    }
}
