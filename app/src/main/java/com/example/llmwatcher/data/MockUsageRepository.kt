package com.example.llmwatcher.data

import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.PlatformUsage
import com.example.llmwatcher.model.UsageIssue
import com.example.llmwatcher.model.UsageIssueType
import com.example.llmwatcher.model.UsageMetric
import com.example.llmwatcher.model.UsageSnapshot
import com.example.llmwatcher.model.WatcherSettings

class MockUsageRepository : UsageRepository {
    override fun loadUsage(settings: WatcherSettings, tick: Int): UsageSnapshot {
        val platforms = settings.enabledPlatforms.ifEmpty { LlmPlatform.entries }
        return UsageSnapshot(
            platforms = platforms.map { platform -> platformUsage(platform, tick) },
            generatedAtLabel = "第 $tick 次刷新",
        )
    }

    private fun platformUsage(platform: LlmPlatform, tick: Int): PlatformUsage {
        val issue = issueFor(platform, tick)
        return PlatformUsage(
            platform = platform,
            metrics = metricsFor(platform, tick),
            issue = issue,
            updatedAtLabel = if (issue == null) "刚刚更新" else "保留上次数据",
        )
    }

    private fun metricsFor(platform: LlmPlatform, tick: Int): List<UsageMetric> {
        return when (platform) {
            LlmPlatform.CLAUDE_CODE -> listOf(
                UsageMetric(
                    label = "5 小时用量",
                    used = steppedValue(base = 38, tick = tick, step = 9, modulus = 100).toDouble(),
                    limit = 100.0,
                    unit = "%",
                ),
                UsageMetric(
                    label = "周用量",
                    used = steppedValue(base = 51, tick = tick, step = 4, modulus = 100).toDouble(),
                    limit = 100.0,
                    unit = "%",
                ),
            )

            LlmPlatform.OPENAI -> listOf(
                UsageMetric(
                    label = "月度预算",
                    used = steppedValue(base = 28, tick = tick, step = 6, modulus = 100).toDouble(),
                    limit = 100.0,
                    unit = "%",
                ),
            )

            LlmPlatform.GEMINI -> listOf(
                UsageMetric(
                    label = "今日请求",
                    used = steppedValue(base = 420, tick = tick, step = 45, modulus = 1_200).toDouble(),
                    limit = 1_200.0,
                    unit = "次",
                ),
            )

            LlmPlatform.CURSOR -> listOf(
                UsageMetric(
                    label = "团队额度",
                    used = steppedValue(base = 64, tick = tick, step = 3, modulus = 100).toDouble(),
                    limit = 100.0,
                    unit = "%",
                ),
                UsageMetric(
                    label = "今日请求",
                    used = steppedValue(base = 130, tick = tick, step = 12, modulus = 500).toDouble(),
                    limit = 500.0,
                    unit = "次",
                ),
            )
        }
    }

    private fun issueFor(platform: LlmPlatform, tick: Int): UsageIssue? {
        return when {
            platform == LlmPlatform.OPENAI && tick % 10 == 6 -> UsageIssue(
                type = UsageIssueType.RATE_LIMIT,
                message = "OpenAI 返回限流，稍后会自动重试。",
            )

            platform == LlmPlatform.GEMINI && tick % 12 == 7 -> UsageIssue(
                type = UsageIssueType.NETWORK,
                message = "当前网络不可用，正在显示上次缓存的 mock 数据。",
            )

            platform == LlmPlatform.CURSOR && tick % 14 == 9 -> UsageIssue(
                type = UsageIssueType.AUTHENTICATION,
                message = "Cursor 令牌可能已失效，请在设置中重新配置。",
            )

            platform == LlmPlatform.CLAUDE_CODE && tick % 16 == 11 -> UsageIssue(
                type = UsageIssueType.UNAVAILABLE,
                message = "Claude Code 用量接口暂不可用。",
            )

            else -> null
        }
    }

    private fun steppedValue(base: Int, tick: Int, step: Int, modulus: Int): Int {
        val raw = base + (tick / 2) * step
        return raw % modulus
    }
}
