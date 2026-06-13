package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.morphingcoffee.gamelauncher.core.architecture.MviViewModel
import com.morphingcoffee.gamelauncher.core.designsystem.platformClockText
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.network.GameCatalogDataSource
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

            CatalogEvent.LaunchClicked -> {
                val game = state.value.selectedGame ?: return
                if (!game.isAvailableOnCurrentPlatform()) return
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
                    updateState {
                        copy(
                            isLoading = false,
                            games = games,
                            selectedGameId = selectedGameId ?: games.firstOrNull()?.id,
                            statusLabel = "READY",
                            errorMessage = null,
                        )
                    }
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
                ambientColor = Color.Transparent,
                launchErrorMessage = null,
            )
        }
    }

    private fun moveSelection(delta: Int) {
        val games = state.value.games
        if (games.isEmpty()) return

        val currentIndex =
            games.indexOfFirst { it.id == state.value.selectedGameId }.takeIf { it >= 0 } ?: 0
        val nextIndex = (currentIndex + delta).coerceIn(0, games.lastIndex)
        selectGame(games[nextIndex].id)
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
                .launchGame(game)
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
