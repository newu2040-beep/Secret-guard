package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Base / Default Emerald Mint Tokens (preserved for full backward compatibility)
val EmeraldPrimary = Color(0xFF079B68)
val EmeraldDarkAction = Color(0xFF087A55)
val EmeraldDeepForest = Color(0xFF055A3E)
val EmeraldLightTint = Color(0xFFE6F5EE)
val EmeraldMintCard = Color(0xFFEAF6F0)
val EmeraldOutgoingBubble = Color(0xFFD2F1DF)
val EmeraldPillBorder = Color(0xFFC5E6D6)

val WarmIvoryBackground = Color(0xFFFAF9F2)
val AlternateMintBackground = Color(0xFFF5F8F4)
val PureWhiteSurface = Color(0xFFFFFFFF)

val DeepGreenBlackText = Color(0xFF102A22)
val SecondarySlateGreenText = Color(0xFF64736B)
val MutedCaptionText = Color(0xFF88968E)

val OnlineGreenStatus = Color(0xFF16A36A)
val WarningAmberStatus = Color(0xFFF59E0B)
val RecordingRedStatus = Color(0xFFD92D20)
val EndCallRed = Color(0xFFE52520)

val CallBackdropTop = Color(0xFF1C4E4F)
val CallBackdropMid = Color(0xFF2F5E4E)
val CallBackdropBottom = Color(0xFF1D3326)

/**
 * Curated Pastel Theme Palettes for SecretGuard.
 * Strictly follows single-primary token rules with >= 4.5:1 body text contrast.
 */
enum class PastelThemeOption(
    val id: String,
    val label: String,
    val subtitle: String,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val primary: Color,
    val onPrimary: Color,
    val text: Color,
    val textMuted: Color,
    val border: Color,
    val outgoingBubble: Color
) {
    EMERALD_MINT(
        id = "emerald_mint",
        label = "Emerald Mint",
        subtitle = "Warm ivory & forest mint",
        bg = Color(0xFFFAF9F2),
        surface = Color(0xFFFFFFFF),
        surface2 = Color(0xFFEAF6F0),
        primary = Color(0xFF079B68),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF102A22),
        textMuted = Color(0xFF56665E),
        border = Color(0xFFC5E6D6),
        outgoingBubble = Color(0xFFD2F1DF)
    ),
    PASTEL_LAVENDER(
        id = "pastel_lavender",
        label = "Pastel Lavender",
        subtitle = "Soft lilac & royal violet",
        bg = Color(0xFFF8F6FF),
        surface = Color(0xFFFFFFFF),
        surface2 = Color(0xFFEEE8FF),
        primary = Color(0xFF6C47FF),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF1E1638),
        textMuted = Color(0xFF5D537A),
        border = Color(0xFFD7CAFF),
        outgoingBubble = Color(0xFFE3D9FF)
    ),
    ROSE_QUARTZ(
        id = "rose_quartz",
        label = "Rose Quartz",
        subtitle = "Blush petal & deep rose",
        bg = Color(0xFFFFF7F9),
        surface = Color(0xFFFFFFFF),
        surface2 = Color(0xFFFFE8EF),
        primary = Color(0xFFD6336C),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF2D121E),
        textMuted = Color(0xFF724B5B),
        border = Color(0xFFF8C6D8),
        outgoingBubble = Color(0xFFFFD6E4)
    ),
    SKY_BREEZE(
        id = "sky_breeze",
        label = "Sky Breeze",
        subtitle = "Cloud mist & ocean azure",
        bg = Color(0xFFF4FAFF),
        surface = Color(0xFFFFFFFF),
        surface2 = Color(0xFFE3F2FF),
        primary = Color(0xFF0277BD),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF0F2537),
        textMuted = Color(0xFF4B657A),
        border = Color(0xFFBCE0FD),
        outgoingBubble = Color(0xFFD1EBFF)
    ),
    PEACH_SUNSET(
        id = "peach_sunset",
        label = "Peach Sunset",
        subtitle = "Apricot cream & warm coral",
        bg = Color(0xFFFFF9F4),
        surface = Color(0xFFFFFFFF),
        surface2 = Color(0xFFFFEDE0),
        primary = Color(0xFFD9532F),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF2F1810),
        textMuted = Color(0xFF755347),
        border = Color(0xFFFAD1B8),
        outgoingBubble = Color(0xFFFFE1CC)
    ),
    SAGE_MATCHA(
        id = "sage_matcha",
        label = "Sage Matcha",
        subtitle = "Botanical leaf & calm sage",
        bg = Color(0xFFF6F9F4),
        surface = Color(0xFFFFFFFF),
        surface2 = Color(0xFFE6F0E0),
        primary = Color(0xFF3A7D44),
        onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF172819),
        textMuted = Color(0xFF526654),
        border = Color(0xFFC5DEC6),
        outgoingBubble = Color(0xFFD8ECD8)
    );

    val key: String get() = id
    val displayName: String get() = label

    companion object {
        fun fromId(id: String): PastelThemeOption {
            return entries.firstOrNull {
                it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true)
            } ?: EMERALD_MINT
        }

        fun fromKey(key: String?): PastelThemeOption {
            if (key.isNullOrBlank()) return EMERALD_MINT
            return fromId(key)
        }
    }
}
