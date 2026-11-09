package com.example

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
    fun unpairedChildStartsOnPairingScreen_andCannotOpenFamilyFeatures() {
        composeTestRule.onNodeWithTag("verify_pairing_button")
            .assertExists()
            .assertIsNotEnabled()

        composeTestRule.onNodeWithTag("goto_c02_permissions_button")
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_tab_requests")
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_tab_safety")
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_tab_privacy")
            .assertDoesNotExist()
    }
}
