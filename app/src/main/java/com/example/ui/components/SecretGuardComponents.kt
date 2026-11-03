package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepGreenBlackText
import com.example.ui.theme.EmeraldDarkAction
import com.example.ui.theme.EmeraldDeepForest
import com.example.ui.theme.EmeraldLightTint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnlineGreenStatus
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.SecondarySlateGreenText

/**
 * Renders the signature SecretGuard 3D-styled emerald shield emblem with either
 * a microphone icon (Welcome / Home) or checkmark icon (Settings), surrounded by
 * subtle orbital privacy rings.
 */
@Composable
fun SecretGuardShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    showCheckmarkInsteadOfMic: Boolean = false,
    showOrbitalRings: Boolean = true
) {
    Box(
        modifier = modifier.size(if (showOrbitalRings) size * 1.45f else size),
        contentAlignment = Alignment.Center
    ) {
        if (showOrbitalRings) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.18f),
                            EmeraldPrimary.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = this.size.minDimension / 2f
                    ),
                    radius = this.size.minDimension / 2f,
                    center = center
                )
                drawOval(
                    color = EmeraldPrimary.copy(alpha = 0.22f),
                    topLeft = Offset(this.size.width * 0.06f, this.size.height * 0.25f),
                    size = androidx.compose.ui.geometry.Size(
                        this.size.width * 0.88f,
                        this.size.height * 0.50f
                    ),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Small orbital node dots matching the reference artwork
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = 0.55f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(this.size.width * 0.12f, this.size.height * 0.50f)
                )
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = 0.55f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(this.size.width * 0.88f, this.size.height * 0.50f)
                )
            }
        }

        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height

                val outerShield = Path().apply {
                    moveTo(w * 0.50f, h * 0.06f)
                    lineTo(w * 0.14f, h * 0.20f)
                    lineTo(w * 0.14f, h * 0.50f)
                    cubicTo(
                        w * 0.14f, h * 0.74f,
                        w * 0.30f, h * 0.89f,
                        w * 0.50f, h * 0.96f
                    )
                    cubicTo(
                        w * 0.70f, h * 0.89f,
                        w * 0.86f, h * 0.74f,
                        w * 0.86f, h * 0.50f
                    )
                    lineTo(w * 0.86f, h * 0.20f)
                    close()
                }

                drawPath(
                    path = outerShield,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF28C78F),
                            EmeraldPrimary,
                            EmeraldDeepForest
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                val innerShield = Path().apply {
                    moveTo(w * 0.50f, h * 0.13f)
                    lineTo(w * 0.21f, h * 0.25f)
                    lineTo(w * 0.21f, h * 0.49f)
                    cubicTo(
                        w * 0.21f, h * 0.69f,
                        w * 0.34f, h * 0.82f,
                        w * 0.50f, h * 0.88f
                    )
                    cubicTo(
                        w * 0.66f, h * 0.82f,
                        w * 0.79f, h * 0.69f,
                        w * 0.79f, h * 0.49f
                    )
                    lineTo(w * 0.79f, h * 0.25f)
                    close()
                }

                drawPath(
                    path = innerShield,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            EmeraldDarkAction,
                            Color(0xFF044932)
                        )
                    )
                )

                drawPath(
                    path = innerShield,
                    color = Color.White.copy(alpha = 0.35f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            if (showCheckmarkInsteadOfMic) {
                Box(
                    modifier = Modifier
                        .size(size * 0.36f)
                        .clip(CircleShape)
                        .background(PureWhiteSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(size * 0.24f)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = PureWhiteSurface,
                    modifier = Modifier.size(size * 0.46f)
                )
            }
        }
    }
}

@Composable
fun SecretGuardWordmark(
    fontSize: TextUnit = 30.sp,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = DeepGreenBlackText,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append("Secret")
            }
            withStyle(
                SpanStyle(
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append("Guard")
            }
        },
        fontSize = fontSize,
        modifier = modifier
    )
}

/**
 * Renders rich illustrated portrait avatars matching the reference screens
 * ("priya", "rahul", "group", "user", or custom initials).
 */
