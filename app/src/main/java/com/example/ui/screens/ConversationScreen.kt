package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.ui.components.Base64BitmapImage
import com.example.ui.components.ContactAvatarView
import com.example.ui.components.OnlinePresenceRow
import com.example.ui.components.SecretGuardEmptyState
import com.example.ui.components.SecretGuardIconButton
import com.example.ui.theme.LocalSecretGuardTokens
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Screen 4 — Real-Time Multi-User Encrypted Conversation:
 * - Header with partner identity, live presence from Firestore `/users`, and real audio/Full HD video call buttons.
 * - Real-time message synchronization across devices and live read receipts.
 * - HD Photo (`ImageOnly`), HD Video (`VideoOnly` with keyframe + duration), and Document (`OpenDocument`) attachments.
 * - Real hardware `MediaRecorder` voice notes and real `MediaPlayer` audio playback.
 * - Permanent message and voice note deletion (`onDeleteMessagePermanently`) for all participants in real time.
 */
@Composable
fun ConversationScreen(
    currentUserId: String,
    conversation: Conversation?,
    directoryUsersState: UiState<List<UserProfile>>,
    messagesState: UiState<List<ChatMessage>>,
    playingMediaId: String?,
    liveMicAmplitude: Float,
    onBackClick: () -> Unit,
    onStartAudioCall: (String, String, String) -> Unit,
    onStartVideoCall: (String, String, String) -> Unit,
    onSendMessage: (String, String, Long, String) -> Unit,
    onSendImageAttachment: (Uri) -> Unit,
    onSendVideoAttachment: (Uri) -> Unit,
    onSendDocumentAttachment: (Uri) -> Unit,
    onDeleteMessagePermanently: (ChatMessage) -> Unit,
    onStartHardwareVoiceRecording: () -> Boolean,
    onCancelHardwareVoiceRecording: () -> Unit,
    onStopAndSendVoiceMessage: (Long) -> Unit,
    onTogglePlayAudioMessage: (ChatMessage) -> Unit,
    onDecryptMessage: (ciphertext: String, fallback: String, channelId: String) -> String,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val tokens = LocalSecretGuardTokens.current

    val partnerUid = remember(conversation, currentUserId) {
        conversation?.memberUids?.firstOrNull { it != currentUserId }
            ?: conversation?.partnerUid.orEmpty()
    }
    val partnerName = remember(conversation, currentUserId) {
        if (conversation == null) {
            "Secret Chat"
        } else if (conversation.ownerUid == currentUserId) {
            conversation.participantName.ifBlank { "Trusted Contact" }
        } else {
            conversation.ownerName.ifBlank { conversation.participantName }
        }
    }

    val directoryUsers = (directoryUsersState as? UiState.Success)?.data.orEmpty()
    val livePartner = remember(directoryUsers, partnerUid) {
        directoryUsers.firstOrNull { it.uid == partnerUid }
    }
    val avatarKey = livePartner?.photoUrl?.ifBlank {
        conversation?.participantAvatarKey ?: "custom"
    } ?: (conversation?.participantAvatarKey?.ifBlank { "custom" } ?: "custom")
    val isOnline = livePartner?.onlineStatus == "online" || conversation?.presenceState == "online"
    val partnerDemo = livePartner?.demographicSummary().orEmpty()

    var messageText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showVoiceNoteDialog by remember { mutableStateOf(false) }
    var inspectedMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val hdPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendImageAttachment(uri)
        }
    }

    val hdVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendVideoAttachment(uri)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onSendDocumentAttachment(uri)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showVoiceNoteDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .widthIn(max = 600.dp)
    ) {
        // 1. Top Conversation Bar (56dp height)
        Surface(
            color = tokens.bg,
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(tokens.topBarHeight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = tokens.sp8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecretGuardIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    testTag = "chat_back_button",
                    onClick = onBackClick
                )

                ContactAvatarView(
                    avatarKey = avatarKey,
                    displayName = partnerName,
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(tokens.sp8))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partnerName,
                        fontSize = tokens.headingSize,
                        lineHeight = tokens.headingLineHeight,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    OnlinePresenceRow(
                        isOnline = isOnline,
                        label = buildString {
                            append(if (isOnline) "Online" else "Offline")
                            if (partnerDemo.isNotBlank()) append(" • $partnerDemo")
                        }
                    )
                }

                SecretGuardIconButton(
                    icon = Icons.Filled.Call,
                    contentDescription = "Audio Call",
                    tint = tokens.primary,
                    testTag = "chat_audio_call_button",
                    onClick = { onStartAudioCall(partnerName, avatarKey, partnerUid) }
                )

                SecretGuardIconButton(
                    icon = Icons.Filled.Videocam,
                    contentDescription = "Full HD Video Call",
                    tint = tokens.primary,
                    testTag = "chat_video_call_button",
                    onClick = { onStartVideoCall(partnerName, avatarKey, partnerUid) }
                )
            }
        }

        // 2. Real-Time Message List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (messagesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = tokens.primary)
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(tokens.sp16),
                        contentAlignment = Alignment.Center
                    ) {
                        SecretGuardEmptyState(
                            icon = Icons.Filled.Lock,
                            title = "Message Sync Error",
                            subtitle = messagesState.message
                        )
                    }
                }

                is UiState.Success -> {
                    val messages = messagesState.data
                    if (messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(tokens.sp16),
                            contentAlignment = Alignment.Center
                        ) {
                            SecretGuardEmptyState(
                                icon = Icons.Filled.Lock,
                                title = "Private Chat",
                                subtitle = "Send a message, photo, video, or voice note to $partnerName."
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = tokens.sp16, vertical = tokens.sp12),
                            verticalArrangement = Arrangement.spacedBy(tokens.sp12)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                val isMine = msg.senderUid == currentUserId || (msg.senderUid.isBlank() && msg.isOutgoing)
                                val decryptedText = remember(msg.id, msg.encryptedCiphertext, msg.text) {
                                    onDecryptMessage(
                                        msg.encryptedCiphertext,
                                        msg.text,
                                        msg.conversationId.ifBlank { "global" }
                                    )
                                }
                                ChatMessageBubbleItem(
                                    message = msg,
                                    decryptedText = decryptedText,
                                    isOutgoing = isMine,
                                    isPlaying = playingMediaId == msg.id,
                                    onTogglePlay = { onTogglePlayAudioMessage(msg) },
                                    onInspectOrDelete = { inspectedMessage = msg },
                                    onDeletePermanently = { onDeleteMessagePermanently(msg) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom Pill Composer Bar (keeps input visible when keyboard opens via imePadding)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.sp16, vertical = tokens.sp8)
        ) {
            Surface(
                shape = CircleShape,
                color = tokens.surface2,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tokens.border, CircleShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = tokens.sp8, vertical = tokens.sp4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_attach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AttachFile,
                            contentDescription = "Attach HD Photo, Video or File",
                            tint = tokens.textMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = tokens.sp4),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (messageText.isEmpty()) {
                            Text(
                                text = "Type a message...",
                                color = tokens.textMuted,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            textStyle = TextStyle(
                                color = tokens.text,
                                fontSize = 16.sp
                            ),
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_message_input")
                        )
                    }

                    IconButton(
                        onClick = {
                            val hasMic = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMic) {
                                showVoiceNoteDialog = true
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Record Voice Message",
                            tint = tokens.primary
                        )
                    }

                    IconButton(
                        onClick = { messageText += " \uD83D\uDE0A" },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_emoji_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SentimentSatisfiedAlt,
                            contentDescription = "Add Emoji",
                            tint = tokens.textMuted
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = tokens.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                val trimmed = messageText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendMessage(trimmed, "text", 0L, "")
                                    messageText = ""
                                }
                            }
                            .testTag("chat_send_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = tokens.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Real Attachment Picker Dialog (HD Photo, HD Video, Files/Documents, Voice Note)
    if (showAttachmentSheet) {
        AlertDialog(
            onDismissRequest = { showAttachmentSheet = false },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Text(
                    text = "Share Media & Files",
                    fontSize = tokens.headingSize,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                    Surface(
                        shape = RoundedCornerShape(tokens.radiusCard),
                        color = tokens.surface2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(tokens.radiusCard))
                            .clickable {
                                showAttachmentSheet = false
                                hdPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(tokens.sp12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = tokens.primary)
                            Spacer(modifier = Modifier.width(tokens.sp12))
                            Text("Send HD Photo", fontWeight = FontWeight.Bold, color = tokens.text)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(tokens.radiusCard),
                        color = tokens.surface2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(tokens.radiusCard))
                            .clickable {
                                showAttachmentSheet = false
                                hdVideoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(tokens.sp12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = tokens.primary)
                            Spacer(modifier = Modifier.width(tokens.sp12))
                            Text("Send HD Video", fontWeight = FontWeight.Bold, color = tokens.text)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(tokens.radiusCard),
                        color = tokens.surface2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(tokens.radiusCard))
                            .clickable {
                                showAttachmentSheet = false
                                documentPickerLauncher.launch(arrayOf("*/*"))
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(tokens.sp12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = tokens.primary)
                            Spacer(modifier = Modifier.width(tokens.sp12))
                            Text("Send File / Document", fontWeight = FontWeight.Bold, color = tokens.text)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(tokens.radiusCard),
                        color = tokens.surface2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(tokens.radiusCard))
                            .clickable {
                                showAttachmentSheet = false
                                val hasMic = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) {
                                    showVoiceNoteDialog = true
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(tokens.sp12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = tokens.primary)
                            Spacer(modifier = Modifier.width(tokens.sp12))
                            Text("Record Voice Note", fontWeight = FontWeight.Bold, color = tokens.text)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachmentSheet = false }) {
                    Text("Close", color = tokens.textMuted)
                }
            }
        )
    }

    // Real Hardware Voice Note Recorder Modal for Chat
    if (showVoiceNoteDialog) {
        var isRecording by remember { mutableStateOf(false) }
        var elapsedSec by remember { mutableLongStateOf(0L) }

        LaunchedEffect(Unit) {
            onStartHardwareVoiceRecording()
            isRecording = true
        }

        LaunchedEffect(isRecording) {
            while (isRecording) {
                delay(1000L)
                elapsedSec += 1L
            }
        }

        AlertDialog(
            onDismissRequest = {
                isRecording = false
                onCancelHardwareVoiceRecording()
                showVoiceNoteDialog = false
            },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = tokens.danger)
                    Spacer(modifier = Modifier.width(tokens.sp8))
                    Text("Voice Note", fontWeight = FontWeight.Bold, color = tokens.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp12)) {
                    Text(
                        text = "● REC ${String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)}",
                        fontSize = tokens.bodySize,
                        color = tokens.danger,
                        fontWeight = FontWeight.Bold
                    )
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        val barCount = 22
                        val spacing = size.width / barCount
                        val amp = liveMicAmplitude.coerceIn(0.15f, 1.0f)
                        for (i in 0 until barCount) {
                            val phase = (((i * 5 + elapsedSec.toInt() * 3) % 7) + 1) / 7f
                            val barH = (size.height * (0.20f + 0.78f * amp * phase)).coerceIn(6f, size.height)
                            drawRoundRect(
                                color = tokens.primary,
                                topLeft = Offset(i * spacing + 3f, (size.height - barH) / 2f),
                                size = Size(spacing * 0.55f, barH),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRecording = false
                        showVoiceNoteDialog = false
                        onStopAndSendVoiceMessage(elapsedSec.coerceAtLeast(1L))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(tokens.sp8))
                    Text("Send (${elapsedSec.coerceAtLeast(1L)}s)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isRecording = false
                        onCancelHardwareVoiceRecording()
                        showVoiceNoteDialog = false
                    }
                ) {
                    Text("Cancel", color = tokens.textMuted)
                }
            }
        )
    }

    // Permanent Delete Confirmation Modal when user taps a message
    if (inspectedMessage != null) {
        val msg = inspectedMessage!!
        AlertDialog(
            onDismissRequest = { inspectedMessage = null },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = tokens.danger)
                    Spacer(modifier = Modifier.width(tokens.sp8))
                    Text("Delete Message", fontWeight = FontWeight.Bold, color = tokens.text)
                }
            },
            text = {
                Text(
                    text = "Permanently delete this message for everyone in the chat?",
                    fontSize = tokens.bodySize,
                    color = tokens.text
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        inspectedMessage = null
                        onDeleteMessagePermanently(msg)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.danger),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(tokens.sp8))
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedMessage = null }) {
                    Text("Cancel", color = tokens.textMuted)
                }
            }
        )
    }
}

