package com.morphingcoffee.gamelauncher.core.logging

internal expect fun currentTimeMillis(): Long

internal expect fun mirrorLogLine(line: String)
