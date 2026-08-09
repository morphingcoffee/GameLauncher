package com.morphingcoffee.gamelauncher.core.telemetry

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelemetryPreferencesTest {
    @Test
    fun defaults_crashReportsOnExtendedOff() {
        val prefs = TelemetryPreferences.DEFAULT
        assertTrue(prefs.sendCrashReports)
        assertFalse(prefs.shareExtendedDiagnostics)
        assertFalse(prefs.effectiveShareExtendedDiagnostics)
    }

    @Test
    fun extendedDiagnosticsRequiresCrashReports() {
        val prefs =
            TelemetryPreferences(
                sendCrashReports = false,
                shareExtendedDiagnostics = true,
            )
        assertFalse(prefs.effectiveShareExtendedDiagnostics)
    }

    @Test
    fun inMemoryStore_roundTrips() {
        val store = InMemoryTelemetryPreferencesStore()
        val next =
            TelemetryPreferences(
                sendCrashReports = false,
                shareExtendedDiagnostics = false,
            )
        store.save(next)
        assertFalse(store.load().sendCrashReports)
    }
}
