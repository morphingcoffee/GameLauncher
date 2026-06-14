package com.morphingcoffee.gamelauncher.core.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AppLog {
    private const val MAX_ENTRIES = 500

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }

    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.INFO, tag, message, throwable)
    }

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.WARN, tag, message, throwable)
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    fun clear() {
        _entries.value = emptyList()
    }

    fun formatEntry(entry: LogEntry): String {
        val time = formatTimestamp(entry.timestampMillis)
        val level = entry.level.name.padEnd(5, ' ')
        val throwableSuffix = entry.throwableSummary?.let { " | $it" }.orEmpty()
        return "$time $level ${entry.tag}: ${entry.message}$throwableSuffix"
    }

    fun formatAll(): String = _entries.value.joinToString(separator = "\n") { formatEntry(it) }

    private fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val entry =
            LogEntry(
                timestampMillis = currentTimeMillis(),
                level = level,
                tag = tag,
                message = message,
                throwableSummary = throwable?.let(::summarizeThrowable),
            )
        _entries.update { current ->
            (current + entry).takeLast(MAX_ENTRIES)
        }
        mirrorLogLine(formatEntry(entry))
    }

    private fun summarizeThrowable(throwable: Throwable): String =
        buildString {
            append(throwable::class.simpleName ?: "Throwable")
            throwable.message?.let { append(": ").append(it) }
        }

    private fun formatTimestamp(timestampMillis: Long): String = formatLogTimestamp(timestampMillis)
}
