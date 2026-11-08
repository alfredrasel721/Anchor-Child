package com.example.model

import com.google.firebase.Timestamp

object AnchorSeedData {

    fun defaultHousehold(uid: String, familyId: String = "fam_$uid"): FamilyHousehold {
        return FamilyHousehold(
            familyId = familyId,
            familyName = "Jenkins Household",
            ownerUid = uid,
            subscriptionTier = "PREMIUM_ANNUAL",
            subscriptionActive = true,
            autoApproveLearningApps = true,
            fairPlayNoticeMinutes = 5,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
    }

    fun defaultChildren(uid: String, familyId: String = "fam_$uid"): List<ChildProfile> {
        return listOf(
            ChildProfile(
                childId = "child_leo",
                familyId = familyId,
                ownerUid = uid,
                name = "Leo",
                age = 11,
                gradeLabel = "6th Grade",
                avatarInitials = "LJ",
                deviceName = "Leo's Pixel 7a",
                deviceModel = "Google Pixel 7a",
                osVersion = "Android 14",
                batteryLevel = 84,
                chargingStatus = "Discharging",
                networkType = "Wi-Fi",
                networkName = "Jenkins_Home_5G",
                storageFreeGb = 42.6,
                syncStatus = "ENFORCED",
                lastSyncLabel = "Synced 12s ago",
                usedMinutesToday = 80,
                dailyAllowanceMinutes = 150,
                currentMode = "Free Play Window",
                locationLabel = "Oakridge Middle School",
                latitude = 37.7749,
                longitude = -122.4194,
                knoxVerified = false,
                accessibilityEnabled = true,
                deviceAdminEnabled = true,
                policyPausedUntil = "",
                updatedAt = Timestamp.now()
            ),
            ChildProfile(
                childId = "child_maya",
                familyId = familyId,
                ownerUid = uid,
                name = "Maya",
                age = 8,
                gradeLabel = "3rd Grade",
                avatarInitials = "MJ",
                deviceName = "Maya's Galaxy Tab A9",
                deviceModel = "Samsung SM-X110",
                osVersion = "Android 14 (One UI 6)",
                batteryLevel = 92,
                chargingStatus = "Charging (AC)",
                networkType = "Wi-Fi",
                networkName = "Jenkins_Home_5G",
                storageFreeGb = 28.4,
                syncStatus = "ENFORCED",
                lastSyncLabel = "Synced 25s ago",
                usedMinutesToday = 45,
                dailyAllowanceMinutes = 90,
                currentMode = "Homework Focus",
                locationLabel = "Jenkins Home Sanctuary",
                latitude = 37.7812,
                longitude = -122.4110,
                knoxVerified = true,
                accessibilityEnabled = true,
                deviceAdminEnabled = true,
                policyPausedUntil = "",
                updatedAt = Timestamp.now()
            )
        )
    }

    fun defaultPairingCodes(uid: String, familyId: String = "fam_$uid"): List<PairingCode> {
        return listOf(
            PairingCode(
                codeId = "pair_child_leo_482915",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                childName = "Leo",
                code = "482915",
                expiresAtEpochMs = System.currentTimeMillis() + 600_000L, // 10-minute expiration
                status = "ACTIVE"
            )
        )
    }

    fun defaultAppRules(uid: String, familyId: String = "fam_$uid"): List<AppRule> {
        return listOf(
            AppRule(
                ruleId = "rule_minecraft",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                packageName = "com.mojang.minecraftpe",
                appName = "Minecraft",
                category = "Games",
                dailyLimitMinutes = 45,
                usedMinutesToday = 45,
                status = "ENFORCED",
                blocked = false,
                whitelisted = false,
                scheduleOnly = false
            ),
            AppRule(
                ruleId = "rule_roblox",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                packageName = "com.roblox.client",
                appName = "Roblox",
                category = "Games",
                dailyLimitMinutes = 45,
                usedMinutesToday = 28,
                status = "ENFORCED",
                blocked = false,
                whitelisted = false,
                scheduleOnly = false
            ),
            AppRule(
                ruleId = "rule_youtube",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                packageName = "com.google.android.youtube",
                appName = "YouTube Kids & Video",
                category = "Entertainment",
                dailyLimitMinutes = 30,
                usedMinutesToday = 20,
                status = "SCHEDULE_ONLY",
                blocked = false,
                whitelisted = false,
                scheduleOnly = true
            ),
            AppRule(
                ruleId = "rule_duolingo",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                packageName = "com.duolingo",
                appName = "Duolingo Spanish",
                category = "Education",
                dailyLimitMinutes = 1440,
                usedMinutesToday = 25,
                status = "WHITELISTED",
                blocked = false,
                whitelisted = true,
                scheduleOnly = false
            ),
            AppRule(
                ruleId = "rule_khan",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                packageName = "org.khanacademy.android",
                appName = "Khan Academy",
                category = "Education",
                dailyLimitMinutes = 1440,
                usedMinutesToday = 30,
                status = "WHITELISTED",
                blocked = false,
                whitelisted = true,
                scheduleOnly = false
            ),
            AppRule(
                ruleId = "rule_spotify",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                packageName = "com.spotify.music",
                appName = "Spotify Kids Audio",
                category = "Creativity & Audio",
                dailyLimitMinutes = 1440,
                usedMinutesToday = 15,
                status = "WHITELISTED",
                blocked = false,
                whitelisted = true,
                scheduleOnly = false
            ),
            AppRule(
                ruleId = "rule_tiktok",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                packageName = "com.zhiliaoapp.musically",
                appName = "TikTok",
                category = "Social",
                dailyLimitMinutes = 0,
                usedMinutesToday = 0,
                status = "BLOCKED",
                blocked = true,
                whitelisted = false,
                scheduleOnly = false
            )
        )
    }

    fun defaultSchedules(uid: String, familyId: String = "fam_$uid"): List<ScheduleRoutine> {
        return listOf(
            ScheduleRoutine(
                scheduleId = "sched_school",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                title = "School Focus Mode",
                windowDescription = "08:00 AM - 03:15 PM",
                daysSummary = "Mon, Tue, Wed, Thu, Fri",
                modeCategory = "SCHOOL_FOCUS",
                iconType = "SCHOOL",
                allowedAppsSummary = "Phone, Family Chat, Calculator, Khan Academy, Google Docs",
                ruleSummary = "Phone, Family Chat, Calculator, Khan Academy, Google Docs",
                status = "ENFORCED",
                syncStatus = "ENFORCED",
                enabled = true
            ),
            ScheduleRoutine(
                scheduleId = "sched_homework",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                title = "After-School Study & Homework",
                windowDescription = "04:00 PM - 05:30 PM",
                daysSummary = "Mon - Fri",
                modeCategory = "STUDY_WINDOW",
                iconType = "SCHOOL",
                allowedAppsSummary = "Education Apps, Duolingo, Khan Academy, Reference Browser",
                ruleSummary = "Education Apps, Duolingo, Khan Academy, Reference Browser",
                status = "ENFORCED",
                syncStatus = "ENFORCED",
                enabled = true
            ),
            ScheduleRoutine(
                scheduleId = "sched_freeplay",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                title = "Afternoon Free Play Window",
                windowDescription = "05:30 PM - 08:30 PM",
                daysSummary = "Daily",
                modeCategory = "FREE_PLAY",
                iconType = "CUSTOM",
                allowedAppsSummary = "All Approved Games & Media within Daily Allowance",
                ruleSummary = "All Approved Games & Media within Daily Allowance",
                status = "ENFORCED",
                syncStatus = "ENFORCED",
                enabled = true
            ),
            ScheduleRoutine(
                scheduleId = "sched_bedtime",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                title = "Bedtime Wind-Down & Rest",
                windowDescription = "08:30 PM - 07:00 AM",
                daysSummary = "Sun - Thu (Fri/Sat 9:30 PM)",
                modeCategory = "BEDTIME",
                iconType = "BEDTIME",
                allowedAppsSummary = "Alarm Clock, Spotify Calm Audio, Emergency SOS & Parents",
                ruleSummary = "Alarm Clock, Spotify Calm Audio, Emergency SOS & Parents",
                status = "ENFORCED",
                syncStatus = "ENFORCED",
                enabled = true
            )
        )
    }

    fun defaultGeofences(uid: String, familyId: String = "fam_$uid"): List<SafeGeofence> {
        return listOf(
            SafeGeofence(
                geofenceId = "geo_home",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                name = "Jenkins Home Sanctuary",
                radiusMeters = 120,
                triggerSummary = "Arrive & Depart Notifications",
                scheduleSummary = "Always Active",
                activeInside = false,
                alertsEnabled = true,
                latitude = 37.7812,
                longitude = -122.4110
            ),
            SafeGeofence(
                geofenceId = "geo_school",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                name = "Oakridge Middle School",
                radiusMeters = 200,
                triggerSummary = "Arrive 8:00 AM • Depart 3:15 PM",
                scheduleSummary = "Weekdays 7:30 AM - 4:30 PM",
                activeInside = true,
                alertsEnabled = true,
                latitude = 37.7749,
                longitude = -122.4194
            ),
            SafeGeofence(
                geofenceId = "geo_grandma",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                name = "Grandma Elena's House",
                radiusMeters = 150,
                triggerSummary = "Arrival Check-In Enabled",
                scheduleSummary = "Weekends & After School",
                activeInside = false,
                alertsEnabled = true,
                latitude = 37.7690,
                longitude = -122.4290
            )
        )
    }

    fun defaultAlerts(uid: String, familyId: String = "fam_$uid"): List<SafetyAlert> {
        return listOf(
            SafetyAlert(
                alertId = "alert_geo_school",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                childName = "Leo",
                title = "Arrived at Oakridge Middle School",
                description = "Automatic safe-zone check-in verified within 200m boundary.",
                category = "GEOFENCE",
                severity = "INFO",
                timestampLabel = "Today, 8:04 AM",
                acknowledged = true
            ),
            SafetyAlert(
                alertId = "alert_sync_ok",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                childName = "Leo",
                title = "Family Agreement Synced",
                description = "Weekday study schedule and +20m Duolingo bonus applied.",
                category = "SYSTEM",
                severity = "INFO",
                timestampLabel = "Today, 12:30 PM",
                acknowledged = true
            )
        )
    }

    fun defaultRequests(uid: String, familyId: String = "fam_$uid"): List<ChildRequest> {
        return listOf(
            ChildRequest(
                requestId = "req_minecraft_15",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                childName = "Leo",
                requestType = "EXTRA_TIME",
                targetName = "Minecraft",
                targetCategory = "Games",
                requestedBonusMinutes = 15,
                childNote = "Finishing our co-op bridge build with Sam before dinner!",
                usedTodaySummary = "45m / 45m daily cap",
                status = "PENDING",
                decisionSummary = "Waiting for Mom or Dad (~4m avg response)",
                decidedBy = ""
            ),
            ChildRequest(
                requestId = "req_spotify_approved",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                childName = "Leo",
                requestType = "EXTRA_TIME",
                targetName = "Spotify Kids Audio",
                targetCategory = "Music & Podcasts",
                requestedBonusMinutes = 30,
                childNote = "Listening to science podcast while drawing",
                usedTodaySummary = "15m used today",
                status = "APPROVED",
                decisionSummary = "Approved! Great choice listening while sketching.",
                decidedBy = "Mom (Sarah)"
            ),
            ChildRequest(
                requestId = "req_roblox_declined",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                childName = "Leo",
                requestType = "EXTRA_TIME",
                targetName = "Roblox",
                targetCategory = "Games",
                requestedBonusMinutes = 45,
                childNote = "Late night lobby event",
                usedTodaySummary = "45m / 45m daily cap",
                status = "DECLINED",
                decisionSummary = "It's past 8:15 PM on a school night—let's save it for Saturday afternoon!",
                decidedBy = "Dad (Mark)"
            )
        )
    }

    fun defaultMessages(uid: String, familyId: String = "fam_$uid"): List<FamilyMessage> {
        return listOf(
            FamilyMessage(
                messageId = "msg_01",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                senderName = "Mom (Sarah)",
                senderRole = "PARENT",
                text = "Hi Leo! Hope science lab went great. Let me know when soccer practice wraps up.",
                isSystemEvent = false,
                timestampLabel = "2:45 PM"
            ),
            FamilyMessage(
                messageId = "msg_02",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                senderName = "Anchor System",
                senderRole = "SYSTEM",
                text = "Check-In Shared: Leo marked 'I'm Safe & Arrived' at Oakridge Middle School.",
                isSystemEvent = true,
                timestampLabel = "3:12 PM"
            ),
            FamilyMessage(
                messageId = "msg_03",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                senderName = "Leo",
                senderRole = "CHILD",
                text = "Finished my math worksheet early! Heading to the library tables now.",
                isSystemEvent = false,
                timestampLabel = "3:18 PM"
            )
        )
    }

    fun defaultTasks(uid: String, familyId: String = "fam_$uid"): List<FamilyTask> {
        return listOf(
            FamilyTask(
                taskId = "task_math",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                title = "Complete 20m Khan Academy Fractions Module",
                rewardSummary = "+20m Bonus Game Time",
                dueSummary = "Today, 5:30 PM",
                status = "COMPLETED",
                completedByChild = true,
                approvedByParent = true
            ),
            FamilyTask(
                taskId = "task_backpack",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                title = "Pack Soccer Gear & Charge Chromebook",
                rewardSummary = "+15m Weekend Allowance + 10 Trust Pts",
                dueSummary = "Today, 7:30 PM",
                status = "IN_PROGRESS",
                completedByChild = false,
                approvedByParent = false
            ),
            FamilyTask(
                taskId = "task_sleep_streak",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                title = "Dock Phone by 8:30 PM (5-Night Streak Pact)",
                rewardSummary = "+30m Friday Movie & Game Night",
                dueSummary = "Tonight, 8:30 PM",
                status = "IN_PROGRESS",
                completedByChild = false,
                approvedByParent = false
            )
        )
    }

    fun defaultGuardians(uid: String, familyId: String = "fam_$uid"): List<GuardianContact> {
        return listOf(
            GuardianContact(
                guardianId = "guard_mom",
                familyId = familyId,
                ownerUid = uid,
                name = "Mom (Sarah Jenkins)",
                relationRole = "Primary Guardian",
                emailOrPhone = "+15552348891",
                isPrimaryGuardian = true,
                permissionsSummary = "Full Policy, Approvals & SOS Dispatch",
                receiveGeofenceAlerts = true,
                receiveSosSms = true,
                status = "VERIFIED"
            ),
            GuardianContact(
                guardianId = "guard_dad",
                familyId = familyId,
                ownerUid = uid,
                name = "Dad (Mark Jenkins)",
                relationRole = "Co-Parent Guardian",
                emailOrPhone = "+15552348892",
                isPrimaryGuardian = false,
                permissionsSummary = "Full Policy, Approvals & SOS Dispatch",
                receiveGeofenceAlerts = true,
                receiveSosSms = true,
                status = "VERIFIED"
            ),
            GuardianContact(
                guardianId = "guard_grandma",
                familyId = familyId,
                ownerUid = uid,
                name = "Grandma Elena",
                relationRole = "Trusted Emergency Contact",
                emailOrPhone = "+15559876123",
                isPrimaryGuardian = false,
                permissionsSummary = "Emergency SOS & Pickup Contact Only",
                receiveGeofenceAlerts = false,
                receiveSosSms = true,
                status = "VERIFIED"
            )
        )
    }

    fun defaultPremiumConfig(uid: String, familyId: String = "fam_$uid"): PremiumFilterConfig {
        return PremiumFilterConfig(
            configId = "prem_child_leo",
            familyId = familyId,
            ownerUid = uid,
            childId = "child_leo",
            webFilterProfile = "BALANCED_GUARDIAN",
            blockedCategories = listOf(
                "Adult & Explicit Content",
                "Gambling, Betting & Loot Boxes",
                "Violence, Hate & Extremism",
                "Social Media Web Portals"
            ),
            whitelistedDomains = listOf(
                "khanacademy.org",
                "duolingo.com",
                "pbskids.org",
                "nasa.gov",
                "oakridgeschools.edu"
            ),
            blacklistedDomains = listOf(
                "omegle.com",
                "4chan.org",
                "discord.com/login",
                "unblockedgames76.io"
            ),
            socialShieldProfile = "SUPERVISED",
            discordDmWhitelistOnly = true,
            robloxChatFiltered = true,
            whatsappUnknownBlocked = true,
            youtubeRestrictedMode = true,
            notificationFilterEnabled = true,
            notificationProtocol = "BATCHING",
            batchMuteSocialDuringStudy = true,
            blockGamingStreakPrompts = true,
            allowLearningNotifications = true,
            liveSessionAuditCode = "#LIV-84920"
        )
    }
}
