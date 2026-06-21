package com.morphingcoffee.gamelauncher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.designsystem.components.AppHeader
import com.morphingcoffee.gamelauncher.core.designsystem.components.DisplayTitle
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateDetails
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateSignal
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel
import com.morphingcoffee.gamelauncher.core.designsystem.components.StatusBar
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalButton
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalLinkRow
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.onEvent(AboutEvent.Started)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            viewModel.onEvent(AboutEvent.ClockTick)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AboutEffect.OpenUrl -> uriHandler.openUri(effect.url)
            }
        }
    }

    SettingsScreenContent(
        state = state,
        onBack = onBack,
        onUpdateClicked = { viewModel.onEvent(AboutEvent.UpdateClicked) },
        onUpdateChargeComplete = { viewModel.onEvent(AboutEvent.UpdateChargeComplete) },
        onGetLatestClicked = { viewModel.onEvent(AboutEvent.GetLatestClicked) },
    )
}

@Composable
fun SettingsScreenContent(
    state: AboutState,
    onBack: () -> Unit,
    onUpdateClicked: () -> Unit = {},
    onUpdateChargeComplete: () -> Unit = {},
    onGetLatestClicked: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val channelLatestVersion = state.channelLatestVersion

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(LauncherColors.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                appVersion = state.appVersion,
                platformLabel = state.platformLabel,
                launcherUpdateSlot =
                    if (state.showUpdateButton && channelLatestVersion != null) {
                        {
                            LauncherUpdateSignal(
                                latestVersion = channelLatestVersion,
                                onClick = onUpdateClicked,
                            )
                        }
                    } else {
                        null
                    },
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = LauncherSpacing.Lg, vertical = LauncherSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Lg),
            ) {
                DisplayTitle(text = "About")

                if (state.showUpdateButton && channelLatestVersion != null) {
                    LauncherUpdateDetails(
                        currentVersion = state.appVersion,
                        latestVersion = channelLatestVersion,
                        channelKey = state.updateEvaluation?.channelKey,
                        fileSizeBytes = state.updateEvaluation?.channelBuild?.fileSizeBytes,
                        errorMessage = state.updateErrorMessage,
                    )

                    TerminalButton(
                        label = "UPDATE LAUNCHER",
                        onClick = onUpdateClicked,
                        charging = state.isUpdateCharging,
                        onChargeComplete = onUpdateChargeComplete,
                        enabled = !state.isUpdateDownloading,
                    )
                } else {
                    SettingsInfoRow(
                        label = "VERSION",
                        value = state.appVersion,
                    )
                }

                state.links.forEach { link ->
                    TerminalLinkRow(
                        label = link.label,
                        linkText = link.displayText,
                        onClick = { uriHandler.openUri(link.url) },
                    )
                }

                TerminalButton(
                    label = "[ GET LATEST ]",
                    onClick = onGetLatestClicked,
                )

                TerminalButton(
                    label = "[ BACK ]",
                    onClick = onBack,
                    modifier = Modifier.padding(top = LauncherSpacing.Md),
                )
            }

            StatusBar(
                statusText =
                    when {
                        state.isUpdateDownloading -> "LAUNCHER · UPDATING"
                        else -> "ABOUT"
                    },
                clockText = state.clockText,
                downloadProgress = state.downloadProgressFraction,
            )
        }
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        MonoLabel(
            text = label,
            muted = true,
            modifier = Modifier.width(72.dp),
        )
        MonoLabel(text = "·")
        MonoLabel(text = value)
    }
}

@Preview(
    name = "About",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun SettingsScreenPreview() {
    LauncherTheme {
        SettingsScreenContent(
            state =
                AboutState(
                    appVersion = LauncherMetadata.VERSION,
                    platformLabel = "macos-arm64",
                    clockText = "12:34:56",
                ),
            onBack = {},
        )
    }
}
