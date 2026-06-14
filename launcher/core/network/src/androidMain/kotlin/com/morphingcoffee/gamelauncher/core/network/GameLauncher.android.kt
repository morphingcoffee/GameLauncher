package com.morphingcoffee.gamelauncher.core.network

actual class GameLauncher {
    actual suspend fun launch(gameId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Game launch is only supported on desktop"))
}

actual fun createGameLauncher(): GameLauncher = GameLauncher()
