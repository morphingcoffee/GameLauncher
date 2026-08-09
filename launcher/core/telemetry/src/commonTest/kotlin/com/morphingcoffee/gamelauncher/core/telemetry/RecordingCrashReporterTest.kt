package com.morphingcoffee.gamelauncher.core.telemetry

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingCrashReporterTest {
    private class RecordingCrashReporter : CrashReporter {
        override var preferences: TelemetryPreferences = TelemetryPreferences.DEFAULT
            private set
        val launchFailures = mutableListOf<GameLaunchFailure>()
        val uncaught = mutableListOf<Throwable>()

        override fun updatePreferences(preferences: TelemetryPreferences) {
            this.preferences = preferences
        }

        override fun captureUncaught(
            thread: Thread,
            throwable: Throwable,
        ) {
            if (!preferences.sendCrashReports) return
            uncaught += throwable
        }

        override fun captureLaunchFailure(failure: GameLaunchFailure) {
            if (!preferences.sendCrashReports) return
            launchFailures += failure
        }

        override fun captureUpdateFailure(
            versionLabel: String,
            error: Throwable,
        ) = Unit

        override fun flush(timeoutMillis: Long) = Unit
    }

    private lateinit var reporter: RecordingCrashReporter

    @BeforeTest
    fun setUp() {
        reporter = RecordingCrashReporter()
        CrashReporting.install(reporter)
    }

    @AfterTest
    fun tearDown() {
        CrashReporting.resetForTests()
    }

    @Test
    fun captureLaunchFailure_includesGameIdentity() {
        CrashReporting.captureLaunchFailure(
            GameLaunchFailure(
                gameId = "demo",
                displayTitle = "Demo Game",
                installedVersion = "1.2.0",
                platformKey = "windows-x64",
                exitCode = 7,
                durationMillis = 1234,
                message = "Game exited with code 7",
            ),
        )

        val failure = reporter.launchFailures.single()
        assertEquals("demo", failure.gameId)
        assertEquals("Demo Game", failure.displayTitle)
        assertEquals("1.2.0", failure.installedVersion)
        assertEquals("windows-x64", failure.platformKey)
        assertEquals(7, failure.exitCode)
    }

    @Test
    fun disabledCrashReports_areNoOps() {
        reporter.updatePreferences(TelemetryPreferences(sendCrashReports = false))
        CrashReporting.captureLaunchFailure(
            GameLaunchFailure(
                gameId = "demo",
                displayTitle = "Demo",
                installedVersion = null,
                platformKey = null,
                exitCode = 1,
                durationMillis = null,
                message = "fail",
            ),
        )
        assertTrue(reporter.launchFailures.isEmpty())
    }
}
