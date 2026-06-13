package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel
import com.morphingcoffee.gamelauncher.core.designsystem.components.PlatformChips
import com.morphingcoffee.gamelauncher.core.designsystem.components.PlatformChipsMode

@Composable
internal fun RosterItem(
    index: Int,
    title: String,
    isSelected: Boolean,
    isAvailable: Boolean,
    availablePlatformKeys: Set<String>,
    currentPlatformKey: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetAlpha =
        when {
            !isAvailable -> LauncherColors.UnavailableAlpha
            isSelected -> 1f
            else -> LauncherColors.MutedTextAlpha
        }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "roster_item_alpha",
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "roster_indicator_alpha",
    )
    val backgroundColor by animateColorAsState(
        targetValue =
            if (isSelected) {
                LauncherColors.Surface.copy(alpha = 0.9f)
            } else {
                LauncherColors.Background
            },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "roster_item_background",
    )
    val hoverAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.04f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "roster_item_hover",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(LauncherSpacing.RosterItemHeight)
                .alpha(alpha)
                .drawBehind {
                    drawRect(backgroundColor)
                    if (hoverAlpha > 0f) {
                        drawRect(Color.White.copy(alpha = hoverAlpha))
                    }
                }.hoverable(interactionSource = interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(2.dp)
                    .alpha(indicatorAlpha)
                    .background(LauncherColors.Accent),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LauncherSpacing.Md, vertical = LauncherSpacing.Xxs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonoLabel(
                    text = index.toString().padStart(2, '0'),
                    muted = !isSelected,
                )
                MonoLabel(
                    text = title,
                    modifier = Modifier.padding(start = LauncherSpacing.Sm).weight(1f),
                )
                if (isSelected) {
                    MonoLabel(text = "▸", accent = true)
                }
            }
            PlatformChips(
                availablePlatformKeys = availablePlatformKeys,
                currentPlatformKey = currentPlatformKey,
                mode = PlatformChipsMode.Roster,
                modifier = Modifier.padding(top = LauncherSpacing.Xxs),
            )
        }
    }
}
