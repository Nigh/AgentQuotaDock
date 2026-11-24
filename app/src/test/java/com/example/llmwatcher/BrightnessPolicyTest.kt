package com.example.llmwatcher

import com.example.llmwatcher.brightness.BrightnessPolicy
import com.example.llmwatcher.model.BrightnessSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessPolicyTest {
    @Test
    fun dimsAfterThirtyMinutes() {
        assertEquals(30 * 60 * 1_000L, BrightnessPolicy.DIM_AFTER_MILLIS)
    }

    @Test
    fun targetBrightnessUsesConfiguredExtremes() {
        val settings = BrightnessSettings(
            dimBrightness = 0.2f,
            brightBrightness = 0.85f,
        )

        assertEquals(0.2f, BrightnessPolicy.targetBrightness(settings, isDimmed = true), 0.001f)
        assertEquals(0.85f, BrightnessPolicy.targetBrightness(settings, isDimmed = false), 0.001f)
    }
}
