package com.example.system

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.domain.model.AssistantLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class BabyTtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingLocale: Locale = Locale.forLanguageTag("hi-IN")

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onSpeakingCompletedCallback: (() -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeakingCompletedCallback?.invoke()
                    onSpeakingCompletedCallback = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeakingCompletedCallback?.invoke()
                    onSpeakingCompletedCallback = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    onSpeakingCompletedCallback?.invoke()
                    onSpeakingCompletedCallback = null
                }
            })
            applyLocale(pendingLocale)
        } else {
            Log.w(TAG, "TextToSpeech init failed with status=$status")
        }
    }

    fun speak(
        message: String,
        language: AssistantLanguage,
        onComplete: (() -> Unit)? = null
    ) {
        if (message.isBlank()) {
            onComplete?.invoke()
            return
        }
        val locale = when (language) {
            AssistantLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
            AssistantLanguage.HINGLISH -> Locale.forLanguageTag("hi-IN")
            AssistantLanguage.ENGLISH -> Locale.forLanguageTag("en-IN")
        }
        pendingLocale = locale

        val engine = tts
        if (!isInitialized || engine == null) {
            onComplete?.invoke()
            return
        }

        applyLocale(locale)
        onSpeakingCompletedCallback = onComplete
        _isSpeaking.value = true

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        val result = engine.speak(message, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            _isSpeaking.value = false
            onSpeakingCompletedCallback = null
            onComplete?.invoke()
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        onSpeakingCompletedCallback = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isSpeaking.value = false
    }

    private fun applyLocale(locale: Locale) {
        val engine = tts ?: return
        val availability = engine.isLanguageAvailable(locale)
        if (availability >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
        } else {
            val fallback = Locale.US
            if (engine.isLanguageAvailable(fallback) >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = fallback
            }
        }
        engine.setSpeechRate(1.02f)
    }

    companion object {
        private const val TAG = "BabyTtsManager"
    }
}
