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
