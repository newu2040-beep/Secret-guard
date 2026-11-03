package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.security.KeystoreCryptoManager
import com.example.core.security.PinChangeResult
import com.example.core.security.PinVerifyResult
import com.example.data.model.CallRecording
import com.example.data.model.CallSession
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.TrustedContact
import com.example.data.model.UserProfile
import com.example.data.repository.SecretGuardRepository
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
    val participantName: String = "Priya Sharma",
    val participantAvatarKey: String = "priya",
    val callType: String = "video",
    val connectionState: String = "Connected • WebRTC Encrypted",
    val elapsedSeconds: Long = 754L, // Starts at 00:12:34 on initial preview or 0 on new call
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCameraOn: Boolean = true,
    val isFrontCamera: Boolean = true,
    val consentState: String = "none", // "none", "requested", "all_consented", "withdrawn"
    val isRecording: Boolean = false,
    val recordingElapsedSeconds: Long = 0L,
    val showConsentDialog: Boolean = false
)

class SecretGuardViewModel(
    private val repository: SecretGuardRepository,
    private val cryptoManager: KeystoreCryptoManager,
    private val currentUserId: String,
    private val initialDisplayName: String = "Secret User",
    private val initialEmail: String = "",
    private val initialPhotoUrl: String = ""
) : ViewModel() {

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

    private var callTimerJob: Job? = null

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
                val priyaConv = list.firstOrNull { it.participantName.contains("Priya", ignoreCase = true) }
                _selectedConversationId.value = (priyaConv ?: list.first()).id
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
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
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
                repository.ensureWorkspaceInitialized(
                    displayName = initialDisplayName,
                    email = initialEmail,
                    photoUrl = initialPhotoUrl
                )
            } catch (e: Exception) {
                Log.w(TAG, "Workspace init warning", e)
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
    }

    fun openConversationByParticipant(participantName: String, avatarKey: String = "priya") {
        val existingList = (conversationsState.value as? UiState.Success)?.data.orEmpty()
        val match = existingList.firstOrNull {
            it.participantName.equals(participantName, ignoreCase = true)
        }
        if (match != null) {
            openConversation(match.id)
        } else {
            viewModelScope.launch {
                try {
                    val newId = repository.createConversationWithId(
                        participantName = participantName,
                        participantAvatarKey = avatarKey,
                        lastMessageText = "Encrypted session initialized."
                    )
                    openConversation(newId)
                } catch (e: Exception) {
                    showBanner("Failed to open conversation: ${e.localizedMessage}")
                }
            }
        }
    }

    fun startCall(
        participantName: String = "Priya Sharma",
        participantAvatarKey: String = "priya",
        callType: String = "video",
        initialDisplayElapsed: Long = 754L
    ) {
        viewModelScope.launch {
            try {
                val callId = repository.startCallSession(
                    participantName = participantName,
                    participantAvatarKey = participantAvatarKey,
                    callType = callType
                )
                _activeCallState.value = ActiveCallUiState(
                    callId = callId,
                    participantName = participantName,
                    participantAvatarKey = participantAvatarKey,
                    callType = callType,
                    connectionState = "Connected • AES-256-GCM",
                    elapsedSeconds = initialDisplayElapsed,
                    isMuted = false,
                    isSpeakerOn = true,
                    isCameraOn = callType == "video",
                    consentState = "none",
                    isRecording = false,
                    recordingElapsedSeconds = 0L,
                    showConsentDialog = false
                )
                _currentDestination.value = MainDestination.ACTIVE_CALL
                startCallTimer()
            } catch (e: Exception) {
                showBanner("Unable to start call: ${e.localizedMessage}")
            }
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _activeCallState.update { state ->
                    state.copy(
                        elapsedSeconds = state.elapsedSeconds + 1L,
                        recordingElapsedSeconds = if (state.isRecording) state.recordingElapsedSeconds + 1L else state.recordingElapsedSeconds
                    )
                }
            }
        }
    }

    fun toggleCallMute() {
        _activeCallState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleCallSpeaker() {
        _activeCallState.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleCallCamera() {
        _activeCallState.update { it.copy(isCameraOn = !it.isCameraOn) }
    }

    fun switchCameraLens() {
        _activeCallState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun promptRecordingConsentDialog(show: Boolean) {
        _activeCallState.update { it.copy(showConsentDialog = show) }
    }

    fun confirmAllParticipantsConsentAndRecord() {
        val current = _activeCallState.value
        _activeCallState.update {
            it.copy(
                consentState = "all_consented",
                isRecording = true,
                showConsentDialog = false
            )
        }
        if (current.callId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    repository.updateCallConsentAndRecording(
                        callId = current.callId,
                        consentState = "all_consented",
                        isRecording = true,
                        durationSec = current.elapsedSeconds
                    )
                    showBanner("All participants consented. Persistent recording indicator active.")
                } catch (e: Exception) {
                    showBanner("Failed to update consent state: ${e.localizedMessage}")
                }
            }
        }
    }

    fun withdrawConsentAndStopRecording() {
        val current = _activeCallState.value
        _activeCallState.update {
            it.copy(
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
        val current = _activeCallState.value
        val wasConsented = current.consentState == "all_consented" &&
            (current.isRecording || current.recordingElapsedSeconds > 0L)
        val recordedSecs = if (current.recordingElapsedSeconds > 0L) {
            current.recordingElapsedSeconds
        } else {
            current.elapsedSeconds
        }
        _currentDestination.value = _previousTabDestination.value

        if (current.callId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    repository.endCallSession(
                        callId = current.callId,
                        participantName = current.participantName,
                        callType = current.callType,
                        durationSec = recordedSecs.coerceAtLeast(1L),
                        wasConsentedRecording = wasConsented
                    )
                    if (wasConsented) {
                        showBanner("Encrypted call ended. Consented recording saved to Storage Manager.")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error ending call session", e)
                }
            }
        }
    }

    fun sendChatMessage(
        text: String,
        messageType: String = "text",
        mediaDurationSec: Long = 0L,
        mediaLabel: String = ""
    ) {
        val convId = _selectedConversationId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data
        val senderName = profile?.displayName ?: initialDisplayName
        viewModelScope.launch {
            try {
                repository.sendMessage(
                    conversationId = convId,
                    senderName = senderName,
                    text = text,
                    messageType = messageType,
                    mediaDurationSec = mediaDurationSec,
                    mediaLabel = mediaLabel
                )
            } catch (e: Exception) {
                showBanner("Message failed to send: ${e.localizedMessage}")
            }
        }
    }

    fun saveConsentedVoiceRecording(title: String, durationSec: Long) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val ownerName = profile?.displayName ?: initialDisplayName
        viewModelScope.launch {
            try {
                repository.createRecordingMetadata(
                    callId = "voice_${System.currentTimeMillis()}",
                    title = title.ifBlank { "Consented Audio Note" },
                    participantName = ownerName,
                    mediaType = "audio",
                    durationSec = durationSec.coerceAtLeast(1L),
                    sizeBytes = durationSec.coerceAtLeast(1L) * 32_000L
                )
                showBanner("Encrypted audio recording saved to Storage Manager.")
            } catch (e: Exception) {
                showBanner("Failed to save audio recording: ${e.localizedMessage}")
            }
        }
    }

    fun addTrustedContact(name: String, handle: String, avatarKey: String = "custom") {
        viewModelScope.launch {
            try {
                repository.addTrustedContact(name, handle, avatarKey)
                showBanner("Added $name to Trusted Contacts.")
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

    fun updateUserSettings(
        displayName: String? = null,
        profileVisibility: String? = null,
        appLockEnabled: Boolean? = null,
        autoRecordCalls: Boolean? = null,
        notificationAlert: Boolean? = null,
        recordingQuality: String? = null,
        retentionDays: Long? = null
    ) {
        val current = (userProfileState.value as? UiState.Success)?.data ?: UserProfile(
            uid = currentUserId,
            displayName = initialDisplayName,
            email = initialEmail
        )
        viewModelScope.launch {
            try {
                repository.updateUserSettings(
                    displayName = displayName ?: current.displayName,
                    profileVisibility = profileVisibility ?: current.profileVisibility,
                    appLockEnabled = appLockEnabled ?: current.appLockEnabled,
                    autoRecordCalls = autoRecordCalls ?: current.autoRecordCalls,
                    notificationAlert = notificationAlert ?: current.notificationAlert,
                    recordingQuality = recordingQuality ?: current.recordingQuality,
                    retentionDays = retentionDays ?: current.retentionDays
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
                showBanner("Recording deleted from Firestore & storage.")
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

    // --- KEYSTORE SECRET CODE PIN OPERATIONS ---

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
        showBanner("Verified via authenticated session.")
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

    fun decryptMessagePreview(ciphertextEnvelope: String, fallbackText: String): String {
        return cryptoManager.decryptMessageEnvelope(ciphertextEnvelope, fallbackText)
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
    }

    private companion object {
        const val TAG = "SecretGuardVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
