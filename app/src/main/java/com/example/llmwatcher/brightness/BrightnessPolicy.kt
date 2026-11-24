package com.example.llmwatcher.brightness

import com.example.llmwatcher.model.BrightnessSettings

object BrightnessPolicy {
    const val DIM_AFTER_MILLIS = 30 * 60 * 1_000L

    fun targetBrightness(settings: BrightnessSettings, isDimmed: Boolean): Float {
        val normalized = settings.normalized()
        return if (isDimmed) normalized.dimBrightness else normalized.brightBrightness
    }
}
