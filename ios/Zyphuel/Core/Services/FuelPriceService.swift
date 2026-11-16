import Combine
import Foundation

// MARK: - Fuel Price Result

struct FuelPriceFetchResult: Codable {
    let petrol: Double
    let diesel: Double
    let highOctane: Double
    let source: String
    let effectiveDate: String?

    var isValid: Bool { petrol > 0 && diesel > 0 }
}

// MARK: - Fuel Price Service

/// Multi-source Pakistan fuel price fetcher (mirror of Android's TrackmateFuelApiService).
/// Priority chain:
///   1. Trackmate Fuel API (aggregated PSO/Shell/OGRA data)
///   2. PSO Pakistan official website scraping
///   3. Shell Pakistan official website scraping
///   4. Codeistry Pakistan fuel prices API
///
/// Each source is tried in order; the first successful response wins.
actor FuelPriceService {

    static let shared = FuelPriceService()

    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 12
        config.timeoutIntervalForResource = 20
        return URLSession(configuration: config)
    }()

    // MARK: - Public API

    /// Fetches latest Pakistan fuel prices using multi-source fallback chain.
    func fetchLatestPrices() async throws -> FuelPriceFetchResult {
        // Source 1: Trackmate API
        if let result = try? await fetchFromTrackmate(), result.isValid {
            return result
        }

        // Source 2: PSO website scraping
        if let result = try? await fetchFromPsoWebsite(), result.isValid {
            return result
        }

        // Source 3: Shell website scraping
        if let result = try? await fetchFromShellWebsite(), result.isValid {
            return result
        }

        // Source 4: Codeistry API
        if let result = try? await fetchFromCodeistry(), result.isValid {
            return result
        }

        throw FuelPriceError.allSourcesFailed
    }

    // MARK: - Source 1: Trackmate API

    private func fetchFromTrackmate() async throws -> FuelPriceFetchResult {
        let url = URL(string: "https://fuel.trackmate.page/api/prices")!
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("ZyphuelApp/2.0 (iOS)", forHTTPHeaderField: "User-Agent")

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw FuelPriceError.httpError
        }

        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
        guard let pricesArray = json["prices"] as? [[String: Any]] else {
            throw FuelPriceError.parseError
        }

        var foundPetrol: Double?
        var foundDiesel: Double?
        var foundOctane: Double?
        var effectiveDate: String?

        for item in pricesArray {
            let product = (item["product"] as? String ?? "").lowercased()
            let source = (item["source"] as? String ?? "").lowercased()
            let price = item["price_pkr"] as? Double ?? 0
            let effDate = item["effective_date"] as? String ?? ""

            if !effDate.isEmpty && effectiveDate == nil { effectiveDate = effDate }
            guard price > 0 else { continue }

            switch product {
            case "petrol":
                if foundPetrol == nil || source == "pso" { foundPetrol = price }
            case "hsd":
                if foundDiesel == nil || source == "pso" || source == "shell" { foundDiesel = price }
            case "octane_plus", "hobc", "high_octane":
                if foundOctane == nil || source == "pso" { foundOctane = price }
            default: break
            }
        }

        return FuelPriceFetchResult(
            petrol: foundPetrol ?? 0,
            diesel: foundDiesel ?? 0,
            highOctane: foundOctane ?? 0,
            source: "Trackmate Fuel API (PSO/Shell)",
            effectiveDate: effectiveDate
        )
    }

    // MARK: - Source 2: PSO Website

    private func fetchFromPsoWebsite() async throws -> FuelPriceFetchResult {
        let url = URL(string: "https://psopk.com/en/products-and-services/product-prices")!
        var request = URLRequest(url: url)
        request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) ZyphuelApp", forHTTPHeaderField: "User-Agent")

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw FuelPriceError.httpError
        }

        let html = String(data: data, encoding: .utf8) ?? ""
        return parseFuelPricesFromHtml(html, source: "PSO Pakistan Official")
    }

    // MARK: - Source 3: Shell Website

    private func fetchFromShellWebsite() async throws -> FuelPriceFetchResult {
        let url = URL(string: "https://www.shell.com.pk/motorists/shell-fuels/fuel-pricing.html")!
        var request = URLRequest(url: url)
        request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) ZyphuelApp", forHTTPHeaderField: "User-Agent")

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw FuelPriceError.httpError
        }

        let html = String(data: data, encoding: .utf8) ?? ""
        return parseFuelPricesFromHtml(html, source: "Shell Pakistan Official")
    }

    // MARK: - Source 4: Codeistry API

    private func fetchFromCodeistry() async throws -> FuelPriceFetchResult {
        let url = URL(string: "https://api.codeistry.com.pk/fuel-prices")!
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("ZyphuelApp/2.0 (iOS)", forHTTPHeaderField: "User-Agent")

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw FuelPriceError.httpError
        }

        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
        let petrol = json["petrol"] as? Double ?? 0
        let diesel = (json["diesel"] as? Double) ?? (json["high_speed_diesel"] as? Double) ?? 0
        let octane = (json["high_octane"] as? Double) ?? (json["hobc"] as? Double) ?? 0

        return FuelPriceFetchResult(
            petrol: petrol,
            diesel: diesel,
            highOctane: octane,
            source: "Codeistry Pakistan Fuel API",
            effectiveDate: nil
        )
    }

    // MARK: - HTML Parser

    private func parseFuelPricesFromHtml(_ html: String, source: String) -> FuelPriceFetchResult {
        let pricePattern = try! NSRegularExpression(
            pattern: #"(?:Rs\.?|PKR)\s*(\d{2,3}(?:\.\d{1,2})?)"#,
            options: .caseInsensitive
        )

        var petrol = 0.0
        var diesel = 0.0
        var octane = 0.0

        let lines = html.components(separatedBy: CharacterSet.newlines)
            .flatMap { $0.components(separatedBy: "<tr>") }
            .flatMap { $0.components(separatedBy: "<td>") }
            .flatMap { $0.components(separatedBy: "<div>") }

        for (i, line) in lines.enumerated() {
            let lower = line.lowercased()
            let searchLines = [line] + (i + 1 < lines.count ? [lines[i + 1]] : []) + (i + 2 < lines.count ? [lines[i + 2]] : [])

            var price: Double?
            for searchLine in searchLines {
                let nsRange = NSRange(searchLine.startIndex..., in: searchLine)
                if let match = pricePattern.firstMatch(in: searchLine, range: nsRange),
                   let range = Range(match.range(at: 1), in: searchLine) {
                    price = Double(searchLine[range])
                    break
                }
            }

            guard let p = price, p >= 100, p <= 600 else { continue }

            if petrol == 0 && (lower.contains("petrol") || lower.contains("motor spirit") || lower.contains("mogas") || lower.contains("super")) && !lower.contains("octane") && !lower.contains("hobc") {
                petrol = p
            } else if diesel == 0 && (lower.contains("diesel") || lower.contains("hsd") || lower.contains("high speed") || lower.contains("high-speed")) {
                diesel = p
            } else if octane == 0 && (lower.contains("octane") || lower.contains("hobc") || lower.contains("ron 97") || lower.contains("hi-octane")) {
                octane = p
            }
        }

        return FuelPriceFetchResult(petrol: petrol, diesel: diesel, highOctane: octane, source: source, effectiveDate: nil)
    }

    // MARK: - Errors

    enum FuelPriceError: LocalizedError {
        case allSourcesFailed
        case httpError
        case parseError

        var errorDescription: String? {
            switch self {
            case .allSourcesFailed: return "Unable to fetch fuel prices from any source."
            case .httpError: return "Network request failed."
            case .parseError: return "Could not parse fuel price data."
            }
        }
    }
}

