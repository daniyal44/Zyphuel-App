import SwiftUI

/// Admin console. Mirrors the Android dashboard tabs: overview, orders, riders,
/// customers, live rates and the email gateway.
struct AdminDashboardView: View {
    private enum Tab: String, CaseIterable, Identifiable {
        case overview = "Overview"
        case orders = "Orders"
        case riders = "Riders"
        case customers = "Customers"
        case rates = "Rates"
        case email = "Email"
        var id: String { rawValue }
    }

    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var config: SystemConfigService
    @State private var tab: Tab = .overview

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                tabBar
                Divider()
                ScrollView {
                    Group {
                        switch tab {
                        case .overview: AdminOverviewTab()
                        case .orders: AdminOrdersTab()
                        case .riders: AdminRidersTab()
                        case .customers: AdminCustomersTab()
                        case .rates: AdminRatesTab()
                        case .email: AdminEmailTab()
                        }
                    }
                    .padding(16)
                    .padding(.bottom, 24)
                }
                .background(ZyphuelColor.background)
            }
            .background(ZyphuelColor.background.ignoresSafeArea())
            .navigationTitle("Admin Dashboard")
            .navigationBarTitleDisplayMode(.inline)
            .zyphuelNavBar()
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("Refresh data") { Task { await appState.refreshProfile() } }
                        Button("Log out", role: .destructive) { appState.signOut() }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .task {
                appState.users.observeRiders()
                appState.users.observeCustomers()
                appState.orders.observeAllOrders()
            }
            .refreshable { await appState.refreshProfile() }
        }
    }

    private var tabBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Tab.allCases) { item in
                    Button {
                        withAnimation(.easeInOut(duration: 0.15)) { tab = item }
                    } label: {
                        Text(item.rawValue)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(tab == item ? .white : ZyphuelColor.textPrimary)
                            .padding(.horizontal, 13)
                            .padding(.vertical, 8)
                            .background(Capsule().fill(tab == item ? ZyphuelColor.brand : Color.white))
                            .overlay(Capsule().stroke(ZyphuelColor.border, lineWidth: tab == item ? 0 : 1))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .background(ZyphuelColor.surface)
    }
}

// MARK: - Overview

private struct AdminOverviewTab: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var config: SystemConfigService

    private var allOrders: [FuelOrder] { appState.orders.orders }
    private var revenue: Double {
        allOrders.filter { $0.status == .completed }.reduce(0) { $0 + $1.totalPrice }
    }
    private var pendingCount: Int { allOrders.filter { $0.status == .pending }.count }

    var body: some View {
        VStack(spacing: 14) {
            HStack(spacing: 10) {
                StatTile(title: "Total orders", value: "\(allOrders.count)", caption: "all time")
                StatTile(title: "Pending", value: "\(pendingCount)", caption: "awaiting dispatch",
                         tint: ZyphuelColor.warning)
            }
            HStack(spacing: 10) {
                StatTile(title: "Completed", value: "\(allOrders.filter { $0.status == .completed }.count)",
                         caption: "delivered", tint: ZyphuelColor.success)
                StatTile(title: "COD collected", value: MoneyFormatter.pkr(revenue), caption: "completed",
                         tint: ZyphuelColor.ocean)
            }

            VStack(alignment: .leading, spacing: 10) {
                SectionHeader(title: "Riders", subtitle: "\(appState.users.riders.count) registered")
                HStack(spacing: 10) {
                    StatTile(title: "Verified", value: "\(appState.users.riders.filter(\.isVerified).count)",
                             caption: "can deliver", tint: ZyphuelColor.success)
                    StatTile(title: "Pending", value: "\(appState.users.riders.filter { !$0.isVerified }.count)",
                             caption: "need review", tint: ZyphuelColor.warning)
                }
            }

            VStack(alignment: .leading, spacing: 9) {
                SectionHeader(title: "App version", subtitle: "system_config/app_version")
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(config.appVersion.latestVersionName)
                            .font(.system(size: 15, weight: .bold))
                        Text("Published build \(config.appVersion.latestVersionCode) • this build \(ZyphuelConfig.buildNumber)")
                            .font(.system(size: 10))
                            .foregroundStyle(ZyphuelColor.textSecondary)
                    }
                    Spacer()
                    BadgePill(
                        text: config.appVersion.isOutdated ? "UPDATE LIVE" : "CURRENT",
                        color: config.appVersion.isOutdated ? ZyphuelColor.warning : ZyphuelColor.success,
                        filled: true
                    )
                }
            }
            .zyphuelCard()

            OperatingHoursBanner(status: config.deliveryStatus)

            VStack(alignment: .leading, spacing: 8) {
                SectionHeader(title: "Operational rules", subtitle: "Enforced on both platforms")
                Text("Max \(PricingEngine.maxLiters)L per fuel order • Rs. 300–400 tiered delivery • +\(MoneyFormatter.pkr(PricingEngine.pumpRateMarkup))/L pump markup • COD only")
                    .font(.system(size: 11))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .zyphuelCard()
        }
    }
}

