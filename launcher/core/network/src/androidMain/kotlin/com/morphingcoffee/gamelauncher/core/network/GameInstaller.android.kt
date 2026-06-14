package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import io.ktor.client.HttpClient

actual class GameInstaller {
    actual suspend fun downloadAndInstall(
        gameId: String,
        version: String,
        build: GameBuild,
        onProgress: (DownloadProgress) -> Unit,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("Game install is only supported on desktop"))

    actual fun getInstallState(gameId: String): InstallState = InstallState.NotInstalled
}

actual fun createGameInstaller(downloadHttpClient: HttpClient): GameInstaller = GameInstaller()
