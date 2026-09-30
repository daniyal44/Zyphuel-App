package com.example.util

/**
 * Standardized delivery fee constants and tiered volume calculation utility.
 * Guarantees 100% consistency across OrderDialog, TrackerScreen, CustomerOrderCard,
 * FareBreakdownDialog, and official invoices.
 *
 * Tiered Delivery Rates for Petrol, Diesel, and High-Octane:
 * - Up to 5 Liters:  Rs. 280.00
 * - Up to 10 Liters: Rs. 300.00
 * - Up to 15 Liters (Max Limit): Rs. 350.00
 *
 * Note: Pure Water and LPG Gas are currently UNAVAILABLE across all sectors.
 */
object FeeConstants {
    const val FUEL_MAX_LITERS = 15
    const val FUEL_DELIVERY_FEE_5L = 280.0
    const val FUEL_DELIVERY_FEE_10L = 300.0
    const val FUEL_DELIVERY_FEE_15L = 350.0

    // Backward compatibility base fee
    const val FUEL_DELIVERY_FEE = 280.0
    const val WATER_DELIVERY_FEE = 50.0

    /**
     * Petrol pump retail rate surcharge (+Rs. 2.50/L) added to official base rates
     * for Petrol, Diesel, and High-Octane.
     */
    const val PETROL_PUMP_RATE_SURCHARGE = 2.50
    const val PETROL_PUMP_RATE_SURCHARGE_FLOAT = 2.50f

    /**
     * Determines whether the given service type is eligible for the petrol pump retail surcharge.
     */
    fun isPumpRateApplicable(serviceType: String?): Boolean {
        if (serviceType.isNullOrBlank()) return false
        val lower = serviceType.lowercase()
        return lower.contains("petrol") || lower.contains("diesel") || lower.contains("octane") || lower.contains("hobc")
    }

    /**
     * Computes the final petrol pump price by adding the +Rs. 2.50/L pump rate offset.
     */
    fun getPumpRate(basePrice: Double, serviceType: String?): Double {
        return if (isPumpRateApplicable(serviceType)) basePrice + PETROL_PUMP_RATE_SURCHARGE else basePrice
    }

    fun getPumpRate(basePrice: Float, serviceType: String?): Float {
        return if (isPumpRateApplicable(serviceType)) basePrice + PETROL_PUMP_RATE_SURCHARGE_FLOAT else basePrice
    }

    /**
     * Calculates the tiered delivery fee based on fuel volume in liters:
     * - 1 to 5 Liters:   Rs. 280.00
     * - 6 to 10 Liters:  Rs. 300.00
     * - 11 to 15 Liters: Rs. 350.00 (Max 15L cap)
     */
    fun calculateFuelDeliveryFee(liters: Int): Double {
        return when {
            liters <= 0 -> 0.0
            liters <= 5 -> FUEL_DELIVERY_FEE_5L
            liters <= 10 -> FUEL_DELIVERY_FEE_10L
            else -> FUEL_DELIVERY_FEE_15L
        }
    }

    /**
     * Calculates the delivery fee based on the service item or combination.
     * Petrol, Diesel, and High-Octane use tiered pricing based on volume (quantityLiters).
     * Pure Water delivery uses light fee (Rs. 50.00).
     */
    @JvmOverloads
    fun calculateDeliveryFee(serviceType: String?, quantityLiters: Int = 5): Double {
        if (serviceType.isNullOrBlank()) return calculateFuelDeliveryFee(quantityLiters)

        val isWaterOnly = serviceType.contains("Water", ignoreCase = true) &&
                !serviceType.contains("Petrol", ignoreCase = true) &&
                !serviceType.contains("Diesel", ignoreCase = true) &&
                !serviceType.contains("Octane", ignoreCase = true) &&
                !serviceType.contains("LPG", ignoreCase = true)

        return if (isWaterOnly) WATER_DELIVERY_FEE else calculateFuelDeliveryFee(quantityLiters)
    }

    /**
     * Returns true if a service type is currently unavailable (Water and LPG Gas).
     */
    fun isServiceUnavailable(serviceType: String?): Boolean {
        if (serviceType.isNullOrBlank()) return false
        val lower = serviceType.lowercase()
        return lower.contains("water") || lower.contains("gas") || lower.contains("lpg")
    }
}
