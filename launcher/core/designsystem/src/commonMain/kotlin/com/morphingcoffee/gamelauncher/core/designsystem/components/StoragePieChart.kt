package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTypography
import com.morphingcoffee.gamelauncher.core.designsystem.formatFileSize
import kotlinx.coroutines.flow.collectLatest
import kotlin.random.Random

private val centerLabelEnterTransition =
    fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
        slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 2 },
            animationSpec =
                spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMedium,
                ),
        )

private val centerEmptyLabelEnterTransition =
    fadeIn(
        animationSpec =
            tween(
                durationMillis = 780,
                delayMillis = 220,
                easing = FastOutSlowInEasing,
            ),
    ) +
        slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 3 },
            animationSpec =
                spring(
                    dampingRatio = 0.84f,
                    stiffness = Spring.StiffnessLow,
                ),
        )

private val centerLabelExitTransition =
    fadeOut(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
        slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight / 4 },
            animationSpec = tween(240, easing = FastOutSlowInEasing),
        )

private enum class CenterContentState {
    Loading,
    Empty,
    Populated,
}

/** Diameter the center label typography is tuned for (~StoragePieTargetSize). */
private const val CENTER_LABEL_REFERENCE_DIAMETER_DP = 280f

@Composable
fun StoragePieChart(
    segments: List<PieSegment>,
    totalBytes: Long,
    centerInstallCount: Int,
    hoveredSegmentId: String?,
    isLoading: Boolean,
    chartAnimation: StorageChartAnimation?,
    onChartAnimationFinished: () -> Unit,
    onSegmentHover: (String?) -> Unit,
    onSegmentClick: (String) -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val loadingPhase = rememberPieLoadingPhase(isLoading)
    var centerHovered by remember { mutableStateOf(false) }
    val centerPulseRotation =
        rememberCenterPulseRotation(
            enabled =
                !isLoading &&
                    (chartAnimation == null || chartAnimation is StorageChartAnimation.Vortex),
        )
    val animationProgress = rememberChartAnimationProgress(chartAnimation, onChartAnimationFinished)
    val segmentLayouts = remember(segments) { buildSegmentLayouts(segments) }
    val burstParticles =
        remember(chartAnimation, segmentLayouts) {
            val burst = chartAnimation as? StorageChartAnimation.SegmentBurst ?: return@remember null
            val layout = segmentLayouts.firstOrNull { it.segment.id == burst.segmentId } ?: return@remember null
            createBurstParticles(layout, Random(burst.segmentId.hashCode()))
        }
    val density = LocalDensity.current
    val hoverProgress = rememberSegmentHoverProgress(hoveredSegmentId)
    val hoverMarginPx = with(density) { 2.dp.toPx() }
    val hoverBorderPx = with(density) { 3.dp.toPx() }
    val interactionsEnabled = !isLoading && chartAnimation == null

    val centerContentState =
        when {
            isLoading -> CenterContentState.Loading
            segments.isEmpty() -> CenterContentState.Empty
            else -> CenterContentState.Populated
        }
    val showEmptyLabel = centerContentState == CenterContentState.Empty
    val showPopulatedLabel =
        centerContentState == CenterContentState.Populated &&
            (chartAnimation == null || chartAnimation is StorageChartAnimation.SegmentReflow)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val center = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
        val outerRadius = minOf(constraints.maxWidth, constraints.maxHeight) / 2f * 0.92f
        val innerRadius = outerRadius * 0.56f
        val chartDiameterPx = minOf(constraints.maxWidth, constraints.maxHeight)
        val centerLabelScale =
            (
                chartDiameterPx /
                    with(density) { CENTER_LABEL_REFERENCE_DIAMETER_DP.dp.toPx() }
            ).coerceIn(0.52f, 1f)
        val centerLabelMaxWidth = with(density) { (innerRadius * 1.85f).toDp() }

        LaunchedEffect(constraints.maxWidth, constraints.maxHeight, interactionsEnabled) {
            centerHovered = false
            onSegmentHover(null)
        }

        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .pointerInput(
                        segments,
                        totalBytes,
                        interactionsEnabled,
                        constraints.maxWidth,
                        constraints.maxHeight,
                    ) {
                        if (!interactionsEnabled) return@pointerInput
                        val centerX = center.x
                        val centerY = center.y
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val position = event.changes.firstOrNull()?.position ?: continue
                                val (target, segmentId) =
                                    hitTestPieChart(
                                        pointerX = position.x,
                                        pointerY = position.y,
                                        centerX = centerX,
                                        centerY = centerY,
                                        outerRadius = outerRadius,
                                        innerRadius = innerRadius,
                                        segments = segments,
                                    )

                                when (event.type) {
                                    PointerEventType.Move,
                                    PointerEventType.Enter,
                                    -> {
                                        centerHovered = target == PieHitTarget.Center
                                        onSegmentHover(
                                            if (target == PieHitTarget.Segment) segmentId else null,
                                        )
                                    }
                                    PointerEventType.Exit -> {
                                        centerHovered = false
                                        onSegmentHover(null)
                                    }
                                    PointerEventType.Press -> {
                                        when (target) {
                                            PieHitTarget.Center -> onCenterClick()
                                            PieHitTarget.Segment -> segmentId?.let(onSegmentClick)
                                            PieHitTarget.Outside -> Unit
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    },
        ) {
            if (isLoading && loadingPhase != null) {
                drawStoragePieLoading(
                    center = center,
                    outerRadius = outerRadius,
                    innerRadius = innerRadius,
                    phase = loadingPhase,
                )
                drawStorageCenterWell(
                    center = center,
                    innerRadius = innerRadius,
                    rotationDegrees = 0f,
                    intensified = false,
                    subtle = false,
                )
                return@Canvas
            }

            when (val animation = chartAnimation) {
                is StorageChartAnimation.Vortex -> {
                    drawStorageVortex(
                        center = center,
                        innerRadius = innerRadius,
                        outerRadius = outerRadius,
                        layouts = segmentLayouts,
                        progress = animationProgress,
                    )
                }
                is StorageChartAnimation.SegmentReflow -> {
                    val reflowLayouts =
                        buildReflowLayouts(
                            fromSegments = animation.fromSegments,
                            toSegments = animation.toSegments,
                            removedSegmentId = animation.removedSegmentId,
                            progress = animationProgress,
                        )
                    drawStorageSegmentReflow(
                        center = center,
                        outerRadius = outerRadius,
                        layouts = reflowLayouts,
                        hoveredSegmentId = hoveredSegmentId,
                        hoverProgress = hoverProgress,
                        hoverMarginPx = hoverMarginPx,
                        hoverBorderPx = hoverBorderPx,
                        textMeasurer = textMeasurer,
                        innerRadius = innerRadius,
                    )
                }
                else -> {
                    val burstId = (animation as? StorageChartAnimation.SegmentBurst)?.segmentId
                    var startAngle = -90f
                    segments.forEach { segment ->
                        if (segment.id == burstId) {
                            startAngle += segment.shareFraction * 360f
                            return@forEach
                        }
                        val layout = segmentLayouts.firstOrNull { it.segment.id == segment.id } ?: return@forEach
                        val sweep = segment.shareFraction * 360f
                        val isHovered = segment.id == hoveredSegmentId
                        drawPieSegment(
                            center = center,
                            innerRadius = innerRadius,
                            outerRadius = outerRadius,
                            startAngle = startAngle,
                            sweep = sweep,
                            color =
                                if (isHovered) {
                                    segment.color.copy(alpha = 1f)
                                } else {
                                    segment.color.copy(alpha = 0.86f)
                                },
                            segmentId = segment.id,
                            isHovered = isHovered,
                            hoverProgress = hoverProgress,
                            hoverMarginPx = hoverMarginPx,
                            hoverBorderPx = hoverBorderPx,
                        )
                        if (shouldEmbedSegmentLabel(layout)) {
                            drawCurvedSegmentLabel(
                                textMeasurer = textMeasurer,
                                layout = layout,
                                center = center,
                                innerRadius = innerRadius,
                                outerRadius = outerRadius,
                                segmentColor = segment.color,
                            )
                        }
                        startAngle += sweep
                    }

                    if (animation is StorageChartAnimation.SegmentBurst && burstParticles != null) {
                        val layout = segmentLayouts.firstOrNull { it.segment.id == animation.segmentId }
                        if (layout != null) {
                            drawStorageSegmentBurst(
                                center = center,
                                innerRadius = innerRadius,
                                outerRadius = outerRadius,
                                layout = layout,
                                particles = burstParticles,
                                progress = animationProgress,
                                color = layout.segment.color,
                            )
                        }
                    }
                }
            }

            if (!isLoading) {
                val isVortex = chartAnimation is StorageChartAnimation.Vortex
                drawStorageCenterWell(
                    center = center,
                    innerRadius = innerRadius,
                    rotationDegrees = centerPulseRotation,
                    intensified = centerHovered && segments.isNotEmpty() && !isVortex,
                    subtle = segments.isEmpty() || isVortex,
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.Center)) {
            AnimatedVisibility(
                visible = showEmptyLabel,
                enter = centerEmptyLabelEnterTransition,
                exit = centerLabelExitTransition,
            ) {
                ColumnCenterLabel(
                    totalBytes = 0L,
                    installCount = 0,
                    labelScale = centerLabelScale,
                    modifier = Modifier.widthIn(max = centerLabelMaxWidth),
                )
            }
            AnimatedVisibility(
                visible = showPopulatedLabel,
                enter = centerLabelEnterTransition,
                exit = centerLabelExitTransition,
            ) {
                ColumnCenterLabel(
                    totalBytes = totalBytes,
                    installCount = centerInstallCount,
                    labelScale = centerLabelScale,
                    modifier = Modifier.widthIn(max = centerLabelMaxWidth),
                )
            }
        }
    }
}

@Composable
private fun ColumnCenterLabel(
    totalBytes: Long,
    installCount: Int,
    labelScale: Float,
    modifier: Modifier = Modifier,
) {
    val displayStyle =
        LauncherTypography.displayLarge.copy(
            fontSize = (28 * labelScale).sp,
            lineHeight = (32 * labelScale).sp,
            letterSpacing = (3 * labelScale).sp,
        )
    val subStyle =
        LauncherTypography.bodyMedium.copy(
            fontSize = (12 * labelScale).sp,
            lineHeight = (16 * labelScale).sp,
        )
    val emptyStyle =
        LauncherTypography.bodyMedium.copy(
            fontSize = (11 * labelScale).sp,
            lineHeight = (14 * labelScale).sp,
        )

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (installCount == 0) {
            MonoLabel(text = "NO INSTALLS", muted = true, style = emptyStyle)
        } else {
            DisplayTitle(
                text = formatFileSize(totalBytes),
                color = LauncherColors.OnBackground,
                style = displayStyle,
            )
            MonoLabel(
                text = "$installCount ${if (installCount == 1) "BUILD" else "BUILDS"}",
                muted = true,
                style = subStyle,
                modifier = Modifier.padding(top = LauncherSpacing.Xxs),
            )
        }
    }
}

@Composable
private fun rememberChartAnimationProgress(
    animation: StorageChartAnimation?,
    onFinished: () -> Unit,
): Float {
    val progress = remember(animation) { Animatable(0f) }
    var frameProgress by remember(animation) { mutableFloatStateOf(0f) }
    LaunchedEffect(progress) {
        snapshotFlow { progress.value }.collectLatest { frameProgress = it }
    }
    LaunchedEffect(animation, progress) {
        when (animation) {
            is StorageChartAnimation.SegmentBurst -> {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(
                            durationMillis = SEGMENT_BURST_DURATION_MS,
                            easing = FastOutSlowInEasing,
                        ),
                )
                onFinished()
            }
            is StorageChartAnimation.SegmentReflow -> {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(
                            durationMillis = SEGMENT_REFLOW_DURATION_MS,
                            easing = FastOutSlowInEasing,
                        ),
                )
                onFinished()
            }
            StorageChartAnimation.Vortex -> {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(
                            durationMillis = VORTEX_DURATION_MS,
                            easing = FastOutSlowInEasing,
                        ),
                )
                onFinished()
            }
            null -> progress.snapTo(0f)
        }
    }
    return frameProgress
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStorageCenterWell(
    center: Offset,
    innerRadius: Float,
    rotationDegrees: Float,
    intensified: Boolean,
    subtle: Boolean,
) {
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        LauncherColors.Background,
                        LauncherColors.Surface.copy(alpha = 0.92f),
                        LauncherColors.Accent.copy(alpha = 0.08f),
                    ),
                center = center,
                radius = innerRadius,
            ),
        radius = innerRadius,
        center = center,
    )
    drawCircle(
        color = LauncherColors.Accent.copy(alpha = 0.35f),
        radius = innerRadius,
        center = center,
        style = Stroke(width = 2.5f),
    )
    drawCircle(
        color = LauncherColors.Rule.copy(alpha = 0.22f),
        radius = innerRadius * 0.94f,
        center = center,
        style = Stroke(width = 1f),
    )
    drawStorageCenterPulse(
        center = center,
        innerRadius = innerRadius,
        rotationDegrees = rotationDegrees,
        intensified = intensified,
        subtle = subtle,
    )
}
