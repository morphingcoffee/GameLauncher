package com.morphingcoffee.gamelauncher.core.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun mirrorLogLine(line: String) {
    // Android mirroring can be wired to Logcat later.
}

internal actual fun formatLogTimestamp(timestampMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    formatter.timeZone = TimeZone.getDefault()
    return formatter.format(Date(timestampMillis))
}
