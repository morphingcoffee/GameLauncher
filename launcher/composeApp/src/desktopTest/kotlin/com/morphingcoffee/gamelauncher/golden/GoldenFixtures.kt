package com.morphingcoffee.gamelauncher.golden

import com.morphingcoffee.gamelauncher.core.designsystem.components.PieSegment
import com.morphingcoffee.gamelauncher.core.designsystem.components.pieSegmentColor
import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme
import com.morphingcoffee.gamelauncher.core.model.LauncherChannelBuild
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateEvaluation
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateStatus
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.InstallState
import com.morphingcoffee.gamelauncher.feature.home.CatalogState
import com.morphingcoffee.gamelauncher.feature.settings.AboutState
import com.morphingcoffee.gamelauncher.feature.settings.SettingsLink
import com.morphingcoffee.gamelauncher.feature.settings.StorageDialog
import com.morphingcoffee.gamelauncher.feature.settings.StorageSegmentUi
import com.morphingcoffee.gamelauncher.feature.settings.StorageState

/** Canonical fixture platform — matches CI `macos-15` / [PlatformKey.MACOS_ARM64]. */
internal const val GOLDEN_PLATFORM = PlatformKey.MACOS_ARM64

internal const val GOLDEN_CLOCK = "13:42:01"
internal const val GOLDEN_APP_VERSION = "0.0.2-golden"

internal fun catalogLoadingState(): CatalogState =
    CatalogState(
        isLoading = true,
        clockText = GOLDEN_CLOCK,
        platformKey = GOLDEN_PLATFORM,
        appVersion = GOLDEN_APP_VERSION,
        statusLabel = "LOADING",
    )

internal fun catalogErrorState(): CatalogState =
    CatalogState(
        isLoading = false,
        errorMessage = "Failed to load catalog (fixture).",
        clockText = GOLDEN_CLOCK,
        platformKey = GOLDEN_PLATFORM,
        appVersion = GOLDEN_APP_VERSION,
        statusLabel = "ERROR",
    )

internal fun catalogEmptyState(): CatalogState =
    CatalogState(
        isLoading = false,
        games = emptyList(),
        clockText = GOLDEN_CLOCK,
        platformKey = GOLDEN_PLATFORM,
        appVersion = GOLDEN_APP_VERSION,
        statusLabel = "READY",
    )

internal fun catalogLoadedState(): CatalogState {
    val games =
        listOf(
            goldenGame(id = "alpha", title = "Alpha Build", available = true),
            goldenGame(id = "beta", title = "Beta Showcase", available = false),
            goldenGame(id = "gamma", title = "Gamma Circuit", available = true),
        )
    return CatalogState(
        isLoading = false,
        games = games,
        selectedGameId = "alpha",
        selectedVersion = "1.2.0",
        installState =
            InstallState.Installed(
                version = "1.2.0",
                executablePath = "Game.app/Contents/MacOS/Game",
            ),
        installStatesByGameId =
            mapOf(
                "alpha" to
                    InstallState.Installed(
                        version = "1.2.0",
                        executablePath = "Game.app/Contents/MacOS/Game",
                    ),
            ),
        onDiskSizeBytes = 96_500_000L,
        clockText = GOLDEN_CLOCK,
        platformKey = GOLDEN_PLATFORM,
        appVersion = GOLDEN_APP_VERSION,
        statusLabel = "READY",
        contentAlpha = 1f,
    )
}

internal fun catalogUpdateGateState(): CatalogState =
    catalogLoadedState().copy(
        updateEvaluation =
            LauncherUpdateEvaluation(
                status = LauncherUpdateStatus.UpdateRequired,
                channelKey = "macos-arm64-dmg",
                channelBuild = goldenChannelBuild(),
            ),
        statusLabel = "UPDATE REQUIRED",
    )

internal fun catalogLauncherUpdateSheetState(): CatalogState =
    catalogLoadedState().copy(
        updateEvaluation =
            LauncherUpdateEvaluation(
                status = LauncherUpdateStatus.UpdateAvailable,
                channelKey = "macos-arm64-dmg",
                channelBuild = goldenChannelBuild(),
            ),
        isLauncherUpdateSheetVisible = true,
    )

