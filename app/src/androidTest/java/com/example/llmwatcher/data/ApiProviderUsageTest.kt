package com.example.llmwatcher.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.UsageMetric
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiProviderUsageTest {
    @Test
    fun deepSeekReturnsBalancesWithoutInventingBudget() {
        val metrics = parseApiProvider(LlmPlatform.DEEPSEEK, """{
            "is_available":true,"balance_infos":[{"currency":"CNY","total_balance":"12.50",
            "granted_balance":"2.50","topped_up_balance":"10.00"}]}""")
        assertEquals(listOf(UsageMetric("余额", 12.5, 0.0, "CNY")), metrics)
    }

    @Test
    fun zaiUsesCountsAndPercentageFallback() {
        val metrics = parseApiProvider(LlmPlatform.ZAI, """{"code":200,"data":{"limits":[
            {"unit":3,"usage":100,"currentValue":20,"remaining":70},
            {"unit":6,"percentage":"42.5"}]}}""")
        assertEquals(UsageMetric("5 小时用量", 30.0, 100.0, "次"), metrics[0])
        assertEquals(UsageMetric("周用量", 42.5, 100.0, "%"), metrics[1])
    }

    @Test
    fun miniMaxUsesRemainingCountsAndPercentAndSkipsDeniedModels() {
        val metrics = parseApiProvider(LlmPlatform.MINIMAX, """{"data":{"base_resp":{"status_code":0},
            "model_remains":[{"model_name":"MiniMax-M2","current_interval_total_count":100,
            "current_interval_usage_count":60,"current_weekly_remaining_percent":25},
            {"model_name":"video","current_interval_status":3,"current_interval_total_count":10,
            "current_interval_usage_count":0}]}}""")
        assertEquals(listOf(UsageMetric("MiniMax-M2 用量", 40.0, 100.0, "次"),
            UsageMetric("MiniMax-M2 周用量", 75.0, 100.0, "%")), metrics)
    }

    @Test
    fun kimiCodePrefersRatiosAndFallsBackToCounts() {
        val metrics = parseApiProvider(LlmPlatform.KIMI_CODE, """{
            "usages":{"limit_5h":{"used_ratio":0.25},"limit_month_total":{"used_ratio":0.8}},
            "limits":[{"window":{"duration":5,"timeUnit":"TIME_UNIT_HOUR"},
            "detail":{"limit":"100","used":"90"}}],
            "usage":{"limit":"200","remaining":"120"}}""")
        assertEquals(listOf(UsageMetric("5 小时用量", 25.0, 100.0, "%"),
            UsageMetric("周期用量", 80.0, 200.0, "次"), UsageMetric("月用量", 80.0, 100.0, "%")), metrics)
    }

    @Test
    fun resetTimesFollowEachProviderWindow() {
        val zai = parseApiProvider(LlmPlatform.ZAI, """{"code":200,"data":{"limits":[
            {"unit":3,"percentage":20,"nextResetTime":1704067200000},
            {"unit":6,"percentage":30,"nextResetTime":null}]}}""")
        assertEquals(listOf(1704067200000L, null), zai.map { it.resetsAtEpochMillis })
        val minimax = parseApiProvider(LlmPlatform.MINIMAX, """{"model_remains":[
            {"model_name":"M","current_interval_remaining_percent":90,"end_time":1704067200,
             "current_weekly_remaining_percent":80,"weekly_end_time":1704067200123}]}""")
        assertEquals(listOf(1704067200000L, 1704067200123L), minimax.map { it.resetsAtEpochMillis })
        val kimi = parseApiProvider(LlmPlatform.KIMI_CODE, """{
            "usages":{"limit_5h":{"used_ratio":0.5,"reset_time":"2024-01-01T00:00:00Z"}},
            "usage":{"limit":"10","used":"1","resetTime":"2024-01-01T00:00:00.5Z"}}""")
        assertEquals(listOf(1704067200000L, 1704067200500L), kimi.map { it.resetsAtEpochMillis })
        val fallback = parseApiProvider(LlmPlatform.KIMI_CODE, """{"limits":[{
            "window":{"duration":5},"detail":{"limit":"10","used":"1","reset_at":"2024-01-01T00:00:00Z"}}]}""")
        assertEquals(1704067200000L, fallback.single().resetsAtEpochMillis)
    }

    @Test
    fun otherPlatformsDoNotRequestNetwork() {
        assertTrue(fetchApiProvider(LlmPlatform.CLAUDE, "").isEmpty())
    }

    @Test(expected = JSONException::class)
    fun apiFailureThrows() {
        parseApiProvider(LlmPlatform.ZAI, """{"code":401,"success":false}""")
    }

    @Test(expected = JSONException::class)
    fun missingQuotaThrows() {
        parseApiProvider(LlmPlatform.KIMI_CODE, "{}")
    }
}
