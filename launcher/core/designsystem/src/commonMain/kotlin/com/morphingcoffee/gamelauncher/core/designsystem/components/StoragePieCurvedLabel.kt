package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** Outer radius of a ~280dp storage pie — used to scale curved labels down on smaller charts. */
private const val CURVED_LABEL_REFERENCE_OUTER_RADIUS = 128f

/** Bottom of the pie in Compose arc degrees (6 o'clock). */
private const val FULL_RING_LABEL_MID_ANGLE = 90f

internal fun DrawScope.drawCurvedSegmentLabel(
    textMeasurer: TextMeasurer,
    layout: SegmentLayout,
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    segmentColor: Color,
) {
    val label = curvedSegmentLabel(layout.segment.label, outerRadius)
    if (label.isEmpty()) return

    val scale = (outerRadius / CURVED_LABEL_REFERENCE_OUTER_RADIUS).coerceIn(0.55f, 1f)
    val labelRadius = innerRadius + (outerRadius - innerRadius) * 0.62f
    val charStyle =
        TextStyle(
            color = Color.White.copy(alpha = 0.94f),
            fontFamily = FontFamily.Monospace,
            fontSize = (9f * scale).sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (1.4f * scale).sp,
            shadow =
                Shadow(
                    color = segmentColor.copy(alpha = 0.75f),
                    blurRadius = 6f * scale,
                ),
        )

    val chars = label.toList()
    val degreesPerChar = 5.8f * scale
    val isFullRing = layout.sweepAngle >= 359.5f
    val totalSpan =
        if (isFullRing) {
            min(96f, chars.size * degreesPerChar)
        } else {
            min(layout.sweepAngle * 0.82f, chars.size * degreesPerChar)
        }
    if (totalSpan < degreesPerChar) return

    val midAngle = if (isFullRing) FULL_RING_LABEL_MID_ANGLE else layout.midAngle
    val startAngle = midAngle - totalSpan / 2f + degreesPerChar / 2f

    chars.forEachIndexed { index, char ->
        val angle = startAngle + index * (totalSpan / chars.size.coerceAtLeast(1))
        val anchor = polarToCartesian(center, labelRadius, angle)
        val charLayout =
            textMeasurer.measure(
                text = AnnotatedString(char.toString()),
                style = charStyle,
            )
        rotate(degrees = curvedCharRotationDegrees(angle), pivot = anchor) {
            drawText(
                textLayoutResult = charLayout,
                topLeft =
                    Offset(
                        x = anchor.x - charLayout.size.width / 2f,
                        y = anchor.y - charLayout.size.height / 2f,
                    ),
            )
        }
    }
}

/** Keep characters upright on the bottom half of the ring. */
private fun curvedCharRotationDegrees(angleDegrees: Float): Float {
    var rotation = angleDegrees + 90f
    if (rotation > 90f && rotation < 270f) {
        rotation += 180f
    }
    return rotation
}

internal fun curvedSegmentLabel(
    label: String,
    outerRadius: Float = CURVED_LABEL_REFERENCE_OUTER_RADIUS,
): String {
    val scale = (outerRadius / CURVED_LABEL_REFERENCE_OUTER_RADIUS).coerceIn(0.55f, 1f)
    val maxChars =
        when {
            scale < 0.65f -> 8
            scale < 0.8f -> 10
            else -> 14
        }
    val trimmed = label.trim().uppercase()
    if (trimmed.length <= maxChars) return trimmed
    val firstWord = trimmed.substringBefore(' ')
    return if (firstWord.length in 3..maxChars) firstWord else trimmed.take(maxChars - 1) + "…"
}
