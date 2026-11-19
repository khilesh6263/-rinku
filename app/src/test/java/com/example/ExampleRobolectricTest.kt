package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Baby Assistant", appName)
    }

    @Test
    fun `home dashboard navigates to test lab and simulates incoming call commands`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_scroll_list")
            .performScrollToNode(hasTestTag("assistant_master_switch"))
        composeRule.onNodeWithTag("assistant_master_switch").assertIsDisplayed()

        // Navigate to Test Lab
        composeRule.onNodeWithTag("nav_test_lab").performClick()
        composeRule.waitForIdle()

        // Simulate incoming call and verify Answer command
        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("simulate_incoming_call_button"))
        composeRule.onNodeWithTag("simulate_incoming_call_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SIMULATED RINGING").assertIsDisplayed()

        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("preset_cmd_0"))
        composeRule.onNodeWithTag("preset_cmd_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("simulate_incoming_call_button"))
        composeRule.onNodeWithText("SIMULATED CALL CONNECTED").assertIsDisplayed()

        // Speaker ON & Mute presets
        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("preset_cmd_5"))
        composeRule.onNodeWithTag("preset_cmd_5").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("simulate_incoming_call_button"))
        composeRule.onNodeWithText("Speaker: ON").assertIsDisplayed()

        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("preset_cmd_7"))
        composeRule.onNodeWithTag("preset_cmd_7").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("simulate_incoming_call_button"))
        composeRule.onNodeWithText("Mic: MUTED").assertIsDisplayed()

        // Reset & test Do Not Answer and Reject
        composeRule.onNodeWithTag("simulate_incoming_call_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("preset_cmd_3"))
        composeRule.onNodeWithTag("preset_cmd_3").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("simulate_incoming_call_button"))
        composeRule.onNodeWithText("LEFT UNANSWERED").assertIsDisplayed()

        composeRule.onNodeWithTag("simulate_incoming_call_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("preset_cmd_4"))
        composeRule.onNodeWithTag("preset_cmd_4").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("test_lab_scroll_list")
            .performScrollToNode(hasTestTag("simulate_incoming_call_button"))
        composeRule.onNodeWithText("SIMULATED CALL REJECTED").assertIsDisplayed()

        // Navigate to Settings and verify Auto-Start Mic on Incoming Call toggle is displayed
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_scroll_list")
            .performScrollToNode(hasTestTag("toggle_auto_start_mic"))
        composeRule.onNodeWithTag("toggle_auto_start_mic").assertIsDisplayed()
    }

    @Test
    fun `foreground service incoming call ringing automatically triggers speech recognition`() {
        val app = ApplicationProvider.getApplicationContext<BabyAssistantApp>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_PHONE_STATE
        )

        val coordinator = app.container.coordinator
        coordinator.toggleAssistantFromUi(true)
        coordinator.setForegroundMonitorActive(true)
        coordinator.onLiveCallStateChanged(com.example.domain.model.CallLifecycleState.RINGING)

        // Immediately activates ring listening without any manual tap
        assertEquals(
            com.example.domain.model.CallLifecycleState.RINGING,
            coordinator.callState.value
        )
        assertEquals(
            "Incoming call detected — microphone started automatically.",
            coordinator.lastOutcomeBanner.value
        )

        // Drain Main Looper to confirm BabySpeechRecognizerManager.startListening() ran
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        org.junit.Assert.assertNotNull(coordinator.speechManager.statusMessage.value)

        coordinator.onLiveCallStateChanged(com.example.domain.model.CallLifecycleState.IDLE)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(
            com.example.domain.model.CallLifecycleState.IDLE,
            coordinator.callState.value
        )
    }
}
