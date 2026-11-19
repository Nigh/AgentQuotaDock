package com.example.llmwatcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.llmwatcher.model.ThemePreference

// ── Standard Dark color scheme (xianii-theme dark palette) ───────────────────
private val DarkColorScheme = darkColorScheme(
    primary = XianiiPrimary,
    onPrimary = XianiiOnPrimary,
    secondary = XianiiSecondary,
    onSecondary = XianiiOnSecondary,
    tertiary = XianiiTertiary,
    onTertiary = XianiiOnTertiary,
    error = XianiiError,
    onError = XianiiOnError,
    background = XianiiBackground,
    onBackground = XianiiOnSurface,
    surface = XianiiSurface,
    onSurface = XianiiOnSurface,
    surfaceVariant = XianiiSurfaceVariant,
    onSurfaceVariant = XianiiOnSurface,
    outline = XianiiOutline,
    outlineVariant = XianiiOutline,
    inverseSurface = XianiiOnSurface,
    inverseOnSurface = XianiiBackground,
    inversePrimary = XianiiPrimary,
    scrim = Color.Black.copy(alpha = 0.5f),
    surfaceContainerHighest = XianiiNeutral,
)

// ── OLED Black color scheme (pure black backgrounds for OLED displays) ────────
private val OledBlackColorScheme = darkColorScheme(
    primary = XianiiPrimary,
    onPrimary = XianiiOnPrimary,
    secondary = XianiiSecondary,
    onSecondary = XianiiOnSecondary,
    tertiary = XianiiTertiary,
    onTertiary = XianiiOnTertiary,
    error = XianiiError,
    onError = XianiiOnError,
    background = OledBackground,
    onBackground = XianiiOnSurface,
    surface = OledSurface,
    onSurface = XianiiOnSurface,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = XianiiOnSurface,
    outline = OledOutline,
    outlineVariant = OledOutlineVariant,
    inverseSurface = XianiiOnSurface,
    inverseOnSurface = OledBackground,
    inversePrimary = XianiiPrimary,
    scrim = Color.Black.copy(alpha = 0.6f),
    surfaceContainerHighest = OledSurfaceVariant,
)

@Composable
fun LLMWatcherTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themePreference) {
        ThemePreference.DARK -> DarkColorScheme
        ThemePreference.OLED_BLACK -> OledBlackColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
