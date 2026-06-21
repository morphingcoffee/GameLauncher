package com.morphingcoffee.gamelauncher.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LauncherVersionDisplayTest {
    @Test
    fun formatLauncherHeaderVersion_includesBuildWhenPresent() {
        val labels = formatLauncherHeaderVersion("0.0.1-build65")

        assertEquals("v0.0.1", labels.marketingLabel)
        assertEquals("build65", labels.buildLabel)
    }

    @Test
    fun formatLauncherHeaderVersion_omitsBuildForMarketingOnly() {
        val labels = formatLauncherHeaderVersion("0.0.1")

        assertEquals("v0.0.1", labels.marketingLabel)
        assertNull(labels.buildLabel)
    }

    @Test
    fun formatLauncherUpdateSignalLabel_showsFullTargetVersion() {
        assertEquals(
            "LAUNCHER ↑ v0.0.1-build66",
            formatLauncherUpdateSignalLabel(
                currentVersion = "0.0.1",
                latestVersion = "0.0.1-build66",
                isHovered = false,
            ),
        )
    }

    @Test
    fun formatLauncherUpdateSignalLabel_hoverShowsBuildDeltaWhenBothExplicit() {
        assertEquals(
            "[ LAUNCHER ↑ build65 → build66 ]",
            formatLauncherUpdateSignalLabel(
                currentVersion = "0.0.1-build65",
                latestVersion = "0.0.1-build66",
                isHovered = true,
            ),
        )
    }

    @Test
    fun formatLauncherVersionDelta_usesWireFormat() {
        assertEquals(
            "0.0.1 → 0.0.1-build66",
            formatLauncherVersionDelta(
                currentVersion = "0.0.1",
                latestVersion = "0.0.1-build66",
            ),
        )
        assertEquals(
            "0.0.1-build65 → 0.0.1-build66",
            formatLauncherVersionDelta(
                currentVersion = "0.0.1-build65",
                latestVersion = "0.0.1-build66",
            ),
        )
    }
}
