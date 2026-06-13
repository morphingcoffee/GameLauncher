package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.designsystem.components.AppHeader
import com.morphingcoffee.gamelauncher.core.designsystem.components.StatusBar
import com.morphingcoffee.gamelauncher.core.designsystem.components.VerticalTerminalRule
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CatalogScreen(viewModel: CatalogViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var requestRosterFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(CatalogEvent.Started)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            viewModel.onEvent(CatalogEvent.ClockTick)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CatalogEffect.RequestFocusRoster -> requestRosterFocus = true
            }
        }
    }

    CatalogScreenContent(
        state = state,
        requestRosterFocus = requestRosterFocus,
        onRosterFocusHandled = { requestRosterFocus = false },
        onGameSelected = { viewModel.onEvent(CatalogEvent.GameSelected(it)) },
        onMoveSelection = { viewModel.onEvent(CatalogEvent.MoveSelection(it)) },
        onLaunchClicked = { viewModel.onEvent(CatalogEvent.LaunchClicked) },
        onLaunchChargeComplete = { viewModel.onEvent(CatalogEvent.LaunchChargeComplete) },
        onAmbientColorExtracted = { color, imageUrl ->
            viewModel.onEvent(CatalogEvent.AmbientColorExtracted(color, imageUrl))
        },
        onRetryLoad = { viewModel.onEvent(CatalogEvent.RetryLoad) },
    )
}

@Composable
fun CatalogScreenContent(
    state: CatalogState,
    requestRosterFocus: Boolean,
    onRosterFocusHandled: () -> Unit,
    onGameSelected: (String) -> Unit,
    onMoveSelection: (Int) -> Unit,
    onLaunchClicked: () -> Unit,
    onLaunchChargeComplete: () -> Unit,
    onAmbientColorExtracted: (Color, String?) -> Unit,
    onRetryLoad: () -> Unit,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = state.contentAlpha,
        animationSpec = tween(durationMillis = 200),
        label = "catalog_content_alpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ShaderBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(contentAlpha),
        ) {
            AppHeader(
                appVersion = state.appVersion,
                platformLabel = formatPlatformLabel(state.platformKey),
            )

            Row(modifier = Modifier.weight(1f)) {
                CatalogRoster(
                    games = state.games,
                    selectedGameId = state.selectedGameId,
                    onGameSelected = onGameSelected,
                    onMoveSelection = onMoveSelection,
                    requestFocus = requestRosterFocus,
                    onFocusHandled = onRosterFocusHandled,
                )

                VerticalTerminalRule()

                GameDetailPane(
                    game = state.selectedGame,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    launchErrorMessage = state.launchErrorMessage,
                    isChargingLaunch = state.isChargingLaunch,
                    ambientColor = state.ambientColor,
                    onLaunchClicked = onLaunchClicked,
                    onLaunchChargeComplete = onLaunchChargeComplete,
                    onAmbientColorExtracted = onAmbientColorExtracted,
                    modifier = Modifier.weight(1f),
                )
            }

            StatusBar(
                statusText = state.statusLabel,
                clockText = state.clockText,
                downloadProgress = state.downloadProgressFraction,
            )
        }
    }
}

@Preview(
    name = "Catalog — mainframe",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun CatalogScreenPreview() {
    LauncherTheme {
        CatalogScreenContent(
            state = catalogPreviewState(),
            requestRosterFocus = false,
            onRosterFocusHandled = {},
            onGameSelected = {},
            onMoveSelection = {},
            onLaunchClicked = {},
            onLaunchChargeComplete = {},
            onAmbientColorExtracted = { _, _ -> },
            onRetryLoad = {},
        )
    }
}
