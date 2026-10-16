package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChessLensOverlayService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _isOverlayRunning.value = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(isPaused = false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ANALYZE_NOW -> {
                // Request capture
                ChessLensAccessibilityService.instance?.captureScreen { bitmap ->
                    _lastCapturedBitmap.value = bitmap
                }
            }
            ACTION_PAUSE -> {
                _isPaused.value = true
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(isPaused = true))
            }
            ACTION_RESUME -> {
                _isPaused.value = false
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(isPaused = false))
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        _isOverlayRunning.value = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val analyzeIntent = Intent(this, ChessLensOverlayService::class.java).apply {
            action = ACTION_ANALYZE_NOW
        }
        val analyzePendingIntent = PendingIntent.getService(
            this, 1, analyzeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionIntent = Intent(this, ChessLensOverlayService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 2, toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.chesslens_icon)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(if (isPaused) "Analysis paused" else getString(R.string.notification_text))
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.chesslens_icon,
                getString(R.string.action_analyze_now),
                analyzePendingIntent
            )
            .addAction(
                R.drawable.chesslens_icon,
                if (isPaused) getString(R.string.action_resume) else getString(R.string.action_pause),
                togglePendingIntent
            )
            .build()
    }

    companion object {
        const val CHANNEL_ID = "chesslens_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_ANALYZE_NOW = "com.example.chesslens.ACTION_ANALYZE_NOW"
        const val ACTION_PAUSE = "com.example.chesslens.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.chesslens.ACTION_RESUME"
        const val ACTION_STOP = "com.example.chesslens.ACTION_STOP"

        private val _isOverlayRunning = MutableStateFlow(false)
        val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

        private val _lastCapturedBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
        val lastCapturedBitmap: StateFlow<android.graphics.Bitmap?> = _lastCapturedBitmap.asStateFlow()
    }
}
