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

                            LegalSectionTitle("1. Acceptance & Service Scope")
                            LegalParagraph("By accessing or using Zyphuel, you agree to these Terms. Zyphuel operates an on-demand logistics dispatch network in Lahore, Pakistan connecting customers with certified delivery couriers for Super Petrol, High-Speed Diesel, Pure Mineral Drinking Water, and LPG Gas Cylinders.")

                            LegalSectionTitle("2. Safety & Hazardous Materials (OGRA Compliance)")
                            LegalParagraph("Petroleum and LPG are volatile and flammable materials. In compliance with Oil & Gas Regulatory Authority (OGRA) guidelines:")
                            LegalBulletPoint("Customers must ensure no active flames, burning cigarettes, or active generators are present within 10 meters of fuel dispensing.")
                            LegalBulletPoint("Fuel is delivered exclusively into motor vehicle fuel tanks or approved explosion-proof safety containers.")
                            LegalBulletPoint("Delivery couriers possess the statutory right and duty to refuse dispensing if conditions violate safety standards.")

                            LegalSectionTitle("3. Pricing & Billing")
                            LegalParagraph("Fuel rates strictly adhere to officially notified OGRA retail prices. Transparent delivery and convenience charges are itemized prior to order checkout. Payments may be completed via Cash on Delivery (COD), JazzCash, EasyPaisa, or bank transfer.")

                            LegalSectionTitle("4. Cancellations & Disputes")
                            LegalParagraph("Orders may be cancelled free of charge prior to driver dispatch. Report any quality or calibration concerns via in-app Support within 2 hours of delivery.")

                            LegalSectionTitle("5. Account Deletion")
                            LegalParagraph("You may permanently delete your account and personal data at any time via Profile Settings > Delete Account or online at https://www.zyphuel.com/request-deletion.")
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

                            // Highlighted Location Disclosure
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "📍 Prominent Location Disclosure",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Zyphuel accesses fine and coarse device location to enable doorstep deliveries, identify drop-off locations, and provide live telematic tracking while an order is active. Couriers use Foreground Service Location during deliveries. We do not track continuous background location outside of active delivery fulfillment.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                                    )
                                }
                            }

                            LegalSectionTitle("1. Data Collected")
                            LegalBulletPoint("Account Identifiers: Full Name, Email, Phone Number, Delivery Addresses.")
                            LegalBulletPoint("Rider Credentials: CNIC/ID, Driving License, Vehicle Plate, Emergency Contacts for fleet safety.")
                            LegalBulletPoint("Order Records: Products purchased, invoice summaries, selected payment method (no raw card CVVs stored).")

                            LegalSectionTitle("2. Data Sharing & Protection")
                            LegalParagraph("We never sell user data. Data is securely processed with Google Maps SDK and Firebase services over encrypted HTTPS/TLS. Local sensitive credentials are protected using AES-256 Room database encryption.")

                            LegalSectionTitle("3. User Rights & Account Erasure")
                            LegalParagraph("You have full control to inspect, export, or permanently erase your account data directly in-app or via https://www.zyphuel.com/request-deletion.")
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
                                "https://www.zyphuel.com/terms-and-conditions"
                            } else {
                                "https://www.zyphuel.com/privacy-policy"
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
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun LegalParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
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
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        )
    }
}
