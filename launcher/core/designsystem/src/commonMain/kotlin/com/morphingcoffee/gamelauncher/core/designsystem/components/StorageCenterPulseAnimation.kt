package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import kotlin.math.cos
import kotlin.math.sin

private const val CENTER_PARTICLE_COUNT = 16
private const val ROTATION_CYCLE_MS = 20_000

@Composable
internal fun rememberCenterPulseRotation(enabled: Boolean): Float {
    if (!enabled) return 0f

    val transition = rememberInfiniteTransition(label = "center-pulse")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = ROTATION_CYCLE_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "center-pulse-rotation",
    )
    return rotation
}

internal fun DrawScope.drawStorageCenterPulse(
    center: Offset,
    innerRadius: Float,
    rotationDegrees: Float,
    intensified: Boolean = false,
    subtle: Boolean = false,
) {
    val accentScale = if (subtle) 0.72f else 1f
    val breath = ((sin(Math.toRadians(rotationDegrees.toDouble())) + 1.0) / 2.0).toFloat()
    val breatheRadius = innerRadius * (0.98f + breath * 0.035f)

    drawCircle(
        color = LauncherColors.Accent.copy(alpha = (0.04f + breath * 0.1f) * accentScale),
        radius = breatheRadius,
        center = center,
    )

    drawCircle(
        color = LauncherColors.Accent.copy(alpha = (0.12f + breath * 0.18f) * accentScale),
        radius = innerRadius * (0.88f + breath * 0.04f),
        center = center,
        style = Stroke(width = if (intensified) 1.8f else 1.2f),
    )

    val dashPhase = (rotationDegrees * 1.15f) % 48f
    drawArc(
        color = LauncherColors.Accent.copy(alpha = (0.22f + breath * 0.28f) * accentScale),
        startAngle = rotationDegrees - 90f,
        sweepAngle = 52f,
        useCenter = false,
        topLeft = Offset(center.x - innerRadius * 0.78f, center.y - innerRadius * 0.78f),
        size =
            androidx.compose.ui.geometry.Size(
                innerRadius * 1.56f,
                innerRadius * 1.56f,
            ),
        style =
            Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f), dashPhase),
            ),
    )

    repeat(3) { tick ->
        val tickAngle = rotationDegrees + tick * 120f
        val radians = Math.toRadians(tickAngle.toDouble())
        val outer =
            Offset(
                x = center.x + (cos(radians) * innerRadius * 0.92f).toFloat(),
                y = center.y + (sin(radians) * innerRadius * 0.92f).toFloat(),
            )
        val inner =
            Offset(
                x = center.x + (cos(radians) * innerRadius * 0.78f).toFloat(),
                y = center.y + (sin(radians) * innerRadius * 0.78f).toFloat(),
            )
        drawLine(
            color = LauncherColors.Accent.copy(alpha = (0.25f + breath * 0.45f) * accentScale),
            start = inner,
            end = outer,
            strokeWidth = 1.6f,
        )
    }

    val driftIn = if (intensified) 0.08f else 0.04f
    val twinkleSpeed = if (intensified) 2.4f else 2f
    repeat(CENTER_PARTICLE_COUNT) { index ->
        val seed = index.toFloat() / CENTER_PARTICLE_COUNT
        val orbit = innerRadius * (0.68f + seed * 0.26f)
        val angle = (seed * 360f) + rotationDegrees
        val radians = Math.toRadians(angle.toDouble())
        val orbitScale = 1f - breath * driftIn
        val particleCenter =
            Offset(
                x = center.x + (cos(radians) * orbit * orbitScale).toFloat(),
                y = center.y + (sin(radians) * orbit * orbitScale).toFloat(),
            )
        val twinkle =
            (
                (
                    sin(
                        Math.toRadians((rotationDegrees * twinkleSpeed + seed * 180f).toDouble()),
                    ) + 1.0
                ) / 2.0
            ).toFloat()
        val radius = 1.4f + twinkle * (if (intensified) 2.8f else 2.2f)
        drawCircle(
            color =
                LauncherColors.Accent.copy(
                    alpha = (0.12f + twinkle * (if (intensified) 0.75f else 0.55f)) * accentScale,
                ),
            radius = radius,
            center = particleCenter,
        )
    }
}
