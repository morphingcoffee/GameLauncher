package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.TerminalRule
import com.morphingcoffee.gamelauncher.core.designsystem.components.DisplayTitle
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel
import com.morphingcoffee.gamelauncher.core.designsystem.components.PlatformUnavailableBadge
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalButton
import com.morphingcoffee.gamelauncher.core.designsystem.components.ThumbnailImage
import com.morphingcoffee.gamelauncher.core.designsystem.components.VersionSelector
import com.morphingcoffee.gamelauncher.core.designsystem.components.ambientGlow
import com.morphingcoffee.gamelauncher.core.model.GameBuild
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry
import com.morphingcoffee.gamelauncher.core.model.PlatformKey

@Composable
internal fun GameDetailPane(
    games: List<GameCatalogEntry>,
    selectedGameId: String?,
    isLoading: Boolean,
    errorMessage: String?,
    launchErrorMessage: String?,
    displayVersion: String,
    displayBuild: GameBuild?,
    versionHistory: List<GameVersionEntry>,
    isVersionPickerVisible: Boolean,
    isVersionHistoryLoading: Boolean,
    isInstalledForDisplay: Boolean,
    gameUpdateAvailable: Boolean,
    isWebGame: Boolean,
    isInstallStatePending: Boolean,
    isDownloading: Boolean,
    isChargingLaunch: Boolean,
    canUninstall: Boolean,
    isChargingUninstall: Boolean,
    isUninstalling: Boolean,
    onDiskSizeBytes: Long?,
    ambientColor: Color,
    onVersionPickerToggled: () -> Unit,
    onVersionSelected: (String) -> Unit,
    onDownloadClicked: () -> Unit,
    onLaunchClicked: () -> Unit,
    onOpenClicked: () -> Unit,
    onLaunchChargeComplete: () -> Unit,
    onUninstallClicked: () -> Unit,
    onUninstallChargeComplete: () -> Unit,
    onAmbientColorExtracted: (Color, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.matchParentSize().ambientGlow(ambientColor))

        AnimatedContent(
            targetState = selectedGameId ?: "empty",
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (
                    fadeIn(tween(150, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(150, easing = FastOutSlowInEasing)) { it }
                ).togetherWith(
                    fadeOut(tween(150)) +
                        slideOutHorizontally(tween(150)) { -it / 4 },
                )
            },
            label = "game_detail_pane",
        ) { animatedGameId ->
            val animatedGame = games.firstOrNull { it.id == animatedGameId }
            val isActiveSelection = animatedGameId == selectedGameId

            when {
                isLoading -> {
                    DetailMessage(text = "LOADING CATALOG...")
                }

                errorMessage != null && games.isEmpty() -> {
                    DetailMessage(text = errorMessage.uppercase())
                }

                animatedGame == null -> {
                    DetailMessage(text = "NO ENTRIES")
                }

                else -> {
                    GameDetailContent(
                        game = animatedGame,
                        displayVersion =
                            if (isActiveSelection) {
                                displayVersion
                            } else {
                                animatedGame.latestVersion
                            },
                        displayBuild =
                            if (isActiveSelection) {
                                displayBuild
                            } else {
                                animatedGame.buildForCurrentPlatform()
                            },
                        versionHistory = if (isActiveSelection) versionHistory else emptyList(),
                        isVersionPickerVisible = isActiveSelection && isVersionPickerVisible,
                        isVersionHistoryLoading = isActiveSelection && isVersionHistoryLoading,
                        isInstalledForDisplay = isActiveSelection && isInstalledForDisplay,
                        gameUpdateAvailable = isActiveSelection && gameUpdateAvailable,
                        isWebGame =
                            if (isActiveSelection) {
                                isWebGame
                            } else {
                                PlatformKey.WEB in animatedGame.builds
                            },
                        isInstallStatePending = isActiveSelection && isInstallStatePending,
                        isDownloading = isActiveSelection && isDownloading,
                        isChargingLaunch = isActiveSelection && isChargingLaunch,
                        canUninstall = isActiveSelection && canUninstall,
                        isChargingUninstall = isActiveSelection && isChargingUninstall,
                        isUninstalling = isActiveSelection && isUninstalling,
                        onDiskSizeBytes = if (isActiveSelection) onDiskSizeBytes else null,
                        launchErrorMessage = if (isActiveSelection) launchErrorMessage else null,
                        ambientColor = if (isActiveSelection) ambientColor else Color.Transparent,
                        onVersionPickerToggled = onVersionPickerToggled,
                        onVersionSelected = onVersionSelected,
                        onDownloadClicked = onDownloadClicked,
                        onLaunchClicked = onLaunchClicked,
                        onOpenClicked = onOpenClicked,
                        onLaunchChargeComplete = onLaunchChargeComplete,
                        onUninstallClicked = onUninstallClicked,
                        onUninstallChargeComplete = onUninstallChargeComplete,
                        onAmbientColorExtracted =
                            if (isActiveSelection) {
                                onAmbientColorExtracted
                            } else {
                                { _, _ -> }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun GameDetailContent(
    game: GameCatalogEntry,
    displayVersion: String,
    displayBuild: GameBuild?,
    versionHistory: List<GameVersionEntry>,
    isVersionPickerVisible: Boolean,
    isVersionHistoryLoading: Boolean,
    isInstalledForDisplay: Boolean,
    gameUpdateAvailable: Boolean,
    isWebGame: Boolean,
    isInstallStatePending: Boolean,
    isDownloading: Boolean,
    isChargingLaunch: Boolean,
    canUninstall: Boolean,
    isChargingUninstall: Boolean,
    isUninstalling: Boolean,
    onDiskSizeBytes: Long?,
    launchErrorMessage: String?,
    ambientColor: Color,
    onVersionPickerToggled: () -> Unit,
    onVersionSelected: (String) -> Unit,
    onDownloadClicked: () -> Unit,
    onLaunchClicked: () -> Unit,
    onOpenClicked: () -> Unit,
    onLaunchChargeComplete: () -> Unit,
    onUninstallClicked: () -> Unit,
    onUninstallChargeComplete: () -> Unit,
    onAmbientColorExtracted: (Color, String?) -> Unit,
) {
    val currentPlatformKey = PlatformKey.current()
    val highlightedPlatformKey =
        if (isWebGame) {
            PlatformKey.WEB
        } else {
            currentPlatformKey
        }
    val availableBuilds =
        versionHistory
            .firstOrNull { it.version == displayVersion }
            ?.builds
            ?: game.builds

    Column(modifier = Modifier.fillMaxSize()) {
        ThumbnailImage(
            imageUrl = game.thumbnailUrl,
            contentDescription = game.title,
            ambientColor = ambientColor,
            onColorExtracted = { color ->
                onAmbientColorExtracted(color, game.thumbnailUrl)
            },
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(LauncherSpacing.Lg),
        ) {
            DisplayTitle(text = game.title)

            TerminalRule(modifier = Modifier.padding(vertical = LauncherSpacing.Md))

            VersionSelector(
                selectedVersion = displayVersion,
                versions = versionHistory,
                isLoading = isVersionHistoryLoading,
                isExpanded = isVersionPickerVisible,
                currentPlatformKey = highlightedPlatformKey,
                onToggle = onVersionPickerToggled,
                onVersionSelected = onVersionSelected,
            )

            MetadataTable(
                currentPlatformKey = currentPlatformKey,
                currentPlatformBuild = displayBuild,
                availableBuilds = availableBuilds,
                isWebGame = isWebGame,
                isInstalled = isInstalledForDisplay,
                isDownloading = isDownloading,
                onDiskSizeBytes = if (isInstalledForDisplay) onDiskSizeBytes else null,
                modifier = Modifier.padding(top = LauncherSpacing.Md),
            )

            when {
                displayBuild == null -> {
                    PlatformUnavailableBadge(modifier = Modifier.padding(top = LauncherSpacing.Lg))
                }

                isWebGame -> {
                    TerminalButton(
                        label = "OPEN",
                        onClick = onOpenClicked,
                        modifier = Modifier.padding(top = LauncherSpacing.Lg),
                    )
                }

                isInstallStatePending -> Unit

                gameUpdateAvailable -> {
                    TerminalButton(
                        label = "UPDATE",
                        onClick = onDownloadClicked,
                        enabled = !isDownloading,
                        modifier = Modifier.padding(top = LauncherSpacing.Lg),
                    )
                    if (isInstalledForDisplay) {
                        TerminalButton(
                            label = "LAUNCH",
                            onClick = onLaunchClicked,
                            charging = isChargingLaunch,
                            onChargeComplete = onLaunchChargeComplete,
                            enabled = !isChargingUninstall && !isUninstalling && !isDownloading,
                            modifier = Modifier.padding(top = LauncherSpacing.Sm),
                        )
                        TerminalButton(
                            label = "UNINSTALL",
                            onClick = onUninstallClicked,
                            enabled = canUninstall,
                            charging = isChargingUninstall,
                            onChargeComplete = onUninstallChargeComplete,
                            modifier = Modifier.padding(top = LauncherSpacing.Sm),
                        )
                    }
                }

                isInstalledForDisplay -> {
                    TerminalButton(
                        label = "LAUNCH",
                        onClick = onLaunchClicked,
                        charging = isChargingLaunch,
                        onChargeComplete = onLaunchChargeComplete,
                        enabled = !isChargingUninstall && !isUninstalling,
                        modifier = Modifier.padding(top = LauncherSpacing.Lg),
                    )
                    TerminalButton(
                        label = "UNINSTALL",
                        onClick = onUninstallClicked,
                        enabled = canUninstall,
                        charging = isChargingUninstall,
                        onChargeComplete = onUninstallChargeComplete,
                        modifier = Modifier.padding(top = LauncherSpacing.Sm),
                    )
                    when {
                        isChargingUninstall -> {
                            MonoLabel(
                                text = "CONFIRM UNINSTALL…",
                                muted = true,
                                modifier = Modifier.padding(top = LauncherSpacing.Sm),
                            )
                        }
                        isUninstalling -> {
                            MonoLabel(
                                text = "REMOVING FILES…",
                                muted = true,
                                modifier = Modifier.padding(top = LauncherSpacing.Sm),
                            )
                        }
                    }
                }

                else -> {
                    TerminalButton(
                        label = "DOWNLOAD",
                        onClick = onDownloadClicked,
                        enabled = !isDownloading,
                        modifier = Modifier.padding(top = LauncherSpacing.Lg),
                    )
                }
            }

            if (launchErrorMessage != null) {
                MonoLabel(
                    text = launchErrorMessage.uppercase(),
                    accent = true,
                    modifier = Modifier.padding(top = LauncherSpacing.Sm),
                )
            }
        }
    }
}

@Composable
private fun DetailMessage(text: String) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(LauncherSpacing.Lg),
    ) {
        MonoLabel(text = text, accent = true)
    }
}
