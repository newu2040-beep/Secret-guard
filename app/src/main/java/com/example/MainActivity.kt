package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.media.AudioMediaController
import com.example.core.notifications.SecretGuardNotificationHelper
import com.example.core.security.KeystoreCryptoManager
import com.example.data.repository.SecretGuardRepository
import com.example.ui.components.ContactAvatarView
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.CallsHistoryTabScreen
import com.example.ui.screens.ChatsListTabScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.SettingsPrivacyScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.theme.LocalSecretGuardTokens
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainDestination
import com.example.ui.viewmodel.SecretGuardViewModel
import com.example.ui.viewmodel.UiState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecretGuardNotificationHelper.ensureChannelsCreated(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SecretGuardAppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun SecretGuardAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isSigningIn by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { authErrorMessage = null },
                onUnauthenticated = { /* Stay on WelcomeAuthScreen */ },
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        WelcomeAuthScreen(
            isLoading = isSigningIn,
            errorMessage = authErrorMessage,
            onGoogleSignInClick = {
                isSigningIn = true
                authErrorMessage = null
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        isSigningIn = false
                        authErrorMessage = null
                    },
                    onAuthError = { errMsg ->
                        isSigningIn = false
                        authErrorMessage = errMsg
                    },
                    scope = scope,
                    onAuthCancelled = {
                        isSigningIn = false
                    }
                )
            }
        )
    } else {
        AuthenticatedSecretGuardWorkspace(
            currentUser = user,
            onSignOut = {
                signOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {},
                    scope = scope
                )
            }
        )
    }
}

