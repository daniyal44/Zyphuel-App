import SwiftUI

/// Customer shell: three tabs, matching the Android bottom navigation
/// (Home, Orders, Profile).
struct CustomerShellView: View {
    @EnvironmentObject private var language: LanguageManager
    @State private var selection = 0

    var body: some View {
        TabView(selection: $selection) {
            CustomerHomeView()
                .tabItem { Label(language.tr("tab.home", "Home"), systemImage: "house.fill") }
                .tag(0)

            OrderHistoryView()
                .tabItem { Label(language.tr("tab.orders", "Orders"), systemImage: "list.bullet.rectangle") }
                .tag(1)

            ProfileView()
                .tabItem { Label(language.tr("tab.profile", "Profile"), systemImage: "person.crop.circle") }
                .tag(2)
        }
        .tint(ZyphuelColor.brand)
    }
}

/// Customer dashboard. Layout follows the current Android home screen: location card,
/// operating-hours state, live OGRA rates, service grid, active order and COD ordering.
struct CustomerHomeView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var config: SystemConfigService
    @EnvironmentObject private var location: LocationService
    @EnvironmentObject private var language: LanguageManager

    @State private var searchQuery = ""
    @State private var showAllCategories = false
    @State private var selectedItem: ServiceItem?
    @State private var showAddressEditor = false
    @State private var draftAddress = ""
    @State private var showUpdates = false
    @State private var showProfileMenu = false

    private var deliveryStatus: OperatingHours.Status { OperatingHours.status() }
    private var visibleCategories: [ServiceCategory] {
        let base = showAllCategories ? ServiceCatalog.all : ServiceCatalog.featured
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return base }
        return ServiceCatalog.all.filter { category in
            category.name.localizedCaseInsensitiveContains(query)
                || category.summary.localizedCaseInsensitiveContains(query)
                || category.items.contains { $0.name.localizedCaseInsensitiveContains(query) }
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    greetingRow
                    locationCard
                    OperatingHoursBanner(status: deliveryStatus)

                    if config.appVersion.isOutdated {
                        outdatedVersionBar
                    }

                    fuelRateCard
                    searchField
                    servicesSection
                    activeOrderSection
                    transparencyNote
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 100)
            }
            .background(ZyphuelColor.background.ignoresSafeArea())
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle("Zyphuel")
            .navigationBarTitleDisplayMode(.inline)
            .zyphuelNavBar()
            .overlay(alignment: .bottom) { orderNowButton }
            .sheet(item: $selectedItem) { item in
                OrderSheetView(item: item)
                    .environmentObject(appState)
                    .environmentObject(config)
            }
            .sheet(isPresented: $showUpdates) {
                OrderUpdatesSheet()
                    .environmentObject(appState)
            }
            .sheet(isPresented: $showProfileMenu) {
                ProfileView()
                    .environmentObject(appState)
            }
            .alert("Delivery address", isPresented: $showAddressEditor) {
                TextField("Street, block, area", text: $draftAddress)
                Button("Cancel", role: .cancel) {}
                Button("Save") {
                    let trimmed = draftAddress.trimmingCharacters(in: .whitespacesAndNewlines)
                    if !trimmed.isEmpty { location.resolve(address: trimmed) }
                }
            } message: {
                Text("Enter the Lahore address where the rider should deliver.")
            }
            .onAppear {
                if location.address.isEmpty { draftAddress = "" } else { draftAddress = location.address }
            }
            .task { await appState.requestNotificationsIfNeeded() }
        }
    }

    // MARK: Header

    private var greetingRow: some View {
        HStack(spacing: 12) {
            Button {
                showProfileMenu = true
            } label: {
                ZStack {
                    Circle().fill(ZyphuelColor.brand.opacity(0.12))
                    Text(appState.currentUser?.displayInitials ?? "Z")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(ZyphuelColor.brand)
                }
                .frame(width: 42, height: 42)
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 2) {
                Text("Assalam-o-Alaikum")
                    .font(.system(size: 11))
                    .foregroundStyle(ZyphuelColor.textSecondary)
                HStack(spacing: 4) {
                    Text(appState.currentUser?.name.isEmpty == false ? appState.currentUser!.name : "Zyphuel Customer")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(ZyphuelColor.textPrimary)
                        .lineLimit(1)
                    if appState.currentUser?.isSuperAdmin == true {
                        Image(systemName: "checkmark.seal.fill")
                            .font(.system(size: 13))
                            .foregroundStyle(ZyphuelColor.verified)
                    }
                }
            }
            Spacer()

            Button {
                showUpdates = true
            } label: {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: "bell.fill")
                        .font(.system(size: 16))
                        .foregroundStyle(ZyphuelColor.textPrimary)
                        .padding(10)
                        .background(Circle().fill(Color.white))
                    if !appState.orders.currentOrders.isEmpty {
                        Circle()
                            .fill(ZyphuelColor.danger)
                            .frame(width: 8, height: 8)
                            .offset(x: 1, y: 1)
                    }
                }
            }
        }
        .padding(.top, 4)
    }

    private var locationCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label("Deliver to Lahore", systemImage: "mappin.and.ellipse")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.85))
                Spacer()
                HStack(spacing: 4) {
                    Circle()
                        .fill(ZyphuelColor.success)
                        .frame(width: 6, height: 6)
                    Text("LIVE AREA")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(.white.opacity(0.9))
                }
            }

            Button {
                draftAddress = location.address
                showAddressEditor = true
            } label: {
                HStack(alignment: .top, spacing: 8) {
                    Text(location.address.isEmpty ? ZyphuelConfig.defaultDeliveryAddress : location.address)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.leading)
                        .lineLimit(2)
                    Spacer()
                    Image(systemName: "pencil")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.85))
                }
            }
            .buttonStyle(.plain)

            HStack(spacing: 9) {
                Button {
                    location.refresh()
                } label: {
                    Label(location.isResolving ? "Locating…" : "Auto Detect GPS",
                          systemImage: "location.fill")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Capsule().fill(.white.opacity(0.18)))
                }
                .disabled(location.isResolving)

                ShareLink(item: location.shareMessage) {
                    Label("Share", systemImage: "square.and.arrow.up")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Capsule().fill(.white.opacity(0.18)))
                }
                Spacer()
            }

            Text(ZyphuelConfig.coverageLabel)
                .font(.system(size: 10))
                .foregroundStyle(.white.opacity(0.65))
        }
        .padding(16)
        .background(
            LinearGradient(
                colors: [ZyphuelColor.navy, ZyphuelColor.brandDark, ZyphuelColor.ocean],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var outdatedVersionBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "arrow.down.circle.fill")
                .foregroundStyle(ZyphuelColor.brand)
            VStack(alignment: .leading, spacing: 2) {
                Text("New Version Available")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.textPrimary)
                Text("Version \(config.appVersion.latestVersionName) is ready to download.")
                    .font(.system(size: 10))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
            Spacer()
            if let link = config.appVersion.downloadURL, let url = URL(string: link) {
                Link("Download", destination: url)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.brand)
            }
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 14).fill(ZyphuelColor.brand.opacity(0.07)))
    }

    // MARK: Rates

    private var fuelRateCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(title: "Today's OGRA Rates", subtitle: "Pump rate includes doorstep service")
            RateRow(name: "Super Euro-V Petrol", rate: PricingEngine.pumpRate(base: config.rates.petrol, serviceType: "petrol"), badge: "LIVE")
            Divider()
            RateRow(name: "High-Speed Diesel", rate: PricingEngine.pumpRate(base: config.rates.diesel, serviceType: "diesel"), badge: "LIVE")
            Divider()
            RateRow(name: "High-Octane 97", rate: PricingEngine.pumpRate(base: config.rates.highOctane, serviceType: "octane"), badge: "LIVE")
            Text("LPG gas cylinders and pure water delivery are temporarily unavailable.")
                .font(.system(size: 10))
                .foregroundStyle(ZyphuelColor.textSecondary)
        }
        .zyphuelCard()
    }

    private var searchField: some View {
        HStack(spacing: 9) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(ZyphuelColor.textSecondary)
            TextField("Search fuel, repair, tyres, battery…", text: $searchQuery)
                .font(.system(size: 14))
            if !searchQuery.isEmpty {
                Button {
                    searchQuery = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(ZyphuelColor.textSecondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Capsule().fill(Color.white))
        .overlay(Capsule().stroke(ZyphuelColor.border, lineWidth: 1))
    }

    // MARK: Services

    private var servicesSection: some View {
        VStack(spacing: 12) {
            SectionHeader(
                title: "Our Services",
                subtitle: showAllCategories ? "All 10 categories" : "7 featured categories"
            )

            if visibleCategories.isEmpty {
                EmptyStateView(
                    icon: "magnifyingglass",
                    title: "No matching service",
                    message: "Try petrol, battery, tyres or roadside assistance."
                )
            } else {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(visibleCategories) { category in
                        ServiceCategoryCard(category: category) {
                            if let first = category.items.first(where: { $0.isActive }) {
                                selectedItem = first
                            }
                        }
                    }
                }
            }

            Button {
                withAnimation(.easeInOut(duration: 0.2)) { showAllCategories.toggle() }
            } label: {
                Text(showAllCategories ? "Show Featured Only ▴" : "View All 10 Categories (+3 More: EV, Lubricants, Fleet) ▾")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.brand)
            }
        }
    }

    private var activeOrderSection: some View {
        VStack(spacing: 10) {
            if let order = appState.orders.currentOrders.first {
                SectionHeader(title: "Current Order", subtitle: order.serviceType)
                NavigationLink {
                    TrackerView(order: order)
                        .environmentObject(appState)
                        .environmentObject(language)
                } label: {
                    OrderSummaryCard(order: order)
                }
                .buttonStyle(.plain)
            }

            if !appState.orders.pastOrders.isEmpty {
                SectionHeader(
                    title: "Recent Orders",
                    subtitle: "\(appState.orders.pastOrders.count) completed or cancelled"
                )
                Button {
                    showUpdates = true
                } label: {
                    Text(language.tr("home.viewHistory", "View Full Order History →"))
                        .font(.system(size: 13, weight: .semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(SecondaryButtonStyle())
            }
        }
    }

    private var transparencyNote: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("About Zyphuel")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(ZyphuelColor.textPrimary)
            Text("Zyphuel is an early-stage Lahore startup run by a small local team. Fuel is delivered Cash on Delivery inside our operating hours: \(OperatingHours.fullScheduleSummary).")
                .font(.system(size: 11))
                .foregroundStyle(ZyphuelColor.textSecondary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 14).fill(ZyphuelColor.brand.opacity(0.05)))
    }

    private var orderNowButton: some View {
        Group {
            if deliveryStatus.isOpen {
                Button {
                    selectedItem = ServiceCatalog.fuel.items.first { $0.id == "petrol_regular" }
                } label: {
                    Label(language.tr("home.orderNow", "Order Now"), systemImage: "bolt.fill")
                }
                .buttonStyle(PrimaryButtonStyle())
                .padding(.horizontal, 18)
                .padding(.bottom, 10)
                .background(
                    LinearGradient(colors: [ZyphuelColor.background.opacity(0), ZyphuelColor.background],
                                   startPoint: .top, endPoint: .bottom)
                )
            } else {
                Text("Ordering is paused. \(deliveryStatus.activeSchedule) — PKT")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(ZyphuelColor.textSecondary)
                    .padding(12)
                    .frame(maxWidth: .infinity)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Color.white))
                    .padding(.horizontal, 18)
                    .padding(.bottom, 10)
            }
        }
    }
}

