package com.morphingcoffee.gamelauncher.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LauncherVersionTest {
    @Test
    fun parse_marketingOnlyVersion_hasNoBuildNumber() {
        val parts = LauncherVersion.parse("0.0.1")

        assertEquals("0.0.1", parts.marketing)
        assertNull(parts.buildNumber)
        assertEquals("0.0.1", LauncherVersion.formatWire(parts))
    }

    @Test
    fun parse_buildVersion_extractsMarketingAndBuild() {
        val parts = LauncherVersion.parse("0.0.1-build66")

        assertEquals("0.0.1", parts.marketing)
        assertEquals(66, parts.buildNumber)
        assertEquals("0.0.1-build66", LauncherVersion.formatWire(parts))
    }
}
