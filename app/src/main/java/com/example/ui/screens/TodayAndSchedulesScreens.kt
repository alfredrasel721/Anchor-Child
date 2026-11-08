package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.AppRule
import com.example.model.ChildScreenId
import com.example.model.ScheduleRoutine
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
import com.example.ui.theme.StitchSecondaryContainer
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceContainerLow
import com.example.ui.theme.StitchTertiary
import com.example.ui.theme.StitchTertiaryFixed
import com.example.viewmodel.AnchorChildUiState

@Composable
fun C03TodayScreenTimeScreen(
    uiState: AnchorChildUiState,
    onNavigate: (ChildScreenId) -> Unit,
    onInspectRestrictedApp: (AppRule) -> Unit,
    onRefreshTelemetry: () -> Unit
) {
    val child = uiState.activeChild
    val remainingMins = (child.dailyAllowanceMinutes - child.usedMinutesToday).coerceAtLeast(0)
    val remHours = remainingMins / 60
    val remMins = remainingMins % 60
    val usedFraction = if (child.dailyAllowanceMinutes > 0) {
        (child.usedMinutesToday.toFloat() / child.dailyAllowanceMinutes.toFloat()).coerceIn(0f, 1f)
    } else {
        0.5f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Screen Time Allowance Ring Card (Stitch C-03)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                text = "ACTIVE MODE: ${child.currentMode.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StitchSecondary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = child.lastSyncLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Circular Allowance Gauge using DrawScope size
                    Box(
                        modifier = Modifier.size(190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 16.dp.toPx()
                            drawArc(
                                color = Color(0xFFE0E9F9),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color(0xFF113C8D),
                                startAngle = 135f,
                                sweepAngle = 270f * (1f - usedFraction),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (remHours > 0) "${remHours}h ${remMins}m" else "${remMins}m",
                                style = MaterialTheme.typography.displayMedium,
                                color = StitchPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "REMAINING TODAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${child.usedMinutesToday}m used of ${child.dailyAllowanceMinutes}m",
                                style = MaterialTheme.typography.labelMedium,
                                color = StitchSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Primary Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(ChildScreenId.C06_REQUEST_EXTRA_TIME) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c03_ask_bonus_time_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreTime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ask for Extra Time")
                        }

                        OutlinedButton(
                            onClick = { onNavigate(ChildScreenId.C04_ALLOWED_APPS_SCHEDULES) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("c03_view_apps_schedules_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Allowed Apps")
                        }
                    }
                }
            }
        }

        // Category Usage Breakdown Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Breakdown Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = onRefreshTelemetry,
                            modifier = Modifier.testTag("c03_refresh_telemetry_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh UsageStats",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Usage", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    CategoryProgressBar(
                        category = "Games (Minecraft, Roblox)",
                        usedText = "45m / 60m cap",
                        progress = 0.75f,
                        barColor = StitchPrimary
                    )
                    CategoryProgressBar(
                        category = "Education (Khan Academy, Duolingo) • Free Bonus",
                        usedText = "55m used • Unlimited",
                        progress = 0.45f,
                        barColor = StatusEnforcedGreen
                    )
                    CategoryProgressBar(
                        category = "Entertainment & Video (YouTube Kids)",
                        usedText = "20m / 30m cap",
                        progress = 0.66f,
                        barColor = StatusPendingAmber
                    )
                }
            }
        }

        // App Limits Snapshot (Tap any paused/capped app to see C-05 Friendly Lockout)
        item {
            Text(
                text = "Today's App Limits (Tap any app for restriction details)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.appRules) { rule ->
            val isPaused = rule.blocked || rule.usedMinutesToday >= rule.dailyLimitMinutes
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInspectRestrictedApp(rule) }
                    .testTag("c03_app_rule_${rule.ruleId}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rule.appName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SemanticStatusBadge(
                                status = if (rule.whitelisted) {
                                    "WHITELISTED"
                                } else if (isPaused) {
                                    "BLOCKED"
                                } else {
                                    rule.status
                                }
                            )
                        }
                        Text(
                            text = if (rule.whitelisted) {
                                "${rule.category} • Always Allowed (${rule.usedMinutesToday}m today)"
                            } else if (rule.blocked) {
                                "${rule.category} • Paused by Family Policy • Tap for reason"
                            } else {
                                "${rule.category} • ${rule.usedMinutesToday}m / ${rule.dailyLimitMinutes}m daily cap"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (isPaused) "Why?" else "Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = StitchPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressBar(
    category: String,
    usedText: String,
    progress: Float,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = usedText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = barColor,
            trackColor = StitchSurfaceContainerLow
        )
    }
}

