package com.morphingcoffee.gamelauncher.core.network

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

            ProcessBuilder(executable.absolutePath)
                .directory(gameDir)
                .inheritIO()
                .start()
        }
}

actual fun createGameLauncher(): GameLauncher = GameLauncher()
