package com.example.core.media

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.example.core.security.KeystoreCryptoManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RecordedAudioResult(
    val localFilePath: String,
    val rawBytes: ByteArray,
    val encryptedBase64: String,
    val durationSec: Long,
    val sizeBytes: Long
)

data class EncodedVideoAttachment(
    val thumbnailBase64: String,
    val durationSec: Long,
    val displayTitle: String,
    val sizeBytes: Long
)

data class EncodedFileAttachment(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val previewBase64: String
)

enum class BuiltInRingtone(val id: String, val title: String, val subtitle: String) {
    EMERALD_PULSE("emerald_pulse", "Emerald Pulse", "Warm harmonic privacy chime"),
    CRYSTAL_BELL("crystal_bell", "Crystal Bell", "Clear resonant acoustic bell"),
    NORDIC_BREEZE("nordic_breeze", "Nordic Breeze", "Soft pastel ascending arpeggio"),
    CLASSIC_PHONE("classic_phone", "System Default Ringtone", "Device native phone ringtone"),
    SILENT_VIBRATE("silent_vibrate", "Silent Vibrate Only", "Haptic pulse without audio")
}

/**
 * Real hardware audio recorder (`MediaRecorder`), live two-way call voice transceiver
 * (`AudioRecord` + `AudioTrack` with μ-law compression & echo cancellation), audio player
 * (`MediaPlayer`), ringtone controller, and HD image/video/file attachment processor.
 */
