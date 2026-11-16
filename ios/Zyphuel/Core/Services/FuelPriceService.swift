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
///   1. OilPrices.pk API (FREE, no API key, JSON, OGRA official prices, CC BY 4.0)
///   2. TrackMate Fuel API (FREE, no API key, JSON, aggregates PSO/Shell/PakWheels)
///   3. HamariWeb Pakistan (HTML scraping fallback)
///   4. GlobalPetrolPrices.com (HTML scraping fallback)
///   5. PSO Pakistan website (JSON-in-script + HTML scraping)
///   6. Shell Pakistan website (JSON-in-script + HTML scraping)
///
/// Sources 1 & 2 are proper JSON APIs — reliable and fast.
/// Sources 3-6 are HTML scrapers kept as emergency fallbacks.
actor FuelPriceService {

    static let shared = FuelPriceService()

    // ═══════════════════════════════════════════════════════════
    //  PRIMARY JSON APIs (no API key needed, free, reliable)
    // ═══════════════════════════════════════════════════════════
    private let oilPricesPkURL = "https://oilprices.pk/api/latest"
    private let trackMateURL = "https://fuel.trackmate.page/api/prices"

    // ═══════════════════════════════════════════════════════════
    //  FALLBACK HTML SCRAPERS
    // ═══════════════════════════════════════════════════════════
    private let hamariWebURL = "https://hamariweb.com/finance/petrol-prices.aspx"
    private let globalPetrolPricesURL = "https://www.globalpetrolprices.com/Pakistan/gasoline_prices/"
    private let globalPetrolDieselURL = "https://www.globalpetrolprices.com/Pakistan/diesel_prices/"
    private let psoURL = "https://psopk.com/en/products-and-services/product-prices"
    private let shellURL = "https://www.shell.com.pk/motorists/shell-fuels/fuel-pricing.html"

    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        config.timeoutIntervalForResource = 25
        return URLSession(configuration: config)
    }()

    // MARK: - Public API

    /// Fetches latest Pakistan fuel prices using multi-source fallback chain.
    func fetchLatestPrices() async throws -> FuelPriceFetchResult {
        // Source 1: OilPrices.pk API (JSON, OGRA official, most reliable)
        if let result = try? await fetchFromOilPricesPk(), result.isValid {
            return result
        }

        // Source 2: TrackMate Fuel API (JSON, aggregated PSO/Shell data)
        if let result = try? await fetchFromTrackMateApi(), result.isValid {
            return result
        }

        // Source 3: HamariWeb (HTML scraping fallback)
        if let result = try? await fetchFromHamariWeb(), result.isValid {
            return result
        }

        // Source 4: GlobalPetrolPrices.com (HTML scraping fallback)
        if let result = try? await fetchFromGlobalPetrolPrices(), result.isValid {
            return result
        }

        // Source 5: PSO website (JSON-in-script + HTML scraping)
        if let result = try? await fetchFromWebsite(
            urlString: psoURL,
            source: "PSO Pakistan Official"
        ), result.isValid {
            return result
        }

        // Source 6: Shell website (JSON-in-script + HTML scraping)
        if let result = try? await fetchFromWebsite(
            urlString: shellURL,
            source: "Shell Pakistan Official"
        ), result.isValid {
            return result
        }

        throw FuelPriceError.allSourcesFailed
    }

    // MARK: - Source 1: OilPrices.pk API (JSON — PRIMARY)

    private func fetchFromOilPricesPk() async throws -> FuelPriceFetchResult {
        let json = try await fetchJSON(from: oilPricesPkURL)

        var petrol = 0.0, diesel = 0.0, octane = 0.0
        var effectiveDate: String?

        // Try direct top-level keys
        petrol = extractPrice(from: json, keys: ["petrol", "petrol_price", "ms_petrol", "motor_spirit", "gasoline"])
        diesel = extractPrice(from: json, keys: ["diesel", "diesel_price", "hsd", "high_speed_diesel"])
        octane = extractPrice(from: json, keys: ["high_octane", "hi_octane", "hobc", "octane", "high_octane_price"])

        // Try "data" wrapper object
        if petrol == 0 || diesel == 0, let dataDict = json["data"] as? [String: Any] {
            if petrol == 0 { petrol = extractPrice(from: dataDict, keys: ["petrol", "petrol_price", "ms_petrol", "motor_spirit", "gasoline"]) }
            if diesel == 0 { diesel = extractPrice(from: dataDict, keys: ["diesel", "diesel_price", "hsd", "high_speed_diesel"]) }
            if octane == 0 { octane = extractPrice(from: dataDict, keys: ["high_octane", "hi_octane", "hobc", "octane", "high_octane_price"]) }

            // Nested objects: { "data": { "petrol": { "price": 252 } } }
            if petrol == 0 { petrol = extractNestedPrice(from: dataDict, keys: ["petrol", "motor_spirit", "ms_petrol", "gasoline"]) }
            if diesel == 0 { diesel = extractNestedPrice(from: dataDict, keys: ["diesel", "hsd", "high_speed_diesel"]) }
            if octane == 0 { octane = extractNestedPrice(from: dataDict, keys: ["high_octane", "hi_octane", "hobc", "octane"]) }
        }

        // Try arrays
        if petrol == 0 || diesel == 0 {
            let arr = (json["prices"] as? [[String: Any]])
                ?? (json["data"] as? [[String: Any]])
                ?? (json["products"] as? [[String: Any]])
            if let arr = arr {
                let (p, d, o) = parsePricesFromArray(arr)
                if petrol == 0 { petrol = p }
                if diesel == 0 { diesel = d }
                if octane == 0 { octane = o }
            }
        }

        // Deep key scan
        if petrol == 0 || diesel == 0 {
            for (key, value) in json {
                let lower = key.lowercased()
                var price = 0.0
                if let num = value as? Double { price = num }
                else if let num = value as? Int { price = Double(num) }
                else if let nested = value as? [String: Any] {
                    price = (nested["price"] as? Double) ?? (nested["retail_price"] as? Double) ?? (nested["rate"] as? Double) ?? 0
                }
                guard price >= 100, price <= 600 else { continue }
                if petrol == 0, isPetrolKeyword(lower), !isOctaneKeyword(lower) { petrol = price }
                else if diesel == 0, isDieselKeyword(lower) { diesel = price }
                else if octane == 0, isOctaneKeyword(lower) { octane = price }
            }
        }

        effectiveDate = (json["effective_date"] as? String)
            ?? (json["date"] as? String)
            ?? (json["updated_at"] as? String)
            ?? (json["data"] as? [String: Any])?["effective_date"] as? String

        return FuelPriceFetchResult(
            petrol: petrol, diesel: diesel, highOctane: octane,
            source: "OilPrices.pk API (OGRA Official)",
            effectiveDate: effectiveDate
        )
    }

    // MARK: - Source 2: TrackMate Fuel API (JSON — SECONDARY)

    private func fetchFromTrackMateApi() async throws -> FuelPriceFetchResult {
        let data = try await fetchData(from: trackMateURL)
        guard let rawString = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) else {
            throw FuelPriceError.parseError
        }

        var petrol = 0.0, diesel = 0.0, octane = 0.0
        var effectiveDate: String?

        if rawString.hasPrefix("[") {
            // Array of fuel products
            guard let arr = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
                throw FuelPriceError.parseError
            }
            let (p, d, o) = parsePricesFromArray(arr)
            petrol = p; diesel = d; octane = o
        } else if rawString.hasPrefix("{") {
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                throw FuelPriceError.parseError
            }

            // Direct keys
            petrol = extractPrice(from: json, keys: ["petrol", "petrol_price", "ms_petrol", "motor_spirit", "gasoline"])
            diesel = extractPrice(from: json, keys: ["diesel", "diesel_price", "hsd", "high_speed_diesel"])
            octane = extractPrice(from: json, keys: ["high_octane", "hi_octane", "hobc", "octane", "high_octane_price"])

            // "data" wrapper
            if petrol == 0 || diesel == 0, let dataDict = json["data"] as? [String: Any] {
                if petrol == 0 { petrol = extractPrice(from: dataDict, keys: ["petrol", "petrol_price", "ms_petrol", "motor_spirit", "gasoline"]) }
                if diesel == 0 { diesel = extractPrice(from: dataDict, keys: ["diesel", "diesel_price", "hsd", "high_speed_diesel"]) }
                if octane == 0 { octane = extractPrice(from: dataDict, keys: ["high_octane", "hi_octane", "hobc", "octane", "high_octane_price"]) }
                if petrol == 0 { petrol = extractNestedPrice(from: dataDict, keys: ["petrol", "motor_spirit", "gasoline"]) }
                if diesel == 0 { diesel = extractNestedPrice(from: dataDict, keys: ["diesel", "hsd", "high_speed_diesel"]) }
                if octane == 0 { octane = extractNestedPrice(from: dataDict, keys: ["high_octane", "hobc", "octane"]) }
            }

            // Arrays inside object
            if petrol == 0 || diesel == 0 {
                let arr = (json["prices"] as? [[String: Any]])
                    ?? (json["data"] as? [[String: Any]])
                    ?? (json["products"] as? [[String: Any]])
                    ?? (json["fuels"] as? [[String: Any]])
                if let arr = arr {
                    let (p, d, o) = parsePricesFromArray(arr)
                    if petrol == 0 { petrol = p }
                    if diesel == 0 { diesel = d }
                    if octane == 0 { octane = o }
                }
            }

            // Deep key scan
            if petrol == 0 || diesel == 0 {
                for (key, value) in json {
                    let lower = key.lowercased()
                    var price = 0.0
                    if let num = value as? Double { price = num }
                    else if let num = value as? Int { price = Double(num) }
                    else if let nested = value as? [String: Any] {
                        price = (nested["price"] as? Double) ?? (nested["retail_price"] as? Double) ?? (nested["rate"] as? Double) ?? 0
                    }
                    guard price >= 100, price <= 600 else { continue }
                    if petrol == 0, isPetrolKeyword(lower), !isOctaneKeyword(lower) { petrol = price }
                    else if diesel == 0, isDieselKeyword(lower) { diesel = price }
                    else if octane == 0, isOctaneKeyword(lower) { octane = price }
                }
            }

            effectiveDate = (json["effective_date"] as? String)
                ?? (json["date"] as? String)
                ?? (json["updated_at"] as? String)
        }

        return FuelPriceFetchResult(
            petrol: petrol, diesel: diesel, highOctane: octane,
            source: "TrackMate Fuel API",
            effectiveDate: effectiveDate
        )
    }

    // MARK: - JSON Helpers

    private func extractPrice(from dict: [String: Any], keys: [String]) -> Double {
        for key in keys {
            if let num = dict[key] as? Double, num >= 100, num <= 600 { return num }
            if let num = dict[key] as? Int, Double(num) >= 100, Double(num) <= 600 { return Double(num) }
            if let str = dict[key] as? String, let num = Double(str), num >= 100, num <= 600 { return num }
        }
        return 0
    }

    private func extractNestedPrice(from dict: [String: Any], keys: [String]) -> Double {
        for key in keys {
            guard let nested = dict[key] as? [String: Any] else { continue }
            for priceKey in ["price", "retail_price", "rate", "value", "current_price"] {
                if let num = nested[priceKey] as? Double, num >= 100, num <= 600 { return num }
                if let num = nested[priceKey] as? Int, Double(num) >= 100, Double(num) <= 600 { return Double(num) }
            }
        }
        return 0
    }

    private func parsePricesFromArray(_ arr: [[String: Any]]) -> (Double, Double, Double) {
        var petrol = 0.0, diesel = 0.0, octane = 0.0

        for item in arr {
            let name = ((item["name"] as? String)
                ?? (item["product"] as? String)
                ?? (item["fuel"] as? String)
                ?? (item["fuel_type"] as? String)
                ?? (item["type"] as? String)
                ?? (item["title"] as? String)
                ?? (item["label"] as? String)
                ?? "").lowercased()

            var price = 0.0
            for priceKey in ["price", "retail_price", "rate", "value", "current_price"] {
                if let num = item[priceKey] as? Double, num >= 100, num <= 600 { price = num; break }
                if let num = item[priceKey] as? Int, Double(num) >= 100, Double(num) <= 600 { price = Double(num); break }
                if let str = item[priceKey] as? String, let num = Double(str), num >= 100, num <= 600 { price = num; break }
            }
            guard price >= 100, price <= 600 else { continue }

            if petrol == 0, isPetrolKeyword(name), !isOctaneKeyword(name) { petrol = price }
            else if diesel == 0, isDieselKeyword(name) { diesel = price }
            else if octane == 0, isOctaneKeyword(name) { octane = price }
        }

        return (petrol, diesel, octane)
    }

    // MARK: - Source 3: HamariWeb Pakistan (HTML fallback)

    private func fetchFromHamariWeb() async throws -> FuelPriceFetchResult {
        let html = try await fetchHTML(from: hamariWebURL)
        return parseFuelPricesFromHtml(html, source: "HamariWeb Pakistan")
    }

    // MARK: - Source 4: GlobalPetrolPrices.com (HTML fallback)

    private func fetchFromGlobalPetrolPrices() async throws -> FuelPriceFetchResult {
        let petrolHtml = try await fetchHTML(from: globalPetrolPricesURL)
        let dieselHtml = try await fetchHTML(from: globalPetrolDieselURL)

        let petrol = extractGlobalPetrolPrice(from: petrolHtml)
        let diesel = extractGlobalPetrolPrice(from: dieselHtml)
        let octane = petrol > 0 ? petrol + 40 : 0

        return FuelPriceFetchResult(
            petrol: petrol, diesel: diesel, highOctane: octane,
            source: "GlobalPetrolPrices.com",
            effectiveDate: nil
        )
    }

    private func extractGlobalPetrolPrice(from html: String) -> Double {
        let patterns = [
            #"(\d{2,3}\.\d{1,3})\s*PKR"#,
            #"PKR\s*(\d{2,3}\.\d{1,3})"#,
            #"(?:Rs\.?|₨)\s*(\d{2,3}\.\d{1,2})"#,
            #">\s*(\d{2,3}\.\d{2,3})\s*<"#
        ]

        for pattern in patterns {
            guard let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive) else { continue }
            let nsRange = NSRange(html.startIndex..., in: html)
            let results = regex.matches(in: html, range: nsRange)
            for match in results {
                guard let range = Range(match.range(at: 1), in: html) else { continue }
                if let price = Double(html[range]), price >= 100, price <= 600 {
                    return price
                }
            }
        }
        return 0
    }

    // MARK: - Source 5 & 6: Generic website (PSO / Shell)

    private func fetchFromWebsite(urlString: String, source: String) async throws -> FuelPriceFetchResult {
        let html = try await fetchHTML(from: urlString)

        let jsonResult = extractPricesFromEmbeddedJson(html, source: source)
        if jsonResult.isValid { return jsonResult }

        return parseFuelPricesFromHtml(html, source: source)
    }

    // MARK: - Network Helpers

    private func fetchJSON(from urlString: String) async throws -> [String: Any] {
        guard let url = URL(string: urlString) else { throw FuelPriceError.httpError }
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Zyphuel-App/1.0 (iOS)", forHTTPHeaderField: "User-Agent")

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw FuelPriceError.httpError
        }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw FuelPriceError.parseError
        }
        return json
    }

    private func fetchData(from urlString: String) async throws -> Data {
        guard let url = URL(string: urlString) else { throw FuelPriceError.httpError }
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Zyphuel-App/1.0 (iOS)", forHTTPHeaderField: "User-Agent")

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw FuelPriceError.httpError
        }
        return data
    }

    private func fetchHTML(from urlString: String) async throws -> String {
        guard let url = URL(string: urlString) else { throw FuelPriceError.httpError }
        var request = URLRequest(url: url)
        request.setValue(
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
            forHTTPHeaderField: "User-Agent"
        )
        request.setValue("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", forHTTPHeaderField: "Accept")
        request.setValue("en-US,en;q=0.9,ur;q=0.8", forHTTPHeaderField: "Accept-Language")

        let (data, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw FuelPriceError.httpError
        }
        return String(data: data, encoding: .utf8) ?? ""
    }

    // MARK: - JSON-in-Script Extraction

    private func extractPricesFromEmbeddedJson(_ html: String, source: String) -> FuelPriceFetchResult {
        var petrol = 0.0, diesel = 0.0, octane = 0.0

        let scriptPattern = try? NSRegularExpression(pattern: #"<script[^>]*>([\s\S]*?)</script>"#, options: .caseInsensitive)
        let nsRange = NSRange(html.startIndex..., in: html)
        let scriptMatches = scriptPattern?.matches(in: html, range: nsRange) ?? []

        for match in scriptMatches {
            guard let range = Range(match.range(at: 1), in: html) else { continue }
            let script = String(html[range])

            let productPricePattern = try? NSRegularExpression(
                pattern: #"(?:petrol|diesel|hsd|octane|hobc|motor.?spirit|super)[^}]*?(\d{2,3}\.\d{1,2})"#,
                options: .caseInsensitive
            )
            let scriptNsRange = NSRange(script.startIndex..., in: script)
            let priceMatches = productPricePattern?.matches(in: script, range: scriptNsRange) ?? []

            for pm in priceMatches {
                guard let fullRange = Range(pm.range, in: script),
                      let priceRange = Range(pm.range(at: 1), in: script) else { continue }
                let fullMatch = String(script[fullRange]).lowercased()
                guard let price = Double(script[priceRange]), price >= 100, price <= 600 else { continue }

                if petrol == 0 && (fullMatch.contains("petrol") || fullMatch.contains("super") || fullMatch.contains("motor")) && !fullMatch.contains("octane") {
                    petrol = price
                } else if diesel == 0 && (fullMatch.contains("diesel") || fullMatch.contains("hsd")) {
                    diesel = price
                } else if octane == 0 && (fullMatch.contains("octane") || fullMatch.contains("hobc")) {
                    octane = price
                }
            }
            if petrol > 0 && diesel > 0 { break }
        }

        return FuelPriceFetchResult(petrol: petrol, diesel: diesel, highOctane: octane, source: source, effectiveDate: nil)
    }

    // MARK: - Enhanced HTML Parser

    private func parseFuelPricesFromHtml(_ html: String, source: String) -> FuelPriceFetchResult {
        let pricePattern = try! NSRegularExpression(
            pattern: #"(?:Rs\.?|PKR|₨)\s*(\d{2,3}(?:\.\d{1,2})?)"#,
            options: .caseInsensitive
        )
        let standalonePattern = try! NSRegularExpression(
            pattern: #">\s*(\d{2,3}\.\d{2})\s*<"#,
            options: []
        )

        var petrol = 0.0, diesel = 0.0, octane = 0.0

        let cleanHtml = html
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "&#160;", with: " ")
            .replacingOccurrences(of: "&amp;", with: "&")

        let lines = cleanHtml.components(separatedBy: CharacterSet.newlines)
            .flatMap { $0.components(separatedBy: "<tr>") }
            .flatMap { $0.components(separatedBy: "<td>") }
            .flatMap { $0.components(separatedBy: "<div>") }
            .flatMap { $0.components(separatedBy: "<span>") }
            .flatMap { $0.components(separatedBy: "<li>") }

        for (i, line) in lines.enumerated() {
            let lower = line.lowercased()
            guard lower.count >= 3 else { continue }

            let searchRange = i...min(i + 4, lines.count - 1)
            var foundPrice = 0.0

            for j in searchRange {
                let candidate = lines[j].replacingOccurrences(of: ",", with: "")
                let nsRange = NSRange(candidate.startIndex..., in: candidate)

                if let match = pricePattern.firstMatch(in: candidate, range: nsRange),
                   let range = Range(match.range(at: 1), in: candidate),
                   let p = Double(candidate[range]), p >= 100, p <= 600 {
                    foundPrice = p; break
                }
                if let match = standalonePattern.firstMatch(in: candidate, range: nsRange),
                   let range = Range(match.range(at: 1), in: candidate),
                   let p = Double(candidate[range]), p >= 100, p <= 600 {
                    foundPrice = p; break
                }
            }

            guard foundPrice > 0 else { continue }

            let isPetrol = isPetrolKeyword(lower) && !isOctaneKeyword(lower)
            let isDiesel = isDieselKeyword(lower)
            let isOctane = isOctaneKeyword(lower)

            if petrol == 0 && isPetrol { petrol = foundPrice }
            else if diesel == 0 && isDiesel { diesel = foundPrice }
            else if octane == 0 && isOctane { octane = foundPrice }
        }

        return FuelPriceFetchResult(petrol: petrol, diesel: diesel, highOctane: octane, source: source, effectiveDate: nil)
    }

    // MARK: - Keyword Matchers

    private func isPetrolKeyword(_ text: String) -> Bool {
        text.contains("petrol") || text.contains("motor spirit") || text.contains("mogas") ||
        text.contains("super") || text.contains("gasoline") || text.contains("unleaded")
    }

    private func isDieselKeyword(_ text: String) -> Bool {
        text.contains("diesel") || text.contains("hsd") || text.contains("high speed") || text.contains("high-speed")
    }

    private func isOctaneKeyword(_ text: String) -> Bool {
        text.contains("octane") || text.contains("hobc") || text.contains("hi-octane") ||
        text.contains("ron 97") || text.contains("high octane") || text.contains("premium")
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

    @Published private(set) var lastPetrol: Double = 252.10
    @Published private(set) var lastDiesel: Double = 258.34
    @Published private(set) var lastHighOctane: Double = 289.62
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
        let cachedPetrol = defaults.double(forKey: "zyphuel_price_petrol")
        let cachedDiesel = defaults.double(forKey: "zyphuel_price_diesel")
        let cachedOctane = defaults.double(forKey: "zyphuel_price_high_octane")
        if cachedPetrol > 0 { lastPetrol = cachedPetrol }
        if cachedDiesel > 0 { lastDiesel = cachedDiesel }
        if cachedOctane > 0 { lastHighOctane = cachedOctane }
        lastSource = defaults.string(forKey: "zyphuel_price_source") ?? "OGRA Default Rates"
        let ts = defaults.double(forKey: "zyphuel_price_last_updated")
        if ts > 0 { lastUpdated = Date(timeIntervalSince1970: ts) }

        ServiceCatalog.updateFuelPrices(petrol: lastPetrol, diesel: lastDiesel, highOctane: lastHighOctane)
    }
}
