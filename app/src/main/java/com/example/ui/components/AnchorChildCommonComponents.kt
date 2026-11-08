package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ChildBottomTab
import com.example.model.ChildProfile
import com.example.model.ChildScreenId
import com.example.model.LiveSessionStatus
import com.example.ui.theme.StatusEnforcedBg
import com.example.ui.theme.StatusEnforcedGreen
import com.example.ui.theme.StatusFailedBg
import com.example.ui.theme.StatusFailedCrimson
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StitchOutlineVariant
import com.example.ui.theme.StitchPrimary
import com.example.ui.theme.StitchPrimaryFixed
import com.example.ui.theme.StitchSecondary
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceContainerLow
import com.google.firebase.auth.FirebaseUser

@Composable
fun AnchorChildTopBar(
    currentScreen: ChildScreenId,
    activeChild: ChildProfile,
    isOnline: Boolean,
    isAuthenticated: Boolean,
    liveStatus: LiveSessionStatus,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    onOpenScreenAtlas: () -> Unit,
    onOpenAuthSheet: () -> Unit,
    onNavigateToOfflineHelp: () -> Unit,
    onNavigateToActiveLive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchSurface)
    ) {
        // Mandatory high-visibility banner if a Live Session is active or requesting consent
        if (liveStatus == LiveSessionStatus.ACTIVE_STREAMING ||
            liveStatus == LiveSessionStatus.INCOMING_CONSENT_REQUIRED
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StatusFailedCrimson)
                    .clickable { onNavigateToActiveLive() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("live_session_global_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Live Session Indicator",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (liveStatus == LiveSessionStatus.ACTIVE_STREAMING) {
                            "LIVE PARENT SESSION ACTIVE • TAP FOR INSTANT STOP"
                        } else {
                            "INCOMING PARENT LIVE CHECK-IN REQUEST • TAP TO REVIEW"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "OPEN",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (canNavigateBack) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("top_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = StitchPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StitchPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Anchor Child Shield",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = StitchPrimaryFixed,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = currentScreen.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = StitchPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentScreen.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${activeChild.name} • ${activeChild.deviceName} • ${activeChild.batteryLevel}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Live Sync / Offline Status Pill
                val syncBg = if (isOnline) StatusEnforcedBg else StatusPendingBg
                val syncFg = if (isOnline) StatusEnforcedGreen else StatusPendingAmber
                Surface(
                    color = syncBg,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clickable { onNavigateToOfflineHelp() }
                        .testTag("sync_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                            contentDescription = "Sync status",
                            tint = syncFg,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOnline) "ENFORCED" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = syncFg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // All 15 Stitch Screens Switcher Button
                IconButton(
                    onClick = onOpenScreenAtlas,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchSurfaceContainerLow)
                        .testTag("open_screen_atlas_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "All 15 Stitch Screens",
                        tint = StitchPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Google Cloud Account Button
                IconButton(
                    onClick = onOpenAuthSheet,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isAuthenticated) StatusEnforcedBg else StitchSurfaceContainerLow)
                        .testTag("open_auth_sheet_button")
                ) {
                    Icon(
                        imageVector = if (isAuthenticated) Icons.Default.VerifiedUser else Icons.Default.AccountCircle,
                        contentDescription = "Account & Cloud Sync",
                        tint = if (isAuthenticated) StatusEnforcedGreen else StitchPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick horizontal screen strip so any of the 15 Stitch screens is 1 tap away
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(StitchSurfaceContainerLow)
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(ChildScreenId.entries) { screen ->
                val selected = screen == currentScreen
                FilterChip(
                    selected = selected,
                    onClick = { /* Handled by caller via chip click wrapper if needed */ },
                    label = {
                        Text(
                            text = "${screen.code} ${screen.title}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.testTag("quick_chip_${screen.code}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StitchPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun AnchorChildQuickScreenStrip(
    currentScreen: ChildScreenId,
    onSelectScreen: (ChildScreenId) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchSurfaceContainerLow)
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(ChildScreenId.entries) { screen ->
            val selected = screen == currentScreen
            Surface(
                color = if (selected) StitchPrimary else Color.White,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (selected) StitchPrimary else StitchOutlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectScreen(screen) }
                    .testTag("strip_${screen.code}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = screen.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color(0xFF94F1D6) else StitchPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AnchorChildBottomBar(
    currentTab: ChildBottomTab,
    pendingDecisionsCount: Int,
    onSelectTab: (ChildBottomTab) -> Unit
) {
    BottomAppBar(
        containerColor = StitchSurface,
        tonalElevation = 4.dp
    ) {
        ChildBottomTab.entries.forEach { tab ->
            val selected = tab == currentTab
            val icon: ImageVector = when (tab) {
                ChildBottomTab.TODAY -> Icons.Default.Schedule
                ChildBottomTab.REQUESTS -> Icons.Default.MoreTime
                ChildBottomTab.SAFETY -> Icons.Default.HealthAndSafety
                ChildBottomTab.WELLBEING -> Icons.Default.Favorite
                ChildBottomTab.PRIVACY -> Icons.Default.Shield
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.testTag("bottom_tab_${tab.name.lowercase()}"),
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    val labelText = if (tab == ChildBottomTab.REQUESTS && pendingDecisionsCount > 0) {
                        "${tab.label} ($pendingDecisionsCount)"
                    } else {
                        tab.label
                    }
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = StitchPrimary,
                    selectedTextColor = StitchPrimary,
                    indicatorColor = StitchPrimaryFixed
                )
            )
        }
    }
}

@Composable
fun StatusFeedbackBanner(
    message: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = !message.isNullOrBlank()) {
        if (!message.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("status_feedback_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF001847)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Confirmation",
                            tint = Color(0xFF94F1D6),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss notification",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchScreenAtlasModal(
    currentScreen: ChildScreenId,
    onSelectScreen: (ChildScreenId) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StitchSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Anchor Child • All 15 Stitch Screens",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Jump directly to any screen from the Master Specification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            ChildScreenId.entries.forEach { screen ->
                val isCurrent = screen == currentScreen
                Surface(
                    color = if (isCurrent) StitchPrimaryFixed else StitchSurfaceContainerLow,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable {
                            onSelectScreen(screen)
                            onDismiss()
                        }
                        .testTag("atlas_item_${screen.code}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = StitchPrimary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = screen.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = screen.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isCurrent) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = StitchSecondary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SemanticStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status.uppercase()) {
        "ENFORCED", "APPROVED", "COMPLETED", "VERIFIED", "WHITELISTED", "ACTIVE" ->
            StatusEnforcedBg to StatusEnforcedGreen
        "PENDING", "SCHEDULE_ONLY", "READY_FOR_REVIEW", "IN_PROGRESS", "WARNING" ->
            StatusPendingBg to StatusPendingAmber
        "FAILED", "BLOCKED", "DECLINED", "CRITICAL", "EXPIRED" ->
            StatusFailedBg to StatusFailedCrimson
        else -> StitchSurfaceContainerLow to StitchPrimary
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = status.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
