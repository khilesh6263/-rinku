package com.example.domain.voice

import com.example.domain.model.CallCommandIntent
import com.example.domain.model.ClassificationResult
import java.util.Locale

/**
 * Deterministic, privacy-first voice command classifier for Hindi, English, and Hinglish.
 * Handles optional wake word ("Baby"), negation priority ("mat karo" before "karo"),
 * noise filtering, uncertain-action confirmation detection, and duplicate debouncing.
 */
class VoiceCommandClassifier(
    private val duplicateCooldownMs: Long = 2500L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var lastAcceptedIntent: CallCommandIntent? = null
    private var lastAcceptedTimestamp: Long = 0L

    fun resetDebounce() {
        lastAcceptedIntent = null
        lastAcceptedTimestamp = 0L
    }

    fun classify(
        transcript: String,
        recognizerConfidence: Float = 0.9f,
        requireWakeWord: Boolean = true,
        awaitingConfirmation: Boolean = false,
        noiseSensitivity: Float = 0.35f,
        confirmUncertainActions: Boolean = true
    ): ClassificationResult {
        val trimmed = transcript.trim()
        val normalized = normalizeText(trimmed)

        if (normalized.isBlank() || isLikelyNoise(normalized, recognizerConfidence, noiseSensitivity)) {
            return ClassificationResult(
                rawTranscript = trimmed,
                normalizedTranscript = normalized,
                intent = CallCommandIntent.UNKNOWN,
                confidence = 0f,
                wakeWordDetected = false,
                needsConfirmation = false,
                isNoiseFiltered = true,
                reason = "Filtered background noise or empty utterance"
            )
        }

        // Confirmation mode bypasses wake-word requirement so user can quickly say "Haan" or "Naa"
        if (awaitingConfirmation) {
            val confirmMatch = matchConfirmation(normalized)
            if (confirmMatch != null) {
                return ClassificationResult(
                    rawTranscript = trimmed,
                    normalizedTranscript = normalized,
                    intent = confirmMatch.first,
                    confidence = confirmMatch.second,
                    wakeWordDetected = containsWakeWord(normalized),
                    needsConfirmation = false,
                    reason = "Matched confirmation response"
                )
            }
        }

        val wakeWordDetected = containsWakeWord(normalized)
        val stripped = stripWakeWord(normalized)

        if (requireWakeWord && !wakeWordDetected) {
            return ClassificationResult(
                rawTranscript = trimmed,
                normalizedTranscript = normalized,
                intent = CallCommandIntent.UNKNOWN,
                confidence = 0f,
                wakeWordDetected = false,
                needsConfirmation = false,
                reason = "Wake word 'Baby' not detected"
            )
        }

        val targetText = stripped.ifBlank { normalized }
        val (intent, ruleScore, matchReason) = matchCommandRules(targetText)

        if (intent == CallCommandIntent.UNKNOWN) {
            return ClassificationResult(
                rawTranscript = trimmed,
                normalizedTranscript = normalized,
                intent = CallCommandIntent.UNKNOWN,
                confidence = 0.2f,
                wakeWordDetected = wakeWordDetected,
                needsConfirmation = false,
                reason = "No matching Hindi/English/Hinglish call command"
            )
        }

        val effectiveConfidence = ((ruleScore * 0.7f) + (recognizerConfidence.coerceIn(0f, 1f) * 0.3f))
            .coerceIn(0f, 1f)

        val now = timeProvider()
        val isDuplicate = lastAcceptedIntent == intent && (now - lastAcceptedTimestamp) < duplicateCooldownMs
        if (isDuplicate) {
            return ClassificationResult(
                rawTranscript = trimmed,
                normalizedTranscript = normalized,
                intent = intent,
                confidence = effectiveConfidence,
                wakeWordDetected = wakeWordDetected,
                needsConfirmation = false,
                isDuplicate = true,
                reason = "Duplicate command ignored within cooldown"
            )
        }

        val needsConfirmation = confirmUncertainActions &&
            intent.isDestructiveOrCallAltering &&
            effectiveConfidence < 0.78f

        if (!needsConfirmation) {
            lastAcceptedIntent = intent
            lastAcceptedTimestamp = now
        }

        return ClassificationResult(
            rawTranscript = trimmed,
            normalizedTranscript = normalized,
            intent = intent,
            confidence = effectiveConfidence,
            wakeWordDetected = wakeWordDetected,
            needsConfirmation = needsConfirmation,
            isDuplicate = false,
            reason = matchReason
        )
    }

    private fun normalizeText(input: String): String {
        return input
            .lowercase(Locale.ROOT)
            .replace(Regex("[,?.!।\"'\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isLikelyNoise(normalized: String, confidence: Float, noiseSensitivity: Float): Boolean {
        val minConfidence = 0.20f + (noiseSensitivity.coerceIn(0f, 1f) * 0.35f)
        if (confidence in 0.01f..minConfidence) return true
        val fillerSet = setOf("um", "uh", "hmm", "hm", "ah", "oh", "ha", "h", "a", "o", "the")
        val tokens = normalized.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true
        if (tokens.size == 1 && (tokens[0].length <= 1 || tokens[0] in fillerSet)) return true
        return false
    }

    private fun containsWakeWord(normalized: String): Boolean {
        return WAKE_WORD_PATTERNS.any { it.containsMatchIn(normalized) }
    }

    private fun stripWakeWord(normalized: String): String {
        var result = normalized
        for (pattern in WAKE_WORD_PATTERNS) {
            result = result.replace(pattern, " ")
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun matchConfirmation(normalized: String): Pair<CallCommandIntent, Float>? {
        val noTerms = listOf(
            "no", "nahi", "naa", "na", "mat", "cancel", "stop", "rehne do", "नहीं", "ना", "मत", "रुक"
        )
        if (noTerms.any { hasWordOrPhrase(normalized, it) }) {
            return CallCommandIntent.CONFIRM_NO to 0.92f
        }
        val yesTerms = listOf(
            "yes", "yeah", "yep", "haan", "ha", "han", "ok", "okay", "karo", "kar do", "confirm", "हाँ", "जी", "करो"
        )
        if (yesTerms.any { hasWordOrPhrase(normalized, it) }) {
            return CallCommandIntent.CONFIRM_YES to 0.92f
        }
        return null
    }

    private fun matchCommandRules(text: String): Triple<CallCommandIntent, Float, String> {
        // 1. NEGATION / DO NOT ANSWER MUST BE EVALUATED FIRST!
        // Prevents "call attend mat karo" from matching "call attend karo".
        val doNotAnswerPatterns = listOf(
            "attend mat", "mat attend", "call attend mat", "atend mat",
            "utha mat", "mat utha", "uthao mat", "mat uthao",
            "receive mat", "mat receive", "answer mat", "mat answer",
            "don't answer", "dont answer", "do not answer",
            "don't pick", "dont pick", "do not pick",
            "nahi uthana", "nahi attend", "attend nahi", "mat karo",
            "rehne do", "chhod do", "leave the call", "ignore call", "ignore the call",
            "अटेंड मत", "मत उठाओ", "मत उठा", "अटेंड नहीं", "रहने दो"
        )
        if (doNotAnswerPatterns.any { text.contains(it) } ||
            (containsAny(text, listOf("attend", "utha", "uthao", "answer", "receive", "pick", "उठा", "अटेंड")) &&
                containsAny(text, listOf("mat", "nahi", "naa", "dont", "don't", "not", "मत", "नहीं")))
        ) {
            return Triple(CallCommandIntent.DO_NOT_ANSWER, 0.97f, "Matched do-not-answer negation command")
        }

        // 2. REJECT / DECLINE / CUT CALL
        val rejectPatterns = listOf(
            "reject", "decline", "dismiss", "cut the call", "cut call", "call cut",
            "hang up", "hangup", "kaat do", "kat do", "call kaat", "call kat",
            "disconnect", "end call", "end the call", "busy",
            "रिजेक्ट", "काट दो", "कॉल काट", "डिक्लाइन"
        )
        if (rejectPatterns.any { text.contains(it) }) {
            val score = if (containsAny(text, listOf("call", "phone", "कॉल", "फोन"))) 0.95f else 0.74f
            return Triple(CallCommandIntent.REJECT_CALL, score, "Matched reject/decline command")
        }

        // 3. SPEAKER OFF (check before SPEAKER ON or check explicit off words)
        val hasSpeaker = containsAny(text, listOf("speaker", "loudspeaker", "loud speaker", "स्पीकर", "लाउडस्पीकर"))
        if (hasSpeaker && containsAny(text, listOf("off", "band", "bnd", "close", "disable", "hatao", "stop", "ऑफ", "बंद", "हटाओ"))) {
            return Triple(CallCommandIntent.SPEAKER_OFF, 0.95f, "Matched speaker-off command")
        }

        // 4. SPEAKER ON
        if (hasSpeaker && containsAny(text, listOf("on", "chalu", "start", "enable", "lagao", "karo", "करो", "ऑन", "चालू"))) {
            return Triple(CallCommandIntent.SPEAKER_ON, 0.95f, "Matched speaker-on command")
        }
        if (text == "speaker" || text == "loudspeaker") {
            return Triple(CallCommandIntent.SPEAKER_ON, 0.72f, "Ambiguous speaker keyword (assumed ON)")
        }

        // 5. UNMUTE MIC (evaluate before MUTE because "unmute" contains "mute"!)
        val unmutePatterns = listOf(
            "unmute", "un mute", "mute hatao", "mute hata", "mute off", "mute band",
            "mic on", "mic chalu", "microphone on", "awaz chalu", "awaaz chalu",
            "अनम्यूट", "म्यूट हटाओ", "म्यूट ऑफ", "माइक चालू"
        )
        if (unmutePatterns.any { text.contains(it) }) {
            return Triple(CallCommandIntent.UNMUTE_MIC, 0.96f, "Matched unmute command")
        }

        // 6. MUTE MIC
        val mutePatterns = listOf(
            "mute", "mic off", "mic band", "microphone off", "silence mic",
            "awaz band", "awaaz band", "chup",
            "म्यूट", "माइक बंद", "आवाज़ बंद"
        )
        if (mutePatterns.any { text.contains(it) }) {
            return Triple(CallCommandIntent.MUTE_MIC, 0.95f, "Matched mute command")
        }

        // 7. ANSWER / ATTEND CALL
        val answerPatterns = listOf(
            "attend", "atend", "utha lo", "uthao", "utha", "answer", "pick up", "pickup",
            "receive", "accept", "take the call", "take call", "connect",
            "अटेंड", "उठा लो", "उठाओ", "उठा", "आंसर"
        )
        if (answerPatterns.any { text.contains(it) }) {
            val score = if (containsAny(text, listOf("call", "phone", "कॉल", "फोन"))) 0.96f else 0.74f
            return Triple(CallCommandIntent.ANSWER_CALL, score, "Matched answer-call command")
        }

        return Triple(CallCommandIntent.UNKNOWN, 0.2f, "Unrecognized utterance")
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { hasWordOrPhrase(text, it) || text.contains(it) }
    }

    private fun hasWordOrPhrase(text: String, phrase: String): Boolean {
        val escaped = Regex.escape(phrase)
        return Regex("(^|\\s)$escaped(\\s|$)").containsMatchIn(text)
    }

    companion object {
        private val WAKE_WORD_PATTERNS = listOf(
            Regex("\\bbaby\\b"),
            Regex("\\bbebi\\b"),
            Regex("\\bbabi\\b"),
            Regex("\\bhey baby\\b"),
            Regex("\\bokay baby\\b"),
            Regex("\\bok baby\\b"),
            Regex("बेबी"),
            Regex("बेबि")
        )

        val SAMPLE_COMMANDS: List<Pair<String, CallCommandIntent>> = listOf(
            "Baby, call attend karo." to CallCommandIntent.ANSWER_CALL,
            "Baby, call utha lo." to CallCommandIntent.ANSWER_CALL,
            "Baby, answer the call." to CallCommandIntent.ANSWER_CALL,
            "Baby, call attend mat karo." to CallCommandIntent.DO_NOT_ANSWER,
            "Baby, call reject karo." to CallCommandIntent.REJECT_CALL,
            "Baby, speaker on karo." to CallCommandIntent.SPEAKER_ON,
            "Baby, speaker off karo." to CallCommandIntent.SPEAKER_OFF,
            "Baby, mute karo." to CallCommandIntent.MUTE_MIC,
            "Baby, mute hatao." to CallCommandIntent.UNMUTE_MIC
        )
    }
}
