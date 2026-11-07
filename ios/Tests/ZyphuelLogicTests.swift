import XCTest
@testable import Zyphuel

// MARK: - Pricing parity

final class PricingEngineTests: XCTestCase {

    func testTieredDeliveryFeesMatchAndroid() {
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 0), 0)
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 1), 300)
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 5), 300)
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 7), 300)
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 10), 300)
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 11), 320)
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 12), 340)
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 13), 360)
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 14), 380)
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 15), 400)
        // The Android `else` branch also returns 400; the cap is enforced by `price()`.
        XCTAssertEqual(PricingEngine.deliveryFee(liters: 16), 400)
    }

    func testPumpMarkupAddsFiveRupeesPerLitre() {
        XCTAssertEqual(PricingEngine.pumpRate(base: 289.38, serviceType: "Super Euro-V Petrol"),
                       294.38, accuracy: 0.0001)
        XCTAssertEqual(PricingEngine.pumpRate(base: 289.84, serviceType: "High-Speed Diesel"),
                       294.84, accuracy: 0.0001)
        XCTAssertEqual(PricingEngine.pumpRate(base: 355.00, serviceType: "High-Octane (RON 97)"),
                       360.00, accuracy: 0.0001)
        // Non-fuel services are never marked up.
        XCTAssertEqual(PricingEngine.pumpRate(base: 1500, serviceType: "General Inspection"), 1500)
    }

    func testPriceBreakdownForTenLitres() throws {
        let breakdown = try PricingEngine.price(
            baseRate: 326.17, liters: 10, serviceType: "Super Euro-V Petrol"
        )
        XCTAssertEqual(breakdown.unitPrice, 331.17, accuracy: 0.0001)
        XCTAssertEqual(breakdown.subtotal, 3311.70, accuracy: 0.01)
        XCTAssertEqual(breakdown.deliveryFee, 300, accuracy: 0.0001)
        XCTAssertEqual(breakdown.total, 3611.70, accuracy: 0.01)
    }

    func testPriceRejectsOverLimitAndZero() {
        XCTAssertThrowsError(try PricingEngine.price(baseRate: 300, liters: 16, serviceType: "Petrol"))
        XCTAssertThrowsError(try PricingEngine.price(baseRate: 300, liters: 0, serviceType: "Petrol"))
    }

    func testWaterAndLpgAreBlocked() {
        XCTAssertTrue(PricingEngine.isServiceUnavailable("Pure Water Delivery"))
        XCTAssertTrue(PricingEngine.isServiceUnavailable("LPG Cylinder"))
        XCTAssertTrue(PricingEngine.isServiceUnavailable("gas cylinder"))
        XCTAssertFalse(PricingEngine.isServiceUnavailable("Super Euro-V Petrol"))
        XCTAssertThrowsError(try PricingEngine.price(baseRate: 0, liters: 5, serviceType: "LPG Gas"))
    }

    func testClampKeepsVolumeInsideLegalRange() {
        XCTAssertEqual(PricingEngine.clampLiters(0), 1)
        XCTAssertEqual(PricingEngine.clampLiters(9), 9)
        XCTAssertEqual(PricingEngine.clampLiters(99), 15)
    }

    func testMoneyFormattingUsesTwoDecimals() {
        XCTAssertEqual(MoneyFormatter.pkr(3611.7), "Rs. 3,611.70")
        XCTAssertEqual(MoneyFormatter.pkr(0), "Rs. 0.00")
    }
}

// MARK: - Operating hours

final class OperatingHoursTests: XCTestCase {

    private func date(_ year: Int, _ month: Int, _ day: Int, _ hour: Int, _ minute: Int = 0) -> Date {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = OperatingHours.timeZone
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = day
        components.hour = hour
        components.minute = minute
        guard let value = calendar.date(from: components) else {
            XCTFail("Could not build a Pakistan-time date")
            return Date()
        }
        return value
    }

    func testThursdayWindow() {
        // 5 Nov 2026 is a Thursday.
        XCTAssertFalse(OperatingHours.isDeliveryOpen(at: date(2026, 11, 5, 7, 59)))
        XCTAssertTrue(OperatingHours.isDeliveryOpen(at: date(2026, 11, 5, 8, 0)))
        XCTAssertTrue(OperatingHours.isDeliveryOpen(at: date(2026, 11, 5, 19, 59)))
        XCTAssertFalse(OperatingHours.isDeliveryOpen(at: date(2026, 11, 5, 20, 0)))
    }

