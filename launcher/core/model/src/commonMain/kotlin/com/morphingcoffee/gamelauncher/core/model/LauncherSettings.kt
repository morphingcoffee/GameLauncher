package com.morphingcoffee.gamelauncher.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Local launcher preferences persisted as `settings.json`.
 *
 * Not part of the CDN manifest wire contract.
 */
@Serializable
data class LauncherSettings(
    @SerialName("background_theme")
    val backgroundTheme: String = LauncherBackgroundTheme.DEFAULT.id,
) {
    companion object {
        val DEFAULT = LauncherSettings()
    }
}
