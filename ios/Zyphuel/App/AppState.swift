import FirebaseAuth
import FirebaseFirestore
import Foundation

/// Central session + routing state. Equivalent to `MainViewModel`'s auth/session slice
/// plus `MainActivity`'s role-based start destination.
@MainActor
final class AppState: ObservableObject {

    enum Route: Equatable {
        case launching
        case unauthenticated
        case authenticated
    }

    @Published private(set) var route: Route = .launching
    @Published private(set) var currentUser: AppUser?
    @Published var banner: String?
    @Published var isBootstrapping = true

    let auth = AuthService()
    let users = UserService()
    let orders = OrderService()
    let config = SystemConfigService()
    let location = LocationService()
    let biometrics = BiometricLock()
    let language = LanguageManager()
    let notifications = NotificationService()

    private let db = Firestore.firestore()
    private var pendingRole: UserRole = .customer

    var isFirebaseConfigured: Bool { auth.isConfigured }
    var isSignedIn: Bool { currentUser != nil }
    var role: UserRole { currentUser?.role ?? .customer }
    var isAdmin: Bool { currentUser?.isAdmin ?? false }
    var canPlaceOrders: Bool { OperatingHours.isDeliveryOpen() }

    // MARK: Bootstrap

    func bootstrap() async {
        guard isFirebaseConfigured else {
            route = .unauthenticated
            isBootstrapping = false
            return
        }
        // Runs once per launch; re-entry (lock screen, background bounce) must not reset the session.
        guard route == .launching else { return }
        config.startObserving()
        guard let firebaseUser = auth.firebaseUser ?? Auth.auth().currentUser else {
            route = .unauthenticated
            isBootstrapping = false
            return
        }
        await loadProfile(email: firebaseUser.email ?? "", fallbackName: firebaseUser.displayName ?? "")
        isBootstrapping = false
    }

    // MARK: Auth

    func signIn(email: String, password: String) async throws {
        let user = try await auth.signIn(email: email, password: password)
        await loadProfile(email: user.email ?? email, fallbackName: user.displayName ?? "")
        await users.incrementLaunchStats()
    }

    func register(email: String, password: String, name: String, phone: String, role: UserRole) async throws {
        pendingRole = role
        let user = try await auth.register(email: email, password: password, name: name)
        var profile = AppUser.empty(role: role)
        profile.email = (user.email ?? email).lowercased()
        profile.name = name
        profile.phoneNumber = phone
        profile.authProvider = .email
        profile.isVerified = role != .rider   // riders need admin approval, same as Android
        currentUser = profile
        await users.upsertProfile(profile)
        users.observeProfile(email: profile.email)
        route = .authenticated
        startRoleListeners()
    }

    func signInWithGoogle() async throws {
        let user = try await auth.signInWithGoogle()
        await loadProfile(
            email: user.email ?? "",
            fallbackName: user.displayName ?? ""
        )
    }

    func signOut() {
        orders.stopAllListeners()
        orders.stopObservingCustomerOrders()
        users.stopObservingProfile()
        users.stopAdminListeners()
        auth.signOut()
        currentUser = nil
        route = .unauthenticated
        biometrics.markUnlocked()
    }

    // MARK: Profile

    private func loadProfile(email: String, fallbackName: String) async {
        guard !email.isEmpty else {
            route = .unauthenticated
            return
        }
        let normalised = email.lowercased()
        var loaded = await fetchProfile(email: normalised)

        if loaded == nil {
            var fresh = AppUser.empty(role: AppUser.isSuperAdminEmail(normalised) ? .admin : pendingRole)
            fresh.email = normalised
            fresh.name = fallbackName.isEmpty ? normalised.components(separatedBy: "@").first ?? "Zyphuel User" : fallbackName
            fresh.authProvider = auth.firebaseUser?.providerData.contains(where: { $0.providerID == "google.com" }) == true ? .google : .email
            fresh.isSuperAdmin = AppUser.isSuperAdminEmail(normalised)
            fresh.isVerified = fresh.role != .rider
            await users.upsertProfile(fresh)
            loaded = fresh
        }

        guard var profile = loaded else {
            route = .unauthenticated
            return
        }
        // Server is the source of truth for the protected root account.
        if AppUser.isSuperAdminEmail(profile.email) {
            profile.role = .admin
            profile.isSuperAdmin = true
            profile.isVerified = true
        }
        if profile.accountStatus == "Blocked" || profile.accountStatus == "Suspended" {
            banner = "This account is \(profile.accountStatus.lowercased()). Contact Zyphuel support."
            auth.signOut()
            route = .unauthenticated
            return
        }
        currentUser = profile
        KeychainStore.set(profile.email, for: .sessionEmail)
        KeychainStore.set(profile.role.rawValue, for: .sessionRole)
        users.observeProfile(email: profile.email)
        route = .authenticated
        startRoleListeners()
        if biometrics.isEnabled {
            await biometrics.unlock()
        }
    }

