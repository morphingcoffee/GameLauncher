package com.morphingcoffee.gamelauncher.core.network

import java.nio.file.Paths

object LibraryPaths {
    private fun rootDirectory(): String {
        val os =
            System
                .getProperty("os.name")
                .lowercase()
        return when {
            "win" in os -> {
                val appData =
                    System.getenv("APPDATA")
                        ?: error("APPDATA is not set")
                path(appData, "GameLauncher")
            }
            "mac" in os || "darwin" in os -> {
                val home =
                    System.getProperty("user.home")
                        ?: error("user.home is not set")
                path(home, "Library", "Application Support", "GameLauncher")
            }
            else -> error("Unsupported operating system: $os")
        }
    }

    fun gameDirectory(gameId: String): String = path(rootDirectory(), "games", gameId)

    private fun path(vararg segments: String): String {
        require(segments.isNotEmpty()) { "path requires at least one segment" }
        return Paths.get(segments.first(), *segments.drop(1).toTypedArray()).toString()
    }
}
