package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
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
 *   1. OilPrices.pk API (FREE, no API key, JSON, OGRA official prices, CC BY 4.0)
 *   2. TrackMate Fuel API (FREE, no API key, JSON, aggregates PSO/Shell/PakWheels)
 *   3. HamariWeb Pakistan (HTML scraping fallback)
 *   4. GlobalPetrolPrices.com (HTML scraping fallback)
 *   5. PSO Pakistan website (JSON-in-HTML extraction)
 *   6. Shell Pakistan website (JSON-in-HTML extraction)
 *
 * Sources 1 & 2 are proper JSON APIs — reliable and fast.
 * Sources 3-6 are HTML scrapers kept as emergency fallbacks.
 *
 * FuelPriceWorker and MainViewModel call [fetchLatestFuelPrices] which
 * automatically cascades through all sources before giving up.
 */
object TrackmateFuelApiService {

    // ═══════════════════════════════════════════════════════════
    //  PRIMARY JSON APIs (no API key needed, free, reliable)
    // ═══════════════════════════════════════════════════════════
    private const val OILPRICES_PK_API = "https://oilprices.pk/api/latest"
    private const val TRACKMATE_API = "https://fuel.trackmate.page/api/prices"
    private const val TRACKMATE_PSO_API = "https://fuel.trackmate.page/api/prices/pso"
    private const val TRACKMATE_SHELL_API = "https://fuel.trackmate.page/api/prices/shell"

    // ═══════════════════════════════════════════════════════════
    //  FALLBACK HTML SCRAPERS (legacy, less reliable)
    // ═══════════════════════════════════════════════════════════
    private const val HAMARIWEB_URL = "https://hamariweb.com/finance/petrol-prices.aspx"
    private const val GLOBAL_PETROL_PRICES_URL = "https://www.globalpetrolprices.com/Pakistan/gasoline_prices/"
    private const val GLOBAL_PETROL_DIESEL_URL = "https://www.globalpetrolprices.com/Pakistan/diesel_prices/"
    private const val PSO_URL = "https://psopk.com/en/products-and-services/product-prices"
    private const val SHELL_URL = "https://www.shell.com.pk/motorists/shell-fuels/fuel-pricing.html"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Fetches latest Pakistan fuel prices using a multi-source fallback chain.
     * Throws only if ALL sources fail.
     */
    suspend fun fetchLatestFuelPrices(): FuelPriceFetchResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()

