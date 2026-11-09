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
import com.example.data.local.PairingSessionStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AnchorChildUiState(
    val currentUser: FirebaseUser? = null,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val currentScreen: ChildScreenId = ChildScreenId.C01_WELCOME_PAIRING,
    val screenHistory: List<ChildScreenId> = emptyList(),
    val isPaired: Boolean = false,
    val pairedFamilyId: String = "",
    val pairingCodeInput: String = "",
    val selectedChildId: String = "child_leo",
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

    val targetFamilyKey: String
        get() = pairedFamilyId
}

class AnchorChildViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application)
    private val repository = AnchorRepository(application)
    private val localDb = AnchorChildLocalDatabase.getInstance(application)
    private val localDao = localDb.childLocalDao()
    private val pairingSessionStore = PairingSessionStore(application)
    val systemMonitor = DeviceSystemMonitor(application)

    private val _uiState = MutableStateFlow(AnchorChildUiState())
    val uiState: StateFlow<AnchorChildUiState> = _uiState.asStateFlow()

    private var firestoreListenersJob: Job? = null
    private var sosCountdownJob: Job? = null
    private var liveTimerJob: Job? = null

    init {
        refreshSystemTelemetry()
        observePersistedPairingSession()
        observeLocalOfflineDatabase()
        observeAuthChanges()
    }

    private fun observePersistedPairingSession() {
        viewModelScope.launch {
            pairingSessionStore.sessionFlow.collectLatest { session ->
                val user = _uiState.value.currentUser
                if (user != null && session.isPaired && session.familyId.isNotBlank()) {
                    restoreVerifiedPairingSession(user, session.familyId, session.childId)
                } else if (!session.isPaired || session.familyId.isBlank()) {
                    firestoreListenersJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        isPaired = false,
                        pairedFamilyId = "",
                        selectedChildId = "child_leo",
                        currentScreen = ChildScreenId.C01_WELCOME_PAIRING
                    )
                }
            }
        }
    }

    private suspend fun restoreVerifiedPairingSession(
        user: FirebaseUser,
        familyId: String,
        childId: String
    ) {
        val verification = repository.verifyPairingSession(familyId, childId)
        if (verification.verified) {
            _uiState.value = _uiState.value.copy(
                isPaired = true,
                pairedFamilyId = verification.familyId,
                selectedChildId = verification.childId,
                currentScreen = if (_uiState.value.currentScreen == ChildScreenId.C01_WELCOME_PAIRING) {
                    ChildScreenId.C02_PERMISSION_EDUCATION
                } else {
                    _uiState.value.currentScreen
                },
                statusBannerMessage = "Saved pairing verified with ${verification.childName}."
            )
            startRealtimeHouseholdStreams(user, verification.familyId)
        } else {
            pairingSessionStore.clearPairingSession()
            firestoreListenersJob?.cancel()
            _uiState.value = _uiState.value.copy(
                isPaired = false,
                pairedFamilyId = "",
                selectedChildId = "child_leo",
                currentScreen = ChildScreenId.C01_WELCOME_PAIRING,
                statusBannerMessage = verification.errorMessage
                    ?: "This device is no longer paired. Enter a new code from Anchor Parent."
            )
        }
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
                    val session = pairingSessionStore.sessionFlow.first()
                    if (session.isPaired && session.familyId.isNotBlank()) {
                        restoreVerifiedPairingSession(user, session.familyId, session.childId)
                    } else {
                        firestoreListenersJob?.cancel()
                        _uiState.value = _uiState.value.copy(
                            isPaired = false,
                            pairedFamilyId = "",
                            currentScreen = ChildScreenId.C01_WELCOME_PAIRING
                        )
                    }
                } else {
                    firestoreListenersJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        isPaired = false,
                        pairedFamilyId = "",
                        currentScreen = ChildScreenId.C01_WELCOME_PAIRING
                    )
                }
            }
        }
    }

    private fun startRealtimeHouseholdStreams(user: FirebaseUser, familyIdOrUid: String) {
        firestoreListenersJob?.cancel()
        firestoreListenersJob = viewModelScope.launch {
            val targetFamily = familyIdOrUid
            if (targetFamily.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isPaired = false,
                    pairedFamilyId = "",
                    currentScreen = ChildScreenId.C01_WELCOME_PAIRING,
                    statusBannerMessage = "Pair this device with Anchor Parent before syncing family data."
                )
                return@launch
            }

            launch {
                repository.observeHousehold(targetFamily).collectLatest { household ->
                    if (household != null) {
                        _uiState.value = _uiState.value.copy(household = household)
                    }
                }
            }
            launch {
                repository.observeChildren(targetFamily).collectLatest { list ->
                    if (list.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(children = list)
                    }
                }
            }
            launch {
                repository.observeAppRules(targetFamily).collectLatest { rules ->
                    if (rules.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(appRules = rules)
                        cacheAppRulesLocally(rules)
                    }
                }
            }
            launch {
                repository.observeSchedules(targetFamily).collectLatest { schedules ->
                    if (schedules.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(schedules = schedules)
                        cacheSchedulesLocally(schedules)
                    }
                }
            }
            launch {
                repository.observeGeofences(targetFamily).collectLatest { geos ->
                    if (geos.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(geofences = geos)
                    }
                }
            }
            launch {
                repository.observeAlerts(targetFamily).collectLatest { alerts ->
                    if (alerts.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(alerts = alerts)
                    }
                }
            }
            launch {
                repository.observeRequests(targetFamily).collectLatest { requests ->
                    if (requests.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(requests = requests)
                    }
                }
            }
            launch {
                repository.observeMessages(targetFamily).collectLatest { messages ->
                    if (messages.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(messages = messages)
                    }
                }
            }
            launch {
                repository.observeTasks(targetFamily).collectLatest { tasks ->
                    if (tasks.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(tasks = tasks)
                    }
                }
            }
            launch {
                repository.observeGuardians(targetFamily).collectLatest { guardians ->
                    if (guardians.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(guardians = guardians)
                    }
                }
            }
            launch {
                repository.observePremiumConfigs(targetFamily).collectLatest { configs ->
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
            val targetFam = _uiState.value.targetFamilyKey
            viewModelScope.launch {
                try {
                    repository.updateChildDeviceTelemetry(targetFam, updatedChild)
                } catch (_: Exception) {
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
                children = updatedChildren
            )

            val uid = _uiState.value.currentUser?.uid
            if (uid != null && _uiState.value.isEffectiveOnline) {
                try {
                    repository.updateChildDeviceTelemetry(_uiState.value.targetFamilyKey, updatedChild)
                    _uiState.value = _uiState.value.copy(
                        statusBannerMessage = "Location & telemetry synced to Anchor Cloud (${updatedChild.locationLabel})"
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        statusBannerMessage = "GPS updated locally; cloud sync failed: ${e.localizedMessage ?: "Permission or network error"}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "GPS updated locally (${updatedChild.locationLabel})"
                )
            }
        }
    }

    // ========================================================================
    // Secure Pairing Flow (Screen C-01)
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
        if (code.filter { it.isDigit() }.length != 6) {
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "Please enter the complete 6-digit pairing code shown in Anchor Parent."
            )
            return
        }

        val child = _uiState.value.activeChild
        val user = _uiState.value.currentUser
        viewModelScope.launch {
            if (user != null && _uiState.value.isEffectiveOnline) {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Verifying the one-time pairing code securely..."
                )
                val redemption = repository.redeemPairingCode(
                    rawCode = code,
                    deviceName = child.deviceName,
                    deviceModel = child.deviceModel,
                    osVersion = child.osVersion
                )
                if (redemption.verified) {
                    pairingSessionStore.saveVerifiedPairing(
                        familyId = redemption.familyId,
                        ownerUid = redemption.ownerUid,
                        childId = redemption.childId,
                        childName = redemption.childName,
                        codeId = redemption.codeId
                    )
                    _uiState.value = _uiState.value.copy(
                        isPaired = true,
                        pairedFamilyId = redemption.familyId,
                        selectedChildId = redemption.childId,
                        statusBannerMessage = "Verified & paired with ${redemption.childName}!",
                        currentScreen = ChildScreenId.C02_PERMISSION_EDUCATION
                    )
                    startRealtimeHouseholdStreams(user, redemption.familyId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPaired = false,
                        pairedFamilyId = "",
                        statusBannerMessage = redemption.errorMessage
                            ?: "Pairing code verification failed on the trusted server."
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Sign in with Google and connect to the internet so Anchor Cloud can verify this one-time pairing code."
                )
            }
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
                        familyIdOrUid = _uiState.value.targetFamilyKey,
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

            // Offline fallback / persistent local Room queue
            val idempotencyKey = "req_${System.currentTimeMillis()}"
            val localReq = ChildRequest(
                requestId = idempotencyKey,
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
                decisionSummary = "Queued in local Room storage • Will upload upon reconnection"
            )
            enqueueOfflineItem(
                idempotencyKey = idempotencyKey,
                actionType = "EXTRA_TIME_REQUEST",
                title = "Request +${bonusMinutes}m for $targetName",
                payloadSummary = combinedNote,
                targetName = targetName,
                targetCategory = targetCategory,
                bonusMinutes = bonusMinutes
            )
            _uiState.value = _uiState.value.copy(
                requests = listOf(localReq) + _uiState.value.requests,
                statusBannerMessage = "Saved request for $targetName (+${bonusMinutes}m) to offline queue!",
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
            val coords = systemMonitor.fetchCurrentGpsCoordinates()
            val locationSummary = if (coords != null) {
                "${child.locationLabel} (${String.format(Locale.US, "%.4f, %.4f", coords.first, coords.second)})"
            } else {
                child.locationLabel
            }

            if (uid != null && _uiState.value.isEffectiveOnline) {
                try {
                    val alert = repository.triggerChildSosBeacon(
                        familyIdOrUid = _uiState.value.targetFamilyKey,
                        childId = child.childId,
                        childName = child.name,
                        locationLabel = locationSummary,
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
                idempotencyKey = "sos_${System.currentTimeMillis()}",
                actionType = "SOS_BEACON",
                title = if (isPracticeDrill) "SOS Practice Drill" else "URGENT SOS Emergency Beacon",
                payloadSummary = "Location: $locationSummary • Battery ${child.batteryLevel}%"
            )
            _uiState.value = _uiState.value.copy(
                sosGraceCountdownSeconds = 0,
                sosDispatchedActive = !isPracticeDrill,
                statusBannerMessage = if (isPracticeDrill) {
                    "SOS Practice Drill queued locally."
                } else {
                    "SOS Emergency Beacon queued locally + ready for Carrier SMS fallback!"
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
                        familyIdOrUid = _uiState.value.targetFamilyKey,
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
                idempotencyKey = "checkin_${System.currentTimeMillis()}",
                actionType = "SAFE_CHECK_IN",
                title = checkInHeadline,
                payloadSummary = "${child.locationLabel} • Battery ${child.batteryLevel}%"
            )
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "Check-In queued offline: \"$checkInHeadline\" (${child.locationLabel})"
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
                        familyIdOrUid = _uiState.value.targetFamilyKey,
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

            val idempotencyKey = "msg_${System.currentTimeMillis()}"
            val localMsg = FamilyMessage(
                messageId = idempotencyKey,
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
                idempotencyKey = idempotencyKey,
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
                    repository.markTaskCompletedByChild(_uiState.value.targetFamilyKey, task)
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
                    familyIdOrUid = _uiState.value.targetFamilyKey,
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
                    familyIdOrUid = _uiState.value.targetFamilyKey,
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
                    familyIdOrUid = _uiState.value.targetFamilyKey,
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
    // Offline Sync Queue with Reliable Acknowledgements & Deduplication (C-15)
    // ========================================================================

    fun toggleSimulatedOfflineMode() {
        val next = !_uiState.value.isOfflineSimulated
        _uiState.value = _uiState.value.copy(
            isOfflineSimulated = next,
            statusBannerMessage = if (next) {
                "Offline Mode active: Local Room rules remain enforced & actions queue locally."
            } else {
                "Reconnected • Verifying and uploading queued offline events..."
            }
        )
        if (!next) {
            flushOfflineQueueNow()
        }
    }

    fun flushOfflineQueueNow() {
        viewModelScope.launch {
            refreshSystemTelemetry()
            val uid = _uiState.value.currentUser?.uid
            val isOnline = systemMonitor.isNetworkOnline() && !_uiState.value.isOfflineSimulated
            if (uid == null || !isOnline) {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Queued items remain safely stored in Room SQLite until signed in & online."
                )
                return@launch
            }

            val pendingItems = localDao.getPendingUnsyncedItems()
            val child = _uiState.value.activeChild
            val targetFam = _uiState.value.targetFamilyKey
            var uploadedCount = 0

            for (item in pendingItems) {
                try {
                    when (item.actionType) {
                        "EXTRA_TIME_REQUEST" -> {
                            repository.submitChildRequest(
                                familyIdOrUid = targetFam,
                                childId = child.childId,
                                childName = child.name,
                                requestType = "EXTRA_TIME",
                                targetName = item.targetName.ifBlank { "Minecraft" },
                                targetCategory = item.targetCategory.ifBlank { "Games" },
                                requestedBonusMinutes = if (item.bonusMinutes > 0) item.bonusMinutes else 15,
                                childNote = item.payloadSummary,
                                usedTodaySummary = "${child.usedMinutesToday}m / ${child.dailyAllowanceMinutes}m",
                                deterministicRequestId = item.idempotencyKey
                            )
                        }
                        "SAFE_CHECK_IN" -> {
                            repository.sendSafeZoneCheckIn(
                                familyIdOrUid = targetFam,
                                childId = child.childId,
                                childName = child.name,
                                checkInHeadline = item.title,
                                locationLabel = child.locationLabel,
                                batteryLevel = child.batteryLevel,
                                deterministicAlertId = item.idempotencyKey
                            )
                        }
                        "SOS_BEACON" -> {
                            repository.triggerChildSosBeacon(
                                familyIdOrUid = targetFam,
                                childId = child.childId,
                                childName = child.name,
                                locationLabel = child.locationLabel,
                                batteryLevel = child.batteryLevel,
                                isPracticeDrill = item.title.contains("Practice", ignoreCase = true),
                                deterministicAlertId = item.idempotencyKey
                            )
                        }
                        "FAMILY_CHAT" -> {
                            repository.sendFamilyMessage(
                                familyIdOrUid = targetFam,
                                childId = child.childId,
                                senderName = child.name,
                                senderRole = "CHILD",
                                text = item.payloadSummary,
                                isSystemEvent = false,
                                deterministicMessageId = item.idempotencyKey
                            )
                        }
                    }
                    // Mark synced ONLY after server operation succeeds!
                    localDao.markItemSynced(item.id)
                    uploadedCount++
                } catch (_: Exception) {
                    localDao.incrementRetryCount(item.id)
                }
            }

            localDao.clearSyncedItems()
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = if (uploadedCount > 0) {
                    "Verified & uploaded $uploadedCount queued offline event(s) to Anchor Cloud!"
                } else if (pendingItems.isEmpty()) {
                    "All device telemetry & family rules are in sync with Anchor Cloud."
                } else {
                    "Some queued items could not be verified and remain safely in local queue for retry."
                }
            )
        }
    }

    private suspend fun enqueueOfflineItem(
        idempotencyKey: String,
        actionType: String,
        title: String,
        payloadSummary: String,
        targetName: String = "",
        targetCategory: String = "",
        bonusMinutes: Int = 0
    ) {
        val fmt = SimpleDateFormat("h:mm a", Locale.US)
        localDao.enqueueOfflineAction(
            OfflineSyncQueueEntity(
                idempotencyKey = idempotencyKey,
                actionType = actionType,
                title = title,
                payloadSummary = payloadSummary,
                targetName = targetName,
                targetCategory = targetCategory,
                bonusMinutes = bonusMinutes,
                createdAtLabel = fmt.format(Date()),
                synced = false
            )
        )
    }
}
