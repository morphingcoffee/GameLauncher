package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

private const val AMBIENT_COLOR_MORPH_DURATION_MILLIS = 900

@Composable
fun Modifier.ambientGlow(color: Color): Modifier {
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec =
            tween(
                durationMillis = AMBIENT_COLOR_MORPH_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        label = "ambient_glow_color",
    )
    val glowColor = remember(animatedColor) { boostForAmbientGlow(animatedColor) }

    return this.drawBehind {
        if (glowColor != Color.Transparent) {
            drawAmbientGlow(glowColor)
        }
    }
}

internal fun DrawScope.drawAmbientGlow(glowColor: Color) {
    drawRect(
        brush =
            Brush.radialGradient(
                colorStops =
                    arrayOf(
                        0f to glowColor.copy(alpha = 0.22f),
                        0.4f to glowColor.copy(alpha = 0.10f),
                        0.7f to glowColor.copy(alpha = 0.04f),
                        1f to Color.Transparent,
                    ),
                center = Offset(size.width * 0.5f, size.height * 0.3f),
                radius = size.width * 0.9f,
            ),
    )
}

private fun boostForAmbientGlow(color: Color): Color {
    if (color == Color.Transparent) return Color.Transparent

    val red = color.red
    val green = color.green
    val blue = color.blue
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val luminance = (max + min) / 2f

    val minLuminance = 0.38f
    val luminanceScale = if (luminance < 0.01f) minLuminance else (minLuminance / luminance).coerceAtMost(2f)
    var boostedRed = (red * luminanceScale).coerceIn(0f, 1f)
    var boostedGreen = (green * luminanceScale).coerceIn(0f, 1f)
    var boostedBlue = (blue * luminanceScale).coerceIn(0f, 1f)

    val boostedMax = maxOf(boostedRed, boostedGreen, boostedBlue)
    val boostedMin = minOf(boostedRed, boostedGreen, boostedBlue)
    if (boostedMax - boostedMin < 0.08f && boostedMax > 0.01f) {
        when {
            red >= green && red >= blue -> boostedRed = (boostedRed * 1.08f).coerceIn(0f, 1f)
            green >= blue -> boostedGreen = (boostedGreen * 1.08f).coerceIn(0f, 1f)
            else -> boostedBlue = (boostedBlue * 1.08f).coerceIn(0f, 1f)
        }
    }

    return Color(red = boostedRed, green = boostedGreen, blue = boostedBlue)
}
