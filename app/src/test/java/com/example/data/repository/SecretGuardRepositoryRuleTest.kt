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
    fun ensureUserProfile_and_sendMessage_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = SecretGuardRepository(firestore, auth)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile(displayName = "Alice Secret", email = ALICE_EMAIL)
        }
        assertEquals(aliceUid, profile.uid)

        val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createConversationWithId(
                participantName = "Priya Sharma",
                participantAvatarKey = "priya"
            )
        }
        assertTrue(convId.isNotBlank())

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendMessage(
                conversationId = convId,
                senderName = "Alice Secret",
                text = "Hello Priya, encrypted check!"
            )
        }
        assertTrue(msgId.isNotBlank())

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(aliceUid, convId).first { list ->
                list.any { it.id == msgId }
            }
        }
        assertTrue(messages.any { it.id == msgId && it.encryptedCiphertext.startsWith("AES256GCM:") })
    }

    @Test
    fun callConsentAndRecording_enforcesConsentPipeline() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = SecretGuardRepository(firestore, auth)

        val callId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.startCallSession(
                participantName = "Priya Sharma",
                participantAvatarKey = "priya",
                callType = "video"
            )
        }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateCallConsentAndRecording(
                callId = callId,
                consentState = "all_consented",
                isRecording = true,
                durationSec = 42L
            )
        }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.endCallSession(
                callId = callId,
                participantName = "Priya Sharma",
                callType = "video",
                durationSec = 65L,
                wasConsentedRecording = true
            )
        }

        val recordings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRecordings(aliceUid).first { list ->
                list.any { it.callId == callId }
            }
        }
        assertTrue(recordings.any { it.callId == callId && it.consentVerified })
    }

    @Test
    fun crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = SecretGuardRepository(firestore, auth)
        val contactId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addTrustedContact("Priya Sharma", "priya@secretguard.org")
        }

        // Switch to Bob and attempt to read Alice's contact document directly
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("contacts").document(contactId).get().await()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user read")
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
