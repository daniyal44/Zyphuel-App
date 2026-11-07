import Combine
import FirebaseFirestore
import Foundation
import UIKit
import UserNotifications

/// Push + in-app notification bridge. Android uses FCM directly; iOS uses APNs through
/// FCM, and stores the token on the user document like the Android client does.
@MainActor
final class NotificationService: ObservableObject {

    @Published private(set) var isAuthorized = false
    @Published private(set) var deviceToken: String?

    // Lazy so a Firebase-less setup mode (and the unit-test host) can launch.
    private lazy var db = Firestore.firestore()
    private var observer: NSObjectProtocol?

    init() {
        observer = NotificationCenter.default.addObserver(
            forName: .zyphuelFCMToken,
            object: nil,
            queue: .main
        ) { [weak self] note in
            guard let token = note.object as? String else { return }
            Task { @MainActor in
                self?.deviceToken = token
            }
        }
    }

    deinit {
        if let observer { NotificationCenter.default.removeObserver(observer) }
    }

    func requestAuthorization() async {
        do {
            let granted = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
            isAuthorized = granted
            if granted {
                UIApplication.shared.registerForRemoteNotifications()
            }
        } catch {
            isAuthorized = false
        }
    }

    /// Persists the token so server-side dispatch can target this device.
    /// Field is additive; if the deployed rules reject it the app ignores the failure.
    func saveToken(forEmail email: String) async {
        guard let token = deviceToken, !email.isEmpty else { return }
        let reference = db.collection(ZyphuelConfig.Collection.users)
            .document(UserService.documentId(for: email))
        try? await reference.setData([
            "fcmToken": token,
            "lastDevicePlatform": "iOS"
        ], merge: true)
    }

    /// Local notification used for status changes while the app is foregrounded,
    /// matching the Android `ZyphuelNotificationHelper` channel behaviour.
    func notify(title: String, body: String) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )
        try? await UNUserNotificationCenter.current().add(request)
    }
}
