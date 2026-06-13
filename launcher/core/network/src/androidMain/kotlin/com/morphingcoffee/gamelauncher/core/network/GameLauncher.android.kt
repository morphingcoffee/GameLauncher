package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry

actual class GameLauncher {
    actual suspend fun launch(entry: GameCatalogEntry): Result<Unit> =
        Result.failure(UnsupportedOperationException("Game launch is only supported on desktop"))
}

actual fun createGameLauncher(): GameLauncher = GameLauncher()
