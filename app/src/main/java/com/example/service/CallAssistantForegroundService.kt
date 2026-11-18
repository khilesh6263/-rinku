package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.BabyAssistantApp
import com.example.MainActivity
import com.example.R
import com.example.domain.model.CallLifecycleState
import com.example.domain.model.CommandSource

class CallAssistantForegroundService : Service() {

    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: Any? = null
    private var legacyPhoneStateListener: Any? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val coordinator = (application as BabyAssistantApp).container.coordinator

        when (intent?.action) {
            ACTION_STOP_MONITOR -> {
                coordinator.setForegroundMonitorActive(false)
                coordinator.speechManager.stopListening()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_LISTEN_NOW -> {
                startForegroundCompat()
                coordinator.setForegroundMonitorActive(true)
                coordinator.startManualVoiceCommand(CommandSource.LIVE_CALL)
                return START_STICKY
            }

            else -> {
                val started = startForegroundCompat()
                if (!started) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                coordinator.setForegroundMonitorActive(true)
                registerCallStateListener()
                return START_STICKY
            }
        }
    }

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        val coordinator = (application as? BabyAssistantApp)?.container?.coordinator
        coordinator?.setForegroundMonitorActive(false)
        coordinator?.speechManager?.stopListening()
        stopSelf()
    }

    override fun onDestroy() {
        unregisterCallStateListener()
        (application as? BabyAssistantApp)?.container?.coordinator?.setForegroundMonitorActive(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(): Boolean {
        val notification = buildMonitorNotification()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val hasRecordAudio = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasRecordAudio) {
                    Log.w(TAG, "Cannot start microphone FGS without RECORD_AUDIO")
                    return false
                }
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            false
        }
    }

    private fun registerCallStateListener() {
        val tm = telephonyManager ?: return
        val hasPhoneState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPhoneState) return

        val coordinator = (application as BabyAssistantApp).container.coordinator

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (telephonyCallback == null) {
                    val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            coordinator.onLiveCallStateChanged(mapCallState(state))
                        }
                    }
                    tm.registerTelephonyCallback(mainExecutor, cb)
                    telephonyCallback = cb
                }
            } else {
                if (legacyPhoneStateListener == null) {
                    @Suppress("DEPRECATION")
                    val listener = object : PhoneStateListener() {
                        @Deprecated("Deprecated in Java")
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                            coordinator.onLiveCallStateChanged(mapCallState(state))
                        }
                    }
                    @Suppress("DEPRECATION")
                    tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                    legacyPhoneStateListener = listener
                }
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException registering call state listener", se)
        }
    }

    private fun unregisterCallStateListener() {
        val tm = telephonyManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let { tm.unregisterTelephonyCallback(it) }
                telephonyCallback = null
            } else {
                @Suppress("DEPRECATION")
                (legacyPhoneStateListener as? PhoneStateListener)?.let {
                    tm.listen(it, PhoneStateListener.LISTEN_NONE)
                }
                legacyPhoneStateListener = null
            }
        } catch (_: Exception) {
        }
    }

    private fun mapCallState(state: Int): CallLifecycleState = when (state) {
        TelephonyManager.CALL_STATE_RINGING -> CallLifecycleState.RINGING
        TelephonyManager.CALL_STATE_OFFHOOK -> CallLifecycleState.OFFHOOK
        else -> CallLifecycleState.IDLE
    }

    private fun buildMonitorNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val listenIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CallAssistantForegroundService::class.java).apply {
                action = ACTION_LISTEN_NOW
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, CallAssistantForegroundService::class.java).apply {
                action = ACTION_STOP_MONITOR
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_MONITOR_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Baby Call Assistant Active")
            .setContentText("Ready for incoming calls — mic activates only when a call rings.")
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.action_listen_now), listenIntent)
            .addAction(0, getString(R.string.action_stop_assistant), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_ID,
                getString(R.string.notification_channel_assistant_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_assistant_desc)
            }
            val alertChannel = NotificationChannel(
                CHANNEL_CALL_ALERT_ID,
                getString(R.string.notification_channel_call_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_call_alert_desc)
            }
            nm.createNotificationChannel(monitorChannel)
            nm.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        private const val TAG = "CallAssistantFGS"
        const val CHANNEL_MONITOR_ID = "baby_monitor_channel"
        const val CHANNEL_CALL_ALERT_ID = "baby_call_alert_channel"
        const val NOTIFICATION_ID = 4201
        const val CALL_ALERT_NOTIFICATION_ID = 4202
        const val ACTION_START_MONITOR = "com.example.action.START_MONITOR"
        const val ACTION_STOP_MONITOR = "com.example.action.STOP_MONITOR"
        const val ACTION_LISTEN_NOW = "com.example.action.LISTEN_NOW"
    }
}
