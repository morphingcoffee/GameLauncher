package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.formatFileSize

@Composable
fun StoragePieChart(
    segments: List<PieSegment>,
    totalBytes: Long,
    hoveredSegmentId: String?,
    onSegmentHover: (String?) -> Unit,
    onSegmentClick: (String) -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(segments, totalBytes) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val position = event.changes.firstOrNull()?.position ?: continue
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val outerRadius = minOf(size.width, size.height) / 2f * 0.92f
                                val innerRadius = outerRadius * 0.58f
                                val (target, segmentId) =
                                    hitTestPieChart(
                                        pointerX = position.x,
                                        pointerY = position.y,
                                        centerX = center.x,
                                        centerY = center.y,
                                        outerRadius = outerRadius,
                                        innerRadius = innerRadius,
                                        segments = segments,
                                    )

                                when (event.type) {
                                    PointerEventType.Move,
                                    PointerEventType.Enter,
                                    -> {
                                        onSegmentHover(
                                            if (target == PieHitTarget.Segment) segmentId else null,
                                        )
                                    }
                                    PointerEventType.Exit -> onSegmentHover(null)
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
            val chartSize = minOf(size.width, size.height) * 0.92f
            val topLeft = Offset((size.width - chartSize) / 2f, (size.height - chartSize) / 2f)
            val arcSize = Size(chartSize, chartSize)
            val outerRadius = chartSize / 2f
            val innerRadius = outerRadius * 0.58f
            val strokeWidth = 1.5f

            if (segments.isEmpty()) {
                drawArc(
                    color = LauncherColors.Rule.copy(alpha = 0.12f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                return@Canvas
            }

            var startAngle = -90f
            segments.forEach { segment ->
                val sweep = segment.shareFraction * 360f
                val isHovered = segment.id == hoveredSegmentId
                drawArc(
                    color =
                        if (isHovered) {
                            segment.color.copy(alpha = 1f)
                        } else {
                            segment.color.copy(alpha = 0.82f)
                        },
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize,
                )
                drawArc(
                    color = LauncherColors.Background.copy(alpha = 0.35f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                startAngle += sweep
            }

            drawCircle(
                color = LauncherColors.Background,
                radius = innerRadius,
                center = Offset(size.width / 2f, size.height / 2f),
            )
            drawCircle(
                color = LauncherColors.Rule.copy(alpha = 0.15f),
                radius = innerRadius,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = strokeWidth),
            )
        }

        val hoveredSegment = segments.firstOrNull { it.id == hoveredSegmentId }
        if (hoveredSegment != null) {
            MonoLabel(
                text = "${hoveredSegment.label} · ${formatPercent(hoveredSegment.shareFraction)}",
                accent = true,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = LauncherSpacing.Sm),
            )
        }

        ColumnCenterLabel(
            totalBytes = totalBytes,
            installCount = segments.size,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ColumnCenterLabel(
    totalBytes: Long,
    installCount: Int,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (installCount == 0) {
            MonoLabel(text = "NO INSTALLS", muted = true)
        } else {
            DisplayTitle(text = formatFileSize(totalBytes))
            MonoLabel(
                text = "$installCount ${if (installCount == 1) "BUILD" else "BUILDS"}",
                muted = true,
                modifier = Modifier.padding(top = LauncherSpacing.Xs),
            )
        }
    }
}

private fun formatPercent(fraction: Float): String {
    val percent = (fraction * 100f).coerceIn(0f, 100f)
    return "${percent.toOneDecimal()}%"
}

private fun Float.toOneDecimal(): String {
    val scaled = (this * 10f).toInt() / 10f
    return if (scaled % 1f == 0f) {
        scaled.toInt().toString()
    } else {
        scaled.toString()
    }
}
