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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PairingRedemptionResult(
    val verified: Boolean,
    val familyId: String = "",
    val ownerUid: String = "",
    val childId: String = "child_leo",
    val childName: String = "Leo",
    val codeId: String = "",
    val errorMessage: String? = null
)

class AnchorRepository(
    private val firestore: FirebaseFirestore
) {
    constructor(context: Context) : this(createDefaultFirestore(context))

    private val secureRandom = SecureRandom()

    fun familyIdFor(uid: String): String =
        if (uid.startsWith("fam_")) uid else "fam_$uid"

    private fun resolveOwnerUid(familyIdOrUid: String, fallbackUid: String): String =
        if (familyIdOrUid.startsWith("fam_")) familyIdOrUid.removePrefix("fam_") else fallbackUid

    private fun familyDoc(familyIdOrUid: String) =
        firestore.collection("families").document(familyIdFor(familyIdOrUid))

    private fun nowTimeLabel(): String {
        val fmt = SimpleDateFormat("h:mm a", Locale.US)
        return fmt.format(Date())
    }

    suspend fun ensureSeeded(uid: String, displayName: String, email: String) {
        val familyId = familyIdFor(uid)
        val userPath = "users/$uid"
        try {
            val userRef = firestore.collection("users").document(uid)
            val existingUser = userRef.get().await()
            if (!existingUser.exists()) {
                val profile = UserProfile(
                    uid = uid,
                    displayName = displayName.ifBlank { "Leo Jenkins (Child Device)" },
                    email = email.ifBlank { "leo.device@anchor.family" },
                    role = "CHILD_DEVICE",
                    familyId = familyId,
                    notificationCadence = "Immediate",
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                userRef.set(profile).await()
            }

            val famRef = familyDoc(familyId)
            val existingFam = famRef.get().await()
            if (!existingFam.exists()) {
                val batch = firestore.batch()
                batch.set(famRef, AnchorSeedData.defaultHousehold(uid, familyId))

                AnchorSeedData.defaultChildren(uid, familyId).forEach { child ->
                    batch.set(famRef.collection("children").document(child.childId), child)
                }
                AnchorSeedData.defaultPairingCodes(uid, familyId).forEach { code ->
                    batch.set(famRef.collection("pairingCodes").document(code.codeId), code)
                }
                AnchorSeedData.defaultAppRules(uid, familyId).forEach { rule ->
                    batch.set(famRef.collection("appRules").document(rule.ruleId), rule)
                }
                AnchorSeedData.defaultSchedules(uid, familyId).forEach { schedule ->
                    batch.set(famRef.collection("schedules").document(schedule.scheduleId), schedule)
                }
                AnchorSeedData.defaultGeofences(uid, familyId).forEach { geo ->
                    batch.set(famRef.collection("geofences").document(geo.geofenceId), geo)
                }
                AnchorSeedData.defaultAlerts(uid, familyId).forEach { alert ->
                    batch.set(famRef.collection("alerts").document(alert.alertId), alert)
                }
                AnchorSeedData.defaultRequests(uid, familyId).forEach { req ->
                    batch.set(famRef.collection("requests").document(req.requestId), req)
                }
                AnchorSeedData.defaultMessages(uid, familyId).forEach { msg ->
                    batch.set(famRef.collection("messages").document(msg.messageId), msg)
                }
                AnchorSeedData.defaultTasks(uid, familyId).forEach { task ->
                    batch.set(famRef.collection("tasks").document(task.taskId), task)
                }
                AnchorSeedData.defaultGuardians(uid, familyId).forEach { guardian ->
                    batch.set(famRef.collection("guardians").document(guardian.guardianId), guardian)
                }
                val prem = AnchorSeedData.defaultPremiumConfig(uid, familyId)
                batch.set(famRef.collection("premiumConfigs").document(prem.configId), prem)

                batch.commit().await()
            }
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, userPath)
            throw e
        }
    }

    fun observeHousehold(familyIdOrUid: String): Flow<FamilyHousehold?> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId"
        val registration = familyDoc(familyId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                FirestoreErrorHandler.handleFirestoreError(error, OperationType.GET, path)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(FamilyHousehold::class.java))
        }
        awaitClose { registration.remove() }
    }

    fun observeChildren(familyIdOrUid: String): Flow<List<ChildProfile>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/children"
        val registration = familyDoc(familyId).collection("children")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(ChildProfile::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observePairingCodes(familyIdOrUid: String): Flow<List<PairingCode>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/pairingCodes"
        val registration = familyDoc(familyId).collection("pairingCodes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(PairingCode::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeAppRules(familyIdOrUid: String): Flow<List<AppRule>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/appRules"
        val registration = familyDoc(familyId).collection("appRules")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(AppRule::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeSchedules(familyIdOrUid: String): Flow<List<ScheduleRoutine>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/schedules"
        val registration = familyDoc(familyId).collection("schedules")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(ScheduleRoutine::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeGeofences(familyIdOrUid: String): Flow<List<SafeGeofence>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/geofences"
        val registration = familyDoc(familyId).collection("geofences")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(SafeGeofence::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeAlerts(familyIdOrUid: String): Flow<List<SafetyAlert>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/alerts"
        val registration = familyDoc(familyId).collection("alerts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(SafetyAlert::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeRequests(familyIdOrUid: String): Flow<List<ChildRequest>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/requests"
        val registration = familyDoc(familyId).collection("requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(ChildRequest::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeMessages(familyIdOrUid: String): Flow<List<FamilyMessage>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/messages"
        val registration = familyDoc(familyId).collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(FamilyMessage::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeTasks(familyIdOrUid: String): Flow<List<FamilyTask>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/tasks"
        val registration = familyDoc(familyId).collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(FamilyTask::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeGuardians(familyIdOrUid: String): Flow<List<GuardianContact>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/guardians"
        val registration = familyDoc(familyId).collection("guardians")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(GuardianContact::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observePremiumConfigs(familyIdOrUid: String): Flow<List<PremiumFilterConfig>> = callbackFlow {
        val familyId = familyIdFor(familyIdOrUid)
        val path = "families/$familyId/premiumConfigs"
        val registration = familyDoc(familyId).collection("premiumConfigs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(PremiumFilterConfig::class.java)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    // ========================================================================
    // Secure Pairing System (Parent Generation/Revocation + Child Redemption)
    // ========================================================================

    /**
     * Generates a cryptographically secure, one-time 6-digit pairing code with 10-minute expiration.
     * Revokes any prior ACTIVE pairing codes for the same childId.
     */
    suspend fun createPairingCode(
        parentUid: String,
        childId: String,
        childName: String
    ): PairingCode {
        val familyId = familyIdFor(parentUid)
        val codeDigits = (100000 + secureRandom.nextInt(900000)).toString()
        val codeId = "pair_${childId}_$codeDigits"
        val expiresAt = System.currentTimeMillis() + 600_000L // 10-minute TTL

        val pairing = PairingCode(
            codeId = codeId,
            familyId = familyId,
            ownerUid = parentUid,
            childId = childId,
            childName = childName,
            code = codeDigits,
            expiresAtEpochMs = expiresAt,
            status = "ACTIVE",
            createdAt = Timestamp.now()
        )

        val famRef = familyDoc(familyId)
        val existingSnap = famRef.collection("pairingCodes").get().await()
        val batch = firestore.batch()
        existingSnap.documents.forEach { doc ->
            val old = doc.toObject(PairingCode::class.java)
            if (old != null && old.childId == childId && old.status == "ACTIVE") {
                batch.update(doc.reference, "status", "EXPIRED")
            }
        }
        batch.set(famRef.collection("pairingCodes").document(codeId), pairing)
        batch.commit().await()
        return pairing
    }

    /**
     * Revokes an active pairing code (Parent-only operation).
     */
    suspend fun revokePairingCode(parentUid: String, codeId: String) {
        val familyId = familyIdFor(parentUid)
        familyDoc(familyId)
            .collection("pairingCodes")
            .document(codeId)
            .update("status", "EXPIRED")
            .await()
    }

    /**
     * Redeems a parent-issued 6-digit pairing code for an authenticated Child device.
     * Enforced by Firestore security rules (`isValidPairingRedemption` + `isValidChildPairingCodeConsumption`):
     * - Verifies the code exists, is ACTIVE, and has not expired (`expiresAtEpochMs > now`).
     * - Atomically updates the pairingCode to CONSUMED and creates `/families/{familyId}/pairedDevices/{childUid}`.
     * - Updates the child device telemetry on `/families/{familyId}/children/{childId}`.
     */
    suspend fun redeemPairingCode(
        childUid: String,
        targetFamilyIdOrUid: String,
        rawCode: String,
        childIdHint: String = "child_leo",
        deviceName: String,
        deviceModel: String,
        osVersion: String
    ): PairingRedemptionResult {
        val familyId = familyIdFor(targetFamilyIdOrUid)
        val ownerUid = resolveOwnerUid(familyId, childUid)
        val normalizedDigits = rawCode.filter { it.isDigit() }
        val candidateCodeIds = listOf(
            "pair_${childIdHint}_$normalizedDigits",
            "pair_child_leo_$normalizedDigits",
            "pair_leo_01_$normalizedDigits"
        ).distinct()

        val path = "families/$familyId/pairingCodes"
        return try {
            val famRef = familyDoc(familyId)
            var matchedCode: PairingCode? = null

            for (candidateId in candidateCodeIds) {
                val snap = famRef.collection("pairingCodes").document(candidateId).get().await()
                if (snap.exists()) {
                    val obj = snap.toObject(PairingCode::class.java)
                    if (obj != null) {
                        matchedCode = obj.copy(codeId = snap.id)
                        break
                    }
                }
            }

            if (matchedCode == null) {
                return PairingRedemptionResult(
                    verified = false,
                    errorMessage = "Invalid pairing code. Please check the 6-digit code in Anchor Parent."
                )
            }

            val cleanStoredCode = matchedCode.code.filter { it.isDigit() }
            if (cleanStoredCode != normalizedDigits && matchedCode.code != rawCode.trim()) {
                return PairingRedemptionResult(
                    verified = false,
                    errorMessage = "Pairing code mismatch."
                )
            }

            if (matchedCode.status != "ACTIVE") {
                return PairingRedemptionResult(
                    verified = false,
                    errorMessage = "This pairing code has already been ${matchedCode.status.lowercase(Locale.US)}."
                )
            }

            if (matchedCode.expiresAtEpochMs <= System.currentTimeMillis()) {
                return PairingRedemptionResult(
                    verified = false,
                    errorMessage = "This pairing code has expired (10-minute limit). Generate a fresh code in Anchor Parent."
                )
            }

            val codeRef = famRef.collection("pairingCodes").document(matchedCode.codeId)
            val pairedDeviceRef = famRef.collection("pairedDevices").document(childUid)

            // Step 1: Atomic batch consuming the code and creating pairedDevices membership
            val batch = firestore.batch()
            batch.update(
                codeRef,
                mapOf(
                    "status" to "CONSUMED",
                    "redeemedByUid" to childUid
                )
            )
            val pairedRecord = PairedDeviceRecord(
                deviceUid = childUid,
                familyId = familyId,
                ownerUid = matchedCode.ownerUid.ifBlank { ownerUid },
                childId = matchedCode.childId,
                codeId = matchedCode.codeId,
                redeemedCode = matchedCode.code,
                deviceName = deviceName,
                deviceModel = deviceModel,
                osVersion = osVersion,
                status = "ACTIVE",
                pairedAtEpochMs = System.currentTimeMillis()
            )
            batch.set(pairedDeviceRef, pairedRecord)
            batch.commit().await()

            // Step 2: Now that pairedDevices membership is committed, update child telemetry & post audit message
            val childRef = famRef.collection("children").document(matchedCode.childId)
            val existingChild = childRef.get().await().toObject(ChildProfile::class.java)
            if (existingChild != null) {
                childRef.set(
                    existingChild.copy(
                        deviceName = deviceName,
                        deviceModel = deviceModel,
                        osVersion = osVersion,
                        syncStatus = "ENFORCED",
                        lastSyncLabel = "Paired at ${nowTimeLabel()}",
                        updatedAt = Timestamp.now()
                    )
                ).await()
            }

            sendFamilyMessage(
                familyIdOrUid = familyId,
                childId = matchedCode.childId,
                senderName = matchedCode.childName.ifBlank { "Leo" },
                senderRole = "SYSTEM",
                text = "Device Paired: $deviceName ($osVersion) linked via verified one-time code.",
                isSystemEvent = true
            )

            PairingRedemptionResult(
                verified = true,
                familyId = familyId,
                ownerUid = matchedCode.ownerUid.ifBlank { ownerUid },
                childId = matchedCode.childId,
                childName = matchedCode.childName.ifBlank { "Leo" },
                codeId = matchedCode.codeId
            )
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            PairingRedemptionResult(
                verified = false,
                errorMessage = e.localizedMessage ?: "Pairing verification failed on server."
            )
        }
    }

    /**
     * Backward-compatible wrapper returning Boolean for callers testing simple pairing claim.
     */
    suspend fun claimPairingCode(
        uid: String,
        code: String,
        childId: String,
        deviceName: String,
        deviceModel: String,
        osVersion: String
    ): Boolean {
        val res = redeemPairingCode(
            childUid = uid,
            targetFamilyIdOrUid = uid,
            rawCode = code,
            childIdHint = childId,
            deviceName = deviceName,
            deviceModel = deviceModel,
            osVersion = osVersion
        )
        return res.verified
    }

    // ========================================================================
    // Child App Telemetry & Permitted Mutations
    // ========================================================================

    suspend fun updateChildDeviceTelemetry(
        familyIdOrUid: String,
        child: ChildProfile
    ) {
        val familyId = familyIdFor(familyIdOrUid)
        val ownerUid = resolveOwnerUid(familyId, child.ownerUid)
        val path = "families/$familyId/children/${child.childId}"
        try {
            val childRef = familyDoc(familyId).collection("children").document(child.childId)
            val existing = childRef.get().await().toObject(ChildProfile::class.java)
            val preservedAllowance = existing?.dailyAllowanceMinutes ?: child.dailyAllowanceMinutes
            val preservedOwner = existing?.ownerUid?.ifBlank { ownerUid } ?: ownerUid

            childRef.set(
                child.copy(
                    familyId = familyId,
                    ownerUid = preservedOwner,
                    dailyAllowanceMinutes = preservedAllowance,
                    lastSyncLabel = "Synced at ${nowTimeLabel()}",
                    updatedAt = Timestamp.now()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun submitChildRequest(
        familyIdOrUid: String,
        childId: String,
        childName: String,
        requestType: String,
        targetName: String,
        targetCategory: String,
        requestedBonusMinutes: Int,
        childNote: String,
        usedTodaySummary: String,
        deterministicRequestId: String? = null
    ): ChildRequest {
        val familyId = familyIdFor(familyIdOrUid)
        val ownerUid = resolveOwnerUid(familyId, familyIdOrUid)
        val requestId = deterministicRequestId ?: "req_${UUID.randomUUID().toString().take(8)}"
        val path = "families/$familyId/requests/$requestId"
        val req = ChildRequest(
            requestId = requestId,
            familyId = familyId,
            ownerUid = ownerUid,
            childId = childId,
            childName = childName,
            requestType = requestType,
            targetName = targetName,
            targetCategory = targetCategory,
            requestedBonusMinutes = requestedBonusMinutes,
            childNote = childNote.ifBlank { "Requesting +${requestedBonusMinutes}m for $targetName" },
            usedTodaySummary = usedTodaySummary,
            status = "PENDING",
            decisionSummary = "Sent at ${nowTimeLabel()} • Waiting for parent review",
            decidedBy = "",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        try {
            familyDoc(familyId).collection("requests").document(requestId).set(req).await()
            sendFamilyMessage(
                familyIdOrUid = familyId,
                childId = childId,
                senderName = childName,
                senderRole = "CHILD",
                text = "Requested +${requestedBonusMinutes}m on $targetName: \"${req.childNote}\"",
                isSystemEvent = true
            )
            return req
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun triggerChildSosBeacon(
        familyIdOrUid: String,
        childId: String,
        childName: String,
        locationLabel: String,
        batteryLevel: Int,
        isPracticeDrill: Boolean = false,
        deterministicAlertId: String? = null
    ): SafetyAlert {
        val familyId = familyIdFor(familyIdOrUid)
        val ownerUid = resolveOwnerUid(familyId, familyIdOrUid)
        val alertId = deterministicAlertId ?: "sos_${UUID.randomUUID().toString().take(8)}"
        val path = "families/$familyId/alerts/$alertId"
        val alert = SafetyAlert(
            alertId = alertId,
            familyId = familyId,
            ownerUid = ownerUid,
            childId = childId,
            childName = childName,
            title = if (isPracticeDrill) {
                "SOS Practice Drill Completed ($childName)"
            } else {
                "URGENT SOS Emergency Beacon ($childName)"
            },
            description = "Location: $locationLabel • Battery: $batteryLevel% • Dispatched to Primary & Co-Parent Guardians.",
            category = "SOS",
            severity = if (isPracticeDrill) "WARNING" else "CRITICAL",
            timestampLabel = "Today, ${nowTimeLabel()}",
            acknowledged = false,
            createdAt = Timestamp.now()
        )
        try {
            familyDoc(familyId).collection("alerts").document(alertId).set(alert).await()
            sendFamilyMessage(
                familyIdOrUid = familyId,
                childId = childId,
                senderName = childName,
                senderRole = "SYSTEM",
                text = if (isPracticeDrill) {
                    "SOS Practice Drill verified from $locationLabel ($batteryLevel% battery)."
                } else {
                    "EMERGENCY SOS BEACON sent from $locationLabel ($batteryLevel% battery). Please check in immediately!"
                },
                isSystemEvent = true
            )
            return alert
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun sendSafeZoneCheckIn(
        familyIdOrUid: String,
        childId: String,
        childName: String,
        checkInHeadline: String,
        locationLabel: String,
        batteryLevel: Int,
        deterministicAlertId: String? = null
    ): SafetyAlert {
        val familyId = familyIdFor(familyIdOrUid)
        val ownerUid = resolveOwnerUid(familyId, familyIdOrUid)
        val alertId = deterministicAlertId ?: "checkin_${UUID.randomUUID().toString().take(8)}"
        val path = "families/$familyId/alerts/$alertId"
        val alert = SafetyAlert(
            alertId = alertId,
            familyId = familyId,
            ownerUid = ownerUid,
            childId = childId,
            childName = childName,
            title = "$childName: $checkInHeadline",
            description = "Verified GPS Check-In at $locationLabel • Battery $batteryLevel%",
            category = "GEOFENCE",
            severity = "INFO",
            timestampLabel = "Today, ${nowTimeLabel()}",
            acknowledged = false,
            createdAt = Timestamp.now()
        )
        try {
            familyDoc(familyId).collection("alerts").document(alertId).set(alert).await()
            sendFamilyMessage(
                familyIdOrUid = familyId,
                childId = childId,
                senderName = childName,
                senderRole = "CHILD",
                text = "$checkInHeadline ($locationLabel • Battery $batteryLevel%)",
                isSystemEvent = false
            )
            return alert
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun sendFamilyMessage(
        familyIdOrUid: String,
        childId: String,
        senderName: String,
        senderRole: String,
        text: String,
        isSystemEvent: Boolean = false,
        deterministicMessageId: String? = null
    ): FamilyMessage {
        val familyId = familyIdFor(familyIdOrUid)
        val ownerUid = resolveOwnerUid(familyId, familyIdOrUid)
        val messageId = deterministicMessageId ?: "msg_${UUID.randomUUID().toString().take(8)}"
        val path = "families/$familyId/messages/$messageId"
        val message = FamilyMessage(
            messageId = messageId,
            familyId = familyId,
            ownerUid = ownerUid,
            childId = childId,
            senderName = senderName,
            senderRole = senderRole,
            text = text,
            isSystemEvent = isSystemEvent,
            timestampLabel = nowTimeLabel(),
            createdAt = Timestamp.now()
        )
        try {
            familyDoc(familyId).collection("messages").document(messageId).set(message).await()
            return message
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun markTaskCompletedByChild(familyIdOrUid: String, task: FamilyTask) {
        val familyId = familyIdFor(familyIdOrUid)
        val ownerUid = resolveOwnerUid(familyId, task.ownerUid)
        val path = "families/$familyId/tasks/${task.taskId}"
        val nextCompleted = !task.completedByChild
        val nextStatus = if (nextCompleted) "READY_FOR_REVIEW" else "IN_PROGRESS"
        val updated = task.copy(
            familyId = familyId,
            ownerUid = ownerUid,
            completedByChild = nextCompleted,
            status = nextStatus,
            updatedAt = Timestamp.now()
        )
        try {
            familyDoc(familyId).collection("tasks").document(task.taskId).set(updated).await()
            if (nextCompleted) {
                sendFamilyMessage(
                    familyIdOrUid = familyId,
                    childId = task.childId,
                    senderName = "Leo",
                    senderRole = "CHILD",
                    text = "Marked task ready for review: \"${task.title}\" (${task.rewardSummary})",
                    isSystemEvent = true
                )
            }
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun recordLiveSessionAuditEvent(
        familyIdOrUid: String,
        childId: String,
        childName: String,
        sessionId: String,
        modalityLabel: String,
        actionDescription: String
    ) {
        val familyId = familyIdFor(familyIdOrUid)
        val ownerUid = resolveOwnerUid(familyId, familyIdOrUid)
        val alertId = "live_${UUID.randomUUID().toString().take(8)}"
        val alert = SafetyAlert(
            alertId = alertId,
            familyId = familyId,
            ownerUid = ownerUid,
            childId = childId,
            childName = childName,
            title = "Live Session ($modalityLabel): $actionDescription",
            description = "Audit ID: $sessionId • Logged transparently on both Parent & Child devices.",
            category = "SYSTEM",
            severity = "INFO",
            timestampLabel = "Today, ${nowTimeLabel()}",
            acknowledged = true,
            createdAt = Timestamp.now()
        )
        try {
            familyDoc(familyId).collection("alerts").document(alertId).set(alert).await()
            sendFamilyMessage(
                familyIdOrUid = familyId,
                childId = childId,
                senderName = "Anchor Transparency Log",
                senderRole = "SYSTEM",
                text = "Live Session [$sessionId] ($modalityLabel): $actionDescription by $childName.",
                isSystemEvent = true
            )
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(
                e,
                OperationType.CREATE,
                "families/$familyId/alerts/$alertId"
            )
        }
    }

    // ========================================================================
    // Parent-Side Operations (Used by Parent App & End-to-End Integration Tests)
    // ========================================================================

    suspend fun createChildProfileAsParent(parentUid: String, child: ChildProfile) {
        val familyId = familyIdFor(parentUid)
        val updated = child.copy(familyId = familyId, ownerUid = parentUid, updatedAt = Timestamp.now())
        familyDoc(familyId).collection("children").document(updated.childId).set(updated).await()
    }

    suspend fun resolveChildRequest(
        parentUid: String,
        request: ChildRequest,
        approved: Boolean,
        decisionNote: String
    ) {
        val familyId = familyIdFor(parentUid)
        val status = if (approved) "APPROVED" else "DECLINED"
        val updated = request.copy(
            familyId = familyId,
            ownerUid = parentUid,
            status = status,
            decisionSummary = decisionNote,
            decidedBy = "Parent Guardian",
            updatedAt = Timestamp.now()
        )
        familyDoc(familyId).collection("requests").document(request.requestId).set(updated).await()
    }

    suspend fun saveAppRule(parentUid: String, rule: AppRule) {
        val familyId = familyIdFor(parentUid)
        val updated = rule.copy(familyId = familyId, ownerUid = parentUid, updatedAt = Timestamp.now())
        familyDoc(familyId).collection("appRules").document(rule.ruleId).set(updated).await()
    }

    companion object {
        private fun createDefaultFirestore(context: Context): FirebaseFirestore {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
                    ?: com.google.firebase.FirebaseApp.initializeApp(
                        context,
                        com.google.firebase.FirebaseOptions.Builder()
                            .setProjectId("gen-lang-client-0963224775")
                            .setApplicationId("com.anchor.child")
                            .setApiKey("AIzaSyDummyKeyForLocalRobolectricTest")
                            .build()
                    )
            }
            return FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        }
    }
}
