package com.example.llmwatcher.data

import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.UsageMetric
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONException
import org.json.JSONObject

internal fun fetchApiProvider(platform: LlmPlatform, credential: String): List<UsageMetric> {
    val region = when {
        credential.startsWith("cn:") -> "cn"
        credential.startsWith("global:") -> "global"
        else -> null
    }
    val key = if (region == null) credential else credential.substringAfter(':')
    val url = when (platform) {
        LlmPlatform.DEEPSEEK -> "https://api.deepseek.com/user/balance"
        LlmPlatform.ZAI -> "https://${if (region == "global") "api.z.ai" else "open.bigmodel.cn"}/api/monitor/usage/quota/limit"
        LlmPlatform.MINIMAX -> "https://${if (region == "cn") "api.minimaxi.com" else "api.minimax.io"}/v1/api/openplatform/coding_plan/remains"
        LlmPlatform.KIMI_CODE -> "https://api.kimi.com/coding/v1/usages"
        else -> return emptyList()
    }
    val connection = URL(url).openConnection() as HttpsURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Authorization", "Bearer $key")
        connection.setRequestProperty("Accept", "application/json")
        if (platform == LlmPlatform.MINIMAX) {
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("MM-API-Source", "CodexMeter")
        }
        val status = connection.responseCode
        if (status !in 200..299) throw ProviderHttpException(status)
        return parseApiProvider(platform, connection.inputStream.bufferedReader().use { it.readText() })
    } finally {
        connection.disconnect()
    }
}

internal fun parseApiProvider(platform: LlmPlatform, body: String): List<UsageMetric> {
    val root = JSONObject(body)
    val metrics = when (platform) {
        LlmPlatform.DEEPSEEK -> {
            val balances = root.getJSONArray("balance_infos")
            if (balances.length() == 0) listOf(UsageMetric("余额", 0.0, 0.0, ""))
            else (0 until balances.length()).map { index ->
                val balance = balances.getJSONObject(index)
                UsageMetric("余额", balance.getDouble("total_balance"), 0.0, balance.getString("currency"))
            }
        }
        LlmPlatform.ZAI -> {
            if (!root.optBoolean("success") && root.optInt("code", -1) !in listOf(0, 200)) {
                throw JSONException("z.ai API error")
            }
            val limits = root.getJSONObject("data").getJSONArray("limits")
            (0 until limits.length()).map { index ->
                val item = limits.getJSONObject(index)
                val label = when (item.optInt("unit")) {
                    3 -> "5 小时用量"
                    6 -> "周用量"
                    else -> item.optString("type", "用量")
                }
                val limit = item.number("usage")
                val current = item.number("currentValue")
                val remaining = item.number("remaining")
                val used = if (limit != null && remaining != null) maxOf(limit - remaining, current ?: 0.0) else current
                if (limit != null && limit > 0 && used != null) UsageMetric(label, used, limit, "次")
                else UsageMetric(label, item.requiredNumber("percentage"), 100.0, "%")
            }
        }
        LlmPlatform.MINIMAX -> {
            val data = root.optJSONObject("data")
            val status = data?.optJSONObject("base_resp") ?: root.optJSONObject("base_resp")
            if (status != null && status.optInt("status_code", 0) != 0) throw JSONException("MiniMax API error")
            val models = data?.optJSONArray("model_remains")?.takeIf { it.length() > 0 }
                ?: root.getJSONArray("model_remains")
            buildList {
                for (index in 0 until models.length()) {
                    val model = models.getJSONObject(index)
                    if (model.optInt("current_interval_status") == 3) continue
                    val name = model.optString("model_name", "模型")
                    fun addWindow(prefix: String, label: String) {
                        val limit = model.number("current_${prefix}_total_count")
                        val remaining = model.number("current_${prefix}_usage_count")
                        val percent = model.number("current_${prefix}_remaining_percent")
                        if (limit != null && limit > 0 && remaining != null) {
                            add(UsageMetric("$name $label", (limit - remaining).coerceAtLeast(0.0), limit, "次"))
                        } else if (percent != null) {
                            add(UsageMetric("$name $label", (100.0 - percent).coerceIn(0.0, 100.0), 100.0, "%"))
                        }
                    }
                    addWindow("interval", "用量")
                    if (model.optInt("current_weekly_status") != 3) addWindow("weekly", "周用量")
                }
            }
        }
        LlmPlatform.KIMI_CODE -> buildList {
            val ratios = root.optJSONObject("usages")
            fun addRatio(key: String, label: String): Boolean {
                val ratio = ratios?.optJSONObject(key)?.number("used_ratio") ?: return false
                add(UsageMetric(label, (ratio * 100.0).coerceIn(0.0, 100.0), 100.0, "%"))
                return true
            }
            fun addCounts(detail: JSONObject?, label: String) {
                if (detail == null) return
                val limit = detail.number("limit") ?: return
                if (limit <= 0) return
                val used = detail.number("used") ?: detail.number("remaining")?.let { limit - it } ?: return
                add(UsageMetric(label, used.coerceAtLeast(0.0), limit, "次"))
            }
            if (!addRatio("limit_5h", "5 小时用量")) {
                val limits = root.optJSONArray("limits")
                if (limits != null && limits.length() > 0) {
                    val first = limits.getJSONObject(0)
                    if (first.optJSONObject("window") != null) addCounts(first.optJSONObject("detail"), "限额用量")
                }
            }
            if (!addRatio("limit_7d", "周用量")) addCounts(root.optJSONObject("usage"), "周期用量")
            addRatio("limit_month_total", "月用量")
        }
        else -> return emptyList()
    }
    if (metrics.isEmpty()) throw JSONException("No usage data")
    return metrics
}

private fun JSONObject.number(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key, Double.NaN).takeIf { it.isFinite() }
}

private fun JSONObject.requiredNumber(key: String): Double =
    number(key) ?: throw JSONException("Missing usage number")
