package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LauncherUpdateRepositoryTest {
    @BeforeTest
    fun clearDevProperty() {
        System.clearProperty("game.launcher.dev")
    }

    @AfterTest
    fun tearDown() {
        System.clearProperty("game.launcher.dev")
    }

    @Test
    fun loadAndRefresh_inDevBuild_skipsManifestFetchAndDisablesUpdates() =
        runTest {
            System.setProperty("game.launcher.dev", "true")

            val manifestClient =
                HttpClient(
                    MockEngine {
                        error("Prod manifest must not be fetched in dev builds")
                    },
                )

            val repository =
                LauncherUpdateRepository(
                    manifestRepository = ManifestRepository(manifestClient),
                    updateInstaller = NoOpLauncherUpdateInstaller(),
                )

            val result = repository.loadAndRefresh()

            assertEquals(ManifestLoadResult.SkippedInDevBuild, result)
            assertEquals(LauncherUpdateStatus.Supported, repository.evaluation.value?.status)
            assertTrue(repository.downloadAndApplyUpdate().isFailure)
        }

    @Test
    fun loadAndRefresh_inProdBuild_fetchesAndEvaluatesManifest() =
        runTest {
            val manifestJson =
                """
                {
                  "launcher_minimum_version": "99.0.0",
                  "games": []
                }
                """.trimIndent()

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

            val repository =
                LauncherUpdateRepository(
                    manifestRepository = ManifestRepository(manifestClient),
                    updateInstaller = NoOpLauncherUpdateInstaller(),
                )

            val result = repository.loadAndRefresh()

            assertTrue(result is ManifestLoadResult.Success)
            assertEquals(LauncherUpdateStatus.ManualUpdateRequired, repository.evaluation.value?.status)
        }

    private class NoOpLauncherUpdateInstaller : LauncherUpdateInstaller {
        private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
        override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress

        override suspend fun downloadAndApply(
            channelBuild: LauncherChannelBuild,
            versionLabel: String,
        ): Result<Unit> = Result.success(Unit)
    }
}
