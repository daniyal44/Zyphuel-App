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
                                "Terms & Conditions",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
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
                                "Privacy Policy",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        icon = { Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
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

                            LegalSectionTitle("2. Safety & Petroleum Compliance (OGRA)")
                            LegalParagraph("Petroleum and LPG are hazardous and flammable materials. In strict compliance with Oil & Gas Regulatory Authority (OGRA) and Civil Defense regulations:")
                            LegalBulletPoint("Customers must maintain a strict 10-meter safety perimeter free from open flames, smoking, or active generators during fuel dispensing.")
                            LegalBulletPoint("Fuel is dispensed exclusively into motor vehicle fuel tanks or certified explosion-proof fuel canisters.")
                            LegalBulletPoint("All fuel bowsers utilize calibrated digital flow-meters and anti-adulteration security seals.")
                            LegalBulletPoint("Delivery riders have the statutory authority and safety obligation to decline dispensing if on-site conditions present a fire or safety hazard.")

                            LegalSectionTitle("3. Transparent Pricing & Cash on Delivery")
                            LegalParagraph("All petroleum and LPG prices strictly adhere to official notified OGRA retail prices. Delivery charges are transparently itemized prior to checkout. Payment is collected 100% via Cash on Delivery (COD) upon inspection at your doorstep.")

                            LegalSectionTitle("4. Cancellations & Quality Assurance")
                            LegalParagraph("Orders may be cancelled free of charge prior to driver dispatch. Any quantity or quality inquiries can be reported immediately to our support team via in-app Live Support or WhatsApp within 2 hours of delivery.")

                            LegalSectionTitle("5. Permanent Account Deletion")
                            LegalParagraph("You maintain full control to permanently delete your account, marked location coordinates, and order data directly in the app via Profile Settings > Delete Account, or online at https://zyphuel.netlify.app/privacy.")

                            LegalSectionTitle("6. Contact & Support Helpline")
                            LegalParagraph("For customer assistance, business inquiries, or legal notices:\n• Phone / WhatsApp: +92 323 0112464\n• Support Email: m.daniyalkhan490@gmail.com\n• Operations: 75-Main Boulevard, Gulberg III, Lahore, Pakistan\n• Website: https://zyphuel.netlify.app/terms-of-use")
                        }
                    } else {
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

                            LegalSectionTitle("2. Data Protection & Zero Third-Party Sale")
                            LegalParagraph("We never sell, rent, or trade your personal information. Data is transmitted securely over encrypted TLS/SSL channels, and local credentials are protected with AES-256 Room database encryption.")

                            LegalSectionTitle("3. User Rights & Permanent Account Erasure")
                            LegalParagraph("In accordance with Google Play Developer policies, you have the right to permanently erase your account, location pins, and all stored data at any time via Profile Settings > Delete Account, or online at https://zyphuel.netlify.app/privacy.")

                            LegalSectionTitle("4. Privacy Support & Contact")
                            LegalParagraph("For privacy inquiries, data requests, or compliance questions:\n• Email: m.daniyalkhan490@gmail.com\n• Phone / WhatsApp: +92 323 0112464\n• Official Policy: https://zyphuel.netlify.app/privacy")
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
                                "https://zyphuel.netlify.app/terms-of-use"
                            } else {
                                "https://zyphuel.netlify.app/privacy"
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
