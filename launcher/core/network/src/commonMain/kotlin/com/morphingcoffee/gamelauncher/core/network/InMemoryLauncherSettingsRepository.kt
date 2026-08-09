package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory settings store for tests and non-desktop targets. */
class InMemoryLauncherSettingsRepository(
    initialTheme: LauncherBackgroundTheme = LauncherBackgroundTheme.DEFAULT,
) : LauncherSettingsRepository {
    private val _backgroundTheme = MutableStateFlow(initialTheme)
    override val backgroundTheme: StateFlow<LauncherBackgroundTheme> = _backgroundTheme.asStateFlow()

    override fun setBackgroundTheme(theme: LauncherBackgroundTheme) {
        _backgroundTheme.value = theme
    }
}
