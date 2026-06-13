package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.TerminalRule
import com.morphingcoffee.gamelauncher.core.designsystem.components.RuleText
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.PlatformKey

@Composable
internal fun CatalogRoster(
    games: List<GameCatalogEntry>,
    selectedGameId: String?,
    onGameSelected: (String) -> Unit,
    onMoveSelection: (Int) -> Unit,
    requestFocus: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = FocusRequester()

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier =
            modifier
                .width(LauncherSpacing.RosterWidth)
                .fillMaxHeight()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp, Key.W -> {
                            onMoveSelection(-1)
                            true
                        }
                        Key.DirectionDown, Key.S -> {
                            onMoveSelection(1)
                            true
                        }
                        else -> false
                    }
                },
    ) {
        RuleText(
            text = "◉  CATALOG",
            modifier = Modifier.padding(horizontal = LauncherSpacing.Md, vertical = LauncherSpacing.Sm),
        )
        TerminalRule()
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(games, key = { _, game -> game.id }) { index, game ->
                RosterItem(
                    index = index + 1,
                    title = game.title,
                    isSelected = game.id == selectedGameId,
                    isAvailable = game.isAvailableOnCurrentPlatform(),
                    availablePlatformKeys = game.builds.keys,
                    currentPlatformKey = PlatformKey.current(),
                    onClick = { onGameSelected(game.id) },
                )
            }
        }
    }
}
