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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.designsystem.components.AppHeader
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateSheet
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateSheetState
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateSignal
import com.morphingcoffee.gamelauncher.core.designsystem.components.StatusBar
import com.morphingcoffee.gamelauncher.core.designsystem.components.StatusBarAction
import com.morphingcoffee.gamelauncher.core.designsystem.components.VerticalTerminalRule
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.navigation.DebugNavigation
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel = koinViewModel(),
    onOpenAbout: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
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
                is CatalogEffect.OpenUrl -> uriHandler.openUri(effect.url)
            }
        }
    }

    CatalogScreenContent(
        state = state,
        requestRosterFocus = requestRosterFocus,
        onRosterFocusHandled = { requestRosterFocus = false },
        onGameSelected = { viewModel.onEvent(CatalogEvent.GameSelected(it)) },
        onMoveSelection = { viewModel.onEvent(CatalogEvent.MoveSelection(it)) },
        onVersionPickerToggled = { viewModel.onEvent(CatalogEvent.VersionPickerToggled) },
        onVersionSelected = { viewModel.onEvent(CatalogEvent.VersionSelected(it)) },
        onDownloadClicked = { viewModel.onEvent(CatalogEvent.DownloadClicked) },
        onLaunchClicked = { viewModel.onEvent(CatalogEvent.LaunchClicked) },
        onOpenClicked = { viewModel.onEvent(CatalogEvent.OpenClicked) },
        onLaunchChargeComplete = { viewModel.onEvent(CatalogEvent.LaunchChargeComplete) },
        onUninstallClicked = { viewModel.onEvent(CatalogEvent.UninstallClicked) },
        onUninstallChargeComplete = { viewModel.onEvent(CatalogEvent.UninstallChargeComplete) },
        onUpdateClicked = { viewModel.onEvent(CatalogEvent.UpdateClicked) },
        onUpdateChargeComplete = { viewModel.onEvent(CatalogEvent.UpdateChargeComplete) },
        onLauncherUpdateSignalClicked = { viewModel.onEvent(CatalogEvent.LauncherUpdateSignalClicked) },
        onLauncherUpdateSheetDismissed = { viewModel.onEvent(CatalogEvent.LauncherUpdateSheetDismissed) },
        onGetLatestClicked = { viewModel.onEvent(CatalogEvent.GetLatestClicked) },
        onAmbientColorExtracted = { color, imageUrl ->
            viewModel.onEvent(CatalogEvent.AmbientColorExtracted(color, imageUrl))
        },
        onRetryLoad = { viewModel.onEvent(CatalogEvent.RetryLoad) },
        onOpenAbout = onOpenAbout,
        onOpenStorage = onOpenStorage,
    )
}

