package com.morphingcoffee.gamelauncher.core.designsystem

import androidx.compose.ui.graphics.Color
import coil3.Bitmap

expect fun extractAmbientColor(bitmap: Bitmap): Color

internal fun clampSaturation(
    color: Color,
    maxSaturation: Float,
): Color {
    val red = color.red
    val green = color.green
    val blue = color.blue
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    if (delta <= 0.001f || max <= 0.001f) return color

    val saturation = delta / max
    if (saturation <= maxSaturation) return color

    val luminance = (max + min) / 2f
    val scale = maxSaturation / saturation
    return Color(
        red = (luminance + (red - luminance) * scale).coerceIn(0f, 1f),
        green = (luminance + (green - luminance) * scale).coerceIn(0f, 1f),
        blue = (luminance + (blue - luminance) * scale).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
}

internal fun weightedSampledColor(
    width: Int,
    height: Int,
    samplePixel: (x: Int, y: Int) -> Int,
): Color {
    if (width <= 0 || height <= 0) return Color.Transparent

    var redTotal = 0f
    var greenTotal = 0f
    var blueTotal = 0f
    var weightTotal = 0f

    val gridX = 12
    val gridY = 12
    for (gx in 0 until gridX) {
        for (gy in 0 until gridY) {
            val x = ((gx + 0.5f) / gridX * width).toInt().coerceIn(0, width - 1)
            val y = ((gy + 0.5f) / gridY * height).toInt().coerceIn(0, height - 1)
            val pixel = samplePixel(x, y)
            val red = channelFromPixel(pixel, Channel.Red)
            val green = channelFromPixel(pixel, Channel.Green)
            val blue = channelFromPixel(pixel, Channel.Blue)
            val max = maxOf(red, green, blue)
            val min = minOf(red, green, blue)
            if (max < 0.02f) continue

            val saturation = (max - min) / max.coerceAtLeast(0.001f)
            val weight = saturation.coerceAtLeast(0.08f) * max
            redTotal += red * weight
            greenTotal += green * weight
            blueTotal += blue * weight
            weightTotal += weight
        }
    }

    if (weightTotal <= 0f) return Color.Transparent

    return clampSaturation(
        Color(
            red = redTotal / weightTotal,
            green = greenTotal / weightTotal,
            blue = blueTotal / weightTotal,
        ),
        maxSaturation = 0.85f,
    )
}

private enum class Channel {
    Red,
    Green,
    Blue,
}

private fun channelFromPixel(
    pixel: Int,
    channel: Channel,
): Float =
    when (channel) {
        Channel.Red -> ((pixel shr 16) and 0xFF) / 255f
        Channel.Green -> ((pixel shr 8) and 0xFF) / 255f
        Channel.Blue -> (pixel and 0xFF) / 255f
    }
