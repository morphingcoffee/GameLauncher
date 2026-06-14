package com.morphingcoffee.gamelauncher.core.logging

data class LogEntry(
    val timestampMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwableSummary: String? = null,
)
