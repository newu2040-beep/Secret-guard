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
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or list contacts, conversations, calls, or recordings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("contacts").get());
  await assertFails(unauthDb.collection("conversations").get());
  await assertFails(unauthDb.collection("calls").get());
  await assertFails(unauthDb.collection("recordings").get());
});

test("Authenticated user can create and read their own user profile, but cannot read another user's profile", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice Secret",
      email: "alice@example.com",
      photoUrl: "",
      onlineStatus: "online",
      accountType: "google",
      profileVisibility: "contacts",
      appLockEnabled: true,
      autoRecordCalls: false,
      notificationAlert: true,
      recordingQuality: "HD 720p",
      retentionDays: 30,
      blockedUids: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update with ghost field on user profile is rejected", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice Secret",
      email: "alice@example.com",
      photoUrl: "",
      onlineStatus: "online",
      accountType: "google",
      profileVisibility: "contacts",
      appLockEnabled: true,
      autoRecordCalls: false,
      notificationAlert: true,
      recordingQuality: "HD 720p",
      retentionDays: 30,
      blockedUids: [],
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Call recording cannot be active unless consentState is all_consented, and terminal calls are locked", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Attempting to create call with isRecording=true but consentState='none' must fail
  await assertFails(
    aliceDb.collection("calls").doc("call_1").set({
      ownerUid: ALICE_UID,
      participantName: "Priya Sharma",
      participantAvatarKey: "priya",
      callType: "video",
      status: "connected",
      consentState: "none",
      isRecording: true,
      durationSec: 10,
      encryptionVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid call creation with consentState='all_consented' and isRecording=true succeeds
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      ownerUid: ALICE_UID,
      participantName: "Priya Sharma",
      participantAvatarKey: "priya",
      callType: "video",
      status: "ended",
      consentState: "all_consented",
      isRecording: true,
      durationSec: 120,
      encryptionVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Terminal state locking: cannot update a call once status is 'ended'
  await assertFails(
    aliceDb.collection("calls").doc("call_1").update({
      durationSec: 200,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user can query their own conversations and messages with ownerUid filter, and fails without filter", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("conversations").doc("conv_1").set({
      ownerUid: ALICE_UID,
      participantName: "Priya Sharma",
      participantAvatarKey: "priya",
      presenceState: "online",
      lastMessageText: "Hey! Are you free?",
      unreadCount: 0,
      encryptionProtocol: "AES-256-GCM",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").where("ownerUid", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("conversations").get());

  // Creating message inside owned conversation succeeds
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc("conv_1")
      .collection("messages")
      .doc("msg_1")
      .set({
        ownerUid: ALICE_UID,
        conversationId: "conv_1",
        senderUid: ALICE_UID,
        senderName: "Secret User",
        isOutgoing: true,
        messageType: "text",
        text: "Yes, I'm here.",
        encryptedCiphertext: "AESGCM:iv:cipher",
        mediaDurationSec: 0,
        mediaLabel: "",
        deliveryState: "read",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Cross-user Bob cannot read Alice's message
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("conversations")
      .doc("conv_1")
      .collection("messages")
      .doc("msg_1")
      .get()
  );
});