    func testFridayShortDay() {
        // 6 Nov 2026 is a Friday: 08:00 – 13:00.
        XCTAssertTrue(OperatingHours.isDeliveryOpen(at: date(2026, 11, 6, 8, 0)))
        XCTAssertTrue(OperatingHours.isDeliveryOpen(at: date(2026, 11, 6, 12, 59)))
        XCTAssertFalse(OperatingHours.isDeliveryOpen(at: date(2026, 11, 6, 13, 0)))
    }

    func testWeekendWindow() {
        // 7 Nov (Sat) and 8 Nov (Sun) 2026: 10:00 – 18:00.
        XCTAssertFalse(OperatingHours.isDeliveryOpen(at: date(2026, 11, 7, 9, 59)))
        XCTAssertTrue(OperatingHours.isDeliveryOpen(at: date(2026, 11, 7, 10, 0)))
        XCTAssertFalse(OperatingHours.isDeliveryOpen(at: date(2026, 11, 7, 18, 0)))
        XCTAssertTrue(OperatingHours.isDeliveryOpen(at: date(2026, 11, 8, 17, 59)))
    }

    func testStatusCopyMatchesAndroid() {
        let open = OperatingHours.status(at: date(2026, 11, 9, 10, 0))  // Monday
        XCTAssertTrue(open.isOpen)
        XCTAssertEqual(open.title, "Deliveries Open")
        XCTAssertTrue(open.subtitle.contains(OperatingHours.scheduleMonThu))

        let closed = OperatingHours.status(at: date(2026, 11, 9, 22, 0))
        XCTAssertFalse(closed.isOpen)
        XCTAssertEqual(closed.title, "Deliveries Closed")
    }
}

// MARK: - Password hashing and rate limiting

final class SecurityTests: XCTestCase {

    func testPasswordHashRoundTrip() throws {
        let encoded = try PasswordHasher.hash("Zyphuel#2026", iterations: 1_000)
        XCTAssertTrue(PasswordHasher.verify("Zyphuel#2026", against: encoded))
        XCTAssertFalse(PasswordHasher.verify("zyphuel#2026", against: encoded))
        XCTAssertFalse(PasswordHasher.verify("wrong", against: encoded))
    }

    func testEncodedFormatIsAndroidCompatible() throws {
        let encoded = try PasswordHasher.hash("Secret123", iterations: 2_000)
        let parts = encoded.split(separator: "$")
        XCTAssertEqual(parts.count, 5)
        XCTAssertEqual(String(parts[0]), "pbkdf2")
        XCTAssertEqual(String(parts[1]), "sha256")
        XCTAssertEqual(String(parts[2]), "2000")
        XCTAssertNotNil(Data(base64Encoded: String(parts[3])))
        XCTAssertNotNil(Data(base64Encoded: String(parts[4])))
    }

    func testNeedsRehashForLegacyIterationCount() throws {
        let legacy = try PasswordHasher.hash("Legacy#123", iterations: 120_000)
        XCTAssertTrue(PasswordHasher.needsRehash(legacy))
        XCTAssertFalse(PasswordHasher.needsRehash("not-a-hash"))
    }

    func testRateLimiterLocksAfterFourAttempts() {
        var limiter = SecurityRateLimiter()
        let now = Date()
        XCTAssertNoThrow(try limiter.registerFailure(now: now))
        XCTAssertNoThrow(try limiter.registerFailure(now: now.addingTimeInterval(1)))
        XCTAssertNoThrow(try limiter.registerFailure(now: now.addingTimeInterval(2)))
        XCTAssertThrowsError(try limiter.registerFailure(now: now.addingTimeInterval(3))) { error in
            let lockout = error as? SecurityRateLimiter.LockoutError
            XCTAssertEqual(lockout?.errorCode, 422)
            XCTAssertEqual(lockout?.remainingSeconds, 300)
        }
        XCTAssertTrue(limiter.isLocked(now: now.addingTimeInterval(4)))
        // The cooldown starts when the fourth attempt trips the lock (at +3s), so 4s
        // after the first attempt leaves 299 of the 300 seconds.
        XCTAssertEqual(limiter.remainingLockSeconds(now: now.addingTimeInterval(4)), 299)

        limiter.registerSuccess()
        XCTAssertFalse(limiter.isLocked(now: now.addingTimeInterval(5)))
    }

    func testValidatorAcceptsPakistaniNumbers() {
        XCTAssertNoThrow(try SecurityValidator.validatePhone("03001234567"))
        XCTAssertNoThrow(try SecurityValidator.validatePhone("+923001234567"))
        XCTAssertNoThrow(try SecurityValidator.validatePhone("00923001234567"))
        XCTAssertThrowsError(try SecurityValidator.validatePhone("02001234567"))
        XCTAssertThrowsError(try SecurityValidator.validatePhone("123"))
    }

