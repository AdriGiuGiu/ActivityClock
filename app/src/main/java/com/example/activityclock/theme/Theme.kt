package com.example.activityclock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PremiumDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    secondary = VioletNeon,
    tertiary = EmeraldNeon,
    background = BackgroundObsidian,
    surface = SurfaceObsidian,
    onPrimary = BackgroundObsidian,
    onSecondary = TextPrimary,
    onTertiary = BackgroundObsidian,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor
)

@Composable
fun ActivityClockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumDarkColorScheme,
        typography = Typography,
        content = content
    )
}
