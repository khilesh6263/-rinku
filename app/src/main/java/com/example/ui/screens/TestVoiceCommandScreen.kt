package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AssistantLanguage
import com.example.domain.model.ClassificationResult
import com.example.domain.model.PendingConfirmation
import com.example.domain.model.SimulationCallState
import com.example.ui.components.BabyVoiceOrbMicButton
import com.example.ui.components.NebulaCard
import com.example.ui.components.StatusPill
import com.example.ui.components.VoiceWaveformMeter
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NebulaOutline
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SapphireBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestVoiceCommandScreen(
    language: AssistantLanguage,
    simState: SimulationCallState,
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float,
    partialTranscript: String,
    lastClassification: ClassificationResult?,
    lastOutcomeBanner: String?,
    pendingConfirmation: PendingConfirmation?,
    onSimulateIncomingCall: () -> Unit,
    onResetSimulation: () -> Unit,
    onMicClick: () -> Unit,
    onRunTestTranscript: (String, Float) -> Unit,
    onConfirmPending: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var customPhrase by remember { mutableStateOf("Baby, call attend karo.") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("test_lab_scroll_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Voice Command Test Lab",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Test real Hindi, English, and Hinglish commands safely without placing a cellular call.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            // Simulated Incoming Call Stage
            item {
                val accent = when {
                    simState.isRinging -> AmberWarning
                    simState.isConnected -> EmeraldSuccess
                    simState.isRejected -> RoseDanger
                    simState.isIgnored -> TextMuted
                    else -> SapphireBlue
                }
                NebulaCard(highlightBorder = simState.isRinging || simState.isConnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    simState.isRinging -> Icons.Rounded.RingVolume
                                    simState.isRejected -> Icons.Rounded.CallEnd
                                    else -> Icons.Rounded.Call
                                },
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = simState.callerLabel,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = when {
                                        simState.isRinging -> "SIMULATED RINGING"
                                        simState.isConnected -> "SIMULATED CALL CONNECTED"
                                        simState.isRejected -> "SIMULATED CALL REJECTED"
                                        simState.isIgnored -> "LEFT UNANSWERED"
                                        else -> "SIMULATOR IDLE"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
                                )
                            }
                        }

                        StatusPill(text = language.displayName, accentColor = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = simState.statusNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Speaker & Mute Hardware Indicator Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NebulaSurface)
                                .border(
                                    1.dp,
                                    if (simState.isSpeakerOn) NeonCyan else NebulaOutline,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (simState.isSpeakerOn) {
                                    Icons.AutoMirrored.Rounded.VolumeUp
                                } else {
                                    Icons.AutoMirrored.Rounded.VolumeOff
                                },
                                contentDescription = null,
                                tint = if (simState.isSpeakerOn) NeonCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (simState.isSpeakerOn) "Speaker: ON" else "Speaker: OFF",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (simState.isSpeakerOn) NeonCyan else TextMuted
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NebulaSurface)
                                .border(
                                    1.dp,
                                    if (simState.isMuted) RoseDanger else NebulaOutline,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (simState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = if (simState.isMuted) RoseDanger else EmeraldSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (simState.isMuted) "Mic: MUTED" else "Mic: LIVE",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (simState.isMuted) RoseDanger else EmeraldSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSimulateIncomingCall,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("simulate_incoming_call_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.RingVolume, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Call")
                        }
                        OutlinedButton(
                            onClick = onResetSimulation,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reset_simulation_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }
                }
            }

            // Live Microphone Test Section
            item {
                NebulaCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Speak Into Live Microphone",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Uses Android SpeechRecognizer + TextToSpeech feedback",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BabyVoiceOrbMicButton(
                            isAssistantEnabled = true,
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            rmsLevel = rmsLevel,
                            onClick = onMicClick,
                            testTag = "test_lab_mic_button"
                        )
                        VoiceWaveformMeter(isActive = isListening || isSpeaking, rmsLevel = rmsLevel)
                        if (partialTranscript.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Heard: \"$partialTranscript\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonCyan
                            )
                        }
                        if (lastOutcomeBanner != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = lastOutcomeBanner,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Confirmation card if uncertain command was triggered
            if (pendingConfirmation != null) {
                item {
                    NebulaCard(highlightBorder = true) {
                        Text(
                            text = "Confirmation Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberWarning
                        )
                        Text(
                            text = "Uncertain match for \"${pendingConfirmation.originalTranscript}\" -> ${pendingConfirmation.proposedIntent.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onConfirmPending(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                modifier = Modifier.weight(1f)
                            ) { Text("Confirm (Haan)") }
                            OutlinedButton(
                                onClick = { onConfirmPending(false) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel (Naa)") }
                        }
                    }
                }
            }

            // One-Tap Required Scenario Presets
            item {
                NebulaCard {
                    Text(
                        text = "One-Tap Command Scenarios",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tap any command to verify classification, negation handling, and TTS voice reply:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val presets = listOf(
                        "Answer: \"Baby, call attend karo.\"" to ("Baby, call attend karo." to 0.96f),
                        "Answer (HI): \"Baby, call utha lo.\"" to ("Baby, call utha lo." to 0.95f),
                        "Answer (EN): \"Baby, answer the call.\"" to ("Baby, answer the call." to 0.95f),
                        "Do Not Answer: \"Baby, call attend mat karo.\"" to ("Baby, call attend mat karo." to 0.97f),
                        "Reject: \"Baby, call reject karo.\"" to ("Baby, call reject karo." to 0.96f),
                        "Speaker ON: \"Baby, speaker on karo.\"" to ("Baby, speaker on karo." to 0.95f),
                        "Speaker OFF: \"Baby, speaker off karo.\"" to ("Baby, speaker off karo." to 0.95f),
                        "Mute: \"Baby, mute karo.\"" to ("Baby, mute karo." to 0.95f),
                        "Unmute: \"Baby, mute hatao.\"" to ("Baby, mute hatao." to 0.96f),
                        "Uncertain Reject (asks confirmation)" to ("Baby, reject" to 0.45f)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEachIndexed { idx, (label, payload) ->
                            AssistChip(
                                onClick = { onRunTestTranscript(payload.first, payload.second) },
                                label = { Text(label) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("preset_cmd_$idx"),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = NebulaSurface,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom Phrase Tester
                    OutlinedTextField(
                        value = customPhrase,
                        onValueChange = { customPhrase = it },
                        label = { Text("Custom Hindi / Hinglish / English phrase") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_phrase_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = NebulaOutline
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onRunTestTranscript(customPhrase, 0.92f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("run_custom_phrase_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SapphireBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Classifier & Voice Feedback")
                    }

                    lastClassification?.let { res ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Classifier Output: ${res.intent.label} (${(res.confidence * 100).toInt()}%) • WakeWord=${res.wakeWordDetected} • ${res.reason}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