    func testValidatorEmailAndPassword() {
        XCTAssertNoThrow(try SecurityValidator.validateEmail("rider@zyphuel.app"))
        XCTAssertThrowsError(try SecurityValidator.validateEmail("rider@zyphuel"))
        XCTAssertThrowsError(try SecurityValidator.validateEmail("rider zyphuel.app"))
        XCTAssertThrowsError(try SecurityValidator.validatePassword("short"))
        XCTAssertThrowsError(try SecurityValidator.validatePassword("lettersOnly"))
        XCTAssertNoThrow(try SecurityValidator.validatePassword("Zyphuel2026"))
    }

    func testSanitiserStripsControlCharactersAndCapsLength() {
        let dirty = "Ali\u{0000}\nRaza" + String(repeating: "x", count: 20)
        let clean = SecurityValidator.sanitised(dirty, maxLength: 10)
        XCTAssertEqual(clean.count, 10)
        XCTAssertFalse(clean.contains("\u{0000}"))
        XCTAssertFalse(clean.contains("\n"))
    }
}

// MARK: - Order lifecycle

@MainActor
final class OrderLifecycleTests: XCTestCase {

    func testRiderCannotSkipProgression() {
        XCTAssertFalse(OrderService.canTransition(from: .pending, to: .completed, isAdmin: false))
        XCTAssertFalse(OrderService.canTransition(from: .assigned, to: .completed, isAdmin: false))
        XCTAssertTrue(OrderService.canTransition(from: .assigned, to: .delivering, isAdmin: false))
        XCTAssertTrue(OrderService.canTransition(from: .delivering, to: .completed, isAdmin: false))
        XCTAssertTrue(OrderService.canTransition(from: .pending, to: .cancelled, isAdmin: false))
    }

    func testAdminCanDriveFullLifecycle() {
        XCTAssertTrue(OrderService.canTransition(from: .pending, to: .assigned, isAdmin: true))
        XCTAssertTrue(OrderService.canTransition(from: .assigned, to: .delivering, isAdmin: true))
        XCTAssertTrue(OrderService.canTransition(from: .delivering, to: .completed, isAdmin: true))
        XCTAssertFalse(OrderService.canTransition(from: .completed, to: .delivering, isAdmin: true))
        XCTAssertFalse(OrderService.canTransition(from: .cancelled, to: .pending, isAdmin: true))
    }

    func testStatusesAreCurrentOnlyWhileInFlight() {
        XCTAssertTrue(OrderStatus.pending.isCurrent)
        XCTAssertTrue(OrderStatus.assigned.isCurrent)
        XCTAssertTrue(OrderStatus.delivering.isCurrent)
        XCTAssertFalse(OrderStatus.completed.isCurrent)
        XCTAssertFalse(OrderStatus.cancelled.isCurrent)
    }
}

// MARK: - Catalog and identity

final class CatalogTests: XCTestCase {

    func testTenCategoriesWithSevenFeatured() {
        XCTAssertEqual(ServiceCatalog.all.count, 10)
        XCTAssertEqual(ServiceCatalog.featured.count, 7)
        XCTAssertEqual(ServiceCatalog.all.map(\.sortOrder), Array(1...10))
    }

    func testFuelCatalogMatchesAndroidSeed() {
        let fuel = ServiceCatalog.category(id: "fuel_energy")
        XCTAssertNotNil(fuel)
        let petrol = fuel?.items.first { $0.id == "petrol_regular" }
        XCTAssertEqual(petrol?.basePrice, 326.17)
        XCTAssertEqual(petrol?.unit, "L")
        let octane = fuel?.items.first { $0.id == "petrol_octane" }
        XCTAssertEqual(octane?.basePrice, 355.00)
        let diesel = fuel?.items.first { $0.id == "diesel_regular" }
        XCTAssertEqual(diesel?.basePrice, 340.86)
    }

    func testWaterAndLpgItemsAreInactive() {
        let blocked = ServiceCatalog.activeItems.filter {
            $0.name.localizedCaseInsensitiveContains("water")
                || $0.name.localizedCaseInsensitiveContains("lpg")
        }
        XCTAssertTrue(blocked.isEmpty)
    }

    func testSuperAdminRecognitionIsCaseInsensitive() {
        XCTAssertTrue(AppUser.isSuperAdminEmail(ZyphuelConfig.masterAdminEmail))
        XCTAssertTrue(AppUser.isSuperAdminEmail(ZyphuelConfig.masterAdminEmail.uppercased()))
        XCTAssertFalse(AppUser.isSuperAdminEmail("someone@else.com"))
    }
}
