import SwiftUI

/// Legal surface required for App Store review: terms, privacy, refunds, operating
/// hours and the early-stage startup disclosure used on Android.
struct LegalView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    section(
                        title: "Early-stage startup disclosure",
                        body: "Zyphuel is an early-stage delivery startup based in Lahore, Pakistan, run by a small local team. Service availability, coverage areas and delivery windows can change as we grow. We publish changes on this screen and in the app."
                    )

                    section(
                        title: "Delivery operating hours (PKT)",
                        body: """
                        Monday–Thursday: 08:00 AM – 08:00 PM
                        Friday: 08:00 AM – 01:00 PM
                        Saturday–Sunday: 10:00 AM – 06:00 PM

                        Orders are blocked outside these windows on every platform.
                        """
                    )

                    section(
                        title: "Pricing, COD and limits",
                        body: """
                        Fuel is delivered Cash on Delivery only. The pump rate shown includes a Rs. 5.00 per litre service markup on the official OGRA base rate.

                        Doorstep delivery fee: Rs. 300 up to 10L, then Rs. 320 / 340 / 360 / 380 / 400 for 11L to 15L. The maximum is 15 litres per order.

                        Pure water delivery and LPG gas cylinders are currently unavailable and cannot be ordered.
                        """
                    )

                    section(
                        title: "Privacy",
                        body: """
                        We collect your name, email, phone number and delivery address to fulfil orders. Payment is collected in cash; we never store card details.

                        Location is used only to fill in your delivery address and is not uploaded for live tracking. Live rider tracking is disabled on both platforms.

                        You can delete your account at any time from Profile → Delete Account. Deletion removes your profile and saved addresses; completed order totals are retained where required for accounting and tax records.
                        """
                    )

                    section(
                        title: "Security",
                        body: "Accounts are protected by Firebase Authentication. Passwords are never stored in plain text. Four incorrect attempts in one minute lock sign-in for five minutes."
                    )

                    section(
                        title: "Contact",
                        body: "WhatsApp or call \(ZyphuelConfig.whatsappDisplay). We aim to reply during operating hours."
                    )

                    VStack(alignment: .leading, spacing: 10) {
                        Link("Read the full Privacy Policy", destination: ZyphuelConfig.privacyURL)
                        Link("Read the full Terms & Conditions", destination: ZyphuelConfig.termsURL)
                    }
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(ZyphuelColor.brand)
                }
                .padding(18)
            }
            .background(ZyphuelColor.background.ignoresSafeArea())
            .navigationTitle("Terms & Privacy")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private func section(title: String, body: String) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(ZyphuelColor.textPrimary)
            Text(body)
                .font(.system(size: 12))
                .foregroundStyle(ZyphuelColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .zyphuelCard()
    }
}
