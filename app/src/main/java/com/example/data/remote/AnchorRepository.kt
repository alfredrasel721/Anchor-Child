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
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
    val errorMessage: String? = null,
    val serverResponded: Boolean = false
)

class AnchorRepository(
    private val firestore: FirebaseFirestore
) {
    constructor(context: Context) : this(createDefaultFirestore(context))

    private val functions = FirebaseFunctions.getInstance("asia-south1")

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

    @Deprecated("Child devices must use server-verified Parent pairing; they cannot bootstrap households.")
    suspend fun ensureSeeded(
        uid: String,
        displayName: String,
        email: String
    ) {
        throw SecurityException(
            "Child devices cannot create family households. Sign in and enter a one-time code from Anchor Parent."
        )
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
    // Secure Pairing System — all membership and code transitions are server-only
    // ========================================================================

    suspend fun createPairingCode(
        parentUid: String,
        childId: String,
        childName: String
    ): PairingCode {
        val familyId = familyIdFor(parentUid)
        val response = functions.getHttpsCallable("createPairingCode")
            .call(mapOf("familyId" to familyId, "childId" to childId, "childName" to childName))
            .await()
        val data = response.data as? Map<*, *>
            ?: throw IllegalStateException("Pairing service returned an invalid response.")
        return PairingCode(
            codeId = data["codeId"] as? String ?: "",
            familyId = data["familyId"] as? String ?: familyId,
            ownerUid = data["ownerUid"] as? String ?: parentUid,
            childId = data["childId"] as? String ?: childId,
            childName = data["childName"] as? String ?: childName,
            code = data["code"] as? String ?: "",
            expiresAtEpochMs = (data["expiresAtEpochMs"] as? Number)?.toLong() ?: 0L,
            status = data["status"] as? String ?: "ACTIVE"
        )
    }

    suspend fun revokePairingCode(parentUid: String, codeId: String) {
        functions.getHttpsCallable("revokePairingCode")
            .call(mapOf("familyId" to familyIdFor(parentUid), "codeId" to codeId))
            .await()
    }

    suspend fun redeemPairingCode(
        rawCode: String,
        deviceName: String,
        deviceModel: String,
        osVersion: String
    ): PairingRedemptionResult {
        val digits = rawCode.filter { it.isDigit() }
        if (digits.length != 6) {
            return PairingRedemptionResult(
                verified = false,
                errorMessage = "Enter the complete six-digit pairing code from Anchor Parent."
            )
        }

        return try {
            val response = functions.getHttpsCallable("redeemPairingCode")
                .call(
                    mapOf(
                        "code" to digits,
                        "deviceName" to deviceName,
                        "deviceModel" to deviceModel,
                        "osVersion" to osVersion
                    )
                ).await()
            val data = response.data as? Map<*, *>
                ?: throw IllegalStateException("Pairing service returned an invalid response.")
            PairingRedemptionResult(
                verified = data["verified"] as? Boolean == true,
                familyId = data["familyId"] as? String ?: "",
                ownerUid = data["ownerUid"] as? String ?: "",
                childId = data["childId"] as? String ?: "child_leo",
                childName = data["childName"] as? String ?: "Child",
                codeId = data["codeId"] as? String ?: "",
                errorMessage = if (data["verified"] as? Boolean == true) null
                    else "The pairing service could not verify this code.",
                serverResponded = true
            )
        } catch (e: Exception) {
            PairingRedemptionResult(
                verified = false,
                errorMessage = e.localizedMessage ?: "Pairing verification failed. Please try again."
            )
        }
    }

    suspend fun verifyPairingSession(
        familyId: String,
        childId: String
    ): PairingRedemptionResult {
        return try {
            val response = functions.getHttpsCallable("verifyPairingSession")
                .call(mapOf("familyId" to familyId, "childId" to childId))
                .await()
            val data = response.data as? Map<*, *>
                ?: throw IllegalStateException("Pairing service returned an invalid response.")
            val verified = data["verified"] as? Boolean == true
            PairingRedemptionResult(
                verified = verified,
                familyId = data["familyId"] as? String ?: "",
                ownerUid = data["ownerUid"] as? String ?: "",
                childId = data["childId"] as? String ?: childId,
                childName = data["childName"] as? String ?: "Child",
                codeId = data["codeId"] as? String ?: "",
                errorMessage = if (verified) null else "This device is no longer paired to this family.",
                serverResponded = true
            )
        } catch (e: Exception) {
            PairingRedemptionResult(
                verified = false,
                errorMessage = e.localizedMessage ?: "Could not verify the saved pairing session."
            )
        }
    }

    fun observePairedDeviceMembership(
        familyId: String,
        childUid: String
    ): Flow<Boolean> = callbackFlow {
        val path = "families/$familyId/pairedDevices/$childUid"
        val registration = firestore.document(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.metadata.isFromCache) {
                val active = snapshot.exists() &&
                    snapshot.getString("status") == "PAIRED" &&
                    snapshot.getString("childUid") == childUid &&
                    snapshot.getString("familyId") == familyId
                trySend(active)
            }
        }
        awaitClose { registration.remove() }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun claimPairingCode(
        uid: String,
        code: String,
        childId: String,
        deviceName: String,
        deviceModel: String,
        osVersion: String
    ): Boolean = redeemPairingCode(
        rawCode = code,
        deviceName = deviceName,
        deviceModel = deviceModel,
        osVersion = osVersion
    ).verified

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
                            .setProjectId("demo-anchor-local")
                            .setApplicationId("1:000000000000:android:0000000000000000")
                            .setApiKey("AIzaSyDummyKeyForLocalRobolectricTest")
                            .build()
                    )
            }
            return FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        }
    }
}
