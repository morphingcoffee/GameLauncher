package com.morphingcoffee.gamelauncher

import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.DownloadProgress
import com.morphingcoffee.gamelauncher.core.network.GameCatalogDataSource
import com.morphingcoffee.gamelauncher.core.network.GameLauncher
import com.morphingcoffee.gamelauncher.core.network.InstallState
import com.morphingcoffee.gamelauncher.core.network.InstalledGameSummary
import com.morphingcoffee.gamelauncher.core.network.SimulatedLaunchException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class FakeGameCatalogDataSource(
    private val gameLauncher: GameLauncher? = null,
    private val catalogLoadDelayMs: LongRange = 600L..2_500L,
    private val launchDelayMs: LongRange = 800L..1_800L,
    private val versionHistoryDelayMs: LongRange = 300L..900L,
    seedDevInstalls: Boolean = true,
) : GameCatalogDataSource {
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    override val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private val installedGames = mutableMapOf<String, String>()

    init {
        if (seedDevInstalls) {
            // Each entry uses allPlatformBuilds at the seeded version so storage/dev works on every desktop OS.
            installedGames["void-runner"] = "1.4.2"
            installedGames["no-signal"] = "0.0.1"
            installedGames["long-title-alpha"] = "0.0.1"
        }
    }

    override suspend fun loadCatalog(): Result<List<GameCatalogEntry>> {
        delay(catalogLoadDelayMs.random())
        return Result.success(FAKE_CATALOG)
    }

    override suspend fun fetchVersionHistory(versionsUrl: String): Result<List<GameVersionEntry>> {
        delay(versionHistoryDelayMs.random())
        val gameId = versionsUrl.removeSuffix("/versions.json").substringAfterLast("/")
        val game =
            FAKE_CATALOG.firstOrNull { it.id == gameId }
                ?: return Result.failure(IllegalArgumentException("Unknown game id in versions URL: $gameId"))
        return Result.success(game.versionHistory)
    }

    override suspend fun downloadAndInstall(
        gameId: String,
        version: String,
        build: GameBuild,
    ): Result<Unit> {
        if (build.fileSizeBytes <= 0L) {
            return Result.failure(IllegalArgumentException("Build file size must be greater than zero"))
        }

        val entry =
            FAKE_CATALOG.firstOrNull { it.id == gameId }
                ?: return Result.failure(IllegalArgumentException("Unknown game: $gameId"))

        simulateDownloadProgress(entry.id, build.fileSizeBytes)
        installedGames[gameId] = version
        return Result.success(Unit)
    }

    override suspend fun getInstallState(gameId: String): InstallState {
        val version = installedGames[gameId] ?: return InstallState.NotInstalled
        val entry =
            FAKE_CATALOG.firstOrNull { it.id == gameId }
                ?: return InstallState.NotInstalled

        val build =
            entry.versionHistory.firstOrNull { it.version == version }?.buildForCurrentPlatform()
                ?: if (version == entry.latestVersion) {
                    entry.buildForCurrentPlatform()
                } else {
                    null
                }
                ?: return InstallState.NotInstalled

        return InstallState.Installed(
            version = version,
            executablePath = build.executablePath,
        )
    }

    override suspend fun uninstallGame(gameId: String): Result<Unit> {
        if (gameId !in installedGames) {
            return Result.failure(IllegalStateException("Game is not installed: $gameId"))
        }
        installedGames.remove(gameId)
        return Result.success(Unit)
    }

    override suspend fun getOnDiskSizeBytes(gameId: String): Long? {
        val version = installedGames[gameId] ?: return null
        val entry =
            FAKE_CATALOG.firstOrNull { it.id == gameId }
                ?: return null

        val build =
            entry.versionHistory.firstOrNull { it.version == version }?.buildForCurrentPlatform()
                ?: if (version == entry.latestVersion) {
                    entry.buildForCurrentPlatform()
                } else {
                    null
                }
                ?: return null

        // Simulated extracted size is ~8% larger than the download archive for dev previews.
        return (build.fileSizeBytes * 108L) / 100L
    }

    override suspend fun listInstalledGames(): List<InstalledGameSummary> =
        installedGames
            .mapNotNull { (gameId, version) ->
                val sizeBytes = getOnDiskSizeBytes(gameId) ?: return@mapNotNull null
                InstalledGameSummary(
                    gameId = gameId,
                    version = version,
                    sizeBytes = sizeBytes,
                )
            }.sortedByDescending { it.sizeBytes }

    override suspend fun uninstallAllGames(): Result<Unit> =
        runCatching {
            installedGames.keys.toList().forEach { gameId ->
                uninstallGame(gameId).getOrThrow()
            }
        }

    override suspend fun launchGame(gameId: String): Result<Unit> {
        val version =
            installedGames[gameId]
                ?: return Result.failure(IllegalStateException("Game is not installed: $gameId"))

        val entry =
            FAKE_CATALOG.firstOrNull { it.id == gameId }
                ?: return Result.failure(IllegalArgumentException("Unknown game: $gameId"))

        val build =
            entry.versionHistory.firstOrNull { it.version == version }?.buildForCurrentPlatform()
                ?: entry.buildForCurrentPlatform()
                ?: return Result.failure(IllegalStateException("No build available for installed version"))

        simulateDownloadProgress(entry.id, build.fileSizeBytes)
        delay(launchDelayMs.random())
        return Result.failure(SimulatedLaunchException(entry.title))
    }

    override suspend fun openWebGame(url: String): Result<Unit> =
        gameLauncher?.openUrl(url)
            ?: run {
                AppLog.i("FakeGameCatalog", "Would open browser: $url")
                Result.success(Unit)
            }

    private suspend fun simulateDownloadProgress(
        gameId: String,
        totalBytes: Long,
    ) {
        val steps = listOf(0.15f, 0.42f, 0.71f, 0.93f, 1f)
        for (fraction in steps) {
            _downloadProgress.value =
                DownloadProgress(
                    gameId = gameId,
                    bytesDownloaded = (totalBytes * fraction).toLong(),
                    totalBytes = totalBytes,
                )
            delay(Random.nextLong(180, 421))
        }
        _downloadProgress.value = null
    }
}

