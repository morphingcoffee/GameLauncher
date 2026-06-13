package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing

@Composable
fun TerminalLinkRow(
    label: String,
    linkText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoLabel(
            text = label,
            muted = true,
            modifier = Modifier.width(72.dp),
        )
        MonoLabel(text = "·")
        MonoLabel(
            text = linkText,
            accent = true,
            modifier =
                Modifier
                    .hoverable(interactionSource = interactionSource)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    ),
        )
    }
}
