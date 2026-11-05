import Foundation

/// Single source of truth for values that also exist on the Android build.
/// Keeping them here (instead of scattering literals) makes the parity audit easy:
/// every constant below is copied from `app/build.gradle.kts`, `FeeConstants.kt`,
/// `DeliveryOperatingHoursManager.kt` or `ZyphuelConfig`-style Android objects.
enum ZyphuelConfig {

    // MARK: Build identity (mirrors build.gradle.kts)
    static let bundleIdentifier = "com.aistudio.zyphuel.appv2"
    static let marketingVersion = "2.6.4.0.0.38"
    static let buildNumber = 66

    // MARK: Business identity
    static let brandName = "Zyphuel"
    static let city = "Lahore"
    static let depotAddress = "Zyphuel Station, Green Town, Lahore"
    static let coverageLabel = "Coverage: Lahore Active"
    static let coverageZones = [
        "Gulberg", "DHA", "Model Town", "Johar Town",
        "Bahria Town", "Cantt", "All Lahore Operational Zones"
    ]
    static let defaultDeliveryAddress = "Main Boulevard, Gulberg III, Lahore"

    // MARK: Support channels (same numbers as the Android drawer / help centre)
    static let whatsappNumber = "+923230112464"
    static let whatsappDisplay = "+92 323 0112464"
    static let founderPhoneDisplay = "+92 323 0112464"

    /// Permanent root super admin. The account is protected from deletion on both
    /// platforms; the password hash itself is never stored in the repository.
    static let masterAdminEmail = "m.daniyalkhan490@gmail.com"

    // MARK: Firestore collections (must match firestore.rules)
    enum Collection {
        static let users = "users"
        static let orders = "orders"
        static let feedback = "feedback"
        static let auditLogs = "audit_logs"
        static let appStats = "app_stats"
        static let mail = "mail"
        static let systemConfig = "system_config"
    }

    enum ConfigDoc {
        static let fuelRates = "fuel_rates"
        static let appVersion = "app_version"
        static let emailGateway = "email_gateway"
    }

    // MARK: Default fuel rates (overridden live by system_config/fuel_rates)
    /// Values match `MainViewModel` defaults (SharedPreferences fallbacks).
    static let defaultPetrolRate = 289.38
    static let defaultDieselRate = 289.84
    static let defaultHighOctaneRate = 300.00
    static let defaultLpgRate = 0.0

    // MARK: Legal links (Android legal dialog uses the same web pages)
    static let privacyURL = URL(string: "https://zyphuel.netlify.app/privacy")!
    static let termsURL = URL(string: "https://zyphuel.netlify.app/terms")!
    static let websiteURL = URL(string: "https://zyphuel.netlify.app")!

    // MARK: Assets
    /// Flip to false if the generated app icon is not yet in Assets.xcassets.
    static let hasAppIcon = false
}
