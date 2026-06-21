package com.morphingcoffee.gamelauncher.core.model

object LauncherRuntime {
    fun isDevBuild(): Boolean = System.getProperty("game.launcher.dev") == "true"
}
