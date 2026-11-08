package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.model.AnchorSeedData
import com.example.model.AppRule
import com.example.model.ChildProfile
import com.example.model.ChildRequest
import com.example.model.FamilyHousehold
import com.example.model.FamilyMessage
import com.example.model.FamilyTask
import com.example.model.GuardianContact
import com.example.model.PairedDeviceRecord
import com.example.model.PairingCode
import com.example.model.PremiumFilterConfig
import com.example.model.SafeGeofence
import com.example.model.SafetyAlert
import com.example.model.ScheduleRoutine
import com.example.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class AnchorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val secureRandom = SecureRandom()

    companion object {
        fun create(context: Context): AnchorRepository {
            val databaseId = context.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(databaseId)
            return AnchorRepository(firestore, FirebaseAuth.getInstance())
        }
    }

    suspend fun upsertUserProfile(profile: UserProfile) {
        val path = "users/${profile.uid}"
        try {
            db.document(path).set(profile).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val path = "users/$uid"
        return try {
            val snap = db.document(path).get().await()
            snap.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun upsertFamilyHousehold(family: FamilyHousehold) {
        val path = "families/${family.familyId}"
        try {
            db.document(path).set(family).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getFamilyHousehold(familyId: String): FamilyHousehold? {
        val path = "families/$familyId"
        return try {
            val snap = db.document(path).get().await()
            snap.toObject(FamilyHousehold::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun upsertChildProfile(child: ChildProfile) {
        val path = "families/${child.familyId}/children/${child.childId}"
        try {
            db.document(path).set(child).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getChildren(familyId: String): List<ChildProfile> {
        val path = "families/$familyId/children"
        return try {
            val snap = db.collection(path).get().await()
            snap.toObjects(ChildProfile::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun upsertPairingCode(pairingCode: PairingCode) {
        val path = "families/${pairingCode.familyId}/pairingCodes/${pairingCode.codeId}"
        try {
            db.document(path).set(pairingCode).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    /**
     * Generates a cryptographically secure, one-time 6-digit pairing code with a 10-minute
     * expiration (600,000 ms) and revokes any previously active codes for the target child.
     */
    suspend fun createCryptographicPairingCode(
        parentUid: String,
        familyId: String = "fam_$parentUid",
        childId: String,
        childName: String
    ): PairingCode {
        val path = "families/$familyId/pairingCodes"
        try {
            val codesRef = db.collection(path)
            val existingActive = codesRef
                .whereEqualTo("childId", childId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            val numericCode = (100000 + secureRandom.nextInt(900000)).toString()
            val codeId = "pair_${childId}_$numericCode"
            val nowMs = System.currentTimeMillis()
            val newCode = PairingCode(
                id = codeId,
                codeId = codeId,
                familyId = familyId,
                ownerUid = parentUid,
                childId = childId,
                childName = childName,
                code = numericCode,
                expiresAtEpochMs = nowMs + 600_000L,
                status = "ACTIVE",
                createdAt = Timestamp.now()
            )

            val batch = db.batch()
            existingActive.documents.forEach { doc ->
                batch.update(doc.reference, mapOf("status" to "EXPIRED"))
            }
            batch.set(codesRef.document(codeId), newCode)
            batch.commit().await()
            return newCode
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun revokePairingCode(familyId: String, codeId: String) {
        val path = "families/$familyId/pairingCodes/$codeId"
        try {
            db.document(path).set(mapOf("status" to "EXPIRED"), SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun upsertAppRule(rule: AppRule) {
        val path = "families/${rule.familyId}/appRules/${rule.ruleId}"
        try {
            db.document(path).set(rule).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun upsertScheduleRoutine(schedule: ScheduleRoutine) {
        val path = "families/${schedule.familyId}/schedules/${schedule.scheduleId}"
        try {
            db.document(path).set(schedule).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun upsertSafeGeofence(geofence: SafeGeofence) {
        val path = "families/${geofence.familyId}/geofences/${geofence.geofenceId}"
        try {
            db.document(path).set(geofence).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun deleteSafeGeofence(familyId: String, geofenceId: String) {
        val path = "families/$familyId/geofences/$geofenceId"
        try {
            db.document(path).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
        }
    }

    suspend fun upsertSafetyAlert(alert: SafetyAlert) {
        val path = "families/${alert.familyId}/alerts/${alert.alertId}"
        try {
            db.document(path).set(alert).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun upsertChildRequest(request: ChildRequest) {
        val path = "families/${request.familyId}/requests/${request.requestId}"
        try {
            db.document(path).set(request).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun upsertFamilyMessage(message: FamilyMessage) {
        val path = "families/${message.familyId}/messages/${message.messageId}"
        try {
            db.document(path).set(message).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun upsertFamilyTask(task: FamilyTask) {
        val path = "families/${task.familyId}/tasks/${task.taskId}"
        try {
            db.document(path).set(task).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun upsertGuardianContact(guardian: GuardianContact) {
        val path = "families/${guardian.familyId}/guardians/${guardian.guardianId}"
        try {
            db.document(path).set(guardian).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun upsertPremiumConfig(config: PremiumFilterConfig) {
        val path = "families/${config.familyId}/premiumConfigs/${config.configId}"
        try {
            db.document(path).set(config).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun deleteFamilyHouseholdCascade(familyId: String, uid: String) {
        val familyPath = "families/$familyId"
        try {
            db.document(familyPath).delete().await()
            db.document("users/$uid").delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, familyPath)
        }
    }

    // ===============================================================
    // Real-Time Firestore Snapshot Listeners for Bilateral Sync
    // ===============================================================
    fun observeHousehold(familyId: String): Flow<FamilyHousehold?> = callbackFlow {
        val path = "families/$familyId"
        val registration = db.document(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.GET, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(FamilyHousehold::class.java))
        }
        awaitClose { registration.remove() }
    }

    fun observeChildren(familyId: String): Flow<List<ChildProfile>> = callbackFlow {
        val path = "families/$familyId/children"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (e: Exception) {
                    close(e)
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(ChildProfile::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observePairingCodes(familyId: String): Flow<List<PairingCode>> = callbackFlow {
        val path = "families/$familyId/pairingCodes"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(PairingCode::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observePairedDevices(familyId: String): Flow<List<PairedDeviceRecord>> = callbackFlow {
        val path = "families/$familyId/pairedDevices"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(PairedDeviceRecord::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeAppRules(familyId: String): Flow<List<AppRule>> = callbackFlow {
        val path = "families/$familyId/appRules"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(AppRule::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeSchedules(familyId: String): Flow<List<ScheduleRoutine>> = callbackFlow {
        val path = "families/$familyId/schedules"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(ScheduleRoutine::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeGeofences(familyId: String): Flow<List<SafeGeofence>> = callbackFlow {
        val path = "families/$familyId/geofences"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(SafeGeofence::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeAlerts(familyId: String): Flow<List<SafetyAlert>> = callbackFlow {
        val path = "families/$familyId/alerts"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(SafetyAlert::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeRequests(familyId: String): Flow<List<ChildRequest>> = callbackFlow {
        val path = "families/$familyId/requests"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(ChildRequest::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeMessages(familyId: String): Flow<List<FamilyMessage>> = callbackFlow {
        val path = "families/$familyId/messages"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(FamilyMessage::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeTasks(familyId: String): Flow<List<FamilyTask>> = callbackFlow {
        val path = "families/$familyId/tasks"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(FamilyTask::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeGuardians(familyId: String): Flow<List<GuardianContact>> = callbackFlow {
        val path = "families/$familyId/guardians"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(GuardianContact::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun observePremiumConfig(familyId: String): Flow<List<PremiumFilterConfig>> = callbackFlow {
        val path = "families/$familyId/premiumConfigs"
        val registration = db.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.LIST, path)
                } catch (_: Exception) {}
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(PremiumFilterConfig::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun bootstrapHouseholdIfNeeded(
        uid: String,
        displayName: String,
        email: String
    ): FamilyHousehold {
        val familyId = "fam_$uid"
        val existingUser = getUserProfile(uid)
        if (existingUser == null) {
            upsertUserProfile(
                UserProfile(
                    id = uid,
                    uid = uid,
                    displayName = displayName.ifBlank { "Sarah Jenkins" },
                    email = email.ifBlank { "sarah.jenkins@family.org" },
                    role = "PRIMARY_GUARDIAN",
                    familyId = familyId,
                    notificationCadence = "ALL_EVENTS"
                )
            )
        }
        val existingFamily = getFamilyHousehold(familyId)
        if (existingFamily != null) {
            return existingFamily
        }

        val seededFamily = AnchorSeedData.defaultFamily(uid, familyId)
        upsertFamilyHousehold(seededFamily)
        AnchorSeedData.defaultChildren(uid, familyId).forEach { upsertChildProfile(it) }
        upsertPairingCode(
            PairingCode(
                id = "pair_child_leo_482915",
                codeId = "pair_child_leo_482915",
                familyId = familyId,
                ownerUid = uid,
                childId = "child_leo",
                childName = "Leo",
                code = "482915",
                expiresAtEpochMs = System.currentTimeMillis() + 600_000L,
                status = "ACTIVE"
            )
        )
        AnchorSeedData.defaultAppRules(uid, familyId, "child_leo").forEach { upsertAppRule(it) }
        AnchorSeedData.defaultSchedules(uid, familyId, "child_leo").forEach { upsertScheduleRoutine(it) }
        AnchorSeedData.defaultGeofences(uid, familyId, "child_leo").forEach { upsertSafeGeofence(it) }
        AnchorSeedData.defaultAlerts(uid, familyId).forEach { upsertSafetyAlert(it) }
        AnchorSeedData.defaultRequests(uid, familyId).forEach { upsertChildRequest(it) }
        AnchorSeedData.defaultMessages(uid, familyId).forEach { upsertFamilyMessage(it) }
        AnchorSeedData.defaultTasks(uid, familyId).forEach { upsertFamilyTask(it) }
        AnchorSeedData.defaultGuardians(uid, familyId).forEach { upsertGuardianContact(it) }
        upsertPremiumConfig(AnchorSeedData.defaultPremiumConfig(uid, familyId, "child_leo"))
        return seededFamily
    }
}
