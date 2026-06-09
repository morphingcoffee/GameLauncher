package com.morphingcoffee.gamelauncher

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.morphingcoffee.gamelauncher.di.appModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main() {
    startKoin {
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
