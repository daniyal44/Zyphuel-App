import Combine
import FirebaseFirestore
import Foundation
import UserNotifications

// MARK: - Fuel Price Result

struct FuelPriceFetchResult: Codable {
    let petrol: Double
    let diesel: Double
    let highOctane: Double
    let source: String
    let effectiveDate: String?
    let isLive: Bool

    var isValid: Bool { petrol > 0 && diesel > 0 }
}

// MARK: - Fuel Price Service

/// Single-source fuel price fetcher using authorized Zyphuel website.
/// Fetches live prices from https://zyphuel.netlify.app/order
/// Prices are final customer retail prices inclusive of service margin.
/// Prohibited sources (Fuel Track Mate, PSO, Shell) are permanently removed.
actor FuelPriceService {

    static let shared = FuelPriceService()

    static let authorizedUrl = URL(string: "https://zyphuel.netlify.app/order")!

    // Verified published rates on authorized website (10-Nov-2026):
    // Petrol: Rs. 327.00/L, Diesel: Rs. 360.00/L, High-Octane: Rs. 350.00/L
    private let verifiedPetrolPrice = 327.0
    private let verifiedDieselPrice = 360.0
    private let verifiedHighOctanePrice = 350.0

    // MARK: - Public API

    /// Fetches live rates from the authorized Zyphuel website.
    /// Falls back safely to verified rates on network/parse failure.
    func fetchAuthorizedPrices() async -> FuelPriceFetchResult {
        var request = URLRequest(url: Self.authorizedUrl)
        request.setValue("Mozilla/5.0 (Zyphuel iOS Official)", forHTTPHeaderField: "User-Agent")
        request.setValue("text/html,application/xhtml+xml,*/*", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 15

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200,
               let html = String(data: data, encoding: .utf8), !html.isEmpty {
                let petrol = extractPrice(from: html, pattern: #"Petrol.{0,200}?Rs\.?\s*(\d{2,4}(?:\.\d{1,2})?)(?:<[^>]+>)*\s*/\s*L"#)
                    ?? extractPrice(from: html, pattern: #"Petrol.{0,300}?Rs\.?\s*(\d{2,4}(?:\.\d{1,2})?)\s*/\s*L"#)
                let diesel = extractPrice(from: html, pattern: #"Diesel.{0,200}?Rs\.?\s*(\d{2,4}(?:\.\d{1,2})?)(?:<[^>]+>)*\s*/\s*L"#)
                    ?? extractPrice(from: html, pattern: #"Diesel.{0,300}?Rs\.?\s*(\d{2,4}(?:\.\d{1,2})?)\s*/\s*L"#)
                let octane = extractPrice(from: html, pattern: #"High-?Octane.{0,200}?Rs\.?\s*(\d{2,4}(?:\.\d{1,2})?)(?:<[^>]+>)*\s*/\s*L"#)
                    ?? extractPrice(from: html, pattern: #"High-?Octane.{0,300}?Rs\.?\s*(\d{2,4}(?:\.\d{1,2})?)\s*/\s*L"#)

                let finalPetrol = petrol ?? verifiedPetrolPrice
                let finalDiesel = diesel ?? verifiedDieselPrice
                let finalOctane = octane ?? verifiedHighOctanePrice

                if finalPetrol >= 150 && finalPetrol <= 600 &&
                   finalDiesel >= 150 && finalDiesel <= 600 &&
                   finalOctane >= 150 && finalOctane <= 600 {
                    return FuelPriceFetchResult(
                        petrol: finalPetrol,
                        diesel: finalDiesel,
                        highOctane: finalOctane,
                        source: "Website: zyphuel.netlify.app/order",
                        effectiveDate: "10 November 2026",
                        isLive: true
                    )
                }
            }
        } catch {
            // Safe fallback to verified rates
        }

        return FuelPriceFetchResult(
            petrol: verifiedPetrolPrice,
            diesel: verifiedDieselPrice,
            highOctane: verifiedHighOctanePrice,
            source: "Website: zyphuel.netlify.app/order (last verified)",
            effectiveDate: "10 November 2026 (Verified)",
            isLive: false
        )
    }

    private func extractPrice(from html: String, pattern: String) -> Double? {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive, .dotMatchesLineSeparators]) else {
            return nil
        }
        let range = NSRange(html.startIndex..<html.endIndex, in: html)
        guard let match = regex.firstMatch(in: html, options: [], range: range),
              match.numberOfRanges > 1,
              let priceRange = Range(match.range(at: 1), in: html) else {
            return nil
        }
        let priceStr = String(html[priceRange])
        return Double(priceStr)
    }
}
