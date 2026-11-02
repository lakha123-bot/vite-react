package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = SpaceBlack,
    primaryContainer = NeonCyanDark,
    onPrimaryContainer = TextPrimary,
    secondary = NeonPurple,
    onSecondary = SpaceBlack,
    secondaryContainer = NeonPurpleDark,
    onSecondaryContainer = TextPrimary,
    tertiary = NeonBlue,
    onTertiary = SpaceBlack,
    background = SpaceBlack,
    onBackground = TextPrimary,
    surface = DeepNavy,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = GlowCyan
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
