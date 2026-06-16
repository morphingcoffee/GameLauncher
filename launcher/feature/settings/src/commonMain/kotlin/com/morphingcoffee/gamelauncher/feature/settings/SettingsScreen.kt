package com.morphingcoffee.gamelauncher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.designsystem.components.AppHeader
import com.morphingcoffee.gamelauncher.core.designsystem.components.DisplayTitle
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel
import com.morphingcoffee.gamelauncher.core.designsystem.components.StatusBar
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalButton
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalLinkRow
import com.morphingcoffee.gamelauncher.core.designsystem.platformClockText
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var state by remember {
        mutableStateOf(
            SettingsState(
                platformLabel = formatPlatformLabel(PlatformKey.current()),
            ),
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            state = state.copy(clockText = platformClockText())
            delay(1_000)
        }
    }

    SettingsScreenContent(
        state = state,
        onBack = onBack,
    )
}

@Composable
fun SettingsScreenContent(
    state: SettingsState,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

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

                SettingsInfoRow(
                    label = "VERSION",
                    value = state.appVersion,
                )

                state.links.forEach { link ->
                    TerminalLinkRow(
                        label = link.label,
                        linkText = link.displayText,
                        onClick = { uriHandler.openUri(link.url) },
                    )
                }

                TerminalButton(
                    label = "[ BACK ]",
                    onClick = onBack,
                    modifier = Modifier.padding(top = LauncherSpacing.Md),
                )
            }

            StatusBar(
                statusText = "ABOUT",
                clockText = state.clockText,
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
    androidx.compose.foundation.layout.Row(
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

private fun formatPlatformLabel(platformKey: String?): String =
    when (platformKey) {
        PlatformKey.WINDOWS_X64 -> "windows-x64"
        PlatformKey.MACOS_ARM64 -> "macos-arm64"
        PlatformKey.MACOS_X64 -> "macos-x64"
        null -> "unknown"
        else -> platformKey
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
                SettingsState(
                    appVersion = LauncherMetadata.VERSION,
                    platformLabel = "macos-arm64",
                    clockText = "12:34:56",
                ),
            onBack = {},
        )
    }
}
