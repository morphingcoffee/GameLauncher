package com.morphingcoffee.gamelauncher.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.designsystem.TerminalRule
import com.morphingcoffee.gamelauncher.core.designsystem.components.AppHeader
import com.morphingcoffee.gamelauncher.core.designsystem.components.DisplayTitle
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateInfoRow
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateSheet
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateSheetState
import com.morphingcoffee.gamelauncher.core.designsystem.components.LauncherUpdateSignal
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel
import com.morphingcoffee.gamelauncher.core.designsystem.components.StatusBar
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalButton
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalLinkRow
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalOptionRow
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalToggleRow
import com.morphingcoffee.gamelauncher.core.designsystem.formatLauncherVersionInfoValue
import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme
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
        onLauncherUpdateSignalClicked = { viewModel.onEvent(AboutEvent.LauncherUpdateSignalClicked) },
        onLauncherUpdateSheetDismissed = { viewModel.onEvent(AboutEvent.LauncherUpdateSheetDismissed) },
        onUpdateClicked = { viewModel.onEvent(AboutEvent.UpdateClicked) },
        onUpdateChargeComplete = { viewModel.onEvent(AboutEvent.UpdateChargeComplete) },
        onReleaseNotesClicked = { viewModel.onEvent(AboutEvent.ReleaseNotesClicked) },
        onSendCrashReportsToggled = { viewModel.onEvent(AboutEvent.SendCrashReportsToggled) },
        onShareExtendedDiagnosticsToggled = { viewModel.onEvent(AboutEvent.ShareExtendedDiagnosticsToggled) },
        onTestSentryClicked = { viewModel.onEvent(AboutEvent.TestSentryClicked) },
        onBackgroundThemeSelected = { theme ->
            viewModel.onEvent(AboutEvent.BackgroundThemeSelected(theme))
        },
    )
}

@Composable
fun SettingsScreenContent(
    state: AboutState,
    onBack: () -> Unit,
    onLauncherUpdateSignalClicked: () -> Unit = {},
    onLauncherUpdateSheetDismissed: () -> Unit = {},
    onUpdateClicked: () -> Unit = {},
    onUpdateChargeComplete: () -> Unit = {},
    onReleaseNotesClicked: () -> Unit = {},
    onSendCrashReportsToggled: () -> Unit = {},
    onShareExtendedDiagnosticsToggled: () -> Unit = {},
    onTestSentryClicked: () -> Unit = {},
    onBackgroundThemeSelected: (LauncherBackgroundTheme) -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val channelLatestVersion = state.channelLatestVersion
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                appVersion = state.appVersion,
                platformLabel = state.platformLabel,
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

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = LauncherSpacing.Lg, vertical = LauncherSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Lg),
            ) {
                DisplayTitle(text = "Settings")

                MonoLabel(text = "APPEARANCE / BACKGROUND", muted = true)

                Column(
                    verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Xs),
                ) {
                    state.backgroundThemes.forEach { theme ->
                        TerminalOptionRow(
                            label = theme.displayName,
                            selected = theme == state.backgroundTheme,
                            onClick = { onBackgroundThemeSelected(theme) },
                        )
                    }
                }

                TerminalRule()

                MonoLabel(text = "ABOUT", muted = true)

                LauncherUpdateInfoRow(
                    label = "VERSION",
                    value = formatLauncherVersionInfoValue(state.appVersion),
                )

                TerminalToggleRow(
                    label = "CRASH RPT",
                    checked = state.sendCrashReports,
                    description = "Crashes & launch failures. No identity.",
                    onToggle = onSendCrashReportsToggled,
                )

                TerminalToggleRow(
                    label = "EXT DIAG",
                    checked = state.shareExtendedDiagnostics,
                    enabled = state.extendedDiagnosticsEnabled,
                    description = "Redacted output + recent logs on failure.",
                    onToggle = onShareExtendedDiagnosticsToggled,
                )

                if (state.showSentryTestButton) {
                    TerminalButton(
                        label = "[ TEST SENTRY ]",
                        onClick = onTestSentryClicked,
                        enabled = state.sendCrashReports,
                    )
                    state.sentryTestStatus?.let { status ->
                        MonoLabel(text = status, muted = true)
                    }
                }

                state.links.forEach { link ->
                    TerminalLinkRow(
                        label = link.label,
                        linkText = link.displayText,
                        onClick = { uriHandler.openUri(link.url) },
                    )
                }

                if (state.releasesUrl.isNotBlank()) {
                    TerminalLinkRow(
                        label = "RELEASES",
                        linkText = "github.com/morphingcoffee/GameLauncher/releases",
                        onClick = { uriHandler.openUri(state.releasesUrl) },
                    )
                }

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
                        else -> "SETTINGS"
                    },
                clockText = state.clockText,
                downloadProgress = state.downloadProgressFraction,
            )
        }

        LauncherUpdateSheet(
            state = state.toLauncherUpdateSheetState(),
            onDismiss = onLauncherUpdateSheetDismissed,
            onUpdateClicked = onUpdateClicked,
            onUpdateChargeComplete = onUpdateChargeComplete,
            onReleaseNotesClicked = onReleaseNotesClicked,
        )
    }
}

private fun AboutState.toLauncherUpdateSheetState(): LauncherUpdateSheetState =
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
    name = "Settings",
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
                    releasesUrl = "https://github.com/morphingcoffee/GameLauncher/releases",
                    sendCrashReports = true,
                    shareExtendedDiagnostics = false,
                    backgroundTheme = LauncherBackgroundTheme.SPECTRAL_TOPOLOGY,
                ),
            onBack = {},
        )
    }
}
