package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.ChildScreenId
import com.example.model.LiveSessionModality
import com.example.model.LiveSessionStatus
import com.example.service.DeviceSystemMonitor
import com.example.ui.components.SemanticStatusBadge
import com.example.ui.theme.StatusEnforcedBg
import com.example.ui.theme.StatusEnforcedGreen
import com.example.ui.theme.StatusFailedBg
import com.example.ui.theme.StatusFailedCrimson
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StitchPrimary
import com.example.ui.theme.StitchPrimaryFixed
import com.example.ui.theme.StitchSecondary
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceContainerLow
import com.example.viewmodel.AnchorChildUiState
import java.util.Locale

@Composable
fun C12PremiumFiltersDisclosureScreen(
    uiState: AnchorChildUiState,
    systemMonitor: DeviceSystemMonitor,
    onSubmitWebsiteAppeal: (domainUrl: String, note: String) -> Unit,
    onPreviewLiveConsent: (LiveSessionModality) -> Unit
) {
    val context = LocalContext.current
    val config = uiState.premiumConfig
    var appealDomain by remember { mutableStateOf("") }
    var appealNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Disclosure Card (Stitch C-12)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "• Safety & Filter Disclosures",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Synchronized from Household Plan: ${uiState.household.subscriptionTier}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchSecondary
                            )
                        }
                        SemanticStatusBadge(
                            status = if (uiState.household.subscriptionActive) "ACTIVE" else "FREE"
                        )
                    }
                    Text(
                        text = "Anchor discloses all 6 Premium Safeguard groups transparently on your device. No silent filters or undisclosed monitoring ever run in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Website & Web-Content Filtering Disclosure
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Web Filter",
                            tint = StitchPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Web Filter Profile: ${config.webFilterProfile}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Blocked Categories: ${config.blockedCategories.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusFailedCrimson
                    )
                    Text(
                        text = "Always Whitelisted Domains: ${config.whitelistedDomains.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusEnforcedGreen
                    )
                }
            }
        }

        // 2. Social Media & Chat Content Shield Disclosure
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Social Shield",
                            tint = StitchPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Social Media Shield: ${config.socialShieldProfile}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Discord DMs: ${if (config.discordDmWhitelistOnly)"Friends Whitelist Only"else"Standard"}\n" +
                            "• Roblox Chat: ${if (config.robloxChatFiltered)"Filtered Safe Chat"else"Standard"}\n" +
                            "• WhatsApp Unknown Senders: ${if (config.whatsappUnknownBlocked)"Blocked"else"Allowed"}\n" +
                            "• YouTube Restricted Mode: ${if (config.youtubeRestrictedMode)"Enforced"else"Off"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 3. Notification Focus Shield Disclosure
        item {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsPaused,
                                contentDescription = "Notification Filter",
                                tint = StitchPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. Notification Focus Shield",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SemanticStatusBadge(
                            status = if (config.notificationFilterEnabled) "ENFORCED" else "PAUSED"
                        )
                    }
                    Text(
                        text = "Policy: ${config.notificationProtocol}. Holds non-urgent game/social pings during study & bedtime without sharing notification text with parents.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    systemMonitor.buildNotificationListenerSettingsIntent()
                                )
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c12_notif_listener_settings_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Inspect Android Notification Access")
                    }
                }
            }
        }

        // 4, 5 & 6. Live Screen, Camera & Audio Consent Protocol
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
                    Text(
                        text = "4–6. Live Screen, Camera & Audio Consent Protocol",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Audit Code: ${config.liveSessionAuditCode} • Every live check-in requires your explicit consent and shows an instant STOP button .",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveSessionModality.entries.forEach { mod ->
                            FilterChip(
                                selected = false,
                                onClick = { onPreviewLiveConsent(mod) },
                                label = { Text(mod.badgeText, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("c12_test_live_${mod.name.lowercase}")
                            )
                        }
                    }
                }
            }
        }

        // Website Whitelist Appeal Petition Card
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
                    Text(
                        text = "Petition to Whitelist a School or Project Website",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = appealDomain,
                        onValueChange = { appealDomain = it },
                        label = { Text("Website Domain (e.g. scratch.mit.edu)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c12_appeal_domain_input")
                    )
                    OutlinedTextField(
                        value = appealNote,
                        onValueChange = { appealNote = it },
                        label = { Text("Why do you need this site?") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c12_appeal_note_input")
                    )
                    Button(
                        onClick = {
                            onSubmitWebsiteAppeal(appealDomain, appealNote)
                            appealDomain = ""
                            appealNote = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c12_submit_appeal_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Send Whitelist Appeal to Parent Inbox")
                    }
                }
            }
        }
    }
}

