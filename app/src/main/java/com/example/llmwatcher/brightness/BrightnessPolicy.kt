package com.example.llmwatcher.brightness

import com.example.llmwatcher.model.BrightnessSettings

data class BrightnessState(
    val isDimmed: Boolean,
    val unchangedRefreshes: Int,
)

object BrightnessPolicy {
    fun nextState(
        previousFingerprint: String?,
        currentFingerprint: String,
        previousState: BrightnessState,
        settings: BrightnessSettings,
    ): BrightnessState {
        val normalized = settings.normalized()
        val changed = previousFingerprint == null || previousFingerprint != currentFingerprint
        val unchangedRefreshes = if (changed) 0 else previousState.unchangedRefreshes + 1
        return BrightnessState(
            isDimmed = unchangedRefreshes >= normalized.dimAfterUnchangedRefreshes,
            unchangedRefreshes = unchangedRefreshes,
        )
    }

    fun targetBrightness(settings: BrightnessSettings, isDimmed: Boolean): Float {
        val normalized = settings.normalized()
        return if (isDimmed) normalized.dimBrightness else normalized.brightBrightness
    }
}
