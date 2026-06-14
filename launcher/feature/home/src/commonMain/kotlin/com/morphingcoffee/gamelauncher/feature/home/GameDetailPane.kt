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
    game: GameCatalogEntry?,
    isLoading: Boolean,
    errorMessage: String?,
    launchErrorMessage: String?,
    displayVersion: String,
    displayBuild: GameBuild?,
    versionHistory: List<GameVersionEntry>,
    isVersionPickerVisible: Boolean,
    isVersionHistoryLoading: Boolean,
    isInstalledForDisplay: Boolean,
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
    onLaunchChargeComplete: () -> Unit,
    onUninstallClicked: () -> Unit,
    onUninstallChargeComplete: () -> Unit,
    onAmbientColorExtracted: (Color, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.matchParentSize().ambientGlow(ambientColor))

        AnimatedContent(
            targetState = game?.id ?: "empty",
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (
                    fadeIn(tween(150, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(150, easing = FastOutSlowInEasing)) { it / 4 }
                ).togetherWith(
                    fadeOut(tween(150)) +
                        slideOutHorizontally(tween(150)) { -it / 4 },
                )
            },
            label = "game_detail_pane",
        ) { _ ->
            when {
                isLoading -> {
                    DetailMessage(text = "LOADING CATALOG...")
                }

                errorMessage != null && game == null -> {
                    DetailMessage(text = errorMessage.uppercase())
                }

                game == null -> {
                    DetailMessage(text = "NO ENTRIES")
                }

                else -> {
                    GameDetailContent(
                        game = game,
                        displayVersion = displayVersion,
                        displayBuild = displayBuild,
                        versionHistory = versionHistory,
                        isVersionPickerVisible = isVersionPickerVisible,
                        isVersionHistoryLoading = isVersionHistoryLoading,
                        isInstalledForDisplay = isInstalledForDisplay,
                        isInstallStatePending = isInstallStatePending,
                        isDownloading = isDownloading,
                        isChargingLaunch = isChargingLaunch,
                        canUninstall = canUninstall,
                        isChargingUninstall = isChargingUninstall,
                        isUninstalling = isUninstalling,
                        onDiskSizeBytes = onDiskSizeBytes,
                        launchErrorMessage = launchErrorMessage,
                        ambientColor = ambientColor,
                        onVersionPickerToggled = onVersionPickerToggled,
                        onVersionSelected = onVersionSelected,
                        onDownloadClicked = onDownloadClicked,
                        onLaunchClicked = onLaunchClicked,
                        onLaunchChargeComplete = onLaunchChargeComplete,
                        onUninstallClicked = onUninstallClicked,
                        onUninstallChargeComplete = onUninstallChargeComplete,
                        onAmbientColorExtracted = onAmbientColorExtracted,
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
    onLaunchChargeComplete: () -> Unit,
    onUninstallClicked: () -> Unit,
    onUninstallChargeComplete: () -> Unit,
    onAmbientColorExtracted: (Color, String?) -> Unit,
) {
    val currentPlatformKey = PlatformKey.current()
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
                currentPlatformKey = currentPlatformKey,
                onToggle = onVersionPickerToggled,
                onVersionSelected = onVersionSelected,
            )

            MetadataTable(
                currentPlatformKey = currentPlatformKey,
                currentPlatformBuild = displayBuild,
                availableBuilds = availableBuilds,
                onDiskSizeBytes = if (isInstalledForDisplay) onDiskSizeBytes else null,
                modifier = Modifier.padding(top = LauncherSpacing.Md),
            )

            when {
                displayBuild == null -> {
                    PlatformUnavailableBadge(modifier = Modifier.padding(top = LauncherSpacing.Lg))
                }

                isInstallStatePending -> Unit

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
