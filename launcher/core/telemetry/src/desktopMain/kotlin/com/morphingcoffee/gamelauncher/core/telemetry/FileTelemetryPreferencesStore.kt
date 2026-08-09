package com.morphingcoffee.gamelauncher.core.telemetry

import com.morphingcoffee.gamelauncher.core.logging.AppLog
import kotlinx.serialization.json.Json
import java.io.File

class FileTelemetryPreferencesStore(
    private val file: File,
) : TelemetryPreferencesStore {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

    override fun load(): TelemetryPreferences {
        return try {
            if (!file.exists()) {
                return TelemetryPreferences.DEFAULT
            }
            val text = file.readText()
            if (text.isBlank()) {
                TelemetryPreferences.DEFAULT
            } else {
                json.decodeFromString(TelemetryPreferences.serializer(), text)
            }
        } catch (error: Exception) {
            AppLog.w("Telemetry", "Failed to load preferences; using defaults", error)
            TelemetryPreferences.DEFAULT
        }
    }

    override fun save(preferences: TelemetryPreferences) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(TelemetryPreferences.serializer(), preferences))
        } catch (error: Exception) {
            AppLog.w("Telemetry", "Failed to save preferences", error)
        }
    }

    companion object {
        fun defaultFile(): File = File(LibraryPathsBridge.preferencesFile())
    }
}

/**
 * Avoid a hard dependency cycle: LibraryPaths lives in :core:network.
 * Telemetry resolves the same app-support root convention here for bootstrap-before-Koin.
 */
internal object LibraryPathsBridge {
    fun appSupportRoot(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            "win" in os -> {
                val appData = System.getenv("APPDATA") ?: error("APPDATA is not set")
                java.nio.file.Paths
                    .get(appData, "GameLauncher")
                    .toString()
            }
            "mac" in os || "darwin" in os -> {
                val home = System.getProperty("user.home") ?: error("user.home is not set")
                java.nio.file.Paths
                    .get(home, "Library", "Application Support", "GameLauncher")
                    .toString()
            }
            else -> {
                // Agent/unit-test hosts (Linux) — keep prefs under a writable temp-style path.
                val home = System.getProperty("user.home") ?: System.getProperty("java.io.tmpdir") ?: "."
                java.nio.file.Paths
                    .get(home, ".gamelauncher")
                    .toString()
            }
        }
    }

    fun preferencesFile(): String =
        java.nio.file.Paths
            .get(appSupportRoot(), "preferences.json")
            .toString()
}
