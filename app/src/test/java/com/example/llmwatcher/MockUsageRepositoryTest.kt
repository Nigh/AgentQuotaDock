package com.example.llmwatcher

import com.example.llmwatcher.data.MockUsageRepository
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.UsageIssueType
import com.example.llmwatcher.model.WatcherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MockUsageRepositoryTest {
    @Test
    fun claudeCodeContainsFiveHourAndWeeklyMetrics() {
        val snapshot = MockUsageRepository().loadUsage(WatcherSettings(), tick = 1)
        val claude = snapshot.platforms.first { it.platform == LlmPlatform.CLAUDE_CODE }

        assertEquals(listOf("5 小时用量", "周用量"), claude.metrics.map { it.label })
    }

    @Test
    fun openAiCanExposeRateLimitIssue() {
        val snapshot = MockUsageRepository().loadUsage(WatcherSettings(), tick = 6)
        val openAi = snapshot.platforms.first { it.platform == LlmPlatform.OPENAI }

        assertNotNull(openAi.issue)
        assertEquals(UsageIssueType.RATE_LIMIT, openAi.issue?.type)
    }
}
