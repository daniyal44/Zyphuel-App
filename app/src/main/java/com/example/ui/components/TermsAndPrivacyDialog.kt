package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig

/**
 * Interactive, Play Store compliant In-App Terms & Conditions and Privacy Policy Dialog.
 * Allows users and riders to review data safety, hazardous materials rules, and GPS telematics disclaimers.
 */
@Composable
fun TermsAndPrivacyDialog(
    initialTab: Int = 0, // 0 for Terms, 1 for Privacy
    onDismissRequest: () -> Unit,
    onAccept: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val termsScrollState = rememberScrollState()
    val privacyScrollState = rememberScrollState()
    val howItWorksScrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("terms_privacy_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Brand and App Version Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Legal & Compliance",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Zyphuel v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_terms_btn")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("terms_tab"),
                        text = {
                            Text(
                                "Terms",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        },
                        icon = { Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("privacy_tab"),
                        text = {
                            Text(
                                "Privacy",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        },
                        icon = { Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.testTag("how_it_works_tab"),
                        text = {
                            Text(
                                "How It Works",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        icon = { Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    if (selectedTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(termsScrollState),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Zyphuel Terms & Conditions of Service",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "Last Updated: September 2026 • Governed under Laws of Pakistan",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Highlighted Startup Disclosure
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "⚠️ CRITICAL EARLY-STAGE STARTUP NOTICE",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Please note: Zyphuel is currently an early-stage local startup operating in Lahore, Pakistan, run by an independent small founding team, and NOT a large corporate conglomerate or multinational commercial entity. All fuel, gas, and energy dispatches are handled with personalized attention as an agile startup in our designated Lahore coverage zones.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black, lineHeight = 16.sp)
                                    )
                                }
                            }

                            LegalSectionTitle("1. Service Scope & Operational Area")
                            LegalParagraph("Zyphuel operates an on-demand doorstep fuel, energy, and pure drinking water delivery service in Lahore, Pakistan. Our verified logistics fleet delivers core energy products directly to your doorstep: Super Petrol (Euro-V), High-Octane (HOBC 97), High-Speed Diesel (Regular & Generator Diesel), Sealed LPG Gas Cylinders (11.8kg), and Pure Drinking Water across Lahore coverage sectors (Gulberg, DHA, Model Town, Johar Town, Bahria Town, Green Town, Cantt, etc.).")

                            LegalSectionTitle("2. Platform Features & How the Service Works")
                            LegalParagraph("Zyphuel is a technology-driven doorstep delivery platform. After you place an order, a verified rider is dispatched and you can track the bowser live on the map in real time. Each delivery generates an itemized invoice that is emailed to you automatically, and the fuel rates shown in the app stay synced with official notified OGRA prices.")
                            LegalBulletPoint("Live GPS order tracking with real-time rider location.")
                            LegalBulletPoint("Automated real-time invoice generation and email receipts.")
                            LegalBulletPoint("Live OGRA-synced fuel pricing and instant delivery-milestone notifications.")
                            LegalBulletPoint("1-Tap Ordering, Cash on Delivery, optional biometric app lock, and an encrypted (TLS 1.3 / AES-256) secure cloud built on Google Firebase.")
                            LegalParagraph("For a full overview of everything Zyphuel offers, open the \"How It Works\" tab above.")

                            LegalSectionTitle("3. Safety & Petroleum Compliance (OGRA)")
                            LegalParagraph("Petroleum and LPG are hazardous and flammable materials. In strict compliance with Oil & Gas Regulatory Authority (OGRA) and Civil Defense regulations:")
                            LegalBulletPoint("Customers must maintain a strict 10-meter safety perimeter free from open flames, smoking, or active generators during fuel dispensing.")
                            LegalBulletPoint("Fuel is dispensed exclusively into motor vehicle fuel tanks or certified explosion-proof fuel canisters.")
                            LegalBulletPoint("All fuel bowsers utilize calibrated digital flow-meters and anti-adulteration security seals.")
                            LegalBulletPoint("Delivery riders have the statutory authority and safety obligation to decline dispensing if on-site conditions present a fire or safety hazard.")

                            LegalSectionTitle("4. Transparent Pricing & Cash on Delivery")
                            LegalParagraph("All petroleum and LPG prices strictly adhere to official notified OGRA retail prices. In accordance with petroleum retail distribution in Lahore, retail rates for Super Petrol, High-Speed Diesel, and High-Octane include a standard petrol pump rate adjustment of Rs. 2.50 per liter over base notifications, transparently calculated on checkout. Delivery charges are transparently itemized prior to checkout. Payment is collected 100% via Cash on Delivery (COD) upon inspection at your doorstep.")

                            LegalSectionTitle("5. Cancellations & Quality Assurance")
                            LegalParagraph("Orders may be cancelled free of charge prior to driver dispatch. Any quantity or quality inquiries can be reported immediately to our support team via in-app Live Support or WhatsApp within 2 hours of delivery.")

                            LegalSectionTitle("6. Permanent Account Deletion")
                            LegalParagraph("You maintain full control to permanently delete your account, marked location coordinates, and order data directly in the app via Profile Settings > Delete Account, or online at https://www.zyphuel.com/privacy.")

                            LegalSectionTitle("7. Contact & Support Helpline")
                            LegalParagraph("For customer assistance, business inquiries, or legal notices:\n• Phone / WhatsApp: +92 323 0112464\n• Support Email: m.daniyalkhan490@gmail.com\n• Operations: 75-Main Boulevard, Gulberg III, Lahore, Pakistan\n• Website: https://www.zyphuel.com/terms-of-use")
                        }
                    } else if (selectedTab == 1) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(privacyScrollState),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Zyphuel Privacy Policy & Data Safety",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            )
                            Text(
                                text = "Play Store Data Safety Compliant • TLS 1.3 & AES-256 Encrypted",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Highlighted Startup Disclosure
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "⚠️ STARTUP STATUS & PRIVACY TRANSPARENCY",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Please note: Zyphuel is currently an early-stage startup platform operating in Lahore, Pakistan, and NOT a large corporate entity. All user data, telemetry, and location permissions are processed with startup agility and strict end-to-end encryption exclusively to fulfill your on-demand service dispatches.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black, lineHeight = 16.sp)
                                    )
                                }
                            }

                            // Highlighted Location Disclosure
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "📍 Prominent Location & Permissions Disclosure",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Zyphuel accesses fine and coarse device location exclusively to identify your doorstep delivery coordinates in Lahore and route our bowsers directly to you. We do not track continuous background location when no delivery is active. Notification permissions are utilized solely for delivery milestone updates and official rate alerts.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                                    )
                                }
                            }

                            LegalSectionTitle("1. Information We Collect")
                            LegalBulletPoint("Account Identifiers: Full Name, Email Address, Phone Number, and Delivery Location Addresses.")
                            LegalBulletPoint("Order & Invoice History: Products ordered (Super Petrol, High Octane, Diesel, LPG, Water), quantities in liters, and COD invoice breakdown.")
                            LegalBulletPoint("Driver & Logistics Credentials: CNIC/ID, Driving License, Vehicle Registration plate, and emergency contact for verified couriers.")
                            LegalBulletPoint("Live Delivery Telemetry: Real-time GPS location is used only while an order is active — to route your rider to your doorstep — and stops once the delivery is complete.")

                            LegalSectionTitle("2. Data Protection & Zero Third-Party Sale")
                            LegalParagraph("We never sell, rent, or trade your personal information. Data is transmitted securely over encrypted TLS/SSL channels, and local credentials are protected with AES-256 Room database encryption. Our platform runs on a secure, industry-standard encrypted cloud backend (Google Firebase), with real-time delivery notifications delivered via Firebase Cloud Messaging (FCM).")

                            LegalSectionTitle("3. User Rights & Permanent Account Erasure")
                            LegalParagraph("In accordance with Google Play Developer policies, you have the right to permanently erase your account, location pins, and all stored data at any time via Profile Settings > Delete Account, or online at https://www.zyphuel.com/privacy.")

                            LegalSectionTitle("4. Privacy Support & Contact")
                            LegalParagraph("For privacy inquiries, data requests, or compliance questions:\n• Email: m.daniyalkhan490@gmail.com\n• Phone / WhatsApp: +92 323 0112464\n• Official Policy: https://www.zyphuel.com/privacy")
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(howItWorksScrollState),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "How Zyphuel Works",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            )
                            Text(
                                text = "Your on-demand doorstep energy platform • Lahore, Pakistan",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Foundation intro
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "⚡ What Zyphuel Is Built For",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Zyphuel is an on-demand doorstep delivery platform for fuel, energy and pure drinking water. Verified riders bring OGRA-priced fuel, LPG and water straight to your location — tracked live, paid by Cash on Delivery, and receipted instantly by email.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                                    )
                                }
                            }

                            // Products / base
                            LegalSectionTitle("What We Deliver")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ProductRow(Icons.Filled.LocalGasStation, "Super Petrol", "Euro-V grade motor fuel")
                                    ProductRow(Icons.Filled.Speed, "High-Octane (HOBC 97)", "Premium high-performance fuel")
                                    ProductRow(Icons.Filled.LocalShipping, "High-Speed Diesel", "Regular & generator diesel")
                                    ProductRow(Icons.Filled.LocalFireDepartment, "LPG Gas Cylinders", "Sealed & weighed 11.8 kg")
                                    ProductRow(Icons.Filled.WaterDrop, "Pure Drinking Water", "Sealed, certified drinking water")
                                }
                            }

                            // Platform features
                            LegalSectionTitle("Platform Features")
                            FeatureCard(Icons.Filled.LocationOn, "Live GPS Delivery Tracking", "Watch your rider and bowser move to your doorstep in real time on the map.", Color(0xFF0284C7))
                            FeatureCard(Icons.Filled.Receipt, "Real-Time Invoice & Email Receipts", "Every order creates an itemized invoice, emailed to you automatically.", Color(0xFF16A34A))
                            FeatureCard(Icons.Filled.TrendingUp, "Live OGRA Price Sync", "Fuel rates stay synced with official notified OGRA prices — no overcharging.", Color(0xFFF59E0B))
                            FeatureCard(Icons.Filled.LocalGasStation, "1-Tap Quick Order", "Select fuel type, quantity, and confirm delivery in seconds.", Color(0xFF0284C7))
                            FeatureCard(Icons.Filled.Payments, "Cash on Delivery", "Pay only after you inspect the delivery at your doorstep.", Color(0xFF16A34A))
                            FeatureCard(Icons.Filled.Fingerprint, "Biometric Security & Encryption", "Optional app lock with AES-256 storage and TLS 1.3 encrypted transfers.", Color(0xFF7C3AED))
                            FeatureCard(Icons.Filled.Notifications, "Instant Milestone Alerts", "Get notified as your order is accepted, dispatched and arriving.", Color(0xFFF59E0B))
                            FeatureCard(Icons.Filled.SupportAgent, "Live Support & Guided Tour", "In-app help center, WhatsApp support and a guided app tour.", Color(0xFF0284C7))

                            // Technology foundation (light)
                            LegalSectionTitle("Built On")
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    LegalBulletPoint("Secure cloud backend (Google Firebase) for accounts, orders and real-time sync.")
                                    LegalBulletPoint("Google Maps live routing guides riders to your exact delivery coordinates.")
                                    LegalBulletPoint("End-to-end encryption — TLS 1.3 in transit and AES-256 for local data.")
                                    LegalBulletPoint("Real-time push notifications (FCM) for delivery milestones and rate alerts.")
                                }
                            }

                            // Startup footer
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "🚀 Early-Stage Local Startup",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Zyphuel is an independent early-stage startup serving Lahore coverage sectors — Gulberg, DHA, Model Town, Johar Town, Bahria Town, Green Town, Cantt and more — with personalized, agile care.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Black, lineHeight = 16.sp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val targetUrl = if (selectedTab == 0) {
                                "https://www.zyphuel.com/terms-of-use"
                            } else {
                                "https://www.zyphuel.com/privacy"
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Online Web", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onAccept?.invoke()
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("accept_terms_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (onAccept != null) "Accept & Close" else "Close", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black),
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun LegalParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0F172A), lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    )
}

@Composable
private fun LegalBulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(start = 6.dp, top = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0F172A), lineHeight = 18.sp, fontWeight = FontWeight.Normal)
        )
    }
}

@Composable
private fun ProductRow(icon: ImageVector, name: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                )
            }
        }
    }
}
