import Foundation

// MARK: - Roles

enum UserRole: String, Codable, CaseIterable, Identifiable {
    case customer
    case rider
    case admin

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .customer: return "Customer"
        case .rider: return "Rider"
        case .admin: return "Admin"
        }
    }
}

enum AuthProvider: String, Codable {
    case email = "Email"
    case google = "Google"
}

// MARK: - User

/// Mirrors `UserEntity` in Models.kt. Firestore documents may contain extra rider
/// verification fields written by Android clients; they are preserved in `extra`.
struct AppUser: Codable, Identifiable, Equatable {
    var email: String
    var name: String
    var phoneNumber: String
    var role: UserRole
    var isVerified: Bool
    var hasRequestedVerification: Bool
    var authProvider: AuthProvider
    var profilePictureBase64: String?
    var vehicleType: String?
    var vehicleNo: String?
    var riderNumber: Int?
    var riderId: String?
    var approvalStatus: String
    var accountStatus: String
    var isSuperAdmin: Bool
    var createdAt: Date
    var updatedAt: Date

    /// Fields iOS does not edit but must not drop when it writes the document back.
    var extra: [String: String]

    var id: String { email.lowercased() }

    var isAdmin: Bool { role == .admin || isSuperAdmin }
    var isActiveRider: Bool { role == .rider && isVerified }
    var displayInitials: String {
        let parts = name.split(separator: " ").prefix(2)
        let initials = parts.compactMap { $0.first }.map(String.init).joined()
        return initials.isEmpty ? "Z" : initials.uppercased()
    }

    static func empty(role: UserRole = .customer) -> AppUser {
        AppUser(
            email: "",
            name: "",
            phoneNumber: "",
            role: role,
            isVerified: false,
            hasRequestedVerification: false,
            authProvider: .email,
            profilePictureBase64: nil,
            vehicleType: nil,
            vehicleNo: nil,
            riderNumber: nil,
            riderId: nil,
            approvalStatus: "Pending",
            accountStatus: "Active",
            isSuperAdmin: false,
            createdAt: Date(),
            updatedAt: Date(),
            extra: [:]
        )
    }

    static func isSuperAdminEmail(_ email: String) -> Bool {
        email.compare(ZyphuelConfig.masterAdminEmail, options: .caseInsensitive) == .orderedSame
    }
}

// MARK: - Orders

enum OrderStatus: String, Codable, CaseIterable, Identifiable {
    case pending = "Pending"
    case assigned = "Assigned"
    case delivering = "Delivering"
    case completed = "Completed"
    case cancelled = "Cancelled"

    var id: String { rawValue }

    /// Statuses the Android app groups under "Current Order".
    var isCurrent: Bool {
        switch self {
        case .pending, .assigned, .delivering: return true
        case .completed, .cancelled: return false
        }
    }

    var stepIndex: Int {
        switch self {
        case .pending: return 0
        case .assigned: return 1
        case .delivering: return 2
        case .completed: return 3
        case .cancelled: return 0
        }
    }

    var iconName: String {
        switch self {
        case .pending: return "clock"
        case .assigned: return "person.badge.clock"
        case .delivering: return "truck.box"
        case .completed: return "checkmark.seal"
        case .cancelled: return "xmark.octagon"
        }
    }
}

struct FuelOrder: Codable, Identifiable, Equatable {
    var id: String
    var customerEmail: String
    var customerName: String
    var customerPhone: String
    var serviceType: String
    var quantityLiters: Int
    var unitPrice: Double
    var deliveryFee: Double
    var totalPrice: Double
    var deliveryAddress: String
    var paymentMethod: String
    var status: OrderStatus
    var riderEmail: String?
    var riderName: String?
    var createdAt: Date
    var etaMinutes: Int
    var rating: Int?
    var feedback: String?
    var destLat: Double?
    var destLng: Double?
    var originLat: Double?
    var originLng: Double?

