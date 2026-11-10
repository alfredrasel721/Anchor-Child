package com.example.service

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.data.local.AnchorChildLocalDatabase
import com.example.data.local.PairingSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Child-visible, explicitly enabled parental-control accessibility companion.
 *
 * Looks only at TYPE_WINDOW_STATE_CHANGED package names (never the view tree,
 * text, passwords, screenshots or keystrokes). A verified pairing plus cached
 * parent-synced app rules is required before any restriction takes effect.
 *
 * Uses Android's Home action for deterministic app-block/time-limit rules;
 * neither emergency calling nor OS permission/setting screens are blocked.
 * Consent/disclosure and Play Console accessibility declaration are required
 * before public distribution. This is not an anti-uninstall service.
 */
class AnchorAccessibilityService : AccessibilityService() {
    private val evaluationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pairingSessionStore by lazy { PairingSessionStore(applicationContext) }
    private val localDao by lazy {
        AnchorChildLocalDatabase.getInstance(applicationContext).childLocalDao()
    }
    private val systemMonitor by lazy { DeviceSystemMonitor(applicationContext) }
    private var lastEvaluatedAtMs: Long = 0L
    private var lastRestrictedAtMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        _serviceRunning.value = true
        Log.i(TAG, "Anchor parental-control accessibility companion enabled by the device user")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val targetPackage = event.packageName?.toString() ?: return
        _lastForegroundPackage.value = targetPackage

        if (isEssentialPackage(targetPackage)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastEvaluatedAtMs < 700L || now - lastRestrictedAtMs < 2000L) return
        lastEvaluatedAtMs = now

        evaluationScope.launch {
            try {
                // Never enforce a downloaded or stale rule outside the actively paired family.
                val pairing = pairingSessionStore.sessionFlow.first()
                if (!pairing.isPaired || pairing.familyId.isBlank() || pairing.childId.isBlank()) {
                    return@launch
                }
                val rule = localDao.getAppRulesForPairedChild(pairing.familyId, pairing.childId)
                    .firstOrNull { it.packageName == targetPackage }
                    ?: return@launch

                val usageMinutes = if (
                    rule.blocked || rule.status == "BLOCKED" || rule.whitelisted
                ) null else getTodayUsageMinutes(targetPackage)

                val reason = AppRestrictionPolicy.decide(
                    status = rule.status,
                    blockedByParent = rule.blocked,
                    whitelisted = rule.whitelisted,
                    scheduleOnly = rule.scheduleOnly,
                    dailyLimitMinutes = rule.dailyLimitMinutes,
                    measuredMinutesToday = usageMinutes
                )
                if (reason == AppRestrictionPolicy.Reason.NONE) return@launch

                withContext(Dispatchers.Main) {
                    if (_serviceRunning.value &&
                        _lastForegroundPackage.value == targetPackage &&
                        SystemClock.elapsedRealtime() - lastRestrictedAtMs >= 2000L
                    ) {
                        // Do not obscure the restriction: the device user can see
                        // why the rule is applied and request access in the Child app.
                        val explanation = when (reason) {
                            AppRestrictionPolicy.Reason.PARENT_BLOCK ->
                                "${rule.appName} is blocked by your family rule. Open Anchor Child to request access."
                            AppRestrictionPolicy.Reason.APP_TIME_LIMIT ->
                                "${rule.appName} has reached today's family time limit. Open Anchor Child to request extra time."
                            else -> return@withContext
                        }
                        if (performGlobalAction(GLOBAL_ACTION_HOME)) {
                            lastRestrictedAtMs = SystemClock.elapsedRealtime()
                            Toast.makeText(this@AnchorAccessibilityService, explanation, Toast.LENGTH_LONG).show()
                            Log.i(TAG, "Applied disclosed family app rule to $targetPackage")
                        }
                    }
                }
            } catch (error: Exception) {
                Log.w(TAG, "Failed to evaluate cached family rule", error)
            }
        }
    }

    private fun getTodayUsageMinutes(targetPackage: String): Int? {
        if (!systemMonitor.hasUsageStatsPermission()) return null
        return try {
            val manager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val usageMillis = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                System.currentTimeMillis()
            ).orEmpty()
                .filter { it.packageName == targetPackage }
                .sumOf { it.totalTimeInForeground.coerceAtLeast(0L) }
            (usageMillis / 60_000L).toInt()
        } catch (_: Exception) {
            null
        }
    }

    private fun isEssentialPackage(pkg: String): Boolean {
        if (pkg == packageName || pkg == "android") return true
        if (pkg.startsWith("com.android.emergency")) return true
        return pkg in setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.telecom",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.contacts",
            "com.google.android.contacts",
            "com.google.android.gms",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller"
        )
    }

    override fun onInterrupt() {
        Log.i(TAG, "Anchor accessibility companion interrupted")
    }

    override fun onDestroy() {
        _serviceRunning.value = false
        evaluationScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AnchorAccessService"
        private val _serviceRunning = MutableStateFlow(false)
        val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

        private val _lastForegroundPackage = MutableStateFlow("com.anchor.child")
        val lastForegroundPackage: StateFlow<String> = _lastForegroundPackage.asStateFlow()
    }
}
