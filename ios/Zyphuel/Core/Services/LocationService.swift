import CoreLocation
import Foundation

/// Location for the delivery address and coverage check.
///
/// Parity note: Android permanently disabled server-side live GPS tracking
/// (`publishLocation` returns false, `observeLocation` emits null). The iOS port keeps
/// that guarantee — it never uploads coordinates to `live_tracking`; the coordinate is
/// used locally to prefill the delivery address and to build a share link.
@MainActor
final class LocationService: NSObject, ObservableObject {
    @Published private(set) var authorization: CLAuthorizationStatus = .notDetermined
    @Published private(set) var coordinate: CLLocationCoordinate2D?
    @Published private(set) var address: String = ""
    @Published private(set) var isResolving = false
    @Published var errorMessage: String?

    private let manager = CLLocationManager()
    private let geocoder = CLGeocoder()

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        authorization = manager.authorizationStatus
    }

    var isAuthorized: Bool {
        authorization == .authorizedWhenInUse || authorization == .authorizedAlways
    }

    func requestPermission() {
        manager.requestWhenInUseAuthorization()
    }

    /// Stores a manually typed Lahore address (the pin/address editor flow).
    func updateAddress(_ value: String) {
        address = SecurityValidator.sanitised(value, maxLength: 240)
    }

    /// Alias used by the address editor for readability.
    func resolve(address: String) {
        updateAddress(address)
    }

    func refresh() {
        guard isAuthorized else {
            requestPermission()
            return
        }
        manager.requestLocation()
    }

    /// Resolves a coordinate the user tapped into a readable Lahore address.
    func resolve(coordinate: CLLocationCoordinate2D) {
        self.coordinate = coordinate
        isResolving = true
        geocoder.reverseGeocodeLocation(CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)) { [weak self] placemarks, error in
            Task { @MainActor in
                guard let self else { return }
                self.isResolving = false
                if let error {
                    self.errorMessage = error.localizedDescription
                    return
                }
                if let placemark = placemarks?.first {
                    let parts = [
                        placemark.name,
                        placemark.thoroughfare,
                        placemark.subLocality,
                        placemark.locality
                    ].compactMap { $0 }
                    self.address = parts.joined(separator: ", ")
                }
            }
        }
    }

    /// `https://maps.google.com/?q=lat,lng`, exactly the link Android shares.
    var shareLink: String? {
        guard let coordinate else { return nil }
        return String(format: "https://maps.google.com/?q=%.6f,%.6f", coordinate.latitude, coordinate.longitude)
    }

    var shareMessage: String {
        let destination = address.isEmpty ? ZyphuelConfig.defaultDeliveryAddress : address
        if let link = shareLink {
            return "Zyphuel delivery location: \(destination) — \(link)"
        }
        return "Zyphuel delivery location: \(destination)"
    }
}

extension LocationService: CLLocationManagerDelegate {
    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in
            self.authorization = status
            if status == .authorizedWhenInUse || status == .authorizedAlways {
                manager.requestLocation()
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        Task { @MainActor in
            self.resolve(coordinate: location.coordinate)
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in
            self.errorMessage = error.localizedDescription
        }
    }
}
