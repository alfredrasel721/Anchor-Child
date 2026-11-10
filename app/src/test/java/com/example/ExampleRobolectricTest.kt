package com.example

import com.example.model.ChildScreenId
import com.example.viewmodel.AnchorChildUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun freshChildStateIsUnpairedAndStartsAtSecurePairingGate() {
        val state = AnchorChildUiState()

        assertFalse(state.isPaired)
        assertEquals("", state.pairedFamilyId)
        assertEquals(ChildScreenId.C01_WELCOME_PAIRING, state.currentScreen)
        assertTrue(state.pairingCodes.isEmpty())
        assertTrue(state.children.isEmpty())
        assertTrue(state.appRules.isEmpty())
        assertEquals("FREE", state.household.subscriptionTier)
        assertFalse(state.household.subscriptionActive)
        assertEquals("This device", state.activeChild.name)
    }
}
