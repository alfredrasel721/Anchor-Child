package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Optional Child Companion Accessibility Service.
 * Detects foreground package changes so Anchor Child can display a friendly
 * restriction reason & next unlock window (Screen C-05) when a capped or downtime-restricted
 * app is opened. Never reads private text content, messages, or keystrokes.
 */
class AnchorAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        _serviceRunning.value = true
        Log.i(TAG, "AnchorAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            _lastForegroundPackage.value = pkg
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AnchorAccessibilityService interrupted")
    }

    override fun onDestroy() {
        _serviceRunning.value = false
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
