package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.category.*
import com.example.data.vehicle.VehicleEntity
import com.example.util.CoverageStatus
import com.example.util.LocationCoverageManager

val ZyphuelBluePrimary = Color(0xFF0284C7)
val ZyphuelBlueDark = Color(0xFF0369A1)
val ZyphuelBlueSecondary = Color(0xFF0EA5E9)

/**
 * Maps category icon string names to Material icons safely.
 */
object CategoryIconHelper {
    fun getIcon(name: String): ImageVector {
        return when (name) {
            "LocalGasStation" -> Icons.Filled.LocalGasStation
            "Build" -> Icons.Filled.Build
            "CarCrash" -> Icons.Filled.CarCrash
            "CleaningServices" -> Icons.Filled.CleaningServices
            "TireRepair" -> Icons.Filled.TireRepair
            "BatteryChargingFull" -> Icons.Filled.BatteryChargingFull
            "WaterDrop" -> Icons.Filled.WaterDrop
            "ElectricCar" -> Icons.Filled.ElectricCar
            "InvertColors" -> Icons.Filled.InvertColors
            "Business" -> Icons.Filled.Business
            else -> Icons.Filled.MiscellaneousServices
        }
    }

    fun getCategoryColor(id: String): Color {
        return when (id) {
            "fuel_energy" -> ZyphuelBluePrimary       // Electric Cobalt Blue (Logo Primary)
            "gas_cylinder" -> ZyphuelBlueSecondary    // Cyan Flame Accent (Logo Highlight)
            "water_delivery" -> Color(0xFF0284C7)     // Ocean Blue
            "auto_repair" -> ZyphuelBlueDark          // Deep Royal Blue
            "roadside_assistance" -> ZyphuelBluePrimary
            "auto_detailing" -> ZyphuelBluePrimary
            "tyres_wheels" -> ZyphuelBluePrimary
            "battery_services" -> ZyphuelBlueSecondary
            "lubricants_fluids" -> ZyphuelBlueDark
            "ev_services" -> ZyphuelBlueSecondary
            "fleet_business" -> ZyphuelBlueDark
            else -> ZyphuelBluePrimary
        }
    }
}

/**
 * 1. GLOBAL SERVICE SEARCH BAR
 * Fast, typo-tolerant, category-aware, and indexed.
 */
