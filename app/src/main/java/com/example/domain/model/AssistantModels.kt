package com.example.domain.model

enum class AssistantLanguage(
    val displayName: String,
    val nativeLabel: String,
    val recognitionLocale: String,
    val ttsLanguageTag: String
) {
    HINGLISH(
        displayName = "Hinglish",
        nativeLabel = "Hinglish (Hindi + English)",
        recognitionLocale = "en-IN",
        ttsLanguageTag = "hi-IN"
    ),
    HINDI(
        displayName = "Hindi",
        nativeLabel = "हिन्दी (Hindi)",
        recognitionLocale = "hi-IN",
        ttsLanguageTag = "hi-IN"
    ),
    ENGLISH(
        displayName = "English",
        nativeLabel = "English (India / Global)",
        recognitionLocale = "en-IN",
        ttsLanguageTag = "en-IN"
    )
}

enum class CallCommandIntent(val label: String, val isDestructiveOrCallAltering: Boolean) {
    ANSWER_CALL("Answer Call", true),
    DO_NOT_ANSWER("Do Not Answer", false),
    REJECT_CALL("Reject Call", true),
    SPEAKER_ON("Speaker ON", false),
    SPEAKER_OFF("Speaker OFF", false),
    MUTE_MIC("Mute Mic", false),
    UNMUTE_MIC("Unmute Mic", false),
    CONFIRM_YES("Confirm Action", false),
    CONFIRM_NO("Cancel Action", false),
    UNKNOWN("Unrecognized", false)
}

enum class CallLifecycleState(val label: String) {
    IDLE("No Active Call"),
    RINGING("Incoming Call Ringing"),
    OFFHOOK("Call In Progress"),
    SIMULATED_RINGING("Simulated Incoming Call"),
    SIMULATED_ACTIVE("Simulated Active Call")
}

enum class CommandSource(val label: String) {
    LIVE_CALL("Live Call"),
    MANUAL_MIC("Manual Mic"),
    TEST_MODE("Test Mode")
}

data class ClassificationResult(
    val rawTranscript: String,
    val normalizedTranscript: String,
    val intent: CallCommandIntent,
    val confidence: Float,
    val wakeWordDetected: Boolean,
    val needsConfirmation: Boolean,
    val isDuplicate: Boolean = false,
    val isNoiseFiltered: Boolean = false,
    val reason: String = ""
)

sealed interface ActionExecutionResult {
    val summary: String

    data class Success(override val summary: String) : ActionExecutionResult
    data class Ignored(override val summary: String) : ActionExecutionResult
    data class PermissionRequired(val permissionName: String, override val summary: String) : ActionExecutionResult
    data class Unsupported(override val summary: String) : ActionExecutionResult
    data class Failed(override val summary: String) : ActionExecutionResult
}

data class AssistantSettings(
    val isAssistantEnabled: Boolean = false,
    val language: AssistantLanguage = AssistantLanguage.HINGLISH,
    val requireWakeWord: Boolean = true,
    val confirmUncertainActions: Boolean = true,
    val voiceFeedbackEnabled: Boolean = true,
    val autoStartMicOnIncomingCall: Boolean = true,
    val continuousRingListening: Boolean = true,
    val noiseSensitivity: Float = 0.35f,
    val preferOfflineRecognition: Boolean = false,
    val hasCompletedSetupWizard: Boolean = false
)

data class PendingConfirmation(
    val originalTranscript: String,
    val proposedIntent: CallCommandIntent,
    val source: CommandSource,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class DeviceCapabilityReport(
    val androidVersionLabel: String,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val hasRecordAudio: Boolean,
    val hasReadPhoneState: Boolean,
    val hasAnswerPhoneCalls: Boolean,
    val hasPostNotifications: Boolean,
    val hasModifyAudioSettings: Boolean,
    val isCallScreeningRoleSupported: Boolean,
    val isCallScreeningRoleHeld: Boolean,
    val isSpeechRecognitionAvailable: Boolean,
    val supportsAcceptRingingCallApi: Boolean,
    val supportsEndCallApi: Boolean,
    val supportsModernAudioRoutingApi: Boolean,
    val oemRestrictionNote: String?
) {
    val allCorePermissionsGranted: Boolean
        get() = hasRecordAudio && hasReadPhoneState && hasAnswerPhoneCalls && hasPostNotifications
}

data class SimulationCallState(
    val isRinging: Boolean = false,
    val isConnected: Boolean = false,
    val isRejected: Boolean = false,
    val isIgnored: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isMuted: Boolean = false,
    val callerLabel: String = "Mom (+91 98••• ••210)",
    val statusNote: String = "Tap 'Simulate Incoming Call' to test hands-free commands without a cellular call."
)
