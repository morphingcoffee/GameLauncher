package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing

/**
 * Terminal-style selectable option row (`[X]` / `[ ]`) for settings lists.
 *
 * Width hugs content. Built on [clickable], so it participates in keyboard focus
 * and activates with Enter/Space.
 */
@Composable
fun TerminalOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier =
            modifier
                .hoverable(interactionSource = interactionSource, enabled = enabled)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ).padding(vertical = LauncherSpacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoLabel(
            text = if (selected) "[X]" else "[ ]",
            accent = selected && enabled,
            modifier = Modifier.width(28.dp),
        )
        MonoLabel(
            text = label,
            accent = selected && enabled,
        )
    }
}
