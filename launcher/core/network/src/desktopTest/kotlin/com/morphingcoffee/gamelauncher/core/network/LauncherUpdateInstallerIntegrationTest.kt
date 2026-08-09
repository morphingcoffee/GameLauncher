package com.morphingcoffee.gamelauncher.core.network

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LauncherUpdateInstallerIntegrationTest {
    private lateinit var tempRoot: File
    private lateinit var userDownloads: File
    private lateinit var libraryLayout: LibraryLayout

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        tempRoot =
            kotlin.io.path
                .createTempDirectory("launcher-update-test-")
                .toFile()
        userDownloads = File(tempRoot, "UserDownloads").also { it.mkdirs() }
        libraryLayout = temporaryLibraryLayout(tempRoot, userDownloads)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        tempRoot.deleteRecursively()
    }

    @Test
    fun downloadValidatesShaAndSize_writesDestinationUnderUserDownloads() =
        runTest {
            val bytes = ByteArray(32) { 3 }
            val client = mockDownloadClient { respondBytes(this, bytes) }
            val processLauncher = RecordingProcessLauncher()
            val processExiter = RecordingProcessExiter()
            val desktop = RecordingDesktopActions()
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = client,
                    libraryLayout = libraryLayout,
                    processLauncher = processLauncher,
                    desktopActions = desktop,
                    processExiter = processExiter,
                    handoffDelayMillis = 0L,
                )

            val result =
                installer.downloadAndApply(
                    channelBuild(bytes, artifactType = "zip"),
                    versionLabel = "1.2.3",
                )

            assertTrue(result.isSuccess)
            val destination = File(userDownloads, "GameLauncher-1.2.3.zip")
            assertTrue(destination.isFile)
            assertEquals(bytes.toList(), destination.readBytes().toList())
            assertFalse(
                File(libraryLayout.launcherUpdatesDirectory()).listFiles()?.any { it.extension == "part" } == true,
            )
            assertNull(installer.downloadProgress.value)
            assertTrue(processExiter.exits.isEmpty())
        }

    @Test
    fun resumeWithPartialContent_appendsFromOffset() =
        runTest {
            val bytes = ByteArray(40) { it.toByte() }
            val staging = File(libraryLayout.launcherUpdatesDirectory(), "update-1.0.0.zip.part")
            staging.parentFile?.mkdirs()
            staging.writeBytes(bytes.copyOfRange(0, 15))

            var range: String? = null
            val client =
                mockDownloadClient { request ->
                    range = request.headers[HttpHeaders.Range]
                    respondBytes(this, bytes.copyOfRange(15, bytes.size), HttpStatusCode.PartialContent)
                }
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = client,
                    libraryLayout = libraryLayout,
                    processLauncher = RecordingProcessLauncher(),
                    desktopActions = RecordingDesktopActions(),
                    processExiter = RecordingProcessExiter(),
                    osName = { "Mac OS X" },
                    handoffDelayMillis = 0L,
                )

            val result = installer.downloadAndApply(channelBuild(bytes, "zip"), "1.0.0")

            assertTrue(result.isSuccess)
            assertEquals("bytes=15-", range)
        }

    @Test
    fun serverIgnoresRange_redownloadsFromScratch() =
        runTest {
            val bytes = ByteArray(20) { 1 }
            val staging = File(libraryLayout.launcherUpdatesDirectory(), "update-1.0.0.zip.part")
            staging.parentFile?.mkdirs()
            staging.writeBytes(ByteArray(5) { 9 })

            val client =
                mockDownloadClient { request ->
                    assertEquals("bytes=5-", request.headers[HttpHeaders.Range])
                    respondBytes(this, bytes, HttpStatusCode.OK)
                }
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = client,
                    libraryLayout = libraryLayout,
                    processLauncher = RecordingProcessLauncher(),
                    desktopActions = RecordingDesktopActions(),
                    processExiter = RecordingProcessExiter(),
                    osName = { "Mac OS X" },
                    handoffDelayMillis = 0L,
                )

            assertTrue(installer.downloadAndApply(channelBuild(bytes, "zip"), "1.0.0").isSuccess)
        }

    @Test
    fun sizeMismatch_failsClearsProgressAndCleansStaging() =
        runTest {
            val bytes = ByteArray(8) { 1 }
            val client = mockDownloadClient { respondBytes(this, bytes) }
            val build = channelBuild(bytes, "zip").copy(fileSizeBytes = 99L)
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = client,
                    libraryLayout = libraryLayout,
                    processExiter = RecordingProcessExiter(),
                    handoffDelayMillis = 0L,
                )

            val result = installer.downloadAndApply(build, "1.0.0")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("does not match expected"))
            assertNull(installer.downloadProgress.value)
            assertFalse(File(libraryLayout.launcherUpdatesDirectory(), "update-1.0.0.zip.part").exists())
        }

    @Test
    fun shaMismatch_failsWithoutExit() =
        runTest {
            val bytes = ByteArray(8) { 2 }
            val client = mockDownloadClient { respondBytes(this, bytes) }
            val exiter = RecordingProcessExiter()
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = client,
                    libraryLayout = libraryLayout,
                    processExiter = exiter,
                    handoffDelayMillis = 0L,
                )

            val result =
                installer.downloadAndApply(
                    channelBuild(bytes, "msi").copy(sha256 = "00".repeat(32)),
                    "1.0.0",
                )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("SHA-256 mismatch"))
            assertTrue(exiter.exits.isEmpty())
            assertNull(installer.downloadProgress.value)
        }

    @Test
    fun msi_recordsMsiexecThenExit() =
        runTest {
            val bytes = ByteArray(12) { 4 }
            val processLauncher = RecordingProcessLauncher()
            val exiter = RecordingProcessExiter()
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = mockDownloadClient { respondBytes(this, bytes) },
                    libraryLayout = libraryLayout,
                    processLauncher = processLauncher,
                    processExiter = exiter,
                    handoffDelayMillis = 0L,
                )

            val result = installer.downloadAndApply(channelBuild(bytes, "msi"), "9.9.9")

            assertTrue(result.isSuccess)
            val destination = File(userDownloads, "GameLauncher-9.9.9.msi")
            assertEquals(
                listOf("msiexec", "/i", destination.absolutePath, "/passive"),
                processLauncher.starts.single().command,
            )
            assertEquals(listOf(0), exiter.exits)
        }

    @Test
    fun dmg_opensDesktopThenExit() =
        runTest {
            val bytes = ByteArray(12) { 5 }
            val desktop = RecordingDesktopActions()
            val exiter = RecordingProcessExiter()
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = mockDownloadClient { respondBytes(this, bytes) },
                    libraryLayout = libraryLayout,
                    desktopActions = desktop,
                    processExiter = exiter,
                    handoffDelayMillis = 0L,
                )

            val result = installer.downloadAndApply(channelBuild(bytes, "dmg"), "2.0.0")

            assertTrue(result.isSuccess)
            val destination = File(userDownloads, "GameLauncher-2.0.0.dmg")
            assertEquals(listOf(destination.canonicalFile), desktop.openedFiles.map { it.canonicalFile })
            assertEquals(listOf(0), exiter.exits)
        }

    @Test
    fun zipOnWindows_recordsExplorerSelectWithoutExit() =
        runTest {
            val bytes = ByteArray(12) { 6 }
            val processLauncher = RecordingProcessLauncher()
            val exiter = RecordingProcessExiter()
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = mockDownloadClient { respondBytes(this, bytes) },
                    libraryLayout = libraryLayout,
                    processLauncher = processLauncher,
                    processExiter = exiter,
                    osName = { "Windows 11" },
                    handoffDelayMillis = 0L,
                )

            val result = installer.downloadAndApply(channelBuild(bytes, "zip"), "3.0.0")

            assertTrue(result.isSuccess)
            val destination = File(userDownloads, "GameLauncher-3.0.0.zip")
            assertEquals(
                listOf("explorer.exe", "/select,${destination.absolutePath}"),
                processLauncher.starts.single().command,
            )
            assertTrue(exiter.exits.isEmpty())
        }

    @Test
    fun zipOnMac_recordsOpenRevealWithoutExit() =
        runTest {
            val bytes = ByteArray(12) { 6 }
            val processLauncher = RecordingProcessLauncher()
            val exiter = RecordingProcessExiter()
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = mockDownloadClient { respondBytes(this, bytes) },
                    libraryLayout = libraryLayout,
                    processLauncher = processLauncher,
                    processExiter = exiter,
                    osName = { "Mac OS X" },
                    handoffDelayMillis = 0L,
                )

            val result = installer.downloadAndApply(channelBuild(bytes, "zip"), "3.0.1")

            assertTrue(result.isSuccess)
            val destination = File(userDownloads, "GameLauncher-3.0.1.zip")
            assertEquals(
                listOf("open", "-R", destination.absolutePath),
                processLauncher.starts.single().command,
            )
            assertTrue(exiter.exits.isEmpty())
        }

    @Test
    fun unsupportedArtifactWithoutDesktop_failsClearly() =
        runTest {
            val bytes = ByteArray(8) { 1 }
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = mockDownloadClient { respondBytes(this, bytes) },
                    libraryLayout = libraryLayout,
                    desktopActions = RecordingDesktopActions(desktopSupported = false),
                    processExiter = RecordingProcessExiter(),
                    handoffDelayMillis = 0L,
                )

            val result = installer.downloadAndApply(channelBuild(bytes, "bin"), "1.0.0")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("Unsupported update artifact type"))
            assertNull(installer.downloadProgress.value)
        }

    @Test
    fun dmgWithoutDesktop_failsBeforeExit() =
        runTest {
            val bytes = ByteArray(8) { 1 }
            val exiter = RecordingProcessExiter()
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = mockDownloadClient { respondBytes(this, bytes) },
                    libraryLayout = libraryLayout,
                    desktopActions = RecordingDesktopActions(desktopSupported = false),
                    processExiter = exiter,
                    handoffDelayMillis = 0L,
                )

            val result = installer.downloadAndApply(channelBuild(bytes, "dmg"), "1.0.0")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("Desktop API is not supported"))
            assertTrue(exiter.exits.isEmpty())
            assertNull(installer.downloadProgress.value)
        }

    @Test
    fun cancellation_cleansStagingAndClearsProgress() =
        runTest {
            val bytes = ByteArray(64) { 1 }
            val client =
                mockDownloadClient {
                    delay(5_000)
                    respondBytes(this, bytes)
                }
            val installer =
                LauncherUpdateInstallerImpl(
                    downloadHttpClient = client,
                    libraryLayout = libraryLayout,
                    processExiter = RecordingProcessExiter(),
                    handoffDelayMillis = 0L,
                )
            val staging = File(libraryLayout.launcherUpdatesDirectory(), "update-1.0.0.zip.part")

            val deferred =
                async {
                    installer.downloadAndApply(channelBuild(bytes, "zip"), "1.0.0")
                }
            delay(50)
            deferred.cancel()
            runCatching { deferred.await() }

            assertFalse(staging.exists())
            assertNull(installer.downloadProgress.value)
        }
}
