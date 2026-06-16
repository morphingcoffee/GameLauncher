package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GameCatalogRepository(
    private val manifestRepository: ManifestRepository,
    private val gameLauncher: GameLauncher,
    private val gameInstaller: GameInstaller,
) : GameCatalogDataSource {
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    override suspend fun loadCatalog(): Result<List<GameCatalogEntry>> =
        runCatching {
            manifestRepository.fetchManifest().games
        }

    override suspend fun fetchVersionHistory(versionsUrl: String): Result<List<GameVersionEntry>> =
        runCatching {
            manifestRepository.fetchVersionIndex(versionsUrl).versions
        }

    override suspend fun downloadAndInstall(
        gameId: String,
        version: String,
        build: GameBuild,
    ): Result<Unit> {
        _downloadProgress.value = null
        return gameInstaller
            .downloadAndInstall(gameId, version, build) { progress ->
                _downloadProgress.value = progress
            }.also {
                _downloadProgress.value = null
            }
    }

    override suspend fun getInstallState(gameId: String): InstallState = gameInstaller.getInstallState(gameId)

    override suspend fun uninstallGame(gameId: String): Result<Unit> = gameInstaller.uninstall(gameId)

    override suspend fun getOnDiskSizeBytes(gameId: String): Long? =
        withContext(Dispatchers.IO) {
            gameInstaller.getOnDiskSizeBytes(gameId)
        }

    override suspend fun launchGame(gameId: String): Result<Unit> = gameLauncher.launch(gameId)

    override suspend fun openWebGame(url: String): Result<Unit> = gameLauncher.openUrl(url)

    override suspend fun listInstalledGames(): List<InstalledGameSummary> =
        withContext(Dispatchers.IO) {
            gameInstaller.listInstalledGames()
        }

    override suspend fun uninstallAllGames(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val installed = gameInstaller.listInstalledGames()
                val failures = mutableListOf<String>()
                for (game in installed) {
                    gameInstaller.uninstall(game.gameId).onFailure { failures += game.gameId }
                }
                if (failures.isNotEmpty()) {
                    error("Could not uninstall: ${failures.joinToString()}")
                }
            }
        }
}
