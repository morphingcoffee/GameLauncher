package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import io.ktor.client.HttpClient

expect class GameInstaller {
    suspend fun downloadAndInstall(
        gameId: String,
        version: String,
        build: GameBuild,
        onProgress: (DownloadProgress) -> Unit,
    ): Result<Unit>

    fun getInstallState(gameId: String): InstallState

    suspend fun uninstall(gameId: String): Result<Unit>

    fun getOnDiskSizeBytes(gameId: String): Long?

    fun listInstalledGames(): List<InstalledGameSummary>
}

expect fun createGameInstaller(downloadHttpClient: HttpClient): GameInstaller
