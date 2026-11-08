package com.example.model

import com.google.firebase.Timestamp

/**
 * All 15 Google Stitch Child App Screens (C-01 through C-15).
 */
enum class ChildScreenId(
    val code: String,
    val title: String,
    val subtitle: String,
    val bottomTab: ChildBottomTab
) {
    C01_WELCOME_PAIRING(
        "C-01",
        "Welcome & Pairing Code",
        "Secure 6-digit code & QR link to Parent App",
        ChildBottomTab.TODAY
    ),
    C02_PERMISSION_EDUCATION(
        "C-02",
        "Permissions & Transparency",
        "Step-by-step Android permission education & live status",
        ChildBottomTab.PRIVACY
    ),
    C03_TODAY_SCREEN_TIME(
        "C-03",
        "Today's Screen Time",
        "Remaining allowance gauge, categories & current mode",
        ChildBottomTab.TODAY
    ),
    C04_ALLOWED_APPS_SCHEDULES(
        "C-04",
        "Allowed Apps & Schedules",
        "Always-allowed apps, capped apps & daily schedule rhythm",
        ChildBottomTab.TODAY
    ),
    C05_RESTRICTION_REASON(
        "C-05",
        "Restriction & Next Window",
        "Friendly lockout explanation, next unlock countdown & options",
        ChildBottomTab.TODAY
    ),
    C06_REQUEST_EXTRA_TIME(
        "C-06",
        "Request Extra Time",
        "Ask for +15m, +30m, +45m or 1h bonus time or app unlock",
        ChildBottomTab.REQUESTS
    ),
    C07_PARENT_DECISION_INBOX(
        "C-07",
        "Parent Decision Inbox",
        "Approved, pending & declined requests with parent notes",
        ChildBottomTab.REQUESTS
    ),
    C08_SOS_EMERGENCY_CHECKIN(
        "C-08",
        "SOS & Safe Check-In",
        "3-second hold SOS beacon, 1-tap check-in & emergency contacts",
        ChildBottomTab.SAFETY
    ),
    C09_FAMILY_CHAT_TASKS(
        "C-09",
        "Family Chat & Tasks",
        "Encrypted family messages, shared chores & Sunday review",
        ChildBottomTab.SAFETY
    ),
    C10_MY_ACTIVITY_WELLBEING(
        "C-10",
        "My Activity & Wellbeing",
        "Balance scorecard, streaks, 7-day rhythm & digital superpowers",
        ChildBottomTab.WELLBEING
    ),
    C11_PRIVACY_SHARING_DASHBOARD(
        "C-11",
        "Privacy & Sharing Matrix",
        "What parents see vs strictly private + active permissions",
        ChildBottomTab.PRIVACY
    ),
    C12_PREMIUM_FILTERS_DISCLOSURE(
        "C-12",
        "Safety & Filter Disclosures",
        "Transparent web, social & notification shield rules + appeals",
        ChildBottomTab.PRIVACY
    ),
    C13_INCOMING_LIVE_CONSENT(
        "C-13",
        "Incoming Live Consent",
        "Explicit child-side approval or decline for live parent check-in",
        ChildBottomTab.PRIVACY
    ),
    C14_ACTIVE_LIVE_SESSION(
        "C-14",
        "Active Live Session",
        "Real-time indicator, countdown timer & sovereign instant stop",
        ChildBottomTab.PRIVACY
    ),
    C15_OFFLINE_SYNC_HELP(
        "C-15",
        "Offline Sync & Help",
        "Cached local rules, pending sync queue & connection recovery",
        ChildBottomTab.PRIVACY
    )
}

enum class ChildBottomTab(val label: String) {
    TODAY("Today"),
    REQUESTS("Requests"),
    SAFETY("Safety & Chat"),
    WELLBEING("Wellbeing"),
    PRIVACY("Privacy")
}

