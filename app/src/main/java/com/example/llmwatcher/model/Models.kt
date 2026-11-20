package com.example.llmwatcher.model

import java.util.Locale

enum class LlmPlatform(
    val displayName: String,
    val credentialLabel: String,
    val description: String,
) {
    CODEX("Codex", "OAuth access token", "Codex 额度"),
    CLAUDE("Claude", "OAuth access token", "5 小时窗口与周用量"),
    ANTIGRAVITY("Antigravity", "Google OAuth access token", "模型配额"),
    DEEPSEEK("DeepSeek", "API key", "账户余额"),
    ZAI("z.ai", "API key", "使用额度"),
    MINIMAX("MiniMax", "API key", "使用额度"),
    CURSOR("Cursor", "Cookie", "使用额度"),
    KIMI("Kimi", "Cookie", "使用额度"),
    KIMI_CODE("Kimi Code API", "API key", "使用额度"),
}

enum class UsageIssueType(val title: String) {
    NETWORK("网络异常"),
    AUTHENTICATION("认证失败"),
    RATE_LIMIT("平台限流"),
    UNAVAILABLE("数据不可用"),
}

enum class UsageLevel {
    HEALTHY,
    WARNING,
    CRITICAL,
}

enum class CredentialTestState {
    SUCCESS,
    ERROR,
}

data class UsageIssue(
    val type: UsageIssueType,
    val message: String,
)

data class CredentialTestResult(
    val state: CredentialTestState,
    val message: String,
)

data class UsageMetric(
    val label: String,
    val used: Double,
    val limit: Double,
    val unit: String,
) {
    val progressFraction: Float
        get() = if (limit <= 0.0) {
            0f
        } else {
            (used / limit).toFloat().coerceIn(0f, 1f)
        }

    val usageLevel: UsageLevel
        get() = when {
            progressFraction >= 0.9f -> UsageLevel.CRITICAL
            progressFraction >= 0.7f -> UsageLevel.WARNING
            else -> UsageLevel.HEALTHY
        }

    val percentLabel: String
        get() = String.format(Locale.US, "%.0f%%", progressFraction * 100f)

    val amountLabel: String
        get() = if (limit <= 0.0) "${formatAmount(used)} $unit" else "${formatAmount(used)} / ${formatAmount(limit)} $unit"

    private fun formatAmount(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }
}

data class PlatformUsage(
    val platform: LlmPlatform,
    val metrics: List<UsageMetric>,
    val issue: UsageIssue? = null,
    val updatedAtLabel: String,
)

data class UsageSnapshot(
    val platforms: List<PlatformUsage>,
    val generatedAtLabel: String,
)

enum class ThemePreference(val label: String) {
    DARK("标准深色"),
    OLED_BLACK("纯黑 OLED"),
}

data class BrightnessSettings(
    val dimBrightness: Float = 0.18f,
    val brightBrightness: Float = 0.9f,
    val dimAfterUnchangedRefreshes: Int = 2,
) {
    fun normalized(): BrightnessSettings {
        val dim = dimBrightness.coerceIn(0.05f, 1f)
        val bright = brightBrightness.coerceIn(dim, 1f)
        return copy(
            dimBrightness = dim,
            brightBrightness = bright,
            dimAfterUnchangedRefreshes = dimAfterUnchangedRefreshes.coerceAtLeast(1),
        )
    }
}

data class PlatformSettings(
    val platform: LlmPlatform,
    val enabled: Boolean = false,
    val credential: String = "",
    val testResult: CredentialTestResult? = null,
)

data class WatcherSettings(
    val platforms: List<PlatformSettings> = LlmPlatform.entries.map { PlatformSettings(it) },
    val themePreference: ThemePreference = ThemePreference.DARK,
    val brightness: BrightnessSettings = BrightnessSettings(),
    val refreshIntervalSeconds: Int = 15,
) {
    val enabledPlatforms: List<LlmPlatform>
        get() = platforms.filter { it.enabled }.map { it.platform }
}

data class WatcherUiState(
    val settings: WatcherSettings = WatcherSettings(),
    val usageSnapshot: UsageSnapshot = UsageSnapshot(emptyList(), "尚未刷新"),
    val isSettingsOpen: Boolean = false,
    val loginPlatform: LlmPlatform? = null,
    val isRefreshing: Boolean = false,
    val isDimmed: Boolean = false,
    val unchangedRefreshes: Int = 0,
)
