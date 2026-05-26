package com.example.llmwatcher.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.llmwatcher.brightness.BrightnessPolicy
import com.example.llmwatcher.brightness.BrightnessState
import com.example.llmwatcher.data.MockUsageRepository
import com.example.llmwatcher.data.UsageRepository
import com.example.llmwatcher.model.BrightnessSettings
import com.example.llmwatcher.model.CredentialTestResult
import com.example.llmwatcher.model.CredentialTestState
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.ThemePreference
import com.example.llmwatcher.model.WatcherSettings
import com.example.llmwatcher.model.WatcherUiState
import com.example.llmwatcher.model.fingerprint
import kotlinx.coroutines.delay

class WatcherAppState(
    private val repository: UsageRepository,
) {
    var uiState by mutableStateOf(WatcherUiState())
        private set

    private var tick = 0
    private var lastFingerprint: String? = null

    init {
        refreshUsage()
    }

    fun openSettings() {
        uiState = uiState.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        uiState = uiState.copy(isSettingsOpen = false)
    }

    fun refreshUsage() {
        tick += 1
        val snapshot = repository.loadUsage(uiState.settings, tick)
        val fingerprint = snapshot.fingerprint()
        val brightnessState = BrightnessPolicy.nextState(
            previousFingerprint = lastFingerprint,
            currentFingerprint = fingerprint,
            previousState = BrightnessState(
                isDimmed = uiState.isDimmed,
                unchangedRefreshes = uiState.unchangedRefreshes,
            ),
            settings = uiState.settings.brightness,
        )
        lastFingerprint = fingerprint
        uiState = uiState.copy(
            usageSnapshot = snapshot,
            isDimmed = brightnessState.isDimmed,
            unchangedRefreshes = brightnessState.unchangedRefreshes,
        )
    }

    fun setPlatformEnabled(platform: LlmPlatform, enabled: Boolean) {
        updateSettings {
            copy(
                platforms = platforms.map {
                    if (it.platform == platform) it.copy(enabled = enabled) else it
                },
            )
        }
        refreshUsage()
    }

    fun setCredential(platform: LlmPlatform, credential: String) {
        updateSettings {
            copy(
                platforms = platforms.map {
                    if (it.platform == platform) {
                        it.copy(credential = credential, testResult = null)
                    } else {
                        it
                    }
                },
            )
        }
    }

    fun testPlatformCredential(platform: LlmPlatform) {
        updateSettings {
            copy(
                platforms = platforms.map { settings ->
                    if (settings.platform != platform) {
                        settings
                    } else if (!settings.enabled) {
                        settings.copy(
                            testResult = CredentialTestResult(
                                state = CredentialTestState.ERROR,
                                message = "请先启用该平台。",
                            ),
                        )
                    } else if (settings.credential.isBlank()) {
                        settings.copy(
                            testResult = CredentialTestResult(
                                state = CredentialTestState.ERROR,
                                message = "请先填写凭据。",
                            ),
                        )
                    } else {
                        settings.copy(
                            testResult = CredentialTestResult(
                                state = CredentialTestState.SUCCESS,
                                message = "Mock 验证通过，真实 API 接入后将在这里检查凭据有效性。",
                            ),
                        )
                    }
                },
            )
        }
    }

    fun setThemePreference(themePreference: ThemePreference) {
        updateSettings { copy(themePreference = themePreference) }
    }

    fun setDimBrightness(value: Float) {
        updateBrightness { current ->
            current.copy(dimBrightness = value.coerceAtMost(current.brightBrightness)).normalized()
        }
    }

    fun setBrightBrightness(value: Float) {
        updateBrightness { current ->
            current.copy(brightBrightness = value.coerceAtLeast(current.dimBrightness)).normalized()
        }
    }

    fun setRefreshInterval(seconds: Int) {
        updateSettings { copy(refreshIntervalSeconds = seconds.coerceIn(5, 120)) }
    }

    private fun updateBrightness(transform: (BrightnessSettings) -> BrightnessSettings) {
        updateSettings { copy(brightness = transform(brightness)) }
    }

    private fun updateSettings(transform: WatcherSettings.() -> WatcherSettings) {
        uiState = uiState.copy(settings = uiState.settings.transform())
    }
}

@Composable
fun rememberWatcherAppState(): WatcherAppState {
    val repository = remember { MockUsageRepository() }
    val state = remember { WatcherAppState(repository) }
    val refreshIntervalSeconds = state.uiState.settings.refreshIntervalSeconds

    LaunchedEffect(state, refreshIntervalSeconds) {
        while (true) {
            delay(refreshIntervalSeconds * 1_000L)
            state.refreshUsage()
        }
    }

    return state
}
