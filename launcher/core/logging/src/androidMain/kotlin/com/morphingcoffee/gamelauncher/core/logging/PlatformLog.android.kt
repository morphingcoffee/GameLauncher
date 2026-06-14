package com.morphingcoffee.gamelauncher.core.logging

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun mirrorLogLine(line: String) {
    // Android mirroring can be wired to Logcat later.
}
