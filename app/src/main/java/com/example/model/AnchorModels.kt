package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

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
// Firestore Entities (100% Unified Schema Across Anchor Parent & Anchor Child)
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
    val familyName: String = "My Family",
    val ownerUid: String = "",
    val subscriptionTier: String = "FREE", // FREE | PREMIUM_ANNUAL | PREMIUM_MONTHLY
    val subscriptionActive: Boolean = false,
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
    val age: Int = 0,
    val gradeLabel: String = "",
    val avatarInitials: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val batteryLevel: Int = 0,
    val chargingStatus: String = "Unknown",
    val networkType: String = "Unknown",
    val networkName: String = "",
    val storageFreeGb: Double = 0.0,
    val syncStatus: String = "PENDING", // ENFORCED | PENDING | FAILED | UNSUPPORTED
    val lastSyncLabel: String = "Not synced",
    val usedMinutesToday: Int = 0,
    val dailyAllowanceMinutes: Int = 0,
    val currentMode: String = "Not configured",
    val locationLabel: String = "Location unavailable",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val knoxVerified: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val deviceAdminEnabled: Boolean = false,
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
    val status: String = "NONE", // ACTIVE | CONSUMED | EXPIRED
    val redeemedByUid: String = "",
    val createdAt: Timestamp? = null
)

data class PairedDeviceRecord(
    val deviceUid: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val codeId: String = "",
    val redeemedCode: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val status: String = "PENDING",
    val pairedAtEpochMs: Long = 0L
)

data class AppRule(
    val ruleId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val category: String = "Uncategorized",
    val dailyLimitMinutes: Int = 0,
    val usedMinutesToday: Int = 0,
    val status: String = "PENDING", // ENFORCED | PENDING | SCHEDULE_ONLY | WHITELISTED | BLOCKED
    val blocked: Boolean = false,
    val whitelisted: Boolean = false,
    val scheduleOnly: Boolean = false,
    val updatedAt: Timestamp? = null
)

data class ScheduleRoutine(
    val scheduleId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val title: String = "",
    val windowDescription: String = "",
    val daysSummary: String = "",
    val modeCategory: String = "UNCONFIGURED",
    val iconType: String = "DEFAULT",
    val allowedAppsSummary: String = "",
    val ruleSummary: String = "",
    val status: String = "PENDING", // ENFORCED | PENDING | PAUSED
    val syncStatus: String = "PENDING",
    val enabled: Boolean = false,
    val updatedAt: Timestamp? = null
)

data class SafeGeofence(
    val geofenceId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val name: String = "",
    val radiusMeters: Int = 0,
    val triggerSummary: String = "",
    val scheduleSummary: String = "",
    val activeInside: Boolean = false,
    val alertsEnabled: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val updatedAt: Timestamp? = null
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
    val timestampLabel: String = "",
    val acknowledged: Boolean = false,
    val createdAt: Timestamp? = null
)

data class ChildRequest(
    val requestId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val childName: String = "",
    val requestType: String = "EXTRA_TIME", // EXTRA_TIME | NEW_APP
    val targetName: String = "",
    val targetCategory: String = "",
    val requestedBonusMinutes: Int = 0,
    val childNote: String = "",
    val usedTodaySummary: String = "",
    val status: String = "PENDING", // PENDING | APPROVED | DECLINED
    val decisionSummary: String = "",
    val decidedBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FamilyMessage(
    val messageId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val senderName: String = "",
    val senderRole: String = "SYSTEM", // PARENT | CHILD | SYSTEM
    val text: String = "",
    @get:PropertyName("isSystemEvent")
    @set:PropertyName("isSystemEvent")
    var isSystemEvent: Boolean = false,
    val timestampLabel: String = "Just now",
    val createdAt: Timestamp? = null
)

data class FamilyTask(
    val taskId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val title: String = "",
    val rewardSummary: String = "",
    val dueSummary: String = "",
    val status: String = "PENDING", // IN_PROGRESS | READY_FOR_REVIEW | COMPLETED
    val completedByChild: Boolean = false,
    val approvedByParent: Boolean = false,
    val updatedAt: Timestamp? = null
)

data class GuardianContact(
    val guardianId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val relationRole: String = "",
    val emailOrPhone: String = "",
    @get:PropertyName("isPrimaryGuardian")
    @set:PropertyName("isPrimaryGuardian")
    var isPrimaryGuardian: Boolean = false,
    val permissionsSummary: String = "",
    val receiveGeofenceAlerts: Boolean = false,
    val receiveSosSms: Boolean = false,
    val status: String = "PENDING",
    val updatedAt: Timestamp? = null
)

data class PremiumFilterConfig(
    val configId: String = "",
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "",
    val webFilterProfile: String = "",
    val blockedCategories: List<String> = emptyList(),
    val whitelistedDomains: List<String> = emptyList(),
    val blacklistedDomains: List<String> = emptyList(),
    val socialShieldProfile: String = "",
    val discordDmWhitelistOnly: Boolean = false,
    val robloxChatFiltered: Boolean = false,
    val whatsappUnknownBlocked: Boolean = false,
    val youtubeRestrictedMode: Boolean = false,
    val notificationFilterEnabled: Boolean = false,
    val notificationProtocol: String = "",
    val batchMuteSocialDuringStudy: Boolean = false,
    val blockGamingStreakPrompts: Boolean = false,
    val allowLearningNotifications: Boolean = false,
    val liveSessionAuditCode: String = "",
    val updatedAt: Timestamp? = null
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
    val sessionId: String = "",
    val initiatorName: String = "Parent",
    val modality: LiveSessionModality = LiveSessionModality.SCREEN_SHARE,
    val reasonNote: String = "",
    val status: LiveSessionStatus = LiveSessionStatus.IDLE,
    val remainingSeconds: Int = 0,
    val startedAtLabel: String = "",
    val audioMutedByChild: Boolean = false
)

enum class LiveSessionStatus {
    IDLE,
    INCOMING_CONSENT_REQUIRED,
    CONSENT_GRANTED_PENDING_CONNECTION,
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
