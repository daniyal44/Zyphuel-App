import Combine
import CryptoKit
import Foundation
import LocalAuthentication
import Security

// MARK: - PBKDF2-HMAC-SHA256

/// Android uses `PBKDF2WithHmacSHA256` with 600,000 iterations (`SecurityCrypto.kt`).
/// CryptoKit has no PBKDF2 helper, so the same construction is implemented here.
enum PBKDF2 {
    enum Error: Swift.Error { case invalidPassword, emptySalt }

    static func deriveKey(
        password: String,
        salt: Data,
        iterations: Int,
        keyLength: Int = 32
    ) throws -> Data {
        guard let passwordData = password.data(using: .utf8), !passwordData.isEmpty else {
            throw Error.invalidPassword
        }
        guard !salt.isEmpty else { throw Error.emptySalt }
        precondition(iterations > 0 && keyLength > 0)

        let key = SymmetricKey(data: passwordData)
        let hashLength = 32
        let blocks = Int(ceil(Double(keyLength) / Double(hashLength)))
        var derived = Data()

        for blockIndex in 1...blocks {
            var blockSalt = salt
            var bigEndian = UInt32(blockIndex).bigEndian
            withUnsafeBytes(of: &bigEndian) { blockSalt.append(contentsOf: $0) }

            var u = Data(HMAC<SHA256>.authenticationCode(for: blockSalt, using: key))
            var t = u

            if iterations > 1 {
                for _ in 1..<iterations {
                    u = Data(HMAC<SHA256>.authenticationCode(for: u, using: key))
                    for index in 0..<t.count {
                        t[index] = t[index] ^ u[index]
                    }
                }
            }
            derived.append(t)
        }

        return derived.prefix(keyLength)
    }
}

/// Hash storage compatible with the Android `pbkdf2$sha256$<iterations>$<salt>$<hash>` format.
enum PasswordHasher {
    static let iterations = 600_000
    static let saltByteCount = 16
    static let keyByteCount = 32
    static let algorithmTag = "pbkdf2$sha256"

    struct StoredHash: Equatable {
        var iterations: Int
        var salt: Data
        var hash: Data

        var encoded: String {
            "\(PasswordHasher.algorithmTag)$\(iterations)$\(salt.base64EncodedString())$\(hash.base64EncodedString())"
        }

        init(iterations: Int, salt: Data, hash: Data) {
            self.iterations = iterations
            self.salt = salt
            self.hash = hash
        }

        init?(encoded: String) {
            let parts = encoded.split(separator: "$", omittingEmptySubsequences: false)
            guard parts.count == 5,
                  parts[0] == "pbkdf2", parts[1] == "sha256",
                  let rounds = Int(parts[2]), rounds > 0,
                  let salt = Data(base64Encoded: String(parts[3])),
                  let hash = Data(base64Encoded: String(parts[4]))
            else { return nil }
            self.init(iterations: rounds, salt: salt, hash: hash)
        }
    }

    static func hash(_ password: String, iterations: Int = PasswordHasher.iterations) throws -> String {
        var saltBytes = [UInt8](repeating: 0, count: saltByteCount)
        let status = SecRandomCopyBytes(kSecRandomDefault, saltBytes.count, &saltBytes)
        guard status == errSecSuccess else {
            throw NSError(domain: NSOSStatusErrorDomain, code: Int(status))
        }
        let salt = Data(saltBytes)
        let derived = try PBKDF2.deriveKey(
            password: password, salt: salt,
            iterations: iterations, keyLength: keyByteCount
        )
        return StoredHash(iterations: iterations, salt: salt, hash: derived).encoded
    }

    /// Constant-time comparison; never short-circuits on the first differing byte.
    static func verify(_ password: String, against encoded: String) -> Bool {
        guard let stored = StoredHash(encoded: encoded) else { return false }
        guard let candidate = try? PBKDF2.deriveKey(
            password: password, salt: stored.salt,
            iterations: stored.iterations, keyLength: stored.hash.count
        ) else { return false }
        return constantTimeEquals(candidate, stored.hash)
    }

    /// Older Android builds used 120k iterations; those hashes are upgraded on next login.
    static func needsRehash(_ encoded: String) -> Bool {
        guard let stored = StoredHash(encoded: encoded) else { return true }
        return stored.iterations < iterations
    }

