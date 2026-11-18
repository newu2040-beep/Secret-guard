package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.CallSession
import com.example.data.model.Conversation
import com.example.data.model.TrustedContact
import com.example.data.model.UserProfile
import com.example.ui.components.ContactAvatarView
import com.example.ui.components.OnlinePresenceRow
import com.example.ui.components.SecretGuardEmptyState
import com.example.ui.components.SecretGuardIconButton
import com.example.ui.theme.AppFontOption
import com.example.ui.theme.LocalSecretGuardTokens
import com.example.ui.theme.PastelThemeOption
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

private enum class ParticipantPickerAction {
    AUDIO_CALL,
    VIDEO_CALL,
    CHAT,
    ADD_CONTACT
}

/**
 * Screen 2 — Home Dashboard (v5.0):
 * - Clean, refined single-column layout adhering to strict design tokens & Compact Mode (320px–430px).
 * - Profile card showing Display Name, Gender (Female/Male), Age, @handle, and Call Approval status.
 * - Four action tiles: Secret Call, Secret Video Call, Record Audio, Secret Chat.
 * - Mutual Call Approval support: approve incoming call requests or require approval before calling.
 */
@Composable
fun HomeDashboardScreen(
    currentUserId: String,
    userProfileState: UiState<UserProfile?>,
    directoryUsersState: UiState<List<UserProfile>>,
    contactsState: UiState<List<TrustedContact>>,
    liveMicAmplitude: Float,
    selectedPastelTheme: PastelThemeOption,
    selectedAppFont: AppFontOption,
    isCompactMode: Boolean,
    onSelectPastelTheme: (PastelThemeOption) -> Unit,
    onSelectAppFont: (AppFontOption) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit,
    onUpdateProfileCustomization: (
        displayName: String,
        userHandle: String,
        visibility: String,
        avatarUri: Uri?,
        gender: String,
        age: Long,
        requireCallApproval: Boolean
    ) -> Unit,
    onToggleContactCallApproval: (contactId: String, approved: Boolean) -> Unit,
    onTogglePresence: () -> Unit,
    onStartAudioCall: (name: String, avatar: String, calleeUid: String, extraUids: List<String>) -> Unit,
    onStartVideoCall: (name: String, avatar: String, calleeUid: String, extraUids: List<String>) -> Unit,
    onStartHardwareVoiceRecording: () -> Boolean,
    onCancelHardwareVoiceRecording: () -> Unit,
    onStopAndSaveVoiceRecording: (String, Long) -> Unit,
    onOpenChat: (name: String, avatar: String, partnerUid: String) -> Unit,
    onAddTrustedContact: (name: String, handle: String, targetUid: String, avatarKey: String) -> Unit,
    onOpenChatsTab: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalSecretGuardTokens.current

    var showProfileAndThemeDialog by remember { mutableStateOf(false) }
    var showAudioRecorderDialog by remember { mutableStateOf(false) }
    var activePickerAction by remember { mutableStateOf<ParticipantPickerAction?>(null) }
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
    val displayName = profile?.displayName?.ifBlank { "SecretGuard User" } ?: "SecretGuard User"
    val userHandle = profile?.userHandle?.ifBlank { "@${currentUserId.take(6)}" } ?: "@${currentUserId.take(6)}"
    val userAvatarKey = profile?.photoUrl?.ifBlank { "user" } ?: "user"
    val userGender = profile?.gender.orEmpty()
    val userAge = profile?.age ?: 0L
    val requireCallApproval = profile?.requireCallApproval ?: false
    val isOnline = profile?.onlineStatus != "offline"
    val demographicText = profile?.demographicSummary().orEmpty()

    val directoryUsers = (directoryUsersState as? UiState.Success)?.data.orEmpty()
    val directoryByUid = remember(directoryUsers) { directoryUsers.associateBy { it.uid } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = tokens.sp16, vertical = tokens.sp12),
            verticalArrangement = Arrangement.spacedBy(tokens.sp16)
        ) {
            // 1. Top App Bar (56dp height, max 2 actions)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tokens.topBarHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SecretGuardIconButton(
                            icon = Icons.Filled.Menu,
                            contentDescription = "Open Settings",
                            testTag = "home_menu_button",
                            onClick = onOpenSettings
                        )
                        Spacer(modifier = Modifier.width(tokens.sp8))
                        Text(
                            text = "SecretGuard",
                            fontSize = tokens.titleSize,
                            lineHeight = tokens.titleLineHeight,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                        SecretGuardIconButton(
                            icon = Icons.Filled.Palette,
                            contentDescription = "Themes & Fonts",
                            tint = tokens.primary,
                            containerColor = tokens.surface2,
                            testTag = "theme_switcher_button",
                            onClick = { showProfileAndThemeDialog = true }
                        )
                        SecretGuardIconButton(
                            icon = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = tokens.primary,
                            containerColor = tokens.surface2,
                            testTag = "security_badge_button",
                            onClick = onOpenSettings
                        )
                    }
                }
            }

            if (micPermissionDeniedBanner) {
                item {
                    Surface(
                        color = tokens.surface2,
                        shape = RoundedCornerShape(tokens.radiusCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, tokens.danger, RoundedCornerShape(tokens.radiusCard))
                    ) {
                        Row(
                            modifier = Modifier.padding(tokens.sp12),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Microphone access is needed for voice recording.",
                                fontSize = tokens.captionSize,
                                lineHeight = tokens.captionLineHeight,
                                color = tokens.danger,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(
                                onClick = {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            ) {
                                Text("Allow", color = tokens.danger, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. User Profile Card with Avatar, Gender (Female/Male), Age & Live Online Toggle
            item {
                Surface(
                    shape = RoundedCornerShape(tokens.radiusCard),
                    color = tokens.surface2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusCard))
                        .clip(RoundedCornerShape(tokens.radiusCard))
                        .clickable { onTogglePresence() }
                        .testTag("profile_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(tokens.sp16),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactAvatarView(
                            avatarKey = userAvatarKey,
                            displayName = displayName,
                            size = if (tokens.isCompact) 48.dp else 56.dp
                        )

                        Spacer(modifier = Modifier.width(tokens.sp12))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                fontSize = tokens.headingSize,
                                lineHeight = tokens.headingLineHeight,
                                fontWeight = FontWeight.Bold,
                                color = tokens.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val handleAndDemo = if (demographicText.isNotBlank()) {
                                "$userHandle • $demographicText"
                            } else {
                                userHandle
                            }
                            Text(
                                text = handleAndDemo,
                                fontSize = tokens.captionSize,
                                lineHeight = tokens.captionLineHeight,
                                color = tokens.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(tokens.sp4))
                            OnlinePresenceRow(
                                isOnline = isOnline,
                                label = buildString {
                                    append(if (isOnline) "Online" else "Offline")
                                    if (requireCallApproval) append(" • Call Approval On")
                                }
                            )
                        }

                        SecretGuardIconButton(
                            icon = Icons.Filled.Edit,
                            contentDescription = "Edit Profile, Gender, Age & Theme",
                            tint = tokens.primary,
                            containerColor = tokens.bg,
                            onClick = { showProfileAndThemeDialog = true }
                        )
                    }
                }
            }

            // 3. 2x2 Action Tiles: Secret Call, Secret Video Call, Record Audio, Secret Chat
            item {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp12)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(tokens.sp12)
                    ) {
                        DashboardActionTile(
                            title = "Secret Call",
                            subtitle = "Audio & Group",
                            icon = Icons.Filled.Call,
                            testTag = "tile_secret_call",
                            onClick = { activePickerAction = ParticipantPickerAction.AUDIO_CALL },
                            modifier = Modifier.weight(1f)
                        )
                        DashboardActionTile(
                            title = "Secret Video Call",
                            subtitle = "1080p HD",
                            icon = Icons.Filled.Videocam,
                            testTag = "tile_secret_video_call",
                            onClick = { activePickerAction = ParticipantPickerAction.VIDEO_CALL },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(tokens.sp12)
                    ) {
                        DashboardActionTile(
                            title = "Record Audio",
                            subtitle = "Voice Note",
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
                            subtitle = "Private Media",
                            icon = Icons.Filled.ChatBubble,
                            testTag = "tile_secret_chat",
                            onClick = { activePickerAction = ParticipantPickerAction.CHAT },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Recent Activity & Contacts Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        fontSize = tokens.headingSize,
                        lineHeight = tokens.headingLineHeight,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(tokens.radiusPill),
                            color = tokens.surface2,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusPill))
                                .clip(RoundedCornerShape(tokens.radiusPill))
                                .clickable { activePickerAction = ParticipantPickerAction.ADD_CONTACT }
                                .testTag("add_contact_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = tokens.sp12, vertical = tokens.sp8),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Contact",
                                    tint = tokens.primary,
                                    modifier = Modifier.size(tokens.chipIconSize)
                                )
                                Spacer(modifier = Modifier.width(tokens.sp4))
                                Text(
                                    text = "Add",
                                    fontSize = tokens.captionSize,
                                    lineHeight = tokens.captionLineHeight,
                                    color = tokens.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(tokens.radiusSmall),
                            color = tokens.surface2,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusSmall))
                                .clip(RoundedCornerShape(tokens.radiusSmall))
                                .clickable { onOpenChatsTab() }
                                .testTag("see_all_activity_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = tokens.sp8, vertical = tokens.sp8),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "See All",
                                    fontSize = tokens.captionSize,
                                    lineHeight = tokens.captionLineHeight,
                                    color = tokens.textMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "See All",
                                    tint = tokens.textMuted,
                                    modifier = Modifier.size(tokens.chipIconSize)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Real-Time Contacts & Call Approval List (max 6 visible items before See All)
            when (contactsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = tokens.sp24),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = tokens.primary)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        SecretGuardEmptyState(
                            icon = Icons.Filled.Shield,
                            title = "Unable to load contacts",
                            subtitle = contactsState.message
                        )
                    }
                }

                is UiState.Success -> {
                    val contacts = contactsState.data.filter { it.status != "blocked" }.take(6)
                    if (contacts.isEmpty()) {
                        item {
                            SecretGuardEmptyState(
                                icon = Icons.Filled.Shield,
                                title = "No contacts yet",
                                subtitle = if (directoryUsers.isNotEmpty()) {
                                    "${directoryUsers.size} active user(s) available in directory."
                                } else {
                                    "Tap below to connect with a contact."
                                },
                                actionLabel = "Add Contact",
                                onActionClick = { activePickerAction = ParticipantPickerAction.ADD_CONTACT }
                            )
                        }
                    } else {
                        items(contacts, key = { it.id }) { contact ->
                            val partnerUid = if (contact.ownerUid == currentUserId) contact.targetUid else contact.ownerUid
                            val partnerName = if (contact.ownerUid == currentUserId) {
                                contact.contactName
                            } else {
                                contact.requesterName.ifBlank { contact.contactName }
                            }
                            val liveUser = directoryByUid[partnerUid]
                            val liveOnline = liveUser?.onlineStatus == "online" ||
                                (partnerUid == currentUserId && isOnline)
                            val effectiveAvatar = liveUser?.photoUrl?.ifBlank { contact.avatarKey } ?: contact.avatarKey
                            val demoBadge = liveUser?.demographicSummary().orEmpty()

                            RecentActivityItemCard(
                                contactName = partnerName,
                                demographicBadge = demoBadge,
                                avatarKey = effectiveAvatar,
                                isOnline = liveOnline,
                                lastActivityText = contact.lastActivityText,
                                lastActivityType = contact.lastActivityType,
                                callApproved = contact.callApproved,
                                onApproveCallClick = {
                                    onToggleContactCallApproval(contact.id, !contact.callApproved)
                                },
                                onPrimaryClick = {
                                    onOpenChat(partnerName, effectiveAvatar, partnerUid)
                                },
                                onAudioCallClick = {
                                    onStartAudioCall(partnerName, effectiveAvatar, partnerUid, emptyList())
                                },
                                onVideoCallClick = {
                                    onStartVideoCall(partnerName, effectiveAvatar, partnerUid, emptyList())
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(tokens.sp16))
            }
        }
    }

    // Profile, Gender (Female/Male), Age, Call Approval, Pastel Themes & Fonts Dialog
    if (showProfileAndThemeDialog) {
        ProfileAndThemeCustomizationDialog(
            currentDisplayName = displayName,
            currentHandle = userHandle,
            currentAvatarKey = userAvatarKey,
            currentVisibility = profile?.profileVisibility ?: "contacts",
            currentGender = userGender,
            currentAge = userAge,
            currentRequireCallApproval = requireCallApproval,
            selectedTheme = selectedPastelTheme,
            selectedFont = selectedAppFont,
            isCompactMode = isCompactMode,
            onSelectTheme = onSelectPastelTheme,
            onSelectFont = onSelectAppFont,
            onToggleCompactMode = onToggleCompactMode,
            onSaveProfile = { newName, newHandle, newVis, avatarUri, newGender, newAge, newCallApproval ->
                onUpdateProfileCustomization(
                    newName,
                    newHandle,
                    newVis,
                    avatarUri,
                    newGender,
                    newAge,
                    newCallApproval
                )
                showProfileAndThemeDialog = false
            },
            onDismiss = { showProfileAndThemeDialog = false }
        )
    }

    // Clean Voice Recorder Dialog (no technical disclaimer text)
    if (showAudioRecorderDialog) {
        ConsentAudioRecorderDialog(
            liveMicAmplitude = liveMicAmplitude,
            onStartHardwareRecording = onStartHardwareVoiceRecording,
            onDismiss = {
                onCancelHardwareVoiceRecording()
                showAudioRecorderDialog = false
            },
            onSaveRecording = { title, durationSec ->
                showAudioRecorderDialog = false
                onStopAndSaveVoiceRecording(title, durationSec)
            }
        )
    }

    // Participant Picker Modal
    val pickerAction = activePickerAction
    if (pickerAction != null) {
        val contacts = (contactsState as? UiState.Success)?.data.orEmpty().filter { it.status != "blocked" }
        RealTimeParticipantPickerDialog(
            currentUserId = currentUserId,
            action = pickerAction,
            contacts = contacts,
            directoryUsers = directoryUsers,
            onToggleContactCallApproval = onToggleContactCallApproval,
            onDismiss = { activePickerAction = null },
            onSelectParticipants = { primaryName, primaryHandle, primaryUid, avatarKey, extraUids ->
                activePickerAction = null
                when (pickerAction) {
                    ParticipantPickerAction.AUDIO_CALL -> {
                        onStartAudioCall(primaryName, avatarKey, primaryUid, extraUids)
                    }
                    ParticipantPickerAction.VIDEO_CALL -> {
                        onStartVideoCall(primaryName, avatarKey, primaryUid, extraUids)
                    }
                    ParticipantPickerAction.CHAT -> {
                        onOpenChat(primaryName, avatarKey, primaryUid)
                    }
                    ParticipantPickerAction.ADD_CONTACT -> {
                        onAddTrustedContact(primaryName, primaryHandle, primaryUid, avatarKey)
                    }
                }
            }
        )
    }
}

