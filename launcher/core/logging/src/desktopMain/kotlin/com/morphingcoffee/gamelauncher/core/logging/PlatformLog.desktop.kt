package com.morphingcoffee.gamelauncher.core.logging

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun mirrorLogLine(line: String) {
    System.err.println(line)
}