@Composable
private fun AuthenticatedSecretGuardWorkspace(
    currentUser: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = currentUser.uid
    val viewModel: SecretGuardViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val crypto = KeystoreCryptoManager(app)
                val audioController = AudioMediaController(app, crypto)
                SecretGuardViewModel(
                    repository = SecretGuardRepository(db, Firebase.auth, crypto),
                    cryptoManager = crypto,
                    currentUserId = currentUserId,
                    initialDisplayName = currentUser.displayName.orEmpty(),
                    initialEmail = currentUser.email.orEmpty(),
                    initialPhotoUrl = currentUser.photoUrl?.toString().orEmpty(),
                    audioController = audioController,
                    appContext = app
                )
            }
        }
    )

    val selectedPastelTheme by viewModel.selectedPastelTheme.collectAsStateWithLifecycle()
    val selectedAppFont by viewModel.selectedAppFont.collectAsStateWithLifecycle()
    val isCompactModeEnabled by viewModel.isCompactModeEnabled.collectAsStateWithLifecycle()
    val selectedRingtoneId by viewModel.selectedRingtoneId.collectAsStateWithLifecycle()
    val customRingtoneLabel by viewModel.customRingtoneLabel.collectAsStateWithLifecycle()

    // Request essential runtime permissions on initial workspace entry
    val startupPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Handled gracefully by individual screens as well */ }

    LaunchedEffect(currentUserId) {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        startupPermissionsLauncher.launch(perms.toTypedArray())
    }

    // Real-time online/offline lifecycle presence sync in Firestore `/users/{uid}`
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.setPresenceOnline(true)
                Lifecycle.Event.ON_STOP -> viewModel.setPresenceOnline(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val destination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val privateSettingsState by viewModel.privateSettingsState.collectAsStateWithLifecycle()
    val directoryUsersState by viewModel.directoryUsersState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val selectedConversationId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val callsState by viewModel.callsState.collectAsStateWithLifecycle()
    val incomingCall by viewModel.incomingCallState.collectAsStateWithLifecycle()
    val recordingsState by viewModel.recordingsState.collectAsStateWithLifecycle()
    val activeCallState by viewModel.activeCallState.collectAsStateWithLifecycle()
    val playingMediaId by viewModel.playingMediaId.collectAsStateWithLifecycle()
    val liveMicAmplitude by viewModel.liveMicAmplitude.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isAppLockRequired by viewModel.isAppLockRequired.collectAsStateWithLifecycle()
    val pinFeedbackMessage by viewModel.pinFeedbackMessage.collectAsStateWithLifecycle()
    val isPinRevealed by viewModel.isPinRevealed.collectAsStateWithLifecycle()

    // Post real Android system notification when an incoming call rings
    LaunchedEffect(incomingCall?.id) {
        val call = incomingCall
        val notifEnabled = (privateSettingsState as? UiState.Success)?.data?.notificationAlert ?: true
        if (call != null && notifEnabled) {
            SecretGuardNotificationHelper.showIncomingCallNotification(
                context = context,
                callId = call.id,
                callerName = call.callerName.ifBlank { call.participantName },
                callType = call.callType
            )
        }
    }

    MyApplicationTheme(
        pastelTheme = selectedPastelTheme,
        appFont = selectedAppFont,
        isCompactMode = isCompactModeEnabled
    ) {
        val tokens = LocalSecretGuardTokens.current
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(statusBannerMessage) {
            val msg = statusBannerMessage
            if (!msg.isNullOrBlank()) {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearBanner()
            }
        }

        if (destination in listOf(MainDestination.CALLS, MainDestination.CHATS, MainDestination.SETTINGS)) {
            BackHandler {
                viewModel.navigateToTab(MainDestination.HOME)
            }
        }

        val selectedConversation = remember(conversationsState, selectedConversationId) {
            val list = (conversationsState as? UiState.Success)?.data.orEmpty()
            list.firstOrNull { it.id == selectedConversationId } ?: list.firstOrNull()
        }

        val showBottomBar = destination in listOf(
            MainDestination.HOME,
            MainDestination.CALLS,
            MainDestination.CHATS,
            MainDestination.SETTINGS
        )

        Scaffold(
            containerColor = tokens.bg,
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    SecretGuardBottomNavigationBar(
                        currentDestination = destination,
                        onTabSelected = { tab -> viewModel.navigateToTab(tab) }
                    )
                }
            }
        ) { innerPadding ->
            // Smooth 150–250ms fade + vertical slide navigation transitions
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200)) +
                        slideInVertically(animationSpec = tween(200)) { fullHeight -> fullHeight / 20 })
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                label = "SecretGuardScreenTransition"
            ) { targetDestination ->
                when (targetDestination) {
                    MainDestination.HOME -> {
                        HomeDashboardScreen(
                            currentUserId = currentUserId,
                            userProfileState = userProfileState,
                            directoryUsersState = directoryUsersState,
                            contactsState = contactsState,
                            liveMicAmplitude = liveMicAmplitude,
                            selectedPastelTheme = selectedPastelTheme,
                            selectedAppFont = selectedAppFont,
                            isCompactMode = isCompactModeEnabled,
                            onSelectPastelTheme = { viewModel.setPastelTheme(it) },
                            onSelectAppFont = { viewModel.setAppFont(it) },
                            onToggleCompactMode = { viewModel.setCompactMode(it) },
                            onUpdateProfileCustomization = { name, handle, vis, avatarUri, gender, age, reqApproval ->
                                viewModel.updateProfileCustomization(
                                    displayName = name,
                                    userHandle = handle,
                                    profileVisibility = vis,
                                    newAvatarUri = avatarUri,
                                    gender = gender,
                                    age = age,
                                    requireCallApproval = reqApproval
                                )
                            },
                            onToggleContactCallApproval = { contactId, approved ->
                                viewModel.setContactCallApproval(contactId, approved)
                            },
                            onTogglePresence = { viewModel.toggleOnlinePresence() },
                            onStartAudioCall = { name, avatar, calleeUid, extraUids ->
                                viewModel.startCall(
                                    participantName = name,
                                    participantAvatarKey = avatar,
                                    callType = "audio",
                                    calleeUid = calleeUid,
                                    additionalCalleeUids = extraUids
                                )
                            },
                            onStartVideoCall = { name, avatar, calleeUid, extraUids ->
                                viewModel.startCall(
                                    participantName = name,
                                    participantAvatarKey = avatar,
                                    callType = "video",
                                    calleeUid = calleeUid,
                                    additionalCalleeUids = extraUids
                                )
                            },
                            onStartHardwareVoiceRecording = {
                                viewModel.startHardwareVoiceRecording("standalone")
                            },
                            onCancelHardwareVoiceRecording = {
                                viewModel.cancelHardwareVoiceRecording()
                            },
                            onStopAndSaveVoiceRecording = { title, duration ->
                                viewModel.stopAndSaveStandaloneVoiceRecording(title, duration)
                            },
                            onOpenChat = { name, avatar, partnerUid ->
                                viewModel.openConversationByParticipant(name, avatar, partnerUid)
                            },
                            onAddTrustedContact = { name, handle, targetUid, avatarKey ->
                                viewModel.addTrustedContact(name, handle, targetUid, avatarKey)
                            },
                            onOpenChatsTab = { viewModel.navigateToTab(MainDestination.CHATS) },
                            onOpenSettings = { viewModel.navigateToTab(MainDestination.SETTINGS) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    MainDestination.CALLS -> {
                        CallsHistoryTabScreen(
                            currentUserId = currentUserId,
                            callsState = callsState,
                            contactsState = contactsState,
                            directoryUsersState = directoryUsersState,
                            onStartAudioCall = { name, avatar, calleeUid, extraUids ->
                                viewModel.startCall(name, avatar, "audio", calleeUid, extraUids)
                            },
                            onStartVideoCall = { name, avatar, calleeUid, extraUids ->
                                viewModel.startCall(name, avatar, "video", calleeUid, extraUids)
                            },
                            onJoinActiveCall = { callSession ->
                                viewModel.joinExistingCallSession(callSession)
                            },
                            onToggleContactCallApproval = { contactId, approved ->
                                viewModel.setContactCallApproval(contactId, approved)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    MainDestination.CHATS -> {
                        ChatsListTabScreen(
                            currentUserId = currentUserId,
                            conversationsState = conversationsState,
                            directoryUsersState = directoryUsersState,
                            onOpenConversation = { convId -> viewModel.openConversation(convId) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    MainDestination.SETTINGS -> {
                        SettingsPrivacyScreen(
                            userProfileState = userProfileState,
                            privateSettingsState = privateSettingsState,
                            recordingsState = recordingsState,
                            contactsState = contactsState,
                            secretCodeDisplay = viewModel.getCurrentSecretCodeDisplay(),
                            isPinRevealed = isPinRevealed,
                            playingMediaId = playingMediaId,
                            selectedPastelTheme = selectedPastelTheme,
                            selectedAppFont = selectedAppFont,
                            isCompactModeEnabled = isCompactModeEnabled,
                            selectedRingtoneId = selectedRingtoneId,
                            customRingtoneLabel = customRingtoneLabel,
                            onSelectPastelTheme = { viewModel.setPastelTheme(it) },
                            onSelectAppFont = { viewModel.setAppFont(it) },
                            onToggleCompactMode = { viewModel.setCompactMode(it) },
                            onSelectBuiltInRingtone = { viewModel.setBuiltInRingtone(it) },
                            onSelectCustomRingtoneUri = { viewModel.setCustomRingtoneUri(it) },
                            onStopRingtonePreview = { viewModel.stopRingtonePreview() },
                            onUpdateProfileCustomization = { name, handle, vis, avatarUri, gender, age, reqApproval ->
                                viewModel.updateProfileCustomization(
                                    displayName = name,
                                    userHandle = handle,
                                    profileVisibility = vis,
                                    newAvatarUri = avatarUri,
                                    gender = gender,
                                    age = age,
                                    requireCallApproval = reqApproval
                                )
                            },
                            onSetRequireCallApproval = { enabled ->
                                viewModel.setRequireCallApproval(enabled)
                            },
                            onToggleContactCallApproval = { contactId, approved ->
                                viewModel.setContactCallApproval(contactId, approved)
                            },
                            onBackClick = { viewModel.navigateToTab(MainDestination.HOME) },
                            onTogglePinVisibility = { viewModel.togglePinVisibility() },
                            onChangeSecretCode = { currentPin, newPin ->
                                viewModel.changeSecretCode(currentPin, newPin)
                            },
                            onResetSecretCodeViaRecovery = { newPin ->
                                viewModel.resetSecretCodeViaAuthenticatedRecovery(newPin)
                            },
                            onLockAppNow = { viewModel.lockAppNow() },
                            onUpdateSettings = { name, vis, lock, autoRec, notif, quality, retention ->
                                viewModel.updateUserSettings(name, vis, lock, autoRec, notif, quality, retention)
                            },
                            onToggleBlockContact = { contactId, currentlyBlocked ->
                                viewModel.toggleBlockContact(contactId, currentlyBlocked)
                            },
                            onTogglePlayRecording = { rec ->
                                viewModel.togglePlayMedia(
                                    itemId = rec.id,
                                    localFilePath = rec.localFilePath,
                                    encryptedBase64 = rec.audioBase64,
                                    channelId = rec.callId.ifBlank { "global" }
                                )
                            },
                            onDeleteRecording = { recId -> viewModel.deleteRecording(recId) },
                            onSubmitReport = { target, cat, details ->
                                viewModel.submitSafetyReport(target, cat, details)
                            },
                            onDeleteAccountAndData = {
                                viewModel.deleteAccountAndData(onComplete = onSignOut)
                            },
                            onLogoutClick = {
                                viewModel.setPresenceOnline(false)
                                onSignOut()
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    MainDestination.ACTIVE_CALL -> {
                        ActiveCallScreen(
                            currentUserId = currentUserId,
                            callState = activeCallState,
                            contactsState = contactsState,
                            directoryUsersState = directoryUsersState,
                            onBackClick = { viewModel.navigateBackToPreviousTab() },
                            onToggleMute = { viewModel.toggleCallMute() },
                            onToggleSpeaker = { viewModel.toggleCallSpeaker() },
                            onToggleCamera = { viewModel.toggleCallCamera() },
                            onSwitchCamera = { viewModel.switchCameraLens() },
                            onToggleHdResolution = { viewModel.toggleCallVideoHdResolution() },
                            onInviteParticipant = { uid, name ->
                                viewModel.inviteParticipantToActiveCall(uid, name)
                            },
                            onPublishVideoFrame = { frameB64 -> viewModel.publishLocalVideoFrame(frameB64) },
                            onOpenConsentDialog = { viewModel.promptRecordingConsentDialog(true) },
                            onDismissConsentDialog = { viewModel.promptRecordingConsentDialog(false) },
                            onConfirmAllParticipantsConsent = {
                                viewModel.confirmAllParticipantsConsentAndRecord()
                            },
                            onWithdrawConsentAndStop = {
                                viewModel.withdrawConsentAndStopRecording()
                            },
                            onEndCall = { viewModel.endActiveCall() }
                        )
                    }

                    MainDestination.CONVERSATION -> {
                        ConversationScreen(
                            currentUserId = currentUserId,
                            conversation = selectedConversation,
                            directoryUsersState = directoryUsersState,
                            messagesState = activeMessagesState,
                            playingMediaId = playingMediaId,
                            liveMicAmplitude = liveMicAmplitude,
                            onBackClick = { viewModel.navigateBackToPreviousTab() },
                            onStartAudioCall = { name, avatar, calleeUid ->
                                viewModel.startCall(name, avatar, "audio", calleeUid)
                            },
                            onStartVideoCall = { name, avatar, calleeUid ->
                                viewModel.startCall(name, avatar, "video", calleeUid)
                            },
                            onSendMessage = { text, type, duration, label ->
                                viewModel.sendChatMessage(text, type, duration, label)
                            },
                            onSendImageAttachment = { uri ->
                                viewModel.sendImageAttachment(uri)
                            },
                            onSendVideoAttachment = { uri ->
                                viewModel.sendVideoAttachment(uri)
                            },
                            onSendDocumentAttachment = { uri ->
                                viewModel.sendDocumentAttachment(uri)
                            },
                            onDeleteMessagePermanently = { msg ->
                                viewModel.deleteMessagePermanently(msg)
                            },
                            onStartHardwareVoiceRecording = {
                                viewModel.startHardwareVoiceRecording("chat")
                            },
                            onCancelHardwareVoiceRecording = {
                                viewModel.cancelHardwareVoiceRecording()
                            },
                            onStopAndSendVoiceMessage = { durationSec ->
                                viewModel.stopAndSendChatVoiceMessage(durationSec)
                            },
                            onTogglePlayAudioMessage = { msg ->
                                viewModel.togglePlayMedia(
                                    itemId = msg.id,
                                    localFilePath = "",
                                    encryptedBase64 = msg.mediaBase64,
                                    channelId = msg.conversationId.ifBlank { "global" }
                                )
                            },
                            onDecryptMessage = { ciphertext, fallback, channelId ->
                                viewModel.decryptMessagePreview(ciphertext, fallback, channelId)
                            }
                        )
                    }
                }
            }
        }

        // Real-Time Incoming Call Signaling Alert Dialog (1:1 or Group Call)
        val ringingCall = incomingCall
        if (ringingCall != null) {
            val callerDisplay = ringingCall.callerName.ifBlank { ringingCall.participantName }
            val isGroup = ringingCall.memberUids.size > 2
            AlertDialog(
                onDismissRequest = { /* User must explicitly Accept or Decline */ },
                shape = RoundedCornerShape(tokens.radiusCard),
                containerColor = tokens.bg,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (ringingCall.callType == "video") Icons.Filled.Videocam else Icons.Filled.Call,
                            contentDescription = null,
                            tint = tokens.primary
                        )
                        Spacer(modifier = Modifier.width(tokens.sp8))
                        Text(
                            text = if (isGroup) {
                                "Incoming Group ${ringingCall.callType.replaceFirstChar { it.uppercase() }} Call"
                            } else {
                                "Incoming ${ringingCall.callType.replaceFirstChar { it.uppercase() }} Call"
                            },
                            fontSize = tokens.headingSize,
                            lineHeight = tokens.headingLineHeight,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text
                        )
                    }
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(tokens.sp12)
                    ) {
                        ContactAvatarView(
                            avatarKey = ringingCall.participantAvatarKey,
                            displayName = callerDisplay,
                            size = 48.dp
                        )
                        Column {
                            Text(
                                text = callerDisplay,
                                fontSize = tokens.headingSize,
                                lineHeight = tokens.headingLineHeight,
                                fontWeight = FontWeight.Bold,
                                color = tokens.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap Accept to join live call",
                                fontSize = tokens.captionSize,
                                lineHeight = tokens.captionLineHeight,
                                color = tokens.textMuted
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SecretGuardNotificationHelper.dismissCallNotification(context, ringingCall.id)
                            viewModel.acceptIncomingCall(ringingCall)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                        shape = RoundedCornerShape(tokens.radiusPill)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(tokens.sp8))
                        Text("Accept")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            SecretGuardNotificationHelper.dismissCallNotification(context, ringingCall.id)
                            viewModel.declineIncomingCall(ringingCall)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.danger),
                        shape = RoundedCornerShape(tokens.radiusPill)
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(tokens.sp8))
                        Text("Decline")
                    }
                }
            )
        }

        // Native Android Biometric + PIN App Lock Overlay Gate
        if (isAppLockRequired) {
            var enteredPin by remember { mutableStateOf("") }
            val activity = context as? Activity

            LaunchedEffect(Unit) {
                if (activity != null) {
                    SecretGuardNotificationHelper.authenticateWithNativeBiometric(
                        activity = activity,
                        onAuthenticated = {
                            viewModel.unlockAppWithBiometricOrAccountRecovery()
                        },
                        onMessage = { msg ->
                            viewModel.setPinFeedbackMessage(msg)
                        }
                    )
                }
            }

            AlertDialog(
                onDismissRequest = { /* Cannot dismiss App Lock without Biometric or PIN */ },
                shape = RoundedCornerShape(tokens.radiusCard),
                containerColor = tokens.bg,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = tokens.primary)
                        Spacer(modifier = Modifier.width(tokens.sp8))
                        Text(
                            text = "App Lock",
                            fontSize = tokens.headingSize,
                            lineHeight = tokens.headingLineHeight,
                            fontWeight = FontWeight.Bold,
                            color = tokens.text
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(tokens.sp12)) {
                        Text(
                            text = "Unlock with Face / Biometric or enter your Secret Code PIN (Default: 246810).",
                            fontSize = tokens.captionSize,
                            lineHeight = tokens.captionLineHeight,
                            color = tokens.textMuted
                        )
                        if (!pinFeedbackMessage.isNullOrBlank()) {
                            Text(
                                text = pinFeedbackMessage!!,
                                color = tokens.danger,
                                fontSize = tokens.captionSize,
                                lineHeight = tokens.captionLineHeight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { enteredPin = it.filter { c -> c.isDigit() }.take(8) },
                            label = { Text("Secret Code PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.unlockAppWithPin(enteredPin) },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                        shape = RoundedCornerShape(tokens.radiusPill)
                    ) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (activity != null) {
                                SecretGuardNotificationHelper.authenticateWithNativeBiometric(
                                    activity = activity,
                                    onAuthenticated = {
                                        viewModel.unlockAppWithBiometricOrAccountRecovery()
                                    },
                                    onMessage = {
                                        viewModel.unlockAppWithBiometricOrAccountRecovery()
                                    }
                                )
                            } else {
                                viewModel.unlockAppWithBiometricOrAccountRecovery()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                            tint = tokens.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(tokens.sp4))
                        Text("Biometric", color = tokens.primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun SecretGuardBottomNavigationBar(
    currentDestination: MainDestination,
    onTabSelected: (MainDestination) -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = tokens.sp16, vertical = tokens.sp8),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = tokens.surface2,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .height(tokens.bottomNavHeight)
                .border(1.dp, tokens.border, CircleShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = tokens.sp8),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavTabItem(
                    label = "Home",
                    selected = currentDestination == MainDestination.HOME,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    testTag = "nav_tab_home",
                    onClick = { onTabSelected(MainDestination.HOME) }
                )
                BottomNavTabItem(
                    label = "Calls",
                    selected = currentDestination == MainDestination.CALLS,
                    selectedIcon = Icons.Filled.Call,
                    unselectedIcon = Icons.Outlined.Call,
                    testTag = "nav_tab_calls",
                    onClick = { onTabSelected(MainDestination.CALLS) }
                )
                BottomNavTabItem(
                    label = "Chats",
                    selected = currentDestination == MainDestination.CHATS,
                    selectedIcon = Icons.Filled.ChatBubble,
                    unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                    testTag = "nav_tab_chats",
                    onClick = { onTabSelected(MainDestination.CHATS) }
                )
                BottomNavTabItem(
                    label = "Settings",
                    selected = currentDestination == MainDestination.SETTINGS,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    testTag = "nav_tab_settings",
                    onClick = { onTabSelected(MainDestination.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavTabItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 68.dp, height = 48.dp)
            .clip(RoundedCornerShape(tokens.radiusCard))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (selected) tokens.primary else tokens.textMuted,
            modifier = Modifier.size(tokens.iconSize)
        )
        Spacer(modifier = Modifier.height(tokens.sp4))
        Text(
            text = label,
            fontSize = tokens.captionSize,
            lineHeight = tokens.captionLineHeight,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) tokens.primary else tokens.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- JETPACK CREDENTIAL MANAGER GOOGLE SIGN-IN FLOWS ---

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("SecretGuardAuth", "User cancelled Google Sign-In", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("SecretGuardAuth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign-in failed. Please check your Google account on this device.")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            Firebase.auth.signOut()
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
        } catch (e: Exception) {
            Log.e("SecretGuardAuth", "Error during sign out", e)
        } finally {
            onSignOutComplete()
        }
    }
}
