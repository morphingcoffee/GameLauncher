package com.morphingcoffee.gamelauncher.core.network

actual fun createLauncherSettingsRepository(): LauncherSettingsRepository = InMemoryLauncherSettingsRepository()
