package com.morphingcoffee.gamelauncher

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.SingletonImageLoader
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.telemetry.CrashReporting
import com.morphingcoffee.gamelauncher.core.telemetry.TelemetryBootstrap
import com.morphingcoffee.gamelauncher.di.appModule
import org.koin.core.context.startKoin

fun main() {
    val isDev = System.getProperty("game.launcher.dev") == "true"

    // Privacy prefs + Sentry must initialize before Koin/Compose.
    TelemetryBootstrap.initialize(isDevBuild = isDev)

    configureSingletonImageLoader(includeSlowNetwork = isDev)
    installUncaughtExceptionLogger()

    startKoin {
        allowOverride(true)
        modules(if (isDev) listOf(appModule, devModule) else listOf(appModule))
    }

    application {
        Window(
            state = rememberWindowState(width = 900.dp, height = 620.dp),
            onCloseRequest = {
                CrashReporting.flush()
                exitApplication()
            },
            title = if (isDev) "MC.GAME.LAUNCHER [DEV]" else "MC.GAME.LAUNCHER",
        ) {
            DesktopGlobalShortcuts()
            App()
        }
    }
}

private fun configureSingletonImageLoader(includeSlowNetwork: Boolean) {
    SingletonImageLoader.setSafe { context ->
        createImageLoader(context, includeSlowNetwork)
    }
}

private fun installUncaughtExceptionLogger() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        AppLog.e("Uncaught", "Thread ${thread.name} crashed", throwable)
        CrashReporting.captureUncaught(thread, throwable)
        previous?.uncaughtException(thread, throwable)
    }
}
