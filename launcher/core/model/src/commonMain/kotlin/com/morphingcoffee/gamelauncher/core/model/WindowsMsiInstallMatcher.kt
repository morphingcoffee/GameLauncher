package com.morphingcoffee.gamelauncher.core.model

data class WindowsUninstallEntry(
    val displayName: String? = null,
    val installLocation: String? = null,
    val windowsInstaller: Boolean = false,
    val uninstallString: String? = null,
)

object WindowsMsiInstallMatcher {
    fun isWindowsMsiInstall(
        executablePath: String,
        expectedDisplayName: String,
        uninstallEntries: List<WindowsUninstallEntry>,
    ): Boolean {
        val normalizedExecutable = executablePath.trim()
        if (normalizedExecutable.isEmpty()) return false
        if (isUnderDefaultProgramFiles(normalizedExecutable)) return true
        return uninstallEntries.any { entry ->
            entry.matchesMsiInstall(
                executablePath = normalizedExecutable,
                expectedDisplayName = expectedDisplayName,
            )
        }
    }

    fun isUnderDefaultProgramFiles(executablePath: String): Boolean {
        val normalized = executablePath.replace('/', '\\').lowercase()
        return "program files" in normalized || "program files (x86)" in normalized
    }
}

private fun WindowsUninstallEntry.matchesMsiInstall(
    executablePath: String,
    expectedDisplayName: String,
): Boolean {
    if (!displayName.equals(expectedDisplayName, ignoreCase = true)) return false
    if (!windowsInstaller && !uninstallString.orEmpty().contains("msiexec", ignoreCase = true)) {
        return false
    }

    val location = installLocation?.trim().orEmpty()
    return location.isEmpty() || executablePath.startsWith(location, ignoreCase = true)
}
