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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.ContactAvatarView
import com.example.ui.theme.CallBackdropBottom
import com.example.ui.theme.CallBackdropMid
import com.example.ui.theme.CallBackdropTop
import com.example.ui.theme.DeepGreenBlackText
import com.example.ui.theme.EmeraldLightTint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EndCallRed
import com.example.ui.theme.OnlineGreenStatus
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.RecordingRedStatus
import com.example.ui.theme.SecondarySlateGreenText
import com.example.ui.viewmodel.ActiveCallUiState

/**
 * Screen 3 — Active Call matching the reference specification:
 * - Atmospheric mountain/forest gradient surface with optional local PiP preview.
 * - Glowing circular portrait avatar, participant identity ("Priya Sharma"), live timer ("00:12:34") + green dot.
 * - Visible red recording pill and timer; recording starts ONLY after every required participant has consented,
 *   and stops immediately if consent is withdrawn.
 * - Controls: Mute, Speaker, Camera, More (Consent Recording & Camera Switch), and Red End Call button.
 * - Bottom pill: Lock icon + "Encrypted Call".
 */
@Composable
fun ActiveCallScreen(
    callState: ActiveCallUiState,
    onBackClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenConsentDialog: () -> Unit,
    onDismissConsentDialog: () -> Unit,
    onConfirmAllParticipantsConsent: () -> Unit,
    onWithdrawConsentAndStop: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    var showLocalPipPreview by remember { mutableStateOf(false) }
    var permissionNotice by remember { mutableStateOf<String?>(null) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val micGranted = result[Manifest.permission.RECORD_AUDIO] ?: false
        val camGranted = result[Manifest.permission.CAMERA] ?: false
        permissionNotice = if (!micGranted && !camGranted) {
            "Camera & Microphone permissions were declined. Running in encrypted audio-safe mode."
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
        // Atmospheric blurred mountain-lake silhouette matching Screen 3 reference
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft horizon glow
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

            // Distant mountain ridge
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

            // Foreground pine slope
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Back arrow, Consent Recording Pill, and PiP Expand Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Persistent Consent-First Recording Pill (PDF Section 9)
                    if (callState.isRecording && callState.consentState == "all_consented") {
                        Surface(
                            shape = CircleShape,
                            color = RecordingRedStatus,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onWithdrawConsentAndStop() }
                                .testTag("active_recording_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onOpenConsentDialog() }
                                .testTag("request_recording_consent_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8A80),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Consent Record",
                                    color = PureWhiteSurface,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .size(44.dp)
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

                if (permissionNotice != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = permissionNotice!!,
                            color = PureWhiteSurface,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Center Dominant Participant Avatar + Name + Live Call Duration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ContactAvatarView(
                    avatarKey = callState.participantAvatarKey,
                    displayName = callState.participantName,
                    size = 168.dp,
                    showGlowRing = true
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = callState.participantName,
                    color = PureWhiteSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCallDuration(callState.elapsedSeconds),
                        color = PureWhiteSurface.copy(alpha = 0.90f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(OnlineGreenStatus)
                    )
                }

                if (showLocalPipPreview && callState.isCameraOn) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .clickable { onSwitchCamera() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = PureWhiteSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (callState.isFrontCamera) "Local PiP: Front Camera" else "Local PiP: Rear Camera",
                                color = PureWhiteSurface,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Bottom Call Controls + Red End Call Button + Encrypted Call Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 4 Circular Translucent Controls: Mute, Speaker, Camera, More
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
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEndCall)
                        .testTag("call_end_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "End Call",
                            tint = PureWhiteSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom "Encrypted Call" Pill
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                        .padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Consent-First Call Recording Modal (PDF Section 9)
    if (callState.showConsentDialog) {
        AlertDialog(
            onDismissRequest = onDismissConsentDialog,
            containerColor = PureWhiteSurface,
            title = {
                Text(
                    text = "Consent-First Call Recording",
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenBlackText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Every participant must explicitly consent before call recording begins:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenBlackText
                    )
                    Surface(
                        color = EmeraldLightTint,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "• Scope: Captures SecretGuard WebRTC audio/video session with ${callState.participantName}.\n" +
                                "• Indicator: Displays a persistent red recording pill to all participants.\n" +
                                "• Withdrawal: Recording stops immediately if any participant withdraws consent.\n" +
                                "• Storage: Protected with AES-256-GCM and auto-cleaned per retention policy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (callState.isRecording) {
                    Button(
                        onClick = onWithdrawConsentAndStop,
                        colors = ButtonDefaults.buttonColors(containerColor = RecordingRedStatus)
                    ) {
                        Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Withdraw Consent & Stop")
                    }
                } else {
                    Button(
                        onClick = onConfirmAllParticipantsConsent,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("All Participants Consent • Start")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConsentDialog) {
                    Text("Close", color = SecondarySlateGreenText)
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) Color.White.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .size(58.dp)
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
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = label,
            color = PureWhiteSurface.copy(alpha = 0.92f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatCallDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hrs, mins, secs)
}
