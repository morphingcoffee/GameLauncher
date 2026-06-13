package com.morphingcoffee.gamelauncher.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TerminalRule(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier =
            modifier
                .fillMaxWidth()
                .height(1.dp),
        color = LauncherColors.Rule.copy(alpha = LauncherColors.RuleAlpha),
    )
}
