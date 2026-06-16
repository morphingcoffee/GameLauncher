package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

internal data class SegmentLayout(
    val segment: PieSegment,
    val startAngle: Float,
    val sweepAngle: Float,
    val midAngle: Float,
)

internal fun buildSegmentLayouts(
    segments: List<PieSegment>,
    startAngleDegrees: Float = -90f,
): List<SegmentLayout> {
    var cursor = startAngleDegrees
    return segments.map { segment ->
        val sweep = segment.shareFraction * 360f
        val layout =
            SegmentLayout(
                segment = segment,
                startAngle = cursor,
                sweepAngle = sweep,
                midAngle = cursor + sweep / 2f,
            )
        cursor += sweep
        layout
    }
}

internal fun shouldEmbedSegmentLabel(layout: SegmentLayout): Boolean =
    layout.sweepAngle >= 28f && layout.segment.shareFraction >= 0.12f

internal fun embeddedSegmentLabel(label: String): String {
    val trimmed = label.trim()
    if (trimmed.length <= 10) return trimmed
    val firstWord = trimmed.substringBefore(' ')
    return if (firstWord.length in 3..12) firstWord else trimmed.take(9) + "…"
}

internal fun polarToCartesian(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
): Offset {
    val radians = Math.toRadians(angleDegrees.toDouble())
    return Offset(
        x = center.x + (cos(radians) * radius).toFloat(),
        y = center.y + (sin(radians) * radius).toFloat(),
    )
}

internal fun calloutAnchor(
    center: Offset,
    midAngle: Float,
    outerRadius: Float,
    padding: Float,
): Offset = polarToCartesian(center, outerRadius + padding, midAngle)
