package com.morphingcoffee.gamelauncher.core.telemetry

/**
 * Bounded in-memory ring buffer for process stdout/stderr tails.
 */
class ProcessOutputBuffer(
    private val maxChars: Int = DEFAULT_MAX_CHARS,
) {
    private val lock = Any()
    private val builder = StringBuilder()

    fun append(chunk: String) {
        if (chunk.isEmpty()) return
        synchronized(lock) {
            builder.append(chunk)
            if (builder.length > maxChars) {
                builder.delete(0, builder.length - maxChars)
            }
        }
    }

    fun appendLine(line: String) {
        append(if (line.endsWith('\n')) line else "$line\n")
    }

    fun snapshot(): String =
        synchronized(lock) {
            builder.toString()
        }

    fun clear() {
        synchronized(lock) {
            builder.clear()
        }
    }

    companion object {
        const val DEFAULT_MAX_CHARS = 8_192
    }
}