// MARK: - Fuel Price Sync Manager

/// Manages periodic fuel price synchronization on iOS.
/// Mirrors Android's FuelPriceWorker (WorkManager).
/// Runs on app launch + every 4 hours while app is active.
@MainActor
final class FuelPriceSyncManager: ObservableObject {

    static let shared = FuelPriceSyncManager()

    @Published private(set) var lastPetrol: Double = 0
    @Published private(set) var lastDiesel: Double = 0
    @Published private(set) var lastHighOctane: Double = 0
    @Published private(set) var lastSource: String = ""
    @Published private(set) var lastUpdated: Date?
    @Published private(set) var isSyncing = false

    private var syncTimer: Timer?
    private let defaults = UserDefaults.standard
    private let syncIntervalHours: TimeInterval = 4

    private init() {
        loadCached()
    }

    /// Start periodic sync. Call from AppState.bootstrap().
    func startPeriodicSync() {
        // Sync immediately on launch
        Task { await syncNow() }

        // Schedule timer for every 4 hours
        syncTimer?.invalidate()
        syncTimer = Timer.scheduledTimer(withTimeInterval: syncIntervalHours * 3600, repeats: true) { [weak self] _ in
            Task { @MainActor in
                await self?.syncNow()
            }
        }
    }

    func stopSync() {
        syncTimer?.invalidate()
        syncTimer = nil
    }

    /// Force an immediate sync (e.g., pull-to-refresh).
    func syncNow() async {
        guard !isSyncing else { return }
        isSyncing = true
        defer { isSyncing = false }

        do {
            let result = try await FuelPriceService.shared.fetchLatestPrices()
            lastPetrol = result.petrol
            lastDiesel = result.diesel
            lastHighOctane = result.highOctane
            lastSource = result.source
            lastUpdated = Date()
            saveToDisk(result)

            // Update the in-memory service catalog so UI shows live prices
            ServiceCatalog.updateFuelPrices(
                petrol: result.petrol,
                diesel: result.diesel,
                highOctane: result.highOctane
            )
        } catch {
            // Keep using cached prices
            print("[FuelPriceSync] All sources failed: \(error.localizedDescription)")
        }
    }

    // MARK: - Persistence

    private func saveToDisk(_ result: FuelPriceFetchResult) {
        defaults.set(result.petrol, forKey: "zyphuel_price_petrol")
        defaults.set(result.diesel, forKey: "zyphuel_price_diesel")
        defaults.set(result.highOctane, forKey: "zyphuel_price_high_octane")
        defaults.set(result.source, forKey: "zyphuel_price_source")
        defaults.set(Date().timeIntervalSince1970, forKey: "zyphuel_price_last_updated")
    }

    private func loadCached() {
        lastPetrol = defaults.double(forKey: "zyphuel_price_petrol")
        lastDiesel = defaults.double(forKey: "zyphuel_price_diesel")
        lastHighOctane = defaults.double(forKey: "zyphuel_price_high_octane")
        lastSource = defaults.string(forKey: "zyphuel_price_source") ?? ""
        let ts = defaults.double(forKey: "zyphuel_price_last_updated")
        if ts > 0 { lastUpdated = Date(timeIntervalSince1970: ts) }

        // Apply cached prices to catalog immediately
        if lastPetrol > 0 && lastDiesel > 0 {
            ServiceCatalog.updateFuelPrices(petrol: lastPetrol, diesel: lastDiesel, highOctane: lastHighOctane)
        }
    }
}
