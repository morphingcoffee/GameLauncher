package com.morphingcoffee.gamelauncher.core.telemetry

/**
 * Thin telemetry facade. Safe no-op when DSN is missing or crash reporting is off.
 */
interface CrashReporter {
    val preferences: TelemetryPreferences

    fun updatePreferences(preferences: TelemetryPreferences)

    fun captureUncaught(
        thread: Thread,
        throwable: Throwable,
    )

    fun captureLaunchFailure(failure: GameLaunchFailure)

    fun captureUpdateFailure(
        versionLabel: String,
        error: Throwable,
    )

    fun flush(timeoutMillis: Long = DEFAULT_FLUSH_TIMEOUT_MS)

    companion object {
        const val DEFAULT_FLUSH_TIMEOUT_MS = 2_000L
    }
}

object NoOpCrashReporter : CrashReporter {
    @Volatile
    override var preferences: TelemetryPreferences = TelemetryPreferences.DEFAULT
        private set

    override fun updatePreferences(preferences: TelemetryPreferences) {
        this.preferences = preferences
    }

    override fun captureUncaught(
        thread: Thread,
        throwable: Throwable,
    ) = Unit

    override fun captureLaunchFailure(failure: GameLaunchFailure) = Unit

    override fun captureUpdateFailure(
        versionLabel: String,
        error: Throwable,
    ) = Unit

    override fun flush(timeoutMillis: Long) = Unit
}

/**
 * Process-wide facade used by Main, launch, and update paths.
 * Tests may [install] a fake reporter.
 */
object CrashReporting {
    @Volatile
    private var reporter: CrashReporter = NoOpCrashReporter

    fun install(reporter: CrashReporter) {
        this.reporter = reporter
    }

    fun resetForTests() {
        reporter = NoOpCrashReporter
        NoOpCrashReporter.updatePreferences(TelemetryPreferences.DEFAULT)
    }

    fun current(): CrashReporter = reporter

    val preferences: TelemetryPreferences
        get() = reporter.preferences

    fun updatePreferences(preferences: TelemetryPreferences) {
        reporter.updatePreferences(preferences)
    }

    fun captureUncaught(
        thread: Thread,
        throwable: Throwable,
    ) {
        runCatching { reporter.captureUncaught(thread, throwable) }
    }

    fun captureLaunchFailure(failure: GameLaunchFailure) {
        runCatching { reporter.captureLaunchFailure(failure) }
    }

    fun captureUpdateFailure(
        versionLabel: String,
        error: Throwable,
    ) {
        runCatching { reporter.captureUpdateFailure(versionLabel, error) }
    }

    fun flush(timeoutMillis: Long = CrashReporter.DEFAULT_FLUSH_TIMEOUT_MS) {
        runCatching { reporter.flush(timeoutMillis) }
    }
}
