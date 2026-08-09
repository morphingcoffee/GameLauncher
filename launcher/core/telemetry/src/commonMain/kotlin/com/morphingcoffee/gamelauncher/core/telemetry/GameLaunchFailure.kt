package com.morphingcoffee.gamelauncher.core.telemetry

/**
 * Minimal identity + outcome metadata for launcher-observed game launch failures.
 * Never includes absolute executable paths.
 */
data class GameLaunchFailure(
    val gameId: String,
    val displayTitle: String,
    val installedVersion: String?,
    val platformKey: String?,
    val exitCode: Int?,
    val durationMillis: Long?,
    val operation: String = OPERATION_LAUNCH_GAME,
    val message: String,
    val processOutputTail: String? = null,
    val cause: Throwable? = null,
) {
    companion object {
        const val OPERATION_LAUNCH_GAME = "launch_game"
        const val OPERATION_LAUNCH_HELPER = "launch_helper"
    }
}
