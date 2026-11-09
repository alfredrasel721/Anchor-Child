package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.ChildScreenId
import com.example.service.DeviceSystemMonitor
import com.example.ui.components.SemanticStatusBadge
import com.example.ui.theme.StatusEnforcedBg
import com.example.ui.theme.StatusEnforcedGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StitchOutlineVariant
import com.example.ui.theme.StitchPrimary
import com.example.ui.theme.StitchPrimaryContainer
import com.example.ui.theme.StitchPrimaryFixed
import com.example.ui.theme.StitchSecondary
import com.example.ui.theme.StitchSecondaryContainer
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceContainerLow
import com.example.viewmodel.AnchorChildUiState

@Composable
fun C01WelcomePairingScreen(
    uiState: AnchorChildUiState,
    onUpdatePairingCode: (String) -> Unit,
    onSelectChild: (String) -> Unit,
    onSubmitPairing: () -> Unit,
    onSignInWithGoogle: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(StitchPrimary, StitchPrimaryContainer)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = StitchSecondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "C-01 • CHILD COMPANION ONBOARDING",
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Welcome to Anchor Child",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "A clear, fair family agreement for screen time, safe check-ins, and mutual trust—with full transparency into what is shared and what stays strictly private.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Step 1: Google Cloud Account Link (Shared Firebase Project with Parent App)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Cloud Sync",
                                tint = StitchPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Step 1 • Connect Family Cloud",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SemanticStatusBadge(
                            status = when {
                                uiState.isPaired -> "PAIRED"
                                uiState.currentUser != null -> "SIGNED IN"
                                else -> "PENDING"
                            }
                        )
                    }

                    Text(
                        text = if (uiState.currentUser != null) {
                            if (uiState.isPaired) {
                                "Signed in as ${uiState.currentUser.displayName ?: uiState.currentUser.email}. This device is linked to the verified family household."
                            } else {
                                "Signed in as ${uiState.currentUser.displayName ?: uiState.currentUser.email}. Enter the one-time code from Anchor Parent to link this device."
                            }
                        } else {
                            "Sign in with Google to securely link this device to the Anchor Parent App."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.currentUser == null) {
                        Button(
                            onClick = onSignInWithGoogle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("c01_google_sign_in_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sign in with Google")
                        }
                    }
                }
            }
        }

        // Step 2: Select Child Profile & Enter 6-Digit Pairing Code
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhonelinkSetup,
                            contentDescription = "Pairing Code",
                            tint = StitchPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Step 2 • Enter 6-Digit Pairing Code",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Generated in Anchor Parent App -> Add Child Device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Child profile selector pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.children.forEach { child ->
                            val selected = child.childId == uiState.selectedChildId
                            Surface(
                                color = if (selected) StitchPrimaryFixed else StitchSurfaceContainerLow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) StitchPrimary else StitchOutlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectChild(child.childId) }
                                    .testTag("select_child_${child.childId}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${child.name} (Age ${child.age})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimary
                                    )
                                    Text(
                                        text = child.deviceName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 6-Digit Visual Code Boxes (Stitch C-01 Design)
                    val padded = uiState.pairingCodeInput.padEnd(6, '•')
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0 until 6) {
                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StitchSurfaceContainerLow)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (i < uiState.pairingCodeInput.length) StitchPrimary else StitchOutlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = padded[i].toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = StitchPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.pairingCodeInput,
                        onValueChange = onUpdatePairingCode,
                        label = { Text("6-Digit Parent Pairing Code (e.g. 482915)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pairing_code_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSubmitPairing,
                            enabled = uiState.currentUser != null &&
                                uiState.isEffectiveOnline &&
                                uiState.pairingCodeInput.filter { it.isDigit() }.length == 6,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("verify_pairing_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Link Device & Continue")
                        }

                        OutlinedButton(
                            onClick = { onUpdatePairingCode("") },
                            modifier = Modifier.testTag("clear_pairing_code_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }

        // Transparency & Mutual Respect Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusEnforcedBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy Guarantee",
                            tint = StatusEnforcedGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Anchor's Child Privacy Guarantee",
                            style = MaterialTheme.typography.titleSmall,
                            color = StatusEnforcedGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Never secretly records your screen, camera, or microphone.\n" +
                            "• Never reads your private text messages, photos, or passwords.\n" +
                            "• Any live parent check-in requires your explicit approval and shows a giant red STOP button.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.isPaired) {
                        OutlinedButton(
                            onClick = onNavigateToPermissions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goto_c02_permissions_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Review Android Permissions (C-02)")
                        }
                    } else {
                        Text(
                            text = "Complete server-verified pairing before reviewing device permissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun C02PermissionEducationScreen(
    uiState: AnchorChildUiState,
    systemMonitor: DeviceSystemMonitor,
    onRefreshPermissions: () -> Unit,
    onContinueToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val perms = uiState.permissionSnapshot

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshPermissions()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "C-02 • Permission Education & Status",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Why each Android permission is needed & what it never accesses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onRefreshPermissions,
                            modifier = Modifier.testTag("refresh_permissions_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh status",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verify", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (perms.coreGrantedCount / 5f).coerceIn(0.2f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = StitchSecondary,
                        trackColor = StitchSurfaceContainerLow
                    )

                    Text(
                        text = "${perms.coreGrantedCount} of 5 Core Device Safeguards Verified on ${uiState.activeChild.deviceModel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = StitchSecondary
                    )
                }
            }
        }

        // 1. Usage Stats Access
        item {
            PermissionEducationCard(
                icon = Icons.Default.QueryStats,
                title = "1. Screen Time & App Usage Access",
                androidApiName = "android.permission.PACKAGE_USAGE_STATS",
                whyItMatters = "Measures your daily screen-time allowance and shows your category balance scorecard.",
                privacyBoundary = "Only counts minutes per app category. Never sees what videos you watch or what you type.",
                isGranted = perms.usageStatsGranted,
                buttonLabel = if (perms.usageStatsGranted) "Verified in Android Settings" else "Open Usage Access Settings",
                testTag = "perm_usage_stats_button",
                onAction = {
                    try {
                        context.startActivity(systemMonitor.buildUsageAccessSettingsIntent())
                    } catch (_: Exception) {
                    }
                    onRefreshPermissions()
                }
            )
        }

        // 2. Safe-Zone Location & SOS
        item {
            PermissionEducationCard(
                icon = Icons.Default.LocationOn,
                title = "2. Safe-Zone Check-In & SOS Location",
                androidApiName = "ACCESS_FINE_LOCATION / FusedLocationProvider",
                whyItMatters = "Powers your 1-tap 'I'm Safe & Arrived' button, school arrival alerts, and emergency SOS beacon.",
                privacyBoundary = "Shared only with verified family guardians (Mom & Dad). Never sold or shared with ad networks.",
                isGranted = perms.locationGranted,
                buttonLabel = if (perms.locationGranted) "GPS Location Granted" else "Grant Location Permission",
                testTag = "perm_location_button",
                onAction = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }

        // 3. Notifications (5-Minute Fair Play Warnings & Parent Replies)
        item {
            PermissionEducationCard(
                icon = Icons.Default.NotificationsActive,
                title = "3. Fair-Play Warnings & Parent Decisions",
                androidApiName = "android.permission.POST_NOTIFICATIONS",
                whyItMatters = "Gives you a 5-minute heads-up before an app limit ends so you can save your game or homework.",
                privacyBoundary = "Used strictly for Anchor schedule alerts, parent replies, and mandatory transparency banners.",
                isGranted = perms.notificationsGranted,
                buttonLabel = if (perms.notificationsGranted) "Notifications Enabled" else "Allow Fair-Play Alerts",
                testTag = "perm_notifications_button",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onRefreshPermissions()
                    }
                }
            )
        }

        // 4. App Limit Companion (AccessibilityService)
        item {
            PermissionEducationCard(
                icon = Icons.Default.AccessibilityNew,
                title = "4. Friendly App Limit Companion",
                androidApiName = "AnchorAccessibilityService (Window State Only)",
                whyItMatters = "Detects when a paused or bedtime-restricted app opens so Anchor can show the Next Unlock Window (C-05).",
                privacyBoundary = "canRetrieveWindowContent is FALSE. Anchor cannot read screen text, chats, or passwords.",
                isGranted = perms.accessibilityGranted,
                buttonLabel = if (perms.accessibilityGranted) "Accessibility Companion Active" else "Configure Accessibility Service",
                testTag = "perm_accessibility_button",
                onAction = {
                    try {
                        context.startActivity(systemMonitor.buildAccessibilitySettingsIntent())
                    } catch (_: Exception) {
                    }
                    onRefreshPermissions()
                }
            )
        }

        // 5. Device Admin Tamper Protection
        item {
            PermissionEducationCard(
                icon = Icons.Default.AdminPanelSettings,
                title = "5. Agreement Integrity & Bedtime Lock",
                androidApiName = "AnchorDeviceAdminReceiver (DevicePolicyManager)",
                whyItMatters = "Keeps your family pairing intact and supports scheduled bedtime wind-down lockouts.",
                privacyBoundary = "Never wipes personal files or locks your device outside agreed schedules.",
                isGranted = perms.deviceAdminGranted,
                buttonLabel = if (perms.deviceAdminGranted) "Device Admin Active" else "Activate Device Admin",
                testTag = "perm_device_admin_button",
                onAction = {
                    try {
                        context.startActivity(systemMonitor.buildDeviceAdminActivationIntent())
                    } catch (_: Exception) {
                    }
                    onRefreshPermissions()
                }
            )
        }

        // Continue Button to Today's Screen Time (C-03)
        item {
            Button(
                onClick = onContinueToDashboard,
                enabled = uiState.isPaired,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("c02_continue_to_c03_button"),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue to Today's Screen Time (C-03)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionEducationCard(
    icon: ImageVector,
    title: String,
    androidApiName: String,
    whyItMatters: String,
    privacyBoundary: String,
    isGranted: Boolean,
    buttonLabel: String,
    testTag: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isGranted) StatusEnforcedBg else StitchPrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isGranted) StatusEnforcedGreen else StitchPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = androidApiName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SemanticStatusBadge(status = if (isGranted) "ENFORCED" else "PENDING")
            }

            Text(
                text = whyItMatters,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                color = StitchSurfaceContainerLow,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Privacy boundary",
                        tint = StitchSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = privacyBoundary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(buttonLabel)
            }
        }
    }
}
