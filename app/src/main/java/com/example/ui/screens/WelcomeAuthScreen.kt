package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SecretGuardShieldLogo
import com.example.ui.components.SecretGuardWordmark
import com.example.ui.theme.DeepGreenBlackText
import com.example.ui.theme.EmeraldLightTint
import com.example.ui.theme.EmeraldMintCard
import com.example.ui.theme.EmeraldPillBorder
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.SecondarySlateGreenText
import com.example.ui.theme.WarmIvoryBackground

/**
 * Screen 1 — Welcome & Authentication matching the reference specification:
 * - Centered shield/microphone mark, SecretGuard wordmark, and "Record. Call. Chat. Secretly & Securely".
 * - Primary "Continue with Google" pill button powered by Jetpack Credential Manager (GetSignInWithGoogleOption).
 * - Secondary "Continue with Email" and "Continue Anonymously" cards explaining Firebase Console provider setup
 *   and account recovery while routing authentication through the provisioned Google Sign-In flow.
 * - Footer: "Your Privacy, Our Priority".
 */
@Composable
fun WelcomeAuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var providerDialogMode by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvoryBackground)
    ) {
        // Decorative top & bottom organic emerald waves matching the mockup
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val topWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.65f, 0f)
                quadraticTo(w * 0.35f, h * 0.08f, 0f, h * 0.14f)
                close()
            }
            drawPath(
                path = topWave,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        EmeraldPrimary.copy(alpha = 0.16f),
                        Color.Transparent
                    )
                )
            )

            val bottomWaveBack = Path().apply {
                moveTo(0f, h * 0.90f)
                quadraticTo(w * 0.30f, h * 0.85f, w * 0.62f, h * 0.91f)
                quadraticTo(w * 0.84f, h * 0.95f, w, h * 0.89f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = bottomWaveBack,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        EmeraldPrimary.copy(alpha = 0.25f),
                        EmeraldPrimary.copy(alpha = 0.55f)
                    )
                )
            )

            val bottomWaveFront = Path().apply {
                moveTo(0f, h * 0.94f)
                quadraticTo(w * 0.25f, h * 0.98f, w * 0.55f, h * 0.93f)
                quadraticTo(w * 0.80f, h * 0.89f, w, h * 0.95f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = bottomWaveFront,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0A875B),
                        EmeraldPrimary,
                        Color(0xFF28B884)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Hero Emblem + Brand Wordmark
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                SecretGuardShieldLogo(
                    size = 98.dp,
                    showCheckmarkInsteadOfMic = false,
                    showOrbitalRings = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                SecretGuardWordmark(fontSize = 32.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Record. Call. Chat.\nSecretly & Securely",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SecondarySlateGreenText,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Three Authentication Pill Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = Color(0xFFFEE4E2),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFB42318),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 1. Continue with Google (Primary Emerald Pill)
                Button(
                    onClick = onGoogleSignInClick,
                    enabled = !isLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = PureWhiteSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 1.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PureWhiteSurface,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Signing in with Google...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        // Google G Badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PureWhiteSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFF4285F4),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp
                                        )
                                    ) {
                                        append("G")
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 2. Continue with Email (Elevated White Pill)
                Surface(
                    shape = CircleShape,
                    color = PureWhiteSurface,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isLoading) {
                            providerDialogMode = "email"
                        }
                        .testTag("email_auth_info_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = "Email",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Email",
                            color = DeepGreenBlackText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 3. Continue Anonymously (Soft Mint-Tinted Pill)
                Surface(
                    shape = CircleShape,
                    color = EmeraldMintCard,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(1.dp, EmeraldPillBorder, CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = !isLoading) {
                            providerDialogMode = "anonymous"
                        }
                        .testTag("anonymous_auth_info_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = "Anonymous",
                            tint = Color(0xFF065F46),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue Anonymously",
                            color = DeepGreenBlackText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // "or" divider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = SecondarySlateGreenText.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "or",
                        color = SecondarySlateGreenText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = SecondarySlateGreenText.copy(alpha = 0.25f)
                    )
                }
            }

            // Footer: "Your Privacy, Our Priority"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 34.dp, top = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Privacy Verified",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Your Privacy,\nOur Priority",
                        color = SecondarySlateGreenText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    if (providerDialogMode != null) {
        val isEmail = providerDialogMode == "email"
        AlertDialog(
            onDismissRequest = { providerDialogMode = null },
            containerColor = PureWhiteSurface,
            title = {
                Text(
                    text = if (isEmail) "Email Authentication Setup" else "Anonymous Session & Account Linking",
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenBlackText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEmail) {
                            "This Firebase project is provisioned with Google Identity via Jetpack Credential Manager for verified zero-trust Firestore security rules."
                        } else {
                            "Per the SecretGuard security specification, unlinked anonymous sessions risk losing encrypted keys if app data is cleared. Linking with Google preserves account recovery across devices."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepGreenBlackText
                    )
                    Surface(
                        color = EmeraldLightTint,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "To enable additional providers (Email/Password or Anonymous Auth) manually, open Firebase Console -> Authentication -> Sign-in method. You can sign in right now using Continue with Google.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        providerDialogMode = null
                        onGoogleSignInClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Continue with Google")
                }
            },
            dismissButton = {
                TextButton(onClick = { providerDialogMode = null }) {
                    Text("Close", color = SecondarySlateGreenText)
                }
            }
        )
    }
}
