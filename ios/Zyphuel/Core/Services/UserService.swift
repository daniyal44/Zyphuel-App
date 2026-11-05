import FirebaseFirestore
import FirebaseAuth
import Foundation

/// Firestore access for the `users` collection (same document ids and field names
/// as the Android repositories, so one account works on both platforms).
@MainActor
final class UserService: ObservableObject {
    @Published private(set) var profile: AppUser?
    /// Actual document id in `users` (may be `uid` when Android created the account).
    @Published private(set) var profileDocumentID: String?
    @Published private(set) var riders: [AppUser] = []
    @Published private(set) var customers: [AppUser] = []
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var profileListener: ListenerRegistration?
    private var ridersListener: ListenerRegistration?
    private var customersListener: ListenerRegistration?

    /// Android stores the user document under the email with dots replaced,
    /// but newer code writes `users/{uid}`. Both are read during migration.
    static func documentId(for email: String) -> String {
        email.lowercased().replacingOccurrences(of: ".", with: "_")
    }

    // MARK: Profile

    func observeProfile(email: String) {
        let normalised = email.lowercased()
        let fallbackID = Self.documentId(for: normalised)
        attachProfileListener(documentID: fallbackID, email: normalised)

        // Android writes `users/{uid}` when it has one. Adopt that document instead of
        // forking a duplicate profile under the email-derived id.
        db.collection(ZyphuelConfig.Collection.users)
            .whereField("email", isEqualTo: normalised)
            .limit(to: 1)
            .getDocuments { [weak self] snapshot, _ in
                guard let document = snapshot?.documents.first else { return }
                let documentID = document.documentID
                let user = FirestoreMapper.user(from: document.data(), fallbackEmail: normalised)
                Task { @MainActor in
                    guard let self, documentID != fallbackID else { return }
                    self.profileDocumentID = documentID
                    if let user { self.profile = user }
                    self.attachProfileListener(documentID: documentID, email: normalised)
                }
            }
    }

