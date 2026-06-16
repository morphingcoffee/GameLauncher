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
    fun launchComplete_restoresCatalogUi() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val build =
                GameBuild(
                    downloadUrl = "https://example.com/alpha.zip",
                    executablePath = "Game.app/Contents/MacOS/Game",
                    fileSizeBytes = 1024,
                    sha256 = "abc",
                )
            val repository =
                BlockingLaunchDataSource(
                    platformKey = platformKey,
                    build = build,
                    launchDelayMs = 100,
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(CatalogEvent.LaunchChargeComplete)

            delay(20)
            assertEquals(0f, viewModel.state.value.contentAlpha)
            assertTrue(viewModel.state.value.isLaunching)

            delay(200)

            assertEquals(1f, viewModel.state.value.contentAlpha)
            assertFalse(viewModel.state.value.isLaunching)
            assertEquals("READY", viewModel.state.value.statusLabel)
        }

    @Test
    fun openClicked_opensWebGameWithoutInstall() =
        runBlocking {
            val webUrl = "https://morphingcoffee.github.io/apps/games/"
            val repository = WebGameOpenDataSource(webUrl = webUrl)
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)

            assertTrue(viewModel.state.value.isWebGame)
            assertFalse(viewModel.state.value.isInstalledForDisplay)
            assertEquals(
                webUrl,
                viewModel.state.value.displayBuild
                    ?.downloadUrl,
            )

            viewModel.onEvent(CatalogEvent.OpenClicked)
            delay(50)

            assertEquals("READY", viewModel.state.value.statusLabel)
            assertEquals(webUrl, repository.openedUrl)
        }

    @Test
    fun fastGameSwitch_ignoresStaleVersionHistoryFetch() =
        runBlocking {
            val alphaHistory =
                listOf(
                    GameVersionEntry(
                        version = "0.0.1",
                        releasedAt = "2024-01-01",
                        builds = emptyMap(),
                    ),
                )
            val repository =
                DelayedVersionHistoryDataSource(
                    historiesByUrl =
                        mapOf(
                            "https://example.com/alpha/versions.json" to alphaHistory,
                        ),
                    delaysMs =
                        mapOf(
                            "https://example.com/alpha/versions.json" to 200,
                        ),
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(CatalogEvent.VersionPickerToggled)
            viewModel.onEvent(CatalogEvent.MoveSelection(1))
            delay(300)

            assertEquals("beta", viewModel.state.value.selectedGameId)
            assertTrue(
                viewModel.state.value.versionHistory
                    .isEmpty(),
            )
        }

    @Test
    fun repeatedDownloadClick_startsOnlyOneInstallJob() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val repository = CountingDownloadDataSource(platformKey)
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(CatalogEvent.DownloadClicked)
            viewModel.onEvent(CatalogEvent.DownloadClicked)
            delay(300)

            assertEquals(1, repository.downloadInvocationCount)
            assertFalse(viewModel.state.value.isDownloading)
        }

    @Test
    fun gameSelection_hidesDownloadWhileInstallStatePending() =
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
                        ),
                    delaysMs = mapOf("alpha" to 200),
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)

            assertTrue(viewModel.state.value.isInstallStatePending)

            delay(250)

            assertFalse(viewModel.state.value.isInstallStatePending)
            assertTrue(viewModel.state.value.isInstalledForDisplay)
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

    @Test
    fun uninstallClicked_startsChargeWhenInstalled() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val build =
                GameBuild(
                    downloadUrl = "https://example.com/alpha.zip",
                    executablePath = "Game.app/Contents/MacOS/Game",
                    fileSizeBytes = 1024,
                    sha256 = "abc",
                )
            val repository =
                StubGameCatalogDataSource(
                    games =
                        listOf(
                            GameCatalogEntry(
                                id = "alpha",
                                title = "Alpha Build",
                                description = "Preview",
                                latestVersion = "0.0.1",
                                versionsUrl = "https://example.com/alpha/versions.json",
                                builds = mapOf(platformKey to build),
                            ),
                        ),
                    installState =
                        InstallState.Installed(
                            version = "0.0.1",
                            executablePath = build.executablePath,
                        ),
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            delay(50)

            viewModel.onEvent(CatalogEvent.UninstallClicked)

            assertTrue(viewModel.state.value.isChargingUninstall)
        }

    @Test
    fun uninstallComplete_setsNotInstalled() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val build =
                GameBuild(
                    downloadUrl = "https://example.com/alpha.zip",
                    executablePath = "Game.app/Contents/MacOS/Game",
                    fileSizeBytes = 1024,
                    sha256 = "abc",
                )
            val repository = UninstallTrackingDataSource(platformKey, build)
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            delay(50)
            viewModel.onEvent(CatalogEvent.UninstallClicked)
            viewModel.onEvent(CatalogEvent.UninstallChargeComplete)
            delay(50)

            assertEquals(InstallState.NotInstalled, viewModel.state.value.installState)
            assertFalse(viewModel.state.value.isInstalledForDisplay)
            assertFalse(viewModel.state.value.isChargingUninstall)
            assertNull(viewModel.state.value.onDiskSizeBytes)
            assertTrue(repository.uninstallInvoked)
        }

    @Test
    fun uninstallChargeComplete_afterUninstallClicked_invokesUninstall() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val build =
                GameBuild(
                    downloadUrl = "https://example.com/alpha.zip",
                    executablePath = "Game.app/Contents/MacOS/Game",
                    fileSizeBytes = 1024,
                    sha256 = "abc",
                )
            val repository = UninstallTrackingDataSource(platformKey, build)
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            delay(50)

            assertTrue(viewModel.state.value.canUninstall)
            viewModel.onEvent(CatalogEvent.UninstallClicked)
            assertTrue(viewModel.state.value.isChargingUninstall)
            assertFalse(viewModel.state.value.canUninstall)

            viewModel.onEvent(CatalogEvent.UninstallChargeComplete)
            delay(50)

            assertFalse(viewModel.state.value.isChargingUninstall)
            assertTrue(repository.uninstallInvoked)
            assertEquals(InstallState.NotInstalled, viewModel.state.value.installState)
        }

    @Test
    fun repeatedUninstallChargeComplete_startsOnlyOneUninstallJob() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val build =
                GameBuild(
                    downloadUrl = "https://example.com/alpha.zip",
                    executablePath = "Game.app/Contents/MacOS/Game",
                    fileSizeBytes = 1024,
                    sha256 = "abc",
                )
            val repository =
                DelayedUninstallDataSource(
                    platformKey = platformKey,
                    build = build,
                    uninstallDelayMs = 200,
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            delay(50)
            viewModel.onEvent(CatalogEvent.UninstallClicked)
            viewModel.onEvent(CatalogEvent.UninstallChargeComplete)
            viewModel.onEvent(CatalogEvent.UninstallChargeComplete)
            delay(300)

            assertEquals(1, repository.uninstallInvocationCount)
        }

    @Test
    fun uninstallBlockedWhileLaunching() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val build =
                GameBuild(
                    downloadUrl = "https://example.com/alpha.zip",
                    executablePath = "Game.app/Contents/MacOS/Game",
                    fileSizeBytes = 1024,
                    sha256 = "abc",
                )
            val repository =
                BlockingLaunchDataSource(
                    platformKey = platformKey,
                    build = build,
                    launchDelayMs = 500,
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(CatalogEvent.LaunchChargeComplete)
            delay(20)

            assertTrue(viewModel.state.value.isLaunching)
            assertFalse(viewModel.state.value.canUninstall)

            viewModel.onEvent(CatalogEvent.UninstallClicked)

            assertFalse(viewModel.state.value.isChargingUninstall)
        }

    @Test
    fun uninstallBlockedWhileDownloading() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val repository = CountingDownloadDataSource(platformKey)
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(CatalogEvent.DownloadClicked)
            delay(20)

            assertTrue(viewModel.state.value.isDownloading)
            assertFalse(viewModel.state.value.canUninstall)

            viewModel.onEvent(CatalogEvent.UninstallClicked)

            assertFalse(viewModel.state.value.isChargingUninstall)
        }

    @Test
    fun fastGameSwitch_ignoresStaleUninstallResult() =
        runBlocking {
            val platformKey = PlatformKey.current() ?: return@runBlocking
            val build =
                GameBuild(
                    downloadUrl = "https://example.com/alpha.zip",
                    executablePath = "Game.app/Contents/MacOS/Game",
                    fileSizeBytes = 1024,
                    sha256 = "abc",
                )
            val repository =
                DelayedUninstallDataSource(
                    platformKey = platformKey,
                    build = build,
                    uninstallDelayMs = 200,
                )
            val viewModel = CatalogViewModel(repository)

            viewModel.onEvent(CatalogEvent.Started)
            waitForLoadingToFinish(viewModel)
            viewModel.onEvent(CatalogEvent.UninstallChargeComplete)
            viewModel.onEvent(CatalogEvent.MoveSelection(1))
            delay(300)

            assertEquals("beta", viewModel.state.value.selectedGameId)
            assertEquals(InstallState.NotInstalled, viewModel.state.value.installState)
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

    private class WebGameOpenDataSource(
        private val webUrl: String,
    ) : GameCatalogDataSource {
        var openedUrl: String? = null
        private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
        override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress

        override suspend fun loadCatalog(): Result<List<GameCatalogEntry>> =
            Result.success(
                listOf(
                    GameCatalogEntry(
                        id = "game-gallery",
                        title = "Game Gallery",
                        description = "Browser prototypes",
                        latestVersion = "1.0.0",
                        versionsUrl = "https://example.com/game-gallery/versions.json",
                        builds =
                            mapOf(
                                PlatformKey.WEB to
                                    GameBuild(
                                        downloadUrl = webUrl,
                                        executablePath = "",
                                        fileSizeBytes = 0,
                                        sha256 = "",
                                    ),
                            ),
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

        override suspend fun getInstallState(gameId: String): InstallState = InstallState.NotInstalled

        override suspend fun uninstallGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun getOnDiskSizeBytes(gameId: String): Long? = null

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun openWebGame(url: String): Result<Unit> {
            openedUrl = url
            return Result.success(Unit)
        }

        override suspend fun uninstallAllGames(): Result<Unit> = Result.success(Unit)
    }

    private class BlockingLaunchDataSource(
        private val platformKey: String,
        private val build: GameBuild,
        private val launchDelayMs: Long,
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
                        builds = mapOf(platformKey to build),
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

        override suspend fun getInstallState(gameId: String): InstallState =
            InstallState.Installed(
                version = "0.0.1",
                executablePath = build.executablePath,
            )

        override suspend fun uninstallGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun getOnDiskSizeBytes(gameId: String): Long? = null

        override suspend fun launchGame(gameId: String): Result<Unit> {
            delay(launchDelayMs)
            return Result.success(Unit)
        }

        override suspend fun openWebGame(url: String): Result<Unit> = Result.success(Unit)

        override suspend fun uninstallAllGames(): Result<Unit> = Result.success(Unit)
    }

    private class CountingDownloadDataSource(
        private val platformKey: String,
    ) : GameCatalogDataSource {
        var downloadInvocationCount = 0
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
                        builds =
                            mapOf(
                                platformKey to
                                    GameBuild(
                                        downloadUrl = "https://example.com/alpha.zip",
                                        executablePath = "Game.app/Contents/MacOS/Game",
                                        fileSizeBytes = 1024,
                                        sha256 = "abc",
                                    ),
                            ),
                    ),
                ),
            )

        override suspend fun fetchVersionHistory(versionsUrl: String): Result<List<GameVersionEntry>> =
            Result.success(emptyList())

        override suspend fun downloadAndInstall(
            gameId: String,
            version: String,
            build: GameBuild,
        ): Result<Unit> {
            downloadInvocationCount++
            delay(200)
            return Result.success(Unit)
        }

        override suspend fun getInstallState(gameId: String): InstallState = InstallState.NotInstalled

        override suspend fun uninstallGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun getOnDiskSizeBytes(gameId: String): Long? = null

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun openWebGame(url: String): Result<Unit> = Result.success(Unit)

        override suspend fun uninstallAllGames(): Result<Unit> = Result.success(Unit)
    }

    private class DelayedVersionHistoryDataSource(
        private val historiesByUrl: Map<String, List<GameVersionEntry>>,
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

        override suspend fun fetchVersionHistory(versionsUrl: String): Result<List<GameVersionEntry>> {
            delaysMs[versionsUrl]?.let { delay(it) }
            return Result.success(historiesByUrl[versionsUrl] ?: emptyList())
        }

        override suspend fun downloadAndInstall(
            gameId: String,
            version: String,
            build: GameBuild,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun getInstallState(gameId: String): InstallState = InstallState.Unknown

        override suspend fun uninstallGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun getOnDiskSizeBytes(gameId: String): Long? = null

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun openWebGame(url: String): Result<Unit> = Result.success(Unit)

        override suspend fun uninstallAllGames(): Result<Unit> = Result.success(Unit)
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

        override suspend fun uninstallGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun getOnDiskSizeBytes(gameId: String): Long? = null

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun openWebGame(url: String): Result<Unit> = Result.success(Unit)

        override suspend fun uninstallAllGames(): Result<Unit> = Result.success(Unit)
    }

    private class UninstallTrackingDataSource(
        private val platformKey: String,
        private val build: GameBuild,
    ) : GameCatalogDataSource {
        var uninstallInvoked = false
        private var installState: InstallState =
            InstallState.Installed(
                version = "0.0.1",
                executablePath = build.executablePath,
            )
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
                        builds = mapOf(platformKey to build),
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

        override suspend fun getInstallState(gameId: String): InstallState = installState

        override suspend fun uninstallGame(gameId: String): Result<Unit> {
            uninstallInvoked = true
            installState = InstallState.NotInstalled
            return Result.success(Unit)
        }

        override suspend fun getOnDiskSizeBytes(gameId: String): Long? = 2048L

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun openWebGame(url: String): Result<Unit> = Result.success(Unit)

        override suspend fun uninstallAllGames(): Result<Unit> = Result.success(Unit)
    }

    private class DelayedUninstallDataSource(
        private val platformKey: String,
        private val build: GameBuild,
        private val uninstallDelayMs: Long,
    ) : GameCatalogDataSource {
        var uninstallInvocationCount = 0
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
                        builds = mapOf(platformKey to build),
                    ),
                    GameCatalogEntry(
                        id = "beta",
                        title = "Beta Showcase",
                        description = "Preview",
                        latestVersion = "0.0.1",
                        versionsUrl = "https://example.com/beta/versions.json",
                        builds = mapOf(platformKey to build),
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

        override suspend fun getInstallState(gameId: String): InstallState =
            if (gameId == "alpha") {
                InstallState.Installed(
                    version = "0.0.1",
                    executablePath = build.executablePath,
                )
            } else {
                InstallState.NotInstalled
            }

        override suspend fun uninstallGame(gameId: String): Result<Unit> {
            uninstallInvocationCount++
            delay(uninstallDelayMs)
            return Result.success(Unit)
        }

        override suspend fun getOnDiskSizeBytes(gameId: String): Long? = null

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun openWebGame(url: String): Result<Unit> = Result.success(Unit)

        override suspend fun uninstallAllGames(): Result<Unit> = Result.success(Unit)
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

        override suspend fun uninstallGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun getOnDiskSizeBytes(gameId: String): Long? = null

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun openWebGame(url: String): Result<Unit> = Result.success(Unit)

        override suspend fun uninstallAllGames(): Result<Unit> = Result.success(Unit)
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
