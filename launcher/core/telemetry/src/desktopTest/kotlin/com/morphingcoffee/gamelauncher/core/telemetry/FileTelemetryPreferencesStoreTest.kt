package com.morphingcoffee.gamelauncher.core.telemetry

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileTelemetryPreferencesStoreTest {
    @Test
    fun missingFile_returnsDefaults() {
        val dir = createTempDirectory(prefix = "telemetry-prefs-").toFile()
        try {
            val store = FileTelemetryPreferencesStore(File(dir, "preferences.json"))
            val prefs = store.load()
            assertTrue(prefs.sendCrashReports)
            assertFalse(prefs.shareExtendedDiagnostics)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun saveAndLoad_roundTrip() {
        val dir = createTempDirectory(prefix = "telemetry-prefs-").toFile()
        try {
            val file = File(dir, "preferences.json")
            val store = FileTelemetryPreferencesStore(file)
            val saved =
                TelemetryPreferences(
                    sendCrashReports = false,
                    shareExtendedDiagnostics = true,
                )
            store.save(saved)
            val loaded = store.load()
            assertEquals(saved, loaded)
            assertTrue(file.readText().contains("send_crash_reports"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
