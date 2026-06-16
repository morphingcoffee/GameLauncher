package com.morphingcoffee.gamelauncher.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.morphingcoffee.gamelauncher.core.designsystem.components.StorageChartAnimation
import com.morphingcoffee.gamelauncher.core.designsystem.components.StoragePieChart
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalButton
import com.morphingcoffee.gamelauncher.core.designsystem.components.TerminalOverlay
import com.morphingcoffee.gamelauncher.core.designsystem.components.pieSegmentColor
import com.morphingcoffee.gamelauncher.core.designsystem.formatFileSize
import com.morphingcoffee.gamelauncher.core.navigation.NavigationBackInterceptor
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/** Breakdown always occupies this height so the pie slot above stays a constant size. */
private val StoragePieTargetSize = 280.dp

private val StorageBreakdownAreaHeight = 168.dp

@Composable
fun StorageScreen(
    onBack: () -> Unit,
    viewModel: StorageViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(StorageEvent.Started)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(StorageEvent.ScreenHidden)
        }
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
        onChartAnimationFinished = { viewModel.onEvent(StorageEvent.ChartAnimationFinished) },
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
    onChartAnimationFinished: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val dialogFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.activeDialog) {
        if (state.activeDialog != null) {
            dialogFocusRequester.requestFocus()
        } else {
            focusRequester.requestFocus()
        }
    }

    DisposableEffect(state.activeDialog, state.isUninstalling) {
        NavigationBackInterceptor.handler = {
            if (state.isUninstalling) {
                true
            } else if (state.activeDialog != null) {
                onDialogDismiss()
                true
            } else {
                onBack()
                true
            }
        }
        onDispose {
            NavigationBackInterceptor.handler = null
        }
    }

    fun handleBack(): Boolean {
        if (state.isUninstalling) return true
        if (state.activeDialog != null) {
            onDialogDismiss()
            return true
        }
        onBack()
        return true
    }

    val showBreakdown =
        state.displayBreakdownSegments.isNotEmpty() &&
            !state.isLoading &&
            when (state.chartAnimation) {
                is StorageChartAnimation.Vortex,
                is StorageChartAnimation.SegmentBurst,
                -> false
                else -> true
            }

    LaunchedEffect(showBreakdown) {
        if (!showBreakdown) {
            onSegmentHover(null)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(LauncherColors.Background)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || event.key != Key.Escape) return@onPreviewKeyEvent false
                    handleBack()
                },
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
                        .padding(horizontal = LauncherSpacing.Lg),
            ) {
                DisplayTitle(
                    text = "Storage",
                    modifier =
                        Modifier.padding(
                            top = LauncherSpacing.Xl,
                            bottom = LauncherSpacing.Md,
                        ),
                )

                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    BoxWithConstraints(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(StoragePieTargetSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        val chartSize = minOf(maxWidth, maxHeight, StoragePieTargetSize)
                        StoragePieChart(
                            segments = state.segments.map { it.pieSegment },
                            totalBytes = state.displayCenterTotalBytes,
                            centerInstallCount = state.displayCenterInstallCount,
                            hoveredSegmentId = state.hoveredSegmentId,
                            isLoading = state.isLoading,
                            chartAnimation = state.chartAnimation,
                            onChartAnimationFinished = onChartAnimationFinished,
                            onSegmentHover = onSegmentHover,
                            onSegmentClick = onSegmentClick,
                            onCenterClick = onCenterClick,
                            modifier = Modifier.size(chartSize),
                        )
                    }

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(StorageBreakdownAreaHeight)
                                .padding(top = LauncherSpacing.Sm),
                    ) {
                        AnimatedVisibility(
                            visible = showBreakdown,
                            enter = fadeIn(animationSpec = tween(400)),
                            exit = fadeOut(animationSpec = tween(280)),
                        ) {
                            StorageBreakdownPanel(
                                segments = state.displayBreakdownSegments,
                                hoveredSegmentId = state.hoveredSegmentId,
                                onSegmentHover = onSegmentHover,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LauncherSpacing.Lg, vertical = LauncherSpacing.Md),
                horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Sm),
            ) {
                TerminalButton(
                    label = "[ BACK ]",
                    onClick = { handleBack() },
                    enabled = !state.isUninstalling,
                )
                TerminalButton(
                    label = "[ REFRESH ]",
                    onClick = onRefresh,
                    enabled = !state.isLoading && !state.isUninstalling,
                )
            }

            StatusBar(
                statusText = state.statusLabel,
                clockText = state.clockText,
            )
        }

        when (val dialog = state.activeDialog) {
            is StorageDialog.GameDetail -> {
                TerminalOverlay(
                    onDismiss = onDialogDismiss,
                    modifier =
                        Modifier
                            .focusRequester(dialogFocusRequester)
                            .focusable()
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown || event.key != Key.Escape) {
                                    return@onPreviewKeyEvent false
                                }
                                onDialogDismiss()
                                true
                            },
                ) {
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
                TerminalOverlay(
                    onDismiss = onDialogDismiss,
                    modifier =
                        Modifier
                            .focusRequester(dialogFocusRequester)
                            .focusable()
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown || event.key != Key.Escape) {
                                    return@onPreviewKeyEvent false
                                }
                                onDialogDismiss()
                                true
                            },
                ) {
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
private fun StorageBreakdownPanel(
    segments: List<StorageSegmentUi>,
    hoveredSegmentId: String?,
    onSegmentHover: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val canScrollForward by remember {
        derivedStateOf { scrollState.maxValue > scrollState.value + 1 }
    }

    LaunchedEffect(segments) {
        scrollState.scrollTo(0)
    }

    Box(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Sm),
        ) {
            MonoLabel(text = "BREAKDOWN", muted = true)
            segments.forEach { segment ->
                StorageLegendRow(
                    segment = segment,
                    isHovered = segment.gameId == hoveredSegmentId,
                    onHover = { hovered ->
                        onSegmentHover(if (hovered) segment.gameId else null)
                    },
                )
            }
        }

        if (canScrollForward) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            LauncherColors.Background.copy(alpha = 0.94f),
                                        ),
                                ),
                        ),
            )
        }
    }
}

