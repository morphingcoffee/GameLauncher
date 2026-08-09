package com.morphingcoffee.gamelauncher.core.telemetry

import java.io.File

actual fun createTelemetryPreferencesStore(): TelemetryPreferencesStore =
    FileTelemetryPreferencesStore(File(LibraryPathsBridge.preferencesFile()))