    private func attachProfileListener(documentID: String, email: String) {
        profileListener?.remove()
        profileDocumentID = documentID
        profileListener = db.collection(ZyphuelConfig.Collection.users)
            .document(documentID)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let data = snapshot?.data(),
                      let user = FirestoreMapper.user(from: data, fallbackEmail: email) else { return }
                Task { @MainActor in self?.profile = user }
            }
    }

    func stopObservingProfile() {
        profileListener?.remove()
        profileListener = nil
    }

    /// Upserts the account after login/registration. Only writes fields the hardened
    /// Firestore rules allow a signed-in user to write for their own account.
    func upsertProfile(_ user: AppUser) async {
        do {
            let collection = db.collection(ZyphuelConfig.Collection.users)
            var documentID = profileDocumentID ?? Self.documentId(for: user.email)
            if let found = try? await collection
                .whereField("email", isEqualTo: user.email.lowercased())
                .limit(to: 1)
                .getDocuments(),
               let existing = found.documents.first {
                documentID = existing.documentID
                profileDocumentID = documentID
            }
            let document = collection.document(documentID)
            // Only non-privileged fields are written by a client. `isVerified` and
            // `approvalStatus` stay admin-controlled in Firestore, otherwise a rider
            // could self-approve and accept deliveries.
            var payload: [String: Any] = [
                "email": user.email.lowercased(),
                "name": user.name,
                "displayName": user.name, // Android reads displayName
                "phoneNumber": user.phoneNumber,
                "role": user.role.rawValue,
                "authProvider": user.authProvider.rawValue,
                "updatedAt": FieldValue.serverTimestamp()
            ]
            if let photo = user.profilePictureBase64, !photo.isEmpty {
                payload["profilePictureBase64"] = photo
            }
            if let vehicleType = user.vehicleType { payload["vehicleType"] = vehicleType }
            if let vehicleNo = user.vehicleNo { payload["vehicleNo"] = vehicleNo }
            // `extra` fields are read-only for display. They are never echoed back on a
            // write: `setData(merge: true)` keeps them untouched, and echoing unknown
            // keys would violate the hardened user-profile field whitelist.

            let snapshot = try await document.getDocument()
            if !snapshot.exists {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            try await document.setData(payload, merge: true)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    // MARK: Role lists (admin dashboard)

    func observeRiders() {
        ridersListener?.remove()
        ridersListener = db.collection(ZyphuelConfig.Collection.users)
            .whereField("role", isEqualTo: UserRole.rider.rawValue)
            .addSnapshotListener { [weak self] snapshot, _ in
                let items = snapshot?.documents.compactMap {
                    FirestoreMapper.user(from: $0.data(), fallbackEmail: $0.documentID)
                } ?? []
                let sorted = items.sorted { $0.createdAt > $1.createdAt }
                Task { @MainActor in self?.riders = sorted }
            }
    }

    func observeCustomers() {
        customersListener?.remove()
        customersListener = db.collection(ZyphuelConfig.Collection.users)
            .whereField("role", isEqualTo: UserRole.customer.rawValue)
            .addSnapshotListener { [weak self] snapshot, _ in
                let items = snapshot?.documents.compactMap {
                    FirestoreMapper.user(from: $0.data(), fallbackEmail: $0.documentID)
                } ?? []
                let sorted = items.sorted { $0.createdAt > $1.createdAt }
                Task { @MainActor in self?.customers = sorted }
            }
    }

    func stopAdminListeners() {
        ridersListener?.remove(); ridersListener = nil
        customersListener?.remove(); customersListener = nil
    }

    // MARK: Admin mutations

    func setRiderVerified(_ user: AppUser, verified: Bool) async {
        guard !AppUser.isSuperAdminEmail(user.email) else {
            errorMessage = "The permanent root super admin cannot be modified."
            return
        }
        do {
            try await db.collection(ZyphuelConfig.Collection.users)
                .document(Self.documentId(for: user.email))
                .setData([
                    "isVerified": verified,
                    "approvalStatus": verified ? "Approved" : "Pending",
                    "updatedAt": FieldValue.serverTimestamp()
                ], merge: true)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// Deleting the super admin account is blocked on Android; the same guard applies here.
    func deleteAccount(_ user: AppUser) async -> Bool {
        guard !AppUser.isSuperAdminEmail(user.email) else {
            errorMessage = "The permanent root super admin account cannot be deleted."
            return false
        }
        do {
            try await db.collection(ZyphuelConfig.Collection.users)
                .document(Self.documentId(for: user.email))
                .delete()
            try await db.collection(ZyphuelConfig.Collection.users)
                .document(user.email.lowercased())
                .delete()
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func writeAudit(action: String, performedBy: String, details: String) async {
        // Only admins may create audit_logs under the hardened rules.
        let entry: [String: Any] = [
            "action": action,
            "performedBy": performedBy,
            "details": details,
            "timestamp": FieldValue.serverTimestamp()
        ]
        try? await db.collection(ZyphuelConfig.Collection.auditLogs).addDocument(data: entry)
    }

    func incrementLaunchStats() async {
        // Rules only accept exactly +1 on `count`, so this stays a pure launch counter.
        let reference = db.collection(ZyphuelConfig.Collection.appStats).document("launches")
        try? await reference.setData([
            "count": FieldValue.increment(Int64(1)),
            "lastUpdated": FieldValue.serverTimestamp()
        ], merge: true)
    }
}

// MARK: - Firestore mapping

/// Android writes a mix of Timestamps and epoch millis (Room rows are millis).
/// Mapping manually keeps the iOS client tolerant of both instead of failing to decode.
enum FirestoreMapper {

    static func user(from data: [String: Any], fallbackEmail: String) -> AppUser? {
        let email = (data["email"] as? String) ?? fallbackEmail
        guard !email.isEmpty else { return nil }
        let role = UserRole(rawValue: (data["role"] as? String) ?? "customer") ?? .customer
        return AppUser(
            email: email,
            name: (data["name"] as? String) ?? (data["displayName"] as? String) ?? "",
            phoneNumber: (data["phoneNumber"] as? String) ?? "",
            role: role,
            isVerified: (data["isVerified"] as? Bool) ?? false,
            hasRequestedVerification: (data["hasRequestedVerification"] as? Bool) ?? false,
            authProvider: AuthProvider(rawValue: (data["authProvider"] as? String) ?? "Email") ?? .email,
            profilePictureBase64: data["profilePictureBase64"] as? String,
            vehicleType: data["vehicleType"] as? String,
            vehicleNo: data["vehicleNo"] as? String,
            riderNumber: data["riderNumber"] as? Int,
            riderId: data["riderId"] as? String,
            approvalStatus: (data["approvalStatus"] as? String) ?? "Pending",
            accountStatus: (data["accountStatus"] as? String) ?? "Active",
            isSuperAdmin: AppUser.isSuperAdminEmail(email),
            createdAt: date(from: data["createdAt"]) ?? Date(),
            updatedAt: date(from: data["updatedAt"]) ?? Date(),
            extra: stringExtras(from: data)
        )
    }

    static func order(from data: [String: Any], id: String) -> FuelOrder? {
        guard let serviceType = data["serviceType"] as? String else { return nil }
        return FuelOrder(
            id: id,
            customerEmail: (data["customerEmail"] as? String) ?? "",
            customerName: (data["customerName"] as? String) ?? "",
            customerPhone: (data["customerPhone"] as? String) ?? "",
            serviceType: serviceType,
            quantityLiters: int(from: data["quantity"] ?? data["quantityLiters"]) ?? 0,
            unitPrice: double(from: data["unitPrice"]) ?? 0,
            deliveryFee: double(from: data["deliveryFee"]) ?? 0,
            totalPrice: double(from: data["totalPrice"]) ?? 0,
            deliveryAddress: (data["deliveryAddress"] as? String) ?? "",
            paymentMethod: (data["paymentMethod"] as? String) ?? "Cash on Delivery",
            status: OrderStatus(rawValue: (data["status"] as? String) ?? "Pending") ?? .pending,
            riderEmail: data["riderEmail"] as? String,
            riderName: data["riderName"] as? String,
            createdAt: date(from: data["createdAt"]) ?? Date(),
            etaMinutes: int(from: data["etaMinutes"]) ?? 30,
            rating: int(from: data["rating"]),
            feedback: data["feedback"] as? String,
            destLat: double(from: data["destLat"]),
            destLng: double(from: data["destLng"]),
            originLat: double(from: data["originLat"]),
            originLng: double(from: data["originLng"])
        )
    }

    static func orderPayload(_ order: FuelOrder) -> [String: Any] {
        var payload: [String: Any] = [
            "customerEmail": order.customerEmail.lowercased(),
            "customerName": order.customerName,
            "customerPhone": order.customerPhone,
            "serviceType": order.serviceType,
            "quantity": order.quantityLiters,
            "quantityLiters": order.quantityLiters,
            "unitPrice": order.unitPrice,
            "deliveryFee": order.deliveryFee,
            "totalPrice": order.totalPrice,
            "deliveryAddress": order.deliveryAddress,
            "paymentMethod": order.paymentMethod,
            "status": order.status.rawValue,
            "createdAt": Timestamp(date: order.createdAt),
            "etaMinutes": order.etaMinutes
        ]
        if let riderEmail = order.riderEmail { payload["riderEmail"] = riderEmail }
        if let riderName = order.riderName { payload["riderName"] = riderName }
        if let lat = order.destLat { payload["destLat"] = lat }
        if let lng = order.destLng { payload["destLng"] = lng }
        if let lat = order.originLat { payload["originLat"] = lat }
        if let lng = order.originLng { payload["originLng"] = lng }
        return payload
    }

    static func date(from value: Any?) -> Date? {
        if let timestamp = value as? Timestamp { return timestamp.dateValue() }
        if let number = value as? NSNumber { return Date(timeIntervalSince1970: number.doubleValue / 1000) }
        if let string = value as? String {
            let formatter = ISO8601DateFormatter()
            return formatter.date(from: string)
        }
        return nil
    }

    static func int(from value: Any?) -> Int? {
        if let int = value as? Int { return int }
        if let number = value as? NSNumber { return number.intValue }
        if let string = value as? String { return Int(string) }
        return nil
    }

    static func double(from value: Any?) -> Double? {
        if let double = value as? Double { return double }
        if let number = value as? NSNumber { return number.doubleValue }
        if let string = value as? String { return Double(string) }
        return nil
    }

    private static func stringExtras(from data: [String: Any]) -> [String: String] {
        let known: Set<String> = [
            "email", "name", "displayName", "phoneNumber", "role", "isVerified",
            "hasRequestedVerification", "authProvider", "profilePictureBase64",
            "vehicleType", "vehicleNo", "riderNumber", "riderId", "approvalStatus",
            "accountStatus", "createdAt", "updatedAt"
        ]
        var extras: [String: String] = [:]
        for (key, value) in data where !known.contains(key) {
            if let string = value as? String { extras[key] = string }
        }
        return extras
    }
}
