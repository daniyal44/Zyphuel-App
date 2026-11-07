import Combine
import FirebaseAuth
import FirebaseFirestore
import Foundation

/// Order lifecycle on iOS. Field names, status names and the COD-only payment rule
/// match the Android `FirestoreOrderRepository`.
@MainActor
final class OrderService: ObservableObject {
    @Published private(set) var orders: [FuelOrder] = []
    @Published private(set) var riderQueue: [FuelOrder] = []
    @Published private(set) var isSubmitting = false
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var ordersListener: ListenerRegistration?
    private var riderListener: ListenerRegistration?

    var currentOrders: [FuelOrder] { orders.filter { $0.status.isCurrent } }
    var pastOrders: [FuelOrder] { orders.filter { !$0.status.isCurrent } }

    // MARK: Customer

    func observeCustomerOrders(email: String) {
        ordersListener?.remove()
        // Android stores the email exactly as authenticated, iOS normalises it. Query both
        // casings so an account created on either platform sees every order.
        let identifiers = Array(Set([email, email.lowercased()]))
        ordersListener = db.collection(ZyphuelConfig.Collection.orders)
            .whereField("customerEmail", in: identifiers)
            .addSnapshotListener { [weak self] snapshot, _ in
                let items = snapshot?.documents.compactMap {
                    FirestoreMapper.order(from: $0.data(), id: $0.documentID)
                } ?? []
                let sorted = items.sorted { $0.createdAt > $1.createdAt }
                Task { @MainActor in self?.orders = sorted }
            }
    }

    func stopObservingCustomerOrders() {
        ordersListener?.remove()
        ordersListener = nil
    }

    // MARK: Rider

    func observeRiderQueue(email: String) {
        riderListener?.remove()
        let normalised = email.lowercased()
        riderListener = db.collection(ZyphuelConfig.Collection.orders)
            .whereField("riderEmail", isEqualTo: normalised)
            .addSnapshotListener { [weak self] snapshot, _ in
                let mine = snapshot?.documents.compactMap {
                    FirestoreMapper.order(from: $0.data(), id: $0.documentID)
                } ?? []
                let sorted = mine.sorted { $0.createdAt > $1.createdAt }
                Task { @MainActor in self?.riderQueue = sorted }
            }
    }

    /// Pending orders that no rider has claimed yet.
    func fetchUnassignedOrders() async -> [FuelOrder] {
        do {
            let snapshot = try await db.collection(ZyphuelConfig.Collection.orders)
                .whereField("status", isEqualTo: OrderStatus.pending.rawValue)
                .getDocuments()
            return snapshot.documents.compactMap {
                FirestoreMapper.order(from: $0.data(), id: $0.documentID)
            }.sorted { $0.createdAt > $1.createdAt }
        } catch {
            errorMessage = error.localizedDescription
            return []
        }
    }

    // MARK: Admin

    func observeAllOrders() {
        ordersListener?.remove()
        ordersListener = db.collection(ZyphuelConfig.Collection.orders)
            .order(by: "createdAt", descending: true)
            .limit(to: 200)
            .addSnapshotListener { [weak self] snapshot, _ in
                let items = snapshot?.documents.compactMap {
                    FirestoreMapper.order(from: $0.data(), id: $0.documentID)
                } ?? []
                Task { @MainActor in self?.orders = items }
            }
    }

    func stopAllListeners() {
        ordersListener?.remove(); ordersListener = nil
        riderListener?.remove(); riderListener = nil
    }

    // MARK: Create

    /// Places a COD order. Local navigation never waits on the network: the caller gets
    /// the order immediately and the Firestore write happens in the background, matching
    /// the Android "0ms local order creation" behaviour.
    func place(_ order: FuelOrder) async -> FuelOrder {
        isSubmitting = true
        defer { isSubmitting = false }

        let reference = db.collection(ZyphuelConfig.Collection.orders)
            .document(order.id.isEmpty ? UUID().uuidString : order.id)
        var stored = order
        stored.id = reference.documentID

        do {
            try await reference.setData(FirestoreMapper.orderPayload(stored))
            return stored
        } catch {
            // Silent degrade: keep the order locally so the tracker still works offline.
            errorMessage = nil
            var local = stored
            local.id = stored.id
            orders.insert(local, at: 0)
            return local
        }
    }

