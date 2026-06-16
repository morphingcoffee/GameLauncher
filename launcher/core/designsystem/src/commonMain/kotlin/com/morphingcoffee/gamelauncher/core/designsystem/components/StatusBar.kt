package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.TerminalRule

data class StatusBarAction(
    val label: String,
    val accent: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
fun StatusBar(
    statusText: String,
    clockText: String,
    downloadProgress: Float? = null,
    actions: List<StatusBarAction> = emptyList(),
    modifier: Modifier = Modifier,
) {
    ColumnWithRule(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(LauncherSpacing.StatusBarHeight)
                    .padding(horizontal = LauncherSpacing.Md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonoLabel(text = statusText, accent = statusText != "READY")
                if (downloadProgress != null) {
                    DownloadProgressBar(progress = downloadProgress)
                    MonoLabel(
                        text = "${(downloadProgress * 100f).toInt()}%",
                        muted = true,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEach { action ->
                    MonoLabel(
                        text = action.label,
                        accent = action.accent,
                        modifier = Modifier.clickable(onClick = action.onClick),
                    )
                }
                MonoLabel(text = clockText, muted = true)
            }
        }
    }
}

@Composable
private fun ColumnWithRule(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        TerminalRule()
        content()
    }
}

@Composable
private fun DownloadProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier =
            modifier
                .height(8.dp)
                .padding(horizontal = LauncherSpacing.Xs),
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            val trackY = size.height / 2f
            drawLine(
                color = LauncherColors.Rule.copy(alpha = 0.15f),
                start = Offset(0f, trackY),
                end = Offset(size.width, trackY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            val progressX = size.width * clamped
            if (progressX > 0f) {
                drawLine(
                    color = LauncherColors.Accent,
                    start = Offset(0f, trackY),
                    end = Offset(progressX, trackY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = LauncherColors.Accent,
                    radius = 3.dp.toPx(),
                    center = Offset(progressX, trackY),
                )
            }
        }
    }
}
