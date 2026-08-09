package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import com.morphingcoffee.gamelauncher.core.model.LauncherChannelKey
import com.morphingcoffee.gamelauncher.core.model.LauncherRelease
import com.morphingcoffee.gamelauncher.core.model.WindowsUninstallEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LauncherInstallChannelTest {
    @BeforeTest
    fun setUp() {
        LauncherInstallChannelTestHooks.resetAll()
    }

    @AfterTest
    fun tearDown() {
        LauncherInstallChannelTestHooks.resetAll()
    }

    @Test
    fun detect_runsRegistryProbeOnConfiguredIoDispatcher() =
        runTest {
            val ioDispatcher = RecordingDispatcher()
            var registryCalls = 0
            LauncherInstallChannelTestHooks.ioDispatcherOverride = ioDispatcher
            LauncherInstallChannelTestHooks.osNameOverride = "Windows 10"
            LauncherInstallChannelTestHooks.executablePathOverride =
                """C:\Users\player\AppData\Local\GameLauncher\GameLauncher.exe"""
            LauncherInstallChannelTestHooks.uninstallEntriesProvider = {
                registryCalls++
                emptyList()
            }

            val channel = LauncherInstallChannel.detect()

            assertEquals(LauncherChannelKey.WINDOWS_X64_PORTABLE, channel)
            assertEquals(1, registryCalls)
            assertTrue(ioDispatcher.dispatchCount > 0)
        }

    @Test
    fun detect_cachesResultAcrossRepeatedAndConcurrentCallers() =
        runTest {
            var registryCalls = 0
            LauncherInstallChannelTestHooks.osNameOverride = "Windows 10"
            LauncherInstallChannelTestHooks.executablePathOverride =
                """C:\Users\player\AppData\Local\GameLauncher\GameLauncher.exe"""
            LauncherInstallChannelTestHooks.uninstallEntriesProvider = {
                registryCalls++
                emptyList()
            }

            val results =
                (1..8)
                    .map {
                        async { LauncherInstallChannel.detect() }
                    }.awaitAll()

            assertTrue(results.all { it == LauncherChannelKey.WINDOWS_X64_PORTABLE })
            assertEquals(1, registryCalls)
            assertEquals(LauncherChannelKey.WINDOWS_X64_PORTABLE, LauncherInstallChannel.detect())
            assertEquals(1, registryCalls)
        }

    @Test
    fun resolveChannelKey_rechecksEachManifestAgainstCachedDetection() =
        runTest {
            var registryCalls = 0
            LauncherInstallChannelTestHooks.osNameOverride = "Windows 10"
            LauncherInstallChannelTestHooks.executablePathOverride =
                """C:\Users\player\AppData\Local\GameLauncher\GameLauncher.exe"""
            LauncherInstallChannelTestHooks.uninstallEntriesProvider = {
                registryCalls++
                emptyList()
            }

            val portableOnly =
                LauncherRelease(
                    channels =
                        mapOf(
                            LauncherChannelKey.WINDOWS_X64_PORTABLE to sampleChannelBuild(),
                        ),
                )
            val msiOnly =
                LauncherRelease(
                    channels =
                        mapOf(
                            LauncherChannelKey.WINDOWS_X64_MSI to sampleChannelBuild(),
                        ),
                )

            assertEquals(
                LauncherChannelKey.WINDOWS_X64_PORTABLE,
                LauncherInstallChannel.resolveChannelKey(portableOnly),
            )
            assertNull(LauncherInstallChannel.resolveChannelKey(msiOnly))
            assertEquals(1, registryCalls)
        }

    @Test
    fun detect_usesProgramFilesMsiPath() =
        runTest {
            var registryCalls = 0
            LauncherInstallChannelTestHooks.osNameOverride = "Windows 10"
            LauncherInstallChannelTestHooks.executablePathOverride =
                """C:\Program Files\GameLauncher\GameLauncher.exe"""
            LauncherInstallChannelTestHooks.uninstallEntriesProvider = {
                registryCalls++
                emptyList()
            }

            assertEquals(LauncherChannelKey.WINDOWS_X64_MSI, LauncherInstallChannel.detect())
            assertEquals(1, registryCalls)
        }

    @Test
    fun detect_matchesCustomPathViaUninstallRegistryEntries() =
        runTest {
            val customPath = """D:\Games\GameLauncher\app\GameLauncher.exe"""
            LauncherInstallChannelTestHooks.osNameOverride = "Windows 11"
            LauncherInstallChannelTestHooks.executablePathOverride = customPath
            LauncherInstallChannelTestHooks.uninstallEntriesProvider = {
                listOf(
                    WindowsUninstallEntry(
                        displayName = "GameLauncher",
                        installLocation = """D:\Games\GameLauncher""",
                        windowsInstaller = true,
                        uninstallString = """MsiExec.exe /X{GUID}""",
                    ),
                )
            }

            assertEquals(LauncherChannelKey.WINDOWS_X64_MSI, LauncherInstallChannel.detect())
        }

    private fun sampleChannelBuild(): LauncherChannelBuild =
        LauncherChannelBuild(
            version = "0.0.1",
            artifactType = "msi",
            downloadUrl = "https://example.com/launcher.msi",
            fileSizeBytes = 1L,
            sha256 = "abc",
        )

    private class RecordingDispatcher : CoroutineDispatcher() {
        @Volatile
        var dispatchCount: Int = 0

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatchCount++
            block.run()
        }
    }
}
