package com.morphingcoffee.gamelauncher.core.telemetry

import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.LauncherRuntime

object TelemetryBootstrap {
    const val DSN_PROPERTY = "sentry.dsn"

    /**
     * Initialize crash reporting before Koin/Compose.
     * Missing DSN or init failures → safe no-op.
     * When DSN is present, Sentry starts even if the user opted out so toggles can take
     * effect immediately; [SentryCrashReporter] gates every send on preferences.
     */
    fun initialize(
        isDevBuild: Boolean = LauncherRuntime.isDevBuild(),
        dsnOverride: String? = null,
        preferencesStore: TelemetryPreferencesStore? = null,
    ): CrashReporter {
        val store =
            preferencesStore
                ?: runCatching { FileTelemetryPreferencesStore(FileTelemetryPreferencesStore.defaultFile()) }
                    .getOrElse {
                        AppLog.w("Telemetry", "Preferences store unavailable; using in-memory defaults", it)
                        InMemoryTelemetryPreferencesStore()
                    }

        val preferences =
            runCatching { store.load() }
                .getOrDefault(TelemetryPreferences.DEFAULT)

        val dsn =
            (dsnOverride ?: System.getProperty(DSN_PROPERTY) ?: System.getenv("SENTRY_DSN"))
                ?.trim()
                .orEmpty()

        if (dsn.isEmpty()) {
            AppLog.i("Telemetry", "Sentry DSN not configured; crash reporting disabled")
            val noOp =
                NoOpCrashReporter.also {
                    it.updatePreferences(preferences)
                }
            CrashReporting.install(noOp)
            return noOp
        }

        val environment = if (isDevBuild) "development" else "production"
        val pathPrefixes =
            PathRedactor.defaultPrefixes(
                appSupportRoot =
                    runCatching { LibraryPathsBridge.appSupportRoot() }.getOrNull(),
            )

        return try {
            SentryCrashReporter.initializeSentry(
                dsn = dsn,
                environment = environment,
                release = LauncherMetadata.VERSION,
            )
            val reporter =
                SentryCrashReporter(
                    preferencesStore = store,
                    initialPreferences = preferences,
                    environment = environment,
                    pathPrefixes = pathPrefixes,
                )
            CrashReporting.install(reporter)
            val status = if (preferences.sendCrashReports) "enabled" else "paused (user opt-out)"
            AppLog.i("Telemetry", "Sentry crash reporting initialized ($environment, $status)")
            reporter
        } catch (error: Exception) {
            AppLog.w("Telemetry", "Sentry init failed; continuing without crash reporting", error)
            val noOp =
                NoOpCrashReporter.also {
                    it.updatePreferences(preferences)
                }
            CrashReporting.install(noOp)
            noOp
        }
    }
}