@Composable
fun ContactAvatarView(
    avatarKey: String,
    displayName: String,
    size: Dp = 48.dp,
    showGlowRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ringModifier = if (showGlowRing) {
        Modifier
            .size(size + 12.dp)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF64FCD9),
                        Color(0xFFE0C3FC),
                        Color(0xFF64FCD9)
                    )
                )
            )
            .padding(4.dp)
    } else {
        Modifier.size(size)
    }

    Box(
        modifier = modifier.then(ringModifier),
        contentAlignment = Alignment.Center
    ) {
        when (avatarKey.lowercase()) {
            "priya" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFF6D5C9), Color(0xFFE2A898))
                            )
                        )
                        .border(1.5.dp, PureWhiteSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = this.size.width
                        val h = this.size.height
                        // Soft hair silhouette
                        drawCircle(
                            color = Color(0xFF2D1B14),
                            radius = w * 0.36f,
                            center = Offset(w * 0.5f, h * 0.44f)
                        )
                        // Shoulders
                        drawOval(
                            color = Color(0xFFFAF9F2),
                            topLeft = Offset(w * 0.16f, h * 0.68f),
                            size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.45f)
                        )
                        // Face
                        drawCircle(
                            color = Color(0xFFF9DACB),
                            radius = w * 0.23f,
                            center = Offset(w * 0.5f, h * 0.44f)
                        )
                        // Warm smile & eyes
                        drawCircle(
                            color = Color(0xFF2D1B14),
                            radius = w * 0.025f,
                            center = Offset(w * 0.42f, h * 0.42f)
                        )
                        drawCircle(
                            color = Color(0xFF2D1B14),
                            radius = w * 0.025f,
                            center = Offset(w * 0.58f, h * 0.42f)
                        )
                        drawArc(
                            color = Color(0xFFC25953),
                            startAngle = 20f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(w * 0.41f, h * 0.44f),
                            size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.12f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            "rahul", "user" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF8EC5FC), Color(0xFF5B86E5))
                            )
                        )
                        .border(1.5.dp, PureWhiteSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = this.size.width
                        val h = this.size.height
                        // Shoulders
                        drawOval(
                            color = Color(0xFF1A2E3B),
                            topLeft = Offset(w * 0.16f, h * 0.68f),
                            size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.44f)
                        )
                        // Hair & Face
                        drawCircle(
                            color = Color(0xFF1F1A17),
                            radius = w * 0.26f,
                            center = Offset(w * 0.5f, h * 0.38f)
                        )
                        drawCircle(
                            color = Color(0xFFE5B899),
                            radius = w * 0.22f,
                            center = Offset(w * 0.5f, h * 0.45f)
                        )
                        // Sunglasses matching the reference avatar
                        drawRoundRect(
                            color = Color(0xFF102A22),
                            topLeft = Offset(w * 0.34f, h * 0.39f),
                            size = androidx.compose.ui.geometry.Size(w * 0.13f, h * 0.08f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                        drawRoundRect(
                            color = Color(0xFF102A22),
                            topLeft = Offset(w * 0.53f, h * 0.39f),
                            size = androidx.compose.ui.geometry.Size(w * 0.13f, h * 0.08f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                        drawLine(
                            color = Color(0xFF102A22),
                            start = Offset(w * 0.47f, h * 0.42f),
                            end = Offset(w * 0.53f, h * 0.42f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            "group" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(EmeraldLightTint)
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size * 0.64f)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = displayName,
                            tint = PureWhiteSurface,
                            modifier = Modifier.size(size * 0.38f)
                        )
                    }
                }
            }

            else -> {
                val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldPrimary, EmeraldDarkAction)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayName.isBlank()) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = PureWhiteSurface,
                            modifier = Modifier.size(size * 0.5f)
                        )
                    } else {
                        Text(
                            text = initial,
                            color = PureWhiteSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = (size.value * 0.42f).sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnlinePresenceRow(
    isOnline: Boolean,
    label: String = if (isOnline) "Online" else "Offline",
    textColor: Color = SecondarySlateGreenText,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOnline) OnlineGreenStatus else SecondarySlateGreenText.copy(alpha = 0.5f))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}