        // ── Source 1: OilPrices.pk API (JSON, OGRA official, most reliable) ──
        try {
            val result = fetchFromOilPricesPk()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (e: Exception) { errors.add("OilPrices.pk: ${e.message}") }

        // ── Source 2: TrackMate Fuel API (JSON, aggregated PSO/Shell data) ──
        try {
            val result = fetchFromTrackMateApi()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (e: Exception) { errors.add("TrackMate: ${e.message}") }

        // ── Source 3: HamariWeb (HTML scraping fallback) ──
        try {
            val result = fetchFromHamariWeb()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (e: Exception) { errors.add("HamariWeb: ${e.message}") }

        // ── Source 4: GlobalPetrolPrices.com (HTML scraping fallback) ──
        try {
            val result = fetchFromGlobalPetrolPrices()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (e: Exception) { errors.add("GlobalPetrol: ${e.message}") }

        // ── Source 5: Google Search (server-rendered, always has Pakistan fuel prices) ──
        try {
            val result = fetchFromGoogleSearch()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (e: Exception) { errors.add("GoogleSearch: ${e.message}") }

        // ── Source 6: PSO website (JSON-in-script + HTML scraping) ──
        try {
            val result = fetchFromPsoWebsite()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (e: Exception) { errors.add("PSO: ${e.message}") }

        // ── Source 7: Shell website (JSON-in-script + HTML scraping) ──
        try {
            val result = fetchFromShellWebsite()
            if (result.petrol > 0f && result.diesel > 0f) return@withContext result
        } catch (e: Exception) { errors.add("Shell: ${e.message}") }

        // All sources failed — throw so Gemini fallback in FuelPriceWorker kicks in
        throw java.io.IOException("All fuel price sources failed: ${errors.joinToString("; ")}")
    }

    // ╔═══════════════════════════════════════════════════════════╗
    // ║  SOURCE 1: OilPrices.pk API (JSON — PRIMARY)             ║
    // ║  Endpoint: https://oilprices.pk/api/latest               ║
    // ║  No API key required, CC BY 4.0, ~5 min cache            ║
    // ║  Returns OGRA official consumer retail prices             ║
    // ╚═══════════════════════════════════════════════════════════╝
    private fun fetchFromOilPricesPk(): FuelPriceFetchResult {
        val request = Request.Builder()
            .url(OILPRICES_PK_API)
            .header("Accept", "application/json")
            .header("User-Agent", "Zyphuel-App/1.0 (Android)")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("OilPrices.pk: HTTP ${response.code}")

        val body = response.body?.string() ?: throw java.io.IOException("OilPrices.pk: empty body")
        val json = JSONObject(body)

        // The API may return data in different structures — handle common patterns:
        // Pattern 1: { "petrol": 328.00, "diesel": 358.00, ... }
        // Pattern 2: { "data": { "petrol": {..., "price": 328.00}, ... } }
        // Pattern 3: { "prices": [ { "name": "Petrol", "price": 328.00 }, ... ] }
        // Pattern 4: { "data": [ { "product": "Petrol", "price": 328.00 }, ... ] }

        var petrol = 0f
        var diesel = 0f
        var octane = 0f
        var effectiveDate: String? = null

        // Try top-level direct keys first
        petrol = extractPriceFromJson(json, "petrol", "petrol_price", "ms_petrol", "motor_spirit", "gasoline")
        diesel = extractPriceFromJson(json, "diesel", "diesel_price", "hsd", "high_speed_diesel")
        octane = extractPriceFromJson(json, "high_octane", "hi_octane", "hobc", "octane", "high_octane_price")

        // Try "data" object
        if (petrol == 0f || diesel == 0f) {
            val dataObj = json.optJSONObject("data")
            if (dataObj != null) {
                if (petrol == 0f) petrol = extractPriceFromJson(dataObj, "petrol", "petrol_price", "ms_petrol", "motor_spirit", "gasoline")
                if (diesel == 0f) diesel = extractPriceFromJson(dataObj, "diesel", "diesel_price", "hsd", "high_speed_diesel")
                if (octane == 0f) octane = extractPriceFromJson(dataObj, "high_octane", "hi_octane", "hobc", "octane", "high_octane_price")

                // Check nested objects like { "data": { "petrol": { "price": 252 } } }
                if (petrol == 0f) petrol = extractNestedPrice(dataObj, "petrol", "motor_spirit", "ms_petrol", "gasoline")
                if (diesel == 0f) diesel = extractNestedPrice(dataObj, "diesel", "hsd", "high_speed_diesel")
                if (octane == 0f) octane = extractNestedPrice(dataObj, "high_octane", "hi_octane", "hobc", "octane")
            }
        }

        // Try "prices" or "data" as array
        if (petrol == 0f || diesel == 0f) {
            val pricesArray = json.optJSONArray("prices") ?: json.optJSONArray("data") ?: json.optJSONArray("products")
            if (pricesArray != null) {
                parsePricesFromJsonArray(pricesArray).let { (p, d, o) ->
                    if (petrol == 0f) petrol = p
                    if (diesel == 0f) diesel = d
                    if (octane == 0f) octane = o
                }
            }
        }

        // Deep scan: iterate all top-level keys looking for price-like nested objects
        if (petrol == 0f || diesel == 0f) {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val lower = key.lowercase()
                val value = json.opt(key)
                val price = when (value) {
                    is Number -> value.toFloat()
                    is JSONObject -> value.optDouble("price", 0.0).toFloat()
                        .let { if (it == 0f) value.optDouble("retail_price", 0.0).toFloat() else it }
                        .let { if (it == 0f) value.optDouble("rate", 0.0).toFloat() else it }
                    else -> 0f
                }
                if (price in 100f..600f) {
                    when {
                        petrol == 0f && hasPetrolKeyword(lower) && !hasOctaneKeyword(lower) -> petrol = price
                        diesel == 0f && hasDieselKeyword(lower) -> diesel = price
                        octane == 0f && hasOctaneKeyword(lower) -> octane = price
                    }
                }
            }
        }

        effectiveDate = json.optString("effective_date", null)
            ?: json.optString("date", null)
            ?: json.optString("updated_at", null)
            ?: json.optJSONObject("data")?.optString("effective_date", null)

        return FuelPriceFetchResult(
            petrol = petrol,
            diesel = diesel,
            highOctane = octane,
            source = "OilPrices.pk API (OGRA Official)",
            effectiveDate = effectiveDate
        )
    }

    // ╔═══════════════════════════════════════════════════════════╗
    // ║  SOURCE 2: TrackMate Fuel API (JSON — SECONDARY)         ║
    // ║  Endpoint: https://fuel.trackmate.page/api/prices        ║
    // ║  Also: /api/prices/pso, /api/prices/shell                ║
    // ║  No API key, ~60 req/min, aggregates PSO/Shell data      ║
    // ╚═══════════════════════════════════════════════════════════╝
    private fun fetchFromTrackMateApi(): FuelPriceFetchResult {
        val request = Request.Builder()
            .url(TRACKMATE_API)
            .header("Accept", "application/json")
            .header("User-Agent", "Zyphuel-App/1.0 (Android)")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("TrackMate: HTTP ${response.code}")

        val body = response.body?.string() ?: throw java.io.IOException("TrackMate: empty body")

        var petrol = 0f
        var diesel = 0f
        var octane = 0f
        var effectiveDate: String? = null

        // TrackMate may return JSON object or array
        val trimmed = body.trim()
        if (trimmed.startsWith("[")) {
            // Array of fuel products
            val arr = JSONArray(trimmed)
            parsePricesFromJsonArray(arr).let { (p, d, o) ->
                petrol = p; diesel = d; octane = o
            }
        } else if (trimmed.startsWith("{")) {
            val json = JSONObject(trimmed)

            // Try direct keys
            petrol = extractPriceFromJson(json, "petrol", "petrol_price", "ms_petrol", "motor_spirit", "gasoline")
            diesel = extractPriceFromJson(json, "diesel", "diesel_price", "hsd", "high_speed_diesel")
            octane = extractPriceFromJson(json, "high_octane", "hi_octane", "hobc", "octane", "high_octane_price")

            // Try "data" wrapper
            if (petrol == 0f || diesel == 0f) {
                val dataObj = json.optJSONObject("data")
                if (dataObj != null) {
                    if (petrol == 0f) petrol = extractPriceFromJson(dataObj, "petrol", "petrol_price", "ms_petrol", "motor_spirit", "gasoline")
                    if (diesel == 0f) diesel = extractPriceFromJson(dataObj, "diesel", "diesel_price", "hsd", "high_speed_diesel")
                    if (octane == 0f) octane = extractPriceFromJson(dataObj, "high_octane", "hi_octane", "hobc", "octane", "high_octane_price")
                    if (petrol == 0f) petrol = extractNestedPrice(dataObj, "petrol", "motor_spirit", "gasoline")
                    if (diesel == 0f) diesel = extractNestedPrice(dataObj, "diesel", "hsd", "high_speed_diesel")
                    if (octane == 0f) octane = extractNestedPrice(dataObj, "high_octane", "hobc", "octane")
                }
            }

            // Try arrays inside object
            if (petrol == 0f || diesel == 0f) {
                val pricesArray = json.optJSONArray("prices") ?: json.optJSONArray("data") ?: json.optJSONArray("products") ?: json.optJSONArray("fuels")
                if (pricesArray != null) {
                    parsePricesFromJsonArray(pricesArray).let { (p, d, o) ->
                        if (petrol == 0f) petrol = p
                        if (diesel == 0f) diesel = d
                        if (octane == 0f) octane = o
                    }
                }
            }

            // Deep key scan
            if (petrol == 0f || diesel == 0f) {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val lower = key.lowercase()
                    val value = json.opt(key)
                    val price = when (value) {
                        is Number -> value.toFloat()
                        is JSONObject -> value.optDouble("price", 0.0).toFloat()
                            .let { if (it == 0f) value.optDouble("retail_price", 0.0).toFloat() else it }
                            .let { if (it == 0f) value.optDouble("rate", 0.0).toFloat() else it }
                        else -> 0f
                    }
                    if (price in 100f..600f) {
                        when {
                            petrol == 0f && hasPetrolKeyword(lower) && !hasOctaneKeyword(lower) -> petrol = price
                            diesel == 0f && hasDieselKeyword(lower) -> diesel = price
                            octane == 0f && hasOctaneKeyword(lower) -> octane = price
                        }
                    }
                }
            }

            effectiveDate = json.optString("effective_date", null)
                ?: json.optString("date", null)
                ?: json.optString("updated_at", null)
        }

        return FuelPriceFetchResult(
            petrol = petrol,
            diesel = diesel,
            highOctane = octane,
            source = "TrackMate Fuel API",
            effectiveDate = effectiveDate
        )
    }

    // ════════════════════════════════════════════════════════════
    //  JSON HELPER: Extract price from multiple possible key names
    // ════════════════════════════════════════════════════════════
    private fun extractPriceFromJson(json: JSONObject, vararg keys: String): Float {
        for (key in keys) {
            val value = json.optDouble(key, 0.0).toFloat()
            if (value in 100f..600f) return value
        }
        return 0f
    }

    private fun extractNestedPrice(json: JSONObject, vararg keys: String): Float {
        for (key in keys) {
            val nested = json.optJSONObject(key)
            if (nested != null) {
                val price = nested.optDouble("price", 0.0).toFloat()
                    .let { if (it in 100f..600f) it else nested.optDouble("retail_price", 0.0).toFloat() }
                    .let { if (it in 100f..600f) it else nested.optDouble("rate", 0.0).toFloat() }
                    .let { if (it in 100f..600f) it else nested.optDouble("value", 0.0).toFloat() }
                    .let { if (it in 100f..600f) it else nested.optDouble("current_price", 0.0).toFloat() }
                if (price in 100f..600f) return price
            }
        }
        return 0f
    }

    /**
     * Parse a JSONArray of fuel product objects.
     * Handles: { "name"/"product"/"fuel"/"type": "Petrol", "price"/"rate"/"value": 328.00 }
     */
    private fun parsePricesFromJsonArray(arr: JSONArray): Triple<Float, Float, Float> {
        var petrol = 0f
        var diesel = 0f
        var octane = 0f

        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val name = (item.optString("name", "")
                .ifEmpty { item.optString("product", "") }
                .ifEmpty { item.optString("fuel", "") }
                .ifEmpty { item.optString("fuel_type", "") }
                .ifEmpty { item.optString("type", "") }
                .ifEmpty { item.optString("title", "") }
                .ifEmpty { item.optString("label", "") })
                .lowercase()

            val price = item.optDouble("price", 0.0).toFloat()
                .let { if (it in 100f..600f) it else item.optDouble("retail_price", 0.0).toFloat() }
                .let { if (it in 100f..600f) it else item.optDouble("rate", 0.0).toFloat() }
                .let { if (it in 100f..600f) it else item.optDouble("value", 0.0).toFloat() }
                .let { if (it in 100f..600f) it else item.optDouble("current_price", 0.0).toFloat() }

            if (price !in 100f..600f) continue

            when {
                petrol == 0f && hasPetrolKeyword(name) && !hasOctaneKeyword(name) -> petrol = price
                diesel == 0f && hasDieselKeyword(name) -> diesel = price
                octane == 0f && hasOctaneKeyword(name) -> octane = price
            }
        }

        return Triple(petrol, diesel, octane)
    }

    // ╔═══════════════════════════════════════════════════════════╗
    // ║  SOURCE 3: HamariWeb Pakistan (HTML scraping fallback)   ║
    // ╚═══════════════════════════════════════════════════════════╝
    private fun fetchFromHamariWeb(): FuelPriceFetchResult {
        val html = fetchHtml(HAMARIWEB_URL, "HamariWeb")
        return parseFuelPricesFromHtml(html, "HamariWeb Pakistan")
    }

    // ╔═══════════════════════════════════════════════════════════╗
    // ║  SOURCE 4: GlobalPetrolPrices.com (HTML scraping)        ║
    // ╚═══════════════════════════════════════════════════════════╝
    private fun fetchFromGlobalPetrolPrices(): FuelPriceFetchResult {
        val petrolHtml = fetchHtml(GLOBAL_PETROL_PRICES_URL, "GlobalPetrolPrices-Petrol")
        val dieselHtml = fetchHtml(GLOBAL_PETROL_DIESEL_URL, "GlobalPetrolPrices-Diesel")

        val petrol = extractGlobalPetrolPrice(petrolHtml)
        val diesel = extractGlobalPetrolPrice(dieselHtml)
        val octane = if (petrol > 0f) petrol + 40f else 0f

        return FuelPriceFetchResult(
            petrol = petrol,
            diesel = diesel,
            highOctane = octane,
            source = "GlobalPetrolPrices.com",
            effectiveDate = null
        )
    }

    private fun extractGlobalPetrolPrice(html: String): Float {
        val patterns = listOf(
            Regex("""(\d{2,3}\.\d{1,3})\s*PKR""", RegexOption.IGNORE_CASE),
            Regex("""PKR\s*(\d{2,3}\.\d{1,3})""", RegexOption.IGNORE_CASE),
            Regex("""(?:Rs\.?|₨)\s*(\d{2,3}\.\d{1,2})""", RegexOption.IGNORE_CASE),
            Regex(""">\s*(\d{2,3}\.\d{2,3})\s*<""")
        )
        for (pattern in patterns) {
            for (match in pattern.findAll(html)) {
                val price = match.groupValues[1].toFloatOrNull()
                if (price != null && price in 100f..600f) return price
            }
        }
        return 0f
    }

    // ╔═══════════════════════════════════════════════════════════╗
    // ║  SOURCE 5: Google Search (server-rendered, reliable)     ║
    // ║  Google's knowledge panel shows Pakistan fuel prices     ║
    // ║  Server-rendered HTML — works with OkHttp unlike SPAs    ║
    // ╚═══════════════════════════════════════════════════════════╝
    private fun fetchFromGoogleSearch(): FuelPriceFetchResult {
        var petrol = 0f
        var diesel = 0f
        var octane = 0f

        // Fetch petrol price from Google
        try {
            val petrolHtml = fetchHtml(
                "https://www.google.com/search?q=current+petrol+price+in+pakistan+per+litre+PKR+today",
                "Google-Petrol"
            )
            petrol = extractPriceFromGoogleResults(petrolHtml, "petrol")
        } catch (_: Exception) {}

        // Fetch diesel price from Google
        try {
            val dieselHtml = fetchHtml(
                "https://www.google.com/search?q=current+diesel+price+in+pakistan+per+litre+PKR+today",
                "Google-Diesel"
            )
            diesel = extractPriceFromGoogleResults(dieselHtml, "diesel")
        } catch (_: Exception) {}

        // Fetch high octane price from Google
        try {
            val octaneHtml = fetchHtml(
                "https://www.google.com/search?q=current+high+octane+petrol+price+pakistan+PKR+today",
                "Google-Octane"
            )
            octane = extractPriceFromGoogleResults(octaneHtml, "octane")
        } catch (_: Exception) {}

        return FuelPriceFetchResult(
            petrol = petrol,
            diesel = diesel,
            highOctane = octane,
            source = "Google Search (Live)",
            effectiveDate = null
        )
    }

    /**
     * Extract fuel price from Google search result HTML.
     * Google shows prices in knowledge panels, featured snippets, and regular results.
     * Looks for patterns like "Rs. 328", "PKR 328.00", "328 per litre" etc.
     */
    private fun extractPriceFromGoogleResults(html: String, fuelType: String): Float {
        val cleanHtml = html.replace("&nbsp;", " ").replace("&#160;", " ")

        // Strategy 1: Look for prominent price display (knowledge panel / featured snippet)
        // Google often shows "Rs 328.00" or "PKR 328" in large text
        val prominentPatterns = listOf(
            // "Rs. 328.00" or "Rs 328" near fuel keywords
            Regex("""(?:Rs\.?|PKR|₨)\s*(\d{2,3}(?:\.\d{1,2})?)(?:\s*(?:per|/)\s*(?:litre|liter|L))?""", RegexOption.IGNORE_CASE),
            // "328.00 PKR" format
            Regex("""(\d{2,3}\.\d{1,2})\s*(?:Rs\.?|PKR|₨)""", RegexOption.IGNORE_CASE),
            // "328 per litre" format
            Regex("""(\d{2,3}(?:\.\d{1,2})?)\s*(?:per|/)\s*(?:litre|liter|L)""", RegexOption.IGNORE_CASE)
        )

        // Split into chunks and look for prices near fuel type keywords
        val chunks = cleanHtml.split("<div", "<span", "<td", "<p", "<li", "<h2", "<h3")
        for (chunk in chunks) {
            val lower = chunk.lowercase()
            // Check if this chunk mentions the fuel type we're looking for
            val isRelevant = when (fuelType) {
                "petrol" -> (lower.contains("petrol") || lower.contains("gasoline")) && !lower.contains("octane") && !lower.contains("diesel")
                "diesel" -> lower.contains("diesel") || lower.contains("hsd")
                "octane" -> lower.contains("octane") || lower.contains("hobc") || lower.contains("hi-octane")
                else -> false
            }

            if (isRelevant || chunk.length < 500) {
                for (pattern in prominentPatterns) {
                    val match = pattern.find(chunk)
                    if (match != null) {
                        val price = match.groupValues[1].toFloatOrNull()
                        if (price != null && price in 150f..600f) return price
                    }
                }
            }
        }

        // Strategy 2: Just find any PKR price in valid range from the entire page
        for (pattern in prominentPatterns) {
            val matches = pattern.findAll(cleanHtml)
            for (match in matches) {
                val price = match.groupValues[1].toFloatOrNull()
                if (price != null && price in 150f..600f) return price
            }
        }

        return 0f
    }

    // ╔═══════════════════════════════════════════════════════════╗
    // ║  SOURCE 6: PSO Pakistan website                          ║
    // ╚═══════════════════════════════════════════════════════════╝
    private fun fetchFromPsoWebsite(): FuelPriceFetchResult {
        val html = fetchHtml(PSO_URL, "PSO")
        val jsonResult = extractPricesFromEmbeddedJson(html, "PSO Pakistan Official")
        if (jsonResult.petrol > 0f && jsonResult.diesel > 0f) return jsonResult
        return parseFuelPricesFromHtml(html, "PSO Pakistan Official")
    }

    // ╔═══════════════════════════════════════════════════════════╗
    // ║  SOURCE 7: Shell Pakistan website                        ║
    // ╚═══════════════════════════════════════════════════════════╝
    private fun fetchFromShellWebsite(): FuelPriceFetchResult {
        val html = fetchHtml(SHELL_URL, "Shell")
        val jsonResult = extractPricesFromEmbeddedJson(html, "Shell Pakistan Official")
        if (jsonResult.petrol > 0f && jsonResult.diesel > 0f) return jsonResult
        return parseFuelPricesFromHtml(html, "Shell Pakistan Official")
    }

    // ════════════════════════════════════════════════════════════
    //  SHARED: Fetch HTML with browser-like headers
    // ════════════════════════════════════════════════════════════
    private fun fetchHtml(url: String, sourceName: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9,ur;q=0.8")
            .header("Accept-Encoding", "identity")
            .header("Cache-Control", "no-cache")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("$sourceName: ${response.code}")
        return response.body?.string() ?: throw java.io.IOException("$sourceName: empty body")
    }

    // ════════════════════════════════════════════════════════════
    //  JSON-in-Script extraction (for JavaScript-rendered sites)
    // ════════════════════════════════════════════════════════════
    private fun extractPricesFromEmbeddedJson(html: String, sourceName: String): FuelPriceFetchResult {
        var petrol = 0f
        var diesel = 0f
        var octane = 0f

        val scriptRegex = Regex("""<script[^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
        val scriptBlocks = scriptRegex.findAll(html).map { it.groupValues[1] }.toList()

        for (script in scriptBlocks) {
            val productPriceRegex = Regex("""(?:petrol|diesel|hsd|octane|hobc|motor.?spirit|super)[^}]*?(\d{2,3}\.\d{1,2})""", RegexOption.IGNORE_CASE)
            val matches = productPriceRegex.findAll(script)
            for (match in matches) {
                val fullMatch = match.value.lowercase()
                val price = match.groupValues[1].toFloatOrNull() ?: continue
                if (price < 100f || price > 600f) continue

                when {
                    petrol == 0f && (fullMatch.contains("petrol") || fullMatch.contains("super") || fullMatch.contains("motor")) && !fullMatch.contains("octane") -> petrol = price
                    diesel == 0f && (fullMatch.contains("diesel") || fullMatch.contains("hsd")) -> diesel = price
                    octane == 0f && (fullMatch.contains("octane") || fullMatch.contains("hobc")) -> octane = price
                }
            }
            if (petrol > 0f && diesel > 0f) break
        }

        return FuelPriceFetchResult(petrol = petrol, diesel = diesel, highOctane = octane, source = sourceName)
    }

    // ════════════════════════════════════════════════════════════
    //  HTML PRICE PARSER (enhanced, shared by all HTML scrapers)
    // ════════════════════════════════════════════════════════════
    private fun parseFuelPricesFromHtml(html: String, sourceName: String): FuelPriceFetchResult {
        val priceRegex = Regex("""(?:Rs\.?|PKR|₨)\s*(\d{2,3}(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val standalonePrice = Regex(""">\s*(\d{2,3}\.\d{2})\s*<""")

        var petrol = 0f
        var diesel = 0f
        var octane = 0f

        val cleanHtml = html
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&amp;", "&")
            .replace("&#8377;", "₨")
            .replace("&#8360;", "₨")

        val lines = cleanHtml.split("\n", "<tr>", "<tr ", "<td>", "<td ", "<div>", "<div ",
            "<span>", "<span ", "<p>", "<p ", "<li>", "<li ", "<dt>", "<dd>", "<th>", "<th ")

        for (i in lines.indices) {
            val line = lines[i].lowercase()
            if (line.length < 3) continue

            var foundPrice = 0f
            for (j in i..minOf(i + 4, lines.size - 1)) {
                val candidate = lines[j].replace(",", "")
                val prefixedMatch = priceRegex.find(candidate)
                if (prefixedMatch != null) {
                    val p = prefixedMatch.groupValues[1].toFloatOrNull()
                    if (p != null && p in 100f..600f) { foundPrice = p; break }
                }
                val standaloneMatch = standalonePrice.find(candidate)
                if (standaloneMatch != null) {
                    val p = standaloneMatch.groupValues[1].toFloatOrNull()
                    if (p != null && p in 100f..600f) { foundPrice = p; break }
                }
            }

            if (foundPrice > 0f) {
                when {
                    petrol == 0f && hasPetrolKeyword(line) && !hasOctaneKeyword(line) -> petrol = foundPrice
                    diesel == 0f && hasDieselKeyword(line) -> diesel = foundPrice
                    octane == 0f && hasOctaneKeyword(line) -> octane = foundPrice
                }
            }
        }

        return FuelPriceFetchResult(petrol = petrol, diesel = diesel, highOctane = octane, source = sourceName)
    }

    // ════════════════════════════════════════════════════════════
    //  KEYWORD MATCHERS
    // ════════════════════════════════════════════════════════════
    private fun hasPetrolKeyword(text: String): Boolean =
        text.contains("petrol") || text.contains("motor spirit") || text.contains("mogas") ||
        text.contains("euro-v petrol") || text.contains("super") || text.contains("gasoline") ||
        text.contains("ms ") || text.contains("e5 petrol") || text.contains("unleaded")

    private fun hasDieselKeyword(text: String): Boolean =
        text.contains("diesel") || text.contains("hsd") || text.contains("high speed") ||
        text.contains("high-speed") || text.contains("gasoil")

    private fun hasOctaneKeyword(text: String): Boolean =
        text.contains("octane") || text.contains("hobc") || text.contains("hi-octane") ||
        text.contains("ron 97") || text.contains("high octane") || text.contains("premium")
}
