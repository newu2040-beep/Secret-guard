package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "Secret User",
    val email: String = "",
    val photoUrl: String = "",
    val onlineStatus: String = "online",
    val accountType: String = "google",
    val profileVisibility: String = "contacts",
    val appLockEnabled: Boolean = false,
    val autoRecordCalls: Boolean = true,
    val notificationAlert: Boolean = true,
    val recordingQuality: String = "HD 720p",
    val retentionDays: Long = 30L,
    val blockedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TrustedContact(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val contactName: String = "",
    val contactHandle: String = "",
    val avatarKey: String = "priya",
    val status: String = "accepted",
    val presenceState: String = "online",
    val lastActivityText: String = "Video Call • 2m ago",
    val lastActivityType: String = "video_call",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val participantName: String = "",
    val participantAvatarKey: String = "priya",
    val presenceState: String = "online",
    val lastMessageText: String = "",
    val unreadCount: Long = 0L,
    val encryptionProtocol: String = "AES-256-GCM",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val conversationId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val isOutgoing: Boolean = true,
    val messageType: String = "text",
    val text: String = "",
    val encryptedCiphertext: String = "",
    val mediaDurationSec: Long = 0L,
    val mediaLabel: String = "",
    val deliveryState: String = "read",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val participantName: String = "",
    val participantAvatarKey: String = "priya",
    val callType: String = "video",
    val status: String = "connected",
    val consentState: String = "none",
    val isRecording: Boolean = false,
    val durationSec: Long = 0L,
    val encryptionVerified: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallRecording(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val callId: String = "",
    val title: String = "",
    val participantName: String = "",
    val mediaType: String = "audio",
    val durationSec: Long = 12L,
    val sizeBytes: Long = 245760L,
    val consentVerified: Boolean = true,
    val encryptionAlgorithm: String = "AES-256-GCM",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SecurityReport(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val targetName: String = "",
    val category: String = "other",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
