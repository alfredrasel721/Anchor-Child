package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmergencyShare
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
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
import com.example.model.FamilyMessage
import com.example.model.FamilyTask
import com.example.service.DeviceSystemMonitor
import com.example.ui.components.SemanticStatusBadge
import com.example.ui.theme.StatusEnforcedBg
import com.example.ui.theme.StatusEnforcedGreen
import com.example.ui.theme.StatusFailedBg
import com.example.ui.theme.StatusFailedCrimson
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StitchPrimary
import com.example.ui.theme.StitchPrimaryFixed
import com.example.ui.theme.StitchSecondary
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceContainerLow
import com.example.viewmodel.AnchorChildUiState

@Composable
fun C08SosEmergencyCheckInScreen(
    uiState: AnchorChildUiState,
    systemMonitor: DeviceSystemMonitor,
    onStartSosCountdown: () -> Unit,
    onCancelSosCountdown: () -> Unit,
    onDispatchSosImmediately: () -> Unit,
    onRunPracticeSosDrill: () -> Unit,
    onSendQuickCheckIn: (String) -> Unit,
    onRefreshGpsLocation: () -> Unit,
    onOpenFamilyChat: () -> Unit
) {
    val context = LocalContext.current
    val child = uiState.activeChild

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tactile Emergency SOS Beacon Card (Stitch C-08)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.sosGraceCountdownSeconds > 0 || uiState.sosDispatchedActive) {
                        StatusFailedBg
                    } else {
                        StitchSurface
                    }
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = StatusFailedBg,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "C-08 • EMERGENCY SOS & SAFE CHECK-IN",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusFailedCrimson,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (uiState.sosGraceCountdownSeconds > 0) {
                        Text(
                            text = "Sending SOS in ${uiState.sosGraceCountdownSeconds}s...",
                            style = MaterialTheme.typography.headlineLarge,
                            color = StatusFailedCrimson,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Accidental press? Tap Cancel below within the 5-second grace window.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancelSosCountdown,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("c08_cancel_sos_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel Alert")
                            }
                            Button(
                                onClick = onDispatchSosImmediately,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("c08_send_sos_now_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusFailedCrimson),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Send SOS Now")
                            }
                        }
                    } else {
                        // Giant Tactile SOS Circle Button
                        Box(
                            modifier = Modifier
                                .size(132.dp)
                                .clip(CircleShape)
                                .background(StatusFailedCrimson)
                                .clickable { onStartSosCountdown() }
                                .testTag("c08_sos_trigger_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.EmergencyShare,
                                    contentDescription = "Trigger SOS Beacon",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "SOS",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "5s Grace Cancel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Text(
                            text = "Alerts Mom, Dad & Trusted Contacts with your live GPS (${child.locationLabel}) and battery (${child.batteryLevel}%).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRunPracticeSosDrill,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("c08_practice_drill_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Run Practice Drill")
                            }

                            OutlinedButton(
                                onClick = {
                                    val primaryPhone = uiState.guardians.firstOrNull()?.emailOrPhone ?: "+15552348891"
                                    val body = "ANCHOR SOS (${child.name}): I need help at ${child.locationLabel} " +
                                        "(GPS ${child.latitude}, ${child.longitude}) • Battery ${child.batteryLevel}%."
                                    try {
                                        context.startActivity(
                                            systemMonitor.buildCarrierSmsFallbackIntent(primaryPhone, body)
                                        )
                                    } catch (_: Exception) {
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("c08_carrier_sms_fallback_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "SMS Fallback",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SMS Fallback")
                            }
                        }
                    }
                }
            }
        }

        // 1-Tap Safe Zone Check-In Card
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
                                text = "1-Tap Safe Check-In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current Zone: ${child.locationLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = onRefreshGpsLocation,
                            modifier = Modifier.testTag("c08_refresh_gps_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Refresh GPS",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GPS Fix", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = { onSendQuickCheckIn("I'm Safe & Arrived") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c08_checkin_safe_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusEnforcedGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I'm Safe & Arrived at ${child.locationLabel}")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSendQuickCheckIn("Please Pick Me Up When Ready") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c08_checkin_pickup_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Please Pick Me Up")
                        }
                        OutlinedButton(
                            onClick = { onSendQuickCheckIn("Heading Home Now") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c08_checkin_heading_home_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Heading Home")
                        }
                    }
                }
            }
        }

        // Active Family Safe Zones (Geofences)
        item {
            Text(
                text = "Agreed Family Safe Zones (Transparent Geofences)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.geofences) { geo ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = geo.name,
                            tint = if (geo.activeInside) StatusEnforcedGreen else StitchPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = geo.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${geo.radiusMeters}m boundary • ${geo.triggerSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SemanticStatusBadge(status = if (geo.activeInside) "ACTIVE" else "ENFORCED")
                }
            }
        }

        // Trusted Guardians & Direct Call Contacts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Verified Emergency Contacts (Always Unblocked)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onOpenFamilyChat,
                    modifier = Modifier.testTag("c08_goto_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Family Chat")
                }
            }
        }

        items(uiState.guardians) { guardian ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = guardian.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${guardian.relationRole} • ${guardian.emailOrPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    systemMonitor.buildDialGuardianIntent(guardian.emailOrPhone)
                                )
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.testTag("c08_call_${guardian.guardianId}"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call ${guardian.name}",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call")
                    }
                }
            }
        }
    }
}