// MARK: - Cards

struct ServiceCategoryCard: View {
    let category: ServiceCategory
    let onTap: () -> Void

    private var isUnavailable: Bool { !category.isActive || category.availability == .closed }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 9) {
                HStack {
                    Image(systemName: category.symbolName)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(isUnavailable ? ZyphuelColor.textSecondary : ZyphuelColor.brand)
                        .padding(9)
                        .background(
                            RoundedRectangle(cornerRadius: 11)
                                .fill((isUnavailable ? ZyphuelColor.textSecondary : ZyphuelColor.brand).opacity(0.1))
                        )
                    Spacer()
                    if let badge = category.items.first(where: { $0.badge != nil })?.badge {
                        BadgePill(text: badge, color: isUnavailable ? ZyphuelColor.textSecondary : ZyphuelColor.success)
                    }
                }

                Text(category.name)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.textPrimary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)

                Text(category.summary)
                    .font(.system(size: 10))
                    .foregroundStyle(ZyphuelColor.textSecondary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)

                Text(isUnavailable ? "Unavailable" : "From \(MoneyFormatter.pkr(category.items.filter(\.isActive).map(\.basePrice).min() ?? 0))")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(isUnavailable ? ZyphuelColor.danger : ZyphuelColor.brand)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .zyphuelCard(padding: 13, cornerRadius: 16)
        }
        .buttonStyle(.plain)
        .opacity(isUnavailable ? 0.7 : 1)
    }
}

