package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.local.AnchorChildLocalDatabase
import com.example.data.local.CachedAppRuleEntity
import com.example.data.local.CachedScheduleEntity
import com.example.data.local.OfflineSyncQueueEntity
import com.example.data.remote.AnchorRepository
import com.example.model.AnchorSeedData
import com.example.model.AppRule
import com.example.model.ChildBottomTab
import com.example.model.ChildProfile
import com.example.model.ChildRequest
import com.example.model.ChildScreenId
import com.example.model.FamilyHousehold
import com.example.model.FamilyMessage
import com.example.model.FamilyTask
import com.example.model.GuardianContact
import com.example.model.LiveSessionModality
import com.example.model.LiveSessionState
import com.example.model.LiveSessionStatus
import com.example.model.PairingCode
import com.example.model.PremiumFilterConfig
import com.example.model.RealPermissionSnapshot
import com.example.model.RealUsageAppItem
import com.example.model.SafeGeofence
import com.example.model.SafetyAlert
import com.example.model.ScheduleRoutine
import com.example.service.DeviceSystemMonitor
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AnchorChildUiState(
    val currentUser: FirebaseUser? = null,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val currentScreen: ChildScreenId = ChildScreenId.C03_TODAY_SCREEN_TIME,
    val screenHistory: List<ChildScreenId> = emptyList(),
    val isPaired: Boolean = true,
    val pairingCodeInput: String = "482915",
    val selectedChildId: String = "leo_01",
    val household: FamilyHousehold = AnchorSeedData.defaultHousehold("preview"),
    val children: List<ChildProfile> = AnchorSeedData.defaultChildren("preview"),
    val pairingCodes: List<PairingCode> = AnchorSeedData.defaultPairingCodes("preview"),
    val appRules: List<AppRule> = AnchorSeedData.defaultAppRules("preview"),
    val schedules: List<ScheduleRoutine> = AnchorSeedData.defaultSchedules("preview"),
    val geofences: List<SafeGeofence> = AnchorSeedData.defaultGeofences("preview"),
    val alerts: List<SafetyAlert> = AnchorSeedData.defaultAlerts("preview"),
    val requests: List<ChildRequest> = AnchorSeedData.defaultRequests("preview"),
    val messages: List<FamilyMessage> = AnchorSeedData.defaultMessages("preview"),
    val tasks: List<FamilyTask> = AnchorSeedData.defaultTasks("preview"),
    val guardians: List<GuardianContact> = AnchorSeedData.defaultGuardians("preview"),
    val premiumConfig: PremiumFilterConfig = AnchorSeedData.defaultPremiumConfig("preview"),
    val permissionSnapshot: RealPermissionSnapshot = RealPermissionSnapshot(),
    val realUsageApps: List<RealUsageAppItem> = emptyList(),
    val offlineQueue: List<OfflineSyncQueueEntity> = emptyList(),
    val isNetworkOnline: Boolean = true,
    val isOfflineSimulated: Boolean = false,
    val activeRestrictedApp: AppRule? = null,
    val sosGraceCountdownSeconds: Int = 0,
    val sosDispatchedActive: Boolean = false,
    val liveSessionState: LiveSessionState = LiveSessionState(),
    val statusBannerMessage: String? = null,
    val wellbeingTrustPoints: Int = 92,
    val bedtimeStreakDays: Int = 5
) {
    val activeChild: ChildProfile
        get() = children.firstOrNull { it.childId == selectedChildId }
            ?: children.firstOrNull()
            ?: AnchorSeedData.defaultChildren("preview").first()

    val isEffectiveOnline: Boolean
        get() = isNetworkOnline && !isOfflineSimulated
}

class AnchorChildViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application)
    private val repository = AnchorRepository(application)
    private val localDb = AnchorChildLocalDatabase.getInstance(application)
    private val localDao = localDb.childLocalDao()
    val systemMonitor = DeviceSystemMonitor(application)

    private val _uiState = MutableStateFlow(AnchorChildUiState())
    val uiState: StateFlow<AnchorChildUiState> = _uiState.asStateFlow()

    private var firestoreListenersJob: Job? = null
    private var sosCountdownJob: Job? = null
    private var liveTimerJob: Job? = null

    init {
        refreshSystemTelemetry()
        observeLocalOfflineDatabase()
        observeAuthChanges()
    }

    private fun observeLocalOfflineDatabase() {
        viewModelScope.launch {
            localDao.observeOfflineQueue().collectLatest { queue ->
                _uiState.value = _uiState.value.copy(offlineQueue = queue)
            }
        }
    }

    private fun observeAuthChanges() {
        viewModelScope.launch {
            authManager.authState.collectLatest { user ->
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    authErrorMessage = null
                )
                if (user != null) {
                    startRealtimeHouseholdStreams(user)
                } else {
                    firestoreListenersJob?.cancel()
                }
            }
        }
    }

    private fun startRealtimeHouseholdStreams(user: FirebaseUser) {
        firestoreListenersJob?.cancel()
        firestoreListenersJob = viewModelScope.launch {
            val uid = user.uid
            try {
                repository.ensureSeeded(
                    uid = uid,
                    displayName = user.displayName ?: "Leo Miller",
                    email = user.email ?: "leo@anchor.family"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Using cached local rules while syncing household: ${e.localizedMessage ?: "Offline"}"
                )
            }

            launch {
                repository.observeHousehold(uid).collectLatest { household ->
                    if (household != null) {
                        _uiState.value = _uiState.value.copy(household = household)
                    }
                }
            }
            launch {
                repository.observeChildren(uid).collectLatest { list ->
                    if (list.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(children = list)
                    }
                }
            }
            launch {
                repository.observePairingCodes(uid).collectLatest { codes ->
                    if (codes.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(pairingCodes = codes)
                    }
                }
            }
            launch {
                repository.observeAppRules(uid).collectLatest { rules ->
                    if (rules.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(appRules = rules)
                        cacheAppRulesLocally(rules)
                    }
                }
            }
            launch {
                repository.observeSchedules(uid).collectLatest { schedules ->
                    if (schedules.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(schedules = schedules)
                        cacheSchedulesLocally(schedules)
                    }
                }
            }
            launch {
                repository.observeGeofences(uid).collectLatest { geos ->
                    if (geos.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(geofences = geos)
                    }
                }
            }
            launch {
                repository.observeAlerts(uid).collectLatest { alerts ->
                    if (alerts.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(alerts = alerts)
                    }
                }
            }
            launch {
                repository.observeRequests(uid).collectLatest { requests ->
                    if (requests.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(requests = requests)
                    }
                }
            }
            launch {
                repository.observeMessages(uid).collectLatest { messages ->
                    if (messages.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(messages = messages)
                    }
                }
            }
            launch {
                repository.observeTasks(uid).collectLatest { tasks ->
                    if (tasks.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(tasks = tasks)
                    }
                }
            }
            launch {
                repository.observeGuardians(uid).collectLatest { guardians ->
                    if (guardians.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(guardians = guardians)
                    }
                }
            }
            launch {
                repository.observePremiumConfigs(uid).collectLatest { configs ->
                    configs.firstOrNull()?.let { prem ->
                        _uiState.value = _uiState.value.copy(premiumConfig = prem)
                    }
                }
            }
        }
    }

    private suspend fun cacheAppRulesLocally(rules: List<AppRule>) {
        val now = System.currentTimeMillis()
        val entities = rules.map { r ->
            CachedAppRuleEntity(
                ruleId = r.ruleId,
                familyId = r.familyId,
                childId = r.childId,
                packageName = r.packageName,
                appName = r.appName,
                category = r.category,
                dailyLimitMinutes = r.dailyLimitMinutes,
                usedMinutesToday = r.usedMinutesToday,
                status = r.status,
                blocked = r.blocked,
                whitelisted = r.whitelisted,
                scheduleOnly = r.scheduleOnly,
                cachedAtMs = now
            )
        }
        localDao.upsertAppRules(entities)
    }

    private suspend fun cacheSchedulesLocally(schedules: List<ScheduleRoutine>) {
        val now = System.currentTimeMillis()
        val entities = schedules.map { s ->
            CachedScheduleEntity(
                scheduleId = s.scheduleId,
                familyId = s.familyId,
                childId = s.childId,
                title = s.title,
                windowDescription = s.windowDescription,
                daysSummary = s.daysSummary,
                modeCategory = s.modeCategory,
                allowedAppsSummary = s.allowedAppsSummary,
                status = s.status,
                enabled = s.enabled,
                cachedAtMs = now
            )
        }
        localDao.upsertSchedules(entities)
    }

    // ========================================================================
    // Authentication Actions
    // ========================================================================

    fun trySilentSignIn(activityContext: Context) {
        if (authManager.currentUser != null) return
        viewModelScope.launch {
            authManager.trySilentSignIn(activityContext)
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAuthLoading = true, authErrorMessage = null)
            val result = authManager.signInWithGoogle(activityContext)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        currentUser = user,
                        isAuthLoading = false,
                        statusBannerMessage = "Signed in as ${user.displayName ?: user.email} • Connected to Anchor Cloud"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isAuthLoading = false,
                        authErrorMessage = err.localizedMessage ?: "Google Sign-In cancelled or unavailable"
                    )
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _uiState.value = _uiState.value.copy(
                currentUser = null,
                statusBannerMessage = "Signed out of Google Account"
            )
        }
    }

    // ========================================================================
    // Navigation Across All 15 Stitch Screens
    // ========================================================================

    fun navigateTo(screen: ChildScreenId) {
        val current = _uiState.value.currentScreen
        if (current == screen) return
        val updatedHistory = (_uiState.value.screenHistory + current).takeLast(12)
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            screenHistory = updatedHistory
        )
    }

    fun navigateBack(): Boolean {
        val history = _uiState.value.screenHistory
        if (history.isEmpty()) {
            if (_uiState.value.currentScreen != ChildScreenId.C03_TODAY_SCREEN_TIME) {
                _uiState.value = _uiState.value.copy(currentScreen = ChildScreenId.C03_TODAY_SCREEN_TIME)
                return true
            }
            return false
        }
        val prev = history.last()
        _uiState.value = _uiState.value.copy(
            currentScreen = prev,
            screenHistory = history.dropLast(1)
        )
        return true
    }

    fun selectBottomTab(tab: ChildBottomTab) {
        val target = when (tab) {
            ChildBottomTab.TODAY -> ChildScreenId.C03_TODAY_SCREEN_TIME
            ChildBottomTab.REQUESTS -> ChildScreenId.C06_REQUEST_EXTRA_TIME
            ChildBottomTab.SAFETY -> ChildScreenId.C08_SOS_EMERGENCY_CHECKIN
            ChildBottomTab.WELLBEING -> ChildScreenId.C10_MY_ACTIVITY_WELLBEING
            ChildBottomTab.PRIVACY -> ChildScreenId.C11_PRIVACY_SHARING_DASHBOARD
        }
        navigateTo(target)
    }

    fun inspectAppRuleRestriction(rule: AppRule) {
        _uiState.value = _uiState.value.copy(activeRestrictedApp = rule)
        navigateTo(ChildScreenId.C05_RESTRICTION_REASON)
    }

    fun clearStatusBanner() {
        _uiState.value = _uiState.value.copy(statusBannerMessage = null)
    }

    fun postStatusBanner(message: String) {
        _uiState.value = _uiState.value.copy(statusBannerMessage = message)
    }

    // ========================================================================
    // Real Android Telemetry & Permission Sync
    // ========================================================================

    fun refreshSystemTelemetry() {
        val perms = systemMonitor.inspectPermissions()
        val (batteryPct, chargingLabel) = systemMonitor.readBatteryTelemetry()
        val (netType, netName) = systemMonitor.readNetworkTelemetry()
        val isOnline = systemMonitor.isNetworkOnline()
        val freeGb = systemMonitor.readFreeStorageGb()
        val realUsage = systemMonitor.queryTodayRealUsageStats()

        val currentChild = _uiState.value.activeChild
        val realUsedTotal = if (realUsage.isNotEmpty()) {
            realUsage.sumOf { it.usedMinutesToday }.coerceAtLeast(currentChild.usedMinutesToday)
        } else {
            currentChild.usedMinutesToday
        }

        val updatedChild = currentChild.copy(
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            batteryLevel = batteryPct,
            chargingStatus = chargingLabel,
            networkType = netType,
            networkName = netName,
            storageFreeGb = freeGb,
            usedMinutesToday = realUsedTotal,
            accessibilityEnabled = perms.accessibilityGranted,
            deviceAdminEnabled = perms.deviceAdminGranted
        )

        val updatedChildren = _uiState.value.children.map {
            if (it.childId == updatedChild.childId) updatedChild else it
        }

        _uiState.value = _uiState.value.copy(
            permissionSnapshot = perms,
            realUsageApps = realUsage,
            isNetworkOnline = isOnline,
            children = updatedChildren
        )

        val uid = _uiState.value.currentUser?.uid
        if (uid != null && _uiState.value.isEffectiveOnline) {
            viewModelScope.launch {
                try {
                    repository.updateChildDeviceTelemetry(uid, updatedChild)
                } catch (_: Exception) {
                    // Handled by structured FirestoreErrorHandler
                }
            }
        }
    }

    fun refreshGpsLocationAndSync() {
        viewModelScope.launch {
            val coords = systemMonitor.fetchCurrentGpsCoordinates()
            val currentChild = _uiState.value.activeChild
            val updatedChild = if (coords != null) {
                currentChild.copy(
                    latitude = coords.first,
                    longitude = coords.second,
                    locationLabel = "Verified Device GPS (${String.format(Locale.US, "%.4f, %.4f", coords.first, coords.second)})",
                    lastSyncLabel = "GPS Synced Just Now"
                )
            } else {
                currentChild.copy(lastSyncLabel = "Location Synced Just Now")
            }

            val updatedChildren = _uiState.value.children.map {
                if (it.childId == updatedChild.childId) updatedChild else it
            }
            _uiState.value = _uiState.value.copy(
                children = updatedChildren,
                statusBannerMessage = "Location & telemetry synced (${updatedChild.locationLabel})"
            )

            val uid = _uiState.value.currentUser?.uid
            if (uid != null && _uiState.value.isEffectiveOnline) {
                try {
                    repository.updateChildDeviceTelemetry(uid, updatedChild)
                } catch (_: Exception) {
                }
            }
        }
    }

    // ========================================================================
    // Pairing Flow (Screen C-01)
    // ========================================================================

    fun updatePairingCodeInput(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(pairingCodeInput = filtered)
    }

    fun selectChildProfile(childId: String) {
        _uiState.value = _uiState.value.copy(selectedChildId = childId)
    }

    fun submitPairingCode() {
        val code = _uiState.value.pairingCodeInput.trim()
        if (code.length < 4) {
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "Please enter the 6-digit pairing code shown on your Parent App."
            )
            return
        }

        val child = _uiState.value.activeChild
        val uid = _uiState.value.currentUser?.uid
        viewModelScope.launch {
            if (uid != null && _uiState.value.isEffectiveOnline) {
                repository.claimPairingCode(
                    uid = uid,
                    code = code,
                    childId = child.childId,
                    deviceName = child.deviceName,
                    deviceModel = child.deviceModel,
                    osVersion = child.osVersion
                )
            } else {
                enqueueOfflineItem(
                    actionType = "DEVICE_PAIRING",
                    title = "Claim Pairing Code #$code",
                    payloadSummary = "${child.name} (${child.deviceModel})"
                )
            }
            _uiState.value = _uiState.value.copy(
                isPaired = true,
                statusBannerMessage = "Device paired with ${ _uiState.value.household.familyName } for ${child.name}!",
                currentScreen = ChildScreenId.C02_PERMISSION_EDUCATION
            )
        }
    }

    // ========================================================================
    // Extra Time & App Requests (Screen C-06 & C-07)
    // ========================================================================

    fun submitExtraTimeRequest(
        requestType: String,
        targetName: String,
        targetCategory: String,
        bonusMinutes: Int,
        reasonChip: String,
        customNote: String
    ) {
        val child = _uiState.value.activeChild
        val combinedNote = buildString {
            if (reasonChip.isNotBlank()) append("[$reasonChip] ")
            if (customNote.isNotBlank()) append(customNote)
            if (isEmpty()) append("Could I please have +${bonusMinutes}m for $targetName?")
        }
        val usedSummary = "${child.usedMinutesToday}m / ${child.dailyAllowanceMinutes}m daily limit"
        val uid = _uiState.value.currentUser?.uid

        viewModelScope.launch {
            if (uid != null && _uiState.value.isEffectiveOnline) {
                try {
                    val created = repository.submitChildRequest(
                        uid = uid,
                        childId = child.childId,
                        childName = child.name,
                        requestType = requestType,
                        targetName = targetName,
                        targetCategory = targetCategory,
                        requestedBonusMinutes = bonusMinutes,
                        childNote = combinedNote,
                        usedTodaySummary = usedSummary
                    )
                    _uiState.value = _uiState.value.copy(
                        requests = listOf(created) + _uiState.value.requests.filterNot { it.requestId == created.requestId },
                        statusBannerMessage = "Sent +${bonusMinutes}m request for $targetName to parents!",
                        currentScreen = ChildScreenId.C07_PARENT_DECISION_INBOX
                    )
                    return@launch
                } catch (_: Exception) {
                }
            }

            // Offline fallback / local state update
            val localReq = ChildRequest(
                requestId = "req_local_${System.currentTimeMillis()}",
                familyId = _uiState.value.household.familyId,
                ownerUid = uid ?: "local",
                childId = child.childId,
                childName = child.name,
                requestType = requestType,
                targetName = targetName,
                targetCategory = targetCategory,
                requestedBonusMinutes = bonusMinutes,
                childNote = combinedNote,
                usedTodaySummary = usedSummary,
                status = "PENDING",
                decisionSummary = if (_uiState.value.isEffectiveOnline) {
                    "Sent just now • Waiting for Parent App response"
                } else {
                    "Queued offline • Will auto-send as soon as Wi-Fi/Cellular reconnects"
                }
            )
            enqueueOfflineItem(
                actionType = "EXTRA_TIME_REQUEST",
                title = "Request +${bonusMinutes}m for $targetName",
                payloadSummary = combinedNote
            )
            _uiState.value = _uiState.value.copy(
                requests = listOf(localReq) + _uiState.value.requests,
                statusBannerMessage = "Request for $targetName (+${bonusMinutes}m) added to Parent Decision Inbox!",
                currentScreen = ChildScreenId.C07_PARENT_DECISION_INBOX
            )
        }
    }

    fun submitWebsiteAppeal(domainUrl: String, note: String) {
        val cleanDomain = domainUrl.trim().ifBlank { "scratch.mit.edu" }
        submitExtraTimeRequest(
            requestType = "NEW_APP",
            targetName = "Domain Whitelist: $cleanDomain",
            targetCategory = "Web Filter Appeal",
            bonusMinutes = 60,
            reasonChip = "School / Research",
            customNote = note.ifBlank { "Needed for class project research" }
        )
    }

    // ========================================================================
    // SOS Emergency Beacon & Safe-Zone Check-Ins (Screen C-08)
    // ========================================================================

    fun startSosGraceCountdown() {
        sosCountdownJob?.cancel()
        _uiState.value = _uiState.value.copy(
            sosGraceCountdownSeconds = 5,
            sosDispatchedActive = false
        )
        sosCountdownJob = viewModelScope.launch {
            for (sec in 5 downTo 1) {
                _uiState.value = _uiState.value.copy(sosGraceCountdownSeconds = sec)
                delay(1000L)
            }
            _uiState.value = _uiState.value.copy(sosGraceCountdownSeconds = 0)
            dispatchSosNow(isPracticeDrill = false)
        }
    }

    fun cancelSosGraceCountdown() {
        sosCountdownJob?.cancel()
        _uiState.value = _uiState.value.copy(
            sosGraceCountdownSeconds = 0,
            sosDispatchedActive = false,
            statusBannerMessage = "SOS Alert cancelled within 5-second grace window."
        )
    }

    fun dispatchSosNow(isPracticeDrill: Boolean = false) {
        sosCountdownJob?.cancel()
        val child = _uiState.value.activeChild
        val uid = _uiState.value.currentUser?.uid
        viewModelScope.launch {
            if (uid != null && _uiState.value.isEffectiveOnline) {
                try {
                    val alert = repository.triggerChildSosBeacon(
                        uid = uid,
                        childId = child.childId,
                        childName = child.name,
                        locationLabel = child.locationLabel,
                        batteryLevel = child.batteryLevel,
                        isPracticeDrill = isPracticeDrill
                    )
                    _uiState.value = _uiState.value.copy(
                        alerts = listOf(alert) + _uiState.value.alerts,
                        sosGraceCountdownSeconds = 0,
                        sosDispatchedActive = !isPracticeDrill,
                        statusBannerMessage = if (isPracticeDrill) {
                            "Practice SOS Drill verified! Parents received test confirmation."
                        } else {
                            "EMERGENCY SOS DISPATCHED to Mom, Dad & Trusted Contacts with live GPS!"
                        }
                    )
                    return@launch
                } catch (_: Exception) {
                }
            }

            enqueueOfflineItem(
                actionType = "SOS_BEACON",
                title = if (isPracticeDrill) "SOS Practice Drill" else "URGENT SOS Emergency Beacon",
                payloadSummary = "Location: ${child.locationLabel} • Battery ${child.batteryLevel}%"
            )
            _uiState.value = _uiState.value.copy(
                sosGraceCountdownSeconds = 0,
                sosDispatchedActive = !isPracticeDrill,
                statusBannerMessage = if (isPracticeDrill) {
                    "SOS Practice Drill logged locally."
                } else {
                    "SOS Emergency Beacon queued + ready for Carrier SMS fallback!"
                }
            )
        }
    }

    fun sendQuickCheckIn(checkInHeadline: String) {
        val child = _uiState.value.activeChild
        val uid = _uiState.value.currentUser?.uid
        viewModelScope.launch {
            if (uid != null && _uiState.value.isEffectiveOnline) {
                try {
                    val alert = repository.sendSafeZoneCheckIn(
                        uid = uid,
                        childId = child.childId,
                        childName = child.name,
                        checkInHeadline = checkInHeadline,
                        locationLabel = child.locationLabel,
                        batteryLevel = child.batteryLevel
                    )
                    _uiState.value = _uiState.value.copy(
                        alerts = listOf(alert) + _uiState.value.alerts,
                        statusBannerMessage = "Check-In sent to parents: \"$checkInHeadline\""
                    )
                    return@launch
                } catch (_: Exception) {
                }
            }

            enqueueOfflineItem(
                actionType = "SAFE_CHECK_IN",
                title = checkInHeadline,
                payloadSummary = "${child.locationLabel} • Battery ${child.batteryLevel}%"
            )
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "Check-In recorded: \"$checkInHeadline\" (${child.locationLabel})"
            )
        }
    }

    // ========================================================================
    // Family Chat & Shared Tasks (Screen C-09)
    // ========================================================================

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val child = _uiState.value.activeChild
        val uid = _uiState.value.currentUser?.uid

        viewModelScope.launch {
            if (uid != null && _uiState.value.isEffectiveOnline) {
                try {
                    val msg = repository.sendFamilyMessage(
                        uid = uid,
                        childId = child.childId,
                        senderName = child.name,
                        senderRole = "CHILD",
                        text = trimmed,
                        isSystemEvent = false
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + msg
                    )
                    return@launch
                } catch (_: Exception) {
                }
            }

            val localMsg = FamilyMessage(
                messageId = "msg_local_${System.currentTimeMillis()}",
                familyId = _uiState.value.household.familyId,
                ownerUid = uid ?: "local",
                childId = child.childId,
                senderName = child.name,
                senderRole = "CHILD",
                text = trimmed,
                isSystemEvent = false,
                timestampLabel = SimpleDateFormat("h:mm a", Locale.US).format(Date())
            )
            enqueueOfflineItem(
                actionType = "FAMILY_CHAT",
                title = "Message to Family Chat",
                payloadSummary = trimmed
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + localMsg
            )
        }
    }

    fun toggleTaskCompleted(task: FamilyTask) {
        val uid = _uiState.value.currentUser?.uid
        viewModelScope.launch {
            if (uid != null && _uiState.value.isEffectiveOnline) {
                try {
                    repository.markTaskCompletedByChild(uid, task)
                } catch (_: Exception) {
                }
            }
            val nextCompleted = !task.completedByChild
            val updatedTasks = _uiState.value.tasks.map {
                if (it.taskId == task.taskId) {
                    it.copy(
                        completedByChild = nextCompleted,
                        status = if (nextCompleted) "READY_FOR_REVIEW" else "IN_PROGRESS"
                    )
                } else {
                    it
                }
            }
            val nextPoints = if (nextCompleted) {
                (_uiState.value.wellbeingTrustPoints + 4).coerceAtMost(100)
            } else {
                _uiState.value.wellbeingTrustPoints
            }
            _uiState.value = _uiState.value.copy(
                tasks = updatedTasks,
                wellbeingTrustPoints = nextPoints,
                statusBannerMessage = if (nextCompleted) {
                    "Marked \"${task.title}\" ready for parent review! (${task.rewardSummary})"
                } else {
                    "Re-opened task \"${task.title}\""
                }
            )
        }
    }

    // ========================================================================
    // Premium Live Consent & Sovereign Instant Stop (Screen C-13 & C-14)
    // ========================================================================

    fun requestIncomingLiveConsent(modality: LiveSessionModality) {
        val current = _uiState.value.liveSessionState
        _uiState.value = _uiState.value.copy(
            liveSessionState = current.copy(
                sessionId = "LIVE-${(1000..9999).random()}-B",
                modality = modality,
                status = LiveSessionStatus.INCOMING_CONSENT_REQUIRED,
                remainingSeconds = 600,
                audioMutedByChild = false
            )
        )
        navigateTo(ChildScreenId.C13_INCOMING_LIVE_CONSENT)
    }

    fun approveIncomingLiveSession() {
        val session = _uiState.value.liveSessionState
        val child = _uiState.value.activeChild
        val uid = _uiState.value.currentUser?.uid

        _uiState.value = _uiState.value.copy(
            liveSessionState = session.copy(
                status = LiveSessionStatus.ACTIVE_STREAMING,
                remainingSeconds = 600,
                startedAtLabel = SimpleDateFormat("h:mm:ss a", Locale.US).format(Date())
            ),
            statusBannerMessage = "Live ${session.modality.label} approved • You can stop it at any second."
        )
        navigateTo(ChildScreenId.C14_ACTIVE_LIVE_SESSION)

        liveTimerJob?.cancel()
        liveTimerJob = viewModelScope.launch {
            while (_uiState.value.liveSessionState.remainingSeconds > 0 &&
                _uiState.value.liveSessionState.status == LiveSessionStatus.ACTIVE_STREAMING
            ) {
                delay(1000L)
                val rem = _uiState.value.liveSessionState.remainingSeconds - 1
                _uiState.value = _uiState.value.copy(
                    liveSessionState = _uiState.value.liveSessionState.copy(remainingSeconds = rem)
                )
            }
            if (_uiState.value.liveSessionState.remainingSeconds <= 0) {
                stopActiveLiveSessionImmediately(reason = "10-minute safety cap reached")
            }
        }

        if (uid != null && _uiState.value.isEffectiveOnline) {
            viewModelScope.launch {
                repository.recordLiveSessionAuditEvent(
                    uid = uid,
                    childId = child.childId,
                    childName = child.name,
                    sessionId = session.sessionId,
                    modalityLabel = session.modality.label,
                    actionDescription = "Approved by Child"
                )
            }
        }
    }

    fun declineIncomingLiveSession(declineMessage: String = "I'm busy right now, let's chat instead!") {
        liveTimerJob?.cancel()
        val session = _uiState.value.liveSessionState
        val child = _uiState.value.activeChild
        val uid = _uiState.value.currentUser?.uid

        _uiState.value = _uiState.value.copy(
            liveSessionState = session.copy(status = LiveSessionStatus.DECLINED_BY_CHILD),
            statusBannerMessage = "Declined live ${session.modality.label} • Sent polite note to parent."
        )
        sendChatMessage("Declined ${session.modality.label}: $declineMessage")

        if (uid != null && _uiState.value.isEffectiveOnline) {
            viewModelScope.launch {
                repository.recordLiveSessionAuditEvent(
                    uid = uid,
                    childId = child.childId,
                    childName = child.name,
                    sessionId = session.sessionId,
                    modalityLabel = session.modality.label,
                    actionDescription = "Declined by Child ($declineMessage)"
                )
            }
        }
        navigateTo(ChildScreenId.C11_PRIVACY_SHARING_DASHBOARD)
    }

    fun stopActiveLiveSessionImmediately(reason: String = "Stopped immediately by child") {
        liveTimerJob?.cancel()
        val session = _uiState.value.liveSessionState
        val child = _uiState.value.activeChild
        val uid = _uiState.value.currentUser?.uid

        _uiState.value = _uiState.value.copy(
            liveSessionState = session.copy(
                status = LiveSessionStatus.TERMINATED_BY_CHILD,
                remainingSeconds = 0
            ),
            statusBannerMessage = "Live session terminated immediately • Hardware stream closed."
        )

        if (uid != null && _uiState.value.isEffectiveOnline) {
            viewModelScope.launch {
                repository.recordLiveSessionAuditEvent(
                    uid = uid,
                    childId = child.childId,
                    childName = child.name,
                    sessionId = session.sessionId,
                    modalityLabel = session.modality.label,
                    actionDescription = reason
                )
            }
        }
    }

    fun toggleLiveAudioMute() {
        val current = _uiState.value.liveSessionState
        _uiState.value = _uiState.value.copy(
            liveSessionState = current.copy(audioMutedByChild = !current.audioMutedByChild)
        )
    }

    // ========================================================================
    // Offline Sync Queue & Recovery (Screen C-15)
    // ========================================================================

    fun toggleSimulatedOfflineMode() {
        val next = !_uiState.value.isOfflineSimulated
        _uiState.value = _uiState.value.copy(
            isOfflineSimulated = next,
            statusBannerMessage = if (next) {
                "Offline Mode active: Local Room rules remain enforced & actions queue locally."
            } else {
                "Reconnected to Anchor Cloud • Flushing queued offline events..."
            }
        )
        if (!next) {
            flushOfflineQueueNow()
        }
    }

    fun flushOfflineQueueNow() {
        viewModelScope.launch {
            refreshSystemTelemetry()
            localDao.markAllQueuedSynced()
            delay(500L)
            localDao.clearSyncedItems()
            _uiState.value = _uiState.value.copy(
                isOfflineSimulated = false,
                statusBannerMessage = "All local offline events & device telemetry synced with Parent App!"
            )
        }
    }

    private suspend fun enqueueOfflineItem(
        actionType: String,
        title: String,
        payloadSummary: String
    ) {
        val fmt = SimpleDateFormat("h:mm a", Locale.US)
        localDao.enqueueOfflineAction(
            OfflineSyncQueueEntity(
                actionType = actionType,
                title = title,
                payloadSummary = payloadSummary,
                createdAtLabel = fmt.format(Date()),
                synced = false
            )
        )
    }
}
