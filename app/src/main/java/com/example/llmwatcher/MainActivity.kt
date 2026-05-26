package com.example.llmwatcher

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.llmwatcher.brightness.BrightnessPolicy
import com.example.llmwatcher.ui.app.LLMWatcherApp
import com.example.llmwatcher.ui.app.rememberWatcherAppState
import com.example.llmwatcher.ui.theme.LLMWatcherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideStatusBar()
        setContent {
            val appState = rememberWatcherAppState()
            val uiState = appState.uiState
            val targetBrightness = BrightnessPolicy.targetBrightness(
                settings = uiState.settings.brightness,
                isDimmed = uiState.isDimmed,
            )

            SideEffect {
                val attributes = window.attributes
                attributes.screenBrightness = targetBrightness
                window.attributes = attributes
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
        hideStatusBar()
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