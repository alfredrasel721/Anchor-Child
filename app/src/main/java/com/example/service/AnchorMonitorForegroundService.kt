package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R

/**
 * Persistent foreground notification service used when active safety location check-ins
 * or child-approved live sessions (Screen C-14) are running, ensuring mandatory
 * Android transparency indicators at all times.
 */
class AnchorMonitorForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Anchor Child Safety Companion Active"
        val subtitle = intent?.getStringExtra(EXTRA_SUBTITLE)
            ?: "Family schedule & safe-zone sync active • Tap to inspect privacy"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            // In unit test or restricted background environments, ignore startForeground restriction
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anchor Safety & Transparency Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays active family schedule sync or live session indicators."
            }
            mgr?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "anchor_child_status_channel"
        const val NOTIFICATION_ID = 4102
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
    }
}
