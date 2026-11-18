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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.components.SecretGuardShieldLogo
import com.example.ui.components.SecretGuardWordmark
import com.example.ui.theme.LocalSecretGuardTokens
import com.example.ui.viewmodel.SecretGuardViewModel

/**
 * Screen 1 — Welcome & Authentication:
 * - Clean, refined layout adhering to design tokens with zero technical disclaimers or white overlays.
 * - Supports "Continue with Google", "Continue with Email" (with Name, Email, Gender Female/Male, Age),
 *   and "Continue Anonymously" (private cloaked alias + optional Gender/Age).
 */
@Composable
fun WelcomeAuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalSecretGuardTokens.current
    var authSheetMode by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.bg)
    ) {
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
                        tokens.primary.copy(alpha = 0.14f),
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
                        tokens.primary.copy(alpha = 0.22f),
                        tokens.primary.copy(alpha = 0.48f)
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
                        tokens.primary.copy(alpha = 0.85f),
                        tokens.primary,
                        tokens.primary.copy(alpha = 0.72f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.sp24, vertical = tokens.sp16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(tokens.sp12))

            // Hero Emblem + Brand Wordmark
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                SecretGuardShieldLogo(
                    size = if (tokens.isCompact) 80.dp else 96.dp,
                    showCheckmarkInsteadOfMic = false,
                    showOrbitalRings = true
                )

                Spacer(modifier = Modifier.height(tokens.sp8))

                SecretGuardWordmark(fontSize = tokens.titleSize)

                Spacer(modifier = Modifier.height(tokens.sp8))

                Text(
                    text = "Record. Call. Chat.\nSecretly & Securely",
                    fontSize = tokens.bodySize,
                    lineHeight = tokens.bodyLineHeight,
                    color = tokens.textMuted,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(tokens.sp24))

            // Three Authentication Pill Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(tokens.sp12),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = tokens.surface2,
                        shape = RoundedCornerShape(tokens.radiusCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, tokens.danger, RoundedCornerShape(tokens.radiusCard))
                    ) {
                        Text(
                            text = errorMessage,
                            color = tokens.danger,
                            fontSize = tokens.captionSize,
                            lineHeight = tokens.captionLineHeight,
                            modifier = Modifier.padding(horizontal = tokens.sp16, vertical = tokens.sp8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 1. Continue with Google (Primary Pill, 48dp height)
                Button(
                    onClick = onGoogleSignInClick,
                    enabled = !isLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primary,
                        contentColor = tokens.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tokens.buttonHeight)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = tokens.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(tokens.sp12))
                        Text(
                            text = "Signing in...",
                            fontSize = tokens.bodySize,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(tokens.onPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = tokens.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = tokens.bodySize
                                        )
                                    ) {
                                        append("G")
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(tokens.sp12))
                        Text(
                            text = "Continue with Google",
                            fontSize = tokens.bodySize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 2. Continue with Email
                Surface(
                    shape = CircleShape,
                    color = tokens.surface2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tokens.buttonHeight)
                        .border(1.dp, tokens.border, CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = !isLoading) {
                            authSheetMode = "email"
                        }
                        .testTag("email_auth_info_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = tokens.sp16),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = "Email",
                            tint = tokens.primary,
                            modifier = Modifier.size(tokens.chipIconSize)
                        )
                        Spacer(modifier = Modifier.width(tokens.sp12))
                        Text(
                            text = "Continue with Email",
                            color = tokens.text,
                            fontSize = tokens.bodySize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3. Continue Anonymously
                Surface(
                    shape = CircleShape,
                    color = tokens.surface2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tokens.buttonHeight)
                        .border(1.dp, tokens.border, CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = !isLoading) {
                            authSheetMode = "anonymous"
                        }
                        .testTag("anonymous_auth_info_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = tokens.sp16),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = "Anonymous",
                            tint = tokens.primary,
                            modifier = Modifier.size(tokens.chipIconSize)
                        )
                        Spacer(modifier = Modifier.width(tokens.sp12))
                        Text(
                            text = "Continue Anonymously",
                            color = tokens.text,
                            fontSize = tokens.bodySize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(tokens.sp4))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = tokens.sp12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = tokens.border
                    )
                    Text(
                        text = "or",
                        color = tokens.textMuted,
                        fontSize = tokens.captionSize,
                        modifier = Modifier.padding(horizontal = tokens.sp12)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = tokens.border
                    )
                }
            }

            // Footer: "Your Privacy, Our Priority"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = tokens.sp32, top = tokens.sp12)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Privacy Verified",
                        tint = tokens.primary,
                        modifier = Modifier.size(tokens.iconSize)
                    )
                    Spacer(modifier = Modifier.width(tokens.sp8))
                    Text(
                        text = "Your Privacy, Our Priority • v5.0",
                        color = tokens.textMuted,
                        fontSize = tokens.captionSize,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (authSheetMode != null) {
        val isEmail = authSheetMode == "email"
        var emailInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf(if (isEmail) "" else "Anonymous User") }
        var selectedGender by remember { mutableStateOf("Female") }
        var ageInput by remember { mutableStateOf("22") }

        AlertDialog(
            onDismissRequest = { authSheetMode = null },
            shape = RoundedCornerShape(tokens.radiusCard),
            containerColor = tokens.bg,
            title = {
                Text(
                    text = if (isEmail) "Sign In with Email Profile" else "Anonymous Cloak Sign In",
                    fontSize = tokens.headingSize,
                    lineHeight = tokens.headingLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                    if (isEmail) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it.take(100) },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(60) },
                        label = { Text(if (isEmail) "Display Name" else "Anonymous Alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Gender",
                        fontSize = tokens.captionSize,
                        fontWeight = FontWeight.Bold,
                        color = tokens.text
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.sp8)) {
                        listOf("Female", "Male").forEach { g ->
                            FilterChip(
                                selected = selectedGender == g,
                                onClick = { selectedGender = g },
                                label = { Text(g, fontSize = tokens.captionSize) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val effectiveName = nameInput.trim().ifBlank {
                            if (isEmail) emailInput.substringBefore("@").ifBlank { "SecretGuard User" }
                            else "Anonymous User"
                        }
                        val effectiveHandle = if (isEmail && emailInput.isNotBlank()) {
                            "@${emailInput.substringBefore("@").lowercase().replace("[^a-z0-9_]".toRegex(), "")}"
                        } else {
                            "@anon_${(1000..9999).random()}"
                        }
                        val parsedAge = ageInput.toLongOrNull()?.coerceIn(13L, 120L) ?: 22L
                        SecretGuardViewModel.savePendingOnboardingPersona(
                            context = context,
                            displayName = effectiveName,
                            userHandle = effectiveHandle,
                            gender = selectedGender,
                            age = parsedAge,
                            visibility = if (isEmail) "contacts" else "private"
                        )
                        authSheetMode = null
                        onGoogleSignInClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primary),
                    shape = RoundedCornerShape(tokens.radiusPill)
                ) {
                    Text(if (isEmail) "Continue" else "Enter Anonymously")
                }
            },
            dismissButton = {
                TextButton(onClick = { authSheetMode = null }) {
                    Text("Cancel", color = tokens.textMuted)
                }
            }
        )
    }
}
