package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing

@Composable
fun TerminalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    charging: Boolean = false,
    onChargeComplete: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val chargeProgress by animateFloatAsState(
        targetValue = if (charging) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        finishedListener = {
            if (charging && it >= 1f) {
                onChargeComplete?.invoke()
            }
        },
        label = "terminal_button_charge",
    )
    val hoverFillAlpha by animateFloatAsState(
        targetValue = if (isHovered && enabled && !charging) 0.06f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "terminal_button_hover",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .hoverable(interactionSource = interactionSource)
                .clickable(
                    enabled = enabled && !charging,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            val strokeWidth = 1.5.dp.toPx()
            val corner = 4.dp.toPx()
            val perimeter = 2f * (size.width + size.height)
            val phase = chargeProgress * perimeter

            if (hoverFillAlpha > 0f) {
                drawRoundRect(
                    color = LauncherColors.Accent.copy(alpha = hoverFillAlpha),
                    cornerRadius = CornerRadius(corner, corner),
                )
            }

            drawRoundRect(
                color = LauncherColors.Accent.copy(alpha = if (enabled) 1f else 0.3f),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = strokeWidth),
            )

            if (chargeProgress > 0f) {
                drawRoundRect(
                    color = LauncherColors.Accent,
                    cornerRadius = CornerRadius(corner, corner),
                    style =
                        Stroke(
                            width = strokeWidth * 1.5f,
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    floatArrayOf(perimeter * chargeProgress, perimeter),
                                    phase = -phase,
                                ),
                        ),
                )
            }
        }

        MonoLabel(
            text = label,
            accent = enabled,
            modifier = Modifier.padding(horizontal = LauncherSpacing.Md),
        )
    }
}

@Composable
fun PlatformUnavailableBadge(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        MonoLabel(text = "[ PLATFORM UNAVAILABLE ]", accent = true)
    }
}