// MARK: - Orders

private struct AdminOrdersTab: View {
    private enum Filter: String, CaseIterable, Identifiable {
        case all = "All", pending = "Pending", active = "Active"
        case completed = "Completed", cancelled = "Cancelled"
        var id: String { rawValue }
    }

    @EnvironmentObject private var appState: AppState
    @State private var filter: Filter = .all
    @State private var declineTarget: FuelOrder?
    @State private var declineReason = "Out of delivery coverage"
    @State private var busyOrderID: String?

    private let declineReasons = [
        "Out of delivery coverage",
        "Depot fuel stock limit",
        "Customer cancellation request",
        "Phone unreachable"
    ]

    private var filtered: [FuelOrder] {
        appState.orders.orders.filter { order in
            switch filter {
            case .all: return true
            case .pending: return order.status == .pending
            case .active: return order.status == .assigned || order.status == .delivering
            case .completed: return order.status == .completed
            case .cancelled: return order.status == .cancelled
            }
        }
    }

    var body: some View {
        VStack(spacing: 12) {
            ScrollView(.horizontal, showsIndicators: false) {
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
                }
            }

            if filtered.isEmpty {
                EmptyStateView(icon: "shippingbox", title: "No orders here",
                               message: "New COD orders appear as soon as a customer checks out.")
            } else {
                ForEach(filtered) { order in
                    AdminOrderCard(
                        order: order,
                        isBusy: busyOrderID == order.id,
                        onAccept: { Task { await accept(order) } },
                        onStart: { Task { await transition(order, .delivering) } },
                        onComplete: { Task { await transition(order, .completed) } },
                        onDecline: { declineTarget = order },
                        onDelete: { Task { await delete(order) } }
                    )
                }
            }
        }
        .confirmationDialog(
            "Cancel order",
            isPresented: Binding(
                get: { declineTarget != nil },
                set: { if !$0 { declineTarget = nil } }
            ),
            titleVisibility: .visible
        ) {
            ForEach(declineReasons, id: \.self) { reason in
                Button(reason, role: .destructive) {
                    guard let order = declineTarget else { return }
                    declineTarget = nil
                    Task { await decline(order, reason: reason) }
                }
            }
            Button("Keep order", role: .cancel) { declineTarget = nil }
        } message: {
            Text("The customer receives a cancellation email with the selected reason.")
        }
    }

    private func accept(_ order: FuelOrder) async {
        busyOrderID = order.id
        defer { busyOrderID = nil }
        // Prefer an on-duty verified rider; otherwise the admin self-delivers, exactly
        // like the Android "Admin Direct Delivery" fallback.
        if let rider = appState.users.riders.first(where: { $0.isActiveRider }) {
            await appState.orders.assignRider(order: order, rider: rider)
        } else if let admin = appState.currentUser {
            await appState.orders.assignRider(order: order, rider: admin)
        }
    }

    private func transition(_ order: FuelOrder, _ status: OrderStatus) async {
        busyOrderID = order.id
        defer { busyOrderID = nil }
        _ = await appState.orders.advance(order: order, to: status, isAdmin: true)
    }

    private func decline(_ order: FuelOrder, reason: String) async {
        busyOrderID = order.id
        defer { busyOrderID = nil }
        await appState.orders.decline(order: order, reason: reason,
                                      by: appState.currentUser?.email ?? "")
        await appState.users.writeAudit(
            action: "order_declined",
            performedBy: appState.currentUser?.email ?? "admin",
            details: "Order \(order.id) cancelled: \(reason)"
        )
    }

    private func delete(_ order: FuelOrder) async {
        busyOrderID = order.id
        defer { busyOrderID = nil }
        await appState.orders.deleteCancelled(order: order)
    }
}