internal fun aboutDefaultState(): AboutState =
    AboutState(
        appVersion = GOLDEN_APP_VERSION,
        platformLabel = GOLDEN_PLATFORM,
        clockText = GOLDEN_CLOCK,
        links =
            listOf(
                SettingsLink(
                    label = "SITE",
                    displayText = "morphingcoffee.com",
                    url = "https://morphingcoffee.com",
                ),
            ),
        releasesUrl = "https://github.com/morphingcoffee/GameLauncher/releases",
        sendCrashReports = true,
        shareExtendedDiagnostics = false,
        isDevBuild = false,
        backgroundTheme = LauncherBackgroundTheme.DEFAULT,
        backgroundThemes = LauncherBackgroundTheme.entries,
    )

internal fun aboutUpdateSheetState(): AboutState =
    aboutDefaultState().copy(
        updateEvaluation =
            LauncherUpdateEvaluation(
                status = LauncherUpdateStatus.UpdateAvailable,
                channelKey = "macos-arm64-dmg",
                channelBuild = goldenChannelBuild(),
            ),
        isLauncherUpdateSheetVisible = true,
    )

internal fun storageLoadingState(): StorageState =
    StorageState(
        isLoading = true,
        appVersion = GOLDEN_APP_VERSION,
        platformLabel = GOLDEN_PLATFORM,
        clockText = GOLDEN_CLOCK,
    )

internal fun storageLoadedState(): StorageState {
    val segments = goldenStorageSegments()
    return StorageState(
        isLoading = false,
        segments = segments,
        totalBytes = segments.sumOf { it.sizeBytes },
        appVersion = GOLDEN_APP_VERSION,
        platformLabel = GOLDEN_PLATFORM,
        clockText = GOLDEN_CLOCK,
    )
}

internal fun storageUninstallAllDialogState(): StorageState =
    storageLoadedState().copy(activeDialog = StorageDialog.UninstallAll)

private fun goldenChannelBuild(): LauncherChannelBuild =
    LauncherChannelBuild(
        version = "99999.0.0-build99999",
        artifactType = "dmg",
        downloadUrl = "https://cdn.example/launcher.dmg",
        fileSizeBytes = 48_000_000L,
        sha256 = "abc123",
    )

private fun goldenGame(
    id: String,
    title: String,
    available: Boolean,
): GameCatalogEntry =
    GameCatalogEntry(
        id = id,
        title = title,
        description = "Golden fixture entry — no network.",
        thumbnailUrl = null,
        latestVersion = "1.2.0",
        versionsUrl = "https://cdn.example/games/$id/versions.json",
        builds =
            if (available) {
                mapOf(
                    GOLDEN_PLATFORM to
                        GameBuild(
                            downloadUrl = "https://cdn.example/games/$id.zip",
                            executablePath = "Game.app/Contents/MacOS/Game",
                            fileSizeBytes = 48_234_567L,
                            uncompressedSizeBytes = 96_500_000L,
                            sha256 = "abc123",
                        ),
                )
            } else {
                mapOf(
                    PlatformKey.WINDOWS_X64 to
                        GameBuild(
                            downloadUrl = "https://cdn.example/games/$id.zip",
                            executablePath = "Game.exe",
                            fileSizeBytes = 48_234_567L,
                            uncompressedSizeBytes = 96_500_000L,
                            sha256 = "abc123",
                        ),
                )
            },
    )

private fun goldenStorageSegments(): List<StorageSegmentUi> {
    val specs =
        listOf(
            SegmentSpec("void-runner", "VOID RUNNER", "1.4.2", 669_600_000L, 0.57f, 0),
            SegmentSpec("neon-drift", "NEON DRIFT", "0.9.0", 410_400_000L, 0.35f, 1),
            SegmentSpec("solo-arm", "SOLO ARM", "0.0.1", 49_000_000L, 0.04f, 2),
            SegmentSpec("glass-circuit", "GLASS CIRCUIT", "0.0.1", 70_000_000L, 0.06f, 3),
        )
    return specs.map { spec ->
        val pie =
            PieSegment(
                id = spec.gameId,
                label = spec.title,
                sizeBytes = spec.sizeBytes,
                shareFraction = spec.shareFraction,
                color = pieSegmentColor(spec.colorIndex),
            )
        StorageSegmentUi(
            gameId = spec.gameId,
            title = spec.title,
            version = spec.version,
            sizeBytes = spec.sizeBytes,
            shareFraction = spec.shareFraction,
            pieSegment = pie,
        )
    }
}

private data class SegmentSpec(
    val gameId: String,
    val title: String,
    val version: String,
    val sizeBytes: Long,
    val shareFraction: Float,
    val colorIndex: Int,
)
