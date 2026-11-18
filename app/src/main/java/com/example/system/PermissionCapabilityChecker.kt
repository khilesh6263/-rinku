package com.example.system

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.domain.model.DeviceCapabilityReport
import java.util.Locale

class PermissionCapabilityChecker(private val context: Context) {

    fun inspect(): DeviceCapabilityReport {
        val api = Build.VERSION.SDK_INT
        val hasAudio = hasPermission(Manifest.permission.RECORD_AUDIO)
        val hasPhoneState = hasPermission(Manifest.permission.READ_PHONE_STATE)
        val hasAnswerCalls = if (api >= Build.VERSION_CODES.O) {
            hasPermission(Manifest.permission.ANSWER_PHONE_CALLS)
        } else {
            false
        }
        val hasNotifications = if (api >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        val hasModifyAudio = hasPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)

        val roleStatus = checkCallScreeningRole()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val oemNote = buildOemRestrictionNote(manufacturer, api)

        return DeviceCapabilityReport(
            androidVersionLabel = "Android ${Build.VERSION.RELEASE} (API $api)",
            apiLevel = api,
            manufacturer = manufacturer.replaceFirstChar { it.uppercase(Locale.ROOT) },
            model = Build.MODEL.orEmpty(),
            hasRecordAudio = hasAudio,
            hasReadPhoneState = hasPhoneState,
            hasAnswerPhoneCalls = hasAnswerCalls,
            hasPostNotifications = hasNotifications,
            hasModifyAudioSettings = hasModifyAudio,
            isCallScreeningRoleSupported = roleStatus.first,
            isCallScreeningRoleHeld = roleStatus.second,
            isSpeechRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context),
            supportsAcceptRingingCallApi = api >= Build.VERSION_CODES.O,
            supportsEndCallApi = api >= Build.VERSION_CODES.P,
            supportsModernAudioRoutingApi = api >= Build.VERSION_CODES.S,
            oemRestrictionNote = oemNote
        )
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun createCallScreeningRoleIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) return null
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun checkCallScreeningRole(): Pair<Boolean, Boolean> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false to false
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false to false
        val available = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
        val held = available && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        return available to held
    }

    private fun buildOemRestrictionNote(manufacturer: String, api: Int): String? {
        val m = manufacturer.lowercase(Locale.ROOT)
        val oemHint = when {
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ->
                "MIUI/HyperOS devices may restrict background microphone & auto-start. Enable Autostart & 'No restrictions' battery saver for Baby."
            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ->
                "ColorOS/OxygenOS may pause background services. Allow background activity in App Battery Usage."
            m.contains("vivo") || m.contains("iqoo") ->
                "FuntouchOS/OriginOS restricts background call listeners unless High Background Power Consumption is allowed."
            m.contains("samsung") ->
                "On OneUI, exclude Baby from Sleeping Apps so incoming call detection stays responsive."
            else -> null
        }

        val android14Note = if (api >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            "Android 14+ requires starting the Voice Monitor from inside the app before background incoming calls can use the microphone FGS."
        } else {
            null
        }

        return listOfNotNull(android14Note, oemHint).joinToString(" ").ifBlank { null }
    }
}
