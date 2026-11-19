package com.example.system

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AssistantPreferencesRepository
import com.example.data.local.CommandHistoryRepository
import com.example.domain.model.ActionExecutionResult
import com.example.domain.model.AssistantLanguage
import com.example.domain.model.AssistantSettings
import com.example.domain.model.CallCommandIntent
import com.example.domain.model.CallLifecycleState
import com.example.domain.model.ClassificationResult
import com.example.domain.model.CommandSource
import com.example.domain.model.DeviceCapabilityReport
import com.example.domain.model.PendingConfirmation
import com.example.domain.model.SimulationCallState
import com.example.domain.voice.VoiceCommandClassifier
import com.example.domain.voice.VoiceFeedbackMessages
import com.example.service.CallAssistantForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallSessionCoordinator(
    private val appContext: Context,
    private val preferencesRepository: AssistantPreferencesRepository,
    private val historyRepository: CommandHistoryRepository,
    val capabilityChecker: PermissionCapabilityChecker,
    val ttsManager: BabyTtsManager,
    val speechManager: BabySpeechRecognizerManager,
    val actionExecutor: CallActionExecutor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val classifier = VoiceCommandClassifier()

    private val _settings = MutableStateFlow(AssistantSettings())
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    private val _callState = MutableStateFlow(CallLifecycleState.IDLE)
    val callState: StateFlow<CallLifecycleState> = _callState.asStateFlow()

    private val _callerDisplay = MutableStateFlow<String?>(null)
    val callerDisplay: StateFlow<String?> = _callerDisplay.asStateFlow()

    private val _simulationState = MutableStateFlow(SimulationCallState())
    val simulationState: StateFlow<SimulationCallState> = _simulationState.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _lastClassification = MutableStateFlow<ClassificationResult?>(null)
    val lastClassification: StateFlow<ClassificationResult?> = _lastClassification.asStateFlow()

    private val _lastOutcomeBanner = MutableStateFlow<String?>(null)
    val lastOutcomeBanner: StateFlow<String?> = _lastOutcomeBanner.asStateFlow()

    private val _isForegroundMonitorActive = MutableStateFlow(false)
    val isForegroundMonitorActive: StateFlow<Boolean> = _isForegroundMonitorActive.asStateFlow()

    private val _capabilityReport = MutableStateFlow(capabilityChecker.inspect())
    val capabilityReport: StateFlow<DeviceCapabilityReport> = _capabilityReport.asStateFlow()

    private var ignoredCurrentRing = false
    private var ringListenAttempt = 0
    private var ringRetryJob: Job? = null

    init {
        scope.launch {
            preferencesRepository.settingsFlow.collect { latest ->
                _settings.value = latest
            }
        }
    }

    fun refreshCapabilities() {
        _capabilityReport.value = capabilityChecker.inspect()
    }

    fun setForegroundMonitorActive(active: Boolean) {
        _isForegroundMonitorActive.value = active
    }

    fun ensureMonitorRunningIfEnabled() {
        val report = capabilityChecker.inspect()
        _capabilityReport.value = report
        if (_settings.value.isAssistantEnabled && report.hasRecordAudio && !_isForegroundMonitorActive.value) {
            startMonitorServiceSafely(fromForegroundUi = true)
        }
    }

    fun toggleAssistantFromUi(enabled: Boolean): String? {
        _settings.value = _settings.value.copy(isAssistantEnabled = enabled)
        scope.launch {
            preferencesRepository.setAssistantEnabled(enabled)
        }
        return if (enabled) {
            val report = capabilityChecker.inspect()
            _capabilityReport.value = report
            if (!report.hasRecordAudio) {
                "Grant Microphone permission in the Setup Wizard so Baby can listen automatically during incoming calls."
            } else {
                startMonitorServiceSafely(fromForegroundUi = true)
            }
        } else {
            ringRetryJob?.cancel()
            speechManager.cancelAndDestroy()
            stopMonitorService()
            null
        }
    }

    fun startMonitorServiceSafely(
        fromForegroundUi: Boolean,
        action: String = CallAssistantForegroundService.ACTION_START_MONITOR
    ): String? {
        val intent = Intent(appContext, CallAssistantForegroundService::class.java).apply {
            this.action = action
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Unable to start foreground service (fromForegroundUi=$fromForegroundUi)", e)
            "Android restricted starting the microphone service from background: ${e.localizedMessage ?: "ForegroundServiceStartNotAllowed"}"
        }
    }

    fun stopMonitorService() {
        try {
            appContext.stopService(Intent(appContext, CallAssistantForegroundService::class.java))
        } catch (_: Exception) {
        }
        _isForegroundMonitorActive.value = false
    }

    fun onLiveCallStateChanged(newState: CallLifecycleState, callerInfo: String? = null): String? {
        val previous = _callState.value
        _callState.value = newState
        if (callerInfo != null) {
            _callerDisplay.value = callerInfo
        }

        var fgsError: String? = null
        if (newState == CallLifecycleState.RINGING && previous != CallLifecycleState.RINGING) {
            ignoredCurrentRing = false
            ringListenAttempt = 0
            ringRetryJob?.cancel()
            _pendingConfirmation.value = null
            classifier.resetDebounce()

            val currentSettings = _settings.value
            if (currentSettings.isAssistantEnabled && currentSettings.autoStartMicOnIncomingCall) {
                if (_isForegroundMonitorActive.value) {
                    updateServiceNotification(isRinging = true)
                    // FGS with microphone type is already active: immediately turn on SpeechRecognizer
                    startCallRingListening(source = CommandSource.LIVE_CALL)
                } else {
                    fgsError = startMonitorServiceSafely(
                        fromForegroundUi = false,
                        action = CallAssistantForegroundService.ACTION_INCOMING_CALL_RINGING
                    )
                    if (fgsError != null) {
                        // Fallback attempt in case app is in foreground or exempted
                        startCallRingListening(source = CommandSource.LIVE_CALL)
                    }
                }
            } else if (currentSettings.isAssistantEnabled && _isForegroundMonitorActive.value) {
                promptAndListenForCall(source = CommandSource.LIVE_CALL)
            }
        } else if (newState == CallLifecycleState.OFFHOOK) {
            ringRetryJob?.cancel()
            clearCallAlertNotification()
            updateServiceNotification(isRinging = false)
        } else if (newState == CallLifecycleState.IDLE) {
            ignoredCurrentRing = false
            ringListenAttempt = 0
            ringRetryJob?.cancel()
            _callerDisplay.value = null
            _pendingConfirmation.value = null
            actionExecutor.pendingCallScreeningRejectHandler = null
            clearCallAlertNotification()
            speechManager.stopListening()
            updateServiceNotification(isRinging = false)
        }
        return fgsError
    }

    /**
     * Called by CallAssistantForegroundService right after startForeground(..., MICROPHONE)
     * succeeds for ACTION_INCOMING_CALL_RINGING, guaranteeing microphone FGS eligibility
     * before invoking SpeechRecognizer.
     */
    fun onIncomingCallMonitorReady() {
        val wasIdle = _callState.value != CallLifecycleState.RINGING
        if (wasIdle) {
            _callState.value = CallLifecycleState.RINGING
            ignoredCurrentRing = false
            ringListenAttempt = 0
            ringRetryJob?.cancel()
            _pendingConfirmation.value = null
            classifier.resetDebounce()
        }
        clearCallAlertNotification()
        val currentSettings = _settings.value
        if (currentSettings.isAssistantEnabled && currentSettings.autoStartMicOnIncomingCall) {
            if (!speechManager.isListening.value) {
                startCallRingListening(source = CommandSource.LIVE_CALL)
            }
        } else if (currentSettings.isAssistantEnabled) {
            promptAndListenForCall(source = CommandSource.LIVE_CALL)
        }
    }

    private fun clearCallAlertNotification() {
        try {
            val nm = appContext.getSystemService(android.app.NotificationManager::class.java)
            nm?.cancel(CallAssistantForegroundService.CALL_ALERT_NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }

    fun startManualVoiceCommand(source: CommandSource = CommandSource.MANUAL_MIC) {
        ringRetryJob?.cancel()
        val currentSettings = _settings.value
        ttsManager.stop()
        speechManager.startListening(
            language = currentSettings.language,
            preferOffline = currentSettings.preferOfflineRecognition,
            source = source,
            onResult = { transcript, confidence, resultSource ->
                processTranscript(transcript, confidence, resultSource)
            },
            onError = { err ->
                _lastOutcomeBanner.value = err
            }
        )
    }

    private fun startCallRingListening(source: CommandSource) {
        val currentSettings = _settings.value
        if (ignoredCurrentRing) return

        val isStillRinging = _callState.value == CallLifecycleState.RINGING ||
            (_callState.value == CallLifecycleState.SIMULATED_RINGING && _simulationState.value.isRinging)
        if (!isStillRinging) return

        if (ringListenAttempt == 0) {
            _lastOutcomeBanner.value = "Incoming call detected — microphone started automatically."
        }

        ttsManager.stop()
        speechManager.startListening(
            language = currentSettings.language,
            preferOffline = currentSettings.preferOfflineRecognition,
            source = source,
            onResult = { transcript, confidence, resultSource ->
                processTranscript(transcript, confidence, resultSource)
            },
            onError = { err ->
                _lastOutcomeBanner.value = err
                scheduleRingRetryIfActive(source)
            }
        )
    }

    private fun scheduleRingRetryIfActive(source: CommandSource) {
        val currentSettings = _settings.value
        if (!currentSettings.continuousRingListening || ignoredCurrentRing) return
        val isStillRinging = when (source) {
            CommandSource.LIVE_CALL -> _callState.value == CallLifecycleState.RINGING
            CommandSource.TEST_MODE -> _callState.value == CallLifecycleState.SIMULATED_RINGING && _simulationState.value.isRinging
            CommandSource.MANUAL_MIC -> false
        }
        if (!isStillRinging) return
        if (ringListenAttempt >= MAX_RING_LISTEN_RETRIES) return

        ringListenAttempt += 1
        ringRetryJob?.cancel()
        ringRetryJob = scope.launch {
            delay(RING_RETRY_DELAY_MS)
            if (!ttsManager.isSpeaking.value) {
                startCallRingListening(source)
            }
        }
    }

    private fun updateServiceNotification(isRinging: Boolean) {
        if (!_isForegroundMonitorActive.value) return
        val intent = Intent(appContext, CallAssistantForegroundService::class.java).apply {
            action = if (isRinging) {
                CallAssistantForegroundService.ACTION_INCOMING_CALL_RINGING
            } else {
                CallAssistantForegroundService.ACTION_START_MONITOR
            }
        }
        try {
            appContext.startService(intent)
        } catch (_: Exception) {
        }
    }

    fun stopListening() {
        ringRetryJob?.cancel()
        speechManager.stopListening()
    }

    fun simulateIncomingCall() {
        classifier.resetDebounce()
        ignoredCurrentRing = false
        ringListenAttempt = 0
        ringRetryJob?.cancel()
        _pendingConfirmation.value = null
        _callState.value = CallLifecycleState.SIMULATED_RINGING

        val currentSettings = _settings.value
        val hasMic = capabilityChecker.inspect().hasRecordAudio
        val note = if (currentSettings.autoStartMicOnIncomingCall && hasMic) {
            "Simulated incoming call ringing — mic turned ON automatically! Speak a command."
        } else {
            "Simulated incoming call ringing... Tap Mic or pick a test command."
        }

        _simulationState.value = SimulationCallState(
            isRinging = true,
            isConnected = false,
            isRejected = false,
            isIgnored = false,
            isSpeakerOn = false,
            isMuted = false,
            statusNote = note
        )

        if (currentSettings.autoStartMicOnIncomingCall && hasMic) {
            startCallRingListening(CommandSource.TEST_MODE)
        } else if (currentSettings.voiceFeedbackEnabled) {
            val prompt = VoiceFeedbackMessages.incomingCallPrompt(
                _simulationState.value.callerLabel,
                currentSettings.language
            )
            ttsManager.speak(prompt, currentSettings.language)
        }
    }

    fun resetSimulation() {
        ringRetryJob?.cancel()
        speechManager.stopListening()
        _pendingConfirmation.value = null
        _callState.value = CallLifecycleState.IDLE
        _simulationState.value = SimulationCallState()
        _lastOutcomeBanner.value = null
    }

    fun processTranscript(
        transcript: String,
        recognizerConfidence: Float = 0.92f,
        source: CommandSource = CommandSource.MANUAL_MIC
    ) {
        val currentSettings = _settings.value
        val pending = _pendingConfirmation.value

        val classification = classifier.classify(
            transcript = transcript,
            recognizerConfidence = recognizerConfidence,
            requireWakeWord = currentSettings.requireWakeWord,
            awaitingConfirmation = pending != null,
            noiseSensitivity = currentSettings.noiseSensitivity,
            confirmUncertainActions = currentSettings.confirmUncertainActions
        )
        _lastClassification.value = classification

        if (classification.isNoiseFiltered || classification.isDuplicate) {
            _lastOutcomeBanner.value = classification.reason
            scheduleRingRetryIfActive(source)
            return
        }

        // Handle pending confirmation response ("Haan" / "Naa")
        if (pending != null) {
            when (classification.intent) {
                CallCommandIntent.CONFIRM_YES -> {
                    _pendingConfirmation.value = null
                    executeIntentAndRespond(
                        intent = pending.proposedIntent,
                        transcript = "${pending.originalTranscript} -> confirmed ($transcript)",
                        confidence = classification.confidence,
                        source = pending.source,
                        language = currentSettings.language
                    )
                    return
                }
                CallCommandIntent.CONFIRM_NO -> {
                    _pendingConfirmation.value = null
                    val msg = VoiceFeedbackMessages.forIntentExecution(
                        CallCommandIntent.CONFIRM_NO,
                        currentSettings.language
                    )
                    _lastOutcomeBanner.value = msg
                    speakIfEnabled(msg, currentSettings)
                    recordHistory(
                        spokenText = transcript,
                        intent = CallCommandIntent.CONFIRM_NO,
                        confidence = classification.confidence,
                        source = source,
                        outcomeMessage = msg,
                        isSuccess = true,
                        language = currentSettings.language
                    )
                    return
                }
                else -> {
                    _pendingConfirmation.value = null
                }
            }
        }

        if (classification.intent == CallCommandIntent.UNKNOWN) {
            val msg = if (!classification.wakeWordDetected && currentSettings.requireWakeWord) {
                "Wake word \"Baby\" not heard in \"$transcript\"."
            } else {
                VoiceFeedbackMessages.forIntentExecution(CallCommandIntent.UNKNOWN, currentSettings.language)
            }
            _lastOutcomeBanner.value = msg
            recordHistory(
                spokenText = transcript,
                intent = CallCommandIntent.UNKNOWN,
                confidence = classification.confidence,
                source = source,
                outcomeMessage = msg,
                isSuccess = false,
                language = currentSettings.language
            )
            scheduleRingRetryIfActive(source)
            return
        }

        if (classification.needsConfirmation) {
            _pendingConfirmation.value = PendingConfirmation(
                originalTranscript = transcript,
                proposedIntent = classification.intent,
                source = source
            )
            val prompt = VoiceFeedbackMessages.confirmationPrompt(
                classification.intent,
                currentSettings.language
            )
            _lastOutcomeBanner.value = "Confirmation needed: $prompt"
            speakIfEnabled(prompt, currentSettings) {
                if (source != CommandSource.TEST_MODE || _simulationState.value.isRinging) {
                    startManualVoiceCommand(source)
                }
            }
            return
        }

        ringRetryJob?.cancel()
        executeIntentAndRespond(
            intent = classification.intent,
            transcript = transcript,
            confidence = classification.confidence,
            source = source,
            language = currentSettings.language
        )
    }

    fun confirmPendingAction(accepted: Boolean) {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        val currentSettings = _settings.value
        if (accepted) {
            executeIntentAndRespond(
                intent = pending.proposedIntent,
                transcript = "${pending.originalTranscript} (confirmed)",
                confidence = 0.95f,
                source = pending.source,
                language = currentSettings.language
            )
        } else {
            val msg = VoiceFeedbackMessages.forIntentExecution(
                CallCommandIntent.CONFIRM_NO,
                currentSettings.language
            )
            _lastOutcomeBanner.value = msg
            speakIfEnabled(msg, currentSettings)
        }
    }

    private fun executeIntentAndRespond(
        intent: CallCommandIntent,
        transcript: String,
        confidence: Float,
        source: CommandSource,
        language: AssistantLanguage
    ) {
        val effectiveCallState = when {
            source == CommandSource.TEST_MODE && _simulationState.value.isRinging ->
                CallLifecycleState.SIMULATED_RINGING
            source == CommandSource.TEST_MODE ->
                CallLifecycleState.SIMULATED_ACTIVE
            else -> _callState.value
        }

        val result = actionExecutor.execute(
            intent = intent,
            callState = effectiveCallState,
            source = source,
            currentSimState = _simulationState.value,
            onUpdateSimState = { updated ->
                _simulationState.value = updated
                _callState.value = when {
                    updated.isRinging -> CallLifecycleState.SIMULATED_RINGING
                    updated.isConnected -> CallLifecycleState.SIMULATED_ACTIVE
                    else -> CallLifecycleState.IDLE
                }
            }
        )

        val isSuccess = result is ActionExecutionResult.Success || result is ActionExecutionResult.Ignored
        if (intent == CallCommandIntent.DO_NOT_ANSWER) {
            ignoredCurrentRing = true
            ringRetryJob?.cancel()
            speechManager.stopListening()
        }

        val spokenText = when (result) {
            is ActionExecutionResult.Success,
            is ActionExecutionResult.Ignored -> VoiceFeedbackMessages.forIntentExecution(intent, language)
            is ActionExecutionResult.PermissionRequired,
            is ActionExecutionResult.Unsupported,
            is ActionExecutionResult.Failed -> VoiceFeedbackMessages.actionBlockedNotice(result.summary, language)
        }

        _lastOutcomeBanner.value = result.summary
        speakIfEnabled(spokenText, _settings.value)

        recordHistory(
            spokenText = transcript,
            intent = intent,
            confidence = confidence,
            source = source,
            outcomeMessage = result.summary,
            isSuccess = isSuccess,
            language = language
        )
    }

    private fun promptAndListenForCall(source: CommandSource) {
        val currentSettings = _settings.value
        if (currentSettings.voiceFeedbackEnabled) {
            val prompt = VoiceFeedbackMessages.incomingCallPrompt(_callerDisplay.value, currentSettings.language)
            ttsManager.speak(prompt, currentSettings.language) {
                if (_callState.value == CallLifecycleState.RINGING && !ignoredCurrentRing) {
                    startCallRingListening(source)
                }
            }
        } else {
            startCallRingListening(source)
        }
    }

    private fun speakIfEnabled(
        message: String,
        settings: AssistantSettings,
        onComplete: (() -> Unit)? = null
    ) {
        if (settings.voiceFeedbackEnabled) {
            ttsManager.speak(message, settings.language, onComplete)
        } else {
            onComplete?.invoke()
        }
    }

    private fun recordHistory(
        spokenText: String,
        intent: CallCommandIntent,
        confidence: Float,
        source: CommandSource,
        outcomeMessage: String,
        isSuccess: Boolean,
        language: AssistantLanguage
    ) {
        scope.launch {
            historyRepository.recordCommand(
                spokenText = spokenText,
                intent = intent,
                confidence = confidence,
                source = source,
                outcomeMessage = outcomeMessage,
                isSuccess = isSuccess,
                language = language
            )
        }
    }

    companion object {
        private const val TAG = "CallSessionCoordinator"
        private const val MAX_RING_LISTEN_RETRIES = 6
        private const val RING_RETRY_DELAY_MS = 450L
    }
}
