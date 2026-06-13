package com.morphingcoffee.gamelauncher.core.designsystem

import java.time.LocalTime

actual fun platformClockText(): String {
    val now = LocalTime.now()
    return buildString {
        append(now.hour.toString().padStart(2, '0'))
        append(':')
        append(now.minute.toString().padStart(2, '0'))
        append(':')
        append(now.second.toString().padStart(2, '0'))
    }
}
