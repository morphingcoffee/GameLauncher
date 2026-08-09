package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme
import kotlinx.coroutines.isActive
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "LauncherBackground"

@Composable
actual fun LauncherBackground(
    theme: LauncherBackgroundTheme,
    modifier: Modifier,
    timeSeconds: Float?,
    pointerNormalized: Offset?,
) {
    val animated = theme != LauncherBackgroundTheme.STATIC_TERMINAL
    var liveTime by remember(theme) { mutableFloatStateOf(0f) }
    var pointerInside by remember(theme) { mutableStateOf(false) }
    var rawPointer by remember(theme) { mutableStateOf(Offset.Zero) }
    var smoothPointer by remember(theme) { mutableStateOf(Offset.Zero) }

    if (animated && timeSeconds == null) {
        LaunchedEffect(theme) {
            val start = withFrameNanos { it }
            while (isActive) {
                withFrameNanos { frameTime ->
                    val t = (frameTime - start) / 1_000_000_000f
                    liveTime = t
                    val idle =
                        Offset(
                            x = 0.48f * sin(t * 0.11f),
                            y = 0.30f * cos(t * 0.09f),
                        )
                    val target = if (pointerInside) rawPointer else idle
                    smoothPointer =
                        Offset(
                            x = smoothPointer.x + (target.x - smoothPointer.x) * 0.12f,
                            y = smoothPointer.y + (target.y - smoothPointer.y) * 0.12f,
                        )
                }
            }
        }
    }

    val time = timeSeconds ?: liveTime
    val pointer =
        pointerNormalized
            ?: if (animated) {
                if (timeSeconds != null) {
                    Offset(
                        x = 0.48f * sin(time * 0.11f),
                        y = 0.30f * cos(time * 0.09f),
                    )
                } else {
                    smoothPointer
                }
            } else {
                Offset.Zero
            }

    if (!animated) {
        StaticTerminalBackground(modifier = modifier)
        return
    }

    val effect =
        remember(theme) {
            compileEffect(theme)
        }

    if (effect == null) {
        StaticTerminalBackground(modifier = modifier)
        return
    }

    val builder = remember(effect) { RuntimeShaderBuilder(effect) }
    val paint = remember { Paint() }

    Box(
        modifier =
            modifier
                .then(
                    if (pointerNormalized == null) {
                        Modifier.pointerInput(theme) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    when (event.type) {
                                        PointerEventType.Exit -> {
                                            pointerInside = false
                                        }
                                        PointerEventType.Move,
                                        PointerEventType.Enter,
                                        PointerEventType.Press,
                                        -> {
                                            val change = event.changes.firstOrNull() ?: continue
                                            val pos = change.position
                                            val area = size
                                            if (area.width > 0 && area.height > 0) {
                                                val nx = (2f * pos.x - area.width) / area.height
                                                val ny = (2f * pos.y - area.height) / area.height
                                                rawPointer = Offset(nx, ny)
                                                pointerInside = true
                                            }
                                        }
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ).drawBehind {
                    try {
                        builder.uniform("uTime", time)
                        builder.uniform("uResolution", size.width, size.height)
                        builder.uniform("uPointer", pointer.x, pointer.y)
                        paint.shader = builder.makeShader()
                        drawContext.canvas.skiaCanvas.drawPaint(paint)
                    } catch (error: Exception) {
                        AppLog.w(TAG, "Background shader render failed; using solid fallback", error)
                        drawRect(LauncherColors.Background)
                    }
                },
    )
}

private fun compileEffect(theme: LauncherBackgroundTheme): RuntimeEffect? {
    val source =
        when (theme) {
            LauncherBackgroundTheme.SPECTRAL_TOPOLOGY -> BackgroundSksl.SPECTRAL_TOPOLOGY
            LauncherBackgroundTheme.BACKPLANE_LIVE -> BackgroundSksl.BACKPLANE_LIVE
            LauncherBackgroundTheme.ISO_LATTICE -> BackgroundSksl.ISO_LATTICE
            LauncherBackgroundTheme.DRAFT_BLUEPRINT -> BackgroundSksl.DRAFT_BLUEPRINT
            LauncherBackgroundTheme.STATIC_TERMINAL -> return null
        }
    return try {
        RuntimeEffect.makeForShader(source)
    } catch (error: Exception) {
        AppLog.w(TAG, "SkSL compile failed for ${theme.id}; falling back to Static Terminal", error)
        null
    }
}
