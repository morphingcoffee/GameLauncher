package com.morphingcoffee.gamelauncher.core.network

import java.nio.file.Paths

object LibraryPaths : LibraryLayout {
    fun appSupportRoot(): String = rootDirectory()

    fun preferencesFile(): String = path(rootDirectory(), "preferences.json")

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

    fun downloadsDirectory(): String = path(rootDirectory(), "downloads")

    override fun downloadStagingFile(
        gameId: String,
        version: String,
    ): String = path(downloadsDirectory(), "$gameId-$version.zip.part")

    override fun gamesRootDirectory(): String = path(rootDirectory(), "games")

    override fun gameDirectory(gameId: String): String = path(gamesRootDirectory(), gameId)

    override fun installRecordFile(gameId: String): String = path(gameDirectory(gameId), ".install_record.json")

    override fun launcherUpdatesDirectory(): String = path(rootDirectory(), "updates")

    override fun userDownloadsDirectory(): String {
        val os =
            System
                .getProperty("os.name")
                .lowercase()
        return when {
            "win" in os -> {
                val userProfile =
                    System.getenv("USERPROFILE")
                        ?: error("USERPROFILE is not set")
                path(userProfile, "Downloads")
            }
            "mac" in os || "darwin" in os -> {
                val home =
                    System.getProperty("user.home")
                        ?: error("user.home is not set")
                path(home, "Downloads")
            }
            else -> error("Unsupported operating system for downloads: $os")
        }
    }

    private fun path(vararg segments: String): String {
        require(segments.isNotEmpty()) { "path requires at least one segment" }
        return Paths.get(segments.first(), *segments.drop(1).toTypedArray()).toString()
    }
}
