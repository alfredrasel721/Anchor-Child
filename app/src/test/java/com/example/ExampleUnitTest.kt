package com.example

import com.example.model.AnchorSeedData
import com.example.model.ChildScreenId
import org.junit.Assert.assertEquals
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
    fun seedData_matchesParentAppSchemaContract() {
        val uid = "test_uid_01"
        val household = AnchorSeedData.defaultHousehold(uid)
        assertEquals("fam_test_uid_01", household.familyId)
        assertEquals(uid, household.ownerUid)

        val children = AnchorSeedData.defaultChildren(uid)
        assertTrue(children.any { it.childId == "leo_01" && it.familyId == household.familyId })
    }
}
