package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FuelPriceFetchResult(
    val petrol: Float,
    val diesel: Float,
    val highOctane: Float,
    val lpgGas: Float,
    val water: Float = 50.0f,
    val source: String,
    val effectiveDate: String? = null
)

object TrackmateFuelApiService {

    private const val API_URL = "https://fuel.trackmate.page/api/prices"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatestFuelPrices(): FuelPriceFetchResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(API_URL)
            .header("Accept", "application/json")
            .header("User-Agent", "ZyphuelApp/1.0 (Android)")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw java.io.IOException("Trackmate API call failed with status: ${response.code}")
        }

        val bodyString = response.body?.string() ?: throw java.io.IOException("Empty body from Trackmate API")
        val jsonRoot = JSONObject(bodyString)
        val pricesArray = jsonRoot.getJSONArray("prices")

        var foundPetrol: Float? = null
        var foundDiesel: Float? = null
        var foundOctane: Float? = null
        var foundLpg: Float? = null
        var effectiveDateStr: String? = null

        // Loop through prices array to pick official values
        for (i in 0 until pricesArray.length()) {
            val item = pricesArray.getJSONObject(i)
            val product = item.optString("product", "").lowercase()
            val source = item.optString("source", "").lowercase()
            val pricePkr = item.optDouble("price_pkr", 0.0).toFloat()
            val city = item.optString("city", "")
            val effDate = item.optString("effective_date", "")
            if (effDate.isNotBlank() && effectiveDateStr == null) {
                effectiveDateStr = effDate
            }

            if (pricePkr <= 0f) continue

            when (product) {
                "petrol" -> {
                    if (foundPetrol == null || source == "pso") {
                        foundPetrol = pricePkr
                    }
                }
                "hsd" -> { // High Speed Diesel
                    if (foundDiesel == null || source == "pso" || source == "shell") {
                        foundDiesel = pricePkr
                    }
                }
                "octane_plus", "hobc", "high_octane" -> {
                    if (foundOctane == null || city.equals("Lahore", ignoreCase = true) || source == "pso") {
                        foundOctane = pricePkr
                    }
                }
                "lpg" -> {
                    if (foundLpg == null || source == "pso") {
                        foundLpg = pricePkr
                    }
                }
            }
        }

        val petrol = foundPetrol ?: 320.73f
        val diesel = foundDiesel ?: 375.04f
        val octane = foundOctane ?: 340.00f
        val lpg = foundLpg ?: 241.43f

        FuelPriceFetchResult(
            petrol = petrol,
            diesel = diesel,
            highOctane = octane,
            lpgGas = lpg,
            water = 50.0f,
            source = "Trackmate Fuel API (PSO / Shell / PakWheels)",
            effectiveDate = effectiveDateStr
        )
    }
}