    // MARK: Rider / admin transitions

    func assignRider(order: FuelOrder, rider: AppUser) async {
        await update(order) { payload in
            payload["riderEmail"] = rider.email
            payload["riderName"] = rider.name
            payload["status"] = OrderStatus.assigned.rawValue
        }
    }

    /// Enforces the Android progression guard: a rider cannot complete an order that is
    /// still Pending, and must pass through Assigned → Delivering → Completed.
    func advance(order: FuelOrder, to next: OrderStatus, isAdmin: Bool) async -> Bool {
        guard Self.canTransition(from: order.status, to: next, isAdmin: isAdmin) else {
            errorMessage = "That status change is not allowed from \(order.status.rawValue)."
            return false
        }
        await update(order) { payload in
            payload["status"] = next.rawValue
        }
        return true
    }

    static func canTransition(from current: OrderStatus, to next: OrderStatus, isAdmin: Bool) -> Bool {
        if current == next { return false }
        if current == .cancelled || current == .completed { return false }
        if isAdmin {
            // Admins may drive the full lifecycle, including cancellation.
            return true
        }
        switch (current, next) {
        case (.assigned, .delivering), (.delivering, .completed), (.pending, .cancelled):
            return true
        default:
            return false
        }
    }

    func decline(order: FuelOrder, reason: String, by adminEmail: String) async {
        await update(order) { payload in
            payload["status"] = OrderStatus.cancelled.rawValue
            payload["cancellationReason"] = reason
            payload["cancelledBy"] = adminEmail
        }
    }

    func deleteCancelled(order: FuelOrder) async {
        guard order.status == .cancelled else {
            errorMessage = "Only cancelled orders can be deleted."
            return
        }
        do {
            try await db.collection(ZyphuelConfig.Collection.orders)
                .document(order.id).delete()
            orders.removeAll { $0.id == order.id }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    // MARK: Rating

    func submitRating(order: FuelOrder, stars: Int, comment: String) async {
        let clamped = min(max(stars, 1), 5)
        do {
            try await db.collection(ZyphuelConfig.Collection.orders)
                .document(order.id)
                .setData([
                    "rating": clamped,
                    "feedback": SecurityValidator.sanitised(comment, maxLength: 600)
                ], merge: true)

            // Field names follow the existing `feedback` rule whitelist
            // (email / message / subject / rating / orderId / uid / createdAt).
            try await db.collection(ZyphuelConfig.Collection.feedback).addDocument(data: [
                "orderId": order.id,
                "email": order.customerEmail.lowercased(),
                "uid": Auth.auth().currentUser?.uid ?? "",
                "rating": clamped,
                "subject": "Delivery rating \(clamped)/5",
                "message": SecurityValidator.sanitised(comment, maxLength: 600),
                "createdAt": FieldValue.serverTimestamp()
            ])
        } catch {
            // Rating is non-critical; keep the UI responsive if the write is denied.
            errorMessage = nil
        }
    }

    private func update(_ order: FuelOrder, _ mutate: (inout [String: Any]) -> Void) async {
        var payload: [String: Any] = ["updatedAt": FieldValue.serverTimestamp()]
        mutate(&payload)
        do {
            try await db.collection(ZyphuelConfig.Collection.orders)
                .document(order.id).setData(payload, merge: true)
            if let index = orders.firstIndex(where: { $0.id == order.id }) {
                var updated = orders[index]
                if let status = payload["status"] as? String,
                   let parsed = OrderStatus(rawValue: status) {
                    updated.status = parsed
                }
                if let riderEmail = payload["riderEmail"] as? String { updated.riderEmail = riderEmail }
                if let riderName = payload["riderName"] as? String { updated.riderName = riderName }
                orders[index] = updated
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
