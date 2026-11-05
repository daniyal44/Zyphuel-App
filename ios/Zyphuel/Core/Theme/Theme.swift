import SwiftUI

/// Colors lifted from the Android palette (`ui/theme/Color.kt` and the hard-coded
/// brand values used across `Screens.kt`) so both platforms look like one product.
enum ZyphuelColor {
    static let brand = Color(hex: 0x0284C7)          // primary blue
    static let brandDark = Color(hex: 0x0F2B48)
    static let navy = Color(hex: 0x0F172A)
    static let ocean = Color(hex: 0x0369A1)
    static let background = Color(hex: 0xF8FAFC)
    static let surface = Color.white
    static let border = Color(hex: 0xE2E8F0)
    static let textPrimary = Color(hex: 0x0F172A)
    static let textSecondary = Color(hex: 0x64748B)
    static let success = Color(hex: 0x10B981)
    static let warning = Color(hex: 0xF59E0B)
    static let danger = Color(hex: 0xEF4444)
    static let verified = Color(hex: 0x0284C7)
}

extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: alpha
        )
    }
}

/// Shared card / field styling. Mirrors the 16–20pt rounded corners used on Android.
struct ZyphuelCard: ViewModifier {
    var padding: CGFloat = 16
    var cornerRadius: CGFloat = 18

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .background(ZyphuelColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(ZyphuelColor.border, lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 3)
    }
}

extension View {
    func zyphuelCard(padding: CGFloat = 16, cornerRadius: CGFloat = 18) -> some View {
        modifier(ZyphuelCard(padding: padding, cornerRadius: cornerRadius))
    }

    /// The navigation-bar treatment used across the ported screens.
    func zyphuelNavBar() -> some View {
        toolbarBackground(ZyphuelColor.surface, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.light, for: .navigationBar)
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    var enabled: Bool = true

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 15)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(enabled ? ZyphuelColor.brand : ZyphuelColor.textSecondary.opacity(0.4))
            )
            .opacity(configuration.isPressed ? 0.85 : 1)
            .scaleEffect(configuration.isPressed ? 0.99 : 1)
    }
}

struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(ZyphuelColor.brand)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 13)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(ZyphuelColor.brand.opacity(0.45), lineWidth: 1.2)
            )
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}

/// Small pill used for rates, badges and statuses.
struct BadgePill: View {
    let text: String
    var color: Color = ZyphuelColor.brand
    var filled: Bool = false

    var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold))
            .padding(.horizontal, 9)
            .padding(.vertical, 4)
            .foregroundStyle(filled ? Color.white : color)
            .background(
                Capsule().fill(filled ? color : color.opacity(0.12))
            )
    }
}
