package com.morphingcoffee.gamelauncher.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherBackgroundThemeTest {
    @Test
    fun defaultIsSpectralTopology() {
        assertEquals(LauncherBackgroundTheme.SPECTRAL_TOPOLOGY, LauncherBackgroundTheme.DEFAULT)
        assertEquals("spectral_topology", LauncherBackgroundTheme.DEFAULT.id)
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
        assertEquals(LauncherBackgroundTheme.DEFAULT, LauncherBackgroundTheme.fromId("SPECTRAL_TOPOLOGY"))
    }
}
