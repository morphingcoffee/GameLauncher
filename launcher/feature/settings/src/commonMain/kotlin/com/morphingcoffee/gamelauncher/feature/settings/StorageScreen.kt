package com.morphingcoffee.gamelauncher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTheme
import com.morphingcoffee.gamelauncher.core.designsystem.components.AppHeader
import com.morphingcoffee.gamelauncher.core.designsystem.components.DisplayTitle
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel
import com.morphingcoffee.gamelauncher.core.designsystem.components.PieSegment
import com.morphingcoffee.gamelauncher.core.designsystem.components.StatusBar
import com.morphingcoffee.gamelauncher.core.designsystem.components.StoragePieChart
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalButton
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalOverlay
import com.morphingcoffee.gamelauncher.core.designsystem.components.pieSegmentColor
import com.morphingcoffee.gamelauncher.core.designsystem.formatFileSize
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StorageScreen(
    onBack: () -> Unit,
    viewModel: StorageViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(StorageEvent.Started)
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.onEvent(StorageEvent.ClockTick)
            delay(1_000)
        }
    }

    StorageScreenContent(
        state = state,
        onBack = onBack,
        onRefresh = { viewModel.onEvent(StorageEvent.Refresh) },
        onSegmentHover = { viewModel.onEvent(StorageEvent.SegmentHovered(it)) },
        onSegmentClick = { viewModel.onEvent(StorageEvent.SegmentClicked(it)) },
        onCenterClick = { viewModel.onEvent(StorageEvent.CenterClicked) },
        onDialogDismiss = { viewModel.onEvent(StorageEvent.DialogDismissed) },
        onUninstallClicked = { viewModel.onEvent(StorageEvent.UninstallClicked) },
        onUninstallChargeComplete = { viewModel.onEvent(StorageEvent.UninstallChargeComplete) },
        onUninstallAllClicked = { viewModel.onEvent(StorageEvent.UninstallAllClicked) },
        onUninstallAllChargeComplete = { viewModel.onEvent(StorageEvent.UninstallAllChargeComplete) },
    )
}

@Composable
fun StorageScreenContent(
    state: StorageState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSegmentHover: (String?) -> Unit,
    onSegmentClick: (String) -> Unit,
    onCenterClick: () -> Unit,
    onDialogDismiss: () -> Unit,
    onUninstallClicked: () -> Unit,
    onUninstallChargeComplete: () -> Unit,
    onUninstallAllClicked: () -> Unit,
    onUninstallAllChargeComplete: () -> Unit,
) {
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
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = LauncherSpacing.Lg, vertical = LauncherSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Lg),
            ) {
                DisplayTitle(text = "Storage")

                StoragePieChart(
                    segments = state.segments.map { it.pieSegment },
                    totalBytes = state.totalBytes,
                    hoveredSegmentId = state.hoveredSegmentId,
                    onSegmentHover = onSegmentHover,
                    onSegmentClick = onSegmentClick,
                    onCenterClick = onCenterClick,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                )

                if (state.segments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Sm)) {
                        MonoLabel(text = "BREAKDOWN", muted = true)
                        state.segments.forEach { segment ->
                            StorageLegendRow(segment = segment)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Sm)) {
                    TerminalButton(
                        label = "[ REFRESH ]",
                        onClick = onRefresh,
                        enabled = !state.isLoading && !state.isUninstalling,
                    )
                    TerminalButton(
                        label = "[ BACK ]",
                        onClick = onBack,
                        enabled = !state.isUninstalling,
                    )
                }
            }

            StatusBar(
                statusText = state.statusLabel,
                clockText = state.clockText,
            )
        }

        when (val dialog = state.activeDialog) {
            is StorageDialog.GameDetail -> {
                TerminalOverlay(onDismiss = onDialogDismiss) {
                    DisplayTitle(text = dialog.segment.title)
                    StorageDetailRow(label = "VERSION", value = dialog.segment.version)
                    StorageDetailRow(label = "SIZE", value = formatFileSize(dialog.segment.sizeBytes))
                    StorageDetailRow(
                        label = "SHARE",
                        value = formatShare(dialog.segment.shareFraction),
                    )
                    TerminalButton(
                        label = "UNINSTALL",
                        onClick = onUninstallClicked,
                        enabled = state.canUninstall,
                        charging = state.isChargingUninstall,
                        onChargeComplete = onUninstallChargeComplete,
                    )
                    if (state.isUninstalling) {
                        MonoLabel(text = "REMOVING FILES…", muted = true)
                    }
                    state.errorMessage?.let { message ->
                        MonoLabel(text = message.uppercase(), accent = true)
                    }
                    TerminalButton(label = "[ CANCEL ]", onClick = onDialogDismiss)
                }
            }

            StorageDialog.UninstallAll -> {
                TerminalOverlay(onDismiss = onDialogDismiss) {
                    DisplayTitle(text = "Uninstall All")
                    MonoLabel(
                        text =
                            "${state.segments.size} ${if (state.segments.size == 1) "BUILD" else "BUILDS"} · " +
                                formatFileSize(state.totalBytes),
                        muted = true,
                    )
                    TerminalButton(
                        label = "UNINSTALL ALL",
                        onClick = onUninstallAllClicked,
                        enabled = state.canUninstall,
                        charging = state.isChargingUninstall,
                        onChargeComplete = onUninstallAllChargeComplete,
                    )
                    if (state.isUninstalling) {
                        MonoLabel(text = "REMOVING FILES…", muted = true)
                    }
                    state.errorMessage?.let { message ->
                        MonoLabel(text = message.uppercase(), accent = true)
                    }
                    TerminalButton(label = "[ CANCEL ]", onClick = onDialogDismiss)
                }
            }

            null -> Unit
        }
    }
}

