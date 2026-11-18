package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AssistantLanguage
import com.example.domain.model.AssistantSettings
import com.example.domain.model.DeviceCapabilityReport
import com.example.ui.components.NebulaCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NebulaOutline
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SapphireBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: AssistantSettings,
    capabilityReport: DeviceCapabilityReport,
    onLanguageSelected: (AssistantLanguage) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleConfirmUncertain: (Boolean) -> Unit,
    onToggleVoiceFeedback: (Boolean) -> Unit,
    onTogglePreferOffline: (Boolean) -> Unit,
    onNoiseSensitivityChanged: (Float) -> Unit,
    onTestTtsGreeting: () -> Unit,
    onOpenSetupWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Assistant Settings",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Customize language, wake word, voice feedback, and inspect Android API compatibility.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            // Language Selection
            item {
                NebulaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Recognition & Voice Language",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    AssistantLanguage.entries.forEach { lang ->
                        val selected = settings.language == lang
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .minimumInteractiveComponentSize()
                                .clickable { onLanguageSelected(lang) }
                                .testTag("language_option_${lang.name}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) ElectricViolet.copy(alpha = 0.18f) else NebulaSurface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) ElectricViolet else NebulaOutline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onLanguageSelected(lang) },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = lang.nativeLabel,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "STT: ${lang.recognitionLocale} • TTS: ${lang.ttsLanguageTag}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onTestTtsGreeting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("test_tts_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play Sample Voice Greeting (${settings.language.displayName})")
                    }
                }
            }

            // Voice Recognition Behavior
            item {
                NebulaCard {
                    Text(
                        text = "Voice Recognition & Safety",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingToggleRow(
                        title = "Require Wake Word \"Baby\"",
                        subtitle = "Ignore commands unless prefixed with \"Baby\" (reduces accidental triggers)",
                        checked = settings.requireWakeWord,
                        onCheckedChange = onToggleWakeWord,
                        testTag = "toggle_wake_word"
                    )

                    SettingToggleRow(
                        title = "Confirm Uncertain Actions",
                        subtitle = "Ask \"Haan ya Naa\" before answering or rejecting when recognition confidence is low",
                        checked = settings.confirmUncertainActions,
                        onCheckedChange = onToggleConfirmUncertain,
                        testTag = "toggle_confirm_uncertain"
                    )

                    SettingToggleRow(
                        title = "Spoken Voice Feedback (TTS)",
                        subtitle = "Speak confirmations like \"Ji, call attend kar rahi hoon.\"",
                        checked = settings.voiceFeedbackEnabled,
                        onCheckedChange = onToggleVoiceFeedback,
                        testTag = "toggle_voice_feedback"
                    )

                    SettingToggleRow(
                        title = "Prefer Offline Speech Recognition",
                        subtitle = "Request on-device speech pack when supported by Android",
                        checked = settings.preferOfflineRecognition,
                        onCheckedChange = onTogglePreferOffline,
                        testTag = "toggle_prefer_offline"
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Noise Filtering Threshold (${(settings.noiseSensitivity * 100).toInt()}%)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Higher values filter more background chatter before matching commands.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Slider(
                        value = settings.noiseSensitivity,
                        onValueChange = onNoiseSensitivityChanged,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = ElectricViolet
                        )
                    )
                }
            }

            // Privacy Card
            item {
                NebulaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = EmeraldSuccess
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy-First Guarantee",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Baby never records, stores, or uploads voice audio files. Only recognized text summaries are saved locally in Room for your Command History, which you can clear anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Device Compatibility & Permissions Card
            item {
                NebulaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = SapphireBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Device & Android API Report",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${capabilityReport.manufacturer} ${capabilityReport.model} • ${capabilityReport.androidVersionLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    CapabilityRow("SpeechRecognizer Available", capabilityReport.isSpeechRecognitionAvailable)
                    CapabilityRow("Telecom Answer API (API 26+)", capabilityReport.supportsAcceptRingingCallApi)
                    CapabilityRow("Telecom Reject API (API 28+)", capabilityReport.supportsEndCallApi)
                    CapabilityRow("CallScreeningService Role", capabilityReport.isCallScreeningRoleHeld)
                    CapabilityRow("Mic Permission (RECORD_AUDIO)", capabilityReport.hasRecordAudio)
                    CapabilityRow("Phone State (READ_PHONE_STATE)", capabilityReport.hasReadPhoneState)
                    CapabilityRow("Answer Calls (ANSWER_PHONE_CALLS)", capabilityReport.hasAnswerPhoneCalls)

                    capabilityReport.oemRestrictionNote?.let { note ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarning
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onOpenSetupWizard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("settings_open_wizard_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Permission Setup Wizard")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricViolet,
                uncheckedBorderColor = NebulaOutline
            )
        )
    }
}

@Composable
private fun CapabilityRow(label: String, supported: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (supported) EmeraldSuccess else AmberWarning,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (supported) "Supported / Granted" else "Missing / Optional",
                style = MaterialTheme.typography.labelSmall,
                color = if (supported) EmeraldSuccess else AmberWarning
            )
        }
    }
}
