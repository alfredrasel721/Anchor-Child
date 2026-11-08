package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Optional Device Administration Receiver for tamper protection and bedtime lock enforcement.
 */
class AnchorDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "AnchorDeviceAdminReceiver enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "AnchorDeviceAdminReceiver disabled")
    }

    companion object {
        private const val TAG = "AnchorDeviceAdmin"
    }
}