@Composable
private fun StorageLegendRow(
    segment: StorageSegmentUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(12.dp)
                    .height(12.dp)
                    .background(segment.pieSegment.color),
        )
        MonoLabel(
            text = segment.title,
            modifier = Modifier.weight(1f),
        )
        MonoLabel(text = formatFileSize(segment.sizeBytes), muted = true)
        MonoLabel(text = formatShare(segment.shareFraction), muted = true)
    }
}

@Composable
private fun StorageDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoLabel(text = label, muted = true, modifier = Modifier.width(72.dp))
        MonoLabel(text = "·")
        MonoLabel(text = value)
    }
}

private fun formatShare(fraction: Float): String {
    val percent = (fraction * 100f).coerceIn(0f, 100f)
    val scaled = (percent * 10f).toInt() / 10f
    return "$scaled%"
}

@Preview(
    name = "Storage",
    widthDp = 1280,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun StorageScreenPreview() {
    val segments =
        listOf(
            previewSegment("void-runner", "VOID RUNNER", "1.4.2", 55_296_000L, 0.34f, 0),
            previewSegment("neon-drift", "NEON DRIFT", "0.9.0", 95_472_000L, 0.59f, 1),
            previewSegment("iron-ledger", "IRON LEDGER", "0.0.1", 11_340_000L, 0.07f, 2),
        )
    LauncherTheme {
        StorageScreenContent(
            state =
                StorageState(
                    isLoading = false,
                    segments = segments,
                    totalBytes = segments.sumOf { it.sizeBytes },
                    platformLabel = "macos-arm64",
                    clockText = "12:34:56",
                ),
            onBack = {},
            onRefresh = {},
            onSegmentHover = {},
            onSegmentClick = {},
            onCenterClick = {},
            onDialogDismiss = {},
            onUninstallClicked = {},
            onUninstallChargeComplete = {},
            onUninstallAllClicked = {},
            onUninstallAllChargeComplete = {},
        )
    }
}

private fun previewSegment(
    gameId: String,
    title: String,
    version: String,
    sizeBytes: Long,
    shareFraction: Float,
    colorIndex: Int,
): StorageSegmentUi {
    val pieSegment =
        PieSegment(
            id = gameId,
            label = title,
            sizeBytes = sizeBytes,
            shareFraction = shareFraction,
            color = pieSegmentColor(colorIndex),
        )
    return StorageSegmentUi(
        gameId = gameId,
        title = title,
        version = version,
        sizeBytes = sizeBytes,
        shareFraction = shareFraction,
        pieSegment = pieSegment,
    )
}