@Composable
fun CatalogScreenContent(
    state: CatalogState,
    requestRosterFocus: Boolean,
    onRosterFocusHandled: () -> Unit,
    onGameSelected: (String) -> Unit,
    onMoveSelection: (Int) -> Unit,
    onVersionPickerToggled: () -> Unit,
    onVersionSelected: (String) -> Unit,
    onDownloadClicked: () -> Unit,
    onLaunchClicked: () -> Unit,
    onOpenClicked: () -> Unit,
    onLaunchChargeComplete: () -> Unit,
    onUninstallClicked: () -> Unit,
    onUninstallChargeComplete: () -> Unit,
    onUpdateClicked: () -> Unit = {},
    onUpdateChargeComplete: () -> Unit = {},
    onLauncherUpdateSignalClicked: () -> Unit = {},
    onLauncherUpdateSheetDismissed: () -> Unit = {},
    onGetLatestClicked: () -> Unit = {},
    onAmbientColorExtracted: (Color, String?) -> Unit,
    onRetryLoad: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
) {
    val contentAlpha by animateFloatAsState(
        targetValue = state.contentAlpha,
        animationSpec = tween(durationMillis = 200),
        label = "catalog_content_alpha",
    )
    val channelLatestVersion = state.channelLatestVersion

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
                launcherUpdateSlot =
                    if (state.showLauncherUpdateSignal && channelLatestVersion != null) {
                        {
                            LauncherUpdateSignal(
                                currentVersion = state.appVersion,
                                latestVersion = channelLatestVersion,
                                onClick = onLauncherUpdateSignalClicked,
                            )
                        }
                    } else {
                        null
                    },
            )

            Row(modifier = Modifier.weight(1f)) {
                CatalogRoster(
                    games = state.games,
                    selectedGameId = state.selectedGameId,
                    gameIdsWithUpdate =
                        state.games
                            .mapNotNull { game -> game.id.takeIf { state.gameHasUpdate(it) } }
                            .toSet(),
                    onGameSelected = onGameSelected,
                    onMoveSelection = onMoveSelection,
                    requestFocus = requestRosterFocus,
                    onFocusHandled = onRosterFocusHandled,
                )

                VerticalTerminalRule()

                GameDetailPane(
                    games = state.games,
                    selectedGameId = state.selectedGameId,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    launchErrorMessage = state.launchErrorMessage,
                    displayVersion = state.displayVersion,
                    displayBuild = state.displayBuild,
                    versionHistory = state.versionHistory,
                    isVersionPickerVisible = state.isVersionPickerVisible,
                    isVersionHistoryLoading = state.isVersionHistoryLoading,
                    isInstalledForDisplay = state.isInstalledForDisplay,
                    gameUpdateAvailable = state.gameUpdateAvailable,
                    isWebGame = state.isWebGame,
                    isInstallStatePending = state.isInstallStatePending,
                    isDownloading = state.isDownloading,
                    isChargingLaunch = state.isChargingLaunch,
                    canUninstall = state.canUninstall,
                    isChargingUninstall = state.isChargingUninstall,
                    isUninstalling = state.isUninstalling,
                    onDiskSizeBytes = state.onDiskSizeBytes,
                    ambientColor = state.ambientColor,
                    onVersionPickerToggled = onVersionPickerToggled,
                    onVersionSelected = onVersionSelected,
                    onDownloadClicked = onDownloadClicked,
                    onLaunchClicked = onLaunchClicked,
                    onOpenClicked = onOpenClicked,
                    onLaunchChargeComplete = onLaunchChargeComplete,
                    onUninstallClicked = onUninstallClicked,
                    onUninstallChargeComplete = onUninstallChargeComplete,
                    onAmbientColorExtracted = onAmbientColorExtracted,
                    modifier = Modifier.weight(1f),
                )
            }

            StatusBar(
                statusText = state.statusLabel,
                clockText = state.clockText,
                downloadProgress = state.downloadProgressFraction,
                actions =
                    buildList {
                        if (LauncherMetadata.DEBUG_TOOLS_ENABLED && !state.isUpdateGateActive) {
                            add(
                                StatusBarAction(
                                    label = "LOGS",
                                    onClick = { DebugNavigation.requestOpenLogs() },
                                ),
                            )
                        }
                        if (!state.isUpdateGateActive) {
                            add(StatusBarAction(label = "STORAGE", onClick = onOpenStorage))
                            add(StatusBarAction(label = "ABOUT", onClick = onOpenAbout))
                        }
                    },
            )
        }

        UpdateGateOverlay(
            state = state,
            onUpdateClicked = onUpdateClicked,
            onUpdateChargeComplete = onUpdateChargeComplete,
            onGetLatestClicked = onGetLatestClicked,
        )

        LauncherUpdateSheet(
            state = state.toLauncherUpdateSheetState(),
            onDismiss = onLauncherUpdateSheetDismissed,
            onUpdateClicked = onUpdateClicked,
            onUpdateChargeComplete = onUpdateChargeComplete,
            onReleaseNotesClicked = onGetLatestClicked,
        )
    }
}

private fun CatalogState.toLauncherUpdateSheetState(): LauncherUpdateSheetState =
    LauncherUpdateSheetState(
        visible = isLauncherUpdateSheetVisible,
        appVersion = appVersion,
        latestVersion = channelLatestVersion,
        channelKey = updateEvaluation?.channelKey,
        fileSizeBytes = updateEvaluation?.channelBuild?.fileSizeBytes,
        errorMessage = updateErrorMessage,
        isUpdateCharging = isUpdateCharging,
        isUpdateDownloading = isUpdateDownloading,
    )

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
            onVersionPickerToggled = {},
            onVersionSelected = {},
            onDownloadClicked = {},
            onLaunchClicked = {},
            onOpenClicked = {},
            onLaunchChargeComplete = {},
            onUninstallClicked = {},
            onUninstallChargeComplete = {},
            onAmbientColorExtracted = { _, _ -> },
            onRetryLoad = {},
        )
    }
}
