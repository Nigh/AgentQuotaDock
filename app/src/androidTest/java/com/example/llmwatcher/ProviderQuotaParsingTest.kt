package com.example.llmwatcher

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.llmwatcher.data.parseAntigravitySummary
import com.example.llmwatcher.data.parseCodexUsage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderQuotaParsingTest {
    @Test
    fun codexWindowsAndAntigravityRemainingAreMappedToUsedPercent() {
        val codex = parseCodexUsage(JSONObject("""{"rate_limit":{"primary_window":{"used_percent":25,"limit_window_seconds":18000},"secondary_window":{"used_percent":60,"limit_window_seconds":604800}}}"""))
        assertEquals(listOf("5 小时用量", "周用量"), codex.map { it.label })
        assertEquals(listOf(25.0, 60.0), codex.map { it.used })
        val quota = parseAntigravitySummary(JSONObject("""{"response":{"groups":[{"displayName":"Gemini","buckets":[{"remaining":{"remainingFraction":0.25}},{"remainingFraction":0.9,"disabled":true}]}]}}"""))
        assertEquals(1, quota.size)
        assertEquals(75.0, quota.single().used, 0.0)
    }
}
