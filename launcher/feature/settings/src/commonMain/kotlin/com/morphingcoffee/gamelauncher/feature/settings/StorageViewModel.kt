package com.morphingcoffee.gamelauncher.feature.settings

import androidx.lifecycle.viewModelScope
import com.morphingcoffee.gamelauncher.core.architecture.MviViewModel
import com.morphingcoffee.gamelauncher.core.designsystem.components.PieSegment
import com.morphingcoffee.gamelauncher.core.designsystem.components.StorageChartAnimation
import com.morphingcoffee.gamelauncher.core.designsystem.components.pieSegmentColor
import com.morphingcoffee.gamelauncher.core.designsystem.platformClockText
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.GameCatalogDataSource
import com.morphingcoffee.gamelauncher.core.network.InstalledGameSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StorageViewModel(
    private val gameCatalogRepository: GameCatalogDataSource,
) : MviViewModel<StorageState, StorageEvent, Unit>(
        initialState =
            StorageState(
                platformLabel = formatStoragePlatformLabel(PlatformKey.current()),
            ),
    ) {
    private var refreshJob: Job? = null
    private var loadEpoch = 0

    override fun onEvent(event: StorageEvent) {
        when (event) {
            StorageEvent.Started -> {
                updateState {
                    copy(
                        activeDialog = null,
                        hoveredSegmentId = null,
                        chartAnimation = null,
                        breakdownSegments = null,
                        centerDisplayTotalBytes = null,
                        centerDisplayInstallCount = null,
                        pendingUninstallAll = false,
                        pendingUninstallGameId = null,
                    )
                }
                refresh()
            }

            StorageEvent.Refresh -> refresh(clearErrorMessage = true)

            StorageEvent.ClockTick -> {
                updateState { copy(clockText = platformClockText()) }
            }

            is StorageEvent.SegmentHovered -> {
                if (state.value.chartAnimation != null) return
                updateState { copy(hoveredSegmentId = event.segmentId) }
            }

            is StorageEvent.SegmentClicked -> {
                if (state.value.chartAnimation != null) return
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
                if (state.value.segments.isEmpty() || state.value.chartAnimation != null) return
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

            StorageEvent.UninstallChargeComplete -> beginSegmentUninstall()

            StorageEvent.UninstallAllClicked -> {
                if (!state.value.canUninstall || state.value.segments.isEmpty()) return
                AppLog.i("Storage", "Uninstall-all charge started")
                updateState { copy(isChargingUninstall = true, errorMessage = null) }
            }

            StorageEvent.UninstallAllChargeComplete -> beginUninstallAll()

            StorageEvent.ChartAnimationFinished -> completeChartAnimation()

            StorageEvent.ScreenHidden -> {
                updateState {
                    copy(
                        activeDialog = null,
                        hoveredSegmentId = null,
                        chartAnimation = null,
                        breakdownSegments = null,
                        centerDisplayTotalBytes = null,
                        centerDisplayInstallCount = null,
                        isChargingUninstall = false,
                        pendingUninstallAll = false,
                        pendingUninstallGameId = null,
                    )
                }
            }
        }
    }

    private fun refresh(clearErrorMessage: Boolean = true) {
        val epoch = ++loadEpoch
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                if (state.value.isUninstalling) return@launch
                updateState {
                    copy(
                        isLoading = true,
                        errorMessage = if (clearErrorMessage) null else errorMessage,
                    )
                }
                refreshInstalledGames(epoch = epoch)
            }
    }

    private fun applyInstalledGames(
        installed: List<InstalledGameSummary>,
        titles: Map<String, String>,
    ) {
        val colorByGameId = state.value.segments.associate { it.gameId to it.pieSegment.color }
        val segments = buildSegmentUiList(installed, titles, colorByGameId)
        val totalBytes = installed.sumOf { it.sizeBytes }
        updateState {
            copy(
                isLoading = false,
                segments = segments,
                totalBytes = totalBytes,
                breakdownSegments = null,
                centerDisplayTotalBytes = null,
                centerDisplayInstallCount = null,
                hoveredSegmentId = null,
                isChargingUninstall = false,
                isUninstalling = false,
                chartAnimation = null,
                pendingUninstallAll = false,
                pendingUninstallGameId = null,
            )
        }
    }

    private fun beginSegmentReflowAfterBurst(removedSegmentId: String) {
        val snapshot = state.value
        val fromUiSegments = snapshot.segments
        val fromSegments = fromUiSegments.map { it.pieSegment }
        val fromTotalBytes = snapshot.totalBytes
        val predictedUi = predictSegmentsAfterRemoval(fromUiSegments, removedSegmentId)
        val predictedPie = predictedUi.map { it.pieSegment }

        updateState {
            copy(
                segments = predictedUi,
                totalBytes = predictedUi.sumOf { it.sizeBytes },
                breakdownSegments = fromUiSegments,
                centerDisplayTotalBytes = fromTotalBytes,
                centerDisplayInstallCount = fromUiSegments.size,
                isChargingUninstall = false,
                isUninstalling = true,
                hoveredSegmentId = null,
                pendingUninstallAll = false,
                pendingUninstallGameId = removedSegmentId,
                chartAnimation =
                    StorageChartAnimation.SegmentReflow(
                        removedSegmentId = removedSegmentId,
                        fromSegments = fromSegments,
                        toSegments = predictedPie,
                    ),
            )
        }
        confirmUninstall(removedSegmentId)
    }

    private fun confirmUninstall(gameId: String) {
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                AppLog.i("Storage", "Uninstalling $gameId")
                val result = gameCatalogRepository.uninstallGame(gameId)
                result
                    .onSuccess {
                        AppLog.i("Storage", "Uninstall complete for $gameId")
                        val installed = gameCatalogRepository.listInstalledGames()
                        if (installed.isEmpty()) {
                            updateState {
                                copy(
                                    chartAnimation = null,
                                    breakdownSegments = null,
                                    centerDisplayTotalBytes = null,
                                    centerDisplayInstallCount = null,
                                    isUninstalling = false,
                                    pendingUninstallGameId = null,
                                )
                            }
                            val titles =
                                gameCatalogRepository
                                    .loadCatalog()
                                    .getOrNull()
                                    ?.associate { it.id to it.title }
                                    ?: emptyMap()
                            applyInstalledGames(installed, titles)
                        } else if (state.value.chartAnimation is StorageChartAnimation.SegmentReflow) {
                            updateState {
                                copy(
                                    isUninstalling = false,
                                    pendingUninstallGameId = null,
                                )
                            }
                        } else {
                            val titles =
                                gameCatalogRepository
                                    .loadCatalog()
                                    .getOrNull()
                                    ?.associate { it.id to it.title }
                                    ?: emptyMap()
                            applyInstalledGames(installed, titles)
                        }
                    }.onFailure { error ->
                        AppLog.e("Storage", "Uninstall failed for $gameId", error)
                        updateState {
                            copy(
                                isUninstalling = false,
                                chartAnimation = null,
                                breakdownSegments = null,
                                centerDisplayTotalBytes = null,
                                centerDisplayInstallCount = null,
                                pendingUninstallGameId = null,
                                errorMessage =
                                    error.message ?: "Uninstall failed. See F12 logs for details.",
                            )
                        }
                        refresh(clearErrorMessage = false)
                    }
            }
    }

    private fun buildSegmentUiList(
        installed: List<InstalledGameSummary>,
        titles: Map<String, String>,
        colorByGameId: Map<String, androidx.compose.ui.graphics.Color>,
    ): List<StorageSegmentUi> {
        val totalBytes = installed.sumOf { it.sizeBytes }
        return installed.mapIndexed { index, game ->
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
                    color = colorByGameId[game.gameId] ?: pieSegmentColor(index),
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
    }

    private fun predictSegmentsAfterRemoval(
        segments: List<StorageSegmentUi>,
        removedSegmentId: String,
    ): List<StorageSegmentUi> {
        val remaining = segments.filter { it.gameId != removedSegmentId }
        val totalBytes = remaining.sumOf { it.sizeBytes }
        return remaining.map { segment ->
            val shareFraction =
                if (totalBytes > 0L) {
                    segment.sizeBytes.toFloat() / totalBytes.toFloat()
                } else {
                    0f
                }
            segment.copy(
                shareFraction = shareFraction,
                pieSegment = segment.pieSegment.copy(shareFraction = shareFraction),
            )
        }
    }

    private fun beginSegmentUninstall() {
        val dialog = state.value.activeDialog as? StorageDialog.GameDetail ?: return
        if (state.value.isUninstalling) return

        loadEpoch++
        refreshJob?.cancel()
        val gameId = dialog.segment.gameId
        updateState {
            copy(
                activeDialog = null,
                isChargingUninstall = false,
                isUninstalling = true,
                errorMessage = null,
                hoveredSegmentId = null,
                pendingUninstallAll = false,
                pendingUninstallGameId = gameId,
                chartAnimation = StorageChartAnimation.SegmentBurst(gameId),
            )
        }
    }

    private fun beginUninstallAll() {
        if (state.value.isUninstalling) return

        loadEpoch++
        refreshJob?.cancel()
        updateState {
            copy(
                activeDialog = null,
                isChargingUninstall = false,
                isUninstalling = true,
                errorMessage = null,
                hoveredSegmentId = null,
                pendingUninstallAll = true,
                pendingUninstallGameId = null,
                chartAnimation = StorageChartAnimation.Vortex,
            )
        }
    }

    private fun completeChartAnimation() {
        val snapshot = state.value
        val animation = snapshot.chartAnimation
        when {
            animation is StorageChartAnimation.SegmentReflow -> {
                updateState {
                    copy(
                        chartAnimation = null,
                        breakdownSegments = null,
                        centerDisplayTotalBytes = null,
                        centerDisplayInstallCount = null,
                        isUninstalling = false,
                        pendingUninstallGameId = null,
                    )
                }
            }
            animation is StorageChartAnimation.SegmentBurst -> {
                beginSegmentReflowAfterBurst(animation.segmentId)
            }
            animation == StorageChartAnimation.Vortex || snapshot.pendingUninstallAll -> {
                updateState {
                    copy(pendingUninstallAll = false, pendingUninstallGameId = null)
                }
                performUninstallAll()
            }
            snapshot.pendingUninstallGameId != null -> {
                updateState { copy(pendingUninstallGameId = null) }
            }
        }
    }

    private fun performUninstallAll() {
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                AppLog.i("Storage", "Uninstalling all builds")
                val result = gameCatalogRepository.uninstallAllGames()
                result
                    .onSuccess {
                        AppLog.i("Storage", "Uninstall-all complete")
                        refreshInstalledGames(epoch = loadEpoch, force = true)
                    }.onFailure { error ->
                        AppLog.e("Storage", "Uninstall-all failed", error)
                        updateState {
                            copy(
                                isUninstalling = false,
                                chartAnimation = null,
                                errorMessage =
                                    error.message ?: "Uninstall failed. See F12 logs for details.",
                            )
                        }
                        refresh(clearErrorMessage = false)
                    }
            }
    }

    private suspend fun refreshInstalledGames(
        epoch: Int = loadEpoch,
        force: Boolean = false,
    ) {
        if (!force && state.value.isUninstalling && state.value.chartAnimation != null) return
        val installed = gameCatalogRepository.listInstalledGames()
        val titles =
            gameCatalogRepository
                .loadCatalog()
                .getOrNull()
                ?.associate { it.id to it.title }
                ?: emptyMap()
        if (epoch != loadEpoch) return
        if (!force && state.value.isUninstalling) return
        applyInstalledGames(installed, titles)
    }
}
