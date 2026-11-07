import Foundation
import SwiftUI

/// In-app localization matching the Android English/Urdu toggle
/// (`AppLanguageManager`). Copy is kept in one table so a missing key falls back to the
/// English string instead of an empty label.
@MainActor
final class LanguageManager: ObservableObject {

    enum Language: String, CaseIterable, Identifiable {
        case english = "en"
        case urdu = "ur"

        var id: String { rawValue }
        var displayName: String { self == .english ? "English" : "اردو" }
        var isRightToLeft: Bool { self == .urdu }
    }

    private static let storageKey = "zyphuel.language"

    @Published var language: Language {
        didSet { UserDefaults.standard.set(language.rawValue, forKey: Self.storageKey) }
    }

    init() {
        let stored = UserDefaults.standard.string(forKey: Self.storageKey) ?? Language.english.rawValue
        language = Language(rawValue: stored) ?? .english
    }

    func tr(_ key: String, _ fallback: String) -> String {
        Self.table[key]?[language] ?? fallback
    }

    var layoutDirection: LayoutDirection { language.isRightToLeft ? .rightToLeft : .leftToRight }

    private static let table: [String: [Language: String]] = [
        "tab.home": [.english: "Home", .urdu: "ہوم"],
        "tab.orders": [.english: "Orders", .urdu: "آرڈرز"],
        "tab.profile": [.english: "Profile", .urdu: "پروفائل"],
        "home.deliverTo": [.english: "Deliver to", .urdu: "ڈیلیوری پتہ"],
        "home.services": [.english: "Our Services", .urdu: "ہماری سروسز"],
        "home.viewHistory": [.english: "View Full Order History →", .urdu: "مکمل آرڈر ہسٹری دیکھیں →"],
        "home.orderNow": [.english: "Order Now", .urdu: "ابھی آرڈر کریں"],
        "order.volume": [.english: "Volume (Litres)", .urdu: "مقدار (لیٹر)"],
        "order.address": [.english: "Delivery Address", .urdu: "ڈیلیوری کا پتہ"],
        "order.confirm": [.english: "Confirm Order (COD) 💵", .urdu: "آرڈر کنفرم کریں (COD) 💵"],
        "order.placing": [.english: "Placing Order…", .urdu: "آرڈر ہو رہا ہے…"],
        "order.deliveryFee": [.english: "Delivery Charges", .urdu: "ڈیلیوری چارجز"],
        "order.total": [.english: "COD Total", .urdu: "کل رقم"],
        "status.pending": [.english: "Pending", .urdu: "زیرِ التوا"],
        "status.assigned": [.english: "Assigned", .urdu: "تفویض شدہ"],
        "status.delivering": [.english: "Delivering", .urdu: "ڈیلیوری جاری"],
        "status.completed": [.english: "Completed", .urdu: "مکمل"],
        "status.cancelled": [.english: "Cancelled", .urdu: "منسوخ"],
        "profile.language": [.english: "App Language", .urdu: "ایپ کی زبان"],
        "profile.security": [.english: "Security", .urdu: "سیکیورٹی"],
        "profile.help": [.english: "Help & Support", .urdu: "مدد و معاونت"],
        "profile.logout": [.english: "Log Out", .urdu: "لاگ آؤٹ"],
        "admin.dashboard": [.english: "Admin Dashboard", .urdu: "ایڈمن ڈیش بورڈ"]
    ]
}
