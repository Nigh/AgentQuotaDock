package com.example.llmwatcher.model

fun UsageSnapshot.fingerprint(): String {
    return platforms.joinToString(separator = "|") { platformUsage ->
        val metrics = platformUsage.metrics.joinToString(separator = ",") {
            "${it.label}:${it.used}:${it.limit}:${it.unit}"
        }
        "${platformUsage.platform.name}[$metrics]"
    }
}
