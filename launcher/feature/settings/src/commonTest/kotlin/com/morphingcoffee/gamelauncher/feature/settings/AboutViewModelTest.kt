package com.morphingcoffee.gamelauncher.feature.settings

import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme
import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateEvaluation
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateStatus
import com.morphingcoffee.gamelauncher.core.network.DownloadProgress
import com.morphingcoffee.gamelauncher.core.network.InMemoryLauncherSettingsRepository
import com.morphingcoffee.gamelauncher.core.network.LauncherUpdateInstaller
import com.morphingcoffee.gamelauncher.core.network.LauncherUpdateRepository
import com.morphingcoffee.gamelauncher.core.network.ManifestRepository
import com.morphingcoffee.gamelauncher.core.telemetry.CrashReporting
import com.morphingcoffee.gamelauncher.core.telemetry.InMemoryTelemetryPreferencesStore
import com.morphingcoffee.gamelauncher.core.telemetry.TelemetryPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AboutViewModelTest {
    @BeforeTest
    fun clearDevProperty() {
        System.clearProperty("game.launcher.dev")
        CrashReporting.resetForTests()
    }

    @AfterTest
    fun tearDown() {
        System.clearProperty("game.launcher.dev")
        CrashReporting.resetForTests()
    }

    @Test
    fun aboutState_showsLauncherUpdateSignalWhenEvaluationIsUpdateAvailable() {
        val state =
            AboutState(
                updateEvaluation =
                    LauncherUpdateEvaluation(
                        status = LauncherUpdateStatus.UpdateAvailable,
                        channelKey = "windows-x64-msi",
                        channelBuild =
                            LauncherChannelBuild(
                                version = "9.9.9-build99",
                                artifactType = "msi",
                                downloadUrl = "https://example.com/update.msi",
                                fileSizeBytes = 100,
                                sha256 = "abc",
                            ),
                    ),
            )

        assertTrue(state.showLauncherUpdateSignal)
        assertEquals("9.9.9-build99", state.channelLatestVersion)
    }

    @Test
    fun aboutState_hidesLauncherUpdateSignalWhenSupported() {
        val state =
            AboutState(
                updateEvaluation =
                    LauncherUpdateEvaluation(
                        status = LauncherUpdateStatus.Supported,
                    ),
            )

        assertFalse(state.showLauncherUpdateSignal)
    }

    @Test
    fun launcherUpdateSignalClicked_opensSheetWithoutStartingCharge() =
        runTest {
            val repository = createRepository(manifestWithLauncherUpdateJson())
            repository.loadAndRefresh()

            val viewModel = createViewModel(repository)
            viewModel.onEvent(AboutEvent.Started)
            delay(50)

            if (viewModel.state.value.updateEvaluation
                    ?.status != LauncherUpdateStatus.UpdateAvailable
            ) {
                return@runTest
            }

            viewModel.onEvent(AboutEvent.LauncherUpdateSignalClicked)

            assertTrue(viewModel.state.value.isLauncherUpdateSheetVisible)
            assertFalse(viewModel.state.value.isUpdateCharging)
        }

    @Test
    fun telemetryDefaults_crashReportsOnExtendedOff() =
        runTest {
            val viewModel = createViewModel(createRepository(manifestWithLauncherUpdateJson()))
            viewModel.onEvent(AboutEvent.Started)
            assertTrue(viewModel.state.value.sendCrashReports)
            assertFalse(viewModel.state.value.shareExtendedDiagnostics)
        }

    @Test
    fun togglingCrashReportsOff_disablesExtendedDiagnostics() =
        runTest {
            val store =
                InMemoryTelemetryPreferencesStore(
                    TelemetryPreferences(sendCrashReports = true, shareExtendedDiagnostics = true),
                )
            val viewModel = createViewModel(createRepository(manifestWithLauncherUpdateJson()), store)
            viewModel.onEvent(AboutEvent.Started)
            assertTrue(viewModel.state.value.shareExtendedDiagnostics)

            viewModel.onEvent(AboutEvent.SendCrashReportsToggled)

            assertFalse(viewModel.state.value.sendCrashReports)
            assertFalse(viewModel.state.value.shareExtendedDiagnostics)
            assertFalse(viewModel.state.value.extendedDiagnosticsEnabled)
        }

    @Test
    fun extendedDiagnosticsToggle_ignoredWhenCrashReportsOff() =
        runTest {
            val store =
                InMemoryTelemetryPreferencesStore(
                    TelemetryPreferences(sendCrashReports = false, shareExtendedDiagnostics = false),
                )
            val viewModel = createViewModel(createRepository(manifestWithLauncherUpdateJson()), store)
            viewModel.onEvent(AboutEvent.Started)

            viewModel.onEvent(AboutEvent.ShareExtendedDiagnosticsToggled)

            assertFalse(viewModel.state.value.shareExtendedDiagnostics)
        }

    @Test
    fun sentryTestButton_visibleOnlyOnDevBuild() {
        System.clearProperty("game.launcher.dev")
        assertFalse(AboutState(isDevBuild = false).showSentryTestButton)

        System.setProperty("game.launcher.dev", "true")
        assertTrue(AboutState(isDevBuild = true).showSentryTestButton)
    }

    @Test
    fun testSentryClicked_onDevBuild_updatesStatus() =
        runTest {
            System.setProperty("game.launcher.dev", "true")
            val viewModel = createViewModel(createRepository(manifestWithLauncherUpdateJson()))
            viewModel.onEvent(AboutEvent.Started)
            viewModel.onEvent(AboutEvent.TestSentryClicked)
            assertTrue(
                viewModel.state.value.sentryTestStatus
                    ?.contains("Test event sent") == true,
            )
        }

    @Test
    fun backgroundThemeSelected_updatesRepositoryAndState() =
        runTest {
            val settings = InMemoryLauncherSettingsRepository()
            val viewModel =
                createViewModel(
                    createRepository(manifestWithLauncherUpdateJson()),
                    settingsRepository = settings,
                )
            viewModel.onEvent(AboutEvent.Started)
            assertEquals(LauncherBackgroundTheme.DEFAULT, viewModel.state.value.backgroundTheme)
            assertEquals(LauncherBackgroundTheme.STATIC_TERMINAL, viewModel.state.value.backgroundTheme)

            viewModel.onEvent(AboutEvent.BackgroundThemeSelected(LauncherBackgroundTheme.SPECTRAL_TOPOLOGY))
            delay(20)

            assertEquals(LauncherBackgroundTheme.SPECTRAL_TOPOLOGY, settings.backgroundTheme.value)
            assertEquals(LauncherBackgroundTheme.SPECTRAL_TOPOLOGY, viewModel.state.value.backgroundTheme)
        }

    private fun createViewModel(
        repository: LauncherUpdateRepository,
        store: InMemoryTelemetryPreferencesStore = InMemoryTelemetryPreferencesStore(),
        settingsRepository: InMemoryLauncherSettingsRepository = InMemoryLauncherSettingsRepository(),
    ): AboutViewModel = AboutViewModel(repository, store, settingsRepository)

    private fun createRepository(manifestJson: String): LauncherUpdateRepository {
        val manifestClient =
            HttpClient(
                MockEngine {
                    respond(
                        content = manifestJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            )

        return LauncherUpdateRepository(
            manifestRepository = ManifestRepository(manifestClient),
            updateInstaller = NoOpLauncherUpdateInstaller(),
        )
    }

    private class NoOpLauncherUpdateInstaller : LauncherUpdateInstaller {
        private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
        override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress

        override suspend fun downloadAndApply(
            channelBuild: LauncherChannelBuild,
            versionLabel: String,
        ): Result<Unit> = Result.success(Unit)
    }

    private fun manifestWithLauncherUpdateJson(): String =
        """
        {
          "launcher_minimum_version": "0.0.1",
          "launcher": {
            "release_notes_url": "https://github.com/morphingcoffee/GameLauncher/releases",
            "channels": {
              "windows-x64-msi": {
                "version": "0.0.1-build99",
                "artifact_type": "msi",
                "download_url": "https://cdn.example/launcher.msi",
                "file_size_bytes": 100,
                "sha256": "abc"
              },
              "windows-x64-portable": {
                "version": "0.0.1-build99",
                "artifact_type": "zip",
                "download_url": "https://cdn.example/launcher.zip",
                "file_size_bytes": 100,
                "sha256": "abc"
              },
              "macos-arm64-dmg": {
                "version": "0.0.1-build99",
                "artifact_type": "dmg",
                "download_url": "https://cdn.example/launcher-arm64.dmg",
                "file_size_bytes": 100,
                "sha256": "abc"
              },
              "macos-x64-dmg": {
                "version": "0.0.1-build99",
                "artifact_type": "dmg",
                "download_url": "https://cdn.example/launcher-x64.dmg",
                "file_size_bytes": 100,
                "sha256": "abc"
              }
            }
          },
          "games": []
        }
        """.trimIndent()
}