class AudioMediaController(
    private val context: Context?,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager(context)
) {
    private var mediaRecorder: MediaRecorder? = null
    private var activeRecordingFile: File? = null
    private var recordingStartMillis: Long = 0L

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingId: String? = null

    // Real-time two-way call voice streaming state
    private val transceiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var liveVoiceCaptureJob: Job? = null
    private var liveAudioRecord: AudioRecord? = null
    private var liveAudioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private val isLiveCallAudioRunning = AtomicBoolean(false)
    private val isLiveCallMicMuted = AtomicBoolean(false)
    private val outboundAudioSequence = AtomicLong(0L)
    private val lastPlayedRemoteSequence = AtomicLong(0L)

    // Ringtone state
    private var activeRingtoneJob: Job? = null
    private var activeSystemRingtone: Ringtone? = null
    private var activeCustomRingtonePlayer: MediaPlayer? = null

    /**
     * Starts real-time two-way voice capture and playback for an active audio or video call.
     * Captures 8 kHz 16-bit mono PCM from `VOICE_COMMUNICATION`, applies hardware echo cancellation,
     * encodes 350ms packets into compact G.711 μ-law Base64, and emits via [onAudioPacketReady].
     */
    @SuppressLint("MissingPermission")
    fun startLiveCallAudioTransceiver(
        isMuted: Boolean,
        onAudioPacketReady: (sequence: Long, ulawBase64: String) -> Unit
    ) {
        if (context == null) return
        isLiveCallMicMuted.set(isMuted)
        if (isLiveCallAudioRunning.getAndSet(true)) {
            return
        }

        lastPlayedRemoteSequence.set(0L)
        outboundAudioSequence.set(System.currentTimeMillis() / 100L)

        // Initialize low-latency AudioTrack for incoming remote voice playback
        try {
            val minOutBuf = AudioTrack.getMinBufferSize(
                CALL_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(CALL_PACKET_SAMPLES * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(CALL_SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minOutBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
            liveAudioTrack = track
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize live call AudioTrack: ${e.message}")
        }

        // Start coroutine capturing real microphone voice packets via AudioRecord
        liveVoiceCaptureJob?.cancel()
        liveVoiceCaptureJob = transceiverScope.launch {
            try {
                val minInBuf = AudioRecord.getMinBufferSize(
                    CALL_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(CALL_PACKET_SAMPLES * 2)

                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    CALL_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minInBuf * 2
                )

                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    return@launch
                }

                val sessionId = recorder.audioSessionId
                if (AcousticEchoCanceler.isAvailable()) {
                    runCatching {
                        echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
                    }
                }
                if (NoiseSuppressor.isAvailable()) {
                    runCatching {
                        noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
                    }
                }

                liveAudioRecord = recorder
                recorder.startRecording()

                val pcmBuffer = ShortArray(CALL_PACKET_SAMPLES)
                val ulawBuffer = ByteArray(CALL_PACKET_SAMPLES)

                while (isActive && isLiveCallAudioRunning.get()) {
                    val readCount = recorder.read(pcmBuffer, 0, pcmBuffer.size)
                    if (readCount > 0 && !isLiveCallMicMuted.get()) {
                        var peak = 0
                        for (i in 0 until readCount) {
                            val sample = pcmBuffer[i].toInt()
                            val absVal = abs(sample)
                            if (absVal > peak) peak = absVal
                            ulawBuffer[i] = linearPcm16ToUlaw(sample)
                        }
                        // Transmit voice packet when voice energy is detected above ambient silence floor
                        if (peak > 160) {
                            val encoded = Base64.encodeToString(ulawBuffer, 0, readCount, Base64.NO_WRAP)
                            val seq = outboundAudioSequence.incrementAndGet()
                            onAudioPacketReady(seq, encoded)
                        }
                    } else {
                        delay(80L)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Live call AudioRecord loop warning: ${e.message}")
            } finally {
                releaseLiveAudioCaptureResources()
            }
        }
    }

    fun setLiveCallMicMuted(muted: Boolean) {
        isLiveCallMicMuted.set(muted)
    }

    /**
     * Decodes an incoming G.711 μ-law Base64 voice packet from a remote call participant
     * and writes it immediately to the active `AudioTrack` so both participants hear each other.
     */
    fun playIncomingCallAudioChunk(sequence: Long, ulawBase64: String) {
        if (ulawBase64.isBlank() || sequence <= 0L) return
        val prevSeq = lastPlayedRemoteSequence.get()
        if (sequence <= prevSeq) return
        lastPlayedRemoteSequence.set(sequence)

        val track = liveAudioTrack ?: return
        transceiverScope.launch {
            runCatching {
                val ulawBytes = Base64.decode(ulawBase64, Base64.NO_WRAP)
                if (ulawBytes.isEmpty()) return@runCatching
                val pcmShorts = ShortArray(ulawBytes.size)
                for (i in ulawBytes.indices) {
                    pcmShorts[i] = ulawToLinearPcm16(ulawBytes[i])
                }
                track.write(pcmShorts, 0, pcmShorts.size)
            }
        }
    }

    fun stopLiveCallAudioTransceiver() {
        isLiveCallAudioRunning.set(false)
        liveVoiceCaptureJob?.cancel()
        liveVoiceCaptureJob = null
        releaseLiveAudioCaptureResources()
        val track = liveAudioTrack
        liveAudioTrack = null
        if (track != null) {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    private fun releaseLiveAudioCaptureResources() {
        runCatching { echoCanceler?.release() }
        echoCanceler = null
        runCatching { noiseSuppressor?.release() }
        noiseSuppressor = null
        val rec = liveAudioRecord
        liveAudioRecord = null
        if (rec != null) {
            runCatching {
                if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    rec.stop()
                }
            }
            runCatching { rec.release() }
        }
    }

    // --- BUILT-IN & CUSTOM RINGTONE ENGINE ---

    fun startRingingIncomingCall(ringtoneId: String, customRingtoneUri: String = "") {
        stopRingingIncomingCall()
        val ctx = context?.applicationContext ?: return

        // Trigger gentle haptic vibration pattern if Vibrator is available
        triggerIncomingCallVibration(ctx)

        if (ringtoneId == BuiltInRingtone.SILENT_VIBRATE.id) {
            return
        }

        if (customRingtoneUri.isNotBlank()) {
            val startedCustom = runCatching {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(ctx, Uri.parse(customRingtoneUri))
                    isLooping = true
                    prepare()
                    start()
                }
                activeCustomRingtonePlayer = player
                true
            }.getOrDefault(false)
            if (startedCustom) return
        }

        if (ringtoneId == BuiltInRingtone.CLASSIC_PHONE.id) {
            val startedDefault = runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val ringtone = RingtoneManager.getRingtone(ctx, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone?.isLooping = true
                }
                ringtone?.play()
                activeSystemRingtone = ringtone
                ringtone != null
            }.getOrDefault(false)
            if (startedDefault) return
        }

        // Synthesize melodic built-in pastel chime loop via AudioTrack
        activeRingtoneJob = transceiverScope.launch {
            while (isActive) {
                playSynthesizedChimeOnce(ringtoneId)
                delay(1200L)
            }
        }
    }

    fun previewRingtone(ringtoneId: String, customRingtoneUri: String = "") {
        stopRingingIncomingCall()
        val ctx = context?.applicationContext ?: return
        if (ringtoneId == BuiltInRingtone.SILENT_VIBRATE.id) {
            triggerIncomingCallVibration(ctx)
            return
        }
        if (customRingtoneUri.isNotBlank()) {
            val ok = runCatching {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(ctx, Uri.parse(customRingtoneUri))
                    isLooping = false
                    prepare()
                    start()
                }
                activeCustomRingtonePlayer = player
                true
            }.getOrDefault(false)
            if (ok) {
                activeRingtoneJob = transceiverScope.launch {
                    delay(4000L)
                    stopRingingIncomingCall()
                }
                return
            }
        }
        if (ringtoneId == BuiltInRingtone.CLASSIC_PHONE.id) {
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val ringtone = RingtoneManager.getRingtone(ctx, uri)
                ringtone?.play()
                activeSystemRingtone = ringtone
            }
            activeRingtoneJob = transceiverScope.launch {
                delay(3500L)
                stopRingingIncomingCall()
            }
            return
        }
        activeRingtoneJob = transceiverScope.launch {
            playSynthesizedChimeOnce(ringtoneId)
        }
    }

    fun stopRingingIncomingCall() {
        activeRingtoneJob?.cancel()
        activeRingtoneJob = null
        runCatching { activeSystemRingtone?.stop() }
        activeSystemRingtone = null
        val customPlayer = activeCustomRingtonePlayer
        activeCustomRingtonePlayer = null
        if (customPlayer != null) {
            runCatching {
                if (customPlayer.isPlaying) customPlayer.stop()
            }
            runCatching { customPlayer.release() }
        }
        val ctx = context?.applicationContext
        if (ctx != null) {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.cancel()
            }
        }
    }

    private fun triggerIncomingCallVibration(ctx: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0L, 350L, 250L, 350L), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(600L)
                }
            }
        }
    }

    private fun playSynthesizedChimeOnce(ringtoneId: String) {
        runCatching {
            val sampleRate = 16000
            val notesHz = when (ringtoneId) {
                BuiltInRingtone.CRYSTAL_BELL.id -> doubleArrayOf(587.33, 880.0, 1174.66)
                BuiltInRingtone.NORDIC_BREEZE.id -> doubleArrayOf(440.0, 554.37, 659.25, 880.0)
                else -> doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // Emerald Pulse
            }
            val noteDurationSamples = sampleRate / 5 // 200ms per note
            val totalSamples = notesHz.size * noteDurationSamples
            val pcm = ShortArray(totalSamples)

            for (n in notesHz.indices) {
                val freq = notesHz[n]
                val offset = n * noteDurationSamples
                for (i in 0 until noteDurationSamples) {
                    val env = 1.0 - (i.toDouble() / noteDurationSamples.toDouble()) * 0.75
                    val angle = 2.0 * PI * i * freq / sampleRate
                    val harmonic = 0.65 * sin(angle) + 0.35 * sin(angle * 2.0)
                    pcm[offset + i] = (harmonic * env * 14000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((notesHz.size * 210).toLong())
            track.stop()
            track.release()
        }
    }

    /**
     * Starts real microphone recording to an AAC/MPEG-4 (.m4a) file.
     */
    fun startMicrophoneRecording(prefix: String = "rec"): Boolean {
        val ctx = context?.applicationContext ?: return false
        stopMicrophoneRecordingAndDiscard()

        val recordingsDir = File(ctx.filesDir, "encrypted_recordings").apply { mkdirs() }
        val targetFile = File(recordingsDir, "${prefix}_${System.currentTimeMillis()}.m4a")

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioChannels(1)
            recorder.setAudioSamplingRate(16000)
            recorder.setAudioEncodingBitRate(32000)
            recorder.setMaxDuration(120_000) // 2 minutes max per note/segment
            recorder.setOutputFile(targetFile.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            activeRecordingFile = targetFile
            recordingStartMillis = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start real MediaRecorder", e)
            runCatching { targetFile.delete() }
            mediaRecorder = null
            activeRecordingFile = null
            false
        }
    }

    /**
     * Reads the live microphone peak amplitude normalized to [0.15f, 1.0f].
     */
    fun getLiveAmplitudeNormalized(): Float {
        val recorder = mediaRecorder ?: return 0.15f
        return try {
            val raw = recorder.maxAmplitude
            (raw / 18000f).coerceIn(0.15f, 1.0f)
        } catch (_: Exception) {
            0.20f
        }
    }

    /**
     * Stops the active `MediaRecorder`, reads the recorded `.m4a` bytes, and encrypts them
     * with AES-256-GCM for Firestore sync and local playback.
     */
    fun stopMicrophoneRecording(channelId: String = "global"): RecordedAudioResult? {
        val recorder = mediaRecorder
        val file = activeRecordingFile
        mediaRecorder = null
        activeRecordingFile = null

        val elapsedSec = ((System.currentTimeMillis() - recordingStartMillis) / 1000L).coerceAtLeast(1L)

        if (recorder != null) {
            try {
                recorder.stop()
            } catch (e: Exception) {
                Log.w(TAG, "MediaRecorder stop warning", e)
            } finally {
                runCatching { recorder.release() }
            }
        }

        if (file == null || !file.exists() || file.length() <= 0L) {
            return null
        }

        return try {
            val bytes = file.readBytes()
            val encryptedB64 = if (bytes.size <= MAX_FIRESTORE_MEDIA_BYTES) {
                cryptoManager.encryptBinaryPayload(bytes, channelId)
            } else {
                ""
            }
            RecordedAudioResult(
                localFilePath = file.absolutePath,
                rawBytes = bytes,
                encryptedBase64 = encryptedB64,
                durationSec = elapsedSec,
                sizeBytes = bytes.size.toLong()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading recorded audio file", e)
            null
        }
    }

    fun stopMicrophoneRecordingAndDiscard() {
        val recorder = mediaRecorder
        val file = activeRecordingFile
        mediaRecorder = null
        activeRecordingFile = null
        if (recorder != null) {
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
        }
        if (file != null && file.exists()) {
            runCatching { file.delete() }
        }
    }

    /**
     * Plays a real `.m4a` recording from either its local file path or its AES-256-GCM
     * encrypted Base64 payload using Android's `MediaPlayer`.
     */
    fun playAudio(
        itemId: String,
        localFilePath: String,
        encryptedBase64: String,
        channelId: String = "global",
        onCompletion: () -> Unit
    ): Boolean {
        val ctx = context?.applicationContext ?: return false
        if (currentPlayingId == itemId && mediaPlayer?.isPlaying == true) {
            stopPlayback()
            onCompletion()
            return false
        }

        stopPlayback()

        val playableFile: File? = when {
            localFilePath.isNotBlank() && File(localFilePath).exists() && File(localFilePath).length() > 0L -> {
                File(localFilePath)
            }
            encryptedBase64.isNotBlank() -> {
                val decryptedBytes = cryptoManager.decryptBinaryPayload(encryptedBase64, channelId)
                if (decryptedBytes != null && decryptedBytes.isNotEmpty()) {
                    val tempFile = File(ctx.cacheDir, "play_${itemId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")}.m4a")
                    FileOutputStream(tempFile).use { it.write(decryptedBytes) }
                    tempFile
                } else {
                    null
                }
            }
            else -> null
        }

        if (playableFile == null || !playableFile.exists()) {
            Log.w(TAG, "No playable audio file found for $itemId")
            return false
        }

        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(playableFile.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    onCompletion()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            currentPlayingId = itemId
            true
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer failed to play audio", e)
            stopPlayback()
            false
        }
    }

    fun stopPlayback() {
        val player = mediaPlayer
        mediaPlayer = null
        currentPlayingId = null
        if (player != null) {
            runCatching {
                if (player.isPlaying) player.stop()
            }
            runCatching { player.release() }
        }
    }

    /**
     * Applies real hardware call audio routing (`MODE_IN_COMMUNICATION`, mic mute, speakerphone).
     */
    fun configureCallAudioRouting(
        active: Boolean,
        isMuted: Boolean,
        isSpeakerOn: Boolean
    ) {
        isLiveCallMicMuted.set(isMuted)
        val ctx = context?.applicationContext ?: return
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (!active) {
                audioManager.isMicrophoneMute = false
                audioManager.mode = AudioManager.MODE_NORMAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.clearCommunicationDevice()
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                }
                return
            }

            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isMicrophoneMute = isMuted

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val targetType = if (isSpeakerOn) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                } else {
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                }
                val device = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == targetType
                }
                if (device != null) {
                    audioManager.setCommunicationDevice(device)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = isSpeakerOn
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio routing update warning", e)
        }
    }

    /**
     * Reads and compresses a picked image URI from Android Photo Picker into a high-definition
     * Base64 JPEG payload (up to 1080p HD clarity while fitting inside Firestore document limits).
     */
    fun encodePickedImageUri(uri: Uri, channelId: String = "global", highDefinition: Boolean = true): String? {
        val ctx = context?.applicationContext ?: return null
        return try {
            val input = ctx.contentResolver.openInputStream(uri) ?: return null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return null
            val maxDim = if (highDefinition) 1080 else 640
            val ratio = minOf(
                maxDim.toFloat() / original.width.coerceAtLeast(1),
                maxDim.toFloat() / original.height.coerceAtLeast(1),
                1.0f
            )
            val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)

            var quality = if (highDefinition) 84 else 72
            var jpegBytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                jpegBytes = out.toByteArray()
                quality -= 10
            } while (jpegBytes.size > 480_000 && quality >= 40)

            Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed encoding picked HD image URI", e)
            null
        }
    }

    /**
     * Encodes a picked user profile photo into a crisp 256x256 square `data:image/jpeg;base64,...` URI
     * so custom avatars sync across all devices via Firestore `/users/{uid}`.
     */
    fun encodeProfileAvatarUri(uri: Uri): String? {
        val ctx = context?.applicationContext ?: return null
        return try {
            val input = ctx.contentResolver.openInputStream(uri) ?: return null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return null
            val minEdge = minOf(original.width, original.height).coerceAtLeast(1)
            val cropX = ((original.width - minEdge) / 2).coerceAtLeast(0)
            val cropY = ((original.height - minEdge) / 2).coerceAtLeast(0)
            val square = Bitmap.createBitmap(original, cropX, cropY, minEdge, minEdge)
            val scaled = Bitmap.createScaledBitmap(square, 240, 240, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 78, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (e: Exception) {
            Log.e(TAG, "Failed encoding profile avatar URI", e)
            null
        }
    }

    /**
     * Extracts a real HD video keyframe thumbnail, duration, and file name from a picked video URI
     * using Android's `MediaMetadataRetriever`.
     */
    fun encodePickedVideoUri(uri: Uri): EncodedVideoAttachment? {
        val ctx = context?.applicationContext ?: return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(ctx, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 5000L
            val durationSec = (durationMs / 1000L).coerceAtLeast(1L)
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            val thumbB64 = if (frame != null) {
                val maxDim = 960
                val ratio = minOf(
                    maxDim.toFloat() / frame.width.coerceAtLeast(1),
                    maxDim.toFloat() / frame.height.coerceAtLeast(1),
                    1.0f
                )
                val scaled = Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * ratio).toInt().coerceAtLeast(1),
                    (frame.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } else {
                ""
            }

            val fileName = resolveDisplayName(ctx, uri).ifBlank { "HD_Video_${System.currentTimeMillis() % 10000}.mp4" }
            val fileSize = resolveFileSize(ctx, uri)

            EncodedVideoAttachment(
                thumbnailBase64 = thumbB64,
                durationSec = durationSec,
                displayTitle = fileName,
                sizeBytes = fileSize
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed extracting HD video metadata", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Reads metadata and preview payload for any document/file chosen via Android's `OpenDocument`.
     */
    fun encodePickedDocumentUri(uri: Uri): EncodedFileAttachment? {
        val ctx = context?.applicationContext ?: return null
        return try {
            val fileName = resolveDisplayName(ctx, uri).ifBlank { "Document_${System.currentTimeMillis() % 10000}" }
            val mimeType = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
            val fileSize = resolveFileSize(ctx, uri)
            val previewB64 = if (mimeType.startsWith("image/")) {
                encodePickedImageUri(uri, highDefinition = true).orEmpty()
            } else {
                ""
            }
            EncodedFileAttachment(
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = fileSize,
                previewBase64 = previewB64
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading picked document URI", e)
            null
        }
    }

    private fun resolveDisplayName(ctx: Context, uri: Uri): String {
        return runCatching {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else ""
            }.orEmpty()
        }.getOrDefault("")
    }

    private fun resolveFileSize(ctx: Context, uri: Uri): Long {
        return runCatching {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getLong(idx) else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    fun releaseAll() {
        stopLiveCallAudioTransceiver()
        stopRingingIncomingCall()
        stopMicrophoneRecordingAndDiscard()
        stopPlayback()
        configureCallAudioRouting(active = false, isMuted = false, isSpeakerOn = false)
    }

    // --- ITU G.711 μ-LAW VOICE CODEC FOR REAL-TIME TWO-WAY CALL STREAMING ---

    private fun linearPcm16ToUlaw(pcmVal: Int): Byte {
        var pcm = pcmVal shr 2
        val mask: Int
        if (pcm < 0) {
            pcm = -pcm
            mask = 0x7F
        } else {
            mask = 0xFF
        }
        if (pcm > ULAW_CLIP) pcm = ULAW_CLIP
        pcm += (ULAW_BIAS shr 2)

        var seg = 0
        val segEnd = intArrayOf(0x3F, 0x7F, 0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF)
        while (seg < 8 && pcm > segEnd[seg]) {
            seg++
        }
        if (seg >= 8) return (0x7F xor mask).toByte()
        val uval = (seg shl 4) or ((pcm shr (seg + 1)) and 0x0F)
        return (uval xor mask).toByte()
    }

    private fun ulawToLinearPcm16(ulawByte: Byte): Short {
        val uval = ulawByte.toInt().inv() and 0xFF
        var t = ((uval and 0x0F) shl 3) + ULAW_BIAS
        t = t shl ((uval and 0x70) shr 4)
        val sample = if ((uval and 0x80) != 0) (ULAW_BIAS - t) else (t - ULAW_BIAS)
        return sample.coerceIn(-32768, 32767).toShort()
    }

    companion object {
        private const val TAG = "AudioMediaController"
        private const val MAX_FIRESTORE_MEDIA_BYTES = 450_000
        private const val CALL_SAMPLE_RATE = 8000
        private const val CALL_PACKET_SAMPLES = 2800 // 350ms @ 8kHz mono
        private const val ULAW_BIAS = 0x84
        private const val ULAW_CLIP = 8159
    }
}
