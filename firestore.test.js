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

test('1. Authenticated family owner can create and read household and child profile', async () => {
  const uid = 'parent_123';
  const familyId = `fam_${uid}`;
  const db = testEnv.authenticatedContext(uid).firestore(DATABASE_ID);

  await assertSucceeds(
    db.collection('families').doc(familyId).set({
      familyId,
      familyName: 'Miller Family',
      ownerUid: uid,
      subscriptionTier: 'PREMIUM_ANNUAL',
      subscriptionActive: true,
    })
  );

  await assertSucceeds(
    db.collection('families').doc(familyId).collection('children').doc('leo_01').set({
      childId: 'leo_01',
      familyId,
      ownerUid: uid,
      name: 'Leo',
      age: 11,
      dailyAllowanceMinutes: 150,
      usedMinutesToday: 80,
    })
  );

  const snap = await assertSucceeds(
    db.collection('families').doc(familyId).collection('children').doc('leo_01').get()
  );
  assert.equal(snap.data().name, 'Leo');
});

test('2. Paired child device can submit extra-time request, SOS alert, and chat message', async () => {
  const uid = 'parent_123';
  const familyId = `fam_${uid}`;
  const db = testEnv.authenticatedContext(uid).firestore(DATABASE_ID);

  await assertSucceeds(
    db.collection('families').doc(familyId).collection('requests').doc('req_1').set({
      requestId: 'req_1',
      familyId,
      ownerUid: uid,
      childId: 'leo_01',
      childName: 'Leo',
      requestType: 'EXTRA_TIME',
      targetName: 'Minecraft',
      requestedBonusMinutes: 30,
      status: 'PENDING',
    })
  );

  await assertSucceeds(
    db.collection('families').doc(familyId).collection('alerts').doc('alert_sos_1').set({
      alertId: 'alert_sos_1',
      familyId,
      ownerUid: uid,
      childId: 'leo_01',
      childName: 'Leo',
      title: 'SOS Emergency Beacon Triggered',
      category: 'SOS',
      severity: 'CRITICAL',
      acknowledged: false,
    })
  );

  await assertSucceeds(
    db.collection('families').doc(familyId).collection('messages').doc('msg_1').set({
      messageId: 'msg_1',
      familyId,
      ownerUid: uid,
      childId: 'leo_01',
      senderName: 'Leo',
      senderRole: 'CHILD',
      text: "I'm Safe & Arrived at Lincoln Middle School",
      isSystemEvent: false,
    })
  );
});

test('3. Cross-account paired child device can access household after registering pairedDevices', async () => {
  const parentUid = 'parent_999';
  const childUid = 'child_device_777';
  const familyId = `fam_${parentUid}`;

  const parentDb = testEnv.authenticatedContext(parentUid).firestore(DATABASE_ID);
  const childDb = testEnv.authenticatedContext(childUid).firestore(DATABASE_ID);

  await assertSucceeds(
    parentDb.collection('families').doc(familyId).set({
      familyId,
      familyName: 'Miller Family',
      ownerUid: parentUid,
      subscriptionTier: 'PREMIUM_ANNUAL',
      subscriptionActive: true,
    })
  );

  await assertSucceeds(
    childDb.collection('families').doc(familyId).collection('pairedDevices').doc(childUid).set({
      deviceUid: childUid,
      familyId,
      childId: 'leo_01',
    })
  );

  await assertSucceeds(
    childDb.collection('families').doc(familyId).collection('requests').doc('req_cross_1').set({
      requestId: 'req_cross_1',
      familyId,
      ownerUid: parentUid,
      childId: 'leo_01',
      requestType: 'EXTRA_TIME',
      targetName: 'Roblox',
      status: 'PENDING',
    })
  );

  await assertSucceeds(
    childDb.collection('families').doc(familyId).collection('requests').get()
  );
});

test('4. Unauthenticated user and third-party attacker are strictly denied', async () => {
  const ownerUid = 'parent_123';
  const attackerUid = 'attacker_999';
  const familyId = `fam_${ownerUid}`;

  const ownerDb = testEnv.authenticatedContext(ownerUid).firestore(DATABASE_ID);
  const attackerDb = testEnv.authenticatedContext(attackerUid).firestore(DATABASE_ID);
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);

  await assertSucceeds(
    ownerDb.collection('families').doc(familyId).set({
      familyId,
      familyName: 'Protected Household',
      ownerUid,
      subscriptionTier: 'FREE',
      subscriptionActive: true,
    })
  );

  await assertFails(unauthDb.collection('families').doc(familyId).get());
  await assertFails(attackerDb.collection('families').doc(familyId).get());
  await assertFails(
    attackerDb.collection('families').doc(familyId).collection('requests').doc('hack_req').set({
      requestId: 'hack_req',
      familyId,
      ownerUid: attackerUid,
      childId: 'leo_01',
      requestType: 'EXTRA_TIME',
      targetName: 'Minecraft',
      status: 'APPROVED',
    })
  );
});
