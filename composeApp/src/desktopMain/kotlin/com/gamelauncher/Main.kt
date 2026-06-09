package com.gamelauncher

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.gamelauncher.di.GameLauncherKoinApp
import com.gamelauncher.di.appModule
import org.koin.core.context.startKoin

fun main() {
    startKoin<GameLauncherKoinApp> {
        modules(appModule)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Game Launcher",
        ) {
            App()
        }
    }
}
