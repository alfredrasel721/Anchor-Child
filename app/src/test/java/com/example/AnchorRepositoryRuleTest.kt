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
import org.junit.Assert.assertNotNull
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

    private suspend fun expectPermissionDenied(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected Firestore PERMISSION_DENIED.")
        } catch (error: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
        }
    }

    @Test
    fun parentCannotWritePairingRecords_andUnpairedChildCannotReadFamily() = runFirestoreTest {
        val parentUid = signInTestUser("parent.rules@anchor.test")
        val familyId = "fam_$parentUid"
        repository.ensureSeeded(parentUid, "Parent Test", "parent.rules@anchor.test")

        val familyDoc = firestore.document("families/$familyId").get().await()
        assertNotNull("Parent household should be created by the Parent bootstrap.", familyDoc.data)

        // Pairing code documents and device membership are now Admin-SDK-only.
        expectPermissionDenied {
            firestore.document("families/$familyId/pairingCodes/client-created").set(
                mapOf(
                    "codeId" to "client-created",
                    "familyId" to familyId,
                    "ownerUid" to parentUid,
                    "childId" to "child_leo",
                    "code" to "123456",
                    "status" to "ACTIVE"
                )
            ).await()
        }
        expectPermissionDenied {
            firestore.document("families/$familyId/pairedDevices/fake-device").set(
                mapOf(
                    "deviceId" to "fake-device",
                    "childUid" to "fake-device",
                    "familyId" to familyId,
                    "ownerUid" to parentUid,
                    "childId" to "child_leo",
                    "status" to "PAIRED"
                )
            ).await()
        }

        // A separately authenticated Child with no server-created membership is not in this family.
        auth.signOut()
        signInTestUser("unpaired.child@anchor.test")

        expectPermissionDenied {
            firestore.document("families/$familyId").get().await()
        }
        expectPermissionDenied {
            firestore.document("families/$familyId/children/child_leo").get().await()
        }
        expectPermissionDenied {
            firestore.collection("families/$familyId/pairingCodes").get().await()
        }
        expectPermissionDenied {
            firestore.document("pairingCodeLookups/fake-hash").get().await()
        }
        expectPermissionDenied {
            firestore.document("pairingCodeLookupIndexes/fake-code").set(
                mapOf("lookupId" to "fake-hash")
            ).await()
        }
        expectPermissionDenied {
            firestore.document("pairingRateLimits/unpaired-child").set(
                mapOf("failedCount" to 0)
            ).await()
        }
    }

    @Test
    fun unpairedChildCannotReadAnotherHouseholdEvenWhenFamilyIdIsKnown() = runFirestoreTest {
        val parentUid = signInTestUser("another.parent@anchor.test")
        val familyId = "fam_$parentUid"
        repository.ensureSeeded(parentUid, "Another Parent", "another.parent@anchor.test")

        auth.signOut()
        signInTestUser("different.child@anchor.test")

        expectPermissionDenied {
            firestore.document("families/$familyId/children/child_leo").get().await()
        }
        expectPermissionDenied {
            firestore.collection("families/$familyId/pairedDevices").get().await()
        }
    }
}
