package com.example.llmwatcher

import com.example.llmwatcher.model.UsageMetric
import com.example.llmwatcher.model.UsageLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMetricTest {
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
