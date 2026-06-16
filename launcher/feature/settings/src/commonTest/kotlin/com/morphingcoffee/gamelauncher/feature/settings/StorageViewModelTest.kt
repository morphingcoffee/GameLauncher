package com.morphingcoffee.gamelauncher.feature.settings

import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry
import com.morphingcoffee.gamelauncher.core.network.DownloadProgress
import com.morphingcoffee.gamelauncher.core.network.GameCatalogDataSource
import com.morphingcoffee.gamelauncher.core.network.InstallState
import com.morphingcoffee.gamelauncher.core.network.InstalledGameSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StorageViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun started_loadsInstalledSegmentsAndTotal() =
        runTest {
            val dataSource =
                StorageTestDataSource(
                    installed =
                        listOf(
                            InstalledGameSummary("alpha", "1.0.0", 100L),
                            InstalledGameSummary("beta", "2.0.0", 300L),
                        ),
                    catalog =
                        listOf(
                            catalogEntry("alpha", "Alpha Game"),
                            catalogEntry("beta", "Beta Game"),
                        ),
                )
            val viewModel = StorageViewModel(dataSource)

            viewModel.onEvent(StorageEvent.Started)
            waitForLoaded(viewModel)

            val state = viewModel.state.value
            assertEquals(400L, state.totalBytes)
            assertEquals(2, state.segments.size)
            assertEquals("Beta Game", state.segments[0].title)
            assertEquals("Alpha Game", state.segments[1].title)
            assertEquals(0.75f, state.segments[0].shareFraction)
        }

    @Test
    fun uninstallGame_removesSegment() =
        runTest {
            val dataSource =
                StorageTestDataSource(
                    installed =
                        listOf(
                            InstalledGameSummary("alpha", "1.0.0", 100L),
                            InstalledGameSummary("beta", "2.0.0", 300L),
                        ),
                    catalog = listOf(catalogEntry("alpha", "Alpha Game"), catalogEntry("beta", "Beta Game")),
                )
            val viewModel = StorageViewModel(dataSource)
            viewModel.onEvent(StorageEvent.Started)
            waitForLoaded(viewModel)

            val beta =
                viewModel.state.value.segments
                    .first { it.gameId == "beta" }
            viewModel.onEvent(StorageEvent.SegmentClicked(beta.gameId))
            viewModel.onEvent(StorageEvent.UninstallClicked)
            viewModel.onEvent(StorageEvent.UninstallChargeComplete)
            viewModel.onEvent(StorageEvent.ChartAnimationFinished)
            viewModel.onEvent(StorageEvent.ChartAnimationFinished)
            waitForIdle(viewModel)

            assertEquals(1, viewModel.state.value.segments.size)
            assertEquals(
                "alpha",
                viewModel.state.value.segments
                    .single()
                    .gameId,
            )
            assertEquals(100L, viewModel.state.value.totalBytes)
            assertNull(viewModel.state.value.activeDialog)
        }

    @Test
    fun uninstallAll_clearsSegments() =
        runTest {
            val dataSource =
                StorageTestDataSource(
                    installed =
                        listOf(
                            InstalledGameSummary("alpha", "1.0.0", 100L),
                            InstalledGameSummary("beta", "2.0.0", 300L),
                        ),
                    catalog = listOf(catalogEntry("alpha", "Alpha Game"), catalogEntry("beta", "Beta Game")),
                )
            val viewModel = StorageViewModel(dataSource)
            viewModel.onEvent(StorageEvent.Started)
            waitForLoaded(viewModel)

            viewModel.onEvent(StorageEvent.CenterClicked)
            viewModel.onEvent(StorageEvent.UninstallAllClicked)
            viewModel.onEvent(StorageEvent.UninstallAllChargeComplete)
            viewModel.onEvent(StorageEvent.ChartAnimationFinished)
            waitForIdle(viewModel)

            val segments = viewModel.state.value.segments
            assertTrue(segments.isEmpty())
            assertEquals(0L, viewModel.state.value.totalBytes)
        }

    private suspend fun TestScope.waitForLoaded(viewModel: StorageViewModel) {
        repeat(40) {
            advanceUntilIdle()
            if (!viewModel.state.value.isLoading) return
            kotlinx.coroutines.delay(25)
        }
        advanceUntilIdle()
    }

    private suspend fun TestScope.waitForIdle(viewModel: StorageViewModel) {
        repeat(40) {
            advanceUntilIdle()
            val state = viewModel.state.value
            if (!state.isLoading && !state.isUninstalling && state.chartAnimation == null) return
            kotlinx.coroutines.delay(25)
        }
        advanceUntilIdle()
    }

    private fun catalogEntry(
        id: String,
        title: String,
    ): GameCatalogEntry =
        GameCatalogEntry(
            id = id,
            title = title,
            description = "Test",
            thumbnailUrl = null,
            latestVersion = "1.0.0",
            versionsUrl = "https://example.com/$id/versions.json",
            builds = emptyMap(),
            versionHistory = emptyList(),
        )

    private class StorageTestDataSource(
        installed: List<InstalledGameSummary>,
        private val catalog: List<GameCatalogEntry>,
    ) : GameCatalogDataSource {
        private val installedGames = installed.associate { it.gameId to it.version }.toMutableMap()
        private val sizes = installed.associate { it.gameId to it.sizeBytes }.toMutableMap()

        private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
        override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress

        override suspend fun loadCatalog(): Result<List<GameCatalogEntry>> = Result.success(catalog)

        override suspend fun fetchVersionHistory(versionsUrl: String): Result<List<GameVersionEntry>> =
            Result.success(emptyList())

        override suspend fun downloadAndInstall(
            gameId: String,
            version: String,
            build: GameBuild,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun getInstallState(gameId: String): InstallState =
            if (gameId in installedGames) {
                InstallState.Installed(version = installedGames.getValue(gameId), executablePath = "Game")
            } else {
                InstallState.NotInstalled
            }

        override suspend fun uninstallGame(gameId: String): Result<Unit> =
            runCatching {
                check(installedGames.remove(gameId) != null) { "Not installed" }
                sizes.remove(gameId)
            }

        override suspend fun getOnDiskSizeBytes(gameId: String): Long? = sizes[gameId]

        override suspend fun launchGame(gameId: String): Result<Unit> = Result.success(Unit)

        override suspend fun openWebGame(url: String): Result<Unit> = Result.success(Unit)

        override suspend fun listInstalledGames(): List<InstalledGameSummary> =
            installedGames
                .mapNotNull { (gameId, version) ->
                    val sizeBytes = sizes[gameId] ?: return@mapNotNull null
                    InstalledGameSummary(gameId, version, sizeBytes)
                }.sortedByDescending { it.sizeBytes }

        override suspend fun uninstallAllGames(): Result<Unit> =
            runCatching {
                installedGames.clear()
                sizes.clear()
            }
    }
}
