package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig

private val PrimaryBlue = Color(0xFF0284C7)
private val PrimaryDark = Color(0xFF0F172A)
private val AccentEmerald = Color(0xFF10B981)

/**
 * Modern, High-Contrast In-App Legal & Compliance Dialog.
 * Beautifully formats Terms of Service, Privacy Policy, and Operational Disclosures.
 * Fully compliant with OGRA safety standards, Civil Defence Pakistan, and Google Play Store policies.
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
                .fillMaxHeight(0.90f)
                .testTag("terms_privacy_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gavel,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Legal & Compliance",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Zyphuel v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelSmall.copy(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .testTag("close_terms_btn")
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Segmented Pill Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LegalPillTab(
                        title = "Terms",
                        icon = Icons.Filled.Description,
                        selected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    LegalPillTab(
                        title = "Privacy",
                        icon = Icons.Filled.Security,
                        selected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                    LegalPillTab(
                        title = "How It Works",
                        icon = Icons.Filled.Bolt,
                        selected = selectedTab == 2,
                        modifier = Modifier.weight(1.15f),
                        onClick = { selectedTab = 2 }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Legal Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Web Version", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = PrimaryBlue))
                    }

                    Button(
                        onClick = {
                            onAccept?.invoke()
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Understood & Agreed", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
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
        targetValue = if (selected) PrimaryBlue else Color.Transparent,
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pill_content"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
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
        accent = PrimaryBlue,
        icon = Icons.Filled.Verified
    )

    LegalCard(
        icon = Icons.Filled.LocationOn,
        title = "1. Operational Coverage & Service Scope",
        description = "Zyphuel operates an on-demand doorstep energy, fuel, and mineral water delivery ecosystem across Lahore, Punjab. Service sectors include Gulberg, DHA (Phases 1-9), Model Town, Johar Town, Bahria Town, Green Town, Lahore Cantt, and Faisal Town."
    )

    LegalCard(
        icon = Icons.Filled.LocalGasStation,
        title = "2. Strict Safety & Petroleum Compliance (OGRA)",
        description = "Petroleum products and LPG are hazardous flammable fuels. Deliveries comply strictly with statutory petroleum safety regulations:\n• A mandatory 10-meter perimeter free of open flames, active smoking, and generators must be maintained during fuel dispensing.\n• Dispensing occurs exclusively into vehicle tanks or certified explosion-proof containers.\n• Certified digital flow-meters and tamper-evident security seals are installed on all bowsers.\n• Delivery personnel hold statutory authority to halt dispensing if site conditions present an ignition risk."
    )

    LegalCard(
        icon = Icons.Filled.Payments,
        title = "3. Regulated Pricing & Cash on Delivery (COD)",
        description = "Fuel rates strictly track official notified retail prices set by the Oil & Gas Regulatory Authority (OGRA). A transparent petroleum retail adjustment of Rs. 2.50/L applies at checkout. Standard delivery fees are Rs. 250 flat for fuel/gas and Rs. 50 for pure drinking water. All transactions are settled via 100% Cash on Delivery upon direct doorstep verification."
    )

    LegalCard(
        icon = Icons.Filled.Cancel,
        title = "4. Order Cancellation & Customer Rights",
        description = "Orders can be cancelled free of charge prior to driver dispatch. Immediate quality or volumetric inquiries are resolved 24/7 via in-app support within 2 hours of delivery."
    )

    LegalCard(
        icon = Icons.Filled.DeleteForever,
        title = "5. Permanent Account Deletion",
        description = "In full compliance with Google Play Developer Policies, customers retain the right to permanently delete their account, GPS history, and personal data at any time via Profile Settings > Delete Account."
    )
}

@Composable
private fun PrivacyContent() {
    LegalHighlightCard(
        title = "Google Play Store Data Safety & Privacy Guarantee",
        subtitle = "Encrypted in Transit (TLS 1.3) & at Rest (AES-256)",
        accent = AccentEmerald,
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
        title = "Seamless Doorstep Delivery in 4 Simple Steps",
        subtitle = "Verified Logistics Fleet • Zero Wait Time",
        accent = Color(0xFF8B5CF6),
        icon = Icons.Filled.ElectricBolt
    )

    StepCard(
        stepNumber = "1",
        title = "Choose Product & Volume",
        description = "Select Super Euro-V Petrol, High-Octane 97, High-Speed Diesel, 11.8kg LPG Cylinder, or Pure RO Drinking Water. Adjust volume with precision 1-liter steppers or full-tank presets."
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
        description = "Our rider dispenses certified fuel into your vehicle tank. Pay via Cash on Delivery and instantly receive your official PDF tax invoice."
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
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
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
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
