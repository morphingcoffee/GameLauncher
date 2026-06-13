package com.morphingcoffee.gamelauncher.core.network

import java.io.File

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
                "${appData}${File.separator}GameLauncher"
            }
            "mac" in os || "darwin" in os -> {
                val home =
                    System.getProperty("user.home")
                        ?: error("user.home is not set")
                "${home}${File.separator}Library${File.separator}Application Support${File.separator}GameLauncher"
            }
            else -> error("Unsupported operating system: $os")
        }
    }

    fun gameDirectory(gameId: String): String = "${rootDirectory()}${File.separator}games${File.separator}$gameId"
}
