package com.example.llmwatcher

import com.example.llmwatcher.brightness.BrightnessPolicy
import com.example.llmwatcher.brightness.BrightnessState
import com.example.llmwatcher.model.BrightnessSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrightnessPolicyTest {
    @Test
    fun dimsAfterConfiguredUnchangedRefreshes() {
        val settings = BrightnessSettings(dimAfterUnchangedRefreshes = 2)

        val first = BrightnessPolicy.nextState(
            previousFingerprint = null,
            currentFingerprint = "same",
            previousState = BrightnessState(isDimmed = false, unchangedRefreshes = 0),
            settings = settings,
        )
        val second = BrightnessPolicy.nextState(
            previousFingerprint = "same",
            currentFingerprint = "same",
            previousState = first,
            settings = settings,
        )
        val third = BrightnessPolicy.nextState(
            previousFingerprint = "same",
            currentFingerprint = "same",
            previousState = second,
            settings = settings,
        )

        assertFalse(first.isDimmed)
        assertFalse(second.isDimmed)
        assertTrue(third.isDimmed)
        assertEquals(2, third.unchangedRefreshes)
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
