package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry

expect class GameLauncher {
    suspend fun launch(entry: GameCatalogEntry): Result<Unit>
}

expect fun createGameLauncher(): GameLauncher
