package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTypography

@Composable
fun MonoLabel(
    text: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    accent: Boolean = false,
    style: TextStyle = LauncherTypography.bodyMedium,
    maxLines: Int = 1,
) {
    val color =
        when {
            accent -> LauncherColors.Accent
            muted -> LauncherColors.OnBackground.copy(alpha = LauncherColors.MutedTextAlpha)
            else -> LauncherColors.OnBackground
        }
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun RuleText(
    text: String,
    modifier: Modifier = Modifier,
) {
    MonoLabel(
        text = text.uppercase(),
        modifier = modifier,
        style = LauncherTypography.labelSmall,
    )
}

@Composable
fun DisplayTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LauncherColors.OnBackground,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = LauncherTypography.displayLarge,
        color = color,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
