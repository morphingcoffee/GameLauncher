package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.ui.graphics.Color
import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.InstallState

sealed interface CatalogEvent {
    data object Started : CatalogEvent

    data class GameSelected(
        val gameId: String,
    ) : CatalogEvent

    data class MoveSelection(
        val delta: Int,
    ) : CatalogEvent

    data object VersionPickerToggled : CatalogEvent

    data class VersionSelected(
        val version: String,
    ) : CatalogEvent

    data object DownloadClicked : CatalogEvent

    data object LaunchClicked : CatalogEvent

    data object LaunchChargeComplete : CatalogEvent

    data object UninstallClicked : CatalogEvent

    data object UninstallChargeComplete : CatalogEvent

    data object OpenClicked : CatalogEvent

    data object RetryLoad : CatalogEvent

    data object ClockTick : CatalogEvent

    data class AmbientColorExtracted(
        val color: Color,
        val imageUrl: String?,
    ) : CatalogEvent
}

data class CatalogState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val games: List<GameCatalogEntry> = emptyList(),
    val selectedGameId: String? = null,
    val selectedVersion: String? = null,
    val versionHistory: List<GameVersionEntry> = emptyList(),
    val isVersionPickerVisible: Boolean = false,
    val isVersionHistoryLoading: Boolean = false,
    val installState: InstallState = InstallState.Unknown,
    val isDownloading: Boolean = false,
    val statusLabel: String = "READY",
    val downloadProgressFraction: Float? = null,
    val clockText: String = "",
    val platformKey: String? = null,
    val isChargingLaunch: Boolean = false,
    val isLaunching: Boolean = false,
    val isChargingUninstall: Boolean = false,
    val isUninstalling: Boolean = false,
    val onDiskSizeBytes: Long? = null,
    val launchErrorMessage: String? = null,
    val contentAlpha: Float = 1f,
    val appVersion: String = LauncherMetadata.VERSION,
    val ambientColor: Color = Color.Transparent,
) {
    val selectedGame: GameCatalogEntry?
        get() = games.firstOrNull { it.id == selectedGameId }

    val displayVersion: String
        get() = selectedVersion ?: selectedGame?.latestVersion ?: ""

    val displayBuild: GameBuild?
        get() {
            val game = selectedGame ?: return null
            val version = displayVersion
            versionHistory
                .firstOrNull { it.version == version }
                ?.buildForCurrentPlatform()
                ?.let { return it }

            return if (version == game.latestVersion) {
                game.buildForCurrentPlatform()
            } else {
                null
            }
        }

    val isInstalledForDisplay: Boolean
        get() {
            val installed = installState as? InstallState.Installed ?: return false
            return installed.version == displayVersion
        }

    val isInstallStatePending: Boolean
        get() = installState is InstallState.Unknown && !isWebGame

    val isWebGame: Boolean
        get() {
            val game = selectedGame ?: return false
            val version = displayVersion
            val builds =
                versionHistory
                    .firstOrNull { it.version == version }
                    ?.builds
                    ?: game.builds
            return PlatformKey.WEB in builds
        }

    val canUninstall: Boolean
        get() =
            !isWebGame &&
                isInstalledForDisplay &&
                !isInstallStatePending &&
                !isDownloading &&
                !isLaunching &&
                !isChargingLaunch &&
                !isUninstalling &&
                !isChargingUninstall
}

sealed interface CatalogEffect {
    data object RequestFocusRoster : CatalogEffect
}
