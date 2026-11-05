import FirebaseFirestore
import Foundation

/// Live `system_config` documents (fuel rates, app version, email gateway) plus the
/// transactional email bridge. Mirrors the Android `RealtimeEmailEngine` Firestore path.
@MainActor
final class SystemConfigService: ObservableObject {
    @Published private(set) var rates: FuelRates = .fallback
    @Published private(set) var appVersion: AppVersionInfo = .placeholder
    @Published private(set) var emailGateway: EmailGatewayConfig = EmailGatewayConfig(webhookURL: nil, senderEmail: nil)
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    private var listeners: [ListenerRegistration] = []

    var isDeliveriesOpen: Bool { OperatingHours.isDeliveryOpen() }
    var deliveryStatus: OperatingHours.Status { OperatingHours.status() }

    // MARK: Listeners

    func startObserving() {
        stopObserving()
        let config = db.collection(ZyphuelConfig.Collection.systemConfig)

        listeners.append(config.document(ZyphuelConfig.ConfigDoc.fuelRates)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let data = snapshot?.data() else { return }
                let rates = FuelRates(
                    petrol: FirestoreMapper.double(from: data["petrol"]) ?? FuelRates.fallback.petrol,
                    diesel: FirestoreMapper.double(from: data["diesel"]) ?? FuelRates.fallback.diesel,
                    highOctane: FirestoreMapper.double(from: data["highOctane"]) ?? FuelRates.fallback.highOctane,
                    lpg: FirestoreMapper.double(from: data["lpg"]) ?? 0,
                    updatedAt: FirestoreMapper.date(from: data["updatedAt"])
                )
                Task { @MainActor in self?.rates = rates }
            })

        listeners.append(config.document(ZyphuelConfig.ConfigDoc.appVersion)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let data = snapshot?.data() else { return }
                let info = AppVersionInfo(
                    latestVersionCode: FirestoreMapper.int(from: data["latestVersionCode"]) ?? ZyphuelConfig.buildNumber,
                    latestVersionName: (data["latestVersionName"] as? String) ?? ZyphuelConfig.marketingVersion,
                    downloadURL: data["downloadURL"] as? String,
                    mandatory: (data["mandatory"] as? Bool) ?? false
                )
                Task { @MainActor in self?.appVersion = info }
            })

        listeners.append(config.document(ZyphuelConfig.ConfigDoc.emailGateway)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let data = snapshot?.data() else { return }
                let gateway = EmailGatewayConfig(
                    webhookURL: data["webhookURL"] as? String,
                    senderEmail: data["senderEmail"] as? String
                )
                Task { @MainActor in self?.emailGateway = gateway }
            })
    }

    func stopObserving() {
        listeners.forEach { $0.remove() }
        listeners.removeAll()
    }

    func baseRate(for serviceType: String) -> Double {
        let lower = serviceType.lowercased()
        if lower.contains("octane") || lower.contains("hobc") { return rates.highOctane }
        if lower.contains("diesel") { return rates.diesel }
        if lower.contains("lpg") || lower.contains("gas") { return rates.lpg }
        return rates.petrol
    }

    // MARK: Admin writes

    func saveRates(petrol: Double, diesel: Double, highOctane: Double) async {
        do {
            try await db.collection(ZyphuelConfig.Collection.systemConfig)
                .document(ZyphuelConfig.ConfigDoc.fuelRates)
                .setData([
                    "petrol": petrol,
                    "diesel": diesel,
                    "highOctane": highOctane,
                    "updatedAt": FieldValue.serverTimestamp()
                ], merge: true)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func saveGateway(webhookURL: String, senderEmail: String) async {
        do {
            try await db.collection(ZyphuelConfig.Collection.systemConfig)
                .document(ZyphuelConfig.ConfigDoc.emailGateway)
                .setData([
                    "webhookURL": webhookURL,
                    "senderEmail": senderEmail,
                    "updatedAt": FieldValue.serverTimestamp()
                ], merge: true)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Transactional email

/// Android can speak SMTP from a socket. iOS apps cannot hold raw SMTP sockets on a
/// background thread reliably, so the iOS build uses the same two server-side channels
/// the Android email engine already trusts: the HTTPS webhook relay and the Firestore
/// `mail` collection trigger. Both are already deployed for Android devices.
@MainActor
final class EmailGateway {

    enum Channel: String {
        case webhook = "HTTPS Webhook Relay"
        case firestore = "Firestore Mail Trigger"
    }

    struct Result {
        var channel: Channel
        var delivered: Bool
        var detail: String
    }

    private let db = Firestore.firestore()

    /// Queues one email. The `uid` field satisfies the hardened `mail` rule that binds a
    /// sender to their own auth uid and allows exactly one recipient per document.
    func send(
        to recipient: String,
        subject: String,
        body: String,
        senderUID: String,
        gateway: EmailGatewayConfig
    ) async -> Result {
        guard SecurityValidator.isEmailShaped(recipient) else {
            return Result(channel: .firestore, delivered: false, detail: "Invalid recipient address.")
        }

        if let webhook = gateway.webhookURL, let url = URL(string: webhook), !webhook.isEmpty {
            if await postWebhook(url: url, to: recipient, subject: subject, body: body) {
                return Result(channel: .webhook, delivered: true, detail: "Queued via HTTPS relay.")
            }
        }

        do {
            try await db.collection(ZyphuelConfig.Collection.mail).addDocument(data: [
                "to": [recipient.lowercased()],
                "uid": senderUID,
                "recipient": recipient.lowercased(),
                "message": [
                    "subject": SecurityValidator.sanitised(subject, maxLength: 160),
                    "text": SecurityValidator.sanitised(body, maxLength: 90_000),
                    "html": ""
                ],
                "createdAt": FieldValue.serverTimestamp(),
                "deliveryStatus": "queued"
            ])
            return Result(channel: .firestore, delivered: true, detail: "Queued for the mail trigger.")
        } catch {
            return Result(channel: .firestore, delivered: false, detail: error.localizedDescription)
        }
    }

    private func postWebhook(url: URL, to: String, subject: String, body: String) async -> Bool {
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 12
        let payload: [String: Any] = [
            "to": to,
            "subject": SecurityValidator.sanitised(subject, maxLength: 160),
            "body": SecurityValidator.sanitised(body, maxLength: 90_000),
            "app": "Zyphuel iOS"
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: payload) else { return false }
        request.httpBody = data
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse else { return false }
            return (200..<300).contains(http.statusCode)
        } catch {
            return false
        }
    }

    /// Order confirmation template shared with Android wording.
    static func orderConfirmation(order: FuelOrder, statusLine: String) -> (subject: String, body: String) {
        let subject = "Zyphuel order \(order.id.prefix(8).uppercased()) • \(order.status.rawValue)"
        let body = """
        Zyphuel — doorstep energy, Lahore

        Order: \(order.id)
        Service: \(order.serviceType)
        Volume: \(order.quantityLiters)L
        Unit price: \(MoneyFormatter.pkr(order.unitPrice))
        Delivery fee: \(MoneyFormatter.pkr(order.deliveryFee))
        COD total: \(order.totalPriceLabel)
        Address: \(order.deliveryAddress)
        Status: \(statusLine)

        Questions? WhatsApp \(ZyphuelConfig.whatsappDisplay).
        """
        return (subject, body)
    }
}

extension SecurityValidator {
    static func isEmailShaped(_ value: String) -> Bool {
        (try? validateEmail(value)) != nil
    }
}
