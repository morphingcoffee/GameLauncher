package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import kotlin.math.sin

@Composable
actual fun ShaderBackground(modifier: Modifier) {
    var time by remember { mutableFloatStateOf(0f) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                time = (frameTime - start) / 1_000_000_000f
            }
        }
    }

    Box(
        modifier =
            modifier.drawBehind {
                val scanlineAlpha = 0.055f
                val dotAlpha = 0.04f
                val lineStep = 3f
                var y = 0f
                while (y < size.height) {
                    val wave = sin((y / size.height + time * 0.04f) * 200f) * 0.5f + 0.5f
                    if (wave > 0.95f) {
                        drawLine(
                            color = Color.White.copy(alpha = scanlineAlpha),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                        )
                    }
                    y += lineStep
                }

                val gridStep = size.width / 80f
                var gx = gridStep / 2f
                while (gx < size.width) {
                    var gy = gridStep / 2f
                    while (gy < size.height) {
                        drawCircle(
                            color = Color.White.copy(alpha = dotAlpha),
                            radius = 0.75f,
                            center = Offset(gx, gy),
                        )
                        gy += gridStep
                    }
                    gx += gridStep
                }
            },
    )
}