    private static func constantTimeEquals(_ lhs: Data, _ rhs: Data) -> Bool {
        guard lhs.count == rhs.count else { return false }
        var difference: UInt8 = 0
        for index in 0..<lhs.count {
            difference |= lhs[index] ^ rhs[index]
        }
        return difference == 0
    }
}

// MARK: - Rate limiter

/// Port of `SecurityRateLimiter.kt`: 4 attempts inside 1 minute lock the account
/// for a 5-minute cooldown, and the surfaced error carries the Android error code
/// 422 so support scripts and screenshots stay consistent.
struct SecurityRateLimiter {
    static let maxAttempts = 4
    static let attemptWindow: TimeInterval = 60
    static let cooldown: TimeInterval = 300
    static let errorCode = 422

    private(set) var failedAttempts: [Date] = []
    private(set) var lockedUntil: Date?

    struct LockoutError: LocalizedError, Equatable {
        var remainingSeconds: Int
        var errorCode: Int { SecurityRateLimiter.errorCode }
        var errorDescription: String? {
            "Too many incorrect attempts. Try again in \(remainingSeconds)s."
        }
    }

    mutating func isLocked(now: Date = Date()) -> Bool {
        guard let until = lockedUntil else { return false }
        if now >= until {
            lockedUntil = nil
            return false
        }
        return true
    }

    mutating func remainingLockSeconds(now: Date = Date()) -> Int {
        guard let until = lockedUntil, until > now else { return 0 }
        return Int(ceil(until.timeIntervalSince(now)))
    }

    /// Records a failure. Returns a lockout error once the limit is crossed.
    mutating func registerFailure(now: Date = Date()) throws {
        if lockedUntil != nil, isLocked(now: now) {
            throw LockoutError(remainingSeconds: remainingLockSeconds(now: now))
        }
        failedAttempts = failedAttempts.filter { now.timeIntervalSince($0) < attemptWindow }
        failedAttempts.append(now)

        if failedAttempts.count >= Self.maxAttempts {
            lockedUntil = now.addingTimeInterval(cooldown)
            failedAttempts.removeAll()
            throw LockoutError(remainingSeconds: Int(cooldown))
        }
    }

    mutating func registerSuccess() {
        failedAttempts.removeAll()
        lockedUntil = nil
    }

    var remainingAttempts: Int {
        max(0, Self.maxAttempts - failedAttempts.count)
    }
}

// MARK: - Input validation

enum SecurityValidator {
    enum ValidationError: LocalizedError, Equatable {
        case invalidEmail
        case invalidPhone
        case weakPassword
        case nameTooShort
        case addressTooShort
        case unsupportedDocument

        var errorDescription: String? {
            switch self {
            case .invalidEmail: return "Enter a valid email address."
            case .invalidPhone: return "Enter a valid Pakistani mobile number (03XX-XXXXXXX)."
            case .weakPassword: return "Password must be at least 8 characters and include a letter and a number."
            case .nameTooShort: return "Enter your full name."
            case .addressTooShort: return "Enter a complete delivery address."
            case .unsupportedDocument: return "Upload a PDF, JPEG or PNG file."
            }
        }
    }

    static func validateEmail(_ email: String) throws {
        let pattern = #"^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"#
        guard email.range(of: pattern, options: .regularExpression) != nil else {
            throw ValidationError.invalidEmail
        }
    }

    /// Accepts 03XXXXXXXXX, +923XXXXXXXXX and 00923XXXXXXXXX, mirroring the Android validator.
    static func validatePhone(_ phone: String) throws {
        let digits = phone.filter(\.isNumber)
        let normalised: String
        if digits.hasPrefix("0092") {
            normalised = "0" + digits.dropFirst(4)
        } else if digits.hasPrefix("92") {
            normalised = "0" + digits.dropFirst(2)
        } else {
            normalised = digits
        }
        guard normalised.count == 11, normalised.hasPrefix("03") else {
            throw ValidationError.invalidPhone
        }
    }

