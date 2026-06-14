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
}

expect fun createGameInstaller(downloadHttpClient: HttpClient): GameInstaller
