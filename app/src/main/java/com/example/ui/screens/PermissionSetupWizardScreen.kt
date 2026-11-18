package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DeviceCapabilityReport
import com.example.ui.components.NebulaCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NebulaOutline
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SapphireBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

private data class WizardStepInfo(
    val title: String,
    val subtitle: String,
    val whyNeeded: String,
    val privacyGuarantee: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val actionLabel: String
)

@Composable
fun PermissionSetupWizardScreen(
    capabilityReport: DeviceCapabilityReport,
    onRefreshCapabilities: () -> Unit,
    onCreateCallScreeningIntent: () -> Intent?,
    onCompleteWizard: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshCapabilities()
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshCapabilities()
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshCapabilities()
    }

    val steps = listOf(
        WizardStepInfo(
            title = "1. Microphone Access",
            subtitle = "RECORD_AUDIO",
            whyNeeded = "Baby uses Android's SpeechRecognizer to hear voice commands such as \"Baby, call attend karo\" or \"Baby, call reject karo\" when an incoming call rings or when you tap the mic button.",
            privacyGuarantee = "Microphone activates only during incoming calls or manual mic taps. Audio is never recorded or uploaded.",
            icon = Icons.Rounded.Mic,
            isGranted = capabilityReport.hasRecordAudio,
            actionLabel = if (capabilityReport.hasRecordAudio) "Microphone Granted" else "Grant Microphone Permission"
        ),
        WizardStepInfo(
            title = "2. Phone State & Call Control",
            subtitle = "READ_PHONE_STATE + ANSWER_PHONE_CALLS",
            whyNeeded = "Allows Baby to detect when your phone is ringing and execute Android TelecomManager.acceptRingingCall() (Android 8+) or TelecomManager.endCall() (Android 9+) on your voice command.",
            privacyGuarantee = "Baby never places outgoing calls or reads your call log history.",
            icon = Icons.Rounded.PhoneInTalk,
            isGranted = capabilityReport.hasReadPhoneState &&
                (!capabilityReport.supportsAcceptRingingCallApi || capabilityReport.hasAnswerPhoneCalls),
            actionLabel = if (capabilityReport.hasReadPhoneState && capabilityReport.hasAnswerPhoneCalls) {
                "Phone & Call Permissions Granted"
            } else {
                "Grant Phone & Call Permissions"
            }
        ),
        WizardStepInfo(
            title = "3. Call Screening Role (Android 10+)",
            subtitle = "ROLE_CALL_SCREENING",
            whyNeeded = "Android 10+ provides CallScreeningService so Baby can identify incoming calls early and reject unwanted calls via system call-screening APIs when requested.",
            privacyGuarantee = "Optional on older Android versions. You can change the default Call Screening app anytime in Android Settings.",
            icon = Icons.Rounded.Shield,
            isGranted = !capabilityReport.isCallScreeningRoleSupported || capabilityReport.isCallScreeningRoleHeld,
            actionLabel = when {
                !capabilityReport.isCallScreeningRoleSupported -> "Not Required on Android < 10"
                capabilityReport.isCallScreeningRoleHeld -> "Call Screening Role Active"
                else -> "Enable Call Screening Role"
            }
        ),
        WizardStepInfo(
            title = "4. Notifications (Android 13+)",
            subtitle = "POST_NOTIFICATIONS",
            whyNeeded = "Displays the persistent foreground monitor status indicator (showing when Baby is ready for calls) and alerts you if Android pauses background listening.",
            privacyGuarantee = "Only used for active assistant status and incoming-call quick prompts.",
            icon = Icons.Rounded.NotificationsActive,
            isGranted = capabilityReport.hasPostNotifications,
            actionLabel = if (capabilityReport.hasPostNotifications) {
                "Notifications Granted"
            } else {
                "Grant Notification Permission"
            }
        ),
        WizardStepInfo(
            title = "5. Foreground Service & OEM Rules",
            subtitle = "Android 14+ & Manufacturer Compatibility",
            whyNeeded = "Android 14+ requires starting the Voice Monitor from inside the app (foreground) so the microphone foreground service stays eligible when a call rings. Some manufacturers (Xiaomi, Oppo, Vivo, Samsung) also pause background services unless battery optimization is set to Unrestricted.",
            privacyGuarantee = "Baby never bypasses Android security rules. If Android blocks an action, Baby tells you honestly.",
            icon = Icons.Rounded.BatteryChargingFull,
            isGranted = true,
            actionLabel = "Open App Settings (Optional Battery Setup)"
        )
    )

    val step = steps[currentStep]

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("wizard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permission Setup Wizard",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Step ${currentStep + 1} of ${steps.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                    TextButton(
                        onClick = {
                            onCompleteWizard()
                            onNavigateBack()
                        }
                    ) {
                        Text("Skip", color = TextMuted)
                    }
                }
            }

            item {
                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / steps.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = ElectricViolet,
                    trackColor = NebulaOutline
                )
            }

            // Step Detail Card
            item {
                NebulaCard(highlightBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(ElectricViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        StatusPill(
                            text = if (step.isGranted) "READY" else "ACTION NEEDED",
                            accentColor = if (step.isGranted) EmeraldSuccess else AmberWarning
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = step.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Why Baby needs this",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.whyNeeded,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Privacy & Transparency",
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldSuccess
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.privacyGuarantee,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    if (currentStep == 4 && capabilityReport.oemRestrictionNote != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Device Advisory (${capabilityReport.manufacturer} ${capabilityReport.model}): ${capabilityReport.oemRestrictionNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarning
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            when (currentStep) {
                                0 -> singlePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                1 -> {
                                    val perms = buildList {
                                        add(Manifest.permission.READ_PHONE_STATE)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            add(Manifest.permission.ANSWER_PHONE_CALLS)
                                        }
                                    }.toTypedArray()
                                    multiPermissionLauncher.launch(perms)
                                }
                                2 -> {
                                    val roleIntent = onCreateCallScreeningIntent()
                                    if (roleIntent != null) {
                                        roleLauncher.launch(roleIntent)
                                    } else {
                                        onRefreshCapabilities()
                                    }
                                }
                                3 -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onRefreshCapabilities()
                                    }
                                }
                                4 -> {
                                    val settingsIntent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(settingsIntent)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("wizard_grant_step_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (step.isGranted && currentStep < 4) SapphireBlue else ElectricViolet
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (step.isGranted && currentStep < 4) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(step.actionLabel)
                    }
                }
            }

            // Stepper Navigation Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (currentStep > 0) currentStep-- },
                        enabled = currentStep > 0,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("wizard_prev_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previous")
                    }

                    Button(
                        onClick = {
                            if (currentStep < steps.lastIndex) {
                                currentStep++
                            } else {
                                onCompleteWizard()
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("wizard_next_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == steps.lastIndex) EmeraldSuccess else SapphireBlue
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (currentStep == steps.lastIndex) "Finish Setup" else "Next Step")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Summary Checklist
            item {
                NebulaCard {
                    Text(
                        text = "Live Checklist",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    steps.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (idx == currentStep) NeonCyan else TextSecondary
                            )
                            Text(
                                text = if (item.isGranted) "OK" else "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.isGranted) EmeraldSuccess else AmberWarning
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
