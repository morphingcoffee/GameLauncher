package com.morphingcoffee.gamelauncher.feature.settings

import androidx.lifecycle.viewModelScope
import com.morphingcoffee.gamelauncher.core.architecture.MviViewModel
import com.morphingcoffee.gamelauncher.core.designsystem.components.PieSegment
import com.morphingcoffee.gamelauncher.core.designsystem.components.pieSegmentColor
import com.morphingcoffee.gamelauncher.core.designsystem.platformClockText
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.GameCatalogDataSource
import com.morphingcoffee.gamelauncher.core.network.InstalledGameSummary
import kotlinx.coroutines.launch

class StorageViewModel(
    private val gameCatalogRepository: GameCatalogDataSource,
) : MviViewModel<StorageState, StorageEvent, Unit>(
        initialState =
            StorageState(
                platformLabel = formatStoragePlatformLabel(PlatformKey.current()),
            ),
    ) {
    override fun onEvent(event: StorageEvent) {
        when (event) {
            StorageEvent.Started,
            StorageEvent.Refresh,
            -> refresh()

            StorageEvent.ClockTick -> {
                updateState { copy(clockText = platformClockText()) }
            }

            is StorageEvent.SegmentHovered -> {
                updateState { copy(hoveredSegmentId = event.segmentId) }
            }

            is StorageEvent.SegmentClicked -> {
                val segment = state.value.segments.firstOrNull { it.gameId == event.segmentId } ?: return
                updateState {
                    copy(
                        activeDialog = StorageDialog.GameDetail(segment),
                        errorMessage = null,
                        isChargingUninstall = false,
                    )
                }
            }

            StorageEvent.CenterClicked -> {
                if (state.value.segments.isEmpty()) return
                updateState {
                    copy(
                        activeDialog = StorageDialog.UninstallAll,
                        errorMessage = null,
                        isChargingUninstall = false,
                    )
                }
            }

            StorageEvent.DialogDismissed -> {
                if (state.value.isUninstalling) return
                updateState {
                    copy(
                        activeDialog = null,
                        errorMessage = null,
                        isChargingUninstall = false,
                    )
                }
            }

            StorageEvent.UninstallClicked -> {
                if (!state.value.canUninstall) return
                val dialog = state.value.activeDialog as? StorageDialog.GameDetail ?: return
                AppLog.i("Storage", "Uninstall charge started for ${dialog.segment.gameId}")
                updateState { copy(isChargingUninstall = true, errorMessage = null) }
            }

            StorageEvent.UninstallChargeComplete -> uninstallSelectedGame()

            StorageEvent.UninstallAllClicked -> {
                if (!state.value.canUninstall || state.value.segments.isEmpty()) return
                AppLog.i("Storage", "Uninstall-all charge started")
                updateState { copy(isChargingUninstall = true, errorMessage = null) }
            }

            StorageEvent.UninstallAllChargeComplete -> uninstallAllGames()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }
            refreshInstalledGames()
        }
    }

    private fun applyInstalledGames(
        installed: List<InstalledGameSummary>,
        titles: Map<String, String>,
    ) {
        val totalBytes = installed.sumOf { it.sizeBytes }
        val segments =
            installed.mapIndexed { index, game ->
                val shareFraction =
                    if (totalBytes > 0L) {
                        game.sizeBytes.toFloat() / totalBytes.toFloat()
                    } else {
                        0f
                    }
                val title = titles[game.gameId] ?: game.gameId.uppercase()
                val pieSegment =
                    PieSegment(
                        id = game.gameId,
                        label = title,
                        sizeBytes = game.sizeBytes,
                        shareFraction = shareFraction,
                        color = pieSegmentColor(index),
                    )
                StorageSegmentUi(
                    gameId = game.gameId,
                    title = title,
                    version = game.version,
                    sizeBytes = game.sizeBytes,
                    shareFraction = shareFraction,
                    pieSegment = pieSegment,
                )
            }
        updateState {
            copy(
                isLoading = false,
                segments = segments,
                totalBytes = totalBytes,
                hoveredSegmentId = null,
                activeDialog = null,
                isChargingUninstall = false,
                isUninstalling = false,
            )
        }
    }

    private fun uninstallSelectedGame() {
        val dialog = state.value.activeDialog as? StorageDialog.GameDetail ?: return
        if (state.value.isUninstalling) return

        val gameId = dialog.segment.gameId
        updateState {
            copy(
                isChargingUninstall = false,
                isUninstalling = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            AppLog.i("Storage", "Uninstalling $gameId")
            val result = gameCatalogRepository.uninstallGame(gameId)
            result
                .onSuccess {
                    AppLog.i("Storage", "Uninstall complete for $gameId")
                    refreshInstalledGames()
                }.onFailure { error ->
                    AppLog.e("Storage", "Uninstall failed for $gameId", error)
                    updateState {
                        copy(
                            isUninstalling = false,
                            errorMessage =
                                error.message ?: "Uninstall failed. See F12 logs for details.",
                        )
                    }
                }
        }
    }

    private fun uninstallAllGames() {
        if (state.value.isUninstalling || state.value.segments.isEmpty()) return

        updateState {
            copy(
                isChargingUninstall = false,
                isUninstalling = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            AppLog.i("Storage", "Uninstalling all builds")
            val result = gameCatalogRepository.uninstallAllGames()
            result
                .onSuccess {
                    AppLog.i("Storage", "Uninstall-all complete")
                    refreshInstalledGames()
                }.onFailure { error ->
                    AppLog.e("Storage", "Uninstall-all failed", error)
                    refresh()
                    updateState {
                        copy(
                            isUninstalling = false,
                            errorMessage =
                                error.message ?: "Uninstall failed. See F12 logs for details.",
                        )
                    }
                }
        }
    }

    private suspend fun refreshInstalledGames() {
        val installed = gameCatalogRepository.listInstalledGames()
        val titles =
            gameCatalogRepository
                .loadCatalog()
                .getOrNull()
                ?.associate { it.id to it.title }
                ?: emptyMap()
        applyInstalledGames(installed, titles)
    }
}
