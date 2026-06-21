package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.ui.graphics.Color
import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateEvaluation
import com.morphingcoffee.gamelauncher.core.model.LauncherUpdateStatus
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.model.VersionComparator
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

    data object UpdateClicked : CatalogEvent

    data object UpdateChargeComplete : CatalogEvent

    data object LauncherUpdateSignalClicked : CatalogEvent

    data object LauncherUpdateSheetDismissed : CatalogEvent

    data object GetLatestClicked : CatalogEvent

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
    val updateEvaluation: LauncherUpdateEvaluation? = null,
    val isLauncherUpdateSheetVisible: Boolean = false,
    val isUpdateDownloading: Boolean = false,
    val isUpdateCharging: Boolean = false,
    val updateErrorMessage: String? = null,
    val installStatesByGameId: Map<String, InstallState> = emptyMap(),
) {
    val isUpdateGateActive: Boolean
        get() =
            updateEvaluation?.status == LauncherUpdateStatus.UpdateRequired ||
                updateEvaluation?.status == LauncherUpdateStatus.ManualUpdateRequired

    val showOptionalUpdateHint: Boolean
        get() = updateEvaluation?.status == LauncherUpdateStatus.UpdateAvailable

    val showLauncherUpdateSignal: Boolean
        get() = showOptionalUpdateHint && !isUpdateGateActive

    val canTriggerLauncherUpdate: Boolean
        get() =
            when (updateEvaluation?.status) {
                LauncherUpdateStatus.UpdateAvailable,
                LauncherUpdateStatus.UpdateRequired,
                -> updateEvaluation.channelBuild?.downloadUrl?.isNotBlank() == true
                else -> false
            }

    val channelLatestVersion: String?
        get() = updateEvaluation?.channelBuild?.version
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

    val gameUpdateAvailable: Boolean
        get() = selectedGameId?.let(::gameHasUpdate) == true

    fun gameHasUpdate(gameId: String): Boolean {
        val game = games.firstOrNull { it.id == gameId } ?: return false
        if (PlatformKey.WEB in game.builds) return false
        val installed = installStatesByGameId[gameId] as? InstallState.Installed ?: return false
        return VersionComparator.isLessThan(installed.version, game.latestVersion)
    }
}

sealed interface CatalogEffect {
    data object RequestFocusRoster : CatalogEffect

    data class OpenUrl(
        val url: String,
    ) : CatalogEffect
}
