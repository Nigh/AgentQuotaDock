package com.example.llmwatcher

import com.example.llmwatcher.data.AntigravityProjectUnavailable
import com.example.llmwatcher.data.awaitAntigravityProject
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AntigravityProjectPollingTest {
    @Test
    fun pollingReturnsProjectAndStopsAfterFiveAttempts() {
        var attempts = 0
        val project = awaitAntigravityProject(load = { if (++attempts == 3) "project" else null }, pause = {})
        assertEquals("project", project)
        assertEquals(3, attempts)
        attempts = 0
        try {
            awaitAntigravityProject(load = { attempts++; null }, pause = {})
            fail("Expected unavailable project")
        } catch (_: AntigravityProjectUnavailable) {
            assertEquals(5, attempts)
        }
    }

    @Test(expected = InterruptedException::class)
    fun pollingPropagatesCancellation() {
        awaitAntigravityProject(load = { null }, pause = { throw InterruptedException() })
    }
}
