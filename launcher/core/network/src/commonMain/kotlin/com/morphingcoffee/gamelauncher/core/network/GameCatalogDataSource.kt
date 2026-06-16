package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry
import kotlinx.coroutines.flow.StateFlow

interface GameCatalogDataSource {
    val downloadProgress: StateFlow<DownloadProgress?>

    suspend fun loadCatalog(): Result<List<GameCatalogEntry>>

    suspend fun fetchVersionHistory(versionsUrl: String): Result<List<GameVersionEntry>>

    suspend fun downloadAndInstall(
        gameId: String,
        version: String,
        build: GameBuild,
    ): Result<Unit>

    suspend fun getInstallState(gameId: String): InstallState

    suspend fun uninstallGame(gameId: String): Result<Unit>

    suspend fun getOnDiskSizeBytes(gameId: String): Long?

    suspend fun launchGame(gameId: String): Result<Unit>

    suspend fun openWebGame(url: String): Result<Unit>

    suspend fun listInstalledGames(): List<InstalledGameSummary> = emptyList()

    suspend fun uninstallAllGames(): Result<Unit>
}
