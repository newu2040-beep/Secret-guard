package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.security.KeystoreCryptoManager
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.CallRecording
import com.example.data.model.CallSession
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.SecurityReport
import com.example.data.model.TrustedContact
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SecretGuardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth,
        KeystoreCryptoManager(context.applicationContext)
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- REALTIME OBSERVATION FLOWS ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeContacts(userId: String): Flow<List<TrustedContact>> {
        val path = "contacts"
        return db.collection("contacts")
            .whereEqualTo("ownerUid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TrustedContact::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> {
        val path = "conversations"
        return db.collection("conversations")
            .whereEqualTo("ownerUid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Conversation::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .whereEqualTo("ownerUid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeCalls(userId: String): Flow<List<CallSession>> {
        val path = "calls"
        return db.collection("calls")
            .whereEqualTo("ownerUid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeRecordings(userId: String): Flow<List<CallRecording>> {
        val path = "recordings"
        return db.collection("recordings")
            .whereEqualTo("ownerUid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CallRecording::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // --- PROFILE & WORKSPACE BOOTSTRAP ---

    suspend fun ensureUserProfile(
        displayName: String = "Secret User",
        email: String = "",
        photoUrl: String = ""
    ): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "onlineStatus" to "online",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                existingSnap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(id = uid, uid = uid, displayName = displayName)
            } else {
                val safeName = displayName.ifBlank { "Secret User" }.take(80)
                val safeEmail = email.take(120)
                val safePhoto = photoUrl.take(500)
                val payload = mapOf(
                    "uid" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "onlineStatus" to "online",
                    "accountType" to "google",
                    "profileVisibility" to "contacts",
                    "appLockEnabled" to false,
                    "autoRecordCalls" to true,
                    "notificationAlert" to true,
                    "recordingQuality" to "HD 720p",
                    "retentionDays" to 30L,
                    "blockedUids" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                UserProfile(
                    id = uid,
                    uid = uid,
                    displayName = safeName,
                    email = safeEmail,
                    photoUrl = safePhoto
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    /**
     * Ensures the authenticated user's profile and initial trusted communications workspace
     * exist in Firestore so all 5 reference screens operate on live Firestore documents.
     */
    suspend fun ensureWorkspaceInitialized(
        displayName: String = "Secret User",
        email: String = "",
        photoUrl: String = ""
    ) {
        val uid = requireUserId()
        ensureUserProfile(displayName = displayName, email = email, photoUrl = photoUrl)

        val contactsPath = "contacts"
        try {
            val existingContacts = db.collection(contactsPath)
                .whereEqualTo("ownerUid", uid)
                .limit(1)
                .get()
                .await()

            if (!existingContacts.isEmpty) return

            // 1. Seed Trusted Contacts matching reference screens
            val priyaContactId = "contact_priya_${uid.take(8)}"
            val rahulContactId = "contact_rahul_${uid.take(8)}"
            val groupContactId = "contact_group_${uid.take(8)}"

            createTrustedContactInternal(
                contactId = priyaContactId,
                contactName = "Priya Sharma",
                contactHandle = "priya.sharma@secretguard.org",
                avatarKey = "priya",
                presenceState = "online",
                lastActivityText = "Video Call • 2m ago",
                lastActivityType = "video_call"
            )
            createTrustedContactInternal(
                contactId = rahulContactId,
                contactName = "Rahul Karki",
                contactHandle = "rahul.karki@secretguard.org",
                avatarKey = "rahul",
                presenceState = "online",
                lastActivityText = "Audio • 5m ago",
                lastActivityType = "audio_recording"
            )
            createTrustedContactInternal(
                contactId = groupContactId,
                contactName = "Secret Group",
                contactHandle = "trusted-team@secretguard.org",
                avatarKey = "group",
                presenceState = "online",
                lastActivityText = "New Message • 10m ago",
                lastActivityType = "message"
            )

            // 2. Seed Encrypted Conversations & Messages for Priya Sharma, Rahul Karki, and Secret Group
            val priyaConvId = "conv_priya_${uid.take(8)}"
            createConversationWithId(
                conversationId = priyaConvId,
                participantName = "Priya Sharma",
                participantAvatarKey = "priya",
                presenceState = "online",
                lastMessageText = "Great! Let's call now?"
            )

            insertMessageInternal(
                conversationId = priyaConvId,
                senderUid = "priya_uid",
                senderName = "Priya Sharma",
                isOutgoing = false,
                messageType = "text",
                text = "Hey! Are you free?",
                mediaDurationSec = 0L,
                mediaLabel = "10:12 AM"
            )
            insertMessageInternal(
                conversationId = priyaConvId,
                senderUid = uid,
                senderName = displayName.ifBlank { "Secret User" },
                isOutgoing = true,
                messageType = "text",
                text = "Yes, I'm here. \uD83D\uDE0A",
                mediaDurationSec = 0L,
                mediaLabel = "10:13 AM"
            )
            insertMessageInternal(
                conversationId = priyaConvId,
                senderUid = uid,
                senderName = displayName.ifBlank { "Secret User" },
                isOutgoing = true,
                messageType = "video",
                text = "Encrypted Mountain Lake Clip",
                mediaDurationSec = 15L,
                mediaLabel = "00:00:15"
            )
            insertMessageInternal(
                conversationId = priyaConvId,
                senderUid = uid,
                senderName = displayName.ifBlank { "Secret User" },
                isOutgoing = true,
                messageType = "audio",
                text = "Encrypted Voice Note",
                mediaDurationSec = 12L,
                mediaLabel = "00:00:12"
            )
            insertMessageInternal(
                conversationId = priyaConvId,
                senderUid = "priya_uid",
                senderName = "Priya Sharma",
                isOutgoing = false,
                messageType = "text",
                text = "Great! Let's call now?",
                mediaDurationSec = 0L,
                mediaLabel = "10:16 AM"
            )

            val rahulConvId = "conv_rahul_${uid.take(8)}"
            createConversationWithId(
                conversationId = rahulConvId,
                participantName = "Rahul Karki",
                participantAvatarKey = "rahul",
                presenceState = "online",
                lastMessageText = "Shared consented audio note (00:00:18)"
            )
            insertMessageInternal(
                conversationId = rahulConvId,
                senderUid = "rahul_uid",
                senderName = "Rahul Karki",
                isOutgoing = false,
                messageType = "audio",
                text = "Security audit voice briefing",
                mediaDurationSec = 18L,
                mediaLabel = "00:00:18"
            )

            val groupConvId = "conv_group_${uid.take(8)}"
            createConversationWithId(
                conversationId = groupConvId,
                participantName = "Secret Group",
                participantAvatarKey = "group",
                presenceState = "online",
                lastMessageText = "All participants verified AES-256-GCM keys."
            )
            insertMessageInternal(
                conversationId = groupConvId,
                senderUid = "group_uid",
                senderName = "Secret Group",
                isOutgoing = false,
                messageType = "text",
                text = "All participants verified AES-256-GCM keys.",
                mediaDurationSec = 0L,
                mediaLabel = "10:06 AM"
            )

            // 3. Seed Consented Call & Recording History
            val sampleCallId = "call_priya_${uid.take(8)}"
            db.collection("calls").document(sampleCallId).set(
                mapOf(
                    "ownerUid" to uid,
                    "participantName" to "Priya Sharma",
                    "participantAvatarKey" to "priya",
                    "callType" to "video",
                    "status" to "ended",
                    "consentState" to "all_consented",
                    "isRecording" to true,
                    "durationSec" to 754L,
                    "encryptionVerified" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createRecordingMetadata(
                callId = sampleCallId,
                title = "Priya Sharma • Consented Video Session",
                participantName = "Priya Sharma",
                mediaType = "video",
                durationSec = 15L,
                sizeBytes = 1_840_000L
            )
            createRecordingMetadata(
                callId = "audio_rahul_${uid.take(8)}",
                title = "Rahul Karki • Encrypted Voice Note",
                participantName = "Rahul Karki",
                mediaType = "audio",
                durationSec = 12L,
                sizeBytes = 320_000L
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, contactsPath)
        }
    }

    // --- USER SETTINGS & PRESENCE ---

    suspend fun updateUserSettings(
        displayName: String,
        profileVisibility: String,
        appLockEnabled: Boolean,
        autoRecordCalls: Boolean,
        notificationAlert: Boolean,
        recordingQuality: String,
        retentionDays: Long
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "displayName" to displayName.trim().ifBlank { "Secret User" }.take(80),
                    "profileVisibility" to profileVisibility,
                    "appLockEnabled" to appLockEnabled,
                    "autoRecordCalls" to autoRecordCalls,
                    "notificationAlert" to notificationAlert,
                    "recordingQuality" to recordingQuality,
                    "retentionDays" to retentionDays.coerceIn(1L, 365L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateOnlinePresence(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "onlineStatus" to if (isOnline) "online" else "offline",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    // --- TRUSTED CONTACTS ---

    suspend fun addTrustedContact(
        contactName: String,
        contactHandle: String,
        avatarKey: String = "custom"
    ): String {
        val contactId = "contact_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        createTrustedContactInternal(
            contactId = contactId,
            contactName = contactName.trim().ifBlank { "Trusted Contact" }.take(80),
            contactHandle = contactHandle.trim().ifBlank { "contact@secretguard.org" }.take(120),
            avatarKey = avatarKey,
            presenceState = "online",
            lastActivityText = "Trusted Contact • Just now",
            lastActivityType = "message"
        )
        createConversationWithId(
            conversationId = "conv_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            participantName = contactName.trim().ifBlank { "Trusted Contact" }.take(80),
            participantAvatarKey = avatarKey,
            presenceState = "online",
            lastMessageText = "Encrypted channel established."
        )
        return contactId
    }

    private suspend fun createTrustedContactInternal(
        contactId: String,
        contactName: String,
        contactHandle: String,
        avatarKey: String,
        presenceState: String,
        lastActivityText: String,
        lastActivityType: String
    ) {
        val uid = requireUserId()
        val docRef = db.collection("contacts").document(contactId)
        try {
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "contactName" to contactName.take(80),
                    "contactHandle" to contactHandle.take(120),
                    "avatarKey" to avatarKey.take(40),
                    "status" to "accepted",
                    "presenceState" to presenceState,
                    "lastActivityText" to lastActivityText.take(160),
                    "lastActivityType" to lastActivityType,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun setContactBlockStatus(contactId: String, blocked: Boolean) {
        requireUserId()
        val docRef = db.collection("contacts").document(contactId)
        try {
            docRef.update(
                mapOf(
                    "status" to if (blocked) "blocked" else "accepted",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- CONVERSATIONS & ENCRYPTED MESSAGING ---

    suspend fun createConversationWithId(
        conversationId: String = "conv_${UUID.randomUUID().toString().replace("-", "").take(16)}",
        participantName: String,
        participantAvatarKey: String = "priya",
        presenceState: String = "online",
        lastMessageText: String = ""
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("conversations").document(conversationId)
        try {
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "participantName" to participantName.trim().ifBlank { "Trusted Contact" }.take(80),
                    "participantAvatarKey" to participantAvatarKey.take(40),
                    "presenceState" to presenceState,
                    "lastMessageText" to lastMessageText.take(500),
                    "unreadCount" to 0L,
                    "encryptionProtocol" to "AES-256-GCM",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return conversationId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        senderName: String,
        text: String,
        messageType: String = "text",
        mediaDurationSec: Long = 0L,
        mediaLabel: String = ""
    ): String {
        val uid = requireUserId()
        val messageId = insertMessageInternal(
            conversationId = conversationId,
            senderUid = uid,
            senderName = senderName.ifBlank { "Secret User" },
            isOutgoing = true,
            messageType = messageType,
            text = text,
            mediaDurationSec = mediaDurationSec,
            mediaLabel = mediaLabel
        )
        val preview = when (messageType) {
            "video" -> "\uD83C\uDFA5 Encrypted Video (${formatDuration(mediaDurationSec)})"
            "audio" -> "\uD83C\uDF99\uFE0F Voice Message (${formatDuration(mediaDurationSec)})"
            "image" -> "\uD83D\uDDBC\uFE0F Encrypted Photo: $text"
            "recording" -> "\uD83D\uDD12 Consented Recording (${formatDuration(mediaDurationSec)})"
            else -> text
        }
        val convRef = db.collection("conversations").document(conversationId)
        try {
            convRef.update(
                mapOf(
                    "lastMessageText" to preview.take(500),
                    "unreadCount" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
        }
        return messageId
    }

    private suspend fun insertMessageInternal(
        conversationId: String,
        senderUid: String,
        senderName: String,
        isOutgoing: Boolean,
        messageType: String,
        text: String,
        mediaDurationSec: Long,
        mediaLabel: String
    ): String {
        val uid = requireUserId()
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val msgRef = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(messageId)
        val safeText = text.trim().take(2000)
        val encryptedEnvelope = cryptoManager.encryptMessageEnvelope(safeText).take(4000)

        try {
            msgRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "conversationId" to conversationId,
                    "senderUid" to senderUid.take(128),
                    "senderName" to senderName.take(80),
                    "isOutgoing" to isOutgoing,
                    "messageType" to messageType,
                    "text" to safeText,
                    "encryptedCiphertext" to encryptedEnvelope,
                    "mediaDurationSec" to mediaDurationSec.coerceIn(0L, 36000L),
                    "mediaLabel" to mediaLabel.take(120),
                    "deliveryState" to "read",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return messageId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    // --- CALLS & CONSENT-FIRST RECORDING PIPELINE ---

    suspend fun startCallSession(
        participantName: String,
        participantAvatarKey: String,
        callType: String
    ): String {
        val uid = requireUserId()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "participantName" to participantName.trim().ifBlank { "Priya Sharma" }.take(80),
                    "participantAvatarKey" to participantAvatarKey.take(40),
                    "callType" to if (callType == "audio") "audio" else "video",
                    "status" to "connected",
                    "consentState" to "none",
                    "isRecording" to false,
                    "durationSec" to 0L,
                    "encryptionVerified" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return callId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCallConsentAndRecording(
        callId: String,
        consentState: String,
        isRecording: Boolean,
        durationSec: Long
    ) {
        requireUserId()
        val safeRecording = isRecording && consentState == "all_consented"
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "consentState" to consentState,
                    "isRecording" to safeRecording,
                    "durationSec" to durationSec.coerceIn(0L, 86400L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun endCallSession(
        callId: String,
        participantName: String,
        callType: String,
        durationSec: Long,
        wasConsentedRecording: Boolean
    ) {
        requireUserId()
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "status" to "ended",
                    "isRecording" to false,
                    "durationSec" to durationSec.coerceIn(0L, 86400L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (wasConsentedRecording && durationSec >= 1L) {
                createRecordingMetadata(
                    callId = callId,
                    title = "$participantName • Consented ${if (callType == "video") "Video" else "Audio"} Call",
                    participantName = participantName,
                    mediaType = if (callType == "video") "video" else "audio",
                    durationSec = durationSec.coerceAtLeast(1L),
                    sizeBytes = (durationSec.coerceAtLeast(1L) * 48_000L).coerceAtMost(500_000_000L)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- RECORDINGS & STORAGE MANAGER ---

    suspend fun createRecordingMetadata(
        callId: String,
        title: String,
        participantName: String,
        mediaType: String,
        durationSec: Long,
        sizeBytes: Long
    ): String {
        val uid = requireUserId()
        val recordingId = "rec_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("recordings").document(recordingId)
        try {
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "callId" to callId.take(128),
                    "title" to title.trim().ifBlank { "Consented Audio Recording" }.take(120),
                    "participantName" to participantName.trim().ifBlank { "Secret User" }.take(80),
                    "mediaType" to if (mediaType == "video") "video" else "audio",
                    "durationSec" to durationSec.coerceIn(1L, 86400L),
                    "sizeBytes" to sizeBytes.coerceIn(1L, 1073741824L),
                    "consentVerified" to true,
                    "encryptionAlgorithm" to "AES-256-GCM",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return recordingId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteRecording(recordingId: String) {
        requireUserId()
        val docRef = db.collection("recordings").document(recordingId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- SECURITY REPORTS & ACCOUNT DELETION ---

    suspend fun submitSecurityReport(
        targetName: String,
        category: String,
        details: String
    ): String {
        val uid = requireUserId()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("reports").document(reportId)
        val validCategory = if (category in listOf(
                "spam", "harassment", "unauthorized_recording", "impersonation", "other"
            )
        ) category else "other"

        try {
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "targetName" to targetName.trim().ifBlank { "Unknown User" }.take(80),
                    "category" to validCategory,
                    "details" to details.trim().ifBlank { "User submitted safety report." }.take(1000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return reportId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAllUserData() {
        val uid = requireUserId()
        try {
            val recordings = db.collection("recordings").whereEqualTo("ownerUid", uid).get().await()
            for (doc in recordings.documents) {
                doc.reference.delete().await()
            }
            val calls = db.collection("calls").whereEqualTo("ownerUid", uid).get().await()
            for (doc in calls.documents) {
                doc.reference.delete().await()
            }
            val contacts = db.collection("contacts").whereEqualTo("ownerUid", uid).get().await()
            for (doc in contacts.documents) {
                doc.reference.delete().await()
            }
            val conversations = db.collection("conversations").whereEqualTo("ownerUid", uid).get().await()
            for (conv in conversations.documents) {
                val messages = conv.reference.collection("messages")
                    .whereEqualTo("ownerUid", uid)
                    .get()
                    .await()
                for (msg in messages.documents) {
                    msg.reference.delete().await()
                }
                conv.reference.delete().await()
            }
            db.collection("users").document(uid).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            throw e
        }
    }

    private fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
