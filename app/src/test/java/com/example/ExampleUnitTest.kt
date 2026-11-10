package com.example

import com.example.model.ChildScreenId
import com.example.model.ChildProfile
import com.example.model.FamilyHousehold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun allFifteenStitchScreens_areDefinedAndOrdered() {
        val screens = ChildScreenId.entries
        assertEquals(15, screens.size)
        assertEquals("C-01", screens.first().code)
        assertEquals("C-15", screens.last().code)
    }

    @Test
    fun freshModelsDoNotContainDemoChildOrPaidPlanDefaults() {
        val child = ChildProfile()
        val household = FamilyHousehold()

        assertEquals("", child.childId)
        assertEquals("", child.name)
        assertFalse(child.accessibilityEnabled)
        assertFalse(child.deviceAdminEnabled)
        assertEquals("FREE", household.subscriptionTier)
        assertFalse(household.subscriptionActive)
    }
}
