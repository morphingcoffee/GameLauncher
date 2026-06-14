package com.morphingcoffee.gamelauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.navigation.DebugNavigation
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

@Composable
fun DesktopGlobalShortcuts() {
    if (!LauncherMetadata.DEBUG_TOOLS_ENABLED) return

    DisposableEffect(Unit) {
        val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val dispatcher =
            KeyEventDispatcher { event ->
                if (event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_F12) {
                    DebugNavigation.requestOpenLogs()
                    true
                } else {
                    false
                }
            }
        manager.addKeyEventDispatcher(dispatcher)
        onDispose {
            manager.removeKeyEventDispatcher(dispatcher)
        }
    }
}
