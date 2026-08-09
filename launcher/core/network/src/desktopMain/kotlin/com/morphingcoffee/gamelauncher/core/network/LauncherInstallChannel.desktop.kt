package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherChannelKey
import com.morphingcoffee.gamelauncher.core.model.LauncherInstallIdentity
import com.morphingcoffee.gamelauncher.core.model.LauncherRelease
import com.morphingcoffee.gamelauncher.core.model.LauncherRuntime
import com.morphingcoffee.gamelauncher.core.model.PlatformKey
import com.morphingcoffee.gamelauncher.core.model.WindowsMsiInstallMatcher
import com.morphingcoffee.gamelauncher.core.model.WindowsUninstallEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

actual object LauncherInstallChannel {
    private val detectMutex = Mutex()
    private var cachedChannel: CachedChannel? = null

    actual suspend fun detect(): String? {
        cachedChannel?.let { return it.value }
        return detectMutex.withLock {
            cachedChannel?.let { return it.value }
            val detected =
                withContext(LauncherInstallChannelTestHooks.ioDispatcher()) {
                    detectUncached()
                }
            cachedChannel = CachedChannel(detected)
            detected
        }
    }

    actual suspend fun resolveChannelKey(launcher: LauncherRelease?): String? {
        if (launcher == null) return null
        val preferred = detect() ?: return null
        return preferred.takeIf { it in launcher.channels }
    }

    internal fun clearDetectionCacheForTests() {
        cachedChannel = null
    }

    private fun detectUncached(): String? {
        val os = LauncherInstallChannelTestHooks.osName().lowercase()
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

    private fun isWindowsMsiInstall(): Boolean {
        val executablePath = LauncherInstallChannelTestHooks.executablePath() ?: return false

        return WindowsMsiInstallMatcher.isWindowsMsiInstall(
            executablePath = executablePath,
            expectedDisplayName = expectedWindowsMsiDisplayName(),
            uninstallEntries = LauncherInstallChannelTestHooks.readUninstallEntries(),
        )
    }

    private fun expectedWindowsMsiDisplayName(): String =
        if (LauncherRuntime.isDevBuild()) {
            LauncherInstallIdentity.WINDOWS_MSI_DISPLAY_NAME_DEV
        } else {
            LauncherInstallIdentity.WINDOWS_MSI_DISPLAY_NAME_PROD
        }

    private data class CachedChannel(
        val value: String?,
    )
}

/**
 * Test-only hooks for [LauncherInstallChannel] detection. Production code must not call these.
 */
internal object LauncherInstallChannelTestHooks {
    @Volatile
    var ioDispatcherOverride: CoroutineDispatcher? = null

    @Volatile
    var osNameOverride: String? = null

    @Volatile
    var executablePathOverride: String? = null

    @Volatile
    var uninstallEntriesProvider: () -> List<WindowsUninstallEntry> = {
        WindowsUninstallRegistry.readUninstallEntries()
    }

    fun ioDispatcher(): CoroutineDispatcher = ioDispatcherOverride ?: Dispatchers.IO

    fun osName(): String = osNameOverride ?: System.getProperty("os.name").orEmpty()

    fun executablePath(): String? =
        executablePathOverride
            ?: ProcessHandle
                .current()
                .info()
                .command()
                .orElse(null)

    fun readUninstallEntries(): List<WindowsUninstallEntry> = uninstallEntriesProvider()

    fun resetAll() {
        ioDispatcherOverride = null
        osNameOverride = null
        executablePathOverride = null
        uninstallEntriesProvider = { WindowsUninstallRegistry.readUninstallEntries() }
        LauncherInstallChannel.clearDetectionCacheForTests()
    }
}
