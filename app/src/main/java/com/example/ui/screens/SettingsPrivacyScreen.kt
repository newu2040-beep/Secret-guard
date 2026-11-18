package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.media.BuiltInRingtone
import com.example.core.notifications.SecretGuardNotificationHelper
import com.example.data.model.CallRecording
import com.example.data.model.TrustedContact
import com.example.data.model.UserPrivateSettings
import com.example.data.model.UserProfile
import com.example.ui.components.SecretGuardShieldLogo
import com.example.ui.theme.AppFontOption
import com.example.ui.theme.LocalSecretGuardTokens
import com.example.ui.theme.PastelThemeOption
import com.example.ui.viewmodel.UiState

/**
 * Screen 5 — Settings & Privacy (v5.0):
 * - Responsive single-column layout adhering to strict design tokens & Compact Mode (320px–430px).
 * - Profile Customization (Avatar, Name, @userHandle, Gender Female/Male only, Age, Visibility, Call Approval).
 * - Pastel Themes, Font Styles & Compact Display Mode switcher.
 * - Optional Mutual Call Approval ("Approve Call") switch and per-contact approval management.
 * - Custom & Built-in Ringtones with live audio preview.
 * - Permissions & Hardware Access Center (Camera, Mic, Push Notifications, Gallery, Files, Biometric).
 * - Secret Code & Native Android Biometric App Lock.
 * - Recording Settings, Privacy & Security, Storage Manager, About SecretGuard v5.0 ("Made with love by Rahul Shah"), and Logout.
 */
