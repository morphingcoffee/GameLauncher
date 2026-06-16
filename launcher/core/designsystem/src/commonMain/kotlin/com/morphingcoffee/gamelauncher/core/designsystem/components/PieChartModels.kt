package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.ui.graphics.Color
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors

fun pieSegmentColor(index: Int): Color = PIE_SEGMENT_COLORS[index % PIE_SEGMENT_COLORS.size]

private val PIE_SEGMENT_COLORS =
    listOf(
        LauncherColors.Accent,
        LauncherColors.Primary,
        LauncherColors.Secondary,
        Color(0xFF7EE787),
        Color(0xFFFFB86C),
        Color(0xFFD2A8FF),
        Color(0xFFFF7B72),
        Color(0xFF79C0FF),
    )

data class PieSegment(
    val id: String,
    val label: String,
    val sizeBytes: Long,
    val shareFraction: Float,
    val color: Color,
)

internal enum class PieHitTarget {
    Center,
    Segment,
    Outside,
}

internal fun hitTestPieChart(
    pointerX: Float,
    pointerY: Float,
    centerX: Float,
    centerY: Float,
    outerRadius: Float,
    innerRadius: Float,
    segments: List<PieSegment>,
    startAngleDegrees: Float = -90f,
): Pair<PieHitTarget, String?> {
    val dx = pointerX - centerX
    val dy = pointerY - centerY
    val distanceSquared = dx * dx + dy * dy
    if (distanceSquared > outerRadius * outerRadius) {
        return PieHitTarget.Outside to null
    }
    if (distanceSquared <= innerRadius * innerRadius) {
        return PieHitTarget.Center to null
    }

    if (segments.isEmpty()) {
        return PieHitTarget.Outside to null
    }

    var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    angle = (angle - startAngleDegrees + 360f) % 360f

    var cumulative = 0f
    for (segment in segments) {
        val sweep = segment.shareFraction * 360f
        if (angle >= cumulative && angle < cumulative + sweep) {
            return PieHitTarget.Segment to segment.id
        }
        cumulative += sweep
    }

    return PieHitTarget.Segment to segments.last().id
}
