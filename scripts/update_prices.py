import os

def update_file(path, replacements):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements:
        if old in content:
            content = content.replace(old, new)
            print(f'Replaced in {path}: {old[:30]} -> {new[:30]}')
        else:
            print(f'NOT found in {path}: {old[:40]}')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

# 1. CategoryModels.kt
cm_path = os.path.join('app', 'src', 'main', 'java', 'com', 'example', 'data', 'category', 'CategoryModels.kt')
update_file(cm_path, [
    ('Subcategory("petrol_regular", "fuel_energy", "Super Euro-V Petrol", "Certified Euro-V motor gasoline • 5L to 15L Max • Rs. 300–400 tiered delivery", "PETROL", true, 330.00, "L", "Direct Dispatch", badgeText = "CURRENT LIVE")',
     'Subcategory("petrol_regular", "fuel_energy", "Super Euro-V Petrol", "Certified Euro-V motor gasoline • 5L to 15L Max • Rs. 300–400 tiered delivery", "PETROL", true, 327.00, "L", "Direct Dispatch", badgeText = "CURRENT LIVE")'),
    ('Subcategory("diesel_regular", "fuel_energy", "High-Speed Euro-V Diesel", "Euro-V High-Speed Diesel for SUVs, pickups & commercial vehicles • 5L to 15L Max", "DIESEL", true, 336.00, "L", "Direct Dispatch", badgeText = "CURRENT LIVE")',
     'Subcategory("diesel_regular", "fuel_energy", "High-Speed Euro-V Diesel", "Euro-V High-Speed Diesel for SUVs, pickups & commercial vehicles • 5L to 15L Max", "DIESEL", true, 360.00, "L", "Direct Dispatch", badgeText = "CURRENT LIVE")'),
    ('Subcategory("diesel_generator", "fuel_energy", "Generator Diesel", "Backup Euro-V diesel delivery for home and commercial generators • Max 15L", "DIESEL", true, 336.00, "L", "Scheduled Dispatch", requiresVehicle = false, badgeText = "CURRENT LIVE")',
     'Subcategory("diesel_generator", "fuel_energy", "Generator Diesel", "Backup Euro-V diesel delivery for home and commercial generators • Max 15L", "DIESEL", true, 360.00, "L", "Scheduled Dispatch", requiresVehicle = false, badgeText = "CURRENT LIVE")'),
    ('Subcategory("fleet_fuel_delivery", "fleet_business", "Fleet Fuel Delivery", "On-site bowser fueling directly into company fleet parking lots", "FUELING", true, 330.00, "L", "Scheduled")',
     'Subcategory("fleet_fuel_delivery", "fleet_business", "Fleet Fuel Delivery", "On-site bowser fueling directly into company fleet parking lots", "FUELING", true, 327.00, "L", "Scheduled")'),
    ('Subcategory("fleet_bulk_diesel", "fleet_business", "Bulk Diesel", "Industrial grade 500L+ high-speed diesel delivery for logistics hubs", "FUELING", true, 336.00, "L", "Scheduled")',
     'Subcategory("fleet_bulk_diesel", "fleet_business", "Bulk Diesel", "Industrial grade 500L+ high-speed diesel delivery for logistics hubs", "FUELING", true, 360.00, "L", "Scheduled")'),
])

# 2. CategoryComponents.kt
cc_path = os.path.join('app', 'src', 'main', 'java', 'com', 'example', 'ui', 'CategoryComponents.kt')
update_file(cc_path, [
    ('val livePetrol = viewModel?.petrolPumpPrice?.collectAsState()?.value ?: 330.00f',
     'val livePetrol = viewModel?.petrolPumpPrice?.collectAsState()?.value ?: 327.00f'),
    ('val liveDiesel = viewModel?.dieselPumpPrice?.collectAsState()?.value ?: 336.00f',
     'val liveDiesel = viewModel?.dieselPumpPrice?.collectAsState()?.value ?: 360.00f'),
])

# 3. FuelPriceWorker.kt
fpw_path = os.path.join('app', 'src', 'main', 'java', 'com', 'example', 'worker', 'FuelPriceWorker.kt')
update_file(fpw_path, [
    ('val oldPetrol = sharedPrefs.getFloat("petrol", 330.00f)',
     'val oldPetrol = sharedPrefs.getFloat("petrol", 327.00f)'),
    ('val oldDiesel = sharedPrefs.getFloat("diesel", 0f)',
     'val oldDiesel = sharedPrefs.getFloat("diesel", 360.00f)'),
    ('val oldOctane = sharedPrefs.getFloat("high_octane", 0f)',
     'val oldOctane = sharedPrefs.getFloat("high_octane", 350.00f)'),
])

# 4. iOS ZyphuelConfig.swift
ios_cfg = os.path.join('ios', 'Zyphuel', 'Core', 'Config', 'ZyphuelConfig.swift')
update_file(ios_cfg, [
    ('static let defaultPetrolRate = 330.00', 'static let defaultPetrolRate = 327.00'),
    ('static let defaultDieselRate = 336.00', 'static let defaultDieselRate = 360.00'),
    ('static let defaultHighOctaneRate = 350.00', 'static let defaultHighOctaneRate = 350.00'),
    ('/// `petrol` 330.00, `diesel` 336.00, `high_octane` 350.00)',
     '/// `petrol` 327.00, `diesel` 360.00, `high_octane` 350.00)')
])

# 5. iOS Models.swift
ios_models = os.path.join('ios', 'Zyphuel', 'Core', 'Models', 'Models.swift')
update_file(ios_models, [
    ('group: "PETROL", isActive: true, basePrice: 330.00, unit: "L",',
     'group: "PETROL", isActive: true, basePrice: 327.00, unit: "L",'),
    ('group: "DIESEL", isActive: true, basePrice: 336.00, unit: "L",',
     'group: "DIESEL", isActive: true, basePrice: 360.00, unit: "L",'),
])

# 6. iOS AdminDashboardView.swift
ios_admin = os.path.join('ios', 'Zyphuel', 'Features', 'Admin', 'AdminDashboardView.swift')
update_file(ios_admin, [
    ('LabeledField(label: "Super Euro-V Petrol", placeholder: "330.00", text: $petrol,',
     'LabeledField(label: "Super Euro-V Petrol", placeholder: "327.00", text: $petrol,'),
    ('LabeledField(label: "High-Speed Diesel", placeholder: "336.00", text: $diesel,',
     'LabeledField(label: "High-Speed Diesel", placeholder: "360.00", text: $diesel,'),
])
