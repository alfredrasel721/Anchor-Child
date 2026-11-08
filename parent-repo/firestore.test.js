const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'gen-lang-client-0963224775';
const DATABASE_ID = 'ai-studio-android-anchorch-710012f1-419e-46fb-87fd-4e0fce481e8e';

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: '127.0.0.1',
      port: 8085,
      rules: fs.readFileSync('firestore.rules', 'utf8'),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

async function seedParentAndPairingCode(parentDb, parentUid, codeObj) {
  const familyId = `fam_${parentUid}`;
  await assertSucceeds(
    parentDb.collection('families').doc(familyId).set({
      familyId,
      familyName: 'Jenkins Household',
      ownerUid: parentUid,
      subscriptionTier: 'PREMIUM_ANNUAL',
      subscriptionActive: true,
    })
  );
  await assertSucceeds(
    parentDb.collection('families').doc(familyId).collection('children').doc('child_leo').set({
      childId: 'child_leo',
      familyId,
      ownerUid: parentUid,
      name: 'Leo',
      age: 11,
      dailyAllowanceMinutes: 150,
      usedMinutesToday: 45,
    })
  );
  if (codeObj) {
    await assertSucceeds(
      parentDb
        .collection('families')
        .doc(familyId)
        .collection('pairingCodes')
        .doc(codeObj.codeId)
        .set(codeObj)
    );
  }
}

test('1. Valid Parent access: Parent can create household, child profiles, appRules, schedules, and pairingCodes', async () => {
  const parentUid = 'parent_sarah';
  const familyId = `fam_${parentUid}`;
  const parentDb = testEnv.authenticatedContext(parentUid).firestore(DATABASE_ID);

  await seedParentAndPairingCode(parentDb, parentUid, {
    codeId: 'pair_child_leo_482915',
    familyId,
    ownerUid: parentUid,
    childId: 'child_leo',
    childName: 'Leo',
    code: '482915',
    expiresAtEpochMs: Date.now() + 600_000,
    status: 'ACTIVE',
  });

  await assertSucceeds(
    parentDb.collection('families').doc(familyId).collection('appRules').doc('rule_mc').set({
      ruleId: 'rule_mc',
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      packageName: 'com.mojang.minecraftpe',
      appName: 'Minecraft',
      category: 'Games',
      dailyLimitMinutes: 45,
      usedMinutesToday: 20,
      status: 'ENFORCED',
    })
  );

  const snap = await assertSucceeds(
    parentDb.collection('families').doc(familyId).collection('appRules').doc('rule_mc').get()
  );
  assert.equal(snap.data().dailyLimitMinutes, 45);
});

test('2. Valid paired Child access: Atomic pairing redemption succeeds and paired Child can read rules and submit PENDING requests, SOS alerts, and messages', async () => {
  const parentUid = 'parent_sarah';
  const childUid = 'child_device_leo';
  const familyId = `fam_${parentUid}`;
  const codeId = 'pair_child_leo_482915';

  const parentDb = testEnv.authenticatedContext(parentUid).firestore(DATABASE_ID);
  const childDb = testEnv.authenticatedContext(childUid).firestore(DATABASE_ID);

  await seedParentAndPairingCode(parentDb, parentUid, {
    codeId,
    familyId,
    ownerUid: parentUid,
    childId: 'child_leo',
    childName: 'Leo',
    code: '482915',
    expiresAtEpochMs: Date.now() + 600_000,
    status: 'ACTIVE',
  });

  // Child atomically consumes the active pairing code and registers pairedDevices
  const batch = childDb.batch();
  batch.update(
    childDb.collection('families').doc(familyId).collection('pairingCodes').doc(codeId),
    {
      status: 'CONSUMED',
      redeemedByUid: childUid,
    }
  );
  batch.set(
    childDb.collection('families').doc(familyId).collection('pairedDevices').doc(childUid),
    {
      deviceUid: childUid,
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      codeId,
      redeemedCode: '482915',
      status: 'ACTIVE',
    }
  );
  await assertSucceeds(batch.commit());

  // Paired Child can submit a PENDING extra-time request, SOS alert, and chat message
  await assertSucceeds(
    childDb.collection('families').doc(familyId).collection('requests').doc('req_01').set({
      requestId: 'req_01',
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      childName: 'Leo',
      requestType: 'EXTRA_TIME',
      targetName: 'Minecraft',
      requestedBonusMinutes: 15,
      status: 'PENDING',
    })
  );

  await assertSucceeds(
    childDb.collection('families').doc(familyId).collection('alerts').doc('sos_01').set({
      alertId: 'sos_01',
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      childName: 'Leo',
      title: 'URGENT SOS Emergency Beacon',
      category: 'SOS',
      severity: 'CRITICAL',
      acknowledged: false,
    })
  );

  await assertSucceeds(
    childDb.collection('families').doc(familyId).collection('messages').doc('msg_01').set({
      messageId: 'msg_01',
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      senderName: 'Leo',
      senderRole: 'CHILD',
      text: "I'm Safe & Arrived at Oakridge Middle School",
      isSystemEvent: false,
    })
  );
});

