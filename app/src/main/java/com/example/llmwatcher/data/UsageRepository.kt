package com.example.llmwatcher.data

import com.example.llmwatcher.model.UsageSnapshot
import com.example.llmwatcher.model.WatcherSettings

interface UsageRepository {
    fun loadUsage(settings: WatcherSettings, tick: Int): UsageSnapshot
}
