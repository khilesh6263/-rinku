package com.example

import com.example.domain.model.CallCommandIntent
import com.example.domain.voice.VoiceCommandClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandClassifierTest {

    @Test
    fun `matches all Hindi English and Hinglish example commands accurately`() {
        var now = 1000L
        val classifier = VoiceCommandClassifier(duplicateCooldownMs = 2500L) { now }

        val cases = listOf(
            "Baby, call attend karo." to CallCommandIntent.ANSWER_CALL,
            "Baby, call utha lo." to CallCommandIntent.ANSWER_CALL,
            "Baby, answer the call." to CallCommandIntent.ANSWER_CALL,
            "Baby, call attend mat karo." to CallCommandIntent.DO_NOT_ANSWER,
            "Baby, call reject karo." to CallCommandIntent.REJECT_CALL,
            "Baby, speaker on karo." to CallCommandIntent.SPEAKER_ON,
            "Baby, speaker off karo." to CallCommandIntent.SPEAKER_OFF,
            "Baby, mute karo." to CallCommandIntent.MUTE_MIC,
            "Baby, mute hatao." to CallCommandIntent.UNMUTE_MIC,
            "बेबी, कॉल अटेंड करो" to CallCommandIntent.ANSWER_CALL,
            "बेबी, कॉल मत उठाओ" to CallCommandIntent.DO_NOT_ANSWER,
            "बेबी, कॉल रिजेक्ट करो" to CallCommandIntent.REJECT_CALL
        )

        for ((phrase, expected) in cases) {
            now += 5000L // advance past duplicate cooldown
            val result = classifier.classify(
                transcript = phrase,
                recognizerConfidence = 0.95f,
                requireWakeWord = true
            )
            assertEquals("Failed for phrase: $phrase", expected, result.intent)
            assertTrue("Expected wake word for: $phrase", result.wakeWordDetected)
            assertFalse("Unexpected noise filter for: $phrase", result.isNoiseFiltered)
        }
    }

    @Test
    fun `negation takes priority over answer`() {
        val classifier = VoiceCommandClassifier()
        val result = classifier.classify("Baby, call attend mat karo", recognizerConfidence = 0.9f)
        assertEquals(CallCommandIntent.DO_NOT_ANSWER, result.intent)
    }

    @Test
    fun `rejects commands missing wake word when required`() {
        val classifier = VoiceCommandClassifier()
        val blocked = classifier.classify("call attend karo", requireWakeWord = true)
        assertEquals(CallCommandIntent.UNKNOWN, blocked.intent)
        assertFalse(blocked.wakeWordDetected)

        val allowed = classifier.classify("call attend karo", requireWakeWord = false)
        assertEquals(CallCommandIntent.ANSWER_CALL, allowed.intent)
    }

    @Test
    fun `prevents duplicate rapid commands within cooldown`() {
        var now = 10_000L
        val classifier = VoiceCommandClassifier(duplicateCooldownMs = 2500L) { now }

        val first = classifier.classify("Baby, speaker on karo")
        assertEquals(CallCommandIntent.SPEAKER_ON, first.intent)
        assertFalse(first.isDuplicate)

        now += 800L
        val second = classifier.classify("Baby, speaker on karo")
        assertTrue(second.isDuplicate)

        now += 3000L
        val third = classifier.classify("Baby, speaker on karo")
        assertFalse(third.isDuplicate)
    }

    @Test
    fun `requests confirmation on low confidence destructive command`() {
        val classifier = VoiceCommandClassifier()
        val result = classifier.classify(
            transcript = "Baby, reject",
            recognizerConfidence = 0.40f,
            confirmUncertainActions = true
        )
        assertEquals(CallCommandIntent.REJECT_CALL, result.intent)
        assertTrue(result.needsConfirmation)

        val confirmYes = classifier.classify("Haan", awaitingConfirmation = true)
        assertEquals(CallCommandIntent.CONFIRM_YES, confirmYes.intent)
    }

    @Test
    fun `filters noise and empty utterances`() {
        val classifier = VoiceCommandClassifier()
        val filler = classifier.classify("uh", recognizerConfidence = 0.9f)
        assertTrue(filler.isNoiseFiltered)
        assertEquals(CallCommandIntent.UNKNOWN, filler.intent)

        val lowConf = classifier.classify("Baby, call attend karo", recognizerConfidence = 0.15f)
        assertTrue(lowConf.isNoiseFiltered)
    }
}
