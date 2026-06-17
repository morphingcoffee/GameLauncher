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
    val hasInstall = uncompressedSizeBytes?.takeIf { it > 0L } != null
    val hasDownload = downloadSizeBytes?.takeIf { it > 0L } != null
    return when {
        hasInstall && hasDownload -> "SIZE"
        hasInstall -> "INSTALL"
        hasDownload -> "DOWNLOAD"
        else -> "SIZE"
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
    if (isInstalled) {
        return onDiskSizeBytes?.let { "${formatFileSize(it)} ON DISK" } ?: "NOT AVAILABLE"
    }

    val downloadBytes = downloadSizeBytes?.takeIf { it > 0L }
    val installBytes = uncompressedSizeBytes?.takeIf { it > 0L }

    if (isDownloading) {
        return when {
            downloadBytes != null && installBytes != null ->
                "${formatFileSize(downloadBytes)} DL / ${formatFileSize(installBytes)} INSTALLING"
            installBytes != null -> "${formatFileSize(installBytes)} INSTALLING"
            downloadBytes != null -> "${formatFileSize(downloadBytes)} INSTALLING"
            else -> "INSTALLING"
        }
    }

    return when {
        downloadBytes != null && installBytes != null ->
            "${formatFileSize(downloadBytes)} DL / ${formatFileSize(installBytes)} ON DISK"
        installBytes != null -> "${formatFileSize(installBytes)} ON DISK"
        downloadBytes != null -> "${formatFileSize(downloadBytes)} DL"
        else -> "NOT AVAILABLE"
    }
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
