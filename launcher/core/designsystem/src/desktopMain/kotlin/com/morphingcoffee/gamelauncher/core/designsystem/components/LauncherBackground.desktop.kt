package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
) {
    val animated = theme != LauncherBackgroundTheme.STATIC_TERMINAL
    if (!animated) {
        StaticTerminalBackground(modifier = modifier.fillMaxSize())
        return
    }

    val effect =
        remember(theme) {
            compileEffect(theme)
        }
    if (effect == null) {
        StaticTerminalBackground(modifier = modifier.fillMaxSize())
        return
    }

    val probe = LocalLauncherBackgroundPointerProbe.current
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(
                    ShaderBackgroundElement(
                        effect = effect,
                        timeSeconds = timeSeconds,
                        pointerNormalized = pointerNormalized,
                        probe = probe,
                    ),
                ),
    )
}

actual fun Modifier.observeLauncherBackgroundPointer(
    probe: LauncherBackgroundPointerProbe,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    return pointerInput(probe) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                when (event.type) {
                    PointerEventType.Exit -> {
                        probe.inside = false
                    }
                    PointerEventType.Move,
                    PointerEventType.Enter,
                    PointerEventType.Press,
                    -> {
                        val change = event.changes.firstOrNull() ?: continue
                        val pos = change.position
                        val area = size
                        if (area.width > 0 && area.height > 0) {
                            probe.x = (2f * pos.x - area.width) / area.height
                            probe.y = (2f * pos.y - area.height) / area.height
                            probe.inside = true
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}

private data class ShaderBackgroundElement(
    val effect: RuntimeEffect,
    val timeSeconds: Float?,
    val pointerNormalized: Offset?,
    val probe: LauncherBackgroundPointerProbe?,
) : ModifierNodeElement<ShaderBackgroundNode>() {
    override fun create(): ShaderBackgroundNode =
        ShaderBackgroundNode(
            effect = effect,
            timeSeconds = timeSeconds,
            pointerNormalized = pointerNormalized,
            probe = probe,
        )

    override fun update(node: ShaderBackgroundNode) {
        node.update(
            effect = effect,
            timeSeconds = timeSeconds,
            pointerNormalized = pointerNormalized,
            probe = probe,
        )
    }
}

/**
 * Draws the SkSL shader and advances time via [invalidateDraw] — no Compose snapshot state,
 * so sibling navigation/thumbnail UI is never recomposed by the frame clock.
 */
private class ShaderBackgroundNode(
    effect: RuntimeEffect,
    timeSeconds: Float?,
    pointerNormalized: Offset?,
    probe: LauncherBackgroundPointerProbe?,
) : Modifier.Node(),
    DrawModifierNode {
    private var effect: RuntimeEffect = effect
    private var timeSeconds: Float? = timeSeconds
    private var pointerNormalized: Offset? = pointerNormalized
    private var probe: LauncherBackgroundPointerProbe? = probe

    private val paint = Paint()
    private var builder = RuntimeShaderBuilder(effect)
    private var liveTime = 0f
    private var smoothPointer = Offset.Zero
    private var lastFrameNanos = 0L
    private var startNanos = 0L

    fun update(
        effect: RuntimeEffect,
        timeSeconds: Float?,
        pointerNormalized: Offset?,
        probe: LauncherBackgroundPointerProbe?,
    ) {
        val effectChanged = this.effect !== effect
        this.effect = effect
        this.timeSeconds = timeSeconds
        this.pointerNormalized = pointerNormalized
        this.probe = probe
        if (effectChanged) {
            builder = RuntimeShaderBuilder(effect)
        }
        invalidateDraw()
    }

    override fun onAttach() {
        if (timeSeconds != null) return
        startNanos = 0L
        lastFrameNanos = 0L
        coroutineScope.launch {
            while (isActive) {
                withFrameNanos { frameTime ->
                    if (startNanos == 0L) {
                        startNanos = frameTime
                        lastFrameNanos = frameTime
                    }
                    val t = (frameTime - startNanos) / 1_000_000_000f
                    val dt =
                        ((frameTime - lastFrameNanos) / 1_000_000_000f)
                            .coerceIn(0f, 0.1f)
                    lastFrameNanos = frameTime
                    liveTime = t

                    val idle =
                        Offset(
                            x = 0.48f * sin(t * 0.11f),
                            y = 0.30f * cos(t * 0.09f),
                        )
                    val probeSnapshot = probe
                    val external = pointerNormalized
                    val target =
                        when {
                            external != null -> external
                            probeSnapshot != null && probeSnapshot.inside ->
                                Offset(probeSnapshot.x, probeSnapshot.y)
                            else -> idle
                        }
                    val alpha = 1f - exp(-dt / POINTER_SMOOTH_TAU_SECONDS)
                    smoothPointer =
                        Offset(
                            x = smoothPointer.x + (target.x - smoothPointer.x) * alpha,
                            y = smoothPointer.y + (target.y - smoothPointer.y) * alpha,
                        )
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val frozenTime = timeSeconds
        val time = frozenTime ?: liveTime
        val externalPointer = pointerNormalized
        val pointer =
            when {
                frozenTime != null && externalPointer != null -> externalPointer
                frozenTime != null ->
                    Offset(
                        x = 0.48f * sin(time * 0.11f),
                        y = 0.30f * cos(time * 0.09f),
                    )
                else -> smoothPointer
            }
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
        AppLog.w(TAG, "SkSL compile failed for ${theme.id}; falling back to STATIC//TERMINAL", error)
        null
    }
}
