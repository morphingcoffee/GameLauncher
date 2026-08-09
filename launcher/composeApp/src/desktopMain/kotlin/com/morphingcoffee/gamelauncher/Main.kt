package com.morphingcoffee.gamelauncher

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.SingletonImageLoader
import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.di.appModule
import org.koin.core.context.startKoin

fun main() {
    val isDev = System.getProperty("game.launcher.dev") == "true"

    configureSingletonImageLoader(includeSlowNetwork = isDev)
    installUncaughtExceptionLogger()

    startKoin {
        allowOverride(true)
        modules(if (isDev) listOf(appModule, devModule) else listOf(appModule))
    }

    application {
        // Runtime title-bar / taskbar icon (packaging still uses icons/icon.ico|.icns).
        // Must live on the desktop classpath (desktopMain/resources); native iconFile alone
        // does not replace the JVM window chrome icon on Windows.
        val windowIcon = painterResource("window-icon.png")
        Window(
            state = rememberWindowState(width = 900.dp, height = 620.dp),
            onCloseRequest = ::exitApplication,
            title = if (isDev) "MC.GAME.LAUNCHER [DEV]" else "MC.GAME.LAUNCHER",
            icon = windowIcon,
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
        previous?.uncaughtException(thread, throwable)
    }
}
