package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import kotlin.math.min

internal data class PieLoadingPhase(
    val reveal: Float,
    val pulse: Float,
    val rotation: Float,
)

@Composable
internal fun rememberPieLoadingPhase(isLoading: Boolean): PieLoadingPhase? {
    if (!isLoading) return null

    val transition = rememberInfiniteTransition(label = "pie-loading")
    val reveal by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1_400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pie-loading-reveal",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pie-loading-pulse",
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2_400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pie-loading-rotation",
    )

    return PieLoadingPhase(
        reveal = reveal,
        pulse = pulse,
        rotation = rotation,
    )
}

internal fun DrawScope.drawStoragePieLoading(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    phase: PieLoadingPhase,
) {
    val sweep = 300f * phase.reveal
    val topLeft =
        Offset(
            x = center.x - outerRadius,
            y = center.y - outerRadius,
        )
    val arcSize = Size(outerRadius * 2f, outerRadius * 2f)
    val accentAlpha = 0.25f + (phase.pulse * 0.55f)

    drawArc(
        color = LauncherColors.Accent.copy(alpha = accentAlpha * 0.35f),
        startAngle = phase.rotation - 90f,
        sweepAngle = sweep,
        useCenter = true,
        topLeft = topLeft,
        size = arcSize,
    )
    drawArc(
        color = LauncherColors.Accent.copy(alpha = accentAlpha),
        startAngle = phase.rotation - 90f,
        sweepAngle = min(sweep, 120f),
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round),
    )
    drawCircle(
        color = LauncherColors.Rule.copy(alpha = 0.08f + phase.pulse * 0.12f),
        radius = innerRadius,
        center = center,
        style = Stroke(width = 2f),
    )
    drawCircle(
        color = LauncherColors.Accent.copy(alpha = 0.12f + phase.pulse * 0.2f),
        radius = innerRadius * (0.92f + phase.pulse * 0.06f),
        center = center,
        style = Stroke(width = 1.25f),
    )
}
