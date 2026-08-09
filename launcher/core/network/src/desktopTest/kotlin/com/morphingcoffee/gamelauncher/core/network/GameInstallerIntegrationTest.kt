package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameInstallerIntegrationTest {
    private lateinit var tempRoot: File
    private lateinit var libraryLayout: LibraryLayout

    @BeforeTest
    fun setUp() {
        tempRoot = createTempDir(prefix = "game-installer-test-")
        libraryLayout = temporaryLibraryLayout(tempRoot)
    }

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun freshDownload_writesInstallRecordAndDeletesStaging() =
        runTest {
            val zip = zipBytes(mapOf("Game.bin" to byteArrayOf(1, 2, 3, 4)))
            val build = gameBuild(zip)
            val client =
                mockDownloadClient { request ->
                    assertNull(request.headers[HttpHeaders.Range])
                    respondBytes(this, zip)
                }
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", build) {}

            assertTrue(result.isSuccess)
            val installed = assertIs<InstallState.Installed>(installer.getInstallState("alpha"))
            assertEquals("1.0.0", installed.version)
            assertTrue(File(libraryLayout.gameDirectory("alpha"), "Game.bin").isFile)
            assertTrue(File(libraryLayout.installRecordFile("alpha")).isFile)
            assertFalse(File(libraryLayout.downloadStagingFile("alpha", "1.0.0")).exists())
        }

    @Test
    fun resumeWithPartialContent_appendsFromOffset() =
        runTest {
            val zip = zipBytes(mapOf("Game.bin" to ByteArray(64) { it.toByte() }))
            val staging = File(libraryLayout.downloadStagingFile("alpha", "1.0.0"))
            staging.parentFile?.mkdirs()
            staging.writeBytes(zip.copyOfRange(0, 20))

            var sawRange: String? = null
            val client =
                mockDownloadClient { request ->
                    sawRange = request.headers[HttpHeaders.Range]
                    respondBytes(this, zip.copyOfRange(20, zip.size), HttpStatusCode.PartialContent)
                }
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", gameBuild(zip)) {}

            assertTrue(result.isSuccess)
            assertEquals("bytes=20-", sawRange)
            assertIs<InstallState.Installed>(installer.getInstallState("alpha"))
        }

    @Test
    fun serverIgnoresRange_redownloadsFromScratch() =
        runTest {
            val zip = zipBytes(mapOf("Game.bin" to ByteArray(32) { 7 }))
            val staging = File(libraryLayout.downloadStagingFile("alpha", "1.0.0"))
            staging.parentFile?.mkdirs()
            staging.writeBytes(ByteArray(8) { 9 })

            val client =
                mockDownloadClient { request ->
                    assertEquals("bytes=8-", request.headers[HttpHeaders.Range])
                    respondBytes(this, zip, HttpStatusCode.OK)
                }
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", gameBuild(zip)) {}

            assertTrue(result.isSuccess)
            assertIs<InstallState.Installed>(installer.getInstallState("alpha"))
        }

    @Test
    fun sizeMismatch_failsAndDeletesStaging() =
        runTest {
            val payload = ByteArray(10) { 1 }
            val client = mockDownloadClient { respondBytes(this, payload) }
            val build =
                gameBuild(payload).copy(
                    fileSizeBytes = 99L,
                    sha256 = sha256Hex(payload),
                )
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", build) {}

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("does not match expected"))
            assertFalse(File(libraryLayout.downloadStagingFile("alpha", "1.0.0")).exists())
            assertIs<InstallState.NotInstalled>(installer.getInstallState("alpha"))
        }

    @Test
    fun shaMismatch_failsWithExpectedVsActualAndDeletesStaging() =
        runTest {
            val zip = zipBytes(mapOf("Game.bin" to byteArrayOf(1)))
            val client = mockDownloadClient { respondBytes(this, zip) }
            val build = gameBuild(zip).copy(sha256 = "deadbeef")
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", build) {}

            assertTrue(result.isFailure)
            val message = result.exceptionOrNull()!!.message!!
            assertTrue(message.contains("SHA-256 mismatch"))
            assertTrue(message.contains("deadbeef"))
            assertFalse(File(libraryLayout.downloadStagingFile("alpha", "1.0.0")).exists())
        }

    @Test
    fun corruptZip_failsWithoutInstallRecord() =
        runTest {
            val corrupt = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x01)
            val client = mockDownloadClient { respondBytes(this, corrupt) }
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", gameBuild(corrupt)) {}

            assertTrue(result.isFailure)
            assertFalse(File(libraryLayout.installRecordFile("alpha")).exists())
            assertIs<InstallState.NotInstalled>(installer.getInstallState("alpha"))
        }

    @Test
    fun zipSlipRelativePath_throwsSecurityException() =
        runTest {
            val zip = zipBytes(mapOf("../escape.bin" to byteArrayOf(1)))
            val client = mockDownloadClient { respondBytes(this, zip) }
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", gameBuild(zip, executablePath = "Game.bin")) {}

            assertTrue(result.isFailure)
            assertIs<SecurityException>(result.exceptionOrNull())
            assertTrue(result.exceptionOrNull()!!.message!!.contains("escapes game directory"))
            assertFalse(File(tempRoot, "escape.bin").exists())
        }

    @Test
    fun zipSlipAbsolutePath_throwsSecurityException() =
        runTest {
            val absoluteName = File(tempRoot, "absolute-escape.bin").absolutePath
            val zip = zipBytes(mapOf(absoluteName to byteArrayOf(1)))
            val client = mockDownloadClient { respondBytes(this, zip) }
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", gameBuild(zip)) {}

            assertTrue(result.isFailure)
            assertIs<SecurityException>(result.exceptionOrNull())
            assertFalse(File(absoluteName).exists())
        }

    @Test
    fun missingExecutableAfterExtract_failsAndCleansGameDir() =
        runTest {
            val zip = zipBytes(mapOf("other.bin" to byteArrayOf(1)))
            val client = mockDownloadClient { respondBytes(this, zip) }
            val installer = GameInstaller(client, libraryLayout)

            val result =
                installer.downloadAndInstall(
                    "alpha",
                    "1.0.0",
                    gameBuild(zip, executablePath = "Game.bin"),
                ) {}

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("Executable not found"))
            assertFalse(File(libraryLayout.gameDirectory("alpha")).exists())
        }

    @Test
    fun executablePathOutsideGameDir_failsAndDoesNotInstall() =
        runTest {
            val outside = File(tempRoot, "outside.bin")
            outside.writeBytes(byteArrayOf(9))
            val relativeEscape = "../outside.bin"
            val zip = zipBytes(mapOf("Game.bin" to byteArrayOf(1)))
            val client = mockDownloadClient { respondBytes(this, zip) }
            val installer = GameInstaller(client, libraryLayout)

            val result =
                installer.downloadAndInstall(
                    "alpha",
                    "1.0.0",
                    gameBuild(zip, executablePath = relativeEscape),
                ) {}

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("escapes game directory"))
            assertFalse(File(libraryLayout.installRecordFile("alpha")).exists())
            assertIs<InstallState.NotInstalled>(installer.getInstallState("alpha"))
        }

    @Test
    fun zeroFileSize_failsBeforeDownload() =
        runTest {
            var requested = false
            val client =
                mockDownloadClient {
                    requested = true
                    respondBytes(this, byteArrayOf(1))
                }
            val build =
                GameBuild(
                    downloadUrl = "https://cdn.example.com/game.zip",
                    executablePath = "Game.bin",
                    fileSizeBytes = 0L,
                    sha256 = "abc",
                )
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", build) {}

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("greater than zero"))
            assertFalse(requested)
        }

    @Test
    fun negativeFileSize_failsBeforeDownload() =
        runTest {
            val build =
                GameBuild(
                    downloadUrl = "https://cdn.example.com/game.zip",
                    executablePath = "Game.bin",
                    fileSizeBytes = -1L,
                    sha256 = "abc",
                )
            val installer = GameInstaller(mockDownloadClient { respondBytes(this, byteArrayOf()) }, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "1.0.0", build) {}

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("greater than zero"))
        }

    @Test
    fun replaceInstall_deletesPreviousGameDirBeforeExtract() =
        runTest {
            val gameDir = File(libraryLayout.gameDirectory("alpha"))
            gameDir.mkdirs()
            val stale = File(gameDir, "stale.bin")
            stale.writeBytes(byteArrayOf(9))
            File(libraryLayout.installRecordFile("alpha")).writeText("{}")

            val zip = zipBytes(mapOf("Game.bin" to byteArrayOf(1, 2)))
            val client = mockDownloadClient { respondBytes(this, zip) }
            val installer = GameInstaller(client, libraryLayout)

            val result = installer.downloadAndInstall("alpha", "2.0.0", gameBuild(zip)) {}

            assertTrue(result.isSuccess)
            assertFalse(stale.exists())
            assertTrue(File(gameDir, "Game.bin").isFile)
            val installed = assertIs<InstallState.Installed>(installer.getInstallState("alpha"))
            assertEquals("2.0.0", installed.version)
        }

    @Test
    fun validRecordAndExecutable_reportsInstalled() {
        val gameDir = File(libraryLayout.gameDirectory("alpha"))
        gameDir.mkdirs()
        File(gameDir, "Game.bin").writeBytes(byteArrayOf(1))
        File(libraryLayout.installRecordFile("alpha")).writeText(
            """
            {"game_id":"alpha","version":"1.2.3","executable_path":"Game.bin","sha256":"abc"}
            """.trimIndent(),
        )
        val installer = GameInstaller(mockDownloadClient { respondBytes(this, byteArrayOf()) }, libraryLayout)

        val state = assertIs<InstallState.Installed>(installer.getInstallState("alpha"))
        assertEquals("1.2.3", state.version)
        assertEquals("Game.bin", state.executablePath)
    }

    @Test
    fun missingRecord_corruptJson_orMissingExecutable_reportsNotInstalled() {
        val installer = GameInstaller(mockDownloadClient { respondBytes(this, byteArrayOf()) }, libraryLayout)
        assertIs<InstallState.NotInstalled>(installer.getInstallState("missing"))

        val gameDir = File(libraryLayout.gameDirectory("corrupt"))
        gameDir.mkdirs()
        File(libraryLayout.installRecordFile("corrupt")).writeText("{not-json")
        assertIs<InstallState.NotInstalled>(installer.getInstallState("corrupt"))

        val missingExeDir = File(libraryLayout.gameDirectory("noexe"))
        missingExeDir.mkdirs()
        File(libraryLayout.installRecordFile("noexe")).writeText(
            """
            {"game_id":"noexe","version":"1.0.0","executable_path":"Game.bin","sha256":"abc"}
            """.trimIndent(),
        )
        assertIs<InstallState.NotInstalled>(installer.getInstallState("noexe"))
    }

    @Test
    fun listInstalledGamesAndOnDiskSize_onlyValidInstalls() {
        val alphaDir = File(libraryLayout.gameDirectory("alpha"))
        alphaDir.mkdirs()
        File(alphaDir, "Game.bin").writeBytes(ByteArray(10))
        File(libraryLayout.installRecordFile("alpha")).writeText(
            """
            {"game_id":"alpha","version":"1.0.0","executable_path":"Game.bin","sha256":"abc"}
            """.trimIndent(),
        )

        val betaDir = File(libraryLayout.gameDirectory("beta"))
        betaDir.mkdirs()
        File(betaDir, "orphan.bin").writeBytes(ByteArray(100))

        val installer = GameInstaller(mockDownloadClient { respondBytes(this, byteArrayOf()) }, libraryLayout)
        val listed = installer.listInstalledGames()

        assertEquals(1, listed.size)
        assertEquals("alpha", listed.single().gameId)
        assertEquals("1.0.0", listed.single().version)
        val size = assertNotNull(installer.getOnDiskSizeBytes("alpha"))
        assertTrue(size >= 10L)
        assertNull(installer.getOnDiskSizeBytes("beta"))
    }

    @Test
    fun cleanUninstall_removesGameDirAndRecord() =
        runTest {
            val gameDir = File(libraryLayout.gameDirectory("alpha"))
            gameDir.mkdirs()
            File(gameDir, "Game.bin").writeBytes(byteArrayOf(1))
            File(libraryLayout.installRecordFile("alpha")).writeText(
                """
                {"game_id":"alpha","version":"1.0.0","executable_path":"Game.bin","sha256":"abc"}
                """.trimIndent(),
            )
            val installer = GameInstaller(mockDownloadClient { respondBytes(this, byteArrayOf()) }, libraryLayout)

            val result = installer.uninstall("alpha")

            assertTrue(result.isSuccess)
            assertFalse(gameDir.exists())
            assertIs<InstallState.NotInstalled>(installer.getInstallState("alpha"))
        }

    @Test
    fun partialUninstallFailure_returnsUserFacingMessage() =
        runTest {
            val gameDir = File(libraryLayout.gameDirectory("alpha"))
            gameDir.mkdirs()
            val kept = File(gameDir, "Game.bin")
            kept.writeBytes(byteArrayOf(1))
            File(libraryLayout.installRecordFile("alpha")).writeText(
                """
                {"game_id":"alpha","version":"1.0.0","executable_path":"Game.bin","sha256":"abc"}
                """.trimIndent(),
            )
            val installer =
                GameInstaller(
                    downloadHttpClient = mockDownloadClient { respondBytes(this, byteArrayOf()) },
                    libraryLayout = libraryLayout,
                    fileDeleter =
                        FileDeleter { file ->
                            if (file.name == "Game.bin") {
                                false
                            } else {
                                file.delete()
                            }
                        },
                )

            val result = installer.uninstall("alpha")

            assertTrue(result.isFailure)
            assertTrue(
                result.exceptionOrNull()!!.message!!.contains(
                    "Could not remove all game files. Close the game if it is running and try again.",
                ),
            )
            assertTrue(kept.exists())
        }

    @Test
    fun cancellation_cleansStagingAndIsDistinguishableFromDownloadError() =
        runTest {
            val zip = zipBytes(mapOf("Game.bin" to ByteArray(1024) { 1 }))
            val client =
                mockDownloadClient {
                    delay(5_000)
                    respondBytes(this, zip)
                }
            val installer = GameInstaller(client, libraryLayout)
            val staging = File(libraryLayout.downloadStagingFile("alpha", "1.0.0"))

            val deferred =
                async {
                    installer.downloadAndInstall("alpha", "1.0.0", gameBuild(zip)) {}
                }
            delay(50)
            deferred.cancel()
            val result = runCatching { deferred.await() }

            assertTrue(result.exceptionOrNull() is CancellationException || deferred.isCancelled)
            assertFalse(staging.exists())
            // Current runCatching boundary may wrap CancellationException as Result.failure;
            // either propagation or a CancellationException failure is distinguishable from size/SHA errors.
            if (result.isSuccess) {
                val installResult = result.getOrNull()
                assertNotNull(installResult)
                assertTrue(installResult.isFailure)
                assertIs<CancellationException>(installResult.exceptionOrNull())
            }
        }

    private fun createTempDir(prefix: String): File =
        kotlin.io.path.createTempDirectory(prefix).toFile().also {
            File(it, "UserDownloads").mkdirs()
        }
}
