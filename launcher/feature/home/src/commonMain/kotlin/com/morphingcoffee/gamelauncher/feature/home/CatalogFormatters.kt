package com.morphingcoffee.gamelauncher.feature.home

import com.morphingcoffee.gamelauncher.core.model.PlatformKey

internal fun formatFileSize(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes B"
    val kb = bytes / 1_024.0
    if (kb < 1_024.0) return "${kb.toOneDecimal()} KB"
    val mb = kb / 1_024.0
    if (mb < 1_024.0) return "${mb.toOneDecimal()} MB"
    val gb = mb / 1_024.0
    return "${gb.toOneDecimal()} GB"
}

internal fun formatPlatformLabel(platformKey: String?): String =
    when (platformKey) {
        PlatformKey.WINDOWS_X64 -> "windows-x64"
        PlatformKey.MACOS_ARM64 -> "macos-arm64"
        PlatformKey.MACOS_X64 -> "macos-x64"
        null -> "unknown"
        else -> platformKey
    }

internal fun formatPlatformDisplayName(platformKey: String?): String =
    when (platformKey) {
        PlatformKey.WINDOWS_X64 -> "Windows"
        PlatformKey.MACOS_ARM64, PlatformKey.MACOS_X64 -> "macOS"
        null -> "Unknown"
        else -> platformKey
    }

private fun Double.toOneDecimal(): String {
    val scaled = (this * 10.0).toInt() / 10.0
    return if (scaled % 1.0 == 0.0) {
        scaled.toInt().toString()
    } else {
        scaled.toString()
    }
}