@Composable
fun C09FamilyChatTasksScreen(
    uiState: AnchorChildUiState,
    onSendMessage: (String) -> Unit,
    onToggleTaskCompleted: (FamilyTask) -> Unit
) {
    var messageDraft by remember { mutableStateOf("") }
    val quickReplies = listOf(
        "Homework finished! 📚",
        "At soccer practice ⚽",
        "Ready for pickup 🚗",
        "Can we review my request?"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Shared Family Tasks & Habit Pacts Section (Stitch C-09)
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
                        Column {
                            Text(
                                text = "Shared Tasks & Screen-Time Rewards",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap a task to mark it ready for parent verification",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SemanticStatusBadge(status = "${uiState.tasks.count { it.completedByChild }}/${uiState.tasks.size} DONE")
                    }

                    uiState.tasks.forEach { task ->
                        Surface(
                            color = if (task.completedByChild) StatusEnforcedBg else StitchSurfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTaskCompleted(task) }
                                .testTag("c09_task_${task.taskId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (task.completedByChild) {
                                            Icons.Default.CheckBox
                                        } else {
                                            Icons.Default.CheckBoxOutlineBlank
                                        },
                                        contentDescription = task.title,
                                        tint = if (task.completedByChild) StatusEnforcedGreen else StitchPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${task.rewardSummary} • Due: ${task.dueSummary}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StitchSecondary
                                        )
                                    }
                                }
                                SemanticStatusBadge(status = task.status)
                            }
                        }
                    }
                }
            }
        }

        // Family Chat Thread Card
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
                        text = "Family Chat & Bilateral Audit Feed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    uiState.messages.forEach { msg ->
                        ChatMessageBubble(message = msg)
                    }

                    // Quick Reply Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickReplies.take(2).forEach { qr ->
                            FilterChip(
                                selected = false,
                                onClick = { onSendMessage(qr) },
                                label = { Text(qr, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageDraft,
                            onValueChange = { messageDraft = it },
                            placeholder = { Text("Message Mom & Dad...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c09_chat_input")
                        )
                        Button(
                            onClick = {
                                if (messageDraft.isNotBlank()) {
                                    onSendMessage(messageDraft)
                                    messageDraft = ""
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("c09_send_chat_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: FamilyMessage) {
    val isChild = message.senderRole == "CHILD"
    val isSystem = message.isSystemEvent || message.senderRole == "SYSTEM"

    val bg = when {
        isSystem -> StatusPendingBg
        isChild -> StitchPrimaryFixed
        else -> StitchSurfaceContainerLow
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = StitchPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message.timestampLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
