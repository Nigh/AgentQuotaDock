package com.example.llmwatcher.data

import android.util.Base64
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.PlatformUsage
import com.example.llmwatcher.model.UsageIssue
import com.example.llmwatcher.model.UsageIssueType
import com.example.llmwatcher.model.UsageMetric
import com.example.llmwatcher.model.UsageSnapshot
import com.example.llmwatcher.model.WatcherSettings
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

internal class ProviderHttpException(val status: Int) : IOException()
internal class AntigravityProjectUnavailable : Exception()

class RealUsageRepository : UsageRepository {
    override fun loadUsage(settings: WatcherSettings, tick: Int): UsageSnapshot {
        val platforms = settings.platforms.filter { it.enabled }.map { setting ->
            try {
                if (setting.credential.isBlank()) throw ProviderHttpException(401)
                val metrics = when (setting.platform) {
                    LlmPlatform.CODEX -> fetchCodex(setting.credential)
                    LlmPlatform.ANTIGRAVITY -> fetchAntigravity(setting.credential)
                    LlmPlatform.DEEPSEEK, LlmPlatform.ZAI, LlmPlatform.MINIMAX, LlmPlatform.KIMI_CODE ->
                        fetchApiProvider(setting.platform, setting.credential)
                    else -> fetchWebProvider(setting.platform, setting.credential)
                }
                if (metrics.isEmpty()) throw IllegalArgumentException()
                PlatformUsage(setting.platform, metrics, updatedAtLabel = "刚刚更新")
            } catch (error: Exception) {
                if (error is InterruptedException || error is java.util.concurrent.CancellationException) throw error
                val type = when (error) {
                    is ProviderHttpException -> when (error.status) {
                        401, 403 -> UsageIssueType.AUTHENTICATION
                        429 -> UsageIssueType.RATE_LIMIT
                        else -> UsageIssueType.UNAVAILABLE
                    }
                    is IOException -> UsageIssueType.NETWORK
                    else -> UsageIssueType.UNAVAILABLE
                }
                val message = if (error is AntigravityProjectUnavailable) "Antigravity 项目尚未创建，请稍后重试" else type.title
                PlatformUsage(setting.platform, emptyList(), UsageIssue(type, message), "更新失败")
            }
        }
        return UsageSnapshot(platforms, "刚刚更新")
    }
}

private fun request(url: String, token: String, body: JSONObject? = null, accountId: String? = null): JSONObject {
    if (Thread.currentThread().isInterrupted) throw InterruptedException()
    val connection = URL(url).openConnection() as HttpsURLConnection
    try {
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "antigravity/hub/2.9.1 android/arm64")
        if (accountId != null) connection.setRequestProperty("ChatGPT-Account-Id", accountId)
        if (body != null) {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        val status = connection.responseCode
        if (status !in 200..299) throw ProviderHttpException(status)
        return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
    } finally {
        connection.disconnect()
    }
}

private fun fetchCodex(token: String): List<UsageMetric> {
    val accountId = try {
        val payload = token.split('.')[1]
        val claims = JSONObject(String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8))
        claims.optJSONObject("https://api.openai.com/auth")?.optString("chatgpt_account_id")?.takeIf { it.isNotBlank() }
    } catch (_: Exception) { null }
    val response = request("https://chatgpt.com/backend-api/wham/usage", token, accountId = accountId)
    return parseCodexUsage(response)
}

internal fun parseCodexUsage(response: JSONObject): List<UsageMetric> {
    val rate = response.optJSONObject("rate_limit") ?: return emptyList()
    return listOf("primary_window" to "主要窗口", "secondary_window" to "次要窗口").mapNotNull { (key, label) ->
        val window = rate.optJSONObject(key) ?: return@mapNotNull null
        if (!window.has("used_percent") || window.isNull("used_percent")) return@mapNotNull null
        val value = window.getDouble("used_percent")
        if (!value.isFinite()) throw IllegalArgumentException()
        val seconds = window.optInt("limit_window_seconds")
        val name = when (seconds) { 18000 -> "5 小时用量"; 604800 -> "周用量"; else -> label }
        UsageMetric(name, value, 100.0, "%")
    }
}

