package com.morphingcoffee.gamelauncher.core.telemetry

/**
 * Persists privacy toggles under the launcher app-support root.
 * Reads must be safe before Compose/Koin startup.
 */
interface TelemetryPreferencesStore {
    fun load(): TelemetryPreferences

    fun save(preferences: TelemetryPreferences)
}

class InMemoryTelemetryPreferencesStore(
    initial: TelemetryPreferences = TelemetryPreferences.DEFAULT,
) : TelemetryPreferencesStore {
    @Volatile
    private var preferences: TelemetryPreferences = initial

    override fun load(): TelemetryPreferences = preferences

    override fun save(preferences: TelemetryPreferences) {
        this.preferences = preferences
    }
}
