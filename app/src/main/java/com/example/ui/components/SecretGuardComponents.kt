package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.LocalSecretGuardTokens

/**
 * Renders the signature SecretGuard 3D-styled shield emblem adapted to the active pastel theme tokens.
 */
@Composable
fun SecretGuardShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    showCheckmarkInsteadOfMic: Boolean = false,
    showOrbitalRings: Boolean = true
) {
    val tokens = LocalSecretGuardTokens.current
    val primaryColor = tokens.primary
    val onPrimaryColor = tokens.onPrimary

    Box(
        modifier = modifier.size(if (showOrbitalRings) size * 1.38f else size),
        contentAlignment = Alignment.Center
    ) {
        if (showOrbitalRings) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.20f),
                            primaryColor.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = this.size.minDimension / 2f
                    ),
                    radius = this.size.minDimension / 2f,
                    center = center
                )
                drawOval(
                    color = primaryColor.copy(alpha = 0.22f),
                    topLeft = Offset(this.size.width * 0.06f, this.size.height * 0.25f),
                    size = androidx.compose.ui.geometry.Size(
                        this.size.width * 0.88f,
                        this.size.height * 0.50f
                    ),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.55f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(this.size.width * 0.12f, this.size.height * 0.50f)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.55f),
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
                            primaryColor.copy(alpha = 0.85f),
                            primaryColor,
                            tokens.text
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
                            primaryColor,
                            tokens.text
                        )
                    )
                )

                drawPath(
                    path = innerShield,
                    color = onPrimaryColor.copy(alpha = 0.35f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            if (showCheckmarkInsteadOfMic) {
                Box(
                    modifier = Modifier
                        .size(size * 0.36f)
                        .clip(CircleShape)
                        .background(onPrimaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(size * 0.24f)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = onPrimaryColor,
                    modifier = Modifier.size(size * 0.46f)
                )
            }
        }
    }
}

@Composable
fun SecretGuardWordmark(
    fontSize: TextUnit = 22.sp,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSecretGuardTokens.current
    val safeSize = if (fontSize.value > 28f) 22.sp else fontSize
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = tokens.text,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("Secret")
            }
            withStyle(
                SpanStyle(
                    color = tokens.primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("Guard")
            }
        },
        fontSize = safeSize,
        lineHeight = 28.sp,
        modifier = modifier
    )
}

/**
 * Renders rich user avatars:
 * - Supports custom gallery photos encoded as `data:image/jpeg;base64,...`
 * - Supports remote Google profile URLs (`http://`, `https://`)
 * - Supports group avatars (`"group"`) and initial badges using theme design tokens.
 */
@Composable
fun ContactAvatarView(
    avatarKey: String,
    displayName: String,
    size: Dp = 48.dp,
    showGlowRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSecretGuardTokens.current
    val ringModifier = if (showGlowRing) {
        Modifier
            .size(size + tokens.sp12)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    colors = listOf(
                        tokens.primary,
                        tokens.surface2,
                        tokens.primary
                    )
                )
            )
            .padding(tokens.sp4)
    } else {
        Modifier.size(size)
    }

    Box(
        modifier = modifier.then(ringModifier),
        contentAlignment = Alignment.Center
    ) {
        if (avatarKey.startsWith("data:image", ignoreCase = true) || avatarKey.length > 200) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(tokens.surface2)
                    .border(1.5.dp, tokens.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Base64BitmapImage(
                    base64Data = avatarKey,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
            return@Box
        }

        if (avatarKey.startsWith("http://", ignoreCase = true) || avatarKey.startsWith("https://", ignoreCase = true)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(tokens.primary, tokens.text)
                        )
                    )
                    .border(1.5.dp, tokens.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                Text(
                    text = initial,
                    color = tokens.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).coerceIn(12f, 22f).sp
                )
                AsyncImage(
                    model = avatarKey,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
            return@Box
        }

        when (avatarKey.lowercase()) {
            "group" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(tokens.surface2)
                        .border(1.dp, tokens.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size * 0.64f)
                            .clip(CircleShape)
                            .background(tokens.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = displayName,
                            tint = tokens.onPrimary,
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
                                colors = listOf(tokens.primary, tokens.text)
                            )
                        )
                        .border(1.dp, tokens.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayName.isBlank()) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = tokens.onPrimary,
                            modifier = Modifier.size(size * 0.5f)
                        )
                    } else {
                        Text(
                            text = initial,
                            color = tokens.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = (size.value * 0.36f).coerceIn(12f, 22f).sp
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
    textColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSecretGuardTokens.current
    val resolvedColor = if (textColor == Color.Unspecified) tokens.textMuted else textColor
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
    ) {
        Box(
            modifier = Modifier
                .size(tokens.sp8)
                .clip(CircleShape)
                .background(if (isOnline) tokens.success else tokens.textMuted.copy(alpha = 0.45f))
        )
        Text(
            text = label,
            fontSize = tokens.captionSize,
            lineHeight = tokens.captionLineHeight,
            color = resolvedColor,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- REUSABLE STRICT DESIGN-TOKEN COMPONENTS (BUILD ONCE, REUSE) ---

@Composable
fun SecretGuardTopBar(
    title: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingContentDescription: String? = null,
    leadingTestTag: String = "",
    onLeadingClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val tokens = LocalSecretGuardTokens.current
    Surface(
        color = tokens.bg,
        modifier = modifier
            .fillMaxWidth()
            .height(tokens.topBarHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tokens.sp12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (leadingIcon != null && onLeadingClick != null) {
                    SecretGuardIconButton(
                        icon = leadingIcon,
                        contentDescription = leadingContentDescription ?: title,
                        testTag = leadingTestTag,
                        onClick = onLeadingClick
                    )
                    Spacer(modifier = Modifier.width(tokens.sp8))
                }
                Text(
                    text = title,
                    fontSize = tokens.titleSize,
                    lineHeight = tokens.titleLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = tokens.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.sp8),
                content = actions
            )
        }
    }
}

@Composable
fun SecretGuardPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    testTag: String = ""
) {
    val tokens = LocalSecretGuardTokens.current
    val container = if (isDanger) tokens.danger else tokens.primary
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(tokens.radiusPill),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = tokens.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(tokens.buttonHeight)
            .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(tokens.iconSize)
            )
            Spacer(modifier = Modifier.width(tokens.sp8))
        }
        Text(
            text = text,
            fontSize = tokens.bodySize,
            lineHeight = tokens.bodyLineHeight,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SecretGuardIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    containerColor: Color = Color.Transparent,
    testTag: String = ""
) {
    val tokens = LocalSecretGuardTokens.current
    val resolvedTint = if (tint == Color.Unspecified) tokens.text else tint
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(tokens.minTouchTarget)
            .clip(RoundedCornerShape(tokens.radiusPill))
            .background(containerColor)
            .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(tokens.iconSize)
        )
    }
}

