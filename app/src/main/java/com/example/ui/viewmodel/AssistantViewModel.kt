package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BabyAssistantApp
import com.example.data.local.CommandHistoryEntity
import com.example.domain.model.AssistantLanguage
import com.example.domain.model.AssistantSettings
import com.example.domain.model.CallLifecycleState
import com.example.domain.model.ClassificationResult
import com.example.domain.model.CommandSource
import com.example.domain.model.DeviceCapabilityReport
import com.example.domain.model.PendingConfirmation
import com.example.domain.model.SimulationCallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as BabyAssistantApp).container
    private val preferences = container.preferencesRepository
    private val historyRepo = container.historyRepository
    private val coordinator = container.coordinator

    val settings: StateFlow<AssistantSettings> = coordinator.settings
    val callState: StateFlow<CallLifecycleState> = coordinator.callState
    val callerDisplay: StateFlow<String?> = coordinator.callerDisplay
    val simulationState: StateFlow<SimulationCallState> = coordinator.simulationState
    val pendingConfirmation: StateFlow<PendingConfirmation?> = coordinator.pendingConfirmation
    val lastClassification: StateFlow<ClassificationResult?> = coordinator.lastClassification
    val lastOutcomeBanner: StateFlow<String?> = coordinator.lastOutcomeBanner
    val isForegroundMonitorActive: StateFlow<Boolean> = coordinator.isForegroundMonitorActive
    val capabilityReport: StateFlow<DeviceCapabilityReport> = coordinator.capabilityReport

    val isListening: StateFlow<Boolean> = coordinator.speechManager.isListening
    val partialTranscript: StateFlow<String> = coordinator.speechManager.partialTranscript
    val rmsLevel: StateFlow<Float> = coordinator.speechManager.rmsLevel
    val recognizerStatus: StateFlow<String?> = coordinator.speechManager.statusMessage
    val isSpeaking: StateFlow<Boolean> = coordinator.ttsManager.isSpeaking

    val commandHistory: StateFlow<List<CommandHistoryEntity>> = historyRepo.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _serviceWarningMessage = MutableStateFlow<String?>(null)
    val serviceWarningMessage: StateFlow<String?> = _serviceWarningMessage.asStateFlow()

    fun refreshCapabilities() {
        coordinator.refreshCapabilities()
    }

    fun ensureAssistantServiceIfEnabled() {
        coordinator.ensureMonitorRunningIfEnabled()
    }

    fun toggleAssistant(enabled: Boolean) {
        val warning = coordinator.toggleAssistantFromUi(enabled)
        _serviceWarningMessage.value = warning
    }

    fun clearWarning() {
        _serviceWarningMessage.value = null
    }

    fun activateManualMic(source: CommandSource = CommandSource.MANUAL_MIC) {
        if (isListening.value) {
            coordinator.stopListening()
        } else {
            coordinator.startManualVoiceCommand(source)
        }
    }

    fun simulateIncomingCall() {
        coordinator.simulateIncomingCall()
    }

    fun resetSimulation() {
        coordinator.resetSimulation()
    }

    fun runTestTranscript(transcript: String, confidence: Float = 0.92f) {
        coordinator.processTranscript(
            transcript = transcript,
            recognizerConfidence = confidence,
            source = CommandSource.TEST_MODE
        )
    }

    fun confirmPendingAction(accepted: Boolean) {
        coordinator.confirmPendingAction(accepted)
    }

    fun setLanguage(language: AssistantLanguage) {
        viewModelScope.launch { preferences.setLanguage(language) }
    }

    fun setRequireWakeWord(required: Boolean) {
        viewModelScope.launch { preferences.setRequireWakeWord(required) }
    }

    fun setConfirmUncertainActions(confirm: Boolean) {
        viewModelScope.launch { preferences.setConfirmUncertainActions(confirm) }
    }

    fun setVoiceFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setVoiceFeedbackEnabled(enabled) }
    }

    fun setAutoStartMicOnIncomingCall(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoStartMicOnIncomingCall(enabled) }
    }

    fun setContinuousRingListening(enabled: Boolean) {
        viewModelScope.launch { preferences.setContinuousRingListening(enabled) }
    }

    fun setNoiseSensitivity(value: Float) {
        viewModelScope.launch { preferences.setNoiseSensitivity(value) }
    }

    fun setPreferOfflineRecognition(preferOffline: Boolean) {
        viewModelScope.launch { preferences.setPreferOfflineRecognition(preferOffline) }
    }

    fun markSetupWizardCompleted() {
        viewModelScope.launch { preferences.setSetupCompleted(true) }
    }

    fun testTtsGreeting() {
        val lang = settings.value.language
        val greeting = when (lang) {
            AssistantLanguage.HINDI -> "नमस्ते, मैं बेबी हूँ। आपकी कॉल कंट्रोल करने के लिए तैयार हूँ।"
            AssistantLanguage.HINGLISH -> "Namaste, main Baby hoon. Call attend ya reject karne ke liye ready hoon."
            AssistantLanguage.ENGLISH -> "Hello, I am Baby. Ready to manage your incoming calls hands-free."
        }
        coordinator.ttsManager.speak(greeting, lang)
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepo.clearHistory() }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch { historyRepo.deleteEntry(id) }
    }

    fun createCallScreeningRoleIntent() = coordinator.capabilityChecker.createCallScreeningRoleIntent()
}
