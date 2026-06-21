package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.formatFileSize
import com.morphingcoffee.gamelauncher.core.designsystem.formatLauncherVersionDelta

@Composable
fun LauncherUpdateDetails(
    currentVersion: String,
    latestVersion: String,
    channelKey: String?,
    fileSizeBytes: Long?,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
    ) {
        MonoLabel(text = formatLauncherVersionDelta(currentVersion, latestVersion))

        val metadata =
            buildList {
                channelKey?.let { add(it) }
                fileSizeBytes?.takeIf { it > 0L }?.let { add(formatFileSize(it)) }
            }.joinToString(" · ")

        if (metadata.isNotBlank()) {
            MonoLabel(text = metadata, muted = true)
        }

        errorMessage?.let { message ->
            MonoLabel(text = message, accent = true)
        }
    }
}

@Composable
fun LauncherUpdateInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoLabel(
            text = label,
            muted = true,
            modifier = Modifier.width(72.dp),
        )
        MonoLabel(text = "·")
        MonoLabel(text = value)
    }
}
