package com.example.activityclock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val PremiumLightColorScheme = lightColorScheme(
    primary = CyanNeon,
    secondary = VioletNeon,
    tertiary = EmeraldNeon,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = SurfaceLight,
    onSecondary = TextPrimaryLight,
    onTertiary = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceCardLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerColorLight
)

@Composable
fun ActivityClockTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PremiumDarkColorScheme else PremiumLightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
