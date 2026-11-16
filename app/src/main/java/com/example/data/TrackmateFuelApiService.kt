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
    val lpgGas: Float = 0f,
    val water: Float = 0f,
    val source: String,
    val effectiveDate: String? = null
)

/**
 * Multi-source Pakistan fuel price fetcher.
 * Priority chain:
 *   1. Trackmate Fuel API (aggregated PSO/Shell/OGRA data)
 *   2. PSO Pakistan official website scraping
 *   3. Shell Pakistan official website scraping
 *   4. Codeistry Pakistan fuel prices API (community API)
 *
 * Each source is tried in order; the first successful response wins.
 * FuelPriceWorker and MainViewModel call [fetchLatestFuelPrices] which
 * automatically cascades through all sources before giving up.
 */
object TrackmateFuelApiService {

    private const val TRACKMATE_URL = "https://fuel.trackmate.page/api/prices"
    private const val PSO_URL = "https://psopk.com/en/products-and-services/product-prices"
    private const val SHELL_URL = "https://www.shell.com.pk/motorists/shell-fuels/fuel-pricing.html"
    private const val CODEISTRY_URL = "https://api.codeistry.com.pk/fuel-prices"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Fetches latest Pakistan fuel prices using a multi-source fallback chain.
     * Throws only if ALL sources fail.
     */
    suspend fun fetchLatestFuelPrices(): FuelPriceFetchResult = withContext(Dispatchers.IO) {
        // Source 1: Trackmate API
        try {
            val result = fetchFromTrackmate()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (_: Exception) { }

        // Source 2: PSO website scraping
        try {
            val result = fetchFromPsoWebsite()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (_: Exception) { }

        // Source 3: Shell website scraping
        try {
            val result = fetchFromShellWebsite()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (_: Exception) { }

        // Source 4: Codeistry community API
        try {
            val result = fetchFromCodeistry()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (_: Exception) { }

        // All sources failed — throw so Gemini fallback in FuelPriceWorker kicks in
        throw java.io.IOException("All fuel price sources failed")
    }

    // ============================================================
    //  SOURCE 1: Trackmate Fuel API (JSON)
    // ============================================================
    private fun fetchFromTrackmate(): FuelPriceFetchResult {
        val request = Request.Builder()
            .url(TRACKMATE_URL)
            .header("Accept", "application/json")
            .header("User-Agent", "ZyphuelApp/2.0 (Android)")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("Trackmate: ${response.code}")

        val bodyString = response.body?.string() ?: throw java.io.IOException("Trackmate: empty body")
        val jsonRoot = JSONObject(bodyString)
        val pricesArray = jsonRoot.getJSONArray("prices")

        var foundPetrol: Float? = null
        var foundDiesel: Float? = null
        var foundOctane: Float? = null
        var effectiveDateStr: String? = null

        for (i in 0 until pricesArray.length()) {
            val item = pricesArray.getJSONObject(i)
            val product = item.optString("product", "").lowercase()
            val source = item.optString("source", "").lowercase()
            val pricePkr = item.optDouble("price_pkr", 0.0).toFloat()
            val effDate = item.optString("effective_date", "")
            if (effDate.isNotBlank() && effectiveDateStr == null) effectiveDateStr = effDate
            if (pricePkr <= 0f) continue

            when (product) {
                "petrol" -> if (foundPetrol == null || source == "pso") foundPetrol = pricePkr
                "hsd" -> if (foundDiesel == null || source == "pso" || source == "shell") foundDiesel = pricePkr
                "octane_plus", "hobc", "high_octane" -> if (foundOctane == null || source == "pso") foundOctane = pricePkr
            }
        }

        return FuelPriceFetchResult(
            petrol = foundPetrol ?: 0f,
            diesel = foundDiesel ?: 0f,
            highOctane = foundOctane ?: 0f,
            source = "Trackmate Fuel API (PSO/Shell)",
            effectiveDate = effectiveDateStr
        )
    }

    // ============================================================
    //  SOURCE 2: PSO Pakistan official website (HTML scraping)
    // ============================================================
    private fun fetchFromPsoWebsite(): FuelPriceFetchResult {
        val request = Request.Builder()
            .url(PSO_URL)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ZyphuelApp")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("PSO: ${response.code}")

        val html = response.body?.string() ?: throw java.io.IOException("PSO: empty body")
        return parseFuelPricesFromHtml(html, "PSO Pakistan Official")
    }

    // ============================================================
    //  SOURCE 3: Shell Pakistan official website (HTML scraping)
    // ============================================================
    private fun fetchFromShellWebsite(): FuelPriceFetchResult {
        val request = Request.Builder()
            .url(SHELL_URL)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ZyphuelApp")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("Shell: ${response.code}")

        val html = response.body?.string() ?: throw java.io.IOException("Shell: empty body")
        return parseFuelPricesFromHtml(html, "Shell Pakistan Official")
    }

    // ============================================================
    //  SOURCE 4: Codeistry community API (JSON)
    // ============================================================
    private fun fetchFromCodeistry(): FuelPriceFetchResult {
        val request = Request.Builder()
            .url(CODEISTRY_URL)
            .header("Accept", "application/json")
            .header("User-Agent", "ZyphuelApp/2.0 (Android)")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("Codeistry: ${response.code}")

        val bodyString = response.body?.string() ?: throw java.io.IOException("Codeistry: empty body")
        val json = JSONObject(bodyString)

        // Codeistry API typically returns: { "petrol": 272.89, "diesel": 278.19, ... }
        val petrol = json.optDouble("petrol", 0.0).toFloat()
        val diesel = json.optDouble("diesel", json.optDouble("high_speed_diesel", 0.0)).toFloat()
        val octane = json.optDouble("high_octane", json.optDouble("hobc", 0.0)).toFloat()

        return FuelPriceFetchResult(
            petrol = petrol,
            diesel = diesel,
            highOctane = octane,
            source = "Codeistry Pakistan Fuel API"
        )
    }

    // ============================================================
    //  HTML PRICE PARSER (shared by PSO and Shell scrapers)
    // ============================================================
    /**
     * Extracts fuel prices from HTML pages that contain price tables.
     * Looks for patterns like "Rs. 272.89" or "PKR 272.89" near fuel product names.
     * Works with both PSO and Shell website layouts.
     */
    private fun parseFuelPricesFromHtml(html: String, sourceName: String): FuelPriceFetchResult {
        val priceRegex = Regex("""(?:Rs\.?|PKR)\s*(\d{2,3}(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)

        var petrol = 0f
        var diesel = 0f
        var octane = 0f

        // Split into lines/sections and search near product keywords
        val lines = html.split("\n", "<tr>", "<td>", "<div>", "<span>", "<p>")

        for (i in lines.indices) {
            val line = lines[i].lowercase()
            val priceMatch = priceRegex.find(lines[i])
                ?: (if (i + 1 < lines.size) priceRegex.find(lines[i + 1]) else null)
                ?: (if (i + 2 < lines.size) priceRegex.find(lines[i + 2]) else null)

            if (priceMatch != null) {
                val price = priceMatch.groupValues[1].toFloatOrNull() ?: continue
                if (price < 100f || price > 600f) continue // sanity check for PKR range

                when {
                    petrol == 0f && (line.contains("petrol") || line.contains("motor spirit") || line.contains("mogas") || line.contains("euro-v petrol") || line.contains("super")) && !line.contains("octane") && !line.contains("hobc") -> {
                        petrol = price
                    }
                    diesel == 0f && (line.contains("diesel") || line.contains("hsd") || line.contains("high speed") || line.contains("high-speed")) -> {
                        diesel = price
                    }
                    octane == 0f && (line.contains("octane") || line.contains("hobc") || line.contains("hi-octane") || line.contains("ron 97") || line.contains("high octane")) -> {
                        octane = price
                    }
                }
            }
        }

        return FuelPriceFetchResult(
            petrol = petrol,
            diesel = diesel,
            highOctane = octane,
            source = sourceName
        )
    }
}
