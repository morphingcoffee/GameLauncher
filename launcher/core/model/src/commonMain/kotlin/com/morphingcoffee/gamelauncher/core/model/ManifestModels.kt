package com.morphingcoffee.gamelauncher.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Manifest(
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("launcher_minimum_version")
    val launcherMinimumVersion: String,
    val games: List<GameCatalogEntry>,
)

@Serializable
data class GameCatalogEntry(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String,
    @SerialName("latest_version")
    val latestVersion: String,
    @SerialName("versions_url")
    val versionsUrl: String,
    val builds: Map<String, GameBuild>,
) {
    fun buildForPlatform(platformKey: String): GameBuild? = builds[platformKey]

    fun buildForCurrentPlatform(): GameBuild? = PlatformKey.current()?.let(::buildForPlatform)

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
    val sha256: String,
)

@Serializable
data class GameVersionIndex(
    @SerialName("game_id")
    val gameId: String,
    val versions: List<GameVersionEntry>,
)

@Serializable
data class GameVersionEntry(
    val version: String,
    @SerialName("released_at")
    val releasedAt: String,
    val builds: Map<String, GameBuild>,
) {
    fun buildForCurrentPlatform(): GameBuild? = PlatformKey.current()?.let { builds[it] }
}
