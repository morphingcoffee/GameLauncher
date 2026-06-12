package com.morphingcoffee.gamelauncher.core.model

/**
 * Manifest platform build keys: `windows-x64`, `macos-arm64`, `macos-x64`.
 */
object PlatformKey {
    const val WINDOWS_X64 = "windows-x64"
    const val MACOS_ARM64 = "macos-arm64"
    const val MACOS_X64 = "macos-x64"

    val all: Set<String> =
        setOf(
            WINDOWS_X64,
            MACOS_ARM64,
            MACOS_X64,
        )

    /**
     * Returns the manifest build key for the current OS/arch, or null if unsupported.
     */
    fun current(): String? {
        val os =
            System
                .getProperty("os.name")
                .lowercase()
        val arch =
            System
                .getProperty("os.arch")
                .lowercase()

        val osKey =
            when {
                "win" in os -> "windows"
                "mac" in os || "darwin" in os -> "macos"
                else -> return null
            }

        val archKey =
            when (arch) {
                "aarch64", "arm64" -> "arm64"
                "x86_64", "amd64" -> "x64"
                else -> return null
            }

        return "$osKey-$archKey"
    }
}
