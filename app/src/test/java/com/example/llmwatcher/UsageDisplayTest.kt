package com.example.llmwatcher

import com.example.llmwatcher.model.UsageMetric
import com.example.llmwatcher.ui.dashboard.displayedUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageDisplayTest {
    @Test fun remainingUsesQuotaAndPreservesUnboundedValues() {
        val quota = UsageMetric("窗口", 30.0, 100.0, "%")
        assertEquals(30.0, displayedUsage(quota, false).used, 0.0)
        assertEquals(70.0, displayedUsage(quota, true).used, 0.0)
        assertEquals(0.0, displayedUsage(quota.copy(used = 120.0), true).used, 0.0)
        val balance = UsageMetric("余额", 30.0, 0.0, "元")
        assertEquals(balance, displayedUsage(balance, true))
    }
}
