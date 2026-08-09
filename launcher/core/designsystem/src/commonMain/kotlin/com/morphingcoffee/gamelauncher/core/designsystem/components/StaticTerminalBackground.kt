package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors

/**
 * Fixed low-contrast phosphor-dot / scanline field. No animation loop.
 * Also used as the soft-fail fallback when SkSL compilation fails.
 */
@Composable
fun StaticTerminalBackground(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier.drawBehind {
                drawRect(LauncherColors.Background)
                val scanlineAlpha = 0.04f
                val dotAlpha = 0.035f
                val lineStep = 3f
                var y = 0f
                while (y < size.height) {
                    if ((y.toInt() / 3) % 7 == 0) {
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
                            radius = 0.7f,
                            center = Offset(gx, gy),
                        )
                        gy += gridStep
                    }
                    gx += gridStep
                }
            },
    )
}
