package com.example.llmwatcher.model

import java.util.Locale

enum class LlmPlatform(
    val displayName: String,
    val credentialLabel: String,
    val description: String,
) {
    CLAUDE_CODE(
        displayName = "Claude Code",
        credentialLabel = "Anthropic API key / Claude token",
        description = "5 小时窗口与周用量",
    ),
    OPENAI(
        displayName = "OpenAI",
        credentialLabel = "OpenAI API key",
        description = "月度预算用量",
    ),
    GEMINI(
        displayName = "Gemini",
        credentialLabel = "Google AI Studio API key",
        description = "每日请求额度",
    ),
    CURSOR(
        displayName = "Cursor",
        credentialLabel = "Cursor session token",
        description = "团队额度与请求量",
    ),
}

enum class UsageIssueType(val title: String) {
    NETWORK("网络异常"),
    AUTHENTICATION("认证失败"),
    RATE_LIMIT("平台限流"),
    UNAVAILABLE("数据不可用"),
}

data class UsageIssue(
    val type: UsageIssueType,
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

    val percentLabel: String
        get() = String.format(Locale.US, "%.0f%%", progressFraction * 100f)

    val amountLabel: String
        get() = "${formatAmount(used)} / ${formatAmount(limit)} $unit"

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
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
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
    val enabled: Boolean = true,
    val credential: String = "",
)

data class WatcherSettings(
    val platforms: List<PlatformSettings> = LlmPlatform.entries.map { PlatformSettings(it) },
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
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
    val isDimmed: Boolean = false,
    val unchangedRefreshes: Int = 0,
)