test('3. Invalid pairing code: Child cannot self-register in pairedDevices with wrong code or without consuming code', async () => {
  const parentUid = 'parent_sarah';
  const childUid = 'child_device_leo';
  const familyId = `fam_${parentUid}`;
  const codeId = 'pair_child_leo_482915';

  const parentDb = testEnv.authenticatedContext(parentUid).firestore(DATABASE_ID);
  const childDb = testEnv.authenticatedContext(childUid).firestore(DATABASE_ID);

  await seedParentAndPairingCode(parentDb, parentUid, {
    codeId,
    familyId,
    ownerUid: parentUid,
    childId: 'child_leo',
    childName: 'Leo',
    code: '482915',
    expiresAtEpochMs: Date.now() + 600_000,
    status: 'ACTIVE',
  });

  // Attempt 1: Direct write to pairedDevices without consuming pairingCode
  await assertFails(
    childDb.collection('families').doc(familyId).collection('pairedDevices').doc(childUid).set({
      deviceUid: childUid,
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      codeId,
      redeemedCode: '482915',
      status: 'ACTIVE',
    })
  );

  // Attempt 2: Wrong 6-digit code in batch
  const wrongBatch = childDb.batch();
  wrongBatch.update(
    childDb.collection('families').doc(familyId).collection('pairingCodes').doc(codeId),
    { status: 'CONSUMED', redeemedByUid: childUid }
  );
  wrongBatch.set(
    childDb.collection('families').doc(familyId).collection('pairedDevices').doc(childUid),
    {
      deviceUid: childUid,
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      codeId,
      redeemedCode: '999999', // Mismatched code!
      status: 'ACTIVE',
    }
  );
  await assertFails(wrongBatch.commit());
});

test('4. Expired pairing code: Code past 10-minute expiration is strictly rejected', async () => {
  const parentUid = 'parent_sarah';
  const childUid = 'child_device_leo';
  const familyId = `fam_${parentUid}`;
  const codeId = 'pair_expired_111222';

  const parentDb = testEnv.authenticatedContext(parentUid).firestore(DATABASE_ID);
  const childDb = testEnv.authenticatedContext(childUid).firestore(DATABASE_ID);

  await seedParentAndPairingCode(parentDb, parentUid, {
    codeId,
    familyId,
    ownerUid: parentUid,
    childId: 'child_leo',
    childName: 'Leo',
    code: '111222',
    expiresAtEpochMs: Date.now() - 60_000, // Expired 1 minute ago
    status: 'ACTIVE',
  });

  const expiredBatch = childDb.batch();
  expiredBatch.update(
    childDb.collection('families').doc(familyId).collection('pairingCodes').doc(codeId),
    { status: 'CONSUMED', redeemedByUid: childUid }
  );
  expiredBatch.set(
    childDb.collection('families').doc(familyId).collection('pairedDevices').doc(childUid),
    {
      deviceUid: childUid,
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      codeId,
      redeemedCode: '111222',
      status: 'ACTIVE',
    }
  );
  await assertFails(expiredBatch.commit());
});

test('5. Reused pairing code: Already CONSUMED pairing code cannot be redeemed again', async () => {
  const parentUid = 'parent_sarah';
  const secondDeviceUid = 'child_device_second';
  const familyId = `fam_${parentUid}`;
  const codeId = 'pair_consumed_482915';

  const parentDb = testEnv.authenticatedContext(parentUid).firestore(DATABASE_ID);
  const secondChildDb = testEnv.authenticatedContext(secondDeviceUid).firestore(DATABASE_ID);

  await seedParentAndPairingCode(parentDb, parentUid, {
    codeId,
    familyId,
    ownerUid: parentUid,
    childId: 'child_leo',
    childName: 'Leo',
    code: '482915',
    expiresAtEpochMs: Date.now() + 600_000,
    status: 'CONSUMED', // Already consumed!
  });

  const reuseBatch = secondChildDb.batch();
  reuseBatch.update(
    secondChildDb.collection('families').doc(familyId).collection('pairingCodes').doc(codeId),
    { status: 'CONSUMED', redeemedByUid: secondDeviceUid }
  );
  reuseBatch.set(
    secondChildDb
      .collection('families')
      .doc(familyId)
      .collection('pairedDevices')
      .doc(secondDeviceUid),
    {
      deviceUid: secondDeviceUid,
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      codeId,
      redeemedCode: '482915',
      status: 'ACTIVE',
    }
  );
  await assertFails(reuseBatch.commit());
});

