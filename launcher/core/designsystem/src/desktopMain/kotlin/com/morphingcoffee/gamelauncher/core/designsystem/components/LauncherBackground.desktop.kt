package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import kotlin.math.exp
import kotlin.math.sin

private const val TAG = "LauncherBackground"

/** Time constant for pointer smoothing / idle return (~1 second settle). */
private const val POINTER_SMOOTH_TAU_SECONDS = 1.0f

@Composable
actual fun LauncherBackground(
    theme: LauncherBackgroundTheme,
    modifier: Modifier,
    timeSeconds: Float?,
    pointerNormalized: Offset?,
    content: @Composable () -> Unit,
) {
    val animated = theme != LauncherBackgroundTheme.STATIC_TERMINAL
    var liveTime by remember(theme) { mutableFloatStateOf(0f) }
    var pointerInside by remember(theme) { mutableStateOf(false) }
    var rawPointer by remember(theme) { mutableStateOf(Offset.Zero) }
    var smoothPointer by remember(theme) { mutableStateOf(Offset.Zero) }

    val externallyDriven = pointerNormalized != null

    if (animated && timeSeconds == null) {
        LaunchedEffect(theme) {
            val start = withFrameNanos { it }
            var lastFrame = start
            while (isActive) {
                withFrameNanos { frameTime ->
                    val t = (frameTime - start) / 1_000_000_000f
                    val dt =
                        ((frameTime - lastFrame) / 1_000_000_000f)
                            .coerceIn(0f, 0.1f)
                    lastFrame = frameTime
                    liveTime = t

                    val idle =
                        Offset(
                            x = 0.48f * sin(t * 0.11f),
                            y = 0.30f * cos(t * 0.09f),
                        )
                    val external = pointerNormalized
                    val target =
                        when {
                            external != null -> external
                            pointerInside -> rawPointer
                            else -> idle
                        }
                    val alpha = 1f - exp(-dt / POINTER_SMOOTH_TAU_SECONDS)
                    smoothPointer =
                        Offset(
                            x = smoothPointer.x + (target.x - smoothPointer.x) * alpha,
                            y = smoothPointer.y + (target.y - smoothPointer.y) * alpha,
                        )
                }
            }
        }
    }

    val time = timeSeconds ?: liveTime
    val pointer =
        when {
            !animated -> Offset.Zero
            timeSeconds != null && pointerNormalized != null -> pointerNormalized
            timeSeconds != null ->
                Offset(
                    x = 0.48f * sin(time * 0.11f),
                    y = 0.30f * cos(time * 0.09f),
                )
            else -> smoothPointer
        }

    val effect =
        remember(theme) {
            if (animated) compileEffect(theme) else null
        }
    val useShader = animated && effect != null
    val builder = remember(effect) { effect?.let { RuntimeShaderBuilder(it) } }
    val paint = remember { Paint() }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(
                    if (!externallyDriven && animated) {
                        // Host wraps [content], so moves over UI still count as inside.
                        // Exit fires only when the cursor leaves this host (window content).
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
                ).then(
                    if (useShader && builder != null) {
                        Modifier.drawBehind {
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
                        }
                    } else {
                        Modifier
                    },
                ),
    ) {
        if (!useShader) {
            StaticTerminalBackground(modifier = Modifier.fillMaxSize())
        }
        content()
    }
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
