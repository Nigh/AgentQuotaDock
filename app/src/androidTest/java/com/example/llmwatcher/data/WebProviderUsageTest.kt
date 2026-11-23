package com.example.llmwatcher.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.UsageMetric
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebProviderUsageTest {
    @Test
    fun claudeParsesWindowsAndEnabledExtraUsage() {
        val body = """{
            "five_hour":{"utilization":62.5},
            "seven_day":{"utilization":40},
            "extra_usage":{"is_enabled":true,"monthly_limit":10000,"used_credits":2500},
            "seven_day_opus":{"utilization":12},
            "seven_day_sonnet":null
        }"""
        assertEquals(
            listOf(
                UsageMetric("5 小时用量", 62.5, 100.0, "%"),
                UsageMetric("周用量", 40.0, 100.0, "%"),
                UsageMetric("额外用量", 25.0, 100.0, "%"),
                UsageMetric("Opus 周用量", 12.0, 100.0, "%"),
            ),
            parseWebProviderUsage(LlmPlatform.CLAUDE, body),
        )
    }

    @Test
    fun claudeAndKimiReadResetTimesWithoutInventingMissingValues() {
        val claude = parseWebProviderUsage(LlmPlatform.CLAUDE, """{
            "five_hour":{"utilization":25,"resets_at":"2024-01-01T00:00:00.123456Z"},
            "seven_day":{"utilization":30,"resets_at":"2024-01-01T02:00:00+02:00"},
            "seven_day_opus":{"utilization":0,"resets_at":null}}""")
        assertEquals(listOf(1704067200123L, 1704067200000L, null), claude.map { it.resetsAtEpochMillis })
        val kimi = parseWebProviderUsage(LlmPlatform.KIMI, """{"usages":[{"scope":"FEATURE_CODING",
            "detail":{"limit":"100","used":"1","resetTime":"2024-01-01T00:00:00Z"},
            "limits":[{"detail":{"limit":"10","used":"1","resetTime":"invalid"}}]}]}""")
        assertEquals(listOf(null, 1704067200000L), kimi.map { it.resetsAtEpochMillis })
    }

    @Test
    fun cursorPrefersPercentAndFallsBackToCents() {
        val body = """{"individualUsage":{
            "plan":{"used":0,"limit":0,"totalPercentUsed":35.5},
            "onDemand":{"used":2500,"limit":10000}
        }}"""
        assertEquals(
            listOf(
                UsageMetric("套餐用量", 35.5, 100.0, "%"),
                UsageMetric("按需用量", 2500.0, 10000.0, "美分"),
            ),
            parseWebProviderUsage(LlmPlatform.CURSOR, body),
        )
    }

    @Test
    fun kimiSelectsCodingScopeAndCalculatesMissingUsed() {
        val body = """{"usages":[
            {"scope":"OTHER","detail":{"limit":"1","used":"1"}},
            {"scope":"FEATURE_CODING","detail":{"limit":"100","remaining":"80"},
             "limits":[{"detail":{"limit":"50","used":"7","remaining":"43"}}]}
        ]}"""
        assertEquals(
            listOf(
                UsageMetric("限速用量", 7.0, 50.0, "次"),
                UsageMetric("周期用量", 20.0, 100.0, "次"),
            ),
            parseWebProviderUsage(LlmPlatform.KIMI, body),
        )
    }

    @Test
    fun missingOrInvalidUsageFails() {
        for (platform in listOf(LlmPlatform.CLAUDE, LlmPlatform.CURSOR, LlmPlatform.KIMI)) {
            assertThrows(JSONException::class.java) { parseWebProviderUsage(platform, "{}") }
            assertThrows(JSONException::class.java) { parseWebProviderUsage(platform, "not json") }
        }
    }

    @Test
    fun otherPlatformsDoNotMakeRequests() {
        assertEquals(emptyList<UsageMetric>(), fetchWebProvider(LlmPlatform.CODEX, ""))
    }
}