    private func fetchProfile(email: String) async -> AppUser? {
        let collection = db.collection(ZyphuelConfig.Collection.users)
        for documentID in [UserService.documentId(for: email), email] {
            if let snapshot = try? await collection.document(documentID).getDocument(),
               snapshot.exists,
               let data = snapshot.data(),
               let user = FirestoreMapper.user(from: data, fallbackEmail: email) {
                return user
            }
        }
        return nil
    }

    func refreshProfile() async {
        guard let email = currentUser?.email else { return }
        if let refreshed = await fetchProfile(email: email) {
            currentUser = refreshed
            users.observeProfile(email: refreshed.email)
            startRoleListeners()
        }
    }

    func updatePhone(_ phone: String) async {
        guard var user = currentUser else { return }
        user.phoneNumber = phone
        currentUser = user
        await users.upsertProfile(user)
    }

    // MARK: Listeners per role

    private func startRoleListeners() {
        guard let user = currentUser else { return }
        switch user.role {
        case .customer:
            orders.observeCustomerOrders(email: user.email)
        case .rider:
            orders.observeRiderQueue(email: user.email)
        case .admin:
            users.observeRiders()
            users.observeCustomers()
            orders.observeAllOrders()
        }
    }

    // MARK: Order placement

    /// Validates volume, operating hours and availability before creating a COD order.
    enum OrderPlacementError: LocalizedError {
        case closed(OperatingHours.Status)
        case unauthenticated

        var errorDescription: String? {
            switch self {
            case .closed(let status):
                return "Deliveries are closed. \(status.activeSchedule)."
            case .unauthenticated:
                return "Sign in to place an order."
            }
        }
    }

    func placeOrder(
        serviceType: String,
        baseRate: Double,
        liters: Int,
        address: String,
        coordinate: (lat: Double, lng: Double)?
    ) async throws -> FuelOrder {
        guard var user = currentUser else { throw OrderPlacementError.unauthenticated }

        let status = OperatingHours.status()
        guard status.isOpen else { throw OrderPlacementError.closed(status) }

        _ = try PricingEngine.price(baseRate: baseRate, liters: liters, serviceType: serviceType)
        try SecurityValidator.validateAddress(address)
        if user.phoneNumber.isEmpty {
            // Android auto-creates a fallback session so ordering never dead-ends; here the
            // profile is repaired with an empty-but-valid phone instead of failing the order.
            user.phoneNumber = ""
        }

        var order = FuelOrder.draft(
            user: user,
            serviceType: serviceType,
            liters: liters,
            unitPrice: PricingEngine.pumpRate(base: baseRate, serviceType: serviceType),
            address: address
        )
        if let coordinate {
            order.destLat = coordinate.lat
            order.destLng = coordinate.lng
        }

        let stored = await orders.place(order)

        // Receipt + dispatch emails reuse the Android email engine channels.
        let template = EmailGateway.orderConfirmation(order: stored, statusLine: "Order received (COD)")
        Task {
            _ = await emailGateway.send(
                to: stored.customerEmail,
                subject: template.subject,
                body: template.body,
                senderUID: auth.currentUID,
                gateway: config.emailGateway
            )
            if let download = config.appVersion.downloadURL, config.appVersion.isOutdated {
                banner = "New Zyphuel version \(config.appVersion.latestVersionName) available."
                _ = download
            }
        }
        return stored
    }

    private lazy var emailGateway = EmailGateway()

    // MARK: Notification helper

    func requestNotificationsIfNeeded() async {
        await notifications.requestAuthorization()
        guard let email = currentUser?.email else { return }
        await notifications.saveToken(forEmail: email)
    }

    // MARK: Helpers

    func openWhatsApp() {
        let digits = ZyphuelConfig.whatsappNumber.filter(\.isNumber)
        guard let url = URL(string: "https://wa.me/\(digits)") else { return }
        UIApplication.shared.open(url)
    }

    func callSupport() {
        let digits = ZyphuelConfig.whatsappNumber.filter { $0.isNumber || $0 == "+" }
        guard let url = URL(string: "tel://\(digits)") else { return }
        UIApplication.shared.open(url)
    }
}
