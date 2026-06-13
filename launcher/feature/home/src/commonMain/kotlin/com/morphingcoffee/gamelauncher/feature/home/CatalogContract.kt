package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.ui.graphics.Color
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry

sealed interface CatalogEvent {
    data object Started : CatalogEvent

    data class GameSelected(
        val gameId: String,
    ) : CatalogEvent

    data class MoveSelection(
        val delta: Int,
    ) : CatalogEvent

    data object LaunchClicked : CatalogEvent

    data object LaunchChargeComplete : CatalogEvent

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
    val statusLabel: String = "READY",
    val downloadProgressFraction: Float? = null,
    val clockText: String = "",
    val platformKey: String? = null,
    val isChargingLaunch: Boolean = false,
    val contentAlpha: Float = 1f,
    val appVersion: String = "0.0.1",
    val ambientColor: Color = Color.Transparent,
) {
    val selectedGame: GameCatalogEntry?
        get() = games.firstOrNull { it.id == selectedGameId }
}

sealed interface CatalogEffect {
    data object RequestFocusRoster : CatalogEffect
}
