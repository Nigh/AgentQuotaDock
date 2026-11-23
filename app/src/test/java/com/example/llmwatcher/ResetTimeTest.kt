package com.example.llmwatcher

import com.example.llmwatcher.ui.dashboard.resetTimeLabel
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetTimeTest {
    @Test fun countdownAndExpiredReset() {
        val reset = 1_700_000_000_000L
        assertTrue(resetTimeLabel(reset, reset - 90_001).contains("剩余 00小时 01分 31秒"))
        assertTrue(resetTimeLabel(reset, reset - 90_000_000).contains("剩余 1天 01小时 00分 00秒"))
        assertTrue(resetTimeLabel(reset, reset + 1).contains("重置时间已到，等待刷新"))
    }
}