private val FAKE_CATALOG: List<GameCatalogEntry> =
    listOf(
        // All platforms — multi-version history
        fakeGame(
            id = "void-runner",
            title = "VOID RUNNER",
            thumbnailSeed = 101,
            builds = allPlatformBuilds(620_000_000L),
            version = "1.4.2",
            versionHistory =
                listOf(
                    fakeVersion("1.0.0", "2024-01-10", PlatformKey.WINDOWS_X64, PlatformKey.MACOS_ARM64),
                    fakeVersion(
                        "1.2.0",
                        "2024-06-01",
                        PlatformKey.WINDOWS_X64,
                        PlatformKey.MACOS_ARM64,
                        PlatformKey.MACOS_X64,
                    ),
                    fakeVersion(
                        "1.4.2",
                        "2025-03-15",
                        PlatformKey.WINDOWS_X64,
                        PlatformKey.MACOS_ARM64,
                        PlatformKey.MACOS_X64,
                        fileSizeBytes = 620_000_000L,
                    ),
                ),
        ),
        // Platform coverage shifts per version
        fakeGame(
            id = "neon-drift",
            title = "NEON DRIFT",
            thumbnailSeed = 202,
            builds = platformBuilds(PlatformKey.MACOS_ARM64, PlatformKey.MACOS_X64, fileSizeBytes = 380_000_000L),
            version = "0.9.0",
            versionHistory =
                listOf(
                    fakeVersion("0.5.0", "2024-02-01", PlatformKey.MACOS_ARM64),
                    fakeVersion("0.7.0", "2024-08-15", PlatformKey.WINDOWS_X64, PlatformKey.MACOS_ARM64),
                    fakeVersion(
                        "0.9.0",
                        "2025-01-20",
                        PlatformKey.MACOS_ARM64,
                        PlatformKey.MACOS_X64,
                        fileSizeBytes = 380_000_000L,
                    ),
                ),
        ),
        // Windows + macOS ARM — two-version history
        fakeGame(
            id = "cross-wire",
            title = "CROSS WIRE",
            thumbnailSeed = 203,
            builds = platformBuilds(PlatformKey.WINDOWS_X64, PlatformKey.MACOS_ARM64),
            version = "0.0.2",
            versionHistory =
                listOf(
                    fakeVersion("0.0.1", "2024-11-01", PlatformKey.MACOS_ARM64),
                    fakeVersion("0.0.2", "2025-05-01", PlatformKey.WINDOWS_X64, PlatformKey.MACOS_ARM64),
                ),
        ),
        // macOS only (ARM + X64)
        fakeGame(
            id = "orchard-line",
            title = "ORCHARD LINE",
            thumbnailSeed = 404,
            builds = macOnlyBuilds(42_000_000L),
        ),
        fakeGame(
            id = "glass-circuit",
            title = "GLASS CIRCUIT",
            thumbnailSeed = 505,
            builds = macOnlyBuilds(67_500_000L),
        ),
        // ARM64 only
        fakeGame(
            id = "solo-arm",
            title = "SOLO ARM",
            thumbnailSeed = 506,
            builds = platformBuilds(PlatformKey.MACOS_ARM64),
        ),
        // macOS X64 only
        fakeGame(
            id = "solo-x64",
            title = "SOLO X64",
            thumbnailSeed = 507,
            builds = platformBuilds(PlatformKey.MACOS_X64),
        ),
        // Windows only
        fakeGame(
            id = "iron-ledger",
            title = "IRON LEDGER",
            thumbnailSeed = 808,
            builds = windowsOnlyBuilds(95_000_000L),
        ),
        // Windows + X64 (no ARM)
        fakeGame(
            id = "byte-fortress",
            title = "BYTE FORTRESS",
            thumbnailSeed = 909,
            builds = platformBuilds(PlatformKey.WINDOWS_X64, PlatformKey.MACOS_X64),
        ),
        // No thumbnail — title-card hero fallback
        fakeGame(
            id = "ghost-protocol",
            title = "GHOST PROTOCOL",
            hasThumbnail = false,
            builds = platformBuilds(PlatformKey.MACOS_ARM64, PlatformKey.WINDOWS_X64),
            version = "0.3.1",
        ),
        // Long titles
        fakeGame(
            id = "long-title-alpha",
            title = "THE INCREDIBLY LONG AND UNWIELDY TITLE OF A GAME THAT SHOULD TEST OVERFLOW",
            thumbnailSeed = 111,
            builds = allPlatformBuilds(),
        ),
        fakeGame(
            id = "long-title-beta",
            title = "SUPERCALIFRAGILISTICEXPIALIDOCIOUS ADVENTURE COLLECTORS EDITION REMASTERED",
            thumbnailSeed = 222,
            builds = platformBuilds(PlatformKey.MACOS_ARM64, PlatformKey.WINDOWS_X64),
        ),
        // Broken thumbnail
        fakeGame(
            id = "no-signal",
            title = "NO SIGNAL",
            thumbnailUrl = "https://example.com/dev-mode-missing-thumbnail.webp",
            builds = allPlatformBuilds(),
        ),
        // Zero-byte build — multi-version regression
        fakeGame(
            id = "zero-byte",
            title = "ZERO BYTE",
            thumbnailSeed = 333,
            builds = allPlatformBuilds(fileSizeBytes = 0L),
            version = "2.0.0",
            versionHistory =
                listOf(
                    fakeVersion("1.0.0", "2023-12-01", PlatformKey.MACOS_ARM64),
                    fakeVersion("1.5.0", "2024-09-01", PlatformKey.WINDOWS_X64),
                    fakeVersion(
                        "2.0.0",
                        "2025-04-01",
                        PlatformKey.WINDOWS_X64,
                        PlatformKey.MACOS_ARM64,
                        PlatformKey.MACOS_X64,
                        fileSizeBytes = 0L,
                    ),
                ),
        ),
        // No builds
        fakeGame(
            id = "future-title",
            title = "FUTURE TITLE",
            thumbnailSeed = 444,
            builds = emptyMap(),
            version = "TBA",
        ),
        // Web — platform-agnostic, opens in browser
        fakeGame(
            id = "game-gallery",
            title = "GAME GALLERY",
            thumbnailUrl = "https://morphingcoffee.github.io/images/unsplash/lorenzo-herrera-p0j-mE6mGo4-unsplash.webp",
            builds = webBuild("https://morphingcoffee.github.io/apps/games/"),
            version = "1.0.0",
        ),
    )

