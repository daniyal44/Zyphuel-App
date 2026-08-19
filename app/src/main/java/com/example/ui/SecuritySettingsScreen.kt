package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.security.AppModule
import com.example.security.BiometricSecurityManager
import com.example.security.SecureStorageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: MainViewModel,
    module: AppModule,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val currentUser by viewModel.currentUser.collectAsState()

    val isCustomerBioEnabled by viewModel.isCustomerBioEnabled.collectAsState()
    val isRiderBioEnabled by viewModel.isRiderBioEnabled.collectAsState()
    val isAdminBioEnabled by viewModel.isAdminBioEnabled.collectAsState()

    val isBioEnabled = when (module) {
        AppModule.CUSTOMER -> isCustomerBioEnabled
        AppModule.RIDER -> isRiderBioEnabled
        AppModule.ADMIN -> isAdminBioEnabled
    }

    var showPromptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshSecurityAndBiometricStates(context)
    }

    val primaryColor = when (module) {
        AppModule.CUSTOMER -> Color(0xFF0284C7)
        AppModule.RIDER -> Color(0xFF16A34A)
        AppModule.ADMIN -> Color(0xFF7C3AED)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security & Biometrics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("security_settings_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Biometric Status Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Fingerprint Authentication",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isBioEnabled) "Status: Enabled ✔️" else "Status: Disabled 🔒",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isBioEnabled) Color(0xFF059669) else Color.Red,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // Strictly ONLY Enable Biometrics and Disable Biometrics Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Biometric Options",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    // Enable Biometric Option Button
                    Button(
                        onClick = {
                            if (currentUser == null) {
                                Toast.makeText(context, "Please log in first to enable biometrics.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (activity != null) {
                                BiometricSecurityManager.showBiometricPrompt(
                                    activity = activity,
                                    title = "Enable Biometric Login",
                                    subtitle = "Scan your fingerprint on your Android device",
                                    description = "Account: ${currentUser?.email}",
                                    onSuccess = {
                                        viewModel.enableBiometricForModule(context, module, currentUser!!)
                                        Toast.makeText(context, "Biometric authentication enabled successfully! 👆", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { code, err ->
                                        Toast.makeText(context, err.toString(), Toast.LENGTH_LONG).show()
                                        if (code == androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                                            BiometricSecurityManager.openBiometricEnrollmentSettings(context)
                                        }
                                    },
                                    onFailed = {
                                        Toast.makeText(context, "Fingerprint not recognized. Try scanning again.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        enabled = !isBioEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("enable_biometric_action_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            disabledContainerColor = Color.LightGray.copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enable Biometric",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        )
                    }

                    // Disable Biometric Option Button
                    Button(
                        onClick = {
                            viewModel.disableBiometricForModule(context, module)
                            Toast.makeText(context, "Biometric authentication disabled.", Toast.LENGTH_SHORT).show()
                        },
                        enabled = isBioEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("disable_biometric_action_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            disabledContainerColor = Color.LightGray.copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Disable Biometric",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        )
                    }
                }
            }
        }
    }
}
