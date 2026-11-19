package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.CommandSource
import com.example.service.IncomingCallReceiver
import com.example.ui.components.nebulaBackgroundBrush
import com.example.ui.screens.CommandHistoryScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.PermissionSetupWizardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TestVoiceCommandScreen
import com.example.ui.theme.BabyAssistantTheme
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.AssistantViewModel

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private object AppRoutes {
    const val HOME = "home"
    const val TEST_LAB = "test_lab"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PERMISSION_WIZARD = "permission_wizard"
}

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingCallListenIntent(intent)

        setContent {
            BabyAssistantTheme {
                BabyAssistantRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCapabilities()
        viewModel.ensureAssistantServiceIfEnabled()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingCallListenIntent(intent)
    }

    private fun handleIncomingCallListenIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(IncomingCallReceiver.EXTRA_START_VOICE_LISTEN, false) == true) {
            viewModel.activateManualMic(CommandSource.LIVE_CALL)
        }
    }
}

@Composable
fun BabyAssistantRoot(viewModel: AssistantViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isWizardRoute = currentDestination?.route == AppRoutes.PERMISSION_WIZARD

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val callerDisplay by viewModel.callerDisplay.collectAsStateWithLifecycle()
    val simulationState by viewModel.simulationState.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val lastClassification by viewModel.lastClassification.collectAsStateWithLifecycle()
    val lastOutcomeBanner by viewModel.lastOutcomeBanner.collectAsStateWithLifecycle()
    val isForegroundMonitorActive by viewModel.isForegroundMonitorActive.collectAsStateWithLifecycle()
    val capabilityReport by viewModel.capabilityReport.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val recognizerStatus by viewModel.recognizerStatus.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val history by viewModel.commandHistory.collectAsStateWithLifecycle()
    val serviceWarning by viewModel.serviceWarningMessage.collectAsStateWithLifecycle()

    val tabs = listOf(
        NavTab(AppRoutes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
        NavTab(AppRoutes.TEST_LAB, "Test Lab", Icons.Rounded.Science, Icons.Outlined.Science, "nav_test_lab"),
        NavTab(AppRoutes.HISTORY, "History", Icons.Rounded.History, Icons.Outlined.History, "nav_history"),
        NavTab(AppRoutes.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(nebulaBackgroundBrush())
    ) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && !isWizardRoute) {
                NavigationRail(
                    containerColor = NebulaSurface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(tab.testTag),
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                indicatorColor = ElectricViolet.copy(alpha = 0.25f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail && !isWizardRoute) {
                        NavigationBar(
                            containerColor = NebulaSurface.copy(alpha = 0.96f),
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = ElectricViolet.copy(alpha = 0.24f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.HOME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(AppRoutes.HOME) {
                        HomeDashboardScreen(
                            settings = settings,
                            callState = callState,
                            callerDisplay = callerDisplay,
                            capabilityReport = capabilityReport,
                            isForegroundMonitorActive = isForegroundMonitorActive,
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            rmsLevel = rmsLevel,
                            partialTranscript = partialTranscript,
                            recognizerStatus = recognizerStatus,
                            lastClassification = lastClassification,
                            lastOutcomeBanner = lastOutcomeBanner,
                            pendingConfirmation = pendingConfirmation,
                            serviceWarning = serviceWarning,
                            recentHistory = history,
                            onToggleAssistant = viewModel::toggleAssistant,
                            onMicClick = { viewModel.activateManualMic(CommandSource.MANUAL_MIC) },
                            onConfirmPending = viewModel::confirmPendingAction,
                            onDismissWarning = viewModel::clearWarning,
                            onOpenSetupWizard = { navController.navigate(AppRoutes.PERMISSION_WIZARD) },
                            onOpenTestLab = { navigateToTab(AppRoutes.TEST_LAB) },
                            onOpenHistory = { navigateToTab(AppRoutes.HISTORY) },
                            onSpeakSampleCommand = { phrase ->
                                navigateToTab(AppRoutes.TEST_LAB)
                                viewModel.runTestTranscript(phrase, 0.94f)
                            }
                        )
                    }

                    composable(AppRoutes.TEST_LAB) {
                        TestVoiceCommandScreen(
                            language = settings.language,
                            simState = simulationState,
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            rmsLevel = rmsLevel,
                            partialTranscript = partialTranscript,
                            lastClassification = lastClassification,
                            lastOutcomeBanner = lastOutcomeBanner,
                            pendingConfirmation = pendingConfirmation,
                            onSimulateIncomingCall = viewModel::simulateIncomingCall,
                            onResetSimulation = viewModel::resetSimulation,
                            onMicClick = { viewModel.activateManualMic(CommandSource.TEST_MODE) },
                            onRunTestTranscript = viewModel::runTestTranscript,
                            onConfirmPending = viewModel::confirmPendingAction
                        )
                    }

                    composable(AppRoutes.HISTORY) {
                        CommandHistoryScreen(
                            history = history,
                            onDeleteEntry = viewModel::deleteHistoryEntry,
                            onClearAll = viewModel::clearHistory
                        )
                    }

                    composable(AppRoutes.SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            capabilityReport = capabilityReport,
                            onLanguageSelected = viewModel::setLanguage,
                            onToggleWakeWord = viewModel::setRequireWakeWord,
                            onToggleConfirmUncertain = viewModel::setConfirmUncertainActions,
                            onToggleVoiceFeedback = viewModel::setVoiceFeedbackEnabled,
                            onToggleAutoStartMic = viewModel::setAutoStartMicOnIncomingCall,
                            onToggleContinuousRingListening = viewModel::setContinuousRingListening,
                            onTogglePreferOffline = viewModel::setPreferOfflineRecognition,
                            onNoiseSensitivityChanged = viewModel::setNoiseSensitivity,
                            onTestTtsGreeting = viewModel::testTtsGreeting,
                            onOpenSetupWizard = { navController.navigate(AppRoutes.PERMISSION_WIZARD) }
                        )
                    }

                    composable(AppRoutes.PERMISSION_WIZARD) {
                        PermissionSetupWizardScreen(
                            capabilityReport = capabilityReport,
                            onRefreshCapabilities = viewModel::refreshCapabilities,
                            onCreateCallScreeningIntent = viewModel::createCallScreeningRoleIntent,
                            onCompleteWizard = viewModel::markSetupWizardCompleted,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
