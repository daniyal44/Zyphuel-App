import Foundation

/// Port of `FeeConstants.kt`. Every number here is copied 1:1 from the Android
/// pricing engine, so a customer sees the same COD total on both platforms.
enum PricingEngine {

    static let maxLiters = 15
    static let pumpRateMarkup = 5.00
    static let baseDeliveryFee = 300.00

    /// Volume → delivery fee tiers. `upTo` is inclusive.
    static let fuelTiers: [(upTo: Int, fee: Double)] = [
        (10, 300.00),
        (11, 320.00),
        (12, 340.00),
        (13, 360.00),
        (14, 380.00),
        (15, 400.00)
    ]

    enum PricingError: LocalizedError, Equatable {
        case invalidVolume
        case exceedsMaximum(Int)
        case serviceUnavailable(String)

        var errorDescription: String? {
            switch self {
            case .invalidVolume:
                return "Enter a delivery volume greater than 0 liters."
            case .exceedsMaximum(let max):
                return "Maximum \(max)L per doorstep fuel order."
            case .serviceUnavailable(let name):
                return "\(name) is currently unavailable."
            }
        }
    }

    // MARK: Delivery fee

    static func deliveryFee(liters: Int) -> Double {
        guard liters > 0 else { return 0 }
        for tier in fuelTiers where liters <= tier.upTo {
            return tier.fee
        }
        return fuelTiers.last?.fee ?? baseDeliveryFee
    }

    // MARK: Pump rate (+Rs. 5.00/L on OGRA base)

    static func isPumpRateApplicable(serviceType: String?) -> Bool {
        guard let service = serviceType?.lowercased(), !service.isEmpty else { return false }
        return service.contains("petrol") || service.contains("diesel")
            || service.contains("octane") || service.contains("hobc")
    }

    static func pumpRate(base: Double, serviceType: String?) -> Double {
        isPumpRateApplicable(serviceType: serviceType) ? base + pumpRateMarkup : base
    }

    // MARK: Unavailable products (Water + LPG are blocked on Android)

    static func isServiceUnavailable(_ serviceType: String?) -> Bool {
        guard let service = serviceType?.lowercased(), !service.isEmpty else { return false }
        return service.contains("water") || service.contains("gas") || service.contains("lpg")
    }

    // MARK: Order maths

    struct Breakdown: Equatable {
        var liters: Int
        var unitPrice: Double
        var subtotal: Double
        var deliveryFee: Double
        var total: Double

        var subtotalLabel: String { MoneyFormatter.pkr(subtotal) }
        var deliveryFeeLabel: String { MoneyFormatter.pkr(deliveryFee) }
        var totalLabel: String { MoneyFormatter.pkr(total) }
        var unitPriceLabel: String { "\(MoneyFormatter.pkr(unitPrice))/L" }
    }

    /// Validates and prices a fuel order. `baseRate` is the OGRA ex-depot rate;
    /// the +Rs. 5.00 pump markup is applied silently, exactly like Android.
    static func price(
        baseRate: Double,
        liters: Int,
        serviceType: String
    ) throws -> Breakdown {
        guard !isServiceUnavailable(serviceType) else {
            throw PricingError.serviceUnavailable(serviceType)
        }
        guard liters > 0 else { throw PricingError.invalidVolume }
        guard liters <= maxLiters else { throw PricingError.exceedsMaximum(maxLiters) }

        let unit = pumpRate(base: baseRate, serviceType: serviceType)
        let subtotal = unit * Double(liters)
        let fee = deliveryFee(liters: liters)
        return Breakdown(
            liters: liters,
            unitPrice: unit,
            subtotal: subtotal,
            deliveryFee: fee,
            total: subtotal + fee
        )
    }

    /// Clamps a requested volume into the legal 1...15 range.
    static func clampLiters(_ liters: Int) -> Int {
        min(max(liters, 1), maxLiters)
    }
}

/// Port of `DeliveryOperatingHoursManager.kt`.
/// Schedule is evaluated in Pakistan Standard Time on every call so a device in a
/// different region still sees the same open/closed state as the Lahore depot.
enum OperatingHours {

    static let timeZone = TimeZone(identifier: "Asia/Karachi") ?? TimeZone(secondsFromGMT: 5 * 3600)!

    static let scheduleMonThu = "Mon–Thu: 08:00 AM – 08:00 PM"
    static let scheduleFriday = "Fri: 08:00 AM – 01:00 PM"
    static let scheduleWeekend = "Sat–Sun: 10:00 AM – 06:00 PM"

    struct Status: Equatable {
        var isOpen: Bool
        var title: String
        var subtitle: String
        var activeSchedule: String
    }

    enum Weekday: Int, CaseIterable {
        case sunday = 1, monday, tuesday, wednesday, thursday, friday, saturday
    }

    /// Opening window in minutes from midnight (start inclusive, end exclusive).
    static func window(for weekday: Weekday) -> Range<Int>? {
        switch weekday {
        case .monday, .tuesday, .wednesday, .thursday:
            return 480..<1200   // 08:00 – 20:00
        case .friday:
            return 480..<780    // 08:00 – 13:00
        case .saturday, .sunday:
            return 600..<1080   // 10:00 – 18:00
        }
    }

    static func calendar(for date: Date = Date()) -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        return calendar
    }

    static func isDeliveryOpen(at date: Date = Date()) -> Bool {
        let cal = calendar(for: date)
        let weekdayNumber = cal.component(.weekday, from: date)
        guard let weekday = Weekday(rawValue: weekdayNumber),
              let range = window(for: weekday) else { return false }
        let minutes = cal.component(.hour, from: date) * 60 + cal.component(.minute, from: date)
        return range.contains(minutes)
    }

    static func status(at date: Date = Date()) -> Status {
        let cal = calendar(for: date)
        let weekdayNumber = cal.component(.weekday, from: date)
        let weekday = Weekday(rawValue: weekdayNumber) ?? .monday
        let open = isDeliveryOpen(at: date)

        let schedule: String
        switch weekday {
        case .friday: schedule = scheduleFriday
        case .saturday, .sunday: schedule = scheduleWeekend
        default: schedule = scheduleMonThu
        }

        return Status(
            isOpen: open,
            title: open ? "Deliveries Open" : "Deliveries Closed",
            subtitle: open
                ? "Accepting orders now (\(schedule))"
                : "Orders paused outside operating hours (\(schedule))",
            activeSchedule: schedule
        )
    }

    static var fullScheduleSummary: String {
        "\(scheduleMonThu) • \(scheduleFriday) • \(scheduleWeekend)"
    }
}