@Composable
fun ProfileAndThemeCustomizationDialog(
    currentDisplayName: String,
    currentHandle: String,
    currentAvatarKey: String,
    currentVisibility: String,
    currentGender: String,
    currentAge: Long,
    currentRequireCallApproval: Boolean,
    selectedTheme: PastelThemeOption,
    selectedFont: AppFontOption,
    isCompactMode: Boolean,
    onSelectTheme: (PastelThemeOption) -> Unit,
    onSelectFont: (AppFontOption) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit,
    onSaveProfile: (
        name: String,
        handle: String,
        visibility: String,
        avatarUri: Uri?,
        gender: String,
        age: Long,
        requireCallApproval: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    var nameInput by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
    var handleInput by remember(currentHandle) { mutableStateOf(currentHandle) }
    var visibilityInput by remember(currentVisibility) { mutableStateOf(currentVisibility) }
    var genderInput by remember(currentGender) {
        mutableStateOf(if (currentGender in listOf("Female", "Male")) currentGender else "Female")
    }
    var ageInput by remember(currentAge) {
        mutableStateOf(if (currentAge > 0L) currentAge.toString() else "24")
    }
    var requireCallApprovalInput by remember(currentRequireCallApproval) {
        mutableStateOf(currentRequireCallApproval)
    }
    var pickedAvatarUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedAvatarUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(tokens.radiusCard),
        containerColor = tokens.bg,
        title = {
            Text(
                text = "Profile, Themes & Fonts",
                fontSize = tokens.headingSize,
                lineHeight = tokens.headingLineHeight,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(tokens.sp12)
            ) {
                // Avatar Photo + Upload
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(tokens.sp12)
                    ) {
                        ContactAvatarView(
                            avatarKey = currentAvatarKey,
                            displayName = nameInput,
                            size = 52.dp
                        )
                        Button(
                            onClick = {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                            shape = RoundedCornerShape(tokens.radiusPill)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(tokens.sp8))
                            Text(if (pickedAvatarUri != null) "Photo Ready ✓" else "Change Photo")
                        }
                    }
                }

                // Display Name & Handle
                item {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(80) },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = handleInput,
                        onValueChange = { handleInput = it.take(60) },
                        label = { Text("@userHandle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Gender (Female and Male only) & Age
                item {
                    Text(
                        text = "Gender & Age",
                        fontSize = tokens.captionSize,
                        lineHeight = tokens.captionLineHeight,
                        fontWeight = FontWeight.Bold,
                        color = tokens.primary
                    )
                    Spacer(modifier = Modifier.height(tokens.sp4))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
                    ) {
                        listOf("Female", "Male").forEach { option ->
                            FilterChip(
                                selected = genderInput == option,
                                onClick = { genderInput = option },
                                label = {
                                    Text(
                                        text = option,
                                        fontSize = tokens.captionSize,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(tokens.sp4))
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Age") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Optional Approve Call Before Calling toggle
                item {
                    Surface(
                        shape = RoundedCornerShape(tokens.radiusSmall),
                        color = tokens.surface2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusSmall))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(tokens.sp12),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Approve Call First (Optional)",
                                    fontSize = tokens.bodySize,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.text
                                )
                                Text(
                                    text = "Only allow calls when mutually approved",
                                    fontSize = tokens.captionSize,
                                    color = tokens.textMuted
                                )
                            }
                            Switch(
                                checked = requireCallApprovalInput,
                                onCheckedChange = { requireCallApprovalInput = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = tokens.onPrimary,
                                    checkedTrackColor = tokens.primary
                                )
                            )
                        }
                    }
                }

                // Pastel Themes
                item {
                    Text(
                        text = "Pastel Color Themes",
                        fontSize = tokens.captionSize,
                        lineHeight = tokens.captionLineHeight,
                        fontWeight = FontWeight.Bold,
                        color = tokens.primary
                    )
                    Spacer(modifier = Modifier.height(tokens.sp4))
                    Column(verticalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                        PastelThemeOption.entries.forEach { themeOption ->
                            val isSelected = themeOption == selectedTheme
                            Surface(
                                shape = RoundedCornerShape(tokens.radiusSmall),
                                color = if (isSelected) tokens.surface2 else tokens.bg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) tokens.primary else tokens.border,
                                        shape = RoundedCornerShape(tokens.radiusSmall)
                                    )
                                    .clip(RoundedCornerShape(tokens.radiusSmall))
                                    .clickable { onSelectTheme(themeOption) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(tokens.sp12),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(themeOption.primary)
                                        )
                                        Spacer(modifier = Modifier.width(tokens.sp12))
                                        Column {
                                            Text(
                                                text = themeOption.displayName,
                                                fontSize = tokens.bodySize,
                                                fontWeight = FontWeight.Bold,
                                                color = tokens.text
                                            )
                                            Text(
                                                text = themeOption.subtitle,
                                                fontSize = tokens.captionSize,
                                                color = tokens.textMuted
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = tokens.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Typography / Font Styles
                item {
                    Text(
                        text = "Font Style",
                        fontSize = tokens.captionSize,
                        lineHeight = tokens.captionLineHeight,
                        fontWeight = FontWeight.Bold,
                        color = tokens.primary
                    )
                    Spacer(modifier = Modifier.height(tokens.sp4))
                    Column(verticalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                        AppFontOption.entries.forEach { fontOption ->
                            val isSelected = fontOption == selectedFont
                            Surface(
                                shape = RoundedCornerShape(tokens.radiusSmall),
                                color = if (isSelected) tokens.surface2 else tokens.bg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) tokens.primary else tokens.border,
                                        shape = RoundedCornerShape(tokens.radiusSmall)
                                    )
                                    .clip(RoundedCornerShape(tokens.radiusSmall))
                                    .clickable { onSelectFont(fontOption) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(tokens.sp12),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fontOption.displayName,
                                            fontSize = tokens.bodySize,
                                            fontWeight = FontWeight.Bold,
                                            color = tokens.text
                                        )
                                        Text(
                                            text = fontOption.subtitle,
                                            fontSize = tokens.captionSize,
                                            color = tokens.textMuted
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected Font",
                                            tint = tokens.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Compact Display Mode
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Compact Display Mode",
                                fontSize = tokens.bodySize,
                                fontWeight = FontWeight.Bold,
                                color = tokens.text
                            )
                            Text(
                                text = "Resizes layout for small phones (320px–360px)",
                                fontSize = tokens.captionSize,
                                color = tokens.textMuted
                            )
                        }
                        Switch(
                            checked = isCompactMode,
                            onCheckedChange = onToggleCompactMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = tokens.onPrimary,
                                checkedTrackColor = tokens.primary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAge = ageInput.toLongOrNull()?.coerceIn(0L, 120L) ?: 0L
                    onSaveProfile(
                        nameInput,
                        handleInput,
                        visibilityInput,
                        pickedAvatarUri,
                        genderInput,
                        parsedAge,
                        requireCallApprovalInput
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                shape = RoundedCornerShape(tokens.radiusPill)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = tokens.textMuted)
            }
        }
    )
}

@Composable
private fun RealTimeParticipantPickerDialog(
    currentUserId: String,
    action: ParticipantPickerAction,
    contacts: List<TrustedContact>,
    directoryUsers: List<UserProfile>,
    onToggleContactCallApproval: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSelectParticipants: (name: String, handle: String, targetUid: String, avatarKey: String, extraUids: List<String>) -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    var manualName by remember { mutableStateOf("") }
    var manualHandle by remember { mutableStateOf("") }

    val isCallAction = action == ParticipantPickerAction.AUDIO_CALL || action == ParticipantPickerAction.VIDEO_CALL
    val selectedGroupUids = remember { mutableStateListOf<String>() }
    val selectedGroupNames = remember { mutableStateListOf<String>() }

    val dialogTitle = when (action) {
        ParticipantPickerAction.AUDIO_CALL -> "Start Audio or Group Call"
        ParticipantPickerAction.VIDEO_CALL -> "Start 1080p HD Video Call"
        ParticipantPickerAction.CHAT -> "Open Private Chat"
        ParticipantPickerAction.ADD_CONTACT -> "Discover & Add Contact"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(tokens.radiusCard),
        containerColor = tokens.bg,
        title = {
            Text(
                text = dialogTitle,
                fontSize = tokens.headingSize,
                lineHeight = tokens.headingLineHeight,
                fontWeight = FontWeight.Bold,
                color = tokens.text
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(tokens.sp8)
            ) {
                if (contacts.isNotEmpty() && action != ParticipantPickerAction.ADD_CONTACT) {
                    item {
                        Text(
                            text = "Connected Contacts",
                            fontSize = tokens.captionSize,
                            fontWeight = FontWeight.Bold,
                            color = tokens.primary
                        )
                    }
                    items(contacts, key = { it.id }) { c ->
                        val partnerUid = if (c.ownerUid == currentUserId) c.targetUid else c.ownerUid
                        val partnerName = if (c.ownerUid == currentUserId) {
                            c.contactName
                        } else {
                            c.requesterName.ifBlank { c.contactName }
                        }
                        val partnerHandle = if (c.ownerUid == currentUserId) {
                            c.contactHandle
                        } else {
                            c.requesterHandle.ifBlank { c.contactHandle }
                        }
                        val isChecked = partnerUid in selectedGroupUids

                        Surface(
                            shape = RoundedCornerShape(tokens.radiusCard),
                            color = tokens.surface2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(tokens.radiusCard))
                                .clickable {
                                    onSelectParticipants(partnerName, partnerHandle, partnerUid, c.avatarKey, emptyList())
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(tokens.sp12),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatarView(
                                    avatarKey = c.avatarKey,
                                    displayName = partnerName,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(tokens.sp12))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = partnerName,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (c.callApproved) "$partnerHandle • Call Approved ✓" else "$partnerHandle • Tap Approve to Call",
                                        fontSize = tokens.captionSize,
                                        color = if (c.callApproved) tokens.primary else tokens.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!c.callApproved) {
                                    TextButton(
                                        onClick = { onToggleContactCallApproval(c.id, true) }
                                    ) {
                                        Text("Approve", color = tokens.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (isCallAction && partnerUid.isNotBlank()) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (selectedGroupUids.size < 7) {
                                                    selectedGroupUids.add(partnerUid)
                                                    selectedGroupNames.add(partnerName)
                                                }
                                            } else {
                                                selectedGroupUids.remove(partnerUid)
                                                selectedGroupNames.remove(partnerName)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = tokens.primary)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = tokens.primary
                                    )
                                }
                            }
                        }
                    }
                }

                if (directoryUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Directory Users (${directoryUsers.size})",
                            fontSize = tokens.captionSize,
                            fontWeight = FontWeight.Bold,
                            color = tokens.primary
                        )
                    }
                    items(directoryUsers, key = { it.uid }) { user ->
                        val avatar = user.photoUrl.ifBlank { "custom" }
                        val isChecked = user.uid in selectedGroupUids
                        val demo = user.demographicSummary()
                        Surface(
                            shape = RoundedCornerShape(tokens.radiusCard),
                            color = tokens.surface2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(tokens.radiusCard))
                                .clickable {
                                    onSelectParticipants(
                                        user.displayName.ifBlank { "SecretGuard User" },
                                        user.userHandle,
                                        user.uid,
                                        avatar,
                                        emptyList()
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(tokens.sp12),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatarView(
                                    avatarKey = avatar,
                                    displayName = user.displayName,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(tokens.sp12))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (demo.isNotBlank()) "${user.displayName} ($demo)" else user.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    OnlinePresenceRow(
                                        isOnline = user.onlineStatus == "online",
                                        label = buildString {
                                            append(user.userHandle)
                                            if (user.requireCallApproval) append(" • Approval Required")
                                        }
                                    )
                                }
                                if (isCallAction) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (selectedGroupUids.size < 7) {
                                                    selectedGroupUids.add(user.uid)
                                                    selectedGroupNames.add(user.displayName)
                                                }
                                            } else {
                                                selectedGroupUids.remove(user.uid)
                                                selectedGroupNames.remove(user.displayName)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = tokens.primary)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Select",
                                        tint = tokens.primary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Enter Name or @Handle",
                        fontSize = tokens.captionSize,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textMuted
                    )
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(tokens.sp8))
                    OutlinedTextField(
                        value = manualHandle,
                        onValueChange = { manualHandle = it },
                        label = { Text("@handle (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedGroupUids.isNotEmpty()) {
                        val primaryUid = selectedGroupUids.first()
                        val extraUids = selectedGroupUids.drop(1)
                        val combinedNames = selectedGroupNames.joinToString(", ").take(80)
                        val avatar = if (selectedGroupUids.size > 1) "group" else "custom"
                        onSelectParticipants(combinedNames, "@group", primaryUid, avatar, extraUids)
                        return@Button
                    }
                    val trimmedName = manualName.trim()
                    val trimmedHandle = manualHandle.trim()
                    if (trimmedName.isNotEmpty() || trimmedHandle.isNotEmpty()) {
                        val effectiveName = trimmedName.ifBlank { trimmedHandle.removePrefix("@") }
                        val effectiveHandle = trimmedHandle.ifBlank {
                            "@${effectiveName.lowercase().replace(" ", "_")}"
                        }
                        onSelectParticipants(effectiveName, effectiveHandle, "", "custom", emptyList())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                shape = RoundedCornerShape(tokens.radiusPill)
            ) {
                Text(
                    if (selectedGroupUids.size > 1) {
                        "Start Group Call (${selectedGroupUids.size + 1})"
                    } else {
                        when (action) {
                            ParticipantPickerAction.AUDIO_CALL -> "Call Now"
                            ParticipantPickerAction.VIDEO_CALL -> "Video Call"
                            ParticipantPickerAction.CHAT -> "Open Chat"
                            ParticipantPickerAction.ADD_CONTACT -> "Add Contact"
                        }
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = tokens.textMuted)
            }
        }
    )
}

@Composable
private fun DashboardActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSecretGuardTokens.current
    val tileHeight = if (tokens.isCompact) 96.dp else 108.dp
    Surface(
        shape = RoundedCornerShape(tokens.radiusCard),
        color = tokens.surface2,
        modifier = modifier
            .height(tileHeight)
            .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusCard))
            .clip(RoundedCornerShape(tokens.radiusCard))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(tokens.sp12),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tokens.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tokens.primary,
                    modifier = Modifier.size(tokens.iconSize)
                )
            }
            Spacer(modifier = Modifier.height(tokens.sp4))
            Text(
                text = title,
                fontSize = tokens.bodySize,
                lineHeight = tokens.bodyLineHeight,
                fontWeight = FontWeight.Bold,
                color = tokens.text,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = tokens.captionSize,
                lineHeight = tokens.captionLineHeight,
                color = tokens.textMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentActivityItemCard(
    contactName: String,
    demographicBadge: String,
    avatarKey: String,
    isOnline: Boolean,
    lastActivityText: String,
    lastActivityType: String,
    callApproved: Boolean,
    onApproveCallClick: () -> Unit,
    onPrimaryClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    val subIcon = when (lastActivityType) {
        "video_call" -> Icons.Filled.Videocam
        "audio_call" -> Icons.Filled.Call
        "audio_recording" -> Icons.Filled.Mic
        else -> Icons.Filled.ChatBubble
    }

    Surface(
        shape = RoundedCornerShape(tokens.radiusCard),
        color = tokens.surface2,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusCard))
            .clip(RoundedCornerShape(tokens.radiusCard))
            .clickable(onClick = onPrimaryClick)
            .testTag("activity_item_${contactName.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.sp12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatarView(
                avatarKey = avatarKey,
                displayName = contactName,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(tokens.sp12))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
                ) {
                    Text(
                        text = if (demographicBadge.isNotBlank()) "$contactName • $demographicBadge" else contactName,
                        fontSize = tokens.bodySize,
                        lineHeight = tokens.bodyLineHeight,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .size(tokens.sp8)
                            .clip(CircleShape)
                            .background(
                                if (isOnline) tokens.success else tokens.textMuted.copy(alpha = 0.4f)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(tokens.sp4))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = subIcon,
                        contentDescription = null,
                        tint = tokens.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(tokens.sp4))
                    Text(
                        text = lastActivityText,
                        fontSize = tokens.captionSize,
                        lineHeight = tokens.captionLineHeight,
                        color = tokens.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!callApproved) {
                Surface(
                    shape = RoundedCornerShape(tokens.radiusPill),
                    color = tokens.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(tokens.radiusPill))
                        .clickable { onApproveCallClick() }
                        .padding(horizontal = tokens.sp8, vertical = tokens.sp4)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Approve Call",
                            tint = tokens.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(tokens.sp4))
                        Text(
                            text = "Approve",
                            color = tokens.onPrimary,
                            fontSize = tokens.captionSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(tokens.sp4))
            }

            SecretGuardIconButton(
                icon = Icons.Filled.Call,
                contentDescription = "Audio Call $contactName",
                tint = tokens.primary,
                onClick = onAudioCallClick
            )

            SecretGuardIconButton(
                icon = Icons.Filled.Videocam,
                contentDescription = "Video Call $contactName",
                tint = tokens.primary,
                onClick = onVideoCallClick
            )
        }
    }
}

