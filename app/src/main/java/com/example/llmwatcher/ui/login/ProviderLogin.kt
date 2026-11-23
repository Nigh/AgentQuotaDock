package com.example.llmwatcher.ui.login

import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.ui.theme.OledAwareButton as Button
import com.example.llmwatcher.ui.theme.OledBackground
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val CODEX_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
private const val CLAUDE_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e"
private const val GOOGLE_ID = "1071006060591-tmhssin2h21lcre235vtolojh4g403ep.apps.googleusercontent.com"
private const val CLAUDE_REDIRECT = "https://console.anthropic.com/oauth/code/callback"
private const val GOOGLE_REDIRECT = "http://127.0.0.1:8089/callback"
private const val CODEX_REDIRECT = "https://auth.openai.com/deviceauth/callback"
private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
private fun random(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
private fun post(url: String, values: Map<String, String>, json: Boolean = false): Pair<Int, String> {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = false
        connection.doOutput = true
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", if (json) "application/json" else "application/x-www-form-urlencoded")
        val body = if (json) JSONObject(values).toString() else values.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        return status to (stream?.bufferedReader()?.use { it.readText() } ?: "")
    } finally { connection.disconnect() }
}
private fun tokenEndpoint(platform: LlmPlatform) = when (platform) {
    LlmPlatform.CODEX -> "https://auth.openai.com/oauth/token"
    LlmPlatform.CLAUDE -> "https://claude.ai/v1/oauth/token"
    else -> "https://oauth2.googleapis.com/token"
}
private fun clientId(platform: LlmPlatform) = when (platform) {
    LlmPlatform.CODEX -> CODEX_ID
    LlmPlatform.CLAUDE -> CLAUDE_ID
    else -> GOOGLE_ID
}
private fun tokenRequest(platform: LlmPlatform, fields: Map<String, String>, secret: String = ""): JSONObject {
    val params = fields + ("client_id" to clientId(platform)) + if (secret.isNotEmpty()) mapOf("client_secret" to secret) else emptyMap()
    val (status, body) = post(tokenEndpoint(platform), params, platform == LlmPlatform.CLAUDE)
    check(status in 200..299) { "令牌请求失败 (HTTP $status)" }
    return JSONObject(body).also {
        check(it.optString("access_token").isNotBlank()) { "令牌响应无效" }
        it.put("expires_at", System.currentTimeMillis() / 1000 + it.optLong("expires_in", 3600))
        if (secret.isNotEmpty()) it.put("client_secret", secret)
    }
}

suspend fun accessToken(platform: LlmPlatform, stored: String, save: (String) -> Unit): String = withContext(Dispatchers.IO) {
    if (platform !in listOf(LlmPlatform.CODEX, LlmPlatform.CLAUDE, LlmPlatform.ANTIGRAVITY)) return@withContext stored
    val tokens = JSONObject(stored)
    if (tokens.optLong("expires_at") <= System.currentTimeMillis() / 1000 + 60) {
        val refresh = tokens.optString("refresh_token")
        check(refresh.isNotBlank()) { "登录已过期，请重新登录" }
        val updated = tokenRequest(platform, mapOf("grant_type" to "refresh_token", "refresh_token" to refresh), tokens.optString("client_secret"))
        if (updated.optString("refresh_token").isBlank()) updated.put("refresh_token", refresh)
        save(updated.toString())
        updated.getString("access_token")
    } else tokens.getString("access_token")
}

