package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SecretGuardColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhiteSurface,
    primaryContainer = EmeraldMintCard,
    onPrimaryContainer = DeepGreenBlackText,
    secondary = EmeraldDarkAction,
    onSecondary = PureWhiteSurface,
    secondaryContainer = EmeraldLightTint,
    onSecondaryContainer = DeepGreenBlackText,
    tertiary = WarningAmberStatus,
    background = WarmIvoryBackground,
    onBackground = DeepGreenBlackText,
    surface = PureWhiteSurface,
    onSurface = DeepGreenBlackText,
    surfaceVariant = AlternateMintBackground,
    onSurfaceVariant = SecondarySlateGreenText,
    error = RecordingRedStatus,
    onError = PureWhiteSurface,
    outline = EmeraldPillBorder
)

val SecretGuardShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SecretGuardColorScheme,
        typography = Typography,
        shapes = SecretGuardShapes,
        content = content
    )
}