private fun fetchAntigravity(token: String): List<UsageMetric> {
    val base = "https://cloudcode-pa.googleapis.com/v1internal:"
    val metadata = JSONObject().put("ideType", "ANTIGRAVITY").put("platform", "PLATFORM_UNSPECIFIED").put("pluginType", "GEMINI")
    val assist = request(base + "loadCodeAssist", token, JSONObject().put("metadata", metadata))
    var project = assist.optJSONObject("cloudaicompanionProject")?.optString("value")?.takeIf { it.isNotBlank() }
    if (project == null) {
        val tiers = assist.optJSONArray("allowedTiers")
        val allowed = (0 until (tiers?.length() ?: 0)).mapNotNull { tiers?.optJSONObject(it) }
        val tier = (allowed.firstOrNull { it.optBoolean("isDefault") && it.optString("id").isNotBlank() }
            ?: allowed.firstOrNull { it.optString("id").isNotBlank() }
            ?: assist.optJSONObject("paidTier")
            ?: assist.optJSONObject("currentTier"))?.optString("id")?.takeIf { it.isNotBlank() }
        val onboarding = JSONObject().put("metadata", metadata).apply { if (tier != null) put("tierId", tier) }
        val response = request(base + "onboardUser", token, onboarding)
        project = response.optJSONObject("response")?.optJSONObject("cloudaicompanionProject")
            ?.optString("value")?.takeIf { it.isNotBlank() }
        if (project == null) {
            project = awaitAntigravityProject(load = {
                request(base + "loadCodeAssist", token, JSONObject().put("metadata", metadata))
                    .optJSONObject("cloudaicompanionProject")?.optString("value")?.takeIf { it.isNotBlank() }
            })
        }
    }
    val projectBody = JSONObject().apply { if (project != null) put("project", project) }
    val summary = try {
        request("https://daily-cloudcode-pa.googleapis.com/v1internal:retrieveUserQuotaSummary", token, projectBody)
    } catch (error: ProviderHttpException) {
        if (error.status == 401 || error.status == 403) throw error
        null
    } catch (_: IOException) { null }
    val metrics = summary?.let(::parseAntigravitySummary).orEmpty()
    if (metrics.isNotEmpty()) return metrics
    val models = request(base + "fetchAvailableModels", token, projectBody).getJSONObject("models")
    val result = mutableListOf<UsageMetric>()
    val names = models.keys()
    while (names.hasNext()) {
        val id = names.next()
        val model = models.getJSONObject(id)
        val quota = model.optJSONObject("quotaInfo") ?: continue
        if (!quota.has("remainingFraction") || quota.isNull("remainingFraction")) continue
        val remaining = quota.getDouble("remainingFraction")
        if (!remaining.isFinite()) throw IllegalArgumentException()
        result += UsageMetric(model.optString("displayName").ifBlank { id }, (1 - remaining) * 100, 100.0, "%")
    }
    if (result.isNotEmpty() && result.all { it.used <= 0.1 }) {
        val buckets = request(base + "retrieveUserQuota", token, projectBody).optJSONArray("buckets") ?: return emptyList()
        val byModel = mutableMapOf<String, Double>()
        for (i in 0 until buckets.length()) {
            val bucket = buckets.getJSONObject(i)
            val id = bucket.optString("modelId")
            if (id.isNotBlank() && bucket.has("remainingFraction") && !bucket.isNull("remainingFraction")) {
                val remaining = bucket.getDouble("remainingFraction")
                if (!remaining.isFinite()) throw IllegalArgumentException()
                byModel[id] = remaining
            }
        }
        if (byModel.isEmpty()) return emptyList()
        return byModel.map { (id, remaining) ->
            val label = models.optJSONObject(id)?.optString("displayName")?.ifBlank { id } ?: id
            UsageMetric(label, (1 - remaining) * 100, 100.0, "%")
        }
    }
    return result
}

internal fun awaitAntigravityProject(load: () -> String?, pause: () -> Unit = { Thread.sleep(2_000) }): String {
    repeat(5) {
        pause()
        load()?.takeIf { it.isNotBlank() }?.let { return it }
    }
    throw AntigravityProjectUnavailable()
}

internal fun parseAntigravitySummary(root: JSONObject): List<UsageMetric> {
    val payload = root.optJSONObject("response") ?: root.optJSONObject("summary") ?: root
    val groups = payload.optJSONArray("groups") ?: return emptyList()
    val result = mutableListOf<UsageMetric>()
    for (i in 0 until groups.length()) {
        val group = groups.getJSONObject(i)
        val buckets = group.optJSONArray("buckets") ?: continue
        for (j in 0 until buckets.length()) {
            val bucket = buckets.getJSONObject(j)
            if (bucket.optBoolean("disabled")) continue
            val source = if (bucket.has("remainingFraction")) bucket else bucket.optJSONObject("remaining") ?: continue
            if (!source.has("remainingFraction") || source.isNull("remainingFraction")) continue
            val remaining = source.getDouble("remainingFraction")
            if (!remaining.isFinite()) throw IllegalArgumentException()
            val label = bucket.optString("displayName").ifBlank { bucket.optString("name").ifBlank { group.optString("displayName").ifBlank { "配额" } } }
            result += UsageMetric(label, (1 - remaining) * 100, 100.0, "%")
        }
    }
    return result
}
