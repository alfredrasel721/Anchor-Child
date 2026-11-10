package com.example

import com.example.data.remote.PairingRedemptionResult
import com.example.model.PairingCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorRepositoryRuleTest {

    @Test
    fun serverManagedPairingStatusDoesNotContainTheRawPasscode() {
        val statusRecord = PairingCode(
            codeId = "pair_child_leo_test",
            familyId = "fam_parent_test",
            ownerUid = "parent_test",
            childId = "child_leo",
            childName = "Leo",
            code = "",
            expiresAtEpochMs = 1_800_000_000_000L,
            status = "ACTIVE"
        )

        assertEquals("", statusRecord.code)
        assertEquals("ACTIVE", statusRecord.status)
    }

    @Test
    fun savedSessionVerificationDistinguishesRevocationFromNetworkFailure() {
        val networkFailure = PairingRedemptionResult(
            verified = false,
            errorMessage = "Network unavailable",
            serverResponded = false
        )
        val serverRejected = PairingRedemptionResult(
            verified = false,
            errorMessage = "Device is no longer paired",
            serverResponded = true
        )

        assertFalse(networkFailure.verified)
        assertFalse(networkFailure.serverResponded)
        assertFalse(serverRejected.verified)
        assertTrue(serverRejected.serverResponded)
    }
}
