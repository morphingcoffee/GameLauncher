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
import com.morphingcoffee.gamelauncher.core.designsystem.components.ambientGlow
import com.morphingcoffee.gamelauncher.core.model.GameCatalogEntry
import com.morphingcoffee.gamelauncher.core.model.PlatformKey

@Composable
internal fun GameDetailPane(
    game: GameCatalogEntry?,
    isLoading: Boolean,
    errorMessage: String?,
    isChargingLaunch: Boolean,
    ambientColor: Color,
    onLaunchClicked: () -> Unit,
    onLaunchChargeComplete: () -> Unit,
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

                errorMessage != null -> {
                    DetailMessage(text = errorMessage.uppercase())
                }

                game == null -> {
                    DetailMessage(text = "NO ENTRIES")
                }

                else -> {
                    GameDetailContent(
                        game = game,
                        isChargingLaunch = isChargingLaunch,
                        ambientColor = ambientColor,
                        onLaunchClicked = onLaunchClicked,
                        onLaunchChargeComplete = onLaunchChargeComplete,
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
    isChargingLaunch: Boolean,
    ambientColor: Color,
    onLaunchClicked: () -> Unit,
    onLaunchChargeComplete: () -> Unit,
    onAmbientColorExtracted: (Color, String?) -> Unit,
) {
    val build = game.buildForCurrentPlatform()
    val isAvailable = build != null
    val currentPlatformKey = PlatformKey.current()

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
                    .padding(LauncherSpacing.Lg),
        ) {
            DisplayTitle(text = game.title)

            TerminalRule(modifier = Modifier.padding(vertical = LauncherSpacing.Md))

            MetadataTable(
                version = game.latestVersion,
                currentPlatformKey = currentPlatformKey,
                currentPlatformBuild = build,
                availableBuilds = game.builds,
            )

            if (isAvailable) {
                TerminalButton(
                    label = "LAUNCH",
                    onClick = onLaunchClicked,
                    charging = isChargingLaunch,
                    onChargeComplete = onLaunchChargeComplete,
                    modifier = Modifier.padding(top = LauncherSpacing.Lg),
                )
            } else {
                PlatformUnavailableBadge(modifier = Modifier.padding(top = LauncherSpacing.Lg))
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
