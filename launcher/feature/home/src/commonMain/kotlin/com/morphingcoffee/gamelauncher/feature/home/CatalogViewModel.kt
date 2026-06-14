package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.morphingcoffee.gamelauncher.core.architecture.MviViewModel
import com.morphingcoffee.gamelauncher.core.designsystem.platformClockText
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.GameCatalogDataSource
import com.morphingcoffee.gamelauncher.core.network.InstallState
import com.morphingcoffee.gamelauncher.core.network.SimulatedLaunchException
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val gameCatalogRepository: GameCatalogDataSource,
) : MviViewModel<CatalogState, CatalogEvent, CatalogEffect>(
        initialState =
            CatalogState(
                platformKey = PlatformKey.current(),
            ),
    ) {
    init {
        gameCatalogRepository.downloadProgress
            .onEach { progress ->
                updateState {
                    if (progress == null) {
                        copy(
                            statusLabel =
                                if (isChargingLaunch || isLaunching) {
                                    statusLabel
                                } else {
                                    "READY"
                                },
                            downloadProgressFraction = null,
                        )
                    } else {
                        copy(
                            statusLabel = "DOWNLOADING",
                            downloadProgressFraction = progress.fraction,
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: CatalogEvent) {
        when (event) {
            CatalogEvent.Started -> {
                updateState { copy(clockText = platformClockText()) }
                loadCatalog()
                sendEffect(CatalogEffect.RequestFocusRoster)
            }

            CatalogEvent.RetryLoad -> loadCatalog()

            CatalogEvent.ClockTick -> {
                updateState { copy(clockText = platformClockText()) }
            }

            is CatalogEvent.GameSelected -> {
                if (event.gameId == state.value.selectedGameId) return
                selectGame(event.gameId)
            }

            is CatalogEvent.MoveSelection -> moveSelection(event.delta)

            is CatalogEvent.AmbientColorExtracted -> {
                val selected = state.value.selectedGame ?: return
                if (event.imageUrl != selected.thumbnailUrl) return
                updateState { copy(ambientColor = event.color) }
            }

            CatalogEvent.VersionPickerToggled -> toggleVersionPicker()

            is CatalogEvent.VersionSelected -> selectVersion(event.version)

            CatalogEvent.DownloadClicked -> downloadSelectedVersion()

            CatalogEvent.LaunchClicked -> {
                val game = state.value.selectedGame ?: return
                if (!game.isAvailableOnCurrentPlatform()) return
                if (!state.value.isInstalledForDisplay) return
                updateState { copy(isChargingLaunch = true, launchErrorMessage = null) }
            }

            CatalogEvent.LaunchChargeComplete -> launchSelectedGame()
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            updateState {
                copy(
                    isLoading = true,
                    errorMessage = null,
                    statusLabel = "LOADING",
                )
            }

            gameCatalogRepository
                .loadCatalog()
                .onSuccess { games ->
                    val selectedGameId = state.value.selectedGameId ?: games.firstOrNull()?.id
                    updateState {
                        copy(
                            isLoading = false,
                            games = games,
                            selectedGameId = selectedGameId,
                            statusLabel = "READY",
                            errorMessage = null,
                        )
                    }
                    selectedGameId?.let { probeInstallState(it) }
                }.onFailure { error ->
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load catalog",
                            statusLabel = "ERROR",
                        )
                    }
                }
        }
    }

    private fun selectGame(gameId: String) {
        updateState {
            copy(
                selectedGameId = gameId,
                selectedVersion = null,
                versionHistory = emptyList(),
                isVersionPickerVisible = false,
                isVersionHistoryLoading = false,
                installState = InstallState.Unknown,
                ambientColor = Color.Transparent,
                launchErrorMessage = null,
            )
        }
        probeInstallState(gameId)
    }

    private fun moveSelection(delta: Int) {
        val games = state.value.games
        if (games.isEmpty()) return

        val currentIndex =
            games.indexOfFirst { it.id == state.value.selectedGameId }.takeIf { it >= 0 } ?: 0
        val nextIndex = (currentIndex + delta).coerceIn(0, games.lastIndex)
        selectGame(games[nextIndex].id)
    }

    private fun toggleVersionPicker() {
        val game = state.value.selectedGame ?: return
        val willExpand = !state.value.isVersionPickerVisible

        updateState {
            copy(isVersionPickerVisible = willExpand)
        }

        if (willExpand && state.value.versionHistory.isEmpty()) {
            loadVersionHistory(game.versionsUrl, game.versionHistory)
        }
    }

    private fun loadVersionHistory(
        versionsUrl: String,
        embeddedHistory: List<com.morphingcoffee.gamelauncher.core.model.GameVersionEntry>,
    ) {
        if (embeddedHistory.isNotEmpty()) {
            updateState { copy(versionHistory = embeddedHistory) }
            return
        }

        viewModelScope.launch {
            updateState { copy(isVersionHistoryLoading = true) }

            gameCatalogRepository
                .fetchVersionHistory(versionsUrl)
                .onSuccess { versions ->
                    updateState {
                        copy(
                            versionHistory = versions,
                            isVersionHistoryLoading = false,
                        )
                    }
                }.onFailure { error ->
                    updateState {
                        copy(
                            isVersionHistoryLoading = false,
                            launchErrorMessage = error.message,
                            statusLabel = "ERROR",
                        )
                    }
                }
        }
    }

    private fun selectVersion(version: String) {
        updateState {
            copy(
                selectedVersion = version,
                launchErrorMessage = null,
            )
        }
        state.value.selectedGameId?.let { probeInstallState(it) }
    }

    private fun probeInstallState(gameId: String) {
        viewModelScope.launch {
            val installState = gameCatalogRepository.getInstallState(gameId)
            updateState { copy(installState = installState) }
        }
    }

    private fun downloadSelectedVersion() {
        val game = state.value.selectedGame ?: return
        val build = state.value.displayBuild ?: return
        val version = state.value.displayVersion
        if (version.isBlank()) return

        viewModelScope.launch {
            updateState {
                copy(
                    launchErrorMessage = null,
                    statusLabel = "DOWNLOADING",
                )
            }

            gameCatalogRepository
                .downloadAndInstall(
                    gameId = game.id,
                    version = version,
                    build = build,
                ).onSuccess {
                    probeInstallState(game.id)
                    updateState {
                        copy(
                            statusLabel = "READY",
                            launchErrorMessage = null,
                        )
                    }
                }.onFailure { error ->
                    updateState {
                        copy(
                            statusLabel = "ERROR",
                            launchErrorMessage = error.message,
                        )
                    }
                }
        }
    }

    private fun launchSelectedGame() {
        val game = state.value.selectedGame ?: return
        viewModelScope.launch {
            updateState {
                copy(
                    statusLabel = "LAUNCHING",
                    isChargingLaunch = false,
                    isLaunching = true,
                    launchErrorMessage = null,
                )
            }

            gameCatalogRepository
                .launchGame(game.id)
                .onSuccess {
                    updateState { copy(contentAlpha = 0f, isLaunching = false) }
                }.onFailure { error ->
                    if (error is SimulatedLaunchException) {
                        updateState {
                            copy(
                                statusLabel = "LAUNCHED (DEV)",
                                launchErrorMessage = null,
                                isChargingLaunch = false,
                                isLaunching = false,
                                contentAlpha = 1f,
                            )
                        }
                        return@launch
                    }

                    updateState {
                        copy(
                            statusLabel = "ERROR",
                            launchErrorMessage = error.message,
                            isChargingLaunch = false,
                            isLaunching = false,
                            contentAlpha = 1f,
                        )
                    }
                }
        }
    }
}
