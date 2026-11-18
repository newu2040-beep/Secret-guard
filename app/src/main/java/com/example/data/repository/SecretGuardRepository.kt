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
import com.example.data.model.TrustedContact
import com.example.data.model.UserPrivateSettings
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    // --- REALTIME MULTI-USER OBSERVATION FLOWS ---

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

    fun observePrivateSettings(userId: String): Flow<UserPrivateSettings?> {
        val path = "users/$userId/private/settings"
        return db.collection("users").document(userId)
            .collection("private")
            .document("settings")
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserPrivateSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    /**
     * Real-time directory of registered SecretGuard users for contact discovery and live presence.
     */
    fun observeDirectoryUsers(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .whereEqualTo("directoryVisible", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeContacts(userId: String): Flow<List<TrustedContact>> {
        val path = "contacts"
        return db.collection("contacts")
            .whereArrayContains("memberUids", userId)
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
            .whereArrayContains("memberUids", userId)
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
            .whereArrayContains("memberUids", userId)
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
            .whereArrayContains("memberUids", userId)
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

    fun observeCallSession(callId: String): Flow<CallSession?> {
        val path = "calls/$callId"
        return db.collection("calls").document(callId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeRecordings(userId: String): Flow<List<CallRecording>> {
        val path = "recordings"
        return db.collection("recordings")
            .whereArrayContains("memberUids", userId)
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

    // --- PROFILE INITIALIZATION & LEGACY DEMO CLEANUP ---

    suspend fun ensureUserProfile(
        displayName: String = "",
        email: String = "",
        photoUrl: String = ""
    ): UserProfile {
        val uid = requireUserId()
        val derivedName = displayName.trim().ifBlank {
            email.substringBefore("@").replace(".", " ")
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "User ${uid.take(5)}" }
        }.take(80)
        val derivedHandle = deriveHandle(email, derivedName, uid)
        val safePhoto = photoUrl.take(240_000)

        val userRef = db.collection("users").document(uid)
        val privateRef = userRef.collection("private").document("settings")

        return try {
            val existingSnap = userRef.get().await()
            val profile = if (existingSnap.exists()) {
                val currentHandle = existingSnap.getString("userHandle")
                if (currentHandle.isNullOrBlank()) {
                    // Upgrade legacy profile schema to current multi-user directory schema
                    userRef.set(
                        mapOf(
                            "uid" to uid,
                            "displayName" to derivedName,
                            "userHandle" to derivedHandle,
                            "photoUrl" to safePhoto,
                            "onlineStatus" to "online",
                            "directoryVisible" to true,
                            "accountType" to "google",
                            "profileVisibility" to "contacts",
                            "createdAt" to (existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    userRef.update(
                        mapOf(
                            "onlineStatus" to "online",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                existingSnap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(id = uid, uid = uid, displayName = derivedName, userHandle = derivedHandle)
            } else {
                val payload = mapOf(
                    "uid" to uid,
                    "displayName" to derivedName,
                    "userHandle" to derivedHandle,
                    "photoUrl" to safePhoto,
                    "onlineStatus" to "online",
                    "directoryVisible" to true,
                    "accountType" to "google",
                    "profileVisibility" to "contacts",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()
                UserProfile(
                    id = uid,
                    uid = uid,
                    displayName = derivedName,
                    userHandle = derivedHandle,
                    photoUrl = safePhoto
                )
            }

            val privSnap = privateRef.get().await()
            if (!privSnap.exists()) {
                privateRef.set(
                    mapOf(
                        "uid" to uid,
                        "email" to email.take(120),
                        "appLockEnabled" to false,
                        "autoRecordCalls" to true,
                        "notificationAlert" to true,
                        "recordingQuality" to "HD 720p",
                        "retentionDays" to 30L,
                        "blockedUids" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            profile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            throw e
        }
    }

    /**
     * Initializes the authenticated user's real profile and private settings, and purges any
     * legacy fake demo documents ("Priya Sharma", "Rahul Karki", "Secret Group") if present.
     */
    suspend fun ensureWorkspaceInitialized(
        displayName: String = "",
        email: String = "",
        photoUrl: String = ""
    ) {
        val uid = requireUserId()
        ensureUserProfile(displayName = displayName, email = email, photoUrl = photoUrl)
        purgeLegacyDemoArtifactsIfPresent(uid)
    }

    private suspend fun purgeLegacyDemoArtifactsIfPresent(uid: String) {
        val prefix = uid.take(8)
        val legacyContactIds = listOf(
            "contact_priya_$prefix",
            "contact_rahul_$prefix",
            "contact_group_$prefix"
        )
        for (cid in legacyContactIds) {
            runCatching { db.collection("contacts").document(cid).delete().await() }
        }

        val legacyConvIds = listOf(
            "conv_priya_$prefix",
            "conv_rahul_$prefix",
            "conv_group_$prefix"
        )
        for (convId in legacyConvIds) {
            runCatching {
                val convRef = db.collection("conversations").document(convId)
                val msgs = convRef.collection("messages").whereEqualTo("ownerUid", uid).get().await()
                for (m in msgs.documents) {
                    runCatching { m.reference.delete().await() }
                }
                convRef.delete().await()
            }
        }

        runCatching { db.collection("calls").document("call_priya_$prefix").delete().await() }
    }

    // --- USER SETTINGS & REAL-TIME PRESENCE ---

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
        val userRef = db.collection("users").document(uid)
        val privRef = userRef.collection("private").document("settings")
        try {
            userRef.update(
                mapOf(
                    "displayName" to displayName.trim().ifBlank { "User ${uid.take(5)}" }.take(80),
                    "profileVisibility" to profileVisibility,
                    "directoryVisible" to (profileVisibility != "private"),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            privRef.update(
                mapOf(
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
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    suspend fun updateUserProfileCustomization(
        displayName: String,
        userHandle: String,
        photoUrl: String,
        profileVisibility: String,
        gender: String = "",
        age: Long = 0L,
        requireCallApproval: Boolean = false
    ) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val cleanName = displayName.trim().ifBlank { "User ${uid.take(5)}" }.take(80)
        val rawHandle = userHandle.trim().ifBlank { deriveHandle("", cleanName, uid) }
        val cleanHandle = (if (rawHandle.startsWith("@")) rawHandle else "@$rawHandle").take(120)
        val validVis = if (profileVisibility in listOf("public", "contacts", "private")) {
            profileVisibility
        } else {
            "contacts"
        }
        val validGender = if (gender in listOf("Female", "Male")) gender else ""
        val validAge = age.coerceIn(0L, 120L)
        try {
            userRef.update(
                mapOf(
                    "displayName" to cleanName,
                    "userHandle" to cleanHandle,
                    "photoUrl" to photoUrl.take(240_000),
                    "gender" to validGender,
                    "age" to validAge,
                    "requireCallApproval" to requireCallApproval,
                    "profileVisibility" to validVis,
                    "directoryVisible" to (validVis != "private"),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    suspend fun setRequireCallApproval(required: Boolean) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        try {
            userRef.update(
                mapOf(
                    "requireCallApproval" to required,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
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

    // --- REAL MULTI-USER TRUSTED CONTACTS & CALL APPROVAL ---

    /**
     * Connects with a real registered user from the directory or creates a real contact entry,
     * along with a shared real-time conversation room (`memberUids`).
     */
    suspend fun addTrustedContact(
        contactName: String,
        contactHandle: String,
        targetUid: String = "",
        requesterName: String = "",
        requesterHandle: String = "",
        avatarKey: String = "custom",
        callApproved: Boolean = true,
        callApprovalRequestedBy: String = ""
    ): String {
        val uid = requireUserId()
        var resolvedTargetUid = targetUid.trim()
        var resolvedTargetName = contactName.trim()
        var resolvedTargetHandle = contactHandle.trim()
        var resolvedAvatar = avatarKey.ifBlank { "custom" }

        if (resolvedTargetUid.isBlank()) {
            val directorySnap = runCatching {
                db.collection("users")
                    .whereEqualTo("directoryVisible", true)
                    .get()
                    .await()
            }.getOrNull()

            val matchedUser = directorySnap?.documents?.mapNotNull {
                it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }?.firstOrNull { candidate ->
                candidate.uid != uid && (
                    candidate.userHandle.equals(resolvedTargetHandle, ignoreCase = true) ||
                        candidate.userHandle.equals("@$resolvedTargetHandle", ignoreCase = true) ||
                        candidate.displayName.equals(resolvedTargetName, ignoreCase = true) ||
                        candidate.uid.equals(resolvedTargetHandle, ignoreCase = true)
                    )
            }

            if (matchedUser != null) {
                resolvedTargetUid = matchedUser.uid
                resolvedTargetName = matchedUser.displayName
                resolvedTargetHandle = matchedUser.userHandle
                if (matchedUser.photoUrl.isNotBlank()) {
                    resolvedAvatar = matchedUser.photoUrl
                }
            }
        }

        val effectiveTargetUid = resolvedTargetUid.ifBlank { uid }
        val members = if (effectiveTargetUid == uid) listOf(uid) else listOf(uid, effectiveTargetUid).distinct()
        val contactId = if (effectiveTargetUid != uid) {
            val sorted = listOf(uid, effectiveTargetUid).sorted()
            "contact_${sorted[0].take(20)}_${sorted[1].take(20)}"
        } else {
            "contact_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }

        val safeRequesterName = requesterName.trim().ifBlank { "SecretGuard User" }.take(80)
        val safeRequesterHandle = requesterHandle.trim().ifBlank { "@${uid.take(6)}" }.take(120)
        val safeContactName = resolvedTargetName.ifBlank { "Trusted Contact" }.take(80)
        val safeContactHandle = resolvedTargetHandle.ifBlank { "@contact" }.take(120)
        val activityLabel = if (callApproved) "Connected • Call Ready" else "Call Approval Requested"

        val docRef = db.collection("contacts").document(contactId)
        try {
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "targetUid" to effectiveTargetUid.take(128),
                    "memberUids" to members,
                    "requesterName" to safeRequesterName,
                    "requesterHandle" to safeRequesterHandle,
                    "contactName" to safeContactName,
                    "contactHandle" to safeContactHandle,
                    "avatarKey" to resolvedAvatar.take(240_000),
                    "status" to "accepted",
                    "presenceState" to "online",
                    "lastActivityText" to activityLabel,
                    "lastActivityType" to "message",
                    "callApproved" to callApproved,
                    "callApprovalRequestedBy" to callApprovalRequestedBy.take(128),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val convId = buildDeterministicConversationId(uid, effectiveTargetUid)
            createConversationWithId(
                conversationId = convId,
                partnerUid = effectiveTargetUid,
                ownerName = safeRequesterName,
                participantName = safeContactName,
                participantAvatarKey = resolvedAvatar,
                presenceState = "online",
                lastMessageText = "Private chat ready."
            )
            return contactId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun requestCallApprovalForPartner(
        partnerUid: String,
        partnerName: String,
        partnerHandle: String,
        partnerAvatar: String,
        requesterName: String,
        requesterHandle: String
    ) {
        val uid = requireUserId()
        addTrustedContact(
            contactName = partnerName,
            contactHandle = partnerHandle,
            targetUid = partnerUid,
            requesterName = requesterName,
            requesterHandle = requesterHandle,
            avatarKey = partnerAvatar,
            callApproved = false,
            callApprovalRequestedBy = uid
        )
    }

    suspend fun setContactCallApproval(contactId: String, approved: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("contacts").document(contactId)
        try {
            docRef.update(
                mapOf(
                    "callApproved" to approved,
                    "callApprovalRequestedBy" to if (approved) "" else uid,
                    "lastActivityText" to if (approved) "Call Approved ✓" else "Call Approval Required",
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

    suspend fun acceptContactRequest(contactId: String) {
        requireUserId()
        val docRef = db.collection("contacts").document(contactId)
        try {
            docRef.update(
                mapOf(
                    "status" to "accepted",
                    "callApproved" to true,
                    "callApprovalRequestedBy" to "",
                    "lastActivityText" to "Contact request accepted",
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

    // --- REAL-TIME MULTI-USER CONVERSATIONS & ENCRYPTED MESSAGING ---

    fun buildDeterministicConversationId(uidA: String, uidB: String): String {
        val cleanA = uidA.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(24)
        val cleanB = uidB.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(24)
        return if (cleanA == cleanB || cleanB.isBlank()) {
            "conv_self_$cleanA"
        } else {
            val sorted = listOf(cleanA, cleanB).sorted()
            "conv_${sorted[0]}_${sorted[1]}"
        }
    }

    suspend fun createConversationWithId(
        conversationId: String = "conv_${UUID.randomUUID().toString().replace("-", "").take(16)}",
        partnerUid: String = "",
        ownerName: String = "SecretGuard User",
        participantName: String,
        participantAvatarKey: String = "custom",
        presenceState: String = "online",
        lastMessageText: String = ""
    ): String {
        val uid = requireUserId()
        val effectivePartnerUid = partnerUid.ifBlank { uid }.take(128)
        val members = if (effectivePartnerUid == uid) listOf(uid) else listOf(uid, effectivePartnerUid).distinct()
        val docRef = db.collection("conversations").document(conversationId)

        try {
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "partnerUid" to effectivePartnerUid,
                    "memberUids" to members,
                    "ownerName" to ownerName.trim().ifBlank { "SecretGuard User" }.take(80),
                    "participantName" to participantName.trim().ifBlank { "Trusted Contact" }.take(80),
                    "participantAvatarKey" to participantAvatarKey.ifBlank { "custom" }.take(240_000),
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
        mediaBase64: String = "",
        mediaDurationSec: Long = 0L,
        mediaLabel: String = ""
    ): String {
        val uid = requireUserId()
        val convRef = db.collection("conversations").document(conversationId)
        val convSnap = convRef.get().await()
        val rawMembers = (convSnap.get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val memberUids = if (rawMembers.isNotEmpty() && uid in rawMembers) rawMembers else listOf(uid)

        val formattedTime = mediaLabel.ifBlank {
            SimpleDateFormat("hh:mm a", Locale.US).format(Date())
        }

        val messageId = insertMessageInternal(
            conversationId = conversationId,
            memberUids = memberUids,
            senderUid = uid,
            senderName = senderName.ifBlank { "SecretGuard User" },
            isOutgoing = true,
            messageType = messageType,
            text = text,
            mediaBase64 = mediaBase64,
            mediaDurationSec = mediaDurationSec,
            mediaLabel = formattedTime
        )

        val preview = when (messageType) {
            "video" -> "\uD83C\uDFA5 Encrypted Video (${formatDuration(mediaDurationSec)})"
            "audio" -> "\uD83C\uDF99\uFE0F Voice Message (${formatDuration(mediaDurationSec)})"
            "image" -> "\uD83D\uDDBC\uFE0F Encrypted Photo"
            "recording" -> "\uD83D\uDD12 Consented Recording (${formatDuration(mediaDurationSec)})"
            else -> text
        }

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

    suspend fun markIncomingMessagesAsRead(conversationId: String, messages: List<ChatMessage>) {
        val uid = auth.currentUser?.uid ?: return
        val unreadIncoming = messages.filter { it.senderUid != uid && it.deliveryState != "read" }
        for (msg in unreadIncoming) {
            val msgRef = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(msg.id)
            runCatching {
                msgRef.update(
                    mapOf(
                        "deliveryState" to "read",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    suspend fun deleteMessagePermanently(conversationId: String, messageId: String) {
        requireUserId()
        if (conversationId.isBlank() || messageId.isBlank()) return
        val convRef = db.collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document(messageId)
        try {
            msgRef.delete().await()
            runCatching {
                convRef.update(
                    mapOf(
                        "lastMessageText" to "Message permanently deleted",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, msgRef.path)
            throw e
        }
    }

    private suspend fun insertMessageInternal(
        conversationId: String,
        memberUids: List<String>,
        senderUid: String,
        senderName: String,
        isOutgoing: Boolean,
        messageType: String,
        text: String,
        mediaBase64: String,
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
        val encryptedEnvelope = cryptoManager.encryptMessageEnvelope(
            plaintext = safeText.ifBlank { messageType },
            channelId = conversationId
        ).take(4000)

        try {
            msgRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "memberUids" to memberUids.distinct().take(8),
                    "conversationId" to conversationId,
                    "senderUid" to senderUid.take(128),
                    "senderName" to senderName.take(80),
                    "isOutgoing" to isOutgoing,
                    "messageType" to messageType,
                    "text" to safeText,
                    "encryptedCiphertext" to encryptedEnvelope,
                    "mediaBase64" to mediaBase64.take(740_000),
                    "mediaDurationSec" to mediaDurationSec.coerceIn(0L, 36000L),
                    "mediaLabel" to mediaLabel.take(120),
                    "deliveryState" to "sent",
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

    // --- REAL-TIME MULTI-USER CALL SIGNALING, LIVE CAMERA STREAM & CONSENT RECORDING ---

    suspend fun startCallSession(
        calleeUid: String = "",
        additionalCalleeUids: List<String> = emptyList(),
        callerName: String = "SecretGuard User",
        participantName: String,
        participantAvatarKey: String,
        callType: String
    ): String {
        val uid = requireUserId()
        val effectiveCalleeUid = calleeUid.ifBlank {
            additionalCalleeUids.firstOrNull { it.isNotBlank() && it != uid }.orEmpty()
        }.ifBlank { uid }.take(128)
        val members = (listOf(uid, effectiveCalleeUid) + additionalCalleeUids.map { it.trim().take(128) })
            .filter { it.isNotBlank() }
            .distinct()
            .take(8)
        val initialStatus = if (members.size <= 1) "connected" else "ringing"

        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "calleeUid" to effectiveCalleeUid,
                    "memberUids" to members,
                    "callerName" to callerName.trim().ifBlank { "SecretGuard User" }.take(80),
                    "participantName" to participantName.trim().ifBlank { "Trusted Contact" }.take(80),
                    "participantAvatarKey" to participantAvatarKey.ifBlank { "custom" }.take(240_000),
                    "callType" to if (callType == "audio") "audio" else "video",
                    "status" to initialStatus,
                    "consentState" to "none",
                    "callerConsent" to false,
                    "calleeConsent" to false,
                    "isRecording" to false,
                    "durationSec" to 0L,
                    "encryptionVerified" to true,
                    "callerVideoFrame" to "",
                    "calleeVideoFrame" to "",
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

    suspend fun addParticipantToCall(
        callId: String,
        newParticipantUid: String,
        newParticipantName: String
    ) {
        val uid = requireUserId()
        if (callId.isBlank()) return
        val docRef = db.collection("calls").document(callId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) return
            val existingMembers = (snap.get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            val cleanNewUid = newParticipantUid.trim().take(128)
            val updatedMembers = (existingMembers + listOfNotNull(uid, cleanNewUid.takeIf { it.isNotBlank() }))
                .distinct()
                .take(8)
            val currentNames = snap.getString("participantName").orEmpty().trim()
            val cleanNewName = newParticipantName.trim().ifBlank { "Participant" }
            val updatedNames = if (currentNames.contains(cleanNewName, ignoreCase = true)) {
                currentNames
            } else if (currentNames.isBlank()) {
                cleanNewName
            } else {
                "$currentNames, $cleanNewName"
            }.take(80)

            docRef.update(
                mapOf(
                    "memberUids" to updatedMembers,
                    "participantName" to updatedNames,
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

    suspend fun acceptIncomingCall(callId: String) {
        requireUserId()
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "status" to "connected",
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

    suspend fun declineIncomingCall(callId: String) {
        requireUserId()
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "status" to "declined",
                    "isRecording" to false,
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

    suspend fun publishLiveVideoFrame(callId: String, isCaller: Boolean, base64Jpeg: String) {
        auth.currentUser?.uid ?: return
        if (callId.isBlank() || base64Jpeg.length > 440_000) return
        val docRef = db.collection("calls").document(callId)
        val fieldName = if (isCaller) "callerVideoFrame" else "calleeVideoFrame"
        runCatching {
            docRef.update(
                mapOf(
                    fieldName to base64Jpeg,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun updateCallConsentAndRecording(
        callId: String,
        consentState: String,
        callerConsent: Boolean,
        calleeConsent: Boolean,
        isRecording: Boolean,
        durationSec: Long
    ) {
        requireUserId()
        val safeRecording = isRecording && consentState == "all_consented" && callerConsent && calleeConsent
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "consentState" to consentState,
                    "callerConsent" to callerConsent,
                    "calleeConsent" to calleeConsent,
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
        wasConsentedRecording: Boolean,
        recordedAudioBase64: String = "",
        recordedLocalFilePath: String = "",
        recordedSizeBytes: Long = 0L
    ) {
        val uid = requireUserId()
        val docRef = db.collection("calls").document(callId)
        try {
            val snap = docRef.get().await()
            val currentStatus = snap.getString("status") ?: "ended"
            val rawMembers = (snap.get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            val members = if (rawMembers.isNotEmpty() && uid in rawMembers) rawMembers else listOf(uid)

            if (currentStatus !in listOf("ended", "declined", "missed")) {
                docRef.update(
                    mapOf(
                        "status" to "ended",
                        "isRecording" to false,
                        "callerVideoFrame" to "",
                        "calleeVideoFrame" to "",
                        "durationSec" to durationSec.coerceIn(0L, 86400L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (wasConsentedRecording && (recordedSizeBytes > 0L || recordedAudioBase64.isNotBlank())) {
                createRecordingMetadata(
                    callId = callId,
                    memberUids = members,
                    title = "$participantName • Consented ${if (callType == "video") "Video" else "Audio"} Call",
                    participantName = participantName,
                    mediaType = "audio",
                    durationSec = durationSec.coerceAtLeast(1L),
                    sizeBytes = recordedSizeBytes.coerceAtLeast(1L),
                    audioBase64 = recordedAudioBase64,
                    localFilePath = recordedLocalFilePath
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- REAL RECORDINGS & STORAGE MANAGER ---

    suspend fun createRecordingMetadata(
        callId: String,
        memberUids: List<String> = emptyList(),
        title: String,
        participantName: String,
        mediaType: String,
        durationSec: Long,
        sizeBytes: Long,
        audioBase64: String = "",
        localFilePath: String = ""
    ): String {
        val uid = requireUserId()
        val effectiveMembers = if (memberUids.isNotEmpty() && uid in memberUids) {
            memberUids.distinct().take(8)
        } else {
            listOf(uid)
        }
        val recordingId = "rec_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("recordings").document(recordingId)
        try {
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "memberUids" to effectiveMembers,
                    "callId" to callId.take(128),
                    "title" to title.trim().ifBlank { "Consented Audio Recording" }.take(120),
                    "participantName" to participantName.trim().ifBlank { "SecretGuard User" }.take(80),
                    "mediaType" to if (mediaType == "video") "video" else "audio",
                    "durationSec" to durationSec.coerceIn(1L, 86400L),
                    "sizeBytes" to sizeBytes.coerceIn(1L, 1073741824L),
                    "audioBase64" to audioBase64.take(740_000),
                    "localFilePath" to localFilePath.take(500),
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
            val recordings = db.collection("recordings").whereArrayContains("memberUids", uid).get().await()
            for (doc in recordings.documents) {
                doc.reference.delete().await()
            }
            val calls = db.collection("calls").whereArrayContains("memberUids", uid).get().await()
            for (doc in calls.documents) {
                doc.reference.delete().await()
            }
            val contacts = db.collection("contacts").whereArrayContains("memberUids", uid).get().await()
            for (doc in contacts.documents) {
                doc.reference.delete().await()
            }
            val conversations = db.collection("conversations").whereArrayContains("memberUids", uid).get().await()
            for (conv in conversations.documents) {
                val messages = conv.reference.collection("messages")
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                for (msg in messages.documents) {
                    msg.reference.delete().await()
                }
                conv.reference.delete().await()
            }
            runCatching {
                db.collection("users").document(uid).collection("private").document("settings").delete().await()
            }
            db.collection("users").document(uid).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            throw e
        }
    }

    private fun deriveHandle(email: String, displayName: String, uid: String): String {
        val base = if (email.isNotBlank() && email.contains("@")) {
            email.substringBefore("@").lowercase(Locale.US).replace(Regex("[^a-z0-9._\\-]"), "")
        } else {
            displayName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
        }
        val cleanBase = base.ifBlank { "user" }.take(24)
        return "@${cleanBase}_${uid.take(4).lowercase(Locale.US)}"
    }

    private fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
