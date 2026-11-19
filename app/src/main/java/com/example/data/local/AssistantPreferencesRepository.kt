package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AssistantLanguage
import com.example.domain.model.AssistantSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.assistantDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "baby_assistant_prefs"
)

class AssistantPreferencesRepository(private val context: Context) {

    private object Keys {
        val ASSISTANT_ENABLED = booleanPreferencesKey("assistant_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val REQUIRE_WAKE_WORD = booleanPreferencesKey("require_wake_word")
        val CONFIRM_UNCERTAIN = booleanPreferencesKey("confirm_uncertain")
        val VOICE_FEEDBACK = booleanPreferencesKey("voice_feedback")
        val AUTO_START_MIC_ON_CALL = booleanPreferencesKey("auto_start_mic_on_call")
        val CONTINUOUS_RING_LISTENING = booleanPreferencesKey("continuous_ring_listening")
        val NOISE_SENSITIVITY = floatPreferencesKey("noise_sensitivity")
        val PREFER_OFFLINE = booleanPreferencesKey("prefer_offline")
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    }

    val settingsFlow: Flow<AssistantSettings> = context.assistantDataStore.data.map { prefs ->
        val languageName = prefs[Keys.LANGUAGE] ?: AssistantLanguage.HINGLISH.name
        val language = AssistantLanguage.entries.firstOrNull { it.name == languageName }
            ?: AssistantLanguage.HINGLISH
        AssistantSettings(
            isAssistantEnabled = prefs[Keys.ASSISTANT_ENABLED] ?: false,
            language = language,
            requireWakeWord = prefs[Keys.REQUIRE_WAKE_WORD] ?: true,
            confirmUncertainActions = prefs[Keys.CONFIRM_UNCERTAIN] ?: true,
            voiceFeedbackEnabled = prefs[Keys.VOICE_FEEDBACK] ?: true,
            autoStartMicOnIncomingCall = prefs[Keys.AUTO_START_MIC_ON_CALL] ?: true,
            continuousRingListening = prefs[Keys.CONTINUOUS_RING_LISTENING] ?: true,
            noiseSensitivity = prefs[Keys.NOISE_SENSITIVITY] ?: 0.35f,
            preferOfflineRecognition = prefs[Keys.PREFER_OFFLINE] ?: false,
            hasCompletedSetupWizard = prefs[Keys.SETUP_COMPLETED] ?: false
        )
    }

    suspend fun setAssistantEnabled(enabled: Boolean) {
        context.assistantDataStore.edit { it[Keys.ASSISTANT_ENABLED] = enabled }
    }

    suspend fun setLanguage(language: AssistantLanguage) {
        context.assistantDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setRequireWakeWord(required: Boolean) {
        context.assistantDataStore.edit { it[Keys.REQUIRE_WAKE_WORD] = required }
    }

    suspend fun setConfirmUncertainActions(confirm: Boolean) {
        context.assistantDataStore.edit { it[Keys.CONFIRM_UNCERTAIN] = confirm }
    }

    suspend fun setVoiceFeedbackEnabled(enabled: Boolean) {
        context.assistantDataStore.edit { it[Keys.VOICE_FEEDBACK] = enabled }
    }

    suspend fun setAutoStartMicOnIncomingCall(enabled: Boolean) {
        context.assistantDataStore.edit { it[Keys.AUTO_START_MIC_ON_CALL] = enabled }
    }

    suspend fun setContinuousRingListening(enabled: Boolean) {
        context.assistantDataStore.edit { it[Keys.CONTINUOUS_RING_LISTENING] = enabled }
    }

    suspend fun setNoiseSensitivity(value: Float) {
        context.assistantDataStore.edit { it[Keys.NOISE_SENSITIVITY] = value.coerceIn(0f, 1f) }
    }

    suspend fun setPreferOfflineRecognition(preferOffline: Boolean) {
        context.assistantDataStore.edit { it[Keys.PREFER_OFFLINE] = preferOffline }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.assistantDataStore.edit { it[Keys.SETUP_COMPLETED] = completed }
    }
}
