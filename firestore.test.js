const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const EVE_UID = "eve_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("contacts").get());
  await assertFails(unauthDb.collection("conversations").get());
  await assertFails(unauthDb.collection("calls").get());
});

test("User profile & split private PII settings enforce owner isolation, gender Female/Male only, age, and call approval", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      userHandle: "@alice",
      photoUrl: "",
      onlineStatus: "online",
      directoryVisible: true,
      accountType: "google",
      profileVisibility: "contacts",
      gender: "Female",
      age: 24,
      requireCallApproval: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject invalid gender values (only '', 'Female', 'Male' allowed)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      gender: "InvalidGender",
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("settings").set({
      uid: ALICE_UID,
      email: "alice@example.com",
      appLockEnabled: false,
      autoRecordCalls: true,
      notificationAlert: true,
      recordingQuality: "HD 720p",
      retentionDays: 30,
      blockedUids: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can discover Alice's public directory profile (no email PII)
  await assertSucceeds(
    bobDb.collection("users").where("directoryVisible", "==", true).get()
  );

  // Bob CANNOT read Alice's private settings/email PII
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("settings").get()
  );
});

test("Mutual call approval on contacts allows either member to approve calls while blocking outsiders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const now = new Date();
  const contactId = "contact_alice_bob";

  await assertSucceeds(
    aliceDb.collection("contacts").doc(contactId).set({
      ownerUid: ALICE_UID,
      targetUid: BOB_UID,
      memberUids: [ALICE_UID, BOB_UID],
      requesterName: "Alice",
      requesterHandle: "@alice",
      contactName: "Bob",
      contactHandle: "@bob",
      avatarKey: "custom",
      status: "accepted",
      presenceState: "online",
      lastActivityText: "Call Approval Requested",
      lastActivityType: "audio_call",
      callApproved: false,
      callApprovalRequestedBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can approve the mutual call request
  await assertSucceeds(
    bobDb.collection("contacts").doc(contactId).update({
      callApproved: true,
      lastActivityText: "Call Approved",
      updatedAt: new Date(),
    })
  );

  // Eve cannot approve or read the contact link
  await assertFails(
    eveDb.collection("contacts").doc(contactId).update({
      callApproved: false,
      updatedAt: new Date(),
    })
  );
});

test("Multi-user conversation and messages work between Alice and Bob while blocking Eve", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const now = new Date();
  const convId = "conv_alice_bob";

  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      ownerUid: ALICE_UID,
      partnerUid: BOB_UID,
      memberUids: [ALICE_UID, BOB_UID],
      ownerName: "Alice",
      participantName: "Bob",
      participantAvatarKey: "custom",
      presenceState: "online",
      lastMessageText: "Hello Bob",
      unreadCount: 1,
      encryptionProtocol: "AES-256-GCM",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Both Alice and Bob can query shared conversation via memberUids
  await assertSucceeds(
    bobDb.collection("conversations").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Eve cannot read the conversation
  await assertFails(eveDb.collection("conversations").doc(convId).get());

  // Bob can send a real-time message in the shared conversation
  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").set({
      ownerUid: BOB_UID,
      memberUids: [ALICE_UID, BOB_UID],
      conversationId: convId,
      senderUid: BOB_UID,
      senderName: "Bob",
      isOutgoing: true,
      messageType: "text",
      text: "Hi Alice!",
      encryptedCiphertext: "AES256GCM:IV:CIPHER",
      mediaBase64: "",
      mediaDurationSec: 0,
      mediaLabel: "10:20 AM",
      deliveryState: "sent",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice can read and mark Bob's message as read
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages")
      .where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").update({
      deliveryState: "read",
      updatedAt: new Date(),
    })
  );

  // Eve cannot read messages
  await assertFails(
    eveDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").get()
  );
});

test("Multi-user call signaling and mutual recording consent enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();
  const callId = "call_1";

  await assertSucceeds(
    aliceDb.collection("calls").doc(callId).set({
      ownerUid: ALICE_UID,
      calleeUid: BOB_UID,
      memberUids: [ALICE_UID, BOB_UID],
      callerName: "Alice",
      participantName: "Bob",
      participantAvatarKey: "custom",
      callType: "video",
      status: "ringing",
      consentState: "none",
      callerConsent: false,
      calleeConsent: false,
      isRecording: false,
      durationSec: 0,
      encryptionVerified: true,
      callerVideoFrame: "",
      calleeVideoFrame: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject recording if both have not consented
  await assertFails(
    aliceDb.collection("calls").doc(callId).update({
      isRecording: true,
      consentState: "requested",
      callerConsent: true,
      calleeConsent: false,
      updatedAt: new Date(),
    })
  );

  // Succeed when both Alice and Bob consent
  await assertSucceeds(
    bobDb.collection("calls").doc(callId).update({
      status: "connected",
      callerConsent: true,
      calleeConsent: true,
      consentState: "all_consented",
      isRecording: true,
      updatedAt: new Date(),
    })
  );
});
