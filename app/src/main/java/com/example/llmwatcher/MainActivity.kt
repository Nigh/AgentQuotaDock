package com.example.llmwatcher

import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.llmwatcher.brightness.BrightnessPolicy
import com.example.llmwatcher.model.fingerprint
import com.example.llmwatcher.ui.app.LLMWatcherApp
import com.example.llmwatcher.ui.app.rememberWatcherAppState
import com.example.llmwatcher.ui.theme.LLMWatcherTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var isResumed by mutableStateOf(false)
    private var isDimmed by mutableStateOf(false)
    private var interactionCount by mutableStateOf(0L)
    private var originalBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        originalBrightness = window.attributes.screenBrightness
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideStatusBar()
        setContent {
            val appState = rememberWatcherAppState()
            val uiState = appState.uiState
            val fingerprint = uiState.usageSnapshot.fingerprint()
            LaunchedEffect(fingerprint, interactionCount, isResumed) {
                isDimmed = false
                if (isResumed) {
                    delay(BrightnessPolicy.DIM_AFTER_MILLIS)
                    isDimmed = true
                }
            }
            val targetBrightness = BrightnessPolicy.targetBrightness(
                settings = uiState.settings.brightness,
                isDimmed = isDimmed,
            )

            SideEffect {
                if (isResumed) {
                    val attributes = window.attributes
                    attributes.screenBrightness = targetBrightness
                    window.attributes = attributes
                }
            }

            LLMWatcherTheme(themePreference = uiState.settings.themePreference) {
                LLMWatcherApp(state = appState)
            }
        }
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        hideStatusBar()
    }

    override fun onPause() {
        isResumed = false
        isDimmed = false
        val attributes = window.attributes
        attributes.screenBrightness = originalBrightness
        window.attributes = attributes
        super.onPause()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetDimming()
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        resetDimming()
        return super.dispatchGenericMotionEvent(event)
    }

    private fun resetDimming() {
        isDimmed = false
        interactionCount++
    }

    private fun hideStatusBar() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LLMWatcherPreview() {
    LLMWatcherTheme {
        LLMWatcherApp(state = rememberWatcherAppState())
    }
}