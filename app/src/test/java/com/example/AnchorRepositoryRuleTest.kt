package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.AnchorRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Test
    fun ensureSeeded_and_childPairing_requests_sos_and_chat_succeed() = runFirestoreTest {
        val uid = signInTestUser("leo.family@anchor.test")

        repository.ensureSeeded(
            uid = uid,
            displayName = "Leo Miller",
            email = "leo.family@anchor.test"
        )

        val paired = repository.claimPairingCode(
            uid = uid,
            code = "482915",
            childId = "leo_01",
            deviceName = "Leo's Pixel 7a",
            deviceModel = "Google Pixel 7a",
            osVersion = "Android 14"
        )
        assertTrue(paired)

        val req = repository.submitChildRequest(
            uid = uid,
            childId = "leo_01",
            childName = "Leo",
            requestType = "EXTRA_TIME",
            targetName = "Minecraft",
            targetCategory = "Games",
            requestedBonusMinutes = 30,
            childNote = "Finishing bridge build with Sam",
            usedTodaySummary = "45m / 45m cap"
        )
        assertEquals("PENDING", req.status)

        val sos = repository.triggerChildSosBeacon(
            uid = uid,
            childId = "leo_01",
            childName = "Leo",
            locationLabel = "Lincoln Middle School",
            batteryLevel = 84,
            isPracticeDrill = true
        )
        assertEquals("SOS", sos.category)

        val checkIn = repository.sendSafeZoneCheckIn(
            uid = uid,
            childId = "leo_01",
            childName = "Leo",
            checkInHeadline = "I'm Safe & Arrived",
            locationLabel = "Lincoln Middle School",
            batteryLevel = 84
        )
        assertEquals("GEOFENCE", checkIn.category)
    }

    @Test
    fun crossFamilyAttacker_cannotReadOrMutateProtectedChildData() = runFirestoreTest {
        val ownerUid = signInTestUser("owner.guardian@anchor.test")
        repository.ensureSeeded(ownerUid, "Sarah Miller", "owner.guardian@anchor.test")

        auth.signOut()
        val attackerUid = signInTestUser("intruder@anchor.test")
        assertTrue(ownerUid != attackerUid)

        try {
            firestore.collection("families")
                .document("fam_$ownerUid")
                .collection("children")
                .document("leo_01")
                .get()
                .await()
            fail("Expected PERMISSION_DENIED when attacker reads another family's child profile")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }
}
