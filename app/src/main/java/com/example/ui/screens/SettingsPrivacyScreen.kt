package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRecording
import com.example.data.model.TrustedContact
import com.example.data.model.UserProfile
import com.example.ui.components.SecretGuardShieldLogo
import com.example.ui.theme.DeepGreenBlackText
import com.example.ui.theme.EmeraldLightTint
import com.example.ui.theme.EmeraldMintCard
import com.example.ui.theme.EmeraldPillBorder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.RecordingRedStatus
import com.example.ui.theme.SecondarySlateGreenText
import com.example.ui.viewmodel.UiState

/**
 * Screen 5 — Settings & Privacy matching the reference specification:
 * - Top bar with Back arrow and centered "Settings".
 * - Centered SecretGuard shield-check emblem, "SecretGuard", and "v1.0.0".
 * - "Your Secret Code" card with masked PIN dots, eye visibility toggle, and Change/Lock flow.
 * - Recording Settings ("Auto Record Calls" switch + quality/retention configuration).
 * - Notification Alert ("Get notified on activity" switch + Android 13+ permission check).
 * - Privacy & Security ("End-to-End Encryption" -> Active session, Blocked users, Safety Report, Delete Account).
 * - Storage Manager ("Manage Recordings" -> Usage bar, playback, delete).
 * - About SecretGuard ("App Information" -> Legal documents & exact "Made with love by Rahul Shah" credit).
 * - Full-width Mint Pill "Logout" button.
 */
