package com.example.system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.model.AssistantLanguage
import com.example.domain.model.CommandSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BabySpeechRecognizerManager(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var activeSource: CommandSource = CommandSource.MANUAL_MIC
    private var onResultCallback: ((String, Float, CommandSource) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    fun startListening(
        language: AssistantLanguage,
        preferOffline: Boolean,
        source: CommandSource,
        onResult: (transcript: String, confidence: Float, source: CommandSource) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        mainHandler.post {
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val msg = "Microphone permission (RECORD_AUDIO) is required to listen."
                _statusMessage.value = msg
                _isListening.value = false
                onError(msg)
                return@post
            }

            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                val msg = "No system Speech Recognition service available on this device."
                _statusMessage.value = msg
                _isListening.value = false
                onError(msg)
                return@post
            }

            activeSource = source
            onResultCallback = onResult
            onErrorCallback = onError
            _partialTranscript.value = ""
            _statusMessage.value = "Listening (${language.displayName})..."

            try {
                recognizer?.destroy()
                recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(createListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.recognitionLocale)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                    if (preferOffline) {
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    }
                }

                _isListening.value = true
                recognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                _isListening.value = false
                val msg = e.localizedMessage ?: "SpeechRecognizer failed to start."
                _statusMessage.value = msg
                onError(msg)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (_: Exception) {
            }
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }

    fun cancelAndDestroy() {
        mainHandler.post {
            try {
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (_: Exception) {
            }
            recognizer = null
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _statusMessage.value = "Speak now — say \"Baby, ...\""
        }

        override fun onBeginningOfSpeech() {
            _statusMessage.value = "Listening to your command..."
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize typical SpeechRecognizer RMS (-2..10 dB) into 0f..1f
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _rmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _isListening.value = false
            _rmsLevel.value = 0f
            _statusMessage.value = "Processing voice command..."
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _rmsLevel.value = 0f
            val message = mapError(error)
            _statusMessage.value = message
            onErrorCallback?.invoke(message)
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _rmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            val topText = matches?.firstOrNull().orEmpty()
            val topConfidence = scores?.firstOrNull()?.takeIf { it >= 0f } ?: 0.88f

            if (topText.isNotBlank()) {
                _partialTranscript.value = topText
                _statusMessage.value = null
                onResultCallback?.invoke(topText, topConfidence, activeSource)
            } else {
                val msg = "No speech detected. Try again."
                _statusMessage.value = msg
                onErrorCallback?.invoke(msg)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull().orEmpty()
            if (partial.isNotBlank()) {
                _partialTranscript.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun mapError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
        SpeechRecognizer.ERROR_CLIENT -> "Recognition cancelled."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing."
        SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network timeout."
        SpeechRecognizer.ERROR_NO_MATCH -> "Could not recognize speech. Please speak clearly."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Try again."
        SpeechRecognizer.ERROR_SERVER -> "Speech server unavailable."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard before timeout."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Selected language pack is not supported on this device."
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Selected offline language pack is not downloaded."
        else -> "Speech recognition error ($error)."
    }

    companion object {
        private const val TAG = "BabySpeechRecognizer"
    }
}
