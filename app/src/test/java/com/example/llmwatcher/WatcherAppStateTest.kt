package com.example.llmwatcher

import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.model.WatcherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatcherAppStateTest {
    @Test
    fun providersStartHiddenAndCanBeSelectedIndependently() {
        val initial = WatcherSettings()
        assertEquals(9, initial.platforms.size)
        assertTrue(initial.enabledPlatforms.isEmpty())
        val selected = initial.copy(platforms = initial.platforms.map {
            if (it.platform == LlmPlatform.DEEPSEEK) it.copy(enabled = true) else it
        })
        assertEquals(listOf(LlmPlatform.DEEPSEEK), selected.enabledPlatforms)
    }
}
