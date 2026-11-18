package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneCallback
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CommandHistoryEntity
import com.example.domain.model.AssistantSettings
import com.example.domain.model.CallLifecycleState
import com.example.domain.model.ClassificationResult
import com.example.domain.model.CommandSource
import com.example.domain.model.DeviceCapabilityReport
import com.example.domain.model.PendingConfirmation
import com.example.domain.voice.VoiceCommandClassifier
import com.example.ui.components.BabyVoiceOrbMicButton
import com.example.ui.components.NebulaCard
import com.example.ui.components.StatusPill
import com.example.ui.components.VoiceWaveformMeter
import com.example.ui.components.primaryAccentGradient
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NebulaOutline
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NebulaSurfaceBright
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SapphireBlue
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    settings: AssistantSettings,
    callState: CallLifecycleState,
    callerDisplay: String?,
    capabilityReport: DeviceCapabilityReport,
    isForegroundMonitorActive: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float,
    partialTranscript: String,
    recognizerStatus: String?,
    lastClassification: ClassificationResult?,
    lastOutcomeBanner: String?,
    pendingConfirmation: PendingConfirmation?,
    serviceWarning: String?,
    recentHistory: List<CommandHistoryEntity>,
    onToggleAssistant: (Boolean) -> Unit,
    onMicClick: () -> Unit,
    onConfirmPending: (Boolean) -> Unit,
    onDismissWarning: () -> Unit,
    onOpenSetupWizard: () -> Unit,
    onOpenTestLab: () -> Unit,
    onOpenHistory: () -> Unit,
    onSpeakSampleCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("home_scroll_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Brand Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(primaryAccentGradient())
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BABY",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Voice-Controlled Call Assistant",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!capabilityReport.allCorePermissionsGranted) {
                            OutlinedButton(
                                onClick = onOpenSetupWizard,
                                modifier = Modifier.testTag("header_setup_button"),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Setup", style = MaterialTheme.typography.labelMedium, color = AmberWarning)
                            }
                        }
                        StatusPill(
                            text = settings.language.displayName,
                            accentColor = NeonCyan
                        )
                    }
                }
            }

            // Warning or Pending Confirmation Banner
            item {
                AnimatedVisibility(visible = serviceWarning != null) {
                    NebulaCard(highlightBorder = true) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = AmberWarning
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = serviceWarning.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissWarning) {
                                Text("OK")
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = pendingConfirmation != null) {
                    pendingConfirmation?.let { pending ->
                        NebulaCard(highlightBorder = true) {
                            Text(
                                text = "Confirm Uncertain Command",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberWarning
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Heard \"${pending.originalTranscript}\". Execute ${pending.proposedIntent.label}?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { onConfirmPending(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("confirm_yes_button")
                                ) {
                                    Text("Yes / Haan")
                                }
                                OutlinedButton(
                                    onClick = { onConfirmPending(false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("confirm_no_button")
                                ) {
                                    Text("No / Cancel")
                                }
                            }
                        }
                    }
                }
            }

            // Hero Microphone Orb & Live Transcript Panel
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    NebulaSurfaceBright.copy(alpha = 0.72f),
                                    NebulaSurface.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(1.dp, ElectricViolet.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                        .padding(vertical = 18.dp, horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (settings.requireWakeWord) {
                                "Say \"Baby, call attend karo\""
                            } else {
                                "Wake word optional — speak command directly"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = SoftLavender
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        BabyVoiceOrbMicButton(
                            isAssistantEnabled = settings.isAssistantEnabled,
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            rmsLevel = rmsLevel,
                            onClick = onMicClick,
                            testTag = "home_mic_button"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        VoiceWaveformMeter(
                            isActive = isListening || isSpeaking,
                            rmsLevel = rmsLevel
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val liveText = when {
                            partialTranscript.isNotBlank() -> "\"$partialTranscript\""
                            recognizerStatus != null -> recognizerStatus
                            lastOutcomeBanner != null -> lastOutcomeBanner
                            else -> "Tap the orb for manual voice command or open Test Lab to simulate calls."
                        }
                        Text(
                            text = liveText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (partialTranscript.isNotBlank()) NeonCyan else TextSecondary
                        )

                        lastClassification?.let { result ->
                            if (result.rawTranscript.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPill(
                                        text = result.intent.label,
                                        accentColor = if (result.intent.name == "UNKNOWN") RoseDanger else EmeraldSuccess
                                    )
                                    Text(
                                        text = "${(result.confidence * 100).toInt()}% conf",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Assistant Master Switch & Incoming Call Detection Status Card
            item {
                NebulaCard(highlightBorder = settings.isAssistantEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Assistant",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.isAssistantEnabled) {
                                    if (isForegroundMonitorActive) {
                                        "Active — monitoring incoming calls hands-free"
                                    } else {
                                        "Enabled — tap toggle to refresh foreground service"
                                    }
                                } else {
                                    "Off — turn on to listen automatically on incoming calls"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = settings.isAssistantEnabled,
                            onCheckedChange = onToggleAssistant,
                            modifier = Modifier.testTag("assistant_master_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricViolet,
                                uncheckedBorderColor = NebulaOutline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Incoming Call Detection Status Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NebulaSurface)
                            .border(1.dp, NebulaOutline, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when (callState) {
                                    CallLifecycleState.RINGING,
                                    CallLifecycleState.SIMULATED_RINGING -> Icons.Rounded.NotificationsActive
                                    CallLifecycleState.OFFHOOK,
                                    CallLifecycleState.SIMULATED_ACTIVE -> Icons.Rounded.PhoneCallback
                                    CallLifecycleState.IDLE -> Icons.Rounded.Call
                                },
                                contentDescription = null,
                                tint = when (callState) {
                                    CallLifecycleState.RINGING,
                                    CallLifecycleState.SIMULATED_RINGING -> AmberWarning
                                    CallLifecycleState.OFFHOOK,
                                    CallLifecycleState.SIMULATED_ACTIVE -> EmeraldSuccess
                                    CallLifecycleState.IDLE -> SapphireBlue
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Call Detection: ${callState.label}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = callerDisplay
                                        ?: if (capabilityReport.isCallScreeningRoleHeld) {
                                            "CallScreeningService + Telephony active"
                                        } else {
                                            "Telephony listener ready (Role optional)"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        StatusPill(
                            text = if (isForegroundMonitorActive) "LIVE" else "STANDBY",
                            accentColor = if (isForegroundMonitorActive) EmeraldSuccess else TextMuted
                        )
                    }
                }
            }

            // Permission Banner when setup incomplete
            if (!capabilityReport.allCorePermissionsGranted) {
                item {
                    NebulaCard(highlightBorder = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permissions Needed for Live Calls",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Complete the step-by-step setup wizard to grant Mic, Phone State, and Call Control access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenSetupWizard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("open_setup_wizard_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Start Permission Setup Wizard")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Voice Commands Quick Reference + Test Lab CTA
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Supported Voice Commands",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = onOpenTestLab,
                        modifier = Modifier.testTag("home_open_test_lab_button")
                    ) {
                        Text("Test Mode", color = NeonCyan)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VoiceCommandClassifier.SAMPLE_COMMANDS.forEach { (phrase, intent) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(NebulaSurfaceElevatedCompat())
                                .border(1.dp, NebulaOutline, RoundedCornerShape(18.dp))
                                .clickable { onSpeakSampleCommand(phrase) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = phrase,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tap to test • ${intent.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            }

            // Recent Command History Preview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Command Activity",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (recentHistory.isNotEmpty()) {
                        TextButton(onClick = onOpenHistory) {
                            Text("See All", color = ElectricViolet)
                        }
                    }
                }
            }

            if (recentHistory.isEmpty()) {
                item {
                    NebulaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.RecordVoiceOver,
                                contentDescription = null,
                                tint = SapphireBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "No voice commands logged yet",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Tap the mic orb or try a simulated call in Test Lab. Audio is never uploaded.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            } else {
                items(recentHistory.take(3), key = { it.id }) { entry ->
                    HistorySummaryRow(entry = entry)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NebulaSurfaceElevatedCompat(): Color = Color(0xFF151C36)

@Composable
fun HistorySummaryRow(entry: CommandHistoryEntity) {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    NebulaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (entry.isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (entry.isSuccess) EmeraldSuccess else AmberWarning,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "\"${entry.spokenText}\"",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.intentLabel} • ${entry.outcomeMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatter.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = entry.sourceName.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
