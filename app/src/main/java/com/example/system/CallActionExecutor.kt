package com.example.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.domain.model.ActionExecutionResult
import com.example.domain.model.CallCommandIntent
import com.example.domain.model.CallLifecycleState
import com.example.domain.model.CommandSource
import com.example.domain.model.SimulationCallState

/**
 * Executes real Android Telecom & AudioManager actions when permitted, and cleanly reports
 * when Android API level, permissions, or OEM restrictions prevent an operation.
 * Never falsely claims success.
 */
class CallActionExecutor(private val context: Context) {

    private val telecomManager: TelecomManager? =
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @Volatile
    var pendingCallScreeningRejectHandler: (() -> Boolean)? = null

    fun execute(
        intent: CallCommandIntent,
        callState: CallLifecycleState,
        source: CommandSource,
        currentSimState: SimulationCallState,
        onUpdateSimState: (SimulationCallState) -> Unit
    ): ActionExecutionResult {
        // Test mode or active simulated call executes against simulated call engine + real AudioManager preview where harmless
        if (source == CommandSource.TEST_MODE ||
            callState == CallLifecycleState.SIMULATED_RINGING ||
            callState == CallLifecycleState.SIMULATED_ACTIVE
        ) {
            return executeSimulated(intent, currentSimState, onUpdateSimState)
        }

        return when (intent) {
            CallCommandIntent.ANSWER_CALL -> answerRealCall(callState)
            CallCommandIntent.DO_NOT_ANSWER -> ActionExecutionResult.Ignored(
                "Call left unanswered as requested. Baby stopped prompting for this ring."
            )
            CallCommandIntent.REJECT_CALL -> rejectRealCall(callState)
            CallCommandIntent.SPEAKER_ON -> setSpeakerphone(enabled = true)
            CallCommandIntent.SPEAKER_OFF -> setSpeakerphone(enabled = false)
            CallCommandIntent.MUTE_MIC -> setMicMute(muted = true)
            CallCommandIntent.UNMUTE_MIC -> setMicMute(muted = false)
            CallCommandIntent.CONFIRM_YES,
            CallCommandIntent.CONFIRM_NO,
            CallCommandIntent.UNKNOWN -> ActionExecutionResult.Ignored("No call action executed.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun answerRealCall(callState: CallLifecycleState): ActionExecutionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return ActionExecutionResult.Unsupported(
                "TelecomManager.acceptRingingCall() requires Android 8.0 (API 26) or higher."
            )
        }
        if (!hasPermission(Manifest.permission.ANSWER_PHONE_CALLS)) {
            return ActionExecutionResult.PermissionRequired(
                Manifest.permission.ANSWER_PHONE_CALLS,
                "ANSWER_PHONE_CALLS permission is required to answer incoming calls."
            )
        }
        if (callState != CallLifecycleState.RINGING) {
            return ActionExecutionResult.Failed(
                "No live incoming call is currently ringing. Use Test Lab to simulate a call."
            )
        }
        val tm = telecomManager ?: return ActionExecutionResult.Unsupported(
            "TelecomManager service is unavailable on this device."
        )
        return try {
            @Suppress("DEPRECATION")
            tm.acceptRingingCall()
            ActionExecutionResult.Success("Accepted ringing call via Android TelecomManager.")
        } catch (se: SecurityException) {
            ActionExecutionResult.PermissionRequired(
                Manifest.permission.ANSWER_PHONE_CALLS,
                "Android blocked acceptRingingCall(): ${se.localizedMessage ?: "SecurityException"}"
            )
        } catch (e: Exception) {
            ActionExecutionResult.Failed(
                "Could not answer call: ${e.localizedMessage ?: e::class.java.simpleName}"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun rejectRealCall(callState: CallLifecycleState): ActionExecutionResult {
        if (callState != CallLifecycleState.RINGING && callState != CallLifecycleState.OFFHOOK) {
            return ActionExecutionResult.Failed(
                "No live call is active or ringing to reject. Use Test Lab to simulate a call."
            )
        }

        // First attempt CallScreeningService rejection if the call is still inside the screening window
        val screeningHandler = pendingCallScreeningRejectHandler
        if (screeningHandler != null && screeningHandler.invoke()) {
            pendingCallScreeningRejectHandler = null
            return ActionExecutionResult.Success("Call rejected via Android CallScreeningService.")
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return ActionExecutionResult.Unsupported(
                "TelecomManager.endCall() requires Android 9.0 (API 28) or higher."
            )
        }
        if (!hasPermission(Manifest.permission.ANSWER_PHONE_CALLS)) {
            return ActionExecutionResult.PermissionRequired(
                Manifest.permission.ANSWER_PHONE_CALLS,
                "ANSWER_PHONE_CALLS permission or Call Screening role is required to reject calls."
            )
        }
        val tm = telecomManager ?: return ActionExecutionResult.Unsupported(
            "TelecomManager service is unavailable on this device."
        )
        return try {
            @Suppress("DEPRECATION")
            val ended = tm.endCall()
            if (ended) {
                ActionExecutionResult.Success("Call rejected via Android TelecomManager.endCall().")
            } else {
                ActionExecutionResult.Failed(
                    "Android TelecomManager.endCall() returned false — this device/carrier or Android version restricted ending the call."
                )
            }
        } catch (se: SecurityException) {
            ActionExecutionResult.PermissionRequired(
                Manifest.permission.ANSWER_PHONE_CALLS,
                "Android blocked endCall(): ${se.localizedMessage ?: "SecurityException"}"
            )
        } catch (e: Exception) {
            ActionExecutionResult.Failed(
                "Failed to reject call: ${e.localizedMessage ?: e::class.java.simpleName}"
            )
        }
    }

    private fun setSpeakerphone(enabled: Boolean): ActionExecutionResult {
        if (!hasPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)) {
            return ActionExecutionResult.PermissionRequired(
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                "MODIFY_AUDIO_SETTINGS permission is required to control speaker routing."
            )
        }
        val am = audioManager ?: return ActionExecutionResult.Unsupported(
            "AudioManager is unavailable on this device."
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (enabled) {
                    val speakerDevice = am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speakerDevice != null) {
                        val ok = am.setCommunicationDevice(speakerDevice)
                        if (ok) {
                            ActionExecutionResult.Success("Audio routed to Built-in Speaker (API 31+ CommunicationDevice).")
                        } else {
                            ActionExecutionResult.Failed("Android declined speaker routing (no active communication audio session).")
                        }
                    } else {
                        ActionExecutionResult.Unsupported("No built-in speaker communication device reported by AudioManager.")
                    }
                } else {
                    am.clearCommunicationDevice()
                    ActionExecutionResult.Success("Cleared speaker communication device routing (back to earpiece/default).")
                }
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = enabled
                ActionExecutionResult.Success(
                    if (enabled) "Speakerphone turned ON." else "Speakerphone turned OFF."
                )
            }
        } catch (e: Exception) {
            ActionExecutionResult.Failed("Speaker routing failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun setMicMute(muted: Boolean): ActionExecutionResult {
        if (!hasPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)) {
            return ActionExecutionResult.PermissionRequired(
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                "MODIFY_AUDIO_SETTINGS permission is required to toggle microphone mute."
            )
        }
        val am = audioManager ?: return ActionExecutionResult.Unsupported(
            "AudioManager is unavailable on this device."
        )
        return try {
            am.isMicrophoneMute = muted
            val verified = am.isMicrophoneMute == muted
            if (verified) {
                ActionExecutionResult.Success(
                    if (muted) {
                        "System microphone muted via AudioManager. (Note: Some cellular modems only allow mute inside the active dialer UI.)"
                    } else {
                        "System microphone unmuted via AudioManager."
                    }
                )
            } else {
                ActionExecutionResult.Failed(
                    "Android/OEM ignored isMicrophoneMute=$muted outside the system dialer."
                )
            }
        } catch (e: Exception) {
            ActionExecutionResult.Failed("Microphone mute failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun executeSimulated(
        intent: CallCommandIntent,
        sim: SimulationCallState,
        onUpdateSimState: (SimulationCallState) -> Unit
    ): ActionExecutionResult {
        return when (intent) {
            CallCommandIntent.ANSWER_CALL -> {
                if (!sim.isRinging && !sim.isConnected) {
                    onUpdateSimState(
                        sim.copy(
                            isRinging = false,
                            isConnected = true,
                            isRejected = false,
                            isIgnored = false,
                            statusNote = "Simulated call answered via voice command."
                        )
                    )
                } else {
                    onUpdateSimState(
                        sim.copy(
                            isRinging = false,
                            isConnected = true,
                            isRejected = false,
                            isIgnored = false,
                            statusNote = "Simulated call connected (${sim.callerLabel})."
                        )
                    )
                }
                ActionExecutionResult.Success("[Test Mode] Call answered. Spoken feedback played.")
            }

            CallCommandIntent.DO_NOT_ANSWER -> {
                onUpdateSimState(
                    sim.copy(
                        isRinging = false,
                        isConnected = false,
                        isIgnored = true,
                        isRejected = false,
                        statusNote = "Simulated call left unanswered (Do Not Answer)."
                    )
                )
                ActionExecutionResult.Ignored("[Test Mode] Call was NOT answered as instructed.")
            }

            CallCommandIntent.REJECT_CALL -> {
                onUpdateSimState(
                    sim.copy(
                        isRinging = false,
                        isConnected = false,
                        isRejected = true,
                        isIgnored = false,
                        statusNote = "Simulated call rejected."
                    )
                )
                ActionExecutionResult.Success("[Test Mode] Call rejected cleanly.")
            }

            CallCommandIntent.SPEAKER_ON -> {
                onUpdateSimState(
                    sim.copy(
                        isSpeakerOn = true,
                        statusNote = "Simulated Speakerphone ON."
                    )
                )
                ActionExecutionResult.Success("[Test Mode] Speakerphone turned ON.")
            }

            CallCommandIntent.SPEAKER_OFF -> {
                onUpdateSimState(
                    sim.copy(
                        isSpeakerOn = false,
                        statusNote = "Simulated Speakerphone OFF (earpiece)."
                    )
                )
                ActionExecutionResult.Success("[Test Mode] Speakerphone turned OFF.")
            }

            CallCommandIntent.MUTE_MIC -> {
                onUpdateSimState(
                    sim.copy(
                        isMuted = true,
                        statusNote = "Simulated Microphone MUTED."
                    )
                )
                ActionExecutionResult.Success("[Test Mode] Microphone muted.")
            }

            CallCommandIntent.UNMUTE_MIC -> {
                onUpdateSimState(
                    sim.copy(
                        isMuted = false,
                        statusNote = "Simulated Microphone UNMUTED."
                    )
                )
                ActionExecutionResult.Success("[Test Mode] Microphone unmuted.")
            }

            CallCommandIntent.CONFIRM_YES,
            CallCommandIntent.CONFIRM_NO,
            CallCommandIntent.UNKNOWN -> ActionExecutionResult.Ignored("[Test Mode] No call action taken.")
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
