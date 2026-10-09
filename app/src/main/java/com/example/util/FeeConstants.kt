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
    const val FUEL_DELIVERY_FEE_5L = 300.0
    const val FUEL_DELIVERY_FEE_7L = 300.0
    const val FUEL_DELIVERY_FEE_10L = 300.0
    const val FUEL_DELIVERY_FEE_11L = 320.0
    const val FUEL_DELIVERY_FEE_12L = 340.0
    const val FUEL_DELIVERY_FEE_13L = 360.0
    const val FUEL_DELIVERY_FEE_14L = 380.0
    const val FUEL_DELIVERY_FEE_15L = 400.0

    // Backward compatibility base fee
    const val FUEL_DELIVERY_FEE = 300.0

    /**
     * Petrol pump retail rate margin/markup (+Rs. 5.00/L) added on top of official OGRA
     * ex-depot base rates for Super Euro-V Petrol, Euro-V Diesel, and High-Octane 97.
     * Math runs silently in background; UI displays only the final pump rate.
     */
    const val PUMP_RATE_MARKUP = 5.00
    const val PUMP_RATE_MARKUP_FLOAT = 5.00f

    // Compatibility aliases
    const val PETROL_PUMP_RATE_SURCHARGE = PUMP_RATE_MARKUP
    const val PETROL_PUMP_RATE_SURCHARGE_FLOAT = PUMP_RATE_MARKUP_FLOAT

    /**
     * Determines whether the given service type is eligible for the petrol pump retail markup.
     */
    fun isPumpRateApplicable(serviceType: String?): Boolean {
        if (serviceType.isNullOrBlank()) return false
        val lower = serviceType.lowercase()
        return lower.contains("petrol") || lower.contains("diesel") || lower.contains("octane") || lower.contains("hobc")
    }

    /**
     * Computes the final petrol pump price by adding the +Rs. 5.00/L pump rate offset.
     * Formula: finalRate = ograBase + 5.00
     */
    fun getPumpRate(basePrice: Double, serviceType: String?): Double {
        return if (isPumpRateApplicable(serviceType)) basePrice + PUMP_RATE_MARKUP else basePrice
    }

    fun getPumpRate(basePrice: Float, serviceType: String?): Float {
        return if (isPumpRateApplicable(serviceType)) basePrice + PUMP_RATE_MARKUP_FLOAT else basePrice
    }

    /**
     * Calculates the tiered delivery fee based on fuel volume in liters:
     * - Up to 10 Liters (5L, 7L, 10L): Rs. 300.00
     * - 11 Liters: Rs. 320.00
     * - 12 Liters: Rs. 340.00
     * - 13 Liters: Rs. 360.00
     * - 14 Liters: Rs. 380.00
     * - 15 Liters: Rs. 400.00 (Max 15L cap)
     */
    fun calculateFuelDeliveryFee(liters: Int): Double {
        return when {
            liters <= 0 -> 0.0
            liters <= 10 -> FUEL_DELIVERY_FEE_10L
            liters == 11 -> FUEL_DELIVERY_FEE_11L
            liters == 12 -> FUEL_DELIVERY_FEE_12L
            liters == 13 -> FUEL_DELIVERY_FEE_13L
            liters == 14 -> FUEL_DELIVERY_FEE_14L
            else -> FUEL_DELIVERY_FEE_15L
        }
    }

    /**
     * Calculates the delivery fee based on fuel volume (quantityLiters).
     * Petrol, Diesel, and High-Octane use tiered pricing based on volume:
     * - Up to 10 Liters: Rs. 300.00
     * - 11 to 15 Liters: Tiered from Rs. 320.00 to Rs. 400.00
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
