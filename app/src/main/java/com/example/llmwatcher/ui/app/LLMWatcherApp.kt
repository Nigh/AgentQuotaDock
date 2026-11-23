package com.example.llmwatcher.ui.app

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.llmwatcher.ui.dashboard.DashboardScreen
import com.example.llmwatcher.ui.settings.SettingsScreen
import com.example.llmwatcher.ui.login.ProviderLogin

@Composable
fun LLMWatcherApp(
    state: WatcherAppState,
    modifier: Modifier = Modifier,
) {
    val uiState = state.uiState
    val loginPlatform = uiState.loginPlatform
    val context = LocalContext.current
    var lastBackAt by remember(uiState.isSettingsOpen, loginPlatform) { mutableStateOf<Long?>(null) }
    BackHandler {
        when {
            loginPlatform != null -> state.closeLogin()
            uiState.isSettingsOpen -> state.closeSettings()
            else -> {
                val now = SystemClock.elapsedRealtime()
                val previous = lastBackAt
                if (shouldExitOnBack(previous, now)) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackAt = now
                    Toast.makeText(context, "再按一次返回键退出", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    if (loginPlatform != null) {
        ProviderLogin(
            platform = loginPlatform,
            onSuccess = { state.loginSuccess(loginPlatform, it) },
            onCancel = state::closeLogin,
        )
    } else if (uiState.isSettingsOpen) {
        SettingsScreen(
            uiState = uiState,
            onClose = state::closeSettings,
            onPlatformEnabledChange = state::setPlatformEnabled,
            onCredentialChange = state::setCredential,
            onCredentialTest = state::testPlatformCredential,
            onLogin = state::openLogin,
            onThemePreferenceChange = state::setThemePreference,
            onShowRemainingChange = state::setShowRemaining,
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

internal fun shouldExitOnBack(previous: Long?, now: Long): Boolean =
    previous != null && now - previous in 0 until 2_000L
