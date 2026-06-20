package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.WindowsUninstallEntry

internal object WindowsUninstallRegistry {
    private val uninstallRoots =
        listOf(
            """HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall""",
            """HKLM\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall""",
        )

    fun readUninstallEntries(): List<WindowsUninstallEntry> {
        if (!System.getProperty("os.name").contains("win", ignoreCase = true)) {
            return emptyList()
        }

        return uninstallRoots.flatMap { root ->
            runCatching { readUninstallEntries(root) }.getOrElse { emptyList() }
        }
    }

    private fun readUninstallEntries(rootKey: String): List<WindowsUninstallEntry> {
        val output =
            ProcessBuilder("reg", "query", rootKey, "/s")
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .readText()

        return parseRegQueryOutput(output)
    }
}

internal fun parseRegQueryOutput(output: String): List<WindowsUninstallEntry> {
    val entries = mutableListOf<WindowsUninstallEntry>()
    var displayName: String? = null
    var installLocation: String? = null
    var windowsInstaller: Boolean? = null
    var uninstallString: String? = null

    fun flushEntry() {
        if (displayName == null && installLocation == null && windowsInstaller == null && uninstallString == null) {
            return
        }
        entries +=
            WindowsUninstallEntry(
                displayName = displayName,
                installLocation = installLocation,
                windowsInstaller = windowsInstaller == true,
                uninstallString = uninstallString,
            )
        displayName = null
        installLocation = null
        windowsInstaller = null
        uninstallString = null
    }

    for (rawLine in output.lineSequence()) {
        val line = rawLine.trim()
        if (line.startsWith("HKEY_")) {
            flushEntry()
            continue
        }
        if (line.isEmpty()) continue

        val parts = line.split(Regex("\\s{2,}"), limit = 3)
        if (parts.size < 3) continue

        when (parts[0]) {
            "DisplayName" -> displayName = parts[2].trim()
            "InstallLocation" -> installLocation = parts[2].trim()
            "UninstallString" -> uninstallString = parts[2].trim()
            "WindowsInstaller" -> {
                windowsInstaller = parts[2].trim().equals("0x1", ignoreCase = true) || parts[2].trim() == "1"
            }
        }
    }

    flushEntry()
    return entries
}
