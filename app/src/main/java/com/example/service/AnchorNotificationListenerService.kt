package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Optional Focus Notification Shield (Premium Feature).
 * Holds non-urgent social/game notifications during School Focus & Bedtime windows.
 * Never uploads private notification message contents to parents.
 */
class AnchorNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _listenerConnected.value = true
        Log.i(TAG, "AnchorNotificationListenerService connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Observing a notification does NOT mean it has been suppressed.
        // Never claim a notification was "held" without an authorized,
        // entitlement-checked filtering action. Notification contents are
        // not inspected or forwarded to the Parent device.
        if (sbn?.packageName == null) return
    }

    override fun onListenerDisconnected() {
        _listenerConnected.value = false
        super.onListenerDisconnected()
    }

    companion object {
        private const val TAG = "AnchorNotifShield"

        private val _listenerConnected = MutableStateFlow(false)
        val listenerConnected: StateFlow<Boolean> = _listenerConnected.asStateFlow()

        private val _heldNotificationCount = MutableStateFlow(0)
        val heldNotificationCount: StateFlow<Int> = _heldNotificationCount.asStateFlow()
    }
}