    var isRated: Bool { rating != nil }

    /// Same 2-decimal convention as `formatPrice` on Android.
    var totalPriceLabel: String { MoneyFormatter.pkr(totalPrice) }

    static func draft(
        user: AppUser,
        serviceType: String,
        liters: Int,
        unitPrice: Double,
        address: String
    ) -> FuelOrder {
        let fee = PricingEngine.deliveryFee(liters: liters)
        return FuelOrder(
            id: UUID().uuidString,
            customerEmail: user.email,
            customerName: user.name.isEmpty ? "Zyphuel Customer" : user.name,
            customerPhone: user.phoneNumber,
            serviceType: serviceType,
            quantityLiters: liters,
            unitPrice: unitPrice,
            deliveryFee: fee,
            totalPrice: (unitPrice * Double(liters)) + fee,
            deliveryAddress: address,
            paymentMethod: "Cash on Delivery",
            status: .pending,
            riderEmail: nil,
            riderName: nil,
            createdAt: Date(),
            etaMinutes: 30,
            rating: nil,
            feedback: nil,
            destLat: nil,
            destLng: nil,
            originLat: nil,
            originLng: nil
        )
    }
}

enum MoneyFormatter {
    static func pkr(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        formatter.locale = Locale(identifier: "en_PK")
        let number = formatter.string(from: NSNumber(value: value)) ?? String(format: "%.2f", value)
        return "Rs. \(number)"
    }
}

// MARK: - Feedback, audit, notifications

struct DeliveryFeedback: Codable, Identifiable, Equatable {
    var id: String
    var orderId: String
    var customerEmail: String
    var rating: Int
    var comment: String
    var riderEmail: String?
    var createdAt: Date
}

struct AuditEntry: Codable, Identifiable, Equatable {
    var id: String
    var action: String
    var performedBy: String
    var details: String
    var timestamp: Date
}

struct AppNotification: Codable, Identifiable, Equatable {
    var id: String
    var title: String
    var message: String
    var timestamp: Date
    var targetRole: String
    var isRead: Bool
}

struct SavedAddress: Codable, Identifiable, Equatable {
    var id: String
    var label: String
    var address: String
    var latitude: Double
    var longitude: Double
    var isPrimary: Bool
}

// MARK: - Live config from system_config

struct FuelRates: Codable, Equatable {
    var petrol: Double
    var diesel: Double
    var highOctane: Double
    var lpg: Double
    var updatedAt: Date?

    static let fallback = FuelRates(
        petrol: ZyphuelConfig.defaultPetrolRate,
        diesel: ZyphuelConfig.defaultDieselRate,
        highOctane: ZyphuelConfig.defaultHighOctaneRate,
        lpg: ZyphuelConfig.defaultLpgRate,
        updatedAt: nil
    )
}

struct AppVersionInfo: Codable, Equatable {
    var latestVersionCode: Int
    var latestVersionName: String
    var downloadURL: String?
    var mandatory: Bool

    static let placeholder = AppVersionInfo(
        latestVersionCode: ZyphuelConfig.buildNumber,
        latestVersionName: ZyphuelConfig.marketingVersion,
        downloadURL: nil,
        mandatory: false
    )

    /// True when the published build (code or name) is ahead of the running app.
    var isOutdated: Bool {
        if latestVersionCode > ZyphuelConfig.buildNumber { return true }
        return Self.isVersion(latestVersionName, newerThan: ZyphuelConfig.marketingVersion)
    }

    /// Compares dotted numeric version strings such as "2.6.4.0.0.40" vs "2.6.4.0.0.39".
    /// Missing trailing segments count as zero, so a blank published name is never "newer".
    static func isVersion(_ candidate: String, newerThan reference: String) -> Bool {
        let candidateParts = candidate.split(separator: ".").map { Int($0) ?? 0 }
        let referenceParts = reference.split(separator: ".").map { Int($0) ?? 0 }
        for index in 0..<max(candidateParts.count, referenceParts.count) {
            let lhs = index < candidateParts.count ? candidateParts[index] : 0
            let rhs = index < referenceParts.count ? referenceParts[index] : 0
            if lhs != rhs { return lhs > rhs }
        }
        return false
    }
}

