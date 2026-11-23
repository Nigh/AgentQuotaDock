package com.example.llmwatcher.data

import android.util.Base64
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.UsageMetric
import java.net.URL
import java.util.TimeZone
import javax.net.ssl.HttpsURLConnection
import org.json.JSONException
import org.json.JSONObject

internal fun fetchWebProvider(platform: LlmPlatform, credential: String): List<UsageMetric> {
    val url = when (platform) {
        LlmPlatform.CLAUDE -> "https://api.anthropic.com/api/oauth/usage"
        LlmPlatform.CURSOR -> "https://www.cursor.com/api/usage-summary"
        LlmPlatform.KIMI -> "https://www.kimi.com/apiv2/kimi.gateway.billing.v1.BillingService/GetUsages"
        else -> return emptyList()
    }
    val connection = URL(url).openConnection() as HttpsURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = false
        when (platform) {
            LlmPlatform.CLAUDE -> {
                connection.setRequestProperty("Authorization", "Bearer $credential")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("anthropic-beta", "oauth-2025-04-20")
                connection.setRequestProperty("User-Agent", "claude-code/2.1.0")
            }
            LlmPlatform.CURSOR -> {
                val cookie = if ('=' in credential) credential else "WorkosCursorSessionToken=$credential"
                connection.setRequestProperty("Cookie", cookie)
                connection.setRequestProperty("Accept", "application/json")
            }
            LlmPlatform.KIMI -> {
                val token = credential.split(';').map { it.trim() }
                    .firstOrNull { it.startsWith("kimi-auth=") }
                    ?.substringAfter('=') ?: credential
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Cookie", "kimi-auth=$token")
                connection.setRequestProperty("Origin", "https://www.kimi.com")
                connection.setRequestProperty("Referer", "https://www.kimi.com/code/console")
                connection.setRequestProperty("Accept", "*/*")
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                connection.setRequestProperty("connect-protocol-version", "1")
                connection.setRequestProperty("x-language", "en-US")
                connection.setRequestProperty("x-msh-platform", "web")
                connection.setRequestProperty("r-timezone", TimeZone.getDefault().id)
                val claims = try {
                    val payload = token.split('.').takeIf { it.size == 3 }?.get(1)
                    payload?.let { JSONObject(String(Base64.decode(it, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)) }
                } catch (_: IllegalArgumentException) {
                    null
                } catch (_: JSONException) {
                    null
                }
                listOf("device_id" to "x-msh-device-id", "ssid" to "x-msh-session-id", "sub" to "x-traffic-id").forEach { (key, header) ->
                    claims?.optString(key)?.takeIf { it.isNotBlank() }?.let { connection.setRequestProperty(header, it) }
                }
                connection.outputStream.use { it.write("""{"scope":["FEATURE_CODING"]}""".toByteArray(Charsets.UTF_8)) }
            }
            else -> error("Unsupported provider")
        }
        val status = connection.responseCode
        if (status !in 200..299) throw ProviderHttpException(status)
        return parseWebProviderUsage(platform, connection.inputStream.bufferedReader().use { it.readText() })
    } finally {
        connection.disconnect()
    }
}

internal fun parseWebProviderUsage(platform: LlmPlatform, body: String): List<UsageMetric> {
    val root = JSONObject(body)
    val metrics = mutableListOf<UsageMetric>()
    when (platform) {
        LlmPlatform.CLAUDE -> {
            fun window(key: String, label: String) {
                val value = root.optJSONObject(key) ?: return
                if (!value.isNull("utilization")) metrics.add(UsageMetric(label, value.getDouble("utilization"), 100.0, "%", value.isoResetTime("resets_at")))
            }
            window("five_hour", "5 小时用量")
            window("seven_day", "周用量")
            root.optJSONObject("extra_usage")?.let { extra ->
                if (extra.optBoolean("is_enabled") && !extra.isNull("monthly_limit")) {
                    val limit = extra.getDouble("monthly_limit")
                    if (limit > 0) {
                        val used = if (extra.isNull("used_credits")) 0.0 else extra.getDouble("used_credits")
                        val percent = if (extra.isNull("utilization")) used * 100 / limit else extra.getDouble("utilization")
                        metrics.add(UsageMetric("额外用量", percent, 100.0, "%"))
                    }
                }
            }
            window("seven_day_opus", "Opus 周用量")
            window("seven_day_sonnet", "Sonnet 周用量")
        }
        LlmPlatform.CURSOR -> {
            val usage = root.getJSONObject("individualUsage")
            listOf("plan" to "套餐用量", "onDemand" to "按需用量").forEach { (key, label) ->
                usage.optJSONObject(key)?.let { bucket ->
                    if (!bucket.isNull("totalPercentUsed")) {
                        metrics.add(UsageMetric(label, bucket.getDouble("totalPercentUsed"), 100.0, "%"))
                    } else if (!bucket.isNull("limit") && bucket.getDouble("limit") > 0) {
                        val used = if (bucket.isNull("used")) 0.0 else bucket.getDouble("used")
                        metrics.add(UsageMetric(label, used, bucket.getDouble("limit"), "美分"))
                    }
                }
            }
        }
        LlmPlatform.KIMI -> {
            val usages = root.getJSONArray("usages")
            var usage: JSONObject? = null
            for (index in 0 until usages.length()) {
                val candidate = usages.getJSONObject(index)
                if (usage == null) usage = candidate
                if (candidate.optString("scope") == "FEATURE_CODING") {
                    usage = candidate
                    break
                }
            }
            val coding = usage ?: throw JSONException("Kimi usage is missing")
            fun detail(value: JSONObject?, label: String) {
                if (value == null) return
                val limit = value.getDouble("limit")
                if (limit <= 0) return
                val used = if (!value.isNull("used")) value.getDouble("used")
                    else (limit - value.getDouble("remaining")).coerceAtLeast(0.0)
                metrics.add(UsageMetric(label, used, limit, "次", value.isoResetTime("resetTime")))
            }
            val limits = coding.optJSONArray("limits")
            if (limits != null && limits.length() > 0) detail(limits.getJSONObject(0).optJSONObject("detail"), "限速用量")
            detail(coding.optJSONObject("detail"), "周期用量")
        }
        else -> return emptyList()
    }
    if (metrics.isEmpty()) throw JSONException("Provider usage is missing")
    return metrics
}
