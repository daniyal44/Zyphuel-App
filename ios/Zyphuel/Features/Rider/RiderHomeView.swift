import SwiftUI
import UIKit

/// Rider portal. Same lifecycle as the Android `RiderHomeScreen`: pending orders can be
/// accepted, then the rider must pick up fuel, start the route and finally collect COD.
struct RiderHomeView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var language: LanguageManager

    @State private var unassigned: [FuelOrder] = []
    @State private var isLoadingQueue = false
    @State private var isOnline = true
    @State private var errorMessage: String?
    @State private var busyOrderID: String?

    private var rider: AppUser? { appState.currentUser }
    private var isVerified: Bool { rider?.isVerified == true }

    private var activeDeliveries: [FuelOrder] {
        appState.orders.riderQueue.filter { $0.status.isCurrent }
    }
    private var completedDeliveries: [FuelOrder] {
        appState.orders.riderQueue.filter { $0.status == .completed }
    }
    private var cashCollected: Double {
        completedDeliveries.reduce(0) { $0 + $1.totalPrice }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    header
                    if !isVerified { verificationNotice }
                    statsRow
                    dutyToggle
                    activeSection
                    queueSection
                    completedSection
                    signOutButton
                }
                .padding(16)
                .padding(.bottom, 20)
            }
            .background(ZyphuelColor.background.ignoresSafeArea())
            .navigationTitle("Rider Portal")
            .navigationBarTitleDisplayMode(.inline)
            .zyphuelNavBar()
            .refreshable { await loadQueue() }
            .task {
                await loadQueue()
                appState.orders.observeRiderQueue(email: rider?.email ?? "")
            }
            .alert("Action failed", isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button("OK", role: .cancel) { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "")
            }
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(ZyphuelColor.brand.opacity(0.12))
                Text(rider?.displayInitials ?? "R")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(ZyphuelColor.brand)
            }
            .frame(width: 46, height: 46)

            VStack(alignment: .leading, spacing: 2) {
                Text(rider?.name ?? "Zyphuel Rider")
                    .font(.system(size: 16, weight: .bold))
                HStack(spacing: 5) {
                    Text(rider?.riderId ?? "RIDER")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(ZyphuelColor.textSecondary)
                    if isVerified {
                        BadgePill(text: "VERIFIED", color: ZyphuelColor.success, filled: true)
                    } else {
                        BadgePill(text: "PENDING APPROVAL", color: ZyphuelColor.warning)
                    }
                }
            }
            Spacer()
        }
    }

    private var verificationNotice: some View {
        VStack(alignment: .leading, spacing: 6) {
            Label("Verification pending", systemImage: "checkmark.shield")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(ZyphuelColor.warning)
            Text("An admin must approve your CNIC, licence and vehicle documents before you can accept deliveries.")
                .font(.system(size: 11))
                .foregroundStyle(ZyphuelColor.textSecondary)
        }
        .padding(13)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 14).fill(ZyphuelColor.warning.opacity(0.08)))
    }

    private var statsRow: some View {
        HStack(spacing: 10) {
            StatTile(title: "Active", value: "\(activeDeliveries.count)", caption: "in progress",
                     tint: ZyphuelColor.brand)
            StatTile(title: "Completed", value: "\(completedDeliveries.count)", caption: "all time",
                     tint: ZyphuelColor.success)
            StatTile(title: "COD", value: MoneyFormatter.pkr(cashCollected), caption: "collected",
                     tint: ZyphuelColor.ocean)
        }
    }

    private var dutyToggle: some View {
        HStack(spacing: 11) {
            Circle()
                .fill(isOnline ? ZyphuelColor.success : ZyphuelColor.textSecondary)
                .frame(width: 9, height: 9)
            Text(isOnline ? "On duty — accepting dispatches" : "Off duty")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(ZyphuelColor.textPrimary)
            Spacer()
            Toggle("", isOn: $isOnline)
                .labelsHidden()
                .tint(ZyphuelColor.success)
        }
        .zyphuelCard(padding: 14)
    }

    private var activeSection: some View {
        VStack(spacing: 10) {
            SectionHeader(title: "My Deliveries", subtitle: "\(activeDeliveries.count) assigned")
            if activeDeliveries.isEmpty {
                Text("No active delivery. Accept a pending order below.")
                    .font(.system(size: 12))
                    .foregroundStyle(ZyphuelColor.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Color.white))
            } else {
                ForEach(activeDeliveries) { order in
                    RiderOrderCard(
                        order: order,
                        isBusy: busyOrderID == order.id,
                        onPrimary: { Task { await advance(order) } },
                        onCallCustomer: { openDialer(order.customerPhone) }
                    )
                }
            }
        }
    }

    private var queueSection: some View {
        VStack(spacing: 10) {
            SectionHeader(
                title: "Available Orders",
                subtitle: isOnline ? "Tap accept to claim" : "Go on duty to accept",
                actionTitle: "Refresh",
                action: { Task { await loadQueue() } }
            )

            if !isOnline {
                closedQueueNotice
            } else if isLoadingQueue {
                ProgressView("Loading Lahore queue…")
                    .font(.system(size: 12))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
            } else if unassigned.isEmpty {
                EmptyStateView(
                    icon: "tray",
                    title: "Queue is clear",
                    message: "New COD orders appear here the moment a customer checks out."
                )
            } else {
                ForEach(unassigned) { order in
                    VStack(spacing: 10) {
                        OrderHistoryRow(order: order)
                        Button {
                            Task { await accept(order) }
                        } label: {
                            if busyOrderID == order.id {
                                ProgressView().tint(.white)
                            } else {
                                Label("Accept Ride", systemImage: "checkmark.circle.fill")
                            }
                        }
                        .buttonStyle(PrimaryButtonStyle(enabled: isVerified))
                        .disabled(!isVerified || busyOrderID != nil)
                    }
                }
            }
        }
    }

    private var closedQueueNotice: some View {
        Text("You are off duty. New orders are not offered while offline.")
            .font(.system(size: 11))
            .foregroundStyle(ZyphuelColor.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(RoundedRectangle(cornerRadius: 12).fill(ZyphuelColor.border.opacity(0.4)))
    }

    private var completedSection: some View {
        Group {
            if !completedDeliveries.isEmpty {
                VStack(spacing: 10) {
                    SectionHeader(title: "Completed", subtitle: "\(completedDeliveries.count) deliveries")
                    ForEach(completedDeliveries.prefix(5)) { order in
                        OrderHistoryRow(order: order)
                    }
                }
            }
        }
    }

    private var signOutButton: some View {
        Button(role: .destructive) {
            appState.signOut()
        } label: {
            Text(language.tr("profile.logout", "Log Out"))
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(ZyphuelColor.danger)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(RoundedRectangle(cornerRadius: 14).fill(ZyphuelColor.danger.opacity(0.07)))
        }
        .buttonStyle(.plain)
        .padding(.top, 8)
    }

    // MARK: Actions

    private func loadQueue() async {
        guard isOnline else {
            unassigned = []
            return
        }
        isLoadingQueue = true
        defer { isLoadingQueue = false }
        unassigned = await appState.orders.fetchUnassignedOrders()
    }

    private func accept(_ order: FuelOrder) async {
        guard let rider, isVerified else {
            errorMessage = "Your rider account is not verified yet."
            return
        }
        busyOrderID = order.id
        defer { busyOrderID = nil }
        await appState.orders.assignRider(order: order, rider: rider)
        unassigned.removeAll { $0.id == order.id }
        await appState.notifications.notify(
            title: "Delivery assigned",
            body: "\(order.serviceType) • \(order.quantityLiters)L to \(order.deliveryAddress)"
        )
    }

    /// Rider progression: Assigned → Delivering (pick up + start route) → Completed (collect COD).
    private func advance(_ order: FuelOrder) async {
        busyOrderID = order.id
        defer { busyOrderID = nil }
        let next: OrderStatus
        switch order.status {
        case .assigned: next = .delivering
        case .delivering: next = .completed
        default: return
        }
        let success = await appState.orders.advance(order: order, to: next, isAdmin: false)
        if success {
            await appState.notifications.notify(
                title: next == .completed ? "Delivery completed" : "Route started",
                body: next == .completed
                    ? "Collect \(order.totalPriceLabel) cash and hand over the fuel."
                    : "Navigate to \(order.deliveryAddress)."
            )
        } else if let message = appState.orders.errorMessage {
            errorMessage = message
        }
    }

    private func openDialer(_ phone: String) {
        let digits = phone.filter { $0.isNumber || $0 == "+" }
        guard !digits.isEmpty, let url = URL(string: "tel://\(digits)") else { return }
        UIApplication.shared.open(url)
    }
}

