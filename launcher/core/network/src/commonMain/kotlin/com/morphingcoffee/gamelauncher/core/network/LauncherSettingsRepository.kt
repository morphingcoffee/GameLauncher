package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists and exposes launcher preferences (currently background theme).
 *
 * Startup must emit [LauncherBackgroundTheme.DEFAULT] immediately, then update
 * once the on-disk value is loaded off the main thread.
 */
interface LauncherSettingsRepository {
    val backgroundTheme: StateFlow<LauncherBackgroundTheme>

    fun setBackgroundTheme(theme: LauncherBackgroundTheme)
}

expect fun createLauncherSettingsRepository(): LauncherSettingsRepository
