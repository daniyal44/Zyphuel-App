package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.vehicle.VehicleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVehiclesDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val vehicles by viewModel.customerVehicles.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()

    var showAddForm by remember { mutableStateOf(vehicles.isEmpty()) }

    var nickname by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("Car") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("2023") }
    var regNumber by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("Petrol") }
    var isEv by remember { mutableStateOf(false) }
    var isDefault by remember { mutableStateOf(vehicles.isEmpty()) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var fuelDropdownExpanded by remember { mutableStateOf(false) }

    val vehicleTypes = listOf("Motorcycle / Bike", "Car", "SUV / 4x4", "Van", "Commercial Vehicle", "EV")
    val fuelTypes = listOf("Petrol", "Diesel", "High-Octane", "Electric", "Hybrid", "LPG")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = ZyphuelBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("My Vehicles", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ZyphuelBlueDark)
                }
                if (!showAddForm && vehicles.isNotEmpty()) {
                    IconButton(
                        onClick = { showAddForm = true },
                        modifier = Modifier.testTag("add_vehicle_plus_btn")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Add Vehicle", tint = ZyphuelBluePrimary)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showAddForm) {
                    Text(
                        "Add Vehicle Profile",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = ZyphuelBluePrimary
                    )

                    // Vehicle Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = vehicleType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("vehicle_type_dropdown"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            vehicleTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        vehicleType = type
                                        if (type == "EV") {
                                            isEv = true
                                            fuelType = "Electric"
                                        } else if (type.contains("Bike")) {
                                            fuelType = "Petrol"
                                        }
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Nickname
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Nickname (e.g. My Daily Car)") },
                        modifier = Modifier.fillMaxWidth().testTag("vehicle_nickname_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Make & Model Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = make,
                            onValueChange = { make = it },
                            label = { Text("Make (Toyota)") },
                            modifier = Modifier.weight(1f).testTag("vehicle_make_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model (Corolla)") },
                            modifier = Modifier.weight(1f).testTag("vehicle_model_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // Year & Registration No.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = yearStr,
                            onValueChange = { yearStr = it },
                            label = { Text("Year (2023)") },
                            modifier = Modifier.weight(1f).testTag("vehicle_year_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = regNumber,
                            onValueChange = { regNumber = it },
                            label = { Text("Plate (LEA-22-1234)") },
                            modifier = Modifier.weight(1.2f).testTag("vehicle_plate_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // Fuel Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = fuelDropdownExpanded,
                        onExpandedChange = { fuelDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = fuelType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fuel / Energy Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("vehicle_fuel_dropdown"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = fuelDropdownExpanded,
                            onDismissRequest = { fuelDropdownExpanded = false }
                        ) {
                            fuelTypes.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(f) },
                                    onClick = {
                                        fuelType = f
                                        isEv = (f == "Electric")
                                        fuelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isDefault,
                            onCheckedChange = { isDefault = it },
                            colors = CheckboxDefaults.colors(checkedColor = ZyphuelBluePrimary)
                        )
                        Text("Set as default active vehicle", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (vehicles.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showAddForm = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                        Button(
                            onClick = {
                                val cleanMake = make.trim().ifBlank { "Vehicle" }
                                val cleanModel = model.trim().ifBlank { "Standard" }
                                val year = yearStr.toIntOrNull() ?: 2022
                                val reg = regNumber.trim().ifBlank { "LAHORE-REG" }

                                viewModel.saveVehicle(
                                    nickname = nickname.trim().ifBlank { "$cleanMake $cleanModel" },
                                    vehicleType = vehicleType,
                                    make = cleanMake,
                                    model = cleanModel,
                                    year = year,
                                    registrationNumber = reg,
                                    fuelType = fuelType,
                                    isEv = isEv,
                                    isDefault = isDefault
                                )
                                showAddForm = false
                            },
                            modifier = Modifier.weight(1f).testTag("save_vehicle_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                        ) {
                            Text("Save Vehicle", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Text(
                        "Select a vehicle to personalize services and fuel orders:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vehicles) { vehicle ->
                            val isSelected = selectedVehicle?.id == vehicle.id

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectVehicle(vehicle) }
                                    .testTag("vehicle_item_${vehicle.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ZyphuelBluePrimary else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        val icon = when {
                                            vehicle.vehicleType.contains("Bike", ignoreCase = true) -> Icons.Filled.TwoWheeler
                                            vehicle.isEv -> Icons.Filled.ElectricCar
                                            vehicle.vehicleType.contains("Commercial", ignoreCase = true) -> Icons.Filled.LocalShipping
                                            else -> Icons.Filled.DirectionsCar
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(
                                                    if (isSelected) ZyphuelBluePrimary else Color(0xFFE0F2FE),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else ZyphuelBluePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    vehicle.nickname,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = ZyphuelBlueDark
                                                )
                                                if (vehicle.isDefault) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFDCFCE7)
                                                    ) {
                                                        Text(
                                                            "Default",
                                                            color = Color(0xFF15803D),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                "${vehicle.make} ${vehicle.model} • ${vehicle.year} • ${vehicle.registrationNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                "Fuel: ${vehicle.fuelType} ${if (vehicle.isEv) "⚡ EV" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ZyphuelBluePrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!vehicle.isDefault) {
                                            IconButton(
                                                onClick = { viewModel.setDefaultVehicle(vehicle.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.StarBorder,
                                                    contentDescription = "Set Default",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteVehicle(vehicle.id) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showAddForm = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_another_vehicle_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZyphuelBluePrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZyphuelBluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Another Vehicle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
