package com.example.llmwatcher

import com.example.llmwatcher.model.UsageMetric
import com.example.llmwatcher.model.UsageLevel
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.PlatformUsage
import com.example.llmwatcher.model.UsageSnapshot
import com.example.llmwatcher.model.fingerprint
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMetricTest {
    @Test
    fun balanceDoesNotDisplayFictitiousLimit() {
        assertEquals("12.5 CNY", UsageMetric("余额", 12.5, 0.0, "CNY").amountLabel)
    }

    @Test
    fun fingerprintIgnoresResetTime() {
        val metric = UsageMetric("5 小时用量", 25.0, 100.0, "%", 1704067200000L)
        fun snapshot(value: UsageMetric) = UsageSnapshot(
            listOf(PlatformUsage(LlmPlatform.CODEX, listOf(value), updatedAtLabel = "")), "",
        )
        assertEquals(snapshot(metric).fingerprint(), snapshot(metric.copy(resetsAtEpochMillis = 1704067300000L)).fingerprint())
    }

    @Test
    fun progressFractionClampsToOne() {
        val metric = UsageMetric(
            label = "Budget",
            used = 150.0,
            limit = 100.0,
            unit = "%",
        )

        assertEquals(1f, metric.progressFraction, 0.001f)
        assertEquals("100%", metric.percentLabel)
    }

    @Test
    fun progressFractionReturnsZeroForInvalidLimit() {
        val metric = UsageMetric(
            label = "Budget",
            used = 50.0,
            limit = 0.0,
            unit = "%",
        )

        assertEquals(0f, metric.progressFraction, 0.001f)
    }

    @Test
    fun usageLevelUsesDashboardThresholds() {
        assertEquals(
            UsageLevel.HEALTHY,
            UsageMetric(label = "Budget", used = 69.0, limit = 100.0, unit = "%").usageLevel,
        )
        assertEquals(
            UsageLevel.WARNING,
            UsageMetric(label = "Budget", used = 70.0, limit = 100.0, unit = "%").usageLevel,
        )
        assertEquals(
            UsageLevel.CRITICAL,
            UsageMetric(label = "Budget", used = 90.0, limit = 100.0, unit = "%").usageLevel,
        )
    }
}
