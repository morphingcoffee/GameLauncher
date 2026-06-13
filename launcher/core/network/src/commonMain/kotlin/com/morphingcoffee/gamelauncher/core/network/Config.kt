package com.morphingcoffee.gamelauncher.core.network

object Config {
    private const val DEFAULT_MANIFEST_URL =
        "https://pub-7e50fe985fd54f429195235e41363c26.r2.dev/manifest.json"

    val manifestUrl: String
        get() =
            System
                .getenv("GAME_LAUNCHER_MANIFEST_URL")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_MANIFEST_URL
}
