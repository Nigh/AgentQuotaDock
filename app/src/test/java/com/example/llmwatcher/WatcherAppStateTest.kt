package com.example.llmwatcher

import com.example.llmwatcher.data.MockUsageRepository
import com.example.llmwatcher.model.CredentialTestState
import com.example.llmwatcher.model.LlmPlatform
import com.example.llmwatcher.ui.app.WatcherAppState
import org.junit.Assert.assertEquals
import org.junit.Test

class WatcherAppStateTest {
    @Test
    fun credentialTestRequiresCredential() {
        val state = WatcherAppState(MockUsageRepository())

        state.testPlatformCredential(LlmPlatform.OPENAI)

        val result = state.uiState.settings.platforms
            .first { it.platform == LlmPlatform.OPENAI }
            .testResult
        assertEquals(CredentialTestState.ERROR, result?.state)
    }

    @Test
    fun credentialTestPassesForMockCredential() {
        val state = WatcherAppState(MockUsageRepository())

        state.setCredential(LlmPlatform.OPENAI, "sk-test")
        state.testPlatformCredential(LlmPlatform.OPENAI)

        val result = state.uiState.settings.platforms
            .first { it.platform == LlmPlatform.OPENAI }
            .testResult
        assertEquals(CredentialTestState.SUCCESS, result?.state)
    }
}
