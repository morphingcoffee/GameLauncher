package com.morphingcoffee.gamelauncher.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.components.MonoLabel
import com.morphingcoffee.gamelauncher.core.designsystem.components.PlatformChips
import com.morphingcoffee.gamelauncher.core.model.GameBuild

@Composable
internal fun MetadataTable(
    currentPlatformKey: String?,
    currentPlatformBuild: GameBuild?,
    availableBuilds: Map<String, GameBuild>,
    onDiskSizeBytes: Long? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Xs),
    ) {
        MetadataRow(
            label = "SIZE",
            value =
                formatSizeDisplay(
                    downloadSizeBytes = currentPlatformBuild?.fileSizeBytes,
                    onDiskSizeBytes = onDiskSizeBytes,
                ),
        )
        MetadataRow(
            label = "PLATFORM",
            value = formatPlatformDisplayName(currentPlatformKey),
        )
        MetadataRow(
            label = "BUILDS",
            trailing = {
                PlatformChips(
                    availablePlatformKeys = availableBuilds.keys,
                    currentPlatformKey = currentPlatformKey,
                )
            },
        )
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String = "",
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        MonoLabel(
            text = label,
            muted = true,
            modifier = Modifier.width(72.dp),
        )
        if (trailing != null) {
            trailing()
        } else {
            MonoLabel(text = "·")
            MonoLabel(text = value)
        }
    }
}
