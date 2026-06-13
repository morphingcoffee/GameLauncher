package com.morphingcoffee.gamelauncher

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.morphingcoffee.gamelauncher.di.appModule
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule)
    }

    application {
        Window(
            state = rememberWindowState(width = 900.dp, height = 580.dp),
            onCloseRequest = ::exitApplication,
            title = "Game Launcher",
        ) {
            App()
        }
    }
}
