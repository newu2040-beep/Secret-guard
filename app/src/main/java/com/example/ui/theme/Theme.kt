package com.example.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Font options resolved natively via Android system font families (zero binary .ttf files required).
 */
enum class AppFontOption(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    PLUS_JAKARTA(
        id = "plus_jakarta",
        displayName = "Plus Jakarta Sans",
        subtitle = "Modern geometric sans"
    ),
    OUTFIT(
        id = "outfit",
        displayName = "Outfit",
        subtitle = "Clean rounded display"
    ),
    SPACE_GROTESK(
        id = "space_grotesk",
        displayName = "Space Grotesk",
        subtitle = "Crisp condensed sans"
    ),
    PLAYFAIR_DISPLAY(
        id = "playfair_display",
        displayName = "Playfair Display",
        subtitle = "Classic literary serif"
    ),
    ROBOTO_MONO(
        id = "roboto_mono",
        displayName = "Roboto Mono",
        subtitle = "Precision technical mono"
    ),
    SYSTEM_SANS(
        id = "system_sans",
        displayName = "Signature Cursive",
        subtitle = "Expressive script style"
    );

    fun toFontFamily(): FontFamily {
        return when (this) {
            PLUS_JAKARTA -> FontFamily(
                Font(DeviceFontFamilyName("sans-serif"), FontWeight.Normal),
                Font(DeviceFontFamilyName("sans-serif-medium"), FontWeight.Medium),
                Font(DeviceFontFamilyName("sans-serif-medium"), FontWeight.Bold)
            )
            OUTFIT -> FontFamily(
                Font(DeviceFontFamilyName("sans-serif-light"), FontWeight.Normal),
                Font(DeviceFontFamilyName("sans-serif"), FontWeight.Medium),
                Font(DeviceFontFamilyName("sans-serif-black"), FontWeight.Bold)
            )
            SPACE_GROTESK -> FontFamily(
                Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Normal),
                Font(DeviceFontFamilyName("sans-serif-condensed-medium"), FontWeight.Medium),
                Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Bold)
            )
            PLAYFAIR_DISPLAY -> FontFamily.Serif
            ROBOTO_MONO -> FontFamily.Monospace
            SYSTEM_SANS -> FontFamily.Cursive
        }
    }

    companion object {
        fun fromKey(key: String?): AppFontOption {
            if (key.isNullOrBlank()) return PLUS_JAKARTA
            return entries.firstOrNull {
                it.id.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true)
            } ?: PLUS_JAKARTA
        }
    }
}

/**
 * Strict Design Tokens & Compact Display Scaling for 320px–430px viewports.
 * - Colors: bg, surface, surface2, primary, onPrimary, text, textMuted, border, danger, success.
 * - Spacing scale ONLY: 4, 8, 12, 16, 24, 32.
 * - Radius scale ONLY: 8 (small), 16 (cards), 999 (pills).
 * - Type scale ONLY: Title 22/28 bold, Heading 18/24 semibold, Body 15/22 regular, Caption 12/16 regular.
 */
@Immutable
data class SecretGuardDesignTokens(
    val themeOption: PastelThemeOption = PastelThemeOption.EMERALD_MINT,
    val fontOption: AppFontOption = AppFontOption.PLUS_JAKARTA,
    val fontFamily: FontFamily = FontFamily.SansSerif,
    val isCompactMode: Boolean = false,
    val isCompact: Boolean = isCompactMode,
    val bg: Color = PastelThemeOption.EMERALD_MINT.bg,
    val surface: Color = PastelThemeOption.EMERALD_MINT.surface,
    val surface2: Color = PastelThemeOption.EMERALD_MINT.surface2,
    val primary: Color = PastelThemeOption.EMERALD_MINT.primary,
    val onPrimary: Color = PastelThemeOption.EMERALD_MINT.onPrimary,
    val text: Color = PastelThemeOption.EMERALD_MINT.text,
    val textMuted: Color = PastelThemeOption.EMERALD_MINT.textMuted,
    val border: Color = PastelThemeOption.EMERALD_MINT.border,
    val outgoingBubble: Color = PastelThemeOption.EMERALD_MINT.outgoingBubble,
    val danger: Color = RecordingRedStatus,
    val success: Color = OnlineGreenStatus,
    // Spacing tokens (4, 8, 12, 16, 24, 32)
    val space4: Dp = 4.dp,
    val space8: Dp = 8.dp,
    val space12: Dp = 12.dp,
    val space16: Dp = 16.dp,
    val space24: Dp = 24.dp,
    val space32: Dp = 32.dp,
    val sp4: Dp = 4.dp,
    val sp8: Dp = 8.dp,
    val sp12: Dp = 12.dp,
    val sp16: Dp = 16.dp,
    val sp24: Dp = 24.dp,
    val sp32: Dp = 32.dp,
    // Contextual responsive layout tokens mapped strictly from {4, 8, 12, 16, 24, 32}
    val screenPadding: Dp = 16.dp,
    val sectionGap: Dp = 24.dp,
    val cardInnerPadding: Dp = 12.dp,
    val itemGap: Dp = 8.dp,
    // Radius tokens (8, 16, 999)
    val radiusSmall: Dp = 8.dp,
    val radiusCard: Dp = 16.dp,
    val radiusPill: Dp = 999.dp,
    // Typography scale tokens (Title 22/28, Heading 18/24, Body 15/22, Caption 12/16)
    val titleSize: TextUnit = 22.sp,
    val titleLineHeight: TextUnit = 28.sp,
    val headingSize: TextUnit = 18.sp,
    val headingLineHeight: TextUnit = 24.sp,
    val bodySize: TextUnit = 15.sp,
    val bodyLineHeight: TextUnit = 22.sp,
    val captionSize: TextUnit = 12.sp,
    val captionLineHeight: TextUnit = 16.sp,
    // Component dimensions
    val topBarHeight: Dp = 56.dp,
    val bottomNavHeight: Dp = 56.dp,
    val buttonHeight: Dp = 48.dp,
    val inputHeight: Dp = 48.dp,
    val minTouchTarget: Dp = 48.dp,
    val iconSize: Dp = 24.dp,
    val chipIconSize: Dp = 20.dp,
    val avatarSizeMedium: Dp = 48.dp,
    val actionTileHeight: Dp = 96.dp
)

