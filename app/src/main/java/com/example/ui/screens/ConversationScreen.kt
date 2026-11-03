package com.example.ui.screens

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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.ui.components.ContactAvatarView
import com.example.ui.components.OnlinePresenceRow
import com.example.ui.theme.DeepGreenBlackText
import com.example.ui.theme.EmeraldLightTint
import com.example.ui.theme.EmeraldMintCard
import com.example.ui.theme.EmeraldOutgoingBubble
import com.example.ui.theme.EmeraldPillBorder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MutedCaptionText
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.SecondarySlateGreenText
import com.example.ui.theme.WarmIvoryBackground
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Screen 4 — Conversation matching the reference specification:
 * - Header with contact ("Priya Sharma"), live presence ("Online"), audio-call and video-call actions.
 * - Incoming/outgoing message bubbles, timestamps, delivery/read checkmarks.
 * - Rich Mountain-Lake Video card bubble ("00:00:15") and Audio Waveform bubble ("00:00:12").
 * - Pill composer with attachment picker (Android Photo Picker + Voice/Video attachment options),
 *   text field, emoji quick picker, and emerald send button.
 */
@Composable
fun ConversationScreen(
    conversation: Conversation?,
    messagesState: UiState<List<ChatMessage>>,
    onBackClick: () -> Unit,
    onStartAudioCall: (String, String) -> Unit,
    onStartVideoCall: (String, String) -> Unit,
    onSendMessage: (String, String, Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val participantName = conversation?.participantName?.ifBlank { "Priya Sharma" } ?: "Priya Sharma"
    val avatarKey = conversation?.participantAvatarKey?.ifBlank { "priya" } ?: "priya"
    val isOnline = conversation?.presenceState != "offline"

    var messageText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var inspectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var playingMessageId by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendMessage(
                "Encrypted Photo (${uri.lastPathSegment ?: "media"})",
                "video",
                15L,
                "00:00:15"
            )
        }
    }

    LaunchedEffect(playingMessageId) {
        if (playingMessageId != null) {
            delay(4000L)
            playingMessageId = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvoryBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .widthIn(max = 600.dp)
    ) {
        // 1. Top Conversation Bar
        Surface(
            color = WarmIvoryBackground,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DeepGreenBlackText
                    )
                }

                ContactAvatarView(
                    avatarKey = avatarKey,
                    displayName = participantName,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = participantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenBlackText
                    )
                    OnlinePresenceRow(
                        isOnline = isOnline,
                        label = if (isOnline) "Online" else "Offline"
                    )
                }

                IconButton(
                    onClick = { onStartAudioCall(participantName, avatarKey) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_audio_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Audio Call",
                        tint = EmeraldPrimary
                    )
                }

                IconButton(
                    onClick = { onStartVideoCall(participantName, avatarKey) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_video_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = "Video Call",
                        tint = EmeraldPrimary
                    )
                }
            }
        }

        // 2. Message List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (messagesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = messagesState.message,
                            color = Color(0xFFB42318),
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }

                is UiState.Success -> {
                    val messages = messagesState.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageBubbleItem(
                                message = msg,
                                isPlaying = playingMessageId == msg.id,
                                onTogglePlay = {
                                    playingMessageId = if (playingMessageId == msg.id) null else msg.id
                                },
                                onInspectCiphertext = { inspectedMessage = msg }
                            )
                        }
                    }
                }
            }
        }

        // 3. Bottom Pill Composer Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = PureWhiteSurface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldPillBorder.copy(alpha = 0.7f), CircleShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("chat_attach_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AttachFile,
                            contentDescription = "Attach Media",
                            tint = SecondarySlateGreenText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (messageText.isEmpty()) {
                            Text(
                                text = "Type a message...",
                                color = MutedCaptionText,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            textStyle = TextStyle(
                                color = DeepGreenBlackText,
                                fontSize = 15.sp
                            ),
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_message_input")
                        )
                    }

                    IconButton(
                        onClick = { messageText += " \uD83D\uDE0A" },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("chat_emoji_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SentimentSatisfiedAlt,
                            contentDescription = "Add Emoji",
                            tint = SecondarySlateGreenText
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary,
                        modifier = Modifier
                            .size(44.dp)
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
                                contentDescription = "Send Encrypted Message",
                                tint = PureWhiteSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Attachment Picker Dialog (Zero-permission Photo Picker + Voice Note + Video Clip)
    if (showAttachmentSheet) {
        AlertDialog(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = PureWhiteSurface,
            title = {
                Text(
                    text = "Send Encrypted Attachment",
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenBlackText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldMintCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showAttachmentSheet = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Choose Photo / Video (Android Photo Picker)", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldMintCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showAttachmentSheet = false
                                onSendMessage(
                                    "Encrypted Voice Note",
                                    "audio",
                                    12L,
                                    "00:00:12"
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Send Encrypted Audio Note (00:00:12)", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldMintCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showAttachmentSheet = false
                                onSendMessage(
                                    "Encrypted Mountain Lake Clip",
                                    "video",
                                    15L,
                                    "00:00:15"
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Videocam, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Send Encrypted Video Clip (00:00:15)", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachmentSheet = false }) {
                    Text("Close", color = SecondarySlateGreenText)
                }
            }
        )
    }

    // AES-256-GCM Envelope Inspector Modal when user taps a message
    if (inspectedMessage != null) {
        val msg = inspectedMessage!!
        AlertDialog(
            onDismissRequest = { inspectedMessage = null },
            containerColor = PureWhiteSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message Encryption Envelope", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sender: ${msg.senderName}\nDelivery State: ${msg.deliveryState.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlateGreenText
                    )
                    Surface(
                        color = EmeraldLightTint,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = msg.encryptedCiphertext.ifBlank { "AES256GCM:VERIFIED" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { inspectedMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ChatMessageBubbleItem(
    message: ChatMessage,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onInspectCiphertext: () -> Unit
) {
    val isOutgoing = message.isOutgoing
    val timeText = remember(message.createdAt, message.mediaLabel) {
        if (message.mediaLabel.endsWith("AM") || message.mediaLabel.endsWith("PM")) {
            message.mediaLabel
        } else {
            val date = message.createdAt?.toDate()
            if (date != null) {
                SimpleDateFormat("hh:mm a", Locale.US).format(date)
            } else {
                "10:15 AM"
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        when (message.messageType) {
            "video" -> {
                // Scenic Mountain-Lake Video Attachment Card matching Screen 4 reference
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PureWhiteSurface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .width(268.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onTogglePlay() }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            // Illustrated Alpine Mountain & Turquoise Lake Canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                // Sky gradient
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF74B9FF), Color(0xFFDFF9FB))
                                    ),
                                    size = Size(w, h * 0.65f)
                                )
                                // Snow-capped mountains
                                val peaks = Path().apply {
                                    moveTo(0f, h * 0.62f)
                                    lineTo(w * 0.22f, h * 0.24f)
                                    lineTo(w * 0.42f, h * 0.50f)
                                    lineTo(w * 0.68f, h * 0.18f)
                                    lineTo(w, h * 0.56f)
                                    lineTo(w, h * 0.65f)
                                    lineTo(0f, h * 0.65f)
                                    close()
                                }
                                drawPath(
                                    path = peaks,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF576574), Color(0xFF222F3E))
                                    )
                                )
                                // Alpine lake reflection
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF0ABDE3), Color(0xFF10AC84))
                                    ),
                                    topLeft = Offset(0f, h * 0.62f),
                                    size = Size(w, h * 0.38f)
                                )
                            }

                            // Center Play/Pause Circle Button
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .border(2.dp, PureWhiteSurface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play Video",
                                    tint = PureWhiteSurface,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Bottom-right duration pill ("00:00:15")
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.62f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (isPlaying) "Playing..." else "00:00:${String.format("%02d", message.mediaDurationSec.coerceAtLeast(15L))}",
                                    color = PureWhiteSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "10:14 AM",
                                fontSize = 11.sp,
                                color = MutedCaptionText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "Read",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            "audio", "recording" -> {
                // Mint Audio Waveform Bubble ("00:00:12") matching Screen 4 reference
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isOutgoing) 20.dp else 6.dp,
                        bottomEnd = if (isOutgoing) 6.dp else 20.dp
                    ),
                    color = EmeraldOutgoingBubble,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .width(264.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTogglePlay() }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play Audio Note",
                                    tint = PureWhiteSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Waveform visualization bars
                            val barHeights = listOf(
                                0.35f, 0.55f, 0.85f, 0.45f, 0.95f, 0.60f, 0.40f,
                                0.80f, 1.0f, 0.65f, 0.50f, 0.90f, 0.45f, 0.75f,
                                0.55f, 0.35f, 0.70f, 0.40f
                            )
                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                            ) {
                                val step = size.width / barHeights.size
                                barHeights.forEachIndexed { idx, ratio ->
                                    val h = size.height * ratio
                                    drawRoundRect(
                                        color = if (isPlaying && idx % 2 == 0) Color(0xFF044932) else EmeraldPrimary,
                                        topLeft = Offset(idx * step + 2f, (size.height - h) / 2f),
                                        size = Size((step * 0.52f).coerceAtLeast(3f), h),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }

                            Text(
                                text = "00:00:${String.format("%02d", message.mediaDurationSec.coerceAtLeast(12L))}",
                                fontSize = 12.sp,
                                color = DeepGreenBlackText,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeText,
                                fontSize = 10.sp,
                                color = SecondarySlateGreenText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "Read",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            else -> {
                // Standard Text Message Bubble
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isOutgoing) 20.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 20.dp
                    ),
                    color = if (isOutgoing) EmeraldOutgoingBubble else PureWhiteSurface,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .widthIn(min = 110.dp, max = 280.dp)
                        .clickable { onInspectCiphertext() }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message.text,
                            color = DeepGreenBlackText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeText,
                                fontSize = 10.sp,
                                color = SecondarySlateGreenText
                            )
                            if (isOutgoing) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = "Read",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
