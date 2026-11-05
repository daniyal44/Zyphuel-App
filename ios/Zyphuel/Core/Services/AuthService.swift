import FirebaseAuth
import FirebaseCore
import Foundation
import GoogleSignIn

enum AuthError: LocalizedError {
    case missingPresenter
    case missingIDToken
    case notConfigured
    case cancelled

    var errorDescription: String? {
        switch self {
        case .missingPresenter: return "Could not open the Google sign-in sheet. Try again."
        case .missingIDToken: return "Google did not return an identity token. Try again."
        case .notConfigured: return "Firebase is not configured. Add GoogleService-Info.plist first."
        case .cancelled: return "Sign-in cancelled."
        }
    }
}

/// Firebase Auth facade. Mirrors `AuthManager` / `AuthRepository` on Android:
/// email+password plus Google, with one observable session for the whole app.
@MainActor
final class AuthService: ObservableObject {
    @Published private(set) var firebaseUser: User?
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?

    private var handle: AuthStateDidChangeListenerHandle?

    var isConfigured: Bool { FirebaseApp.app() != nil }
    var isSignedIn: Bool { firebaseUser != nil }
    var currentEmail: String { firebaseUser?.email ?? "" }
    var currentUID: String { firebaseUser?.uid ?? "" }

    init() {
        guard isConfigured else { return }
        handle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in
                self?.firebaseUser = user
            }
        }
    }

    deinit {
        if let handle { Auth.auth().removeStateDidChangeListener(handle) }
    }

    // MARK: Email + password

    func signIn(email: String, password: String) async throws -> User {
        try SecurityValidator.validateEmail(email)
        guard isConfigured else { throw AuthError.notConfigured }
        isLoading = true
        defer { isLoading = false }
        do {
            let result = try await Auth.auth().signIn(withEmail: email, password: password)
            firebaseUser = result.user
            KeychainStore.set(result.user.email ?? email, for: .sessionEmail)
            return result.user
        } catch {
            let message = Self.friendlyMessage(for: error)
            errorMessage = message
            throw NSError(domain: "ZyphuelAuth", code: (error as NSError).code,
                          userInfo: [NSLocalizedDescriptionKey: message])
        }
    }

    func register(email: String, password: String, name: String) async throws -> User {
        try SecurityValidator.validateEmail(email)
        try SecurityValidator.validatePassword(password)
        try SecurityValidator.validateName(name)
        guard isConfigured else { throw AuthError.notConfigured }
        isLoading = true
        defer { isLoading = false }
        do {
            let result = try await Auth.auth().createUser(withEmail: email, password: password)
            let change = result.user.createProfileChangeRequest()
            change.displayName = name
            try await change.commitChanges()
            firebaseUser = result.user
            return result.user
        } catch {
            let message = Self.friendlyMessage(for: error)
            errorMessage = message
            throw NSError(domain: "ZyphuelAuth", code: (error as NSError).code,
                          userInfo: [NSLocalizedDescriptionKey: message])
        }
    }

    func sendPasswordReset(email: String) async throws {
        try SecurityValidator.validateEmail(email)
        guard isConfigured else { throw AuthError.notConfigured }
        do {
            try await Auth.auth().sendPasswordReset(withEmail: email)
        } catch {
            throw NSError(domain: "ZyphuelAuth", code: (error as NSError).code,
                          userInfo: [NSLocalizedDescriptionKey: Self.friendlyMessage(for: error)])
        }
    }

    // MARK: Google

    func signInWithGoogle() async throws -> User {
        guard isConfigured else { throw AuthError.notConfigured }
        guard let presenter = Self.topViewController() else { throw AuthError.missingPresenter }

        isLoading = true
        defer { isLoading = false }

        let signInResult: GIDSignInResult
        do {
            signInResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
        } catch {
            let nsError = error as NSError
            if nsError.code == GIDSignInError.canceled.rawValue {
                throw AuthError.cancelled
            }
            throw error
        }

        guard let idToken = signInResult.user.idToken?.tokenString else {
            throw AuthError.missingIDToken
        }
        let credential = GoogleAuthProvider.credential(
            withIDToken: idToken,
            accessToken: signInResult.user.accessToken.tokenString
        )
        let result = try await Auth.auth().signIn(with: credential)
        firebaseUser = result.user
        return result.user
    }

    // MARK: Session

    func signOut() {
        try? Auth.auth().signOut()
        GIDSignIn.sharedInstance.signOut()
        firebaseUser = nil
        KeychainStore.remove(.sessionEmail)
        KeychainStore.remove(.sessionRole)
    }

    func refreshUser() async {
        guard isConfigured, let user = Auth.auth().currentUser else { return }
        do {
            try await user.reload()
            firebaseUser = Auth.auth().currentUser
        } catch {
            // Reload is best-effort; a stale profile never blocks the session.
        }
    }

    // MARK: Helpers

    static func friendlyMessage(for error: Error) -> String {
        let nsError = error as NSError
        guard nsError.domain == AuthErrorDomain,
              let code = AuthErrorCode(rawValue: nsError.code) else {
            return error.localizedDescription
        }
        switch code {
        case .invalidEmail: return "That email address is not valid."
        case .wrongPassword, .invalidCredential: return "Incorrect email or password."
        case .userNotFound: return "No Zyphuel account exists for this email."
        case .emailAlreadyInUse: return "An account already exists for this email."
        case .weakPassword: return "Choose a stronger password (8+ characters)."
        case .tooManyRequests: return "Too many attempts. Please wait and try again."
        case .networkError: return "No internet connection. Check your network and retry."
        case .userDisabled: return "This account has been disabled. Contact Zyphuel support."
        default: return nsError.localizedDescription
        }
    }

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
        guard let windowScene = scenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }),
            let root = windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController
        else { return nil }

        var top = root
        while let presented = top.presentedViewController {
            top = presented
        }
        if let navigation = top as? UINavigationController {
            return navigation.visibleViewController ?? navigation
        }
        return top
    }
}
