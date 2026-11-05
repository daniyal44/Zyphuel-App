import SwiftUI

/// Profile and settings. Mirrors the Android drawer/profile dialog: account summary,
/// language, biometrics, notifications, help channels, legal and account deletion.
struct ProfileView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var biometrics: BiometricLock
    @EnvironmentObject private var language: LanguageManager

    @State private var showLanguagePicker = false
    @State private var showLegal = false
    @State private var showDeleteConfirm = false
    @State private var phoneDraft = ""
    @State private var showPhoneEditor = false
    @State private var statusMessage: String?
    @State private var isDeleting = false

    private var user: AppUser? { appState.currentUser }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    accountCard
                    statsRow
                    preferencesCard
                    supportCard
                    accountActions
                }
                .padding(16)
                .padding(.bottom, 24)
            }
            .background(ZyphuelColor.background.ignoresSafeArea())
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.inline)
            .zyphuelNavBar()
            .sheet(isPresented: $showLanguagePicker) {
                LanguagePickerSheet()
                    .environmentObject(language)
            }
            .sheet(isPresented: $showLegal) {
                LegalView()
            }
            .alert("Phone number", isPresented: $showPhoneEditor) {
                TextField("03XX-XXXXXXX", text: $phoneDraft)
                    .keyboardType(.phonePad)
                Button("Cancel", role: .cancel) {}
                Button("Save") {
                    Task {
                        let trimmed = phoneDraft.trimmingCharacters(in: .whitespaces)
                        if (try? SecurityValidator.validatePhone(trimmed)) != nil {
                            await appState.updatePhone(trimmed)
                            statusMessage = "Phone number updated."
                        } else {
                            statusMessage = "Enter a valid Pakistani mobile number."
                        }
                    }
                }
            } message: {
                Text("Riders use this number to call you on delivery day.")
            }
            .alert("Delete account?", isPresented: $showDeleteConfirm) {
                Button("Cancel", role: .cancel) {}
                Button("Delete permanently", role: .destructive) {
                    Task { await deleteAccount() }
                }
            } message: {
                Text("Your profile, saved addresses and session are removed. Completed order records are retained for accounting. This cannot be undone.")
            }
            .alert("Zyphuel", isPresented: Binding(
                get: { statusMessage != nil },
                set: { if !$0 { statusMessage = nil } }
            )) {
                Button("OK", role: .cancel) { statusMessage = nil }
            } message: {
                Text(statusMessage ?? "")
            }
        }
    }

    // MARK: Account

    private var accountCard: some View {
        VStack(spacing: 12) {
            HStack(spacing: 13) {
                ZStack {
                    Circle().fill(ZyphuelColor.brand.opacity(0.12))
                    Text(user?.displayInitials ?? "Z")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(ZyphuelColor.brand)
                }
                .frame(width: 58, height: 58)

                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 5) {
                        Text(user?.name.isEmpty == false ? user!.name : "Zyphuel User")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(ZyphuelColor.textPrimary)
                        if user?.isSuperAdmin == true {
                            Image(systemName: "checkmark.seal.fill")
                                .font(.system(size: 13))
                                .foregroundStyle(ZyphuelColor.verified)
                        }
                    }
                    Text(user?.email ?? "")
                        .font(.system(size: 11))
                        .foregroundStyle(ZyphuelColor.textSecondary)
                    HStack(spacing: 6) {
                        BadgePill(text: (user?.role.displayName ?? "Customer").uppercased(),
                                  color: ZyphuelColor.brand, filled: true)
                        if user?.isSuperAdmin == true {
                            BadgePill(text: "VERIFIED ADMIN", color: ZyphuelColor.success)
                        }
                    }
                }
                Spacer()
            }

            Button {
                phoneDraft = user?.phoneNumber ?? ""
                showPhoneEditor = true
            } label: {
                HStack(spacing: 9) {
                    Image(systemName: "phone.fill")
                        .font(.system(size: 11))
                        .foregroundStyle(ZyphuelColor.brand)
                    Text(user?.phoneNumber.isEmpty == false ? user!.phoneNumber : "Add phone number")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(ZyphuelColor.textPrimary)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(ZyphuelColor.textSecondary)
                }
                .padding(11)
                .background(RoundedRectangle(cornerRadius: 12).fill(ZyphuelColor.background))
            }
            .buttonStyle(.plain)
        }
        .zyphuelCard()
    }

    private var statsRow: some View {
        HStack(spacing: 10) {
            StatTile(title: "Orders", value: "\(appState.orders.orders.count)", caption: "placed")
            StatTile(title: "Active", value: "\(appState.orders.currentOrders.count)", caption: "in progress",
                     tint: ZyphuelColor.warning)
            StatTile(title: "Spent", value: MoneyFormatter.pkr(
                appState.orders.orders.filter { $0.status == .completed }.reduce(0) { $0 + $1.totalPrice }
            ), caption: "COD delivered", tint: ZyphuelColor.success)
        }
    }

    // MARK: Preferences

    private var preferencesCard: some View {
        VStack(spacing: 0) {
            SectionHeader(title: "Preferences", subtitle: nil)
                .padding(.bottom, 6)

            settingsRow(icon: "globe", title: language.tr("profile.language", "App Language"),
                        value: language.language.displayName) {
                showLanguagePicker = true
            }
            Divider().padding(.leading, 34)

            settingsRow(icon: "faceid", title: "Biometric lock",
                        value: biometrics.isEnabled ? "On" : "Off") {
                guard biometrics.isBiometryAvailable else {
                    statusMessage = "This device has no Face ID or Touch ID enrolled."
                    return
                }
                biometrics.setEnabled(!biometrics.isEnabled)
            }
            Divider().padding(.leading, 34)

            settingsRow(icon: "bell.badge", title: "Order notifications",
                        value: appState.notifications.isAuthorized ? "Allowed" : "Allow") {
                Task { await appState.requestNotificationsIfNeeded() }
            }
            Divider().padding(.leading, 34)

            settingsRow(icon: "lock.shield", title: "Privacy & security",
                        value: nil) {
                showLegal = true
            }
        }
        .zyphuelCard()
    }

    private var supportCard: some View {
        VStack(spacing: 0) {
            SectionHeader(title: language.tr("profile.help", "Help & Support"), subtitle: nil)
                .padding(.bottom, 6)

            settingsRow(icon: "message.fill", title: "WhatsApp operations",
                        value: ZyphuelConfig.whatsappDisplay) {
                appState.openWhatsApp()
            }
            Divider().padding(.leading, 34)

            settingsRow(icon: "phone.fill", title: "Call Zyphuel",
                        value: nil) {
                appState.callSupport()
            }
            Divider().padding(.leading, 34)

            settingsRow(icon: "doc.text", title: "Terms, privacy & refunds",
                        value: nil) {
                showLegal = true
            }
        }
        .zyphuelCard()
    }

    private var accountActions: some View {
        VStack(spacing: 10) {
            if user?.isSuperAdmin == true {
                HStack(spacing: 8) {
                    Image(systemName: "shield.lefthalf.filled")
                        .foregroundStyle(ZyphuelColor.success)
                    Text("Permanent Root Super Admin — this account cannot be deleted.")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(ZyphuelColor.success)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(RoundedRectangle(cornerRadius: 12).fill(ZyphuelColor.success.opacity(0.08)))
            } else {
                Button(role: .destructive) {
                    showDeleteConfirm = true
                } label: {
                    HStack(spacing: 8) {
                        if isDeleting { ProgressView().tint(ZyphuelColor.danger) }
                        Text(isDeleting ? "Deleting…" : "Delete Account")
                            .font(.system(size: 13, weight: .semibold))
                    }
                    .foregroundStyle(ZyphuelColor.danger)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(RoundedRectangle(cornerRadius: 13).fill(ZyphuelColor.danger.opacity(0.07)))
                }
                .buttonStyle(.plain)
                .disabled(isDeleting)
            }

            Button {
                appState.signOut()
            } label: {
                Text(language.tr("profile.logout", "Log Out"))
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.textPrimary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(RoundedRectangle(cornerRadius: 13).fill(Color.white))
                    .overlay(RoundedRectangle(cornerRadius: 13).stroke(ZyphuelColor.border, lineWidth: 1))
            }
            .buttonStyle(.plain)

            Text("Zyphuel iOS \(ZyphuelConfig.marketingVersion) (Build \(ZyphuelConfig.buildNumber)) • Lahore, Pakistan")
                .font(.system(size: 10))
                .foregroundStyle(ZyphuelColor.textSecondary)
                .padding(.top, 4)
        }
    }

    private func settingsRow(
        icon: String,
        title: String,
        value: String?,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 11) {
                Image(systemName: icon)
                    .font(.system(size: 13))
                    .foregroundStyle(ZyphuelColor.brand)
                    .frame(width: 22)
                Text(title)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(ZyphuelColor.textPrimary)
                Spacer()
                if let value {
                    Text(value)
                        .font(.system(size: 11))
                        .foregroundStyle(ZyphuelColor.textSecondary)
                        .lineLimit(1)
                }
                Image(systemName: "chevron.right")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
            .padding(.vertical, 11)
        }
        .buttonStyle(.plain)
    }

    // MARK: Actions

    private func deleteAccount() async {
        guard let user else { return }
        isDeleting = true
        defer { isDeleting = false }
        let deleted = await appState.users.deleteAccount(user)
        if deleted {
            await appState.users.writeAudit(
                action: "account_deleted",
                performedBy: user.email,
                details: "Self-service account deletion from iOS"
            )
            appState.signOut()
        } else {
            statusMessage = appState.users.errorMessage ?? "Could not delete this account."
        }
    }
}

// MARK: - Language picker

struct LanguagePickerSheet: View {
    @EnvironmentObject private var language: LanguageManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                ForEach(LanguageManager.Language.allCases) { option in
                    Button {
                        language.language = option
                        dismiss()
                    } label: {
                        HStack {
                            Text(option.displayName)
                                .font(.system(size: 15))
                                .foregroundStyle(ZyphuelColor.textPrimary)
                            Spacer()
                            if language.language == option {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(ZyphuelColor.brand)
                            }
                        }
                    }
                }
            }
            .navigationTitle("App Language")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium])
    }
}
