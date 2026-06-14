package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.TerminalRule
import com.morphingcoffee.gamelauncher.core.model.GameVersionEntry

@Composable
fun VersionSelector(
    selectedVersion: String,
    versions: List<GameVersionEntry>,
    isLoading: Boolean,
    isExpanded: Boolean,
    currentPlatformKey: String?,
    onToggle: () -> Unit,
    onVersionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Xs),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .hoverable(interactionSource = toggleInteractionSource)
                    .clickable(
                        interactionSource = toggleInteractionSource,
                        indication = null,
                        onClick = onToggle,
                    ),
            horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoLabel(
                text = "VERSION",
                muted = true,
                modifier = Modifier.width(72.dp),
            )
            MonoLabel(text = "·")
            MonoLabel(text = selectedVersion)
            MonoLabel(
                text = if (isExpanded) "[▾]" else "[▸]",
                accent = true,
            )
        }

        if (isExpanded) {
            TerminalRule(modifier = Modifier.padding(vertical = LauncherSpacing.Xs))

            when {
                isLoading -> {
                    MonoLabel(text = "LOADING VERSIONS...", accent = true)
                }

                versions.isEmpty() -> {
                    MonoLabel(text = "NO VERSIONS", muted = true)
                }

                else -> {
                    versions.forEach { versionEntry ->
                        VersionRow(
                            versionEntry = versionEntry,
                            isSelected = versionEntry.version == selectedVersion,
                            currentPlatformKey = currentPlatformKey,
                            onVersionSelected = onVersionSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionRow(
    versionEntry: GameVersionEntry,
    isSelected: Boolean,
    currentPlatformKey: String?,
    onVersionSelected: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .hoverable(interactionSource = interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onVersionSelected(versionEntry.version) },
                ).padding(vertical = LauncherSpacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoLabel(
            text = if (isSelected) "[X]" else "[ ]",
            accent = isSelected,
            modifier = Modifier.width(28.dp),
        )
        MonoLabel(text = versionEntry.version)
        MonoLabel(text = versionEntry.releasedAt, muted = true)
        PlatformChips(
            availablePlatformKeys = versionEntry.builds.keys,
            currentPlatformKey = currentPlatformKey,
        )
    }
}
