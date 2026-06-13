package com.morphingcoffee.gamelauncher

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.morphingcoffee.gamelauncher.di.appModule
import org.koin.core.context.startKoin

fun main() {
    val isDev = System.getProperty("game.launcher.dev") == "true"

    configureSingletonImageLoader(includeSlowNetwork = isDev)

    startKoin {
        allowOverride(true)
        modules(if (isDev) listOf(appModule, devModule) else listOf(appModule))
    }

    application {
        Window(
            state = rememberWindowState(width = 900.dp, height = 580.dp),
            onCloseRequest = ::exitApplication,
            title = if (isDev) "Game Launcher [DEV]" else "Game Launcher",
        ) {
            App()
        }
    }
}

private fun configureSingletonImageLoader(includeSlowNetwork: Boolean) {
    SingletonImageLoader.setSafe { context ->
        createImageLoader(context, includeSlowNetwork)
    }
}

private fun createImageLoader(
    context: PlatformContext,
    includeSlowNetwork: Boolean,
): ImageLoader =
    ImageLoader
        .Builder(context)
        .components {
            if (includeSlowNetwork) {
                add(SlowNetworkImageInterceptor())
            }
            add(KtorNetworkFetcherFactory())
        }.build()
