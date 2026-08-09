package com.morphingcoffee.gamelauncher.core.network

import java.io.File
import java.nio.file.Paths

/**
 * Injectable library / downloads path layout used by install and update flows.
 * Production code uses [LibraryPaths]; tests supply a temp-directory implementation.
 */
interface LibraryLayout {
    fun downloadStagingFile(
        gameId: String,
        version: String,
    ): String

    fun gamesRootDirectory(): String

    fun gameDirectory(gameId: String): String

    fun installRecordFile(gameId: String): String

    fun launcherUpdatesDirectory(): String

    fun userDownloadsDirectory(): String
}

/** Temp-root [LibraryLayout] for deterministic desktop integration tests. */
fun temporaryLibraryLayout(
    root: File,
    userDownloads: File = File(root, "UserDownloads"),
): LibraryLayout =
    object : LibraryLayout {
        private fun path(vararg segments: String): String {
            require(segments.isNotEmpty()) { "path requires at least one segment" }
            return Paths.get(segments.first(), *segments.drop(1).toTypedArray()).toString()
        }

        private fun rootDir(): String = root.absolutePath

        override fun downloadStagingFile(
            gameId: String,
            version: String,
        ): String = path(rootDir(), "downloads", "$gameId-$version.zip.part")

        override fun gamesRootDirectory(): String = path(rootDir(), "games")

        override fun gameDirectory(gameId: String): String = path(gamesRootDirectory(), gameId)

        override fun installRecordFile(gameId: String): String = path(gameDirectory(gameId), ".install_record.json")

        override fun launcherUpdatesDirectory(): String = path(rootDir(), "updates")

        override fun userDownloadsDirectory(): String = userDownloads.absolutePath
    }
