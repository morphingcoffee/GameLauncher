package com.morphingcoffee.gamelauncher.feature.home

import com.morphingcoffee.gamelauncher.core.designsystem.formatFileSize
import com.morphingcoffee.gamelauncher.core.model.PlatformKey

internal fun formatSizeLabel(
    isWebGame: Boolean,
    isInstalled: Boolean,
    isDownloading: Boolean,
    uncompressedSizeBytes: Long?,
    downloadSizeBytes: Long?,
): String? {
    if (isWebGame) return null
    if (isInstalled || isDownloading) return "SIZE"
    return if (uncompressedSizeBytes?.takeIf { it > 0L } != null) {
        "INSTALL"
    } else {
        "DOWNLOAD"
    }
}

internal fun formatSizeDisplay(
    downloadSizeBytes: Long?,
    uncompressedSizeBytes: Long?,
    onDiskSizeBytes: Long?,
    isInstalled: Boolean = false,
    isDownloading: Boolean = false,
    isWebGame: Boolean = false,
): String {
    if (isWebGame) return "BROWSER"
    if (isDownloading) {
        val expectedBytes =
            uncompressedSizeBytes?.takeIf { it > 0L }
                ?: downloadSizeBytes?.takeIf { it > 0L }
        return expectedBytes?.let { "${formatFileSize(it)} INSTALLING" } ?: "INSTALLING"
    }
    if (isInstalled) {
        return onDiskSizeBytes?.let { "${formatFileSize(it)} ON DISK" } ?: "NOT AVAILABLE"
    }
    uncompressedSizeBytes?.takeIf { it > 0L }?.let { return "${formatFileSize(it)} ON DISK" }
    downloadSizeBytes?.takeIf { it > 0L }?.let { return "${formatFileSize(it)} DL" }
    return "NOT AVAILABLE"
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