val LocalSecretGuardTokens = staticCompositionLocalOf { SecretGuardDesignTokens() }

// Use 16.dp for extraLarge so Material 3 AlertDialogs & Sheets render as clean cards, NEVER as giant ovals!
val SecretGuardShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

private fun buildTokenTypography(compact: Boolean, fontFamily: FontFamily): Typography {
    val titleSize = if (compact) 20.sp else 22.sp
    val headingSize = if (compact) 17.sp else 18.sp
    val bodySize = if (compact) 14.sp else 15.sp
    val captionSize = 12.sp

    return Typography(
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = titleSize,
            lineHeight = 28.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = titleSize,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = titleSize,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = headingSize,
            lineHeight = 24.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = bodySize,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = bodySize,
            lineHeight = 22.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = captionSize,
            lineHeight = 16.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = captionSize,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = captionSize,
            lineHeight = 16.sp
        )
    )
}

@Composable
fun MyApplicationTheme(
    pastelTheme: PastelThemeOption = PastelThemeOption.EMERALD_MINT,
    appFont: AppFontOption = AppFontOption.PLUS_JAKARTA,
    isCompactMode: Boolean = false,
    forceCompactMode: Boolean = isCompactMode,
    content: @Composable () -> Unit
) {
    BoxWithConstraints {
        val isSmallScreen = maxWidth < 360.dp
        val effectiveCompact = forceCompactMode || isCompactMode || isSmallScreen
        val activeFontFamily = appFont.toFontFamily()

        val tokens = SecretGuardDesignTokens(
            themeOption = pastelTheme,
            fontOption = appFont,
            fontFamily = activeFontFamily,
            isCompactMode = effectiveCompact,
            isCompact = effectiveCompact,
            bg = pastelTheme.bg,
            // Blend surface softly with pastel bg so cards and dialogs never look like stark white overlays
            surface = pastelTheme.bg,
            surface2 = pastelTheme.surface2,
            primary = pastelTheme.primary,
            onPrimary = pastelTheme.onPrimary,
            text = pastelTheme.text,
            textMuted = pastelTheme.textMuted,
            border = pastelTheme.border,
            outgoingBubble = pastelTheme.outgoingBubble,
            danger = RecordingRedStatus,
            success = OnlineGreenStatus,
            screenPadding = if (effectiveCompact) 12.dp else 16.dp,
            sectionGap = if (effectiveCompact) 16.dp else 24.dp,
            cardInnerPadding = 12.dp,
            itemGap = 8.dp,
            titleSize = if (effectiveCompact) 20.sp else 22.sp,
            titleLineHeight = 28.sp,
            headingSize = if (effectiveCompact) 17.sp else 18.sp,
            headingLineHeight = 24.sp,
            bodySize = if (effectiveCompact) 14.sp else 15.sp,
            bodyLineHeight = 22.sp,
            captionSize = 12.sp,
            captionLineHeight = 16.sp,
            avatarSizeMedium = if (effectiveCompact) 44.dp else 48.dp,
            actionTileHeight = if (effectiveCompact) 84.dp else 96.dp
        )

        val colorScheme = lightColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            primaryContainer = tokens.surface2,
            onPrimaryContainer = tokens.text,
            secondary = tokens.primary,
            onSecondary = tokens.onPrimary,
            secondaryContainer = tokens.surface2,
            onSecondaryContainer = tokens.text,
            tertiary = WarningAmberStatus,
            background = tokens.bg,
            onBackground = tokens.text,
            surface = tokens.bg,
            onSurface = tokens.text,
            surfaceVariant = tokens.surface2,
            onSurfaceVariant = tokens.textMuted,
            surfaceContainerHigh = tokens.bg,
            surfaceContainerHighest = tokens.surface2,
            error = tokens.danger,
            onError = tokens.onPrimary,
            outline = tokens.border
        )

        CompositionLocalProvider(LocalSecretGuardTokens provides tokens) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = buildTokenTypography(effectiveCompact, activeFontFamily),
                shapes = SecretGuardShapes,
                content = content
            )
        }
    }
}
