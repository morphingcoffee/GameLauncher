package com.morphingcoffee.gamelauncher.core.model

object LauncherMetadata {
    const val DEBUG_TOOLS_ENABLED = true

    val VERSION: String
        get() = LauncherVersion.fullVersion()

    const val AUTHOR_NAME = "morphingcoffee"
    const val AUTHOR_GITHUB_URL = "https://github.com/morphingcoffee"
    const val AUTHOR_BLOG_URL = "https://morphingcoffee.github.io/"
    const val REPOSITORY_URL = "https://github.com/morphingcoffee/GameLauncher"

    val RELEASES_URL: String
        get() = "$REPOSITORY_URL/releases"
}
