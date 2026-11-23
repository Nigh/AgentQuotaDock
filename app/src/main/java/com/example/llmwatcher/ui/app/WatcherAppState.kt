package com.example.llmwatcher.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.llmwatcher.brightness.BrightnessPolicy
import com.example.llmwatcher.brightness.BrightnessState
import com.example.llmwatcher.data.RealUsageRepository
import com.example.llmwatcher.data.UsageRepository
import com.example.llmwatcher.model.*
import com.example.llmwatcher.ui.login.accessToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

class WatcherAppState(
    private val repository: UsageRepository,
    private val store: SettingsStore,
    private val scope: CoroutineScope,
) {
    var uiState by mutableStateOf(WatcherUiState(settings = store.load()))
        private set
    private var refreshJob: Job? = null
    private var lastFingerprint: String? = null
    private val tokenMutex = Mutex()
    private var generation = 0

    fun openSettings() { uiState = uiState.copy(isSettingsOpen = true) }
    fun closeSettings() { uiState = uiState.copy(isSettingsOpen = false) }
    fun openLogin(platform: LlmPlatform) { uiState = uiState.copy(loginPlatform = platform) }
    fun closeLogin() { uiState = uiState.copy(loginPlatform = null) }

    fun refreshUsage() {
        if (refreshJob?.isActive == true) return
        val current = ++generation
        refreshJob?.cancel()
        val settings = uiState.settings
        uiState = uiState.copy(isRefreshing = true)
        refreshJob = scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { loadUsage(settings) }
                if (current != generation) return@launch
                val fingerprint = snapshot.fingerprint()
                val brightness = BrightnessPolicy.nextState(
                    previousFingerprint = lastFingerprint,
                    currentFingerprint = fingerprint,
                    previousState = BrightnessState(uiState.isDimmed, uiState.unchangedRefreshes),
                    settings = uiState.settings.brightness,
                )
                lastFingerprint = fingerprint
                uiState = uiState.copy(usageSnapshot = snapshot, isDimmed = brightness.isDimmed,
                    unchangedRefreshes = brightness.unchangedRefreshes)
            } finally {
                if (current == generation) uiState = uiState.copy(isRefreshing = false)
            }
        }
    }

    fun setPlatformEnabled(platform: LlmPlatform, enabled: Boolean) {
        updateSettings { copy(platforms = platforms.map {
            if (it.platform == platform) it.copy(enabled = enabled) else it
        }) }
        refreshJob?.cancel()
        refreshUsage()
    }

    fun setCredential(platform: LlmPlatform, credential: String) {
        store.putSecret(platform.name, credential)
        updateSettings { copy(platforms = platforms.map {
            if (it.platform == platform) it.copy(credential = credential,
                enabled = it.enabled && credential.isNotBlank(), testResult = null) else it
        }) }
        if (credential.isBlank()) {
            refreshJob?.cancel()
            refreshUsage()
        }
    }

    fun loginSuccess(platform: LlmPlatform, credential: String) {
        setCredential(platform, credential)
        closeLogin()
        setPlatformEnabled(platform, true)
    }

    fun testPlatformCredential(platform: LlmPlatform) {
        val selected = uiState.settings.platforms.first { it.platform == platform }
        if (selected.credential.isBlank()) {
            setTestResult(platform, CredentialTestResult(CredentialTestState.ERROR, "请先登录或填写凭据。"))
            return
        }
        val settings = uiState.settings.copy(platforms = listOf(selected.copy(enabled = true)))
        scope.launch {
            val usage = withContext(Dispatchers.IO) { loadUsage(settings).platforms.firstOrNull() }
            if (uiState.settings.platforms.first { it.platform == platform }.credential != selected.credential) return@launch
            val issue = usage?.issue
            setTestResult(platform, if (usage != null && issue == null) {
                CredentialTestResult(CredentialTestState.SUCCESS, "连接成功。")
            } else {
                CredentialTestResult(CredentialTestState.ERROR, issue?.message ?: "未能获取平台用量。")
            })
        }
    }

    private fun setTestResult(platform: LlmPlatform, result: CredentialTestResult) {
        updateSettings { copy(platforms = platforms.map {
            if (it.platform == platform) it.copy(testResult = result) else it
        }) }
    }

    private suspend fun loadUsage(settings: WatcherSettings): UsageSnapshot {
        val failures = mutableMapOf<LlmPlatform, PlatformUsage>()
        val platforms = settings.platforms.map { setting ->
            if (!setting.enabled || setting.credential.isBlank()) return@map setting
            try {
                val credential = tokenMutex.withLock {
                    val stored = store.getSecret(setting.platform.name)
                    accessToken(setting.platform, stored) { replacement ->
                        store.replaceSecret(setting.platform.name, stored, replacement)
                    }
                }
                setting.copy(credential = credential)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val type = if (error is IOException) UsageIssueType.NETWORK else UsageIssueType.AUTHENTICATION
                failures[setting.platform] = PlatformUsage(setting.platform, emptyList(),
                    UsageIssue(type, type.title), "更新失败")
                setting.copy(enabled = false)
            }
        }
        val snapshot = runInterruptible(Dispatchers.IO) { repository.loadUsage(settings.copy(platforms = platforms), 0) }
        val results = snapshot.platforms.associateBy { it.platform } + failures
        return snapshot.copy(platforms = settings.platforms.mapNotNull { results[it.platform] })
    }

    fun setThemePreference(value: ThemePreference) { updateSettings { copy(themePreference = value) } }
    fun setShowRemaining(value: Boolean) { updateSettings { copy(showRemaining = value) } }
    fun setDimBrightness(value: Float) { updateBrightness { it.copy(dimBrightness = value.coerceAtMost(it.brightBrightness)).normalized() } }
    fun setBrightBrightness(value: Float) { updateBrightness { it.copy(brightBrightness = value.coerceAtLeast(it.dimBrightness)).normalized() } }
    fun setRefreshInterval(seconds: Int) { updateSettings { copy(refreshIntervalSeconds = seconds.coerceIn(5, 120)) } }
    private fun updateBrightness(transform: (BrightnessSettings) -> BrightnessSettings) {
        updateSettings { copy(brightness = transform(brightness)) }
    }
    private fun updateSettings(transform: WatcherSettings.() -> WatcherSettings) {
        val settings = uiState.settings.transform()
        store.save(settings)
        uiState = uiState.copy(settings = settings)
    }
}

@Composable
fun rememberWatcherAppState(): WatcherAppState {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val state = remember { WatcherAppState(RealUsageRepository(), SettingsStore(context), scope) }
    val interval = state.uiState.settings.refreshIntervalSeconds
    LaunchedEffect(state, interval) {
        state.refreshUsage()
        while (true) {
            delay(interval * 1_000L)
            state.refreshUsage()
        }
    }
    return state
}
