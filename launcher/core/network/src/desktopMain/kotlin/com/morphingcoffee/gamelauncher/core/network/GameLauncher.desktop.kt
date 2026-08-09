package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.telemetry.CrashReporting
import com.morphingcoffee.gamelauncher.core.telemetry.GameLaunchFailure
import com.morphingcoffee.gamelauncher.core.telemetry.ProcessOutputBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

actual class GameLauncher {
    actual suspend fun launch(identity: GameLaunchIdentity): Result<Unit> {
        val startedAt = System.currentTimeMillis()
        var installedVersion: String? = null
        var exitCode: Int? = null
        var processOutput: String? = null
        var isMac = false

        return runCatching {
            AppLog.i("GameLauncher", "Launch requested for ${identity.gameId}")
            val recordFile = File(LibraryPaths.installRecordFile(identity.gameId))
            if (!recordFile.exists()) {
                error("Game is not installed: ${identity.gameId}")
            }

            val record = Json.decodeFromString<GameInstallRecord>(recordFile.readText())
            installedVersion = record.version
            val gameDir = File(LibraryPaths.gameDirectory(identity.gameId))
            val executable = File(gameDir, record.executablePath)
            if (!executable.exists()) {
                error("Executable not found for ${identity.gameId}")
            }

            val os = System.getProperty("os.name").lowercase()
            isMac = "mac" in os || "darwin" in os

            val outputBuffer = ProcessOutputBuffer()
            val process =
                if (isMac) {
                    withContext(Dispatchers.IO) {
                        MacGameSupport.prepareLaunch(executable)
                        MacGameSupport.launchCommand(gameDir, executable).start()
                    }
                } else {
                    if (!executable.canExecute()) {
                        executable.setExecutable(true, false)
                    }
                    withContext(Dispatchers.IO) {
                        val builder =
                            ProcessBuilder(executable.absolutePath)
                                .directory(gameDir)
                                .redirectErrorStream(true)
                        val started = builder.start()
                        pumpProcessOutput(started, outputBuffer)
                        started
                    }
                }

            exitCode = withContext(Dispatchers.IO) { process.waitFor() }
            if (!isMac) {
                processOutput = outputBuffer.snapshot()
            }

            if (isMac) {
                // Waited process is `open`, not the game. Non-zero means launch helper failed.
                if (exitCode != 0) {
                    error("Launch helper failed with code $exitCode")
                }
            } else if (exitCode != 0) {
                error("Game exited with code $exitCode")
            }

            AppLog.i("GameLauncher", "Launch finished for ${identity.gameId} with exit code $exitCode")
        }.onFailure { error ->
            AppLog.e("GameLauncher", "Launch failed for ${identity.gameId}", error)
            val duration = System.currentTimeMillis() - startedAt
            // Only tag as launch_helper when macOS `open` actually returned a status.
            val operation =
                if (isMac && exitCode != null) {
                    GameLaunchFailure.OPERATION_LAUNCH_HELPER
                } else {
                    GameLaunchFailure.OPERATION_LAUNCH_GAME
                }
            CrashReporting.captureLaunchFailure(
                GameLaunchFailure(
                    gameId = identity.gameId,
                    displayTitle = identity.displayTitle,
                    installedVersion = installedVersion,
                    platformKey = identity.platformKey,
                    exitCode = exitCode,
                    durationMillis = duration,
                    operation = operation,
                    message = error.message ?: "Launch failed",
                    processOutputTail = processOutput,
                    cause = error,
                ),
            )
        }
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

private fun pumpProcessOutput(
    process: Process,
    buffer: ProcessOutputBuffer,
) {
    thread(name = "game-process-output", isDaemon = true) {
        try {
            BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { reader ->
                val chunk = CharArray(1_024)
                while (true) {
                    val read = reader.read(chunk)
                    if (read < 0) break
                    buffer.append(String(chunk, 0, read))
                }
            }
        } catch (_: Exception) {
            // Best-effort capture — never fail the launch path for diagnostics I/O.
        }
    }
}
