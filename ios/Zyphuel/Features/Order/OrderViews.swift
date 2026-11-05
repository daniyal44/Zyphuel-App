import SwiftUI

// MARK: - Order sheet (COD checkout)

struct OrderSheetView: View {
    let item: ServiceItem

    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var config: SystemConfigService
    @EnvironmentObject private var location: LocationService
    @EnvironmentObject private var language: LanguageManager
    @Environment(\.dismiss) private var dismiss

    @State private var liters = 5
    @State private var address = ""
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @State private var placedOrder: FuelOrder?

    private let presetVolumes = [5, 7, 10, 11, 12, 13, 14, 15]

    private var isUnavailable: Bool {
        !item.isActive || PricingEngine.isServiceUnavailable(item.name)
    }

    private var isOpenNow: Bool { OperatingHours.isDeliveryOpen() }

    private var baseRate: Double { config.baseRate(for: item.name) }

    private var breakdown: PricingEngine.Breakdown? {
        try? PricingEngine.price(baseRate: baseRate, liters: liters, serviceType: item.name)
    }

    var body: some View {
        NavigationStack {
            Group {
                if let placedOrder {
                    TrackerView(order: placedOrder)
                        .environmentObject(appState)
                        .environmentObject(language)
                } else {
                    form
                }
            }
            .navigationTitle(placedOrder == nil ? item.name : "Your Order")
            .navigationBarTitleDisplayMode(.inline)
            .zyphuelNavBar()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(placedOrder == nil ? "Close" : "Done") { dismiss() }
                }
            }
            .alert(
                "Order not placed",
                isPresented: Binding(
                    get: { errorMessage != nil },
                    set: { if !$0 { errorMessage = nil } }
                )
            ) {
                Button("OK", role: .cancel) { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "")
            }
        }
    }

    private var form: some View {
        ScrollView {
            VStack(spacing: 16) {
                itemHeader

                if isUnavailable {
                    unavailableNotice
                } else {
                    volumeSection
                    addressSection
                    priceSection
                    paymentSection

                    if !isOpenNow {
                        closedNotice
                    }

                    Button {
                        Task { await confirm() }
                    } label: {
                        if isSubmitting {
                            ProgressView().tint(.white)
                        } else {
                            Text(language.tr("order.confirm", "Confirm Order (COD) 💵"))
                        }
                    }
                    .buttonStyle(PrimaryButtonStyle(enabled: isOpenNow && !isSubmitting))
                    .disabled(!isOpenNow || isSubmitting)

                    Text("Cash on Delivery only. No online payment is collected in the app.")
                        .font(.system(size: 10))
                        .foregroundStyle(ZyphuelColor.textSecondary)
                }
            }
            .padding(16)
            .padding(.bottom, 20)
        }
        .background(ZyphuelColor.background.ignoresSafeArea())
        .scrollDismissesKeyboard(.interactively)
        .onAppear {
            address = location.address.isEmpty ? ZyphuelConfig.defaultDeliveryAddress : location.address
        }
    }

    private var itemHeader: some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack {
                Text(item.name)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(ZyphuelColor.textPrimary)
                Spacer()
                BadgePill(text: item.badge ?? "LIVE", color: ZyphuelColor.success)
            }
            Text(item.detail)
                .font(.system(size: 12))
                .foregroundStyle(ZyphuelColor.textSecondary)
            HStack(spacing: 12) {
                Label("\(MoneyFormatter.pkr(PricingEngine.pumpRate(base: baseRate, serviceType: item.name)))/L", systemImage: "tag.fill")
                Label(item.eta, systemImage: "clock")
            }
            .font(.system(size: 11, weight: .medium))
            .foregroundStyle(ZyphuelColor.brand)
        }
        .zyphuelCard()
    }

    private var unavailableNotice: some View {
        VStack(spacing: 8) {
            Image(systemName: "nosign")
                .font(.system(size: 28))
                .foregroundStyle(ZyphuelColor.danger)
            Text("Currently unavailable")
                .font(.system(size: 15, weight: .semibold))
            Text("Pure water delivery and LPG gas cylinders are paused across all Zyphuel order flows.")
                .font(.system(size: 12))
                .foregroundStyle(ZyphuelColor.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(RoundedRectangle(cornerRadius: 16).fill(ZyphuelColor.danger.opacity(0.06)))
    }

    private var volumeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: language.tr("order.volume", "Volume (Litres)"),
                          subtitle: "Maximum \(PricingEngine.maxLiters)L per doorstep order")
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                ForEach(presetVolumes, id: \.self) { value in
                    Button {
                        liters = value
                    } label: {
                        Text("\(value)L")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(liters == value ? .white : ZyphuelColor.textPrimary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(
                                RoundedRectangle(cornerRadius: 11, style: .continuous)
                                    .fill(liters == value ? ZyphuelColor.brand : Color.white)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 11, style: .continuous)
                                    .stroke(ZyphuelColor.border, lineWidth: liters == value ? 0 : 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .zyphuelCard()
    }

    private var addressSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: language.tr("order.address", "Delivery Address"),
                          subtitle: "Rider delivers to this Lahore address")
            LabeledField(label: "Address", placeholder: "House, block, area", text: $address)
            Button {
                location.refresh()
            } label: {
                Label("Use my current location", systemImage: "location.fill")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.brand)
            }
            .disabled(location.isResolving)
        }
        .zyphuelCard()
    }

    private var priceSection: some View {
        VStack(spacing: 9) {
            SectionHeader(title: "Payment Summary", subtitle: "Cash on Delivery")
            if let breakdown {
                receiptRow("Fuel (\(liters)L × \(MoneyFormatter.pkr(breakdown.unitPrice)))", breakdown.subtotalLabel)
                receiptRow(language.tr("order.deliveryFee", "Delivery Charges"), breakdown.deliveryFeeLabel)
                Divider()
                HStack {
                    Text(language.tr("order.total", "COD Total"))
                        .font(.system(size: 15, weight: .bold))
                    Spacer()
                    Text(breakdown.totalLabel)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(ZyphuelColor.brand)
                }
            }
        }
        .zyphuelCard()
    }

    private var paymentSection: some View {
        HStack(spacing: 10) {
            Image(systemName: "banknote.fill")
                .foregroundStyle(ZyphuelColor.success)
            VStack(alignment: .leading, spacing: 2) {
                Text("Cash on Delivery")
                    .font(.system(size: 13, weight: .semibold))
                Text("Pay the rider when your fuel arrives.")
                    .font(.system(size: 10))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(ZyphuelColor.success)
        }
        .padding(13)
        .background(RoundedRectangle(cornerRadius: 14).fill(ZyphuelColor.success.opacity(0.07)))
    }

    private var closedNotice: some View {
        VStack(alignment: .leading, spacing: 5) {
            Label("Deliveries closed", systemImage: "clock.badge.exclamationmark")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(ZyphuelColor.danger)
            Text("Ordering resumes at the next window. \(OperatingHours.fullScheduleSummary).")
                .font(.system(size: 11))
                .foregroundStyle(ZyphuelColor.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(13)
        .background(RoundedRectangle(cornerRadius: 14).fill(ZyphuelColor.danger.opacity(0.06)))
    }

    private func receiptRow(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title)
                .font(.system(size: 12))
                .foregroundStyle(ZyphuelColor.textSecondary)
            Spacer()
            Text(value)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(ZyphuelColor.textPrimary)
        }
    }

    private func confirm() async {
        errorMessage = nil
        isSubmitting = true
        defer { isSubmitting = false }

        do {
            let order = try await appState.placeOrder(
                serviceType: item.name,
                baseRate: baseRate,
                liters: liters,
                address: address,
                coordinate: location.coordinate.map { (lat: $0.latitude, lng: $0.longitude) }
            )
            placedOrder = order
            await appState.notifications.notify(
                title: "Order placed",
                body: "\(order.serviceType) • \(order.quantityLiters)L • \(order.totalPriceLabel) COD"
            )
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Tracker

struct TrackerView: View {
    let order: FuelOrder

    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var language: LanguageManager

    @State private var stars = 0
    @State private var comment = ""
    @State private var isSubmittingRating = false
    @State private var showRatingThanks = false

    private var liveOrder: FuelOrder {
        appState.orders.orders.first(where: { $0.id == order.id }) ?? order
    }

    private var riderPhone: String? {
        guard let email = liveOrder.riderEmail else { return nil }
        return appState.users.riders.first { $0.email.caseInsensitiveCompare(email) == .orderedSame }?.phoneNumber
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                statusCard
                detailsCard
                riderCard
                if liveOrder.status == .completed {
                    ratingCard
                }
                supportCard
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(ZyphuelColor.background.ignoresSafeArea())
        .navigationTitle("Order Details")
        .navigationBarTitleDisplayMode(.inline)
        .zyphuelNavBar()
    }

    private var statusCard: some View {
        VStack(spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text(liveOrder.status.rawValue)
                        .font(.system(size: 19, weight: .bold))
                        .foregroundStyle(ZyphuelColor.textPrimary)
                    Text("Order \(liveOrder.id.prefix(8).uppercased())")
                        .font(.system(size: 11))
                        .foregroundStyle(ZyphuelColor.textSecondary)
                }
                Spacer()
                StatusBadge(status: liveOrder.status)
            }

            DeliveryStepper(status: liveOrder.status)

            if liveOrder.status == .completed {
                Label("Delivered • please rate your rider below", systemImage: "checkmark.seal.fill")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(ZyphuelColor.success)
            } else if liveOrder.status == .cancelled {
                Label("This order was cancelled. Contact support for a re-delivery.", systemImage: "xmark.octagon.fill")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(ZyphuelColor.danger)
            }
        }
        .zyphuelCard()
    }

    private var detailsCard: some View {
        VStack(spacing: 10) {
            SectionHeader(title: "Order Summary", subtitle: liveOrder.serviceType)
            detailRow("Volume", "\(liveOrder.quantityLiters)L")
            detailRow("Unit price", MoneyFormatter.pkr(liveOrder.unitPrice))
            detailRow(language.tr("order.deliveryFee", "Delivery Charges"), MoneyFormatter.pkr(liveOrder.deliveryFee))
            detailRow("Payment", liveOrder.paymentMethod)
            detailRow("Address", liveOrder.deliveryAddress)
            Divider()
            HStack {
                Text(language.tr("order.total", "COD Total"))
                    .font(.system(size: 14, weight: .bold))
                Spacer()
                Text(liveOrder.totalPriceLabel)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(ZyphuelColor.brand)
            }
        }
        .zyphuelCard()
    }

    private var riderCard: some View {
        Group {
            if let name = liveOrder.riderName, !name.isEmpty {
                VStack(spacing: 12) {
                    HStack(spacing: 11) {
                        ZStack {
                            Circle().fill(ZyphuelColor.brand.opacity(0.12))
                            Image(systemName: "scooter")
                                .foregroundStyle(ZyphuelColor.brand)
                        }
                        .frame(width: 40, height: 40)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(name)
                                .font(.system(size: 14, weight: .semibold))
                            Text(liveOrder.riderEmail ?? "Zyphuel rider")
                                .font(.system(size: 11))
                                .foregroundStyle(ZyphuelColor.textSecondary)
                        }
                        Spacer()
                    }

                    HStack(spacing: 10) {
                        if let phone = riderPhone, !phone.isEmpty {
                            Link(destination: URL(string: "tel://\(phone.filter { $0.isNumber || $0 == "+" })")!) {
                                Label("Call rider", systemImage: "phone.fill")
                                    .font(.system(size: 12, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 9)
                                    .background(Capsule().fill(ZyphuelColor.brand.opacity(0.1)))
                                    .foregroundStyle(ZyphuelColor.brand)
                            }
                        }
                        Button {
                            appState.openWhatsApp()
                        } label: {
                            Label("Support chat", systemImage: "message.fill")
                                .font(.system(size: 12, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 9)
                                .background(Capsule().fill(ZyphuelColor.success.opacity(0.12)))
                                .foregroundStyle(ZyphuelColor.success)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .zyphuelCard()
            }
        }
    }

    private var ratingCard: some View {
        VStack(spacing: 12) {
            if liveOrder.isRated || showRatingThanks {
                Label("Thanks for rating this delivery.", systemImage: "star.fill")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(ZyphuelColor.warning)
            } else {
                SectionHeader(title: "Rate your delivery", subtitle: "Your feedback trains our dispatch quality")
                StarRatingView(rating: $stars)
                TextField("Optional note for the rider or dispatch team", text: $comment, axis: .vertical)
                    .font(.system(size: 13))
                    .lineLimit(2...4)
                    .padding(11)
                    .background(RoundedRectangle(cornerRadius: 11).fill(ZyphuelColor.background))
                Button {
                    Task { await submitRating() }
                } label: {
                    if isSubmittingRating {
                        ProgressView().tint(.white)
                    } else {
                        Text("Submit Feedback")
                    }
                }
                .buttonStyle(PrimaryButtonStyle(enabled: stars > 0))
                .disabled(stars == 0 || isSubmittingRating)
            }
        }
        .zyphuelCard()
    }

    private var supportCard: some View {
        VStack(spacing: 10) {
            SectionHeader(title: "Need help?", subtitle: "Zyphuel operations \(ZyphuelConfig.whatsappDisplay)")
            Button {
                appState.openWhatsApp()
            } label: {
                Label("WhatsApp support", systemImage: "message.fill")
            }
            .buttonStyle(SecondaryButtonStyle())
        }
        .zyphuelCard()
    }

    private func detailRow(_ title: String, _ value: String) -> some View {
        HStack(alignment: .top) {
            Text(title)
                .font(.system(size: 12))
                .foregroundStyle(ZyphuelColor.textSecondary)
            Spacer(minLength: 12)
            Text(value)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(ZyphuelColor.textPrimary)
                .multilineTextAlignment(.trailing)
        }
    }

    private func submitRating() async {
        isSubmittingRating = true
        defer { isSubmittingRating = false }
        await appState.orders.submitRating(order: liveOrder, stars: stars, comment: comment)
        showRatingThanks = true
    }
}

// MARK: - History

struct OrderHistoryView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var language: LanguageManager

    private enum Filter: String, CaseIterable, Identifiable {
        case all = "All"
        case delivered = "Delivered"
        case cancelled = "Cancelled"
        var id: String { rawValue }
    }

    @State private var search = ""
    @State private var filter: Filter = .all

    private var filteredRecent: [FuelOrder] {
        appState.orders.pastOrders.filter { order in
            let matchesFilter: Bool
            switch filter {
            case .all: matchesFilter = true
            case .delivered: matchesFilter = order.status == .completed
            case .cancelled: matchesFilter = order.status == .cancelled
            }
            let query = search.trimmingCharacters(in: .whitespaces)
            let matchesSearch = query.isEmpty
                || order.serviceType.localizedCaseInsensitiveContains(query)
                || order.id.localizedCaseInsensitiveContains(query)
            return matchesFilter && matchesSearch
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    if appState.orders.orders.isEmpty {
                        // Android keeps this screen blank when the customer has no orders.
                        Color.clear.frame(height: 1)
                    } else {
                        if !appState.orders.currentOrders.isEmpty {
                            SectionHeader(title: "Current Order",
                                          subtitle: "\(appState.orders.currentOrders.count) in progress")
                            ForEach(appState.orders.currentOrders) { order in
                                NavigationLink {
                                    TrackerView(order: order)
                                        .environmentObject(appState)
                                        .environmentObject(language)
                                } label: {
                                    OrderHistoryRow(order: order)
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        if !appState.orders.pastOrders.isEmpty {
                            searchField
                            filterChips
                            SectionHeader(title: "Recent Orders",
                                          subtitle: "\(filteredRecent.count) shown")
                            ForEach(filteredRecent) { order in
                                NavigationLink {
                                    TrackerView(order: order)
                                        .environmentObject(appState)
                                        .environmentObject(language)
                                } label: {
                                    OrderHistoryRow(order: order)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(ZyphuelColor.background.ignoresSafeArea())
            .navigationTitle("My Orders")
            .navigationBarTitleDisplayMode(.large)
        }
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(ZyphuelColor.textSecondary)
            TextField("Search by service or order id", text: $search)
                .font(.system(size: 13))
        }
        .padding(.horizontal, 13)
        .padding(.vertical, 10)
        .background(Capsule().fill(Color.white))
        .overlay(Capsule().stroke(ZyphuelColor.border, lineWidth: 1))
    }

    private var filterChips: some View {
        HStack(spacing: 8) {
            ForEach(Filter.allCases) { option in
                Button {
                    filter = option
                } label: {
                    Text(option.rawValue)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(filter == option ? .white : ZyphuelColor.textPrimary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(Capsule().fill(filter == option ? ZyphuelColor.brand : Color.white))
                        .overlay(Capsule().stroke(ZyphuelColor.border, lineWidth: filter == option ? 0 : 1))
                }
                .buttonStyle(.plain)
            }
            Spacer()
        }
    }
}

struct OrderHistoryRow: View {
    let order: FuelOrder

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(ZyphuelColor.brand.opacity(0.1))
                Image(systemName: order.status.iconName)
                    .font(.system(size: 15))
                    .foregroundStyle(ZyphuelColor.brand)
            }
            .frame(width: 40, height: 40)

            VStack(alignment: .leading, spacing: 3) {
                Text(order.serviceType)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.textPrimary)
                Text("\(order.quantityLiters)L • \(order.totalPriceLabel) • \(order.createdAt.formatted(date: .abbreviated, time: .shortened))")
                    .font(.system(size: 10))
                    .foregroundStyle(ZyphuelColor.textSecondary)
                    .lineLimit(1)
            }
            Spacer()
            StatusBadge(status: order.status)
        }
        .zyphuelCard(padding: 12, cornerRadius: 14)
    }
}
