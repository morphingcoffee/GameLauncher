package com.morphingcoffee.gamelauncher.core.telemetry

actual fun createTelemetryPreferencesStore(): TelemetryPreferencesStore = InMemoryTelemetryPreferencesStore()
