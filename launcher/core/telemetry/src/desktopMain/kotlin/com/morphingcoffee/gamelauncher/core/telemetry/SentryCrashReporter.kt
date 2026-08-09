package com.morphingcoffee.gamelauncher.core.telemetry

import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.logging.LogLevel
import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata
import com.morphingcoffee.gamelauncher.core.model.LauncherRuntime
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import io.sentry.Breadcrumb
import io.sentry.IScope
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Message
import java.util.concurrent.atomic.AtomicBoolean

class SentryCrashReporter(
    private val preferencesStore: TelemetryPreferencesStore,
    initialPreferences: TelemetryPreferences,
    private val environment: String,
    private val pathPrefixes: List<String>,
) : CrashReporter {
    @Volatile
    override var preferences: TelemetryPreferences = initialPreferences
        private set

    private val closed = AtomicBoolean(false)

    override fun updatePreferences(preferences: TelemetryPreferences) {
        this.preferences = preferences
        preferencesStore.save(preferences)
    }

    override fun captureUncaught(
        thread: Thread,
        throwable: Throwable,
    ) {
        if (!canSend()) return
        runCatching {
            Sentry.withScope { scope ->
                scope.setTag("operation", "launcher_crash")
                scope.setTag("thread", thread.name)
                attachCommonTags(scope)
                maybeAttachExtendedDiagnostics(scope, processOutputTail = null)
                Sentry.captureException(throwable)
            }
        }
    }

    override fun captureLaunchFailure(failure: GameLaunchFailure) {
        if (!canSend()) return
        runCatching {
            Sentry.withScope { scope ->
                scope.setTag("operation", failure.operation)
                scope.setTag("game_id", failure.gameId)
                scope.setTag("game_title", failure.displayTitle)
                failure.installedVersion?.let { scope.setTag("game_version", it) }
                failure.platformKey?.let { scope.setTag("platform", it) }
                failure.exitCode?.let { scope.setTag("exit_code", it.toString()) }
                failure.durationMillis?.let { scope.setExtra("duration_ms", it.toString()) }
                attachCommonTags(scope)
                maybeAttachExtendedDiagnostics(scope, failure.processOutputTail)

                val cause = failure.cause
                if (cause != null) {
                    Sentry.captureException(cause)
                } else {
                    val event =
                        SentryEvent().apply {
                            message = Message().also { it.message = failure.message }
                            level = SentryLevel.ERROR
                            fingerprints = listOf("launch-failure", failure.gameId, failure.operation)
                        }
                    Sentry.captureEvent(event)
                }
            }
        }
    }

    override fun captureUpdateFailure(
        versionLabel: String,
        error: Throwable,
    ) {
        if (!canSend()) return
        runCatching {
            Sentry.withScope { scope ->
                scope.setTag("operation", "update")
                scope.setTag("update_version", versionLabel)
                attachCommonTags(scope)
                maybeAttachExtendedDiagnostics(scope, processOutputTail = null)
                Sentry.captureException(error)
            }
        }
    }

    override fun captureTestEvent(message: String) {
        if (!canSend()) return
        runCatching {
            Sentry.withScope { scope ->
                scope.setTag("operation", CrashReporter.OPERATION_SENTRY_SMOKE_TEST)
                attachCommonTags(scope)
                Sentry.captureException(SentrySmokeTestException(message))
            }
            flush()
        }
    }

    override fun flush(timeoutMillis: Long) {
        if (closed.get()) return
        runCatching {
            Sentry.flush(timeoutMillis)
        }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching {
            Sentry.close()
        }
    }

    private fun canSend(): Boolean = preferences.sendCrashReports && !closed.get()

    private fun attachCommonTags(scope: IScope) {
        scope.setTag("launcher_version", LauncherMetadata.VERSION)
        scope.setTag("environment", environment)
        PlatformKey.current()?.let { scope.setTag("os_arch", it) }
        scope.setTag("dev_build", LauncherRuntime.isDevBuild().toString())
    }

    private fun maybeAttachExtendedDiagnostics(
        scope: IScope,
        processOutputTail: String?,
    ) {
        if (!preferences.effectiveShareExtendedDiagnostics) return

        processOutputTail
            ?.takeIf { it.isNotBlank() }
            ?.let { tail ->
                scope.setExtra("process_output_tail", PathRedactor.redact(tail, pathPrefixes))
            }
        attachLauncherBreadcrumbs(scope)
    }

    private fun attachLauncherBreadcrumbs(scope: IScope) {
        val entries = AppLog.entries.value.takeLast(MAX_BREADCRUMB_ENTRIES)
        for (entry in entries) {
            val breadcrumb =
                Breadcrumb().apply {
                    category = entry.tag
                    level =
                        when (entry.level) {
                            LogLevel.ERROR -> SentryLevel.ERROR
                            LogLevel.WARN -> SentryLevel.WARNING
                            LogLevel.INFO -> SentryLevel.INFO
                            LogLevel.DEBUG -> SentryLevel.DEBUG
                        }
                    message = PathRedactor.redact(AppLog.formatEntry(entry), pathPrefixes)
                }
            scope.addBreadcrumb(breadcrumb)
        }
    }

    class SentrySmokeTestException(
        message: String,
    ) : RuntimeException(message)

    companion object {
        private const val MAX_BREADCRUMB_ENTRIES = 40

        fun initializeSentry(
            dsn: String,
            environment: String,
            release: String,
        ) {
            Sentry.init { options: SentryOptions ->
                options.dsn = dsn
                options.environment = environment
                options.release = release
                options.isSendDefaultPii = false
                options.isEnableUncaughtExceptionHandler = false
                options.sampleRate = 1.0
                // Keep analytics/performance/replay off — crash events only.
                options.tracesSampleRate = null
            }
        }
    }
}
