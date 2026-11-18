package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.media.AudioMediaController
import com.example.core.media.BuiltInRingtone
import com.example.core.security.KeystoreCryptoManager
import com.example.core.security.PinChangeResult
import com.example.core.security.PinVerifyResult
import com.example.data.model.CallRecording
import com.example.data.model.CallSession
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.TrustedContact
import com.example.data.model.UserPrivateSettings
import com.example.data.model.UserProfile
import com.example.data.repository.SecretGuardRepository
import com.example.ui.theme.AppFontOption
import com.example.ui.theme.PastelThemeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainDestination {
    HOME,
    CALLS,
    CHATS,
    SETTINGS,
    ACTIVE_CALL,
    CONVERSATION
}

data class ActiveCallUiState(
    val callId: String = "",
    val ownerUid: String = "",
    val calleeUid: String = "",
    val memberUids: List<String> = emptyList(),
    val isCaller: Boolean = true,
    val participantName: String = "",
    val participantAvatarKey: String = "custom",
    val callType: String = "video",
    val status: String = "ringing", // "ringing", "connected", "ended", "declined", "missed"
    val connectionState: String = "Ringing • Encrypted Signaling...",
    val elapsedSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCameraOn: Boolean = true,
    val isFrontCamera: Boolean = true,
    val isFullHd1080p: Boolean = true,
    val isLiveVoiceStreaming: Boolean = false,
    val callerConsent: Boolean = false,
    val calleeConsent: Boolean = false,
    val consentState: String = "none", // "none", "requested", "all_consented", "withdrawn"
    val isRecording: Boolean = false,
    val recordingElapsedSeconds: Long = 0L,
    val remoteVideoFrame: String = "",
    val showConsentDialog: Boolean = false
)