// MARK: - Rider order card

struct RiderOrderCard: View {
    let order: FuelOrder
    let isBusy: Bool
    let onPrimary: () -> Void
    let onCallCustomer: () -> Void

    private var primaryTitle: String {
        switch order.status {
        case .assigned: return "Pick Up Fuel & Start Route"
        case .delivering: return "Reached — Complete & Collect COD"
        default: return "No action"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 11) {
            HStack {
                Text(order.serviceType)
                    .font(.system(size: 14, weight: .bold))
                Spacer()
                StatusBadge(status: order.status)
            }

            HStack(spacing: 14) {
                Label("\(order.quantityLiters)L", systemImage: "drop.fill")
                Label(order.totalPriceLabel, systemImage: "banknote")
                Label(order.paymentMethod, systemImage: "creditcard")
            }
            .font(.system(size: 11))
            .foregroundStyle(ZyphuelColor.textSecondary)

            VStack(alignment: .leading, spacing: 3) {
                Text(order.customerName.isEmpty ? "Zyphuel customer" : order.customerName)
                    .font(.system(size: 12, weight: .semibold))
                Text(order.deliveryAddress)
                    .font(.system(size: 11))
                    .foregroundStyle(ZyphuelColor.textSecondary)
            }

            if order.status != .pending && order.status != .cancelled {
                DeliveryStepper(status: order.status)
            }

            HStack(spacing: 9) {
                Button(action: onCallCustomer) {
                    Label("Call", systemImage: "phone.fill")
                        .font(.system(size: 12, weight: .semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(ZyphuelColor.brand.opacity(0.1)))
                        .foregroundStyle(ZyphuelColor.brand)
                }
                .buttonStyle(.plain)

                Button(action: onPrimary) {
                    if isBusy {
                        ProgressView().tint(.white)
                    } else {
                        Text(primaryTitle)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(.white)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 11)
                .background(
                    Capsule().fill(order.status == .delivering ? ZyphuelColor.success : ZyphuelColor.brand)
                )
                .disabled(isBusy)
            }
        }
        .zyphuelCard(padding: 14)
    }
}
