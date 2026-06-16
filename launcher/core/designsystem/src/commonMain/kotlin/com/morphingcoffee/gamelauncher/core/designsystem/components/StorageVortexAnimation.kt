package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

internal fun DrawScope.drawStorageVortex(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    layouts: List<SegmentLayout>,
    progress: Float,
) {
    if (progress <= 0f || layouts.isEmpty()) return

    val eased = progress * progress
    val shrink = 1f - eased
    val spin = eased * 140f
    val ringSpan = outerRadius - innerRadius

    layouts.forEach { layout ->
        val topLeft =
            Offset(
                x = center.x - outerRadius * shrink,
                y = center.y - outerRadius * shrink,
            )
        val arcSize = Size(outerRadius * 2f * shrink, outerRadius * 2f * shrink)
        drawArc(
            color = layout.segment.color.copy(alpha = (1f - eased) * 0.75f),
            startAngle = layout.startAngle + spin,
            sweepAngle = layout.sweepAngle * shrink,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize,
        )
    }

    repeat(36) { index ->
        val seed = index / 36f
        val angle = seed * 360f + spin * 2.4f
        val orbit = innerRadius + ringSpan * (1f - eased) * (0.25f + seed * 0.75f)
        val radians = Math.toRadians(angle.toDouble())
        val particle =
            Offset(
                x = center.x + (cos(radians) * orbit).toFloat(),
                y = center.y + (sin(radians) * orbit).toFloat(),
            )
        val layout = layouts[index % layouts.size]
        drawCircle(
            color = layout.segment.color.copy(alpha = (1f - eased) * 0.8f),
            radius = 1.2f + seed * 2.4f,
            center = particle,
        )
    }
}
