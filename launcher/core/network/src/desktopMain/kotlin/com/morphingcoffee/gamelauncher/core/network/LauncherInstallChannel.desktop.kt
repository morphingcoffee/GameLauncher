package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherChannelKey
import com.morphingcoffee.gamelauncher.core.model.LauncherRelease
import com.morphingcoffee.gamelauncher.core.model.PlatformKey

actual object LauncherInstallChannel {
    actual fun detect(): String? {
        val os = System.getProperty("os.name").lowercase()
        return when {
            "win" in os -> {
                if (isWindowsMsiInstall()) {
                    LauncherChannelKey.WINDOWS_X64_MSI
                } else {
                    LauncherChannelKey.WINDOWS_X64_PORTABLE
                }
            }
            "mac" in os || "darwin" in os -> {
                val platformKey = PlatformKey.current() ?: return null
                LauncherChannelKey.macosDmg(platformKey)
            }
            else -> null
        }
    }

    actual fun resolveChannelKey(launcher: LauncherRelease?): String? {
        if (launcher == null) return null
        val preferred = detect() ?: return null
        if (preferred in launcher.channels) return preferred

        val platformKey = PlatformKey.current() ?: return null
        val zipKey = LauncherChannelKey.macosZip(platformKey)
        if (zipKey != null && zipKey in launcher.channels) {
            return zipKey
        }

        return null
    }

    private fun isWindowsMsiInstall(): Boolean {
        val executablePath =
            ProcessHandle
                .current()
                .info()
                .command()
                .orElse(null)
                ?.lowercase()
                ?: return false
        return "program files" in executablePath
    }
}
