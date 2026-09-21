package com.example.data.category

/**
 * Supported vehicle categories across Zyphuel services.
 */
enum class VehicleType(val displayName: String, val shortCode: String) {
    BIKE("Motorcycle / Bike", "Bike"),
    CAR("Car", "Car"),
    SUV("SUV / 4x4", "SUV"),
    VAN("Van", "Van"),
    COMMERCIAL("Commercial Vehicle", "Commercial"),
    EV("Electric Vehicle (EV)", "EV");

    companion object {
        fun fromString(value: String?): VehicleType {
            if (value.isNullOrBlank()) return CAR
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.contains(value, ignoreCase = true) || 
                it.shortCode.equals(value, ignoreCase = true) 
            } ?: CAR
        }
    }
}

/**
 * Operational service types.
 */
enum class CategoryServiceType(val label: String) {
    ON_DEMAND_DELIVERY("On-Demand Delivery"),
    DOORSTEP_WORKSHOP("Doorstep & Workshop"),
    EMERGENCY_ROADSIDE("24/7 Emergency Roadside"),
    SCHEDULED_APPOINTMENT("Scheduled Appointment"),
    FLEET_B2B("Fleet & Enterprise")
}

/**
 * Availability state of each category/service.
 */
enum class CategoryAvailability(val label: String, val colorHex: Long) {
    AVAILABLE("Available Now", 0xFF10B981),      // Green
    BUSY("High Demand", 0xFFF59E0B),           // Amber
    COMING_SOON("Coming Soon", 0xFF64748B),      // Slate
    CLOSED("Closed", 0xFFEF4444),               // Red
    EMERGENCY_ONLY("Emergency Only", 0xFFDC2626) // Deep Red
}

/**
 * Pricing configuration for each service category.
 */
data class PricingConfig(
    val basePrice: Double = 0.0,
    val unitPrice: Double = 0.0,
    val deliveryFee: Double = 250.0,
    val emergencySurcharge: Double = 0.0,
    val unitLabel: String = "Service",
    val isDynamicFuelRate: Boolean = false
)

/**
 * Individual Subcategory or Service Item.
 */
data class Subcategory(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val serviceGroup: String? = null,
    val isActive: Boolean = true,
    val basePrice: Double = 0.0,
    val unit: String = "Service",
    val estimatedDuration: String = "20-30 mins",
    val requiresVehicle: Boolean = true,
    val supportedVehicleTypes: List<VehicleType> = listOf(VehicleType.BIKE, VehicleType.CAR, VehicleType.SUV, VehicleType.VAN, VehicleType.COMMERCIAL, VehicleType.EV),
    val operationalRestrictions: String? = null,
    val isEmergency: Boolean = false,
    val badgeText: String? = null
)

/**
 * Primary Top-Level Category Model.
 */
data class Category(
    val id: String,
    val name: String,
    val shortDescription: String,
    val iconName: String,
    val isActive: Boolean = true,
    val sortOrder: Int,
    val availabilityStatus: CategoryAvailability = CategoryAvailability.AVAILABLE,
    val serviceType: CategoryServiceType,
    val supportedVehicleTypes: List<VehicleType> = listOf(VehicleType.BIKE, VehicleType.CAR, VehicleType.SUV, VehicleType.VAN, VehicleType.COMMERCIAL, VehicleType.EV),
    val locationCoverage: List<String> = listOf("Lahore Central", "Gulberg", "DHA", "Model Town", "Johar Town", "Bahria Town", "Cantt"),
    val estimatedResponseTime: String = "15-30 mins",
    val pricingConfig: PricingConfig = PricingConfig(),
    val backendServiceMapping: String,
    val subcategories: List<Subcategory> = emptyList()
)

/**
 * Search result item for instant global service search.
 */
data class ServiceSearchResult(
    val subcategory: Subcategory,
    val parentCategory: Category,
    val matchRelevance: Int = 0
)

/**
 * Master Seed Data Factory representing the comprehensive 10-category Zyphuel architecture.
 */
object CategoryCatalogSeed {

