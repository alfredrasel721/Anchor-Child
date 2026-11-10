package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChildRequest
import com.example.ui.components.SemanticStatusBadge
import com.example.ui.theme.StatusEnforcedBg
import com.example.ui.theme.StatusEnforcedGreen
import com.example.ui.theme.StatusFailedBg
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StitchOutlineVariant
import com.example.ui.theme.StitchPrimary
import com.example.ui.theme.StitchPrimaryFixed
import com.example.ui.theme.StitchSecondary
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceContainerLow
import com.example.viewmodel.AnchorChildUiState

@Composable
fun C06RequestExtraTimeScreen(
    uiState: AnchorChildUiState,
    onSubmitRequest: (
        requestType: String,
        targetName: String,
        targetCategory: String,
        bonusMinutes: Int,
        reasonChip: String,
        customNote: String
    ) -> Unit,
    onOpenDecisionInbox: () -> Unit
) {
    val defaultTarget = uiState.activeRestrictedApp?.appName ?: "Minecraft"
    val defaultCategory = uiState.activeRestrictedApp?.category ?: "Games"

    var requestType by remember { mutableStateOf("EXTRA_TIME") }
    var selectedTarget by remember(defaultTarget) { mutableStateOf(defaultTarget) }
    var selectedCategory by remember(defaultCategory) { mutableStateOf(defaultCategory) }
    var selectedMinutes by remember { mutableIntStateOf(15) }
    var selectedReason by remember { mutableStateOf("Finishing Game Match") }
    var customNote by remember { mutableStateOf("") }

    val durationOptions = listOf(15, 30, 45, 60)
    val reasonOptions = listOf(
        "Homework / Research",
        "Finishing Game Match",
        "Co-op with Friends",
        "Creative Project"
    )

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "• Request Extra Time or App",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Parents typically respond in ~4 minutes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenDecisionInbox,
                            modifier = Modifier.testTag("c06_open_inbox_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Inbox",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inbox (${uiState.requests.size})")
                        }
                    }

                    // Request Type Toggle (Bonus Time vs New App Unlock)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "EXTRA_TIME" to "Bonus Screen Time",
                            "NEW_APP" to "Unlock App / Site"
                        ).forEach { (typeKey, label) ->
                            val selected = requestType == typeKey
                            FilterChip(
                                selected = selected,
                                onClick = { requestType = typeKey },
                                label = { Text(label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("c06_type_$typeKey"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StitchPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Target App Selector Pills
                    Text(
                        text = "1. Which App or Activity?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.appRules.filterNot { it.whitelisted }.take(3).forEach { rule ->
                            val isSelected = selectedTarget == rule.appName
                            Surface(
                                color = if (isSelected) StitchPrimaryFixed else StitchSurfaceContainerLow,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) StitchPrimary else StitchOutlineVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedTarget = rule.appName
                                        selectedCategory = rule.category
                                    }
                                    .testTag("c06_target_${rule.ruleId}")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = rule.appName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = StitchPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${rule.usedMinutesToday}/${rule.dailyLimitMinutes}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Duration Selector (+15m, +30m, +45m, +60m)
                    Text(
                        text = "2. How Much Extra Time?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.forEach { mins ->
                            val selected = selectedMinutes == mins
                            Surface(
                                color = if (selected) StitchPrimary else StitchSurfaceContainerLow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMinutes = mins }
                                    .testTag("c06_duration_$mins")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (mins == 60) "+1 hr" else "+${mins}m",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (selected) Color.White else StitchPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // Quick Reason Chips
                    Text(
                        text = "3. Reason for Request",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        reasonOptions.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { reason ->
                                    val selected = selectedReason == reason
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedReason = reason },
                                        label = { Text(reason, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StitchPrimaryFixed,
                                            selectedLabelColor = StitchPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        label = { Text("Add a polite note for Mom & Dad (optional)") },
                        placeholder = { Text("e.g., Wrapping up our science bridge build before dinner!") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c06_note_input")
                    )

                    Button(
                        onClick = {
                            onSubmitRequest(
                                requestType,
                                selectedTarget,
                                selectedCategory,
                                selectedMinutes,
                                selectedReason,
                                customNote
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("c06_submit_request_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send +${selectedMinutes}m Request for $selectedTarget",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun C07ParentDecisionInboxScreen(
    uiState: AnchorChildUiState,
    onNewRequest: () -> Unit,
    onOpenFamilyChat: () -> Unit
) {
    val approvedCount = uiState.requests.count { it.status == "APPROVED" }
    val pendingCount = uiState.requests.count { it.status == "PENDING" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Weekly Agreement & Response Summary Card (Stitch C-07)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(18.dp),
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
                                text = "• Parent Decision Inbox",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$approvedCount Approved • $pendingCount Waiting for Parent Review",
                                style = MaterialTheme.typography.bodySmall,
                                color = StitchSecondary
                            )
                        }
                        Button(
                            onClick = onNewRequest,
                            modifier = Modifier.testTag("c07_new_request_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Request")
                        }
                    }

                    Surface(
                        color = StatusEnforcedBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Trust Score",
                                tint = StatusEnforcedGreen
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Weekly Family Agreement Score: ${uiState.wellbeingTrustPoints}% • Parents include a reason when approving or declining.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        items(uiState.requests) { req ->
            DecisionInboxCard(
                request = req,
                onDiscussInChat = onOpenFamilyChat
            )
        }
    }
}

@Composable
private fun DecisionInboxCard(
    request: ChildRequest,
    onDiscussInChat: () -> Unit
) {
    val cardTone = when (request.status.uppercase()) {
        "APPROVED" -> StatusEnforcedBg
        "DECLINED" -> StatusFailedBg
        else -> StatusPendingBg
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("c07_request_card_${request.requestId}")
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
                        text = "${request.targetName} (+${request.requestedBonusMinutes}m)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${request.targetCategory} • ${request.usedTodaySummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SemanticStatusBadge(status = request.status)
            }

            if (request.childNote.isNotBlank()) {
                Text(
                    text = "Your note: \"${request.childNote}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                color = cardTone,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Decision details",
                            tint = StitchPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (request.decidedBy.isNotBlank()) {
                                "Decision by ${request.decidedBy}"
                            } else {
                                "Parent Review Status"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = request.decisionSummary.ifBlank { "Waiting for Mom or Dad to review." },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            OutlinedButton(
                onClick = onDiscussInChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("c07_discuss_${request.requestId}"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Discuss in Family Chat")
            }
        }
    }
}