// ============================================================================
// Firestore Entities (100% Compatible with Anchor Parent App Schema)
// ============================================================================

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "CHILD_DEVICE", // PRIMARY_GUARDIAN | CO_PARENT | TRUSTED_CONTACT | CHILD_DEVICE
    val familyId: String = "",
    val notificationCadence: String = "Immediate",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FamilyHousehold(
    val familyId: String = "",
    val familyName: String = "Miller Family",
    val ownerUid: String = "",
    val subscriptionTier: String = "PREMIUM_ANNUAL", // FREE | PREMIUM_ANNUAL | PREMIUM_MONTHLY
    val subscriptionActive: Boolean = true,
    val autoApproveLearningApps: Boolean = true,
    val fairPlayNoticeMinutes: Int = 5,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class ChildProfile(
    val childId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val age: Int = 11,
    val gradeLabel: String = "6th Grade",
    val avatarInitials: String = "LM",
    val deviceName: String = "Leo's Pixel 7a",
    val deviceModel: String = "Google Pixel 7a",
    val osVersion: String = "Android 14",
    val batteryLevel: Int = 84,
    val chargingStatus: String = "Discharging",
    val networkType: String = "Wi-Fi",
    val networkName: String = "Miller_Home_5G",
    val storageFreeGb: Double = 42.6,
    val syncStatus: String = "ENFORCED", // ENFORCED | PENDING | FAILED | UNSUPPORTED
    val lastSyncLabel: String = "Synced 12s ago",
    val usedMinutesToday: Int = 80,
    val dailyAllowanceMinutes: Int = 150,
    val currentMode: String = "Free Play Window",
    val locationLabel: String = "Lincoln Middle School",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val knoxVerified: Boolean = false,
    val accessibilityEnabled: Boolean = true,
    val deviceAdminEnabled: Boolean = true,
    val policyPausedUntil: String = "",
    val updatedAt: Timestamp? = null
)

data class PairingCode(
    val codeId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val childName: String = "",
    val code: String = "",
    val expiresAtEpochMs: Long = 0L,
    val status: String = "ACTIVE" // ACTIVE | CONSUMED | EXPIRED
)

data class AppRule(
    val ruleId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val category: String = "Games",
    val dailyLimitMinutes: Int = 60,
    val usedMinutesToday: Int = 0,
    val status: String = "ENFORCED", // ENFORCED | PENDING | SCHEDULE_ONLY | WHITELISTED | BLOCKED
    val blocked: Boolean = false,
    val whitelisted: Boolean = false,
    val scheduleOnly: Boolean = false
)

data class ScheduleRoutine(
    val scheduleId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val title: String = "",
    val windowDescription: String = "",
    val daysSummary: String = "Mon - Fri",
    val modeCategory: String = "SCHOOL_FOCUS",
    val allowedAppsSummary: String = "Phone, Messages, Calculator, Khan Academy",
    val status: String = "ENFORCED", // ENFORCED | PENDING | PAUSED
    val enabled: Boolean = true
)

data class SafeGeofence(
    val geofenceId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val name: String = "",
    val radiusMeters: Int = 150,
    val triggerSummary: String = "Arrive & Depart",
    val scheduleSummary: String = "24/7 Active",
    val activeInside: Boolean = false,
    val alertsEnabled: Boolean = true,
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194
)

data class SafetyAlert(
    val alertId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val childName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "SYSTEM", // SOS | GEOFENCE | DEVICE_HEALTH | SYSTEM
    val severity: String = "INFO", // INFO | WARNING | CRITICAL
    val timestampLabel: String = "Just now",
    val acknowledged: Boolean = false
)

data class ChildRequest(
    val requestId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val childName: String = "",
    val requestType: String = "EXTRA_TIME", // EXTRA_TIME | NEW_APP
    val targetName: String = "",
    val targetCategory: String = "Games",
    val requestedBonusMinutes: Int = 15,
    val childNote: String = "",
    val usedTodaySummary: String = "45m / 45m limit",
    val status: String = "PENDING", // PENDING | APPROVED | DECLINED
    val decisionSummary: String = "",
    val decidedBy: String = ""
)

data class FamilyMessage(
    val messageId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val senderName: String = "",
    val senderRole: String = "CHILD", // PARENT | CHILD | SYSTEM
    val text: String = "",
    @get:com.google.firebase.firestore.PropertyName("isSystemEvent")
    val isSystemEvent: Boolean = false,
    val timestampLabel: String = "Just now"
)

data class FamilyTask(
    val taskId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val title: String = "",
    val rewardSummary: String = "+20m Bonus Screen Time",
    val dueSummary: String = "Today, 6:00 PM",
    val status: String = "IN_PROGRESS", // IN_PROGRESS | READY_FOR_REVIEW | COMPLETED
    val completedByChild: Boolean = false,
    val approvedByParent: Boolean = false
)

data class GuardianContact(
    val guardianId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val relationRole: String = "Primary Guardian",
    val emailOrPhone: String = "",
    @get:com.google.firebase.firestore.PropertyName("isPrimaryGuardian")
    val isPrimaryGuardian: Boolean = true,
    val permissionsSummary: String = "Full Policy & Emergency Access",
    val receiveGeofenceAlerts: Boolean = true,
    val receiveSosSms: Boolean = true,
    val status: String = "VERIFIED"
)


data class PremiumFilterConfig(
    val configId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val webFilterProfile: String = "Pre-Teen Balanced (Ages 10-12)",
    val blockedCategories: List<String> = emptyList(),
    val whitelistedDomains: List<String> = emptyList(),
    val blacklistedDomains: List<String> = emptyList(),
    val socialShieldProfile: String = "Moderate Protection",
    val discordDmWhitelistOnly: Boolean = true,
    val robloxChatFiltered: Boolean = true,
    val whatsappUnknownBlocked: Boolean = true,
    val youtubeRestrictedMode: Boolean = true,
    val notificationFilterEnabled: Boolean = true,
    val notificationProtocol: String = "Hold Non-Urgent During Study & Bedtime",
    val liveSessionAuditCode: String = "AUD-9482-B"
)

// ============================================================================
// Child-Specific Runtime & Telemetry Models
// ============================================================================

enum class LiveSessionModality(val label: String, val badgeText: String) {
    SCREEN_SHARE("Live Screen Share", "Screen View Only"),
    CAMERA_VIEW("Live Camera View", "Front/Rear Camera"),
    AUDIO_LISTEN("Live Microphone Listen-In", "Ambient Audio")
}

data class LiveSessionState(
    val sessionId: String = "LIVE-9482-B",
    val initiatorName: String = "Mom (Sarah Miller)",
    val modality: LiveSessionModality = LiveSessionModality.SCREEN_SHARE,
    val reasonNote: String = "Helping you configure your science project browser settings",
    val status: LiveSessionStatus = LiveSessionStatus.IDLE,
    val remainingSeconds: Int = 600, // Max 10m (600s) auto-stop
    val startedAtLabel: String = "Just now",
    val audioMutedByChild: Boolean = false
)

enum class LiveSessionStatus {
    IDLE,
    INCOMING_CONSENT_REQUIRED,
    ACTIVE_STREAMING,
    TERMINATED_BY_CHILD,
    DECLINED_BY_CHILD
}

data class RealPermissionSnapshot(
    val usageStatsGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val deviceAdminGranted: Boolean = false,
    val notificationListenerGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val microphoneGranted: Boolean = false
) {
    val coreGrantedCount: Int
        get() = listOf(
            usageStatsGranted,
            locationGranted,
            notificationsGranted,
            accessibilityGranted,
            deviceAdminGranted
        ).count { it }
}

data class RealUsageAppItem(
    val packageName: String,
    val appName: String,
    val category: String,
    val usedMinutesToday: Int
)
