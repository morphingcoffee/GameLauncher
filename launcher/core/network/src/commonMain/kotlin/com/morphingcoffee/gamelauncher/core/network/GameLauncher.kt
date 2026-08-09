package com.morphingcoffee.gamelauncher.core.network

expect class GameLauncher {
    suspend fun launch(identity: GameLaunchIdentity): Result<Unit>

    suspend fun openUrl(url: String): Result<Unit>
}

expect fun createGameLauncher(): GameLauncher
