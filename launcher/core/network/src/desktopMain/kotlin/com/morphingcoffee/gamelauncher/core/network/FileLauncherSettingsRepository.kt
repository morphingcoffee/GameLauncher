package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.logging.AppLog
import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme
import com.morphingcoffee.gamelauncher.core.model.LauncherSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * File-backed [LauncherSettingsRepository] writing `settings.json` atomically.
 *
 * Emits [LauncherBackgroundTheme.DEFAULT] immediately; disk load updates later on [ioDispatcher].
 */
class FileLauncherSettingsRepository(
    private val file: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : LauncherSettingsRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

    private val _backgroundTheme = MutableStateFlow(LauncherBackgroundTheme.DEFAULT)
    override val backgroundTheme: StateFlow<LauncherBackgroundTheme> = _backgroundTheme.asStateFlow()

    init {
        scope.launch {
            val loaded =
                withContext(ioDispatcher) {
                    readThemeFromDisk()
                }
            _backgroundTheme.value = loaded
        }
    }

    override fun setBackgroundTheme(theme: LauncherBackgroundTheme) {
        _backgroundTheme.value = theme
        scope.launch {
            withContext(ioDispatcher + NonCancellable) {
                writeThemeToDisk(theme)
            }
        }
    }

    /** Exposed for tests — synchronous disk read on the caller's thread. */
    internal fun readThemeFromDisk(): LauncherBackgroundTheme {
        return try {
            if (!file.exists()) {
                return LauncherBackgroundTheme.DEFAULT
            }
            val text = file.readText()
            if (text.isBlank()) {
                return LauncherBackgroundTheme.DEFAULT
            }
            val settings = json.decodeFromString(LauncherSettings.serializer(), text)
            LauncherBackgroundTheme.fromId(settings.backgroundTheme)
        } catch (error: Exception) {
            AppLog.w(TAG, "Failed to load settings; using default background theme", error)
            LauncherBackgroundTheme.DEFAULT
        }
    }

    /** Exposed for tests — synchronous atomic write on the caller's thread. */
    internal fun writeThemeToDisk(theme: LauncherBackgroundTheme) {
        try {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val settings = LauncherSettings(backgroundTheme = theme.id)
            val encoded = json.encodeToString(LauncherSettings.serializer(), settings)
            val tmp = File(parent, "${file.name}.tmp")
            tmp.writeText(encoded)
            try {
                Files.move(
                    tmp.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    tmp.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } catch (error: Exception) {
            AppLog.w(TAG, "Failed to save settings", error)
        }
    }

    companion object {
        private const val TAG = "Settings"

        fun defaultFile(): File = File(LibraryPaths.settingsFile())
    }
}
