package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing

/**
 * Terminal-style ON/OFF setting row with an explanatory description.
 *
 * Width hugs content (toggle control and description).
 */
@Composable
fun TerminalToggleRow(
    label: String,
    checked: Boolean,
    description: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentAlpha = if (enabled) 1f else 0.45f

    Column(
        modifier = modifier.alpha(contentAlpha),
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Xs),
    ) {
        Row(
            modifier =
                Modifier
                    .hoverable(interactionSource = interactionSource, enabled = enabled)
                    .clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onToggle,
                    ),
            horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoLabel(
                text = label,
                muted = true,
                modifier = Modifier.width(96.dp),
            )
            MonoLabel(text = "·")
            MonoLabel(
                text = if (checked) "[ON]" else "[OFF]",
                accent = checked && enabled,
            )
        }
        MonoLabel(
            text = description,
            muted = true,
        )
    }
}