@Composable
fun C04AllowedAppsSchedulesScreen(
    uiState: AnchorChildUiState,
    onInspectAppRule: (AppRule) -> Unit,
    onRequestExtraTime: () -> Unit
) {
    val whitelistedApps = uiState.appRules.filter { it.whitelisted }
    val managedApps = uiState.appRules.filterNot { it.whitelisted }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Always Allowed (Zero-Cost Whitelisted Apps)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusEnforcedBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Always Allowed",
                            tint = StatusEnforcedGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Always Allowed (Zero Allowance Cost)",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusEnforcedGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Available even during School Focus & Study Windows",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    whitelistedApps.forEach { app ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${app.category} • ${app.usedMinutesToday}m used today",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                SemanticStatusBadge(status = "WHITELISTED")
                            }
                        }
                    }
                }
            }
        }

        // Daily Schedule Rhythm Section (Stitch C-04)
        item {
            Text(
                text = "Daily Schedule Rhythm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.schedules) { sched ->
            ScheduleRoutineCard(schedule = sched)
        }

        // Capped & Scheduled Apps Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time-Capped & Scheduled Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onRequestExtraTime,
                    modifier = Modifier.testTag("c04_request_bonus_button")
                ) {
                    Text("Ask Bonus Time")
                }
            }
        }

        items(managedApps) { app ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInspectAppRule(app) }
                    .testTag("c04_app_${app.ruleId}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${app.category} • ${app.usedMinutesToday}m / ${app.dailyLimitMinutes}m cap",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SemanticStatusBadge(status = app.status)
                    }
                    val ratio = if (app.dailyLimitMinutes > 0) {
                        (app.usedMinutesToday.toFloat() / app.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (ratio >= 1f || app.blocked) StatusFailedCrimson else StitchPrimary,
                        trackColor = StitchSurfaceContainerLow
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRoutineCard(schedule: ScheduleRoutine) {
    val icon = when (schedule.modeCategory) {
        "SCHOOL_FOCUS" -> Icons.Default.School
        "BEDTIME" -> Icons.Default.Bedtime
        "FREE_PLAY" -> Icons.Default.SportsEsports
        else -> Icons.Default.Schedule
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = schedule.title,
                        tint = StitchPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = schedule.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${schedule.windowDescription} • ${schedule.daysSummary}",
                            style = MaterialTheme.typography.labelMedium,
                            color = StitchPrimary
                        )
                    }
                }
                SemanticStatusBadge(status = schedule.status)
            }
            Text(
                text = "Allowed: ${schedule.allowedAppsSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun C05RestrictionReasonScreen(
    uiState: AnchorChildUiState,
    onAskExtraTime: () -> Unit,
    onBackToToday: () -> Unit
) {
    val restricted = uiState.activeRestrictedApp
        ?: uiState.appRules.firstOrNull { it.usedMinutesToday >= it.dailyLimitMinutes || it.blocked }
        ?: uiState.appRules.first()

    val whitelisted = uiState.appRules.filter { it.whitelisted }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Friendly Lockout Explanation Banner (Stitch C-05)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StitchTertiaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = "App Paused",
                            tint = StitchTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Surface(
                        color = StatusPendingBg,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "C-05 • FRIENDLY SCHEDULE PAUSE",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusPendingAmber,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "${restricted.appName} is Paused Right Now",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (restricted.blocked) {
                            "${restricted.appName} is paused under your family's current ${restricted.category} agreement."
                        } else {
                            "You've completed your ${restricted.dailyLimitMinutes}m daily allowance for ${restricted.appName} today. Great job saving your progress before the 5-minute fair-play notice!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Next Unlock Window Pill
                    Surface(
                        color = StitchPrimaryFixed,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Next Unlock Window",
                                    tint = StitchPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Next Automatic Unlock Window",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StitchPrimary
                                    )
                                    Text(
                                        text = "5:30 PM Free Play Window (or Tomorrow 7:00 AM)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = StitchPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onAskExtraTime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("c05_ask_extra_time_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreTime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ask Mom or Dad for Bonus Time (C-06)")
                    }

                    OutlinedButton(
                        onClick = onBackToToday,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("c05_back_today_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Return to Today's Screen Time")
                    }
                }
            }
        }

        // Apps Available Right Now (Always-Allowed Alternatives)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusEnforcedBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Apps You Can Use Right Now (No Limit)",
                        style = MaterialTheme.typography.titleSmall,
                        color = StatusEnforcedGreen,
                        fontWeight = FontWeight.Bold
                    )
                    whitelisted.forEach { wl ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${wl.appName} (${wl.category})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                SemanticStatusBadge(status = "OPEN")
                            }
                        }
                    }
                }
            }
        }
    }
}
