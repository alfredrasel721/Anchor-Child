package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.AnchorRepository
import com.example.model.AppRule
import com.example.model.ChildProfile
import com.example.model.PairingCode
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnchorRepositoryRuleTest {

    companion object {
        private var emulatorsConfigured = false
        private const val PROJECT_ID = "gen-lang-client-0963224775"
    }

    private lateinit var context: Context
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: AnchorRepository
    private lateinit var databaseId: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseId = context.getString(R.string.firestore_database_id)

        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(PROJECT_ID)
                    .setApplicationId("com.anchor.child")
                    .setApiKey("fake-emulator-api-key")
                    .build()
            )
        }

        val clearUrl = URL(
            "http://127.0.0.1:8085/emulator/v1/projects/$PROJECT_ID/databases/$databaseId/documents"
        )
        (clearUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 3000
            readTimeout = 3000
            responseCode
            disconnect()
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), databaseId)

        if (!emulatorsConfigured) {
            try {
                auth.useEmulator("127.0.0.1", 9099)
            } catch (_: IllegalStateException) {
            }
            try {
                firestore.useEmulator("127.0.0.1", 8085)
                firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
            } catch (_: IllegalStateException) {
            }
            emulatorsConfigured = true
        }

        repository = AnchorRepository(firestore)
    }

    @After
    fun tearDown() {
        try {
            auth.signOut()
        } catch (_: Exception) {
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun runFirestoreTest(block: suspend CoroutineScope.() -> Unit) {
        val errorRef = AtomicReference<Throwable?>(null)
        val done = AtomicBoolean(false)
        val worker = Thread {
            try {
                runBlocking { block() }
            } catch (t: Throwable) {
                errorRef.set(t)
            } finally {
                done.set(true)
            }
        }
        worker.start()
        while (!done.get()) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        worker.join()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        errorRef.get()?.let { throw it }
    }

    private suspend fun signInTestUser(email: String): String {
        val result = try {
            auth.createUserWithEmailAndPassword(email, "Password123!").await()
        } catch (_: Exception) {
            auth.signInWithEmailAndPassword(email, "Password123!").await()
        }
        return requireNotNull(result.user?.uid)
    }

    /**
     * Executes all 14 bilateral Parent <-> Child synchronization workflows across two separate
     * authenticated sessions (Parent Session + Child Session) on the shared Firestore database:
     * 1. Parent creates child profile
     * 2. Parent generates a 10-minute cryptographic pairing code
     * 3. Child signs in on a separate account and enters the pairing code
     * 4. Backend / rules verify and atomically consume the code
     * 5. Child device becomes paired in `/families/{familyId}/pairedDevices/{childUid}`
     * 6. Parent sees the paired device status (`ENFORCED`)
     * 7. Child screen-time data (`usedMinutesToday = 95`) appears in Parent
     * 8. Parent changes an app restriction (`Minecraft` limit 45m -> 60m)
     * 9. Child receives the updated rule in real time
     * 10. Child sends an extra-time request (`+30m` on `Minecraft`)
     * 11. Parent approves the request
     * 12. Child receives the `APPROVED` decision
     * 13. Child sends an SOS emergency alert
     * 14. Parent receives the `CRITICAL` SOS emergency alert
     */
    @Test
    fun fullFourteenStep_parentAndChild_bilateralSynchronizationWorkflow_succeeds() = runFirestoreTest {
        // --- PARENT SESSION: Steps 1 & 2 ---
        val parentUid = signInTestUser("sarah.parent@anchor.test")
        val familyId = "fam_$parentUid"
        repository.ensureSeeded(parentUid, "Sarah Jenkins", "sarah.parent@anchor.test")

        // Step 1: Parent creates/updates child profile
        val childLeo = ChildProfile(
            childId = "child_leo",
            familyId = familyId,
            ownerUid = parentUid,
            name = "Leo",
            age = 11,
            dailyAllowanceMinutes = 150,
            usedMinutesToday = 40,
            syncStatus = "PENDING"
        )
        repository.createChildProfileAsParent(parentUid, childLeo)

        // Step 2: Parent generates a cryptographic 6-digit pairing code (10-minute TTL)
        val generatedCode = repository.createPairingCode(
            parentUid = parentUid,
            childId = "child_leo",
            childName = "Leo"
        )
        assertEquals("ACTIVE", generatedCode.status)
        assertEquals(6, generatedCode.code.length)

        // --- CHILD SESSION: Steps 3, 4, 5 ---
        auth.signOut()
        val childUid = signInTestUser("leo.child.device@anchor.test")
        assertTrue(parentUid != childUid)

        // Step 3 & 4 & 5: Child enters the pairing code; verified & atomically consumed
        val redemption = repository.redeemPairingCode(
            childUid = childUid,
            targetFamilyIdOrUid = familyId,
            rawCode = generatedCode.code,
            childIdHint = "child_leo",
            deviceName = "Leo's Pixel 7a",
            deviceModel = "Google Pixel 7a",
            osVersion = "Android 14"
        )
        assertTrue("Pairing redemption should succeed: ${redemption.errorMessage}", redemption.verified)
        assertEquals(familyId, redemption.familyId)
        assertEquals("child_leo", redemption.childId)

        // Verify reused pairing code is rejected (Requirement 4.5)
        val reuseAttempt = repository.redeemPairingCode(
            childUid = childUid,
            targetFamilyIdOrUid = familyId,
            rawCode = generatedCode.code,
            childIdHint = "child_leo",
            deviceName = "Duplicate Device",
            deviceModel = "Pixel",
            osVersion = "Android 14"
        )
        assertFalse("Reused pairing code must be rejected", reuseAttempt.verified)

        // Step 7: Child syncs updated screen-time telemetry (`usedMinutesToday = 95`)
        repository.updateChildDeviceTelemetry(
            familyIdOrUid = familyId,
            child = childLeo.copy(
                usedMinutesToday = 95,
                batteryLevel = 78,
                syncStatus = "ENFORCED"
            )
        )

        // Step 10: Child submits an extra-time request (`+30m` for Minecraft)
        val submittedReq = repository.submitChildRequest(
            familyIdOrUid = familyId,
            childId = "child_leo",
            childName = "Leo",
            requestType = "EXTRA_TIME",
            targetName = "Minecraft",
            targetCategory = "Games",
            requestedBonusMinutes = 30,
            childNote = "Finishing bridge build with Sam",
            usedTodaySummary = "95m / 150m"
        )
        assertEquals("PENDING", submittedReq.status)

        // Step 13: Child triggers an SOS emergency alert
        val sosAlert = repository.triggerChildSosBeacon(
            familyIdOrUid = familyId,
            childId = "child_leo",
            childName = "Leo",
            locationLabel = "Oakridge Middle School",
            batteryLevel = 78,
            isPracticeDrill = false
        )
        assertEquals("SOS", sosAlert.category)
        assertEquals("CRITICAL", sosAlert.severity)

        // --- SWITCH TO PARENT SESSION: Steps 6, 8, 11, 14 ---
        auth.signOut()
        val reloggedParent = signInTestUser("sarah.parent@anchor.test")
        assertEquals(parentUid, reloggedParent)

        // Step 6 & 7: Parent observes paired status (`ENFORCED`) and updated screen time (`95m`)
        val childrenInParent = repository.observeChildren(familyId).first { it.isNotEmpty() }
        val leoInParent = childrenInParent.first { it.childId == "child_leo" }
        assertEquals("ENFORCED", leoInParent.syncStatus)
        assertEquals(95, leoInParent.usedMinutesToday)

        // Step 8: Parent changes Minecraft app rule limit from 45m to 60m
        repository.saveAppRule(
            parentUid = parentUid,
            rule = AppRule(
                ruleId = "rule_minecraft",
                familyId = familyId,
                ownerUid = parentUid,
                childId = "child_leo",
                packageName = "com.mojang.minecraftpe",
                appName = "Minecraft",
                category = "Games",
                dailyLimitMinutes = 60,
                usedMinutesToday = 45,
                status = "ENFORCED"
            )
        )

        // Step 11: Parent approves the child's extra-time request
        repository.resolveChildRequest(
            parentUid = parentUid,
            request = submittedReq,
            approved = true,
            decisionNote = "Approved +30m for your co-op build!"
        )

        // Step 14: Parent receives the CRITICAL SOS alert
        val alertsInParent = repository.observeAlerts(familyId).first { list ->
            list.any { it.alertId == sosAlert.alertId }
        }
        val receivedSos = alertsInParent.first { it.alertId == sosAlert.alertId }
        assertNotNull(receivedSos)
        assertEquals("CRITICAL", receivedSos.severity)

        // --- SWITCH BACK TO CHILD SESSION: Steps 9 & 12 ---
        auth.signOut()
        signInTestUser("leo.child.device@anchor.test")

        // Step 9: Child receives the updated 60m Minecraft rule
        val rulesInChild = repository.observeAppRules(familyId).first { it.isNotEmpty() }
        val mcRuleInChild = rulesInChild.first { it.ruleId == "rule_minecraft" }
        assertEquals(60, mcRuleInChild.dailyLimitMinutes)

        // Step 12: Child receives the APPROVED decision on their request
        val requestsInChild = repository.observeRequests(familyId).first { list ->
            list.any { it.requestId == submittedReq.requestId && it.status == "APPROVED" }
        }
        val decidedReq = requestsInChild.first { it.requestId == submittedReq.requestId }
        assertEquals("APPROVED", decidedReq.status)
    }

    @Test
    fun invalidExpiredAndCrossFamilyAccess_areStrictlyRejected() = runFirestoreTest {
        val ownerUid = signInTestUser("owner.guardian@anchor.test")
        val familyId = "fam_$ownerUid"
        repository.ensureSeeded(ownerUid, "Sarah Jenkins", "owner.guardian@anchor.test")

        // Seed an already-expired pairing code
        val expiredCode = PairingCode(
            codeId = "pair_child_leo_111222",
            familyId = familyId,
            ownerUid = ownerUid,
            childId = "child_leo",
            childName = "Leo",
            code = "111222",
            expiresAtEpochMs = System.currentTimeMillis() - 60_000L,
            status = "ACTIVE"
        )
        firestore.collection("families")
            .document(familyId)
            .collection("pairingCodes")
            .document(expiredCode.codeId)
            .set(expiredCode)
            .await()

        // Switch to an unlinked account
        auth.signOut()
        val attackerUid = signInTestUser("intruder@anchor.test")
        assertTrue(ownerUid != attackerUid)

        // 1. Invalid pairing code must fail
        val invalidRes = repository.redeemPairingCode(
            childUid = attackerUid,
            targetFamilyIdOrUid = familyId,
            rawCode = "999999",
            childIdHint = "child_leo",
            deviceName = "Intruder Phone",
            deviceModel = "Generic",
            osVersion = "Android 14"
        )
        assertFalse(invalidRes.verified)

        // 2. Expired pairing code must fail
        val expiredRes = repository.redeemPairingCode(
            childUid = attackerUid,
            targetFamilyIdOrUid = familyId,
            rawCode = "111222",
            childIdHint = "child_leo",
            deviceName = "Intruder Phone",
            deviceModel = "Generic",
            osVersion = "Android 14"
        )
        assertFalse(expiredRes.verified)

        // 3. Direct cross-family read must throw PERMISSION_DENIED
        try {
            firestore.collection("families")
                .document(familyId)
                .collection("children")
                .document("child_leo")
                .get()
                .await()
            fail("Expected PERMISSION_DENIED when unlinked user reads another family's child profile")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }
}
