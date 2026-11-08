/**
 * Anchor Family — Trusted Firebase Cloud Functions Backend
 * Project: gen-lang-client-0963224775
 * Shared Database: ai-studio-android-anchorch-710012f1-419e-46fb-87fd-4e0fce481e8e
 *
 * Implements:
 * 1. createPairingCode — Parent-only cryptographic 6-digit one-time code generation (10-min TTL)
 * 2. redeemPairingCode — Child authenticated verification, brute-force rate limit, atomic consumption & pairedDevices registration
 * 3. revokePairingCode — Parent-only revocation of active pairing codes or paired child devices
 * 4. onSafetyAlertCreated — Real-time FCM emergency notification dispatch to Parent devices on SOS
 */

const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');
const crypto = require('node:crypto');

admin.initializeApp();

const DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  'ai-studio-android-anchorch-710012f1-419e-46fb-87fd-4e0fce481e8e';

function getDb() {
  return admin.firestore(DATABASE_ID);
}

const TEN_MINUTES_MS = 10 * 60 * 1000;
const MAX_PAIRING_ATTEMPTS_PER_WINDOW = 5;
const RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000;

/**
 * 1. createPairingCode
 * Callable by the authenticated Family Owner (Parent).
 */
exports.createPairingCode = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError('unauthenticated', 'Parent authentication is required.');
  }

  const uid = request.auth.uid;
  const { familyId, childId, childName } = request.data || {};
  const expectedFamilyId = `fam_${uid}`;

  if (!familyId || familyId !== expectedFamilyId) {
    throw new HttpsError(
      'permission-denied',
      'Only the authenticated family owner can generate pairing codes for this household.'
    );
  }
  if (!childId || typeof childId !== 'string') {
    throw new HttpsError('invalid-argument', 'A valid childId is required.');
  }

  const db = getDb();
  const nowMs = Date.now();
  const expiresAtEpochMs = nowMs + TEN_MINUTES_MS;

  // Cryptographically secure 6-digit code
  const rawDigits = crypto.randomInt(100000, 1000000).toString();
  const formattedCode = `${rawDigits.slice(0, 3)}-${rawDigits.slice(3)}`;
  const codeId = `pair_${childId}_${rawDigits}`;

  const pairingDoc = {
    codeId,
    familyId,
    ownerUid: uid,
    childId,
    childName: childName || 'Child',
    code: rawDigits,
    formattedCode,
    expiresAtEpochMs,
    status: 'ACTIVE',
    attemptCount: 0,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  const batch = db.batch();
  // Revoke any previous ACTIVE codes for the same childId
  const existingCodes = await db
    .collection('families')
    .doc(familyId)
    .collection('pairingCodes')
    .where('childId', '==', childId)
    .where('status', '==', 'ACTIVE')
    .get();

  existingCodes.docs.forEach((docSnap) => {
    batch.update(docSnap.ref, {
      status: 'EXPIRED',
      revokedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  const codeRef = db
    .collection('families')
    .doc(familyId)
    .collection('pairingCodes')
    .doc(codeId);
  batch.set(codeRef, pairingDoc);

  await batch.commit();

  return {
    codeId,
    familyId,
    childId,
    code: rawDigits,
    formattedCode,
    expiresAtEpochMs,
    status: 'ACTIVE',
  };
});

/**
 * 2. redeemPairingCode
 * Callable by the authenticated Child device.
 * Enforces rate-limiting, validates expiration & single-use status, atomically marks CONSUMED,
 * and registers `/families/{familyId}/pairedDevices/{childUid}`.
 */
exports.redeemPairingCode = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError('unauthenticated', 'Child device must be signed in before pairing.');
  }

  const childUid = request.auth.uid;
  const { familyId, code, deviceName, deviceModel, osVersion } = request.data || {};
  const normalizedCode = String(code || '').replace(/[^0-9]/g, '');

  if (!familyId || normalizedCode.length !== 6) {
    throw new HttpsError(
      'invalid-argument',
      'A valid familyId and 6-digit pairing code are required.'
    );
  }

  const db = getDb();
  const rateLimitRef = db.collection('pairingRateLimits').doc(childUid);

  return await db.runTransaction(async (tx) => {
    const nowMs = Date.now();
    const rateSnap = await tx.get(rateLimitRef);
    let attempts = 0;
    let windowStart = nowMs;

    if (rateSnap.exists) {
      const rateData = rateSnap.data();
      if (nowMs - (rateData.windowStartMs || 0) < RATE_LIMIT_WINDOW_MS) {
        attempts = rateData.attempts || 0;
        windowStart = rateData.windowStartMs;
      }
    }

    if (attempts >= MAX_PAIRING_ATTEMPTS_PER_WINDOW) {
      throw new HttpsError(
        'resource-exhausted',
        'Too many pairing attempts. Please wait 15 minutes and generate a new code.'
      );
    }

    const codesQuery = db
      .collection('families')
      .doc(familyId)
      .collection('pairingCodes')
      .where('code', '==', normalizedCode)
      .limit(1);

    const codesSnap = await tx.get(codesQuery);
    if (codesSnap.empty) {
      tx.set(rateLimitRef, {
        uid: childUid,
        attempts: attempts + 1,
        windowStartMs: windowStart,
        lastAttemptMs: nowMs,
      });
      throw new HttpsError('not-found', 'Invalid pairing code.');
    }

    const codeDoc = codesSnap.docs[0];
    const codeData = codeDoc.data();

    if (codeData.status !== 'ACTIVE') {
      throw new HttpsError(
        'failed-precondition',
        `Pairing code has already been ${codeData.status.toLowerCase()}.`
      );
    }

    if (Number(codeData.expiresAtEpochMs || 0) <= nowMs) {
      tx.update(codeDoc.ref, { status: 'EXPIRED' });
      throw new HttpsError('deadline-exceeded', 'Pairing code has expired (10-minute limit).');
    }

    const childId = codeData.childId;
    const ownerUid = codeData.ownerUid;

    // 1. Atomically consume the pairing code
    tx.update(codeDoc.ref, {
      status: 'CONSUMED',
      redeemedByUid: childUid,
      redeemedAtEpochMs: nowMs,
    });

    // 2. Register the paired child device membership record
    const pairedDeviceRef = db
      .collection('families')
      .doc(familyId)
      .collection('pairedDevices')
      .doc(childUid);

    tx.set(pairedDeviceRef, {
      deviceUid: childUid,
      familyId,
      ownerUid,
      childId,
      codeId: codeDoc.id,
      redeemedCode: normalizedCode,
      deviceName: deviceName || 'Android Child Device',
      deviceModel: deviceModel || 'Android',
      osVersion: osVersion || 'Android',
      status: 'ACTIVE',
      pairedAtEpochMs: nowMs,
    });

    // 3. Update the child profile telemetry & sync status
    const childRef = db
      .collection('families')
      .doc(familyId)
      .collection('children')
      .doc(childId);

    tx.set(
      childRef,
      {
        syncStatus: 'ENFORCED',
        deviceName: deviceName || 'Android Child Device',
        deviceModel: deviceModel || 'Android',
        osVersion: osVersion || 'Android',
        lastSyncLabel: 'Paired & Verified via Anchor Cloud',
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    // Reset rate limit counter on success
    tx.delete(rateLimitRef);

    return {
      verified: true,
      familyId,
      ownerUid,
      childId,
      childName: codeData.childName || 'Leo',
      codeId: codeDoc.id,
    };
  });
});

/**
 * 3. revokePairingCode
 * Callable by the authenticated Family Owner (Parent) to revoke an active code or unpair a device.
 */
exports.revokePairingCode = onCall(async (request) => {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError('unauthenticated', 'Parent authentication is required.');
  }

  const uid = request.auth.uid;
  const { familyId, codeId, deviceUid } = request.data || {};
  if (!familyId || familyId !== `fam_${uid}`) {
    throw new HttpsError('permission-denied', 'Not authorized for this family household.');
  }

  const db = getDb();
  const batch = db.batch();

  if (codeId) {
    const codeRef = db
      .collection('families')
      .doc(familyId)
      .collection('pairingCodes')
      .doc(codeId);
    batch.update(codeRef, {
      status: 'EXPIRED',
      revokedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }

  if (deviceUid) {
    const devRef = db
      .collection('families')
      .doc(familyId)
      .collection('pairedDevices')
      .doc(deviceUid);
    batch.delete(devRef);
  }

  await batch.commit();
  return { revoked: true };
});

/**
 * 4. onSafetyAlertCreated
 * Dispatches a high-priority FCM emergency push notification to parent devices when an SOS alert is created.
 */
exports.onSafetyAlertCreated = onDocumentCreated(
  {
    document: 'families/{familyId}/alerts/{alertId}',
    database: DATABASE_ID,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const alert = snap.data();
    if (!alert || alert.category !== 'SOS') return;

    const { familyId, childName, title, description } = alert;
    const message = {
      topic: `family_sos_${familyId}`,
      notification: {
        title: title || `URGENT SOS Alert from ${childName || 'Child'}`,
        body: description || 'Open Anchor Parent immediately to view live GPS location.',
      },
      android: {
        priority: 'high',
      },
      data: {
        familyId: String(familyId || ''),
        alertId: String(event.params.alertId || ''),
        category: 'SOS',
        severity: String(alert.severity || 'CRITICAL'),
      },
    };

    try {
      await admin.messaging().send(message);
    } catch (err) {
      console.error('FCM SOS notification dispatch failed:', err);
    }
  }
);
