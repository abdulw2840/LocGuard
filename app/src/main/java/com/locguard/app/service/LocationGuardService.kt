package com.locguard.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.locguard.app.LocGuardApp
import com.locguard.app.MainActivity
import com.locguard.app.R
import com.locguard.app.monitor.GuardPhase
import com.locguard.app.monitor.GuardRuntimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LocationGuardService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var notificationManager: NotificationManager

    private val systemEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val engine = LocGuardApp.instance.guardEngine
            when (intent?.action) {
                LocationManager.MODE_CHANGED_ACTION -> {
                    val enabled = intent.getBooleanExtra(
                        LocationManager.EXTRA_LOCATION_ENABLED,
                        LocGuardApp.instance.privilegeManager.isLocationEnabledSystem()
                    )
                    engine.onLocationModeChanged(enabled)
                }
                Intent.ACTION_SCREEN_OFF -> engine.onScreenStateChanged(false)
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> engine.onScreenStateChanged(true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()

        val initialNotif = buildNotification(GuardRuntimeState(), showCountdown = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                initialNotif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotif)
        }

        val filter = IntentFilter().apply {
            addAction(LocationManager.MODE_CHANGED_ACTION)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(systemEventReceiver, filter)

        val app = LocGuardApp.instance
        serviceScope.launch {
            app.privilegeManager.refreshStatus()
            app.guardEngine.start()
        }

        serviceScope.launch {
            combine(
                app.guardEngine.state,
                app.preferences.settingsFlow
            ) { runtime, settings ->
                runtime to settings.showCountdownNotification
            }.collect { (runtime, showCountdown) ->
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(runtime, showCountdown)
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = LocGuardApp.instance.guardEngine
        when (intent?.action) {
            ACTION_TURN_OFF_NOW -> engine.turnOffNow()
            ACTION_EXTEND_TIMER -> engine.extendTimer(60)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(systemEventReceiver) }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LocGuard Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Battery-efficient location guard status and countdown"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(
        state: GuardRuntimeState,
        showCountdown: Boolean
    ): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val (title, text) = when (state.phase) {
            GuardPhase.IDLE_LOCATION_OFF ->
                "Location Off • Protected" to "Zero background drain"
            GuardPhase.PROTECTED_WHITELISTED ->
                "Whitelisted App Active" to (state.currentForegroundApp ?: "Location kept ON")
            GuardPhase.COUNTING_DOWN -> {
                val label = if (showCountdown) "${state.remainingSeconds}s remaining" else "Auto-off armed"
                "Location Active • $label" to "Will disable automatically"
            }
            GuardPhase.SCREEN_OFF_GRACE ->
                "Screen Off • Closing in ${state.remainingSeconds}s" to "Battery saver shutoff"
            GuardPhase.PAUSED ->
                "LocGuard Paused" to "Tap to resume protection"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location_shield)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)

        if (state.isLocationEnabled) {
            val turnOffIntent = PendingIntent.getService(
                this,
                1,
                Intent(this, LocationGuardService::class.java).setAction(ACTION_TURN_OFF_NOW),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "Turn Off Now", turnOffIntent)

            if (state.phase == GuardPhase.COUNTING_DOWN) {
                val extendIntent = PendingIntent.getService(
                    this,
                    2,
                    Intent(this, LocationGuardService::class.java).setAction(ACTION_EXTEND_TIMER),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(0, "+1m", extendIntent)
            }
        }

        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "locguard_status_channel"
        const val NOTIFICATION_ID = 1042
        const val ACTION_TURN_OFF_NOW = "com.locguard.app.ACTION_TURN_OFF_NOW"
        const val ACTION_EXTEND_TIMER = "com.locguard.app.ACTION_EXTEND_TIMER"

        fun start(context: Context) {
            val intent = Intent(context, LocationGuardService::class.java)
            context.startForegroundService(intent)
        }
    }
}
