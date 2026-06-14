package com.morphingcoffee.gamelauncher.core.network

expect class GameLauncher {
    suspend fun launch(gameId: String): Result<Unit>
}

expect fun createGameLauncher(): GameLauncher
