package com.morphingcoffee.gamelauncher.feature.home

import com.morphingcoffee.gamelauncher.core.designsystem.formatFileSize
import com.morphingcoffee.gamelauncher.core.model.PlatformKey

internal fun formatSizeDisplay(
    downloadSizeBytes: Long?,
    onDiskSizeBytes: Long?,
    isWebGame: Boolean = false,
): String {
    if (isWebGame) return "BROWSER"
    if (downloadSizeBytes == null) return "NOT AVAILABLE"
    if (onDiskSizeBytes != null && onDiskSizeBytes != downloadSizeBytes) {
        return "${formatFileSize(downloadSizeBytes)} DL / ${formatFileSize(onDiskSizeBytes)} ON DISK"
    }
    if (onDiskSizeBytes != null) {
        return formatFileSize(onDiskSizeBytes)
    }
    return formatFileSize(downloadSizeBytes)
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
        PlatformKey.WEB -> "Web"
        null -> "Unknown"
        else -> platformKey
    }
