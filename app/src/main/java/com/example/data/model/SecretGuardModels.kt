package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val userHandle: String = "",
    val photoUrl: String = "",
    val gender: String = "", // "" | "Female" | "Male"
    val age: Long = 0L,
    val requireCallApproval: Boolean = false,
    val onlineStatus: String = "online",
    val directoryVisible: Boolean = true,
    val accountType: String = "google",
    val profileVisibility: String = "contacts",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun demographicSummary(): String {
        val parts = mutableListOf<String>()
        if (gender in listOf("Female", "Male")) parts.add(gender)
        if (age > 0L) parts.add("${age} yrs")
        return parts.joinToString(" • ")
    }
}

@Keep
@IgnoreExtraProperties
data class UserPrivateSettings(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val appLockEnabled: Boolean = false,
    val autoRecordCalls: Boolean = true,
    val notificationAlert: Boolean = true,
    val recordingQuality: String = "FHD 1080p",
    val retentionDays: Long = 30L,
    val blockedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TrustedContact(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val targetUid: String = "",
    val memberUids: List<String> = emptyList(),
    val requesterName: String = "",
    val requesterHandle: String = "",
    val contactName: String = "",
    val contactHandle: String = "",
    val avatarKey: String = "custom",
    val status: String = "accepted",
    val presenceState: String = "online",
    val lastActivityText: String = "",
    val lastActivityType: String = "message",
    val callApproved: Boolean = true,
    val callApprovalRequestedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun peerUid(currentUid: String): String {
        return if (ownerUid == currentUid) targetUid else ownerUid
    }

    fun peerDisplayName(currentUid: String): String {
        return if (ownerUid == currentUid) contactName else requesterName.ifBlank { contactName }
    }

    fun peerHandle(currentUid: String): String {
        return if (ownerUid == currentUid) contactHandle else requesterHandle.ifBlank { contactHandle }
    }
}

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val partnerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val ownerName: String = "",
    val participantName: String = "",
    val participantAvatarKey: String = "custom",
    val presenceState: String = "online",
    val lastMessageText: String = "",
    val unreadCount: Long = 0L,
    val encryptionProtocol: String = "AES-256-GCM",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun peerUid(currentUid: String): String {
        return if (ownerUid == currentUid) partnerUid else ownerUid
    }

    fun peerDisplayName(currentUid: String): String {
        return if (ownerUid == currentUid) participantName else ownerName.ifBlank { participantName }
    }
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val conversationId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val isOutgoing: Boolean = true,
    val messageType: String = "text",
    val text: String = "",
    val encryptedCiphertext: String = "",
    val mediaBase64: String = "",
    val mediaDurationSec: Long = 0L,
    val mediaLabel: String = "",
    val deliveryState: String = "sent",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val calleeUid: String = "",
    val memberUids: List<String> = emptyList(),
    val callerName: String = "",
    val participantName: String = "",
    val participantAvatarKey: String = "custom",
    val callType: String = "video",
    val status: String = "ringing",
    val consentState: String = "none",
    val callerConsent: Boolean = false,
    val calleeConsent: Boolean = false,
    val isRecording: Boolean = false,
    val durationSec: Long = 0L,
    val encryptionVerified: Boolean = true,
    val callerVideoFrame: String = "",
    val calleeVideoFrame: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun peerDisplayName(currentUid: String): String {
        return if (ownerUid == currentUid) participantName else callerName.ifBlank { participantName }
    }
}

@Keep
@IgnoreExtraProperties
data class CallRecording(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val callId: String = "",
    val title: String = "",
    val participantName: String = "",
    val mediaType: String = "audio",
    val durationSec: Long = 1L,
    val sizeBytes: Long = 1L,
    val audioBase64: String = "",
    val localFilePath: String = "",
    val consentVerified: Boolean = true,
    val encryptionAlgorithm: String = "AES-256-GCM",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SecurityReport(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val targetName: String = "",
    val category: String = "other",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
