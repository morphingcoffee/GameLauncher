package com.morphingcoffee.gamelauncher.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowsUninstallRegistryTest {
    @Test
    fun parseRegQueryOutput_extractsDisplayNameAndInstallLocation() {
        val output =
            """
            HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{ABC}
                DisplayName    REG_SZ    GameLauncher
                InstallLocation    REG_SZ    D:\Games\GameLauncher
                WindowsInstaller    REG_DWORD    0x1
                UninstallString    REG_SZ    MsiExec.exe /X{ABC}

            HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{DEF}
                DisplayName    REG_SZ    Other App
            """.trimIndent()

        val entries = parseRegQueryOutput(output)

        assertEquals(2, entries.size)
        assertEquals("GameLauncher", entries[0].displayName)
        assertEquals("""D:\Games\GameLauncher""", entries[0].installLocation)
        assertTrue(entries[0].windowsInstaller)
        assertEquals("Other App", entries[1].displayName)
    }
}
