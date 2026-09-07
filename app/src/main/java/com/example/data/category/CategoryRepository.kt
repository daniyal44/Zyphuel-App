package com.example.data.category

import android.content.Context
import android.content.SharedPreferences
import com.example.util.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for Zyphuel Category & Subcategory Catalog.
 * High-performance, data-driven, and reactive.
 */
class CategoryRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("zyphuel_categories_v2", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _categories = MutableStateFlow<List<Category>>(CategoryCatalogSeed.getDefaultCategories())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadSavedCategoryOverrides()
    }

    private fun loadSavedCategoryOverrides() {
        try {
            val json = prefs.getString("category_status_overrides", null)
            if (!json.isNullOrBlank()) {
                val jsonObject = JSONObject(json)
                val current = _categories.value.map { cat ->
                    var updated = cat
                    if (jsonObject.has("${cat.id}_active")) {
                        updated = updated.copy(isActive = jsonObject.getBoolean("${cat.id}_active"))
                    }
                    if (jsonObject.has("${cat.id}_availability")) {
                        val availName = jsonObject.getString("${cat.id}_availability")
                        val avail = CategoryAvailability.entries.firstOrNull { it.name == availName }
                        if (avail != null) {
                            updated = updated.copy(availabilityStatus = avail)
                        }
                    }
                    if (jsonObject.has("${cat.id}_delivery_fee")) {
                        val fee = jsonObject.getDouble("${cat.id}_delivery_fee")
                        updated = updated.copy(pricingConfig = updated.pricingConfig.copy(deliveryFee = fee))
                    }
                    updated
                }
                _categories.value = current
            }
        } catch (e: Exception) {
            DebugLogger.w("CategoryRepository", "Failed to load category overrides: ${e.message}")
        }
    }

    /**
     * Synchronizes live dynamic fuel prices into the Fuel & Energy category & subcategories.
     */
    fun syncLiveFuelPrices(petrol: Float, diesel: Float, octane: Float, lpg: Float, water: Float) {
        val currentList = _categories.value.map { category ->
            if (category.id == "fuel_energy") {
                val updatedSubcats = category.subcategories.map { sub ->
                    when (sub.id) {
                        "petrol_regular", "petrol_emergency" -> sub.copy(basePrice = petrol.toDouble())
                        "petrol_premium" -> sub.copy(basePrice = (petrol + 5.68).toDouble())
                        "petrol_octane" -> sub.copy(basePrice = octane.toDouble())
                        "diesel_regular", "diesel_generator", "diesel_fleet", "diesel_emergency" -> sub.copy(basePrice = diesel.toDouble())
                        "diesel_premium" -> sub.copy(basePrice = (diesel + 7.50).toDouble())
                        "lpg_sealed_cylinder", "lpg_refill_exchange", "lpg_emergency", "lpg_commercial" -> sub.copy(basePrice = lpg.toDouble())
                        else -> sub
                    }
                }
                category.copy(subcategories = updatedSubcats)
            } else if (category.id == "water_delivery") {
                val updatedSubcats = category.subcategories.map { sub ->
                    if (sub.id == "water_drinking") sub.copy(basePrice = (water * 3.6).coerceAtLeast(180.0))
                    else sub
                }
                category.copy(subcategories = updatedSubcats)
            } else {
                category
            }
        }
        _categories.value = currentList
    }

    /**
     * Fast, typo-tolerant indexed search across all categories and subcategories.
     * Searches category names, subcategory names, service groups, descriptions, and keywords.
     */
    fun searchServices(query: String, selectedVehicle: VehicleType? = null): List<ServiceSearchResult> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return emptyList()

        val tokens = cleanQuery.split(" ").filter { it.isNotBlank() }
        val results = mutableListOf<ServiceSearchResult>()

        for (category in _categories.value) {
            if (!category.isActive) continue

            for (subcategory in category.subcategories) {
                if (!subcategory.isActive) continue

                // Check vehicle compatibility if selected
                if (selectedVehicle != null && subcategory.requiresVehicle && !subcategory.supportedVehicleTypes.contains(selectedVehicle)) {
                    continue
                }

                val targetText = "${category.name} ${subcategory.name} ${subcategory.serviceGroup ?: ""} ${subcategory.description} ${subcategory.badgeText ?: ""}".lowercase()
                
                var matchScore = 0

                // Exact full query match
                if (subcategory.name.lowercase().contains(cleanQuery)) {
                    matchScore += 100
                } else if (category.name.lowercase().contains(cleanQuery)) {
                    matchScore += 70
                }

                // Token based matching
                var tokenHits = 0
                for (token in tokens) {
                    if (subcategory.name.lowercase().contains(token)) {
                        tokenHits++
                        matchScore += 40
                    } else if ((subcategory.serviceGroup ?: "").lowercase().contains(token)) {
                        tokenHits++
                        matchScore += 30
                    } else if (subcategory.description.lowercase().contains(token)) {
                        tokenHits++
                        matchScore += 20
                    } else if (category.name.lowercase().contains(token)) {
                        tokenHits++
                        matchScore += 15
                    } else if (isFuzzyMatch(token, subcategory.name.lowercase())) {
                        tokenHits++
                        matchScore += 25
                    }
                }

                if (matchScore > 0 || tokenHits >= tokens.size) {
                    results.add(ServiceSearchResult(subcategory, category, matchScore))
                }
            }
        }

        return results.sortedByDescending { it.matchRelevance }
    }

    /**
     * Simple fast Levenshtein-distance based fuzzy matcher for typo tolerance.
     */
    private fun isFuzzyMatch(token: String, target: String): Boolean {
        if (token.length < 3) return false
        val maxDist = if (token.length >= 5) 2 else 1
        val words = target.split(" ")
        for (word in words) {
            if (Math.abs(token.length - word.length) <= maxDist) {
                val dist = levenshteinDistance(token, word)
                if (dist <= maxDist) return true
            }
        }
        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Admin toggle category active status.
     */
    fun toggleCategoryActive(categoryId: String, isActive: Boolean) {
        val updated = _categories.value.map {
            if (it.id == categoryId) it.copy(isActive = isActive) else it
        }
        _categories.value = updated
        persistOverride("${categoryId}_active", isActive)
    }

    /**
     * Admin toggle subcategory active status.
     */
    fun toggleSubcategoryActive(categoryId: String, subcategoryId: String, isActive: Boolean) {
        val updated = _categories.value.map { category ->
            if (category.id == categoryId) {
                val updatedSubs = category.subcategories.map { sub ->
                    if (sub.id == subcategoryId) sub.copy(isActive = isActive) else sub
                }
                category.copy(subcategories = updatedSubs)
            } else category
        }
        _categories.value = updated
        persistOverride("${subcategoryId}_active", isActive)
    }

    /**
     * Admin update category availability status.
     */
    fun updateCategoryAvailability(categoryId: String, availability: CategoryAvailability) {
        val updated = _categories.value.map {
            if (it.id == categoryId) it.copy(availabilityStatus = availability) else it
        }
        _categories.value = updated
        persistOverride("${categoryId}_availability", availability.name)
    }

    /**
     * Admin update category delivery fee.
     */
    fun updateCategoryPricing(categoryId: String, deliveryFee: Double) {
        val updated = _categories.value.map {
            if (it.id == categoryId) it.copy(pricingConfig = it.pricingConfig.copy(deliveryFee = deliveryFee)) else it
        }
        _categories.value = updated
        persistOverride("${categoryId}_delivery_fee", deliveryFee)
    }

    private fun persistOverride(key: String, value: Any) {
        scope.launch {
            try {
                val json = prefs.getString("category_status_overrides", null)
                val jsonObject = if (!json.isNullOrBlank()) JSONObject(json) else JSONObject()
                when (value) {
                    is Boolean -> jsonObject.put(key, value)
                    is String -> jsonObject.put(key, value)
                    is Double -> jsonObject.put(key, value)
                    is Int -> jsonObject.put(key, value)
                }
                prefs.edit().putString("category_status_overrides", jsonObject.toString()).apply()
            } catch (e: Exception) {
                DebugLogger.w("CategoryRepository", "Error saving override: ${e.message}")
            }
        }
    }

    fun getCategoryById(categoryId: String): Category? {
        return _categories.value.firstOrNull { it.id == categoryId }
    }
}
