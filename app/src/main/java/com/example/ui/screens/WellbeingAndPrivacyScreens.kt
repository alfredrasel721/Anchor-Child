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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChildScreenId
import com.example.model.LiveSessionModality
import com.example.ui.components.SemanticStatusBadge
import com.example.ui.theme.StatusEnforcedBg
import com.example.ui.theme.StatusEnforcedGreen
import com.example.ui.theme.StitchPrimary
import com.example.ui.theme.StitchPrimaryFixed
import com.example.ui.theme.StitchSecondary
import com.example.ui.theme.StitchSecondaryContainer
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceContainerLow
import com.example.ui.theme.StitchTertiary
import com.example.ui.theme.StitchTertiaryFixed
import com.example.viewmodel.AnchorChildUiState

@Composable
fun C10MyActivityWellbeingScreen(
    uiState: AnchorChildUiState,
    onNavigate: (ChildScreenId) -> Unit
) {
    val weeklyDays = listOf(
        "Mon" to 0.72f,
        "Tue" to 0.65f,
        "Wed" to 0.80f,
        "Thu" to 0.58f,
        "Fri" to 0.85f,
        "Sat" to 0.90f,
        "Today" to 0.53f
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Scorecard & Streak Hero Card (Stitch C-10)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = StitchSecondaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "C-10 • PERSONAL DIGITAL WELLBEING",
                                style = MaterialTheme.typography.labelSmall,
                                color = StitchSecondary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        SemanticStatusBadge(status = "STREAK ACTIVE")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = StitchPrimaryFixed,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "${uiState.wellbeingTrustPoints}/100",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = StitchPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Digital Balance Score",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StitchPrimary
                                )
                            }
                        }

                        Surface(
                            color = StitchTertiaryFixed,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "${uiState.bedtimeStreakDays} Nights",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = StitchTertiary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "On-Time Bedtime Streak",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StitchTertiary
                                )
                            }
                        }
                    }

                    Text(
                        text = "You've balanced 55m of Education & Creative apps with your Game time today. Keep your 5-night bedtime streak tonight to unlock +30m Friday Movie & Game Night!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 7-Day Screen Rhythm Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "7-Day Screen Rhythm (Within Daily Goal)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyDays.forEach { (day, ratio) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height((85 * ratio).dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            if (day == "Today") StitchSecondary else StitchPrimary
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Digital Superpowers Badges
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Superpowers",
                            tint = StitchTertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlocked Digital Superpowers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    SuperpowerRow(
                        title = "Bilingual Explorer",
                        subtitle = "7 days in a row of Duolingo Spanish before Free Play"
                    )
                    SuperpowerRow(
                        title = "Fair-Play Finisher",
                        subtitle = "Saved & closed games before the 5-minute warning expired 4x this week"
                    )
                    SuperpowerRow(
                        title = "Safe-Zone Communicator",
                        subtitle = "Sent timely school arrival check-ins to Mom & Dad"
                    )

                    OutlinedButton(
                        onClick = { onNavigate(ChildScreenId.C09_FAMILY_CHAT_TASKS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c10_open_tasks_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("View Shared Tasks & Rewards (C-09)")
                    }
                }
            }
        }
    }
}

@Composable
private fun SuperpowerRow(title: String, subtitle: String) {
    Surface(
        color = StitchSurfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = StitchPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun C11PrivacySharingDashboardScreen(
    uiState: AnchorChildUiState,
    onNavigate: (ChildScreenId) -> Unit,
    onTriggerLiveConsentPreview: (LiveSessionModality) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bilateral Transparency Header Card (Stitch C-11)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = "Privacy Matrix",
                                tint = StitchPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "C-11 • Privacy & Sharing Matrix",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SemanticStatusBadge(status = uiState.household.subscriptionTier)
                    }

                    Text(
                        text = "Every rule, permission, and safety check-in on Anchor is 100% visible to you. There are zero hidden trackers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Column 1: What Parents CAN See
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
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Shared with Parents",
                            tint = StitchPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shared with Mom & Dad (Guardian View)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Total daily screen-time minutes & app category totals\n" +
                            "• Safe-zone arrival/departure alerts & 1-tap check-ins\n" +
                            "• Device battery level (${uiState.activeChild.batteryLevel}%) & sync health\n" +
                            "• Extra-time requests, SOS beacons & Family Chat messages",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Column 2: What Remains STRICTLY PRIVATE
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
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Strictly Private",
                            tint = StatusEnforcedGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Strictly Private to You (Never Shared)",
                            style = MaterialTheme.typography.titleMedium,
                            color = StatusEnforcedGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Private text messages, SMS, and friend group chats\n" +
                            "• Personal photos, camera roll, and voice memos\n" +
                            "• Individual web search queries or private browsing history\n" +
                            "• Passwords, keystrokes, or silent screen/audio recordings",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Quick Navigation to Related Privacy & Premium Screens (C-02, C-12, C-13, C-14, C-15)
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
                        text = "Inspect Safeguards, Consent & Offline Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(ChildScreenId.C12_PREMIUM_FILTERS_DISCLOSURE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c11_open_c12_filters_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Filter Rules (C-12)")
                        }
                        OutlinedButton(
                            onClick = { onNavigate(ChildScreenId.C02_PERMISSION_EDUCATION) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c11_open_c02_permissions_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Permissions (C-02)")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onTriggerLiveConsentPreview(LiveSessionModality.SCREEN_SHARE)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c11_open_c13_consent_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Live Consent (C-13)")
                        }
                        OutlinedButton(
                            onClick = { onNavigate(ChildScreenId.C15_OFFLINE_SYNC_HELP) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c11_open_c15_offline_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Offline Sync (C-15)")
                        }
                    }
                }
            }
        }
    }
}
