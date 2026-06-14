package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherTypography
import com.morphingcoffee.gamelauncher.core.model.PlatformKey

enum class PlatformChipsMode {
    /** Detail/metadata: accent only the current platform when a build exists. */
    Detail,

    /** Roster: show every available build clearly; accent only if playable on this machine. */
    Roster,
}

private val chipShape = RoundedCornerShape(2.dp)

@Composable
fun PlatformChips(
    availablePlatformKeys: Set<String>,
    currentPlatformKey: String?,
    modifier: Modifier = Modifier,
    mode: PlatformChipsMode = PlatformChipsMode.Detail,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlatformKey.all.forEach { platformKey ->
            val isAvailable = platformKey in availablePlatformKeys
            val isCurrent = platformKey == currentPlatformKey
            PlatformChip(
                label = platformAbbreviation(platformKey),
                isAvailable = isAvailable,
                isCurrent = isCurrent && isAvailable,
                mode = mode,
            )
        }
    }
}

@Composable
private fun PlatformChip(
    label: String,
    isAvailable: Boolean,
    isCurrent: Boolean,
    mode: PlatformChipsMode,
) {
    val alpha = if (isAvailable) 1f else 0.2f
    val borderColor =
        when (mode) {
            PlatformChipsMode.Detail ->
                when {
                    isCurrent -> LauncherColors.Accent
                    isAvailable -> LauncherColors.Rule.copy(alpha = 0.25f)
                    else -> LauncherColors.Rule.copy(alpha = 0.1f)
                }

            PlatformChipsMode.Roster ->
                when {
                    isCurrent -> LauncherColors.Accent
                    isAvailable -> LauncherColors.OnBackground.copy(alpha = 0.45f)
                    else -> LauncherColors.Rule.copy(alpha = 0.1f)
                }
        }
    val accentText = isCurrent
    val mutedText =
        when {
            !isAvailable -> true
            isCurrent -> false
            mode == PlatformChipsMode.Roster -> false
            else -> true
        }

    Box(
        modifier =
            Modifier
                .alpha(alpha)
                .height(LauncherSpacing.PlatformChipHeight)
                .border(width = 1.dp, color = borderColor, shape = chipShape)
                .padding(horizontal = LauncherSpacing.Xs),
        contentAlignment = Alignment.Center,
    ) {
        MonoLabel(
            text = label,
            accent = accentText,
            muted = mutedText,
            style = LauncherTypography.labelSmall,
        )
    }
}

fun platformAbbreviation(platformKey: String): String =
    when (platformKey) {
        PlatformKey.WINDOWS_X64 -> "WIN"
        PlatformKey.MACOS_ARM64 -> "ARM"
        PlatformKey.MACOS_X64 -> "X64"
        PlatformKey.WEB -> "WEB"
        else -> platformKey.uppercase()
    }