private fun fakeGame(
    id: String,
    title: String,
    thumbnailSeed: Int? = null,
    thumbnailUrl: String? = null,
    hasThumbnail: Boolean = true,
    builds: Map<String, GameBuild>,
    version: String = "0.0.1",
    versionHistory: List<GameVersionEntry> = emptyList(),
): GameCatalogEntry =
    GameCatalogEntry(
        id = id,
        title = title,
        description = "Dev catalog entry for $title",
        thumbnailUrl =
            when {
                !hasThumbnail -> null
                thumbnailUrl != null -> thumbnailUrl
                else -> picsumThumbnail(thumbnailSeed ?: id.hashCode())
            },
        latestVersion = version,
        versionsUrl = "https://example.com/dev/$id/versions.json",
        builds = builds,
        versionHistory = versionHistory,
    )

private fun fakeVersion(
    version: String,
    releasedAt: String,
    vararg platformKeys: String,
    fileSizeBytes: Long = 48_000_000L,
): GameVersionEntry =
    GameVersionEntry(
        version = version,
        releasedAt = releasedAt,
        builds = platformBuilds(*platformKeys, fileSizeBytes = fileSizeBytes),
    )

private fun picsumThumbnail(seed: Int): String = "https://picsum.photos/seed/$seed/640/360"

