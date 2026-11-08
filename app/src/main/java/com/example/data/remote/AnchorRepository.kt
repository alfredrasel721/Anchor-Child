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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AnchorRepository(
    private val firestore: FirebaseFirestore
) {
    constructor(context: Context) : this(createDefaultFirestore(context))



    fun familyIdFor(uid: String): String = "fam_$uid"

    private fun familyDoc(uid: String) =
        firestore.collection("families").document(familyIdFor(uid))

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
                    displayName = displayName.ifBlank { "Leo Miller (Child Device)" },
                    email = email.ifBlank { "leo.device@anchor.family" },
                    role = "CHILD_DEVICE",
                    familyId = familyId,
                    notificationCadence = "Immediate",
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                userRef.set(profile).await()
            }

            val famRef = familyDoc(uid)
            val existingFam = famRef.get().await()
            if (!existingFam.exists()) {
                val batch = firestore.batch()
                batch.set(famRef, AnchorSeedData.defaultHousehold(uid))

                AnchorSeedData.defaultChildren(uid).forEach { child ->
                    batch.set(famRef.collection("children").document(child.childId), child)
                }
                AnchorSeedData.defaultPairingCodes(uid).forEach { code ->
                    batch.set(famRef.collection("pairingCodes").document(code.codeId), code)
                }
                AnchorSeedData.defaultAppRules(uid).forEach { rule ->
                    batch.set(famRef.collection("appRules").document(rule.ruleId), rule)
                }
                AnchorSeedData.defaultSchedules(uid).forEach { schedule ->
                    batch.set(famRef.collection("schedules").document(schedule.scheduleId), schedule)
                }
                AnchorSeedData.defaultGeofences(uid).forEach { geo ->
                    batch.set(famRef.collection("geofences").document(geo.geofenceId), geo)
                }
                AnchorSeedData.defaultAlerts(uid).forEach { alert ->
                    batch.set(famRef.collection("alerts").document(alert.alertId), alert)
                }
                AnchorSeedData.defaultRequests(uid).forEach { req ->
                    batch.set(famRef.collection("requests").document(req.requestId), req)
                }
                AnchorSeedData.defaultMessages(uid).forEach { msg ->
                    batch.set(famRef.collection("messages").document(msg.messageId), msg)
                }
                AnchorSeedData.defaultTasks(uid).forEach { task ->
                    batch.set(famRef.collection("tasks").document(task.taskId), task)
                }
                AnchorSeedData.defaultGuardians(uid).forEach { guardian ->
                    batch.set(famRef.collection("guardians").document(guardian.guardianId), guardian)
                }
                val prem = AnchorSeedData.defaultPremiumConfig(uid)
                batch.set(famRef.collection("premiumConfigs").document(prem.configId), prem)

                batch.commit().await()
            }
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, userPath)
            throw e
        }
    }

    fun observeHousehold(uid: String): Flow<FamilyHousehold?> = callbackFlow {
        val path = "families/${familyIdFor(uid)}"
        val registration = familyDoc(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                FirestoreErrorHandler.handleFirestoreError(error, OperationType.GET, path)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(FamilyHousehold::class.java))
        }
        awaitClose { registration.remove() }
    }

    fun observeChildren(uid: String): Flow<List<ChildProfile>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/children"
        val registration = familyDoc(uid).collection("children")
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

    fun observePairingCodes(uid: String): Flow<List<PairingCode>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/pairingCodes"
        val registration = familyDoc(uid).collection("pairingCodes")
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

    fun observeAppRules(uid: String): Flow<List<AppRule>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/appRules"
        val registration = familyDoc(uid).collection("appRules")
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

    fun observeSchedules(uid: String): Flow<List<ScheduleRoutine>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/schedules"
        val registration = familyDoc(uid).collection("schedules")
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

    fun observeGeofences(uid: String): Flow<List<SafeGeofence>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/geofences"
        val registration = familyDoc(uid).collection("geofences")
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

    fun observeAlerts(uid: String): Flow<List<SafetyAlert>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/alerts"
        val registration = familyDoc(uid).collection("alerts")
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

    fun observeRequests(uid: String): Flow<List<ChildRequest>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/requests"
        val registration = familyDoc(uid).collection("requests")
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

    fun observeMessages(uid: String): Flow<List<FamilyMessage>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/messages"
        val registration = familyDoc(uid).collection("messages")
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

    fun observeTasks(uid: String): Flow<List<FamilyTask>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/tasks"
        val registration = familyDoc(uid).collection("tasks")
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

    fun observeGuardians(uid: String): Flow<List<GuardianContact>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/guardians"
        val registration = familyDoc(uid).collection("guardians")
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

    fun observePremiumConfigs(uid: String): Flow<List<PremiumFilterConfig>> = callbackFlow {
        val path = "families/${familyIdFor(uid)}/premiumConfigs"
        val registration = familyDoc(uid).collection("premiumConfigs")
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
    // Child App Mutations & Bilateral Sync Operations
    // ========================================================================

    suspend fun claimPairingCode(
        uid: String,
        code: String,
        childId: String,
        deviceName: String,
        deviceModel: String,
        osVersion: String
    ): Boolean {
        val familyId = familyIdFor(uid)
        val path = "families/$familyId/pairingCodes"
        return try {
            val codesSnap = familyDoc(uid).collection("pairingCodes").get().await()
            val matching = codesSnap.documents.mapNotNull {
                it.toObject(PairingCode::class.java)
            }.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }

            val targetCodeId = matching?.codeId ?: "pair_${childId}_${code.trim()}"
            val consumedCode = PairingCode(
                codeId = targetCodeId,
                familyId = familyId,
                ownerUid = uid,
                childId = matching?.childId ?: childId,
                childName = matching?.childName ?: "Leo",
                code = code.trim(),
                expiresAtEpochMs = System.currentTimeMillis() + 86_400_000L,
                status = "CONSUMED"
            )
            familyDoc(uid).collection("pairingCodes")
                .document(targetCodeId)
                .set(consumedCode)
                .await()

            // Update child profile device metadata and sync state
            val childRef = familyDoc(uid).collection("children").document(consumedCode.childId)
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

            // Record bilateral system audit message
            sendFamilyMessage(
                uid = uid,
                childId = consumedCode.childId,
                senderName = "Anchor System",
                senderRole = "SYSTEM",
                text = "Device Paired: $deviceName ($osVersion) linked via code ${code.trim()}.",
                isSystemEvent = true
            )
            true
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            false
        }
    }

    suspend fun updateChildDeviceTelemetry(
        uid: String,
        child: ChildProfile
    ) {
        val path = "families/${familyIdFor(uid)}/children/${child.childId}"
        try {
            familyDoc(uid).collection("children")
                .document(child.childId)
                .set(
                    child.copy(
                        familyId = familyIdFor(uid),
                        ownerUid = uid,
                        lastSyncLabel = "Synced at ${nowTimeLabel()}",
                        updatedAt = Timestamp.now()
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun submitChildRequest(
        uid: String,
        childId: String,
        childName: String,
        requestType: String,
        targetName: String,
        targetCategory: String,
        requestedBonusMinutes: Int,
        childNote: String,
        usedTodaySummary: String
    ): ChildRequest {
        val familyId = familyIdFor(uid)
        val requestId = "req_${UUID.randomUUID().toString().take(8)}"
        val path = "families/$familyId/requests/$requestId"
        val req = ChildRequest(
            requestId = requestId,
            familyId = familyId,
            ownerUid = uid,
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
            decidedBy = ""
        )
        try {
            familyDoc(uid).collection("requests").document(requestId).set(req).await()
            sendFamilyMessage(
                uid = uid,
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
        uid: String,
        childId: String,
        childName: String,
        locationLabel: String,
        batteryLevel: Int,
        isPracticeDrill: Boolean = false
    ): SafetyAlert {
        val familyId = familyIdFor(uid)
        val alertId = "sos_${UUID.randomUUID().toString().take(8)}"
        val path = "families/$familyId/alerts/$alertId"
        val alert = SafetyAlert(
            alertId = alertId,
            familyId = familyId,
            ownerUid = uid,
            childId = childId,
            childName = childName,
            title = if (isPracticeDrill) {
                "SOS Practice Drill Completed ($childName)"
            } else {
                "URGENT SOS Emergency Beacon ($childName)"
            },
            description = "Location: $locationLabel • Battery: $batteryLevel% • Dispatched to Primary & Co-Parent Guardians.",
            category = "SOS",
            severity = "CRITICAL",
            timestampLabel = "Today, ${nowTimeLabel()}",
            acknowledged = false
        )
        try {
            familyDoc(uid).collection("alerts").document(alertId).set(alert).await()
            sendFamilyMessage(
                uid = uid,
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
        uid: String,
        childId: String,
        childName: String,
        checkInHeadline: String,
        locationLabel: String,
        batteryLevel: Int
    ): SafetyAlert {
        val familyId = familyIdFor(uid)
        val alertId = "checkin_${UUID.randomUUID().toString().take(8)}"
        val path = "families/$familyId/alerts/$alertId"
        val alert = SafetyAlert(
            alertId = alertId,
            familyId = familyId,
            ownerUid = uid,
            childId = childId,
            childName = childName,
            title = "$childName: $checkInHeadline",
            description = "Verified GPS Check-In at $locationLabel • Battery $batteryLevel%",
            category = "GEOFENCE",
            severity = "INFO",
            timestampLabel = "Today, ${nowTimeLabel()}",
            acknowledged = false
        )
        try {
            familyDoc(uid).collection("alerts").document(alertId).set(alert).await()
            sendFamilyMessage(
                uid = uid,
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
        uid: String,
        childId: String,
        senderName: String,
        senderRole: String,
        text: String,
        isSystemEvent: Boolean = false
    ): FamilyMessage {
        val familyId = familyIdFor(uid)
        val messageId = "msg_${UUID.randomUUID().toString().take(8)}"
        val path = "families/$familyId/messages/$messageId"
        val message = FamilyMessage(
            messageId = messageId,
            familyId = familyId,
            ownerUid = uid,
            childId = childId,
            senderName = senderName,
            senderRole = senderRole,
            text = text,
            isSystemEvent = isSystemEvent,
            timestampLabel = nowTimeLabel()
        )
        try {
            familyDoc(uid).collection("messages").document(messageId).set(message).await()
            return message
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun markTaskCompletedByChild(uid: String, task: FamilyTask) {
        val path = "families/${familyIdFor(uid)}/tasks/${task.taskId}"
        val nextCompleted = !task.completedByChild
        val nextStatus = if (nextCompleted) "READY_FOR_REVIEW" else "IN_PROGRESS"
        val updated = task.copy(
            familyId = familyIdFor(uid),
            ownerUid = uid,
            completedByChild = nextCompleted,
            status = nextStatus
        )
        try {
            familyDoc(uid).collection("tasks").document(task.taskId).set(updated).await()
            if (nextCompleted) {
                sendFamilyMessage(
                    uid = uid,
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
        uid: String,
        childId: String,
        childName: String,
        sessionId: String,
        modalityLabel: String,
        actionDescription: String
    ) {
        val familyId = familyIdFor(uid)
        val alertId = "live_${UUID.randomUUID().toString().take(8)}"
        val alert = SafetyAlert(
            alertId = alertId,
            familyId = familyId,
            ownerUid = uid,
            childId = childId,
            childName = childName,
            title = "Live Session ($modalityLabel): $actionDescription",
            description = "Audit ID: $sessionId • Logged transparently on both Parent & Child devices.",
            category = "SYSTEM",
            severity = "INFO",
            timestampLabel = "Today, ${nowTimeLabel()}",
            acknowledged = true
        )
        try {
            familyDoc(uid).collection("alerts").document(alertId).set(alert).await()
            sendFamilyMessage(
                uid = uid,
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
    // Parent-Compatible Helpers (Shared Contract & Rule Test Verification)
    // ========================================================================

    suspend fun createPairingCode(uid: String, childId: String, childName: String): PairingCode {
        val familyId = familyIdFor(uid)
        val codeStr = (100000..999999).random().toString()
        val codeId = "pair_${childId}_$codeStr"
        val pairing = PairingCode(
            codeId = codeId,
            familyId = familyId,
            ownerUid = uid,
            childId = childId,
            childName = childName,
            code = codeStr,
            expiresAtEpochMs = System.currentTimeMillis() + 900_000L,
            status = "ACTIVE"
        )
        familyDoc(uid).collection("pairingCodes").document(codeId).set(pairing).await()
        return pairing
    }

    suspend fun resolveChildRequest(
        uid: String,
        request: ChildRequest,
        approved: Boolean,
        decisionNote: String
    ) {
        val status = if (approved) "APPROVED" else "DECLINED"
        val updated = request.copy(
            familyId = familyIdFor(uid),
            ownerUid = uid,
            status = status,
            decisionSummary = decisionNote,
            decidedBy = "Parent Guardian"
        )
        familyDoc(uid).collection("requests").document(request.requestId).set(updated).await()
    }

    suspend fun saveAppRule(uid: String, rule: AppRule) {
        val updated = rule.copy(familyId = familyIdFor(uid), ownerUid = uid)
        familyDoc(uid).collection("appRules").document(rule.ruleId).set(updated).await()
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