    fun getDefaultCategories(): List<Category> {
        return listOf(
            // 1. FUEL & ENERGY
            Category(
                id = "fuel_energy",
                name = "Fuel & Energy",
                shortDescription = "Doorstep petrol, diesel & emergency fuel",
                iconName = "LocalGasStation",
                isActive = true,
                sortOrder = 1,
                availabilityStatus = CategoryAvailability.AVAILABLE,
                serviceType = CategoryServiceType.ON_DEMAND_DELIVERY,
                supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR, VehicleType.SUV, VehicleType.VAN, VehicleType.COMMERCIAL),
                locationCoverage = listOf("All Lahore Operational Zones", "Gulberg", "DHA", "Model Town", "Johar Town", "Bahria Town"),
                estimatedResponseTime = "Direct Dispatch",
                pricingConfig = PricingConfig(deliveryFee = 250.0, isDynamicFuelRate = true, unitLabel = "L"),
                backendServiceMapping = "fuel_delivery_v1",
                subcategories = listOf(
                    // Petrol (2 subcategories)
                    Subcategory("petrol_regular", "fuel_energy", "Regular Petrol", "Euro-V standard motor gasoline for daily commuting", "PETROL", true, 275.60, "L", "Direct Dispatch", badgeText = "OGRA Live"),
                    Subcategory("petrol_octane", "fuel_energy", "High-Octane Petrol", "HOBC 97-Octane for luxury, turbo & performance vehicles", "PETROL", true, 325.00, "L", "Direct Dispatch", badgeText = "97 Octane"),
                    // Diesel (2 subcategories)
                    Subcategory("diesel_regular", "fuel_energy", "Regular Diesel", "High-Speed Diesel for commercial & heavy vehicles", "DIESEL", true, 284.20, "L", "Direct Dispatch", badgeText = "OGRA Live"),
                    Subcategory("diesel_generator", "fuel_energy", "Generator Diesel", "Bulk & backup diesel delivery for home and commercial generators", "DIESEL", true, 284.20, "L", "Scheduled Dispatch", requiresVehicle = false),
                    // Gas (Only 1 Single Option - Temporarily Unavailable)
                    Subcategory("lpg_sealed_cylinder", "fuel_energy", "Gas Cylinder", "Certified factory-sealed 11.8kg cylinder with safety seal inspection", "GAS", false, 258.65, "Kg", "Direct Dispatch", requiresVehicle = false, badgeText = "Unavailable", operationalRestrictions = "Strictly OGRA & Civil Defence verified cylinders only.")
                )
            ),

            // 2. AUTO REPAIR & CARE
            Category(
                id = "auto_repair",
                name = "Auto Repair & Care",
                shortDescription = "Expert diagnosis, engine, brakes & maintenance",
                iconName = "Build",
                isActive = true,
                sortOrder = 2,
                availabilityStatus = CategoryAvailability.AVAILABLE,
                serviceType = CategoryServiceType.DOORSTEP_WORKSHOP,
                supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR, VehicleType.SUV, VehicleType.VAN, VehicleType.COMMERCIAL, VehicleType.EV),
                locationCoverage = listOf("Gulberg", "DHA", "Model Town", "Johar Town", "All Lahore"),
                estimatedResponseTime = "30-45 mins",
                pricingConfig = PricingConfig(deliveryFee = 350.0, unitLabel = "Service"),
                backendServiceMapping = "auto_repair_v1",
                subcategories = listOf(
                    // General Service
                    Subcategory("gen_inspection", "auto_repair", "General Inspection", "Comprehensive multi-point vehicle condition inspection", "GENERAL SERVICE", true, 1500.0, "Check", "45 mins"),
                    Subcategory("gen_preventive", "auto_repair", "Preventive Maintenance", "Fluid checks, belt tension, filter inspection & adjustments", "GENERAL SERVICE", true, 2500.0, "Service", "60 mins"),
                    Subcategory("gen_scheduled", "auto_repair", "Scheduled Service", "Manufacturer recommended 5,000 / 10,000 km routine service", "GENERAL SERVICE", true, 3500.0, "Service", "90 mins"),
                    Subcategory("gen_health_check", "auto_repair", "Vehicle Health Check", "Computerized scan and full system verification report", "GENERAL SERVICE", true, 2000.0, "Report", "30 mins"),
                    Subcategory("gen_expert_diagnosis", "auto_repair", "Expert Diagnosis", "Master technician in-depth mechanical fault finding", "GENERAL SERVICE", true, 2000.0, "Diagnosis", "45 mins"),
                    // Engine
                    Subcategory("eng_inspection", "auto_repair", "Engine Inspection", "Compression test, leak detection, and noise diagnostics", "ENGINE", true, 1800.0, "Service", "40 mins"),
                    Subcategory("eng_tuning", "auto_repair", "Engine Tuning", "Spark adjustment, timing, sensor calibration & idle setting", "ENGINE", true, 2800.0, "Tuning", "60 mins"),
                    Subcategory("eng_injector", "auto_repair", "Fuel Injector Service", "Ultrasonic cleaning & spray pattern testing for injectors", "ENGINE", true, 3500.0, "Service", "75 mins"),
                    Subcategory("eng_throttle", "auto_repair", "Throttle Body Cleaning", "Carbon removal from butterfly valve & sensor calibration", "ENGINE", true, 1500.0, "Service", "30 mins"),
                    Subcategory("eng_oil_change", "auto_repair", "Engine Oil Change", "Oil drain, genuine oil refill & washer seal replacement", "ENGINE", true, 1200.0, "Service", "25 mins"),
                    Subcategory("eng_oil_filter", "auto_repair", "Oil Filter Replacement", "OEM certified oil filter replacement to prevent wear", "ENGINE", true, 800.0, "Item", "20 mins"),
                    Subcategory("eng_air_filter", "auto_repair", "Air Filter Replacement", "High-flow air filter replacement for maximum engine efficiency", "ENGINE", true, 950.0, "Item", "15 mins"),
                    Subcategory("eng_spark_plug", "auto_repair", "Spark Plug Replacement", "Iridium / Copper plug inspection, gapping & renewal", "ENGINE", true, 1400.0, "Set", "30 mins"),
                    // Brakes
                    Subcategory("brk_inspection", "auto_repair", "Brake Inspection", "Lining thickness, brake lines, fluid check & wear assessment", "BRAKES", true, 800.0, "Check", "20 mins"),
                    Subcategory("brk_pads", "auto_repair", "Brake Pad Replacement", "Front or rear ceramic brake pad renewal with anti-squeal grease", "BRAKES", true, 2200.0, "Axle", "45 mins"),
                    Subcategory("brk_rotors", "auto_repair", "Brake Disc/Rotors", "Rotor surfacing, lip removal or complete rotor replacement", "BRAKES", true, 3000.0, "Pair", "60 mins"),
                    Subcategory("brk_fluid", "auto_repair", "Brake Fluid", "Complete hydraulic brake bleed and fresh DOT-4 fluid flush", "BRAKES", true, 1500.0, "Flush", "35 mins"),
                    Subcategory("brk_adjustment", "auto_repair", "Brake Adjustment", "Handbrake cable tightening and rear drum shoe calibration", "BRAKES", true, 1000.0, "Service", "25 mins"),
                    // Transmission
                    Subcategory("trans_oil", "auto_repair", "Gear Oil Change", "Drain and refill manual / differential gear lubricant", "TRANSMISSION", true, 2200.0, "Service", "35 mins"),
                    Subcategory("trans_inspection", "auto_repair", "Transmission Inspection", "Fluid condition, gear shift smoothness & solenoid check", "TRANSMISSION", true, 1500.0, "Check", "30 mins"),
                    Subcategory("trans_service", "auto_repair", "Transmission Service", "Automatic transmission filter, pan gasket & fluid replacement", "TRANSMISSION", true, 4500.0, "Service", "90 mins"),
                    Subcategory("trans_clutch_service", "auto_repair", "Clutch Service", "Clutch plate, pressure plate, and release bearing overhaul", "TRANSMISSION", true, 6000.0, "Job", "180 mins"),
                    Subcategory("trans_clutch_adj", "auto_repair", "Clutch Adjustment", "Pedal free-play adjustment and slave cylinder bleed", "TRANSMISSION", true, 1000.0, "Service", "25 mins"),
                    // Suspension & Steering
                    Subcategory("susp_inspection", "auto_repair", "Suspension Inspection", "Ball joint, control arm, stabilizer bar & damper check", "SUSPENSION & STEERING", true, 1200.0, "Check", "30 mins"),
                    Subcategory("susp_shocks", "auto_repair", "Shock Absorbers", "Front or rear hydraulic / gas damper strut replacement", "SUSPENSION & STEERING", true, 3500.0, "Pair", "90 mins"),
                    Subcategory("susp_bush", "auto_repair", "Bush Replacement", "Rubber / polyurethane control arm bushing pressing", "SUSPENSION & STEERING", true, 2800.0, "Set", "75 mins"),
                    Subcategory("susp_wheel_alignment", "auto_repair", "Wheel Alignment", "Computerized 3D toe, camber, and caster alignment", "SUSPENSION & STEERING", true, 1800.0, "Car", "40 mins"),
                    Subcategory("susp_steering", "auto_repair", "Steering Inspection", "Steering rack, tie rod ends & power steering pump test", "SUSPENSION & STEERING", true, 1200.0, "Check", "30 mins"),
                    // Electrical
                    Subcategory("elec_diag", "auto_repair", "Electrical Diagnostics", "Short circuit tracing, battery drain test & parasitic draw", "ELECTRICAL", true, 1800.0, "Diagnosis", "45 mins"),
                    Subcategory("elec_starter", "auto_repair", "Starter/Motor Issues", "Starter motor relay, solenoid & carbon brush servicing", "ELECTRICAL", true, 2500.0, "Repair", "60 mins"),
                    Subcategory("elec_alternator", "auto_repair", "Alternator", "Charging voltage test, diode pack & alternator refurbishment", "ELECTRICAL", true, 2800.0, "Repair", "60 mins"),
                    Subcategory("elec_fuse_wiring", "auto_repair", "Fuse & Wiring", "Blown fuse replacement, loom repair & heat-shrink insulating", "ELECTRICAL", true, 1200.0, "Job", "35 mins"),
                    Subcategory("elec_lights", "auto_repair", "Lights", "Headlight bulb, LED upgrade, tail lamp & indicator renewal", "ELECTRICAL", true, 800.0, "Service", "20 mins"),
                    Subcategory("elec_horn", "auto_repair", "Horn", "Horn relay wiring, disc horn replacement and tuning", "ELECTRICAL", true, 700.0, "Service", "20 mins"),
                    Subcategory("elec_ecu", "auto_repair", "ECU Diagnostics", "OBD-II live parameter scan, clear trouble codes & reset", "ELECTRICAL", true, 1500.0, "Scan", "25 mins"),
                    // AC & Cooling
                    Subcategory("ac_inspection", "auto_repair", "AC Inspection", "Pressure gauge reading, cooling efficiency & vent temp test", "AC & COOLING", true, 1000.0, "Check", "20 mins"),
                    Subcategory("ac_gas", "auto_repair", "AC Gas Service", "Vacuum test, leak check and R134a refrigerant charge", "AC & COOLING", true, 3800.0, "Recharge", "45 mins"),
                    Subcategory("ac_repair", "auto_repair", "AC Repair", "Compressor magnetic clutch, condenser or evaporator coil", "AC & COOLING", true, 5000.0, "Job", "120 mins"),
                    Subcategory("ac_radiator", "auto_repair", "Radiator Check", "Radiator fin cleaning, pressure cap test & core inspection", "AC & COOLING", true, 1000.0, "Check", "25 mins"),
                    Subcategory("ac_coolant_replace", "auto_repair", "Coolant Replacement", "Thermostat check, complete cooling circuit flush & fill", "AC & COOLING", true, 1800.0, "Flush", "40 mins"),
                    Subcategory("ac_overheating", "auto_repair", "Overheating Diagnosis", "Water pump, fan clutch, head gasket sniff test", "AC & COOLING", true, 2000.0, "Diagnosis", "45 mins"),
                    // Bike Services
                    Subcategory("bike_general", "auto_repair", "Bike General Service", "Washing, oil change, tuning, brake & chain adjustment", "BIKE SERVICES", true, 950.0, "Package", "45 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("bike_oil_change", "auto_repair", "Bike Oil Change", "Genuine 20W-40 / 10W-40 bike oil drain and refill", "BIKE SERVICES", true, 450.0, "Service", "15 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("bike_chain_adj", "auto_repair", "Chain Adjustment", "Drive chain tensioning, slack alignment & cleaning", "BIKE SERVICES", true, 300.0, "Service", "15 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("bike_chain_lube", "auto_repair", "Chain Lubrication", "High-tack O-ring chain spray lubrication", "BIKE SERVICES", true, 250.0, "Service", "10 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("bike_brake_service", "auto_repair", "Brake Service", "Drum shoe cleanup or disc pad replacement and cable oiling", "BIKE SERVICES", true, 400.0, "Service", "25 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("bike_battery", "auto_repair", "Battery", "Bike dry battery testing, terminal cleaning & replacement", "BIKE SERVICES", true, 500.0, "Check", "15 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("bike_puncture", "auto_repair", "Tyre/Puncture", "Tube patching, valve core replacement or tubeless plug", "BIKE SERVICES", true, 350.0, "Puncture", "20 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("bike_electrical", "auto_repair", "Electrical", "Bike wiring loom, headlight, flasher and switch repair", "BIKE SERVICES", true, 500.0, "Job", "30 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("bike_starting", "auto_repair", "Starting Problem", "Carburetor jet cleaning, CDI unit & spark ignition check", "BIKE SERVICES", true, 600.0, "Diagnosis", "30 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    // Car Services
                    Subcategory("car_general", "auto_repair", "Car General Service", "Bumper to bumper comprehensive maintenance package", "CAR SERVICES", true, 4500.0, "Package", "90 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN)),
                    Subcategory("car_oil_change", "auto_repair", "Car Oil Change", "Full synthetic oil change with OEM filter & inspection", "CAR SERVICES", true, 1500.0, "Service", "30 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN)),
                    Subcategory("car_brake_service", "auto_repair", "Brake Service", "4-wheel brake inspection, caliper slider greasing & fluid", "CAR SERVICES", true, 2500.0, "Service", "50 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN)),
                    Subcategory("car_battery_service", "auto_repair", "Battery", "CCA conductance test, charging system test & terminal service", "CAR SERVICES", true, 600.0, "Test", "15 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN)),
                    Subcategory("car_ac_service", "auto_repair", "AC", "Cabin pollen filter cleaning, condenser wash & gas top-up", "CAR SERVICES", true, 2800.0, "Service", "45 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN)),
                    Subcategory("car_engine_diag", "auto_repair", "Engine Diagnostics", "Live telemetry logging, cylinder misfire & sensor check", "CAR SERVICES", true, 1800.0, "Diagnosis", "35 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN)),
                    Subcategory("car_transmission", "auto_repair", "Transmission", "CVT / Automatic gearbox fluid level check & calibration", "CAR SERVICES", true, 2500.0, "Check", "40 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN)),
                    Subcategory("car_suspension", "auto_repair", "Suspension", "Road test, undercarriage inspection & torque check", "CAR SERVICES", true, 1500.0, "Check", "35 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN)),
                    Subcategory("car_electrical", "auto_repair", "Electrical", "Central locking, power windows & fuse box debugging", "CAR SERVICES", true, 1800.0, "Job", "45 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN)),
                    Subcategory("car_inspection", "auto_repair", "Inspection", "Detailed pre-purchase or periodic 120-point inspection", "CAR SERVICES", true, 3500.0, "Report", "60 mins", supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.VAN))
                )
            ),

            // 3. ROADSIDE ASSISTANCE (Emergency Priority)
            Category(
                id = "roadside_assistance",
                name = "Roadside Assistance",
                shortDescription = "24/7 Emergency SOS, jump start, flat tyre & towing",
                iconName = "CarCrash",
                isActive = true,
                sortOrder = 3,
                availabilityStatus = CategoryAvailability.AVAILABLE,
                serviceType = CategoryServiceType.EMERGENCY_ROADSIDE,
                supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR, VehicleType.SUV, VehicleType.VAN, VehicleType.COMMERCIAL, VehicleType.EV),
                locationCoverage = listOf("All Lahore 24/7", "Canal Road", "Ring Road", "Motorway M2 / M3 Junctions", "Gulberg", "DHA"),
                estimatedResponseTime = "10-20 mins (Emergency Priority)",
                pricingConfig = PricingConfig(deliveryFee = 500.0, emergencySurcharge = 200.0, unitLabel = "Emergency"),
                backendServiceMapping = "roadside_sos_v1",
                subcategories = listOf(
                    Subcategory("sos_fuel", "roadside_assistance", "Fuel Emergency", "Rapid 5L canister delivery to get you to the nearest pump", "EMERGENCY", true, 2000.0, "Delivery", "10-15 mins", isEmergency = true, badgeText = "Rapid SOS"),
                    Subcategory("sos_flat_tyre", "roadside_assistance", "Flat Tyre", "On-spot puncture repair or spare tyre swap on roadside", "EMERGENCY", true, 1200.0, "Assistance", "15-20 mins", isEmergency = true, badgeText = "24/7 SOS"),
                    Subcategory("sos_jump_start", "roadside_assistance", "Battery Jump Start", "High-amperage booster pack instant jump start", "EMERGENCY", true, 1000.0, "Jump", "10-15 mins", isEmergency = true, badgeText = "Instant Boost"),
                    Subcategory("sos_battery_replace", "roadside_assistance", "Battery Replacement", "Delivery and on-spot installation of brand new battery", "EMERGENCY", true, 1500.0, "Install", "20-30 mins", isEmergency = true),
                    Subcategory("sos_wont_start", "roadside_assistance", "Vehicle Won't Start", "On-road diagnosis for starter, fuel pump, or ignition failure", "EMERGENCY", true, 1500.0, "Diagnosis", "15-25 mins", isEmergency = true),
                    Subcategory("sos_minor_repair", "roadside_assistance", "Minor Roadside Repair", "Radiator hose leak, loose belt, fuse or quick mechanical fix", "EMERGENCY", true, 1800.0, "Repair", "20-30 mins", isEmergency = true),
                    Subcategory("sos_towing", "roadside_assistance", "Towing", "Flatbed or wheel-lift hydraulic tow truck to preferred workshop", "EMERGENCY", true, 4500.0, "Tow", "20-35 mins", isEmergency = true, badgeText = "Flatbed Tow"),
                    Subcategory("sos_recovery", "roadside_assistance", "Vehicle Recovery", "Ditch, mud, or post-collision winching and recovery", "EMERGENCY", true, 6000.0, "Recovery", "30-45 mins", isEmergency = true),
                    Subcategory("sos_lockout", "roadside_assistance", "Lockout Assistance", "Non-destructive professional door unlocking for keys locked inside", "EMERGENCY", true, 2000.0, "Unlock", "15-25 mins", isEmergency = true),
                    Subcategory("sos_mech_help", "roadside_assistance", "Emergency Mechanical Help", "Urgent mobile mechanic dispatch for sudden breakdown", "EMERGENCY", true, 2200.0, "Dispatch", "15-25 mins", isEmergency = true)
                )
            ),

            // 4. AUTO CARE & DETAILING
            Category(
                id = "auto_detailing",
                name = "Auto Care & Detailing",
                shortDescription = "Car & bike wash, interior cleaning, wax & ceramic",
                iconName = "CleaningServices",
                isActive = true,
                sortOrder = 4,
                availabilityStatus = CategoryAvailability.AVAILABLE,
                serviceType = CategoryServiceType.DOORSTEP_WORKSHOP,
                supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR, VehicleType.SUV, VehicleType.VAN),
                locationCoverage = listOf("Gulberg", "DHA", "Model Town", "Johar Town", "All Lahore"),
                estimatedResponseTime = "30-45 mins",
                pricingConfig = PricingConfig(deliveryFee = 200.0, unitLabel = "Detailing"),
                backendServiceMapping = "detailing_v1",
                subcategories = listOf(
                    Subcategory("wash_car", "auto_detailing", "Car Wash", "Doorstep foam pressure wash, wheel arch clean & dry wipe", "WASH", true, 1200.0, "Wash", "40 mins"),
                    Subcategory("wash_bike", "auto_detailing", "Bike Wash", "Pressure degreasing, foam soak, rim clean & chain shine", "WASH", true, 500.0, "Wash", "25 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("detail_exterior_clean", "auto_detailing", "Exterior Cleaning", "High-gloss shampoo wash, glass treatment & tyre dressing", "CLEANING", true, 1800.0, "Service", "50 mins"),
                    Subcategory("detail_interior_clean", "auto_detailing", "Interior Cleaning", "Deep vacuum, upholstery shampoo, AC vent steam & plastics dressing", "CLEANING", true, 3500.0, "Service", "75 mins"),
                    Subcategory("detail_full", "auto_detailing", "Full Detailing", "Comprehensive bumper-to-bumper deep detail inside and out", "DETAILING", true, 8500.0, "Package", "180 mins", badgeText = "Best Value"),
                    Subcategory("detail_interior", "auto_detailing", "Interior Detailing", "Leather conditioning, roof liner cleaning, stain removal & ozone odor elimination", "DETAILING", true, 5000.0, "Service", "120 mins"),
                    Subcategory("detail_exterior", "auto_detailing", "Exterior Detailing", "Clay bar decontamination, iron remover & 1-step machine glaze", "DETAILING", true, 5500.0, "Service", "120 mins"),
                    Subcategory("detail_polish", "auto_detailing", "Polish", "3-stage machine compounding to remove swirl marks and light scratches", "POLISH & WAX", true, 4500.0, "Job", "90 mins"),
                    Subcategory("detail_wax", "auto_detailing", "Wax", "High-carnauba hydrophobic protective wax coat", "POLISH & WAX", true, 2500.0, "Coat", "45 mins"),
                    Subcategory("detail_ceramic", "auto_detailing", "Ceramic Coating", "9H Nano-ceramic quartz paint protection with 2-year warranty", "COATINGS", true, 28000.0, "Package", "360 mins", badgeText = "9H Quartz"),
                    Subcategory("detail_paint_protection", "auto_detailing", "Paint Protection", "Paint sealant barrier against UV, bird drops & road grit", "COATINGS", true, 12000.0, "Application", "180 mins"),
                    Subcategory("detail_headlight", "auto_detailing", "Headlight Restoration", "Wet-sand yellow oxidation removal and UV clear sealant", "SPECIALTY", true, 2200.0, "Pair", "40 mins"),
                    Subcategory("detail_vacuum", "auto_detailing", "Vacuum Service", "Deep carpet, boot space and seat crevice vacuuming", "CLEANING", true, 1000.0, "Service", "30 mins"),
                    Subcategory("detail_dashboard", "auto_detailing", "Dashboard Cleaning", "Matte finish UV protective dressing & air vent dusting", "CLEANING", true, 800.0, "Service", "20 mins")
                )
            ),

            // 5. TYRES & WHEELS
            Category(
                id = "tyres_wheels",
                name = "Tyres & Wheels",
                shortDescription = "Puncture repair, replacement, balancing & rotation",
                iconName = "TireRepair",
                isActive = true,
                sortOrder = 5,
                availabilityStatus = CategoryAvailability.AVAILABLE,
                serviceType = CategoryServiceType.ON_DEMAND_DELIVERY,
                supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR, VehicleType.SUV, VehicleType.COMMERCIAL),
                locationCoverage = listOf("All Lahore", "Gulberg", "DHA", "Model Town", "Johar Town"),
                estimatedResponseTime = "20-30 mins",
                pricingConfig = PricingConfig(deliveryFee = 250.0, unitLabel = "Job"),
                backendServiceMapping = "tyres_v1",
                subcategories = listOf(
                    Subcategory("tyre_puncture", "tyres_wheels", "Tyre Puncture", "Doorstep or roadside tubeless strip puncture repair", "REPAIR", true, 600.0, "Puncture", "20 mins", badgeText = "Quick Fix"),
                    Subcategory("tyre_emergency_repair", "tyres_wheels", "Emergency Tyre Repair", "Urgent on-spot tube change, patch or spare wheel fitment", "REPAIR", true, 1000.0, "Repair", "15 mins", isEmergency = true, badgeText = "SOS Priority"),
                    Subcategory("tyre_replacement", "tyres_wheels", "Tyre Replacement", "Delivery and fitment of new tyre from approved brands", "REPLACEMENT", true, 1200.0, "Tyre", "35 mins"),
                    Subcategory("tyre_delivery", "tyres_wheels", "Tyre Delivery", "Doorstep drop-off of brand-new OEM matching tyre", "REPLACEMENT", true, 500.0, "Delivery", "45 mins"),
                    Subcategory("tyre_rotation", "tyres_wheels", "Tyre Rotation", "4-wheel cross-rotation to ensure even tread wear", "MAINTENANCE", true, 1200.0, "Set", "30 mins"),
                    Subcategory("tyre_balancing", "tyres_wheels", "Wheel Balancing", "Dynamic spin balancing with precision lead weights", "MAINTENANCE", true, 1500.0, "Set", "40 mins"),
                    Subcategory("tyre_alignment", "tyres_wheels", "Wheel Alignment", "3D laser alignment to eliminate pull and uneven tyre wear", "MAINTENANCE", true, 1800.0, "Car", "40 mins"),
                    Subcategory("tyre_pressure", "tyres_wheels", "Tyre Pressure Check", "Digital PSI calibration and pure nitrogen top-up", "MAINTENANCE", true, 300.0, "Check", "10 mins"),
                    Subcategory("tyre_rim_service", "tyres_wheels", "Rim/Wheel Service", "Alloy bend removal, crack inspection & bead seal cleaning", "REPAIR", true, 2500.0, "Rim", "60 mins")
                )
            ),

            // 6. BATTERY SERVICES
            Category(
                id = "battery_services",
                name = "Battery Services",
                shortDescription = "Jump start, testing, replacement & fresh battery delivery",
                iconName = "BatteryChargingFull",
                isActive = true,
                sortOrder = 6,
                availabilityStatus = CategoryAvailability.AVAILABLE,
                serviceType = CategoryServiceType.ON_DEMAND_DELIVERY,
                supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR, VehicleType.SUV, VehicleType.COMMERCIAL),
                locationCoverage = listOf("All Lahore", "Gulberg", "DHA", "Johar Town", "Cantt"),
                estimatedResponseTime = "15-25 mins",
                pricingConfig = PricingConfig(deliveryFee = 250.0, unitLabel = "Service"),
                backendServiceMapping = "battery_v1",
                subcategories = listOf(
                    Subcategory("bat_jump_start", "battery_services", "Battery Jump Start", "Rapid mobile booster pack jump start at your location", "SERVICE", true, 1000.0, "Jump", "15 mins", isEmergency = true, badgeText = "SOS Quick Jump"),
                    Subcategory("bat_testing", "battery_services", "Battery Testing", "State of health (SOH), cranking voltage & alternator load test", "SERVICE", true, 500.0, "Test", "15 mins"),
                    Subcategory("bat_replacement", "battery_services", "Battery Replacement", "Doorstep delivery, terminal cleanup, installation & old battery scrap adjustment", "REPLACEMENT", true, 1000.0, "Installation", "30 mins", badgeText = "Doorstep Install"),
                    Subcategory("bat_delivery", "battery_services", "Battery Delivery", "Instant delivery of brand new sealed maintenance-free battery", "DELIVERY", true, 500.0, "Delivery", "30 mins"),
                    Subcategory("bat_car", "battery_services", "Car Battery", "AGM / MF 45Ah - 75Ah batteries for sedan, hatchback & crossover", "PRODUCTS", true, 14500.0, "Unit", "30 mins"),
                    Subcategory("bat_bike", "battery_services", "Bike Battery", "12V 4Ah - 9Ah dry batteries for Honda, Yamaha, Suzuki", "PRODUCTS", true, 3200.0, "Unit", "20 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("bat_commercial", "battery_services", "Commercial Battery", "Heavy-duty 100Ah - 200Ah batteries for pickups, trucks & buses", "PRODUCTS", true, 28000.0, "Unit", "45 mins", supportedVehicleTypes = listOf(VehicleType.COMMERCIAL)),
                    Subcategory("bat_charging", "battery_services", "Battery Charging", "Bench slow-charging and desulfation for deeply discharged batteries", "SERVICE", true, 1200.0, "Charge", "180 mins")
                )
            ),

            // 7. LUBRICANTS & FLUIDS
            Category(
                id = "lubricants_fluids",
                name = "Lubricants & Fluids",
                shortDescription = "Engine oil, transmission fluid, coolant & filters",
                iconName = "WaterDrop",
                isActive = true,
                sortOrder = 7,
                availabilityStatus = CategoryAvailability.AVAILABLE,
                serviceType = CategoryServiceType.ON_DEMAND_DELIVERY,
                supportedVehicleTypes = listOf(VehicleType.BIKE, VehicleType.CAR, VehicleType.SUV, VehicleType.COMMERCIAL),
                locationCoverage = listOf("All Lahore", "Gulberg", "DHA", "Model Town"),
                estimatedResponseTime = "25-35 mins",
                pricingConfig = PricingConfig(deliveryFee = 250.0, unitLabel = "Item"),
                backendServiceMapping = "lubricants_v1",
                subcategories = listOf(
                    Subcategory("lub_engine_oil", "lubricants_fluids", "Engine Oil", "API SP / SN Plus Full Synthetic (0W-20, 5W-30, 10W-40)", "OILS", true, 4200.0, "4L Can", "30 mins"),
                    Subcategory("lub_motorcycle_oil", "lubricants_fluids", "Motorcycle Oil", "JASO MA2 4T 20W-40 / 10W-40 premium bike lubricant", "OILS", true, 1100.0, "1L Bottle", "20 mins", supportedVehicleTypes = listOf(VehicleType.BIKE)),
                    Subcategory("lub_trans_oil", "lubricants_fluids", "Transmission Oil", "CVT-F / ATF WS synthetic fluid for smooth gear shifts", "FLUIDS", true, 6500.0, "4L Can", "30 mins"),
                    Subcategory("lub_gear_oil", "lubricants_fluids", "Gear Oil", "GL-5 80W-90 differential and manual gearbox oil", "OILS", true, 1800.0, "1L Can", "30 mins"),
                    Subcategory("lub_brake_fluid", "lubricants_fluids", "Brake Fluid", "DOT-4 high-performance synthetic hydraulic brake fluid", "FLUIDS", true, 750.0, "500ml", "20 mins"),
                    Subcategory("lub_coolant", "lubricants_fluids", "Coolant", "Long-life ethylene glycol pre-mixed 50/50 radiator coolant", "FLUIDS", true, 1400.0, "3L Bottle", "25 mins"),
                    Subcategory("lub_power_steering", "lubricants_fluids", "Power Steering Fluid", "Anti-wear hydraulic power steering fluid", "FLUIDS", true, 850.0, "Bottle", "20 mins"),
                    Subcategory("lub_grease", "lubricants_fluids", "Grease", "High-temperature lithium complex wheel bearing grease", "SPECIALTY", true, 600.0, "Tub", "20 mins"),
                    Subcategory("lub_oil_filters", "lubricants_fluids", "Oil Filters", "High-efficiency micronic OEM replacement oil filter", "FILTERS", true, 950.0, "Item", "20 mins"),
                    Subcategory("lub_air_filters", "lubricants_fluids", "Air Filters", "Pleated synthetic dry air filter element", "FILTERS", true, 1200.0, "Item", "20 mins")
                )
            ),

            // 8. EV SERVICES (Architecture-Ready)
            Category(
                id = "ev_services",
                name = "EV Services",
                shortDescription = "Mobile EV charging, station finder & EV assistance",
                iconName = "ElectricCar",
                isActive = true,
                sortOrder = 8,
                availabilityStatus = CategoryAvailability.AVAILABLE,
                serviceType = CategoryServiceType.ON_DEMAND_DELIVERY,
                supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.SUV, VehicleType.EV),
                locationCoverage = listOf("Gulberg", "DHA Phase 5 & 6", "Model Town", "Lahore Core"),
                estimatedResponseTime = "25-35 mins",
                pricingConfig = PricingConfig(deliveryFee = 400.0, unitLabel = "Session"),
                backendServiceMapping = "ev_services_v1",
                subcategories = listOf(
                    Subcategory("ev_charging", "ev_services", "EV Charging", "Emergency mobile DC fast-charge van rescue (adds 20-30 km range)", "CHARGING", true, 3500.0, "Boost", "30 mins", badgeText = "Mobile DC Boost"),
                    Subcategory("ev_station_finder", "ev_services", "Charging Station Finder", "Verified live charging network locator across Lahore (real stations only)", "NAVIGATION", true, 0.0, "Free", "Instant", requiresVehicle = false, operationalRestrictions = "Real-time network status verified via official partner APIs."),
                    Subcategory("ev_emergency_assist", "ev_services", "Emergency EV Assistance", "Flatbed transport with specialized EV zero-drag wheel dollies", "EMERGENCY", true, 5500.0, "Rescue", "25 mins", isEmergency = true),
                    Subcategory("ev_battery_diag", "ev_services", "EV Battery Diagnostics", "High-voltage state of health (SOH), cell balance & thermal telemetry scan", "DIAGNOSTICS", true, 4500.0, "Scan", "40 mins"),
                    Subcategory("ev_service", "ev_services", "EV Service", "Cooling loop inspection, 12V auxiliary battery & high-voltage isolator check", "MAINTENANCE", true, 5000.0, "Service", "60 mins"),
                    Subcategory("ev_tyre", "ev_services", "EV Tyre Service", "Sound-absorbing foam tyre puncture repair and high-torque fitment", "TYRES", true, 1500.0, "Service", "30 mins"),
                    Subcategory("ev_roadside", "ev_services", "EV Roadside Assistance", "12V system wake-up boost & computer reboot rescue", "EMERGENCY", true, 2000.0, "Assistance", "20 mins", isEmergency = true)
                )
            ),

            // 9. WATER DELIVERY (Temporarily Unavailable)
            Category(
                id = "water_delivery",
                name = "Water Delivery",
                shortDescription = "Purified mineral water, dispenser bottles & bulk tankers",
                iconName = "InvertColors",
                isActive = false,
                sortOrder = 9,
                availabilityStatus = CategoryAvailability.CLOSED,
                serviceType = CategoryServiceType.ON_DEMAND_DELIVERY,
                supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.VAN, VehicleType.COMMERCIAL),
                locationCoverage = listOf("All Lahore Residential & Commercial Zones"),
                estimatedResponseTime = "Temporarily Unavailable",
                pricingConfig = PricingConfig(deliveryFee = 50.0, unitLabel = "Unit"),
                backendServiceMapping = "water_v1",
                subcategories = listOf(
                    Subcategory("water_drinking", "water_delivery", "Drinking Water", "Certified 19-Litre multi-stage RO purified drinking water bottle", "BOTTLED", false, 180.0, "19L Bottle", "Unavailable", requiresVehicle = false, badgeText = "Unavailable"),
                    Subcategory("water_bottles", "water_delivery", "Water Bottles", "Pack of 12 x 1.5L premium mineral water bottles", "BOTTLED", false, 960.0, "Pack", "Unavailable", requiresVehicle = false, badgeText = "Unavailable"),
                    Subcategory("water_can", "water_delivery", "Water Can", "Portable 10-Litre food-grade handled container for events & trips", "CONTAINER", false, 120.0, "10L Can", "Unavailable", requiresVehicle = false, badgeText = "Unavailable"),
                    Subcategory("water_dispenser", "water_delivery", "Water Dispenser Supply", "Bi-weekly scheduled 4-bottle home or office refill plan", "SUBSCRIPTION", false, 700.0, "Bundle", "Unavailable", requiresVehicle = false, badgeText = "Unavailable"),
                    Subcategory("water_bulk", "water_delivery", "Bulk Water", "1,000 to 3,000 Litre clean water supply for overhead tanks", "BULK", false, 3200.0, "Tank", "Unavailable", requiresVehicle = false, badgeText = "Unavailable"),
                    Subcategory("water_tanker", "water_delivery", "Water Tanker", "5,000+ Litre high-capacity commercial and residential tanker dispatch", "BULK", false, 6500.0, "Tanker", "Unavailable", requiresVehicle = false, badgeText = "Unavailable", operationalRestrictions = "Dispatched via verified municipal water haulers.")
                )
            ),

            // 10. FLEET & BUSINESS
            Category(
                id = "fleet_business",
                name = "Fleet & Business",
                shortDescription = "Bulk fuel, recurring corporate delivery & fleet tracking",
                iconName = "Business",
                isActive = true,
                sortOrder = 10,
                availabilityStatus = CategoryAvailability.AVAILABLE,
                serviceType = CategoryServiceType.FLEET_B2B,
                supportedVehicleTypes = listOf(VehicleType.CAR, VehicleType.VAN, VehicleType.COMMERCIAL),
                locationCoverage = listOf("Lahore Industrial Zones", "Sundar", "Kot Lakhpat", "Commercial Plazas"),
                estimatedResponseTime = "Scheduled / Priority",
                pricingConfig = PricingConfig(deliveryFee = 0.0, unitLabel = "B2B"),
                backendServiceMapping = "fleet_b2b_v1",
                subcategories = listOf(
                    Subcategory("fleet_fuel_delivery", "fleet_business", "Fleet Fuel Delivery", "On-site bowser fueling directly into company fleet parking lots", "FUELING", true, 280.50, "L", "Scheduled"),
                    Subcategory("fleet_bulk_diesel", "fleet_business", "Bulk Diesel", "Industrial grade 500L+ high-speed diesel delivery for logistics hubs", "FUELING", true, 280.50, "L", "Scheduled"),
                    Subcategory("fleet_generator_fuel", "fleet_business", "Generator Fuel", "Scheduled automated refueling contracts for towers & plazas", "FUELING", true, 280.50, "L", "Contract"),
                    Subcategory("fleet_corp_service", "fleet_business", "Corporate Vehicle Service", "Routine on-site maintenance packages for company employee cars", "MAINTENANCE", true, 3500.0, "Vehicle", "Custom"),
                    Subcategory("fleet_maintenance", "fleet_business", "Fleet Maintenance", "Contractual quarterly fleet inspections & brake/oil overhauls", "MAINTENANCE", true, 15000.0, "Package", "Scheduled"),
                    Subcategory("fleet_recurring", "fleet_business", "Recurring Fuel Orders", "Automated weekly / monthly recurring scheduled delivery runs", "AUTOMATION", true, 0.0, "Plan", "Recurring"),
                    Subcategory("fleet_business_account", "fleet_business", "Business Account", "Corporate credit line, multi-tier billing & dedicated account manager", "ACCOUNT", true, 0.0, "Registration", "Instant", requiresVehicle = false),
                    Subcategory("fleet_monthly_billing", "fleet_business", "Monthly Billing", "Consolidated 30-day corporate invoicing with tax documentation", "BILLING", true, 0.0, "Invoice", "Monthly", requiresVehicle = false),
                    Subcategory("fleet_tracking", "fleet_business", "Fleet Tracking", "Real-time dispatch telemetry dashboard for business logistics teams", "PORTAL", true, 0.0, "Access", "Live", requiresVehicle = false),
                    Subcategory("fleet_multi_vehicle", "fleet_business", "Multiple Vehicle Management", "Centralized portal to register and manage 100+ company vehicles", "MANAGEMENT", true, 0.0, "Portal", "Instant", requiresVehicle = false)
                )
            )
        )
    }
}
