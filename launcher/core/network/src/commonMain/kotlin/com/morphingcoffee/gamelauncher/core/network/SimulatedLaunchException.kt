package com.morphingcoffee.gamelauncher.core.network

/**
 * Thrown when a data source simulates a launch without starting a real process.
 * The UI should treat this as a successful dev launch and stay visible.
 */
class SimulatedLaunchException(
    val gameTitle: String,
) : Exception("Launch simulated for $gameTitle")
