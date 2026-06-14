package com.morphingcoffee.gamelauncher.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

actual class GameLauncher {
    actual suspend fun launch(gameId: String): Result<Unit> =
        runCatching {
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
        }
}

actual fun createGameLauncher(): GameLauncher = GameLauncher()
