package com.example.llmwatcher

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.llmwatcher.model.ThemePreference
import com.example.llmwatcher.ui.app.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreTest {
    @Test
    fun usageDisplayPersistsAndExistingSettingsRemainCompatible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("settings_store_test", Context.MODE_PRIVATE)
        preferences.edit().clear().putString("theme", ThemePreference.OLED_BLACK.name)
            .putInt("interval", 30).commit()
        val isolatedContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences
        }
        try {
            val store = SettingsStore(isolatedContext)
            val existing = store.load()
            assertFalse(existing.showRemaining)
            assertEquals(ThemePreference.OLED_BLACK, existing.themePreference)
            assertEquals(30, existing.refreshIntervalSeconds)

            store.save(existing.copy(showRemaining = true))
            val reloaded = SettingsStore(isolatedContext).load()
            assertTrue(reloaded.showRemaining)
            assertEquals(existing.themePreference, reloaded.themePreference)
            assertEquals(existing.refreshIntervalSeconds, reloaded.refreshIntervalSeconds)

            store.save(reloaded.copy(showRemaining = false))
            assertFalse(SettingsStore(isolatedContext).load().showRemaining)
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
