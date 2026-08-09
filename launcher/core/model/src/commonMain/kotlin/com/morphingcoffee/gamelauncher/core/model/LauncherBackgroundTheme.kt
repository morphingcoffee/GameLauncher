package com.morphingcoffee.gamelauncher.core.model

/**
 * Selectable launcher background themes.
 *
 * Persisted by stable [id] strings — never derive wire/persistence keys from [name].
 */
enum class LauncherBackgroundTheme(
    val id: String,
    val displayName: String,
) {
    SPECTRAL_TOPOLOGY(
        id = "spectral_topology",
        displayName = "Spectral Topology",
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
    STATIC_TERMINAL(
        id = "static_terminal",
        displayName = "Static Terminal",
    ),
    ;

    companion object {
        val DEFAULT: LauncherBackgroundTheme = SPECTRAL_TOPOLOGY

        fun fromId(id: String?): LauncherBackgroundTheme = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
