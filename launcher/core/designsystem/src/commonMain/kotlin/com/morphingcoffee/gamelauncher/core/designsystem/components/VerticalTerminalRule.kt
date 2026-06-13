package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors

@Composable
fun VerticalTerminalRule(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(LauncherColors.Rule.copy(alpha = LauncherColors.RuleAlpha)),
    )
}
