package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer

internal data class InterpolatedSegmentLayout(
    val segment: PieSegment,
    val startAngle: Float,
    val sweepAngle: Float,
    val midAngle: Float,
)

internal fun buildReflowLayouts(
    fromSegments: List<PieSegment>,
    toSegments: List<PieSegment>,
    removedSegmentId: String,
    progress: Float,
): List<InterpolatedSegmentLayout> {
    val eased = reflowEase(progress)
    val fromLayouts = buildSegmentLayouts(fromSegments)
    val toLayouts = buildSegmentLayouts(toSegments)
    val fromById = fromLayouts.associateBy { it.segment.id }
    val toById = toLayouts.associateBy { it.segment.id }

    return toSegments.mapNotNull { segment ->
        if (segment.id == removedSegmentId) return@mapNotNull null
        val from = fromById[segment.id] ?: return@mapNotNull null
        val to = toById[segment.id] ?: return@mapNotNull null

        val startAngle = lerpAngle(from.startAngle, to.startAngle, eased)
        val sweepAngle = lerp(from.sweepAngle, to.sweepAngle, eased)
        InterpolatedSegmentLayout(
            segment = segment,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            midAngle = startAngle + sweepAngle / 2f,
        )
    }
}

internal fun DrawScope.drawStorageSegmentReflow(
    center: Offset,
    outerRadius: Float,
    layouts: List<InterpolatedSegmentLayout>,
    hoveredSegmentId: String?,
    hoverProgress: Float,
    hoverMarginPx: Float,
    hoverBorderPx: Float,
    textMeasurer: TextMeasurer,
    innerRadius: Float,
) {
    layouts.forEach { layout ->
        val isHovered = layout.segment.id == hoveredSegmentId
        drawPieSegment(
            center = center,
            innerRadius = innerRadius,
            outerRadius = outerRadius,
            startAngle = layout.startAngle,
            sweep = layout.sweepAngle,
            color =
                if (isHovered) {
                    layout.segment.color.copy(alpha = 1f)
                } else {
                    layout.segment.color.copy(alpha = 0.86f)
                },
            segmentId = layout.segment.id,
            isHovered = isHovered,
            hoverProgress = hoverProgress,
            hoverMarginPx = hoverMarginPx,
            hoverBorderPx = hoverBorderPx,
        )
        val segmentLayout =
            SegmentLayout(
                segment = layout.segment,
                startAngle = layout.startAngle,
                sweepAngle = layout.sweepAngle,
                midAngle = layout.midAngle,
            )
        if (shouldEmbedSegmentLabel(segmentLayout)) {
            drawCurvedSegmentLabel(
                textMeasurer = textMeasurer,
                layout = segmentLayout,
                center = center,
                innerRadius = innerRadius,
                outerRadius = outerRadius,
                segmentColor = layout.segment.color,
            )
        }
    }
}

private fun reflowEase(progress: Float): Float = 1f - (1f - progress) * (1f - progress)

private fun lerp(
    start: Float,
    end: Float,
    fraction: Float,
): Float = start + (end - start) * fraction

private fun lerpAngle(
    start: Float,
    end: Float,
    fraction: Float,
): Float {
    var delta = end - start
    while (delta > 180f) delta -= 360f
    while (delta < -180f) delta += 360f
    return start + delta * fraction
}