struct EmailGatewayConfig: Codable, Equatable {
    var webhookURL: String?
    var senderEmail: String?
    /// Shared with Android in `system_config/email_gateway`. iOS itself delivers through the
    /// HTTPS relay and the Firestore mail trigger, but it keeps the same credential record so
    /// an owner rotation made on Android reaches every client.
    var appPassword: String? = nil
    var isConfigured: Bool { (webhookURL?.isEmpty == false) }
}

// MARK: - Service catalog

enum ServiceAvailability: String, Codable, CaseIterable {
    case available = "Available Now"
    case busy = "High Demand"
    case comingSoon = "Coming Soon"
    case closed = "Closed"
    case emergencyOnly = "Emergency Only"
}

struct ServiceItem: Identifiable, Codable, Hashable {
    let id: String
    let categoryId: String
    let name: String
    let detail: String
    let group: String?
    let isActive: Bool
    let basePrice: Double
    let unit: String
    let eta: String
    let badge: String?
    let isEmergency: Bool
}

struct ServiceCategory: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let summary: String
    let symbolName: String
    let sortOrder: Int
    let availability: ServiceAvailability
    let isActive: Bool
    let items: [ServiceItem]
}

/// Mirrors `CategoryCatalogSeed` on Android. Live admin-managed catalog documents in
/// Firestore take precedence; this seed keeps the iOS app usable offline and on first run.
enum ServiceCatalog {

    static let all: [ServiceCategory] = [
        fuel,
        autoRepair,
        roadside,
        autoDetailing,
        tyres,
        battery,
        lubricants,
        ev,
        fleet,
        waterDelivery
    ]

    /// The 7 featured categories shown by default on the Android home screen.
    static let featured: [ServiceCategory] = Array(all.prefix(7))

    static var activeItems: [ServiceItem] {
        all.flatMap { $0.items }.filter { $0.isActive }
    }

    static func category(id: String) -> ServiceCategory? {
        all.first { $0.id == id }
    }

    /// Referenced from the home screen's primary "Order Now" action.
    static let fuel = ServiceCategory(
        id: "fuel_energy",
        name: "Fuel & Energy",
        summary: "Super Euro-V Petrol, High-Speed Diesel & High-Octane 97 (Max 15L • Rs. 300–400 tiered fee)",
        symbolName: "fuelpump.fill",
        sortOrder: 1,
        availability: .available,
        isActive: true,
        items: [
            ServiceItem(id: "petrol_regular", categoryId: "fuel_energy", name: "Super Euro-V Petrol",
                        detail: "Certified Euro-V motor gasoline • 5L to 15L max • Rs. 300–400 tiered delivery",
                        group: "PETROL", isActive: true, basePrice: 326.17, unit: "L",
                        eta: "Direct Dispatch", badge: "CURRENT LIVE", isEmergency: false),
            ServiceItem(id: "petrol_octane", categoryId: "fuel_energy", name: "High-Octane (RON 97)",
                        detail: "HOBC 97-Octane for luxury, turbo & performance vehicles • 5L to 15L max",
                        group: "PETROL", isActive: true, basePrice: 355.00, unit: "L",
                        eta: "Direct Dispatch", badge: "CURRENT LIVE", isEmergency: false),
            ServiceItem(id: "diesel_regular", categoryId: "fuel_energy", name: "High-Speed Euro-V Diesel",
                        detail: "Euro-V diesel for SUVs, pickups & commercial vehicles • 5L to 15L max",
                        group: "DIESEL", isActive: true, basePrice: 340.86, unit: "L",
                        eta: "Direct Dispatch", badge: "CURRENT LIVE", isEmergency: false),
            ServiceItem(id: "diesel_generator", categoryId: "fuel_energy", name: "Generator Diesel",
                        detail: "Backup Euro-V diesel for home and commercial generators • max 15L",
                        group: "DIESEL", isActive: true, basePrice: 340.86, unit: "L",
                        eta: "Scheduled Dispatch", badge: "CURRENT LIVE", isEmergency: false),
            ServiceItem(id: "water_pure_blocked", categoryId: "fuel_energy", name: "Pure Water Delivery (Unavailable)",
                        detail: "Currently unavailable across all order flows",
                        group: "UNAVAILABLE", isActive: false, basePrice: 0, unit: "Can",
                        eta: "Unavailable", badge: "UNAVAILABLE", isEmergency: false),
            ServiceItem(id: "lpg_cylinder_blocked", categoryId: "fuel_energy", name: "LPG Gas Cylinders (Unavailable)",
                        detail: "Currently unavailable across all order flows",
                        group: "UNAVAILABLE", isActive: false, basePrice: 0, unit: "Cylinder",
                        eta: "Unavailable", badge: "UNAVAILABLE", isEmergency: false)
        ]
    )

