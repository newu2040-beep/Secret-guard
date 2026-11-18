package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.TrustedContact
import com.example.data.model.UserProfile
import com.example.ui.components.Base64BitmapImage
import com.example.ui.components.ContactAvatarView
import com.example.ui.components.LiveCameraPreview
import com.example.ui.theme.CallBackdropBottom
import com.example.ui.theme.CallBackdropMid
import com.example.ui.theme.CallBackdropTop
import com.example.ui.theme.EndCallRed
import com.example.ui.theme.LocalSecretGuardTokens
import com.example.ui.theme.OnlineGreenStatus
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.RecordingRedStatus
import com.example.ui.theme.WarningAmberStatus
import com.example.ui.viewmodel.ActiveCallUiState
import com.example.ui.viewmodel.UiState

/**
 * Screen 3 — Active Call with real-time 2-Way Voice Streaming (`AudioRecord` + `AudioTrack`),
 * Full HD 1080p CameraX hardware video preview & low-latency frame sync, Group Call participant
 * invite/join (up to 8 members), and Call Recording.
 */
@Composable
fun ActiveCallScreen(
    currentUserId: String,
    callState: ActiveCallUiState,
    contactsState: UiState<List<TrustedContact>>,
    directoryUsersState: UiState<List<UserProfile>>,
    onBackClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleHdResolution: () -> Unit,
    onInviteParticipant: (uid: String, name: String) -> Unit,
    onPublishVideoFrame: (String) -> Unit,
    onOpenConsentDialog: () -> Unit,
    onDismissConsentDialog: () -> Unit,
    onConfirmAllParticipantsConsent: () -> Unit,
    onWithdrawConsentAndStop: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val tokens = LocalSecretGuardTokens.current
    var showLocalPipPreview by remember { mutableStateOf(true) }
    var showInviteParticipantDialog by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionNotice by remember { mutableStateOf<String?>(null) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val micGranted = result[Manifest.permission.RECORD_AUDIO] ?: false
        val camGranted = result[Manifest.permission.CAMERA] ?: false
        hasCameraPermission = camGranted
        permissionNotice = if (!micGranted && !camGranted) {
            "Camera & Microphone permissions were declined."
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasCam = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = hasCam

        if (!hasMic || !hasCam) {
            permissionsLauncher.launch(
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CallBackdropTop,
                        CallBackdropMid,
                        CallBackdropBottom
                    )
                )
            )
    ) {
        // Render remote participant's Full HD 1080p video feed full-bleed with high filter quality
        if (callState.remoteVideoFrame.isNotBlank()) {
            Base64BitmapImage(
                base64Data = callState.remoteVideoFrame,
                contentDescription = "Remote Full HD Video Stream",
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFAD9A1).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.52f),
                        radius = w * 0.65f
                    ),
                    radius = w * 0.65f,
                    center = Offset(w * 0.5f, h * 0.52f)
                )

                val backMountain = Path().apply {
                    moveTo(0f, h * 0.62f)
                    lineTo(w * 0.28f, h * 0.48f)
                    lineTo(w * 0.55f, h * 0.57f)
                    lineTo(w * 0.82f, h * 0.45f)
                    lineTo(w, h * 0.55f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = backMountain,
                    color = Color(0xFF1F3F34).copy(alpha = 0.65f)
                )

                val frontSlope = Path().apply {
                    moveTo(0f, h * 0.72f)
                    quadraticTo(w * 0.45f, h * 0.64f, w, h * 0.76f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = frontSlope,
                    color = Color(0xFF13261C).copy(alpha = 0.82f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = tokens.sp16, vertical = tokens.sp12),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Back arrow, Recording Pill, Add Participant (Group Call), and PiP Toggle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(tokens.sp8),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("call_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhiteSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (callState.isRecording && callState.consentState == "all_consented") {
                        Surface(
                            shape = CircleShape,
                            color = RecordingRedStatus,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onWithdrawConsentAndStop() }
                                .testTag("active_recording_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = tokens.sp12, vertical = tokens.sp8),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(tokens.sp4)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FiberManualRecord,
                                    contentDescription = "Recording Active",
                                    tint = PureWhiteSurface,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "REC ${formatCallDuration(callState.recordingElapsedSeconds)} • Stop",
                                    color = PureWhiteSurface,
                                    fontSize = tokens.captionSize,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (callState.consentState == "requested") {
                        Surface(
                            shape = CircleShape,
                            color = WarningAmberStatus,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onOpenConsentDialog() }
                                .testTag("request_recording_consent_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = tokens.sp12, vertical = tokens.sp8),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(tokens.sp4)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FiberManualRecord,
                                    contentDescription = null,
                                    tint = PureWhiteSurface,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Approve Record",
                                    color = PureWhiteSurface,
                                    fontSize = tokens.captionSize,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onOpenConsentDialog() }
                                .testTag("request_recording_consent_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = tokens.sp12, vertical = tokens.sp8),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(tokens.sp4)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8A80),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Record",
                                    color = PureWhiteSurface,
                                    fontSize = tokens.captionSize,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { showInviteParticipantDialog = true }
                                .testTag("call_add_participant_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = "Add Participant to Group Call",
                                    tint = PureWhiteSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { showLocalPipPreview = !showLocalPipPreview }
                                .testTag("call_pip_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInFull,
                                    contentDescription = "Toggle Local Preview",
                                    tint = PureWhiteSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Live 2-Way Voice + Full HD 1080p Resolution & Group Member Pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.sp8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.38f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onToggleHdResolution() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = tokens.sp12, vertical = tokens.sp4),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(tokens.sp4)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HighQuality,
                                contentDescription = null,
                                tint = OnlineGreenStatus,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (callState.isFullHd1080p) "Full HD 1080p" else "HD 720p",
                                color = PureWhiteSurface,
                                fontSize = tokens.captionSize,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.38f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = tokens.sp12, vertical = tokens.sp4),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(tokens.sp4)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = if (callState.isMuted) WarningAmberStatus else OnlineGreenStatus,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (callState.isMuted) {
                                    "Muted"
                                } else {
                                    "Live Voice (${callState.memberUids.size.coerceAtLeast(1)})"
                                },
                                color = PureWhiteSurface,
                                fontSize = tokens.captionSize,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (permissionNotice != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(tokens.radiusSmall)
                    ) {
                        Text(
                            text = permissionNotice!!,
                            color = PureWhiteSurface,
                            fontSize = tokens.captionSize,
                            modifier = Modifier.padding(horizontal = tokens.sp12, vertical = tokens.sp4)
                        )
                    }
                }
            }

            // Center Dominant Participant Avatar + Name + Real-Time Call Status & Duration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val avatarSize = when {
                    tokens.isCompact && callState.isCameraOn && showLocalPipPreview -> 96.dp
                    callState.isCameraOn && showLocalPipPreview && hasCameraPermission -> 120.dp
                    tokens.isCompact -> 128.dp
                    else -> 152.dp
                }

                ContactAvatarView(
                    avatarKey = if (callState.memberUids.size > 2) "group" else callState.participantAvatarKey,
                    displayName = callState.participantName.ifBlank { "Participant" },
                    size = avatarSize,
                    showGlowRing = true
                )

                Spacer(modifier = Modifier.height(tokens.sp12))

                Text(
                    text = callState.participantName.ifBlank { "Secret Call" },
                    color = PureWhiteSurface,
                    fontSize = tokens.titleSize,
                    lineHeight = tokens.titleLineHeight,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(tokens.sp4))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
                ) {
                    Text(
                        text = if (callState.status == "connected") {
                            formatCallDuration(callState.elapsedSeconds)
                        } else {
                            callState.connectionState
                        },
                        color = PureWhiteSurface.copy(alpha = 0.92f),
                        fontSize = tokens.bodySize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .size(tokens.sp8)
                            .clip(CircleShape)
                            .background(
                                if (callState.status == "connected") OnlineGreenStatus else WarningAmberStatus
                            )
                    )
                }

                // Real Hardware CameraX Full HD 1080p Local PiP Preview Card
                if (showLocalPipPreview && callState.isCameraOn && hasCameraPermission) {
                    Spacer(modifier = Modifier.height(tokens.sp12))
                    val pipWidth = if (tokens.isCompact) 112.dp else 132.dp
                    val pipHeight = if (tokens.isCompact) 144.dp else 168.dp
                    Surface(
                        shape = RoundedCornerShape(tokens.radiusCard),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(width = pipWidth, height = pipHeight)
                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(tokens.radiusCard))
                            .clip(RoundedCornerShape(tokens.radiusCard))
                            .clickable { onSwitchCamera() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LiveCameraPreview(
                                isFrontCamera = callState.isFrontCamera,
                                isFullHd1080p = callState.isFullHd1080p,
                                onFrameCaptured = onPublishVideoFrame,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(tokens.radiusSmall),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(tokens.sp4)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = tokens.sp8, vertical = tokens.sp4),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(tokens.sp4)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Cameraswitch,
                                        contentDescription = "Switch Camera",
                                        tint = PureWhiteSurface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (callState.isFrontCamera) "1080p Front" else "1080p Rear",
                                        color = PureWhiteSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Call Controls + Red End Call Button + Encrypted Call Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(tokens.sp16),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlCircleButton(
                        label = "Mute",
                        icon = if (callState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        isActive = callState.isMuted,
                        testTag = "call_mute_button",
                        onClick = onToggleMute
                    )
                    CallControlCircleButton(
                        label = "Speaker",
                        icon = if (callState.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        isActive = callState.isSpeakerOn,
                        testTag = "call_speaker_button",
                        onClick = onToggleSpeaker
                    )
                    CallControlCircleButton(
                        label = "Camera",
                        icon = if (callState.isCameraOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                        isActive = callState.isCameraOn,
                        testTag = "call_camera_button",
                        onClick = onToggleCamera
                    )
                    CallControlCircleButton(
                        label = "More",
                        icon = Icons.Filled.MoreHoriz,
                        isActive = callState.isRecording,
                        testTag = "call_more_button",
                        onClick = onOpenConsentDialog
                    )
                }

                // Red End Call Circle Button
                Surface(
                    shape = CircleShape,
                    color = EndCallRed,
                    modifier = Modifier
                        .size(if (tokens.isCompact) 60.dp else 68.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEndCall)
                        .testTag("call_end_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "End Call",
                            tint = PureWhiteSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom "Encrypted Call" Pill
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = tokens.sp16, vertical = tokens.sp8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = PureWhiteSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Encrypted Call",
                            color = PureWhiteSurface,
                            fontSize = tokens.captionSize,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Add Participant to Group Call Modal (Up to 8 Members)
    if (showInviteParticipantDialog) {
        val contacts = (contactsState as? UiState.Success)?.data.orEmpty().filter { it.status != "blocked" }
        val directoryUsers = (directoryUsersState as? UiState.Success)?.data.orEmpty()
        var customParticipantName by remember { mutableStateOf("") }
        var customParticipantUid by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showInviteParticipantDialog = false },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Text(
                    text = "Add to Group Call (${callState.memberUids.size}/8)",
                    fontSize = tokens.headingSize,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(tokens.sp8)
                ) {
                    item {
                        Text(
                            text = "Select a contact or user to join this call:",
                            fontSize = tokens.captionSize,
                            color = tokens.textMuted
                        )
                    }

                    items(contacts, key = { it.id }) { c ->
                        val pUid = if (c.ownerUid == currentUserId) c.targetUid else c.ownerUid
                        val pName = if (c.ownerUid == currentUserId) c.contactName else c.requesterName.ifBlank { c.contactName }
                        val alreadyInCall = pUid in callState.memberUids
                        Surface(
                            shape = RoundedCornerShape(tokens.radiusCard),
                            color = tokens.surface2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(tokens.radiusCard))
                                .clickable(enabled = !alreadyInCall) {
                                    onInviteParticipant(pUid, pName)
                                    showInviteParticipantDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(tokens.sp12),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatarView(avatarKey = c.avatarKey, displayName = pName, size = 36.dp)
                                Spacer(modifier = Modifier.width(tokens.sp12))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pName, fontWeight = FontWeight.Bold, color = tokens.text)
                                    Text(
                                        text = if (alreadyInCall) "In call" else "Tap to invite",
                                        fontSize = tokens.captionSize,
                                        color = tokens.textMuted
                                    )
                                }
                                if (!alreadyInCall) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = tokens.primary)
                                }
                            }
                        }
                    }

                    items(directoryUsers, key = { it.uid }) { u ->
                        val alreadyInCall = u.uid in callState.memberUids
                        Surface(
                            shape = RoundedCornerShape(tokens.radiusCard),
                            color = tokens.surface2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(tokens.radiusCard))
                                .clickable(enabled = !alreadyInCall) {
                                    onInviteParticipant(u.uid, u.displayName)
                                    showInviteParticipantDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(tokens.sp12),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatarView(avatarKey = u.photoUrl, displayName = u.displayName, size = 36.dp)
                                Spacer(modifier = Modifier.width(tokens.sp12))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(u.displayName, fontWeight = FontWeight.Bold, color = tokens.text)
                                    Text(
                                        text = if (alreadyInCall) "In call" else "${u.userHandle} • Tap to invite",
                                        fontSize = tokens.captionSize,
                                        color = tokens.textMuted
                                    )
                                }
                                if (!alreadyInCall) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = tokens.primary)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = customParticipantName,
                            onValueChange = { customParticipantName = it },
                            label = { Text("Participant Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(tokens.sp8))
                        OutlinedTextField(
                            value = customParticipantUid,
                            onValueChange = { customParticipantUid = it },
                            label = { Text("Participant ID (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customParticipantName.isNotBlank()) {
                            onInviteParticipant(customParticipantUid, customParticipantName)
                        }
                        showInviteParticipantDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text("Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteParticipantDialog = false }) {
                    Text("Close", color = tokens.textMuted)
                }
            }
        )
    }

    // Call Options & Recording Modal
    if (callState.showConsentDialog) {
        AlertDialog(
            onDismissRequest = onDismissConsentDialog,
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Text(
                    text = "Call Options & Recording",
                    fontSize = tokens.headingSize,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp12)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Video Quality",
                            fontSize = tokens.bodySize,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text
                        )
                        TextButton(onClick = onToggleHdResolution) {
                            Text(
                                text = if (callState.isFullHd1080p) "Full HD 1080p ✓" else "HD 720p",
                                color = tokens.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = tokens.surface2,
                        shape = RoundedCornerShape(tokens.radiusSmall)
                    ) {
                        Text(
                            text = "• Caller Approval: ${if (callState.callerConsent) "Ready" else "Pending"}\n" +
                                "• Partner Approval: ${if (callState.calleeConsent) "Ready" else "Pending"}\n" +
                                "• Participants: ${callState.memberUids.size.coerceAtLeast(1)}/8",
                            fontSize = tokens.captionSize,
                            lineHeight = tokens.captionLineHeight,
                            color = tokens.text,
                            modifier = Modifier.padding(tokens.sp12)
                        )
                    }
                }
            },
            confirmButton = {
                if (callState.isRecording) {
                    Button(
                        onClick = onWithdrawConsentAndStop,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.danger),
                        shape = RoundedCornerShape(tokens.radiusPill)
                    ) {
                        Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(tokens.sp8))
                        Text("Stop Recording")
                    }
                } else {
                    Button(
                        onClick = onConfirmAllParticipantsConsent,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                        shape = RoundedCornerShape(tokens.radiusPill)
                    ) {
                        Text("Start Recording")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConsentDialog) {
                    Text("Close", color = tokens.textMuted)
                }
            }
        )
    }
}

@Composable
private fun CallControlCircleButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    val btnSize = if (tokens.isCompact) 50.dp else 56.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.sp4)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) Color.White.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .size(btnSize)
                .border(
                    width = 1.dp,
                    color = if (isActive) PureWhiteSurface else Color.White.copy(alpha = 0.25f),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = PureWhiteSurface,
                    modifier = Modifier.size(tokens.iconSize)
                )
            }
        }
        Text(
            text = label,
            color = PureWhiteSurface,
            fontSize = tokens.captionSize,
            lineHeight = tokens.captionLineHeight,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatCallDuration(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hrs, mins, secs)
}
