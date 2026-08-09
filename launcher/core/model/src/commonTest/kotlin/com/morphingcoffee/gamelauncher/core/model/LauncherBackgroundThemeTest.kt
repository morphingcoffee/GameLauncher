package com.morphingcoffee.gamelauncher.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherBackgroundThemeTest {
    @Test
    fun defaultIsStaticTerminalAndFirstInList() {
        assertEquals(LauncherBackgroundTheme.STATIC_TERMINAL, LauncherBackgroundTheme.DEFAULT)
        assertEquals("static_terminal", LauncherBackgroundTheme.DEFAULT.id)
        assertEquals(LauncherBackgroundTheme.STATIC_TERMINAL, LauncherBackgroundTheme.entries.first())
    }

    @Test
    fun fromId_resolvesStableIds() {
        for (theme in LauncherBackgroundTheme.entries) {
            assertEquals(theme, LauncherBackgroundTheme.fromId(theme.id))
        }
    }

    @Test
    fun fromId_unknownOrNull_fallsBackToDefault() {
        assertEquals(LauncherBackgroundTheme.DEFAULT, LauncherBackgroundTheme.fromId(null))
        assertEquals(LauncherBackgroundTheme.DEFAULT, LauncherBackgroundTheme.fromId(""))
        assertEquals(LauncherBackgroundTheme.DEFAULT, LauncherBackgroundTheme.fromId("Spectral Topology"))
        assertEquals(LauncherBackgroundTheme.DEFAULT, LauncherBackgroundTheme.fromId("STATIC_TERMINAL"))
    }
}
