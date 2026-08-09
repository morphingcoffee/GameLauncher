package com.morphingcoffee.gamelauncher.core.network

import com.morphingcoffee.gamelauncher.core.model.LauncherBackgroundTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FileLauncherSettingsRepositoryTest {
    @Test
    fun missingFile_keepsDefaultTheme() =
        runTest {
            withTempSettingsFile { file ->
                val repository = FileLauncherSettingsRepository(file, scope = backgroundScope)
                assertEquals(LauncherBackgroundTheme.DEFAULT, repository.backgroundTheme.value)
                advanceUntilIdle()
                assertEquals(LauncherBackgroundTheme.DEFAULT, repository.backgroundTheme.value)
            }
        }

    @Test
    fun roundTrip_eachThemeId() =
        runTest {
            withTempSettingsFile { file ->
                for (theme in LauncherBackgroundTheme.entries) {
                    val writer = FileLauncherSettingsRepository(file, scope = backgroundScope)
                    writer.writeThemeToDisk(theme)
                    val reader = FileLauncherSettingsRepository(file, scope = backgroundScope)
                    assertEquals(theme, reader.readThemeFromDisk())
                    assertTrue(file.readText().contains(theme.id))
                }
            }
        }

    @Test
    fun unknownThemeId_fallsBackToDefault() =
        runTest {
            withTempSettingsFile { file ->
                file.writeText("""{"background_theme":"not_a_real_theme"}""")
                val repository = FileLauncherSettingsRepository(file, scope = backgroundScope)
                assertEquals(LauncherBackgroundTheme.DEFAULT, repository.readThemeFromDisk())
            }
        }

    @Test
    fun malformedJson_fallsBackToDefault() =
        runTest {
            withTempSettingsFile { file ->
                file.writeText("{ this is not json")
                val repository = FileLauncherSettingsRepository(file, scope = backgroundScope)
                assertEquals(LauncherBackgroundTheme.DEFAULT, repository.readThemeFromDisk())
            }
        }

    @Test
    fun atomicWrite_leavesNoPartialSettingsFile() =
        runTest {
            withTempSettingsFile { file ->
                val repository = FileLauncherSettingsRepository(file, scope = backgroundScope)
                repository.writeThemeToDisk(LauncherBackgroundTheme.DRAFT_BLUEPRINT)
                assertTrue(file.exists())
                assertFalse(File(file.parentFile, "${file.name}.tmp").exists())
                val text = file.readText()
                assertTrue(text.contains("draft_blueprint"))
                assertTrue(text.trimStart().startsWith("{"))
            }
        }

    @Test
    fun startup_emitsDefaultBeforeLoadCompletes() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            withTempSettingsFile { file ->
                file.writeText("""{"background_theme":"static_terminal"}""")
                val repository =
                    FileLauncherSettingsRepository(
                        file = file,
                        ioDispatcher = dispatcher,
                        scope = TestScope(dispatcher),
                    )
                assertEquals(LauncherBackgroundTheme.DEFAULT, repository.backgroundTheme.value)
                dispatcher.scheduler.advanceUntilIdle()
                assertEquals(LauncherBackgroundTheme.STATIC_TERMINAL, repository.backgroundTheme.value)
            }
        }

    @Test
    fun setBackgroundTheme_writesOffCallingThread() =
        runTest {
            val ioStarted = AtomicBoolean(false)
            val dispatcher = StandardTestDispatcher(testScheduler)
            withTempSettingsFile { file ->
                val repository =
                    FileLauncherSettingsRepository(
                        file = file,
                        ioDispatcher = dispatcher,
                        scope = TestScope(dispatcher),
                    )
                dispatcher.scheduler.advanceUntilIdle()

                repository.setBackgroundTheme(LauncherBackgroundTheme.ISO_LATTICE)
                assertEquals(LauncherBackgroundTheme.ISO_LATTICE, repository.backgroundTheme.value)
                assertFalse(file.exists() && file.readText().contains("iso_lattice"))

                dispatcher.scheduler.advanceUntilIdle()
                ioStarted.set(true)
                assertTrue(file.readText().contains("iso_lattice"))
                assertTrue(ioStarted.get())
            }
        }

    private inline fun withTempSettingsFile(block: (File) -> Unit) {
        val dir = createTempDirectory(prefix = "launcher-settings-").toFile()
        try {
            block(File(dir, "settings.json"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