@Composable
private fun StorageLegendRow(
    segment: StorageSegmentUi,
    isHovered: Boolean,
    onHover: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isLocallyHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(isLocallyHovered) {
        onHover(isLocallyHovered)
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .hoverable(interactionSource = interactionSource)
                .background(
                    if (isHovered) {
                        LauncherColors.Accent.copy(alpha = 0.07f)
                    } else {
                        Color.Transparent
                    },
                ).border(
                    width = if (isHovered) 1.dp else 0.dp,
                    color =
                        if (isHovered) {
                            segment.pieSegment.color.copy(alpha = 0.55f)
                        } else {
                            Color.Transparent
                        },
                ).padding(
                    horizontal = LauncherSpacing.Sm,
                    vertical = LauncherSpacing.Xs,
                ),
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
            accent = isHovered,
            modifier = Modifier.weight(1f),
        )
        MonoLabel(text = formatFileSize(segment.sizeBytes), muted = !isHovered, accent = isHovered)
        MonoLabel(text = formatShare(segment.shareFraction), muted = !isHovered, accent = isHovered)
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
    name = "Storage — default window",
    widthDp = 900,
    heightDp = 620,
    showBackground = true,
)
@Composable
private fun StorageScreenDefaultWindowPreview() {
    val segments =
        listOf(
            previewSegment("void-runner", "VOID RUNNER", "1.4.2", 669_600_000L, 0.57f, 0),
            previewSegment("neon-drift", "NEON DRIFT", "0.9.0", 410_400_000L, 0.35f, 1),
            previewSegment("solo-arm", "SOLO ARM", "0.0.1", 49_000_000L, 0.04f, 2),
            previewSegment("glass-circuit", "GLASS CIRCUIT", "0.0.1", 70_000_000L, 0.06f, 3),
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
            onChartAnimationFinished = {},
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
