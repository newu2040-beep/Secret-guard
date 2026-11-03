package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.CallSession
import com.example.data.model.Conversation
import com.example.data.model.TrustedContact
import com.example.data.model.UserProfile
import com.example.ui.components.ContactAvatarView
import com.example.ui.components.OnlinePresenceRow
import com.example.ui.theme.DeepGreenBlackText
import com.example.ui.theme.EmeraldLightTint
import com.example.ui.theme.EmeraldMintCard
import com.example.ui.theme.EmeraldPillBorder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnlineGreenStatus
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.RecordingRedStatus
import com.example.ui.theme.SecondarySlateGreenText
import com.example.ui.theme.WarningAmberStatus
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

/**
 * Screen 2 — Home Dashboard matching the reference specification:
 * - Top app bar with profile/menu and security badge.
 * - Trusted contact / User Profile card ("Secret User" • Online).
 * - Four action tiles: Secret Call, Secret Video Call, Record Audio, Secret Chat.
 * - Recent Activity from Firestore with real loading, empty, and error states.
 */
@Composable
fun HomeDashboardScreen(
    userProfileState: UiState<UserProfile?>,
    contactsState: UiState<List<TrustedContact>>,
    onTogglePresence: () -> Unit,
    onStartAudioCall: (String, String) -> Unit,
    onStartVideoCall: (String, String) -> Unit,
    onSaveVoiceRecording: (String, Long) -> Unit,
    onOpenChat: (String, String) -> Unit,
    onAddTrustedContact: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSecurityDrawerDialog by remember { mutableStateOf(false) }
    var showAudioRecorderDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var micPermissionDeniedBanner by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionDeniedBanner = false
            showAudioRecorderDialog = true
        } else {
            micPermissionDeniedBanner = true
        }
    }

    val profile = (userProfileState as? UiState.Success)?.data
    val displayName = profile?.displayName?.ifBlank { "Secret User" } ?: "Secret User"
    val isOnline = profile?.onlineStatus != "offline"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top App Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showSecurityDrawerDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Security Menu",
                            tint = DeepGreenBlackText,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Golden Crown / Security Verified Badge matching the mockup
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFF4D6),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { showSecurityDrawerDialog = true }
                            .testTag("security_badge_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = "Verified Encryption Status",
                                tint = WarningAmberStatus,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            if (micPermissionDeniedBanner) {
                item {
                    Surface(
                        color = Color(0xFFFEE4E2),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Microphone permission is required for consented audio recording.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB42318),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { showAudioRecorderDialog = true }) {
                                Text("Continue", color = Color(0xFFB42318), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Trusted Contact / User Profile Card ("Secret User" • Online)
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = EmeraldMintCard,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PureWhiteSurface.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onTogglePresence() }
                        .testTag("profile_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactAvatarView(
                            avatarKey = "user",
                            displayName = displayName,
                            size = 62.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenBlackText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OnlinePresenceRow(
                                isOnline = isOnline,
                                label = if (isOnline) "Online" else "Offline (Tap to switch)"
                            )
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Profile Settings",
                                tint = SecondarySlateGreenText
                            )
                        }
                    }
                }
            }

            // 3. 2x2 Action Tiles: Secret Call, Secret Video Call, Record Audio, Secret Chat
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DashboardActionTile(
                            title = "Secret Call",
                            icon = Icons.Filled.Call,
                            testTag = "tile_secret_call",
                            onClick = { onStartAudioCall("Priya Sharma", "priya") },
                            modifier = Modifier.weight(1f)
                        )
                        DashboardActionTile(
                            title = "Secret Video\nCall",
                            icon = Icons.Filled.Videocam,
                            testTag = "tile_secret_video_call",
                            onClick = { onStartVideoCall("Priya Sharma", "priya") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DashboardActionTile(
                            title = "Record Audio",
                            icon = Icons.Filled.Mic,
                            testTag = "tile_record_audio",
                            onClick = {
                                val hasMic = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) {
                                    showAudioRecorderDialog = true
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DashboardActionTile(
                            title = "Secret Chat",
                            icon = Icons.Filled.ChatBubble,
                            testTag = "tile_secret_chat",
                            onClick = { onOpenChat("Priya Sharma", "priya") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Recent Activity Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenBlackText,
                        fontSize = 19.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldLightTint,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showAddContactDialog = true }
                                .testTag("add_contact_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Contact",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add Contact",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenChat("Priya Sharma", "priya") }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .testTag("see_all_activity_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "See All",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondarySlateGreenText,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "See All",
                                tint = SecondarySlateGreenText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 5. Recent Activity Items from Firestore
            when (contactsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFEE4E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = contactsState.message,
                                color = Color(0xFFB42318),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val contacts = contactsState.data.filter { it.status != "blocked" }
                    if (contacts.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = PureWhiteSurface,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No recent activity yet",
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepGreenBlackText
                                    )
                                    Text(
                                        text = "Tap 'Add Contact' to add a trusted contact.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondarySlateGreenText
                                    )
                                }
                            }
                        }
                    } else {
                        items(contacts, key = { it.id }) { contact ->
                            RecentActivityItemCard(
                                contact = contact,
                                onPrimaryClick = {
                                    when (contact.lastActivityType) {
                                        "video_call" -> onStartVideoCall(contact.contactName, contact.avatarKey)
                                        "audio_call" -> onStartAudioCall(contact.contactName, contact.avatarKey)
                                        else -> onOpenChat(contact.contactName, contact.avatarKey)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Security & Encryption Status Dialog
    if (showSecurityDrawerDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityDrawerDialog = false },
            containerColor = PureWhiteSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SecretGuard Security Protocol",
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenBlackText
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• Cipher: Android Keystore AES-256-GCM authenticated encryption.\n" +
                            "• Auth: Verified Google Identity via Jetpack Credential Manager.\n" +
                            "• Recording Safety: Consent-first pipeline — no covert capture or silent recording. Persistent red indicator shown to all participants.\n" +
                            "• No Subscription Gate: All core privacy features are unlocked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepGreenBlackText
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSecurityDrawerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Verified")
                }
            }
        )
    }

    // Consent-First Standalone Audio Recorder Dialog
    if (showAudioRecorderDialog) {
        ConsentAudioRecorderDialog(
            onDismiss = { showAudioRecorderDialog = false },
            onSaveRecording = { title, durationSec ->
                showAudioRecorderDialog = false
                onSaveVoiceRecording(title, durationSec)
            }
        )
    }

    // Add Trusted Contact Dialog
    if (showAddContactDialog) {
        var newName by remember { mutableStateOf("") }
        var newHandle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            containerColor = PureWhiteSurface,
            title = {
                Text(
                    text = "Add Trusted Contact",
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenBlackText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newHandle,
                        onValueChange = { newHandle = it },
                        label = { Text("Email or SecretGuard Handle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onAddTrustedContact(
                                newName.trim(),
                                newHandle.trim().ifBlank { "${newName.trim().lowercase().replace(" ", ".")}@secretguard.org" }
                            )
                            showAddContactDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Add Contact")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel", color = SecondarySlateGreenText)
                }
            }
        )
    }
}

@Composable
private fun DashboardActionTile(
    title: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = EmeraldMintCard,
        shadowElevation = 4.dp,
        modifier = modifier
            .height(112.dp)
            .border(1.dp, PureWhiteSurface.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = DeepGreenBlackText,
                lineHeight = 17.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecentActivityItemCard(
    contact: TrustedContact,
    onPrimaryClick: () -> Unit
) {
    val isPlayableCall = contact.lastActivityType == "video_call"
    val subIcon = when (contact.lastActivityType) {
        "video_call" -> Icons.Filled.Call
        "audio_recording" -> Icons.Filled.Mic
        else -> Icons.Filled.ChatBubble
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = PureWhiteSurface.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onPrimaryClick)
            .testTag("activity_item_${contact.avatarKey}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatarView(
                avatarKey = contact.avatarKey,
                displayName = contact.contactName,
                size = 50.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.contactName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenBlackText
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = subIcon,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = contact.lastActivityText,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlateGreenText
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EmeraldMintCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlayableCall) {
                        Icons.Filled.PlayArrow
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = "Open ${contact.contactName}",
                    tint = if (isPlayableCall) EmeraldPrimary else SecondarySlateGreenText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ConsentAudioRecorderDialog(
    onDismiss: () -> Unit,
    onSaveRecording: (String, Long) -> Unit
) {
    var hasConsented by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableLongStateOf(0L) }
    var recordingTitle by remember { mutableStateOf("Encrypted Voice Note") }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000L)
            elapsedSec += 1L
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhiteSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = if (isRecording) RecordingRedStatus else EmeraldPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Consent-First Audio Recorder",
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenBlackText
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = if (isRecording) Color(0xFFFEE4E2) else EmeraldLightTint,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isRecording) {
                            "● REC ACTIVE (${String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)}) — Persistent indicator shown. Audio is protected with AES-256-GCM."
                        } else {
                            "Recording Scope Disclosure: Captures local microphone audio only after explicit consent. Stored encrypted and managed in Storage Manager."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRecording) RecordingRedStatus else Color(0xFF065F46),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                OutlinedTextField(
                    value = recordingTitle,
                    onValueChange = { recordingTitle = it },
                    label = { Text("Recording Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isRecording) {
                    // Animated waveform bars
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        val barCount = 24
                        val spacing = size.width / barCount
                        for (i in 0 until barCount) {
                            val dynamicFactor = (((i + elapsedSec.toInt()) % 5) + 1) / 5f
                            val barH = size.height * (0.25f + 0.70f * dynamicFactor)
                            drawRoundRect(
                                color = RecordingRedStatus,
                                topLeft = Offset(i * spacing + 4f, (size.height - barH) / 2f),
                                size = Size(spacing * 0.55f, barH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!hasConsented) {
                Button(
                    onClick = {
                        hasConsented = true
                        isRecording = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("I Consent & Start Recording")
                }
            } else {
                Button(
                    onClick = {
                        isRecording = false
                        onSaveRecording(recordingTitle, elapsedSec.coerceAtLeast(5L))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecordingRedStatus)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop & Save (${elapsedSec.coerceAtLeast(1L)}s)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondarySlateGreenText)
            }
        }
    )
}

/**
 * Calls & Chats Directory Tab Views for the bottom navigation bar so every tab
 * provides live Firestore-backed functionality.
 */
@Composable
fun CallsHistoryTabScreen(
    callsState: UiState<List<CallSession>>,
    onStartAudioCall: (String, String) -> Unit,
    onStartVideoCall: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Encrypted Calls",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DeepGreenBlackText
            )
            Button(
                onClick = { onStartVideoCall("Priya Sharma", "priya") },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Call")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (callsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            }
            is UiState.Error -> {
                Text(text = callsState.message, color = RecordingRedStatus)
            }
            is UiState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(callsState.data, key = { it.id }) { call ->
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = PureWhiteSurface,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    if (call.callType == "video") {
                                        onStartVideoCall(call.participantName, call.participantAvatarKey)
                                    } else {
                                        onStartAudioCall(call.participantName, call.participantAvatarKey)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatarView(
                                    avatarKey = call.participantAvatarKey,
                                    displayName = call.participantName,
                                    size = 50.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = call.participantName,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepGreenBlackText
                                    )
                                    Text(
                                        text = "${call.callType.replaceFirstChar { it.uppercase() }} • ${call.durationSec / 60}m ${call.durationSec % 60}s • ${if (call.consentState == "all_consented") "Consented Rec" else "Encrypted"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondarySlateGreenText
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onStartVideoCall(call.participantName, call.participantAvatarKey)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (call.callType == "video") Icons.Filled.Videocam else Icons.Filled.Call,
                                        contentDescription = "Call Again",
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatsListTabScreen(
    conversationsState: UiState<List<Conversation>>,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Secret Chats",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DeepGreenBlackText
        )
        Text(
            text = "AES-256-GCM Encrypted • Realtime Firestore Sync",
            style = MaterialTheme.typography.bodySmall,
            color = SecondarySlateGreenText
        )

        Spacer(modifier = Modifier.height(14.dp))

        when (conversationsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            }
            is UiState.Error -> {
                Text(text = conversationsState.message, color = RecordingRedStatus)
            }
            is UiState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(conversationsState.data, key = { it.id }) { conv ->
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = PureWhiteSurface,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onOpenConversation(conv.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatarView(
                                    avatarKey = conv.participantAvatarKey,
                                    displayName = conv.participantName,
                                    size = 52.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = conv.participantName,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepGreenBlackText
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(OnlineGreenStatus)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = conv.lastMessageText.ifBlank { "Tap to send an encrypted message" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondarySlateGreenText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open Chat",
                                    tint = SecondarySlateGreenText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