@Composable
fun SettingsPrivacyScreen(
    userProfileState: UiState<UserProfile?>,
    privateSettingsState: UiState<UserPrivateSettings?>,
    recordingsState: UiState<List<CallRecording>>,
    contactsState: UiState<List<TrustedContact>>,
    secretCodeDisplay: String,
    isPinRevealed: Boolean,
    playingMediaId: String?,
    selectedPastelTheme: PastelThemeOption,
    selectedAppFont: AppFontOption,
    isCompactModeEnabled: Boolean,
    selectedRingtoneId: String,
    customRingtoneLabel: String,
    onSelectPastelTheme: (PastelThemeOption) -> Unit,
    onSelectAppFont: (AppFontOption) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit,
    onSelectBuiltInRingtone: (BuiltInRingtone) -> Unit,
    onSelectCustomRingtoneUri: (Uri) -> Unit,
    onStopRingtonePreview: () -> Unit,
    onUpdateProfileCustomization: (
        displayName: String,
        userHandle: String,
        visibility: String,
        avatarUri: Uri?,
        gender: String?,
        age: Long?,
        requireCallApproval: Boolean?
    ) -> Unit,
    onSetRequireCallApproval: (Boolean) -> Unit,
    onToggleContactCallApproval: (String, Boolean) -> Unit,
    onBackClick: () -> Unit,
    onTogglePinVisibility: () -> Unit,
    onChangeSecretCode: (String, String) -> Boolean,
    onResetSecretCodeViaRecovery: (String) -> Boolean,
    onLockAppNow: () -> Unit,
    onUpdateSettings: (
        displayName: String?,
        profileVisibility: String?,
        appLockEnabled: Boolean?,
        autoRecordCalls: Boolean?,
        notificationAlert: Boolean?,
        recordingQuality: String?,
        retentionDays: Long?
    ) -> Unit,
    onToggleBlockContact: (String, Boolean) -> Unit,
    onTogglePlayRecording: (CallRecording) -> Unit,
    onDeleteRecording: (String) -> Unit,
    onSubmitReport: (String, String, String) -> Unit,
    onDeleteAccountAndData: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val tokens = LocalSecretGuardTokens.current

    val profile = (userProfileState as? UiState.Success)?.data
    val privateSettings = (privateSettingsState as? UiState.Success)?.data
    val autoRecordCalls = privateSettings?.autoRecordCalls ?: true
    val notificationAlert = privateSettings?.notificationAlert ?: true
    val appLockEnabled = privateSettings?.appLockEnabled ?: false
    val recordingQuality = privateSettings?.recordingQuality ?: "FHD 1080p"
    val retentionDays = privateSettings?.retentionDays ?: 30L
    val requireCallApproval = profile?.requireCallApproval ?: false

    var showSecretCodeDialog by remember { mutableStateOf(false) }
    var showProfileThemeDialog by remember { mutableStateOf(false) }
    var showRingtoneDialog by remember { mutableStateOf(false) }
    var showPermissionsCenterDialog by remember { mutableStateOf(false) }
    var showRecordingSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacySecurityDialog by remember { mutableStateOf(false) }
    var showStorageManagerDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onUpdateSettings(null, null, null, null, granted, null, null)
    }

    val customRingtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onSelectCustomRingtoneUri(uri)
        }
    }

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.sp12)
        ) {
            // 1. Top Header ("<   Settings")
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tokens.topBarHeight)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = tokens.text
                        )
                    }
                    Text(
                        text = "Settings",
                        fontSize = tokens.titleSize,
                        lineHeight = tokens.titleLineHeight,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 2. Centered Shield Emblem + "SecretGuard" + "v5.0"
            item {
                val demographicBadge = profile?.demographicSummary().orEmpty()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = tokens.sp4)
                ) {
                    SecretGuardShieldLogo(
                        size = if (tokens.isCompact) 52.dp else 64.dp,
                        showCheckmarkInsteadOfMic = true,
                        showOrbitalRings = false
                    )
                    Spacer(modifier = Modifier.height(tokens.sp8))
                    Text(
                        text = "SecretGuard",
                        fontSize = tokens.titleSize,
                        lineHeight = tokens.titleLineHeight,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Text(
                        text = if (demographicBadge.isNotBlank()) {
                            "v5.0 • $demographicBadge"
                        } else {
                            "v5.0"
                        },
                        fontSize = tokens.captionSize,
                        color = tokens.textMuted
                    )
                }
            }

            // 3. "Your Secret Code" Card
            item {
                Surface(
                    shape = RoundedCornerShape(tokens.radiusCard),
                    color = tokens.surface2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusCard))
                        .clip(RoundedCornerShape(tokens.radiusCard))
                        .clickable { showSecretCodeDialog = true }
                        .testTag("secret_code_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(tokens.sp16),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your Secret Code",
                            fontSize = tokens.headingSize,
                            lineHeight = tokens.headingLineHeight,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text
                        )
                        Spacer(modifier = Modifier.height(tokens.sp8))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.width(36.dp))
                            Text(
                                text = secretCodeDisplay,
                                fontSize = tokens.headingSize,
                                fontWeight = FontWeight.Bold,
                                color = tokens.text,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = onTogglePinVisibility,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("toggle_pin_visibility_button")
                            ) {
                                Icon(
                                    imageVector = if (isPinRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle Secret Code Visibility",
                                    tint = tokens.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(tokens.sp4))
                        Text(
                            text = "Keep your code private",
                            fontSize = tokens.captionSize,
                            color = tokens.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 4. Profile, Gender/Age, Pastel Themes & Fonts
            item {
                val demoInfo = profile?.demographicSummary().orEmpty().let {
                    if (it.isNotBlank()) " • $it" else ""
                }
                SettingsCardNavigationRow(
                    title = "Profile, Themes & Fonts",
                    subtitle = "${selectedPastelTheme.displayName} • ${selectedAppFont.displayName}$demoInfo",
                    icon = Icons.Filled.Palette,
                    testTag = "setting_pastel_themes",
                    onClick = { showProfileThemeDialog = true }
                )
            }

            // 5. Optional Mutual Call Approval ("Approve Call") Switch Row
            item {
                SettingsCardSwitchRow(
                    title = "Approve Call (Mutual Approval)",
                    subtitle = if (requireCallApproval) {
                        "Call only after both users approve"
                    } else {
                        "Optional — require approval before calling"
                    },
                    icon = Icons.Filled.Call,
                    checked = requireCallApproval,
                    testTag = "setting_approve_call_switch",
                    onCheckedChange = { enabled ->
                        onSetRequireCallApproval(enabled)
                    },
                    onCardClick = {
                        onSetRequireCallApproval(!requireCallApproval)
                    }
                )
            }

            // 6. Custom & Built-in Ringtones
            item {
                val activeRingtoneName = if (customRingtoneLabel.isNotBlank()) {
                    "Custom: $customRingtoneLabel"
                } else {
                    BuiltInRingtone.entries.firstOrNull { it.id == selectedRingtoneId }?.title ?: "Emerald Pulse"
                }
                SettingsCardNavigationRow(
                    title = "Call Ringtones",
                    subtitle = activeRingtoneName,
                    icon = Icons.Filled.MusicNote,
                    testTag = "setting_ringtones",
                    onClick = { showRingtoneDialog = true }
                )
            }

            // 7. All Permissions & Hardware Access Center
            item {
                SettingsCardNavigationRow(
                    title = "Permissions & Access",
                    subtitle = "Camera, Mic, Notifications, Gallery & Files",
                    icon = Icons.Filled.Security,
                    testTag = "setting_permissions_center",
                    onClick = { showPermissionsCenterDialog = true }
                )
            }

            // 8. Recording Settings Row ("Auto Record Calls")
            item {
                SettingsCardSwitchRow(
                    title = "Recording Settings",
                    subtitle = "Auto Record Calls ($recordingQuality)",
                    icon = Icons.Filled.SettingsSuggest,
                    checked = autoRecordCalls,
                    testTag = "setting_recording_switch",
                    onCheckedChange = { enabled ->
                        onUpdateSettings(null, null, null, enabled, null, null, null)
                    },
                    onCardClick = { showRecordingSettingsDialog = true }
                )
            }

            // 9. Notification Alert Row ("Get notified on activity")
            item {
                SettingsCardSwitchRow(
                    title = "Notification Alert",
                    subtitle = "Get notified on activity",
                    icon = Icons.Filled.NotificationsNone,
                    checked = notificationAlert,
                    testTag = "setting_notification_switch",
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onUpdateSettings(null, null, null, null, enabled, null, null)
                        }
                    },
                    onCardClick = {
                        onUpdateSettings(null, null, null, null, !notificationAlert, null, null)
                    }
                )
            }

            // 10. Privacy & Security Row
            item {
                SettingsCardNavigationRow(
                    title = "Privacy & Security",
                    subtitle = "Call Approvals & Contact Blocking",
                    icon = Icons.Filled.VerifiedUser,
                    testTag = "setting_privacy_security",
                    onClick = { showPrivacySecurityDialog = true }
                )
            }

            // 11. Storage Manager Row ("Manage Recordings")
            item {
                val recCount = (recordingsState as? UiState.Success)?.data?.size ?: 0
                SettingsCardNavigationRow(
                    title = "Storage Manager",
                    subtitle = "Manage Recordings ($recCount saved)",
                    icon = Icons.Filled.FolderOpen,
                    testTag = "setting_storage_manager",
                    onClick = { showStorageManagerDialog = true }
                )
            }

            // 12. About SecretGuard Row ("App Information")
            item {
                SettingsCardNavigationRow(
                    title = "About SecretGuard",
                    subtitle = "App Information • v5.0",
                    icon = Icons.Filled.Info,
                    testTag = "setting_about",
                    onClick = { showAboutDialog = true }
                )
            }

            // 13. Full-Width Pill Logout Button
            item {
                Spacer(modifier = Modifier.height(tokens.sp4))
                Surface(
                    shape = CircleShape,
                    color = tokens.surface2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tokens.buttonHeight)
                        .border(1.dp, tokens.border, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onLogoutClick)
                        .testTag("logout_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = tokens.primary,
                            modifier = Modifier.size(tokens.iconSize)
                        )
                        Spacer(modifier = Modifier.width(tokens.sp8))
                        Text(
                            text = "Logout",
                            color = tokens.text,
                            fontSize = tokens.bodySize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Required footer credit visible directly on Settings screen & in About modal
            item {
                Text(
                    text = "Made with love by Rahul Shah",
                    fontSize = tokens.captionSize,
                    color = tokens.textMuted,
                    modifier = Modifier
                        .padding(top = tokens.sp8, bottom = tokens.sp16)
                        .testTag("developer_credit_text")
                )
            }
        }
    }

    // --- DIALOG: Profile Customization, Gender/Age, Call Approval, Pastel Themes & Fonts ---
    if (showProfileThemeDialog) {
        ProfileAndThemeCustomizationDialog(
            currentDisplayName = profile?.displayName ?: "SecretGuard User",
            currentHandle = profile?.userHandle ?: "@user",
            currentAvatarKey = profile?.photoUrl?.ifBlank { "user" } ?: "user",
            currentVisibility = profile?.profileVisibility ?: "contacts",
            currentGender = profile?.gender.orEmpty(),
            currentAge = profile?.age ?: 0,
            currentRequireCallApproval = profile?.requireCallApproval ?: false,
            selectedTheme = selectedPastelTheme,
            selectedFont = selectedAppFont,
            isCompactMode = isCompactModeEnabled,
            onSelectTheme = onSelectPastelTheme,
            onSelectFont = onSelectAppFont,
            onToggleCompactMode = onToggleCompactMode,
            onSaveProfile = { name, handle, vis, avatarUri, gender, age, reqApproval ->
                onUpdateProfileCustomization(name, handle, vis, avatarUri, gender, age, reqApproval)
                showProfileThemeDialog = false
            },
            onDismiss = { showProfileThemeDialog = false }
        )
    }

    // --- DIALOG: Custom & Built-in Ringtones ---
    if (showRingtoneDialog) {
        AlertDialog(
            onDismissRequest = {
                onStopRingtonePreview()
                showRingtoneDialog = false
            },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = tokens.primary)
                    Spacer(modifier = Modifier.width(tokens.sp8))
                    Text("Call Ringtones", fontWeight = FontWeight.Bold, color = tokens.text)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(tokens.sp8)
                ) {
                    item {
                        Text(
                            text = "Tap a ringtone to preview or pick an audio file:",
                            fontSize = tokens.captionSize,
                            color = tokens.textMuted
                        )
                    }

                    items(BuiltInRingtone.entries) { ringtone ->
                        val isSelected = customRingtoneLabel.isBlank() && selectedRingtoneId == ringtone.id
                        Surface(
                            shape = RoundedCornerShape(tokens.radiusSmall),
                            color = if (isSelected) tokens.surface2 else tokens.bg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isSelected) tokens.primary else tokens.border,
                                    RoundedCornerShape(tokens.radiusSmall)
                                )
                                .clip(RoundedCornerShape(tokens.radiusSmall))
                                .clickable { onSelectBuiltInRingtone(ringtone) }
                        ) {
                            Row(
                                modifier = Modifier.padding(tokens.sp12),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ringtone.title,
                                        fontSize = tokens.bodySize,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.text
                                    )
                                    Text(
                                        text = ringtone.subtitle,
                                        fontSize = tokens.captionSize,
                                        color = tokens.textMuted
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = tokens.primary)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(tokens.sp4))
                        Button(
                            onClick = {
                                customRingtonePickerLauncher.launch(arrayOf("audio/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                            shape = RoundedCornerShape(tokens.radiusPill),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(tokens.sp8))
                            Text(
                                if (customRingtoneLabel.isNotBlank()) {
                                    "Custom: $customRingtoneLabel"
                                } else {
                                    "Pick Custom Ringtone"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStopRingtonePreview()
                        showRingtoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // --- DIALOG: All Permissions & Hardware Access Center ---
    if (showPermissionsCenterDialog) {
        var permissionsStatusNote by remember { mutableStateOf("") }
        val allPermsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val grantedCount = results.values.count { it }
            permissionsStatusNote = "Granted $grantedCount/${results.size} permissions."
        }
        val galleryTestLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                permissionsStatusNote = "Gallery media ready: ${uri.lastPathSegment}"
            }
        }
        val filesTestLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                permissionsStatusNote = "File access ready: ${uri.lastPathSegment}"
            }
        }

        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasNotif = SecretGuardNotificationHelper.hasNotificationPermission(context)

        AlertDialog(
            onDismissRequest = { showPermissionsCenterDialog = false },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = tokens.primary)
                    Spacer(modifier = Modifier.width(tokens.sp8))
                    Text("Permissions & Access", fontWeight = FontWeight.Bold, color = tokens.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                    Text(
                        text = "• Camera (1080p Video): ${if (hasCam) "Granted ✓" else "Tap Below"}\n" +
                            "• Microphone (Voice): ${if (hasMic) "Granted ✓" else "Tap Below"}\n" +
                            "• Notifications: ${if (hasNotif) "Granted ✓" else "Tap Below"}\n" +
                            "• Gallery & Files: Ready\n" +
                            "• Biometric Lock: Ready",
                        fontSize = tokens.captionSize,
                        lineHeight = tokens.captionLineHeight,
                        color = tokens.text
                    )

                    if (permissionsStatusNote.isNotBlank()) {
                        Surface(
                            color = tokens.surface2,
                            shape = RoundedCornerShape(tokens.radiusSmall)
                        ) {
                            Text(
                                text = permissionsStatusNote,
                                fontSize = tokens.captionSize,
                                fontWeight = FontWeight.Bold,
                                color = tokens.primary,
                                modifier = Modifier.padding(tokens.sp8)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val list = mutableListOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                list.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            allPermsLauncher.launch(list.toTypedArray())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                        shape = RoundedCornerShape(tokens.radiusPill),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Camera, Mic & Notifications")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
                    ) {
                        Button(
                            onClick = {
                                galleryTestLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.surface2, contentColor = tokens.text),
                            shape = RoundedCornerShape(tokens.radiusPill),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Gallery", fontSize = tokens.captionSize)
                        }
                        Button(
                            onClick = {
                                filesTestLauncher.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.surface2, contentColor = tokens.text),
                            shape = RoundedCornerShape(tokens.radiusPill),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Files", fontSize = tokens.captionSize)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPermissionsCenterDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // --- DIALOG 1: Secret Code PIN Management & Biometric App Lock ---
    if (showSecretCodeDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var recoveryMode by remember { mutableStateOf(false) }
        var biometricStatus by remember { mutableStateOf<String?>(null) }
        val activity = context as? Activity

        AlertDialog(
            onDismissRequest = { showSecretCodeDialog = false },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = tokens.primary)
                    Spacer(modifier = Modifier.width(tokens.sp8))
                    Text("Secret Code & App Lock", fontWeight = FontWeight.Bold, color = tokens.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Require App Lock on Launch", fontWeight = FontWeight.Bold, color = tokens.text)
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = {
                                onUpdateSettings(null, null, it, null, null, null, null)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = tokens.onPrimary, checkedTrackColor = tokens.primary)
                        )
                    }

                    Button(
                        onClick = {
                            if (activity != null) {
                                SecretGuardNotificationHelper.authenticateWithNativeBiometric(
                                    activity = activity,
                                    onAuthenticated = {
                                        biometricStatus = "Biometric verified ✓"
                                    },
                                    onMessage = { msg ->
                                        biometricStatus = msg
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.surface2, contentColor = tokens.text),
                        shape = RoundedCornerShape(tokens.radiusPill),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = tokens.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(tokens.sp8))
                        Text("Verify Face / Biometric Unlock")
                    }

                    if (!biometricStatus.isNullOrBlank()) {
                        Text(
                            text = biometricStatus!!,
                            fontSize = tokens.captionSize,
                            color = tokens.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!recoveryMode) {
                        OutlinedTextField(
                            value = currentPinInput,
                            onValueChange = { currentPinInput = it.filter { c -> c.isDigit() }.take(8) },
                            label = { Text("Current 6–8 Digit PIN (Default: 246810)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("New 6–8 Digit Secret Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { recoveryMode = !recoveryMode }) {
                            Text(
                                text = if (recoveryMode) "Use Current PIN" else "Reset PIN",
                                color = tokens.primary
                            )
                        }
                        TextButton(
                            onClick = {
                                showSecretCodeDialog = false
                                onLockAppNow()
                            }
                        ) {
                            Text("Lock App Now", color = tokens.text, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = if (recoveryMode) {
                            onResetSecretCodeViaRecovery(newPinInput)
                        } else {
                            onChangeSecretCode(currentPinInput, newPinInput)
                        }
                        if (ok) showSecretCodeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text(if (recoveryMode) "Save New PIN" else "Update Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecretCodeDialog = false }) {
                    Text("Cancel", color = tokens.textMuted)
                }
            }
        )
    }

    // --- DIALOG 2: Recording Settings (Quality, Retention) ---
    if (showRecordingSettingsDialog) {
        var selectedQuality by remember { mutableStateOf(recordingQuality) }
        var selectedRetention by remember { mutableLongStateOf(retentionDays) }

        AlertDialog(
            onDismissRequest = { showRecordingSettingsDialog = false },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Text("Recording Settings", fontWeight = FontWeight.Bold, color = tokens.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp12)) {
                    Text("Video & Audio Quality", fontWeight = FontWeight.Bold, color = tokens.text)
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                        listOf("FHD 1080p", "HD 720p", "Audio Only").forEach { q ->
                            FilterChip(
                                selected = selectedQuality == q,
                                onClick = { selectedQuality = q },
                                label = { Text(q, fontSize = 12.sp) }
                            )
                        }
                    }
                    Text("Auto-Cleanup Retention", fontWeight = FontWeight.Bold, color = tokens.text)
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                        listOf(7L, 30L, 90L).forEach { days ->
                            FilterChip(
                                selected = selectedRetention == days,
                                onClick = { selectedRetention = days },
                                label = { Text("$days Days", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSettings(null, null, null, null, null, selectedQuality, selectedRetention)
                        showRecordingSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordingSettingsDialog = false }) {
                    Text("Close", color = tokens.textMuted)
                }
            }
        )
    }

    // --- DIALOG 3: Privacy & Security (Call Approvals, Blocked Contacts, Report, Delete Account) ---
    if (showPrivacySecurityDialog) {
        var reportTarget by remember { mutableStateOf("") }
        var reportDetails by remember { mutableStateOf("") }
        val contacts = (contactsState as? UiState.Success)?.data.orEmpty()

        AlertDialog(
            onDismissRequest = { showPrivacySecurityDialog = false },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Text("Privacy & Security", fontWeight = FontWeight.Bold, color = tokens.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Require Call Approval", fontWeight = FontWeight.Bold, color = tokens.text)
                            Text(
                                "Only allow calls from approved contacts",
                                fontSize = tokens.captionSize,
                                color = tokens.textMuted
                            )
                        }
                        Switch(
                            checked = requireCallApproval,
                            onCheckedChange = { onSetRequireCallApproval(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = tokens.onPrimary,
                                checkedTrackColor = tokens.primary
                            )
                        )
                    }

                    if (contacts.isNotEmpty()) {
                        Text("Contacts: Call Approval & Blocking", fontWeight = FontWeight.Bold, fontSize = tokens.bodySize, color = tokens.text)
                        contacts.take(4).forEach { contact ->
                            val isBlocked = contact.status == "blocked"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = contact.contactName,
                                    fontSize = tokens.bodySize,
                                    color = tokens.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(tokens.sp4)) {
                                    TextButton(
                                        onClick = {
                                            onToggleContactCallApproval(contact.id, !contact.callApproved)
                                        }
                                    ) {
                                        Text(
                                            text = if (contact.callApproved) "Approved ✓" else "Approve Call",
                                            color = tokens.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = tokens.captionSize
                                        )
                                    }
                                    TextButton(onClick = { onToggleBlockContact(contact.id, isBlocked) }) {
                                        Text(
                                            text = if (isBlocked) "Unblock" else "Block",
                                            color = if (isBlocked) tokens.primary else tokens.danger,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = tokens.captionSize
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text("Submit Safety Report", fontWeight = FontWeight.Bold, fontSize = tokens.bodySize, color = tokens.text)
                    OutlinedTextField(
                        value = reportTarget,
                        onValueChange = { reportTarget = it },
                        label = { Text("User or Contact") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        label = { Text("Details") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                if (reportTarget.isNotBlank() || reportDetails.isNotBlank()) {
                                    onSubmitReport(
                                        reportTarget.ifBlank { "Reported Contact" },
                                        "unauthorized_recording",
                                        reportDetails.ifBlank { "Safety report submitted." }
                                    )
                                    showPrivacySecurityDialog = false
                                }
                            }
                        ) {
                            Text("Submit Report", color = tokens.primary, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = {
                                showPrivacySecurityDialog = false
                                onDeleteAccountAndData()
                            }
                        ) {
                            Text("Delete Account", color = tokens.danger, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacySecurityDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // --- DIALOG 4: Storage Manager (Usage, Saved Recordings, Playback & Permanent Delete) ---
    if (showStorageManagerDialog) {
        val recordings = (recordingsState as? UiState.Success)?.data.orEmpty()
        val totalBytes = recordings.sumOf { it.sizeBytes }
        val totalMb = String.format("%.2f MB", totalBytes / (1024f * 1024f))

        AlertDialog(
            onDismissRequest = { showStorageManagerDialog = false },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Text("Storage Manager", fontWeight = FontWeight.Bold, color = tokens.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                    Text(
                        text = "Storage Used: $totalMb of 100 MB",
                        fontSize = tokens.captionSize,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    LinearProgressIndicator(
                        progress = { (totalBytes / (100f * 1024f * 1024f)).coerceIn(0.04f, 1f) },
                        color = tokens.primary,
                        trackColor = tokens.surface2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )

                    if (recordings.isEmpty()) {
                        Text(
                            text = "No recordings saved.",
                            fontSize = tokens.bodySize,
                            color = tokens.textMuted,
                            modifier = Modifier.padding(vertical = tokens.sp12)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(210.dp),
                            verticalArrangement = Arrangement.spacedBy(tokens.sp8)
                        ) {
                            items(recordings, key = { it.id }) { rec ->
                                val isPlayingThis = playingMediaId == rec.id
                                Surface(
                                    shape = RoundedCornerShape(tokens.radiusCard),
                                    color = tokens.surface2,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(tokens.sp8),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onTogglePlayRecording(rec) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play Recording",
                                                tint = tokens.primary
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rec.title,
                                                fontSize = tokens.captionSize,
                                                fontWeight = FontWeight.Bold,
                                                color = tokens.text,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isPlayingThis) {
                                                    "Playing audio..."
                                                } else {
                                                    "${rec.durationSec}s"
                                                },
                                                fontSize = tokens.captionSize,
                                                color = tokens.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteRecording(rec.id) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Delete Recording Permanently",
                                                tint = tokens.danger
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStorageManagerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // --- DIALOG 5: About SecretGuard v5.0 ---
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Text("About SecretGuard v5.0", fontWeight = FontWeight.Bold, color = tokens.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                    Text(
                        text = "SecretGuard v5.0\nRecord • Call • Chat — Secretly & Securely",
                        fontSize = tokens.bodySize,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Surface(
                        color = tokens.surface2,
                        shape = RoundedCornerShape(tokens.radiusSmall),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Made with love by Rahul Shah",
                            fontSize = tokens.bodySize,
                            fontWeight = FontWeight.Bold,
                            color = tokens.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(tokens.sp12)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingsCardSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    onCardClick: () -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    Surface(
        shape = RoundedCornerShape(tokens.radiusCard),
        color = tokens.surface2,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusCard))
            .clip(RoundedCornerShape(tokens.radiusCard))
            .clickable(onClick = onCardClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.sp16, vertical = tokens.sp12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tokens.bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tokens.primary,
                    modifier = Modifier.size(tokens.iconSize)
                )
            }

            Spacer(modifier = Modifier.width(tokens.sp12))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = tokens.bodySize,
                    lineHeight = tokens.bodyLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = tokens.captionSize,
                    lineHeight = tokens.captionLineHeight,
                    color = tokens.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.onPrimary,
                    checkedTrackColor = tokens.primary,
                    uncheckedThumbColor = tokens.bg,
                    uncheckedTrackColor = tokens.textMuted.copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
private fun SettingsCardNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    Surface(
        shape = RoundedCornerShape(tokens.radiusCard),
        color = tokens.surface2,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusCard))
            .clip(RoundedCornerShape(tokens.radiusCard))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.sp16, vertical = tokens.sp12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tokens.bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tokens.primary,
                    modifier = Modifier.size(tokens.iconSize)
                )
            }

            Spacer(modifier = Modifier.width(tokens.sp12))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = tokens.bodySize,
                    lineHeight = tokens.bodyLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = tokens.captionSize,
                    lineHeight = tokens.captionLineHeight,
                    color = tokens.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title,
                tint = tokens.textMuted
            )
        }
    }
}
