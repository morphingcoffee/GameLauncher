package com.morphingcoffee.gamelauncher.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LauncherRelease(
    @SerialName("release_notes_url")
    val releaseNotesUrl: String? = null,
    @SerialName("channels")
    val channels: Map<String, LauncherChannelBuild> = emptyMap(),
)

@Serializable
data class LauncherChannelBuild(
    @SerialName("version")
    val version: String,
    @SerialName("artifact_type")
    val artifactType: String,
    @SerialName("released_at")
    val releasedAt: String? = null,
    @SerialName("download_url")
    val downloadUrl: String,
    @SerialName("file_size_bytes")
    val fileSizeBytes: Long,
    @SerialName("sha256")
    val sha256: String,
)

object LauncherChannelKey {
    const val WINDOWS_X64_MSI = "windows-x64-msi"
    const val WINDOWS_X64_PORTABLE = "windows-x64-portable"

    fun macosDmg(platformKey: String): String? =
        when (platformKey) {
            PlatformKey.MACOS_ARM64 -> "macos-arm64-dmg"
            PlatformKey.MACOS_X64 -> "macos-x64-dmg"
            else -> null
        }

    fun macosZip(platformKey: String): String? =
        when (platformKey) {
            PlatformKey.MACOS_ARM64 -> "macos-arm64-zip"
            PlatformKey.MACOS_X64 -> "macos-x64-zip"
            else -> null
        }
}

enum class LauncherUpdateStatus {
    Supported,
    UpdateAvailable,
    UpdateRequired,
    ManualUpdateRequired,
}

data class LauncherUpdateEvaluation(
    val status: LauncherUpdateStatus,
    val channelKey: String? = null,
    val channelBuild: LauncherChannelBuild? = null,
    val releaseNotesUrl: String? = null,
)