    static func validatePassword(_ password: String) throws {
        guard password.count >= 8,
              password.rangeOfCharacter(from: .letters) != nil,
              password.rangeOfCharacter(from: .decimalDigits) != nil
        else { throw ValidationError.weakPassword }
    }

    static func validateName(_ name: String) throws {
        guard name.trimmingCharacters(in: .whitespacesAndNewlines).count >= 3 else {
            throw ValidationError.nameTooShort
        }
    }

    static func validateAddress(_ address: String) throws {
        guard address.trimmingCharacters(in: .whitespacesAndNewlines).count >= 6 else {
            throw ValidationError.addressTooShort
        }
    }

    /// Basic upload guard: extension allow-list and a size ceiling (5 MB, same as Android).
    static func validateUpload(fileName: String, byteCount: Int) throws {
        let allowed = ["pdf", "jpg", "jpeg", "png"]
        let ext = (fileName as NSString).pathExtension.lowercased()
        guard allowed.contains(ext) else { throw ValidationError.unsupportedDocument }
        guard byteCount <= 5 * 1024 * 1024 else { throw ValidationError.unsupportedDocument }
    }

    /// Strips control characters before anything is written to Firestore or logs.
    static func sanitised(_ value: String, maxLength: Int = 500) -> String {
        let filtered = value.unicodeScalars.filter { !CharacterSet.controlCharacters.contains($0) }
        return String(String.UnicodeScalarView(filtered)).prefix(maxLength).description
    }
}

// MARK: - Keychain

/// Mirrors `SecureStorageManager` (EncryptedSharedPreferences): small secrets live in the
/// iOS Keychain instead of UserDefaults.
enum KeychainStore {
    private static let service = ZyphuelConfig.bundleIdentifier + ".secure"

    enum Key: String {
        case sessionEmail = "session.email"
        case sessionRole = "session.role"
        case rateLimiterState = "security.rate_limiter"
        case biometricLockEnabled = "security.biometric_lock"
    }

    @discardableResult
    static func set(_ value: String, for key: Key) -> Bool {
        guard let data = value.data(using: .utf8) else { return false }
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue
        ]
        SecItemDelete(query as CFDictionary)
        var attributes = query
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        return SecItemAdd(attributes as CFDictionary, nil) == errSecSuccess
    }

    static func get(_ key: Key) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data
        else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func remove(_ key: Key) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue
        ]
        SecItemDelete(query as CFDictionary)
    }

    static func reset() {
        [Key.sessionEmail, .sessionRole, .rateLimiterState, .biometricLockEnabled]
            .forEach(remove)
    }
}

// MARK: - Biometrics

/// Face ID / Touch ID gate. Android uses `BiometricSecurityManager` with the same
/// "unlock the app" purpose.
@MainActor
final class BiometricLock: ObservableObject {
    @Published private(set) var isEnabled: Bool
    @Published private(set) var isLocked: Bool
    @Published private(set) var lastError: String?

    private let context = LAContext()

    init() {
        let enabled = KeychainStore.get(.biometricLockEnabled) == "true"
        self.isEnabled = enabled
        self.isLocked = enabled
    }

    var biometryLabel: String {
        switch context.biometryType {
        case .faceID: return "Face ID"
        case .touchID: return "Touch ID"
        case .opticID: return "Optic ID"
        default: return "Biometrics"
        }
    }

    var isBiometryAvailable: Bool {
        context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: nil)
    }

    func setEnabled(_ enabled: Bool) {
        isEnabled = enabled
        KeychainStore.set(enabled ? "true" : "false", for: .biometricLockEnabled)
        isLocked = enabled
    }

    func unlock() async -> Bool {
        guard isEnabled else {
            isLocked = false
            return true
        }
        let context = LAContext()
        context.localizedCancelTitle = "Use password"
        do {
            let success = try await context.evaluatePolicy(
                .deviceOwnerAuthenticationWithBiometrics,
                localizedReason: "Unlock Zyphuel to continue"
            )
            isLocked = !success
            lastError = success ? nil : "Biometric check failed."
            return success
        } catch {
            isLocked = true
            lastError = error.localizedDescription
            return false
        }
    }

    /// Called after a successful password login so the user is not asked twice.
    func markUnlocked() {
        isLocked = false
    }
}
