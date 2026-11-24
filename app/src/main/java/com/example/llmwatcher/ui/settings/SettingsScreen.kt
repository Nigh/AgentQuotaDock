package com.example.llmwatcher.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.llmwatcher.model.BrightnessSettings
import com.example.llmwatcher.model.CredentialTestState
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.PlatformSettings
import com.example.llmwatcher.model.ThemePreference
import com.example.llmwatcher.model.WatcherUiState
import com.example.llmwatcher.ui.theme.OledBackground
import com.example.llmwatcher.ui.theme.OledAwareButton as Button
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: WatcherUiState,
    onClose: () -> Unit,
    onPlatformEnabledChange: (LlmPlatform, Boolean) -> Unit,
    onCredentialChange: (LlmPlatform, String) -> Unit,
    onCredentialTest: (LlmPlatform) -> Unit,
    onLogin: (LlmPlatform) -> Unit,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onShowRemainingChange: (Boolean) -> Unit,
    onDimBrightnessChange: (Float) -> Unit,
    onBrightBrightnessChange: (Float) -> Unit,
    burnInProtectionEnabled: Boolean,
    onBurnInProtectionEnabledChange: (Boolean) -> Unit,
    onRefreshIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPage by rememberSaveable { mutableStateOf(0) }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsHeader(onClose = onClose)
            TabRow(selectedTabIndex = selectedPage) {
                listOf("账号登录", "系统设置").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedPage == index,
                        onClick = { selectedPage = index },
                        text = { Text(title) },
                    )
                }
            }
            if (selectedPage == 0) {
                PlatformSection(
                    platforms = uiState.settings.platforms,
                    onPlatformEnabledChange = onPlatformEnabledChange,
                    onCredentialChange = onCredentialChange,
                    onCredentialTest = onCredentialTest,
                    onLogin = onLogin,
                )
            } else {
                ThemeSection(
                    selected = uiState.settings.themePreference,
                    onThemePreferenceChange = onThemePreferenceChange,
                )
                UsageDisplaySection(
                    showRemaining = uiState.settings.showRemaining,
                    onShowRemainingChange = onShowRemainingChange,
                )
                BrightnessSection(
                    brightness = uiState.settings.brightness,
                    onDimBrightnessChange = onDimBrightnessChange,
                    onBrightBrightnessChange = onBrightBrightnessChange,
                )
                SectionCard(title = "防烧屏") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("防烧屏保护", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "定期移动看板内容，降低屏幕烧屏风险。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = burnInProtectionEnabled,
                            onCheckedChange = onBurnInProtectionEnabledChange,
                        )
                    }
                }
                RefreshSection(
                    refreshIntervalSeconds = uiState.settings.refreshIntervalSeconds,
                    onRefreshIntervalChange = onRefreshIntervalChange,
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "设置",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "管理账号登录与系统设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onClose) {
            Text("返回看板")
        }
    }
}

@Composable
private fun PlatformSection(
    platforms: List<PlatformSettings>,
    onPlatformEnabledChange: (LlmPlatform, Boolean) -> Unit,
    onCredentialChange: (LlmPlatform, String) -> Unit,
    onCredentialTest: (LlmPlatform) -> Unit,
    onLogin: (LlmPlatform) -> Unit,
) {
    SectionCard(title = "LLM 平台") {
        Text(
            text = "登录或填写凭据后，可单独选择在看板显示的平台。凭据使用设备密钥加密保存。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        platforms.forEach { platformSettings ->
            PlatformSettingsRow(
                platformSettings = platformSettings,
                onPlatformEnabledChange = onPlatformEnabledChange,
                onCredentialChange = onCredentialChange,
                onCredentialTest = onCredentialTest,
                onLogin = onLogin,
            )
        }
    }
}