    private static let autoRepair = ServiceCategory(
        id: "auto_repair", name: "Auto Repair & Care",
        summary: "Expert diagnosis, engine, brakes & maintenance",
        symbolName: "wrench.and.screwdriver.fill", sortOrder: 2,
        availability: .available, isActive: true,
        items: [
            item("gen_inspection", "auto_repair", "General Inspection", 1500, "Check", "45 mins", "GENERAL SERVICE"),
            item("gen_preventive", "auto_repair", "Preventive Maintenance", 2500, "Service", "60 mins", "GENERAL SERVICE"),
            item("gen_scheduled", "auto_repair", "Scheduled Service", 3500, "Service", "90 mins", "GENERAL SERVICE"),
            item("eng_oil_change", "auto_repair", "Engine Oil Change", 1200, "Service", "25 mins", "ENGINE"),
            item("eng_tuning", "auto_repair", "Engine Tuning", 2800, "Tuning", "60 mins", "ENGINE"),
            item("brk_pads", "auto_repair", "Brake Pad Replacement", 2200, "Axle", "45 mins", "BRAKES"),
            item("susp_wheel_alignment", "auto_repair", "Wheel Alignment", 1800, "Car", "40 mins", "SUSPENSION & STEERING"),
            item("elec_diag", "auto_repair", "Electrical Diagnostics", 1800, "Diagnosis", "45 mins", "ELECTRICAL"),
            item("ac_gas", "auto_repair", "AC Refrigerant Service", 3800, "Recharge", "45 mins", "AC & COOLING"),
            item("bike_general", "auto_repair", "Bike General Service", 950, "Package", "45 mins", "BIKE SERVICES"),
            item("car_general", "auto_repair", "Car General Service", 4500, "Package", "90 mins", "CAR SERVICES")
        ]
    )

    private static let roadside = ServiceCategory(
        id: "roadside_assistance", name: "Roadside Assistance",
        summary: "24/7 emergency SOS, jump start, flat tyre & towing",
        symbolName: "exclamationmark.triangle.fill", sortOrder: 3,
        availability: .available, isActive: true,
        items: [
            emergency("sos_fuel", "Fuel Emergency", 2000, "Delivery", "10-15 mins", "Rapid SOS"),
            emergency("sos_flat_tyre", "Flat Tyre", 1200, "Assistance", "15-20 mins", "24/7 SOS"),
            emergency("sos_jump_start", "Battery Jump Start", 1000, "Jump", "10-15 mins", "Instant Boost"),
            emergency("sos_battery_replace", "Battery Replacement", 1500, "Install", "20-30 mins", nil),
            emergency("sos_wont_start", "Vehicle Won't Start", 1500, "Diagnosis", "15-25 mins", nil),
            emergency("sos_minor_repair", "Minor Roadside Repair", 1800, "Repair", "20-30 mins", nil),
            emergency("sos_towing", "Towing", 4500, "Tow", "20-35 mins", "Flatbed Tow"),
            emergency("sos_recovery", "Vehicle Recovery", 6000, "Recovery", "30-45 mins", nil),
            emergency("sos_lockout", "Lockout Assistance", 2000, "Unlock", "15-25 mins", nil),
            emergency("sos_mech_help", "Emergency Mechanical Help", 2200, "Dispatch", "15-25 mins", nil)
        ]
    )