class SecretGuardViewModel(
    private val repository: SecretGuardRepository,
    private val cryptoManager: KeystoreCryptoManager,
    val currentUserId: String,
    private val initialDisplayName: String = "",
    private val initialEmail: String = "",
    private val initialPhotoUrl: String = "",
    private val audioController: AudioMediaController = AudioMediaController(null, cryptoManager),
    appContext: Context? = null
) : ViewModel() {

    private val prefs = appContext?.applicationContext?.getSharedPreferences(
        "secretguard_ui_prefs",
        Context.MODE_PRIVATE
    )

    private val _selectedPastelTheme = MutableStateFlow(
        PastelThemeOption.fromKey(prefs?.getString(KEY_PASTEL_THEME, null))
    )
    val selectedPastelTheme: StateFlow<PastelThemeOption> = _selectedPastelTheme.asStateFlow()

    private val _selectedAppFont = MutableStateFlow(
        AppFontOption.fromKey(prefs?.getString(KEY_APP_FONT, null))
    )
    val selectedAppFont: StateFlow<AppFontOption> = _selectedAppFont.asStateFlow()

    private val _isCompactModeEnabled = MutableStateFlow(
        prefs?.getBoolean(KEY_COMPACT_MODE, false) ?: false
    )
    val isCompactModeEnabled: StateFlow<Boolean> = _isCompactModeEnabled.asStateFlow()

    private val _selectedRingtoneId = MutableStateFlow(
        prefs?.getString(KEY_RINGTONE_ID, BuiltInRingtone.EMERALD_PULSE.id)
            ?: BuiltInRingtone.EMERALD_PULSE.id
    )
    val selectedRingtoneId: StateFlow<String> = _selectedRingtoneId.asStateFlow()

    private val _customRingtoneUri = MutableStateFlow(
        prefs?.getString(KEY_CUSTOM_RINGTONE_URI, "").orEmpty()
    )
    val customRingtoneUri: StateFlow<String> = _customRingtoneUri.asStateFlow()

    private val _customRingtoneLabel = MutableStateFlow(
        prefs?.getString(KEY_CUSTOM_RINGTONE_LABEL, "").orEmpty()
    )
    val customRingtoneLabel: StateFlow<String> = _customRingtoneLabel.asStateFlow()

    private val _currentDestination = MutableStateFlow(MainDestination.HOME)
    val currentDestination: StateFlow<MainDestination> = _currentDestination.asStateFlow()

    private val _previousTabDestination = MutableStateFlow(MainDestination.HOME)

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _activeCallState = MutableStateFlow(ActiveCallUiState())
    val activeCallState: StateFlow<ActiveCallUiState> = _activeCallState.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isAppLockRequired = MutableStateFlow(false)
    val isAppLockRequired: StateFlow<Boolean> = _isAppLockRequired.asStateFlow()

    private val _pinFeedbackMessage = MutableStateFlow<String?>(null)
    val pinFeedbackMessage: StateFlow<String?> = _pinFeedbackMessage.asStateFlow()

    private val _isPinRevealed = MutableStateFlow(false)
    val isPinRevealed: StateFlow<Boolean> = _isPinRevealed.asStateFlow()

    private val _playingMediaId = MutableStateFlow<String?>(null)
    val playingMediaId: StateFlow<String?> = _playingMediaId.asStateFlow()

    private val _isHardwareRecordingVoice = MutableStateFlow(false)
    val isHardwareRecordingVoice: StateFlow<Boolean> = _isHardwareRecordingVoice.asStateFlow()

    private val _liveMicAmplitude = MutableStateFlow(0.18f)
    val liveMicAmplitude: StateFlow<Float> = _liveMicAmplitude.asStateFlow()

    private var callTimerJob: Job? = null
    private var callSessionObserverJob: Job? = null
    private var micAmplitudeJob: Job? = null
    private var isCallMicRecordingActive: Boolean = false
    private var appLockInitialized: Boolean = false

    // Multiplexed live Full HD video + live G.711 μ-law voice packet state
    @Volatile
    private var latestLocalVideoFrameB64: String = ""
    @Volatile
    private var latestLocalAudioSeq: Long = 0L
    @Volatile
    private var latestLocalAudioUlawB64: String = ""
    @Volatile
    private var lastMultiplexPublishMs: Long = 0L

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing user profile", error)
            emit(UiState.Error(error.message ?: "Failed to load user profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val privateSettingsState: StateFlow<UiState<UserPrivateSettings?>> = repository.observePrivateSettings(currentUserId)
        .map<UserPrivateSettings?, UiState<UserPrivateSettings?>> { settings ->
            if (!appLockInitialized && settings != null) {
                appLockInitialized = true
                if (settings.appLockEnabled) {
                    _isAppLockRequired.value = true
                }
            }
            UiState.Success(settings)
        }
        .catch { error ->
            Log.w(TAG, "Error observing private settings", error)
            emit(UiState.Error(error.message ?: "Failed to load private settings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val directoryUsersState: StateFlow<UiState<List<UserProfile>>> = repository.observeDirectoryUsers()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { users ->
            UiState.Success(users.filter { it.uid.isNotBlank() && it.uid != currentUserId })
        }
        .catch { error ->
            Log.w(TAG, "Error observing user directory", error)
            emit(UiState.Error(error.message ?: "Failed to load user directory"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val contactsState: StateFlow<UiState<List<TrustedContact>>> = repository.observeContacts(currentUserId)
        .map<List<TrustedContact>, UiState<List<TrustedContact>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing contacts", error)
            emit(UiState.Error(error.message ?: "Failed to load trusted contacts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val conversationsState: StateFlow<UiState<List<Conversation>>> = repository.observeConversations(currentUserId)
        .map<List<Conversation>, UiState<List<Conversation>>> { list ->
            if (_selectedConversationId.value == null && list.isNotEmpty()) {
                _selectedConversationId.value = list.first().id
            }
            UiState.Success(list)
        }
        .catch { error ->
            Log.w(TAG, "Error observing conversations", error)
            emit(UiState.Error(error.message ?: "Failed to load conversations"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> = _selectedConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(currentUserId, convId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { list ->
                        if (_currentDestination.value == MainDestination.CONVERSATION) {
                            viewModelScope.launch {
                                repository.markIncomingMessagesAsRead(convId, list)
                            }
                        }
                        UiState.Success(list)
                    }
                    .catch { error ->
                        Log.w(TAG, "Error observing messages", error)
                        emit(UiState.Error(error.message ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val callsState: StateFlow<UiState<List<CallSession>>> = repository.observeCalls(currentUserId)
        .map<List<CallSession>, UiState<List<CallSession>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing calls", error)
            emit(UiState.Error(error.message ?: "Failed to load calls"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    /**
     * Emits an active incoming ringing 1:1 or group call targeting `currentUserId`.
     */
    val incomingCallState: StateFlow<CallSession?> = callsState
        .map { state ->
            val list = (state as? UiState.Success)?.data.orEmpty()
            val activeId = _activeCallState.value.callId
            list.firstOrNull { call ->
                (call.calleeUid == currentUserId || currentUserId in call.memberUids) &&
                    call.ownerUid != currentUserId &&
                    call.status == "ringing" &&
                    call.id != activeId
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val recordingsState: StateFlow<UiState<List<CallRecording>>> = repository.observeRecordings(currentUserId)
        .map<List<CallRecording>, UiState<List<CallRecording>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing recordings", error)
            emit(UiState.Error(error.message ?: "Failed to load recordings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            try {
                val pendingName = prefs?.getString(KEY_PENDING_AUTH_NAME, "").orEmpty()
                val pendingHandle = prefs?.getString(KEY_PENDING_AUTH_HANDLE, "").orEmpty()
                val pendingGender = prefs?.getString(KEY_PENDING_AUTH_GENDER, "").orEmpty()
                val pendingAge = prefs?.getLong(KEY_PENDING_AUTH_AGE, 0L) ?: 0L
                val pendingVis = prefs?.getString(KEY_PENDING_AUTH_VISIBILITY, "").orEmpty()

                repository.ensureWorkspaceInitialized(
                    displayName = pendingName.ifBlank { initialDisplayName },
                    email = initialEmail,
                    photoUrl = initialPhotoUrl
                )

                if (pendingName.isNotBlank() || pendingGender.isNotBlank() || pendingAge > 0L || pendingVis.isNotBlank()) {
                    repository.updateUserProfileCustomization(
                        displayName = pendingName.ifBlank { initialDisplayName.ifBlank { "SecretGuard User" } },
                        userHandle = pendingHandle,
                        photoUrl = initialPhotoUrl,
                        profileVisibility = pendingVis.ifBlank { "contacts" },
                        gender = pendingGender,
                        age = pendingAge,
                        requireCallApproval = false
                    )
                    prefs?.edit()
                        ?.remove(KEY_PENDING_AUTH_NAME)
                        ?.remove(KEY_PENDING_AUTH_HANDLE)
                        ?.remove(KEY_PENDING_AUTH_GENDER)
                        ?.remove(KEY_PENDING_AUTH_AGE)
                        ?.remove(KEY_PENDING_AUTH_VISIBILITY)
                        ?.apply()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Workspace init warning", e)
            }
        }
        // Automatically start/stop incoming call ringtone when an incoming call rings
        viewModelScope.launch {
            var currentlyRingingCallId: String? = null
            incomingCallState.collect { ringingCall ->
                if (ringingCall != null && ringingCall.id != currentlyRingingCallId) {
                    currentlyRingingCallId = ringingCall.id
                    audioController.startRingingIncomingCall(
                        ringtoneId = _selectedRingtoneId.value,
                        customRingtoneUri = _customRingtoneUri.value
                    )
                } else if (ringingCall == null && currentlyRingingCallId != null) {
                    currentlyRingingCallId = null
                    audioController.stopRingingIncomingCall()
                }
            }
        }
    }

    // --- PASTEL THEME, FONTS, COMPACT MODE & RINGTONE PREFERENCES ---

    fun setPastelTheme(theme: PastelThemeOption) {
        _selectedPastelTheme.value = theme
        prefs?.edit()?.putString(KEY_PASTEL_THEME, theme.key)?.apply()
        showBanner("Theme set to ${theme.displayName}.")
    }

    fun setAppFont(font: AppFontOption) {
        _selectedAppFont.value = font
        prefs?.edit()?.putString(KEY_APP_FONT, font.id)?.apply()
        showBanner("Font updated to ${font.displayName}.")
    }

    fun setCompactMode(enabled: Boolean) {
        _isCompactModeEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_COMPACT_MODE, enabled)?.apply()
        showBanner(if (enabled) "Compact Display Mode enabled." else "Standard Display Mode enabled.")
    }

    fun setBuiltInRingtone(ringtone: BuiltInRingtone) {
        _selectedRingtoneId.value = ringtone.id
        _customRingtoneUri.value = ""
        _customRingtoneLabel.value = ""
        prefs?.edit()
            ?.putString(KEY_RINGTONE_ID, ringtone.id)
            ?.putString(KEY_CUSTOM_RINGTONE_URI, "")
            ?.putString(KEY_CUSTOM_RINGTONE_LABEL, "")
            ?.apply()
        audioController.previewRingtone(ringtone.id, "")
    }

    fun setCustomRingtoneUri(uri: Uri) {
        val uriStr = uri.toString()
        val label = uri.lastPathSegment?.substringAfterLast('/') ?: "Custom Ringtone"
        _customRingtoneUri.value = uriStr
        _customRingtoneLabel.value = label
        prefs?.edit()
            ?.putString(KEY_CUSTOM_RINGTONE_URI, uriStr)
            ?.putString(KEY_CUSTOM_RINGTONE_LABEL, label)
            ?.apply()
        audioController.previewRingtone(_selectedRingtoneId.value, uriStr)
        showBanner("Custom ringtone selected: $label")
    }

    fun stopRingtonePreview() {
        audioController.stopRingingIncomingCall()
    }

    // --- PROFILE CUSTOMIZATION (DISPLAY NAME, HANDLE, GENDER, AGE, CALL APPROVAL, PHOTO) ---

    fun updateProfileCustomization(
        displayName: String,
        userHandle: String,
        profileVisibility: String,
        newAvatarUri: Uri? = null,
        gender: String? = null,
        age: Long? = null,
        requireCallApproval: Boolean? = null
    ) {
        val currentProfile = (userProfileState.value as? UiState.Success)?.data
        viewModelScope.launch {
            try {
                val encodedAvatar = if (newAvatarUri != null) {
                    withContext(Dispatchers.IO) {
                        audioController.encodeProfileAvatarUri(newAvatarUri)
                    } ?: currentProfile?.photoUrl.orEmpty()
                } else {
                    currentProfile?.photoUrl.orEmpty()
                }
                repository.updateUserProfileCustomization(
                    displayName = displayName,
                    userHandle = userHandle,
                    photoUrl = encodedAvatar,
                    profileVisibility = profileVisibility,
                    gender = gender ?: currentProfile?.gender.orEmpty(),
                    age = age ?: (currentProfile?.age ?: 0L),
                    requireCallApproval = requireCallApproval ?: (currentProfile?.requireCallApproval ?: false)
                )
                showBanner("Profile saved.")
            } catch (e: Exception) {
                showBanner("Failed to update profile: ${e.localizedMessage}")
            }
        }
    }

    fun setRequireCallApproval(required: Boolean) {
        viewModelScope.launch {
            try {
                repository.setRequireCallApproval(required)
                showBanner(
                    if (required) "Mutual Call Approval enabled for incoming calls."
                    else "Direct calling enabled."
                )
            } catch (e: Exception) {
                showBanner("Failed to update call approval setting: ${e.localizedMessage}")
            }
        }
    }

    fun requestCallApproval(
        partnerName: String,
        partnerHandle: String,
        partnerUid: String,
        avatarKey: String = "custom"
    ) {
        val myProfile = (userProfileState.value as? UiState.Success)?.data
        val myName = myProfile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }
        val myHandle = myProfile?.userHandle?.ifBlank { "@${currentUserId.take(6)}" } ?: "@${currentUserId.take(6)}"
        viewModelScope.launch {
            try {
                repository.requestCallApprovalForPartner(
                    partnerUid = partnerUid,
                    partnerName = partnerName,
                    partnerHandle = partnerHandle,
                    partnerAvatar = avatarKey,
                    requesterName = myName,
                    requesterHandle = myHandle
                )
                showBanner("Call approval request sent to $partnerName.")
            } catch (e: Exception) {
                showBanner("Failed to request call approval: ${e.localizedMessage}")
            }
        }
    }

    fun setContactCallApproval(contactId: String, approved: Boolean) {
        viewModelScope.launch {
            try {
                repository.setContactCallApproval(contactId, approved)
                showBanner(if (approved) "Call approved! You can now call each other." else "Call approval revoked.")
            } catch (e: Exception) {
                showBanner("Failed to update call approval: ${e.localizedMessage}")
            }
        }
    }

    fun navigateToTab(destination: MainDestination) {
        if (destination in listOf(
                MainDestination.HOME,
                MainDestination.CALLS,
                MainDestination.CHATS,
                MainDestination.SETTINGS
            )
        ) {
            _previousTabDestination.value = destination
        }
        _currentDestination.value = destination
    }

    fun navigateBackToPreviousTab() {
        _currentDestination.value = _previousTabDestination.value
    }

    fun openConversation(conversationId: String) {
        _selectedConversationId.value = conversationId
        _currentDestination.value = MainDestination.CONVERSATION
        val currentMsgs = (activeMessagesState.value as? UiState.Success)?.data.orEmpty()
        if (currentMsgs.isNotEmpty()) {
            viewModelScope.launch {
                repository.markIncomingMessagesAsRead(conversationId, currentMsgs)
            }
        }
    }

    fun openConversationByParticipant(
        participantName: String,
        avatarKey: String = "custom",
        partnerUid: String = ""
    ) {
        val existingList = (conversationsState.value as? UiState.Success)?.data.orEmpty()
        val match = existingList.firstOrNull { conv ->
            (partnerUid.isNotBlank() && partnerUid in conv.memberUids && conv.memberUids.size > 1) ||
                conv.participantName.equals(participantName, ignoreCase = true) ||
                conv.ownerName.equals(participantName, ignoreCase = true)
        }
        if (match != null) {
            openConversation(match.id)
        } else {
            val myProfile = (userProfileState.value as? UiState.Success)?.data
            val myName = myProfile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }
            viewModelScope.launch {
                try {
                    val deterministicId = if (partnerUid.isNotBlank()) {
                        repository.buildDeterministicConversationId(currentUserId, partnerUid)
                    } else {
                        "conv_${java.util.UUID.randomUUID().toString().replace("-", "").take(16)}"
                    }
                    val newId = repository.createConversationWithId(
                        conversationId = deterministicId,
                        partnerUid = partnerUid,
                        ownerName = myName,
                        participantName = participantName,
                        participantAvatarKey = avatarKey,
                        lastMessageText = "Encrypted AES-256-GCM session initialized."
                    )
                    openConversation(newId)
                } catch (e: Exception) {
                    showBanner("Failed to open conversation: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- REAL-TIME MULTI-USER & GROUP CALL SIGNALING + LIVE TWO-WAY VOICE & FULL HD 1080P VIDEO ---

    fun startCall(
        participantName: String,
        participantAvatarKey: String = "custom",
        callType: String = "video",
        calleeUid: String = "",
        additionalCalleeUids: List<String> = emptyList()
    ) {
        val myProfile = (userProfileState.value as? UiState.Success)?.data
        val callerName = myProfile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }

        val contacts = (contactsState.value as? UiState.Success)?.data.orEmpty()
        val dirUsers = (directoryUsersState.value as? UiState.Success)?.data.orEmpty()

        val resolvedCalleeUid = if (calleeUid.isNotBlank()) {
            calleeUid
        } else {
            val matchedContact = contacts.firstOrNull { c ->
                val display = if (c.ownerUid == currentUserId) c.contactName else c.requesterName
                display.equals(participantName, ignoreCase = true)
            }
            val contactPartnerUid = matchedContact?.let { c ->
                if (c.ownerUid == currentUserId) c.targetUid else c.ownerUid
            }.orEmpty()
            if (contactPartnerUid.isNotBlank() && contactPartnerUid != currentUserId) {
                contactPartnerUid
            } else {
                dirUsers.firstOrNull { u ->
                    u.displayName.equals(participantName, ignoreCase = true) ||
                        u.userHandle.equals(participantName, ignoreCase = true)
                }?.uid.orEmpty()
            }
        }

        // Check mutual Call Approval (optional feature: if either user requires call approval or contact is unapproved)
        if (additionalCalleeUids.isEmpty() && resolvedCalleeUid.isNotBlank() && resolvedCalleeUid != currentUserId) {
            val existingContact = contacts.firstOrNull { c ->
                c.peerUid(currentUserId) == resolvedCalleeUid
            }
            val targetUserProfile = dirUsers.firstOrNull { it.uid == resolvedCalleeUid }
            val approvalRequired = (myProfile?.requireCallApproval == true) ||
                (targetUserProfile?.requireCallApproval == true) ||
                (existingContact != null && !existingContact.callApproved)

            if (approvalRequired && (existingContact == null || !existingContact.callApproved)) {
                requestCallApproval(
                    partnerName = participantName,
                    partnerHandle = targetUserProfile?.userHandle ?: "@user",
                    partnerUid = resolvedCalleeUid,
                    avatarKey = participantAvatarKey
                )
                return
            }
        }

        viewModelScope.launch {
            try {
                latestLocalVideoFrameB64 = ""
                latestLocalAudioSeq = 0L
                latestLocalAudioUlawB64 = ""

                val callId = repository.startCallSession(
                    calleeUid = resolvedCalleeUid,
                    additionalCalleeUids = additionalCalleeUids,
                    callerName = callerName,
                    participantName = participantName,
                    participantAvatarKey = participantAvatarKey,
                    callType = callType
                )
                val allMembers = (listOf(currentUserId, resolvedCalleeUid.ifBlank { currentUserId }) + additionalCalleeUids)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(8)
                val isSelfCall = allMembers.size <= 1
                val initialStatus = if (isSelfCall) "connected" else "ringing"
                _activeCallState.value = ActiveCallUiState(
                    callId = callId,
                    ownerUid = currentUserId,
                    calleeUid = resolvedCalleeUid.ifBlank { currentUserId },
                    memberUids = allMembers,
                    isCaller = true,
                    participantName = participantName,
                    participantAvatarKey = participantAvatarKey,
                    callType = callType,
                    status = initialStatus,
                    connectionState = if (isSelfCall) "Connected • Full HD 1080p" else "Ringing $participantName...",
                    elapsedSeconds = 0L,
                    isMuted = false,
                    isSpeakerOn = true,
                    isCameraOn = callType == "video",
                    isFrontCamera = true,
                    isFullHd1080p = true,
                    isLiveVoiceStreaming = true,
                    callerConsent = false,
                    calleeConsent = false,
                    consentState = "none",
                    isRecording = false,
                    recordingElapsedSeconds = 0L,
                    remoteVideoFrame = "",
                    showConsentDialog = false
                )
                audioController.configureCallAudioRouting(
                    active = true,
                    isMuted = false,
                    isSpeakerOn = true
                )
                startTwoWayLiveCallVoiceBridge()
                _currentDestination.value = MainDestination.ACTIVE_CALL
                observeActiveCallRealtime(callId)
                if (isSelfCall) {
                    startCallTimer()
                }
            } catch (e: Exception) {
                showBanner("Unable to start call: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Invites an additional contact/user into the currently active audio or video call (up to 8 participants).
     */
    fun inviteParticipantToActiveCall(newParticipantUid: String, newParticipantName: String) {
        val current = _activeCallState.value
        if (current.callId.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addParticipantToCall(
                    callId = current.callId,
                    newParticipantUid = newParticipantUid,
                    newParticipantName = newParticipantName
                )
                showBanner("Invited $newParticipantName to group ${current.callType} call.")
            } catch (e: Exception) {
                showBanner("Failed to add participant: ${e.localizedMessage}")
            }
        }
    }

    fun acceptIncomingCall(call: CallSession) {
        audioController.stopRingingIncomingCall()
        viewModelScope.launch {
            try {
                latestLocalVideoFrameB64 = ""
                latestLocalAudioSeq = 0L
                latestLocalAudioUlawB64 = ""

                repository.acceptIncomingCall(call.id)
                val remoteName = call.callerName.ifBlank { call.participantName }
                _activeCallState.value = ActiveCallUiState(
                    callId = call.id,
                    ownerUid = call.ownerUid,
                    calleeUid = call.calleeUid,
                    memberUids = call.memberUids,
                    isCaller = false,
                    participantName = remoteName,
                    participantAvatarKey = call.participantAvatarKey,
                    callType = call.callType,
                    status = "connected",
                    connectionState = "Connected • Full HD 1080p",
                    elapsedSeconds = 0L,
                    isMuted = false,
                    isSpeakerOn = true,
                    isCameraOn = call.callType == "video",
                    isFrontCamera = true,
                    isFullHd1080p = true,
                    isLiveVoiceStreaming = true,
                    callerConsent = call.callerConsent,
                    calleeConsent = call.calleeConsent,
                    consentState = call.consentState,
                    isRecording = call.isRecording,
                    recordingElapsedSeconds = 0L,
                    remoteVideoFrame = extractVideoPartFromMultiplex(call.callerVideoFrame),
                    showConsentDialog = false
                )
                audioController.configureCallAudioRouting(
                    active = true,
                    isMuted = false,
                    isSpeakerOn = true
                )
                startTwoWayLiveCallVoiceBridge()
                _currentDestination.value = MainDestination.ACTIVE_CALL
                observeActiveCallRealtime(call.id)
                startCallTimer()
            } catch (e: Exception) {
                showBanner("Failed to accept call: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Allows joining an active or ringing group/multi-user call from the Calls tab.
     */
    fun joinExistingCallSession(call: CallSession) {
        if (call.status == "ringing" && call.ownerUid != currentUserId) {
            acceptIncomingCall(call)
            return
        }
        val myProfile = (userProfileState.value as? UiState.Success)?.data
        val myName = myProfile?.displayName?.ifBlank { initialDisplayName } ?: "Participant"
        viewModelScope.launch {
            try {
                if (currentUserId !in call.memberUids) {
                    repository.addParticipantToCall(call.id, currentUserId, myName)
                }
                val isCaller = call.ownerUid == currentUserId
                val remoteName = if (isCaller) call.participantName else call.callerName.ifBlank { call.participantName }
                _activeCallState.value = ActiveCallUiState(
                    callId = call.id,
                    ownerUid = call.ownerUid,
                    calleeUid = call.calleeUid,
                    memberUids = (call.memberUids + currentUserId).distinct(),
                    isCaller = isCaller,
                    participantName = remoteName,
                    participantAvatarKey = call.participantAvatarKey,
                    callType = call.callType,
                    status = "connected",
                    connectionState = "Joined Group Call • Full HD",
                    elapsedSeconds = call.durationSec,
                    isMuted = false,
                    isSpeakerOn = true,
                    isCameraOn = call.callType == "video",
                    isFrontCamera = true,
                    isFullHd1080p = true,
                    isLiveVoiceStreaming = true,
                    callerConsent = call.callerConsent,
                    calleeConsent = call.calleeConsent,
                    consentState = call.consentState,
                    isRecording = call.isRecording,
                    recordingElapsedSeconds = 0L,
                    remoteVideoFrame = extractVideoPartFromMultiplex(
                        if (isCaller) call.calleeVideoFrame else call.callerVideoFrame
                    ),
                    showConsentDialog = false
                )
                audioController.configureCallAudioRouting(active = true, isMuted = false, isSpeakerOn = true)
                startTwoWayLiveCallVoiceBridge()
                _currentDestination.value = MainDestination.ACTIVE_CALL
                observeActiveCallRealtime(call.id)
                startCallTimer()
            } catch (e: Exception) {
                showBanner("Unable to join call: ${e.localizedMessage}")
            }
        }
    }

    fun declineIncomingCall(call: CallSession) {
        audioController.stopRingingIncomingCall()
        viewModelScope.launch {
            try {
                repository.declineIncomingCall(call.id)
                showBanner("Declined incoming call from ${call.callerName.ifBlank { call.participantName }}.")
            } catch (e: Exception) {
                showBanner("Failed to decline call: ${e.localizedMessage}")
            }
        }
    }

    private fun startTwoWayLiveCallVoiceBridge() {
        audioController.startLiveCallAudioTransceiver(
            isMuted = _activeCallState.value.isMuted,
            onAudioPacketReady = { seq, ulawB64 ->
                latestLocalAudioSeq = seq
                latestLocalAudioUlawB64 = ulawB64
                publishMultiplexedCallMediaPacket()
            }
        )
    }

    private fun publishMultiplexedCallMediaPacket() {
        val current = _activeCallState.value
        if (current.callId.isBlank() || current.status !in listOf("ringing", "connected")) return
        val now = System.currentTimeMillis()
        if (now - lastMultiplexPublishMs < 260L) return
        lastMultiplexPublishMs = now

        val videoPart = if (current.isCameraOn) latestLocalVideoFrameB64 else ""
        val audioSeq = latestLocalAudioSeq
        val audioPart = if (!current.isMuted) latestLocalAudioUlawB64 else ""
        if (videoPart.isBlank() && audioPart.isBlank()) return

        val payload = "AV1|$audioSeq|$audioPart|$videoPart"
        if (payload.length > 430_000) return

        viewModelScope.launch {
            repository.publishLiveVideoFrame(
                callId = current.callId,
                isCaller = current.isCaller,
                base64Jpeg = payload
            )
        }
    }

    private fun extractVideoPartFromMultiplex(rawPayload: String): String {
        if (rawPayload.isBlank()) return ""
        if (!rawPayload.startsWith("AV1|")) return rawPayload
        val parts = rawPayload.split("|", limit = 4)
        return if (parts.size == 4) parts[3] else ""
    }

    private fun demultiplexAndPlayRemoteCallMedia(rawPayload: String, currentRemoteVideo: String): String {
        if (rawPayload.isBlank()) return ""
        if (!rawPayload.startsWith("AV1|")) return rawPayload
        val parts = rawPayload.split("|", limit = 4)
        if (parts.size < 4) return currentRemoteVideo
        val seq = parts[1].toLongOrNull() ?: 0L
        val audioUlawB64 = parts[2]
        val videoJpegB64 = parts[3]
        if (seq > 0L && audioUlawB64.isNotBlank()) {
            audioController.playIncomingCallAudioChunk(seq, audioUlawB64)
        }
        return if (videoJpegB64.isNotBlank()) videoJpegB64 else currentRemoteVideo
    }

    private fun observeActiveCallRealtime(callId: String) {
        callSessionObserverJob?.cancel()
        callSessionObserverJob = viewModelScope.launch {
            repository.observeCallSession(callId)
                .catch { e -> Log.w(TAG, "Active call observer error", e) }
                .collect { session ->
                    if (session == null) return@collect
                    val current = _activeCallState.value
                    val isCaller = session.ownerUid == currentUserId
                    val rawRemotePayload = if (isCaller) session.calleeVideoFrame else session.callerVideoFrame
                    val nextRemoteVideo = demultiplexAndPlayRemoteCallMedia(
                        rawPayload = rawRemotePayload,
                        currentRemoteVideo = current.remoteVideoFrame
                    )

                    val remoteDisplayName = if (isCaller) {
                        session.participantName
                    } else {
                        session.callerName.ifBlank { session.participantName }
                    }

                    val participantCount = session.memberUids.size.coerceAtLeast(1)
                    val connectionLabel = when (session.status) {
                        "ringing" -> "Ringing $remoteDisplayName..."
                        "connected" -> if (participantCount > 2) {
                            "Group Call ($participantCount) • Full HD Live"
                        } else {
                            "Connected • Full HD 1080p Live"
                        }
                        "declined" -> "Call Declined"
                        "ended" -> "Call Ended"
                        else -> "Encrypted Session"
                    }

                    if (session.status == "connected" && current.status != "connected") {
                        startCallTimer()
                    }

                    val shouldRecordNow = session.status == "connected" &&
                        session.consentState == "all_consented" &&
                        session.isRecording
                    if (shouldRecordNow && !isCallMicRecordingActive) {
                        isCallMicRecordingActive = audioController.startMicrophoneRecording("call")
                    } else if (!shouldRecordNow && isCallMicRecordingActive) {
                        isCallMicRecordingActive = false
                        audioController.stopMicrophoneRecordingAndDiscard()
                    }

                    _activeCallState.update { state ->
                        state.copy(
                            ownerUid = session.ownerUid,
                            calleeUid = session.calleeUid,
                            memberUids = session.memberUids,
                            isCaller = isCaller,
                            participantName = remoteDisplayName,
                            status = session.status,
                            connectionState = connectionLabel,
                            callerConsent = session.callerConsent,
                            calleeConsent = session.calleeConsent,
                            consentState = session.consentState,
                            isRecording = shouldRecordNow,
                            remoteVideoFrame = nextRemoteVideo
                        )
                    }

                    if (session.status in listOf("declined", "ended") &&
                        _currentDestination.value == MainDestination.ACTIVE_CALL &&
                        current.status !in listOf("declined", "ended")
                    ) {
                        callTimerJob?.cancel()
                        audioController.stopLiveCallAudioTransceiver()
                        audioController.configureCallAudioRouting(active = false, isMuted = false, isSpeakerOn = false)
                        showBanner(if (session.status == "declined") "Call was declined." else "Call ended.")
                        _currentDestination.value = _previousTabDestination.value
                    }
                }
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _activeCallState.update { state ->
                    if (state.status == "connected") {
                        state.copy(
                            elapsedSeconds = state.elapsedSeconds + 1L,
                            recordingElapsedSeconds = if (state.isRecording) {
                                state.recordingElapsedSeconds + 1L
                            } else {
                                state.recordingElapsedSeconds
                            }
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun publishLocalVideoFrame(base64Jpeg: String) {
        val current = _activeCallState.value
        if (current.callId.isBlank() || !current.isCameraOn || current.status !in listOf("ringing", "connected")) return
        latestLocalVideoFrameB64 = base64Jpeg
        publishMultiplexedCallMediaPacket()
    }

    fun toggleCallMute() {
        _activeCallState.update { state ->
            val nextMute = !state.isMuted
            audioController.setLiveCallMicMuted(nextMute)
            audioController.configureCallAudioRouting(
                active = true,
                isMuted = nextMute,
                isSpeakerOn = state.isSpeakerOn
            )
            state.copy(isMuted = nextMute)
        }
    }

    fun toggleCallSpeaker() {
        _activeCallState.update { state ->
            val nextSpeaker = !state.isSpeakerOn
            audioController.configureCallAudioRouting(
                active = true,
                isMuted = state.isMuted,
                isSpeakerOn = nextSpeaker
            )
            state.copy(isSpeakerOn = nextSpeaker)
        }
    }

    fun toggleCallCamera() {
        _activeCallState.update { state ->
            val nextCam = !state.isCameraOn
            if (!nextCam) {
                latestLocalVideoFrameB64 = ""
            }
            state.copy(isCameraOn = nextCam)
        }
    }

    fun switchCameraLens() {
        _activeCallState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun toggleCallVideoHdResolution() {
        _activeCallState.update { state ->
            val nextHd = !state.isFullHd1080p
            showBanner(if (nextHd) "Video Call set to Full HD 1080p." else "Video Call set to HD 720p.")
            state.copy(isFullHd1080p = nextHd)
        }
    }

    fun promptRecordingConsentDialog(show: Boolean) {
        _activeCallState.update { it.copy(showConsentDialog = show) }
    }

    /**
     * Records the current user's consent or confirms mutual consent across participants.
     */
    fun confirmAllParticipantsConsentAndRecord() {
        val current = _activeCallState.value
        val isSingleDeviceOrBothConsented = current.ownerUid == current.calleeUid ||
            current.calleeUid.isBlank() ||
            (current.isCaller && current.calleeConsent) ||
            (!current.isCaller && current.callerConsent)

        val newCallerConsent = if (current.isCaller) true else (current.callerConsent || isSingleDeviceOrBothConsented)
        val newCalleeConsent = if (!current.isCaller) true else (current.calleeConsent || isSingleDeviceOrBothConsented)
        val bothNowConsented = newCallerConsent && newCalleeConsent
        val nextConsentState = if (bothNowConsented) "all_consented" else "requested"

        if (bothNowConsented && !isCallMicRecordingActive) {
            isCallMicRecordingActive = audioController.startMicrophoneRecording("call")
        }

        _activeCallState.update {
            it.copy(
                callerConsent = newCallerConsent,
                calleeConsent = newCalleeConsent,
                consentState = nextConsentState,
                isRecording = bothNowConsented,
                showConsentDialog = false
            )
        }

        if (current.callId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    repository.updateCallConsentAndRecording(
                        callId = current.callId,
                        consentState = nextConsentState,
                        callerConsent = newCallerConsent,
                        calleeConsent = newCalleeConsent,
                        isRecording = bothNowConsented,
                        durationSec = current.elapsedSeconds
                    )
                    if (bothNowConsented) {
                        showBanner("All participants consented. Hardware call audio recording active.")
                    } else {
                        showBanner("Your recording consent is registered. Waiting for ${current.participantName} to consent.")
                    }
                } catch (e: Exception) {
                    showBanner("Failed to update consent state: ${e.localizedMessage}")
                }
            }
        }
    }

    fun withdrawConsentAndStopRecording() {
        val current = _activeCallState.value
        if (isCallMicRecordingActive) {
            isCallMicRecordingActive = false
            audioController.stopMicrophoneRecordingAndDiscard()
        }
        _activeCallState.update {
            it.copy(
                callerConsent = false,
                calleeConsent = false,
                consentState = "withdrawn",
                isRecording = false,
                showConsentDialog = false
            )
        }
        if (current.callId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    repository.updateCallConsentAndRecording(
                        callId = current.callId,
                        consentState = "withdrawn",
                        callerConsent = false,
                        calleeConsent = false,
                        isRecording = false,
                        durationSec = current.elapsedSeconds
                    )
                    showBanner("Consent withdrawn. Call recording stopped immediately.")
                } catch (e: Exception) {
                    showBanner("Failed to stop recording: ${e.localizedMessage}")
                }
            }
        }
    }

    fun endActiveCall() {
        callTimerJob?.cancel()
        callSessionObserverJob?.cancel()
        audioController.stopLiveCallAudioTransceiver()
        audioController.stopRingingIncomingCall()
        audioController.configureCallAudioRouting(active = false, isMuted = false, isSpeakerOn = false)

        val current = _activeCallState.value
        val wasConsented = current.consentState == "all_consented" &&
            (current.isRecording || current.recordingElapsedSeconds > 0L || isCallMicRecordingActive)

        val recordedResult = if (isCallMicRecordingActive) {
            isCallMicRecordingActive = false
            audioController.stopMicrophoneRecording(current.callId.ifBlank { "global" })
        } else {
            null
        }

        val recordedSecs = recordedResult?.durationSec
            ?: if (current.recordingElapsedSeconds > 0L) current.recordingElapsedSeconds else current.elapsedSeconds

        _activeCallState.update { it.copy(status = "ended", isRecording = false) }
        _currentDestination.value = _previousTabDestination.value

        if (current.callId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    repository.endCallSession(
                        callId = current.callId,
                        participantName = current.participantName,
                        callType = current.callType,
                        durationSec = recordedSecs.coerceAtLeast(1L),
                        wasConsentedRecording = wasConsented,
                        recordedAudioBase64 = recordedResult?.encryptedBase64.orEmpty(),
                        recordedLocalFilePath = recordedResult?.localFilePath.orEmpty(),
                        recordedSizeBytes = recordedResult?.sizeBytes ?: (recordedSecs.coerceAtLeast(1L) * 4000L)
                    )
                    if (wasConsented) {
                        showBanner("Encrypted call ended. Consented audio saved to Storage Manager.")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error ending call session", e)
                }
            }
        }
    }

    // --- REAL HARDWARE MICROPHONE VOICE RECORDING & PLAYBACK ---

    fun startHardwareVoiceRecording(prefix: String = "voice"): Boolean {
        val started = audioController.startMicrophoneRecording(prefix)
        _isHardwareRecordingVoice.value = started
        if (started) {
            micAmplitudeJob?.cancel()
            micAmplitudeJob = viewModelScope.launch {
                while (_isHardwareRecordingVoice.value) {
                    _liveMicAmplitude.value = audioController.getLiveAmplitudeNormalized()
                    delay(120L)
                }
            }
        }
        return started
    }

    fun cancelHardwareVoiceRecording() {
        micAmplitudeJob?.cancel()
        _isHardwareRecordingVoice.value = false
        _liveMicAmplitude.value = 0.18f
        audioController.stopMicrophoneRecordingAndDiscard()
    }

    fun stopAndSaveStandaloneVoiceRecording(title: String, fallbackDurationSec: Long = 1L) {
        micAmplitudeJob?.cancel()
        _isHardwareRecordingVoice.value = false
        _liveMicAmplitude.value = 0.18f

        val recorded = audioController.stopMicrophoneRecording("global")
        val profile = (userProfileState.value as? UiState.Success)?.data
        val ownerName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }
        val duration = recorded?.durationSec ?: fallbackDurationSec.coerceAtLeast(1L)
        val sizeBytes = recorded?.sizeBytes ?: (duration * 4000L)

        viewModelScope.launch {
            try {
                repository.createRecordingMetadata(
                    callId = "voice_${System.currentTimeMillis()}",
                    memberUids = listOf(currentUserId),
                    title = title.ifBlank { "Consented Audio Note" },
                    participantName = ownerName,
                    mediaType = "audio",
                    durationSec = duration,
                    sizeBytes = sizeBytes,
                    audioBase64 = recorded?.encryptedBase64.orEmpty(),
                    localFilePath = recorded?.localFilePath.orEmpty()
                )
                showBanner("Encrypted hardware audio recording saved (${duration}s).")
            } catch (e: Exception) {
                showBanner("Failed to save audio recording: ${e.localizedMessage}")
            }
        }
    }

    fun stopAndSendChatVoiceMessage(fallbackDurationSec: Long = 1L) {
        micAmplitudeJob?.cancel()
        _isHardwareRecordingVoice.value = false
        _liveMicAmplitude.value = 0.18f

        val convId = _selectedConversationId.value ?: return
        val recorded = audioController.stopMicrophoneRecording(convId)
        val duration = recorded?.durationSec ?: fallbackDurationSec.coerceAtLeast(1L)
        val profile = (userProfileState.value as? UiState.Success)?.data
        val senderName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }

        viewModelScope.launch {
            try {
                repository.sendMessage(
                    conversationId = convId,
                    senderName = senderName,
                    text = "Encrypted Voice Message (${duration}s)",
                    messageType = "audio",
                    mediaBase64 = recorded?.encryptedBase64.orEmpty(),
                    mediaDurationSec = duration,
                    mediaLabel = ""
                )
            } catch (e: Exception) {
                showBanner("Voice note failed to send: ${e.localizedMessage}")
            }
        }
    }

    fun togglePlayMedia(
        itemId: String,
        localFilePath: String,
        encryptedBase64: String,
        channelId: String = "global"
    ) {
        if (_playingMediaId.value == itemId) {
            audioController.stopPlayback()
            _playingMediaId.value = null
            return
        }
        val started = audioController.playAudio(
            itemId = itemId,
            localFilePath = localFilePath,
            encryptedBase64 = encryptedBase64,
            channelId = channelId,
            onCompletion = {
                _playingMediaId.value = null
            }
        )
        if (started) {
            _playingMediaId.value = itemId
        } else {
            _playingMediaId.value = null
            showBanner("No recorded audio stream found on device for this item.")
        }
    }

    // --- HD PHOTO, HD VIDEO, FILE ATTACHMENTS & PERMANENT MESSAGE DELETION ---

    fun sendChatMessage(
        text: String,
        messageType: String = "text",
        mediaDurationSec: Long = 0L,
        mediaLabel: String = "",
        mediaBase64: String = ""
    ) {
        val convId = _selectedConversationId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data
        val senderName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }
        viewModelScope.launch {
            try {
                repository.sendMessage(
                    conversationId = convId,
                    senderName = senderName,
                    text = text,
                    messageType = messageType,
                    mediaBase64 = mediaBase64,
                    mediaDurationSec = mediaDurationSec,
                    mediaLabel = mediaLabel
                )
            } catch (e: Exception) {
                showBanner("Message failed to send: ${e.localizedMessage}")
            }
        }
    }

    fun sendImageAttachment(uri: Uri) {
        val convId = _selectedConversationId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data
        val senderName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }

        viewModelScope.launch {
            try {
                val encodedJpeg = withContext(Dispatchers.IO) {
                    audioController.encodePickedImageUri(uri, convId, highDefinition = true)
                }
                if (encodedJpeg.isNullOrBlank()) {
                    showBanner("Unable to read selected HD image.")
                    return@launch
                }
                repository.sendMessage(
                    conversationId = convId,
                    senderName = senderName,
                    text = "HD Encrypted Photo",
                    messageType = "image",
                    mediaBase64 = encodedJpeg,
                    mediaDurationSec = 0L,
                    mediaLabel = ""
                )
            } catch (e: Exception) {
                showBanner("Failed to send HD photo: ${e.localizedMessage}")
            }
        }
    }

    fun sendVideoAttachment(uri: Uri) {
        val convId = _selectedConversationId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data
        val senderName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }

        viewModelScope.launch {
            try {
                val videoMeta = withContext(Dispatchers.IO) {
                    audioController.encodePickedVideoUri(uri)
                }
                if (videoMeta == null) {
                    showBanner("Unable to process selected HD video.")
                    return@launch
                }
                repository.sendMessage(
                    conversationId = convId,
                    senderName = senderName,
                    text = "HD Video • ${videoMeta.displayTitle}",
                    messageType = "video",
                    mediaBase64 = videoMeta.thumbnailBase64,
                    mediaDurationSec = videoMeta.durationSec,
                    mediaLabel = ""
                )
            } catch (e: Exception) {
                showBanner("Failed to send HD video: ${e.localizedMessage}")
            }
        }
    }

    fun sendDocumentAttachment(uri: Uri) {
        val convId = _selectedConversationId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data
        val senderName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }

        viewModelScope.launch {
            try {
                val docMeta = withContext(Dispatchers.IO) {
                    audioController.encodePickedDocumentUri(uri)
                }
                if (docMeta == null) {
                    showBanner("Unable to read selected file.")
                    return@launch
                }
                val kb = (docMeta.sizeBytes / 1024L).coerceAtLeast(1L)
                val msgType = if (docMeta.previewBase64.isNotBlank()) "image" else "text"
                repository.sendMessage(
                    conversationId = convId,
                    senderName = senderName,
                    text = "\uD83D\uDCC4 ${docMeta.fileName} (${kb} KB • AES-256-GCM)",
                    messageType = msgType,
                    mediaBase64 = docMeta.previewBase64,
                    mediaDurationSec = 0L,
                    mediaLabel = ""
                )
            } catch (e: Exception) {
                showBanner("Failed to send file: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Permanently deletes a voice message, text message, or HD photo/video from Firestore
     * so it is removed for all conversation participants in real time.
     */
    fun deleteMessagePermanently(message: ChatMessage) {
        val convId = message.conversationId.ifBlank { _selectedConversationId.value.orEmpty() }
        if (convId.isBlank() || message.id.isBlank()) return
        if (_playingMediaId.value == message.id) {
            audioController.stopPlayback()
            _playingMediaId.value = null
        }
        viewModelScope.launch {
            try {
                repository.deleteMessagePermanently(convId, message.id)
                showBanner("Message permanently deleted for all participants.")
            } catch (e: Exception) {
                showBanner("Failed to delete message: ${e.localizedMessage}")
            }
        }
    }

    // --- MULTI-USER CONTACTS, PRESENCE & SETTINGS ---

    fun addTrustedContact(
        name: String,
        handle: String,
        targetUid: String = "",
        avatarKey: String = "custom"
    ) {
        val myProfile = (userProfileState.value as? UiState.Success)?.data
        val myName = myProfile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName.ifBlank { "SecretGuard User" }
        val myHandle = myProfile?.userHandle.orEmpty()

        viewModelScope.launch {
            try {
                repository.addTrustedContact(
                    contactName = name,
                    contactHandle = handle,
                    targetUid = targetUid,
                    requesterName = myName,
                    requesterHandle = myHandle,
                    avatarKey = avatarKey
                )
                showBanner("Connected with $name. Real-time encrypted channel ready.")
            } catch (e: Exception) {
                showBanner("Failed to add contact: ${e.localizedMessage}")
            }
        }
    }

    fun toggleBlockContact(contactId: String, currentlyBlocked: Boolean) {
        viewModelScope.launch {
            try {
                repository.setContactBlockStatus(contactId, !currentlyBlocked)
                showBanner(if (!currentlyBlocked) "Contact blocked." else "Contact unblocked.")
            } catch (e: Exception) {
                showBanner("Failed to update contact status: ${e.localizedMessage}")
            }
        }
    }

    fun toggleOnlinePresence() {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val currentlyOnline = profile?.onlineStatus != "offline"
        viewModelScope.launch {
            repository.updateOnlinePresence(!currentlyOnline)
        }
    }

    fun setPresenceOnline(isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateOnlinePresence(isOnline)
        }
    }

    fun updateUserSettings(
        displayName: String? = null,
        profileVisibility: String? = null,
        appLockEnabled: Boolean? = null,
        autoRecordCalls: Boolean? = null,
        notificationAlert: Boolean? = null,
        recordingQuality: String? = null,
        retentionDays: Long? = null
    ) {
        val currentProfile = (userProfileState.value as? UiState.Success)?.data ?: UserProfile(
            uid = currentUserId,
            displayName = initialDisplayName
        )
        val currentPrivate = (privateSettingsState.value as? UiState.Success)?.data ?: UserPrivateSettings(
            uid = currentUserId,
            email = initialEmail
        )
        viewModelScope.launch {
            try {
                repository.updateUserSettings(
                    displayName = displayName ?: currentProfile.displayName,
                    profileVisibility = profileVisibility ?: currentProfile.profileVisibility,
                    appLockEnabled = appLockEnabled ?: currentPrivate.appLockEnabled,
                    autoRecordCalls = autoRecordCalls ?: currentPrivate.autoRecordCalls,
                    notificationAlert = notificationAlert ?: currentPrivate.notificationAlert,
                    recordingQuality = recordingQuality ?: currentPrivate.recordingQuality,
                    retentionDays = retentionDays ?: currentPrivate.retentionDays
                )
            } catch (e: Exception) {
                showBanner("Failed to save settings: ${e.localizedMessage}")
            }
        }
    }

    fun deleteRecording(recordingId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRecording(recordingId)
                showBanner("Recording permanently deleted from Firestore & storage.")
            } catch (e: Exception) {
                showBanner("Failed to delete recording: ${e.localizedMessage}")
            }
        }
    }

    fun submitSafetyReport(targetName: String, category: String, details: String) {
        viewModelScope.launch {
            try {
                repository.submitSecurityReport(targetName, category, details)
                showBanner("Safety report submitted for review.")
            } catch (e: Exception) {
                showBanner("Failed to submit report: ${e.localizedMessage}")
            }
        }
    }

    fun deleteAccountAndData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAllUserData()
                onComplete()
            } catch (e: Exception) {
                showBanner("Failed to delete account data: ${e.localizedMessage}")
            }
        }
    }

    // --- KEYSTORE SECRET CODE PIN & NATIVE BIOMETRIC OPERATIONS ---

    fun getCurrentSecretCodeDisplay(): String {
        val raw = cryptoManager.getDisplayPinForOwnerPreview()
        return if (_isPinRevealed.value) {
            raw.toCharArray().joinToString("  ")
        } else {
            List(raw.length.coerceIn(5, 8)) { "\u25CF" }.joinToString("   ")
        }
    }

    fun togglePinVisibility() {
        _isPinRevealed.update { !it }
    }

    fun lockAppNow() {
        _isAppLockRequired.value = true
        _pinFeedbackMessage.value = null
    }

    fun unlockAppWithPin(candidatePin: String) {
        when (val result = cryptoManager.verifyPin(candidatePin)) {
            is PinVerifyResult.Success -> {
                _isAppLockRequired.value = false
                _pinFeedbackMessage.value = null
            }
            is PinVerifyResult.Failed -> {
                _pinFeedbackMessage.value =
                    "Incorrect Secret Code (${result.attempts}/${KeystoreCryptoManager.MAX_ATTEMPTS_BEFORE_THROTTLE} attempts before lockout)."
            }
            is PinVerifyResult.Throttled -> {
                _pinFeedbackMessage.value =
                    "Attempt throttling active. Try again in ${result.remainingSeconds}s."
            }
            is PinVerifyResult.InvalidFormat -> {
                _pinFeedbackMessage.value = result.reason
            }
        }
    }

    fun unlockAppWithBiometricOrAccountRecovery() {
        _isAppLockRequired.value = false
        _pinFeedbackMessage.value = null
        showBanner("Unlocked via native Android Biometric / Verified Session.")
    }

    fun setPinFeedbackMessage(message: String?) {
        _pinFeedbackMessage.value = message
    }

    fun changeSecretCode(currentPin: String, newPin: String): Boolean {
        return when (val result = cryptoManager.changePin(currentPin, newPin)) {
            is PinChangeResult.Success -> {
                showBanner("Secret Code updated in Android Keystore (AES-256-GCM).")
                true
            }
            is PinChangeResult.Error -> {
                showBanner(result.message)
                false
            }
        }
    }

    fun resetSecretCodeViaAuthenticatedRecovery(newPin: String): Boolean {
        return when (val result = cryptoManager.resetPinViaAuthenticatedAccount(newPin)) {
            is PinChangeResult.Success -> {
                showBanner("Secret Code recovered and re-encrypted via authenticated Google session.")
                true
            }
            is PinChangeResult.Error -> {
                showBanner(result.message)
                false
            }
        }
    }

    fun decryptMessagePreview(
        ciphertextEnvelope: String,
        fallbackText: String,
        channelId: String = "global"
    ): String {
        return cryptoManager.decryptMessageEnvelope(ciphertextEnvelope, fallbackText, channelId)
    }

    fun showBanner(message: String?) {
        _statusBannerMessage.value = message
    }

    fun clearBanner() {
        _statusBannerMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        callTimerJob?.cancel()
        callSessionObserverJob?.cancel()
        micAmplitudeJob?.cancel()
        audioController.releaseAll()
    }

    companion object {
        const val TAG = "SecretGuardVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val KEY_PASTEL_THEME = "selected_pastel_theme"
        const val KEY_APP_FONT = "selected_app_font"
        const val KEY_COMPACT_MODE = "compact_mode_enabled"
        const val KEY_RINGTONE_ID = "selected_ringtone_id"
        const val KEY_CUSTOM_RINGTONE_URI = "custom_ringtone_uri"
        const val KEY_CUSTOM_RINGTONE_LABEL = "custom_ringtone_label"
        const val KEY_PENDING_AUTH_NAME = "pending_auth_name"
        const val KEY_PENDING_AUTH_HANDLE = "pending_auth_handle"
        const val KEY_PENDING_AUTH_GENDER = "pending_auth_gender"
        const val KEY_PENDING_AUTH_AGE = "pending_auth_age"
        const val KEY_PENDING_AUTH_VISIBILITY = "pending_auth_visibility"

        fun savePendingOnboardingPersona(
            context: Context,
            displayName: String,
            userHandle: String,
            gender: String,
            age: Long,
            visibility: String
        ) {
            context.applicationContext
                .getSharedPreferences("secretguard_ui_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PENDING_AUTH_NAME, displayName.trim())
                .putString(KEY_PENDING_AUTH_HANDLE, userHandle.trim())
                .putString(KEY_PENDING_AUTH_GENDER, gender)
                .putLong(KEY_PENDING_AUTH_AGE, age.coerceIn(0L, 120L))
                .putString(KEY_PENDING_AUTH_VISIBILITY, visibility)
                .apply()
        }
    }
}
