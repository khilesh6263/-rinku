package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.BabyAssistantApp
import com.example.MainActivity
import com.example.R
import com.example.domain.model.CallLifecycleState

class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val newState = when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> CallLifecycleState.RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> CallLifecycleState.OFFHOOK
            else -> CallLifecycleState.IDLE
        }

        val app = context.applicationContext as? BabyAssistantApp ?: return
        val coordinator = app.container.coordinator
        val fgsError = coordinator.onLiveCallStateChanged(newState)

        // If Android 14+ restricted background FGS start when incoming call arrived,
        // post a high-priority tap notification as a safe fallback.
        if (newState == CallLifecycleState.RINGING && fgsError != null) {
            postIncomingCallPromptNotification(context)
        }
    }

    private fun postIncomingCallPromptNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CallAssistantForegroundService.CHANNEL_CALL_ALERT_ID,
                context.getString(R.string.notification_channel_call_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val tapIntent = PendingIntent.getActivity(
            context,
            101,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_START_VOICE_LISTEN, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CallAssistantForegroundService.CHANNEL_CALL_ALERT_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Incoming Call — Tap for Baby Voice Control")
            .setContentText("Android paused background mic monitoring. Tap to speak a command.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapIntent)
            .build()

        nm.notify(CallAssistantForegroundService.CALL_ALERT_NOTIFICATION_ID, notification)
    }

    companion object {
        const val EXTRA_START_VOICE_LISTEN = "extra_start_voice_listen"
    }
}
