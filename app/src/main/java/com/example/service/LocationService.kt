package com.example.service

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-accuracy real-time LocationService for Zyphuel.
 * Uses FusedLocationProviderClient for reliable, automatic live GPS updates.
 * Manages location state flows and error reporting while supporting manual address overrides if detection fails.
 */
object LocationService {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private val _lastFreshLocation = MutableStateFlow<Location?>(null)
    val lastFreshLocation: StateFlow<Location?> = _lastFreshLocation.asStateFlow()

    private val _isTrackingActive = MutableStateFlow(false)
    val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _isLocationAvailable = MutableStateFlow(true)
    val isLocationAvailable: StateFlow<Boolean> = _isLocationAvailable.asStateFlow()

    private fun getClient(context: Context): FusedLocationProviderClient {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        }
        return fusedLocationClient!!
    }

    /**
     * Clears cached/stale location data to ensure fresh GPS coordinates.
     */
    fun clearPreviousLocationCache(context: Context? = null) {
        _lastFreshLocation.value = null
        _locationError.value = null
        try {
            if (context != null) {
                getClient(context).flushLocations()
            } else {
                fusedLocationClient?.flushLocations()
            }
        } catch (e: Exception) {
            // Safe ignore if client unavailable
        }
    }

    /**
     * Starts persistent real-time FusedLocationProviderClient listener for automatic live GPS updates.
     */
    fun startPersistentLocationUpdates(
        context: Context,
        onLocationUpdate: (lat: Double, lng: Double, accuracy: Float) -> Unit
    ) {
        try {
            val client = getClient(context)
            clearPreviousLocationCache(context)
            stopLocationUpdates()

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000L
            )
                .setMinUpdateIntervalMillis(1000L)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setWaitForAccurateLocation(true)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val freshLocation = result.lastLocation
                    if (freshLocation != null) {
                        _lastFreshLocation.value = freshLocation
                        _isLocationAvailable.value = true
                        _locationError.value = null
                        onLocationUpdate(freshLocation.latitude, freshLocation.longitude, freshLocation.accuracy)
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    val isAvailable = availability.isLocationAvailable
                    _isLocationAvailable.value = isAvailable
                    if (!isAvailable) {
                        _locationError.value = "GPS signal weak or disabled. Manual address override available."
                    }
                }
            }

            client.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            _isTrackingActive.value = true
            _locationError.value = null
        } catch (e: SecurityException) {
            _isTrackingActive.value = false
            _isLocationAvailable.value = false
            _locationError.value = "Location permission denied. Please enter address manually."
        } catch (e: Exception) {
            _isTrackingActive.value = false
            _isLocationAvailable.value = false
            _locationError.value = e.localizedMessage ?: "Failed to initialize live GPS."
        }
    }

    /**
     * Fetches a single fresh location using FusedLocationProviderClient with strict cache clearance and fallback.
     */
    fun fetchFreshSingleLocation(
        context: Context,
        onLocationResult: (lat: Double, lng: Double) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        try {
            val client = getClient(context)
            clearPreviousLocationCache(context)
            val cancelToken = CancellationTokenSource()

            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancelToken.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    _lastFreshLocation.value = location
                    _isLocationAvailable.value = true
                    _locationError.value = null
                    onLocationResult(location.latitude, location.longitude)
                } else {
                    // Fallback to single location request update
                    val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                        .setMinUpdateIntervalMillis(500L)
                        .setMaxUpdates(1)
                        .setWaitForAccurateLocation(true)
                        .build()

                    val tempCallback = object : LocationCallback() {
                        override fun onLocationResult(res: LocationResult) {
                            try {
                                client.removeLocationUpdates(this)
                            } catch (e: Exception) { /* Safe ignore */ }
                            val loc = res.lastLocation
                            if (loc != null) {
                                _lastFreshLocation.value = loc
                                _isLocationAvailable.value = true
                                _locationError.value = null
                                onLocationResult(loc.latitude, loc.longitude)
                            } else {
                                // Fallback to default Lahore coordinates
                                _isLocationAvailable.value = false
                                _locationError.value = "GPS position unavailable. Using default Lahore map position."
                                onLocationResult(31.4380, 74.3050)
                            }
                        }
                    }
                    try {
                        client.requestLocationUpdates(req, tempCallback, Looper.getMainLooper())
                    } catch (ex: SecurityException) {
                        _isLocationAvailable.value = false
                        _locationError.value = "Location permission required for GPS auto-detection."
                        onLocationResult(31.4380, 74.3050)
                    }
                }
            }.addOnFailureListener { ex ->
                _isLocationAvailable.value = false
                val msg = ex.localizedMessage ?: "GPS location request failed."
                _locationError.value = msg
                onError(msg)
                onLocationResult(31.4380, 74.3050)
            }
        } catch (e: Exception) {
            _isLocationAvailable.value = false
            val msg = e.localizedMessage ?: "Location fetch error"
            _locationError.value = msg
            onError(msg)
            onLocationResult(31.4380, 74.3050)
        }
    }

    /**
     * Stops the persistent FusedLocationProviderClient listener.
     */
    fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            try {
                fusedLocationClient?.removeLocationUpdates(callback)
            } catch (e: Exception) {
                // Safe ignore
            }
        }
        locationCallback = null
        _isTrackingActive.value = false
    }
}