@Composable
fun ProviderLogin(platform: LlmPlatform, onSuccess: (String) -> Unit, onCancel: () -> Unit) {
    val isOled = MaterialTheme.colorScheme.background == OledBackground
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(platform != LlmPlatform.ANTIGRAVITY) }
    var consumed by remember { mutableStateOf(false) }
    val verifier = remember { random() }
    val state = remember { random() }
    val challenge = remember { Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }
    var authCode by remember { mutableStateOf("") }
    LaunchedEffect(platform, start) {
        if (!start || platform != LlmPlatform.CODEX) return@LaunchedEffect
        try {
            val result = withContext(Dispatchers.IO) { post("https://auth.openai.com/api/accounts/deviceauth/usercode", mapOf("client_id" to CODEX_ID, "scope" to "openid profile email offline_access", "redirect_uri" to CODEX_REDIRECT), true) }
            check(result.first in 200..299) { "获取设备码失败 (HTTP ${result.first})" }
            val data = JSONObject(result.second)
            code = data.getString("user_code")
            val id = data.getString("device_auth_id")
            val interval = data.optLong("interval", 5).coerceAtLeast(1)
            val expires = data.optLong("expires_in", 900).coerceAtMost(1800)
            val deadline = System.currentTimeMillis() + expires * 1000
            while (System.currentTimeMillis() < deadline) {
                delay(interval * 1000)
                val poll = withContext(Dispatchers.IO) { post("https://auth.openai.com/api/accounts/deviceauth/token", mapOf("client_id" to CODEX_ID, "user_code" to code, "device_auth_id" to id, "redirect_uri" to CODEX_REDIRECT), true) }
                if (poll.first == 403 || poll.first == 404) continue
                check(poll.first in 200..299) { "授权失败 (HTTP ${poll.first})" }
                val authorization = JSONObject(poll.second)
                val token = withContext(Dispatchers.IO) { tokenRequest(platform, mapOf("grant_type" to "authorization_code", "code" to authorization.getString("authorization_code"), "code_verifier" to authorization.getString("code_verifier"), "redirect_uri" to CODEX_REDIRECT)) }
                onSuccess(token.toString())
                return@LaunchedEffect
            }
            message = "设备码已过期，请重新登录"
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; message = e.message ?: "登录失败" }
    }
    LaunchedEffect(authCode) {
        if (authCode.isEmpty()) return@LaunchedEffect
        try {
            val redirect = if (platform == LlmPlatform.CLAUDE) CLAUDE_REDIRECT else GOOGLE_REDIRECT
            val fields = mutableMapOf("grant_type" to "authorization_code", "code" to authCode, "code_verifier" to verifier, "redirect_uri" to redirect)
            if (platform == LlmPlatform.CLAUDE) fields["state"] = state
            val token = withContext(Dispatchers.IO) { tokenRequest(platform, fields, secret) }
            onSuccess(token.toString())
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; message = e.message ?: "登录失败"; consumed = false }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel) { Text("返回") }
            Text("登录 ${platform.displayName}", style = MaterialTheme.typography.titleLarge)
        }
        if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
        when {
            platform == LlmPlatform.CODEX -> {
                Text(if (code.isEmpty()) "正在获取设备码…" else "设备码：$code")
                if (code.isNotEmpty()) Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://auth.openai.com/codex/device"))) }) { Text("打开授权页面") }
            }
            platform == LlmPlatform.ANTIGRAVITY && !start -> {
                OutlinedTextField(value = secret, onValueChange = { secret = it }, label = { Text("Antigravity OAuth client secret") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                Button(onClick = { start = true }, enabled = secret.isNotBlank()) { Text("继续") }
            }
            else -> {
                val cookie = platform == LlmPlatform.CURSOR || platform == LlmPlatform.KIMI
                val host = if (platform == LlmPlatform.CURSOR) "cursor.com" else "www.kimi.com"
                val loginUrl = when (platform) {
                    LlmPlatform.CURSOR -> "https://cursor.com"
                    LlmPlatform.KIMI -> "https://www.kimi.com/code"
                    LlmPlatform.CLAUDE -> "https://claude.ai/oauth/authorize?code=true&client_id=$CLAUDE_ID&response_type=code&redirect_uri=${enc(CLAUDE_REDIRECT)}&scope=${enc("org:create_api_key user:profile user:inference")}&state=$state&code_challenge=$challenge&code_challenge_method=S256"
                    else -> "https://accounts.google.com/o/oauth2/v2/auth?client_id=$GOOGLE_ID&redirect_uri=${enc(GOOGLE_REDIRECT)}&response_type=code&scope=${enc("https://www.googleapis.com/auth/cloud-platform https://www.googleapis.com/auth/userinfo.email")}&state=$state&code_challenge=$challenge&code_challenge_method=S256&access_type=offline"
                }
                if (cookie) Button(onClick = {
                    val name = if (platform == LlmPlatform.CURSOR) "WorkosCursorSessionToken" else "kimi-auth"
                    val raw = CookieManager.getInstance().getCookie("https://$host") ?: ""
                    val value = raw.split(';').map { it.trim() }.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')
                    if (value.isNullOrBlank()) message = "未找到登录会话，请先完成登录" else onSuccess(value)
                }) { Text("完成登录") }
                AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { ctx ->
                    WebView(ctx).apply {
                        if (isOled) setBackgroundColor(android.graphics.Color.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val uri = request.url
                                val redirect = if (platform == LlmPlatform.CLAUDE) Uri.parse(CLAUDE_REDIRECT) else Uri.parse(GOOGLE_REDIRECT)
                                if (!cookie && uri.scheme == redirect.scheme && uri.host == redirect.host && uri.port == redirect.port && uri.path == redirect.path) {
                                    if (!consumed && uri.getQueryParameter("state") == state && !uri.getQueryParameter("code").isNullOrBlank()) { consumed = true; authCode = uri.getQueryParameter("code").orEmpty() } else message = "授权失败或状态不匹配"
                                    return true
                                }
                                return false
                            }
                        }
                        loadUrl(loginUrl)
                    }
                }, onRelease = { it.destroy() })
            }
        }
    }
}
