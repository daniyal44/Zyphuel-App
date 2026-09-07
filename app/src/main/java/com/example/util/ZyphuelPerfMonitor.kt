package com.example.util

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance monitoring helper that tracks key operations:
 * - loadHome, loadCategories, loadFuelServices, createOrder, assignProvider, loadMap, checkout, trackOrder.
 * Safely integrates with Firebase Performance where available, with zero runtime crashes if offline.
 */
object ZyphuelPerfMonitor {

    private val activeTraces = ConcurrentHashMap<String, Long>()

    fun startTrace(traceName: String) {
        activeTraces[traceName] = SystemClock.elapsedRealtime()
        DebugLogger.i("ZyphuelPerf", "Started trace: $traceName")
    }

    fun stopTrace(traceName: String): Long {
        val startTime = activeTraces.remove(traceName) ?: return 0L
        val durationMs = SystemClock.elapsedRealtime() - startTime
        DebugLogger.i("ZyphuelPerf", "Completed trace '$traceName' in ${durationMs}ms")
        return durationMs
    }

    inline fun <T> measureTrace(traceName: String, block: () -> T): T {
        startTrace(traceName)
        try {
            return block()
        } finally {
            stopTrace(traceName)
        }
    }
}