private fun platformBuilds(
    vararg platformKeys: String,
    fileSizeBytes: Long = 48_000_000L,
): Map<String, GameBuild> = platformKeys.associateWith { gameBuild(it, fileSizeBytes) }

private fun allPlatformBuilds(fileSizeBytes: Long = 48_000_000L): Map<String, GameBuild> =
    mapOf(
        PlatformKey.WINDOWS_X64 to gameBuild(PlatformKey.WINDOWS_X64, fileSizeBytes),
        PlatformKey.MACOS_ARM64 to gameBuild(PlatformKey.MACOS_ARM64, fileSizeBytes),
        PlatformKey.MACOS_X64 to gameBuild(PlatformKey.MACOS_X64, fileSizeBytes),
    )

private fun macOnlyBuilds(fileSizeBytes: Long = 48_000_000L): Map<String, GameBuild> =
    mapOf(
        PlatformKey.MACOS_ARM64 to gameBuild(PlatformKey.MACOS_ARM64, fileSizeBytes),
        PlatformKey.MACOS_X64 to gameBuild(PlatformKey.MACOS_X64, fileSizeBytes),
    )

private fun windowsOnlyBuilds(fileSizeBytes: Long = 48_000_000L): Map<String, GameBuild> =
    mapOf(
        PlatformKey.WINDOWS_X64 to gameBuild(PlatformKey.WINDOWS_X64, fileSizeBytes),
    )

private fun webBuild(url: String): Map<String, GameBuild> =
    mapOf(
        PlatformKey.WEB to
            GameBuild(
                downloadUrl = url,
                executablePath = "",
                fileSizeBytes = 0,
                sha256 = "",
            ),
    )

private fun gameBuild(
    platformKey: String,
    fileSizeBytes: Long,
): GameBuild =
    GameBuild(
        downloadUrl = "https://example.com/dev/$platformKey/game.zip",
        executablePath = executablePath(platformKey),
        fileSizeBytes = fileSizeBytes,
        uncompressedSizeBytes = (fileSizeBytes * 108L) / 100L,
        sha256 = "dev-sha256-$platformKey",
    )

private fun executablePath(platformKey: String): String =
    when (platformKey) {
        PlatformKey.WINDOWS_X64 -> "Game.exe"
        else -> "Game.app/Contents/MacOS/Game"
    }