@Composable
private fun ChatMessageBubbleItem(
    message: ChatMessage,
    decryptedText: String,
    isOutgoing: Boolean,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onInspectOrDelete: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    val timeText = remember(message.createdAt, message.mediaLabel) {
        if (message.mediaLabel.endsWith("AM") || message.mediaLabel.endsWith("PM")) {
            message.mediaLabel
        } else {
            val date = message.createdAt?.toDate()
            if (date != null) {
                SimpleDateFormat("hh:mm a", Locale.US).format(date)
            } else {
                "Just now"
            }
        }
    }

    val isRead = message.deliveryState == "read"
    val bubbleColor = if (isOutgoing) tokens.surface2 else tokens.bg
    val maxBubbleWidth = if (tokens.isCompact) 252.dp else 284.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOutgoing) {
            IconButton(
                onClick = onDeletePermanently,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("delete_message_${message.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Message Permanently",
                    tint = tokens.textMuted.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        when (message.messageType) {
            "image", "video" -> {
                val isVideo = message.messageType == "video"
                Surface(
                    shape = RoundedCornerShape(tokens.radiusCard),
                    color = bubbleColor,
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusCard))
                        .clip(RoundedCornerShape(tokens.radiusCard))
                        .clickable { onInspectOrDelete() }
                ) {
                    Column(modifier = Modifier.padding(tokens.sp8)) {
                        if (message.mediaBase64.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (tokens.isCompact) 148.dp else 176.dp)
                                    .clip(RoundedCornerShape(tokens.radiusSmall)),
                                contentAlignment = Alignment.Center
                            ) {
                                Base64BitmapImage(
                                    base64Data = message.mediaBase64,
                                    contentDescription = decryptedText,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isVideo) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "HD Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(tokens.radiusSmall),
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(tokens.sp8)
                                    ) {
                                        Text(
                                            text = "00:${String.format("%02d", message.mediaDurationSec.coerceAtLeast(1L))}",
                                            color = Color.White,
                                            fontSize = tokens.captionSize,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = tokens.sp8, vertical = tokens.sp4)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(tokens.sp4))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = tokens.sp4),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = decryptedText,
                                fontSize = tokens.captionSize,
                                color = tokens.text,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = timeText,
                                    fontSize = tokens.captionSize,
                                    color = tokens.textMuted
                                )
                                if (isOutgoing) {
                                    Spacer(modifier = Modifier.width(tokens.sp4))
                                    Icon(
                                        imageVector = if (isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                                        contentDescription = message.deliveryState,
                                        tint = if (isRead) tokens.primary else tokens.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "audio", "recording" -> {
                // Real Playable Audio Waveform Bubble with Permanent Delete support
                Surface(
                    shape = RoundedCornerShape(tokens.radiusCard),
                    color = bubbleColor,
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusCard))
                        .clip(RoundedCornerShape(tokens.radiusCard))
                        .clickable { onTogglePlay() }
                ) {
                    Column(
                        modifier = Modifier.padding(tokens.sp12)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(tokens.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play Audio Note",
                                    tint = tokens.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            val barHeights = listOf(
                                0.35f, 0.55f, 0.85f, 0.45f, 0.95f, 0.60f, 0.40f,
                                0.80f, 1.0f, 0.65f, 0.50f, 0.90f, 0.45f, 0.75f,
                                0.55f, 0.35f
                            )
                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                            ) {
                                val step = size.width / barHeights.size
                                barHeights.forEachIndexed { idx, ratio ->
                                    val h = size.height * ratio
                                    drawRoundRect(
                                        color = if (isPlaying && idx % 2 == 0) tokens.text else tokens.primary,
                                        topLeft = Offset(idx * step + 2f, (size.height - h) / 2f),
                                        size = Size((step * 0.52f).coerceAtLeast(3f), h),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }

                            Text(
                                text = "00:${String.format("%02d", message.mediaDurationSec.coerceAtLeast(1L))}",
                                fontSize = tokens.captionSize,
                                color = tokens.text,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(tokens.sp4))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPlaying) "Playing..." else "Voice note",
                                fontSize = tokens.captionSize,
                                color = tokens.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = timeText,
                                    fontSize = tokens.captionSize,
                                    color = tokens.textMuted
                                )
                                if (isOutgoing) {
                                    Spacer(modifier = Modifier.width(tokens.sp4))
                                    Icon(
                                        imageVector = if (isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                                        contentDescription = message.deliveryState,
                                        tint = if (isRead) tokens.primary else tokens.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Text or Document Message Bubble
                Surface(
                    shape = RoundedCornerShape(
                        topStart = tokens.radiusCard,
                        topEnd = tokens.radiusCard,
                        bottomStart = if (isOutgoing) tokens.radiusCard else tokens.radiusSmall,
                        bottomEnd = if (isOutgoing) tokens.radiusSmall else tokens.radiusCard
                    ),
                    color = bubbleColor,
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .border(
                            1.dp,
                            tokens.border,
                            RoundedCornerShape(
                                topStart = tokens.radiusCard,
                                topEnd = tokens.radiusCard,
                                bottomStart = if (isOutgoing) tokens.radiusCard else tokens.radiusSmall,
                                bottomEnd = if (isOutgoing) tokens.radiusSmall else tokens.radiusCard
                            )
                        )
                        .clip(
                            RoundedCornerShape(
                                topStart = tokens.radiusCard,
                                topEnd = tokens.radiusCard,
                                bottomStart = if (isOutgoing) tokens.radiusCard else tokens.radiusSmall,
                                bottomEnd = if (isOutgoing) tokens.radiusSmall else tokens.radiusCard
                            )
                        )
                        .clickable { onInspectOrDelete() }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = tokens.sp12, vertical = tokens.sp8)
                    ) {
                        Text(
                            text = decryptedText,
                            fontSize = tokens.bodySize,
                            lineHeight = tokens.bodyLineHeight,
                            color = tokens.text
                        )
                        Spacer(modifier = Modifier.height(tokens.sp4))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = timeText,
                                fontSize = tokens.captionSize,
                                color = tokens.textMuted
                            )
                            if (isOutgoing) {
                                Spacer(modifier = Modifier.width(tokens.sp4))
                                Icon(
                                    imageVector = if (isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                                    contentDescription = message.deliveryState,
                                    tint = if (isRead) tokens.primary else tokens.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isOutgoing) {
            IconButton(
                onClick = onDeletePermanently,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("delete_message_${message.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Message Permanently",
                    tint = tokens.textMuted.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
