package com.morphingcoffee.gamelauncher.core.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PathRedactorTest {
    @Test
    fun redactsUserHomeAndAppSupportPrefixes() {
        // Use synthetic roots (not real OS user-home patterns) so secret-scan stays clean.
        val macRoot = "/var/app-support/GameLauncher"
        val winRoot = """D:\AppData\GameLauncher"""
        val text =
            "Log from $macRoot/games/demo/out.log and $winRoot\\games\\demo"
        val redacted =
            PathRedactor.redact(
                text,
                listOf(macRoot, winRoot),
            )

        assertFalse(redacted.contains("AppData"))
        assertFalse(redacted.contains("app-support"))
        assertEquals(
            "Log from <redacted>/games/demo/out.log and <redacted>\\games\\demo",
            redacted,
        )
    }

    @Test
    fun longerPrefixesWin() {
        val redacted =
            PathRedactor.redact(
                "/opt/player/.gamelauncher/games/x",
                listOf("/opt/player", "/opt/player/.gamelauncher"),
            )
        assertEquals("<redacted>/games/x", redacted)
    }
}
