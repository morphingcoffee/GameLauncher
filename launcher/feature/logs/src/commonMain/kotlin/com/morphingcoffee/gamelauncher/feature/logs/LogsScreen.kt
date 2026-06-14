package com.morphingcoffee.gamelauncher.feature.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.designsystem.components.AppHeader
import com.morphingcoffee.gamelauncher.core.designsystem.components.DisplayTitle
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel
import com.morphingcoffee.gamelauncher.core.designsystem.components.StatusBar
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalButton
import com.morphingcoffee.gamelauncher.core.designsystem.platformClockText
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.logging.LogEntry
import com.morphingcoffee.gamelauncher.core.logging.LogLevel
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import kotlinx.coroutines.delay

@Composable
fun LogsScreen(onBack: () -> Unit) {
    var clockText by remember { mutableStateOf(platformClockText()) }
    val entries by AppLog.entries.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            clockText = platformClockText()
            delay(1_000)
        }
    }

    LogsScreenContent(
        entries = entries,
        clockText = clockText,
        onBack = onBack,
        onClear = { AppLog.clear() },
        onCopyAll = { copyTextToClipboard(AppLog.formatAll()) },
    )
}

@Composable
fun LogsScreenContent(
    entries: List<LogEntry>,
    clockText: String,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(LauncherColors.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                appVersion = LauncherMetadata.VERSION,
                platformLabel = formatPlatformLabel(PlatformKey.current()),
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = LauncherSpacing.Lg, vertical = LauncherSpacing.Md),
            ) {
                DisplayTitle(text = "Logs")

                MonoLabel(
                    text = "${entries.size} ENTRIES · NEWEST AT BOTTOM",
                    muted = true,
                    modifier = Modifier.padding(top = LauncherSpacing.Sm, bottom = LauncherSpacing.Md),
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Xs),
                ) {
                    items(entries, key = { "${it.timestampMillis}-${it.tag}-${it.message}" }) { entry ->
                        MonoLabel(
                            text = AppLog.formatEntry(entry),
                            accent = entry.level == LogLevel.ERROR || entry.level == LogLevel.WARN,
                            muted = entry.level == LogLevel.DEBUG,
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = LauncherSpacing.Md),
                    horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Sm),
                ) {
                    TerminalButton(
                        label = "[ BACK ]",
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                    )
                    TerminalButton(
                        label = "[ CLEAR ]",
                        onClick = onClear,
                        modifier = Modifier.weight(1f),
                    )
                    TerminalButton(
                        label = "[ COPY ALL ]",
                        onClick = onCopyAll,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            StatusBar(
                statusText = "LOGS",
                clockText = clockText,
            )
        }
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
    name = "Logs",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun LogsScreenPreview() {
    LauncherTheme {
        LogsScreenContent(
            entries =
                listOf(
                    LogEntry(
                        timestampMillis = 0L,
                        level = LogLevel.INFO,
                        tag = "Catalog",
                        message = "Catalog loaded",
                    ),
                    LogEntry(
                        timestampMillis = 1L,
                        level = LogLevel.ERROR,
                        tag = "Installer",
                        message = "Uninstall failed",
                        throwableSummary = "IOException: file locked",
                    ),
                ),
            clockText = "12:34:56",
            onBack = {},
            onClear = {},
            onCopyAll = {},
        )
    }
}
