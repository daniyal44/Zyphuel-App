package com.example.util

/**
 * Standardized delivery fee constants and calculation utility.
 * Guarantees 100% consistency across OrderDialog, TrackerScreen, CustomerOrderCard,
 * FareBreakdownDialog, and official invoices.
 */
object FeeConstants {
    const val FUEL_DELIVERY_FEE = 250.0
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
     * Calculates the delivery fee based on the service item or combination.
     * Petrol, Diesel, High-Octane, and LPG use the fixed delivery fee of Rs. 250.00.
     * Pure Water delivery uses the light delivery fee (Rs. 50.00).
     */
    fun calculateDeliveryFee(serviceType: String?): Double {
        if (serviceType.isNullOrBlank()) return FUEL_DELIVERY_FEE

        val isWaterOnly = serviceType.contains("Water", ignoreCase = true) &&
                !serviceType.contains("Petrol", ignoreCase = true) &&
                !serviceType.contains("Diesel", ignoreCase = true) &&
                !serviceType.contains("Octane", ignoreCase = true) &&
                !serviceType.contains("LPG", ignoreCase = true)

        return if (isWaterOnly) WATER_DELIVERY_FEE else FUEL_DELIVERY_FEE
    }
}
