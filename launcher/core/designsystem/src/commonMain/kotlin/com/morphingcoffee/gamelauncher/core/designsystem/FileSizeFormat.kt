package com.morphingcoffee.gamelauncher.core.designsystem

fun formatFileSize(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes B"
    val kb = bytes / 1_024.0
    if (kb < 1_024.0) return "${kb.toOneDecimal()} KB"
    val mb = kb / 1_024.0
    if (mb < 1_024.0) return "${mb.toOneDecimal()} MB"
    val gb = mb / 1_024.0
    return "${gb.toOneDecimal()} GB"
}

private fun Double.toOneDecimal(): String {
    val scaled = (this * 10.0).toInt() / 10.0
    return if (scaled % 1.0 == 0.0) {
        scaled.toInt().toString()
    } else {
        scaled.toString()
    }
}