@Composable
private fun PlatformSettingsRow(
    platformSettings: PlatformSettings,
    onPlatformEnabledChange: (LlmPlatform, Boolean) -> Unit,
    onCredentialChange: (LlmPlatform, String) -> Unit,
    onCredentialTest: (LlmPlatform) -> Unit,
    onLogin: (LlmPlatform) -> Unit,
) {
    val regional = platformSettings.platform == LlmPlatform.ZAI || platformSettings.platform == LlmPlatform.MINIMAX
    var region by remember(platformSettings.platform) {
        mutableStateOf(if (platformSettings.credential.startsWith("cn:") ||
            (!platformSettings.credential.startsWith("global:") && platformSettings.platform == LlmPlatform.ZAI)) "cn" else "global")
    }
    val apiKey = if (regional) platformSettings.credential.removePrefix("cn:").removePrefix("global:")
        else platformSettings.credential
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = platformSettings.platform.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = platformSettings.platform.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = platformSettings.enabled,
                enabled = platformSettings.credential.isNotBlank(),
                onCheckedChange = {
                    onPlatformEnabledChange(platformSettings.platform, it)
                },
            )
        }
        if (platformSettings.platform in listOf(LlmPlatform.CODEX, LlmPlatform.CLAUDE,
                LlmPlatform.ANTIGRAVITY, LlmPlatform.CURSOR, LlmPlatform.KIMI)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onLogin(platformSettings.platform) }) {
                    Text(if (platformSettings.credential.isBlank()) "登录" else "重新登录")
                }
                Text(if (platformSettings.credential.isBlank()) "未登录" else "已配置")
                OutlinedButton(onClick = { onCredentialTest(platformSettings.platform) },
                    enabled = platformSettings.credential.isNotBlank()) { Text("测试") }
                if (platformSettings.credential.isNotBlank()) {
                    OutlinedButton(onClick = {
                        onPlatformEnabledChange(platformSettings.platform, false)
                        onCredentialChange(platformSettings.platform, "")
                    }) { Text("移除") }
                }
            }
        } else {
            if (regional) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cn" to "中国区", "global" to "国际区").forEach { (value, label) ->
                        val select = {
                            region = value
                            if (apiKey.isNotBlank()) onCredentialChange(platformSettings.platform, "$value:$apiKey")
                        }
                        if (region == value) Button(onClick = select, selected = true) { Text(label) }
                        else OutlinedButton(onClick = select) { Text(label) }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        onCredentialChange(platformSettings.platform, if (regional && it.isNotBlank()) "$region:$it" else it)
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(platformSettings.platform.credentialLabel) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Password,
                    ),
                )
                OutlinedButton(onClick = { onCredentialTest(platformSettings.platform) },
                    enabled = platformSettings.credential.isNotBlank()) { Text("测试") }
            }
        }
        platformSettings.testResult?.let { result ->
            val color = when (result.state) {
                CredentialTestState.SUCCESS -> MaterialTheme.colorScheme.primary
                CredentialTestState.ERROR -> MaterialTheme.colorScheme.error
            }
            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
        }
    }
}

@Composable
private fun ThemeSection(
    selected: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
) {
    SectionCard(title = "主题模式") {
        Text(
            text = "纯黑 OLED 主题将大部分背景设为纯黑色，适合常亮放置的 OLED 屏幕手机，降低能耗与烧屏风险。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemePreference.entries.forEach { themePreference ->
                if (selected == themePreference) {
                    Button(onClick = { onThemePreferenceChange(themePreference) }, selected = true) {
                        Text(themePreference.label)
                    }
                } else {
                    OutlinedButton(onClick = { onThemePreferenceChange(themePreference) }) {
                        Text(themePreference.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageDisplaySection(
    showRemaining: Boolean,
    onShowRemainingChange: (Boolean) -> Unit,
) {
    SectionCard(title = "用量显示") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(false to "已用量", true to "剩余用量").forEach { (remaining, label) ->
                if (showRemaining == remaining) {
                    Button(onClick = { onShowRemainingChange(remaining) }, selected = true) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onShowRemainingChange(remaining) }) { Text(label) }
                }
            }
        }
    }
}

@Composable
private fun BrightnessSection(
    brightness: BrightnessSettings,
    onDimBrightnessChange: (Float) -> Unit,
    onBrightBrightnessChange: (Float) -> Unit,
) {
    SectionCard(title = "屏幕亮度") {
        BrightnessSlider(
            label = "数据静止时最低亮度",
            value = brightness.dimBrightness,
            onValueChange = onDimBrightnessChange,
        )
        BrightnessSlider(
            label = "数据变化时最高亮度",
            value = brightness.brightBrightness,
            onValueChange = onBrightBrightnessChange,
        )
        Text(
            text = "数据连续 30 分钟未变化后会自动调暗。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BrightnessSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${(value * 100f).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.05f..1f,
        )
    }
}

@Composable
private fun RefreshSection(
    refreshIntervalSeconds: Int,
    onRefreshIntervalChange: (Int) -> Unit,
) {
    SectionCard(title = "刷新频率") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "自动刷新间隔", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "$refreshIntervalSeconds 秒",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = refreshIntervalSeconds.toFloat(),
            onValueChange = { onRefreshIntervalChange(it.roundToInt()) },
            valueRange = 5f..120f,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth()
    ElevatedCard(
        modifier = if (MaterialTheme.colorScheme.background == OledBackground) {
            cardModifier.border(1.dp, MaterialTheme.colorScheme.outline, CardDefaults.elevatedShape)
        } else {
            cardModifier
        },
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}
