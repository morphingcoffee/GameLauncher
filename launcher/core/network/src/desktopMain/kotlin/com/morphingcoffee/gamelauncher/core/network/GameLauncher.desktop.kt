package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.logging.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.File
import java.net.URI

actual class GameLauncher {
    actual suspend fun launch(gameId: String): Result<Unit> =
        runCatching {
            AppLog.i("GameLauncher", "Launch requested for $gameId")
            val recordFile = File(LibraryPaths.installRecordFile(gameId))
            if (!recordFile.exists()) {
                error("Game is not installed: $gameId")
            }

            val record = Json.decodeFromString<GameInstallRecord>(recordFile.readText())
            val gameDir = File(LibraryPaths.gameDirectory(gameId))
            val executable = File(gameDir, record.executablePath)
            if (!executable.exists()) {
                error("Executable not found: ${executable.absolutePath}")
            }

            val process =
                ProcessBuilder(executable.absolutePath)
                    .directory(gameDir)
                    .inheritIO()
                    .start()
            val exitCode = withContext(Dispatchers.IO) { process.waitFor() }
            if (exitCode != 0) {
                error("Game exited with code $exitCode")
            }
            AppLog.i("GameLauncher", "Launch finished for $gameId with exit code $exitCode")
        }.onFailure { error ->
            AppLog.e("GameLauncher", "Launch failed for $gameId", error)
        }

    actual suspend fun openUrl(url: String): Result<Unit> =
        runCatching {
            AppLog.i("GameLauncher", "Opening URL in browser: $url")
            // Desktop.browse uses AWT; on Compose Desktop this must run on the Swing main thread.
            withContext(Dispatchers.Main) {
                if (!Desktop.isDesktopSupported()) {
                    error("Desktop API is not supported on this platform")
                }
                val desktop = Desktop.getDesktop()
                if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                    error("Opening URLs in a browser is not supported on this platform")
                }
                desktop.browse(URI(url))
            }
            AppLog.i("GameLauncher", "Browser opened for $url")
        }.onFailure { error ->
            AppLog.e("GameLauncher", "Failed to open URL: $url", error)
        }
}

actual fun createGameLauncher(): GameLauncher = GameLauncher()