private struct AdminOrderCard: View {
    let order: FuelOrder
    let isBusy: Bool
    let onAccept: () -> Void
    let onStart: () -> Void
    let onComplete: () -> Void
    let onDecline: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(order.serviceType)
                    .font(.system(size: 14, weight: .bold))
                Spacer()
                StatusBadge(status: order.status)
            }

            Text(order.deliveryAddress)
                .font(.system(size: 11))
                .foregroundStyle(ZyphuelColor.textSecondary)

            HStack(spacing: 12) {
                Label("\(order.quantityLiters)L", systemImage: "drop.fill")
                Label(order.totalPriceLabel, systemImage: "banknote")
                if let rider = order.riderName, !rider.isEmpty {
                    Label(rider, systemImage: "person.fill")
                }
            }
            .font(.system(size: 11))
            .foregroundStyle(ZyphuelColor.textSecondary)

            HStack(spacing: 8) {
                if order.status == .pending {
                    actionButton("Accept", color: ZyphuelColor.brand, action: onAccept)
                }
                if order.status == .assigned {
                    actionButton("Start Delivery 🚚", color: ZyphuelColor.brand, action: onStart)
                }
                if order.status == .delivering {
                    actionButton("Mark Delivered ✅", color: ZyphuelColor.success, action: onComplete)
                }
                if order.status.isCurrent {
                    actionButton("Decline ❌", color: ZyphuelColor.danger, action: onDecline)
                }
                if order.status == .cancelled {
                    actionButton("Delete", color: ZyphuelColor.danger, action: onDelete)
                }
            }
            .disabled(isBusy)
        }
        .zyphuelCard(padding: 14)
    }

    private func actionButton(_ title: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            if isBusy {
                ProgressView().tint(color)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
            } else {
                Text(title)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(color)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
                    .background(Capsule().fill(color.opacity(0.1)))
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Riders

private struct AdminRidersTab: View {
    @EnvironmentObject private var appState: AppState
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 12) {
            if appState.users.riders.isEmpty {
                EmptyStateView(icon: "scooter", title: "No riders yet",
                               message: "Rider sign-ups appear here for verification.")
            } else {
                ForEach(appState.users.riders) { rider in
                    VStack(alignment: .leading, spacing: 9) {
                        HStack(spacing: 10) {
                            ZStack {
                                Circle().fill(ZyphuelColor.brand.opacity(0.1))
                                Text(rider.displayInitials)
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundStyle(ZyphuelColor.brand)
                            }
                            .frame(width: 38, height: 38)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(rider.name.isEmpty ? rider.email : rider.name)
                                    .font(.system(size: 13, weight: .semibold))
                                Text(rider.email)
                                    .font(.system(size: 10))
                                    .foregroundStyle(ZyphuelColor.textSecondary)
                            }
                            Spacer()
                            BadgePill(
                                text: rider.isVerified ? "VERIFIED" : "PENDING",
                                color: rider.isVerified ? ZyphuelColor.success : ZyphuelColor.warning,
                                filled: true
                            )
                        }

                        HStack(spacing: 8) {
                            Button(rider.isVerified ? "Revoke" : "Approve") {
                                Task {
                                    await appState.users.setRiderVerified(rider, verified: !rider.isVerified)
                                    if let message = appState.users.errorMessage { errorMessage = message }
                                }
                            }
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(rider.isVerified ? ZyphuelColor.warning : ZyphuelColor.success)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 9)
                            .background(
                                Capsule().fill(
                                    (rider.isVerified ? ZyphuelColor.warning : ZyphuelColor.success).opacity(0.1)
                                )
                            )

                            Button("Delete", role: .destructive) {
                                Task {
                                    let deleted = await appState.users.deleteAccount(rider)
                                    if !deleted, let message = appState.users.errorMessage {
                                        errorMessage = message
                                    } else if deleted {
                                        await appState.users.writeAudit(
                                            action: "rider_deleted",
                                            performedBy: appState.currentUser?.email ?? "admin",
                                            details: "Rider \(rider.email) removed"
                                        )
                                    }
                                }
                            }
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(ZyphuelColor.danger)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 9)
                            .background(Capsule().fill(ZyphuelColor.danger.opacity(0.1)))
                        }
                        .buttonStyle(.plain)
                    }
                    .zyphuelCard(padding: 13, cornerRadius: 16)
                }
            }
        }
        .alert("Rider action", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }
}

// MARK: - Customers

private struct AdminCustomersTab: View {
    @EnvironmentObject private var appState: AppState
    @State private var search = ""
    @State private var confirmTarget: AppUser?
    @State private var errorMessage: String?