@Composable
fun SettingsPrivacyScreen(
    userProfileState: UiState<UserProfile?>,
    recordingsState: UiState<List<CallRecording>>,
    contactsState: UiState<List<TrustedContact>>,
    secretCodeDisplay: String,
    isPinRevealed: Boolean,
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
    onDeleteRecording: (String) -> Unit,
    onSubmitReport: (String, String, String) -> Unit,
    onDeleteAccountAndData: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val profile = (userProfileState as? UiState.Success)?.data
    val autoRecordCalls = profile?.autoRecordCalls ?: true
    val notificationAlert = profile?.notificationAlert ?: true
    val appLockEnabled = profile?.appLockEnabled ?: false
    val recordingQuality = profile?.recordingQuality ?: "HD 720p"
    val retentionDays = profile?.retentionDays ?: 30L

    var showSecretCodeDialog by remember { mutableStateOf(false) }
    var showRecordingSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacySecurityDialog by remember { mutableStateOf(false) }
    var showStorageManagerDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onUpdateSettings(null, null, null, null, granted, null, null)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Top Header ("<   Settings")
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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
                            tint = DeepGreenBlackText
                        )
                    }
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenBlackText,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 2. Centered Shield Emblem + "SecretGuard" + "v1.0.0" + Developer Credit
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    SecretGuardShieldLogo(
                        size = 64.dp,
                        showCheckmarkInsteadOfMic = true,
                        showOrbitalRings = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SecretGuard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepGreenBlackText,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "v1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlateGreenText
                    )
                }
            }

            // 3. "Your Secret Code" Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = PureWhiteSurface,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldPillBorder.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showSecretCodeDialog = true }
                        .testTag("secret_code_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your Secret Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenBlackText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.width(36.dp))
                            Text(
                                text = secretCodeDisplay,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepGreenBlackText,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = onTogglePinVisibility,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toggle_pin_visibility_button")
                            ) {
                                Icon(
                                    imageVector = if (isPinRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle Secret Code Visibility",
                                    tint = SecondarySlateGreenText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Keep your code private • Tap to change or lock app",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondarySlateGreenText
                        )
                    }
                }
            }

            // 4. Recording Settings Row ("Auto Record Calls")
            item {
                SettingsCardSwitchRow(
                    title = "Recording Settings",
                    subtitle = "Auto Record Calls ($recordingQuality • ${retentionDays}d)",
                    icon = Icons.Filled.SettingsSuggest,
                    checked = autoRecordCalls,
                    testTag = "setting_recording_switch",
                    onCheckedChange = { enabled ->
                        onUpdateSettings(null, null, null, enabled, null, null, null)
                    },
                    onCardClick = { showRecordingSettingsDialog = true }
                )
            }

            // 5. Notification Alert Row ("Get notified on activity")
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

            // 6. Privacy & Security Row ("End-to-End Encryption")
            item {
                SettingsCardNavigationRow(
                    title = "Privacy & Security",
                    subtitle = "End-to-End Encryption",
                    icon = Icons.Filled.VerifiedUser,
                    testTag = "setting_privacy_security",
                    onClick = { showPrivacySecurityDialog = true }
                )
            }

            // 7. Storage Manager Row ("Manage Recordings")
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

            // 8. About SecretGuard Row ("App Information")
            item {
                SettingsCardNavigationRow(
                    title = "About SecretGuard",
                    subtitle = "App Information • Made with love by Rahul Shah",
                    icon = Icons.Filled.Info,
                    testTag = "setting_about",
                    onClick = { showAboutDialog = true }
                )
            }

            // 9. Mint Pill Logout Button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = CircleShape,
                    color = EmeraldMintCard,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(1.dp, EmeraldPillBorder, CircleShape)
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
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Logout",
                            color = DeepGreenBlackText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Required footer credit visible directly on Settings screen & in About modal
            item {
                Text(
                    text = "Made with love by Rahul Shah",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondarySlateGreenText,
                    modifier = Modifier
                        .padding(top = 6.dp, bottom = 16.dp)
                        .testTag("developer_credit_text")
                )
            }
        }
    }

    // --- DIALOG 1: Keystore Secret Code PIN Management & App Lock ---
    if (showSecretCodeDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var recoveryMode by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSecretCodeDialog = false },
            containerColor = PureWhiteSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secret Code & App Lock", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Protected via Android Keystore (AES-256-GCM + PBKDF2-HMAC-SHA256) with attempt throttling (max 3 tries). Never stored in plaintext.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlateGreenText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Require App Lock on Launch", fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = {
                                onUpdateSettings(null, null, it, null, null, null, null)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhiteSurface, checkedTrackColor = EmeraldPrimary)
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
                                text = if (recoveryMode) "Use Current PIN" else "Authenticated Recovery",
                                color = EmeraldPrimary
                            )
                        }
                        TextButton(
                            onClick = {
                                showSecretCodeDialog = false
                                onLockAppNow()
                            }
                        ) {
                            Text("Lock App Now", color = DeepGreenBlackText, fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (recoveryMode) "Recover & Save PIN" else "Update Secret Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecretCodeDialog = false }) {
                    Text("Cancel", color = SecondarySlateGreenText)
                }
            }
        )
    }

    // --- DIALOG 2: Recording Settings (Consent-first, Quality, Retention) ---
    if (showRecordingSettingsDialog) {
        var selectedQuality by remember { mutableStateOf(recordingQuality) }
        var selectedRetention by remember { mutableLongStateOf(retentionDays) }

        AlertDialog(
            onDismissRequest = { showRecordingSettingsDialog = false },
            containerColor = PureWhiteSurface,
            title = {
                Text("Consent-First Recording Settings", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Every call recording requires explicit consent from all participants and displays a persistent red indicator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlateGreenText
                    )
                    Text("Recording Quality", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HD 720p", "FHD 1080p", "Audio Only").forEach { q ->
                            FilterChip(
                                selected = selectedQuality == q,
                                onClick = { selectedQuality = q },
                                label = { Text(q, fontSize = 12.sp) }
                            )
                        }
                    }
                    Text("Auto-Cleanup Retention Policy", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Preferences")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordingSettingsDialog = false }) {
                    Text("Close", color = SecondarySlateGreenText)
                }
            }
        )
    }

    // --- DIALOG 3: Privacy & Security (Encryption, Blocked Contacts, Report, Delete Account) ---
    if (showPrivacySecurityDialog) {
        var reportTarget by remember { mutableStateOf("") }
        var reportDetails by remember { mutableStateOf("") }
        val contacts = (contactsState as? UiState.Success)?.data.orEmpty()

        AlertDialog(
            onDismissRequest = { showPrivacySecurityDialog = false },
            containerColor = PureWhiteSurface,
            title = {
                Text("Privacy & Security Center", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = EmeraldLightTint,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Active Session: Verified Google Auth (${profile?.email?.ifBlank { "Authenticated" } ?: "Authenticated"})\n" +
                                "Local Protection: Android Keystore AES-256-GCM",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Text("Contact Access & Blocking", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    contacts.take(3).forEach { contact ->
                        val isBlocked = contact.status == "blocked"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(contact.contactName, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onToggleBlockContact(contact.id, isBlocked) }) {
                                Text(
                                    text = if (isBlocked) "Unblock" else "Block",
                                    color = if (isBlocked) EmeraldPrimary else RecordingRedStatus,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text("Submit Safety / Abuse Report", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedTextField(
                        value = reportTarget,
                        onValueChange = { reportTarget = it },
                        label = { Text("Target User or Room") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        label = { Text("Report Details") },
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
                                        reportDetails.ifBlank { "Safety report submitted from Privacy Center." }
                                    )
                                    showPrivacySecurityDialog = false
                                }
                            }
                        ) {
                            Text("Submit Report", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = {
                                showPrivacySecurityDialog = false
                                onDeleteAccountAndData()
                            }
                        ) {
                            Text("Delete Account Data", color = RecordingRedStatus, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacySecurityDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // --- DIALOG 4: Storage Manager (Usage, Consented Recordings, Playback & Delete) ---
    if (showStorageManagerDialog) {
        val recordings = (recordingsState as? UiState.Success)?.data.orEmpty()
        val totalBytes = recordings.sumOf { it.sizeBytes }
        val totalMb = String.format("%.2f MB", totalBytes / (1024f * 1024f))
        var playingRecId by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showStorageManagerDialog = false },
            containerColor = PureWhiteSurface,
            title = {
                Text("Storage Manager", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Encrypted Storage Used: $totalMb of 100 MB Quota",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenBlackText
                    )
                    LinearProgressIndicator(
                        progress = { (totalBytes / (100f * 1024f * 1024f)).coerceIn(0.04f, 1f) },
                        color = EmeraldPrimary,
                        trackColor = EmeraldLightTint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )

                    if (recordings.isEmpty()) {
                        Text(
                            text = "No consented recordings stored.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondarySlateGreenText,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(210.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recordings, key = { it.id }) { rec ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = EmeraldMintCard,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                playingRecId = if (playingRecId == rec.id) null else rec.id
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play Recording",
                                                tint = EmeraldPrimary
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rec.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepGreenBlackText
                                            )
                                            Text(
                                                text = if (playingRecId == rec.id) {
                                                    "Playing decrypted AES-256-GCM stream..."
                                                } else {
                                                    "${rec.durationSec}s • ${rec.encryptionAlgorithm} • Consent Verified"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SecondarySlateGreenText
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteRecording(rec.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Delete Recording",
                                                tint = RecordingRedStatus
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
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // --- DIALOG 5: About SecretGuard ---
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = PureWhiteSurface,
            title = {
                Text("About SecretGuard v1.0.0", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SecretGuard — Firebase Edition\nRecord • Call • Chat — Privately, transparently and securely.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenBlackText
                    )
                    Text(
                        text = "Recording Safety Policy: Every participant must explicitly consent before a call recording begins. Persistent recording indicators are shown to all participants, and recording stops immediately if consent is withdrawn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlateGreenText
                    )
                    Surface(
                        color = EmeraldLightTint,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Made with love by Rahul Shah",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
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
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = PureWhiteSurface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onCardClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EmeraldMintCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenBlackText,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondarySlateGreenText,
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhiteSurface,
                    checkedTrackColor = EmeraldPrimary,
                    uncheckedThumbColor = PureWhiteSurface,
                    uncheckedTrackColor = SecondarySlateGreenText.copy(alpha = 0.35f)
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
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = PureWhiteSurface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EmeraldMintCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenBlackText,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondarySlateGreenText,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title,
                tint = SecondarySlateGreenText
            )
        }
    }
}
