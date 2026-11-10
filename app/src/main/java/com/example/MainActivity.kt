package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ChildScreenId
import com.example.service.AnchorMonitorForegroundService
import com.example.ui.components.AnchorChildBottomBar
import com.example.ui.components.AnchorChildQuickScreenStrip
import com.example.ui.components.AnchorChildTopBar
import com.example.ui.components.SemanticStatusBadge
import com.example.ui.components.StatusFeedbackBanner
import com.example.ui.components.StitchScreenAtlasModal
import com.example.ui.screens.C01WelcomePairingScreen
import com.example.ui.screens.C02PermissionEducationScreen
import com.example.ui.screens.C03TodayScreenTimeScreen
import com.example.ui.screens.C04AllowedAppsSchedulesScreen
import com.example.ui.screens.C05RestrictionReasonScreen
import com.example.ui.screens.C06RequestExtraTimeScreen
import com.example.ui.screens.C07ParentDecisionInboxScreen
import com.example.ui.screens.C08SosEmergencyCheckInScreen
import com.example.ui.screens.C09FamilyChatTasksScreen
import com.example.ui.screens.C10MyActivityWellbeingScreen
import com.example.ui.screens.C11PrivacySharingDashboardScreen
import com.example.ui.screens.C12PremiumFiltersDisclosureScreen
import com.example.ui.screens.C13IncomingLiveConsentScreen
import com.example.ui.screens.C14ActiveLiveSessionScreen
import com.example.ui.screens.C15OfflineSyncHelpScreen
import com.example.ui.theme.AnchorChildTheme
import com.example.ui.theme.StitchPrimary
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceContainerLow
import com.example.viewmodel.AnchorChildViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebaseSafely()
        enableEdgeToEdge()
        try {
            startService(Intent(this, AnchorMonitorForegroundService::class.java))
        } catch (_: Exception) {
        }
        setContent {
            AnchorChildTheme {
                AnchorChildAppRoot()
            }
        }
    }

    private fun initializeFirebaseSafely() {
        try {
            val firebaseApp = FirebaseApp.initializeApp(this)
                ?: FirebaseApp.initializeApp(
                    this,
                    FirebaseOptions.Builder()
                        .setProjectId("demo-anchor-local")
                        .setApplicationId("1:000000000000:android:0000000000000000")
                        .setApiKey("AIzaSyDummyKeyForLocalRobolectricTest")
                        .build()
                )

            // Keep debug attestation for local/CI builds; use Play Integrity in release builds.
            val appCheck = FirebaseAppCheck.getInstance(firebaseApp)
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            } else {
                appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        } catch (_: Exception) {
            // Cloud sign-in and pairing require the real Firebase google-services.json.
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorChildAppRoot(
    viewModel: AnchorChildViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showScreenAtlas by remember { mutableStateOf(false) }
    var showAccountModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.trySilentSignIn(context)
    }

    // BackHandler on all secondary screens so system back returns cleanly
    BackHandler(enabled = uiState.currentScreen != ChildScreenId.C03_TODAY_SCREEN_TIME) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                AnchorChildTopBar(
                    currentScreen = uiState.currentScreen,
                    activeChild = uiState.activeChild,
                    isOnline = uiState.isEffectiveOnline,
                    isPaired = uiState.isPaired,
                    isAuthenticated = uiState.currentUser != null,
                    liveStatus = uiState.liveSessionState.status,
                    canNavigateBack = uiState.currentScreen != ChildScreenId.C03_TODAY_SCREEN_TIME,
                    onNavigateBack = { viewModel.navigateBack() },
                    onOpenScreenAtlas = { showScreenAtlas = true },
                    onOpenAuthSheet = { showAccountModal = true },
                    onNavigateToOfflineHelp = {
                        viewModel.navigateTo(ChildScreenId.C15_OFFLINE_SYNC_HELP)
                    },
                    onNavigateToActiveLive = {
                        viewModel.navigateTo(ChildScreenId.C14_ACTIVE_LIVE_SESSION)
                    }
                )
                if (BuildConfig.DEBUG) {
                    AnchorChildQuickScreenStrip(
                        currentScreen = uiState.currentScreen,
                        onSelectScreen = { viewModel.navigateTo(it) }
                    )
                }
                StatusFeedbackBanner(
                    message = uiState.statusBannerMessage,
                    onDismiss = { viewModel.clearStatusBanner() }
                )
            }
        },
        bottomBar = {
            if (uiState.isPaired) {
                AnchorChildBottomBar(
                    currentTab = uiState.currentScreen.bottomTab,
                    pendingDecisionsCount = uiState.requests.count { it.status == "PENDING" },
                    onSelectTab = { viewModel.selectBottomTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentScreen) {
                ChildScreenId.C01_WELCOME_PAIRING -> C01WelcomePairingScreen(
                    uiState = uiState,
                    onUpdatePairingCode = viewModel::updatePairingCodeInput,
                    onSubmitPairing = viewModel::submitPairingCode,
                    onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
                    onNavigateToPermissions = {
                        viewModel.navigateTo(ChildScreenId.C02_PERMISSION_EDUCATION)
                    }
                )

                ChildScreenId.C02_PERMISSION_EDUCATION -> C02PermissionEducationScreen(
                    uiState = uiState,
                    systemMonitor = viewModel.systemMonitor,
                    onRefreshPermissions = viewModel::refreshSystemTelemetry,
                    onContinueToDashboard = {
                        viewModel.navigateTo(ChildScreenId.C03_TODAY_SCREEN_TIME)
                    }
                )

                ChildScreenId.C03_TODAY_SCREEN_TIME -> C03TodayScreenTimeScreen(
                    uiState = uiState,
                    onNavigate = viewModel::navigateTo,
                    onInspectRestrictedApp = viewModel::inspectAppRuleRestriction,
                    onRefreshTelemetry = viewModel::refreshSystemTelemetry
                )

                ChildScreenId.C04_ALLOWED_APPS_SCHEDULES -> C04AllowedAppsSchedulesScreen(
                    uiState = uiState,
                    onInspectAppRule = viewModel::inspectAppRuleRestriction,
                    onRequestExtraTime = {
                        viewModel.navigateTo(ChildScreenId.C06_REQUEST_EXTRA_TIME)
                    }
                )

                ChildScreenId.C05_RESTRICTION_REASON -> C05RestrictionReasonScreen(
                    uiState = uiState,
                    onAskExtraTime = {
                        viewModel.navigateTo(ChildScreenId.C06_REQUEST_EXTRA_TIME)
                    },
                    onBackToToday = {
                        viewModel.navigateTo(ChildScreenId.C03_TODAY_SCREEN_TIME)
                    }
                )

                ChildScreenId.C06_REQUEST_EXTRA_TIME -> C06RequestExtraTimeScreen(
                    uiState = uiState,
                    onSubmitRequest = viewModel::submitExtraTimeRequest,
                    onOpenDecisionInbox = {
                        viewModel.navigateTo(ChildScreenId.C07_PARENT_DECISION_INBOX)
                    }
                )

                ChildScreenId.C07_PARENT_DECISION_INBOX -> C07ParentDecisionInboxScreen(
                    uiState = uiState,
                    onNewRequest = {
                        viewModel.navigateTo(ChildScreenId.C06_REQUEST_EXTRA_TIME)
                    },
                    onOpenFamilyChat = {
                        viewModel.navigateTo(ChildScreenId.C09_FAMILY_CHAT_TASKS)
                    }
                )

                ChildScreenId.C08_SOS_EMERGENCY_CHECKIN -> C08SosEmergencyCheckInScreen(
                    uiState = uiState,
                    systemMonitor = viewModel.systemMonitor,
                    onStartSosCountdown = viewModel::startSosGraceCountdown,
                    onCancelSosCountdown = viewModel::cancelSosGraceCountdown,
                    onDispatchSosImmediately = { viewModel.dispatchSosNow(isPracticeDrill = false) },
                    onRunPracticeSosDrill = { viewModel.dispatchSosNow(isPracticeDrill = true) },
                    onSendQuickCheckIn = viewModel::sendQuickCheckIn,
                    onRefreshGpsLocation = viewModel::refreshGpsLocationAndSync,
                    onOpenFamilyChat = {
                        viewModel.navigateTo(ChildScreenId.C09_FAMILY_CHAT_TASKS)
                    }
                )

                ChildScreenId.C09_FAMILY_CHAT_TASKS -> C09FamilyChatTasksScreen(
                    uiState = uiState,
                    onSendMessage = viewModel::sendChatMessage,
                    onToggleTaskCompleted = viewModel::toggleTaskCompleted
                )

                ChildScreenId.C10_MY_ACTIVITY_WELLBEING -> C10MyActivityWellbeingScreen(
                    uiState = uiState,
                    onNavigate = viewModel::navigateTo
                )

                ChildScreenId.C11_PRIVACY_SHARING_DASHBOARD -> C11PrivacySharingDashboardScreen(
                    uiState = uiState,
                    onNavigate = viewModel::navigateTo,
                    onTriggerLiveConsentPreview = viewModel::requestIncomingLiveConsent
                )

                ChildScreenId.C12_PREMIUM_FILTERS_DISCLOSURE -> C12PremiumFiltersDisclosureScreen(
                    uiState = uiState,
                    systemMonitor = viewModel.systemMonitor,
                    onSubmitWebsiteAppeal = viewModel::submitWebsiteAppeal,
                    onPreviewLiveConsent = viewModel::requestIncomingLiveConsent
                )

                ChildScreenId.C13_INCOMING_LIVE_CONSENT -> C13IncomingLiveConsentScreen(
                    uiState = uiState,
                    onApproveSession = viewModel::approveIncomingLiveSession,
                    onDeclineSession = viewModel::declineIncomingLiveSession,
                    onSelectModality = viewModel::requestIncomingLiveConsent
                )

                ChildScreenId.C14_ACTIVE_LIVE_SESSION -> C14ActiveLiveSessionScreen(
                    uiState = uiState,
                    onStopImmediately = { viewModel.stopActiveLiveSessionImmediately() },
                    onToggleMuteAudio = viewModel::toggleLiveAudioMute,
                    onReturnToPrivacy = {
                        viewModel.navigateTo(ChildScreenId.C11_PRIVACY_SHARING_DASHBOARD)
                    }
                )

                ChildScreenId.C15_OFFLINE_SYNC_HELP -> C15OfflineSyncHelpScreen(
                    uiState = uiState,
                    onForceSyncNow = viewModel::flushOfflineQueueNow,
                    onToggleOfflineSimulation = viewModel::toggleSimulatedOfflineMode,
                    onNavigateToSos = {
                        viewModel.navigateTo(ChildScreenId.C08_SOS_EMERGENCY_CHECKIN)
                    }
                )
            }
        }
    }

    if (BuildConfig.DEBUG && showScreenAtlas) {
        StitchScreenAtlasModal(
            currentScreen = uiState.currentScreen,
            onSelectScreen = viewModel::navigateTo,
            onDismiss = { showScreenAtlas = false }
        )
    }

    if (showAccountModal) {
        ModalBottomSheet(
            onDismissRequest = { showAccountModal = false },
            containerColor = StitchSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Anchor Cloud & Parent Sync",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    SemanticStatusBadge(
                        status = if (uiState.currentUser != null) "CONNECTED" else "LOCAL PREVIEW"
                    )
                }

                Surface(
                    color = StitchSurfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Shared Firebase Project: gen-lang-client-0963224775",
                            style = MaterialTheme.typography.labelMedium,
                            color = StitchPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Household: ${uiState.household.familyName} (${uiState.household.familyId})\n" +
                                "Active Child: ${uiState.activeChild.name} (${uiState.activeChild.childId})\n" +
                                "Account: ${uiState.currentUser?.email ?:"Not signed in (Using offline/seeded household)"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (uiState.currentUser == null) {
                    Button(
                        onClick = {
                            viewModel.signInWithGoogle(context)
                            showAccountModal = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_google_sign_in_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sign in with Google")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            viewModel.signOut()
                            showAccountModal = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_sign_out_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sign Out")
                    }
                }

                OutlinedButton(
                    onClick = {
                        showAccountModal = false
                        viewModel.navigateTo(ChildScreenId.C01_WELCOME_PAIRING)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modal_open_pairing_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open Pairing Code Setup")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
