package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SecretGuardRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun multiUser_contactAndMessageExchange_succeedsAcrossAliceAndBob() = runBlocking {
        // 1. Register Bob in directory first
        val bobUid = signInTestUser(BOB_EMAIL)
        val repo = SecretGuardRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(displayName = "Bob Verifier", email = BOB_EMAIL)
        }

        // 2. Sign in as Alice, connect with Bob, and send an encrypted message
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(displayName = "Alice Secret", email = ALICE_EMAIL)
        }
        assertEquals(aliceUid, aliceProfile.uid)

        val contactId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addTrustedContact(
                contactName = "Bob Verifier",
                contactHandle = "@bob",
                targetUid = bobUid,
                requesterName = "Alice Secret",
                requesterHandle = aliceProfile.userHandle
            )
        }
        assertTrue(contactId.isNotBlank())

        val convId = repo.buildDeterministicConversationId(aliceUid, bobUid)
        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.sendMessage(
                conversationId = convId,
                senderName = "Alice Secret",
                text = "Real-time multi-user message from Alice to Bob!"
            )
        }
        assertTrue(msgId.isNotBlank())

        // 3. Switch back to Bob and verify Bob observes Alice's message in real time
        signInTestUser(BOB_EMAIL)
        val bobMessages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(bobUid, convId).first { list ->
                list.any { it.id == msgId }
            }
        }
        assertTrue(bobMessages.any { it.id == msgId && it.encryptedCiphertext.startsWith("AES256GCM:") })

        // 4. Permanently delete the message and verify it is removed for all participants
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deleteMessagePermanently(convId, msgId)
        }
        val afterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(bobUid, convId).first { list ->
                list.none { it.id == msgId }
            }
        }
        assertTrue(afterDelete.none { it.id == msgId })
    }

    @Test
    fun multiUser_callSignalingAndMutualConsentRecording_succeeds() = runBlocking {
        val bobUid = signInTestUser(BOB_EMAIL)
        val repo = SecretGuardRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(displayName = "Bob Verifier", email = BOB_EMAIL)
            repo.updateUserProfileCustomization(
                displayName = "Bob HD Verifier",
                userHandle = "@bob_hd",
                photoUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
                profileVisibility = "public",
                gender = "Male",
                age = 27L,
                requireCallApproval = true
            )
        }

        val aliceUid = signInTestUser(ALICE_EMAIL)
        val callId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.startCallSession(
                calleeUid = bobUid,
                callerName = "Alice Secret",
                participantName = "Bob HD Verifier",
                participantAvatarKey = "custom",
                callType = "video"
            )
        }

        // Alice invites a 3rd participant into the group call and streams a multiplexed AV1 voice+1080p packet
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addParticipantToCall(
                callId = callId,
                newParticipantUid = "charlie_group_uid_123",
                newParticipantName = "Charlie Group"
            )
            repo.publishLiveVideoFrame(
                callId = callId,
                isCaller = true,
                base64Jpeg = "AV1|101|dWxhd19hdWRpb19packet|amBlZ192aWRlb18xMDgwcA=="
            )
        }

        // Bob accepts the incoming call
        signInTestUser(BOB_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.acceptIncomingCall(callId)
        }

        // Mutual recording consent granted by both caller & callee
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateCallConsentAndRecording(
                callId = callId,
                consentState = "all_consented",
                callerConsent = true,
                calleeConsent = true,
                isRecording = true,
                durationSec = 42L
            )
        }

        // Alice ends the call and saves the consented recording
        signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.endCallSession(
                callId = callId,
                participantName = "Bob HD Verifier",
                callType = "video",
                durationSec = 65L,
                wasConsentedRecording = true,
                recordedAudioBase64 = "AES256GCM_BIN:testpayload",
                recordedSizeBytes = 16000L
            )
        }

        val recordings = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeRecordings(aliceUid).first { list ->
                list.any { it.callId == callId }
            }
        }
        assertTrue(recordings.any { it.callId == callId && it.consentVerified })
    }

    @Test
    fun nonMemberAndPrivateSettingsAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = SecretGuardRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(displayName = "Alice Secret", email = ALICE_EMAIL)
        }
        val contactId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addTrustedContact(
                contactName = "Self Vault",
                contactHandle = "@alice",
                targetUid = aliceUid
            )
        }

        // Switch to Bob (not in memberUids of Alice's self-contact) and attempt to read Alice's contact or private settings
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("contacts").document(contactId).get().await()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for non-member contact read")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("private").document("settings").get().await()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for private settings read")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun observeContacts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SecretGuardRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeContacts("unauth_uid").first()
            }
            fail("Expected exception for unauthenticated query")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@secretguard.test"
        const val BOB_EMAIL = "bob@secretguard.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
