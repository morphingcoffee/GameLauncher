package com.morphingcoffee.gamelauncher.feature.settings

import com.morphingcoffee.gamelauncher.core.model.LauncherMetadata

data class SettingsLink(
    val label: String,
    val displayText: String,
    val url: String,
)

data class SettingsState(
    val appVersion: String = LauncherMetadata.VERSION,
    val platformLabel: String = "unknown",
    val clockText: String = "",
    val links: List<SettingsLink> = defaultSettingsLinks(),
)

internal fun defaultSettingsLinks(): List<SettingsLink> =
    listOf(
        SettingsLink(
            label = "AUTHOR",
            displayText = LauncherMetadata.AUTHOR_NAME,
            url = LauncherMetadata.AUTHOR_GITHUB_URL,
        ),
        SettingsLink(
            label = "BLOG",
            displayText = "morphingcoffee.github.io",
            url = LauncherMetadata.AUTHOR_BLOG_URL,
        ),
        SettingsLink(
            label = "SOURCE",
            displayText = "github.com/morphingcoffee/GameLauncher",
            url = LauncherMetadata.REPOSITORY_URL,
        ),
    )
