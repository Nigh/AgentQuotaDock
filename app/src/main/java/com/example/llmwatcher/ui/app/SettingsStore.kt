package com.example.llmwatcher.ui.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.llmwatcher.model.BrightnessSettings
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.PlatformSettings
import com.example.llmwatcher.model.ThemePreference
import com.example.llmwatcher.model.WatcherSettings
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("watcher_settings", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "watcher_credentials"

    private fun key(): javax.crypto.SecretKey {
        (keyStore.getKey(keyAlias, null) as? javax.crypto.SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    @Synchronized
    fun putSecret(name: String, value: String) {
        if (value.isEmpty()) {
            preferences.edit().remove("secret_$name").apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        preferences.edit().putString("secret_$name", Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    @Synchronized
    fun getSecret(name: String): String {
        val encoded = preferences.getString("secret_$name", null) ?: return ""
        val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, encrypted.copyOfRange(0, 12)))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(encrypted, 12, encrypted.size - 12).toString(Charsets.UTF_8)
    }

    @Synchronized
    fun replaceSecret(name: String, expected: String, replacement: String) {
        if (getSecret(name) == expected) putSecret(name, replacement)
    }

    fun load(): WatcherSettings = WatcherSettings(
        platforms = LlmPlatform.entries.map { platform ->
            PlatformSettings(platform, preferences.getBoolean("enabled_${platform.name}", false),
                getSecret(platform.name))
        },
        themePreference = ThemePreference.entries.firstOrNull { it.name == preferences.getString("theme", null) }
            ?: ThemePreference.DARK,
        brightness = BrightnessSettings(
            dimBrightness = preferences.getFloat("dim", 0.18f),
            brightBrightness = preferences.getFloat("bright", 0.9f),
        ).normalized(),
        refreshIntervalSeconds = preferences.getInt("interval", 15).coerceIn(5, 120),
        showRemaining = preferences.getBoolean("show_remaining", false),
    )

    fun save(settings: WatcherSettings) {
        preferences.edit().apply {
            settings.platforms.forEach { putBoolean("enabled_${it.platform.name}", it.enabled) }
            putString("theme", settings.themePreference.name)
            putFloat("dim", settings.brightness.dimBrightness)
            putFloat("bright", settings.brightness.brightBrightness)
            putInt("interval", settings.refreshIntervalSeconds)
            putBoolean("show_remaining", settings.showRemaining)
        }.apply()
    }
}
