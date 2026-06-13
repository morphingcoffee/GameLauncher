package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameCatalogRepository(
    private val manifestRepository: ManifestRepository,
    private val gameLauncher: GameLauncher,
) : GameCatalogDataSource {
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    override suspend fun loadCatalog(): Result<List<GameCatalogEntry>> =
        runCatching {
            manifestRepository.fetchManifest().games
        }

    override suspend fun launchGame(entry: GameCatalogEntry): Result<Unit> = gameLauncher.launch(entry)
}