@Composable
fun SecretGuardCard(
    modifier: Modifier = Modifier,
    useSecondarySurface: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalSecretGuardTokens.current
    val bg = if (useSecondarySurface) tokens.surface2 else tokens.surface
    val shape = RoundedCornerShape(tokens.radiusCard)
    Surface(
        shape = shape,
        color = bg,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, tokens.border, shape)
            .clip(shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.sp16),
            verticalArrangement = Arrangement.spacedBy(tokens.sp12),
            content = content
        )
    }
}

@Composable
fun SecretGuardChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    testTag: String = ""
) {
    val tokens = LocalSecretGuardTokens.current
    val shape = RoundedCornerShape(tokens.radiusPill)
    Surface(
        shape = shape,
        color = if (selected) tokens.primary else tokens.surface2,
        modifier = modifier
            .heightIn(min = 44.dp)
            .border(1.dp, if (selected) tokens.primary else tokens.border, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.sp12, vertical = tokens.sp8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.sp8)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (selected) tokens.onPrimary else tokens.primary,
                    modifier = Modifier.size(tokens.chipIconSize)
                )
            }
            Text(
                text = label,
                fontSize = tokens.captionSize,
                lineHeight = tokens.captionLineHeight,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) tokens.onPrimary else tokens.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SecretGuardInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    testTag: String = ""
) {
    val tokens = LocalSecretGuardTokens.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.sp4)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontSize = tokens.captionSize,
                lineHeight = tokens.captionLineHeight,
                fontWeight = FontWeight.Medium,
                color = tokens.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            shape = RoundedCornerShape(tokens.radiusSmall),
            color = tokens.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(tokens.inputHeight)
                .border(1.dp, tokens.border, RoundedCornerShape(tokens.radiusSmall))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = tokens.sp12),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = tokens.bodySize,
                        lineHeight = tokens.bodyLineHeight,
                        color = tokens.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = tokens.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
                )
            }
        }
    }
}

@Composable
fun SecretGuardEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val tokens = LocalSecretGuardTokens.current
    SecretGuardCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.sp8)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tokens.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tokens.primary,
                    modifier = Modifier.size(tokens.iconSize)
                )
            }
            Text(
                text = title,
                fontSize = tokens.headingSize,
                lineHeight = tokens.headingLineHeight,
                fontWeight = FontWeight.Medium,
                color = tokens.text,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = tokens.bodySize,
                lineHeight = tokens.bodyLineHeight,
                color = tokens.textMuted,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(tokens.sp4))
                SecretGuardPrimaryButton(
                    text = actionLabel,
                    onClick = onActionClick
                )
            }
        }
    }
}
