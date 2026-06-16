package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import kotlin.math.PI
import kotlin.random.Random

@Composable
internal fun rememberSegmentHoverProgress(hoveredSegmentId: String?): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(hoveredSegmentId) {
        if (hoveredSegmentId != null) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing),
            )
        }
    }
    return progress.value
}

internal fun marginDegreesForPx(
    marginPx: Float,
    outerRadius: Float,
): Float = (marginPx / outerRadius) * (180f / PI.toFloat())

private const val SEGMENT_GRAIN_COUNT = 18

internal fun DrawScope.drawPieSegment(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweep: Float,
    color: Color,
    segmentId: String,
    isHovered: Boolean = false,
    hoverProgress: Float = 0f,
    hoverMarginPx: Float = 0f,
    hoverBorderPx: Float = 0f,
) {
    val appliedProgress = if (isHovered) hoverProgress else 0f
    val isFullRing = sweep >= 359.5f
    val marginDegrees =
        if (!isFullRing && appliedProgress > 0f && hoverMarginPx > 0f) {
            marginDegreesForPx(hoverMarginPx, outerRadius) * appliedProgress
        } else {
            0f
        }
    val effectiveStart = startAngle + marginDegrees
    val effectiveSweep = (sweep - marginDegrees * 2f).coerceAtLeast(0.5f)
    val topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
    val arcSize = Size(outerRadius * 2f, outerRadius * 2f)

    drawArc(
        brush =
            segmentRadialBrush(
                color = color,
                pieCenter = center,
                innerRadius = innerRadius,
                outerRadius = outerRadius,
                isHovered = isHovered,
            ),
        startAngle = effectiveStart,
        sweepAngle = effectiveSweep,
        useCenter = true,
        topLeft = topLeft,
        size = arcSize,
    )
    drawSegmentGrain(
        center = center,
        innerRadius = innerRadius,
        outerRadius = outerRadius,
        startAngle = effectiveStart,
        sweep = effectiveSweep,
        segmentId = segmentId,
        color = color,
        isHovered = isHovered,
    )
    drawArc(
        color = LauncherColors.Background.copy(alpha = if (isHovered) 0.18f else 0.28f),
        startAngle = effectiveStart,
        sweepAngle = effectiveSweep,
        useCenter = true,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = 1.25f),
    )

    if (appliedProgress > 0f && hoverBorderPx > 0f) {
        val borderWidth = hoverBorderPx * appliedProgress
        val borderColor = LauncherColors.OnBackground.copy(alpha = 0.94f * appliedProgress)
        drawArc(
            color = borderColor,
            startAngle = effectiveStart,
            sweepAngle = if (isFullRing) 360f else effectiveSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = borderWidth),
        )
        if (!isFullRing) {
            val startInner = polarToCartesian(center, innerRadius, effectiveStart)
            val startOuter = polarToCartesian(center, outerRadius, effectiveStart)
            val endAngle = effectiveStart + effectiveSweep
            val endInner = polarToCartesian(center, innerRadius, endAngle)
            val endOuter = polarToCartesian(center, outerRadius, endAngle)
            drawLine(
                color = borderColor,
                start = startInner,
                end = startOuter,
                strokeWidth = borderWidth,
            )
            drawLine(
                color = borderColor,
                start = endInner,
                end = endOuter,
                strokeWidth = borderWidth,
            )
        }
    }
}

private fun segmentRadialBrush(
    color: Color,
    pieCenter: Offset,
    innerRadius: Float,
    outerRadius: Float,
    isHovered: Boolean,
): Brush {
    val baseAlpha = color.alpha
    val lift = if (isHovered) 0.06f else 0f
    val highlight = brightenSegmentColor(color, isHovered)
    val shadow = deepenSegmentColor(color)
    val innerStop = (innerRadius / outerRadius).coerceIn(0.08f, 0.72f)
    return Brush.radialGradient(
        colorStops =
            arrayOf(
                0f to shadow.copy(alpha = baseAlpha * (0.62f + lift)),
                innerStop to color.copy(alpha = baseAlpha * (0.84f + lift)),
                0.92f to color.copy(alpha = baseAlpha * (0.96f + lift)),
                1f to highlight.copy(alpha = baseAlpha * (0.88f + lift)),
            ),
        center = pieCenter,
        radius = outerRadius.coerceAtLeast(1f),
    )
}

private fun brightenSegmentColor(
    color: Color,
    isHovered: Boolean,
): Color {
    val mix = if (isHovered) 0.28f else 0.22f
    return Color(
        red = (color.red + (1f - color.red) * mix).coerceIn(0f, 1f),
        green = (color.green + (1f - color.green) * mix).coerceIn(0f, 1f),
        blue = (color.blue + (1f - color.blue) * mix).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
}

private fun deepenSegmentColor(color: Color): Color {
    val mix = 0.18f
    return Color(
        red = (color.red * (1f - mix)).coerceIn(0f, 1f),
        green = (color.green * (1f - mix)).coerceIn(0f, 1f),
        blue = (color.blue * (1f - mix)).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
}

private fun DrawScope.drawSegmentGrain(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweep: Float,
    segmentId: String,
    color: Color,
    isHovered: Boolean,
) {
    if (sweep < 4f) return

    val random = Random(segmentId.hashCode())
    val ringSpan = (outerRadius - innerRadius).coerceAtLeast(1f)
    val intensity = if (isHovered) 1.15f else 1f

    repeat(SEGMENT_GRAIN_COUNT) {
        val angle = startAngle + sweep * (0.06f + random.nextFloat() * 0.88f)
        val radius = innerRadius + ringSpan * (0.12f + random.nextFloat() * 0.86f)
        val position = polarToCartesian(center, radius, angle)
        val twinkle = 0.35f + random.nextFloat() * 0.65f
        val dotRadius = 0.7f + random.nextFloat() * 1.5f
        val useHighlight = random.nextFloat() > 0.38f
        val grainAlpha = (0.04f + random.nextFloat() * 0.08f) * twinkle * intensity
        drawCircle(
            color =
                if (useHighlight) {
                    LauncherColors.OnBackground.copy(alpha = grainAlpha)
                } else {
                    deepenSegmentColor(color).copy(alpha = grainAlpha * 1.35f)
                },
            radius = dotRadius,
            center = position,
        )
    }
}