@Composable
fun ServiceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    searchResults: List<ServiceSearchResult>,
    onSelectResult: (Subcategory, Category) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search fuel, puncture, battery jump, oil...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(ZyphuelBluePrimary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = ZyphuelBluePrimary, modifier = Modifier.size(18.dp))
                }
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = ZyphuelBluePrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
                .testTag("global_service_search_bar")
        )

        // Dropdown Search Results Overlay
        AnimatedVisibility(visible = query.isNotBlank() && searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .heightIn(max = 280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Text(
                            text = "Found ${searchResults.size} matching services:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    items(searchResults) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectResult(result.subcategory, result.parentCategory) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(CategoryIconHelper.getCategoryColor(result.parentCategory.id).copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(result.parentCategory.iconName),
                                        contentDescription = null,
                                        tint = CategoryIconHelper.getCategoryColor(result.parentCategory.id),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = result.subcategory.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ZyphuelBlueDark
                                    )
                                    Text(
                                        text = "${result.parentCategory.name} • ${result.subcategory.estimatedDuration}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (result.subcategory.basePrice > 0) {
                                    Text(
                                        text = "Rs. ${String.format(java.util.Locale.US, "%,.0f", result.subcategory.basePrice)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ZyphuelBluePrimary
                                    )
                                }
                                Text(
                                    text = "Book >",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF059669),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Divider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

/**
 * 2. QUICK ACTIONS PILL BAR
 */
@Composable
fun QuickActionsBar(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        Triple("Order Fuel ⛽", Icons.Filled.LocalGasStation, "fuel_energy"),
        Triple("Gas Cylinders 🔥", Icons.Filled.PropaneTank, "gas_cylinder"),
        Triple("Pure Water 🚰", Icons.Filled.WaterDrop, "water_delivery")
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
            )
            Text(
                text = "Instant 1-Tap",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions.forEach { (label, icon, categoryId) ->
                val tint = CategoryIconHelper.getCategoryColor(categoryId)
                Surface(
                    onClick = { onActionClick(categoryId) },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.testTag("quick_action_${categoryId}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(tint.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ZyphuelBlueDark
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. SAVED VEHICLES STRIP (MY VEHICLES)
 */
@Composable
fun SavedVehiclesBar(
    selectedVehicle: VehicleEntity?,
    onOpenMyVehicles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenMyVehicles() }
            .testTag("my_vehicles_bar_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                val icon = when {
                    selectedVehicle == null -> Icons.Filled.DirectionsCar
                    selectedVehicle.vehicleType.contains("Bike", ignoreCase = true) -> Icons.Filled.TwoWheeler
                    selectedVehicle.isEv -> Icons.Filled.ElectricCar
                    else -> Icons.Filled.DirectionsCar
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(ZyphuelBlueSecondary.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    if (selectedVehicle != null) {
                        Text(
                            text = "${selectedVehicle.nickname} (${selectedVehicle.registrationNumber})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                        )
                        Text(
                            text = "${selectedVehicle.make} ${selectedVehicle.model} • ${selectedVehicle.fuelType}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    } else {
                        Text(
                            text = "Add Your Vehicle Profile",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBlueDark)
                        )
                        Text(
                            text = "Save your bike or car for 1-tap personalized service",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }
                }
            }

            Surface(
                color = ZyphuelBluePrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (selectedVehicle != null) "Switch" else "+ Add",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBluePrimary
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * 4. THE MAIN CATEGORIES GRID (7 FEATURED + ALL 10 EXPANDABLE)
 * Displays 7 primary categories prominently (with Fuel & Energy as a Hero Showcase Card),
 * and provides seamless 1-tap animated expansion or filter tabs to access all 10 categories.
 */
@Composable
fun CategoryGridSection(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Services, 1: Emergency & Fuel, 2: Care & Maintenance

    val livePetrol = viewModel?.petrolPrice?.collectAsState()?.value ?: 272.82f
    val liveDiesel = viewModel?.dieselPrice?.collectAsState()?.value ?: 273.40f
    val liveLpg = viewModel?.lpgGasPrice?.collectAsState()?.value ?: 241.43f

    val displayedCategories = when (selectedTab) {
        0 -> categories
        1 -> categories.filter { it.id in listOf("fuel_energy", "roadside_assistance", "battery_services", "tyres_wheels") }
        2 -> categories.filter { it.id in listOf("auto_repair", "auto_detailing", "lubricants_fluids", "ev_services", "water_delivery", "fleet_business") }
        else -> categories
    }

    val fuelCategory = categories.firstOrNull { it.id == "fuel_energy" }
    val showHeroFuel = (selectedTab == 0 || selectedTab == 1) && fuelCategory != null
    val remainingGridCategories = if (showHeroFuel) {
        displayedCategories.filter { it.id != "fuel_energy" }
    } else {
        displayedCategories
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Category Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("All Services") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZyphuelBluePrimary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Emergency & Fuel") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFDC2626),
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                label = { Text("Care & Maintenance") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZyphuelBlueDark,
                    selectedLabelColor = Color.White
                )
            )
        }

        // 1. Hero Showcase Card for Fuel & Energy
        if (showHeroFuel && fuelCategory != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick(fuelCategory) }
                    .testTag("category_card_fuel_energy"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalGasStation,
                                    contentDescription = "Fuel & Energy",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Fuel & Energy",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Surface(
                                        color = Color(0xFF10B981),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "OGRA LIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Doorstep petrol, diesel, octane & emergency fuel",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA7F3D0))
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Dynamic Live Rates Ticker Row (Pakistani OGRA Rates)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF047857).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Petrol", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD1FAE5), fontSize = 10.sp)
                                Text("Rs. ${String.format(java.util.Locale.US, "%.2f", livePetrol)}/L", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF047857).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Diesel", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD1FAE5), fontSize = 10.sp)
                                Text("Rs. ${String.format(java.util.Locale.US, "%.2f", liveDiesel)}/L", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF047857).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Gas", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD1FAE5), fontSize = 10.sp)
                                Text("Rs. ${String.format(java.util.Locale.US, "%.2f", liveLpg)}/Kg", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2-Column Grid for remaining or all categories
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val chunked = remainingGridCategories.chunked(2)
            chunked.forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowCategories.forEach { category ->
                        CategoryCard(
                            category = category,
                            onClick = { onCategoryClick(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowCategories.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}


@Composable
fun CategoryCard(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = CategoryIconHelper.getCategoryColor(category.id)

    // Only show badge for special categories or non-standard availability to eliminate clutter
    val specialBadge = when {
        category.id == "roadside_assistance" -> "24/7 SOS" to Color(0xFFDC2626)
        category.id == "fuel_energy" -> "OGRA" to Color(0xFF059669)
        category.availabilityStatus != CategoryAvailability.AVAILABLE -> category.availabilityStatus.label to Color(category.availabilityStatus.colorHex)
        else -> null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconHelper.getIcon(category.iconName),
                        contentDescription = category.name,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (specialBadge != null) {
                    Surface(
                        color = specialBadge.second.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = specialBadge.first,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = specialBadge.second,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ZyphuelBlueDark,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = category.shortDescription,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Gray,
                        lineHeight = 14.sp,
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${category.subcategories.size} Services",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                )
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * 5. COMPREHENSIVE CATEGORY DETAIL & BOOKING MODAL
 * Supports all 10 categories, fuel dynamic pricing, roadside SOS priorities, and real ETA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailModal(
    category: Category,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val deviceLat by viewModel.deviceLatitude.collectAsState()
    val deviceLng by viewModel.deviceLongitude.collectAsState()
    val liveLocationAddr by viewModel.liveLocationCoordinates.collectAsState()

    // Dynamic Live Rates from ViewModel (OGRA notified)
    val livePetrol by viewModel.petrolPrice.collectAsState()
    val liveDiesel by viewModel.dieselPrice.collectAsState()
    val liveOctane by viewModel.highOctanePrice.collectAsState()
    val liveLpg by viewModel.lpgGasPrice.collectAsState()

    val getEffectivePrice = remember(category.id, livePetrol, liveDiesel, liveOctane, liveLpg) {
        { sub: Subcategory ->
            if (category.id != "fuel_energy") {
                sub.basePrice
            } else {
                when (sub.id) {
                    "petrol_regular" -> livePetrol.toDouble()
                    "petrol_octane" -> liveOctane.toDouble()
                    "diesel_regular", "diesel_generator" -> liveDiesel.toDouble()
                    "lpg_sealed_cylinder" -> liveLpg.toDouble()
                    else -> sub.basePrice
                }
            }
        }
    }

    val coverage = remember(deviceLat, deviceLng, category.id) {
        LocationCoverageManager.checkCoverage(
            deviceLat,
            deviceLng,
            isEmergency = category.id == "roadside_assistance"
        )
    }

    var selectedSubcategory by remember { mutableStateOf<Subcategory?>(null) }
    var orderNotes by remember { mutableStateOf("") }
    var orderQuantity by remember { mutableIntStateOf(1) }
    var showMyVehicles by remember { mutableStateOf(false) }

    // Filter subcategories by selected vehicle if applicable
    var vehicleFilter by remember { mutableStateOf<VehicleType?>(null) }

    val filteredSubcategories = remember(category.subcategories, vehicleFilter) {
        if (vehicleFilter == null) category.subcategories
        else category.subcategories.filter { !it.requiresVehicle || it.supportedVehicleTypes.contains(vehicleFilter) }
    }

    // Group subcategories by serviceGroup
    val grouped = remember(filteredSubcategories) {
        filteredSubcategories.groupBy { it.serviceGroup ?: "SERVICES" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = CategoryIconHelper.getCategoryColor(category.id)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CategoryIconHelper.getIcon(category.iconName),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(category.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ZyphuelBlueDark)
                        Text(category.shortDescription, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Location & ETA Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (category.id == "roadside_assistance") Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (category.id == "roadside_assistance") Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (category.id == "roadside_assistance") Icons.Filled.Warning else Icons.Filled.NearMe,
                                contentDescription = null,
                                tint = if (category.id == "roadside_assistance") Color(0xFFDC2626) else Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                when (coverage) {
                                    is CoverageStatus.Covered -> {
                                        Text(
                                            text = "📍 Service Zone: ${coverage.zoneName} (Covered)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (category.id == "roadside_assistance") Color(0xFF991B1B) else Color(0xFF14532D)
                                        )
                                        Text(
                                            text = "${coverage.distanceKm} km from depot • $liveLocationAddr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    is CoverageStatus.OutOfZone -> {
                                        Text(
                                            text = "Service Area Limited",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFDC2626)
                                        )
                                        Text(
                                            text = coverage.message,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        if (category.id == "roadside_assistance") {
                            IconButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:+923001234567")
                                    }
                                    context.startActivity(dialIntent)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFDC2626), CircleShape)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = "Emergency Call", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Official OGRA Compliance & Live Fuel Rates Banner for Fuel & Energy
                if (category.id == "fuel_energy") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Official OGRA Notified Rates • Euro-V Standard",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF14532D)
                                )
                            }
                            Text(
                                "100% Calibrated Digital Flow-Meter • Anti-Adulteration Security Seal",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF166534)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(color = Color.White, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Color(0xFFBBF7D0))) {
                                    Text("Petrol: Rs. ${String.format(java.util.Locale.US, "%.2f", livePetrol)}/L", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                Surface(color = Color.White, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Color(0xFFBBF7D0))) {
                                    Text("Diesel: Rs. ${String.format(java.util.Locale.US, "%.2f", liveDiesel)}/L", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                Surface(color = Color.White, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Color(0xFFBBF7D0))) {
                                    Text("Octane: Rs. ${String.format(java.util.Locale.US, "%.2f", liveOctane)}/L", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                Surface(color = Color.White, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Color(0xFFBBF7D0))) {
                                    Text("Gas: Rs. ${String.format(java.util.Locale.US, "%.2f", liveLpg)}/Kg", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }

                // Selected Vehicle Pill
                Surface(
                    onClick = { showMyVehicles = true },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedVehicle?.let { "${it.nickname} (${it.registrationNumber})" } ?: "Assign a Vehicle (Optional)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ZyphuelBlueDark
                            )
                        }
                        Text("Change", style = MaterialTheme.typography.labelSmall, color = ZyphuelBluePrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // Vehicle Type Filter for Auto Repair & Detailing
                if (category.id == "auto_repair") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = vehicleFilter == null,
                            onClick = { vehicleFilter = null },
                            label = { Text("All Vehicles") }
                        )
                        VehicleType.entries.forEach { vType ->
                            FilterChip(
                                selected = vehicleFilter == vType,
                                onClick = { vehicleFilter = if (vehicleFilter == vType) null else vType },
                                label = { Text(vType.shortCode) }
                            )
                        }
                    }
                }

                // Subcategories Grouped List
                grouped.forEach { (groupName, subList) ->
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ZyphuelBluePrimary
                        ),
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    subList.forEach { subcat ->
                        val isChosen = selectedSubcategory?.id == subcat.id
                        val dynamicPrice = getEffectivePrice(subcat)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSubcategory = if (isChosen) null else subcat },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChosen) Color(0xFFEFF6FF) else Color.White
                            ),
                            border = BorderStroke(
                                width = if (isChosen) 2.dp else 1.dp,
                                color = if (isChosen) ZyphuelBluePrimary else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        RadioButton(
                                            selected = isChosen,
                                            onClick = { selectedSubcategory = if (isChosen) null else subcat },
                                            colors = RadioButtonDefaults.colors(selectedColor = ZyphuelBluePrimary)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = subcat.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ZyphuelBlueDark
                                            )
                                            Text(
                                                text = subcat.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (dynamicPrice > 0) {
                                            Text(
                                                text = "Rs. ${String.format(java.util.Locale.US, "%,.2f", dynamicPrice)}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ZyphuelBlueDark
                                            )
                                            Text(
                                                text = "per ${subcat.unit}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        } else {
                                            Text(
                                                text = "Custom",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ZyphuelBluePrimary
                                            )
                                        }
                                    }
                                }

                                if (subcat.operationalRestrictions != null) {
                                    Surface(
                                        color = Color(0xFFFFFBEB),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = subcat.operationalRestrictions,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color(0xFF92400E))
                                            )
                                        }
                                    }
                                }

                                if (subcat.badgeText != null) {
                                    Surface(
                                        color = if (subcat.isEmergency) Color(0xFFFEE2E2) else Color(0xFFECFDF5),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = subcat.badgeText,
                                            color = if (subcat.isEmergency) Color(0xFFDC2626) else Color(0xFF059669),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Booking Options (Notes, Presets & Quantity)
                if (selectedSubcategory != null) {
                    val activeSub = selectedSubcategory!!
                    val chosenPrice = getEffectivePrice(activeSub)

                    Divider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        "Booking Details & Volume",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = ZyphuelBlueDark
                    )

                    // Quick Volume Preset Chips for Fuel & LPG
                    if (activeSub.unit in listOf("L", "Kg")) {
                        val presets = if (activeSub.unit == "L") {
                            listOf(5 to "5 Litres", 10 to "10 Litres", 20 to "20 Litres", 30 to "30 Litres", 40 to "40 Litres", 50 to "Full (50L)")
                        } else {
                            listOf(12 to "11.8 Kg (1 Cylinder)", 24 to "23.6 Kg (2 Cylinders)")
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Quick Select Volume:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.forEach { (qty, label) ->
                                    FilterChip(
                                        selected = orderQuantity == qty,
                                        onClick = { orderQuantity = qty },
                                        label = { Text(label, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("preset_chip_${qty}")
                                    )
                                }
                            }
                        }
                    }

                    // Quantity Counter if applicable
                    if (activeSub.unit in listOf("L", "Kg", "Bottle", "Can", "Pack")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quantity (${activeSub.unit}):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (orderQuantity > 1) orderQuantity-- },
                                    modifier = Modifier.size(32.dp).background(Color(0xFFE2E8F0), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                                }
                                Text(" $orderQuantity ${activeSub.unit} ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(
                                    onClick = { orderQuantity++ },
                                    modifier = Modifier.size(32.dp).background(Color(0xFFE2E8F0), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = orderNotes,
                        onValueChange = { orderNotes = it },
                        label = { Text("Special instructions / Landmark / Vehicle details") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    val itemCost = chosenPrice * orderQuantity
                    val totalEst = itemCost + category.pricingConfig.deliveryFee + (if (activeSub.isEmergency) category.pricingConfig.emergencySurcharge else 0.0)

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Item/Fuel Subtotal (${orderQuantity} ${activeSub.unit} @ Rs. ${String.format(java.util.Locale.US, "%.2f", chosenPrice)}):", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text("Rs. ${String.format(java.util.Locale.US, "%,.2f", itemCost)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("OGRA-Compliant Doorstep Fee:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text("Rs. ${String.format(java.util.Locale.US, "%,.2f", category.pricingConfig.deliveryFee)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            if (activeSub.isEmergency) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Emergency Priority Surcharge:", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDC2626))
                                    Text("Rs. ${String.format(java.util.Locale.US, "%,.2f", category.pricingConfig.emergencySurcharge)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                            }
                            Divider(color = Color(0xFFE2E8F0))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total (Cash on Delivery):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Rs. ${String.format(java.util.Locale.US, "%,.2f", totalEst)}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ZyphuelBluePrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sub = selectedSubcategory ?: return@Button
                    val unitPrice = getEffectivePrice(sub)
                    viewModel.bookCategoryService(
                        subcategory = sub,
                        parentCategory = category,
                        vehicle = selectedVehicle,
                        notes = orderNotes,
                        deliveryAddress = liveLocationAddr,
                        quantity = orderQuantity,
                        isEmergency = sub.isEmergency,
                        unitPriceOverride = unitPrice,
                        onSuccess = { onDismiss() }
                    )
                },
                enabled = selectedSubcategory != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (category.id == "roadside_assistance") Color(0xFFDC2626) else ZyphuelBluePrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("modal_book_service_btn")
            ) {
                Text(
                    text = if (category.id == "roadside_assistance") "Request Emergency SOS 🚨" else "Confirm & Book Now",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    )

    if (showMyVehicles) {
        MyVehiclesDialog(viewModel = viewModel) {
            showMyVehicles = false
        }
    }
}

/**
 * 6. ADMIN CATEGORY & SERVICE MANAGEMENT TAB
 * Allows operations admins to toggle categories, subcategories, pricing, availability, and inspect operational zones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCategoryManagementTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    var selectedCategoryId by remember(categories) {
        mutableStateOf(categories.firstOrNull { it.isActive }?.id ?: categories.firstOrNull()?.id ?: "fuel_petrol")
    }
    val currentCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.firstOrNull()

    var expandedCatId by remember { mutableStateOf<String?>(null) }
    var editingFeeCatId by remember { mutableStateOf<String?>(null) }
    var feeInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Category & Service Operations Control", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = ZyphuelBlueDark)
                        Text("Manage availability, pricing, and service activation without republishing the app.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }

        // --- CURRENT CATEGORY OVERVIEW / SPOTLIGHT CARD ---
        if (currentCategory != null) {
            item {
                val accentColor = CategoryIconHelper.getCategoryColor(currentCategory.id)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("admin_current_category_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, ZyphuelBluePrimary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(currentCategory.iconName),
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = currentCategory.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ZyphuelBlueDark
                                        )
                                        Surface(
                                            color = if (currentCategory.isActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, if (currentCategory.isActive) Color(0xFF86EFAC) else Color(0xFFFCA5A5))
                                        ) {
                                            Text(
                                                text = if (currentCategory.isActive) "CURRENT ACTIVE 🎯" else "INACTIVE 🔴",
                                                color = if (currentCategory.isActive) Color(0xFF15803D) else Color(0xFFB91C1C),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "ID: ${currentCategory.id} • ${currentCategory.estimatedResponseTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            Switch(
                                checked = currentCategory.isActive,
                                onCheckedChange = { viewModel.toggleCategoryActive(currentCategory.id, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                            )
                        }

                        // Operational Details Row for Current Category
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Delivery Fee", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("Rs. ${String.format(java.util.Locale.US, "%.0f", currentCategory.pricingConfig.deliveryFee)}", fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("Subcategories", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("${currentCategory.subcategories.size} (${currentCategory.subcategories.count { it.isActive }} Active)", fontWeight = FontWeight.Bold, color = Color.Black, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("Status", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(currentCategory.availabilityStatus.label, fontWeight = FontWeight.Bold, color = Color(currentCategory.availabilityStatus.colorHex), style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("Coverage", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(currentCategory.locationCoverage.firstOrNull() ?: "Lahore", fontWeight = FontWeight.Bold, color = Color.Black, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        // Switch Current Category in Focus Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Switch Current Category in Focus:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(categories) { cat ->
                                    val isSelected = cat.id == currentCategory.id
                                    val catColor = CategoryIconHelper.getCategoryColor(cat.id)
                                    Surface(
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            expandedCatId = cat.id
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) ZyphuelBluePrimary else Color(0xFFF1F5F9),
                                        border = if (isSelected) BorderStroke(1.dp, ZyphuelBlueDark) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else catColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else Color.Black
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        items(categories) { cat ->
            val isExpanded = expandedCatId == cat.id
            val accentColor = CategoryIconHelper.getCategoryColor(cat.id)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, if (cat.isActive) Color(0xFFE2E8F0) else Color(0xFFFECACA)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(accentColor.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    cat.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ZyphuelBlueDark
                                )
                                Text(
                                    "${cat.subcategories.size} Subcategories • ${cat.estimatedResponseTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = cat.isActive,
                                onCheckedChange = { viewModel.toggleCategoryActive(cat.id, it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                            )
                        }
                    }

                    // Status & Fee Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Availability selector chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CategoryAvailability.entries.take(3).forEach { avail ->
                                val isSelected = cat.availabilityStatus == avail
                                Surface(
                                    onClick = { viewModel.updateCategoryAvailability(cat.id, avail) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(avail.colorHex).copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                    border = if (isSelected) BorderStroke(1.dp, Color(avail.colorHex)) else null
                                ) {
                                    Text(
                                        text = avail.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(avail.colorHex) else Color.Gray
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Fee info
                        Text(
                            text = "Fee: Rs. ${String.format(java.util.Locale.US, "%.0f", cat.pricingConfig.deliveryFee)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ZyphuelBluePrimary
                        )
                    }

                    // Toggle Subcategories Expand
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCatId = if (isExpanded) null else cat.id }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "Hide Subcategories ▲" else "Configure Subcategories (${cat.subcategories.size}) ▼",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZyphuelBluePrimary
                            )
                        )
                        Text(
                            text = "Coverage: ${cat.locationCoverage.firstOrNull() ?: "Lahore"}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp)
                        )
                    }

                    if (isExpanded) {
                        Divider(color = Color(0xFFE2E8F0))

                        // Edit fee section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = if (editingFeeCatId == cat.id) feeInput else cat.pricingConfig.deliveryFee.toInt().toString(),
                                onValueChange = {
                                    editingFeeCatId = cat.id
                                    feeInput = it
                                },
                                label = { Text("Delivery/Service Fee (PKR)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    val newFee = feeInput.toDoubleOrNull() ?: cat.pricingConfig.deliveryFee
                                    viewModel.updateCategoryPricing(cat.id, newFee)
                                    editingFeeCatId = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                            ) {
                                Text("Update Fee")
                            }
                        }

                        // Subcategory list with individual toggles
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            cat.subcategories.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            sub.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ZyphuelBlueDark
                                        )
                                        Text(
                                            "Rs. ${String.format(java.util.Locale.US, "%,.2f", sub.basePrice)} / ${sub.unit} • ${sub.estimatedDuration}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = sub.isActive,
                                        onCheckedChange = { viewModel.toggleSubcategoryActive(cat.id, sub.id, it) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
