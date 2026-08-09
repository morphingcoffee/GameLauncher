package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

fun interface BlockingInstallStateProbe {
    fun probe(gameId: String): InstallState
}

fun interface DownloadInstallRunner {
    suspend fun downloadAndInstall(
        gameId: String,
        version: String,
        build: GameBuild,
        onProgress: (DownloadProgress) -> Unit,
    ): Result<Unit>
}

class GameCatalogRepository(
    private val manifestRepository: ManifestRepository,
    private val gameLauncher: GameLauncher,
    private val gameInstaller: GameInstaller,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val installStateProbe: BlockingInstallStateProbe =
        BlockingInstallStateProbe { gameInstaller.getInstallState(it) },
    private val downloadInstallRunner: DownloadInstallRunner =
        DownloadInstallRunner { gameId, version, build, onProgress ->
            gameInstaller.downloadAndInstall(gameId, version, build, onProgress)
        },
) : GameCatalogDataSource {
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    /** Fail-fast lock: installer staging/game dirs are not safe for concurrent installs. */
    private val downloadInstallMutex = Mutex()

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
        if (!downloadInstallMutex.tryLock()) {
            return Result.failure(
                IllegalStateException("A game download is already in progress"),
            )
        }
        _downloadProgress.value = null
        return try {
            downloadInstallRunner
                .downloadAndInstall(gameId, version, build) { progress ->
                    _downloadProgress.value = progress
                }.also {
                    _downloadProgress.value = null
                }
        } finally {
            downloadInstallMutex.unlock()
        }
    }

    override suspend fun getInstallState(gameId: String): InstallState =
        withContext(ioDispatcher) {
            installStateProbe.probe(gameId)
        }

    override suspend fun uninstallGame(gameId: String): Result<Unit> = gameInstaller.uninstall(gameId)

    override suspend fun getOnDiskSizeBytes(gameId: String): Long? =
        withContext(ioDispatcher) {
            gameInstaller.getOnDiskSizeBytes(gameId)
        }

    override suspend fun launchGame(
        gameId: String,
        displayTitle: String,
    ): Result<Unit> =
        gameLauncher.launch(
            GameLaunchIdentity(
                gameId = gameId,
                displayTitle = displayTitle,
                platformKey = PlatformKey.current(),
            ),
        )

    override suspend fun openWebGame(url: String): Result<Unit> = gameLauncher.openUrl(url)

    override suspend fun listInstalledGames(): List<InstalledGameSummary> =
        withContext(ioDispatcher) {
            gameInstaller.listInstalledGames()
        }

    override suspend fun uninstallAllGames(): Result<Unit> =
        withContext(ioDispatcher) {
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