    private static let autoDetailing = ServiceCategory(
        id: "auto_detailing", name: "Auto Care & Detailing",
        summary: "Car & bike wash, interior cleaning, wax & ceramic",
        symbolName: "sparkles", sortOrder: 4, availability: .available, isActive: true,
        items: [
            item("wash_car", "auto_detailing", "Car Wash", 1200, "Wash", "40 mins", "WASH"),
            item("wash_bike", "auto_detailing", "Bike Wash", 500, "Wash", "25 mins", "WASH"),
            item("detail_interior_clean", "auto_detailing", "Interior Cleaning", 3500, "Service", "75 mins", "CLEANING"),
            item("detail_full", "auto_detailing", "Full Detailing", 8500, "Package", "180 mins", "DETAILING", badge: "Best Value"),
            item("detail_wax", "auto_detailing", "Wax", 2500, "Coat", "45 mins", "POLISH & WAX"),
            item("detail_ceramic", "auto_detailing", "Ceramic Coating", 28000, "Package", "360 mins", "COATINGS", badge: "9H Quartz")
        ]
    )

    private static let tyres = ServiceCategory(
        id: "tyres_wheels", name: "Tyres & Wheels",
        summary: "Puncture repair, replacement, balancing & rotation",
        symbolName: "circle.dashed", sortOrder: 5, availability: .available, isActive: true,
        items: [
            item("tyre_puncture", "tyres_wheels", "Tyre Puncture", 600, "Puncture", "20 mins", "REPAIR", badge: "Quick Fix"),
            emergency("tyre_emergency_repair", "Emergency Tyre Repair", 1000, "Repair", "15 mins", "SOS Priority", category: "tyres_wheels"),
            item("tyre_replacement", "tyres_wheels", "Tyre Replacement", 1200, "Tyre", "35 mins", "REPLACEMENT"),
            item("tyre_rotation", "tyres_wheels", "Tyre Rotation", 1200, "Set", "30 mins", "MAINTENANCE"),
            item("tyre_balancing", "tyres_wheels", "Wheel Balancing", 1500, "Set", "40 mins", "MAINTENANCE"),
            item("tyre_alignment", "tyres_wheels", "Wheel Alignment", 1800, "Car", "40 mins", "MAINTENANCE")
        ]
    )

    private static let battery = ServiceCategory(
        id: "battery_services", name: "Battery Services",
        summary: "Jump start, testing, replacement & fresh battery delivery",
        symbolName: "battery.100percent.bolt", sortOrder: 6, availability: .available, isActive: true,
        items: [
            emergency("bat_jump_start", "Battery Jump Start", 1000, "Jump", "15 mins", "SOS Quick Jump", category: "battery_services"),
            item("bat_testing", "battery_services", "Battery Testing", 500, "Test", "15 mins", "SERVICE"),
            item("bat_replacement", "battery_services", "Battery Replacement", 1000, "Installation", "30 mins", "REPLACEMENT", badge: "Doorstep Install"),
            item("bat_car", "battery_services", "Car Battery", 14500, "Unit", "30 mins", "PRODUCTS"),
            item("bat_bike", "battery_services", "Bike Battery", 3200, "Unit", "20 mins", "PRODUCTS")
        ]
    )

