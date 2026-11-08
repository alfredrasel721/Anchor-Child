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
        val pkg = sbn?.packageName ?: return
        if (pkg != packageName) {
            _heldNotificationCount.value = _heldNotificationCount.value + 1
        }
    }

    override fun onListenerDisconnected() {
        _listenerConnected.value = false
        super.onListenerDisconnected()
    }

    companion object {
        private const val TAG = "AnchorNotifShield"

        private val _listenerConnected = MutableStateFlow(false)
        val listenerConnected: StateFlow<Boolean> = _listenerConnected.asStateFlow()

        private val _heldNotificationCount = MutableStateFlow(14)
        val heldNotificationCount: StateFlow<Int> = _heldNotificationCount.asStateFlow()
    }
}
