package com.morphingcoffee.gamelauncher.core.network

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameLauncherIntegrationTest {
    private lateinit var tempRoot: File
    private lateinit var libraryLayout: LibraryLayout

    @BeforeTest
    fun setUp() {
        tempRoot =
            kotlin.io.path
                .createTempDirectory("game-launcher-test-")
                .toFile()
        libraryLayout = temporaryLibraryLayout(tempRoot)
    }

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun nonMacLaunch_usesAbsoluteExecutableAndGameWorkingDir() =
        runTest {
            val gameDir = prepareInstall(executablePath = "Game.bin")
            val executable = File(gameDir, "Game.bin")
            val processLauncher = RecordingProcessLauncher(exitCode = 0)
            val launcher =
                GameLauncher(
                    libraryLayout = libraryLayout,
                    isMacOs = { false },
                    processLauncher = processLauncher,
                )

            val result = launcher.launch(identity())

            assertTrue(result.isSuccess)
            val start = processLauncher.starts.single()
            assertEquals(listOf(executable.absolutePath), start.command)
            assertEquals(gameDir.canonicalFile, start.workingDirectory?.canonicalFile)
            assertTrue(start.redirectErrorStream)
        }

    @Test
    fun nonMacNonZeroExit_failsWithExitCode() =
        runTest {
            prepareInstall(executablePath = "Game.bin")
            val launcher =
                GameLauncher(
                    libraryLayout = libraryLayout,
                    isMacOs = { false },
                    processLauncher = RecordingProcessLauncher(exitCode = 7),
                )

            val result = launcher.launch(identity())

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("exited with code 7"))
        }

    @Test
    fun macAppBundleLaunch_usesOpenDashN() =
        runTest {
            val gameDir =
                prepareInstall(
                    executablePath = "Cool.app/Contents/MacOS/Cool",
                    nestedDirs = listOf("Cool.app/Contents/MacOS"),
                )
            val appBundle = File(gameDir, "Cool.app")
            val processLauncher = RecordingProcessLauncher(exitCode = 0)
            val launcher =
                GameLauncher(
                    libraryLayout = libraryLayout,
                    isMacOs = { true },
                    processLauncher = processLauncher,
                )

            val result = launcher.launch(identity())

            assertTrue(result.isSuccess)
            val start = processLauncher.starts.single()
            assertEquals(listOf("open", "-n", appBundle.absolutePath), start.command)
            assertEquals(gameDir.canonicalFile, start.workingDirectory?.canonicalFile)
        }

    @Test
    fun macExecutableNotUnderAppBundle_failsWithoutProcessStart() =
        runTest {
            prepareInstall(executablePath = "bin/Cool")
            val processLauncher = RecordingProcessLauncher()
            val launcher =
                GameLauncher(
                    libraryLayout = libraryLayout,
                    isMacOs = { true },
                    processLauncher = processLauncher,
                )

            val result = launcher.launch(identity())

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains(".app bundle path"))
            assertTrue(processLauncher.starts.isEmpty())
        }

    @Test
    fun missingInstall_failsBeforeProcessStart() =
        runTest {
            val processLauncher = RecordingProcessLauncher()
            val launcher =
                GameLauncher(
                    libraryLayout = libraryLayout,
                    isMacOs = { false },
                    processLauncher = processLauncher,
                )

            val result = launcher.launch(identity())

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("not installed"))
            assertTrue(processLauncher.starts.isEmpty())
        }

    @Test
    fun missingExecutable_failsBeforeProcessStart() =
        runTest {
            val gameDir = File(libraryLayout.gameDirectory("alpha"))
            gameDir.mkdirs()
            File(libraryLayout.installRecordFile("alpha")).writeText(
                """
                {"game_id":"alpha","version":"1.0.0","executable_path":"Game.bin","sha256":"abc"}
                """.trimIndent(),
            )
            val processLauncher = RecordingProcessLauncher()
            val launcher =
                GameLauncher(
                    libraryLayout = libraryLayout,
                    isMacOs = { false },
                    processLauncher = processLauncher,
                )

            val result = launcher.launch(identity())

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("Executable not found"))
            assertTrue(processLauncher.starts.isEmpty())
        }

    private fun prepareInstall(
        executablePath: String,
        nestedDirs: List<String> = emptyList(),
    ): File {
        val gameDir = File(libraryLayout.gameDirectory("alpha"))
        gameDir.mkdirs()
        nestedDirs.forEach { File(gameDir, it).mkdirs() }
        val executable = File(gameDir, executablePath)
        executable.parentFile?.mkdirs()
        executable.writeBytes(byteArrayOf(1, 2, 3))
        executable.setExecutable(true, false)
        File(libraryLayout.installRecordFile("alpha")).writeText(
            """
            {"game_id":"alpha","version":"1.0.0","executable_path":"$executablePath","sha256":"abc"}
            """.trimIndent(),
        )
        return gameDir
    }

    private fun identity(): GameLaunchIdentity =
        GameLaunchIdentity(
            gameId = "alpha",
            displayTitle = "Alpha",
            platformKey = "linux-x64",
        )
}
