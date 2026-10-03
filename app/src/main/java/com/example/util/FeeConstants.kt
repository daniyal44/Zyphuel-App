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
 * Zyphuel exclusively provides Euro-V Petrol, High-Speed Diesel, and High-Octane 97 fuel delivery.
 */
object FeeConstants {
    const val FUEL_MAX_LITERS = 15
    const val FUEL_DELIVERY_FEE_5L = 280.0
    const val FUEL_DELIVERY_FEE_10L = 300.0
    const val FUEL_DELIVERY_FEE_15L = 350.0

    // Backward compatibility base fee
    const val FUEL_DELIVERY_FEE = 280.0

    /**
     * Petrol pump retail rate margin/surcharge (+Rs. 5.00/L) added to official base rates
     * for Petrol, Diesel, and High-Octane.
     */
    const val PETROL_PUMP_RATE_SURCHARGE = 5.00
    const val PETROL_PUMP_RATE_SURCHARGE_FLOAT = 5.00f

    /**
     * Determines whether the given service type is eligible for the petrol pump retail surcharge.
     */
    fun isPumpRateApplicable(serviceType: String?): Boolean {
        if (serviceType.isNullOrBlank()) return false
        val lower = serviceType.lowercase()
        return lower.contains("petrol") || lower.contains("diesel") || lower.contains("octane") || lower.contains("hobc")
    }

    /**
     * Computes the final petrol pump price by adding the +Rs. 5.00/L pump rate offset.
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
     * Calculates the delivery fee based on fuel volume (quantityLiters).
     * Petrol, Diesel, and High-Octane use tiered pricing based on volume:
     * - 1 to 5 Liters:   Rs. 280.00
     * - 6 to 10 Liters:  Rs. 300.00
     * - 11 to 15 Liters: Rs. 350.00
     */
    @JvmOverloads
    fun calculateDeliveryFee(serviceType: String?, quantityLiters: Int = 5): Double {
        return calculateFuelDeliveryFee(quantityLiters)
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
