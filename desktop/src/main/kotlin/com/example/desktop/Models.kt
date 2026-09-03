package com.example.desktop

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Zyphuel's Lahore depot, matching AppRepository.DEPOT_LAT / DEPOT_LNG in the Android app. */
const val DEPOT_LAT = 31.4380
const val DEPOT_LNG = 74.3050

data class LatLng(val lat: Double, val lng: Double)

/**
 * One order, mirroring the `orders` Firestore documents written by the Android app.
 */
data class DesktopOrder(
    val id: Int,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val serviceType: String,
    val quantity: Int,
    val totalPrice: Double,
    val deliveryAddress: String,
    val paymentMethod: String,
    val status: String,
    val riderName: String?,
    val riderEmail: String?,
    val createdAt: Long,
    val etaMinutes: Int,
    val destLat: Double?,
    val destLng: Double?,
    val originLat: Double?,
    val originLng: Double?
) {
    val destination: LatLng get() = LatLng(destLat ?: 31.5204, destLng ?: 74.3587)
    val origin: LatLng get() = LatLng(originLat ?: DEPOT_LAT, originLng ?: DEPOT_LNG)

    val isActive: Boolean
        get() = status in setOf("Pending", "Assigned", "Accepted", "Delivering", "On the way")

    val isFinished: Boolean
        get() = status in setOf("Completed", "Delivered", "Cancelled", "Canceled")
}

/**
 * A rider's live position for one order, mirroring `live_tracking/{orderId}`.
 */
data class RiderPosition(
    val orderId: Int,
    val riderEmail: String,
    val lat: Double,
    val lng: Double,
    val bearing: Float,
    val speedKmh: Float,
    val status: String,
    val updatedAt: Long
) {
    val position: LatLng get() = LatLng(lat, lng)

    /** A fix older than two minutes means the rider app stopped reporting. */
    fun isStale(nowMillis: Long): Boolean = nowMillis - updatedAt > 120_000
}

/** Great-circle distance in kilometres. */
fun haversineKm(a: LatLng, b: LatLng): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * earthRadiusKm * atan2(sqrt(h), sqrt(1 - h))
}

/** Compass bearing in degrees from [from] to [to]. */
fun bearingBetween(from: LatLng, to: LatLng): Float {
    val lat1 = Math.toRadians(from.lat)
    val lat2 = Math.toRadians(to.lat)
    val dLng = Math.toRadians(to.lng - from.lng)
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    val deg = Math.toDegrees(atan2(y, x))
    return ((deg + 360.0) % 360.0).toFloat()
}

/**
 * Minutes remaining, from live speed when the rider is actually moving and from a
 * conservative city-traffic estimate otherwise. Same rule as the Android tracker.
 */
fun estimateEtaMinutes(remainingKm: Double, speedKmh: Float, fallbackMinutes: Int): Int {
    if (remainingKm <= 0.0) return fallbackMinutes.coerceAtLeast(1)
    return if (speedKmh > 4f) {
        Math.ceil(remainingKm / speedKmh * 60.0).toInt().coerceAtLeast(1)
    } else {
        Math.ceil(remainingKm * 3.0).toInt().coerceAtLeast(1)
    }
}

/** Fraction of the depot -> destination trip already covered, 0..1. */
fun tripProgress(origin: LatLng, current: LatLng, destination: LatLng): Float {
    val total = haversineKm(origin, destination).coerceAtLeast(0.1)
    val remaining = haversineKm(current, destination)
    return (1.0 - (remaining / total)).coerceIn(0.0, 1.0).toFloat()
}

/**
 * Fleet vehicle and rider specifications for the operations console.
 */
data class DesktopFleetRider(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val vehicleName: String,
    val vehicleNumber: String,
    val capacityLiters: Int,
    val fuelType: String
)

val DEFAULT_FLEET_RIDERS = listOf(
    DesktopFleetRider(
        id = "RIDER-1",
        name = "Rashid Minhas",
        email = "rider.rashid@zyphuel.com",
        phone = "+92 300 1234567",
        vehicleName = "Bowser Alpha (Hino 500 Heavy)",
        vehicleNumber = "LHR-7890",
        capacityLiters = 5000,
        fuelType = "Super Petrol"
    ),
    DesktopFleetRider(
        id = "RIDER-2",
        name = "Hamza Akram",
        email = "rider.hamza@zyphuel.com",
        phone = "+92 321 9876543",
        vehicleName = "Bowser Beta (Isuzu Forward)",
        vehicleNumber = "LHR-4512",
        capacityLiters = 3000,
        fuelType = "High-Speed Diesel"
    ),
    DesktopFleetRider(
        id = "RIDER-3",
        name = "Usman Farooq",
        email = "rider.usman@zyphuel.com",
        phone = "+92 333 4567890",
        vehicleName = "Bowser Gamma (FAW J5M Water)",
        vehicleNumber = "LHR-1122",
        capacityLiters = 4000,
        fuelType = "Drinking Water"
    ),
    DesktopFleetRider(
        id = "RIDER-4",
        name = "Tariq Mehmood",
        email = "rider.tariq@zyphuel.com",
        phone = "+92 302 7654321",
        vehicleName = "Bowser Delta (Forland LPG/Octane)",
        vehicleNumber = "LHR-9988",
        capacityLiters = 2500,
        fuelType = "High Octane / LPG"
    )
)

