import SwiftUI
import UIKit

// MARK: - Section header

struct SectionHeader: View {
    let title: String
    var subtitle: String?
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(ZyphuelColor.textPrimary)
                if let subtitle {
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(ZyphuelColor.textSecondary)
                }
            }
            Spacer()
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.brand)
            }
        }
    }
}

// MARK: - Field

struct LabeledField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default
    var isSecure: Bool = false
    var autocapitalise: TextInputAutocapitalization = .sentences

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(ZyphuelColor.textSecondary)
            Group {
                if isSecure {
                    SecureField(placeholder, text: $text)
                } else {
                    TextField(placeholder, text: $text)
                }
            }
            .keyboardType(keyboard)
            .textInputAutocapitalization(autocapitalise)
            .autocorrectionDisabled(keyboard == .emailAddress)
            .font(.system(size: 15))
            .padding(.horizontal, 13)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(ZyphuelColor.background)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(ZyphuelColor.border, lineWidth: 1)
            )
        }
    }
}

// MARK: - Status badge

struct StatusBadge: View {
    let status: OrderStatus

    private var color: Color {
        switch status {
        case .pending: return ZyphuelColor.warning
        case .assigned: return ZyphuelColor.brand
        case .delivering: return ZyphuelColor.ocean
        case .completed: return ZyphuelColor.success
        case .cancelled: return ZyphuelColor.danger
        }
    }

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: status.iconName)
                .font(.system(size: 10, weight: .bold))
            Text(status.rawValue)
                .font(.system(size: 11, weight: .semibold))
        }
        .foregroundStyle(color)
        .padding(.horizontal, 9)
        .padding(.vertical, 5)
        .background(Capsule().fill(color.opacity(0.12)))
    }
}

// MARK: - Stat tile

struct StatTile: View {
    let title: String
    let value: String
    var caption: String?
    var tint: Color = ZyphuelColor.brand

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(ZyphuelColor.textSecondary)
                .textCase(.uppercase)
            Text(value)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(ZyphuelColor.textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            if let caption {
                Text(caption)
                    .font(.system(size: 11))
                    .foregroundStyle(tint)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .zyphuelCard(padding: 14, cornerRadius: 16)
    }
}

// MARK: - Empty state

struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 34))
                .foregroundStyle(ZyphuelColor.brand.opacity(0.7))
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(ZyphuelColor.textPrimary)
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(ZyphuelColor.textSecondary)
                .multilineTextAlignment(.center)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(SecondaryButtonStyle())
                    .frame(maxWidth: 240)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
    }
}

// MARK: - Operating-hours banner

struct OperatingHoursBanner: View {
    let status: OperatingHours.Status

    var body: some View {
        HStack(spacing: 11) {
            Circle()
                .fill(status.isOpen ? ZyphuelColor.success : ZyphuelColor.danger)
                .frame(width: 8, height: 8)
            VStack(alignment: .leading, spacing: 2) {
                Text(status.title)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.textPrimary)
                Text(status.activeSchedule)
                    .font(.system(size: 11))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
            Spacer()
            Text(status.isOpen ? "OPEN" : "CLOSED")
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(status.isOpen ? ZyphuelColor.success : ZyphuelColor.danger)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill((status.isOpen ? ZyphuelColor.success : ZyphuelColor.danger).opacity(0.07))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke((status.isOpen ? ZyphuelColor.success : ZyphuelColor.danger).opacity(0.25), lineWidth: 1)
        )
    }
}

// MARK: - Rate row

struct RateRow: View {
    let name: String
    let rate: Double
    var badge: String?

    var body: some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(name)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(ZyphuelColor.textPrimary)
                    if let badge {
                        BadgePill(text: badge, color: ZyphuelColor.success)
                    }
                }
                Text("OGRA notified • Euro-V standard")
                    .font(.system(size: 10))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
            Spacer()
            Text("\(MoneyFormatter.pkr(rate))/L")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(ZyphuelColor.brand)
        }
        .padding(.vertical, 7)
    }
}

// MARK: - Delivery progress stepper (replaces Android's removed live map)

struct DeliveryStepper: View {
    let status: OrderStatus
    private let steps = ["Placed", "Assigned", "En route", "Delivered"]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(steps.enumerated()), id: \.offset) { index, label in
                VStack(spacing: 6) {
                    ZStack {
                        Circle()
                            .fill(index <= status.stepIndex && status != .cancelled
                                  ? ZyphuelColor.brand : ZyphuelColor.border)
                            .frame(width: 26, height: 26)
                        if index <= status.stepIndex && status != .cancelled {
                            Image(systemName: index == steps.count - 1 ? "checkmark" : "\(index + 1)")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundStyle(.white)
                        }
                    }
                    Text(label)
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(index <= status.stepIndex && status != .cancelled
                                         ? ZyphuelColor.textPrimary : ZyphuelColor.textSecondary)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity)

                if index < steps.count - 1 {
                    Rectangle()
                        .fill(index < status.stepIndex && status != .cancelled
                              ? ZyphuelColor.brand : ZyphuelColor.border)
                        .frame(height: 2)
                        .offset(y: -10)
                }
            }
        }
        .padding(.vertical, 6)
    }
}

// MARK: - Star rating

struct StarRatingView: View {
    @Binding var rating: Int
    var size: CGFloat = 28

    var body: some View {
        HStack(spacing: 6) {
            ForEach(1...5, id: \.self) { star in
                Button {
                    rating = star
                } label: {
                    Image(systemName: star <= rating ? "star.fill" : "star")
                        .font(.system(size: size))
                        .foregroundStyle(star <= rating ? ZyphuelColor.warning : ZyphuelColor.border)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(star) star")
            }
        }
    }
}
