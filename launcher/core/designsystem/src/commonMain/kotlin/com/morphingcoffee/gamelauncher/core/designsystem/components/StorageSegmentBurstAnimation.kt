package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal data class BurstParticle(
    val angleDegrees: Float,
    val speed: Float,
    val size: Float,
    val drift: Float,
)

internal fun createBurstParticles(
    layout: SegmentLayout,
    random: Random = Random(0),
): List<BurstParticle> =
    List(28) {
        val spread = layout.sweepAngle * 0.85f
        val angle = layout.midAngle + random.nextFloat() * spread - spread / 2f
        BurstParticle(
            angleDegrees = angle,
            speed = 0.35f + random.nextFloat() * 0.9f,
            size = 1.4f + random.nextFloat() * 3.2f,
            drift = random.nextFloat(),
        )
    }

internal fun DrawScope.drawStorageSegmentBurst(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    layout: SegmentLayout,
    particles: List<BurstParticle>,
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    if (progress <= 0f) return

    val eased = 1f - (1f - progress) * (1f - progress)
    val ringRadius = innerRadius + (outerRadius - innerRadius) * 0.5f
    // Hold particle brightness through the shatter, then fade quickly in the last ~12%.
    val tailStart = 0.88f
    val tailFade =
        if (progress < tailStart) {
            1f
        } else {
            ((1f - progress) / (1f - tailStart)).coerceIn(0f, 1f)
        }

    particles.forEach { particle ->
        val travel = ringRadius + eased * outerRadius * particle.speed
        val radians = Math.toRadians(particle.angleDegrees.toDouble())
        val wobble = sin(Math.toRadians((eased * 240f + particle.drift * 90f).toDouble())).toFloat() * 6f
        val position =
            Offset(
                x = center.x + (cos(radians) * travel).toFloat() + wobble,
                y = center.y + (sin(radians) * travel).toFloat(),
            )
        val alpha =
            (1f - eased * eased).coerceIn(0f, 1f) *
                tailFade *
                (0.35f + particle.drift * 0.65f)
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = particle.size * (1f - eased * 0.35f),
            center = position,
        )
    }
}