    private var customers: [AppUser] {
        let query = search.trimmingCharacters(in: .whitespaces)
        guard !query.isEmpty else { return appState.users.customers }
        return appState.users.customers.filter {
            $0.email.localizedCaseInsensitiveContains(query) || $0.name.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(ZyphuelColor.textSecondary)
                TextField("Search customers", text: $search)
                    .font(.system(size: 13))
            }
            .padding(.horizontal, 13)
            .padding(.vertical, 10)
            .background(Capsule().fill(Color.white))
            .overlay(Capsule().stroke(ZyphuelColor.border, lineWidth: 1))

            if customers.isEmpty {
                EmptyStateView(icon: "person.2", title: "No customers found",
                               message: "Customer accounts sync from Firestore in real time.")
            } else {
                ForEach(customers) { customer in
                    HStack(spacing: 10) {
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 4) {
                                Text(customer.name.isEmpty ? customer.email : customer.name)
                                    .font(.system(size: 13, weight: .semibold))
                                if customer.isSuperAdmin {
                                    Image(systemName: "checkmark.seal.fill")
                                        .font(.system(size: 11))
                                        .foregroundStyle(ZyphuelColor.verified)
                                }
                            }
                            Text(customer.email)
                                .font(.system(size: 10))
                                .foregroundStyle(ZyphuelColor.textSecondary)
                            Text(customer.phoneNumber.isEmpty ? "No phone on file" : customer.phoneNumber)
                                .font(.system(size: 10))
                                .foregroundStyle(ZyphuelColor.textSecondary)
                        }
                        Spacer()
                        Button {
                            confirmTarget = customer
                        } label: {
                            Image(systemName: "trash")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(ZyphuelColor.danger)
                                .padding(8)
                                .background(Circle().fill(ZyphuelColor.danger.opacity(0.08)))
                        }
                        .buttonStyle(.plain)
                    }
                    .zyphuelCard(padding: 12, cornerRadius: 14)
                }
            }
        }
        .confirmationDialog(
            "Delete customer?",
            isPresented: Binding(
                get: { confirmTarget != nil },
                set: { if !$0 { confirmTarget = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("Delete account permanently", role: .destructive) {
                guard let user = confirmTarget else { return }
                confirmTarget = nil
                Task {
                    let deleted = await appState.users.deleteAccount(user)
                    if deleted {
                        await appState.users.writeAudit(
                            action: "customer_deleted",
                            performedBy: appState.currentUser?.email ?? "admin",
                            details: "Customer \(user.email) removed"
                        )
                    } else if let message = appState.users.errorMessage {
                        errorMessage = message
                    }
                }
            }
            Button("Cancel", role: .cancel) { confirmTarget = nil }
        } message: {
            Text("The root super admin account is permanently protected and cannot be deleted.")
        }
        .alert("Delete failed", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }
}

// MARK: - Rates

private struct AdminRatesTab: View {
    @EnvironmentObject private var config: SystemConfigService

    @State private var petrol = ""
    @State private var diesel = ""
    @State private var octane = ""
    @State private var saved = false
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 9) {
                SectionHeader(title: "Live OGRA base rates",
                              subtitle: "Pump price adds Rs. 5.00/L automatically")
                LabeledField(label: "Super Euro-V Petrol", placeholder: "289.38", text: $petrol,
                             keyboard: .decimalPad)
                LabeledField(label: "High-Speed Diesel", placeholder: "289.84", text: $diesel,
                             keyboard: .decimalPad)
                LabeledField(label: "High-Octane 97", placeholder: "300.00", text: $octane,
                             keyboard: .decimalPad)
            }
            .zyphuelCard()

            VStack(alignment: .leading, spacing: 8) {
                Text("Customer-facing pump rates")
                    .font(.system(size: 12, weight: .semibold))
                RateRow(name: "Petrol", rate: PricingEngine.pumpRate(base: parsed(petrol, config.rates.petrol), serviceType: "petrol"))
                Divider()
                RateRow(name: "Diesel", rate: PricingEngine.pumpRate(base: parsed(diesel, config.rates.diesel), serviceType: "diesel"))
                Divider()
                RateRow(name: "High-Octane", rate: PricingEngine.pumpRate(base: parsed(octane, config.rates.highOctane), serviceType: "octane"))
            }
            .zyphuelCard()

            Button {
                Task { await save() }
            } label: {
                Text(saved ? "Saved ✓" : "Save & Sync to Cloud")
            }
            .buttonStyle(PrimaryButtonStyle())

            Text("Rates sync to every Android and iOS device through system_config/fuel_rates.")
                .font(.system(size: 10))
                .foregroundStyle(ZyphuelColor.textSecondary)
                .multilineTextAlignment(.center)
        }
        .onAppear {
            petrol = String(format: "%.2f", config.rates.petrol)
            diesel = String(format: "%.2f", config.rates.diesel)
            octane = String(format: "%.2f", config.rates.highOctane)
        }
        .alert("Rates not saved", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private func parsed(_ value: String, _ fallback: Double) -> Double {
        Double(value.replacingOccurrences(of: ",", with: "")) ?? fallback
    }

    private func save() async {
        guard let petrolValue = Double(petrol), let dieselValue = Double(diesel), let octaneValue = Double(octane),
              petrolValue > 0, dieselValue > 0, octaneValue > 0 else {
            errorMessage = "Enter valid positive rates for all three fuels."
            return
        }
        await config.saveRates(petrol: petrolValue, diesel: dieselValue, highOctane: octaneValue)
        if let message = config.errorMessage {
            errorMessage = message
        } else {
            saved = true
        }
    }
}

// MARK: - Email gateway

private struct AdminEmailTab: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var config: SystemConfigService

    @State private var webhook = ""
    @State private var sender = ""
    @State private var statusMessage: String?
    @State private var isTesting = false

    var body: some View {
        VStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 9) {
                SectionHeader(title: "Email gateway", subtitle: "Shared with Android dispatch")
                LabeledField(label: "HTTPS Webhook Relay", placeholder: "https://script.google.com/…",
                             text: $webhook, keyboard: .URL, autocapitalise: .never)
                LabeledField(label: "Sender email", placeholder: "orders@zyphuel.app",
                             text: $sender, keyboard: .emailAddress, autocapitalise: .never)
                Text("iOS sends through the HTTPS relay and the Firestore mail trigger. Raw SMTP is Android-only.")
                    .font(.system(size: 10))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }
            .zyphuelCard()

            HStack(spacing: 9) {
                Button {
                    Task {
                        await config.saveGateway(webhookURL: webhook.trimmingCharacters(in: .whitespaces),
                                                 senderEmail: sender.trimmingCharacters(in: .whitespaces))
                        statusMessage = config.errorMessage ?? "Gateway saved and synced."
                    }
                } label: {
                    Text("Save & Sync")
                }
                .buttonStyle(PrimaryButtonStyle())

                Button {
                    Task { await sendTest() }
                } label: {
                    if isTesting {
                        ProgressView().tint(ZyphuelColor.brand)
                    } else {
                        Text("Send Test")
                    }
                }
                .buttonStyle(SecondaryButtonStyle())
                .disabled(isTesting)
            }

            if let statusMessage {
                Text(statusMessage)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(ZyphuelColor.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Color.white))
            }

            VStack(alignment: .leading, spacing: 6) {
                Text("Delivery channels")
                    .font(.system(size: 12, weight: .semibold))
                channelRow("HTTPS Webhook Relay (port 443)", configured: !(config.emailGateway.webhookURL ?? "").isEmpty)
                channelRow("Firestore mail trigger", configured: true)
                channelRow("Direct SMTP (Android devices only)", configured: false)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .zyphuelCard()
        }
        .onAppear {
            webhook = config.emailGateway.webhookURL ?? ""
            sender = config.emailGateway.senderEmail ?? ""
        }
    }

    private func channelRow(_ title: String, configured: Bool) -> some View {
        HStack(spacing: 8) {
            Image(systemName: configured ? "checkmark.circle.fill" : "circle.dashed")
                .font(.system(size: 12))
                .foregroundStyle(configured ? ZyphuelColor.success : ZyphuelColor.textSecondary)
            Text(title)
                .font(.system(size: 11))
                .foregroundStyle(ZyphuelColor.textSecondary)
            Spacer()
            Text(configured ? "READY" : "INACTIVE")
                .font(.system(size: 9, weight: .bold))
                .foregroundStyle(configured ? ZyphuelColor.success : ZyphuelColor.textSecondary)
        }
    }

    private func sendTest() async {
        isTesting = true
        defer { isTesting = false }
        let recipient = sender.isEmpty ? (appState.currentUser?.email ?? "") : sender
        guard !recipient.isEmpty else {
            statusMessage = "Enter a sender/recipient email first."
            return
        }
        let gateway = EmailGateway()
        let result = await gateway.send(
            to: recipient,
            subject: "Zyphuel iOS test email",
            body: "This is a live dispatch test from the Zyphuel iOS admin console.",
            senderUID: appState.auth.currentUID,
            gateway: config.emailGateway
        )
        statusMessage = result.delivered
            ? "Test queued via \(result.channel.rawValue)."
            : "Test failed: \(result.detail)"
    }
}
