package com.example.core.notifications

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object SecretGuardNotificationHelper {
    private const val CALL_CHANNEL_ID = "secretguard_calls_channel"
    private const val MESSAGE_CHANNEL_ID = "secretguard_messages_channel"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "Incoming Encrypted Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time incoming audio and Full HD 1080p video calls"
                enableVibration(true)
            }
            val msgChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "Encrypted Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Real-time AES-256-GCM encrypted messages and media"
                enableVibration(true)
            }
            manager.createNotificationChannel(callChannel)
            manager.createNotificationChannel(msgChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerName: String,
        callType: String
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            callId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeLabel = if (callType == "video") "Full HD 1080p Video Call" else "Encrypted Audio Call"
        val notification = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Incoming $typeLabel")
            .setContentText("$callerName is calling you via AES-256-GCM")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(callId.hashCode(), notification)
        }
    }

    fun dismissCallNotification(context: Context, callId: String) {
        if (callId.isBlank()) return
        runCatching {
            NotificationManagerCompat.from(context).cancel(callId.hashCode())
        }
    }

    fun showIncomingMessageNotification(
        context: Context,
        conversationId: String,
        senderName: String,
        previewText: String
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName.ifBlank { "SecretGuard Contact" })
            .setContentText(previewText.ifBlank { "New encrypted message" })
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(conversationId.hashCode(), notification)
        }
    }

    /**
     * Triggers Android's native hardware BiometricPrompt (Face Unlock / Fingerprint / Device Credential)
     * on API 28+ devices.
     */
    fun authenticateWithNativeBiometric(
        activity: Activity,
        onAuthenticated: () -> Unit,
        onMessage: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onMessage("Native BiometricPrompt requires Android 9+. Use your Secret Code PIN.")
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val builder = BiometricPrompt.Builder(activity)
                .setTitle("SecretGuard Biometric App Lock")
                .setSubtitle("Unlock with Face Unlock or Fingerprint")
                .setDescription("Authenticate to unlock your encrypted Keystore session")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                builder.setNegativeButton("Use PIN", executor) { _, _ ->
                    onMessage("Enter your 6–8 digit Secret Code PIN.")
                }
            }

            val prompt = builder.build()
            val cancellationSignal = CancellationSignal()
            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onAuthenticated()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        onMessage(errString?.toString() ?: "Biometric authentication unavailable; enter your PIN.")
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onMessage("Biometric not recognized. Try again or enter your Secret Code PIN.")
                    }
                }
            )
        } catch (e: Exception) {
            onMessage("Hardware biometric sensor not enrolled on this device. Please use your Secret Code PIN.")
        }
    }
}
