import SwiftUI

/// Root switch: launch → (biometric lock) → auth → role-specific shell.
struct RootView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var biometrics: BiometricLock

    var body: some View {
        Group {
            switch appState.route {
            case .launching:
                LaunchView()
            case .unauthenticated:
                AuthView()
            case .authenticated:
                if biometrics.isEnabled && biometrics.isLocked {
                    LockScreenView()
                } else {
                    RoleShellView()
                }
            }
        }
        .task { await appState.bootstrap() }
        .overlay(alignment: .top) {
            if let banner = appState.banner {
                BannerView(message: banner) { appState.banner = nil }
                    .padding(.top, 8)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.2), value: appState.banner)
    }
}

struct LaunchView: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [ZyphuelColor.navy, ZyphuelColor.brandDark, ZyphuelColor.ocean],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 18) {
                Image(systemName: "fuelpump.fill")
                    .font(.system(size: 44, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(22)
                    .background(Circle().fill(.white.opacity(0.14)))

                Text("Zyphuel")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(.white)
                Text("Doorstep energy • Lahore")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(.white.opacity(0.75))

                ProgressView()
                    .tint(.white)
                    .padding(.top, 10)
            }
        }
    }
}

struct LockScreenView: View {
    @EnvironmentObject private var biometrics: BiometricLock
    @State private var isWorking = false
    @State private var message: String?

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "faceid")
                .font(.system(size: 52))
                .foregroundStyle(ZyphuelColor.brand)
            Text("Zyphuel is locked")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(ZyphuelColor.textPrimary)
            Text("Unlock with \(biometrics.biometryLabel) to continue.")
                .font(.system(size: 14))
                .foregroundStyle(ZyphuelColor.textSecondary)
                .multilineTextAlignment(.center)

            if let message {
                Text(message)
                    .font(.system(size: 13))
                    .foregroundStyle(ZyphuelColor.danger)
            }

            Button {
                Task {
                    isWorking = true
                    _ = await biometrics.unlock()
                    isWorking = false
                }
            } label: {
                if isWorking {
                    ProgressView().tint(.white)
                } else {
                    Text("Unlock")
                }
            }
            .buttonStyle(PrimaryButtonStyle())
            .padding(.horizontal, 40)
            .disabled(isWorking)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(ZyphuelColor.background.ignoresSafeArea())
    }
}

struct RoleShellView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        switch appState.role {
        case .customer:
            CustomerShellView()
        case .rider:
            RiderHomeView()
        case .admin:
            AdminDashboardView()
        }
    }
}

struct BannerView: View {
    let message: String
    let onDismiss: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "info.circle.fill")
                .foregroundStyle(ZyphuelColor.brand)
            Text(message)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(ZyphuelColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 4)
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color.white)
                .shadow(color: .black.opacity(0.08), radius: 10, y: 4)
        )
        .padding(.horizontal, 14)
    }
}
