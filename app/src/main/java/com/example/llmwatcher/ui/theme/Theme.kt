package com.example.llmwatcher.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    onBackground = OledOnSurface,
    surface = OledSurface,
    onSurface = OledOnSurface,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledOnSurfaceVariant,
    outline = OledOutline,
    outlineVariant = OledOutlineVariant,
    inverseSurface = OledOnSurface,
    inverseOnSurface = OledBackground,
    inversePrimary = XianiiPrimary,
    scrim = Color.Black.copy(alpha = 0.6f),
    surfaceTint = Color.Transparent,
    surfaceDim = OledSurface,
    surfaceBright = OledSurface,
    surfaceContainerLowest = OledSurface,
    surfaceContainerLow = OledSurface,
    surfaceContainer = OledSurface,
    surfaceContainerHigh = OledSurface,
    surfaceContainerHighest = OledSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DE),
    onPrimaryContainer = Color(0xFF3E0010),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0DBFF),
    onSecondaryContainer = Color(0xFF290C42),
    tertiary = LightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA9F2E5),
    onTertiaryContainer = Color(0xFF00201C),
    error = LightError,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFD4C2C5),
    inverseSurface = Color(0xFF362F30),
    inverseOnSurface = Color(0xFFFBEEEE),
    inversePrimary = XianiiPrimary,
    scrim = Color.Black,
    surfaceContainerHighest = Color(0xFFECE0E0),
)

@Composable
fun LLMWatcherTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themePreference) {
        ThemePreference.DARK -> DarkColorScheme
        ThemePreference.OLED_BLACK -> OledBlackColorScheme
        ThemePreference.LIGHT -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalTonalElevationEnabled provides
            (themePreference != ThemePreference.OLED_BLACK && LocalTonalElevationEnabled.current),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

@Composable
fun OledAwareButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    if (MaterialTheme.colorScheme.background == OledBackground) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            border = BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            ),
            content = content,
        )
    } else {
        Button(onClick = onClick, enabled = enabled, content = content)
    }
}
