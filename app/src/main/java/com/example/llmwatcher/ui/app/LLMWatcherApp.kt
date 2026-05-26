package com.example.llmwatcher.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.llmwatcher.ui.dashboard.DashboardScreen
import com.example.llmwatcher.ui.settings.SettingsScreen

@Composable
fun LLMWatcherApp(
    state: WatcherAppState,
    modifier: Modifier = Modifier,
) {
    val uiState = state.uiState
    if (uiState.isSettingsOpen) {
        SettingsScreen(
            uiState = uiState,
            onClose = state::closeSettings,
            onPlatformEnabledChange = state::setPlatformEnabled,
            onCredentialChange = state::setCredential,
            onCredentialTest = state::testPlatformCredential,
            onThemePreferenceChange = state::setThemePreference,
            onDimBrightnessChange = state::setDimBrightness,
            onBrightBrightnessChange = state::setBrightBrightness,
            onRefreshIntervalChange = state::setRefreshInterval,
            modifier = modifier,
        )
    } else {
        DashboardScreen(
            uiState = uiState,
            onOpenSettings = state::openSettings,
            onRefresh = state::refreshUsage,
            modifier = modifier,
        )
    }
}
