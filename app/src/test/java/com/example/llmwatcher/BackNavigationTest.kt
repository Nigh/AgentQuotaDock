package com.example.llmwatcher

import com.example.llmwatcher.ui.app.shouldExitOnBack
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackNavigationTest {
    @Test fun secondBackMustArriveWithinTwoSeconds() {
        assertFalse(shouldExitOnBack(null, 1000))
        assertTrue(shouldExitOnBack(1000, 2999))
        assertFalse(shouldExitOnBack(1000, 3000))
    }
}