@Composable
private fun ConsentAudioRecorderDialog(
    liveMicAmplitude: Float,
    onStartHardwareRecording: () -> Boolean,
    onDismiss: () -> Unit,
    onSaveRecording: (String, Long) -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableLongStateOf(0L) }
    var recordingTitle by remember { mutableStateOf("Voice Note") }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000L)
            elapsedSec += 1L
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(tokens.radiusCard),
        containerColor = tokens.bg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = if (isRecording) tokens.danger else tokens.primary
                )
                Spacer(modifier = Modifier.width(tokens.sp8))
                Text(
                    text = if (isRecording) {
                        "Recording (${String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)})"
                    } else {
                        "Record Voice Note"
                    },
                    fontSize = tokens.headingSize,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.sp12)) {
                OutlinedTextField(
                    value = recordingTitle,
                    onValueChange = { recordingTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isRecording) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        val barCount = 24
                        val spacing = size.width / barCount
                        val amp = liveMicAmplitude.coerceIn(0.15f, 1.0f)
                        for (i in 0 until barCount) {
                            val phase = (((i * 3 + elapsedSec.toInt() * 2) % 7) + 1) / 7f
                            val barH = (size.height * (0.18f + 0.80f * amp * phase)).coerceIn(6f, size.height)
                            drawRoundRect(
                                color = tokens.danger,
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
            if (!isRecording) {
                Button(
                    onClick = {
                        onStartHardwareRecording()
                        isRecording = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text("Start Recording")
                }
            } else {
                Button(
                    onClick = {
                        isRecording = false
                        onSaveRecording(recordingTitle, elapsedSec.coerceAtLeast(1L))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.danger),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(tokens.sp8))
                    Text("Save (${elapsedSec.coerceAtLeast(1L)}s)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = tokens.textMuted)
            }
        }
    )
}

/**
 * Calls & Chats Directory Tab Views for the bottom navigation bar backed by real-time multi-user Firestore streams.
 * Supports Mutual Call Approval and joining live group/1:1 calls directly.
 */
@Composable
fun CallsHistoryTabScreen(
    currentUserId: String,
    callsState: UiState<List<CallSession>>,
    contactsState: UiState<List<TrustedContact>>,
    directoryUsersState: UiState<List<UserProfile>>,
    onToggleContactCallApproval: (String, Boolean) -> Unit,
    onStartAudioCall: (String, String, String, List<String>) -> Unit,
    onStartVideoCall: (String, String, String, List<String>) -> Unit,
    onJoinActiveCall: (CallSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSecretGuardTokens.current
    var showNewCallPicker by remember { mutableStateOf(false) }

    val contacts = (contactsState as? UiState.Success)?.data.orEmpty().filter { it.status != "blocked" }
    val pendingApprovals = contacts.filter { !it.callApproved }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg)
            .widthIn(max = 600.dp)
            .padding(horizontal = tokens.sp16, vertical = tokens.sp12)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Calls",
                    fontSize = tokens.titleSize,
                    lineHeight = tokens.titleLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
                Text(
                    text = "1:1, Group & Approved Calls",
                    fontSize = tokens.captionSize,
                    color = tokens.textMuted
                )
            }
            Button(
                onClick = { showNewCallPicker = true },
                shape = RoundedCornerShape(tokens.radiusPill),
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primary)
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(tokens.sp8))
                Text("New Call")
            }
        }

        if (pendingApprovals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(tokens.sp12))
            Text(
                text = "Pending Call Approvals",
                fontSize = tokens.captionSize,
                fontWeight = FontWeight.Bold,
                color = tokens.primary
            )
            Spacer(modifier = Modifier.height(tokens.sp8))
            pendingApprovals.take(3).forEach { pending ->
                val pName = pending.peerDisplayName(currentUserId)
                Surface(
                    shape = RoundedCornerShape(tokens.radiusCard),
                    color = tokens.surface2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = tokens.sp8)
                        .border(1.dp, tokens.primary, RoundedCornerShape(tokens.radiusCard))
                ) {
                    Row(
                        modifier = Modifier.padding(tokens.sp12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pName, fontWeight = FontWeight.Bold, color = tokens.text)
                            Text(
                                text = "Approve to allow direct calling",
                                fontSize = tokens.captionSize,
                                color = tokens.textMuted
                            )
                        }
                        Button(
                            onClick = { onToggleContactCallApproval(pending.id, true) },
                            shape = RoundedCornerShape(tokens.radiusPill),
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.primary)
                        ) {
                            Text("Approve Call")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(tokens.sp16))

        when (callsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = tokens.primary)
                }
            }
            is UiState.Error -> {
                SecretGuardEmptyState(
                    icon = Icons.Filled.Call,
                    title = "Failed to load calls",
                    subtitle = callsState.message
                )
            }
            is UiState.Success -> {
                val calls = callsState.data
                if (calls.isEmpty()) {
                    SecretGuardEmptyState(
                        icon = Icons.Filled.Videocam,
                        title = "No call history yet",
                        subtitle = "Tap 'New Call' to start a 1:1 or group audio/video call.",
                        actionLabel = "Start Call",
                        onActionClick = { showNewCallPicker = true }
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.sp12)) {
                        items(calls, key = { it.id }) { call ->
                            val isOutgoing = call.ownerUid == currentUserId
                            val remoteUid = if (isOutgoing) call.calleeUid else call.ownerUid
                            val remoteName = if (isOutgoing) {
                                call.participantName
                            } else {
                                call.callerName.ifBlank { call.participantName }
                            }
                            val isLiveJoinable = call.status in listOf("ringing", "connected")
                            val isGroup = call.memberUids.size > 2

                            Surface(
                                shape = RoundedCornerShape(tokens.radiusCard),
                                color = tokens.surface2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (isLiveJoinable) tokens.primary else tokens.border,
                                        shape = RoundedCornerShape(tokens.radiusCard)
                                    )
                                    .clip(RoundedCornerShape(tokens.radiusCard))
                                    .clickable {
                                        if (isLiveJoinable) {
                                            onJoinActiveCall(call)
                                        } else if (call.callType == "video") {
                                            onStartVideoCall(remoteName, call.participantAvatarKey, remoteUid, emptyList())
                                        } else {
                                            onStartAudioCall(remoteName, call.participantAvatarKey, remoteUid, emptyList())
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(tokens.sp12),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContactAvatarView(
                                        avatarKey = if (isGroup) "group" else call.participantAvatarKey,
                                        displayName = remoteName,
                                        size = 48.dp
                                    )
                                    Spacer(modifier = Modifier.width(tokens.sp12))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isGroup) "$remoteName (${call.memberUids.size})" else remoteName,
                                            fontSize = tokens.bodySize,
                                            fontWeight = FontWeight.Bold,
                                            color = tokens.text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val directionLabel = if (isOutgoing) "Outgoing" else "Incoming"
                                        Text(
                                            text = "$directionLabel ${call.callType.uppercase()} • ${call.status.uppercase()} • ${call.durationSec / 60}m ${call.durationSec % 60}s",
                                            fontSize = tokens.captionSize,
                                            color = if (isLiveJoinable) tokens.primary else tokens.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isLiveJoinable) {
                                        Button(
                                            onClick = { onJoinActiveCall(call) },
                                            shape = RoundedCornerShape(tokens.radiusPill),
                                            colors = ButtonDefaults.buttonColors(containerColor = tokens.primary)
                                        ) {
                                            Text("Join Live")
                                        }
                                    } else {
                                        SecretGuardIconButton(
                                            icon = if (call.callType == "video") Icons.Filled.Videocam else Icons.Filled.Call,
                                            contentDescription = "Call Again",
                                            tint = tokens.primary,
                                            onClick = {
                                                if (call.callType == "video") {
                                                    onStartVideoCall(remoteName, call.participantAvatarKey, remoteUid, emptyList())
                                                } else {
                                                    onStartAudioCall(remoteName, call.participantAvatarKey, remoteUid, emptyList())
                                                }
                                            }
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

    if (showNewCallPicker) {
        val dirUsers = (directoryUsersState as? UiState.Success)?.data.orEmpty()
        RealTimeParticipantPickerDialog(
            currentUserId = currentUserId,
            action = ParticipantPickerAction.VIDEO_CALL,
            contacts = contacts,
            directoryUsers = dirUsers,
            onToggleContactCallApproval = onToggleContactCallApproval,
            onDismiss = { showNewCallPicker = false },
            onSelectParticipants = { name, _, targetUid, avatarKey, extraUids ->
                showNewCallPicker = false
                onStartVideoCall(name, avatarKey, targetUid, extraUids)
            }
        )
    }
}

@Composable
fun ChatsListTabScreen(
    currentUserId: String,
    conversationsState: UiState<List<Conversation>>,
    directoryUsersState: UiState<List<UserProfile>>,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSecretGuardTokens.current
    val directoryUsers = (directoryUsersState as? UiState.Success)?.data.orEmpty()
    val directoryByUid = remember(directoryUsers) { directoryUsers.associateBy { it.uid } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg)
            .widthIn(max = 600.dp)
            .padding(horizontal = tokens.sp16, vertical = tokens.sp12)
    ) {
        Text(
            text = "Chats",
            fontSize = tokens.titleSize,
            lineHeight = tokens.titleLineHeight,
            fontWeight = FontWeight.Bold,
            color = tokens.text
        )
        Text(
            text = "Private Messages, Voice Notes & HD Media",
            fontSize = tokens.captionSize,
            color = tokens.textMuted
        )

        Spacer(modifier = Modifier.height(tokens.sp16))

        when (conversationsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = tokens.primary)
                }
            }
            is UiState.Error -> {
                SecretGuardEmptyState(
                    icon = Icons.Filled.ChatBubble,
                    title = "Failed to load chats",
                    subtitle = conversationsState.message
                )
            }
            is UiState.Success -> {
                val conversations = conversationsState.data
                if (conversations.isEmpty()) {
                    SecretGuardEmptyState(
                        icon = Icons.Filled.ChatBubble,
                        title = "No conversations yet",
                        subtitle = "Select a contact from the Home screen to start chatting."
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(tokens.sp12)) {
                        items(conversations, key = { it.id }) { conv ->
                            val partnerUid = conv.memberUids.firstOrNull { it != currentUserId } ?: conv.partnerUid
                            val partnerName = if (conv.ownerUid == currentUserId) {
                                conv.participantName
                            } else {
                                conv.ownerName.ifBlank { conv.participantName }
                            }
                            val liveUser = directoryByUid[partnerUid]
                            val isOnline = liveUser?.onlineStatus == "online" || conv.presenceState == "online"
                            val effectiveAvatar = liveUser?.photoUrl?.ifBlank { conv.participantAvatarKey } ?: conv.participantAvatarKey
                            val demoBadge = liveUser?.demographicSummary().orEmpty()

                            Surface(
                                shape = RoundedCornerShape(tokens.radiusCard),
                                color = tokens.surface2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusCard))
                                    .clip(RoundedCornerShape(tokens.radiusCard))
                                    .clickable { onOpenConversation(conv.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(tokens.sp12),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContactAvatarView(
                                        avatarKey = effectiveAvatar,
                                        displayName = partnerName,
                                        size = 48.dp
                                    )
                                    Spacer(modifier = Modifier.width(tokens.sp12))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
                                        ) {
                                            Text(
                                                text = if (demoBadge.isNotBlank()) "$partnerName • $demoBadge" else partnerName,
                                                fontSize = tokens.bodySize,
                                                fontWeight = FontWeight.Bold,
                                                color = tokens.text,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(tokens.sp8)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isOnline) tokens.success else tokens.textMuted.copy(alpha = 0.4f)
                                                    )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(tokens.sp4))
                                        Text(
                                            text = conv.lastMessageText,
                                            fontSize = tokens.captionSize,
                                            color = tokens.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open Chat",
                                        tint = tokens.primary
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
