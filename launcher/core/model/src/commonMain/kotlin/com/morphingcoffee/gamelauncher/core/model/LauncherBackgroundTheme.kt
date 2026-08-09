package com.morphingcoffee.gamelauncher.core.model

/**
 * Selectable launcher background themes.
 *
 * Persisted by stable [id] strings — never derive wire/persistence keys from [name].
 * [entries] order is the Settings list order; [DEFAULT] is first.
 */
enum class LauncherBackgroundTheme(
    val id: String,
    val displayName: String,
) {
    STATIC_TERMINAL(
        id = "static_terminal",
        displayName = "STATIC//TERMINAL",
    ),
    SPECTRAL_TOPOLOGY(
        id = "spectral_topology",
        displayName = "SPECTRAL//TOPOLOGY",
    ),
    BACKPLANE_LIVE(
        id = "backplane_live",
        displayName = "BACKPLANE//LIVE",
    ),
    ISO_LATTICE(
        id = "iso_lattice",
        displayName = "ISO//LATTICE",
    ),
    DRAFT_BLUEPRINT(
        id = "draft_blueprint",
        displayName = "DRAFT//BLUEPRINT",
    ),
    ;

    companion object {
        val DEFAULT: LauncherBackgroundTheme = STATIC_TERMINAL

        fun fromId(id: String?): LauncherBackgroundTheme = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
