package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// Zyphuel Official Brand & High-Contrast Typography Palette
private val ZyphuelBluePrimary = Color(0xFF0062FF)     // Electric Cobalt Blue
private val ZyphuelBlueDark = Color(0xFF003087)        // Deep Royal Navy
private val ZyphuelBlueLight = Color(0xFFEFF6FF)       // Light Blue Accent Tint
private val ZyphuelCardBorder = Color(0xFFE2E8F0)      // Crisp Card Border
private val TextPrimary = Color(0xFF0F172A)            // Deep Charcoal for Headings (High Contrast)
private val TextSecondary = Color(0xFF334155)          // Clear Slate for Paragraphs (High Contrast)
private val AccentGreen = Color(0xFF10B981)            // Emerald Green for Safe/Verified
private val AccentAmber = Color(0xFFD97706)            // Amber for Status Warning

/**
 * High-Contrast, Brand-Aligned In-App Legal & Compliance Dialog.
 * Formats Terms of Service, Privacy Policy, and How It Works with maximum readability.
 * Strictly complies with OGRA petroleum safety standards and Google Play Store policies.
 */
@Composable
fun TermsAndPrivacyDialog(
    initialTab: Int = 0, // 0 for Terms, 1 for Privacy, 2 for How It Works
    onDismissRequest: () -> Unit,
    onAccept: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val scrollState = rememberScrollState()

    // Reset scroll position when switching tabs
    LaunchedEffect(selectedTab) {
        scrollState.scrollTo(0)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("terms_privacy_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
            border = BorderStroke(1.5.dp, ZyphuelCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ZyphuelBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gavel,
                                contentDescription = null,
                                tint = ZyphuelBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Terms & Privacy Policy",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Zyphuel App v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ZyphuelBluePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .testTag("close_terms_btn")
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Segmented Pill Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LegalPillTab(
                        title = "Terms of Service",
                        icon = Icons.Filled.Description,
                        selected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    LegalPillTab(
                        title = "Privacy Policy",
                        icon = Icons.Filled.Security,
                        selected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                    LegalPillTab(
                        title = "How It Works",
                        icon = Icons.Filled.Bolt,
                        selected = selectedTab == 2,
                        modifier = Modifier.weight(1.05f),
                        onClick = { selectedTab = 2 }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Legal Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, ZyphuelCardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> TermsContent()
                            1 -> PrivacyContent()
                            2 -> HowItWorksContent()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val url = if (selectedTab == 1) "https://www.zyphuel.com/privacy" else "https://www.zyphuel.com/terms-of-use"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, ZyphuelBluePrimary.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = ZyphuelBluePrimary
                        )
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = ZyphuelBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Web Version", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZyphuelBluePrimary))
                    }

                    Button(
                        onClick = {
                            onAccept?.invoke()
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZyphuelBluePrimary)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I Understand & Agree", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalPillTab(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) ZyphuelBluePrimary else Color.Transparent,
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        label = "pill_content"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 11.5.sp
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TermsContent() {
    // Verified Compliance Card
    LegalHighlightCard(
        title = "Official Petroleum & Fuel Delivery Standard",
        subtitle = "Governed under OGRA & Civil Defence Laws of Pakistan",
        accent = ZyphuelBluePrimary,
        icon = Icons.Filled.Verified
    )

    LegalCard(
        icon = Icons.Filled.LocationOn,
        title = "1. Operational Coverage & Service Scope",
        description = "Zyphuel operates an on-demand doorstep petroleum delivery ecosystem across Lahore, Punjab. Active operational sectors include Gulberg, DHA (Phases 1-9), Model Town, Johar Town, Bahria Town, Green Town, Lahore Cantt, and Faisal Town."
    )

    LegalCard(
        icon = Icons.Filled.LocalGasStation,
        title = "2. Available Fuel Products & Unavailability Notice",
        description = "Zyphuel provides doorstep delivery for Super Euro-V Petrol, High-Octane 97, and High-Speed Diesel (Regular & Generator).\n\n⚠️ Operational Notice: Water Delivery and LPG Gas Cylinders are currently TEMPORARILY UNAVAILABLE across all sectors. Orders for these two items cannot be placed at this time."
    )

    LegalCard(
        icon = Icons.Filled.Shield,
        title = "3. Strict Safety & Petroleum Compliance (OGRA)",
        description = "Petroleum products are hazardous flammable fuels. Deliveries comply strictly with statutory petroleum safety regulations:\n• A mandatory 10-meter perimeter free of open flames, active smoking, and generators must be maintained during fuel dispensing.\n• Dispensing occurs exclusively into vehicle tanks or certified explosion-proof containers.\n• Certified digital flow-meters and tamper-evident security seals are installed on all delivery bowsers.\n• Delivery personnel hold statutory authority to halt dispensing if site conditions present an ignition risk."
    )

    LegalCard(
        icon = Icons.Filled.Payments,
        title = "4. Transparent Pricing & Cash on Delivery (COD)",
        description = "All fuel rates strictly track notified retail pump prices. A flat delivery fee of Rs. 250 applies per dispatch. All transactions are settled via 100% Cash on Delivery (COD) upon direct doorstep verification. No hidden charges apply."
    )

    LegalCard(
        icon = Icons.Filled.Cancel,
        title = "5. Order Cancellation & Customer Rights",
        description = "Orders can be cancelled free of charge prior to driver dispatch. Immediate quality or volumetric inquiries are resolved 24/7 via in-app support within 2 hours of delivery."
    )

    LegalCard(
        icon = Icons.Filled.DeleteForever,
        title = "6. Permanent Account Deletion",
        description = "In full compliance with Google Play Developer Policies, customers retain the statutory right to permanently delete their account, order history, and personal data at any time via Profile Settings > Delete Account."
    )
}

@Composable
private fun PrivacyContent() {
    LegalHighlightCard(
        title = "Google Play Store Data Safety & Privacy Guarantee",
        subtitle = "Encrypted in Transit (TLS 1.3) & at Rest (AES-256)",
        accent = AccentGreen,
        icon = Icons.Filled.Shield
    )

    LegalCard(
        icon = Icons.Filled.MyLocation,
        title = "1. Location Telematics (FOREGROUND_SERVICE_LOCATION)",
        description = "Your precise GPS coordinates are collected strictly while the application is active or tracking an active order. Location coordinates are used solely to guide the delivery bowser to your vehicle doorstep and calculate ETA. Location data is never sold, shared with advertising brokers, or tracked in the background without user consent."
    )

    LegalCard(
        icon = Icons.Filled.Fingerprint,
        title = "2. Hardware Biometric Authentication",
        description = "Fingerprint and Face ID scans are processed 100% locally on your device via the AndroidX BiometricPrompt hardware enclave (TEE). Zyphuel servers never receive, store, or transmit your raw biometric data."
    )

    LegalCard(
        icon = Icons.Filled.Lock,
        title = "3. Data Encryption & Storage Standards",
        description = "All user credentials, session tokens, and communication pass through TLS 1.3 encrypted sockets. Local storage is fortified with Android Keystore AES-256 GCM encryption. Cloud database endpoints require cryptographically authenticated Firebase Auth tokens with role isolation."
    )

    LegalCard(
        icon = Icons.Filled.Email,
        title = "4. Invoicing & Email Privacy",
        description = "Your registered email address is utilized solely for official transactional order invoices, delivery updates, and password resets via Google SMTP relay. We maintain a zero-spam policy."
    )

    LegalCard(
        icon = Icons.Filled.ContactSupport,
        title = "5. Privacy Officer & Legal Contact",
        description = "For privacy inquiries or compliance notices:\n• Support: m.daniyalkhan490@gmail.com\n• Phone / WhatsApp: +92 323 0112464\n• Corporate Office: 75-Main Boulevard, Gulberg III, Lahore, Pakistan"
    )
}

@Composable
private fun HowItWorksContent() {
    LegalHighlightCard(
        title = "Doorstep Fueling in 4 Simple Steps",
        subtitle = "Verified Logistics Fleet • Zero Wait Time",
        accent = ZyphuelBluePrimary,
        icon = Icons.Filled.ElectricBolt
    )

    StepCard(
        stepNumber = "1",
        title = "Select Fuel Product & Quantity",
        description = "Choose Super Euro-V Petrol, High-Octane 97, or High-Speed Diesel. Adjust volume with precision 1-liter steppers or full-tank presets. (Water and Gas are currently unavailable)."
    )

    StepCard(
        stepNumber = "2",
        title = "Pin Doorstep Delivery Location",
        description = "Confirm your exact vehicle location on the interactive Lahore map or share your live GPS pin."
    )

    StepCard(
        stepNumber = "3",
        title = "Real-Time Rider Live Tracking",
        description = "Watch your certified delivery bowser navigate towards you on the live radar map with dynamic ETA updates."
    )

    StepCard(
        stepNumber = "4",
        title = "Direct Fueling & Instant Invoice",
        description = "Our certified rider dispenses fuel directly into your vehicle tank. Pay via Cash on Delivery and instantly receive your official digital receipt."
    )
}

@Composable
private fun LegalHighlightCard(
    title: String,
    subtitle: String,
    accent: Color,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.35f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun LegalCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ZyphuelCardBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ZyphuelBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ZyphuelBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = TextPrimary
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 19.sp,
                    fontSize = 12.5.sp,
                    color = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: String,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ZyphuelCardBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ZyphuelBluePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp,
                        fontSize = 12.5.sp,
                        color = TextSecondary
                    )
                )
            }
        }
    }
}
