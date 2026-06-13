package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import java.io.File

actual class GameLauncher {
    actual suspend fun launch(entry: GameCatalogEntry): Result<Unit> =
        runCatching {
            val build =
                entry.buildForCurrentPlatform()
                    ?: error("No build available for the current platform")
            val gameDir = LibraryPaths.gameDirectory(entry.id)
            val executable = File(gameDir, build.executablePath)
            if (!executable.exists()) {
                error("Executable not found: ${executable.absolutePath}")
            }

            ProcessBuilder(executable.absolutePath)
                .directory(File(gameDir))
                .inheritIO()
                .start()
        }
}

actual fun createGameLauncher(): GameLauncher = GameLauncher()
