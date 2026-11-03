package com.example

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.security.KeystoreCryptoManager
import com.example.data.repository.SecretGuardRepository
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.CallsHistoryTabScreen
import com.example.ui.screens.ChatsListTabScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.SettingsPrivacyScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.theme.DeepGreenBlackText
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.RecordingRedStatus
import com.example.ui.theme.SecondarySlateGreenText
import com.example.ui.theme.WarmIvoryBackground
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

    // Attempt silent auto-sign-in once on cold start if unauthenticated
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
                SecretGuardViewModel(
                    repository = SecretGuardRepository(db, Firebase.auth, crypto),
                    cryptoManager = crypto,
                    currentUserId = currentUserId,
                    initialDisplayName = currentUser.displayName?.ifBlank { "Secret User" } ?: "Secret User",
                    initialEmail = currentUser.email.orEmpty(),
                    initialPhotoUrl = currentUser.photoUrl?.toString().orEmpty()
                )
            }
        }
    )

    val destination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val selectedConversationId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val callsState by viewModel.callsState.collectAsStateWithLifecycle()
    val recordingsState by viewModel.recordingsState.collectAsStateWithLifecycle()
    val activeCallState by viewModel.activeCallState.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isAppLockRequired by viewModel.isAppLockRequired.collectAsStateWithLifecycle()
    val pinFeedbackMessage by viewModel.pinFeedbackMessage.collectAsStateWithLifecycle()
    val isPinRevealed by viewModel.isPinRevealed.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBannerMessage) {
        val msg = statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    // Back button behavior for non-home tabs
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
        containerColor = WarmIvoryBackground,
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
        when (destination) {
            MainDestination.HOME -> {
                HomeDashboardScreen(
                    userProfileState = userProfileState,
                    contactsState = contactsState,
                    onTogglePresence = { viewModel.toggleOnlinePresence() },
                    onStartAudioCall = { name, avatar ->
                        viewModel.startCall(
                            participantName = name,
                            participantAvatarKey = avatar,
                            callType = "audio",
                            initialDisplayElapsed = 754L
                        )
                    },
                    onStartVideoCall = { name, avatar ->
                        viewModel.startCall(
                            participantName = name,
                            participantAvatarKey = avatar,
                            callType = "video",
                            initialDisplayElapsed = 754L
                        )
                    },
                    onSaveVoiceRecording = { title, duration ->
                        viewModel.saveConsentedVoiceRecording(title, duration)
                    },
                    onOpenChat = { name, avatar ->
                        viewModel.openConversationByParticipant(name, avatar)
                    },
                    onAddTrustedContact = { name, handle ->
                        viewModel.addTrustedContact(name, handle)
                    },
                    onOpenSettings = { viewModel.navigateToTab(MainDestination.SETTINGS) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            MainDestination.CALLS -> {
                CallsHistoryTabScreen(
                    callsState = callsState,
                    onStartAudioCall = { name, avatar ->
                        viewModel.startCall(name, avatar, "audio", 0L)
                    },
                    onStartVideoCall = { name, avatar ->
                        viewModel.startCall(name, avatar, "video", 0L)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            MainDestination.CHATS -> {
                ChatsListTabScreen(
                    conversationsState = conversationsState,
                    onOpenConversation = { convId -> viewModel.openConversation(convId) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            MainDestination.SETTINGS -> {
                SettingsPrivacyScreen(
                    userProfileState = userProfileState,
                    recordingsState = recordingsState,
                    contactsState = contactsState,
                    secretCodeDisplay = viewModel.getCurrentSecretCodeDisplay(),
                    isPinRevealed = isPinRevealed,
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
                    onDeleteRecording = { recId -> viewModel.deleteRecording(recId) },
                    onSubmitReport = { target, cat, details ->
                        viewModel.submitSafetyReport(target, cat, details)
                    },
                    onDeleteAccountAndData = {
                        viewModel.deleteAccountAndData(onComplete = onSignOut)
                    },
                    onLogoutClick = onSignOut,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            MainDestination.ACTIVE_CALL -> {
                ActiveCallScreen(
                    callState = activeCallState,
                    onBackClick = { viewModel.navigateBackToPreviousTab() },
                    onToggleMute = { viewModel.toggleCallMute() },
                    onToggleSpeaker = { viewModel.toggleCallSpeaker() },
                    onToggleCamera = { viewModel.toggleCallCamera() },
                    onSwitchCamera = { viewModel.switchCameraLens() },
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
                    conversation = selectedConversation,
                    messagesState = activeMessagesState,
                    onBackClick = { viewModel.navigateBackToPreviousTab() },
                    onStartAudioCall = { name, avatar ->
                        viewModel.startCall(name, avatar, "audio", 0L)
                    },
                    onStartVideoCall = { name, avatar ->
                        viewModel.startCall(name, avatar, "video", 0L)
                    },
                    onSendMessage = { text, type, duration, label ->
                        viewModel.sendChatMessage(text, type, duration, label)
                    }
                )
            }
        }
    }

    // Keystore PIN App Lock Overlay Gate when locked
    if (isAppLockRequired) {
        var enteredPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss App Lock without PIN or verified session */ },
            containerColor = PureWhiteSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SecretGuard App Lock", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your 6–8 digit Secret Code (Default: 246810) to unlock local Keystore session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlateGreenText
                    )
                    if (!pinFeedbackMessage.isNullOrBlank()) {
                        Text(
                            text = pinFeedbackMessage!!,
                            color = RecordingRedStatus,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
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
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.unlockAppWithBiometricOrAccountRecovery() }) {
                    Text("Biometric / Verified Session", color = EmeraldPrimary)
                }
            }
        )
    }
}

@Composable
private fun SecretGuardBottomNavigationBar(
    currentDestination: MainDestination,
    onTabSelected: (MainDestination) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = PureWhiteSurface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .border(1.dp, Color(0xFFE5EFEA), CircleShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 72.dp, height = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (selected) EmeraldPrimary else SecondarySlateGreenText,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) EmeraldPrimary else SecondarySlateGreenText
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
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}
