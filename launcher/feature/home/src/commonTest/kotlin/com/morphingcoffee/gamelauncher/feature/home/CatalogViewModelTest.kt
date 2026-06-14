package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.ui.graphics.Color
import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.DownloadProgress
import com.morphingcoffee.gamelauncher.core.network.GameCatalogDataSource
import com.morphingcoffee.gamelauncher.core.network.GameCatalogRepository
import com.morphingcoffee.gamelauncher.core.network.InstallState
import com.morphingcoffee.gamelauncher.core.network.ManifestRepository
import com.morphingcoffee.gamelauncher.core.network.createDownloadHttpClient
import com.morphingcoffee.gamelauncher.core.network.createGameInstaller
import com.morphingcoffee.gamelauncher.core.network.createGameLauncher
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogViewModelTest {
    @Test
    fun started_loadsCatalogAndSelectsFirstGame() =
        runBlocking {
            val repository = createRepository(createManifestRepository(sampleManifestJson()))
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("READY", state.statusLabel)
            assertEquals(2, state.games.size)
            assertEquals("alpha", state.selectedGameId)
            assertNull(state.errorMessage)
        }

    @Test
    fun moveSelection_updatesSelectedGame() =
        runBlocking {
            val repository = createRepository(createManifestRepository(sampleManifestJson()))
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(CatalogEvent.MoveSelection(1))
            delay(50)

            assertEquals("beta", viewModel.state.value.selectedGameId)
        }

    @Test
    fun loadFailure_setsErrorState() =
        runBlocking {
            val client =
                HttpClient(
                    MockEngine {
                        respond(
                            content = "not-json",
                            status = HttpStatusCode.InternalServerError,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                ) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
            val repository =
                GameCatalogRepository(
                    ManifestRepository(client),
                    createGameLauncher(),
                    createGameInstaller(createDownloadHttpClient()),
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("ERROR", state.statusLabel)
            assertEquals(null, state.selectedGameId)
        }

    @Test
    fun launchClicked_startsChargeWhenOlderVersionHasPlatformBuild() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val otherPlatformKey =
                listOf(PlatformKey.WINDOWS_X64, PlatformKey.MACOS_ARM64, PlatformKey.MACOS_X64)
                    .first { it != platformKey }
            val legacyBuild =
                GameBuild(
                    downloadUrl = "https://example.com/legacy.zip",
                    executablePath = "Game.app/Contents/MacOS/Game",
                    fileSizeBytes = 1024,
                    sha256 = "legacy-sha",
                )
            val game =
                GameCatalogEntry(
                    id = "legacy",
                    title = "Legacy Platform",
                    description = "Latest version is unavailable on this platform",
                    latestVersion = "2.0.0",
                    versionsUrl = "https://example.com/legacy/versions.json",
                    builds =
                        mapOf(
                            otherPlatformKey to legacyBuild,
                        ),
                    versionHistory =
                        listOf(
                            GameVersionEntry(
                                version = "1.0.0",
                                releasedAt = "2024-01-01",
                                builds = mapOf(platformKey to legacyBuild),
                            ),
                            GameVersionEntry(
                                version = "2.0.0",
                                releasedAt = "2025-01-01",
                                builds = mapOf(otherPlatformKey to legacyBuild),
                            ),
                        ),
                )
            val repository =
                StubGameCatalogDataSource(
                    games = listOf(game),
                    installState =
                        InstallState.Installed(
                            version = "1.0.0",
                            executablePath = legacyBuild.executablePath,
                        ),
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(CatalogEvent.VersionPickerToggled)
            viewModel.onEvent(CatalogEvent.VersionSelected("1.0.0"))
            delay(50)

            assertFalse(
                viewModel.state.value.selectedGame!!
                    .isAvailableOnCurrentPlatform(),
            )
            assertTrue(viewModel.state.value.isInstalledForDisplay)
            assertTrue(viewModel.state.value.displayBuild != null)

            viewModel.onEvent(CatalogEvent.LaunchClicked)

            assertTrue(viewModel.state.value.isChargingLaunch)
        }

    @Test
    fun fastGameSwitch_ignoresStaleInstallStateProbe() =
        runBlocking {
            val repository =
                DelayedInstallStateDataSource(
                    installStates =
                        mapOf(
                            "alpha" to
                                InstallState.Installed(
                                    version = "0.0.1",
                                    executablePath = "Game.app/Contents/MacOS/Game",
                                ),
                            "beta" to InstallState.NotInstalled,
                        ),
                    delaysMs =
                        mapOf(
                            "alpha" to 200,
                            "beta" to 0,
                        ),
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            assertEquals("alpha", viewModel.state.value.selectedGameId)

            viewModel.onEvent(CatalogEvent.MoveSelection(1))
            delay(300)

            assertEquals("beta", viewModel.state.value.selectedGameId)
            assertEquals(InstallState.NotInstalled, viewModel.state.value.installState)
        }

    @Test
    fun gameSelection_clearsAmbientColorAndResetsVersionState() =
        runBlocking {
            val repository =
                createRepository(
                    createManifestRepository(sampleManifestJson()),
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(
                CatalogEvent.AmbientColorExtracted(
                    color = Color.Red,
                    imageUrl = "https://example.com/alpha.webp",
                ),
            )
            viewModel.onEvent(CatalogEvent.VersionPickerToggled)
            viewModel.onEvent(CatalogEvent.VersionSelected("0.0.1"))
            delay(50)

            viewModel.onEvent(CatalogEvent.MoveSelection(1))
            delay(50)

            val state = viewModel.state.value
            assertEquals("beta", state.selectedGameId)
            assertNull(state.selectedVersion)
            assertEquals(Color.Transparent, state.ambientColor)
        }

    private fun createRepository(manifestRepository: ManifestRepository): GameCatalogRepository =
        GameCatalogRepository(
            manifestRepository = manifestRepository,
            gameLauncher = createGameLauncher(),
            gameInstaller = createGameInstaller(createDownloadHttpClient()),
        )

    private suspend fun waitForLoadingToFinish(viewModel: CatalogViewModel) {
        repeat(100) {
            if (!viewModel.state.value.isLoading) return
            delay(25)
        }
        error("Catalog load did not complete in time: ${viewModel.state.value}")
    }

    private fun createManifestRepository(manifestJson: String): ManifestRepository {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        content = manifestJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        return ManifestRepository(client, manifestUrl = "https://example.com/manifest.json")
    }

    private class DelayedInstallStateDataSource(
        private val installStates: Map<String, InstallState>,
        private val delaysMs: Map<String, Long> = emptyMap(),
    ) : GameCatalogDataSource {
        private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
        override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress

        override suspend fun loadCatalog(): Result<List<GameCatalogEntry>> =
            Result.success(
                listOf(
                    GameCatalogEntry(
                        id = "alpha",
                        title = "Alpha Build",
                        description = "Preview",
                        latestVersion = "0.0.1",
                        versionsUrl = "https://example.com/alpha/versions.json",
                        builds = emptyMap(),
                    ),
                    GameCatalogEntry(
                        id = "beta",
                        title = "Beta Showcase",
                        description = "Preview",
                        latestVersion = "0.0.1",
                        versionsUrl = "https://example.com/beta/versions.json",
                        builds = emptyMap(),
                    ),
                ),
            )

        override suspend fun fetchVersionHistory(versionsUrl: String): Result<List<GameVersionEntry>> =
            Result.success(emptyList())

        override suspend fun downloadAndInstall(
            gameId: String,
            version: String,
            build: GameBuild,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun getInstallState(gameId: String): InstallState {
            delaysMs[gameId]?.let { delay(it) }
            return installStates[gameId] ?: InstallState.Unknown
        }

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)
    }

    private class StubGameCatalogDataSource(
        private val games: List<GameCatalogEntry>,
        private val installState: InstallState,
    ) : GameCatalogDataSource {
        private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
        override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress

        override suspend fun loadCatalog(): Result<List<GameCatalogEntry>> = Result.success(games)

        override suspend fun fetchVersionHistory(versionsUrl: String): Result<List<GameVersionEntry>> {
            val gameId = versionsUrl.removeSuffix("/versions.json").substringAfterLast("/")
            val game =
                games.firstOrNull { it.id == gameId }
                    ?: return Result.failure(IllegalArgumentException("Unknown game: $gameId"))
            return Result.success(game.versionHistory)
        }

        override suspend fun downloadAndInstall(
            gameId: String,
            version: String,
            build: GameBuild,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun getInstallState(gameId: String): InstallState = installState

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)
    }

    private fun sampleManifestJson(): String =
        """
        {
          "schema_version": 1,
          "launcher_minimum_version": "0.0.1",
          "games": [
            {
              "id": "alpha",
              "title": "Alpha Build",
              "description": "Preview",
              "thumbnail_url": "https://example.com/alpha.webp",
              "latest_version": "0.0.1",
              "versions_url": "https://example.com/alpha/versions.json",
              "builds": {
                "macos-arm64": {
                  "download_url": "https://example.com/alpha.zip",
                  "executable_path": "Game.app/Contents/MacOS/Game",
                  "file_size_bytes": 1024,
                  "sha256": "abc"
                }
              }
            },
            {
              "id": "beta",
              "title": "Beta Showcase",
              "description": "Preview",
              "thumbnail_url": "https://example.com/beta.webp",
              "latest_version": "0.0.1",
              "versions_url": "https://example.com/beta/versions.json",
              "builds": {
                "macos-arm64": {
                  "download_url": "https://example.com/beta.zip",
                  "executable_path": "Game.app/Contents/MacOS/Game",
                  "file_size_bytes": 1024,
                  "sha256": "abc"
                }
              }
            }
          ]
        }
        """.trimIndent()
}
