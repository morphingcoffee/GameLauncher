package com.morphingcoffee.gamelauncher.core.telemetry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelemetryPreferences(
    @SerialName("send_crash_reports")
    val sendCrashReports: Boolean = true,
    @SerialName("share_extended_diagnostics")
    val shareExtendedDiagnostics: Boolean = false,
) {
    /** Extended diagnostics only apply when crash reporting is enabled. */
    val effectiveShareExtendedDiagnostics: Boolean
        get() = sendCrashReports && shareExtendedDiagnostics

    companion object {
        val DEFAULT = TelemetryPreferences()
    }
}