    private static let lubricants = ServiceCategory(
        id: "lubricants_fluids", name: "Lubricants & Fluids",
        summary: "Engine oil, transmission fluid, coolant & filters",
        symbolName: "drop.fill", sortOrder: 7, availability: .available, isActive: true,
        items: [
            item("lub_engine_oil", "lubricants_fluids", "Engine Oil", 4200, "4L Can", "30 mins", "OILS"),
            item("lub_motorcycle_oil", "lubricants_fluids", "Motorcycle Oil", 1100, "1L Bottle", "20 mins", "OILS"),
            item("lub_coolant", "lubricants_fluids", "Coolant", 1400, "3L Bottle", "25 mins", "FLUIDS"),
            item("lub_brake_fluid", "lubricants_fluids", "Brake Fluid", 750, "500ml", "20 mins", "FLUIDS"),
            item("lub_oil_filters", "lubricants_fluids", "Oil Filters", 950, "Item", "20 mins", "FILTERS")
        ]
    )

    private static let ev = ServiceCategory(
        id: "ev_services", name: "EV Services",
        summary: "Mobile EV charging, station finder & EV assistance",
        symbolName: "bolt.car.fill", sortOrder: 8, availability: .available, isActive: true,
        items: [
            item("ev_charging", "ev_services", "EV Charging", 3500, "Boost", "30 mins", "CHARGING", badge: "Mobile DC Boost"),
            item("ev_station_finder", "ev_services", "Charging Station Finder", 0, "Free", "Instant", "NAVIGATION"),
            emergency("ev_emergency_assist", "Emergency EV Assistance", 5500, "Rescue", "25 mins", nil, category: "ev_services"),
            item("ev_battery_diag", "ev_services", "EV Battery Diagnostics", 4500, "Scan", "40 mins", "DIAGNOSTICS")
        ]
    )

    private static let fleet = ServiceCategory(
        id: "fleet_business", name: "Fleet & Business",
        summary: "Bulk fuel, recurring corporate delivery & fleet tracking",
        symbolName: "building.2.fill", sortOrder: 9, availability: .comingSoon, isActive: true,
        items: [
            item("fleet_bulk_fuel", "fleet_business", "Bulk Fuel Supply", 0, "Quote", "Scheduled", "B2B"),
            item("fleet_contract", "fleet_business", "Corporate Contract", 0, "Quote", "Scheduled", "B2B")
        ]
    )

    private static let waterDelivery = ServiceCategory(
        id: "water_delivery", name: "Water Delivery",
        summary: "Drinking water cans, RO mineral bottles & bulk tankers",
        symbolName: "waterbottle.fill", sortOrder: 10, availability: .closed, isActive: false,
        items: [
            ServiceItem(id: "water_can", categoryId: "water_delivery", name: "Drinking Water Can (Unavailable)",
                        detail: "Currently unavailable across all order flows", group: "UNAVAILABLE",
                        isActive: false, basePrice: 0, unit: "Can", eta: "Unavailable",
                        badge: "UNAVAILABLE", isEmergency: false)
        ]
    )

    private static func item(
        _ id: String, _ category: String, _ name: String, _ price: Double,
        _ unit: String, _ eta: String, _ group: String, badge: String? = nil
    ) -> ServiceItem {
        ServiceItem(id: id, categoryId: category, name: name, detail: "\(name) • Lahore doorstep service",
                    group: group, isActive: true, basePrice: price, unit: unit,
                    eta: eta, badge: badge, isEmergency: false)
    }

    private static func emergency(
        _ id: String, _ name: String, _ price: Double, _ unit: String,
        _ eta: String, _ badge: String?, category: String = "roadside_assistance"
    ) -> ServiceItem {
        ServiceItem(id: id, categoryId: category, name: name, detail: "\(name) • 24/7 emergency dispatch",
                    group: "EMERGENCY", isActive: true, basePrice: price, unit: unit,
                    eta: eta, badge: badge, isEmergency: true)
    }
}
