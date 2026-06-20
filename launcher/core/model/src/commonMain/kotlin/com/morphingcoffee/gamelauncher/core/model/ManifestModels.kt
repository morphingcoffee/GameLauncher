package com.morphingcoffee.gamelauncher.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Manifest(
    @SerialName("launcher_minimum_version")
    val launcherMinimumVersion: String,
    @SerialName("launcher")
    val launcher: LauncherRelease? = null,
    @SerialName("games")
    val games: List<GameCatalogEntry>,
)

@Serializable
data class GameCatalogEntry(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("latest_version")
    val latestVersion: String,
    @SerialName("versions_url")
    val versionsUrl: String,
    @SerialName("builds")
    val builds: Map<String, GameBuild>,
    @Transient
    val versionHistory: List<GameVersionEntry> = emptyList(),
) {
    fun buildForPlatform(platformKey: String): GameBuild? = builds[platformKey]

    fun buildForCurrentPlatform(): GameBuild? =
        builds[PlatformKey.WEB] ?: PlatformKey.current()?.let(::buildForPlatform)

    fun isWebGame(): Boolean = PlatformKey.WEB in builds

    fun isAvailableOnCurrentPlatform(): Boolean = buildForCurrentPlatform() != null
}

@Serializable
data class GameBuild(
    @SerialName("download_url")
    val downloadUrl: String,
    @SerialName("executable_path")
    val executablePath: String,
    @SerialName("file_size_bytes")
    val fileSizeBytes: Long,
    @SerialName("uncompressed_size_bytes")
    val uncompressedSizeBytes: Long? = null,
    @SerialName("sha256")
    val sha256: String,
)

@Serializable
data class GameVersionIndex(
    @SerialName("game_id")
    val gameId: String,
    @SerialName("versions")
    val versions: List<GameVersionEntry>,
)

@Serializable
data class GameVersionEntry(
    @SerialName("version")
    val version: String,
    @SerialName("released_at")
    val releasedAt: String,
    @SerialName("builds")
    val builds: Map<String, GameBuild>,
) {
    fun buildForCurrentPlatform(): GameBuild? = builds[PlatformKey.WEB] ?: PlatformKey.current()?.let { builds[it] }

    fun isWebGame(): Boolean = PlatformKey.WEB in builds
}