@Composable
fun C13IncomingLiveConsentScreen(
    uiState: AnchorChildUiState,
    onApproveSession: () -> Unit,
    onDeclineSession: (String) -> Unit,
    onSelectModality: (LiveSessionModality) -> Unit
) {
    val session = uiState.liveSessionState

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = StatusPendingBg,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "• EXPLICIT CHILD AUTHORIZATION REQUIRED",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusPendingAmber,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(StitchPrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (session.modality) {
                            LiveSessionModality.SCREEN_SHARE -> Icons.Default.ScreenShare
                            LiveSessionModality.CAMERA_VIEW -> Icons.Default.CameraAlt
                            LiveSessionModality.AUDIO_LISTEN -> Icons.Default.Mic
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = session.modality.label,
                            tint = StitchPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "${session.initiatorName} Requested ${session.modality.label}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    // Modality Switcher for Testing All 3 Supported Live Modalities
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveSessionModality.entries.forEach { mod ->
                            FilterChip(
                                selected = session.modality == mod,
                                onClick = { onSelectModality(mod) },
                                label = { Text(mod.badgeText, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Surface(
                        color = StitchSurfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Parent Reason Note:",
                                style = MaterialTheme.typography.labelMedium,
                                color = StitchPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"${session.reasonNote}\"",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Audit ID: ${session.sessionId} • Max Duration: 10m 00s Auto-Cutoff",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mandatory Privacy & Sovereign Control Guarantees
                    Surface(
                        color = StatusEnforcedBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Your Sovereign Rights During Live Sessions",
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusEnforcedGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Nothing starts unless you tap Approve below.\n" +
                                    "• A persistent red banner stays visible the entire time.\n" +
                                    "• You can tap STOP SESSION IMMEDIATELY at any second.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Equal-Weight Approve & Decline Buttons (Stitch C-13 Spec)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDeclineSession("I'm busy right now, let's message in Family Chat!")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("c13_decline_live_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Decline",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onApproveSession,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("c13_approve_live_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusEnforcedGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Approve",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun C14ActiveLiveSessionScreen(
    uiState: AnchorChildUiState,
    onStopImmediately: () -> Unit,
    onToggleMuteAudio: () -> Unit,
    onReturnToPrivacy: () -> Unit
) {
    val session = uiState.liveSessionState
    val streaming = session.status == LiveSessionStatus.ACTIVE_STREAMING
    val mins = session.remainingSeconds / 60
    val secs = session.remainingSeconds % 60
    val timerFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = StatusFailedBg,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StatusFailedCrimson)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (streaming) "• LIVE ${session.modality.label.uppercase} ACTIVE" else "• CONSENT RECORDED • NOT STREAMING",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusFailedCrimson,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Text(
                        text = timerFormatted,
                        style = MaterialTheme.typography.displayLarge,
                        color = StatusFailedCrimson,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = if (streaming) "Auto-Cutoff Countdown • Connected with ${session.initiatorName}" else "No screen, camera, or microphone media is being captured or transmitted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        color = StitchSurfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (streaming) "Active Hardware Stream Details" else "Awaiting Secure Media Connection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (streaming) {
                                    "• Modality: ${session.modality.label} (${session.modality.badgeText})\n" +
                                        "• Started At: ${session.startedAtLabel}\n" +
                                        "• Microphone Status: ${if (session.audioMutedByChild)"Muted by You"else"Active"}\n" +
                                        "• Bilateral Audit ID: ${session.sessionId}"
                                } else {
                                    "• Consent recorded for ${session.modality.label}.\n" +
                                        "• No verified WebRTC media connection exists.\n" +
                                        "• Your device is not sharing screen, camera, or microphone content.\n" +
                                        "• Audit ID: ${session.sessionId}"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleMuteAudio,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c14_toggle_mute_button"),
                        enabled = streaming,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (session.audioMutedByChild) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle audio mute"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (!streaming) "Microphone control available only during a live stream" else if (session.audioMutedByChild) "Microphone Muted (Tap to Unmute)" else "Mute Microphone Immediately"
                        )
                    }

                    // Giant Sovereign Instant Stop Button (Stitch C-14 Requirement)
                    Button(
                        onClick = onStopImmediately,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("c14_instant_stop_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusFailedCrimson),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Instant Stop",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "STOP SESSION IMMEDIATELY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    OutlinedButton(
                        onClick = onReturnToPrivacy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c14_back_privacy_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Return to Privacy Dashboard")
                    }
                }
            }
        }
    }
}

@Composable
fun C15OfflineSyncHelpScreen(
    uiState: AnchorChildUiState,
    onForceSyncNow: () -> Unit,
    onToggleOfflineSimulation: () -> Unit,
    onNavigateToSos: () -> Unit
) {
    val isOnline = uiState.isEffectiveOnline

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Offline / Cloud Sync Status Card (Stitch C-15)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = "Connection state",
                                tint = if (isOnline) StatusEnforcedGreen else StatusPendingAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "• Offline Sync & Resilience",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${uiState.activeChild.networkType} (${uiState.activeChild.networkName}) • ${uiState.activeChild.lastSyncLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        SemanticStatusBadge(status = if (isOnline) "ENFORCED" else "PENDING")
                    }

                    Surface(
                        color = if (isOnline) StatusEnforcedBg else StatusPendingBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isOnline) {
                                "Real-time Firestore sync is active, and ${uiState.appRules.size} app rules + ${uiState.schedules.size} schedules are cached locally in Room SQLite for airplane/offline resilience."
                            } else {
                                "Device is currently offline. Your existing daily allowance (${uiState.activeChild.dailyAllowanceMinutes}m) and ${uiState.schedules.size} schedules remain enforced from Room local storage. New requests and check-ins are queued below."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onForceSyncNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c15_force_sync_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now")
                        }

                        OutlinedButton(
                            onClick = onToggleOfflineSimulation,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c15_toggle_offline_mode_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (uiState.isOfflineSimulated) "Go Back Online" else "Test Offline Mode")
                        }
                    }
                }
            }
        }

        // Pending Offline Sync Queue (Room SQLite)
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
                        Text(
                            text = "Offline Sync Queue (${uiState.offlineQueue.size} items)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        SemanticStatusBadge(
                            status = if (uiState.offlineQueue.isEmpty()) "SYNCED" else "QUEUED"
                        )
                    }

                    if (uiState.offlineQueue.isEmpty()) {
                        Text(
                            text = "All extra-time requests, check-ins, and telemetry heartbeats are up to date with the Parent App.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.offlineQueue.forEach { item ->
                            Surface(
                                color = StitchSurfaceContainerLow,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.payloadSummary} • ${item.createdAtLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    SemanticStatusBadge(
                                        status = if (item.synced) "ENFORCED" else "PENDING"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Offline Emergency Help Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusFailedBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Need Help While Offline?",
                        style = MaterialTheme.typography.titleSmall,
                        color = StatusFailedCrimson,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Even without Wi-Fi or mobile data, your SOS Emergency Beacon can send a Carrier SMS with your last GPS coordinates or call Mom & Dad directly.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = onNavigateToSos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c15_goto_sos_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusFailedCrimson),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open Emergency SOS & Carrier SMS")
                    }
                }
            }
        }
    }
}
