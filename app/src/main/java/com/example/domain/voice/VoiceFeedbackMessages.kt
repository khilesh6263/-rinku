package com.example.domain.voice

import com.example.domain.model.AssistantLanguage
import com.example.domain.model.CallCommandIntent

object VoiceFeedbackMessages {

    fun forIntentExecution(intent: CallCommandIntent, language: AssistantLanguage): String {
        return when (intent) {
            CallCommandIntent.ANSWER_CALL -> when (language) {
                AssistantLanguage.ENGLISH -> "Answering the call now."
                AssistantLanguage.HINDI -> "जी, कॉल अटेंड कर रही हूँ।"
                AssistantLanguage.HINGLISH -> "Ji, call attend kar rahi hoon."
            }

            CallCommandIntent.DO_NOT_ANSWER -> when (language) {
                AssistantLanguage.ENGLISH -> "Okay, I will not answer this call."
                AssistantLanguage.HINDI -> "ठीक है, कॉल अटेंड नहीं करूँगी।"
                AssistantLanguage.HINGLISH -> "Theek hai, call attend nahi karungi."
            }

            CallCommandIntent.REJECT_CALL -> when (language) {
                AssistantLanguage.ENGLISH -> "The call has been rejected."
                AssistantLanguage.HINDI -> "कॉल रिजेक्ट कर दिया गया।"
                AssistantLanguage.HINGLISH -> "Call reject kar diya gaya."
            }

            CallCommandIntent.SPEAKER_ON -> when (language) {
                AssistantLanguage.ENGLISH -> "Speaker is turned on."
                AssistantLanguage.HINDI -> "स्पीकर ऑन कर दिया है।"
                AssistantLanguage.HINGLISH -> "Speaker on kar diya hai."
            }

            CallCommandIntent.SPEAKER_OFF -> when (language) {
                AssistantLanguage.ENGLISH -> "Speaker is turned off."
                AssistantLanguage.HINDI -> "स्पीकर ऑफ कर दिया है।"
                AssistantLanguage.HINGLISH -> "Speaker off kar diya hai."
            }

            CallCommandIntent.MUTE_MIC -> when (language) {
                AssistantLanguage.ENGLISH -> "Microphone is muted."
                AssistantLanguage.HINDI -> "माइक म्यूट कर दिया है।"
                AssistantLanguage.HINGLISH -> "Mic mute kar diya hai."
            }

            CallCommandIntent.UNMUTE_MIC -> when (language) {
                AssistantLanguage.ENGLISH -> "Microphone is unmuted."
                AssistantLanguage.HINDI -> "माइक अनम्यूट कर दिया है।"
                AssistantLanguage.HINGLISH -> "Mic unmute kar diya hai."
            }

            CallCommandIntent.CONFIRM_YES -> when (language) {
                AssistantLanguage.ENGLISH -> "Confirmed."
                AssistantLanguage.HINDI -> "ठीक है।"
                AssistantLanguage.HINGLISH -> "Theek hai."
            }

            CallCommandIntent.CONFIRM_NO -> when (language) {
                AssistantLanguage.ENGLISH -> "Action cancelled."
                AssistantLanguage.HINDI -> "कैंसल कर दिया है।"
                AssistantLanguage.HINGLISH -> "Cancel kar diya hai."
            }

            CallCommandIntent.UNKNOWN -> when (language) {
                AssistantLanguage.ENGLISH -> "Sorry, I did not understand that command."
                AssistantLanguage.HINDI -> "माफ़ कीजिए, मैं कमांड समझ नहीं पाई।"
                AssistantLanguage.HINGLISH -> "Maaf kijiye, main command samajh nahi payi."
            }
        }
    }

    fun confirmationPrompt(intent: CallCommandIntent, language: AssistantLanguage): String {
        return when (intent) {
            CallCommandIntent.ANSWER_CALL -> when (language) {
                AssistantLanguage.ENGLISH -> "Should I answer the call? Say yes or no."
                AssistantLanguage.HINDI -> "क्या मैं कॉल उठाऊँ? हाँ या ना बोलें।"
                AssistantLanguage.HINGLISH -> "Kya main call attend karun? Haan ya Naa bolein."
            }

            CallCommandIntent.REJECT_CALL -> when (language) {
                AssistantLanguage.ENGLISH -> "Should I reject the call? Say yes or no."
                AssistantLanguage.HINDI -> "क्या मैं कॉल रिजेक्ट कर दूँ? हाँ या ना बोलें।"
                AssistantLanguage.HINGLISH -> "Kya main call reject karun? Haan ya Naa bolein."
            }

            else -> when (language) {
                AssistantLanguage.ENGLISH -> "Please confirm. Say yes or no."
                AssistantLanguage.HINDI -> "कृपया कन्फर्म करें। हाँ या ना बोलें।"
                AssistantLanguage.HINGLISH -> "Confirm karein. Haan ya Naa bolein."
            }
        }
    }

    fun incomingCallPrompt(callerDisplay: String?, language: AssistantLanguage): String {
        val caller = callerDisplay?.takeIf { it.isNotBlank() } ?: "Incoming call"
        return when (language) {
            AssistantLanguage.ENGLISH -> "$caller. Say Baby, answer or reject the call."
            AssistantLanguage.HINDI -> "कॉल आ रही है। बोलिए बेबी, कॉल अटेंड करो या रिजेक्ट करो।"
            AssistantLanguage.HINGLISH -> "Call aa rahi hai. Bolein Baby, call attend karo ya reject karo."
        }
    }

    fun actionBlockedNotice(reason: String, language: AssistantLanguage): String {
        return when (language) {
            AssistantLanguage.ENGLISH -> "Android did not permit this action: $reason"
            AssistantLanguage.HINDI -> "एंड्रॉइड ने यह एक्शन अनुमति नहीं दी: $reason"
            AssistantLanguage.HINGLISH -> "Android ne yeh action allow nahi kiya: $reason"
        }
    }
}