struct OrderSummaryCard: View {
    let order: FuelOrder

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(order.serviceType)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(ZyphuelColor.textPrimary)
                Spacer()
                StatusBadge(status: order.status)
            }
            HStack(spacing: 12) {
                Label("\(order.quantityLiters)L", systemImage: "drop.fill")
                Label(order.totalPriceLabel, systemImage: "banknote")
            }
            .font(.system(size: 11))
            .foregroundStyle(ZyphuelColor.textSecondary)

            Text(order.deliveryAddress)
                .font(.system(size: 11))
                .foregroundStyle(ZyphuelColor.textSecondary)
                .lineLimit(1)

            DeliveryStepper(status: order.status)
        }
        .zyphuelCard(padding: 14)
    }
}

// MARK: - Order updates sheet

struct OrderUpdatesSheet: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                if appState.orders.orders.isEmpty {
                    Section {
                        Text("No dispatch updates yet. Place an order to receive live status changes.")
                            .font(.system(size: 13))
                            .foregroundStyle(ZyphuelColor.textSecondary)
                    }
                } else {
                    Section("Order updates") {
                        ForEach(appState.orders.orders) { order in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(order.serviceType).font(.system(size: 13, weight: .semibold))
                                    Spacer()
                                    StatusBadge(status: order.status)
                                }
                                Text("\(order.quantityLiters)L • \(order.totalPriceLabel) • \(order.createdAt.formatted(date: .abbreviated, time: .shortened))")
                                    .font(.system(size: 11))
                                    .foregroundStyle(ZyphuelColor.textSecondary)
                            }
                            .padding(.vertical, 2)
                        }
                    }
                }
            }
            .navigationTitle("Notifications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
