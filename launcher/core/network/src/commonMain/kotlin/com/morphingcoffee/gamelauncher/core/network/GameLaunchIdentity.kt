package com.morphingcoffee.gamelauncher.core.network

/**
 * Identity metadata for a launch attempt. Does not include absolute paths.
 */
data class GameLaunchIdentity(
    val gameId: String,
    val displayTitle: String,
    val platformKey: String?,
)
