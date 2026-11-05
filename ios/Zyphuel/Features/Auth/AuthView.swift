import SwiftUI

/// Login, registration and password recovery. Mirrors the Android auth flow:
/// email + password, Google Sign-In, and a reset link instead of the old
/// email+phone recovery path.
struct AuthView: View {
    private enum Mode: String, CaseIterable, Identifiable {
        case signIn = "Sign In"
        case signUp = "Create Account"
        var id: String { rawValue }
    }

    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var language: LanguageManager

    @State private var mode: Mode = .signIn
    @State private var email = ""
    @State private var password = ""
    @State private var fullName = ""
    @State private var phone = ""
    @State private var role: UserRole = .customer
    @State private var acceptedLegal = false
    @State private var isWorking = false
    @State private var errorMessage: String?
    @State private var showResetSheet = false
    @State private var showLegal = false
    @State private var rateLimiter = SecurityRateLimiter()

    private var isFirebaseReady: Bool { appState.isFirebaseConfigured }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    header

                    if !isFirebaseReady {
                        setupNotice
                    }

                    formCard
                    googleButton
                    legalFooter
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 28)
            }
            .background(ZyphuelColor.background.ignoresSafeArea())
            .scrollDismissesKeyboard(.interactively)
            .navigationBarHidden(true)
            .alert(
                "Unable to continue",
                isPresented: Binding(
                    get: { errorMessage != nil },
                    set: { if !$0 { errorMessage = nil } }
                )
            ) {
                Button("OK", role: .cancel) { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "")
            }
            .sheet(isPresented: $showResetSheet) {
                PasswordResetSheet()
                    .environmentObject(appState)
            }
            .sheet(isPresented: $showLegal) {
                LegalView()
            }
        }
    }

    // MARK: Header

    private var header: some View {
        VStack(spacing: 10) {
            Image(systemName: "fuelpump.fill")
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(.white)
                .padding(16)
                .background(Circle().fill(.white.opacity(0.15)))

            Text("Zyphuel")
                .font(.system(size: 30, weight: .bold))
                .foregroundStyle(.white)
            Text("Doorstep fuel & auto care • Lahore")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.white.opacity(0.8))
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 46)
        .padding(.bottom, 20)
        .background(
            LinearGradient(
                colors: [ZyphuelColor.navy, ZyphuelColor.brandDark, ZyphuelColor.ocean],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea(edges: .top)
        )
    }

    private var setupNotice: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(ZyphuelColor.warning)
            VStack(alignment: .leading, spacing: 3) {
                Text("Firebase is not configured on this build")
                    .font(.system(size: 13, weight: .semibold))
                Text("Add GoogleService-Info.plist to ios/Config and regenerate the project so sign-in and live orders work.")
                    .font(.system(size: 11))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 14).fill(ZyphuelColor.warning.opacity(0.1)))
    }

    // MARK: Form

    private var formCard: some View {
        VStack(spacing: 14) {
            Picker("Mode", selection: $mode) {
                ForEach(Mode.allCases) { item in
                    Text(item.rawValue).tag(item)
                }
            }
            .pickerStyle(.segmented)
            .onChange(of: mode) { _, _ in errorMessage = nil }

            if mode == .signUp {
                LabeledField(label: "Full Name", placeholder: "e.g. Ali Raza", text: $fullName,
                             autocapitalise: .words)
                LabeledField(label: "Phone Number", placeholder: "03XX-XXXXXXX", text: $phone,
                             keyboard: .phonePad)
                roleSelector
            }

            LabeledField(label: "Email", placeholder: "you@example.com", text: $email,
                         keyboard: .emailAddress, autocapitalise: .never)
            LabeledField(label: "Password", placeholder: "Minimum 8 characters", text: $password,
                         isSecure: true, autocapitalise: .never)

            if mode == .signIn {
                HStack {
                    Spacer()
                    Button("Forgot password?") { showResetSheet = true }
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(ZyphuelColor.brand)
                }
            }

            if mode == .signUp {
                legalCheckbox
            }

            Button {
                Task { await submit() }
            } label: {
                if isWorking {
                    ProgressView().tint(.white)
                } else {
                    Text(mode == .signIn ? "Sign In" : "Create Zyphuel Account")
                }
            }
            .buttonStyle(PrimaryButtonStyle(enabled: canSubmit))
            .disabled(!canSubmit || isWorking)
        }
        .padding(18)
        .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Color.white))
        .shadow(color: .black.opacity(0.05), radius: 12, y: 4)
    }

    private var roleSelector: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text("Account Type")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(ZyphuelColor.textSecondary)
            HStack(spacing: 8) {
                ForEach([UserRole.customer, .rider], id: \.self) { option in
                    Button {
                        role = option
                    } label: {
                        HStack(spacing: 5) {
                            Image(systemName: option == .customer ? "person.fill" : "scooter")
                            Text(option == .customer ? "Customer" : "Rider")
                                .font(.system(size: 13, weight: .semibold))
                        }
                        .foregroundStyle(role == option ? Color.white : ZyphuelColor.textPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(
                            RoundedRectangle(cornerRadius: 11, style: .continuous)
                                .fill(role == option ? ZyphuelColor.brand : ZyphuelColor.background)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            if role == .rider {
                Text("Rider accounts require admin verification before deliveries can be accepted.")
                    .font(.system(size: 10))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
        }
    }

    private var legalCheckbox: some View {
        Button {
            acceptedLegal.toggle()
        } label: {
            HStack(alignment: .top, spacing: 9) {
                Image(systemName: acceptedLegal ? "checkmark.square.fill" : "square")
                    .foregroundStyle(acceptedLegal ? ZyphuelColor.brand : ZyphuelColor.textSecondary)
                Text("I agree to the Zyphuel Terms & Conditions and Privacy Policy.")
                    .font(.system(size: 11))
                    .foregroundStyle(ZyphuelColor.textSecondary)
                    .multilineTextAlignment(.leading)
                Spacer(minLength: 0)
            }
        }
        .buttonStyle(.plain)
    }

    private var googleButton: some View {
        VStack(spacing: 10) {
            HStack {
                Rectangle().fill(ZyphuelColor.border).frame(height: 1)
                Text("or").font(.system(size: 11)).foregroundStyle(ZyphuelColor.textSecondary)
                Rectangle().fill(ZyphuelColor.border).frame(height: 1)
            }

            Button {
                Task { await signInWithGoogle() }
            } label: {
                HStack(spacing: 9) {
                    Image(systemName: "globe")
                        .font(.system(size: 15, weight: .semibold))
                    Text("Continue with Google")
                        .font(.system(size: 15, weight: .semibold))
                }
                .foregroundStyle(ZyphuelColor.textPrimary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(ZyphuelColor.border, lineWidth: 1.2)
                )
            }
            .disabled(!isFirebaseReady || isWorking)
            .opacity(isFirebaseReady ? 1 : 0.5)
        }
    }

    private var legalFooter: some View {
        VStack(spacing: 6) {
            Text("Zyphuel is an early-stage Lahore startup. Deliveries operate \(OperatingHours.fullScheduleSummary).")
                .font(.system(size: 10))
                .foregroundStyle(ZyphuelColor.textSecondary)
                .multilineTextAlignment(.center)
            Button("Terms & Privacy") { showLegal = true }
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(ZyphuelColor.brand)
        }
        .padding(.top, 4)
    }

    // MARK: Actions

    private var canSubmit: Bool {
        guard !email.isEmpty, !password.isEmpty else { return false }
        if mode == .signUp {
            return !fullName.isEmpty && !phone.isEmpty && acceptedLegal
        }
        return true
    }

    private func submit() async {
        errorMessage = nil
        if rateLimiter.isLocked() {
            errorMessage = "Too many attempts. Try again in \(rateLimiter.remainingLockSeconds())s."
            return
        }
        isWorking = true
        defer { isWorking = false }

        do {
            switch mode {
            case .signIn:
                try await appState.signIn(email: email, password: password)
                rateLimiter.registerSuccess()
            case .signUp:
                try SecurityValidator.validateName(fullName)
                try SecurityValidator.validatePhone(phone)
                try SecurityValidator.validatePassword(password)
                try await appState.register(
                    email: email, password: password, name: fullName,
                    phone: phone, role: role
                )
            }
        } catch {
            let message = error.localizedDescription
            if mode == .signIn, !message.lowercased().contains("internet") {
                do {
                    try rateLimiter.registerFailure()
                } catch let lockout as SecurityRateLimiter.LockoutError {
                    errorMessage = "Too many incorrect attempts. Try again in \(lockout.remainingSeconds)s."
                    return
                } catch {
                    // Unreachable: only LockoutError is thrown.
                }
            }
            errorMessage = message
        }
    }

    private func signInWithGoogle() async {
        isWorking = true
        defer { isWorking = false }
        do {
            try await appState.signInWithGoogle()
        } catch AuthError.cancelled {
            // User dismissed the Google sheet; nothing to report.
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Password reset

struct PasswordResetSheet: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.dismiss) private var dismiss

    @State private var email = ""
    @State private var isSending = false
    @State private var sent = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Image(systemName: "envelope.badge.fill")
                    .font(.system(size: 36))
                    .foregroundStyle(ZyphuelColor.brand)

                Text("Reset your password")
                    .font(.system(size: 19, weight: .bold))
                Text("We email a secure reset link to your registered address. Never share this link with anyone.")
                    .font(.system(size: 13))
                    .foregroundStyle(ZyphuelColor.textSecondary)
                    .multilineTextAlignment(.center)

                LabeledField(label: "Email", placeholder: "you@example.com", text: $email,
                             keyboard: .emailAddress, autocapitalise: .never)

                if sent {
                    Label("Reset link sent. Check your inbox and spam folder.", systemImage: "checkmark.circle.fill")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(ZyphuelColor.success)
                }
                if let errorMessage {
                    Text(errorMessage)
                        .font(.system(size: 12))
                        .foregroundStyle(ZyphuelColor.danger)
                        .multilineTextAlignment(.center)
                }

                Button {
                    Task { await send() }
                } label: {
                    if isSending {
                        ProgressView().tint(.white)
                    } else {
                        Text("Send Reset Link")
                    }
                }
                .buttonStyle(PrimaryButtonStyle(enabled: !email.isEmpty))
                .disabled(email.isEmpty || isSending)

                Spacer()
            }
            .padding(20)
            .background(ZyphuelColor.background.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }

    private func send() async {
        isSending = true
        errorMessage = nil
        defer { isSending = false }
        do {
            try await appState.auth.sendPasswordReset(email: email)
            sent = true
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
