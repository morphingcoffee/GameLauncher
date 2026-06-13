package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import kotlinx.coroutines.flow.StateFlow

interface GameCatalogDataSource {
    val downloadProgress: StateFlow<DownloadProgress?>

    suspend fun loadCatalog(): Result<List<GameCatalogEntry>>

    suspend fun launchGame(entry: GameCatalogEntry): Result<Unit>
}