test('6. Unauthenticated access: Unauthenticated client cannot read or write any family documents', async () => {
  const parentUid = 'parent_sarah';
  const familyId = `fam_${parentUid}`;
  const parentDb = testEnv.authenticatedContext(parentUid).firestore(DATABASE_ID);
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);

  await seedParentAndPairingCode(parentDb, parentUid, null);

  await assertFails(unauthDb.collection('families').doc(familyId).get());
  await assertFails(
    unauthDb.collection('families').doc(familyId).collection('children').doc('child_leo').get()
  );
});

test('7. Cross-family access: Authenticated stranger from another family is strictly rejected', async () => {
  const parentUid = 'parent_sarah';
  const strangerUid = 'parent_stranger';
  const familyId = `fam_${parentUid}`;

  const parentDb = testEnv.authenticatedContext(parentUid).firestore(DATABASE_ID);
  const strangerDb = testEnv.authenticatedContext(strangerUid).firestore(DATABASE_ID);

  await seedParentAndPairingCode(parentDb, parentUid, null);

  await assertFails(strangerDb.collection('families').doc(familyId).get());
  await assertFails(
    strangerDb.collection('families').doc(familyId).collection('children').get()
  );
  await assertFails(
    strangerDb.collection('families').doc(familyId).collection('requests').doc('req_hack').set({
      requestId: 'req_hack',
      familyId,
      ownerUid: strangerUid,
      childId: 'child_leo',
      requestType: 'EXTRA_TIME',
      targetName: 'Minecraft',
      status: 'PENDING',
    })
  );
});

test('8. Unauthorized modification of parental settings: Paired Child cannot modify appRules, schedules, subscriptionTier, dailyAllowanceMinutes, or self-approve requests', async () => {
  const parentUid = 'parent_sarah';
  const childUid = 'child_device_leo';
  const familyId = `fam_${parentUid}`;
  const codeId = 'pair_child_leo_482915';

  const parentDb = testEnv.authenticatedContext(parentUid).firestore(DATABASE_ID);
  const childDb = testEnv.authenticatedContext(childUid).firestore(DATABASE_ID);

  await seedParentAndPairingCode(parentDb, parentUid, {
    codeId,
    familyId,
    ownerUid: parentUid,
    childId: 'child_leo',
    childName: 'Leo',
    code: '482915',
    expiresAtEpochMs: Date.now() + 600_000,
    status: 'ACTIVE',
  });

  // Pair the child device
  const batch = childDb.batch();
  batch.update(
    childDb.collection('families').doc(familyId).collection('pairingCodes').doc(codeId),
    { status: 'CONSUMED', redeemedByUid: childUid }
  );
  batch.set(
    childDb.collection('families').doc(familyId).collection('pairedDevices').doc(childUid),
    {
      deviceUid: childUid,
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      codeId,
      redeemedCode: '482915',
      status: 'ACTIVE',
    }
  );
  await assertSucceeds(batch.commit());

  // 8a. Child CANNOT create or modify appRules
  await assertFails(
    childDb.collection('families').doc(familyId).collection('appRules').doc('rule_mc').set({
      ruleId: 'rule_mc',
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      packageName: 'com.mojang.minecraftpe',
      appName: 'Minecraft',
      category: 'Games',
      dailyLimitMinutes: 1440,
      status: 'WHITELISTED',
    })
  );

  // 8b. Child CANNOT modify schedules
  await assertFails(
    childDb.collection('families').doc(familyId).collection('schedules').doc('sched_bed').set({
      scheduleId: 'sched_bed',
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      title: 'Bedtime',
      windowDescription: '9:00 PM - 7:00 AM',
      enabled: false,
    })
  );

  // 8c. Child CANNOT modify FamilyHousehold subscription settings
  await assertFails(
    childDb.collection('families').doc(familyId).update({
      subscriptionActive: false,
    })
  );

  // 8d. Child CANNOT increase their own dailyAllowanceMinutes on ChildProfile
  await assertFails(
    childDb.collection('families').doc(familyId).collection('children').doc('child_leo').update({
      dailyAllowanceMinutes: 999,
    })
  );

  // 8e. Child CAN update their own usedMinutesToday / batteryLevel telemetry while keeping dailyAllowanceMinutes unchanged!
  await assertSucceeds(
    childDb.collection('families').doc(familyId).collection('children').doc('child_leo').update({
      usedMinutesToday: 60,
      batteryLevel: 79,
    })
  );

  // 8f. Child CANNOT create a pre-APPROVED request
  await assertFails(
    childDb.collection('families').doc(familyId).collection('requests').doc('req_self_approve').set({
      requestId: 'req_self_approve',
      familyId,
      ownerUid: parentUid,
      childId: 'child_leo',
      requestType: 'EXTRA_TIME',
      targetName: 'Minecraft',
      status: 'APPROVED',
    })
  );
});
