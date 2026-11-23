package com.example.llmwatcher.ui.dashboard

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun resetTimeLabel(resetAt: Long, now: Long): String {
    val time = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(resetAt))
    val seconds = ((resetAt - now).coerceAtLeast(0L) + 999L) / 1_000L
    if (seconds == 0L) return "$time 重置时间已到，等待刷新"
    val days = seconds / 86_400
    val hours = seconds % 86_400 / 3_600
    val minutes = seconds % 3_600 / 60
    val remainder = seconds % 60
    val clock = String.format(Locale.ROOT, "%02d小时 %02d分 %02d秒", hours, minutes, remainder)
    val countdown = if (days > 0) "${days}天 $clock" else clock
    return "$time 重置 · 剩余 $countdown"
}
