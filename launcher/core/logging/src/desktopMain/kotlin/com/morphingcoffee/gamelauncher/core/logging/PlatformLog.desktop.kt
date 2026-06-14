package com.morphingcoffee.gamelauncher.core.logging

import java.time.Instant
import java.time.ZoneId

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun mirrorLogLine(line: String) {
    System.err.println(line)
}

internal actual fun formatLogTimestamp(timestampMillis: Long): String {
    val time = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    val millis = timestampMillis % 1_000
    return buildString {
        append(time.hour.toString().padStart(2, '0'))
        append(':')
        append(time.minute.toString().padStart(2, '0'))
        append(':')
        append(time.second.toString().padStart(2, '0'))
        append('.')
        append(millis.toString().padStart(3, '0'))
    }
}
