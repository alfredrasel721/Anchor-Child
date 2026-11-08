package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun todayScreenRenders_andNavigatesAcrossStitchScreens() {
        // Verify default C-03 Today Screen Time loads
        composeTestRule.onNodeWithTag("sync_status_pill").assertExists()
        composeTestRule.onAllNodesWithText("Today's Screen Time", substring = true).onFirst().assertExists()

        // Navigate to C-06 Request Extra Time via bottom tab
        composeTestRule.onNodeWithTag("bottom_tab_requests").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Request Extra Time", substring = true).onFirst().assertExists()

        // Navigate to Safety & SOS (C-08) via bottom tab
        composeTestRule.onNodeWithTag("bottom_tab_safety").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("c08_sos_trigger_button").assertExists()

        // Navigate to Privacy & Sharing Matrix (C-11) via bottom tab
        composeTestRule.onNodeWithTag("bottom_tab_privacy").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Privacy & Sharing Matrix", substring = true).onFirst().assertExists()
    }
}
